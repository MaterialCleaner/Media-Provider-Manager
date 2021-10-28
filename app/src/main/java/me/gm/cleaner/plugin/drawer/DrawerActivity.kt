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

package me.gm.cleaner.plugin.drawer

import android.os.Bundle
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.app.BaseActivity
import me.gm.cleaner.plugin.dao.ModulePreferences
import me.gm.cleaner.plugin.databinding.DrawerActivityBinding

abstract class DrawerActivity : BaseActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private val navController by lazy { findNavController(R.id.nav_host) }
    private val appBarConfiguration by lazy {
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration(topLevelDestinationIds, drawerLayout)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DrawerActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val shouldAlterStartDestination =
            savedInstanceState == null && ModulePreferences.startDestination in topLevelDestinationIds
        if (shouldAlterStartDestination) {
            val navGraph = navController.navInflater.inflate(R.navigation.nav_graph).apply {
                setStartDestination(ModulePreferences.startDestination)
            }
            navController.graph = navGraph
        }
        ModulePreferences.isNavInitialized = true

        drawerLayout = binding.drawerLayout
        setupActionBarWithNavController(navController, appBarConfiguration)
        val navView = binding.navView
        navView.setupWithNavController(navController)
        if (shouldAlterStartDestination) {
            navView.setCheckedItem(ModulePreferences.startDestination)
        }
    }

    override fun onBackPressed() {
        when {
            drawerLayout.isOpen -> drawerLayout.close()
            navController.currentDestination!!.id in topLevelDestinationIds -> {
                ModulePreferences.isNavInitialized = false
                super.onSupportNavigateUp()
            }
            else -> super.onBackPressed()
        }
    }

    override fun onSupportNavigateUp() =
        navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()

    companion object {
        val topLevelDestinationIds = setOf(
            R.id.applist_fragment,
            R.id.usage_record_fragment,
            R.id.settings_fragment,
            R.id.audio_fragment,
            R.id.downloads_fragment,
            R.id.files_fragment,
            R.id.images_fragment,
            R.id.video_fragment,
            R.id.experiment_fragment,
            R.id.about_fragment,
        )
    }
}
