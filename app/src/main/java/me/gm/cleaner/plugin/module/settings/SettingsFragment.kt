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
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.transition.platform.Hold
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.ktx.mediumAnimTime

class SettingsFragment : AbsSettingsFragment() {
    override val who: Int
        get() = R.xml.root_preferences

    private val navController by lazy { findNavController() }
    var enterKey: String? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val scanForObsoleteInsert = getString(R.string.scan_for_obsolete_insert_key)
        findPreference<SwitchPreferenceCompat>(scanForObsoleteInsert)?.isChecked =
            remoteSp.getBoolean(scanForObsoleteInsert, true)

        val usageRecord = getString(R.string.usage_record_key)
        findPreference<SwitchPreferenceCompat>(usageRecord)?.isChecked =
            remoteSp.getBoolean(usageRecord, true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ) = super.onCreateView(inflater, container, savedInstanceState).also {
        parentFragment?.setFragmentResultListener(TemplatesFragment::class.java.name) { _, bundle ->
            enterKey = bundle.getString(TemplatesFragment.KEY)
            parentFragment?.postponeEnterTransition()
        }
    }

    override fun onBindPreferencesViewHolder(holder: PreferenceViewHolder, preference: Preference) {
        val itemView = holder.itemView
        when (preference.key) {
            getString(R.string.template_management_key) -> {
                itemView.transitionName = preference.key
                itemView.setOnClickListener {
                    if (navController.currentDestination?.id != R.id.settings_fragment) {
                        return@setOnClickListener
                    }
                    enterKey = preference.key
                    parentFragment?.exitTransition = Hold().apply {
                        duration = requireContext().mediumAnimTime
                    }

                    val direction = SettingsFragmentStubDirections.actionSettingsToTemplates()
                    val extras = FragmentNavigatorExtras(it to it.transitionName)
                    navController.navigate(direction, extras)
                }
                parentFragment?.startPostponedEnterTransition()
            }
        }
    }
}
