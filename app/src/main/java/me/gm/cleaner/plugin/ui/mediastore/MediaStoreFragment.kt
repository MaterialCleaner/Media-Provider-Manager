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

package me.gm.cleaner.plugin.ui.mediastore

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.children
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.SelectionTracker.SelectionPredicate
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.app.ConfirmationDialog
import me.gm.cleaner.plugin.app.InfoDialog
import me.gm.cleaner.plugin.databinding.MediaStoreFragmentBinding
import me.gm.cleaner.plugin.ktx.addOnExitListener
import me.gm.cleaner.plugin.ktx.overScrollIfContentScrollsPersistent
import me.gm.cleaner.plugin.ktx.submitListKeepPosition
import me.gm.cleaner.plugin.ui.mediastore.audio.AudioFragment
import me.gm.cleaner.plugin.ui.mediastore.files.FilesFragment
import me.gm.cleaner.plugin.ui.mediastore.files.MediaStoreFiles
import me.gm.cleaner.plugin.ui.mediastore.images.ImagesFragment
import me.gm.cleaner.plugin.ui.mediastore.video.VideoFragment
import me.gm.cleaner.plugin.util.MediaStoreCompat
import me.gm.cleaner.plugin.util.MediaStoreCompat.DELETE_PERMISSION_REQUEST
import me.gm.cleaner.plugin.util.PermissionUtils
import me.gm.cleaner.plugin.util.RequesterFragment
import me.gm.cleaner.plugin.xposed.util.MimeUtils
import me.zhanghai.android.fastscroll.ItemsHeightsObserver
import me.zhanghai.android.fastscroll.PreciseRecyclerViewHelper
import rikka.recyclerview.fixEdgeEffect
import java.lang.ref.WeakReference
import java.util.function.Supplier

abstract class MediaStoreFragment : BaseFragment(), ToolbarActionModeIndicator {
    protected abstract val viewModel: MediaStoreViewModel<*>
    protected abstract val requesterFragmentClass: Class<out MediaPermissionsRequesterFragment>
    protected lateinit var selectionTracker: SelectionTracker<Long>
    private var actionMode: ActionMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val binding = MediaStoreFragmentBinding.inflate(inflater)

