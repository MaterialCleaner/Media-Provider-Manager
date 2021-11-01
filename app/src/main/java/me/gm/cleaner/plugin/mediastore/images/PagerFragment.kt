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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Px
import androidx.core.app.SharedElementCallback
import androidx.core.transition.doOnEnd
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.google.android.material.transition.platform.MaterialContainerTransform
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.databinding.PagerFragmentBinding

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
                // TODO: use flow
                pagerViewModel.updateAppBar(supportActionBar, imagesViewModel.images)
            }
        })

        prepareSharedElementTransition()
        // Avoid a postponeEnterTransition on orientation change, and postpone only of first creation.
        savedInstanceState ?: postponeEnterTransition()
        return binding.root
    }

    private fun prepareSharedElementTransition() {
        sharedElementEnterTransition = MaterialContainerTransform().apply {
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
        pagerViewModel.isFirstEntrance = true
        val currentDestination = findNavController().currentDestination ?: return
        if (currentDestination.id != R.id.pager_fragment) {
            supportActionBar?.subtitle = null
            toggleAppBar(true)
        }
    }
}
