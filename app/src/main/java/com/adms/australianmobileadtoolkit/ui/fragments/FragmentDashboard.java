package com.adms.australianmobileadtoolkit.ui.fragments;

import static android.view.View.TEXT_ALIGNMENT_CENTER;
import static android.widget.LinearLayout.VERTICAL;
import static com.adms.australianmobileadtoolkit.MainActivity.THIS_OBSERVER_ID;
import static com.adms.australianmobileadtoolkit.appSettings.get_ACTIVATION_CODE_NOT_APPLICABLE_STRING;
import static com.adms.australianmobileadtoolkit.appSettings.get_ACTIVATION_CODE_PREFIX_STRING;
import static com.adms.australianmobileadtoolkit.appSettings.sharedPreferenceGet;

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

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.adms.australianmobileadtoolkit.R;
import com.adms.australianmobileadtoolkit.appSettings;
import com.adms.australianmobileadtoolkit.ui.SortByObservedAt;

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

      public DownloadImageTask(ImageView bmImage) {
         this.bmImage = bmImage;
      }

      protected Bitmap doInBackground(String... urls) {
         String urldisplay = urls[0];
         Bitmap mIcon11 = null;
         try {
            InputStream in = new java.net.URL(urldisplay).openStream();
            mIcon11 = BitmapFactory.decodeStream(in);
         } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
         }
         return mIcon11;
      }

      protected void onPostExecute(Bitmap result) {
         bmImage.setImageBitmap(result);
      }
   }

   private View thisView;

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container,
                            Bundle savedInstanceState) {

      View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
      thisView = view;

      String activationCodeNotApplicableString = get_ACTIVATION_CODE_NOT_APPLICABLE_STRING(getContext());
      String myActivationCodeUUIDString = sharedPreferenceGet(getActivity(),
            "SHARED_PREFERENCE_OBSERVER_ID", activationCodeNotApplicableString);
      TextView myActivationCode = ((TextView) view.findViewById(R.id.myActivationCode));
      myActivationCode.setText(get_ACTIVATION_CODE_PREFIX_STRING(getContext()) + myActivationCodeUUIDString);

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

      Thread thread = new Thread(() -> {
         try {
            JSONObject response = httpRequestDashboard();
            if (response != null) {
               getActivity().runOnUiThread(() -> {
                  View loadingBar = view.findViewById(R.id.dashboardLoading);
                  ((ViewGroup) loadingBar.getParent()).removeView(loadingBar);
               });

               JSONObject ads = (JSONObject) response.get("ads");
               Iterator<String> keys = ads.keys();
               List<JSONObject> adsSorted = new ArrayList<>();
               while(keys.hasNext()) {
                  adsSorted.add((JSONObject) ads.get(keys.next()));
               }
               Collections.sort(adsSorted, new SortByObservedAt());
               for (JSONObject thisAd : adsSorted) {
                  String bannerURL = thisAd.getString("banner_url");
                  String headerURL = thisAd.getString("header_url");
                  long observedAt = thisAd.getLong("observed_at");
                  getActivity().runOnUiThread(() -> adDivider(headerURL, bannerURL, observedAt));
               }
            }
         } catch (Exception e) {
            Log.e(TAG, e.getMessage());
         }
      });
      thread.start();
      return view;


   }

   static int id = 1;

   @SuppressLint("SetTextI18n")
   private void adDivider(String headerURL, String bannerURL, long observedAt) {
      LinearLayout fdo = thisView.findViewById(R.id.fragment_dashboard_overview);
      LinearLayout.MarginLayoutParams x = new LinearLayout.MarginLayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
      x.setMargins(0,0,0,(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics()));
      LinearLayout f = new LinearLayout(getActivity());
      f.setLayoutParams(new LinearLayout.LayoutParams(x));
      f.setOrientation(VERTICAL);
      f.setBackgroundColor(getResources().getColor(R.color.yellow_primary_transparent));
      f.setBackground(getResources().getDrawable(R.drawable.border_radius));

      FrameLayout fHeader = new FrameLayout(getActivity());
      ImageView headerImage = new ImageView(getActivity());
      new DownloadImageTask(headerImage).execute(headerURL);
      fHeader.addView(headerImage);
      headerImage.setAdjustViewBounds(true);
      headerImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
      headerImage.invalidate();
      fHeader.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
      f.addView(fHeader);
      fHeader.invalidate();
      headerImage.setMinimumHeight((int) Math.round(headerImage.getHeight()*2));



      FrameLayout fBanner = new FrameLayout(getActivity());
      ImageView bannerImage = new ImageView(getActivity());
      new DownloadImageTask(bannerImage).execute(bannerURL);
      fBanner.addView(bannerImage);
      bannerImage.setAdjustViewBounds(true);
      bannerImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
      bannerImage.invalidate();
      f.addView(fBanner);
      fBanner.invalidate();
      bannerImage.setMinimumHeight((int) Math.round(bannerImage.getHeight()*2));

      Calendar cal = Calendar.getInstance(Locale.ENGLISH);
      cal.setTimeInMillis(observedAt);
      String date = DateFormat.format("hh:mm:ss a dd-MM-yyyy", cal.getTime()).toString();

      TextView thisDateText = new TextView(getActivity());
      thisDateText.setTextColor(getResources().getColor(R.color.yellow_primary));
      thisDateText.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics()));
      thisDateText.setText(getString(R.string.dashboard_observed_prefix)+date);
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

      fdo.addView(f);
      fdo.invalidate();
   }


   private JSONObject httpRequestDashboard() {
      try {
         // Declare the AWS Lambda endpoint
         String urlParam = appSettings.AWS_LAMBDA_ENDPOINT;
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
         requestBody.put("action", identifierDataDonation);
         requestBody.put("observer_id", observerID);
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

         JSONObject obj = new JSONObject(rd.lines().collect(Collectors.joining()));
         return obj;
         //new DownloadImageTask((ImageView) findViewById(R.id.imageView1))
         //      .execute(MY_URL_STRING);

      } catch (Exception e) {
         Log.e(TAG, "Failed to run httpRequestDataDonation: ", e);
         return null;
      }
   }
}