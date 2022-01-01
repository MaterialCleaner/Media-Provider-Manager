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

package me.gm.cleaner.plugin.module.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.preference.*
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.ktx.addLiftOnScrollListener
import me.gm.cleaner.plugin.ktx.fitsSystemWindowInsetBottom
import me.gm.cleaner.plugin.ktx.overScrollIfContentScrollsPersistent
import me.gm.cleaner.plugin.ktx.setObjectField
import me.gm.cleaner.plugin.module.BinderViewModel
import rikka.recyclerview.fixEdgeEffect

@SuppressLint("RestrictedApi")
abstract class AbsSettingsFragment : PreferenceFragmentCompat() {
    protected val binderViewModel: BinderViewModel by activityViewModels()
    abstract val who: Int
    protected val remoteSp by lazy { BinderSpImpl(binderViewModel, who) }

    open fun onCreatePreferenceManager(savedInstanceState: Bundle?) =
        object : PreferenceManager(context) {
            override fun getSharedPreferences() = remoteSp
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setObjectField(
            onCreatePreferenceManager(savedInstanceState), PreferenceFragmentCompat::class.java
        )
    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater, parent: ViewGroup, savedInstanceState: Bundle?
    ): RecyclerView {
        val list = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        list.setHasFixedSize(true)
        list.fixEdgeEffect(false)
        list.overScrollIfContentScrollsPersistent()
        list.addLiftOnScrollListener {
            val appBarLayout: AppBarLayout = requireActivity().findViewById(R.id.toolbar_container)
            appBarLayout.isLifted = it
        }
        list.fitsSystemWindowInsetBottom()
        return list
    }

    override fun onCreateAdapter(preferenceScreen: PreferenceScreen?): RecyclerView.Adapter<*> {
        return object : PreferenceGroupAdapter(preferenceScreen) {
            override fun onBindViewHolder(holder: PreferenceViewHolder, position: Int) {
                val preference = getItem(position)
                preference.onBindViewHolder(holder)
                onBindPreferencesViewHolder(holder, preference)
            }
        }
    }

    open fun onBindPreferencesViewHolder(holder: PreferenceViewHolder, preference: Preference) {}
}
