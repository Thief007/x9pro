package com.mediatek.settings.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

public class DefaultAudioProfileExt extends ContextWrapper implements IAudioProfileExt {
    public DefaultAudioProfileExt(Context context) {
        super(context);
    }

    public void setRingtonePickerParams(Intent intent) {
        intent.putExtra("android.intent.extra.ringtone.SHOW_MORE_RINGTONES", true);
    }

    public void addCustomizedPreference(PreferenceScreen preferenceScreen) {
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return false;
    }

    public void onAudioProfileSettingResumed(PreferenceFragment fragment) {
    }

    public void onAudioProfileSettingPaused(PreferenceFragment fragment) {
    }

    public boolean isOtherAudioProfileEditable() {
        return false;
    }
}
