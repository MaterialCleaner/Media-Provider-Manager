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
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.app.SharedElementCallback
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.app.InfoDialog
import me.gm.cleaner.plugin.dao.ModulePreferences
import me.gm.cleaner.plugin.databinding.ImagesFragmentBinding
import me.gm.cleaner.plugin.ktx.addLiftOnScrollListener
import me.gm.cleaner.plugin.ktx.addOnExitListener
import me.gm.cleaner.plugin.ktx.overScrollIfContentScrollsPersistent
import me.gm.cleaner.plugin.mediastore.*
import me.gm.cleaner.plugin.mediastore.StableIdKeyProvider
import me.gm.cleaner.plugin.mediastore.imagepager.ImagePagerFragment
import me.gm.cleaner.plugin.widget.FullyDraggableContainer
import me.gm.cleaner.plugin.widget.makeSnackbarWithFullyDraggableContainer
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import rikka.recyclerview.fixEdgeEffect

class ImagesFragment : MediaStoreFragment(), ToolbarActionModeIndicator {
    private val viewModel: ImagesViewModel by viewModels()
    private lateinit var list: RecyclerView
    private lateinit var keyProvider: StableIdKeyProvider
    private lateinit var selectionTracker: SelectionTracker<Long>
    private val detector by lazy { SelectionDetector(requireContext(), LongPressingListener()) }
    private var actionMode: ActionMode? = null
    var lastPosition = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = ImagesFragmentBinding.inflate(inflater)

        val adapter = ImagesAdapter(this).apply {
            setHasStableIds(true)
        }
        list = binding.list
        list.adapter = adapter
        list.layoutManager = GridLayoutManager(requireContext(), 3)
        list.setHasFixedSize(true)
        list.fixEdgeEffect(false)
        list.overScrollIfContentScrollsPersistent()
        list.addLiftOnScrollListener { appBarLayout.isLifted = it }

        keyProvider = StableIdKeyProvider(list)
        selectionTracker = SelectionTracker.Builder(
            ImagesAdapter::class.java.simpleName, list, keyProvider,
            DetailsLookup(list), StorageStrategy.createLongStorage()
        )
            .withSelectionPredicate(SelectionPredicates.createSelectAnything())
            .build()
        selectionTracker.onRestoreInstanceState(savedInstanceState)
        selectionTracker.addObserver(object : SelectionTracker.SelectionObserver<Long>() {
            override fun onSelectionChanged() {
                if (selectionTracker.hasSelection()) {
                    startActionMode()
                } else {
                    actionMode?.finish()
                }
            }
        })
        adapter.selectionTracker = selectionTracker
        // Build FastScroller after SelectionTracker so that we can intercept SelectionTracker's OnItemTouchListener.
        FastScrollerBuilder(list)
            .useMd2Style()
            .build()

        findNavController().addOnExitListener { _, destination, _ ->
            actionMode?.finish()
            supportActionBar?.title = destination.label
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.imagesFlow.collect { images ->
                    adapter.submitList(images)
                }
            }
        }
        viewModel.permissionNeededForDelete.observe(viewLifecycleOwner) { intentSender ->
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
                dispatchRequestPermissions(requiredPermissions, savedInstanceState)
            }
        })
        return binding.root
    }

    // TODO: move this fun to MediaStoreFragment, make MediaStoreFragment implement ActionMode.Callback,
    //  override onActionItemClicked in this fragment
    private fun startActionMode() {
        if (!isInActionMode()) {
            val activity = requireActivity() as AppCompatActivity
            actionMode = activity.startToolbarActionMode(object : ActionMode.Callback {
                override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                    mode.menuInflater.inflate(R.menu.mediastore_actionmode, menu)
                    return true
                }

                override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false
                override fun onActionItemClicked(mode: ActionMode, item: MenuItem) =
                    when (item.itemId) {
                        R.id.menu_delete -> {
                            val images = selectionTracker.selection.map {
                                viewModel.images[keyProvider.getPosition(it)]
                            }
                            selectionTracker.clearSelection()
                            when {
                                images.size == 1 -> viewModel.deleteImage(images.single())
                                Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q -> InfoDialog.newInstance(
                                    // @see https://stackoverflow.com/questions/58283850/scoped-storage-how-to-delete-multiple-audio-files-via-mediastore
                                    getString(R.string.unsupported_delete_in_bulk)
                                ).show(childFragmentManager, null)
                                else -> viewModel.deleteImages(images.toTypedArray())
                            }
                            true
                        }
                        else -> false
                    }

                override fun onDestroyActionMode(mode: ActionMode) {
                    selectionTracker.clearSelection()
                    actionMode = null
                }
            })
        }
        actionMode?.title = selectionTracker.selection.size().toString()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == DELETE_PERMISSION_REQUEST &&
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q
        ) {
            viewModel.deletePendingImage()
        }
    }

    override fun onRequestPermissionsSuccess(
        permissions: Set<String>, savedInstanceState: Bundle?
    ) {
        super.onRequestPermissionsSuccess(permissions, savedInstanceState)
        if (savedInstanceState == null) {
            viewModel.loadImages()
        }
        setFragmentResultListener(ImagePagerFragment::class.java.simpleName) { _, bundle ->
            val position = bundle.getInt(ImagePagerFragment.KEY_POSITION)
            lastPosition = position
            prepareTransitions()
            savedInstanceState ?: postponeEnterTransition()
            scrollToPosition(position)
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
                    list.findViewHolderForAdapterPosition(lastPosition) ?: return

                // Map the first shared element name to the child ImageView.
                sharedElements[names[0]] = selectedViewHolder.itemView.findViewById(R.id.image)
            }
        })
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
                val lastPosition = layoutManager.findLastCompletelyVisibleItemPosition()
                if (position >= lastPosition && lastPosition - layoutManager.findFirstCompletelyVisibleItemPosition() > 0) {
                    layoutManager.scrollToPosition(position)
                } else {
                    layoutManager.scrollToPositionWithOffset(position, list.paddingTop)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            inflater.inflate(R.menu.images_toolbar, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_validation -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                lifecycleScope.launch {
                    if (!viewModel.validateAsync().await()) {
                        makeSnackbarWithFullyDraggableContainer(
                            { requireActivity().findViewById(R.id.fully_draggable_container) },
                            requireView(), R.string.validation_nop, Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                throw UnsupportedOperationException()
            }
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (selectionTracker.hasSelection()) {
            startActionMode()
        }
        requireActivity().findViewById<FullyDraggableContainer>(R.id.fully_draggable_container)
            .addInterceptTouchEventListener { _, ev ->
                detector.onTouchEvent(ev)
                detector.isSelecting && selectionTracker.hasSelection()
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::selectionTracker.isInitialized) {
            selectionTracker.onSaveInstanceState(outState)
        }
    }

    override fun isInActionMode() = actionMode != null

    companion object {
        /**
         * Code used with [IntentSender] to request user permission to delete an image with scoped storage.
         */
        private const val DELETE_PERMISSION_REQUEST = 0x1033
    }
}
