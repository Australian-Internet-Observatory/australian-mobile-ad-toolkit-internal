package com.adms.australianmobileadtoolkit.ui;

import android.content.Context;

import com.google.android.play.core.assetpacks.AssetPackLocation;
import com.google.android.play.core.assetpacks.AssetPackManager;
import com.google.android.play.core.assetpacks.AssetPackManagerFactory;
import com.google.android.play.core.assetpacks.model.AssetPackStorageMethod;

import java.io.File;

public final class PadFiles {
    public static File getAssetFileFromPack(Context context, String packName, String relativeAssetPath) {
        AssetPackManager apm = AssetPackManagerFactory.getInstance(context);

        AssetPackLocation loc = apm.getPackLocation(packName);
        if (loc == null) {
            throw new IllegalStateException("Asset pack not installed yet: " + packName);
        }

        if (loc.packStorageMethod() == AssetPackStorageMethod.STORAGE_FILES) {
            // Pack is extracted to filesystem → normal File I/O works
            String assetsRoot = loc.assetsPath(); // folder that corresponds to the pack's "assets/"
            if (assetsRoot == null) {
                throw new IllegalStateException("assetsPath() was null for pack: " + packName);
            }
            return new File(assetsRoot, relativeAssetPath);
        }

        // Pack is installed as an APK → assetsPath() may not help; use AssetManager fallback
        throw new IllegalStateException("Pack is installed as APK; use AssetManager.open() for access: " + packName);
    }
}