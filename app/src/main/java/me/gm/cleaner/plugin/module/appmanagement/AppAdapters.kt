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

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.transition.platform.Hold
import com.google.gson.Gson
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.dao.usagerecord.MediaProviderDeleteRecord
import me.gm.cleaner.plugin.dao.usagerecord.MediaProviderInsertRecord
import me.gm.cleaner.plugin.dao.usagerecord.MediaProviderQueryRecord
import me.gm.cleaner.plugin.databinding.AppHeaderBinding
import me.gm.cleaner.plugin.databinding.TemplatesHeaderBinding
import me.gm.cleaner.plugin.databinding.TemplatesItemBinding
import me.gm.cleaner.plugin.ktx.DividerViewHolder
import me.gm.cleaner.plugin.ktx.mediumAnimTime
import me.gm.cleaner.plugin.model.Template
import me.gm.cleaner.plugin.model.Templates
import me.gm.cleaner.plugin.module.settings.CreateTemplateFragment

class AppHeaderAdapter(private val fragment: AppFragment) :
    RecyclerView.Adapter<AppHeaderAdapter.ViewHolder>() {
    private val args: AppFragmentArgs by fragment.navArgs()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(AppHeaderBinding.inflate(LayoutInflater.from(parent.context)))

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        Glide.with(fragment)
            .load(args.pi)
            .into(binding.icon)
        binding.labelVersion.text = "${args.label} ${args.pi.versionName}"
        binding.packageName.text = args.pi.packageName
        binding.sdk.text = "SDK ${args.pi.applicationInfo.targetSdkVersion}"
        binding.sdk.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", args.pi.packageName, null)
            }
            fragment.startActivity(intent)
        }
        val usageTimes = arrayOf(
            MediaProviderQueryRecord::class.simpleName to R.string.query_times,
            MediaProviderInsertRecord::class.simpleName to R.string.insert_times,
            MediaProviderDeleteRecord::class.simpleName to R.string.delete_times,
        ).mapNotNull {
            val packageUsageTimes = fragment.binderViewModel.packageUsageTimes(
                it.first!!, listOf(args.pi.packageName)
            )
            if (packageUsageTimes == 0) {
                return@mapNotNull null
            }
            fragment.getString(it.second, packageUsageTimes)
        }
        if (usageTimes.isNotEmpty()) {
            binding.usageTimes.isVisible = true
            binding.usageTimes.text =
                usageTimes.joinToString(fragment.getString(R.string.delimiter))
        }
    }

    override fun getItemCount() = 1

    class ViewHolder(val binding: AppHeaderBinding) : DividerViewHolder(binding.root) {
        init {
            isDividerAllowedBelow = true
        }
    }
}

class TemplatesAdapter(private val fragment: AppFragment) :
    ListAdapter<Template, TemplatesAdapter.ViewHolder>(CALLBACK) {
    private val navController by lazy { fragment.findNavController() }
    private val activity = fragment.requireActivity() as AppCompatActivity
    private lateinit var selectedHolder: ViewHolder

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(TemplatesItemBinding.inflate(LayoutInflater.from(parent.context)))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val item = getItem(position)
        binding.title.text = item.templateName
        binding.summary.text =
            fragment.getString(R.string.applied_app_count, item.applyToApp?.size ?: 0)
        binding.root.transitionName = item.templateName
        binding.root.setOnClickListener {
            if (navController.currentDestination?.id != R.id.app_fragment) {
                return@setOnClickListener
            }
            fragment.lastTemplateName = item.templateName
            fragment.exitTransition = Hold().apply {
                duration = fragment.requireContext().mediumAnimTime
            }
            fragment.setExitSharedElementCallback(null)

            val direction =
                AppFragmentDirections.actionAppToCreateTemplate(item.templateName)
            val extras = FragmentNavigatorExtras(it to it.transitionName)
            navController.navigate(direction, extras)
        }
        binding.root.setOnLongClickListener {
            selectedHolder = holder
            false
        }
        binding.root.setOnCreateContextMenuListener { menu, _, _ ->
            activity.menuInflater.inflate(R.menu.item_delete, menu)
            menu.setHeaderTitle(item.templateName)
            menu.forEach { it.setOnMenuItemClickListener(::onContextItemSelected) }
        }

        if (fragment.lastTemplateName == item.templateName) {
            fragment.startPostponedEnterTransition()
        }
    }

    private fun onContextItemSelected(item: MenuItem): Boolean {
        if (!::selectedHolder.isInitialized) return false
        if (item.itemId == R.id.menu_delete) {
            val position = selectedHolder.bindingAdapterPosition
            val templateToRemove = getItem(position).templateName
            val modified = Templates(fragment.binderViewModel.readSp(R.xml.template_preferences))
                .filterNot { it.templateName == templateToRemove }
            fragment.binderViewModel.writeSp(R.xml.template_preferences, Gson().toJson(modified))
            return true
        }
        return false
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

class TemplatesFooterAdapter(private val fragment: AppFragment) :
    RecyclerView.Adapter<TemplatesFooterAdapter.ViewHolder>() {
    private val args: AppFragmentArgs by fragment.navArgs()
    private val navController by lazy { fragment.findNavController() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(TemplatesHeaderBinding.inflate(LayoutInflater.from(parent.context)))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        binding.root.transitionName = args.label
        binding.root.setOnClickListener {
            if (navController.currentDestination?.id != R.id.app_fragment) {
                return@setOnClickListener
            }
            fragment.lastTemplateName = args.label
            fragment.exitTransition = Hold().apply {
                duration = fragment.requireContext().mediumAnimTime
            }
            fragment.setExitSharedElementCallback(null)

            val direction =
                AppFragmentDirections.actionAppToCreateTemplate(args.label, args.pi.packageName)
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
            isDividerAllowedAbove = true
        }
    }
}
