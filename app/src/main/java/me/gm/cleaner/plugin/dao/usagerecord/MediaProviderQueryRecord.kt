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
data class MediaProviderQueryRecord(
    @PrimaryKey @ColumnInfo(name = "time_millis") override val timeMillis: Long,
    @ColumnInfo(name = "package_name") override val packageName: String,
    @ColumnInfo(name = "table") val table: Int,
    @ColumnInfo(name = "data") val data: List<String>,
    @ColumnInfo(name = "mime_type") val mimeType: List<String>,
    @ColumnInfo(name = "intercepted") val intercepted: List<Boolean>,
) : MediaProviderRecord(timeMillis, packageName, data, mimeType, intercepted) {
    override fun convert(cursor: Cursor): List<MediaProviderQueryRecord> {
        val timeMillisColumn = cursor.getColumnIndexOrThrow("time_millis")
        val packageNameColumn = cursor.getColumnIndexOrThrow("package_name")
        val tableColumn = cursor.getColumnIndexOrThrow("table")
        val dataColumn = cursor.getColumnIndexOrThrow("data")
        val mimeTypeColumn = cursor.getColumnIndexOrThrow("mime_type")
        val interceptedColumn = cursor.getColumnIndexOrThrow("intercepted")

        val records = mutableListOf<MediaProviderQueryRecord>()
        while (cursor.moveToNext()) {
            records += MediaProviderQueryRecord(
                cursor.getLong(timeMillisColumn),
                cursor.getString(packageNameColumn),
                cursor.getInt(tableColumn),
                ListConverter.fromString(cursor.getString(dataColumn)) ?: continue,
                ListConverter.fromString(cursor.getString(mimeTypeColumn)) ?: continue,
                ListConverter.booleanListFromString(cursor.getString(interceptedColumn)),
            )
        }
        return records
    }
}

@Dao
interface MediaProviderQueryRecordDao {
    @Query("SELECT * FROM MediaProviderQueryRecord WHERE time_millis BETWEEN (:start) AND (:end)")
    fun loadForTimeMillis(start: Long, end: Long): Cursor

    @Query("SELECT count(*) FROM MediaProviderQueryRecord WHERE package_name IN (:packageNames)")
    fun packageUsageTimes(vararg packageNames: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(records: MediaProviderQueryRecord)

    @Delete
    fun delete(record: MediaProviderQueryRecord)
}
