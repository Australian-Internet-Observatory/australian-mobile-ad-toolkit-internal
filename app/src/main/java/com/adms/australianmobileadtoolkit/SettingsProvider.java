package com.adms.australianmobileadtoolkit;
import static com.example.KotlinInterop.yieldEmptyPreferences;

import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import io.reactivex.rxjava3.core.Single;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.datastore.rxjava3.RxDataStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.datastore.preferences.core.PreferencesKeys;

public class SettingsProvider extends ContentProvider {

    public static final String METHOD_GET_STRING = "getString";
    public static final String METHOD_PUT_STRING = "putString";

    public static final String KEY_VALUE = "value";
    public static final String KEY_DEFAULT = "default";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public @Nullable Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        Log.i("SettingsProvider", "CALL method=" + method);
        Log.i("SettingsProvider", "CALL arg=" + arg);

        Context ctx = getContext();
        if (ctx == null) return null;

        // Provider runs in main process; DataStore should be created here (single owner)
        RxDataStore<Preferences> ds = SettingsStore.get(ctx);

        try {
            // 1) Ping: proves provider is reachable and shows process
            if ("ping".equals(method)) {
                Bundle out = new Bundle();
                out.putBoolean("ok", true);
                out.putInt("pid", android.os.Process.myPid());
                out.putString("process", Application.getProcessName());
                out.putLong("ts", System.currentTimeMillis());
                return out;
            }

            // 2) Get a string
            if ("getString".equals(method)) {
                final String key = arg;
                final String def = (extras != null) ? extras.getString("default", null) : null;

                String value = ds.data()
                        .onErrorReturnItem(yieldEmptyPreferences()) // handle IO errors gracefully
                        .firstOrError()
                        .map(prefs -> {
                            String got = prefs.get(PreferencesKeys.stringKey(key));
                            return got != null ? got : def;
                        })
                        .blockingGet();

                Bundle out = new Bundle();
                out.putBoolean("ok", true);
                out.putString("value", value);
                return out;
            }

            // 3) Put a string (BLOCK until committed), then verify readback
            if ("putString".equals(method)) {
                final String key = arg;
                final String value = (extras != null) ? extras.getString("value", null) : null;

                ds.updateDataAsync(prefsIn -> {
                    MutablePreferences mp = prefsIn.toMutablePreferences();
                    if (value == null) {
                        mp.remove(PreferencesKeys.stringKey(key));
                    } else {
                        mp.set(PreferencesKeys.stringKey(key), value);
                    }
                    return Single.just(mp);
                }).blockingGet(); // <-- commit + wait

                String readback = ds.data()
                        .onErrorReturnItem(yieldEmptyPreferences())
                        .firstOrError()
                        .map(prefs -> prefs.get(PreferencesKeys.stringKey(key)))
                        .blockingGet();

                Bundle out = new Bundle();
                out.putBoolean("ok", true);
                out.putString("readback", readback);
                return out;
            }

            // Unknown method
            Bundle out = new Bundle();
            out.putBoolean("ok", false);
            out.putString("error", "Unknown method: " + method);
            return out;

        } catch (Throwable t) {
            Bundle out = new Bundle();
            out.putBoolean("ok", false);
            out.putString("error", t.toString());
            return out;
        }
    }

    @Override
    public @Nullable Cursor query(
            @NonNull Uri uri,
            @Nullable String[] projection,
            @Nullable String selection,
            @Nullable String[] selectionArgs,
            @Nullable String sortOrder
    ) {
        throw new UnsupportedOperationException("query not supported");
    }

    @Override
    public @Nullable String getType(@NonNull Uri uri) {
        return null;
    }

    @Override
    public @Nullable Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        throw new UnsupportedOperationException("insert not supported");
    }

    @Override
    public int delete(
            @NonNull Uri uri,
            @Nullable String selection,
            @Nullable String[] selectionArgs
    ) {
        throw new UnsupportedOperationException("delete not supported");
    }

    @Override
    public int update(
            @NonNull Uri uri,
            @Nullable ContentValues values,
            @Nullable String selection,
            @Nullable String[] selectionArgs
    ) {
        throw new UnsupportedOperationException("update not supported");
    }
}