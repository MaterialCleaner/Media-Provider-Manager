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

package me.gm.cleaner.plugin.test

import android.Manifest
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.databinding.HomeButtonBinding

// https://developer.android.com/training/data-storage/shared/media?hl=zh-cn

class TestAdapter(fragment: TestFragment) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val activity: TestActivity = fragment.requireActivity() as TestActivity
    private val requestPermission =
        fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                Navigation
                    .findNavController(activity, android.R.id.home)
                    .navigate(R.id.action_test_to_query)
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        ButtonHolder(HomeButtonBinding.inflate(LayoutInflater.from(parent.context)))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val binding = (holder as ButtonHolder).binding
        // TODO: add explain abstract at position 0
        when (position) {
            0 -> {
                binding.icon.setImageResource(R.drawable.ic_outline_search_24)
                binding.title.setText(R.string.query)
                binding.background.setOnClickListener {
                    if (haveStoragePermission()) {
                        Navigation
                            .findNavController(activity, R.id.home)
                            .navigate(R.id.action_test_to_query)
                    } else {
                        requestPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
            }
            1 -> {
                binding.icon.setImageResource(R.drawable.ic_outline_save_alt_24)
                binding.title.setText(R.string.insert)
                binding.background.setOnClickListener {
                    Navigation
                        .findNavController(activity, android.R.id.home)
                        .navigate(R.id.action_test_to_insert)
                }
            }
        }
    }

    private fun haveStoragePermission() = PackageManager.PERMISSION_GRANTED ==
            ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)

    override fun getItemCount(): Int = 2

    class ButtonHolder(val binding: HomeButtonBinding) : RecyclerView.ViewHolder(binding.root)
}
