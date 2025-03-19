package com.adms.australianmobileadtoolkit.interpreter;

import static com.adms.australianmobileadtoolkit.Common.writeToFile;
import static com.adms.australianmobileadtoolkit.InactivityReceiver.sendPeriodicNotification;
import static com.adms.australianmobileadtoolkit.RecorderService.createIntentForScreenRecording;
import static com.adms.australianmobileadtoolkit.appSettings.sharedPreferenceGet;
import static com.adms.australianmobileadtoolkit.appSettings.sharedPreferencePut;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.adms.australianmobileadtoolkit.MainActivity;
import com.adms.australianmobileadtoolkit.RecorderServiceIntentActivity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class AccessibilityService extends android.accessibilityservice.AccessibilityService{
    public static AccessibilityService instance;
    private static final Long millisecondsWithinACooldown = 5000L; // 5 seconds


    private static String TAG = "AccessibilityService";

    private static final List<Integer> triggerableEvents = Arrays.asList(
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
    );

    private static final List<String> triggerableAppPackageNames = Arrays.asList(
            "com.facebook.", // Facebook
            "com.instagram.", // Instagram
            "com.zhiliaoapp.musically" // TikTok
    );

    public static boolean withinCooldown(String lastCallMillis, String currentCallMillis) {
        Long differenceOfMilliseconds = Math.abs(Long.valueOf(lastCallMillis) - Long.valueOf(currentCallMillis));
        Log.i(TAG, "differenceOfMilliseconds: "+differenceOfMilliseconds);
        return (differenceOfMilliseconds < millisecondsWithinACooldown);
    }

    public static String currentMillis() {
        return String.valueOf(System.currentTimeMillis());
    }

    public static void wipeTemporals(Context context) {
        sharedPreferencePut(context, "SHARED_PREFERENCE_RECORDER_SERVICE_INTENT_LAST_CALL", "NULL");
        sharedPreferencePut(context, "SHARED_PREFERENCE_RECORDER_SERVICE_INTENT_RUNNING", "false");
    }


    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        try {
            if (triggerableEvents.contains(accessibilityEvent.getEventType())) {
                if (accessibilityEvent.getEventType() == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
                    Log.i(TAG, "AccessibilityEvent.TYPE_WINDOWS_CHANGED");
                }

                if (accessibilityEvent.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    Log.d(TAG, "AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED");
                }

                Log.i(TAG,accessibilityEvent.getPackageName().toString());
                // Within target platform...
                if (isTargetPlatformBoolean(accessibilityEvent.getPackageName().toString())) {
                    Log.i(TAG, "Within target platform...");
                    sharedPreferencePut(this, "SHARED_PREFERENCE_RECORDER_SERVICE_INTENT_TARGET_PLATFORM", accessibilityEvent.getPackageName().toString());
                    // Recording intent hasn't opened...
                    if (sharedPreferenceGet(this, "SHARED_PREFERENCE_RECORDER_SERVICE_INTENT_RUNNING", "false").equals("false")) {
                        Log.i(TAG, "Recording intent has not been called...");
                        // Not currently recording...
                        if (sharedPreferenceGet(this, "RECORDING_STATUS", "false").equals("false")) {
                            Log.i(TAG, "No current recording...");
                            // Not currently in cooldown...
                            String recorderServiceIntentLastCall = sharedPreferenceGet(this, "SHARED_PREFERENCE_RECORDER_SERVICE_INTENT_LAST_CALL", "NULL");
                            Log.i(TAG, "recorderServiceIntentLastCall: " + recorderServiceIntentLastCall);
                            boolean isWithinCooldown = ((!recorderServiceIntentLastCall.equals("NULL")) && withinCooldown(recorderServiceIntentLastCall,currentMillis()));
                            Log.i(TAG, String.valueOf(isWithinCooldown));
                            if (!isWithinCooldown) {
                                Log.i(TAG, "Not currently in cooldown...");
                                sharedPreferencePut(this, "SHARED_PREFERENCE_RECORDER_SERVICE_INTENT_LAST_CALL", currentMillis());
                                startActivity(new Intent(this, RecorderServiceIntentActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                            }
                        }
                    }
                }

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

    public static boolean isTargetPlatformBoolean(String packageName) {
        return (triggerableAppPackageNames.stream().anyMatch(packageName::contains));
    }

    public static String isTargetPlatform(String packageName) {
        if (isTargetPlatformBoolean(packageName)) {
            return packageName;
        }
        return "NULL";
    }
}


// https://stackoverflow.com/questions/18094982/detect-if-my-accessibility-service-is-enabled