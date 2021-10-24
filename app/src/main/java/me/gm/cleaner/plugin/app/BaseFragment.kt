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

package me.gm.cleaner.plugin.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.AppBarLayout
import me.gm.cleaner.plugin.R

abstract class BaseFragment : Fragment() {
    val supportActionBar: ActionBar?
        get() = (requireActivity() as AppCompatActivity).supportActionBar
    val appBarLayout: AppBarLayout
        get() {
            val appBarLayout: AppBarLayout = requireActivity().findViewById(R.id.toolbar_container)
            appBarLayout.setLiftable(true)
            return appBarLayout
        }
    protected lateinit var dialog: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        savedInstanceState?.run {
            if (::dialog.isInitialized && getBoolean(SAVED_SHOWS_DIALOG, false)) {
                dialog.show()
            }
        }
        return container!!
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::dialog.isInitialized) {
            outState.putBoolean(SAVED_SHOWS_DIALOG, dialog.isShowing)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::dialog.isInitialized && dialog.isShowing) {
            dialog.dismiss()
        }
    }

    companion object {
        private const val SAVED_SHOWS_DIALOG = "android:showsDialog"
    }
}
