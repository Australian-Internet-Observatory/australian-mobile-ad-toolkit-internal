package com.adms.australianmobileadtoolkit.interpreter.platform;

import static com.adms.australianmobileadtoolkit.interpreter.detector.Constants.MODEL_PATH;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import androidx.annotation.NonNull;
import com.adms.australianmobileadtoolkit.R;
import com.adms.australianmobileadtoolkit.interpreter.detector.BoundingBox;
import com.adms.australianmobileadtoolkit.interpreter.detector.Detector;
import java.util.List;

public class ImageClassifier {
    public static String TAG = "ImageClassifier";

    public ImageClassifier(Context context) {
        Detector.DetectorListener thisDetectorListener = new Detector.DetectorListener() {
            @Override
            public void onEmptyDetect() {
                Log.i(TAG, "Nothing found!");

            }

            @Override
            public void onDetect(@NonNull List<BoundingBox> boundingBoxes, long inferenceTime) {
                Log.i(TAG, boundingBoxes.toString());
            }
        };

        Detector thisDetector = new Detector(context, MODEL_PATH, "fdsfsdf", thisDetectorListener);
        Double elapsedTime = Long.valueOf(System.currentTimeMillis()).doubleValue();

        for (int i = 0; i < 300; i ++) {
            Bitmap testImage1 = BitmapFactory.decodeResource(context.getResources(), R.drawable.test_image_1);
            thisDetector.detect(testImage1);
        }
        thisDetector.close();
        elapsedTime = Math.abs(elapsedTime - Long.valueOf(System.currentTimeMillis()).doubleValue()) / 1000;
        Log.i(TAG, "Time taken: "+ elapsedTime);
    }

    // SPONSORED

    // Inference (small model) takes
    // 0.2s on Google Pixel Pro 9
    // 1.2s on samsung a30
    // 0.24s on Google Pixel Pro 7
    // 0.15s on Oppo A79


    // Inference (nano model) takes
    // 0.16s on Google Pixel Pro 9
    // 0.41s on samsung a30
    // 0.15s on Google Pixel Pro 7
    // 0.12s on Oppo A79

    // GENERAL V1

    // Inference (small model) takes
    // 0.22s on Google Pixel Pro 9
    // 2.18s on samsung a30
    // 0.26s on Google Pixel Pro 7
}
