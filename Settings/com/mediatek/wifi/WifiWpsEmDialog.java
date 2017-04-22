package com.mediatek.wifi;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WpsCallback;
import android.net.wifi.WpsInfo;
import android.os.Bundle;
import android.provider.Settings.System;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import com.android.settings.R;

public class WifiWpsEmDialog extends AlertDialog implements OnClickListener, View.OnClickListener, OnItemSelectedListener {
    private final AccessPoint mAccessPoint;
    private Context mContext;
    private boolean mIsOnlyVisibilityWpsPin;
    EditText mKey;
    EditText mPinCode;
    private Spinner mPinSecuritySpinner;
    EditText mSsid;
    private View mView;
    private WifiManager mWifiManager;
    private WpsCallback mWpsListener;

    class C07611 extends WpsCallback {
        C07611() {
        }

        public void onStarted(String pin) {
        }

        public void onSucceeded() {
            if (!WifiWpsEmDialog.this.mIsOnlyVisibilityWpsPin && WifiWpsEmDialog.this.mPinSecuritySpinner.getSelectedItemPosition() == 0) {
                Toast.makeText(WifiWpsEmDialog.this.mContext, R.string.wifi_open_mode, 1).show();
            }
        }

        public void onFailed(int reason) {
            Log.d("@M_WifiWpsEmDialog", "onFailed, the reason is :" + reason);
            if (reason == 10) {
                Toast.makeText(WifiWpsEmDialog.this.mContext, "Invalid PIN code", 0).show();
            }
        }
    }

    public WifiWpsEmDialog(Context context, AccessPoint accessPoint, boolean isOnlyVisibilityWpsPin) {
        super(context, R.style.Theme.WifiDialog);
        this.mAccessPoint = accessPoint;
        this.mContext = context;
        this.mIsOnlyVisibilityWpsPin = isOnlyVisibilityWpsPin;
    }

    protected void onCreate(Bundle savedInstanceState) {
        this.mView = getLayoutInflater().inflate(R.layout.wifi_dialog_wps_em, null);
        setView(this.mView);
        if (this.mIsOnlyVisibilityWpsPin) {
            this.mView.findViewById(R.id.wifi_wps_pin_fields).setVisibility(8);
            this.mView.findViewById(R.id.nfc_password_token).setVisibility(8);
        }
        if (this.mAccessPoint != null) {
            setTitle(this.mAccessPoint.ssid);
        } else {
            setTitle(R.string.wifi_wps_em_reg_pin);
        }
        setButton(-1, this.mContext.getString(R.string.wifi_save), this);
        setButton(-2, this.mContext.getString(R.string.wifi_cancel), this);
        ((CheckBox) this.mView.findViewById(R.id.wifi_defalut_pins_togglebox)).setOnClickListener(this);
        this.mPinSecuritySpinner = (Spinner) this.mView.findViewById(R.id.pin_security);
        this.mSsid = (EditText) this.mView.findViewById(R.id.wifi_wps_em_ssid);
        this.mKey = (EditText) this.mView.findViewById(R.id.wifi_wps_em_key);
        this.mPinCode = (EditText) this.mView.findViewById(R.id.wifi_pin_code);
        this.mPinSecuritySpinner.setOnItemSelectedListener(this);
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        setInverseBackgroundForced(true);
        super.onCreate(savedInstanceState);
    }

    public void onClick(View view) {
        boolean z = false;
        if (view.getId() == R.id.wifi_defalut_pins_togglebox) {
            Log.d("@M_WifiWpsEmDialog", "onClick wifi_defalut_pins_togglebox clicked hide PIN items");
            if (((CheckBox) view).isChecked()) {
                ((EditText) this.mView.findViewById(R.id.wifi_wps_em_ssid)).setEnabled(false);
                ((Spinner) this.mView.findViewById(R.id.pin_security)).setEnabled(false);
                ((EditText) this.mView.findViewById(R.id.wifi_wps_em_key)).setEnabled(false);
                return;
            }
            ((EditText) this.mView.findViewById(R.id.wifi_wps_em_ssid)).setEnabled(true);
            ((Spinner) this.mView.findViewById(R.id.pin_security)).setEnabled(true);
            ((EditText) this.mView.findViewById(R.id.wifi_wps_em_key)).setEnabled(true);
        } else if (view.getId() == R.id.nfc_password_token_togglebox) {
            Log.d("@M_WifiWpsEmDialog", "onClick nfc_password_token_togglebox clicked disable PIN items");
            Log.d("@M_WifiWpsEmDialog", "nfc_password_token_togglebox is checked : " + ((CheckBox) view).isChecked());
            EditText editText = (EditText) this.mView.findViewById(R.id.wifi_pin_code);
            if (!((CheckBox) view).isChecked()) {
                z = true;
            }
            editText.setEnabled(z);
        }
    }

    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == -1) {
            Log.d("@M_WifiWpsEmDialog", "onClick, save configuration");
            this.mWpsListener = new C07611();
            WpsInfo config = new WpsInfo();
            config.setup = 2;
            config.pin = this.mPinCode.getText().toString();
            if (this.mIsOnlyVisibilityWpsPin) {
                if (this.mAccessPoint == null || this.mAccessPoint.networkId != -1) {
                    Log.d("@M_WifiWpsEmDialog", "startWpsExternalRegistrar, config = " + config.toString());
                    this.mWifiManager.startWpsExternalRegistrar(config, this.mWpsListener);
                    return;
                }
                Log.d("@M_WifiWpsEmDialog", "startWps, config = " + config.toString());
                config.BSSID = this.mAccessPoint.bssid;
                this.mWifiManager.startWps(config, this.mWpsListener);
            } else if (((CheckBox) this.mView.findViewById(R.id.nfc_password_token_togglebox)).isChecked() || !(this.mPinCode.getText() == null || "".equals(this.mPinCode.getText()))) {
                if (((CheckBox) this.mView.findViewById(R.id.nfc_password_token_togglebox)).isChecked()) {
                    System.putInt(this.mContext.getContentResolver(), "nfc_pw", 1);
                } else {
                    System.putInt(this.mContext.getContentResolver(), "nfc_pw", 0);
                }
                config.BSSID = this.mAccessPoint.bssid;
                config.key = this.mKey.getText().toString();
                if (this.mPinSecuritySpinner.getSelectedItemPosition() == 0) {
                    config.authentication = "OPEN";
                    config.encryption = "NONE";
                } else if (this.mPinSecuritySpinner.getSelectedItemPosition() == 1) {
                    config.authentication = "WPA2PSK";
                    config.encryption = "CCMP";
                }
                config.ssid = this.mSsid.getText().toString();
                Log.d("@M_WifiWpsEmDialog", "startWpsRegistrar, config = " + config.toString());
                this.mWifiManager.startWpsRegistrar(config, this.mWpsListener);
            } else {
                Toast.makeText(this.mContext, "Please enter PIN code", 0).show();
            }
        }
    }

    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.d("@M_WifiWpsEmDialog", "onItemSelected");
        if (parent != this.mPinSecuritySpinner) {
            return;
        }
        if (this.mPinSecuritySpinner.getSelectedItemPosition() == 0) {
            this.mKey.setEnabled(false);
        } else {
            this.mKey.setEnabled(true);
        }
    }

    public void onNothingSelected(AdapterView<?> adapterView) {
    }
}
