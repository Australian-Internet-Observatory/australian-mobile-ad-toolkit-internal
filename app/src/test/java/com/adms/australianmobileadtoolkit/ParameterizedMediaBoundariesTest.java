package com.adms.australianmobileadtoolkit;

import static com.adms.australianmobileadtoolkit.Arguments.A;
import static com.adms.australianmobileadtoolkit.Arguments.Args;
import static com.adms.australianmobileadtoolkit.Common.optionalGetDouble;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.colourPalette;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.colourToHex;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.pixelDifferencePercentage;
import static com.adms.australianmobileadtoolkit.updateTest.averageColours;
import static com.adms.australianmobileadtoolkit.updateTest.whitespacePixelFromImage;

import static org.robolectric.Shadows.shadowOf;
import static java.util.Arrays.asList;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import com.adms.australianmobileadtoolkit.utils.Guards;
import com.adms.australianmobileadtoolkit.utils.ImageAnalyser;
import com.adms.australianmobileadtoolkit.utils.ImageAnnotator;
import com.adms.australianmobileadtoolkit.utils.ImageProcessor;
import com.adms.australianmobileadtoolkit.utils.ImageTransformer;
import com.adms.australianmobileadtoolkit.utils.Orientation;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowCanvas;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RunWith(ParameterizedRobolectricTestRunner.class)
@Config(sdk = {33})
public final class ParameterizedMediaBoundariesTest {
    private final String frame;
    private final String extension;
    private final File testFile;
    private final File resultFolder;
    private static final File simulationsFolder = (
            new File(String.valueOf(filePath(asList(((new File("")).getAbsolutePath()),
                    "src", "test", "res", "simulations")))));

    public ParameterizedMediaBoundariesTest(String frame, String extension) throws FileNotFoundException {
        this.frame = frame;
        this.extension = extension;
        // Prepare the test file and result folder
        String fileName = frame + "." + extension;
        testFile = new File(simulationsFolder, fileName);
        resultFolder = new File(simulationsFolder, frame + "-results");
        if (!resultFolder.exists()) {
            resultFolder.mkdirs();
        }
        Guards.ensureFileExists(testFile);
    }

    @ParameterizedRobolectricTestRunner.Parameters(name = "Test {index}: {0}.{1}")
    public static Collection getTestData() {
        Object[][] frames = new Object[][] {
                { "84", "png "},
                { "504", "png "},
                { "518", "png "},
                { "frame-0", "jpg" },
//                { "frame-24", "jpg" },
//                { "frame-48", "jpg" },
//                { "frame-72", "jpg" },
//                { "frame-96", "jpg" },
                { "frame-120", "jpg" },
//                { "frame-144", "jpg" },
//                { "frame-168", "jpg" },
//                { "frame-192", "jpg" },
//                { "frame-216", "jpg" },
                { "frame-240", "jpg" },
//                { "frame-264", "jpg" },
//                { "frame-288", "jpg" },
//                { "frame-312", "jpg" },
                { "frame-321", "jpg" },
//                { "frame-336", "jpg" },
//                { "frame-360", "jpg" },
//                { "frame-384", "jpg" },
//                { "frame-408", "jpg" },
//                { "frame-432", "jpg" },
//                { "frame-456", "jpg" },
                { "frame-480", "jpg" },
//                { "frame-504", "jpg" },
//                { "frame-528", "jpg" },
//                { "frame-552", "jpg" },
//                { "frame-576", "jpg" },
//                { "frame-600", "jpg" },
//                { "frame-624", "jpg" },
//                { "frame-648", "jpg" },
//                { "frame-672", "jpg" },
//                { "frame-696", "jpg" },
                { "frame-720", "jpg" },
        };
        return Arrays.asList(frames);
    }

    // ----------------------------- START OF TEST DECLARATIONS -----------------------------

