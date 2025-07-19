package com.adms.australianmobileadtoolkit.interpreter.platform;


import static com.adms.australianmobileadtoolkit.appSettings.logMessage;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.compositeBoundingBox;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.deleteScreenRecordingAnalysis;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.detailDirectoryStructure;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.evaluationPostMethod;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.groupAdjacentAds;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.inferencePassthrough;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Common.consumerOverGroupedAdFrames;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Common.deriveAds;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Common.organizeAds;

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
                List<JSONXObject> thisAdFrameGroupMetadatasUnseparated = new ArrayList<>();
                JSONXObject inferencesDeepByFrames = (new JSONXObject((JSONObject) inferenceResultDeep.get("inferencesByFrames"), true));

                // Firstly determine the types of ads that may be present within the ad
                JSONXObject tentativeAdTypes = new JSONXObject();
                consumerOverGroupedAdFrames(groupsOfAdFrames,inferencesByFrames,inferencesDeepByFrames,(i -> adFrameGroup -> adFrame -> boundingBoxesShallow -> boundingBoxesDeep -> {
                    if (!tentativeAdTypes.has(i)) { tentativeAdTypes.set(i, new JSONXObject()); }
                    JSONXObject adTypesForGroup = (JSONXObject) tentativeAdTypes.get(i);

                    List<String> tentativeAdTypesForFrame = new ArrayList<>();
                    boolean sponsoredTextIsPresent = boundingBoxesShallow.stream().anyMatch(x -> x.get("className").equals("SPONSORED_TEXT"));
                    if ((!boundingBoxesDeep.isEmpty()) && (sponsoredTextIsPresent)) {

                        // Do any Sponsored texts overlap the engagement buttons on the y axis
                        if (boundingBoxesDeep.stream().anyMatch(x -> (x.get("className").equals("BUTTONS_ENGAGEMENT")
                                && (boundingBoxesShallow.stream().anyMatch(y -> y.get("className").equals("SPONSORED_TEXT") && ((((Double) x.get("y1")) <= (Double) y.get("cy")) && (((Double) x.get("y2")) >= (Double) y.get("cy")))))
                        ))) {
                            // If yes, we are dealing with a reel-based ad
                            tentativeAdTypesForFrame.add("REEL_BASED");
                        }

                        if (boundingBoxesDeep.stream().anyMatch(x -> (x.get("className").equals("FEED_POST_HEADER")))) {
                            // For all frames in the entire adFrameGroup, have the
                            // FEED_POST_HEADER or SPONSORED_TEXT not appeared in the lower 25% of the y axis? Furthermore,
                            // is the BUTTON_NEW_POST absent? If so, we're dealing with a story-based ad
                            boolean isStoryType = adFrameGroup.stream().allMatch(y -> {
                                List<JSONXObject> boundingBoxesDeepAlt = ((List<JSONObject>) inferencesDeepByFrames.get(y))
                                        .stream().map(x -> (new JSONXObject(x, true))).collect(Collectors.toList());
                                return boundingBoxesDeepAlt.stream().allMatch(z -> {
                                    // BUTTON_NEW_POST is nowhere on the page
                                    boolean buttonNewPostNotInFrame = (!(z.get("className").equals("BUTTON_NEW_POST") && (((double) z.get("confidence")) > 0.3)));
                                    // is there a FEED_POST_HEADER or SPONSORED_TEXT in the lower 75% of the y axis
                                    boolean lowerQuartersOfYAxisContainPosts = ((
                                            z.get("className").equals("FEED_POST_HEADER")
                                                    || z.get("className").equals("SPONSORED_TEXT"))
                                            && (((double) z.get("cy")) >= 0.25));

                                    return (buttonNewPostNotInFrame && (!lowerQuartersOfYAxisContainPosts));
                                });
                            });
                            if (isStoryType) {
                                tentativeAdTypesForFrame.add("STORY_BASED");
                            }
                        }

                        if (boundingBoxesDeep.stream().anyMatch(x -> (x.get("className").equals("FEED_POST_HEADER")))) {
                            tentativeAdTypesForFrame.add("FEED_BASED");
                        }
                    }

                    //Guarding logic (against overloads)
                    // Reel-based ads do not appear with other ad types
                    if (tentativeAdTypesForFrame.contains("REEL_BASED") && (tentativeAdTypesForFrame.contains("STORY_BASED") || tentativeAdTypesForFrame.contains("FEED_BASED"))) {
                        tentativeAdTypesForFrame = tentativeAdTypesForFrame.stream()
                                .filter(x -> !Arrays.asList("STORY_BASED", "FEED_BASED").contains(x)).collect(Collectors.toList());
                    }

                    if (tentativeAdTypesForFrame.contains("STORY_BASED") && (tentativeAdTypesForFrame.contains("FEED_BASED"))) {
                        tentativeAdTypesForFrame = tentativeAdTypesForFrame.stream()
                                .filter(x -> !Objects.equals("FEED_BASED", x)).collect(Collectors.toList());
                    }
                    adTypesForGroup.set(adFrame, tentativeAdTypesForFrame);
                }));


                JSONXObject adFrameGroupsAdsFrames = deriveAds(List.of("SPONSORED_TEXT"), tentativeAdTypes, groupsOfAdFrames, inferencesByFrames, inferencesDeepByFrames,
                        (tentativeAdType -> boundingBoxesDeep -> boundingBoxSponsored -> sponsoredTextCenterX -> sponsoredTextCenterY -> {

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
                    }

                    if (Objects.equals(tentativeAdType, "STORY_BASED")) {
                        // Take the upper-most y coordinate of the first FEED_POST_HEADER
                        // and crop down the entire frame

                        JSONXObject feedPostHeaderBoundingBox = boundingBoxesDeep.stream()
                                .filter(x->x.get("className").equals("FEED_POST_HEADER"))
                                .collect(Collectors.toList()).get(0);
                        upperMostY = (Double) feedPostHeaderBoundingBox.get("y1");
                        lowerMostY = 1.0;
                        lowerMostX = (Double) feedPostHeaderBoundingBox.get("x1");
                        upperMostX = (Double) feedPostHeaderBoundingBox.get("x2");
                    }

                    if (Objects.equals(tentativeAdType, "FEED_BASED")) {
                        boolean hello = true;
                        try {
                            if (((Double) boundingBoxSponsored.get("confidence")) > 0.4) {
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
                                try {
                                    Double finalUpperMostY = upperMostY;
                                    lowerMostY = Collections.min(boundingBoxesDeep.stream().filter(x -> Arrays.asList(
                                                    "FEED_POST_HEADER", "BUTTON_NEW_POST").contains((String) x.get("className")) && (((Double) x.get("y1") > finalUpperMostY)))
                                            .map(y -> (Double) y.get("y1")).collect(Collectors.toList()));
                                } catch (Exception e) {
                                    lowerMostY = 1.0; // When the bottom of the ad cannot refer to a post header or post footer, simply grab the entire lower portion of the screen.
                                }
                            }
                        } catch (Exception ignored) {
                            ignored.printStackTrace();
                            // This block occurs on a malformed feed-based ad
                        }
                    }

                    return (new JSONXObject())
                            .set("lowerMostX", lowerMostX)
                            .set("upperMostX", upperMostX)
                            .set("upperMostY", upperMostY)
                            .set("lowerMostY", lowerMostY);
                }));

                organizeAds(thisAdFrameGroupMetadatasUnseparated, adFrameGroupsAdsFrames);

                evaluationPostMethod( context, thisAdFrameGroupMetadatasUnseparated,  thisInterpretation,
                        thisComprehensiveReading,  implementedOnAndroid,  inferenceResultShallow,  inferenceResultDeep,
                        rootDirectory,  screenRecordingAnalysisDirectory,  screenRecordingFile, "INSTAGRAM");
            }
        }
    }
}
