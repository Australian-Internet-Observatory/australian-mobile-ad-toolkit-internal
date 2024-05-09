/*
 *
 * This class is the entry point and main routine of the app
 *
 * */

package com.adms.australianmobileadtoolkit;

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static com.adms.australianmobileadtoolkit.Debugger.copyDebugData;
import static com.adms.australianmobileadtoolkit.InactivityReceiver.cancelAllInactivityNotifications;
import static com.adms.australianmobileadtoolkit.InactivityReceiver.generateNotificationChannel;
import static com.adms.australianmobileadtoolkit.InactivityReceiver.setPeriodicNotifications;
import static com.adms.australianmobileadtoolkit.Settings.DEBUG;
import static com.adms.australianmobileadtoolkit.Settings.NOTIFICATION_PERIODIC_CHANNEL_DESCRIPTION;
import static com.adms.australianmobileadtoolkit.Settings.NOTIFICATION_PERIODIC_CHANNEL_ID;
import static com.adms.australianmobileadtoolkit.Settings.NOTIFICATION_PERIODIC_CHANNEL_ID_NAME;
import static com.adms.australianmobileadtoolkit.Settings.SHARED_PREFERENCE_OBSERVER_ID_DEFAULT_VALUE;
import static com.adms.australianmobileadtoolkit.Settings.SHARED_PREFERENCE_REGISTERED_DEFAULT_VALUE;
import static com.adms.australianmobileadtoolkit.Settings.sharedPreferenceGet;
import static com.adms.australianmobileadtoolkit.Settings.sharedPreferencePut;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.adms.australianmobileadtoolkit.interpreter.InterpreterWorker;
import com.adms.australianmobileadtoolkit.ui.ItemViewModel;
import com.adms.australianmobileadtoolkit.ui.fragments.FragmentMain;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

// TODO do further testing on intermittent stops

