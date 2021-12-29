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

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import dagger.hilt.android.HiltAndroidApp
import me.gm.cleaner.plugin.BuildConfig
import me.gm.cleaner.plugin.dao.ModulePreferences
import org.lsposed.hiddenapibypass.HiddenApiBypass

@HiltAndroidApp
class App : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions(
                "Landroid/graphics/drawable/AdaptiveIconDrawableInjector;",
                "Landroid/os/storage/StorageVolume;",
            )
        }
        if (!BuildConfig.DEBUG) {
            AppCenter.start(
                this, "274b837f-ed2e-43ec-b36d-b08328b353ca",
                Analytics::class.java, Crashes::class.java
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        ModulePreferences.init(createDeviceProtectedStorageContext())
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
