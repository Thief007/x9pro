package com.android.settings;

import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBar.OnSwitchChangeListener;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IWfcSettingsExt;

public class WifiCallingSettings extends SettingsPreferenceFragment implements OnSwitchChangeListener, OnPreferenceChangeListener {
    private ListPreference mButtonWfcMode;
    private TextView mEmptyView;
    private IntentFilter mIntentFilter;
    private BroadcastReceiver mIntentReceiver = new C02061();
    private Switch mSwitch;
    private SwitchBar mSwitchBar;
    private boolean mValidListener = false;
    IWfcSettingsExt mWfcExt;

    class C02061 extends BroadcastReceiver {
        C02061() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("WifiCallingSettings", "onReceive()... " + action);
            if (action.equals("com.android.ims.REGISTRATION_ERROR")) {
                setResultCode(0);
                WifiCallingSettings.this.mSwitch.setChecked(false);
                WifiCallingSettings.this.showAlert(intent);
            } else if (action.equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                if (!ImsManager.isWfcEnabledByPlatform(context)) {
                    Log.d("WifiCallingSettings", "carrier config changed, finish WFC activity");
                    WifiCallingSettings.this.getActivity().finish();
                }
            } else if (action.equals("android.intent.action.PHONE_STATE")) {
                WifiCallingSettings.this.updateScreen();
            }
        }
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        this.mSwitch = this.mSwitchBar.getSwitch();
        this.mSwitchBar.show();
        this.mEmptyView = (TextView) getView().findViewById(16908292);
        getListView().setEmptyView(this.mEmptyView);
        this.mEmptyView.setText(R.string.wifi_calling_off_explanation);
    }

    public void onDestroyView() {
        super.onDestroyView();
        this.mSwitchBar.hide();
    }

    private void showAlert(Intent intent) {
        Context context = getActivity();
        CharSequence title = intent.getCharSequenceExtra("alertTitle");
        CharSequence message = intent.getCharSequenceExtra("alertMessage");
        Builder builder = new Builder(context);
        builder.setMessage(message).setTitle(title).setIcon(17301543).setPositiveButton(17039370, null);
        builder.create().show();
    }

    protected int getMetricsCategory() {
        return 105;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.wifi_calling_settings);
        this.mWfcExt = UtilsExt.getWfcSettingsPlugin(getActivity());
        this.mWfcExt.initPlugin(this);
        this.mButtonWfcMode = (ListPreference) findPreference("wifi_calling_mode");
        this.mButtonWfcMode.setOnPreferenceChangeListener(this);
        this.mWfcExt.addOtherCustomPreference();
        this.mIntentFilter = new IntentFilter();
        this.mIntentFilter.addAction("com.android.ims.REGISTRATION_ERROR");
        this.mIntentFilter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        this.mIntentFilter.addAction("android.intent.action.PHONE_STATE");
    }

    public void onResume() {
        boolean isNonTtyOrTtyOnVolteEnabled;
        super.onResume();
        Context context = getActivity();
        if (ImsManager.isWfcEnabledByPlatform(context)) {
            this.mSwitchBar.addOnSwitchChangeListener(this);
            this.mValidListener = true;
        }
        if (ImsManager.isWfcEnabledByUser(context)) {
            isNonTtyOrTtyOnVolteEnabled = ImsManager.isNonTtyOrTtyOnVolteEnabled(context);
        } else {
            isNonTtyOrTtyOnVolteEnabled = false;
        }
        this.mSwitch.setChecked(isNonTtyOrTtyOnVolteEnabled);
        int wfcMode = ImsManager.getWfcMode(context);
        this.mButtonWfcMode.setValue(Integer.toString(wfcMode));
        this.mWfcExt.initPlugin(this);
        this.mWfcExt.updateWfcModePreference(getPreferenceScreen(), this.mButtonWfcMode, isNonTtyOrTtyOnVolteEnabled, wfcMode);
        updateScreen();
        context.registerReceiver(this.mIntentReceiver, this.mIntentFilter);
        Intent intent = getActivity().getIntent();
        if (intent.getBooleanExtra("alertShow", false)) {
            showAlert(intent);
        }
        this.mWfcExt.onWfcSettingsEvent(0);
    }

    public void onPause() {
        super.onPause();
        Context context = getActivity();
        if (this.mValidListener) {
            this.mValidListener = false;
            this.mSwitchBar.removeOnSwitchChangeListener(this);
        }
        context.unregisterReceiver(this.mIntentReceiver);
        this.mWfcExt.onWfcSettingsEvent(1);
    }

    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        boolean z = false;
        Log.d("WifiCallingSettings", "OnSwitchChanged");
        if (isInSwitchProcess()) {
            Log.d("WifiCallingSettings", "[onClick] Switching process ongoing");
            Toast.makeText(getActivity(), R.string.Switch_not_in_use_string, 0).show();
            Switch switchR = this.mSwitch;
            if (!isChecked) {
                z = true;
            }
            switchR.setChecked(z);
            return;
        }
        Context context = getActivity();
        ImsManager.setWfcSetting(context, isChecked);
        int wfcMode = ImsManager.getWfcMode(context);
        this.mWfcExt.updateWfcModePreference(getPreferenceScreen(), this.mButtonWfcMode, isChecked, wfcMode);
        if (isChecked) {
            MetricsLogger.action(getActivity(), getMetricsCategory(), wfcMode);
        } else {
            MetricsLogger.action(getActivity(), getMetricsCategory(), -1);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Context context = getActivity();
        if (preference == this.mButtonWfcMode) {
            this.mButtonWfcMode.setValue((String) newValue);
            int buttonMode = Integer.valueOf((String) newValue).intValue();
            if (buttonMode != ImsManager.getWfcMode(context)) {
                ImsManager.setWfcMode(context, buttonMode);
                this.mButtonWfcMode.setSummary(getWfcModeSummary(context, buttonMode));
                MetricsLogger.action(getActivity(), getMetricsCategory(), buttonMode);
            }
        }
        return true;
    }

    static int getWfcModeSummary(Context context, int wfcMode) {
        if (!ImsManager.isWfcEnabledByUser(context)) {
            return 17039561;
        }
        switch (wfcMode) {
            case 0:
                return 17039564;
            case 1:
                return 17039563;
            case 2:
                return 17039562;
            default:
                Log.e("WifiCallingSettings", "Unexpected WFC mode value: " + wfcMode);
                return 17039561;
        }
    }

    private boolean isInSwitchProcess() {
        boolean z = true;
        try {
            int imsState = ImsManager.getInstance(getActivity(), SubscriptionManager.getDefaultVoiceSubId()).getImsState();
            Log.d("@M_WifiCallingSettings", "isInSwitchProcess , imsState = " + imsState);
            if (!(imsState == 3 || imsState == 2)) {
                z = false;
            }
            return z;
        } catch (ImsException e) {
            return false;
        }
    }

    private void updateScreen() {
        SettingsActivity activity = (SettingsActivity) getActivity();
        if (activity != null) {
            boolean z;
            boolean isNonTtyOrTtyOnVolteEnabled = ImsManager.isNonTtyOrTtyOnVolteEnabled(activity);
            SwitchBar switchBar = activity.getSwitchBar();
            if (switchBar.getSwitch().isChecked()) {
                z = isNonTtyOrTtyOnVolteEnabled;
            } else {
                z = false;
            }
            boolean isCallStateIdle = !TelecomManager.from(activity).isInCall();
            Log.d("WifiCallingSettings", "isWfcEnabled: " + z + ", isCallStateIdle: " + isCallStateIdle);
            if (!isCallStateIdle) {
                isNonTtyOrTtyOnVolteEnabled = false;
            }
            switchBar.setEnabled(isNonTtyOrTtyOnVolteEnabled);
            Preference pref = getPreferenceScreen().findPreference("wifi_calling_mode");
            if (pref != null) {
                if (!z) {
                    isCallStateIdle = false;
                }
                pref.setEnabled(isCallStateIdle);
            }
        }
    }
}
