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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.platform.Hold
import com.google.gson.Gson
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.databinding.TemplatesHeaderBinding
import me.gm.cleaner.plugin.databinding.TemplatesItemBinding
import me.gm.cleaner.plugin.ktx.DividerViewHolder
import me.gm.cleaner.plugin.ktx.mediumAnimTime
import me.gm.cleaner.plugin.model.Template
import me.gm.cleaner.plugin.model.Templates

class TemplatesHeaderAdapter(private val fragment: TemplatesFragment) :
    RecyclerView.Adapter<TemplatesHeaderAdapter.ViewHolder>() {
    private val navController by lazy { fragment.findNavController() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(TemplatesHeaderBinding.inflate(LayoutInflater.from(parent.context)))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        binding.root.transitionName = CreateTemplateFragment.NULL_TEMPLATE_NAME
        binding.root.setOnClickListener {
            if (navController.currentDestination?.id != R.id.templates_fragment) {
                return@setOnClickListener
            }
            fragment.lastTemplateName = CreateTemplateFragment.NULL_TEMPLATE_NAME
            fragment.exitTransition = Hold().apply {
                duration = fragment.requireContext().mediumAnimTime
            }
            fragment.setExitSharedElementCallback(null)

            val direction = TemplatesFragmentDirections.actionTemplatesToCreateTemplate()
            val extras = FragmentNavigatorExtras(it to it.transitionName)
            navController.navigate(direction, extras)
        }

        if (fragment.lastTemplateName == CreateTemplateFragment.NULL_TEMPLATE_NAME) {
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

class TemplatesAdapter(private val fragment: TemplatesFragment) :
    ListAdapter<Template, TemplatesAdapter.ViewHolder>(CALLBACK) {
    private val navController by lazy { fragment.findNavController() }
    private val activity = fragment.requireActivity() as AppCompatActivity

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(TemplatesItemBinding.inflate(LayoutInflater.from(parent.context)))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val item = getItem(position)
        val templateName = item.templateName
        binding.title.text = templateName
        binding.summary.text = fragment.getString(
            R.string.applied_app_count, item.applyToApp?.size ?: 0
        )
        binding.root.transitionName = templateName
        binding.root.setOnClickListener {
            if (navController.currentDestination?.id != R.id.templates_fragment) {
                return@setOnClickListener
            }
            fragment.lastTemplateName = templateName
            fragment.exitTransition = Hold().apply {
                duration = fragment.requireContext().mediumAnimTime
            }
            fragment.setExitSharedElementCallback(null)

            val direction =
                TemplatesFragmentDirections.actionTemplatesToCreateTemplate(templateName)
            val extras = FragmentNavigatorExtras(it to it.transitionName)
            navController.navigate(direction, extras)
        }
        binding.root.setOnCreateContextMenuListener { menu, _, _ ->
            activity.menuInflater.inflate(R.menu.item_delete, menu)
            menu.setHeaderTitle(templateName)
            menu.forEach {
                it.setOnMenuItemClickListener { item ->
                    if (item.itemId == R.id.menu_delete) {
                        val modified =
                            Templates(fragment.binderViewModel.readSp(R.xml.template_preferences))
                                .values.filterNot { it.templateName == templateName }
                        fragment.binderViewModel.writeSp(
                            R.xml.template_preferences, Gson().toJson(modified)
                        )
                        true
                    } else {
                        false
                    }
                }
            }
        }

        if (fragment.lastTemplateName == templateName) {
            fragment.startPostponedEnterTransition()
        }
    }

    class ViewHolder(val binding: TemplatesItemBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private val CALLBACK: DiffUtil.ItemCallback<Template> =
            object : DiffUtil.ItemCallback<Template>() {
                override fun areItemsTheSame(oldItem: Template, newItem: Template) =
                    oldItem.templateName == newItem.templateName

                override fun areContentsTheSame(oldItem: Template, newItem: Template) =
                    oldItem == newItem
            }
    }
}
