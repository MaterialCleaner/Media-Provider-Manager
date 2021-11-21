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
import androidx.room.*
import me.gm.cleaner.plugin.dao.ListConverter

@Entity
data class MediaProviderQueryRecord(
    @ColumnInfo(name = "time_millis") override val timeMillis: Long,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "table") val table: Int,
    @ColumnInfo(name = "data") val data: List<String>,
    @ColumnInfo(name = "mime_type") val mimeType: List<String>,
    @ColumnInfo(name = "intercepted") val intercepted: Boolean,
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
) : MediaProviderRecord(timeMillis) {
    override fun convert(cursor: Cursor): List<MediaProviderQueryRecord> {
        val timeMillisColumn = cursor.getColumnIndex("time_millis")
        val packageNameColumn = cursor.getColumnIndex("package_name")
        val tableColumn = cursor.getColumnIndex("table")
        val dataColumn = cursor.getColumnIndex("data")
        val mimeTypeColumn = cursor.getColumnIndex("mime_type")
        val interceptedColumn = cursor.getColumnIndex("intercepted")

        val records = mutableListOf<MediaProviderQueryRecord>()
        while (cursor.moveToNext()) {
            records += MediaProviderQueryRecord(
                cursor.getLong(timeMillisColumn),
                cursor.getString(packageNameColumn),
                cursor.getInt(tableColumn),
                ListConverter.fromString(cursor.getString(dataColumn))!!,
                ListConverter.fromString(cursor.getString(mimeTypeColumn))!!,
                cursor.getLong(interceptedColumn) != 0L,
            )
        }
        return records
    }
}

@Dao
interface MediaProviderQueryRecordDao {
    @Query("SELECT count(*) FROM MediaProviderQueryRecord")
    fun size(): Int

    @Query("SELECT * FROM MediaProviderQueryRecord WHERE time_millis BETWEEN (:start) AND (:end)")
    fun loadForTimeMillis(start: Long, end: Long): Cursor

    @Query("SELECT * FROM MediaProviderQueryRecord WHERE time_millis BETWEEN (:start) AND (:end) AND package_name IN (:packageNames)")
    fun loadForPackageName(start: Long, end: Long, vararg packageNames: String): Cursor

    @Insert
    fun insert(records: MediaProviderQueryRecord)

    @Delete
    fun delete(record: MediaProviderQueryRecord)
}
