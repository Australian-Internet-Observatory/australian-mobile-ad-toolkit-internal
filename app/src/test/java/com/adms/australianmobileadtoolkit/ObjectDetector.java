package com.adms.australianmobileadtoolkit;

import static com.adms.australianmobileadtoolkit.interpreter.Platform.filePath;
import static com.google.gson.JsonParser.parseString;

import static java.util.Arrays.asList;

import com.google.gson.JsonArray;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class ObjectDetector {

    private Integer currentFrame = 0;
    private JSONXObject inferencesByFrames = new JSONXObject();
    public JSONXObject inferenceResult = new JSONXObject();
    private Double elapsedTime = Long.valueOf(System.currentTimeMillis()).doubleValue();
    private String inferenceCase;
    private checkPoint thisCheckPoint;

    public static List<JSONObject> simulatedYOLODetection(String modelPath, String imagePath) {
        final String PYTHON_EXECUTABLE = "/Users/obei/anaconda3/envs/keras-env-1/bin/python";
        final String PYTHON_DETECTION_SCRIPT = "/Users/obei/Developer/2024/_08_app/detectorSimulation/detect.py";
        List<JSONObject> boundingBoxes = new ArrayList<>();
        try {
            String s;
            String command = PYTHON_EXECUTABLE+" "+PYTHON_DETECTION_SCRIPT+" "+modelPath+" "+imagePath;
            Process p = Runtime.getRuntime().exec(command);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((s = stdInput.readLine()) != null) {
                try {
                    JsonArray jsonElements = parseString(s).getAsJsonArray();
                    for (int i = 0; i < jsonElements.size(); i++) {
                        boundingBoxes.add(new JSONObject(jsonElements.get(i).getAsJsonObject().toString()));
                    }
                } catch (Exception e) {}
            }
            while ((s = stdError.readLine()) != null) {}
            System.out.println(boundingBoxes);
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
        return boundingBoxes;
    }

    public ObjectDetector(File analysisDirectory, File thisScreenRecordingFile,
                           List<String> retainedFrameFiles, List<Integer> retainedFrames, String modelName, String thisCase) {
        inferenceCase = "inference"+thisCase;
        thisCheckPoint = new checkPoint(thisScreenRecordingFile.getName(), (new File(analysisDirectory, "checkpoint")));

        if (thisCheckPoint.container.has(inferenceCase)) {
            inferenceResult = new JSONXObject((JSONObject) thisCheckPoint.container.get(inferenceCase), true);
        } else {
                String adjustedModelPath = filePath(asList(((new File(".")).getAbsolutePath()), "..", "..", "pt_models", modelName)).getAbsolutePath().replace(".tflite", ".pt").replace("float16", "float32");
                Integer currentFrameIndex = 0;
                for (String retainedFrameFile : retainedFrameFiles) {
                    currentFrame = retainedFrames.get(currentFrameIndex);
                    inferencesByFrames.set(currentFrame, simulatedYOLODetection(adjustedModelPath, retainedFrameFile));
                    if (Objects.equals(currentFrame, retainedFrames.get(retainedFrames.size() - 1))) {
                        elapsedTime = Math.abs(elapsedTime - Long.valueOf(System.currentTimeMillis()).doubleValue()) / 1000;
                        inferenceResult.set("nFramesAnalyzed", retainedFrames.size());
                        inferenceResult.set("inferencesByFrames", inferencesByFrames.internalJSONObject);
                        inferenceResult.set("elapsedTime", elapsedTime);
                        thisCheckPoint.set(inferenceCase, inferenceResult.internalJSONObject);
                        thisCheckPoint.save();
                    }
                    currentFrameIndex ++;
                }
        }
    }

    public static Function<JSONXObject, JSONXObject> objectDetectorMachine = (x) -> {
        File analysisDirectory = (File) x.get("analysisDirectory");
        File thisScreenRecordingFile = (File) x.get("thisScreenRecordingFile");
        List<String> retainedFrameFiles = (List<String>) x.get("retainedFrameFiles");
        List<Integer> retainedFrames = (List<Integer>) x.get("retainedFrames");
        String modelName = (String) x.get("modelName");
        String thisCase = (String) x.get("thisCase");
        return new ObjectDetector(analysisDirectory, thisScreenRecordingFile,
                retainedFrameFiles, retainedFrames, modelName, thisCase).inferenceResult;
    };
}