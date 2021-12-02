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

package me.gm.cleaner.plugin.module.settings

import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.platform.Hold
import me.gm.cleaner.plugin.BuildConfig
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.databinding.TemplatesHeaderBinding
import me.gm.cleaner.plugin.databinding.TemplatesItemBinding
import me.gm.cleaner.plugin.ktx.DividerViewHolder
import me.gm.cleaner.plugin.ktx.mediumAnimTime
import me.gm.cleaner.plugin.module.BinderViewModel

class TemplatesHeaderAdapter(private val fragment: TemplatesFragment) :
    RecyclerView.Adapter<TemplatesHeaderAdapter.ViewHolder>() {
    private val navController by lazy { fragment.findNavController() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(TemplatesHeaderBinding.inflate(LayoutInflater.from(parent.context)))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        binding.root.transitionName = "null"
        binding.root.setOnClickListener {
            if (navController.currentDestination?.id != R.id.templates_fragment) {
                return@setOnClickListener
            }
            fragment.enterRuleLabel = "null"
            fragment.exitTransition = Hold().apply {
                duration = fragment.requireContext().mediumAnimTime
            }

            val direction = TemplatesFragmentDirections.actionTemplatesToCreateTemplate()
            val extras = FragmentNavigatorExtras(it to it.transitionName)
            navController.navigate(direction, extras)
        }

        if (fragment.enterRuleLabel == "null") {
            fragment.startPostponedEnterTransition()
        }
    }

    override fun getItemCount() = 1

    class ViewHolder(val binding: TemplatesHeaderBinding) : DividerViewHolder(binding.root) {
        init {
            isDividerAllowedBelow = true
        }
    }
}

class TemplatesAdapter(private val fragment: TemplatesFragment, binderViewModel: BinderViewModel) :
    ListAdapter<Pair<String, Int>, TemplatesAdapter.ViewHolder>(CALLBACK) {
    private val navController by lazy { fragment.findNavController() }
    private val activity = fragment.requireActivity() as AppCompatActivity
    private lateinit var selectedHolder: ViewHolder

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(TemplatesItemBinding.inflate(LayoutInflater.from(parent.context)))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val item = getItem(position)
        binding.title.text = item.first
        binding.summary.text = item.second.toString()
        binding.root.transitionName = item.first
        binding.root.setOnClickListener {
            if (navController.currentDestination?.id != R.id.templates_fragment) {
                return@setOnClickListener
            }
            fragment.enterRuleLabel = item.first
            fragment.exitTransition = Hold().apply {
                duration = fragment.requireContext().mediumAnimTime
            }

            val direction = TemplatesFragmentDirections.actionTemplatesToCreateTemplate(item.first)
            val extras = FragmentNavigatorExtras(it to it.transitionName)
            navController.navigate(direction, extras)
        }
        binding.root.setOnLongClickListener {
            selectedHolder = holder
            false
        }
        binding.root.setOnCreateContextMenuListener { menu, _, _ ->
            activity.menuInflater.inflate(R.menu.item_delete_all_rules, menu)
            menu.setHeaderTitle(item.first)
            menu.forEach { it.setOnMenuItemClickListener(::onContextItemSelected) }
        }

        if (fragment.enterRuleLabel == item.first) {
            fragment.startPostponedEnterTransition()
        }
    }

    private fun onContextItemSelected(item: MenuItem): Boolean {
        if (!::selectedHolder.isInitialized) return false
        val position = selectedHolder.bindingAdapterPosition
        val label = getItem(position)?.first
        if (item.itemId == R.id.menu_delete_all_rules) {
            Log.e(BuildConfig.APPLICATION_ID,label!!)
            val modified =
                fragment.binderViewModel.readSpAsJson(R.xml.template_preferences).remove(label)
            fragment.binderViewModel.writeSp(R.xml.template_preferences, modified.toString())
            return true
        }
        return false
    }

    class ViewHolder(val binding: TemplatesItemBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private val CALLBACK: DiffUtil.ItemCallback<Pair<String, Int>> =
            object : DiffUtil.ItemCallback<Pair<String, Int>>() {
                override fun areItemsTheSame(
                    oldItem: Pair<String, Int>, newItem: Pair<String, Int>
                ) = oldItem.first == newItem.first

                override fun areContentsTheSame(
                    oldItem: Pair<String, Int>, newItem: Pair<String, Int>
                ) = oldItem == newItem
            }
    }
}
