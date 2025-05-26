package com.adms.australianmobileadtoolkit.interpreter;

import static com.adms.australianmobileadtoolkit.Common.makeDirectory;
import static com.adms.australianmobileadtoolkit.appSettings.prescribedMinVideoWidth;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.persistThread;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.saveBitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.adms.australianmobileadtoolkit.JSONXObject;
import com.adms.australianmobileadtoolkit.checkPoint;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import ru.avicorp.phashcalc.pHashCalc;

public class Sampler {
    private static String TAG = "Sampler";


    /*
     *
     *
     * Comprehensive Readings
     *
     *
     * */

    /**
     *
     * TODO - this function needs to be adapted to the Android device
     *
     */
    // We need a function that can determine the frames to derive from the sample

    public static List<JSONXObject> filterCompat(List<JSONXObject> thisList, Function<JSONXObject, Boolean> thisFilter) {
        List<JSONXObject> adjustedList = new ArrayList<>();
        for (JSONXObject x : thisList) {
            if (thisFilter.apply(x)) {
                adjustedList.add(x);
            }
        }
        return adjustedList;
    }

    public static boolean comprehensiveReadingStrongOverlap(List<Integer> x, List<Integer> y) {
        return Math.max(x.get(0), y.get(0)) < Math.min(x.get(1), y.get(1));
    }

    // This function works as a frame-grabbing intermediate, sampling a screen recording if necessary, or otherwise
    // retrieving the image from a cache
    public static JSONXObject comprehensiveReadingSampleImage(File targetDirectory, Function<JSONXObject, Bitmap> frameGrabFunction, JSONXObject frameGrabFunctionInput) {
        // Attempt to make the 'temp' directories if they don't already exist
        File analysisDirectory = new File(targetDirectory,"analysis");
        File screenRecordingAnalysisDirectory = new File(analysisDirectory,((File) frameGrabFunctionInput.get("thisScreenRecordingFile")).getName() + ".analysis");
        File screenRecordingCRTDirectory = new File(screenRecordingAnalysisDirectory,"frames");
        makeDirectory(analysisDirectory);
        makeDirectory(screenRecordingAnalysisDirectory);
        makeDirectory(screenRecordingCRTDirectory);

        JSONXObject output = new JSONXObject();

        // Declare the file's path
        boolean sampled = false;
        File thisFrameFile = new File(screenRecordingCRTDirectory, ((int) frameGrabFunctionInput.get("f")) + ".jpg");
        // If the file exists, reload it
        if (thisFrameFile.exists()) {
            output.set("wellFormed", true);
            //thisSampleImage = BitmapFactory.decodeFile(thisFrameFile.getAbsolutePath());
        } else {
            // Otherwise generate it and store it for later use if necessary
            sampled = true;
            output.set("wellFormed", false);
            Bitmap thisSampleBitmap = frameGrabFunction.apply(frameGrabFunctionInput);
            if (thisSampleBitmap != null) {
                output.set("wellFormed", true);
                saveBitmap(thisSampleBitmap, thisFrameFile.getAbsolutePath());
            }
        }
        //output.set("bitmap", thisSampleImage);
        output.set("file", thisFrameFile);
        output.set("sampled", sampled);

        return output;
    }

    public static Double comprehensiveReadingGetFrameSimilarityPercentage(JSONXObject imageSample, int lastFrame, int thisFrame) {
        pHashCalc thisPHashCalc = new pHashCalc();
        thisPHashCalc.loadSourceFile((String) imageSample.get(lastFrame), (String) imageSample.get(thisFrame));
        return thisPHashCalc.calculateIdentical() / 100.0;
    }

    public static JSONXObject comprehensiveReadingGetFrameRelation(JSONXObject imageSample, int lastFrame, int thisFrame) {
        Double SIMILARITY_THRESHOLD = 0.90;
        Double similarityPercentage = comprehensiveReadingGetFrameSimilarityPercentage(imageSample, lastFrame, thisFrame);
        String verdict = (similarityPercentage >= SIMILARITY_THRESHOLD) ? "SIMILAR" : "DIFFERENT";

        Log.i(TAG, "lastFrame: "+lastFrame+" thisFrame: "+thisFrame+" verdict: "+verdict);

        return (new JSONXObject())
                .set("verdict", verdict)
                .set("similarityPercentage", similarityPercentage);
    }

