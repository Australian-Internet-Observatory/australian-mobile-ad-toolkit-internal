package com.adms.australianmobileadtoolkit;
import static com.adms.australianmobileadtoolkit.appSettings.get_NOTIFICATION_PERIODIC_CHANNEL_DESCRIPTION;
import static com.adms.australianmobileadtoolkit.appSettings.get_NOTIFICATION_PERIODIC_CHANNEL_ID;
import static com.adms.australianmobileadtoolkit.appSettings.get_NOTIFICATION_PERIODIC_CHANNEL_ID_NAME;
import static com.adms.australianmobileadtoolkit.appSettings.get_NOTIFICATION_PERIODIC_DESCRIPTION;
import static com.adms.australianmobileadtoolkit.appSettings.get_NOTIFICATION_PERIODIC_DESCRIPTION_UNREGISTERED;
import static com.adms.australianmobileadtoolkit.appSettings.get_NOTIFICATION_REBOOT_DESCRIPTION;
import static com.adms.australianmobileadtoolkit.appSettings.SHARED_PREFERENCE_REGISTERED_DEFAULT_VALUE;
import static com.adms.australianmobileadtoolkit.appSettings.get_NOTIFICATION_PERIODIC_TITLE;
import static com.adms.australianmobileadtoolkit.appSettings.get_NOTIFICATION_PERIODIC_TITLE_UNREGISTERED;
import static com.adms.australianmobileadtoolkit.appSettings.get_NOTIFICATION_REBOOT_TITLE;
import static com.adms.australianmobileadtoolkit.appSettings.intervalMillisecondsBetweenPeriodicNotifications;
import static com.adms.australianmobileadtoolkit.appSettings.sharedPreferenceGet;
import static com.adms.australianmobileadtoolkit.appSettings.sharedPreferencePut;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.icu.util.Calendar;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

public class InactivityReceiver extends BroadcastReceiver {

   private static String TAG = "InactivityReceiver";

   private static int DEFAULT_NOTIFICATION_RANDOM_ID_VALUE = -1;

   public static int generateRandomPositiveNumber() {
      Random r = new Random();
      int randomNumberGeneratorBound = 999999999;
      return r.nextInt(randomNumberGeneratorBound);
   }

   /*
   *
   * This function is responsible for setting the internal alarm associated with ensuring that periodic
   * notifications are regularly sent to the user when they are not running the app in the background
   *
   * */
   public static void setPeriodicNotifications(Context context) {
      if (sharedPreferenceGet(context, "SHARED_PREFERENCE_PERIODIC_NOTIFICATIONS_SET", "false").equals("false")) {
         System.out.println( "Attempted to set periodic notifications: success");
         Intent notificationIntent = new Intent(context, InactivityReceiver.class); // TODO - checked for API migration
         notificationIntent.putExtra("INTENT_ACTION", "PERIODIC_NOTIFICATION");
         final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
               notificationIntent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
         if (pendingIntent == null) {
            PendingIntent pending = PendingIntent.getBroadcast(context, 0, notificationIntent,
                  PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            // Set the time of both the initial (and recurring) periodic notifications
            // Note that the first instance will occur intervalMillisecondsBetweenPeriodicNotifications*2
            // milliseconds into the future
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(System.currentTimeMillis() + intervalMillisecondsBetweenPeriodicNotifications);
            ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).setRepeating(
                  AlarmManager.RTC, Calendar.getInstance().getTime().toInstant().toEpochMilli(),
                  intervalMillisecondsBetweenPeriodicNotifications, pending);
         }
         sharedPreferencePut(context, "SHARED_PREFERENCE_PERIODIC_NOTIFICATIONS_SET", "true");
      } else {
         System.out.println( "Attempted to set periodic notifications: already set");
      }
   }

   private static boolean isDarkTheme(Context context) {
      int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
      return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
   }

