package com.mediatek.settings.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.ImageView;

public class DefaultSettingsMiscExt extends ContextWrapper implements ISettingsMiscExt {
    static final String TAG = "DefaultSettingsMiscExt";

    public DefaultSettingsMiscExt(Context base) {
        super(base);
    }

    public String customizeSimDisplayString(String simString, int slotId) {
        return simString;
    }

    public void initCustomizedLocationSettings(PreferenceScreen root, int order) {
    }

    public void updateCustomizedLocationSettings() {
    }

    public void setFactoryResetTitle(Object obj) {
    }

    public void setTimeoutPrefTitle(Preference pref) {
    }

    public void addCustomizedItem(Object targetDashboardCategory, Boolean add) {
        Log.i(TAG, "DefaultSettingsMisc addCustomizedItem method going");
    }

    public void customizeDashboardTile(Object tile, ImageView tileIcon) {
    }

    public boolean isWifiOnlyModeSet() {
        return false;
    }
}
