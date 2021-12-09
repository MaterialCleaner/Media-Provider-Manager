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
import androidx.room.*
import me.gm.cleaner.plugin.dao.ListConverter

@Entity
data class MediaProviderDeleteRecord(
    @PrimaryKey @ColumnInfo(name = "time_millis") override val timeMillis: Long,
    @ColumnInfo(name = "package_name") override val packageName: String,
    @ColumnInfo(name = "match") val match: Int,
    @ColumnInfo(name = "data") val data: List<String>,
    @ColumnInfo(name = "mime_type") val mimeType: List<String>,
    @ColumnInfo(name = "intercepted") override val intercepted: Boolean,
) : MediaProviderRecord(timeMillis, packageName, data, intercepted) {
    override fun convert(cursor: Cursor): List<MediaProviderDeleteRecord> {
        val timeMillisColumn = cursor.getColumnIndex("time_millis")
        val packageNameColumn = cursor.getColumnIndex("package_name")
        val matchColumn = cursor.getColumnIndex("match")
        val dataColumn = cursor.getColumnIndex("data")
        val mimeTypeColumn = cursor.getColumnIndex("mime_type")
        val interceptedColumn = cursor.getColumnIndex("intercepted")

        val records = mutableListOf<MediaProviderDeleteRecord>()
        while (cursor.moveToNext()) {
            records += MediaProviderDeleteRecord(
                cursor.getLong(timeMillisColumn),
                cursor.getString(packageNameColumn),
                cursor.getInt(matchColumn),
                ListConverter.fromString(cursor.getString(dataColumn)) ?: continue,
                ListConverter.fromString(cursor.getString(mimeTypeColumn)) ?: continue,
                cursor.getLong(interceptedColumn) != 0L,
            )
        }
        return records
    }
}

@Dao
interface MediaProviderDeleteRecordDao {
    @Query("SELECT * FROM MediaProviderDeleteRecord WHERE time_millis BETWEEN (:start) AND (:end)")
    fun loadForTimeMillis(start: Long, end: Long): Cursor

    @Query("SELECT count(*) FROM MediaProviderDeleteRecord WHERE package_name IN (:packageNames)")
    fun packageUsageTimes(vararg packageNames: String): Int

    @Insert
    fun insert(records: MediaProviderDeleteRecord)

    @Delete
    fun delete(record: MediaProviderDeleteRecord)
}
