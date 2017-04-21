package com.mediatek.systemui.ext;

import android.annotation.SuppressLint;
import android.telephony.SubscriptionInfo;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.mediatek.systemui.statusbar.extcb.INetworkControllerExt;
import com.mediatek.systemui.statusbar.extcb.ISignalClusterInfo;
import com.mediatek.systemui.statusbar.extcb.PhoneStateExt;
import java.util.List;

public interface ISignalClusterExt {
    void apply();

    void customizeVoLTEIcon(ImageView imageView, int i);

    void onAttachedToWindow(LinearLayout linearLayout, ImageView imageView);

    @SuppressLint({"MissingSuperCall"})
    void onDetachedFromWindow();

    void onRtlPropertiesChanged(int i);

    void setHDVoiceIcon(ImageView imageView);

    void setMobileDataIndicators(int i, boolean z, ViewGroup viewGroup, ImageView imageView, ViewGroup viewGroup2, ImageView imageView2, ImageView imageView3, int i2, int i3, String str, String str2, boolean z2);

    void setNetworkControllerExt(INetworkControllerExt iNetworkControllerExt);

    void setSignalClusterInfo(ISignalClusterInfo iSignalClusterInfo);

    void setSubs(List<SubscriptionInfo> list, PhoneStateExt[] phoneStateExtArr);
}
