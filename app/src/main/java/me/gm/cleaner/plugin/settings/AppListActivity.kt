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
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gm.cleaner.plugin.BinderReceiver
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.app.BaseActivity
import me.gm.cleaner.plugin.dao.ModulePreferences
import me.gm.cleaner.plugin.databinding.ApplistActivityBinding
import me.gm.cleaner.plugin.util.DisplayUtils.getColorByAttr
import rikka.recyclerview.addFastScroller
import rikka.recyclerview.fixEdgeEffect
import rikka.widget.borderview.BorderView.OnBorderVisibilityChangedListener

class AppListActivity : BaseActivity() {
    private val viewModel by viewModels<AppListViewModel>()
    private lateinit var adapter: AppListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ApplistActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_outline_arrow_back_24)
        }

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
        viewModel.showingList.observe(this) {
            adapter.submitList(it)
        }

        if (viewModel.installedPackages.value!!.isEmpty()) {
            MainScope().launch(Dispatchers.Default) {
                viewModel.installedPackages.load(
                    packageManager, object : AppListLiveData.ProgressListener {
                        override fun onProgress(progress: Int) {
                            runOnUiThread {
                                binding.progress.progress = progress
                            }
                        }
                    }
                )
            }
        }

        ModulePreferences.setOnPreferenceChangeListener(object :
            ModulePreferences.PreferencesChangeListener {
            override fun onPreferencesChanged(isNotifyService: Boolean) {
                viewModel.installedPackages.refreshPreferencesCount()
                if (isNotifyService) {
                    BinderReceiver.notifyPreferencesChanged()
                }
            }
        })
        binding.listContainer.setOnRefreshListener {
            MainScope().launch {
                withContext(Dispatchers.Default) {
                    viewModel.installedPackages.load(packageManager, null)
                }
                binding.listContainer.isRefreshing = false
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean =
        if (adapter.onContextItemSelected(item)) true
        else super.onContextItemSelected(item)

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
            it.title = getSpannableString(it.title)
        }
        return super.onCreateOptionsMenu(menu)
    }

    private fun getSpannableString(text: CharSequence): SpannableStringBuilder =
        SpannableStringBuilder(text).apply {
            setSpan(
                ForegroundColorSpan(getColorByAttr(android.R.attr.colorPrimary)), 0, length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            setSpan(AbsoluteSizeSpan(14, true), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
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
