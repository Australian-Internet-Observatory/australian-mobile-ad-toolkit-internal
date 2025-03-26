package com.adms.australianmobileadtoolkit;

import static com.adms.australianmobileadtoolkit.InactivityReceiver.sendScreenLockNotification;
import static com.adms.australianmobileadtoolkit.appSettings.sharedPreferenceGet;
import static com.adms.australianmobileadtoolkit.appSettings.sharedPreferencePut;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Objects;

public class ScreenLockReceiver extends BroadcastReceiver {

    private static String TAG = "ScreenLockReceiver";

    private static void testRecordingInterrupted(Context context) {
        if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (sharedPreferenceGet(context, "SHARED_PREFERENCE_RECORDING_INTERRUPTED", "false").equals("true")) {
                Log.i(TAG, "Being advised that SHARED_PREFERENCE_RECORDING_INTERRUPTED was indeed triggered!!");
                //sendScreenLockNotification(context); // Commenting this out
                sharedPreferencePut(context, "SHARED_PREFERENCE_RECORDING_INTERRUPTED", "false");
            } else {
                Log.i(TAG, "Being advised that not SHARED_PREFERENCE_RECORDING_INTERRUPTED");
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        switch(Objects.requireNonNull(intent.getAction())) {
            case Intent.ACTION_USER_PRESENT:
                Log.i(TAG, "ACTION_USER_PRESENT");
                testRecordingInterrupted(context);
                break;
            case Intent.ACTION_USER_UNLOCKED:
                Log.i(TAG, "ACTION_USER_UNLOCKED");
                testRecordingInterrupted(context);
                break;
            case Intent.ACTION_SCREEN_OFF:
                Log.i(TAG, "ACTION_SCREEN_OFF");
                // This relies on the idea that this event is received before the recording status is formally updated - in which case, the
                if (sharedPreferenceGet(context, "RECORDING_STATUS", "false").equals("true")) {
                    Log.i(TAG, "SHARED_PREFERENCE_RECORDING_INTERRUPTED");
                    sharedPreferencePut(context, "SHARED_PREFERENCE_RECORDING_INTERRUPTED", "true"); // Always the case
                }
                break;
        }


    }
}
