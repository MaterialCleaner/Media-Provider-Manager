/*
 * Copyright 2023 Green Mushroom
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

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.os.bundleOf
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TextSelectableInfoDialog : AppCompatDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog =
        MaterialAlertDialogBuilder(requireContext(), theme)
            .setMessage(requireArguments().getString(KEY_MESSAGE))
            .setPositiveButton(android.R.string.ok, null)
            .create().apply {
                setOnShowListener {
                    window?.findViewById<TextView>(android.R.id.message)?.apply {
                        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                        setTextIsSelectable(true)
                    }
                }
            }

    companion object {
        private const val KEY_MESSAGE: String = "me.gm.cleaner.key.message"
        fun newInstance(message: String): TextSelectableInfoDialog =
            TextSelectableInfoDialog().apply { arguments = bundleOf(KEY_MESSAGE to message) }
    }
}
