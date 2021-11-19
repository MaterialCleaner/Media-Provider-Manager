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

package me.gm.cleaner.plugin.dao

import android.database.Cursor
import androidx.room.*

@Entity
data class MediaProviderQueryRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "time_millis") val timeMillis: Int,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "table") val table: Int,
    @ColumnInfo(name = "data") val data: List<String>,
    @ColumnInfo(name = "mime_type") val mimeType: List<String>,
    @ColumnInfo(name = "intercepted") val intercepted: Boolean,
)

@Dao
interface MediaProviderQueryRecordDao {
    @Query("SELECT * FROM MediaProviderQueryRecord")
    fun getAll(): Cursor

    @Query("SELECT * FROM MediaProviderQueryRecord WHERE package_name IN (:packageNames)")
    fun loadForPackageName(vararg packageNames: String): Cursor

    @Insert
    fun insertAll(vararg records: MediaProviderQueryRecord)

    @Delete
    fun delete(record: MediaProviderQueryRecord)
}

@Database(entities = [MediaProviderQueryRecord::class], version = 1, exportSchema = false)
@TypeConverters(ListConverter::class)
abstract class MediaProviderQueryRecordDatabase : RoomDatabase() {
    abstract fun MediaProviderQueryRecordDao(): MediaProviderQueryRecordDao
}