public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";
    // The permission code necessary for screen-recording
    public static final int SCREEN_RECORDING_PERMISSION_CODE = 1;
    // The MediaProjectionManager used with screen-recording
    public static MediaProjectionManager mProjectionManager;
    // The main directory variable (to be used with file copying)
    public static File mainDir;
    // The observer ID is set to nothing to begin
    public static String THIS_OBSERVER_ID = SHARED_PREFERENCE_OBSERVER_ID_DEFAULT_VALUE;
    // The registration status of the user
    public static String THIS_REGISTRATION_STATUS = SHARED_PREFERENCE_REGISTERED_DEFAULT_VALUE;

    public static String intentOfMainActivity = "NONE";

    // TODO
    private static ItemViewModel viewModel;

    /*
     *
     * This method assists with creating new configuration details for devices
     *
     * */
    public void logDeviceIdentifier() {
        System.out.println( "Device Identifier: " + android.os.Build.MODEL);
    }

    /*
     *
     * This method is called anytime the app spins up
     *
     * */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The directories that will need to be created
        List<String> directoriesToCreate = Settings.DIRECTORIES_TO_CREATE;
        // Set the main view
        setContentView(R.layout.activity_base);
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        // Determine whether the service is running, and use this to determine whether the mToggleButton
        // is then checked
        FragmentMain.setToggle(isServiceRunning());
        Log.i(TAG, "on create was called");
        // Identify the main directory of the app
        mainDir = getMainDir(this.getApplicationContext());
        //prefs.edit().clear().commit(); // Uncomment this line to wipe the Shared Preferences
        // (in case it doesn't wipe when clearing the cache and storage, which technically shouldn't
        // happen, but here we are)
        // Run this block on the first run of the app
        if (sharedPreferenceGet(this, "SHARED_PREFERENCE_FIRST_RUN", "true").equals("true")) {
            System.out.println( "First run: setting shared preferences and generating directories");
            // Create the directory required by the app within the mainDir
            if ((!mainDir.exists()) && (!mainDir.mkdirs())) {
                Log.e(TAG, "Failure on onCreate: couldn't create main directory");
            }
            // Create the directories that are necessary for the app's functionality
            for (String value : directoriesToCreate) {
                File dir = new File(mainDir
                      + (File.separatorChar + value + File.separatorChar));
                // Fail-safe (in case the directory already exists)
                if ((!dir.exists()) && (!dir.mkdirs())) {
                    Log.e(TAG, "Failure on onCreate: couldn't create sub-directories");
                }
            }
            // Generate an observer ID for this device, to be later submitted with data donations
            sharedPreferencePut(this, "SHARED_PREFERENCE_OBSERVER_ID", UUID.randomUUID().toString());
            // This code block has finished - commit the SHARED_PREFERENCE_FIRST_RUN variable,
            // to ensure it doesn't run again
            sharedPreferencePut(this, "SHARED_PREFERENCE_FIRST_RUN", "false");
        }
        if (DEBUG) {
            copyDebugData(this);
        }
        // Set the observer ID
        THIS_OBSERVER_ID = sharedPreferenceGet(this, "SHARED_PREFERENCE_OBSERVER_ID",
              SHARED_PREFERENCE_OBSERVER_ID_DEFAULT_VALUE);
        THIS_REGISTRATION_STATUS = sharedPreferenceGet(this, "SHARED_PREFERENCE_REGISTERED",
              SHARED_PREFERENCE_REGISTERED_DEFAULT_VALUE);
        boolean THIS_REGISTRATION_STATUS_AS_BOOLEAN = (!Objects.equals(THIS_REGISTRATION_STATUS, SHARED_PREFERENCE_REGISTERED_DEFAULT_VALUE));

        // Retrieve permission to send notifications whenever the app is opened
        if (ContextCompat.checkSelfPermission(MainActivity.this, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{POST_NOTIFICATIONS},101);
        }
        // Generate the notification channel
        generateNotificationChannel(this, NOTIFICATION_PERIODIC_CHANNEL_ID,
              NOTIFICATION_PERIODIC_CHANNEL_ID_NAME, NOTIFICATION_PERIODIC_CHANNEL_DESCRIPTION);
        // Attempt to set the periodic notifications
        setPeriodicNotifications(this);

        viewModel = new ViewModelProvider(this).get(ItemViewModel.class);

        String PERIODIC_WORK_TAG = "PERIODIC_WORK_TAG";

        try { WorkManager.getInstance().cancelAllWorkByTag(PERIODIC_WORK_TAG).getResult(); } catch (Exception e) { /* Do nothing */ }
        try {  WorkManager.getInstance().pruneWork().getResult(); } catch (Exception e) { /* Do nothing */ }
        try {
            System.out.println( "WorkManager is set.");
            PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(InterpreterWorker.class, 15, TimeUnit.MINUTES)
                  .addTag(PERIODIC_WORK_TAG)
                  .build();
            // Do not start another worker if the current one is active
            WorkManager.getInstance().enqueueUniquePeriodicWork(
                    "DETECTION_OR_SIFT", ExistingPeriodicWorkPolicy.KEEP,  periodicWork);

        } catch (Exception e) { /* Do nothing */ }

        Intent intentOfMainActivityAsIntent = getIntent();
        refreshIntent(intentOfMainActivityAsIntent, THIS_REGISTRATION_STATUS_AS_BOOLEAN);
    }

    public void refreshIntent(Intent intentOfMainActivityAsIntent, Boolean THIS_REGISTRATION_STATUS_AS_BOOLEAN) {
        // Deal with the intent (if the app was called by an intent)
        if (intentOfMainActivityAsIntent.hasExtra("INTENT_ACTION")) {
            Log.i(TAG, "Called by intent: "+intentOfMainActivityAsIntent.getStringExtra("INTENT_ACTION"));
            switch (Objects.requireNonNull(intentOfMainActivityAsIntent.getStringExtra("INTENT_ACTION"))) {
                case "REGISTER" :
                    // Ignoring cases where a register notification triggers a registered instance of the app
                    if (!THIS_REGISTRATION_STATUS_AS_BOOLEAN) {
                        intentOfMainActivity = "REGISTER";
                    }
                    cancelAllInactivityNotifications(this);
                    ; break ;
                case "TURN_ON_SCREEN_RECORDER" :
                    // Ignoring cases where a periodic notification triggers a non-registered instance of the app
                    if (THIS_REGISTRATION_STATUS_AS_BOOLEAN) {
                        intentOfMainActivity = "TURN_ON_SCREEN_RECORDER";
                    }
                    cancelAllInactivityNotifications(this);
                    ; break ;
            }
        }
    }

    /*
     *
     * This method determines the location of the mainDir variable, depending on internal/external
     * storage configurations (NOTE: Functionality has been removed due to updated permissions on newer
     * Android SDKs)
     *
     * */
    public static File getMainDir(Context context) {
        // The child directory to instantiate
        String childDirectory = Settings.APP_CHILD_DIRECTORY;
        // Determine the external files directories
        File[] externalFilesDirs = ContextCompat.getExternalFilesDirs(context, null);
        // If an SD card is detected, use it; otherwise use the internal storage
        return new File(externalFilesDirs[0], childDirectory);
    }

    /*
     *
     * This method attempts to create a list of files
     *
     * */
    public static void createFiles(final Context context, final List<Integer> inputRawResources) {
        try {
            // Get the context's resources
            final Resources resources = context.getResources();
            // Number of bytes to read in a single chunk
            final byte[] largeBuffer = new byte[1024 * 4];
            // Ephemeral variable used for tracking bytes to allocate for each file
            int bytesRead = 0;
            // For each file
            for (Integer resource : inputRawResources) {
                String fName = resources.getResourceEntryName(resource)
                      .substring(9)
                      .replace("_", ".")
                      .replace("0", "-");
                File outFile = new File(mainDir.getAbsolutePath()
                      + (File.separatorChar + "tessdata" + File.separatorChar), fName);
                // Read the file as a stream, and allocate it
                final OutputStream outputStream = new FileOutputStream(outFile);
                final InputStream inputStream = resources.openRawResource(resource);
                while ((bytesRead = inputStream.read(largeBuffer)) > 0) {
                    if (largeBuffer.length == bytesRead) {
                        outputStream.write(largeBuffer);
                    } else {
                        final byte[] shortBuffer = new byte[bytesRead];
                        System.arraycopy(largeBuffer, 0, shortBuffer, 0, bytesRead);
                        outputStream.write(shortBuffer);
                    }
                }
                inputStream.close();
                outputStream.flush();
                outputStream.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed on createFiles: ", e);
        }

    }

    /*
     *
     * This method handles the functionality of resuming the app
     *
     * */
    @Override
    public void onResume() {
        super.onResume();
        boolean serviceIsRunning = isServiceRunning();
        FragmentMain.setToggle(serviceIsRunning);
        safelySetToggleInViewModel(serviceIsRunning); // we can't have this code here as it will be
        // called directly after issuing permission, with an outdated value for the toggle
        Log.i(TAG, "on resume was called");
    }

    /*
     *
     * This method handles activity results (e.g. getting permission for screen-recording)
     *
     * */
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

            //Intent notificationIntent = new Intent(getApplicationContext(), RecorderService.class);  // TODO - checked for API migration
            //notificationIntent.putExtra("INTENT_ACTION", "PERIODIC_NOTIFICATION");
            startRecordingService(resultCode, data);
            //FragmentMain.setToggle(true);
            //safelySetToggleInViewModel(true);
        } else {
            // The mToggleButton must be forced off in case the permission request fails
            Log.i(TAG, "Permission was not granted");
            FragmentMain.setToggle(false);
            //safelySetToggleInViewModel(false);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sendBroadcast(new Intent(this, InactivityReceiver.class)
              .putExtra("INTENT_ACTION", "APP_HAS_CLOSED"));  // TODO - checked for API migration
    }

    /*
     *
     * This method starts the screen-recording
     *
     * */
    private void startRecordingService(int resultCode, Intent data) {
        Intent intent = RecorderService.newIntent(this, resultCode, data);
        startService(intent);
    }

    public static void safelySetToggleInViewModel(Boolean thisValue) {
        if (viewModel != null) {
            System.out.println( "viewModel set toggle value: "+thisValue);
            viewModel.setToggleStatusInViewModel(thisValue);
        }
    }

    /*
     *
     * This method determines whether the service associated with the app is running, mainly for use
     * with setting the toggle button 'on'
     *
     * */
    public boolean isServiceRunning() {
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

}

