package me.gm.cleaner.plugin;

import me.gm.cleaner.plugin.model.ParceledListSlice;
import me.gm.cleaner.plugin.IMediaChangeObserver;

interface IManagerService {

    int getModuleVersion() = 0;

    ParceledListSlice<PackageInfo> getInstalledPackages(int userId, int flags) = 10;

    PackageInfo getPackageInfo(String packageName, int flags, int userId) = 11;

    String readSp(int who) = 20;

    void writeSp(int who, String what) = 21;

    void clearAllTables() = 30;

    int packageUsageTimes(int operation, in List<String> packageNames) = 31;

    void registerMediaChangeObserver(in IMediaChangeObserver observer) = 32;

    void unregisterMediaChangeObserver(in IMediaChangeObserver observer) = 33;
}
