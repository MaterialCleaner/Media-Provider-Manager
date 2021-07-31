package me.gm.cleaner.plugin.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;

import me.gm.cleaner.R;
import me.zhanghai.android.appiconloader.glide.AppIconModelLoader;

@GlideModule
public class AppGlideModule extends com.bumptech.glide.module.AppGlideModule {
    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide,
                                   @NonNull Registry registry) {
        int iconSize = context.getResources().getDimensionPixelSize(R.dimen.large_icon_size);
        registry.prepend(PackageInfo.class, Bitmap.class, new AppIconModelLoader.Factory(iconSize,
                false, context));
    }
}