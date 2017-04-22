package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;
import android.provider.Settings.Global;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.PhoneStateIntentReceiver;
import com.android.settingslib.WirelessUtils;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.internal.telephony.ITelephonyEx.Stub;

public class AirplaneModeEnabler implements OnPreferenceChangeListener {
    private ContentObserver mAirplaneModeObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            if (AirplaneModeEnabler.this.mSwitchPref.isChecked() != WirelessUtils.isAirplaneModeOn(AirplaneModeEnabler.this.mContext)) {
                Log.d("AirplaneModeEnabler", "airplanemode changed by others, update UI...");
                AirplaneModeEnabler.this.onAirplaneModeChanged();
            }
        }
    };
    private final Context mContext;
    private Handler mHandler = new C00351();
    private PhoneStateIntentReceiver mPhoneStateReceiver;
    private BroadcastReceiver mReceiver = new C00373();
    private final SwitchPreference mSwitchPref;

    class C00351 extends Handler {
        C00351() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 3:
                    AirplaneModeEnabler.this.onAirplaneModeChanged();
                    return;
                default:
                    return;
            }
        }
    }

    class C00373 extends BroadcastReceiver {
        C00373() {
        }

        public void onReceive(Context context, Intent intent) {
            if ("com.mediatek.intent.action.AIRPLANE_CHANGE_DONE".equals(intent.getAction())) {
                Log.d("AirplaneModeEnabler", "onReceive, ACTION_AIRPLANE_CHANGE_DONE, " + intent.getBooleanExtra("airplaneMode", false));
                AirplaneModeEnabler.this.mSwitchPref.setEnabled(AirplaneModeEnabler.this.isAirplaneModeAvailable());
            }
        }
    }

    public AirplaneModeEnabler(Context context, SwitchPreference airplaneModeSwitchPreference) {
        this.mContext = context;
        this.mSwitchPref = airplaneModeSwitchPreference;
        airplaneModeSwitchPreference.setPersistent(false);
        this.mPhoneStateReceiver = new PhoneStateIntentReceiver(this.mContext, this.mHandler);
        this.mPhoneStateReceiver.notifyServiceState(3);
    }

    public void resume() {
        this.mSwitchPref.setChecked(WirelessUtils.isAirplaneModeOn(this.mContext));
        this.mPhoneStateReceiver.registerIntent();
        this.mSwitchPref.setOnPreferenceChangeListener(this);
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("airplane_mode_on"), true, this.mAirplaneModeObserver);
        this.mSwitchPref.setEnabled(isAirplaneModeAvailable());
        this.mContext.registerReceiver(this.mReceiver, new IntentFilter("com.mediatek.intent.action.AIRPLANE_CHANGE_DONE"));
    }

    public void pause() {
        this.mPhoneStateReceiver.unregisterIntent();
        this.mSwitchPref.setOnPreferenceChangeListener(null);
        this.mContext.getContentResolver().unregisterContentObserver(this.mAirplaneModeObserver);
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    private void setAirplaneModeOn(boolean enabling) {
        int i;
        ContentResolver contentResolver = this.mContext.getContentResolver();
        String str = "airplane_mode_on";
        if (enabling) {
            i = 1;
        } else {
            i = 0;
        }
        Global.putInt(contentResolver, str, i);
        this.mSwitchPref.setChecked(enabling);
        Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
        intent.putExtra("state", enabling);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        this.mSwitchPref.setEnabled(false);
    }

    private void onAirplaneModeChanged() {
        this.mSwitchPref.setChecked(WirelessUtils.isAirplaneModeOn(this.mContext));
        this.mSwitchPref.setEnabled(isAirplaneModeAvailable());
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!Boolean.parseBoolean(SystemProperties.get("ril.cdma.inecmmode"))) {
            Boolean value = (Boolean) newValue;
            MetricsLogger.action(this.mContext, 177, value.booleanValue());
            setAirplaneModeOn(value.booleanValue());
        }
        return true;
    }

    public void setAirplaneModeInECM(boolean isECMExit, boolean isAirplaneModeOn) {
        if (isECMExit) {
            setAirplaneModeOn(isAirplaneModeOn);
        } else {
            onAirplaneModeChanged();
        }
    }

    private boolean isAirplaneModeAvailable() {
        ITelephonyEx telephonyEx = Stub.asInterface(ServiceManager.getService("phoneEx"));
        boolean isAvailable = false;
        if (telephonyEx != null) {
            try {
                isAvailable = telephonyEx.isAirplanemodeAvailableNow();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        Log.d("AirplaneModeEnabler", "isAirplaneModeAvailable = " + isAvailable);
        return isAvailable;
    }
}
