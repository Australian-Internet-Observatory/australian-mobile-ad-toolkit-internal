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

            // If the shallow inference result yields a null response, it is most likely due to a malformed image
            // that was originally obtained from the reading of the screen-recording file - otherwise, it might be
            // because the actual screen-recording is corrupt.




            //



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


                // There are six kinds of ads that we have observed in Facebook
                //  * Reel Ads Within Navigation
                //  * Reel Ads - Fullscreen
                //  * Reel Footer Ads
                //  * Marketplace Ads
                //  * Feed Ads
                //  * Story Ads
                // All six can be reliably retrieved - logic for capture proceeds as follows:
                //
                //      is VIDEO_BUTTONS present?
                //              Y -> dealing with a reel-type ad - is a a CROSS_AND_ELLIPSIS present?
                //      Y -> dealing with a reel footer ad
                //      N -> dealing with either a fullscreen reel ad, or a reel ad within navigation (they are practically the same)
                //      N -> is there a MARKETPLACE_POST present?
                //      Y -> dealing with marketplace ad
                //      N -> (is the POST_HEADER in the top quarter of the page) AND (is there no indication of a POST_HEADER or POST_FOOTER in the other three quarters?)
                //      Y -> dealing with a story ad
                //      N -> dealing with a feed ad

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

                            if (!boundingBoxesDeep.isEmpty()) { // TODO - only proceed for this frame if there are 'deep' bounding boxes - note that if there are no bounding boxes, we can't determine where the contents of the ad is

                                if (boundingBoxesDeep.stream().anyMatch(x -> (x.get("className").equals("VIDEO_BUTTONS")))) {
                                    if (boundingBoxesDeep.stream().anyMatch(x -> (x.get("className").equals("CROSS_AND_ELLIPSIS")))) {
                                        tentativeAdType = "REEL_FOOTER_BASED";
                                    } else {
                                        tentativeAdType = "REEL_BASED";
                                    }
                                } else if (boundingBoxesDeep.stream().anyMatch(x -> (x.get("className").equals("MARKETPLACE_ELEMENT")))) {
                                    tentativeAdType = "MARKETPLACE_BASED";
                                } else if ((boundingBoxesDeep.stream().anyMatch(x -> (x.get("className").equals("POST_HEADER")   && (((Double) x.get("cy")) <= 0.25)   )))
                                        && (boundingBoxesDeep.stream().noneMatch(x -> ((Arrays.asList("POST_HEADER", "POST_FOOTER").contains((String) x.get("className"))) && (((Double) x.get("cy")) > 0.25)  )))) {
                                    tentativeAdType = "STORY_BASED";
                                } else {
                                    tentativeAdType = "FEED_BASED";
                                }

                                // TODO - while MARKETPLACE_BASED ads can come in doubles - we'll only regard the first of the two for the time being


                                // We assert that there is only ever one bounding box for a Sponsored text on any ad, and it is the one with the highest confidence
                                Double maxConfidenceSponsoredTextCandidate = Collections.max(boundingBoxesShallow.stream().map(x-> (Double) x.get("confidence")).collect(Collectors.toList()).stream().filter(y->y != null).collect(Collectors.toList()));
                                JSONXObject boundingBoxSponsored = boundingBoxesShallow.stream().filter(x ->
                                        x.get("className").equals("SPONSORED_TEXT") && (Objects.equals(x.get("confidence"), maxConfidenceSponsoredTextCandidate))).collect(Collectors.toList()).get(0); // TODO - cannot simply refer to one
                                Double sponsoredTextCenterX = (Double) boundingBoxSponsored.get("cx");
                                Double sponsoredTextCenterY = (Double) boundingBoxSponsored.get("cy");

                                thisAdFrameData.set("adType", tentativeAdType);

                                // PRODUCE CROPPING REGION
                                Double lowerMostY = null;
                                Double upperMostY = null;
                                Double lowerMostX = 0.0;
                                Double upperMostX = 1.0;
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
                                        // Disregard
                                    }
                                } else
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
                                } else
                                if (Objects.equals(tentativeAdType, "MARKETPLACE_BASED")) {
                                    try {
                                        // Occassionally, a 'Sponsored by sellers' section may be confused as a 'Sponsored' ad, and so we have to disregard it
                                        // TODO - this can cause data loss from sections that contain both 'Sponsored by sellers' content, and actual ads - adjust the logic
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
                                } else
                                if (Objects.equals(tentativeAdType, "STORY_BASED")) {
                                    // Cut directly from the post header and go downwards
                                    JSONXObject postHeaderElement = boundingBoxesDeep.stream()
                                            .filter(x -> (x.get("className").equals("POST_HEADER") )).collect(Collectors.toList()).get(0);
                                    lowerMostX = (Double) postHeaderElement.get("x1");
                                    upperMostX = (Double) postHeaderElement.get("x2");
                                    upperMostY = (Double) postHeaderElement.get("y1");
                                    lowerMostY = 1.0;
                                } else
                                if (Objects.equals(tentativeAdType, "FEED_BASED")) {
                                    try {
                                        JSONXObject postHeaderElement = boundingBoxesDeep.stream()
                                                .filter(x -> (x.get("className").equals("POST_HEADER")) && (rectangularAreaOverlap(x, boundingBoxSponsored) > 0.0))
                                                .collect(Collectors.toList()).get(0);
                                        upperMostY = (Double) postHeaderElement.get("y1");
                                        // Find the post header (or footer) (or end of page), and declare it as the lower bound of the ad
                                        lowerMostY = Math.min(Collections.min(boundingBoxesDeep.stream()
                                                .filter(x -> ((x.get("className").equals("POST_HEADER")) || (x.get("className").equals("POST_FOOTER")))
                                                        && (rectangularAreaOverlap(x, boundingBoxSponsored) <= 0.0)
                                                        && (((Double) x.get("cy")) > sponsoredTextCenterY)).map(y -> (Double) y.get("y1")).collect(Collectors.toList())), 1.0);
                                    } catch (Exception ignored) {
                                        // The post header may not exist
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
                                }
                            }
                        }
                        thisAdFrameGroupMetadatasUnseparated.add(thisAdFrameGroupMetadata);
                    }

                    evaluationPostMethod( context, thisAdFrameGroupMetadatasUnseparated,  thisInterpretation,
                             thisComprehensiveReading,  implementedOnAndroid,  inferenceResultShallow,  inferenceResultDeep,
                             rootDirectory,  screenRecordingAnalysisDirectory,  screenRecordingFile, "FACEBOOK");


                //}
            }
        }
    }



















}
