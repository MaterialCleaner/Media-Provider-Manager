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

package me.gm.cleaner.plugin.mediastore.files

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.dao.ModulePreferences
import me.gm.cleaner.plugin.databinding.MediaStoreFragmentBinding
import me.gm.cleaner.plugin.ktx.LayoutCompleteAwareGridLayoutManager
import me.gm.cleaner.plugin.ktx.buildStyledTitle
import me.gm.cleaner.plugin.ktx.isItemCompletelyVisible
import me.gm.cleaner.plugin.mediastore.MediaStoreAdapter
import me.gm.cleaner.plugin.mediastore.MediaStoreFragment
import me.gm.cleaner.plugin.mediastore.MediaStoreModel

class FilesFragment : MediaStoreFragment() {
    override val viewModel: FilesViewModel by viewModels()

    override fun onCreateAdapter(): MediaStoreAdapter<MediaStoreModel, *> =
        FilesAdapter(this) as MediaStoreAdapter<MediaStoreModel, *>

    override fun onBindView(binding: MediaStoreFragmentBinding) {
        list.layoutManager = LayoutCompleteAwareGridLayoutManager(requireContext(), 1)
            .setOnLayoutCompletedListener {
                appBarLayout.isLifted =
                    list.adapter?.itemCount != 0 && !list.isItemCompletelyVisible(0)
            }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.requeryFlow.collect {
                    if (!isInActionMode()) {
                        dispatchRequestPermissions(requiredPermissions, null)
                    }
                }
            }
        }
        ModulePreferences.addOnPreferenceChangeListener(object :
            ModulePreferences.PreferencesChangeListener {
            override val lifecycle = getLifecycle()
            override fun onPreferencesChanged() {
                dispatchRequestPermissions(requiredPermissions, null)
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.files_toolbar, menu)
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

        when (ModulePreferences.sortMediaBy) {
            ModulePreferences.SORT_BY_PATH ->
                menu.findItem(R.id.menu_sort_by_file_path).isChecked = true
            ModulePreferences.SORT_BY_DATE_TAKEN ->
                menu.findItem(R.id.menu_sort_by_modify_time).isChecked = true
            ModulePreferences.SORT_BY_SIZE ->
                menu.findItem(R.id.menu_sort_by_file_size).isChecked = true
        }
        arrayOf(menu.findItem(R.id.menu_header_sort)).forEach {
            it.title = requireContext().buildStyledTitle(it.title)
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_by_file_path -> {
                item.isChecked = true
                ModulePreferences.sortMediaBy = ModulePreferences.SORT_BY_PATH
            }
            R.id.menu_sort_by_modify_time -> {
                item.isChecked = true
                ModulePreferences.sortMediaBy = ModulePreferences.SORT_BY_DATE_TAKEN
            }
            R.id.menu_sort_by_file_size -> {
                item.isChecked = true
                ModulePreferences.sortMediaBy = ModulePreferences.SORT_BY_SIZE
            }
            R.id.menu_validation -> viewModel.rescanFiles()
            else -> super.onOptionsItemSelected(item)
        }
        return true
    }
}
