package com.adms.australianmobileadtoolkit.interpreter.platform;


import static com.adms.australianmobileadtoolkit.appSettings.logMessage;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.compositeBoundingBox;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.deleteScreenRecordingAnalysis;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.detailDirectoryStructure;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.evaluationPostMethod;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.groupAdjacentAds;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.inferencePassthrough;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.rectangularArea;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.rectangularAreaOverlap;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.yAgnosticCompositeBoundingBox;

import static java.util.Arrays.asList;

import android.content.Context;
import android.util.Log;

import com.adms.australianmobileadtoolkit.JSONXObject;
import com.adms.australianmobileadtoolkit.checkPoint;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Youtube {
    private static String TAG = "Youtube";


    public static boolean elementIn( String elementType, List<JSONXObject> boundingBoxes) {
        return (boundingBoxes.stream().anyMatch(x -> (x.get("className").equals(elementType))));
    }

    public static List<JSONXObject> getElementsIn( String elementType, List<JSONXObject> boundingBoxes) {
        return (boundingBoxes.stream().filter(x -> (x.get("className").equals(elementType)))).collect(Collectors.toList());
    }

    public static JSONXObject strongestOverlapCandidate(JSONXObject thisX, List<JSONXObject> potentialYs) {
        List<Double> overlapPercentages = potentialYs.stream().map(x -> rectangularAreaOverlap(thisX, x)).collect(Collectors.toList());
        if (!overlapPercentages.isEmpty()) {
            return potentialYs.get(overlapPercentages.indexOf(overlapPercentages.stream().mapToDouble(Double::doubleValue).max().getAsDouble()));
        }
        return null;
    }

    public static void evaluateYoutubeAd(Context context, File rootDirectory, HashMap<String, String> thisInterpretation,
                                        Function<JSONXObject, JSONXObject> objectDetectorFunction, JSONXObject thisComprehensiveReading, Boolean implementedOnAndroid, Boolean applyingQuantizedModels) throws Exception {

        HashMap<String, File> directoryStructure = detailDirectoryStructure(rootDirectory);

        File screenRecordingFile = (new File(directoryStructure.get("videos"), thisInterpretation.get("filename")));
        File screenRecordingAnalysisDirectory = new File(directoryStructure.get("analysis"), thisInterpretation.get("filename") + ".analysis");



        if (!thisComprehensiveReading.keys().isEmpty()) {

            // Undertake preliminary inference
            logMessage(TAG, "a");
            // TODO - improve nano reading model
            List<Integer> retainedFrames = (List<Integer>) thisComprehensiveReading.get("retainedFrames");
            List<String> retainedFramesAsFiles = (List<String>) thisComprehensiveReading.get("retainedFramesAsFiles");

            JSONXObject inferenceResultShallow = inferencePassthrough(context, objectDetectorFunction,"youtube_sponsored",
                    (new JSONXObject()).set("retainedFramesAsFiles", retainedFramesAsFiles).set("retainedFrames", retainedFrames),
                    screenRecordingFile, screenRecordingAnalysisDirectory, applyingQuantizedModels);

            // Group together ads that are adjacent
            JSONXObject groupedAdsObject = groupAdjacentAds(inferenceResultShallow, retainedFrames, retainedFramesAsFiles);
            List<List<Integer>> groupsOfAdFrames = (List<List<Integer>>) groupedAdsObject.get("groupsOfAdFrames");
            JSONXObject inferencesByFrames = (JSONXObject) groupedAdsObject.get("inferencesByFrames");

            logMessage(TAG, groupsOfAdFrames.toString());

            if (groupsOfAdFrames.isEmpty()) {
                logMessage(TAG, "Deleting empty video");
                deleteScreenRecordingAnalysis(screenRecordingFile, screenRecordingAnalysisDirectory, implementedOnAndroid);
            } else {

                // Undertake the deep-pass on all retained frames that contained 'Sponsored' texts
                JSONXObject inferenceResultDeep = inferencePassthrough(context, objectDetectorFunction,
                        "youtube_elements", groupedAdsObject, screenRecordingFile, screenRecordingAnalysisDirectory, applyingQuantizedModels);

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
                checkPoint thisCheckPoint = new checkPoint(screenRecordingFile.getName(), (new File(screenRecordingAnalysisDirectory, "checkpoint")));

                // TODO - Confirm that the ad frames belong to the same ads
                // There are two giveaways for this -
                //  1. whether the crop areas have any degree of overlap between frames
                //  2. Whether the frames have the same classname

                if (thisCheckPoint.container.has("inferenceDeep")) {
                    logMessage(TAG, "Proceeding with ad object construction");
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
                            if (!boundingBoxesDeep.isEmpty()) { // TODO - only proceed for this frame if there are 'deep' bounding boxes - note that if there are no bounding boxes, we can't determine where the contents of the ad is

                                if (elementIn("ENGAGEMENT_BUTTONS", boundingBoxesDeep)) {
                                    if (elementIn("PLUS_BUTTON", boundingBoxesDeep) && elementIn("SPONSORED_TEXT", boundingBoxesShallow)) {
                                        tentativeAdType = "REEL_BASED";
                                    }
                                } else if (Objects.equals(thisInterpretation.get("orientation"), "landscape") && elementIn("SPONSORED_TEXT_HORIZONTAL", boundingBoxesShallow)) {
                                    tentativeAdType = "PREVIEW_LANDSCAPE_BASED";
                                } else if (elementIn("APP_STYLE_ELEMENT", boundingBoxesDeep)) {
                                    tentativeAdType = "APP_FEED_BASED";
                                } else if (elementIn("PRODUCT_IN_THIS_VIDEO_TEXT", boundingBoxesShallow)) {
                                    tentativeAdType = "PRODUCT_FEED_BASED";
                                } else if (elementIn("SPONSORED_TEXT", boundingBoxesShallow)) {
                                    if ((getElementsIn("SPONSORED_TEXT", boundingBoxesShallow).stream().anyMatch(
                                            x -> getElementsIn("PREVIEW_ELEMENT", boundingBoxesDeep).stream().anyMatch(y -> rectangularAreaOverlap(x, y) > 0.0)))) {
                                        tentativeAdType = "GENERAL_FEED_BASED";
                                    } else {
                                        tentativeAdType = "PREVIEW_PORTRAIT_BASED";
                                    }
                                }

                                // TODO - at the moment, we only handle the possibility of a single ad being on a page at a time

                                if (tentativeAdType != null) {
                                    // PRODUCE CROPPING REGION
                                    Double yStart = null;
                                    Double yEnd = null;
                                    Double xStart = 0.0;
                                    Double xEnd = 1.0;
                                    JSONXObject boundingBoxSponsored = null;
                                    if (Arrays.asList("REEL_BASED", "APP_FEED_BASED", "GENERAL_FEED_BASED", "PREVIEW_PORTRAIT_BASED").contains(tentativeAdType)) {
                                        boundingBoxSponsored = getElementsIn("SPONSORED_TEXT", boundingBoxesShallow).get(0);
                                    } else if (Objects.equals(tentativeAdType, "PRODUCT_FEED_BASED")) {
                                        boundingBoxSponsored = getElementsIn("PRODUCT_IN_THIS_VIDEO_TEXT", boundingBoxesShallow).get(0);
                                    } else if (Objects.equals(tentativeAdType, "PREVIEW_LANDSCAPE_BASED")) {
                                        boundingBoxSponsored = getElementsIn("SPONSORED_TEXT_HORIZONTAL", boundingBoxesShallow).get(0);
                                    }
                                    if (Objects.equals(tentativeAdType, "REEL_BASED")) {
                                        boundingBoxSponsored = getElementsIn("SPONSORED_TEXT", boundingBoxesShallow).get(0);
                                        // Remove the top 10% from the view, and go down to the plus button
                                        yStart = 0.1;
                                        Double plusButtonYStart = (Double) getElementsIn("PLUS_BUTTON", boundingBoxesDeep).get(0).get("y1");
                                        Double sponsoredTextYStart = (Double) getElementsIn("SPONSORED_TEXT", boundingBoxesShallow).get(0).get("cy") + 0.05;
                                        // Take the minimum between both values, as the 'Sponsored' text
                                        // may be located before the 'Plus' button
                                        yEnd = Math.min(plusButtonYStart, sponsoredTextYStart);
                                    } else if (Objects.equals(tentativeAdType, "PREVIEW_LANDSCAPE_BASED")) {
                                        boundingBoxSponsored = getElementsIn("SPONSORED_TEXT_HORIZONTAL", boundingBoxesShallow).get(0);
                                        yStart = 0.0;
                                        yEnd = 1.0;
                                    } else if (Objects.equals(tentativeAdType,"APP_FEED_BASED")) {
                                        JSONXObject thisElement = getElementsIn("APP_STYLE_ELEMENT", boundingBoxesDeep).get(0);
                                        yStart = (Double) thisElement.get("y1");
                                        yEnd = (Double) thisElement.get("y2");
                                        xStart = (Double) thisElement.get("x1");
                                        xEnd = (Double) thisElement.get("x2");
                                    } else if (Objects.equals(tentativeAdType, "PRODUCT_FEED_BASED")) {
                                        try {
                                            JSONXObject thisElement = getElementsIn("PRODUCT_ELEMENT", boundingBoxesDeep).get(0);
                                            yStart = (Double) thisElement.get("y1");
                                            yEnd = (Double) thisElement.get("y2");
                                            xStart = (Double) thisElement.get("x1");
                                            xEnd = (Double) thisElement.get("x2");
                                        } catch (Exception e) {}
                                    } else if (Objects.equals(tentativeAdType, "GENERAL_FEED_BASED")) {
                                        // Find out how much each PREVIEW_FOOTER_ELLIPSIS overlaps the 'Sponsored' text
                                        // and return the one with the highest match
                                        JSONXObject containerElement = strongestOverlapCandidate(boundingBoxSponsored, getElementsIn("PREVIEW_ELEMENT", boundingBoxesDeep));
                                        if (containerElement == null) {
                                            containerElement = strongestOverlapCandidate(boundingBoxSponsored, getElementsIn("PREVIEW_FOOTER_ELLIPSIS", boundingBoxesDeep));
                                        }
                                        if (containerElement != null) {
                                            yStart = (Double) containerElement.get("y1");
                                            yEnd = (Double) containerElement.get("y2");
                                            xStart = (Double) containerElement.get("x1");
                                            xEnd = (Double) containerElement.get("x2");
                                        }
                                    }
                                    if ((yStart != null) && (yEnd != null)) {
                                        // Correct y axis issues
                                        Double tempYStart = yStart;
                                        yStart = Math.min(yStart, yEnd);
                                        yEnd = Math.max(tempYStart, yEnd);

                                        JSONXObject boundingBoxCropped = compositeBoundingBox(yStart, yEnd, xEnd, xStart);
                                        thisAdFrameData.set("inference", (new JSONXObject())
                                                .set("boundingBoxCropped", boundingBoxCropped)
                                                .set("boundingBoxSponsored", boundingBoxSponsored)
                                                .set("boundingBoxes", boundingBoxesDeep));
                                        thisAdFrameGroupMetadata.set(adFrame, thisAdFrameData);
                                    }
                                }
                            }
                        }
                        thisAdFrameGroupMetadatasUnseparated.add(thisAdFrameGroupMetadata);
                    }

                    evaluationPostMethod( context, thisAdFrameGroupMetadatasUnseparated,  thisInterpretation,
                            thisComprehensiveReading,  implementedOnAndroid,  inferenceResultShallow,  inferenceResultDeep,
                            rootDirectory,  screenRecordingAnalysisDirectory,  screenRecordingFile, "YOUTUBE");
                }
            }

        }
    }



















}
