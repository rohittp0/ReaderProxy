package com.rohitp.readerproxy

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "reader_proxy_prefs")

object AppSettings {
    private val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")

    /** Flow<Boolean> that emits the stored flag, default = false. */
    fun onboardingDone(ctx: Context) =
        ctx.dataStore.data.map { prefs -> prefs[ONBOARDING_DONE] == true }

    /** Persist `true` once onboarding completes. */
    suspend fun markOnboardingDone(ctx: Context) {
        ctx.dataStore.edit { it[ONBOARDING_DONE] = true }
    }
}
