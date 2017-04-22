package com.android.settings.wifi;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkRequest.Builder;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController.AlertParams;
import com.android.settings.R;

public final class WifiNoInternetDialog extends AlertActivity implements OnClickListener {
    private CheckBox mAlwaysAllow;
    private ConnectivityManager mCM;
    private Network mNetwork;
    private NetworkCallback mNetworkCallback;
    private String mNetworkName;

    class C06091 extends NetworkCallback {
        C06091() {
        }

        public void onLost(Network network) {
            if (WifiNoInternetDialog.this.mNetwork.equals(network)) {
                Log.d("WifiNoInternetDialog", "Network " + WifiNoInternetDialog.this.mNetwork + " disconnected");
                WifiNoInternetDialog.this.finish();
            }
        }

        public void onCapabilitiesChanged(Network network, NetworkCapabilities nc) {
            if (WifiNoInternetDialog.this.mNetwork.equals(network) && nc.hasCapability(16)) {
                Log.d("WifiNoInternetDialog", "Network " + WifiNoInternetDialog.this.mNetwork + " validated");
                WifiNoInternetDialog.this.finish();
            }
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null && intent.getAction().equals("android.net.conn.PROMPT_UNVALIDATED") && "netId".equals(intent.getScheme())) {
            try {
                this.mNetwork = new Network(Integer.parseInt(intent.getData().getSchemeSpecificPart()));
            } catch (NullPointerException e) {
                this.mNetwork = null;
            }
            if (this.mNetwork == null) {
                Log.e("WifiNoInternetDialog", "Can't determine network from '" + intent.getData() + "' , exiting");
                finish();
                return;
            }
            NetworkRequest request = new Builder().clearCapabilities().build();
            this.mNetworkCallback = new C06091();
            this.mCM = (ConnectivityManager) getSystemService("connectivity");
            this.mCM.registerNetworkCallback(request, this.mNetworkCallback);
            NetworkInfo ni = this.mCM.getNetworkInfo(this.mNetwork);
            if (ni == null || !ni.isConnectedOrConnecting()) {
                Log.d("WifiNoInternetDialog", "Network " + this.mNetwork + " is not connected: " + ni);
                finish();
                return;
            }
            this.mNetworkName = ni.getExtraInfo();
            if (this.mNetworkName != null) {
                this.mNetworkName = this.mNetworkName.replaceAll("^\"|\"$", "");
            }
            createDialog();
            return;
        }
        Log.e("WifiNoInternetDialog", "Unexpected intent " + intent + ", exiting");
        finish();
    }

    private void createDialog() {
        this.mAlert.setIcon(R.drawable.ic_settings_wireless);
        AlertParams ap = this.mAlertParams;
        ap.mTitle = this.mNetworkName;
        ap.mMessage = getString(R.string.no_internet_access_text);
        ap.mPositiveButtonText = getString(R.string.yes);
        ap.mNegativeButtonText = getString(R.string.no);
        ap.mPositiveButtonListener = this;
        ap.mNegativeButtonListener = this;
        View checkbox = LayoutInflater.from(ap.mContext).inflate(17367089, null);
        ap.mView = checkbox;
        this.mAlwaysAllow = (CheckBox) checkbox.findViewById(16909055);
        this.mAlwaysAllow.setText(getString(R.string.no_internet_access_remember));
        setupAlert();
    }

    protected void onDestroy() {
        if (this.mNetworkCallback != null) {
            this.mCM.unregisterNetworkCallback(this.mNetworkCallback);
            this.mNetworkCallback = null;
        }
        super.onDestroy();
    }

    public void onClick(DialogInterface dialog, int which) {
        boolean accept = which == -1;
        String action = accept ? "Connect" : "Ignore";
        boolean always = this.mAlwaysAllow.isChecked();
        switch (which) {
            case -2:
            case -1:
                this.mCM.setAcceptUnvalidated(this.mNetwork, accept, always);
                Log.d("WifiNoInternetDialog", action + " network=" + this.mNetwork + (always ? " and remember" : ""));
                return;
            default:
                return;
        }
    }
}
