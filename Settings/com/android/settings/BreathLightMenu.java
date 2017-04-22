package com.android.settings;

import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;

public class BreathLightMenu extends SettingsPreferenceFragment implements OnPreferenceChangeListener {
    private SwitchPreference mBreathLightBatteryLow;
    private SwitchPreference mBreathLightCharge;
    private SwitchPreference mBreathLightMissEvent;

    protected int getMetricsCategory() {
        return 81;
    }

    public void onCreate(Bundle savedInstanceState) {
        boolean z;
        boolean z2 = true;
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.breath_light_setting);
        this.mBreathLightCharge = (SwitchPreference) findPreference("charging");
        SwitchPreference switchPreference = this.mBreathLightCharge;
        if (SystemProperties.getInt("persist.sys.bl_charge", 1) == 1) {
            z = true;
        } else {
            z = false;
        }
        switchPreference.setChecked(z);
        this.mBreathLightCharge.setOnPreferenceChangeListener(this);
        this.mBreathLightBatteryLow = (SwitchPreference) findPreference("battery_low");
        switchPreference = this.mBreathLightBatteryLow;
        if (SystemProperties.getInt("persist.sys.bl_battery_low", 1) == 1) {
            z = true;
        } else {
            z = false;
        }
        switchPreference.setChecked(z);
        this.mBreathLightBatteryLow.setOnPreferenceChangeListener(this);
        this.mBreathLightMissEvent = (SwitchPreference) findPreference("missing_event");
        SwitchPreference switchPreference2 = this.mBreathLightMissEvent;
        if (SystemProperties.getInt("persist.sys.bl_miss_evevt", 1) != 1) {
            z2 = false;
        }
        switchPreference2.setChecked(z2);
        this.mBreathLightMissEvent.setOnPreferenceChangeListener(this);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == this.mBreathLightCharge) {
            SystemProperties.set("persist.sys.bl_charge", ((Boolean) objValue).booleanValue() ? "1" : "0");
        } else if (preference == this.mBreathLightBatteryLow) {
            SystemProperties.set("persist.sys.bl_battery_low", ((Boolean) objValue).booleanValue() ? "1" : "0");
        } else if (preference == this.mBreathLightMissEvent) {
            SystemProperties.set("persist.sys.bl_miss_evevt", ((Boolean) objValue).booleanValue() ? "1" : "0");
        }
        return true;
    }
}
