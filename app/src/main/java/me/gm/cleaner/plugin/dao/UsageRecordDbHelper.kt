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

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class UsageRecordDbHelper(context: Context) :
    SQLiteOpenHelper(context, "UsageRecord.db", null, 1) {

    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        // TODO: record query or insert
        // TODO: do we need two tables?
        sqLiteDatabase.execSQL("create table if not exists media(id integer primary key autoincrement, _display_name varchar(255), mime_type varchar(255), relative_path varchar(255))")
                                                                                                                      // mkdir,rename,createFile  // method returned true or false
        sqLiteDatabase.execSQL("create table if not exists file(id integer primary key autoincrement, path varchar(255), method varchar(255), result varchar(255))")
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, i: Int, i1: Int) {}
}
