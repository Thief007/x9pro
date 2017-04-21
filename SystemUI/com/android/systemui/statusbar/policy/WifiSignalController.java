package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import com.android.internal.util.AsyncChannel;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.mediatek.systemui.PluginManager;
import com.mediatek.systemui.ext.IMobileIconExt;
import com.mediatek.systemui.statusbar.util.FeatureOptions;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;

public class WifiSignalController extends SignalController<WifiState, IconGroup> {
    private final boolean mHasMobileData;
    private IMobileIconExt mMobileIconExt;
    private final AsyncChannel mWifiChannel = new AsyncChannel();
    private final WifiManager mWifiManager;

    private class WifiHandler extends Handler {
        private WifiHandler() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    WifiSignalController.this.setActivity(msg.arg1);
                    return;
                case 69632:
                    if (msg.arg1 == 0) {
                        WifiSignalController.this.mWifiChannel.sendMessage(Message.obtain(this, 69633));
                        return;
                    } else {
                        Log.e(WifiSignalController.this.mTag, "Failed to connect to wifi");
                        return;
                    }
                default:
                    return;
            }
        }
    }

    static class WifiState extends State {
        String ssid;

        WifiState() {
        }

        public void copyFrom(State s) {
            super.copyFrom(s);
            this.ssid = ((WifiState) s).ssid;
        }

        protected void toString(StringBuilder builder) {
            super.toString(builder);
            builder.append(',').append("ssid=").append(this.ssid);
        }

        public boolean equals(Object o) {
            if (super.equals(o)) {
                return Objects.equals(((WifiState) o).ssid, this.ssid);
            }
            return false;
        }
    }

    public WifiSignalController(Context context, boolean hasMobileData, CallbackHandler callbackHandler, NetworkControllerImpl networkController) {
        super("WifiSignalController", context, 1, callbackHandler, networkController);
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mHasMobileData = hasMobileData;
        Handler handler = new WifiHandler();
        Messenger wifiMessenger = this.mWifiManager.getWifiServiceMessenger();
        if (wifiMessenger != null) {
            this.mWifiChannel.connect(context, handler, wifiMessenger);
        }
        WifiState wifiState = (WifiState) this.mCurrentState;
        IconGroup iconGroup = new IconGroup("Wi-Fi Icons", WifiIcons.WIFI_SIGNAL_STRENGTH, WifiIcons.QS_WIFI_SIGNAL_STRENGTH, AccessibilityContentDescriptions.WIFI_CONNECTION_STRENGTH, R.drawable.stat_sys_wifi_signal_null, R.drawable.ic_qs_wifi_no_network, R.drawable.stat_sys_wifi_signal_null, R.drawable.ic_qs_wifi_no_network, R.string.accessibility_no_wifi);
        ((WifiState) this.mLastState).iconGroup = iconGroup;
        wifiState.iconGroup = iconGroup;
        this.mMobileIconExt = PluginManager.getMobileIconExt(context);
    }

    protected WifiState cleanState() {
        return new WifiState();
    }

    public void notifyListeners() {
        boolean z;
        boolean z2 = false;
        boolean wifiVisible = ((WifiState) this.mCurrentState).enabled ? ((WifiState) this.mCurrentState).connected || !this.mHasMobileData : false;
        String str = wifiVisible ? ((WifiState) this.mCurrentState).ssid : null;
        boolean ssidPresent = wifiVisible && ((WifiState) this.mCurrentState).ssid != null;
        String contentDescription = getStringIfExists(getContentDescription());
        IconState statusIcon = new IconState(wifiVisible, getCurrentIconId(), contentDescription);
        IconState qsIcon = new IconState(((WifiState) this.mCurrentState).connected, getQsCurrentIconId(), contentDescription);
        CallbackHandler callbackHandler = this.mCallbackHandler;
        boolean z3 = ((WifiState) this.mCurrentState).enabled;
        if (ssidPresent) {
            z = ((WifiState) this.mCurrentState).activityIn;
        } else {
            z = false;
        }
        if (ssidPresent) {
            z2 = ((WifiState) this.mCurrentState).activityOut;
        }
        callbackHandler.setWifiIndicators(z3, statusIcon, qsIcon, z, z2, str);
    }

    public void handleBroadcast(Intent intent) {
        boolean z = false;
        String action = intent.getAction();
        WifiState wifiState;
        if (action.equals("android.net.wifi.WIFI_STATE_CHANGED")) {
            wifiState = (WifiState) this.mCurrentState;
            if (intent.getIntExtra("wifi_state", 4) == 3) {
                z = true;
            }
            wifiState.enabled = z;
        } else if (action.equals("android.net.wifi.STATE_CHANGE")) {
            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
            wifiState = (WifiState) this.mCurrentState;
            if (networkInfo != null) {
                z = networkInfo.isConnected();
            }
            wifiState.connected = z;
            if (((WifiState) this.mCurrentState).connected) {
                WifiInfo info;
                if (intent.getParcelableExtra("wifiInfo") != null) {
                    info = (WifiInfo) intent.getParcelableExtra("wifiInfo");
                } else {
                    info = this.mWifiManager.getConnectionInfo();
                }
                if (info != null) {
                    ((WifiState) this.mCurrentState).ssid = getSsid(info);
                } else {
                    ((WifiState) this.mCurrentState).ssid = null;
                }
            } else if (!((WifiState) this.mCurrentState).connected) {
                ((WifiState) this.mCurrentState).ssid = null;
            }
        } else if (action.equals("android.net.wifi.RSSI_CHANGED")) {
            ((WifiState) this.mCurrentState).rssi = intent.getIntExtra("newRssi", -200);
            ((WifiState) this.mCurrentState).level = WifiManager.calculateSignalLevel(((WifiState) this.mCurrentState).rssi, WifiIcons.WIFI_LEVEL_COUNT);
        }
        notifyListenersIfNecessary();
    }

    private String getSsid(WifiInfo info) {
        String ssid = info.getSSID();
        if (ssid != null) {
            return ssid;
        }
        List<WifiConfiguration> networks = this.mWifiManager.getConfiguredNetworks();
        int length = networks.size();
        for (int i = 0; i < length; i++) {
            if (((WifiConfiguration) networks.get(i)).networkId == info.getNetworkId()) {
                return ((WifiConfiguration) networks.get(i)).SSID;
            }
        }
        return null;
    }

    void setActivity(int wifiActivity) {
        boolean z;
        boolean z2 = true;
        WifiState wifiState = (WifiState) this.mCurrentState;
        if (wifiActivity == 3) {
            z = true;
        } else if (wifiActivity == 1) {
            z = true;
        } else {
            z = false;
        }
        wifiState.activityIn = z;
        wifiState = (WifiState) this.mCurrentState;
        if (!(wifiActivity == 3 || wifiActivity == 2)) {
            z2 = false;
        }
        wifiState.activityOut = z2;
        notifyListenersIfNecessary();
    }

    public void updateConnectivity(BitSet connectedTransports, BitSet validatedTransports) {
        ((WifiState) this.mCurrentState).inetCondition = validatedTransports.get(this.mTransportType) ? 1 : 0;
        Log.d("WifiSignalController", "mCurrentState.inetCondition = " + ((WifiState) this.mCurrentState).inetCondition);
        ((WifiState) this.mCurrentState).inetCondition = this.mMobileIconExt.customizeWifiNetCondition(((WifiState) this.mCurrentState).inetCondition);
        notifyListenersIfNecessary();
    }

    public int getCurrentIconId() {
        if (FeatureOptions.MTK_A1_SUPPORT) {
            return super.getCurrentIconId();
        }
        int iconId = super.getCurrentIconId();
        if (((WifiState) this.mCurrentState).activityIn || ((WifiState) this.mCurrentState).activityOut) {
            int type = getActiveType();
            if (type < WifiIcons.WIFI_SIGNAL_STRENGTH_INOUT[0].length) {
                iconId = WifiIcons.WIFI_SIGNAL_STRENGTH_INOUT[((WifiState) this.mCurrentState).level][type];
            }
        }
        return iconId;
    }

    private int getActiveType() {
        if (((WifiState) this.mCurrentState).activityIn && ((WifiState) this.mCurrentState).activityOut) {
            return 3;
        }
        if (((WifiState) this.mCurrentState).activityIn) {
            return 1;
        }
        if (((WifiState) this.mCurrentState).activityOut) {
            return 2;
        }
        return 0;
    }
}
