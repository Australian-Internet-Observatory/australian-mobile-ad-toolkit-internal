package com.adms.australianmobileadtoolkit.interpreter.detector;

import static com.adms.australianmobileadtoolkit.appSettings.logMessage;
import static com.adms.australianmobileadtoolkit.interpreter.Platform.persistThread;
import static com.adms.australianmobileadtoolkit.interpreter.Sampler.basicReading;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.annotation.NonNull;

import com.adms.australianmobileadtoolkit.JSONXObject;
import com.adms.australianmobileadtoolkit.R;
import com.adms.australianmobileadtoolkit.checkPoint;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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




    public static String adaptClassName(String thisCaseUnadapted, String className) {
        String thisCase = thisCaseUnadapted.replace("float32_", "").replace("_int8.tflite", "");
        if (!className.contains("class")) {
            return className;
        }
        try {
            Integer actualIndex = Integer.parseInt(className.replace("class",""))-1;
            if (Objects.equals(thisCase, "facebook_sponsored")) {
                return List.of("SPONSORED_TEXT").get(actualIndex);
            } else
            if (Objects.equals(thisCase, "facebook_elements")) {
                return Arrays.asList("CROSS_AND_ELLIPSIS", "ENGAGEMENT_BUTTONS", "MARKETPLACE_ELEMENT", "POST_FOOTER", "POST_HEADER", "SPONSORED_BY_SELLERS_TEXT", "VIDEO_BUTTONS").get(actualIndex);
            } else
            if (Objects.equals(thisCase, "tiktok_sponsored")) {
                return Arrays.asList("PAID_PARTNERSHIP_TEXT", "PROMOTIONAL_CONTENT_TEXT", "SPONSORED_TEXT").get(actualIndex);
            } else
            if (Objects.equals(thisCase, "tiktok_elements")) {
                return Arrays.asList("ENGAGEMENT_BUTTONS", "LIVE_BUTTON", "POST_THUMBNAIL", "REEL_SEARCH_INPUT", "SEARCH_BUTTON").get(actualIndex);
            } else
            if (Objects.equals(thisCase, "instagram_sponsored")) {
                return Arrays.asList("SPONSORED_TEXT").get(actualIndex);
            } else
            if (Objects.equals(thisCase, "instagram_elements")) {
                return Arrays.asList("BUTTONS_ENGAGEMENT", "BUTTON_NEW_POST", "FEED_POST_HEADER").get(actualIndex);
            } else
            if (Objects.equals(thisCase, "youtube_sponsored")) {
                return Arrays.asList("PRODUCT_IN_THIS_VIDEO_TEXT", "SPONSORED_TEXT", "SPONSORED_TEXT_HORIZONTAL").get(actualIndex);
            } else
            if (Objects.equals(thisCase, "youtube_elements")) {
                return Arrays.asList("APP_STYLE_ELEMENT", "ENGAGEMENT_BUTTONS", "PLUS_BUTTON", "PREVIEW_ELEMENT", "PREVIEW_FOOTER_ELLIPSIS", "PRODUCT_ELEMENT", "VISIT_ADVERTISER_TEXT_HORIZONTAL").get(actualIndex);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return className;
    }

    public List<JSONObject> inferencesOnFrame(String modelName, List<BoundingBox> boundingBoxes) {
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
                    //.set("className", b.getClsName())
                    .set("className", adaptClassName(modelName, b.getClsName()))
                    .set("confidence", (double) b.getCnf()).internalJSONObject
            );
        }
        return boundingBoxesRecorded;
    }

    public void setInferencesOnFrame(String modelName, List<Integer> retainedFrames, List<BoundingBox> inferenceOutcome, String thisCase) {
        inferencesByFrames.set(currentFrame, inferencesOnFrame(modelName, inferenceOutcome));
        if (Objects.equals(currentFrame, retainedFrames.get(retainedFrames.size() - 1))) {
            thisDetector.close();
            elapsedTime = Math.abs(elapsedTime - Long.valueOf(System.currentTimeMillis()).doubleValue()) / 1000;
            // when the process is complete, add to checkpoint
            inferenceResult.set("nFramesAnalyzed", retainedFrames.size());
            inferenceResult.set("inferencesByFrames", inferencesByFrames.internalJSONObject);
            inferenceResult.set("elapsedTime", elapsedTime);
            if (!thisCase.equals("Provision")) {
                thisCheckPoint.set(inferenceCase, inferenceResult.internalJSONObject);
                thisCheckPoint.save();
            }
        }
    }

    // TODO - rename to inference event
    public ObjectDetector(Context context, File analysisDirectory, File thisScreenRecordingFile,
                           List<String> retainedFrameFiles, List<Integer> retainedFrames, String modelName, String thisCase) throws Exception {
        inferenceCase = "inference"+thisCase;

        Detector.DetectorListener thisDetectorListener = new Detector.DetectorListener() {
            @Override
            public void onEmptyDetect() {
                setInferencesOnFrame(modelName, retainedFrames, (new ArrayList<>()), thisCase);
            }

            @Override
            public void onDetect(@NonNull List<BoundingBox> boundingBoxes, long inferenceTime) {
                setInferencesOnFrame(modelName, retainedFrames, boundingBoxes, thisCase);
            }
        };

        if (thisCase.equals("Provision")) {
            thisDetector = new Detector(context, modelName, null, thisDetectorListener);
            persistThread(context, TAG);
            try {
                currentFrame = 0;
                Bitmap thisBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ad_detection_test_image_2);
                thisDetector.detect(thisBitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            thisCheckPoint = new checkPoint(thisScreenRecordingFile.getName(), new File(analysisDirectory, "checkpoint"));
            if (thisCheckPoint.container.has(inferenceCase)) {
                inferenceResult = new JSONXObject((JSONObject) thisCheckPoint.container.get(inferenceCase), true);
            } else {
                thisDetector = new Detector(context, modelName, null, thisDetectorListener);
                Integer currentFrameIndex = 0;
                for (String retainedFrameFile : retainedFrameFiles) {
                    persistThread(context, TAG);
                    logMessage(TAG, retainedFrameFile.toString());
                    try {
                        currentFrame = retainedFrames.get(currentFrameIndex);
                        thisDetector.detect(BitmapFactory.decodeFile(retainedFrameFile));
                        currentFrameIndex ++;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
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
        }
        catch (InterruptedException e) {
            return new JSONXObject().set("interrupted", true);
        }
        catch (Exception e) {
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
