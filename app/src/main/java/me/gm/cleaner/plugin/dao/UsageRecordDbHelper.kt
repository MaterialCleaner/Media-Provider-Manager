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
