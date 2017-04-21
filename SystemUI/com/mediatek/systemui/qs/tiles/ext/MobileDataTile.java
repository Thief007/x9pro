package com.mediatek.systemui.qs.tiles.ext;

import android.telephony.SubscriptionManager;
import android.util.Log;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTile.Host;
import com.android.systemui.qs.QSTile.Icon;
import com.android.systemui.qs.QSTile.ResourceIcon;
import com.android.systemui.qs.QSTile.SignalState;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.NetworkController.MobileDataController;
import com.android.systemui.statusbar.policy.SignalCallbackAdapter;
import com.mediatek.systemui.statusbar.extcb.BehaviorSet;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;
import com.mediatek.systemui.statusbar.extcb.PluginFactory;
import com.mediatek.systemui.statusbar.util.SIMHelper;

public class MobileDataTile extends QSTile<SignalState> {
    private static final int AIRPLANE_DATA_CONNECT = 2;
    private static final int DATA_CONNECT = 1;
    private static final int DATA_CONNECT_DISABLE = 3;
    private static final int DATA_DISCONNECT = 0;
    private static final int DATA_RADIO_OFF = 4;
    private static final boolean DEBUG = true;
    private static final int QS_MOBILE_DISABLE = 2130837684;
    private static final int QS_MOBILE_ENABLE = 2130837685;
    private final MobileDataSignalCallback mCallback = new MobileDataSignalCallback();
    private final NetworkController mController;
    private int mDataConnectionState = 0;
    private final MobileDataController mDataController;
    private final Icon mDataStateIcon = new QsIconWrapper(this.mDataStateIconIdWrapper);
    private int mDataStateIconId = R.drawable.ic_qs_mobile_off;
    private final IconIdWrapper mDataStateIconIdWrapper = new IconIdWrapper();

    private static final class CallbackInfo {
        public boolean activityIn;
        public boolean activityOut;
        public boolean airplaneModeEnabled;
        public int dataTypeIconId;
        public boolean enabled;
        public String enabledDesc;
        public int mobileSignalIconId;
        public boolean noSim;
        public boolean wifiConnected;
        public boolean wifiEnabled;

        private CallbackInfo() {
        }

        public String toString() {
            return "CallbackInfo[" + "enabled=" + this.enabled + ",wifiEnabled=" + this.wifiEnabled + ",wifiConnected=" + this.wifiConnected + ",airplaneModeEnabled=" + this.airplaneModeEnabled + ",mobileSignalIconId=" + this.mobileSignalIconId + ",dataTypeIconId=" + this.dataTypeIconId + ",activityIn=" + this.activityIn + ",activityOut=" + this.activityOut + ",enabledDesc=" + this.enabledDesc + ",noSim=" + this.noSim + ']';
        }
    }

    private final class MobileDataSignalCallback extends SignalCallbackAdapter {
        final CallbackInfo mInfo;

        private MobileDataSignalCallback() {
            this.mInfo = new CallbackInfo();
        }

        public void setWifiIndicators(boolean enabled, IconState statusIcon, IconState qsIcon, boolean activityIn, boolean activityOut, String description) {
            this.mInfo.wifiEnabled = enabled;
            this.mInfo.wifiConnected = qsIcon.visible;
            MobileDataTile.this.refreshState(this.mInfo);
        }

        public void setMobileDataIndicators(IconState statusIcon, IconState qsIcon, int statusType, int networkIcon, int qsType, boolean activityIn, boolean activityOut, int dataActivity, int primarySimIcon, String typeContentDescription, String description, boolean isWide, int subId) {
            if (qsIcon != null) {
                this.mInfo.enabled = qsIcon.visible;
                this.mInfo.mobileSignalIconId = qsIcon.icon;
                this.mInfo.dataTypeIconId = qsType;
                this.mInfo.activityIn = activityIn;
                this.mInfo.activityOut = activityOut;
                this.mInfo.enabledDesc = description;
                Log.d(MobileDataTile.this.TAG, "setMobileDataIndicators mInfo=" + this.mInfo);
                MobileDataTile.this.refreshState(this.mInfo);
            }
        }

        public void setNoSims(boolean show) {
            this.mInfo.noSim = show;
            if (this.mInfo.noSim) {
                this.mInfo.mobileSignalIconId = 0;
                this.mInfo.dataTypeIconId = 0;
                this.mInfo.enabled = false;
                Log.d(MobileDataTile.this.TAG, "setNoSims noSim=" + show);
            }
            MobileDataTile.this.refreshState(this.mInfo);
        }

