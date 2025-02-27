package com.adms.australianmobileadtoolkit.interpreter.platform;

import static com.adms.australianmobileadtoolkit.Arguments.A;
import static com.adms.australianmobileadtoolkit.Arguments.Args;
import static com.adms.australianmobileadtoolkit.Common.binAsAverages;
import static com.adms.australianmobileadtoolkit.Common.exceptionWrite;
import static com.adms.australianmobileadtoolkit.Common.optionalGetDouble;
import static com.adms.australianmobileadtoolkit.appSettings.DEBUG;
import static com.adms.australianmobileadtoolkit.appSettings.prescribedMinVideoWidth;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.averageColours;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.boundsOnStride;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.createDirectory;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.deleteRecursive;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.discreteIntervalsToRanges;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.forceToRange;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.generateSamplePositions;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.getStandardDeviation;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.getStandardDeviationD;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.getStandardDeviationDouble;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.logger;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.orderRange;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.overlayBitmaps;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.printJSON;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.randomInRange;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.rangesOverlap;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.saveBitmap;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.subtractRanges;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.whitespacePixelFromImage;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.writeToJSON;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.colourPalette;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.colourQuantizeBitmap;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.colourToHex;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.imageToPictogram;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.imageToStencil;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.pixelDifferencePercentage;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.stencilSimilarity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.util.Pair;

