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

import static java.util.Arrays.asList;

import android.content.Context;
import android.util.Log;

import com.adms.australianmobileadtoolkit.JSONXObject;
import com.adms.australianmobileadtoolkit.checkPoint;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
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

                    evaluationPostMethod( context, thisAdFrameGroupMetadatasUnseparated,  thisInterpretation,
                            thisComprehensiveReading,  implementedOnAndroid,  inferenceResultShallow,  inferenceResultDeep,
                            rootDirectory,  screenRecordingAnalysisDirectory,  screenRecordingFile, "TIKTOK");
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
    }



















}
