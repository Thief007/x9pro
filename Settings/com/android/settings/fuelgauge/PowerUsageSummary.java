package com.android.settings.fuelgauge;

import android.app.Activity;
import android.os.BatteryStats;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatterySipper.DrainType;
import com.android.internal.os.PowerProfile;
import com.android.settings.R;
import com.android.settings.Settings.HighPowerApplicationsActivity;
import com.android.settings.SettingsActivity;
import com.android.settings.applications.ManageApplications;
import com.mediatek.settings.fuelgauge.PowerUsageExts;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PowerUsageSummary extends PowerUsageBase {
    private PreferenceGroup mAppListGroup;
    Handler mHandler = new C04021();
    private BatteryHistoryPreference mHistPref;
    PowerUsageExts mPowerUsageExts;
    private int mStatsType = 0;

    class C04021 extends Handler {
        C04021() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    BatteryEntry entry = msg.obj;
                    PowerGaugePreference pgp = (PowerGaugePreference) PowerUsageSummary.this.findPreference(Integer.toString(entry.sipper.uidObj.getUid()));
                    if (pgp != null) {
                        pgp.setIcon(PowerUsageSummary.this.mUm.getBadgedIconForUser(entry.getIcon(), new UserHandle(UserHandle.getUserId(entry.sipper.getUid()))));
                        pgp.setTitle(entry.name);
                        break;
                    }
                    break;
                case 2:
                    Activity activity = PowerUsageSummary.this.getActivity();
                    if (activity != null) {
                        activity.reportFullyDrawn();
                        break;
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    }

    static class C04032 implements Comparator<BatterySipper> {
        C04032() {
        }

        public int compare(BatterySipper a, BatterySipper b) {
            return Double.compare(b.totalPowerMah, a.totalPowerMah);
        }
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.power_usage_summary);
        this.mHistPref = (BatteryHistoryPreference) findPreference("battery_history");
        this.mAppListGroup = (PreferenceGroup) findPreference("app_list");
        this.mPowerUsageExts = new PowerUsageExts(getActivity(), getPreferenceScreen());
        this.mPowerUsageExts.initPowerUsageExtItems();
    }

    protected int getMetricsCategory() {
        return 54;
    }

    public void onResume() {
        super.onResume();
        refreshStats();
    }

    public void onPause() {
        BatteryEntry.stopRequestQueue();
        this.mHandler.removeMessages(1);
        super.onPause();
    }

    public void onDestroy() {
        super.onDestroy();
        if (getActivity().isChangingConfigurations()) {
            BatteryEntry.clearUidCache();
        }
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference instanceof PowerGaugePreference) {
            PowerUsageDetail.startBatteryDetailPage((SettingsActivity) getActivity(), this.mStatsHelper, this.mStatsType, ((PowerGaugePreference) preference).getInfo(), true);
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        } else if (this.mPowerUsageExts.onPowerUsageExtItemsClick(preferenceScreen, preference)) {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        } else {
            return false;
        }
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, 3, 0, R.string.battery_saver).setShowAsAction(0);
        menu.add(0, 4, 0, R.string.high_power_apps);
        super.onCreateOptionsMenu(menu, inflater);
    }

    protected int getHelpResource() {
        return R.string.help_url_battery;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        SettingsActivity sa = (SettingsActivity) getActivity();
        switch (item.getItemId()) {
            case 1:
                if (this.mStatsType == 0) {
                    this.mStatsType = 2;
                } else {
                    this.mStatsType = 0;
                }
                refreshStats();
                return true;
            case 3:
                sa.startPreferencePanel(BatterySaverSettings.class.getName(), null, R.string.battery_saver, null, null, 0);
                return true;
            case 4:
                Bundle args = new Bundle();
                args.putString("classname", HighPowerApplicationsActivity.class.getName());
                sa.startPreferencePanel(ManageApplications.class.getName(), args, R.string.high_power_apps, null, null, 0);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void addNotAvailableMessage() {
        Preference notAvailable = new Preference(getActivity());
        notAvailable.setTitle(R.string.power_usage_not_available);
        this.mAppListGroup.addPreference(notAvailable);
    }

    private static boolean isSharedGid(int uid) {
        return UserHandle.getAppIdFromSharedAppGid(uid) > 0;
    }

    private static boolean isSystemUid(int uid) {
        return uid >= 1000 && uid < 10000;
    }

    private static List<BatterySipper> getCoalescedUsageList(List<BatterySipper> sippers) {
        int i;
        SparseArray<BatterySipper> uidList = new SparseArray();
        ArrayList<BatterySipper> results = new ArrayList();
        int numSippers = sippers.size();
        for (i = 0; i < numSippers; i++) {
            BatterySipper sipper = (BatterySipper) sippers.get(i);
            if (sipper.getUid() > 0) {
                int realUid = sipper.getUid();
                if (isSharedGid(sipper.getUid())) {
                    realUid = UserHandle.getUid(0, UserHandle.getAppIdFromSharedAppGid(sipper.getUid()));
                }
                if (isSystemUid(realUid) && !"mediaserver".equals(sipper.packageWithHighestDrain)) {
                    realUid = 1000;
                }
                if (realUid != sipper.getUid()) {
                    BatterySipper newSipper = new BatterySipper(sipper.drainType, new FakeUid(realUid), 0.0d);
                    newSipper.add(sipper);
                    newSipper.packageWithHighestDrain = sipper.packageWithHighestDrain;
                    newSipper.mPackages = sipper.mPackages;
                    sipper = newSipper;
                }
                int index = uidList.indexOfKey(realUid);
                if (index < 0) {
                    uidList.put(realUid, sipper);
                } else {
                    BatterySipper existingSipper = (BatterySipper) uidList.valueAt(index);
                    existingSipper.add(sipper);
                    if (existingSipper.packageWithHighestDrain == null && sipper.packageWithHighestDrain != null) {
                        existingSipper.packageWithHighestDrain = sipper.packageWithHighestDrain;
                    }
                    int existingPackageLen = existingSipper.mPackages != null ? existingSipper.mPackages.length : 0;
                    int newPackageLen = sipper.mPackages != null ? sipper.mPackages.length : 0;
                    if (newPackageLen > 0) {
                        String[] newPackages = new String[(existingPackageLen + newPackageLen)];
                        if (existingPackageLen > 0) {
                            System.arraycopy(existingSipper.mPackages, 0, newPackages, 0, existingPackageLen);
                        }
                        System.arraycopy(sipper.mPackages, 0, newPackages, existingPackageLen, newPackageLen);
                        existingSipper.mPackages = newPackages;
                    }
                }
            } else {
                results.add(sipper);
            }
        }
        int numUidSippers = uidList.size();
        for (i = 0; i < numUidSippers; i++) {
            results.add((BatterySipper) uidList.valueAt(i));
        }
        Collections.sort(results, new C04032());
        return results;
    }

    protected void refreshStats() {
        super.refreshStats();
        updatePreference(this.mHistPref);
        this.mAppListGroup.removeAll();
        this.mAppListGroup.setOrderingAsAdded(false);
        boolean addedSome = false;
        PowerProfile powerProfile = this.mStatsHelper.getPowerProfile();
        BatteryStats stats = this.mStatsHelper.getStats();
        double averagePower = powerProfile.getAveragePower("screen.full");
        TypedValue value = new TypedValue();
        getContext().getTheme().resolveAttribute(16843817, value, true);
        int colorControl = getContext().getColor(value.resourceId);
        if (averagePower >= 10.0d) {
            int dischargeAmount;
            List<BatterySipper> usageList = getCoalescedUsageList(this.mStatsHelper.getUsageList());
            if (stats != null) {
                dischargeAmount = stats.getDischargeAmount(this.mStatsType);
            } else {
                dischargeAmount = 0;
            }
            int numSippers = usageList.size();
            for (int i = 0; i < numSippers; i++) {
                BatterySipper sipper = (BatterySipper) usageList.get(i);
                if (sipper.totalPowerMah * 3600.0d >= 5.0d) {
                    double percentOfTotal = (sipper.totalPowerMah / this.mStatsHelper.getTotalPower()) * ((double) dischargeAmount);
                    if (((int) (0.5d + percentOfTotal)) >= 1 && ((sipper.drainType != DrainType.OVERCOUNTED || (sipper.totalPowerMah >= (this.mStatsHelper.getMaxRealPower() * 2.0d) / 3.0d && percentOfTotal >= 10.0d && !"user".equals(Build.TYPE))) && (sipper.drainType != DrainType.UNACCOUNTED || (sipper.totalPowerMah >= this.mStatsHelper.getMaxRealPower() / 2.0d && percentOfTotal >= 5.0d && !"user".equals(Build.TYPE))))) {
                        UserHandle userHandle = new UserHandle(UserHandle.getUserId(sipper.getUid()));
                        BatteryEntry entry = new BatteryEntry(getActivity(), this.mHandler, this.mUm, sipper);
                        Preference powerGaugePreference = new PowerGaugePreference(getActivity(), this.mUm.getBadgedIconForUser(entry.getIcon(), userHandle), this.mUm.getBadgedLabelForUser(entry.getLabel(), userHandle), entry);
                        double percentOfMax = (sipper.totalPowerMah * 100.0d) / this.mStatsHelper.getMaxPower();
                        sipper.percent = percentOfTotal;
                        powerGaugePreference.setTitle(entry.getLabel());
                        powerGaugePreference.setOrder(i + 1);
                        powerGaugePreference.setPercent(percentOfMax, percentOfTotal);
                        if (sipper.uidObj != null) {
                            powerGaugePreference.setKey(Integer.toString(sipper.uidObj.getUid()));
                        }
                        if ((sipper.drainType != DrainType.APP || sipper.uidObj.getUid() == 0) && sipper.drainType != DrainType.USER) {
                            powerGaugePreference.setTint(colorControl);
                        }
                        addedSome = true;
                        this.mAppListGroup.addPreference(powerGaugePreference);
                        if (this.mAppListGroup.getPreferenceCount() > 11) {
                            break;
                        }
                    }
                }
            }
        }
        if (!addedSome) {
            addNotAvailableMessage();
        }
        BatteryEntry.startRequestQueue();
    }

    private static List<BatterySipper> getFakeStats() {
        ArrayList<BatterySipper> stats = new ArrayList();
        float use = 5.0f;
        for (DrainType type : DrainType.values()) {
            if (type != DrainType.APP) {
                stats.add(new BatterySipper(type, null, (double) use));
                use += 5.0f;
            }
        }
        stats.add(new BatterySipper(DrainType.APP, new FakeUid(10000), (double) use));
        stats.add(new BatterySipper(DrainType.APP, new FakeUid(0), (double) use));
        BatterySipper sipper = new BatterySipper(DrainType.APP, new FakeUid(UserHandle.getSharedAppGid(10000)), 10.0d);
        sipper.packageWithHighestDrain = "dex2oat";
        stats.add(sipper);
        sipper = new BatterySipper(DrainType.APP, new FakeUid(UserHandle.getSharedAppGid(10001)), 10.0d);
        sipper.packageWithHighestDrain = "dex2oat";
        stats.add(sipper);
        stats.add(new BatterySipper(DrainType.APP, new FakeUid(UserHandle.getSharedAppGid(1007)), 9.0d));
        return stats;
    }
}
