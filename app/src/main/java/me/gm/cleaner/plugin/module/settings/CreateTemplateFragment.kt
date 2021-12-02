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

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.content.edit
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceManager
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.dao.JsonSharedPreferencesImpl
import org.json.JSONException

class CreateTemplateFragment : AbsSettingsFragment() {
    override val who: Int
        get() = R.xml.template_preferences

    private val args: CreateTemplateFragmentArgs by navArgs()
    private val tempSp by lazy {
        try {
            JsonSharedPreferencesImpl(binderSp.getString(args.label, null))
        } catch (e: JSONException) {
            JsonSharedPreferencesImpl()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    @SuppressLint("RestrictedApi")
    override fun onCreatePreferenceManager() = object : PreferenceManager(context) {
        override fun getSharedPreferences() = tempSp
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.template_preferences, rootKey)
        val sharedPreferences = preferenceManager.sharedPreferences

        val templateName = getString(R.string.template_name_key)
        findPreference<EditTextPreference>(templateName)?.setDefaultValue(args.label)
        // TODO: unique templateName
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.toolbar_save, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_save -> {
            val templateName = getString(R.string.template_name_key)
            val label = tempSp.getString(templateName, null)
            if (!label.isNullOrEmpty()) {
                tempSp.edit(true) {
                    remove(templateName)
                }
                binderSp.edit {
                    putString(label, tempSp.toString())
                }
                findNavController().navigateUp()
                true
            } else {
                false
            }
        }
        else -> super.onOptionsItemSelected(item)
    }
}
