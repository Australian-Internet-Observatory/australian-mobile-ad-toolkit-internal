package com.adms.australianmobileadtoolkit.interpreter;

import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class AccessibilityService extends android.accessibilityservice.AccessibilityService{
    public static AccessibilityService instance;
    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        // Note : This event is sometimes called more than one for a foreground service
        if (accessibilityEvent.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED){
            Log.d("Event","TYPE_WINDOW_STATE_CHANGED");
            Log.d("Pkg",accessibilityEvent.getPackageName().toString());
        }
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d("Accessibility","Service Connected");
    }
}


// https://stackoverflow.com/questions/18094982/detect-if-my-accessibility-service-is-enabled