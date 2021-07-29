package me.gm.cleaner.xposed;

import android.util.Log;

import java.io.File;

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

        DevelopUtils.logMethods("com.android.providers.media.MediaProvider");
        XposedHelpers.findAndHookMethod(File.class, "mkdirs", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String path = (String) XposedHelpers.getObjectField(param.thisObject, "path");
                XposedBridge.log(path);
                XposedBridge.log(Log.getStackTraceString(new Exception()));
            }
        });
    }
}
