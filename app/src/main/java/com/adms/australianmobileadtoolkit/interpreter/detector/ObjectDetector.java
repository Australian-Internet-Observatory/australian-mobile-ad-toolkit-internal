package com.adms.australianmobileadtoolkit.interpreter.detector;

import static com.adms.australianmobileadtoolkit.interpreter.Platform.persistThread;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;

import com.adms.australianmobileadtoolkit.JSONXObject;
import com.adms.australianmobileadtoolkit.checkPoint;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class ObjectDetector {
    public static String TAG = "ImageClassifier";

    private Integer currentFrame = 0;
    private JSONXObject inferencesByFrames = new JSONXObject();
    public JSONXObject inferenceResult = new JSONXObject();
    private Double elapsedTime = Long.valueOf(System.currentTimeMillis()).doubleValue();
    private String inferenceCase;
    private checkPoint thisCheckPoint;
    private Detector thisDetector;

    public List<JSONObject> inferencesOnFrame(List<BoundingBox> boundingBoxes) {
        List<JSONObject> boundingBoxesRecorded = new ArrayList<>();
        for (BoundingBox b : boundingBoxes) {
            boundingBoxesRecorded.add((new JSONXObject())
                    .set("x1", (double) b.getX1())
                    .set("x2", (double) b.getX2())
                    .set("y1", (double) b.getY1())
                    .set("y2", (double) b.getY2())
                    .set("cx", (double) b.getCx())
                    .set("cy", (double) b.getCy())
                    .set("w", (double) b.getW())
                    .set("h", (double) b.getH())
                    .set("confidence", (double) b.getCnf())
                    .set("className", b.getClsName()).internalJSONObject
            );
        }
        return boundingBoxesRecorded;
    }

    public void setInferencesOnFrame(List<Integer> retainedFrames, List<BoundingBox> inferenceOutcome) {
        inferencesByFrames.set(currentFrame, inferencesOnFrame(inferenceOutcome));
        if (Objects.equals(currentFrame, retainedFrames.get(retainedFrames.size() - 1))) {
            thisDetector.close();
            elapsedTime = Math.abs(elapsedTime - Long.valueOf(System.currentTimeMillis()).doubleValue()) / 1000;
            // when the process is complete, add to checkpoint
            inferenceResult.set("nFramesAnalyzed", retainedFrames.size());
            inferenceResult.set("inferencesByFrames", inferencesByFrames.internalJSONObject);
            inferenceResult.set("elapsedTime", elapsedTime);
            thisCheckPoint.set(inferenceCase, inferenceResult.internalJSONObject);
            thisCheckPoint.save();
        }
    }

    // TODO - rename to inference event
    public ObjectDetector(Context context, File analysisDirectory, File thisScreenRecordingFile,
                           List<String> retainedFrameFiles, List<Integer> retainedFrames, String modelName, String thisCase) throws Exception {
        Integer INFERENCE_IMAGE_WIDTH = 640;
        Integer INFERENCE_IMAGE_HEIGHT = 640;
        inferenceCase = "inference"+thisCase;

        thisCheckPoint = new checkPoint(thisScreenRecordingFile.getName(), new File(analysisDirectory, "checkpoint"));

        if (thisCheckPoint.container.has(inferenceCase)) {
            inferenceResult = new JSONXObject((JSONObject) thisCheckPoint.container.get(inferenceCase), true);
        } else {
            Detector.DetectorListener thisDetectorListener = new Detector.DetectorListener() {
                @Override
                public void onEmptyDetect() {
                    setInferencesOnFrame(retainedFrames, (new ArrayList<>()));
                }

                @Override
                public void onDetect(@NonNull List<BoundingBox> boundingBoxes, long inferenceTime) {
                    setInferencesOnFrame(retainedFrames, boundingBoxes);
                }
            };

            thisDetector = new Detector(context, modelName, null, thisDetectorListener);
            Integer currentFrameIndex = 0;
            for (String retainedFrameFile : retainedFrameFiles) {
                persistThread(context, TAG);
                currentFrame = retainedFrames.get(currentFrameIndex);
                Bitmap thisFrameBitmap = Bitmap.createScaledBitmap(
                        BitmapFactory.decodeFile(retainedFrameFile), INFERENCE_IMAGE_WIDTH, INFERENCE_IMAGE_HEIGHT, false);
                thisDetector.detect(thisFrameBitmap);
                currentFrameIndex ++;
            }
        }
    }

    public static Function<JSONXObject, JSONXObject> objectDetectorAndroid = (x) -> {
        Context context = (Context) x.get("context");
        File analysisDirectory = (File) x.get("analysisDirectory");
        File thisScreenRecordingFile = (File) x.get("thisScreenRecordingFile");
        List<String> retainedFrameFiles = (List<String>) x.get("retainedFrameFiles");
        List<Integer> retainedFrames = (List<Integer>) x.get("retainedFrames");
        String modelName = (String) x.get("modelName");
        String thisCase = (String) x.get("thisCase");
        try {
            return new ObjectDetector(context, analysisDirectory, thisScreenRecordingFile,
                    retainedFrameFiles, retainedFrames, modelName, thisCase).inferenceResult;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    };

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
