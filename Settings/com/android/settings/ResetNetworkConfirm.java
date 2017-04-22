package com.android.settings;

import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkPolicyManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.UserManager;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Toast;

public class ResetNetworkConfirm extends InstrumentedFragment {
    private View mContentView;
    private OnClickListener mFinalClickListener = new C01751();
    private int mSubId = -1;

    class C01751 implements OnClickListener {
        C01751() {
        }

        public void onClick(View v) {
            if (!Utils.isMonkeyRunning()) {
                Context context = ResetNetworkConfirm.this.getActivity();
                ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
                if (connectivityManager != null) {
                    connectivityManager.factoryReset();
                }
                WifiManager wifiManager = (WifiManager) context.getSystemService("wifi");
                if (wifiManager != null) {
                    wifiManager.factoryReset();
                }
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
                if (telephonyManager != null) {
                    telephonyManager.factoryReset(ResetNetworkConfirm.this.mSubId);
                }
                NetworkPolicyManager policyManager = (NetworkPolicyManager) context.getSystemService("netpolicy");
                if (policyManager != null) {
                    policyManager.factoryReset(telephonyManager.getSubscriberId(ResetNetworkConfirm.this.mSubId));
                }
                BluetoothManager btManager = (BluetoothManager) context.getSystemService("bluetooth");
                if (btManager != null) {
                    btManager.getAdapter().factoryReset();
                }
                Toast.makeText(context, R.string.reset_network_complete_toast, 0).show();
            }
        }
    }

    private void establishFinalConfirmationState() {
        this.mContentView.findViewById(R.id.execute_reset_network).setOnClickListener(this.mFinalClickListener);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (UserManager.get(getActivity()).hasUserRestriction("no_network_reset")) {
            return inflater.inflate(R.layout.network_reset_disallowed_screen, null);
        }
        this.mContentView = inflater.inflate(R.layout.reset_network_confirm, null);
        establishFinalConfirmationState();
        return this.mContentView;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            this.mSubId = args.getInt("subscription", -1);
        }
    }

    protected int getMetricsCategory() {
        return 84;
    }
}
