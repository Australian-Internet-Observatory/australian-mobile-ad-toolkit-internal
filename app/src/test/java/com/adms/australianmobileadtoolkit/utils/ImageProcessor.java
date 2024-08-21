package com.adms.australianmobileadtoolkit.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;

public abstract class ImageProcessor {
    protected Bitmap bitmap;

    protected String logDirectory = null;
    private int logStepCount = 0;

    public void setLogDirectory(String logDirectory) {
        this.logDirectory = logDirectory;
        this.logStepCount = 0;
    }

    protected void log(String stepName) {
        if (logDirectory == null) {
            return;
        }
        String transformerId = this.hashCode() + "";
        save(new File(logDirectory, transformerId + "_" + logStepCount + "_" + stepName + ".jpg"));
        logStepCount++;
    }

    public ImageProcessor(Bitmap bitmap) {
        this.bitmap = bitmap.copy(bitmap.getConfig(), bitmap.isMutable());
    }
    public ImageProcessor(File originalImage) {
        bitmap = BitmapFactory.decodeFile(originalImage.getAbsolutePath());
    }
    public Bitmap getBitmap() {
        return bitmap;
    }
    public int[] getRow(int y) {
        int width = bitmap.getWidth();
        int[] row = new int[width];
        for (int x = 0; x < width; x++) {
            row[x] = bitmap.getPixel(x, y);
        }
        return row;
    }
    public int[] getColumn(int x) {
        int height = bitmap.getHeight();
        int[] column = new int[height];
        for (int y = 0; y < height; y++) {
            column[y] = bitmap.getPixel(x, y);
        }
        return column;
    }
    public int[][] getPixels() {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[][] pixels = new int[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                pixels[x][y] = bitmap.getPixel(x, y);
            }
        }
        return pixels;
    }
    public void save(File output) {
        // Save the annotated image as jpg
        try (FileOutputStream out = new FileOutputStream(output)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