        public void setIsAirplaneMode(IconState icon) {
            this.mInfo.airplaneModeEnabled = icon.visible;
            if (this.mInfo.airplaneModeEnabled) {
                this.mInfo.mobileSignalIconId = 0;
                this.mInfo.dataTypeIconId = 0;
                this.mInfo.enabled = false;
            }
            MobileDataTile.this.refreshState(this.mInfo);
        }

        public void setMobileDataEnabled(boolean enabled) {
            MobileDataTile.this.refreshState(this.mInfo);
        }
    }

    public MobileDataTile(Host host) {
        super(host);
        this.mController = host.getNetworkController();
        this.mDataController = this.mController.getMobileDataController();
        Log.d(this.TAG, "create MobileDataTile");
    }

    public void setListening(boolean listening) {
        Log.d(this.TAG, "setListening = " + listening);
        if (listening) {
            this.mController.addSignalCallback(this.mCallback);
        } else {
            this.mController.removeSignalCallback(this.mCallback);
        }
    }

    protected SignalState newTileState() {
        return new SignalState();
    }

    public int getMetricsCategory() {
        return 111;
    }

    protected void handleClick() {
        if (this.mDataController.isMobileDataSupported() && ((SignalState) this.mState).enabled) {
            boolean z;
            if (!((SignalState) this.mState).connected) {
                int subId = SubscriptionManager.getDefaultDataSubId();
                if (subId < 0 || !SIMHelper.isRadioOn(subId)) {
                    return;
                }
            }
            MobileDataController mobileDataController = this.mDataController;
            if (((SignalState) this.mState).connected) {
                z = false;
            } else {
                z = DEBUG;
            }
            mobileDataController.setMobileDataEnabled(z);
        }
    }

    protected void handleUpdateState(SignalState state, Object arg) {
        Log.d(this.TAG, "handleUpdateState arg=" + arg);
        state.visible = this.mController.hasMobileDataFeature();
        if (state.visible) {
            boolean z;
            boolean z2;
            CallbackInfo cb = (CallbackInfo) arg;
            if (cb == null) {
                cb = this.mCallback.mInfo;
            }
            if (!this.mDataController.isMobileDataSupported() || cb.noSim || cb.airplaneModeEnabled) {
                z = false;
            } else {
                z = isDefaultDataSimRadioOn();
            }
            boolean dataConnected = (z && this.mDataController.isMobileDataEnabled()) ? cb.mobileSignalIconId > 0 ? DEBUG : false : false;
            boolean dataNotConnected = (cb.mobileSignalIconId <= 0 || cb.enabledDesc != null) ? false : DEBUG;
            state.enabled = z;
            state.connected = dataConnected;
            if (cb.enabled) {
                z2 = cb.activityIn;
            } else {
                z2 = false;
            }
            state.activityIn = z2;
            if (cb.enabled) {
                z2 = cb.activityOut;
            } else {
                z2 = false;
            }
            state.activityOut = z2;
            state.filter = DEBUG;
            if (!state.enabled) {
                this.mDataConnectionState = 3;
                this.mDataStateIconId = R.drawable.ic_qs_mobile_off;
            } else if (dataConnected) {
                this.mDataConnectionState = 1;
                this.mDataStateIconId = R.drawable.ic_qs_mobile_white;
            } else if (dataNotConnected) {
                this.mDataConnectionState = 0;
                this.mDataStateIconId = R.drawable.ic_qs_mobile_off;
            } else {
                this.mDataConnectionState = 0;
                this.mDataStateIconId = R.drawable.ic_qs_mobile_off;
            }
            if (PluginFactory.getStatusBarPlugin(this.mContext).customizeBehaviorSet() == BehaviorSet.OP09_BS) {
                state.label = PluginFactory.getQuickSettingsPlugin(this.mContext).customizeDataConnectionTile(this.mDataConnectionState, this.mDataStateIconIdWrapper, this.mContext.getString(R.string.mobile));
                state.icon = this.mDataStateIcon;
            } else {
                state.label = this.mContext.getString(R.string.mobile);
                state.icon = ResourceIcon.get(this.mDataStateIconId);
            }
            Log.d(this.TAG, "handleUpdateState state=" + state);
        }
    }

    private final boolean isDefaultDataSimRadioOn() {
        int subId = SubscriptionManager.getDefaultDataSubId();
        boolean isRadioOn = subId >= 0 ? SIMHelper.isRadioOn(subId) : false;
        Log.d(this.TAG, "isDefaultDataSimRadioOn subId=" + subId + ", isRadioOn=" + isRadioOn);
        return isRadioOn;
    }
}
