package me.gm.cleaner.plugin.util

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import java.lang.reflect.Field

class PreferencesPackageInfo private constructor() : PackageInfo() {
    lateinit var label: String
    var srCount = 0

    companion object {
        private val fieldCache: MutableMap<Field, Field> by lazy { HashMap() }

        private fun PreferencesPackageInfo.copyFieldsFrom(old: PackageInfo) {
            try {
                for (oldField in old.javaClass.fields) {
                    oldField.isAccessible = true
                    val newFieldFromCache = fieldCache[oldField]
                    if (newFieldFromCache != null) {
                        newFieldFromCache.isAccessible = true
                        newFieldFromCache[this] = oldField[old]
                    } else {
                        for (newField in javaClass.fields) {
                            newField.isAccessible = true
                            if (oldField == newField) {
                                fieldCache[oldField] = newField
                                newField[this] = oldField[old]
                                break
                            }
                        }
                    }
                }
            } catch (th: Throwable) {
                LogUtils.handleThrowable(th)
            }
        }

        fun newInstance(old: PackageInfo, pm: PackageManager): PreferencesPackageInfo =
            PreferencesPackageInfo().apply {
                copyFieldsFrom(old)
                label = pm.getApplicationLabel(old.applicationInfo).toString()
//                srCount = ModulePreferences.getPackageSRCount(old.packageName)
            }

        fun PreferencesPackageInfo.copy(): PreferencesPackageInfo =
            PreferencesPackageInfo().also {
                it.copyFieldsFrom(this)
//                it.srCount = ModulePreferences.getPackageSRCount(packageName)
            }
    }
}
