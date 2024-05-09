/*
*
* This class deals with the direct analysis of video content (frame by frame), to identify instances
* where the individual is using their Facebook app, and then furthermore, if they are observing
* sponsored advertisement content. In such cases, the content is then submitted to an AWS Lambda
* endpoint
*
* */

package com.adms.australianmobileadtoolkit.interpreter;

import static com.adms.australianmobileadtoolkit.Arguments.A;
import static com.adms.australianmobileadtoolkit.Arguments.Args;
import static com.adms.australianmobileadtoolkit.Common.filePath;
import static com.adms.australianmobileadtoolkit.interpreter.Depreciated.processScreenshots;
import static com.adms.australianmobileadtoolkit.interpreter.FFmpegPseudoFrameGrabber.getBitmapAtMilliseconds;
import static com.adms.australianmobileadtoolkit.interpreter.FFmpegPseudoFrameGrabber.getTimeInMilliseconds;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.getWhitespacePixel;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Facebook.retrieveReferenceStencilsPictograms;
import static com.adms.australianmobileadtoolkit.MainActivity.THIS_OBSERVER_ID;
import static com.adms.australianmobileadtoolkit.Settings.DEBUG;
import static com.adms.australianmobileadtoolkit.Settings.IntendedFrameRate;
import static com.adms.australianmobileadtoolkit.Settings.sharedPreferenceGet;
//import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
//import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

import static java.lang.Long.parseLong;
import static java.util.Arrays.asList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Base64;
import android.util.Log;

import com.adms.australianmobileadtoolkit.MainActivity;
import com.adms.australianmobileadtoolkit.Settings;
import com.adms.australianmobileadtoolkit.interpreter.platform.FacebookScreenshot;
import com.adms.australianmobileadtoolkit.interpreter.visual.DividerSet;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
//import com.arthenica.mobileffmpeg.Config;
//import com.arthenica.mobileffmpeg.ExecuteCallback;
//import com.arthenica.mobileffmpeg.FFmpeg;

public class Interpreter {
    private static final String TAG = "Interpreter";
    private static File rootDirectoryPath;
    private Context thisContext;

    /*
    *
    * This method gets the image similarity of two images, by comparing their pixels, at a reduced
    * resolution.
    *
    * */
    private double getNaiveImageSimilarity(Bitmap lastBitmapFromFrame,
                                           Bitmap thisBitmapFromFrame) {
        // To up the accuracy of the method, increase this value //CONFIGURABLE
        int scalePixelsWidth = Settings.IMAGE_SIMILARITY_SCALE_PIXELS_WIDTH;
        // Derive the scaled height, by applying the ratio of the inserted bitmaps
        int scalePixelsHeight = Math.round( (float)lastBitmapFromFrame.getWidth()
                / (float)lastBitmapFromFrame.getHeight() * (float) scalePixelsWidth);

        int hh = thisBitmapFromFrame.getHeight();
        int ww = thisBitmapFromFrame.getWidth();

        // Generate the scaled bitmaps
        //itmap lastBitmapFromFrameScaled = Bitmap.createScaledBitmap(
        //        lastBitmapFromFrame, scalePixelsWidth, scalePixelsHeight,false);
        //Bitmap thisBitmapFromFrameScaled = Bitmap.createScaledBitmap(
        //        thisBitmapFromFrame, scalePixelsWidth, scalePixelsHeight,false);
        // Analyse the color difference (RGB) between the scaled bitmaps
        double maximumCumulativeDifference = (255.0 * 3.0 * scalePixelsWidth * scalePixelsHeight);
        long thisCumulativeDifference = 0;
        for (int y = scalePixelsHeight - 1; y >= 0; y--) {
            for (int x = scalePixelsWidth - 1; x >= 0; x--) {
                int posX = (int) Math.min(Math.ceil(ww*((double)x/scalePixelsWidth)), ww-1);
                int posY = (int) Math.min(Math.ceil(hh*((double)y/scalePixelsHeight)), hh-1);
                int c1 = lastBitmapFromFrame.getPixel(posX, posY);
                int c2 = thisBitmapFromFrame.getPixel(posX, posY);
                thisCumulativeDifference += (Math.abs(Color.red(c1) - Color.red(c2))
                        + Math.abs(Color.blue(c1) - Color.blue(c2))
                        + Math.abs(Color.green(c1) - Color.green(c2)));
            }
        }
        // Take the total cumulative difference, and divide it by the maximum possible difference
        return 1.0 - (thisCumulativeDifference / maximumCumulativeDifference);
    }

