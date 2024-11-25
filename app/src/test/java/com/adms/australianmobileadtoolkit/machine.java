package com.adms.australianmobileadtoolkit;

import static com.adms.australianmobileadtoolkit.appSettings.prescribedMinVideoWidth;
import static com.adms.australianmobileadtoolkit.interpreter.FFmpegFrameGrabberAndroid.adjustDimensions;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.createDirectory;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.filenameUnextended;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.logger;
import static com.adms.australianmobileadtoolkit.interpreter.visual.Visual.colourToHex;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Pair;

import androidx.test.platform.app.InstrumentationRegistry;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.stream.IntStream;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {33})
public class machine {

    public static final Context testContext = InstrumentationRegistry.getInstrumentation().getTargetContext();


    public static Function<JSONXObject, JSONXObject> getVideoMetadataMachine = y -> {
        return getVideoMetadataMachine(
                (File) y.get("screenRecordingFile"));
    };


    public static Function<JSONXObject, Bitmap> frameGrabMachine = (y -> {
        // The function retrieves and handles majority of the functionality that then does not
        // require it to have to input certain fields
        return getMP4At(
                (File) y.get("thisScreenRecordingFile"),
                null,
                (Integer) y.get("f"));
    });

    /*
     *
     * Retrieve a bitmap at a given millisecond within a video file (note that this is the mocked version of the function)
     *
     * */
    public static Bitmap getMP4At(File videoFile, Integer timeInMicroseconds, Integer frameNumber) {
        Bitmap thisBitmap = null;
        try {
            // Initialize the pseudo frame grabber

            //avutil.av_log_set_level(avutil.AV_LOG_PANIC);
            FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(videoFile);
            //frameGrabber.setOption();
            frameGrabber.setFormat("mp4");
            frameGrabber.setOption("log_level", "panic");
            frameGrabber.start();
            // Jump to (and grab) the desired frame
            if (timeInMicroseconds !=  null) {
                frameGrabber.setTimestamp((long) timeInMicroseconds);
            } else {
                frameGrabber.setVideoFrameNumber(frameNumber);
            }
            Frame frame = frameGrabber.grab();
            // Run the necessary conversion to adjust the format
            AndroidPseudoFrameConverter convertToBitmap = new AndroidPseudoFrameConverter();
            Bitmap bitmap = convertToBitmap.convert(frame);

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            // The original bitmap container is corrupted slightly to make all RGB values half of their absolute values // TODO - correct this
            thisBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);

            for (Integer xx : IntStream.range(0, bitmap.getWidth()).toArray()) {
                for (Integer yy : IntStream.range(0, bitmap.getHeight()).toArray()) {
                    thisBitmap.setPixel(xx, yy, Color.parseColor(colourToHex(bitmap.getPixel(xx, yy))));
                }
            }
            // Run resizing (if necessary);
            Pair<Integer, Integer> adjustedDimensions = adjustDimensions(width, height, prescribedMinVideoWidth);
            if (width < prescribedMinVideoWidth) {
                thisBitmap = Bitmap.createScaledBitmap(thisBitmap, adjustedDimensions.first, adjustedDimensions.second, false);
                return thisBitmap;
            }
            frameGrabber.stop();
            return thisBitmap;
        } catch (Exception e) {
            return null;
        }
    }


    public static JSONXObject getVideoMetadataMachine(File thisVideoFile) {
        Integer METADATA_KEY_DURATION = null;
        Integer METADATA_KEY_VIDEO_FRAME_COUNT = null;
        Double METADATA_KEY_DURATION_IN_SECONDS = null;
        Double METADATA_KEY_CAPTURE_FRAMERATE = null;
        Integer METADATA_KEY_IMAGE_WIDTH = null;
        Integer METADATA_KEY_IMAGE_HEIGHT = null;
        try {
            FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(thisVideoFile);
            frameGrabber.setFormat("mp4");
            frameGrabber.start();
            METADATA_KEY_IMAGE_WIDTH = frameGrabber.getImageWidth();
            METADATA_KEY_IMAGE_HEIGHT = frameGrabber.getImageHeight();
            METADATA_KEY_DURATION = Math.toIntExact(frameGrabber.getLengthInTime());
            METADATA_KEY_DURATION_IN_SECONDS = (double) (METADATA_KEY_DURATION / (double) (1000 * 1000)); //
            METADATA_KEY_VIDEO_FRAME_COUNT = Math.toIntExact(frameGrabber.getLengthInVideoFrames());
            METADATA_KEY_CAPTURE_FRAMERATE = frameGrabber.getFrameRate();
            frameGrabber.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }

        JSONXObject output = new JSONXObject();
        output.set("METADATA_KEY_DURATION", METADATA_KEY_DURATION);
        output.set("METADATA_KEY_IMAGE_WIDTH", METADATA_KEY_IMAGE_WIDTH);
        output.set("METADATA_KEY_IMAGE_HEIGHT", METADATA_KEY_IMAGE_HEIGHT);
        output.set("METADATA_KEY_VIDEO_FRAME_COUNT", METADATA_KEY_VIDEO_FRAME_COUNT);
        output.set("METADATA_KEY_CAPTURE_FRAMERATE", METADATA_KEY_CAPTURE_FRAMERATE);
        output.set("METADATA_DERIVED_FRAMERATE", METADATA_KEY_VIDEO_FRAME_COUNT / (double) METADATA_KEY_DURATION_IN_SECONDS);

        return output;
    }


    public static File prepareForPlatformInterpretationTest(IsolatedTest thisTest, File x, String testVideo) {
        if ((testVideo == null) || (filenameUnextended(x).equals(testVideo.replace(".mp4", "")))) {

            String xUnextended = filenameUnextended(x);
            logger.info("Running test " + x);

            File debugFile = new File(thisTest.outputDirectory.getAbsolutePath(), xUnextended);
            File adsFromFacebookDirectory = new File(debugFile, "adsToDispatch");

            File simulatedRecordingsDirectory = new File(debugFile, "videos");
            createDirectory(simulatedRecordingsDirectory, true);
            createDirectory(adsFromFacebookDirectory, true);

            File tentativeRecordingFile = new File(simulatedRecordingsDirectory,
                    "unclassified.0.TESTVIDEO.portrait.mp4");

            try {
                Files.copy(Paths.get(x.getAbsolutePath()), Paths.get(tentativeRecordingFile.getAbsolutePath()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return debugFile;
        }
        return null;
    }

    // TODO - currently on the device, an ffmpeg session is opened on every bitmap grab - would it be better to just do a
    // smack bang grab on the entire thing

    // fitter reports functionality of 80%
    // quick reading reports functionality of 70%
    //

    // TODO - trycatch removals on JSONObjects

    // TODO - test Sponsored text reading

    // TODO - quick sample

    // TODO - frame on frame comparison

    // TODO - comprehensive

    // TODO - full run-through
}
