package com.mediatek.systemui.statusbar.extcb;

import android.annotation.SuppressLint;
import android.telephony.SubscriptionInfo;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.mediatek.systemui.ext.ISignalClusterExt;
import java.util.List;

public class DefaultEmptySignalClusterExt implements ISignalClusterExt {
    public void setSignalClusterInfo(ISignalClusterInfo signalClusterInfo) {
    }

    public void setNetworkControllerExt(INetworkControllerExt networkControllerExt) {
    }

    public void customizeVoLTEIcon(ImageView voLTEIcon, int resId) {
        voLTEIcon.setImageResource(resId);
    }

    public void setSubs(List<SubscriptionInfo> list, PhoneStateExt[] states) {
    }

    public void setHDVoiceIcon(ImageView hDVoiceIcon) {
    }

    public void setMobileDataIndicators(int subId, boolean mobileVisible, ViewGroup signalClusterCombo, ImageView mobileNetworkType, ViewGroup mobileGroup, ImageView mobileStrength, ImageView mobileType, int mobileStrengthIconId, int mobileDataTypeIconId, String mobileDescription, String mobileTypeDescription, boolean isMobileTypeIconWide) {
    }

    public void onAttachedToWindow(LinearLayout mobileSignalGroup, ImageView noSimsView) {
    }

    @SuppressLint({"MissingSuperCall"})
    public void onDetachedFromWindow() {
    }

    public void onRtlPropertiesChanged(int layoutDirection) {
    }

    public void apply() {
    }
}
