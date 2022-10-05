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
import androidx.lifecycle.asLiveData
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.dao.RootPreferences
import me.gm.cleaner.plugin.databinding.MediaStoreFragmentBinding
import me.gm.cleaner.plugin.ktx.LayoutCompleteAwareGridLayoutManager
import me.gm.cleaner.plugin.ktx.PermissionUtils
import me.gm.cleaner.plugin.ktx.buildStyledTitle
import me.gm.cleaner.plugin.ktx.fitsSystemWindowInsetBottom
import me.gm.cleaner.plugin.mediastore.MediaStoreFragment
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import me.zhanghai.android.fastscroll.PopupStyle
import me.zhanghai.android.fastscroll.PreciseRecyclerViewHelper

open class FilesFragment : MediaStoreFragment() {
    override val viewModel: FilesViewModel by viewModels()

    override fun onCreateAdapter() = FilesAdapter(this)

    override fun onBindView(binding: MediaStoreFragmentBinding) {
        list.layoutManager = LayoutCompleteAwareGridLayoutManager(requireContext(), 1)
        val fastScroller = FastScrollerBuilder(list)
            .useMd2Style()
            .setPopupStyle(PopupStyle.MD3)
            .setViewHelper(PreciseRecyclerViewHelper(list))
            .build()
        list.fitsSystemWindowInsetBottom(fastScroller)

        viewModel.requeryFlow.asLiveData().observe(viewLifecycleOwner) {
            if (!isInActionMode()) {
                PermissionUtils.requestPermissions(
                    childFragmentManager, MediaPermissionRequesterFragment()
                )
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (selectionTracker.hasSelection()) {
            return
        }
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

        when (RootPreferences.sortMediaBy) {
            RootPreferences.SORT_BY_PATH ->
                menu.findItem(R.id.menu_sort_by_path).isChecked = true
            RootPreferences.SORT_BY_DATE_TAKEN ->
                menu.findItem(R.id.menu_sort_by_date_taken).isChecked = true
            RootPreferences.SORT_BY_SIZE ->
                menu.findItem(R.id.menu_sort_by_size).isChecked = true
        }
        arrayOf(menu.findItem(R.id.menu_header_sort)).forEach {
            it.title = requireContext().buildStyledTitle(it.title!!)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_by_path -> {
                item.isChecked = true
                RootPreferences.sortMediaBy = RootPreferences.SORT_BY_PATH
            }
            R.id.menu_sort_by_date_taken -> {
                item.isChecked = true
                RootPreferences.sortMediaBy = RootPreferences.SORT_BY_DATE_TAKEN
            }
            R.id.menu_sort_by_size -> {
                item.isChecked = true
                RootPreferences.sortMediaBy = RootPreferences.SORT_BY_SIZE
            }
            R.id.menu_validation -> viewModel.rescanFiles()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
