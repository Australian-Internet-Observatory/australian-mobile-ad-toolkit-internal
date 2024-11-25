package com.adms.australianmobileadtoolkit.platform.facebook;

import static com.adms.australianmobileadtoolkit.interpreter.platform.Facebook.blue;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Facebook.fitterGenerate;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Facebook.isFacebookAdHeader;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Facebook.red;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Facebook.retrieveReferenceStencilsPictograms;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.readJSONFromFile;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Facebook.reservedAbsoluteWhitespaceColourDark;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Facebook.reservedAbsoluteWhitespaceColourLight;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.saveBitmap;
import static com.adms.australianmobileadtoolkit.machine.testContext;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;

import com.adms.australianmobileadtoolkit.IsolatedTest;
import com.adms.australianmobileadtoolkit.JSONXObject;
import com.adms.australianmobileadtoolkit.R;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {33})

public class adHeaderDetection {

    /*
     *
     * This function visualizes the fitter as a Bitmap
     *
     * */
    public static void fitterVisualizeAsBitmap(JSONObject thisFitter, File outputFile) {
        Integer w = null;
        Integer h = null;
        List<List<Integer>> negatives = new ArrayList<>();
        List<List<Integer>> positives = new ArrayList<>();
        try {
            w = (Integer) thisFitter.get("w");
            h = (Integer) thisFitter.get("h");
            negatives = (List<List<Integer>>) thisFitter.get("negatives");
            positives = (List<List<Integer>>) thisFitter.get("positives");
        } catch (Exception e) { }
        Bitmap outputBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        for (List<Integer> thisCoord : negatives) {
            outputBitmap.setPixel(thisCoord.get(0), thisCoord.get(1), red);
        }
        for (List<Integer> thisCoord : positives) {
            outputBitmap.setPixel(thisCoord.get(0), thisCoord.get(1), blue);
        }
        saveBitmap(outputBitmap, outputFile.getAbsolutePath());
    }

    @Test
    public void testFacebookAdHeaderDetection() {
        IsolatedTest thisTest = new IsolatedTest("facebookAdHeaderDetection");

        Boolean useCustomStencil = false; // Apply the testMedia stencil
        Boolean visualizeStencil = true; // Visualize the stencil during execution

        // Load in the stencil
        Bitmap appliedStencil;
        if (useCustomStencil) {
            // Test-case stencil
            File customStencilFile = new File(thisTest.inputDirectory, "stencil.png");
            appliedStencil = BitmapFactory.decodeFile(customStencilFile.getAbsolutePath());
        } else {
            // Stored stencil
            appliedStencil = ((BitmapDrawable) testContext.getResources().getDrawable(
                    R.drawable.facebook_ad_header_stencil)).getBitmap();
        }

        // Generate the fitter
        JSONObject thisFitter = fitterGenerate(appliedStencil);

        // Visualize the stencil
        if (visualizeStencil) {
            File stencilFacebookPostVisualisedFile = new File(thisTest.outputDirectory,
                    "stencil-visualised.png");
            fitterVisualizeAsBitmap(thisFitter, stencilFacebookPostVisualisedFile);
        }

        // Load in the pictogramReference
        HashMap<String, Object> pictogramsReference = retrieveReferenceStencilsPictograms(testContext);

        // Define the local routine for the test cases
        Function<File, JSONObject> localRoutine = x -> {
            // Retrieve the relative image
            Bitmap testImage = BitmapFactory.decodeFile(x.getAbsolutePath());

            // Get the exposure type
            String exposureType = ((x.getAbsolutePath().contains("dark")) ? "dark" : "light");
            Integer wsColor = (exposureType.equals("light")) ? reservedAbsoluteWhitespaceColourLight : reservedAbsoluteWhitespaceColourDark;
            // Generate the result
            return isFacebookAdHeader(testImage, thisFitter, pictogramsReference, wsColor,  exposureType);
        };

        thisTest.applyToCases(localRoutine);

        for (File thisCaseDirectory : thisTest.outputDirectory.listFiles()) {
            if (thisCaseDirectory.isDirectory()) {
                JSONXObject thisMetadata = new JSONXObject(readJSONFromFile(new File(thisCaseDirectory, "metadata.json")));

                Boolean shouldBeMatched = (thisCaseDirectory.getName().contains("facebookAd"));
                Boolean wasMatched = ((new JSONXObject((JSONObject) thisMetadata.get("nameValuePairs"))).get("outcome")).equals("MATCHED");

                // Apply the outcome of the case to the confusion matrix
                thisTest.confusionMatrix.add(
                        (((shouldBeMatched == wasMatched) ? "T" : "F")  + ((wasMatched) ? "P" : "N")),
                        thisCaseDirectory.getName());
            }
        }

        // Print the confusion matrix results
        thisTest.confusionMatrix.print();
    }

}
