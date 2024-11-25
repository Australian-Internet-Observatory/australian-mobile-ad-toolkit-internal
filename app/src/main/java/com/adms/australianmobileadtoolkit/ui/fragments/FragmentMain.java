package com.adms.australianmobileadtoolkit.ui.fragments;

import static com.adms.australianmobileadtoolkit.RecorderService.createIntentForScreenRecording;
import static com.adms.australianmobileadtoolkit.appSettings.DEBUG;
import static com.adms.australianmobileadtoolkit.appSettings.SHARED_PREFERENCE_REGISTERED_DEFAULT_VALUE;
import static com.adms.australianmobileadtoolkit.appSettings.sharedPreferenceGet;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.adms.australianmobileadtoolkit.MainActivity;
import com.adms.australianmobileadtoolkit.RecorderService;
import com.adms.australianmobileadtoolkit.ui.ItemViewModel;
import com.adms.australianmobileadtoolkit.R;
import com.adms.australianmobileadtoolkit.interpreter.Interpreter;

import java.util.Objects;

public class FragmentMain extends Fragment {
   // The tag of this class
   private static final String TAG = "FragmentMain";
   // The SwitchCompat toggler used with screen-recording
   private static Switch mToggleButton;
   // TODO - comment
   private Button mregisterButton;
   // De-bouncer variable on toggler
   private static boolean mToggleButtonDebouncerActivated;
   // The registration status of the user
   public static boolean THIS_REGISTRATION_STATUS = false;

   private static ItemViewModel viewModel;

   public void goToRegistration() {
      Fragment fragment = new FragmentRegistration1();

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

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container,
                            Bundle savedInstanceState) {
      THIS_REGISTRATION_STATUS = (!Objects.equals(sharedPreferenceGet(
            getActivity(), "SHARED_PREFERENCE_REGISTERED", SHARED_PREFERENCE_REGISTERED_DEFAULT_VALUE), SHARED_PREFERENCE_REGISTERED_DEFAULT_VALUE));
      // TODO Auto-generated method stub

      View view = inflater.inflate(R.layout.fragment_main, container, false);

      // Attach the variable mToggleButton to the control within the view
      mToggleButton = (Switch) view.findViewById(R.id.simpleSwitch);
      // TODO // - comment
      mregisterButton = (Button) view.findViewById(R.id.buttonRegister);
      // Initialise the de-bouncer on the mToggleButton control
      mToggleButtonDebouncerActivated = false;
      // Apply a listener to the mToggleButton control, to execute the onToggleScreenShare method
      mToggleButton.setOnClickListener(v -> {
         if (((Switch)view.findViewById(R.id.simpleSwitch)).isChecked()) {
            // ask for permission to capture screen and act on result after
            // TODO - this is the part that caused the intent error
            // TODO - move to separate function

            createIntentForScreenRecording(getActivity());
            Log.v(TAG, "Screen-recording has started");
         } else {
            Log.v(TAG, "Screen-recording has stopped");
            getActivity().stopService(new Intent(getActivity(), RecorderService.class));
         }
         mToggleButtonDebouncerActivated = true;
      });
      // TODO - comment
      mregisterButton.setOnClickListener(v ->{
         goToRegistration();
         //startActivity(new Intent(this, RegistrationActivity.class));
      });

      ((TextView)view.findViewById(R.id.fragment_main_learn_more)).setMovementMethod(LinkMovementMethod.getInstance());
      ((TextView)view.findViewById(R.id.fragment_main_privacy_policy)).setMovementMethod(LinkMovementMethod.getInstance());
      ((TextView)view.findViewById(R.id.fragment_main_learn_more_unregistered)).setMovementMethod(LinkMovementMethod.getInstance());
      ((TextView)view.findViewById(R.id.fragment_main_privacy_policy_unregistered)).setMovementMethod(LinkMovementMethod.getInstance());

      // If the device is registered
      if (THIS_REGISTRATION_STATUS) { // NB: This can be inverted for testing purposes - default is (!THIS_REGISTRATION_STATUS)
         // Hide the 'unregistered' screen
         view.findViewById(R.id.fragment_main_unregistered).setVisibility(View.GONE);

         Button buttonDashboard = (Button) view.findViewById(R.id.buttonDashboard);
         buttonDashboard.setOnClickListener(v ->{
            Fragment fragment = new FragmentDashboard();

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
            //startActivity(new Intent(this, RegistrationActivity.class));
         });

      } else {
         // Or else hide the 'registered' screen
         view.findViewById(R.id.fragment_main_registered).setVisibility(View.GONE);
      }

      if (DEBUG) { // TODO - clean
         Button btn = new Button(getActivity());
         btn.setText("RUN DEBUG");
         LinearLayout.MarginLayoutParams x = new LinearLayout.MarginLayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
               LinearLayout.LayoutParams.WRAP_CONTENT);
         x.setMargins(0,(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics()), 0, 0);
         btn.setLayoutParams(new LinearLayout.LayoutParams(x));
         btn.setBackgroundColor(Color.rgb(252, 3, 44));
         LinearLayout ll = (THIS_REGISTRATION_STATUS) ?
               view.findViewById(R.id.fragment_main_registered) : view.findViewById(R.id.fragment_main_unregistered);
         ll.addView(btn);
         ll.invalidate();
         btn.setOnClickListener(view1 -> {
            Thread thread = new Thread(() -> {
               Interpreter lManager = new Interpreter(getActivity());
               //try {
                  lManager.run("DETECTION");
             //  } catch (JSONException e) {
//throw new RuntimeException(e);
             //  }
            });
            thread.start();
         });
      }
      return view;


   }

   public static void setToggle(Boolean check) {
      try {
         if (!mToggleButtonDebouncerActivated) {
            mToggleButton.setChecked(check);
         }
      } catch (Exception e) {

      }

      // Then deactivate the de-bouncer in case (as we are resuming the app)
      mToggleButtonDebouncerActivated = false;
   }

   public static void safelySetToggleFromViewModel() {
      if (viewModel != null) {
         Boolean thisValue = viewModel.getToggleStatusInViewModel().getValue();
         System.out.println( "viewModel get toggle value: "+thisValue);
         setToggle(thisValue);
      }
   }

   @Override
   public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
      super.onViewCreated(view, savedInstanceState);
      viewModel = new ViewModelProvider(requireActivity()).get(ItemViewModel.class);
      safelySetToggleFromViewModel();
   }

   @Override
   public void onResume() {
      super.onResume();
      String intentOfMainActivity = MainActivity.intentOfMainActivity;

      switch (intentOfMainActivity) {
         case "REGISTER" :
            // Ignoring cases where a register notification triggers a registered instance of the app
            if (!THIS_REGISTRATION_STATUS) {
               goToRegistration();
            }
            ; break ;
         case "TURN_ON_SCREEN_RECORDER" :

            if ((THIS_REGISTRATION_STATUS) && (!Boolean.TRUE.equals(viewModel.getToggleStatusInViewModel().getValue())))  {
               mToggleButton.performClick(); // We have to simulate a click, as the toggle's control behaviour interferes with the
               // overriding post-functions that come from whether the service is running or not
               //createIntentForScreenRecording(getActivity());
               //setToggle(thisValue);
            }
            ; break ;
         default : break ;
      }
      MainActivity.intentOfMainActivity = "NONE";
   }
}