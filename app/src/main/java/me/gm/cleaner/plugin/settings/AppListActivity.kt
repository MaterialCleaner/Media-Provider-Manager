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

package me.gm.cleaner.plugin.settings

import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import me.gm.cleaner.plugin.BinderReceiver
import me.gm.cleaner.plugin.app.BaseActivity
import me.gm.cleaner.plugin.dao.ModulePreferences
import me.gm.cleaner.plugin.databinding.ApplistActivityBinding
import rikka.recyclerview.addFastScroller
import rikka.recyclerview.fixEdgeEffect
import rikka.widget.borderview.BorderView.OnBorderVisibilityChangedListener
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AppListActivity : BaseActivity() {
    private lateinit var adapter: AppListAdapter
    private val executor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ApplistActivityBinding.inflate(layoutInflater)
        adapter = AppListAdapter(this)
        binding.list.layoutManager = GridLayoutManager(this, 1)
        binding.list.setHasFixedSize(true)
        binding.list.fixEdgeEffect()
        binding.list.addFastScroller()
        binding.list.borderViewDelegate.borderVisibilityChangedListener =
            OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
                appBarLayout?.isRaised = !top
            }
        binding.list.adapter = adapter

        val viewModel = ViewModelProvider(this)[AppListViewModel::class.java]
        viewModel.isSearching.observe(this, {
            if (!it) {
                viewModel.refreshShowingList()
            }
        })
        viewModel.queryText.observe(this, {
            if (!TextUtils.isEmpty(it)) {
                viewModel.refreshSearchingList()
            }
        })

        viewModel.installedPackagesCache.observe(this, {
            viewModel.refreshShowingList()
            binding.listContainer.isRefreshing = false
        })
        viewModel.showingList.observe(this, {
            if (viewModel.isSearching()) {
                viewModel.refreshSearchingList()
            } else {
                adapter.submitList(it)
            }
        })
        viewModel.searchingList.observe(this, {
            if (viewModel.isSearching()) {
                adapter.submitList(it)
            }
        })

        viewModel.loadingProgress.observe(this, {
            if (it == -1) binding.progress.hide()
            else binding.progress.progress = it
        })
        if (viewModel.installedPackagesCache.value!!.isEmpty()) {
            Thread {
                viewModel.fetchInstalledPackages(packageManager)
            }.start()
        }

        ModulePreferences.setOnPreferenceChangeListener(object :
            ModulePreferences.PreferencesChangeListener {
            override fun onPreferencesChanged(shouldNotifyServer: Boolean) {
                executor.execute {
                    viewModel.refreshPreferencesCountInCache()
                }
                if (shouldNotifyServer) {
                    BinderReceiver.notifyPreferencesChanged()
                }
            }
        })
        binding.listContainer.setOnRefreshListener {
            binding.listContainer.isRefreshing = true
            executor.execute {
                viewModel.fetchInstalledPackages(packageManager)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean =
        if (adapter.onContextItemSelected(item)) true
        else super.onContextItemSelected(item)
}
