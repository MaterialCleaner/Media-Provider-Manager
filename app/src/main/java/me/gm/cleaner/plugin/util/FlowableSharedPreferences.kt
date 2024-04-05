/*
 * Copyright 2024 Green Mushroom
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

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FlowableSharedPreferences<T>(
    private val preferences: SharedPreferences,
    private val key: String,
    defaultValue: T,
) {
    private val _preferenceFlow: MutableStateFlow<T> = MutableStateFlow(
        @Suppress("UNCHECKED_CAST")
        when (defaultValue) {
            is String -> preferences.getString(key, defaultValue) as T
            is Set<*> -> preferences.getStringSet(key, defaultValue as Set<String>) as T
            is Int -> preferences.getInt(key, defaultValue) as T
            is Long -> preferences.getLong(key, defaultValue) as T
            is Float -> preferences.getFloat(key, defaultValue) as T
            is Boolean -> preferences.getBoolean(key, defaultValue) as T
            else -> throw IllegalArgumentException("Unsupported type")
        }
    )

    var value: T
        get() = _preferenceFlow.value
        set(value) {
            _preferenceFlow.value = value
            preferences.edit {
                when (value) {
                    is String -> putString(key, value as String)
                    is Set<*> -> putStringSet(key, value as Set<String>)
                    is Int -> putInt(key, value as Int)
                    is Long -> putLong(key, value as Long)
                    is Float -> putFloat(key, value as Float)
                    is Boolean -> putBoolean(key, value as Boolean)
                    else -> throw IllegalArgumentException("Unsupported type")
                }
            }
        }

    fun asFlow(): StateFlow<T> = _preferenceFlow
}
