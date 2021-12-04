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

package me.gm.cleaner.plugin.module.settings.preference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.icu.text.ListFormatter;
import android.util.AttributeSet;

import androidx.core.content.res.TypedArrayUtils;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.R;

import java.util.Arrays;
import java.util.Set;

@SuppressLint({"RestrictedApi", "PrivateResource"})
public class RefinedMultiSelectListPreference extends MultiSelectListPreference {
    public RefinedMultiSelectListPreference(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        setSummaryProvider(SimpleSummaryProvider.getInstance());
    }

    public RefinedMultiSelectListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RefinedMultiSelectListPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context, R.attr.dialogPreferenceStyle,
                android.R.attr.dialogPreferenceStyle));
    }

    public RefinedMultiSelectListPreference(Context context) {
        this(context, null);
    }

    @Override
    public void setValues(Set<String> values) {
        super.setValues(values);
        notifyDependencyChange(shouldDisableDependents());
    }

    @Override
    public boolean shouldDisableDependents() {
        return getValues().isEmpty();
    }

    /**
     * A simple {@link androidx.preference.Preference.SummaryProvider} implementation for a
     * {@link MultiSelectListPreference}. If no value has been set, the summary displayed will be 'Not set',
     * otherwise the summary displayed will be the entry set for this preference.
     */
    public static final class SimpleSummaryProvider
            implements SummaryProvider<RefinedMultiSelectListPreference> {

        private static SimpleSummaryProvider sSimpleSummaryProvider;

        private SimpleSummaryProvider() {
        }

        /**
         * Retrieve a singleton instance of this simple
         * {@link androidx.preference.Preference.SummaryProvider} implementation.
         *
         * @return a singleton instance of this simple
         * {@link androidx.preference.Preference.SummaryProvider} implementation
         */
        public static SimpleSummaryProvider getInstance() {
            if (sSimpleSummaryProvider == null) {
                sSimpleSummaryProvider = new SimpleSummaryProvider();
            }
            return sSimpleSummaryProvider;
        }

        @Override
        public CharSequence provideSummary(RefinedMultiSelectListPreference preference) {
            if (preference.getValues().isEmpty()) {
                return preference.getContext().getString(R.string.not_set);
            } else {
                var entryValues = Arrays.asList(preference.getEntryValues());
                var values = Arrays.stream(preference.getEntryValues())
                        .filter(it -> preference.getValues().contains(it))
                        .map(it -> preference.getEntries()[entryValues.indexOf(it)])
                        .toArray();
                return ListFormatter.getInstance().format(values);
            }
        }
    }
}
