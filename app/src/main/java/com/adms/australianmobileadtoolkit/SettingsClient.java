package com.adms.australianmobileadtoolkit;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

public final class SettingsClient {

    private SettingsClient() {}

    private static Uri uri(Context c) {
        return Uri.parse("content://" + c.getPackageName() + ".settings");
    }

    public static String getString(Context c, String key, String def) {
        Bundle in = new Bundle();
        in.putString("default", def);

        Bundle out = c.getContentResolver().call(
                uri(c),
                "getString",
                key,
                in
        );

        return out != null ? out.getString("value", def) : def;
    }

    public static void putString(Context c, String key, String value) {

        Bundle in = new Bundle();
        in.putString("value", value);

        c.getContentResolver().call(
                uri(c),
                "putString",
                key,
                in
        );
    }

    public static boolean getBoolean(Context c, String key, boolean def) {
        Bundle in = new Bundle();
        in.putBoolean("default", def);

        Bundle out = c.getContentResolver().call(
                uri(c),
                "getBoolean",
                key,
                in
        );

        return out != null ? out.getBoolean("value", def) : def;
    }

    public static void putBoolean(Context c, String key, boolean value) {
        Bundle in = new Bundle();
        in.putBoolean("value", value);

        c.getContentResolver().call(
                uri(c),
                "putBoolean",
                key,
                in
        );
    }
}
