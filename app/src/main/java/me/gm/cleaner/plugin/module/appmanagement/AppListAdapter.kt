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

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.platform.Hold
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.databinding.ApplistItemBinding
import me.gm.cleaner.plugin.di.GlideApp
import me.gm.cleaner.plugin.drawer.DrawerActivity
import me.gm.cleaner.plugin.ktx.buildStyledTitle
import me.gm.cleaner.plugin.ktx.mediumAnimTime
import me.gm.cleaner.plugin.module.PreferencesPackageInfo

class AppListAdapter(private val fragment: AppListFragment) :
    ListAdapter<PreferencesPackageInfo, AppListAdapter.ViewHolder>(CALLBACK) {
    private val viewModel: AppListViewModel by fragment.viewModels()
    private val navController by lazy { fragment.findNavController() }
    private val activity = fragment.requireActivity() as DrawerActivity
    private lateinit var selectedHolder: ViewHolder

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ApplistItemBinding.inflate(LayoutInflater.from(parent.context)))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val pi = getItem(position)
        GlideApp.with(fragment)
            .load(pi)
            .into(binding.icon)
        binding.title.text = pi.label
        binding.summary.text = if (pi.ruleCount > 0) {
            activity.buildStyledTitle(pi.ruleCount.toString())
        } else {
            pi.packageName
        }
        binding.root.transitionName = pi.packageName
        binding.root.setOnClickListener {
            if (navController.currentDestination?.id != R.id.applist_fragment) {
                return@setOnClickListener
            }
            viewModel.enterPosition = holder.bindingAdapterPosition
            fragment.exitTransition = Hold().apply {
                duration = fragment.requireContext().mediumAnimTime
            }

            val direction = AppListFragmentDirections.actionApplistToApp(pi, pi.label)
            val extras = FragmentNavigatorExtras(it to it.transitionName)
            navController.navigate(direction, extras)
        }
        binding.root.setOnLongClickListener {
            selectedHolder = holder
            false
        }
        binding.root.setOnCreateContextMenuListener { menu, _, _ ->
            activity.menuInflater.inflate(R.menu.item_delete_all_rules, menu)
            menu.setHeaderTitle(pi.label)
            if (pi.ruleCount == 0) {
                menu.removeItem(R.id.menu_delete_all_rules)
            } else {
                menu.forEach { it.setOnMenuItemClickListener(::onContextItemSelected) }
            }
        }

        if (position == viewModel.enterPosition) {
            fragment.startPostponedEnterTransition()
        }
    }

    private fun onContextItemSelected(item: MenuItem): Boolean {
        if (!::selectedHolder.isInitialized) return false
        val position = selectedHolder.bindingAdapterPosition
        val pi = getItem(position)!!
        if (item.itemId == R.id.menu_delete_all_rules) {
//            ModulePreferences.removePackage(pi.packageName)
            return true
        }
        return false
    }

    class ViewHolder(val binding: ApplistItemBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private val CALLBACK: DiffUtil.ItemCallback<PreferencesPackageInfo> =
            object : DiffUtil.ItemCallback<PreferencesPackageInfo>() {
                override fun areItemsTheSame(
                    oldItem: PreferencesPackageInfo, newItem: PreferencesPackageInfo
                ) = oldItem.packageName == newItem.packageName

                override fun areContentsTheSame(
                    oldItem: PreferencesPackageInfo, newItem: PreferencesPackageInfo
                ) = oldItem == newItem
            }
    }
}
