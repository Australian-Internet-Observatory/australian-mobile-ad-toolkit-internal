package com.adms.australianmobileadtoolkit;

import static com.adms.australianmobileadtoolkit.Common.dataStoreWrite;
import static com.adms.australianmobileadtoolkit.RecorderService.createIntentForScreenRecording;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class RecorderServiceIntentActivity extends Activity {
    public static final int SCREEN_RECORDING_PERMISSION_CODE = 1;
    public static String TAG = "RecorderServiceIntentActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public void onStart() {
        super.onStart();
        createIntentForScreenRecording(this);
        dataStoreWrite(this, "recorderServiceIntentRunning", "true");
    }

    @Override
    public void onStop() {
        super.onStop();
        dataStoreWrite(this, "recorderServiceIntentRunning", "false");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Inform us if the activity code doesn't correspond to a permission request
        if (requestCode != SCREEN_RECORDING_PERMISSION_CODE) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }

        Log.i(TAG, String.valueOf(data));
        Log.i(TAG, String.valueOf(requestCode));
        Log.i(TAG, String.valueOf(resultCode));

        // If given permission to record the device, begin recording
        if (resultCode == RESULT_OK) {
            startRecordingService(resultCode, data);
            /*
            String targetPlatform = dataStoreRead(this, "recorderServiceIntentTargetPlatform", "NULL");

            if (!targetPlatform.equals("NULL")) {
                Intent intent = new Intent();
                intent.setClassName(targetPlatform, "");
                startActivity(intent);
            }*/
        }

        finish();
        //
    }


    private void startRecordingService(int resultCode, Intent data) {
        Intent intent = RecorderService.newIntent(this, resultCode, data);
        startService(intent);
    }
}
