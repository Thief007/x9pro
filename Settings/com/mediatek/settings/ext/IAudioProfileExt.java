package com.mediatek.settings.ext;

import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

public interface IAudioProfileExt {
    void addCustomizedPreference(PreferenceScreen preferenceScreen);

    boolean isOtherAudioProfileEditable();

    void onAudioProfileSettingPaused(PreferenceFragment preferenceFragment);

    void onAudioProfileSettingResumed(PreferenceFragment preferenceFragment);

    boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference);

    void setRingtonePickerParams(Intent intent);
}
