package com.adms.australianmobileadtoolkit;

import static com.adms.australianmobileadtoolkit.Arguments.A;
import static com.adms.australianmobileadtoolkit.Arguments.Args;
import static com.adms.australianmobileadtoolkit.Common.binAsAverages;
import static com.adms.australianmobileadtoolkit.Common.optionalGetDouble;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.colourPalette;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.colourToHex;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.pixelDifferencePercentage;
import static java.util.Arrays.asList;
import static java.util.stream.StreamSupport.intStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;

import androidx.test.platform.app.InstrumentationRegistry;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.paukov.combinatorics3.Generator;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import kotlin.Triple;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {33})
public class updateTest {

    private static final File simulationsFolder = (new File(String.valueOf(filePath(asList(((new File(".")).getAbsolutePath()),
            "src", "debug", "assets", "local", "simulations")))));

    private static final File screenRecordingsFolder = (new File(simulationsFolder.getAbsolutePath(), "screenRecordings"));
    private static final File testsFolder = (new File(simulationsFolder.getAbsolutePath(), "tests"));

    @Test
    public void test() {
        System.out.println("Testing testing 1 2 3...");
    }


    public static int averageColours(Arguments args) {
        List<Integer> colors = (List<Integer>) args.get("colors", 0);
        List<Integer> reds = colors.stream().map(x -> (int) Math.pow(Color.red(x), 2)).collect(Collectors.toList());
        List<Integer> greens = colors.stream().map(x -> (int) Math.pow(Color.green(x), 2)).collect(Collectors.toList());
        List<Integer> blues = colors.stream().map(x -> (int) Math.pow(Color.blue(x), 2)).collect(Collectors.toList());
        int averageRed = (int) Math.round(Math.sqrt(reds.stream().mapToDouble(x -> x).sum() / reds.size()));
        int averageGreen = (int) Math.round(Math.sqrt(greens.stream().mapToDouble(x -> x).sum() / greens.size()));
        int averageBlue = (int) Math.round(Math.sqrt(blues.stream().mapToDouble(x -> x).sum() / blues.size()));
        //System.out.println( averageRed + " : " + averageGreen + " : " + averageBlue);
        return Color.rgb( averageRed, averageGreen, averageBlue);
    }

    /*
     *
     * This function converts a list of strings into a File path object
     *
     * */
    public static File filePath(List<String> path) {
        File output = null;
        for (String s : path) {
            output = (path.indexOf(s) == 0) ? new File(s) : (new File(output, s));
        }
        return output;
    }

    /*
     *
     * This function generates a sample of pixels over an image
     *
     *     Set a ratio bound on the image (from which the whitespace pixel is derived
     *
     * TODO - annotate
     *
     * */
    public static int[] generatePixelSample(Arguments args) {
        Bitmap thisBitmap = (Bitmap) args.get("bitmap", null);

        Integer sampleWidth = (Integer) args.get("sampleWidth", null);
        Integer sampleHeight = (Integer) args.get("sampleHeight", null);
        Integer sampleOffsetX = (Integer) args.get("sampleOffsetX", null);
        Integer sampleOffsetY = (Integer) args.get("sampleOffsetY", null);

        // Deter
        Integer strides = (Integer) args.get("strides", null);
        int strideX = Math.toIntExact(Math.round(sampleWidth / (double) strides));
        int strideY = Math.toIntExact(Math.round(sampleHeight / (double) strides));
        // Retrieve a sample of pixels from the image
        int ii = 0;
        //System.out.println(strides*strides);
        //System.out.println((sampleOffsetX + sampleWidth));
        //System.out.println(strideX);
        int[] pixels = new int[strides*strides];
        for (int xx = sampleOffsetX; xx < (sampleOffsetX + sampleWidth); xx += strideX) {
            for (int yy = sampleOffsetY; yy < (sampleOffsetY + sampleHeight); yy += strideY) {
                if (ii < (strides*strides)) {
                    pixels[ii] = thisBitmap.getPixel(xx, yy);
                }
                ii++;
            }
        }

        return pixels;
    }


    public static int dominantColourInPalette(HashMap<String, Integer> thisColourPalette) {
        return Color.parseColor(Collections.max(thisColourPalette.entrySet(), Map.Entry.comparingByValue()).getKey());
    }

    /*
     *
     * This function determines what the dominant whitespace pixel is of an image, provided that the
     * image is within Facebook - note that we don't know if the image is of Facebook beforehand, however
     * we go about analysing it anyway
     *
     * */
    public static int whitespacePixelFromImage(Arguments args) {
        Bitmap bitmap = (Bitmap) args.get("bitmap", null);
        // Set a ratio bound on the image (from which the whitespace pixel is derived
        Integer strides = 10; // TODO - confident with 10 - will work with 3 experimentally
        Integer sampleWidth = Math.max(strides, Math.toIntExact(Math.round(bitmap.getWidth() * 0.5)));
        Integer sampleHeight = Math.max(strides, Math.toIntExact(Math.round(bitmap.getHeight() * 0.1)));
        Integer sampleOffsetX = Math.toIntExact(Math.round(bitmap.getWidth() * 0.25));
        Integer sampleOffsetY = Math.toIntExact(Math.round(0.0));
        Double linkingThreshold = 0.3;

        int[] pixels = generatePixelSample(Args(
                A("bitmap", bitmap),
                A("sampleWidth", sampleWidth),
                A("sampleHeight", sampleHeight),
                A("sampleOffsetX", sampleOffsetX),
                A("sampleOffsetY", sampleOffsetY),
                A("strides", strides)
        ));
        return dominantColourInPalette(colourPalette(Args(A("sample", pixels), A("threshold", linkingThreshold))));
    }

    public static Double getStandardDeviation(List<Integer> thisArray) {
        return Math.sqrt(thisArray.stream()
                .map(x -> Math.pow(x - optionalGetDouble(thisArray.stream().mapToDouble(y -> y).average()), 2))
                .mapToDouble(x -> x).sum() / thisArray.size() );
    }

    private static void saveBitmap(Bitmap bmp, String fname) {
        try (FileOutputStream out = new FileOutputStream(fname)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    private static Bitmap overlayBitmaps(Bitmap bmp1, Bitmap bmp2, int x, int y) {
        Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp1, 0,0, null);
        canvas.drawBitmap(bmp2, x,y, null);
        return bmOverlay;
    }


    /*
     *
     * To avoid complicating the function (and of great convenience), offset chains are consecutive
     *
     * */
    public static Integer cumulativeOffsetForwards(HashMap<Integer, HashMap<Integer, Integer>> thisOffsetChain, Integer sourceFrameIndex, Integer destinationFrameIndex) {
        Integer currentFrame = sourceFrameIndex;
        Integer appliedOffset = 0;
        while (!Objects.equals(currentFrame, destinationFrameIndex)) {
            // Get the next frame
            Integer nextFrame = thisOffsetChain.get(currentFrame).keySet().stream().collect(Collectors.toList()).get(0);
            appliedOffset += thisOffsetChain.get(currentFrame).get(nextFrame);
            currentFrame = nextFrame;
        }
        return appliedOffset;
    }


    // needs to be done in chunks (no more than 20 images per chunk)
    public static List<List<File>> chunksToAnalyze(File[] rawFileList) {
        Integer maximumNumberOfImagesPerChunk = 20; // TODO - we should check this across different devices

        List<List<File>> outputChunks = new ArrayList<>();
        List<File> thisOutputChunk = new ArrayList<>();
        for (File thisFile : rawFileList) {
            // Add the file
            thisOutputChunk.add(thisFile);
            // If the output chunk exceeds the maximum number of files that can be assigned to it,
            // generate a new chunk
            if (thisOutputChunk.size() >= maximumNumberOfImagesPerChunk) {
                outputChunks.add(thisOutputChunk);
                thisOutputChunk = new ArrayList<>();
            }
        }
        if (!thisOutputChunk.isEmpty()) {
            outputChunks.add(thisOutputChunk);
        }
        return outputChunks;
    }
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

        JSONObject statistics = new JSONObject();
        try {
            statistics.put("verticalStrideUnit", verticalStrideUnit);
            statistics.put("ArrayThisAverageColourDifferenceToWhitespacePixelPercentage", ArrayThisAverageColourDifferenceToWhitespacePixelPercentage);
            statistics.put("ArrayThisAverageColourRDifferenceToWhitespacePixelRPercentage", ArrayThisAverageColourRDifferenceToWhitespacePixelRPercentage);
            statistics.put("ArrayThisAverageColourGDifferenceToWhitespacePixelGPercentage", ArrayThisAverageColourGDifferenceToWhitespacePixelGPercentage);
            statistics.put("ArrayThisAverageColourBDifferenceToWhitespacePixelBPercentage", ArrayThisAverageColourBDifferenceToWhitespacePixelBPercentage);
            statistics.put("ArrayThisDominantColourDifferenceToWhitespacePixelPercentage", ArrayThisDominantColourDifferenceToWhitespacePixelPercentage);
            statistics.put("ArraySpreadOfDominantColourPercentage", ArraySpreadOfDominantColourPercentage);
            statistics.put("ArrayFrequencyOfDominantColourPercentage", ArrayFrequencyOfDominantColourPercentage);
            statistics.put("ArrayMaxPixelAdjacency", ArrayMaxPixelAdjacency);
            statistics.put("ArrayPixels", ArrayPixels);
        } catch (Exception e) {

        }


        return statistics;
    }


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





    public static JSONObject chunkDataStructure(List<File> chunkFiles) {

        // TODO - determining that we are in facebook has to come before all else

        HashMap<Integer, List<Integer>> screenshotDividers = new HashMap<>();
        HashMap<Integer, HashMap<Integer, String>> screenshotMediaBoundaries = new HashMap<>();
        HashMap<Integer, JSONObject> statisticsReference = new HashMap<>();
        Integer consistentWidth = null;
        Integer consistentHeight =  null;
        HashMap<Integer, Bitmap> bitmapsMap = new HashMap<>();

        int ii = 1;
        for (String fname : chunkFiles.stream().map(File::getAbsolutePath).sorted().collect(Collectors.toList())) {
            if (fname.endsWith(".png") || fname.endsWith(".jpg")) {
                Bitmap thisBitmap = BitmapFactory.decodeFile(fname);
                bitmapsMap.put(ii, thisBitmap);
                JSONObject statistics = generateScreenshotStatistics(thisBitmap, false);
                /*if (ii == 7) {
                    System.out.println(fname);
                    generateScreenshotStatistics(thisBitmap, true);
                }*/
                List<Integer> dividersFoundInScreenshot = findDividersInScreenshot(thisBitmap, statistics, false);
                HashMap<Integer, String> mediaBoundaries = findMediaBoundaries(thisBitmap, statistics);

                screenshotDividers.put(ii, dividersFoundInScreenshot);
                screenshotMediaBoundaries.put(ii, mediaBoundaries);
                statisticsReference.put(ii, statistics);
                System.out.println(ii);
                System.out.println(screenshotDividers);

                if (consistentWidth == null) {
                    consistentWidth = thisBitmap.getWidth();
                    consistentHeight = thisBitmap.getHeight();
                }
                ii ++;
            }
        }

        JSONObject dataStructure = new JSONObject();
        try {
            dataStructure.put("screenshotDividers", screenshotDividers);
            dataStructure.put("screenshotMediaBoundaries", screenshotMediaBoundaries);
            dataStructure.put("statisticsReference", statisticsReference);
            dataStructure.put("consistentWidth", consistentWidth);
            dataStructure.put("consistentHeight", consistentHeight);
            dataStructure.put("bitmapsMap", bitmapsMap);
            dataStructure.put("verticalStrideUnit",
                    (Integer) Objects.requireNonNull(statisticsReference.get(0)).get("verticalStrideUnit"));
        } catch (Exception e) {
            // TODO - unexpected
        }

        return dataStructure;
    }


