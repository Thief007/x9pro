package com.mediatek.systemui.statusbar.extcb;

import android.content.res.Resources;

public class DefaultEmptyNetworkControllerExt implements INetworkControllerExt {
    public boolean isShowAtLeast3G() {
        return false;
    }

    public boolean isHspaDataDistinguishable() {
        return false;
    }

    public boolean hasMobileDataFeature() {
        return true;
    }

    public Resources getResources() {
        return null;
    }

    public void getDefaultSignalNullIcon(IconIdWrapper icon) {
    }

    public void getDefaultRoamingIcon(IconIdWrapper icon) {
    }

    public boolean hasService(int subId) {
        return false;
    }

    public boolean isDataConnected(int subId) {
        return false;
    }

    public boolean isEmergencyOnly(int subId) {
        return false;
    }

    public boolean isRoaming(int subId) {
        return false;
    }

    public int getSignalStrengthLevel(int subId) {
        return 0;
    }

    public NetworkType getNetworkType(int subId) {
        return null;
    }

    public DataType getDataType(int subId) {
        return null;
    }

    public int getDataActivity(int subId) {
        return 0;
    }

    public boolean isOffline(int subId) {
        return false;
    }

    public boolean isLteTddSingleDataMode(int subId) {
        return false;
    }

    public boolean isRoamingGGMode() {
        return false;
    }

    public SvLteController getSvLteController(int subId) {
        return null;
    }
}
