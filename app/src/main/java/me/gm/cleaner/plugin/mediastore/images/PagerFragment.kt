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

package me.gm.cleaner.plugin.mediastore.images

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Px
import androidx.core.app.SharedElementCallback
import androidx.core.transition.doOnEnd
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.NavDestination
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.google.android.material.transition.platform.FitsScaleMaterialContainerTransform
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.databinding.PagerFragmentBinding
import me.gm.cleaner.plugin.util.mediumAnimTime

class PagerFragment : BaseFragment() {
    private val pagerViewModel: PagerViewModel by activityViewModels()
    private val imagesViewModel: ImagesViewModel by activityViewModels()
    private lateinit var viewPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = PagerFragmentBinding.inflate(inflater)
        val size = imagesViewModel.images.size

        viewPager = binding.viewPager
        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun createFragment(position: Int) = PagerItem.newInstance(position)
            override fun getItemCount() = size
        }
        // Set the current position and add a listener that will update the selection coordinator when
        // paging the images.
        viewPager.setCurrentItem(pagerViewModel.currentPosition, false)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int, positionOffset: Float, @Px positionOffsetPixels: Int
            ) {
                pagerViewModel.currentPosition = position
                val ssiv: SubsamplingScaleImageView =
                    viewPager.findViewById(R.id.subsampling_scale_image_view)
                appBarLayout.isLifted = pagerViewModel.isOverlay(ssiv)
            }
        })
        pagerViewModel.currentAppBarTitleSourceFlow.asLiveData().observe(viewLifecycleOwner) {
            val currentPosition = it.first
            val currentDestination = it.second ?: return@observe
            when (currentDestination.id) {
                R.id.images_fragment -> toDefaultAppBarState(currentDestination)
                R.id.pager_fragment -> supportActionBar?.apply {
                    title = imagesViewModel.images[currentPosition].displayName
                    subtitle = "${currentPosition + 1} / ${imagesViewModel.images.size}"
                }
            }
        }

        prepareSharedElementTransition()
        // Avoid a postponeEnterTransition on orientation change, and postpone only of first creation.
        if (savedInstanceState == null) {
            postponeEnterTransition()
        } else {
            viewPager.visibility = View.VISIBLE
        }
        return binding.root
    }

    private fun prepareSharedElementTransition() {
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
                    childFragmentManager.findFragmentByTag("f${pagerViewModel.currentPosition}")
                val view = currentFragment?.view ?: return

                // Map the first shared element name to the child ImageView.
                if (names.isNotEmpty()) {
                    sharedElements[names[0]] = view.findViewById(R.id.image_view)
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val currentDestination = findNavController().currentDestination ?: return
        // Restore AppBar state.
        if (currentDestination.id == R.id.pager_fragment) {
            pagerViewModel.isFirstEntrance = true
        } else {
            toDefaultAppBarState(currentDestination)
        }
    }

    private fun toDefaultAppBarState(currentDestination: NavDestination) {
        supportActionBar?.apply {
            title = currentDestination.label
            subtitle = null
        }
        toggleAppBar(true)
    }
}
