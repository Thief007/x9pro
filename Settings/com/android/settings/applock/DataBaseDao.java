package com.android.settings.applock;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public abstract class DataBaseDao {
    protected DBAdapter adapter = null;
    protected Context context = null;
    protected SQLiteDatabase sqLiteDatabase = null;

    public abstract void delete(String str);

    public abstract void insert(AppsInfoBean appsInfoBean);

    public abstract AppsInfoBean selectAppLock(AppsInfoBean appsInfoBean);

    public abstract AppsInfoBean selectOne(AppsInfoBean appsInfoBean);

    public abstract void update();

    public abstract void update(String str);

    public DataBaseDao(Context context) {
        this.context = context;
        this.adapter = new DBAdapter(context);
        this.sqLiteDatabase = this.adapter.getSqLiteDatabase();
    }

    protected void open() {
        if (this.sqLiteDatabase == null || !this.sqLiteDatabase.isOpen()) {
            this.sqLiteDatabase = new DBAdapter(this.context).getSqLiteDatabase();
        }
    }
}
