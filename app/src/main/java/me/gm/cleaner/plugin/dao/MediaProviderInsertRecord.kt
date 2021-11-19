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
data class MediaProviderInsertRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "time_millis") val timeMillis: Int,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "match") val match: Int,
    @ColumnInfo(name = "data") val data: String,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "intercepted") val intercepted: Boolean,
)

@Dao
interface MediaProviderInsertRecordDao {
    @Query("SELECT * FROM MediaProviderInsertRecord")
    fun getAll(): Cursor

    @Query("SELECT * FROM MediaProviderInsertRecord WHERE package_name IN (:packageNames)")
    fun loadForPackageName(vararg packageNames: String): Cursor

    @Insert
    fun insertAll(vararg records: MediaProviderInsertRecord)

    @Delete
    fun delete(record: MediaProviderInsertRecord)
}

@Database(entities = [MediaProviderInsertRecord::class], version = 1, exportSchema = false)
abstract class MediaProviderInsertRecordDatabase : RoomDatabase() {
    abstract fun MediaProviderInsertRecordDao(): MediaProviderInsertRecordDao
}
