/*
*
* This class deals with the direct analysis of video content (frame by frame), to identify instances
* where the individual is using their Facebook app, and then furthermore, if they are observing
* sponsored advertisement content. In such cases, the content is then submitted to an AWS Lambda
* endpoint
*
* */

package com.adms.australianmobileadtoolkit.interpreter;

import static com.adms.australianmobileadtoolkit.interpreter.FFmpegFrameGrabberAndroid.frameGrabAndroid;
import static com.adms.australianmobileadtoolkit.interpreter.FFmpegFrameGrabberAndroid.getVideoMetadataAndroid;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.platformInterpretationRoutine;

import android.annotation.SuppressLint;
import android.content.Context;

import com.adms.australianmobileadtoolkit.MainActivity;
import java.io.File;

public class Interpreter {
    private static final String TAG = "Interpreter";
    public static File rootDirectoryPath;
    private Context thisContext;


    /*
     *
     * This method initialises an instance of the Interpreter class
     *
     * */
    @SuppressLint("ResourceType")
    public Interpreter(Context context){
        // The rootDirectoryPath variable must be initialised here, to access the app context
        rootDirectoryPath = MainActivity.getMainDir(context);
        thisContext = context;
    }


    // TODO - make instance have some bearing over controllability of battery optimisation
    public void run(String instance) {
        platformInterpretationRoutine(thisContext, rootDirectoryPath, getVideoMetadataAndroid, frameGrabAndroid, true);
    }

}
