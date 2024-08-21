package com.adms.australianmobileadtoolkit.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

public class ImageTransformer extends ImageProcessor {


    public ImageTransformer(Bitmap bitmap) {
        super(bitmap);
    }

    public ImageTransformer(File originalImage) {
        super(originalImage);
    }

    public ImageTransformer grayscale() {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[][] pixels = new int[width][height];
        int[][] newPixels = new int[width][height];
        // Convert the pixels to grayscale
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                pixels[x][y] = bitmap.getPixel(x, y);
                int r = (pixels[x][y] >> 16) & 0xFF;
                int g = (pixels[x][y] >> 8) & 0xFF;
                int b = pixels[x][y] & 0xFF;
                int gray = (r + g + b) / 3;
                newPixels[x][y] = 0xFF000000 | (gray << 16) | (gray << 8) | gray;
            }
        }
        // Set the grayscale pixels back to the bitmap
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y, newPixels[x][y]);
            }
        }
        log("grayscale");
        return this;
    }

    private static int colourQuantizeBitmapSnap(int colourChannel, int interval) {
        return Math.min(((Math.floorDiv(colourChannel, interval) * interval) + (interval / 2)), 255);
    }

    public ImageTransformer quantize(int interval) {
        Bitmap newBitmap = bitmap.copy(bitmap.getConfig(), true);
        // For all pixels in the image...
        for (int xx = 0; xx < bitmap.getWidth(); xx ++) {
            for (int yy = 0; yy < bitmap.getHeight(); yy ++) {
                // Reconstruct the pixels by 'snapping' the colour channels
                int thisColour = bitmap.getPixel(xx,yy);
                newBitmap.setPixel(xx,yy, Color.valueOf(Color.rgb(
                        colourQuantizeBitmapSnap(Color.red(thisColour), interval),
                        colourQuantizeBitmapSnap(Color.green(thisColour), interval),
                        colourQuantizeBitmapSnap(Color.blue(thisColour), interval))
                ).toArgb());
            }
        }
        bitmap = newBitmap;
        log("quantize");
        return this;
    }

    public ImageTransformer blendLighten(Bitmap overlay) {
        Bitmap newBitmap = bitmap.copy(bitmap.getConfig(), true);
        // For all pixels in the image...
        for (int xx = 0; xx < bitmap.getWidth(); xx ++) {
            for (int yy = 0; yy < bitmap.getHeight(); yy ++) {
                // Blend the pixels using the 'lighten' blending mode
                int thisColour = bitmap.getPixel(xx,yy);
                int overlayColour = overlay.getPixel(xx,yy);
                newBitmap.setPixel(xx,yy, Color.valueOf(Color.rgb(
                        Math.max(Color.red(thisColour), Color.red(overlayColour)),
                        Math.max(Color.green(thisColour), Color.green(overlayColour)),
                        Math.max(Color.blue(thisColour), Color.blue(overlayColour)))
                ).toArgb());
            }
        }
        bitmap = newBitmap;
        log("blendLighten");
        return this;
    }

    public ImageTransformer maxPool(int poolSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newWidth = width / poolSize;
        int newHeight = height / poolSize;
        Bitmap newBitmap = Bitmap.createBitmap(newWidth, newHeight, bitmap.getConfig());
        // Perform max pooling on the image
        for (int x = 0; x < newWidth; x++) {
            for (int y = 0; y < newHeight; y++) {
                int maxR = 0;
                int maxG = 0;
                int maxB = 0;
                for (int px = 0; px < poolSize; px++) {
                    for (int py = 0; py < poolSize; py++) {
                        int pixelX = x * poolSize + px;
                        int pixelY = y * poolSize + py;
                        int pixel = bitmap.getPixel(pixelX, pixelY);
                        maxR = Math.max(maxR, (pixel >> 16) & 0xFF);
                        maxG = Math.max(maxG, (pixel >> 8) & 0xFF);
                        maxB = Math.max(maxB, pixel & 0xFF);
                    }
                }
                newBitmap.setPixel(x, y, 0xFF000000 | (maxR << 16) | (maxG << 8) | maxB);
            }
        }
        bitmap = newBitmap;
        log("maxPool");
        return this;
    }

    public ImageTransformer averagePool(int poolSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newWidth = width / poolSize;
        int newHeight = height / poolSize;
        Bitmap newBitmap = Bitmap.createBitmap(newWidth, newHeight, bitmap.getConfig());
        // Perform average pooling on the image
        for (int x = 0; x < newWidth; x++) {
            for (int y = 0; y < newHeight; y++) {
                int totalR = 0;
                int totalG = 0;
                int totalB = 0;
                for (int px = 0; px < poolSize; px++) {
                    for (int py = 0; py < poolSize; py++) {
                        int pixelX = x * poolSize + px;
                        int pixelY = y * poolSize + py;
                        int pixel = bitmap.getPixel(pixelX, pixelY);
                        totalR += (pixel >> 16) & 0xFF;
                        totalG += (pixel >> 8) & 0xFF;
                        totalB += pixel & 0xFF;
                    }
                }
                int avgR = totalR / (poolSize * poolSize);
                int avgG = totalG / (poolSize * poolSize);
                int avgB = totalB / (poolSize * poolSize);
                newBitmap.setPixel(x, y, 0xFF000000 | (avgR << 16) | (avgG << 8) | avgB);
            }
        }
        bitmap = newBitmap;
        log("averagePool");
        return this;
    }

    public ImageTransformer medianFilter(int kernelSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap newBitmap = Bitmap.createBitmap(width, height, bitmap.getConfig());
        // Perform median filtering on the image
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int[] r = new int[kernelSize * kernelSize];
                int[] g = new int[kernelSize * kernelSize];
                int[] b = new int[kernelSize * kernelSize];
                int index = 0;
                for (int kx = 0; kx < kernelSize; kx++) {
                    for (int ky = 0; ky < kernelSize; ky++) {
                        int pixelX = x + kx - kernelSize / 2;
                        int pixelY = y + ky - kernelSize / 2;
                        if (pixelX >= 0 && pixelX < width && pixelY >= 0 && pixelY < height) {
                            int pixel = bitmap.getPixel(pixelX, pixelY);
                            r[index] = (pixel >> 16) & 0xFF;
                            g[index] = (pixel >> 8) & 0xFF;
                            b[index] = pixel & 0xFF;
                            index++;
                        }
                    }
                }
                // Sort the RGB values
                int medianIndex = index / 2;
                int medianR = r[medianIndex];
                int medianG = g[medianIndex];
                int medianB = b[medianIndex];
                newBitmap.setPixel(x, y, 0xFF000000 | (medianR << 16) | (medianG << 8) | medianB);
            }
        }
        bitmap = newBitmap;
        log("medianFilter");
        return this;
    }

    public ImageTransformer invert() {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        // Invert the pixels in the image
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = bitmap.getPixel(x, y);
                int r = 255 - (pixel >> 16 & 0xFF);
                int g = 255 - (pixel >> 8 & 0xFF);
                int b = 255 - (pixel & 0xFF);
                bitmap.setPixel(x, y, 0xFF000000 | (r << 16) | (g << 8) | b);
            }
        }
        log("invert");
        return this;
    }

    public ImageTransformer blendMultiply(Bitmap overlay) {
        Bitmap newBitmap = bitmap.copy(bitmap.getConfig(), true);
        // For all pixels in the image...
        for (int xx = 0; xx < bitmap.getWidth(); xx ++) {
            for (int yy = 0; yy < bitmap.getHeight(); yy ++) {
                // Blend the pixels using the 'multiply' blending mode
                int thisColour = bitmap.getPixel(xx,yy);
                int overlayColour = overlay.getPixel(xx,yy);
                newBitmap.setPixel(xx,yy, Color.valueOf(Color.rgb(
                        (Color.red(thisColour) * Color.red(overlayColour)) / 255,
                        (Color.green(thisColour) * Color.green(overlayColour)) / 255,
                        (Color.blue(thisColour) * Color.blue(overlayColour)) / 255)
                ).toArgb());
            }
        }
        bitmap = newBitmap;
        log("blendMultiply");
        return this;
    }

    public ImageTransformer topKAveragePool(int poolSize, int k) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newWidth = width / poolSize;
        int newHeight = height / poolSize;
        Bitmap newBitmap = Bitmap.createBitmap(newWidth, newHeight, bitmap.getConfig());
        // Perform top-k average pooling on the image
        for (int x = 0; x < newWidth; x++) {
            for (int y = 0; y < newHeight; y++) {
                int[] topK = new int[k];
                for (int i = 0; i < k; i++) {
                    topK[i] = 0;
                }
                for (int px = 0; px < poolSize; px++) {
                    for (int py = 0; py < poolSize; py++) {
                        int pixelX = x * poolSize + px;
                        int pixelY = y * poolSize + py;
                        int pixel = bitmap.getPixel(pixelX, pixelY);
                        for (int i = 0; i < k; i++) {
                            if (pixel > topK[i]) {
                                int temp = topK[i];
                                topK[i] = pixel;
                                pixel = temp;
                            }
                        }
                    }
                }
                int totalR = 0;
                int totalG = 0;
                int totalB = 0;
                for (int i = 0; i < k; i++) {
                    totalR += (topK[i] >> 16) & 0xFF;
                    totalG += (topK[i] >> 8) & 0xFF;
                    totalB += topK[i] & 0xFF;
                }
                int avgR = totalR / k;
                int avgG = totalG / k;
                int avgB = totalB / k;
                newBitmap.setPixel(x, y, 0xFF000000 | (avgR << 16) | (avgG << 8) | avgB);
            }
        }
        bitmap = newBitmap;
        log("topKAveragePool");
        return this;
    }



    /**
     * Perform a 2D convolution on the image with the specified kernel
     * @param kernel The kernel to convolve the image with
     * @param invertNegative Whether to invert the negative values in the result
     * @return The ImageTransformer object
     */
    public ImageTransformer convolve2d(double[][] kernel, boolean invertNegative) {
//        if (kernel.length % 2 == 0 || kernel[0].length % 2 == 0) {
//            throw new IllegalArgumentException("Kernel must have an odd width and height");
//        }
//        if (kernel.length != kernel[0].length) {
//            throw new IllegalArgumentException("Kernel must be square");
//        }
//        if (kernel.length == 1) {
//            throw new IllegalArgumentException("Kernel must be at least 3x3");
//        }
        if (kernel.length > bitmap.getWidth() || kernel[0].length > bitmap.getHeight()) {
            throw new IllegalArgumentException("Kernel must be smaller than the image");
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[][] pixels = new int[height][width];
        int[][] newPixels = new int[height][width];
        // Convolve the pixels with the kernel
        int kernelHeight = kernel.length;
        int kernelWidth = kernel[0].length;
        int kernelHalfWidth = kernelWidth / 2;
        int kernelHalfHeight = kernelHeight / 2;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = 0;
                int g = 0;
                int b = 0;
                for (int ky = 0; ky < kernelHeight; ky++) {
                    for (int kx = 0; kx < kernelWidth; kx++) {
                        int pixelX = x + kx - kernelHalfWidth;
                        int pixelY = y + ky - kernelHalfHeight;
                        if (pixelX >= 0 && pixelX < width && pixelY >= 0 && pixelY < height) {
                            int pixel = bitmap.getPixel(pixelX, pixelY);
                            r += (int) ((pixel >> 16 & 0xFF) * kernel[ky][kx]);
                            g += (int) ((pixel >> 8 & 0xFF) * kernel[ky][kx]);
                            b += (int) ((pixel & 0xFF) * kernel[ky][kx]);
                        }
                    }
                }
                if (invertNegative) {
                    r = Math.abs(r);
                    g = Math.abs(g);
                    b = Math.abs(b);
                }
                r = Math.min(Math.max(r, 0), 255);
                g = Math.min(Math.max(g, 0), 255);
                b = Math.min(Math.max(b, 0), 255);
                newPixels[y][x] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }
        // Set the convolved pixels back to the bitmap
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y, newPixels[y][x]);
            }
        }
        log("convolve2d");
        return this;
    }
    public ImageTransformer convolve2d(double[][] kernel) {
        return convolve2d(kernel, false);
    }
    public ImageTransformer convolve2d(ConvolutionKernels kernel) {
        return convolve2d(kernel.getKernel());
    }

    public ImageTransformer convolve2d(ConvolutionKernels kernel, boolean invertNegative) {
        return convolve2d(kernel.getKernel(), invertNegative);
    }

    public ImageTransformer shift(int dx, int dy) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap newBitmap = Bitmap.createBitmap(width, height, bitmap.getConfig());
        // Shift the image
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int newX = x + dx;
                int newY = y + dy;
                if (newX >= 0 && newX < width && newY >= 0 && newY < height) {
                    newBitmap.setPixel(newX, newY, bitmap.getPixel(x, y));
                }
            }
        }
        bitmap = newBitmap;
        log("shift");
        return this;
    }

    private int toGray(int pixel) {
        int r = (pixel >> 16) & 0xFF;
        int g = (pixel >> 8) & 0xFF;
        int b = pixel & 0xFF;
        return (r + g + b) / 3;
    }

    public ImageTransformer equaliseHistogram() {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] histogram = new int[256];
        int[] cumulativeHistogram = new int[256];
        int totalPixels = width * height;
        // Calculate the histogram of the image
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = bitmap.getPixel(x, y);
                int gray = toGray(pixel);
                histogram[gray]++;
            }
        }
        // Calculate the cumulative histogram of the image
        cumulativeHistogram[0] = histogram[0];
        for (int i = 1; i < 256; i++) {
            cumulativeHistogram[i] = cumulativeHistogram[i - 1] + histogram[i];
        }
        // Equalise the histogram of the image
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = bitmap.getPixel(x, y);
                int gray = toGray(pixel);
                int newGray = (int) (cumulativeHistogram[gray] * 255.0 / totalPixels);
                bitmap.setPixel(x, y, Color.valueOf(Color.rgb(newGray, newGray, newGray)).toArgb());
            }
        }
        log("equaliseHistogram");
        return this;
    }

    public ImageTransformer copy() {
        return new ImageTransformer(bitmap.copy(bitmap.getConfig(), true));
    }

    public ImageTransformer upsample(int scale) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newWidth = width * scale;
        int newHeight = height * scale;
        Bitmap newBitmap = Bitmap.createBitmap(newWidth, newHeight, bitmap.getConfig());
        // Upsample the image
        for (int x = 0; x < newWidth; x++) {
            for (int y = 0; y < newHeight; y++) {
                int pixelX = x / scale;
                int pixelY = y / scale;
                newBitmap.setPixel(x, y, bitmap.getPixel(pixelX, pixelY));
            }
        }
        bitmap = newBitmap;
        log("upsample");
        return this;
    }

    public ImageTransformer aggregate(Orientation orientation, Function<int[], Integer> operation, boolean repeatToFitBitmapSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        // Aggregate the pixels along the specified orientation
        // vertical: combine the pixels in each column
        // horizontal: combine the pixels in each row
        int size = orientation == Orientation.VERTICAL ? width : height;
        int[] aggregated = new int[size];
        for (int i = 0; i < size; i ++) {
            int[] line = new int[orientation == Orientation.VERTICAL ? height : width];
            for (int j = 0; j < (orientation == Orientation.VERTICAL ? height : width); j++) {
                int pixelX = orientation == Orientation.VERTICAL ? i : j;
                int pixelY = orientation == Orientation.VERTICAL ? j : i;
                line[j] = bitmap.getPixel(pixelX, pixelY);
            }
            aggregated[i] = operation.apply(line);
        }
        if (!repeatToFitBitmapSize) {
            // Construct a new bitmap with the aggregated pixels
            int newHeight = orientation == Orientation.VERTICAL ? 1 : height;
            int newWidth = orientation == Orientation.VERTICAL ? width : 1;
            Bitmap newBitmap = Bitmap.createBitmap(newWidth, newHeight, bitmap.getConfig());
            if (orientation == Orientation.VERTICAL) {
                for (int i = 0; i < width; i++) {
                    newBitmap.setPixel(i, 0, aggregated[i]);
                }
            } else {
                for (int i = 0; i < height; i++) {
                    newBitmap.setPixel(0, i, aggregated[i]);
                }
            }
            bitmap = newBitmap;
            log("aggregate");
            return this;
        }

        // Set the aggregated pixels back to the bitmap
        for (int i = 0; i < (orientation == Orientation.VERTICAL ? width : height); i++) {
            for (int j = 0; j < (orientation == Orientation.VERTICAL ? height : width); j++) {
                int pixelX = orientation == Orientation.VERTICAL ? i : j;
                int pixelY = orientation == Orientation.VERTICAL ? j : i;
                bitmap.setPixel(pixelX, pixelY, aggregated[i]);
            }
        }
        log("aggregate");
        return this;
    }

    public ImageTransformer crop(int x, int y, int width, int height) {
        bitmap = Bitmap.createBitmap(bitmap, x, y, width, height);
        log("crop");
        return this;
    }

    /**
     * Sample the image in the specified direction at the specified positions
     * and merge into a new image
     * @param orientation
     * @param positions
     * @return
     */
    public ImageTransformer sampleInDirection(Orientation orientation, List<Integer> positions) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newWidth = orientation == Orientation.VERTICAL ? positions.size() : width;
        int newHeight = orientation == Orientation.HORIZONTAL ? positions.size() : height;
        Bitmap newBitmap = Bitmap.createBitmap(newWidth, newHeight, bitmap.getConfig());
        for (int i = 0; i < positions.size(); i++) {
            int position = positions.get(i);
            for (int j = 0; j < (orientation == Orientation.VERTICAL ? height : width); j++) {
                int pixelX = orientation == Orientation.VERTICAL ? i : j;
                int pixelY = orientation == Orientation.VERTICAL ? j : i;
                if (orientation == Orientation.VERTICAL) {
                    newBitmap.setPixel(pixelX, pixelY, bitmap.getPixel(position, j));
                } else {
                    newBitmap.setPixel(pixelX, pixelY, bitmap.getPixel(j, position));
                }
            }
        }
        bitmap = newBitmap;
        log("sampleInDirection");
        return this;
    }

    public ImageTransformer updateColor(int fromColor, int toColor) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (bitmap.getPixel(x, y) == fromColor) {
                    bitmap.setPixel(x, y, toColor);
                }
            }
        }
        log("updateColor");
        return this;
    }

    public ImageTransformer sampleInDirection(Orientation orientation, int sampleSize) {
        // Randomly generate the positions to sample
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Random random = new Random();
        Set<Integer> positions = new HashSet<>();
        while (positions.size() < sampleSize) {
            positions.add(random.nextInt(orientation == Orientation.VERTICAL ? width : height));
        }
        return sampleInDirection(orientation, List.copyOf(positions));
    }

    public ImageTransformer aggregate(Orientation orientation, Function<int[], Integer> operation) {
        return aggregate(orientation, operation, true);
    }
}
