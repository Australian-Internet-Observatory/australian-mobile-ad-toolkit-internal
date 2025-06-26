/*
 *
 * This class is the entry point and main routine of the app
 *
 * */

package com.adms.australianmobileadtoolkit;

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static com.adms.australianmobileadtoolkit.Common.dataStoreRead;
import static com.adms.australianmobileadtoolkit.Common.dataStoreWrite;
import static com.adms.australianmobileadtoolkit.Common.dataStoreWriteToCorrupt;
import static com.adms.australianmobileadtoolkit.Debugger.copyDebugData;
import static com.adms.australianmobileadtoolkit.InactivityReceiver.cancelAllInactivityNotifications;
import static com.adms.australianmobileadtoolkit.InactivityReceiver.generateNotificationChannel;
import static com.adms.australianmobileadtoolkit.InactivityReceiver.setPeriodicNotifications;
import static com.adms.australianmobileadtoolkit.appSettings.DEBUG;
import static com.adms.australianmobileadtoolkit.appSettings.get_ACTIVATION_CODE_SHORT_DEFAULT;
import static com.adms.australianmobileadtoolkit.appSettings.get_APP_CHILD_DIRECTORY;
import static com.adms.australianmobileadtoolkit.appSettings.get_NOTIFICATION_PERIODIC_CHANNEL_DESCRIPTION;
import static com.adms.australianmobileadtoolkit.appSettings.get_NOTIFICATION_PERIODIC_CHANNEL_ID;
import static com.adms.australianmobileadtoolkit.appSettings.get_NOTIFICATION_PERIODIC_CHANNEL_ID_NAME;
import static com.adms.australianmobileadtoolkit.appSettings.logMessage;
import static com.adms.australianmobileadtoolkit.appSettings.observerIDDefaultValue;
import static com.adms.australianmobileadtoolkit.appSettings.observerRegisteredDefaultValue;
import static com.adms.australianmobileadtoolkit.appSettings.hardFixObserverIDRead;
import static com.adms.australianmobileadtoolkit.interpreter.AccessibilityService.wipeTemporals;
import static com.adms.australianmobileadtoolkit.interpreter.InterpreterWorker.httpRequestRegisterJoinedAt;
import static com.adms.australianmobileadtoolkit.ui.fragments.FragmentMain.safelySetToggleFromViewModel;
import static com.example.KotlinInterop.yieldEmptyPreferences;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.media.projection.MediaProjectionManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.datastore.core.CorruptionException;
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava3.RxDataStore;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.adms.australianmobileadtoolkit.interpreter.InterpreterWorker;
import com.adms.australianmobileadtoolkit.ui.ItemViewModel;
import androidx.datastore.preferences.core.PreferencesSerializer;

