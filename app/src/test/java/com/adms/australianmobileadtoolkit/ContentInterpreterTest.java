package com.adms.australianmobileadtoolkit;

import static com.adms.australianmobileadtoolkit.Arguments.A;
import static com.adms.australianmobileadtoolkit.Arguments.Args;
import static com.adms.australianmobileadtoolkit.Common.filePath;
import static com.adms.australianmobileadtoolkit.Common.getFilesInDirectory;
import static com.adms.australianmobileadtoolkit.Logger.DEFAULT_LOG_OUTPUT;
import static com.adms.australianmobileadtoolkit.Logger.LOG_OUTPUT_MACHINE;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Facebook.retrieveReferenceStencilsPictograms;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Facebook.DEFAULT_RETRIEVE_REFERENCE_STENCILS_PICTOGRAMS_INDEX;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.colourQuantizeBitmap;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.getWhitespacePixel;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.imageToPictogram;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.pictogramSimilarity;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.pictogramSimilarityV2;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.stencilToString;

import static java.util.Arrays.asList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.test.platform.app.InstrumentationRegistry;

import com.adms.australianmobileadtoolkit.interpreter.platform.FacebookScreenshot;
import com.adms.australianmobileadtoolkit.interpreter.visual.DividerSet;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(RobolectricTestRunner.class)
public class ContentInterpreterTest {
   private static final String TAG = "Interpreter";
   public final Context testContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

   public static List<String> retrieveScreenshotsForProcessing(String thisSimulationTest) {
      if (thisSimulationTest != null) {
         return getFilesInDirectory(filePath(asList(((new File(".")).getAbsolutePath()),
               "src", "debug", "assets", "local", "contentInterpreterSimulations", thisSimulationTest)));
      } else {
         return new ArrayList<>(); // TODO - update for android
      }
   }

   public static File retrieveLocalScreenshotImage(String thisSimulatedTest, String thisFilename) {
      return (new File(filePath(asList(((new File(".")).getAbsolutePath()),
            "src", "debug", "assets", "local", "contentInterpreterSimulations", thisSimulatedTest, thisFilename)).getAbsolutePath()));
   }

   @Test
   public void testPostEngagementPictogramSimilarity() {
      HashMap<String, Object> pictogramsReference = retrieveReferenceStencilsPictograms(testContext);

      HashMap<String, Integer> size = new HashMap<String, Integer>() {{
         put("w", 16);
         put("h", 16);
      }};


      String thisSimulationTest = "post_engagement_pictogram_similarity";

      Bitmap testPictogramA = colourQuantizeBitmap(Args(A("bitmap", (Bitmap) pictogramsReference.get("facebookReactLike"))));
      Bitmap testPictogramB = colourQuantizeBitmap(Args(A("bitmap",
              imageToPictogram(Args(A("bitmap", (Bitmap) BitmapFactory.decodeFile(
                      retrieveLocalScreenshotImage(thisSimulationTest, "test-image-2.png").getAbsolutePath())),A("size", size),A("crop", true))))));
      Bitmap testPictogramC = colourQuantizeBitmap(Args(A("bitmap",
              imageToPictogram(Args(A("bitmap", (Bitmap) BitmapFactory.decodeFile(
                      retrieveLocalScreenshotImage(thisSimulationTest, "test-image.png").getAbsolutePath())),A("size", size),A("crop", true))))));

      Double thisPictogramSimiliarity = pictogramSimilarityV2(testPictogramA, testPictogramB,
              (Bitmap) pictogramsReference.get("facebookReactMask"));
      System.out.println("thisPictogramSimiliarity: "+ thisPictogramSimiliarity);


      Double thisPictogramSimiliarity2 = pictogramSimilarityV2(testPictogramA, testPictogramC,
              (Bitmap) pictogramsReference.get("facebookReactMask"));
      System.out.println("thisPictogramSimiliarity2: "+ thisPictogramSimiliarity2);


      try (FileOutputStream out = new FileOutputStream("/Users/obei/Desktop/testPictogramA.png")) {
         testPictogramA.compress(Bitmap.CompressFormat.PNG, 100, out); } catch (IOException e) {}
      try (FileOutputStream out = new FileOutputStream("/Users/obei/Desktop/testPictogramB.png")) {
         testPictogramB.compress(Bitmap.CompressFormat.PNG, 100, out); } catch (IOException e) {}

   }

