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

package me.gm.cleaner.plugin.experiment

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.databinding.ComingSoonFragmentBinding

@AndroidEntryPoint
class ExperimentFragment : BaseFragment() {
    private val viewModel: ExperimentViewModel by viewModels()
    private val width by lazy { resources.displayMetrics.widthPixels }
    private val downloadManager by lazy { requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = ComingSoonFragmentBinding.inflate(layoutInflater)

        viewModel.unsplashPhotosFlow.observe(viewLifecycleOwner) { result ->
            result.onSuccess { unsplashPhotos ->
                repeat(15) {
                    val unsplashPhoto = unsplashPhotos.random()
                    val request = DownloadManager
                        .Request(Uri.parse(unsplashPhoto.getPhotoUrl(width)))
                        .setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_PICTURES, unsplashPhoto.filename
                        )
                    val id = downloadManager.enqueue(request)
                }
            }
        }
        viewModel.loadPhotos()

        return binding.root
    }
}