    public static HashMap<Integer, HashMap<Integer, Integer>> matchFramesOnOffsets(JSONObject thisChunkDataStructure) {
        HashMap<Integer, List<Integer>> screenshotDividers = new HashMap<>();
        HashMap<Integer, HashMap<Integer, String>> screenshotMediaBoundaries = new HashMap<>();
        HashMap<Integer, JSONObject> statisticsReference = new HashMap<>();
        Integer consistentWidth = null;
        Integer consistentHeight =  null;
        Integer verticalStrideUnit = null;
        HashMap<Integer, Bitmap> bitmapsMap = new HashMap<>();
        try {
            screenshotDividers = (HashMap<Integer, List<Integer>>) thisChunkDataStructure.get("screenshotDividers");
            screenshotMediaBoundaries = (HashMap<Integer, HashMap<Integer, String>>) thisChunkDataStructure.get("screenshotMediaBoundaries");
            statisticsReference = (HashMap<Integer, JSONObject>) thisChunkDataStructure.get("statisticsReference");
            bitmapsMap = (HashMap<Integer, Bitmap>) thisChunkDataStructure.get("bitmapsMap");
            consistentWidth = (Integer) thisChunkDataStructure.get("consistentWidth");
            consistentHeight = (Integer) thisChunkDataStructure.get("consistentHeight");
            verticalStrideUnit = (Integer) (statisticsReference.get(1)).get("verticalStrideUnit");
        } catch (Exception e) {}


        // STEP2 : Match the frames on offsets
        // TODO - repair the dividers and media boundaries

        HashMap<Integer, HashMap<Integer, Integer>> offsetsChainMap = new HashMap(); // link a -> link b -> offset
        // for any key A, the offset communicated for its linking keys B would be how A would have to offset (either up (-) or down (+)) in order to align with B
        int allowableOffset = (int) Math.round(consistentHeight*0.02); // ADJUSTINGFORRATIO
        int jump = 1;
        // for each two frames
        for (int i = 1; i <= (bitmapsMap.size()-1); i ++) {
            //i = 20;
            int j = i + jump;
            List<Integer> iScreenshotDividers = screenshotDividers.get(i);
            HashMap<Integer, String> iMediaBoundaries = screenshotMediaBoundaries.get(i);
            List<Integer> jScreenshotDividers =  (screenshotDividers.get(j) == null) ? new ArrayList<>() : screenshotDividers.get(j);
            HashMap<Integer, String> jMediaBoundaries = (screenshotMediaBoundaries.get(j) == null) ? new HashMap<>() : screenshotMediaBoundaries.get(j);

            //System.out.println(i);
            //System.out.println(iMediaBoundaries);
            //System.out.println(jMediaBoundaries);
            // slide them across each other - in order to establish a match, at least three components need to match
            boolean matched = false;
            double reasonableOffsetScreenPCT = 1;
            int reasonableOffset = ((int)Math.round(consistentHeight*reasonableOffsetScreenPCT));

            // TODO - optimise a way to avoid scanning the entire image
            HashMap<Integer, Double> potentialOffsetsToSampleSizes = new HashMap<>();
            for (int offset = 0-reasonableOffset; offset < reasonableOffset; offset += verticalStrideUnit) {
                int finalOffset = offset;
                List<Integer> iScreenshotDividersOffseted = iScreenshotDividers.stream().map(x -> x + finalOffset).collect(Collectors.toList());
                HashMap<Integer, Integer> iMediaBoundaryOffseted = new HashMap<>();
                for (Integer iMediaBoundaryIndex : iMediaBoundaries.keySet()) {
                    iMediaBoundaryOffseted.put(iMediaBoundaryIndex + offset, iMediaBoundaryIndex);
                }


                // determine media boundary matches
                List<Integer> matchingMediaBoundariesJIndices = new ArrayList<>();
                for (Integer iMediaBoundaryIndex : iMediaBoundaryOffseted.keySet()) {
                    List<Integer> tentativeJAlignedMediaBoundaries = jMediaBoundaries.keySet().stream().filter(x -> Math.abs(x - iMediaBoundaryIndex) < allowableOffset).collect(Collectors.toList());
                    // If the normal (whitespace specification) is the same between them
                    matchingMediaBoundariesJIndices = Stream.concat(tentativeJAlignedMediaBoundaries.stream()
                                    .filter(x -> jMediaBoundaries.get(x) == iMediaBoundaries.get(iMediaBoundaryOffseted.get(iMediaBoundaryIndex))).collect(Collectors.toList()).stream(),
                            matchingMediaBoundariesJIndices.stream() ).collect(Collectors.toList());
                }
            /*if (i == 6 && j == 7 && (offset == -298)) {
               System.out.println("iMediaBoundaryOffseted : "+iMediaBoundaryOffseted);
               System.out.println("jMediaBoundaries : "+jMediaBoundaries);
            }*/
                //System.out.println(offset);
                // These are the indices of the media boundaries of J that match I at this offset
                //System.out.println("\t\t" + matchingMediaBoundariesJIndices);

                List<Integer> matchingDividersJIndices = new ArrayList<>();
                for (Integer iScreenshotDividerOffseted : iScreenshotDividersOffseted) {
                    List<Integer> tentativeJAlignedDividers = jScreenshotDividers.stream().filter(x -> Math.abs(x - iScreenshotDividerOffseted) < allowableOffset).collect(Collectors.toList());

                    matchingDividersJIndices = Stream.concat(tentativeJAlignedDividers.stream(), matchingDividersJIndices.stream()).collect(Collectors.toList());
                }

                //System.out.println("\t\t" + matchingDividersJIndices);

                // If there is a match from either the dividers or the media boundaries
                if ((matchingMediaBoundariesJIndices.size() > 0) || (matchingDividersJIndices.size() > 0)) {
                    // use the media boundaries in each to isolate the whitespace areas
                    //statisticsReference.get(i)

                    HashMap<Integer, Double> avgColoursI = generateProjection(consistentHeight, iMediaBoundaries, statisticsReference.get(i));
                    HashMap<Integer, Double> avgColoursJ = generateProjection(consistentHeight, jMediaBoundaries, statisticsReference.get(j));

                    // TODO - investigate projections accuracy
                    // The projection is multiplied by the average colours for the frame


                    // System.out.println("avgColoursI size:" + avgColoursI.size());

               /*List<Double> signatureDifference = new ArrayList<>();
               for (Integer indexI : avgColoursI.keySet()) {
                  if (avgColoursI.get(indexI) != -1.0) {
                     for (Integer indexJ : avgColoursJ.keySet()) {
                        if (avgColoursJ.get(indexJ) != -1.0) {
                           // If the offset applied to I matches J (roughly)
                           int roughDifference = (int) Math.round(consistentHeight*0.005); // ADJUSTINGFORRATIO
                           if (Math.abs((indexI + offset) - indexJ) < roughDifference) {
                              signatureDifference.add(Math.abs(avgColoursI.get(indexI) - avgColoursJ.get(indexJ)));
                           }
                        }
                     }
                  }
               }*/

                    List<Double> signatureDifference = new ArrayList<>();
                    for (Integer indexI : avgColoursI.keySet()) {
                        Integer indexOffseted = (indexI + offset);

                        Double valueOfJ = null;
                        Double valueOfI = avgColoursI.get(indexI);
                        // If the value within the y axis of I is out of bounds of J
                        if ((indexOffseted < 0) || (indexOffseted >= consistentHeight)) {
                            // Register a non-comparable value

                        } else {
                    /* if (i == 4 && j == 5 && offset == -280) {
                        System.out.println("indexOffseted : " + indexI + " : " + indexOffseted );
                        //System.out.println("indexOffseted : " + avgColoursI.keySet().stream().sorted().collect(Collectors.toList()).indexOf(indexI) +  " : " + avgColoursJ.keySet().stream().sorted().collect(Collectors.toList()).indexOf(indexOffseted));
                     }
                     //Integer indexOffsetedAdjusted = (int) (Math.round(indexOffseted / verticalStrideUnit) * verticalStrideUnit);*/
                            if (!avgColoursJ.containsKey(indexOffseted)) {
                                System.out.println("NO KEEEYYYYY");
                            }
                            valueOfJ = avgColoursJ.get(indexOffseted);
                        }

                        Double resultingValue = null;
                        // If both are image content - we can relinquish the reading
                        if (valueOfJ == null) {

                        } else
                        if ((Objects.equals(valueOfI, valueOfJ)) && (valueOfI == -1.0)) {
                            // Already set to null
                        } else
                        if ((!Objects.equals(valueOfI, valueOfJ)) && ((valueOfI == -1.0) || (valueOfJ == -1.0))) {
                            // We have an instance of a mismatch - one is imagery - the other is whitespace
                            resultingValue = 1.0;
                        } else {
                            resultingValue = (Math.abs(valueOfI - valueOfJ));
                        }
                        signatureDifference.add(resultingValue);
                    }
                    List<Double> signatureDifferenceAlt = signatureDifference;
                    signatureDifference = signatureDifference.stream().filter(x -> (x != null)).collect(Collectors.toList());

                    double pctPerfect = ((double) signatureDifference.stream().filter(x -> (x < 0.05)).count() / signatureDifference.size()); // set it back to 0.025 and attempt to jump the frame
                    double pctPerfect2 = ((double) signatureDifference.stream().filter(x -> (x < 0.1)).count() / signatureDifference.size()); // set it back to 0.025 and attempt to jump the frame
                    Double signatureDifferenceAvg = optionalGetDouble(signatureDifference.stream().filter(x -> (x != null)).mapToDouble(x -> x).average());
                    Boolean reasonableSample = (signatureDifference.size() > (avgColoursI.size() * 0.2));

                    /*if (i == 7 && j == 8 && reasonableSample && ((offset == 45))) {
                        System.out.println("offset : " + offset);
                        System.out.println("pctPerfect : " + pctPerfect);
                        System.out.println("pctPerfect2 : " + pctPerfect2);
                        System.out.println("signatureDifference.size() : " + signatureDifference.size());
                        System.out.println("avgColoursI.size() : " + avgColoursI.size());
                        //System.out.println("signatureDifferenceAvg : " + signatureDifferenceAvg);
                        System.out.println("reasonableOffset : " + reasonableOffset);
                        //System.out.println("avgColoursI.max : " + avgColoursI.keySet().stream().mapToDouble(x -> x).max());
                        //Integer indexOffseted = (0+ offset);
                        //Integer indexOffsetedAdjusted = (int) (Math.round(indexOffseted / verticalStrideUnit) * verticalStrideUnit);
                        // System.out.println(indexOffsetedAdjusted);
                        //System.out.println(signatureDifferenceAlt);
                        // System.out.println(signatureDifferenceAlt);
                        try {
                            System.out.println((new ObjectMapper().writer().withDefaultPrettyPrinter()).writeValueAsString(avgColoursI.keySet().stream().sorted().map(x -> new Pair(x, avgColoursI.get(x))).collect(Collectors.toList())));
                            System.out.println((new ObjectMapper().writer().withDefaultPrettyPrinter()).writeValueAsString(avgColoursJ.keySet().stream().sorted().map(x -> new Pair(x, avgColoursJ.get(x))).collect(Collectors.toList())));

                        } catch (Exception e) {}

                    }*/
                    //System.out.println(signatureDifference); // TODO - signature difference must be larger
                    //System.out.println("pctPerfect : " + pctPerfect);
                    //System.out.println((signatureDifference.size() > (avgColoursI.size() * 0.2)));
                    // System.out.println((new ObjectMapper().writer().withDefaultPrettyPrinter()).writeValueAsString(avgColoursI.keySet().stream().sorted().map(x -> avgColoursI.get(x)).collect(Collectors.toList())));
                    // System.out.println((new ObjectMapper().writer().withDefaultPrettyPrinter()).writeValueAsString(avgColoursI.keySet().stream().sorted().map(x -> avgColoursJ.get(x)).collect(Collectors.toList())));




                    //Double signatureDifferenceAvg = optionalGetDouble(signatureDifference.stream().filter(x -> (x != null)).mapToDouble(x -> x).average());
                    //System.out.println("\t\tpctPerfect: " + pctPerfect);
                    //System.out.println("\t\tsignatureDifference: " + signatureDifferenceAvg);
                    if ((pctPerfect2 > 0.55) && reasonableSample) {// && (signatureDifferenceAvg < 0.05)) {// adjusted from 0.6

                        potentialOffsetsToSampleSizes.put(offset, pctPerfect);//signatureDifference.size());

                        matched = true;
                        //System.out.println("Screenshots "+i+" & "+(j)+" MATCHED ON OFFSET: "+(offset));
                        //System.out.println("\t\tpctPerfect: "+pctPerfect);
                        //System.out.println("Screenshots "+i+" & "+(j)+" MATCHED ON OFFSET: "+(offset));
                        //System.out.println("\t\tpctPerfect: "+pctPerfect);
                        //System.out.println("\t\tsignatureDifferenceAvg: "+signatureDifferenceAvg);
                        //System.out.println("\t\tsignatureDifferenceSampleSize: "+signatureDifference.size());



                        //System.out.println((new ObjectMapper().writer().withDefaultPrettyPrinter()).writeValueAsString(signatureDifference));
                        // System.out.println((new ObjectMapper().writer().withDefaultPrettyPrinter()).writeValueAsString(avgColoursI));
                        //System.out.println((new ObjectMapper().writer().withDefaultPrettyPrinter()).writeValueAsString(avgColoursJ));

                  /*
                  if (!matched) {

                     // insert forwards
                     if (!offsetsChainMap.containsKey(i)) {
                        offsetsChainMap.put(i, new HashMap<>());
                     }
                     HashMap<Integer, Integer> offsetsChainMapLinkB = offsetsChainMap.get(i);
                     offsetsChainMapLinkB.put(j, offset);
                     offsetsChainMap.put(i, offsetsChainMapLinkB);

                     // insert backwards
                     if (!offsetsChainMap.containsKey(j)) {
                        offsetsChainMap.put(j, new HashMap<>());
                     }
                     HashMap<Integer, Integer> offsetsChainMapLinkA = offsetsChainMap.get(j);
                     offsetsChainMapLinkA.put(i, offset*-1);
                     offsetsChainMap.put(j, offsetsChainMapLinkA);
                     //System.out.println("avgColoursI:");
                     //System.out.println((new ObjectMapper().writer().withDefaultPrettyPrinter()).writeValueAsString(avgColoursI));
                     //System.out.println("avgColoursJ:");
                     //System.out.println((new ObjectMapper().writer().withDefaultPrettyPrinter()).writeValueAsString(avgColoursJ));
                  }*/
                    }
                    //jMediaBoundaries
                    // cross-check the whitespace areas between both frames, applying the offset to the first so that they line up
                    // bearing in mind that the top of the page may change, we only require that at least half of all whitespace sampled matches (as technically the top bar, which influences analysis)
                    // can take up as much as half of all whitespace shown on a page
                }
                /* if the frames are not matched, then attempt to jump a frame */
            }

            if (matched) {

                // get potentialOffsetsToSampleSizes key with largest value
                Integer determinedOffset = Collections.max(potentialOffsetsToSampleSizes.entrySet(), Map.Entry.comparingByValue()).getKey();
                if (!offsetsChainMap.containsKey(i)) {
                    offsetsChainMap.put(i, new HashMap<>());
                }
                HashMap<Integer, Integer> offsetsChainMapLinkB = offsetsChainMap.get(i);
                offsetsChainMapLinkB.put(j, determinedOffset);
                offsetsChainMap.put(i, offsetsChainMapLinkB);

                // insert backwards
                if (!offsetsChainMap.containsKey(j)) {
                    offsetsChainMap.put(j, new HashMap<>());
                }
                HashMap<Integer, Integer> offsetsChainMapLinkA = offsetsChainMap.get(j);
                offsetsChainMapLinkA.put(i, determinedOffset*-1);
                offsetsChainMap.put(j, offsetsChainMapLinkA);

                System.out.println("Screenshots "+i+" & "+(j)+" MATCHED ON OFFSET: "+(determinedOffset*-1));
                //i += (jump-1);

                // Set this
                jump = 1;
            }

            if (!matched) {
                if ((jump < 3) && (((i-1)+(jump+1)) <= bitmapsMap.size())) {
                    // stall the iterator
                    i --;
                    // increase the jump
                    jump += 1;
                } else {
                    jump = 1;
                }
            }
        }



        //System.out.println("screenshotMediaBoundaries");
        //System.out.println(screenshotMediaBoundaries);

        //System.out.println("screenshotDividers");
        //System.out.println((new ObjectMapper().writer().withDefaultPrettyPrinter()).writeValueAsString(screenshotDividers));

        return offsetsChainMap;
    }

