package com.android.settings.nfc;

import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.preference.DialogPreference;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.RadioButton;
import com.android.settings.R;
import com.android.settings.nfc.PaymentBackend.Callback;
import com.android.settings.nfc.PaymentBackend.PaymentAppInfo;
import java.util.List;

public class NfcPaymentPreference extends DialogPreference implements OnClickListener, Callback, View.OnClickListener {
    private final NfcPaymentAdapter mAdapter = new NfcPaymentAdapter();
    private final Context mContext;
    private final LayoutInflater mLayoutInflater;
    private final PaymentBackend mPaymentBackend;
    private ImageView mSettingsButtonView;

    class NfcPaymentAdapter extends BaseAdapter implements OnCheckedChangeListener, View.OnClickListener {
        private PaymentAppInfo[] appInfos;

        public class ViewHolder {
            public ImageView imageView;
            public RadioButton radioButton;
        }

        public void updateApps(PaymentAppInfo[] appInfos, PaymentAppInfo currentDefault) {
            this.appInfos = appInfos;
            notifyDataSetChanged();
        }

        public int getCount() {
            return this.appInfos.length;
        }

        public PaymentAppInfo getItem(int i) {
            return this.appInfos[i];
        }

        public long getItemId(int i) {
            return (long) this.appInfos[i].componentName.hashCode();
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            PaymentAppInfo appInfo = this.appInfos[position];
            if (convertView == null) {
                convertView = NfcPaymentPreference.this.mLayoutInflater.inflate(R.layout.nfc_payment_option, parent, false);
                holder = new ViewHolder();
                holder.imageView = (ImageView) convertView.findViewById(R.id.banner);
                holder.radioButton = (RadioButton) convertView.findViewById(R.id.button);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.imageView.setImageDrawable(appInfo.banner);
            holder.imageView.setTag(appInfo);
            holder.imageView.setContentDescription(appInfo.label);
            holder.imageView.setOnClickListener(this);
            holder.radioButton.setOnCheckedChangeListener(null);
            holder.radioButton.setChecked(appInfo.isDefault);
            holder.radioButton.setContentDescription(appInfo.label);
            holder.radioButton.setOnCheckedChangeListener(this);
            holder.radioButton.setTag(appInfo);
            return convertView;
        }

        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            makeDefault((PaymentAppInfo) compoundButton.getTag());
        }

        public void onClick(View view) {
            makeDefault((PaymentAppInfo) view.getTag());
        }

        void makeDefault(PaymentAppInfo appInfo) {
            if (!appInfo.isDefault) {
                NfcPaymentPreference.this.mPaymentBackend.setDefaultPaymentApp(appInfo.componentName);
            }
            NfcPaymentPreference.this.getDialog().dismiss();
        }
    }

    public NfcPaymentPreference(Context context, PaymentBackend backend) {
        super(context, null);
        this.mPaymentBackend = backend;
        this.mContext = context;
        backend.registerCallback(this);
        setDialogTitle(context.getString(R.string.nfc_payment_pay_with));
        this.mLayoutInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        setWidgetLayoutResource(R.layout.preference_widget_settings);
        refresh();
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        this.mSettingsButtonView = (ImageView) view.findViewById(R.id.settings_button);
        this.mSettingsButtonView.setOnClickListener(this);
        updateSettingsVisibility();
    }

    public void refresh() {
        List<PaymentAppInfo> appInfos = this.mPaymentBackend.getPaymentAppInfos();
        PaymentAppInfo defaultApp = this.mPaymentBackend.getDefaultApp();
        if (appInfos != null) {
            this.mAdapter.updateApps((PaymentAppInfo[]) appInfos.toArray(new PaymentAppInfo[appInfos.size()]), defaultApp);
        }
        setTitle(R.string.nfc_payment_default);
        if (defaultApp != null) {
            setSummary(defaultApp.label);
        } else {
            setSummary(this.mContext.getString(R.string.nfc_payment_default_not_set));
        }
        updateSettingsVisibility();
    }

    protected void onPrepareDialogBuilder(Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setSingleChoiceItems(this.mAdapter, 0, this);
    }

    public void onPaymentAppsChanged() {
        refresh();
    }

    public void onClick(View view) {
        PaymentAppInfo defaultAppInfo = this.mPaymentBackend.getDefaultApp();
        if (defaultAppInfo != null && defaultAppInfo.settingsComponent != null) {
            Intent settingsIntent = new Intent("android.intent.action.MAIN");
            settingsIntent.setComponent(defaultAppInfo.settingsComponent);
            settingsIntent.addFlags(268435456);
            try {
                this.mContext.startActivity(settingsIntent);
            } catch (ActivityNotFoundException e) {
                Log.e("NfcPaymentPreference", "Settings activity not found.");
            }
        }
    }

    void updateSettingsVisibility() {
        if (this.mSettingsButtonView != null) {
            PaymentAppInfo defaultApp = this.mPaymentBackend.getDefaultApp();
            if (defaultApp == null || defaultApp.settingsComponent == null) {
                this.mSettingsButtonView.setVisibility(8);
            } else {
                this.mSettingsButtonView.setVisibility(0);
            }
        }
    }
}
