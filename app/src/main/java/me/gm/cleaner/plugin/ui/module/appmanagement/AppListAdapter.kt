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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.transition.platform.Hold
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.databinding.ApplistItemBinding
import me.gm.cleaner.plugin.ktx.buildStyledTitle
import me.gm.cleaner.plugin.ktx.mediumAnimTime

class AppListAdapter(private val fragment: AppListFragment) :
    ListAdapter<AppListModel, AppListAdapter.ViewHolder>(CALLBACK) {
    private val activity = fragment.requireActivity() as AppCompatActivity

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ApplistItemBinding.inflate(LayoutInflater.from(parent.context)))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val model = getItem(position)
        Glide.with(fragment)
            .load(model.packageInfo)
            .into(binding.icon)
        binding.title.text = model.label
        binding.summary.text = if (model.ruleCount > 0) {
            activity.buildStyledTitle(
                fragment.getString(R.string.enabled_rule_count, model.ruleCount)
            )
        } else {
            model.packageInfo.packageName
        }
        binding.root.transitionName = model.packageInfo.packageName
        binding.root.setOnClickListener {
            val navController = fragment.findNavController()
            if (navController.currentDestination?.id != R.id.applist_fragment) {
                return@setOnClickListener
            }
            fragment.enterPackageName = model.packageInfo.packageName
            fragment.exitTransition = Hold().apply {
                duration = fragment.requireContext().mediumAnimTime
            }

            val direction = AppListFragmentDirections.actionApplistToApp(
                packageInfo = model.packageInfo,
                label = model.label,
            )
            val extras = FragmentNavigatorExtras(it to it.transitionName)
            navController.navigate(direction, extras)
        }

        if (fragment.enterPackageName == model.packageInfo.packageName) {
            fragment.startPostponedEnterTransition()
        }
    }

    class ViewHolder(val binding: ApplistItemBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private val CALLBACK = object : DiffUtil.ItemCallback<AppListModel>() {
            override fun areItemsTheSame(oldItem: AppListModel, newItem: AppListModel): Boolean =
                oldItem.packageInfo.packageName == newItem.packageInfo.packageName

            override fun areContentsTheSame(oldItem: AppListModel, newItem: AppListModel): Boolean =
                oldItem == newItem
        }
    }
}
