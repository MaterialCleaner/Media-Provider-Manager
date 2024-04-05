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

package me.gm.cleaner.plugin.ui.module.usagerecord

import android.app.Application
import android.icu.text.DateFormat
import android.icu.util.TimeZone
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import me.gm.cleaner.plugin.IMediaChangeObserver
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.dao.RootPreferences
import me.gm.cleaner.plugin.databinding.UsagerecordFragmentBinding
import me.gm.cleaner.plugin.ktx.addOnExitListener
import me.gm.cleaner.plugin.ktx.buildSpannableString
import me.gm.cleaner.plugin.ktx.fitsSystemWindowInsets
import me.gm.cleaner.plugin.ktx.overScrollIfContentScrollsPersistent
import me.gm.cleaner.plugin.ui.module.ModuleFragment
import me.gm.cleaner.plugin.widget.FixQueryChangeSearchView
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import rikka.recyclerview.fixEdgeEffect
import java.lang.ref.WeakReference
import java.util.Date
import java.util.Locale

class UsageRecordFragment : ModuleFragment() {
    private val viewModel: UsageRecordViewModel by viewModels(
        factoryProducer = {
            UsageRecordViewModel.provideFactory(
                requireContext().applicationContext as Application,
                binderViewModel
            )
        }
    )
    private val mediaChangeObserver = object : IMediaChangeObserver.Stub() {
        override fun onChange() {
            viewModel.reload()
        }
    }

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

        viewModel.recordsFlow.asLiveData().observe(viewLifecycleOwner) { records ->
            when (records) {
                is UsageRecordState.Loading -> binding.progress.show()
                is UsageRecordState.Done -> adapter.submitList(records.list) {
                    binding.progress.hide()
                    supportActionBar?.subtitle = DateFormat.getInstanceForSkeleton(
                        DateFormat.YEAR_ABBR_MONTH_DAY, Locale.getDefault()
                    ).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(Date(viewModel.calendar.timeInMillis))
                }
            }
        }

        binderViewModel.registerMediaChangeObserver(mediaChangeObserver)
        findNavController().addOnExitListener { _, _, _ ->
            binderViewModel.unregisterMediaChangeObserver(mediaChangeObserver)
            supportActionBar?.subtitle = null
        }
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

        menu.findItem(R.id.menu_hide_query).isChecked = RootPreferences.isHideQuery.value
        menu.findItem(R.id.menu_hide_insert).isChecked = RootPreferences.isHideInsert.value
        menu.findItem(R.id.menu_hide_delete).isChecked = RootPreferences.isHideDelete.value
        arrayOf(menu.findItem(R.id.menu_header_hide)).forEach {
            it.title = requireContext().buildSpannableString(it.title!!)
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
                    viewModel.selectedTime = selection
                }
                datePicker.show(childFragmentManager, null)
            }

            R.id.menu_hide_query -> {
                val isHideQuery = !item.isChecked
                item.isChecked = isHideQuery
                RootPreferences.isHideQuery.value = isHideQuery
            }

            R.id.menu_hide_insert -> {
                val isHideInsert = !item.isChecked
                item.isChecked = isHideInsert
                RootPreferences.isHideInsert.value = isHideInsert
            }

            R.id.menu_hide_delete -> {
                val isHideDelete = !item.isChecked
                item.isChecked = isHideDelete
                RootPreferences.isHideDelete.value = isHideDelete
            }

            R.id.menu_clear -> {
                binderViewModel.clearAllTables()
                viewModel.reload()
            }

            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
