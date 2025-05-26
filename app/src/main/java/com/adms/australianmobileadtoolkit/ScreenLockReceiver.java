package com.adms.australianmobileadtoolkit;

import static com.adms.australianmobileadtoolkit.Common.dataStoreRead;
import static com.adms.australianmobileadtoolkit.Common.dataStoreWrite;
import static com.adms.australianmobileadtoolkit.InactivityReceiver.sendScreenLockNotification;

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
            if (dataStoreRead(context,"recordingInterrupted", "false").equals("true")) {
                Log.i(TAG, "Being advised that recordingInterrupted was indeed triggered!!");
                //sendScreenLockNotification(context); // Commenting this out
                dataStoreWrite(context,"recordingInterrupted", "false");
            } else {
                Log.i(TAG, "Being advised that not recordingInterrupted");
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
                Log.i(TAG, "q - Getting recordingStatus: "+ dataStoreRead(context, "recordingStatus", "false"));
                if (dataStoreRead(context, "recordingStatus", "false").equals("true")) {
                    Log.i(TAG, "recordingInterrupted");
                    dataStoreWrite(context, "recordingInterrupted", "true"); // Always the case
                }
                break;
        }
    }
}
