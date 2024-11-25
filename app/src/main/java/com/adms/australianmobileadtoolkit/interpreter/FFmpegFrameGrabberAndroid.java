package com.adms.australianmobileadtoolkit.interpreter;

import static com.adms.australianmobileadtoolkit.Common.filePath;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.logger;
import static java.util.Arrays.asList;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;
import com.adms.australianmobileadtoolkit.JSONXObject;
import com.adms.australianmobileadtoolkit.MainActivity;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.FFprobeKit;
import com.arthenica.ffmpegkit.FFprobeSession;
import com.arthenica.ffmpegkit.ReturnCode;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FFmpegFrameGrabberAndroid {
   private static final String TAG = "FFmpegFrameGrabberAndroid";


   public static Function<JSONXObject, Bitmap> frameGrabAndroid = (x -> getBitmapAtFrame(
           (Context) x.get("context"),
           (File) x.get("thisScreenRecordingFile"),
           (Integer) x.get("f"),
           (Integer) x.get("videoFrames"),
           (Integer) x.get("videoDuration"),
           (Integer) x.get("minWidth")));


   public static Function<JSONXObject, JSONXObject> getVideoMetadataAndroid = x -> getVideoMetadata(
           (Context) x.get("context"),
           (File) x.get("screenRecordingFile"));

   public static JSONXObject getVideoMetadata(Context context, File videoFile) {
      JSONXObject output = new JSONXObject();
      MediaMetadataRetriever retriever = new MediaMetadataRetriever();
      if (android.os.Build.VERSION.SDK_INT >= 28) {
         retriever.setDataSource(context, Uri.fromFile(videoFile));
      } else {
         retriever.setDataSource(videoFile.getAbsolutePath());
      }
      Integer METADATA_KEY_DURATION = null;
      Integer METADATA_KEY_VIDEO_FRAME_COUNT = null;
      Integer METADATA_KEY_CAPTURE_FRAMERATE = null;


      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
         METADATA_KEY_VIDEO_FRAME_COUNT = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT));
         METADATA_KEY_DURATION = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
         METADATA_KEY_CAPTURE_FRAMERATE = (int) Math.floor(Double.parseDouble(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)));
      } else {

         FFprobeSession session = FFprobeKit.execute("-v quiet -select_streams v:0 -count_packets -show_entries stream=nb_read_packets -of csv=p=0 "+videoFile.getAbsolutePath());
         String result = session.getOutput();
         if (!ReturnCode.isSuccess(session.getReturnCode())) {
            Log.d(TAG, "Command failed. Please check output for the details.");
         } else {
            result = result.replaceAll("[^0-9]", "");
            METADATA_KEY_VIDEO_FRAME_COUNT = Integer.parseInt(result);
         }

         Double METADATA_KEY_DURATION_RETAINED = 0.0;
         session = FFprobeKit.execute("-i "+videoFile.getAbsolutePath()+" -show_entries format=duration -v quiet -of csv=\"p=0\"");
         result = session.getOutput();
         if (!ReturnCode.isSuccess(session.getReturnCode())) {
            Log.d(TAG, "Command failed. Please check output for the details.");
         } else {
            result = result.replaceAll("[^0-9\\.]", "");
            METADATA_KEY_DURATION_RETAINED = Double.parseDouble(result);
            METADATA_KEY_DURATION = (int) Math.floor(METADATA_KEY_DURATION_RETAINED*1000);
         }

         METADATA_KEY_CAPTURE_FRAMERATE = (int) Math.floor(METADATA_KEY_VIDEO_FRAME_COUNT / METADATA_KEY_DURATION_RETAINED);

      }

      Integer METADATA_KEY_DURATION_IN_SECONDS = (int) (METADATA_KEY_DURATION / (double) 1000);

      output.set("METADATA_KEY_DURATION", METADATA_KEY_DURATION);
      output.set("METADATA_KEY_DURATION_IN_SECONDS", METADATA_KEY_DURATION_IN_SECONDS);
      output.set("METADATA_KEY_VIDEO_FRAME_COUNT", METADATA_KEY_VIDEO_FRAME_COUNT);
      output.set("METADATA_KEY_CAPTURE_FRAMERATE", METADATA_KEY_CAPTURE_FRAMERATE);
      Double derivedFrameRate = 0.0;
      try {
         derivedFrameRate = METADATA_KEY_VIDEO_FRAME_COUNT / (double) METADATA_KEY_DURATION_IN_SECONDS;
         if (derivedFrameRate.isInfinite()) {
            derivedFrameRate = 30.0;
         }
      } catch (Exception e) {
         derivedFrameRate = 30.0;
      }
      output.set("METADATA_DERIVED_FRAMERATE",derivedFrameRate );
      Log.i(TAG, String.valueOf(output));
      return output;
   }

   public static Bitmap getBitmapAtFrame(Context context, File videoFile, Integer thisFrame, Integer totalFrameCount, Integer durationInMilliseconds, int minWidth) {
      String timeSignature = generateTimeSignature((long) Math.floor((thisFrame / (double) totalFrameCount) * durationInMilliseconds));
      return bitmapPassthrough(context, timeSignature, videoFile, minWidth);
   }

   public static String generateTimeSignature(long timeInMilliseconds) {
      Integer milliseconds = Math.min(Math.round(timeInMilliseconds % 1000), 999);
      Integer seconds = (int) Math.floor(timeInMilliseconds/1000);
      Integer minutes = (int) Math.floor(seconds/60);
      Integer hours = (int) Math.floor(minutes/60);

      List<String> units = Arrays.asList(hours, minutes, seconds).stream().map(x->String.format("%02d", x)).collect(Collectors.toList());
      String timeSignature = String.join(":", units)+"."+milliseconds;
      return timeSignature;
   }

   public static Pair<Integer, Integer> adjustDimensions(Integer width, Integer height, Integer minWidth) {
      Integer adjustedWidth = width;
      Integer adjustedHeight = height;
      if (adjustedWidth < minWidth) {
         adjustedWidth = minWidth;
         adjustedHeight = (int) Math.floor(adjustedWidth * (height / (double) width));
      }
      return new Pair<>(adjustedWidth, adjustedHeight);
   }

   public static Bitmap bitmapPassthrough(Context context, String timeSignature, File videoFile, int minWidth) {
      String identifier = UUID.randomUUID().toString();

      int width = Resources.getSystem().getDisplayMetrics().widthPixels;
      int height = Resources.getSystem().getDisplayMetrics().heightPixels;

      Pair<Integer, Integer> adjustedDimensions = adjustDimensions(width, height, minWidth);

      File tempBitmapFile = filePath(asList(MainActivity.getMainDir(context).getAbsolutePath(), "ffmpeg_cache", identifier+".bmp"));
      String command = String.format("-ss %1$s -i %2$s -update true -frames:v 1 -s "+adjustedDimensions.first+"x"+adjustedDimensions.second+" %3$s -hide_banner -loglevel panic ", timeSignature, videoFile.getAbsolutePath(), tempBitmapFile.getAbsolutePath());
      FFmpegSession session = FFmpegKit.execute(command);
      if (ReturnCode.isSuccess(session.getReturnCode())) {
         Bitmap thisBitmap = BitmapFactory.decodeFile(tempBitmapFile.getAbsolutePath());
         tempBitmapFile.delete();
         try {
            if (thisBitmap == null) return null;
            return thisBitmap;
         } catch (Exception e) {
            // It's expected that this may occasionally produce an error (for instance, when the last or near last frame
            // doesn't exist)
            logger.error(e);
            return null;
         }
      } else if (ReturnCode.isCancel(session.getReturnCode())) {
         // TODO
      } else {
         Log.d(TAG, String.format("Command failed with state %s and rc %s.%s", session.getState(), session.getReturnCode(), session.getFailStackTrace()));
      }
      return null;
   }
}
