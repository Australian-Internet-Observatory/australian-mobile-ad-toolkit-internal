package com.adms.australianmobileadtoolkit.interpreter;

import static com.adms.australianmobileadtoolkit.Common.dataStoreRead;
import static com.adms.australianmobileadtoolkit.Common.dataStoreWrite;
import static com.adms.australianmobileadtoolkit.Common.writeToFile;
import static com.adms.australianmobileadtoolkit.appSettings.logMessage;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.adms.australianmobileadtoolkit.MainActivity;
import com.adms.australianmobileadtoolkit.RecorderService;
import com.adms.australianmobileadtoolkit.RecorderServiceIntentActivity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class AccessibilityService extends android.accessibilityservice.AccessibilityService{
    public static AccessibilityService instance;
    private static final Long millisecondsWithinACooldown = 180000L; // 5 seconds TODO:14.04.25
    private static boolean logging = false;

    private static String TAG = "AccessibilityService";

    private static final List<Integer> triggerableEvents = Arrays.asList(
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
    );

    public static final List<String> triggerableAppPackageNames = Arrays.asList(
            "com.facebook.katana", // Facebook
            "com.facebook.lite", // Facebook
            "com.instagram.android", // Instagram
            "com.zhiliaoapp.musically", // TikTok
            "com.google.android.youtube" // Youtube
    );

    public static boolean withinCooldown(String lastCallMillis, String currentCallMillis) {
        Long differenceOfMilliseconds = Math.abs(Long.valueOf(lastCallMillis) - Long.valueOf(currentCallMillis));
        if (logging) { logMessage(TAG, "differenceOfMilliseconds: "+differenceOfMilliseconds); };
        return (differenceOfMilliseconds < millisecondsWithinACooldown);
    }

    public static String currentMillis() {
        return String.valueOf(System.currentTimeMillis());
    }

    public static void wipeTemporals(Context context) {
        dataStoreWrite(context, "recorderServiceIntentLastCall", "NULL");
        dataStoreWrite(context, "recorderServiceIntentRunning", "false");
    }


    private boolean isServiceRunningA() {
        // Get the ActivityManager for the device
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        // Loop through all services within it
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            // If any of the services are equal to that of this app, return true
            if (RecorderService.class.getName().equals(service.service.getClassName())) {

                return true;
            }
        }
        return false;
    }


    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        try {
            if (triggerableEvents.contains(accessibilityEvent.getEventType())) {
                if (accessibilityEvent.getEventType() == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
                    if (logging) { logMessage(TAG, "AccessibilityEvent.TYPE_WINDOWS_CHANGED"); }
                }

                if (accessibilityEvent.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    if (logging) { logMessage(TAG, "AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED"); }
                }

                if (accessibilityEvent.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                    if (logging) { logMessage(TAG, "AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED"); }
                }

                if (logging) { logMessage(TAG,accessibilityEvent.getPackageName().toString()); }
                // Within target platform...
                if (isTargetPlatformBoolean(accessibilityEvent.getPackageName().toString())) {
                    if (logging) { logMessage(TAG, "Within target platform..."); }
                    dataStoreWrite(this, "recorderServiceIntentTargetPlatform", accessibilityEvent.getPackageName().toString());
                    dataStoreWrite(this, "recorderServiceIntentTargetPlatform_CALL_TIME", String.valueOf(System.currentTimeMillis()));
                    // Recording intent hasn't opened...
                    //if (dataStoreRead(this, "recorderServiceIntentRunning", "false").equals("false")) {
                    if (logging) { logMessage(TAG, "Recording intent has not been called...");}
                        KeyguardManager myKM = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);

                        // TODO - the recording status desyncs from the fragmentmain toggle when this safeguard turns off - there should be a closer sync strategy between both features
                        if(myKM.inKeyguardRestrictedInputMode()) {
                            if (logging) { logMessage(TAG, "Averting screen recording as screen is locked...");}
                        } else {
                            // Not currently recording...
                            if (logging) { logMessage(TAG, "r - Getting recordingStatus: "+ dataStoreRead( this, "recordingStatus", "false"));}
                            //if (dataStoreRead(this, "recordingStatus", "false").equals("false")) {
                            if (logging) { logMessage(TAG, "No current recording...");}
                                // Not currently in cooldown...
                                String recorderServiceIntentLastCall = dataStoreRead(this, "recorderServiceIntentLastCall", "NULL");
                            if (logging) { logMessage(TAG, "recorderServiceIntentLastCall: " + recorderServiceIntentLastCall);}
                                boolean isWithinCooldown = ((!recorderServiceIntentLastCall.equals("NULL")) && withinCooldown(recorderServiceIntentLastCall,currentMillis()));
                            if (logging) { logMessage(TAG, String.valueOf(isWithinCooldown));}
                                if (!isWithinCooldown) {

                                    if (!isServiceRunningA()) { // TODO - this is a temporary fix
                                        if (logging) { logMessage(TAG, "Not currently in cooldown...");}
                                        dataStoreWrite(this, "recorderServiceIntentLastCall", currentMillis());
                                        startActivity(new Intent(this, RecorderServiceIntentActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                    }
                                }
                            //}
                        }
                    //}
                }

                File rootDirectoryPath = MainActivity.getMainDir(this);
                if (!accessibilityEvent.getPackageName().toString().contains("australianmobileadtoolkit")) {
                    writeToFile(new File(rootDirectoryPath, "withinTargetApplication"),isTargetPlatform(accessibilityEvent.getPackageName().toString()));
                }

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