   @Test
   public void testAll() throws JSONException {
      DEFAULT_LOG_OUTPUT = LOG_OUTPUT_MACHINE;

      String thisSimulationTest = "aspect_ratio_warp_1";

      File thisScreenshotFile = retrieveLocalScreenshotImage(thisSimulationTest, "raw-0.png");
      Bitmap thisScreenshotBitmap = (Bitmap) BitmapFactory.decodeFile(thisScreenshotFile.getAbsolutePath());

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

              )










      );


      List<Bitmap> thisImageDividersReduced = thisVisualComponentsReduced.dividerImages;

      FacebookScreenshot result = new FacebookScreenshot(Args(
              A("dividers", thisImageDividersReduced),
              A("whitespacePixel", thisWhitespacePixel),
              A("tolerancePictogramDiff", 0.25),
              A("referenceStencilsPictograms", retrieveReferenceStencilsPictograms(testContext)),
              A("h", thisScreenshotBitmap.getHeight())));

      System.out.println("result.inFacebook: " + result.inFacebook);


      /*
      Bitmap thisBitmap = BitmapFactory.decodeFile(filePath(Arrays.asList(((new File(".")).getAbsolutePath()), "src", "debug",  "assets", "local", "raw-18.png")).getAbsolutePath());
      System.out.println(new DividerSet(
            new Arguments()
                  .put("bitmap", thisBitmap)
                  .put("orientation", "v")
                  .put("scaleMinor", 0.1)
                  .put("absorbMinimums", false)
                  .put("whitespaceThreshold", 0.1)
                  .put("colourPaletteThreshold", 0.05)
                  .put("scanUntil", 1.0)
                  .put("retainMinimums", true)
                  .put("minDividerApproach", "complex")
      ).dividers);
       */




      /*
      Bitmap thisBitmap1 = BitmapFactory.decodeFile(filePath(Arrays.asList(((new File(".")).getAbsolutePath()), "src", "debug",  "assets", "local", "adheader119.png")).getAbsolutePath());


      int thisWhitespacePixelAlt = getWhitespacePixel(thisBitmap1);
      JSONObject result = divideImageIntoVisualComponentsV2(thisBitmap1, "horizontal", 1.0, false, 0.01, 0.2, 1.0, false);
      //JSONObject result = divideImageIntoVisualComponents(
      //      rawDividers.get(h), thisWhitespacePixelAlt, "horizontal",
      //      1.0, 1.0, 0.2, 0.05, false, false, 3, false);
      List<Bitmap> dividerImages = (List<Bitmap>) result.get("dividerImages");
      List<HashMap<String, Integer>> thisImageDividerBounds = (List<HashMap<String, Integer>>) result.get("dividers");
      System.out.println(thisImageDividerBounds);
      List<Boolean> dividerOffsetsWhitespace = (List<Boolean>) dividerImages.stream().map(x -> isImageWhitespace(x, thisWhitespacePixelAlt, 0.1, 0.9,false)).collect(Collectors.toList());

      int thisWidth = thisBitmap1.getWidth();
      List<Integer> dividerOffsetsSignature = offsetSignature(thisImageDividerBounds, dividerOffsetsWhitespace, thisWidth);
      System.out.println(dividerOffsetsSignature);
      //Bitmap thisBitmap2 = BitmapFactory.decodeFile(filePath(Arrays.asList(((new File(".")).getAbsolutePath()), "src", "debug",  "assets", "local", "ad-header-343.png")).getAbsolutePath());
      */

      /*
      List<Bitmap> candidates = Arrays.asList(thisBitmap1, thisBitmap2);
      for (Bitmap candidate : candidates) {
         HashMap<String, JSONObject> pictogramsReference = retrieveReferencePictograms(testContext);
         JSONObject output = adHeaderDividersSubProcess(candidate, 0, pictogramsReference, 0, 0);
         JSONObject debugByDivider = (JSONObject) output.get("debugByDivider");

         List<String> alts = Arrays.asList("thisVisualComponentsDividerImages", "thisVisualComponentsAlt1verticalDividers", "sponsoredTextDividers", "verticalDividersCompellingIndex", "isolatedDividers", "potentialSponsoredText");
         for (String alt : alts) {
            if (debugByDivider.has(alt)) {
               List<Bitmap> xxx = (List<Bitmap>) debugByDivider.get(alt);
               for (Bitmap x : xxx) {
                  ii ++;
                  saveImage(alt+"-"+ii, x);
               }
            }
         }


         if ((Boolean) output.get("found")) {
            System.out.println("FOUND!");
            //saveImage("potentialSponsoredText", (Bitmap) debugByDivider.get("potentialSponsoredText"));
         }
      }*/


      /*
      Bitmap thisBitmap = BitmapFactory.decodeFile(filePath(Arrays.asList(((new File(".")).getAbsolutePath()), "src", "debug",  "assets", "local", "raw-18.png")).getAbsolutePath());
      //thisBitmap = colorQuantizeBitmap(thisBitmap, 64);
      JSONObject x = divideImageIntoVisualComponents2(thisBitmap, getWhitespacePixel(thisBitmap), "vertical",
            1.0, 0.3, 1.0, 0.05, true, true, 3, true);
      List<Bitmap> dividerImages = (List<Bitmap>) x.get("dividerImages");
      for (int i = 0; i < dividerImages.size(); i ++) {
         try (FileOutputStream out = new FileOutputStream("C:\\Users\\akobe\\Desktop\\dividerImages" + i + ".png")) {
            dividerImages.get(i).compress(Bitmap.CompressFormat.PNG, 100, out); } catch (
               IOException e) {}
      }*/

      //Bitmap testBitmap = drawableToBitmap(testContext.getResources().getIdentifier("pixel_5_dark", "drawable",testContext.getPackageName()),testContext);

      // Testing makeWeightedNumericalDictionary
      /*
      List<Integer> Content = Arrays.asList(1,2,3,4,1,2,32,1,1,1,3);
      tneNumbers.addAll(Content);
      System.out.println(tneNumbers);
      System.out.println(makeWeightedNumericalDictionary(tneNumbers));*/

      // Testing makeAveragesDictionary
      /*
      List<Double> Content = Arrays.asList(1,2,1,1,1,2,2,2,2,5,5,5,6,6,6,9,9,9,9,7,7,7).stream().mapToDouble(Double::valueOf).boxed().collect(Collectors.toList());
      System.out.println(makeAveragesDictionary(Content, 1.0));
       */

      // Test colorQuantizeBitmap
      /*
      Bitmap testImage1 = ((BitmapDrawable)testContext.getResources().getDrawable(R.drawable.testimage1)).getBitmap();
      Bitmap testImage2 = colorQuantizeBitmap(testImage1, 64);
      try (FileOutputStream out = new FileOutputStream("C:\\Users\\akobe\\Desktop\\testImage2.png")) {
         testImage2.compress(Bitmap.CompressFormat.PNG, 100, out); } catch (
            IOException e) {}
       */

      /*
      int ColorA = testBitmap.getPixel(20,20);
      int ColorB = testBitmap.getPixel(40,300);
      System.out.println(pixelDifference(ColorA, ColorB));
      System.out.println(pixelDifferencePercentage(ColorA, ColorB));
      */

      //final OCRManager ocrManager = new OCRManager(testContext);
      //String thisResult = ocrManager.TesseractOCRResult(testBitmap);
      //System.out.println(thisResult);

      //int[][] reduced = new int[2][2];
      //for (int i = 0; i < testBitmap.getWidth(); i ++) {
      //   reduced
      //}


      // test divideImageIntoVisualComponents
      /*
      Bitmap testImage1 = ((BitmapDrawable)testContext.getResources().getDrawable(R.drawable.testimage1)).getBitmap();
      JSONObject x = divideImageIntoVisualComponents(testImage1, getWhitespacePixel(testBitmap), "vertical",
            1.0, 1.0, 1.0, 0.1, true, false, 3);
      List<Bitmap> dividerImages = (List<Bitmap>) x.get("dividerImages");
      for (int i = 0; i < dividerImages.size(); i ++) {
         try (FileOutputStream out = new FileOutputStream("C:\\Users\\akobe\\Desktop\\dividerImages" + i + ".png")) {
            dividerImages.get(i).compress(Bitmap.CompressFormat.PNG, 100, out); } catch (
               IOException e) {}
      }*/

      // test imageReplaceColor
      /*
      Bitmap testImage1 = ((BitmapDrawable)testContext.getResources().getDrawable(R.drawable.testimage1)).getBitmap();
      List<Integer> isolatedColors = new ArrayList<>();
      isolatedColors.add(getWhitespacePixel(testImage1));
      int replacementColor = Color.rgb(255,0,0);
      Bitmap testImage2 = imageReplaceColor(testImage1, isolatedColors, replacementColor, 0.05);
      try (FileOutputStream out = new FileOutputStream("C:\\Users\\akobe\\Desktop\\testImage2.png")) {
         testImage2.compress(Bitmap.CompressFormat.PNG, 100, out); } catch (
            IOException e) {}
       */

      // flattenImageToPalette
      /*
      Bitmap testImage1 = ((BitmapDrawable)testContext.getResources().getDrawable(R.drawable.testimage1)).getBitmap();
      List<Integer> retainedColors = flattenImageToPalette(testImage1, 0.1, 64);
      System.out.println(retainedColors);
      System.out.println(retainedColors.size());
      */

      // cropWhitespace
      /*
      Bitmap whitespaceTestImage = ((BitmapDrawable)testContext.getResources().getDrawable(R.drawable.whitespacetest)).getBitmap();
      Bitmap whitespaceTestImageCropped = cropWhitespace(whitespaceTestImage, getWhitespacePixel(whitespaceTestImage), 0.1);
      try (FileOutputStream out = new FileOutputStream("C:\\Users\\akobe\\Desktop\\whitespaceTestImageCropped.png")) {
         whitespaceTestImageCropped.compress(Bitmap.CompressFormat.PNG, 100, out); } catch (
            IOException e) {}
       */

      // imageToPictogram
      /*
      Bitmap facebookDarkHomeActiveBitmap = ((BitmapDrawable)testContext.getResources().getDrawable(R.drawable.facebook_dark_home_active)).getBitmap();
      HashMap<String, Integer> size = new HashMap<>();
      size.put("s", 64);
      HashMap<String, Integer> exclusion = new HashMap<>();
      exclusion.put("x", 32);
      exclusion.put("y", 0);
      exclusion.put("h", 32);
      exclusion.put("w", 32);
      JSONObject thisImagePictogram = imageToPictogram(facebookDarkHomeActiveBitmap, getWhitespacePixel(facebookDarkHomeActiveBitmap),
                                                                           size, exclusion, 0.2, 0.1, false);
      System.out.println(thisImagePictogram.get("nonWhitespacePixels"));
      int[][] thisStencil = (int[][]) thisImagePictogram.get("stencil");
      for (int y = 0; y < 64; y ++) {
         String thisRow = "";
         for (int x = 0; x < thisStencil.length; x ++) {
            thisRow += ((thisStencil[x][y] != -1) ? thisStencil[x][y]: "X");
         }
         System.out.println(thisRow);
      }
       */

      // pictogramSimilarity
      /*
      Bitmap facebookDarkHomeActiveBitmap = ((BitmapDrawable)testContext.getResources().getDrawable(R.drawable.facebook_dark_home_active)).getBitmap();
      JSONObject pictogramFacebookDarkHomeActive = imageToPictogram(facebookDarkHomeActiveBitmap, getWhitespacePixel(facebookDarkHomeActiveBitmap),
            new HashMap<String, Integer>() {{ put("s", 64); }}, new HashMap<String, Integer>() {{ put("x",32); put("y",0); put("h",32); put("w",32); }}, 0.2, 0.1, false);

      Bitmap facebookDarkWatchActiveBitmap = ((BitmapDrawable)testContext.getResources().getDrawable(R.drawable.facebook_dark_watch_active)).getBitmap();
      JSONObject pictogramFacebookDarkWatchActive = imageToPictogram(facebookDarkWatchActiveBitmap, getWhitespacePixel(facebookDarkWatchActiveBitmap),
            new HashMap<String, Integer>() {{ put("s", 64); }}, new HashMap<String, Integer>() {{ put("x",32); put("y",0); put("h",32); put("w",32); }}, 0.2, 0.1, false);


      Bitmap facebookDarkHomeInactiveBitmap = ((BitmapDrawable)testContext.getResources().getDrawable(R.drawable.facebook_dark_home_inactive)).getBitmap();
      JSONObject pictogramFacebookDarkHomeInactive = imageToPictogram(facebookDarkHomeInactiveBitmap, getWhitespacePixel(facebookDarkHomeInactiveBitmap),
            new HashMap<String, Integer>() {{ put("s", 64); }}, new HashMap<String, Integer>() {{ put("x",32); put("y",0); put("h",32); put("w",32); }}, 0.2, 0.1, false);

      System.out.println(pictogramSimilarity(pictogramFacebookDarkHomeActive, pictogramFacebookDarkHomeInactive));
      System.out.println(pictogramSimilarity(pictogramFacebookDarkHomeActive, pictogramFacebookDarkWatchActive));
      System.out.println(pictogramSimilarity(pictogramFacebookDarkHomeActive, pictogramFacebookDarkHomeActive));

       */

      // visual_components_whitespace_alternation
      /*
      Bitmap testImage1 = ((BitmapDrawable)testContext.getResources().getDrawable(R.drawable.testimage1)).getBitmap();
      JSONObject x1 = divideImageIntoVisualComponents(testImage1, getWhitespacePixel(testImage1), "vertical",
            1.0, 1.0, 1.0, 0.1, true, false, 3);
      List<Bitmap> dividerImages = (List<Bitmap>) x1.get("dividerImages");
      for (int i = 0; i < dividerImages.size(); i ++) {
         try (FileOutputStream out = new FileOutputStream("C:\\Users\\akobe\\Desktop\\dividerImages" + i + ".png")) {
            dividerImages.get(i).compress(Bitmap.CompressFormat.PNG, 100, out);
         } catch (
               IOException e) {
         }
      }
      JSONObject output = visualComponentWhitespaceAlternation(dividerImages, getWhitespacePixel(testImage1));

      System.out.println(output.get("alternations"));
      System.out.println(output.get("isWellFormed"));


      Bitmap whitespacetest2 = ((BitmapDrawable)testContext.getResources().getDrawable(R.drawable.whitespacetest2)).getBitmap();
      JSONObject x2 = divideImageIntoVisualComponents(whitespacetest2, getWhitespacePixel(whitespacetest2), "vertical",
            1.0, 1.0, 1.0, 0.1, true, false, 3);
      List<Bitmap> dividerImages2 = (List<Bitmap>) x2.get("dividerImages");
      for (int i = 0; i < dividerImages2.size(); i ++) {
         try (FileOutputStream out = new FileOutputStream("C:\\Users\\akobe\\Desktop\\dividerImages_2_" + i + ".png")) {
            dividerImages2.get(i).compress(Bitmap.CompressFormat.PNG, 100, out);
         } catch (
               IOException e) {
         }
      }
      JSONObject output2 = visualComponentWhitespaceAlternation(dividerImages2, getWhitespacePixel(whitespacetest2));

      System.out.println(output2.get("alternations"));
      System.out.println(output2.get("isWellFormed"));
       */


      // splitAlphaNumeric
      /*
      System.out.println(splitAlphaNumeric("sdadasdadasddwsd dsadad 3243244234 dsfff"));*/


      // visualComponentsEquallySpacedByWhitespace
      /*
      Bitmap whitespacetest2 = ((BitmapDrawable)testContext.getResources().getDrawable(R.drawable.whitespacetest2)).getBitmap();
      JSONObject x2 = divideImageIntoVisualComponents(whitespacetest2, getWhitespacePixel(whitespacetest2), "vertical",
            1.0, 1.0, 1.0, 0.1, true, false, 3);
      List<Bitmap> dividerImages2 = (List<Bitmap>) x2.get("dividerImages");
      for (int i = 0; i < dividerImages2.size(); i ++) {
         try (FileOutputStream out = new FileOutputStream("C:\\Users\\akobe\\Desktop\\dividerImages_2_" + i + ".png")) {
            dividerImages2.get(i).compress(Bitmap.CompressFormat.PNG, 100, out);
         } catch (
               IOException e) {
         }
      }
      JSONObject output2 = visualComponentWhitespaceAlternation(dividerImages2, getWhitespacePixel(whitespacetest2));
      System.out.println(output2.get("alternations"));
      System.out.println(output2.get("isWellFormed"));
      System.out.println(visualComponentsEquallySpacedByWhitespace(dividerImages2, (List<Boolean>) output2.get("alternations"), "vertical", 0.1, false));


      Bitmap whitespacetest3 = ((BitmapDrawable)testContext.getResources().getDrawable(R.drawable.whitespacetest3)).getBitmap();
      JSONObject x3 = divideImageIntoVisualComponents(whitespacetest3, getWhitespacePixel(whitespacetest3), "vertical",
            1.0, 1.0, 1.0, 0.1, true, false, 3);
      List<Bitmap> dividerImages3 = (List<Bitmap>) x3.get("dividerImages");
      for (int i = 0; i < dividerImages3.size(); i ++) {
         try (FileOutputStream out = new FileOutputStream("C:\\Users\\akobe\\Desktop\\dividerImages_3_" + i + ".png")) {
            dividerImages3.get(i).compress(Bitmap.CompressFormat.PNG, 100, out);
         } catch (
               IOException e) {
         }
      }
      JSONObject output3 = visualComponentWhitespaceAlternation(dividerImages3, getWhitespacePixel(whitespacetest3));
      System.out.println(output3.get("alternations"));
      System.out.println(output3.get("isWellFormed"));
      System.out.println(visualComponentsEquallySpacedByWhitespace(dividerImages3, (List<Boolean>) output3.get("alternations"), "vertical", 0.1, true));



      Bitmap whitespacetest4 = ((BitmapDrawable)testContext.getResources().getDrawable(R.drawable.whitespacetest4)).getBitmap();
      JSONObject x4 = divideImageIntoVisualComponents(whitespacetest4, whitespacetest4.getPixel(0,130), "vertical",
            1.0, 1.0, 1.0, 0.1, true, false, 3);
      List<Bitmap> dividerImages4 = (List<Bitmap>) x4.get("dividerImages");
      for (int i = 0; i < dividerImages4.size(); i ++) {
         try (FileOutputStream out = new FileOutputStream("C:\\Users\\akobe\\Desktop\\dividerImages_4_" + i + ".png")) {
            dividerImages4.get(i).compress(Bitmap.CompressFormat.PNG, 100, out);
         } catch (
               IOException e) {
         }
      }
      JSONObject output4 = visualComponentWhitespaceAlternation(dividerImages4, whitespacetest4.getPixel(0,130));
      System.out.println(output4.get("alternations"));
      System.out.println(output4.get("isWellFormed"));
      System.out.println(visualComponentsEquallySpacedByWhitespace(dividerImages4, (List<Boolean>) output4.get("alternations"), "vertical", 0.1, false));
      System.out.println(visualComponentsEquallySpacedByWhitespace(dividerImages4, (List<Boolean>) output4.get("alternations"), "vertical", 0.1, true));
      */


      // isImageWithinFacebook
      /*
      Bitmap testImage1 = ((BitmapDrawable)testContext.getResources().getDrawable(R.drawable.testimage1)).getBitmap();
      JSONObject x1 = divideImageIntoVisualComponents(testImage1, getWhitespacePixel(testImage1), "vertical", 1.0, 0.25, 1.0, 0.01, true, false, 3);
      JSONObject result = isImageWithinFacebook((List<Bitmap>) x1.get("dividerImages"), getWhitespacePixel(testImage1), pictogramsReference, true, 0.15, 0.27);
      System.out.println(result.get("inFacebook"));
      JSONObject statistics = (JSONObject) result.get("statistics");
      System.out.println(statistics);

       */

      // segmentTimestampSeparated - TODO - test with timestamps that differ significantly
      /*
      String thisTest = "hd_dark_sliding_carousel_1";
      File thisTestPath = filePath(Arrays.asList(((new File(".")).getAbsolutePath()),
                                 "src", "debug",  "assets", "local", "contentInterpreterSimulations", thisTest));
      List<String> thisFileNames = getFilesInFolder(thisTestPath);
      List<Bitmap> thisFileBitmaps = thisFileNames.stream().map(x -> BitmapFactory.decodeFile(filePath(
                     Arrays.asList(thisTestPath.getAbsolutePath(), x)).getPath())).collect(Collectors.toList());

      List<List<JSONObject>> segments = segmentTimestampSeparated(thisFileNames, "xxx");

      System.out.println(segments);
       */

      // TODO - test on timestamp separated content
      //processScreenshots("pixel_7_3",0, true);
      /*
      List<String> searchForArray = Arrays.asList(
            "Sponso",
            "ponsor",
            "ponsor",
            "onsore",
            "nsored",
            "Sponsor",
            "ponsore",
            "onsored",
            "Sponsored"
      );
      Bitmap testImage1 = ((BitmapDrawable)testContext.getResources().getDrawable(R.drawable.testimage1)).getBitmap();

      String thisFile = filePath(Arrays.asList(((new File(".")).getAbsolutePath()),
                  "src", "debug", "assets", "local", "contentInterpreterSimulations", "hd_dark_sliding_carousel_1", "1694450152153.90d6d490-a320-4378-9b5e-361eb703234a.jpg")).getAbsolutePath();

      //List<String> listOfScreenshots = retrieveScreenshotsForProcessing("hd_dark_sliding_carousel_1");

      Bitmap thisBitmap = BitmapFactory.decodeFile(thisFile);
      */

      // testImage1 = ((BitmapDrawable)testContext.getResources().getDrawable(R.drawable.testimage1)).getBitmap();


      //try (FileOutputStream out = new FileOutputStream("C:\\Users\\akobe\\Desktop\\xxx.png")) {
      //   combineBitmapsList(Arrays.asList(testImage1,testImage1, testImage1), "horizontal").compress(Bitmap.CompressFormat.PNG, 100, out); } catch (IOException e) {}
      /*

      File videoFilePath = filePath(Arrays.asList(((new File(".")).getAbsolutePath()),
            "src", "debug", "assets", "local", "contentInterpreterSimulations", "video_1", "1709719702125.ff8676e0-feee-4230-9bae-604c7ed1d7b9.portrait.mp4"));


         // The base number of frame intervals to skip
         int numberOfFrameIntervals = Settings.RECORDER_FRAME_INTERVALS;
         int maximumLastFrameWasAffirmative = Settings.RECORDER_FRAME_POSITIVE_COOLDOWN;
         double frameSimilarityThreshold = Settings.RECORDER_FRAME_SIMILARITY_THRESHOLD;
         // This event is timed for optimisation purposes
         long startTime = System.nanoTime();
         System.out.println( "Beginning eventDetection of videoFilePath: " + videoFilePath);
         // Provided that the video is rotated to 'portrait' mode
         if(videoFilePath.getAbsolutePath().contains("portrait")) {
            // Load up the VideoManager instance for the video, and retrieve a frame
            VideoManager vManager = new VideoManager(videoFilePath.getAbsolutePath());
            int thisExactFrameRate = getExactFrameRate(videoFilePath.getAbsolutePath());
            System.out.println( "thisExactFrameRate: " + thisExactFrameRate);
            Frame currentFrame = vManager.getNextFrame();
            // Cursor the frame with the ii variable, and 'cool-down' on frames that contain the
            // 'Sponsored' text by the lastFrameWasAffirmative variable
            int ii = 0;
            Bitmap lastBitmapFromFrame = null;
            // Begin looping through the frames
            while (currentFrame != null) {
               // Step through the frame, and synchronize the cursor
               ii += 1;
               currentFrame = vManager.getNextFrame();
               // At every so many frames, implement an analysis
               if (ii % numberOfFrameIntervals == 0) {
                  // Convert the frame into a bitmap
                  Bitmap thisBitmapFromFrame = (new AndroidFrameConverter()).convert(currentFrame);
                  boolean framesAreIdentical = false;
                  if (lastBitmapFromFrame != null) {
                     try {
                        // Determine the similarity of this frame to the last frame
                        double similarity = getNaiveImageSimilarity(
                              lastBitmapFromFrame,thisBitmapFromFrame);
                        System.out.println( "\t* Similarity of frames " + ii
                              + " & " + (ii + 1) + " : " + similarity);
                        framesAreIdentical = (similarity > frameSimilarityThreshold);
                     } catch (Exception e) {
                        framesAreIdentical = true;
                     }
                  }
                  System.out.println( "\t* Processing frame " + ii);
                  if (!framesAreIdentical) {
                     long actualTime = startTime + (Math.round(1000f/videoRecordingFrameRate)*ii);
                     String fname = actualTime+"."+ UUID.randomUUID().toString()+".png";
                     try (FileOutputStream out = new FileOutputStream("C:\\Users\\akobe\\Desktop\\"+fname)) {
                        thisBitmapFromFrame.compress(Bitmap.CompressFormat.PNG, 100, out); } catch (IOException e) {}

                  } else {
                     System.out.println( "\t* Bypassing due to identical frames");
                  }
                  // Set the last frame to the current frame before the next iteration
                  lastBitmapFromFrame = thisBitmapFromFrame;
               }
            }
            System.out.println( "\t* Time taken: " + ((System.nanoTime() - startTime) / 1e+9));
            System.out.println( "Ending eventDetection of videoFilePath: " + videoFilePath);
         }
         // Inform us if the video cannot be deleted
         if ((videoFilePath.exists()) && (!videoFilePath.delete())) {
            Log.e(TAG, "Failed to delete videoFilePath: " + videoFilePath);
         }
       */
   }
}


// make iut collect all data on dividers