    /*
     *
     * This method initialises an instance of the Interpreter class
     *
     * */
    @SuppressLint("ResourceType")
    public Interpreter(Context context){
        // The rootDirectoryPath variable must be initialised here, to access the app context
        rootDirectoryPath = MainActivity.getMainDir(context);
        thisContext = context;
    }

    public Boolean iterateOverVideos(List<File> filesToProcess, String instance, Boolean allowEarlyExit) {
        Boolean foundAPositiveInSift = false;
        // If there are at least two videos, then processing can begin - this is because
        // the Interpreter can only spin up when the screen-recording functionality is
        // active or in the background during idle behaviour, which means that a 'WIP' video
        // will always be present when it activates - this does also introduce the issue that
        // the very last time the app closes, we will lose some data, however this should be
        // negligible
        if ((!filesToProcess.isEmpty()) && ((Objects.equals(instance, "SIFTING"))
              || (Objects.equals(instance, "DETECTION")))) {
            // The latest screen recording is identified
            int delete = 0;
            long latest = filesToProcess.get(0).lastModified();
            for(int i = 0; i < filesToProcess.size(); i++) {
                if( filesToProcess.get(i).lastModified() > latest) {
                    latest = filesToProcess.get(i).lastModified();
                    delete = i;
                }
            }
            // It is then removed from the list of videos that will be processed (as it may
            // still be instantiating from the screen recording)
            if (Objects.equals(sharedPreferenceGet(thisContext, "RECORDING_STATUS", "true"), "true")
                  && (Objects.equals(instance, "SIFTING"))) {
                filesToProcess.remove(delete);
                Log.i(TAG, "\t... excluding 1 video to mitigate in-progress recordings (where applicable)");
            } else {
                Log.i(TAG, "\t... processing all videos (as not currently recording and SIFTING OR not within SIFTING instance)");
            }


            // Loop through all the videoFiles, and run the startEventDetection for each
            int i = 0;
            while (i < filesToProcess.size()) {
                if (Objects.equals(instance, "SIFTING")) {
                    foundAPositiveInSift = sift( filesToProcess.get(i), thisContext);
                } else if (Objects.equals(instance, "DETECTION")){
                    startEventDetection( filesToProcess.get(i));
                }
                i++;
                if ((foundAPositiveInSift) && (allowEarlyExit)) {
                    break;
                }
            }
        }
        return foundAPositiveInSift;
    }

