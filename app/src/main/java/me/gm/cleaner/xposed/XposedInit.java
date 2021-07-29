package me.gm.cleaner.xposed;

import android.content.Context;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import me.gm.cleaner.xposed.util.DevelopUtils;

public class XposedInit extends XposedContext implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.android.providers.media.module")) return;
        sClassLoader = lpparam.classLoader;

        DevelopUtils.logMethods("com.android.providers.media.MediaDocumentsProvider");
        DevelopUtils.logMethods(findInnerClass("AlbumQuery"));
        DevelopUtils.logMethods(findInnerClass("ArtistQuery"));
        DevelopUtils.logMethods(findInnerClass("DocumentQuery"));
        DevelopUtils.logMethods(findInnerClass("DocumentsBucketQuery"));
        DevelopUtils.logMethods(findInnerClass("Ident"));
        DevelopUtils.logMethods(findInnerClass("ImageQuery"));
        DevelopUtils.logMethods(findInnerClass("ImagesBucketQuery"));
        DevelopUtils.logMethods(findInnerClass("ImagesBucketThumbnailQuery"));
        DevelopUtils.logMethods(findInnerClass("SongQuery"));
        DevelopUtils.logMethods(findInnerClass("VideosQuery"));
        DevelopUtils.logMethods(findInnerClass("VideosBucketQuery"));
        DevelopUtils.logMethods(findInnerClass("VideosBucketThumbnailQuery"));
        Class<?> providerClass = XposedHelpers.findClass("com.android.providers.media.MediaDocumentsProvider", sClassLoader);
        XposedHelpers.findAndHookMethod(providerClass, "onMediaStoreInsert",
                Context.class, String.class, int.class, long.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log((String) param.args[1]);
                    }
                });
        XposedHelpers.findAndHookMethod(providerClass, "onMediaStoreDelete",
                Context.class, String.class, int.class, long.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log((String) param.args[1]);
                    }
                });
    }

    Class<?> findInnerClass(String innerClassName) {
        return XposedHelpers.findClass("com.android.providers.media.MediaDocumentsProvider$" + innerClassName, sClassLoader);
    }
}
