package com.adms.australianmobileadtoolkit.ui.dialogs;

import static com.adms.australianmobileadtoolkit.interpreter.AccessibilityServiceManager.isAccessibilityServiceEnabled;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.adms.australianmobileadtoolkit.R;
import com.adms.australianmobileadtoolkit.interpreter.AccessibilityService;
import com.adms.australianmobileadtoolkit.ui.fragments.FragmentAccessibilityDisclosure;
import com.adms.australianmobileadtoolkit.ui.fragments.FragmentMain;

public class DialogEnableAccessibilityService extends Dialog implements android.view.View.OnClickListener {

    private String TAG = "DialogEnableAccessibilityService";

    private FragmentManager thisFragmentManager;

    public DialogEnableAccessibilityService(@NonNull Context context, @NonNull FragmentManager fragmentManager) {
        super(context);
        thisFragmentManager = fragmentManager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_accessibility_launch);

        Button mbuttonEnableAccessibilityServices = (Button)findViewById(R.id.buttonEnableAccessibilityServices);
        mbuttonEnableAccessibilityServices.setOnClickListener(this);


        Button mbuttonLearnMoreAccessibilityServices = (Button)findViewById(R.id.buttonLearnMoreAccessibilityServices);
        mbuttonLearnMoreAccessibilityServices.setOnClickListener(v ->{
            this.dismiss();
            Fragment fragment = new FragmentAccessibilityDisclosure();

            FragmentTransaction transaction = thisFragmentManager.beginTransaction();
            transaction.setCustomAnimations(
                    R.anim.enter_from_left,  // enter
                    R.anim.exit_to_right,  // exit
                    R.anim.enter_from_right,   // popEnter
                    R.anim.exit_to_left  // popExit
            );
            transaction.replace(R.id.fragmentContainerView, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
        });

        Button mbuttonDismissAccessibilityServices = (Button)findViewById(R.id.buttonDismissAccessibilityServices);
        mbuttonDismissAccessibilityServices.setOnClickListener(v ->{
            this.dismiss();
        });



        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }

    @Override
    public void onClick(View view) {
        if (R.id.buttonEnableAccessibilityServices == view.getId()) {
            if (!isAccessibilityServiceEnabled(getContext(), AccessibilityService.class)) {
                getContext().startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
            dismiss();
        }
    }
}