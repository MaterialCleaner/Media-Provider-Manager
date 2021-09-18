/*
 * Copyright 2021 Green Mushroom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
                old.javaClass.fields.forEach {
                    it.isAccessible = true
                    val newFieldFromCache = fieldCache[it]
                    if (newFieldFromCache != null) {
                        newFieldFromCache.isAccessible = true
                        newFieldFromCache[this] = it[old]
                    } else {
                        for (newField in javaClass.fields) {
                            newField.isAccessible = true
                            if (it == newField) {
                                fieldCache[it] = newField
                                newField[this] = it[old]
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
