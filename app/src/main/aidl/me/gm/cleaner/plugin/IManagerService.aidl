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

    int packageUsageTimes(String table, in List<String> packageNames) = 31;

    void registerMediaChangeObserver(in IMediaChangeObserver observer) = 32;

    void unregisterMediaChangeObserver(in IMediaChangeObserver observer) = 33;
}
