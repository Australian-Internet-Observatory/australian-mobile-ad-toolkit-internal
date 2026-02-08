package com.adms.australianmobileadtoolkit.ui.fragments;

import static com.adms.australianmobileadtoolkit.Common.dataStoreRead;
import static com.adms.australianmobileadtoolkit.Common.dataStoreWrite;
import static com.adms.australianmobileadtoolkit.ui.OnDemandAssetPackInstaller.getInstalledAssetsPath;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.adms.australianmobileadtoolkit.R;
import com.adms.australianmobileadtoolkit.ui.OnDemandAssetPackInstaller;
import com.adms.australianmobileadtoolkit.ui.dialogs.DialogLoading;

import java.util.Arrays;
import java.util.HashMap;

public class FragmentSetupLanguage extends Fragment {

    private Dialog setUpLanguageLoading = null;

    private static final String TAG = "FragmentSetupLanguage";

    private final HashMap<String, String> languageToAssetPackMap = new HashMap<>();

    public FragmentSetupLanguage() {
        languageToAssetPackMap.put("LANGUAGE_ENGLISH", "models-en");
        languageToAssetPackMap.put("LANGUAGE_VIETNAMESE", "models-vn");
        languageToAssetPackMap.put("LANGUAGE_PORTUGUESE", "models-pt");
    }

    public void goToMain() {
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
    }

    public void setLanguageSettingRoutine(String languageSetting) {
        dataStoreWrite(requireContext(), "appLanguage", languageSetting);

        setUpLanguageLoading = new DialogLoading(requireContext());
        setUpLanguageLoading.setOnDismissListener((l)->{ l = null; });
        setUpLanguageLoading.create();
        setUpLanguageLoading.show();
        // TODO - download routine

        // If the asset pack is not installed, download it...


        String thisAssetPack = languageToAssetPackMap.get(languageSetting);

        retrieveAssetPack(requireActivity(), requireContext(), thisAssetPack);
    }


    public void retrieveAssetPack(Activity thisActivity, Context thisContext, String assetPackName) {
        if (getInstalledAssetsPath(thisContext, assetPackName) == null) {
            OnDemandAssetPackInstaller pad = new OnDemandAssetPackInstaller(thisContext, assetPackName);
            pad.fetchIfNeeded(thisActivity, new OnDemandAssetPackInstaller.Callback() {
                @Override public void onProgress(int percent) {
                    // Update UI (progress bar / text)
                    Log.i(TAG, "Asset pack '"+assetPackName+"' download completion: "+String.valueOf(percent));
                }

                @Override public void onReady(@NonNull String assetsPath) {
                    // Example: assetsPath points to the pack’s assets root
                    // If you placed "models/model.tflite" inside the pack:
                    //java.io.File model = new java.io.File(assetsPath, "models/model.tflite");
                    // Load / use it…
                    Log.i(TAG, "Asset pack is downloaded, proceeding to Main...");
                    if (setUpLanguageLoading != null) {
                        setUpLanguageLoading.dismiss();
                    }
                    goToMain();
                }

                @Override public void onError(@NonNull String message, int errorCode) {
                    // Show error + retry option
                    Log.e(TAG, message);
                }
            });
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_setup_language, container, false);
        Button mbuttonSetLanguageEnglish = (Button) view.findViewById(R.id.buttonSetLanguageEnglish);
        Button mbuttonSetLanguagePortuguese = (Button) view.findViewById(R.id.buttonSetLanguagePortuguese);
        Button mbuttonSetLanguageVietnamese = (Button) view.findViewById(R.id.buttonSetLanguageVietnamese);


        // If the language has already been set, navigate to the main fragment
        String appLanguage = dataStoreRead(requireContext(), "appLanguage", "NULL");
        if (!appLanguage.equals("NULL")) {


            // Do a soft-evaluation of the necessary asset pack

            String thisAssetPack = languageToAssetPackMap.get(appLanguage);

            if (getInstalledAssetsPath(requireContext(), thisAssetPack) == null) {
                retrieveAssetPack(requireActivity(), requireContext(), thisAssetPack);
            } else {
                goToMain();
            }



        } else {

            // Or else set the button click event listeners

            mbuttonSetLanguageEnglish.setOnClickListener(v ->{
                setLanguageSettingRoutine("LANGUAGE_ENGLISH");
            });

            mbuttonSetLanguageVietnamese.setOnClickListener(v ->{
                setLanguageSettingRoutine("LANGUAGE_VIETNAMESE");
            });

            mbuttonSetLanguagePortuguese.setOnClickListener(v ->{
                setLanguageSettingRoutine("LANGUAGE_PORTUGUESE");
            });
        }


        /*
        */
        return view;


    }
}