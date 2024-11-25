package com.adms.australianmobileadtoolkit.platform.facebook;

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

        String testVideo = "facebook_dark_lq_large_slow_exapp_nav_1.mp4";

        Function<File, JSONObject> localRoutine = (x -> {
            File debugFile = prepareForPlatformInterpretationTest(thisTest, x, testVideo);
            if (debugFile != null) {
                printJSON(debugFile);
                platformInterpretationRoutine(testContext, debugFile, getVideoMetadataMachine, frameGrabMachine, false);
            }
            return new JSONObject();
        });

        Function<File, JSONObject> evaluateRoutine = (x -> {
            JSONXObject output = new JSONXObject();
            String xUnextended = filenameUnextended(x);

            File debugDirectory = new File(thisTest.outputDirectory.getAbsolutePath(), xUnextended);
            File adsFromFacebookDirectory = new File(debugDirectory, "adsToDispatch");
            // Retrieve the adContent file

            JSONXObject thisVideoMetadata = getVideoMetadataMachine(x);
            Integer frameHeight = (Integer) thisVideoMetadata.get("METADATA_KEY_IMAGE_HEIGHT");
            Double frameAreaBound = 0.05; // A fraction of the image's center area will be regarded as the center bound

            // This hashmap will record determinations of ads within frames
            HashMap<String, Boolean> adWithinFrameAccounted = new HashMap<>();
            List<String> falseAds = new ArrayList<>();

            JSONXObject thisTestObject = (new JSONXObject((JSONObject) screenRecordingsMetadata.get(xUnextended)));

            List<JSONXObject> prescribedAdsForTest = JSONArrayToList((JSONArray) thisTestObject.get("ads")).stream()
                    .map(y -> new JSONXObject((JSONObject) y) ).collect(Collectors.toList());

            Integer nPosts = (Integer) thisTestObject.get("nPosts");
            nPosts = (nPosts == null) ? 0 : nPosts;

            for (JSONXObject thisPrescribedAd : prescribedAdsForTest) {
                // Initiate all as false
                adWithinFrameAccounted.put((String) thisPrescribedAd.get("identifier"), false);
            }

            // For all ads determined on this recording...
            if (adsFromFacebookDirectory.listFiles() != null) {
                for (File thisTentativeAdDirectory : Objects.requireNonNull(adsFromFacebookDirectory.listFiles())) {
                    if (thisTentativeAdDirectory.isDirectory()) {
                        Boolean tentativeAdIsAbsoluteAd = false;
                        for (JSONXObject thisPrescribedAd : prescribedAdsForTest) {
                            Double frameHeightDivisor = 0.5;
                            switch ((String) thisPrescribedAd.get("occupyingAreaOnScreen")) {
                                case "top" : frameHeightDivisor = 0.0; break ;
                                case "center" : frameHeightDivisor = 0.5; break ;
                                case "bottom" : frameHeightDivisor = 1.0; break ;
                            }
                            Integer frameBoundHalf = ((int) Math.floor((frameHeight * frameAreaBound) / (double) 2));
                            Integer frameLowerBound = ((int) Math.floor(frameHeight * frameHeightDivisor)) - frameBoundHalf;
                            Integer frameUpperBound = ((int) Math.floor(frameHeight * frameHeightDivisor)) + frameBoundHalf;

                            List<List<Integer>> framesTruthfullyHavingAd =
                                    JSONArrayToList((JSONArray) thisPrescribedAd.get("adPositions"))
                                            .stream().map(y -> JSONArrayToList((JSONArray) y).stream().map(z -> (int) z).collect(Collectors.toList())).collect(Collectors.toList());

                            logger.info(thisTentativeAdDirectory.getAbsolutePath());

                            JSONXObject xxx = (new JSONXObject(readJSONFromFile(new File(thisTentativeAdDirectory, "adContent.json"))));
                            JSONXObject thisAdContent = (new JSONXObject((JSONObject) xxx.get("nameValuePairs")));

                            // Identify the ad's boundaries within the frameSnippetIDsByOffsetChain and compare them to the
                            // presets for this frame
                            JSONXObject boundariesForAdByFrame = (JSONXObject) thisAdContent.get(
                                    "frameSnippetIDsByOffsetChain", "nameValuePairs", "frameSnippetIDsByOffsetChains",
                                    thisAdContent.get("thisAdOffsetChainID").toString(), thisAdContent.get("thisAdFrameSnippetID").toString(),
                                    "nameValuePairs", "boundaries");

                            // If any of the desired frames feature the tentative ad...
                            for (List<Integer> fList : framesTruthfullyHavingAd) {
                                // If frames overlap
                                for (Integer identifyingFrame : boundariesForAdByFrame.keys().stream().map(Integer::parseInt).collect(Collectors.toList())) {
                                    // If the frame bounds set by the test criteria match those that were identified by the ad detector
                                    if (rangesOverlap(fList.get(0), fList.get(1), identifyingFrame, identifyingFrame) != 0) {
                                        // If within the bounding frame the dimensions of the ad overlap those of the test criteria
                                        List<Integer> boundsForFrame = JSONArrayToList((JSONArray) boundariesForAdByFrame.get(identifyingFrame.toString()))
                                                .stream().map(y -> (int) y).collect(Collectors.toList());
                                        if (rangesOverlap(boundsForFrame.get(0), boundsForFrame.get(1), frameLowerBound, frameUpperBound) != 0) {
                                            tentativeAdIsAbsoluteAd = true;
                                            adWithinFrameAccounted.put((String) thisPrescribedAd.get("identifier"), true);
                                        }
                                    }
                                }
                            }

                        }
                        // If this ad doesn't match a desired ad, then its a false positive
                        if ((!tentativeAdIsAbsoluteAd)) {
                            falseAds.add(thisTentativeAdDirectory.getName());
                        }
                        logger.info("\tadIdentifiedWithinDesiredFrame: " + tentativeAdIsAbsoluteAd);
                    }
                }

                // Open the OCR data for this test, and subtract all ads therein that
                // do not resolve from the false positives.
                JSONXObject metadataOCR = (new JSONXObject(readJSONFromFile(new File(x, "metadataOCR.json"))));
                falseAds = falseAds.stream().filter(z -> metadataOCR.has(z) && ((Boolean) metadataOCR.get(z))).collect(Collectors.toList());

                Integer nTruePositives = adWithinFrameAccounted.keySet().stream().filter(k -> adWithinFrameAccounted.get(k)).collect(Collectors.toList()).size();
                Integer nFalseNegatives = adWithinFrameAccounted.keySet().stream().filter(k -> (!adWithinFrameAccounted.get(k))).collect(Collectors.toList()).size();
                Integer nFalsePositives = falseAds.size();
                // Here, the true negatives are the total number of posts minus all other post types
                Integer nTrueNegatives = Math.max(0, nPosts - (nTruePositives + nFalseNegatives + nFalsePositives));

                output
                        .set("tp", nTruePositives)
                        .set("fn", nFalseNegatives)
                        .set("fp", nFalsePositives)
                        .set("tn", nTrueNegatives)
                        .set("falseAds", falseAds);

                thisTest.confusionMatrix.tpN += nTruePositives;
                thisTest.confusionMatrix.fpN += nFalsePositives;
                thisTest.confusionMatrix.tnN += nTrueNegatives;
                thisTest.confusionMatrix.fnN += nFalseNegatives;
            }

            return output.internalJSONObject;
        });


        thisTest.applyToCases(localRoutine); // TODO

        // OCR evaluation happens here

        thisTest.evaluateOnCases(evaluateRoutine);

        logger.info("True Positives: " + thisTest.confusionMatrix.tpN);
        logger.info("False Positives: " + thisTest.confusionMatrix.fpN);
        logger.info("True Negatives: " + thisTest.confusionMatrix.tnN);
        logger.info("False Negatives: " + thisTest.confusionMatrix.fnN);
    }
}
