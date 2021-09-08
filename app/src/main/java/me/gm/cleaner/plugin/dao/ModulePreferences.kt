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

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import androidx.preference.PreferenceManager
import me.gm.cleaner.plugin.R

@SuppressLint("StaticFieldLeak")
object ModulePreferences {
    const val SORT_BY_NAME = 0
    const val SORT_BY_UPDATE_TIME = 1
    private lateinit var DISPLAY_NAME: String
    private lateinit var RELATIVE_PATH: String
    private lateinit var MIME_TYPE: String
    private var broadcasting = false
    private val listeners: MutableSet<PreferencesChangeListener> = HashSet()
    private lateinit var defaultSp: SharedPreferences
    private lateinit var context: Context

    fun init(context: Context) {
        this.context = context
        DISPLAY_NAME = context.getString(R.string.display_name)
        RELATIVE_PATH = context.getString(R.string.relative_path)
        MIME_TYPE = context.getString(R.string.mime_type)

        defaultSp = PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun setOnPreferenceChangeListener(l: PreferencesChangeListener) {
        listeners.add(l)
        if (broadcasting) {
            return
        }
        broadcasting = true
        l.onPreferencesChanged(false)
        broadcasting = false
    }

    private fun notifyListeners(isNotifyService: Boolean) {
        if (broadcasting) {
            return
        }
        broadcasting = true
        listeners.forEach {
            it.onPreferencesChanged(isNotifyService)
        }
        broadcasting = false
    }

    // APP LIST CONFIG
    var sortBy: Int
        get() = defaultSp.getInt(context.resources.getString(R.string.sort_key), SORT_BY_NAME)
        set(value) {
            defaultSp.edit().apply {
                putInt(context.resources.getString(R.string.sort_key), value)
                apply()
            }
            notifyListeners(false)
        }

    var ruleCount: Boolean
        get() = defaultSp.getBoolean(context.getString(R.string.menu_rule_count_key), true)
        set(value) = putBoolean(context.getString(R.string.menu_rule_count_key), value)

    var isHideSystemApp: Boolean
        get() = defaultSp.getBoolean(context.getString(R.string.menu_hide_system_app_key), true)
        set(value) = putBoolean(context.getString(R.string.menu_hide_system_app_key), value)

    var isHideNoStoragePermissionApp: Boolean
        get() = defaultSp.getBoolean(
            context.getString(R.string.menu_hide_no_storage_permission_key), true
        )
        set(value) = putBoolean(
            context.getString(R.string.menu_hide_no_storage_permission_key), value
        )

    private fun putBoolean(key: String, value: Boolean) {
        defaultSp.edit().apply {
            putBoolean(key, value)
            apply()
        }
        notifyListeners(false)
    }

    // MODULE PREFERENCES
    fun removePackage(packageName: String) {
        context.deleteSharedPreferences(packageName)
        notifyListeners(true)
    }

    fun putPackage(packageName: String, key: String, value: String?) {
        context.getSharedPreferences(packageName, Context.MODE_PRIVATE).edit().apply {
            putString(key, value)
            commit()
        }
        notifyListeners(true)
    }

    fun getStringSet(packageName: String, key: String): String? {
        val sp = context.getSharedPreferences(packageName, Context.MODE_PRIVATE)
        return sp.getString(key, null)
    }

    fun enquireAboutPackagePreferences(packageName: String): List<String> {
        val list = mutableListOf<String>()
        if (!TextUtils.isEmpty(getStringSet(packageName, DISPLAY_NAME))) {
            list.add(DISPLAY_NAME)
        }
        if (!TextUtils.isEmpty(getStringSet(packageName, RELATIVE_PATH))) {
            list.add(RELATIVE_PATH)
        }
        if (!TextUtils.isEmpty(getStringSet(packageName, MIME_TYPE))) {
            list.add(MIME_TYPE)
        }
        return list
    }

    interface PreferencesChangeListener {
        fun onPreferencesChanged(isNotifyService: Boolean)
    }
}
