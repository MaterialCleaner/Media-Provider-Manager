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

package me.gm.cleaner.plugin.module.appmanagement

import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.model.Templates
import me.gm.cleaner.plugin.module.BinderViewModel
import java.util.concurrent.atomic.AtomicInteger

class AppListLoader(private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default) {

    private fun fetchRuleCount(templates: Templates): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        templates.values.forEach { templateName ->
            templateName.applyToApp?.forEach { packageName ->
                map[packageName] = map.getOrDefault(packageName, 0) + 1
            }
        }
        return map
    }

    suspend fun load(
        binderViewModel: BinderViewModel, pm: PackageManager, l: ProgressListener?
    ) = withContext(defaultDispatcher) {
        val packageNameToRuleCount =
            fetchRuleCount(Templates(binderViewModel.readSp(R.xml.template_preferences)))
        val installedPackages = binderViewModel.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        val size = installedPackages.size
        val count = AtomicInteger(0)
        installedPackages.map { pi ->
            ensureActive()
            l?.onProgress(100 * count.incrementAndGet() / size)
            AppListModel(
                pi,
                pm.getApplicationLabel(pi.applicationInfo).toString(),
                packageNameToRuleCount.getOrDefault(pi.packageName, 0),
            )
        }
    }

    suspend fun update(old: List<AppListModel>, binderViewModel: BinderViewModel) =
        withContext(defaultDispatcher) {
            val packageNameToRuleCount =
                fetchRuleCount(Templates(binderViewModel.readSp(R.xml.template_preferences)))
            old.map {
                it.copy(
                    ruleCount = packageNameToRuleCount.getOrDefault(it.packageInfo.packageName, 0)
                )
            }
        }

    interface ProgressListener {
        fun onProgress(progress: Int)
    }
}
