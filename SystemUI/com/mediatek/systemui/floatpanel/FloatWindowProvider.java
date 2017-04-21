package com.mediatek.systemui.floatpanel;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import com.android.systemui.R;
import com.android.systemui.R$styleable;
import com.mediatek.multiwindow.MultiWindowProxy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

public class FloatWindowProvider extends ContentProvider {
    private static int mCellIndex = 0;
    private static Context mContext;
    private static long mMaxIdInAllAppsList = -1;
    private static DatabaseHelper sOpenHelper;

    static class DatabaseHelper extends SQLiteOpenHelper {
        private final Context mContext;
        private long mMaxId = -1;

        DatabaseHelper(Context context) {
            super(context, "float.db", null, 12);
            Log.d("FloatWindowProvider", "DatabaseHelper**");
            this.mContext = context;
            if (this.mMaxId == -1) {
                this.mMaxId = initializeMaxId(getWritableDatabase());
                Log.d("FloatWindowProvider", "DatabaseHelper_mMaxId=**" + this.mMaxId);
            }
        }

        public void onCreate(SQLiteDatabase db) {
            Log.d("FloatWindowProvider", "creating new float database");
            Log.d("FloatWindowProvider", "DatabaseHelper_*onCreate*");
            db.execSQL("CREATE TABLE float (_id INTEGER PRIMARY KEY,componentName TEXT,intent TEXT,position INTEGER,floatContainer INTEGER NOT NULL DEFAULT 1);");
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }

        public long generateNewId() {
            if (this.mMaxId < 0) {
                throw new RuntimeException("Error: max id was not initialized");
            }
            this.mMaxId++;
            return this.mMaxId;
        }

        private long initializeMaxId(SQLiteDatabase db) {
            Cursor c = db.rawQuery("SELECT MAX(_id) FROM float", null);
            long id = -1;
            if (c != null && c.moveToNext()) {
                id = c.getLong(0);
            }
            if (c != null) {
                c.close();
            }
            if (id != -1) {
                return id;
            }
            throw new RuntimeException("Error: could not query max id");
        }

        public void loadDefaultAllAppsList(SQLiteDatabase db) {
            Log.d("FloatWindowProvider", "loadDefaultAllAppsList begin");
            Log.d("FloatWindowProvider", "DatabaseHelper_*loadDefaultAllAppsList_begin*");
            Intent intent = new Intent("android.intent.action.MAIN", null);
            intent.addCategory("android.intent.category.LAUNCHER");
            PackageManager packageManager = this.mContext.getPackageManager();
            List<ResolveInfo> apps = packageManager.queryIntentActivities(intent, 0);
            if (apps == null || apps.size() == 0) {
                Log.e("FloatWindowProvider", "queryIntentActivities got null or zero!");
            }
            List<ComponentName> extentComponentNames = new ArrayList();
            List<ComponentName> residentComponentNames = loadResidentComponents(db);
            for (ResolveInfo info : apps) {
                String packageName = info.activityInfo.applicationInfo.packageName;
                if (MultiWindowProxy.getInstance().inWhiteList(packageName)) {
                    extentComponentNames.add(new ComponentName(packageName, info.activityInfo.name));
                }
            }
            extentComponentNames.removeAll(residentComponentNames);
            MultiWindowProxy mwProxy = MultiWindowProxy.getInstance();
            ContentValues values = new ContentValues();
            long count = 0;
            for (ComponentName componentName : extentComponentNames) {
                values.clear();
                long count2 = count + 1;
                values.put("position", Long.valueOf(count));
                values.put("floatContainer", Integer.valueOf(2));
                if (addItemToAllAppsList(db, values, packageManager, intent, componentName) < 0) {
                    count = count2 - 1;
                } else {
                    count = count2;
                }
            }
            Log.d("FloatWindowProvider", "loadDefaultAllAppsList, query PMS got extent = " + extentComponentNames.size() + ", resident = " + residentComponentNames.size());
        }

