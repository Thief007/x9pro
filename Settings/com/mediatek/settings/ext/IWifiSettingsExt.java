package com.mediatek.settings.ext;

import android.content.ContentResolver;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiConfiguration;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.view.ContextMenu;

public interface IWifiSettingsExt {
    void addCategories(PreferenceScreen preferenceScreen);

    void addPreference(PreferenceScreen preferenceScreen, Preference preference, boolean z);

    void disconnect(WifiConfiguration wifiConfiguration);

    void emptyCategory(PreferenceScreen preferenceScreen);

    void emptyScreen(PreferenceScreen preferenceScreen);

    void recordPriority(int i);

    void refreshCategory(PreferenceScreen preferenceScreen);

    void registerPriorityObserver(ContentResolver contentResolver);

    void setLastConnectedConfig(WifiConfiguration wifiConfiguration);

    void setNewPriority(WifiConfiguration wifiConfiguration);

    void unregisterPriorityObserver(ContentResolver contentResolver);

    void updateContextMenu(ContextMenu contextMenu, int i, DetailedState detailedState);

    void updatePriority();

    void updatePriorityAfterSubmit(WifiConfiguration wifiConfiguration);
}
