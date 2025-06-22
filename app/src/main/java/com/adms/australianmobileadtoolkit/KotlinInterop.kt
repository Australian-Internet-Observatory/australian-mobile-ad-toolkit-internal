package com.example

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import kotlinx.coroutines.runBlocking
import okio.BufferedSource

object KotlinInterop {
    @JvmStatic
    fun runBlockingReadFrom(source: BufferedSource): Preferences {
        return runBlocking {
            PreferencesSerializer.readFrom(source)
        }
    }
}