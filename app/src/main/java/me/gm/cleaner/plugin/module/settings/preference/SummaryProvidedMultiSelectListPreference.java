package me.gm.cleaner.plugin.module.settings.preference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.icu.text.ListFormatter;
import android.util.AttributeSet;

import androidx.core.content.res.TypedArrayUtils;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.R;

import java.text.Collator;
import java.util.Arrays;

@SuppressLint({"RestrictedApi", "PrivateResource"})
public class SummaryProvidedMultiSelectListPreference extends MultiSelectListPreference {
    public SummaryProvidedMultiSelectListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        setSummaryProvider(SimpleSummaryProvider.getInstance());
    }

    public SummaryProvidedMultiSelectListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SummaryProvidedMultiSelectListPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context, R.attr.dialogPreferenceStyle,
                android.R.attr.dialogPreferenceStyle));
    }

    public SummaryProvidedMultiSelectListPreference(Context context) {
        this(context, null);
    }

    /**
     * A simple {@link androidx.preference.Preference.SummaryProvider} implementation for a
     * {@link MultiSelectListPreference}. If no value has been set, the summary displayed will be 'Not set',
     * otherwise the summary displayed will be the entry set for this preference.
     */
    public static final class SimpleSummaryProvider implements SummaryProvider<SummaryProvidedMultiSelectListPreference> {

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
        public CharSequence provideSummary(SummaryProvidedMultiSelectListPreference preference) {
            if (preference.getValues().isEmpty()) {
                return (preference.getContext().getString(R.string.not_set));
            } else {
                var entryValues = Arrays.asList(preference.getEntryValues());
                var collator = Collator.getInstance();
                var values = preference.getValues().stream()
                        .map(it -> preference.getEntries()[entryValues.indexOf(it)])
                        .sorted(collator)
                        .toArray();
                return ListFormatter.getInstance().format(values);
            }
        }
    }
}
