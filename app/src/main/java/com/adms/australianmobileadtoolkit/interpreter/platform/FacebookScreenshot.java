package com.adms.australianmobileadtoolkit.interpreter.platform;

import static com.adms.australianmobileadtoolkit.Arguments.A;
import static com.adms.australianmobileadtoolkit.Arguments.Args;
import static com.adms.australianmobileadtoolkit.Common.filePath;
import static com.adms.australianmobileadtoolkit.Logger.log;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.dividerWhitespaceAlternations;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.dividerWhitespaceAlternationsWellFormed;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.getWhitespacePixel;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.imageToStencil;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.pixelDifferencePercentage;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.stencilSimilarity;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.stencilToString;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.visualComponentsEquallySpaced;

import static java.util.Arrays.asList;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.util.Pair;

import com.adms.australianmobileadtoolkit.Arguments;
import com.adms.australianmobileadtoolkit.Settings;
import com.adms.australianmobileadtoolkit.interpreter.visual.DividerSet;
import com.adms.australianmobileadtoolkit.interpreter.visual.Stencil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FacebookScreenshot {

   public boolean inFacebook;
   public JSONObject statistics;

   final private static String TAG = "FacebookScreenshot";
   public static double DEFAULT_FACEBOOK_SCREENSHOT_TOP_THIRD_RATIO = 0.3;
   public static double DEFAULT_FACEBOOK_SCREENSHOT_BLUE_THRESHOLD = 0.3;
   public static double DEFAULT_FACEBOOK_SCREENSHOT_STENCIL_DIFFERENCE_THRESHOLD = 0.3;
   public static List<Integer>
                        DEFAULT_FACEBOOK_SCREENSHOT_ANTICIPATED_NAVBAR_BUTTONS_N = asList(5, 6);

   // The following defaults are relative to the calculation of horizontal dividers on each of the
   // respective vertical dividers
   public static double DEFAULT_FACEBOOK_SCREENSHOT_HORIZONTAL_DIVIDERS_SCALE_MINOR = 1.0;
   public static boolean DEFAULT_FACEBOOK_SCREENSHOT_HORIZONTAL_DIVIDERS_ABSORB_MINIMUMS = true;
   public static double DEFAULT_FACEBOOK_SCREENSHOT_HORIZONTAL_DIVIDERS_WHITESPACE_THRESHOLD = 0.2;
   public static double DEFAULT_FACEBOOK_SCREENSHOT_HORIZONTAL_DIVIDERS_COLOUR_PALETTE_THRESHOLD = 0.3;
   public static double DEFAULT_FACEBOOK_SCREENSHOT_HORIZONTAL_DIVIDERS_SCAN_UNTIL = 1.0;
   public static boolean DEFAULT_FACEBOOK_SCREENSHOT_HORIZONTAL_DIVIDERS_RETAIN_MINIMUMS = false;
   public static String DEFAULT_FACEBOOK_SCREENSHOT_HORIZONTAL_DIVIDERS_MIN_DIVIDERS_APPROACH = "complex";

   // The following defaults are relative to the determination of whitespace alternations in horizontal
   // dividers
   public static boolean DEFAULT_FACEBOOK_SCREENSHOT_WHITESPACE_ALTERNATIONS_PRESERVE_DIMENSIONS = true;
   public static String DEFAULT_FACEBOOK_SCREENSHOT_WHITESPACE_ALTERNATIONS_METHOD = "prescribed";

   // The following defaults are relative to the identification of potential Facebook navbar buttons
   public static int DEFAULT_FACEBOOK_SCREENSHOT_POTENTIAL_NAVBAR_BUTTON_STENCIL_SIZE_UNIT = 64;
   public static HashMap<String, Integer>
         DEFAULT_FACEBOOK_SCREENSHOT_POTENTIAL_NAVBAR_BUTTON_STENCIL_SIZE =
               new HashMap<String, Integer>() {{
                  put("s", DEFAULT_FACEBOOK_SCREENSHOT_POTENTIAL_NAVBAR_BUTTON_STENCIL_SIZE_UNIT);
               }};
   public static int DEFAULT_FACEBOOK_SCREENSHOT_REFERENCE_COLOUR_DARK = Color.rgb(0,0,0);
   public static int DEFAULT_FACEBOOK_SCREENSHOT_REFERENCE_COLOUR_LIGHT = Color.rgb(255,255,255);
   public static List<String> DEFAULT_FACEBOOK_SCREENSHOT_BUTTON_STATES = asList("Active", "Inactive");
   public static List<String> DEFAULT_FACEBOOK_SCREENSHOT_EXPOSURE_STATES = asList("Dark", "Light");
   public static List<String> DEFAULT_FACEBOOK_SCREENSHOT_NAVBAR_BUTTON_IDENTITIES = asList("Home", "Watch");
   public static String DEFAULT_FACEBOOK_SCREENSHOT_POTENTIAL_NAVBAR_BUTTON_STENCIL_SIMILARITY_METHOD = "summated";
   public static String DEFAULT_FACEBOOK_SCREENSHOT_POTENTIAL_NAVBAR_BUTTON_STENCIL_TARGET_STATE = "Active";
   public static int DEFAULT_FACEBOOK_SCREENSHOT_REFERENCE_COLOUR_BLUE_LIGHT = Color.rgb(96,96,224);
   public static int DEFAULT_FACEBOOK_SCREENSHOT_REFERENCE_COLOUR_BLUE_DARK = Color.rgb(44,100,245);
   Integer retainedIndexMidpoint = 0;

   public FacebookScreenshot(Arguments args) throws JSONException {
      long elapsedTime = System.currentTimeMillis();

      List<Bitmap> dividers = args.getListBitmap("dividers", new ArrayList<>() );
      int whitespacePixel = (int) args.get("whitespacePixel",
                                             getWhitespacePixel(Args(A("bitmap", dividers.get(0)))));
      double thresholdBlue = (double) args.get("toleranceBlue",
                                             DEFAULT_FACEBOOK_SCREENSHOT_BLUE_THRESHOLD);
      double thresholdStencilDifference = (double) args.get("tolerancePictogramDiff",
                                             DEFAULT_FACEBOOK_SCREENSHOT_STENCIL_DIFFERENCE_THRESHOLD);
      HashMap<String, Object> referenceStencilsPictograms =
                        args.getHashMapStringObject("referenceStencilsPictograms", new HashMap<>());


      int h = (int) args.get("h", 0 );

      double topThirdRatio = DEFAULT_FACEBOOK_SCREENSHOT_TOP_THIRD_RATIO;

      Log.i(TAG, "There are "+dividers.size()+" vertical components in this image");

      int cumulativeH = 0;
      List<Bitmap> dVerticalsTT = new ArrayList<>();
      List<Integer> dVerticalsTTIndices = new ArrayList<>();
      for (int i = 0; i < dividers.size(); i ++) {
         int thisHeight = dividers.get(i).getHeight();
         cumulativeH += thisHeight;
         if (cumulativeH > (h*topThirdRatio)) {
            break;
         }
         dVerticalsTT.add(dividers.get(i));
         dVerticalsTTIndices.add(i);
      }

      Log.i(TAG, "navbar_index dVerticalsTTIndices:" + dVerticalsTTIndices);

      Log.i(TAG, "There are "+dVerticalsTT.size()+" components in the top-third of the image");

      List<List<Bitmap>> dHorizontalsTT = new ArrayList<>();
      for (int i = 0; i < dVerticalsTT.size(); i ++) {
         dHorizontalsTT.add(
               new DividerSet(Args(
                     A("bitmap", dVerticalsTT.get(i)),
                     A("orientation", "h"),
                     A("scaleMinor",
                           DEFAULT_FACEBOOK_SCREENSHOT_HORIZONTAL_DIVIDERS_SCALE_MINOR),
                     A("absorbMinimums",
                           DEFAULT_FACEBOOK_SCREENSHOT_HORIZONTAL_DIVIDERS_ABSORB_MINIMUMS),
                     A("whitespaceThreshold",
                           DEFAULT_FACEBOOK_SCREENSHOT_HORIZONTAL_DIVIDERS_WHITESPACE_THRESHOLD),
                     A("colourPaletteThreshold",
                           DEFAULT_FACEBOOK_SCREENSHOT_HORIZONTAL_DIVIDERS_COLOUR_PALETTE_THRESHOLD),
                     A("scanUntil",
                           DEFAULT_FACEBOOK_SCREENSHOT_HORIZONTAL_DIVIDERS_SCAN_UNTIL),
                     A("retainMinimums",
                           DEFAULT_FACEBOOK_SCREENSHOT_HORIZONTAL_DIVIDERS_RETAIN_MINIMUMS),
                     A("minDividerApproach",
                           DEFAULT_FACEBOOK_SCREENSHOT_HORIZONTAL_DIVIDERS_MIN_DIVIDERS_APPROACH))).dividerImages

               );
      }

      List<Integer> anticipatedNavbarButtons = new ArrayList<>();
      anticipatedNavbarButtons.add(5);
      anticipatedNavbarButtons.add(6);

      List<Integer> anticipatedNavbarButtonOffsets = new ArrayList<>();
      for (int i = 0; i < anticipatedNavbarButtons.size(); i ++) {
         anticipatedNavbarButtonOffsets.add((anticipatedNavbarButtons.get(i)*2)+1);
      }

      List<Integer> retainedIndices = new ArrayList<>();
      List<Integer> retainedIndicesIndices = new ArrayList<>();
      List<List<Bitmap>> dHorizontalsTTCorrectN = new ArrayList<>();
      for (int i = 0; i < dHorizontalsTT.size(); i ++) {
         if (anticipatedNavbarButtonOffsets.contains(dHorizontalsTT.get(i).size())) {
            retainedIndices.add(i);
            retainedIndicesIndices.add(dVerticalsTTIndices.get(i));
            dHorizontalsTTCorrectN.add(dHorizontalsTT.get(i));
         }
      }

      Log.i(TAG, "There are "+dHorizontalsTTCorrectN.size()+" components therein that have the correct number of elements");


      List<List<Boolean>> wsAlternationData = new ArrayList<>();
      Log.i(TAG, "whitespacePixel: "+ String.valueOf(whitespacePixel));
      for (int i = 0; i < dHorizontalsTTCorrectN.size(); i ++) {
         wsAlternationData.add(dividerWhitespaceAlternations(Args(
               A("visualComponents", dHorizontalsTTCorrectN.get(i)),
               A("whitespacePixel", whitespacePixel),
               A("preserveDimensions",
                     DEFAULT_FACEBOOK_SCREENSHOT_WHITESPACE_ALTERNATIONS_PRESERVE_DIMENSIONS),
               A("method",
                     DEFAULT_FACEBOOK_SCREENSHOT_WHITESPACE_ALTERNATIONS_METHOD)))); // TODO was 0.15
      }









      Log.i(TAG, "\t Alternation data:");
      Log.i(TAG, "\t\t"+wsAlternationData);

      List<Integer> retainedIndices2 = new ArrayList<>();
      List<Integer> retainedIndices2Indices = new ArrayList<>();
      List<JSONObject> dHorizontalsTTCorrectNwsAlts = new ArrayList<>();
      for (int i = 0; i < retainedIndices.size(); i ++) {
         if (dividerWhitespaceAlternationsWellFormed(wsAlternationData.get(i))) {
            retainedIndices2.add(retainedIndices.get(i));
            retainedIndices2Indices.add(retainedIndicesIndices.get(i));
            JSONObject thisPair = new JSONObject();
            thisPair.put("dHorizontalsTTCorrectN", dHorizontalsTTCorrectN.get(i));
            thisPair.put("wsAlternationData", wsAlternationData.get(i));
            dHorizontalsTTCorrectNwsAlts.add(thisPair);
         }
      }

      Log.i(TAG, "There are "+dHorizontalsTTCorrectNwsAlts.size()+" components therein that alternate from/to whitespace correctly");

      List<Integer> retainedIndices3 = new ArrayList<>();
      List<Integer> retainedIndices3Indices = new ArrayList<>();
      List<JSONObject> dHorizontalsTTCorrectNwsAltsEncased = new ArrayList<>();
      for (int i = 0; i < retainedIndices2.size(); i ++) {
         List<Boolean> thisAlternationData = ((List<Boolean>) dHorizontalsTTCorrectNwsAlts.get(i).get("wsAlternationData"));
         int thisSize = thisAlternationData.size();
         if ((thisAlternationData.get(0))
               && (thisAlternationData.get(thisSize-1))) {
            retainedIndices3.add(retainedIndices2.get(i));
            retainedIndices3Indices.add(retainedIndices2Indices.get(i));
            dHorizontalsTTCorrectNwsAltsEncased.add(dHorizontalsTTCorrectNwsAlts.get(i));
         }
      }

      Log.i(TAG, "There are "+dHorizontalsTTCorrectNwsAltsEncased.size()+" components therein that are encased in whitespace");

      List<Boolean> equallySpacedBooleans = new ArrayList<>();
      for (int i = 0; i < dHorizontalsTTCorrectNwsAltsEncased.size(); i ++) {
         equallySpacedBooleans.add(
               visualComponentsEquallySpaced(Args(
                           A("visualComponents", (List<Bitmap>) dHorizontalsTTCorrectNwsAltsEncased.get(i).get("dHorizontalsTTCorrectN")),
                           A("alternations", (List<Boolean>) dHorizontalsTTCorrectNwsAltsEncased.get(i).get("wsAlternationData")))));
      }

      List<Integer> retainedIndices4 = new ArrayList<>();
      List<Integer> retainedIndices4Indices = new ArrayList<>();
      List<JSONObject> dHorizontalsTTCorrectNwsAltsEncasedEqSpaced = new ArrayList<>();
      for (int i = 0; i < retainedIndices3.size(); i ++) {
         if (equallySpacedBooleans.get(i)) {
            retainedIndices4.add(retainedIndices3.get(i));
            retainedIndices4Indices.add(retainedIndices3Indices.get(i));
            dHorizontalsTTCorrectNwsAltsEncasedEqSpaced.add(dHorizontalsTTCorrectNwsAltsEncased.get(i));
            Log.i(TAG, "\t Prefacing equal spacing: "+retainedIndices3.get(i));
         }
      }

      Log.i(TAG, "There are "+dHorizontalsTTCorrectNwsAltsEncasedEqSpaced.size()+" components therein that are equally spaced: ");

      inFacebook = false;
      statistics = null;

      if (dHorizontalsTTCorrectNwsAltsEncasedEqSpaced.size() != 1) {
         // do nothing
      } else {
         int retainedIndex = retainedIndices4Indices.get(0);
         for (int i = 0; i <= retainedIndex; i ++) {
            retainedIndexMidpoint += (dividers.get(i).getHeight() / ((i == retainedIndex) ? 2 : 1));
         }

         List<Bitmap> thisDividers = (List<Bitmap>) dHorizontalsTTCorrectNwsAltsEncasedEqSpaced.get(0).get("dHorizontalsTTCorrectN");
         List<Boolean> thisAlternations = (List<Boolean>) dHorizontalsTTCorrectNwsAltsEncasedEqSpaced.get(0).get("wsAlternationData");






         List<Stencil> potentialNavbarButtonPictograms = new ArrayList<>();
         HashMap<String, Integer> size = new HashMap<>();
         size.put("s", 64);
         for (int i = 0; i < thisDividers.size(); i ++) {

            if (!thisAlternations.get(i)) {
               potentialNavbarButtonPictograms.add(
                     imageToStencil(Args(
                           A("bitmap", thisDividers.get(i)),
                           A("whitespacePixel", whitespacePixel),
                           A("size", DEFAULT_FACEBOOK_SCREENSHOT_POTENTIAL_NAVBAR_BUTTON_STENCIL_SIZE),
                           A("colourPaletteThreshold", 0.2),
                           A("isReference", false))));
            }
         }
         potentialNavbarButtonPictograms.remove(potentialNavbarButtonPictograms.size()-1);
         int nTabs = potentialNavbarButtonPictograms.size();

         HashMap<Integer, String> navbarHits = new HashMap<>();
         boolean dark = (pixelDifferencePercentage(whitespacePixel, Color.rgb(0,0,0))
               < pixelDifferencePercentage(whitespacePixel, Color.rgb(255,255,255)));
         List<String> buttonStates = asList("Active", "Inactive");
         List<String> exposureStates = asList("Dark", "Light");

         String exposureState = ((dark) ? exposureStates.get(0) : exposureStates.get(1));
         String activeState = "Active";
         List<String> nominatedExposureStates = asList("facebook"+exposureState+"Home", "facebook"+exposureState+"Watch");
         for (int i = 0; i < nominatedExposureStates.size(); i ++) {
            String thisPictogramName = nominatedExposureStates.get(i);
            int cursor = 0;
            while (cursor < Math.min(potentialNavbarButtonPictograms.size(), 2)) { // TODO = remove the lock on 2
               for (int j = 0; j < buttonStates.size(); j ++) {
                  String thisPictogramNameAugmented = thisPictogramName+buttonStates.get(j);
                  Stencil thisStencil = (Stencil) referenceStencilsPictograms.get(thisPictogramNameAugmented);
                  Stencil thatStencil = potentialNavbarButtonPictograms.get(cursor);

                  System.out.println("thisStencil:");
                  System.out.println(stencilToString(thisStencil.getStencil()));
                  System.out.println("thatStencil:");
                  System.out.println(stencilToString(thatStencil.getStencil()));


                  double matchResult = stencilSimilarity(Args(
                        A("a", thisStencil),
                        A("b", thatStencil),
                        A("method",
                              DEFAULT_FACEBOOK_SCREENSHOT_POTENTIAL_NAVBAR_BUTTON_STENCIL_SIMILARITY_METHOD)
                  ));
                  Log.i(TAG,"Testing design of '"+thisPictogramNameAugmented+"' at navbar button '"+cursor+"' : " + matchResult);

                  if (matchResult > (1 - thresholdStencilDifference)) {
                     navbarHits.put(cursor, thisPictogramNameAugmented);
                     break;
                  }
               }
               cursor ++;
            }
         }

         List<Integer> navbarActiveKI = new ArrayList<>();
         for (Integer key : navbarHits.keySet()) {
            if (navbarHits.get(key).endsWith(activeState)) {
               navbarActiveKI.add(key);
            }
         }

         if (navbarActiveKI.size() != 1) {
            Log.i(TAG,"navbarActiveKI test : failed");
         } else {
            Log.i(TAG,"navbarActiveKI test : passed");

            Integer navbarActiveKIInteger = navbarActiveKI.get(0);
            List<Integer> paletteOfActiveNavbarButton = potentialNavbarButtonPictograms.get(navbarActiveKIInteger).getColourPaletteNonWhitespace();

            int signatureFacebookBlue = ((dark) ? Color.rgb(44,100,245) : Color.rgb(96,96,224));

            if (paletteOfActiveNavbarButton.stream().noneMatch(x -> pixelDifferencePercentage(x, signatureFacebookBlue) <= thresholdBlue)) {
               Log.i(TAG,"signatureFacebookBlue test : failed");

            } else {

               Log.i(TAG, "signatureFacebookBlue test : passed");

               Log.i(TAG,"Elapsed time: " + Math.abs((System.currentTimeMillis()) - elapsedTime));

               inFacebook = true;
               statistics = new JSONObject();
               statistics.put("tabActive", navbarHits.get(navbarActiveKIInteger).replace("Active", "").replace("Inactive",""));
               for (Integer key : navbarHits.keySet()) { navbarHits.put(key, navbarHits.get(key).replace("Active", "").replace("Inactive","")); }
               statistics.put("tabsIdentified", navbarHits);
               statistics.put("tabsN", nTabs);
               statistics.put("retainedIndexMidpoint", retainedIndexMidpoint);
               statistics.put("navbarDividerIndex", retainedIndex);
            }
         }
      }
   }

   // TODO update constructor to this
   public void FacebookScreenshotV2(Arguments args) throws JSONException {
      long elapsedTime = System.currentTimeMillis();

      List<Bitmap> dividers = args.getListBitmap("dividers", new ArrayList<>() );
      int whitespacePixel = (int) args.get("whitespacePixel",
            getWhitespacePixel(Args(A("bitmap", dividers.get(0)))));
      double thresholdBlue = (double) args.get("toleranceBlue",
            DEFAULT_FACEBOOK_SCREENSHOT_BLUE_THRESHOLD);
      double thresholdStencilDifference = (double) args.get("tolerancePictogramDiff",
            DEFAULT_FACEBOOK_SCREENSHOT_STENCIL_DIFFERENCE_THRESHOLD);
      HashMap<String, Object> referenceStencilsPictograms =
            args.getHashMapStringObject("referenceStencilsPictograms", new HashMap<>());
      double topThirdRatio = DEFAULT_FACEBOOK_SCREENSHOT_TOP_THIRD_RATIO;

      log(Args(A("in", TAG),
            A("message", "dividers.size() : " + dividers.size())));

      // Isolate all dividers in the top-third of the image
      int totalHeight = ((int) dividers.stream().mapToDouble(x -> (double) x.getHeight()).sum());
      log(Args(A("in", TAG),
            A("message", "totalHeight : "+dividers.stream().map(x -> x.getHeight()).collect(Collectors.toList()))));
      int cumulativeH = 0;
      List<Bitmap> dividersInTopThird = new ArrayList<>();
      for (int i = 0; i < dividers.size(); i ++) {
         int thisHeight = dividers.get(i).getHeight();
         cumulativeH += thisHeight;
         if (cumulativeH > (topThirdRatio * totalHeight)) {
            break;
         }
         dividersInTopThird.add(dividers.get(i));
      }

      log(Args(A("in", TAG),
            A("message", "dividersInTopThird.size() : "+dividersInTopThird.size())));

      // Split the dividers horizontally
      List<List<Bitmap>> horizontalDividersInDividersInTopThird =
            dividersInTopThird.stream().map(x -> new
                  DividerSet(Args(
                  A("bitmap", x),
                  A("orientation", "h"),
                  A("scaleMinor",
                        DEFAULT_FACEBOOK_SCREENSHOT_HORIZONTAL_DIVIDERS_SCALE_MINOR),
                  A("absorbMinimums",
                        DEFAULT_FACEBOOK_SCREENSHOT_HORIZONTAL_DIVIDERS_ABSORB_MINIMUMS),
                  A("whitespaceThreshold",
                        DEFAULT_FACEBOOK_SCREENSHOT_HORIZONTAL_DIVIDERS_WHITESPACE_THRESHOLD),
                  A("colourPaletteThreshold",
                        DEFAULT_FACEBOOK_SCREENSHOT_HORIZONTAL_DIVIDERS_COLOUR_PALETTE_THRESHOLD),
                  A("scanUntil",
                        DEFAULT_FACEBOOK_SCREENSHOT_HORIZONTAL_DIVIDERS_SCAN_UNTIL),
                  A("retainMinimums",
                        DEFAULT_FACEBOOK_SCREENSHOT_HORIZONTAL_DIVIDERS_RETAIN_MINIMUMS),
                  A("minDividerApproach",
                        DEFAULT_FACEBOOK_SCREENSHOT_HORIZONTAL_DIVIDERS_MIN_DIVIDERS_APPROACH))
            ).dividerImages).collect(Collectors.toList());

      // Of those, determine which have 5 - 7 horizontal dividers (if any)
      // Note: 5 - 7 visual components means that there are 6 - 8 whitespace dividers,
      // and these need to be added
      List<Integer> anticipatedNavbarButtons
            = DEFAULT_FACEBOOK_SCREENSHOT_ANTICIPATED_NAVBAR_BUTTONS_N;
      List<Integer> anticipatedNavbarButtonOffsets
            = anticipatedNavbarButtons.stream().map(x -> (x*2)+1).collect(Collectors.toList());
      // Filter the vertical dividers for those whose horizontal dividers indicate
      // the desired number of navbar buttons
      List<List<Bitmap>> dividersCorrectNavbarButtonsN = horizontalDividersInDividersInTopThird.stream()
            .filter(x -> (anticipatedNavbarButtonOffsets.contains(x.size()))).collect(Collectors.toList());
      log(Args(A("in", TAG),
            A("message",
                  "horizontalDividersInDividersInTopThird : "+horizontalDividersInDividersInTopThird.stream().map(x->x.size()).collect(Collectors.toList()))));

      log(Args(A("in", TAG),
            A("message",
                  "dividersCorrectNavbarButtonsN.size() : "+dividersCorrectNavbarButtonsN.size())));

      // Determine which horizontal divider sets have alternating (whitespace/non-whitespace) dividers
      //	and yield the alternation data
      List<Pair<Boolean, List<Boolean>>> whitespaceAlternationsData =
            dividersCorrectNavbarButtonsN.stream().map(
                  x -> {
                     List<Boolean> alternations = dividerWhitespaceAlternations(Args(
                           A("visualComponents", x),
                           A("whitespacePixel", whitespacePixel),
                           A("preserveDimensions",
                                 DEFAULT_FACEBOOK_SCREENSHOT_WHITESPACE_ALTERNATIONS_PRESERVE_DIMENSIONS),
                           A("method",
                                 DEFAULT_FACEBOOK_SCREENSHOT_WHITESPACE_ALTERNATIONS_METHOD)));
                     return new Pair<>(dividerWhitespaceAlternationsWellFormed(alternations), alternations);
                  }
            ).collect(Collectors.toList());

      log(Args(A("in", TAG),
            A("message",
                  "whitespaceAlternationsData : "+whitespaceAlternationsData)));

      // Isolate those that have well-formed alternations



      List<Pair<List<Bitmap>, List<Boolean>>> dividersCorrectAlternations =
            IntStream.range(0, whitespaceAlternationsData.size()).filter(i -> whitespaceAlternationsData.get(i).first)
                  .mapToObj(i ->
                        new Pair<>(
                              dividersCorrectNavbarButtonsN.get(i),
                              whitespaceAlternationsData.get(i).second
                        )
                  ).collect(Collectors.toList());

      log(Args(A("in", TAG),
            A("message",
                  "dividersCorrectAlternations.size() : "+dividersCorrectAlternations.size())));

      // Of those, determine which are preceded and proceeded by whitespace
      List<Pair<List<Bitmap>, List<Boolean>>> dividersCorrectEncasings = dividersCorrectAlternations.stream().filter(x ->{
         List<Boolean> alternationData = x.second;
         return ((alternationData.get(0))
               && (alternationData.get(alternationData.size()-1)));}
      ).collect(Collectors.toList());

      log(Args(A("in", TAG),
            A("message",
                  "dividersCorrectEncasings.size() : "+dividersCorrectEncasings.size())));

      // Of those, determine how many have equally-spaced non-whitespace dividers
      // Note: This step is reversed-informed by the elements, and not the retained indices (like
      // in previous steps)
      List<Boolean> equallySpacedBooleans = dividersCorrectEncasings.stream()
            .map(x -> visualComponentsEquallySpaced(Args(
                  A("visualComponents", x.first),
                  A("alternations", x.second))
            )).collect(Collectors.toList());

      List<Pair<List<Bitmap>, List<Boolean>>> dividersEquallySpaced =
            IntStream.range(0, dividersCorrectEncasings.size())
                  .filter(equallySpacedBooleans::get)
                  .mapToObj(dividersCorrectEncasings::get).collect(Collectors.toList());

      log(Args(A("in", TAG),
            A("message",
                  "dividersEquallySpaced.size() : "+dividersEquallySpaced.size())));

      // Instantiate the constructor values
      inFacebook = false;
      statistics = null;

      // Assert that there is only one candidate that fulfils all the aforementioned conditions
      if (dividersEquallySpaced.size() != 1) {

         log(Args(A("in", TAG),
               A("message",
                     "dividersEquallySpaced.size() != 1 -> inFacebook = false")));

      } else {

         // Focus on the divider that has fulfilled all preceding conditions
         int retainedIndex = -1;//retainedIndices.get(0);
         List<Bitmap> thisDividers = dividersEquallySpaced.get(0).first;
         List<Boolean> thisAlternations = dividersEquallySpaced.get(0).second;

         // Isolate the potential navbar buttons as stencils
         List<Stencil> potentialNavbarButtonStencils = IntStream.range(0, thisDividers.size())
               .filter(i -> (!thisAlternations.get(i)))
               .mapToObj(i -> imageToStencil(Args(
                     A("bitmap", thisDividers.get(i)),
                     A("whitespacePixel", whitespacePixel),
                     A("size", DEFAULT_FACEBOOK_SCREENSHOT_POTENTIAL_NAVBAR_BUTTON_STENCIL_SIZE),
                     A("colourPaletteThreshold", 0.2),
                     A("isReference", false)))).collect(Collectors.toList());

         // Make the assumption that the last stencil is the profile picture of the
         // user and remove it as it might interfere with the logo detection
         potentialNavbarButtonStencils.remove(potentialNavbarButtonStencils.size()-1);

         // Note: A by-product of this process is that we can determine how many tabs are present
         int nTabs = potentialNavbarButtonStencils.size();

         // Determine if the screenshot is 'dark' themed or 'light' themed
         boolean screenshotIsInDarkMode = (pixelDifferencePercentage(
               whitespacePixel, DEFAULT_FACEBOOK_SCREENSHOT_REFERENCE_COLOUR_DARK)
               < pixelDifferencePercentage(
               whitespacePixel, DEFAULT_FACEBOOK_SCREENSHOT_REFERENCE_COLOUR_LIGHT));

         // Retrieve the exposure state
         String exposureState = DEFAULT_FACEBOOK_SCREENSHOT_EXPOSURE_STATES.get(
               (screenshotIsInDarkMode) ? 0 : 1);

         // Determine the stencils that will be tested
         List<String> stencilNames = DEFAULT_FACEBOOK_SCREENSHOT_NAVBAR_BUTTON_IDENTITIES.stream()
               .map(x -> "facebook"+exposureState+x).collect(Collectors.toList());

         // Loop through all stencils and run comparisons, exiting early on a match for each given
         // stencil
         HashMap<Integer, String> navbarHits = new HashMap<>();
         for (String stencilName : stencilNames) {
            int cursor = 0;
            while (cursor < potentialNavbarButtonStencils.size()) {
               for (String defaultFacebookScreenshotButtonState
                     : DEFAULT_FACEBOOK_SCREENSHOT_BUTTON_STATES) {
                  String thisStencilNameAugmented =
                        stencilName+defaultFacebookScreenshotButtonState;
                  double matchResult = stencilSimilarity(Args(
                        A("a", referenceStencilsPictograms.get(thisStencilNameAugmented)),
                        A("b", potentialNavbarButtonStencils.get(cursor)),
                        A("method",
                              DEFAULT_FACEBOOK_SCREENSHOT_POTENTIAL_NAVBAR_BUTTON_STENCIL_SIMILARITY_METHOD)
                  ));

                  log(Args(A("in", TAG),
                        A("message",
                              "Testing stencil '"+thisStencilNameAugmented
                                    +"' at index "+cursor+": Similarity: "+matchResult)));

                  if (matchResult > (1 - thresholdStencilDifference)) {
                     navbarHits.put(cursor, thisStencilNameAugmented);
                     break;
                  }
               }
               cursor ++;
            }
         }

         List<Integer> navbarHitsOfTargetState = navbarHits.keySet().stream()
               .filter(k -> (Objects.requireNonNull(navbarHits.get(k))
                     .endsWith(DEFAULT_FACEBOOK_SCREENSHOT_POTENTIAL_NAVBAR_BUTTON_STENCIL_TARGET_STATE)))
               .collect(Collectors.toList());

         // Assert that only one 'active' navbar exists
         if (navbarHitsOfTargetState.size() != 1) {

            log(Args(A("in", TAG),
                  A("message",
                        "navbarHitsOfTargetState.size() != 1 -> inFacebook = false")));

         } else {

            // Determine the colour palette of the target navbar button
            Integer navbarHitsOfTargetStateIndex = navbarHitsOfTargetState.get(0);
            List<Integer> paletteOfTargetNavbarButton = potentialNavbarButtonStencils
                  .get(navbarHitsOfTargetStateIndex).getColourPaletteNonWhitespace();

            // Assess if the colour palette contains Facebook's signature blue
            int signatureFacebookBlue = ((screenshotIsInDarkMode)
                  ? DEFAULT_FACEBOOK_SCREENSHOT_REFERENCE_COLOUR_BLUE_DARK
                  : DEFAULT_FACEBOOK_SCREENSHOT_REFERENCE_COLOUR_BLUE_LIGHT);
            if (paletteOfTargetNavbarButton.stream()
                  .noneMatch(x -> pixelDifferencePercentage(x, signatureFacebookBlue) <= thresholdBlue)) {

               log(Args(A("in", TAG), A("message",
                     "Colour palette does not contain Facebook signature blue -> inFacebook = false")));

            } else {

               log(Args(A("in", TAG), A("message",
                     "inFacebook = true")));

               // Record the positive match, and the necessary statistics
               inFacebook = true;
               statistics.put("tabActive",
                     Objects.requireNonNull(navbarHits.get(navbarHitsOfTargetStateIndex))
                           .replaceAll("("+String.join("|",
                                 DEFAULT_FACEBOOK_SCREENSHOT_BUTTON_STATES)+")", ""));
               navbarHits.replaceAll((k, v) -> Objects.requireNonNull(navbarHits.get(k))
                     .replaceAll("("+String.join("|",
                           DEFAULT_FACEBOOK_SCREENSHOT_BUTTON_STATES)+")", ""));
               statistics.put("tabsIdentified", navbarHits);
               statistics.put("tabsN", nTabs);
               statistics.put("navbarDividerIndex", retainedIndex);
               statistics.put("retainedIndexMidpoint", retainedIndexMidpoint);
               statistics.put("screenshotIsInDarkMode", screenshotIsInDarkMode);

            }
         }
      }

      log(Args(A("in", TAG), A("message",
            "Elapsed time: "+ (Math.abs((System.currentTimeMillis()) - elapsedTime)/1000))));
   }
}
