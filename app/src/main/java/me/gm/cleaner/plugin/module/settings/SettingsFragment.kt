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

package me.gm.cleaner.plugin.module.settings

import android.os.Bundle
import androidx.preference.SwitchPreferenceCompat
import me.gm.cleaner.plugin.R

class SettingsFragment : AbsSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        val sharedPreferences = preferenceManager.sharedPreferences

        val scanForObsoleteInsert = getString(R.string.scan_for_obsolete_insert_key)
        findPreference<SwitchPreferenceCompat>(scanForObsoleteInsert)?.isChecked =
            sharedPreferences.getBoolean(scanForObsoleteInsert, true)

        val usageRecord = getString(R.string.usage_record_key)
        findPreference<SwitchPreferenceCompat>(usageRecord)?.isChecked =
            sharedPreferences.getBoolean(usageRecord, true)
    }
}
