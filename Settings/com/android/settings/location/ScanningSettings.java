package com.android.settings.location;

import android.content.ContentResolver;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings.Global;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class ScanningSettings extends SettingsPreferenceFragment {
    protected int getMetricsCategory() {
        return 131;
    }

    public void onResume() {
        super.onResume();
        createPreferenceHierarchy();
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.location_scanning);
        root = getPreferenceScreen();
        initPreferences();
        return root;
    }

    private void initPreferences() {
        boolean z;
        boolean z2 = true;
        SwitchPreference wifiScanAlwaysAvailable = (SwitchPreference) findPreference("wifi_always_scanning");
        if (Global.getInt(getContentResolver(), "wifi_scan_always_enabled", 0) == 1) {
            z = true;
        } else {
            z = false;
        }
        wifiScanAlwaysAvailable.setChecked(z);
        SwitchPreference bleScanAlwaysAvailable = (SwitchPreference) findPreference("bluetooth_always_scanning");
        if (Global.getInt(getContentResolver(), "ble_scan_always_enabled", 0) != 1) {
            z2 = false;
        }
        bleScanAlwaysAvailable.setChecked(z2);
    }

    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        int i = 0;
        String key = preference.getKey();
        ContentResolver contentResolver;
        String str;
        if ("wifi_always_scanning".equals(key)) {
            contentResolver = getContentResolver();
            str = "wifi_scan_always_enabled";
            if (((SwitchPreference) preference).isChecked()) {
                i = 1;
            }
            Global.putInt(contentResolver, str, i);
        } else if (!"bluetooth_always_scanning".equals(key)) {
            return super.onPreferenceTreeClick(screen, preference);
        } else {
            contentResolver = getContentResolver();
            str = "ble_scan_always_enabled";
            if (((SwitchPreference) preference).isChecked()) {
                i = 1;
            }
            Global.putInt(contentResolver, str, i);
        }
        return true;
    }
}
