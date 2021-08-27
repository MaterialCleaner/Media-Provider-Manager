package me.gm.cleaner.plugin.xposed.hooker

import android.content.ContentValues
import android.net.Uri
import de.robv.android.xposed.XC_MethodHook
import me.gm.cleaner.plugin.xposed.ManagerService

class InsertHooker(private val service: ManagerService) : XC_MethodHook(), MediaProviderHooker {
    @Throws(Throwable::class)
    override fun beforeHookedMethod(param: MethodHookParam) {
        val uri = param.args[0] as Uri
        val contentValues = param.args[1] as ContentValues

//        contentValues.get("_display_name")
//        XposedBridge.log("_display_name: " + contentValues.get("_display_name"))
//        contentValues.get("relative_path")
//        XposedBridge.log("relative_path: " + contentValues.get("relative_path"))
//
//        // "mime_type" = image / png
//        XposedBridge.log("packageName: " + param.thisObject.callingPackage)
    }
}
