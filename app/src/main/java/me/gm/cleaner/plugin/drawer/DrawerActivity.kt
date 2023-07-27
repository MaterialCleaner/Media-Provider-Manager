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

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.internal.NavigationMenuPresenter
import com.google.android.material.internal.NavigationMenuView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.transition.platform.Hold
import me.gm.cleaner.plugin.BuildConfig
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.app.BaseActivity
import me.gm.cleaner.plugin.dao.RootPreferences
import me.gm.cleaner.plugin.databinding.DrawerActivityBinding
import me.gm.cleaner.plugin.ktx.getObjectField
import me.gm.cleaner.plugin.ktx.overScrollIfContentScrollsPersistent
import me.gm.cleaner.plugin.mediastore.ToolbarActionModeIndicator
import me.gm.cleaner.plugin.module.BinderViewModel
import me.gm.cleaner.plugin.xposed.util.MimeUtils
import rikka.recyclerview.fixEdgeEffect

abstract class DrawerActivity : BaseActivity() {
    private val viewModel: BinderViewModel by viewModels()
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController
    private val appBarConfiguration by lazy {
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration(topLevelDestinationIds, drawerLayout)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DrawerActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        navController = findNavController(R.id.nav_host)
        // NavController's backend
        val action = intent.action
        val shouldAlterStartDestination = savedInstanceState == null &&
                (action != Intent.ACTION_MAIN ||
                        RootPreferences.startDestination in topLevelDestinationIds)
        if (shouldAlterStartDestination) {
            when (action) {
                "me.gm.cleaner.plugin.intent.action.AUDIO" ->
                    RootPreferences.startDestination = R.id.audio_fragment

                "me.gm.cleaner.plugin.intent.action.FILES" ->
                    RootPreferences.startDestination = R.id.files_fragment

                "me.gm.cleaner.plugin.intent.action.IMAGES" ->
                    RootPreferences.startDestination = R.id.images_fragment

                "me.gm.cleaner.plugin.intent.action.VIDEO" ->
                    RootPreferences.startDestination = R.id.video_fragment
            }
            val navGraph = navController.navInflater.inflate(R.navigation.nav_graph).apply {
                val startDestId = if (action == Intent.ACTION_VIEW) {
                    when {
                        MimeUtils.isAudioMimeType(intent.type) -> R.id.audio_fragment
                        MimeUtils.isImageMimeType(intent.type) -> R.id.image_pager_fragment
                        MimeUtils.isVideoMimeType(intent.type) -> R.id.video_player_fragment
                        else -> throw IllegalArgumentException(intent.type)
                    }
                } else {
                    RootPreferences.startDestination
                }
                setStartDestination(startDestId)
            }
            val args = if (action == Intent.ACTION_VIEW) {
                bundleOf(
                    "uris" to arrayOf(intent.data),
                    "displayNames" to arrayOf(intent.data?.lastPathSegment ?: ""),
                )
            } else {
                null
            }
            navController.setGraph(navGraph, args)
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id in topLevelDestinationIds) {
                RootPreferences.startDestination = destination.id

                supportFragmentManager.findFragmentById(R.id.nav_host)
                    ?.childFragmentManager?.fragments?.forEach {
                        if (it.exitTransition is Hold) {
                            it.exitTransition = null
                        }
                    }
            }
        }

        // NavController's frontend
        drawerLayout = binding.drawerLayout
        setupActionBarWithNavController(navController, appBarConfiguration)
        val navView = binding.navView
        navView.setupWithNavController(navController)
        customizeNavViewStyle(navView)
        if (shouldAlterStartDestination) {
            navView.setCheckedItem(RootPreferences.startDestination)
        }

        navView.getHeaderView(0).findViewById<TextView>(R.id.status).setText(
            when {
                !viewModel.pingBinder() -> R.string.not_active
                viewModel.moduleVersion != BuildConfig.VERSION_CODE -> R.string.restart_system
                else -> R.string.active
            }
        )
    }

    @SuppressLint("RestrictedApi")
    private fun customizeNavViewStyle(navView: NavigationView) {
        val presenter = navView.getObjectField<NavigationMenuPresenter>()
        val menuView = presenter.getMenuView(navView) as NavigationMenuView
        menuView.fixEdgeEffect(false)
        menuView.overScrollIfContentScrollsPersistent()
    }

    override fun onBackPressed() {
        when {
            drawerLayout.isOpen -> drawerLayout.close()
            navController.currentDestination?.id in topLevelDestinationIds &&
                    supportFragmentManager.findFragmentById(R.id.nav_host)
                        ?.childFragmentManager?.fragments?.first()?.let {
                            it !is ToolbarActionModeIndicator || !it.isInActionMode()
                        } == true -> super.onSupportNavigateUp()

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
            R.id.playground_fragment,
            R.id.about_fragment,
        )
    }
}
