package com.adms.australianmobileadtoolkit.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Used to perform image analysis to extract information from images
 * without modifying the original image
 */
public class ImageAnalyser extends ImageProcessor {
    public ImageAnalyser(Bitmap bitmap) {
        super(bitmap);
    }

    public ImageAnalyser(File originalImage) {
        super(originalImage);
    }

    /**
     * Get the most common color in the image, weighted by the color consistency
     * (how consistent two adjacent pixels are in terms of color)
     * @param pixels The pixels to get the weighted mode from
     * @return The mode color, weighted by the color consistency
     */
    private static Integer getWeightedModeByColorConsistency(int[] pixels) {
        HashMap<Integer, Integer> frequency = new HashMap<>();
        for (int pixel : pixels) {
            int currentCount = frequency.getOrDefault(pixel, 0);
            frequency.put(pixel, currentCount + 1);
        }
        int firstOrderDiff = 0;
        for (int i = 0; i < pixels.length - 1; i++) {
            firstOrderDiff += Math.abs(Color.red(pixels[i]) - Color.red(pixels[i + 1]));
        }
        int firstOrderDiffAvg = firstOrderDiff / (pixels.length - 1);
        firstOrderDiffAvg = Math.min(firstOrderDiffAvg, 255);
        double diffMultiplier = (255 - firstOrderDiffAvg) / 255d;
        int modeColor = Collections.max(frequency.entrySet(), Map.Entry.comparingByValue()).getKey();
        return Color.rgb(
                (int) (Color.red(modeColor) * diffMultiplier),
                (int) (Color.green(modeColor) * diffMultiplier),
                (int) (Color.blue(modeColor) * diffMultiplier)
        );
    }

    /**
     * Get the horizontal edges of the image as a list of y-coordinates (rows)
     * where the edges are located. Edges at the top and bottom of the image
     * are ignored to prevent false positives due to image borders.
     * @return The y-coordinates of the horizontal edges in the image
     */
    public List<Integer> getHorizontalEdges() {
        // The threshold for a weighted mode to be indicative of an edge
        double EDGE_THRESHOLD = 0.33;
        // The minimum spacing between edges
        int MIN_SPACING = 10;

        ImageTransformer transformer = new ImageTransformer(this.getBitmap());
        transformer.setLogDirectory(this.logDirectory);
        int POOL_SIZE = transformer.getBitmap().getWidth() / 250;
        transformer
                .averagePool(POOL_SIZE);

        List<Integer> samplePositions = IntStream.range(0, transformer.getBitmap().getWidth())
                .filter(x -> x % 10 == 0) // Sample every 10 pixels
                .boxed()
                .collect(Collectors.toList());

        transformer
                .sampleInDirection(Orientation.VERTICAL, samplePositions)
                .grayscale()
                .equaliseHistogram()
                // Apply edge detection twice to enhance the edges
                .convolve2d(ConvolutionKernels.EDGE_DETECTION_HORIZONTAL, true)
                .convolve2d(ConvolutionKernels.EDGE_DETECTION_HORIZONTAL, true);

        ImageTransformer verticalTransformer = transformer.copy();
        verticalTransformer.setLogDirectory(this.logDirectory);
        verticalTransformer.convolve2d(ConvolutionKernels.EDGE_DETECTION_VERTICAL, true)
                .invert();

        // Subtract the vertical edges from the horizontal edges
        // to further strengthen the horizontal edges
        transformer
                .blendMultiply(verticalTransformer.getBitmap())
                .convolve2d(ConvolutionKernels.VERTICAL_SMOOTHING)
                .convolve2d(ConvolutionKernels.EDGE_DETECTION_HORIZONTAL)
                .shift(0, -1) // shift to compensate for the shift in the vertical edge detection
                .quantize(64)
                .aggregate(Orientation.HORIZONTAL,
                        ImageAnalyser::getWeightedModeByColorConsistency,
                        false);

        List<Double> weightedModes = Arrays.stream(transformer.getColumn(0))
                .map(Color::red) // Since image is grayscale, R = G = B
                .mapToObj(x -> x / 255d)
                .collect(Collectors.toList());

        List<Integer> edgeLocations = new ArrayList<>();
        for (int i = 0; i < weightedModes.size(); i += MIN_SPACING) {
            int edgeCount = 0;
            int firstEdgePos = -1;
            // Check the next few pixels to see if they are also edges
            // and if they are, average their values and join them into one edge
            double edgeWeight = 0;
            for (int j = i; j < i + MIN_SPACING && j < weightedModes.size(); j++) {
                if (weightedModes.get(j) < EDGE_THRESHOLD) continue;
                edgeCount++;
                edgeWeight += weightedModes.get(j);
                if (firstEdgePos != -1) continue;
                firstEdgePos = j;
            }
            edgeWeight /= edgeCount;
            if (edgeWeight >= EDGE_THRESHOLD) {
                // Multiply by the pool size to get the original image coordinates
                edgeLocations.add(firstEdgePos * POOL_SIZE);
            }
        }

        // Filter out the edges that are too close to the top and bottom
        int EDGE_MARGIN = 10;
        edgeLocations = edgeLocations.stream()
                .filter(x -> x > EDGE_MARGIN && x < bitmap.getHeight() - EDGE_MARGIN)
                .collect(Collectors.toList());


        // Take a few rows above and below the edge to ensure that the edge is straight
        // by thresholding the total first order difference
        List<Integer> validRows = new ArrayList<>();
        int SIDE_PIXELS_COUNT = 2;
        int DIFF_THRESHOLD = 5;

        // Extract the bitmap of each row and some pixels above and below
        // and find the smallest first order difference, so as long as there is a
        // straight line, boundary is accepted
        // (should be low for actual boundaries)
        for (int row : edgeLocations) {
            int totalFirstOrderDiff = 0;
            for (int i = -SIDE_PIXELS_COUNT; i <= SIDE_PIXELS_COUNT; i++) {
                int y = row + i;
                if (y < 0 || y >= bitmap.getHeight()) continue;
                int[] pixels = Arrays.stream(this.getRow(y))
                        .map(Color::red)
                        .toArray();
                int firstOrderDiff = 0;
                for (int j = 0; j < pixels.length - 1; j++) {
                    firstOrderDiff += Math.abs(pixels[j] - pixels[j + 1]);
                }
                firstOrderDiff /= pixels.length - 1;
                totalFirstOrderDiff += firstOrderDiff;
            }
            if (totalFirstOrderDiff < DIFF_THRESHOLD) validRows.add(row);
        }

        return validRows;
    }
}
