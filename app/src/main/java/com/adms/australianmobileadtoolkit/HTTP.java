package com.adms.australianmobileadtoolkit;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class HTTP {

    public static boolean HTTPRequest(JSONObject requestBody) {
        String urlParam = appSettings.AWS_LAMBDA_ENDPOINT;
        // The HTTP request connection timeout (in milliseconds)
        int requestConnectTimeout = appSettings.AWS_LAMBDA_ENDPOINT_CONNECTION_TIMEOUT;
        // The HTTP request read timeout (in milliseconds)
        int requestReadTimeout = appSettings.AWS_LAMBDA_ENDPOINT_READ_TIMEOUT;
        //
        boolean successfullyDispatched = true;
        // Assemble the request JSON object
        try {
            // Set up the HTTP request configuration
            URL url = new URL(urlParam);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept", "text/plain");
            connection.setRequestProperty("Content-Type", "text/plain");
            connection.setConnectTimeout(requestConnectTimeout);
            connection.setReadTimeout(requestReadTimeout);
            // Submit the HTTP request
            OutputStream os = connection.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
            osw.write(requestBody.toString());
            osw.flush();
            osw.close();
            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream thisInputStream = connection.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(thisInputStream));
                StringBuilder sb = new StringBuilder();
                String output;
                while ((output = br.readLine()) != null) {
                    sb.append(output);
                }
                JSONObject response = new JSONObject(sb.toString());
                try {
                    successfullyDispatched = (response.get("dispatched")).equals("TRUE");
                } catch (Exception e) {
                    e.printStackTrace();}
                thisInputStream.close();
            }

        } catch (Exception e) {
            successfullyDispatched = false;
            e.printStackTrace();
        }
        return successfullyDispatched;
    }
}
