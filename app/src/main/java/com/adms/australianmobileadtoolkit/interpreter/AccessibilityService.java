package com.adms.australianmobileadtoolkit.interpreter;

import static com.adms.australianmobileadtoolkit.Common.writeToFile;

import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.adms.australianmobileadtoolkit.MainActivity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class AccessibilityService extends android.accessibilityservice.AccessibilityService{
    public static AccessibilityService instance;

    private static final List<Integer> triggerableEvents = Arrays.asList(
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
    );

    private static final List<String> triggerableAppPackageNames = Arrays.asList(
            "com.facebook.", // Facebook
            "com.instagram.", // Instagram
            "com.zhiliaoapp.musically" // TikTok
    );

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        try {
            if (triggerableEvents.contains(accessibilityEvent.getEventType())) {
                if (accessibilityEvent.getEventType() == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
                    Log.d("Event", "AccessibilityEvent.TYPE_WINDOWS_CHANGED");
                }

                if (accessibilityEvent.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    Log.d("Event", "AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED");
                }

                Log.d("Pkg",accessibilityEvent.getPackageName().toString());
                File rootDirectoryPath = MainActivity.getMainDir(this);
                writeToFile(new File(rootDirectoryPath, "withinTargetApplication"),isTargetPlatform(accessibilityEvent.getPackageName().toString()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onInterrupt() { }

    @Override
    protected void onServiceConnected() {
        try {
            super.onServiceConnected();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String isTargetPlatform(String packageName) {
        if (triggerableAppPackageNames.stream().anyMatch(packageName::contains)) {
            return packageName;
        }
        return "NULL";
    }
}


// https://stackoverflow.com/questions/18094982/detect-if-my-accessibility-service-is-enabled