/*
 * Copyright 2022 Green Mushroom
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

package me.gm.cleaner.plugin.mediastore.audio

import android.Manifest
import android.os.Build
import androidx.fragment.app.viewModels
import me.gm.cleaner.plugin.mediastore.files.FilesFragment

class AudioFragment : FilesFragment() {
    override val viewModel: AudioViewModel by viewModels()
    override val requesterFragmentClass: Class<out MediaPermissionsRequesterFragment> =
        AudioPermissionsRequesterFragment::class.java

//    override fun onCreateAdapter() = AudioAdapter(this)

    class AudioPermissionsRequesterFragment : MediaPermissionsRequesterFragment() {
        override val requiredPermissions: Array<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
    }
}