    public static List<Object> combineLists(List a, List b) {
        List<Object> c = new ArrayList<>();
        for (Object objectFromA : a) {
            c.add(objectFromA);
        }
        for (Object objectFromB : b) {
            c.add(objectFromB);
        }
        return c;
    }

    public static JSONXObject comprehensiveReadingRetainedFrames(List<JSONXObject> masterFrameSimilarityReadings) {
        // Determine the frames to retain
        List<List<Integer>> similarityGroups = new ArrayList<>();
        List<Integer> retainedFrames = new ArrayList<>();
        String lastReadingVerdict = null;
        for (int i = 0; i < masterFrameSimilarityReadings.size(); i ++) {
            JSONXObject thisReading = masterFrameSimilarityReadings.get(i);
            Integer lastFrame = (Integer) thisReading.get("lastFrame");
            Integer thisFrame = (Integer) thisReading.get("thisFrame");
            String verdict = ((String) ((JSONXObject) thisReading.get("relation")).get("verdict"));
            if (i == 0) {
                retainedFrames.add(lastFrame);
            } else if ((!Objects.equals(verdict, lastReadingVerdict)) && (verdict.equals("DIFFERENT"))) {
                retainedFrames.add(lastFrame);
                retainedFrames.add(thisFrame);
            }
            lastReadingVerdict = verdict;
            if (verdict.equals("SIMILAR")) {
                List<Integer> thisReadingIndices = Arrays.asList(lastFrame, thisFrame);
                boolean applied = false;
                for (int j = 0; j < similarityGroups.size(); j ++) {
                    if ((similarityGroups.contains(lastFrame)) || similarityGroups.contains(thisFrame)) {
                        similarityGroups.set(j, combineLists(similarityGroups.get(j),
                                thisReadingIndices).stream().map(x -> (Integer) x).collect(Collectors.toList()));
                        similarityGroups.set(j, similarityGroups.get(j).stream().distinct().collect(Collectors.toList()));
                        applied = true;
                    }
                }
                if (!applied) {
                    similarityGroups.add(thisReadingIndices);
                }
            }
        }

        List<Integer> inhibitedFrames = new ArrayList<>();
        for (List<Integer> thisGroup : similarityGroups) {
            List<Integer> thisIntersection = new ArrayList<>(thisGroup.stream().distinct().collect(Collectors.toList()));
            thisIntersection.retainAll(retainedFrames.stream().distinct().collect(Collectors.toList()));
            thisIntersection.sort(Comparator.naturalOrder());
            if (thisIntersection.size() > 1) {
                inhibitedFrames.add(thisIntersection.get(0));
            }
        }

        JSONXObject output = (new JSONXObject())
                .set("retainedFrames",retainedFrames.stream().filter(x -> (!inhibitedFrames.contains(x))).distinct().collect(Collectors.toList()))
                .set("similarityGroups", similarityGroups);

        return output;
    }

