package com.adms.australianmobileadtoolkit.interpreter;

import static com.adms.australianmobileadtoolkit.Arguments.A;
import static com.adms.australianmobileadtoolkit.Arguments.Args;
import static com.adms.australianmobileadtoolkit.Common.BooleanIndexOfN;
import static com.adms.australianmobileadtoolkit.Common.binAsAverages;
import static com.adms.australianmobileadtoolkit.Common.filePath;
import static com.adms.australianmobileadtoolkit.Common.makeDirectory;
import static com.adms.australianmobileadtoolkit.Common.weightedHashMap;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.colourQuantizeBitmap;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.combineImagesList;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.cropWhitespace;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.dividerBoundOffsets;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.dividerWhitespaceAlternations;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.getWhitespacePixel;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.imageToPictogram;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.imageToStencil;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.isWhitespace;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.isWhitespaceLocalDifferencesSubFunction;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.pictogramSimilarity;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.pictogramSimilarityV2;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.pixelDifference;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.pixelsAtAxisI;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.stencilSimilarity;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.stencilToString;
import static com.adms.australianmobileadtoolkit.Settings.DEBUG;
import static java.util.Arrays.asList;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.adms.australianmobileadtoolkit.MainActivity;
import com.adms.australianmobileadtoolkit.interpreter.platform.FacebookScreenshot;
import com.adms.australianmobileadtoolkit.interpreter.visual.DividerSet;
import com.adms.australianmobileadtoolkit.interpreter.visual.Stencil;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Depreciated {
   private static final String TAG = "Depreciated";

   private static Context thisMasterContext;










   /*
    *
    * segments_timestamp_separated
    *
    * */
   public static List<List<JSONObject>> segmentTimestampSeparated(List<String> filenames, String sourceFolderName) throws JSONException {
      int maximumSegmentFrameInterval = 1000*10;
      int maximumNScreenshotsPerSegment = 30;
      List<List<JSONObject>> segments = new ArrayList<>();
      long timestampCursor = 0;
      int targetSegmentI = 0;
      for (int i = 0; i < filenames.size(); i ++) {
         String fname = filenames.get(i);
         String[] fnameDecomposed = fname.split("\\.");
         long thisTimestamp =  Long.parseLong(fnameDecomposed[0]);
         String thisUUID = fnameDecomposed[1];

         if ((!((timestampCursor == 0) || (Math.abs(timestampCursor - thisTimestamp) < maximumSegmentFrameInterval)))
               || ((segments.size() > targetSegmentI) && (segments.get(targetSegmentI).size() >= maximumNScreenshotsPerSegment))) {
            targetSegmentI ++;
         }

         if ((targetSegmentI+1) > segments.size()) {
            segments.add(new ArrayList<>());
         }
         JSONObject thisEntry = new JSONObject();
         thisEntry.put("uuid", thisUUID);
         thisEntry.put("timestamp", thisTimestamp);
         thisEntry.put("fname", fname);
         segments.get(targetSegmentI).add(thisEntry);
         timestampCursor = thisTimestamp;
      }
      for (int i = 0; i < segments.size(); i ++) {
         segments.get(i).sort((a, b) -> {
            long valA = 0;
            long valB = 0;
            try {
               valA = (long) a.get("timestamp");
               valB = (long) b.get("timestamp");
            } catch (JSONException ignored) {
            }
            return ((valA >= valB) ? ((valA > valB) ? 1 : 0) : -1);
         });
      }
      segments.sort((a,b) -> ((a.size() >= b.size()) ? ((a.size() > b.size()) ? 1 : 0) : -1));
      return segments;
   }




   /*
    *
    * determine_consistent_bounds
    *
    * */
   public static JSONObject determineConsistentBounds(List<JSONObject> thisSegment, boolean leftAndRight) throws JSONException {
      int thisWidth = (int) ((HashMap<String, Integer>) thisSegment.get(0).get("dimensions")).get("width");
      int thisHeight = (int) ((HashMap<String, Integer>) thisSegment.get(0).get("dimensions")).get("height");
      JSONObject thisBounds = new JSONObject();
      thisBounds.put("t", 0);
      thisBounds.put("b", thisHeight-1);
      thisBounds.put("l", 0);
      thisBounds.put("r", thisWidth-1);
      if (thisSegment.stream().noneMatch(x -> {
         try {
            return (boolean) x.get("inFacebook");
         } catch (JSONException e) {
            throw new RuntimeException(e);
         }
      })) {
         return null;
      }
      JSONObject cropFromDict = new JSONObject(thisBounds.toString());
      if (thisSegment.size() == 1) {
         cropFromDict = thisBounds;
      } else {

         String[] qualifyingBounds = ((leftAndRight) ? (new String[]{"t", "b", "l", "r"}) : (new String[]{"t", "b"}));
         for (String location : qualifyingBounds) {
            int cropFrom = (int) thisBounds.get(location);
            boolean consistent = true;
            while ((consistent) && (
                  ((location.equals("t")) && cropFrom < thisHeight)
                        || ((location.equals("b")) && cropFrom >= 0)
                        || ((location.equals("l")) && cropFrom < thisWidth)
                        || ((location.equals("r")) && cropFrom >= 0))) {
               Bitmap thisImage = (Bitmap) thisSegment.get(0).get("image");
               int[] comparingArray = ((location.equals("t") || location.equals("b")) ?
                     pixelsAtAxisI(thisImage, "v", cropFrom) : pixelsAtAxisI(thisImage, "h", cropFrom));
               for (int i = 0; i < thisSegment.size(); i ++) {
                  JSONObject thisScreenshot = thisSegment.get(i);
                  if ((boolean) thisScreenshot.get("inFacebook")) {
                     Bitmap thisOtherImage = (Bitmap) thisScreenshot.get("image");
                     int[] thisArray = ((location.equals("t") || location.equals("b")) ?
                           pixelsAtAxisI(thisOtherImage, "v", cropFrom) : pixelsAtAxisI(thisOtherImage, "h", cropFrom));
                     int maxDifferencePixels = 30;

                     int[] differenceArray = new int[thisArray.length];
                     for (int j = 0; j < comparingArray.length; j ++) {
                        differenceArray[j] = Math.abs(pixelDifference(thisArray[j], comparingArray[j]));
                     }

                     Integer differenceArraySum = Arrays.stream(differenceArray).sum();

                     if (differenceArraySum > (comparingArray.length*maxDifferencePixels)) {
                        consistent = false;
                     }
                  }
               }
               if (consistent) {
                  cropFrom += ((location.equals("t") || location.equals("l")) ? 1 : -1);
               }
            }
            cropFromDict.put(location, cropFrom);
         }
      }
      Iterator<String> keys = thisBounds.keys();
      while(keys.hasNext()) {
         String bound = keys.next();
         cropFromDict.put(bound, Math.abs(((int) cropFromDict.get(bound)) - ((int) thisBounds.get(bound))));
      }
      return cropFromDict;
   }

   public static int cropExcludeDividers(List<Bitmap> thisDividers, JSONObject thisConsistentBounds, int absoluteHeight) throws JSONException {
      if (thisConsistentBounds == null) {
         return thisDividers.size()-1;
      }
      int cumulativeHeight = 0;
      int cutHeight = (absoluteHeight - ((int) thisConsistentBounds.get("b")));
      for (int i = 0; i < thisDividers.size(); i ++) {
         int thisHeight = thisDividers.get(i).getHeight();
         double pctOverlap = (Math.abs((cumulativeHeight + thisHeight)-cutHeight)/( (double) thisHeight));
         if (((cumulativeHeight + thisHeight) >= cutHeight) && (pctOverlap > 0.5)) {
            return i;
         }
         cumulativeHeight += thisHeight;
      }
      return thisDividers.size();
   }


   /*
    *
    * are_screenshots_linked_by_scroll
    *
    * */
   public static JSONObject areScreenshotsLinkedByScroll(List<JSONObject> screenshots, int maxDifferencePixels, double tolerance, int minMatches) throws JSONException {
      long elapsedTime = System.currentTimeMillis();
      List<JSONObject> matchingDividers = new ArrayList<>();

      List<Bitmap> dividersAllA = ((List<Bitmap>) screenshots.get(0).get("dividers"));
      List<Bitmap> dividersExcludedA = ((List<Bitmap>) screenshots.get(0).get("dividersExcludedTop"));
      List<Bitmap> dividersRetainedA = ((List<Bitmap>) screenshots.get(0).get("dividersRetained"));
      List<Bitmap> dividersExcludedB = ((List<Bitmap>) screenshots.get(1).get("dividersExcludedTop"));
      List<Bitmap> dividersRetainedB = ((List<Bitmap>) screenshots.get(1).get("dividersRetained"));

      HashMap<String, Double> matchMap = new HashMap<>();

      List<Integer> matchedI = new ArrayList<>();
      List<Integer> matchedJ = new ArrayList<>();



      Integer dividersExcludedAOffset = (int) dividersExcludedA.stream().map(x -> x.getHeight()).mapToDouble(x -> x).sum();
      Integer dividersExcludedBOffset = (int) dividersExcludedB.stream().map(x -> x.getHeight()).mapToDouble(x -> x).sum();
      for (int i = 0; i < dividersRetainedA.size(); i ++) {
         int AH = (dividersRetainedA.get(i).getHeight());

         for (int j = 0; j < dividersRetainedB.size(); j ++) {
            if ((!(((List<Double>) screenshots.get(0).get("whitespace")).get(i) < 0.1)) && (!(((List<Double>) screenshots.get(1).get("whitespace")).get(j) < 0.1))) {




               int BH = (dividersRetainedB.get(j).getHeight());
               List<Integer> RHOA = ((List<List<Integer>>) screenshots.get(0).get("dividersRetaineddividerBoundOffsets")).get(i);
               List<Integer> RHOB = ((List<List<Integer>>) screenshots.get(1).get("dividersRetaineddividerBoundOffsets")).get(j);
               double RHOSum = 0.0;
               for (int k = 0; k < RHOA.size(); k++) {
                  RHOSum += Math.abs(RHOA.get(k) - RHOB.get(k));
               }
               double differencePercentage = (RHOSum / ((Integer) ((HashMap<String, Integer>) screenshots.get(0).get("dimensions")).get("width")));
               if (differencePercentage <= 0.01) {
                  int oA = (int) dividersRetainedA.subList(0,i).stream().map(x->x.getHeight()).mapToDouble(x->x).sum();
                  int oB = (int) dividersRetainedB.subList(0,j).stream().map(x->x.getHeight()).mapToDouble(x->x).sum();
                  if ((Math.abs(AH - BH) < maxDifferencePixels)) {
                     double matchResult = pictogramSimilarity(((List<Bitmap>) screenshots.get(0).get("pictograms")).get(i)
                           ,((List<Bitmap>) screenshots.get(1).get("pictograms")).get(j), null);

                     //Log.i(TAG, "\t\tavg: "+totalDifferenceArray.stream().mapToDouble(x->x).average());
                     Log.i(TAG, "\tmatched: "+i+" <-> "+j+" : "+matchResult+" : "+differencePercentage);
                     matchMap.put(i+":"+j,matchResult);
                     if (matchResult > (1.0 - tolerance)) {
                        Log.i(TAG, "\t\tmatched success: "+i+" <-> "+j+" : "+matchResult+" w/ offset:"+(oA - oB));
                        JSONObject thisMatchingDivider = new JSONObject();
                        matchedI.add(i);
                        matchedJ.add(j);
                        thisMatchingDivider.put("indexA", i);
                        thisMatchingDivider.put("indexB", j);
                        thisMatchingDivider.put("offset", (oA - oB));
                        matchingDividers.add(thisMatchingDivider);
                     }
                  }
               }
            }
         }
      }

      HashMap<Integer, Integer> weightedOffsets = weightedHashMap(
            matchingDividers.stream().map(x -> {
               try {
                  return ((Integer) x.get("offset"));
               } catch (JSONException e) {
                  throw new RuntimeException(e);
               }
            }).collect(Collectors.toList()));
      List<Double> inputList = (List<Double>) weightedOffsets.keySet().stream().map(Integer::doubleValue).collect(Collectors.toList());

      HashMap<Double, List<Double>> groups = binAsAverages(Args(
                                 A("input", inputList), A("likeness", (double) maxDifferencePixels)));
      List<Double> offsetsReducedListified = new ArrayList<>();
      for (int i = 0; i < matchingDividers.size(); i ++) {
         List<Double> listToAppend = new ArrayList<>();
         for (Double key : groups.keySet()) {
            if (groups.get(key).contains(((Integer) ((JSONObject) matchingDividers.get(i)).get("offset")).doubleValue())) {
               listToAppend.add(key);
            }
         }
         offsetsReducedListified.add(listToAppend.get(0));
      }
      HashMap<Integer, Integer> offsetsReduced = weightedHashMap(offsetsReducedListified.stream().map(x -> x.intValue()).collect(Collectors.toList()));
      HashMap<Integer, Integer> offsetsReducedFurther = new HashMap<>();
      for (Integer key : offsetsReduced.keySet()) {
         offsetsReducedFurther.put(key, offsetsReduced.get(key));
      }
      int trueOffset = ((!(offsetsReducedFurther.size() > 0)) ? -10000 : Collections.max(offsetsReducedFurther.entrySet(), HashMap.Entry.comparingByValue()).getKey());
      List<JSONObject> trueMatchingDividers = new ArrayList<>();
      if (trueOffset != -10000) {
         for (int i = 0; i < matchingDividers.size(); i ++) {
            if (Math.abs(((Integer) matchingDividers.get(i).get("offset"))- trueOffset) < maxDifferencePixels) {
               trueMatchingDividers.add(matchingDividers.get(i));
               Log.i(TAG, "\t\tretaining "+matchingDividers.get(i)+" : "+Math.abs(((Integer) matchingDividers.get(i).get("offset"))- trueOffset));
            } else {
               Log.i(TAG, "\t\texcluding "+matchingDividers.get(i)+" : "+Math.abs(((Integer) matchingDividers.get(i).get("offset"))- trueOffset));
            }
         }
      }

      if (trueMatchingDividers.size() < minMatches) {
         JSONObject offsetObject = new JSONObject();
         offsetObject.put("trueOffset", -10000);
         offsetObject.put("trueMatchingDividers", trueMatchingDividers);
         return offsetObject;
      }
      Log.i(TAG, "trueMatchingDividers: " + trueMatchingDividers.size());

      // Then attach divs that don't match by prior tests, but that match given the updated trueMatchingDividers
      for (int i = 0; i < dividersRetainedA.size(); i ++) {
         int AH = (dividersRetainedA.get(i).getHeight());
         for (int j = 0; j < dividersRetainedB.size(); j++) {
            if ((!matchedI.contains(i)) && (!matchedJ.contains(j))) {
               int BH = (dividersRetainedB.get(j).getHeight());
               if ((!(((List<Double>) screenshots.get(0).get("whitespace")).get(i) < 0.1)) && (!(((List<Double>) screenshots.get(1).get("whitespace")).get(j) < 0.1))) {
                  if ((Math.abs(AH - BH) < maxDifferencePixels)) {
                     if ((matchMap.containsKey(i + ":" + j) && (matchMap.get(i + ":" + j) != null) && (matchMap.get(i + ":" + j) < tolerance)) || (pictogramSimilarity(((List<Bitmap>) screenshots.get(0).get("pictograms")).get(i)
                           ,((List<Bitmap>) screenshots.get(1).get("pictograms")).get(j), null) > (1.0-tolerance))) {
                        int oA = (int) dividersRetainedA.subList(0, i).stream().map(x -> x.getHeight()).mapToDouble(x -> x).sum();
                        int oB = (int) dividersRetainedB.subList(0, j).stream().map(x -> x.getHeight()).mapToDouble(x -> x).sum();
                        if (Math.abs((oA - oB) - trueOffset) < 5) {
                           JSONObject thisMatchingDivider = new JSONObject();
                           matchedI.add(i);
                           matchedJ.add(j);
                           thisMatchingDivider.put("indexA", i);
                           thisMatchingDivider.put("indexB", j);
                           thisMatchingDivider.put("offset", (oA - oB));
                           trueMatchingDividers.add(thisMatchingDivider);
                        }
                     }
                  }
               }
            }
         }
      }
      Log.i(TAG, "trueMatchingDividers (after attachment): " + trueMatchingDividers.size());


               JSONObject offsetObject = new JSONObject();
      offsetObject.put("trueOffset", trueOffset);
      offsetObject.put("trueMatchingDividers", trueMatchingDividers);
      Log.i(TAG, "Elapsed time MATCHING: " + Math.abs((System.currentTimeMillis()) - elapsedTime));
      return offsetObject;
   }

   public static List<JSONObject> segmentsStitch(List<JSONObject> thisSegment, JSONObject thisConsistentBounds, Context thisContext) throws JSONException {
      long elapsedTime = System.currentTimeMillis();
      List<List<List<JSONObject>>> outputSegmentsMatchingDividers = new ArrayList<>();
      List<List<JSONObject>> outputSegmentMatchingDividers = new ArrayList<>();
      List<List<Integer>> outputSegments = new ArrayList<>();
      List<List<Integer>> outputSegmentsOffsets = new ArrayList<>();
      List<Integer> outputSegment = new ArrayList<>();
      List<Integer> outputSegmentOffsets = new ArrayList<>();


      File dirDividers = filePath(asList(MainActivity.getMainDir(thisContext).getAbsolutePath(), "debug", "dividers_raw"));
      if (DEBUG) {
         if (!dirDividers.exists()) { dirDividers.mkdirs(); }
      }


      for (int i = 0; i < Math.max(thisSegment.size()-1, 1); i ++) {
         int nScreenshotsPerLink = ((thisSegment.size() > 1) ? 2 : 1);
         for (int j = 0; j < nScreenshotsPerLink; j ++) {
            if ((i+j) <= (thisSegment.size()-1)) {
               if (!thisSegment.get(i+j).has("dividersRetained")) {


                  int endingIndex = Math.max(cropExcludeDividers(
                        (List<Bitmap>) thisSegment.get(i+j).get("dividers"), thisConsistentBounds,
                       ((HashMap<String, Integer>) thisSegment.get(i+j).get("dimensions")).get("height")), 0);
                  //Log.i(TAG, "Ending index has been set: " + endingIndex);
                  thisSegment.get(i+j).put("dividersRetained",
                        ((List<Bitmap>) thisSegment.get(i+j).get("dividers")).subList(
                              ((Integer) thisSegment.get(i+j).get("navbarDividerIndex"))+1, endingIndex));
                  thisSegment.get(i+j).put("dividersExcludedTop",
                        ((List<Bitmap>) thisSegment.get(i+j).get("dividers")).subList(0,
                              ((Integer) thisSegment.get(i+j).get("navbarDividerIndex"))+1));


                  List<Bitmap> rawDividers = ((List<Bitmap>) thisSegment.get(i+j).get("dividersRetained"));

                  if (DEBUG) {
                     for (int k = 0; k < rawDividers.size(); k++) {
                        try (FileOutputStream out = new FileOutputStream(
                              filePath(asList(dirDividers.getAbsolutePath(), "divider-" + (i + j) + "-" + k + ".png")).getAbsolutePath())) {
                           rawDividers.get(k).compress(Bitmap.CompressFormat.PNG, 100, out);
                        } catch (IOException ignored) { }
                     }
                  }

                  // TODO debug
                  List<Bitmap> excludedDividers = Stream.concat((((List<Bitmap>) thisSegment.get(i+j).get("dividers")).subList(0, ((Integer) thisSegment.get(i+j).get("navbarDividerIndex"))+1)).stream(),
                     (((List<Bitmap>) thisSegment.get(i+j).get("dividers")).subList(endingIndex, ((List<Bitmap>) thisSegment.get(i+j).get("dividers")).size())).stream()).collect(Collectors.toList());
                  File adExcluded = filePath(asList(MainActivity.getMainDir(thisContext).getAbsolutePath(), "debug", "crop_excluded"));
                  if (!adExcluded.exists()) { adExcluded.mkdirs(); }
                  int qqq = 0;
                  for (Bitmap b : excludedDividers) {
                        try (FileOutputStream out = new FileOutputStream(
                              filePath(asList(adExcluded.getAbsolutePath(), (i + j) + "-"+qqq+".png")).getAbsolutePath())) {
                           b.compress(Bitmap.CompressFormat.PNG, 100, out);
                        } catch (IOException e) { }
                        qqq ++;
                  }


                  HashMap<String, Integer> size = new HashMap<>(); size.put("w", 32); size.put("h", 32);
                  int thisWhitespacePixel = (int) thisSegment.get(i+j).get("whitespacePixel");
                  thisSegment.get(i+j).put("pictograms",
                        rawDividers.stream().map(x -> {
                           return imageToPictogram(

                                 Args(A("bitmap", x), A("size", size), A("quantizationInterval", 10), A("crop", false))

                                 );
                        }).collect(Collectors.toList()));


                  thisSegment.get(i+j).put("whitespace", ((List<Double>)
                        rawDividers.stream().map(x ->

                                    isWhitespaceLocalDifferencesSubFunction(x, 4) ).collect(Collectors.toList()))); // TODO - this has changed
                  File adWhitespace = filePath(asList(MainActivity.getMainDir(thisContext).getAbsolutePath(), "debug", "whitespace"));
                  if (!adWhitespace.exists()) { adWhitespace.mkdirs(); }
                  int rrr = 0;
                  for (Bitmap rawDivider : rawDividers) {
                     Double thisWhitespace = ((List<Double>) thisSegment.get(i+j).get("whitespace")).get(rrr);
                     if (thisWhitespace < 0.1) {
                        try (FileOutputStream out = new FileOutputStream(
                              filePath(asList(adWhitespace.getAbsolutePath(), (i + j) + "-"+rrr+".png")).getAbsolutePath())) {
                           rawDivider.compress(Bitmap.CompressFormat.PNG, 100, out);
                        } catch (IOException e) { }
                     }
                     rrr ++;
                  }

                  // TODO debug







                  long elapsedTimeOS = System.currentTimeMillis();
                  ///*
                  thisSegment.get(i+j).put("dividersRetaineddividerBoundOffsets", new ArrayList<List<Integer>>() );
                  for (int h = 0; h < ((List<Bitmap>) thisSegment.get(i + j).get("dividersRetained")).size(); h ++) {
                     List<Integer> dividerOffsetsSignature = null;
                     //if (!((List<Boolean>) thisSegment.get(i+j).get("whitespace")).get(h)) {
                        int thisWhitespacePixelAlt = getWhitespacePixel(
                              Args(A("bitmap", rawDividers.get(h)))
                        );
                     DividerSet result = new DividerSet(Args(
                              A("bitmap", Bitmap.createScaledBitmap(
                                             rawDividers.get(h), 64, 16, false)),
                              A("orientation", "h"),
                              A("scaleMinor", 1.0),
                              A("absorbMinimums", false),
                              A("whitespaceThreshold", 0.01),
                              A("colourPaletteThreshold", 0.2),
                              A("scanUntil", 1.0),
                              A("retainMinimums", true),
                              A("minDividerApproach", "complex")
                        ));

                        List<Bitmap> dividerImages = result.dividerImages;
                        List<HashMap<String, Integer>> thisImageDividerBounds = result.dividers;
                        List<Boolean> dividerOffsetsWhitespace = dividerImages.stream().map(x ->
                              isWhitespace(Args(
                                          A("bitmap", x),
                                          A("whitespacePixel", thisWhitespacePixelAlt),
                                          A("thresholdColourPalette", 0.9)))
                              ).collect(Collectors.toList());
                        int thisWidth = rawDividers.get(h).getWidth();
                        dividerOffsetsSignature = dividerBoundOffsets(thisImageDividerBounds, dividerOffsetsWhitespace, thisWidth);
                     //}
                     List<List<Integer>> dividersRetaineddividerBoundOffsets = ((List<List<Integer>>) thisSegment.get(i+j).get("dividersRetaineddividerBoundOffsets"));
                     dividersRetaineddividerBoundOffsets.add(dividerOffsetsSignature); // TODO - check pointer behaviour here
                  }
                  //*/
                  Log.i(TAG, "segmentsStitch dividersRetaineddividerBoundOffsets XXX Elapsed time: " + Math.abs((System.currentTimeMillis()) - elapsedTimeOS));
               }
            }
         }
         //System.out.println( ((List<Bitmap>)thisSegment.get(i+1).get("dividers")).size());
         //System.out.println( ((List<Bitmap>)thisSegment.get(i+1).get("dividersRetained")).size());
         int offset = -10000;
         List<JSONObject> matchingDividers = new ArrayList<>();
         if (thisSegment.size() > 1) {
            Log.i(TAG, "Matching: "+(i)+" & "+(i+1));
            JSONObject result = areScreenshotsLinkedByScroll(asList(thisSegment.get(i), thisSegment.get(i+1)), 5, 0.05, 3); // TODO - maybe adjust to three
            Log.i(TAG, "areScreenshotsLinkedByScroll: "+result);
            offset = (int) result.get("trueOffset");
            matchingDividers = (List<JSONObject>) result.get("trueMatchingDividers");
         }
         outputSegment.add(i);
         if (offset != -10000) {
            outputSegment.add(i + 1);
            outputSegmentOffsets.add(offset);
            outputSegmentMatchingDividers.add(new ArrayList<>(matchingDividers));
         }
         if ((offset == -10000) || (i == (thisSegment.size()-2))) {
            outputSegments.add(new ArrayList<>(outputSegment));
            outputSegmentsOffsets.add(new ArrayList<>(outputSegmentOffsets));
            outputSegmentsMatchingDividers.add(new ArrayList<>(outputSegmentMatchingDividers));
            outputSegment = new ArrayList<>();
            outputSegmentOffsets = new ArrayList<>();
            outputSegmentMatchingDividers = new ArrayList<>();
         }
      }

      List<JSONObject> segmentTimestampSeparatedStitched = new ArrayList<>();
      List<List<Integer>> stitchingIndices = outputSegments.stream().map(x -> x.stream().distinct().collect(Collectors.toList())).collect(Collectors.toList());
      for (int i = 0; i < stitchingIndices.size(); i ++) {
         JSONObject thisObject = new JSONObject();
         thisObject.put("screenshots", stitchingIndices.get(i).stream().map(x -> thisSegment.get(x)).collect(Collectors.toList()));
         thisObject.put("matchingDividers", outputSegmentsMatchingDividers.get(i));
         thisObject.put("offsets", outputSegmentsOffsets.get(i));
         segmentTimestampSeparatedStitched.add(thisObject);
      }
      Log.i(TAG, "segmentsStitch XXX Elapsed time: " + Math.abs((System.currentTimeMillis()) - elapsedTime));
      return segmentTimestampSeparatedStitched;
   }

   // while we could rely on direct projection, this would flatten and exclude data about changing imagery
   public static JSONObject segmentWithoutOCR(JSONObject thisSegment) throws JSONException {
      long elapsedTime = System.currentTimeMillis();
      List<List<List<Integer>>> analysisDS = new ArrayList<>();
      List<List<Integer>> analysisDSReduced = new ArrayList<>();
      List<JSONObject> thisScreenshots = (List<JSONObject>) thisSegment.get("screenshots");
      Log.i(TAG, "segmentWithoutOCR thisScreenshots.size: "+thisScreenshots.size());
      for (int j = 0; j < thisScreenshots.size(); j ++) {
         analysisDS.add(new ArrayList<>());
         for (int k = 0; k < ((List<Bitmap>) ((JSONObject) thisScreenshots.get(j)).get("dividersRetained")).size(); k ++) {
            analysisDS.get(analysisDS.size()-1).add(new ArrayList<>());
         }
      }
      int ii = 0;
      List<List<JSONObject>> thisMatchingDividers = ((List<List<JSONObject>>) thisSegment.get("matchingDividers"));
      for (int j = 0; j < thisMatchingDividers.size(); j ++) {
         List<JSONObject> thisMatchingDivider = thisMatchingDividers.get(j);
         for (int match = 0; match < thisMatchingDivider.size(); match ++) {
            JSONObject thisMatch = thisMatchingDivider.get(match);
            analysisDS.get(j).get((int) thisMatch.get("indexA")).add(ii);
            analysisDS.get(j+1).get((int) thisMatch.get("indexB")).add(ii);
            ii += 1;
         }
      }

      for (int j = 0; j < thisScreenshots.size(); j ++) {
         for (int k = 0; k < ((List<Bitmap>) ((JSONObject) thisScreenshots.get(j)).get("whitespace")).size(); k ++) {
            if ((Double) ((List<Double>) ((JSONObject) thisScreenshots.get(j)).get("whitespace")).get(k) < 0.1) {
               analysisDS.get(j).set(k, Collections.singletonList(-1));
            }
         }
      }

      for (int j = 0; j < analysisDS.size(); j ++) {
         analysisDSReduced.add(new ArrayList<>());
         for (int k = 0; k < analysisDS.get(j).size(); k ++) {
            analysisDSReduced.get(j).add(-1);
            if (analysisDS.get(j).get(k).size() == 0) { // implies it is not none
               analysisDSReduced.get(j).set(k, ii);
               analysisDS.get(j).set(k, Collections.singletonList(ii));
               ii ++;
            }
         }
      }
      HashMap<Integer, List<Integer>> analysisDSGrouper = new HashMap<>();
      for (int j = 0; j < analysisDS.size(); j ++) {
         for (int k = 0; k < analysisDS.get(j).size(); k ++) {
            if ((!((analysisDS.get(j).get(k).size() != 0) && (analysisDS.get(j).get(k).get(0) == -1)))
                  && (analysisDS.get(j).get(k).size() > 0)) { // (analysisDS.get(j).get(k).get(0) != -1) &&
               //List<Integer> groups = analysisDSGrouper.keySet().stream().filter(x -> analysisDS.get(j).get(k).stream().filter(y ->
               //      analysisDSGrouper.get(x).contains(y)).collect(Collectors.toList()).size() > 0).collect(Collectors.toList());

               List<Integer> groups = new ArrayList<>();
               for (Integer y : analysisDSGrouper.keySet()) {
                  boolean found = false;
                  for (int x = 0; x < analysisDS.get(j).get(k).size(); x ++) {


                     if (analysisDSGrouper.get(y).contains(analysisDS.get(j).get(k).get(x))) {
                        found = true;
                     }
                  }
                  if (found) {
                     groups.add(y);
                  }
               }

               if (groups.size() > 0) {
                  List<Integer> newArray = Stream.concat(analysisDSGrouper.get(groups.get(0)).stream(), analysisDS.get(j).get(k).stream()).collect(Collectors.toList());
                  analysisDSGrouper.put(groups.get(0), newArray.stream().distinct().collect(Collectors.toList()));
                  analysisDS.get(j).set(k, Collections.singletonList(groups.get(0)));
                  analysisDSReduced.get(j).set(k, groups.get(0));
               } else {
                  analysisDSGrouper.put(ii, analysisDS.get(j).get(k));
                  analysisDS.get(j).set(k, Collections.singletonList(ii));
                  analysisDSReduced.get(j).set(k, ii);
               }
               ii ++;
            }
         }
      }
      JSONObject output = new JSONObject();
      output.put("analysisDSReduced", analysisDSReduced);
      output.put("ii", ii);
      Log.i(TAG, "segmentWithoutOCR XXX Elapsed time: " + Math.abs((System.currentTimeMillis()) - elapsedTime));
      return output;
   }


   public static JSONObject adHeaderDividersSubProcess(Bitmap quantizedImage, Integer thisIndex, HashMap<String, Object> pictogramsReference, int screenshotI, int dividerI) throws JSONException {
      JSONObject output = new JSONObject();
      JSONObject debugByDivider = new JSONObject();
      int wsPixelRelative = getWhitespacePixel(
Args(A("bitmap", quantizedImage))


      );
      DividerSet thisVisualComponents = new DividerSet(

         Args(
               A("bitmap", quantizedImage),
               A("orientation", "h"),
               A("scaleMinor", 1.0),
               A("absorbMinimums", true),
               A("whitespaceThreshold", 0.2),
               A("colourPaletteThreshold", 0.2),
               A("scanUntil", 1.0),
               A("retainMinimums", false),
               A("minDividerApproach", "simple")
         )



);

      List<Bitmap> horizontalDividers = thisVisualComponents.dividerImages; debugByDivider.put("thisVisualComponentsDividerImages", horizontalDividers);

      List<Boolean> alternations = dividerWhitespaceAlternations(



            Args(A("visualComponents", horizontalDividers),
                  A("whitespacePixel", wsPixelRelative),
                  A("thresholdAmbiguous", 0.3),
                  A("thresholdColourPalette", 0.9),
                  A("method", "localDifferences"))



            );  debugByDivider.put("alternations", alternations);
      debugByDivider.put("alternationsHorizontalDividers", horizontalDividers);

      int indexSecondWhitespace = BooleanIndexOfN(alternations, true, 2);
      if ((indexSecondWhitespace != -1) && (indexSecondWhitespace < (alternations.size()-1))) {
      //if ((alternations.size() >= 3) && (alternations.subList(0,3).equals(Arrays.asList(true, false, true)))) {


         Log.i(TAG, "Testing Ad Header Divider: "+ad_headers_segment_index+" - "+screenshotI+" - "+dividerI + " aka. "+thisIndex);
         int xFrom = (int) IntStream.range(0, indexSecondWhitespace+1).boxed().collect(Collectors.toList()).stream().map(x -> horizontalDividers.get(x).getWidth()).collect(Collectors.toList()).stream().mapToDouble(x->x).sum();
         //int xFrom = (horizontalDividers.get(0).getWidth() + horizontalDividers.get(1).getWidth() + horizontalDividers.get(2).getWidth());
         int xTo = (int) Math.abs(quantizedImage.getWidth() / 2.0);
         int designatedWidth = (int) Math.abs(xTo - xFrom);

         if (((xFrom + designatedWidth) < quantizedImage.getWidth()) && (designatedWidth > 0)) {

            DividerSet thisVisualComponentsAlt1 = new DividerSet(



            Args(A("bitmap", Bitmap.createBitmap(quantizedImage, xFrom, 0, designatedWidth, quantizedImage.getHeight())),
                  A("orientation", "v"),
                  A("scaleMinor", 1.0),
                  A("absorbMinimums", true),
                  A("whitespaceThreshold", 0.1),
                  A("colourPaletteThreshold", 0.3),
                  A("scanUntil", 1.0),
                  A("retainMinimums", false),
                  A("minDividerApproach", "simple"))






            );// TODO  - alternativeWSthreshold was 0.05

            List<Bitmap> verticalDividers = thisVisualComponentsAlt1.dividerImages;
            debugByDivider.put("thisVisualComponentsAlt1verticalDividers", verticalDividers);
            List<Boolean> verticalAlternations = dividerWhitespaceAlternations(

                  Args(
                        A("visualComponents", verticalDividers),
                        A("whitespacePixel", wsPixelRelative),
                        A("thresholdAmbiguous", 0.3),
                        A("thresholdColourPalette", 0.9),
                        A("preserveDimensions", true),
                        A("method", "localDifferences"))

                  );

            debugByDivider.put("verticalAlternationsData", verticalDividers.stream().map(x ->



                  isWhitespaceLocalDifferencesSubFunction(x, 3)).collect(Collectors.toList()));


            debugByDivider.put("verticalAlternations", verticalAlternations);
            if ((verticalAlternations.size() >= 3) && (verticalAlternations.contains(true) && verticalAlternations.contains(false))) {
               //if (((verticalAlternations.size() >= 3) && (verticalAlternations.subList(0, 3).equals(Arrays.asList(true, false, true))))
               //      || ((verticalAlternations.size() >= 3) && (verticalAlternations.subList(0, 3).equals(Arrays.asList(true, false, false))))) {


               //List<Boolean> verticalAlternationsReversed = verticalAlternations.subList(0, verticalAlternations.size());
               //Collections.reverse(verticalAlternationsReversed);
               int compellingIndex = verticalAlternations.lastIndexOf(false);//(verticalAlternations.size() - 1 - verticalAlternationsReversed.indexOf(false));

               debugByDivider.put("verticalDividersCompellingIndex", Arrays.asList(verticalDividers.get(compellingIndex)));

               List<Bitmap> sponsoredTextDividers = new DividerSet(

                    Args(A("bitmap", colourQuantizeBitmap(
                          Args(A("bitmap", verticalDividers.get(compellingIndex)), A("interval", 10))
                          )),
                           A("orientation", "h"),
                           A("scaleMinor", 1.0),
                           A("absorbMinimums", true),
                           A("whitespaceThreshold", 0.1),
                           A("colourPaletteThreshold", 0.1),
                           A("scanUntil", 1.0),
                           A("retainMinimums", false),
                           A("minDividerApproach", "simple"))
               ).dividerImages;
               debugByDivider.put("sponsoredTextDividers", sponsoredTextDividers.stream().collect(Collectors.toList()));

               List<Boolean> sponsoredTextAlternations = dividerWhitespaceAlternations(

                        Args(
                           A("visualComponents", sponsoredTextDividers),
                           A("whitespacePixel", wsPixelRelative),
                           A("thresholdAmbiguous", 0.2),
                           A("thresholdColourPalette", 0.9),
                           A("preserveDimensions", true))

                     );
               debugByDivider.put("sponsoredTextAlternations", sponsoredTextAlternations);

               List<Integer> leads = new ArrayList<>();
               Boolean captured = false;
               for (int i = 0; i < sponsoredTextAlternations.size(); i++) {
                  if (!sponsoredTextAlternations.get(i)) {
                     leads.add(i);
                     captured = true;
                  } else if (captured) {
                     break;
                  }
               }

               debugByDivider.put("leads", leads);
               List<Bitmap> isolatedDividers = new ArrayList<>();
               for (Integer lead : leads) {
                  isolatedDividers.add(sponsoredTextDividers.get(lead));
               }
               //debugByDivider.put("isolatedDividers", Collections.singletonList(combineBitmapsList(isolatedDividers, "h")));

               Bitmap potentialSponsoredText = combineImagesList(
                     Args(A("listOfBitmaps", isolatedDividers))
               );

               if (potentialSponsoredText != null) {

                  potentialSponsoredText = cropWhitespace(
                        Args(
                              A("bitmap", potentialSponsoredText),
                              A("whitespacePixel", wsPixelRelative),
                              A("tolerance", 0.1))
                  );
               }
               //debugByDivider.put("potentialSponsoredText", Collections.singletonList(potentialSponsoredText));
               Double ratio = 0.0;
               if (potentialSponsoredText != null) { ratio = (Double.valueOf(potentialSponsoredText.getHeight())/potentialSponsoredText.getWidth()); }
               Log.i(TAG, "\t-> Sponsored text height ratio: " + ratio);

               if ((potentialSponsoredText != null) && ((ratio >= 0.1) && ratio <= 0.3)) {
                  //potentialSponsoredText = colourQuantizeBitmap(potentialSponsoredText, 32);
                  //debugByDivider.put("potentialSponsoredText", Collections.singletonList(potentialSponsoredText));


                  //debugByDivider.put("potentialSponsoredText", potentialSponsoredText);

                  //if ((potentialSponsoredText.getHeight() > 5) && (potentialSponsoredText.getWidth() > 15)) {


                  HashMap<String, Integer> size = new HashMap<String, Integer>() {{
                     put("w", 128);
                     put("h", 64);
                  }};
                  Stencil potentialSponsoredTextPictogram = imageToStencil(


                        Args(
                              A("bitmap", potentialSponsoredText),
                              A("whitespacePixel", wsPixelRelative),
                              A("size", size),
                              A("snapThreshold", 0.25),
                              A("cropThreshold", 0.05),
                              A("colourPaletteThreshold", 0.05),
                              A("isReference", false)
                  )
                        );

                  debugByDivider.put("potentialSponsoredTextPictogram", potentialSponsoredTextPictogram);

                  List<String> sponsoredTextExposure = Arrays.asList("Light", "Dark");
                  List<String> sponsoredTextAlt = Arrays.asList("", "Alt");
                  boolean found = false;
                  for (int i = 0; i < sponsoredTextExposure.size(); i++) {
                     for (int j = 0; j < sponsoredTextAlt.size(); j++) {
                        double matchResult = stencilSimilarity(


                              Args(
                                    A("a", pictogramsReference.get("facebook" + sponsoredTextExposure.get(i) + "Sponsored" + sponsoredTextAlt.get(j))),
                                    A("b", potentialSponsoredTextPictogram),
                                    A("method", "multiplied"),
                                    A("deepSampling", true))
                              );

                        debugByDivider.put("matchResultIndication:" + sponsoredTextExposure.get(i), false);
                        debugByDivider.put("matchResult:" + sponsoredTextExposure.get(i) + sponsoredTextAlt.get(j), matchResult);

                        int[][] thisStencil = ((Stencil) pictogramsReference.get("facebook" + sponsoredTextExposure.get(i) + "Sponsored" + sponsoredTextAlt.get(j))).getStencil();
                        int[][] thatStencil = potentialSponsoredTextPictogram.getStencil();

                        Log.i(TAG,  "\t-> Match result: " + matchResult);
                        Log.i(TAG, stencilToString(thisStencil));
                        Log.i(TAG, stencilToString(thatStencil));


                        debugByDivider.put("matchResultIndication:" + sponsoredTextExposure.get(i) + ":" + sponsoredTextAlt.get(j), true);
                        if (matchResult > 0.75) {
                           found = true;
                           break;
                        }
                     }
                  }
                  if (found) {
                     output.put("debugByDivider", debugByDivider);
                     output.put("found", true);
                     return output;
                  } else {
                     Log.i(TAG, "Excluding Ad Header Divider (step5): "+ad_headers_segment_index+" - "+screenshotI+" - "+dividerI + " aka. "+thisIndex+" : "+alternations);
                  }
               } else {
                  Log.i(TAG, "Excluding Ad Header Divider (step4): "+ad_headers_segment_index+" - "+screenshotI+" - "+dividerI + " aka. "+thisIndex+" : "+sponsoredTextAlternations);
               }

            } else {
               Log.i(TAG, "Excluding Ad Header Divider (step3): "+ad_headers_segment_index+" - "+screenshotI+" - "+dividerI + " aka. "+thisIndex+" : "+alternations);
            }
         } else {
            Log.i(TAG, "Excluding Ad Header Divider (step2): "+ad_headers_segment_index+" - "+screenshotI+" - "+dividerI + " aka. "+thisIndex+" : "+alternations);
         }

      } else {
         Log.i(TAG, "Excluding Ad Header Divider (step1): "+ad_headers_segment_index+" - "+screenshotI+" - "+dividerI + " aka. "+thisIndex+" : "+alternations);
      }
      output.put("debugByDivider", debugByDivider);
      output.put("found", false);
      return output;
   }

   private static int ad_headers_segment_index = 0;

   public static List<Integer> adHeaderDividers(JSONObject segmentStitched, List<List<Integer>> analysisDS, HashMap<String, Object> pictogramsReference, Context thisContext, Integer totalHeight) throws JSONException, IOException {
      long elapsedTime = System.currentTimeMillis();
      List<Integer> retainedDividers = new ArrayList<>();
      List<Integer> indexed = new ArrayList<>();
      List<JSONObject> thisScreenshots = (List<JSONObject>) segmentStitched.get("screenshots");
      JSONObject debugData = new JSONObject();
      for (int screenshotI = 0; screenshotI < analysisDS.size(); screenshotI ++) {
         debugData.put(String.valueOf(screenshotI), new JSONObject());
         List<Double> thisScreenshotWhitespace = (List<Double>) thisScreenshots.get(screenshotI).get("whitespace");
         for (int dividerI = 0; dividerI < analysisDS.get(screenshotI).size(); dividerI ++) {
            JSONObject debugByScreenshot = (JSONObject) debugData.get(String.valueOf(screenshotI));
            debugByScreenshot.put(String.valueOf(dividerI), new JSONObject());
            JSONObject debugByDivider = (JSONObject) debugByScreenshot.get(String.valueOf(dividerI));
            Integer thisIndex = analysisDS.get(screenshotI).get(dividerI);

            if (!indexed.contains(thisIndex)) {

               indexed.add(thisIndex);
               if (!(thisScreenshotWhitespace.get(dividerI) < 0.1)) {
                  Bitmap dividerImage = ((List<Bitmap>) thisScreenshots.get(screenshotI).get("dividersRetained")).get(dividerI); debugByDivider.put("dividerImage", dividerImage);

                  Double heightRatio = (dividerImage.getHeight() / totalHeight.doubleValue());

                  //if ((heightRatio > 0.025) && (heightRatio < 0.1)) {
                     Bitmap quantizedImage = dividerImage; debugByDivider.put("quantizedImage", quantizedImage);

                     JSONObject output = adHeaderDividersSubProcess( quantizedImage, thisIndex, pictogramsReference, screenshotI, dividerI);
                     JSONObject debugByDividerInstantiated = (JSONObject) output.get("debugByDivider");
                     for (Iterator<String> it = debugByDividerInstantiated.keys(); it.hasNext(); ) {
                        String k = it.next();
                        debugByDivider.put(k, debugByDividerInstantiated.get(k));
                     }
                     if ((Boolean) output.get("found")) {
                        Log.i(TAG, "heightRatio: "+heightRatio);
                        retainedDividers.add(analysisDS.get(screenshotI).get(dividerI));
                     }
                  //}

               } else {
                  Log.i(TAG,"Excluding Ad Header Divider (whitespace): "+ad_headers_segment_index+" - "+screenshotI+" - "+dividerI+" aka. "+thisIndex);
               }
            } else {
               Log.i(TAG,"Excluding Ad Header Divider (indexed): "+ad_headers_segment_index+" - "+screenshotI+" - "+dividerI+" aka. "+thisIndex);
            }
         }
      }
      if (DEBUG) {
         File adHeadersDirName = filePath(asList(MainActivity.getMainDir(thisContext).getAbsolutePath(), "debug", "1_ad_headers"));
         if (!adHeadersDirName.exists()) { adHeadersDirName.mkdirs(); }

         File adHeaderSuccessesDirName = filePath(asList(adHeadersDirName.getAbsolutePath(), "successes"));
         if (!adHeaderSuccessesDirName.exists()) { adHeaderSuccessesDirName.mkdirs(); }
         File adHeaderFailuresDirName = filePath(asList(adHeadersDirName.getAbsolutePath(), "failures"));
         if (!adHeaderFailuresDirName.exists()) { adHeaderFailuresDirName.mkdirs(); }

         for (int screenshotI = 0; screenshotI < analysisDS.size(); screenshotI ++) {
            for (int dividerI = 0; dividerI < analysisDS.get(screenshotI).size(); dividerI++) {
               JSONObject debugByScreenshot = (JSONObject) debugData.get(String.valueOf(screenshotI));
               JSONObject debugByDivider = (JSONObject) debugByScreenshot.get(String.valueOf(dividerI));
               Boolean matchResultIndicationLight = false;
               try { matchResultIndicationLight = (Boolean) debugByDivider.get("matchResultIndication:Light"); } catch (Exception e) {}
               Boolean matchResultIndicationDark = false;
               try { matchResultIndicationDark = (Boolean) debugByDivider.get("matchResultIndication:Dark"); } catch (Exception e) {}

               File indicatedDirectory = (matchResultIndicationLight || matchResultIndicationDark)
                     ? adHeaderSuccessesDirName : adHeaderFailuresDirName;

               File thisAdHeaderDirName = filePath(asList(indicatedDirectory.getAbsolutePath(), ad_headers_segment_index+"_"+screenshotI+"_"+dividerI));
               if (!thisAdHeaderDirName.exists()) { thisAdHeaderDirName.mkdirs(); }

               List<String> bitmapsToWrite = Arrays.asList("dividerImage", "quantizedImage", "potentialSponsoredText");
               for (String s : bitmapsToWrite) {
                  if (debugByDivider.has(s)) {
                     Bitmap thisBitmap = (Bitmap) debugByDivider.get(s);
                     try (FileOutputStream out = new FileOutputStream(
                           filePath(asList(thisAdHeaderDirName.getAbsolutePath(), s+".png")).getAbsolutePath())) {
                        thisBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                     } catch (IOException e) { }
                  }
               }


               List<String> bitmapListsToWrite = Arrays.asList(
                     "thisVisualComponentsDividerImages",
                     "alternationsHorizontalDividers",
                     "verticalDividersCompellingIndex",
                     "thisVisualComponentsAlt1verticalDividers",
                     "sponsoredTextDividers");

               for (String s : bitmapListsToWrite) {
                  if (debugByDivider.has(s)) {
                     File thisBitmapListDirName = filePath(asList(thisAdHeaderDirName.getAbsolutePath(), s));
                     if (!thisBitmapListDirName.exists()) { thisBitmapListDirName.mkdirs(); }
                     List<Bitmap> bitmapsListToWrite = (List<Bitmap>) debugByDivider.get(s);
                     int ii = 0;
                     for (Bitmap thisBitmap : bitmapsListToWrite) {
                        try (FileOutputStream out = new FileOutputStream(
                              filePath(asList(thisBitmapListDirName.getAbsolutePath(), ii+".png")).getAbsolutePath())) {
                           thisBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        } catch (IOException e) { }
                        ii ++;
                     }
                  }
               }

               List<String> metadataKeys = Arrays.asList(
                     "leads",
                     "prescribedLeadingCharacterIndex",
                     "prescribedCharacterWidth",
                     "trails",
                     "alternations",
                     "verticalAlternations",
                     "verticalAlternationsData",
                     "sponsoredTextAlternations",
                     "prescribedTrailingCharacterIndex",
                     "matchResult:Light",
                     //"potentialSponsoredTextPictogram",
                     "matchResult:Dark");

               JSONObject metadata = new JSONObject();
               for (String metadataKey : metadataKeys) {
                  if (debugByDivider.has(metadataKey)) {
                     metadata.put(metadataKey, debugByDivider.get(metadataKey));
                  }
               }

               Writer output;
               Gson gson = new Gson();
               File file = filePath(asList(thisAdHeaderDirName.getAbsolutePath(), "metadata.json"));
               output = new BufferedWriter(new FileWriter(file));
               output.write(gson.toJson(metadata));
               output.close();
            }
         }
      }
      Log.i(TAG, "adHeaderDividers XXX Elapsed time: " + Math.abs((System.currentTimeMillis()) - elapsedTime));
      ad_headers_segment_index ++;
      return retainedDividers;
   }


   public static List<Integer> postEngagementDividers(JSONObject segmentStitched, List<List<Integer>> analysisDS, HashMap<String, Object> pictogramsReference, Context thisContext) throws JSONException {
      long elapsedTime = System.currentTimeMillis();
      List<Integer> retainedSponsoredTextDividers = new ArrayList<>();
      List<Integer> indexed = new ArrayList<>();
      List<JSONObject> thisScreenshots = (List<JSONObject>) segmentStitched.get("screenshots");

      for (int screenshotI = 0; screenshotI < analysisDS.size(); screenshotI ++) {
         List<Double> thisScreenshotWhitespace = (List<Double>) thisScreenshots.get(screenshotI).get("whitespace");
         for (int dividerI = 0; dividerI < analysisDS.get(screenshotI).size(); dividerI++) {
            Log.i(TAG, "postEngagementDividers: Testing screenshotI: "+screenshotI+" dividerI: "+dividerI);

            Integer thisIndex = analysisDS.get(screenshotI).get(dividerI);
            if (!indexed.contains(thisIndex)) {
               Log.i(TAG, "postEngagementDividers: \t* unindexed");
               indexed.add(thisIndex);
               if (!(thisScreenshotWhitespace.get(dividerI) < 0.1)) {
                  Log.i(TAG, "postEngagementDividers: \t* non-whitespace");
                  Bitmap dividerImage = ((List<Bitmap>) thisScreenshots.get(screenshotI).get("dividersRetained")).get(dividerI);
                  if (dividerImage.getWidth() > dividerImage.getHeight()*20) {
                     Log.i(TAG, "postEngagementDividers: \t* correct height");

                     DividerSet result = new DividerSet(



                     Args(
                           A("bitmap", dividerImage),
                           A("orientation", "h"),
                           A("scaleMinor", 1.0),
                           A("absorbMinimums", false),
                           A("whitespaceThreshold", 0.2),
                           A("colourPaletteThreshold", 0.1),
                           A("scanUntil", 1.0),
                           A("retainMinimums", true),
                           A("minDividerApproach", "complex")
                     )



                     );
                     List<Bitmap> dividerImages = result.dividerImages;
                     if (dividerImages.size() >= 2) {
                        Log.i(TAG, "postEngagementDividers: \t* contains > 2 visuals");
                        if (!


                              isWhitespace(

                                    Args(
                                    A("bitmap", dividerImages.get(1)),
                                    A("thresholdColourPalette", 0.9)))

                        ) {
                           Log.i(TAG, "postEngagementDividers: \t* 1st div is not whitespace");

                           Bitmap pre = cropWhitespace(

                                 Args(
                                       A("bitmap", dividerImages.get(1)),
                                       A("whitespacePixel", getWhitespacePixel(

                                             Args(A("bitmap", dividerImage))


                                       )))
                                 );
                           Log.i(TAG, "postEngagementDividers: \t* height width: "+pre.getHeight()+" : "+pre.getWidth());
                           double ratio = (pre.getHeight()/ ((double) pre.getWidth()));
                           Log.i(TAG, "postEngagementDividers: \t* height/width ratios: "+ratio);
                           if ((ratio > 0.25) && (ratio < 1.1)) {
                              Log.i(TAG, "postEngagementDividers: \t* height/width ratios are correct");
                              Bitmap post = pre;
                              if (pre.getWidth() > pre.getHeight()) {
                                 post = Bitmap.createBitmap(pre, 0, 0, pre.getHeight(), pre.getHeight());
                              }
                              final Bitmap postFinal = post;
                              String fname = "pe-" + UUID.randomUUID().toString() + ".png";


                              HashMap<String, Integer> size = new HashMap<String, Integer>() {{
                                 put("w", 16);
                                 put("h", 16);
                              }};
                              //
                              List<String> candidates = Arrays.asList("Like", "Love", "Laugh", "Care", "Wow", "Sad", "Hate");

                              Bitmap testImageA = (Bitmap) pictogramsReference.get("facebookReactMask");
                              Log.i(TAG, "postEngagementDividers: \t* facebookReactMask width: "+ testImageA.getWidth());





                              List<Double> candidatesResults = candidates.stream().map(x -> pictogramSimilarityV2(
                                 colourQuantizeBitmap(Args(A("bitmap", (Bitmap) pictogramsReference.get("facebookReact" + x)))),
                                 colourQuantizeBitmap(Args(A("bitmap", imageToPictogram(Args(A("bitmap", postFinal),A("size", size),A("crop", false)))))),
                                    (Bitmap) pictogramsReference.get("facebookReactMask"))).collect(Collectors.toList());
                              /*

                              List<Double> candidatesResults = candidates.stream().map(x -> pictogramSimilarityV2(
                                      (Bitmap) pictogramsReference.get("facebookReact" + x),
                                      imageToPictogram(Args(A("bitmap", postFinal),A("size", size),A("crop", false))),
                                      (Bitmap) pictogramsReference.get("facebookReactMask"))).collect(Collectors.toList());

                               */

                              Log.i(TAG, "postEngagementDividers: \t* emoticon results: ");
                              Log.i(TAG, "postEngagementDividers: \t"+candidates);
                              Log.i(TAG, "postEngagementDividers: \t"+candidatesResults);



                              /*
                              if (DEBUG) {
                                 File DirName = filePath(asList(MainActivity.getMainDir(thisContext).getAbsolutePath(), "debug", "post_engagement_focus_divs"));
                                 if (!DirName.exists()) {
                                    DirName.mkdirs();
                                 }


                                 try (FileOutputStream out = new FileOutputStream(
                                         filePath(asList(DirName.getAbsolutePath(), fname)).getAbsolutePath())) {
                                    postFinal.compress(Bitmap.CompressFormat.PNG, 100, out);
                                 } catch (IOException e) {
                                 }
                              }*/



                              if (candidatesResults.stream().anyMatch(x -> x > 0.90)) { // TODO - this should be the other way around




                                             Log.i(TAG, "postEngagementDividers: \t* height/width ratios: "+ratio);


                                                         if (DEBUG) {
                                                            File DirName = filePath(asList(MainActivity.getMainDir(thisContext).getAbsolutePath(), "debug", "post_engagement_focus_divs"));
                                                            if (!DirName.exists()) {
                                                               DirName.mkdirs();
                                                            }


                                                            try (FileOutputStream out = new FileOutputStream(
                                                                  filePath(asList(DirName.getAbsolutePath(), fname)).getAbsolutePath())) {
                                                               post.compress(Bitmap.CompressFormat.PNG, 100, out);
                                                            } catch (IOException e) {
                                                            }
                                                         }




                                                         Log.i(TAG, "WWW "+fname+": "+ (candidates.stream().map(x -> pictogramSimilarity((Bitmap) pictogramsReference.get("facebookReact" + x),


                                                               imageToPictogram(


                                                                     Args(
                                                                           A("bitmap", postFinal),
                                                                           A("size", size),
                                                                           A("crop", false))

                                                               ), (Bitmap) pictogramsReference.get("facebookReactMask")) ).collect(Collectors.toList()).toString()));

                                                         retainedSponsoredTextDividers.add(analysisDS.get(screenshotI).get(dividerI));

                                                         File postEngagementDirName = filePath(asList(MainActivity.getMainDir(thisContext).getAbsolutePath(), "debug", "post_engagements"));
                                                         try (FileOutputStream out = new FileOutputStream(
                                                                 filePath(asList(postEngagementDirName.getAbsolutePath(), "post-engagement-" + screenshotI + "-" + dividerI + ".png")).getAbsolutePath())) {
                                                            retrieveRetainedDivider(segmentStitched, screenshotI, dividerI).compress(Bitmap.CompressFormat.PNG, 100, out);
                                                         } catch (IOException e) { }

                              }


                        }

                        }
                     }

                  }
               }
            }
         }
      }
      Log.i(TAG, "postEngagementDividers XXX Elapsed time: " + Math.abs((System.currentTimeMillis()) - elapsedTime));
      return retainedSponsoredTextDividers;
   }

   public static HashMap<Integer, List<List<Integer>>> getIndicators(
         List<List<Integer>> analysisDS, int j, List<Integer> xIndices, List<Integer> yIndices, String thisCase, HashMap<Integer, List<List<Integer>>> argIndicators) {
      HashMap<Integer, List<List<Integer>>> indicators = new HashMap<>();
      for (Iterator<Integer> iter = xIndices.iterator(); iter.hasNext(); ) {
         Integer x = iter.next();
         indicators.put(x, new ArrayList<>());
      }
      if (argIndicators != null) {
         indicators = new HashMap<>(argIndicators);
      }
      for (Iterator<Integer> iter = xIndices.iterator(); iter.hasNext(); ) {
         Integer x = iter.next();
         if (analysisDS.get(j).contains(x)) {
            int xIndex = analysisDS.get(j).indexOf(x);
            List<Integer> dividersAround = ((thisCase == "header") ? analysisDS.get(j).subList(xIndex+1, analysisDS.get(j).size()) : analysisDS.get(j).subList(0, xIndex));
            dividersAround = dividersAround.stream().filter(d -> (d != -1)).collect(Collectors.toList());
            List<Integer> prematureCutIndices = Stream.concat(xIndices.stream(), yIndices.stream().filter(d -> (d != xIndex)).collect(Collectors.toList()).stream()).collect(Collectors.toList());
            List<Integer> dividersAroundIndicated = dividersAround.stream().map(d ->{if (!prematureCutIndices.contains(d)) { return d;} {return -1;}}).collect(Collectors.toList());
            if (dividersAroundIndicated.contains(-1)) {
               dividersAround = ((thisCase == "header") ?
                     dividersAround.subList(0, dividersAroundIndicated.indexOf(-1)+1) : dividersAround.subList(dividersAroundIndicated.lastIndexOf(-1), dividersAround.size()));
               indicators.get(x).add(dividersAround);
            }
         } else {
            indicators.get(x).add(new ArrayList<>());
         }
      }
      return indicators;
   }

   public static HashMap<Integer, List<Integer>> getIndicatorsFlattened(HashMap<Integer, List<List<Integer>>> indicator) {
      HashMap<Integer, List<Integer>> thisFlattened = new HashMap<>();
      for (Iterator<Integer> iter = indicator.keySet().iterator(); iter.hasNext(); ) {
         Integer k = iter.next(); thisFlattened.put(k, new ArrayList<>());
      }
      for (Iterator<Integer> iter = indicator.keySet().iterator(); iter.hasNext(); ) {
         Integer k = iter.next();
         List<List<Integer>> v = indicator.get(k);
         for (int j = 0; j < v.size(); j ++) {
            List<Integer> y = v.get(j);
            if (y.size() > 0) {
               thisFlattened.put(k, Stream.concat(thisFlattened.get(k).stream(), y.stream()).collect(Collectors.toList()));
            }
         }
      }
      for (Iterator<Integer> iter = thisFlattened.keySet().iterator(); iter.hasNext(); ) {
         Integer k = iter.next();
         if ( thisFlattened.get(k).contains(k)) {
            thisFlattened.put(k, thisFlattened.get(k).stream().distinct().collect(Collectors.toList()));
         }
         else {
            thisFlattened.put(k, Stream.concat(Collections.singletonList(k).stream(),
                  thisFlattened.get(k).stream().distinct().collect(Collectors.toList()).stream()).collect(Collectors.toList()));
         }
      }
      return thisFlattened;
   }

   public static JSONObject retainedDividerIndices(List<List<Integer>> analysisDS, Integer k) throws JSONException {
      for (int j = 0; j < analysisDS.size(); j ++) {
         if (analysisDS.get(j).contains(k)) {
            JSONObject output = new JSONObject();
            output.put("screenshotI", j);
            output.put("dividerI", analysisDS.get(j).indexOf(k));
            return output;
         }
      }
      return null;
   }

   public static Bitmap retrieveRetainedDivider(JSONObject segmentStitched, Integer screenshotIndex, Integer dividerIndex) throws JSONException {
      return ((List<Bitmap>) ((List<JSONObject>) segmentStitched.get("screenshots")).get(screenshotIndex).get("dividersRetained")).get(dividerIndex);
   }
   public static List<JSONObject> extractAds(JSONObject segmentStitched, List<List<Integer>> analysisDS, Integer ii, HashMap<String, Object> pictogramsReference, Context thisContext, int totalHeight) throws JSONException, IOException {
      long elapsedTime = System.currentTimeMillis();
      List<JSONObject> anticipatedAds = new ArrayList<>();

      List<Integer> adHeaderDividerIndices = adHeaderDividers(segmentStitched, analysisDS, pictogramsReference, thisContext, totalHeight);
      List<Integer> postEngagementDividersIndices = postEngagementDividers(segmentStitched, analysisDS, pictogramsReference, thisContext);
      Log.i(TAG, "adHeaderDividerIndices: "+adHeaderDividerIndices);
      Log.i(TAG, "postEngagementDividersIndices: "+postEngagementDividersIndices);

      List<Integer> finalAdHeaderDividerIndices = adHeaderDividerIndices;
      postEngagementDividersIndices = postEngagementDividersIndices.stream().filter(x -> !finalAdHeaderDividerIndices.contains(x)).collect(Collectors.toList());

      Log.i(TAG, "analysisDS: Stream: "+analysisDS);
      Log.i(TAG, "analysisDS: adHeaderDividerIndices: "+adHeaderDividerIndices);
      Log.i(TAG, "analysisDS: postEngagementDividersIndices: "+postEngagementDividersIndices);

      HashMap<Integer, List<List<Integer>>> adHeaderTails = null;
      HashMap<Integer, List<List<Integer>>> postEngagementHeads = null;

      for (int j = 0; j < analysisDS.size(); j ++) {
         adHeaderTails = getIndicators(analysisDS, j, adHeaderDividerIndices, postEngagementDividersIndices, "header", adHeaderTails);
         postEngagementHeads = getIndicators(analysisDS, j, postEngagementDividersIndices, adHeaderDividerIndices, "engagement", postEngagementHeads);
      }

      Log.i(TAG, "postEngagementHeads: "+postEngagementHeads);

      HashMap<Integer, List<Integer>> adHeaderTailsFlattened = getIndicatorsFlattened(adHeaderTails);
      HashMap<Integer, List<Integer>> postEngagementHeadsFlattened = getIndicatorsFlattened(postEngagementHeads);
      Log.i(TAG, "adHeaderTailsFlattened: "+adHeaderTailsFlattened);
      Log.i(TAG, "postEngagementHeadsFlattened: "+postEngagementHeadsFlattened);

      if (DEBUG) {
         File adHeadersDirName = filePath(asList(MainActivity.getMainDir(thisContext).getAbsolutePath(), "debug", "ad_headers"));
         if (!adHeadersDirName.exists()) { adHeadersDirName.mkdirs(); }
         for (Iterator<Integer> iterAdHeader = adHeaderTailsFlattened.keySet().iterator(); iterAdHeader.hasNext();) {
            Integer adHeaderIndex = iterAdHeader.next();
            JSONObject retainedDividerIndex = retainedDividerIndices(analysisDS, adHeaderIndex);
            Integer screenshotI = (Integer) retainedDividerIndex.get("screenshotI");
            Integer dividerI = (Integer) retainedDividerIndex.get("dividerI");
            try (FileOutputStream out = new FileOutputStream(
                  filePath(asList(adHeadersDirName.getAbsolutePath(), "ad-header-" + adHeaderIndex + ".png")).getAbsolutePath())) {
               retrieveRetainedDivider(segmentStitched, screenshotI, dividerI).compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (IOException e) { }
         }
      }

      if (DEBUG) {
         File postEngagementDirName = filePath(asList(MainActivity.getMainDir(thisContext).getAbsolutePath(), "debug", "post_engagements"));
         if (!postEngagementDirName.exists()) { postEngagementDirName.mkdirs(); }
         for (Iterator<Integer> iterPostEngagement = postEngagementHeadsFlattened.keySet().iterator(); iterPostEngagement.hasNext();) {
            Integer postEngagementIndex = iterPostEngagement.next();
            JSONObject retainedDividerIndex = retainedDividerIndices(analysisDS, postEngagementIndex);
            Integer screenshotI = (Integer) retainedDividerIndex.get("screenshotI");
            Integer dividerI = (Integer) retainedDividerIndex.get("dividerI");
            /*try (FileOutputStream out = new FileOutputStream(
                  filePath(asList(postEngagementDirName.getAbsolutePath(), "post-engagement-" + postEngagementIndex + ".png")).getAbsolutePath())) {
               retrieveRetainedDivider(segmentStitched, screenshotI, dividerI).compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (IOException e) { }*/
         }
      }


      if (DEBUG) {
         File dir = filePath(asList(MainActivity.getMainDir(thisContext).getAbsolutePath(), "debug", "dividers_retained"));
         if (!dir.exists()) { dir.mkdirs(); }
         for (int screenshotI = 0; screenshotI < analysisDS.size(); screenshotI ++) {
            for (int dividerI = 0; dividerI < analysisDS.get(screenshotI).size(); dividerI++) {
               Integer thisIndex = analysisDS.get(screenshotI).get(dividerI);
               try (FileOutputStream out = new FileOutputStream(
                     filePath(asList(dir.getAbsolutePath(), "divider-" + thisIndex + ".png")).getAbsolutePath())) {
                  retrieveRetainedDivider(segmentStitched, screenshotI, dividerI).compress(Bitmap.CompressFormat.PNG, 100, out);
               } catch (IOException e) { }
            }
         }
      }


      for (Iterator<Integer> iter = adHeaderTailsFlattened.keySet().iterator(); iter.hasNext(); ) {
         Integer adHeaderIndex = iter.next();
         for (Iterator<Integer> iterB = postEngagementHeadsFlattened.keySet().iterator(); iterB.hasNext(); ) {
            Integer postEngagementIndex = iterB.next();
            List<Integer> candidateList = Stream.concat(adHeaderTailsFlattened.get(adHeaderIndex).stream(),
                  Collections.singletonList(adHeaderIndex).stream()).collect(Collectors.toList());
            if (candidateList.stream().anyMatch(x -> (postEngagementHeadsFlattened.get(postEngagementIndex).contains(x)))) {
               if (adHeaderTailsFlattened.get(adHeaderIndex).contains(postEngagementIndex)) {
                  JSONObject anticipatedAd = new JSONObject();
                  anticipatedAd.put("ad_header_index", adHeaderIndex);
                  anticipatedAd.put("post_engagement_index", postEngagementIndex);
                  anticipatedAds.add(anticipatedAd);
               }
            }
         }
      }

      Log.i(TAG, "anticipatedAds: "+ anticipatedAds.toString());


      for (Iterator<JSONObject> iter = anticipatedAds.iterator(); iter.hasNext(); ) {
         JSONObject anticipatedAd = iter.next();
         int AHI = (int) anticipatedAd.get("ad_header_index");
         int PEI = (int) anticipatedAd.get("post_engagement_index");

         HashMap<Integer, List<List<Integer>>> finalAdHeaderTails = adHeaderTails;

         List<Integer> adHeaderDividerScreenshotIndexDerivingList = new ArrayList<>();
         for (int q = 0; q < adHeaderTails.get(AHI).size(); q ++) {
            if (finalAdHeaderTails.get(AHI).get(q).size() > 0) {
               adHeaderDividerScreenshotIndexDerivingList.add(q);
            }
         }

         int adHeaderDividerScreenshotIndex = adHeaderDividerScreenshotIndexDerivingList.get(0);
         int adHeaderDividerIndex = analysisDS.get(adHeaderDividerScreenshotIndex).indexOf(AHI);
         List<Integer> relevantDividerIndices = Stream.concat(
               adHeaderTailsFlattened.get(AHI).stream().distinct().collect(Collectors.toList()).stream(),
               postEngagementHeadsFlattened.get(PEI).stream().distinct().collect(Collectors.toList()).stream()).collect(Collectors.toList());
         relevantDividerIndices = Stream.concat(relevantDividerIndices.stream(), Collections.singletonList(AHI).stream()).collect(Collectors.toList());

         HashMap<Integer, Bitmap> adRetainedDividers = new HashMap<>();

         for (Iterator<Integer> iterC = relevantDividerIndices.iterator(); iterC.hasNext(); ) {
            Integer k = iterC.next();

            JSONObject retainedDividerIndex = retainedDividerIndices(analysisDS, k);
            Integer screenshotI = (Integer) retainedDividerIndex.get("screenshotI");
            Integer dividerI = (Integer) retainedDividerIndex.get("dividerI");

            adRetainedDividers.put(k, retrieveRetainedDivider(segmentStitched, screenshotI, dividerI));
         }

         anticipatedAd.put("screenshot_divider_orderings", analysisDS);
         anticipatedAd.put("divider_images", adRetainedDividers);
         anticipatedAd.put("screenshot_offsets", segmentStitched.get("offsets"));
         //anticipatedAd.put("post_engagement_divider_indices", postEngagementDividersIndices);

         List<JSONObject> screenshotRelativeData = new ArrayList<>();

         for (int j = 0; j < analysisDS.size(); j ++) {
            JSONObject thisScreenshotObject = new JSONObject();
            JSONObject thisScreenshotRaw = ((List<JSONObject>) segmentStitched.get("screenshots")).get(j);
            thisScreenshotObject.put("uuid", thisScreenshotRaw.get("uuid"));
            thisScreenshotObject.put("timestamp", thisScreenshotRaw.get("timestamp"));
            thisScreenshotObject.put("fname", thisScreenshotRaw.get("fname"));
            thisScreenshotObject.put("in_facebook", thisScreenshotRaw.get("inFacebook"));
            thisScreenshotObject.put("divider_bounds", thisScreenshotRaw.get("dividerBounds"));
            thisScreenshotObject.put("ws_pxl", thisScreenshotRaw.get("whitespacePixel"));
            thisScreenshotObject.put("tab_active", thisScreenshotRaw.get("tabActive"));
            thisScreenshotObject.put("tabs_identified", thisScreenshotRaw.get("tabsIdentified"));
            thisScreenshotObject.put("tabs_n", thisScreenshotRaw.get("tabsN"));
            thisScreenshotObject.put("whitespace", thisScreenshotRaw.get("whitespace"));
            thisScreenshotObject.put("navbar_divider_index", thisScreenshotRaw.get("navbarDividerIndex"));
            screenshotRelativeData.add(thisScreenshotObject);
         }
         Gson gson = new Gson();
         anticipatedAd.put("screenshot_relative_data",  screenshotRelativeData);
      }

      Log.i(TAG, "extractAds XXX Elapsed time: " + Math.abs((System.currentTimeMillis()) - elapsedTime));
      return anticipatedAds;
   }


   public static List<JSONObject> processScreenshots(List<JSONObject> bitmapsToProcess,
                                         boolean Verbose, HashMap<String, Object> pictogramsReference, Context thisContext) throws JSONException, IOException {

      thisMasterContext = thisContext;

      long elapsedTime = System.currentTimeMillis();
      long elapsedTimeTotal = System.currentTimeMillis();
      //String thisEvent = UUID.randomUUID().toString();

      List<JSONObject> thisOutput = new ArrayList<>();

      makeDirectory(filePath(asList(((new File(".")).getAbsolutePath()),
            "src", "debug",  "assets", "local", "contentInterpreterSimulationsOutput")));

      JSONObject bitmapsObject = new JSONObject();
      List<String> listOfScreenshots = new ArrayList<>();
      for (JSONObject bitmapToProcess : bitmapsToProcess) {
         String fname = (String) bitmapToProcess.get("fname");
         try {
            Bitmap bitmap = (Bitmap) bitmapToProcess.get("bitmap");
            bitmapsObject.put(fname, bitmap);
            listOfScreenshots.add(fname);
         } catch (Exception e) {
            Log.i(TAG, "Caught a bad bitmap: "+ fname);
         }
      }


      //List<String> listOfScreenshots = retrieveScreenshotsForProcessing(thisSimulationTest);

      List<List<JSONObject>> segmentsTimestamped = segmentTimestampSeparated(listOfScreenshots, null);

      Log.i(TAG, segmentsTimestamped.toString());

      if (Verbose) {
         Log.i(TAG,  "There are "+segmentsTimestamped.size()+" segments, separated by timestamps...");
      }

      List<JSONObject> consistentBounds = new ArrayList<>();
      int ii = 0;
      int nAdsFound = 0;
      for (int i = 0; i < segmentsTimestamped.size(); i ++) {
         if (Verbose) {
            Log.i(TAG, "\tIndexing segment "+i+"...");
         }
         for (int j = 0; j < segmentsTimestamped.get(i).size(); j ++) {
            if (Verbose) {
               Log.i(TAG,  "\t\tIndexing segment screenshot "+j+"...divideImageIntoVisualComponents");
            }
            //File thisScreenshotFile = retrieveLocalScreenshotImage(thisSimulationTest, String.valueOf(segmentsTimestamped.get(i).get(j).get("fname")));
            String thisFname = (String) segmentsTimestamped.get(i).get(j).get("fname");
            Bitmap thisScreenshotBitmap = (Bitmap) bitmapsObject.get(thisFname);//BitmapFactory.decodeFile(thisScreenshotFile.getPath());
            int thisWhitespacePixel = getWhitespacePixel(
                  Args(A("bitmap", thisScreenshotBitmap))
                  );
            DividerSet thisVisualComponents = new DividerSet(


Args(
                  A("bitmap", thisScreenshotBitmap),
                  A("orientation", "v"),
                  A("scaleMinor", 0.1),
                  A("absorbMinimums", false),
                  A("whitespaceThreshold", 0.1),
                  A("colourPaletteThreshold", 0.05),
                  A("scanUntil", 1.0),
                  A("retainMinimums", true),
                  A("minDividerApproach", "complex")
            )







                  );
            // TODO up from 0.01
            List<Bitmap> thisImageDividers = (List<Bitmap>) thisVisualComponents.dividerImages;
            List<HashMap<String, Integer>> thisImageDividerBounds = (List<HashMap<String, Integer>>) thisVisualComponents.dividers;
            elapsedTime = System.currentTimeMillis();


            Bitmap thisScreenshotBitmapReduced = Bitmap.createScaledBitmap(thisScreenshotBitmap, 220, (int) Math.round(thisScreenshotBitmap.getHeight()/thisScreenshotBitmap.getWidth()*220), false);
            DividerSet thisVisualComponentsReduced = new DividerSet( // TODO this is shared with sift event




            Args(
                  A("bitmap", thisScreenshotBitmapReduced),
                  A("orientation", "v"),
                  A("scaleMinor", 0.1),
                  A("absorbMinimums", false),
                  A("whitespaceThreshold", 0.1),
                  A("colourPaletteThreshold", 0.05),
                  A("scanUntil", 0.2),
                  A("retainMinimums", true),
                  A("minDividerApproach", "complex")

            ));



            List<Bitmap> thisImageDividersReduced = thisVisualComponentsReduced.dividerImages;

            FacebookScreenshot result = new FacebookScreenshot(Args(
                        A("dividers", thisImageDividersReduced),
                        A("whitespacePixel", thisWhitespacePixel),
                        A("tolerancePictogramDiff", 0.25),
                        A("referenceStencilsPictograms", pictogramsReference),
                        A("h", thisScreenshotBitmap.getHeight())));

            segmentsTimestamped.get(i).get(j).put("inFacebook", result.inFacebook);
            Log.i(TAG,  "\t\tFacebookScreenshot "+j+" is within Facebook:"+result.inFacebook);
            segmentsTimestamped.get(i).get(j).put("image", thisScreenshotBitmap); // TODO put back into block below
            boolean inFacebook = (boolean) result.inFacebook;
            if (inFacebook) {
               segmentsTimestamped.get(i).get(j).put("dividerBounds", thisImageDividerBounds);
               HashMap<String, Integer> dimensions = new HashMap<>();
               dimensions.put("width", thisScreenshotBitmap.getWidth());
               dimensions.put("height", thisScreenshotBitmap.getHeight());
               segmentsTimestamped.get(i).get(j).put("dimensions", dimensions);
               segmentsTimestamped.get(i).get(j).put("dividers", thisImageDividers);
               segmentsTimestamped.get(i).get(j).put("whitespacePixel", thisWhitespacePixel);
               Iterator<String> keys = ((JSONObject) result.statistics).keys();
               while(keys.hasNext()) {
                  String key = keys.next();
                  segmentsTimestamped.get(i).get(j).put(key, ((JSONObject) result.statistics).get(key));
               }
            }
            if (Verbose) {
               Log.i(TAG,  "\t\t\tFacebookScreenshot within Facebook: "+inFacebook);
            }
         }

         File thisSegmentsTimestampsDirName = filePath(asList(MainActivity.getMainDir(thisContext).getAbsolutePath(), "debug", "0_timestamps"));
         if (DEBUG) {
            if (!thisSegmentsTimestampsDirName.exists()) { thisSegmentsTimestampsDirName.mkdirs(); }
         }

         File thisTimstampsDirName = filePath(asList(thisSegmentsTimestampsDirName.getAbsolutePath(), "segment-"+i));
         if (DEBUG) {
            if (!thisTimstampsDirName.exists()) { thisTimstampsDirName.mkdirs(); }
            for (int j = 0; j < segmentsTimestamped.get(i).size(); j ++) {
               Bitmap thisBitmap = (Bitmap)segmentsTimestamped.get(i).get(j).get("image");
               try (FileOutputStream out = new FileOutputStream(
                     filePath(asList(thisTimstampsDirName.getAbsolutePath(), "raw-" + j + ".png")).getAbsolutePath())) {
                  thisBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
               } catch (IOException e) {}
            }
         }

         // Filter out non-Facebook frames
         segmentsTimestamped.set(i,segmentsTimestamped.get(i).stream().filter(x -> {
            try {
               return (boolean) x.get("inFacebook");
            } catch (JSONException e) {
               throw new RuntimeException(e);
            }
         }).collect(Collectors.toList()));


         if ((segmentsTimestamped.get(i).size() > 0)/* (segmentsTimestamped.get(i).stream().anyMatch(x -> {
               try {
                  return (boolean) x.get("inFacebook");
               } catch (JSONException e) {
                  return false;
               }
            }))*/) {
            consistentBounds.add(determineConsistentBounds(segmentsTimestamped.get(i), false));
            if (Verbose) {
               Log.i(TAG, "\t\tConsistent bounds have been defined: ");
               Log.i(TAG, "\t\t\t"+consistentBounds.get(consistentBounds.size()-1));
            }

            for (int j = 0; j < segmentsTimestamped.get(i).size(); j ++) {
               segmentsTimestamped.get(i).get(j).remove("image");
            }
            Log.i(TAG, "pretimestamps XXX Elapsed time: " + Math.abs((System.currentTimeMillis()) - elapsedTime));


            List<JSONObject> segmentsStitched = segmentsStitch(segmentsTimestamped.get(i), consistentBounds.get(consistentBounds.size()-1), thisContext);
            Log.i(TAG, "stitched size: "+segmentsStitched.size());
            for (int j = 0; j < segmentsStitched.size(); j ++) {
               // TODO - make safe
               Integer heightOfSegment = ((HashMap<String, Integer>) ((JSONObject) ((List<JSONObject>) segmentsStitched.get(j).get("screenshots")).get(0)).get("dimensions")).get("height");

               JSONObject outputFromSegmentation = segmentWithoutOCR(segmentsStitched.get(j));
               List<List<Integer>> analysisDS = (List<List<Integer>>) outputFromSegmentation.get("analysisDSReduced");
               Integer iiTransported = (Integer) outputFromSegmentation.get("ii");
               List<JSONObject> adsForSubmission = extractAds(segmentsStitched.get(j), analysisDS, iiTransported, pictogramsReference, thisContext, heightOfSegment);

               File thisOutputFolder = filePath(asList(MainActivity.getMainDir(thisContext).getAbsolutePath(), "debug", "output"));
               File thisSegmentDirName = filePath(asList((thisOutputFolder.getAbsolutePath()), "segment-"+ii));
               if (DEBUG) {
                  if (!thisOutputFolder.exists()){ thisOutputFolder.mkdirs(); }
                  if (!thisSegmentDirName.exists()){ thisSegmentDirName.mkdirs(); }
               }

               for (int h = 0; h < adsForSubmission.size(); h ++) {
                  File thisAdDirName = filePath(asList((thisSegmentDirName.getAbsolutePath()), "ad-" + h));
                  File thisAdDividersDirName = filePath(asList((thisAdDirName.getAbsolutePath()), "dividers"));
                  if (DEBUG) {
                     if (!thisAdDirName.exists()) { thisAdDirName.mkdirs(); }
                     if (!thisAdDividersDirName.exists()) { thisAdDividersDirName.mkdirs(); }
                  }

                  HashMap<Integer, Bitmap> dividerImages = (HashMap<Integer, Bitmap>) adsForSubmission.get(h).get("divider_images");

                  // content is chunked - start
                  int maxHeightPerChunk = 1500;
                  int chunkIndex = 0;
                  int cumulativeChunkHeight = 0;
                  List<List<Bitmap>> chunkContainer = new ArrayList<>();
                  List<List<JSONObject>> chunkContainerHeights = new ArrayList<>();

                  for (Iterator<Integer> iterG = dividerImages.keySet().iterator(); iterG.hasNext(); ) {
                     Integer k = iterG.next();

                     if (cumulativeChunkHeight > maxHeightPerChunk) {
                        chunkIndex ++;
                        cumulativeChunkHeight = 0;
                     }

                     if ((chunkIndex+1) > chunkContainer.size()) {
                        chunkContainer.add(new ArrayList<>());
                        chunkContainerHeights.add(new ArrayList<>());
                     }

                     cumulativeChunkHeight += dividerImages.get(k).getHeight();
                     chunkContainer.get(chunkIndex).add(dividerImages.get(k));
                     JSONObject thisJSON = new JSONObject();
                     thisJSON.put("key", k);
                     thisJSON.put("height", dividerImages.get(k).getHeight());
                     chunkContainerHeights.get(chunkIndex).add(thisJSON);
                  }
                  // content is chunked - end
                  adsForSubmission.get(h).put("chunk_container_heights", chunkContainerHeights);

                  List<Bitmap> chunkedBitmaps = new ArrayList<>();
                  for (List<Bitmap> bitmaps : chunkContainer) {
                     chunkedBitmaps.add(combineImagesList(
                           Args(
                                 A("listOfBitmaps", bitmaps),
                                 A("orientation", "v")
                           )
                           ));
                  }

                  if (DEBUG) {
                     int jj = 0;
                     for (Bitmap chunkedBitmap : chunkedBitmaps) {
                        try (FileOutputStream out = new FileOutputStream(
                              filePath(asList(thisAdDividersDirName.getAbsolutePath(), "chunk-" + jj + ".png")).getAbsolutePath())) {
                           chunkedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        } catch (IOException e) {
                        }
                        jj++;
                     }
                     for (Iterator<Integer> iterD = dividerImages.keySet().iterator(); iterD.hasNext(); ) {
                        Integer q = iterD.next();
                        try (FileOutputStream out = new FileOutputStream(
                              filePath(asList(thisAdDividersDirName.getAbsolutePath(), "divider-"+q+".png")).getAbsolutePath())) {
                           dividerImages.get(q).compress(Bitmap.CompressFormat.PNG, 100, out);
                        } catch (IOException e) {}
                     }
                  }

                  adsForSubmission.get(h).remove("divider_images");

                  if (DEBUG) {
                     Writer output;
                     Gson gson = new Gson();
                     File file = filePath(asList(thisAdDirName.getAbsolutePath(), "metadata.json"));
                     output = new BufferedWriter(new FileWriter(file));
                     output.write(gson.toJson(adsForSubmission.get(h)));
                     output.close();
                  }

                  JSONObject thisOutputI = new JSONObject();
                  thisOutputI.put("json", adsForSubmission.get(h));
                  thisOutputI.put("chunks", chunkedBitmaps);
                  thisOutput.add(thisOutputI);

               }
               nAdsFound += adsForSubmission.size();
               ii ++;
            }
         }
      }
      Log.i(TAG, "processScreenshots XXX Elapsed time: " + Math.abs((System.currentTimeMillis()) - elapsedTimeTotal));
      return thisOutput;
   }


}
