package com.adms.australianmobileadtoolkit.ui.fragments;

import static com.adms.australianmobileadtoolkit.MainActivity.retrieveShortActivationCode;
import static com.adms.australianmobileadtoolkit.MainActivity.safelySetToggleInViewModel;
import static com.adms.australianmobileadtoolkit.RecorderService.createIntentForScreenRecording;
import static com.adms.australianmobileadtoolkit.appSettings.logMessage;
import static com.adms.australianmobileadtoolkit.interpreter.AccessibilityServiceManager.isAccessibilityServiceEnabled;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.adms.australianmobileadtoolkit.MainActivity;
import com.adms.australianmobileadtoolkit.RecorderService;
import com.adms.australianmobileadtoolkit.interpreter.AccessibilityService;
import com.adms.australianmobileadtoolkit.ui.ItemViewModel;
import com.adms.australianmobileadtoolkit.R;
import com.adms.australianmobileadtoolkit.ui.dialogs.DialogEnableAccessibilityService;
import com.adms.australianmobileadtoolkit.ui.dialogs.DialogEnableAccessibilityServiceIntermediate;
import com.adms.australianmobileadtoolkit.ui.dialogs.DialogSubmitAds;

import java.util.Objects;

public class FragmentMain extends Fragment {

   private DialogSubmitAds submitAdsDialog;
   private DialogEnableAccessibilityService enableAccessibilityService;
   private DialogEnableAccessibilityServiceIntermediate enableAccessibilityServiceIntermediate;

   // The tag of this class
   private static final String TAG = "FragmentMain";
   // The SwitchCompat toggler used with screen-recording
   private static Switch mToggleButton;
   // TODO - comment
   // De-bouncer variable on toggler
   private static boolean mToggleButtonDebouncerActivated;

   private static ItemViewModel viewModel;

   private boolean actionedLatestIntent = false;

