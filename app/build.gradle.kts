import com.android.build.api.dsl.ManagedVirtualDevice
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    val properties = Properties().apply {
        load(rootProject.file("local.properties").inputStream())
    }

    val gradleProperties = Properties().apply {
        load(rootProject.file("gradle.properties").inputStream())
    }

    namespace = "com.rohitp.readerproxy"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rohitp.readerproxy"
        minSdk = 30
        targetSdk = 36

        versionCode = Integer.parseInt(gradleProperties.getProperty("VERSION_CODE"))
        versionName = gradleProperties.getProperty("VERSION")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
    }

    signingConfigs {
        fun getFromEnvOrProperties(key: String): String {
            return System.getenv(key) ?: properties.getOrDefault(key, "") as String
        }

        create("release") {
            storeFile = file("$projectDir/keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
        getByName("debug") {
            storeFile = file("$projectDir/keystore-debug.jks")
            storePassword = getFromEnvOrProperties("KEYSTORE_PASSWORD_DEBUG")
            keyAlias = getFromEnvOrProperties("KEY_ALIAS_DEBUG")
            keyPassword = getFromEnvOrProperties("KEY_PASSWORD_DEBUG")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"

        managedDevices {
            /* ---------- define virtual devices ---------- */
            allDevices {
                for (level in 30..35) {
                    create<ManagedVirtualDevice>("pixel6Api$level") {
                        device = "Pixel 6"
                        apiLevel = level
                        systemImageSource = "aosp-atd"
                    }
                }
            }

            /* ---------- group that runs every test on every API ---------- */
            groups {
                create("allDevices") {
                    for (level in 30..35) {
                        targetDevices.add(allDevices["pixel6Api$level"])
                    }
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11

    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

tasks.register<Zip>("zipNativeDebugSymbols") {
    from("build/intermediates/merged_native_libs/release/mergeReleaseNativeLibs/out/lib")
    exclude("armeabi*")
    exclude("mips")
    archiveFileName.set("native-debug-symbols.zip")
    destinationDirectory.set(file("release"))
}

afterEvaluate {
    tasks.named("bundleRelease") {
        finalizedBy("zipNativeDebugSymbols")
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.accompanist.pager)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.timber)

    implementation(libs.bcprov.jdk15on)
    implementation(libs.bcpkix.jdk15on)

    implementation(libs.jsoup)            // DOM parsing / sanitising
    implementation(libs.readability)  // Readability4J
    implementation(libs.slf4j.nop)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.rules)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.ui.test.junit4)
    androidTestImplementation(libs.androidx.uiautomator)

// Robolectric for local unit tests across multiple API levels
    testImplementation(libs.robolectric) // supports API 30-35
    androidTestUtil(libs.androidx.orchestrator)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}