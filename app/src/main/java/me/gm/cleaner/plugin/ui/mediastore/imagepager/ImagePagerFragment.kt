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

package me.gm.cleaner.plugin.ui.mediastore.imagepager

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.transition.TransitionInflater
import android.transition.TransitionSet
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.SharedElementCallback
import androidx.core.math.MathUtils.clamp
import androidx.core.os.bundleOf
import androidx.core.transition.doOnEnd
import androidx.core.transition.doOnStart
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.CarouselSnapHelper
import com.google.android.material.carousel.CustomHeroCarouselStrategy
import kotlinx.coroutines.launch
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.databinding.ImagePagerFragmentBinding
import me.gm.cleaner.plugin.ktx.addOnExitListener
import me.gm.cleaner.plugin.ui.mediastore.images.ImagesViewModel
import me.gm.cleaner.plugin.util.MediaStoreCompat
import me.gm.cleaner.plugin.util.MediaStoreCompat.DELETE_PERMISSION_REQUEST

/**
 * A fragment for displaying a series of images in a [ViewPager2].
 *
 * This is implemented with [androidx.navigation.Navigation] and has 4 [androidx.navigation.NavArgs].
 * See nav_graph.xml for more details.
 */
class ImagePagerFragment : BaseFragment() {
    private val viewModel: ImagePagerViewModel by viewModels()
    private val imagesViewModel: ImagesViewModel by activityViewModels()
    private val args: ImagePagerFragmentArgs by navArgs()
    private val lastPosition by lazy { bundleOf(KEY_POSITION to args.initialPosition) }
    private lateinit var viewPager: ViewPager2
    private lateinit var bottomBar: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = ImagePagerFragmentBinding.inflate(inflater)

        viewModel.isOverlayingLiveData.observe(viewLifecycleOwner) { isOverlaying ->
            appBarLayout?.post {
                appBarLayout?.isLifted = isOverlaying
            }
        }

        bottomBar = binding.bottomBar
        binding.bottomActionBar.setOnMenuItemClickListener(::onOptionsItemSelected)
        val carouselRecyclerView = binding.carouselRecyclerView

        viewPager = binding.viewPager
        if (args.uri == null) {
            bindForMediaStoreImages(savedInstanceState, carouselRecyclerView)
        } else {
            bindForContentProviderImage(carouselRecyclerView)
        }

        findNavController().addOnExitListener { _, destination, _ ->
            restoreAppBar(destination)
            appBarLayout?.setLiftableOverrideEnabled(false)
            requireActivity().findViewById<DrawerLayout>(R.id.drawer_layout)
                .setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        }

