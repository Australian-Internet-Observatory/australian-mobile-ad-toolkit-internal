package com.adms.australianmobileadtoolkit;

import static com.adms.australianmobileadtoolkit.Common.filePath;
import static com.adms.australianmobileadtoolkit.MainActivity.mainDir;

import static java.util.Arrays.asList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class Debugger {

   static void deleteRecursive(File fileOrDirectory) {
      if (fileOrDirectory.isDirectory())
         for (File child : fileOrDirectory.listFiles())
            deleteRecursive(child);

      fileOrDirectory.delete();
   }

   static String TAG = "Debugger";
   /*
    *
    * This method copies across the debug data
    *
    * */
   public static void copyDebugData(Context context) {
      File debugDirName = filePath(asList(mainDir.getAbsolutePath(), "debug"));
      if (debugDirName.exists()) {deleteRecursive(debugDirName); debugDirName.mkdirs(); }

      // The source folder of the debug data files
      String debugDataFilesSourceDir = appSettings.DEBUG_DATA_FILES_SOURCE_DIRECTORY;
      // Index the debug files, and generate file IDs for each of them
      List<Integer> fileIDs = new ArrayList<>();
      try {
         for (Field f : R.raw.class.getFields()) {
            @SuppressLint("DiscouragedApi")
            int id = context.getResources().getIdentifier(
                    f.getName(), debugDataFilesSourceDir, context.getPackageName());
            if (id > 0) {
               fileIDs.add(id);
            }
         }
         // Attempt to create the necessary files
         createDebugDataDuplicates(context, fileIDs);
      } catch (Exception e) {
         Log.e(TAG, "Failed on : Debugger", e);
      }
   }


   /*
    *
    * This method attempts to create the debug data
    *
    * */
   public static void createDebugDataDuplicates(final Context context, final List<Integer> inputRawResources) {
      try {
         // Get the context's resources
         final Resources resources = context.getResources();
         // Number of bytes to read in a single chunk
         final byte[] largeBuffer = new byte[1024 * 4];
         // Ephemeral variable used for tracking bytes to allocate for each file
         int bytesRead = 0;

         File thisDebugDataInputDirName = filePath(asList(mainDir.getAbsolutePath(), "debug", "input"));
         if (!thisDebugDataInputDirName.exists()){ thisDebugDataInputDirName.mkdirs(); }
         // For each file
         for (Integer resource : inputRawResources) {
            String fName = resources.getResourceEntryName(resource)+".mp4";
            File outFile = filePath(asList(thisDebugDataInputDirName.getAbsolutePath(), fName));
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
         Log.e(TAG, "Failed on createDebugDataDuplicates: ", e);
      }

   }
}