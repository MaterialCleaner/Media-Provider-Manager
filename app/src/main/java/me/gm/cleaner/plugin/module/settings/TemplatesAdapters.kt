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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.databinding.TemplatesHeaderBinding

class TemplatesHeaderAdapter(private val fragment: TemplatesFragment) :
    RecyclerView.Adapter<TemplatesHeaderAdapter.ViewHolder>() {
    private val navController by lazy { fragment.findNavController() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(TemplatesHeaderBinding.inflate(LayoutInflater.from(parent.context)))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        binding.root.setOnClickListener {
            if (navController.currentDestination?.id != R.id.templates_fragment) {
                return@setOnClickListener
            }
//            fragment.enterPackageName = pi.packageName
//            fragment.exitTransition = Hold().apply {
//                duration = fragment.requireContext().mediumAnimTime
//            }

            val direction = TemplatesFragmentDirections.actionTemplatesToCreateTemplate()
//            val extras = FragmentNavigatorExtras(it to it.transitionName)
            navController.navigate(direction)
        }
    }

    override fun getItemCount() = 1

    class ViewHolder(val binding: TemplatesHeaderBinding) : RecyclerView.ViewHolder(binding.root)
}
