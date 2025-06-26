package com.adms.australianmobileadtoolkit.interpreter;

import static com.adms.australianmobileadtoolkit.appSettings.logMessage;
import static com.arthenica.ffmpegkit.Packages.getPackageName;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import com.adms.australianmobileadtoolkit.ui.dialogs.DialogEnableAccessibilityService;
import com.adms.australianmobileadtoolkit.ui.dialogs.DialogFailedRegistration;

import java.util.List;

public class AccessibilityServiceManager {
    private static String TAG = "AccessibilityServiceManager";

    public static boolean isAccessibilityServiceEnabled(Context context, Class<? extends AccessibilityService> service) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        for (AccessibilityServiceInfo enabledService : enabledServices) {
            ServiceInfo enabledServiceInfo = enabledService.getResolveInfo().serviceInfo;
            if (enabledServiceInfo.packageName.equals(context.getPackageName()) && enabledServiceInfo.name.equals(service.getName())) {
                logMessage(TAG, "Accessibility Service Check Enabled: TRUE");
                return true;
            }
        }
        logMessage(TAG, "Accessibility Service Check Enabled: FALSE");
        return false;
    }
}
