/*
 *
 * This class deals with the recording service, responsible for managing the creation of
 * screen recording video files
 *
 * */

package com.adms.australianmobileadtoolkit;

import static android.app.Activity.RESULT_OK;
import static android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION;

import static com.adms.australianmobileadtoolkit.Common.dataStoreRead;
import static com.adms.australianmobileadtoolkit.Common.dataStoreWrite;
import static com.adms.australianmobileadtoolkit.Common.filePath;
import static com.adms.australianmobileadtoolkit.Common.getFilesInDirectory;
import static com.adms.australianmobileadtoolkit.Common.readStringFromFile;
import static com.adms.australianmobileadtoolkit.InactivityReceiver.constructNotification;
import static com.adms.australianmobileadtoolkit.InactivityReceiver.constructNotificationForward;
import static com.adms.australianmobileadtoolkit.InactivityReceiver.generateNotificationChannel;
import static com.adms.australianmobileadtoolkit.logging.Logging.addALog;
import static com.adms.australianmobileadtoolkit.MainActivity.SCREEN_RECORDING_PERMISSION_CODE;
import static com.adms.australianmobileadtoolkit.appSettings.get_NOTIFICATION_RECORDING_CHANNEL_DESCRIPTION;
import static com.adms.australianmobileadtoolkit.appSettings.get_NOTIFICATION_RECORDING_CHANNEL_ID;
import static com.adms.australianmobileadtoolkit.appSettings.get_NOTIFICATION_RECORDING_CHANNEL_ID_NAME;
import static com.adms.australianmobileadtoolkit.appSettings.get_NOTIFICATION_RECORDING_DESCRIPTION;
import static com.adms.australianmobileadtoolkit.appSettings.get_NOTIFICATION_RECORDING_TITLE;
import static com.adms.australianmobileadtoolkit.appSettings.get_RECORD_SERVICE_EXTRA_RESULT_CODE;
import static com.adms.australianmobileadtoolkit.appSettings.logMessage;
import static com.adms.australianmobileadtoolkit.appSettings.maxNumberOfVideos;
import static com.adms.australianmobileadtoolkit.interpreter.AccessibilityService.triggerableAppPackageNames;
import static com.adms.australianmobileadtoolkit.interpreter.AccessibilityServiceManager.isAccessibilityServiceEnabled;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionConfig;
import android.media.projection.MediaProjectionManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.Surface;
import android.view.WindowManager;

import androidx.core.app.NotificationCompat;