   private View view;

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container,
                            Bundle savedInstanceState) {
      // TODO Auto-generated method stub

      view = inflater.inflate(R.layout.fragment_main, container, false);

      // Attach the variable mToggleButton to the control within the view
      mToggleButton = (Switch) view.findViewById(R.id.simpleSwitch);

      // Initialise the de-bouncer on the mToggleButton control
      mToggleButtonDebouncerActivated = false;
      // Apply a listener to the mToggleButton control, to execute the onToggleScreenShare method
      mToggleButton.setOnClickListener(v -> {
         if (((Switch)view.findViewById(R.id.simpleSwitch)).isChecked()) {
            // ask for permission to capture screen and act on result after
            // TODO - this is the part that caused the intent error
            // TODO - move to separate function


            if ((enableAccessibilityServiceIntermediate == null) || (!enableAccessibilityServiceIntermediate.isShowing())) {
               if (!isAccessibilityServiceEnabled(requireContext(), AccessibilityService.class)) {
                  dialogEnableAccessibilityServiceIntermediate();
               } else {
                  createIntentForScreenRecording(getActivity());
               }
            }
            logMessage(TAG, "Screen-recording has started");
         } else {
            logMessage(TAG, "Screen-recording has stopped");
            getActivity().stopService(new Intent(getActivity(), RecorderService.class));
         }
         mToggleButtonDebouncerActivated = true;
      });

      ((TextView)view.findViewById(R.id.fragment_main_learn_more)).setMovementMethod(LinkMovementMethod.getInstance());
      ((TextView)view.findViewById(R.id.fragment_main_privacy_policy)).setMovementMethod(LinkMovementMethod.getInstance());
      ((TextView)view.findViewById(R.id.fragment_main_privacy_policy_unregistered)).setMovementMethod(LinkMovementMethod.getInstance());

      // If the device is registeredc
      //if (THIS_REGISTRATION_STATUS) { // NB: This can be inverted for testing purposes - default is (!THIS_REGISTRATION_STATUS)
         // Hide the 'unregistered' screen

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
         });





         Button buttonGoToProcessMyAdDigest = (Button) view.findViewById(R.id.buttonGoToProcessMyAdDigest);
         buttonGoToProcessMyAdDigest.setOnClickListener(v ->{
            submitAdsDialog = (new DialogSubmitAds(requireContext(), getParentFragmentManager()));
            submitAdsDialog.create();
            submitAdsDialog.show();
         });




         String myActivationCodeUUIDString = retrieveShortActivationCode(requireContext());
         TextView myActivationCode = ((TextView) view.findViewById(R.id.indicator_activation_code));
         myActivationCode.setText( myActivationCodeUUIDString);


      /*
      } else {
         // Or else hide the 'registered' screen
         view.findViewById(R.id.fragment_main_registered).setVisibility(View.GONE);
      }*/


      View.OnClickListener commonAccessibilityServicesProminentDisclosureRoutine = (v -> {
         Fragment fragment = new FragmentAccessibilityDisclosure();

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

      Button buttonAccessibilityDisclosureB = (Button) view.findViewById(R.id.buttonAccessibilityServicesProminentDisclosureB);
      buttonAccessibilityDisclosureB.setOnClickListener(commonAccessibilityServicesProminentDisclosureRoutine);


      Button buttonSettings = (Button) view.findViewById(R.id.buttonSettings);
      buttonSettings.setOnClickListener((v -> {
         Fragment fragment = new FragmentAppSettings();

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
      }));



      // TODO - check accessibility permissions status and then relay to button design
      Button buttonAccessibilityDisclosureBER = (Button) view.findViewById(R.id.buttonAccessibilityServicesProminentDisclosureBER);

      updateAccessibilityServicesButtonText(requireContext(), view);

      View.OnClickListener commonLaunchAccessibilityPermissionsRoutine = (v -> {
         requireContext().startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
      });
      buttonAccessibilityDisclosureBER.setOnClickListener(commonLaunchAccessibilityPermissionsRoutine);

      startAccessibilityService();

      logMessage(TAG, "Starting FragmentMain");

      return view;

   }

   public static void updateAccessibilityServicesButtonText(Context context, View view) {
      Button buttonAccessibilityDisclosureBER = view.findViewById(R.id.buttonAccessibilityServicesProminentDisclosureBER);
      if (!isAccessibilityServiceEnabled(context, AccessibilityService.class)) {
         buttonAccessibilityDisclosureBER.setText(R.string.dialog_accessibility_launch_text);
      } else {
         buttonAccessibilityDisclosureBER.setText(R.string.dialog_accessibility_revoke_text);
      }
   }

   public static void setToggle(Boolean check) {
      if (check != null) {
         try {
            if ((!mToggleButtonDebouncerActivated) && (mToggleButton != null)) {
               mToggleButton.setChecked(check);
            }
         } catch (Exception e) {
            e.printStackTrace();
         }

         // Then deactivate the de-bouncer in case (as we are resuming the app)
         mToggleButtonDebouncerActivated = false;
      }
   }

   public static void safelySetToggleFromViewModel() {
      if (viewModel != null) {
         Boolean thisValue = viewModel.getToggleStatusInViewModel().getValue();
         logMessage(TAG,  "viewModel get toggle value: "+thisValue);
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


      Intent intentOfMainActivityAsIntent = getActivity().getIntent();
      logMessage(TAG, String.valueOf(intentOfMainActivityAsIntent));
      if (intentOfMainActivityAsIntent.hasExtra("INTENT_ACTION")) {
         logMessage(TAG, "Interpreted value of intent: "+ (Objects.requireNonNull(intentOfMainActivityAsIntent.getStringExtra("INTENT_ACTION"))) );
      }

      String intentOfMainActivity = MainActivity.intentOfMainActivity;

      if (!actionedLatestIntent) {
         switch (intentOfMainActivity) {
            case "REGISTER" :
               actionedLatestIntent = true;
               break ;
            case "TURN_ON_SCREEN_RECORDER" :

               if (!Boolean.TRUE.equals(viewModel.getToggleStatusInViewModel().getValue()))  {
                  mToggleButton.performClick(); // We have to simulate a click, as the toggle's control behaviour interferes with the
                  // overriding post-functions that come from whether the service is running or not
                  //createIntentForScreenRecording(getActivity());
                  //setToggle(thisValue);
               }
               actionedLatestIntent = true;
               break ;
            default : break ;
         }
         MainActivity.intentOfMainActivity = "NONE";
      }

      updateAccessibilityServicesButtonText(requireContext(), view);
      logMessage(TAG, "Resuming FragmentMain");

   }



   public void startAccessibilityService() {
      if ((enableAccessibilityService == null) || (!enableAccessibilityService.isShowing())) {
         if (!isAccessibilityServiceEnabled(requireContext(), AccessibilityService.class)) {
            dialogEnableAccessibilityService();
         }
      }
   }

   public void dialogEnableAccessibilityService() {
      enableAccessibilityService = new DialogEnableAccessibilityService(requireContext(), getParentFragmentManager());
      enableAccessibilityService.show();
      /*
      enableAccessibilityService.setOnDismissListener(new DialogInterface.OnDismissListener() {
         @Override
         public void onDismiss(DialogInterface dialog) {
            if (!isAccessibilityServiceEnabled(requireContext(), AccessibilityService.class)) {
               enableAccessibilityService.dismiss();
               requireContext().startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
         }
      });*/
   }

   public void dialogEnableAccessibilityServiceIntermediate() {
      safelySetToggleInViewModel(false);
      enableAccessibilityServiceIntermediate = new DialogEnableAccessibilityServiceIntermediate(requireContext(), getParentFragmentManager());
      enableAccessibilityServiceIntermediate.show();
      /*
      enableAccessibilityService.setOnDismissListener(new DialogInterface.OnDismissListener() {
         @Override
         public void onDismiss(DialogInterface dialog) {
            if (!isAccessibilityServiceEnabled(requireContext(), AccessibilityService.class)) {
               enableAccessibilityService.dismiss();
               requireContext().startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
         }
      });*/
   }

   @Override
   public void onDestroyView() {
      super.onDestroyView();
      logMessage(TAG, "VIEW WAS DESTROYED");
      if (submitAdsDialog != null) {
         submitAdsDialog.killThread();//forceThreadDeath();
         submitAdsDialog.dismiss();
      }
      if (enableAccessibilityService != null) {
         enableAccessibilityService.dismiss();
      }

      if (enableAccessibilityServiceIntermediate != null) {
         enableAccessibilityServiceIntermediate.dismiss();
      }

   }

}