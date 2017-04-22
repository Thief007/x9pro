package com.android.settings;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;
import android.util.Log;
import android.widget.Switch;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBar.OnSwitchChangeListener;

public class StatusLightMenu extends SettingsPreferenceFragment implements OnPreferenceChangeListener, OnSwitchChangeListener {
    private boolean isSwitchChecked;
    private SwitchPreference mStatusLightCharging;
    private SwitchPreference mStatusLightMissEvent;
    private SwitchPreference mStatusLightWarning;
    private SwitchBar mSwitchBar;

    protected int getMetricsCategory() {
        return 81;
    }

    public void onCreate(Bundle savedInstanceState) {
        boolean z;
        boolean z2 = true;
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_status_liaght);
        this.mStatusLightWarning = (SwitchPreference) findPreference("low_battery_warning");
        SwitchPreference switchPreference = this.mStatusLightWarning;
        if (SystemProperties.getInt("persist.sys.status_low", 1) == 1) {
            z = true;
        } else {
            z = false;
        }
        switchPreference.setChecked(z);
        this.mStatusLightWarning.setOnPreferenceChangeListener(this);
        this.mStatusLightCharging = (SwitchPreference) findPreference("battery_charging_light");
        switchPreference = this.mStatusLightCharging;
        if (SystemProperties.getInt("persist.sys.status_charging", 1) == 1) {
            z = true;
        } else {
            z = false;
        }
        switchPreference.setChecked(z);
        this.mStatusLightCharging.setOnPreferenceChangeListener(this);
        this.mStatusLightMissEvent = (SwitchPreference) findPreference("light_miss_mmsandcall");
        SwitchPreference switchPreference2 = this.mStatusLightMissEvent;
        if (SystemProperties.getInt("persist.sys.status_miss_event", 1) != 1) {
            z2 = false;
        }
        switchPreference2.setChecked(z2);
        this.mStatusLightMissEvent.setOnPreferenceChangeListener(this);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d("StatusLightMenu", "onActivityCreated() :");
        this.mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        this.mSwitchBar.show();
        this.isSwitchChecked = SystemProperties.getBoolean("persist.sys.status_light_switch", true);
        this.mSwitchBar.setChecked(this.isSwitchChecked);
        isCheckSwitch(this.isSwitchChecked);
        this.mSwitchBar.addOnSwitchChangeListener(this);
    }

    private void isCheckSwitch(boolean isCheck) {
        this.mStatusLightWarning.setEnabled(isCheck);
        this.mStatusLightCharging.setEnabled(isCheck);
        this.mStatusLightMissEvent.setEnabled(isCheck);
    }

    public void onPause() {
        super.onPause();
    }

    public void onDestroyView() {
        super.onDestroyView();
        this.mSwitchBar.hide();
    }

    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        String str;
        Intent intent = new Intent();
        intent.setAction("com.vanzo.staus.switch");
        getActivity().sendBroadcast(intent);
        isCheckSwitch(isChecked);
        String str2 = "persist.sys.status_light_switch";
        if (isChecked) {
            str = "true";
        } else {
            str = "false";
        }
        SystemProperties.set(str2, str);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        Intent intent = new Intent();
        intent.setAction("com.vanzo.staus.switch");
        getActivity().sendBroadcast(intent);
        if (preference == this.mStatusLightWarning) {
            SystemProperties.set("persist.sys.status_low", ((Boolean) objValue).booleanValue() ? "1" : "0");
        } else if (preference == this.mStatusLightCharging) {
            SystemProperties.set("persist.sys.status_charging", ((Boolean) objValue).booleanValue() ? "1" : "0");
        } else if (preference == this.mStatusLightMissEvent) {
            SystemProperties.set("persist.sys.status_miss_event", ((Boolean) objValue).booleanValue() ? "1" : "0");
        }
        return true;
    }
}
