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

package me.gm.cleaner.plugin.home

import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import me.gm.cleaner.plugin.app.BaseActivity
import me.gm.cleaner.plugin.databinding.HomeActivityBinding
import rikka.recyclerview.fixEdgeEffect
import rikka.widget.borderview.BorderView.OnBorderVisibilityChangedListener

abstract class HomeActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = HomeActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val list = binding.list
        list.adapter = HomeAdapter(this)
        list.layoutManager = GridLayoutManager(this, 1)
        list.setHasFixedSize(true)
        list.fixEdgeEffect()
        list.borderViewDelegate.borderVisibilityChangedListener =
            OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
                appBarLayout?.isRaised = !top
            }
    }
}
