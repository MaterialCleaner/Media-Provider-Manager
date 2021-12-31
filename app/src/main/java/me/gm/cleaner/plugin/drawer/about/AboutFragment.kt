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

package me.gm.cleaner.plugin.drawer.about

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.target.Target
import dagger.hilt.android.AndroidEntryPoint
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.glide.GlideImagesPlugin
import kotlinx.coroutines.launch
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.databinding.AboutFragmentBinding

@AndroidEntryPoint
class AboutFragment : BaseFragment() {
    private val viewModel: AboutViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = AboutFragmentBinding.inflate(layoutInflater)

        binding.listContainer.setOnScrollChangeListener { v, _, scrollY, _, _ ->
            appBarLayout.isLifted = v.canScrollVertically(-1) || scrollY > 0
        }

        lifecycleScope.launch {
            val rawReadme = viewModel.getRawReadmeAsync().await()
            binding.progress.hide()
            val text = rawReadme.getOrElse {
                binding.content.text = it.stackTraceToString()
                return@launch
            }
            val context = requireContext()
            val markwon = Markwon.builder(context)
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(GlideImagesPlugin.create(context))
                .usePlugin(GlideImagesPlugin.create(Glide.with(context)))
                .usePlugin(GlideImagesPlugin.create(object : GlideImagesPlugin.GlideStore {
                    override fun load(drawable: AsyncDrawable): RequestBuilder<Drawable> {
                        return Glide.with(context).load(drawable.destination)
                    }

                    override fun cancel(target: Target<*>) {
                        Glide.with(context).clear(target)
                    }
                }))
                .build()
            markwon.setMarkdown(binding.content, text)
        }
        return binding.root
    }
}
