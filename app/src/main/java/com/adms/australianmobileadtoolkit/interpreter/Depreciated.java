package com.adms.australianmobileadtoolkit.interpreter;

import static com.adms.australianmobileadtoolkit.Arguments.A;
import static com.adms.australianmobileadtoolkit.Arguments.Args;
import static com.adms.australianmobileadtoolkit.Common.BooleanIndexOfN;
import static com.adms.australianmobileadtoolkit.Common.binAsAverages;
import static com.adms.australianmobileadtoolkit.Common.filePath;
import static com.adms.australianmobileadtoolkit.Common.makeDirectory;
import static com.adms.australianmobileadtoolkit.Common.optionalGetDouble;
import static com.adms.australianmobileadtoolkit.Common.weightedHashMap;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.colourListUniformity;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.colourPalette;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.colourQuantizeBitmap;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.colourToHex;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.combineImagesList;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.cropWhitespace;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.dividerBoundOffsets;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.dividerWhitespaceAlternations;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.dominantColourInImage;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.getWhitespacePixel;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.imageToPictogram;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.imageToStencil;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.isRowWhitespace;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.isWhitespace;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.isWhitespaceLocalDifferencesSubFunction;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.pictogramSimilarity;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.pictogramSimilarityV2;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.pixelDifference;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.pixelDifferencePercentage;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.pixelsAtAxisI;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.stencilSimilarity;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.stencilToString;
import static com.adms.australianmobileadtoolkit.Settings.DEBUG;
import static java.util.Arrays.asList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
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
   public static JSONObject areScreenshotsLinkedByScroll(List<JSONObject> screenshots, int maxDifferencePixels, double tolerance, int minMatches, int lastOffset) throws JSONException {
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

      Integer screenshotHeight = (Integer) ((HashMap<String, Integer>) screenshots.get(0).get("dimensions")).get("height");



      Integer dividersExcludedAOffset = (int) dividersExcludedA.stream().map(x -> x.getHeight()).mapToDouble(x -> x).sum();
      Integer dividersExcludedBOffset = (int) dividersExcludedB.stream().map(x -> x.getHeight()).mapToDouble(x -> x).sum();
      HashMap<Integer, Integer> nOfMatches = new HashMap<>();
      List<List<Integer>> inputListIndices = new ArrayList<>();
      for (int i = 0; i < dividersRetainedA.size(); i ++) {
         int AH = (dividersRetainedA.get(i).getHeight());

         for (int j = 0; j < dividersRetainedB.size(); j ++) {
            //if ((!(((List<Double>) screenshots.get(0).get("whitespace")).get(i) < 0.025)) && (!(((List<Double>) screenshots.get(1).get("whitespace")).get(j) < 0.025))) {
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

                           if (!nOfMatches.containsKey(i)) {
                              nOfMatches.put(i, 0);
                           }
                           nOfMatches.put(i, nOfMatches.get(i)+1);

                           if (!nOfMatches.containsKey(j)) {
                              nOfMatches.put(j, 0);
                           }
                           nOfMatches.put(j, nOfMatches.get(j)+1);

                           inputListIndices.add(Arrays.asList(i, j));

                        }
                     }
                  }


            //}
         }
      }

      // i was compared 10 times

      HashMap<Integer, Double> weightedOffsets = weightedHashMap(
            matchingDividers.stream().map(x -> {
               try {
                  return ((Integer) x.get("offset"));
               } catch (JSONException e) {
                  throw new RuntimeException(e);
               }
            }).collect(Collectors.toList()), inputListIndices, nOfMatches);
      List<Double> inputList = (List<Double>) weightedOffsets.keySet().stream().map(Integer::doubleValue).collect(Collectors.toList());

      Log.i(TAG, "weightedOffsets: " + weightedOffsets);
      Log.i(TAG, "inputList: " + inputList);

      HashMap<Double, List<Double>> groups = binAsAverages(Args(
                                 A("input", inputList), A("likeness", (double) maxDifferencePixels)));
      Log.i(TAG, "groups: " + groups);

      HashMap<Double, List<Double>> groupsWeighted = new HashMap<>();
      for (Double key : groups.keySet()) {
         List<Double> thisEntries = groups.get(key);
         List<Double> cumulation = new ArrayList<>();
         for (Double entry : thisEntries) {
            cumulation.add(weightedOffsets.get((int) Math.round(entry)));
         }
         groupsWeighted.put(key, cumulation);
      }


      Log.i(TAG, "groupsWeighted: " + groupsWeighted);

      HashMap<Integer, Double> groupsReducedFurther = new HashMap<>();

      for (Double key : groups.keySet()) {
         groupsReducedFurther.put(key.intValue(), Objects.requireNonNull(groupsWeighted.get(key)).stream().mapToDouble(x->x).sum());
      }
      Log.i(TAG, "groupsReducedFurther: " + groupsReducedFurther);


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

      Log.i(TAG, "offsetsReducedFurther: " + offsetsReducedFurther);
      int trueOffset = ((groupsReducedFurther.isEmpty()) ? -10000 : Collections.max(groupsReducedFurther.entrySet(), HashMap.Entry.comparingByValue()).getKey());

      if (trueOffset != -10000) {
         if (/*(Math.abs(trueOffset) > (screenshotHeight*0.3)) ||*/ (Math.abs(trueOffset - lastOffset) > (screenshotHeight*0.3))) {
            trueOffset = -10000;
         }
      }

      if (trueOffset != -10000) {
         List<Integer> candidates = new ArrayList<>();
         for (Integer g : groupsReducedFurther.keySet()) {
            if (Math.abs(groupsReducedFurther.get(g)-groupsReducedFurther.get(trueOffset)) < 0.000001) {
               candidates.add(g);
            }
         }
         Log.i(TAG, "candidates: " + candidates);

         if (candidates.size() > 1) {
            int minAbsValue = 10000;
            for (Integer c: candidates) {
               if (Math.abs(c) < minAbsValue) {
                  minAbsValue = Math.abs(c);
                  trueOffset = c;
               }
            }
         }

      }


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

   public static void areScreenshotsLinkedByScrollV2(List<JSONObject> screenshots, int maxDifferencePixels, double tolerance, int minMatches) throws JSONException {
      // take both screenshots
      // for both, determine the regions that are predominantly whitespace, and that are not
      // cross compare regions to determine if there are overlappings

   }


   public static int randInt(int min, int max) {
      return (new Random()).nextInt((max - min) + 1) + min;
   }

   public static boolean isWhitespaceIndicator(int number) {
      return (number < -1); // Deliberately set past -1 (to avoid confusion with -1)
   }

   public static int generateWhitespaceIndicator() {
      return (-randInt(2, 10000)); // Deliberately set bound past 2 (to avoid confusion with -1)
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

      int lastOffset = 0;
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
                              A("bitmap", Bitmap.createScaledBitmap(rawDividers.get(h), 64, 16, false)),
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
            JSONObject result = areScreenshotsLinkedByScroll(asList(thisSegment.get(i), thisSegment.get(i+1)), 5, 0.15, 2, lastOffset); // TODO - maybe adjust to three
            Log.i(TAG, "areScreenshotsLinkedByScroll: "+result);
            offset = (int) result.get("trueOffset");
            matchingDividers = (List<JSONObject>) result.get("trueMatchingDividers");
         }
         outputSegment.add(i);
         if (offset != -10000) {
            lastOffset = offset;
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
               analysisDS.get(j).set(k, Collections.singletonList(generateWhitespaceIndicator()));
            }
         }
      }

      for (int j = 0; j < analysisDS.size(); j ++) {
         analysisDSReduced.add(new ArrayList<>());
         for (int k = 0; k < analysisDS.get(j).size(); k ++) {
            analysisDSReduced.get(j).add(generateWhitespaceIndicator());
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
            if ((!((analysisDS.get(j).get(k).size() != 0) && (isWhitespaceIndicator(analysisDS.get(j).get(k).get(0)))))
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

                        debugByDivider.put("matchResultIndication:" + sponsoredTextExposure.get(i)+ ":" + sponsoredTextAlt.get(j), false);
                        debugByDivider.put("matchResult:" + sponsoredTextExposure.get(i) + sponsoredTextAlt.get(j), matchResult);

                        int[][] thisStencil = ((Stencil) pictogramsReference.get("facebook" + sponsoredTextExposure.get(i) + "Sponsored" + sponsoredTextAlt.get(j))).getStencil();
                        int[][] thatStencil = potentialSponsoredTextPictogram.getStencil();

                        Log.i(TAG,  "\t-> Match result: " + matchResult);
                        Log.i(TAG, stencilToString(thisStencil));
                        Log.i(TAG, stencilToString(thatStencil));


                        if (matchResult > 0.75) {
                           found = true;
                           debugByDivider.put("matchResultIndication:" + sponsoredTextExposure.get(i) + ":" + sponsoredTextAlt.get(j), true);
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
               try { matchResultIndicationLight = (Boolean) debugByDivider.get("matchResultIndication:Light:"); } catch (Exception e) {}
               try { matchResultIndicationLight = (Boolean) debugByDivider.get("matchResultIndication:Light:Alt"); } catch (Exception e) {}
               Boolean matchResultIndicationDark = false;
               try { matchResultIndicationDark = (Boolean) debugByDivider.get("matchResultIndication:Dark:"); } catch (Exception e) {}
               try { matchResultIndicationDark = (Boolean) debugByDivider.get("matchResultIndication:Dark:Alt"); } catch (Exception e) {}

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
            dividersAround = dividersAround.stream().filter(d -> (!isWhitespaceIndicator(d))).collect(Collectors.toList());
            List<Integer> prematureCutIndices = Stream.concat(xIndices.stream(), yIndices.stream().filter(d -> (d != xIndex)).collect(Collectors.toList()).stream()).collect(Collectors.toList());
            List<Integer> dividersAroundIndicated = dividersAround.stream().map(d ->{if (!prematureCutIndices.contains(d)) { return d;} {return generateWhitespaceIndicator();}}).collect(Collectors.toList());
            if (dividersAroundIndicated.stream().anyMatch(Depreciated::isWhitespaceIndicator)) {
               Integer whitespaceConveyer = -10000;
               List<Integer> dividersAroundIndicatedWhitespaceConveyed = dividersAroundIndicated.stream().map(y -> {return (isWhitespaceIndicator(y) ? whitespaceConveyer : y);}).collect(Collectors.toList());
               dividersAround = ((thisCase == "header") ?
                     dividersAround.subList(0, dividersAroundIndicatedWhitespaceConveyed.indexOf(whitespaceConveyer)+1) : dividersAround.subList(dividersAroundIndicatedWhitespaceConveyed.lastIndexOf(whitespaceConveyer), dividersAround.size()));
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

   public static List<Bitmap> verySimpleDividers(Bitmap thisBitmap) {
      List<HashMap<String, Integer>> colourPalettes = new ArrayList<>();
      for (int i = 0; i < thisBitmap.getHeight(); i ++) {
         colourPalettes.add(colourPalette(Args(A("sample", pixelsAtAxisI(thisBitmap, "v", i)))));
      }
      List<List<Integer>> bounds = new ArrayList<>();
      List<Integer> currentBound = new ArrayList<>();
      currentBound.add(0);
      for (int i = 0; i < thisBitmap.getHeight()-1; i ++) {
         HashMap<String, Integer> mapI = colourPalettes.get(i);
         HashMap<String, Integer> mapJ = colourPalettes.get(i+1);
         int dominantColourForI = Color.parseColor(Collections.max(mapI.entrySet(), Map.Entry.comparingByValue()).getKey());
         int dominantColourForJ = Color.parseColor(Collections.max(mapJ.entrySet(), Map.Entry.comparingByValue()).getKey());

         if ((dominantColourForI != dominantColourForJ) || (i == thisBitmap.getHeight()-2)) {
            currentBound.add(i);
            if (currentBound.size() == 2) {
               bounds.add(currentBound);
               currentBound = new ArrayList<>();
               currentBound.add(i);
            }
         }
      }
      bounds.get(bounds.size()-1).set(1, thisBitmap.getHeight()-1);
      List<Bitmap> thisDividerImages = new ArrayList<>();
      for (List<Integer> b : bounds) {
         try {
            thisDividerImages.add(Bitmap.createBitmap(thisBitmap, 0, b.get(0), thisBitmap.getWidth() - 1, Math.abs(b.get(0) - b.get(1))));
         } catch (Exception e) {}
      }
      return thisDividerImages;
   }

   public static Integer rangesOverlap(int aMin, int aMax, int bMin, int bMax) {
      return Math.max(0, Math.min(aMax, bMax) - Math.max(aMin, bMin) + 1);
   }

   public static List<List<Integer>> combineRanges(List<List<Integer>> derivedCutPositions) {
      // aggregate derived cut positions for all cuts that were found
      // if two or more are overlapping, then merge (and remove original candidates)
      // if non-overlapping, make disjoint
      // get min val of all that overlap with
      // get max val of all that overlap with it
      // preserve those that dont overlap with it
      List<List<Integer>> derivedCutPositionsCombined = new ArrayList<>();
      for (int i = 0; i < derivedCutPositions.size(); i++) {
         List<List<Integer>> overlapping = new ArrayList<>();
         List<List<Integer>> nonoverlapping = new ArrayList<>();
         for (int j = 0; j < derivedCutPositionsCombined.size(); j++) {
            int aMin = derivedCutPositions.get(i).get(0);
            int aMax = derivedCutPositions.get(i).get(1);
            int bMin = derivedCutPositionsCombined.get(j).get(0);
            int bMax = derivedCutPositionsCombined.get(j).get(1);
            if (rangesOverlap(aMin, aMax, bMin, bMax) > 0) {
               overlapping.add(derivedCutPositionsCombined.get(j));
            } else {
               nonoverlapping.add(derivedCutPositionsCombined.get(j));
            }
         }
         overlapping.add(derivedCutPositions.get(i));
         Integer minOfOverlapping = null;
         Integer maxOfOverlapping = null;
         for (List<Integer> x : overlapping) {
            if (minOfOverlapping == null) {
               minOfOverlapping = x.get(0);
            }
            if (maxOfOverlapping == null) {
               maxOfOverlapping = x.get(1);
            }
            if (x.get(0) < minOfOverlapping) {
               minOfOverlapping = x.get(0);
            } else if (x.get(1) > maxOfOverlapping) {
               maxOfOverlapping = x.get(1);
            }
         }
         if ((minOfOverlapping != null) && (maxOfOverlapping != null)) {
            derivedCutPositionsCombined = Arrays.asList(Arrays.asList(minOfOverlapping, maxOfOverlapping));
         }
         derivedCutPositionsCombined = Stream.concat(
                 derivedCutPositionsCombined.stream(), nonoverlapping.stream()).collect(Collectors.toList());
      }

      return derivedCutPositionsCombined;
   }

   // determine the parts of the image that are not images
   // take sample size of N pixels across width vs full y
   // if for any row, pixels are dominantly whitespace, then row is definitely non-image
   // TODO in future, adapt to run quicker by not doing full y on first run, but full y on extremes of boundaries only
   public static JSONObject determineImageRegions(int WSColor, Bitmap thisScreenshot) throws JSONException {
      List<List<Integer>> rowSamples = new ArrayList<>();
      List<Double> rawPcts = new ArrayList<>();
      Boolean on = false;
      List<Integer> currentEntry = new ArrayList<>();
      for (int i = 0; i < thisScreenshot.getHeight(); i ++) {
         int finalI = i;
         int sample_n = 30;
         List<Integer> rowSample = IntStream.range(0, sample_n).boxed().collect(Collectors.toList()).stream().map(x -> thisScreenshot.getPixel((int) Math.floor(x*((double) thisScreenshot.getWidth() /sample_n)), finalI)).collect(Collectors.toList());
         Double pixelDiffAvg = optionalGetDouble(rowSample.stream().map(x -> pixelDifferencePercentage(WSColor, x)).collect(Collectors.toList()).stream().mapToDouble(x->x).average());
         rawPcts.add(pixelDiffAvg);
         if (pixelDiffAvg < 0.25) {
            if (!on) {
               on = true;
               currentEntry = new ArrayList<>();
               currentEntry.add(i);
            }
         } else {
            if (on) {
               on = false;

               if (Math.abs(currentEntry.get(0)-i) > 1) {
                  currentEntry.add(i);
                  rowSamples.add(currentEntry);
               }
            }
         }
      }

      JSONObject output = new JSONObject();

      output.put("rowSamples", rowSamples);
      output.put("rawPcts", rawPcts);

      return output;
   }

   public static List<JSONObject> extractAds(JSONObject segmentStitched, List<List<Integer>> analysisDS, Integer ii, HashMap<String, Object> pictogramsReference, Context thisContext, int totalHeight) throws JSONException, IOException {
      long elapsedTime = System.currentTimeMillis();
      List<JSONObject> anticipatedAds = new ArrayList<>();

      Log.i(TAG, "analysisDS: "+ analysisDS);

      List<List<Integer>> preservedAnalysisDS = new ArrayList<>();
      for (int screenshotI = 0; screenshotI < analysisDS.size(); screenshotI ++) {
         preservedAnalysisDS.add(new ArrayList<>());
         for (int dividerI = 0; dividerI < analysisDS.get(screenshotI).size(); dividerI ++) {
            preservedAnalysisDS.get(screenshotI).add(analysisDS.get(screenshotI).get(dividerI));
         }
      }


      if (DEBUG) {
         File dir = filePath(asList(MainActivity.getMainDir(thisContext).getAbsolutePath(), "debug", "screenshots_unexcludewd"));
         if (!dir.exists()) {
            dir.mkdirs();
         }

         for (int screenshotI = 0; screenshotI < analysisDS.size(); screenshotI ++) {

            List<Bitmap> imagesOfCurrentScreenshot = new ArrayList<>();
            for (int i = 0; i < analysisDS.get(screenshotI).size(); i++) {
               imagesOfCurrentScreenshot.add(retrieveRetainedDivider(segmentStitched, screenshotI, i));
            }

            Bitmap stitchedImage = combineImagesList(Args(A("listOfBitmaps", imagesOfCurrentScreenshot), A("orientation", "v")));

            String fname = screenshotI + ".png";

            try {
               try (FileOutputStream out = new FileOutputStream(
                       filePath(asList(dir.getAbsolutePath(), fname + ".png")).getAbsolutePath())) {
                  stitchedImage.compress(Bitmap.CompressFormat.PNG, 100, out);
               } catch (IOException e) {}
            } catch (Exception e) {}

         }
      }

      // compare and combine ad headers prior to post dividers
      List<Integer> adHeaderDividerIndices = adHeaderDividers(segmentStitched, analysisDS, pictogramsReference, thisContext, totalHeight);

      // project known ad header indices into other screenshots, and record those ad headers that were overtaken
      // the ad header ID (and its position) are both stored in a single entry of this list
      List<HashMap<Integer, List<Integer>>> projectionList = new ArrayList<>(); // screenshot -> adheaderobj -> ad header index - offset position start - offset position end

      for (int screenshotI = 0; screenshotI < analysisDS.size(); screenshotI ++) {
         projectionList.add(new HashMap<>());
         for (Integer adHeaderDividerIndex : adHeaderDividerIndices) {
            List<Integer> thisScreenshot = analysisDS.get(screenshotI);
            Integer adHeaderDividerI = thisScreenshot.indexOf(adHeaderDividerIndex);
            if (adHeaderDividerI != -1) {
               // ad header was found in this screenshot - find its offset bound
               int boundaryStart = 0;
               for (int i = 0; i < adHeaderDividerI; i ++) {
                  boundaryStart += retrieveRetainedDivider(segmentStitched, screenshotI, i).getHeight();
               }
               int boundaryEnd = boundaryStart + retrieveRetainedDivider(segmentStitched, screenshotI, adHeaderDividerI).getHeight();
               projectionList.get(projectionList.size()-1).put(adHeaderDividerIndex, Arrays.asList(boundaryStart, boundaryEnd));
            }
         }
      }
      Log.i(TAG, "projectionList: "+ projectionList);


      // sweep forwards projection
      for (int screenshotI = 0; screenshotI < analysisDS.size()-1; screenshotI ++) { // do all except last
         for (Integer adHeaderDividerIndex : projectionList.get(screenshotI).keySet()) {
            List<Integer> thisAdHeaderIndexObject = projectionList.get(screenshotI).get(adHeaderDividerIndex);
            if (!projectionList.get(screenshotI+1).containsKey(adHeaderDividerIndex)) {
               projectionList.get(screenshotI+1).put(
                       adHeaderDividerIndex,
                       Arrays.asList(
                               thisAdHeaderIndexObject.get(0)-((List<Integer>) segmentStitched.get("offsets")).get(screenshotI),
                               thisAdHeaderIndexObject.get(1)-((List<Integer>) segmentStitched.get("offsets")).get(screenshotI)
                       )
               );
            }
         }
      }

      // sweep backwards projection
      for (int screenshotI = analysisDS.size()-1; screenshotI > 0; screenshotI --) { // do all except last
         for (Integer adHeaderDividerIndex : projectionList.get(screenshotI).keySet()) {
            List<Integer> thisAdHeaderIndexObject = projectionList.get(screenshotI).get(adHeaderDividerIndex);
            if (!projectionList.get(screenshotI-1).containsKey(adHeaderDividerIndex)) {
               projectionList.get(screenshotI-1).put(
                       adHeaderDividerIndex,
                       Arrays.asList(
                               thisAdHeaderIndexObject.get(0)+((List<Integer>) segmentStitched.get("offsets")).get(screenshotI-1),
                               thisAdHeaderIndexObject.get(1)+((List<Integer>) segmentStitched.get("offsets")).get(screenshotI-1)
                       )
               );
            }
         }
      }
      Log.i(TAG, "projectionList: "+ projectionList);


      // remove the overtaken indices from the adHeaderDividerIndicesƒ
      HashMap<Integer, Integer> toBeReplaced = new HashMap<>();
      for (int screenshotI = 0; screenshotI < analysisDS.size(); screenshotI ++) {
         for (Integer thisAdHeaderIndex : projectionList.get(screenshotI).keySet()) {
            List<Integer> thisAdHeaderIndexObject = projectionList.get(screenshotI).get(thisAdHeaderIndex);
            // Find overlapping indices
            int offsetFromTop = 0;
            for (int i = 0; i < analysisDS.get(screenshotI).size(); i ++) {
               int boundaryHeight = retrieveRetainedDivider(segmentStitched, screenshotI, i).getHeight();

               int boundaryDivisor = Math.min(boundaryHeight, Math.abs(thisAdHeaderIndexObject.get(0)-thisAdHeaderIndexObject.get(1)));

               if (((double) rangesOverlap(offsetFromTop, offsetFromTop + boundaryHeight,
                       thisAdHeaderIndexObject.get(0), thisAdHeaderIndexObject.get(1)) /boundaryDivisor) > 0.9) {
                  // do replacement
                  Log.i(TAG, "screenshot "+screenshotI+" divider "+i+" shouldbereplaced with "+thisAdHeaderIndex);
                  if (!Objects.equals(thisAdHeaderIndex, analysisDS.get(screenshotI).get(i))) {
                     toBeReplaced.put(analysisDS.get(screenshotI).get(i), thisAdHeaderIndex);
                     analysisDS.get(screenshotI).set(i,thisAdHeaderIndex);
                  }
                  break; // stop on first match
               }
               offsetFromTop += retrieveRetainedDivider(segmentStitched, screenshotI, i).getHeight();
            }
         }
      }
      // TODO - deal with double replacements
      // TODO - deal with replacing an index with itself
      Log.i(TAG, "toBeReplaced1: "+ toBeReplaced);

      // remove transitives
      boolean passthrough = true;
      while (passthrough) {
         passthrough = false;
         for (Integer adHeaderDividerIndex : toBeReplaced.keySet()) {
            if (toBeReplaced.containsKey(toBeReplaced.get(adHeaderDividerIndex))) {
               if (Objects.equals(toBeReplaced.get(toBeReplaced.get(adHeaderDividerIndex)), adHeaderDividerIndex)) {
                  toBeReplaced.remove(adHeaderDividerIndex);
               } else {
                  toBeReplaced.put(adHeaderDividerIndex, toBeReplaced.get(toBeReplaced.get(adHeaderDividerIndex)));
               }
               passthrough = true;
               break;
            }
         }
      }
      Log.i(TAG, "toBeReplaced2: "+ toBeReplaced);


      // There is a possibility that an index can become circular - if so - break the circular reference
      passthrough = true;
      while (passthrough) {
         passthrough = false;
         for (Integer adHeaderDividerIndex : toBeReplaced.keySet()) {
            if (toBeReplaced.containsValue(adHeaderDividerIndex)) {
               toBeReplaced.remove(adHeaderDividerIndex);
               passthrough = true;
               break;
            }
         }
      }
      Log.i(TAG, "toBeReplaced3: "+ toBeReplaced);

      // remove replaced indices from adHeaderDividerIndices
      adHeaderDividerIndices = adHeaderDividerIndices.stream().filter(x -> (!toBeReplaced.containsKey(x))).collect(Collectors.toList());

      // adjust indices within analysisDS
      for (int screenshotI = 0; screenshotI < analysisDS.size(); screenshotI ++) {
         for (int i = 0; i < analysisDS.get(screenshotI).size(); i ++) {
            if (toBeReplaced.containsKey(analysisDS.get(screenshotI).get(i))) {
               analysisDS.get(screenshotI).set(i, toBeReplaced.get(analysisDS.get(screenshotI).get(i)));
            }
         }
      }









      // find post dividers

      Log.i(TAG, "global offsets: "+((List<Integer>) segmentStitched.get("offsets")).toString());

      // get ws color from ad header
      // stitch dividers below ad header
      // if they contain the post divider band, identify it and report back the divider that has it
      List<Integer> runningIndices = new ArrayList<>();
      Integer retainedWSColor = null;
      HashMap<Integer, List<Integer>> offsetCaptures = new HashMap<>();
      HashMap<Integer, List<List<Integer>>> retainedDividersNew = new HashMap<>();

      List<List<Integer>> dividersToRetain = new ArrayList<>();

      Log.i(TAG, "adHeaderDividerIndices: "+adHeaderDividerIndices);


      for (Integer adHeaderDividerIndex : adHeaderDividerIndices) {
         // Aggregsate once per ad header
         List<List<List<Integer>>> screenshotIndependentDerivedCutDS = new ArrayList<>();
         List<List<List<Integer>>> screenshotIndependentDerivedCutDSCombined = new ArrayList<>();
         List<List<List<Integer>>> screenshotIndependentDerivedCutDSCombinedBackwards = new ArrayList<>();
         List<List<List<Integer>>> screenshotIndependentDerivedCutDSCombinedForwards = new ArrayList<>();
         // post divider offsets keep track of post dividers between screenshots
         List<List<Integer>> postDividerOffsets = new ArrayList<>();
         for (int screenshotI = 0; screenshotI < analysisDS.size(); screenshotI ++) {

            postDividerOffsets.add(new ArrayList<>());

            // If there are post dividers from the previous screenshot, offset them into this screenshot
            if (postDividerOffsets.size() > 1) {
               // Get last index
               int previousIndex = postDividerOffsets.size()-2;
               for (int i = 0; i < postDividerOffsets.get(previousIndex).size(); i ++) {
                  // take each post divider offset and project it into this one
                  int thisPostDividerOffset = postDividerOffsets.get(previousIndex).get(i) - ((List<Integer>) segmentStitched.get("offsets")).get(screenshotI-1);
                  postDividerOffsets.get(postDividerOffsets.size()-1).add(thisPostDividerOffset);
               }
            }

            Bitmap thisScreenshotBitmap = combineImagesList(Args(
                    A("listOfBitmaps", ((List<Bitmap>) ((List<JSONObject>) segmentStitched.get("screenshots")).get(screenshotI).get("dividersRetained"))),
                    A("orientation", "v")));


            Log.i(TAG, "Postdividerscan screenshot: "+ (ii + screenshotI));
            Log.i(TAG, "Postdividerscan (post divider offsets): "+ postDividerOffsets);
            List<List<Integer>> derivedCutPositions = new ArrayList<>();
            //Log.i(TAG, "xxx1 : " + screenshotI);
            List<Integer> thisScreenshot = analysisDS.get(screenshotI);
            Integer adHeaderDividerI = thisScreenshot.indexOf(adHeaderDividerIndex);
            Log.i(TAG, "Postdividerscan adHeaderDividerI: "+ (adHeaderDividerI));
            Integer offsetFromTop = 0;
                    List<Integer> indicesAfterAdHeader = new ArrayList<>();
            Integer cutPosition = null;
            if (adHeaderDividerI != -1) {
               //Log.i(TAG, "xxx2 : " + screenshotI);
               // ad header was found - proceed
               indicesAfterAdHeader = thisScreenshot.subList(adHeaderDividerI, thisScreenshot.size()-1);
               for (int i = 0; i < adHeaderDividerI; i ++) {
                  offsetFromTop += retrieveRetainedDivider(segmentStitched, screenshotI, i).getHeight();
               }
               cutPosition = adHeaderDividerI;
            } else {
               //Log.i(TAG, "xxx3 : " + screenshotI);
               // when the ad header is not found, use the running indices to find it
               for (Integer x : runningIndices) {
                  if (thisScreenshot.contains(x)) {
                     indicesAfterAdHeader = thisScreenshot.subList(thisScreenshot.indexOf(x), thisScreenshot.size()-1);
                     cutPosition = thisScreenshot.indexOf(x);
                     for (int i = 0; i < thisScreenshot.indexOf(x); i ++) {
                        offsetFromTop += retrieveRetainedDivider(segmentStitched, screenshotI, i).getHeight();
                     }
                     break;
                  }
               }
            }

            // if we found indices
            boolean postDividerWasFound = false;
            if ((!indicesAfterAdHeader.isEmpty()) && (cutPosition != null)) {


               //Log.i(TAG, "xxx4 : " + screenshotI);
               // Add indices to running list for later usage
               for (Integer x : indicesAfterAdHeader) {
                  if (!runningIndices.contains(x)) {
                     runningIndices.add(x);
                  }
               }
               // get the whitespace color if we don't have it
               if (retainedWSColor == null) {
                  //Log.i(TAG, "xxx5 : " + screenshotI);
                  Bitmap adHeaderDivider = retrieveRetainedDivider(segmentStitched, screenshotI, adHeaderDividerI);
                  retainedWSColor = getWhitespacePixel(Args(A("bitmap", adHeaderDivider)));
                  //Log.i(TAG, "xxx5 : wscolor: " + colourToHex(retainedWSColor));
               }

               // if we have a whitespace color to work with
               if (retainedWSColor != null) {
                 // Log.i(TAG, "xxx6 : " + screenshotI);

                  // stitch the images of the current screenshot
                  List<Bitmap> imagesOfCurrentScreenshot = new ArrayList<>();
                  for (int i = cutPosition; i < thisScreenshot.size(); i ++) {
                     imagesOfCurrentScreenshot.add(retrieveRetainedDivider(segmentStitched, screenshotI, i));
                  }

                  Bitmap stitchedImage = combineImagesList(Args(A("listOfBitmaps", imagesOfCurrentScreenshot), A("orientation", "v")));

                  // pick three vertically spaced pixels and go down vertically
                  Double POST_DIVIDER_SPACING_RATIO = 0.01;
                  Double POST_DIVIDER_COLOUR_DIFFERENCE = 0.04;
                  Double POST_DIVIDER_COLOUR_UNIFORMITY = 0.1;
                  int threePixelVerticalspacing = (int) Math.round(POST_DIVIDER_SPACING_RATIO*totalHeight);//Math.round(stitchedImage.getHeight()*POST_DIVIDER_SPACING_RATIO);
                  Log.i(TAG, "threePixelVerticalspacing: "+threePixelVerticalspacing);
                  for (int i = 0; i < stitchedImage.getHeight(); i ++) {
                     if (i+(threePixelVerticalspacing*2) <= stitchedImage.getHeight()-1) {

                        Integer finalRetainedWSColor = retainedWSColor;
                        int finalI = i;

                        // get 20 pixels spaced out across the width of the image
                        List<Integer> p1C = IntStream.range(0, 5).boxed().collect(Collectors.toList()).stream().map(x -> stitchedImage.getPixel((int) Math.floor(x*((double) stitchedImage.getWidth() /5)), finalI)).collect(Collectors.toList());
                        List<Integer> p2C = IntStream.range(0, 5).boxed().collect(Collectors.toList()).stream().map(x -> stitchedImage.getPixel((int) Math.floor(x*((double) stitchedImage.getWidth() /5)), finalI+threePixelVerticalspacing)).collect(Collectors.toList());
                        List<Integer> p3C = IntStream.range(0, 5).boxed().collect(Collectors.toList()).stream().map(x -> stitchedImage.getPixel((int) Math.floor(x*((double) stitchedImage.getWidth() /5)), finalI+(threePixelVerticalspacing*2))).collect(Collectors.toList());

                        Double p1 = optionalGetDouble(p1C.stream().map(x -> pixelDifferencePercentage(finalRetainedWSColor, x)).collect(Collectors.toList()).stream().mapToDouble(x->x).average());
                        Double p2 = optionalGetDouble(p2C.stream().map(x -> pixelDifferencePercentage(finalRetainedWSColor, x)).collect(Collectors.toList()).stream().mapToDouble(x->x).average());
                        Double p3 = optionalGetDouble(p3C.stream().map(x -> pixelDifferencePercentage(finalRetainedWSColor, x)).collect(Collectors.toList()).stream().mapToDouble(x->x).average());


                        //Log.i(TAG, "xxx7.1 : " + screenshotI + " : " + i + " : PU : " + p1 + " " + p2 + " " + p3 );

                        // when the 1st and 3rd are the same colour as the ws color, and the middle is of a decent difference to them...
                        if ((p1 < POST_DIVIDER_COLOUR_DIFFERENCE)
                                && (p2 > POST_DIVIDER_COLOUR_DIFFERENCE)
                                    && (p3 < POST_DIVIDER_COLOUR_DIFFERENCE)) {

                           // get 20 pixels spaced out across the width of the image
                           p1C = IntStream.range(0, 100).boxed().collect(Collectors.toList()).stream().map(x -> stitchedImage.getPixel((int) Math.floor(x*((double) stitchedImage.getWidth() /100)), finalI)).collect(Collectors.toList());
                           p2C = IntStream.range(0, 100).boxed().collect(Collectors.toList()).stream().map(x -> stitchedImage.getPixel((int) Math.floor(x*((double) stitchedImage.getWidth() /100)), finalI+threePixelVerticalspacing)).collect(Collectors.toList());
                           p3C = IntStream.range(0, 100).boxed().collect(Collectors.toList()).stream().map(x -> stitchedImage.getPixel((int) Math.floor(x*((double) stitchedImage.getWidth() /100)), finalI+(threePixelVerticalspacing*2))).collect(Collectors.toList());

                              Double p1R = optionalGetDouble(p1C.stream().map(x -> pixelDifferencePercentage(finalRetainedWSColor, x)).collect(Collectors.toList()).stream().mapToDouble(x->x).average());
                              Double p2R = optionalGetDouble(p2C.stream().map(x -> pixelDifferencePercentage(finalRetainedWSColor, x)).collect(Collectors.toList()).stream().mapToDouble(x->x).average());
                              Double p3R = optionalGetDouble(p3C.stream().map(x -> pixelDifferencePercentage(finalRetainedWSColor, x)).collect(Collectors.toList()).stream().mapToDouble(x->x).average());

                           Double p1U = colourListUniformity(p1C);
                           Double p2U = colourListUniformity(p2C);
                           Double p3U = colourListUniformity(p3C);
                           //Log.i(TAG, "xxx7.2 : " + screenshotI + " : " + i + " : PR  : " + p1R + " " + p2R + " " + p3R );
                           //Log.i(TAG, "xxx7.2 : " + screenshotI + " : " + i + " : PU  : " + p1U + " " + p2U + " " + p3U );

                              if (((p1R < POST_DIVIDER_COLOUR_DIFFERENCE)
                                      && (p2R > POST_DIVIDER_COLOUR_DIFFERENCE)
                                      && (p3R < POST_DIVIDER_COLOUR_DIFFERENCE)) &&
                                      ((p1U < POST_DIVIDER_COLOUR_UNIFORMITY)
                                         && (p2U < POST_DIVIDER_COLOUR_UNIFORMITY)
                                         && (p3U < POST_DIVIDER_COLOUR_UNIFORMITY))) {

                                 int[] i1 = pixelsAtAxisI(stitchedImage, "v", i);
                                 int[] i2 = pixelsAtAxisI(stitchedImage, "v", i + threePixelVerticalspacing);
                                 int[] i3 = pixelsAtAxisI(stitchedImage, "v", i + (threePixelVerticalspacing * 2));

                                 // check the colour palettes of the entire rows to determine that they are consistent (as post dividers should be
                                 Double i1C = pixelDifferencePercentage(Color.parseColor(Collections.max(
                                         colourPalette(Args(A("sample", i1), A("threshold", 0.01))).entrySet(),
                                         Map.Entry.comparingByValue()).getKey()), retainedWSColor);
                                 Double i2C = pixelDifferencePercentage(Color.parseColor(Collections.max(
                                         colourPalette(Args(A("sample", i2), A("threshold", 0.01))).entrySet(),
                                         Map.Entry.comparingByValue()).getKey()), retainedWSColor);
                                 Double i3C = pixelDifferencePercentage(Color.parseColor(Collections.max(
                                         colourPalette(Args(A("sample", i3), A("threshold", 0.01))).entrySet(),
                                         Map.Entry.comparingByValue()).getKey()), retainedWSColor);
                                 Log.i(TAG, "xxx8 : " + screenshotI + " : " + i + " : " + i1C + " " + i2C + " " + i3C);
                                 if ((i1C < POST_DIVIDER_COLOUR_DIFFERENCE)
                                         && (i2C > POST_DIVIDER_COLOUR_DIFFERENCE)
                                         && (i3C < POST_DIVIDER_COLOUR_DIFFERENCE)) {
                                    Log.i(TAG, "xxx7 : " + screenshotI + " : " + i + " : P  : " + p1 + " " + p2 + " " + p3);
                                    Log.i(TAG, "xxx7.2 : " + screenshotI + " : " + i + " : PR  : " + p1R + " " + p2R + " " + p3R );
                                    //Log.i(TAG, "xxx7 : " + screenshotI + " : " + i + " : PU : " + p1U + " " + p2U + " " + p3U);
                                    //Log.i(TAG, "xxx8 : " + screenshotI + " : " + i + " : " + i1C + " " + i2C + " " + i3C);
                                    //Log.i(TAG, "xxx9");
                                    //Log.i(TAG, "i : " + i + " height: " + stitchedImage.getHeight());
                                    //Log.i(TAG, "threePixelVerticalspacing: " + threePixelVerticalspacing);

                                    // identify the cut point and report the x
                                    Integer postDividerY = i;
                                    Log.i(TAG, "Postdividerscan at y position of " + (postDividerY + threePixelVerticalspacing) + " (height is " + stitchedImage.getHeight() + ")");
                                    //Log.i(TAG, "Postdividerfound clear offfset: " + Arrays.asList(offsetFromTop, offsetFromTop + postDividerY));

                                    int intendedLowerBound = offsetFromTop + postDividerY + threePixelVerticalspacing;
                                    derivedCutPositions.add(Arrays.asList(offsetFromTop, intendedLowerBound));
                                    postDividerWasFound = true;


                                    //
                                    int runningHeight = 0;
                                    for (int iii = cutPosition; iii < thisScreenshot.size(); iii ++) {
                                       runningHeight += retrieveRetainedDivider(segmentStitched, screenshotI, iii).getHeight();
                                       if (runningHeight >= i) {
                                          Log.i(TAG, "Postdividerscan divider index is "+iii);
                                          // Add to last index of post divider offsets
                                          postDividerOffsets.get(postDividerOffsets.size()-1).add(i+offsetFromTop);
                                          break;
                                       }
                                    }



                                    if (DEBUG) {
                                       File dir = filePath(asList(MainActivity.getMainDir(thisContext).getAbsolutePath(), "debug", "post_dividers"));
                                       if (!dir.exists()) {
                                          dir.mkdirs();
                                       }
                                       String fname = UUID.randomUUID().toString() + ".png";
                                       //Log.i(TAG, "Postdividerfound creating: " + fname);
                                       try {
                                          try (FileOutputStream out = new FileOutputStream(
                                                  filePath(asList(dir.getAbsolutePath(), fname + ".png")).getAbsolutePath())) {
                                             Bitmap.createBitmap(
                                                     thisScreenshotBitmap,
                                                     0,
                                                     (postDividerY + threePixelVerticalspacing) - 25,
                                                     thisScreenshotBitmap.getWidth(),
                                                     50).compress(Bitmap.CompressFormat.PNG, 100, out);
                                          } catch (IOException e) {
                                          }
                                       } catch (Exception e) {
                                       }
                                    }

                                    // flush the indicesAfterAdHeader and runningIndices
                                    indicesAfterAdHeader = new ArrayList<>();
                                    runningIndices = new ArrayList<>();
                                 }
                              }
                        }
                     }
                  }

                  // TODO if no cuts were derived, manufacture them with whatever we have

                  if (!postDividerWasFound) {
                        Log.i(TAG, "adding w/ Indicesafterheader: "+ indicesAfterAdHeader);
                           int intendedLowerBound = stitchedImage.getHeight() + offsetFromTop;
                           derivedCutPositions.add(Arrays.asList(offsetFromTop, intendedLowerBound));
                  }







                  // TODO - deal with cases of both complete identification of post divider, and latter
                  // TODO - backwards stitching - use offsets
               }
            }


            // if no post divider was found, but we have indices after header
            //

            /////

            List<List<Integer>> derivedCutPositionsCombined = combineRanges(derivedCutPositions);

            // apply aggregated derived cuts to screenshotI datastructure
            screenshotIndependentDerivedCutDS.add(derivedCutPositionsCombined);

         }

         // project the postDividerOffsets into previous screenshots (and trim derived cuts if necessary)
         for (int screenshotI = analysisDS.size()-1; screenshotI > 0; screenshotI --) {
            for (int q = 0; q < postDividerOffsets.get(screenshotI).size(); q ++) {
               // project this into the previous screenshot
               postDividerOffsets.get(screenshotI-1).add(postDividerOffsets.get(screenshotI).get(q) + ((List<Integer>) segmentStitched.get("offsets")).get(screenshotI-1));
            }
         }
         // TODO - collapse the postDividerOffsets
         Log.i(TAG, "postDividerOffsets (after backwards projection):"+postDividerOffsets);

         // TODO - for all screenshots, trim derived cuts by postDividerOffsets
         List<List<List<Integer>>> screenshotIndependentDerivedCutDSAdjusted = new ArrayList<>();
         for (int screenshotI = 0; screenshotI < analysisDS.size(); screenshotI ++) {
            screenshotIndependentDerivedCutDSAdjusted.add(new ArrayList<>());
            for (int derivedCutI = 0; derivedCutI < screenshotIndependentDerivedCutDS.get(screenshotI).size(); derivedCutI ++) {
               List<Integer> thisDerivedCut = screenshotIndependentDerivedCutDS.get(screenshotI).get(derivedCutI);
               int intendedUpperBound = thisDerivedCut.get(0);
               int intendedLowerBound = thisDerivedCut.get(1);
               for (int postDividerOffsetI = 0; postDividerOffsetI < postDividerOffsets.get(screenshotI).size(); postDividerOffsetI++) {
                  if ((postDividerOffsets.get(screenshotI).get(postDividerOffsetI) < intendedLowerBound) && (postDividerOffsets.get(screenshotI).get(postDividerOffsetI) > intendedUpperBound)) {
                     intendedLowerBound = postDividerOffsets.get(screenshotI).get(postDividerOffsetI);
                     break;
                  }
               }
               screenshotIndependentDerivedCutDSAdjusted.get(screenshotI).add(Arrays.asList(intendedUpperBound, intendedLowerBound));
            }
         }

         screenshotIndependentDerivedCutDS = screenshotIndependentDerivedCutDSAdjusted;

         Log.i(TAG, "adHeaderDividerIndices size:" + adHeaderDividerIndices.size());
         Log.i(TAG, "screenshotIndependentDerivedCutDS: "+screenshotIndependentDerivedCutDS);



         List<Integer> offsets = (List<Integer>) segmentStitched.get("offsets");

         Log.i(TAG, String.valueOf(segmentStitched.keys()));


         // instantiate
         for (int i = 0; i < screenshotIndependentDerivedCutDS.size(); i ++) {
            screenshotIndependentDerivedCutDSCombinedBackwards.add(new ArrayList<>(screenshotIndependentDerivedCutDS.get(i)));
            screenshotIndependentDerivedCutDSCombinedForwards.add(new ArrayList<>(screenshotIndependentDerivedCutDS.get(i)));
         }

         // backwards
         for (int j = screenshotIndependentDerivedCutDSCombinedBackwards.size()-2; j >= 0; j --) { // we dont do last index
            // get contents of index after this one
            for (List<Integer> thisCut : screenshotIndependentDerivedCutDSCombinedBackwards.get(j+1)) {
               // for each cut,
               screenshotIndependentDerivedCutDSCombinedBackwards.get(j).add(Arrays.asList(thisCut.get(0) + offsets.get(j), thisCut.get(1) + offsets.get(j)));
            }
         }

         // forwards
         for (int j = 1; j < screenshotIndependentDerivedCutDSCombinedForwards.size()-1; j ++) { // we dont do last index
            // get contents of index after this one
            for (List<Integer> thisCut : screenshotIndependentDerivedCutDSCombinedForwards.get(j-1)) {
               // for each cut,
               screenshotIndependentDerivedCutDSCombinedForwards.get(j).add(Arrays.asList(thisCut.get(0) - offsets.get(j-1), thisCut.get(1) - offsets.get(j-1)));
            }
         }



         Log.i(TAG, "screenshotIndependentDerivedCutDS:" + screenshotIndependentDerivedCutDS);
         Log.i(TAG, "screenshotIndependentDerivedCutDSCombinedBackwards:" + screenshotIndependentDerivedCutDSCombinedBackwards);
         Log.i(TAG, "screenshotIndependentDerivedCutDSCombinedForwards:" + screenshotIndependentDerivedCutDSCombinedForwards);


         for (int  i = 0; i < analysisDS.size(); i ++) {
            screenshotIndependentDerivedCutDSCombined.add(
                    Stream.concat(
                            screenshotIndependentDerivedCutDSCombinedBackwards.get(i).stream(),
                            screenshotIndependentDerivedCutDSCombinedForwards.get(i).stream()
                    ).collect(Collectors.toList()));
         }

         // flatten down further overlaps
         for (int i = 0; i < screenshotIndependentDerivedCutDSCombined.size(); i ++) {
            screenshotIndependentDerivedCutDSCombined.set(i, combineRanges(new ArrayList<>(screenshotIndependentDerivedCutDSCombined.get(i))));
         }

         // Project across screenshots
         // retrieve offsets and project derive cuts across
         // screenshotIndependentDerivedCutDS.add()
         Log.i(TAG, "screenshotIndependentDerivedCutDSCombined: "+ screenshotIndependentDerivedCutDSCombined);


         if (screenshotIndependentDerivedCutDSCombined.size() > 0) {

            // after the offset boundaries are derived, we need to determine if any are disjoint, to avoid grouping
            // together ads that are not the same
            int nOfDisjointAds = 0;
            List<List<Integer>> disjointAdTags = new ArrayList<>();
            for (int screenshotI = 0; screenshotI < analysisDS.size(); screenshotI++) {
               disjointAdTags.add(new ArrayList<>());
               if (screenshotI == 0) {
                  // On the first run, declare the tags (we assume they are disjoint as they weren't merged)
                  for (int j = 0; j < screenshotIndependentDerivedCutDSCombined.get(screenshotI).size(); j++) {
                     disjointAdTags.get(screenshotI).add(j);
                  }
                  nOfDisjointAds = screenshotIndependentDerivedCutDSCombined.get(screenshotI).size();
               } else {
                  // On the second run, we cross-check the entries against those of the previous screenshot
                  for (int j = 0; j < screenshotIndependentDerivedCutDSCombined.get(screenshotI).size(); j++) {
                     // If it overlaps a candidate in the previous screenshot
                     Integer overlappingCandidateIndex = null;
                     for (int k = 0; k < screenshotIndependentDerivedCutDSCombined.get(screenshotI - 1).size(); k++) {
                        List<Integer> rangeA = screenshotIndependentDerivedCutDSCombined.get(screenshotI - 1).get(k);
                        List<Integer> rangeB = screenshotIndependentDerivedCutDSCombined.get(screenshotI).get(j);
                        Integer minRange = Math.min(Math.abs(rangeA.get(0)-rangeA.get(1)), Math.abs(rangeB.get(0)-rangeB.get(1)));
                        Integer thisRangeOverlapLiteral = rangesOverlap(rangeA.get(0), rangeA.get(1), rangeB.get(0), rangeB.get(1));
                        Double thisRangeOverlap = thisRangeOverlapLiteral/(double) minRange;
                        Log.i(TAG, "thisRangeOverlap: c: "+rangeA.get(0)+" : "+rangeA.get(1)+" : "+rangeB.get(0)+" : "+rangeB.get(1));
                        Log.i(TAG, "thisRangeOverlap minRange: "+minRange);
                        Log.i(TAG, "thisRangeOverlap: thisRangeOverlapLiteral: "+ thisRangeOverlapLiteral);
                        Log.i(TAG, "thisRangeOverlap: "+thisRangeOverlap);
                        if (thisRangeOverlap > 0.5) {
                           // derive the match
                           overlappingCandidateIndex = disjointAdTags.get(screenshotI - 1).get(k);
                           break;
                        }
                     }
                     // Instantitate the index if it does not exist
                     if (overlappingCandidateIndex == null) {
                        nOfDisjointAds += 1;
                        overlappingCandidateIndex = nOfDisjointAds - 1;
                     }
                     // apply it
                     disjointAdTags.get(screenshotI).add(overlappingCandidateIndex);
                  }

               }

            }
            Log.i(TAG, "DC screenshotIndependentDerivedCutDSCombined: " + screenshotIndependentDerivedCutDSCombined);
            Log.i(TAG, "DC disjointAdTags: " + disjointAdTags);

            Log.i(TAG, "DC nOfDisjointAds " + String.valueOf(nOfDisjointAds));

            // Then for each disjoint ad...
            for (int i = 0; i < nOfDisjointAds; i++) {
               retainedDividersNew.put(i, new ArrayList<>());
               for (int screenshotI = 0; screenshotI < analysisDS.size(); screenshotI++) {
                  retainedDividersNew.get(i).add(new ArrayList<>());
                  List<Integer> thisScreenshot = analysisDS.get(screenshotI);

                  screenshotIndependentDerivedCutDSCombined.get(screenshotI);

                  int runningDividerOffset = 0;
                  // for each divider in said screenshot, get its start and end boundary
                  for (int dividerJ = 0; dividerJ < thisScreenshot.size(); dividerJ++) {
                     Bitmap thisDivider = retrieveRetainedDivider(segmentStitched, screenshotI, dividerJ);

                     int thisStart = runningDividerOffset;
                     int thisEnd = thisStart + thisDivider.getHeight();

                     boolean within = false;

                     for (int w = 0; w < screenshotIndependentDerivedCutDSCombined.get(screenshotI).size(); w++) {
                        // if we are dealing with the relevant ad
                        if (disjointAdTags.get(screenshotI).get(w) == i) {
                           List<Integer> thisCut = screenshotIndependentDerivedCutDSCombined.get(screenshotI).get(w);

                           if ((rangesOverlap(thisStart, thisEnd, thisCut.get(0), thisCut.get(1))/(double) Math.abs(thisStart - thisEnd)) > 0.5) {
                              within = true;
                              if ((thisCut.get(0) == 667) && (thisCut.get(1) == 1453)) {
                                 Log.i(TAG, "DC screenshotI " + screenshotI);
                                 Log.i(TAG, "DC i " + i);
                                 Log.i(TAG, "DC dividerJ " + dividerJ);
                              }
                           }
                        }
                     }
                     runningDividerOffset += thisDivider.getHeight();

                     // log divider
                     if (within) {
                        List<List<Integer>> ds = retainedDividersNew.get(i);
                        List<Integer> thisScreenshotRetainedNew = ds.get(screenshotI);
                        thisScreenshotRetainedNew.add(thisScreenshot.get(dividerJ));
                     }
                  }
               }
            }

            Log.i(TAG, "DC retained: " + String.valueOf(retainedDividersNew));
            String segmentID = UUID.randomUUID().toString();

            if (DEBUG) {
               File dir = filePath(asList(MainActivity.getMainDir(thisContext).getAbsolutePath(), "debug", "dividers_retained"));
               if (!dir.exists()) {
                  dir.mkdirs();
               }
               for (Integer key : retainedDividersNew.keySet()) {
                  for (int screenshotI = 0; screenshotI < retainedDividersNew.get(key).size(); screenshotI++) {
                     List<Bitmap> bitmapsToCombine = new ArrayList<>();
                     for (int dividerI = 0; dividerI < retainedDividersNew.get(key).get(screenshotI).size(); dividerI++) {
                        bitmapsToCombine.add(retrieveRetainedDivider(segmentStitched, screenshotI, analysisDS.get(screenshotI).indexOf(retainedDividersNew.get(key).get(screenshotI).get(dividerI))));
                     }
                     if (bitmapsToCombine.size() > 0) {
                        try {
                           try (FileOutputStream out = new FileOutputStream(
                                   filePath(asList(dir.getAbsolutePath(), segmentID + "-" + key + "-" + screenshotI + ".png")).getAbsolutePath())) {
                              combineImagesList(Args(A("listOfBitmaps", bitmapsToCombine), A("orientation", "v"))).compress(Bitmap.CompressFormat.PNG, 100, out);
                           } catch (IOException e) {
                           }
                        } catch (Exception e) {

                        }
                     }
                  }


               }
            }

            // add ad
            JSONObject anticipatedAd = new JSONObject();

            List<Integer> adHeaderIndices = Stream.concat(Stream.of(adHeaderDividerIndex), toBeReplaced.keySet().stream().filter(x-> Objects.equals(toBeReplaced.get(x), adHeaderDividerIndex))).collect(Collectors.toList());

            anticipatedAd.put("ad_header_indices", adHeaderIndices);
            anticipatedAd.put("screenshot_divider_orderings", analysisDS);

            HashMap<Integer, Bitmap> adRetainedDividers = new HashMap<>();
            for (Integer key : retainedDividersNew.keySet()) {
               for (int screenshotI = 0; screenshotI < retainedDividersNew.get(key).size(); screenshotI++) {
                  for (int dividerI = 0; dividerI < retainedDividersNew.get(key).get(screenshotI).size(); dividerI++) {
                     adRetainedDividers.put(retainedDividersNew.get(key).get(screenshotI).get(dividerI),
                             retrieveRetainedDivider(segmentStitched, screenshotI, analysisDS.get(screenshotI).indexOf(retainedDividersNew.get(key).get(screenshotI).get(dividerI))));
                  }
               }
            }

            //apply all ad header indices (they wont be picked up by the retainedDividersNew dictionary)
            for (Integer k : adHeaderIndices) {
               for (int screenshotI = 0; screenshotI < analysisDS.size(); screenshotI++) {
                  if (preservedAnalysisDS.get(screenshotI).contains(k)) {
                     adRetainedDividers.put(k, retrieveRetainedDivider(segmentStitched, screenshotI, preservedAnalysisDS.get(screenshotI).indexOf(k)));
                     break;
                  }
               }
            }

            anticipatedAd.put("divider_images", adRetainedDividers);
            anticipatedAd.put("screenshot_offsets", segmentStitched.get("offsets"));

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

            anticipatedAd.put("screenshot_relative_data",  screenshotRelativeData);
            anticipatedAds.add(anticipatedAd);

         }
      }




      /*
      for (int i = 0; i < nOfDisjointAds; i ++) {
         // For each of the found ads
         // dividers reference
         // retainedDividersNew
         // ad header data
         // offsets

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
      }*/


      /*
      *
      *

         // get all dividers that are majority overlapping the derived cuts






      *
      *
      *
      * */


      // with list of offsets captures
      // combine them into common offset captures within segments
      // then find all divs that fit in region
      // those form the desired ad

      /*

      // identify all dividers before ad headers
      List<Bitmap> bitmapsToPut = new ArrayList<>();
      List<Bitmap> bitmapsSources = new ArrayList<>();

      for (Integer adHeaderDividerIndex : adHeaderDividerIndices) {
         for (int screenshotI = 0; screenshotI < analysisDS.size(); screenshotI ++) {
            List<Integer> thisScreenshot = analysisDS.get(screenshotI);
            Integer adHeaderDividerI = thisScreenshot.indexOf(adHeaderDividerIndex);
            // If the ad header index position is not the first one in the list (if it is, then there is no preceding post divider to compare against)
            if ((adHeaderDividerI != 0) && (adHeaderDividerI != -1)) {
               // attempt to isolate a post divider
               Bitmap postDividerIsolated = null;
               Integer postDividerIsolatedIndex = null;
               Integer postDividerIsolatedIndexAs = null;

               Integer possiblePostDividerI = adHeaderDividerI-1;
               Integer possiblePostDividerPreI = adHeaderDividerI-2;

               Bitmap adHeaderDivider = retrieveRetainedDivider(segmentStitched, screenshotI, adHeaderDividerI);
               Bitmap postDividerDivider = retrieveRetainedDivider(segmentStitched, screenshotI, possiblePostDividerI);

               int adHeaderWhitespaceColor = getWhitespacePixel(Args(A("bitmap", adHeaderDivider)));
               int possibleDividerWhitespaceColor = dominantColourInImage(postDividerDivider);

               List<Bitmap> result = verySimpleDividers(postDividerDivider);

               Double thresholdColourPaletteToUseForWhitespace = 0.9;

               List<Bitmap> possibleDividerWhitespaceDividers = result.stream().filter(x ->
                       isWhitespace(Args(
                               A("bitmap", x),
                               A("method", "localDifferences"),
                               A("thresholdColourPalette", thresholdColourPaletteToUseForWhitespace))) // perhaps check this
               ).collect(Collectors.toList());

               if (!possibleDividerWhitespaceDividers.isEmpty()) {
                  // If it consists of one divider...
                  if (result.size() == 1) {
                     if (DEBUG) {
                        File adHeadersDirName = filePath(asList(MainActivity.getMainDir(thisContext).getAbsolutePath(), "debug", "result_divider_images"));
                        if (!adHeadersDirName.exists()) { adHeadersDirName.mkdirs(); }
                        for (Bitmap b : result) {
                           try (FileOutputStream out = new FileOutputStream(
                                   filePath(asList(adHeadersDirName.getAbsolutePath(), "ad-header-" + UUID.randomUUID().toString() + ".png")).getAbsolutePath())) {
                              b.compress(Bitmap.CompressFormat.PNG, 100, out);
                           } catch (IOException e) { }
                        }
                     }

                     // There needs to be at least a slight difference between both dividers
                     if (pixelDifferencePercentage(possibleDividerWhitespaceColor, adHeaderWhitespaceColor) > 0.02) {
                        // We have to determine that the divider above the post divider is at least different to the post divider as well
                        if (possiblePostDividerPreI >= 0) {
                           Bitmap postDividerPreDivider = retrieveRetainedDivider(segmentStitched, screenshotI, possiblePostDividerPreI);

                           List<Bitmap> result2 = verySimpleDividers(postDividerPreDivider);

                           List<Bitmap> possibleDividerPreWhitespaceDividers = result2.stream().filter(x ->
                                   isWhitespace(Args(
                                           A("bitmap", x),
                                           A("method", "localDifferences"),
                                           A("thresholdColourPalette", thresholdColourPaletteToUseForWhitespace))) // perhaps check this
                           ).collect(Collectors.toList());

                           if (possibleDividerPreWhitespaceDividers.size() >= 1) {
                              int possibleDividerPreWhitespaceColor = dominantColourInImage(possibleDividerPreWhitespaceDividers.get(possibleDividerPreWhitespaceDividers.size()-1));


                              // NOTE: We don't need to evaluate more than 1 divider above, as past conditions evaluate this one

                              // If this condition is fulfilled, then we can successfully say that the post divider is isolated from
                              // not only the divider below, but also the one above it
                              if (pixelDifferencePercentage(possibleDividerWhitespaceColor, possibleDividerPreWhitespaceColor) > 0.02) {
                                 // isolated
                              } else {
                                 // there is no distinction, we can't make any judgements
                              }
                           }
                        } else {
                           // If its a negative index, then it wasn't captured, and we can't make any further assumptions about the height of
                           // the post divider
                        }
                     } else {
                        // Don't do anything, they're the same colour (technically, this shouldn't happen)
                     }
                  } else {

                     boolean postDividerFound = false;
                     boolean dividerAfterPostDividerFound = false;
                     int thisPostDividerIndexInResult = 0;
                     for (Bitmap b : result) {
                        int thisWSColor = dominantColourInImage(b);
                        Log.i(TAG, "thisWSColor: " + colourToHex(thisWSColor));
                        if ((pixelDifferencePercentage(adHeaderWhitespaceColor, thisWSColor) < 0.05)
                                && (postDividerFound)) {
                           dividerAfterPostDividerFound = true;
                        } else if (pixelDifferencePercentage(adHeaderWhitespaceColor, thisWSColor) > 0.05) {
                           postDividerFound = true;
                           thisPostDividerIndexInResult = result.indexOf(b);
                        }
                     }

                     if (dividerAfterPostDividerFound && postDividerFound) {

                        //postDividerIsolated = lastDivider;
                        //postDividerIsolatedIndex = possiblePostDividerI;
                        //postDividerIsolatedIndexAs = thisScreenshot.get(postDividerIsolatedIndex);
                        bitmapsToPut.add(result.get(thisPostDividerIndexInResult));
                        bitmapsSources.add(postDividerDivider);
                     }
                  }
               }

               if (postDividerIsolated != null) {
                  // found a match - apply it
                  Log.i(TAG, "Found postDivider : postDividerIsolatedIndex: "+postDividerIsolatedIndex);
                  Log.i(TAG, "Found postDivider : postDividerIsolatedIndexAs: "+postDividerIsolatedIndexAs);
               }

            }

         }
      }

      if (DEBUG) {
         File adHeadersDirName = filePath(asList(MainActivity.getMainDir(thisContext).getAbsolutePath(), "debug", "post_dividers"));
         if (!adHeadersDirName.exists()) { adHeadersDirName.mkdirs(); }
         for (Bitmap b : bitmapsToPut) {
            try (FileOutputStream out = new FileOutputStream(
                    filePath(asList(adHeadersDirName.getAbsolutePath(), UUID.randomUUID().toString() + ".png")).getAbsolutePath())) {
               b.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (IOException e) { }
         }
      }
      if (DEBUG) {
         File adHeadersDirName = filePath(asList(MainActivity.getMainDir(thisContext).getAbsolutePath(), "debug", "post_dividers_sources"));
         if (!adHeadersDirName.exists()) { adHeadersDirName.mkdirs(); }
         for (Bitmap b : bitmapsSources) {
            try (FileOutputStream out = new FileOutputStream(
                    filePath(asList(adHeadersDirName.getAbsolutePath(), UUID.randomUUID().toString() + ".png")).getAbsolutePath())) {
               b.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (IOException e) { }
         }
      }*/


      ///

      /*



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
      */

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
                  A("whitespaceThreshold", 0.2),
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


            Integer adjustedHeight = Math.round(thisScreenshotBitmap.getHeight()/thisScreenshotBitmap.getWidth()*220);

            Bitmap thisScreenshotBitmapReduced = Bitmap.createScaledBitmap(thisScreenshotBitmap, 220, adjustedHeight, false);
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


            if (result.statistics != null) {
               Log.i(TAG, "segment: "+i+" screenshot: "+j+" navbar_index: "+ ((JSONObject) result.statistics).get("navbarDividerIndex"));
               Log.i(TAG, "segment: "+i+" screenshot: "+j+" navbar_index retainedIndexMidpoint: "+ ((JSONObject) result.statistics).get("retainedIndexMidpoint"));
               Log.i(TAG, "segment: "+i+" screenshot: "+j+" navbar_index adjusted height: "+ adjustedHeight);
            }







            if (DEBUG) {
               File dirDividers = filePath(asList(MainActivity.getMainDir(thisContext).getAbsolutePath(), "debug", "dividers_before_raw"));
               if (!dirDividers.exists()) { dirDividers.mkdirs(); }
               for (int k = 0; k < thisImageDividers.size(); k++) {
                  try (FileOutputStream out = new FileOutputStream(
                          filePath(asList(dirDividers.getAbsolutePath(), "divider-" + i + "-" + j + "-" + k + ".png")).getAbsolutePath())) {
                     thisImageDividers.get(k).compress(Bitmap.CompressFormat.PNG, 100, out);
                  } catch (IOException ignored) { }
               }
            }















                        segmentsTimestamped.get(i).get(j).put("inFacebook", result.inFacebook);
            Log.i(TAG,  "\t\tFacebookScreenshot "+j+" is within Facebook:"+result.inFacebook);
            segmentsTimestamped.get(i).get(j).put("image", thisScreenshotBitmap); // TODO put back into block below
            boolean inFacebook = (boolean) result.inFacebook;
            if (inFacebook) {
               Double navbarPercentageRatio = (((Integer) result.statistics.get("retainedIndexMidpoint"))/(double) adjustedHeight);

               int adjustedNavbarIndex = 0;
               int runningHeight = 0;
               int navbarMidpointTrueHeight = (int) Math.floor(navbarPercentageRatio*thisScreenshotBitmap.getHeight());
               for (int r = 0; r < thisImageDividers.size(); r ++) {
                  runningHeight += thisImageDividers.get(r).getHeight();
                  if (runningHeight > navbarMidpointTrueHeight) {
                     adjustedNavbarIndex = r;
                     break;
                  }
               }

               Log.i(TAG, "segment: "+i+" screenshot: "+j+" navbar_index: retainedIndexMidpoint: "+ ((JSONObject) result.statistics).get("retainedIndexMidpoint"));
               Log.i(TAG, "segment: "+i+" screenshot: "+j+" navbar_index: thisScreenshotBitmap.getHeight(): "+ thisScreenshotBitmap.getHeight());
               Log.i(TAG, "segment: "+i+" screenshot: "+j+" navbar_index: navbarPercentageRatio: "+ navbarPercentageRatio);
               Log.i(TAG, "segment: "+i+" screenshot: "+j+" navbar_index: navbarMidpointTrueHeight: "+ navbarMidpointTrueHeight);
               Log.i(TAG, "segment: "+i+" screenshot: "+j+" navbar_index: TRUE: "+ adjustedNavbarIndex);


               segmentsTimestamped.get(i).get(j).put("dividerBounds", thisImageDividerBounds);
               HashMap<String, Integer> dimensions = new HashMap<>();
               dimensions.put("width", thisScreenshotBitmap.getWidth());
               dimensions.put("height", thisScreenshotBitmap.getHeight());
               segmentsTimestamped.get(i).get(j).put("dimensions", dimensions);
               segmentsTimestamped.get(i).get(j).put("dividers", thisImageDividers);
               segmentsTimestamped.get(i).get(j).put("whitespacePixel", thisWhitespacePixel);
               segmentsTimestamped.get(i).get(j).put("navbarPercentageRatio", navbarPercentageRatio);
               Iterator<String> keys = ((JSONObject) result.statistics).keys();
               while(keys.hasNext()) {
                  String key = keys.next();
                  segmentsTimestamped.get(i).get(j).put(key, ((JSONObject) result.statistics).get(key));
               }


               segmentsTimestamped.get(i).get(j).put("navbarDividerIndex", adjustedNavbarIndex); // TODO this overwrites a now depreciated value
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

            int screenshotOffset = 0;
            for (int j = 0; j < segmentsStitched.size(); j ++) {
               // TODO - make safe
               Integer heightOfSegment = ((HashMap<String, Integer>) ((JSONObject) ((List<JSONObject>) segmentsStitched.get(j).get("screenshots")).get(0)).get("dimensions")).get("height");

               JSONObject outputFromSegmentation = segmentWithoutOCR(segmentsStitched.get(j));
               List<List<Integer>> analysisDS = (List<List<Integer>>) outputFromSegmentation.get("analysisDSReduced");
               Integer iiTransported = (Integer) outputFromSegmentation.get("ii");


               List<JSONObject> adsForSubmission = extractAds(segmentsStitched.get(j), analysisDS, screenshotOffset, pictogramsReference, thisContext, heightOfSegment);

               screenshotOffset += ((List<JSONObject>) segmentsStitched.get(j).get("screenshots")).size();


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

   // note1
}
