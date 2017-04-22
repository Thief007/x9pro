package com.mediatek.beam;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import com.mediatek.beam.BeamShareTask.BeamShareTaskMetaData;
import java.util.HashMap;

public class BeamShareProvider extends ContentProvider {
    private static final UriMatcher URI_MATCHER = new UriMatcher(-1);
    private static HashMap<String, String> sProjectionMap = new HashMap();
    private SQLiteDatabase mDb;
    private DatabaseHelper mDbHelper;

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, "share.db", null, 1);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE share_tasks (_id INTEGER PRIMARY KEY,type INTEGER,state INTEGER,data TEXT,mime TEXT,total INTEGER,done INTEGER,modified INTEGER);");
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.d("@M_BeamShareProvider", "Upgrading database from version " + oldVersion + " to " + newVersion + " (will destroy all old data)!");
            db.execSQL("DROP TABLE IF EXISTS share_tasks");
            onCreate(db);
        }
    }

    static {
        sProjectionMap.put("_id", "_id");
        sProjectionMap.put("type", "type");
        sProjectionMap.put("state", "state");
        sProjectionMap.put("data", "data");
        sProjectionMap.put("mime", "mime");
        sProjectionMap.put("total", "total");
        sProjectionMap.put("done", "done");
        sProjectionMap.put("modified", "modified");
        URI_MATCHER.addURI("com.android.settings.provider.beam.share", "share_tasks", 1);
        URI_MATCHER.addURI("com.android.settings.provider.beam.share", "share_tasks/#", 2);
    }

    public boolean onCreate() {
        this.mDbHelper = new DatabaseHelper(getContext());
        this.mDb = this.mDbHelper.getWritableDatabase();
        return true;
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count;
        checkWritePermissions();
        this.mDb = this.mDbHelper.getWritableDatabase();
        switch (URI_MATCHER.match(uri)) {
            case 1:
                count = this.mDb.delete("share_tasks", selection, selectionArgs);
                break;
            case 2:
                String str;
                String rowId = (String) uri.getPathSegments().get(1);
                SQLiteDatabase sQLiteDatabase = this.mDb;
                String str2 = "share_tasks";
                StringBuilder append = new StringBuilder().append("_id=").append(rowId);
                if (TextUtils.isEmpty(selection)) {
                    str = "";
                } else {
                    str = " AND (" + selection + ')';
                }
                count = sQLiteDatabase.delete(str2, append.append(str).toString(), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        sendNotify(uri);
        return count;
    }

    public String getType(Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case 1:
                return "vnd.android.cursor.dir/vnd.mtkbeam.share.task";
            case 2:
                return "vnd.android.cursor.item/vnd.mtkbeam.share.task";
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    private void checkWritePermissions() {
        if (getContext().checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            throw new SecurityException(String.format("Permission denial: writing to secure settings requires %1$s", new Object[]{"android.permission.WRITE_SECURE_SETTINGS"}));
        }
    }

    public Uri insert(Uri url, ContentValues initialValues) {
        if (URI_MATCHER.match(url) != 1) {
            throw new IllegalArgumentException("Invalid URI: " + url);
        }
        checkWritePermissions();
        long rowId = this.mDbHelper.getWritableDatabase().insert("share_tasks", null, initialValues);
        if (rowId <= 0) {
            return null;
        }
        Log.v("@M_BeamShareProvider", "share_tasks <- " + initialValues);
        url = ContentUris.withAppendedId(BeamShareTaskMetaData.CONTENT_URI, rowId);
        getContext().getContentResolver().notifyChange(BeamShareTaskMetaData.CONTENT_URI, null);
        sendNotify(url);
        return url;
    }

    private void sendNotify(Uri uri) {
        String notify = uri.getQueryParameter("notify");
        if (notify == null || "true".equals(notify)) {
            getContext().getContentResolver().notifyChange(uri, null);
            Log.v("@M_BeamShareProvider", "notifying: " + uri);
            return;
        }
        Log.v("@M_BeamShareProvider", "notification suppressed: " + uri);
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        this.mDb = this.mDbHelper.getReadableDatabase();
        try {
            SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
            qb.setTables("share_tasks");
            qb.setProjectionMap(sProjectionMap);
            if (URI_MATCHER.match(uri) == 2) {
                qb.appendWhere("_id=" + ((String) uri.getPathSegments().get(1)));
            }
            String orderBy = sortOrder;
            if (TextUtils.isEmpty(sortOrder)) {
                orderBy = "modified DESC";
            }
            Cursor c = qb.query(this.mDb, projection, selection, selectionArgs, null, null, orderBy);
            if (c != null) {
                c.setNotificationUri(getContext().getContentResolver(), uri);
            }
            return c;
        } catch (SQLiteDiskIOException e) {
            Log.e("@M_BeamShareProvider", e.toString());
            return null;
        }
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count;
        checkWritePermissions();
        this.mDb = this.mDbHelper.getWritableDatabase();
        switch (URI_MATCHER.match(uri)) {
            case 1:
                count = this.mDb.update("share_tasks", values, selection, selectionArgs);
                break;
            case 2:
                String str;
                String rowId = (String) uri.getPathSegments().get(1);
                SQLiteDatabase sQLiteDatabase = this.mDb;
                String str2 = "share_tasks";
                StringBuilder append = new StringBuilder().append("_id=").append(rowId);
                if (TextUtils.isEmpty(selection)) {
                    str = "";
                } else {
                    str = " AND (" + selection + ')';
                }
                count = sQLiteDatabase.update(str2, values, append.append(str).toString(), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        sendNotify(uri);
        return count;
    }
}
