package com.adms.australianmobileadtoolkit.interpreter;

import static com.adms.australianmobileadtoolkit.Common.filePath;

import static java.util.Arrays.asList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;

import com.adms.australianmobileadtoolkit.MainActivity;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.ReturnCode;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class FFmpegPseudoFrameGrabber {
   private static final String TAG = "FFmpegPseudoFrameGrabber";

   private static Integer totalNumberOfFrames;

   public static long getTimeInMilliseconds(Context context, File videoFile) throws IOException {
      try {
         MediaMetadataRetriever retriever = new MediaMetadataRetriever();
         retriever.setDataSource(context, Uri.fromFile(videoFile));
         String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
         long timeInMilliseconds = Long.parseLong(time);
         retriever.release();
         return timeInMilliseconds;
      } catch (Exception e) {}
      return -1;
   }

   /*
    *
    * This method attempts to go to a specific frame within the total frames of the video, relative to
    * a time signature.
    *
    * */
   public static Bitmap getBitmapAtMilliseconds(Context context, File videoFile, long timeInMilliseconds, double scale, int minWidth) {
      Integer milliseconds = Math.min(Math.round(timeInMilliseconds % 1000), 999);
      Integer seconds = (int) Math.floor(timeInMilliseconds/1000);
      Integer minutes = (int) Math.floor(seconds/60);
      Integer hours = (int) Math.floor(minutes/60);

      List<String> units = Arrays.asList(hours, minutes, seconds).stream().map(x->String.format("%02d", x)).collect(Collectors.toList());
      String timeSignature = String.join(":", units)+"."+milliseconds;

      final AtomicReference<Boolean> notifier = new AtomicReference();


      String identifier = UUID.randomUUID().toString();


      File tempBitmapFile = filePath(asList(MainActivity.getMainDir(context).getAbsolutePath(), "ffmpeg_cache", identifier+".png"));
      String command = String.format("-ss %1$s -i %2$s -frames:v 1 %3$s ", timeSignature, videoFile.getAbsolutePath(), tempBitmapFile.getAbsolutePath());
      Log.i(TAG, "Executing FFMPEG command: "+command);
      FFmpegSession session = FFmpegKit.execute(command);
      if (ReturnCode.isSuccess(session.getReturnCode())) {
         Bitmap thisBitmap = BitmapFactory.decodeFile(tempBitmapFile.getAbsolutePath());
         tempBitmapFile.delete();
         try {
            if (thisBitmap == null) return null;
            if ((thisBitmap.getWidth()*scale) < minWidth) {
               thisBitmap = Bitmap.createScaledBitmap(thisBitmap,
                     minWidth,
                     (int)Math.floor(thisBitmap.getHeight()/thisBitmap.getWidth()*minWidth), false);
               return thisBitmap;
            }
            thisBitmap = Bitmap.createScaledBitmap(thisBitmap,
                  Math.max((int)Math.floor(thisBitmap.getWidth()*scale),1),
                  Math.max((int)Math.floor(thisBitmap.getHeight()*scale),1), false);
            return thisBitmap;
         } catch (Exception e) {
            return null;
         }
      } else if (ReturnCode.isCancel(session.getReturnCode())) {
         // TODO
      } else {
         Log.d(TAG, String.format("Command failed with state %s and rc %s.%s", session.getState(), session.getReturnCode(), session.getFailStackTrace()));
      }


      /*(if (status == RETURN_CODE_SUCCESS) {
         Bitmap thisBitmap = BitmapFactory.decodeFile(tempBitmapFile.getAbsolutePath());
         tempBitmapFile.delete();
         if (thisBitmap == null) return null;
         if ((thisBitmap.getWidth()*scale) < minWidth) {
            thisBitmap = Bitmap.createScaledBitmap(thisBitmap,
                  minWidth,
                  (int)Math.floor(thisBitmap.getHeight()/thisBitmap.getWidth()*minWidth), false);
            return thisBitmap;
         }
         thisBitmap = Bitmap.createScaledBitmap(thisBitmap,
               Math.max((int)Math.floor(thisBitmap.getWidth()*scale),1),
               Math.max((int)Math.floor(thisBitmap.getHeight()*scale),1), false);
         return thisBitmap;
      }*/
      return null;
   }
}
