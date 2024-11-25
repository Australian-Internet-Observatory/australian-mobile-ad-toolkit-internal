package com.adms.australianmobileadtoolkit.interpreter;

import static com.adms.australianmobileadtoolkit.MainActivity.THIS_OBSERVER_ID;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

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

   public InterpreterWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
      super(context, workerParams);
   }

   @Override
   public Result doWork() {

      // Do the work here
      httpRequestPing();
      Interpreter lManager = new Interpreter(getApplicationContext());
      //try {
         // Opt for detection (as it can divert to sifting)
         // See Interpreter.run for more details
         lManager.run("DETECTION");//(deviceIsCharging(getApplicationContext())) ? "DETECTION" : "SIFTING");
      /*} catch (JSONException e) {
         throw new RuntimeException(e);
      }*/

      // Indicate success or failure with your return value:
      return Result.success();
   }

   private static boolean httpRequestPing() {
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
         requestBody.put("action","PING");
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
}
