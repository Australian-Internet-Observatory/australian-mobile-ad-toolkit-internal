package com.adms.australianmobileadtoolkit.interpreter.platform;

import static com.adms.australianmobileadtoolkit.Arguments.A;
import static com.adms.australianmobileadtoolkit.Arguments.Args;
import static com.adms.australianmobileadtoolkit.Common.makeDirectory;
import static com.adms.australianmobileadtoolkit.Common.optionalGetDouble;
import static com.adms.australianmobileadtoolkit.appSettings.DEBUG;
import static com.adms.australianmobileadtoolkit.appSettings.SHARED_PREFERENCE_OBSERVER_ID_DEFAULT_VALUE;
import static com.adms.australianmobileadtoolkit.appSettings.sharedPreferenceGet;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Facebook.comprehensiveReading;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Facebook.facebookInterpretation;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Facebook.fitterGenerate;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Facebook.facebookGenerateQuickReading;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Facebook.retrieveReferenceStencilsPictograms;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Tiktok.tiktokGenerateQuickReading;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Tiktok.tiktokInterpretation;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.colourPalette;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import static java.util.Arrays.asList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import com.adms.australianmobileadtoolkit.Arguments;
import com.adms.australianmobileadtoolkit.JSONXObject;
import com.adms.australianmobileadtoolkit.R;
import com.adms.australianmobileadtoolkit.appSettings;
import com.adms.australianmobileadtoolkit.checkPoint;
import com.adms.australianmobileadtoolkit.interpreter.detector.BoundingBox;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.collections4.IteratorUtils;
import org.json.JSONArray;
import org.json.JSONException;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


class SortByLastFrame implements Comparator<JSONObject>
{
    public int compare(JSONObject a, JSONObject b)
    {
        try {
            return (((Integer) a.get("lastFrame")) - ((Integer) b.get("lastFrame")));
        } catch (Exception e) {
            Platform.logger.error(e);
            return 0;
        }
    }
}

class SortByXLastFrame implements Comparator<JSONXObject>
{
    public int compare(JSONXObject a, JSONXObject b)
    {
        try {
            return (((Integer) a.get("lastFrame")) - ((Integer) b.get("lastFrame")));
        } catch (Exception e) {
            Platform.logger.error(e);
            return 0;
        }
    }
}

public class Platform {

    public static final Logger logger = LogManager.getLogger(Platform.class);


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

    public static Pair<Integer, Integer> orderRange(List<Integer> thisRange) {
        Integer thisMin = Math.min(thisRange.get(0), thisRange.get(1));
        Integer thisMax = Math.max(thisRange.get(0), thisRange.get(1));
        return new Pair(thisMin, thisMax);
    }

    public static Integer forceToRange(int x, int boundA, int boundB) {
        int min = Math.min(boundA, boundB);
        int max = Math.max(boundA, boundB);
        if (x < min) { return min; }
        if (x > max) { return max; }
        return x;
    }

    public static Double getStandardDeviation(List<Integer> thisArray) {
        return Math.sqrt(thisArray.stream()
                .map(x -> Math.pow(x - optionalGetDouble(thisArray.stream().mapToDouble(y -> y).average()), 2))
                .mapToDouble(x -> x).sum() / thisArray.size() );
    }

    public static Double getStandardDeviationD(List<Double> thisArray) {
        return Math.sqrt(thisArray.stream()
                .map(x -> Math.pow(x - optionalGetDouble(thisArray.stream().mapToDouble(y -> y).average()), 2))
                .mapToDouble(x -> x).sum() / thisArray.size() );
    }


    public static Double getStandardDeviationDouble(List<Double> thisArray) {
        return Math.sqrt(thisArray.stream()
                .map(x -> Math.pow(x - optionalGetDouble(thisArray.stream().mapToDouble(y -> y).average()), 2))
                .mapToDouble(x -> x).sum() / thisArray.size() );
    }

    public static List<List<Integer>> discreteIntervalsToRanges(Integer interval, List<Integer> intervals) {
        List<List<Integer>> intervalsSortedAsRanges = new ArrayList<>();
        List<Integer> intervalsSorted = intervals.stream().sorted().collect(Collectors.toList());

        List<Integer> intervalsSortedFinal = intervalsSorted;
        // adjacent to another interval?
        intervalsSorted = IntStream.range(0, intervalsSorted.size()).filter(i ->
                (((i != 0) && (Math.abs(intervalsSortedFinal.get(i-1) - intervalsSortedFinal.get(i)) <= interval)) ||
                        ((i != (intervalsSortedFinal.size()-1)) && (Math.abs(intervalsSortedFinal.get(i+1) - intervalsSortedFinal.get(i)) <= interval)))
        ).map(x -> intervalsSortedFinal.get(x)).boxed().collect(Collectors.toList());

        List<Integer> currentRange = null;
        for (Integer thisRangeValue : intervalsSorted) {
            if (currentRange == null) {
                currentRange = asList(thisRangeValue);
            } else {
                if (Math.abs(currentRange.get(currentRange.size() - 1) - thisRangeValue) <= interval) {
                    currentRange = asList(currentRange.get(0), thisRangeValue);
                    if (thisRangeValue.equals(intervalsSorted.get(intervalsSorted.size() - 1))) {
                        intervalsSortedAsRanges.add(new ArrayList<>(currentRange));
                    }
                } else {
                    intervalsSortedAsRanges.add(new ArrayList<>(currentRange));
                    currentRange = asList(thisRangeValue);
                    // We don't have to worry about retaining the last value to come into range as the code block is never reached
                    // where it is incorrectly discarded
                }
            }
        }

        // Apply an interval forward and back to each range
        List<List<Integer>> consistentListAsRangesUpdated = new ArrayList<>();
        System.out.println(intervalsSorted);
        System.out.println(intervalsSortedAsRanges);
        for (List<Integer> aRange : intervalsSortedAsRanges) {
            consistentListAsRangesUpdated.add(asList(aRange.get(0)-interval, aRange.get(1)+interval));
        }
        return consistentListAsRangesUpdated;
    }

    // subtract b from a
    public static List<List<Integer>> subtractRanges(List<Integer> a, List<Integer> b) {
        // if a and b partially overlap, return the part of a that is not within b
        // if a is entirely within b, don't return anything
        // if b is entirely within a, return the disjoint parts within a

        Integer minimumRangeSize = 2;
        Integer aMin = Math.min(a.get(0), a.get(1));
        Integer aMax = Math.max(a.get(0), a.get(1));
        Integer bMin = Math.min(b.get(0), b.get(1));
        Integer bMax = Math.max(b.get(0), b.get(1));

        Boolean partialOverlapASmaller = ((aMin < bMin) && (aMax > bMin)); // partial overlap - a smaller
        Boolean partialOverlapBSmaller = ((bMin < aMin) && (bMax > aMin)); // partial overlap - b smaller

        Boolean aInsideOrEqualToB = ((aMin >= bMin) && (aMax <= bMax)); // a inside or equal to b
        Boolean bInsideOrEqualToA = ((bMin >= aMin) && (bMax <= aMax)); // b inside or equal to a

        List<List<Integer>> output = new ArrayList<>();

        if (partialOverlapASmaller) {
            output.add(asList(aMin, bMin));
        } else
        if (partialOverlapBSmaller) {
            output.add(asList(bMax, aMax));
        } else
        if (aInsideOrEqualToB) {
            // Do not add anything
        } else
        if (bInsideOrEqualToA) {
            output.add(asList(aMin, bMin));
            output.add(asList(bMax, aMax));
        }
        output = output.stream().filter(x -> Math.abs(x.get(0) - x.get(1)) >= minimumRangeSize).collect(Collectors.toList());
        return output;
    }

