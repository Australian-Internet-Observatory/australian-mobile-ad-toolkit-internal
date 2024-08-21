package com.adms.australianmobileadtoolkit.utils;

import static org.robolectric.Shadows.shadowOf;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import org.robolectric.shadows.ShadowCanvas;

import java.io.File;
import java.util.List;

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

    public ImageAnnotator drawLine(Orientation orientation, int position, int thickness, int color) {
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
        return this;
    }

    public ImageAnnotator drawLine(Orientation orientation, int position) {
        drawLine(orientation, position, 3, Color.RED);
        return this;
    }

    public ImageAnnotator drawLines(Orientation orientation, List<Integer> positions, int thickness, int color) {
        for (int position : positions) {
            drawLine(orientation, position, thickness, color);
        }
        return this;
    }
    public ImageAnnotator drawLines(Orientation orientation, List<Integer> positions, int thickness) {
        drawLines(orientation, positions, thickness, Color.RED);
        return this;
    }
    public ImageAnnotator drawLines(Orientation orientation, List<Integer> positions) {
        drawLines(orientation, positions, 3, Color.RED);
        return this;
    }

    public ImageAnnotator drawRect(int top, int left, int bottom, int right, int thickness, int color, boolean fill) {
        // Add a rectangle to the image
        for (int i = 0; i < thickness; i++) {
            if (fill) {
                for (int x = left; x < right; x++) {
                    for (int y = top; y < bottom; y++) {
                        if (x < 0 || x >= bitmap.getWidth() || y < 0 || y >= bitmap.getHeight()) {
                            continue;
                        }
                        bitmap.setPixel(x, y, color);
                    }
                }
            } else {
                for (int x = left; x < right; x++) {
                    if (top + i >= bitmap.getHeight() || top + i < 0) {
                        continue;
                    }
                    bitmap.setPixel(x, top + i, color);
                    if (bottom + i >= bitmap.getHeight() || bottom + i < 0) {
                        continue;
                    }
                    bitmap.setPixel(x, bottom + i, color);
                }
                for (int y = top; y < bottom; y++) {
                    if (left + i >= bitmap.getWidth() || left + i < 0) {
                        continue;
                    }
                    bitmap.setPixel(left + i, y, color);
                    if (right + i >= bitmap.getWidth() || right + i < 0) {
                        continue;
                    }
                    bitmap.setPixel(right + i, y, color);
                }
            }
        }
        return this;
    }
}
