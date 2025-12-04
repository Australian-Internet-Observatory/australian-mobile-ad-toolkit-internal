package com.adms.australianmobileadtoolkit.ui.fragments;

import static android.view.View.TEXT_ALIGNMENT_CENTER;
import static android.widget.LinearLayout.VERTICAL;
import static androidx.core.content.ContentProviderCompat.requireContext;
import static com.adms.australianmobileadtoolkit.MainActivity.THIS_OBSERVER_ID;
import static com.adms.australianmobileadtoolkit.appSettings.logMessage;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.icu.util.Calendar;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.adms.australianmobileadtoolkit.JSONXObject;
import com.adms.australianmobileadtoolkit.R;
import com.adms.australianmobileadtoolkit.appSettings;
import com.adms.australianmobileadtoolkit.ui.SortByObservedAt;
import com.adms.australianmobileadtoolkit.ui.dialogs.DialogLoading;
import com.google.gson.JsonArray;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class FragmentDashboard extends Fragment {


   private static final String TAG = "FragmentDashboard";

   private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
      ImageView bmImage;
      View loadingPanel;

      public DownloadImageTask(ImageView bmImage, View loadingPanel) {
         this.bmImage = bmImage;
         this.loadingPanel = loadingPanel;
      }

      protected Bitmap doInBackground(String... urls) {
         String urldisplay = urls[0];
         Bitmap mIcon11 = null;
         try {
            InputStream in = new java.net.URL(urldisplay).openStream();
            mIcon11 = BitmapFactory.decodeStream(in);
         } catch (Exception e) {
            logMessage("Error", e.getMessage());
            e.printStackTrace();
         }
         return mIcon11;
      }

      protected void onPostExecute(Bitmap result) {
         ((ViewGroup) loadingPanel.getParent()).removeView(loadingPanel);
         bmImage.setImageBitmap(result);
         bmImage.setMinimumHeight(Math.round(result.getHeight()));
         bmImage.setAdjustViewBounds(true);
         bmImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
      }
   }

   private View thisView;
   private Integer thisOffset = 0;
   private boolean loadingDashboard = false;
   private ViewGroup mContainer;
   private DialogLoading loadDialog;
   private boolean canForward = false;
   private boolean canBackward = false;

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container,
                            Bundle savedInstanceState) {

      mContainer = container;
      View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
      thisView = view;
      /*
      String myActivationCodeUUIDString = retrieveShortActivationCode(requireContext());
      TextView myActivationCode = ((TextView) view.findViewById(R.id.myActivationCode));
      myActivationCode.setText(get_ACTIVATION_SHORT_CODE_PREFIX_STRING(requireContext()) + myActivationCodeUUIDString);*/

      Button mbuttonBackToMain = (Button) view.findViewById(R.id.buttonBackToMain);
      mbuttonBackToMain.setOnClickListener(v ->{
         Fragment fragment = new FragmentMain();
         //FragmentMain.setToggle(isServiceRunning());

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

      loadDashboard(view);
      (view.findViewById(R.id.prevPaginationButton)).setOnClickListener(v ->{ loadDashboardOnOffset( -1); });
      (view.findViewById(R.id.nextPaginationButton)).setOnClickListener(v ->{ loadDashboardOnOffset( +1); });
      return view;


   }

   public void loadDashboardOnOffset(Integer offset) {
      if (!loadingDashboard) {
         boolean antiConditionA = ((thisOffset == 0) && (offset == -1));
         boolean antiConditionB = ((!canBackward) && (offset == -1));
         boolean antiConditionC = ((!canForward) && (offset == 1));
         if ((!antiConditionA) && (!antiConditionB) && (!antiConditionC)) {
            resetDashboardView();
            thisOffset += offset;
            loadDashboard(thisView);
         }
      }
   }

   public void loadDashboard(View view) {
      Thread thread = new Thread(() -> {
         loadingDashboard = true;
         try {
            JSONXObject response = httpRequestDashboard();
            if (response != null) {
               getActivity().runOnUiThread(() -> {
                  View loadingBar = view.findViewById(R.id.dashboardLoading);
                  ((ViewGroup) loadingBar.getParent()).removeView(loadingBar);
               });
               JSONArray adObjectsAsJSONArray = ((JSONArray) response.get("ad_objs"));
               for (var i = 0; i < adObjectsAsJSONArray.length(); i ++) {
                  final Integer finalI = i;
                  try {
                     JSONObject x = adObjectsAsJSONArray.getJSONObject(finalI);
                     getActivity().runOnUiThread(() -> adDivider(new JSONXObject(x)));
                  } catch (Exception e) {
                     e.printStackTrace();
                  }
               }
               JSONXObject paginationAllowances = (new JSONXObject((JSONObject) response.get("paginate")));
               canForward = ((boolean) paginationAllowances.get("forward"));
               canBackward = ((boolean) paginationAllowances.get("backward"));
               getActivity().runOnUiThread(() -> {
                  (view.findViewById(R.id.paginationButtons)).setVisibility((canForward || canBackward) ? View.VISIBLE : View.GONE);
                  /*
                  (view.findViewById(R.id.prevPaginationButton)).setVisibility(canBackward ? View.VISIBLE : View.GONE);
                  (view.findViewById(R.id.nextPaginationButton)).setVisibility(canForward ? View.VISIBLE : View.GONE);
                  */
                  (view.findViewById(R.id.prevPaginationButton)).setAlpha(canBackward ? 1.0f : 0.5f);
                  (view.findViewById(R.id.nextPaginationButton)).setAlpha(canForward ? 1.0f : 0.5f);
               });
               if (adObjectsAsJSONArray.length() == 0) {
                  getActivity().runOnUiThread(() -> {
                     LinearLayout fdo = thisView.findViewById(R.id.fragment_dashboard_overview);
                     View viewEmptyDashboard = getLayoutInflater().inflate(R.layout.fragment_dashboard_empty, mContainer, false);
                     fdo.addView(viewEmptyDashboard);
                     fdo.invalidate();
                     view.findViewById(R.id.noteAboutShowing).setVisibility(View.GONE);
                  });
               } else {
                  getActivity().runOnUiThread(() -> {
                     view.findViewById(R.id.noteAboutShowing).setVisibility(View.VISIBLE);
                  });
               }
            }
         } catch (Exception e) {
            logMessage(TAG, e.getMessage());
         }
         loadingDashboard = false;
      });
      thread.start();
   }

   private void resetDashboardView() {
      LinearLayout fdo = thisView.findViewById(R.id.fragment_dashboard_overview);
      fdo.removeAllViews();
      View viewLoading = getLayoutInflater().inflate(R.layout.fragment_dashboard_loading, mContainer, false);
      fdo.addView(viewLoading);
      fdo.invalidate();
   }

   static int id = 1;

   @SuppressLint("SetTextI18n")
   private void adDivider(JSONXObject thisAd) {
      LinearLayout fdo = thisView.findViewById(R.id.fragment_dashboard_overview);
      View viewAdCard = getLayoutInflater().inflate(R.layout.fragment_dashboard_card, mContainer, false);

      Calendar cal = Calendar.getInstance(Locale.ENGLISH);
      cal.setTimeInMillis(((Integer) thisAd.get("observed_at")) * 1000L);
      String date = DateFormat.format("hh:mm:ss a dd-MM-yyyy", cal.getTime()).toString();
      ((TextView) viewAdCard.findViewById(R.id.observed_at_text)).setText(date);

      /*
      int adPlatformDrawable = R.drawable.platform_facebook_icon;;
      switch ((String) thisAd.get("platform")) {
         case "FACEBOOK" :
            adPlatformDrawable = R.drawable.platform_facebook_icon;
            break ;
         case "INSTAGRAM" :
            adPlatformDrawable = R.drawable.platform_instagram_icon;
            break ;
         case "TIKTOK" :
            adPlatformDrawable = R.drawable.platform_tiktok_icon;
            break ;
         case "YOUTUBE" :
            adPlatformDrawable = R.drawable.platform_youtube_icon;
            break ;
      }
      ((ImageView) viewAdCard.findViewById(R.id.platform_icon)).setImageDrawable(ContextCompat.getDrawable(requireActivity(), adPlatformDrawable));
       */

      new DownloadImageTask(viewAdCard.findViewById(R.id.ad_image), viewAdCard.findViewById(R.id.loadingPanel)).execute((String) thisAd.get("banner_img"));

      viewAdCard.findViewById(R.id.showhide_option).setOnClickListener(v ->{

         loadDialog = new DialogLoading(requireContext());
         loadDialog.setOnDismissListener((l)->{
            l = null;
         });
         loadDialog.create();
         loadDialog.show();

         Thread thread = new Thread(() -> {
            httpRequestDisableAd((String) thisAd.get("rdo_uuid_unsplit"));
         });
         thread.start();
      });

      /*
      LinearLayout.MarginLayoutParams x = new LinearLayout.MarginLayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
      x.setMargins(0,0,0,(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics()));
      LinearLayout f = new LinearLayout(getActivity());
      f.setLayoutParams(new LinearLayout.LayoutParams(x));
      f.setOrientation(VERTICAL);
      f.setBackgroundColor(getResources().getColor(R.color.yellow_primary_transparent));
      f.setBackground(getResources().getDrawable(R.drawable.border_radius));

      FrameLayout fBanner = new FrameLayout(getActivity());
      ImageView bannerImage = new ImageView(getActivity());
      new DownloadImageTask(bannerImage).execute((String) thisAd.get("banner_img"));
      fBanner.addView(bannerImage);
      bannerImage.setAdjustViewBounds(true);
      bannerImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
      bannerImage.invalidate();
      f.addView(fBanner);
      fBanner.invalidate();
      bannerImage.setMinimumHeight((int) Math.round(bannerImage.getHeight()*2));


      TextView thisDateText = new TextView(getActivity());
      thisDateText.setTextColor(getResources().getColor(R.color.yellow_primary));
      thisDateText.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics()));
      thisDateText.setText(getString(R.string.dashboard_observed_prefix)+" "+date);
      thisDateText.setTypeface(null, Typeface.BOLD_ITALIC);
      thisDateText.setTextAlignment(TEXT_ALIGNMENT_CENTER);
      thisDateText.setPadding(
            (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15, getResources().getDisplayMetrics()),
            (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()),
            (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15, getResources().getDisplayMetrics()),
            (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()));
      thisDateText.setLayoutParams(new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
      FrameLayout fText = new FrameLayout(getActivity());
      fText.addView(thisDateText);
      bannerImage.invalidate();
      f.addView(fText);
      fText.invalidate();
      //new DownloadImageTask((ImageView) thisView.findViewById(id)).execute(MY_URL_STRING);
      */
      fdo.addView(viewAdCard);
      fdo.invalidate();
   }

   private void httpRequestDisableAd(String thisAd) {
      try {
         // Declare the AWS Lambda endpoint
         String urlParam = "https://bxxqvaozhe237ak5ndca2zftz40kvgfm.lambda-url.ap-southeast-2.on.aws/";
         // The unique ID of the observer to insert with the HTTP request
         String observerID = THIS_OBSERVER_ID;
         // The identifier for submitting data donations
         String identifierDataDonation = appSettings.IDENTIFIER_AD_LEADS;
         // The HTTP request connection timeout (in milliseconds)
         int requestConnectTimeout = appSettings.AWS_LAMBDA_ENDPOINT_CONNECTION_TIMEOUT;
         // The HTTP request read timeout (in milliseconds)
         int requestReadTimeout = appSettings.AWS_LAMBDA_ENDPOINT_READ_TIMEOUT;
         // Write up the stream for inserting the image (as a Base64 string) into the request
         // Assemble the request JSON object
         JSONObject requestBody = new JSONObject();
         requestBody.put("action", "DISABLE_AD");
         requestBody.put("observer_uuid", observerID);
         logMessage(TAG, observerID);
         requestBody.put("rdo_uuid_unsplit", thisAd);
         logMessage(TAG, thisAd);
         String bodyParam = requestBody.toString();
         // Set up the HTTP request configuration
         URL url = new URL(urlParam);
         HttpURLConnection connection = (HttpURLConnection) url.openConnection();
         connection.setDoOutput(true);
         connection.setRequestMethod("POST");
         connection.setRequestProperty("Accept", "text/plain");
         connection.setRequestProperty("Content-Type", "text/plain");
         connection.setConnectTimeout(requestConnectTimeout);
         connection.setReadTimeout(requestReadTimeout);
         OutputStream os = connection.getOutputStream();
         OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
         osw.write(bodyParam);
         osw.flush();
         osw.close();
         connection.connect();
         BufferedReader rd = new BufferedReader(new InputStreamReader(
                 connection.getInputStream()));

         JSONXObject obj = new JSONXObject(new JSONObject(rd.lines().collect(Collectors.joining())));

         getActivity().runOnUiThread(() -> {
            resetDashboardView();
            loadDashboard(thisView);
            loadDialog.dismiss();
         });
      } catch (Exception e) {
         logMessage(TAG, "Failed to run httpRequestDisableAd: ");
         e.printStackTrace();
      }
   }


   private JSONXObject httpRequestDashboard() {
      try {
         // Declare the AWS Lambda endpoint
         String urlParam = "https://bxxqvaozhe237ak5ndca2zftz40kvgfm.lambda-url.ap-southeast-2.on.aws/";
         // The unique ID of the observer to insert with the HTTP request
         String observerID = THIS_OBSERVER_ID;
         // The identifier for submitting data donations
         String identifierDataDonation = appSettings.IDENTIFIER_AD_LEADS;
         // The HTTP request connection timeout (in milliseconds)
         int requestConnectTimeout = appSettings.AWS_LAMBDA_ENDPOINT_CONNECTION_TIMEOUT;
         // The HTTP request read timeout (in milliseconds)
         int requestReadTimeout = appSettings.AWS_LAMBDA_ENDPOINT_READ_TIMEOUT;
         // Write up the stream for inserting the image (as a Base64 string) into the request
         // Assemble the request JSON object
         JSONObject requestBody = new JSONObject();
         requestBody.put("action", "GET_ADS");
         requestBody.put("observer_uuid", observerID);
         requestBody.put("offset", thisOffset.toString());
         String bodyParam = requestBody.toString();
         // Set up the HTTP request configuration
         URL url = new URL(urlParam);
         HttpURLConnection connection = (HttpURLConnection) url.openConnection();
         connection.setDoOutput(true);
         connection.setRequestMethod("POST");
         connection.setRequestProperty("Accept", "text/plain");
         connection.setRequestProperty("Content-Type", "text/plain");
         connection.setConnectTimeout(requestConnectTimeout);
         connection.setReadTimeout(requestReadTimeout);
         OutputStream os = connection.getOutputStream();
         OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
         osw.write(bodyParam);
         osw.flush();
         osw.close();
         connection.connect();
         // Interpret the output
         BufferedReader rd = new BufferedReader(new InputStreamReader(
               connection.getInputStream()));

         JSONXObject obj = new JSONXObject(new JSONObject(rd.lines().collect(Collectors.joining())));
         return obj;

      } catch (Exception e) {
         logMessage(TAG, "Failed to run httpRequestDataDonation: ");
         e.printStackTrace();
         return null;
      }
   }
}