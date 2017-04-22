package com.android.settings.wifi;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources.Theme;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;
import com.android.settings.ButtonBarHandler;
import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.android.setupwizardlib.view.NavigationBar;
import com.android.setupwizardlib.view.NavigationBar.NavigationBarListener;

public class WifiSetupActivity extends WifiPickerActivity implements ButtonBarHandler, NavigationBarListener {
    private boolean mAutoFinishOnConnection;
    private IntentFilter mFilter = new IntentFilter();
    private boolean mIsNetworkRequired;
    private boolean mIsWifiRequired;
    private NavigationBar mNavigationBar;
    private final BroadcastReceiver mReceiver = new C06191();
    private boolean mUserSelectedNetwork;
    private boolean mWifiConnected;

    class C06191 extends BroadcastReceiver {
        C06191() {
        }

        public void onReceive(Context context, Intent intent) {
            WifiSetupActivity.this.refreshConnectionState();
        }
    }

    public static class WifiSkipDialog extends DialogFragment {

        class C06201 implements OnClickListener {
            C06201() {
            }

            public void onClick(DialogInterface dialog, int id) {
                ((WifiSetupActivity) WifiSkipDialog.this.getActivity()).finish(1);
            }
        }

        class C06212 implements OnClickListener {
            C06212() {
            }

            public void onClick(DialogInterface dialog, int id) {
            }
        }

        public static WifiSkipDialog newInstance(int messageRes) {
            Bundle args = new Bundle();
            args.putInt("messageRes", messageRes);
            WifiSkipDialog dialog = new WifiSkipDialog();
            dialog.setArguments(args);
            return dialog;
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog dialog = new Builder(getActivity()).setMessage(getArguments().getInt("messageRes")).setCancelable(false).setPositiveButton(R.string.wifi_skip_anyway, new C06201()).setNegativeButton(R.string.wifi_dont_skip, new C06212()).create();
            SetupWizardUtils.applyImmersiveFlags(dialog);
            return dialog;
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        boolean z = false;
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        this.mFilter.addAction("android.net.wifi.STATE_CHANGE");
        this.mFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        this.mAutoFinishOnConnection = intent.getBooleanExtra("wifi_auto_finish_on_connect", false);
        this.mIsNetworkRequired = intent.getBooleanExtra("is_network_required", false);
        this.mIsWifiRequired = intent.getBooleanExtra("is_wifi_required", false);
        if (!intent.getBooleanExtra("wifi_require_user_network_selection", false)) {
            z = true;
        }
        this.mUserSelectedNetwork = z;
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("userSelectedNetwork", this.mUserSelectedNetwork);
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        this.mUserSelectedNetwork = savedInstanceState.getBoolean("userSelectedNetwork", true);
    }

    private boolean isWifiConnected() {
        boolean isConnected;
        ConnectivityManager connectivity = (ConnectivityManager) getSystemService("connectivity");
        if (connectivity != null) {
            isConnected = connectivity.getNetworkInfo(1).isConnected();
        } else {
            isConnected = false;
        }
        this.mWifiConnected = isConnected;
        return isConnected;
    }

    private void refreshConnectionState() {
        if (isWifiConnected()) {
            if (this.mAutoFinishOnConnection && this.mUserSelectedNetwork) {
                Log.d("WifiSetupActivity", "Auto-finishing with connection");
                finish(-1);
                this.mUserSelectedNetwork = false;
            }
            setNextButtonText(R.string.setup_wizard_next_button_label);
            setNextButtonEnabled(true);
        } else if (this.mIsWifiRequired || (this.mIsNetworkRequired && !isNetworkConnected())) {
            setNextButtonText(R.string.skip_label);
            setNextButtonEnabled(false);
        } else {
            setNextButtonText(R.string.skip_label);
            setNextButtonEnabled(true);
        }
    }

    private void setNextButtonEnabled(boolean enabled) {
        if (this.mNavigationBar != null) {
            this.mNavigationBar.getNextButton().setEnabled(enabled);
        }
    }

    private void setNextButtonText(int resId) {
        if (this.mNavigationBar != null) {
            this.mNavigationBar.getNextButton().setText(resId);
        }
    }

    void networkSelected() {
        Log.d("WifiSetupActivity", "Network selected by user");
        this.mUserSelectedNetwork = true;
    }

    public void onResume() {
        super.onResume();
        registerReceiver(this.mReceiver, this.mFilter);
        refreshConnectionState();
    }

    public void onPause() {
        unregisterReceiver(this.mReceiver);
        super.onPause();
    }

    protected void onApplyThemeResource(Theme theme, int resid, boolean first) {
        super.onApplyThemeResource(theme, SetupWizardUtils.getTheme(getIntent()), first);
    }

    protected boolean isValidFragment(String fragmentName) {
        return WifiSettingsForSetupWizard.class.getName().equals(fragmentName);
    }

    Class<? extends PreferenceFragment> getWifiSettingsClass() {
        return WifiSettingsForSetupWizard.class;
    }

    public void finish(int resultCode) {
        Log.d("WifiSetupActivity", "finishing, resultCode=" + resultCode);
        setResult(resultCode);
        finish();
    }

    public void onNavigationBarCreated(NavigationBar bar) {
        this.mNavigationBar = bar;
        bar.setNavigationBarListener(this);
        SetupWizardUtils.setImmersiveMode(this);
    }

    public void onNavigateBack() {
        onBackPressed();
    }

    public void onNavigateNext() {
        if (this.mWifiConnected) {
            finish(-1);
            return;
        }
        int message;
        if (isNetworkConnected()) {
            message = R.string.wifi_skipped_message;
        } else {
            message = R.string.wifi_and_mobile_skipped_message;
        }
        WifiSkipDialog.newInstance(message).show(getFragmentManager(), "dialog");
    }

    private boolean isNetworkConnected() {
        boolean z = false;
        ConnectivityManager connectivity = (ConnectivityManager) getSystemService("connectivity");
        if (connectivity == null) {
            return false;
        }
        NetworkInfo info = connectivity.getActiveNetworkInfo();
        if (info != null) {
            z = info.isConnected();
        }
        return z;
    }
}
