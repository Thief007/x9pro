package com.android.settings.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings.Secure;
import android.util.Log;
import com.android.settings.SettingsPreferenceFragment;

public abstract class LocationSettingsBase extends SettingsPreferenceFragment {
    private boolean mActive = false;
    private int mCurrentMode;
    private BroadcastReceiver mReceiver;

    class C04191 extends BroadcastReceiver {
        C04191() {
        }

        public void onReceive(Context context, Intent intent) {
            if (Log.isLoggable("LocationSettingsBase", 3)) {
                Log.d("LocationSettingsBase", "Received location mode change intent: " + intent);
            }
            LocationSettingsBase.this.refreshLocationMode();
        }
    }

    public abstract void onModeChanged(int i, boolean z);

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mReceiver = new C04191();
    }

    public void onResume() {
        super.onResume();
        this.mActive = true;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.location.MODE_CHANGED");
        getActivity().registerReceiver(this.mReceiver, filter);
    }

    public void onPause() {
        try {
            getActivity().unregisterReceiver(this.mReceiver);
        } catch (RuntimeException e) {
        }
        super.onPause();
        this.mActive = false;
    }

    private boolean isRestricted() {
        return ((UserManager) getActivity().getSystemService("user")).hasUserRestriction("no_share_location");
    }

    public void setLocationMode(int mode) {
        if (isRestricted()) {
            if (Log.isLoggable("LocationSettingsBase", 4)) {
                Log.i("LocationSettingsBase", "Restricted user, not setting location mode");
            }
            mode = Secure.getInt(getContentResolver(), "location_mode", 0);
            if (this.mActive) {
                onModeChanged(mode, true);
            }
            return;
        }
        Intent intent = new Intent("com.android.settings.location.MODE_CHANGING");
        intent.putExtra("CURRENT_MODE", this.mCurrentMode);
        intent.putExtra("NEW_MODE", mode);
        getActivity().sendBroadcast(intent, "android.permission.WRITE_SECURE_SETTINGS");
        Secure.putInt(getContentResolver(), "location_mode", mode);
        refreshLocationMode();
    }

    public void refreshLocationMode() {
        if (this.mActive) {
            int mode = Secure.getInt(getContentResolver(), "location_mode", 0);
            this.mCurrentMode = mode;
            if (Log.isLoggable("LocationSettingsBase", 4)) {
                Log.i("LocationSettingsBase", "Location mode has been changed");
            }
            onModeChanged(mode, isRestricted());
        }
    }
}
