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

package me.gm.cleaner.plugin.module.usagerecord

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import me.gm.cleaner.plugin.BuildConfig
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.dao.ModulePreferences
import me.gm.cleaner.plugin.databinding.ComingSoonFragmentBinding
import me.gm.cleaner.plugin.ktx.buildStyledTitle
import me.gm.cleaner.plugin.module.BinderViewModel

class UsageRecordFragment : BaseFragment() {
    private val binderViewModel: BinderViewModel by activityViewModels()
    private val viewModel: UsageRecordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = ComingSoonFragmentBinding.inflate(layoutInflater)

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recordsFlow.collect { records ->
                    Log.e(BuildConfig.APPLICATION_ID, records.toString())
                }
            }
        }
        viewModel.loadRecords(
            binderViewModel, requireContext().packageManager, System.currentTimeMillis()
        )

        ModulePreferences.setOnPreferenceChangeListener(object :
            ModulePreferences.PreferencesChangeListener {
            override val lifecycle = getLifecycle()
            override fun onPreferencesChanged(isNotifyService: Boolean) {
                viewModel.reloadRecords(binderViewModel, requireContext().packageManager)
            }
        })
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.usagerecord_toolbar, menu)
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

        menu.findItem(R.id.menu_hide_query).isChecked = ModulePreferences.isHideQuery
        menu.findItem(R.id.menu_hide_insert).isChecked = ModulePreferences.isHideInsert
        menu.findItem(R.id.menu_hide_delete).isChecked = ModulePreferences.isHideDelete
        arrayOf(menu.findItem(R.id.menu_header_hide)).forEach {
            it.title = requireContext().buildStyledTitle(it.title)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_pick_date -> MaterialDatePicker.Builder
                .datePicker()
                .build()
                .apply {
                    addOnPositiveButtonClickListener { selection ->
                        viewModel.loadRecords(
                            binderViewModel, requireContext().packageManager, selection
                        )
                    }
                }
                .show(childFragmentManager, null)
            R.id.menu_hide_query -> {
                val isHideQuery = !item.isChecked
                item.isChecked = isHideQuery
                ModulePreferences.isHideQuery = isHideQuery
            }
            R.id.menu_hide_insert -> {
                val isHideInsert = !item.isChecked
                item.isChecked = isHideInsert
                ModulePreferences.isHideInsert = isHideInsert
            }
            R.id.menu_hide_delete -> {
                val isHideDelete = !item.isChecked
                item.isChecked = isHideDelete
                ModulePreferences.isHideDelete = isHideDelete
            }
            R.id.menu_clear -> binderViewModel.clearAllTables()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
