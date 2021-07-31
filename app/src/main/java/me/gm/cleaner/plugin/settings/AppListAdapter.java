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

package me.gm.cleaner.plugin.settings;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AppListAdapter extends ListAdapter<PreferencesPackageInfo, AppListAdapter.ViewHolder> {
    @NonNull
    private static final DiffUtil.ItemCallback<PreferencesPackageInfo> CALLBACK = new DiffUtil.ItemCallback<PreferencesPackageInfo>() {
        @Override
        public boolean areItemsTheSame(@NonNull PreferencesPackageInfo oldItem, @NonNull PreferencesPackageInfo newItem) {
            return oldItem.applicationInfo.packageName.equals(newItem.applicationInfo.packageName);
        }

        @Override
        public boolean areContentsTheSame(@NonNull PreferencesPackageInfo oldItem, @NonNull PreferencesPackageInfo newItem) {
            return oldItem.ruleCount == newItem.ruleCount && oldItem.faCount == newItem.faCount;
        }
    };
    private final Fragment mFragment;
    private final ServiceSettingsActivity mActivity;
    private final PackageManager mPM;
    private final int mTitle;
    private final ServiceSettingsModel mServiceSettingsModel;
    private final Map<String, Pair<CharSequence, CharSequence>> mTextCache = new HashMap<>();
    private ViewHolder mSelectedHolder;

    public AppListAdapter(Fragment fragment) {
        super(CALLBACK);
        mFragment = fragment;
        mActivity = (ServiceSettingsActivity) fragment.requireActivity();
        mPM = mActivity.getPackageManager();
        mTitle = fragment.getArguments().getInt(ClientConstants.TITLE);
        mServiceSettingsModel = new ViewModelProvider(mActivity).get(ServiceSettingsModel.class);
    }

    @Override
    public void onCurrentListChanged(@NonNull List<PreferencesPackageInfo> previousList, @NonNull List<PreferencesPackageInfo> currentList) {
        super.onCurrentListChanged(previousList, currentList);
        mTextCache.clear();
        new Thread(() -> {
            for (PreferencesPackageInfo pi : currentList) {
                mTextCache.put(pi.packageName, new Pair<>(pi.applicationInfo.loadLabel(mPM), getSummary(pi)));
            }
        }).start();
    }

    @SuppressLint("NonConstantResourceId")
    private CharSequence getSummary(PreferencesPackageInfo pi) {
        ServicePreferences preferences = ServicePreferences.getInstance();
        switch (mTitle) {
            case R.string.storage_redirect_title:
                if (pi.ruleCount > 0) {
                    return getSpannableString(String.format(Locale.ENGLISH, mActivity
                            .getString(R.string.enabled_rule_count), pi.ruleCount));
                }
                break;
            case R.string.foreground_activity_observer_title:
                if (pi.faCount > 0) {
                    List<Integer> list = preferences.enquireAboutPackageFA(pi.packageName);
                    List<String> enabledFunctions = new ArrayList<>();
                    for (int id : list) {
                        enabledFunctions.add(mActivity.getString(id));
                    }
                    return getSpannableString(TextUtils.join(", ", enabledFunctions));
                }
                break;
        }
        return pi.packageName;
    }

    private SpannableStringBuilder getSpannableString(CharSequence text) {
        SpannableStringBuilder sb = new SpannableStringBuilder(text);
        sb.setSpan(new ForegroundColorSpan(DisplayUtils.getColorByAttr(mActivity, R.attr.colorPrimary)),
                0, sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        sb.setSpan(new StyleSpan(Typeface.BOLD), 0, sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        return sb;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ApplistItemBinding binding = ApplistItemBinding.inflate(LayoutInflater.from(parent.getContext()));
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ApplistItemBinding binding = holder.binding;
        PreferencesPackageInfo pi = getItem(position);
        GlideApp.with(mActivity)
                .load(pi)
                .into(binding.icon);
        ApplicationInfo ai = pi.applicationInfo;
        setText(pi, binding.title, binding.summary);
        binding.getRoot().setOnClickListener(v -> startActivity(ai));
        if (mTitle == R.string.storage_redirect_title) {
            binding.getRoot().setOnClickListener(v -> {
                if (mServiceSettingsModel.srAppCount.getValue() >= 30 && TextUtils.isEmpty(
                        ServicePreferences.getInstance().isStorageRedirect(pi.packageName))) {
                    new TestVerDialog().show(mFragment.getParentFragmentManager(), null);
                } else {
                    startActivity(ai);
                }
            });
            binding.getRoot().setOnLongClickListener(v -> {
                mSelectedHolder = holder;
                return false;
            });
            binding.getRoot().setOnCreateContextMenuListener((menu, v, menuInfo) -> {
                mActivity.getMenuInflater().inflate(R.menu.menu_storage_redirect_item, menu);
                menu.setHeaderTitle(ai.loadLabel(mPM));
            });
        }
    }

    private void setText(PreferencesPackageInfo pi, TextView title, TextView summary) {
        Pair<CharSequence, CharSequence> pair = mTextCache.get(pi.packageName);
        if (pair != null) {
            title.setText(pair.first);
            summary.setText(pair.second);
        } else {
            title.setText(pi.applicationInfo.loadLabel(mPM));
            summary.setText(getSummary(pi));
        }
    }

    private void startActivity(ApplicationInfo ai) {
        Class<?> clazz = (Class<?>) mFragment.getArguments().getSerializable(ClientConstants.ACTIVITY);
        mActivity.startActivity(new Intent(mActivity, clazz).putExtra(ClientConstants.APP_INFO, ai));
    }

    public boolean onContextItemSelected(MenuItem item) {
        int position = mSelectedHolder.getBindingAdapterPosition();
        PreferencesPackageInfo pi = getItem(position);
        if (item.getItemId() == R.id.menu_delete_all_rules) {
            ServicePreferences.getInstance().removeStorageRedirect(pi.packageName);
            return true;
        }
        return false;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ApplistItemBinding binding;

        public ViewHolder(@NonNull ApplistItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
