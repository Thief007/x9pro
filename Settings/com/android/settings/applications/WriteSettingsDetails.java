package com.android.settings.applications;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.AppOpsManager.PackageOps;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.SwitchPreference;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.applications.AppStateAppOpsBridge.PermissionState;
import com.android.settings.applications.AppStateWriteSettingsBridge.WriteSettingsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import java.util.List;

public class WriteSettingsDetails extends AppInfoWithHeader implements OnPreferenceChangeListener, OnPreferenceClickListener {
    private static final int[] APP_OPS_OP_CODE = new int[]{23};
    private AppStateWriteSettingsBridge mAppBridge;
    private AppOpsManager mAppOpsManager;
    private Intent mSettingsIntent;
    private SwitchPreference mSwitchPref;
    private Preference mWriteSettingsDesc;
    private Preference mWriteSettingsPrefs;
    private WriteSettingsState mWriteSettingsState;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getActivity();
        this.mAppBridge = new AppStateWriteSettingsBridge(context, this.mState, null);
        this.mAppOpsManager = (AppOpsManager) context.getSystemService("appops");
        addPreferencesFromResource(R.xml.app_ops_permissions_details);
        this.mSwitchPref = (SwitchPreference) findPreference("app_ops_settings_switch");
        this.mWriteSettingsPrefs = findPreference("app_ops_settings_preference");
        this.mWriteSettingsDesc = findPreference("app_ops_settings_description");
        getPreferenceScreen().setTitle(R.string.write_settings);
        this.mSwitchPref.setTitle(R.string.permit_write_settings);
        this.mWriteSettingsPrefs.setTitle(R.string.write_settings_preference);
        this.mWriteSettingsDesc.setSummary(R.string.write_settings_description);
        this.mSwitchPref.setOnPreferenceChangeListener(this);
        this.mWriteSettingsPrefs.setOnPreferenceClickListener(this);
        this.mSettingsIntent = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.USAGE_ACCESS_CONFIG").setPackage(this.mPackageName);
    }

    public boolean onPreferenceClick(Preference preference) {
        if (preference != this.mWriteSettingsPrefs) {
            return false;
        }
        if (this.mSettingsIntent != null) {
            try {
                getActivity().startActivityAsUser(this.mSettingsIntent, new UserHandle(this.mUserId));
            } catch (ActivityNotFoundException e) {
                Log.w("WriteSettingsDetails", "Unable to launch write system settings " + this.mSettingsIntent, e);
            }
        }
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean z = false;
        if (preference != this.mSwitchPref) {
            return false;
        }
        if (!(this.mWriteSettingsState == null || ((Boolean) newValue).booleanValue() == this.mWriteSettingsState.isPermissible())) {
            if (!this.mWriteSettingsState.isPermissible()) {
                z = true;
            }
            setCanWriteSettings(z);
            refreshUi();
        }
        return true;
    }

    private void setCanWriteSettings(boolean newState) {
        this.mAppOpsManager.setMode(23, this.mPackageInfo.applicationInfo.uid, this.mPackageName, newState ? 0 : 2);
    }

    protected boolean refreshUi() {
        this.mWriteSettingsState = this.mAppBridge.getWriteSettingsInfo(this.mPackageName, this.mPackageInfo.applicationInfo.uid);
        boolean canWrite = this.mWriteSettingsState.isPermissible();
        this.mSwitchPref.setChecked(canWrite);
        this.mSwitchPref.setEnabled(this.mWriteSettingsState.permissionDeclared);
        this.mWriteSettingsPrefs.setEnabled(canWrite);
        getPreferenceScreen().removePreference(this.mWriteSettingsPrefs);
        return true;
    }

    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    protected int getMetricsCategory() {
        return 221;
    }

    public static CharSequence getSummary(Context context, AppEntry entry) {
        if (entry.extraInfo != null) {
            return getSummary(context, new WriteSettingsState((PermissionState) entry.extraInfo));
        }
        return getSummary(context, entry.info.packageName);
    }

    public static CharSequence getSummary(Context context, WriteSettingsState writeSettingsState) {
        int i;
        if (writeSettingsState.isPermissible()) {
            i = R.string.write_settings_on;
        } else {
            i = R.string.write_settings_off;
        }
        return context.getString(i);
    }

    public static CharSequence getSummary(Context context, String pkg) {
        int i = R.string.write_settings_off;
        boolean isSystem = false;
        try {
            if ((context.getPackageManager().getApplicationInfo(pkg, 0).flags & 1) != 0) {
                isSystem = true;
            }
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService("appops");
            List<PackageOps> packageOps = appOpsManager.getPackagesForOps(APP_OPS_OP_CODE);
            if (packageOps == null) {
                return context.getString(R.string.write_settings_off);
            }
            int uid = isSystem ? 0 : -1;
            for (PackageOps packageOp : packageOps) {
                if (pkg.equals(packageOp.getPackageName())) {
                    uid = packageOp.getUid();
                    break;
                }
            }
            if (uid == -1) {
                return context.getString(R.string.write_settings_off);
            }
            if (appOpsManager.noteOpNoThrow(23, uid, pkg) == 0) {
                i = R.string.write_settings_on;
            }
            return context.getString(i);
        } catch (NameNotFoundException e) {
            Log.w("WriteSettingsDetails", "Package " + pkg + " not found", e);
            return context.getString(R.string.write_settings_off);
        }
    }
}
