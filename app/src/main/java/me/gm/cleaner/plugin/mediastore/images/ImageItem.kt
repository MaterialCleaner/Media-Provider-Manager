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
import com.bumptech.glide.Glide
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.databinding.ImageItemBinding
import me.gm.cleaner.plugin.widget.StateSavedSubsamplingScaleImageView

/**
 * A fragment for displaying an image.
 */
class ImageItem : BaseFragment() {
    private val imageViewModel: ImageViewModel by activityViewModels()
    private val imagesViewModel: ImagesViewModel by activityViewModels()
    private val position by lazy { requireArguments().getInt(KEY_IMAGE_URI) }
    private lateinit var subsamplingScaleImageView: StateSavedSubsamplingScaleImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = ImageItemBinding.inflate(inflater)

        val uri = imagesViewModel.images.value[position].contentUri
        // Just like we do when binding views at the grid, we set the transition name to be the string
        // value of the image res.
        binding.imageView.transitionName = uri.toString()
        binding.imageView.setImageBitmap(
            requireContext().contentResolver.loadThumbnail(uri, imageViewModel.size, null)
        )
        parentFragment?.startPostponedEnterTransition()
        subsamplingScaleImageView = binding.subsamplingScaleImageView
        subsamplingScaleImageView.setOnImageEventListener(object :
            SubsamplingScaleImageView.OnImageEventListener {
            private fun updateAppBar() {
                supportActionBar?.apply {
                    title = imagesViewModel.images.value[position].displayName
                    subtitle = "${position + 1} / ${imagesViewModel.images.value.size}"
                }
                val isOverlay = imageViewModel.isOverlay(subsamplingScaleImageView)
                appBarLayout.isLifted = isOverlay
                savedInstanceState ?: toggleAppBar(!isOverlay)
            }

            override fun onReady() {
                updateAppBar()
            }

            override fun onImageLoadError(e: Exception?) {
                updateAppBar()
                Glide.with(this@ImageItem)
                    .load(uri)
                    .into(binding.imageView)
            }

            override fun onImageLoaded() {}
            override fun onPreviewLoadError(e: Exception?) {}
            override fun onTileLoadError(e: Exception?) {}
            override fun onPreviewReleased() {}
        })
        if (imageViewModel.isPostponed) {
            imageViewModel.isPostponedLiveData.observe(viewLifecycleOwner) {
                if (!it) {
                    subsamplingScaleImageView.setImageSource(ImageSource.uri(uri))
                }
            }
        } else {
            subsamplingScaleImageView.setImageSource(ImageSource.uri(uri))
        }
        return binding.root
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
