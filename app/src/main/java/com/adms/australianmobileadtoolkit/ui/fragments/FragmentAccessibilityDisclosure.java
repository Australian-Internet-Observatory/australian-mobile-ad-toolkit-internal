package com.adms.australianmobileadtoolkit.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.adms.australianmobileadtoolkit.R;

public class FragmentAccessibilityDisclosure extends Fragment {


    private static final String TAG = "FragmentAccessibilityDisclosure";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_accessibility_disclosure, container, false);


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