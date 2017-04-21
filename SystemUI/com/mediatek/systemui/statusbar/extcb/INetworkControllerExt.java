package com.mediatek.systemui.statusbar.extcb;

import android.content.res.Resources;

public interface INetworkControllerExt {
    int getDataActivity(int i);

    DataType getDataType(int i);

    void getDefaultRoamingIcon(IconIdWrapper iconIdWrapper);

    void getDefaultSignalNullIcon(IconIdWrapper iconIdWrapper);

    NetworkType getNetworkType(int i);

    Resources getResources();

    int getSignalStrengthLevel(int i);

    SvLteController getSvLteController(int i);

    boolean hasMobileDataFeature();

    boolean hasService(int i);

    boolean isDataConnected(int i);

    boolean isEmergencyOnly(int i);

    boolean isHspaDataDistinguishable();

    boolean isLteTddSingleDataMode(int i);

    boolean isOffline(int i);

    boolean isRoaming(int i);

    boolean isRoamingGGMode();

    boolean isShowAtLeast3G();
}