    @Test
    public void updatedEdgeDetectionTest() {
        ImageAnalyser imageAnalyser = new ImageAnalyser(testFile);
        imageAnalyser.setLogDirectory(resultFolder.getAbsolutePath());
        List<Integer> horizontalEdges = imageAnalyser.getHorizontalEdges();

        // Annotate the image with the horizontal edges
        new ImageAnnotator(testFile)
                .drawLines(Orientation.HORIZONTAL, horizontalEdges, 3, Color.GREEN)
                .save(new File(resultFolder, "updated-edges.jpg"));
    }

    @Test
    public void originalEdgeDetectionTest() {
        Bitmap thisBitmap = BitmapFactory.decodeFile(testFile.getAbsolutePath());

        // Generate Y axis statistics about bitmap
        JSONObject statistics = generateScreenshotStatistics(thisBitmap, false);

        // Locate whitespace
        HashMap<Integer, String> mediaBoundaries = findMediaBoundaries(thisBitmap, statistics);

        // Annotate the media boundaries on the image
        ImageAnnotator annotator = new ImageAnnotator(testFile);
        annotator
                .drawLines(Orientation.HORIZONTAL, new ArrayList<>(mediaBoundaries.keySet()), 3, Color.GREEN);
        for (Map.Entry<Integer, String> entry : mediaBoundaries.entrySet()) {
            int y = entry.getKey();
            String label = entry.getValue();
            // If label is "whitespaceAbove", draw a rectangle above the media boundary
            if (label.equals("whitespaceAbove")) {
                annotator.drawRect(y - 10, 0, y - 3, thisBitmap.getWidth(), 3, Color.RED, false);
            }
            // If label is "whitespaceBelow", draw a rectangle below the media boundary
            else if (label.equals("whitespaceBelow")) {
                annotator.drawRect(y + 3, 0, y + 10, thisBitmap.getWidth(), 3, Color.RED, false);
            }
        }
        annotator.save(new File(resultFolder, "original-edges.jpg"));
    }

    // ----------------------------- END OF TEST DECLARATIONS -----------------------------
    // Below are the utility functions used by the tests

    public static File filePath(List<String> path) {
        File output = null;
        for (String s : path) {
            output = (path.indexOf(s) == 0) ? new File(s) : (new File(output, s));
        }
        return output;
    }

