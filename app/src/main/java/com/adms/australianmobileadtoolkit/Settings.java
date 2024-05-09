package com.adms.australianmobileadtoolkit;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.pm.ApplicationInfo;

import java.util.Arrays;
import java.util.List;

public class Settings {
   // The extra result code associated with the intent of the recording service
   public static final String RECORD_SERVICE_EXTRA_RESULT_CODE = "AustralianMobileAdObservatoryExtraResultCode";
   public static final String RECORD_SERVICE_EXTRA_DATA = "data";
   // The ID of the notification associated with the recording service
   // (the value has no actual bearing on the functionality, although don't set it to zero:
   // https://developer.android.com/guide/components/foreground-services#:~:text=startForeground(ONGOING_NOTIFICATION_ID%2C%20notification)%3B)
   public static final int RECORD_SERVICE_ONGOING_NOTIFICATION_ID = 1;
   // Maximum file size for video recordings (5MB)
   public static int videoRecordingMaximumFileSize = 2000000;
   // Video recording encoding bit rate
   public static int videoRecordingEncodingBitRate = 10000; // 400000 on hpd -
   // Video recording frame rate
   public static int videoRecordingFrameRate = 30;
   public static int IntendedFrameRate = 4;//6;
   // The upload job service ID
   public static final int jobServiceID = 999;
   public static double recordScaleDivisor = 2;

   public static boolean DEBUG = false;

   // The directories that will need to be created
   //         "videos" (Required by screen-recorder)
   public static List<String> DIRECTORIES_TO_CREATE = (DEBUG) ? Arrays.asList("videos", "debug", "ffmpeg_cache") : Arrays.asList("videos", "ffmpeg_cache");

   // The child directory to instantiate for the app
   public static String APP_CHILD_DIRECTORY = "australianmobileadobservatory";
   // The source folder of the training data files
   public static String DEBUG_DATA_FILES_SOURCE_DIRECTORY = "raw";
   public static String AWS_LAMBDA_ENDPOINT = "https://nmzoodzqpiuok4adbqvldcog4y0mlumv.lambda-url.us-east-2.on.aws/";
   public static int IMAGE_EXPORT_QUALITY = 100; // TODO - quality is already reduced at this point
   // The amount to compress the image during conversion
   public static int IMAGE_CONVERSION_QUALITY = 90;
   public static String IDENTIFIER_DATA_DONATION = "DATA_DONATION";
   public static String IDENTIFIER_REGISTRATION = "REGISTRATION";
   public static String IDENTIFIER_AD_LEADS = "AD_LEADS";

   public static String DEBUG_TARGET_VIDEO = "demonstration_test.mp4";//"debug_new_2.mp4";//"debug2.mp4"; //"debug3.mp4";//"debug_new_3.mp4";//

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
      return "Ad observations stop after a reboot";
   }

   // The description of the notification that is sent off whenever a reboot of the device takes place
   public static String NOTIFICATION_REBOOT_DESCRIPTION =
         "Tap to start observing ads again";

   /*
    *
    * This method retrieves the title of the notification that is sent off periodically, when the device
    * is not observing ads
    *
    * */
   public static String get_NOTIFICATION_PERIODIC_TITLE(Context context) {
      return "Ads are not being observed";
   }

   public static String get_NOTIFICATION_PERIODIC_TITLE_UNREGISTERED(Context context) {
      return "The app is not registered";
   }

   // The description of the notification that is sent off periodically, when the device is not observing ads
   public static String NOTIFICATION_PERIODIC_DESCRIPTION =
           "Tap to start observing ads";

   public static String DEMOGRAPHIC_FAILSAFE_STRING = "One or more fields need to be entered before you can continue";
   public static String DEMOGRAPHIC_FAILSAFE_COUNTRY = "Research participation is not available to users outside Australia at this point.";

   public static String ACTIVATION_CODE_NOT_APPLICABLE_STRING = "N/A";
   public static String ACTIVATION_CODE_PREFIX_STRING = " My Activation Code: ";
   public static String NOTIFICATION_PERIODIC_DESCRIPTION_UNREGISTERED =
           "Tap to register the app";
   // The unique ID associated with the periodic notification channel
   public static String NOTIFICATION_PERIODIC_CHANNEL_ID = "adms_mobile_ad_observatory_notification_periodic_channel";
   // The front-facing name associated with the periodic notification channel
   public static String NOTIFICATION_PERIODIC_CHANNEL_ID_NAME = "Inactivity Reminders";
   // The front-facing description associated with the periodic notification channel
   public static String NOTIFICATION_PERIODIC_CHANNEL_DESCRIPTION = "This app is designed to stop collecting ads whenever your device restarts. When this happens, we'll need your permission to re-enable it";
   // The interval (in milliseconds) between periodic notifications
   public static int intervalMillisecondsBetweenPeriodicNotifications = 1000*60*60*8; // TODO adjust - make it start after given amount of time // 1000*60*60*8;
   // The default value of the observer ID
   public static String SHARED_PREFERENCE_OBSERVER_ID_DEFAULT_VALUE = "undefined";
   // The default value of the registrationStatus
   public static String SHARED_PREFERENCE_REGISTERED_DEFAULT_VALUE = "undefined";

   // The unique ID associated with the periodic notification channel
   public static String NOTIFICATION_RECORDING_CHANNEL_ID = "adms_mobile_ad_observatory_notification_recording_channel";
   // The front-facing name associated with the periodic notification channel
   public static String NOTIFICATION_RECORDING_CHANNEL_ID_NAME = "Recording Reminders";
   // The front-facing description associated with the periodic notification channel
   public static String NOTIFICATION_RECORDING_CHANNEL_DESCRIPTION = "Be informed of when our app has started collecting Facebook ads";

   /*
    *
    * This method retrieves the title of the notification that is sent off whenever the app starts recording
    *
    * */
   public static String get_NOTIFICATION_RECORDING_TITLE(Context context) {
      return "Ads are being observed";
   }

   public static String NOTIFICATION_RECORDING_DESCRIPTION =
         "The app is now collecting Facebook ads that have been served to you";

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
}