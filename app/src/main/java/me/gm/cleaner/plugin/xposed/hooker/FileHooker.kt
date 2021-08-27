package me.gm.cleaner.plugin.xposed.hooker

import android.os.Environment
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import me.gm.cleaner.plugin.BuildConfig
import me.gm.cleaner.plugin.util.FileUtils
import me.gm.cleaner.plugin.xposed.ManagerService

class FileHooker(private val service: ManagerService) : XC_MethodHook() {
    private val niceParents = FileUtils.standardDirs.apply { add(FileUtils.androidDir) }
    private val redirectDir = service.context.getExternalFilesDir(null)!!.path
    private val externalStorageDirectory = Environment.getExternalStorageDirectory().path

    @Throws(Throwable::class)
    override fun beforeHookedMethod(param: MethodHookParam) {
        val path = XposedHelpers.getObjectField(
            param.thisObject, "path"
        ) as String
        // record
        // TODO
        // redirect
        if (niceParents.none { FileUtils.startsWith(it, path) }) {
            val redirect = redirectDir + path.substring(
                externalStorageDirectory.length
            )
            XposedHelpers.setObjectField(
                param.thisObject, "path", redirect
            )
            if (BuildConfig.DEBUG) {
                XposedBridge.log("redirected a dir: $redirect")
            }
        }
    }
}