import com.adms.australianmobileadtoolkit.serialXObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.adms.australianmobileadtoolkit.Arguments;
import com.adms.australianmobileadtoolkit.JSONXObject;
import com.adms.australianmobileadtoolkit.R;
import com.adms.australianmobileadtoolkit.interpreter.visual.Stencil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Facebook {
    private static String TAG = "Facebook";
    private static String fileFormat = "jpg";

    public static Integer reservedAbsoluteWhitespaceColourLight = Color.rgb(252, 254, 253);
    public static Integer reservedAbsoluteWhitespaceColourDark = Color.rgb(35, 35, 35);

    /*
    *
    * Quick Readings
    *
    * */



    public static JSONObject facebookSampleImage(Bitmap thisBitmap) {
        Random r = new Random();

        int burnLoops = 3;

        Integer nSamplesX = 6;
        Integer nSamplesY = 12;
        Integer xLowerBound = 0;
        Integer xUpperBound = thisBitmap.getWidth();
        Integer yLowerBound = 0;
        Integer yUpperBound = thisBitmap.getHeight();

        Pair<Integer, List<Integer>> xSample = generateSamplePositions(nSamplesX, xLowerBound, xUpperBound);
        Pair<Integer, List<Integer>> ySample = generateSamplePositions(nSamplesY, yLowerBound, yUpperBound);
        HashMap<Integer, Double> pctConsistentGlobal = new HashMap<>();
        HashMap<Integer, String> maxColourGlobal = new HashMap<>();

        for (Integer stridePositionY : ySample.second) {
            Pair<Integer, Integer> boundsY = boundsOnStride(ySample.first, stridePositionY, yLowerBound, yUpperBound-1);

            // Take the burn samples
            List<Integer> burnSamples = new ArrayList<>();
            for (Integer thisBurnIteration : IntStream.range(0,burnLoops).toArray()) {
                // do burn loops to get an idea of dominant colours
                for (Integer stridePositionX : xSample.second) {
                    Pair<Integer, Integer> boundsX = boundsOnStride(xSample.first, stridePositionX, xLowerBound, xUpperBound-1);
                    // generate a random coordinate
                    Integer randX = randomInRange(r, boundsX);
                    Integer randY = randomInRange(r, boundsY);
                    burnSamples.add(thisBitmap.getPixel(randX, randY));
                }
            }
            //System.out.println(stridePositionY);
            // From the burn sample, get the colour that is most frequented


            HashMap<String, Integer> thisColourPalette = colourPalette(Args(
                    Arguments.A("threshold", 0.025),
                    Arguments.A("sample", burnSamples.stream().mapToInt(i->i).toArray())));
            //printJSON(thisColourPalette);

            String thisMaxColourHex = Collections.max(thisColourPalette.entrySet(), Map.Entry.comparingByValue()).getKey();
            Integer thisMaxColour = Color.parseColor(thisMaxColourHex);
            //System.out.println(thisMaxColourHex);

            int maxTries = 6;
            int nFound = 0;

            // Then do iterative sampling over all strides within the x axis, to determine if the colour is consistent
            for (Integer stridePositionX : xSample.second) {
                Pair<Integer, Integer> boundsX = boundsOnStride(xSample.first, stridePositionX, xLowerBound, xUpperBound-1);
                // generate a random coordinate
                int tries = 0;
                Boolean found = false;
                while ((tries < maxTries) && (!found)) {
                    Integer randX = randomInRange(r, boundsX);
                    Integer randY = randomInRange(r, boundsY);
                    if (pixelDifferencePercentage(thisBitmap.getPixel(randX, randY), thisMaxColour) < 0.025) {
                        found = true;
                    }
                    tries ++;
                }
                if (found) {
                    nFound ++;
                }
            }
            Double pctConsistent = (nFound / (double) nSamplesX);
            //System.out.println(pctConsistent);

            pctConsistentGlobal.put(stridePositionY, pctConsistent);
            maxColourGlobal.put(stridePositionY, thisMaxColourHex);


        }
        JSONObject output = new JSONObject();
        try {
            output.put("maxColourGlobal", maxColourGlobal);
            output.put("pctConsistentGlobal", pctConsistentGlobal);
            output.put("strideY", ySample.first);
        } catch (Exception e) {
            logger.error(e); }
        return output;
    }




    /*
     *
     * Note: it does not matter if the sample is not large enough - so long as there are a strong number
     * of comparisons
     *
     * TODO - identify whitespace and maximize sampling on it
     *
     * */
    public static JSONObject facebookFrameSignature(Bitmap thisBitmap, Integer wsColor) {

        Integer referenceColour = wsColor;

        JSONObject statistics = new JSONObject();
        Integer w = thisBitmap.getWidth();
        Integer h = thisBitmap.getHeight();
        Integer sampleOnX = 5;
        Integer sampleOnY = 30; // total sampled pixels will be 100
        Integer strideX = (int) Math.round(w/(double) (sampleOnX+2)); // adding on 2 for padding
        Integer strideY = (int) Math.round(h/(double) (sampleOnY+2)); // adding on 2 for padding

        HashMap<Integer, HashMap<Integer, Integer>> thisFrameSignature = new HashMap<>();
        HashMap<Integer, Boolean> thisFrameSignatureWS = new HashMap<>();

        // the loops assume (and work around) padding - simply because pixels on the edge of hte image are
        // more susceptible to contamination (by bordering or crappy frame-on-frame rendering by mmpeg)
        for (int yy = strideY; yy < h; yy += strideY) {
            HashMap<Integer, Integer> frameSignaturePart = new HashMap<>();
            List<Integer> colours = new ArrayList<>();
            for (int xx = strideX; xx < w; xx += strideX) {
                Integer thisPixel = thisBitmap.getPixel(xx, yy);
                frameSignaturePart.put(xx, thisPixel);
                colours.add(thisPixel);
            }
            thisFrameSignature.put(yy, frameSignaturePart);
            thisFrameSignatureWS.put(yy, pixelDifferencePercentage(referenceColour, averageColours(Args(A("colors", colours)))) < 0.25); // TODO adjusting from 0.05
        }


        try {
            statistics.put("frameSignature", thisFrameSignature);
            statistics.put("frameSignatureWS", thisFrameSignatureWS);
            statistics.put("strideX", strideX);
            statistics.put("strideY", strideY);
            statistics.put("h", h);
        } catch (Exception e) {

            logger.error(e);

        }
        return statistics;
    }











    // TODO - substantiate
    public static List<Integer> facebookFrameSignaturesCompareDetermineLocalMaxima(HashMap<Integer, Double> similaritiesAtOffsets, Integer interval) {
        List<Integer> localMaxima = new ArrayList<>();
        List<Integer> orderedKeySet = similaritiesAtOffsets.keySet().stream().sorted().collect(Collectors.toList());
        Double minClimb = 0.005;
        for (Integer thisOffset : orderedKeySet) {

            List<Double> leftSideVals = similaritiesAtOffsets.keySet().stream()
                    .filter(z -> z < thisOffset && z > (thisOffset - interval))
                    .map(a -> similaritiesAtOffsets.get(a)).collect(Collectors.toList());
            Double leftSideMax = optionalGetDouble(leftSideVals.stream().mapToDouble(b -> b).max());
            Double leftSideMin = optionalGetDouble(leftSideVals.stream().mapToDouble(b -> b).min());

            List<Double> rightSideVals = similaritiesAtOffsets.keySet().stream()
                    .filter(z -> z > thisOffset && z < (thisOffset + interval))
                    .map(a -> similaritiesAtOffsets.get(a)).collect(Collectors.toList());
            Double rightSideMax = optionalGetDouble(rightSideVals.stream().mapToDouble(b -> b).max());
            Double rightSideMin = optionalGetDouble(rightSideVals.stream().mapToDouble(b -> b).min());

            if ((similaritiesAtOffsets.get(thisOffset) > leftSideMax)
                    && (similaritiesAtOffsets.get(thisOffset) > rightSideMax)
                    && (Math.abs(similaritiesAtOffsets.get(thisOffset) - leftSideMin) > minClimb)
                    && (Math.abs(similaritiesAtOffsets.get(thisOffset) - rightSideMin) > minClimb)) {
                localMaxima.add(thisOffset);
            }
        }
        return localMaxima;
    }






    /*
     *
     * do first pass acc=.4, second pass acc=0.6 etc to minimize the number of comparison
     *
     * areas to go over again are dependent on deviation of results
     *
     *
     * */
    public static JSONObject facebookframeSignaturesCompare(JSONObject FSAJ, Bitmap frameB, Integer wsColor) {
        JSONObject statistics = new JSONObject();
        HashMap<Integer, HashMap<Integer, Integer>> FSANF = null;
        HashMap<Integer, HashMap<Integer, Integer>> FSBNF = null;
        HashMap<Integer, Boolean> frameSignatureWS = null;
        Integer strideX = 0;
        Integer strideY = 0;
        Integer h = 0;
        Boolean breakOnTooSimilar = false;
        try {
            FSANF = (HashMap<Integer, HashMap<Integer, Integer>>) FSAJ.get("frameSignature");
            frameSignatureWS = (HashMap<Integer, Boolean>) FSAJ.get("frameSignatureWS");
            strideX = (Integer) FSAJ.get("strideX");
            strideY = (Integer) FSAJ.get("strideY");
            h = (Integer) FSAJ.get("h");
        } catch (Exception e) {
            logger.error(e);
        }
        final HashMap<Integer, HashMap<Integer, Integer>> FSA = FSANF;

        // Create an inhibited section (to accomodate parts that dont move (like the navbar))
        Double inhibitedRangeSimilarityThreshold = 0.99;
        List<Integer> inhibitedRange = FSA.keySet().stream().filter(y -> {
            Double averageDifference = optionalGetDouble(FSA.get(y).keySet().stream().map(x ->
                    pixelDifferencePercentage(FSA.get(y).get(x), frameB.getPixel(x, y)) ).mapToDouble(z -> z).average());
            Double averageSimilarity = (1.0 - averageDifference);
            return (averageSimilarity > inhibitedRangeSimilarityThreshold);
        }).collect(Collectors.toList());

        // if half the image is identical, they're techniaclly teh same
        if (inhibitedRange.size() > (FSA.keySet().size() * 0.5)) {
            breakOnTooSimilar = true;
        }

        Integer halfHeight = (int) Math.floor(h * 0.4);

        // This figure determines the precision of the offset (if it can be found)
        Integer minimumComparisonStrideY = (int) Math.floor(h*0.005);
        Integer comparisonStrideY = (int) Math.floor(h*0.05);
        Integer sampleModulusInteger = 8;

        Integer nComparisons = 0;

        Boolean foundReliableOffset = false;
        Double desiredSimilarity = 0.90;
        Boolean breakOnImmediateDifference = false;
        Boolean breakOnRangeAbsence = false;
        Boolean breakOnNoMatch = false;
        Double determinedOffsetAccuracy = null;
        Double determinedOffsetSTDev = null;
        Integer determinedOffset = null;
        Integer absoluteBoundaryMin = (0 - halfHeight);
        Integer absoluteBoundaryMax = (0 + halfHeight);
        List<Pair<Integer, Integer>> rangesToScan = new ArrayList<>();
        rangesToScan.add(new Pair(absoluteBoundaryMin, absoluteBoundaryMax));
        while ((comparisonStrideY >= minimumComparisonStrideY) && (!foundReliableOffset)
                && (!breakOnRangeAbsence) && (!breakOnNoMatch) && (!breakOnImmediateDifference) && (!breakOnTooSimilar)) {
            HashMap<Integer, Double> similaritiesAtOffsets = new HashMap<>();
            HashMap<Integer, Double> stdevsAtOffsets = new HashMap<>();
            // For all of the current ranges
            for (Pair<Integer, Integer> thisRange : rangesToScan) {
                // Go over the range at intervals of the comparisonStrideY
                for (int i = thisRange.first; i < thisRange.second; i += comparisonStrideY) {
                    List<Double> pixelSimilarities = new ArrayList<>();
                    // determine the similarity at this offset
                    Integer m = 0;
                    for (Integer yy : FSA.keySet()) {
                        for (Integer xx : FSA.get(yy).keySet()) {
                            Integer yyAtOffset = (yy + i);
                            if ((yyAtOffset > 0) && (yyAtOffset < h)) {
                                //pixelSimilarities.add(1.0 - pixelDifferencePercentage(FSA.get(yy).get(xx), frameB.getPixel(xx, yyAtOffset)));
                                // TODO - ptimize
                                m ++;
                                if (m % sampleModulusInteger == 0) {
                                    if (frameSignatureWS.get(yy)) {
                                        nComparisons++;
                                        if (pixelDifferencePercentage(FSA.get(yy).get(xx), wsColor) <= 0.25) { // TODO up from 0.05
                                            pixelSimilarities.add(1.0 - pixelDifferencePercentage(FSA.get(yy).get(xx), frameB.getPixel(xx, yyAtOffset)));
                                            pixelSimilarities.add(1.0 - pixelDifferencePercentage(FSA.get(yy).get(xx), frameB.getPixel(xx, yyAtOffset)));
                                            pixelSimilarities.add(1.0 - pixelDifferencePercentage(FSA.get(yy).get(xx), frameB.getPixel(xx, yyAtOffset)));
                                        } else {
                                            pixelSimilarities.add(1.0 - pixelDifferencePercentage(FSA.get(yy).get(xx), frameB.getPixel(xx, yyAtOffset)));
                                            pixelSimilarities.add(1.0 - pixelDifferencePercentage(FSA.get(yy).get(xx), frameB.getPixel(xx, yyAtOffset)));
                                            pixelSimilarities.add(1.0 - pixelDifferencePercentage(FSA.get(yy).get(xx), frameB.getPixel(xx, yyAtOffset)));
                                            pixelSimilarities.add(1.0 - pixelDifferencePercentage(FSA.get(yy).get(xx), frameB.getPixel(xx, yyAtOffset)));
                                        }
                                    } else {
                                        pixelSimilarities.add(1.0 - pixelDifferencePercentage(FSA.get(yy).get(xx), frameB.getPixel(xx, yyAtOffset)));
                                        pixelSimilarities.add(1.0 - pixelDifferencePercentage(FSA.get(yy).get(xx), frameB.getPixel(xx, yyAtOffset)));
                                    }
                                }

                            }
                        }
                    }
                    Double thisAverageSimilarityAtOffset = optionalGetDouble(pixelSimilarities.stream().mapToDouble(x->x).average());
                    //System.out.println("offset: " + i);
                    //System.out.println("similarity: "+ thisAverageSimilarityAtOffset + "\n");
                    similaritiesAtOffsets.put(i, thisAverageSimilarityAtOffset);
                    stdevsAtOffsets.put(i, getStandardDeviationDouble(pixelSimilarities));
                }
            }

            if (optionalGetDouble(similaritiesAtOffsets.values().stream().mapToDouble(z -> z).max()) < 0.5) {
                breakOnImmediateDifference = true;
                break;
            }

            // Determine the local maxima
            // This is necessary to determine which ranges we should go over again
            List<Integer> lmIntervalOffsets = similaritiesAtOffsets.keySet().stream().sorted().collect(Collectors.toList());
            Integer lmInterval = (Math.abs(lmIntervalOffsets.get(0) - lmIntervalOffsets.get(1))*4);
            List<Integer> localMaxima = facebookFrameSignaturesCompareDetermineLocalMaxima(similaritiesAtOffsets, lmInterval);

            // Convert the local maxima into ranges
            rangesToScan = new ArrayList<>();
            for (Integer thisLocalMaxima : localMaxima) {
                List<Integer> tentativeRange = Arrays.asList(thisLocalMaxima - comparisonStrideY, thisLocalMaxima + comparisonStrideY);
                // The range needs to be adapted to the boundaries of the image
                tentativeRange.set(0, ((tentativeRange.get(0) < absoluteBoundaryMin) ? absoluteBoundaryMin : tentativeRange.get(0)));
                tentativeRange.set(1, ((tentativeRange.get(1) > absoluteBoundaryMax) ? absoluteBoundaryMax : tentativeRange.get(1)));
                // The range might be null after this transformation - check
                if (!Objects.equals(tentativeRange.get(0), tentativeRange.get(1))) {
                    // proceed if it isn't null
                    // lastly, determine that the tentative range does not overlap any existing
                    // range within the ranges to scan
                    List<Pair<Integer, Integer>> adaptedRangesToScan = new ArrayList<>();
                    Boolean overlapFound = false;
                    for (Pair<Integer, Integer> thisRange : rangesToScan) {
                        // If they do overlap, combine them
                        if (rangesOverlap(tentativeRange.get(0), tentativeRange.get(1), thisRange.first, thisRange.second) != 0) {
                            Pair<Integer, Integer> thisNewRange = new Pair(Math.min(tentativeRange.get(0), thisRange.first), Math.max(tentativeRange.get(1), thisRange.second));
                            adaptedRangesToScan.add(thisNewRange);
                            overlapFound = true;
                        } else {
                            adaptedRangesToScan.add(thisRange);
                        }
                    }
                    if (!overlapFound) {
                        Pair<Integer, Integer> thisNewRange = new Pair(tentativeRange.get(0), tentativeRange.get(1));
                        adaptedRangesToScan.add(thisNewRange);
                    }
                    rangesToScan = adaptedRangesToScan;
                } else {
                    // if teh ranges are equal, we dont need to apply them
                }
            }



            if (rangesToScan.isEmpty()) {
                // TODO - have to carry fowrard best match if it exists
                breakOnRangeAbsence = true;
                break;
            } else {





                comparisonStrideY = (int) Math.floor(comparisonStrideY * 0.5);
                sampleModulusInteger = Math.max((int) Math.floor(sampleModulusInteger * 0.5), 1);

                if (comparisonStrideY <= minimumComparisonStrideY) {
                    List<Integer> flaggedOffsets = similaritiesAtOffsets.keySet().stream()
                            .filter(z ->  (similaritiesAtOffsets.get(z) > desiredSimilarity)).collect(Collectors.toList());



                    if (flaggedOffsets.isEmpty()) {
                        breakOnNoMatch = true;
                        break;
                    } else {
                        foundReliableOffset = true;
                        determinedOffsetAccuracy = optionalGetDouble(similaritiesAtOffsets.values().stream().mapToDouble(x -> x).max()); // TODO
                        determinedOffset = Collections.max(similaritiesAtOffsets.entrySet(), Map.Entry.comparingByValue()).getKey(); // TODO
                        determinedOffsetSTDev = stdevsAtOffsets.get(determinedOffset);
                    }
                }
            }
        }

        try {
            String outcome = "COMPARABLE";
            if (breakOnImmediateDifference || breakOnNoMatch || breakOnRangeAbsence) {
                outcome = "TOO_DIFFERENT";
            }
            if (breakOnTooSimilar) {
                outcome = "TOO_SIMILAR";
            }
            if (foundReliableOffset) {
                outcome = "COMPARABLE";
            }
            statistics.put("outcome", outcome);
            statistics.put("maxSimilarity", determinedOffsetAccuracy);
            statistics.put("standardDeviation", determinedOffsetSTDev);
            statistics.put("nComparisons", nComparisons);
            statistics.put("offsetWithMaxSimilarity", determinedOffset);
        } catch (Exception e) {

            logger.error(e);
        }

        return statistics;
    }


    // TODO - facebooklite has a period withiun frames where loaded content doesnt display and ws ratio is thrown off

    public static JSONObject facebookGenerateQuickReadingOnFrame(Bitmap thisBitmap) {
        Integer absoluteWhitespaceColourLight = Color.rgb(252, 254, 253);
        Integer absoluteWhitespaceColourDark = Color.rgb(35, 35, 35);

        JSONObject sampleFromImage = facebookSampleImage(thisBitmap);
        //printJSON(sampleFromImage);

        // filtering out nonconsistent rows in both stages reduces contamination of readings

        HashMap<Integer, String> maxColourGlobal = new HashMap<>();
        HashMap<Integer, Double> pctConsistentGlobal = new HashMap<>();
        Integer strideY = null;
        try {
            maxColourGlobal = (HashMap<Integer, String>) sampleFromImage.get("maxColourGlobal");
            pctConsistentGlobal = (HashMap<Integer, Double>) sampleFromImage.get("pctConsistentGlobal");
            strideY = (Integer) sampleFromImage.get("strideY");
        } catch (Exception e) {
            logger.error(e);
        };

        HashMap<Integer, Double> finalPctConsistentGlobal = pctConsistentGlobal;
        HashMap<Integer, Integer> regardedMaxColourGlobal = new HashMap<>();
        Double consistencyThreshold = 0.9;
        for (Integer x : maxColourGlobal.keySet()) {
            if (finalPctConsistentGlobal.get(x) > consistencyThreshold) {
                regardedMaxColourGlobal.put(x, Color.parseColor(maxColourGlobal.get(x)));
            }
        }
        int[] thisCP = regardedMaxColourGlobal.values().stream().mapToInt(y -> y).toArray();

        // generate colour palette for the entire image
        HashMap<String, Integer> thisColourPalette = colourPalette(Args(
                Arguments.A("threshold", 0.025),
                Arguments.A("sample", thisCP)));

        //printJSON(thisColourPalette);

        // treat each colour as a group, and tierate over all rows to derive groups
        HashMap<String, List<Integer>> groupedIndicesByColourHex = new HashMap<>();
        for (String thisColourHex : thisColourPalette.keySet()) {
            int thisColor = Color.parseColor(thisColourHex);
            List<Integer> colours = new ArrayList<>();
            for (Integer x : regardedMaxColourGlobal.keySet()) {
                if (pixelDifferencePercentage(thisColor, regardedMaxColourGlobal.get(x)) < 0.05) { // Setting up from 0.025
                    colours.add(x);
                }
            }
            groupedIndicesByColourHex.put(thisColourHex, colours);
        }

        //printJSON(groupedIndicesByColourHex);

        // for each group, pass the sliding windows over the indices in order to determine if they are
        // well spaced
        Double lowerBoundWS = (1 / (6 + 0.5))*thisBitmap.getHeight();
        Double upperBoundWS = (1 / (4 - 0.5))*thisBitmap.getHeight();

        List<JSONObject> possibleWhitespaces = new ArrayList<>();
        List<JSONObject> allCandidates = new ArrayList<>();

        // a coisnsntet region m must be of a particular threshold in order to be regarded as whitespace
        Double whitespaceSimilarityThreshold = 0.5;

        for (String thisColourHex : groupedIndicesByColourHex.keySet()) {
            Integer thisColour = Color.parseColor(thisColourHex);
            Double similarityLight = (1 - pixelDifferencePercentage(thisColour, absoluteWhitespaceColourLight));
            Double similarityDark = (1 - pixelDifferencePercentage(thisColour, absoluteWhitespaceColourDark));

            if ((similarityLight > whitespaceSimilarityThreshold) || (similarityDark > whitespaceSimilarityThreshold)) {

                List<Integer> thisValues = groupedIndicesByColourHex.get(thisColourHex);

                // at each stride
                int nStrides = (int) Math.floor(thisBitmap.getHeight() / strideY);
                for (Integer thisStrideYn : IntStream.range(0, nStrides + 1).toArray()) {
                    int thisStrideY = thisStrideYn * strideY;
                    int halfUpperBoundWS = (int) Math.floor(upperBoundWS / 2.0);
                    List<Integer> filteredIndices = thisValues.stream().filter(x -> (x > (thisStrideY - halfUpperBoundWS))
                            && (x < (thisStrideY + halfUpperBoundWS))).collect(Collectors.toList());
                    int nFilteredIndices = filteredIndices.size();
                    int nTotalIndices = thisValues.size();
                    double pctWithinFilter = (nFilteredIndices / (double) nTotalIndices);
                    int extent = (int) Math.floor(Math.abs(
                            optionalGetDouble(filteredIndices.stream().mapToDouble(x->x).max()) -
                                    optionalGetDouble(filteredIndices.stream().mapToDouble(x->x).min())));
                    double pctOfWhole = (nTotalIndices / (double) 12); // TODO - fix hardcode
                    double maxPctOfWhole = 0.6; // on larger devices 0.5 - smaller 0.6
                    JSONObject thisCandidate = new JSONObject();
                    if (extent > 0) {
                        try {
                            thisCandidate.put("nFilteredIndices", nFilteredIndices);
                            thisCandidate.put("nTotalIndices", nTotalIndices);
                            thisCandidate.put("pctOfWhole", pctOfWhole);
                            thisCandidate.put("pctWithinFilter",pctWithinFilter);
                            thisCandidate.put("thisColourHex", thisColourHex);
                            thisCandidate.put("thisColour", thisColour);
                            thisCandidate.put("exposureType", ((similarityLight > similarityDark) ? "light" : "dark"));
                            thisCandidate.put("wsSimilarity", ((similarityLight > similarityDark) ? similarityLight : similarityDark));
                            thisCandidate.put("denotedStrideY", thisStrideY);
                            thisCandidate.put("extent", extent);
                            thisCandidate.put("lowerBoundWS",lowerBoundWS);
                            thisCandidate.put("upperBoundWS",upperBoundWS);
                        } catch (Exception e) {
                            logger.error(e);}
                        allCandidates.add(thisCandidate);
                    }
                    if (((extent > lowerBoundWS) && (extent < upperBoundWS)) && (pctOfWhole <= maxPctOfWhole)) {
                        //System.out.println("Potential ws of colour "+thisColourHex+" at stride of "+thisStrideY+" with extent length of "+extent);
                        possibleWhitespaces.add(thisCandidate);
                    }
                }
            }
        }

        // Then go over the possible matches, and choose the one that is closest to an absolute whitespace colour
        // note: it is possible that there might be a positive match for both dark and light whitespace - log both
        Boolean ofFacebook = false;
        Integer derivedWSColour = null;
        String derivedExposure = null;
        JSONObject highestRanked = new JSONObject();
        for (String exposureType : Arrays.asList("light", "dark")) {
            try { highestRanked.put(exposureType, null); } catch (Exception e) {
                logger.error(e);}
            for (JSONObject thisCandidate : possibleWhitespaces) {
                String candidateExposureType = null;
                Double candidateSimilarity = null;
                Integer candidateColour = null;
                try {
                    candidateExposureType = (String) thisCandidate.get("exposureType");
                    candidateSimilarity = (double) thisCandidate.get("wsSimilarity");
                    candidateColour = (Integer) thisCandidate.get("thisColour");
                } catch (Exception e) {
                    logger.error(e);}
                if (candidateExposureType.equals(exposureType)) {
                    JSONObject highestRankedForExposureType = null;
                    Double highestRankedForExposureTypeSimilarity = null;
                    try {
                        highestRankedForExposureType = (JSONObject) highestRanked.get(exposureType);
                        highestRankedForExposureTypeSimilarity = (double) highestRankedForExposureType.get("wsSimilarity");
                    } catch (Exception e) {
                        logger.info(e);}
                    if ((highestRankedForExposureType == null)
                            || ((highestRankedForExposureType != null) && (candidateSimilarity > highestRankedForExposureTypeSimilarity))) {
                        try {
                            highestRanked.put(exposureType, thisCandidate);
                            ofFacebook = true;
                            derivedExposure = exposureType;
                            derivedWSColour = candidateColour;
                        } catch (Exception e) {
                            logger.error(e);}
                    }
                }
            }
        }
        //printJSON(highestRanked);
        JSONObject output = new JSONObject();
        try {
            output.put("of", ofFacebook);
            output.put("derivedExposure", derivedExposure);
            output.put("derivedWSColour", derivedWSColour);
            output.put("highestRanked", highestRanked);
            output.put("allCandidates", allCandidates);
        } catch (Exception e) {
            logger.error(e); }
        return output;
    }

    /*
     *
     *
     * This function takes an initial 'quick-reading' of the screen recording and then determines
     * if it is within Facebook or not.
     *
     * We do this by taking a set of well-spaced frames across the full duration of the video.
     * 'Well-spaced' is here defined as being equally the same amount of seconds apart. We note the
     * very real possibility that this may not capture everything within the file.
     *
     * When an MP4 video is compiled, frames over how ever many seconds are compressed together if
     * they are identical. This means that 20 minutes of content, or 2 minutes of content can have
     * the same file size. As we split our video's by file size, this affects our implementation,
     * especially considering that we retrieve our 'quick-reading' by means of sampling frames within
     * the given video file.
     *
     * Determining how to sample the video is relatively easy - take N frames at equal duration apart
     * within the video. If at least half of the frames are determined to be of Facebook content, we can proceed.
     * However, once we identify a video containing Facebook content, we should go further and actually isolate
     * the start and end of said content, within the entire video itself. This can be done through deeper processing.
     *
     * NOTE: In order to prioritize speed over accuracy, the 'quick reading' can only determine if there is a strong
     * possibility of the frames being within Facebook. It shouldn't be used as a point of reference for determining
     * where (within a video) Facebook content resides. Originally, it was conceived that this would occur within
     * 'quick reading' function, however its now more sensible to leave the advanced functionality (requiring more
     * accuracy to the next stages), especially given that the function does everything it is required to do.
     *
     * TODO IMMEDIATELY:
     *
     * * This function is adapted to run within the Android Studio test environment - a separate version of
     * the function needs to be created that can run specifically on Android devices.
     *
     * */
    public static JSONObject facebookGenerateQuickReadingEvaluate(HashMap<Integer, JSONObject> readings) {
        // Assess for dark and light exposures
        String dominantExposure = null;
        Double dominantExposurePercentage = null;
        Integer dominantColourDark = null;
        Integer dominantColourLight = null;
        HashMap<String, Double> confidences = new HashMap<>();
        for (String exposureType : Arrays.asList("light", "dark")) {
            int nValidReadings = 0;
            for (Integer thisFrame: readings.keySet()) {
                JSONObject thisReading = readings.get(thisFrame);
                Boolean ofFacebook = false;
                Boolean ofExposure = false;
                try {
                    ofFacebook = (Boolean) thisReading.get("of");
                    ofExposure = ((JSONObject)
                            thisReading.get("highestRanked")).has(exposureType);
                } catch (Exception e) {
                    logger.error(e);
                }
                if (ofFacebook && ofExposure) {
                    try {
                        if (exposureType.equals("dark")) {
                            dominantColourDark = (Integer) thisReading.get("derivedWSColour");
                        } else {
                            dominantColourLight = (Integer) thisReading.get("derivedWSColour");
                        }
                    } catch (Exception e) {

                        logger.error(e);
                    }
                    nValidReadings ++;
                }
            }
            Double thisReadingConfidence = (nValidReadings / (double) readings.size());
            confidences.put(exposureType, thisReadingConfidence);

            if ((dominantExposurePercentage == null) || (thisReadingConfidence > dominantExposurePercentage)) {
                dominantExposurePercentage = thisReadingConfidence;
                dominantExposure = exposureType;
            }
        }
        JSONObject output = new JSONObject();
        try {
            output.put("confidences", confidences);
            output.put("readings", readings);
            output.put("denotedMode", dominantExposure);
            output.put("denotedColour", (dominantExposure.equals("dark") ? dominantColourDark : dominantColourLight));
            output.put("of", dominantExposurePercentage >= 0.5);
        } catch (Exception e) {

            logger.error(e);

        }

        return output;
    }

    public static JSONObject facebookGenerateQuickReading(Context context, Boolean DEBUG, File debugDirectory, File screenRecordingFile,
                                                          Function<JSONXObject, JSONXObject> functionGetVideoMetadata, Function<JSONXObject, Bitmap> frameGrabFunction) {

        File readingsFolder = new File(debugDirectory, "sample");
        if (DEBUG) {
            createDirectory(readingsFolder, true);
        }

        HashMap<Integer, JSONObject> readings = new HashMap<>();

        JSONXObject thisVideoMetadata = functionGetVideoMetadata.apply(new JSONXObject().set("context", context).set("screenRecordingFile", screenRecordingFile));
        int nScreenshotsToGrab = 7;
        Integer nFrames = null;
        Integer videoDuration = null;
        try {
            nFrames = (Integer) thisVideoMetadata.get("METADATA_KEY_VIDEO_FRAME_COUNT");
            videoDuration = (Integer) thisVideoMetadata.get("METADATA_KEY_DURATION");
        } catch (Exception e) {
            logger.error(e);
        };
        Integer interval = (int) Math.floor(nFrames/ (double) nScreenshotsToGrab);
        if ((nFrames == 0) || (interval == 0)) {
            return facebookGenerateQuickReadingEvaluate(readings);
        }
        for (int f = 0; f < nFrames; f += interval) {
            //Log.i(TAG, String.valueOf(f));
            try {
                JSONXObject bitmapStat = new JSONXObject()
                        .set("context", (Context) context)
                        .set("thisScreenRecordingFile", screenRecordingFile)
                        .set("f", f)
                        .set("videoFrames", nFrames)
                        .set("videoDuration", videoDuration)
                        .set("minWidth", prescribedMinVideoWidth);
                Bitmap thisBitmap = frameGrabFunction.apply(bitmapStat);
                if (DEBUG) {
                    saveBitmap(thisBitmap, new File(readingsFolder, "frame-" + f + ".jpg").getAbsolutePath());
                }
                if (thisBitmap != null) {
                    readings.put(f, facebookGenerateQuickReadingOnFrame(thisBitmap));
                } else {
                    logger.info("Reached 'null' Bitmap at frame "+f+"/"+nFrames);
                }
            } catch (Exception e) {
                logger.error(e);
                readings.put(f, null);
            }

        }
        return facebookGenerateQuickReadingEvaluate(readings);
    }












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

    /*
     *
     * for any two frames, we firstly determine how much they are similar - if they are practically
     * identical, we can replace the second frame with the one thereafter it and so on.
     *
     * then if the frames differ, we need to determine in what way - if we were to naively cross-check all parts
     * of the images, there might be instances where we not only sample enough frames to have them connected together,
     * but also capture instances where the scroller does not move, yet there is movie content playing.
     *
     * to overcome this, we are only going to be interested in parts of the frames where the whitespace sits.
     * then we can determine how that whitespace moves.
     *
     * a very simple way of doing this is sampling the image for bands of whitespace
     *
     * if we take one such band of whitespace and check to see where it moves, we can get an idea about
     * how smooth the transition is between the frames
     *
     * selecting said band of whitespace is difficult - for devices that have nav bars, there is always
     * a whitespace band at the top of the page
     *
     * furthermore, when a band of whitespace disappears, it can't be referenced any longer
     *
     * thus a robust approach is to consider all bands of whitesapce on the page at once.
     * when a band of whitespace nears the edges of the y axis, it should be discounted from any comparisons
     * to avoid misreading its absence as its disappearance
     *
     * when comparing two bands of whitespace, we should measure distance of their averages - if its greater than
     * 1/5th of hte page height, then we should step backwards
     *
     *   note that we should be creating a 'whitespace band' reference all the while we undergo this process
     *   the reference should be used to avoid double-sampling frames while iterating across
     *
     * set an upper bound also on accuracy, as sometimes it isn't possible to link the frames together
     *
     *
     *
     *
     * */
    public static JSONObject facebookComprehensiveReading(Context context,
                                                          File tempDirectory, File thisScreenRecordingFile, Function<JSONXObject, JSONXObject> videoMetadataFunction,
                                                          Function<JSONXObject, Bitmap> frameGrabFunction) {
        Double elapsedTimeFacebookComprehensiveFramesSample = Long.valueOf(System.currentTimeMillis()).doubleValue();
        JSONObject output = new JSONObject();
        JSONObject statistics = new JSONObject();
        Double intervalAsPercentageOfFrameRate = 0.5;
        Integer intervalMinimumComparable = 2;
        Double adjustingIntervalDivisionFactor = 2.0;
        List<HashMap<String, Integer>> scanLog = new ArrayList<>();
        List<List<Pair<Integer, Integer>>> rangesToScanLog = new ArrayList<>();
        Integer totalFramesSampled = 0;
        Integer totalComparisons = 0;
        Integer durationInMilliseconds = 0;
        Double frameRate = null;
        Integer w = null;
        Integer h = null;

        String exitStatus = "INCONCLUSIVE";
        Boolean proceedOnThis = true;




        //TODO MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        //TODO retriever.setDataSource(context, Uri.fromFile(thisScreenRecordingFile));
        JSONXObject videoMetadata = videoMetadataFunction.apply(new JSONXObject().set("context", context).set("screenRecordingFile", thisScreenRecordingFile));


        System.out.println(videoMetadata);

        JSONXObject outputOfQuickReading = new JSONXObject(facebookGenerateQuickReading(context, false, null, thisScreenRecordingFile, videoMetadataFunction, frameGrabFunction));
        String denotedMode = (String) outputOfQuickReading.get("denotedMode");
        Integer wsColor = (Integer) outputOfQuickReading.get("denotedColour");

        try {
            // Initialize the pseudo frame grabber
            frameRate = (Double) videoMetadata.get("METADATA_DERIVED_FRAMERATE");
            Log.i(TAG, "frameRate: "+frameRate);
            // Determine the length of the recording in frames
            Integer thisScreenRecordingVideoFrames = ((int) videoMetadata.get("METADATA_KEY_VIDEO_FRAME_COUNT")) - 1;
            durationInMilliseconds = ((int) videoMetadata.get("METADATA_KEY_DURATION")) - 1;

            Log.i(TAG, "thisScreenRecordingVideoFrames: "+thisScreenRecordingVideoFrames);
            // Derive a 'base-maximum' interval to jump between frames (as a percentage of the frame rate)
            Integer baseMaximumInterval = (int) Math.floor(frameRate * intervalAsPercentageOfFrameRate);

            /*
             *
             * In developing this approach, we anticipate that the code may run on devices with a strictly
             * small amount of memory. Thus it is wise for us to save the sampled frames as bitmaps, in a temporary
             * sample folder
             *
             * */
            // Before running a sample, the temp folder is cleared and recreated
            createDirectory(tempDirectory, true);

            Integer adjustingInterval = baseMaximumInterval;
            Boolean earlyExitFlag = false;




            // do the frame checking in waves - first wsave does bare minimum

            // second wave goes in, checks where there are bad cuts and fills in their half reaches

            // continue to do this until a precision is reached

            // then go in and cull the parts that are redundant

            Boolean scanFlag = true;
            List<JSONObject> frameComparisonStructure = new ArrayList<>();
            List<JSONObject> frameComparisonStructureDisqualified = new ArrayList<>();
            HashMap<Integer, JSONObject> signaturesMap = new HashMap<>();
            while (scanFlag) {



                List<Pair<Integer, Integer>> rangesToScan = new ArrayList<>();

                HashMap<String, Integer> thisScanLog = new HashMap<>();

                // If the frameComparisonStructure is empty, do a full loop through
                if (frameComparisonStructure.isEmpty()) {
                    rangesToScan.add(new Pair(0, thisScreenRecordingVideoFrames));
                } else {

                    // Go through the list of frameComparisons
                    //
                    // For those that are too similar, remove the second frame and attach the first to
                    // the consecutive comparison (logical approach, considering they are similar)
                    // Note: We are making an assumption that if two frames are too similar, that their
                    // adjacent comparisons are transitively attributed the same comparison results
                    //
                    // Note: This step assumes items are added to array-lists in order (verified)
                    List<JSONObject> updatedFrameComparisonStructure = new ArrayList<>();
                    Integer transitedSimilarFrame = null;
                    Integer n_TOO_SIMILAR = 0;
                    for (JSONObject thisFrameComparison : frameComparisonStructure) {
                        // If this comparison is preceded by one that was 'too similar', augment this
                        // comparison with the transitedSimilarFrame
                        if (transitedSimilarFrame != null) {
                            thisFrameComparison.put("lastFrame", transitedSimilarFrame);
                            transitedSimilarFrame = null;
                        }
                        String thisComparisonOutcome = (String) ((JSONObject) thisFrameComparison.get("comparisonResult")).get("outcome");
                        // If this frame comparison is too similar, do not add it, and transit the
                        // second frame of its comparison to the next comparison
                        // Note: If this is the last framecomparison, we might naively allow it to be retained - although there is no reason
                        if (Objects.equals(thisComparisonOutcome, "TOO_SIMILAR")) {
                            //    && (!thisFrameComparison.equals(frameComparisonStructure.get(frameComparisonStructure.size() - 1)))) {
                            transitedSimilarFrame = (Integer) thisFrameComparison.get("lastFrame");
                            frameComparisonStructureDisqualified.add(thisFrameComparison);
                            n_TOO_SIMILAR ++;
                        } else {
                            updatedFrameComparisonStructure.add(thisFrameComparison);
                        }
                    }
                    thisScanLog.put("n_TOO_SIMILAR", n_TOO_SIMILAR);
                    // Update the frameComparisonStructure for the next step
                    frameComparisonStructure = new ArrayList<>(updatedFrameComparisonStructure);
                    // Flush the updatedFrameComparisonStructure
                    updatedFrameComparisonStructure = new ArrayList<>();

                    // For those that are too different, if their interval is large enough, add
                    // their frame range - otherwise preserve their frame comparison
                    //
                    // NOTE: This step has to occur separate to the previous, as the frameComparisonStructure
                    // is already edited at this point
                    Integer n_TOO_DIFFERENT = 0;
                    Integer n_COMPARABLE = 0;
                    for (JSONObject thisFrameComparison : frameComparisonStructure) {
                        String thisComparisonOutcome = (String) ((JSONObject) thisFrameComparison.get("comparisonResult")).get("outcome");
                        if (Objects.equals(thisComparisonOutcome, "TOO_DIFFERENT")) {
                            // Is the interval large enough?
                            Integer lastFrame = (Integer) thisFrameComparison.get("lastFrame");
                            Integer currentFrame = (Integer) thisFrameComparison.get("currentFrame");

                            if (Math.abs(lastFrame - currentFrame) >= intervalMinimumComparable) {
                                // If the interval between the frames is large enough to warrant
                                // the creation of a range for scanning, do not add the comparison to
                                // the updatedFrameComparisonStructure, but add a new range to the
                                // rangesToScan
                                rangesToScan.add(new Pair(lastFrame, currentFrame));
                                frameComparisonStructureDisqualified.add(thisFrameComparison);
                            } else {
                                updatedFrameComparisonStructure.add(thisFrameComparison);
                            }
                            n_TOO_DIFFERENT ++;
                        } else {
                            updatedFrameComparisonStructure.add(thisFrameComparison);
                            n_COMPARABLE ++;
                        }
                    }
                    thisScanLog.put("n_COMPARABLE", n_COMPARABLE);
                    thisScanLog.put("n_TOO_DIFFERENT", n_TOO_DIFFERENT);
                    // Update the frameComparisonStructure for the next step
                    frameComparisonStructure = new ArrayList<>(updatedFrameComparisonStructure);

                    //writeToJSON((new File(tempDirectory, "frameComparisonStructurePost.json")), frameComparisonStructure);

                    // Collapse adjacent ranges if they exist
                    if (rangesToScan.size() > 1) {
                        Integer retainedRangeStart = null;
                        Integer retainedRangeEnd = null;
                        List<Pair<Integer, Integer>> adjustedRangesToScan = new ArrayList<>();
                        for (Pair<Integer, Integer> thisRange : rangesToScan) {
                            if (retainedRangeStart == null) {
                                retainedRangeStart = thisRange.first;
                                retainedRangeEnd = thisRange.second;
                            } else
                            if (retainedRangeEnd == thisRange.first) {
                                retainedRangeEnd = thisRange.second;
                            } else {
                                adjustedRangesToScan.add(new Pair(retainedRangeStart, retainedRangeEnd));
                                retainedRangeStart = thisRange.first;
                                retainedRangeEnd = thisRange.second;
                            }
                        }
                        if (retainedRangeStart != null) {
                            adjustedRangesToScan.add(new Pair(retainedRangeStart, retainedRangeEnd));
                        }
                        rangesToScan = new ArrayList<>(adjustedRangesToScan);
                    }
                    scanLog.add(thisScanLog);
                }

                rangesToScanLog.add(rangesToScan);

                for (Pair<Integer, Integer> thisRange : rangesToScan) {
                    earlyExitFlag = false;
                    Integer lastFrame = thisRange.first;
                    Integer currentFrame = thisRange.first;
                    Integer destinationFrame = thisRange.second;
                    Boolean compareFrames = true;
                    while (compareFrames) {
                        if (currentFrame >= destinationFrame) {
                            compareFrames = false;
                        }
                        // Retrieve the bitmap for the current frame
                        Bitmap currentFrameBitmap = null;
                        while (currentFrameBitmap == null) {
                            // Set the frame number
                            // Grab the frame
                            JSONXObject bitmapGrabStat = new JSONXObject();

                            bitmapGrabStat
                                    .set("context", context)
                                    .set("thisScreenRecordingFile", thisScreenRecordingFile)
                                    .set("f", currentFrame)
                                    .set("videoFrames", thisScreenRecordingVideoFrames)
                                    .set("videoDuration", durationInMilliseconds)
                                    .set("minWidth", prescribedMinVideoWidth);

                            currentFrameBitmap = frameGrabFunction.apply(bitmapGrabStat);

                            totalFramesSampled ++;
                            System.out.println(currentFrame);

                            // The bitmap may still be null at this step - step backwards one frame in said case
                            if (currentFrameBitmap == null) {
                                System.out.println("frame is null");
                                if (currentFrame == destinationFrame) {
                                    currentFrame --;
                                } else {
                                    earlyExitFlag = true;
                                    //scanFlag = false;
                                    //System.out.println("SCAN FLAG TRIGGERED FROM HERE");
                                    break;
                                }
                            } else {
                                if (h == null) {
                                    w = currentFrameBitmap.getWidth();
                                    h = currentFrameBitmap.getHeight();
                                }
                            }
                        }

                        // The indication of the early exit flag will be that the currentFrameBitmap is null -
                        // the latter will mean that we can proceed with a comparison at this frame.
                        if (!earlyExitFlag) {

                            if (!signaturesMap.containsKey(currentFrame)) {
                                signaturesMap.put(currentFrame,  facebookFrameSignature(currentFrameBitmap, wsColor));//frameSignature(currentFrameBitmap));
                                saveBitmap(currentFrameBitmap, (new File(tempDirectory, currentFrame + "." + fileFormat).getAbsolutePath()));
                            }

                            // Provided the lastFrame is instantiated
                            if (lastFrame != null) {
                                JSONObject signatureOfLastFrame = signaturesMap.get(lastFrame);
                                JSONObject signatureOfCurrentFrame = signaturesMap.get(currentFrame);
                                JSONObject comparisonResult = facebookframeSignaturesCompare(signatureOfLastFrame, currentFrameBitmap, wsColor);
                                totalComparisons ++;
                                JSONObject thisComparison = new JSONObject();
                                try {
                                    thisComparison.put("lastFrame", lastFrame);
                                    thisComparison.put("currentFrame", currentFrame);
                                    thisComparison.put("comparisonResult", comparisonResult);
                                } catch (Exception e) {

                                    logger.error(e);
                                }
                                frameComparisonStructure.add(thisComparison);
                            }

                            // At the end of the comparison, the currentFrameBitmap is applied to the lastFrameBitmap
                            // lastFrameBitmap = currentFrameBitmap;
                        }

                        // Update the position within the sample
                        lastFrame = currentFrame;
                        currentFrame += adjustingInterval;
                        System.out.println("adjustingInterval: " + adjustingInterval);
                        System.out.println("destinationFrame: " + destinationFrame);
                        if (currentFrame > destinationFrame) {
                            currentFrame = destinationFrame;
                        }
                    }
                }

                // Sort the results
                Collections.sort(frameComparisonStructure, new SortByLastFrame());

                adjustingInterval = (int) Math.floor(adjustingInterval / adjustingIntervalDivisionFactor);
                if (adjustingInterval < intervalMinimumComparable) {
                    scanFlag = false;
                }


            }

            frameComparisonStructure = frameComparisonStructure.stream().filter(x -> {
                try {
                    return (((JSONObject) x.get("comparisonResult")).get("outcome") != "TOO_SIMILAR");
                } catch (Exception e) {

                    logger.error(e);
                    return false;
                } } ).collect(Collectors.toList());

            // Delete all frames that aren't part of the frameComparisonStructure
            List<Integer> runningListOfFrames = new ArrayList<>();
            for (JSONObject thisComparison : frameComparisonStructure) {
                for (String alt : Arrays.asList("last", "current")) {
                    runningListOfFrames.add((Integer) thisComparison.get(alt + "Frame"));
                }
            }
            runningListOfFrames = runningListOfFrames.stream().distinct().collect(Collectors.toList());

            for (File child : Objects.requireNonNull(tempDirectory.listFiles())) {
                if (runningListOfFrames.indexOf(Integer.parseInt(child.getName().replaceAll("\\." + fileFormat, ""))) == -1) {
                    child.delete();
                }
            }


            //try { retriever.release(); } catch (Exception e) { }

            // TODO - may not be necessary
            // Create a condensed version of the data-structure for future processing
            List<JSONObject> offsetChain = new ArrayList<>();
            if (runningListOfFrames.size() >= 2) {
                for (int i = 0; i < (runningListOfFrames.size() - 1); i ++) {
                    Integer frameThis = runningListOfFrames.get(i);
                    Integer frameNext = runningListOfFrames.get(i + 1);
                    List<JSONObject> candidateComparisons = Stream.concat(
                            frameComparisonStructure.stream().filter(x -> {
                                try {
                                    return ((Objects.equals(x.get("lastFrame"), frameThis))
                                            && (Objects.equals(x.get("currentFrame"), frameNext)));
                                } catch (Exception e) {

                                    logger.error(e);
                                    return false;
                                }
                            }),
                            frameComparisonStructureDisqualified.stream().filter(x -> {
                                try {
                                    return ((Objects.equals(x.get("lastFrame"), frameThis))
                                            && (Objects.equals(x.get("currentFrame"), frameNext)));
                                } catch (Exception e) {

                                    logger.error(e);
                                    return false;
                                }
                            })).collect(Collectors.toList());
                    if (!candidateComparisons.isEmpty()) {
                        offsetChain.add(candidateComparisons.get(0));
                    } else {
                        // TODO
                    }
                }
            } else {
                // TODO
            }

            exitStatus = "ANALYSED "+totalComparisons.toString() + " FRAMES";

            // Capture the necessary statistics
            statistics.put("signaturesMap", signaturesMap);
            statistics.put("offsetChain", offsetChain);
            statistics.put("earlyExitFlag", earlyExitFlag);
            statistics.put("runningListOfFrames", runningListOfFrames);
            statistics.put("frameComparisonStructure", frameComparisonStructure);
            statistics.put("frameComparisonStructureDisqualified", frameComparisonStructureDisqualified);
            statistics.put("minWidth", prescribedMinVideoWidth);
            statistics.put("thisScreenRecordingFile", thisScreenRecordingFile.getAbsolutePath());
            statistics.put("intervalAsPercentageOfFrameRate", intervalAsPercentageOfFrameRate);
            statistics.put("intervalMinimumComparable", intervalMinimumComparable);
            statistics.put("adjustingIntervalDivisionFactor", adjustingIntervalDivisionFactor);
            statistics.put("scanLog", scanLog);
            //statistics.put("rangesToScanLog", rangesToScanLog);
            statistics.put("totalComparisons", totalComparisons);
            statistics.put("frameRate", frameRate);
            statistics.put("w", w);
            statistics.put("h", h);

            JSONObject statisticsSummary = new JSONObject();
            statisticsSummary.put("elapsedTimeFacebookComprehensiveFramesSample", Math.abs(elapsedTimeFacebookComprehensiveFramesSample - System.currentTimeMillis()));
            statisticsSummary.put("nScreenRecordingVideoFrames", thisScreenRecordingVideoFrames);
            statisticsSummary.put("nScreenRecordingVideoFramesRetained", runningListOfFrames.size());
            statisticsSummary.put("nScreenRecordingVideoFramesSampled", totalFramesSampled);
            statisticsSummary.put("nComparisons", totalComparisons);
            statisticsSummary.put("nScreenRecordingVideoFrameRate", frameRate);
            statisticsSummary.put("nScreenRecordingVideoSeconds", (thisScreenRecordingVideoFrames / frameRate));

            output.put("statistics", statistics);
            output.put("statisticsSummary", statisticsSummary);
            output.put("denotedMode", denotedMode);
            output.put("wsColor", wsColor);
            // frameComparisonStructure // TODO - preserve matched offsets

        } catch (Exception e) {
            exitStatus = "READING_FAILURE: "+exceptionWrite(e);
            proceedOnThis = false;
            logger.error(e);
            try {
                output.put("error", e.getMessage());
            } catch (JSONException e2) {

                logger.error(e2);
            }
        }
        try {
            output.put("exitStatus", exitStatus);
            output.put("proceedOnThis", proceedOnThis);
        } catch (Exception e2) {}
        return output;
    }




    /*
    *
    * Facebook Ad Header Specific Logic
    *
    * */


    public static Integer red = Color.rgb(255, 0, 0);
    public static Integer blue = Color.rgb(0, 0, 255);
    public static Integer yellow = Color.rgb(255, 255, 0);
    public static Integer white = Color.rgb(255, 255, 255);

    public static JSONObject fitterGenerate(Bitmap stencil) {
        Integer marginAmount = 5;
        Integer modValue = 4;



        Double precision = 0.025;
        Integer w = stencil.getWidth();
        Integer h = stencil.getHeight();
        Integer strideX = (int) Math.floor(precision * w);
        Integer strideY = (int) Math.floor(precision * h);
        HashMap<String, Integer> extremes = new HashMap<>();

        for (int x = 0; x < w; x += strideX) {
            for (int y = 0; y < h; y += strideY) {
                Integer thisPixel = stencil.getPixel(x, y);
                if (thisPixel.equals(blue)) {
                    if ((!extremes.containsKey("minY")) || (y < extremes.get("minY"))) {
                        extremes.put("minY", y);
                    }
                    if ((!extremes.containsKey("maxY")) || (y > extremes.get("maxY"))) {
                        extremes.put("maxY", y);
                    }
                    if ((!extremes.containsKey("minX")) || (x < extremes.get("minX"))) {
                        extremes.put("minX", x);
                    }
                    if ((!extremes.containsKey("maxX")) || (x > extremes.get("maxX"))) {
                        extremes.put("maxX", x);
                    }
                }
            }
        }
        HashMap<String, Integer> margins = new HashMap<>();
        for (String thisKey : extremes.keySet()) {
            margins.put(thisKey, extremes.get(thisKey) + (marginAmount * (thisKey.contains("max") ? +1 : -1)));
        }
        Integer xx = 0;
        Integer yy = 0;
        List<List<Integer>> negatives = new ArrayList<>();
        List<List<Integer>> positives = new ArrayList<>();
        Integer stencilW = margins.get("maxX") - margins.get("minX");
        Integer stencilH = margins.get("maxY") - margins.get("minY");
        for (int x = margins.get("minX"); x < margins.get("maxX"); x += strideX) {
            if (xx % modValue == 0) {
                for (int y = margins.get("minY"); y < margins.get("maxY"); y += strideY) {
                    Integer thisPixel = stencil.getPixel(x,y);
                    List<Integer> appliedCoord = Arrays.asList(x - margins.get("minX"), y - margins.get("minY"));
                    if (yy % modValue == 0) {
                        if (thisPixel.equals(white)) {
                            negatives.add(appliedCoord);
                        } else if (thisPixel.equals(blue) || thisPixel.equals(red)) {
                            positives.add(appliedCoord);
                        }
                    }
                    yy += 1;
                }
            }
            xx += 1;
        }
        HashMap<String, Integer> interest = new HashMap<>();
        Integer interestStride = 2;
        for (int x = margins.get("minX"); x < margins.get("maxX"); x += interestStride) {
            for (int y = margins.get("minY"); y < margins.get("maxY"); y += interestStride) {
                Integer thisPixel = stencil.getPixel(x,y);
                if (thisPixel.equals(yellow)) {
                    if ((!interest.containsKey("l")) || (x < interest.get("l"))) {
                        interest.put("l", x);
                    }
                    if ((!interest.containsKey("r")) || (x > interest.get("r"))) {
                        interest.put("r", x);
                    }
                    if ((!interest.containsKey("t")) || (y < interest.get("t"))) {
                        interest.put("t", y);
                    }
                    if ((!interest.containsKey("b")) || (y > interest.get("b"))) {
                        interest.put("b", y);
                    }
                }
            }
        }
        Integer nPoints = (negatives.size() * positives.size());

        JSONObject output = new JSONObject();
        try {
            output.put("positives", positives);
            output.put("interest", interest);
            output.put("negatives", negatives);
            output.put("nPoints", nPoints);
            output.put("w", stencilW);
            output.put("h", stencilH);
        } catch (Exception e) {
            logger.error(e);
        }

        return output;
    }

    // clones fitter
    public static JSONObject fitterDispatch(JSONObject thisFitter) {
        try {
            JSONObject output = new JSONObject();
            output.put("positives", new ArrayList<List<Integer>> ((List<List<Integer>>) thisFitter.get("positives")));
            output.put("interest", new HashMap<String, Integer> ( (HashMap<String, Integer>) thisFitter.get("interest")));
            output.put("negatives", new ArrayList<List<Integer>> ((List<List<Integer>>) thisFitter.get("negatives")));
            output.put("nPoints", thisFitter.get("nPoints"));
            output.put("w", thisFitter.get("w"));
            output.put("h", thisFitter.get("h"));
            return output;
        } catch (Exception e) {
            logger.error(e);
            return null;
        }
    }

    public static JSONObject fitterFitOnImage(JSONObject thisFitter, Bitmap thisImage, Integer thisWSColor, File thisPictogramFittingDirectory, Boolean verbose, String testName) {
        Integer minIntervalInPixels = 5;
        List<String> alts = Arrays.asList("positives", "negatives");
        HashMap<String, Integer> interest = new HashMap<>();
        Integer imgW = thisImage.getWidth();
        Integer imgH = thisImage.getHeight();

        // Failsafe for images that have bad ratios (and would leave to over-processing or under-processing)
        Double minAllowableRatio = 0.1 / 1.0; // The height should not be less than 1/10th of the width
        Double maxAllowableRatio = 0.4 / 1.0; // The height should not be more than 4/10ths of the width
        Double thisRatio = (imgH / (double) imgW);
        // TODO - the stepper should be contextualised to different heights
        if ((thisRatio < minAllowableRatio) || (thisRatio > maxAllowableRatio)) {
            System.out.println(thisImage.getWidth());
            System.out.println(thisImage.getHeight());
            return null;
        }

        Integer ftrW = null;
        Integer ftrH = null;
        HashMap<String, List<List<Integer>>> thisFitterAlt = new HashMap<>();
        try {
            ftrW = (Integer) thisFitter.get("w");
            ftrH = (Integer) thisFitter.get("h");
            interest = new HashMap<String, Integer>((HashMap<String, Integer>) thisFitter.get("interest"));
            alts.stream().forEach(x -> { try { thisFitterAlt.put(x, new ArrayList<List<Integer>>((List<List<Integer>>) thisFitter.get(x))); } catch (Exception e) {
                logger.error(e);
            } });
        } catch (Exception e) {

            logger.error(e);

        }
        // The maximum height of the fitter is set to be equal to approx. 4/5th of the
        // comparing image's height
        Double ftrMaxHRatio = 0.8;
        Double ftrMaxH = imgH * ftrMaxHRatio;
        // The maximum width of the fitter is derived accordingly
        Double ftrMaxW = ftrMaxH * (ftrW / ftrH);
        // The minimum width of the fitter is set to be equal to approx. 1/6th of the
        // comparing image's width
        Double ftrMinWRatio = 0.15;
        Double ftrMinW = imgW * ftrMinWRatio;
        // The minimum height of the fitter is derived accordingly
        Double ftrMinH = ftrMinW * (ftrH / (double) ftrW);

        // Indication can be set between 0 and 1 (as we progress over the image)
        List<Integer> chosenCoord = null;
        List<Integer> chosenDims = null;
        Double chosenCoordThreshold = null;
        Integer nComparisons = 0;
        for (int indicationRaw = 0; indicationRaw <= 100; indicationRaw += 20) { // TODO - magic numbers
            Double indication = indicationRaw / 100.0;
            Integer ftrWApplied = (int) Math.floor(ftrMaxW - (indication * Math.abs(ftrMaxW - ftrMinW)));
            Integer ftrHApplied = (int) Math.floor(ftrMaxH - (indication * Math.abs(ftrMaxH - ftrMinH)));
            // Determine the scaling figure for the fitter at this indication (note we only
            // have to do it for one dimension, as its consistent for both)
            Double fitterMultiplicant = ftrWApplied / (double) ftrW;

            // n_steps to do over either axis when testing

            Double pctSurveyW = 0.25;
            Integer stepsX = 20;
            Integer traversalX = (int) Math.floor(Math.abs((imgW * pctSurveyW) - ftrWApplied));
            Integer intervalX = (int) Math.max(Math.floor(traversalX / (double) stepsX), minIntervalInPixels);
            List<Integer> rangeX = (intervalX.equals(0)) ? Collections.singletonList(0) : IntStream.range(0, traversalX).filter(x -> (x % intervalX == 0)).boxed().collect(Collectors.toList());

            Double pctSurveyH = 1.0;
            Integer stepsY = 10;
            Integer traversalY = (int) Math.floor(Math.abs((imgH * pctSurveyH) - ftrHApplied));
            Integer intervalY = (int) Math.max(Math.floor(traversalY / (double) stepsY), minIntervalInPixels);
            List<Integer> rangeY = (intervalY.equals(0)) ? Collections.singletonList(0) : IntStream.range(0, traversalY).filter(y -> (y % intervalY == 0)).boxed().collect(Collectors.toList());

            for (Integer xx : rangeX) {
                for (Integer yy : rangeY) {
                    HashMap<String, List<Integer>> scores = new HashMap<>();
                    alts.stream().forEach(x -> { scores.put(x, new ArrayList<>()); });
                    for (String alt : alts) {
                        for (List<Integer> z : thisFitterAlt.get(alt)) {
                            nComparisons ++;
                            Integer adjX = Math.min(xx + ((int) Math.floor(z.get(0) * fitterMultiplicant)), imgW - 1);
                            Integer adjY = Math.min(yy + ((int) Math.floor(z.get(1) * fitterMultiplicant)), imgH - 1);
                            Double scoreValue = pixelNonWSScore(thisImage.getPixel(adjX, adjY), thisWSColor, 0.1);//015); // TODO added
                            if (alt == "negatives") {
                                scoreValue = 1.0 - scoreValue;
                            }
                            // We want a high positive score, and low negative score
                            scores.get(alt).add((int) Math.round(scoreValue));
                        }
                    }
                    Double positiveMean = optionalGetDouble(scores.get("positives").stream().mapToDouble(x->x).average()) + getStandardDeviation(scores.get("positives")); // TODO added
                    Double negativeMean = optionalGetDouble(scores.get("negatives").stream().mapToDouble(x->x).average()) + getStandardDeviation(scores.get("negatives")); // TODO added
                    Double combinedPct = optionalGetDouble(Arrays.asList(positiveMean, negativeMean).stream().mapToDouble(x -> x).average());
                    if ((chosenCoordThreshold == null) || (combinedPct > chosenCoordThreshold)) {
                        chosenCoordThreshold = combinedPct;
                        chosenCoord = Arrays.asList(xx, yy);
                        chosenDims = Arrays.asList(ftrWApplied, ftrHApplied);
                    }
                }
            }
        }
        if (verbose) {
            Bitmap thisImageApplied = thisImage.copy(thisImage.getConfig(), true);
            for (int xx = chosenCoord.get(0); xx < chosenCoord.get(0) + chosenDims.get(0); xx += 2) {
                for (int yy = chosenCoord.get(1); yy < chosenCoord.get(1) + chosenDims.get(1); yy += 2) {
                    thisImageApplied.setPixel(xx, yy, red);
                }
            }
            saveBitmap(thisImageApplied, new File(thisPictogramFittingDirectory, testName+"-1-fitter-result." + fileFormat).getAbsolutePath());
        }

        Double adjustedRatio = chosenDims.get(0) / (double) ftrW;
        for (String k : interest.keySet()) {
            interest.put(k, (int) Math.floor(interest.get(k) * adjustedRatio));
        }

        Integer adjInterestW = (int) Math.abs(interest.get("l") - interest.get("r"));
        Integer adjInterestH = (int) Math.abs(interest.get("t") - interest.get("b"));

        JSONObject adaptiveBoundary = new JSONObject();
        try {
            adaptiveBoundary.put("x", chosenCoord.get(0) + interest.get("l"));
            adaptiveBoundary.put("y", chosenCoord.get(1) + interest.get("t"));
            adaptiveBoundary.put("w", adjInterestW);
            adaptiveBoundary.put("h", adjInterestH);
        } catch (Exception e) {

            logger.error(e);

        }
        if (verbose) {
            System.out.println(testName);
            printJSON(adaptiveBoundary);
        }

        return adaptiveBoundary;
    }

    public static Double pixelNonWSScore(Integer pixel, Integer thisWSColor, Double precision) {
        return (pixelDifferencePercentage(pixel, thisWSColor) > precision) ? 1.0 : 0.0;
    }

    // TODO - crosscheck number of actual ads in vidoes vs. what is captured by algorithm

    public static JSONObject finderGenerate(Bitmap thisImage, JSONObject adaptiveBoundary, Integer thisWSColor, File thisPictogramFittingDirectory, Boolean verbose, String testName) {
        Double maxProportionShift = 0.5;
        Integer aX = 0;
        Integer aY = 0;
        Integer aW = 0;
        Integer aH = 0;
        try {
            aX = (int) adaptiveBoundary.get("x");
            aY = (int) adaptiveBoundary.get("y");
            aW = (int) adaptiveBoundary.get("w");
            aH = (int) adaptiveBoundary.get("h");
        } catch (Exception e) {
            logger.error(e);
        }
        Boolean find = true;
        Double finderRatio = (thisImage.getWidth() / (double) prescribedMinVideoWidth);
        final Integer sampleMagnitude = (int) Math.round(3*finderRatio);
        Integer stride = (int) Math.round(2*finderRatio);
        Integer ii = 0;
        Integer nSamples = 0;
        List<Integer> areaStrip = new ArrayList<>();
        Integer loopFailSafe = 1000;
        Integer convergenceNSamples = 4;
        while (find) {

            /*if (verbose) {
                System.out.println("ii : "+ii);
                System.out.println("\taX : "+aX);
                System.out.println("\taY : "+aY);
                System.out.println("\taW : "+aW);
                System.out.println("\taH : "+aH);
            }*/

            if (aX < 0) { aX = 0; }
            if (aX > thisImage.getWidth()) { aX = thisImage.getWidth(); }
            if (aY < 0) { aY = 0; }
            if (aY > thisImage.getHeight()) { aY = thisImage.getHeight(); }
            if (aX+aW > thisImage.getWidth()) { aW = thisImage.getWidth()-aX; }
            if (aY+aH > thisImage.getHeight()) { aH = thisImage.getHeight()-aY; }

            ii += 1;
            HashMap<String, List<Integer>> samples = new HashMap<>();
            // Retrieve sample on left of finder
            List<Integer> samplesL = new ArrayList<>();
            for (int thisY = aY; thisY < aY+aH; thisY += 1) {
                for (int thisX = Math.max(0, aX-sampleMagnitude); thisX < aX; thisX += 1) {
                    samplesL.add(thisImage.getPixel(thisX, thisY));
                }
            }
            samples.put("l", samplesL);
            // Retrieve sample on right of finder
            List<Integer> samplesR = new ArrayList<>();
            for (int thisY = aY; thisY < aY+aH; thisY += 1) {
                for (int thisX = aX+aW; thisX < Math.min(aX+aW+sampleMagnitude, thisImage.getWidth()); thisX += 1) {
                    samplesR.add(thisImage.getPixel(thisX, thisY));
                }
            }
            samples.put("r", samplesR);
            // Retrieve sample on top of finder
            List<Integer> samplesT = new ArrayList<>();
            for (int thisY = Math.max(0, aY-sampleMagnitude); thisY < aY; thisY += 1) {
                for (int thisX = aX; thisX < aX+aW; thisX += 1) {
                    samplesT.add(thisImage.getPixel(thisX, thisY));
                }
            }
            samples.put("t", samplesT);
            // Retrieve sample on bottom of finder
            List<Integer> samplesB = new ArrayList<>();
            for (int thisY = aY+aH; thisY < Math.min(aY+aH+sampleMagnitude, thisImage.getHeight()); thisY += 1) {
                for (int thisX = aX; thisX < aX+aW; thisX += 1) {
                    samplesB.add(thisImage.getPixel(thisX, thisY));
                }
            }
            samples.put("b", samplesB);
            HashMap<String, Integer> samplesSummarized = new HashMap<>();
            for (String k : samples.keySet()) {
                Integer jj = 0;
                List<Integer> thisSampleDerivation = new ArrayList<>();
                for (Integer z : samples.get(k)) {
                    if (jj % 2 == 0) {
                        thisSampleDerivation.add((int) Math.round(pixelNonWSScore(z, thisWSColor, 0.05)));
                    }
                    jj ++;
                }
                nSamples += thisSampleDerivation.size();
                samplesSummarized.put(k, (int) Math.round(optionalGetDouble(thisSampleDerivation.stream().mapToDouble(x -> x).max())));
            }
            areaStrip.add(aW * aH);
            if (ii >= loopFailSafe) {
                if (verbose) {
                    printJSON(adaptiveBoundary);
                }
                break;
            }
            if ((areaStrip.size() >= convergenceNSamples)
                    && (((int) Math.round(getStandardDeviation(areaStrip.subList(areaStrip.size()-convergenceNSamples, areaStrip.size())))) == 0)) {
                find = false;
            } else {
                if ((samplesSummarized.get("l").equals(samplesSummarized.get("r"))) && (samplesSummarized.get("l") == 1)) {
                    aX -= stride;
                    aW += (stride * 2);
                } else {
                    if (samplesSummarized.get("l") == 1) {
                        aX -= stride;
                    }
                    if (samplesSummarized.get("r") == 1) {
                        aX += stride;
                    }
                }
                if ((samplesSummarized.get("t").equals(samplesSummarized.get("b"))) && (samplesSummarized.get("t") == 1)) {
                    aY -= stride;
                    aH += (stride * 2);
                } else {
                    if (samplesSummarized.get("t") == 1) {
                        aY -= stride;
                    }
                    if (samplesSummarized.get("b") == 1) {
                        aY += stride;
                    }
                }
            }
        }
        if (verbose) {
            Bitmap thisImageApplied = thisImage.copy(thisImage.getConfig(), true);
            for (int xx = aX; xx < aX+aW; xx += 2) {
                for (int yy = aY; yy < aY+aH; yy += 2) {
                    thisImageApplied.setPixel(xx, yy, blue);
                }
            }
            saveBitmap(thisImageApplied, new File(thisPictogramFittingDirectory, testName + "-2-finder-result." + fileFormat).getAbsolutePath());
        }
        JSONObject output = new JSONObject();
        try {
            output.put("x", aX);
            output.put("y", aY);
            output.put("w", aW);
            output.put("h", aH);
        } catch (Exception e) {

            logger.error(e);

        }
        if (find) {
            return null;
        }
        return output;
    }

    public static JSONObject isFacebookAdHeaderPassThrough(JSONObject output, Double elapsedTimeIsFacebookAdHeader) {
        try {
            output.put("elapsedTimeIsFacebookAdHeader", Math.abs(elapsedTimeIsFacebookAdHeader - System.currentTimeMillis()));
        } catch (Exception e) {

            logger.error(e);

        }
        return output;
    }

    public static JSONObject isFacebookAdHeader(Bitmap thisBitmap, JSONObject thisFitter, HashMap<String, Object> pictogramsReference,
                                                Integer determinedWSColor, String determinedExposureType) {
        Double elapsedTimeIsFacebookAdHeader = Long.valueOf(System.currentTimeMillis()).doubleValue();

        Integer sponsoredTextTypicalWidth = 143;
        Integer sponsoredTextTypicalWidthAlt = 154;
        Integer sponsoredTextTypicalHeight = 32;
        Integer minimumAdaptiveBoundaryHeight = 50;
        JSONObject output = new JSONObject();

        try {
            output.put("sponsoredTextTypicalWidth", sponsoredTextTypicalWidth);
            output.put("sponsoredTextTypicalWidthAlt", sponsoredTextTypicalWidthAlt);
            output.put("sponsoredTextTypicalHeight", sponsoredTextTypicalHeight);
            output.put("minimumAdaptiveBoundaryHeight", minimumAdaptiveBoundaryHeight);
            output.put("outcome", "MATCHED");
        } catch (Exception e) {

            logger.error(e);

        }

        if (thisBitmap.getHeight() < minimumAdaptiveBoundaryHeight) {
            try {
                output.put("outcome", "UNMATCHED_BELOW_MINIMUM_ADAPTIVE_BOUNDARY_HEIGHT");
            } catch (Exception e) {

                logger.error(e);

            }
            return isFacebookAdHeaderPassThrough(output, elapsedTimeIsFacebookAdHeader);
        }

        String exposureType = (determinedExposureType.equals("dark")) ? "Dark" : "Light";
        Integer thisWSColor = determinedWSColor;
        // Quantize the test imgae
        Double elapsedTimeColorQuantization = Long.valueOf(System.currentTimeMillis()).doubleValue();
        Bitmap thisAppliedBitmap = colourQuantizeBitmap(Args(A("bitmap", thisBitmap), A("interval", 4)));
        try { output.put("elapsedTimeColorQuantization", Math.abs(elapsedTimeColorQuantization - System.currentTimeMillis())); } catch (Exception e) {
            logger.error(e); }

        // Fit the fitter over the test image
        Double elapsedTimeFitOnImage = Long.valueOf(System.currentTimeMillis()).doubleValue();
        JSONObject adaptiveBoundary = fitterFitOnImage(thisFitter, thisAppliedBitmap, thisWSColor, null, false, null);
        try { output.put("elapsedTimeFitOnImage", Math.abs(elapsedTimeFitOnImage - System.currentTimeMillis())); } catch (Exception e) {
            logger.error(e);}

        // Find the boundary of the 'Sponsored' text
        if (adaptiveBoundary == null) {
            try {
                output.put("outcome", "UNMATCHED_ADAPTIVE_BOUNDARY_NOT_FOUND");
            } catch (Exception e) {
                logger.error(e); }
            return isFacebookAdHeaderPassThrough(output, elapsedTimeIsFacebookAdHeader);
        } else {
            try {
                output.put("adaptiveBoundary", adaptiveBoundary);
            } catch (Exception e) {
                logger.error(e); }
        }

        // Determine the sponsored text boundary
        Double elapsedTimeFinderGenerate = Long.valueOf(System.currentTimeMillis()).doubleValue();
        JSONObject sponsoredTextBoundary = finderGenerate(thisAppliedBitmap, adaptiveBoundary, thisWSColor, null, false, null);
        try { output.put("elapsedTimeFinderGenerate", Math.abs(elapsedTimeFinderGenerate - System.currentTimeMillis())); } catch (Exception e) {
            logger.error(e); }

        if (sponsoredTextBoundary == null) {
            try {
                output.put("outcome", "UNMATCHED_SPONSORED_TEXT_BOUNDARY_NOT_FOUND");
            } catch (Exception e) {
                logger.error(e); }
            return isFacebookAdHeaderPassThrough(output, elapsedTimeIsFacebookAdHeader);
        } else {
            try {
                output.put("sponsoredTextBoundary", sponsoredTextBoundary);
            } catch (Exception e) {
                logger.error(e);}
        }
        try {

            Double elapsedTimePictogramComparison = Long.valueOf(System.currentTimeMillis()).doubleValue();
            // Determine the area to test for the 'Sponsored' text
            Integer cropX = (Integer) sponsoredTextBoundary.get("x");
            Integer cropY = (Integer) sponsoredTextBoundary.get("y");
            Integer cropW = (Integer) sponsoredTextBoundary.get("w");
            Integer cropH = (Integer) sponsoredTextBoundary.get("h");
            Bitmap areaToTest = Bitmap.createBitmap(thisAppliedBitmap, cropX, cropY, cropW, cropH);

            // Determine the crop ratio against a predetermined 'desired' ratio for what would constitute
            // 'Sponsored' text
            Double cropRatio = cropW / (double) cropH;
            Double maxCropRatioDifference = 1.25;
            Double sponsoredTextRatio = (sponsoredTextTypicalWidth / (double) sponsoredTextTypicalHeight);
            Double sponsoredTextRatioAlt = (sponsoredTextTypicalWidthAlt / (double) sponsoredTextTypicalHeight);
            Double cropRatioDifference = Math.min(Math.abs(cropRatio - sponsoredTextRatio), Math.abs(cropRatio - sponsoredTextRatioAlt));

            // If the difference is too large, it cannot be matched
            if (cropRatioDifference > maxCropRatioDifference) {
                output.put("outcome", "UNMATCHED_CROP_RATIO_DIFFERENCE");
                output.put("maxCropRatioDifference", maxCropRatioDifference);
                output.put("cropRatioDifference", cropRatioDifference);
                return isFacebookAdHeaderPassThrough(output, elapsedTimeIsFacebookAdHeader);
            }

            // If at this stage, all tests have been passed, run the comparison of the test image against
            // the 'Sponsored' text pictogram

            // Generate the pictogram for the candidate
            Stencil potentialSponsoredTextPictogram = imageToStencil(Args(
                    A("bitmap", areaToTest),
                    A("whitespacePixel", thisWSColor),
                    A("size", new HashMap<String, Integer>() {{
                        put("w", 128);
                        put("h", 64);
                    }}),
                    A("snapThreshold", 0.1),
                    A("cropThreshold", 0.05),
                    A("colourPaletteThreshold", 0.05),
                    A("isReference", false)
            ));

            // Determine the match result // TODO - deal with the alt case as well
            Double matchResult = stencilSimilarity(Args(
                    A("a", pictogramsReference.get("facebook" + exposureType + "Sponsored")),
                    A("b", potentialSponsoredTextPictogram),
                    A("method", "multiplied"),
                    A("deepSampling", true))
            );

            Double desiredMatchResult = 0.5; // TODO - tune
            try {
                output.put("outcome", ((matchResult >= desiredMatchResult) ? "MATCHED" : "UNMATCHED"));
                output.put("matchResult", matchResult);
            } catch (Exception e) {
                logger.error(e);}
            try { output.put("elapsedTimePictogramComparison", Math.abs(elapsedTimePictogramComparison - System.currentTimeMillis())); } catch (Exception e) { }

            return isFacebookAdHeaderPassThrough(output, elapsedTimeIsFacebookAdHeader);
        } catch (Exception e) {
            logger.error(e);
            try {
                output.put("outcome", "UNMATCHED_SPONSORED_TEXT_ERROR");
            } catch (Exception ignored) { }
            return isFacebookAdHeaderPassThrough(output, elapsedTimeIsFacebookAdHeader);
        }
    }





    /*
     *
     * this function goes beyond basic identification of an inhibited range, by actually determine boundaries on a frame that should be omitted
     *
     * */
    public static List<List<Integer>> generateInhibitedRange(List<HashMap<Integer, HashMap<Integer, Integer>>> FSList, List<HashMap<Integer, Boolean>> FSWSList, Integer h, Integer FSInterval) {
        List<Integer> consistentList = null;
        List<List<Integer>> consistentListAsRanges = new ArrayList<>();
        Double inhibitedRangeSimilarityThreshold = 0.99;
        Double upperBoundCutOff = 0.2;
        Double lowerBoundCutOff = 0.1;
        if (FSList.size() > 1) {
            for (int i = 0; i < FSList.size() - 1; i ++) {
                HashMap<Integer, HashMap<Integer, Integer>> FSA = FSList.get(i);
                HashMap<Integer, HashMap<Integer, Integer>> FSB = FSList.get(i+1);


                HashMap<Integer, Boolean> FSAWS = FSWSList.get(i);
                HashMap<Integer, Boolean> FSBWS = FSWSList.get(i+1);

                List<Integer> thisInhibitedRange = FSA.keySet().stream().filter(y -> {
                    // needs to be in top or bottom of bounds
                    if ((y > upperBoundCutOff*h) && (y < (h-(lowerBoundCutOff*h)))) {
                        return false;
                    }
                    // needs to be whitespace
                    if ((!FSAWS.get(y)) || (!FSBWS.get(y))) {
                        return false;
                    }
                    Double averageDifference = optionalGetDouble(FSA.get(y).keySet().stream().map(x ->
                            pixelDifferencePercentage(FSA.get(y).get(x), FSB.get(y).get(x)) ).mapToDouble(z -> z).average());
                    Double averageSimilarity = (1.0 - averageDifference);
                    return (averageSimilarity > inhibitedRangeSimilarityThreshold);
                }).collect(Collectors.toList());
                if (consistentList == null) {
                    consistentList = thisInhibitedRange;
                } else {
                    List<Integer> finalConsistentList = consistentList;
                    consistentList = thisInhibitedRange.stream().filter(x -> finalConsistentList.contains(x)).collect(Collectors.toList());
                }
            }
            // With the inhibited range, we are interested in the part that is prominent - we can derive this by taking the
            // smallest index of either FS and interpreting it as an interval - then we find a range of at least two

            consistentListAsRanges = discreteIntervalsToRanges(FSInterval, consistentList);
        }
        return consistentListAsRanges;
    }


    // TODO - needs to return true dividers, calculated for each of the frames
    public static HashMap<Integer, List<Integer>> generateSuperOffsetChain(HashMap<Integer, Integer> thisOffsetChain, HashMap<Integer, List<Integer>> thisDividerMap) {

        System.out.println("thisOffsetChain: "+thisOffsetChain);
        HashMap<Integer, List<Integer>> superOC = new HashMap<>();
        List<Integer> frames = thisOffsetChain.keySet().stream().sorted().collect(Collectors.toList());
        List<Integer> framesBackward = new ArrayList<>(frames); Collections.reverse(framesBackward);
        Integer frameMin = Collections.min(frames);
        Integer frameMax = Collections.max(frames);

        for (Integer thisFrame : frames) {
            superOC.put(thisFrame, new ArrayList<>());
        }

        List<Integer> retainedDividers = new ArrayList<>();
        Integer previousFrame = null;

        for (Integer thisFrame : frames) {
            // Offset all existing dividers within the retainedDividers list
            if (!thisFrame.equals(frameMin)) {
                Integer finalPreviousFrame = previousFrame;
                retainedDividers = retainedDividers.stream().map(x -> x + thisOffsetChain.get(finalPreviousFrame)).collect(Collectors.toList());
            }
            previousFrame = thisFrame;
            // Append the dividers from this frame to the retainedDividers
            retainedDividers = Stream.concat(retainedDividers.stream(), thisDividerMap.get(thisFrame).stream()).collect(Collectors.toList());

            // Set the superOC as this field
            superOC.put(thisFrame, retainedDividers);
        }

        List<Integer> retainedDividersB = new ArrayList<>();
        for (Integer thisFrame : framesBackward) {
            // Offset all existing dividers within the retainedDividers list
            if (!thisFrame.equals(frameMax)) {
                retainedDividersB = retainedDividersB.stream().map(x -> x - thisOffsetChain.get(thisFrame)).collect(Collectors.toList());
            }
            // Append the dividers from this frame to the retainedDividers
            retainedDividersB = Stream.concat(retainedDividersB.stream(), thisDividerMap.get(thisFrame).stream()).collect(Collectors.toList());

            // Set the superOC as this field
            superOC.put(thisFrame, Stream.concat(superOC.get(thisFrame).stream(), retainedDividersB.stream()).collect(Collectors.toList()));
        }

        // A by-product of this approach is that the dividers from the original frames are carried forward, causing an imbalance in the number of
        // projected dividers across the offset chain - although this is not a bad thing, it can carry forward into an imbalance in the true dividers
        // that are generated in further steps - to prevent incosnsistnecy in resutls and unexpected behavuiour (especially considering that we
        // use true dividers as a reference point for further steps), we adjust the results in the last step by subtracting the divider map from the superoffset chain

        for (Integer thisFrame : frames) {
            for (Integer dividerToRemove : thisDividerMap.get(thisFrame)) {
                superOC.get(thisFrame).remove(dividerToRemove);
            }
        }

        return superOC;
    }

    public static HashMap<Integer, List<Integer>> trueDividersFromSuperOffsetChain(HashMap<Integer, List<Integer>> superOffsetChain, Integer h) {
        /*
         *
         * how do we determine true dividers for a frame?
         *
         * to do this, we firstly bin dividers into an average value - consider that within any fraction of the frame's height,
         * that all dividers that contribute to a single true divider should converge in a 0.1*frame-height range
         *
         * we bin the dividers (excluding those with limited observations), and then determine the standard deviation of each - if the dividers fit the 68 25 rule, then
         * their avg is a true divider
         *
         * */
        HashMap<Integer, List<Integer>> trueDividers = new HashMap<>();
        Double likenessThreshold = h * 0.1;
        Integer minAppearance = 1;


        for (Integer thisFrame : superOffsetChain.keySet()) {
            HashMap<Double, List<Double>> binnedDividers = binAsAverages(Args(
                    A("input", superOffsetChain.get(thisFrame).stream().mapToDouble(x -> x).boxed().collect(Collectors.toList())),
                    A("likeness", likenessThreshold)));

            // For each converged divider
            List<Integer> frameTrueDividers = new ArrayList<>();
            for (Double thisDividerAvg : binnedDividers.keySet()) {
                List<Double> dividersInBin = binnedDividers.get(thisDividerAvg);
                if (dividersInBin.size() >= minAppearance) {
                    //
                    // Consider that under normal circumstances that 68% of data resides in 1 SD from the average - 95% in 2 SD
                    // If we wish to obtain the top most centrally tended ~25% of data, we divide the SD by 68, then multiply by 25
                    // and add / minus it from the average to obtain an ideal bound
                    // EG. if the average is 3.69 and the SD is 3.48, the bound is 3.69 +- 1.27941176471
                    //
                    printJSON(thisDividerAvg);
                    printJSON(dividersInBin);
                    double stDev = getStandardDeviationD(dividersInBin);
                    printJSON(stDev);
                    List<Double> dividersOfOneDev = dividersInBin.stream().filter(x -> Math.abs(x - thisDividerAvg) <= (stDev*2)).collect(Collectors.toList());
                    printJSON(dividersOfOneDev);
                    printJSON((dividersOfOneDev.size() / (double) dividersInBin.size()));
                    if ((dividersOfOneDev.size() / (double) dividersInBin.size()) >= 0.75) { // TODO - can be relaxed
                        frameTrueDividers.add((int) Math.round(thisDividerAvg));
                    }
                }
            }
            trueDividers.put(thisFrame, frameTrueDividers);
        }

        return trueDividers;
    }



    public static JSONXObject frameSnippetsFromTrueDividers(HashMap<Integer, List<Integer>> trueDividers,
                                                                 HashMap<Integer, Integer> thisOffsetChain, Integer h,
                                                                 HashMap<Integer, JSONObject> signaturesMap, String thisMode) {
        JSONXObject output = new JSONXObject();
        output.set("success", true);
        output.set("exitStatus", "INCONCLUSIVE");
        List<JSONObject> frameSnippetsCollapsed = new ArrayList<>();
        try {

            // Firstly tag the true dividers so that they can be identified, independent of the frames they are identified in
            // also convey that the (upper/lower)-most limits define synthetic true dividers also
            Integer dividerMostUpper = null;
            Integer dividerMostLower = null;
            Integer cumulativeOffset = 0;
            Integer FSInterval = null;
            List<Integer> frames = thisOffsetChain.keySet().stream().sorted().collect(Collectors.toList());
            for (Integer thisFrame : frames) {
                if ((dividerMostUpper == null) || (dividerMostUpper > cumulativeOffset)) {
                    dividerMostUpper = cumulativeOffset;
                }
                if ((dividerMostLower == null) || (dividerMostLower < (cumulativeOffset + h))) {
                    dividerMostLower = cumulativeOffset + h;
                }
                // Apply the offset
                if (!thisFrame.equals(frames.get(frames.size()-1))) {
                    cumulativeOffset += thisOffsetChain.get(thisFrame);
                }
            }

            // you have to add the upper bound to the consistent boundaries to adjust them so that they align with the first frame
            List<Integer> consistentBoundaries = Arrays.asList(dividerMostUpper - dividerMostUpper, dividerMostLower - dividerMostUpper);
            // then take the true dividers for the first frame (assuming that all true dividers are carried across all frames)
            // and define boundaries

            // consistent boundaries are the bounds of the divider, as according to the first frame
            consistentBoundaries = Stream.concat(trueDividers.get(frames.get(0)).stream(), consistentBoundaries.stream()).sorted().collect(Collectors.toList());

            //System.out.println("dividerMostUpper: " + dividerMostUpper);
            //System.out.println("dividerMostLower: " + dividerMostLower);
            //System.out.println("consistentBoundaries: " + consistentBoundaries);

            List<JSONObject> snippets = new ArrayList<>();
            // generate the snippets, as according to the consistent boujndaries
            Integer nSnippets = 0;
            for (int i = 0; i < consistentBoundaries.size()-1; i ++) {
                JSONObject thisSnippet = new JSONObject();
                try {
                    thisSnippet.put("snippetID", nSnippets);
                    thisSnippet.put("upper", consistentBoundaries.get(i));
                    thisSnippet.put("lower", consistentBoundaries.get(i+1));
                    thisSnippet.put("height", Math.abs(consistentBoundaries.get(i) - consistentBoundaries.get(i+1)));
                    nSnippets ++;
                } catch (Exception e) {
                    output.set("exitStatus", "ERROR_AT_FRAME_SNIPPETS_SET_SNIPPET");
                    output.set("success", false); // TODO - determine if this causes a lock on processing
                    logger.error(e);
                    return output;

                }
                snippets.add(thisSnippet);
            }
            // then consider what boundaries are taken from each frame to derive the 'frame-contextualised snippets'
            Integer inhibitedRangeLookaround = 1;
            Integer cumulativeOffsetB = 0;

            try {
                FSInterval = (Integer) signaturesMap.get(frames.get(0)).get("strideY");
            } catch (Exception e) {
                output.set("exitStatus", "ERROR_AT_FRAME_SNIPPETS_RETRIEVE_FS_INTERVAL");
                output.set("success", false); // TODO - determine if this causes a lock on processing
                logger.error(e);
                return output;

            }

            Log.i(TAG, frames.toString());

            HashMap<Integer, List<JSONObject>> frameSnippets = new HashMap<>();
            for (Integer thisFrame : frames) {
                    Boolean cancelFrameSnippetAppendage = false;
                    //System.out.println("This frame: " + thisFrame);
                    // Retrieve the relevant frame signatures for this frame's inhibited range
                    List<HashMap<Integer, Boolean>> frameSignaturesWS = new ArrayList();
                    List<HashMap<Integer, HashMap<Integer, Integer>>> frameSignatures = new ArrayList();
                    int inhibitedRangeLookBehind = Math.max(frames.indexOf(thisFrame)-inhibitedRangeLookaround, 0);
                    int inhibitedRangeLookForward = Math.min(frames.indexOf(thisFrame)+inhibitedRangeLookaround, frames.size()-1);
                    for (int i = inhibitedRangeLookBehind; i <= inhibitedRangeLookForward; i ++) {
                        try {
                            frameSignatures.add((HashMap<Integer, HashMap<Integer, Integer>>) (signaturesMap.get(frames.get(i))).get("frameSignature"));
                            frameSignaturesWS.add((HashMap<Integer, Boolean>) (signaturesMap.get(frames.get(i))).get("frameSignatureWS"));
                        } catch (Exception e) {
                            cancelFrameSnippetAppendage = true;
                            //logger.error(e);
                            Log.i(TAG, "ATTACH: "+frames.get(i));
                            output.set("exitStatus", "ERROR_AT_FRAME_SNIPPETS_FRAME_SIGNATURES_ADD: "+exceptionWrite(e));
                            output.set("success", false); // TODO - determine if this causes a lock on processing
                            return output;
                        }
                    }

                    //if (!cancelFrameSnippetAppendage) {

                        // generate an inhibited range
                        List<List<Integer>> thisInhibitedRange = generateInhibitedRange(frameSignatures, frameSignaturesWS, h, FSInterval);

                        // as according to this frame, go over the snippets, and contextualise them - determine what parts reside in which frames, if at all
                        List<JSONObject> adjustedSnippets = new ArrayList<>();
                        for (JSONObject thisSnippetRaw : snippets) {
                            JSONObject thisSnippet = null;
                            try {
                                thisSnippet = new JSONObject(thisSnippetRaw.toString());
                            } catch (Exception e) {

                                output.set("exitStatus", "ERROR_AT_FRAME_SNIPPETS_RETRIEVE_SNIPPET_RAW");
                                output.set("success", false); // TODO - determine if this causes a lock on processing
                                logger.error(e);
                                return output;
                            }
                            Integer upper = null;
                            Integer lower = null;
                            try {
                                int x = forceToRange(((Integer) thisSnippet.get("upper")) + cumulativeOffsetB, 0, h - 1);
                                int y = forceToRange(((Integer) thisSnippet.get("lower")) + cumulativeOffsetB, 0, h - 1);
                                upper = Math.min(x, y);
                                lower = Math.max(x, y);
                                thisSnippet.put("upper", upper);
                                thisSnippet.put("lower", lower);
                            } catch (Exception e) {
                                output.set("exitStatus", "ERROR_AT_FRAME_SNIPPETS_FORCE_TO_RANGE");
                                output.set("success", false); // TODO - determine if this causes a lock on processing
                                logger.error(e);
                                return output;

                            }

                            // added - if the snippet is outside of the bounds of the frame, dont add it
                            if (!((upper == lower) && ((upper == 0) || (upper == (h - 1))))) {

                                //apply the adjusted values

                                // determine the inhibited range for this adjusted snippet
                                List<List<Integer>> adjustedInhibitedRange = new ArrayList<>();
                                Boolean perfectlyOverlapped = false;
                                Boolean outOfBounds = false;
                                for (List<Integer> aRange : thisInhibitedRange) {
                                    Integer rangeStart = aRange.get(0);
                                    Integer rangeEnd = aRange.get(1);
                                    Integer adjustedRangeStart = Math.max(rangeStart, upper);
                                    Integer adjustedRangeEnd = Math.min(rangeEnd, lower);
                                    if ((adjustedRangeEnd < 0) || (adjustedRangeStart > h)) {
                                        outOfBounds = true;
                                        break;
                                    }

                                    if (!(((rangeStart <= upper) && (rangeEnd <= upper)) || ((rangeStart >= lower) && (rangeEnd >= lower)))) { // out of range
                                        if ((adjustedRangeStart >= upper) && (adjustedRangeEnd <= lower)) {
                                            adjustedInhibitedRange.add(Arrays.asList(adjustedRangeStart, adjustedRangeEnd));
                                        }
                                        if ((adjustedRangeStart == upper) && (adjustedRangeEnd == lower)) {
                                            perfectlyOverlapped = true;
                                            break;
                                        }
                                    }
                                }
                                try {
                                    thisSnippet.put("inhibitedRanges", adjustedInhibitedRange);
                                } catch (Exception e) {
                                    output.set("exitStatus", "ERROR_AT_FRAME_SNIPPETS_INHIBITED_RANGES");
                                    output.set("success", false); // TODO - determine if this causes a lock on processing
                                    logger.error(e);
                                    return output;
                                }

                                if ((!perfectlyOverlapped) && (!outOfBounds)) {

                                    // determine the whitespace for this adjusted snippet
                                    try {
                                        HashMap<Integer, Boolean> thisFrameSignatureWS = (HashMap<Integer, Boolean>) signaturesMap.get(thisFrame).get("frameSignatureWS");
                                        List<Integer> isolatedFrameSignatureIntervals = thisFrameSignatureWS.keySet().stream().sorted()
                                                .filter(x -> thisFrameSignatureWS.get(x)).collect(Collectors.toList());
                                        //printJSON(isolatedFrameSignatureIntervals);
                                        //printJSON(discreteIntervalsToRanges(FSInterval, isolatedFrameSignatureIntervals));
                                        thisSnippet.put("whitespaceRanges", discreteIntervalsToRanges(FSInterval, isolatedFrameSignatureIntervals));
                                    } catch (Exception e) {
                                        output.set("exitStatus", "ERROR_AT_FRAME_SNIPPETS_WS_RANGES");
                                        output.set("success", false); // TODO - determine if this causes a lock on processing
                                        logger.error(e);
                                        return output;

                                    }

                                    adjustedSnippets.add(thisSnippet);
                                }
                            }
                            frameSnippets.put(thisFrame, adjustedSnippets);
                        }
                    //}

                if (thisFrame != frames.get(frames.size() - 1)) {
                    cumulativeOffsetB += thisOffsetChain.get(thisFrame);
                }
            }

            //printJSON(frameSnippets);
            //System.exit(0);

            // Collapse the snippets into datastructures

            for (int thisSnippetID = 0; thisSnippetID < nSnippets; thisSnippetID ++) {
                Boolean thisSnippetInitiated = false;
                JSONObject thisSnippet = new JSONObject();
                HashMap<Integer, List<Integer>> thisSnippetBoundaries = new HashMap<>();
                HashMap<Integer, List<List<Integer>>> thisSnippetWhitespaceRanges = new HashMap<>();
                HashMap<Integer, List<List<Integer>>> thisSnippetInhibitedRanges = new HashMap<>();

                for (Integer thisFrame : frames) {
                    int finalThisSnippetID = thisSnippetID;
                    // Retrieve the snippet, as relative to this frame
                    List<JSONObject> thisSnippetWithinFrameCandidates = frameSnippets.get(thisFrame).stream().filter(x -> {
                        try {
                            return (((Integer) x.get("snippetID")) == finalThisSnippetID);
                        } catch (Exception e) {
                            // This does not cause an error as its a lambda call on a filter
                            return false;
                        }
                    }).collect(Collectors.toList());
                    JSONObject thisSnippetWithinFrame = null;
                    if (!thisSnippetWithinFrameCandidates.isEmpty()) {
                        thisSnippetWithinFrame = thisSnippetWithinFrameCandidates.get(0);
                    }
                    //printJSON(thisSnippetWithinFrame);

                    // If the snippet is within the frame, we can proceed
                    if (thisSnippetWithinFrame != null) {
                        thisSnippetBoundaries.put(thisFrame, Arrays.asList(
                                (Integer) thisSnippetWithinFrame.get("upper"),
                                (Integer) thisSnippetWithinFrame.get("lower")
                        ));
                        List<List<Integer>> thisSnippetFrameWhitespaceRanges = (List<List<Integer>>) thisSnippetWithinFrame.get("whitespaceRanges");
                        List<List<Integer>> thisSnippetFrameInhibitedRanges = (List<List<Integer>>) thisSnippetWithinFrame.get("inhibitedRanges");
                        if (!thisSnippetFrameWhitespaceRanges.isEmpty()) {
                            thisSnippetWhitespaceRanges.put(thisFrame, thisSnippetFrameWhitespaceRanges);
                        }
                        if (!thisSnippetFrameInhibitedRanges.isEmpty()) {
                            thisSnippetInhibitedRanges.put(thisFrame, thisSnippetFrameInhibitedRanges);
                        }
                        if (!thisSnippetInitiated) {
                            thisSnippet.put("mode", thisMode);
                            thisSnippet.put("height", thisSnippetWithinFrame.get("height"));
                            thisSnippetInitiated = true;
                        }
                    }
                }

                try {
                    thisSnippet.put("boundaries", thisSnippetBoundaries);
                    thisSnippet.put("inhibitedRanges", thisSnippetInhibitedRanges);
                    thisSnippet.put("whitespaceRanges", thisSnippetWhitespaceRanges);
                } catch (Exception e) {
                    output.set("exitStatus", "ERROR_AT_FRAME_SNIPPETS_APPLY_ON_SNIPPET");
                    output.set("success", false); // TODO - determine if this causes a lock on processing
                    logger.error(e);
                    return output;

                }
                frameSnippetsCollapsed.add(thisSnippet);
            }
            int a = 1;
            int b = 2;

        } catch (Exception e) {
            e.printStackTrace();
            output.set("exitStatus", "ERROR_AT_FRAME_SNIPPETS_UNCAUGHT: "+e.getMessage());
            output.set("success", false); // TODO - determine if this causes a lock on processing
            logger.error(e);
            return output;
        }
        //System.exit(0);
        output.set("frameSnippetsCollapsed", frameSnippetsCollapsed); // List<JSONObject>

        return output;
    }

    public static List<Integer> cropRangeToBoundary(List<Integer> thisRange, List<Integer> thisBoundary) {
        Integer bMin = Math.min(thisBoundary.get(0), thisBoundary.get(1));
        Integer bMax = Math.max(thisBoundary.get(0), thisBoundary.get(1));
        Integer rMin = Math.min(thisRange.get(0), thisRange.get(1));
        Integer rMax = Math.max(thisRange.get(0), thisRange.get(1));
        if (rangesOverlap(rMin, rMax, bMin, bMax) != 0) {
            if ((rMin < bMin) && (rMax > bMax)) {
                return Arrays.asList(bMin, bMax);
            } else
            if ((rMin < bMin) && (rMax > bMin)) {
                return Arrays.asList(bMin, rMax);
            } else
            if ((rMin > bMin) && (rMax > bMax)) {
                return Arrays.asList(rMin, bMax);
            } else if ((rMin >= bMin) && (rMax <= bMax)) {
                return Arrays.asList(rMin, rMax);
            }
        }
        return null;
    }

    /* at this stage in the analysis, the temp directory has been populated with screenshots of the relevant video,
     * and each of the consecutive sdcreenshots have been reasonably linked, with the boundaries of each post therein analysed.
     *
     * a consistent linking of screenshots is called an offset chain
     *
     * a post (be it ad or otherwise) that has been independently extracted from its observation across all screenshots is then a framesnippet
     *
     * this function considers a single frame snippet, preparing it for upload
     *
     * NOTE: Certain frame-relative statistics are only collected if they are evaulated
     * (which doesn't happen for instances that short-circuit on the detection of a Facebook ad header)
     *
     * TODO - we can optimize this part by not analyzing all frames (i.e., finding out which are different, and running the analysis that way)
     * TODO - potentially can adapt the censoring of the inhibited range to a different logic that cuts it out of the image
     * TODO - not all images are picked up by the ad header checker
     *
     * */
    public static JSONXObject generateSnippetCroppings(File thisFrameSnippetDirectory, File tempFramesDirectory, JSONObject thisFrameSnippet,
                                                      JSONObject thisFitter, HashMap<String, Object> pictogramsReference, Integer thisFrameSnippetID, Boolean verbose,
                                                      Integer determinedWSColor, String determinedExposureType) {
        JSONXObject output = new JSONXObject();
        output.set("success", true);
        output.set("exitStatus", "INCONCLUSIVE");
        JSONObject statistics = new JSONObject();
        try {
            Double elapsedTimeToGenerateSnippetCroppings = Long.valueOf(System.currentTimeMillis()).doubleValue();
            Integer minimumAnalyzableHeightOfFrame = 30;
            Double topRegionOfImageRatio = 0.3;
            List<String> generatedCroppingFiles = new ArrayList<>();
            HashMap<Integer, List<Integer>> boundaries = new HashMap<>();
            HashMap<Integer, List<List<Integer>>> inhibitedRanges = new HashMap<>();
            HashMap<Integer, List<List<Integer>>> whitespaceRanges = new HashMap<>();
            HashMap<Integer, List<List<Integer>>> whitespaceRangesAdjustedA = new HashMap<>();
            HashMap<Integer, List<List<Integer>>> whitespaceRangesAdjustedB = new HashMap<>();
            HashMap<Integer, JSONObject> frameFacebookAdHeaderDetections = new HashMap<>();
            HashMap<Integer, Integer> frameBoundaryHeightsContextualised = new HashMap<>();
            Integer maximumHeightOfFrameSnippet = 0;
            try {
                boundaries = (HashMap<Integer, List<Integer>>) thisFrameSnippet.get("boundaries");
                inhibitedRanges = (HashMap<Integer, List<List<Integer>>>) thisFrameSnippet.get("inhibitedRanges");
                whitespaceRanges = (HashMap<Integer, List<List<Integer>>>) thisFrameSnippet.get("whitespaceRanges");
                maximumHeightOfFrameSnippet = (Integer) thisFrameSnippet.get("height");
            } catch (Exception e) {
                output.set("success", false);
                output.set("exitStatus", "ERROR_AT_RETRIEVE_VALUES");
                logger.error(e);
                return output;

            }

            // For each frame...
            Boolean determinedAsFacebookAd = false;
            for (Integer thisFrame : boundaries.keySet()) {
                Bitmap thisFrameBitmap = BitmapFactory.decodeFile((new File(tempFramesDirectory, thisFrame + "." + fileFormat)).getAbsolutePath());

                // Retrieve the inhibited ranges for this frame
                List<List<Integer>> frameInhibitedRanges = new ArrayList<>();
                if (inhibitedRanges.containsKey(thisFrame)) { frameInhibitedRanges = inhibitedRanges.get(thisFrame); }

                // Determine the height of frame, as relative to the frame
                Integer boundaryHeightContextualised = Math.abs(boundaries.get(thisFrame).get(0) - boundaries.get(thisFrame).get(1));
                frameBoundaryHeightsContextualised.put(thisFrame, boundaryHeightContextualised);

                if (!determinedAsFacebookAd) {
                    // Retrieve the whitespace ranges for this frame
                    List<List<Integer>> frameWhitespaceRanges = new ArrayList<>();
                    if (whitespaceRanges.containsKey(thisFrame)) { frameWhitespaceRanges = whitespaceRanges.get(thisFrame); }

                    // Filter the frameWhitespaceRanges to those that occur within the top region of the image
                    List<List<Integer>> frameWhitespaceRangesAdjusted = new ArrayList<>();
                    for (List<Integer> frameWhitespaceRange : frameWhitespaceRanges) {
                        List<Integer> thisCroppedRange = cropRangeToBoundary(frameWhitespaceRange, boundaries.get(thisFrame));
                        if (thisCroppedRange != null) {
                            frameWhitespaceRangesAdjusted.add(thisCroppedRange);
                        }
                    }

                    // Put the adjusted ranges into the whitespaceRangesAdjusted data-structure
                    whitespaceRangesAdjustedA.put(thisFrame, new ArrayList<>(frameWhitespaceRangesAdjusted));

                    // Go over the inhibited ranges and subtract them from the whitespace ranges
                    for (List<Integer> thisInhibitedRange : frameInhibitedRanges) {
                        // For thisInhibitedRange...
                        Pair<Integer, Integer> iR = orderRange(thisInhibitedRange);
                        List<List<Integer>> frameWhitespaceRangesTemp = new ArrayList<>();
                        // Interrogate thisWhitespaceRange...
                        for (List<Integer> thisWhitespaceRange : frameWhitespaceRangesAdjusted) {
                            Pair<Integer, Integer> wR = orderRange(thisWhitespaceRange);
                            // If the ranges of both overlap...
                            if (rangesOverlap(iR.first, iR.second, wR.first, wR.second) != 0) {
                                // Derive new ranges from their difference
                                List<List<Integer>> derivedRanges = subtractRanges(thisWhitespaceRange, thisInhibitedRange);
                                // Append the new ranges to the temporary aggregation of all ranges
                                frameWhitespaceRangesTemp = Stream.concat(frameWhitespaceRangesTemp.stream(), derivedRanges.stream()).collect(Collectors.toList());
                            } else {
                                // If thisWhitespaceRange is not overlapped by thisInhibitedRange, it can be applied as it is
                                frameWhitespaceRangesTemp.add(thisWhitespaceRange);
                            }
                        }
                        // Dispatch the adjusted whitespace ranges to the next iteration of the loop
                        frameWhitespaceRangesAdjusted = frameWhitespaceRangesTemp;
                    }

                    // Don't analyze ranges below a certain height
                    frameWhitespaceRangesAdjusted = frameWhitespaceRangesAdjusted.stream()
                            .filter(x -> Math.abs(x.get(0) - x.get(1)) >= minimumAnalyzableHeightOfFrame).collect(Collectors.toList());

                    // Put the adjusted ranges into the whitespaceRangesAdjusted data-structure
                    whitespaceRangesAdjustedB.put(thisFrame, frameWhitespaceRangesAdjusted);

                    // Each retained whitespace range then corresponds to a possible ad header - we now assess it by taking
                    // the range, and cropping the frame's bitmap to it, passing it through the ad checker, and returning the result
                    for (List<Integer> thisRange : frameWhitespaceRangesAdjusted) {
                        // If this frame snippet has not already been determined as a Facebook ad, and this whitespace range is within
                        // the top region of the boundary that belongs to the frame snippet...
                        if (rangesOverlap(thisRange.get(0), thisRange.get(1), boundaries.get(thisFrame).get(0),
                                boundaries.get(thisFrame).get(0) + ((int) Math.floor(boundaryHeightContextualised * topRegionOfImageRatio))) != 0) {
                            // Assign a UUID to uniquely characterize this whitespace range within the frame snippet
                            // Isolate the bitmap for this range
                            Integer cappedHeight = Math.min(Math.abs(thisRange.get(0) - thisRange.get(1)), thisFrameBitmap.getHeight() - (1 + thisRange.get(0)));
                            int a = 1;
                            int frameHeight = thisFrameBitmap.getHeight();
                            int offset = thisRange.get(0);
                            int cappedHeightPlusOffset = thisRange.get(0) + cappedHeight;
                            int b = 2;
                            Bitmap thisRangeBitmap = Bitmap.createBitmap(thisFrameBitmap, 0, thisRange.get(0),
                                    thisFrameBitmap.getWidth(), cappedHeight);
                            // Determine whether thisRangeBitmap is a Facebook Ad Header
                            JSONObject result = isFacebookAdHeader(thisRangeBitmap, fitterDispatch(thisFitter), pictogramsReference,
                                    determinedWSColor, determinedExposureType);
                            try {
                                result.put("thisRange", thisRange);
                                if (result.get("outcome").equals("MATCHED")) {
                                    determinedAsFacebookAd = true;
                                }
                            } catch (Exception e) {
                                output.set("success", false);
                                output.set("exitStatus", "ERROR_AT_DETERMINE_AS_FB_AD");
                                logger.error(e);
                                return output;
                            }
                            frameFacebookAdHeaderDetections.put(thisFrame, result);

                            if (verbose) {
                                String rangeUUID = UUID.randomUUID().toString();
                                writeToJSON(new File(thisFrameSnippetDirectory, thisFrame + "-x-" + rangeUUID + ".json"), result);
                                saveBitmap(thisRangeBitmap, new File(thisFrameSnippetDirectory, thisFrame + "-x-" + rangeUUID + "." + fileFormat).getAbsolutePath());
                            }
                        }
                    }
                }

                // The next code block crops the frame to the whitespace range therein...

                // Censor the inhibited ranges prior to cropping
                if (inhibitedRanges.containsKey(thisFrame)) {
                    // For each inhibited range...

                    for (List<Integer> thisInhibitedRange : inhibitedRanges.get(thisFrame)) {
                        try {
                            // Override it with empty pixels
                            Pair<Integer, Integer> iR = orderRange(thisInhibitedRange);
                            Integer w = thisFrameBitmap.getWidth();
                            Integer h = Math.abs(iR.first - iR.second);
                            int[] pixelFill = IntStream.range(0, w*h).map(x -> Color.argb(0,0,0,0)).toArray();
                            thisFrameBitmap.setPixels(pixelFill, 0, 0, 0, iR.first, w, h);
                        } catch (Exception e) {
                            // Errors caused here are usually the result of overflow in pixel settings and shouldn't be worried about
                            logger.error(e);

                        }
                    }
                }

                // Isolate the cropping of the frame snippet from thisFrame
                if (rangesOverlap(boundaries.get(thisFrame).get(0), boundaries.get(thisFrame).get(1), 0, thisFrameBitmap.getHeight()) > 0) {
                    // Cap the height of the boundary to the height of the image
                    Integer cappedRangeEnd = Math.min(boundaries.get(thisFrame).get(0) + boundaryHeightContextualised, thisFrameBitmap.getHeight()-1);
                    boundaryHeightContextualised = Math.min(boundaryHeightContextualised, Math.abs(cappedRangeEnd - boundaries.get(thisFrame).get(0)));
                    if (boundaryHeightContextualised > 0) {
                        Bitmap thisCropping = Bitmap.createBitmap(thisFrameBitmap, 0, boundaries.get(thisFrame).get(0), thisFrameBitmap.getWidth(), boundaryHeightContextualised);
                        Bitmap thisAdjustedCropping = thisCropping.copy(thisCropping.getConfig(), true);
                        // Insert the cropping onto a canvas that is of boundaryHeight, relative to frame snippet itself
                        thisAdjustedCropping = overlayBitmaps(
                                Bitmap.createBitmap(Bitmap.createBitmap(thisFrameBitmap.getWidth(), maximumHeightOfFrameSnippet, Bitmap.Config.ARGB_8888)), thisAdjustedCropping, 0, 0);
                        // Save the bitmap
                        // NOTE: While it can be argued that we shouldn't be saving bitmaps for frame snippets that end up not being ads, it is less computationally
                        // expensive to save it while it is in memory (just after evaluating it), instead of running the evaluations, and then going back and saving the
                        // frame snippet a second time.
                        String thisFrameCroppingName = thisFrame+"." + fileFormat;
                        generatedCroppingFiles.add(thisFrameCroppingName);
                        saveBitmap(thisAdjustedCropping, new File(thisFrameSnippetDirectory, thisFrameCroppingName).getAbsolutePath());
                    }
                }
            }

            try {
                statistics.put("thisFrameSnippetID", thisFrameSnippetID);
                statistics.put("topRegionOfImageRatio", topRegionOfImageRatio);
                statistics.put("generatedCroppingFiles", generatedCroppingFiles);
                statistics.put("height", null); // TODO - fix
                statistics.put("exposureMode", determinedExposureType);
                statistics.put("boundaries", boundaries);
                statistics.put("inhibitedRanges", inhibitedRanges);
                statistics.put("whitespaceRanges", whitespaceRanges);
                statistics.put("whitespaceRangesAdjustedA", whitespaceRangesAdjustedA);
                statistics.put("whitespaceRangesAdjustedB", whitespaceRangesAdjustedB);
                statistics.put("maximumHeightOfFrameSnippet", maximumHeightOfFrameSnippet);
                statistics.put("frameFacebookAdHeaderDetections", frameFacebookAdHeaderDetections);
                statistics.put("frameBoundaryHeightsContextualised", frameBoundaryHeightsContextualised);
                statistics.put("determinedAsFacebookAd", determinedAsFacebookAd);
                statistics.put("elapsedTimeToGenerateSnippetCroppings", Math.abs(elapsedTimeToGenerateSnippetCroppings - System.currentTimeMillis()));
            } catch (Exception e) {
                output.set("success", false);
                output.set("exitStatus", "ERROR_AT_SET_VALUES");
                logger.error(e);
                return output;
            }
        } catch (Exception eX) {
            logger.info(eX.getStackTrace()[0].getLineNumber());
            logger.info(eX.getCause());
            logger.error(eX);
            eX.printStackTrace();
            output.set("success", false);
            output.set("exitStatus", "GENERAL_ERROR");
            return output;
        }
        output.set("statistics", statistics);
        return output;
    }

    // this function takes a master offset chain, and converts it into sub-offset chains

    /*
     *
     * This function accepts frameSampleMetadata, converting a masterOffsetChain (for the entire video)
     * into its constituent offset chains, based off how the corresponding frames within the
     * offset chain compare to one another.
     *
     * */
    public static JSONObject masterOffsetChainToSubOffsetChains(JSONObject frameSampleMetadata) {
        String exitStatus = "INCONCLUSIVE";
        Boolean proceedOnThis = true;

        Double elapsedTimeMasterOffsetChainToSubOffsetChains = Long.valueOf(System.currentTimeMillis()).doubleValue();
        JSONObject statistics = new JSONObject();
        // Determine the 'sub-' offset chains from the 'masterOffsetChain'
        List<JSONObject> masterOffsetChain = new ArrayList<>();
        try {
            masterOffsetChain = ((List<JSONObject>) ((JSONObject) frameSampleMetadata.get("statistics")).get("offsetChain"));
        } catch (Exception e) {
            exitStatus = "MASTER_OFFSET_CHAIN_LOAD_FAILURE: "+exceptionWrite(e);
            logger.error(e);
            proceedOnThis = false;

        }

        // Paginate through the entire masterOffsetChain
        List<HashMap<Integer, Integer>> offsetChains = new ArrayList<>();
        HashMap<Integer, Integer> currentOffsetChain = new HashMap<>();
        Integer retainedLastFrame = null;
        // For each comparison within the masterOffsetChain...
        for (JSONObject thisComparison : masterOffsetChain) {
            try {
                Integer offsetWithMaxSimilarity = null;
                try {
                    // There's a possibility that the offset between the frames may not be accounted
                    // (as such in the case of 'TOO_DIFFERENT' frames) - this is not determined until later in the code block
                    // so there is a possibility that this expression may fail
                    JSONObject comparisonResult = (JSONObject) thisComparison.get("comparisonResult");
                    if (comparisonResult.has("offsetWithMaxSimilarity")) {
                        offsetWithMaxSimilarity = ((Integer) (comparisonResult.get("offsetWithMaxSimilarity")));
                    }
                } catch (Exception e) {
                    logger.error(e); }

                String outcome = ((String) ((JSONObject) thisComparison.get("comparisonResult")).get("outcome"));
                Integer lastFrame = (Integer) thisComparison.get("lastFrame");
                Integer currentFrame = (Integer) thisComparison.get("currentFrame");

                // It is very possible that two consecutive frames may (under circumstance) be entirely dissimilar -
                // this causes two values within the currentOffsetChain to be null, violating the assumption
                // of only the last entry holding 'null' status - to counter this, we apply the condition below:
                // If the lastFrame within thisComparison does not equal the retained frame
                // from the last comparison...
                if ((retainedLastFrame != null
                        && (currentOffsetChain.containsKey(retainedLastFrame))
                        && (currentOffsetChain.get(retainedLastFrame) == null)
                        && (!retainedLastFrame.equals(lastFrame)))) {
                    // Dispatch the currentOffsetChain and start anew
                    offsetChains.add(new HashMap<>(currentOffsetChain));
                    currentOffsetChain = new HashMap<>();
                }



                // In all cases, the lastFrame within thisComparison is added to the currentOffsetChain, along
                // with its offset (offsetWithMaxSimilarity)
                currentOffsetChain.put(lastFrame, offsetWithMaxSimilarity);

                // If the outcome of thisComparison is TOO_DIFFERENT...
                if (outcome.equals("TOO_DIFFERENT")) {
                    // Dispatch the currentOffsetChain and start anew, with the currentFrame having a 'null' offset
                    // NOTE: The null offset shouldn't be declared for the first frame of an offset chain, considering
                    // that each frame's offset indicates how it relates to the frame after it (which hasn't yet been
                    // determined, given that we are paginating forwards and not backwards). However, in this case it
                    // can be overlooked, as the data-structure is a HashMap, which will most likely override the null
                    // value when a tangible offset is determined.
                    offsetChains.add(new HashMap<>(currentOffsetChain));
                    currentOffsetChain = new HashMap<>();
                    currentOffsetChain.put(currentFrame, null);
                } else if (thisComparison == masterOffsetChain.get(masterOffsetChain.size() - 1)) {
                    // In the alternative case, thisComparison is the last within the masterOffsetChain and needs to be
                    // dispatched regardless
                    currentOffsetChain.put(currentFrame, null);
                    offsetChains.add(new HashMap<>(currentOffsetChain));
                } else if (offsetWithMaxSimilarity == null) {
                    offsetChains.add(new HashMap<>(currentOffsetChain));
                    currentOffsetChain = new HashMap<>();
                    currentOffsetChain.put(currentFrame, null);
                }

                // Update the retainedLastFrame at the end of every iteration
                retainedLastFrame = currentFrame;
            } catch (Exception e) {
                logger.error(e);
                exitStatus = "INTANGIBLE_ERROR_REACHED";
                proceedOnThis = false;
                // TODO - not yet reached in any tangible case
            }
        }
        try {
            statistics.put("offsetChains", offsetChains);
            statistics.put("elapsedTimeMasterOffsetChainToSubOffsetChains", Math.abs(elapsedTimeMasterOffsetChainToSubOffsetChains - System.currentTimeMillis()));
            if (exitStatus.equals("INCONCLUSIVE")) {
                exitStatus = "SUCCESS";
            }
        } catch (Exception e) {
            logger.error(e);
            proceedOnThis = false;
            exitStatus = "WRITE_FAILURE: "+exceptionWrite(e);
        }

        try {
            statistics.put("exitStatus", exitStatus);
            statistics.put("proceedOnThis", proceedOnThis);
        } catch (Exception e) {}
        return statistics;
    }



    // TODO - find out what this function does
    public static List<Integer> generateNewMatches(List<Integer> matches, int verticalStrideUnit) {
        List<Integer> newMatches = new ArrayList<>();
        for (int i  = 0; i < matches.size(); i ++) {
            List<Integer> thisBundle = new ArrayList<>();
            for (int j = 0; j < newMatches.size(); j ++) {
                if (Math.abs(newMatches.get(j) - matches.get(i)) <= (verticalStrideUnit*1.2*2)) {
                    thisBundle.add(newMatches.get(j));
                }
            }
            thisBundle.add(matches.get(i));
            // Filter the newMatches using the bundle
            newMatches = newMatches.stream().filter(x -> (!thisBundle.contains(x))).collect(Collectors.toList());
            // Add the bundle's average to the newMatches
            newMatches.add((int) Math.round(optionalGetDouble(thisBundle.stream().mapToDouble(x -> x).average())));
        }

        return newMatches;
    }


    // facebook

    public static List<Integer> findDividersInScreenshot(Bitmap thisBitmap, JSONObject statistics, boolean verbose) {

        int verticalStrideUnit = 0;
        HashMap<Integer, Double> ArrayThisAverageColourDifferenceToWhitespacePixelPercentage = new HashMap<>();
        HashMap<Integer, Double> ArrayThisDominantColourDifferenceToWhitespacePixelPercentage = new HashMap<>();
        HashMap<Integer, Double> ArraySpreadOfDominantColourPercentage = new HashMap<>();
        HashMap<Integer, Double> ArrayFrequencyOfDominantColourPercentage = new HashMap<>();
        HashMap<Integer, Double> ArrayMaxPixelAdjacency = new HashMap<>();
        try {
            verticalStrideUnit = (int) statistics.get("verticalStrideUnit");
            ArrayThisAverageColourDifferenceToWhitespacePixelPercentage = (HashMap<Integer, Double>) statistics.get("ArrayThisAverageColourDifferenceToWhitespacePixelPercentage");
            ArrayThisDominantColourDifferenceToWhitespacePixelPercentage = (HashMap<Integer, Double>) statistics.get("ArrayThisDominantColourDifferenceToWhitespacePixelPercentage");
            ArraySpreadOfDominantColourPercentage = (HashMap<Integer, Double>) statistics.get("ArraySpreadOfDominantColourPercentage");
            ArrayFrequencyOfDominantColourPercentage = (HashMap<Integer, Double>) statistics.get("ArrayFrequencyOfDominantColourPercentage");
            ArrayMaxPixelAdjacency = (HashMap<Integer, Double>) statistics.get("ArrayMaxPixelAdjacency");
        } catch (Exception e) {

            logger.error(e);
        }

        final int verticalStrideUnitFinal = verticalStrideUnit;

        List<Integer> matches = new ArrayList<>();

        // take the max of the last 6 rows' averages
        int lastYYThatWasFlagged = 0;
        int sampleSizeForColourChecking = 8;
        int startOffset = 4;
        for (int yy =  ((int)Math.round(Math.round(thisBitmap.getHeight()*0.1/verticalStrideUnit)*verticalStrideUnit)); yy < thisBitmap.getHeight(); yy += verticalStrideUnit) {
            int backLower = Math.max(0, yy-(sampleSizeForColourChecking*verticalStrideUnit));
            int backUpper = Math.max(0, yy-(verticalStrideUnit*startOffset));
            int forwardLower = yy+(verticalStrideUnit*startOffset);
            int forwardUpper = Math.min(thisBitmap.getHeight(), yy+(sampleSizeForColourChecking*verticalStrideUnit));
            Double avgAverageColourDifferenceBackwargs = optionalGetDouble(
                    IntStream.range(backLower, backUpper)
                            .filter(x -> x % verticalStrideUnitFinal == 0).mapToObj(
                                    ArrayThisAverageColourDifferenceToWhitespacePixelPercentage::get).mapToDouble(x -> x).average());

            Double avgAverageColourDifferenceForwards = optionalGetDouble(
                    IntStream.range(forwardLower, forwardUpper)
                            .filter(x -> x % verticalStrideUnitFinal == 0).mapToObj(
                                    ArrayThisAverageColourDifferenceToWhitespacePixelPercentage::get).mapToDouble(x -> x).average());


            Double maxDominantColourDifferenceBackwargs = optionalGetDouble(
                    IntStream.range(backLower, backUpper)
                            .filter(x -> x % verticalStrideUnitFinal == 0).mapToObj(
                                    ArrayThisDominantColourDifferenceToWhitespacePixelPercentage::get).mapToDouble(x -> x).average());

            // Does the most frequented colour of this row differ significantly from the coloour of whitespace?
            //boolean c1 = (ArrayThisAverageColourDifferenceToWhitespacePixelPercentage.get(yy) > 0.02); // very sensitive reading - it was set originally to 0.02, but we found that certain exposures failed on it (e.g. still_frames_test_3)


            double BD = Math.abs(ArrayThisAverageColourDifferenceToWhitespacePixelPercentage.get(yy) - avgAverageColourDifferenceBackwargs);

            double FD = Math.abs(ArrayThisAverageColourDifferenceToWhitespacePixelPercentage.get(yy) - avgAverageColourDifferenceForwards);

            double TD = ArrayThisAverageColourDifferenceToWhitespacePixelPercentage.get(yy);
            if (verbose) {

                System.out.println("yy: "+ yy + " B: "+backLower+" -> "+backUpper+" F: "+forwardLower+" -> "+forwardUpper);
                System.out.println("\t\tBD: "+ BD);
                System.out.println("\t\tFD: "+ FD);
                System.out.println("\t\tTD: "+ TD);
            }


            double uB = TD*0.95; // 0.04
            double lB = TD*0.4; // 0.04



            boolean c1 = (((BD < uB) || (FD < uB)) && ((BD <= TD) && (FD <= TD)));
            boolean cZ = true;//((BD > lB) && (FD > lB));

            boolean cX = (TD > 0.02);//Math.abs(BD - FD) < (Math.abs(BD) - TD / 2); // up from 0.01

            double upperBoundOnWhitespaceColor = 0.005;// optionalGetDouble(Arrays.asList(BD, FD).stream().mapToDouble(x -> x).average())*3;

            boolean cY = true;//(ArrayThisAverageColourDifferenceToWhitespacePixelPercentage.get(yy) > upperBoundOnWhitespaceColor);

            //boolean c1 = true;//((Math.abs(ArrayThisAverageColourDifferenceToWhitespacePixelPercentage.get(yy) - avgAverageColourDifferenceBackwargs) <= 0.02)
            //  && (Math.abs(ArrayThisAverageColourDifferenceToWhitespacePixelPercentage.get(yy) - avgAverageColourDifferenceForwards) <= 0.02));

            //boolean c7 = (ArrayThisDominantColourDifferenceToWhitespacePixelPercentage.get(yy) > 0.0); // very sensitive reading
            // Is the spread of the dominant colour at least across 90% of the space of the row
            boolean c2 = (ArraySpreadOfDominantColourPercentage.get(yy) >= 0.90);
            // Does the dominant colour appear in at least 60% of the colours sampled within the row?
            boolean c3 = (ArrayFrequencyOfDominantColourPercentage.get(yy) > 0.6);

            boolean c12 = (ArrayMaxPixelAdjacency.get(yy) < 0.10);

            //ystem.out.println(yy + " " + ArrayThisDominantColourDifferenceToWhitespacePixelPercentage.get(yy));
            //System.out.println(yy + " " + maxAverageColourDifferenceBackwargs + " " + maxDominantColourDifferenceBackwargs + " " + ArrayThisAverageColourDifferenceToWhitespacePixelPercentage.get(yy) );
            if (c1 && c2 && c3 && c12 && cX && cY && cZ) {

                double allowableDeviation = 0.03;
                boolean c4 = true;//(avgAverageColourDifferenceBackwargs <= allowableDeviation);

                boolean c5 = (maxDominantColourDifferenceBackwargs <= 0.05);

                Double maxAverageColourDifferenceForwards = optionalGetDouble(
                        IntStream.range(Math.min(thisBitmap.getHeight(), yy+verticalStrideUnit + (verticalStrideUnit*startOffset)), Math.min(thisBitmap.getHeight(), yy+(sampleSizeForColourChecking*verticalStrideUnit)))
                                .filter(x -> x % verticalStrideUnitFinal == 0).mapToObj(
                                        ArrayThisAverageColourDifferenceToWhitespacePixelPercentage::get).mapToDouble(x -> x).average());

                boolean c6 = (maxAverageColourDifferenceForwards <= allowableDeviation);


                if (c4 && c5 && c6) {
                    if (verbose) {
                        System.out.println(yy + " !!!!!!!!!!!!!!!!!!!!!!!!!!!!!! ");
                        System.out.println("\t maxAverageColourDifferenceBackwargs: " + avgAverageColourDifferenceBackwargs);
                        System.out.println("\t maxAverageColourDifferenceForwards: " + maxAverageColourDifferenceForwards);
                    }
                    matches.add(yy);
                }
            }
        }

        List<Integer> newMatches = generateNewMatches(matches, verticalStrideUnit);

        return newMatches;
    }





    public static JSONObject generateScreenshotStatistics(Bitmap thisBitmap, boolean verbose) {
        int whitespaceColour = whitespacePixelFromImage(Args(A("bitmap", thisBitmap)));
        int whitespaceColourR = Color.red(whitespaceColour);
        int whitespaceColourG = Color.green(whitespaceColour);
        int whitespaceColourB = Color.blue(whitespaceColour);

        if (verbose) { System.out.println("Whitespace colour: " + colourToHex(whitespaceColour)); }

        // Firstly identify all dividers within the image
        // Take the edges of the page vertically down and use that to identify dividers
        // To avoid double-analysing content, we are going to generate a few statistics once
        //    * The whitespace reading of each pixel is one of them


        HashMap<Integer, Integer> verticalIndicesColours = new HashMap<>();
        HashMap<Integer, Double> verticalIndicesWhitespaceDiff = new HashMap<>();

        int verticalStrideUnit = (int) Math.round(thisBitmap.getHeight()*0.002); // 0.0025);
        //System.out.println(verticalStrideUnit);
        int horizontalStrideUnit = (int) Math.round(thisBitmap.getWidth()*0.025);
        double linkingThreshold = 0.3;

        HashMap<Integer, Double> ArraySpreadOfDominantColourPercentage = new HashMap<>();
        HashMap<Integer, Double> ArrayFrequencyOfDominantColourPercentage = new HashMap<>();
        HashMap<Integer, Double> ArrayThisDominantColourDifferenceToWhitespacePixelPercentage = new HashMap<>();
        HashMap<Integer, Double> ArrayThisAverageColourDifferenceToWhitespacePixelPercentage = new HashMap<>();
        HashMap<Integer, Double> ArrayThisAverageColourRDifferenceToWhitespacePixelRPercentage = new HashMap<>();
        HashMap<Integer, Double> ArrayThisAverageColourGDifferenceToWhitespacePixelGPercentage = new HashMap<>();
        HashMap<Integer, Double> ArrayThisAverageColourBDifferenceToWhitespacePixelBPercentage = new HashMap<>();
        HashMap<Integer, Double> ArrayMaxPixelAdjacency = new HashMap<>();
        HashMap<Integer, Double> ArrayPixels = new HashMap<>();



        // Determine the number of pixels that should be stridden across the X axis
        int nPixels = (int) Math.floor((double) thisBitmap.getWidth() / horizontalStrideUnit);

        for (int yy = 0; yy < thisBitmap.getHeight(); yy += verticalStrideUnit) {
            // Retrieve the pixels for each row
            int[] pixels = new int[nPixels];
            for (int ii = 0; ii < nPixels; ii += 1) {
                pixels[ii] = thisBitmap.getPixel(Math.round(ii*horizontalStrideUnit), yy);
            }


            // Generate the colour palette (with high sensitivity for colour differences)
            HashMap <String, Integer> thisColourPalette = colourPalette(Args(A("sample", pixels), A("threshold", 0.05)));

            // Determine the dominant colour
            String dominantColourHex = Collections.max(thisColourPalette.entrySet(), Map.Entry.comparingByValue()).getKey();

            int dominantColourInt = Color.parseColor(dominantColourHex);

            // Determine the frequency of the dominant colour
            int frequencyOfDominantColour = thisColourPalette.get(dominantColourHex);

            // Determine the frequency of the dominant colour as a percentage of all frequencies of all colours
            Double frequencyOfDominantColourPercentage = frequencyOfDominantColour / (double) nPixels;

            // Determine the spread of the colo
            int spreadOfDominantColourStart = 0;
            int spreadOfDominantColourEnd = 0;
            boolean indexing = false;
            for (int jj = 0; jj < nPixels; jj += 1) {
                if (pixelDifferencePercentage(pixels[jj], dominantColourInt) < 0.2) {
                    if (!indexing) {
                        indexing = true;
                        spreadOfDominantColourStart = jj;
                    } else {
                        spreadOfDominantColourEnd = jj;
                    }
                }
            }

            double maxPixelAdjacencyDifference = 0.0;
            for (int jj = 0; jj < nPixels; jj += 1) {
                double thisPixelAdjacencyDifference = pixelDifferencePercentage(pixels[jj], dominantColourInt);
                if (thisPixelAdjacencyDifference > maxPixelAdjacencyDifference) {
                    maxPixelAdjacencyDifference = thisPixelAdjacencyDifference;
                }
            }

            int spreadOfDominantColour = Math.abs(spreadOfDominantColourStart - spreadOfDominantColourEnd);

            double spreadOfDominantColourPercentage = spreadOfDominantColour/ (double) nPixels;

            double thisDominantColourDifferenceToWhitespacePixelPercentage = pixelDifferencePercentage(whitespaceColour, dominantColourInt);

            List<Integer> pixelsAsList = Arrays.stream(pixels).boxed().collect(Collectors.toList());
            List<Integer> pixelsRAsList = Arrays.stream(pixels).map(Color::red).boxed().collect(Collectors.toList());
            List<Integer> pixelsGAsList = Arrays.stream(pixels).map(Color::green).boxed().collect(Collectors.toList());
            List<Integer> pixelsBAsList = Arrays.stream(pixels).map(Color::blue).boxed().collect(Collectors.toList());

            int thisAverageColour = averageColours(Args(A("colors", pixelsAsList)));
            int thisAverageColourR = averageColours(Args(A("colors", pixelsRAsList)));
            int thisAverageColourG = averageColours(Args(A("colors", pixelsGAsList)));
            int thisAverageColourB = averageColours(Args(A("colors", pixelsBAsList)));

            int thisAverageColourOnEdge = averageColours(Args(A("colors", IntStream.range(0, 2).map(x -> pixelsAsList.get(x)).boxed().collect(Collectors.toList()))));

            double thisAverageColourDifferenceToWhitespacePixelPercentage = pixelDifferencePercentage(whitespaceColour, thisAverageColour);
            double thisAverageColourRDifferenceToWhitespacePixelRPercentage = pixelDifferencePercentage(whitespaceColourR, thisAverageColourR);
            double thisAverageColourGDifferenceToWhitespacePixelGPercentage = pixelDifferencePercentage(whitespaceColourG, thisAverageColourG);
            double thisAverageColourBDifferenceToWhitespacePixelBPercentage = pixelDifferencePercentage(whitespaceColourB, thisAverageColourB);

            double thisAverageColourOnEdgeDifference = pixelDifferencePercentage(whitespaceColour, thisAverageColourOnEdge);

            if (verbose) {
                System.out.println("Pixel " + yy);
                //System.out.println("\t\tpixels: "+Arrays.stream(pixels).boxed().map(Visual::colourToHex).collect(Collectors.toList()));
                //System.out.println("\t\tthisColourPalette: " + thisColourPalette);
                //System.out.println("\t\tfrequencyOfDominantColourPercentage: " + frequencyOfDominantColourPercentage);
                //System.out.println("\t\tspreadOfDominantColourPercentage: " + spreadOfDominantColourPercentage);
                System.out.println("\t\tdominantColourHex: " + dominantColourHex);
                System.out.println("\t\t\tthisDominantColourDifferenceToWhitespacePixelPercentage: " + thisDominantColourDifferenceToWhitespacePixelPercentage);
                System.out.println("\t\tthisAverageColour: " + colourToHex(thisAverageColour));
                System.out.println("\t\t\tthisAverageColourDifferenceToWhitespacePixelPercentage: " + thisAverageColourDifferenceToWhitespacePixelPercentage);
                if (yy == 290) {
                    System.out.println(pixelsAsList.stream().map(x -> colourToHex(x)).collect(Collectors.toList()));
                }
                //System.out.println("\t\t\tthisAverageColourRDifferenceToWhitespacePixelRPercentage: " + thisAverageColourRDifferenceToWhitespacePixelRPercentage);
                //System.out.println("\t\t\tthisAverageColourGDifferenceToWhitespacePixelGPercentage: " + thisAverageColourGDifferenceToWhitespacePixelGPercentage);
                //System.out.println("\t\t\tthisAverageColourBDifferenceToWhitespacePixelBPercentage: " + thisAverageColourBDifferenceToWhitespacePixelBPercentage);
                //System.out.println("\t\tmaxPixelAdjacencyDifference: " + maxPixelAdjacencyDifference);
                if ((spreadOfDominantColourPercentage >= 0.90) && (frequencyOfDominantColourPercentage > 0.6) && (thisDominantColourDifferenceToWhitespacePixelPercentage > 0.05)) {
                    System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                }
            }

            ArraySpreadOfDominantColourPercentage.put(yy, spreadOfDominantColourPercentage);
            ArrayFrequencyOfDominantColourPercentage.put(yy, frequencyOfDominantColourPercentage);
            ArrayThisDominantColourDifferenceToWhitespacePixelPercentage.put(yy, thisDominantColourDifferenceToWhitespacePixelPercentage);
            ArrayThisAverageColourDifferenceToWhitespacePixelPercentage.put(yy, thisAverageColourDifferenceToWhitespacePixelPercentage);

            ArrayThisAverageColourRDifferenceToWhitespacePixelRPercentage.put(yy, thisAverageColourRDifferenceToWhitespacePixelRPercentage);
            ArrayThisAverageColourGDifferenceToWhitespacePixelGPercentage.put(yy, thisAverageColourGDifferenceToWhitespacePixelGPercentage);
            ArrayThisAverageColourBDifferenceToWhitespacePixelBPercentage.put(yy, thisAverageColourBDifferenceToWhitespacePixelBPercentage);

            ArrayMaxPixelAdjacency.put(yy, maxPixelAdjacencyDifference);
            ArrayPixels.put(yy, thisAverageColourOnEdgeDifference);


            // The dominant colour needs to have at least 60% dominance

            // no more than 6 rows of similarity





            // isolate parts of the image that occupy no more than 3 consecutive entries
            // AND
            // are occupied by a dominant colour taht takes up at least 60% of the row
            // and is distributed across 95% of the space of the row
            // AND are preceded by whitespace dominated rows
            // AND are proceeded by whitespace dominanted rows

            /*
             *
             * is there a dominant colour? (one that takes up at least 50% of the total frequency of colours
             *
             * does said colour distribute consistently across the space of the row (must be seen in at least 95% of row
             *
             * then it is the dominant colour
             *
             *
             *
             * */
         /*
         int thisDominantColour =  dominantColourInPalette(thisColourPalette);
         double thisPixelDiffPercentage = pixelDifferencePercentage(thisDominantColour, whitespaceColour);
         verticalIndicesColours.put(yy, thisDominantColour);
         verticalIndicesWhitespaceDiff.put(yy, thisPixelDiffPercentage);
         System.out.println(yy + " : " + colourToHex(thisDominantColour) + " : " + thisPixelDiffPercentage);
         System.out.println(thisColourPalette);*/
        }

        JSONXObject statistics = new JSONXObject();
        statistics.set("verticalStrideUnit", verticalStrideUnit);
        statistics.set("ArrayThisAverageColourDifferenceToWhitespacePixelPercentage", ArrayThisAverageColourDifferenceToWhitespacePixelPercentage);
        statistics.set("ArrayThisAverageColourRDifferenceToWhitespacePixelRPercentage", ArrayThisAverageColourRDifferenceToWhitespacePixelRPercentage);
        statistics.set("ArrayThisAverageColourGDifferenceToWhitespacePixelGPercentage", ArrayThisAverageColourGDifferenceToWhitespacePixelGPercentage);
        statistics.set("ArrayThisAverageColourBDifferenceToWhitespacePixelBPercentage", ArrayThisAverageColourBDifferenceToWhitespacePixelBPercentage);
        statistics.set("ArrayThisDominantColourDifferenceToWhitespacePixelPercentage", ArrayThisDominantColourDifferenceToWhitespacePixelPercentage);
        statistics.set("ArraySpreadOfDominantColourPercentage", ArraySpreadOfDominantColourPercentage);
        statistics.set("ArrayFrequencyOfDominantColourPercentage", ArrayFrequencyOfDominantColourPercentage);
        statistics.set("ArrayMaxPixelAdjacency", ArrayMaxPixelAdjacency);
        statistics.set("ArrayPixels", ArrayPixels);


        return statistics.internalJSONObject;
    }


    /*
     *
     * This function accepts constituent offset chains, deriving from them frame snippets, which are
     * independent posts, separate to the frames in which they were observed.
     *
     * TODO - with the frame snippet alts, we can further improve the alignment of content
     * */
    public static JSONXObject offsetChainsToFrameSnippets(JSONObject offsetChainsContainer, JSONObject frameSampleMetadata,
                                                         File framesSampleTempDirectory, File thisFacebookSnippetDirectory,
                                                         JSONObject fitterFacebookAdHeader, HashMap<String, Object> pictogramsReference,
                                                         Boolean verbose, Integer determinedWSColor, String determinedExposureType) {
        String exitStatus = "INCONCLUSIVE";
        Boolean proceedOnThis = true;
        JSONXObject statistics = new JSONXObject();
        try {
            // Generate the frame snippets directory (if it doesn't exist)
            createDirectory(thisFacebookSnippetDirectory, true);

            Double elapsedTimeOffsetChainsToFrameSnippets = Long.valueOf(System.currentTimeMillis()).doubleValue();
            HashMap<Integer, HashMap<Integer, JSONObject>> frameSnippetIDsByOffsetChains = new HashMap<>();
            List<HashMap<Integer, Integer>> offsetChains = new ArrayList<>();
            try {
                offsetChains = (List<HashMap<Integer, Integer>>) offsetChainsContainer.get("offsetChains");
            } catch (Exception e) {
                exitStatus = "ERROR_AT_LOAD_OFFSET_CHAINS: "+exceptionWrite(e);
                proceedOnThis = false;
                logger.error(e);
            }

            Integer heightOfFrame = null;
            HashMap<Integer, JSONObject> signaturesMap = null;
            try {
                heightOfFrame = ((Integer) ((JSONObject) frameSampleMetadata.get("statistics")).get("h"));
                signaturesMap = (HashMap<Integer, JSONObject>) ((JSONObject) frameSampleMetadata.get("statistics")).get("signaturesMap");
            } catch (Exception e) {
                exitStatus = "ERROR_AT_LOAD_SIGNATURE_MAPS: "+exceptionWrite(e);
                proceedOnThis = false;
                logger.error(e);
            }

            // For each offset chain...
            Integer thisOffsetChainID = 0;
            for (HashMap<Integer, Integer> thisOffsetChain : offsetChains) {
                Boolean thisOffsetChainContainsAd = false;
                // Create the offset chain's respective directory
                File thisOffsetChainDirectory = new File(thisFacebookSnippetDirectory, "offsetChain-"+thisOffsetChainID);
                createDirectory(thisOffsetChainDirectory, true);
                HashMap<Integer, List<Integer>> thisDividerMap = new HashMap<>();
                List<Integer> orderedFrames = thisOffsetChain.keySet().stream().sorted().collect(Collectors.toList());

                // Frames are loaded in here to determine basic statistics, and where dividers reside therein
                for (Integer thisFrame : orderedFrames) {
                    // Load in the bitmap (temporarily)
                    File thisBitmapFile = new File(framesSampleTempDirectory, thisFrame + "." + fileFormat);
                    Log.i(TAG, "TENTATIVE BITMAP:");
                    Log.i(TAG, thisBitmapFile.getAbsolutePath());
                    Bitmap thisFrameBitmap = BitmapFactory.decodeFile(thisBitmapFile.getAbsolutePath());
                    // Calculate the general frame statistics
                    JSONObject frameStatistics;
                    try {
                        frameStatistics = generateScreenshotStatistics(thisFrameBitmap, false); // examined
                    } catch (Exception e) {
                        statistics.set("proceedOnThis", false);
                        statistics.set("exitStatus", "MISSING_FRAME");
                        return statistics;
                    }
                    // Find the dividers
                    thisDividerMap.put(thisFrame, findDividersInScreenshot(thisFrameBitmap, frameStatistics, false)); // examined
                }

                // Write thisDividerMap to file
                if (verbose) {
                    writeToJSON(new File(thisOffsetChainDirectory, "dividerMap.json"), thisDividerMap);
                }

                // Given thisOffsetChain and thisDividerMap, thisSuperOffsetChain can be constructed.
                // This data-structure projects all dividers across all frames, in order to determine
                // determine trueDividers within the next step
                HashMap<Integer, List<Integer>> thisSuperOffsetChain = generateSuperOffsetChain(thisOffsetChain, thisDividerMap); // examined

                // The trueDividers (i.e., those that are certain to not have been generated out of error) are derived
                HashMap<Integer, List<Integer>> trueDividers = trueDividersFromSuperOffsetChain(thisSuperOffsetChain, heightOfFrame); // examined

                List<JSONObject> frameSnippets;
                // At this stage, the boundaries of the frameSnippets can be generated (and without having to load any images into immediate memory)
                JSONXObject frameSnippetsFromTrueDividersResult = frameSnippetsFromTrueDividers(trueDividers, thisOffsetChain, heightOfFrame, signaturesMap, determinedExposureType); // examined
                if (!((Boolean) frameSnippetsFromTrueDividersResult.get("success"))) {
                    statistics.set("proceedOnThis", false);
                    statistics.set("exitStatus", frameSnippetsFromTrueDividersResult.get("exitStatus"));
                    return statistics;
                } else {
                    frameSnippets = (List<JSONObject>) frameSnippetsFromTrueDividersResult.get("frameSnippetsCollapsed");
                }

                // Then the image content for the frame snippets are determined, and the necessary croppings are undertaken
                Integer thisFrameSnippetID = 0;
                HashMap<Integer, JSONObject> frameSnippetIDs = new HashMap<>();
                for (JSONObject thisFrameSnippet : frameSnippets) {
                    // Create the directory to contain the content of the frame snippet
                    File thisFrameSnippetDirectory = new File(thisOffsetChainDirectory, "frameSnippet-"+thisFrameSnippetID);
                    createDirectory(thisFrameSnippetDirectory, true);
                    // Generate the frame snippet's croppings, along with its respective metadata
                    JSONXObject snippetCroppingResultsContainer = new JSONXObject();
                    snippetCroppingResultsContainer = generateSnippetCroppings(thisFrameSnippetDirectory, framesSampleTempDirectory,
                            thisFrameSnippet, fitterFacebookAdHeader, pictogramsReference, thisFrameSnippetID, verbose, determinedWSColor, determinedExposureType); // examined
                    if (!((Boolean) snippetCroppingResultsContainer.get("success"))) {
                        statistics.set("proceedOnThis", false);
                        statistics.set("exitStatus", snippetCroppingResultsContainer.get("exitStatus"));
                        return statistics;
                    }
                    JSONObject snippetCroppingResults = (JSONObject) snippetCroppingResultsContainer.get("statistics");

                    writeToJSON(new File(thisFrameSnippetDirectory, "metadata.json"), snippetCroppingResults);
                    frameSnippetIDs.put(thisFrameSnippetID, snippetCroppingResults);
                    thisFrameSnippetID ++;
                    try {
                        // If this frame snippet does not contain an ad (and we are not in 'verbose' mode), remove its storage folder
                        if (!(Boolean) snippetCroppingResults.get("determinedAsFacebookAd")) {
                            if (!verbose) {
                                deleteRecursive(thisFrameSnippetDirectory);
                            }
                        } else {
                            // Flag the offset chain as containing an ad
                            thisOffsetChainContainsAd = true;
                        }
                    } catch (Exception e) {
                        logger.error(e);
                        e.printStackTrace();
                        statistics.set("proceedOnThis", false);
                        statistics.set("exitStatus", "ERROR_AT_CLEANUP");
                        return statistics;
                    }
                }
                frameSnippetIDsByOffsetChains.put(thisOffsetChainID, frameSnippetIDs);

                // If the offset chain does not contain any ads (and we are not in 'verbose' mode), remove its storage folder
                if ((!thisOffsetChainContainsAd) && (!verbose)) {
                    deleteRecursive(thisOffsetChainDirectory);
                }

                thisOffsetChainID ++;
            }
            statistics.set("frameSnippetIDsByOffsetChains", frameSnippetIDsByOffsetChains);
            statistics.set("elapsedTimeOffsetChainsToFrameSnippets", Math.abs(System.currentTimeMillis() - elapsedTimeOffsetChainsToFrameSnippets));
        } catch (Exception eX) {
            // examined
            logger.error(eX);
            proceedOnThis = false;
            exitStatus = "ENCOUNTERED_ERROR: "+exceptionWrite(eX);
        }

        if (exitStatus.equals("INCONCLUSIVE")) {
            exitStatus = "SUCCESS";
        }
        statistics.set("proceedOnThis", proceedOnThis);
        statistics.set("exitStatus", exitStatus);
        return statistics;
    }

    public static void offsetChainsToFrameSnippetsRollback(File rootDirectory, HashMap<String, String> thisInterpretation) {
        Log.i(TAG, "PERFORMING ROLLBACK!");
        // Delete the serialXObject's record
        serialObjectDelete(rootDirectory, thisInterpretation, "frameSnippetIDsByOffsetChain");
        // Delete the tangible files that were generated in this step
        File tempFacebookFrameSnippetsMasterDirectory = new File(rootDirectory, "tempFacebookFrameSnippets");
        createDirectory(tempFacebookFrameSnippetsMasterDirectory, false);
        File tempFacebookFrameSnippetsDirectory = new File(tempFacebookFrameSnippetsMasterDirectory, thisInterpretation.get("filename"));
        deleteRecursive(tempFacebookFrameSnippetsDirectory);
    }

    public static void facebookComprehensiveReadingOffsetChainsRollback(File rootDirectory, HashMap<String, String> thisInterpretation) {
        Log.i(TAG, "PERFORMING ROLLBACK!");
        // Delete the serialXObject's record
        serialObjectDelete(rootDirectory, thisInterpretation, "offsetChains");
        serialObjectDelete(rootDirectory, thisInterpretation, "frameSampleMetadata");
        // Delete the tangible files that were generated in this step
        File tempFacebookComprehensiveSampleMasterDirectory = new File(rootDirectory, "tempFacebookComprehensiveSample");
        createDirectory(tempFacebookComprehensiveSampleMasterDirectory, false);
        File tempFacebookComprehensiveSampleDirectory = new File(tempFacebookComprehensiveSampleMasterDirectory, thisInterpretation.get("filename"));
        deleteRecursive(tempFacebookComprehensiveSampleDirectory);
    }





    /*
     *
     * This function instantiates the HashMap that contains the references for all pictograms and
     * stencils used in the Facebook ad interpreter software logic
     *
     * */
    public static HashMap<String, Integer> DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_INDEX =
            new HashMap<String, Integer>() {{
                // Stencils
                put("facebookDarkHomeActive", R.drawable.facebook_dark_home_active);
                put("facebookDarkHomeInactive", R.drawable.facebook_dark_home_inactive);
                put("facebookDarkWatchActive", R.drawable.facebook_dark_watch_active);
                put("facebookDarkWatchInactive", R.drawable.facebook_dark_watch_inactive);
                put("facebookLightHomeActive", R.drawable.facebook_light_home_active);
                put("facebookLightHomeInactive", R.drawable.facebook_light_home_inactive);
                put("facebookLightWatchActive", R.drawable.facebook_light_watch_active);
                put("facebookLightWatchInactive", R.drawable.facebook_light_watch_inactive);
                put("facebookLightSponsored", R.drawable.facebook_light_sponsored);
                put("facebookDarkSponsored", R.drawable.facebook_dark_sponsored);
                put("facebookLightSponsoredAlt", R.drawable.facebook_light_sponsored_alt);
                put("facebookDarkSponsoredAlt", R.drawable.facebook_dark_sponsored_alt);
                // Pictograms
                put("facebookReactLike", R.drawable.facebook_react_like);
                put("facebookReactLove", R.drawable.facebook_react_love);
                put("facebookReactCare", R.drawable.facebook_react_care);
                put("facebookReactLaugh", R.drawable.facebook_react_laugh);
                put("facebookReactWow", R.drawable.facebook_react_wow);
                put("facebookReactSad", R.drawable.facebook_react_sad);
                put("facebookReactHate", R.drawable.facebook_react_hate);
                put("facebookReactMask", R.drawable.facebook_react_mask);
            }};
    // By default, the exclusion is applied to the Facebook navbar buttons, where it overtakes the part
    // of each icon that occasionally changes to display notifications
    public static int DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_EXCLUSION_RADIUS = 32;
    public static HashMap<String, Integer>
            DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_EXCLUSION =
            new HashMap<String, Integer>() {{
                put("x",DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_EXCLUSION_RADIUS);
                put("y",0);
                put("h",DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_EXCLUSION_RADIUS);
                put("w",DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_EXCLUSION_RADIUS);
            }};
    public static int DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_SIZE_RADIUS = 64;
    public static HashMap <String,Integer>
            DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_SIZE =
            new HashMap<String, Integer>() {{
                put("s", DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_SIZE_RADIUS);
            }};
    public static int DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_SIZE_WIDTH = 128;
    public static int DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_SIZE_HEIGHT = 64;
    public static HashMap <String,Integer>
            DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_SIZE_SPONSORED =
            new HashMap<String, Integer>() {{
                put("w", DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_SIZE_WIDTH);
                put("h", DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_SIZE_HEIGHT);
            }};

    // image manipulation


    @SuppressLint("UseCompatLoadingForDrawables")
    public static HashMap<String, Object> retrieveReferenceStencilsPictograms(Context context) {
        HashMap<String, Bitmap> pictogramReferenceHashMap = new HashMap<>();
        // Apply the drawable references to a hashmap that loads in the corresponding resources
        for (HashMap.Entry<String, Integer> e
                : DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_INDEX.entrySet()) {
            pictogramReferenceHashMap.put(e.getKey(),
                    ((BitmapDrawable)context.getResources().getDrawable(e.getValue())).getBitmap());
        }
        // Assemble the references
        HashMap<String, Object> reference = new HashMap<>();
        for (String key : pictogramReferenceHashMap.keySet()) {
            Bitmap thisPictogram = pictogramReferenceHashMap.get(key);
            HashMap <String,Integer> exclusion = DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_EXCLUSION;
            HashMap <String,Integer> size = DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_SIZE;
            // Sizing specific to Facebook 'sponsored' text case...
            if (key.contains("facebook") && key.contains("Sponsored")) {
                exclusion = null;
                size = DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_SIZE_SPONSORED;
            }
            // Sizing specific to Facebook 'react' button case...
            if (key.contains("facebookReact")) {
                reference.put(key,imageToPictogram(Args(A("bitmap", thisPictogram))));
            } else {
                reference.put(key,imageToStencil(Args(
                        A("bitmap", thisPictogram),
                        A("size", size),
                        A("exclusion", exclusion))));
            }
        }
        return reference;
    }


    /*
     *
     * This function accepts data about frame snippets that have been identified as ads, and prepares them
     * for upload.
     *
     * In practise, it is expected that the user may not always have internet. Thus, if the ad is made ephemeral,
     * it would be lost between sessions. To overcome this, the ads are stored in a holding directory,
     * where they are routinely uploaded when there is capability to do so.
     *
     *
     * // TODO - make logic for deleting ad contents after overfilling and no internet
     *
     * */
    public static String prepareAdContentForUpload(JSONObject analysisData, File adHoldingDirectory, File facebookSnippetDirectory) {
        String exitStatus = "INCONCLUSIVE";
        HashMap<Integer, HashMap<Integer, JSONObject>> frameSnippetIDsByOffsetChain = new HashMap<>();
        try {
            frameSnippetIDsByOffsetChain = ((HashMap<Integer, HashMap<Integer, JSONObject>>) ((JSONObject)
                    analysisData.get("frameSnippetIDsByOffsetChain")).get("frameSnippetIDsByOffsetChains"));
        } catch (Exception e) {
            exitStatus = "ERROR_AT_LOAD_FRAME_SNIPPETS: "+exceptionWrite(e);
            logger.error(e);
        }

        // For each frame snippet...
        Integer nAds = 0;
        for (Integer offsetChainID : frameSnippetIDsByOffsetChain.keySet()) {
            for (Integer frameSnippetID : frameSnippetIDsByOffsetChain.get(offsetChainID).keySet()) {
                JSONObject frameSnippet = frameSnippetIDsByOffsetChain.get(offsetChainID).get(frameSnippetID);
                // Determine whether the frame snippet is an ad (or not)
                Boolean determinedAsFacebookAd = false;
                // Occassionally, an empty frame snippet may be reached, which can derail the entire preparation ad upload process
                // and put the software in a lock - to get around this, we firstly observe that the frameSnippet is even workable before
                // proceeding - otherwise, if its an empty frame snippet, just overlook it.
                if (frameSnippet.has("determinedAsFacebookAd")) {
                    List<String> generatedCroppingFiles = new ArrayList<>();
                    try {
                        determinedAsFacebookAd = (Boolean) frameSnippet.get("determinedAsFacebookAd");
                        generatedCroppingFiles = (List<String>) frameSnippet.get("generatedCroppingFiles");
                    } catch (Exception e) {
                        exitStatus = "ERROR_AT_DETERMINATION";
                        logger.error(e);
                    }
                    if (determinedAsFacebookAd) {
                        nAds ++;
                        // Prepare the ad content for this frame snippet
                        String uuidForAdContent = System.currentTimeMillis()  + "." + UUID.randomUUID().toString();
                        // Create a folder for the ad within the holding folder
                        File thisAdDirectory = new File(adHoldingDirectory, uuidForAdContent);
                        createDirectory(thisAdDirectory, true);
                        File thisOffsetChainDirectory = new File(facebookSnippetDirectory, "offsetChain-" + offsetChainID);
                        File thisFrameSnippetDirectory = new File(thisOffsetChainDirectory, "frameSnippet-" + frameSnippetID);
                        // Copy across the images from the frame snippet
                        for (String generatedCroppingFile : generatedCroppingFiles) {
                            File fileFrom = new File(thisFrameSnippetDirectory, generatedCroppingFile);
                            File fileTo = new File(thisAdDirectory, generatedCroppingFile);
                            try {
                                Files.copy(Paths.get(fileFrom.getAbsolutePath()), Paths.get(fileTo.getAbsolutePath()));
                            } catch (Exception e) {
                                exitStatus = "ERROR_AT_COPYING";
                                logger.error(e);}
                        }
                        // Write the analysis data for this ad to file
                        try {
                            analysisData.put("thisAdOffsetChainID",offsetChainID);
                            analysisData.put("thisAdFrameSnippetID",frameSnippetID);
                        } catch (Exception e) {
                            exitStatus = "ERROR_AT_ANALYSIS";
                            logger.error(e);}
                        if (exitStatus.equals("INCONCLUSIVE")) {
                            writeToJSON(new File(thisAdDirectory, "adContent.json"), analysisData);
                        }
                    }
                }

            }
        }
        if (exitStatus.equals("INCONCLUSIVE")) {
            exitStatus = "SUCCESSFUL WITH N ADS: "+nAds.toString();
        }
        return exitStatus;
    }

    public static JSONObject serialObjectPassthrough(Supplier<JSONObject> supplier, File rootDirectory, String identifier, String subIdentifier) {
        File serializationDirectory = new File(rootDirectory, "serializations");
        createDirectory(serializationDirectory, false);
        File thisSerializationDirectory = new File(serializationDirectory, identifier);
        serialXObject.setTargetDirectory(thisSerializationDirectory);
        createDirectory(thisSerializationDirectory, false);
        serialXObject outputSerialized = new serialXObject(subIdentifier);
        JSONObject output;
        if (!outputSerialized.container.has("DATA")) {
            output = supplier.get();
            try {
                if ((Boolean) output.get("proceedOnThis")) {
                    outputSerialized.set("DATA", output);
                    outputSerialized.save();
                    Log.i(TAG, "##########################################");
                    Log.i(TAG, "SAVED DATA");
                    Log.i(TAG, "##########################################");
                } else {
                    Log.i(TAG, "##########################################");
                    Log.i(TAG, "COULD NOT SAVE DATA");
                    Log.i(TAG, "##########################################");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            output = (JSONObject) outputSerialized.container.get("DATA");
            Log.i(TAG, "##########################################");
            Log.i(TAG, "LOADING PERSISTED DATA");
            Log.i(TAG, "##########################################");
            try { output.put("exitStatus", "LOADED_FROM_PERSISTED_DATA"); } catch (Exception e) {}
        }
        return output;
    }

    public static void serialObjectDelete(File rootDirectory, HashMap<String, String> thisInterpretation, String subidentifier) {
        File serializationDirectory = new File(rootDirectory, "serializations");
        createDirectory(serializationDirectory, false);
        File thisSerializationDirectory = new File(serializationDirectory,thisInterpretation.get("filename"));
        serialXObject.setTargetDirectory(thisSerializationDirectory);
        Log.i(TAG, "SERIAL OBJECT DIR");
        Log.i(TAG, thisSerializationDirectory.getAbsolutePath());
        createDirectory(thisSerializationDirectory, false);
        serialXObject outputSerialized = new serialXObject(subidentifier);
        Log.i(TAG, thisInterpretation.get("filename"));
        outputSerialized.delete();
    }

    public static void facebookInterpretation(Context context, File appStorageRecordingsDirectory, HashMap<String, String> thisInterpretation, File rootDirectory, Function<JSONXObject, JSONXObject> getVideoMetadataFunction,
                                              Function<JSONXObject, Bitmap> frameGrabFunction, Boolean implementedOnAndroid, File adsFromDispatchDirectory, JSONObject fitterFacebookAdHeader, HashMap<String, Object> pictogramsReference) {
        Boolean proceedable = true;
        JSONXObject highLevelOutcome = new JSONXObject();
        // Determine the exposure mode of the screen recording
        File screenRecordingFile = new File(appStorageRecordingsDirectory, thisInterpretation.get("filename"));
        Integer thisScreenRecordingTimestamp = Integer.parseInt(thisInterpretation.get("timestamp"));

        // Create two temp folders that are necessary for the analysis to take place
        // The tempFacebookComprehensiveSampleDirectory is necessary to hold raw screenshots obtained from sampling the screen recording
        // NOTE: It will be created by the facebookComprehensiveFramesSample method
        File tempFacebookComprehensiveSampleDirectoryMaster = new File(rootDirectory, "tempFacebookComprehensiveSample");

        // Create the master temp folder if it doesn't exist
        createDirectory(tempFacebookComprehensiveSampleDirectoryMaster, false);

        // Create the directory contextualised to this analysis
        File tempFacebookComprehensiveSampleDirectory = new File(tempFacebookComprehensiveSampleDirectoryMaster, thisInterpretation.get("filename"));




        // The tempFacebookFrameSnippetsDirectory is necessary to hold temporary frame snippet data
        File tempFacebookFrameSnippetsMasterDirectory = new File(rootDirectory, "tempFacebookFrameSnippets");
        createDirectory(tempFacebookFrameSnippetsMasterDirectory, false);
        File tempFacebookFrameSnippetsDirectory = new File(tempFacebookFrameSnippetsMasterDirectory, thisInterpretation.get("filename"));



        // NOTE: Prior to running the analysis, there is a possibility that these directories exist, in which case, they need to be deleted
        // TODO
        //facebookAnalysisCleanup(tempFacebookComprehensiveSampleDirectory, tempFacebookFrameSnippetsDirectory);

        // Run the comprehensive sampling process
        JSONObject frameSampleMetadata = serialObjectPassthrough(
                (() -> facebookComprehensiveReading(context, tempFacebookComprehensiveSampleDirectory, screenRecordingFile, getVideoMetadataFunction, frameGrabFunction)),
                rootDirectory, thisInterpretation.get("filename"), "frameSampleMetadata");






        try {
            highLevelOutcome.set("facebookComprehensiveReading", frameSampleMetadata.get("exitStatus"));
            if (!((Boolean) frameSampleMetadata.get("proceedOnThis"))) {
                proceedable = false;
            }
        } catch (Exception e) {
            proceedable = false;
        }

        Integer wsColor = null;
        String denotedMode = null;
        try {
            wsColor = (Integer) frameSampleMetadata.get("wsColor");
            denotedMode = (String) frameSampleMetadata.get("denotedMode");
        } catch (Exception e) {
            proceedable = false;
            logger.error(e);
        }

        JSONObject offsetChains = new JSONObject();
        JSONObject frameSnippetIDsByOffsetChain = new JSONObject();
        JSONObject analysisData = new JSONObject();

        // If the comprehensive sampling process generated the necessary folder
        if ((proceedable) && (tempFacebookComprehensiveSampleDirectory.exists())) {
            // Generate the 'sub-' offset chains for the master offset chain within the frameSampleMetadata

            offsetChains = serialObjectPassthrough(
                    (() -> masterOffsetChainToSubOffsetChains(frameSampleMetadata)),
                    rootDirectory, thisInterpretation.get("filename"), "offsetChains");


            try {
                highLevelOutcome.set("masterOffsetChainToSubOffsetChains", offsetChains.get("exitStatus"));
                if (!((Boolean) offsetChains.get("proceedOnThis"))) {
                    proceedable = false;
                }
            } catch (Exception e) {
                proceedable = false;
            }

            // Generate the frame snippets from the offsetChains

            JSONObject finalOffsetChains = offsetChains;
            Integer finalWsColor = wsColor;
            String finalDenotedMode = denotedMode;
            frameSnippetIDsByOffsetChain = serialObjectPassthrough(
                    (() -> offsetChainsToFrameSnippets(
                            finalOffsetChains, frameSampleMetadata, tempFacebookComprehensiveSampleDirectory, tempFacebookFrameSnippetsDirectory,
                            fitterFacebookAdHeader, pictogramsReference, DEBUG, finalWsColor, finalDenotedMode).internalJSONObject),
                    rootDirectory, thisInterpretation.get("filename"), "frameSnippetIDsByOffsetChain"); // This has been examined for errors


            try {
                highLevelOutcome.set("offsetChainsToFrameSnippets", frameSnippetIDsByOffsetChain.get("exitStatus"));
                if (!((Boolean) frameSnippetIDsByOffsetChain.get("proceedOnThis"))) {
                    proceedable = false;
                }
            } catch (Exception e) {
                proceedable = false;
            }

            try {
                if (!((Boolean) frameSnippetIDsByOffsetChain.get("proceedOnThis"))) {
                    offsetChainsToFrameSnippetsRollback(rootDirectory, thisInterpretation);
                }

                // A frame signature adding error
                if (((String) frameSnippetIDsByOffsetChain.get("exitStatus")).contains("ERROR_AT_FRAME_SNIPPETS_FRAME_SIGNATURES_ADD")) {
                    facebookComprehensiveReadingOffsetChainsRollback(rootDirectory, thisInterpretation);
                }

                // A missing frame error
                if (((String) frameSnippetIDsByOffsetChain.get("exitStatus")).contains("MISSING_FRAME")) {
                    facebookComprehensiveReadingOffsetChainsRollback(rootDirectory, thisInterpretation);
                }
            } catch (Exception e) {}

            // Isolate ad content
            try {
                analysisData.put("timestampOfDerivingScreenRecording", thisScreenRecordingTimestamp);
                analysisData.put("frameSampleMetadata", frameSampleMetadata);
                analysisData.put("offsetChains", offsetChains);
                analysisData.put("frameSnippetIDsByOffsetChain", frameSnippetIDsByOffsetChain);
            } catch (Exception e) {
                logger.error(e);}
            // Prepare the ad content for upload (if it exists)
            String adContentResult = "INCONCLUSIVE";
            if (proceedable) {
                adContentResult = prepareAdContentForUpload(analysisData, adsFromDispatchDirectory, tempFacebookFrameSnippetsDirectory);
            }
            try {
                if (!adContentResult.contains("SUCCESSFUL")) {
                    proceedable = false;
                }

                if (adContentResult.equals("ERROR_AT_COPYING")) {
                    // Occassionally the frame snippets 'creation process' does not create all the files needed in the next steps - this
                    // would require a rollback on the files from the previous step

                    // Perform rollback
                    offsetChainsToFrameSnippetsRollback(rootDirectory, thisInterpretation);

                }
                highLevelOutcome.set("prepareAdContentForUpload", adContentResult);
            } catch (Exception e) {
                proceedable = false;
            }

        } else {
            proceedable = false; // reasserting in case folder does not exist but proceedable
            // There is no guarantee that the facebookComprehensiveFramesSample method will succeed in creating the tempFacebookComprehensiveSample
            // directory. To overcome this, we need a logic here.
            // TODO
        }
        if (!implementedOnAndroid) {
            JSONObject classificationAnalysis = new JSONObject();
            try {
                classificationAnalysis.put("interpretation", thisInterpretation);
                classificationAnalysis.put("facebookSampleOutcome", frameSampleMetadata);
                classificationAnalysis.put("offsetChains", offsetChains);
                classificationAnalysis.put("frameSnippetIDsByOffsetChain", frameSnippetIDsByOffsetChain);
                classificationAnalysis.put("analysisData", analysisData);
            } catch (Exception e) {
                logger.error(e);}
        }

        if (implementedOnAndroid) {
            // Finally delete the recording

            File logsDirectory = new File(rootDirectory, "logs");
            createDirectory(logsDirectory, false);
            try {
                highLevelOutcome.set("proceedable", proceedable);
            } catch (Exception e) {}
            writeToJSON((new File(logsDirectory, screenRecordingFile.getName()+"."+UUID.randomUUID().toString()+".json")), highLevelOutcome.internalJSONObject);

            if ((proceedable)
                    && (highLevelOutcome.has("facebookComprehensiveReading"))
                    && (highLevelOutcome.has("masterOffsetChainToSubOffsetChains"))
                    && (highLevelOutcome.has("offsetChainsToFrameSnippets"))
                    && (highLevelOutcome.has("prepareAdContentForUpload"))) {
                // TODO - the serializations need to be cleaned up after execution (AS WELL)
                facebookAnalysisCleanup(tempFacebookComprehensiveSampleDirectory, tempFacebookFrameSnippetsDirectory);
                screenRecordingFile.delete();
            }
            // Clean up
        }
    }

    /*
    *
    * Cleanup
    *
    * */
    public static void facebookAnalysisCleanup(File comprehensiveSampleDirectory, File frameSnippetsDirectory) {
        deleteRecursive(comprehensiveSampleDirectory);
        deleteRecursive(frameSnippetsDirectory);
    }
}
