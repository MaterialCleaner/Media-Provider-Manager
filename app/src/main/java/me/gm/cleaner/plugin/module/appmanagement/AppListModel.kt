/*
 * Copyright 2022 Green Mushroom
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

package me.gm.cleaner.plugin.module.appmanagement

import android.content.pm.PackageInfo

data class AppListModel(
    val packageInfo: PackageInfo,
    val label: String,
    val ruleCount: Int,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppListModel

        if (label != other.label) return false
        if (ruleCount != other.ruleCount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = label.hashCode()
        result = 31 * result + ruleCount
        return result
    }
}
