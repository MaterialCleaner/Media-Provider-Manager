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

package me.gm.cleaner.plugin.mediastore.images

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.databinding.ImageItemBinding
import me.gm.cleaner.plugin.widget.StateSavedSubsamplingScaleImageView

/**
 * A fragment for displaying an image.
 */
class ImageItem : BaseFragment() {
    private val viewModel: ImageViewModel by viewModels()
    private val imagesViewModel: ImagesViewModel by activityViewModels()
    private val position by lazy { requireArguments().getInt(KEY_IMAGE_URI) }
    private lateinit var photoView: StateSavedSubsamplingScaleImageView
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = ImageItemBinding.inflate(inflater)

        val uri = imagesViewModel.images.value[position].contentUri
        photoView = binding.photoView
        // Just like we do when binding views at the grid, we set the transition name to be the string
        // value of the image res.
        photoView.transitionName = uri.toString()
        photoView.setOnImageEventListener(object : SubsamplingScaleImageView.OnImageEventListener {
            override fun onImageLoaded() {
                appBarLayout.isLifted = viewModel.isOverlay(photoView)
            }

            override fun onImageLoadError(e: Exception?) {}
            override fun onPreviewLoadError(e: Exception?) {}
            override fun onTileLoadError(e: Exception?) {}
            override fun onReady() {}
            override fun onPreviewReleased() {}
        })
        savedInstanceState ?: photoView.setImageSource(ImageSource.uri(uri))
        parentFragment?.startPostponedEnterTransition()
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        appBarLayout.post {
            supportActionBar?.apply {
                title = imagesViewModel.images.value[position].displayName
                subtitle = "${position + 1} / ${imagesViewModel.images.value.size}"
            }
        }
    }

    companion object {
        private const val KEY_IMAGE_URI = "me.gm.cleaner.plugin.key.imageUri"

        fun newInstance(position: Int): ImageItem {
            val argument = Bundle(1).apply {
                putInt(KEY_IMAGE_URI, position)
            }
            return ImageItem().apply { arguments = argument }
        }
    }
}
