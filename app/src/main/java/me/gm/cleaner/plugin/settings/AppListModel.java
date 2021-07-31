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

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import me.gm.cleaner.R;
import me.gm.cleaner.client.ServicePreferencesPackageInfo;
import me.gm.cleaner.dao.ServicePreferences;

public class AppListModel extends ViewModel {
    public final MutableLiveData<List<ServicePreferencesPackageInfo>> showingPackages
            = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<List<ServicePreferencesPackageInfo>> searchingPackages
            = new MutableLiveData<>(new ArrayList<>());
    private PackageManager mPM;
    private int mTitle;
    private ServiceSettingsModel mServiceSettingsModel;

    public void init(PackageManager pm, int title, ServiceSettingsModel serviceSettingsModel) {
        if (mPM == null) {
            mPM = pm;
            mTitle = title;
            mServiceSettingsModel = serviceSettingsModel;
        }
    }

    @SuppressLint("NonConstantResourceId")
    public void refreshShowingPackages() {
        ServicePreferences preferences = ServicePreferences.getInstance();
        List<ServicePreferencesPackageInfo> list
                = new ArrayList<>(mServiceSettingsModel.installedPackages.getValue());
        if (preferences.isHideSystemApp()) {
            list.removeIf(packageInfo ->
                    (packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
        }
        if (preferences.isHideNoStoragePermissionApp()) {
            list.removeIf(packageInfo -> {
                String[] requestedPermissions = packageInfo.requestedPermissions;
                return requestedPermissions == null || !Arrays.asList(requestedPermissions)
                        .contains(Manifest.permission.READ_EXTERNAL_STORAGE);
            });
        }
        switch (preferences.getSortBy()) {
            case ServicePreferences.SORT_BY_NAME:
                list.sort((o1, o2) -> new ApplicationInfo.DisplayNameComparator(mPM)
                        .compare(o1.applicationInfo, o2.applicationInfo));
                break;
            case ServicePreferences.SORT_BY_UPDATE_TIME:
                list.sort(Comparator.comparingLong(value -> -value.lastUpdateTime));
                break;
        }
        list.sort((o1, o2) -> {
            switch (mTitle) {
                case R.string.storage_redirect_title:
                    return o2.srCount - o1.srCount;
                case R.string.foreground_activity_observer_title:
                    return o2.faCount - o1.faCount;
                default:
                    return 0;
            }
        });
        showingPackages.postValue(list);
    }

    public void refreshSearchingPackages() {
        String lowerQuery = mServiceSettingsModel.queryText.getValue().toLowerCase();
        searchingPackages.postValue(showingPackages.getValue().stream().filter(packageInfo -> {
            String label = packageInfo.applicationInfo.loadLabel(mPM).toString();
            String packageName = packageInfo.applicationInfo.packageName;
            return label.toLowerCase().contains(lowerQuery)
                    || packageName.toLowerCase().contains(lowerQuery);
        }).collect(Collectors.toList()));
    }
}
