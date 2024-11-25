package com.adms.australianmobileadtoolkit.ui;

import static com.adms.australianmobileadtoolkit.MainActivity.THIS_OBSERVER_ID;

import android.os.AsyncTask;
import android.util.Log;

import com.adms.australianmobileadtoolkit.appSettings;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Registration extends AsyncTask<Object, Void, Boolean> {
   public AsyncResponse delegate = null;
   @Override
   protected Boolean doInBackground(Object... objects) {
      return httpRequestRegistration((JSONObject)objects[0]);
   }
   @Override
   protected void onPostExecute(Boolean result) {
      super.onPostExecute(result);
      delegate.processFinish(result);
   }

   /*
    *
    * This method attempts to send a HTTP POST request containing the registration of the user
    * for data donations
    *
    * */
   public static boolean httpRequestRegistration(JSONObject registrationJSONObject) {
      try {
         // Declare the AWS Lambda endpoint
         String urlParam = appSettings.AWS_LAMBDA_ENDPOINT;
         // The unique ID of the observer to insert with the HTTP request
         String observerID = THIS_OBSERVER_ID;
         // The identifier for submitting a registration
         String identifierDataDonation = appSettings.IDENTIFIER_REGISTRATION;
         // The HTTP request connection timeout (in milliseconds)
         int requestConnectTimeout = appSettings.AWS_LAMBDA_ENDPOINT_CONNECTION_TIMEOUT;
         // The HTTP request read timeout (in milliseconds)
         int requestReadTimeout = appSettings.AWS_LAMBDA_ENDPOINT_READ_TIMEOUT;
         // Assemble the request JSON object
         JSONObject requestBody = new JSONObject();
         requestBody.put("action",identifierDataDonation);
         requestBody.put("observer_id",observerID);
         requestBody.put("user_details",registrationJSONObject);
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
         // TODO read output and determine shitty responses
         return true;
      } catch (Exception e) {
         return false;
      }
   }
}
