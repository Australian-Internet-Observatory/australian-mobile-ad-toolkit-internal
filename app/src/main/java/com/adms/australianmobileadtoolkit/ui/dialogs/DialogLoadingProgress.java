package com.adms.australianmobileadtoolkit.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.adms.australianmobileadtoolkit.R;

import java.util.Locale;

public class DialogLoadingProgress extends Dialog implements View.OnClickListener {

   private ProgressBar indicativeProgressBar;
   private TextView indicativeProgressBarText;



   public DialogLoadingProgress(@NonNull Context context) {
      super(context);
   }

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      requestWindowFeature(Window.FEATURE_NO_TITLE);
      setContentView(R.layout.dialog_loading_progress);
      setCancelable(false);
      setCanceledOnTouchOutside(false);


      indicativeProgressBar = ((ProgressBar) this.findViewById(R.id.progress_bar_indicator));
      indicativeProgressBarText = ((TextView) this.findViewById(R.id.progress_bar_indicator_text));
   }

   public void setProgressLoading(int newValue) {

      if (indicativeProgressBar != null) {
         indicativeProgressBar.startAnimation((new ProgressBarAnimation(indicativeProgressBar, indicativeProgressBar.getProgress(), newValue)));
      }
      if (indicativeProgressBarText != null) {
         indicativeProgressBarText.setText(String.format(Locale.getDefault(), "%d%%", newValue));
      }
   }

   @Override
   public void onClick(View v) {
      // Nothing happens for the 'loading bar' dialog
   }
}