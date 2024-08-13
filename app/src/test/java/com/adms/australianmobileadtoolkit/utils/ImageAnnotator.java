package com.adms.australianmobileadtoolkit.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Used to annotate images with labels and elements
 */
public class ImageAnnotator extends ImageProcessor {

    public ImageAnnotator(Bitmap bitmap) {
        super(bitmap);
    }

    public ImageAnnotator(File originalImage) {
        super(originalImage);
    }

    public void drawLine(Orientation orientation, int position, int thickness, int color) {
        // Add a line to the image
        for (int i = 0; i < thickness; i++) {
            if (orientation == Orientation.HORIZONTAL) {
                for (int x = 0; x < bitmap.getWidth(); x++) {
                    if (position + i >= bitmap.getHeight() || position + i < 0) {
                        continue;
                    }
                    bitmap.setPixel(x, position + i, color);
                }
            } else {
                for (int y = 0; y < bitmap.getHeight(); y++) {
                    if (position + i >= bitmap.getWidth() || position + i < 0) {
                        continue;
                    }
                    bitmap.setPixel(position + i, y, color);
                }
            }
        }
    }

    public void drawLine(Orientation orientation, int position) {
        int RED = 0xFFFF0000;
        drawLine(orientation, position, 3, RED);
    }
}