import com.adms.australianmobileadtoolkit.ui.dialogs.DialogSubmitAds;
import com.adms.australianmobileadtoolkit.ui.fragments.FragmentMain;
import com.example.KotlinInterop;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;

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
    public static String THIS_OBSERVER_ID = observerIDDefaultValue;
    public static MediaProjectionManager mMediaProjectionManager;

    public static String intentOfMainActivity = "NONE";
    public static MediaRecorder currentMediaRecorder;

    public Boolean initiated = false;
    public static String PERIODIC_WORK_TAG = "PERIODIC_WORK_TAG";
    public static String MANUAL_WORK_TAG = "MANUAL_WORK_TAG";

    public static RxDataStore<Preferences> dataStore;

    public static Thread manualAdDigestThread;

    public static Preferences generateEmptyPreferences() {
        // Return empty preferences to replace the corrupted data
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
        Source source = Okio.source(inputStream);
        BufferedSource bufferedSource = Okio.buffer(source);
        // Kotlin coroutines interop; need to call the suspend function from Java
        // Solution: call runBlocking using Kotlin helper (see below)
        return KotlinInterop.runBlockingReadFrom(bufferedSource);
    }

    public static void initiateDataStore(Context context) {
        if (dataStore == null) {
            ReplaceFileCorruptionHandler<Preferences> corruptionHandler = new ReplaceFileCorruptionHandler<>(
                    ex -> {
                        // Log the corruption exception for debugging purposes
                        System.err.println("DataStore corruption detected: " + ex.getMessage());
                        return yieldEmptyPreferences(); //
                    }
            );
            dataStore = new RxPreferenceDataStoreBuilder(context, /*name=*/ "settings").setCorruptionHandler(corruptionHandler).build();
            CorruptionException xxx = new CorruptionException("fdsfdsf",new Throwable("dsfdsfsf"));
            try {
                corruptionHandler.handleCorruption(xxx, new Continuation<Preferences>() {
                    @NonNull
                    @Override
                    public CoroutineContext getContext() {
                        return null;
                    }

                    @Override
                    public void resumeWith(@NonNull Object o) {

                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    // TODO
    private static ItemViewModel viewModel;
    // private BroadcastReceiver mScreenLockReceiver;  // any intention of using this in future should have a GC
    /*
     *
     * This method is called anytime the app spins up
     *
     * */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initiateDataStore(this);

            // TODO - apply loading dialog to start of recording event

            // TODO - examine recording status's being dropped (and unable to reinitiate) on Pixel devices

            mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            // The directories that will need to be created
            List<String> directoriesToCreate = appSettings.DIRECTORIES_TO_CREATE;
            // Set the main view
            setContentView(R.layout.activity_base);

            // Determine whether the service is running, and use this to determine whether the mToggleButton
            // is then checked
            FragmentMain.setToggle(isServiceRunning());
            logMessage(TAG, "on create was called");
            // Identify the main directory of the app
            mainDir = getMainDir(this.getApplicationContext());
            //prefs.edit().clear().commit(); // Uncomment this line to wipe the Shared Preferences
            // (in case it doesn't wipe when clearing the cache and storage, which technically shouldn't
            // happen, but here we are)
            // Run this block on the first run of the app
            if (dataStoreRead(this, "appFirstCallTime", "NULL").equals("NULL")) {
                dataStoreWrite(this, "appFirstCallTime", String.valueOf(System.currentTimeMillis()));
            }


            if (dataStoreRead(this, "observerFirstRun", "true").equals("true")) {
                logMessage(TAG,  "First run: setting shared preferences and generating directories");
                // Create the directory required by the app within the mainDir
                if ((!mainDir.exists()) && (!mainDir.mkdirs())) {
                    logMessage(TAG, "Failure on onCreate: couldn't create main directory");
                }
                // Create the directories that are necessary for the app's functionality
                for (String value : directoriesToCreate) {
                    File dir = new File(mainDir
                          + (File.separatorChar + value + File.separatorChar));
                    // Fail-safe (in case the directory already exists)
                    if ((!dir.exists()) && (!dir.mkdirs())) {
                        logMessage(TAG, "Failure on onCreate: couldn't create sub-directories");
                    }
                }
                // Generate an observer ID for this device, to be later submitted with data donations
                // dataStoreWrite(this, "observerID", UUID.randomUUID().toString());
                // This code block has finished - commit the observerFirstRun variable,
                // to ensure it doesn't run again
                dataStoreWrite(this, "observerFirstRun", "false");
            }


            if (DEBUG) {
                copyDebugData(this);
            }
            // Set the observer ID
            try {
                THIS_OBSERVER_ID = hardFixObserverIDRead(this);
            } catch (Exception e) {
                if (THIS_OBSERVER_ID == null) {
                    System.exit(0);
                }
            }




            if (dataStoreRead(this, "joinedAtLogged", "false").equals("false")) {
                logMessage("JOINED_AT", "First time run!");
                PackageManager pm = this.getPackageManager();
                ApplicationInfo appInfo = null;
                try {
                    appInfo = pm.getApplicationInfo("com.adms.australianmobileadtoolkit", 0);
                    String appFile = appInfo.sourceDir;
                    long installed = new File(appFile).lastModified(); //Epoch Time
                    dataStoreWrite(this, "joinedAtLogged", "true");
                    logMessage("JOINED_AT", THIS_OBSERVER_ID);
                    logMessage("JOINED_AT", String.valueOf(installed));

                    Thread thread = new Thread(() -> {
                        try {
                            httpRequestRegisterJoinedAt(String.valueOf(installed), THIS_OBSERVER_ID);
                        } catch (Exception e) {
                            logMessage(TAG, e.getMessage());
                        }
                    });
                    thread.start();


                    logMessage("JOINED_AT", "Applied");
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }

            dataStoreWrite(this, "observerRegistered", "true"); // This line is added in to auto-register the user on the first run
            boolean THIS_REGISTRATION_STATUS_AS_BOOLEAN = (!Objects.equals(dataStoreRead(this, "observerRegistered",
                    observerRegisteredDefaultValue), observerRegisteredDefaultValue));

            // Retrieve permission to send notifications whenever the app is opened
            if (ContextCompat.checkSelfPermission(MainActivity.this, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{POST_NOTIFICATIONS},101);
            }

            /*
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                Toast.makeText(this, "no allowed", Toast.LENGTH_SHORT).show();
                // it is not enabled. Ask the user to do so from the settings.
                Intent notificationIntent = new Intent(Settings.ACTION_APP_USAGE_SETTINGS);
                PendingIntent pendingIntent = PendingIntent.getActivity(this,0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
                String expandedNotificationText = String.format("Background activity is restricted on this device.\nPlease allow it so we can post an active notification during work sessions.\n\nTo do so, click on the notification to go to\nApp management -> search for %s -> Battery Usage -> enable 'Allow background activity')", getString(R.string.app_name));
                NotificationCompat.Builder builderRebootNotification = constructNotification(this,
                        get_NOTIFICATION_PERIODIC_CHANNEL_ID(this),
                        "ooby",
                        expandedNotificationText, null)
                        .setContentIntent(pendingIntent);
                sendNotification(this, builderRebootNotification, "NOTIFICATION_REBOOT_ID_CASE");

            }else {
                Toast.makeText(this, "allowed", Toast.LENGTH_SHORT).show();
                // good news! It works fine even in the background.
            }*/
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent();
                String packageName = getPackageName();
                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    startActivity(intent);
                }
            }

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent();
                String packageName = getPackageName();
                ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                if (cm.getRestrictBackgroundStatus() == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED) {
                    intent.setAction(Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS);
                    intent.setData(Uri.parse("package:" + packageName));
                    startActivity(intent);
                }
            }

            openBatteryUsagePage(this);


            // Generate the notification channel
            generateNotificationChannel(this, get_NOTIFICATION_PERIODIC_CHANNEL_ID(this),
                  get_NOTIFICATION_PERIODIC_CHANNEL_ID_NAME(this), get_NOTIFICATION_PERIODIC_CHANNEL_DESCRIPTION(this));
            // Attempt to set the periodic notifications
            setPeriodicNotifications(this);

            viewModel = new ViewModelProvider(this).get(ItemViewModel.class);



            try { WorkManager.getInstance().cancelAllWorkByTag(PERIODIC_WORK_TAG).getResult(); } catch (Exception e) {
                e.printStackTrace();
            }
            try {  WorkManager.getInstance().pruneWork().getResult(); } catch (Exception e) {
                e.printStackTrace();
            }
            try {

                PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(InterpreterWorker.class, 15, TimeUnit.MINUTES)
                      .addTag(PERIODIC_WORK_TAG)
                      .build();
                // Do not start another worker if the current one is active
                WorkManager.getInstance(this.getApplicationContext()).enqueueUniquePeriodicWork("workName", ExistingPeriodicWorkPolicy.KEEP,  periodicWork);
                logMessage(TAG,  "WorkManager is set.");

            } catch (Exception e) {
                e.printStackTrace();
            }

            Intent intentOfMainActivityAsIntent = getIntent();
            refreshIntent(intentOfMainActivityAsIntent, THIS_REGISTRATION_STATUS_AS_BOOLEAN);

            /*
            stateObject.init(this);

            String dummyIdentifier = "1234";
            stateObject xxx = new stateObject(this, dummyIdentifier);
            logMessage(TAG, xxx.toString());1
            if (!xxx.has("hello")) {
                HashMap<Integer, Integer> yyy = new HashMap<>();
                yyy.put(1,2);
                yyy.put(3,4);
                xxx.set("hello", yyy);
                JSONObject berg = new JSONObject();
                try {berg.put("a","b");}catch(Exception e){}
                xxx.set("gerh", berg);
                xxx.commit(this);
                logMessage(TAG, "stateObject committed to memory");
            } else {
                logMessage(TAG, "stateObject retrieved from memory");
                logMessage(TAG, xxx.toString());
                try {
                    logMessage(TAG, (String) ((JSONObject) xxx.get("gerh")).get("a"));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }*/

        /*
        mScreenLockReceiver = new ScreenLockReceiver();
        IntentFilter screenStateFilter = new IntentFilter();
        screenStateFilter.addAction(Intent.ACTION_USER_PRESENT);
        screenStateFilter.addAction(Intent.ACTION_USER_UNLOCKED);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenLockReceiver, screenStateFilter);*/

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        wipeTemporals(this);
    }

    public void openBatteryUsagePage(Context ctx) {
        //Intent intent = new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS);
        //startActivity();
    }

    public void refreshIntent(Intent intentOfMainActivityAsIntent, Boolean THIS_REGISTRATION_STATUS_AS_BOOLEAN) {
        // Deal with the intent (if the app was called by an intent)
        if (intentOfMainActivityAsIntent.hasExtra("INTENT_ACTION")) {
            logMessage(TAG, "Called by intent: "+intentOfMainActivityAsIntent.getStringExtra("INTENT_ACTION"));
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
            logMessage(TAG, "Arrived at intent: "+intentOfMainActivity);
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
        String childDirectory = get_APP_CHILD_DIRECTORY(context);
        // Determine the external files directories
        File[] externalFilesDirs = ContextCompat.getExternalFilesDirs(context, null);
        // If an SD card is detected, use it; otherwise use the internal storage
        return new File(externalFilesDirs[0], childDirectory);
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
        logMessage(TAG, "on resume was called");


        Intent intentOfMainActivityAsIntent = getIntent();
        boolean THIS_REGISTRATION_STATUS_AS_BOOLEAN = (!Objects.equals(dataStoreRead(this, "observerRegistered",
                observerRegisteredDefaultValue), observerRegisteredDefaultValue));
        refreshIntent(intentOfMainActivityAsIntent, THIS_REGISTRATION_STATUS_AS_BOOLEAN);
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
            logMessage(TAG, "Unknown request code: " + requestCode);
            return;
        }

        logMessage(TAG, String.valueOf(data));
        logMessage(TAG, String.valueOf(requestCode));
        logMessage(TAG, String.valueOf(resultCode));

        // If given permission to record the device, begin recording
        if (resultCode == RESULT_OK) {

            //Intent notificationIntent = new Intent(getApplicationContext(), RecorderService.class);  // TODO - checked for API migration
            //notificationIntent.putExtra("INTENT_ACTION", "PERIODIC_NOTIFICATION");
            startRecordingService(resultCode, data);
            //FragmentMain.setToggle(true);
            //safelySetToggleInViewModel(true);
        } else {
            // The mToggleButton must be forced off in case the permission request fails
            logMessage(TAG, "Permission was not granted");
            FragmentMain.setToggle(false);
            //safelySetToggleInViewModel(false);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sendBroadcast(new Intent(this, InactivityReceiver.class)
              .putExtra("INTENT_ACTION", "APP_HAS_CLOSED"));  // TODO - checked for API migration
        //killCurrentThread();

        logMessage(TAG, "APP CLOSED");
    }

    /*
     *
     * This method starts the screen-recording
     *
     * */
    private void startRecordingService(int resultCode, Intent data) {
        Intent intent = RecorderService.newIntent(this, resultCode, data);
        startForegroundService(intent);
        //Intent intent2= new Intent(this, RecorderService.class);
        //bindService(intent2, serviceConnector, Context.BIND_AUTO_CREATE);
       //thisRecorderService.mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    public static void safelySetToggleInViewModel(Boolean thisValue) {
        if (viewModel != null) {
            logMessage(TAG, "viewModel set toggle value: "+thisValue);
            viewModel.setToggleStatusInViewModel(thisValue);
            safelySetToggleFromViewModel();
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


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_POWER) {
            // Do something here...
            logMessage(TAG, "testing key down");
            event.startTracking(); // Needed to track long presses
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_POWER) {
            // Do something here...
            logMessage(TAG, "testing key down");
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public void onStop() {
       super.onStop();
        logMessage(TAG, "running a stop event");
    }
    public static RecorderService thisRecorderService;
    ServiceConnection serviceConnector;

    @Override
    public void onStart() {
        super.onStart();
    }

    public void stopRecorderService() {
        stopService(new Intent(this, RecorderService.class));
    }

    public static String retrieveShortActivationCode(Context context) {
        String myActivationCodeUUIDStringDefault = get_ACTIVATION_CODE_SHORT_DEFAULT(context);
        String myActivationCodeUUIDString = hardFixObserverIDRead(context);
        if (myActivationCodeUUIDString != null) {
            Integer activationShortCodeLength = 6;
            myActivationCodeUUIDString = myActivationCodeUUIDString.substring(myActivationCodeUUIDString.length()-(1+activationShortCodeLength), myActivationCodeUUIDString.length()-1).toUpperCase();
        } else {
            myActivationCodeUUIDString = myActivationCodeUUIDStringDefault;
        }
        return myActivationCodeUUIDString;
    }

}

