package com.android.settings.nfc;

import android.content.Context;
import com.android.settings.DropDownPreference;
import com.android.settings.DropDownPreference.Callback;
import com.android.settings.R;
import com.android.settings.nfc.PaymentBackend.PaymentAppInfo;

public class NfcForegroundPreference extends DropDownPreference implements Callback, PaymentBackend.Callback {
    private final PaymentBackend mPaymentBackend;

    public NfcForegroundPreference(Context context, PaymentBackend backend) {
        super(context);
        this.mPaymentBackend = backend;
        this.mPaymentBackend.registerCallback(this);
        setCallback(this);
        refresh();
    }

    public void onPaymentAppsChanged() {
        refresh();
    }

    void refresh() {
        PaymentAppInfo defaultApp = this.mPaymentBackend.getDefaultApp();
        boolean foregroundMode = this.mPaymentBackend.isForegroundMode();
        setPersistent(false);
        setTitle(getContext().getString(R.string.nfc_payment_use_default));
        clearItems();
        addItem(getContext().getString(R.string.nfc_payment_favor_open), (Object) Boolean.valueOf(true));
        addItem(getContext().getString(R.string.nfc_payment_favor_default), (Object) Boolean.valueOf(false));
        if (foregroundMode) {
            setSelectedValue(Boolean.valueOf(true));
        } else {
            setSelectedValue(Boolean.valueOf(false));
        }
    }

    public boolean onItemSelected(int pos, Object value) {
        this.mPaymentBackend.setForegroundMode(((Boolean) value).booleanValue());
        return true;
    }
}
