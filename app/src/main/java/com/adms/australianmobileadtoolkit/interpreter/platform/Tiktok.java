package com.adms.australianmobileadtoolkit.interpreter.platform;

import static com.adms.australianmobileadtoolkit.appSettings.DEBUG;
import static com.adms.australianmobileadtoolkit.appSettings.prescribedMinVideoWidth;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.createDirectory;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.logger;
import static com.adms.australianmobileadtoolkit.interpreter.platform.Platform.saveBitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.Pair;

import com.adms.australianmobileadtoolkit.JSONXObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Tiktok {

    public static JSONObject tiktokGenerateQuickReading(Context context, Boolean DEBUG, File debugDirectory, File screenRecordingFile,
                                                          Function<JSONXObject, JSONXObject> functionGetVideoMetadata, Function<JSONXObject, Bitmap> frameGrabFunction) {

        // TODO

        return new JSONObject();
    }



    public static JSONObject tiktokComprehensiveReading(Context context,
                                                          File tempDirectory, File thisScreenRecordingFile, Function<JSONXObject, JSONXObject> videoMetadataFunction,
                                                          Function<JSONXObject, Bitmap> frameGrabFunction) {

        // TODO

        return new JSONObject();
    }


    public static void tiktokInterpretation(Context context, File appStorageRecordingsDirectory, HashMap<String, String> thisInterpretation,
                                            File rootDirectory, Function<JSONXObject, JSONXObject> getVideoMetadataFunction,
                                              Function<JSONXObject, Bitmap> frameGrabFunction, Boolean implementedOnAndroid,
                                            File adsFromDispatchDirectory, JSONObject fitterFacebookAdHeader, HashMap<String, Object> pictogramsReference) {
        // TODO
    }
}
