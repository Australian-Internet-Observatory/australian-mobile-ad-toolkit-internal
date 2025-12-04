package com.adms.australianmobileadtoolkit;

import static com.adms.australianmobileadtoolkit.Common.readStringFromFile;
import static com.adms.australianmobileadtoolkit.Common.writeToFile;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class appSettings {

   public static final boolean CONST_IS_LOGGING = true;
   // The extra result code associated with the intent of the recording service
   public static final String get_RECORD_SERVICE_EXTRA_RESULT_CODE(Context context) {
      return context.getString(R.string.app_titled_code)+"ExtraResultCode";
   }
   public static final String RECORD_SERVICE_EXTRA_DATA = "data";
   // The ID of the notification associated with the recording service
   // (the value has no actual bearing on the functionality, although don't set it to zero:
   // https://developer.android.com/guide/components/foreground-services#:~:text=startForeground(ONGOING_NOTIFICATION_ID%2C%20notification)%3B)
   public static final int RECORD_SERVICE_ONGOING_NOTIFICATION_ID = 1;

   public static boolean USING_DEMOGRAPHIC_QUESTIONS = false;
   // Maximum file size for video recordings (5MB)

   public static int videoRecordingMaximumFileSize = 2000000;
   // Originally, we had set that the maximum video file size to be set to 2MB - later we found that this size
   // affected processing, as it was too large (and time-intensive as a consequence) to maintain a serializeable
   // object that could be persisted for the sequential background processing aspects of the apps functionality.
   // So we reduced the size to 1MB, and found that while it improved the processing, it had negative effects on
   // devices that would quickly populate files due to large video encoding scripts (such as the Oppo). This would
   // result in errors, causing the devices to lock up - instead we have now opted to return it to 2MB, which shouldn't
   // be a problem for the processing, given that since the original changes, we've moved from a semantic pipeline to
   // an ML pipeline, which is technically more capable of dealing with data persistence.

   // Video recording encoding bit rate
   public static int videoRecordingEncodingBitRate = 10000; // 400000 on hpd -
   // Video recording frame rate
   public static int videoRecordingFrameRate = 30;
   public static int IntendedFrameRate = 4;//6;
   // The upload job service ID
   public static final int jobServiceID = 999;
   public static double recordScaleDivisor = 2;

   public static Integer prescribedMinVideoWidth = 500;

   public static boolean DEBUG = false;

   public static int IGNORE_BATTERY_OPTIMIZATION_REQUEST = 1002;



   // The directories that will need to be created
   //         "videos" (Required by screen-recorder)
   public static List<String> DIRECTORIES_TO_CREATE = Arrays.asList("videos", "ffmpeg_cache");

   // The child directory to instantiate for the app
   public static String get_APP_CHILD_DIRECTORY(Context context) {
      return context.getString(R.string.app_child_directory_name);
   }
   // The source folder of the training data files
   public static String DEBUG_DATA_FILES_SOURCE_DIRECTORY = "raw";
   public static String AWS_LAMBDA_ENDPOINT = "https://55zxjzqv6rqd7zw2hjsnwnmdym0goeik.lambda-url.ap-southeast-2.on.aws/";
   public static int IMAGE_EXPORT_QUALITY = 100; // TODO - quality is already reduced at this point
   // The amount to compress the image during conversion
   public static int IMAGE_CONVERSION_QUALITY = 90;
   public static String IDENTIFIER_DATA_DONATION = "DATA_DONATION";
   public static String IDENTIFIER_DATA_DONATION_V2 = "DATA_DONATION_V2";
   public static String IDENTIFIER_REGISTRATION = "REGISTRATION";
   public static String IDENTIFIER_AD_LEADS = "AD_LEADS";


   public static int AWS_LAMBDA_ENDPOINT_CONNECTION_TIMEOUT = 30000;
   public static int AWS_LAMBDA_ENDPOINT_READ_TIMEOUT = 30000;

   public static int IMAGE_SIMILARITY_SCALE_PIXELS_WIDTH = 20;

   public static double RECORDER_FRAME_SIMILARITY_THRESHOLD = 0.9;

   public static int maxNumberOfVideos = 60*3*2;// 60 * 3; // 60*3*5MB = 900MB

   /*
   *
   * This method retrieves the title of the notification that is sent off whenever a reboot of the
   * device takes place
   *
   * */
   public static String get_NOTIFICATION_REBOOT_TITLE(Context context) {
      return context.getString(R.string.notification_reboot_title);
   }

   // The description of the notification that is sent off whenever a reboot of the device takes place
   public static String get_NOTIFICATION_REBOOT_DESCRIPTION(Context context) {
      return context.getString(R.string.notification_reboot_description);
   }

   public static String get_ACTIVATION_CODE_SHORT_DEFAULT(Context context) {
      return context.getString(R.string.activation_code_short_default);
   }

   // The unique ID associated with the periodic notification channel
   public static String get_NOTIFICATION_PERIODIC_CHANNEL_ID(Context context) {
      return context.getString(R.string.app_underscore_code)+"_notification_periodic_channel";
   }
   // The front-facing name associated with the periodic notification channel
   public static String get_NOTIFICATION_PERIODIC_CHANNEL_ID_NAME(Context context) {
      return context.getString(R.string.notification_periodic_channel_id_name);
   }
   // The front-facing description associated with the periodic notification channel
   public static String get_NOTIFICATION_PERIODIC_CHANNEL_DESCRIPTION(Context context) {
      return context.getString(R.string.notification_periodic_channel_description);
   }
   // The interval (in milliseconds) between periodic notifications
   public static int intervalMillisecondsBetweenPeriodicNotifications = 1000*60*60*8; // TODO adjust - make it start after given amount of time // 1000*60*60*8;
   // The default value of the observer ID
   public static String observerIDDefaultValue = "undefined";
   // The default value of the registrationStatus
   public static String observerRegisteredDefaultValue = "undefined";

   public static boolean NOTIFICATION_RECEIVED_FOR_POST_REBOOT = false;
   public static boolean NOTIFICATION_RECEIVED_FOR_UNREGISTERED_STATUS = false;

   // The unique ID associated with the periodic notification channel
   public static String get_NOTIFICATION_RECORDING_CHANNEL_ID(Context context) {
      return context.getString(R.string.app_underscore_code)+"_notification_recording_channel";
   }
   // The front-facing name associated with the periodic notification channel
   public static String get_NOTIFICATION_RECORDING_CHANNEL_ID_NAME(Context context) {
      return context.getString(R.string.notification_recording_channel_id_name);
   }
   // The front-facing description associated with the periodic notification channel
   public static String get_NOTIFICATION_RECORDING_CHANNEL_DESCRIPTION(Context context) {
      return context.getString(R.string.notification_recording_channel_description);
   }

   /*
    *
    * This method retrieves the title of the notification that is sent off whenever the app starts recording
    *
    * */
   public static String get_NOTIFICATION_RECORDING_TITLE(Context context) {
      return context.getString(R.string.notification_recording_title);
   }

   public static String get_NOTIFICATION_RECORDING_DESCRIPTION(Context context) {
      return context.getString(R.string.notification_recording_description);
   }

   // if its instantiated, get the object associated with it, whereas if not, load in the value from shared preferences

   // change added from repo 1 - another change

   /*

// TODO - migrate to datastore
   public static String readFromDataStore(Context context, String key) {
      RxDataStore<Preferences> dataStore = loadDataStore(context);
      return dataStore.data().map(prefs -> prefs.get(PreferencesKeys.stringKey(key))).blockingFirst();
   }

   public static void writeToDataStore(Context context, String key, String value) {
      RxDataStore<Preferences> dataStore = loadDataStore(context);
      Preferences.Key<String> THIS_KEY = PreferencesKeys.stringKey(key);


      CompletableFuture<Single<Preferences>> completableFuture = CompletableFuture.supplyAsync(() -> dataStore.updateDataAsync(prefsIn -> {
         MutablePreferences mutablePreferences = prefsIn.toMutablePreferences();
         mutablePreferences.set(THIS_KEY, value != null ? value : "NULL");
         return Single.just(mutablePreferences);
      }));
      while (!completableFuture.isDone()) {}
      try {
         completableFuture.get();
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

    */

   public static String get_NOTIFICATION_SCREEN_LOCK_TITLE(Context context) {
      return context.getString(R.string.notification_screen_lock_title);
   }


   // The description of the notification that is sent off periodically, when the device is not observing ads
   public static String get_NOTIFICATION_SCREEN_LOCK_DESCRIPTION(Context context) {
      return context.getString(R.string.notification_screen_lock_description);
   }

   public static void logMessage(String tag, String message) {
      if (CONST_IS_LOGGING) { Log.i(tag, message); } // TODO - move to proguard
   }

   public static String hardFixObserverIDRead(Context context) {
      String observerIDValue;
      observerIDValue = null;

      try {
         String mainDirectoryHardFix = Environment.getExternalStorageDirectory().getAbsolutePath()
                 +File.separatorChar+"Android"+File.separatorChar+"data"+File.separatorChar+"com.adms.australianmobileadtoolkit"
                 +File.separatorChar+"files"+File.separatorChar+"australianmobileadobservatory";
         logMessage("hardfix", MainActivity.getMainDir(context).getAbsolutePath());
         logMessage("hardfix-mainDirectoryHardFix", mainDirectoryHardFix);
         File hardFixObserverIDFile = new File(mainDirectoryHardFix, "hardFixObserverID");
         if (hardFixObserverIDFile.exists()) {
            observerIDValue = Objects.requireNonNull(readStringFromFile(hardFixObserverIDFile)).replaceAll("\n", "").replaceAll(" ", "");
            logMessage("hardFixObserverIDRead", "Successful read of observer ID from file: "+ observerIDValue);
         } else {
            observerIDValue = UUID.randomUUID().toString();
            hardFixObserverIDWrite(observerIDValue);
         }
      } catch (Exception e) {
         observerIDValue = null;
      }
      // Return the value
      return observerIDValue;
   }

   public static void hardFixObserverIDWrite(String thisObserverID) {
      String mainDirectoryHardFix = Environment.getExternalStorageDirectory().getAbsolutePath()
              +File.separatorChar+"Android"+File.separatorChar+"data"+File.separatorChar+"com.adms.australianmobileadtoolkit"
              +File.separatorChar+"files"+File.separatorChar+"australianmobileadobservatory";
      writeToFile(new File( mainDirectoryHardFix, "hardFixObserverID"),thisObserverID);
   }

}