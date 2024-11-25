package com.adms.australianmobileadtoolkit.platform.tiktok;

import static com.adms.australianmobileadtoolkit.IsolatedTest.testScreenRecordingsDirectory;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Facebook.facebookComprehensiveReading;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.filenameUnextended;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.readJSONFromFile;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.writeToJSON;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Tiktok.tiktokComprehensiveReading;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Tiktok.tiktokGenerateQuickReading;
import static com.adms.australianmobileadtoolkit.machine.frameGrabMachine;
import static com.adms.australianmobileadtoolkit.machine.getVideoMetadataMachine;
import static com.adms.australianmobileadtoolkit.machine.testContext;


import com.adms.australianmobileadtoolkit.IsolatedTest;
import com.adms.australianmobileadtoolkit.JSONXObject;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.function.Function;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {33})

public class readings {


    @Test
    public void testTikTokQuickReading() {
        IsolatedTest thisTest = new IsolatedTest("tiktokQuickReading");
        thisTest.setCasesDirectory(testScreenRecordingsDirectory);

        Function<File, JSONObject> localRoutine = x -> {
            File debugDirectory = new File(thisTest.outputDirectory.getAbsolutePath(), filenameUnextended(x));

            return tiktokGenerateQuickReading(testContext, true, debugDirectory, x, getVideoMetadataMachine, frameGrabMachine);
        };

        thisTest.applyToCases(localRoutine); // Comment out this line to speed up reporting

        for (File thisCaseDirectory : thisTest.outputDirectory.listFiles()) {
            if (thisCaseDirectory.isDirectory()) {
                JSONXObject thisMetadata = new JSONXObject(readJSONFromFile(new File(thisCaseDirectory, "metadata.json")));


                Boolean wasMatched = (Boolean) (new JSONXObject((JSONObject) thisMetadata.get("nameValuePairs"))).get("of");
                Boolean shouldBeMatched = (thisCaseDirectory.getName().contains("tiktok"));

                // Apply the outcome of the case to the confusion matrix
                thisTest.confusionMatrix.add(
                        (((shouldBeMatched == wasMatched) ? "T" : "F")  + ((wasMatched) ? "P" : "N")),
                        thisCaseDirectory.getName());
            }
        }

        // Print the confusion matrix results
        thisTest.confusionMatrix.print();
    }

    @Test
    public void testTikTokComprehensiveReading() {
        IsolatedTest thisTest = new IsolatedTest("facebookComprehensiveReading");
        File tempDirectory = new File(thisTest.outputDirectory, "temp");
        File thisScreenRecordingFile = new File(testScreenRecordingsDirectory, "facebook_dark_hq_large_slow_inapp_nonav_1.mp4"); // TODO
        writeToJSON(new File(thisTest.outputDirectory, "output.json"), tiktokComprehensiveReading(testContext, tempDirectory,
                thisScreenRecordingFile, getVideoMetadataMachine, frameGrabMachine));
    }
}