   private static PendingIntent constructNotificationScreenRecorderPendingIntent(Context context, String intentAction) {
      Intent intent = new Intent(context, MainActivity.class);
      intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
      intent.putExtra("INTENT_ACTION",intentAction);
      return PendingIntent.getActivity(context,0, intent,
              PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
   }

   /*
   *
   * This method is responsible for handling consistent stylisation of notifications that are sent out
   * through the app
   *
   * */
   public static NotificationCompat.Builder constructNotification(Context context, String channelID, String title, String text, NotificationCompat.Action action) {

      NotificationCompat.Builder thisNotificationBuilder = new NotificationCompat.Builder(context, channelID)
            .setSmallIcon(R.mipmap.ic_stat_adaptive) // entirely white
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setColor(ContextCompat.getColor(context, R.color.colorNotificationLightMode))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

      if (action != null) {
         thisNotificationBuilder = thisNotificationBuilder.addAction(action);
      }
      if (isDarkTheme(context)) {
         thisNotificationBuilder.setColor(ContextCompat.getColor(context, R.color.colorNotificationDarkMode));
      }
      /*
      switch (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
         case Configuration.UI_MODE_NIGHT_YES:
            thisNotificationBuilder.setColor(ContextCompat.getColor(context, R.color.colorNotificationDarkMode));
            break;
         case Configuration.UI_MODE_NIGHT_NO:
            thisNotificationBuilder.setColor(ContextCompat.getColor(context, R.color.colorNotificationLightMode));
            break;
         case Configuration.UI_MODE_NIGHT_UNDEFINED:
            thisNotificationBuilder.setColor(ContextCompat.getColor(context, R.color.colorNotificationLightMode));
            break;
      }*/
      return thisNotificationBuilder;
   }

   /*
    *
    * This method is responsible for handling intermittent build aspects of the notification construction
    *
    * */
   public static Notification constructNotificationForward(Context context, NotificationCompat.Builder builder) {
      NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
      /*if (ActivityCompat.checkSelfPermission(context,
            android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
         // We don't request permissions here - this is for the user to do when they use the app.
         System.out.println( "Permission not registering");
         return null;
      }*/ // This has been commented out because it can be auto-handled by general notification behaviour
      Notification builtNotification = builder.build();
      return builtNotification;
   }



   /*
   *
   * This method is responsible for sending notifications
   *
   * */
   public static void sendNotification(Context context, NotificationCompat.Builder builder, String notificationIDCase) {
      NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
      if (ActivityCompat.checkSelfPermission(context,
            android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
         // We don't request permissions here - this is for the user to do when they use the app.
         return;
      }
      Notification builtNotification = builder.build();

      // Get the current random notification ID (if it exists)
      int thisCurrentNotificationRandomID = Integer.parseInt(
                                               sharedPreferenceGet(context, notificationIDCase,
                                                   String.valueOf(DEFAULT_NOTIFICATION_RANDOM_ID_VALUE)));
      // And generate a new random notification ID (that will replace the current one)
      int thisNewNotificationRandomID = generateRandomPositiveNumber();
      // If there are no past notifications to cancel
      // (as indicated by the default value in the current random notification ID)...
      if (thisCurrentNotificationRandomID != DEFAULT_NOTIFICATION_RANDOM_ID_VALUE) {
         // Or if there are past notifications to cancel, cancel them...
         cancelNotificationOfID(context, thisCurrentNotificationRandomID, notificationIDCase);
      }
      sharedPreferencePut(context, notificationIDCase, String.valueOf(thisNewNotificationRandomID));
      // Irrespectively, send off the current notification (with the newly defined
      // random notification ID (that will be cancelled upon re-initiation in future)
      Log.i("randomNotificationID", "Sending new "+notificationIDCase+" notification of ID: " + String.valueOf(thisNewNotificationRandomID));
      notificationManager.notify(thisNewNotificationRandomID, builtNotification);
   }

   /*
   *
   * This method generates the notification channel, on which reminders to re-enable the app are
   * broadcasted - the channel can be re-declared repeatedly without any side effects, but MUST
   * be instantiated before any notifications are sent off to allow successful transmission.
   * It is thus instantiated both within this class's onRecieve method, and within the onCreate
   * method of the MainActivity class.
   *
   * */
   public static void generateNotificationChannel(Context context, String channelID, String channelName, String channelDescription) {
      // Generate the notification channel
      NotificationChannel channel = new NotificationChannel(
            channelID, channelName, NotificationManager.IMPORTANCE_DEFAULT);
      channel.setDescription(channelDescription);
      context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
   }

   /*
   *
   * This method determines whether the user should be notified of the app's inactivity
   *
   * */
   private boolean shouldUserBeNotifiedAboutInactivity(Context context) {
      return (!sharedPreferenceGet(context, "RECORDING_STATUS", "false").equals("true"));
   }

   /*
   *
   * This function cancels all notifications of a particular ID
   *
   * */
   public static void cancelNotificationOfID(Context context, int notificationID, String notificationCase) {
      Log.i("randomNotificationID", "Cancelling "+notificationCase+" notification of ID: " + String.valueOf(notificationID));
      NotificationManager nMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
      nMgr.cancel( notificationID);
   }

   public static void cancelAllInactivityNotifications(Context context) {
      // For each notification case...
      for (String notificationCase :
              Arrays.asList("NOTIFICATION_PERIODIC_ID_CASE", "NOTIFICATION_REBOOT_ID_CASE")) {
         // Retrieve the current random notification ID
         int currentNotificationRandomIDPeriodic = Integer.parseInt(
                 sharedPreferenceGet(context, notificationCase,
                         String.valueOf(DEFAULT_NOTIFICATION_RANDOM_ID_VALUE)));
         // If there is a notification to be cancelled
         // (by indication that the notification ID is not its default value)
         if (currentNotificationRandomIDPeriodic != DEFAULT_NOTIFICATION_RANDOM_ID_VALUE) {
            // Cancel it
            cancelNotificationOfID(context, currentNotificationRandomIDPeriodic, notificationCase);
         }
      }
   }

   public static void sendPeriodicNotification(Context context, boolean THIS_REGISTRATION_STATUS) {
      NotificationCompat.Builder builderPeriodicNotification;
      // Send the periodic notification to inform the user that the app is not observing ads
      if (THIS_REGISTRATION_STATUS) {
         builderPeriodicNotification = constructNotification(context,
                 get_NOTIFICATION_PERIODIC_CHANNEL_ID(context),
                 get_NOTIFICATION_PERIODIC_TITLE(context),
                 get_NOTIFICATION_PERIODIC_DESCRIPTION(context),null)
                 .setContentIntent(constructNotificationScreenRecorderPendingIntent(context, "TURN_ON_SCREEN_RECORDER"));
      } else {
         builderPeriodicNotification = constructNotification(context,
                 get_NOTIFICATION_PERIODIC_CHANNEL_ID(context),
                 get_NOTIFICATION_PERIODIC_TITLE_UNREGISTERED(context),
                 get_NOTIFICATION_PERIODIC_DESCRIPTION_UNREGISTERED(context),null)
                 .setContentIntent(constructNotificationScreenRecorderPendingIntent(context, "REGISTER"));

      }
      sendNotification(context, builderPeriodicNotification, "NOTIFICATION_PERIODIC_ID_CASE");
   }

   public static void sendRebootNotification(Context context) {
      NotificationCompat.Builder builderRebootNotification = constructNotification(context,
              get_NOTIFICATION_PERIODIC_CHANNEL_ID(context),
              get_NOTIFICATION_REBOOT_TITLE(context),
              get_NOTIFICATION_REBOOT_DESCRIPTION(context), null)
              .setContentIntent(constructNotificationScreenRecorderPendingIntent(context, "TURN_ON_SCREEN_RECORDER"));
      sendNotification(context, builderRebootNotification, "NOTIFICATION_REBOOT_ID_CASE");
   }

   /*
   *
   * This method receives intents in two cases:
   *     1. The device has rebooted: In this case, the app automatically stops recording because it
   *        no longer has permission to record the screen (note that it is impossible to persist
   *        permissions between boots, as the intents that carry the permissions are serialised using
   *        Parcel, which facilitates information transfer that can't be read off disk. Upon reboot,
   *        we designate three actions: the first action is that a perpetual alarm is set to fire off
   *        periodic notifications to ensure that the user is aware when the app is not recording;
   *        the second action is that a one-time notification is also sent to the user to let them know
   *        that the app has stopped recording; the third action is that the user is directed to the
   *        app (provided that certain device-specific conservation features are met).
   *     2. The periodic alarm has been fired: In this case, the aforementioned periodic
   *        notification is sent to the user.
   *
   * */
   @Override
   public void onReceive(Context context, Intent intent) {
      boolean THIS_REGISTRATION_STATUS = (!Objects.equals(sharedPreferenceGet(
              context, "SHARED_PREFERENCE_REGISTERED", SHARED_PREFERENCE_REGISTERED_DEFAULT_VALUE), SHARED_PREFERENCE_REGISTERED_DEFAULT_VALUE));

      // Set the periodic alarm that is called to determine if the software is observing ads or not
      // We place it for any received event, as we try to maximise the instances in which it is called
      setPeriodicNotifications(context);
      try {
         // Attempt to generate the notification channel
         generateNotificationChannel(context, get_NOTIFICATION_PERIODIC_CHANNEL_ID(context),
               get_NOTIFICATION_PERIODIC_CHANNEL_ID_NAME(context), get_NOTIFICATION_PERIODIC_CHANNEL_DESCRIPTION(context));
         // If the device has rebooted:
         if ((intent != null) && (intent.getAction() != null) && (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))) {
            System.out.println( "'SCREEN_OFF_DURING_RECORDING' and 'RECORDING_STATUS' variables have been set back to 'false'");
            // Reset the 'SCREEN_OFF_DURING_RECORDING' and 'RECORDING_STATUS' variables
            sharedPreferencePut(context, "SCREEN_OFF_DURING_RECORDING", "false");
            Log.i("RECORDING_STATUS", "Setting RECORDING_STATUS to false - 1");
            sharedPreferencePut(context, "RECORDING_STATUS", "false");
            // Reset the periodic notifications indicator (so that it isn't doubly set)
            sharedPreferencePut(context, "SHARED_PREFERENCE_PERIODIC_NOTIFICATIONS_SET", "false");
            // If the app is not recording after a reboot...
            if (shouldUserBeNotifiedAboutInactivity(context)) {
               // Attempt to start the app, to the main screen with a directive
               Intent i = new Intent(context, MainActivity.class);  // TODO - checked for API migration
               i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
               i.setAction(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
               context.startActivity(i);
               if (THIS_REGISTRATION_STATUS) {
                  // Send the reboot notification informing the user that the app is not observing ads
                  sendRebootNotification(context);
               } else {
                  // Send a periodic notification instead - as the user is not logged in
                  sendPeriodicNotification(context, THIS_REGISTRATION_STATUS);
               }

            }
         } else
         // If the periodic alarm has been set off:
         if ((intent != null) && (intent.hasExtra("INTENT_ACTION"))
            && (intent.getStringExtra("INTENT_ACTION").equals("PERIODIC_NOTIFICATION"))) {
               System.out.println( "Periodic inactivity notification intent has been received");
               // If the app is not recording periodically...
               if (shouldUserBeNotifiedAboutInactivity(context)) {
                  System.out.println( "Periodic inactivity notification has been fired");
                  // Check the registration

                  // Send the periodic notification to inform the user that the app is not observing ads
                  sendPeriodicNotification(context, THIS_REGISTRATION_STATUS);
               } else {
                  System.out.println( "Periodic inactivity notification has been rejected - the screen recording is running");
               }
         } else
         // If the screen recording has started
         if ((intent != null) && (intent.hasExtra("INTENT_ACTION"))
            && (intent.getStringExtra("INTENT_ACTION").equals("RECORDING_HAS_STARTED"))) {
               System.out.println( "RECORDING_HAS_STARTED");
               MainActivity.safelySetToggleInViewModel(true);
            // Cancel all inactivity notifications
            cancelAllInactivityNotifications(context);
            Log.i("RECORDING_STATUS", "Setting RECORDING_STATUS to true - 1");
               sharedPreferencePut(context, "RECORDING_STATUS", "true");
         } else
         // If the screen recording has stopped
         if ((intent != null) && (intent.hasExtra("INTENT_ACTION"))
            && (intent.getStringExtra("INTENT_ACTION").equals("RECORDING_HAS_STOPPED"))) {
               System.out.println( "RECORDING_HAS_STOPPED");
               // If the recording stops after a registered screen off event
               if (sharedPreferenceGet(context, "SCREEN_OFF_DURING_RECORDING", "false").equals("true")) {
                  // Do nothing, as we know that the screen is off during a recording session
               } else {
                  MainActivity.safelySetToggleInViewModel(false);
                  Log.i("RECORDING_STATUS", "Setting RECORDING_STATUS to false - 2");
                  sharedPreferencePut(context, "RECORDING_STATUS", "false");
               }
         } else
         // If the screen is on
         if ((intent != null) && (intent.hasExtra("INTENT_ACTION"))
            && (intent.getStringExtra("INTENT_ACTION").equals("SCREEN_IS_ON"))) {
               System.out.println( "SCREEN_IS_ON");
               // By extension of that the screen is on, the screen being off during a recording is falsified
               sharedPreferencePut(context, "SCREEN_OFF_DURING_RECORDING", "false");

         } else
            // If the screen is off
            if ((intent != null) && (intent.hasExtra("INTENT_ACTION"))
                    && (intent.getStringExtra("INTENT_ACTION").equals("SCREEN_IS_OFF"))) {
               System.out.println( "SCREEN_IS_OFF");
               // If the screen switches off during a recording
               if (sharedPreferenceGet(context, "RECORDING_STATUS", "false").equals("true")) {
                  sharedPreferencePut(context, "SCREEN_OFF_DURING_RECORDING", "true");
               }
         }
      } catch (Exception e) {
         // Do nothing
         Log.i("onReceive has failed: ", String.valueOf(e));
      }
   }
}


