package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

public class CryptKeeperSettings extends InstrumentedFragment {
    private View mBatteryWarning;
    private View mContentView;
    private Button mInitiateButton;
    private OnClickListener mInitiateListener = new C00902();
    private IntentFilter mIntentFilter;
    private BroadcastReceiver mIntentReceiver = new C00891();
    private View mPowerWarning;

    class C00891 extends BroadcastReceiver {
        C00891() {
        }

        public void onReceive(Context context, Intent intent) {
            int i = 8;
            if (intent.getAction().equals("android.intent.action.BATTERY_CHANGED")) {
                boolean z;
                int i2;
                int level = intent.getIntExtra("level", 0);
                int plugged = intent.getIntExtra("plugged", 0);
                int invalidCharger = intent.getIntExtra("invalid_charger", 0);
                boolean levelOk = level >= 80;
                boolean pluggedOk = (plugged & 7) != 0 ? invalidCharger == 0 : false;
                Button -get1 = CryptKeeperSettings.this.mInitiateButton;
                if (levelOk) {
                    z = pluggedOk;
                } else {
                    z = false;
                }
                -get1.setEnabled(z);
                View -get2 = CryptKeeperSettings.this.mPowerWarning;
                if (pluggedOk) {
                    i2 = 8;
                } else {
                    i2 = 0;
                }
                -get2.setVisibility(i2);
                View -get0 = CryptKeeperSettings.this.mBatteryWarning;
                if (!levelOk) {
                    i = 0;
                }
                -get0.setVisibility(i);
            }
        }
    }

    class C00902 implements OnClickListener {
        C00902() {
        }

        public void onClick(View v) {
            if (!CryptKeeperSettings.this.runKeyguardConfirmation(55)) {
                new Builder(CryptKeeperSettings.this.getActivity()).setTitle(R.string.crypt_keeper_dialog_need_password_title).setMessage(R.string.crypt_keeper_dialog_need_password_message).setPositiveButton(17039370, null).create().show();
            }
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        this.mContentView = inflater.inflate(R.layout.crypt_keeper_settings, null);
        this.mIntentFilter = new IntentFilter();
        this.mIntentFilter.addAction("android.intent.action.BATTERY_CHANGED");
        this.mInitiateButton = (Button) this.mContentView.findViewById(R.id.initiate_encrypt);
        this.mInitiateButton.setOnClickListener(this.mInitiateListener);
        this.mInitiateButton.setEnabled(false);
        this.mPowerWarning = this.mContentView.findViewById(R.id.warning_unplugged);
        this.mBatteryWarning = this.mContentView.findViewById(R.id.warning_low_charge);
        return this.mContentView;
    }

    protected int getMetricsCategory() {
        return 32;
    }

    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(this.mIntentReceiver, this.mIntentFilter);
    }

    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(this.mIntentReceiver);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Activity activity = getActivity();
        if ("android.app.action.START_ENCRYPTION".equals(activity.getIntent().getAction())) {
            DevicePolicyManager dpm = (DevicePolicyManager) activity.getSystemService("device_policy");
            if (dpm != null && dpm.getStorageEncryptionStatus() != 1) {
                activity.finish();
            }
        }
    }

    private boolean runKeyguardConfirmation(int request) {
        Resources res = getActivity().getResources();
        ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(getActivity(), this);
        if (helper.utils().getKeyguardStoredPasswordQuality(UserHandle.myUserId()) != 0) {
            return helper.launchConfirmationActivity(request, res.getText(R.string.crypt_keeper_encrypt_title), true);
        }
        showFinalConfirmation(1, "");
        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 55 && resultCode == -1 && data != null) {
            int type = data.getIntExtra("type", -1);
            String password = data.getStringExtra("password");
            if (!TextUtils.isEmpty(password)) {
                showFinalConfirmation(type, password);
            }
        }
    }

    private void showFinalConfirmation(int type, String password) {
        Preference preference = new Preference(getActivity());
        preference.setFragment(CryptKeeperConfirm.class.getName());
        preference.setTitle(R.string.crypt_keeper_confirm_title);
        addEncryptionInfoToPreference(preference, type, password);
        ((SettingsActivity) getActivity()).onPreferenceStartFragment(null, preference);
    }

    private void addEncryptionInfoToPreference(Preference preference, int type, String password) {
        if (((DevicePolicyManager) getActivity().getSystemService("device_policy")).getDoNotAskCredentialsOnBoot()) {
            preference.getExtras().putInt("type", 1);
            preference.getExtras().putString("password", "");
            return;
        }
        preference.getExtras().putInt("type", type);
        preference.getExtras().putString("password", password);
    }
}