        private List<ComponentName> loadResidentComponents(SQLiteDatabase db) {
            List<ComponentName> residentComponentNames = new ArrayList();
            try {
                XmlResourceParser parser = this.mContext.getResources().getXml(R.xml.default_residentpackage);
                AttributeSet attrs = Xml.asAttributeSet(parser);
                XmlUtils.beginDocument(parser, "residentpackages");
                int depth = parser.getDepth();
                ContentValues values = new ContentValues();
                PackageManager packageManager = this.mContext.getPackageManager();
                while (true) {
                    int type = parser.next();
                    if ((type == 3 && parser.getDepth() <= depth) || type == 1) {
                        break;
                    } else if (type == 2) {
                        TypedArray a = this.mContext.obtainStyledAttributes(attrs, R$styleable.ResidentPackage);
                        ComponentName componentName = new ComponentName(a.getString(0), a.getString(1));
                        String packageName = componentName.getPackageName();
                        if (MultiWindowProxy.getInstance().inWhiteList(packageName)) {
                            Log.d("FloatWindowProvider", "loadResidentPackage: packageName = " + packageName);
                            residentComponentNames.add(componentName);
                            Intent intent = new Intent("android.intent.action.MAIN", null);
                            intent.addCategory("android.intent.category.LAUNCHER");
                            values.clear();
                            values.put("position", Integer.valueOf(a.getInt(2, 0)));
                            values.put("floatContainer", Integer.valueOf(1));
                            addItemToAllAppsList(db, values, packageManager, intent, componentName);
                            Log.d("FloatWindowProvider", "loadResidentPackage: packageName = " + a.getString(0) + ", className = " + a.getString(1));
                            a.recycle();
                        }
                    }
                }
            } catch (XmlPullParserException e) {
                Log.w("FloatWindowProvider", "Got XmlPullParserException while parsing toppackage.", e);
            } catch (IOException e2) {
                Log.w("FloatWindowProvider", "Got IOException while parsing toppackage.", e2);
            }
            return residentComponentNames;
        }

        private long addItemToAllAppsList(SQLiteDatabase db, ContentValues values, PackageManager packageManager, Intent intent, ComponentName componentName) {
            ActivityInfo activityInfo = null;
            try {
                activityInfo = packageManager.getActivityInfo(componentName, 0);
            } catch (NameNotFoundException e) {
                Log.w("FloatWindowProvider", "Can not add such application: " + componentName);
            }
            if (activityInfo == null) {
                return -1;
            }
            intent.setComponent(componentName);
            intent.setFlags(270532608);
            values.put("_id", Long.valueOf(generateNewId()));
            values.put("componentName", componentName.toString());
            values.put("intent", intent.toUri(0));
            Log.d("FloatWindowProvider", "Load app item ,intent = " + intent + ",componentName = " + componentName);
            if (FloatWindowProvider.dbInsertAndCheck(this, db, "float", null, values) < 0) {
                Log.w("FloatWindowProvider", "Insert app item (" + values + ") to database failed.");
                return -1;
            }
            FloatWindowProvider.mCellIndex = FloatWindowProvider.mCellIndex + 1;
            return 1;
        }
    }

    static class SqlArguments {
        public final String[] args;
        public final String table;
        public final String where;

        SqlArguments(Uri url, String where, String[] args) {
            if (url.getPathSegments().size() == 1) {
                this.table = (String) url.getPathSegments().get(0);
                this.where = where;
                this.args = args;
            } else if (url.getPathSegments().size() != 2) {
                throw new IllegalArgumentException("Invalid URI: " + url);
            } else if (TextUtils.isEmpty(where)) {
                this.table = (String) url.getPathSegments().get(0);
                this.where = "_id=" + ContentUris.parseId(url);
                this.args = null;
            } else {
                throw new UnsupportedOperationException("WHERE clause not supported: " + url);
            }
        }

        SqlArguments(Uri url) {
            if (url.getPathSegments().size() == 1) {
                this.table = (String) url.getPathSegments().get(0);
                this.where = null;
                this.args = null;
                return;
            }
            throw new IllegalArgumentException("Invalid URI: " + url);
        }
    }

    public boolean onCreate() {
        Log.d("FloatWindowProvider", "FloatWindowProvider_onCreate**");
        mContext = getContext();
        sOpenHelper = new DatabaseHelper(mContext);
        return true;
    }

