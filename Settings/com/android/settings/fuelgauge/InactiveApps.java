package com.android.settings.fuelgauge;

import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class InactiveApps extends SettingsPreferenceFragment implements OnPreferenceClickListener {
    private UsageStatsManager mUsageStats;

    protected int getMetricsCategory() {
        return 238;
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mUsageStats = (UsageStatsManager) getActivity().getSystemService(UsageStatsManager.class);
        addPreferencesFromResource(R.xml.inactive_apps);
    }

    public void onResume() {
        super.onResume();
        init();
    }

    private void init() {
        PreferenceGroup screen = getPreferenceScreen();
        screen.removeAll();
        screen.setOrderingAsAdded(false);
        Context context = getActivity();
        PackageManager pm = context.getPackageManager();
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(UsageStatsManager.class);
        Intent launcherIntent = new Intent("android.intent.action.MAIN");
        launcherIntent.addCategory("android.intent.category.LAUNCHER");
        for (ResolveInfo app : pm.queryIntentActivities(launcherIntent, 0)) {
            String packageName = app.activityInfo.applicationInfo.packageName;
            Preference p = new Preference(context);
            p.setTitle(app.loadLabel(pm));
            p.setIcon(app.loadIcon(pm));
            p.setKey(packageName);
            updateSummary(p);
            p.setOnPreferenceClickListener(this);
            screen.addPreference(p);
        }
    }

    private void updateSummary(Preference p) {
        int i;
        if (this.mUsageStats.isAppInactive(p.getKey())) {
            i = R.string.inactive_app_inactive_summary;
        } else {
            i = R.string.inactive_app_active_summary;
        }
        p.setSummary(i);
    }

    public boolean onPreferenceClick(Preference preference) {
        String packageName = preference.getKey();
        this.mUsageStats.setAppInactive(packageName, !this.mUsageStats.isAppInactive(packageName));
        updateSummary(preference);
        return false;
    }
}
