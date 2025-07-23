package com.adms.australianmobileadtoolkit.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.adms.australianmobileadtoolkit.R;

public class DialogOutcome extends Dialog implements android.view.View.OnClickListener {

    private int icon;
    private int title;
    private int description;
    private int dismiss;
    private boolean cancelable;

    public DialogOutcome(@NonNull Context context, @DrawableRes int icon, @StringRes int title, @StringRes int description, @StringRes int dismiss, boolean cancelable) {
        super(context);
        this.icon = icon;
        this.title = title;
        this.description = description;
        this.dismiss = dismiss;
        this.cancelable = cancelable;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_outcome);
        this.setTitle(this.title);
        this.setDescription(this.description);
        this.setDismiss(this.dismiss);
        this.setIcon(this.icon);

        Button dismissButton = this.findViewById(R.id.dialog_dismiss);
        dismissButton.setOnClickListener(v -> {
            DialogOutcome.this.dismiss();
        });
        setCancelable(this.cancelable);
        setCanceledOnTouchOutside(this.cancelable);
    }

    public void setTitle(@StringRes int title) {
        this.title = title;
        ((TextView) this.findViewById(R.id.dialog_title)).setText(title);
    }

    public void setDescription(@StringRes int description) {
        this.description = description;
        ((TextView) this.findViewById(R.id.dialog_description)).setText(description);
    }

    public void setDismiss(@StringRes int dismiss) {
        this.dismiss = dismiss;
        Button dismissButton = this.findViewById(R.id.dialog_dismiss);
        dismissButton.setText(this.dismiss);
    }

    public void setIcon(@DrawableRes int icon) {
        this.icon = icon;
        ((ImageView) this.findViewById(R.id.dialog_icon)).setImageDrawable(getContext().getDrawable(this.icon));
    }

    @Override
    public void onClick(View v) {
        // Nothing happens
    }
}
