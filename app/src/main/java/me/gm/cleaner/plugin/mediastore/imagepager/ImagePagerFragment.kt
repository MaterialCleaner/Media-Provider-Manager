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

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import androidx.annotation.Px
import androidx.core.app.SharedElementCallback
import androidx.core.os.bundleOf
import androidx.core.transition.doOnEnd
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.google.android.material.transition.platform.FitsScaleMaterialContainerTransform
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.databinding.ImagePagerFragmentBinding
import me.gm.cleaner.plugin.util.LogUtils
import me.gm.cleaner.plugin.util.mediumAnimTime

/**
 * Display a series of images in a [ViewPager2].
 *
 * This is implemented with [androidx.navigation.Navigation] and requires 3 [androidx.navigation.NavArgs].
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
        // TODO: enable options menu when PagerFragment decoupled
//        setHasOptionsMenu(true)
        LogUtils.e("Useful overriding method.")
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
                ImagePagerItem.newInstance(args.uris[position])

            override fun getItemCount() = size
        }
        // Set the current position and add a listener that will update the selection coordinator when
        // paging the images.
        viewPager.setCurrentItem(initialPosition, false)
        supportActionBar?.apply {
            title = args.displayNames[initialPosition]
            subtitle = "${initialPosition + 1} / $size"
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int, positionOffset: Float, @Px positionOffsetPixels: Int
            ) {
                lastPosition.putInt(KEY_POSITION, position)
                supportActionBar?.apply {
                    title = args.displayNames[position]
                    subtitle = "${position + 1} / $size"
                }
                val ssiv: SubsamplingScaleImageView =
                    viewPager.findViewById(R.id.subsampling_scale_image_view)
                appBarLayout.isLifted = viewModel.isOverlay(ssiv)
            }
        })
        navController.addOnDestinationChangedListener(object :
            NavController.OnDestinationChangedListener {
            override fun onDestinationChanged(
                controller: NavController, destination: NavDestination, arguments: Bundle?
            ) {
                if (destination.id != R.id.image_pager_fragment) {
                    navController.removeOnDestinationChangedListener(this)
                    toDefaultAppBarState(destination)
                }
            }
        })

        prepareSharedElementTransition()
        // Avoid a postponeEnterTransition on orientation change, and postpone only of first creation.
        if (savedInstanceState == null) {
            postponeEnterTransition()
        } else {
            viewPager.visibility = View.VISIBLE
        }
        return binding.root
    }

    private fun toDefaultAppBarState(currentDestination: NavDestination) {
        supportActionBar?.apply {
            title = currentDestination.label
            subtitle = null
        }
        toggleAppBar(true)
    }

    private fun prepareSharedElementTransition() {
        setFragmentResult(ImagePagerFragment::class.java.simpleName, lastPosition)

        sharedElementEnterTransition = FitsScaleMaterialContainerTransform().apply {
            scrimColor = Color.TRANSPARENT
            duration = requireContext().mediumAnimTime
            doOnEnd {
                viewPager.visibility = View.VISIBLE
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
                // https://stackoverflow.com/questions/55728719/get-current-fragment-with-viewpager2
                val currentFragment =
                    childFragmentManager.findFragmentByTag("f${lastPosition.getInt(KEY_POSITION)}")
                val view = currentFragment?.view ?: return

                val imageView: ImageView = view.findViewById(R.id.image_view)
                val ssiv: SubsamplingScaleImageView =
                    view.findViewById(R.id.subsampling_scale_image_view)
                if (names.isNotEmpty()) {
                    if (navController.currentDestination?.id == R.id.images_fragment &&
                        imageView.visibility == View.INVISIBLE
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
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_info -> {
            TODO("Not yet implemented")
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Restore AppBar state.
        if (navController.currentDestination?.id == R.id.image_pager_fragment) {
            viewModel.isFirstEntrance = true
        }
    }

    companion object {
        const val KEY_POSITION = "me.gm.cleaner.plugin.key.position"
    }
}
