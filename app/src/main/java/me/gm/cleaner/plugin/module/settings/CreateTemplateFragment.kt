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
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.SharedElementCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
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
import me.gm.cleaner.plugin.module.settings.preference.AppListMultiSelectListPreference
import me.gm.cleaner.plugin.module.settings.preference.PathListPreference
import me.gm.cleaner.plugin.module.settings.preference.PathListPreferenceFragmentCompat
import me.gm.cleaner.plugin.widget.makeSnackbarWithFullyDraggableContainer
import kotlin.collections.set

class CreateTemplateFragment : AbsSettingsFragment() {
    override val who: Int
        get() = R.xml.template_preferences

    private val args: CreateTemplateFragmentArgs by navArgs()
    private val tempSp by lazy {
        try {
            JsonSharedPreferencesImpl(
                Gson().toJson(
                    binderViewModel.readTemplates().first { it.templateName == args.label })
            )
        } catch (e: NoSuchElementException) {
            JsonSharedPreferencesImpl()
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreatePreferenceManager() = object : PreferenceManager(context) {
        override fun getSharedPreferences() = tempSp
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.template_preferences, rootKey)
        val templateName = getString(R.string.template_name_key)
        findPreference<EditTextPreference>(templateName)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                when {
                    args.label == newValue as String -> false
                    binderViewModel.readTemplates().any { it.templateName == newValue } -> {
                        makeSnackbarWithFullyDraggableContainer(
                            { requireActivity().findViewById(R.id.fully_draggable_container) },
                            requireView(), R.string.template_name_not_unique, Snackbar.LENGTH_SHORT
                        ).show()
                        false
                    }
                    else -> true
                }
            }

        val applyToApp = getString(R.string.apply_to_app_key)
        findPreference<AppListMultiSelectListPreference>(applyToApp)
            ?.loadApps(lifecycleScope) { binderViewModel.installedPackages }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = super.onCreateView(inflater, container, savedInstanceState)
        ?.apply { prepareSharedElementTransition(findViewById(R.id.recycler_view)) }

    private fun prepareSharedElementTransition(list: RecyclerView) {
        val label = args.label ?: "null"
        setFragmentResult(
            CreateTemplateFragment::class.java.simpleName, bundleOf(KEY_LABEL to label)
        )
        list.transitionName = label

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
                    sharedElements[names[0]] = list
                }
            }
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (args.label != null) {
            (requireActivity() as AppCompatActivity).supportActionBar?.setTitle(R.string.edit_template_title)
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference?) {
        val f = when (preference) {
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
        val templateName = getString(R.string.template_name_key)
        val label = tempSp.getString(templateName, null)
        val hookOperationValues =
            findPreference<MultiSelectListPreference>(getString(R.string.hook_operation_key))?.values
        if (!label.isNullOrEmpty() && hookOperationValues?.isNotEmpty() == true) {
            val template = Gson().fromJson(tempSp.delegate.toString(), Template::class.java)
            val json = Gson().toJson(
                binderViewModel.readTemplates()
                    .filterNot { it.templateName == args.label } + template
            )
            binderViewModel.writeSp(who, json)
        }
        return true
    }

    companion object {
        const val KEY_LABEL = "me.gm.cleaner.plugin.key.label"
        private const val DIALOG_FRAGMENT_TAG = "androidx.preference.PreferenceFragment.DIALOG"
    }
}
