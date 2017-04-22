package com.android.settings.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.preference.SwitchPreference;
import android.provider.Settings.Global;
import com.android.settings.R;
import com.android.settingslib.TetherUtil;

public class WifiApEnabler {
    private final Context mContext;
    private final IntentFilter mIntentFilter;
    private final CharSequence mOriginalSummary;
    private final BroadcastReceiver mReceiver;
    private final SwitchPreference mSwitch;

    public void resume() {
        this.mContext.registerReceiver(this.mReceiver, this.mIntentFilter);
        enableWifiSwitch();
    }

    public void pause() {
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    private void enableWifiSwitch() {
        if (Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) != 0) {
            this.mSwitch.setSummary(this.mOriginalSummary);
            this.mSwitch.setEnabled(false);
            return;
        }
        this.mSwitch.setEnabled(true);
    }

    public void setSoftapEnabled(boolean enable) {
        if (TetherUtil.setWifiTethering(enable, this.mContext)) {
            this.mSwitch.setEnabled(false);
        } else {
            this.mSwitch.setSummary(R.string.wifi_error);
        }
    }
}
