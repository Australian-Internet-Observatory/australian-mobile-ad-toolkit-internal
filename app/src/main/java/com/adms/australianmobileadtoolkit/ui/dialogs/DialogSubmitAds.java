package com.adms.australianmobileadtoolkit.ui.dialogs;

import static com.adms.australianmobileadtoolkit.Common.dataStoreRead;
import static com.adms.australianmobileadtoolkit.Common.dataStoreWrite;
import static com.adms.australianmobileadtoolkit.MainActivity.PERIODIC_WORK_TAG;
import static com.adms.australianmobileadtoolkit.MainActivity.manualAdDigestThread;
import static com.adms.australianmobileadtoolkit.appSettings.logMessage;
import static com.adms.australianmobileadtoolkit.interpreter.InterpreterWorker.platformInterpretationRoutineContainer;
import static com.adms.australianmobileadtoolkit.interpreter.detector.ObjectDetector.objectDetectorAndroid;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.adms.australianmobileadtoolkit.MainActivity;
import com.adms.australianmobileadtoolkit.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Arrays;
import java.util.List;

public class DialogSubmitAds extends Dialog implements android.view.View.OnClickListener {

    private static String TAG = "DialogSubmitAds";

    private DialogLoading loadKillAdDigest;


    public DialogSubmitAds(@NonNull Context context, @NonNull FragmentManager fragmentManager) {
        super(context);
        if (context instanceof Activity) {
            setOwnerActivity((Activity) context);
        }
        //thisDialogSubmitAds = this;
    }

    public void runThis() {
        getOwnerActivity().getParent().runOnUiThread(()-> {
            logMessage(TAG, "Hellop");
        });
    }

    public void killPeriodicWorker(Context context) {
        if (isPeriodicWorkerRunning(context)) {
            //WorkManager.getInstance(context.getApplicationContext()).cancelAllWorkByTag(PERIODIC_WORK_TAG);
            //WorkManager.getInstance(context.getApplicationContext()).cancelAllWorkByTag(MANUAL_WORK_TAG);
            //WorkManager.getInstance(context.getApplicationContext()).cancelAllWork();
            WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork("workName");
            logMessage(TAG, "killPeriodicWorker iteration");

        }
    }

    public Thread constructProcessThread(Context context) {
        return new Thread(() -> platformInterpretationRoutineContainer(context, true));
    }

    public static Integer safeIntegerRead(Context context, String key) {
        try {
            return Integer.parseInt(dataStoreRead(context, key, "0"));
        } catch (Exception e) {}
        return 0;
    }

