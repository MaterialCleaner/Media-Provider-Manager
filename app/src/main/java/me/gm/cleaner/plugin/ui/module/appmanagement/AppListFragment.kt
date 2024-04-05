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

package me.gm.cleaner.plugin.ui.module.appmanagement

import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.launch
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.dao.RootPreferences
import me.gm.cleaner.plugin.dao.RootPreferences.SORT_BY_APP_NAME
import me.gm.cleaner.plugin.dao.RootPreferences.SORT_BY_UPDATE_TIME
import me.gm.cleaner.plugin.databinding.ApplistFragmentBinding
import me.gm.cleaner.plugin.ktx.buildSpannableString
import me.gm.cleaner.plugin.ktx.fitsSystemWindowInsets
import me.gm.cleaner.plugin.ktx.overScrollIfContentScrollsPersistent
import me.gm.cleaner.plugin.ktx.submitListKeepPosition
import me.gm.cleaner.plugin.ui.module.ModuleFragment
import me.gm.cleaner.plugin.widget.FixQueryChangeSearchView
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import rikka.recyclerview.fixEdgeEffect
import java.lang.ref.WeakReference

class AppListFragment : ModuleFragment() {
    private val viewModel: AppListViewModel by viewModels(
        factoryProducer = {
            AppListViewModel.provideFactory(
                requireContext().applicationContext as Application,
                binderViewModel
            )
        }
    )
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
        liftOnScrollTargetView = WeakReference(list)
        list.adapter = adapter
        list.layoutManager = GridLayoutManager(requireContext(), 1)
        list.setHasFixedSize(true)
        val fastScroller = FastScrollerBuilder(list)
            .useMd2Style()
            .build()
        list.fixEdgeEffect(false)
        list.overScrollIfContentScrollsPersistent()
        list.fitsSystemWindowInsets(fastScroller)
        binding.listContainer.setOnRefreshListener {
            viewModel.load(null)
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
                        is AppListState.Loading -> binding.progress.progress = apps.progress
                        is AppListState.Done -> adapter.submitListKeepPosition(apps.list, list) {
                            binding.progress.hide()
                            binding.listContainer.isRefreshing = false
                        }
                    }
                }
            }
        }
        setFragmentResultListener(AppFragment::class.java.name) { _, bundle ->
            enterPackageName = bundle.getString(AppFragment.KEY_PACKAGENAME)
            postponeEnterTransition()
        }

        binderViewModel.remoteSpCacheLiveData.observe(viewLifecycleOwner) {
            viewModel.update()
        }
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
                viewModel.queryText = ""
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                viewModel.isSearching = false
                return true
            }
        })
        val searchView = searchItem.actionView as FixQueryChangeSearchView
        searchView.setQuery(viewModel.queryText, false)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                viewModel.queryText = query
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (!searchView.shouldIgnoreQueryChange) {
                    viewModel.queryText = newText
                }
                return false
            }
        })

        when (RootPreferences.sortByFlowable.value) {
            SORT_BY_APP_NAME ->
                menu.findItem(R.id.menu_sort_by_app_name).isChecked = true

            SORT_BY_UPDATE_TIME ->
                menu.findItem(R.id.menu_sort_by_update_time).isChecked = true
        }
        menu.findItem(R.id.menu_rule_count).isChecked = RootPreferences.ruleCountFlowable.value
        menu.findItem(R.id.menu_hide_system_app).isChecked =
            RootPreferences.isHideSystemAppFlowable.value
        arrayOf(
            menu.findItem(R.id.menu_header_sort), menu.findItem(R.id.menu_header_hide)
        ).forEach {
            it.title = requireContext().buildSpannableString(it.title!!)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_by_app_name -> {
                item.isChecked = true
                RootPreferences.sortByFlowable.value = SORT_BY_APP_NAME
            }

            R.id.menu_sort_by_update_time -> {
                item.isChecked = true
                RootPreferences.sortByFlowable.value = SORT_BY_UPDATE_TIME
            }

            R.id.menu_rule_count -> {
                val ruleCount = !item.isChecked
                item.isChecked = ruleCount
                RootPreferences.ruleCountFlowable.value = ruleCount
            }

            R.id.menu_hide_system_app -> {
                val isHideSystemApp = !item.isChecked
                item.isChecked = isHideSystemApp
                RootPreferences.isHideSystemAppFlowable.value = isHideSystemApp
            }

            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
