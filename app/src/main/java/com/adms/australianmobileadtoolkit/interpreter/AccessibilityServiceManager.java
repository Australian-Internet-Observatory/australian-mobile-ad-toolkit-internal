package com.adms.australianmobileadtoolkit.interpreter;

import static com.adms.australianmobileadtoolkit.appSettings.logMessage;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.view.accessibility.AccessibilityManager;

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
