package com.android.settings.wifi;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings.System;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import com.android.settings.R;
import com.mediatek.custom.CustomProperties;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IWifiApDialogExt;
import com.mediatek.wifi.Utf8ByteLengthFilter;
import java.nio.charset.Charset;
import java.util.UUID;

public class WifiApDialog extends AlertDialog implements OnClickListener, TextWatcher, OnItemSelectedListener {
    private int mBandIndex = 0;
    private Context mContext;
    IWifiApDialogExt mExt;
    private final DialogInterface.OnClickListener mListener;
    private Spinner mMaxConnSpinner;
    private EditText mPassword;
    private Spinner mSecurity;
    private int mSecurityTypeIndex = 0;
    private TextView mSsid;
    private View mView;
    WifiConfiguration mWifiConfig;
    WifiManager mWifiManager;

    public WifiApDialog(Context context, DialogInterface.OnClickListener listener, WifiConfiguration wifiConfig) {
        super(context);
        this.mListener = listener;
        this.mWifiConfig = wifiConfig;
        if (wifiConfig != null) {
            this.mSecurityTypeIndex = getSecurityTypeIndex(wifiConfig);
        }
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mContext = context;
    }

    public static int getSecurityTypeIndex(WifiConfiguration wifiConfig) {
        if (wifiConfig.allowedKeyManagement.get(4)) {
            return 1;
        }
        return 0;
    }

