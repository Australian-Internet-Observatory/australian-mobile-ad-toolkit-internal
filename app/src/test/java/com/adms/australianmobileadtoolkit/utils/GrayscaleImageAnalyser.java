package com.adms.australianmobileadtoolkit.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;

/**
 * Used to create analysis of a grayscale image without modifying it
 */
public class GrayscaleImageAnalyser {
    private Bitmap bitmap;

    public GrayscaleImageAnalyser(File originalImage) {
        bitmap = BitmapFactory.decodeFile(originalImage.getAbsolutePath());
    }
    public GrayscaleImageAnalyser(Bitmap bitmap) {
        this.bitmap = bitmap;
    }
    public Bitmap getBitmap() {
        return bitmap;
    }
    public int[][] filterPixelsAboveThreshold(double threshold) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[][] pixels = new int[width][height];
        // Convert the pixels to grayscale
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                pixels[x][y] = bitmap.getPixel(x, y);
                int r = (pixels[x][y] >> 16) & 0xFF;
                int g = (pixels[x][y] >> 8) & 0xFF;
                int b = pixels[x][y] & 0xFF;
                int gray = (r + g + b) / 3;
                if (gray > threshold) {
                    pixels[x][y] = 0xFFFFFFFF;
                } else {
                    pixels[x][y] = 0xFF000000;
                }
            }
        }
        return pixels;
    }
}
