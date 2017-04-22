package com.mediatek.wifi.hotspot;

import android.content.Context;
import android.net.wifi.HotspotClient;
import android.preference.Preference;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.android.settings.R;

public class ButtonPreference extends Preference implements OnClickListener {
    private Button mButton;
    private OnButtonClickCallback mCallBack;
    private HotspotClient mHotspotClient;
    private String mMacAddress;
    private String mText;

    interface OnButtonClickCallback {
        void onClick(View view, HotspotClient hotspotClient);
    }

    public ButtonPreference(Context context, HotspotClient hotspotClient, OnButtonClickCallback listner) {
        super(context);
        setWidgetLayoutResource(R.layout.preference_button);
        this.mHotspotClient = hotspotClient;
        this.mCallBack = listner;
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        this.mButton = (Button) view.findViewById(R.id.preference_button);
        this.mButton.setText(this.mText);
        this.mButton.setFocusable(false);
        this.mButton.setOnClickListener(this);
    }

    public void onClick(View v) {
        if (!(v == null || this.mCallBack == null)) {
            this.mCallBack.onClick(v, this.mHotspotClient);
        }
    }

    public void setButtonText(String text) {
        this.mText = text;
        notifyChanged();
    }

    public void setMacAddress(String macAddress) {
        this.mMacAddress = macAddress;
        setTitle(this.mMacAddress);
    }

    public String getMacAddress() {
        return this.mMacAddress;
    }

    public boolean isBlocked() {
        return this.mHotspotClient.isBlocked;
    }
}
