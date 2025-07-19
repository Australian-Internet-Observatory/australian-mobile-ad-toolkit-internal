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
import static com.adms.australianmobileadtoolkit.interpreter.platform.Common.consumerOverGroupedAdFrames;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Common.deriveAds;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Common.organizeAds;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Common.strongestOverlap;

import static java.util.Arrays.asList;

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
                logMessage(TAG, "Proceeding with ad object construction");
                List<JSONXObject> thisAdFrameGroupMetadatasUnseparated = new ArrayList<>();
                JSONXObject inferencesDeepByFrames = (new JSONXObject((JSONObject) inferenceResultDeep.get("inferencesByFrames"), true));

                // Firstly determine the types of ads that may be present within the ad
                JSONXObject tentativeAdTypes = new JSONXObject();
                consumerOverGroupedAdFrames(groupsOfAdFrames,inferencesByFrames,inferencesDeepByFrames,(i -> adFrameGroup -> adFrame -> boundingBoxesShallow -> boundingBoxesDeep -> {
                    if (!tentativeAdTypes.has(i)) { tentativeAdTypes.set(i, new JSONXObject()); }
                    JSONXObject adTypesForGroup = (JSONXObject) tentativeAdTypes.get(i);

                    List<String> tentativeAdTypesForFrame = new ArrayList<>();
                    boolean sponsoredTextIsPresent = boundingBoxesShallow.stream().anyMatch(x -> Arrays.asList("PRODUCT_IN_THIS_VIDEO_TEXT", "SPONSORED_TEXT", "SPONSORED_TEXT_HORIZONTAL").contains(x.get("className")));
                    if (sponsoredTextIsPresent) {


                        if (elementIn("ENGAGEMENT_BUTTONS", boundingBoxesDeep)) {
                            if (elementIn("PLUS_BUTTON", boundingBoxesDeep) && elementIn("SPONSORED_TEXT", boundingBoxesShallow)) {
                                tentativeAdTypesForFrame.add("REEL_BASED");
                            }
                        }

                        if (Objects.equals(thisInterpretation.get("orientation"), "landscape") && elementIn("SPONSORED_TEXT_HORIZONTAL", boundingBoxesShallow)) {
                            tentativeAdTypesForFrame.add("PREVIEW_LANDSCAPE_BASED");
                        }

                        if (elementIn("APP_STYLE_ELEMENT", boundingBoxesDeep)) {
                            tentativeAdTypesForFrame.add("APP_FEED_BASED");
                        }

                        if (elementIn("PRODUCT_IN_THIS_VIDEO_TEXT", boundingBoxesShallow)) {
                            tentativeAdTypesForFrame.add("PRODUCT_FEED_BASED");
                        }

                        if (elementIn("SPONSORED_TEXT", boundingBoxesShallow)) {
                            if ((getElementsIn("SPONSORED_TEXT", boundingBoxesShallow).stream().anyMatch(
                                    x -> getElementsIn("PREVIEW_ELEMENT", boundingBoxesDeep).stream().anyMatch(y -> (rectangularAreaOverlap(x, y) > 0.0)

                                    && (((Double) x.get("cy")) > (((Double) y.get("y1")) + (((Double) y.get("h")) * 0.25)))


                                    )))) {
                                tentativeAdTypesForFrame.add("GENERAL_FEED_BASED");
                            }
                        }

                        if (elementIn("SPONSORED_TEXT", boundingBoxesShallow)) {
                            tentativeAdTypesForFrame.add("PREVIEW_PORTRAIT_BASED");
                        }
                    }

                    //Guarding logic (against overloads)

                    if (tentativeAdTypesForFrame.contains("REEL_BASED")) {
                        tentativeAdTypesForFrame = Arrays.asList("REEL_BASED");
                    }

                    // Feed ads supercede all except reel ads, and block all except app feed or product in video ads
                    if (tentativeAdTypesForFrame.contains("GENERAL_FEED_BASED") || tentativeAdTypesForFrame.contains("PRODUCT_FEED_BASED") || tentativeAdTypesForFrame.contains("APP_FEED_BASED")) {
                        tentativeAdTypesForFrame = tentativeAdTypesForFrame.stream()
                                .filter(x -> !Arrays.asList("PREVIEW_PORTRAIT_BASED", "PREVIEW_LANDSCAPE_BASED").contains(x)).collect(Collectors.toList());
                    }

                    if (tentativeAdTypesForFrame.contains("PREVIEW_LANDSCAPE_BASED")) {
                        tentativeAdTypesForFrame = Arrays.asList("PREVIEW_LANDSCAPE_BASED");
                    }

                    if (tentativeAdTypesForFrame.contains("PREVIEW_PORTRAIT_BASED")) {
                        tentativeAdTypesForFrame = Arrays.asList("PREVIEW_PORTRAIT_BASED");
                    }

                    adTypesForGroup.set(adFrame, tentativeAdTypesForFrame);
                }));




                JSONXObject adFrameGroupsAdsFrames = deriveAds(List.of("PRODUCT_IN_THIS_VIDEO_TEXT", "SPONSORED_TEXT", "SPONSORED_TEXT_HORIZONTAL"),
                        tentativeAdTypes, groupsOfAdFrames, inferencesByFrames, inferencesDeepByFrames,
                        (tentativeAdType -> boundingBoxesDeep -> boundingBoxSponsored -> sponsoredTextCenterX -> sponsoredTextCenterY -> {

                    // PRODUCE CROPPING REGION
                    Double boundingBoxSponsoredY2 = (Double) boundingBoxSponsored.get("y2");
                    Double marginOnY = 0.01;
                    Double lowerMostY = null;
                    Double upperMostY = null;
                    Double lowerMostX = 0.0;
                    Double upperMostX = 1.0;

                    if (Objects.equals(tentativeAdType, "REEL_BASED")) {
                        // Remove the top 10% from the view, and go down to the plus button
                        upperMostY = 0.1;
                        Double plusButtonYStart = (Double) getElementsIn("PLUS_BUTTON", boundingBoxesDeep).get(0).get("y1");
                        Double sponsoredTextYStart = (Double) sponsoredTextCenterY + 0.05;
                        // Take the minimum between both values, as the 'Sponsored' text
                        // may be located before the 'Plus' button
                        lowerMostY = Math.min(plusButtonYStart, sponsoredTextYStart);
                    }

                    if (Objects.equals(tentativeAdType, "PREVIEW_PORTRAIT_BASED")) {
                        if ((((Double) boundingBoxSponsored.get("confidence")) > 0.25) && boundingBoxesDeep.isEmpty()) {
                            upperMostY = 0.05;
                            lowerMostY = 1.0;
                        }
                    }

                    if (Objects.equals(tentativeAdType, "PREVIEW_LANDSCAPE_BASED")) {
                        upperMostY = 0.0;
                        lowerMostY = 1.0;
                    }

                    if (Objects.equals(tentativeAdType,"APP_FEED_BASED")) {
                        JSONXObject thisElement = strongestOverlapCandidate(boundingBoxSponsored, getElementsIn("APP_STYLE_ELEMENT", boundingBoxesDeep));
                        if (thisElement != null) {
                            upperMostY = (Double) thisElement.get("y1");
                            lowerMostY = (Double) thisElement.get("y2");
                            lowerMostX = (Double) thisElement.get("x1");
                            upperMostX = (Double) thisElement.get("x2");
                        }
                    }

                    if (Objects.equals(tentativeAdType, "PRODUCT_FEED_BASED")) {
                        JSONXObject thisElement = strongestOverlapCandidate(boundingBoxSponsored, getElementsIn("PRODUCT_ELEMENT", boundingBoxesDeep));
                        if (thisElement != null) {
                            upperMostY = (Double) thisElement.get("y1");
                            lowerMostY = Math.max(boundingBoxSponsoredY2, (Double) thisElement.get("y2") + marginOnY );
                            lowerMostX = (Double) thisElement.get("x1");
                            upperMostX = (Double) thisElement.get("x2");

                        }
                    }

                    if (Objects.equals(tentativeAdType, "GENERAL_FEED_BASED")) {
                        // Find out how much each PREVIEW_FOOTER_ELLIPSIS overlaps the 'Sponsored' text
                        // and return the one with the highest match
                        JSONXObject containerElement = strongestOverlapCandidate(boundingBoxSponsored, getElementsIn("PREVIEW_ELEMENT", boundingBoxesDeep));
                        if (containerElement == null) {
                            containerElement = strongestOverlapCandidate(boundingBoxSponsored, getElementsIn("PREVIEW_FOOTER_ELLIPSIS", boundingBoxesDeep));
                        }
                        if (containerElement != null) {
                            upperMostY = (Double) containerElement.get("y1");
                            lowerMostY = Math.max(boundingBoxSponsoredY2, (Double) containerElement.get("y2") + marginOnY );
                            lowerMostX = (Double) containerElement.get("x1");
                            upperMostX = (Double) containerElement.get("x2");
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
                        rootDirectory,  screenRecordingAnalysisDirectory,  screenRecordingFile, "YOUTUBE");
            }
        }
    }
}
