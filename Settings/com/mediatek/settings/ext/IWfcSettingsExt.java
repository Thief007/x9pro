package com.mediatek.settings.ext;

import android.content.Context;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

public interface IWfcSettingsExt {
    void addOtherCustomPreference();

    void customizedWfcPreference(Context context, PreferenceScreen preferenceScreen);

    String getWfcSummary(Context context, int i);

    void initPlugin(PreferenceFragment preferenceFragment);

    void onWfcSettingsEvent(int i);

    void onWirelessSettingsEvent(int i);

    boolean showWfcTetheringAlertDialog(Context context);

    void updateWfcModePreference(PreferenceScreen preferenceScreen, ListPreference listPreference, boolean z, int i);
}
