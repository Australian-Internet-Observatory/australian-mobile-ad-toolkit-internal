package com.adms.australianmobileadtoolkit;

import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.createDirectory;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.filePath;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.filenameUnextended;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.writeToJSON;

import static java.util.Arrays.asList;

import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;
import java.util.function.Function;

public class IsolatedTest {
    public static final File externalAssetsDirectory = (new File(String.valueOf(filePath(asList(((new File(".")).getAbsolutePath()),
            "..", "..", "externalAssets")))));
    public static final File simulationsDirectory = (new File(externalAssetsDirectory, "simulations"));
    public static final File testScreenRecordingsDirectory = (new File(simulationsDirectory, "screenRecordings"));
    public File inputDirectory;
    public File outputDirectory;
    public File casesDirectory;
    public confusionMatrix confusionMatrix = new confusionMatrix();
    public static final File isolatedTestsDirectory = (new File(simulationsDirectory.getAbsolutePath(), "tests"));
    
    public IsolatedTest(String thisTestType) {
        File testDirectory = new File(isolatedTestsDirectory, thisTestType);
        inputDirectory = new File(testDirectory, "input");
        outputDirectory = new File(testDirectory, "output");
        casesDirectory = new File(inputDirectory, "cases");
        // (Re)Instantiate the necessary directories for the test
        createDirectory(testDirectory, false);
        createDirectory(outputDirectory, false);
    }

    public void setCasesDirectory(File casesDirectory) {
        this.casesDirectory = casesDirectory;
    }

    public void applyToCases(Function<File, JSONObject> localRoutine) {
        Arrays.stream(casesDirectory.listFiles()).toList().forEach(thisTestCaseFile -> {
            if (!thisTestCaseFile.getName().startsWith(".")) {
                String thisTestCase = filenameUnextended(thisTestCaseFile);
                // Generate the output directory for this test
                File thisTestCaseOutputDirectory = new File(outputDirectory, thisTestCase);
                createDirectory(thisTestCaseOutputDirectory, true);
                // Run the local routine
                JSONObject result = localRoutine.apply(thisTestCaseFile);
                // Write the output
                File metadataFile = new File(thisTestCaseOutputDirectory, "metadata.json");
                writeToJSON(metadataFile, result);
            }
        });
    }
    public void evaluateOnCases(Function<File, JSONObject> localRoutine) {
        Arrays.stream(casesDirectory.listFiles()).toList().forEach(thisTestCaseFile -> {
            if (!thisTestCaseFile.getName().startsWith(".")) {
                String thisTestCase = filenameUnextended(thisTestCaseFile);
                File thisTestCaseOutputDirectory = new File(outputDirectory, thisTestCase);
                JSONObject result = localRoutine.apply(thisTestCaseFile);
                // Write the output
                File metadataFile = new File(thisTestCaseOutputDirectory, "metadataEvaluation.json");
                writeToJSON(metadataFile, result);
            }
        });
    }
}
