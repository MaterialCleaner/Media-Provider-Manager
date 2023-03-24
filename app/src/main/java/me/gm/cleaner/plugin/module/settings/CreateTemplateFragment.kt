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
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.SharedElementCallback
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.navigation.fragment.navArgs
import androidx.preference.EditTextPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.gson.Gson
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.dao.JsonSharedPreferencesImpl
import me.gm.cleaner.plugin.ktx.colorSurface
import me.gm.cleaner.plugin.ktx.mediumAnimTime
import me.gm.cleaner.plugin.model.Template
import me.gm.cleaner.plugin.model.Templates
import me.gm.cleaner.plugin.module.settings.preference.*
import me.gm.cleaner.plugin.widget.makeSnackbarWithFullyDraggableContainer
import kotlin.collections.set

class CreateTemplateFragment : AbsSettingsFragment() {
    override val who: Int
        get() = R.xml.template_preferences

    private val args: CreateTemplateFragmentArgs by navArgs()
    private val lastTemplateName by lazy { bundleOf(KEY_TEMPLATE_NAME to currentTemplateName) }
    private lateinit var tempSp: JsonSharedPreferencesImpl
    private val currentTemplateName: String
        get() = tempSp.getString(getString(R.string.template_name_key), NULL_TEMPLATE_NAME)!!

    @SuppressLint("RestrictedApi")
    override fun onCreatePreferenceManager(savedInstanceState: Bundle?) =
        object : PreferenceManager(requireContext()) {
            override fun getSharedPreferences(): SharedPreferences {
                if (!::tempSp.isInitialized) {
                    tempSp = try {
                        JsonSharedPreferencesImpl(
                            Gson().toJson(
                                Templates(binderViewModel.readSp(R.xml.template_preferences)).values.first {
                                    it.templateName == if (savedInstanceState == null) args.templateName
                                    else savedInstanceState.getString(KEY_TEMPLATE_NAME)
                                }
                            )
                        )
                    } catch (e: NoSuchElementException) {
                        JsonSharedPreferencesImpl()
                    }.apply {
                        if (savedInstanceState == null) {
                            edit {
                                putString(getString(R.string.template_name_key), args.templateName)
                            }
                        }
                    }
                }
                return tempSp
            }
        }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_TEMPLATE_NAME, currentTemplateName)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.template_preferences, rootKey)

        val templateName = getString(R.string.template_name_key)
        findPreference<EditTextPreference>(templateName)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                when {
                    args.templateName == newValue as String -> false
                    Templates(binderViewModel.readSp(R.xml.template_preferences)).values
                        .any { it.templateName == newValue } -> {
                        makeSnackbarWithFullyDraggableContainer(
                            { requireActivity().findViewById(R.id.fully_draggable_container) },
                            requireView(), R.string.template_name_not_unique, Snackbar.LENGTH_SHORT
                        ).show()
                        false
                    }
                    else -> {
                        lastTemplateName.putString(KEY_TEMPLATE_NAME, newValue)
                        true
                    }
                }
            }

        args.hookOperation?.let {
            val hookOperation = getString(R.string.hook_operation_key)
            findPreference<RefinedMultiSelectListPreference>(hookOperation)
                ?.values = it.toSet()
        }

        val applyToApp = getString(R.string.apply_to_app_key)
        findPreference<AppListMultiSelectListPreference>(applyToApp)
            ?.loadApps { binderViewModel.getInstalledPackages(0) }
            ?.setOnAppsLoadedListener { preference ->
                args.packageNames?.let {
                    preference.values = preference.values + it
                }
            }

        args.permittedMediaTypes?.let {
            val permittedMediaTypes = getString(R.string.permitted_media_types_key)
            findPreference<RefinedMultiSelectListPreference>(permittedMediaTypes)
                ?.values = it.toSet()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ) = super.onCreateView(inflater, container, savedInstanceState)
        .apply { prepareSharedElementTransition(listView) }

    private fun prepareSharedElementTransition(list: RecyclerView) {
        setFragmentResult(CreateTemplateFragment::class.java.name, lastTemplateName)

        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.nav_host
            setAllContainerColors(requireContext().colorSurface)
            interpolator = FastOutSlowInInterpolator()
            fadeMode = MaterialContainerTransform.FADE_MODE_CROSS
            duration = requireContext().mediumAnimTime
        }

        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: List<String>, sharedElements: MutableMap<String, View>
            ) {
                if (names.isNotEmpty()) {
                    list.transitionName = currentTemplateName
                    sharedElements[names[0]] = list
                }
            }
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (args.templateName != null && args.packageNames == null) {
            (requireActivity() as AppCompatActivity).supportActionBar?.setTitle(R.string.edit_template_title)
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        val f = when (preference) {
            is EditTextPreference -> MaterialEditTextPreferenceDialogFragmentCompat
                .newInstance(preference.key)
            is MultiSelectListPreference -> MaterialMultiSelectListPreferenceDialogFragmentCompat
                .newInstance(preference.key)
            is PathListPreference -> PathListPreferenceFragmentCompat.newInstance(preference.key)
            else -> {
                super.onDisplayPreferenceDialog(preference)
                return
            }
        }
        f.setTargetFragment(this, 0)
        f.show(parentFragmentManager, DIALOG_FRAGMENT_TAG)
    }

    override fun onStop() {
        super.onStop()
        save()
    }

    private fun save(): Boolean {
        val templateName = tempSp.getString(getString(R.string.template_name_key), null)
        val hookOperationValues =
            findPreference<MultiSelectListPreference>(getString(R.string.hook_operation_key))?.values
        if (!templateName.isNullOrEmpty() && hookOperationValues?.isNotEmpty() == true) {
            val template = Gson().fromJson(tempSp.delegate.toString(), Template::class.java)
            val json = Gson().toJson(
                Templates(binderViewModel.readSp(R.xml.template_preferences)).values.filterNot {
                    it.templateName == templateName || it.templateName == args.templateName
                } + template
            )
            binderViewModel.writeSp(who, json)
        }
        return true
    }

    companion object {
        const val KEY_TEMPLATE_NAME = "me.gm.cleaner.plugin.key.templateName"
        const val NULL_TEMPLATE_NAME = "@null"
        private const val DIALOG_FRAGMENT_TAG = "androidx.preference.PreferenceFragment.DIALOG"
    }
}
