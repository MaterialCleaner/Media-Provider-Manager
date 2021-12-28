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

package me.gm.cleaner.plugin.dao.usagerecord

import android.database.Cursor
import androidx.room.Database
import androidx.room.Ignore
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import me.gm.cleaner.plugin.dao.ListConverter
import me.gm.cleaner.plugin.module.PreferencesPackageInfo

abstract class MediaProviderRecord(
    open val timeMillis: Long,
    open val packageName: String,
    @Ignore val dataList: List<String>,
    @Ignore val mimeTypeList: List<String>,
    @Ignore val interceptedList: List<Boolean>,
    @Ignore var packageInfo: PreferencesPackageInfo? = null,
) {
    abstract fun convert(cursor: Cursor): List<MediaProviderRecord>

    override fun hashCode(): Int {
        var result = timeMillis.hashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + dataList.hashCode()
        result = 31 * result + mimeTypeList.hashCode()
        result = 31 * result + interceptedList.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MediaProviderRecord

        if (timeMillis != other.timeMillis) return false
        if (packageName != other.packageName) return false
        if (dataList != other.dataList) return false
        if (mimeTypeList != other.mimeTypeList) return false
        if (interceptedList != other.interceptedList) return false

        return true
    }
}

@Database(
    entities = [
        MediaProviderQueryRecord::class,
        MediaProviderInsertRecord::class,
        MediaProviderDeleteRecord::class,
    ], version = 1, exportSchema = false
)
@TypeConverters(ListConverter::class)
abstract class MediaProviderRecordDatabase : RoomDatabase() {
    abstract fun MediaProviderQueryRecordDao(): MediaProviderQueryRecordDao
    abstract fun MediaProviderInsertRecordDao(): MediaProviderInsertRecordDao
    abstract fun MediaProviderDeleteRecordDao(): MediaProviderDeleteRecordDao
}
