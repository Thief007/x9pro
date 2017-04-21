package com.mediatek.systemui.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.telephony.ServiceState;
import com.mediatek.systemui.statusbar.extcb.BehaviorSet;
import com.mediatek.systemui.statusbar.extcb.DataType;
import com.mediatek.systemui.statusbar.extcb.DefaultEmptySignalClusterExt;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;
import com.mediatek.systemui.statusbar.extcb.NetworkType;
import com.mediatek.systemui.statusbar.extcb.SvLteController;

public class DefaultStatusBarPlugin extends ContextWrapper implements IStatusBarPlugin {
    public DefaultStatusBarPlugin(Context context) {
        super(context);
    }

    public void customizeSignalStrengthIcon(int level, boolean roaming, IconIdWrapper icon) {
    }

    public void customizeSignalStrengthNullIcon(int slotId, IconIdWrapper icon) {
    }

    public void customizeSignalStrengthOfflineIcon(int slotId, IconIdWrapper icon) {
    }

    public void customizeSignalIndicatorIcon(int slotId, IconIdWrapper icon) {
    }

    public void customizeDataTypeIcon(IconIdWrapper icon, boolean roaming, DataType dataType) {
    }

    public void customizeDataNetworkTypeIcon(IconIdWrapper icon, boolean roaming, NetworkType networkType) {
    }

    public void customizeDataNetworkTypeIcon(IconIdWrapper icon, boolean roaming, NetworkType networkType, SvLteController svLteController) {
    }

    public void customizeDataActivityIcon(IconIdWrapper icon, int dataActivity) {
    }

    public BehaviorSet customizeBehaviorSet() {
        return BehaviorSet.DEFAULT_BS;
    }

    public boolean customizeHspaDistinguishable(boolean distinguishable) {
        return distinguishable;
    }

    public boolean customizeHasNoSims(boolean orgHasNoSims) {
        return orgHasNoSims;
    }

    public void customizeHDVoiceIcon(IconIdWrapper icon) {
    }

    public ISignalClusterExt customizeSignalCluster() {
        return new DefaultEmptySignalClusterExt();
    }

    public void customizeVoLTEIconId(IconIdWrapper icon) {
    }

    public boolean updateSignalStrengthWifiOnlyMode(ServiceState serviceState, boolean connected) {
        return connected;
    }

    public int customizeMobileState(ServiceState serviceState, int state) {
        return state;
    }
}