    public static List<HashMap<Integer, HashMap<Integer, Integer>>> expandOffsetChainsMap(JSONObject dataStructure) {

        HashMap<Integer, HashMap<Integer, Integer>> offsetsChainMap = null;
        HashMap<Integer, Bitmap> bitmapsMap = null;
        Integer nFrames = null;
        try {
            offsetsChainMap = (HashMap<Integer, HashMap<Integer, Integer>>) dataStructure.get("offsetsChainMap");
            bitmapsMap = (HashMap<Integer, Bitmap>) dataStructure.get("bitmapsMap");
            nFrames = bitmapsMap.size();
        } catch (Exception e) {}

        // STEP3 : Generate an offset chain map between the various frames

        // with the matches, we can use commonly joined up frames to repair other frames
        // turn the links into a dict, where each key is link b, and all values of link are all link a's for link b
        // possibly do vice versa on each link a
        // attempt to restore the chain of offsets
        // if an offset cannot be restored to link together two frames, then fine, dont attempt to link it
        // in any case, now we will have a stable chain where applicable
        // NOTE: Hypothetically, we could repeatedly inject frame offsets into each other, informing the entire dictionary eventually
        // to allow any frame to reach any other frame
        // however we dont do this because we only jump a maximum of a few frames either forwards or backwards before repairs take place
        // such jumps are expected to be entirely captured by the frame injections up until this point
        // anything more than this would probably be overkill, as the whole point of this process is to simply ensure that no overlooked frames get left behind
        // in the next step, we will attempt to chain the frames - the chaining can technically work with/without repeated injections, as the chaining will choose the closest (smallest interval) from any
        // given frame to draw a link in the chain. so then, unless a repeated injection is necessary to repair two frames that are 5 intervals apart, we can
        // just link them together using whats available, or just leave them separate
        for (Integer linkA : offsetsChainMap.keySet()) {
            System.out.println(linkA);
            HashMap<Integer, HashMap<Integer, Integer>> finalOffsetsChainMap = offsetsChainMap;
            Generator.combination(new ArrayList<>(Objects.requireNonNull(offsetsChainMap.get(linkA)).keySet()))
                    .simple(2)
                    .stream()
                    .forEach(x -> {
                        HashMap<Integer, Integer> offsetsChainMapLinkA = finalOffsetsChainMap.get(x.get(0));
                        offsetsChainMapLinkA.put(x.get(1), 0 - (finalOffsetsChainMap.get(linkA).get(x.get(0)) - finalOffsetsChainMap.get(linkA).get(x.get(1))));
                        finalOffsetsChainMap.put(x.get(0), offsetsChainMapLinkA);
                    });
        }


        // offsetsChainMap.remove(3); // uncomment to demonstrate multiple chain handling

        // In this step, we attempt to put together the chain(s) - each index in the offsetsChainMap is polled for the smallest interval that succeeds
        // it. if no interval exists after it, the chain is dispatched, and another is started at the next index

        System.out.println(offsetsChainMap);


        int frameJumper = 1;
        List<HashMap<Integer, HashMap<Integer, Integer>>> offsetChains = new ArrayList<>();
        List<HashMap<Integer, HashMap<String, HashMap<Double, Integer>>>> retainedFeaturesGlobalList = new ArrayList<>();
        HashMap<Integer, HashMap<Integer, Integer>> thisOffsetChain = new HashMap<>();
        while (frameJumper <= nFrames) {
            int finalFrameJumper = frameJumper;
            // Only jump forwards (from the current frame) for iteration

            // We can only jump forward from this frame if it is actually recorded in the offsetsChainMap
            List<Integer> framesFromThisFrame = new ArrayList<>();
            if (offsetsChainMap.containsKey(frameJumper)) {
                framesFromThisFrame = new ArrayList<>(Objects.requireNonNull(offsetsChainMap.get(frameJumper)).keySet()).stream().filter(x -> x > finalFrameJumper).collect(Collectors.toList());
            }
            if (framesFromThisFrame.isEmpty()) {
                // If the frame does not jump anywhere, add the current chain to the global list, and start a new one
                HashMap<Integer, HashMap<Integer, Integer>> clonedOffsetChain = (HashMap<Integer, HashMap<Integer, Integer>>) thisOffsetChain.clone();
                offsetChains.add(clonedOffsetChain);
                thisOffsetChain = new HashMap<>();
                // Also jump a frame forward to keep the iterator going
                frameJumper ++;

            } else {
                // Determine that the next is that which is the shortest jump (forward) from the current frame
                Integer nextFrame = Math.toIntExact(Math.round(optionalGetDouble(framesFromThisFrame.stream().mapToDouble(x -> x).min())));
                // Recall the offset
                Integer thisOffset = offsetsChainMap.get(frameJumper).get(nextFrame);
                // Generate a closed hashmap containing only the next frame and its offset
                HashMap<Integer, Integer> closedHashMapForFrame = new HashMap<>();
                closedHashMapForFrame.put(nextFrame, thisOffset);
                // Apply it to the offset chain
                thisOffsetChain.put(frameJumper, closedHashMapForFrame);
                // Set the frame to the next frame
                frameJumper = nextFrame;
            }
        }
        System.out.println("A total of "+offsetChains.size()+" offset chains were found");
        System.out.println(offsetChains);

        return offsetChains;
    }

    public static void printJSON(Object thisToJSON) {
        try {
            System.out.println((new ObjectMapper().writer().withDefaultPrettyPrinter()).writeValueAsString(thisToJSON));
        } catch (Exception e) {}
    }

