package com.mediatek.systemui.statusbar.extcb;

import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class PhoneStateExt {
    public boolean mIsMobileTypeIconWide = false;
    public int mMobileDataTypeIcon = 0;
    public String mMobileDescription;
    public ViewGroup mMobileGroup;
    public ImageView mMobileNetworkType;
    public ImageView mMobileStrength;
    public int mMobileStrengthIcon = 0;
    public ImageView mMobileType;
    public String mMobileTypeDescription;
    public boolean mMobileVisible = false;
    public ViewGroup mSignalClusterCombo;
    public final int mSlotId;
    public final int mSubId;

    public PhoneStateExt(int slotId, int subId) {
        this.mSlotId = slotId;
        this.mSubId = subId;
    }

    public void setViews(ViewGroup signalClusterCombo, ImageView mobileNetworkType, ViewGroup mobileGroup, ImageView mobileStrength, ImageView mobileType) {
        this.mSignalClusterCombo = signalClusterCombo;
        this.mMobileNetworkType = mobileNetworkType;
        this.mMobileGroup = mobileGroup;
        this.mMobileStrength = mobileStrength;
        this.mMobileType = mobileType;
    }

    protected LayoutParams generateLayoutParams() {
        return new FrameLayout.LayoutParams(-2, -2);
    }

    public boolean apply() {
        return true;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        toString(builder);
        return builder.toString();
    }

    protected void toString(StringBuilder builder) {
        builder.append("mSubId=").append(this.mSubId).append(',').append("mSlotId=").append(this.mSlotId);
    }
}
