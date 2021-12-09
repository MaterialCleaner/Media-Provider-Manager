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
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.platform.Hold
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.databinding.ApplistItemBinding
import me.gm.cleaner.plugin.di.GlideApp
import me.gm.cleaner.plugin.ktx.buildStyledTitle
import me.gm.cleaner.plugin.ktx.mediumAnimTime
import me.gm.cleaner.plugin.module.PreferencesPackageInfo

class AppListAdapter(private val fragment: AppListFragment) :
    ListAdapter<PreferencesPackageInfo, AppListAdapter.ViewHolder>(CALLBACK) {
    private val navController by lazy { fragment.findNavController() }
    private val activity = fragment.requireActivity() as AppCompatActivity

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
            activity.buildStyledTitle(fragment.getString(R.string.enabled_rule_count, pi.ruleCount))
        } else {
            pi.packageName
        }
        binding.root.transitionName = pi.packageName
        binding.root.setOnClickListener {
            if (navController.currentDestination?.id != R.id.applist_fragment) {
                return@setOnClickListener
            }
            fragment.enterPackageName = pi.packageName
            fragment.exitTransition = Hold().apply {
                duration = fragment.requireContext().mediumAnimTime
            }

            val direction = AppListFragmentDirections.actionApplistToApp(pi, pi.label)
            val extras = FragmentNavigatorExtras(it to it.transitionName)
            navController.navigate(direction, extras)
        }

        if (fragment.enterPackageName == pi.packageName) {
            fragment.startPostponedEnterTransition()
        }
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
