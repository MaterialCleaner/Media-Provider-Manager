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

package me.gm.cleaner.plugin.settings
import android.content.pm.PackageInfo

class PreferencesPackageInfo : PackageInfo() {
    var ruleCount = 0

    companion object {
        fun newInstance(pi: PackageInfo): PreferencesPackageInfo {
            val ppi = PreferencesPackageInfo()
            try {
                for (field in pi.javaClass.declaredFields) {
                    field.isAccessible = true
                    val objectField = field[pi]
                    if (objectField != null) {
                        val newField = ppi.javaClass.getField(field.name)
                        newField.isAccessible = true
                        newField[ppi] = objectField
                    }
                }
            } catch (th: Throwable) {
                th.printStackTrace()
            }
           // ppi.ruleCount = getPackageRuleCount(pi.packageName)
            return ppi
        }
    }
}
