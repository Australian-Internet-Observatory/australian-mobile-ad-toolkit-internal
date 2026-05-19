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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.adms.australianmobileadtoolkit.R;
import com.adms.australianmobileadtoolkit.ui.OnDemandAssetPackInstaller;
import com.adms.australianmobileadtoolkit.ui.dialogs.DialogLoadingProgress;
import com.adms.australianmobileadtoolkit.ui.dialogs.ProgressBarAnimation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

public class FragmentSetupLanguage extends Fragment {

    private DialogLoadingProgress setUpLanguageLoading = null;

    private static final String TAG = "FragmentSetupLanguage";

    private String instanceFragmentCase;

    public static FragmentSetupLanguage newInstance(String thisFragmentCase) {
        FragmentSetupLanguage f = new FragmentSetupLanguage();
        Bundle args = new Bundle();
        args.putString("FRAGMENT_CASE", thisFragmentCase);
        f.setArguments(args);
        return f;
    }

    public void goToMain(String languageSetting) {

        Locale current =
                getResources().getConfiguration().getLocales().get(0);

        Log.d(TAG, "UI locale now: " + current.toLanguageTag());

        if (languageSetting != null) {
            String languageCode = languageSetting.toLowerCase(Locale.ROOT);
            Log.i(TAG, "Setting language to " + languageCode);
            if (setUpLanguageLoading != null) {
                setUpLanguageLoading.dismiss();
            }
            dataStoreWrite(requireContext(), "pendingMainRecreate", "TRUE");
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode));
            requireActivity().recreate();

        } else {

            Fragment fragment = new FragmentMain();
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.setCustomAnimations(0, 0, 0, 0);
            transaction.replace(R.id.fragmentContainerView, fragment);
            getParentFragmentManager().popBackStack(null, getParentFragmentManager().POP_BACK_STACK_INCLUSIVE);
            transaction.commit();
            /**/

        }
    }

    public void assetPackRetrievalRoutine(String thisLanguage) {
        retrieveAssetPack(requireActivity(), requireContext(), "models"+thisLanguage,
                () -> retrieveAssetPack(requireActivity(), requireContext(), "modelsquantized"+thisLanguage, () -> {
                    if (setUpLanguageLoading != null) {
                        setUpLanguageLoading.dismiss();
                    }
                    goToMain(thisLanguage);
                }));
    }

    public void setLanguageSettingRoutine(String languageSetting) {
        Log.i(TAG, "Writing language to " + languageSetting);
        dataStoreWrite(requireContext(), "appLanguage", languageSetting);

        setUpLanguageLoading = new DialogLoadingProgress(requireContext());
        setUpLanguageLoading.setOnDismissListener((l)->{ l = null; });
        setUpLanguageLoading.create();
        setUpLanguageLoading.show();
        // If the asset pack is not installed, download it...
        assetPackRetrievalRoutine(languageSetting);
    }


    public void retrieveAssetPack(Activity thisActivity, Context thisContext, String assetPackName, Runnable callback) {
        if (getInstalledAssetsPath(thisContext, assetPackName) == null) {
            OnDemandAssetPackInstaller pad = new OnDemandAssetPackInstaller(thisContext, assetPackName);
            pad.fetchIfNeeded(thisActivity, new OnDemandAssetPackInstaller.Callback() {
                @Override public void onProgress(int percent) {
                    // Update UI (progress bar / text)
                    Log.i(TAG, "Asset pack '"+assetPackName+"' download completion: "+String.valueOf(percent));
                    setUpLanguageLoading.setProgressLoading(percent);
                }

                @Override public void onReady(@NonNull String assetsPath) {
                    // Example: assetsPath points to the pack’s assets root
                    // If you placed "models/model.tflite" inside the pack:
                    //java.io.File model = new java.io.File(assetsPath, "models/model.tflite");
                    // Load / use it…
                    callback.run();
                }

                @Override public void onError(@NonNull String message, int errorCode) {
                    // Show error + retry option
                    Log.e(TAG, message);
                }
            });
        } else {
            callback.run();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Boolean buttonsAreActionable = false;
        Bundle args = getArguments();
        if (args != null) {
            instanceFragmentCase = args.getString("FRAGMENT_CASE");
        }

        View view = inflater.inflate(R.layout.fragment_setup_language, container, false);
        Button mbuttonSetLanguageEnglish = (Button) view.findViewById(R.id.buttonSetLanguageEnglish);
        Button mbuttonSetLanguagePortuguese = (Button) view.findViewById(R.id.buttonSetLanguagePortuguese);
        Button mbuttonSetLanguageVietnamese = (Button) view.findViewById(R.id.buttonSetLanguageVietnamese);


        // If the language has already been set, navigate to the main fragment
        String appLanguage = dataStoreRead(requireContext(), "appLanguage", "NULL");
        if (!appLanguage.equals("NULL")) {


            // Do a soft-evaluation of the necessary asset pack

            Log.i(TAG, "appLanguage: "+appLanguage);
            Log.i(TAG, "Asset pack is detected!");

            if (getInstalledAssetsPath(requireContext(), "models"+appLanguage) == null) {
                assetPackRetrievalRoutine(appLanguage);
            } else {
                if ((instanceFragmentCase != null) && (instanceFragmentCase.equals("SETTINGS"))) {
                    Log.i(TAG, "Within settings - undertake no bypass");
                    buttonsAreActionable = true;
                } else {
                    Log.i(TAG, "Startup screen - bypass to main!");
                    goToMain(null);
                }
            }



        } else {
            // Or else set the button click event listeners
            Log.i(TAG, "Asset pack is not detected!");
            buttonsAreActionable = true;
        }


        if (buttonsAreActionable) {
            mbuttonSetLanguageEnglish.setOnClickListener(v -> setLanguageSettingRoutine("EN"));

            mbuttonSetLanguageVietnamese.setOnClickListener(v -> setLanguageSettingRoutine("VI"));

            mbuttonSetLanguagePortuguese.setOnClickListener(v -> setLanguageSettingRoutine("PT"));
        }

        return view;


    }
}