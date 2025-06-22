package com.adms.australianmobileadtoolkit.interpreter;

import static com.adms.australianmobileadtoolkit.Common.dataStoreWrite;
import static com.adms.australianmobileadtoolkit.Common.makeDirectory;
import static com.adms.australianmobileadtoolkit.appSettings.hardFixObserverIDRead;
import static com.adms.australianmobileadtoolkit.interpreter.Sampler.basicReading;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Facebook.evaluateFacebookAd;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Instagram.evaluateInstagramAd;
import static com.adms.australianmobileadtoolkit.interpreter.platform.TikTok.evaluateTikTokAd;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Youtube.evaluateYoutubeAd;


import static java.util.Arrays.asList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import com.adms.australianmobileadtoolkit.JSONXObject;
import com.adms.australianmobileadtoolkit.appSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Platform {


    private static String TAG = "Platform";

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Numerical Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public static Integer rangesOverlap(int aMin, int aMax, int bMin, int bMax) {
        return Math.max(0, Math.min(aMax, bMax) - Math.max(aMin, bMin) + 1);
    }

    public static Double rectangularArea(JSONXObject a) {
        return (((double) a.get("w")) * ((double) a.get("h")));
    }

    public static Double rectangularAreaOverlap(JSONXObject a, JSONXObject b) {
        Double aX1 = (double) a.get("x1");
        Double aX2 = (double) a.get("x2");
        Double aY1 = (double) a.get("y1");
        Double aY2 = (double) a.get("y2");
        Double bX1 = (double) b.get("x1");
        Double bX2 = (double) b.get("x2");
        Double bY1 = (double) b.get("y1");
        Double bY2 = (double) b.get("y2");
        return Math.max(0, Math.min(aX2, bX2) - Math.max(aX1, bX1)) * Math.max(0, Math.min(aY2, bY2) - Math.max(aY1, bY1));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Image-Related Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public static void saveBitmap(Bitmap bmp, String fname) {
        try (FileOutputStream out = new FileOutputStream(fname)) {
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // File Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String filenameUnextended(File thisFilename) {
        return Arrays.stream(thisFilename.getName().split("\\.")).collect(Collectors.toList()).get(0);
    }

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

    public static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : Objects.requireNonNull(fileOrDirectory.listFiles()))
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

    public static void createDirectory(File folderToCreate, Boolean repopulate) {
        if (repopulate) {
            deleteRecursive(folderToCreate);
        }
        try {
            // Create it if it doesn't exist
            if (!folderToCreate.exists()) {
                Files.createDirectories(Paths.get(folderToCreate.getAbsolutePath()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static JSONObject readJSONFromFile(File thisFile) {
        try {
            return new JSONObject(new String(Files.readAllBytes(Paths.get(thisFile.getAbsolutePath()))));
        } catch (Exception e) {

            e.printStackTrace();
        }
        return null;
    }

    public static void writeToJSON(File outputFile, Object content) {
        try {
            PrintWriter writer = new PrintWriter(outputFile, "UTF-8");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(content);
            writer.println(json);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void printJSON(Object thisToJSON) {
        try {
            System.out.println((new ObjectMapper().writer().withDefaultPrettyPrinter()).writeValueAsString(thisToJSON));
        } catch (Exception e) {
            e.printStackTrace();
            try {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(thisToJSON);
                System.out.println(json);
            } catch (Exception e2) {
                e2.printStackTrace();}
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Dispatch Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // TODO - get some logging
    public static Boolean dispatchAdFile(String thisParticipantUUID, String thisAdUUID, File thisDispatchableFile, String identifierDataDonation) {
        Boolean successfullyDispatched = false;
        // Declare the AWS Lambda endpoint
        String urlParam = appSettings.AWS_LAMBDA_ENDPOINT;
        // The exported image quality
        int imageExportQuality = appSettings.IMAGE_EXPORT_QUALITY;
        // The HTTP request connection timeout (in milliseconds)
        int requestConnectTimeout = appSettings.AWS_LAMBDA_ENDPOINT_CONNECTION_TIMEOUT;
        // The HTTP request read timeout (in milliseconds)
        int requestReadTimeout = appSettings.AWS_LAMBDA_ENDPOINT_READ_TIMEOUT;

        // Assemble the request JSON object
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("action",identifierDataDonation);
            requestBody.put("ad_id",thisAdUUID);
            requestBody.put("observer_id",thisParticipantUUID);
            requestBody.put("filename",thisDispatchableFile.getName());
            if (thisDispatchableFile.getName().endsWith(".bmp") || thisDispatchableFile.getName().endsWith(".png") || thisDispatchableFile.getName().endsWith(".jpg")) {
                // Load in the bitmap
                Bitmap thisBitmap = BitmapFactory.decodeFile(thisDispatchableFile.getAbsolutePath());
                // Write up the stream for inserting the image (as a Base64 string) into the request
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                // Compress the bitmap
                thisBitmap.compress(Bitmap.CompressFormat.JPEG, imageExportQuality, stream);
                String imageEncodedAsBase64 = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT);
                requestBody.put("content", imageEncodedAsBase64);
            } else if (thisDispatchableFile.getName().endsWith(".json")) {
                requestBody.put("content", (new String(Files.readAllBytes(Paths.get(thisDispatchableFile.getAbsolutePath())))));
            }
        } catch (Exception e) {
            successfullyDispatched = false;
            e.printStackTrace();
        }
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

    public static List<File> generateOrderedAdFiles(File adsFromDispatchDirectory) {
        if (!adsFromDispatchDirectory.isDirectory()) {
            return new ArrayList<>();
        }
        List<String> adsFromDispatchDirectoryNames = Arrays
                .stream(adsFromDispatchDirectory.listFiles())
                .map(x -> x.getName())
                .sorted()
                .collect(Collectors.toList());
        List<File> adsFromDispatchDirectoryFiles = adsFromDispatchDirectoryNames
                .stream()
                .map(x -> (new File(adsFromDispatchDirectory, x)))
                .collect(Collectors.toList());
        return adsFromDispatchDirectoryFiles;
    }

    public static void dispatchAdsV2(Context context, String thisParticipantUUID, File dispatchDirectory) throws Exception {
        Integer numberOfAdsToDelete = 50;
        Integer maximumNumberOfHeldAds = 5000;

        // Order the ads, as they will be dipatched (or deleted) in order
        List<File> adsFromDispatchDirectoryFiles = generateOrderedAdFiles(dispatchDirectory);

        // Prior to dispatching ads, a check is done to ensure that the folder is not overflowing
        if (adsFromDispatchDirectoryFiles.size() > maximumNumberOfHeldAds) {
            for (File thisFile : adsFromDispatchDirectoryFiles.subList(0, numberOfAdsToDelete)) {
                thisFile.delete();
            }
        }

        // Retrieve (and order) the ad files again
        adsFromDispatchDirectoryFiles = generateOrderedAdFiles(dispatchDirectory);



        if ((dispatchDirectory != null) && (adsFromDispatchDirectoryFiles != null)) {

            dataStoreWrite(context, "platformRoutineToRelay", String.valueOf(adsFromDispatchDirectoryFiles.size()));
            Integer platformRoutineRelayed = 0;

            for (File thisAdSuperDirectory : adsFromDispatchDirectoryFiles) {
                persistThread(context, TAG);
                if (thisAdSuperDirectory.listFiles().length == 0) {
                    Log.i("Dispatch", "Deleting empty super directory: "+thisAdSuperDirectory.getAbsolutePath());
                    thisAdSuperDirectory.delete();
                } else {
                    for (File thisAdDirectory : thisAdSuperDirectory.listFiles()) {
                        // A dispatch can only begin when the adContent file has been submitted - this prevents 'half-baked'
                        // entries from being prematurely uploaded.
                        Log.i("Dispatch", (new File(thisAdDirectory, "metadata.json")).getAbsolutePath());
                        if ((new File(thisAdDirectory, "metadata.json")).exists()) {
                            String thisAdUUID = thisAdDirectory.getName();
                            // Paginate over the files within thisAdDirectory
                            File[] filesWithinThisAdDirectory = thisAdDirectory.listFiles();

                            Function<File, Boolean> dispatchThenDelete = x -> {
                                Boolean successfullyDispatched = dispatchAdFile(thisParticipantUUID, thisAdUUID, x, "DATA_DONATION_V3");
                                if (successfullyDispatched) {
                                    x.delete();
                                }
                                return successfullyDispatched;
                            };

                            Boolean adContentMetadataDispatched = true;
                            Boolean adMediasDispatched = true;
                            for (File thisDispatchableFile : filesWithinThisAdDirectory) {
                                if (thisDispatchableFile.getName().equals("metadata.json")) {
                                    adContentMetadataDispatched = false;
                                } else {
                                    if (!dispatchThenDelete.apply(thisDispatchableFile)) {
                                        adMediasDispatched = false;
                                    }
                                }
                            }
                            if ((!adContentMetadataDispatched) && (adMediasDispatched)) {
                                dispatchThenDelete.apply(new File(thisAdDirectory, "metadata.json"));
                            }

                            // TODO - order ads correctly
                            // Delete the folder if all constituent files have been uploaded
                            if (thisAdDirectory.listFiles().length == 0) {
                                deleteRecursive(thisAdDirectory);
                            }
                        } else {
                            deleteRecursive(thisAdDirectory);
                            Log.i(TAG, "Deleting incomplete directory.");
                    /*
                    // If the folder is empty, delete it
                    if (thisAdDirectory.listFiles().length == 0) {
                        deleteRecursive(thisAdDirectory);
                        Log.i(TAG, "Deleting empty directory.");
                    } else {
                        // Naively, we might assume that the upload of ad content is
                        Log.i(TAG, "Bypassing upload of ad content as not ready yet.");
                    }*/
                        }
                    }
                    platformRoutineRelayed ++;
                    dataStoreWrite(context, "platformRoutineRelayed", String.valueOf(platformRoutineRelayed));
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Practical Interpretation
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     *
     * This function interprets a recording file's filename and divides it into necessary parts.
     *
     * */
    public static HashMap<String, String> interpretRecordingFileName(String filename) {
        HashMap<String, String> outputHashMap = null;
        try {
            outputHashMap = new HashMap<>();
            outputHashMap.put("filename", filename);
            outputHashMap.put("tags", "");
            outputHashMap.put("timestamp", "");
            outputHashMap.put("UUID", "");
            outputHashMap.put("orientation", "");
            try {
                String[] parts = filename.split("\\."); //  TODO - very real possibility that the code fails here (on malformed, older files)
                //assert (parts.length == 5);
                if (parts.length == 5) {
                    outputHashMap.put("tags", String.join(" ", asList(parts).subList(0, parts.length-4)));
                    outputHashMap.put("timestamp", parts[parts.length-4]);
                    outputHashMap.put("UUID", parts[parts.length-3]);
                    outputHashMap.put("orientation", parts[parts.length-2]);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Error e) {
            // TODO

            e.printStackTrace();
        }
        return outputHashMap;
    }

    private static File appStorageRecordingsDirectory;


    public static JSONXObject yAgnosticCompositeBoundingBox(double upperMostY, double lowerMostY) {
        return (new JSONXObject())
                .set("x1", 0.0)
                .set("x2", 1.0)
                .set("y1", upperMostY)
                .set("y2", lowerMostY)
                .set("cx", 0.5)
                .set("cy", ((upperMostY + lowerMostY) / 2))
                .set("w", 1.0)
                .set("h", lowerMostY - upperMostY)
                .set("confidence", null)
                .set("className", "COMPOSITE");
    }

    public static JSONXObject compositeBoundingBox(double upperMostY, double lowerMostY, double upperMostX, double lowerMostX) {
        return (new JSONXObject())
                .set("x1", lowerMostX)
                .set("x2", upperMostX)
                .set("y1", upperMostY)
                .set("y2", lowerMostY)
                .set("cx", ((upperMostX + lowerMostX) / 2))
                .set("cy", ((upperMostY + lowerMostY) / 2))
                .set("w", upperMostX - lowerMostX)
                .set("h", lowerMostY - upperMostY)
                .set("confidence", null)
                .set("className", "COMPOSITE");
    }

    public static JSONXObject groupAdjacentAds(JSONXObject inferenceResultShallow, List<Integer> retainedFrames, List<String> retainedFramesAsFiles) {
        // Form groups for adjacent retained frames that contain 'Sponsored' text
        // TODO - note that this assumes that any bounding box retrieved from the shallow inference is an indication of sponsorship (which is the general case)
        List<Integer> retainedFramesForDeepInference = new ArrayList<>(); //
        List<String> retainedFramesAsFilesForDeepInference = new ArrayList<>(); //
        List<List<Integer>> groupsOfAdFrames = new ArrayList<>(); //
        List<Integer> currentAdFrames = new ArrayList<>();
        JSONXObject inferencesByFrames = (new JSONXObject((JSONObject) inferenceResultShallow.get("inferencesByFrames"), true)); //
        Integer cursorFrameFile = 0;
        for (Integer thisFrame : retainedFrames) {
            String thisFrameFile = retainedFramesAsFiles.get(cursorFrameFile);
            List<JSONXObject> boundingBoxes = (List<JSONXObject>) inferencesByFrames.get(thisFrame);
            if (boundingBoxes != null) {
                if (!boundingBoxes.isEmpty()) {
                    currentAdFrames.add(thisFrame);
                    retainedFramesAsFilesForDeepInference.add(thisFrameFile);
                    retainedFramesForDeepInference.add(thisFrame);
                }
            }
            // If the current frame has no reading, or we are at the end of the retained frames
            if ((boundingBoxes == null) || (boundingBoxes.isEmpty()) || (thisFrame.equals(retainedFrames.get(retainedFrames.size() - 1)))) {
                if (!currentAdFrames.isEmpty()) {
                    // Dispatch and start anew
                    groupsOfAdFrames.add(new ArrayList<>(currentAdFrames));
                    currentAdFrames = new ArrayList<>();
                }
            }
            cursorFrameFile ++;
        }

        return (new JSONXObject()
                .set("retainedFrames", retainedFramesForDeepInference)
                .set("retainedFramesAsFiles", retainedFramesAsFilesForDeepInference)
                .set("groupsOfAdFrames", groupsOfAdFrames)
                .set("inferencesByFrames", inferencesByFrames));
    }

    public static void deleteScreenRecordingAnalysis(File screenRecordingFile, File screenRecordingAnalysisDirectory, boolean implementedOnAndroid) {
        Log.i(TAG, screenRecordingFile.getName());
        if (implementedOnAndroid) {
            deleteRecursive(screenRecordingAnalysisDirectory);
            screenRecordingFile.delete();
        }
    }

    public static List<JSONXObject> adFrameGroupsToAdObjects(List<JSONXObject> thisAdFrameGroupMetadatasUnseparated) {
        // For each group of frames - go through, and separate them even further, based on overlaps and ad types
        List<JSONXObject> advertisementObjects = new ArrayList<>(0);
        for (JSONXObject thisAdFrameGroupMetadata : thisAdFrameGroupMetadatasUnseparated) {
            List<String> orderedFrames = thisAdFrameGroupMetadata.keys().stream().sorted().collect(Collectors.toList());
            Integer adFrameI = 0;
            JSONXObject currentAdObject = new JSONXObject();
            for (String thisFrame : orderedFrames) {
                boolean retain = true;
                if (adFrameI != 0) {
                    String lastFrame = orderedFrames.get(adFrameI-1);
                    JSONXObject lastFrameObject = ((JSONXObject) thisAdFrameGroupMetadata.get(lastFrame));
                    JSONXObject thisFrameObject = ((JSONXObject) thisAdFrameGroupMetadata.get(thisFrame));
                    // Assess for likeness to previous frame
                    String lastFrameAdType = ((String) lastFrameObject.get("adType"));
                    String thisFrameAdType = ((String) thisFrameObject.get("adType"));
                    if (!Objects.equals(lastFrameAdType, thisFrameAdType)) {
                        retain = false;
                    }

                    if (retain) {
                        if (rectangularAreaOverlap((JSONXObject) ((JSONXObject) lastFrameObject.get("inference")).get("boundingBoxCropped"),
                                (JSONXObject) ((JSONXObject) thisFrameObject.get("inference")).get("boundingBoxCropped")) <= 0.0) {
                            retain = false;
                        }
                    }
                }
                if (!retain) {
                    advertisementObjects.add(new JSONXObject(currentAdObject.internalJSONObject, true));
                    currentAdObject = new JSONXObject();
                }
                currentAdObject.set(thisFrame, thisAdFrameGroupMetadata.get(thisFrame));

                if (adFrameI == orderedFrames.size()-1) {
                    advertisementObjects.add(currentAdObject);
                }
                adFrameI ++;
            }
        }
        return advertisementObjects;
    }

    public static double safeDivisionPassthrough(Double v) {
        try {
            if (Double.isInfinite(v)) {
                return 0.0;
            } else {
                return v;
            }
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    public static double safeDivision(Double a, Double b) {
        try {
            return safeDivisionPassthrough(a / b);
        } catch (Exception ignored) {
            return 0.0;
        }
    }
    public static double safeDivision(Double a, Integer b) {
        try {
            return safeDivisionPassthrough(a / b);
        } catch (Exception ignored) {
            return 0.0;
        }
    }
    public static double safeDivision(Integer a, Double b) {
        try {
            return safeDivisionPassthrough(a / b);
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    public static JSONXObject inferencePassthrough(Context context, Function<JSONXObject, JSONXObject> objectDetectorFunction,
               String modelIdentifier, JSONXObject groupedAdsObject, File screenRecordingFile, File screenRecordingAnalysisDirectory) throws Exception {

        dataStoreWrite(context, "platformRoutineState", "PERFORMING ANALYSIS");
        String thisModelIdentifier = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Log.i(TAG, "Applying newer models");
            thisModelIdentifier = "float32_"+modelIdentifier+"_int8.tflite";
        } else {
            Log.i(TAG, "Applying older models");
            thisModelIdentifier = "float16_"+modelIdentifier+".tflite";
        }

        JSONXObject result = objectDetectorFunction.apply((new JSONXObject())
                    .set("context", context)
                    .set("analysisDirectory", screenRecordingAnalysisDirectory)
                    .set("thisScreenRecordingFile", screenRecordingFile)
                    .set("retainedFrameFiles", groupedAdsObject.get("retainedFramesAsFiles"))
                    .set("retainedFrames", groupedAdsObject.get("retainedFrames"))
                    //.set("modelName", "float16_"+modelIdentifier+".tflite")
                    .set("modelName", thisModelIdentifier)
                    .set("thisCase", ((modelIdentifier.contains("sponsored")) ? "Shallow" : "Deep"))
            );
        if (result == null) { persistThread(context, TAG); }
        return result;
    }



    public static boolean prepareForDispatch(Context context, File rootDirectory, HashMap<String, String> thisInterpretation,
                                          List<JSONXObject> advertisementObjects, File screenRecordingAnalysisDirectory,
                                          JSONXObject thisComprehensiveReading, JSONXObject inferenceResultShallow,
                                          JSONXObject inferenceResultDeep, String platform) throws Exception {
        // Prepare for dispatch
        boolean success = true;
        Integer w = null;
        Integer h = null;
        File dispatchDirectory = new File(rootDirectory, "dispatch");
        File screenRecordingDispatchDirectory = new File(dispatchDirectory, thisInterpretation.get("filename") + ".dispatch");
        createDirectory(screenRecordingDispatchDirectory, true); // Force re-creation to avoid doubling up on submissions
        for (JSONXObject thisAdFrameGroupMetadata : advertisementObjects) {
            persistThread(context, TAG);
            try {
                //
                // Procure a UUID for this advertisement
                String advertisementUUID = UUID.randomUUID().toString();
                File advertisementDirectory = new File(screenRecordingDispatchDirectory, advertisementUUID);
                makeDirectory(advertisementDirectory);
                for (String thisFrame : thisAdFrameGroupMetadata.keys()) {
                    JSONXObject boundingBoxCropped = (JSONXObject) ((JSONXObject) ((JSONXObject) thisAdFrameGroupMetadata.get(thisFrame)).get("inference")).get("boundingBoxCropped");
                    // Retrieve the frame to crop
                    Bitmap bitmapToCrop = BitmapFactory.decodeFile(new File(new File(screenRecordingAnalysisDirectory, "frames"), thisFrame + ".jpg").getAbsolutePath());
                    w = bitmapToCrop.getWidth();
                    h = bitmapToCrop.getHeight();
                    Integer cropX = Math.min((int) Math.floor(((Double) boundingBoxCropped.get("x1")) * w), w-1);
                    Integer cropY = Math.min((int) Math.floor(((Double) boundingBoxCropped.get("y1")) * h), h-1);
                    Integer cropW = Math.max(Math.min((int) Math.floor(((Double) boundingBoxCropped.get("w")) * w), (w - cropX)),1);
                    Integer cropH = Math.max(Math.min((int) Math.floor(((Double) boundingBoxCropped.get("h")) * h), (h - cropY)),1);
                    saveBitmap(Bitmap.createBitmap(bitmapToCrop, cropX, cropY, cropW, cropH), new File(advertisementDirectory, thisFrame + ".jpg").getAbsolutePath());
                }
                // frames go into metadata as key
                // observer uuid
                // time of screen recording
                // basic stats about fps and inference
                // platform from which details are retriveed
                String observerID = hardFixObserverIDRead(context);

                JSONXObject dispatchObject = (new JSONXObject())
                        .set("observerUUID", observerID)
                        .set("observedAt", thisInterpretation.get("timestamp"))
                        .set("preparedAt", ((int) Math.floor(System.currentTimeMillis() / (double) 1000)))
                        .set("platform", platform)
                        .set("systemInformation", (new JSONXObject())
                                .set("operatingSystemVersion", System.getProperty("os.version") + "(" + Build.VERSION.INCREMENTAL + ")")
                                .set("apiLevel", Build.VERSION.SDK_INT)
                                .set("device", Build.DEVICE)
                                .set("screenDimensions", (new JSONXObject())
                                        .set("w", w.toString())
                                        .set("h", h.toString())
                                )
                                .set("model", Build.MODEL + " (" + Build.PRODUCT + ")")
                        )
                        .set("recordingInformation", (new JSONXObject())
                                .set("FPS", thisComprehensiveReading.get("fps"))
                                .set("nFrames", thisComprehensiveReading.get("nFrames"))
                                .set("durationInMilliseconds", thisComprehensiveReading.get("durationInMilliseconds"))
                        )
                        .set("processingStatistics", (new JSONXObject())
                                .set("comprehensiveReading", (new JSONXObject())
                                        .set("nFramesSampled", thisComprehensiveReading.get("nFramesSampled"))
                                        .set("nFramesSampledAtLastCall", thisComprehensiveReading.get("nFramesSampledAtLastCall"))
                                        .set("elapsedTimePerFrameAtLastCall", safeDivision((Double) thisComprehensiveReading.get("elapsedTime") , (Integer) thisComprehensiveReading.get("nFramesSampled")))
                                        .set("elapsedTimeAtLastCall", thisComprehensiveReading.get("elapsedTime"))


                                )
                                .set("inferenceShallow", (new JSONXObject())
                                        .set("nFramesAnalyzed", inferenceResultShallow.get("nFramesAnalyzed"))
                                        .set("elapsedTimePerFrame", safeDivision((Double) inferenceResultShallow.get("elapsedTime") * 1000, (Integer) inferenceResultShallow.get("nFramesAnalyzed")))
                                        .set("elapsedTime", (Double) inferenceResultShallow.get("elapsedTime") * 1000))
                                .set("inferenceDeep", (new JSONXObject())
                                        .set("nFramesAnalyzed", inferenceResultDeep.get("nFramesAnalyzed"))
                                        .set("elapsedTimePerFrame", safeDivision((Double) inferenceResultDeep.get("elapsedTime") * 1000 , (Integer) inferenceResultDeep.get("nFramesAnalyzed")))
                                        .set("elapsedTime", (Double) inferenceResultDeep.get("elapsedTime") * 1000))
                        )
                        .set("frameMetadata", thisAdFrameGroupMetadata);

                writeToJSON((new File(advertisementDirectory, "metadata.json")), dispatchObject.internalJSONObject);
            } catch (Exception e) {
                e.printStackTrace();

                success = false;
            }
        }
        return success;
    }

    public static HashMap<String, File> detailDirectoryStructure(File rootDirectory) {
        HashMap directoryStructure = (new HashMap<>());
        directoryStructure.put("root", rootDirectory);
        directoryStructure.put("videos", (new File (rootDirectory.getAbsolutePath(), "videos")));
        directoryStructure.put("analysis", (new File (rootDirectory.getAbsolutePath(), "analysis")));
        directoryStructure.put("dispatch", (new File (rootDirectory.getAbsolutePath(), "dispatch")));

        return directoryStructure;
    }

    public static void evaluationPostMethod(Context context, List<JSONXObject> thisAdFrameGroupMetadatasUnseparated, HashMap<String, String> thisInterpretation,
                                JSONXObject thisComprehensiveReading, Boolean implementedOnAndroid, JSONXObject inferenceResultShallow, JSONXObject inferenceResultDeep,
                                File rootDirectory, File screenRecordingAnalysisDirectory, File screenRecordingFile, String platform) throws Exception {
        // Separate ads based on ad type and non-overlaps
        List<JSONXObject> advertisementObjects = adFrameGroupsToAdObjects(thisAdFrameGroupMetadatasUnseparated);

        // TODO - note : we don't retain data after the above block, as the logic execution is almost instantaneous

        boolean successfulPreparation = prepareForDispatch(context,  rootDirectory,
                thisInterpretation, advertisementObjects,  screenRecordingAnalysisDirectory,
                thisComprehensiveReading,  inferenceResultShallow, inferenceResultDeep,  platform);

        if (successfulPreparation) {
            // Completion is concluded by deleting the screen recording, and the analysis folder
            deleteScreenRecordingAnalysis(screenRecordingFile, screenRecordingAnalysisDirectory, implementedOnAndroid);
        }
    }

    public static void persistThread(Context context, String thisTag) throws Exception {
        if (Thread.interrupted()) {
            Log.i(thisTag, "Thread exited...");
            throw new Exception("Deliberate thread interruption");
        }
    }

    public static void platformInterpretationRoutine(Context context, File rootDirectory,
                                                     Function<JSONXObject, JSONXObject> getVideoMetadataFunction,
                                                     Function<JSONXObject, Bitmap> frameGrabFunction, Boolean implementedOnAndroid, Function<JSONXObject, JSONXObject> objectDetectorFunction) throws Exception {

        dataStoreWrite(context, "platformRoutineState", "STARTING");
        dataStoreWrite(context, "platformRoutineToAnalyze", "0");
        dataStoreWrite(context, "platformRoutineToRelay", "0");
        dataStoreWrite(context, "platformRoutineRelayed", "0");
        Integer platformRoutineAnalyzed = 0;
        dataStoreWrite(context, "platformRoutineAnalyzed", platformRoutineAnalyzed.toString());
        List<HashMap<String,String>> recordingsClassified = new ArrayList<>();
        List<HashMap<String,String>> recordingsToDelete = new ArrayList<>();

        Log.i(TAG, "Attempting dispatch...");
        appStorageRecordingsDirectory = (new File (rootDirectory.getAbsolutePath(), "videos"));
        File dispatchDirectory = (new File (rootDirectory.getAbsolutePath(), "dispatch"));

        String observerID = hardFixObserverIDRead(context);

        if (observerID == null) {
            return;
        }

        for (File thisFile : appStorageRecordingsDirectory.listFiles()) {
            persistThread(context, TAG);
            if (thisFile.getAbsolutePath().contains("unclassified")) {
                thisFile.delete();
            }
        }

        try {
            dataStoreWrite(context, "platformRoutineState", "RELAYING DATA");
            dispatchAdsV2(context, observerID, dispatchDirectory);
        } catch (Exception e) {
            e.printStackTrace(); // TODO
        }

        for (File thisFile : appStorageRecordingsDirectory.listFiles()) {
            Log.i(TAG, thisFile.getAbsolutePath());
            try {
                HashMap<String, String> thisInterpretation = interpretRecordingFileName(thisFile.getName());
                if (thisInterpretation != null) {
                    // TODO - adapt this to a pre-condition for all platforms
                    if (
                            (((!thisInterpretation.containsKey("orientation")) || (Objects.equals(thisInterpretation.get("orientation"), "landscape"))) && (thisInterpretation.get("tags").equals("com_zhiliaoapp_musically"))) ||
                            (((!thisInterpretation.containsKey("orientation")) || (Objects.equals(thisInterpretation.get("orientation"), "landscape"))) && (thisInterpretation.get("tags").equals("com_facebook_katana"))) ||
                            (((!thisInterpretation.containsKey("orientation")) || (Objects.equals(thisInterpretation.get("orientation"), "landscape"))) && (thisInterpretation.get("tags").equals("com_facebook_lite"))) ||
                            (((!thisInterpretation.containsKey("orientation")) || (Objects.equals(thisInterpretation.get("orientation"), "landscape"))) && (thisInterpretation.get("tags").equals("com_instagram_android"))) ||
                            ((!thisInterpretation.containsKey("orientation")) && (Objects.equals(thisInterpretation.get("tags"), "com_google_android_youtube")))

                    )
                    {
                        recordingsToDelete.add(thisInterpretation);
                    } else {
                        if (!Objects.equals(thisInterpretation.get("tags"), "unclassified")) {
                            recordingsClassified.add(thisInterpretation);
                        }
                    }
                } else {
                    recordingsToDelete.add(thisInterpretation);
                }
            } catch (Exception e) {
                e.printStackTrace(); // TODO
            }
        }


        dataStoreWrite(context, "platformRoutineToAnalyze", String.valueOf(recordingsClassified.size()));
        Log.i(TAG, recordingsClassified.toString());
        // Run the comprehensive sampling process
        // TODO - sort alphabetically to stop randomly starting differing entries
        for (HashMap<String, String> thisInterpretation : recordingsClassified) {

            File analysisDirectory = new File(rootDirectory, "analysis");
            File screenRecordingAnalysisDirectory = new File(analysisDirectory, thisInterpretation.get("filename") + ".analysis");

            // TODO - fresh passes upstream have to trigger fresh passes downstream

            List<String> targetedPlatforms = Arrays.asList("com_zhiliaoapp_musically", "com_facebook_katana", "com_facebook_lite", "com_instagram_android", "com_google_android_youtube");

            if (targetedPlatforms.contains(thisInterpretation.get("tags"))) {
                File screenRecordingFile = (new File(appStorageRecordingsDirectory, thisInterpretation.get("filename")));
                JSONXObject thisComprehensiveReading = new JSONXObject();
                try {
                    dataStoreWrite(context, "platformRoutineState", "SAMPLING IMAGERY");
                    thisComprehensiveReading = basicReading(context, rootDirectory, screenRecordingAnalysisDirectory,
                            screenRecordingFile, getVideoMetadataFunction, frameGrabFunction);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (screenRecordingFile.length() < 2000) {
                        screenRecordingFile.delete(); // TODO - Due to empty file size - make more stringent
                    }
                }

                if (thisComprehensiveReading.has("malformedFlag") && ((Boolean) thisComprehensiveReading.get("malformedFlag"))) {
                    deleteScreenRecordingAnalysis(screenRecordingFile, screenRecordingAnalysisDirectory, implementedOnAndroid);
                } else {
                    if ((Objects.equals(thisInterpretation.get("tags"), "com_facebook_katana")) || (Objects.equals(thisInterpretation.get("tags"), "com_facebook_lite"))) {
                        evaluateFacebookAd(context, rootDirectory, thisInterpretation,
                                objectDetectorFunction, thisComprehensiveReading, implementedOnAndroid);
                    }

                    if (Objects.equals(thisInterpretation.get("tags"), "com_zhiliaoapp_musically")) {
                        evaluateTikTokAd(context, rootDirectory, thisInterpretation,
                                objectDetectorFunction, thisComprehensiveReading, implementedOnAndroid);
                    }

                    if (Objects.equals(thisInterpretation.get("tags"), "com_instagram_android")) {
                        evaluateInstagramAd(context, rootDirectory, thisInterpretation,
                                objectDetectorFunction, thisComprehensiveReading, implementedOnAndroid);
                    }
                    if (Objects.equals(thisInterpretation.get("tags"), "com_google_android_youtube")) {
                        evaluateYoutubeAd(context, rootDirectory, thisInterpretation,
                                objectDetectorFunction, thisComprehensiveReading, implementedOnAndroid);
                    }
                }

            }
            platformRoutineAnalyzed ++;
            dataStoreWrite(context, "platformRoutineAnalyzed", platformRoutineAnalyzed.toString());

        }

        // Do again at end of process
        try {
            if (implementedOnAndroid) {
                dataStoreWrite(context, "platformRoutineState", "RELAYING DATA");
                dispatchAdsV2(context, observerID, dispatchDirectory);
            }
        } catch (Exception e) {
            e.printStackTrace(); // TODO
        }
        Log.i(TAG, "Completed routine!");
        dataStoreWrite(context, "platformRoutineState", "COMPLETE");
        dataStoreWrite(context, "platformRoutineToAnalyze", "0");
        dataStoreWrite(context, "platformRoutineAnalyzed", "0");
    }



}
