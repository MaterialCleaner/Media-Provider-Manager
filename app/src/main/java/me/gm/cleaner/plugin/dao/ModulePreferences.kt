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
import androidx.preference.PreferenceManager
import me.gm.cleaner.plugin.R
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import rikka.core.os.FileUtils
import java.io.*
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
    private lateinit var sp: SharedPreferences
    private lateinit var res: Resources
    private lateinit var modulePreferences: File
    private var modulePreferencesCache: JSONObject? = null
    private var broadcasting = false
    private val mListeners: MutableSet<PreferencesChangeListener> = HashSet()

    fun init(context: Context) {
        res = context.resources
        DISPLAY_NAME = res.getString(R.string.display_name)
        RELATIVE_PATH = res.getString(R.string.relative_path)
        MIME_TYPE = res.getString(R.string.mime_type)

        sp = PreferenceManager.getDefaultSharedPreferences(context)
        modulePreferences = File(context.filesDir, "modulePreferences")
        readPackage()
    }

    fun setOnPreferenceChangeListener(l: PreferencesChangeListener) {
        mListeners.add(l)
        if (broadcasting) {
            return
        }
        broadcasting = true
        l.onPreferencesChanged(false)
        broadcasting = false
    }

    private fun notifyPreferenceChanged(shouldNotifyServer: Boolean) {
        if (broadcasting) {
            return
        }
        broadcasting = true
        mListeners.forEach {
            it.onPreferencesChanged(shouldNotifyServer)
        }
        broadcasting = false
    }

    // APP LIST CONFIG
    fun putSortBy(value: Int) {
        val editor = sp.edit()
        editor.putInt(res.getString(R.string.sort_key), value)
        editor.apply()
        notifyPreferenceChanged(false)
    }

    val sortBy: Int
        get() = sp.getInt(res.getString(R.string.sort_key), SORT_BY_NAME)

    // MODULE PREFERENCES
    fun putPackage(
        packageName: String, displayName: String, relativePath: String, mimeType: String
    ) {
        val all = readPackage()
        var rule = JSONArray()
        try {
            if (all.has(packageName)) {
                rule = all.getJSONArray(packageName)
            }
        } catch (ignored: JSONException) {
        }
        try {
            rule.put(
                JSONObject()
                    .put(DISPLAY_NAME, displayName)
                    .put(RELATIVE_PATH, relativePath)
                    .put(MIME_TYPE, mimeType)
            )
            all.put(packageName, rule)
            writePackage(all)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun removePackage(packageName: String) {
        val all = readPackage()
        if (!all.has(packageName)) return
        all.remove(packageName)
        writePackage(all)
    }

    fun removePackage(packageName: String, keys: Set<String>) {
        val all = readPackage()
        if (!all.has(packageName)) return
        try {
            val rule = all.getJSONObject(packageName)
            keys.forEach {
                rule.remove(it)
            }
            all.put(packageName, rule)
            writePackage(all)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun removeUninstalledPackage(installedPackages: Set<String>): Int {
        val all = readPackage()
        val it = all.keys()
        var count = 0
        var isWrite = false
        while (it.hasNext()) {
            val packageName = it.next()
            if (!installedPackages.contains(packageName)) {
                it.remove()
                isWrite = true
            } else count++
        }
        if (isWrite) {
            writePackage(all)
        }
        return count
    }

    fun getPackageRuleCount(packageName: String): Int {
        try {
            val all: JSONObject = readPackage()
            if (all.has(packageName)) {
                return all.getJSONArray(packageName).length()
            }
        } catch (ignored: JSONException) {
        }
        return 0
    }

    fun enquireAboutPackageRule(packageName: String): Map<String, String>? {
        try {
            val all = readPackage()
            if (all.has(packageName)) {
                return all.getJSONObject(packageName).let {
                    hashMapOf(
                        DISPLAY_NAME to it.getString(DISPLAY_NAME),
                        RELATIVE_PATH to it.getString(RELATIVE_PATH),
                        MIME_TYPE to it.getString(MIME_TYPE)
                    )
                }
            }
        } catch (ignored: JSONException) {
        }
        return null
    }

    fun clearPreferencesCache() {
        modulePreferencesCache = null
    }

    @Synchronized
    private fun writePackage(json: JSONObject) {
        try {
            modulePreferences.delete()
            modulePreferences.createNewFile()
            val writer = PrintWriter(FileWriter(modulePreferences))
            writer.write(json.toString())
            writer.flush()
            writer.close()
            modulePreferencesCache = json
            notifyPreferenceChanged(true)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun readPackage(): JSONObject {
        if (modulePreferencesCache == null) {
            synchronized(ModulePreferences::class.java) {
                try {
                    FileInputStream(modulePreferences).use { fips ->
                        ByteArrayOutputStream().use { baos ->
                            FileUtils.copy(fips, baos)
                            modulePreferencesCache = JSONObject(baos.toString())
                        }
                    }
                } catch (e: IOException) {
                    modulePreferencesCache = JSONObject()
                } catch (ignored: JSONException) {
                }
            }
        }
        return modulePreferencesCache!!
    }

    interface PreferencesChangeListener {
        fun onPreferencesChanged(shouldNotifyServer: Boolean)
    }
}
