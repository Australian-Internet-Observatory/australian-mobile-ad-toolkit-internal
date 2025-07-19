package com.adms.australianmobileadtoolkit.interpreter.platform;

import static com.adms.australianmobileadtoolkit.interpreter.Platform.compositeBoundingBox;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.rectangularAreaOverlap;

import com.adms.australianmobileadtoolkit.JSONXObject;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Common {

    public static JSONXObject strongestOverlap(JSONXObject targetElement, List<JSONXObject> elements) {

        List<Double> overlaps = elements.stream().map(x ->
                rectangularAreaOverlap( ((JSONXObject) ((JSONXObject) targetElement.get("inference")).get("boundingBoxCropped")),
                (JSONXObject) ((JSONXObject) x.get("inference")).get("boundingBoxCropped"))).collect(Collectors.toList());
        if (!overlaps.isEmpty()) {
            Double maxOverlap = Collections.max(overlaps);
            if (maxOverlap > 0.0) {
                Integer maxOverlapIndex = overlaps.indexOf(maxOverlap);
                return elements.get(maxOverlapIndex);
            }
        }
        return null;
    }

    public static void consumerOverGroupedAdFrames(List<List<Integer>> groupsOfAdFrames, JSONXObject inferencesByFrames, JSONXObject inferencesDeepByFrames,
                                                   Function<String, Function<List<Integer>, Function<String, Function<List<JSONXObject>, Consumer<List<JSONXObject>>>>>> thisConsumer) {

        int i = 0;
        for (List<Integer> adFrameGroup : groupsOfAdFrames) {
            for (Integer adFrame : adFrameGroup) {
                List<JSONXObject> boundingBoxesShallow = ((List<JSONObject>) inferencesByFrames.get(adFrame)).stream().map(x -> (new JSONXObject(x, true))).collect(Collectors.toList());
                List<JSONXObject> boundingBoxesDeep = ((List<JSONObject>) inferencesDeepByFrames.get(adFrame)).stream().map(x -> (new JSONXObject(x, true))).collect(Collectors.toList());

                thisConsumer.apply(String.valueOf(i)).apply(adFrameGroup).apply(String.valueOf(adFrame)).apply(boundingBoxesShallow).accept(boundingBoxesDeep);
            }
            i ++;
        }
    }

    public static void prepareAdFrameCropping(List<JSONXObject> elements, JSONXObject tentativeCropping, String tentativeAdType, JSONXObject boundingBoxSponsored, List<JSONXObject> boundingBoxesDeep) {
        Double upperMostY = (Double) tentativeCropping.get("upperMostY");
        Double lowerMostY = (Double) tentativeCropping.get("lowerMostY");
        Double upperMostX = (Double) tentativeCropping.get("upperMostX");
        Double lowerMostX = (Double) tentativeCropping.get("lowerMostX");

        // For all three currently retrieved ad types, the upperMost and lowerMost Y crop the ad using the
        // same method
        if ((lowerMostY != null) && (upperMostY != null)) {
            JSONXObject thisAdFrameData = new JSONXObject();
            // Correct y axis issues
            Double tempUpperMostY = upperMostY;
            upperMostY = Math.min(upperMostY, lowerMostY);
            lowerMostY = Math.max(tempUpperMostY, lowerMostY);

            JSONXObject boundingBoxCropped = compositeBoundingBox(upperMostY, lowerMostY, upperMostX, lowerMostX);
            thisAdFrameData.set("adType", tentativeAdType);
            thisAdFrameData.set("inference", (new JSONXObject())
                    .set("boundingBoxCropped", boundingBoxCropped)
                    .set("boundingBoxSponsored", boundingBoxSponsored)
                    .set("boundingBoxes", boundingBoxesDeep));
            elements.add(thisAdFrameData);
        }
    }


    public static void organizeAds(List<JSONXObject> thisAdFrameGroupMetadatasUnseparated, JSONXObject adFrameGroupsAdsFrames) {
        // thirdly, we attempt to connect up the bounding boxes into individual ads
        for (String thisKey : adFrameGroupsAdsFrames.keys()) {
            JSONXObject thisGroup = (JSONXObject) adFrameGroupsAdsFrames.get(thisKey);
            List<JSONXObject> groupedAdFrames = new ArrayList<>();
            List<String> orderedFrames = thisGroup.keys().stream().map(Integer::parseInt).sorted().map(Object::toString).collect(Collectors.toList());
            for (String thisFrame : orderedFrames) {
                // For each element in this frame, if it overlaps any elements of the previous frames,
                for (JSONXObject thisElement : (List<JSONXObject>) thisGroup.get(thisFrame)) {

                    boolean foundOverlap = false;
                    // If not dealing with first frame
                    if (!Objects.equals(thisFrame, orderedFrames.get(0))) {
                        String previousFrame = orderedFrames.get(orderedFrames.indexOf(thisFrame)-1);
                        // Find all overlaps
                        List<Double> overlaps = groupedAdFrames.stream().map(x -> (x.has(previousFrame) &&
                                (((String) ((JSONXObject) x.get(previousFrame)).get("adType")).equals((String) ((JSONXObject) thisElement).get("adType")))) // must match in terms of ad type
                                ? rectangularAreaOverlap( ((JSONXObject) ((JSONXObject) thisElement.get("inference")).get("boundingBoxCropped")),
                                (JSONXObject) ((JSONXObject) ((JSONXObject) x.get(previousFrame)).get("inference")).get("boundingBoxCropped")
                        ) : 0.0).collect(Collectors.toList());
                        if (!overlaps.isEmpty()) {
                            Double maxOverlap = Collections.max(overlaps);
                            // Assert that a grouping can be linked if there is a strong overlap
                            if (maxOverlap > 0.0) {
                                Integer maxOverlapIndex = overlaps.indexOf(maxOverlap);
                                JSONXObject thisElements = groupedAdFrames.get(maxOverlapIndex);
                                thisElements.set(thisFrame, thisElement);
                                foundOverlap = true;
                            }
                        }
                    }

                    // If no overlap is found, create a new group
                    if (!foundOverlap) {
                        JSONXObject thisGroupedAdFrames = new JSONXObject().set(thisFrame, thisElement);
                        groupedAdFrames.add(thisGroupedAdFrames);
                    }
                }
            }
            thisAdFrameGroupMetadatasUnseparated.addAll(groupedAdFrames);
        }
    }

    public static JSONXObject deriveAds(List<String> sponsoredTypes, JSONXObject tentativeAdTypes, List<List<Integer>> groupsOfAdFrames, JSONXObject inferencesByFrames, JSONXObject inferencesDeepByFrames,
                                        Function<String, Function<List<JSONXObject>, Function<JSONXObject, Function<Double, Function<Double, JSONXObject>>>>> derivationRoutine) {
        JSONXObject adFrameGroupsAdsFrames = new JSONXObject();
        consumerOverGroupedAdFrames(groupsOfAdFrames, inferencesByFrames, inferencesDeepByFrames, (i -> adFrameGroup -> adFrame -> boundingBoxesShallow -> boundingBoxesDeep -> {
            if (!adFrameGroupsAdsFrames.has(i)) { adFrameGroupsAdsFrames.set(i, new JSONXObject()); }
            JSONXObject thisAdsFrames = (JSONXObject) adFrameGroupsAdsFrames.get(i);

                // We assert that there is only ever one bounding box for a Sponsored text on any ad, and it is the one with the highest confidence
                List<JSONXObject> boundingBoxesSponsored = boundingBoxesShallow.stream().filter(x -> sponsoredTypes.contains(x.get("className"))).collect(Collectors.toList());

                List<JSONXObject> thisAdsElements = new ArrayList<>();
                for (String tentativeAdType : (List<String>) ((JSONXObject) (tentativeAdTypes.get(i))).get(adFrame)) {

                    for (JSONXObject boundingBoxSponsored : boundingBoxesSponsored) {

                        Double sponsoredTextCenterX = (Double) boundingBoxSponsored.get("cx");
                        Double sponsoredTextCenterY = (Double) boundingBoxSponsored.get("cy");

                        JSONXObject tentativeCropping = derivationRoutine.apply(tentativeAdType).apply(boundingBoxesDeep).apply(boundingBoxSponsored).apply(sponsoredTextCenterX).apply(sponsoredTextCenterY);
                        prepareAdFrameCropping(thisAdsElements, tentativeCropping, tentativeAdType, boundingBoxSponsored, boundingBoxesDeep);
                    }
                }
                thisAdsFrames.set(adFrame, thisAdsElements);
        }));
        return adFrameGroupsAdsFrames;
    }
}