    public static Integer boundToDimensions(int x, int lb, int up) {
        return Math.max(Math.min(x, up), lb);
    }

    public static Integer randomInRange(Random r, Pair<Integer, Integer> bounds) {
        int min = bounds.first;
        int max = bounds.second;
        return (r.nextInt(max - min) + min);
    }

    // given a position and the stride that goes with it, define the lower and upper bound for it
    public static Pair<Integer, Integer> boundsOnStride(Integer stride, Integer position, Integer lb, Integer maxPosition) {
        Integer lower = boundToDimensions((int) Math.floor(position - (stride / (double) 2)), lb, maxPosition);
        Integer upper = boundToDimensions(lower+stride, lb, maxPosition);
        return new Pair(lower, upper);
    }

    // given a scalar, and a number of samples to derive for said scalar, generate the samples, and teh stride between them
    public static Pair<Integer, List<Integer>> generateSamplePositions(int n, int lb, int scalar) {
        Integer stride = (int) Math.floor(scalar / (double) n);
        Integer startingPosition = (int) Math.floor(stride / (double) 2);
        List<Integer> toReturn = new ArrayList<>();

        for (int i = 0; i <= (n-1); i ++) {
            toReturn.add(boundToDimensions(startingPosition + (stride * i), lb, scalar-1));
        }
        return new Pair(stride, toReturn);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Image-Related Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static int dominantColourInPalette(HashMap<String, Integer> thisColourPalette) {
        return Color.parseColor(Collections.max(thisColourPalette.entrySet(), Map.Entry.comparingByValue()).getKey());
    }

    /*
     *
     * This function determines the average colour within a set of provided colours
     *
     * */
    public static int averageColours(Arguments args) {
        List<Integer> colors = (List<Integer>) args.get("colors", 0);
        List<Integer> reds = colors.stream().map(x -> (int) Math.pow(Color.red(x), 2)).collect(Collectors.toList());
        List<Integer> greens = colors.stream().map(x -> (int) Math.pow(Color.green(x), 2)).collect(Collectors.toList());
        List<Integer> blues = colors.stream().map(x -> (int) Math.pow(Color.blue(x), 2)).collect(Collectors.toList());
        int averageRed = (int) Math.round(Math.sqrt(reds.stream().mapToDouble(x -> x).sum() / reds.size()));
        int averageGreen = (int) Math.round(Math.sqrt(greens.stream().mapToDouble(x -> x).sum() / greens.size()));
        int averageBlue = (int) Math.round(Math.sqrt(blues.stream().mapToDouble(x -> x).sum() / blues.size()));
        return Color.rgb( averageRed, averageGreen, averageBlue);
    }

    /*
     *
     * This function generates a sample of pixels over an image
     *
     *     Set a ratio bound on the image (from which the whitespace pixel is derived
     *
     * TODO - annotate
     *
     * */
    public static int[] generatePixelSample(Arguments args) {
        Bitmap thisBitmap = (Bitmap) args.get("bitmap", null);

        Integer sampleWidth = (Integer) args.get("sampleWidth", null);
        Integer sampleHeight = (Integer) args.get("sampleHeight", null);
        Integer sampleOffsetX = (Integer) args.get("sampleOffsetX", null);
        Integer sampleOffsetY = (Integer) args.get("sampleOffsetY", null);

        // Deter
        Integer strides = (Integer) args.get("strides", null);
        int strideX = Math.toIntExact(Math.round(sampleWidth / (double) strides));
        int strideY = Math.toIntExact(Math.round(sampleHeight / (double) strides));
        // Retrieve a sample of pixels from the image
        int ii = 0;
        //System.out.println(strides*strides);
        //System.out.println((sampleOffsetX + sampleWidth));
        //System.out.println(strideX);
        int[] pixels = new int[strides*strides];
        for (int xx = sampleOffsetX; xx < (sampleOffsetX + sampleWidth); xx += strideX) {
            for (int yy = sampleOffsetY; yy < (sampleOffsetY + sampleHeight); yy += strideY) {
                if (ii < (strides*strides)) {
                    pixels[ii] = thisBitmap.getPixel(xx, yy);
                }
                ii++;
            }
        }

        return pixels;
    }

    /*
     *
     * This function determines what the dominant whitespace pixel is of an image, and can be used to consequently determine
     * exposure.
     *
     * Note: We don't know if the image is of any social media platform beforehand, however we go about analysing it anyway
     *
     * */
    public static int whitespacePixelFromImage(Arguments args) {
        Bitmap bitmap = (Bitmap) args.get("bitmap", null);
        // Set a ratio bound on the image (from which the whitespace pixel is derived
        Integer strides = 10; // TODO - confident with 10 - will work with 3 experimentally
        Integer sampleWidth = Math.max(strides, Math.toIntExact(Math.round(bitmap.getWidth() * 0.5)));
        Integer sampleHeight = Math.max(strides, Math.toIntExact(Math.round(bitmap.getHeight() * 0.1)));
        Integer sampleOffsetX = Math.toIntExact(Math.round(bitmap.getWidth() * 0.25));
        Integer sampleOffsetY = Math.toIntExact(Math.round(0.0));
        Double linkingThreshold = 0.3;

        int[] pixels = generatePixelSample(Args(
                A("bitmap", bitmap),
                A("sampleWidth", sampleWidth),
                A("sampleHeight", sampleHeight),
                A("sampleOffsetX", sampleOffsetX),
                A("sampleOffsetY", sampleOffsetY),
                A("strides", strides)
        ));
        return dominantColourInPalette(colourPalette(Args(A("sample", pixels), A("threshold", linkingThreshold))));
    }

    public static Bitmap overlayBitmaps(Bitmap bmp1, Bitmap bmp2, int x, int y) {
        Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp1, 0,0, null);
        canvas.drawBitmap(bmp2, x,y, null);
        return bmOverlay;
    }

    public static void saveBitmap(Bitmap bmp, String fname) {
        try (FileOutputStream out = new FileOutputStream(fname)) {
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
        } catch (IOException e) {
            logger.error(e);
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
            logger.error(e);
        }
    }

    public static JSONObject readJSONFromFile(File thisFile) {
        try {
            return new JSONObject(new String(Files.readAllBytes(Paths.get(thisFile.getAbsolutePath()))));
        } catch (Exception e) {

            logger.error(e);
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
            logger.error(e);
        }
    }

    public static void printJSON(Object thisToJSON) {
        try {
            System.out.println((new ObjectMapper().writer().withDefaultPrettyPrinter()).writeValueAsString(thisToJSON));
        } catch (Exception e) {
            logger.error(e);
            try {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(thisToJSON);
                System.out.println(json);
            } catch (Exception e2) {
                logger.error(e2); }
        }
    }

