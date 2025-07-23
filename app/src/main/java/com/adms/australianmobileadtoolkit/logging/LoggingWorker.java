package com.adms.australianmobileadtoolkit.logging;

import static com.adms.australianmobileadtoolkit.appSettings.logMessage;
import static com.adms.australianmobileadtoolkit.logging.Logging.dispatchLogRoutine;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class LoggingWorker extends Worker {

    private static String TAG = "LoggingWorker";

    public LoggingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        logMessage(TAG, "Attempting to dispatch logs...");
        dispatchLogRoutine(getApplicationContext());
        return null;
    }
}
