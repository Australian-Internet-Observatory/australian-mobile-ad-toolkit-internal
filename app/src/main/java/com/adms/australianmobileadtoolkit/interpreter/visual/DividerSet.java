package com.adms.australianmobileadtoolkit.interpreter.visual;

import static com.adms.australianmobileadtoolkit.Arguments.A;
import static com.adms.australianmobileadtoolkit.Arguments.Args;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.colourPalette;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.colourPaletteDiff;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.pixelsAtAxisI;

import android.graphics.Bitmap;

import com.adms.australianmobileadtoolkit.Arguments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class DividerSet {
   public List<Bitmap> dividerImages;
   public List<HashMap<String, Integer>> dividers;
   public static String DEFAULT_DIVIDE_IMAGE_ALONG_AXIS_ORIENTATION = "v";
   public static boolean DEFAULT_DIVIDE_IMAGE_ALONG_AXIS_ABSORB_MINIMUMS = true;
   public static boolean DEFAULT_DIVIDE_IMAGE_ALONG_AXIS_RETAIN_MINIMUMS = false;
   public static String DEFAULT_DIVIDE_IMAGE_ALONG_AXIS_MIN_DIVIDER_APPROACH = "complex";

   public static Double DEFAULT_DIVIDE_IMAGE_ALONG_AXIS_SCALE_MINOR_DIMENSION = 1.0;
   public static Double DEFAULT_DIVIDE_IMAGE_ALONG_AXIS_SCAN_UNTIL = 1.0;
   public static Double DEFAULT_DIVIDE_IMAGE_ALONG_AXIS_COLOUR_PALETTE_THRESHOLD = 0.1;
   public static Double DEFAULT_DIVIDE_IMAGE_ALONG_AXIS_WHITESPACE_THRESHOLD = 0.1;
   public static Double DEFAULT_DIVIDE_IMAGE_ALONG_AXIS_RETAIN_MINIMUMS_SCALE_ON = 0.01;
   public static Double DEFAULT_DIVIDE_IMAGE_ALONG_AXIS_RETAIN_MINIMUMS_SCALE_OFF = 0.0;
   public static int DEFAULT_DIVIDE_IMAGE_ALONG_AXIS_MAJOR_DIMENSION_Y = 1000;
   public static int DEFAULT_DIVIDE_IMAGE_ALONG_AXIS_MAJOR_DIMENSION_X = 500;

   public static int DIVIDE_IMAGE_ALONG_AXIS_MIN_DIVIDERS_FOR_ABSORPTION = 4;

   @SuppressWarnings("all")
   public DividerSet(Arguments args) {

      Bitmap bitmap = (Bitmap) args.get("bitmap", null);
      String orientation = (String) args.get("orientation",
            DEFAULT_DIVIDE_IMAGE_ALONG_AXIS_ORIENTATION);
      boolean absorbMinimums = (boolean) args.get("absorbMinimums",
            DEFAULT_DIVIDE_IMAGE_ALONG_AXIS_ABSORB_MINIMUMS);
      boolean retainMinimums = (boolean) args.get("retainMinimums",
            DEFAULT_DIVIDE_IMAGE_ALONG_AXIS_RETAIN_MINIMUMS);
      String minDividerApproach = (String) args.get("minDividerApproach",
            DEFAULT_DIVIDE_IMAGE_ALONG_AXIS_MIN_DIVIDER_APPROACH);
      Double scaleMinor = (Double) args.get("scaleMinor",
            DEFAULT_DIVIDE_IMAGE_ALONG_AXIS_SCALE_MINOR_DIMENSION);
      Double scanUntil = (Double) args.get("scanUntil",
            DEFAULT_DIVIDE_IMAGE_ALONG_AXIS_SCAN_UNTIL);
      Double colourPaletteThreshold = (Double) args.get("colourPaletteThreshold",
            DEFAULT_DIVIDE_IMAGE_ALONG_AXIS_COLOUR_PALETTE_THRESHOLD);
      Double whitespaceThreshold = (Double) args.get("whitespaceThreshold",
            DEFAULT_DIVIDE_IMAGE_ALONG_AXIS_WHITESPACE_THRESHOLD);
      int dimensionMajor = (Objects.equals(orientation, "v"))
            ? bitmap.getHeight() : bitmap.getWidth();
      Integer dimensionMinor = (Objects.equals(orientation, "v"))
            ? bitmap.getWidth() : bitmap.getHeight();

      int majorDimensionAdjustedSize = Math.min(dimensionMajor, ((Objects.equals(orientation, "v"))
            ? DEFAULT_DIVIDE_IMAGE_ALONG_AXIS_MAJOR_DIMENSION_Y
            : DEFAULT_DIVIDE_IMAGE_ALONG_AXIS_MAJOR_DIMENSION_X));

      Double scaleMinimumDividerSize = ((retainMinimums)
            ? DEFAULT_DIVIDE_IMAGE_ALONG_AXIS_RETAIN_MINIMUMS_SCALE_OFF
            : DEFAULT_DIVIDE_IMAGE_ALONG_AXIS_RETAIN_MINIMUMS_SCALE_ON);
      // Scale the bitmap
      List<Integer> scaledDimensions = new ArrayList<>(Arrays.asList(
            majorDimensionAdjustedSize, (int) Math.ceil(dimensionMinor*scaleMinor)));
      if (Objects.equals(orientation, "v")) { Collections.reverse(scaledDimensions); }
      Bitmap scaledBitmap = Bitmap.createScaledBitmap(
            bitmap, scaledDimensions.get(0), scaledDimensions.get(1), false);
      // Determine what is classified as a minimum divider
      int minimumDividerSize = (int) Math.ceil(dimensionMajor*scaleMinimumDividerSize);
      // Scale the major dimension's reference size in accordance with the scaled bitmap
      int majorDimensionScaledSize = (int) Math.floor(((Objects.equals(orientation, "v")) ?
            scaledBitmap.getHeight() : scaledBitmap.getWidth())*scanUntil);
      // Retrieve the colour palettes for each of the rows across the major dimension's axis
      List<HashMap<String,Integer>> palettes = IntStream.range(0, majorDimensionScaledSize)
            .mapToObj(i -> colourPalette(Args(
                  A("sample", pixelsAtAxisI(scaledBitmap, orientation, i)),
                  A("threshold", colourPaletteThreshold)))
            ).collect(Collectors.toList());
      // Calculate the maximum differences of said colour palettes
      List<Double> colourPaletteDiffMaxs = IntStream.range(0, majorDimensionScaledSize)
            .mapToObj(i -> colourPaletteDiff(
                  Args(A("palette", palettes.get(i)))
            )).collect(Collectors.toList());
      // Calculate the alternations between said maximums to derive divider bounds
      List<Boolean> colourPaletteDiffMaxAlts = IntStream.range(0, colourPaletteDiffMaxs.size()-1)
            .mapToObj(i -> Stream.of(Arrays.asList(i, i+1), Arrays.asList(i+1, i)).anyMatch(j ->
                  ((colourPaletteDiffMaxs.get(j.get(0)) > whitespaceThreshold)
                        && (colourPaletteDiffMaxs.get(j.get(1)) <= whitespaceThreshold)))
            ).collect(Collectors.toList());
      // Structure the divider bounds
      List<Integer> dividerBounds = IntStream.range(0, colourPaletteDiffMaxAlts.size())
            .filter(colourPaletteDiffMaxAlts::get).boxed().map(x -> x+1).collect(Collectors.toList());
      // If there are no divider bounds to report, generate a single candidate spanning the entire
      // image, and return it
      if (dividerBounds.isEmpty()) {
         HashMap<String, Integer> candidate = new HashMap<>();
         candidate.put("start", 0);
         candidate.put("end", dimensionMajor-1);
         dividerImages = Collections.singletonList(bitmap);
         dividers = Collections.singletonList(candidate);
      } else {
         // Add the extremes to the divider bounds (if they have not yet been initiated)
         if (dividerBounds.get(0) != 0)
            dividerBounds = Stream.concat(
                  Stream.of(0), dividerBounds.stream()
            ).collect(Collectors.toList());
         if (!Objects.equals(dividerBounds.get(dividerBounds.size() - 1), majorDimensionScaledSize))
            dividerBounds.add(majorDimensionScaledSize);
         // Upscale the divider bounds back to the original dimensions
         dividerBounds = dividerBounds.stream().map(
               x -> (int) Math.ceil(x/(majorDimensionAdjustedSize/(double) dimensionMajor))
         ).collect(Collectors.toList());

         // There need to be at least so many dividers in order to undertake absorption of dividers
         // TODO - this condition was previously done in realtime - might affect code
         if (dividerBounds.size() >= DIVIDE_IMAGE_ALONG_AXIS_MIN_DIVIDERS_FOR_ABSORPTION) {
            int lowerStart = 1, lowerEnd = 0, upperStart = 0, upperEnd = 0;
            while (lowerStart != -1) {
               // Initiate the loop - in instances where there are no more dividers to augment, this
               // declaration will close the loop
               lowerStart = -1;
               for (int i = 0; i < dividerBounds.size()-2; i ++) {
                  List<Integer> finalDividerBounds1 = dividerBounds;
                  int finalI = i;
                  int[] localDividerSizes = IntStream.range(-1, 2).map(x ->
                        Math.abs(finalDividerBounds1.get(Math.max(finalI+x,0))
                              - finalDividerBounds1.get(Math.min(finalI+1+x,
                              finalDividerBounds1.size()-1)))
                  ).toArray();
                  if (Objects.equals(minDividerApproach, "simple")) {
                     // The 'simple' case goes over only the current divider, removing only the bound
                     // if it falls below the minimum size while absorbing minimums
                     if (absorbMinimums && (localDividerSizes[1] <= minimumDividerSize)) {
                        // Remove this index
                        int offset = (i == dividerBounds.size() - 2) ? 0 : 1;
                        lowerStart = 0;
                        lowerEnd = i + offset;
                        upperStart = Math.min(i + 1 + offset, dividerBounds.size() - 1);
                        upperEnd = dividerBounds.size();
                        break;
                     }
                  } else {
                     // The 'complex' case examines the forward, behind, and current dividers, and removes
                     // entire partitions (composed of two dividers) if they are below the minimum size
                     // while absorbing minimums
                     if (i >= 1) {
                        // TODO - this has been augmented
                        if (absorbMinimums) {
                           int offset = (localDividerSizes[0] <= minimumDividerSize)
                                 ? -1 : (localDividerSizes[1] <= minimumDividerSize)
                                 ? 0 : (localDividerSizes[2] <= minimumDividerSize)
                                 ? 1 : -2;
                           // If either of the current divider, the behind divider, or the forward
                           // divider are below the minimum size, absorb (provided that we are absorbing
                           // minimums)
                           if (offset != -2) {
                              lowerStart = 0;
                              lowerEnd = i + offset;
                              upperStart = Math.min(i + 2 + offset, dividerBounds.size() - 1);
                              upperEnd = dividerBounds.size();
                              break;
                           }
                        }
                        // Irrespective of whether we are absorbing minimums, the 'complex' case removes
                        // single dividers
                        if (Arrays.stream(localDividerSizes).anyMatch(x -> x <= minimumDividerSize)) {
                           int offset = (localDividerSizes[0] <= localDividerSizes[2]) ? 1 : 0;
                           lowerStart = 0;
                           lowerEnd = i - offset;
                           upperStart = i + (1 - offset);
                           upperEnd = dividerBounds.size();
                           break;
                        }
                     }
                  }
               }
               // If the lowerStart variable is not -1, then augment the dividerBounds
               if (lowerStart != -1) {
                  dividerBounds = Stream.concat(
                        dividerBounds.subList(lowerStart, lowerEnd).stream(),
                        dividerBounds.subList(upperStart, upperEnd).stream()
                  ).collect(Collectors.toList());
               }
            }
         }

         // Expand the dividers into HashMaps, and subtract one unit from the end of each ending divider
         // to avoid overlap
         List<Integer> finalDividerBounds = dividerBounds;
         List<HashMap<String, Integer>> dividerBoundsJSON = IntStream.range(0, dividerBounds.size()-1)
               .mapToObj(i -> {
                  HashMap<String, Integer> thisDividerBound = new HashMap<>();
                  thisDividerBound.put("start", finalDividerBounds.get(i));
                  thisDividerBound.put("end", finalDividerBounds.get(i+1)-1);
                  return thisDividerBound;
               }).collect(Collectors.toList());


         // Remove dividers that are only a pixel long
         dividers = dividerBoundsJSON.stream().filter( x -> {
            int length = Math.abs(x.get("start")-x.get("end"));
            int cropW = (Objects.equals(orientation, "v")) ? bitmap.getWidth() : length;
            int cropH = (Objects.equals(orientation, "v")) ? length : bitmap.getHeight();
            return ((cropW > 0) && (cropH > 0));
         }).collect(Collectors.toList());

         // Stretch both the 'start' and 'end' positions of the dividers
         for (int j : Arrays.asList(-1,+1)) {
            for (int i = 0; i < dividers.size(); i ++) {
               List<String> indices = Arrays.asList("start", "end");
               if (j == 1) { Collections.reverse(indices); }
               if (((i != 0) && (j == -1)) || ((i != (dividers.size()-1)) && (j == 1))) {
                  dividers.get(i).put(
                        indices.get(0), dividers.get(i+j).get(indices.get(1))+(j*-1));
               }
            }
         }

         // Produce the images that correspond to the dividers
         dividerImages = new ArrayList<>();
         for (int i = 0; i < dividers.size(); i ++) {
            int length = Math.abs(dividers.get(i).get("start")-dividers.get(i).get("end"))+1;
            int cropX = (Objects.equals(orientation, "v")) ? 0 : dividers.get(i).get("start");
            int cropY = (Objects.equals(orientation, "v")) ? dividers.get(i).get("start") : 0;
            int cropW = (Objects.equals(orientation, "v")) ? bitmap.getWidth() : length;
            int cropH = (Objects.equals(orientation, "v")) ? length : bitmap.getHeight();
            dividerImages.add(Bitmap.createBitmap(bitmap, cropX, cropY, cropW, cropH));
         }
      }
   }

}