    public static JSONXObject basicReading(Context context,
                                           File rootDirectory, File screenRecordingAnalysisDirectory, File thisScreenRecordingFile, Function<JSONXObject, JSONXObject> videoMetadataFunction,
                                           Function<JSONXObject, Bitmap> frameGrabFunction) throws Exception {
        checkPoint checkPoint = new checkPoint(thisScreenRecordingFile.getName(), (new File(screenRecordingAnalysisDirectory, "checkpoint")));

        if (checkPoint.container.has("comprehensiveReading")) {
            return new JSONXObject((JSONObject) checkPoint.container.get("comprehensiveReading"), true);
        } else {
            JSONXObject output;
            Double elapsedTime = Long.valueOf(System.currentTimeMillis()).doubleValue();
            JSONXObject videoMetadata = videoMetadataFunction.apply(new JSONXObject().set("context", context).set("screenRecordingFile", thisScreenRecordingFile));

            // Determine the length of the recording in frames
            final Integer FRAME_SAMPLE_LOWER_THRESHOLD = 2;

            Integer totalFrames = ((int) videoMetadata.get("METADATA_KEY_VIDEO_FRAME_COUNT"));
            Integer totalFramesUnadjusted = totalFrames;

            Integer durationInMilliseconds = ((int) videoMetadata.get("METADATA_KEY_DURATION")) - 1;
            Double FPS = (Double) videoMetadata.get("METADATA_DERIVED_FRAMERATE");




            // The only way to actually determine the last frame is to step back from the total number of frames and stop when a result is yielded
            // TODO - needs a timeout
            Integer attemptedEndingFrameSizeInLength = 0;
            while (attemptedEndingFrameSizeInLength == 0) {
                persistThread(context, TAG);
                totalFrames --;
                try {
                    JSONXObject currentFrameObject = comprehensiveReadingSampleImage(rootDirectory, frameGrabFunction, (new JSONXObject())
                            .set("context", context)
                            .set("thisScreenRecordingFile", thisScreenRecordingFile)
                            .set("f", totalFrames)
                            .set("videoFrames", totalFramesUnadjusted)
                            .set("videoDuration", durationInMilliseconds)
                            .set("minWidth", prescribedMinVideoWidth));
                    attemptedEndingFrameSizeInLength = Math.toIntExact(((File) currentFrameObject.get("file")).length());
                } catch (Exception ignored) {};
            }
            totalFrames ++;
            Log.i(TAG, "totalFrames: "+totalFrames.toString());


            // TODO - at this stage, we know exactly how many frames we are dealing with

            Integer nFramesSampled = 0;
            Integer nFramesSampledAtLastCall = 0;
            List<Integer> retainedFrames = new ArrayList<>();
            JSONXObject imageSample = new JSONXObject();

            Integer nFramesToGetInSecond = 4; // Grab 4 frames a second by setting this as the step
            Integer frameIndexStep = (int) Math.max(1, Math.floor(FPS / nFramesToGetInSecond));

            for (int frameIndex = 0; frameIndex <= totalFrames; frameIndex += frameIndexStep) {
                persistThread(context, TAG);
                JSONXObject currentFrameObject = comprehensiveReadingSampleImage(rootDirectory, frameGrabFunction, (new JSONXObject())
                        .set("context", context)
                        .set("thisScreenRecordingFile", thisScreenRecordingFile)
                        .set("f", frameIndex)
                        .set("videoFrames", totalFramesUnadjusted)
                        .set("videoDuration", durationInMilliseconds)
                        .set("minWidth", prescribedMinVideoWidth));
                if ((boolean) currentFrameObject.get("wellFormed")) {
                    retainedFrames.add(frameIndex);
                }
                if ((boolean) currentFrameObject.get("sampled")) {
                    nFramesSampledAtLastCall ++;
                }
                try {
                    imageSample.set(frameIndex, ((File) currentFrameObject.get("file")).getAbsolutePath());
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            //retainedFrames.sort(Comparator.naturalOrder());
            elapsedTime = Math.abs(Long.valueOf(System.currentTimeMillis()).doubleValue() - elapsedTime);

            output = (new JSONXObject())
                    .set("nFramesSampled", nFramesSampled)
                    .set("nFramesSampledAtLastCall", nFramesSampledAtLastCall)
                    .set("elapsedTime", elapsedTime)
                    .set("retainedFrames", retainedFrames)
                    .set("nFrames", totalFrames)
                    .set("durationInMilliseconds", durationInMilliseconds)
                    .set("nFramesUnadjusted", totalFramesUnadjusted)
                    .set("retainedFramesAsFiles", retainedFrames.stream().map(imageSample::get).collect(Collectors.toList()))
                    .set("fps", FPS);
            checkPoint.set("comprehensiveReading", output.internalJSONObject);
            checkPoint.save();
            // TODO - this has been added for a comprehensive reading test...
            //writeToJSON((new File(rootDirectory, "thisBasicReadingDump.json")), output.internalJSONObject);
            return output;
        }
    }

    // An agnostic implementatotion of the Facebook-specific function, with a few error fixes
    // TODO - make the folder delete after a successful analysis
    public static JSONXObject comprehensiveReading(Context context,
                                                   File rootDirectory, File screenRecordingAnalysisDirectory, File thisScreenRecordingFile, Function<JSONXObject, JSONXObject> videoMetadataFunction,
                                                   Function<JSONXObject, Bitmap> frameGrabFunction) {

        checkPoint checkPoint = new checkPoint(thisScreenRecordingFile.getName(), (new File(screenRecordingAnalysisDirectory, "checkpoint")));

        if (checkPoint.container.has("comprehensiveReading")) {
            return new JSONXObject((JSONObject) checkPoint.container.get("comprehensiveReading"), true);
        } else {
            JSONXObject output;
            Double elapsedTime = Long.valueOf(System.currentTimeMillis()).doubleValue();
            JSONXObject videoMetadata = videoMetadataFunction.apply(new JSONXObject().set("context", context).set("screenRecordingFile", thisScreenRecordingFile));

            // Determine the length of the recording in frames
            final Integer FRAME_SAMPLE_LOWER_THRESHOLD = 2;

            Integer totalFrames = ((int) videoMetadata.get("METADATA_KEY_VIDEO_FRAME_COUNT"));
            Integer totalFramesUnadjusted = totalFrames;

            Integer durationInMilliseconds = ((int) videoMetadata.get("METADATA_KEY_DURATION")) - 1;
            Double FPS = (Double) videoMetadata.get("METADATA_DERIVED_FRAMERATE");




            // The only way to actually determine the last frame is to step back from the total number of frames and stop when a result is yielded
            // TODO - needs a timeout
            Integer attemptedEndingFrameSizeInLength = 0;
            while (attemptedEndingFrameSizeInLength == 0) {
                totalFrames --;
                try {
                    JSONXObject currentFrameObject = comprehensiveReadingSampleImage(rootDirectory, frameGrabFunction, (new JSONXObject())
                            .set("context", context)
                            .set("thisScreenRecordingFile", thisScreenRecordingFile)
                            .set("f", totalFrames)
                            .set("videoFrames", totalFramesUnadjusted)
                            .set("videoDuration", durationInMilliseconds)
                            .set("minWidth", prescribedMinVideoWidth));
                    attemptedEndingFrameSizeInLength = Math.toIntExact(((File) currentFrameObject.get("file")).length());
                } catch (Exception ignored) {};
            }
            totalFrames ++;
            Log.i(TAG, "totalFrames: "+totalFrames.toString());

            Integer frameSampleUpperThreshold = Math.toIntExact((long) Math.floor(FPS)); //(int) Math.floor(totalFrames / FPS);
            Log.i(TAG, "frameSampleUpperThreshold: "+frameSampleUpperThreshold.toString());
            Log.i(TAG, "FPS: "+FPS.toString());
            Integer frameSampleThresholdPrevious = null;
            Integer frameSampleThreshold = frameSampleUpperThreshold;
            List<List<Integer>> targetRanges = Arrays.asList(Arrays.asList(0, totalFrames-1));
            List<Integer> forcedRetainedFrames = new ArrayList<>();
            JSONXObject imageSample = new JSONXObject();
            List<JSONXObject> masterFrameSimilarityReadings = new ArrayList<>();
            JSONXObject contextualisedFrameSimilarityReadings = new JSONXObject();
            Integer nFramesSampled = 0;
            Integer nFramesSampledAtLastCall = 0;
            while ((frameSampleThreshold >= FRAME_SAMPLE_LOWER_THRESHOLD) && (!targetRanges.isEmpty())) {

                // Purge any readings that already exist within the masterFrameSimilarityReadings that may overlap the target ranges
                // (this is done to avoid overlap of frames)

                List<List<Integer>> finalTargetRanges = targetRanges;
                Function<JSONXObject, Boolean> commonStrongOverlapFunction = x -> {
                    boolean strongOverlapExists = false;
                    List<Integer> xList = Arrays.asList((Integer) x.get("lastFrame"),(Integer) x.get("thisFrame"));
                    for (List<Integer> y : finalTargetRanges) {
                        strongOverlapExists = (comprehensiveReadingStrongOverlap(xList, y));
                    }
                    return (!strongOverlapExists);
                };

                masterFrameSimilarityReadings = filterCompat(masterFrameSimilarityReadings, commonStrongOverlapFunction);

                if (frameSampleThresholdPrevious != null) {
                    contextualisedFrameSimilarityReadings.set(frameSampleThreshold, filterCompat(
                            (List<JSONXObject>) contextualisedFrameSimilarityReadings.get(frameSampleThresholdPrevious), commonStrongOverlapFunction
                    ));
                }

                // Run sampling over target ranges - for each target range, produce the readings
                List<JSONXObject> frameSimilarityReadings = new ArrayList<>();
                for (List<Integer> thisRange : targetRanges) {
                    // Go over the frames within this range
                    Integer lastFrame = null;
                    List<JSONXObject> thisRangeReadings = new ArrayList<>();
                    for (int _thisFrame = thisRange.get(0); _thisFrame <= thisRange.get(1)+frameSampleThreshold; _thisFrame += frameSampleThreshold) {
                        boolean wellFormed = true;
                        int thisFrame = _thisFrame;
                        if (thisFrame >= totalFrames) {
                            thisFrame = totalFrames - 1; // TODO - check this for errors
                        }
                        if (thisFrame > thisRange.get(1)) {
                            thisFrame = thisRange.get(1);
                        }
                        Log.i(TAG, "Indexing frame: "+thisFrame);
                        // Generate the p-hash for the image
                        if (!(imageSample.has(thisFrame))) {
                            nFramesSampled ++;
                            JSONXObject currentFrameObject = comprehensiveReadingSampleImage(rootDirectory, frameGrabFunction, (new JSONXObject())
                                    .set("context", context)
                                    .set("thisScreenRecordingFile", thisScreenRecordingFile)
                                    .set("f", thisFrame)
                                    .set("videoFrames", totalFramesUnadjusted)
                                    .set("videoDuration", durationInMilliseconds)
                                    .set("minWidth", prescribedMinVideoWidth));
                            wellFormed = (boolean) currentFrameObject.get("wellFormed");
                            if (wellFormed) {
                                if ((boolean) currentFrameObject.get("sampled")) {
                                    nFramesSampledAtLastCall ++;
                                }
                                try {
                                    imageSample.set(thisFrame, ((File) currentFrameObject.get("file")).getAbsolutePath());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                Log.i(TAG, "Discard frame " + thisFrame);
                            }
                        }

                        if (wellFormed) {
                            if (lastFrame != null) {
                                JSONXObject thisReading = (new JSONXObject())
                                        .set("lastFrame", lastFrame)
                                        .set("thisFrame", thisFrame)
                                        .set("relation", comprehensiveReadingGetFrameRelation(imageSample, lastFrame, thisFrame));
                                frameSimilarityReadings.add(thisReading);
                                thisRangeReadings.add(thisReading);
                            }

                            lastFrame = thisFrame;
                        }
                    }

                    // If a range is entirely similar (from all its sub-comparisons), yet has become the subject of comparison (from being different in a
                    // comparison of a larger frame similarity threshold, then we must place the boundaries of the range into a 'forced' list of retained frames)
                    if (thisRangeReadings.stream().noneMatch(x -> Objects.equals((String) ((JSONXObject) x.get("relation")).get("verdict"), "DIFFERENT"))) {
                        forcedRetainedFrames = combineLists(forcedRetainedFrames, thisRange).stream().map(x -> (Integer) x).collect(Collectors.toList());
                    }
                }

                // Establish new ranges to index
                targetRanges = frameSimilarityReadings.stream()
                        .filter(x -> ((String) ((JSONXObject) x.get("relation")).get("verdict")).equals("DIFFERENT"))
                        .map(y -> Arrays.asList((Integer) y.get("lastFrame"), (Integer) y.get("thisFrame"))).collect(Collectors.toList());

                // Halve the frame sampling threshold
                frameSampleThreshold = Math.toIntExact(Math.round(Math.floor(frameSampleThreshold / 2.0)));

                // Record the frame similarity readings if they are either well-formed, or if the frame similarity readings have been calculated
                // for the minimum threshold
                Integer finalFrameSampleThreshold = frameSampleThreshold;
                List<JSONXObject> listToApply = frameSimilarityReadings.stream()
                        .filter(x -> (!((String) ((JSONXObject) x.get("relation")).get("verdict")).equals("DIFFERENT"))
                                || (finalFrameSampleThreshold < FRAME_SAMPLE_LOWER_THRESHOLD) )
                        .collect(Collectors.toList());
                masterFrameSimilarityReadings = combineLists(masterFrameSimilarityReadings, listToApply).stream().map(x -> (JSONXObject) x).collect(Collectors.toList());
                contextualisedFrameSimilarityReadings.set(frameSampleThreshold, frameSimilarityReadings);
                frameSampleThresholdPrevious = frameSampleThreshold;
            }
            // Order the results
            Collections.sort(masterFrameSimilarityReadings, new SortByXLastFrame());

            // Retrieve the retained frames
            JSONXObject retainedFramesResult = comprehensiveReadingRetainedFrames(masterFrameSimilarityReadings);
            List<Integer> retainedFrames = (List<Integer>) retainedFramesResult.get("retainedFrames");
            Log.i(TAG, "Number of frames retained: "+retainedFrames);
            List<List<Integer>> similarityGroups = (List<List<Integer>>) retainedFramesResult.get("similarityGroups");

            JSONXObject contextualisedRetainedFrames = new JSONXObject();
            for (String k : contextualisedFrameSimilarityReadings.keys()) {
                List<JSONXObject> thisList = (List<JSONXObject>) contextualisedFrameSimilarityReadings.get(k);
                Collections.sort( thisList, new SortByXLastFrame());
                contextualisedFrameSimilarityReadings.set(k, thisList);
                contextualisedRetainedFrames.set(k,
                        comprehensiveReadingRetainedFrames((List<JSONXObject>) contextualisedFrameSimilarityReadings.get(k)).get("retainedFrames"));
            }
            Log.i(TAG, "Number of frames sampled: "+nFramesSampled);

            Log.i(TAG, "Number of frames retained: "+retainedFrames);
            retainedFrames = combineLists(retainedFrames, forcedRetainedFrames).stream().distinct().map(x -> (Integer) x).collect(Collectors.toList());
            retainedFrames.sort(Comparator.naturalOrder());

            Log.i(TAG, "Number of frames retained: "+retainedFrames);

            // Do one final sweep over the frames to remove duplicates inserted by separated iterations

            boolean canExitDeduplication = false;
            JSONXObject crossComparisons = new JSONXObject();
            while (!canExitDeduplication) {
                Integer discardingFrame = null;
                for (int i = 0; i < (retainedFrames.size() - 1); i++) {
                    if (!crossComparisons.has(i)) {
                        crossComparisons.set(i, new ArrayList<>());
                    }
                    if (!((List<Integer>) crossComparisons.get(i)).contains(i+1)) {
                        String verdict = (String) comprehensiveReadingGetFrameRelation(imageSample, retainedFrames.get(i), retainedFrames.get(i + 1)).get("verdict");
                        ((List<Integer>) crossComparisons.get(i)).add(i+1);
                        if (verdict.equals("SIMILAR")) {
                            discardingFrame = retainedFrames.get(i);
                            break;
                        }
                    }
                }
                if (discardingFrame != null) {
                    retainedFrames.remove(discardingFrame);
                } else {
                    canExitDeduplication = true;
                }
            }
            Log.i(TAG, "Number of frames retained: "+retainedFrames);
            elapsedTime = Math.abs(Long.valueOf(System.currentTimeMillis()).doubleValue() - elapsedTime);

            output = (new JSONXObject())
                    .set("nFramesSampled", nFramesSampled)
                    .set("nFramesSampledAtLastCall", nFramesSampledAtLastCall)
                    .set("elapsedTime", elapsedTime)
                    .set("retainedFrames", retainedFrames)
                    .set("nFrames", totalFrames)
                    .set("durationInMilliseconds", durationInMilliseconds)
                    .set("nFramesUnadjusted", totalFramesUnadjusted)
                    .set("retainedFramesAsFiles", retainedFrames.stream().map(imageSample::get).collect(Collectors.toList()))
                    .set("fps", FPS);
            checkPoint.set("comprehensiveReading", output.internalJSONObject);
            checkPoint.save();

            // TODO - this has been added for a comprehensive reading test...
            //writeToJSON((new File(rootDirectory, "thisComprehensiveReadingDump.json")), output.internalJSONObject);
            return output;
        }
    }

}
