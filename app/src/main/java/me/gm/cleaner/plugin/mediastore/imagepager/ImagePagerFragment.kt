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
import android.widget.Toast
import androidx.core.app.SharedElementCallback
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.view.get
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.github.chrisbanes.photoview.PhotoView
import kotlinx.coroutines.launch
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.databinding.ImagePagerFragmentBinding
import me.gm.cleaner.plugin.ktx.addOnExitListener

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
    private val uris = arrayListOf<Uri>()
    private val displayNames = arrayListOf<String>()
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = ImagePagerFragmentBinding.inflate(inflater)
        if (savedInstanceState == null) {
            uris += args.uris
            displayNames += args.displayNames

            updateTitle(args.initialPosition, uris.size)
        } else {
            uris += BundleCompat.getParcelableArrayList(
                savedInstanceState, SAVED_URIS, Uri::class.java
            )!!
            displayNames += savedInstanceState.getStringArrayList(SAVED_DISPLAY_NAMES)!!
        }
        require(uris.size == displayNames.size)

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
            override fun createFragment(position: Int): ImagePagerItem =
                ImagePagerItem.newInstance(uris[position])

            override fun getItemCount(): Int = uris.size

            // Override to support collections that remove items.
            override fun getItemId(position: Int): Long = uris[position].hashCode().toLong()

            override fun containsItem(itemId: Long): Boolean =
                uris.any { itemId == it.hashCode().toLong() }
        }
        // Set the current position and add a listener that will update the selection coordinator when
        // paging the images.
        viewPager.setCurrentItem(args.initialPosition, false)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                lastPosition.putInt(KEY_POSITION, position)

                val photoView = findPhotoViewForAdapterPosition(position)
                viewModel.isOverlaying(photoView.displayRect)

                updateTitle(position, uris.size)
            }
        })
        findNavController().addOnExitListener { _, destination, _ ->
            toDefaultAppBarState(destination)
        }
        viewModel.permissionNeededForDelete.observe(viewLifecycleOwner) { intentSender ->
            intentSender?.let {
                startIntentSenderForResult(
                    intentSender, DELETE_PERMISSION_REQUEST, null, 0, 0, 0, null
                )
            }
        }

        prepareSharedElementTransition()
        // Avoid a postponeEnterTransition on orientation change, and postpone only of first creation.
        if (savedInstanceState == null && args.isMediaStoreUri) {
            postponeEnterTransition()
        }
        return binding.root
    }

    private fun updateTitle(position: Int, size: Int) {
        supportActionBar?.apply {
            title = displayNames[position]
            if (args.isMediaStoreUri) {
                subtitle = "${position + 1} / $size"
            }
        }
    }

    private fun findPhotoViewForAdapterPosition(position: Int): PhotoView =
        (viewPager[0] as RecyclerView)
            .findViewHolderForAdapterPosition(position)!!
            .itemView
            .findViewById(R.id.photo_view)

    private fun deleteCurrentImage() {
        lifecycleScope.launch {
            val position = viewPager.currentItem
            val isSuccessfullyDeleted = viewModel.deleteImageAsync(uris[position]).await()
            if (isSuccessfullyDeleted) {
                uris.removeAt(position)
                displayNames.removeAt(position)
                if (uris.isEmpty()) {
                    findNavController().navigateUp()
                } else {
                    viewPager.adapter!!.notifyItemRemoved(position)
                    updateTitle(position, uris.size)
                }
            }
        }
    }

    private fun prepareSharedElementTransition() {
        setFragmentResult(ImagePagerFragment::class.java.name, lastPosition)

        sharedElementEnterTransition = (TransitionInflater.from(requireContext())
            .inflateTransition(R.transition.grid_pager_transition) as TransitionSet)
            .addTransition(CustomChangeImageTransform())

        // A similar mapping is set at the GridFragment with a setExitSharedElementCallback.
        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: List<String>, sharedElements: MutableMap<String, View>
            ) {
                // Locate the image view at the primary fragment (the ImageFragment that is currently
                // visible). To locate the fragment, call instantiateItem with the selection position.
                // At this stage, the method will simply return the fragment at the position and will
                // not create a new one.
                val photoView = findPhotoViewForAdapterPosition(viewPager.currentItem)
                if (names.isNotEmpty()) {
                    // Map the first shared element name to the child ImageView.
                    sharedElements[names[0]] = photoView
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == DELETE_PERMISSION_REQUEST) {
            deleteCurrentImage()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.image_pager_toolbar, menu)
        if (!args.isMediaStoreUri) {
            menu.removeItem(R.id.menu_info)
            menu.removeItem(R.id.menu_share)
            menu.removeItem(R.id.menu_delete)
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
                val result = viewModel.queryImageInfoAsync(uris[viewPager.currentItem]).await()
                TextSelectableInfoDialog
                    .newInstance(result.getOrElse { it.stackTraceToString() })
                    .show(childFragmentManager, null)
            }
            true
        }

        R.id.menu_share -> {
            val currentItem = viewPager.currentItem
            val sendIntent = Intent(Intent.ACTION_SEND)
                .setType("image/*")
                .putExtra(Intent.EXTRA_STREAM, uris[currentItem])
                .putExtra(Intent.EXTRA_TEXT, displayNames[currentItem])
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
        outState.putParcelableArrayList(SAVED_URIS, uris)
        outState.putStringArrayList(SAVED_DISPLAY_NAMES, displayNames)
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
        private const val SAVED_URIS = "android:uris"
        private const val SAVED_DISPLAY_NAMES = "android:displayNames"
        private const val SAVED_TITLE = "android:title"
        private const val SAVED_SUBTITLE = "android:subtitle"
        private const val SAVED_SHOWS_APPBAR = "android:showsAppBar"
        private const val DELETE_PERMISSION_REQUEST = 0x1145
    }
}
