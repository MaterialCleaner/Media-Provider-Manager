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

import android.content.pm.PackageInfo
import android.database.Cursor
import androidx.annotation.IntDef
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import me.gm.cleaner.plugin.dao.MediaProviderOperation.Companion.OP_DELETE
import me.gm.cleaner.plugin.dao.MediaProviderOperation.Companion.OP_INSERT
import me.gm.cleaner.plugin.dao.MediaProviderOperation.Companion.OP_QUERY

@Entity
data class MediaProviderRecord(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "time_millis") val timeMillis: Long,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "match") val match: Int,
    @ColumnInfo(name = "operation") @MediaProviderOperation val operation: Int,
    @ColumnInfo(name = "data") val data: List<String>,
    @ColumnInfo(name = "mime_type") val mimeType: List<String>,
    @ColumnInfo(name = "intercepted") val intercepted: List<Boolean>,
) {
    @Ignore
    var packageInfo: PackageInfo? = null

    @Ignore
    var label: String? = null

    companion object {
        fun convert(cursor: Cursor): List<MediaProviderRecord> {
            val timeMillisColumn = cursor.getColumnIndexOrThrow("time_millis")
            val packageNameColumn = cursor.getColumnIndexOrThrow("package_name")
            val matchColumn = cursor.getColumnIndexOrThrow("match")
            val operationColumn = cursor.getColumnIndexOrThrow("operation")
            val dataColumn = cursor.getColumnIndexOrThrow("data")
            val mimeTypeColumn = cursor.getColumnIndexOrThrow("mime_type")
            val interceptedColumn = cursor.getColumnIndexOrThrow("intercepted")

            val records = mutableListOf<MediaProviderRecord>()
            while (cursor.moveToNext()) {
                records += MediaProviderRecord(
                    0,
                    cursor.getLong(timeMillisColumn),
                    cursor.getString(packageNameColumn),
                    cursor.getInt(matchColumn),
                    cursor.getInt(operationColumn),
                    ListConverter.fromString(cursor.getString(dataColumn)) ?: continue,
                    ListConverter.fromString(cursor.getString(mimeTypeColumn)) ?: continue,
                    ListConverter.booleanListFromString(cursor.getString(interceptedColumn)),
                )
            }
            return records
        }
    }
}

@Dao
interface MediaProviderRecordDao {
    @Query("SELECT * FROM MediaProviderRecord WHERE operation IN (:operations) AND time_millis BETWEEN (:start) AND (:end) ORDER BY time_millis DESC")
    fun loadForTimeMillis(
        start: Long, end: Long, @MediaProviderOperation vararg operations: Int
    ): Cursor

    @Query("SELECT count(*) FROM MediaProviderRecord WHERE package_name IN (:packageNames) AND operation IN (:operation)")
    fun packageUsageTimes(@MediaProviderOperation operation: Int, vararg packageNames: String): Int

    @Insert
    fun insert(records: MediaProviderRecord)

    @Delete
    fun delete(record: MediaProviderRecord)
}

@IntDef(value = [OP_QUERY, OP_INSERT, OP_DELETE])
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
annotation class MediaProviderOperation {
    companion object {
        const val OP_QUERY = 0
        const val OP_INSERT = 1
        const val OP_DELETE = 2
    }
}

@Database(entities = [MediaProviderRecord::class], version = 2, exportSchema = false)
@TypeConverters(ListConverter::class)
abstract class MediaProviderRecordDatabase : RoomDatabase() {
    abstract fun mediaProviderRecordDao(): MediaProviderRecordDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(_db: SupportSQLiteDatabase) {
        _db.execSQL("DELETE FROM `MediaProviderQueryRecord`")
        _db.execSQL("DELETE FROM `MediaProviderInsertRecord`")
        _db.execSQL("DELETE FROM `MediaProviderDeleteRecord`")
        _db.execSQL("CREATE TABLE IF NOT EXISTS `MediaProviderRecord` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `time_millis` INTEGER NOT NULL, `package_name` TEXT NOT NULL, `match` INTEGER NOT NULL, `operation` INTEGER NOT NULL, `data` TEXT NOT NULL, `mime_type` TEXT NOT NULL, `intercepted` TEXT NOT NULL)")
    }
}
