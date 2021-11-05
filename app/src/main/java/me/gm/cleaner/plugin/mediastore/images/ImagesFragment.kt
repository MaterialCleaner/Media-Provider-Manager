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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.SharedElementCallback
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.dao.ModulePreferences
import me.gm.cleaner.plugin.databinding.ImagesFragmentBinding
import me.gm.cleaner.plugin.mediastore.MediaStoreFragment
import me.gm.cleaner.plugin.util.addLiftOnScrollListener
import me.gm.cleaner.plugin.util.initFastScroller
import me.gm.cleaner.plugin.util.overScrollIfContentScrollsPersistent
import rikka.recyclerview.fixEdgeEffect

class ImagesFragment : MediaStoreFragment() {
    private val pagerViewModel: PagerViewModel by activityViewModels()
    private val imagesViewModel: ImagesViewModel by activityViewModels()
    private lateinit var list: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = ImagesFragmentBinding.inflate(inflater)

        val adapter = ImagesAdapter(this)
        val layoutManager = GridLayoutManager(requireContext(), 3)
        list = binding.list
        list.adapter = adapter
        list.layoutManager = layoutManager
        list.setHasFixedSize(true)
        list.initFastScroller()
        list.fixEdgeEffect(false)
        list.overScrollIfContentScrollsPersistent()
        list.addLiftOnScrollListener { appBarLayout.isLifted = it }
//        var tracker = SelectionTracker.Builder(
//            "my-selection-id",
//            recyclerView,
//            StableIdKeyProvider(recyclerView),
//            MyDetailsLookup(recyclerView),
//            StorageStrategy.createLongStorage())
//            .withOnItemActivatedListener(myItemActivatedListener)
//            .build()
//
//        list.addOnItemTouchListener( object : RecyclerView.OnItemTouchListener {
//            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
//            }
//
//            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
//            }
//
//            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
//            }
//        })

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                imagesViewModel.imagesFlow.collect { images ->
                    adapter.submitList(images)
                }
            }
        }
        imagesViewModel.permissionNeededForDelete.observe(viewLifecycleOwner) { intentSender ->
            intentSender?.let {
                // On Android 10+, if the app doesn't have permission to modify
                // or delete an item, it returns an `IntentSender` that we can
                // use here to prompt the user to grant permission to delete (or modify)
                // the image.
                startIntentSenderForResult(
                    intentSender, DELETE_PERMISSION_REQUEST, null, 0, 0, 0, null
                )
            }
        }

        ModulePreferences.setOnPreferenceChangeListener(object :
            ModulePreferences.PreferencesChangeListener {
            override val lifecycle = getLifecycle()
            override fun onPreferencesChanged(isNotifyService: Boolean) {
                imagesViewModel.contentObserver = null
                dispatchRequestPermissions(requiredPermissions, savedInstanceState)
            }
        })

        prepareTransitions()
        if (pagerViewModel.isFromPager) {
            postponeEnterTransition()
        }
        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == DELETE_PERMISSION_REQUEST) {
            imagesViewModel.deletePendingImage()
        }
    }

    /**
     * Prepares the shared element transition to the pager fragment, as well as the other transitions
     * that affect the flow.
     */
    private fun prepareTransitions() {
        // A similar mapping is set at the ImagePagerFragment with a setEnterSharedElementCallback.
        setExitSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: List<String>, sharedElements: MutableMap<String, View>
            ) {
                // Locate the ViewHolder for the clicked position.
                val selectedViewHolder =
                    list.findViewHolderForAdapterPosition(pagerViewModel.currentPosition) ?: return

                // Map the first shared element name to the child ImageView.
                sharedElements[names[0]] = selectedViewHolder.itemView.findViewById(R.id.image)
            }
        })
    }

    override fun onRequestPermissionsSuccess(
        permissions: Set<String>, savedInstanceState: Bundle?
    ) {
        super.onRequestPermissionsSuccess(permissions, savedInstanceState)
        if (savedInstanceState == null) {
                imagesViewModel.loadImages()
            val navController = findNavController()
            pagerViewModel.setDestinationChangedListener(navController)
            if (pagerViewModel.isFromPager) {
                pagerViewModel.isFromPager = false
                scrollToPosition(pagerViewModel.currentPosition)
            }
        }
    }

    /**
     * Scrolls the recycler view to show the last viewed item in the grid. This is important when
     * navigating back from the grid.
     */
    private fun scrollToPosition(position: Int) {
        list.doOnPreDraw {
            val layoutManager = list.layoutManager as? LinearLayoutManager ?: return@doOnPreDraw
            val viewAtPosition = layoutManager.findViewByPosition(position)
            // Scroll to position if the view for the current position is null (not currently part of
            // layout manager children), or it's not completely visible.
            if (viewAtPosition == null ||
                layoutManager.isViewPartiallyVisible(viewAtPosition, false, true)
            ) {
                if (position >= layoutManager.findLastCompletelyVisibleItemPosition()) {
                    layoutManager.scrollToPosition(position)
                } else {
                    layoutManager.scrollToPositionWithOffset(position, list.paddingTop)
                }
            }
        }
    }

    companion object {
        /**
         * Code used with [IntentSender] to request user permission to delete an image with scoped storage.
         */
        private const val DELETE_PERMISSION_REQUEST = 0x1033
    }
}
