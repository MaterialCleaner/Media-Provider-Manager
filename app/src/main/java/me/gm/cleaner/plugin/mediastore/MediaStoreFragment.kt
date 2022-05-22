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

package me.gm.cleaner.plugin.mediastore

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.SelectionTracker.SelectionPredicate
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import me.gm.cleaner.plugin.BuildConfig
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.app.InfoDialog
import me.gm.cleaner.plugin.dao.ModulePreferences
import me.gm.cleaner.plugin.databinding.MediaStoreFragmentBinding
import me.gm.cleaner.plugin.ktx.*
import me.gm.cleaner.plugin.mediastore.audio.AudioFragment
import me.gm.cleaner.plugin.mediastore.downloads.DownloadsFragment
import me.gm.cleaner.plugin.mediastore.files.FilesFragment
import me.gm.cleaner.plugin.mediastore.files.MediaStoreFiles
import me.gm.cleaner.plugin.mediastore.files.MediaStoreFilesHeader
import me.gm.cleaner.plugin.mediastore.images.*
import me.gm.cleaner.plugin.mediastore.video.VideoFragment
import me.gm.cleaner.plugin.widget.FullyDraggableContainer
import me.gm.cleaner.plugin.xposed.util.MimeUtils
import rikka.recyclerview.fixEdgeEffect

abstract class MediaStoreFragment : BaseFragment(), ToolbarActionModeIndicator {
    abstract val viewModel: MediaStoreViewModel<*>
    protected lateinit var list: RecyclerView
    protected lateinit var selectionTracker: SelectionTracker<Long>
    private val detector by lazy { SelectionDetector(requireContext(), LongPressingListener()) }
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
        }
        list = binding.list
        list.adapter = adapter
        list.setHasFixedSize(true)
        list.fixEdgeEffect(false)
        list.overScrollIfContentScrollsPersistent()
        val keyProvider = StableIdKeyProvider(list)
        selectionTracker = SelectionTracker
            .Builder(
                javaClass.name, list, keyProvider, DetailsLookup(list),
                StorageStrategy.createLongStorage()
            )
            .withSelectionPredicate(object : SelectionPredicate<Long>() {
                override fun canSetStateForKey(key: Long, nextState: Boolean): Boolean {
                    val position = keyProvider.getPosition(key)
                    return canSetStateAtPosition(position, nextState)
                }

                override fun canSetStateAtPosition(position: Int, nextState: Boolean): Boolean {
                    val currentList = adapter.currentList
                    // empty onConfigurationChanged
                    return currentList.isEmpty() || currentList[position] !is MediaStoreFilesHeader
                }

                override fun canSelectMultiple() = true
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
        onBindView(binding)
        list.addLiftOnScrollListener { appBarLayout.isLifted = it }

        findNavController().addOnExitListener { _, destination, _ ->
            actionMode?.finish()
            supportActionBar?.title = destination.label
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mediasFlow.collect { medias ->
                    adapter.submitListKeepPosition(medias, list)
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
        ModulePreferences.addOnPreferenceChangeListener(object :
            ModulePreferences.PreferencesChangeListener {
            override val lifecycle = getLifecycle()
            override fun onPreferencesChanged() {
                dispatchRequestPermissions(requiredPermissions, null)
            }
        })
        return binding.root
    }

    abstract fun onCreateAdapter(): MediaStoreAdapter<MediaStoreModel, *>

    open fun onBindView(binding: MediaStoreFragmentBinding) {}

    override fun onRequestPermissionsSuccess(
        permissions: Set<String>, savedInstanceState: Bundle?
    ) {
        savedInstanceState ?: viewModel.loadMedias()
    }

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
                    val medias = selectionTracker.selection.map { selection ->
                        viewModel.medias.first { it.id == selection }
                    }
                    actionMode?.finish()
                    when (item.itemId) {
                        R.id.menu_share -> {
                            val mimeType = when (this@MediaStoreFragment) {
                                is AudioFragment -> "audio/*"
                                is ImagesFragment -> "image/*"
                                is VideoFragment -> "video/*"
                                is FilesFragment, is DownloadsFragment -> when {
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
                        R.id.menu_delete -> when {
                            medias.size == 1 -> viewModel.deleteMedia(medias.first())
                            Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q -> InfoDialog.newInstance(
                                // @see https://stackoverflow.com/questions/58283850/scoped-storage-how-to-delete-multiple-audio-files-via-mediastore
                                getString(R.string.unsupported_delete_in_bulk)
                            ).show(childFragmentManager, null)
                            else -> viewModel.deleteMedias(medias.toTypedArray())
                        }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == DELETE_PERMISSION_REQUEST) {
            viewModel.deletePendingMedia()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
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

    // PERMISSION
    override val requiredPermissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    override fun dispatchRequestPermissions(
        permissions: Array<String>, savedInstanceState: Bundle?
    ) {
        if (ModulePreferences.isShowAllMediaFiles) {
            super.dispatchRequestPermissions(permissions, savedInstanceState)
        } else {
            if (requiredPermissions.any {
                    ActivityCompat.checkSelfPermission(requireContext(), it) ==
                            PackageManager.PERMISSION_GRANTED
                }
            ) {
                dialog = MaterialAlertDialogBuilder(requireContext())
                    .setMessage(R.string.revoke_self_permission)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        }
                        startActivity(intent)
                    }
                    .show()
            }
            onRequestPermissionsSuccess(requiredPermissions.toSet(), savedInstanceState)
        }
    }

    override fun onRequestPermissionsFailure(
        shouldShowRationale: Set<String>, permanentlyDenied: Set<String>,
        savedInstanceState: Bundle?
    ) {
        if (shouldShowRationale.isNotEmpty()) {
            dialog = MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.rationale_shouldShowRationale)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    onRequestPermissions(shouldShowRationale.toTypedArray(), savedInstanceState)
                }
                .show()
        } else if (permanentlyDenied.isNotEmpty()) {
            dialog = MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.rationale_permanentlyDenied)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    }
                    startActivity(intent)
                }
                .show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (selectionTracker.hasSelection()) {
            startActionMode()
            return
        }
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.mediastore_toolbar, menu)
        menu.findItem(R.id.menu_show_all).isChecked = ModulePreferences.isShowAllMediaFiles
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_refresh -> {
            dispatchRequestPermissions(requiredPermissions, null)
            true
        }
        R.id.menu_show_all -> {
            val isShowAllMediaFiles = !item.isChecked
            item.isChecked = isShowAllMediaFiles
            ModulePreferences.isShowAllMediaFiles = isShowAllMediaFiles
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    companion object {
        /**
         * Code used with [IntentSender] to request user permission to delete an image with scoped storage.
         */
        private const val DELETE_PERMISSION_REQUEST = 0x1033
    }
}
