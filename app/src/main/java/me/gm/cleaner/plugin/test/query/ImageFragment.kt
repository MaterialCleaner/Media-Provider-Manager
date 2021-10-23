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

package me.gm.cleaner.plugin.test.query

import android.graphics.PointF
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Px
import androidx.core.app.SharedElementCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.databinding.GalleryFragmentBinding
import me.gm.cleaner.plugin.test.TestActivity
import me.gm.cleaner.plugin.util.getDimenByAttr
import rikka.core.util.ResourceUtils

class ImageFragment : BaseFragment() {
    private val viewModel: QueryViewModel by activityViewModels()
    private lateinit var viewPager: ViewPager2
    private val top by lazy {
        val actionBarSize = requireContext().getDimenByAttr(android.R.attr.actionBarSize).toInt()
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        resources.getDimensionPixelSize(resourceId) + actionBarSize
    }
    private val vTarget by lazy { PointF() }

    private class PagerAdapter(fragment: Fragment, private val imageCount: Int) :
        FragmentStateAdapter(fragment) {
        override fun createFragment(position: Int): Fragment = GalleryItem.newInstance(position)

        override fun getItemCount(): Int = imageCount
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = GalleryFragmentBinding.inflate(inflater)
        setAppBar(binding.root).apply {
            setNavigationOnClickListener { it.findNavController().navigateUp() }
            setNavigationIcon(R.drawable.ic_outline_arrow_back_24)
        }

        viewPager = binding.viewPager
        val size = viewModel.images.value!!.size
        viewPager.adapter = PagerAdapter(this, size)
        // Set the current position and add a listener that will update the selection coordinator when
        // paging the images.
        viewPager.setCurrentItem(viewModel.currentPosition, false)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int, positionOffset: Float, @Px positionOffsetPixels: Int
            ) {
                viewModel.currentPosition = position
                val photoView: SubsamplingScaleImageView = viewPager.findViewById(R.id.photo_view)
                (requireActivity() as TestActivity).supportActionBar?.apply {
                    title = viewModel.images.value!![position].displayName
                    subtitle = "${position + 1} / $size"
                    photoView.setOnClickListener {
                        val decorView = requireActivity().window.decorView
                        if (isShowing) {
                            hide()
                            // Fullscreen is costly in my case, so I come to terms with immersive.
                            // If you persist in fullscreen, I'd advise you to display the photos with activity.
                            // See also: https://developer.android.com/training/system-ui/immersive
                            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        } else {
                            show()
                            var flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            if (!ResourceUtils.isNightMode(resources.configuration)) {
                                flags = flags or
                                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                            }
                            decorView.systemUiVisibility = flags
                        }
                    }
                }
                photoView.setOnStateChangedListener(object :
                    SubsamplingScaleImageView.OnStateChangedListener {
                    override fun onScaleChanged(newScale: Float, origin: Int) {
                        if (photoView.isReady) {
                            photoView.sourceToViewCoord(0f, 0f, vTarget)
                            appBarLayout.isRaised = vTarget.y - top < 0
                        }
                    }

                    override fun onCenterChanged(newCenter: PointF?, origin: Int) {
                        if (photoView.isReady) {
                            photoView.sourceToViewCoord(0f, 0f, vTarget)
                            appBarLayout.isRaised = vTarget.y - top < 0
                        }
                    }
                })
                if (photoView.isReady) {
                    photoView.sourceToViewCoord(0f, 0f, vTarget)
                    appBarLayout.isRaised = vTarget.y - top < 0
                }
            }
        })

        prepareSharedElementTransition()
        // Avoid a postponeEnterTransition on orientation change, and postpone only of first creation.
        savedInstanceState ?: postponeEnterTransition()
        return binding.root
    }

    private fun prepareSharedElementTransition() {
        sharedElementEnterTransition = TransitionInflater.from(context)
            .inflateTransition(R.transition.image_shared_element_transition)

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
                    childFragmentManager.findFragmentByTag("f${viewModel.currentPosition}")
                val view = currentFragment?.view ?: return

                // Map the first shared element name to the child ImageView.
                sharedElements[names[0]] = view.findViewById(R.id.photo_view)
            }
        })
    }
}