import com.adms.australianmobileadtoolkit.interpreter.AccessibilityService;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class RecorderService extends Service {
    private Intent data;
    private ServiceHandler mServiceHandler;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaRecorder mMediaRecorder;
    private BroadcastReceiver mScreenStateReceiver;
    private int resultCode;
    private final int dispatchCooldownResetValue = 2;
    private int dispatchCooldown = dispatchCooldownResetValue;
    private String previousRecordingFilename;
    private String tentativeRecordingFilename;
    private String appPackageName = "";
    private static final String TAG = "RecorderService";

    // The extra result code associated with the intent of the recording service
    private static final String EXTRA_DATA = appSettings.RECORD_SERVICE_EXTRA_DATA;
    // The ID of the notification associated with the recording service
    // (the value has no actual bearing on the functionality, although don't set it to zero:
    // https://developer.android.com/guide/components/foreground-services#:~:text=startForeground(ONGOING_NOTIFICATION_ID%2C%20notification)%3B)
    private static final int ONGOING_NOTIFICATION_ID = appSettings.RECORD_SERVICE_ONGOING_NOTIFICATION_ID;
    // Whether or not the device screen is off
    public static boolean screenOff = false;
    // Whether or not a recording is in progress
    public static boolean recordingInProgress = false;
    // The videoDir variable is responsible for identifying the folder where the recordings
    // will be stored
    private String videoDir;

    private static Intent recorderIntent;
    private MediaProjectionManager projectionManager;

    private int displayWidth = 0;
    private int displayHeight = 0;
    private int screenDensity = 0;

    /*
     *
     * This method generates a new intent for the recording service
     *
     * */
    static Intent newIntent(Context context, int resultCode, Intent data) {
        recorderIntent = new Intent(context, RecorderService.class);  // TODO - checked for API migration
        recorderIntent.putExtra(get_RECORD_SERVICE_EXTRA_RESULT_CODE(context), resultCode);
        recorderIntent.putExtra(EXTRA_DATA, data);
        return recorderIntent;
    }

    public static void createIntentForScreenRecording(Activity fragmentActivity) {
        Intent screenRecordingIntent;
        // In Android API version 14, configurations are introduced for screen recordings - this code
        // ensures that the default configuration is selected for the display when the dialog is shown
        // (on newer devices)

        /*if (mProjectionManager == null) {
            mProjectionManager = (MediaProjectionManager) fragmentActivity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        }*/
        MediaProjectionManager thisMediaProjectionManager = (MediaProjectionManager) fragmentActivity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            /*screenRecordingIntent = mProjectionManager.createScreenCaptureIntent(
                    MediaProjectionConfig.createConfigForUserChoice());*/
            screenRecordingIntent = thisMediaProjectionManager.createScreenCaptureIntent(
                    MediaProjectionConfig.createConfigForDefaultDisplay()); // TODO - desired effect was not observed
        } else {
            screenRecordingIntent = thisMediaProjectionManager.createScreenCaptureIntent();
        }
        fragmentActivity.startActivityForResult(screenRecordingIntent, SCREEN_RECORDING_PERMISSION_CODE);
    }

    /*
     *
     * The broadcast receiver is responsible for identifying when the device enters various states,
     * and handling the corresponding functionality
     *
     * */
    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch(intent.getAction()) {
                case Intent.ACTION_SCREEN_ON:
                    logMessage(TAG,  "The device's screen is on: start recording");
                    if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        mMediaRecorder.resume();
                    }
                    screenOff = false;
                    sendBroadcast(new Intent(context, InactivityReceiver.class)
                            .putExtra("INTENT_ACTION", "SCREEN_IS_ON"));  // TODO - checked for API migration
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    logMessage(TAG,  "The device's screen is off: stop recording and schedule the ..");
                    screenOff = true;
                    sendBroadcast(new Intent(context, InactivityReceiver.class)
                            .putExtra("INTENT_ACTION", "SCREEN_IS_OFF"));  // TODO - checked for API migration
                    if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        mMediaRecorder.pause();
                    }
                    break;
                case Intent.ACTION_CONFIGURATION_CHANGED:
                    if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {

                        //stopRecording();
                        /*
                        mMediaRecorder.reset();
                        //stopRecording();
                        boolean didPrepare = configureMediaRecorder();
                        attemptToStartRecording(didPrepare, resultCode, data, false);
                        logMessage(TAG, "configuration changed logged here");*/
                    } else {
                        if (!screenOff) {
                            //logMessage(TAG,  "The device's configuration has changed: restarting recording");
                            //stopRecording();

                            //projectionManager = (MediaProjectionManager) getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                            //startRecording(resultCode, data);
                        }
                    }
                    break;
                case Intent.ACTION_BATTERY_CHANGED:
                    break;
            }
        }
    }

    /*
     *
     * This method determines if the device is charging
     *
     * */
    public static boolean deviceIsCharging(Context context) {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent = context.registerReceiver(
                    null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED), Context.RECEIVER_NOT_EXPORTED);
        } else {
            intent = context.registerReceiver(
                    null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        }
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return (plugged == BatteryManager.BATTERY_PLUGGED_AC
                || plugged == BatteryManager.BATTERY_PLUGGED_USB);
    }

    /*
     *
     * The ServiceHandler is here applied to assist with messages involved in starting the recording
     * service
     *
     * */
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            if (resultCode == RESULT_OK) {
                startRecording(resultCode, data);
            }
        }
    }

    PowerManager.WakeLock wakeLock;

    /*
     *
     * This method deals with the initiation events of the recording service
     *
     * */
    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyApp::MyWakelockTag");
        wakeLock.acquire();
        // The service is instantiated in the foreground to prevent it from getting killed when the
        // app is closed
        Intent notificationIntent = new Intent(this, RecorderService.class);  // TODO - checked for API migration
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_MUTABLE);
        // Attempt to generate the notification channel
        generateNotificationChannel(this, get_NOTIFICATION_RECORDING_CHANNEL_ID(this),
                get_NOTIFICATION_RECORDING_CHANNEL_ID_NAME(this), get_NOTIFICATION_RECORDING_CHANNEL_DESCRIPTION(this));
        // Send the notification
        NotificationCompat.Builder builderPeriodicNotification = constructNotification(this,
                get_NOTIFICATION_RECORDING_CHANNEL_ID(this),
                get_NOTIFICATION_RECORDING_TITLE(this),
                get_NOTIFICATION_RECORDING_DESCRIPTION(this), null)
                .setContentIntent(pendingIntent);
        Notification notification = constructNotificationForward(this, builderPeriodicNotification);
        // Configure and start the service
        // Forward/backwards compatibility
        startForeground(ONGOING_NOTIFICATION_ID, notification);
        // The receiver registers for determining if the device screen is on or off
        mScreenStateReceiver = new MyBroadcastReceiver();
        IntentFilter screenStateFilter = new IntentFilter();
        screenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        screenStateFilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        screenStateFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mScreenStateReceiver, screenStateFilter);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mScreenStateReceiver, screenStateFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(mScreenStateReceiver, screenStateFilter);
        }

        // Set the handler's operation to be conducted in the background
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        Looper mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
        // The videoDir variable is set here
        videoDir = MainActivity.getMainDir(this.getApplicationContext()).getAbsolutePath()
                + (File.separatorChar + "videos" + File.separatorChar);
    }

    /*
     *
     * This method is executed when the service is started
     *
     * */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Get the intent
        resultCode = intent.getIntExtra(get_RECORD_SERVICE_EXTRA_RESULT_CODE(this), 0);
        data = intent.getParcelableExtra(EXTRA_DATA);
        // If the intent is malformed, throw an error
        if (resultCode == 0 || data == null) {
            throw new IllegalStateException("Result code or data missing.");
        }
        // Send the message to the mServiceHandler
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);
        return START_REDELIVER_INTENT;
    }

    private Handler mHandler;

    private String recordingFilename(String videoDir, String orientation, Context context) {
        return (videoDir + File.separatorChar + getCurrentAppPackageName(context) + "." + ((int) Math.floor(System.currentTimeMillis() / (double) 1000)) + "." + UUID.randomUUID().toString() + "." + orientation + ".mp4");
    }

    public static String augmentAppPackageName(String tentativeAppPackageName) {
        return tentativeAppPackageName.replaceAll("\n", "").replaceAll("\\.","_");

    }

    public static String getCurrentAppPackageName(Context context) {
        String appPackageName = "unclassified";
        String tentativeAppPackageName = "unclassified";
        try { tentativeAppPackageName = readStringFromFile(new File(MainActivity.getMainDir(context), "withinTargetApplication")); } catch (Exception ignored) {
            ignored.printStackTrace();
        }
        String finalTentativeAppPackageName = tentativeAppPackageName;
        if (triggerableAppPackageNames.stream().anyMatch(x -> finalTentativeAppPackageName.contains(x))) {
            appPackageName = augmentAppPackageName(tentativeAppPackageName);
        }
        return appPackageName;
    }

    /*
    *
    * This method awaits a premature stop of the screen-recorder, and retrieves the lsat screen-recording's filename, adjusting it
    * to the name of the current app package name (if applicable)
    *
    * */
    public static void tentativeRecordingRecovery(Context context) {

        File videosDirectory = (new File (MainActivity.getMainDir(context).getAbsolutePath(), "videos"));
        String tentativeRecordingFilename = "NULL";

        if (Objects.requireNonNull(videosDirectory.listFiles()).length > 0) {
            List<String> orderedVideoPaths = Arrays.stream(videosDirectory.listFiles()).map((x) -> {
                List<String> splitFile = Arrays.asList(x.getAbsolutePath().split("/"));
                Integer timestamp = Integer.valueOf(Arrays.asList(splitFile.get(splitFile.size() - 1).split("\\.")).get(1));
                return new Pair<>(x.getAbsolutePath(), timestamp);
            }).sorted(Comparator.comparing(o -> o.second)).map(y -> y.first).collect(Collectors.toList());
            tentativeRecordingFilename = orderedVideoPaths.get(orderedVideoPaths.size() - 1);
        }

        logMessage(TAG, "The current recording at time of premature stop is:");
        logMessage(TAG, tentativeRecordingFilename);//dataStoreRead(context, "TENTATIVE_RECORDING_FILENAME", "NULL"));
        logMessage(TAG, dataStoreRead(context,"recorderServiceIntentTargetPlatform", "NULL"));
        logMessage(TAG, dataStoreRead(context,"recorderServiceIntentTargetPlatform_CALL_TIME", "NULL"));

        // At the point of starting a recording, it may not yet be classified (e.g. if we are not within the target app at starting the recording
        // but then go into it - thereafter, we abruptly stop the recording, causing it to be retained as an unclassified recording
        // To overcome this, we have a 'catching' event that recovers unclassified recordings
        // TODO
        // To replicate, begin a recording in this app, and then switch to a target platform
        long cooldownOnCallTime = (10 * 1000);
        long tentativeCallTime = Long.parseLong((dataStoreRead(context,"recorderServiceIntentTargetPlatform_CALL_TIME", String.valueOf(0))));

        logMessage(TAG, "cooldownOnCallTime: " + cooldownOnCallTime);
        logMessage(TAG, "tentativeCallTime: " + tentativeCallTime);

        String tentativePlatform = dataStoreRead(context,"recorderServiceIntentTargetPlatform", "NULL");
        // If the recording is unclassified, the tentative platform is non-null, and the time of reading the tentative platform was not more than 10 seconds ago
        if ((tentativeRecordingFilename.contains("unclassified")) && (!tentativePlatform.equals("NULL")) && (Math.abs(System.currentTimeMillis() - tentativeCallTime) < cooldownOnCallTime)) {
            logMessage(TAG, "Can recover!!!!");
            String newFilename = tentativeRecordingFilename.replace("unclassified", augmentAppPackageName(tentativePlatform));
            logMessage(TAG, "Original filename: " + tentativeRecordingFilename);
            logMessage(TAG, "New filename: " + newFilename);
            (new File(tentativeRecordingFilename)).renameTo(new File(newFilename));
        } else {
            (new File(tentativeRecordingFilename)).delete();
        }
    }

    private boolean configureMediaRecorder() {

        final Integer lowerBoundOnWidth = (android.os.Build.VERSION.SDK_INT < 28) ? 2000 : 600;

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getRealMetrics(metrics);
        screenDensity = metrics.densityDpi;

        displayWidth = 0;
        displayHeight = 0;

        // This code block only initiates when a new stream of recordings is started (originally, we had hoped to have
        // it execute in the 'configuration change' event (such as when changing orientation of the device), although it
        // was found that it would cause the screen recordings to drop off and then re-request permission, and so we avoided in said case.
        //
        // What this left us with was a configuration of the recording dimensions to be set when the screen recordings' stream initiates
        // and not at the start of each individual recording. So then, if a user was to consent screen recordings while their device was in landscape
        // mode, all subsequent recordings in that stream would be configured to landscape as well, even if they weren't.
        //
        // This is especially problematic as it can cause the on-device ML capabilities to neglect advertisements if viewed in portrait-mode
        // after giving consent while in landscape mode.
        //
        // Consequently, attempting to override the anticipated dimensions (small-width on big-height vs. small-height on big-width) causes the screen
        // recorder not to initiate
        //
        // The Fix: Hereafter, all recordings will be assigned in the orientation in the stream for which they begin. Then, during sampling, the sampler
        // will pinpoint the orientation at the point in time that the recording was undertaken, through means of the logs - if a mismatch is observed
        // between the orientation of the screen-recording, and the one provided in the logs, a cropping will then be warranted on the frame.

        if (metrics.widthPixels < metrics.heightPixels) {
            displayWidth = Math.max((int)Math.round(metrics.widthPixels/ appSettings.recordScaleDivisor), lowerBoundOnWidth);
            displayHeight = (int)Math.round(displayWidth*((double)metrics.heightPixels/(double)metrics.widthPixels));
        } else {
            displayWidth = Math.max((int)Math.round(metrics.widthPixels/ appSettings.recordScaleDivisor), lowerBoundOnWidth);
            displayHeight = (int)Math.round(displayWidth*((double)metrics.heightPixels/(double)metrics.widthPixels));
        }

        String finalOrientationAdjusted = ((displayWidth < displayHeight) ? "portrait" : "landscape");

        int videoRecordingMaximumFileSize = appSettings.videoRecordingMaximumFileSize;

        if (android.os.Build.VERSION.SDK_INT < 28) {
            videoRecordingMaximumFileSize = 1000000;
        }

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        logMessage(TAG, "displayWidthAdjusted:"+displayWidth );
        logMessage(TAG, "displayHeightAdjusted:"+displayHeight);
        mMediaRecorder.setVideoSize(displayWidth, displayHeight);
        mMediaRecorder.setMaxFileSize(videoRecordingMaximumFileSize); // 5mb (4.7mb)
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
        //mMediaRecorder.setVideoEncodingBitRate(25000);
        mMediaRecorder.setCaptureRate(30); // success with 1 - 30
        mMediaRecorder.setVideoFrameRate(30);
        // Set the preliminary output file
        previousRecordingFilename = recordingFilename(videoDir, finalOrientationAdjusted, this);
        dataStoreWrite( this, "TENTATIVE_RECORDING_FILENAME", previousRecordingFilename);
        mMediaRecorder.setOutputFile(previousRecordingFilename);
        boolean didPrepare = false;
        try {
            // Attempt to prepare the recording
            mMediaRecorder.prepare();
            didPrepare = true;
        } catch (Exception e) {
            logMessage(TAG, "Failed on startRecording: ");
            e.printStackTrace();
        }
        return didPrepare;
    }

    /*
     *
     * This method starts the media recorder, and generates the resulting video files
     *
     * */
    private void startRecording(int resultCode, Intent data) {
        final Integer lowerBoundOnWidth = (android.os.Build.VERSION.SDK_INT < 28) ? 2000 : 500;
        // If the recording is not in progress
        if(!recordingInProgress) {
            projectionManager = (MediaProjectionManager)
                    getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            mMediaRecorder = new MediaRecorder();

            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager wm = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
            wm.getDefaultDisplay().getRealMetrics(metrics);
            screenDensity = metrics.densityDpi;
            displayWidth = Math.max((int)Math.round(metrics.widthPixels/ appSettings.recordScaleDivisor), lowerBoundOnWidth);
            displayHeight = (int)Math.round(displayWidth*((double)metrics.heightPixels/(double)metrics.widthPixels));

            // Determine the orientation of the device
            String finalOrientation = ((displayWidth < displayHeight) ? "portrait" : "landscape");
            // The following info listener is set to execute when the mMediaRecorder identifies that
            // the recording service has created a video recording that exceeds the maximum file size
            mMediaRecorder.setOnInfoListener((mr, what, extra) -> {
                // If the maximum file size has been reached
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_NEXT_OUTPUT_FILE_STARTED) {
                    //logMessage(TAG, );

                    if (previousRecordingFilename != null) {
                        String tentativeAppPackageName = "";
                        Boolean withinTargetPlatform = true;

                        // By default (if the user has not yet accepted to accessibility services), we do not conduct a check on the target platform
                        if (isAccessibilityServiceEnabled(getApplicationContext(), AccessibilityService.class)) {
                            // There is a possibility that the value may've not been written.
                            try {
                                tentativeAppPackageName = getCurrentAppPackageName(this);
                                assert tentativeAppPackageName != null;
                                withinTargetPlatform = (!(tentativeAppPackageName.contains("NULL")));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            if (withinTargetPlatform) {
                                appPackageName = tentativeAppPackageName;
                                dispatchCooldown = dispatchCooldownResetValue;
                            } else {
                                dispatchCooldown --;
                            }
                        }

                        // This block is only actioned when the previous recording has not yet been identified as relating to a target app,
                        // which in turn preserves recordings that have already been identified as relevant.
                        if (previousRecordingFilename.contains("unclassified")) {
                            if (dispatchCooldown <= 0) {
                                (new File(previousRecordingFilename)).delete();
                            } else {
                                if (appPackageName.isEmpty()) {
                                    (new File(previousRecordingFilename)).delete();
                                } else {
                                    // Apply the app package name to the recording
                                    (new File(previousRecordingFilename)).renameTo(
                                            new File(previousRecordingFilename.replace("unclassified", appPackageName)));
                                    // The tentative recording filename only indicates the current file - which is no longer this one
                                    // dataStoreWrite(this, "TENTATIVE_RECORDING_FILENAME", previousRecordingFilename);
                                }
                            }
                        }

                        // Delete the previous recording
                        // Note: There is an instance where accessibilityService may observe that the user leaves a target platform before the current
                        // recording has finished dispatching - when the recorderService then checks the accessibilityService
                        // for the current app, it will return to it that the last reading was not in the target platform, causing it
                        // to discard the recording (which would still contain portion thereof the target platform.
                        //
                        // To get around this, we can set a cooldown that requires that at least one recording containing non-target apps
                        // be retained (after leaving a target app). This can't guarantee desired functionality, but it should catch
                        // most issues.


                    }
                    previousRecordingFilename = tentativeRecordingFilename;
                }
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_APPROACHING) {
                    logMessage(TAG, "The media recorder has identified that the maximum file size has"
                            + " been reached; setting new output file.");

                    // Change configuration of metrics
                    /*
                    int displayWidthAdjusted = 0;
                    int displayHeightAdjusted = 0;

                    if (metrics.widthPixels < metrics.heightPixels) {
                        displayWidthAdjusted = Math.max((int)Math.round(metrics.widthPixels/ appSettings.recordScaleDivisor), lowerBoundOnWidth);
                        displayHeightAdjusted = (int)Math.round(displayWidthAdjusted*((double)metrics.heightPixels/(double)metrics.widthPixels));
                    } else {
                        displayHeightAdjusted = Math.max((int)Math.round(metrics.heightPixels/ appSettings.recordScaleDivisor), lowerBoundOnWidth);
                        displayWidthAdjusted = (int)Math.round(displayHeightAdjusted*((double)metrics.widthPixels/(double)metrics.heightPixels));
                    }

                    String finalOrientationAdjusted = ((displayWidthAdjusted < displayHeightAdjusted) ? "portrait" : "landscape");
                    mMediaRecorder.setVideoSize(displayWidthAdjusted, displayHeightAdjusted);

                     */


                    // Write out a new file
                    tentativeRecordingFilename = recordingFilename(videoDir, finalOrientation, this); // TODO - set to adjusted
                    dataStoreWrite(this, "TENTATIVE_RECORDING_FILENAME", tentativeRecordingFilename);
                    try (RandomAccessFile newRandomAccessFile =
                                 new RandomAccessFile(tentativeRecordingFilename,"rw")) {



                        mMediaRecorder.setNextOutputFile(newRandomAccessFile.getFD());



                        File thisVideoFolder = filePath(Arrays.asList((videoDir))); // TODO - inserted

                        // delete landscapes, then non-positives, then positives
                        List<String> files = getFilesInDirectory(thisVideoFolder);
                        List<String> filesPositive = files.stream().filter(x -> x.contains("positive")).collect(Collectors.toList());
                        List<String> filesLandscape = files.stream().filter(x -> x.contains("landscape")).collect(Collectors.toList());
                        List<String> filesUnsifted = files.stream().filter(x ->
                                (!(x.contains("positive") || x.contains("landscape")))).collect(Collectors.toList());
                        Collections.sort(filesPositive);
                        Collections.sort(filesLandscape);
                        Collections.sort(filesUnsifted);

                        List<String> filesSeparated = Stream.concat( filesLandscape.stream(), filesUnsifted.stream() ).collect(Collectors.toList());
                        filesSeparated = Stream.concat( filesSeparated.stream(), filesPositive.stream()).collect(Collectors.toList());

                        if (filesSeparated.size() > maxNumberOfVideos) {
                            int numberExcessFiles = filesSeparated.size()-maxNumberOfVideos;
                            List<String> filesToDelete = filesSeparated.subList(0, numberExcessFiles);
                            for (String s : filesToDelete) {
                                logMessage(TAG,  "Deleting file: "+videoDir+s);
                                File thisFile = filePath(Arrays.asList(videoDir, s));
                                thisFile.delete();
                            }
                        }
                        logMessage(TAG,  "number of files in videos folder: "+filesSeparated.size());
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            //CamcorderProfile cpLow = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);
            // Configure the mMediaRecorder
            //mMediaRecorder.setProfile(cpLow);
            //CamcorderProfile cpHigh = CamcorderProfile.get(CamcorderProfile.QUALITY_);

            /*logMessage(TAG, "cpHigh.videoBitRate: " + cpHigh.videoBitRate);
            logMessage(TAG, "cpHigh.videoFrameWidth: " + cpHigh.videoFrameWidth);
            logMessage(TAG, "cpHigh.videoFrameHeight: " + cpHigh.videoFrameHeight);
            logMessage(TAG, "cpHigh.videoFrameRate: " + cpHigh.videoFrameRate);*/


            boolean didPrepare = configureMediaRecorder();

            attemptToStartRecording(didPrepare, resultCode, data, true);
        }
    }

    private void attemptToStartRecording(boolean didPrepare, int thisResultCode, Intent thisData, boolean firstPass) {

        // Set up a new MediaProjectionManager for the recording process
        boolean didStart = false;
        if (didPrepare) {
            try {
                if (firstPass) {
                    mMediaProjection = projectionManager.getMediaProjection(thisResultCode, thisData); // Data is intent
                    // TODO - fix hander to do something when stopped : https://github.com/mtsahakis/MediaProjectionDemo/blob/3a98fc8e5e86da4dc75c3c048d27ddcd4f2925e9/app/src/main/java/com/mtsahakis/mediaprojectiondemo/ScreenCaptureService.java#L49
                    mMediaProjection.registerCallback(new MediaProjection.Callback() {
                        // Implement callback methods here
                        @Override
                        public void onStop() {
                            super.onStop();
                            dataStoreWrite( getApplicationContext(), "recordingStatus", "false");
                            logMessage("recordingStatus", "Setting recordingStatus to false - recorderService");
                            dataStoreWrite(getApplicationContext(), "recorderServiceIntentLastCall", String.valueOf(System.currentTimeMillis()));
                            logMessage(TAG, "RecorderService was possibly stopped prematurely.");
                            // This event is triggered one of a few ways - either from the status bar chip,
                            // from the screen lock 'auto stop' functionality, or from manually stopping the Media Projection
                            // In any case, we make it also stop the service.
                            try {
                                MainActivity.safelySetToggleInViewModel(false);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            try {
                                stopSelf();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }, mHandler);
                    Surface surface = mMediaRecorder.getSurface();
                    mVirtualDisplay = mMediaProjection.createVirtualDisplay("MainActivity",
                            displayWidth, displayHeight, screenDensity,
                            VIRTUAL_DISPLAY_FLAG_PRESENTATION,
                            surface, null, null);
                }
                // Start the recording
                mMediaRecorder.start();
                didStart = true;
                addALog(getApplicationContext(), "REC-BGN");
            } catch (Exception e) {
                logMessage(TAG, "Failed on startRecording: ");
                e.printStackTrace();
            }
        }

        if (didStart) {
            recordingInProgress = true;
            dataStoreWrite( getApplicationContext(), "recordingStatus", "true"); // TODO:14.04.25
            // logMessage("recordingStatus", "Setting recordingStatus to true - 1");

            sendBroadcast(new Intent(this, InactivityReceiver.class)
                    .putExtra("INTENT_ACTION", "RECORDING_HAS_STARTED"));  // TODO - checked for API migration
            //MainActivity.safelySetToggleInViewModel(true);
        }
    }

    /*
     *
     * This method stops the recording service
     *
     * */
    private void stopRecording() {
        // If the recording is in progress
        logMessage(TAG,  "Recording has been forced to stop.");
        logMessage(TAG,  "Service is running: "+recordingInProgress);
        if (recordingInProgress) {
            // Attempt to stop the service
            boolean actionedStop = true;
            try {
                mMediaRecorder.stop();
            } catch(Exception e) {
                actionedStop = false;
                logMessage(TAG, "Failed on stopRecording: ");
                e.printStackTrace();
            }
            try {
                mMediaProjection.stop();
            } catch(Exception e) {
                actionedStop = false;
                logMessage(TAG, "Failed on stopRecording: ");
                e.printStackTrace();
            }
            try {
                mMediaRecorder.release();
            } catch(Exception e) {
                actionedStop = false;
                logMessage(TAG, "Failed on stopRecording: ");
                e.printStackTrace();
            }
            try {
                mVirtualDisplay.release();
            } catch(Exception e) {
                actionedStop = false;
                logMessage(TAG, "Failed on stopRecording: ");
                e.printStackTrace();
            }
            //if (actionedStop) { // TODO
            recordingInProgress = false;
            addALog(getApplicationContext(), "REC-END");
            sendBroadcast(new Intent(this, InactivityReceiver.class)
                    .putExtra("INTENT_ACTION", "RECORDING_HAS_STOPPED"));  // TODO - checked for API migration
            //MainActivity.safelySetToggleInViewModel(false);
            //}
        }
    }

    /*
     *
     * This method is executed on binding the recording service
     *
     * */
    @Override
    public IBinder onBind(Intent intent) {
        // There is no binding, so return null
        return null;
    }

    /*
     *
     * This method is executed on destroying the service
     *
     * */
    @Override
    public void onDestroy() {
        stopRecording();
        unregisterReceiver(mScreenStateReceiver);
        wakeLock.release();
        addALog(getApplicationContext(), "REC-KLL");
        super.onDestroy();
    }
}