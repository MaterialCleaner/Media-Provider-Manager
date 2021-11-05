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

import android.graphics.PointF
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.databinding.PagerItemBinding
import java.io.FileNotFoundException

/**
 * A fragment for displaying an image.
 */
class PagerItem : BaseFragment() {
    private val pagerViewModel: PagerViewModel by activityViewModels()
    private val imagesViewModel: ImagesViewModel by activityViewModels()
    private val position by lazy { requireArguments().getInt(KEY_IMAGE_URI) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = PagerItemBinding.inflate(inflater)

        val uri = imagesViewModel.images[position].contentUri
        val imageView = binding.imageView
        // Just like we do when binding views at the grid, we set the transition name to be the string
        // value of the image res.
        imageView.transitionName = uri.toString()
        try {
            // Setting thumbnail on an invisible image view to figure out the image's size for a better transition.
            if (pagerViewModel.isFirstEntrance) {
                imageView.setImageBitmap(
                    requireContext().contentResolver.loadThumbnail(uri, pagerViewModel.size, null)
                )
            }
        } catch (e: Throwable) {
            Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
            // https://developer.android.com/training/data-storage/shared/media#update-other-apps-files
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    imagesViewModel.deleteImage(imagesViewModel.images[position])
                    findNavController().navigateUp()
                } catch (e: FileNotFoundException) {
                }
            }
        }
        val ssiv = binding.subsamplingScaleImageView
        ssiv.setOnImageEventListener(object : SubsamplingScaleImageView.OnImageEventListener {
            override fun onImageLoaded() {
                if (pagerViewModel.isFirstEntrance) {
                    pagerViewModel.isFirstEntrance = false
                    val isOverlay = pagerViewModel.isOverlay(ssiv)
                    appBarLayout.isLifted = isOverlay
                    toggleAppBar(savedInstanceState?.getBoolean(SAVED_SHOWS_APPBAR) ?: !isOverlay)
                }
                imageView.visibility = View.INVISIBLE
            }

            override fun onImageLoadError(e: Exception?) {
                pagerViewModel.isFirstEntrance = false
                imageView.visibility = View.VISIBLE
                Glide.with(this@PagerItem)
                    .load(uri)
                    .into(imageView)
            }

            override fun onReady() {}
            override fun onPreviewLoadError(e: Exception?) {}
            override fun onTileLoadError(e: Exception?) {}
            override fun onPreviewReleased() {}
        })
        ssiv.setOnClickListener {
            toggleAppBar(!supportActionBar!!.isShowing)
        }
        ssiv.setOnStateChangedListener(object : SubsamplingScaleImageView.OnStateChangedListener {
            override fun onScaleChanged(newScale: Float, origin: Int) {
                appBarLayout.isLifted = pagerViewModel.isOverlay(ssiv)
            }

            override fun onCenterChanged(newCenter: PointF?, origin: Int) {
                appBarLayout.isLifted = pagerViewModel.isOverlay(ssiv)
            }
        })
        if (savedInstanceState == null) {
            ssiv.setImageUri(uri)
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

        fun newInstance(position: Int): PagerItem {
            val argument = Bundle(1).apply {
                putInt(KEY_IMAGE_URI, position)
            }
            return PagerItem().apply { arguments = argument }
        }
    }
}
