package com.adms.australianmobileadtoolkit.ui.dialogs;

import static com.adms.australianmobileadtoolkit.Common.dataStoreRead;
import static com.adms.australianmobileadtoolkit.Common.dataStoreWrite;
import static com.adms.australianmobileadtoolkit.interpreter.FFmpegFrameGrabberAndroid.frameGrabAndroid;
import static com.adms.australianmobileadtoolkit.interpreter.FFmpegFrameGrabberAndroid.getVideoMetadataAndroid;
import static com.adms.australianmobileadtoolkit.interpreter.InterpreterWorker.canForcePlatformInterpretation;
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
import androidx.fragment.app.FragmentManager;
import androidx.work.WorkManager;

import com.adms.australianmobileadtoolkit.MainActivity;
import com.adms.australianmobileadtoolkit.R;

import java.util.Arrays;

public class DialogSubmitAds extends Dialog implements android.view.View.OnClickListener {

    private static String TAG = "DialogSubmitAds";
    private Thread tentativeThread;

    private DialogLoading loadKillAdDigest;
    private DialogSubmitAds thisDialogSubmitAds;

    Activity ownerActivity;

    public DialogSubmitAds(@NonNull Context context, @NonNull FragmentManager fragmentManager) {
        super(context);
        if (context instanceof Activity) {
            setOwnerActivity((Activity) context);
        }
        //thisDialogSubmitAds = this;
    }

    public void runThis() {
        getOwnerActivity().getParent().runOnUiThread(()-> {
            Log.i(TAG, "Hellop");
        });
    }

    public void killPeriodicWorker(Context context) {
        if (isPeriodicWorkerRunning(context)) {
            //WorkManager.getInstance(context.getApplicationContext()).cancelAllWorkByTag(PERIODIC_WORK_TAG);
            //WorkManager.getInstance(context.getApplicationContext()).cancelAllWorkByTag(MANUAL_WORK_TAG);
            //WorkManager.getInstance(context.getApplicationContext()).cancelAllWork();
            WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork("workName");
            Log.i(TAG, "killPeriodicWorker iteration");

        }
        dataStoreWrite(context, "periodicWorkerRunning", "false");
        dataStoreWrite(context, "platformRoutineRunning", "false");
    }

    public Thread constructProcessThread(Context context) {
        return new Thread(() -> platformInterpretationRoutineContainer(context));
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
                instance.ownerActivity.runOnUiThread(()-> {
                    ((TextView) instance.findViewById(R.id.dialog_submit_ads_processing_annotation)).setText("Your ads are now processing...");
                    ((RelativeLayout) instance.findViewById(R.id.loadingPanel)).setVisibility(View.VISIBLE);
                    ((Button) instance.findViewById(R.id.buttonExitProcessMyAdDigest)).setVisibility(View.VISIBLE);
                    ((ImageView) instance.findViewById(R.id.process_ad_digest_complete_icon)).setVisibility(View.GONE);
                    ((ProgressBar) instance.findViewById(R.id.progress_bar_processing)).setProgress(0);
                    ((TextView) instance.findViewById(R.id.progress_bar_processing_text)).setText("STARTING: 0%");

                });

                // If we can force a platform interpretation
                if (canForcePlatformInterpretation(context)) {
                    // Exit the current lock
                    instance.killAdDigestProcess(context);
                }

                Integer platformRoutineToAnalyze = 0;
                Integer platformRoutineAnalyzed = 0;

                Integer platformRoutineRelayed = 0;
                Integer platformRoutineToRelay = 0;
                Integer progressReadingAnalyzed = 0;
                Integer progressReadingRelayed = 0;
                while (instance.adDigestIsRunning(instance.getOwnerActivity().getApplicationContext())) {


                    try {
                        Log.i(TAG, "Sleeping on adDigestProcess...");

                        String tentativePlatformRoutineState = dataStoreRead(context, "platformRoutineState", "false");

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

                        instance.ownerActivity.runOnUiThread(()-> {

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



                instance.ownerActivity.runOnUiThread(()-> {
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

                Log.i(TAG, "Complete with process...");

                final Integer platformRoutineToAnalyzeFinal = platformRoutineToAnalyze;

                instance.ownerActivity.runOnUiThread(()-> {
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
            tentativeThread = constructProcessThread(context);
            tentativeThread.start();
            refreshDialog(context);


            return true;
        }
        return false;
    }

    public boolean isThreadAlive() {
        if (tentativeThread == null) {
            return false;
        } else {
            return tentativeThread.isAlive();
        }
    }

    public void killThread() {
        try {
            if (isThreadAlive()) {
                tentativeThread.interrupt();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean adDigestIsRunning(Context context) {
        return ((isPeriodicWorkerRunning(context)) || (isThreadAlive()) || (isRoutineRunning(context)));
    }

    public boolean isPeriodicWorkerRunning(Context context) {
        return (dataStoreRead(context, "periodicWorkerRunning", "false").equals("true"));
    }

    public boolean isRoutineRunning(Context context) {
        return (dataStoreRead(context, "platformRoutineRunning", "false").equals("true"));
    }

    public void killAdDigestProcess(Context context) {
        ownerActivity.runOnUiThread(()-> {
            thisDialogSubmitAds.hide();
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
                    Log.i(TAG, "Sleeping on killAdDigestProcesss...");
                    Thread.currentThread().sleep(1000);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            Log.i(TAG, "Complete with 'kill' process...");

            ownerActivity.runOnUiThread(()-> {
                loadKillAdDigest.dismiss();
                thisDialogSubmitAds.show();
                refreshDialog(context);
            });
        })).start();
    }


    public void refreshDialog(Context context) {
        if (!adDigestIsRunning(context)) {
            thisDialogSubmitAds.findViewById(R.id.dialog_submit_ads_process_ad_digest).setVisibility(View.VISIBLE);
            thisDialogSubmitAds.findViewById(R.id.dialog_submit_ads_processing_ad_digest).setVisibility(View.GONE);

            // Process My Ads Button
            Button mbuttonProcessMyAdDigest = (Button)thisDialogSubmitAds.findViewById(R.id.buttonProcessMyAdDigest);
            mbuttonProcessMyAdDigest.setOnClickListener((v) -> {
                attemptProcessOnThread(v.getContext());
            });

            // Go Back Button
            Button mbuttonGoBack = (Button)thisDialogSubmitAds.findViewById(R.id.buttonGoBackFromProcessMyAdDigest);
            mbuttonGoBack.setOnClickListener(v ->{
                thisDialogSubmitAds.dismiss();
            });


        } else {
            thisDialogSubmitAds.findViewById(R.id.dialog_submit_ads_process_ad_digest).setVisibility(View.GONE);
            thisDialogSubmitAds.findViewById(R.id.dialog_submit_ads_processing_ad_digest).setVisibility(View.VISIBLE);

            // Cancel process
            Button mbuttonExitProcessMyAdDigest = (Button)thisDialogSubmitAds.findViewById(R.id.buttonExitProcessMyAdDigest);
            mbuttonExitProcessMyAdDigest.setOnClickListener(v ->{
                if (adDigestIsRunning(getContext())) {
                    killAdDigestProcess(getContext());
                }
            });

            // Go Back Button
            Button mbuttonGoBack = (Button)thisDialogSubmitAds.findViewById(R.id.buttonGoBackFromProcessMyAdDigestAlternative);
            mbuttonGoBack.setOnClickListener(v ->{
                thisDialogSubmitAds.dismiss();
            });

            pollAdDigest(this, context);


        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_submit_ads);

        ownerActivity = getOwnerActivity();

        thisDialogSubmitAds = this;

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