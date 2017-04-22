package com.android.settings.applications;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.applications.ProcStatsData.MemInfo;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ProcessStatsUi extends ProcessStatsBase {
    public static final int[] BACKGROUND_AND_SYSTEM_PROC_STATES = new int[]{0, 2, 3, 4, 5, 6, 7, 8, 9};
    public static final int[] CACHED_PROC_STATES = new int[]{11, 12, 13};
    public static final int[] FOREGROUND_PROC_STATES = new int[]{1};
    static final Comparator<ProcStatsPackageEntry> sMaxPackageEntryCompare = new C02792();
    static final Comparator<ProcStatsPackageEntry> sPackageEntryCompare = new C02781();
    private PreferenceGroup mAppListGroup;
    private MenuItem mMenuAvg;
    private MenuItem mMenuMax;
    private PackageManager mPm;
    private boolean mShowMax;

    static class C02781 implements Comparator<ProcStatsPackageEntry> {
        C02781() {
        }

        public int compare(ProcStatsPackageEntry lhs, ProcStatsPackageEntry rhs) {
            double rhsWeight = Math.max(rhs.mRunWeight, rhs.mBgWeight);
            double lhsWeight = Math.max(lhs.mRunWeight, lhs.mBgWeight);
            if (lhsWeight == rhsWeight) {
                return 0;
            }
            return lhsWeight < rhsWeight ? 1 : -1;
        }
    }

    static class C02792 implements Comparator<ProcStatsPackageEntry> {
        C02792() {
        }

        public int compare(ProcStatsPackageEntry lhs, ProcStatsPackageEntry rhs) {
            double rhsMax = (double) Math.max(rhs.mMaxBgMem, rhs.mMaxRunMem);
            double lhsMax = (double) Math.max(lhs.mMaxBgMem, lhs.mMaxRunMem);
            if (lhsMax == rhsMax) {
                return 0;
            }
            return lhsMax < rhsMax ? 1 : -1;
        }
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mPm = getActivity().getPackageManager();
        addPreferencesFromResource(R.xml.process_stats_ui);
        this.mAppListGroup = (PreferenceGroup) findPreference("app_list");
        setHasOptionsMenu(true);
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        this.mMenuAvg = menu.add(0, 1, 0, R.string.sort_avg_use);
        this.mMenuMax = menu.add(0, 2, 0, R.string.sort_max_use);
        updateMenu();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
            case 2:
                boolean z;
                if (this.mShowMax) {
                    z = false;
                } else {
                    z = true;
                }
                this.mShowMax = z;
                refreshUi();
                updateMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateMenu() {
        this.mMenuMax.setVisible(!this.mShowMax);
        this.mMenuAvg.setVisible(this.mShowMax);
    }

    protected int getMetricsCategory() {
        return 23;
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (!(preference instanceof ProcessStatsPreference)) {
            return false;
        }
        ProcessStatsPreference pgp = (ProcessStatsPreference) preference;
        ProcessStatsBase.launchMemoryDetail((SettingsActivity) getActivity(), this.mStatsManager.getMemInfo(), pgp.getEntry());
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public void refreshUi() {
        int i;
        int i2;
        double maxMemory;
        this.mAppListGroup.removeAll();
        this.mAppListGroup.setOrderingAsAdded(false);
        PreferenceGroup preferenceGroup = this.mAppListGroup;
        if (this.mShowMax) {
            i = R.string.maximum_memory_use;
        } else {
            i = R.string.average_memory_use;
        }
        preferenceGroup.setTitle(i);
        Context context = getActivity();
        MemInfo memInfo = this.mStatsManager.getMemInfo();
        List<ProcStatsPackageEntry> pkgEntries = this.mStatsManager.getEntries();
        int N = pkgEntries.size();
        for (i2 = 0; i2 < N; i2++) {
            ((ProcStatsPackageEntry) pkgEntries.get(i2)).updateMetrics();
        }
        Collections.sort(pkgEntries, this.mShowMax ? sMaxPackageEntryCompare : sPackageEntryCompare);
        if (this.mShowMax) {
            maxMemory = memInfo.realTotalRam;
        } else {
            maxMemory = memInfo.usedWeight * memInfo.weightToRam;
        }
        for (i2 = 0; i2 < pkgEntries.size(); i2++) {
            ProcStatsPackageEntry pkg = (ProcStatsPackageEntry) pkgEntries.get(i2);
            ProcessStatsPreference pref = new ProcessStatsPreference(context);
            pkg.retrieveUiData(context, this.mPm);
            pref.init(pkg, this.mPm, maxMemory, memInfo.weightToRam, memInfo.totalScale, !this.mShowMax);
            pref.setOrder(i2);
            this.mAppListGroup.addPreference(pref);
        }
    }
}
