package com.mediatek.systemui.floatpanel;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;
import com.mediatek.multiwindow.MultiWindowProxy;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FloatModel extends BroadcastReceiver {
    private static final Uri CONTENT_URI = Uri.parse("content://com.android.systemui.floatwindow/float?notify=true");
    private static Context mContext;
    private static List<FloatAppItem> mExtentAppList;
    private static FloatPanelView mFloatPanelView;
    private static FloatWindowProvider mFloatWindowProvider;
    private static List<FloatAppItem> mResidentAppList;
    private static final Handler sFloatHandler = new Handler(sFloatThread.getLooper());
    private static final HandlerThread sFloatThread = new HandlerThread("float-loader");
    private List<FloatAppItem> mAddedList = new ArrayList();
    private List<FloatAppItem> mDeletedList = new ArrayList();
    private List<FloatAppItem> mModifiedList = new ArrayList();
    private Handler mUiHandler = new Handler();

    private class PackageUpdatedTask implements Runnable {
        int mOp;
        String[] mPackages;

        public PackageUpdatedTask(int op, String[] packages) {
            this.mOp = op;
            this.mPackages = packages;
        }

        public void run() {
            int i;
            Context context = FloatModel.mContext;
            String[] packages = this.mPackages;
            int N = packages.length;
            switch (this.mOp) {
                case 1:
                    for (i = 0; i < N; i++) {
                        Log.d("FloatModel", "addPackage " + packages[i]);
                        FloatModel.this.addPackage(FloatModel.mContext, packages[i]);
                    }
                    break;
                case 2:
                    for (i = 0; i < N; i++) {
                        Log.d("FloatModel", "updatePackage " + packages[i]);
                        FloatModel.this.updatePackage(FloatModel.mContext, packages[i]);
                    }
                    break;
                case 3:
                case 4:
                    for (i = 0; i < N; i++) {
                        Log.d("FloatModel", "removePackage " + packages[i]);
                        FloatModel.this.removePackage(packages[i]);
                    }
                    break;
            }
            if (FloatModel.mFloatPanelView != null) {
                FloatModel.this.runOnMainThread(new Runnable() {
                    public void run() {
                        FloatModel.mFloatPanelView.refreshUI();
                        Log.d("FloatModel", "mFloatPanelView refreshUI");
                    }
                });
            }
            if (FloatModel.this.mAddedList.size() > 0) {
                for (i = 0; i < FloatModel.this.mAddedList.size(); i++) {
                    FloatModel.this.addItemToDatabase((FloatAppItem) FloatModel.this.mAddedList.get(i));
                }
            }
            if (FloatModel.this.mDeletedList.size() > 0) {
                for (i = 0; i < FloatModel.this.mDeletedList.size(); i++) {
                    FloatModel.this.deleteItemToDatabase((FloatAppItem) FloatModel.this.mDeletedList.get(i));
                }
            }
        }
    }

    static {
        sFloatThread.start();
    }

    public FloatModel(FloatPanelView floatPanelView) {
        if (mFloatWindowProvider == null) {
            mFloatWindowProvider = new FloatWindowProvider();
        }
        mFloatPanelView = floatPanelView;
        mContext = mFloatPanelView.getContext();
    }

    protected void addItemToModifyListIfNeeded(FloatAppItem item) {
        int size = this.mModifiedList.size();
        int i = 0;
        while (i < size) {
            FloatAppItem appItem = (FloatAppItem) this.mModifiedList.get(i);
            if (appItem.className.equals(item.className)) {
                appItem.position = item.position;
                appItem.container = item.container;
                break;
            }
            i++;
        }
        if (i == size) {
            this.mModifiedList.add(item);
        }
    }

    protected void commitModify() {
        int size = this.mModifiedList.size();
        for (int i = 0; i < size; i++) {
            FloatAppItem item = (FloatAppItem) this.mModifiedList.get(i);
            Log.d("FloatModel", "Modify item: i = " + i + ", position = " + item.position + ", container = " + item.container + ", className = " + item.className);
            modifyItemToDatabase(item);
        }
        this.mModifiedList.clear();
    }

    protected void modifyItemToDatabase(FloatAppItem item) {
        final ContentResolver contentResolver = mContext.getContentResolver();
        final ContentValues values = new ContentValues();
        final String componentName = "ComponentInfo{" + item.packageName + "/" + item.className + "}";
        values.put("position", Integer.valueOf(item.position));
        values.put("floatContainer", Integer.valueOf(item.container));
        Log.d("FloatModel", "modifyItemToDatabase: componentName = " + componentName + ", position = " + item.position + ", floatContainer = " + item.container);
        runOnWorkerThread(new Runnable() {
            public void run() {
                contentResolver.update(FloatModel.CONTENT_URI, values, "componentName = ?", new String[]{componentName});
            }
        });
    }

    protected void deleteItemToDatabase(FloatAppItem item) {
        final ContentResolver contentResolver = mContext.getContentResolver();
        final String componentName = "ComponentInfo{" + item.packageName + "/" + item.className + "}";
        runOnWorkerThread(new Runnable() {
            public void run() {
                contentResolver.delete(FloatModel.CONTENT_URI, "componentName = ?", new String[]{componentName});
            }
        });
        Log.d("FloatModel", "deleteItemToDatabase. componentName = " + componentName);
    }

    protected void addItemToDatabase(FloatAppItem item) {
        final ContentResolver contentResolver = mContext.getContentResolver();
        final ContentValues values = new ContentValues();
        String componentName = "ComponentInfo{" + item.packageName + "/" + item.className + "}";
        Intent intent = new Intent("android.intent.action.MAIN", null);
        intent.addCategory("android.intent.category.LAUNCHER");
        intent.setComponent(new ComponentName(item.packageName, item.className));
        intent.setFlags(270532608);
        values.put("_id", Long.valueOf(mFloatWindowProvider.generateNewId()));
        values.put("componentName", componentName);
        values.put("floatContainer", Integer.valueOf(item.container));
        values.put("position", Integer.valueOf(item.position));
        values.put("intent", intent.toUri(0));
        runOnWorkerThread(new Runnable() {
            public void run() {
                contentResolver.insert(FloatModel.CONTENT_URI, values);
            }
        });
        Log.d("FloatModel", "addItemToDatabase. componentName = " + componentName);
    }

    private void runOnMainThread(Runnable r) {
        if (sFloatThread.getThreadId() == Process.myTid()) {
            this.mUiHandler.post(r);
        } else {
            r.run();
        }
    }

    private static void runOnWorkerThread(Runnable r) {
        if (sFloatThread.getThreadId() == Process.myTid()) {
            r.run();
        } else {
            sFloatHandler.post(r);
        }
    }

    protected List<FloatAppItem> getFloatApps() {
        if (mResidentAppList == null) {
            loadAllApps();
        }
        return mResidentAppList;
    }

    protected List<FloatAppItem> getEditApps() {
        if (mExtentAppList == null) {
            loadAllApps();
        }
        return mExtentAppList;
    }

    private void loadAllApps() {
        ContentResolver contentResolver = mContext.getContentResolver();
        PackageManager packageManager = mContext.getPackageManager();
        boolean loadDefault = mFloatWindowProvider.loadDefaultAllAppsIfNecessary(mContext);
        Cursor cursor = contentResolver.query(CONTENT_URI, null, null, null, null);
        Log.d("FloatModel", ",loadAllApps, cursor  : " + cursor);
        if (cursor != null) {
            try {
                int componentNameIndex = cursor.getColumnIndexOrThrow("componentName");
                int intentIndex = cursor.getColumnIndexOrThrow("intent");
                int positionIndex = cursor.getColumnIndexOrThrow("position");
                int floatContainerIndex = cursor.getColumnIndexOrThrow("floatContainer");
                Intent intent = new Intent();
                mResidentAppList = new ArrayList();
                mExtentAppList = new ArrayList();
                while (cursor.moveToNext()) {
                    String intentDescription = cursor.getString(intentIndex);
                    Log.d("FloatModel", "componentName :" + cursor.getString(componentNameIndex));
                    try {
                        intent = Intent.parseUri(intentDescription, 0);
                        ResolveInfo resolveInfo = packageManager.resolveActivity(intent, 0);
                        if (intent != null) {
                            int floatContainer = cursor.getInt(floatContainerIndex);
                            int floatPosition = cursor.getInt(positionIndex);
                            if (floatContainer == 1) {
                                mResidentAppList.add(new FloatAppItem(packageManager, resolveInfo, null, floatPosition));
                            } else if (floatContainer == 2) {
                                mExtentAppList.add(new FloatAppItem(packageManager, resolveInfo, null, floatPosition));
                            } else {
                                continue;
                            }
                        } else {
                            continue;
                        }
                    } catch (URISyntaxException e) {
                        Log.w("FloatModel", "loadAllApps, parse Intent Uri error: " + intentDescription);
                    }
                }
                Collections.sort(mResidentAppList, new Comparator<FloatAppItem>() {
                    public int compare(FloatAppItem f1, FloatAppItem f2) {
                        return f1.position - f2.position;
                    }
                });
                Collections.sort(mExtentAppList, new Comparator<FloatAppItem>() {
                    public int compare(FloatAppItem f1, FloatAppItem f2) {
                        return f1.position - f2.position;
                    }
                });
            } catch (Exception e2) {
            } finally {
                cursor.close();
            }
            Log.d("TAG", "Load all apps done");
        }
    }

    public void onReceive(Context context, Intent intent) {
        Log.d("FloatModel", "onReceive: intent = " + intent);
        mContext = context;
        if (mFloatWindowProvider == null) {
            mFloatWindowProvider = new FloatWindowProvider();
        }
        if (mResidentAppList == null && mExtentAppList == null) {
            mResidentAppList = getFloatApps();
            mExtentAppList = getEditApps();
        }
        String action = intent.getAction();
        String packageName;
        if ("android.intent.action.PACKAGE_CHANGED".equals(action) || "android.intent.action.PACKAGE_REMOVED".equals(action) || "android.intent.action.PACKAGE_ADDED".equals(action)) {
            if (intent.getData() != null) {
                packageName = intent.getData().getSchemeSpecificPart();
                boolean replacing = intent.getBooleanExtra("android.intent.extra.REPLACING", false);
                int op = 0;
                if (packageName != null && packageName.length() != 0) {
                    if ("android.intent.action.PACKAGE_CHANGED".equals(action)) {
                        op = 2;
                    } else if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
                        if (!replacing) {
                            op = 3;
                        }
                    } else if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                        op = !replacing ? 1 : 2;
                    }
                    if (op != 0) {
                        enqueuePackageUpdated(new PackageUpdatedTask(op, new String[]{packageName}));
                    }
                }
            }
        } else if ("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE".equals(action)) {
            enqueuePackageUpdated(new PackageUpdatedTask(1, intent.getStringArrayExtra("android.intent.extra.changed_package_list")));
        } else if ("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE".equals(action)) {
            enqueuePackageUpdated(new PackageUpdatedTask(4, intent.getStringArrayExtra("android.intent.extra.changed_package_list")));
        } else if ("android.intent.action.LOCALE_CHANGED".equals(action)) {
            runOnWorkerThread(new Runnable() {
                public void run() {
                    FloatModel.this.loadAllApps();
                }
            });
            if (mFloatPanelView != null) {
                runOnMainThread(new Runnable() {
                    public void run() {
                        FloatModel.mFloatPanelView.refreshUI();
                        Log.d("FloatModel", "mFloatPanelView refreshUI");
                    }
                });
            }
        } else if ("action_multiwindow_disable_pkg_updated".equals(action)) {
            packageName = intent.getStringExtra("packageName");
            if (packageName != null && packageName.length() != 0) {
                Log.d("FloatModel", "DISABLE_PKG_NAME=" + packageName);
                enqueuePackageUpdated(new PackageUpdatedTask(3, new String[]{packageName}));
            }
        }
    }

    void enqueuePackageUpdated(PackageUpdatedTask task) {
        sFloatHandler.post(task);
    }

    public void addPackage(Context context, String packageName) {
        if (MultiWindowProxy.getInstance().inWhiteList(packageName)) {
            List<ResolveInfo> matches = findActivitiesForPackage(context, packageName);
            Log.d("FloatModel", "addPackage: packageName = " + packageName + ", matches = " + matches.size());
            if (matches.size() > 0) {
                for (ResolveInfo info : matches) {
                    FloatAppItem appItem = new FloatAppItem(mContext.getPackageManager(), info, null, mExtentAppList.size());
                    appItem.container = 2;
                    mExtentAppList.add(appItem);
                    this.mAddedList.add(appItem);
                }
            }
        }
    }

    public void removePackage(String packageName) {
        int i;
        Log.d("FloatModel", "removePackage: packageName = " + packageName);
        for (i = 0; i < mResidentAppList.size(); i++) {
            FloatAppItem listItem = (FloatAppItem) mResidentAppList.get(i);
            if (packageName.equals(listItem.packageName)) {
                mResidentAppList.remove(i);
                this.mDeletedList.add(listItem);
            }
        }
        for (i = 0; i < mExtentAppList.size(); i++) {
            listItem = (FloatAppItem) mExtentAppList.get(i);
            if (packageName.equals(listItem.packageName)) {
                mExtentAppList.remove(i);
                this.mDeletedList.add(listItem);
            }
        }
    }

    public void updatePackage(Context context, String packageName) {
        List<ResolveInfo> matches = findActivitiesForPackage(context, packageName);
        Log.d("FloatModel", "updatePackage: packageName = " + packageName + ", matches = " + matches.size());
        if (matches.size() > 0) {
            int i;
            FloatAppItem listItem;
            for (i = 0; i < mResidentAppList.size(); i++) {
                listItem = (FloatAppItem) mResidentAppList.get(i);
                if (packageName.equals(listItem.packageName) && !findActivity(matches, listItem.className)) {
                    mResidentAppList.remove(i);
                    this.mDeletedList.add(listItem);
                }
            }
            for (i = 0; i < mExtentAppList.size(); i++) {
                listItem = (FloatAppItem) mExtentAppList.get(i);
                if (packageName.equals(listItem.packageName) && !findActivity(matches, listItem.className)) {
                    mExtentAppList.remove(i);
                    this.mDeletedList.add(listItem);
                }
            }
            return;
        }
        removePackage(packageName);
    }

    private static List<ResolveInfo> findActivitiesForPackage(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        Intent mainIntent = new Intent("android.intent.action.MAIN", null);
        mainIntent.addCategory("android.intent.category.LAUNCHER");
        mainIntent.setPackage(packageName);
        List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent, 0);
        return apps != null ? apps : new ArrayList();
    }

    private static boolean findActivity(List<ResolveInfo> apps, String className) {
        for (ResolveInfo info : apps) {
            if (info.activityInfo.name.equals(className)) {
                return true;
            }
        }
        return false;
    }
}
