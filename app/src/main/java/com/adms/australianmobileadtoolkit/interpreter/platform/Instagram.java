package com.adms.australianmobileadtoolkit.interpreter.platform;


import static com.adms.australianmobileadtoolkit.appSettings.logMessage;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.compositeBoundingBox;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.deleteScreenRecordingAnalysis;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.detailDirectoryStructure;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.evaluationPostMethod;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.groupAdjacentAds;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.inferencePassthrough;

import android.content.Context;
import android.util.Log;

import com.adms.australianmobileadtoolkit.JSONXObject;

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

public class Instagram {
    private static String TAG = "Instagram";

    public static void evaluateInstagramAd(Context context, File rootDirectory, HashMap<String, String> thisInterpretation,
                                       Function<JSONXObject, JSONXObject> objectDetectorFunction, JSONXObject thisComprehensiveReading, Boolean implementedOnAndroid, Boolean applyingQuantizedModels) throws Exception {

        HashMap<String, File> directoryStructure = detailDirectoryStructure(rootDirectory);

        File screenRecordingFile = (new File(directoryStructure.get("videos"), thisInterpretation.get("filename")));
        File screenRecordingAnalysisDirectory = new File(directoryStructure.get("analysis"), thisInterpretation.get("filename") + ".analysis");

        if (!thisComprehensiveReading.keys().isEmpty()) {


            // Undertake preliminary inference
            // TODO - improve nano reading model
            List<Integer> retainedFrames = (List<Integer>) thisComprehensiveReading.get("retainedFrames");
            List<String> retainedFramesAsFiles = (List<String>) thisComprehensiveReading.get("retainedFramesAsFiles");

            JSONXObject inferenceResultShallow = inferencePassthrough(context, objectDetectorFunction,"instagram_sponsored",
                    (new JSONXObject()).set("retainedFramesAsFiles", retainedFramesAsFiles).set("retainedFrames", retainedFrames),
                    screenRecordingFile, screenRecordingAnalysisDirectory, applyingQuantizedModels);
            //writeToJSON((new File(rootDirectory, "inferenceResultShallow.json")), inferenceResultShallow.internalJSONObject);

            // Group together ads that are adjacent
            JSONXObject groupedAdsObject = groupAdjacentAds(inferenceResultShallow, retainedFrames, retainedFramesAsFiles);
            List<List<Integer>> groupsOfAdFrames = (List<List<Integer>>) groupedAdsObject.get("groupsOfAdFrames");
            JSONXObject inferencesByFrames = (JSONXObject) groupedAdsObject.get("inferencesByFrames");

            if (groupsOfAdFrames.isEmpty()) {
                logMessage(TAG, "Deleting empty video");
                deleteScreenRecordingAnalysis(screenRecordingFile, screenRecordingAnalysisDirectory, implementedOnAndroid);
            } else {

                // Undertake the deep-pass on all retained frames that contained 'Sponsored' texts
                JSONXObject inferenceResultDeep = inferencePassthrough(context, objectDetectorFunction,
                        "instagram_elements", groupedAdsObject, screenRecordingFile, screenRecordingAnalysisDirectory, applyingQuantizedModels);

                // There are four kinds of ads that we have observed in Instagram - feed-based, reel-based, stories-based, and 'Explore' based
                // The former three can be reliably captured, while the latter can't be.

                // Construct ad objects

                //checkPoint thisCheckPoint = new checkPoint(screenRecordingFile.getName(), (new File(screenRecordingAnalysisDirectory, "checkpoint")));

                // TODO - Confirm that the ad frames belong to the same ads
                // There are two giveaways for this -
                //  1. whether the crop areas have any degree of overlap between frames
                //  2. Whether the frames have the same classname

                //if (thisCheckPoint.container.has("inferenceDeep")) {
                    logMessage(TAG, "Proceeding with ad object construction");
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

                                // We assert that there is only ever one bounding box for a Sponsored text on any ad, and it is the one with the highest confidence
                                Double maxConfidenceSponsoredTextCandidate = Collections.max(boundingBoxesShallow.stream().map(x-> (Double) x.get("confidence")).collect(Collectors.toList()).stream().filter(y->y != null).collect(Collectors.toList()));
                                JSONXObject boundingBoxSponsored = boundingBoxesShallow.stream().filter(x ->
                                        x.get("className").equals("SPONSORED_TEXT") && (Objects.equals(x.get("confidence"), maxConfidenceSponsoredTextCandidate))).collect(Collectors.toList()).get(0); // TODO - cannot simply refer to one
                                Double sponsoredTextCenterY = (Double) boundingBoxSponsored.get("cy");

                                // Are there any engagement buttons present? And secondly, is the 'Sponsored' text overlapping the element on the y axis
                                // (this is really important for weeding out posts that screenshot reels - yes it exists)
                                if (boundingBoxesDeep.stream().anyMatch(x -> (x.get("className").equals("BUTTONS_ENGAGEMENT")
                                        && ((((Double) x.get("y1")) <= sponsoredTextCenterY) && (((Double) x.get("y2")) >= sponsoredTextCenterY))))) {
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

                                // PRODUCE CROPPING REGION
                                Double lowerMostY = null;
                                Double upperMostY = null;
                                Double lowerMostX = 0.0;
                                Double upperMostX = 1.0;
                                if (Objects.equals(tentativeAdType, "REEL_BASED")) {
                                    // For reel-based ads, we cannot determine the identity of the status bar, and so have to
                                    // discard an agnostic 15% off the entire y axis

                                    // Beyond this, the bottom y is defined as the furthest extent of either the SPONSORED_TEXT
                                    // or the BUTTONS_ENGAGEMENT
                                    upperMostY = 0.15;

                                    lowerMostY = Collections.max(boundingBoxesDeep.stream().filter(x -> Arrays.asList(
                                                    "SPONSORED_TEXT", "BUTTONS_ENGAGEMENT").contains((String) x.get("className")))
                                            .map(y -> (Double) y.get("y2")).collect(Collectors.toList()));
                                } else if (Objects.equals(tentativeAdType, "STORY_BASED")) {
                                    // Take the upper-most y coordinate of the first FEED_POST_HEADER
                                    // and crop down the entire frame

                                    JSONXObject feedPostHeaderBoundingBox = boundingBoxesDeep.stream()
                                            .filter(x->x.get("className").equals("FEED_POST_HEADER"))
                                            .collect(Collectors.toList()).get(0);
                                    upperMostY = (Double) feedPostHeaderBoundingBox.get("y1");
                                    lowerMostY = 1.0;
                                    lowerMostX = (Double) feedPostHeaderBoundingBox.get("x1");
                                    upperMostX = (Double) feedPostHeaderBoundingBox.get("x2");
                                } else if (Objects.equals(tentativeAdType, "FEED_BASED")) {
                                    try {
                                        // Identify the FEED_POST_HEADER that intersects the SPONSORED_TEXT - we are going
                                        // to make an assumption that this always exists
                                        upperMostY = (Double) boundingBoxesDeep.stream()
                                                .filter(x -> x.get("className").equals("FEED_POST_HEADER")
                                                        && ((((Double) x.get("y1")) <= sponsoredTextCenterY) && (((Double) x.get("y2")) >= sponsoredTextCenterY)))
                                                .collect(Collectors.toList()).get(0).get("y1");
                                        // We are also going to assume that the lowerMostY is always well-formed, even though
                                        // its possible that it could latch onto the same element that produced the upperMostY
                                        //
                                        // In fact, the lowermost Y must be larger than the uppermost Y, as otherwise, it might latch onto a separate element
                                        Double finalUpperMostY = upperMostY;
                                        lowerMostY = Collections.min(boundingBoxesDeep.stream().filter(x -> Arrays.asList(
                                                        "FEED_POST_HEADER", "BUTTON_NEW_POST").contains((String) x.get("className")) && (((Double) x.get("y1") > finalUpperMostY)))
                                                .map(y -> (Double) y.get("y1")).collect(Collectors.toList()));
                                    } catch (Exception ignored) {
                                        // This block occurs on a malformed feed-based ad
                                    }
                                }


                                // For all three currently retrieved ad types, the upperMost and lowerMost Y crop the ad using the
                                // same method
                                if ((lowerMostY != null) && (upperMostY != null)) {
                                    // Correct y axis issues
                                    Double tempUpperMostY = upperMostY;
                                    upperMostY = Math.min(upperMostY, lowerMostY);
                                    lowerMostY = Math.max(tempUpperMostY, lowerMostY);

                                    JSONXObject boundingBoxCropped = compositeBoundingBox(upperMostY, lowerMostY, upperMostX, lowerMostX);
                                    thisAdFrameData.set("inference", (new JSONXObject())
                                            .set("boundingBoxCropped", boundingBoxCropped)
                                            .set("boundingBoxSponsored", boundingBoxSponsored)
                                            .set("boundingBoxes", boundingBoxesDeep));
                                    thisAdFrameGroupMetadata.set(adFrame, thisAdFrameData);
                                    //writeToJSON((new File(rootDirectory, "thisAdFrameGroupMetadata."+UUID.randomUUID().toString()+".json")), thisAdFrameGroupMetadata.internalJSONObject);
                                }
                            }
                        }
                        thisAdFrameGroupMetadatasUnseparated.add(thisAdFrameGroupMetadata);
                    }

                    evaluationPostMethod( context, thisAdFrameGroupMetadatasUnseparated,  thisInterpretation,
                            thisComprehensiveReading,  implementedOnAndroid,  inferenceResultShallow,  inferenceResultDeep,
                            rootDirectory,  screenRecordingAnalysisDirectory,  screenRecordingFile, "INSTAGRAM");
                //}
            }
        }
    }



















}
