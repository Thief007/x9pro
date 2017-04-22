package com.mediatek.settings.ext;

import android.preference.Preference;
import android.preference.PreferenceScreen;

public interface IDeviceInfoSettingsExt {
    void addEpushPreference(PreferenceScreen preferenceScreen);

    void updateSummary(Preference preference, String str, String str2);
}
