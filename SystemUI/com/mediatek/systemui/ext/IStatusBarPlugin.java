package com.mediatek.systemui.ext;

import android.telephony.ServiceState;
import com.mediatek.systemui.statusbar.extcb.BehaviorSet;
import com.mediatek.systemui.statusbar.extcb.DataType;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;
import com.mediatek.systemui.statusbar.extcb.NetworkType;
import com.mediatek.systemui.statusbar.extcb.SvLteController;

public interface IStatusBarPlugin {
    BehaviorSet customizeBehaviorSet();

    void customizeDataActivityIcon(IconIdWrapper iconIdWrapper, int i);

    void customizeDataNetworkTypeIcon(IconIdWrapper iconIdWrapper, boolean z, NetworkType networkType);

    void customizeDataNetworkTypeIcon(IconIdWrapper iconIdWrapper, boolean z, NetworkType networkType, SvLteController svLteController);

    void customizeDataTypeIcon(IconIdWrapper iconIdWrapper, boolean z, DataType dataType);

    void customizeHDVoiceIcon(IconIdWrapper iconIdWrapper);

    boolean customizeHasNoSims(boolean z);

    boolean customizeHspaDistinguishable(boolean z);

    int customizeMobileState(ServiceState serviceState, int i);

    ISignalClusterExt customizeSignalCluster();

    void customizeSignalIndicatorIcon(int i, IconIdWrapper iconIdWrapper);

    void customizeSignalStrengthIcon(int i, boolean z, IconIdWrapper iconIdWrapper);

    void customizeSignalStrengthNullIcon(int i, IconIdWrapper iconIdWrapper);

    void customizeSignalStrengthOfflineIcon(int i, IconIdWrapper iconIdWrapper);

    void customizeVoLTEIconId(IconIdWrapper iconIdWrapper);

    boolean updateSignalStrengthWifiOnlyMode(ServiceState serviceState, boolean z);
}
