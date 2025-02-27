package com.adms.australianmobileadtoolkit.interpreter.platform;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;


import com.adms.australianmobileadtoolkit.R;

import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import kotlin.Result;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.jvm.functions.Function1;
import kotlin.sequences.Sequence;
import kotlinx.coroutines.ChildHandle;
import kotlinx.coroutines.ChildJob;
import kotlinx.coroutines.CoroutineName;
import kotlinx.coroutines.DisposableHandle;
import kotlinx.coroutines.InternalCoroutinesApi;
import kotlinx.coroutines.Job;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.FlowCollector;
import kotlinx.coroutines.selects.SelectClause0;

public class ImageClassifier {


    private Continuation<Unit> suspendedWrapper(CoroutineContext context) {
        Continuation<Unit> continuation = new Continuation<>() {
            @NonNull
            @Override
            public CoroutineContext getContext() {
                return context;
            }

            @Override
            public void resumeWith(@NonNull Object o) {
                // TODO
            }
        };
        return continuation;
    }

    public static String TAG = "ImageClassifier";

    private ObjectDetectorHelper classifier;
    private CoroutineContext parentContext;

    public ImageClassifier(Context context) {
        parentContext = EmptyCoroutineContext.INSTANCE;
        Continuation<Unit> setupWrapper = suspendedWrapper(parentContext);
        classifier = new ObjectDetectorHelper(
                context,
                ObjectDetectorHelper.THRESHOLD_DEFAULT,
                ObjectDetectorHelper.MAX_RESULTS_DEFAULT,
                ObjectDetectorHelper.Delegate.CPU,
                ObjectDetectorHelper.Model.EfficientDetLite2);
        classifier.setupObjectDetector(setupWrapper);
    }


    public void detectAndCallback(Bitmap thisBitmap) {


        /*

        THIS HAS TO RUN ON OLDER DEVICES (THIS IS THE FIRST TEST YOU DO)
        *
        * there is instantiation
        *
        * and then after that, the detection event is handed a preconceived set of images
        *
        * it has to run the detections on all of these through the flow collector (one cannot begin until the other ends
        *
        *
        *
        * */

        CoroutineContext coroutineContextA = parentContext.plus(new CoroutineName("coroutineContextA"));
        Continuation<Unit> detectionWrapperA = suspendedWrapper(coroutineContextA);
        FlowCollector<ObjectDetectorHelper.DetectionResult> flowCollector = new FlowCollector<>() {
            @Nullable
            @Override
            public Object emit(ObjectDetectorHelper.DetectionResult detectionResult, @NonNull Continuation<? super Unit> continuation) {
                Log.i(TAG, String.valueOf(detectionResult));
                //classifier.detectImageObject(thisBitmap, 0);
                classifier.detect(thisBitmap, 0, detectionWrapperA);
                return null;
            }
        };

        classifier.detect(thisBitmap, 0, detectionWrapperA);
       // classifier.detect(thisBitmap, 0, detectionWrapperA);
        //classifier.detectImageObject(thisBitmap, 0);
        classifier.getDetectionResult().collect(flowCollector, detectionWrapperA);


    }

    // if it is detecting, it cannot run another detection until the last one is done
}
