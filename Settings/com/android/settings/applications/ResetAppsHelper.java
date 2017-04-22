package com.android.settings.applications;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.AppOpsManager;
import android.app.INotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.IPackageManager.Stub;
import android.content.pm.PackageManager;
import android.net.NetworkPolicyManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import com.android.settings.R;
import java.util.List;

public class ResetAppsHelper implements OnClickListener, OnDismissListener {
    private final AppOpsManager mAom;
    private final Context mContext;
    private final IPackageManager mIPm = Stub.asInterface(ServiceManager.getService("package"));
    private final INotificationManager mNm = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
    private final NetworkPolicyManager mNpm;
    private final PackageManager mPm;
    private AlertDialog mResetDialog;

    class C02801 implements Runnable {
        C02801() {
        }

        public void run() {
            List<ApplicationInfo> apps = ResetAppsHelper.this.mPm.getInstalledApplications(512);
            for (int i = 0; i < apps.size(); i++) {
                ApplicationInfo app = (ApplicationInfo) apps.get(i);
                try {
                    ResetAppsHelper.this.mNm.setNotificationsEnabledForPackage(app.packageName, app.uid, true);
                } catch (RemoteException e) {
                }
                if (!app.enabled && ResetAppsHelper.this.mPm.getApplicationEnabledSetting(app.packageName) == 3) {
                    ResetAppsHelper.this.mPm.setApplicationEnabledSetting(app.packageName, 0, 1);
                }
            }
            try {
                ResetAppsHelper.this.mIPm.resetApplicationPreferences(UserHandle.myUserId());
            } catch (RemoteException e2) {
            }
            ResetAppsHelper.this.mAom.resetAllModes();
            int[] restrictedUids = ResetAppsHelper.this.mNpm.getUidsWithPolicy(1);
            int currentUserId = ActivityManager.getCurrentUser();
            for (int uid : restrictedUids) {
                if (UserHandle.getUserId(uid) == currentUserId) {
                    ResetAppsHelper.this.mNpm.setUidPolicy(uid, 0);
                }
            }
        }
    }

    public ResetAppsHelper(Context context) {
        this.mContext = context;
        this.mPm = context.getPackageManager();
        this.mNpm = NetworkPolicyManager.from(context);
        this.mAom = (AppOpsManager) context.getSystemService("appops");
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.getBoolean("resetDialog")) {
            buildResetDialog();
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        if (this.mResetDialog != null) {
            outState.putBoolean("resetDialog", true);
        }
    }

    public void stop() {
        if (this.mResetDialog != null) {
            this.mResetDialog.dismiss();
            this.mResetDialog = null;
        }
    }

    void buildResetDialog() {
        if (this.mResetDialog == null) {
            this.mResetDialog = new Builder(this.mContext).setTitle(R.string.reset_app_preferences_title).setMessage(R.string.reset_app_preferences_desc).setPositiveButton(R.string.reset_app_preferences_button, this).setNegativeButton(R.string.cancel, null).setOnDismissListener(this).show();
        }
    }

    public void onDismiss(DialogInterface dialog) {
        if (this.mResetDialog == dialog) {
            this.mResetDialog = null;
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        if (this.mResetDialog == dialog) {
            AsyncTask.execute(new C02801());
        }
    }
}
