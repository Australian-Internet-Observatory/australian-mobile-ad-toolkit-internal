package com.adms.australianmobileadtoolkit.interpreter.platform;


import static com.adms.australianmobileadtoolkit.appSettings.logMessage;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.compositeBoundingBox;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.deleteScreenRecordingAnalysis;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.detailDirectoryStructure;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.evaluationPostMethod;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.groupAdjacentAds;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.inferencePassthrough;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.persistThread;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.rectangularAreaOverlap;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Common.consumerOverGroupedAdFrames;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Common.deriveAds;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Common.organizeAds;

import static java.lang.Integer.parseInt;

import android.content.Context;
import android.util.Log;

import com.adms.australianmobileadtoolkit.JSONXObject;

import org.checkerframework.checker.units.qual.A;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Facebook {
    private static String TAG = "Facebook";


    public static void evaluateFacebookAd(Context context, File rootDirectory, HashMap<String, String> thisInterpretation,
                                       Function<JSONXObject, JSONXObject> objectDetectorFunction, JSONXObject thisComprehensiveReading, Boolean implementedOnAndroid, Boolean applyingQuantizedModels) throws Exception {

        HashMap<String, File> directoryStructure = detailDirectoryStructure(rootDirectory);

        File screenRecordingFile = (new File(directoryStructure.get("videos"), thisInterpretation.get("filename")));
        File screenRecordingAnalysisDirectory = new File(directoryStructure.get("analysis"), thisInterpretation.get("filename") + ".analysis");

        if (!thisComprehensiveReading.keys().isEmpty()) {
            // Undertake preliminary inference
            // TODO - improve nano reading model
            List<Integer> retainedFrames = (List<Integer>) thisComprehensiveReading.get("retainedFrames");
            List<String> retainedFramesAsFiles = (List<String>) thisComprehensiveReading.get("retainedFramesAsFiles");

            JSONXObject inferenceResultShallow = inferencePassthrough(context, objectDetectorFunction,"facebook_sponsored",
                    (new JSONXObject()).set("retainedFramesAsFiles", retainedFramesAsFiles).set("retainedFrames", retainedFrames),
                    screenRecordingFile, screenRecordingAnalysisDirectory, applyingQuantizedModels);

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
                        "facebook_elements", groupedAdsObject, screenRecordingFile, screenRecordingAnalysisDirectory, applyingQuantizedModels);
                logMessage(TAG, "Proceeding with ad object construction");
                List<JSONXObject> thisAdFrameGroupMetadatasUnseparated = new ArrayList<>();
                JSONXObject inferencesDeepByFrames = (new JSONXObject((JSONObject) inferenceResultDeep.get("inferencesByFrames"), true));


                // Firstly determine the types of ads that may be present within the ad
                JSONXObject tentativeAdTypes = new JSONXObject();
                consumerOverGroupedAdFrames(groupsOfAdFrames,inferencesByFrames,inferencesDeepByFrames,(i -> adFrameGroup -> adFrame -> boundingBoxesShallow -> boundingBoxesDeep -> {
                    if (!tentativeAdTypes.has(i)) { tentativeAdTypes.set(i, new JSONXObject()); }
                    JSONXObject adTypesForGroup = (JSONXObject) tentativeAdTypes.get(i);

                    List<String> tentativeAdTypesForFrame = new ArrayList<>();
                    // Presently, Facebook does not have any ad types that might appear at the same time on the screen, although
                    // we split the logic anyway to be consistent
                    boolean sponsoredTextIsPresent = boundingBoxesShallow.stream().anyMatch(x -> x.get("className").equals("SPONSORED_TEXT"));
                    boolean comprisesReelAd = false;
                    boolean comprisesFeedOrStoryAd = false;
                    boolean comprisesMarketplaceAd = false;
                    if ((!boundingBoxesDeep.isEmpty()) && (sponsoredTextIsPresent)) {
                        // only proceed for this frame if there are 'deep' bounding boxes - note that if there are no bounding boxes, we can't determine where the contents of the ad is

                        if (boundingBoxesDeep.stream().anyMatch(x -> (x.get("className").equals("VIDEO_BUTTONS")))) {
                            if (boundingBoxesDeep.stream().anyMatch(x -> (x.get("className").equals("CROSS_AND_ELLIPSIS")))) {
                                tentativeAdTypesForFrame.add("REEL_FOOTER_BASED");
                            } else {
                                tentativeAdTypesForFrame.add("REEL_BASED");
                            }
                            comprisesReelAd = true;
                        }

                        if (boundingBoxesDeep.stream().anyMatch(x -> (x.get("className").equals("MARKETPLACE_ELEMENT")))) {
                            tentativeAdTypesForFrame.add("MARKETPLACE_BASED");
                            comprisesMarketplaceAd = true;
                        }

                        if ((boundingBoxesDeep.stream().anyMatch(x -> (x.get("className").equals("POST_HEADER"))))) {
                            if ((boundingBoxesDeep.stream().anyMatch(x -> (x.get("className").equals("POST_HEADER") && (((Double) x.get("cy")) <= 0.25))))
                                    && (boundingBoxesDeep.stream().noneMatch(x -> ((Arrays.asList("POST_HEADER", "POST_FOOTER").contains((String) x.get("className"))) && (((Double) x.get("cy")) > 0.25))))) {
                                tentativeAdTypesForFrame.add("STORY_BASED");
                            } else {
                                tentativeAdTypesForFrame.add("FEED_BASED");
                            }
                            comprisesFeedOrStoryAd = true;
                        }

                    }

                    //Guarding logic (against overloads)
                    // Reel-based ads do not appear with marketplace ads or feed-based ads (default to reels)
                    if (comprisesReelAd && (comprisesMarketplaceAd || comprisesFeedOrStoryAd)) {
                        tentativeAdTypesForFrame = tentativeAdTypesForFrame.stream()
                                .filter(x -> !Arrays.asList("MARKETPLACE_BASED", "FEED_BASED", "STORY_BASED").contains(x)).collect(Collectors.toList());
                    }
                    // Marketplace ads do not appear with feed-based ads (or reel based ads - but that's already handled in the previous block)
                    if (comprisesMarketplaceAd && (comprisesFeedOrStoryAd)) {
                        tentativeAdTypesForFrame = tentativeAdTypesForFrame.stream()
                                .filter(x -> !Arrays.asList("FEED_BASED", "STORY_BASED").contains(x)).collect(Collectors.toList());
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

                    if (Objects.equals(tentativeAdType, "MARKETPLACE_BASED")) {
                        try {
                            // Occassionally, a 'Sponsored by sellers' section may be confused as a 'Sponsored' ad, and so we have to disregard it
                            if (boundingBoxesDeep.stream().noneMatch(x -> (x.get("className").equals("SPONSORED_BY_SELLERS_TEXT") && (rectangularAreaOverlap(x, boundingBoxSponsored) > 0.0) ))) {
                                // Find a post header that overlaps the sponsored text - this is the post header of the ad
                                JSONXObject postHeaderElement = boundingBoxesDeep.stream()
                                        .filter(x -> (x.get("className").equals("POST_HEADER")
                                                && (rectangularAreaOverlap(x, boundingBoxSponsored) > 0.0) )).collect(Collectors.toList()).get(0);

                                // Find a marketplace element with a y that is larger than that of the sponsored text, but that encapsulates its x axis
                                List<JSONXObject> tentativeMarketplaceElements = boundingBoxesDeep.stream()
                                        .filter(x -> (x.get("className").equals("MARKETPLACE_ELEMENT")
                                                && (((Double) x.get("cy")) > sponsoredTextCenterY)
                                                && (sponsoredTextCenterX >= (Double) x.get("x1"))
                                                && (sponsoredTextCenterX <= (Double) x.get("x2")))).collect(Collectors.toList());

                                // For all candidates, isolate the one that with the smallest cy
                                Double minimumYOfMarketplaceElement = 1.0;
                                JSONXObject marketplaceElement = null;
                                for (JSONXObject x : tentativeMarketplaceElements) {
                                    Double thisCY = (Double) x.get("cy");
                                    if (thisCY < minimumYOfMarketplaceElement) {
                                        marketplaceElement = x;
                                        minimumYOfMarketplaceElement = thisCY;
                                    } // TODO - not used
                                }
                                lowerMostX = (Double) marketplaceElement.get("x1");
                                upperMostX = (Double) marketplaceElement.get("x2");
                                upperMostY = (Double) postHeaderElement.get("y1");
                                lowerMostY = (Double) marketplaceElement.get("y2");
                            }
                        } catch (Exception ignored) {
                            // The post header element may not exist
                        }
                    }

                    if (Objects.equals(tentativeAdType, "REEL_FOOTER_BASED")) {
                        // We will only regard the footer-based ad if it is well-formed
                        // The CROSS_AND_ELLPSIS's Y coordinates must encapsulate the Y coordinates of the SPONSORED_TEXT
                        JSONXObject crossAndEllipsisElement = boundingBoxesDeep.stream()
                                .filter(x -> (x.get("className").equals("CROSS_AND_ELLIPSIS") )).collect(Collectors.toList()).get(0);
                        Double crossAndEllipsisElementY1 = (Double) crossAndEllipsisElement.get("y1");
                        Double crossAndEllipsisElementY2 = (Double) crossAndEllipsisElement.get("y2");
                        if ((crossAndEllipsisElementY1 <= sponsoredTextCenterY) && (crossAndEllipsisElementY2 >= sponsoredTextCenterY)) {
                            upperMostY = crossAndEllipsisElementY1;
                            lowerMostY = crossAndEllipsisElementY2;
                        } else {
                            // Do nothing ie. disregard
                        }
                    }

                    if (Objects.equals(tentativeAdType, "REEL_BASED")) {
                        lowerMostY = (Double) boundingBoxSponsored.get("y2");
                        try {
                            JSONXObject engagementButtonsElement = boundingBoxesDeep.stream()
                                    .filter(x -> (x.get("className").equals("ENGAGEMENT_BUTTONS") )).collect(Collectors.toList()).get(0);
                            Double tentativeTargetLowerElementY2 = (Double) engagementButtonsElement.get("y2");
                            if (tentativeTargetLowerElementY2 > lowerMostY) {
                                lowerMostY = tentativeTargetLowerElementY2;
                            }
                        } catch (Exception ignored) {
                            // The engagement buttons might not exist
                        }
                        JSONXObject videoButtonsElement = boundingBoxesDeep.stream().filter(x -> (x.get("className").equals("VIDEO_BUTTONS") )).collect(Collectors.toList()).get(0);
                        upperMostY = (Double) videoButtonsElement.get("y1");
                    }

                    if (Objects.equals(tentativeAdType, "STORY_BASED")) {
                        // Cut directly from the post header and go downwards
                        JSONXObject postHeaderElement = boundingBoxesDeep.stream()
                                .filter(x -> (x.get("className").equals("POST_HEADER") )).collect(Collectors.toList()).get(0);
                        lowerMostX = (Double) postHeaderElement.get("x1");
                        upperMostX = (Double) postHeaderElement.get("x2");
                        upperMostY = (Double) postHeaderElement.get("y1");
                        lowerMostY = 1.0;
                    }

                    if (Objects.equals(tentativeAdType, "FEED_BASED")) {
                        try {
                            JSONXObject postHeaderElement = boundingBoxesDeep.stream()
                                    .filter(x -> (x.get("className").equals("POST_HEADER")) && (rectangularAreaOverlap(x, boundingBoxSponsored) > 0.0))
                                    .collect(Collectors.toList()).get(0);
                            upperMostY = (Double) postHeaderElement.get("y1");
                            // Find the post header (or footer) (or end of page), and declare it as the lower bound of the ad
                            try {
                                lowerMostY = Math.min(Collections.min(boundingBoxesDeep.stream()
                                        .filter(x -> ((x.get("className").equals("POST_HEADER")) || (x.get("className").equals("POST_FOOTER")))
                                                && (rectangularAreaOverlap(x, boundingBoxSponsored) <= 0.0)
                                                && (((Double) x.get("cy")) > sponsoredTextCenterY)).map(y -> (Double) y.get("y1")).collect(Collectors.toList())), 1.0);
                            } catch (Exception e) {
                                lowerMostY = 1.0; // When the bottom of the ad cannot refer to a post header or post footer, simply grab the entire lower portion of the screen.
                            }
                        } catch (Exception ignored) {
                            ignored.printStackTrace();
                            // The post header may not exist
                        }
                    }

                    return (new JSONXObject())
                            .set("lowerMostX", lowerMostX)
                            .set("upperMostX", upperMostX)
                            .set("upperMostY", upperMostY)
                            .set("lowerMostY", lowerMostY);
                }));

                organizeAds(thisAdFrameGroupMetadatasUnseparated, adFrameGroupsAdsFrames);

                evaluationPostMethod( context, thisAdFrameGroupMetadatasUnseparated, thisInterpretation,
                        thisComprehensiveReading, implementedOnAndroid, inferenceResultShallow, inferenceResultDeep,
                        rootDirectory, screenRecordingAnalysisDirectory,  screenRecordingFile, "FACEBOOK");


            }
        }
    }
}