    public static List<Object> JSONArrayToList(JSONArray thisJSONArray) {
        List<Object> thisList = new ArrayList<>();
        if (thisJSONArray != null) {
            for (int i=0;i<thisJSONArray.length();i++){
                try {
                    thisList.add(thisJSONArray.get(i));
                } catch (Exception e) {}
            }
        }
        return thisList;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Dispatch Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // TODO - get some logging
    public static Boolean dispatchAdFile(String thisParticipantUUID, String thisAdUUID, File thisDispatchableFile) {
        Boolean successfullyDispatched = false;
        // Declare the AWS Lambda endpoint
        String urlParam = appSettings.AWS_LAMBDA_ENDPOINT;
        // The exported image quality
        int imageExportQuality = appSettings.IMAGE_EXPORT_QUALITY;
        // The identifier for submitting data donations
        String identifierDataDonation = appSettings.IDENTIFIER_DATA_DONATION_V2;
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
            logger.error(e);
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
                    logger.error(e); }
                thisInputStream.close();
            }

        } catch (Exception e) {
            successfullyDispatched = false;
            logger.error(e);
        }
        return successfullyDispatched;
    }

    public static List<File> generateOrderedAdFiles(File adsFromDispatchDirectory) {
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

    public static void dispatchAds(String thisParticipantUUID, File adsFromDispatchDirectory) {
        Integer numberOfAdsToDelete = 50;
        Integer maximumNumberOfHeldAds = 300;

        // Order the ads, as they will be dipatched (or deleted) in order
        List<File> adsFromDispatchDirectoryFiles = generateOrderedAdFiles(adsFromDispatchDirectory);

        // Prior to dispatching ads, a check is done to ensure that the folder is not overflowing
        if (adsFromDispatchDirectoryFiles.size() > maximumNumberOfHeldAds) {
            for (File thisFile : adsFromDispatchDirectoryFiles.subList(0, numberOfAdsToDelete)) {
                thisFile.delete();
            }
        }

        // Retrieve (and order) the ad files again
        adsFromDispatchDirectoryFiles = generateOrderedAdFiles(adsFromDispatchDirectory);

        if ((adsFromDispatchDirectory != null) && (adsFromDispatchDirectoryFiles != null)) {
            for (File thisAdDirectory : adsFromDispatchDirectoryFiles) {

                // A dispatch can only begin when the adContent file has been submitted - this prevents 'half-baked'
                // entries from being prematurely uploaded.
                if ((new File(thisAdDirectory, "adContent.json")).exists()) {
                    String thisAdUUID = thisAdDirectory.getName();
                    // Paginate over the files within thisAdDirectory
                    File[] filesWithinThisAdDirectory = thisAdDirectory.listFiles();

                    Function<File, Boolean> dispatchThenDelete = x -> {
                        Boolean successfullyDispatched = dispatchAdFile(thisParticipantUUID, thisAdUUID, x);
                        if (successfullyDispatched) {
                            x.delete();
                        }
                        return successfullyDispatched;
                    };

                    Boolean adContentMetadataDispatched = true;
                    Boolean adMediasDispatched = true;
                    for (File thisDispatchableFile : filesWithinThisAdDirectory) {
                        if (thisDispatchableFile.getName().equals("adContent.json")) {
                            adContentMetadataDispatched = false;
                        } else {
                            if (!dispatchThenDelete.apply(thisDispatchableFile)) {
                                adMediasDispatched = false;
                            }
                        }
                    }
                    if ((!adContentMetadataDispatched) && (adMediasDispatched)) {
                        dispatchThenDelete.apply(new File(thisAdDirectory, "adContent.json"));
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
            try {
                String[] parts = filename.split("\\."); //  TODO - very real possibility that the code fails here (on malformed, older files)
                assert (parts.length == 5);
                outputHashMap.put("tags", String.join(" ", asList(parts).subList(0, parts.length-4)));
                outputHashMap.put("timestamp", parts[parts.length-4]);
                outputHashMap.put("UUID", parts[parts.length-3]);
                outputHashMap.put("orientation", parts[parts.length-2]);
            } catch (Exception e) {
                logger.error(e);
            }
        } catch (Error e) {
            // TODO

            logger.error(e);
        }
        return outputHashMap;
    }

    private static File appStorageRecordingsDirectory;

    /*
    *
    * This is the entry point method for all advanced functionality within the app - all on-device image analysis
    * processing for the identification of ads is facilitated here.
    *
    * */
    public static void platformInterpretationRoutineB(Context context, File rootDirectory,
                                                     Function<JSONXObject, JSONXObject> getVideoMetadataFunction,
                                                     Function<JSONXObject, Bitmap> frameGrabFunction, Boolean implementedOnAndroid) {

        String observerID = sharedPreferenceGet(context, "SHARED_PREFERENCE_OBSERVER_ID", SHARED_PREFERENCE_OBSERVER_ID_DEFAULT_VALUE);


        JSONXObject debug = new JSONXObject();
        File debugDirectory = new File(rootDirectory, "debug");
        appStorageRecordingsDirectory = (new File (rootDirectory.getAbsolutePath(), "videos"));
        File adsFromDispatchDirectory = new File(rootDirectory, "adsToDispatch");

        // warm-start directory
        //
        // the warm-start directory helps us determine


        // Create the 'holding' directory for identified ads (if it hasn't already been created).
        createDirectory(adsFromDispatchDirectory, false);

        // Dispatch already identified ads to our server
        dispatchAds(observerID, adsFromDispatchDirectory);

        // In some debugging cases, we inject pre-recorded videos directly into the device.
        if (DEBUG) {
            createDirectory(debugDirectory, true);
            createDirectory(appStorageRecordingsDirectory, true);
            // Copy across files from the input folder within the 'injected' debug folder
            File injectedDirectory = (new File(debugDirectory, "input"));
            for (File injectedFile : Objects.requireNonNull(injectedDirectory.listFiles())) {
                if (!injectedFile.getName().equals("put_test_videos_here.mp4")) {
                    File outputFile = new File(appStorageRecordingsDirectory, injectedFile.getName().replaceAll("_","."));
                    try {
                        Files.copy(Paths.get(injectedFile.getAbsolutePath()), Paths.get(outputFile.getAbsolutePath()));
                    } catch (Exception ignored) { }
                }
            }
        }

        // Assess the ads for upload


        // Platform-specific resources are loaded in here
        JSONObject fitterFacebookAdHeader = fitterGenerate(((BitmapDrawable)context.getResources().getDrawable(R.drawable.facebook_ad_header_stencil)).getBitmap());
        HashMap<String, Object> pictogramsReference = retrieveReferenceStencilsPictograms(context);
        // TODO - add your platform specific resources here
        // ...



        // Gather and assess the files within the recordings folder - filter out unwanted videos,
        // and then relabel them as 'unclassified' videos for future steps
        List<HashMap<String,String>> recordingsUnclassified = new ArrayList<>();
        List<HashMap<String,String>> recordingsClassified = new ArrayList<>();
        List<HashMap<String,String>> recordingsToDelete = new ArrayList<>();
        File[] filesInRecordingsDirectory = appStorageRecordingsDirectory.listFiles();
        try {
            assert filesInRecordingsDirectory != null;
            Log.i(TAG, Arrays.toString(filesInRecordingsDirectory));
            for (File thisFile : filesInRecordingsDirectory) {
                try {
                    HashMap<String, String> thisInterpretation = interpretRecordingFileName(thisFile.getName());
                    if (thisInterpretation != null) {
                        if ((!thisInterpretation.containsKey("orientation")) || (Objects.equals(thisInterpretation.get("orientation"), "landscape"))) {
                            recordingsToDelete.add(thisInterpretation);
                        } else {
                            if (Objects.equals(thisInterpretation.get("tags"), "unclassified")) {
                                recordingsUnclassified.add(thisInterpretation);
                            } else {
                                recordingsClassified.add(thisInterpretation);
                            }
                        }
                    } else {
                        recordingsToDelete.add(thisInterpretation);
                    }
                } catch (Exception e) {
                    logger.error(e);
                }
            }
        } catch (AssertionError e) {
            logger.error(e);
        }

        // Delete all landscape videos (we do not analyse these at this stage)
        for (HashMap<String, String> thisInterpretation: recordingsToDelete) {
            (new File(appStorageRecordingsDirectory, thisInterpretation.get("filename"))).delete();
        }

        if (!implementedOnAndroid) {
            debug.set("filesInRecordingsDirectory", filesInRecordingsDirectory);
            debug.set("recordingsUnclassified", recordingsUnclassified);
            debug.set("recordingsClassified", recordingsClassified);
            debug.set("recordingsToDelete", recordingsToDelete);
        }

        // Classify the recordings that are unclassified
        HashMap<String, JSONObject> recordingClassifications = new HashMap();
        for (HashMap<String, String> unclassifiedFileInterpretation : recordingsUnclassified) {
            JSONObject thisClassification = new JSONObject();
            File thisRecordingFile = new File(appStorageRecordingsDirectory, unclassifiedFileInterpretation.get("filename"));
            logger.info(String.valueOf(thisRecordingFile));

            try {

                // Generate the quick reading for Facebook
                JSONObject outputOfFacebookQuickReading = facebookGenerateQuickReading(
                        context, DEBUG, debugDirectory, thisRecordingFile, getVideoMetadataFunction, frameGrabFunction);

                // TODO
                JSONObject outputOfTiktokQuickReading = tiktokGenerateQuickReading(
                        context, DEBUG, debugDirectory, thisRecordingFile, getVideoMetadataFunction, frameGrabFunction);

                // TODO - other social media platforms
                // ...

                try {
                    String denotedMode = (String) outputOfFacebookQuickReading.get("denotedMode");
                    thisClassification.put("facebook-"+denotedMode, outputOfFacebookQuickReading);
                } catch (Exception e) {
                    logger.error(e);
                }

                thisClassification.put("tiktok", outputOfTiktokQuickReading);

                try {
                    recordingClassifications.put(unclassifiedFileInterpretation.get("filename"), thisClassification);
                } catch (Exception e) {
                    logger.error(e);
                }
            } catch (Exception e) {
                logger.error(e);
            }
        }
        if (!implementedOnAndroid) {
            debug.set("recordingClassifications", recordingClassifications);
        }


        // And adjust the recordings to reflect the changes
        //
        // The unclassified file is deleted, and the tagged 'classified' file is generated
        //
        HashMap<String, String> classificationOutcomeMap = new HashMap<>();
        for (String filename : recordingClassifications.keySet()) {
            List<String> parts = asList(filename.split("\\.")); parts = parts.subList(1, parts.size());
            List<String> classifications = IteratorUtils.toList(recordingClassifications.get(filename).keys());
            classifications = classifications.stream().filter(x -> {
                try {
                    return ((Boolean) ((JSONObject) recordingClassifications.get(filename).get(x)).get("of"));
                } catch (JSONException e) {
                    logger.error(e);
                    return false;
                }
            }).sorted().collect(Collectors.toList());
            if (classifications.isEmpty()) {
                // If no classifications yielded, delete the file
                (new File(appStorageRecordingsDirectory, filename)).delete();
                if (DEBUG) { classificationOutcomeMap.put(filename, "deleted"); }
            } else {
                // If classifications did yield, rename the file
                parts = Stream.concat(classifications.stream(), parts.stream()).collect(Collectors.toList());
                String newFilename = String.join(".", parts);
                (new File(appStorageRecordingsDirectory, filename)).renameTo(new File(appStorageRecordingsDirectory, newFilename));
                // Apply it to the 'recordingsClassified' data-structure
                HashMap<String, String> thisInterpretation = interpretRecordingFileName(newFilename);
                recordingsClassified.add(thisInterpretation);
                if (DEBUG) {
                    classificationOutcomeMap.put(filename, newFilename);
                }
            }
        }

        if (!implementedOnAndroid) {
            debug.set("classificationOutcomeMap", classificationOutcomeMap);
        }

        // Then for each individual interpretation, run the deeper analysis
        for (HashMap<String, String> thisInterpretation : recordingsClassified) {
            System.out.println(thisInterpretation);


            //  Conduct comprehensive analysis for Facebook
            if (thisInterpretation.get("tags").contains("facebook")) {
                facebookInterpretation( context,  appStorageRecordingsDirectory,  thisInterpretation,  rootDirectory,  getVideoMetadataFunction,
                        frameGrabFunction,  implementedOnAndroid,  adsFromDispatchDirectory,  fitterFacebookAdHeader,  pictogramsReference);
            }
            if (thisInterpretation.get("tags").contains("tiktok")) {
                tiktokInterpretation( context,  appStorageRecordingsDirectory,  thisInterpretation,  rootDirectory,  getVideoMetadataFunction,
                        frameGrabFunction,  implementedOnAndroid,  adsFromDispatchDirectory,  fitterFacebookAdHeader,  pictogramsReference);
            }


            // TODO - other social media platforms
            // ...

        }

        if (!implementedOnAndroid) {
            try {
                writeToJSON((new File(rootDirectory, "debug.json")), debug.internalJSONObject);
            } catch (Exception e) {
                logger.error(e);
            }
        }

        // If there is residual time at the end of the analysis, attempt to do another dispatch
        if (implementedOnAndroid) {
            dispatchAds(observerID, adsFromDispatchDirectory);
        }

    }

    public static Double tiktokLowerMostY(List<JSONXObject> boundingBoxes, JSONXObject boundingBoxSponsored) {
        List<JSONXObject> boundingBoxEngagementButtonsTentative = boundingBoxes.stream().filter(x ->
                x.get("className").equals("ENGAGEMENT_BUTTONS")).collect(Collectors.toList());
        Double lowerMostY = (boundingBoxEngagementButtonsTentative.isEmpty()) ? (double) boundingBoxSponsored.get("y2") :
                Math.max((double) boundingBoxEngagementButtonsTentative.get(0).get("y2"), (double) boundingBoxSponsored.get("y2"));
        return lowerMostY;
    }

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
            if (!boundingBoxes.isEmpty()) {
                currentAdFrames.add(thisFrame);
                retainedFramesAsFilesForDeepInference.add(thisFrameFile);
                retainedFramesForDeepInference.add(thisFrame);
            }
            // If the current frame has no reading, or we are at the end of the retained frames
            if ((boundingBoxes.isEmpty()) || (thisFrame.equals(retainedFrames.get(retainedFrames.size() - 1)))) {
                if (!currentAdFrames.isEmpty()) {
                    // Dispatch and start anew
                    groupsOfAdFrames.add(new ArrayList<>(currentAdFrames));
                    currentAdFrames = new ArrayList<>();
                }
            }
            cursorFrameFile ++;
        }

        return (new JSONXObject()
                .set("retainedFramesForDeepInference", retainedFramesForDeepInference)
                .set("retainedFramesAsFilesForDeepInference", retainedFramesAsFilesForDeepInference)
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

    public static boolean prepareForDispatch(Context context, File rootDirectory, HashMap<String, String> thisInterpretation,
                                          List<JSONXObject> advertisementObjects, File screenRecordingAnalysisDirectory,
                                          JSONXObject thisComprehensiveReading, JSONXObject inferenceResultShallow,
                                          JSONXObject inferenceResultDeep, String platform) {
        // Prepare for dispatch
        boolean success = true;
        Integer w = null;
        Integer h = null;
        File dispatchDirectory = new File(rootDirectory, "dispatch");
        File screenRecordingDispatchDirectory = new File(dispatchDirectory, thisInterpretation.get("filename") + ".dispatch");
        createDirectory(screenRecordingDispatchDirectory, true); // Force re-creation to avoid doubling up on submissions
        try {
            for (JSONXObject thisAdFrameGroupMetadata : advertisementObjects) {
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
                String observerID = sharedPreferenceGet(context, "SHARED_PREFERENCE_OBSERVER_ID", SHARED_PREFERENCE_OBSERVER_ID_DEFAULT_VALUE);

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
                                        .set("elapsedTimePerFrameAtLastCall", (Double) thisComprehensiveReading.get("elapsedTime") / (Integer) thisComprehensiveReading.get("nFramesSampled"))
                                        .set("elapsedTimeAtLastCall", thisComprehensiveReading.get("elapsedTime"))


                                )
                                .set("inferenceShallow", (new JSONXObject())
                                        .set("nFramesAnalyzed", inferenceResultShallow.get("nFramesAnalyzed"))
                                        .set("elapsedTimePerFrame", ((Double) inferenceResultShallow.get("elapsedTime") * 1000 / (Integer) inferenceResultShallow.get("nFramesAnalyzed")))
                                        .set("elapsedTime", (Double) inferenceResultShallow.get("elapsedTime") * 1000))
                                .set("inferenceDeep", (new JSONXObject())
                                        .set("nFramesAnalyzed", inferenceResultDeep.get("nFramesAnalyzed"))
                                        .set("elapsedTimePerFrame", ((Double) inferenceResultDeep.get("elapsedTime") * 1000 / (Integer) inferenceResultDeep.get("nFramesAnalyzed")))
                                        .set("elapsedTime", (Double) inferenceResultDeep.get("elapsedTime") * 1000))
                        )
                        .set("frameMetadata", thisAdFrameGroupMetadata);

                writeToJSON((new File(advertisementDirectory, "metadata.json")), dispatchObject.internalJSONObject);
            }
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    public static void platformInterpretationRoutine(Context context, File rootDirectory,
                                                     Function<JSONXObject, JSONXObject> getVideoMetadataFunction,
                                                     Function<JSONXObject, Bitmap> frameGrabFunction, Boolean implementedOnAndroid, Function<JSONXObject, JSONXObject> objectDetectorFunction) {

        List<HashMap<String,String>> recordingsClassified = new ArrayList<>();
        List<HashMap<String,String>> recordingsToDelete = new ArrayList<>();

        appStorageRecordingsDirectory = (new File (rootDirectory.getAbsolutePath(), "videos"));
        for (File thisFile : appStorageRecordingsDirectory.listFiles()) {
            Log.i(TAG, thisFile.getAbsolutePath());
            try {
                HashMap<String, String> thisInterpretation = interpretRecordingFileName(thisFile.getName());
                if (thisInterpretation != null) {
                    if ((!thisInterpretation.containsKey("orientation")) || (Objects.equals(thisInterpretation.get("orientation"), "landscape"))) {
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
                e.printStackTrace();
                logger.error(e); // TODO
            }
        }

        Log.i(TAG, recordingsClassified.toString());
        // Run the comprehensive sampling process
        // TODO - sort alphabetically to stop randomly starting differing entries
        for (HashMap<String, String> thisInterpretation : recordingsClassified) {

            File analysisDirectory = new File(rootDirectory, "analysis");
            File screenRecordingAnalysisDirectory = new File(analysisDirectory, thisInterpretation.get("filename") + ".analysis");

            // TODO - fresh passes upstream have to trigger fresh passes downstream

            if (Objects.equals(thisInterpretation.get("tags"), "com_zhiliaoapp_musically")) {
                // Will grab comprehensive reading using image hashing to determine differences between frames
                // works sequentially to avoid issues with periodic workers stopping
                // uses checkpoint to load up past execution if its already been compiled
                // TODO - check edge case errors
                File screenRecordingFile = (new File(appStorageRecordingsDirectory, thisInterpretation.get("filename")));
                JSONXObject thisComprehensiveReading = comprehensiveReading(context, rootDirectory, screenRecordingAnalysisDirectory,
                        screenRecordingFile, getVideoMetadataFunction, frameGrabFunction);
                // Undertake preliminary inference
                Log.i(TAG, "a");
                // TODO - improve nano reading model
                List<Integer> retainedFrames = (List<Integer>) thisComprehensiveReading.get("retainedFrames");
                List<String> retainedFramesAsFiles = (List<String>) thisComprehensiveReading.get("retainedFramesAsFiles");

                JSONXObject inferenceResultShallow = objectDetectorFunction.apply((new JSONXObject())
                        .set("context", context)
                        .set("analysisDirectory", screenRecordingAnalysisDirectory)
                        .set("thisScreenRecordingFile", screenRecordingFile)
                        .set("retainedFrameFiles", retainedFramesAsFiles)
                        .set("retainedFrames", retainedFrames)
                        .set("modelName", "float32_tiktok_sponsored.tflite")
                        .set("thisCase", "Shallow")
                    );

                // Group together ads that are adjacent
                JSONXObject groupedAdsObject = groupAdjacentAds(inferenceResultShallow, retainedFrames, retainedFramesAsFiles);
                List<List<Integer>> groupsOfAdFrames = (List<List<Integer>>) groupedAdsObject.get("groupsOfAdFrames");
                JSONXObject inferencesByFrames = (JSONXObject) groupedAdsObject.get("inferencesByFrames");

                Log.i(TAG, groupsOfAdFrames.toString());

                if (groupsOfAdFrames.isEmpty()) {
                    Log.i(TAG, "Deleting empty video");
                    deleteScreenRecordingAnalysis(screenRecordingFile, screenRecordingAnalysisDirectory, implementedOnAndroid);
                } else {

                    // Undertake the deep-pass on all retained frames that contained 'Sponsored' texts
                    JSONXObject inferenceResultDeep = objectDetectorFunction.apply((new JSONXObject())
                            .set("context", context)
                            .set("analysisDirectory", screenRecordingAnalysisDirectory)
                            .set("thisScreenRecordingFile", screenRecordingFile)
                            .set("retainedFrameFiles", groupedAdsObject.get("retainedFramesAsFilesForDeepInference"))
                            .set("retainedFrames", groupedAdsObject.get("retainedFramesForDeepInference"))
                            .set("modelName", "float32_tiktok_elements.tflite")
                            .set("thisCase", "Deep")
                        );

                    // TODO - note: This part requires a logic that is specialised to tiktok, and should be represented as such
                    // There are three kinds of ads that we have observed in TikTok - thumbnail ads, home ads, and search ads
                    // for each frame, bounding boxes are assessed to determine the kind of ad we are dealing with...


                    // thumbnail ads - go over each thumbnail, and check if it encases the majority of a sponsored text - if so, its an ad

                    // home ads - take upper most y of live and search buttons, and lowermost y of sponsored text and engagement buttons

                    // search reel ads - take upper most y of serach reel input and lowermost y of sponsored text and engagmeent buttons

                    // Crop out ad images


                    // TODO - unclassified ads are currently being ignored in processing

                    // TODO - currently there is an error experienced where two frames both have sponsored text but belong to different ads, however are grouped

                    // TODO - functionalize
                    // Construct ad objects

                    File checkPointDirectory = new File(screenRecordingAnalysisDirectory, "checkpoint");
                    checkPoint.setTargetDirectory(checkPointDirectory);
                    checkPoint thisCheckPoint = new checkPoint(screenRecordingFile.getName());

                    // TODO - Confirm that the ad frames belong to the same ads
                    // There are two giveaways for this -
                    //  1. whether the crop areas have any degree of overlap between frames
                    //  2. Whether the frames have the same classname

                    if (thisCheckPoint.container.has("inferenceDeep")) {
                        Log.i(TAG, "Proceeding with ad object construction");
                        List<JSONXObject> thisAdFrameGroupMetadatasUnseparated = new ArrayList<>();
                        JSONXObject inferencesDeepByFrames = (new JSONXObject((JSONObject) inferenceResultDeep.get("inferencesByFrames"), true));
                        for (List<Integer> adFrameGroup : groupsOfAdFrames) {
                            JSONXObject thisAdFrameGroupMetadata = new JSONXObject();
                            for (Integer adFrame : adFrameGroup) {
                                JSONXObject thisAdFrameData = new JSONXObject();
                                List<JSONXObject> boundingBoxesShallow = ((List<JSONObject>) inferencesByFrames.get(adFrame)).stream().map(x -> (new JSONXObject(x, true))).collect(Collectors.toList());
                                List<JSONXObject> boundingBoxesDeep = ((List<JSONObject>) inferencesDeepByFrames.get(adFrame)).stream().map(x -> (new JSONXObject(x, true))).collect(Collectors.toList());


                                // Determine the type of ad we are dealing with...
                                String tentativeAdType = null;
                                // Note that a thumbnail ad supercedes all other alternatives
                                // if there is at least one thumbnail - then we are dealing with a thumbnail
                                // otherwise
                                // if there is a serach reel inpuit - we are dealing with a search
                                // otherwise
                                // if there is either a live or search button, we are dealing with a home ad
                                if (!boundingBoxesDeep.isEmpty()) { // TODO - only proceed for this frame if there are 'deep' bounding boxes - note that if there are no bounding boxes, we can't determine where the contents of the ad is
                                    for (JSONXObject boundingBox : boundingBoxesDeep) {
                                        if (boundingBox.get("className").equals("POST_THUMBNAIL")) {
                                            tentativeAdType = "THUMBNAIL";
                                            break;
                                        } else {
                                            if (boundingBox.get("className").equals("REEL_SEARCH_INPUT")) {
                                                tentativeAdType = "REEL_FROM_SEARCH";
                                            } else if (((boundingBox.get("className").equals("LIVE_BUTTON")) || (boundingBox.get("className").equals("SEARCH_BUTTON"))) && (tentativeAdType == null)) {
                                                tentativeAdType = "REEL_FROM_HOME";
                                            }
                                        }
                                    }
                                    thisAdFrameData.set("adType", tentativeAdType);
                                    Double SPONSORSHIP_INTERSECTION_THRESHOLD = 0.8;
                                    List<String> sponsoredAdTexts = asList("SPONSORED_TEXT", "PROMOTIONAL_CONTENT_TEXT", "PAID_PARTNERSHIP_TEXT");

                                    // We assert that there is only ever one bounding box for a Sponsored text on any ad
                                    JSONXObject boundingBoxSponsored = boundingBoxesShallow.stream().filter(x ->
                                            sponsoredAdTexts.contains((String) x.get("className"))).collect(Collectors.toList()).get(0);

                                    // Evaluate thumbnail ads
                                    if (Objects.equals(tentativeAdType, "THUMBNAIL")) {
                                        // Retrieve the first thumbnail that significantly intersects a Sponsored text (we note that we only ever expect
                                        // to see one at any given time
                                        for (JSONXObject boundingBoxThumbnail : boundingBoxesDeep) {
                                            if (boundingBoxThumbnail.get("className").equals("POST_THUMBNAIL")) {
                                                Double intersectionPercentage = (rectangularAreaOverlap(boundingBoxThumbnail, boundingBoxSponsored)
                                                        / rectangularArea(boundingBoxSponsored));
                                                if (intersectionPercentage >= SPONSORSHIP_INTERSECTION_THRESHOLD) {
                                                    thisAdFrameData.set("inference", (new JSONXObject())
                                                            .set("boundingBoxCropped", boundingBoxThumbnail)
                                                            .set("boundingBoxSponsored", boundingBoxSponsored)
                                                            .set("boundingBoxes", boundingBoxesDeep));
                                                    break;
                                                }
                                            }
                                        }
                                    } else {
                                        Double lowerMostY = tiktokLowerMostY(boundingBoxesDeep, boundingBoxSponsored);
                                        Double upperMostY = 0.0;

                                        if (Objects.equals(tentativeAdType, "REEL_FROM_SEARCH")) {
                                            // Identify the comprising bounding boxes
                                            JSONXObject boundingBoxSearchInput = boundingBoxesDeep.stream().filter(x ->
                                                    x.get("className").equals("REEL_SEARCH_INPUT")).collect(Collectors.toList()).get(0);
                                            upperMostY = (double) boundingBoxSearchInput.get("y1");
                                        } else if (Objects.equals(tentativeAdType, "REEL_FROM_HOME")) {
                                            List<JSONXObject> boundingBoxLiveButtonsTentative = boundingBoxesDeep.stream().filter(x ->
                                                    x.get("className").equals("LIVE_BUTTON")).collect(Collectors.toList());
                                            List<JSONXObject> boundingBoxSearchButtonsTentative = boundingBoxesDeep.stream().filter(x ->
                                                    x.get("className").equals("SEARCH_BUTTON")).collect(Collectors.toList());
                                            Double y1LiveButton = (!boundingBoxLiveButtonsTentative.isEmpty()) ? (double) boundingBoxLiveButtonsTentative.get(0).get("y1") : 1.0;
                                            Double y1SearchButton = (!boundingBoxSearchButtonsTentative.isEmpty()) ? (double) boundingBoxSearchButtonsTentative.get(0).get("y1") : 1.0;
                                            upperMostY = Math.min(y1LiveButton, y1SearchButton);
                                        }

                                        JSONXObject boundingBoxCropped = yAgnosticCompositeBoundingBox(upperMostY, lowerMostY);
                                        thisAdFrameData.set("inference", (new JSONXObject())
                                                .set("boundingBoxCropped", boundingBoxCropped)
                                                .set("boundingBoxSponsored", boundingBoxSponsored)
                                                .set("boundingBoxes", boundingBoxesDeep));
                                    }
                                    if (thisAdFrameData.has("inference")) {
                                        thisAdFrameGroupMetadata.set(adFrame, thisAdFrameData);
                                    }
                                }
                            }
                            thisAdFrameGroupMetadatasUnseparated.add(thisAdFrameGroupMetadata);
                        }

                        List<JSONXObject> advertisementObjects = adFrameGroupsToAdObjects(thisAdFrameGroupMetadatasUnseparated);

                        // TODO - note : we don't retain data after the above block, as the logic execution is almost instantaneous

                        boolean successfulPreparation = prepareForDispatch(context,  rootDirectory,
                                thisInterpretation, advertisementObjects,  screenRecordingAnalysisDirectory,
                                 thisComprehensiveReading,  inferenceResultShallow, inferenceResultDeep,  "TIKTOK");
                        // Completion is concluded by deleting the screen recording, and the analysis folder
                        if (successfulPreparation) {
                            deleteScreenRecordingAnalysis(screenRecordingFile, screenRecordingAnalysisDirectory, implementedOnAndroid);
                        }
                    }
                }

                // Undertake comprehensive inference

                // assume y axis is point of shift
                // let a strong overlap between two frames be defined as some offset where the elements link share a union of

                // the union is more than the symmetric difference

                // union is part where elements are identical
                // differenec is part where elements are unidentical or dont overlap


                // it is very possible that two frames can be for two completely different posts

                // invent a really strict criteria
                    // for two adjacent frames - if the sponsored text appears in both, align them and their posts and perform the cropouts


                // Consider from the retained frames those that have been flagged as containing the 'Sponsored Text' and that are adjacent - these can be
                // grouped into a single ad reading
            }

            if (Objects.equals(thisInterpretation.get("tags"), "com_instagram_android")) {

                // TODO - we've done a short run through on the code that is shared between this and TikTok - need to refactorize further

                // Get the screen recording
                File screenRecordingFile = (new File(appStorageRecordingsDirectory, thisInterpretation.get("filename")));
                JSONXObject thisComprehensiveReading = comprehensiveReading(context, rootDirectory, screenRecordingAnalysisDirectory,
                        screenRecordingFile, getVideoMetadataFunction, frameGrabFunction);

                // Undertake preliminary inference
                // TODO - improve nano reading model
                List<Integer> retainedFrames = (List<Integer>) thisComprehensiveReading.get("retainedFrames");
                List<String> retainedFramesAsFiles = (List<String>) thisComprehensiveReading.get("retainedFramesAsFiles");
                JSONXObject inferenceResultShallow = objectDetectorFunction.apply((new JSONXObject())
                        .set("context", context)
                        .set("analysisDirectory", screenRecordingAnalysisDirectory)
                        .set("thisScreenRecordingFile", screenRecordingFile)
                        .set("retainedFrameFiles", retainedFramesAsFiles)
                        .set("retainedFrames", retainedFrames)
                        .set("modelName", "float32_instagram_sponsored.tflite") // TODO
                        .set("thisCase", "Shallow")
                );

                // Group together ads that are adjacent
                JSONXObject groupedAdsObject = groupAdjacentAds(inferenceResultShallow, retainedFrames, retainedFramesAsFiles);
                List<List<Integer>> groupsOfAdFrames = (List<List<Integer>>) groupedAdsObject.get("groupsOfAdFrames");
                JSONXObject inferencesByFrames = (JSONXObject) groupedAdsObject.get("inferencesByFrames");

                if (groupsOfAdFrames.isEmpty()) {
                    Log.i(TAG, "Deleting empty video");
                    deleteScreenRecordingAnalysis(screenRecordingFile, screenRecordingAnalysisDirectory, implementedOnAndroid);
                } else {

                    // Undertake the deep-pass on all retained frames that contained 'Sponsored' texts
                    JSONXObject inferenceResultDeep = objectDetectorFunction.apply((new JSONXObject())
                            .set("context", context)
                            .set("analysisDirectory", screenRecordingAnalysisDirectory)
                            .set("thisScreenRecordingFile", screenRecordingFile)
                            .set("retainedFrameFiles", groupedAdsObject.get("retainedFramesAsFilesForDeepInference"))
                            .set("retainedFrames", groupedAdsObject.get("retainedFramesForDeepInference"))
                            .set("modelName", "float32_instagram_elements.tflite")
                            .set("thisCase", "Deep")
                    );

                    // There are four kinds of ads that we have observed in Instagram - feed-based, reel-based, stories-based, and 'Explore' based
                    // The former three can be reliably captured, while the latter can't be.

                    // Construct ad objects

                    File checkPointDirectory = new File(screenRecordingAnalysisDirectory, "checkpoint");
                    checkPoint.setTargetDirectory(checkPointDirectory);
                    checkPoint thisCheckPoint = new checkPoint(screenRecordingFile.getName());

                    // TODO - Confirm that the ad frames belong to the same ads
                    // There are two giveaways for this -
                    //  1. whether the crop areas have any degree of overlap between frames
                    //  2. Whether the frames have the same classname

                    if (thisCheckPoint.container.has("inferenceDeep")) {
                        Log.i(TAG, "Proceeding with ad object construction");
                        List<JSONXObject> thisAdFrameGroupMetadatasUnseparated = new ArrayList<>();
                        JSONXObject inferencesDeepByFrames = (new JSONXObject((JSONObject) inferenceResultDeep.get("inferencesByFrames"), true));
                        for (List<Integer> adFrameGroup : groupsOfAdFrames) {
                            JSONXObject thisAdFrameGroupMetadata = new JSONXObject();
                            for (Integer adFrame : adFrameGroup) {
                                JSONXObject thisAdFrameData = new JSONXObject();
                                List<JSONXObject> boundingBoxesShallow = ((List<JSONObject>) inferencesByFrames.get(adFrame)).stream().map(x -> (new JSONXObject(x, true))).collect(Collectors.toList());
                                List<JSONXObject> boundingBoxesDeep = ((List<JSONObject>) inferencesDeepByFrames.get(adFrame)).stream().map(x -> (new JSONXObject(x, true))).collect(Collectors.toList());

                                // DETERMINE AD TYPE FOR FRAME
                                // Determine the type of ad we are dealing with...
                                String tentativeAdType = null;
                                // Note that a thumbnail ad supercedes all other alternatives
                                // if there is at least one thumbnail - then we are dealing with a thumbnail
                                // otherwise
                                // if there is a serach reel inpuit - we are dealing with a search
                                // otherwise
                                // if there is either a live or search button, we are dealing with a home ad
                                if (!boundingBoxesDeep.isEmpty()) { // TODO - only proceed for this frame if there are 'deep' bounding boxes - note that if there are no bounding boxes, we can't determine where the contents of the ad is
                                    // Are there any engagement buttons present?
                                    if (boundingBoxesDeep.stream().anyMatch(x -> (x.get("className").equals("BUTTONS_ENGAGEMENT")))) {
                                        // If yes, we are dealing with a reel-based ad
                                        tentativeAdType = "REEL_BASED";
                                    } else {
                                        // Is a FEED_POST_HEADER present
                                        if (boundingBoxesDeep.stream().anyMatch(x -> (x.get("className").equals("FEED_POST_HEADER")))) {
                                            // For all frames in the entire adFrameGroup, have the
                                            // FEED_POST_HEADER or SPONSORED_TEXT not appeared in the lower 25% of the y axis? Furthermore,
                                            // is the BUTTON_NEW_POST absent? If so, we're dealing with a story-based ad
                                            boolean isStoryType = adFrameGroup.stream().allMatch(y -> {
                                                        List<JSONXObject> boundingBoxesDeepAlt = ((List<JSONObject>) inferencesDeepByFrames.get(y))
                                                                .stream().map(x -> (new JSONXObject(x, true))).collect(Collectors.toList());
                                                        return boundingBoxesDeepAlt.stream().allMatch(z -> {
                                                            // BUTTON_NEW_POST is nowhere on the page
                                                            boolean buttonNewPostNotInFrame = (!z.get("className").equals("BUTTON_NEW_POST"));
                                                            // is there a FEED_POST_HEADER or SPONSORED_TEXT in the lower 75% of the y axis
                                                            boolean lowerQuartersOfYAxisContainPosts = ((
                                                                    z.get("className").equals("FEED_POST_HEADER")
                                                                        || z.get("className").equals("SPONSORED_TEXT"))
                                                                            && (((double) z.get("cy")) >= 0.25));

                                                            return (buttonNewPostNotInFrame && (!lowerQuartersOfYAxisContainPosts));
                                                        });
                                                    });
                                            if (isStoryType) {
                                                tentativeAdType = "STORY_BASED";
                                            } else {
                                                tentativeAdType = "FEED_BASED";
                                            }
                                        } else {
                                            // TODO - Dealing with an 'Explore' based ad - tentatively abort
                                        }
                                    }

                                    thisAdFrameData.set("adType", tentativeAdType);
                                    // We assert that there is only ever one bounding box for a Sponsored text on any ad
                                    JSONXObject boundingBoxSponsored = boundingBoxesShallow.stream().filter(x ->
                                            x.get("className").equals("SPONSORED_TEXT")).collect(Collectors.toList()).get(0);

                                    // PRODUCE CROPPING REGION
                                    Double lowerMostY = null;
                                    Double upperMostY = null;
                                    if (Objects.equals(tentativeAdType, "REEL_BASED")) {
                                        // For reel-based ads, we cannot determine the identity of the status bar, and so have to
                                        // discard an agnostic 15% off the entire y axis

                                        // Beyond this, the bottom y is defined as the furthest extent of either the SPONSORED_TEXT
                                        // or the BUTTONS_ENGAGEMENT or the BUTTON_NEW_POST (if it present)
                                        upperMostY = 0.15;
                                        lowerMostY = Collections.max(boundingBoxesDeep.stream().filter(x -> Arrays.asList(
                                                "SPONSORED_TEXT", "BUTTONS_ENGAGEMENT", "BUTTON_NEW_POST").contains((String) x.get("className")))
                                                .map(y -> (Double) y.get("y2")).collect(Collectors.toList()));
                                    } else if (Objects.equals(tentativeAdType, "STORY_BASED")) {
                                        // Take the upper-most y coordinate of the first FEED_POST_HEADER
                                        // and crop down the entire frame
                                        upperMostY = (Double) boundingBoxesDeep.stream()
                                                .filter(x->x.get("className").equals("FEED_POST_HEADER"))
                                                .collect(Collectors.toList()).get(0).get("y1");
                                        lowerMostY = 1.0;
                                    } else if (Objects.equals(tentativeAdType, "FEED_BASED")) {
                                        try {
                                            // Identify the FEED_POST_HEADER that intersects the SPONSORED_TEXT - we are going
                                            // to make an assumption that this always exists
                                            upperMostY = (Double) boundingBoxesDeep.stream()
                                                    .filter(x -> x.get("className").equals("FEED_POST_HEADER")
                                                            && (rectangularAreaOverlap(x, boundingBoxSponsored) > 0))
                                                    .collect(Collectors.toList()).get(0).get("y1");
                                            // We are also going to assume that the lowerMostY is always well-formed, even though
                                            // its possible that it could latch onto the same element that produced the upperMostY
                                            Double finalUpperMostY = upperMostY;
                                            lowerMostY = Collections.min(boundingBoxesDeep.stream().filter(x -> Arrays.asList(
                                                            "FEED_POST_HEADER", "BUTTON_NEW_POST").contains((String) x.get("className")) && (!Objects.equals((Double) x.get("y1"), finalUpperMostY)) )
                                                    .map(y -> (Double) y.get("y1")).collect(Collectors.toList()));
                                        } catch (Exception ignored) {
                                            // This block occurs on a malformed feed-based ad
                                        }
                                    }

                                    // For all three currently retrieved ad types, the upperMost and lowerMost Y crop the ad using the
                                    // same method
                                    if ((lowerMostY != null) && (upperMostY != null)) {
                                        JSONXObject boundingBoxCropped = yAgnosticCompositeBoundingBox(upperMostY, lowerMostY);
                                        thisAdFrameData.set("inference", (new JSONXObject())
                                                .set("boundingBoxCropped", boundingBoxCropped)
                                                .set("boundingBoxSponsored", boundingBoxSponsored)
                                                .set("boundingBoxes", boundingBoxesDeep));
                                        thisAdFrameGroupMetadata.set(adFrame, thisAdFrameData);
                                    }
                                }
                            }
                            thisAdFrameGroupMetadatasUnseparated.add(thisAdFrameGroupMetadata);
                        }

                        // Separate ads based on ad type and non-overlaps
                        List<JSONXObject> advertisementObjects = adFrameGroupsToAdObjects(thisAdFrameGroupMetadatasUnseparated);

                        // TODO - note : we don't retain data after the above block, as the logic execution is almost instantaneous

                        boolean successfulPreparation = prepareForDispatch(context,  rootDirectory,
                                thisInterpretation, advertisementObjects,  screenRecordingAnalysisDirectory,
                                thisComprehensiveReading,  inferenceResultShallow, inferenceResultDeep,  "INSTAGRAM");

                        if (successfulPreparation) {
                            // Completion is concluded by deleting the screen recording, and the analysis folder
                            deleteScreenRecordingAnalysis(screenRecordingFile, screenRecordingAnalysisDirectory, implementedOnAndroid);
                        }
                    }
                }
            }
        }
    }

}
