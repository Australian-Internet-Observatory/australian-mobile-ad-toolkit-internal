package com.adms.australianmobileadtoolkit.platform.tiktok;

import static com.adms.australianmobileadtoolkit.IsolatedTest.simulationsDirectory;
import static com.adms.australianmobileadtoolkit.IsolatedTest.testScreenRecordingsDirectory;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.JSONArrayToList;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.createDirectory;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.filenameUnextended;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.logger;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.platformInterpretationRoutine;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.printJSON;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.rangesOverlap;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.readJSONFromFile;
import static com.adms.australianmobileadtoolkit.machine.frameGrabMachine;
import static com.adms.australianmobileadtoolkit.machine.getVideoMetadataMachine;
import static com.adms.australianmobileadtoolkit.machine.prepareForPlatformInterpretationTest;
import static com.adms.australianmobileadtoolkit.machine.testContext;

import com.adms.australianmobileadtoolkit.IsolatedTest;
import com.adms.australianmobileadtoolkit.JSONXObject;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {33})
public class interpretation {

    public static JSONXObject screenRecordingsMetadata = new JSONXObject(readJSONFromFile(new File(simulationsDirectory, "screenRecordingsMetadata.json")));


    @Test
    public void testPlatformInterpretationRoutine() {
        //org.bytedeco.javacpp.av_log_set_level(avutil.AV_LOG_QUIET);
        IsolatedTest thisTest = new IsolatedTest("practicalInterpretation");
        thisTest.setCasesDirectory(testScreenRecordingsDirectory);

        // You can run the execution over a single video by setting the value below from null to the name of the file you wish to test,
        // given of course that the file is within the externalAssets/simulations/screenRecordings directory. If the file is set to null,
        // all videos within the directory will be executed upon (which typically takes longer).

        String testVideo = null; // e.g. "facebook_dark_lq_large_slow_exapp_nav_1.mp4";

        Function<File, JSONObject> localRoutine = (x -> {
            File debugFile = prepareForPlatformInterpretationTest(thisTest, x, testVideo);
            if (debugFile != null) {
                printJSON(debugFile);
                platformInterpretationRoutine(testContext, debugFile, getVideoMetadataMachine, frameGrabMachine, false);
            }
            return new JSONObject();
        });

        // In order to determine that your interpretation of the platform has been correctly implemented, you will need to develop a logic in the
        // evaluation routine to examine how many candidate files pass execution. Here you can set the number of true positives, false positives, etc.
        // to construct a confusion matrix over all tests. For an example of evaluation logic relative to Facebook, you can examine the
        // com/adms/australianmobileadtoolkit/platform/facebook/interpretation.java test class

        Function<File, JSONObject> evaluateRoutine = (x -> {
            JSONXObject output = new JSONXObject();

            // TODO

            output.set("tp", 0);
            output.set("fp", 0);
            output.set("fn", 0);
            output.set("tn", 0);

            return output.internalJSONObject;
        });


        thisTest.applyToCases(localRoutine);

        thisTest.evaluateOnCases(evaluateRoutine);
    }
}
