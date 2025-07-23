package com.adms.australianmobileadtoolkit.ui.fragments;

import static com.adms.australianmobileadtoolkit.Common.dataStoreRead;
import static com.adms.australianmobileadtoolkit.Common.dataStoreWrite;
import static com.adms.australianmobileadtoolkit.appSettings.logMessage;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.adms.australianmobileadtoolkit.R;
import com.adms.australianmobileadtoolkit.logging.Logging;
import com.adms.australianmobileadtoolkit.ui.dialogs.DialogLoading;
import com.adms.australianmobileadtoolkit.ui.dialogs.DialogOutcome;

import java.lang.ref.WeakReference;

public class FragmentAppSettings extends Fragment {


    private static final String TAG = "FragmentAppSettings";

    private Dialog processLogsLoading = null;
    private Dialog processLogsSuccess = null;
    private Dialog processLogsFailure = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        CheckBox checkboxOverrideDefaultAdDetectionSettings = view.findViewById(R.id.checkboxOverrideDefaultAdDetectionSettings);

        if (dataStoreRead(requireContext(), "overrideDefaultAdDetectionSettings", "false").equals("true")) {
            checkboxOverrideDefaultAdDetectionSettings.setChecked(true);
        } else {
            checkboxOverrideDefaultAdDetectionSettings.setChecked(false);
        }

        checkboxOverrideDefaultAdDetectionSettings.setOnCheckedChangeListener((compoundButton, b) -> {
            dataStoreWrite(requireContext(), "overrideDefaultAdDetectionSettings", ((b) ? "true" : "false"));
        });

        Button mbuttonBackToMain = (Button) view.findViewById(R.id.buttonBackToMain);
        mbuttonBackToMain.setOnClickListener(v ->{
            Fragment fragment = new FragmentMain();

            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.setCustomAnimations(
                    R.anim.enter_from_right,  // enter
                    R.anim.exit_to_left,  // exit
                    R.anim.enter_from_left,   // popEnter
                    R.anim.exit_to_right  // popExit
            );
            transaction.replace(R.id.fragmentContainerView, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
        });

        Button mbuttonProcessLogs = (Button) view.findViewById(R.id.buttonProcessLogs);
        mbuttonProcessLogs.setOnClickListener(v ->{
            processLogsLoading = new DialogLoading(requireContext());
            processLogsLoading.setOnDismissListener((l)->{ l = null; });
            processLogsLoading.create();
            processLogsLoading.show();

            WeakReference<Activity> thisActivity = new WeakReference<>(getActivity());
            (new Thread(() -> {
                boolean success = true;
                try {
                    Thread.sleep(1000); // Apply a timeout so that the dialog displays long enough to be registered by the users
                } catch (InterruptedException e) {
                    success = false;
                    throw new RuntimeException(e);
                }
                try {
                    success = Logging.dispatchLogRoutine(this.requireContext());
                } catch ( Exception e) {
                    success = false;
                    e.printStackTrace();
                }

                // TODO - do up a confirmation dialog on this
                // TODO - run testing of fields
                processLogsLoading.dismiss();
                boolean finalSuccess = success;
                Activity thisActivityGot = thisActivity.get();
                if (thisActivityGot != null) {
                    thisActivityGot.runOnUiThread(()-> {
                        DialogOutcome thisOutcome = new DialogOutcome(
                                getContext(),
                                R.drawable.tick_icon,
                                R.string.dialog_logs_success_title,
                                R.string.dialog_logs_success_description,
                                R.string.dialog_logs_success_back,
                                true);
                        thisOutcome.create();
                        if (!finalSuccess) {
                            thisOutcome.setIcon(R.drawable.cross_icon);
                            thisOutcome.setTitle(R.string.dialog_logs_error_title);
                            thisOutcome.setDescription(R.string.dialog_logs_error_description);
                            thisOutcome.setDismiss(R.string.dialog_logs_error_back);
                        }
                        thisOutcome.show();
                    });
                }

            })).start();
        });


        return view;


    }
}