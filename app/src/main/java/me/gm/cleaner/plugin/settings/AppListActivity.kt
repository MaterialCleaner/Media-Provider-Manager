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
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import me.gm.cleaner.plugin.BinderReceiver
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.app.BaseActivity
import me.gm.cleaner.plugin.dao.ModulePreferences
import me.gm.cleaner.plugin.databinding.ApplistActivityBinding
import me.gm.cleaner.plugin.util.colorPrimary
import me.gm.cleaner.plugin.util.initFastScroller
import rikka.recyclerview.fixEdgeEffect
import rikka.widget.borderview.BorderView.OnBorderVisibilityChangedListener

class AppListActivity : BaseActivity() {
    private val viewModel: AppListViewModel by viewModels()
    private val adapter by lazy { AppListAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ApplistActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_outline_arrow_back_24)
        }

        val list = binding.list
        list.adapter = adapter
        list.layoutManager = GridLayoutManager(this, 1)
        list.setHasFixedSize(true)
        list.fixEdgeEffect()
        list.initFastScroller()
        list.borderViewDelegate.borderVisibilityChangedListener =
            OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
                appBarLayout?.isRaised = !top
            }
        binding.listContainer.setOnRefreshListener {
            viewModel.loadApps(packageManager, null)
        }

        // Start a coroutine in the lifecycle scope
        lifecycleScope.launch {
            // repeatOnLifecycle launches the block in a new coroutine every time the
            // lifecycle is in the STARTED state (or above) and cancels it when it's STOPPED.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Trigger the flow and start listening for values.
                // Note that this happens when lifecycle is STARTED and stops
                // collecting when the lifecycle is STOPPED
                viewModel.apps.collect { apps ->
                    // New value received
                    when (apps) {
                        is SourceState.Loading -> binding.progress.progress = apps.progress
                        is SourceState.Done -> {
                            binding.progress.progress = 0
                            binding.listContainer.isRefreshing = false
                            adapter.submitList(apps.list)
                        }
                    }
                }
            }
        }
        savedInstanceState ?: viewModel.loadApps(packageManager)

        ModulePreferences.setOnPreferenceChangeListener(object :
            ModulePreferences.PreferencesChangeListener {
            override fun onPreferencesChanged(isNotifyService: Boolean) {
                viewModel.updateApps()
                if (isNotifyService) {
                    BinderReceiver.notifyPreferencesChanged()
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_applist, menu)
        val searchItem = menu.findItem(R.id.menu_search)
        if (viewModel.isSearching) {
            searchItem.expandActionView()
        }
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                viewModel.isSearching = true
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                viewModel.isSearching = false
                return true
            }
        })
        val searchView = searchItem.actionView as SearchView
        searchView.setQuery(viewModel.queryText, false)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                viewModel.queryText = query
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                viewModel.queryText = newText
                return false
            }
        })

        when (ModulePreferences.sortBy) {
            ModulePreferences.SORT_BY_NAME ->
                menu.findItem(R.id.menu_sort_by_name).isChecked = true
            ModulePreferences.SORT_BY_UPDATE_TIME ->
                menu.findItem(R.id.menu_sort_by_update_time).isChecked = true
        }
        menu.findItem(R.id.menu_rule_count).isChecked = ModulePreferences.ruleCount
        menu.findItem(R.id.menu_hide_system_app).isChecked = ModulePreferences.isHideSystemApp
        menu.findItem(R.id.menu_hide_no_storage_permission).isChecked =
            ModulePreferences.isHideNoStoragePermissionApp
        listOf(menu.findItem(R.id.menu_header_sort), menu.findItem(R.id.menu_header_hide)).forEach {
            it.isEnabled = false
            it.title = SpannableStringBuilder(it.title).apply {
                setSpan(
                    ForegroundColorSpan(colorPrimary), 0, length, Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                setSpan(AbsoluteSizeSpan(14, true), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_by_name -> {
                ModulePreferences.sortBy = ModulePreferences.SORT_BY_NAME
                item.isChecked = true
            }
            R.id.menu_sort_by_update_time -> {
                ModulePreferences.sortBy = ModulePreferences.SORT_BY_UPDATE_TIME
                item.isChecked = true
            }
            R.id.menu_rule_count -> {
                val ruleCount = !item.isChecked
                item.isChecked = ruleCount
                ModulePreferences.ruleCount = ruleCount
            }
            R.id.menu_hide_system_app -> {
                val isHideSystemApp = !item.isChecked
                item.isChecked = isHideSystemApp
                ModulePreferences.isHideSystemApp = isHideSystemApp
            }
            R.id.menu_hide_no_storage_permission -> {
                val isHideNoStoragePermissionApp = !item.isChecked
                item.isChecked = isHideNoStoragePermissionApp
                ModulePreferences.isHideNoStoragePermissionApp = isHideNoStoragePermissionApp
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