    public static void deriveBoundaries(JSONObject dataStructure, File thisOutputFolder, Integer thisChunkNumber) {
        Integer consistentWidth = null;
        Integer consistentHeight =  null;
        List<HashMap<Integer, HashMap<Integer, Integer>>> offsetChains = null;
        HashMap<Integer, List<Integer>> screenshotDividers = new HashMap<>();
        HashMap<Integer, Bitmap> bitmapsMap = new HashMap<>();
        HashMap<Integer, HashMap<Integer, String>> screenshotMediaBoundaries = new HashMap<>();
        try {
            consistentWidth = (Integer) dataStructure.get("consistentWidth");
            consistentHeight = (Integer) dataStructure.get("consistentHeight");
            bitmapsMap = (HashMap<Integer, Bitmap>) dataStructure.get("bitmapsMap");
            offsetChains = (List<HashMap<Integer, HashMap<Integer, Integer>>>) dataStructure.get("offsetChains");
            screenshotDividers = (HashMap<Integer, List<Integer>>) dataStructure.get("screenshotDividers");
            screenshotMediaBoundaries = (HashMap<Integer, HashMap<Integer, String>>) dataStructure.get("screenshotMediaBoundaries");
        } catch (Exception e) {}


        /*
        HashMap<Integer, JSONObject> statisticsReference = new HashMap<>();
        Integer verticalStrideUnit = null;
        try {
            statisticsReference = (HashMap<Integer, JSONObject>) thisChunkDataStructure.get("statisticsReference");
            verticalStrideUnit = (Integer) statisticsReference.get(0).get("verticalStrideUnit");
        } catch (Exception e) {}*/

        // then for each chain, all dividers and media boundaries are projection across all frames
        // then where large frequencies of either are observed in any of the updated frames (enriched by the projections),
        // the true dividers/media boundaries are then derived

        // for all offset chains, take a given offset chain X
        // create a canvas chain Y
        // for the entire key-set (all frames in the offset chain X), take a given frame A
        // then (cumulatively) determine the offset from A to all other frames in the offset chain X
        // finally, use the offsets to project the media boundaries and dividers in frame A onto all other frames within the canvas chain Y
        // then consider the frequencies of the media boundaries/dividers within

        //System.out.println("offsetChains:");
        //printJSON(offsetChains);
        // for all offset chains, take a given offset chain X
        for (int i = 0; i < offsetChains.size(); i ++) {
            HashMap<Integer, HashMap<Integer, Integer>> thisOC = offsetChains.get(i);

            if (bitmapsMap.keySet().size() > 0) {


            HashMap<Integer, HashMap<String, List<Integer>>> superOC = new HashMap<>();
            // create a canvas chain Y
            // a given frame -> list of pairs -> pair has a y index and a state (whitespace(above/below), divider)
            // Initialize all frames as having empty lists
            for (Integer thisFrame : bitmapsMap.keySet()) {
                superOC.put(thisFrame, new HashMap<>());
            }
            //thisOC.keySet().forEach(thisFrame -> { superOC.put(thisFrame, new HashMap<>()); });


            HashMap<Integer, HashMap<String, HashMap<Double, Integer>>> retainedFeaturesGlobal = new HashMap<>();

            // for the entire key-set (all frames in the offset chain X), take any given frame A
            /// XXX thisFrame = 1

            // filter to only those frames that are in this OC
            List<Integer> distinctKeys = new ArrayList<>();
            for (Integer aKey : thisOC.keySet()) {
                distinctKeys.add(aKey);
                distinctKeys = Stream.concat(distinctKeys.stream(),  thisOC.get(aKey).keySet().stream()).collect(Collectors.toList());
            }
            distinctKeys = distinctKeys.stream().distinct().collect(Collectors.toList());

            System.out.println(thisOC);

            System.out.println("distinctKeys:");
            printJSON(distinctKeys);

            for (Integer thisFrame : distinctKeys) {
                // attempt to go forwards on the frame
                System.out.println("Running on frame: " + thisFrame);

                int frameJumpForward = thisFrame;
                if (thisFrame < Collections.max(distinctKeys)) {
                    int offsetCumulative = 0;
                    while (frameJumpForward != -1) {
                        // Determine the next frame
                        // XXX frameJumpForwardNext = 2
                        System.out.println("At frame "+thisFrame);
                        int frameJumpForwardNext = thisOC.get(frameJumpForward).keySet().iterator().next();
                        System.out.println("At frame "+thisFrame+" and projecting to "+frameJumpForwardNext);
                        // Determine the cumulative offset to the next frame
                        /// XXX offsetToNextFrame = -612
                        int offsetToNextFrame = thisOC.get(frameJumpForward).get(frameJumpForwardNext);
                        offsetCumulative += offsetToNextFrame;

                        // Project the media boundaries of thisFrame onto the frame's list within the superOC
                        HashMap<String, List<Integer>> pairsToAdd = new HashMap<>();
                        for (Integer indexY : screenshotMediaBoundaries.get(thisFrame).keySet()) {
                            String thisKey = screenshotMediaBoundaries.get(thisFrame).get(indexY);
                            if (!pairsToAdd.containsKey(thisKey)) {
                                pairsToAdd.put(thisKey, new ArrayList<>());
                            }
                            pairsToAdd.put(thisKey, Stream.concat(pairsToAdd.get(thisKey).stream(), Collections.singletonList(offsetCumulative + indexY).stream()).collect(Collectors.toList()));
                        }
                        // Project the dividers
                        for (Integer dividerIndexY : screenshotDividers.get(thisFrame)) {
                            String thisKey = "divider";
                            if (!pairsToAdd.containsKey(thisKey)) {
                                pairsToAdd.put(thisKey, new ArrayList<>());
                            }
                            pairsToAdd.put(thisKey, Stream.concat(pairsToAdd.get(thisKey).stream(), Collections.singletonList(offsetCumulative + dividerIndexY).stream()).collect(Collectors.toList()));
                        }

                        // Apply
                        if (!superOC.containsKey(frameJumpForwardNext)) {
                            superOC.put(frameJumpForwardNext, pairsToAdd);

                        } else {
                            HashMap<String, List<Integer>> combinedPairs = new HashMap<>();
                            asList("divider", "whitespaceAbove", "whitespaceBelow").forEach(x -> {
                                List<Integer> listOfValuesToPut = new ArrayList<>();
                                if (pairsToAdd.containsKey(x)) {
                                    listOfValuesToPut = Stream.concat(listOfValuesToPut.stream(), pairsToAdd.get(x).stream()).collect(Collectors.toList());
                                }
                                if (superOC.get(frameJumpForwardNext).containsKey(x)) {
                                    listOfValuesToPut = Stream.concat(listOfValuesToPut.stream(), superOC.get(frameJumpForwardNext).get(x).stream()).collect(Collectors.toList());
                                }

                                combinedPairs.put(x, listOfValuesToPut);
                            });
                            superOC.put(frameJumpForwardNext, combinedPairs);
                        }

                        // If the key doesn't go anywhere, we've reached the end of the chain
                        if (!thisOC.containsKey(frameJumpForwardNext)) {
                            frameJumpForward = -1;
                        } else {
                            frameJumpForward = frameJumpForwardNext;
                        }
                    }
                }
                if (thisFrame > Collections.min(distinctKeys)) {

                // and also attempt to backwards on the frame
                int frameJumpBackward = thisFrame;
                int offsetCumulative = 0;
                while (frameJumpBackward != -1) {
                    // Determine the next frame
                    // XXX frameJumpForwardNext = 2
                    int finalFrameJumpBackward = frameJumpBackward;
                    List<Integer> frameJumpBackwardNextCandidate = thisOC.keySet().stream().filter(x -> thisOC.get(x).containsKey(finalFrameJumpBackward)).collect(Collectors.toList());
                    if (frameJumpBackwardNextCandidate.isEmpty()) {
                        frameJumpBackward = -1;
                    } else {
                        int frameJumpBackwardNext = frameJumpBackwardNextCandidate.get(0);
                        //if (thisFrame == 6) {
                         //   System.out.println("At frame "+thisFrame+" and projecting to "+frameJumpBackwardNext);
                       // }
                        // Determine the cumulative offset to the next frame
                        /// XXX offsetToNextFrame = -612
                        int offsetToNextFrame = thisOC.get(frameJumpBackwardNext).get(frameJumpBackward);
                        offsetCumulative += offsetToNextFrame;

                        // Project the media boundaries of thisFrame onto the frame's list within the superOC

                        HashMap<String, List<Integer>> pairsToAddB = new HashMap<>();
                        for (Integer indexY : screenshotMediaBoundaries.get(thisFrame).keySet()) {
                            String thisKey = screenshotMediaBoundaries.get(thisFrame).get(indexY);
                            if (!pairsToAddB.containsKey(thisKey)) {
                                pairsToAddB.put(thisKey, new ArrayList<>());
                            }
                            pairsToAddB.put(thisKey, Stream.concat(pairsToAddB.get(thisKey).stream(), Collections.singletonList( - offsetCumulative+ indexY).stream()).collect(Collectors.toList()));
                        }
                        // Project the dividers
                        for (Integer dividerIndexY : screenshotDividers.get(thisFrame)) {
                            String thisKey = "divider";
                            if (!pairsToAddB.containsKey(thisKey)) {
                                pairsToAddB.put(thisKey, new ArrayList<>());
                            }
                            pairsToAddB.put(thisKey, Stream.concat(pairsToAddB.get(thisKey).stream(), Collections.singletonList( - offsetCumulative + dividerIndexY).stream()).collect(Collectors.toList()));
                        }
                        // Apply
                        if (!superOC.containsKey(frameJumpBackwardNext)) {
                            superOC.put(frameJumpBackwardNext, pairsToAddB);

                        } else {
                            HashMap<String, List<Integer>> combinedPairs = new HashMap<>();
                            asList("divider", "whitespaceAbove", "whitespaceBelow").forEach(x -> {
                                List<Integer> listOfValuesToPut = new ArrayList<>();
                                if (pairsToAddB.containsKey(x)) {
                                    listOfValuesToPut = Stream.concat(listOfValuesToPut.stream(), pairsToAddB.get(x).stream()).collect(Collectors.toList());
                                }
                                if (superOC.get(frameJumpBackwardNext).containsKey(x)) {
                                    listOfValuesToPut = Stream.concat(listOfValuesToPut.stream(), superOC.get(frameJumpBackwardNext).get(x).stream()).collect(Collectors.toList());
                                }

                                combinedPairs.put(x, listOfValuesToPut);
                            });
                            superOC.put(frameJumpBackwardNext, combinedPairs);
                        }
                        // If the key doesn't go anywhere, we've reached the end of the chain
                        if (!thisOC.containsKey(frameJumpBackwardNext)) {
                            frameJumpBackward = -1;
                        } else {
                            frameJumpBackward = frameJumpBackwardNext;
                        }
                    }
                }

                }




                // dont forget to also add the frame's original values to the mix as well
                HashMap<String, List<Integer>> pairsToAddC = new HashMap<>();
                for (Integer indexY : screenshotMediaBoundaries.get(thisFrame).keySet()) {
                    String thisKey = screenshotMediaBoundaries.get(thisFrame).get(indexY);
                    if (!pairsToAddC.containsKey(thisKey)) {
                        pairsToAddC.put(thisKey, new ArrayList<>());
                    }
                    pairsToAddC.put(thisKey, Stream.concat(pairsToAddC.get(thisKey).stream(), Collections.singletonList(indexY).stream()).collect(Collectors.toList()));
                }
                for (Integer dividerIndexY : screenshotDividers.get(thisFrame)) {
                    if (!pairsToAddC.containsKey("divider")) {
                        pairsToAddC.put("divider", new ArrayList<>());
                    }
                    pairsToAddC.put("divider", Stream.concat(pairsToAddC.get("divider").stream(), Collections.singletonList(dividerIndexY).stream()).collect(Collectors.toList()));
                }


                HashMap<String, List<Integer>> combinedPairs = new HashMap<>();
                Integer finalThisFrame = thisFrame;
                asList("divider", "whitespaceAbove", "whitespaceBelow").forEach(x -> {
                    List<Integer> listOfValuesToPut = new ArrayList<>();
                    if (pairsToAddC.containsKey(x)) {
                        listOfValuesToPut = Stream.concat(listOfValuesToPut.stream(), pairsToAddC.get(x).stream()).collect(Collectors.toList());
                    }
                    if (superOC.get(finalThisFrame).containsKey(x)) {
                        listOfValuesToPut = Stream.concat(listOfValuesToPut.stream(), superOC.get(finalThisFrame).get(x).stream()).collect(Collectors.toList());
                    }
                    combinedPairs.put(x, listOfValuesToPut);
                });
                superOC.put(thisFrame, combinedPairs);
            }



            //ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            //String json = ow.writeValueAsString(thisOC);
                System.out.println("superOC: ");
                printJSON(superOC);
            //System.out.println(json);


            int likenessThreshold = (int) Math.round(consistentHeight*0.01); // ADJUSTINGFORRATIO 0.01
            //System.out.println("likenessThreshold: " + likenessThreshold);
            int minAppearance = 1;
            int cumulativeOffset = 0;
            HashMap<Integer, HashMap<Integer, List<Triple<Integer, Integer, Integer>>>> globalPartMapIntermediate = new HashMap<>();

            // global offset -> frame -> list of pairs -> [start, end]

                // Firstly determine the frequencies of various features - and bin them into averages based on likeness
                for (Integer thisFrame : distinctKeys) {

                    int finalConsistentHeight = consistentHeight;
                    HashMap<Double, List<Double>> binnedAveragesWhitespaceAbove = binAsAverages(Args(
                            A("input", superOC.get(thisFrame).get("whitespaceAbove").stream()
                                    .filter(x -> ((x > 0) && (x < finalConsistentHeight))).mapToDouble(x -> x).boxed().collect(Collectors.toList())), A("likeness", (double) likenessThreshold)));
                    HashMap<Double, List<Double>> binnedAveragesWhitespaceBelow = binAsAverages(Args(
                            A("input", superOC.get(thisFrame).get("whitespaceBelow").stream()
                                    .filter(x -> ((x > 0) && (x < finalConsistentHeight))).mapToDouble(x -> x).boxed().collect(Collectors.toList())), A("likeness", (double) likenessThreshold)));
                    HashMap<Double, List<Double>> binnedAveragesDividers = binAsAverages(Args(
                            A("input", superOC.get(thisFrame).get("divider").stream()
                                    .filter(x -> ((x > 0) && (x < finalConsistentHeight))).mapToDouble(x -> x).boxed().collect(Collectors.toList())), A("likeness", (double) likenessThreshold)));

                    HashMap<Double, Integer> frequenciesWhitespaceAbove = new HashMap<>();
                    for (Double x : binnedAveragesWhitespaceAbove.keySet().stream().filter(x -> (binnedAveragesWhitespaceAbove.get(x).size() >= minAppearance)).collect(Collectors.toList())) {
                        frequenciesWhitespaceAbove.put(x, binnedAveragesWhitespaceAbove.get(x).size());
                    }
                    HashMap<Double, Integer> frequenciesWhitespaceBelow = new HashMap<>();
                    for (Double x : binnedAveragesWhitespaceBelow.keySet().stream().filter(x -> (binnedAveragesWhitespaceBelow.get(x).size() >= minAppearance)).collect(Collectors.toList())) {
                        frequenciesWhitespaceBelow.put(x, binnedAveragesWhitespaceBelow.get(x).size());
                    }
                    HashMap<Double, Integer> frequenciesDivider = new HashMap<>();
                    for (Double x : binnedAveragesDividers.keySet().stream().filter(x -> (binnedAveragesDividers.get(x).size() >= minAppearance)).collect(Collectors.toList())) {
                        frequenciesDivider.put(x, binnedAveragesDividers.get(x).size());
                    }

                    HashMap<String, HashMap<Double, Integer>> retainedFeatures = new HashMap<>();
                    retainedFeatures.put("whitespaceAbove", frequenciesWhitespaceAbove);
                    retainedFeatures.put("whitespaceBelow", frequenciesWhitespaceBelow);
                    retainedFeatures.put("divider", frequenciesDivider);

                    retainedFeaturesGlobal.put(thisFrame, retainedFeatures);
                }

                List<Integer> commonValues = new ArrayList<>();
                for (Integer thisFrame : retainedFeaturesGlobal.keySet()) {
                    commonValues = Stream.concat(commonValues.stream(), retainedFeaturesGlobal.get(thisFrame).get("divider").values().stream()).collect(Collectors.toList());
                }

                // Two values are important for determining the min number of times a divider should be seen to be true
                // The first is the average number of observations - the second is the standard deviation
                //
                // Consider that under normal circumstances that 68% of data resides in 1 SD from the average
                // If we wish to obtain the top most centrally tended ~25% of data, we divide the SD by 68, then multiply by 25
                // and add / minus it from the average to obtain an ideal bound
                // EG. if the average is 3.69 and the SD is 3.48, the bound is 3.69 +- 1.27941176471
                //
                // NB: We can relinquish the upper bound - more is better

                printJSON(commonValues);
                //System.out.println(commonValues.stream().mapToDouble(x -> x).average()*0.75*05);
                //System.out.println(getStandardDeviation(commonValues));

                double avgNObservations = optionalGetDouble(commonValues.stream().mapToDouble(x -> x).average());
                double sdvNObservations = getStandardDeviation(commonValues);
                double sdvNObservationsQtr = sdvNObservations / 68 * 25;
                double minNumberOfTimesADividerShouldBeSeenToBeTrueLB = avgNObservations - sdvNObservationsQtr;
                System.out.println(avgNObservations);
                System.out.println(sdvNObservations);
                System.out.println(sdvNObservationsQtr);
                System.out.println(minNumberOfTimesADividerShouldBeSeenToBeTrueLB);


                //System.out.println("screenshotDividers:  ");
                //System.out.println((new ObjectMapper().writer().withDefaultPrettyPrinter()).writeValueAsString(screenshotDividers));
                //System.out.println("superOC:  ");
                //System.out.println((new ObjectMapper().writer().withDefaultPrettyPrinter()).writeValueAsString(superOC));



                for (Integer thisFrame : distinctKeys) {
                    HashMap<String, HashMap<Double, Integer>> retainedFeatures = retainedFeaturesGlobal.get(thisFrame);
                    HashMap<Double, Integer> frequenciesDivider = retainedFeatures.get("divider");

                    // Derive the true dividers
               /*
               //make it that we need at least 3 indices to qualify a divider reading -< tentative

               for each index

               find all other indices that occur within 100 pixels either above or below it

               if it is not the maximum of said range, it is not a divider
               */
                    HashMap<Double, Integer> frequenciesDividerTrue = new HashMap<>();
                    double trueDividerDerivingRange = 0.1;
                    double minObservations = minNumberOfTimesADividerShouldBeSeenToBeTrueLB;


                    // To derive the true dividers, we need to determine the minimum number of observations that
                    // qualifies a divider to be "true" - to do this, we firstly group up observations of dividers
                    // across the various superimposed frames, and then determine the 'average' number of times a divider
                    // is seen across all frames

                    // TODO - the true dividers needs to be derived more carefully - its not enough to say that we rely on 3 observations - we need a
                    // more charitable (yet cautious) approach
                    // also - deal with sizing issues
                    List<Integer> frequenciesDividerTrueKeySet = new ArrayList<>();
                    for (Double indexY : frequenciesDivider.keySet()) {
                        if (frequenciesDivider.get(indexY) > minObservations) {
                            double lowerBound = (indexY - consistentHeight*trueDividerDerivingRange);
                            double upperBound = (indexY + consistentHeight*trueDividerDerivingRange);
                            // Find all dividers within 10% range of this y
                            List<Double> altDividersYList = frequenciesDivider.keySet().stream().filter(x -> ((x > lowerBound) && (x < upperBound))).collect(Collectors.toList());
                            if ((altDividersYList.isEmpty()) || (altDividersYList.stream().noneMatch(x -> frequenciesDivider.get(x) > frequenciesDivider.get(indexY)))) {
                                frequenciesDividerTrue.put(indexY, frequenciesDivider.get(indexY));
                                frequenciesDividerTrueKeySet.add(Math.toIntExact(Math.round(indexY)));
                            }
                        }
                    }
                    retainedFeatures.put("trueDividers",frequenciesDividerTrue);


                    // Then for each offset chain
                    // go through and split up each frame using the true dividers
                    // for each part, determine its absolute offset (relative to all other frames, using a cumulative offset)
                    // use the y index at the top of the 'part' to assign it into a bucket, based on values of true dividers, or on its own device


                    // global offset -> frame -> list of pairs -> [start, end]
                    //HashMap<Integer, HashMap<Integer, List<Pair<Integer, Integer>>>> globalPartMap = new HashMap<>();

                    // Create a list of boundaries, using the true dividers, and the boundaries of the image
                    List<Integer> boundaries = Stream.concat(Stream.of(0, consistentHeight), frequenciesDividerTrueKeySet.stream()).distinct().sorted().collect(Collectors.toList());
                    System.out.println("boundaries: " + boundaries);


                    if (boundaries.size() > 1) {
                        for (int j = 0; j < boundaries.size()-1; j ++) {
                            int startOfBoundary = boundaries.get(j);
                            int endOfBoundary = boundaries.get(j+1);

                            // TODO - introducing an assumption that a boundary less than 5% the size of the canvas is negligible
                            if (Math.abs(startOfBoundary - endOfBoundary) > consistentHeight*0.05) {

                                // determine the snapped key, by taking this offset, and determining if it matches an existing offset
                                int thisAdjustedOffset = startOfBoundary + cumulativeOffset;
                                Triple<Integer, Integer, Integer> actualBoundaries = new Triple<>(startOfBoundary, endOfBoundary, thisAdjustedOffset); // TODO - check fidelity of this - we need to conduct an oscillating test

                                List<Integer> adjustedOffsetsSmaller = globalPartMapIntermediate.keySet().stream().filter(x -> (x <= thisAdjustedOffset)).collect(Collectors.toList());
                                int roughOffset = (int) Math.round(consistentHeight*0.01);
                                List<Integer> adjustedOffsetsClose = globalPartMapIntermediate.keySet().stream().filter(x -> Math.abs(x - thisAdjustedOffset) < roughOffset).collect(Collectors.toList());
                                // Defaults to the offset of the boundary itself
                                Integer thisDeterminedOffset = thisAdjustedOffset;

                                // If there are no potentially close adjusted offsets within the globalPartMap.get(thisFrame).keySet()
                                if (adjustedOffsetsClose.isEmpty()) {
                                    // If there are candidates in the adjustedOffsetsSmaller list, AND this offset is derived from the image, and not a true divider
                                    if ((!adjustedOffsetsSmaller.isEmpty()) && (!frequenciesDividerTrueKeySet.contains(startOfBoundary))) {
                                        // Apply the last value
                                        thisDeterminedOffset = adjustedOffsetsSmaller.get(adjustedOffsetsSmaller.size()-1);
                                    } else {
                                        // Do nothing, we'll apply the default value
                                    }
                                } else {
                                    // Get the last index of adjustedOffsetsClose that matches this
                                    thisDeterminedOffset = adjustedOffsetsClose.get(adjustedOffsetsClose.size()-1);
                                }

                                if (!globalPartMapIntermediate.containsKey(thisDeterminedOffset)) {
                                    globalPartMapIntermediate.put(thisDeterminedOffset, new HashMap<>());
                                }

                                if (!globalPartMapIntermediate.get(thisDeterminedOffset).containsKey(thisFrame)) {
                                    globalPartMapIntermediate.get(thisDeterminedOffset).put(thisFrame, new ArrayList<>());//new ArrayList<>());
                                }
                                globalPartMapIntermediate.get(thisDeterminedOffset).put(thisFrame, Stream.concat(asList(actualBoundaries).stream(),
                                        globalPartMapIntermediate.get(thisDeterminedOffset).get(thisFrame).stream()).collect(Collectors.toList()));
                                //List<Pair<Integer, Integer>> listOfValues = globalPartMap.get(thisDeterminedOffset).get(thisFrame);
                                //listOfValues.add(actualBoundaries);
                                //globalPartMap.get(thisDeterminedOffset).put(thisFrame, listOfValues);

                            }








                     /*
                     if (!globalPartMap.containsKey(thisFrame)) {
                        globalPartMap.put(thisFrame, new HashMap<>());
                     }
                     List<Integer> adjustedOffsetsSmaller = globalPartMap.get(thisFrame).keySet().stream().filter(x -> (x < thisAdjustedOffset)).collect(Collectors.toList());
                     List<Integer> adjustedOffsetsClose = globalPartMap.get(thisFrame).keySet().stream().filter(x -> Math.abs(x - thisAdjustedOffset) < 50).collect(Collectors.toList());

                     // If there are no potentially close adjusted offsts within the globalPartMap.get(thisFrame).keySet()
                     if (adjustedOffsetsClose.isEmpty()) {
                        // If there are candidates in the adjustedOffsetsSmaller list, AND this offset is derived from the image, and not a true divider
                        if ((!adjustedOffsetsSmaller.isEmpty()) && (!frequenciesDividerTrueKeySet.contains(startOfBoundary))) {
                           globalPartMap.get(thisFrame).get(adjustedOffsetsSmaller.get(adjustedOffsetsSmaller.size()-1)).add(actualBoundaries);
                        } else {
                           // Create a new field for this
                           globalPartMap.get(thisFrame).put(thisAdjustedOffset, Arrays.asList(actualBoundaries).stream().collect(Collectors.toList()));
                        }
                     } else {
                        // Get the last index of adjustedOffsetsClose that matches this
                        System.out.println("thisFrame: "  + thisFrame);
                        System.out.println("adjustedOffsetsClose.size()-1: "  + (adjustedOffsetsClose.size()-1));
                        System.out.println("adjustedOffsetsClose: "  + (adjustedOffsetsClose));
                        System.out.println("actualBoundaries: "  + actualBoundaries);
                        System.out.println("globalPartMap.get(thisFrame): "  + globalPartMap.get(thisFrame));
                        HashMap<Integer, List<Pair<Integer, Integer>>> frameListOfPairs = globalPartMap.get(thisFrame);
                        List<Pair<Integer, Integer>> listOfValues = frameListOfPairs.get(adjustedOffsetsClose.get(adjustedOffsetsClose.size()-1));
                        listOfValues.add(actualBoundaries);
                        globalPartMap.get(thisFrame).put(adjustedOffsetsClose.get(adjustedOffsetsClose.size()-1), listOfValues);

                     }*/
                        }

                    }

                    // Apply the offset
                    if (thisOC.containsKey(thisFrame)) {
                        cumulativeOffset -= thisOC.get(thisFrame).get(thisOC.get(thisFrame).keySet().iterator().next());
                        System.out.println("cumulativeOffset: " + cumulativeOffset);
                    }






                    retainedFeaturesGlobal.put(thisFrame, retainedFeatures);
                    //superOC.get(thisFrame).put("whitespaceAbove", binAsAverages(Args(A("input", superOC.get(thisFrame).get("whitespaceAbove")), A("likeness", (double) 50))).keySet().stream().collect(Collectors.toList()) );
                }



            System.out.println("thisOC: ");
            printJSON(thisOC);


            System.out.println("globalPartMapIntermediate: ");
                printJSON(globalPartMapIntermediate);
            //System.out.println((new ObjectMapper().writer().withDefaultPrettyPrinter()).writeValueAsString(globalPartMapIntermediate));
            // A single boundary may contain multiple parts - here we aggregate them
            HashMap<Integer, HashMap<Integer, Triple<Integer, Integer, Integer>>> globalPartMap = new HashMap<>();
            for (Integer determinedOffset : globalPartMapIntermediate.keySet()) {
                globalPartMap.put(determinedOffset, new HashMap<>());
                for (Integer thisFrame : globalPartMapIntermediate.get(determinedOffset).keySet()) {
                    Integer boundaryStart = (int) Math.round(optionalGetDouble(globalPartMapIntermediate.get(determinedOffset).get(thisFrame).stream().map(Triple::getFirst).mapToDouble(x -> x).min()));
                    Integer boundaryEnd = (int) Math.round(optionalGetDouble(globalPartMapIntermediate.get(determinedOffset).get(thisFrame).stream().map(Triple::getSecond).mapToDouble(x -> x).max()));
                    Integer boundaryOffset = (int) Math.round(optionalGetDouble(globalPartMapIntermediate.get(determinedOffset).get(thisFrame).stream().map(Triple::getThird).mapToDouble(x -> x).max()));
                    globalPartMap.get(determinedOffset).put(thisFrame, new Triple(boundaryStart, boundaryEnd, boundaryOffset));
                }
            }

            System.out.println("globalPartMap:");
            printJSON(globalPartMap);
            //System.out.println((new ObjectMapper().writer().withDefaultPrettyPrinter()).writeValueAsString(globalPartMap));

            // consider that the corresponding global part map of this offset chain
            // groups up all posts by key of the global part map

            // take the largest frame (there always is one)
            // step backwards on it, and insert each consecutive frame before it, as superimposed, and offset to the starting frame
            // do the same thing forwards

            for (Integer offsetID : globalPartMap.keySet()) {
                // take the largest frame (there always is one)
                Integer finalOffsetID = offsetID;
                List<List< Integer>> frameCaptureHeights = globalPartMap.get(offsetID).keySet().stream()
                        .map(x -> asList(x, Math.abs(globalPartMap.get(finalOffsetID).get(x).getFirst() - globalPartMap.get(finalOffsetID).get(x).getSecond()))).collect(Collectors.toList());
                Integer maxHeight = Math.toIntExact(Math.round(optionalGetDouble(frameCaptureHeights.stream().map(x -> x.get(1)).mapToDouble(x -> x).max())));
                Integer indexOfTallestFrame = frameCaptureHeights.stream().filter(y -> y.get(1).equals(maxHeight)).collect(Collectors.toList()).get(0).get(0);
                Integer keyOfTallestFrame = indexOfTallestFrame;
                // step backwards on it, and insert each consecutive frame before it, as superimposed, and offset to the starting frame
                Bitmap compositeCanvas = null;


                // Do a dry-run to determine the actual composite height of the content
                //TODO - this MUST be refactored
                ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

                // start at the key of the tallest frame

                // insert its composite canvas



                //System.out.println("minValue: " + minValue);
                // System.out.println("maxValue: " + maxValue);
                List<Integer> keysInOffset = new ArrayList<>(globalPartMap.get(offsetID).keySet().stream().sorted().collect(Collectors.toList()));
                // Initialize the composite canvas
                Bitmap compositeBitmap = Bitmap.createBitmap(consistentWidth, maxHeight, Bitmap.Config.ARGB_8888); //Bitmap.createBitmap(bitmapsMap.get(keyOfTallestFrame), 0, globalPartMap.get(offsetID).get(keyOfTallestFrame).first, consistentWidth, maxHeight);
                // Re-initialize the composite canvas
                compositeBitmap = Bitmap.createBitmap(consistentWidth, maxHeight, Bitmap.Config.ARGB_8888); //Bitmap.createBitmap(bitmapsMap.get(keyOfTallestFrame), 0, globalPartMap.get(offsetID).get(keyOfTallestFrame).first, consistentWidth, maxHeight);

                System.out.println("keysInOffset: " + keysInOffset);
                printJSON(keysInOffset);
                // CONSIDER the first offset bracket of each

                Integer runningOffset = 0;
                for (int k = 0; k < keysInOffset.size(); k ++) {
                    System.out.println(k);
                    Integer thisKey = keysInOffset.get(k);

                    Integer thisHeight = Math.abs(globalPartMap.get(offsetID).get(thisKey).getFirst() - globalPartMap.get(offsetID).get(thisKey).getSecond());

                    // System.out.println("projectedOffset: " + xOffset);
                    // Generate (and create a copy for superimposition of) the cropping
                    System.out.println("runningOffset: " + runningOffset);
                    System.out.println("thisHeight: " + thisHeight);
                    Integer thisBoundaryOffset = (globalPartMap.get(offsetID).get(thisKey).getThird() - offsetID);
                    Bitmap thisKeyCropping = Bitmap.createBitmap(bitmapsMap.get(thisKey), 0, globalPartMap.get(offsetID).get(thisKey).getFirst(), consistentWidth, thisHeight);
                    thisKeyCropping = thisKeyCropping.copy(thisKeyCropping.getConfig(), true);

                    System.out.println("thisBoundaryOffset: " + thisBoundaryOffset);

                    Bitmap keyCroppingOffsetInComposite = overlayBitmaps(Bitmap.createBitmap(compositeBitmap), thisKeyCropping, 0,thisBoundaryOffset);// Math.max(0, runningOffset - offsetID));//#Math.max(0, runningOffset - offsetID));
                    saveBitmap(keyCroppingOffsetInComposite,(new File(thisOutputFolder.getAbsolutePath(), "compositeCanvas-"+thisChunkNumber+"-"+offsetID+"-"+thisKey+".png")).getAbsolutePath());

                    if (k != (keysInOffset.size() - 1)) {
                        System.out.println(thisKey + " "+ keysInOffset.get(k + 1));
                        runningOffset -= cumulativeOffsetForwards(thisOC, thisKey, keysInOffset.get(k + 1));
                    }
                }

            }

            System.out.println("retainedFeaturesGlobal: ");
            System.out.println("Writing metadata.json");
            try {
                PrintWriter writer = new PrintWriter((new File(thisOutputFolder, "metadata-"+thisChunkNumber+".json")), "UTF-8");
                writer.println((new ObjectMapper().writer().withDefaultPrettyPrinter()).writeValueAsString(retainedFeaturesGlobal));
                writer.close();
            } catch (Exception e) {}
            try {
                PrintWriter writer = new PrintWriter((new File(thisOutputFolder, "fnames-"+thisChunkNumber+".json")), "UTF-8");
                writer.println((new ObjectMapper().writer().withDefaultPrettyPrinter()).writeValueAsString(dataStructure.get("fnames")));
                writer.close();
            } catch (Exception e) {}

            //System.out.println((new ObjectMapper().writer().withDefaultPrettyPrinter()).writeValueAsString(retainedFeaturesGlobal));



            // ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            // String json = ow.writeValueAsString(retainedFeaturesGlobal);
            // System.out.println(json);


            //ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            //String json = ow.writeValueAsString(globalPartMap);
            //System.out.println(json);





            // Uncomment to write up
            //System.out.println("retainedFeaturesGlobal");
            //ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            //String json = ow.writeValueAsString(retainedFeaturesGlobal);
            //System.out.println(json);
            }
        }
    }

