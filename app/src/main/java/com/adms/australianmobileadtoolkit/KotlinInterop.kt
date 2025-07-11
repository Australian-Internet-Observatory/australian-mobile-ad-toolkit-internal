package com.example

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.runBlocking
import okio.BufferedSource

object KotlinInterop {

    @JvmStatic
    fun yieldEmptyPreferences(): Preferences {
        return emptyPreferences();
    }
}