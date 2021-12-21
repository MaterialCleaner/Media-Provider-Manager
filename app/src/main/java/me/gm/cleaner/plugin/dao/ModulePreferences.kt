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

package me.gm.cleaner.plugin.dao

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.core.content.edit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.preference.PreferenceManager
import me.gm.cleaner.plugin.R

object ModulePreferences {
    const val SORT_BY_APP_NAME = 0
    const val SORT_BY_UPDATE_TIME = 1
    const val SORT_BY_FILE_NAME = 0
    const val SORT_BY_DATE_TAKEN = 1
    private var broadcasting = false
    private val listeners by lazy { mutableListOf<PreferencesChangeListener>() }
    private lateinit var resources: Resources
    private lateinit var defaultSp: SharedPreferences

    fun init(context: Context) {
        resources = context.resources
        defaultSp = PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun addOnPreferenceChangeListener(l: PreferencesChangeListener) {
        listeners.add(l)
        l.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                l.lifecycle.removeObserver(this)
                listeners.remove(l)
            }
        })
    }

    private fun notifyListeners() {
        if (broadcasting) {
            return
        }
        broadcasting = true
        listeners.forEach {
            it.onPreferencesChanged()
        }
        broadcasting = false
    }

    var startDestination: Int
        get() = defaultSp.getInt(
            resources.getString(R.string.start_destination_key), R.id.about_fragment
        )
        set(value) {
            defaultSp.edit {
                putInt(resources.getString(R.string.start_destination_key), value)
            }
        }

    // APP LIST CONFIG
    var sortBy: Int
        get() = defaultSp.getInt(resources.getString(R.string.sort_key), SORT_BY_APP_NAME)
        set(value) = putInt(resources.getString(R.string.sort_key), value)
    var ruleCount: Boolean
        get() = defaultSp.getBoolean(resources.getString(R.string.menu_rule_count_key), true)
        set(value) = putBoolean(resources.getString(R.string.menu_rule_count_key), value)
    var isHideSystemApp: Boolean
        get() = defaultSp.getBoolean(resources.getString(R.string.menu_hide_system_app_key), true)
        set(value) = putBoolean(resources.getString(R.string.menu_hide_system_app_key), value)
    var isHideNoStoragePermissionApp: Boolean
        get() = defaultSp.getBoolean(
            resources.getString(R.string.menu_hide_no_storage_permission_key), true
        )
        set(value) = putBoolean(
            resources.getString(R.string.menu_hide_no_storage_permission_key), value
        )

    // USAGE RECORD CONFIG
    var isHideQuery: Boolean
        get() = defaultSp.getBoolean(resources.getString(R.string.menu_hide_query_key), false)
        set(value) = putBoolean(resources.getString(R.string.menu_hide_query_key), value)
    var isHideInsert: Boolean
        get() = defaultSp.getBoolean(resources.getString(R.string.menu_hide_insert_key), false)
        set(value) = putBoolean(resources.getString(R.string.menu_hide_insert_key), value)
    var isHideDelete: Boolean
        get() = defaultSp.getBoolean(resources.getString(R.string.menu_hide_delete_key), false)
        set(value) = putBoolean(resources.getString(R.string.menu_hide_delete_key), value)

    // MEDIA STORE CONFIG
    var isShowAllMediaFiles: Boolean
        get() = defaultSp.getBoolean(resources.getString(R.string.menu_show_all_key), true)
        set(value) = putBoolean(resources.getString(R.string.menu_show_all_key), value)

    // MEDIA STORE
    var sortMediaBy: Int
        get() = defaultSp.getInt(resources.getString(R.string.sort_media_key), SORT_BY_FILE_NAME)
        set(value) = putInt(resources.getString(R.string.sort_media_key), value)

    private fun putBoolean(key: String, value: Boolean) {
        val isValueChanged = defaultSp.getBoolean(key, value) != value
        defaultSp.edit {
            putBoolean(key, value)
        }
        if (isValueChanged) {
            notifyListeners()
        }
    }

    private fun putInt(key: String, value: Int) {
        val isValueChanged = defaultSp.getInt(key, value) != value
        defaultSp.edit {
            putInt(key, value)
        }
        if (isValueChanged) {
            notifyListeners()
        }
    }

    interface PreferencesChangeListener {
        val lifecycle: Lifecycle
        fun onPreferencesChanged()
    }
}
