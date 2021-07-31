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

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import me.gm.cleaner.plugin.app.BaseActivity;
import me.gm.cleaner.plugin.databinding.HomeActivityBinding;
import rikka.recyclerview.RecyclerViewKt;
import rikka.widget.borderview.BorderRecyclerView;

public class AppListActivity extends BaseActivity {
    private AppListAdapter mAdapter;

    @Override
    protected void onCreate(                             @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HomeActivityBinding binding = HomeActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BorderRecyclerView recyclerView = binding.list;
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 1));
        recyclerView.setHasFixedSize(true);
        RecyclerViewKt.fixEdgeEffect(recyclerView, true, true);
        RecyclerViewKt.addFastScroller(recyclerView, recyclerView);
        recyclerView.getBorderViewDelegate().setBorderVisibilityChangedListener((top, oldTop, bottom, oldBottom)
                -> ((BaseActivity) requireActivity()).appBarLayout.setRaised(!top));
        mAdapter = new AppListAdapter(this);
        recyclerView.setAdapter(mAdapter);

        int title = getArguments().getInt(ClientConstants.TITLE);
        ServiceSettingsModel serviceSettingsModel = new ViewModelProvider(requireActivity())
                .get(ServiceSettingsModel.class);
        me.gm.cleaner.client.ui.AppListModel viewModel = new ViewModelProvider(this).get(me.gm.cleaner.client.ui.AppListModel.class);
        viewModel.init(requireContext().getPackageManager(), title, serviceSettingsModel);

        serviceSettingsModel.isSearching.observe(getViewLifecycleOwner(), isSearching -> {
            if (!isSearching) {
                viewModel.refreshShowingPackages();
            }
        });
        serviceSettingsModel.queryText.observe(getViewLifecycleOwner(), queryText -> {
            if (!TextUtils.isEmpty(queryText)) {
                viewModel.refreshSearchingPackages();
            }
        });
        serviceSettingsModel.installedPackages.observe(getViewLifecycleOwner(), installedPackages -> {
            viewModel.refreshShowingPackages();
            binding.listContainer.setRefreshing(false);
        });

        viewModel.showingPackages.observe(getViewLifecycleOwner(), showingPackages -> {
            if (serviceSettingsModel.isSearching()) {
                viewModel.refreshSearchingPackages();
            } else {
                mAdapter.submitList(showingPackages);
            }
        });
        viewModel.searchingPackages.observe(getViewLifecycleOwner(), searchingPackages -> {
            if (serviceSettingsModel.isSearching()) {
                mAdapter.submitList(searchingPackages);
            }
        });

        /*
        ServicePreferences.getInstance().setOnPreferenceChangeListener(() -> {
            viewModel.refreshShowingPackages();
            if (title == R.string.storage_redirect_title) {
                CleanerClient.notifySRChanged();
            } else {
                CleanerClient.notifyConfigChanged(0);
            }
        });
         */
        binding.listContainer.setOnRefreshListener(() -> {
            binding.listContainer.setRefreshing(true);
            serviceSettingsModel.refreshInstalledPackages();
        });
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (mAdapter.onContextItemSelected(item)) {
            return true;
        }
        return super.onContextItemSelected(item);
    }
}
