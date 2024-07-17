package com.adms.australianmobileadtoolkit.ui.fragments;

import static com.adms.australianmobileadtoolkit.Settings.USING_DEMOGRAPHIC_QUESTIONS;
import static com.adms.australianmobileadtoolkit.Settings.sharedPreferenceGet;
import static com.adms.australianmobileadtoolkit.Settings.sharedPreferencePut;

import android.content.DialogInterface;
import android.content.res.Configuration;
import android.media.Image;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.adms.australianmobileadtoolkit.R;
import com.adms.australianmobileadtoolkit.ui.AsyncResponse;
import com.adms.australianmobileadtoolkit.ui.Registration;
import com.adms.australianmobileadtoolkit.ui.dialogs.DialogFailedRegistration;
import com.adms.australianmobileadtoolkit.ui.dialogs.DialogLoading;
import com.adms.australianmobileadtoolkit.ui.dialogs.DialogSuccessfulRegistration;

import org.json.JSONException;
import org.json.JSONObject;

public class FragmentRegistration1 extends Fragment implements AsyncResponse {
   Registration asyncTask = new Registration();
   private Button mbuttonIAgree;
   private DialogLoading loadRegistration;

   private ImageView mEthicsVendorLogo;
   private DialogFailedRegistration loadFailedRegistration;
   private Button mbuttonBackFromRegistration1;
   private DialogSuccessfulRegistration loadSuccessfulRegistration;

   @Override
   public void onCreate(Bundle savedInstanceState) {

      //this to set delegate/listener back to this class
      super.onCreate(savedInstanceState);
      asyncTask.delegate = this;
   }

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container,
                            Bundle savedInstanceState) {

         View view = inflater.inflate(R.layout.fragment_registration_1, container, false);

      // Find the vendor logo and set it correctly (depending on the colour scheme)
      mEthicsVendorLogo = (ImageView) view.findViewById(R.id.ethicsVendorLogo);
      switch (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
         case Configuration.UI_MODE_NIGHT_YES: mEthicsVendorLogo.setImageResource(R.drawable.ethics_vendor_logo_light); break;
         case Configuration.UI_MODE_NIGHT_NO: mEthicsVendorLogo.setImageResource(R.drawable.ethics_vendor_logo_dark); break;
      }

      mbuttonIAgree = (Button) view.findViewById(R.id.buttonIAgree);



      mbuttonBackFromRegistration1 = (Button) view.findViewById(R.id.buttonBackFromRegistration1);

      // TODO - comment
      mbuttonIAgree.setOnClickListener(v ->{
         if (USING_DEMOGRAPHIC_QUESTIONS) {
            Fragment fragment = new FragmentRegistration2();

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
         } else {

            loadRegistration = new DialogLoading(requireContext());
            loadRegistration.show();
            // If the registration value is not yet set
            if (!sharedPreferenceGet(requireContext(),"SHARED_PREFERENCE_REGISTERED", "false").equals("true")) {
               JSONObject registrationJSONObject;
               registrationJSONObject = new JSONObject();
               asyncTask.execute(registrationJSONObject);
            }
         }
         //startActivity(new Intent(this, RegistrationActivity.class));
      });
      mbuttonBackFromRegistration1.setOnClickListener(v ->{
         Fragment fragment = new FragmentMain();

         FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
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


      ((TextView)view.findViewById(R.id.fragment_main_privacy_policy_unregistered)).setMovementMethod(LinkMovementMethod.getInstance());

      return view;


   }

   @Override
   public void processFinish(Boolean successfulRegistration) {


      if (successfulRegistration) {
         loadRegistration.dismiss();
         sharedPreferencePut(getContext(), "SHARED_PREFERENCE_REGISTERED", "true");
         loadSuccessfulRegistration = new DialogSuccessfulRegistration(requireContext());
         loadSuccessfulRegistration.show();
         loadSuccessfulRegistration.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
               goBack("FragmentMain");
            }
         });
      } else {
         failedRegistration(loadRegistration);
      }


      FragmentManager fm = getActivity().getSupportFragmentManager();
      for(int i = 0; i < fm.getBackStackEntryCount(); ++i) { fm.popBackStack(); }


   }


   private void goBack(String instance) {
      Fragment fragment = null;
      if (instance.equals("FragmentRegistration1")) {
         fragment = new FragmentRegistration1();
      } else
      if (instance.equals("FragmentMain")) {
         fragment = new FragmentMain();
      }

      FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
      transaction.setCustomAnimations(
              R.anim.enter_from_left,  // enter
              R.anim.exit_to_right,  // exit
              R.anim.enter_from_right,   // popEnter
              R.anim.exit_to_left  // popExit
      );
      assert fragment != null;
      transaction.replace(R.id.fragmentContainerView, fragment);
      transaction.addToBackStack(null);
      transaction.commit();
   }


   private void failedRegistration(DialogLoading loadRegistration) {
      System.out.println( "got here");
      loadRegistration.dismiss();
      loadFailedRegistration = new DialogFailedRegistration(requireContext());
      loadFailedRegistration.show();
      loadFailedRegistration.setOnDismissListener(new DialogInterface.OnDismissListener() {
         @Override
         public void onDismiss(DialogInterface dialog) {
            goBack("FragmentMain");
         }
      });
   }
}