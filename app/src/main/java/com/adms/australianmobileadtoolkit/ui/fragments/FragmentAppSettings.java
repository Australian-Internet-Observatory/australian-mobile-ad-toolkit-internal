package com.adms.australianmobileadtoolkit.ui.fragments;

import static com.adms.australianmobileadtoolkit.Common.dataStoreRead;
import static com.adms.australianmobileadtoolkit.Common.dataStoreWrite;

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

public class FragmentAppSettings extends Fragment {


    private static final String TAG = "FragmentAppSettings";

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
        return view;


    }
}