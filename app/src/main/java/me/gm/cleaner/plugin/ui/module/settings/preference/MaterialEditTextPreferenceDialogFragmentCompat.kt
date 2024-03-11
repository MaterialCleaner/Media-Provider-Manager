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

package me.gm.cleaner.plugin.ui.module.settings.preference

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.preference.EditTextPreferenceDialogFragmentCompat
import com.google.android.material.dialog.InsetDialogOnTouchListener
import com.google.android.material.dialog.MaterialDialogs
import me.gm.cleaner.plugin.ktx.createMaterialAlertDialogThemedContext
import me.gm.cleaner.plugin.ktx.materialDialogBackgroundDrawable
import me.gm.cleaner.plugin.ktx.materialDialogBackgroundInsets

class MaterialEditTextPreferenceDialogFragmentCompat : EditTextPreferenceDialogFragmentCompat() {

    @SuppressLint("RestrictedApi")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val alertDialog = super.onCreateDialog(savedInstanceState)
        val window = alertDialog.window!!
        /* {@link Window#getDecorView()} should be called before any changes are made to the Window
         * as it locks in attributes and affects layout. */
        val decorView = window.decorView
        val context = requireContext().createMaterialAlertDialogThemedContext()
        val background = context.materialDialogBackgroundDrawable()
        background.elevation = ViewCompat.getElevation(decorView)
        val backgroundInsets = context.materialDialogBackgroundInsets()

        val insetDrawable = MaterialDialogs.insetDrawable(background, backgroundInsets)
        window.setBackgroundDrawable(insetDrawable)
        decorView.setOnTouchListener(InsetDialogOnTouchListener(alertDialog, backgroundInsets))
        return alertDialog
    }

    companion object {
        fun newInstance(key: String?): MaterialEditTextPreferenceDialogFragmentCompat {
            val fragment = MaterialEditTextPreferenceDialogFragmentCompat()
            val b = Bundle(1)
            b.putString(ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }
    }
}