    public static JSONObject generateScreenshotStatistics(Bitmap thisBitmap, boolean verbose) {
        if (thisBitmap.getHeight() == 0 || thisBitmap.getWidth() == 0) throw new IllegalArgumentException("Bitmap width or height must be at least 1");


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

    private class Statistics {
        public int verticalStrideUnit;
        public HashMap<Integer, Double> ArrayThisAverageColourDifferenceToWhitespacePixelPercentage;
        public HashMap<Integer, Double> ArrayThisDominantColourDifferenceToWhitespacePixelPercentage;
        public HashMap<Integer, Double> ArraySpreadOfDominantColourPercentage;
        public HashMap<Integer, Double> ArrayFrequencyOfDominantColourPercentage;
        public HashMap<Integer, Double> ArrayMaxPixelAdjacency;
        public HashMap<Integer, Double> ArrayAverageOnEdge;
        public HashMap<Integer, Double> ArrayThisAverageColourRDifferenceToWhitespacePixelRPercentage;
        public HashMap<Integer, Double> ArrayThisAverageColourGDifferenceToWhitespacePixelGPercentage;
        public HashMap<Integer, Double> ArrayThisAverageColourBDifferenceToWhitespacePixelBPercentage;

        public Statistics(int verticalStrideUnit, HashMap<Integer, Double> ArrayThisAverageColourDifferenceToWhitespacePixelPercentage, HashMap<Integer, Double> ArrayThisDominantColourDifferenceToWhitespacePixelPercentage, HashMap<Integer, Double> ArraySpreadOfDominantColourPercentage, HashMap<Integer, Double> ArrayFrequencyOfDominantColourPercentage, HashMap<Integer, Double> ArrayMaxPixelAdjacency, HashMap<Integer, Double> ArrayAverageOnEdge, HashMap<Integer, Double> ArrayThisAverageColourRDifferenceToWhitespacePixelRPercentage, HashMap<Integer, Double> ArrayThisAverageColourGDifferenceToWhitespacePixelGPercentage, HashMap<Integer, Double> ArrayThisAverageColourBDifferenceToWhitespacePixelBPercentage) {
            this.verticalStrideUnit = verticalStrideUnit;
            this.ArrayThisAverageColourDifferenceToWhitespacePixelPercentage = ArrayThisAverageColourDifferenceToWhitespacePixelPercentage;
            this.ArrayThisDominantColourDifferenceToWhitespacePixelPercentage = ArrayThisDominantColourDifferenceToWhitespacePixelPercentage;
            this.ArraySpreadOfDominantColourPercentage = ArraySpreadOfDominantColourPercentage;
            this.ArrayFrequencyOfDominantColourPercentage = ArrayFrequencyOfDominantColourPercentage;
            this.ArrayMaxPixelAdjacency = ArrayMaxPixelAdjacency;
            this.ArrayAverageOnEdge = ArrayAverageOnEdge;
            this.ArrayThisAverageColourRDifferenceToWhitespacePixelRPercentage = ArrayThisAverageColourRDifferenceToWhitespacePixelRPercentage;
            this.ArrayThisAverageColourGDifferenceToWhitespacePixelGPercentage = ArrayThisAverageColourGDifferenceToWhitespacePixelGPercentage;
            this.ArrayThisAverageColourBDifferenceToWhitespacePixelBPercentage = ArrayThisAverageColourBDifferenceToWhitespacePixelBPercentage;
        }
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

            List<Integer> thisRangeB = IntStream.range(Math.max(0, yy - (verticalStrideUnit*offset)), yy)
                    .filter(x -> x % finalVerticalStrideUnit == 0)
                    .boxed()
                    .collect(Collectors.toList());
            List<Integer> thisRangeF = IntStream.range(yy, Math.min(thisBitmap.getHeight(), yy + (verticalStrideUnit*offset)))
                    .filter(x -> x % finalVerticalStrideUnit == 0)
                    .boxed()
                    .collect(Collectors.toList());

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

    public static void printJSON(Object thisToJSON) {
        try {
            System.out.println((new ObjectMapper().writer().withDefaultPrettyPrinter()).writeValueAsString(thisToJSON));
        } catch (Exception e) {}
    }

    public static void saveJSON(Object json, File file) {
        // Save the json object into the file given
        try {
            // Create the file if it does not exist
            if (!file.exists()) {
                file.createNewFile();
            }
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writer().withDefaultPrettyPrinter().writeValue(file, json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static double[][] getSharpeningKernel(int blurFactor, int sharpenFactor) {
        // sharpened = original + (original - blurred) * sharpenFactor
        return new double[][]{
            {0, -1f/blurFactor*sharpenFactor, 0},
            {-1f/blurFactor*sharpenFactor, 1 + (1 - 1f/blurFactor) * sharpenFactor, -1f/blurFactor*sharpenFactor},
            {0, -1f/blurFactor*sharpenFactor, 0}
        };
    }

//    @Test
    public void convolutionTest() throws FileNotFoundException {
        String fileName = frame + "." + extension;
        File testFile = new File(simulationsFolder, fileName);
        File resultFolder = new File(simulationsFolder, frame + "-results");
        if (!resultFolder.exists()) {
            resultFolder.mkdirs();
        }
        Guards.ensureFileExists(testFile);
        Bitmap thisBitmap = BitmapFactory.decodeFile(testFile.getAbsolutePath());
        System.out.println("Created bitmap");

        // Grayscale the image
        ImageTransformer grayscale = new ImageTransformer(thisBitmap);
        grayscale.grayscale();
        grayscale.save(new File(resultFolder, "grayscale.jpg"));

        // Sobel kernel for horizontal edge detection
        double[][] horizontalEdgeDetectionKernel = {
            {-1, -2, -1},
            {0, 0, 0},
            {1, 2, 1}
        };

        double[][] blurKernel = {
            {1/9f, 1/9f, 1/9f},
            {1/9f, 1/9f, 1/9f},
            {1/9f, 1/9f, 1/9f}
        };

        double[][] outlineKernel = {
            {-1, -1, -1},
            {-1, 8, -1},
            {-1, -1, -1}
        };

        double[][] sharpenKernel = getSharpeningKernel(5, 10);
        ImageTransformer sharpen = new ImageTransformer(grayscale.getBitmap());
        sharpen.convolve2d(sharpenKernel);
        sharpen.save(new File(resultFolder, "sharpened.jpg"));

        // Apply the kernel to the image
        ImageTransformer convolution = new ImageTransformer(sharpen.getBitmap());
        convolution.convolve2d(horizontalEdgeDetectionKernel);
        convolution.save(new File(resultFolder, "convolved.jpg"));

        // Aggregate the image along the X axis
        ImageTransformer aggregation = new ImageTransformer(convolution.getBitmap());
        Function<int[], Integer> average = pixels -> {
            int r = 0, g = 0, b = 0;
            for (int pixel : pixels) {
                r += (pixel >> 16) & 0xFF;
                g += (pixel >> 8) & 0xFF;
                b += pixel & 0xFF;
            }
            r /= pixels.length;
            g /= pixels.length;
            b /= pixels.length;
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        };
        Function<int[], Integer> median = pixels -> {
            Arrays.sort(pixels);
            return pixels[pixels.length / 2];
        };
        Function<int[], Integer> mode = pixels -> {
            HashMap<Integer, Integer> frequency = new HashMap<>();
            for (int pixel : pixels) {
                frequency.put(pixel, frequency.getOrDefault(pixel, 0) + 1);
            }
            return Collections.max(frequency.entrySet(), Map.Entry.comparingByValue()).getKey();
        };
        aggregation.aggregate(Orientation.HORIZONTAL, mode);
        aggregation.save(new File(resultFolder, "aggregated-average.jpg"));

        ImageTransformer transformer = new ImageTransformer(thisBitmap);
        transformer
                .grayscale()
                .convolve2d(horizontalEdgeDetectionKernel)
                .aggregate(Orientation.HORIZONTAL, mode)
                .save(new File(resultFolder, "aggregated-no-sharpen.jpg"));
    }

//    @Test
//    @Config(manifest = Config.NONE)
    public void edgeMatchingTest() throws FileNotFoundException {
        String fileName = frame + "." + extension;
        File testFile = new File(simulationsFolder, fileName);
        File resultFolder = new File(simulationsFolder, frame + "-results");
        if (!resultFolder.exists()) {
            resultFolder.mkdirs();
        }
        Guards.ensureFileExists(testFile);
        ImageTransformer transformer = new ImageTransformer(testFile);

        // https://homepages.inf.ed.ac.uk/rbf/HIPR2/gsmooth.htm
//        double[][] gaussianSmoothKernel = {
//                {1/273f, 4/273f, 7/273f, 4/273f, 1/273f},
//                {4/273f, 16/273f, 26/273f, 16/273f, 4/273f},
//                {7/273f, 26/273f, 41/273f, 26/273f, 7/273f},
//                {4/273f, 16/273f, 26/273f, 16/273f, 4/273f},
//                {1/273f, 4/273f, 7/273f, 4/273f, 1/273f}
//        };

        double[][] gaussianSmoothKernel = {
                {1/16f, 2/16f, 1/16f},
                {2/16f, 4/16f, 2/16f},
                {1/16f, 2/16f, 1/16f}
        };

        // 5x5 Sobel kernel for horizontal edge detection
//        double[][] horizontalEdgeDetectionKernel = {
//                {5, 8, 10, 8, 5},
//                {4, 10, 20, 10, 4},
//                {0, 0, 0, 0, 0},
//                {-4, -10, -20, -10, -4},
//                {-5, -8, -10, -8, -5}
//        };

        // Pre-witt kernels for horizontal edge detection => too noisy
//        double[][] horizontalEdgeDetectionKernel = {
//                {-4, -4, -4, -4, -4, -4, -4},
//                {-2, -2, -2, -2, -2, -2, -2},
//                {-1, -1, -1, -1, -1, -1, -1},
//                {0, 0, 0, 0, 0, 0, 0},
//                {1, 1, 1, 1, 1, 1, 1},
//                {2, 2, 2, 2, 2, 2, 2},
//                {4, 4, 4, 4, 4, 4, 4},
//        };

//        double[][] horizontalEdgeDetectionKernel = {
//                {2, 2, 2, 2, 2},
//                {1, 1, 1, 1, 1},
//                {0, 0, 0, 0, 0},
//                {-1, -1, -1, -1, -1},
//                {-2, -2, -2, -2, -2},
//        };
//
//        double[][] bottomHorizontalEdgeDetectionKernel = {
//                {-2, -2, -2, -2, -2},
//                {-1, -1, -1, -1, -1},
//                {0, 0, 0, 0, 0},
//                {1, 1, 1, 1, 1},
//                {2, 2, 2, 2, 2},
//        };
//
        double[][] horizontalEdgeDetectionKernel = {
                {1, 1, 1},
                {0, 0, 0},
                {-1, -1, -1},
        };

//        double[][] horizontalEdgeDetectionKernel = {
//                {-1, -1, -1},
//                {0, 0, 0},
//                {1, 1, 1},
//        };

        double[][] verticalEdgeDetectionKernel = {
                {1, 0, -1},
                {1, 0, -1},
                {1, 0, -1}
        };

        double[][] outlineKernel = {
                {-1, -1, -1},
                {-1, 8, -1},
                {-1, -1, -1}
        };

        Function<int[], Integer> average = pixels -> {
            int r = 0, g = 0, b = 0;
            for (int pixel : pixels) {
                r += (pixel >> 16) & 0xFF;
                g += (pixel >> 8) & 0xFF;
                b += pixel & 0xFF;
            }
            r /= pixels.length;
            g /= pixels.length;
            b /= pixels.length;
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        };

        Function<int[], Integer> mode = pixels -> {
            HashMap<Integer, Integer> frequency = new HashMap<>();
            for (int pixel : pixels) {
                frequency.put(pixel, frequency.getOrDefault(pixel, 0) + 1);
            }
            int color = Collections.max(frequency.entrySet(), Map.Entry.comparingByValue()).getKey();
            return color;
//            int frequencyOfColor = frequency.get(color);
//            return frequencyOfColor > pixels.length * 0.9 ? color : 0xFF000000;
//            // Find the weighted average of the top 3 most frequent pixels
//            int TOP_NUM = 3;
//            List<Map.Entry<Integer, Integer>> sorted = frequency.entrySet().stream()
//                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
//                    .collect(Collectors.toList());
//            if (sorted.size() > TOP_NUM) {
//                sorted = sorted.subList(0, TOP_NUM);
//            }
//            int total = sorted.stream().mapToInt(Map.Entry::getValue).sum();
//            int weightedAverage = 0;
//            for (int i = 0; i < TOP_NUM && i < sorted.size(); i++) {
//                Map.Entry<Integer, Integer> entry = sorted.get(i);
//                int gray = Color.red(entry.getKey());
//                weightedAverage += gray * entry.getValue();
//            }
//            weightedAverage /= total;
//            // Convert back to RGB
//            return 0xFF000000 | (weightedAverage << 16) | (weightedAverage << 8) | weightedAverage;
        };


        int POOL_SIZE = transformer.getBitmap().getWidth() / 250;

        transformer
//                .crop(startX, 0, endX - startX, transformer.getBitmap().getHeight())
                .averagePool(POOL_SIZE)
                .save(new File(resultFolder, "pooled.jpg"));

        List<Integer> samplePositions = IntStream.range(0, transformer.getBitmap().getWidth())
                .filter(x -> x % 10 == 0) // Sample every 10 pixels
                .boxed()
                .collect(Collectors.toList());

        transformer
                .sampleInDirection(Orientation.VERTICAL, samplePositions)
                .grayscale()
//                .quantize(16)
                .equaliseHistogram()
                .save(new File(resultFolder, "equalised.jpg"));

        transformer
                .convolve2d(horizontalEdgeDetectionKernel, true)
                .save(new File(resultFolder, "top-edges.jpg"));

        transformer
                .convolve2d(horizontalEdgeDetectionKernel, true)
                .save(new File(resultFolder, "median-filtered.jpg"));

        ImageTransformer verticalTransformer = transformer.copy()
                .convolve2d(verticalEdgeDetectionKernel, true)
                .invert();
        verticalTransformer.save(new File(resultFolder, "vertical-edges.jpg"));

        transformer
                .blendMultiply(verticalTransformer.getBitmap())
                .convolve2d(new double[][] {
                        {1/4f},
                        {1/2f},
                        {1/4f},
                })
                .convolve2d(horizontalEdgeDetectionKernel)
                .shift(0, -1) // to compensate for the shift in the vertical edge detection
                .quantize(64)
                .save(new File(resultFolder, "blended-edges.jpg"));

        Function<int[], Integer> variance = pixels -> {
            int sum = 0;
            for (int pixel : pixels) {
                sum += Color.red(pixel);
            }
            int mean = sum / pixels.length;
            int sumOfSquares = 0;
            for (int pixel : pixels) {
                int diff = Color.red(pixel) - mean;
                sumOfSquares += diff * diff;
            }
            int var = sumOfSquares / pixels.length;
            int std = (int) Math.sqrt(var);
            int stdAsColor = Math.min(std, 255);
            return 0xFF000000 | (stdAsColor << 16) | (stdAsColor << 8) | stdAsColor;
        };

        transformer
                .aggregate(Orientation.HORIZONTAL, pixels -> {
                    HashMap<Integer, Integer> frequency = new HashMap<>();
                    for (int pixel : pixels) {
                        frequency.put(pixel, frequency.getOrDefault(pixel, 0) + 1);
                    }
                    int firstOrderDiff = 0;
                    for (int i = 0; i < pixels.length - 1; i++) {
                        firstOrderDiff += Math.abs(Color.red(pixels[i]) - Color.red(pixels[i + 1]));
                    }
                    int firstOrderDiffAvg = firstOrderDiff / (pixels.length - 1);
                    firstOrderDiffAvg = Math.min(firstOrderDiffAvg, 255);
                    double diffMultiplier = (255 - firstOrderDiffAvg) / 255d;
                    int modeColorGray = Collections.max(frequency.entrySet(), Map.Entry.comparingByValue()).getKey();
                    return Color.rgb(
                            (int) (Color.red(modeColorGray) * diffMultiplier),
                            (int) (Color.green(modeColorGray) * diffMultiplier),
                            (int) (Color.blue(modeColorGray) * diffMultiplier)
                    );
                }, true)
                .save(new File(resultFolder, "aggregated-edges.jpg"));

        List<Double> column = Arrays.stream(transformer.getColumn(0))
                .map(Color::red) // Since image is grayscale, R = G = B
                .mapToObj(x -> x / 255d)
                .collect(Collectors.toList());

//        System.out.println(column);
        double EDGE_THRESHOLD = 0.33; // How much of the pixel is considered an edge
        int MAX_NEIGHBOURS = 10; // How many neighbours to add to the edge

        List<Integer> edgeLocations = new ArrayList<>();
        for (int i = 0; i < column.size(); i += MAX_NEIGHBOURS) {
            int goodNeighbours = 0;
            int firstGoodNeighbour = -1;
            // Check the next few pixels to see if they are also edges
            double augmentedValue = 0;
            for (int j = i; j < i + MAX_NEIGHBOURS && j < column.size(); j++) {
                if (column.get(j) < EDGE_THRESHOLD) continue;
                goodNeighbours++;
                augmentedValue += column.get(j);
                if (firstGoodNeighbour == -1) {
                    firstGoodNeighbour = j;
                }
            }
            augmentedValue /= goodNeighbours;
            if (augmentedValue >= EDGE_THRESHOLD) {
                // Multiply to get the location on original image
                edgeLocations.add(firstGoodNeighbour * POOL_SIZE);
            }
        }
//        System.out.println(edgeLocations);

        ImageTransformer origin = new ImageTransformer(testFile);

        // Ignore edges too close to the top or bottom
        int EDGE_MARGIN = 10;
        edgeLocations = edgeLocations.stream()
                .filter(x -> x > EDGE_MARGIN && x < origin.getBitmap().getHeight() - EDGE_MARGIN)
                .collect(Collectors.toList());

        ImageAnnotator annotator = new ImageAnnotator(testFile);
        for (int edgeLocation : edgeLocations) {
            annotator.drawLine(Orientation.HORIZONTAL, edgeLocation, 5, Color.GREEN);
        }
        annotator.save(new File(resultFolder, "unfiltered-new-boundaries.jpg"));

        // Boundary rows from full image (second analysis)
        // The some pixels above and below the edge
        int EDGE_PROBE_WIDTH = 2;
        List<Integer> rowsToTake = new ArrayList<>();
        for (int edgeLocation : edgeLocations) {
            for (int i = -EDGE_PROBE_WIDTH; i <= EDGE_PROBE_WIDTH; i++) {
                rowsToTake.add(edgeLocation + i);
            }
        }


        List<Integer> validRows = new ArrayList<>();
        int DIFF_THRESHOLD = 5;
        // Extract the bitmap of each row and some pixels above and below
        // and find the smallest first order difference, so as long as there is a
        // straight line, boundary is accepted
        // (should be low for actual boundaries)
        for (int row : edgeLocations) {
            int totalFirstOrderDiff = 0;
            for (int i = -EDGE_PROBE_WIDTH; i <= EDGE_PROBE_WIDTH; i++) {
                int y = row + i;
                if (y < 0 || y >= origin.getBitmap().getHeight()) continue;
                int[] pixels = Arrays.stream(origin.getRow(y))
                        .map(Color::red)
                        .toArray();
                int firstOrderDiff = 0;
                for (int j = 0; j < pixels.length - 1; j++) {
                    firstOrderDiff += Math.abs(pixels[j] - pixels[j + 1]);
                }
                firstOrderDiff /= pixels.length - 1;
                totalFirstOrderDiff += firstOrderDiff;
            }
            System.out.println(row + ": " + totalFirstOrderDiff);
            if (totalFirstOrderDiff < DIFF_THRESHOLD) {
                validRows.add(row);
            }
        }

        // Annotate the valid rows
        ImageAnnotator validRowsAnnotator = new ImageAnnotator(testFile);
        for (int validRow : validRows) {
            validRowsAnnotator.drawLine(Orientation.HORIZONTAL, validRow, 5, Color.GREEN);
        }
        validRowsAnnotator.save(new File(resultFolder, "new-boundaries.jpg"));

        ImageTransformer boundaryRows = new ImageTransformer(testFile);
        boundaryRows
                .sampleInDirection(Orientation.HORIZONTAL, rowsToTake)
                .grayscale()
                .save(new File(resultFolder, "boundary-rows.jpg"));
//
//        ImageAnnotator boundaryRowsAnnotator = new ImageAnnotator(boundaryRows.getBitmap());
//        for (int i = 0; i < boundaryRows.getBitmap().getHeight(); i += EDGE_PROBE_WIDTH * 2 + 1) {
//            boundaryRowsAnnotator.drawLine(Orientation.HORIZONTAL, i, 1, Color.RED);
//        }
//        boundaryRowsAnnotator.save(new File(resultFolder, "boundary-rows-annotated.jpg"));
//
//        boundaryRows
//                .equaliseHistogram()
//                .quantize(64)
//                .convolve2d(horizontalEdgeDetectionKernel, true)
//                .save(new File(resultFolder, "boundary-vertical-edges.jpg"));

        // Filter for edges with consistent colour
    }

//    @Test
//    @Config(manifest = Config.NONE)
    public void dansMediaBoundariesTest() throws FileNotFoundException, JSONException {
        String fileName = frame + "." + extension;
        File testFile = new File(simulationsFolder, fileName);
        File resultFolder = new File(simulationsFolder, frame + "-results");
        if (!resultFolder.exists()) {
            resultFolder.mkdirs();
        }
        Guards.ensureFileExists(testFile);
        Bitmap thisBitmap = BitmapFactory.decodeFile(testFile.getAbsolutePath());

        // Generate Y axis statistics about bitmap

        JSONObject statistics = generateScreenshotStatistics(thisBitmap, false);

        Statistics statisticsObj = new Statistics(
            (int) statistics.get("verticalStrideUnit"),
            (HashMap<Integer, Double>) statistics.get("ArrayThisAverageColourDifferenceToWhitespacePixelPercentage"),
            (HashMap<Integer, Double>) statistics.get("ArrayThisDominantColourDifferenceToWhitespacePixelPercentage"),
            (HashMap<Integer, Double>) statistics.get("ArraySpreadOfDominantColourPercentage"),
            (HashMap<Integer, Double>) statistics.get("ArrayFrequencyOfDominantColourPercentage"),
            (HashMap<Integer, Double>) statistics.get("ArrayMaxPixelAdjacency"),
            (HashMap<Integer, Double>) statistics.get("ArrayPixels"),
            (HashMap<Integer, Double>) statistics.get("ArrayThisAverageColourRDifferenceToWhitespacePixelRPercentage"),
            (HashMap<Integer, Double>) statistics.get("ArrayThisAverageColourGDifferenceToWhitespacePixelGPercentage"),
            (HashMap<Integer, Double>) statistics.get("ArrayThisAverageColourBDifferenceToWhitespacePixelBPercentage")
        );

//        printJSON((new ObjectMapper()).readValue(statistics, Map.class));
        //List<Integer> dividersFoundInScreenshot = findDividersInScreenshot(thisBitmap, statistics, false);
        saveJSON(statisticsObj, new File(resultFolder, "statistics.json"));

        // Locate whitespace
        HashMap<Integer, String> mediaBoundaries = findMediaBoundaries(thisBitmap, statistics);
        // Print result
//        printJSON(mediaBoundaries);
        // Convert the result to a JSON object
        JSONObject mediaBoundariesJson = new JSONObject();
        for (Integer key : mediaBoundaries.keySet()) {
            mediaBoundariesJson.put(key.toString(), mediaBoundaries.get(key));
        }
        // Save the result to a file
        saveJSON(mediaBoundaries, new File(resultFolder, "mediaBoundaries.json"));

        // Annotate the media boundaries on the image
        ImageAnnotator imageAnnotator = new ImageAnnotator(testFile);
        for (Integer key : mediaBoundaries.keySet()) {
            imageAnnotator.drawLine(Orientation.HORIZONTAL, key, 5, Color.GREEN);
        }
        imageAnnotator.save(new File(resultFolder, "original-boundaries.jpg"));

        Assert.assertTrue(true);
    }
}