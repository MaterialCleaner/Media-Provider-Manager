/*
* Copyright 2018 Google LLC
* Copyright 2021 Green Mushroom
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

package me.gm.cleaner.plugin.mediastore.imagepager

import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isInvisible
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.databinding.ImagePagerItemBinding

/**
 * A fragment for displaying an image.
 */
class ImagePagerItem : BaseFragment() {
    private val viewModel by lazy { ViewModelProvider(requireParentFragment())[ImagePagerViewModel::class.java] }
    private val uri by lazy { requireArguments().getParcelable<Uri>(KEY_IMAGE_URI)!! }
    private val isMediaStoreUri by lazy { requireArguments().getBoolean(KEY_IS_MEDIA_STORE_URI) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = ImagePagerItemBinding.inflate(inflater)

        val imageView = binding.imageView
        // Just like we do when binding views at the grid, we set the transition name to be the string
        // value of the image res.
        imageView.transitionName = uri.toString()
        try {
            // Setting thumbnail on an invisible image view to figure out the image's size for a better transition.
            if (viewModel.isFirstEntrance) {
                imageView.setImageBitmap(
                    requireContext().contentResolver.loadThumbnail(uri, viewModel.size, null)
                )
            }
        } catch (e: Throwable) {
            Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
            parentFragment?.startPostponedEnterTransition()
            return binding.root
        }
        val ssiv = binding.subsamplingScaleImageView
        ssiv.orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF
        ssiv.setOnImageEventListener(object : SubsamplingScaleImageView.OnImageEventListener {
            override fun onImageLoaded() {
                if (viewModel.isFirstEntrance) {
                    viewModel.isFirstEntrance = false
                    val isOverlay = viewModel.isOverlay(ssiv)
                    appBarLayout.isLifted = isOverlay
                    toggleAppBar(savedInstanceState?.getBoolean(SAVED_SHOWS_APPBAR) ?: !isOverlay)
                }
                imageView.isInvisible = true
            }

            override fun onImageLoadError(e: Exception?) {
                viewModel.isFirstEntrance = false
                imageView.isInvisible = false
                Glide.with(this@ImagePagerItem)
                    .load(uri)
                    .into(imageView)
            }

            override fun onReady() {}
            override fun onPreviewLoadError(e: Exception?) {}
            override fun onTileLoadError(e: Exception?) {}
            override fun onPreviewReleased() {}
        })
        ssiv.setOnClickListener {
            toggleAppBar(supportActionBar?.isShowing == false)
        }
        ssiv.setOnStateChangedListener(object : SubsamplingScaleImageView.OnStateChangedListener {
            override fun onScaleChanged(newScale: Float, origin: Int) {
                appBarLayout.isLifted = viewModel.isOverlay(ssiv)
            }

            override fun onCenterChanged(newCenter: PointF?, origin: Int) {
                appBarLayout.isLifted = viewModel.isOverlay(ssiv)
            }
        })
        if (savedInstanceState == null) {
            if (isMediaStoreUri) {
                ssiv.setImageUri(uri)
            } else {
                ssiv.decodeImageUri(uri)
            }
        }
        parentFragment?.startPostponedEnterTransition()
        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(SAVED_SHOWS_APPBAR, supportActionBar?.isShowing ?: true)
        outState.putCharSequence(SAVED_TITLE, supportActionBar?.title)
        outState.putCharSequence(SAVED_SUBTITLE, supportActionBar?.subtitle)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        savedInstanceState?.run {
            supportActionBar?.apply {
                title = getCharSequence(SAVED_TITLE)
                subtitle = getCharSequence(SAVED_SUBTITLE)
            }
        }
    }

    companion object {
        private const val SAVED_TITLE = "android:title"
        private const val SAVED_SUBTITLE = "android:subtitle"
        private const val SAVED_SHOWS_APPBAR = "android:showsAppBar"
        private const val KEY_IMAGE_URI = "me.gm.cleaner.plugin.key.imageUri"
        private const val KEY_IS_MEDIA_STORE_URI = "me.gm.cleaner.plugin.key.isMediaStoreUri"
        fun newInstance(uri: Uri, isMediaStoreUri: Boolean) = ImagePagerItem().apply {
            arguments = bundleOf(
                KEY_IMAGE_URI to uri,
                KEY_IS_MEDIA_STORE_URI to isMediaStoreUri
            )
        }
    }
}
