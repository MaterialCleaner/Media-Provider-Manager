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

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.databinding.ImagePagerItemBinding

/**
 * A fragment for displaying an image.
 */
class ImagePagerItem : BaseFragment() {
    private val viewModel: ImagePagerViewModel by viewModels({ requireParentFragment() })
    private val uri by lazy { requireArguments().getParcelable<Uri>(KEY_IMAGE_URI)!! }
    private val isMediaStoreUri by lazy { requireArguments().getBoolean(KEY_IS_MEDIA_STORE_URI) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = ImagePagerItemBinding.inflate(inflater)

        val photoView = binding.photoView
        // Just like we do when binding views at the grid, we set the transition name to be the string
        // value of the image res.
        photoView.transitionName = uri.toString()
        photoView.setScaleLevels(1F, 3F, 9F)
        photoView.setOnScaleChangeListener { _, _, _ ->
            viewModel.isOverlaying(photoView.displayRect)
        }
        photoView.setOnClickListener {
            toggleAppBar(supportActionBar?.isShowing == false)
        }
        Glide.with(this)
            .load(uri)
            .listener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(
                    e: GlideException?, model: Any?, target: Target<Drawable?>?,
                    isFirstResource: Boolean
                ): Boolean {
                    Toast
                        .makeText(requireContext(), e?.message.toString(), Toast.LENGTH_SHORT)
                        .show()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?, model: Any?, target: Target<Drawable?>?,
                    dataSource: DataSource?, isFirstResource: Boolean
                ): Boolean {
                    photoView.doOnPreDraw {
                        viewModel.isOverlaying(photoView.displayRect)
                    }
                    if (resource is BitmapDrawable) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            requireContext().contentResolver.openFileDescriptor(uri, "r")
                                .use { pfd ->
                                    photoView.setupTilesProvider(pfd)
                                }
                        }
                    }
                    return false
                }
            })
            .into(photoView)
        if (savedInstanceState != null) {
            // TODO: restore photoView state
        }
        // TODO: maybe move to Glide listener
        parentFragment?.startPostponedEnterTransition()
        return binding.root
    }

    companion object {
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
