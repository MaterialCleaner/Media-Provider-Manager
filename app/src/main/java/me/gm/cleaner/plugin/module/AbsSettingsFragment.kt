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

package me.gm.cleaner.plugin.module

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.ktx.addLiftOnScrollListener
import me.gm.cleaner.plugin.ktx.overScrollIfContentScrollsPersistent
import me.gm.cleaner.plugin.ktx.setObjectField
import rikka.recyclerview.fixEdgeEffect

open class AbsSettingsFragment : PreferenceFragmentCompat() {
    @SuppressLint("RestrictedApi")
    open fun onCreatePreferenceManager() = object : PreferenceManager(context) {
        override fun getSharedPreferences(): SharedPreferences {
            // TODO: MMKV
            if (false) {
                return super.getSharedPreferences()
            }
            return super.getSharedPreferences()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setObjectField(onCreatePreferenceManager(), PreferenceFragmentCompat::class.java)
    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater, parent: ViewGroup, savedInstanceState: Bundle?
    ): RecyclerView {
        val list = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        list.fixEdgeEffect(false)
        list.overScrollIfContentScrollsPersistent()
        list.addLiftOnScrollListener {
            val appBarLayout: AppBarLayout = requireActivity().findViewById(R.id.toolbar_container)
            appBarLayout.isLifted = it
        }
        return list
    }
}
