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

import android.icu.text.DateFormat
import android.icu.util.TimeZone
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.dao.RootPreferences
import me.gm.cleaner.plugin.databinding.UsagerecordFragmentBinding
import me.gm.cleaner.plugin.ktx.*
import me.gm.cleaner.plugin.module.ModuleFragment
import me.gm.cleaner.plugin.widget.FixQueryChangeSearchView
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import rikka.recyclerview.fixEdgeEffect
import java.util.*

class UsageRecordFragment : ModuleFragment() {
    private val viewModel: UsageRecordViewModel by viewModels()
    private val navController by lazy { findNavController() }

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
        val binding = UsagerecordFragmentBinding.inflate(layoutInflater)

        val adapter = UsageRecordAdapter(this).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
        val list = binding.list
        list.adapter = adapter
        list.layoutManager = LayoutCompleteAwareGridLayoutManager(requireContext(), 1)
        list.setHasFixedSize(true)
        val fastScroller = FastScrollerBuilder(list)
            .useMd2Style()
            .build()
        list.fixEdgeEffect(false)
        list.overScrollIfContentScrollsPersistent()
        list.addLiftOnScrollListener { appBarLayout.isLifted = it }
        list.fitsSystemWindowInsetBottom(fastScroller)

        viewModel.recordsFlow.asLiveData().observe(viewLifecycleOwner) { records ->
            when (records) {
                is SourceState.Loading -> binding.progress.show()
                is SourceState.Done -> adapter.submitList(records.list) {
                    binding.progress.hide()
                    supportActionBar?.subtitle = DateFormat.getInstanceForSkeleton(
                        DateFormat.YEAR_ABBR_MONTH_DAY, Locale.getDefault()
                    ).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(Date(viewModel.calendar.timeInMillis))
                }
            }
        }
        viewModel.reloadRecordsLiveData.observe(viewLifecycleOwner) { reloadNeeded ->
            if (reloadNeeded) {
                viewModel.reloadRecords(binderViewModel)
            }
        }

        if (savedInstanceState == null) {
            viewModel.reloadRecords(binderViewModel)
            viewModel.registerMediaChangeObserver(binderViewModel)
        }
        navController.addOnExitListener { _, _, _ ->
            supportActionBar?.subtitle = null
        }

        RootPreferences.addOnPreferenceChangeListener(object :
            RootPreferences.PreferencesChangeListener {
            override val lifecycle = this@UsageRecordFragment.lifecycle
            override fun onPreferencesChanged() {
                viewModel.reloadRecords(binderViewModel)
            }
        })
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (!binderViewModel.pingBinder()) {
            return
        }
        inflater.inflate(R.menu.usagerecord_toolbar, menu)
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

        menu.findItem(R.id.menu_hide_query).isChecked = RootPreferences.isHideQuery
        menu.findItem(R.id.menu_hide_insert).isChecked = RootPreferences.isHideInsert
        menu.findItem(R.id.menu_hide_delete).isChecked = RootPreferences.isHideDelete
        arrayOf(menu.findItem(R.id.menu_header_hide)).forEach {
            it.title = requireContext().buildStyledTitle(it.title!!)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_pick_date -> {
                val calendarConstraints = CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointBackward.now())
                    .build()
                val datePicker = MaterialDatePicker.Builder.datePicker()
                    .setCalendarConstraints(calendarConstraints)
                    .setSelection(viewModel.calendar.timeInMillis)
                    .build()
                datePicker.addOnPositiveButtonClickListener { selection ->
                    viewModel.loadRecords(binderViewModel, selection)
                }
                datePicker.show(childFragmentManager, null)
            }
            R.id.menu_hide_query -> {
                val isHideQuery = !item.isChecked
                item.isChecked = isHideQuery
                RootPreferences.isHideQuery = isHideQuery
            }
            R.id.menu_hide_insert -> {
                val isHideInsert = !item.isChecked
                item.isChecked = isHideInsert
                RootPreferences.isHideInsert = isHideInsert
            }
            R.id.menu_hide_delete -> {
                val isHideDelete = !item.isChecked
                item.isChecked = isHideDelete
                RootPreferences.isHideDelete = isHideDelete
            }
            R.id.menu_clear -> {
                binderViewModel.clearAllTables()
                viewModel.reloadRecords(binderViewModel)
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
