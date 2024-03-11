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

package me.gm.cleaner.plugin.ui.module.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.commit
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.databinding.SettingsFragmentStubBinding
import me.gm.cleaner.plugin.ui.module.ModuleFragment

class SettingsFragmentStub : ModuleFragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ) = if (!binderViewModel.pingBinder()) {
        super.onCreateView(inflater, container, savedInstanceState)
    } else {
        SettingsFragmentStubBinding.inflate(layoutInflater).root.also {
            savedInstanceState ?: childFragmentManager.commit {
                replace(R.id.settings, SettingsFragment())
            }
        }
    }
}
