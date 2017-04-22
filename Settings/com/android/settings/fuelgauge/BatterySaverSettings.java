package com.android.settings.fuelgauge;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings.Global;
import android.util.Log;
import android.widget.Switch;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.notification.SettingPref;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBar.OnSwitchChangeListener;

public class BatterySaverSettings extends SettingsPreferenceFragment implements OnSwitchChangeListener {
    private static final boolean DEBUG = Log.isLoggable("BatterySaverSettings", 3);
    private Context mContext;
    private boolean mCreated;
    private final Handler mHandler = new Handler();
    private PowerManager mPowerManager;
    private final Receiver mReceiver = new Receiver();
    private final SettingsObserver mSettingsObserver = new SettingsObserver(this.mHandler);
    private final Runnable mStartMode = new C03952();
    private Switch mSwitch;
    private SwitchBar mSwitchBar;
    private SettingPref mTriggerPref;
    private final Runnable mUpdateSwitch = new C03931();
    private boolean mValidListener;

    class C03931 implements Runnable {
        C03931() {
        }

        public void run() {
            BatterySaverSettings.this.updateSwitch();
        }
    }

    class C03952 implements Runnable {

        class C03941 implements Runnable {
            C03941() {
            }

            public void run() {
                if (BatterySaverSettings.DEBUG) {
                    Log.d("BatterySaverSettings", "Starting low power mode from settings");
                }
                BatterySaverSettings.this.trySetPowerSaveMode(true);
            }
        }

        C03952() {
        }

        public void run() {
            AsyncTask.execute(new C03941());
        }
    }

    private final class Receiver extends BroadcastReceiver {
        private boolean mRegistered;

        private Receiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (BatterySaverSettings.DEBUG) {
                Log.d("BatterySaverSettings", "Received " + intent.getAction());
            }
            BatterySaverSettings.this.mHandler.post(BatterySaverSettings.this.mUpdateSwitch);
        }

        public void setListening(boolean listening) {
            if (listening && !this.mRegistered) {
                BatterySaverSettings.this.mContext.registerReceiver(this, new IntentFilter("android.os.action.POWER_SAVE_MODE_CHANGING"));
                this.mRegistered = true;
            } else if (!listening && this.mRegistered) {
                BatterySaverSettings.this.mContext.unregisterReceiver(this);
                this.mRegistered = false;
            }
        }
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri LOW_POWER_MODE_TRIGGER_LEVEL_URI = Global.getUriFor("low_power_trigger_level");

        public SettingsObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange, Uri uri) {
            if (this.LOW_POWER_MODE_TRIGGER_LEVEL_URI.equals(uri)) {
                BatterySaverSettings.this.mTriggerPref.update(BatterySaverSettings.this.mContext);
            }
        }

        public void setListening(boolean listening) {
            ContentResolver cr = BatterySaverSettings.this.getContentResolver();
            if (listening) {
                cr.registerContentObserver(this.LOW_POWER_MODE_TRIGGER_LEVEL_URI, false, this);
            } else {
                cr.unregisterContentObserver(this);
            }
        }
    }

    protected int getMetricsCategory() {
        return 52;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (this.mCreated) {
            this.mSwitchBar.show();
            return;
        }
        this.mCreated = true;
        addPreferencesFromResource(R.xml.battery_saver_settings);
        this.mContext = getActivity();
        this.mSwitchBar = ((SettingsActivity) this.mContext).getSwitchBar();
        this.mSwitch = this.mSwitchBar.getSwitch();
        this.mSwitchBar.show();
        this.mTriggerPref = new SettingPref(1, "turn_on_automatically", "low_power_trigger_level", 0, getResources().getIntArray(R.array.battery_saver_trigger_values)) {
            protected String getCaption(Resources res, int value) {
                if (value <= 0 || value >= 100) {
                    return res.getString(R.string.battery_saver_turn_on_automatically_never);
                }
                return res.getString(R.string.battery_saver_turn_on_automatically_pct, new Object[]{Utils.formatPercentage(value)});
            }
        };
        this.mTriggerPref.init(this);
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
    }

    public void onDestroyView() {
        super.onDestroyView();
        this.mSwitchBar.hide();
    }

    public void onResume() {
        super.onResume();
        this.mSettingsObserver.setListening(true);
        this.mReceiver.setListening(true);
        if (!this.mValidListener) {
            this.mSwitchBar.addOnSwitchChangeListener(this);
            this.mValidListener = true;
        }
        updateSwitch();
    }

    public void onPause() {
        super.onPause();
        this.mSettingsObserver.setListening(false);
        this.mReceiver.setListening(false);
        if (this.mValidListener) {
            this.mSwitchBar.removeOnSwitchChangeListener(this);
            this.mValidListener = false;
        }
    }

    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        this.mHandler.removeCallbacks(this.mStartMode);
        if (isChecked) {
            this.mHandler.postDelayed(this.mStartMode, 500);
            return;
        }
        if (DEBUG) {
            Log.d("BatterySaverSettings", "Stopping low power mode from settings");
        }
        trySetPowerSaveMode(false);
    }

    private void trySetPowerSaveMode(boolean mode) {
        if (!this.mPowerManager.setPowerSaveMode(mode)) {
            if (DEBUG) {
                Log.d("BatterySaverSettings", "Setting mode failed, fallback to current value");
            }
            this.mHandler.post(this.mUpdateSwitch);
        }
    }

    private void updateSwitch() {
        boolean mode = this.mPowerManager.isPowerSaveMode();
        if (DEBUG) {
            Log.d("BatterySaverSettings", "updateSwitch: isChecked=" + this.mSwitch.isChecked() + " mode=" + mode);
        }
        if (mode != this.mSwitch.isChecked()) {
            if (this.mValidListener) {
                this.mSwitchBar.removeOnSwitchChangeListener(this);
            }
            this.mSwitch.setChecked(mode);
            if (this.mValidListener) {
                this.mSwitchBar.addOnSwitchChangeListener(this);
            }
        }
    }
}
