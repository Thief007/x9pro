package com.mediatek.systemui.statusbar.extcb;

public interface ISignalClusterInfo {
    int getSecondaryTelephonyPadding();

    int getWideTypeIconStartPadding();

    boolean isAirplaneMode();

    boolean isNoSimsVisible();

    boolean isWifiIndicatorsVisible();
}
