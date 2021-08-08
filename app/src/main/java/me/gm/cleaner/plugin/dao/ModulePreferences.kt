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
import androidx.preference.PreferenceManager
import me.gm.cleaner.plugin.R
import java.lang.ref.WeakReference
import java.util.*

object ModulePreferences {
    const val SORT_BY_NAME = 0
    const val SORT_BY_UPDATE_TIME = 1
    lateinit var DISPLAY_NAME: String
        private set
    lateinit var RELATIVE_PATH: String
        private set
    lateinit var MIME_TYPE: String
        private set
    private var broadcasting = false
    private val listeners: MutableSet<PreferencesChangeListener> = HashSet()
    private lateinit var defaultSp: SharedPreferences
    private lateinit var contextRef: WeakReference<Context>
    private val context: Context
        get() = contextRef.get()!!

    fun init(context: Context) {
        contextRef = WeakReference(context)
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

    private fun notifyListeners(shouldNotifyServer: Boolean) {
        if (broadcasting) {
            return
        }
        broadcasting = true
        listeners.forEach {
            it.onPreferencesChanged(shouldNotifyServer)
        }
        broadcasting = false
    }

    // APP LIST CONFIG
    fun putSortBy(value: Int) {
        val editor = defaultSp.edit()
        editor.putInt(context.getString(R.string.sort_key), value)
        editor.apply()
        notifyListeners(false)
    }

    val sortBy: Int
        get() = defaultSp.getInt(context.getString(R.string.sort_key), SORT_BY_NAME)

    // MODULE PREFERENCES
    fun removePackage(packageName: String) {
        context.deleteSharedPreferences(packageName)
        notifyListeners(true)
    }

    fun putPackage(packageName: String, key: String, value: String?) {
        val sp = context.getSharedPreferences(packageName, Context.MODE_PRIVATE)
        sp.edit().apply {
            putString(key, value)
            commit()
        }
        notifyListeners(true)
    }

    fun getStringSet(packageName: String, key: String): String? {
        val sp = context.getSharedPreferences(packageName, Context.MODE_PRIVATE)
        return sp.getString(key, null)
    }

    interface PreferencesChangeListener {
        fun onPreferencesChanged(shouldNotifyServer: Boolean)
    }
}
