package com.adms.australianmobileadtoolkit.utils;

/**
 * Enum containing convolution kernels for image processing
 */
public enum ConvolutionKernels {
    /**
     * The Prewitt kernel for edge detection
     */
    EDGE_DETECTION_VERTICAL(new double[][]{
            {1, 0, -1},
            {1, 0, -1},
            {1, 0, -1}
    }),
    /**
     * The Prewitt kernel for horizontal edge detection
     */
    EDGE_DETECTION_HORIZONTAL(new double[][]{
            {1, 1, 1},
            {0, 0, 0},
            {-1, -1, -1}
    }),
    /**
     * A kernel that performs a simple smoothing operation on 3 pixels on a column
     */
    VERTICAL_SMOOTHING(new double[][]{
            {1/4f},
            {1/2f},
            {1/4f},
    }),;

    private final double[][] kernel;

    ConvolutionKernels(double[][] kernel) {
        this.kernel = kernel;
    }

    public double[][] getKernel() {
        return kernel;
    }
}
