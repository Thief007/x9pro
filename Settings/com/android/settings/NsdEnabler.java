package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.nsd.NsdManager;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;

public class NsdEnabler implements OnPreferenceChangeListener {
    private final Context mContext;
    private final IntentFilter mIntentFilter;
    private NsdManager mNsdManager;
    private final BroadcastReceiver mReceiver;
    private final SwitchPreference mSwitchPreference;

    public void resume() {
        this.mContext.registerReceiver(this.mReceiver, this.mIntentFilter);
        this.mSwitchPreference.setOnPreferenceChangeListener(this);
    }

    public void pause() {
        this.mContext.unregisterReceiver(this.mReceiver);
        this.mSwitchPreference.setOnPreferenceChangeListener(null);
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        boolean desiredState = ((Boolean) value).booleanValue();
        this.mSwitchPreference.setEnabled(false);
        this.mNsdManager.setEnabled(desiredState);
        return false;
    }
}
