package com.android.settings.applications;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.PermissionsSummaryHelper.PermissionsResultCallback;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.Callbacks;
import com.android.settingslib.applications.ApplicationsState.Session;
import com.mediatek.common.mom.MobileManagerUtils;
import com.mediatek.settings.UtilsExt;
import java.util.ArrayList;

public class AdvancedAppSettings extends SettingsPreferenceFragment implements Callbacks {
    private Preference mAppDomainURLsPreference;
    private Preference mAppPermsPreference;
    private Preference mHighPowerPreference;
    private final PermissionsResultCallback mPermissionCallback = new C02361();
    private BroadcastReceiver mPermissionReceiver;
    private Session mSession;
    private Preference mSystemAlertWindowPreference;
    private Preference mWriteSettingsPreference;

    class C02361 implements PermissionsResultCallback {
        C02361() {
        }

        public void onPermissionSummaryResult(int[] counts, CharSequence[] groupLabels) {
            if (AdvancedAppSettings.this.getActivity() != null) {
                AdvancedAppSettings.this.mPermissionReceiver = null;
                if (counts != null) {
                    AdvancedAppSettings.this.mAppPermsPreference.setSummary(AdvancedAppSettings.this.getContext().getString(R.string.app_permissions_summary, new Object[]{Integer.valueOf(counts[0]), Integer.valueOf(counts[1])}));
                } else {
                    AdvancedAppSettings.this.mAppPermsPreference.setSummary(null);
                }
            }
        }
    }

    private class CountAppsWithOverlayPermission extends AsyncTask<AppStateOverlayBridge, Void, Integer> {
        int numOfPackagesRequestedPermission;
        final /* synthetic */ AdvancedAppSettings this$0;

        protected Integer doInBackground(AppStateOverlayBridge... params) {
            AppStateOverlayBridge overlayBridge = params[0];
            this.numOfPackagesRequestedPermission = overlayBridge.getNumberOfPackagesWithPermission();
            return Integer.valueOf(overlayBridge.getNumberOfPackagesCanDrawOverlay());
        }

        protected void onPostExecute(Integer result) {
            if (this.this$0.isAdded()) {
                this.this$0.mSystemAlertWindowPreference.setSummary(this.this$0.getContext().getString(R.string.system_alert_window_summary, new Object[]{result, Integer.valueOf(this.numOfPackagesRequestedPermission)}));
            }
        }
    }

    private class CountAppsWithWriteSettingsPermission extends AsyncTask<AppStateWriteSettingsBridge, Void, Integer> {
        int numOfPackagesRequestedPermission;
        final /* synthetic */ AdvancedAppSettings this$0;

        protected Integer doInBackground(AppStateWriteSettingsBridge... params) {
            AppStateWriteSettingsBridge writeSettingsBridge = params[0];
            this.numOfPackagesRequestedPermission = writeSettingsBridge.getNumberOfPackagesWithPermission();
            return Integer.valueOf(writeSettingsBridge.getNumberOfPackagesCanWriteSettings());
        }

        protected void onPostExecute(Integer result) {
            if (this.this$0.isAdded()) {
                this.this$0.mWriteSettingsPreference.setSummary(this.this$0.getContext().getString(R.string.write_settings_summary, new Object[]{result, Integer.valueOf(this.numOfPackagesRequestedPermission)}));
            }
        }
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.advanced_apps);
        getPreferenceScreen().findPreference("manage_perms").setIntent(new Intent("android.intent.action.MANAGE_PERMISSIONS"));
        this.mSession = ApplicationsState.getInstance(getActivity().getApplication()).newSession(this);
        this.mAppPermsPreference = findPreference("manage_perms");
        this.mAppDomainURLsPreference = findPreference("domain_urls");
        this.mHighPowerPreference = findPreference("high_power_apps");
        this.mSystemAlertWindowPreference = findPreference("system_alert_window");
        this.mWriteSettingsPreference = findPreference("write_settings_apps");
        if (!UtilsExt.isGmsBuild(getActivity()) && MobileManagerUtils.isSupported()) {
            removePreference("manage_perms");
        }
    }

    protected int getMetricsCategory() {
        return 130;
    }

    public void onRunningStateChanged(boolean running) {
    }

    public void onPackageListChanged() {
    }

    public void onRebuildComplete(ArrayList<AppEntry> arrayList) {
    }

    public void onPackageIconChanged() {
    }

    public void onPackageSizeChanged(String packageName) {
    }

    public void onAllSizesComputed() {
    }

    public void onLauncherInfoChanged() {
    }

    public void onLoadEntriesCompleted() {
    }
}
