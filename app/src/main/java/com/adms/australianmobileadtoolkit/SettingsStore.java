package com.adms.australianmobileadtoolkit;

import static com.example.KotlinInterop.yieldEmptyPreferences;

import android.content.Context;

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava3.RxDataStore;

public final class SettingsStore {

    private static volatile RxDataStore<Preferences> dataStore;

    private SettingsStore() {}

    public static RxDataStore<Preferences> get(Context context) {
        if (dataStore == null) {
            synchronized (SettingsStore.class) {
                if (dataStore == null) {
                    Context appContext = context.getApplicationContext();

                    ReplaceFileCorruptionHandler<Preferences> corruptionHandler =
                            new ReplaceFileCorruptionHandler<>(ex -> {
                                System.err.println("DataStore corruption detected: " + ex.getMessage());
                                return yieldEmptyPreferences(); // simplest “empty prefs”
                            });

                    dataStore = new RxPreferenceDataStoreBuilder(appContext, "settings")
                            .setCorruptionHandler(corruptionHandler)
                            .build();
                }
            }
        }
        return dataStore;
    }
}
