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

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.databinding.ImageItemBinding
import java.io.FileNotFoundException

/**
 * A fragment for displaying an image.
 */
class ImageItem : BaseFragment() {
    private val imageViewModel: ImageViewModel by activityViewModels()
    private val imagesViewModel: ImagesViewModel by activityViewModels()
    private val position by lazy { requireArguments().getInt(KEY_IMAGE_URI) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = ImageItemBinding.inflate(inflater)

        val uri = imagesViewModel.images[position].contentUri
        val imageView = binding.imageView
        // Just like we do when binding views at the grid, we set the transition name to be the string
        // value of the image res.
        imageView.transitionName = uri.toString()
        try {
            imageView.setImageBitmap(
                requireContext().contentResolver.loadThumbnail(uri, imageViewModel.size, null)
            )
        } catch (securityException: Throwable) {
            Toast.makeText(requireContext(), securityException.message, Toast.LENGTH_SHORT).show()
            // https://developer.android.com/training/data-storage/shared/media#update-other-apps-files
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    requireContext().contentResolver.openFileDescriptor(uri, "w")
                } catch (securityException: SecurityException) {
                    if (securityException is RecoverableSecurityException) {
                        val intentSender = securityException.userAction.actionIntent.intentSender
                        startIntentSenderForResult(intentSender, REQUEST_CODE, null, 0, 0, 0, null)
                    }
                } catch (e: FileNotFoundException) {
                }
            }
        }
        parentFragment?.startPostponedEnterTransition()
        val subsamplingScaleImageView = binding.subsamplingScaleImageView
        subsamplingScaleImageView.setOnImageEventListener(object :
            SubsamplingScaleImageView.OnImageEventListener {
            private fun consumeAppBar() {
                if (!imageViewModel.isAppBarUpToDate) {
                    imageViewModel.isAppBarUpToDate = true
                    val isOverlay = imageViewModel.isOverlay(subsamplingScaleImageView)
                    appBarLayout.isLifted = isOverlay
                    toggleAppBar(savedInstanceState?.getBoolean(SAVED_SHOWS_APPBAR) ?: !isOverlay)
                }
            }

            override fun onReady() {
                consumeAppBar()
            }

            override fun onImageLoadError(e: Exception?) {
                imageViewModel.isAppBarUpToDate = true
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
                    imageView.doOnPreDraw {
                        subsamplingScaleImageView.setImageSource(ImageSource.uri(uri))
                    }
                }
            }
        } else {
            subsamplingScaleImageView.setImageSource(ImageSource.uri(uri))
        }
        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (REQUEST_CODE == requestCode && resultCode == Activity.RESULT_OK) {
            findNavController().navigateUp()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(SAVED_SHOWS_APPBAR, supportActionBar?.isShowing ?: true)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            appBarLayout.post {
                imageViewModel.updateAppBar(supportActionBar, imagesViewModel.images)
            }
        }
    }

    companion object {
        private const val REQUEST_CODE = 0
        private const val SAVED_SHOWS_APPBAR = "android:showsAppBar"
        private const val KEY_IMAGE_URI = "me.gm.cleaner.plugin.key.imageUri"

        fun newInstance(position: Int): ImageItem {
            val argument = Bundle(1).apply {
                putInt(KEY_IMAGE_URI, position)
            }
            return ImageItem().apply { arguments = argument }
        }
    }
}
