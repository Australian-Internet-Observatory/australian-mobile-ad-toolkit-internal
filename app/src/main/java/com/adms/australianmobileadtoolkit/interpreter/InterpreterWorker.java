package com.adms.australianmobileadtoolkit.interpreter;

import static com.adms.australianmobileadtoolkit.Common.dataStoreRead;
import static com.adms.australianmobileadtoolkit.Common.dataStoreWrite;
import static com.adms.australianmobileadtoolkit.interpreter.FFmpegFrameGrabberAndroid.frameGrabAndroid;
import static com.adms.australianmobileadtoolkit.interpreter.FFmpegFrameGrabberAndroid.getVideoMetadataAndroid;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.platformInterpretationRoutine;
import static com.adms.australianmobileadtoolkit.interpreter.detector.ObjectDetector.objectDetectorAndroid;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.adms.australianmobileadtoolkit.MainActivity;
import com.adms.australianmobileadtoolkit.appSettings;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class InterpreterWorker extends Worker {

   private static String TAG = "InterpreterWorker";
   private Thread localThread;

   public InterpreterWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
      super(context, workerParams);
   }

   public static void platformInterpretationRoutineInterruption(Context context) {
      Log.i(TAG, "Perhaps thread related?...");
      dataStoreWrite(context, "platformRoutineRunning", "false");
      dataStoreWrite(context, "platformRoutineState", "COMPLETE");
      dataStoreWrite(context, "platformRoutineToAnalyze", "0");
      dataStoreWrite(context, "platformRoutineAnalyzed", "0");
   }

   public static Integer currentTimeInSeconds() {
      return Math.toIntExact(Math.round (Long.valueOf(System.currentTimeMillis()).doubleValue() / 1000));
   }

   public static boolean canForcePlatformInterpretation(Context context) {
      Integer lastCall = Integer.parseInt(dataStoreRead(context, "platformInterpretationLastCall", "0"));
      return (Math.abs(lastCall - currentTimeInSeconds()) > (15 * 60));
   }


   public static void platformInterpretationRoutineContainer(Context context) {
      dataStoreWrite(context, "periodicWorkerRunning", "true");
      dataStoreWrite(context, "platformInterpretationLastCall", String.valueOf(currentTimeInSeconds()));
      try {
         platformInterpretationRoutine(context, MainActivity.getMainDir(context),
                 getVideoMetadataAndroid, frameGrabAndroid, true, objectDetectorAndroid);
      } catch (Exception e) {
         e.printStackTrace();
         platformInterpretationRoutineInterruption(context);
      }
      dataStoreWrite(context, "periodicWorkerRunning", "false");
   }

   @Override
   public Result doWork() {
      localThread = Thread.currentThread();
      // This check stops a periodic worker from overlapping on a manual process
      if ((dataStoreRead(getApplicationContext(), "platformRoutineRunning", "false").equals("false")) || canForcePlatformInterpretation(getApplicationContext())) {
         platformInterpretationRoutineContainer(getApplicationContext());
      } else {
         Log.i(TAG, "Periodic worker has been cancelled due to manual process...");
      }



      // Indicate success or failure with your return value:
      return Result.success();
   }


   public static boolean httpRequestRegisterJoinedAt(String joinedAt, String observerID) {
      try {
         // Declare the AWS Lambda endpoint
         String urlParam = appSettings.AWS_LAMBDA_ENDPOINT;
         // The unique ID of the observer to insert with the HTTP request
         // The identifier for submitting a registration
         // The HTTP request connection timeout (in milliseconds)
         int requestConnectTimeout = appSettings.AWS_LAMBDA_ENDPOINT_CONNECTION_TIMEOUT;
         // The HTTP request read timeout (in milliseconds)
         int requestReadTimeout = appSettings.AWS_LAMBDA_ENDPOINT_READ_TIMEOUT;
         // Assemble the request JSON object
         JSONObject requestBody = new JSONObject();
         requestBody.put("action","JOINED");
         requestBody.put("observerID",observerID);
         requestBody.put("joinedAt",joinedAt);
         String bodyParam = requestBody.toString();
         // Set up the HTTP request configuration
         URL url = new URL(urlParam);
         HttpURLConnection connection = (HttpURLConnection) url.openConnection();
         connection.setDoOutput(true);
         connection.setRequestMethod("POST");
         connection.setRequestProperty("Accept", "text/plain");
         connection.setRequestProperty("Content-Type", "text/plain");
         connection.setConnectTimeout(requestConnectTimeout);
         connection.setReadTimeout(requestReadTimeout);
         OutputStream os = connection.getOutputStream();
         OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
         osw.write(bodyParam);
         osw.flush();
         osw.close();
         connection.connect();
         // Interpret the output
         BufferedReader rd = new BufferedReader(new InputStreamReader(
                 connection.getInputStream()));
         return true;
      } catch (Exception e) {
         Log.e(TAG, "Failed to run httpRequestPing: ", e);
         return false;
      }
   }

   @Override
   public void onStopped() {
      super.onStopped();
      Log.i(TAG, "Periodic worker was stopped!!!!!!!!");
      localThread.interrupt();
   }
}
