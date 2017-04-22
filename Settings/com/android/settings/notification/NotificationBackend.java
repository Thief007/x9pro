package com.android.settings.notification;

import android.app.INotificationManager;
import android.app.INotificationManager.Stub;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.ServiceManager;
import android.util.Log;

public class NotificationBackend {
    static INotificationManager sINM = Stub.asInterface(ServiceManager.getService("notification"));

    static class Row {
        Row() {
        }
    }

    public static class AppRow extends Row {
        public boolean banned;
        public Drawable icon;
        public CharSequence label;
        public boolean peekable;
        public String pkg;
        public boolean priority;
        public boolean sensitive;
        public Intent settingsIntent;
        public int uid;
    }

    public AppRow loadAppRow(PackageManager pm, ApplicationInfo app) {
        AppRow row = new AppRow();
        row.pkg = app.packageName;
        row.uid = app.uid;
        try {
            row.label = app.loadLabel(pm);
        } catch (Throwable t) {
            Log.e("NotificationBackend", "Error loading application label for " + row.pkg, t);
            row.label = row.pkg;
        }
        row.icon = app.loadIcon(pm);
        row.banned = getNotificationsBanned(row.pkg, row.uid);
        row.priority = getHighPriority(row.pkg, row.uid);
        row.peekable = getPeekable(row.pkg, row.uid);
        row.sensitive = getSensitive(row.pkg, row.uid);
        return row;
    }

    public boolean setNotificationsBanned(String pkg, int uid, boolean banned) {
        try {
            boolean z;
            INotificationManager iNotificationManager = sINM;
            if (banned) {
                z = false;
            } else {
                z = true;
            }
            iNotificationManager.setNotificationsEnabledForPackage(pkg, uid, z);
            return true;
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
            return false;
        }
    }

    public boolean getNotificationsBanned(String pkg, int uid) {
        boolean z = false;
        try {
            if (!sINM.areNotificationsEnabledForPackage(pkg, uid)) {
                z = true;
            }
            return z;
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
            return false;
        }
    }

    public boolean getHighPriority(String pkg, int uid) {
        boolean z = false;
        try {
            if (sINM.getPackagePriority(pkg, uid) == 2) {
                z = true;
            }
            return z;
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
            return false;
        }
    }

    public boolean setHighPriority(String pkg, int uid, boolean highPriority) {
        try {
            int i;
            INotificationManager iNotificationManager = sINM;
            if (highPriority) {
                i = 2;
            } else {
                i = 0;
            }
            iNotificationManager.setPackagePriority(pkg, uid, i);
            return true;
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
            return false;
        }
    }

    public boolean getPeekable(String pkg, int uid) {
        try {
            return sINM.getPackagePeekable(pkg, uid);
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
            return false;
        }
    }

    public boolean setPeekable(String pkg, int uid, boolean peekable) {
        try {
            sINM.setPackagePeekable(pkg, uid, peekable);
            return true;
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
            return false;
        }
    }

    public boolean getSensitive(String pkg, int uid) {
        boolean z = false;
        try {
            if (sINM.getPackageVisibilityOverride(pkg, uid) == 0) {
                z = true;
            }
            return z;
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
            return false;
        }
    }

    public boolean setSensitive(String pkg, int uid, boolean sensitive) {
        try {
            int i;
            INotificationManager iNotificationManager = sINM;
            if (sensitive) {
                i = 0;
            } else {
                i = -1000;
            }
            iNotificationManager.setPackageVisibilityOverride(pkg, uid, i);
            return true;
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
            return false;
        }
    }
}
