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

package me.gm.cleaner.plugin.ui.module.settings.preference

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.icu.text.ListFormatter
import android.util.AttributeSet
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.TypedArrayUtils
import androidx.lifecycle.lifecycleScope
import androidx.preference.MultiSelectListPreference
import androidx.preference.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Arrays
import java.util.function.Consumer
import java.util.function.Supplier

@SuppressLint("RestrictedApi", "PrivateResource")
class AppListMultiSelectListPreference @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = TypedArrayUtils.getAttr(
        context, R.attr.dialogPreferenceStyle, android.R.attr.dialogPreferenceStyle
    ), defStyleRes: Int = 0
) : MultiSelectListPreference(context, attrs, defStyleAttr, defStyleRes) {
    private val lifecycleScope = (context as AppCompatActivity).lifecycleScope
    private lateinit var packageNameToLabel: List<Pair<String, CharSequence>>
    private var onAppsLoadedListener: Consumer<AppListMultiSelectListPreference>? = null

    /** Delay showDialog if applist is not loaded when clicked. */
    private val mutex = Mutex()

    fun loadApps(applistSupplier: Supplier<List<PackageInfo>>): AppListMultiSelectListPreference {
        lifecycleScope.launch {
            mutex.withLock {
                val pm = context.packageManager
                val collator = Collator.getInstance()
                packageNameToLabel = withContext(Dispatchers.Default) {
                    applistSupplier.get()
                        .map { it.packageName to pm.getApplicationLabel(it.applicationInfo) }
                        .sortedWith { o1, o2 -> collator.compare(o1?.second, o2?.second) }
                }
                liftSelected()
            }
            summaryProvider = instance
        }
        return this
    }

    fun setOnAppsLoadedListener(l: Consumer<AppListMultiSelectListPreference>): AppListMultiSelectListPreference {
        onAppsLoadedListener = l
        return this
    }

    override fun setValues(values: Set<String>) {
        super.setValues(values)
        liftSelected()
    }

    private fun liftSelected() {
        if (!::packageNameToLabel.isInitialized) {
            return
        }
        val list = packageNameToLabel
            .sortedWith(Comparator.comparing { it.first !in values })
            .unzip()
        entries = list.second.toTypedArray()
        entryValues = list.first.toTypedArray()

        val l = onAppsLoadedListener
        // Avoid infinite recursions if liftSelected() is called from a listener
        onAppsLoadedListener = null
        l?.accept(this)
    }

    override fun onClick() {
        lifecycleScope.launch {
            mutex.withLock {
                super.onClick()
            }
        }
    }

    class SimpleSummaryProvider : SummaryProvider<AppListMultiSelectListPreference> {
        override fun provideSummary(preference: AppListMultiSelectListPreference): CharSequence =
            if (preference.values.isEmpty()) {
                preference.context.getString(R.string.not_set)
            } else {
                val values = Arrays.stream(preference.entryValues)
                    .filter { preference.values.contains(it) }
                    .map { preference.entries[preference.entryValues.indexOf(it)] }
                    .toArray()
                ListFormatter.getInstance().format(*values)
            }
    }

    companion object {
        val instance by lazy { SimpleSummaryProvider() }
    }
}
