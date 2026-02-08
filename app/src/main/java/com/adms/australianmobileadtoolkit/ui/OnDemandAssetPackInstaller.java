package com.adms.australianmobileadtoolkit.ui;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.play.core.assetpacks.AssetPackLocation;
import com.google.android.play.core.assetpacks.AssetPackManager;
import com.google.android.play.core.assetpacks.AssetPackManagerFactory;
import com.google.android.play.core.assetpacks.AssetPackState;
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener;
import com.google.android.play.core.assetpacks.model.AssetPackStatus;
import com.google.android.play.core.assetpacks.model.AssetPackErrorCode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class OnDemandAssetPackInstaller {
    public interface Callback {
        void onProgress(int percent);
        void onReady(@NonNull String assetsPath);
        void onError(@NonNull String message, int errorCode);
    }

    private static final String TAG = "OnDemandAssetPackInstaller";
    private final AssetPackManager assetPackManager;
    private final String packName;

    private AssetPackStateUpdateListener listener;

    public OnDemandAssetPackInstaller(@NonNull Context context, @NonNull String packName) {
        this.assetPackManager = AssetPackManagerFactory.getInstance(context.getApplicationContext());
        this.packName = packName;
    }

    public void fetchIfNeeded(@NonNull Activity activity, @NonNull Callback cb) {
        // If already installed, return immediately.
        AssetPackLocation loc = assetPackManager.getPackLocation(packName);
        if (loc != null) {
            cb.onReady(loc.assetsPath());
            return;
        }

        listener = state -> {
            if (!packName.equals(state.name())) return;

            switch (state.status()) {
                case AssetPackStatus.PENDING:
                case AssetPackStatus.DOWNLOADING: {
                    long total = state.totalBytesToDownload();
                    long done = state.bytesDownloaded();
                    int pct = (total > 0) ? (int)((done * 100L) / total) : 0;
                    cb.onProgress(pct);
                    break;
                }
                case AssetPackStatus.WAITING_FOR_WIFI: {
                    // Optional: prompt user to allow cellular download
                    assetPackManager.showCellularDataConfirmation(activity)
                            .addOnSuccessListener(result -> Log.d(TAG, "Cellular confirmation shown: " + result))
                            .addOnFailureListener(e -> Log.w(TAG, "Cellular confirmation failed", e));
                    break;
                }
                case AssetPackStatus.TRANSFERRING:
                    cb.onProgress(100);
                    break;

                case AssetPackStatus.COMPLETED: {
                    unregister();
                    AssetPackLocation loc2 = assetPackManager.getPackLocation(packName);
                    if (loc2 == null) {
                        cb.onError("Pack completed but location was null", -1);
                        return;
                    }
                    cb.onReady(loc2.assetsPath());
                    break;
                }
                case AssetPackStatus.FAILED: {
                    unregister();
                    int code = state.errorCode();
                    cb.onError("Pack failed: " + describeError(code), code);
                    break;
                }
                case AssetPackStatus.CANCELED: {
                    unregister();
                    int code = state.errorCode();
                    cb.onError("Pack download canceled", code);
                    break;
                }
                default:
                    // Ignore other states
                    break;
            }
        };

        assetPackManager.registerListener(listener);

        assetPackManager.fetch(Collections.singletonList(packName))
                .addOnSuccessListener(unused -> Log.d(TAG, "Fetch started for " + packName))
                .addOnFailureListener(e -> {
                    unregister();
                    cb.onError("fetch() threw: " + e.getMessage(), -1);
                });
    }

    public void unregister() {
        if (listener != null) {
            assetPackManager.unregisterListener(listener);
            listener = null;
        }
    }

    private static String describeError(int code) {
        switch (code) {
            case AssetPackErrorCode.NETWORK_ERROR: return "NETWORK_ERROR";
            case AssetPackErrorCode.ACCESS_DENIED: return "ACCESS_DENIED";
            case AssetPackErrorCode.INSUFFICIENT_STORAGE: return "INSUFFICIENT_STORAGE";
            case AssetPackErrorCode.APP_UNAVAILABLE: return "APP_UNAVAILABLE";
            case AssetPackErrorCode.PACK_UNAVAILABLE: return "PACK_UNAVAILABLE";
            default: return "ERROR_CODE_" + code;
        }
    }

    @Nullable
    public static String getInstalledAssetsPath(Context context, String packName) {
        AssetPackManager apm =
                AssetPackManagerFactory.getInstance(context.getApplicationContext());

        AssetPackLocation loc = apm.getPackLocation(packName);

        if (loc == null) {
            // Pack is not installed
            return null;
        }

        // This is the root of the pack's assets/
        return loc.assetsPath();
    }
}
