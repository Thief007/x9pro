package com.android.settings.applications;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
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
import com.android.settings.applications.AppStateOverlayBridge.OverlayState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

public class DrawOverlayDetails extends AppInfoWithHeader implements OnPreferenceChangeListener, OnPreferenceClickListener {
    private static final int[] APP_OPS_OP_CODE = new int[]{24};
    private AppOpsManager mAppOpsManager;
    private AppStateOverlayBridge mOverlayBridge;
    private Preference mOverlayDesc;
    private Preference mOverlayPrefs;
    private OverlayState mOverlayState;
    private Intent mSettingsIntent;
    private SwitchPreference mSwitchPref;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getActivity();
        this.mOverlayBridge = new AppStateOverlayBridge(context, this.mState, null);
        this.mAppOpsManager = (AppOpsManager) context.getSystemService("appops");
        addPreferencesFromResource(R.xml.app_ops_permissions_details);
        this.mSwitchPref = (SwitchPreference) findPreference("app_ops_settings_switch");
        this.mOverlayPrefs = findPreference("app_ops_settings_preference");
        this.mOverlayDesc = findPreference("app_ops_settings_description");
        getPreferenceScreen().setTitle(R.string.draw_overlay);
        this.mSwitchPref.setTitle(R.string.permit_draw_overlay);
        this.mOverlayPrefs.setTitle(R.string.app_overlay_permission_preference);
        this.mOverlayDesc.setSummary(R.string.allow_overlay_description);
        this.mSwitchPref.setOnPreferenceChangeListener(this);
        this.mOverlayPrefs.setOnPreferenceClickListener(this);
        this.mSettingsIntent = new Intent("android.intent.action.MAIN").setAction("android.settings.action.MANAGE_OVERLAY_PERMISSION");
    }

    public boolean onPreferenceClick(Preference preference) {
        if (preference != this.mOverlayPrefs) {
            return false;
        }
        if (this.mSettingsIntent != null) {
            try {
                getActivity().startActivityAsUser(this.mSettingsIntent, new UserHandle(this.mUserId));
            } catch (ActivityNotFoundException e) {
                Log.w("DrawOverlayDetails", "Unable to launch app draw overlay settings " + this.mSettingsIntent, e);
            }
        }
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean z = false;
        if (preference != this.mSwitchPref) {
            return false;
        }
        if (!(this.mOverlayState == null || ((Boolean) newValue).booleanValue() == this.mOverlayState.isPermissible())) {
            if (!this.mOverlayState.isPermissible()) {
                z = true;
            }
            setCanDrawOverlay(z);
            refreshUi();
        }
        return true;
    }

    private void setCanDrawOverlay(boolean newState) {
        this.mAppOpsManager.setMode(24, this.mPackageInfo.applicationInfo.uid, this.mPackageName, newState ? 0 : 2);
    }

    protected boolean refreshUi() {
        this.mOverlayState = this.mOverlayBridge.getOverlayInfo(this.mPackageName, this.mPackageInfo.applicationInfo.uid);
        boolean isAllowed = this.mOverlayState.isPermissible();
        this.mSwitchPref.setChecked(isAllowed);
        this.mSwitchPref.setEnabled(this.mOverlayState.permissionDeclared);
        this.mOverlayPrefs.setEnabled(isAllowed);
        getPreferenceScreen().removePreference(this.mOverlayPrefs);
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
            return getSummary(context, new OverlayState((PermissionState) entry.extraInfo));
        }
        return getSummary(context, entry.info.packageName);
    }

    public static CharSequence getSummary(Context context, OverlayState overlayState) {
        return context.getString(overlayState.isPermissible() ? R.string.system_alert_window_on : R.string.system_alert_window_off);
    }

    public static CharSequence getSummary(Context context, String pkg) {
        int i = R.string.system_alert_window_on;
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(pkg, 0);
            int uid = appInfo.uid;
            if ((appInfo.flags & 1) != 0) {
                return context.getString(R.string.system_alert_window_on);
            }
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService("appops");
            if (uid == -1) {
                return context.getString(R.string.system_alert_window_off);
            }
            if (appOpsManager.noteOpNoThrow(24, uid, pkg) != 0) {
                i = R.string.system_alert_window_off;
            }
            return context.getString(i);
        } catch (NameNotFoundException e) {
            Log.w("DrawOverlayDetails", "Package " + pkg + " not found", e);
            return context.getString(R.string.system_alert_window_off);
        }
    }
}
