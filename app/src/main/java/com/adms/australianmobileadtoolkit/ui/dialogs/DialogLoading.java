package com.adms.australianmobileadtoolkit.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import androidx.annotation.NonNull;

import com.adms.australianmobileadtoolkit.R;

public class DialogLoading extends Dialog implements android.view.View.OnClickListener {

   public DialogLoading(@NonNull Context context) {
      super(context);
   }

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      requestWindowFeature(Window.FEATURE_NO_TITLE);
      setContentView(R.layout.dialog_loading);
      setCancelable(false);
      setCanceledOnTouchOutside(false);
   }

   @Override
   public void onClick(View v) {
      // Nothing happens for the 'loading bar' dialog
   }
}