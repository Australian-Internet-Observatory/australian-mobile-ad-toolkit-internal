package com.adms.australianmobileadtoolkit.interpreter.platform;


import static com.adms.australianmobileadtoolkit.appSettings.logMessage;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.deleteScreenRecordingAnalysis;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.detailDirectoryStructure;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.evaluationPostMethod;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.groupAdjacentAds;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.inferencePassthrough;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.rectangularArea;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.rectangularAreaOverlap;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.yAgnosticCompositeBoundingBox;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Common.consumerOverGroupedAdFrames;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Common.deriveAds;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Common.organizeAds;

import static java.util.Arrays.asList;

import android.content.Context;
import android.util.Log;

import com.adms.australianmobileadtoolkit.JSONXObject;

import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TikTok {
    private static String TAG = "TikTok";




    public static Double tiktokLowerMostY(List<JSONXObject> boundingBoxes, JSONXObject boundingBoxSponsored) {
        List<JSONXObject> boundingBoxEngagementButtonsTentative = boundingBoxes.stream().filter(x ->
                x.get("className").equals("ENGAGEMENT_BUTTONS")).collect(Collectors.toList());
        Double lowerMostY = (boundingBoxEngagementButtonsTentative.isEmpty()) ? (double) boundingBoxSponsored.get("y2") :
                Math.max((double) boundingBoxEngagementButtonsTentative.get(0).get("y2"), (double) boundingBoxSponsored.get("y2"));
        return lowerMostY;
    }

    public static void evaluateTikTokAd(Context context, File rootDirectory, HashMap<String, String> thisInterpretation,
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

            JSONXObject inferenceResultShallow = inferencePassthrough(context, objectDetectorFunction,"tiktok_sponsored",
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
                        "tiktok_elements", groupedAdsObject, screenRecordingFile, screenRecordingAnalysisDirectory, applyingQuantizedModels);
                logMessage(TAG, "Proceeding with ad object construction");
                List<JSONXObject> thisAdFrameGroupMetadatasUnseparated = new ArrayList<>();
                JSONXObject inferencesDeepByFrames = (new JSONXObject((JSONObject) inferenceResultDeep.get("inferencesByFrames"), true));

                // Firstly determine the types of ads that may be present within the ad
                JSONXObject tentativeAdTypes = new JSONXObject();
                consumerOverGroupedAdFrames(groupsOfAdFrames,inferencesByFrames,inferencesDeepByFrames,(i -> adFrameGroup -> adFrame -> boundingBoxesShallow -> boundingBoxesDeep -> {
                    if (!tentativeAdTypes.has(i)) { tentativeAdTypes.set(i, new JSONXObject()); }
                    JSONXObject adTypesForGroup = (JSONXObject) tentativeAdTypes.get(i);

                    List<String> tentativeAdTypesForFrame = new ArrayList<>();

                    boolean sponsoredTextIsPresent = boundingBoxesShallow.stream().anyMatch(x -> Arrays.asList("PAID_PARTNERSHIP_TEXT", "SPONSORED_TEXT", "PROMOTIONAL_CONTENT_TEXT").contains(x.get("className")));
                    if ((!boundingBoxesDeep.isEmpty()) && (sponsoredTextIsPresent)) {

                        if (boundingBoxesDeep.stream().anyMatch(x -> (x.get("className").equals("POST_THUMBNAIL")))) {
                            tentativeAdTypesForFrame.add("THUMBNAIL");
                        }

                        if ((boundingBoxesDeep.stream().anyMatch(x -> (x.get("className").equals("REEL_SEARCH_INPUT")))) || (boundingBoxesDeep.stream().anyMatch(x -> Arrays.asList("ENGAGEMENT_BUTTONS").contains(x.get("className"))))) {
                            tentativeAdTypesForFrame.add("REEL_FROM_SEARCH");
                        }

                        if (boundingBoxesDeep.stream().anyMatch(x -> Arrays.asList("LIVE_BUTTON", "SEARCH_BUTTON").contains(x.get("className")))) {
                            tentativeAdTypesForFrame.add("REEL_FROM_HOME");
                        }
                    }

                    //Guarding logic (against overloads)
                    if (tentativeAdTypesForFrame.contains("REEL_FROM_SEARCH") && (tentativeAdTypesForFrame.contains("REEL_FROM_HOME"))) {
                        tentativeAdTypesForFrame = tentativeAdTypesForFrame.stream()
                                .filter(x -> !Objects.equals("REEL_FROM_HOME", x)).collect(Collectors.toList());
                    }
                    if (tentativeAdTypesForFrame.contains("THUMBNAIL") && (tentativeAdTypesForFrame.contains("REEL_FROM_SEARCH") || tentativeAdTypesForFrame.contains("REEL_FROM_HOME"))) {
                        tentativeAdTypesForFrame = tentativeAdTypesForFrame.stream()
                                .filter(x -> !Arrays.asList("THUMBNAIL").contains(x)).collect(Collectors.toList());
                    }

                    adTypesForGroup.set(adFrame, tentativeAdTypesForFrame);
                }));


                JSONXObject adFrameGroupsAdsFrames = deriveAds(List.of("PAID_PARTNERSHIP_TEXT", "SPONSORED_TEXT", "PROMOTIONAL_CONTENT_TEXT"), tentativeAdTypes, groupsOfAdFrames, inferencesByFrames, inferencesDeepByFrames,
                        (tentativeAdType -> boundingBoxesDeep -> boundingBoxSponsored -> sponsoredTextCenterX -> sponsoredTextCenterY -> {

                    // PRODUCE CROPPING REGION
                    Double lowerMostY = null;
                    Double upperMostY = null;
                    Double lowerMostX = 0.0;
                    Double upperMostX = 1.0;

                    // TODO - technically, the way around this is to have a consistent(ly large) number of training candidates for all terms, although we have enough in practise to get a stable confidence threshold
                    // for classes that have fewer candidates - enforce a stronger confidence threshold
                    if ((((Double) boundingBoxSponsored.get("confidence") >= 0.1) && ((String) boundingBoxSponsored.get("className")).equals("SPONSORED_TEXT")) ||
                            (((Double) boundingBoxSponsored.get("confidence") >= 0.5) && (!((String) boundingBoxSponsored.get("className")).equals("SPONSORED_TEXT")))) {

                        if (Objects.equals(tentativeAdType, "THUMBNAIL")) {
                            Double maxOverlap = Collections.max(boundingBoxesDeep.stream().map(x -> (rectangularAreaOverlap(x, boundingBoxSponsored) / rectangularArea(boundingBoxSponsored))).collect(Collectors.toList()));
                            for (JSONXObject boundingBoxThumbnail : boundingBoxesDeep) {
                                if (boundingBoxThumbnail.get("className").equals("POST_THUMBNAIL")) {
                                    Double intersectionPercentage = (rectangularAreaOverlap(boundingBoxThumbnail, boundingBoxSponsored)
                                            / rectangularArea(boundingBoxSponsored));
                                    if ((intersectionPercentage.equals(maxOverlap)) && (!intersectionPercentage.equals(0.0))) {
                                        lowerMostX = (Double) boundingBoxThumbnail.get("x1");
                                        upperMostX = (Double) boundingBoxThumbnail.get("x2");
                                        upperMostY = (Double) boundingBoxThumbnail.get("y1");
                                        lowerMostY = (Double) boundingBoxThumbnail.get("y2");
                                        break;
                                    }
                                }
                            }
                        }

                        if (Objects.equals(tentativeAdType, "REEL_FROM_SEARCH")) {
                            lowerMostY = tiktokLowerMostY(boundingBoxesDeep, boundingBoxSponsored);
                            // Identify the comprising bounding boxes
                            try {
                                JSONXObject boundingBoxSearchInput = boundingBoxesDeep.stream().filter(x ->
                                        x.get("className").equals("REEL_SEARCH_INPUT")).collect(Collectors.toList()).get(0);
                                upperMostY = (double) boundingBoxSearchInput.get("y1");
                            } catch (Exception ignored) {
                                upperMostY = 0.15; // The search input element is difficult to locate sometimes - so then rather than throw the ad, we simply set the uppermost y as such
                            }
                        }

                        if (Objects.equals(tentativeAdType, "REEL_FROM_HOME")) {
                            lowerMostY = tiktokLowerMostY(boundingBoxesDeep, boundingBoxSponsored);
                            List<JSONXObject> boundingBoxLiveButtonsTentative = boundingBoxesDeep.stream().filter(x ->
                                    x.get("className").equals("LIVE_BUTTON")).collect(Collectors.toList());
                            List<JSONXObject> boundingBoxSearchButtonsTentative = boundingBoxesDeep.stream().filter(x ->
                                    x.get("className").equals("SEARCH_BUTTON")).collect(Collectors.toList());
                            Double y1LiveButton = (!boundingBoxLiveButtonsTentative.isEmpty()) ? (double) boundingBoxLiveButtonsTentative.get(0).get("y1") : 1.0;
                            Double y1SearchButton = (!boundingBoxSearchButtonsTentative.isEmpty()) ? (double) boundingBoxSearchButtonsTentative.get(0).get("y1") : 1.0;
                            upperMostY = Math.min(y1LiveButton, y1SearchButton);
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
                        rootDirectory,  screenRecordingAnalysisDirectory,  screenRecordingFile, "TIKTOK");
            }
        }
    }
}