    /*
     *
     * This method initiates the startEventDetection for each of the processable videos
     *
     * */
    public void run(String instance) throws JSONException {
        List<File> unsiftedVideoFiles = new ArrayList<>();
        Log.i(TAG, "Running Interpreter as "+instance+" instance!");
        if (DEBUG) {
            Log.i(TAG, "Running Interpreter in debug mode!");
            startEventDetection( filePath(asList(rootDirectoryPath.getAbsolutePath(), "debug", "input", Settings.DEBUG_TARGET_VIDEO)));
        } else {
            Log.i(TAG, "Running Interpreter...");
            try {
                // Retrieve the video files within the designated folder
                List<File> videoFiles = new ArrayList<>();
                // We add all to the empty array, to avoid unanticipated construction errors
                videoFiles.addAll(asList(Objects.requireNonNull(new File(
                      rootDirectoryPath + (File.separatorChar
                            + "videos" + File.separatorChar)).listFiles())));

                List<File> positiveVideoFiles = videoFiles.stream().filter(x -> x.getAbsolutePath().contains("positive")).collect(Collectors.toList());
                unsiftedVideoFiles = videoFiles.stream().filter(x -> (!x.getAbsolutePath().contains("positive"))).collect(Collectors.toList());


                if (Objects.equals(instance, "SIFTING")) {
                    Log.i(TAG, "\t... "+unsiftedVideoFiles.size()+" video(s) to process");
                    iterateOverVideos(unsiftedVideoFiles, instance, false);
                } else
                if (Objects.equals(instance, "DETECTION")) {
                    // Attempt to analyze existing positives
                    Log.i(TAG, "\t... "+positiveVideoFiles.size()+" video(s) to process");
                    iterateOverVideos(positiveVideoFiles, instance, false);
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to run Interpreter: ", e);
            }
        }
        Log.i(TAG, "Ending Interpreter as "+instance+" instance!");

        // In the detection event, after the routine is complete, if there are still unsifted videos, go
        // ahead and sift them, but allow an early exit if a positive match is found. Then, if a positive
        // match is indeed found, run the instance again as a detection to duly process it.
        if (Objects.equals(instance, "DETECTION") && (unsiftedVideoFiles.size() > 0)) {
            Boolean exitedEarlyDueToPositiveMatch = iterateOverVideos(unsiftedVideoFiles, "SIFTING", true);
            if (exitedEarlyDueToPositiveMatch) {
                run("DETECTION");
            }
        }
    }

    /*
    *
    * This method gets the exact frame rate of the video, as adapted from
    * https://stackoverflow.com/questions/42204944/how-to-get-frame-rate-of-video-in-android-os
    *
    * */
    private int getExactFrameRate(String filePath) {
        MediaExtractor extractor = new MediaExtractor();
        int frameRate = 24; //may be default
        try {
            extractor.setDataSource(filePath);
            int numTracks = extractor.getTrackCount();
            for (int i = 0; i < numTracks; ++i) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            extractor.release();
        }
        return frameRate;
    }


    /*
     * TODO - clean
     */
    private Boolean sift(File videoFileToProcess, Context context) {
        Boolean foundAPositive = false;
        long elapsedTime = System.currentTimeMillis();
        try {
            // The base number of frame intervals to skip
            double frameSimilarityThreshold = Settings.RECORDER_FRAME_SIMILARITY_THRESHOLD;
            // This event is timed for optimisation purposes
            Log.i(TAG, "(sift) Beginning sift of videoFilePath: " + videoFileToProcess);

            HashMap<String, Object> pictogramsReference = retrieveReferenceStencilsPictograms(thisContext);

            // Provided that the video is rotated to 'portrait' mode
            if ((videoFileToProcess.getAbsolutePath().contains("portrait") && (!videoFileToProcess.getAbsolutePath().contains("positive"))) || (DEBUG)) {

                File videoDirectory = filePath(asList(MainActivity.getMainDir(context).getAbsolutePath(), "videos"));

                Integer desiredSampleElements = 4;
                long thisVideoTotalMilliseoconds = getTimeInMilliseconds(thisContext, videoFileToProcess);
                long interval = 1000;//(long) Math.round(thisVideoTotalMilliseoconds/desiredSampleElements.doubleValue());
                Log.i(TAG, "\t(sift) No. of milliseconds in video: " + thisVideoTotalMilliseoconds);
                Log.i(TAG, "\t(sift) Interval setting: " + interval);
                int ii = (int) interval;
                Boolean inFacebook = false;
                while ((!(thisVideoTotalMilliseoconds == -1)) && (ii < (thisVideoTotalMilliseoconds-1))) {

                    Bitmap thisBitmapAtMilliseconds = getBitmapAtMilliseconds(thisContext, videoFileToProcess, ii, 0.3, 330);
                    if (thisBitmapAtMilliseconds != null) {

                        // TODO - this is a debug feature
                        /*try (FileOutputStream out = new FileOutputStream(
                            filePath(asList(videoDirectory.getAbsolutePath(), ii+".png")).getAbsolutePath())) {
                                thisBitmapAtMilliseconds.compress(Bitmap.CompressFormat.PNG, 100, out); } catch (IOException e) {}*/
                        long startTime;
                        startTime = System.currentTimeMillis();
                        int thisWhitespacePixel = getWhitespacePixel(Args(A("bitmap", thisBitmapAtMilliseconds)));
                        DividerSet thisVisualComponents = new DividerSet(Args(
                                                                A("bitmap", thisBitmapAtMilliseconds),
                                                                A("orientation", "v"),
                                                                A("scaleMinor", 0.1),
                                                                A("absorbMinimums", false),
                                                                A("whitespaceThreshold", 0.1),
                                                                A("colourPaletteThreshold", 0.05),
                                                                A("scanUntil", 0.2),
                                                                A("retainMinimums", true),
                                                                A("minDividerApproach", "complex")));
                        Log.i(TAG, "\t* Time measurement: " + ((System.currentTimeMillis() - startTime)));

                        startTime = System.currentTimeMillis();
                        List<Bitmap> thisImageDividers = (List<Bitmap>) thisVisualComponents.dividerImages;
                        FacebookScreenshot result = new FacebookScreenshot(Args(
                              A("dividers", thisImageDividers),
                              A("whitespacePixel", thisWhitespacePixel),
                              A("referenceStencilsPictograms", pictogramsReference),
                              A("h", thisBitmapAtMilliseconds.getHeight())));



                        Log.i(TAG, "\t* Time measurement: " + ((System.currentTimeMillis() - startTime)));






                        inFacebook = (Boolean) result.inFacebook;
                        Log.i(TAG, "\t(sift) Frame " + ii + " within Facebook: " + inFacebook);
                        if (inFacebook) {
                            Log.i(TAG, "\t(sift) ... exiting loop prematurely");
                            break;
                        }
                    } else {
                        Log.i(TAG, "\t(sift) ... Frame "+ii+" has reported an error");
                    }
                    ii += interval;
                }
                if (!inFacebook) {
                    if ((videoFileToProcess.exists()) && (!videoFileToProcess.delete())) {
                        Log.e(TAG, "(sift) Failed to delete videoFilePath: " + videoFileToProcess);
                    }
                } else {
                    // Tag the identified video
                    Log.w(TAG, "(sift) Tagging positive video: " + videoFileToProcess);
                    File newFile = new File(videoFileToProcess.getParent(), videoFileToProcess.getName().replace(".mp4", ".positive.mp4"));
                    Files.move(videoFileToProcess.toPath(), newFile.toPath());
                    foundAPositive = true;
                }
            } else {
                // Landscape videos are deleted by default
                if ((videoFileToProcess.exists()) && (!videoFileToProcess.delete())) {
                    Log.e(TAG, "(sift) Failed to delete videoFilePath: " + videoFileToProcess);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "(sift) Failed to execute startEventDetection: ", e);
        }
        Log.i(TAG, "(sift) Elapsed time: " + Math.abs((System.currentTimeMillis()) - elapsedTime));

        return foundAPositive;
    }


    /*
    *
    * This method runs the logo detection functionality to determine if sponsored content is present
    * within the individual's Facebook News Feed, and submits instances that involve such content to
    * an AWS Lambda endpoint
    *
    * */
    // TODO introduce a faster looping function
    // This is achieved by upping the frame intervals (to perhaps a second, and then when
    // identifying Facebook, reduce the interval size, and step back one large interval to
    // preserve any possibly skipped frames
    // TODO Send up logoDetector statistics with regular data
    private void startEventDetection(File videoFileToProcess) {
        try {
            // The base number of frame intervals to skip
            double frameSimilarityThreshold = Settings.RECORDER_FRAME_SIMILARITY_THRESHOLD;
            // This event is timed for optimisation purposes
            long startTime = System.currentTimeMillis();
            Log.i(TAG, "Beginning eventDetection of videoFilePath: " + videoFileToProcess);

            List<JSONObject> bitmapsToProcess = new ArrayList<>();

            HashMap<String, Object> pictogramsReference = retrieveReferenceStencilsPictograms(thisContext);

            // Provided that the video is rotated to 'portrait' mode and positively sifted...
            if ((videoFileToProcess.getAbsolutePath().contains("portrait") && videoFileToProcess.getAbsolutePath().contains("positive")) || (DEBUG)) {

                long startingPosition = (DEBUG) ? 0L : parseLong(Arrays.stream((videoFileToProcess.getName().split("\\."))).findFirst().get());
                long thisVideoTotalMilliseoconds = getTimeInMilliseconds(thisContext, videoFileToProcess);
                long interval = (int) Math.ceil(1000f/IntendedFrameRate);

                long position = 0;
                Bitmap lastBitmap = null;
                // Begin looping through the frames
                while ((position + interval) < thisVideoTotalMilliseoconds) {

                    Bitmap thisBitmapAtMilliseconds = getBitmapAtMilliseconds(thisContext, videoFileToProcess, position, 0.5, 500);
                    boolean framesAreIdentical = false;
                    if (lastBitmap != null) {
                        try {
                            // Determine the similarity of this frame to the last frame
                            double similarity = getNaiveImageSimilarity(
                                  lastBitmap, thisBitmapAtMilliseconds);
                            Log.i(TAG, "\t* Similarity of bitmaps " + position
                                  + " & " + (position + interval) + " : " + similarity);
                            framesAreIdentical = (similarity > frameSimilarityThreshold);
                        } catch (Exception e) {
                            framesAreIdentical = true;
                        }
                    }
                    Log.i(TAG, "\t* Processing bitmap " + position);
                    if (!framesAreIdentical) {
                        String fname = (position+startingPosition) + "." + UUID.randomUUID().toString() + ".png";
                        JSONObject thisBitmap = new JSONObject();
                        thisBitmap.put("fname", fname);
                        thisBitmap.put("bitmap", thisBitmapAtMilliseconds);
                        bitmapsToProcess.add(thisBitmap);
                    } else {
                        Log.i(TAG, "\t* Bypassing due to identical bitmaps");
                    }
                    position += interval;
                    // Set the last frame to the current frame before the next iteration
                    lastBitmap = thisBitmapAtMilliseconds;
                }
                Log.i(TAG,  "\t* Time taken: " + ((System.nanoTime() - startTime) / 1e+9));
                Log.i(TAG,  "Ending eventDetection of videoFilePath: " + videoFileToProcess);

                // Inform us if the video cannot be deleted - note that only portrait videos can be deleted
                if ((!DEBUG) && ((videoFileToProcess.exists()) && (!videoFileToProcess.delete()))) {
                    Log.e(TAG, "Failed to delete videoFilePath: " + videoFileToProcess);
                }
            } else {
                // Landscape videos are deleted by default
                if ((videoFileToProcess.exists()) && (!videoFileToProcess.delete())) {
                    Log.e(TAG, "Failed to delete videoFilePath: " + videoFileToProcess);
                }
            }

            Log.i(TAG, "bitmapsToProcess.size" + String.valueOf(bitmapsToProcess.size()));

            String thisEvent = UUID.randomUUID().toString();
            List<JSONObject> processed = processScreenshots(bitmapsToProcess, true, pictogramsReference, thisContext);
            Log.i(TAG, "Ads found: " + String.valueOf(processed.size()));

            if (!DEBUG) {
                for (JSONObject jsonObject : processed) {
                    JSONObject json = (JSONObject) jsonObject.get("json");
                    List<Bitmap> chunks = (List<Bitmap>) jsonObject.get("chunks");
                    int jj = 0;
                    for (Bitmap chunk : chunks) {
                        httpRequestDataDonationV2(chunk, json, thisEvent, jj);
                        jj ++;
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to execute startEventDetection: ", e);
        }
    }

    /*
    *
    * This method attempts to send a HTTP POST request containing the screenshot data to the AWS
    * Lambda endpoint that is responsible for this project
    *
    * */
    private void httpRequestDataDonationV2(Bitmap thisBitmapFromFrame, JSONObject json, String thisEvent, Integer chunkID) {
        try {
            // Declare the AWS Lambda endpoint
            String urlParam = Settings.AWS_LAMBDA_ENDPOINT;
            // The exported image quality
            int imageExportQuality = Settings.IMAGE_EXPORT_QUALITY;
            // The unique ID of the observer to insert with the HTTP request
            String observerID = THIS_OBSERVER_ID;
            // The identifier for submitting data donations
            String identifierDataDonation = Settings.IDENTIFIER_DATA_DONATION;
            // The HTTP request connection timeout (in milliseconds)
            int requestConnectTimeout = Settings.AWS_LAMBDA_ENDPOINT_CONNECTION_TIMEOUT;
            // The HTTP request read timeout (in milliseconds)
            int requestReadTimeout = Settings.AWS_LAMBDA_ENDPOINT_READ_TIMEOUT;
            // Write up the stream for inserting the image (as a Base64 string) into the request
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            thisBitmapFromFrame.compress(Bitmap.CompressFormat.JPEG, imageExportQuality, stream);
            String imageEncodedAsBase64 = Base64.encodeToString(
                  stream.toByteArray(), Base64.DEFAULT);
            // Assemble the request JSON object
            JSONObject requestBody = new JSONObject();
            requestBody.put("action",identifierDataDonation);
            requestBody.put("observer_id",observerID);
            requestBody.put("observation_id",thisEvent);
            requestBody.put("chunk_id",chunkID);
            requestBody.put("json",json);
            requestBody.put("imageEncodedAsBase64",imageEncodedAsBase64);
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
        } catch (Exception e) {
            Log.e(TAG, "Failed to run httpRequestDataDonation: ", e);
        }
    }

    private void httpRequestDataDonation(Bitmap thisBitmapFromFrame, String videoFileName,
                                         String currentFrame, int thisExactFrameRate) {
        try {
            // Declare the AWS Lambda endpoint
            String urlParam = Settings.AWS_LAMBDA_ENDPOINT;
            // The exported image quality
            int imageExportQuality = Settings.IMAGE_EXPORT_QUALITY;
            // The unique ID of the observer to insert with the HTTP request
            String observerID = THIS_OBSERVER_ID;
            // The identifier for submitting data donations
            String identifierDataDonation = Settings.IDENTIFIER_DATA_DONATION;
            // The HTTP request connection timeout (in milliseconds)
            int requestConnectTimeout = Settings.AWS_LAMBDA_ENDPOINT_CONNECTION_TIMEOUT;
            // The HTTP request read timeout (in milliseconds)
            int requestReadTimeout = Settings.AWS_LAMBDA_ENDPOINT_READ_TIMEOUT;
            // Write up the stream for inserting the image (as a Base64 string) into the request
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            thisBitmapFromFrame.compress(Bitmap.CompressFormat.JPEG, imageExportQuality, stream);
            String imageEncodedAsBase64 = Base64.encodeToString(
                                            stream.toByteArray(), Base64.DEFAULT);
            // Assemble the request JSON object
            JSONObject requestBody = new JSONObject();
            requestBody.put("action",identifierDataDonation);
            requestBody.put("observer_id",observerID);
            requestBody.put("video_filename",videoFileName);
            requestBody.put("current_frame",currentFrame);
            requestBody.put("exact_framerate",thisExactFrameRate);
            requestBody.put("imageEncodedAsBase64",imageEncodedAsBase64);
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
        } catch (Exception e) {
            Log.e(TAG, "Failed to run httpRequestDataDonation: ", e);
        }
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
            String urlParam = Settings.AWS_LAMBDA_ENDPOINT;
            // The unique ID of the observer to insert with the HTTP request
            String observerID = THIS_OBSERVER_ID;
            // The identifier for submitting a registration
            String identifierDataDonation = Settings.IDENTIFIER_REGISTRATION;
            // The HTTP request connection timeout (in milliseconds)
            int requestConnectTimeout = Settings.AWS_LAMBDA_ENDPOINT_CONNECTION_TIMEOUT;
            // The HTTP request read timeout (in milliseconds)
            int requestReadTimeout = Settings.AWS_LAMBDA_ENDPOINT_READ_TIMEOUT;
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
            Log.i(TAG, rd.readLine());
            // TODO read output and determine shitty responses
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to run httpRequestRegistration: ", e);
            return false;
        }
    }

}