        val adapter = onCreateAdapter().apply {
            setHasStableIds(true)
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
        val list = binding.list
        liftOnScrollTargetView = WeakReference(list)
        list.adapter = adapter
        list.setHasFixedSize(true)
        list.fixEdgeEffect(false)
        list.overScrollIfContentScrollsPersistent()
        selectionTracker = SelectionTracker
            .Builder(
                /* selectionId = */ javaClass.name,
                list,
                StableIdKeyProvider(adapter),
                DetailsLookup(list),
                StorageStrategy.createLongStorage()
            )
            .withSelectionPredicate(object : SelectionPredicate<Long>() {
                override fun canSetStateForKey(key: Long, nextState: Boolean): Boolean =
                    !nextState || viewModel.medias.any { it.id == key }

                override fun canSetStateAtPosition(position: Int, nextState: Boolean): Boolean {
                    if (position == RecyclerView.NO_POSITION) {
                        return false
                    }
                    val currentList = adapter.currentList
                    // empty onConfigurationChanged
                    return currentList.isEmpty() || currentList[position] !is MediaStoreHeader
                }

                override fun canSelectMultiple(): Boolean = true
            })
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
        onBindView(binding, list, adapter)

        findNavController().addOnExitListener { _, destination, _ ->
            actionMode?.finish()
            supportActionBar?.title = destination.label
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mediasFlow.collect { medias ->
                    val mediaIds = medias.map { it.id }.toSet()
                    val deletedItems =
                        selectionTracker.selection.filterNot { mediaIds.contains(it) }
                    deletedItems.forEach { key ->
                        selectionTracker.deselect(key)
                    }
                    adapter.submitListKeepPosition(medias, list)
                }
            }
        }
        if (savedInstanceState == null) {
            PermissionUtils.requestPermissions(
                childFragmentManager, requesterFragmentClass.newInstance()
            )
        }
        return binding.root
    }

    abstract fun onCreateAdapter(): MediaStoreAdapter

    open fun onBindView(
        binding: MediaStoreFragmentBinding,
        list: RecyclerView,
        adapter: MediaStoreAdapter
    ) {
    }

    class MediaStoreRecyclerViewHelper(
        list: RecyclerView, currentListSupplier: Supplier<List<MediaStoreModel>>
    ) : PreciseRecyclerViewHelper(
        list, null, false, object : ItemsHeightsObserver(list, false) {
            private fun guessItemOffset(isHeader: Boolean): Int? {
                for (child in list.children) {
                    val vh = list.getChildViewHolder(child)
                    val position = vh.bindingAdapterPosition
                    if (isHeader && currentListSupplier.get()[position] is MediaStoreHeader ||
                        !isHeader && currentListSupplier.get()[position] !is MediaStoreHeader
                    ) {
                        return child.measuredHeight
                    }
                }
                return null
            }

            override fun guessItemOffsetAt(position: Int): Int? = try {
                if (currentListSupplier.get()[position] is MediaStoreHeader) {
                    guessItemOffset(true)
                } else {
                    guessItemOffset(false)
                }
            } catch (e: IndexOutOfBoundsException) {
                super.guessItemOffsetAt(position)
            }
        }
    )

    fun startActionMode() {
        if (!isInActionMode()) {
            val activity = requireActivity() as AppCompatActivity
            actionMode = activity.startToolbarActionMode(object : ActionMode.Callback {
                override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                    mode.menuInflater.inflate(R.menu.mediastore_actionmode, menu)
                    return true
                }

                override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false
                override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                    val medias = selectionTracker.selection.mapNotNull { selection ->
                        viewModel.medias.firstOrNull { it.id == selection }
                    }
                    if (medias.isEmpty()) {
                        return true
                    }
                    when (item.itemId) {
                        R.id.menu_share -> {
                            val mimeType = when (this@MediaStoreFragment) {
                                is AudioFragment -> "audio/*"
                                is ImagesFragment -> "image/*"
                                is VideoFragment -> "video/*"
                                is FilesFragment -> when {
                                    medias.all { MimeUtils.isAudioMimeType((it as MediaStoreFiles).mimeType) } -> "audio/*"
                                    medias.all { MimeUtils.isImageMimeType((it as MediaStoreFiles).mimeType) } -> "image/*"
                                    medias.all { MimeUtils.isVideoMimeType((it as MediaStoreFiles).mimeType) } -> "video/*"
                                    else -> "*/*"
                                }

                                else -> throw UnsupportedOperationException()
                            }
                            val sendIntent = if (medias.size == 1) {
                                Intent(Intent.ACTION_SEND)
                                    .setType(mimeType)
                                    .putExtra(Intent.EXTRA_STREAM, medias.first().contentUri)
                                    .putExtra(Intent.EXTRA_TEXT, medias.first().displayName)
                            } else {
                                val mediaUris = ArrayList<Uri>(medias.size)
                                medias.mapTo(mediaUris) { it.contentUri }
                                Intent(Intent.ACTION_SEND_MULTIPLE)
                                    .setType(mimeType)
                                    .putParcelableArrayListExtra(Intent.EXTRA_STREAM, mediaUris)
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            try {
                                startActivity(shareIntent)
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }

                        R.id.menu_delete -> deleteSelectedMedias(true)

                        else -> return false
                    }
                    return true
                }

                override fun onDestroyActionMode(mode: ActionMode) {
                    selectionTracker.clearSelection()
                    actionMode = null
                }
            })
        }
        actionMode?.title = selectionTracker.selection.size().toString()
    }

    private fun deleteSelectedMedias(allowBulkDelete: Boolean) {
        lifecycleScope.launch {
            val medias = selectionTracker.selection.mapNotNull { selection ->
                viewModel.medias.firstOrNull { it.id == selection }
            }
            when {
                medias.size == 1 -> MediaStoreCompat.delete(
                    this@MediaStoreFragment, medias.single().contentUri
                )

                Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q -> InfoDialog
                    // @see https://stackoverflow.com/questions/58283850/scoped-storage-how-to-delete-multiple-audio-files-via-mediastore
                    .newInstance(getString(R.string.unsupported_delete_in_bulk))
                    .show(childFragmentManager, null)

                else -> if (allowBulkDelete) {
                    MediaStoreCompat.delete(
                        this@MediaStoreFragment, medias.map { it.contentUri }
                    )
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == DELETE_PERMISSION_REQUEST) {
            deleteSelectedMedias(false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::selectionTracker.isInitialized) {
            selectionTracker.onSaveInstanceState(outState)
        }
    }

    override fun isInActionMode(): Boolean = actionMode != null

    abstract class MediaPermissionsRequesterFragment : RequesterFragment() {
        private val parentFragment: MediaStoreFragment by lazy {
            requireParentFragment() as MediaStoreFragment
        }
        private val viewModel: MediaStoreViewModel<*> by lazy {
            parentFragment.viewModel
        }

        override fun onRequestPermissionsSuccess(permissions: Set<String>) {
            viewModel.load()
        }

        override fun onRequestPermissionsFailure(
            shouldShowRationale: Set<String>, denied: Set<String>
        ) {
            if (shouldShowRationale.isNotEmpty()) {
                ConfirmationDialog
                    .newInstance(getString(R.string.rationale_shouldShowRationale))
                    .apply {
                        addOnPositiveButtonClickListener {
                            onRequestPermissions(
                                shouldShowRationale.toTypedArray()
                            )
                        }
                    }
                    .show(childFragmentManager, null)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (selectionTracker.hasSelection()) {
            startActionMode()
            return
        }
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.mediastore_toolbar, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.menu_refresh -> {
            viewModel.load()
            true
        }

        R.id.menu_scan_external_storage -> {
            MediaScannerConnection.scanFile(
                requireContext(), arrayOf(Environment.getExternalStorageDirectory().path),
                null, null
            )
            true
        }

        else -> super.onOptionsItemSelected(item)
    }
}
