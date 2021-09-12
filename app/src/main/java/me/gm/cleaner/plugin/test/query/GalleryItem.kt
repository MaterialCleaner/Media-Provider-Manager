/*
* Copyright 2018 Google LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package me.gm.cleaner.plugin.test.query

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import me.gm.cleaner.plugin.databinding.GalleryItemBinding

/**
 * A fragment for displaying an image.
 */
class GalleryItem : Fragment() {
    private val viewModel by activityViewModels<QueryViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = GalleryItemBinding.inflate(inflater)
        val position = requireArguments().getInt(KEY_IMAGE_URI)
        val uri = viewModel.images.value!![position].contentUri

        // Just like we do when binding views at the grid, we set the transition name to be the string
        // value of the image res.
        if (savedInstanceState == null) {
            binding.photoView.transitionName = uri.toString()
            binding.photoView.setImageSource(ImageSource.uri(uri))
            binding.photoView.setOnImageEventListener(object :
                SubsamplingScaleImageView.OnImageEventListener {
                override fun onImageLoaded() {
                    parentFragment?.startPostponedEnterTransition()
                }

                override fun onImageLoadError(e: Exception?) {
                    parentFragment?.startPostponedEnterTransition()
                }

                override fun onPreviewLoadError(e: Exception?) {}
                override fun onTileLoadError(e: Exception?) {}
                override fun onReady() {}
                override fun onPreviewReleased() {}
            })
        }
        return binding.root
    }

    companion object {
        private const val KEY_IMAGE_URI = "me.gm.cleaner.plugin.key.imageUri"

        fun newInstance(position: Int): GalleryItem {
            val fragment = GalleryItem()
            val argument = Bundle()
            argument.putInt(KEY_IMAGE_URI, position)
            fragment.arguments = argument
            return fragment
        }
    }
}
