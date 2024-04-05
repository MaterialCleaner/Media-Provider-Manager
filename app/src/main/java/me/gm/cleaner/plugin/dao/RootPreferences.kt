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
import me.gm.cleaner.plugin.util.FlowableSharedPreferences

object RootPreferences {
    const val SORT_BY_APP_NAME = 0
    const val SORT_BY_UPDATE_TIME = 1
    const val SORT_BY_PATH = 0
    const val SORT_BY_DATE_TAKEN = 1
    const val SORT_BY_SIZE = 2
    private var broadcasting = false
    private val listeners by lazy { mutableListOf<PreferencesChangeListener>() }
    private lateinit var resources: Resources
    private lateinit var defaultSp: SharedPreferences

    fun init(context: Context) {
        resources = context.resources
        defaultSp = PreferenceManager.getDefaultSharedPreferences(context)
    }

    interface PreferencesChangeListener {
        val lifecycle: Lifecycle
        fun onPreferencesChanged()
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

    private fun putBoolean(key: String, value: Boolean) {
        defaultSp.edit {
            putBoolean(key, value)
        }
        notifyListeners()
    }

    private fun putInt(key: String, value: Int) {
        defaultSp.edit {
            putInt(key, value)
        }
        notifyListeners()
    }

    var startDestination: Int
        get() = defaultSp.getInt(
            resources.getString(R.string.start_destination_key), R.id.about_fragment
        )
        set(value) = defaultSp.edit {
            putInt(resources.getString(R.string.start_destination_key), value)
        }

    // APP LIST
    val sortBy: FlowableSharedPreferences<Int> by lazy {
        FlowableSharedPreferences(
            defaultSp,
            resources.getString(R.string.sort_key),
            SORT_BY_APP_NAME
        )
    }
    val ruleCount: FlowableSharedPreferences<Boolean> by lazy {
        FlowableSharedPreferences(
            defaultSp,
            resources.getString(R.string.menu_rule_count_key),
            true
        )
    }
    val isHideSystemApp: FlowableSharedPreferences<Boolean> by lazy {
        FlowableSharedPreferences(
            defaultSp,
            resources.getString(R.string.menu_hide_system_app_key),
            true
        )
    }

    // USAGE RECORD
    val isHideQuery: FlowableSharedPreferences<Boolean> by lazy {
        FlowableSharedPreferences(
            defaultSp,
            resources.getString(R.string.menu_hide_query_key),
            false
        )
    }
    val isHideInsert: FlowableSharedPreferences<Boolean> by lazy {
        FlowableSharedPreferences(
            defaultSp,
            resources.getString(R.string.menu_hide_insert_key),
            false
        )
    }
    val isHideDelete: FlowableSharedPreferences<Boolean> by lazy {
        FlowableSharedPreferences(
            defaultSp,
            resources.getString(R.string.menu_hide_delete_key),
            false
        )
    }

    // MEDIA STORE
    var isShowAllMediaFiles: Boolean
        get() = defaultSp.getBoolean(resources.getString(R.string.menu_show_all_key), true)
        set(value) = putBoolean(resources.getString(R.string.menu_show_all_key), value)

    var sortMediaBy: Int
        get() = defaultSp.getInt(resources.getString(R.string.sort_media_key), SORT_BY_PATH)
        set(value) = putInt(resources.getString(R.string.sort_media_key), value)

    var spanCount: Int
        get() = defaultSp.getInt(resources.getString(R.string.span_count_key), 3)
        set(value) = putInt(resources.getString(R.string.span_count_key), value)

    var playbackSpeed: Float
        get() = defaultSp.getFloat(resources.getString(R.string.playback_speed_key), 1F)
        set(value) = defaultSp.edit {
            putFloat(resources.getString(R.string.playback_speed_key), value)
        }
}