    // TODO - splitter can be made more accurate with rgb checks - checked, and it uses up a lot of processing

    public static HashMap<Integer, Double> generateProjection(int consistentHeight, HashMap<Integer, String> iMediaBoundaries, JSONObject thisJSONObject) {
        List<Integer> projectionI = IntStream.range(0, consistentHeight).map(x-> -1).boxed().collect(Collectors.toList());
        for (Integer thisMediaBoundaryI : iMediaBoundaries.keySet()) {
            if (Objects.equals(iMediaBoundaries.get(thisMediaBoundaryI), "whitespaceAbove")) {
                if (thisMediaBoundaryI > 0) {
                    projectionI.set(thisMediaBoundaryI-1, 1);
                }
                projectionI.set(thisMediaBoundaryI, 0);
            } else {
                if (thisMediaBoundaryI > 0) {
                    projectionI.set(thisMediaBoundaryI-1, 0);
                }
                projectionI.set(thisMediaBoundaryI, 1);
            }
        }

        // Fill in the projection
        int retainedValue = -1;

        for (int q = 0; q < consistentHeight; q ++) {
            if (projectionI.get(q) != -1) {
                // If this is the first determined value, do a backfill
                if (retainedValue == -1) {
                    for (int r = 0; r < q; r ++) {
                        projectionI.set(r, projectionI.get(q));
                    }
                }
                retainedValue = projectionI.get(q);
            } else {
                projectionI.set(q, retainedValue);
            }
        }

        HashMap<Integer, Double> avgColoursI = new HashMap<>();
        try {
            avgColoursI = (HashMap<Integer, Double>) thisJSONObject.get("ArrayThisAverageColourDifferenceToWhitespacePixelPercentage");
        } catch (Exception e) {}
        for (Integer yy : avgColoursI.keySet()) {
            if (projectionI.get(yy) == 0) {
                avgColoursI.put(yy, -1.0);
            } else {
                avgColoursI.put(yy, projectionI.get(yy) * avgColoursI.get(yy));
            }
        }

        return avgColoursI;
    }