        prepareSharedElementTransition()
        // Avoid a postponeEnterTransition on orientation change, and postpone only of first creation.
        if (savedInstanceState == null && args.uri == null) {
            postponeEnterTransition()
        }
        return binding.root
    }

    private fun bindForMediaStoreImages(
        savedInstanceState: Bundle?, carouselRecyclerView: RecyclerView
    ) {
        val adapter = CarouselAdapter { _, position ->
            viewPager.setCurrentItem(position, true)
        }
        carouselRecyclerView.adapter = adapter
        carouselRecyclerView.layoutManager =
            CarouselLayoutManager(CustomHeroCarouselStrategy()).apply {
                carouselAlignment = CarouselLayoutManager.ALIGNMENT_CENTER
            }
        carouselRecyclerView.isNestedScrollingEnabled = false
        val enableFlingSnapHelper = CarouselSnapHelper(false)
        enableFlingSnapHelper.attachToRecyclerView(carouselRecyclerView)

        viewPager.adapter = object : FragmentStateAdapter(this) {
            val initialItemId: Long = if (savedInstanceState == null) {
                imagesViewModel.medias[args.initialPosition].id
            } else {
                0
            }

            override fun createFragment(position: Int): ImagePagerItem {
                val media = imagesViewModel.medias[position]
                return ImagePagerItem.newInstance(
                    media.contentUri, savedInstanceState == null && initialItemId == media.id
                )
            }

            override fun getItemCount(): Int = imagesViewModel.medias.size

            // Override to support collections that remove items.
            override fun getItemId(position: Int): Long = imagesViewModel.medias[position].id

            override fun containsItem(itemId: Long): Boolean =
                imagesViewModel.medias.any { itemId == it.id }
        }
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                lastPosition.putInt(KEY_POSITION, position)
                viewModel.currentItemId = imagesViewModel.medias[position].id
                updateTitle(position)
                if (carouselRecyclerView.scrollState != RecyclerView.SCROLL_STATE_DRAGGING) {
                    carouselRecyclerView.smoothScrollToPosition(position)
                }
            }
        })

        carouselRecyclerView.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                private var dragged: Boolean = false

                private fun scrollToSnapView(recyclerView: RecyclerView) {
                    val snapView = enableFlingSnapHelper.findSnapView(recyclerView.layoutManager)
                        ?: return
                    val position = recyclerView.getChildAdapterPosition(snapView)
                    viewPager.setCurrentItem(position, true)
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        dragged = true
                    } else if (dragged && newState == RecyclerView.SCROLL_STATE_IDLE) {
                        scrollToSnapView(recyclerView)
                        dragged = false
                    }
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (recyclerView.scrollState != RecyclerView.SCROLL_STATE_DRAGGING ||
                        viewPager.scrollState != ViewPager2.SCROLL_STATE_IDLE
                    ) {
                        return
                    }
                    scrollToSnapView(recyclerView)
                }
            }
        )

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                imagesViewModel.mediasFlow.collect { medias ->
                    if (medias.isEmpty()) {
                        findNavController().navigateUp()
                    } else {
                        var position = medias.indexOfFirst { viewModel.currentItemId == it.id }
                        if (position == -1) {
                            viewPager.adapter!!.notifyItemRemoved(viewPager.currentItem)
                            position = clamp(viewPager.currentItem, 0, medias.size - 1)
                        } else {
                            viewPager.setCurrentItem(position, false)
                        }
                        updateTitle(position)
                        adapter.submitList(medias) {
                            carouselRecyclerView.scrollToPosition(position)
                        }
                    }
                }
            }
        }
        // Set the current position and add a listener that will update the selection coordinator when
        // paging the images.
        if (savedInstanceState == null) {
            val position = args.initialPosition
            viewPager.setCurrentItem(position, false)
            viewModel.currentItemId = imagesViewModel.medias[position].id
        }
    }

    private fun bindForContentProviderImage(carouselRecyclerView: RecyclerView) {
        carouselRecyclerView.isVisible = false

        viewPager.adapter = object : FragmentStateAdapter(this) {

            override fun createFragment(position: Int): ImagePagerItem =
                ImagePagerItem.newInstance(args.uri!!, true)

            override fun getItemCount(): Int = 1
        }

        updateTitle(0)
    }

    private fun updateTitle(position: Int) {
        supportActionBar?.apply {
            if (args.uri == null) {
                title = imagesViewModel.medias[position].displayName
                subtitle = "${position + 1} / ${imagesViewModel.medias.size}"
            } else {
                lifecycleScope.launch {
                    val result = viewModel
                        .queryImageTitleAsync(args.uri!!)
                        .await()
                    title = result.getOrNull()
                }
            }
        }
    }

    private fun findPhotoViewForAdapterPosition(position: Int): PhotoView? =
        (viewPager[0] as RecyclerView)
            .findViewHolderForAdapterPosition(position)
            ?.itemView
            ?.findViewById(R.id.photo_view)

    private fun prepareSharedElementTransition() {
        setFragmentResult(ImagePagerFragment::class.java.name, lastPosition)

        sharedElementEnterTransition = (TransitionInflater.from(requireContext())
            .inflateTransition(R.transition.grid_pager_transition) as TransitionSet)
            .addTransition(CustomChangeImageTransform())
            .apply {
                doOnStart {
                    viewPager.isUserInputEnabled = false
                }
                doOnEnd {
                    viewPager.isUserInputEnabled = true
                }
            }

        enterTransition = TransitionInflater.from(requireContext())
            .inflateTransition(R.transition.pager_enter_transition)

        // A similar mapping is set at the GridFragment with a setExitSharedElementCallback.
        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: List<String>, sharedElements: MutableMap<String, View>
            ) {
                // Locate the image view at the primary fragment (the ImageFragment that is currently
                // visible). To locate the fragment, call instantiateItem with the selection position.
                // At this stage, the method will simply return the fragment at the position and will
                // not create a new one.
                val photoView = findPhotoViewForAdapterPosition(viewPager.currentItem) ?: return
                if (names.isNotEmpty()) {
                    // Map the first shared element name to the child ImageView.
                    sharedElements[names[0]] = photoView
                }
            }
        })
    }

    private fun getCurrentImageUri(): Uri = if (args.uri == null) {
        imagesViewModel.medias[viewPager.currentItem].contentUri
    } else {
        args.uri!!
    }

    private fun deleteCurrentImage() {
        lifecycleScope.launch {
            try {
                val isSuccessfullyDeleted =
                    MediaStoreCompat.delete(this@ImagePagerFragment, getCurrentImageUri())
                if (isSuccessfullyDeleted && args.uri != null) {
                    activity?.finish()
                }
            } catch (e: Throwable) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == DELETE_PERMISSION_REQUEST) {
            deleteCurrentImage()
        }
    }

    override fun toggleAppBar(show: Boolean) {
        super.toggleAppBar(show)
        bottomBar.isVisible = show
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.image_pager_toolbar, menu)
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
                val result = viewModel
                    .queryImageInfoAsync(getCurrentImageUri())
                    .await()
                TextSelectableInfoDialog
                    .newInstance(result.getOrElse { it.stackTraceToString() })
                    .show(childFragmentManager, null)
            }
            true
        }

        R.id.menu_share -> {
            val sendIntent = Intent(Intent.ACTION_SEND)
                .setType("image/*")
                .putExtra(Intent.EXTRA_STREAM, getCurrentImageUri())
            val shareIntent = Intent.createChooser(sendIntent, null)
            try {
                startActivity(shareIntent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
            }
            true
        }

        R.id.menu_delete -> {
            deleteCurrentImage()
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
        appBarLayout?.setLiftableOverrideEnabled(true)
        savedInstanceState?.run {
            supportActionBar?.apply {
                title = getCharSequence(SAVED_TITLE)
                subtitle = getCharSequence(SAVED_SUBTITLE)
            }
            toggleAppBar(getBoolean(SAVED_SHOWS_APPBAR))
        }
        requireActivity().findViewById<DrawerLayout>(R.id.drawer_layout)
            .setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    companion object {
        const val KEY_POSITION = "me.gm.cleaner.plugin.key.position"
        private const val SAVED_TITLE = "android:title"
        private const val SAVED_SUBTITLE = "android:subtitle"
        private const val SAVED_SHOWS_APPBAR = "android:showsAppBar"
    }
}
