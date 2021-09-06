package me.gm.cleaner.plugin.xposed.hooker

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

interface MediaProviderHooker {
    val XC_MethodHook.MethodHookParam.callingPackage: String
        get() {
            require(method.declaringClass.name == "com.android.providers.media.MediaProvider")
            val threadLocal =
                XposedHelpers.getObjectField(thisObject, "mCallingIdentity") as ThreadLocal<*>
            return XposedHelpers.callMethod(threadLocal.get(), "getPackageName") as String
        }
}
