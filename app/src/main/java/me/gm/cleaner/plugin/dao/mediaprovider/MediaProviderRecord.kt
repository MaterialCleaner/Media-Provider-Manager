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

package me.gm.cleaner.plugin.dao.mediaprovider

import android.database.Cursor
import androidx.annotation.Keep
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import me.gm.cleaner.plugin.dao.ListConverter

abstract class MediaProviderRecord(
    open val timeMillis: Long
) {
    @Keep
    constructor() : this(0L)

    abstract fun convert(cursor: Cursor): List<MediaProviderRecord>
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
