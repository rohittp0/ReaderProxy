package com.rohitp.readerproxy

import android.Manifest
import android.os.Build
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingVpnFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val pkg get() = composeRule.activity.packageName
    private val device
        get() =
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private fun grantPostNotifIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            device.executeShellCommand("pm grant $pkg ${Manifest.permission.POST_NOTIFICATIONS}")
        }
    }

    private fun acceptVpnDialog(timeoutMs: Long = 3_000) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Wait until the positive button shows up
        if (device.wait(
                Until.hasObject(By.res("android:id/button1")),
                timeoutMs
            )
        ) {
            device.findObject(By.res("android:id/button1")).click()
        }
    }


    private fun waitForVpn(expected: Boolean) {
        composeRule.waitUntil(timeoutMillis = 4_000) {
            MyVpnService.isRunning.value == expected
        }
        assertTrue(MyVpnService.isRunning.value == expected)
    }

    @OptIn(ExperimentalTestApi::class)
    fun assertAndClick(text: String, timeoutMs: Long = 1_000, exception: Boolean = true) {
        val matcher = SemanticsMatcher("Button with text '$text'") { node ->
            val textProperty = node.config.getOrNull(SemanticsProperties.Text)
                ?: return@SemanticsMatcher false
            val match = textProperty.find { it.text.contains(text, true) }
            val role = node.config.getOrNull(SemanticsProperties.Role)
            match != null && (role == Role.Button || role == Role.Checkbox)
        }

        try {
            composeRule.waitUntilExactlyOneExists(matcher, timeoutMs)
        } catch (e: ComposeTimeoutException) {
            if (!exception) return
            throw AssertionError("Button with text '$text' not found within $timeoutMs ms", e)
        }

        composeRule.onNode(matcher).performClick()
    }

    /* ---------- tests ---------- */

    @Test
    fun firstLaunch_fullOnboarding_thenToggleVpn() {
        // page 1
        composeRule.onNodeWithText("Welcome to Reader Proxy!")
            .assertIsDisplayed()
        assertAndClick("Next")
        assertAndClick("Next", exception = false)

        grantPostNotifIfNeeded()
        assertAndClick("Grant")

        // page 3 – skip CA for test speed
        assertAndClick("Skip")

        // home screen → start VPN
        assertAndClick("Start Reader Mode")
        acceptVpnDialog()
        waitForVpn(expected = true)

        // stop VPN
        assertAndClick("Stop Reader Mode")
        waitForVpn(expected = false)
    }
}
