package com.mediatek.settings.fuelgauge;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings.System;
import android.util.Log;
import com.android.settings.R;
import com.mediatek.settings.FeatureOption;

public class PowerUsageExts {
    private SwitchPreference mBatteryPercentPrf;
    private SwitchPreference mBgPowerSavingPrf;
    private Context mContext;
    private PreferenceScreen mPowerUsageScreen;

    public PowerUsageExts(Context context, PreferenceScreen appListGroup) {
        this.mContext = context;
        this.mPowerUsageScreen = appListGroup;
    }

    public void initPowerUsageExtItems() {
        boolean z = true;
        if (FeatureOption.MTK_BG_POWER_SAVING_SUPPORT && FeatureOption.MTK_BG_POWER_SAVING_UI_SUPPORT) {
            boolean z2;
            this.mBgPowerSavingPrf = new SwitchPreference(this.mContext);
            this.mBgPowerSavingPrf.setKey("background_power_saving");
            this.mBgPowerSavingPrf.setTitle(R.string.bg_power_saving_title);
            this.mBgPowerSavingPrf.setOrder(-100);
            SwitchPreference switchPreference = this.mBgPowerSavingPrf;
            if (System.getInt(this.mContext.getContentResolver(), "background_power_saving_enable", 1) != 0) {
                z2 = true;
            } else {
                z2 = false;
            }
            switchPreference.setChecked(z2);
            this.mPowerUsageScreen.addPreference(this.mBgPowerSavingPrf);
        }
        this.mBatteryPercentPrf = new SwitchPreference(this.mContext);
        this.mBatteryPercentPrf.setKey("battery_percentage");
        this.mBatteryPercentPrf.setTitle(this.mContext.getString(R.string.battery_percent));
        this.mBatteryPercentPrf.setOrder(-3);
        SwitchPreference switchPreference2 = this.mBatteryPercentPrf;
        if (System.getInt(this.mContext.getContentResolver(), "status_bar_show_battery_percent", 0) == 0) {
            z = false;
        }
        switchPreference2.setChecked(z);
        this.mPowerUsageScreen.addPreference(this.mBatteryPercentPrf);
    }

    public boolean onPowerUsageExtItemsClick(PreferenceScreen preferenceScreen, Preference preference) {
        SwitchPreference pref;
        if ("background_power_saving".equals(preference.getKey())) {
            if (preference instanceof SwitchPreference) {
                pref = (SwitchPreference) preference;
                int bgState = pref.isChecked() ? 1 : 0;
                Log.d("PowerUsageSummary", "background power saving state: " + bgState);
                System.putInt(this.mContext.getContentResolver(), "background_power_saving_enable", bgState);
                if (this.mBgPowerSavingPrf != null) {
                    this.mBgPowerSavingPrf.setChecked(pref.isChecked());
                }
            }
            return true;
        } else if (!"battery_percentage".equals(preference.getKey())) {
            return false;
        } else {
            pref = (SwitchPreference) preference;
            int state = pref.isChecked() ? 1 : 0;
            System.putInt(this.mContext.getContentResolver(), "status_bar_show_battery_percent", state);
            Intent intent = new Intent("com.intent.action.BATTERY_PERCENTAGE_SWITCH");
            intent.putExtra("state", state);
            if (this.mBatteryPercentPrf != null) {
                this.mBatteryPercentPrf.setChecked(pref.isChecked());
            }
            this.mContext.sendBroadcast(intent);
            return true;
        }
    }
}
