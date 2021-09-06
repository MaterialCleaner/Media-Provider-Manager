package me.gm.cleaner.plugin.xposed.hooker

import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import de.robv.android.xposed.XC_MethodHook
import me.gm.cleaner.plugin.BuildConfig
import me.gm.cleaner.plugin.xposed.ManagerService

class QueryHooker(private val service: ManagerService) : XC_MethodHook(), MediaProviderHooker {
    @Throws(Throwable::class)
    override fun beforeHookedMethod(param: MethodHookParam) {
        val uri = param.args[0] as? Uri
        val projection = param.args[1] as? Array<String>
        val queryArgs = param.args[2] as? Bundle
        val signal = param.args[3] as? CancellationSignal
    }

    // for interaction
    @Throws(Throwable::class)
    override fun afterHookedMethod(param: MethodHookParam) {
        if (param.callingPackage == BuildConfig.APPLICATION_ID) {
            val c = param.result as? Cursor ?: MatrixCursor(arrayOf("binder"))
            c.extras = c.extras.apply {
                putBinder("me.gm.cleaner.plugin.intent.extra.BINDER", service)
            }
            param.result = c
        }
    }
}
