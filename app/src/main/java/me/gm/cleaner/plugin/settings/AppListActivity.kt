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
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
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
    private lateinit var adapter: AppListAdapter
    private lateinit var viewModel: AppListViewModel

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

        viewModel = ViewModelProvider(this)[AppListViewModel::class.java]
        viewModel.isSearching.observe(this) {
            if (!it) {
                viewModel.refreshShowingList()
            }
        }
        viewModel.queryText.observe(this) {
            if (!TextUtils.isEmpty(it)) {
                viewModel.refreshSearchingList()
            }
        }

        viewModel.installedPackagesCache.observe(this) {
            viewModel.refreshShowingList()
            binding.listContainer.isRefreshing = false
        }
        viewModel.showingList.observe(this) {
            if (viewModel.isSearching()) {
                viewModel.refreshSearchingList()
            } else {
                adapter.submitList(it)
            }
        }
        viewModel.searchingList.observe(this) {
            if (viewModel.isSearching()) {
                adapter.submitList(it)
            }
        }

        viewModel.loadingProgress.observe(this) {
            if (it == -1) binding.progress.hide()
            else binding.progress.progress = it
        }
        if (viewModel.installedPackagesCache.value!!.isEmpty()) {
            MainScope().launch(Dispatchers.Default) {
                viewModel.fetchInstalledPackages(packageManager)
            }
        }

        ModulePreferences.setOnPreferenceChangeListener(object :
            ModulePreferences.PreferencesChangeListener {
            override fun onPreferencesChanged(shouldNotifyServer: Boolean) {
                MainScope().launch(Dispatchers.Default) {
                    viewModel.refreshPreferencesCountInCache()
                }
                if (shouldNotifyServer) {
                    BinderReceiver.notifyPreferencesChanged()
                }
            }
        })
        binding.listContainer.setOnRefreshListener {
            binding.listContainer.isRefreshing = true
            MainScope().launch(Dispatchers.Default) {
                viewModel.fetchInstalledPackages(packageManager)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean =
        if (adapter.onContextItemSelected(item)) true
        else super.onContextItemSelected(item)

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_applist, menu)
        val searchItem = menu.findItem(R.id.menu_search)
        if (viewModel.isSearching()) searchItem.expandActionView()
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                viewModel.isSearching.postValue(true)
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                viewModel.isSearching.postValue(false)
                return true
            }
        })
        val searchView = searchItem.actionView as SearchView
        searchView.setQuery(viewModel.queryText.value, false)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                viewModel.queryText.postValue(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                viewModel.queryText.postValue(newText)
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

        for (headerItem in listOf<MenuItem>(
            menu.findItem(R.id.menu_header_sort), menu.findItem(R.id.menu_header_hide)
        )) {
            headerItem.apply {
                isEnabled = false
                title = getSpannableString(title)
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    private fun getSpannableString(text: CharSequence): SpannableStringBuilder =
        SpannableStringBuilder(text).apply {
            setSpan(
                ForegroundColorSpan(getColorByAttr(R.attr.colorPrimary)), 0, length,
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
        return super.onOptionsItemSelected(item)
    }
}
