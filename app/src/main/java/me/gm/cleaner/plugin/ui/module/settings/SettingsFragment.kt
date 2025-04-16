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

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.transition.platform.Hold
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.ktx.mediumAnimTime
import me.gm.cleaner.plugin.provider.ClipBoardUtils
import me.gm.cleaner.plugin.provider.xSpUtils

class SettingsFragment : AbsSettingsFragment() {
    override val who: Int
        get() = R.xml.root_preferences

    var enterKey: String? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val usageRecord = getString(R.string.usage_record_key)
        findPreference<SwitchPreferenceCompat>(usageRecord)?.isChecked =
            remoteSp.getBoolean(usageRecord, true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = super.onCreateView(inflater, container, savedInstanceState).also {
        parentFragment?.setFragmentResultListener(TemplatesFragment::class.java.name) { _, bundle ->
            enterKey = bundle.getString(TemplatesFragment.KEY)
            parentFragment?.postponeEnterTransition()
        }
    }

    override fun onBindPreferencesViewHolder(holder: PreferenceViewHolder, preference: Preference) {
        val itemView = holder.itemView
        when (preference.key) {
            getString(R.string.template_management_key) -> {
                itemView.transitionName = preference.key
                itemView.setOnClickListener {
                    val navController = findNavController()
                    if (navController.currentDestination?.id != R.id.settings_fragment) {
                        return@setOnClickListener
                    }
                    enterKey = preference.key
                    parentFragment?.exitTransition = Hold().apply {
                        duration = requireContext().mediumAnimTime
                    }

                    val direction = SettingsFragmentStubDirections.actionSettingsToTemplates()
                    val extras = FragmentNavigatorExtras(it to it.transitionName)
                    navController.navigate(direction, extras)
                }
                parentFragment?.startPostponedEnterTransition()
            }

            getString(R.string.reload_data_key) -> {
                itemView.transitionName = preference.key
                itemView.setOnClickListener {
                    //强制重载Host侧的数据
                    binderViewModel.refreshSp()
                    Toast.makeText(requireContext(), R.string.reload_ok, Toast.LENGTH_SHORT).show();
                }
            }

            getString(R.string.backup_key) -> {
                itemView.transitionName = preference.key
                itemView.setOnClickListener {
                    // 备份到剪切板 按钮 被按下

                    //读取SP数据并复制到剪切板
                    ClipBoardUtils.setClipBoardDataString(
                        requireContext(), xSpUtils.backupSharedPreferences(requireContext())
                    );
                    Toast.makeText(requireContext(), R.string.backup_ok, Toast.LENGTH_SHORT).show();
                }
            }

            getString(R.string.restore_key) -> {
                itemView.transitionName = preference.key
                itemView.setOnClickListener {
                    // 从剪切板恢复 按钮 被按下
                    AlertDialog.Builder(requireContext())
                        .setTitle(R.string.restore_title)
                        .setMessage("确定要从剪切板恢复数据？")
                        .setNegativeButton("否", null)
                        .setPositiveButton("是", object : DialogInterface.OnClickListener {
                            override fun onClick(dialogInterface: DialogInterface?, i: Int) {
                                //从剪切板读取数据
                                var clipData =
                                    ClipBoardUtils.getClipBoardDataString(requireContext());
                                if (xSpUtils.restoreSharedPreferences(requireContext(), clipData)) {
                                    binderViewModel.refreshSp();
                                    //如果恢复成功，则提示成功
                                    Toast.makeText(
                                        requireContext(),
                                        R.string.restore_ok,
                                        Toast.LENGTH_SHORT
                                    )
                                        .show();
                                } else {
                                    //如果恢复失败，则提示成功
                                    Toast.makeText(
                                        requireContext(),
                                        R.string.restore_failed,
                                        Toast.LENGTH_SHORT
                                    )
                                        .show();
                                }
                            }
                        }).create().show()
                }
            }
        }
    }
}
