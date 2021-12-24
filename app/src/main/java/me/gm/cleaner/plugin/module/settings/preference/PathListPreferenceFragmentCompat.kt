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

package me.gm.cleaner.plugin.module.settings.preference

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.preference.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.ktx.addLiftOnScrollListener
import me.gm.cleaner.plugin.ktx.overScrollIfContentScrollsPersistent
import rikka.recyclerview.fixEdgeEffect
import java.text.Collator

class PathListPreferenceFragmentCompat : PreferenceDialogFragmentCompat(),
    PreferenceManager.OnDisplayPreferenceDialogListener, DialogPreference.TargetFragment {
    private val pathListPreference by lazy { preference as PathListPreference }
    private val adapter by lazy { PathListPreferenceAdapter(this) }
    var newValues = emptyList<String>()
        set(value) {
            val collator = Collator.getInstance()
            field = value.sortedWith { o1, o2 ->
                collator.compare(o1, o2)
            }
            adapter.submitList(field)
        }
    private val preferenceChanged
        get() = pathListPreference.values != newValues.toSet()
    var ignorePreferenceChanged = false
    private val dialog by lazy {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.quit_without_save)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                ignorePreferenceChanged = true
                onDismiss(requireDialog())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }
    val openDocumentTreeLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree(), this::onFragmentResult
    )
    private val preferences = mutableListOf<Preference>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_FullScreenDialog)
        newValues = if (savedInstanceState == null) {
            pathListPreference.values.toList()
        } else {
            if (savedInstanceState.getBoolean(SAVED_SHOWS_ALERT_DIALOG, false)) {
                dialog.show()
            }
            savedInstanceState.getStringArrayList(SAVE_STATE_VALUES)!!
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(SAVED_SHOWS_ALERT_DIALOG, dialog.isShowing)
        outState.putStringArrayList(SAVE_STATE_VALUES, ArrayList(newValues))
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        object : Dialog(requireContext(), theme) {
            override fun onBackPressed() {
                onDismiss(requireDialog())
            }
        }.apply {
            val contentView = onCreateDialogView(context)
            if (contentView != null) {
                onBindDialogView(contentView)
                setContentView(contentView)
            }
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

    @SuppressLint("RestrictedApi")
    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        val appBarLayout = view.findViewById<AppBarLayout>(R.id.toolbar_container)
        view.findViewById<Toolbar>(R.id.toolbar).apply {
            setNavigationOnClickListener { onDismiss(requireDialog()) }
            setNavigationIcon(R.drawable.ic_outline_close_24)
            SupportMenuInflater(context).inflate(R.menu.toolbar_save, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_save -> {
                        val dialog = requireDialog()
                        onClick(dialog, DialogInterface.BUTTON_POSITIVE)
                        ignorePreferenceChanged = true
                        onDismiss(requireDialog())
                    }
                    else -> return@setOnMenuItemClickListener false
                }
                true
            }
            title = pathListPreference.dialogTitle
        }

        val list = view.findViewById<RecyclerView>(R.id.list)
        list.adapter = adapter
        list.layoutManager = GridLayoutManager(requireContext(), 1)
        list.fixEdgeEffect(false)
        list.overScrollIfContentScrollsPersistent()
        list.addLiftOnScrollListener { appBarLayout.isLifted = it }
        val preference = object : EditTextPreference(requireContext()) {
            override fun getPreferenceManager() = object : PreferenceManager(requireContext()) {
                init {
                    onDisplayPreferenceDialogListener = this@PathListPreferenceFragmentCompat
                }
            }
        }.apply {
            key = TEXT_EDITOR
            dialogTitle = pathListPreference.dialogTitle
        }
        preferences.add(preference)

        view.findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            openDocumentTreeLauncher.launch(null)
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference?) {
        preference ?: return
        val f = when (preference) {
            is EditTextPreference -> {
                EditTextPreferenceDialogFragmentCompat.newInstance(preference.getKey())
            }
            is ListPreference -> {
                ListPreferenceDialogFragmentCompat.newInstance(preference.getKey())
            }
            is MultiSelectListPreference -> {
                MultiSelectListPreferenceDialogFragmentCompat.newInstance(preference.getKey())
            }
            else -> {
                throw IllegalArgumentException(
                    "Cannot display dialog for an unknown Preference type: "
                            + preference.javaClass.simpleName
                            + ". Make sure to implement onPreferenceDisplayDialog() to handle "
                            + "displaying a custom dialog for this Preference."
                )
            }
        }
        f.setTargetFragment(this, 0)
        f.show(parentFragmentManager, null)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Preference?> findPreference(key: CharSequence): T? {
        for (preference in preferences) {
            val curKey = preference.key
            if (curKey == key) {
                return preference as T
            }
        }
        return null
    }

    fun onFragmentResult(result: Uri?) {
        result ?: return
        val target = treeUriToFile(result, requireContext()) ?: return
        newValues += target.path
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (preferenceChanged && !ignorePreferenceChanged) {
            this.dialog.show()
        } else {
            super.onDismiss(dialog)
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult && preferenceChanged) {
            val newValues = newValues.toSet()
            if (pathListPreference.callChangeListener(newValues)) {
                pathListPreference.values = newValues
            }
        }
    }

    companion object {
        private const val SAVE_STATE_VALUES = "PathListPreferenceFragmentCompat.values"
        private const val SAVED_SHOWS_ALERT_DIALOG = "android:showsAlertDialog"
        const val TEXT_EDITOR = "android:textEditor"
        fun newInstance(key: String?) =
            PathListPreferenceFragmentCompat().apply { arguments = bundleOf(ARG_KEY to key) }
    }
}
