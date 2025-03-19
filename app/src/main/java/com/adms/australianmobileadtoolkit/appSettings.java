package com.adms.australianmobileadtoolkit;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;

import java.util.Arrays;
import java.util.List;

public class appSettings {
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
   public static List<String> DIRECTORIES_TO_CREATE = (DEBUG) ? Arrays.asList("videos", "debug", "ffmpeg_cache", "logs", "serializations") : Arrays.asList("videos", "ffmpeg_cache", "logs", "serializations");

   // The child directory to instantiate for the app
   public static String get_APP_CHILD_DIRECTORY(Context context) {
      return context.getString(R.string.app_child_directory_name);
   }
   // The source folder of the training data files
   public static String DEBUG_DATA_FILES_SOURCE_DIRECTORY = "raw";
   public static String AWS_LAMBDA_ENDPOINT = "https://nmzoodzqpiuok4adbqvldcog4y0mlumv.lambda-url.us-east-2.on.aws/";
   public static int IMAGE_EXPORT_QUALITY = 100; // TODO - quality is already reduced at this point
   // The amount to compress the image during conversion
   public static int IMAGE_CONVERSION_QUALITY = 90;
   public static String IDENTIFIER_DATA_DONATION = "DATA_DONATION";
   public static String IDENTIFIER_DATA_DONATION_V2 = "DATA_DONATION_V2";
   public static String IDENTIFIER_REGISTRATION = "REGISTRATION";
   public static String IDENTIFIER_AD_LEADS = "AD_LEADS";

   public static String DEBUG_TARGET_VIDEO = "light_5_long.mp4";//"debug_new_2.mp4";//"debug2.mp4"; //"debug3.mp4";//"debug_new_3.mp4";//

   public static int AWS_LAMBDA_ENDPOINT_CONNECTION_TIMEOUT = 30000;
   public static int AWS_LAMBDA_ENDPOINT_READ_TIMEOUT = 30000;

   public static int IMAGE_SIMILARITY_SCALE_PIXELS_WIDTH = 20;

   public static double RECORDER_FRAME_SIMILARITY_THRESHOLD = 0.9;

   public static int maxNumberOfVideos = 60*3;// 60 * 3; // 60*3*5MB = 900MB

   /*
    *
    * This method retrieves the name of the application
    *
    * */
   public static String getApplicationName(Context context) {
      ApplicationInfo applicationInfo = context.getApplicationInfo();
      int stringId = applicationInfo.labelRes;
      return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);
   }

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

   /*
    *
    * This method retrieves the title of the notification that is sent off periodically, when the device
    * is not observing ads
    *
    * */
   public static String get_NOTIFICATION_PERIODIC_TITLE(Context context) {
      return context.getString(R.string.notification_periodic_title);
   }

   public static String get_NOTIFICATION_PERIODIC_TITLE_UNREGISTERED(Context context) {
      return context.getString(R.string.notification_periodic_title_unregistered);
   }

   // The description of the notification that is sent off periodically, when the device is not observing ads
   public static String get_NOTIFICATION_PERIODIC_DESCRIPTION(Context context) {
      return context.getString(R.string.notification_periodic_description);
   }

   public static String get_DEMOGRAPHIC_FAILSAFE_STRING(Context context) {
      return context.getString(R.string.demographic_failsafe_string);
   };
   public static String get_DEMOGRAPHIC_FAILSAFE_COUNTRY(Context context) {
      return context.getString(R.string.demographic_failsafe_country);
   };

   public static String get_ACTIVATION_CODE_NOT_APPLICABLE_STRING(Context context) {
      return context.getString(R.string.activation_code_not_applicable);
   }
   public static String get_ACTIVATION_CODE_PREFIX_STRING(Context context) {
      return context.getString(R.string.activation_code_prefix);
   }

   public static String get_ACTIVATION_CODE_SHORT_DEFAULT(Context context) {
      return context.getString(R.string.activation_code_short_default);
   }

   public static String get_ACTIVATION_SHORT_CODE_PREFIX_STRING(Context context) {
      return context.getString(R.string.activation_code_short_prefix);
   }
   public static String get_NOTIFICATION_PERIODIC_DESCRIPTION_UNREGISTERED(Context context) {
      return context.getString(R.string.notification_periodic_description_unregistered);
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
   public static String SHARED_PREFERENCE_OBSERVER_ID_DEFAULT_VALUE = "undefined";
   // The default value of the registrationStatus
   public static String SHARED_PREFERENCE_REGISTERED_DEFAULT_VALUE = "undefined";

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

   /*
   *
   * This method retrieves persistent shared preference values
   *
   * */
   public static String sharedPreferenceGet(Context context, String name, String defaultValue) {
      SharedPreferences preferences = context.getSharedPreferences(getApplicationName(context), Context.MODE_MULTI_PROCESS);
      return preferences.getString(name, defaultValue);
   }

   /*
    *
    * This method assigns persistent shared preference values
    *
    * */
   public static void sharedPreferencePut(Context context, String name, String value) {
      SharedPreferences preferences = context.getSharedPreferences(getApplicationName(context), Context.MODE_MULTI_PROCESS);
      SharedPreferences.Editor editor = preferences.edit();
      editor.putString(name, value);
      editor.apply();
   }

   // if its instantiated, get the object associated with it, whereas if not, load in the value from shared preferences

   // change added from repo 1 - another change



   public static String get_NOTIFICATION_SCREEN_LOCK_TITLE(Context context) {
      return context.getString(R.string.notification_screen_lock_title);
   }


   // The description of the notification that is sent off periodically, when the device is not observing ads
   public static String get_NOTIFICATION_SCREEN_LOCK_DESCRIPTION(Context context) {
      return context.getString(R.string.notification_screen_lock_description);
   }

}