package com.mediatek.settings.ext;

import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.widget.ImageView;

public interface ISettingsMiscExt {
    void addCustomizedItem(Object obj, Boolean bool);

    void customizeDashboardTile(Object obj, ImageView imageView);

    String customizeSimDisplayString(String str, int i);

    void initCustomizedLocationSettings(PreferenceScreen preferenceScreen, int i);

    boolean isWifiOnlyModeSet();

    void setFactoryResetTitle(Object obj);

    void setTimeoutPrefTitle(Preference preference);

    void updateCustomizedLocationSettings();
}
