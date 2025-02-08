package com.adms.australianmobileadtoolkit.interpreter.platform;

import static com.adms.australianmobileadtoolkit.Arguments.A;
import static com.adms.australianmobileadtoolkit.Arguments.Args;
import static com.adms.australianmobileadtoolkit.Common.optionalGetDouble;
import static com.adms.australianmobileadtoolkit.appSettings.DEBUG;
import static com.adms.australianmobileadtoolkit.appSettings.SHARED_PREFERENCE_OBSERVER_ID_DEFAULT_VALUE;
import static com.adms.australianmobileadtoolkit.appSettings.sharedPreferenceGet;
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
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import com.adms.australianmobileadtoolkit.Arguments;
import com.adms.australianmobileadtoolkit.JSONXObject;
import com.adms.australianmobileadtoolkit.R;
import com.adms.australianmobileadtoolkit.appSettings;
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
                currentRange = Arrays.asList(thisRangeValue);
            } else {
                if (Math.abs(currentRange.get(currentRange.size() - 1) - thisRangeValue) <= interval) {
                    currentRange = Arrays.asList(currentRange.get(0), thisRangeValue);
                    if (thisRangeValue.equals(intervalsSorted.get(intervalsSorted.size() - 1))) {
                        intervalsSortedAsRanges.add(new ArrayList<>(currentRange));
                    }
                } else {
                    intervalsSortedAsRanges.add(new ArrayList<>(currentRange));
                    currentRange = Arrays.asList(thisRangeValue);
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
            consistentListAsRangesUpdated.add(Arrays.asList(aRange.get(0)-interval, aRange.get(1)+interval));
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
            output.add(Arrays.asList(aMin, bMin));
        } else
        if (partialOverlapBSmaller) {
            output.add(Arrays.asList(bMax, aMax));
        } else
        if (aInsideOrEqualToB) {
            // Do not add anything
        } else
        if (bInsideOrEqualToA) {
            output.add(Arrays.asList(aMin, bMin));
            output.add(Arrays.asList(bMax, aMax));
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
                    // If the folder is empty, delete it
                    if (thisAdDirectory.listFiles().length == 0) {
                        deleteRecursive(thisAdDirectory);
                        Log.i(TAG, "Deleting empty directory.");
                    } else {
                        // Naively, we might assume that the upload of ad content is
                        Log.i(TAG, "Bypassing upload of ad content as not ready yet.");
                    }
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
    public static void platformInterpretationRoutine(Context context, File rootDirectory,
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



}
