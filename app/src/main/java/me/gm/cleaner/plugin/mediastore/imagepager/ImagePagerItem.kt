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

import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.transition.TransitionSet
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.values
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.transition.doOnEnd
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.github.chrisbanes.photoview.PhotoView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.databinding.ImagePagerItemBinding
import kotlin.math.max

/**
 * A fragment for displaying an image.
 */
class ImagePagerItem : BaseFragment() {
    private val viewModel: ImagePagerViewModel by viewModels({ requireParentFragment() })
    private val uri: Uri by lazy {
        BundleCompat.getParcelable(requireArguments(), KEY_IMAGE_URI, Uri::class.java)!!
    }
    private lateinit var photoView: PhotoView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = ImagePagerItemBinding.inflate(inflater)

        photoView = binding.photoView
        // Just like we do when binding views at the grid, we set the transition name to be the string
        // value of the image res.
        photoView.transitionName = uri.toString()
        photoView.setOnScaleChangeListener { _, _, _ ->
            viewModel.isOverlaying(photoView.displayRect)
        }
        photoView.setOnDoubleTapListener(object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                toggleAppBar(supportActionBar?.isShowing == false)
                appBarLayout.post {
                    viewModel.isOverlaying(photoView.displayRect)
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                try {
                    val scale = photoView.scale
                    val x = e.x
                    val y = e.y
                    if (scale < photoView.mediumScale) {
                        photoView.setScale(photoView.mediumScale, x, y, true)
                        toggleAppBar(false)
                    } else if (scale >= photoView.mediumScale && scale < photoView.maximumScale) {
                        photoView.setScale(photoView.maximumScale, x, y, true)
                    } else {
                        photoView.setScale(photoView.minimumScale, x, y, true)
                    }
                } catch (e: ArrayIndexOutOfBoundsException) {
                    // Can sometimes happen when getX() and getY() is called
                }
                return true
            }
        })
        Glide.with(this)
            .load(uri)
            .listener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(
                    e: GlideException?, model: Any?, target: Target<Drawable?>,
                    isFirstResource: Boolean
                ): Boolean {
                    Toast
                        .makeText(requireContext(), e?.message.toString(), Toast.LENGTH_SHORT)
                        .show()

                    parentFragment?.startPostponedEnterTransition()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable, model: Any, target: Target<Drawable?>,
                    dataSource: DataSource, isFirstResource: Boolean
                ): Boolean {
                    photoView.doOnPreDraw {
                        val displayRect = photoView.displayRect
                        var midScale = max(
                            photoView.width / displayRect.width(),
                            photoView.height / displayRect.height()
                        )
                        if (midScale == 1F) {
                            midScale = 3F
                        }
                        val maxScale = max(3 * midScale, 9F)
                        photoView.setScaleLevels(1F, midScale, maxScale)

                        if (savedInstanceState != null) {
                            val matrix = Matrix()
                            matrix.setValues(savedInstanceState.getFloatArray(KEY_MATRIX))
                            photoView.setSuppMatrix(matrix)
                        }

                        // Get the displayRect again because it may change due to restoring state.
                        viewModel.isOverlaying(photoView.displayRect)

                        parentFragment?.startPostponedEnterTransition()
                    }

                    if (resource is BitmapDrawable) {
                        val initialEntry = requireArguments().getBoolean(KEY_INITIAL_ENTRY)
                        if (savedInstanceState == null && initialEntry &&
                            findNavController().previousBackStackEntry?.destination?.id == R.id.images_fragment
                        ) {
                            (parentFragment?.sharedElementEnterTransition as TransitionSet).doOnEnd {
                                setupTilesProvider()
                            }
                        } else {
                            setupTilesProvider()
                        }
                    }
                    return false
                }
            })
            .into(photoView)
        return binding.root
    }

    private fun setupTilesProvider() {
        lifecycleScope.launch(Dispatchers.IO) {
            // runCatching is needed because the loaded resource may be from Glide cache.
            runCatching {
                requireContext().contentResolver.openFileDescriptor(uri, "r").use { pfd ->
                    photoView.setupTilesProvider(pfd)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::photoView.isInitialized) {
            val matrix = Matrix()
            photoView.getSuppMatrix(matrix)
            outState.putFloatArray(KEY_MATRIX, matrix.values())
        }
    }

    companion object {
        private const val KEY_IMAGE_URI: String = "me.gm.cleaner.plugin.key.imageUri"
        private const val KEY_INITIAL_ENTRY: String = "me.gm.cleaner.plugin.key.initialEntry"
        private const val KEY_MATRIX: String = "me.gm.cleaner.plugin.key.matrix"
        fun newInstance(uri: Uri, initialEntry: Boolean): ImagePagerItem = ImagePagerItem().apply {
            arguments = bundleOf(
                KEY_IMAGE_URI to uri,
                KEY_INITIAL_ENTRY to initialEntry
            )
        }
    }
}
