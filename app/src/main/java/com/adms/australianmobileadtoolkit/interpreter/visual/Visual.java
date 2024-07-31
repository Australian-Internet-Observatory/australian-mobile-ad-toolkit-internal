package com.adms.australianmobileadtoolkit.interpreter.visual;

import static com.adms.australianmobileadtoolkit.Arguments.A;
import static com.adms.australianmobileadtoolkit.Arguments.Args;
import static com.adms.australianmobileadtoolkit.Common.combinationPairs;
import static com.adms.australianmobileadtoolkit.Common.frequencyInArray;
import static com.adms.australianmobileadtoolkit.Common.optionalGetDouble;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.Log;

import com.adms.australianmobileadtoolkit.Arguments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Visual {

   // TODO - rename instances of visual components to dividers

   private static String TAG = "Visual";

   /**
    * This function converts a colour to a hex string
    * @param colour The colour as an integer
    * @return The colour as a hex string
    */
   public static String colourToHex(Integer colour) {
      return String.format("#%06X", (0xFFFFFF & colour));
   }

   /**
    * This function assists the colourQuantizeBitmap function in snapping a colour channel to a given
    * interval
    * @param colourChannel
    * @param interval
    * @return
    */
   private static int colourQuantizeBitmapSnap(int colourChannel, int interval) {
      return Math.min(((Math.floorDiv(colourChannel, interval) * interval) + (interval / 2)), 255);
   }


   public static int DEFAULT_COLOUR_QUANTIZE_BITMAP_INTERVAL = 64;

   /**
    * This function performs colour quantization on a bitmap, by snapping the RGB values of all
    * comprising pixels to a supplied interval
    * @param args {
    *             (Bitmap) bitmap:
    *             (int) interval:
    * }
    * @return The quantized bitmap
    */
   public static Bitmap colourQuantizeBitmap(Arguments args) {
      Bitmap bitmap = (Bitmap) args.get("bitmap", null);
      int interval = (int) args.get("interval", DEFAULT_COLOUR_QUANTIZE_BITMAP_INTERVAL);
      // Copy the bitmap
      Bitmap newBitmap = bitmap.copy(bitmap.getConfig(), true);
      // For all pixels in the image...
      for (int xx = 0; xx < bitmap.getWidth(); xx ++) {
         for (int yy = 0; yy < bitmap.getHeight(); yy ++) {
            // Reconstruct the pixels by 'snapping' the colour channels
            int thisColour = bitmap.getPixel(xx,yy);
            newBitmap.setPixel(xx,yy,Color.valueOf(Color.rgb(
                  colourQuantizeBitmapSnap(Color.red(thisColour), interval),
                  colourQuantizeBitmapSnap(Color.green(thisColour), interval),
                  colourQuantizeBitmapSnap(Color.blue(thisColour), interval))
            ).toArgb());
         }
      }
      return newBitmap;
   }

   /**
    * This function retrieves the colour-based difference difference between two pixels,
    * as the sum of the difference of the respective colour channels
    * @param a An integer representing the first pixel
    * @param b An integer representing the second pixel
    * @return The difference between two pixels
    */
   public static Integer pixelDifference(int a, int b) {
      return (Math.abs(Color.red(a) - Color.red(b))
            + Math.abs(Color.green(a) - Color.green(b))
            + Math.abs(Color.blue(a) - Color.blue(b)));
   }

   /**
    * This function converts the integer pixel difference obtained from the 'pixelDifference'
    * function to a percentage value
    * @param a An integer representing the first pixel
    * @param b An integer representing the second pixel
    * @return The difference between two pixels as a percentage
    */
   public static Double pixelDifferencePercentage(int a, int b) {
      return Double.valueOf(pixelDifference(a, b)) / (255 * 3);
   }


   /**
    * This function returns a pixel array at a designated index of an axis (either
    * vertical or horizontal) of an image
    * @param bitmap
    * @param orientation
    * @param i
    * @return
    */
   public static int[] pixelsAtAxisI(Bitmap bitmap, String orientation, int i) {
      return IntStream.range(
               // The length of the pixel array is equivalent to the length of the alternative axis
               0, ((Objects.equals(orientation, "v")) ?
                        bitmap.getWidth() : bitmap.getHeight())
                  // Retrieve all pixels for said axis at the given index
                  ).map(zz -> (Objects.equals(orientation, "v")) ?
                           bitmap.getPixel(zz, i) : bitmap.getPixel(i, zz)).toArray();
   }

   /**
    *
    * @param row
    * @return
    */
   public static boolean isRowWhitespace(int[] row) {
      List<Double> diffs = new ArrayList<>();
      for (int i = 0; i < row.length-1; i ++) {
         diffs.add(pixelDifferencePercentage(row[i], row[i+1]));
      }
      return (optionalGetDouble(diffs.stream().mapToDouble(x->x).max()) < 0.02);
   }

   public static Integer DEFAULT_THUMBNAIL_SIZE = 10;

   /**
    * This function generates a scaled-down version of a bitmap to a desired size
    * @param args {
    *             (Bitmap) bitmap: The bitmap to scale down
    *             (int) s: The default desired size if width and height are not supplied
    *             (int) w: The desired width
    *             (int) h: The desired height
    * }
    * @return The scaled down version of the given bitmap
    */
   public static Bitmap thumbnail(Arguments args) {
      Bitmap bitmap = (Bitmap) args.get("bitmap", null);
      Integer s = (Integer) args.get("s", DEFAULT_THUMBNAIL_SIZE);
      Integer w = (args.has("w")) ? (Integer) args.get("w", DEFAULT_THUMBNAIL_SIZE) : s;
      Integer h = (args.has("h")) ? (Integer) args.get("h", DEFAULT_THUMBNAIL_SIZE) : s;
      return Bitmap.createScaledBitmap(bitmap, w, h, true);
   }

   public static Double DEFAULT_COLOUR_PALETTE_LINKING_THRESHOLD = 0.3;

   // TODO - implement sub-sampling for speed?
   /**
    * This function generates a colour palette from an array of colours
    * @param args {
    *             (int[]) sample: An array containing the colours as integers
    *             (double) threshold: The percentage threshold where two colours occupies the same
    *             entry in the palette
    * }
    * @return A hashmap with hex colours as keys and the values are the number of occurrences of
    * each colour (or those similar to the colour by a certain threshold percentage)
    */
   public static HashMap<String, Integer> colourPalette(Arguments args) {
      int[] sample = (int[]) args.get("sample", new int[0]);
      Double linkingThreshold = (Double) args.get("threshold",
                                                      DEFAULT_COLOUR_PALETTE_LINKING_THRESHOLD);
      HashMap<String, Integer> palette = new HashMap<>();

       // Returns the nearest colour in the palette, or the given colour if none found
      Function<String, String> getNearestColourInPalette =
              (colour) -> palette.keySet()
                                 .stream()
                                 .filter(paletteKey ->
                                         pixelDifferencePercentage(
                                                 Color.parseColor(paletteKey),
                                                 Color.parseColor(colour)
                                         ) < linkingThreshold
                                 )
                                 .findFirst()
                                 .orElse(colour);

      // For each pixel within the sample, add 1 to the palette key containing the colour
      // closest to it
      for (Integer thisPixel : sample) {
//         boolean found = false;
//         // For each existing colour within the palette
//         for (String hexColour : palette.keySet()) {
//            // If the pixel's colour differs less from the palette's colour than the threshold
//            if (pixelDifferencePercentage(Color.parseColor(hexColour), thisPixel)
//                                                                  < linkingThreshold) {
//               // Then apply it to the existing colour
//               palette.put(hexColour, Objects.requireNonNull(palette.get(hexColour)) + 1);
//               found = true;
//               break;
//            }
//         }
//         // Otherwise generate it within the palette
//         if (!found) {
//            palette.put(colourToHex(thisPixel), 1);
//         }
         String nearestColour = getNearestColourInPalette.apply(colourToHex(thisPixel));
         int currentCount = Objects.requireNonNull(palette.getOrDefault(nearestColour, 0));
         palette.put(nearestColour, currentCount + 1);
      }
      return palette;
   }

   /*
   *
   *
   *
   * TODO - implement sub-sampling for speed?
   *
   * */
   public static Double DEFAULT_COLOUR_PALETTE_FROM_IMAGE_LINKING_THRESHOLD
                                                         = DEFAULT_COLOUR_PALETTE_LINKING_THRESHOLD;

   // TODO - threshold is too weak in some cases

   /**
    * This function adapts the 'colourPalette' function, to retrieve a colour palette from an image
    * @param args {
    *             (Bitmap) bitmap: The bitmap
    *             (double) threshold: The threshold (percentage) to which two colours are considered
    *             the same in the palette
    * }
    * @return A hashmap with hex colours as keys and the values are the number of occurrences of
    * each colour in the image (or those similar to the colour by a certain threshold percentage)
    */
   public static HashMap<String, Integer> colourPaletteFromImage(Arguments args) {
      Bitmap bitmap = (Bitmap) args.get("bitmap", null);
      Double linkingThreshold = (Double) args.get("threshold",
                                             DEFAULT_COLOUR_PALETTE_FROM_IMAGE_LINKING_THRESHOLD);
      // Construct an array of colours that correspond directly to the pixels of the image
      int[] pixels = new int[bitmap.getWidth()*bitmap.getHeight()];
      int i = 0;
      for (int xx = 0; xx < bitmap.getWidth(); xx ++) {
         for (int yy = 0; yy < bitmap.getHeight(); yy++) {
            pixels[i] = bitmap.getPixel(xx,yy); i ++;
         }
      }
      return colourPalette(Args(A("sample", pixels), A("threshold", linkingThreshold)));
   }

   public static Integer DEFAULT_COLOUR_PALETTE_DIFF_COLOURS_N = 10;
   public static String DEFAULT_COLOUR_PALETTE_DIFF_APPROACH = "max";

   /**
    * This function retrieves a statistic (min, max, or average) from the pair-wise comparisons of
    * all differences of all colours within a colour palette
    * @param args {
    *             (HashMap<String, Integer>) palette
    *             (int) maxColours
    *             (String) approach
    * }
    * @return
    */
   @SuppressWarnings("all")
   public static Double colourPaletteDiff(Arguments args) {
      HashMap<String, Integer> palette = (HashMap<String, Integer>)
            args.get("palette", new HashMap<>());
      Integer maxColours = (Integer) args.get("maxColours", DEFAULT_COLOUR_PALETTE_DIFF_COLOURS_N);
      String approach = (String) args.get("approach", DEFAULT_COLOUR_PALETTE_DIFF_APPROACH);
      // If there are more than a given number of colours in the palette, we can safely determine
      // that the palette is too varied for meaningful analysis
      if (palette.keySet().size() >= maxColours) return 1.0;
      // Report no variance (zero) if there is only one colour in the palette
      if (palette.keySet().size() == 1) return 0.0;
      List<Integer> colours = palette
              .keySet()
              .stream()
              .map(Color::parseColor)
              .collect(Collectors.toList());
      // Assemble the results of all cross-compared differences among all colours
      // that constitute the palette
      List<Double> results = combinationPairs(
            IntStream.range(0, colours.size())
                     .boxed()
                     .collect(Collectors.toList()))
              .stream()
              .map(c -> pixelDifferencePercentage(
                  colours.get(c.get(0)),colours.get(c.get(1))))
              .collect(Collectors.toList());
      // Route the output, depending on the approach
      switch (approach) {
         case "min" : return Collections.min(results);
         case "avg" : return optionalGetDouble(results.stream().mapToDouble(x->x).average());
         default :  return Collections.max(results);
      }
   }


   /**
    * Find the uniformity of a colour list by finding the maximum difference between pair of
    * consecutive pixels
    * @param thisList The colour list
    * @return The maximum percentage difference between any pair of consecutive pixels
    */
   public static Double colourListUniformity(List<Integer> thisList) {
      double maxDiff = 0.0;
      for (int i = 0; i < thisList.size()-1; i ++) {
         double thisPixelDiff = pixelDifferencePercentage(thisList.get(i), thisList.get(i+1));
         if (thisPixelDiff > maxDiff) {
            maxDiff = thisPixelDiff;
         }
      }
      return maxDiff;
   }

   /**
    * This function converts a colour palette's keys into a list of distinct colours
    * @param colourPalette
    * @return
    */
   public static List<Integer> colourPaletteKeysToList(HashMap<String, Integer> colourPalette) {
      return colourPalette.keySet().stream().map(Color::parseColor).collect(Collectors.toList());
   }

   /**
    * This function handles the process of assessing rows of pixels, as part of cropping whitespace
    * from images
    * @param thisImage
    * @param h
    * @param w
    * @param whitespacePixel
    * @param threshold
    * @param orientation
    * @param bound
    * @return
    */
   public static int cropWhitespaceSubFunction(Bitmap thisImage, int h, int w,
                          int whitespacePixel, double threshold, String orientation, String bound) {
      int cursor = 0;
      boolean isWhitespacePixel = true;
      int dimension = (Arrays.asList("l","r").contains(bound)) ? w : h;
      // From the bound, start evaluating rows of pixels
      while (isWhitespacePixel && (cursor < (dimension - 1))) {
         int appliedCursor =
               (Arrays.asList("l", "t").contains(bound)) ? cursor : ((dimension - cursor) - 1);
         int[] thisRow = pixelsAtAxisI(thisImage, orientation, appliedCursor);
         // When a row is identified that no longer contains whitespace pixels, exit...
         isWhitespacePixel = Arrays.stream(thisRow)
               .noneMatch(x -> (pixelDifferencePercentage(x, whitespacePixel) > threshold));
         if (isWhitespacePixel) {
            cursor++;
         }
      }
      return cursor;
   }

   public static double DEFAULT_CROP_WHITESPACE_THRESHOLD = 0.2;

    /**
    * Provided an image and the prescription of a whitespace pixel, this function
    * crops whitespace from an image
    * @param args
    * @return
    */
   @SuppressWarnings("all")
   public static Bitmap cropWhitespace(Arguments args) {
      Bitmap bitmap = (Bitmap) args.get("bitmap", null);

      int whitespacePixel = (int) args.get("whitespacePixel", getWhitespacePixel(
            Args(A("bitmap", bitmap))));
      Double threshold = (Double) args.get("threshold", DEFAULT_CROP_WHITESPACE_THRESHOLD);

      // Sometimes it may not be possible to crop the image, in which case we return it
      try {
         HashMap<String, Integer> thisBounds = new HashMap<>();
         int w = bitmap.getWidth();
         int h = bitmap.getHeight();
         // Evaluate all bounds on the image, cropping whitespace accordingly
         for (int i = 0; i < 2; i ++) {
            String orientation = (i == 0) ? "h" : "v";
            for (int j = 0; j < 2; j ++) {
               String bound = (i == 0) ? ((j == 0) ? "l" : "r") : ((j == 0) ? "t" : "b");
               thisBounds.put(bound,
                  cropWhitespaceSubFunction(bitmap, h,w,whitespacePixel, threshold, orientation,bound));
            }
         }
         // Crop the image
         Bitmap output = Bitmap.createBitmap(bitmap,
               thisBounds.get("l"),thisBounds.get("t"),
               (w-thisBounds.get("r"))-thisBounds.get("l"),
               (h-thisBounds.get("b"))-thisBounds.get("t"));
         // If either axis has been reduced to zero, return the original image
         if ((output.getWidth() == 0) || (output.getHeight() == 0)) {
            output = bitmap;
         }
         return output;
      } catch (Exception e) {
         return bitmap;
      }
   }

   public static int DEFAULT_IMAGE_TO_STENCIL_SIZE_UNIT = 16;
   public static HashMap<String, Integer> DEFAULT_IMAGE_TO_STENCIL_SIZE =
         new HashMap<String, Integer>() {{ put("s", DEFAULT_IMAGE_TO_STENCIL_SIZE_UNIT); }};
   public static double DEFAULT_IMAGE_TO_STENCIL_SNAP_THRESHOLD = 0.2;
   public static double DEFAULT_IMAGE_TO_STENCIL_CROP_THRESHOLD = 0.1;
   public static double DEFAULT_IMAGE_TO_STENCIL_COLOUR_PALETTE_THRESHOLD = 0.1;
   public static boolean DEFAULT_IMAGE_TO_STENCIL_IS_REFERENCE = true;

   /**
    * This function converts an image into a stencil
    *
    * snapThreshold: The threshold that separates positive pixels from negative pixels
    * cropThreshold: The threshold that determines how sensitive the image is to cropping
    * colourPaletteThreshold: The threshold that separates/groups colours within the image
    * @param args
    * @return
    */
   @SuppressWarnings("all")
   public static Stencil imageToStencil(Arguments args) {
      Bitmap bitmap = (Bitmap) args.get("bitmap", null);

      int whitespacePixel = (int) args.get("whitespacePixel", getWhitespacePixel(
                                                                        Args(A("bitmap", bitmap))));
      HashMap<String, Integer> size = args.getHashMap("size", DEFAULT_IMAGE_TO_STENCIL_SIZE);
      HashMap<String, Integer> exclusion = args.getHashMap("exclusion", null);
      double snapThreshold = (double) args.get("snapThreshold",
                                                DEFAULT_IMAGE_TO_STENCIL_SNAP_THRESHOLD);
      double cropThreshold = (double) args.get("cropThreshold",
                                                DEFAULT_IMAGE_TO_STENCIL_CROP_THRESHOLD);
      double colourPaletteThreshold = (double) args.get("colourPaletteThreshold",
                                                DEFAULT_IMAGE_TO_STENCIL_COLOUR_PALETTE_THRESHOLD);
      boolean isReference = (boolean) args.get("isReference",
                                                DEFAULT_IMAGE_TO_STENCIL_IS_REFERENCE);
      // Generate the cropped bitmap
      Bitmap croppedBitmap = cropWhitespace(Args(
            A("bitmap", bitmap), A("whitespacePixel", whitespacePixel), A("threshold", cropThreshold)));
      // Generate the scaled version of the cropped bitmap
      Bitmap croppedRescaledBitmap = (size.get("s") != null)
                                       ? thumbnail(Args(
                                             A("bitmap", croppedBitmap), A("s", size.get("s"))))
                                       : thumbnail(Args(
                                             A("bitmap", croppedBitmap),
                                                      A("w", size.get("w")), A("h", size.get("h"))));
      // Generate the stencil of the scaled cropped bitmap
      int[][] stencil = new int[croppedRescaledBitmap.getWidth()][croppedRescaledBitmap.getHeight()];
      for (int xx = 0; xx < croppedRescaledBitmap.getWidth(); xx ++) {
         for (int yy = 0; yy < croppedRescaledBitmap.getHeight(); yy ++) {
            stencil[xx][yy] =
               ((pixelDifferencePercentage(croppedRescaledBitmap.getPixel(xx,yy), whitespacePixel)
                                                                           > snapThreshold) ? 1 : 0);
         }
      }
      // Apply the exclusion (if it exists)
      if ((exclusion != null) && (exclusion.get("x") != null)) {
         for (int xx = 0; xx < croppedRescaledBitmap.getWidth(); xx ++) {
            for (int yy = 0; yy < croppedRescaledBitmap.getHeight(); yy ++) {
               stencil[xx][yy] = ((
                     ((exclusion.get("y")+exclusion.get("h")) > yy) && (yy >= exclusion.get("y"))
                     && (((exclusion.get("x")+exclusion.get("w")) > xx) && (xx >= exclusion.get("x")))
                  ) ? -1 : stencil[xx][yy]);
            }
         }
      }

      Stencil output = new Stencil().setStencil(stencil);

      // Reference stencils are yielded early, as they don't carry colour statistics
      if (isReference) {
         return output;
      }

      // Assess the non-whitespace pixels of the image, for usage in understanding the image's
      // colour palette
      List<Integer> thisColourPalette = colourPaletteKeysToList(
                  colourPaletteFromImage(Args(
                        A("bitmap", colourQuantizeBitmap(Args(
                              A("bitmap", croppedRescaledBitmap)))
                        ), A("threshold", colourPaletteThreshold))));
      List<Integer> colourPaletteNonWhitespace = thisColourPalette.stream().filter(
               x -> (pixelDifferencePercentage(x, whitespacePixel) > colourPaletteThreshold)
         ).collect(Collectors.toList());
      output.setColourPaletteNonWhitespace(colourPaletteNonWhitespace);
      return output;
   }

   public static int DEFAULT_IMAGE_TO_PICTOGRAM_SIZE_UNIT = 16;


   public static HashMap<String, Integer> DEFAULT_IMAGE_TO_PICTOGRAM_SIZE =
                                                new HashMap<String, Integer>() {{
                                                   put("w", DEFAULT_IMAGE_TO_PICTOGRAM_SIZE_UNIT);
                                                   put("h", DEFAULT_IMAGE_TO_PICTOGRAM_SIZE_UNIT);
                                                }};
   public static int DEFAULT_IMAGE_TO_PICTOGRAM_QUANTIZATION_INTERVAL = 5;
   public static boolean DEFAULT_IMAGE_TO_PICTOGRAM_CROP = true;

   /**
    * This function converts an image into a pictogram
    * @param args
    * @return
    */
   @SuppressWarnings("all")
   public static Bitmap imageToPictogram(Arguments args) {
      Bitmap bitmap = (Bitmap) args.get("bitmap", null);
      HashMap<String, Integer> size = args.getHashMap("size",
                                                   DEFAULT_IMAGE_TO_PICTOGRAM_SIZE);
      int quantizationInterval = (int) args.get("quantizationInterval",
                                                   DEFAULT_IMAGE_TO_PICTOGRAM_QUANTIZATION_INTERVAL);
      boolean crop = (boolean) args.get("crop",
                                                   DEFAULT_IMAGE_TO_PICTOGRAM_CROP);
      // Crop, quantize, and scale the image, before returning it
      Bitmap thisBitmapCropped = (crop)
               ? cropWhitespace(Args(A("bitmap", bitmap))) : bitmap;
      return colourQuantizeBitmap(
            Args(A("bitmap", Bitmap.createScaledBitmap(
                     thisBitmapCropped, size.get("w"), size.get("h"), false)),
                  A("interval", quantizationInterval)));
   }


   public static int DEFAULT_STENCIL_SIMILARITY_STRIDE = 6;
   public static int DEFAULT_STENCIL_SIMILARITY_STRIDE_IN_SAMPLE = 6;
   public static int DEFAULT_STENCIL_SIMILARITY_STRIDE_IN_SAMPLE_INTERVAL = 2;
   public static boolean DEFAULT_STENCIL_SIMILARITY_DEEP_SAMPLING = false;
   public static String DEFAULT_STENCIL_SIMILARITY_METHOD = "multiplied";

   /**
    * This function checks the similarity between two stencils. In areas that aren't an exclusion,
    * the values between both stencils are compared
    * @param args
    * @return
    */
   public static double stencilSimilarity(Arguments args) {
      Stencil a = (Stencil) args.get("a", null);
      Stencil b = (Stencil) args.get("b", null);
      boolean deepSampling = (boolean) args.get("deepSampling",
                                             DEFAULT_STENCIL_SIMILARITY_DEEP_SAMPLING);
      String method = (String) args.get("method",
                                             DEFAULT_STENCIL_SIMILARITY_METHOD);
      int strideInStencil = (int) args.get("strideAcrossStencil",
                                             DEFAULT_STENCIL_SIMILARITY_STRIDE);
      int strideInSample = (int) args.get("strideInSample",
                                             DEFAULT_STENCIL_SIMILARITY_STRIDE_IN_SAMPLE);
      int strideInSampleInterval = (int) args.get("strideInSampleInterval",
                                             DEFAULT_STENCIL_SIMILARITY_STRIDE_IN_SAMPLE_INTERVAL);
      int[][] aS = a.getStencil(), bS = b.getStencil();
      double readingsFP = 0, readingsAP = 0, readingsFN = 0, readingsAN = 0;
      // Determine the dominant readings for both stencils - an image may be
      // predominantly whitespace, and this may affect the distinction of the comparison)
      int dominantReadingsA = ((frequencyInArray(aS, 1) > frequencyInArray(aS, 0)) ? 1 : 0);
      int dominantReadingsB = ((frequencyInArray(bS, 1) > frequencyInArray(bS, 0)) ? 1 : 0);
      // Traverse both axes of stencils, in intervals equivalent to the stride
      for (int xx = 0; xx < aS.length; xx += strideInStencil) {
         for (int yy = 0; yy < aS[0].length; yy += strideInStencil) {
            // Depending on whether the comparison is deep or not, assess samples of
            // pixels between the stencils
            for (int j = ((deepSampling) ? Math.max(-strideInSample, -xx) : 0);
                 j < ((deepSampling) ?
                       Math.min(strideInSample, Math.abs(xx-(aS.length-1))) : 1);
                                                         j += strideInSampleInterval) {
               for (int k = ((deepSampling) ? Math.max(-strideInSample, -yy) : 0);
                    k < ((deepSampling) ?
                          Math.min(strideInSample, Math.abs(yy-(aS[0].length-1))) : 1);
                                                            k += strideInSampleInterval) {
                  int aV = aS[xx+j][yy+k], bV = bS[xx+j][yy+k];
                  // If the assessed pixel is not an excluded pixel
                  if ((aV != -1) && (bV != -1)) {
                     // Tally its reading
                     if ((aV == bV)) {
                        if (aV != dominantReadingsA) { readingsFP += 1; } else { readingsFN += 1; }
                        if (bV != dominantReadingsB) { readingsFP += 1; } else { readingsFN += 1; }
                     } else {
                        if (aV != dominantReadingsA) { readingsAP += 1; } else { readingsAN += 1; }
                        if (bV != dominantReadingsB) { readingsAP += 1; } else { readingsAN += 1; }
                     }
                  }
               }
            }
         }
      }
      // Quantify the similarity
      return (Objects.equals(method, "summated")) // v1 is summated
            ? (readingsFP + readingsFN) / ((readingsFP + readingsAP) + (readingsFN + readingsAN))
            : Math.min(((((readingsFP / (readingsFP + readingsAP))
                                             * (readingsFN / (readingsFN + readingsAN)))) * 2), 1.0);
   }

   /**
    * This function converts a stencil to a string (for logging purposes)
    * @param thisStencil
    * @return
    */
   public static String stencilToString(int[][] thisStencil) {
      StringBuilder combinedString = new StringBuilder();
      for (int yy = 0; yy < thisStencil[0].length; yy +=3) {
         StringBuilder line = new StringBuilder();
         for (int[] ints : thisStencil) {
            line.append(String.valueOf(String.valueOf(ints[yy]).toCharArray()).replaceAll("-1", "□")
                  .replaceAll("1", "#").replaceAll("0", "`"));
         }
         combinedString.append("\t\t").append(line+"\n");
      }
      return combinedString.toString();
   }

   /**
    * This function determines the similarity between two pictograms - note that supplying a
    * maskPictogram will create an exclusion on the comparison of any pixels between pictograms that
    * are black within the mask
    * @param a
    * @param b
    * @param maskPictogram
    * @return
    */
   public static Double pictogramSimilarity(Bitmap a, Bitmap b, Bitmap maskPictogram) {
      List<Double> diffs = new ArrayList<>();
      for (int xx = 0; xx < a.getWidth(); xx ++) {
         for (int yy = 0; yy < a.getHeight(); yy ++) {
            if (maskPictogram == null) {
               diffs.add(pixelDifferencePercentage(a.getPixel(xx,yy), b.getPixel(xx,yy)));
            } else if (maskPictogram.getPixel(xx,yy) != Color.rgb(0,0,0)) {
               diffs.add(pixelDifferencePercentage(a.getPixel(xx,yy), b.getPixel(xx,yy)));
            }
         }
      }
      return (1.0 - optionalGetDouble(diffs.stream().mapToDouble(x->x).average()));
   }

   // TODO integrate

   /**
    *
    * @param a
    * @param b
    * @param maskPictogram
    * @return
    */
   public static Double pictogramSimilarityV2(Bitmap a, Bitmap b, Bitmap maskPictogram) {
      List<Double> diffs = new ArrayList<>();
      for (int xx = 0; xx < a.getWidth(); xx ++) {
         for (int yy = 0; yy < a.getHeight(); yy ++) {
            if (maskPictogram == null) {
               diffs.add(pixelDifferencePercentage(a.getPixel(xx,yy), b.getPixel(xx,yy)));
            } else if (maskPictogram.getPixel(xx,yy) != Color.rgb(0,0,0)) {
               diffs.add(pixelDifferencePercentage(a.getPixel(xx,yy), b.getPixel(xx,yy)));
            }
         }
      }
      List<Double> diffsSorted = diffs.stream().sorted().collect(Collectors.toList());
      assert (diffsSorted.size() > 4);

      int quarterSize = (int) Math.ceil(diffsSorted.size()/4);

      List<Double> qValues = new ArrayList<>();
      qValues.add(1.0 - optionalGetDouble( diffsSorted.subList(0, quarterSize).stream().mapToDouble(x->x).average() ));
      qValues.add(1.0 - optionalGetDouble( diffsSorted.subList( quarterSize, (int) Math.ceil(diffs.size()/2)).stream().mapToDouble(x->x).average() ));
      qValues.add(1.0 - optionalGetDouble( diffsSorted.subList((int) Math.ceil(diffs.size()/2), diffs.size() - quarterSize).stream().mapToDouble(x->x).average() ));
      qValues.add(1.0 - optionalGetDouble( diffsSorted.subList(diffs.size() - quarterSize, diffs.size()).stream().mapToDouble(x->x).average() ));
      return (((qValues.get(0)+qValues.get(1)) + optionalGetDouble(qValues.stream().mapToDouble(x->x).average()))/3);

   }

   /**
    * This function handles the 'local differences' method for the 'isWhitespace' function, returning
    * the maximum difference between local pixels of the image
    * @param bitmap
    * @param stride
    * @return
    */
   public static double isWhitespaceLocalDifferencesSubFunction(Bitmap bitmap, int stride) {
      // Index the entire image, and only compare pixels that are local wrt. each other
      // (where local pixels are those within distance of the stride variable)
      List<Double> diffs = new ArrayList<>();
      for (int xx = 0; xx < bitmap.getWidth(); xx += stride) {
         for (int yy = 0; yy < bitmap.getHeight(); yy += stride) {
            for (int xxx = xx-stride; xxx < xx+stride; xxx += stride) {
               for (int yyy = yy-stride; yyy < yy+stride; yyy += stride) {
                  diffs.add(pixelDifferencePercentage(bitmap.getPixel(
                              Math.min(Math.max(0, xxx), bitmap.getWidth()-1),
                              Math.min(Math.max(0, yyy), bitmap.getHeight()-1)),
                        bitmap.getPixel(xx,yy)));
               }
            }
         }
      }
      return optionalGetDouble(diffs.stream().mapToDouble(x->x).max());
   }

   public static int DEFAULT_IS_WHITESPACE_MINIMUM_RADIUS = 2;
   public static int DEFAULT_IS_WHITESPACE_LOCAL_SAMPLING_STRIDE = 3;
   public static double DEFAULT_IS_WHITESPACE_THRESHOLD_AMBIGUOUS = 0.1;
   public static double DEFAULT_IS_WHITESPACE_THRESHOLD_COLOUR_PALETTE = 0.8;
   public static boolean DEFAULT_IS_WHITESPACE_PRESERVE_DIMENSIONS = false;
   public static String DEFAULT_IS_WHITESPACE_METHOD = "prescribedAndMaximum";

   public static int DEFAULT_IS_WHITESPACE_PRESERVE_DIMENSIONS_THUMBNAIL_SIZE = 20;

   /**
    * This function determines whether a supplied image is whitespace or not. It can use one of
    * three differing methods to arrive at a determination of the result:
    *
    *    1. prescribed
    *
    *    2. prescribedAndMaximum
    *
    *    3. localDifferences
    *
    * @param args
    * @return
    */
   public static boolean isWhitespace(Arguments args) {
      Bitmap bitmap = (Bitmap) args.get("bitmap", null);
      int stride = (int) args.get("stride", DEFAULT_IS_WHITESPACE_LOCAL_SAMPLING_STRIDE);

      int whitespacePixel = (int) args.get("whitespacePixel",
                                          getWhitespacePixel(Args(A("bitmap", bitmap))));
      double threshold = (double) args.get("thresholdAmbiguous",
                                                DEFAULT_IS_WHITESPACE_THRESHOLD_AMBIGUOUS);
      double thresholdColourPalette = (double) args.get("thresholdColourPalette",
                                                DEFAULT_IS_WHITESPACE_THRESHOLD_COLOUR_PALETTE);
      boolean preserveDimensions = (boolean) args.get("preserveDimensions",
                                                DEFAULT_IS_WHITESPACE_PRESERVE_DIMENSIONS);
      String method = (String) args.get("method", DEFAULT_IS_WHITESPACE_METHOD);

      // If the image is too small to evaluate, determine that it is whitespace by default
      if ((bitmap.getHeight() <= DEFAULT_IS_WHITESPACE_MINIMUM_RADIUS)
            || (bitmap.getWidth() <= DEFAULT_IS_WHITESPACE_MINIMUM_RADIUS)) return true;

      if (method.startsWith("prescribed")) {
         Arguments appliedArguments = Args(A("bitmap", bitmap));

         // If we are preserving the dimensions, generate a thumbnail that fits the specification
         if (preserveDimensions) {
            appliedArguments =
                  Args(A("bitmap", bitmap),
                        A("w", (int) Math.max(
                              Math.ceil(DEFAULT_IS_WHITESPACE_PRESERVE_DIMENSIONS_THUMBNAIL_SIZE
                                    * ((double) bitmap.getWidth()/bitmap.getHeight())),1)),
                        A("h", DEFAULT_IS_WHITESPACE_PRESERVE_DIMENSIONS_THUMBNAIL_SIZE));
         }
         Bitmap thisThumbnail = thumbnail( appliedArguments );

         switch (method) {
            case "prescribed" :
               // Index the entire image's differences
               List<Double> diffs = new ArrayList<>();
               for (int xx = 0; xx < thisThumbnail.getWidth(); xx ++) {
                  for (int yy = 0; yy < thisThumbnail.getHeight(); yy ++) {
                     diffs.add(pixelDifferencePercentage(thisThumbnail.getPixel(xx,yy), whitespacePixel));
                  }
               }
               return (optionalGetDouble(diffs.stream().mapToDouble(x->x).average()) < threshold);
            case "prescribedAndMaximum" :
               // Index the colour palette of the image, and cross-check the difference against the
               // maximum colour palette difference
               HashMap<String, Integer> thisPalette = colourPaletteFromImage(
                     Args(A("bitmap", thisThumbnail), A("threshold", threshold)));
               List<Integer> frequencies = new ArrayList<>(thisPalette.values());
               Boolean c1 = (pixelDifferencePercentage(
                     Color.parseColor(thisPalette.keySet().stream().filter(
                           k -> Objects.equals(thisPalette.get(k), Collections.max(frequencies))
                        ).collect(Collectors.toList()).get(0)
                     ), whitespacePixel) < threshold);

               Boolean c2 = (Collections.max(frequencies).doubleValue() /
                     (frequencies.stream().mapToInt(Integer::intValue).sum()) > thresholdColourPalette);
               return (c1 && c2);
         }
      } else if (method.equals("localDifferences")) {
         return isWhitespaceLocalDifferencesSubFunction(bitmap, stride) < threshold;
      }
      return false;
   }

   public static double DEFAULT_DIVIDER_WHITESPACE_ALTERNATIONS_THRESHOLD_AMBIGUOUS = 0.1;
   public static double DEFAULT_DIVIDER_WHITESPACE_ALTERNATIONS_THRESHOLD_COLOUR_PALETTE = 0.8;
   public static boolean DEFAULT_DIVIDER_WHITESPACE_ALTERNATIONS_PRESERVE_DIMENSIONS = false;
   public static String DEFAULT_DIVIDER_WHITESPACE_ALTERNATIONS_METHOD = "prescribedAndMaximum";

   /**
    * This function evaluates the whitespace of the supplied dividers
    * @param args
    * @return
    */
   public static List<Boolean> dividerWhitespaceAlternations(Arguments args) {
      List<Bitmap> dividers = args.getListBitmap("visualComponents", new ArrayList<>());
      if (dividers.isEmpty()) return new ArrayList<>();

      int whitespacePixel = (int) args.get("whitespacePixel", getWhitespacePixel(
                                                         Args(A("bitmap", dividers.get(0))))); // here is the error
      double threshold = (double) args.get("thresholdAmbiguous",
                                       DEFAULT_DIVIDER_WHITESPACE_ALTERNATIONS_THRESHOLD_AMBIGUOUS);
      double thresholdColourPalette = (double) args.get("thresholdColourPalette",
                                       DEFAULT_DIVIDER_WHITESPACE_ALTERNATIONS_THRESHOLD_COLOUR_PALETTE);
      boolean preserveDimensions = (boolean) args.get("preserveDimensions",
                                       DEFAULT_DIVIDER_WHITESPACE_ALTERNATIONS_PRESERVE_DIMENSIONS);
      String method = (String) args.get("method",
                                       DEFAULT_DIVIDER_WHITESPACE_ALTERNATIONS_METHOD);

      return dividers.stream()
            .map(x -> isWhitespace(Args(
                  A("bitmap", x),
                  A("whitespacePixel", whitespacePixel),
                  A("thresholdAmbiguous", threshold),
                  A("thresholdColourPalette", thresholdColourPalette),
                  A("preserveDimensions", preserveDimensions),
                  A("method", method)))).collect(Collectors.toList());
   }

   /**
    * This function determines if whitespace alternations (generated by the dividerWhitespaceAlternations
    * function) alternate (to and from whitespace) evenly
    * @param thisDividerWhitespaceAlternations
    * @return
    */
   public static boolean dividerWhitespaceAlternationsWellFormed(
                                                   List<Boolean> thisDividerWhitespaceAlternations) {
      return IntStream.range(0, thisDividerWhitespaceAlternations.size())
                                       .noneMatch(i -> ((i != 0)
                                             && (thisDividerWhitespaceAlternations.get(i)
                                                == thisDividerWhitespaceAlternations.get(i - 1))));
   }


   public static String DEFAULT_VISUAL_COMPONENTS_EQUALLY_SPACED_ORIENTATION = "h";
   public static double DEFAULT_VISUAL_COMPONENTS_EQUALLY_SPACED_THRESHOLD_DISTANCE = 0.1;
   public static boolean DEFAULT_VISUAL_COMPONENTS_EQUALLY_SPACED_ENCASED = true;

   /**
    * This function determines whether the supplied visual components are equally spaced (in terms
    * of distance) by whitespace or not
    * @param args
    * @return
    */
   public static boolean visualComponentsEquallySpaced(Arguments args) {
      List<Bitmap> visualComponents = args.getListBitmap("visualComponents", new ArrayList<>());
      List<Boolean> alternations = args.getListBoolean("alternations", new ArrayList<>());
      String orientation = (String) args.get("orientation",
                                          DEFAULT_VISUAL_COMPONENTS_EQUALLY_SPACED_ORIENTATION);
      double thresholdDistance = (double) args.get("thresholdDistance",
                                          DEFAULT_VISUAL_COMPONENTS_EQUALLY_SPACED_THRESHOLD_DISTANCE);
      boolean encased = (boolean) args.get("encased",
                                          DEFAULT_VISUAL_COMPONENTS_EQUALLY_SPACED_ENCASED);
      // If there are less than a given number of elements, we cannot make a determination
      if (visualComponents.size() < 3) return false;

      List<List<Double>> aggregateDistances = new ArrayList<>();
      IntStream.range(0,2).forEach(x-> aggregateDistances.add(new ArrayList<>()) );
      // For each of the visual components (excluding the last two components)
      for (int i = 0; i < (visualComponents.size()-2); i ++) {
         // Calculate the distance between this component and the one of its likeness thereafter
         // (in the case of alternating elements, this is the 2nd element after)
         double distanceBetweenVisualComponents = (Objects.equals(orientation, "v"))
               ? visualComponents.get(i+1).getHeight() : visualComponents.get(i+1).getWidth();
         for (int j = 0; j < 2; j ++) {
            int x = ((j == 0) ? i : (i + 2));
            distanceBetweenVisualComponents += (((Objects.equals(orientation, "v"))
                  ? visualComponents.get(x).getHeight() : visualComponents.get(x).getWidth()) / 2.0);
         }
         aggregateDistances.get((alternations.get(i) ? 1 : 0)).add(distanceBetweenVisualComponents);
      }

      // In the 'encased' case, we discount the distances on the ends of the visual components
      // (this is useful for cases where the visual components ARE equally spaced, but centered by
      //   larger yet equivalent outer components)
      if (encased) {
         int largerI = ((aggregateDistances.get(1).size() > aggregateDistances.get(0).size()) ? 1 : 0);
         aggregateDistances.set(largerI,
               aggregateDistances.get(largerI).subList(1,aggregateDistances.get(largerI).size()-1));
      }

      // Determine if any of the distances between the visual components exceed the threshold; if so,
      // return that the visual components ARE NOT equally spaced...
      for (int i = 0; i < 2; i ++) {
         List<Double> altDouble = aggregateDistances.get(i);
         Double aggregateDistancesAverage = optionalGetDouble(
               altDouble.stream().mapToDouble(a -> a).average());
         Double aggregateDistancesMax = optionalGetDouble(
               altDouble.stream().mapToDouble(a -> a).max());
         Double aggregateDistancesMin = optionalGetDouble(
               altDouble.stream().mapToDouble(a -> a).min());

         if (!((Math.abs(aggregateDistancesAverage - aggregateDistancesMax)
                                       < (aggregateDistancesMax*thresholdDistance))
               && (Math.abs(aggregateDistancesAverage - aggregateDistancesMin)
                                          < (aggregateDistancesMin*thresholdDistance)))) {
            return false;
         }
      }
      // ...otherwise, return that they ARE equally spaced
      return true;
   }

   public static String DEFAULT_GET_WHITESPACE_PIXEL_METHOD = "simple";

   /**
    * This function retrieves the whitespace pixel from an image
    * @param args
    * @return
    */
   public static int getWhitespacePixel(Arguments args) {
      // Run a check to determine that the bitmap is not null first
      Bitmap bitmap = (Bitmap) args.get("bitmap", null);
      String method = (String) args.get("method", DEFAULT_GET_WHITESPACE_PIXEL_METHOD);
      // The 'simple' method assumes the pixel within the top left corner of the image
      // is whitespace, whereas...
      if (Objects.equals(method, "simple")) return bitmap.getPixel(0,0);
      // ...the 'complex' method evaluates the dominant colour within the image and returns it
      HashMap<String, Integer> thisPalette = colourPaletteFromImage(Args(A("bitmap", bitmap)));
      String hexColorString = thisPalette.keySet().stream().filter(
                  x -> (Objects.equals(thisPalette.get(x), Collections.max(thisPalette.values())))
            ).collect(Collectors.toList()).get(0);
      return Color.parseColor(hexColorString);
   }

   /**
    * This function finds the dominant colour in a bitmap
    * @param bitmap
    * @return The dominant colour in a bitmap
    */
   public static int dominantColourInImage(Bitmap bitmap) {
      HashMap<String, Integer> colourPalette = colourPaletteFromImage(Args(A("bitmap", bitmap),
              A("threshold", 0.01)));
      System.out.println(colourPalette);
      return Color.parseColor(Collections.max(colourPalette.entrySet(), Map.Entry.comparingByValue()).getKey());
   }

   /**
    * This function returns a list of integers, representing the various divider bound offsets for a
    * list of visual components derived from an image
    * @param offsets
    * @param whitespace
    * @param length
    * @return
    */
   @SuppressWarnings("all")
   public static List<Integer> dividerBoundOffsets(
                        List<HashMap<String, Integer>> offsets, List<Boolean> whitespace, int length) {
      // Instantiate the list of integers that will be returned by the function
      List<Integer> listOfOffsets = new ArrayList<>();
      IntStream.range(0, length).forEach(x-> listOfOffsets.add(0));
      // Convert each divider bound object into an offset
      for (int i = 0; i < offsets.size(); i ++) {
         for (int j = offsets.get(i).get("start"); j < offsets.get(i).get("end"); j ++) {
            listOfOffsets.set(j, ((whitespace.get(i)) ? 1 : 0));
         }
      }
      return listOfOffsets;
   }


   public static String DEFAULT_COMBINE_IMAGES_ORIENTATION = "h";

   /**
    * This function combines two images along an axis
    * @param args
    * @return
    */
   public static Bitmap combineImages(Arguments args) {
      Bitmap a = (Bitmap) args.get("a", null);
      Bitmap b = (Bitmap) args.get("b", null);
      String orientation = (String) args.get("orientation", DEFAULT_COMBINE_IMAGES_ORIENTATION);
      // Determine the dimensions of the composite image
      Bitmap comboBitmap;
      int width = (Objects.equals(orientation, "h"))
                     ? (a.getWidth() + b.getWidth()) : a.getWidth();
      int height = (Objects.equals(orientation, "h"))
                     ? a.getHeight() : (a.getHeight() + b.getHeight());
      // Combine the images along the given axis
      comboBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
      Canvas comboImage = new Canvas(comboBitmap);
      comboImage.drawBitmap(a, 0f, 0f, null);
      if (Objects.equals(orientation, "h")) {
         comboImage.drawBitmap(b, a.getWidth(), 0f , null);
      } else {
         comboImage.drawBitmap(b, 0f, a.getHeight(), null);
      }

      return comboBitmap;
   }

   public static String DEFAULT_COMBINE_IMAGES_LIST_ORIENTATION = "h";

   /**
    * This function combines a list of images along an axis
    * @param args
    * @return
    */
   public static Bitmap combineImagesList(Arguments args) {
      List<Bitmap> listOfBitmaps = args.getListBitmap("listOfBitmaps", null);
      String orientation = (String) args.get("orientation", DEFAULT_COMBINE_IMAGES_LIST_ORIENTATION);
      // Iterate through the list of bitmaps, appending each onto the output
      Bitmap output = null;
      for (Bitmap bitmap : listOfBitmaps) {
         if (output == null) {
            output = bitmap.copy(bitmap.getConfig(), false);
         } else {
            output = combineImages(Args(
                  A("a", output.copy(output.getConfig(), false)),
                  A("b", bitmap.copy(bitmap.getConfig(), false)),
                  A("orientation", orientation)
            ));
         }
      }
      return output;
   }
}