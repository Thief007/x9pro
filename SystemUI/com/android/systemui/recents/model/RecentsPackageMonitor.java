package com.android.systemui.recents.model;

import android.content.ComponentName;
import android.content.Context;
import android.os.Looper;
import android.os.UserHandle;
import com.android.internal.content.PackageMonitor;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.Task.TaskKey;
import java.util.HashSet;
import java.util.List;

public class RecentsPackageMonitor extends PackageMonitor {
    PackageCallbacks mCb;
    SystemServicesProxy mSystemServicesProxy;

    public interface PackageCallbacks {
        void onPackagesChanged(RecentsPackageMonitor recentsPackageMonitor, String str, int i);
    }

    public void register(Context context, PackageCallbacks cb) {
        this.mSystemServicesProxy = new SystemServicesProxy(context);
        this.mCb = cb;
        try {
            register(context, Looper.getMainLooper(), UserHandle.ALL, true);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void unregister() {
        try {
            super.unregister();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        this.mSystemServicesProxy = null;
        this.mCb = null;
    }

    public void onPackageRemoved(String packageName, int uid) {
        if (this.mCb != null) {
            this.mCb.onPackagesChanged(this, packageName, getChangingUserId());
        }
    }

    public boolean onPackageChanged(String packageName, int uid, String[] components) {
        onPackageModified(packageName);
        return true;
    }

    public void onPackageModified(String packageName) {
        if (this.mCb != null) {
            this.mCb.onPackagesChanged(this, packageName, getChangingUserId());
        }
    }

    public HashSet<ComponentName> computeComponentsRemoved(List<TaskKey> taskKeys, String packageName, int userId) {
        HashSet<ComponentName> existingComponents = new HashSet();
        HashSet<ComponentName> removedComponents = new HashSet();
        for (TaskKey t : taskKeys) {
            if (t.userId == userId) {
                ComponentName cn = t.baseIntent.getComponent();
                if (cn.getPackageName().equals(packageName) && !existingComponents.contains(cn)) {
                    if (this.mSystemServicesProxy.getActivityInfo(cn, userId) != null) {
                        existingComponents.add(cn);
                    } else {
                        removedComponents.add(cn);
                    }
                }
            }
        }
        return removedComponents;
    }
}
