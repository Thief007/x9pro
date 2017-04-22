package com.android.settings.location;

import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.AppOpsManager.OpEntry;
import android.app.AppOpsManager.PackageOps;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import com.android.settings.DimmableIconPreference;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.applications.InstalledAppDetails;
import java.util.ArrayList;
import java.util.List;

public class RecentLocationApps {
    private static final String TAG = RecentLocationApps.class.getSimpleName();
    private final SettingsActivity mActivity;
    private final PackageManager mPackageManager;

    private class PackageEntryClickedListener implements OnPreferenceClickListener {
        private String mPackage;
        private UserHandle mUserHandle;

        public PackageEntryClickedListener(String packageName, UserHandle userHandle) {
            this.mPackage = packageName;
            this.mUserHandle = userHandle;
        }

        public boolean onPreferenceClick(Preference preference) {
            Bundle args = new Bundle();
            args.putString("package", this.mPackage);
            RecentLocationApps.this.mActivity.startPreferencePanelAsUser(InstalledAppDetails.class.getName(), args, R.string.application_info_label, null, this.mUserHandle);
            return true;
        }
    }

    public RecentLocationApps(SettingsActivity activity) {
        this.mActivity = activity;
        this.mPackageManager = activity.getPackageManager();
    }

    private DimmableIconPreference createRecentLocationEntry(Drawable icon, CharSequence label, boolean isHighBattery, CharSequence contentDescription, OnPreferenceClickListener listener) {
        DimmableIconPreference pref = new DimmableIconPreference(this.mActivity, contentDescription);
        pref.setIcon(icon);
        pref.setTitle(label);
        if (isHighBattery) {
            pref.setSummary(R.string.location_high_battery_use);
        } else {
            pref.setSummary(R.string.location_low_battery_use);
        }
        pref.setOnPreferenceClickListener(listener);
        return pref;
    }

    public List<Preference> getAppList() {
        List<PackageOps> appOps = ((AppOpsManager) this.mActivity.getSystemService("appops")).getPackagesForOps(new int[]{41, 42});
        ArrayList<Preference> prefs = new ArrayList();
        long now = System.currentTimeMillis();
        UserManager um = (UserManager) this.mActivity.getSystemService("user");
        List<UserHandle> profiles = um.getUserProfiles();
        int appOpsN = appOps.size();
        for (int i = 0; i < appOpsN; i++) {
            PackageOps ops = (PackageOps) appOps.get(i);
            String packageName = ops.getPackageName();
            int uid = ops.getUid();
            int userId = UserHandle.getUserId(uid);
            if (!(uid == 1000 ? "android".equals(packageName) : false) && profiles.contains(new UserHandle(userId))) {
                Preference preference = getPreferenceFromOps(um, now, ops);
                if (preference != null) {
                    prefs.add(preference);
                }
            }
        }
        return prefs;
    }

    private Preference getPreferenceFromOps(UserManager um, long now, PackageOps ops) {
        String packageName = ops.getPackageName();
        boolean highBattery = false;
        boolean normalBattery = false;
        long recentLocationCutoffTime = now - 900000;
        for (OpEntry entry : ops.getOps()) {
            if (entry.isRunning() || entry.getTime() >= recentLocationCutoffTime) {
                switch (entry.getOp()) {
                    case 41:
                        normalBattery = true;
                        break;
                    case 42:
                        highBattery = true;
                        break;
                    default:
                        break;
                }
            }
        }
        if (highBattery || normalBattery) {
            int userId = UserHandle.getUserId(ops.getUid());
            DimmableIconPreference preference = null;
            try {
                ApplicationInfo appInfo = AppGlobals.getPackageManager().getApplicationInfo(packageName, 128, userId);
                if (appInfo == null) {
                    Log.w(TAG, "Null application info retrieved for package " + packageName + ", userId " + userId);
                    return null;
                }
                Resources res = this.mActivity.getResources();
                UserHandle userHandle = new UserHandle(userId);
                Drawable icon = this.mPackageManager.getUserBadgedIcon(this.mPackageManager.getApplicationIcon(appInfo), userHandle);
                CharSequence appLabel = this.mPackageManager.getApplicationLabel(appInfo);
                CharSequence badgedAppLabel = this.mPackageManager.getUserBadgedLabel(appLabel, userHandle);
                if (appLabel.toString().contentEquals(badgedAppLabel)) {
                    badgedAppLabel = null;
                }
                preference = createRecentLocationEntry(icon, appLabel, highBattery, badgedAppLabel, new PackageEntryClickedListener(packageName, userHandle));
                return preference;
            } catch (RemoteException e) {
                Log.w(TAG, "Error while retrieving application info for package " + packageName + ", userId " + userId, e);
            }
        } else {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, packageName + " hadn't used location within the time interval.");
            }
            return null;
        }
    }
}
