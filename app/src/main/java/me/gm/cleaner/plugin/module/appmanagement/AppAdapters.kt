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
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.dao.usagerecord.MediaProviderDeleteRecord
import me.gm.cleaner.plugin.dao.usagerecord.MediaProviderInsertRecord
import me.gm.cleaner.plugin.dao.usagerecord.MediaProviderQueryRecord
import me.gm.cleaner.plugin.databinding.AppHeaderBinding
import me.gm.cleaner.plugin.di.GlideApp

class AppHeaderAdapter(private val fragment: AppFragment) :
    RecyclerView.Adapter<AppHeaderAdapter.ViewHolder>() {
    private val args: AppFragmentArgs by fragment.navArgs()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(AppHeaderBinding.inflate(LayoutInflater.from(parent.context)))

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        GlideApp.with(fragment)
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

    class ViewHolder(val binding: AppHeaderBinding) : RecyclerView.ViewHolder(binding.root)
}
