package com.android.settings.applock;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBAdapter {
    private static DBHelper helper = null;
    private Context context;
    private SQLiteDatabase sqLiteDatabase;

    private class DBHelper extends SQLiteOpenHelper {
        public DBHelper(Context context) {
            super(context, "fingerinfo.db", null, 1);
        }

        public void onCreate(SQLiteDatabase db) {
            db.beginTransaction();
            db.execSQL("create table app_list_table ( id  INTEGER PRIMARY KEY  , apname , apactname , aunlock )");
            db.execSQL("create table finger_table ( id  INTEGER PRIMARY KEY  , fname_id , fcontext BLOB  , ftype )");
            db.execSQL("create table finger_name_table ( id  INTEGER PRIMARY KEY  , fname )");
            db.execSQL("create table finger_open_fingers_table(id  INTEGER PRIMARY KEY  , fname , fcontext BLOB  , fapp_id INTEGER  ,fapp_use_flag )");
            db.execSQL("create table finger_open_apps_table(id  INTEGER PRIMARY KEY  , fapp , fapp_classname , fapp_package )");
            db.setTransactionSuccessful();
            db.endTransaction();
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }

    public DBAdapter(Context context) {
        this.context = context;
        if (helper == null) {
            helper = new DBHelper(context);
        }
        this.sqLiteDatabase = helper.getWritableDatabase();
    }

    public SQLiteDatabase getSqLiteDatabase() {
        return this.sqLiteDatabase;
    }
}
