package com.adms.australianmobileadtoolkit;

import static com.adms.australianmobileadtoolkit.MainActivity.dataStore;
import static com.adms.australianmobileadtoolkit.MainActivity.initiateDataStore;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.createDirectory;
import static java.util.Collections.frequency;

import android.content.Context;
import android.view.View;
import android.widget.Button;

import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.adms.australianmobileadtoolkit.ui.fragments.FragmentAccessibilityDisclosure;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@SuppressWarnings("unchecked")
public class Common {


   /*
   *
   * This function converts a list of strings into a File path object
   *
   * */
   public static File filePath(List<String> path) {
      File output = null;
      for (String s : path) {
         output = (path.indexOf(s) == 0) ? new File(s) : (new File(output, s));
      }
      return output;
   }

   /*
   *
   * This function retrieves the files within a directory
   *
   * */
   public static List<String> getFilesInDirectory(File thisDirectory) {
      return Arrays.stream(Objects.requireNonNull(thisDirectory.listFiles()))
                     .filter(File::isFile).map(File::getName).collect(Collectors.toList());
   }

   /*
   *
   * This function makes a new directory (if it does not exist)
   *
   * */
   public static void makeDirectory(File thisDirectory) {
      if (!thisDirectory.exists()){ thisDirectory.mkdirs(); }
   }


   /*
   *
   * This function determines whether a value is within another value
   *
   * */
   public static boolean in(Object z, Object y) {
      if (z instanceof HashMap) {
         return (((HashMap<?, ?>) z).containsKey(y));
      } else if (z instanceof List) {
         List<?> zFormalised = ((List<?>) z);
         for (int i = 0; i < zFormalised.size(); i ++) {
            if (zFormalised.get(i).equals(y)) {
               return true;
            }
         }
      }
      return false;
   }

   public static void writeToFile(File thisFile, String contents) {
      try {
         PrintWriter writer = new PrintWriter(thisFile, "UTF-8");
         writer.print(contents);
         writer.close();
      } catch (Exception e) { }
   }

   public static String convertStreamToString(InputStream is) throws Exception {
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      StringBuilder sb = new StringBuilder();
      String line = null;
      while ((line = reader.readLine()) != null) {
         sb.append(line).append("\n");
      }
      reader.close();
      return sb.toString();
   }

   public static String readStringFromFile(File thisFile) {
      try {
         return convertStreamToString(Files.newInputStream(new File(thisFile.getAbsolutePath()).toPath()));
      } catch (Exception e) {
         return null;
      }
   }


   public static void dataStoreWrite(Context context, String key, String value) {
      initiateDataStore(context);
      String appliedValue = value;
      if (appliedValue == null) {
         appliedValue = "NULL_VALUE";
      }
      final String appliedValueFinal = appliedValue;
      Single<Preferences> updateResult = dataStore.updateDataAsync(prefsIn -> {
         MutablePreferences mutablePreferences = prefsIn.toMutablePreferences();
         mutablePreferences.set(PreferencesKeys.stringKey(key), appliedValueFinal);
         return Single.just(mutablePreferences);
      });
   }

   public static void dataStoreWriteToCorrupt(Context context, String key) {
      initiateDataStore(context);
      Single<Preferences> updateResult = dataStore.updateDataAsync(prefsIn -> {
         MutablePreferences mutablePreferences = prefsIn.toMutablePreferences();
         mutablePreferences.set(PreferencesKeys.stringKey(key), new String(new byte[0]));
         return Single.just(mutablePreferences);
      });
   }

   public static String dataStoreRead(Context context, String key, String defaultValue) {
      initiateDataStore(context);
      try {
         String retrievedValue = dataStore.data().map(prefs -> prefs.get(PreferencesKeys.stringKey(key))).blockingFirst();
         if (Objects.equals(retrievedValue, "NULL_VALUE")) {
            return null;
         } else {
            return retrievedValue;
         }
      } catch (java.lang.NullPointerException e) {
         return defaultValue;
      } catch (java.lang.RuntimeException e2) { // Typically caused by corruption
         return defaultValue;
      }
   }
}
