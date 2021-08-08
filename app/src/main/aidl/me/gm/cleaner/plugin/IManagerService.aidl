package me.gm.cleaner.plugin;

import me.gm.cleaner.plugin.ParceledListSlice;

interface IManagerService {

    int getServerVersion() = 0;

    ParceledListSlice<PackageInfo> getInstalledPackages() = 10;

    void notifyPreferencesChanged() = 11;

    // TODO: getUsageRecord
}