    public static HashMap<Integer, String> findMediaBoundaries(Bitmap thisBitmap, JSONObject statistics) {


        int verticalStrideUnit = 0;
        HashMap<Integer, Double> ArrayThisAverageColourDifferenceToWhitespacePixelPercentage = new HashMap<>();
        HashMap<Integer, Double> ArrayThisDominantColourDifferenceToWhitespacePixelPercentage = new HashMap<>();
        HashMap<Integer, Double> ArraySpreadOfDominantColourPercentage = new HashMap<>();
        HashMap<Integer, Double> ArrayFrequencyOfDominantColourPercentage = new HashMap<>();
        HashMap<Integer, Double> ArrayMaxPixelAdjacency = new HashMap<>();
        HashMap<Integer, Double> ArrayAverageOnEdge = new HashMap<>();
        HashMap<Integer, Double> ArrayThisAverageColourRDifferenceToWhitespacePixelRPercentage = new HashMap<>();
        HashMap<Integer, Double> ArrayThisAverageColourGDifferenceToWhitespacePixelGPercentage = new HashMap<>();
        HashMap<Integer, Double> ArrayThisAverageColourBDifferenceToWhitespacePixelBPercentage = new HashMap<>();
        try {
            verticalStrideUnit = (int) statistics.get("verticalStrideUnit");
            ArrayThisAverageColourDifferenceToWhitespacePixelPercentage = (HashMap<Integer, Double>) statistics.get("ArrayThisAverageColourDifferenceToWhitespacePixelPercentage");

            ArrayThisAverageColourRDifferenceToWhitespacePixelRPercentage = (HashMap<Integer, Double>) statistics.get("ArrayThisAverageColourRDifferenceToWhitespacePixelRPercentage");
            ArrayThisAverageColourGDifferenceToWhitespacePixelGPercentage = (HashMap<Integer, Double>) statistics.get("ArrayThisAverageColourGDifferenceToWhitespacePixelGPercentage");
            ArrayThisAverageColourBDifferenceToWhitespacePixelBPercentage = (HashMap<Integer, Double>) statistics.get("ArrayThisAverageColourBDifferenceToWhitespacePixelBPercentage");

            ArrayThisDominantColourDifferenceToWhitespacePixelPercentage = (HashMap<Integer, Double>) statistics.get("ArrayThisDominantColourDifferenceToWhitespacePixelPercentage");
            ArraySpreadOfDominantColourPercentage = (HashMap<Integer, Double>) statistics.get("ArraySpreadOfDominantColourPercentage");
            ArrayFrequencyOfDominantColourPercentage = (HashMap<Integer, Double>) statistics.get("ArrayFrequencyOfDominantColourPercentage");
            ArrayMaxPixelAdjacency = (HashMap<Integer, Double>) statistics.get("ArrayMaxPixelAdjacency");
            ArrayAverageOnEdge = (HashMap<Integer, Double>) statistics.get("ArrayPixels");
        } catch (Exception e) {

        }

        List<Integer> matches = new ArrayList<>();
        List<Boolean> whiteSpaceAbove = new ArrayList<>();

        // TODO - determine which sides of media boundaries belong to whitespace / not whitespcae

        int offset = 4;
        for (int yy = 0; yy < thisBitmap.getHeight(); yy += verticalStrideUnit) {
            int finalVerticalStrideUnit = verticalStrideUnit;

            List<Integer> thisRangeB = IntStream.range(Math.max(0, yy - (verticalStrideUnit*offset)), yy).filter(x -> x % finalVerticalStrideUnit == 0).boxed().collect(Collectors.toList());
            List<Integer> thisRangeF = IntStream.range(yy, Math.min(thisBitmap.getHeight(), yy + (verticalStrideUnit*offset))).filter(x -> x % finalVerticalStrideUnit == 0).boxed().collect(Collectors.toList());

            double ArrayAverageOnEdgeB = optionalGetDouble(thisRangeB.stream().mapToDouble(ArrayAverageOnEdge::get).average());

            double ArrayAverageOnEdgeF = optionalGetDouble(thisRangeF.stream().mapToDouble(ArrayAverageOnEdge::get).average());


            double ArrayRAverageColourB = optionalGetDouble(thisRangeB.stream().mapToDouble(ArrayThisAverageColourRDifferenceToWhitespacePixelRPercentage::get).average());
            double ArrayGAverageColourB = optionalGetDouble(thisRangeB.stream().mapToDouble(ArrayThisAverageColourGDifferenceToWhitespacePixelGPercentage::get).average());
            double ArrayBAverageColourB = optionalGetDouble(thisRangeB.stream().mapToDouble(ArrayThisAverageColourBDifferenceToWhitespacePixelBPercentage::get).average());

            double ArrayRAverageColourF = optionalGetDouble(thisRangeF.stream().mapToDouble(ArrayThisAverageColourRDifferenceToWhitespacePixelRPercentage::get).average());
            double ArrayGAverageColourF = optionalGetDouble(thisRangeF.stream().mapToDouble(ArrayThisAverageColourGDifferenceToWhitespacePixelGPercentage::get).average());
            double ArrayBAverageColourF = optionalGetDouble(thisRangeF.stream().mapToDouble(ArrayThisAverageColourBDifferenceToWhitespacePixelBPercentage::get).average());

            double thresholdRGB = 0.025;
            boolean c3 = ((ArrayRAverageColourB > thresholdRGB) || (ArrayGAverageColourB > thresholdRGB) || (ArrayBAverageColourB > thresholdRGB));
            boolean c4 = ((ArrayRAverageColourF > thresholdRGB) || (ArrayGAverageColourF > thresholdRGB) || (ArrayBAverageColourF > thresholdRGB));
            boolean c5 = (c3 && (!c4));
            boolean c6 = ((!c3) && c4);




            double threshold = 0.075; // adjusted from 0.05 - a glorious threshold
            boolean c1 = ((ArrayAverageOnEdgeB <= threshold) && (ArrayAverageOnEdgeF > threshold));
            boolean c2 = ((ArrayAverageOnEdgeF <= threshold) && (ArrayAverageOnEdgeB > threshold));



            if (c1 || c2) {//{/((c1 && c6) || (c2 && c5)) {//(c1 || c1Alt) && (c2 || c2Alt) && (c3 || c3Alt)) {
                // System.out.println(yy + " !!!!!!!!");
                // System.out.println("\t\t ArrayAverageOnEdgeB: "+ArrayAverageOnEdgeB);
                // System.out.println("\t\t ArrayAverageOnEdgeF: "+ArrayAverageOnEdgeF);
                matches.add(yy);
                whiteSpaceAbove.add(c1);
            }

        }


        List<Integer> newMatches = generateNewMatches(matches, verticalStrideUnit);
        // System.out.println(newMatches);
        HashMap<Integer, String> newMatchesWS = new HashMap<>();
        for (int i = 0; i < newMatches.size(); i ++) {
            int finalVerticalStrideUnit1 = verticalStrideUnit;
            int finalI = i;
            List<Integer> applicableMatches = matches.stream().filter(x -> Math.abs(x - newMatches.get(finalI)) < finalVerticalStrideUnit1).collect(Collectors.toList());
            if (applicableMatches.size() > 0) {
                newMatchesWS.put(newMatches.get(i), (whiteSpaceAbove.get(matches.indexOf(applicableMatches.get(0)))) ? "whitespaceAbove" : "whitespaceBelow");
            }
        }

        //System.out.println(matches);
        //System.out.println(whiteSpaceAbove);
        //System.out.println(newMatchesWS);
        return newMatchesWS;
    }
    public final Context testContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Test
    public void test2() {

        File commonFolder = (new File(String.valueOf(filePath(asList(((new File(".")).getAbsolutePath()),
                "src", "debug", "assets", "local", "contentInterpreterSimulations", "NEW", "facebook_dark_sd_large_long_inapp_2")))));

        String bitmapFname = (new File( commonFolder, "x.png").getAbsolutePath());
        // Load it up and then back down
        Bitmap thisBitmap = BitmapFactory.decodeFile(bitmapFname);

        System.out.println(colourToHex(thisBitmap.getPixel(195, 733)));

        try (FileOutputStream out = new FileOutputStream(bitmapFname)) {
            thisBitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (IOException e) {
            e.printStackTrace();
        }// #73584F

    }

    /*
    *
    * Retrieve a bitmap at a given millisecond within a video file (note that this is the mocked version of the function)
    *
    * */
    private static Bitmap getMP4At(File videoFile, Integer timeInMicroseconds, Integer frameNumber, double scale, int minWidth) {
        Bitmap thisBitmap = null;
        try {
            // Initialize the pseudo frame grabber
            FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(videoFile);
            frameGrabber.setFormat("mp4");
            frameGrabber.start();
            // Jump to (and grab) the desired frame
            if (timeInMicroseconds !=  null) {
                frameGrabber.setTimestamp((long) timeInMicroseconds);
            } else {
                frameGrabber.setVideoFrameNumber(frameNumber);
            }
            Frame frame = frameGrabber.grab();
            // Run the necessary conversion to adjust the format
            AndroidPseudoFrameConverter convertToBitmap = new AndroidPseudoFrameConverter();
            Bitmap bitmap = convertToBitmap.convert(frame);
            // The original bitmap container is corrupted slightly to make all RGB values half of their absolute values // TODO - correct this
            thisBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            for (Integer xx : IntStream.range(0, bitmap.getWidth()).toArray()) {
                for (Integer yy : IntStream.range(0, bitmap.getHeight()).toArray()) {
                    thisBitmap.setPixel(xx, yy, Color.parseColor(colourToHex(bitmap.getPixel(xx, yy))));
                }
            }
            // Run resizing (if necessary);
            if ((thisBitmap.getWidth()*scale) < minWidth) {
                thisBitmap = Bitmap.createScaledBitmap(thisBitmap,
                minWidth,
                (int)Math.floor((double) thisBitmap.getHeight() /thisBitmap.getWidth()*minWidth), false);
                return thisBitmap;
            }
            thisBitmap = Bitmap.createScaledBitmap(thisBitmap,
            Math.max((int)Math.floor(thisBitmap.getWidth()*scale),1),
            Math.max((int)Math.floor(thisBitmap.getHeight()*scale),1), false);
            frameGrabber.stop();
            return thisBitmap;
        } catch (Exception e) {
            return null;
        }
    }

    public static JSONObject frameWithinFacebook(Bitmap thisFrame, Integer frameAnnotation) {
        JSONObject statistics = new JSONObject();
        try {
            for (String thisMode : Arrays.asList("light", "dark")) {
                JSONObject outcome = new JSONObject();
                statistics.put(thisMode, outcome);
            }
        } catch (Exception e) {

        }

        Integer absoluteWhitespaceColourLight = Color.rgb(252, 254, 253);
        Integer absoluteWhitespaceColourDark = Color.rgb(35, 35, 35);

        Integer w = thisFrame.getWidth();
        Integer h = thisFrame.getHeight();
        Double samplePercentageW = 0.1; // Get 10% of width
        Double samplePercentageH = 0.02; // Get 5% of height
        Double thresholdOfWhitespaceSimilarityAtPixel = 0.05;
        Double thresholdOfWhitespaceSimilarityAtRow = 0.05;
        int strideX = (int) Math.round(w * samplePercentageW);
        int strideY = (int) Math.round(h * samplePercentageH);


        // Firstly determine the colours of a sample of pixels within the image
        HashMap<String, HashMap<Integer, Boolean>> whitespaceRows = new HashMap<>();
        HashMap<String, HashMap<Integer, Double>> whitespaceRowsPcts = new HashMap<>(); // TODO - unnecessary
        HashMap<String, HashMap<Integer, List<Integer>>> dissimilarPixels = new HashMap<>();
        for (String thisMode : Arrays.asList("light", "dark")) {
            whitespaceRows.put(thisMode, new HashMap<>());
            whitespaceRowsPcts.put(thisMode, new HashMap<>());
            dissimilarPixels.put(thisMode, new HashMap<>());
        }
        for (int yy = 0; yy < h; yy += strideY) {
            HashMap<String, List<Double>> thisRow = new HashMap<>();
            for (String thisMode : Arrays.asList("light", "dark")) {
                dissimilarPixels.get(thisMode).put(yy, new ArrayList<>());
                thisRow.put(thisMode, new ArrayList<>());
            }
            for (int xx = 0; xx < w; xx += strideX) {
                // Section up the frame into rows
                // (where each row has a percentage therein that match the whitespace colour)
                for (String thisMode : Arrays.asList("light", "dark")) {
                    Integer thisWhitespaceColour = (thisMode.equals("light")) ? absoluteWhitespaceColourLight : absoluteWhitespaceColourDark;
                    Double thisPixelDifference = pixelDifferencePercentage(thisWhitespaceColour, thisFrame.getPixel(xx, yy));
                    thisRow.get(thisMode).add(thisPixelDifference);
                    // Retain the pixels that are not similar to the whitespace (we should observe that they are inconsistent wrt. each other
                    if (thisPixelDifference > thresholdOfWhitespaceSimilarityAtPixel) {
                        Objects.requireNonNull(dissimilarPixels.get(thisMode).get(yy)).add(xx);
                    }
                }
            }
            for (String thisMode : Arrays.asList("light", "dark")) {
                // Determine the average colour of this row
                Double averageColourDifferenceOfThisRow = optionalGetDouble(thisRow.get(thisMode).stream().mapToDouble(x -> x).average());
                // Record its result as a measure of whether it is whitespace (or not)
                whitespaceRows.get(thisMode).put(yy, (averageColourDifferenceOfThisRow <= thresholdOfWhitespaceSimilarityAtRow));
                whitespaceRowsPcts.get(thisMode).put(yy, averageColourDifferenceOfThisRow);
            }
        }

        // Go over the frame and determine what part of it is consistently whitespace
        // We should expect that all whitespace should pile together, with the exception of regions that
        // sit on the edges of the Y axis
        // Then derive a percentage of the frame that 'should' be whitespace, dependent on the samplePercentageH value
        /*
        if (frameAnnotation == 168) {
            for (Integer yy : whitespaceRows.get("dark").keySet().stream().sorted().collect(Collectors.toList())) {
                System.out.println(yy);
                System.out.println("\t\t" + whitespaceRows.get("dark").get(yy));
                System.out.println("\t\t" + whitespaceRowsPcts.get("dark").get(yy));
                System.out.println("\t\t" + dissimilarPixels.get("dark").get(yy));
            }
            System.out.println("####################################");
            System.out.println("####################################");
            System.out.println("####################################");
        }*/


        // Go over groups of adjacent rows that are consistently not whitespace
        //
        // Identify problem pixels - if any single consecutive set of pixels is less than half the width
        // of the entire image, qualify it as a consistent set of pixels for that row
        //
        // Then compare the sets of problem pixels between the adjacent rows - if they are
        // almost identical with respect to each other, we can reassess the rows as being whitespace
        HashMap<String, List<Integer>> thisGroupOfAdjacentRows = new HashMap<>();
        HashMap<String, HashMap<Integer, List<Integer>>> pixelsInWhitespaceThatAreVisualElements = new HashMap<>();
        for (String thisMode : Arrays.asList("light", "dark")) {
            thisGroupOfAdjacentRows.put(thisMode, new ArrayList<>());
            pixelsInWhitespaceThatAreVisualElements.put(thisMode, new HashMap<>());

        }
        for (int yy = 0; yy < h; yy += strideY) {
            for (String thisMode : Arrays.asList("light", "dark")) {
                if (!whitespaceRows.get(thisMode).get(yy)) {
                    thisGroupOfAdjacentRows.get(thisMode).add(yy);
                } else {
                    if (!thisGroupOfAdjacentRows.get(thisMode).isEmpty()) {
                        // TODO - do processing here
                        // Construct the sets of problem pixels, within their respective rows
                        HashMap<Integer, List<List<Integer>>> groupsOfAdjacentRowsGroupedProblemPixels = new HashMap<>();
                        for (Integer thisProblemRowY : thisGroupOfAdjacentRows.get(thisMode)) {
                            groupsOfAdjacentRowsGroupedProblemPixels.put(thisProblemRowY, new ArrayList<>());
                            // Reconstruct the pixels into consecutive groups
                            List<Integer> currentSetOfPixels = new ArrayList<>();
                            for (Integer problemPixel : dissimilarPixels.get(thisMode).get(thisProblemRowY).stream().sorted().collect(Collectors.toList())) {
                                if (currentSetOfPixels.isEmpty()) {
                                    currentSetOfPixels.add(problemPixel);
                                } else {
                                    // If the pixel's distance to the last pixel in the current set is smaller than or equal to the strideX,
                                    // join it to the set
                                    Integer lastValueInSet = currentSetOfPixels.stream().sorted().collect(Collectors.toList()).get(currentSetOfPixels.size() - 1);
                                    if (Math.abs(lastValueInSet - problemPixel) <= strideX) {
                                        currentSetOfPixels.add(problemPixel);
                                    } else {
                                        // Dispatch the current set of pixels
                                        groupsOfAdjacentRowsGroupedProblemPixels.get(thisProblemRowY).add(currentSetOfPixels);
                                        // And flush...
                                        currentSetOfPixels = new ArrayList<>();
                                    }
                                }
                            }
                            if (!currentSetOfPixels.isEmpty()) {
                                groupsOfAdjacentRowsGroupedProblemPixels.get(thisProblemRowY).add(currentSetOfPixels);
                            }
                        }

                        // Filter out the sets of problem pixels that are larger than half the width of the frame.
                        //
                        // Note: We're creating an assumption here that visual elements existing in whitespace should not
                        // take up more than half the page - this assumption fails for content like carousels, which
                        // exist in whitespace.
                        //
                        // Also filter out visual elements that touch the edges of the frame's X axis
                        //
                        for (Integer thisProblemRowY : groupsOfAdjacentRowsGroupedProblemPixels.keySet()) {
                            groupsOfAdjacentRowsGroupedProblemPixels.put(thisProblemRowY, groupsOfAdjacentRowsGroupedProblemPixels.get(thisProblemRowY).stream()
                                    .filter(thisSetOfPixels -> Math.abs(thisSetOfPixels.get(0) - thisSetOfPixels.get(thisSetOfPixels.size() - 1)) < (w / 2f))
                                    .filter(thisSetOfPixels -> thisSetOfPixels.get(0) != 0 && thisSetOfPixels.get(thisSetOfPixels.size() - 1) != (w - (w % strideX))).collect(Collectors.toList()));
                        }
                        // If a visual element exists in more than 1/4th of y axis, it's not a mere button or pictogram
                        // and shouldn't be removed
                        if ((groupsOfAdjacentRowsGroupedProblemPixels.keySet().size() * strideY) <= (h / 4f)) {
                            for (Integer yyy : groupsOfAdjacentRowsGroupedProblemPixels.keySet()) {
                                // Note: We don't need to worry about overriding anything, as this process is done consecutively, and so there is no overlap.
                                List<Integer> commonPixels = new ArrayList<>();
                                for (List<Integer> pixels : groupsOfAdjacentRowsGroupedProblemPixels.get(yyy)) {
                                    commonPixels = Stream.concat(commonPixels.stream(), pixels.stream()).collect(Collectors.toList());
                                }
                                pixelsInWhitespaceThatAreVisualElements.get(thisMode).put(yyy, commonPixels);
                            }
                        }

                        // Note: There is no minimum size for a visual element, and this opens up the possibility of diagonal content
                        // being misclassified as whitespace
                        // Relinquish the current row
                        thisGroupOfAdjacentRows.put(thisMode, new ArrayList<>());
                    }
                }
            }
        }

        // Repeat the sampling process, however this time accounting for obviously visual elements
        for (String thisMode : Arrays.asList("light", "dark")) {
            Integer thisWhitespaceColour = (thisMode.equals("light")) ? absoluteWhitespaceColourLight : absoluteWhitespaceColourDark;
            for (int yy = 0; yy < h; yy += strideY) {
                List<Double> thisRow = new ArrayList<>();
                for (int xx = 0; xx < w; xx += strideX) {
                    if ((!pixelsInWhitespaceThatAreVisualElements.get(thisMode).containsKey(yy))
                            || (!pixelsInWhitespaceThatAreVisualElements.get(thisMode).get(yy).contains(xx))) {
                        Double thisPixelDifference = pixelDifferencePercentage(thisWhitespaceColour, thisFrame.getPixel(xx, yy));
                        thisRow.add(thisPixelDifference);
                    }
                }
                // Determine the average colour of this row
                Double averageColourDifferenceOfThisRow = optionalGetDouble(thisRow.stream().mapToDouble(x -> x).average());
                whitespaceRows.get(thisMode).put(yy, (averageColourDifferenceOfThisRow <= thresholdOfWhitespaceSimilarityAtRow));
                whitespaceRowsPcts.get(thisMode).put(yy, averageColourDifferenceOfThisRow);
            }
        }
        /*
        if (frameAnnotation == 50) {
            for (Integer yy : whitespaceRows.get("dark").keySet().stream().sorted().collect(Collectors.toList())) {
                System.out.println(yy);
                System.out.println("\t\t" + whitespaceRows.get("dark").get(yy));
                System.out.println("\t\t" + whitespaceRowsPcts.get("dark").get(yy));
                System.out.println("\t\t" + dissimilarPixels.get("dark").get(yy));
            }
            System.out.println("####################################");
            System.out.println("####################################");
            System.out.println("####################################");
        }*/

        try {
            // At least 1/5th of the page should be whitespace
            // TODO - potentially set this to 1/6th
            for (String thisMode : Arrays.asList("light", "dark")) {
                Double totalPercentageWhitespace = ((whitespaceRows.get(thisMode).keySet().stream()
                        .filter(x -> whitespaceRows.get(thisMode).get(x)).collect(Collectors.toList()).size()) / (double) whitespaceRows.get(thisMode).keySet().size());
                Boolean totalPercentageWhitespaceCorrect = (totalPercentageWhitespace > (1 / 7f));

                //System.out.println("totalPercentageWhitespace: "+ totalPercentageWhitespace);

                // All disjoint non-whitespace areas need to not exceed 3/4s of the frame height
                List<List<Integer>> nonWhitespaceRanges = new ArrayList<>();
                List<Integer> currentRange = new ArrayList<>();
                for (int yy = 0; yy < h; yy += strideY) {
                    if (!whitespaceRows.get(thisMode).get(yy)) {
                        if (currentRange.isEmpty()) {
                            currentRange.add(yy);
                        }
                    } else {
                        if (!currentRange.isEmpty()) {
                            currentRange.add(yy);
                            nonWhitespaceRanges.add(currentRange);
                            currentRange = new ArrayList<>();
                        }
                    }
                }
                if (!currentRange.isEmpty()) {
                    currentRange.add(h);
                    nonWhitespaceRanges.add(currentRange);
                }

                // A further condition of determining whether a frame is whitespace is whether
                // the non-whitespace content piles into certain regions of the frame.
                // Ideally, we should observe that the majority of the non-whitespace content
                // is within a single, two, or even three equally spaced distributions that are
                // divided by whitespace on the screen. This is tested below.
                Double distanceBetweenAnyTwoDistributions = h * 1.0;
                Double halfSizeOfDistribution = (h * 0.25); // TODO - this may differ on different aspect ratios
                Integer halfSizeOfDistributionAsInteger = (int) Math.round(halfSizeOfDistribution);
                Boolean disjointNonWhitespaceIsReasonablySized = nonWhitespaceRanges.stream().filter(x -> Math.abs(x.get(0) - x.get(1)) > (h / 4f * 3)).collect(Collectors.toList()).isEmpty();

                // At least half of all non-whitespace content should be piled into a single part of the image
                // that is half the size of the canvas
                Boolean maxCapturedInSlidingWindowAdequate = false;
                Double maxCapturedInSlidingWindowPercentage = 0.0;
                //if (totalPercentageWhitespaceCorrect && disjointNonWhitespaceIsReasonablySized) { // TODO -reinstate
                    for (int i = (0 - h); i < (h + h); i ++) {
                        int finalI = i;

                        Integer capturedN = (whitespaceRows.get(thisMode).keySet().stream()
                                .filter(x -> ((whitespaceRows.get(thisMode).get(x))
                                        && (x > (finalI - halfSizeOfDistribution))
                                        && (x < (finalI + halfSizeOfDistribution))))
                                .collect(Collectors.toList()).size() +
                                    whitespaceRows.get(thisMode).keySet().stream()
                                        .filter(x -> ((whitespaceRows.get(thisMode).get(x))
                                                && (x > ((finalI + distanceBetweenAnyTwoDistributions) - halfSizeOfDistribution))
                                                && (x < ((finalI + distanceBetweenAnyTwoDistributions) + halfSizeOfDistribution))))
                                        .collect(Collectors.toList()).size() +
                                whitespaceRows.get(thisMode).keySet().stream()
                                        .filter(x -> ((whitespaceRows.get(thisMode).get(x))
                                                && (x > ((finalI + (2*distanceBetweenAnyTwoDistributions)) - halfSizeOfDistribution))
                                                && (x < ((finalI + (2*distanceBetweenAnyTwoDistributions)) + halfSizeOfDistribution))))
                                        .collect(Collectors.toList()).size());

                        //Double thisMaxCapturedInSlidingWindow = (capturedN / (double) whitespaceRows.get(thisMode).keySet().size());



                        Double thisMaxCapturedInSlidingWindow = 0.0;
                        try {
                            Double totalWhitespaceOnRow = ((double) whitespaceRows.get(thisMode).keySet().stream()
                                    .filter(z ->  (whitespaceRows.get(thisMode).get(z))).collect(Collectors.toList()).size());
                            thisMaxCapturedInSlidingWindow = (totalWhitespaceOnRow == 0) ? 0.0 : (capturedN / totalWhitespaceOnRow);
                        } catch (Exception e) {  }

                        if (thisMaxCapturedInSlidingWindow > maxCapturedInSlidingWindowPercentage) {
                            maxCapturedInSlidingWindowPercentage = thisMaxCapturedInSlidingWindow;
                        }
                    }
                    maxCapturedInSlidingWindowAdequate = (maxCapturedInSlidingWindowPercentage > 0.7);
                //}

                JSONObject outcome = (JSONObject) statistics.get(thisMode);
                outcome.put("maxCapturedInSlidingWindowPercentage", maxCapturedInSlidingWindowPercentage);
                outcome.put("maxCapturedInSlidingWindowAdequate", maxCapturedInSlidingWindowAdequate);
                //outcome.put("whitespaceRows", whitespaceRows.get(thisMode));
                outcome.put("totalPercentageWhitespace", totalPercentageWhitespace);
                outcome.put("totalPercentageWhitespaceCorrect", totalPercentageWhitespaceCorrect);
                outcome.put("disjointNonWhitespaceIsReasonablySized", disjointNonWhitespaceIsReasonablySized);
                outcome.put("isFrameInFacebook",
                        (totalPercentageWhitespaceCorrect && disjointNonWhitespaceIsReasonablySized && maxCapturedInSlidingWindowAdequate));
            }
            return statistics;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statistics;
    }

    @Test
    public void testProbabilityFrameWithinFacebook() {
        File commonFolder = (new File(String.valueOf(filePath(asList(((new File(".")).getAbsolutePath()),
                "src", "debug", "assets", "local", "contentInterpreterSimulations", "NEW", "facebook_dark_sd_large_long_inapp_2")))));

        Bitmap thisBitmap = BitmapFactory.decodeFile((new File(commonFolder, "thisBitmap.png")).getAbsolutePath());
        //System.out.println("frameWithinFacebook: " + frameWithinFacebook(thisBitmap, false));
    }
    public static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : Objects.requireNonNull(fileOrDirectory.listFiles()))
                deleteRecursive(child);

        fileOrDirectory.delete();
    }
    public static void createDirectory(File folderToCreate, Boolean repopulate) {
        if (repopulate) {
            deleteRecursive(folderToCreate);
        }
        try {
            // Create it if it doesn't exist
            if (!folderToCreate.exists()) {
                Files.createDirectories(Paths.get(folderToCreate.getAbsolutePath()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Integer getDurationOfVideoInMicroseconds(File thisVideoFile) {
        // Retrieve the total duration of the video
        Integer thisValue = null;
        try {
            FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(thisVideoFile);
            frameGrabber.setFormat("mp4");
            frameGrabber.start();
            thisValue = Math.toIntExact(frameGrabber.getLengthInTime());
            frameGrabber.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return thisValue;
    }
    public static Integer getDurationOfVideoInFrames(File thisVideoFile) {
        // Retrieve the total duration of the video
        Integer thisValue = null;
        try {
            FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(thisVideoFile);
            frameGrabber.setFormat("mp4");
            frameGrabber.start();
            thisValue = Math.toIntExact(frameGrabber.getLengthInVideoFrames());
            frameGrabber.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return thisValue;
    }

    public static void writeToJSON(File outputFile, Object content) {
        try {
            PrintWriter writer = new PrintWriter(outputFile, "UTF-8");
            if (content instanceof JSONObject) {
                writer.println(((JSONObject) content).toString(3));
            } else {
                writer.println((new ObjectMapper().writer().withDefaultPrettyPrinter()).writeValueAsString(content));
            }
            writer.close();
        } catch (Exception e) { }

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
    private static JSONObject generateQuickReading(File thisScreenRecordingFile, Boolean generateTestContent) {
        // Determine the test name
        String thisTestName = Arrays.stream(thisScreenRecordingFile.getName().split("\\.")).collect(Collectors.toList()).get(0);
        // (Re)Create this test's folder, deleting any prior version of the folder
        File thisTestFolder = new File(testsFolder, thisTestName);
        File readingsFolder = new File(thisTestFolder, "sampleIdentifyingFacebook");
        if (generateTestContent) {
            createDirectory(readingsFolder, true);
        }
        // Take an initial reading, as a number of frames
        Double startingSampleSize = 5.0; // TODO - shift to constant
        Integer videoScale = 1; // TODO - shift to constant
        Integer minWidth = 500; // TODO - shift to constant
        Integer videoFrames = getDurationOfVideoInFrames(thisScreenRecordingFile);
        Integer interval = Math.toIntExact(Math.round(videoFrames / startingSampleSize));
        JSONObject frameStatistics = new JSONObject();
        // NB: The sample does not get the 'absolute' last frame - although it is negligible for the quick reading
        // since it retrieves a frame that is technically close to the last frame.
        Integer nFrames = 0;
        for (int f = 0; f < videoFrames; f += interval) {
            Bitmap thisBitmap = getMP4At(thisScreenRecordingFile, null, f, videoScale, minWidth);
            assert thisBitmap != null;
            JSONObject thisFrameStatistic = frameWithinFacebook(thisBitmap,f);
            if (generateTestContent) {
                saveBitmap(thisBitmap, new File(readingsFolder, "frame-" + f + ".jpg").getAbsolutePath());
                try {
                    frameStatistics.put(String.valueOf(f), thisFrameStatistic);
                } catch (Exception e) {}
            }
            nFrames ++;
        }
        JSONObject frameStatisticsSummary = new JSONObject();
        Integer nInFacebook = 0;
        try {
            for (Iterator<String> it = frameStatistics.keys(); it.hasNext(); ) {
                String thisFrameAsString = it.next();
                Boolean isFrameInFacebook =
                        ((Boolean) (((JSONObject) ((JSONObject) frameStatistics.get(thisFrameAsString)).get("light")).get("isFrameInFacebook"))
                            || ((Boolean) ((JSONObject) ((JSONObject) frameStatistics.get(thisFrameAsString)).get("dark")).get("isFrameInFacebook")));
                frameStatisticsSummary.put(thisFrameAsString, isFrameInFacebook);
                nInFacebook += (isFrameInFacebook ? 1 : 0 );
            }
        } catch (Exception e) {}
        Double percentageOfSampleRecordedInFacebook = (nInFacebook/ (double) nFrames);
        JSONObject statistics = new JSONObject();
        JSONObject statisticsSummary = new JSONObject();
        JSONObject output = new JSONObject();
        Double percentageOfSampleThatShouldQualifyVideoAsFacebook = 0.5; // TODO - shift to constant
        try {
            Boolean isFacebook = (percentageOfSampleRecordedInFacebook >= percentageOfSampleThatShouldQualifyVideoAsFacebook);
            if (generateTestContent) {
                Boolean thisSuccess;
                if (Arrays.stream(thisTestName.split("_")).collect(Collectors.toList()).get(0).equals("facebook")) {
                    thisSuccess = isFacebook;
                } else {
                    thisSuccess = (percentageOfSampleRecordedInFacebook == 0.0);
                }
                statisticsSummary.put("success", thisSuccess);
                statisticsSummary.put("percentageOfSampleRecordedInFacebook", percentageOfSampleRecordedInFacebook);
            }
            statistics.put("frameStatistics", frameStatistics);
            statistics.put("frameStatisticsSummary", frameStatisticsSummary);
            statistics.put("isFacebook", isFacebook);
            output.put("statistics", statistics);
            output.put("statisticsSummary", statisticsSummary);
        } catch (Exception e) { }
        if (generateTestContent) {
            writeToJSON(new File(readingsFolder, "metadata.json"), statistics);
        }
        return output;
    }

    /*
    *
    * Known issues:
    *
    * * Tests "appswitch_light_hq_large_fast_exapp_nonav_1" and "appswitch_light_hq_large_fast_exapp_nonav_2"
    * cannot be filtered out as they contain 'app-switching', which is known to mimic the same height ratios
    * as Facebook content. // TODO - build a fix for this
    *
    * * In one of the two Google Chome tests ("chrome_dark_hq_large_fast_exapp_nonav_1"), a few frames (in
    * which the keyboard appears) get misclassified as Facebook content. It is suspected that the cause of this
    * is that the keyboard gets interpreted as imagery. Paired with the background colour of Google Chrome, the
    * content mimics Facebook's UI design. Fortunately, this can be resolved by upping the cutoff of the number
    * of frames to qualify the video as being 'of' Facebook, given that keyboards are only intermittently activated.
    * At this stage, the error will be treated as negligible, however we are interested to determine how the
    * issue pans out.
    *
    * * In one Facebook test ("facebook_light_hq_large_slow_exapp_nonav_1"), no frames are captured. On further
    * inspection, it is determined that only two of the five sampled frames are of Facebook, and both are malformed
    * (one loads content - the other does not contain a tangible post). At this stage, we will treat this test as
    * negligible.
    *
    * * In the test "outlook_light_hq_large_fast_exapp_nav_1", one frame is determined to be misclassified as Facebook.
    * While this can be rectified with a frame cutoff for qualification as Facebook content, we note that the
    * mis-classification happens on the home screen of the device. Although this is concerning (as the home screen is
    * frequented), we will treat it as negligible at this stage, considering that numerous other 'home screen' frames
    * have been disqualified (see "appswitch_light_hq_large_fast_exapp_nonav_2" for example).
    *
    * * It is known that certain apps with similar UI designs are cross-captured by the function. These are given by
    * the following respective tests that give said indication:
    *       * snapchat_light_hq_large_slow_inapp_nonav_1 : 83% match
    *       * instagram_light_hq_large_slow_exapp_nav_1 : 40% match
    *       * instagram_light_hq_large_slow_exapp_nav_2 : 60% match
    *       * instagram_light_hq_large_slow_inapp_nav_1 : 83% match
    *       * linkedin_light_hq_large_slow_inapp_nav_1 : 40% match
    *       * linkedin_light_hq_large_slow_inapp_nav_2 : 40% match
    *       * reddit_light_hq_large_fast_inapp_nav_1 : 20% match
    *       * reddit_light_hq_large_fast_inapp_nav_2 : 80% match
    *       * reddit_light_hq_large_fast_inapp_nav_3 : 80% match
    * Determining that certain apps within this set may be relevant to future data acquisition processes, the cross-capture
    * will be temporarily deemed negligible // TODO - build cross-capture fix, or classifier to determine what is not Facebook
    *
    *
    * */
    @Test
    public void testInterpreter() {
        // Declare the tests for the interpreter
        List<String> tests = Arrays.asList(
                "appswitch_dark_hq_large_fast_exapp_nonav_3",
                "appswitch_dark_hq_large_fast_exapp_nonav_4",
                "appswitch_dark_hq_large_fast_exapp_nonav_5",
                "appswitch_light_hq_large_fast_exapp_nonav_1",
                "appswitch_light_hq_large_fast_exapp_nonav_2",
                "chrome_dark_hq_large_fast_exapp_nonav_1",
                "chrome_light_hq_large_fast_inapp_nonav_1",
                "facebook_dark_hq_large_slow_inapp_nav_1",
                "facebook_dark_hq_large_slow_inapp_nav_2",
                "facebook_dark_hq_large_slow_inapp_nonav_1",
                "facebook_dark_lq_large_slow_exapp_nav_1",
                "facebook_dark_lq_large_slow_inapp_nav_1",
                "facebook_dark_lq_large_slow_inapp_nav_2",
                "facebook_dark_lq_large_slow_inapp_nav_3",
                "facebook_light_hq_large_quick_exapp_nav_1",
                "facebook_light_hq_large_quick_inapp_nav_1",
                "facebook_light_hq_large_quick_inapp_nonav_1",
                "facebook_light_hq_large_quick_inapp_nonav_2",
                "facebook_light_hq_large_quick_inapp_nonav_3",
                "facebook_light_hq_large_slow_exapp_nonav_1",
                "facebook_light_hq_large_slow_inapp_nonav_1",
                "facebook_light_hq_small_slow_inapp_nav_1",
                "facebook_light_hq_small_slow_inapp_nav_2",
                "instagram_light_hq_large_slow_exapp_nav_1",
                "instagram_light_hq_large_slow_exapp_nav_2",
                "instagram_light_hq_large_slow_inapp_nav_1",
                "linkedin_light_hq_large_fast_exapp_nonav_1",
                "linkedin_light_hq_large_slow_inapp_nav_1",
                "linkedin_light_hq_large_slow_inapp_nav_2",
                "messenger_dark_hq_large_fast_exapp_nonav_1",
                "outlook_dark_hq_large_slow_inapp_nav_1",
                "outlook_light_hq_large_fast_exapp_nav_1",
                "reddit_light_hq_large_fast_inapp_nav_1",
                "reddit_light_hq_large_fast_inapp_nav_2",
                "reddit_light_hq_large_fast_inapp_nav_3",
                "slack_dark_hq_large_fast_exapp_nonav_1",
                "snapchat_light_hq_large_slow_inapp_nonav_1"
        );

        // Create the global tests folder if it doesn't exist
        createDirectory(testsFolder, false);

        // Keep track of the tests via a global data-structure
        JSONObject statisticsForAll = new JSONObject();

        // For each test...
        for (String thisTestName : tests) {
            System.out.println(thisTestName);

            // Load in the screen recording file
            File thisScreenRecordingFile = new File(screenRecordingsFolder, thisTestName + ".mp4");
            assert (thisScreenRecordingFile.exists());

            // Generate a summary of its outputs
            JSONObject thisTestSummaryOutput = new JSONObject();

            // Step 1 : sampleIdentifyingFacebook : Generate the quick reading
            try {
                JSONObject outputOfQuickReading = generateQuickReading(thisScreenRecordingFile, true);
                JSONObject statisticsSummary = (JSONObject) outputOfQuickReading.get("statisticsSummary");
                thisTestSummaryOutput.put("sampleIdentifyingFacebook", statisticsSummary);
                statisticsForAll.put(thisTestName, thisTestSummaryOutput);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Step 2 :

            // TODO - if it is Facebook, go in and attempt to join the content together

            /*
            *
            *
            * of the five initial frames, determine areas of analysis eg. all frames until frame 3 are good
            * so go ahead and analyse all frames after frame 3, or start at frame 3 and go onwards
            *
            * start with a large interval, and going down into smaller intervals only if necessary
            *
            * when analysing frames, cross-check their similarity - if they differ by a certain threshold,
            * we need to go back and insert frames between their interval, until there is a smooth difference
            * between them
            *
            * then begin the rest of the process
            *
            *
            *
            *
            * */
        }

        writeToJSON(new File(testsFolder, "metadata.json"), statisticsForAll);
    }

    @Test
    public void testAlt() throws IOException, JSONException {



        // determine dividers and media boundaries for all screenshots

        // Load in the folder

        String thisTest = "nonfacebook_dark_hq_large_fast_exapp_nonav_4";//"test_fb_sd_long_1_fb_only"; //   test_fb_hq_quick_part

        File commonFolder = (new File(String.valueOf(filePath(asList(((new File(".")).getAbsolutePath()),
                "src", "debug", "assets", "local", "contentInterpreterSimulations", "NEW", thisTest)))));


        // Create the output folder (if it doesn't exist)
        File thisOutputFolder = (new File( commonFolder, "output"));
        Files.createDirectories(Paths.get(thisOutputFolder.getAbsolutePath()));




        /*
        Bitmap bitMap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        bitMap = bitMap.copy(bitMap.getConfig(), true);
        Canvas canvas = new Canvas(bitMap);
        canvas.setBitmap(bitMap);

        Paint paint2 = new Paint();
        paint2.setColor(Color.WHITE);
        paint2.setStyle(Paint.Style.FILL);
        canvas.drawPaint(paint2);

        String fname = (new File(commonFolder, "this.png")).getAbsolutePath();
        try {
            bitMap.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(fname));
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        //saveBitmap(bitMap, );


        File thisInputFolder = (new File( commonFolder, "input"));
        // Load in a synthetic data-structure
        File[] files = thisInputFolder.listFiles();

        assert (files != null);


        // Generate the chunks to analyze
        List<List<File>> chunks = chunksToAnalyze(files);

        int magicN = files.length;

        // TODO - cross-check our whitespace determination functions
        /*
        * you take a given frame
        *
        * does it have consistent spaces of whitespace (in a common shade of fb colour) split by non-consistent imagery
        *
        *
        *
        *
        * */

        // TODO - we need numerous tests to validate that it can differentiate facebook from everyhting else

        // TODO - how are we dealing with fast moving content - can we implement a frame staggerer that attempts to resample frames when they cannot be joined together
        // TODO - when we start doing frame staggering, we're going to need a solution for local FFMPEG frame grabbing

        Integer thisChunkNumber = 0;
        for (List<File> thisChunk : chunks) {
            thisChunkNumber ++;
            // STEP0 : Determine what part of this chunk is within Facebook (if at all)

            // STEP1 : Generate the chunk datastructure
            JSONObject dataStructure = chunkDataStructure(thisChunk);
            dataStructure.put("fnames", thisChunk.stream().map(x -> x.getName()).collect(Collectors.toList()));

            // STEP2 : Match the frames within the chunk on offsets
            dataStructure.put("offsetsChainMap", matchFramesOnOffsets(dataStructure));

            // STEP3 : Generate an offset chain map between the various frames
            dataStructure.put("offsetChains", expandOffsetChainsMap(dataStructure));

            // STEP4 : Project the offset chains into a super OC, generate a global part map, and derive the boundaries
            deriveBoundaries(dataStructure, thisOutputFolder, thisChunkNumber);
        }

        // STEP1 : Generate the chunk datastructure
        // Create a datastructure

        // TODO - determining that we are in facebook has to come before all else

        //System.out.println( (new ObjectMapper().writer().withDefaultPrettyPrinter()).writeValueAsString(screenshotMediaBoundaries));


        //System.out.println((new ObjectMapper().writer().withDefaultPrettyPrinter()).writeValueAsString(screenshotDividers));






        // STEP4 : Project the offset chains into a super OC, generate a global part map, and derive the boundaries







        // with retainedFeaturesGlobal, we need a decision process for determining what is 'true', and what isnt
        // possibly looking at max and mins for each frame, and determining if there is a contrast relative to the frame

        // todo - how much processing is undertaken on false positives - how do we guard against it


        // finally, each true divider is examined (where applicable) to determine if the sponsored text resides thereafter it
        // where it does, the ad stitch is constructed
    }

    @Test
    public void dansMediaBoundariesTest() {
        File dansTestFolder = new File(simulationsFolder, "danstest");
        File testFile = new File(dansTestFolder, "frame-240.jpg");
        Bitmap thisBitmap = BitmapFactory.decodeFile(testFile.getAbsolutePath());

        System.out.println("Created");

        // Generate Y axis statistics about bitmap
        JSONObject statistics = generateScreenshotStatistics(thisBitmap, false);
        System.out.println("Statistics generated");
        printJSON(statistics);
        //List<Integer> dividersFoundInScreenshot = findDividersInScreenshot(thisBitmap, statistics, false);

        // Locate whitespace
        HashMap<Integer, String> mediaBoundaries = findMediaBoundaries(thisBitmap, statistics);

        // Print result
        printJSON(mediaBoundaries);
    }
}
