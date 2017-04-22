package com.android.settings.applock;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

public class AppListDaoImpl extends DataBaseDao {
    public AppListDaoImpl(Context context) {
        super(context);
    }

    public void delete(String packageName) {
        open();
        this.sqLiteDatabase.delete("app_list_table", "apactname=? ", new String[]{packageName});
    }

    public AppsInfoBean selectOne(AppsInfoBean appsInfoBean) {
        AppsInfoBean appsInfoBean2 = null;
        try {
            open();
            Cursor cursor = this.sqLiteDatabase.rawQuery("select *  from app_list_table where  apname=?  and  aunlock=? ;", new String[]{appsInfoBean.getPackageName(), "y"});
            if (cursor.moveToNext()) {
                AppsInfoBean appsInfoTemp = new AppsInfoBean();
                try {
                    appsInfoTemp.setPackageName(cursor.getString(cursor.getColumnIndex("apname")));
                    appsInfoTemp.setAppUnlock(cursor.getString(cursor.getColumnIndex("aunlock")));
                    appsInfoBean2 = appsInfoTemp;
                } catch (Exception e) {
                    return appsInfoTemp;
                }
            }
            if (cursor == null) {
                return appsInfoBean2;
            }
            if (cursor.isClosed()) {
                return appsInfoBean2;
            }
            cursor.close();
            return appsInfoBean2;
        } catch (Exception e2) {
            return appsInfoBean2;
        }
    }

    public AppsInfoBean selectAppLock(AppsInfoBean appsInfoBean) {
        AppsInfoBean appsInfoBean2 = null;
        try {
            open();
            Cursor cursor = this.sqLiteDatabase.rawQuery("select *  from app_list_table where  apactname=?  ;", new String[]{appsInfoBean.getPkgActivityName()});
            if (cursor.moveToNext()) {
                AppsInfoBean appsInfoTemp = new AppsInfoBean();
                try {
                    appsInfoTemp.setPackageName(cursor.getString(cursor.getColumnIndex("apname")));
                    appsInfoTemp.setAppUnlock(cursor.getString(cursor.getColumnIndex("aunlock")));
                    Log.i("zjy", "selectAppLock.getPackageName():" + appsInfoTemp.getPkgActivityName());
                    appsInfoBean2 = appsInfoTemp;
                } catch (Exception e) {
                    return appsInfoTemp;
                }
            }
            if (cursor == null) {
                return appsInfoBean2;
            }
            if (cursor.isClosed()) {
                return appsInfoBean2;
            }
            cursor.close();
            return appsInfoBean2;
        } catch (Exception e2) {
            return appsInfoBean2;
        }
    }

    public void update(String packageName) {
        open();
        this.sqLiteDatabase.execSQL("update app_list_table set aunlock =? where apname =?", new String[]{"n", packageName});
        Log.i("zjy", "更新成功！" + packageName);
    }

    public void update() {
        open();
        this.sqLiteDatabase.execSQL("update app_list_table set aunlock =? ", new String[]{"y"});
    }

    public void insert(AppsInfoBean appsInfoBean) {
        open();
        SQLiteStatement statement = this.sqLiteDatabase.compileStatement("insert into app_list_table(apname,apactname,aunlock ) values (?,?,?)");
        Log.i("zjy  insert", "appsInfoBean.getPackageName():" + appsInfoBean.getPkgActivityName());
        statement.bindString(1, appsInfoBean.getPackageName());
        statement.bindString(2, appsInfoBean.getPkgActivityName());
        statement.bindString(3, "y");
        statement.executeInsert();
    }
}
