package me.gm.cleaner.plugin;

import me.gm.cleaner.plugin.ParceledListSlice;

interface IManagerService {

    int getServerVersion() = 0;

    ParceledListSlice<PackageInfo> getInstalledPackages() = 1;

    void notifyConfigChanged(int id) = 2;

    // TODO: getUsageRecord
}
