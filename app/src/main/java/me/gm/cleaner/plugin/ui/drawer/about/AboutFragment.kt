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

package me.gm.cleaner.plugin.ui.drawer.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.ImagesPlugin
import kotlinx.coroutines.launch
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.databinding.AboutFragmentBinding
import me.gm.cleaner.plugin.ktx.fitsSystemWindowInsets
import java.lang.ref.WeakReference

@AndroidEntryPoint
class AboutFragment : BaseFragment() {
    private val viewModel: AboutViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = AboutFragmentBinding.inflate(layoutInflater)

        liftOnScrollTargetView = WeakReference(binding.listContainer)
        binding.listContainer.fitsSystemWindowInsets()

        lifecycleScope.launch {
            val rawReadme = viewModel.getRawReadmeAsync().await()
            binding.progress.hide()
            val md = rawReadme.getOrElse {
                binding.content.text = it.stackTraceToString()
                return@launch
            }
            val markwon = Markwon.builder(requireContext())
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(HtmlPlugin.create())
                .usePlugin(ImagesPlugin.create())
                .build()
            markwon.setMarkdown(binding.content, md)
        }
        return binding.root
    }
}
