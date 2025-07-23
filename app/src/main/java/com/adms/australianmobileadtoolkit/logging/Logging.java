package com.adms.australianmobileadtoolkit.logging;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.POWER_SERVICE;
import static com.adms.australianmobileadtoolkit.Common.readStringFromFile;
import static com.adms.australianmobileadtoolkit.Common.safeSublist;
import static com.adms.australianmobileadtoolkit.HTTP.HTTPRequest;
import static com.adms.australianmobileadtoolkit.MainActivity.getMainDir;
import static com.adms.australianmobileadtoolkit.appSettings.hardFixObserverIDRead;
import static com.adms.australianmobileadtoolkit.appSettings.logMessage;
import static com.adms.australianmobileadtoolkit.interpreter.AccessibilityServiceManager.isAccessibilityServiceEnabled;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.createDirectory;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.PowerManager;
import android.util.Base64;

import com.adms.australianmobileadtoolkit.JSONXObject;
import com.adms.australianmobileadtoolkit.interpreter.AccessibilityService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class Logging {

    private static String TAG = "Logging";
    private static Integer maxLogGroupInterval = (5 * 60);

    // A log is a single line describing various important aspects of the app at a given point in time
    //
    // Logs are committed to log groups (a single log group corresponds to a single file).
    //
    // When a log group reaches a certain size, to avoid slowing down intermittent file uploads to the cloud,
    // we signal that it is ready for dispatch and begin a new log group.


    public static File logGroupDirectory(Context context) {
        File logGroupDirectory = new File(getMainDir(context), "logs");
        if (!logGroupDirectory.isDirectory()) {
            createDirectory(logGroupDirectory, false);
        }
        return logGroupDirectory;
    }

    public static boolean logGroupIsExpired(File logGroup) {
        Integer logGroupTime = Integer.parseInt(logGroup.getName().replace(".log", "").split("-")[1]);
        Integer currentTime = Integer.parseInt(logValueTimeInSeconds());
        return (Math.abs(logGroupTime - currentTime) > maxLogGroupInterval);
    }

    /*
    *
    * This function simply appends to a file (creating it in the process if it doesn't exist
    *
    * */
    public static void appendLineToFile(File file, String line) {
        try (FileOutputStream fos = new FileOutputStream(file, true);
             OutputStreamWriter osw = new OutputStreamWriter(fos);
             BufferedWriter writer = new BufferedWriter(osw)) {
            writer.write(line);
            writer.newLine();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String logValueTimeInSeconds() {
        return String.valueOf(System.currentTimeMillis() / 1000);
    }

    public static String logValueDeviceOrientation(Context context) {
        switch (context.getResources().getConfiguration().orientation) {
            case 0 : return "U";
            case 1 : return "P";
            case 2 : return "L";
            default : return "U";
        }
    }

    /*
    *
    * This function determines the current log group to append to, and creates a log group if it does not yet
    * exist
    *
    * */
    public static File appendableLogGroup(Context context) {
        // Firstly declare and create the 'logs' directory (if it is not yet created).
        File logGroupDirectory = logGroupDirectory(context);

        //
        List<File> filesList = new ArrayList<>();
        File[] files = logGroupDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    filesList.add(file);
                }
            }
        }
        // Get the first appendable log group (if such a group exists)
        File appendableLogGroup = null;
        List<File> appendableLogGroups = filesList.stream().filter(x -> ((!x.getName().contains("-rfd")) && (!logGroupIsExpired(x)))).collect(Collectors.toList());
        if (appendableLogGroups.size() > 0) {
            appendableLogGroup = appendableLogGroups.get(0);
        }
        if (appendableLogGroup == null) {
            String tentativeLogGroupName = "logGroup-" + logValueTimeInSeconds() + ".log";
            appendableLogGroup = new File(logGroupDirectory, tentativeLogGroupName);
        }

        return appendableLogGroup;
    }

    public static List<File> getFilesInDirectory(File thisDirectory) {
        List<File> filesList = new ArrayList<>();
        File[] files = thisDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    filesList.add(file);
                }
            }
        }
        return filesList;
    }

    public static List<File> getSubDirectories(File thisDirectory) {
        List<File> filesList = new ArrayList<>();
        if (thisDirectory.isDirectory()) {
            File[] files = thisDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        filesList.add(file);
                    }
                }
            }
        }
        return filesList;
    }

    public static String getSubDirectoriesN(Context context, String thisDirectoryName) {
        return String.valueOf(getSubDirectories(new File(getMainDir(context), thisDirectoryName)).size());
    }
    public static String getAccessibilityServicesEnabled(Context context) {
        return (isAccessibilityServiceEnabled(context, AccessibilityService.class)) ? "T" : "F";
    }

    public static String getBatteryOptimizationRelaxed(Context context) {
        String packageName = context.getPackageName();
        PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            return "T";
        } else {
            return "F";
        }
    }

    public static String getBackgroundProcessingEnabled(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        switch (cm.getRestrictBackgroundStatus()) {
            case ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED : return "F";
            case ConnectivityManager.RESTRICT_BACKGROUND_STATUS_WHITELISTED : return "W";
            case ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED : return "R";
        }
        return "U";
    }

    /*
    *
    * This function adds a log to the current log group
    *
    * */
    public static void addALog(Context context, String logCase) {
        File appendableLogGroup = appendableLogGroup(context);
        List<String> logFields = new ArrayList<>();

        // The following values are applied to the log...
        logFields.add(UUID.randomUUID().toString());
        logFields.add(logValueTimeInSeconds()); // Current time
        logFields.add(logCase); // Log case
        logFields.add(logValueDeviceOrientation(context)); // Device orientation
        logFields.add(getAccessibilityServicesEnabled(context)); // Accessibility Services Enabled?
        logFields.add(getBatteryOptimizationRelaxed(context)); // Battery Optimization Relaxed?
        logFields.add(getBackgroundProcessingEnabled(context)); // Background Processing Enabled?
        logFields.add(getSubDirectoriesN(context, "analysis")); // No. of active analyses
        logFields.add(getSubDirectoriesN(context, "dispatch")); // No. of currently held dispatches
        logFields.add(getSubDirectoriesN(context, "videos")); // No. of currently held screen-recordings
        appendLineToFile(appendableLogGroup, String.join(",", logFields));


    }

    /*
    *
    * This method identifies all logs that are currently ready for dispatch, and firstly flags them - it then attempts to
    * dispatch as many as possible
    *
    * */
    public static boolean dispatchLogRoutine(Context context) {
        boolean success = true;
        logMessage(TAG, "Beginning 'dispatch log' routine...");
        Integer maxLogGroupsToDispatch = 10;
        File logGroupDirectory = logGroupDirectory(context);
        List<File> logGroups = getFilesInDirectory(logGroupDirectory);
        List<File> appendableLogGroups = logGroups.stream().filter(x -> (!x.getName().contains("-rfd"))).collect(Collectors.toList());

        // Firstly prepare the appendable log groups that have expired
        for (File thisLogGroup : appendableLogGroups) {
            if (logGroupIsExpired(thisLogGroup)) {
                thisLogGroup.renameTo(new File(thisLogGroup.getParentFile(), thisLogGroup.getName().replace(".log", "") + "-rfd.log" ));
            }
        }

        // Retrieve the log groups a second time (after the modifications are made...
        logGroups = getFilesInDirectory(logGroupDirectory);
        List<File> dispatchableLogGroups = safeSublist(logGroups.stream().filter(x -> (x.getName().contains("-rfd"))).collect(Collectors.toList()),0,maxLogGroupsToDispatch);
        String thisParticipantUUID = hardFixObserverIDRead(context);
        for (File thisLogGroup : dispatchableLogGroups) {
            boolean successfulDispatch = HTTPRequest(new JSONXObject()
                            .set("action", "LOG")
                            .set("observer_id",thisParticipantUUID)
                            .set("content", Base64.encodeToString(readStringFromFile(thisLogGroup).getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP))
                            .internalJSONObject
            );
            if (!successfulDispatch) {
                success = false;
            }
            logMessage(TAG, "Dispatching log: "+thisLogGroup.getName());
            thisLogGroup.delete();
        }
        return success;
    }
}
