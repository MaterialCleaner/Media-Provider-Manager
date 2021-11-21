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

@Entity
data class MediaProviderInsertRecord(
    @ColumnInfo(name = "time_millis") override val timeMillis: Long,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "match") val match: Int,
    @ColumnInfo(name = "data") val data: String,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "intercepted") val intercepted: Boolean,
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
) : MediaProviderRecord(timeMillis) {
    override fun convert(cursor: Cursor): List<MediaProviderInsertRecord> {
        val timeMillisColumn = cursor.getColumnIndex("time_millis")
        val packageNameColumn = cursor.getColumnIndex("package_name")
        val matchColumn = cursor.getColumnIndex("match")
        val dataColumn = cursor.getColumnIndex("data")
        val mimeTypeColumn = cursor.getColumnIndex("mime_type")
        val interceptedColumn = cursor.getColumnIndex("intercepted")

        val records = mutableListOf<MediaProviderInsertRecord>()
        while (cursor.moveToNext()) {
            records += MediaProviderInsertRecord(
                cursor.getLong(timeMillisColumn),
                cursor.getString(packageNameColumn),
                cursor.getInt(matchColumn),
                cursor.getString(dataColumn),
                cursor.getString(mimeTypeColumn),
                cursor.getLong(interceptedColumn) != 0L,
            )
        }
        return records
    }
}

@Dao
interface MediaProviderInsertRecordDao {
    @Query("SELECT count(*) FROM MediaProviderInsertRecord")
    fun size(): Int

    @Query("SELECT * FROM MediaProviderInsertRecord WHERE time_millis BETWEEN (:start) AND (:end)")
    fun loadForTimeMillis(start: Long, end: Long): Cursor

    @Query("SELECT * FROM MediaProviderInsertRecord WHERE time_millis BETWEEN (:start) AND (:end) AND package_name IN (:packageNames)")
    fun loadForPackageName(start: Long, end: Long, vararg packageNames: String): Cursor

    @Insert
    fun insert(vararg records: MediaProviderInsertRecord)

    @Delete
    fun delete(record: MediaProviderInsertRecord)
}
