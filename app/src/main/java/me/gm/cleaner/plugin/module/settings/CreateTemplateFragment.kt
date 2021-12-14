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
import me.gm.cleaner.plugin.model.Templates
import me.gm.cleaner.plugin.module.settings.preference.AppListMultiSelectListPreference
import me.gm.cleaner.plugin.module.settings.preference.PathListPreference
import me.gm.cleaner.plugin.module.settings.preference.PathListPreferenceFragmentCompat
import me.gm.cleaner.plugin.widget.makeSnackbarWithFullyDraggableContainer
import kotlin.collections.set

class CreateTemplateFragment : AbsSettingsFragment() {
    override val who: Int
        get() = R.xml.template_preferences

    private val args: CreateTemplateFragmentArgs by navArgs()
    private lateinit var tempSp: JsonSharedPreferencesImpl

    @SuppressLint("RestrictedApi")
    override fun onCreatePreferenceManager(savedInstanceState: Bundle?) =
        object : PreferenceManager(context) {
            override fun getSharedPreferences(): SharedPreferences {
                tempSp = try {
                    JsonSharedPreferencesImpl(
                        Gson().toJson(
                            Templates(binderViewModel.readSp(R.xml.template_preferences)).first {
                                it.templateName == if (savedInstanceState == null) args.templateName
                                else savedInstanceState.getString(KEY_TEMPLATE_NAME)
                            })
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
                return tempSp
            }
        }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(
            KEY_TEMPLATE_NAME,
            tempSp.getString(getString(R.string.template_name_key), args.templateName)
        )
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.template_preferences, rootKey)
        val templateName = getString(R.string.template_name_key)
        findPreference<EditTextPreference>(templateName)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                when {
                    args.templateName == newValue as String -> false
                    Templates(binderViewModel.readSp(R.xml.template_preferences))
                        .any { it.templateName == newValue } -> {
                        makeSnackbarWithFullyDraggableContainer(
                            { requireActivity().findViewById(R.id.fully_draggable_container) },
                            requireView(), R.string.template_name_not_unique, Snackbar.LENGTH_SHORT
                        ).show()
                        false
                    }
                    else -> {
                        prepareSharedElementTransition(newValue, listView)
                        true
                    }
                }
            }

        val applyToApp = getString(R.string.apply_to_app_key)
        findPreference<AppListMultiSelectListPreference>(applyToApp)
            ?.loadApps(lifecycleScope) { binderViewModel.installedPackages }
            ?.setOnAppsLoadedListener {
                if (args.packageName != null) {
                    it.values = it.values + args.packageName
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = super.onCreateView(inflater, container, savedInstanceState)
        ?.apply {
            prepareSharedElementTransition(
                tempSp.getString(getString(R.string.template_name_key), NULL_TEMPLATE_NAME),
                listView
            )
        }

    private fun prepareSharedElementTransition(templateName: String?, list: RecyclerView) {
        setFragmentResult(
            CreateTemplateFragment::class.java.simpleName,
            bundleOf(KEY_TEMPLATE_NAME to templateName)
        )
        list.transitionName = templateName

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
        if (args.templateName != null && args.packageName == null) {
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
        val templateName = tempSp.getString(getString(R.string.template_name_key), null)
        val hookOperationValues =
            findPreference<MultiSelectListPreference>(getString(R.string.hook_operation_key))?.values
        if (!templateName.isNullOrEmpty() && hookOperationValues?.isNotEmpty() == true) {
            val template = Gson().fromJson(tempSp.delegate.toString(), Template::class.java)
            val json = Gson().toJson(
                Templates(binderViewModel.readSp(R.xml.template_preferences)).filterNot {
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
