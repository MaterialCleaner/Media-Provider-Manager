package me.gm.cleaner.plugin.xposed.hooker

import de.robv.android.xposed.XposedHelpers

interface MediaProviderHooker {
    val Any.callingPackage: String
        get() {
            require(javaClass.name == "com.android.providers.media.MediaProvider")
            val threadLocal =
                XposedHelpers.getObjectField(this, "mCallingIdentity") as ThreadLocal<*>
            return XposedHelpers.callMethod(threadLocal.get()!!, "getPackageName") as String
        }
}