    public static void pollAdDigest(DialogSubmitAds instance, Context context) {
        // Set a thread to attempt to refresh the dialog when the process is complete
        if (instance != null) {
            (new Thread(() -> {
                instance.getOwnerActivity().runOnUiThread(()-> {
                    ((TextView) instance.findViewById(R.id.dialog_submit_ads_processing_annotation)).setText("Your ads are now processing...");
                    ((RelativeLayout) instance.findViewById(R.id.loadingPanel)).setVisibility(View.VISIBLE);
                    ((Button) instance.findViewById(R.id.buttonExitProcessMyAdDigest)).setVisibility(View.VISIBLE);
                    ((ImageView) instance.findViewById(R.id.process_ad_digest_complete_icon)).setVisibility(View.GONE);
                    ((ProgressBar) instance.findViewById(R.id.progress_bar_processing)).setProgress(0);
                    ((TextView) instance.findViewById(R.id.progress_bar_processing_text)).setText("STARTING: 0%");

                });

                Integer platformRoutineToAnalyze = 0;
                Integer platformRoutineAnalyzed = 0;

                Integer platformRoutineRelayed = 0;
                Integer platformRoutineToRelay = 0;
                Integer progressReadingAnalyzed = 0;
                Integer progressReadingRelayed = 0;
                while (instance.adDigestIsRunning(instance.getOwnerActivity().getApplicationContext())) {


                    try {
                        logMessage(TAG, "Sleeping on adDigestProcess...");

                        String tentativePlatformRoutineState = dataStoreRead(context, "platformRoutineState", "LOADING");

                        platformRoutineToAnalyze = safeIntegerRead(context, "platformRoutineToAnalyze");
                        platformRoutineAnalyzed = safeIntegerRead(context, "platformRoutineAnalyzed");

                        platformRoutineRelayed = safeIntegerRead(context, "platformRoutineRelayed");
                        platformRoutineToRelay = safeIntegerRead(context, "platformRoutineToRelay");



                        if (!((platformRoutineRelayed.equals(platformRoutineToRelay)) && (platformRoutineToRelay.equals(0)))) {
                            try {

                                progressReadingRelayed = Math.min(100, Math.toIntExact(Math.round(((double) platformRoutineRelayed / platformRoutineToRelay) * 100.0)));
                            } catch (Exception e) {

                            }
                        } else {
                            progressReadingRelayed = 100;
                        }


                        if (!((platformRoutineToAnalyze.equals(platformRoutineAnalyzed)) && (platformRoutineToAnalyze.equals(0)))) {
                            try {

                                progressReadingAnalyzed = Math.min(100, Math.toIntExact(Math.round(((double) platformRoutineAnalyzed / platformRoutineToAnalyze) * 100.0)));
                            } catch (Exception e) {

                            }
                        } else {
                            progressReadingAnalyzed = 0;
                        }

                        String formalPlatformRoutineState;
                        if ((!(tentativePlatformRoutineState == null)) && (!(tentativePlatformRoutineState.equals("COMPLETE")))) {
                            formalPlatformRoutineState = tentativePlatformRoutineState;
                        } else {
                            formalPlatformRoutineState = "LOADING";
                        }

                        final Integer progressReadingAnalyzedF = progressReadingAnalyzed;
                        final Integer progressReadingRelayedF = progressReadingRelayed;

                        instance.getOwnerActivity().runOnUiThread(()-> {

                            ((TextView) instance.findViewById(R.id.dialog_submit_ads_processing_status)).setText("STATUS: " + formalPlatformRoutineState);

                            Integer progressReadingApplied = 0;
                            String progressReadingAnnotation = "ANALYSED";
                            if (Arrays.asList("STARTING", "SAMPLING IMAGERY", "PERFORMING ANALYSIS").contains(formalPlatformRoutineState)) {
                                progressReadingApplied = progressReadingAnalyzedF;
                                progressReadingAnnotation = "ANALYSED";


                            } else
                            if (Arrays.asList("RELAYING DATA", "COMPLETE").contains(formalPlatformRoutineState)) {
                                progressReadingApplied = progressReadingRelayedF;
                                progressReadingAnnotation = "RELAYED";

                            }

                            ((TextView) instance.findViewById(R.id.progress_bar_processing_text)).setText(progressReadingAnnotation+": " + progressReadingApplied.toString() + "%");
                            ProgressBar progressBarAnalyzed = ((ProgressBar) instance.findViewById(R.id.progress_bar_processing));
                            progressBarAnalyzed.startAnimation((new ProgressBarAnimation(progressBarAnalyzed, progressBarAnalyzed.getProgress(), progressReadingApplied)));

                        });

                        Thread.currentThread().sleep(1000);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }



                instance.getOwnerActivity().runOnUiThread(()-> {
                    ((TextView) instance.findViewById(R.id.dialog_submit_ads_processing_status)).setText("STATUS: COMPLETE" );
                    ((TextView) instance.findViewById(R.id.progress_bar_processing_text)).setText("100%");
                    ProgressBar progressBarAnalyzed = ((ProgressBar) instance.findViewById(R.id.progress_bar_processing));
                    progressBarAnalyzed.startAnimation((new ProgressBarAnimation(progressBarAnalyzed, progressBarAnalyzed.getProgress(), 100)));
                });
                try {
                    Thread.currentThread().sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                logMessage(TAG, "Complete with process...");

                final Integer platformRoutineToAnalyzeFinal = platformRoutineToAnalyze;

                instance.getOwnerActivity().runOnUiThread(()-> {
                    //instance.refreshDialog(context);
                    ((TextView) instance.findViewById(R.id.dialog_submit_ads_processing_annotation)).setText("A total of "+platformRoutineToAnalyzeFinal+" files have been processed.");
                    ((Button) instance.findViewById(R.id.buttonExitProcessMyAdDigest)).setVisibility(View.GONE);
                    ((RelativeLayout) instance.findViewById(R.id.loadingPanel)).setVisibility(View.GONE);
                    ((ImageView) instance.findViewById(R.id.process_ad_digest_complete_icon)).setVisibility(View.VISIBLE);



                });
            })).start();
        }
    }

    public boolean attemptProcessOnThread(Context context) {
        if (!adDigestIsRunning(context)) {
            manualAdDigestThread = constructProcessThread(context);
            manualAdDigestThread.start();
            refreshDialog(context);


            return true;
        }
        return false;
    }

    public boolean isManualProcessRunning() {
        if (manualAdDigestThread == null) {
            return false;
        } else {
            return manualAdDigestThread.isAlive();
        }
    }

    public void killThread() {
        try {
            if (isManualProcessRunning()) {
                logMessage(TAG, "XXX - Manual thread was interrupted...");
                manualAdDigestThread.interrupt();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void forceThreadDeath() {
        try {
            if (isManualProcessRunning()) {
                logMessage(TAG, "XXX - Forcing thread death...");
                manualAdDigestThread.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void killCurrentThread() {
        try {
            if ((manualAdDigestThread != null) && (manualAdDigestThread.isAlive())) {
                manualAdDigestThread.interrupt();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean adDigestIsRunning(Context context) {
        logMessage(TAG, "XXX - isManualProcessRunning:"+isPeriodicWorkerRunning(context));
        return ((isPeriodicWorkerRunning(context)) || (isManualProcessRunning()));
    }

    public boolean isPeriodicWorkerRunning(Context context) {
        boolean periodicWorkerIsRunning = false;
        try {
            List<WorkInfo> thisPeriodicWorkerInfo = WorkManager.getInstance(context).getWorkInfosByTag(PERIODIC_WORK_TAG).get();
            periodicWorkerIsRunning = thisPeriodicWorkerInfo.stream().anyMatch(x -> x.getState() == WorkInfo.State.RUNNING);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return periodicWorkerIsRunning;
    }

    public void killAdDigestProcess(Context context) {
        getOwnerActivity().runOnUiThread(()-> {
            this.hide();
            loadKillAdDigest = new DialogLoading(context);
            loadKillAdDigest.setOnDismissListener((l)->{
                l = null;
            });
            loadKillAdDigest.create();
            loadKillAdDigest.show();
        });

        (new Thread(() -> {
            killThread();
            killPeriodicWorker(context);

            while (adDigestIsRunning(context)) {
                try {
                    logMessage(TAG, "Sleeping on killAdDigestProcesss...");
                    Thread.currentThread().sleep(1000);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            logMessage(TAG, "Complete with 'kill' process...");

            getOwnerActivity().runOnUiThread(()-> {
                loadKillAdDigest.dismiss();
                this.show();
                refreshDialog(context);
            });
        })).start();
    }


    public void refreshDialog(Context context) {
        if (!adDigestIsRunning(context)) {
            this.findViewById(R.id.dialog_submit_ads_process_ad_digest).setVisibility(View.VISIBLE);
            this.findViewById(R.id.dialog_submit_ads_processing_ad_digest).setVisibility(View.GONE);

            // Process My Ads Button
            Button mbuttonProcessMyAdDigest = (Button)this.findViewById(R.id.buttonProcessMyAdDigest);
            mbuttonProcessMyAdDigest.setOnClickListener((v) -> {
                attemptProcessOnThread(v.getContext());
            });

            // Go Back Button
            Button mbuttonGoBack = (Button)this.findViewById(R.id.buttonGoBackFromProcessMyAdDigest);
            mbuttonGoBack.setOnClickListener(v ->{
                this.dismiss();
            });


        } else {
            this.findViewById(R.id.dialog_submit_ads_process_ad_digest).setVisibility(View.GONE);
            this.findViewById(R.id.dialog_submit_ads_processing_ad_digest).setVisibility(View.VISIBLE);

            // Cancel process
            Button mbuttonExitProcessMyAdDigest = (Button)this.findViewById(R.id.buttonExitProcessMyAdDigest);
            mbuttonExitProcessMyAdDigest.setOnClickListener(v ->{
                if (adDigestIsRunning(getContext())) {
                    logMessage(TAG, "XXY - CANCEL PROCESS IN EFFECT");
                    killAdDigestProcess(getContext());
                }
            });

            // Go Back Button
            Button mbuttonGoBack = (Button)this.findViewById(R.id.buttonGoBackFromProcessMyAdDigestAlternative);
            mbuttonGoBack.setOnClickListener(v ->{
                this.dismiss();
            });

            pollAdDigest(this, context);


        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_submit_ads);
        //Button mbuttonEnableAccessibilityServices = (Button)findViewById(R.id.buttonEnableAccessibilityServices);
        //mbuttonEnableAccessibilityServices.setOnClickListener(this);

        refreshDialog(getContext());

        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }

    @Override
    public void onClick(View view) {
        /*
        if (R.id.buttonEnableAccessibilityServices == view.getId()) {
            if (!isAccessibilityServiceEnabled(getContext(), AccessibilityService.class)) {
                getContext().startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
            dismiss();
        }*/
    }
}