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

package me.gm.cleaner.plugin.mediastore.imagepager

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import androidx.annotation.Px
import androidx.core.app.SharedElementCallback
import androidx.core.os.bundleOf
import androidx.core.transition.doOnEnd
import androidx.core.view.isInvisible
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.google.android.material.transition.platform.MaterialContainerTransform
import kotlinx.coroutines.launch
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.app.InfoDialog
import me.gm.cleaner.plugin.databinding.ImagePagerFragmentBinding
import me.gm.cleaner.plugin.ktx.addOnExitListener
import me.gm.cleaner.plugin.ktx.colorSurface
import me.gm.cleaner.plugin.ktx.mediumAnimTime

/**
 * A fragment for displaying a series of images in a [ViewPager2].
 *
 * This is implemented with [androidx.navigation.Navigation] and has 4 [androidx.navigation.NavArgs].
 * See nav_graph.xml for more details.
 */
class ImagePagerFragment : BaseFragment() {
    private val viewModel: ImagePagerViewModel by viewModels()
    private val args: ImagePagerFragmentArgs by navArgs()
    private val navController by lazy { findNavController() }
    private val lastPosition by lazy { bundleOf(KEY_POSITION to args.initialPosition) }
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = ImagePagerFragmentBinding.inflate(inflater)
        val size = args.uris.size
        val initialPosition = args.initialPosition

        viewPager = binding.viewPager
        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun createFragment(position: Int) =
                ImagePagerItem.newInstance(args.uris[position], args.isMediaStoreUri)

            override fun getItemCount() = size
        }
        // Set the current position and add a listener that will update the selection coordinator when
        // paging the images.
        viewPager.setCurrentItem(initialPosition, false)
        supportActionBar?.apply {
            title = args.displayNames[initialPosition]
            if (args.isMediaStoreUri) {
                subtitle = "${initialPosition + 1} / $size"
            }
        }
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int, positionOffset: Float, @Px positionOffsetPixels: Int
            ) {
                lastPosition.putInt(KEY_POSITION, position)
                val ssiv: SubsamplingScaleImageView =
                    viewPager.findViewById(R.id.subsampling_scale_image_view)
                appBarLayout.isLifted = viewModel.isOverlay(ssiv)
            }

            override fun onPageSelected(position: Int) {
                supportActionBar?.apply {
                    title = args.displayNames[position]
                    if (args.isMediaStoreUri) {
                        subtitle = "${position + 1} / $size"
                    }
                }
            }
        })
        navController.addOnExitListener { _, destination, _ ->
            toDefaultAppBarState(destination)
        }

        prepareSharedElementTransition()
        // Avoid a postponeEnterTransition on orientation change, and postpone only of first creation.
        if (savedInstanceState == null && args.isMediaStoreUri) {
            postponeEnterTransition()
        } else {
            viewPager.isInvisible = false
        }
        return binding.root
    }

    private fun prepareSharedElementTransition() {
        setFragmentResult(ImagePagerFragment::class.java.name, lastPosition)

        sharedElementEnterTransition = MaterialContainerTransform(requireContext(), true).apply {
            setAllContainerColors(requireContext().colorSurface)
            scrimColor = Color.TRANSPARENT
            fadeMode = MaterialContainerTransform.FADE_MODE_CROSS
            duration = requireContext().mediumAnimTime
            doOnEnd {
                viewPager.isInvisible = false
            }
        }

        // A similar mapping is set at the GridFragment with a setExitSharedElementCallback.
        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: List<String>, sharedElements: MutableMap<String, View>
            ) {
                // Locate the image view at the primary fragment (the ImageFragment that is currently
                // visible). To locate the fragment, call instantiateItem with the selection position.
                // At this stage, the method will simply return the fragment at the position and will
                // not create a new one.
                // @see https://stackoverflow.com/questions/55728719/get-current-fragment-with-viewpager2
                val currentFragment =
                    childFragmentManager.findFragmentByTag("f${lastPosition.getInt(KEY_POSITION)}")
                val view = currentFragment?.view ?: return

                val imageView: ImageView = view.findViewById(R.id.image_view)
                val ssiv: SubsamplingScaleImageView =
                    view.findViewById(R.id.subsampling_scale_image_view)
                if (names.isNotEmpty()) {
                    if (navController.currentDestination?.id != R.id.image_pager_fragment &&
                        imageView.isInvisible
                    ) {
                        // Change the registered shared element for a better exit transition.
                        // Note that this is not the perfect solution but much better than don't.
                        ssiv.transitionName = imageView.transitionName
                        imageView.transitionName = null
                        sharedElements[names[0]] = ssiv
                    } else {
                        // Map the first shared element name to the child ImageView.
                        sharedElements[names[0]] = imageView
                    }
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.image_pager_toolbar, menu)
        if (!args.isMediaStoreUri) {
            menu.removeItem(R.id.menu_info)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_screen_rotation -> {
            requireActivity().requestedOrientation = when (resources.configuration.orientation) {
                Configuration.ORIENTATION_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                Configuration.ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                else -> throw IllegalArgumentException()
            }
            true
        }
        R.id.menu_info -> {
            lifecycleScope.launch {
                val result = viewModel.queryImageInfoAsync(args.uris[viewPager.currentItem]).await()
                InfoDialog.newInstance(result.getOrElse { it.stackTraceToString() })
                    .show(childFragmentManager, null)
            }
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (navController.currentDestination?.id == R.id.image_pager_fragment) {
            // Restore AppBar state.
            viewModel.isFirstEntrance = true
        }
    }

    companion object {
        const val KEY_POSITION = "me.gm.cleaner.plugin.key.position"
    }
}
