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

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.launch
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.dao.ModulePreferences
import me.gm.cleaner.plugin.databinding.ApplistFragmentBinding
import me.gm.cleaner.plugin.ktx.*
import me.gm.cleaner.plugin.module.ModuleFragment
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import me.zhanghai.android.fastscroll.SimpleRecyclerViewHelper
import rikka.recyclerview.fixEdgeEffect

class AppListFragment : ModuleFragment() {
    private val viewModel: AppListViewModel by viewModels()
    var enterPackageName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        if (!binderViewModel.pingBinder()) {
            return super.onCreateView(inflater, container, savedInstanceState)
        }
        val binding = ApplistFragmentBinding.inflate(layoutInflater)

        val adapter = AppListAdapter(this)
        val list = binding.list
        list.adapter = adapter
        list.layoutManager = GridLayoutManager(requireContext(), 1)
        list.setHasFixedSize(true)
        val fastScroller = FastScrollerBuilder(list)
            .useMd2Style()
            .setViewHelper(SimpleRecyclerViewHelper(list))
            .build()
        list.fixEdgeEffect(false)
        list.overScrollIfContentScrollsPersistent()
        list.addLiftOnScrollListener { appBarLayout.isLifted = it }
        list.fitsSystemWindowInsetBottom(fastScroller)
        binding.listContainer.setOnRefreshListener {
            viewModel.loadApps(binderViewModel, requireContext(), null)
        }

        // Start a coroutine in the lifecycle scope
        lifecycleScope.launch {
            // repeatOnLifecycle launches the block in a new coroutine every time the
            // lifecycle is in the STARTED state (or above) and cancels it when it's STOPPED.
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Trigger the flow and start listening for values.
                // Note that this happens when lifecycle is STARTED and stops
                // collecting when the lifecycle is STOPPED
                viewModel.appsFlow.collect { apps ->
                    // New value received
                    when (apps) {
                        is SourceState.Loading -> binding.progress.progress = apps.progress
                        is SourceState.Done -> {
                            binding.progress.hide()
                            binding.listContainer.isRefreshing = false
                            adapter.submitListKeepPosition(apps.list, list)
                        }
                    }
                }
            }
        }
        if (savedInstanceState == null && viewModel.isLoading) {
            viewModel.loadApps(binderViewModel, requireContext())
        }
        setFragmentResultListener(AppFragment::class.java.name) { _, bundle ->
            enterPackageName = bundle.getString(AppFragment.KEY_PACKAGENAME)
            postponeEnterTransition()
        }

        binderViewModel.remoteSpCacheLiveData.observe(viewLifecycleOwner) {
            viewModel.updateApps(binderViewModel)
        }
        ModulePreferences.addOnPreferenceChangeListener(object :
            ModulePreferences.PreferencesChangeListener {
            override val lifecycle = getLifecycle()
            override fun onPreferencesChanged() {
                viewModel.updateApps(binderViewModel)
            }
        })
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (!binderViewModel.pingBinder()) {
            return
        }
        inflater.inflate(R.menu.applist_toolbar, menu)
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
            private val navController = findNavController()

            override fun onQueryTextSubmit(query: String): Boolean {
                viewModel.queryText = query
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (navController.currentDestination?.id == R.id.applist_fragment) {
                    viewModel.queryText = newText
                }
                return false
            }
        })

        when (ModulePreferences.sortBy) {
            ModulePreferences.SORT_BY_APP_NAME ->
                menu.findItem(R.id.menu_sort_by_app_name).isChecked = true
            ModulePreferences.SORT_BY_UPDATE_TIME ->
                menu.findItem(R.id.menu_sort_by_update_time).isChecked = true
        }
        menu.findItem(R.id.menu_rule_count).isChecked = ModulePreferences.ruleCount
        menu.findItem(R.id.menu_hide_system_app).isChecked = ModulePreferences.isHideSystemApp
        menu.findItem(R.id.menu_hide_no_storage_permission).isChecked =
            ModulePreferences.isHideNoStoragePermissionApp
        arrayOf(
            menu.findItem(R.id.menu_header_sort), menu.findItem(R.id.menu_header_hide)
        ).forEach {
            it.title = requireContext().buildStyledTitle(it.title)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_by_app_name -> {
                item.isChecked = true
                ModulePreferences.sortBy = ModulePreferences.SORT_BY_APP_NAME
            }
            R.id.menu_sort_by_update_time -> {
                item.isChecked = true
                ModulePreferences.sortBy = ModulePreferences.SORT_BY_UPDATE_TIME
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
