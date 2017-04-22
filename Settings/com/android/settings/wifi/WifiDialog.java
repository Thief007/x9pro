package com.android.settings.wifi;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settingslib.wifi.AccessPoint;

class WifiDialog extends AlertDialog implements WifiConfigUiBase {
    private final AccessPoint mAccessPoint;
    private WifiConfigController mController;
    private final boolean mEdit;
    private boolean mHideForgetButton;
    private boolean mHideSubmitButton;
    private final OnClickListener mListener;
    private final boolean mModify;
    private View mView;

    public WifiDialog(Context context, OnClickListener listener, AccessPoint accessPoint, boolean edit, boolean modify, boolean hideSubmitButton, boolean hideForgetButton) {
        this(context, listener, accessPoint, edit, modify);
        this.mHideSubmitButton = hideSubmitButton;
        this.mHideForgetButton = hideForgetButton;
    }

    public WifiDialog(Context context, OnClickListener listener, AccessPoint accessPoint, boolean edit, boolean modify) {
        super(context);
        this.mEdit = edit;
        this.mModify = modify;
        this.mListener = listener;
        this.mAccessPoint = accessPoint;
        this.mHideSubmitButton = false;
        this.mHideForgetButton = false;
    }

    public WifiConfigController getController() {
        return this.mController;
    }

    protected void onCreate(Bundle savedInstanceState) {
        this.mView = getLayoutInflater().inflate(R.layout.wifi_dialog, null);
        setView(this.mView);
        modifyIpTitle(this.mView);
        setInverseBackgroundForced(true);
        this.mController = new WifiConfigController(this, this.mView, this.mAccessPoint, this.mEdit, this.mModify);
        super.onCreate(savedInstanceState);
        if (this.mHideSubmitButton) {
            this.mController.hideSubmitButton();
        } else {
            this.mController.enableSubmitIfAppropriate();
        }
        if (this.mHideForgetButton) {
            this.mController.hideForgetButton();
        }
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        this.mController.updatePassword();
    }

    public Button getSubmitButton() {
        return getButton(-1);
    }

    public Button getForgetButton() {
        return getButton(-3);
    }

    public void setSubmitButton(CharSequence text) {
        setButton(-1, text, this.mListener);
    }

    public void setForgetButton(CharSequence text) {
        setButton(-3, text, this.mListener);
    }

    public void setCancelButton(CharSequence text) {
        setButton(-2, text, this.mListener);
    }

    private void modifyIpTitle(View view) {
        TextView ipSettingsView = (TextView) this.mView.findViewById(R.id.wifi_ip_settings);
        ipSettingsView.setText(ipSettingsView.getText().toString().replace("IP", "IPv4"));
        TextView ipAddressView = (TextView) this.mView.findViewById(R.id.wifi_ip_address);
        ipAddressView.setText(ipAddressView.getText().toString().replace("IP", "IPv4"));
    }
}