    public WifiConfiguration getConfig() {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = this.mSsid.getText().toString();
        config.apBand = this.mBandIndex;
        switch (this.mSecurityTypeIndex) {
            case 0:
                config.allowedKeyManagement.set(0);
                return config;
            case 1:
                config.allowedKeyManagement.set(4);
                config.allowedAuthAlgorithms.set(0);
                if (this.mPassword.length() != 0) {
                    config.preSharedKey = this.mPassword.getText().toString();
                }
                return config;
            default:
                return null;
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        ArrayAdapter<CharSequence> channelAdapter;
        Context context = getContext();
        this.mView = getLayoutInflater().inflate(R.layout.wifi_ap_dialog, null);
        this.mSecurity = (Spinner) this.mView.findViewById(R.id.security);
        final Spinner mChannel = (Spinner) this.mView.findViewById(R.id.choose_channel);
        this.mExt = UtilsExt.getWifiApDialogPlugin(context);
        this.mExt.setAdapter(context, this.mSecurity, R.array.wifi_ap_security);
        this.mMaxConnSpinner = (Spinner) this.mView.findViewById(R.id.max_connection_num);
        this.mMaxConnSpinner.setOnItemSelectedListener(this);
        setView(this.mView);
        setInverseBackgroundForced(true);
        setTitle(R.string.wifi_tether_configure_ap_text);
        this.mView.findViewById(R.id.type).setVisibility(0);
        this.mSsid = (TextView) this.mView.findViewById(R.id.ssid);
        this.mPassword = (EditText) this.mView.findViewById(R.id.password);
        String countryCode = this.mWifiManager.getCountryCode();
        if (!this.mWifiManager.isDualBandSupported() || countryCode == null) {
            Log.i("WifiApDialog", (!this.mWifiManager.isDualBandSupported() ? "Device do not support 5GHz " : "") + (countryCode == null ? " NO country code" : "") + " forbid 5GHz");
            channelAdapter = ArrayAdapter.createFromResource(this.mContext, R.array.wifi_ap_band_config_2G_only, 17367048);
            this.mWifiConfig.apBand = 0;
        } else {
            channelAdapter = ArrayAdapter.createFromResource(this.mContext, R.array.wifi_ap_band_config_full, 17367048);
        }
        channelAdapter.setDropDownViewResource(17367049);
        setButton(-1, context.getString(R.string.wifi_save), this.mListener);
        setButton(-2, context.getString(R.string.wifi_cancel), this.mListener);
        if (this.mWifiConfig != null) {
            this.mSsid.setText(this.mWifiConfig.SSID);
            if (this.mWifiConfig.apBand == 0) {
                this.mBandIndex = 0;
            } else {
                this.mBandIndex = 1;
            }
            this.mSecurity.setSelection(this.mSecurityTypeIndex);
            if (this.mSecurityTypeIndex == 1) {
                this.mPassword.setText(this.mWifiConfig.preSharedKey);
            }
        }
        mChannel.setAdapter(channelAdapter);
        mChannel.setOnItemSelectedListener(new OnItemSelectedListener() {
            boolean mInit = true;

            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                if (this.mInit) {
                    this.mInit = false;
                    mChannel.setSelection(WifiApDialog.this.mBandIndex);
                    return;
                }
                WifiApDialog.this.mBandIndex = position;
                WifiApDialog.this.mWifiConfig.apBand = WifiApDialog.this.mBandIndex;
                Log.i("WifiApDialog", "config on channelIndex : " + WifiApDialog.this.mBandIndex + " Band: " + WifiApDialog.this.mWifiConfig.apBand);
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        this.mSsid.setFilters(new InputFilter[]{new Utf8ByteLengthFilter(32)});
        this.mPassword.setFilters(new InputFilter[]{new Utf8ByteLengthFilter(63)});
        ((Button) this.mView.findViewById(R.id.reset_oob)).setOnClickListener(this);
        this.mMaxConnSpinner.setSelection(System.getInt(this.mContext.getContentResolver(), "wifi_hotspot_max_client_num", 6) - 1);
        this.mSsid.addTextChangedListener(this);
        this.mPassword.addTextChangedListener(this);
        ((CheckBox) this.mView.findViewById(R.id.show_password)).setOnClickListener(this);
        this.mSecurity.setOnItemSelectedListener(this);
        super.onCreate(savedInstanceState);
        showSecurityFields();
        validate();
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        int i;
        super.onRestoreInstanceState(savedInstanceState);
        EditText editText = this.mPassword;
        if (((CheckBox) this.mView.findViewById(R.id.show_password)).isChecked()) {
            i = 144;
        } else {
            i = 128;
        }
        editText.setInputType(i | 1);
    }

    private void validate() {
        String mSsidString = this.mSsid.getText().toString();
        if ((this.mSsid == null || this.mSsid.length() != 0) && ((this.mSecurityTypeIndex != 1 || this.mPassword.length() >= 8) && (this.mSsid == null || Charset.forName("UTF-8").encode(mSsidString).limit() <= 32))) {
            getButton(-1).setEnabled(true);
        } else {
            getButton(-1).setEnabled(false);
        }
    }

    public void onClick(View view) {
        if (view.getId() == R.id.show_password) {
            int i;
            EditText editText = this.mPassword;
            if (((CheckBox) view).isChecked()) {
                i = 144;
            } else {
                i = 128;
            }
            editText.setInputType(i | 1);
        } else if (view.getId() == R.id.reset_oob) {
            this.mSsid.setText(CustomProperties.getString("wlan", "SSID", this.mContext.getString(17040282)));
            this.mSecurityTypeIndex = 1;
            this.mSecurity.setSelection(this.mSecurityTypeIndex);
            String randomUUID = UUID.randomUUID().toString();
            this.mPassword.setText(randomUUID.substring(0, 8) + randomUUID.substring(9, 13));
        }
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void afterTextChanged(Editable editable) {
        validate();
    }

    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.equals(this.mSecurity)) {
            this.mSecurityTypeIndex = position;
            Log.d("WifiApDialog", "mSecurityTypeIndex: " + this.mSecurityTypeIndex);
            showSecurityFields();
            validate();
        } else if (parent.equals(this.mMaxConnSpinner)) {
            System.putInt(this.mContext.getContentResolver(), "wifi_hotspot_max_client_num", position + 1);
        }
    }

    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    private void showSecurityFields() {
        if (this.mSecurityTypeIndex == 0) {
            this.mView.findViewById(R.id.fields).setVisibility(8);
        } else {
            this.mView.findViewById(R.id.fields).setVisibility(0);
        }
    }

    public void closeSpinnerDialog() {
        if (this.mSecurity != null && this.mSecurity.isPopupShowing()) {
            this.mSecurity.dismissPopup();
        }
    }
}
