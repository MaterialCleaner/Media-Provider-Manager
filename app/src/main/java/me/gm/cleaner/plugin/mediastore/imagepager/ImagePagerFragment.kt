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
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.app.SharedElementCallback
import androidx.core.os.bundleOf
import androidx.core.transition.doOnEnd
import androidx.core.view.get
import androidx.core.view.isInvisible
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.transition.platform.MaterialContainerTransform
import kotlinx.coroutines.launch
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.app.BaseFragment
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

        var isNotifyingActive = true
        var isFirstTimeEntry = true
        viewModel.isOverlayingLiveData.observe(viewLifecycleOwner) { isOverlaying ->
            appBarLayout.isLifted = isOverlaying
            // LiveData will notify us when the lifecycle starts to become active,
            // but as for toggleAppBar it is not needed, so ignore it.
            if (isNotifyingActive) {
                isNotifyingActive = false
            } else if (savedInstanceState == null && isFirstTimeEntry) {
                isFirstTimeEntry = false
                toggleAppBar(!isOverlaying)
            }
        }

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
            if (args.isMediaStoreUri) {
                subtitle = "${initialPosition + 1} / $size"
            }
        }
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                lastPosition.putInt(KEY_POSITION, position)

                val photoView = (viewPager[0] as RecyclerView)
                    .findViewHolderForAdapterPosition(position)!!
                    .itemView
                    .findViewById<PhotoView>(R.id.photo_view)
                viewModel.isOverlaying(photoView.displayRect)

                supportActionBar?.apply {
                    title = args.displayNames[position]
                    if (args.isMediaStoreUri) {
                        subtitle = "${position + 1} / $size"
                    }
                }
            }
        })
        findNavController().addOnExitListener { _, destination, _ ->
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

                val photoView = view.findViewById<PhotoView>(R.id.photo_view)
                if (names.isNotEmpty()) {
                    // Map the first shared element name to the child ImageView.
                    sharedElements[names[0]] = photoView
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
                TextSelectableInfoDialog
                    .newInstance(result.getOrElse { it.stackTraceToString() })
                    .show(childFragmentManager, null)
            }
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence(SAVED_TITLE, supportActionBar?.title)
        outState.putCharSequence(SAVED_SUBTITLE, supportActionBar?.subtitle)
        outState.putBoolean(SAVED_SHOWS_APPBAR, supportActionBar?.isShowing ?: true)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        appBarLayout.setLiftOnScrollTargetView(null)
        savedInstanceState?.run {
            supportActionBar?.apply {
                title = getCharSequence(SAVED_TITLE)
                subtitle = getCharSequence(SAVED_SUBTITLE)
            }
            toggleAppBar(getBoolean(SAVED_SHOWS_APPBAR))
        }
    }

    companion object {
        const val KEY_POSITION = "me.gm.cleaner.plugin.key.position"
        private const val SAVED_TITLE = "android:title"
        private const val SAVED_SUBTITLE = "android:subtitle"
        private const val SAVED_SHOWS_APPBAR = "android:showsAppBar"
    }
}