    public String getType(Uri uri) {
        SqlArguments args = new SqlArguments(uri, null, null);
        if (TextUtils.isEmpty(args.where)) {
            return "vnd.android.cursor.dir/" + args.table;
        }
        return "vnd.android.cursor.item/" + args.table;
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log.d("FloatWindowProvider", "FloatWindowProvider_query**");
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(args.table);
        Cursor result = qb.query(sOpenHelper.getWritableDatabase(), projection, args.where, args.args, null, null, sortOrder);
        if (result != null) {
            result.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return result;
    }

    private static long dbInsertAndCheck(DatabaseHelper helper, SQLiteDatabase db, String table, String nullColumnHack, ContentValues values) {
        if (values.containsKey("componentName")) {
            return db.insert(table, nullColumnHack, values);
        }
        throw new RuntimeException("Error: attempting to add item without specifying an componentName");
    }

    public Uri insert(Uri uri, ContentValues initialValues) {
        Log.d("FloatWindowProvider", "FloatWindowProvider_insert**");
        SqlArguments args = new SqlArguments(uri);
        long rowId = dbInsertAndCheck(sOpenHelper, sOpenHelper.getWritableDatabase(), args.table, null, initialValues);
        if (rowId <= 0) {
            return null;
        }
        uri = ContentUris.withAppendedId(uri, rowId);
        sendNotify(uri);
        return uri;
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        int count = sOpenHelper.getWritableDatabase().delete(args.table, args.where, args.args);
        if (count > 0) {
            sendNotify(uri);
        }
        return count;
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Log.d("FloatWindowProvider", "FloatWindowProvider_update**");
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        int count = sOpenHelper.getWritableDatabase().update(args.table, values, args.where, args.args);
        if (count > 0) {
            sendNotify(uri);
        }
        return count;
    }

    private void sendNotify(Uri uri) {
        String notify = uri.getQueryParameter("notify");
        if (notify == null || "true".equals(notify)) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
    }

    public long generateNewId() {
        return sOpenHelper.generateNewId();
    }

    public synchronized boolean loadDefaultAllAppsIfNecessary(Context context) {
        boolean loadDefault;
        Log.d("FloatWindowProvider", "loadDefaultAllAppsIfNecessary: context = " + context + "this =" + this + ",  context.getPackageManager()=" + context.getPackageManager());
        boolean isSafeMode = context.getPackageManager().isSafeMode();
        SharedPreferences sharedPreferences = context.getSharedPreferences("mediatek_float_info", 0);
        Log.d("FloatWindowProvider", "loadDefaultAllAppsIfNecessary: sharedPreferences = " + sharedPreferences + "this =" + this);
        loadDefault = sharedPreferences.getBoolean("com.mediatek.float.allapps.not.loaded", true);
        Log.d("FloatWindowProvider", "loadDefaultAllAppsIfNecessary: loadDefault = " + loadDefault + ",mMaxIdInAllAppsList = " + mMaxIdInAllAppsList + ",isSafeMode =" + isSafeMode);
        if (loadDefault || isSafeMode) {
            Editor editor;
            if (isSafeMode) {
                sOpenHelper.getWritableDatabase().delete("float", null, null);
                editor = sharedPreferences.edit();
                editor.putBoolean("com.mediatek.float.allapps.not.loaded", true);
                editor.commit();
            } else {
                Log.d("FloatWindowProvider", "sOpenHelpert= " + sOpenHelper + ",getWritableDatabase=" + sOpenHelper.getWritableDatabase());
                sOpenHelper.getWritableDatabase().delete("float", null, null);
                editor = sharedPreferences.edit();
                editor.putBoolean("com.mediatek.float.allapps.not.loaded", false);
                editor.commit();
            }
            Log.d("FloatWindowProvider", "loadDefaultAllAppsIfNecessary: loadDefaultAllAppsList = " + loadDefault + ",mMaxIdInAllAppsList = " + mMaxIdInAllAppsList);
            sOpenHelper.loadDefaultAllAppsList(sOpenHelper.getWritableDatabase());
        }
        return loadDefault;
    }
}
