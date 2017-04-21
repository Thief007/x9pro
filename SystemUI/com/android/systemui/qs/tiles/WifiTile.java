package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.wifi.AccessPoint;
import com.android.systemui.R;
import com.android.systemui.qs.QSDetailItems;
import com.android.systemui.qs.QSDetailItems.Callback;
import com.android.systemui.qs.QSDetailItems.Item;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTile.DetailAdapter;
import com.android.systemui.qs.QSTile.Host;
import com.android.systemui.qs.QSTile.ResourceIcon;
import com.android.systemui.qs.QSTile.SignalState;
import com.android.systemui.qs.QSTileView;
import com.android.systemui.qs.SignalTileView;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.AccessPointController;
import com.android.systemui.statusbar.policy.NetworkController.AccessPointController.AccessPointCallback;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.SignalCallbackAdapter;
import java.util.List;

public class WifiTile extends QSTile<SignalState> {
    private static final Intent WIFI_SETTINGS = new Intent("android.settings.WIFI_SETTINGS");
    private final NetworkController mController;
    private final WifiDetailAdapter mDetailAdapter;
    private final WifiSignalCallback mSignalCallback = new WifiSignalCallback();
    private final SignalState mStateBeforeClick = newTileState();
    private final AccessPointController mWifiController;

    private static final class CallbackInfo {
        boolean activityIn;
        boolean activityOut;
        boolean connected;
        boolean enabled;
        String enabledDesc;
        String wifiSignalContentDescription;
        int wifiSignalIconId;

        private CallbackInfo() {
        }

        public String toString() {
            return "CallbackInfo[" + "enabled=" + this.enabled + ",connected=" + this.connected + ",wifiSignalIconId=" + this.wifiSignalIconId + ",enabledDesc=" + this.enabledDesc + ",activityIn=" + this.activityIn + ",activityOut=" + this.activityOut + ",wifiSignalContentDescription=" + this.wifiSignalContentDescription + ']';
        }
    }

    private final class WifiDetailAdapter implements DetailAdapter, AccessPointCallback, Callback {
        private AccessPoint[] mAccessPoints;
        private QSDetailItems mItems;

        private WifiDetailAdapter() {
        }

        public int getTitle() {
            return R.string.quick_settings_wifi_label;
        }

        public Intent getSettingsIntent() {
            return WifiTile.WIFI_SETTINGS;
        }

        public Boolean getToggleState() {
            return Boolean.valueOf(((SignalState) WifiTile.this.mState).enabled);
        }

        public void setToggleState(boolean state) {
            if (WifiTile.DEBUG) {
                Log.d(WifiTile.this.TAG, "setToggleState " + state);
            }
            MetricsLogger.action(WifiTile.this.mContext, 153, state);
            WifiTile.this.mController.setWifiEnabled(state);
            WifiTile.this.showDetail(false);
        }

        public int getMetricsCategory() {
            return 152;
        }

        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            if (WifiTile.DEBUG) {
                Log.d(WifiTile.this.TAG, "createDetailView convertView=" + (convertView != null));
            }
            this.mAccessPoints = null;
            WifiTile.this.mWifiController.scanForAccessPoints();
            WifiTile.this.fireScanStateChanged(true);
            this.mItems = QSDetailItems.convertOrInflate(context, convertView, parent);
            this.mItems.setTagSuffix("Wifi");
            this.mItems.setCallback(this);
            this.mItems.setEmptyState(R.drawable.ic_qs_wifi_detail_empty, R.string.quick_settings_wifi_detail_empty_text);
            updateItems();
            setItemsVisible(((SignalState) WifiTile.this.mState).enabled);
            return this.mItems;
        }

        public void onAccessPointsChanged(List<AccessPoint> accessPoints) {
            this.mAccessPoints = (AccessPoint[]) accessPoints.toArray(new AccessPoint[accessPoints.size()]);
            updateItems();
            if (accessPoints != null && accessPoints.size() > 0) {
                WifiTile.this.fireScanStateChanged(false);
            }
        }

        public void onSettingsActivityTriggered(Intent settingsIntent) {
            WifiTile.this.mHost.startActivityDismissingKeyguard(settingsIntent);
        }

        public void onDetailItemClick(Item item) {
            if (item != null && item.tag != null) {
                AccessPoint ap = item.tag;
                if (!ap.isActive() && WifiTile.this.mWifiController.connect(ap)) {
                    WifiTile.this.mHost.collapsePanels();
                }
                WifiTile.this.showDetail(false);
            }
        }

        public void onDetailItemDisconnect(Item item) {
        }

        public void setItemsVisible(boolean visible) {
            if (this.mItems != null) {
                this.mItems.setItemsVisible(visible);
            }
        }

        private void updateItems() {
            if (this.mItems != null) {
                Item[] itemArr = null;
                if (this.mAccessPoints != null) {
                    itemArr = new Item[this.mAccessPoints.length];
                    for (int i = 0; i < this.mAccessPoints.length; i++) {
                        CharSequence summary;
                        Drawable drawable;
                        AccessPoint ap = this.mAccessPoints[i];
                        Item item = new Item();
                        item.tag = ap;
                        item.icon = WifiTile.this.mWifiController.getIcon(ap);
                        item.line1 = ap.getSsid();
                        if (ap.isActive()) {
                            summary = ap.getSummary();
                        } else {
                            summary = null;
                        }
                        item.line2 = summary;
                        if (ap.getSecurity() != 0) {
                            drawable = WifiTile.this.mContext.getDrawable(R.drawable.qs_ic_wifi_lock);
                        } else {
                            drawable = null;
                        }
                        item.overlay = drawable;
                        itemArr[i] = item;
                    }
                }
                this.mItems.setItems(itemArr);
            }
        }
    }

    private final class WifiSignalCallback extends SignalCallbackAdapter {
        final CallbackInfo mInfo;

        private WifiSignalCallback() {
            this.mInfo = new CallbackInfo();
        }

        public void setWifiIndicators(boolean enabled, IconState statusIcon, IconState qsIcon, boolean activityIn, boolean activityOut, String description) {
            if (WifiTile.DEBUG) {
                Log.d(WifiTile.this.TAG, "onWifiSignalChanged enabled=" + enabled);
            }
            this.mInfo.enabled = enabled;
            this.mInfo.connected = qsIcon.visible;
            this.mInfo.wifiSignalIconId = qsIcon.icon;
            this.mInfo.enabledDesc = description;
            this.mInfo.activityIn = activityIn;
            this.mInfo.activityOut = activityOut;
            this.mInfo.wifiSignalContentDescription = qsIcon.contentDescription;
            WifiTile.this.refreshState(this.mInfo);
        }
    }

    public WifiTile(Host host) {
        super(host);
        this.mController = host.getNetworkController();
        this.mWifiController = this.mController.getAccessPointController();
        this.mDetailAdapter = new WifiDetailAdapter();
    }

    public boolean supportsDualTargets() {
        return true;
    }

    protected SignalState newTileState() {
        return new SignalState();
    }

    public void setListening(boolean listening) {
        if (listening) {
            this.mController.addSignalCallback(this.mSignalCallback);
        } else {
            this.mController.removeSignalCallback(this.mSignalCallback);
        }
    }

    public void setDetailListening(boolean listening) {
        if (listening) {
            this.mWifiController.addAccessPointCallback(this.mDetailAdapter);
        } else {
            this.mWifiController.removeAccessPointCallback(this.mDetailAdapter);
        }
    }

    public DetailAdapter getDetailAdapter() {
        return this.mDetailAdapter;
    }

    public QSTileView createTileView(Context context) {
        return new SignalTileView(context);
    }

    protected void handleClick() {
        boolean z;
        boolean z2 = false;
        ((SignalState) this.mState).copyTo(this.mStateBeforeClick);
        Context context = this.mContext;
        int metricsCategory = getMetricsCategory();
        if (((SignalState) this.mState).enabled) {
            z = false;
        } else {
            z = true;
        }
        MetricsLogger.action(context, metricsCategory, z);
        NetworkController networkController = this.mController;
        if (!((SignalState) this.mState).enabled) {
            z2 = true;
        }
        networkController.setWifiEnabled(z2);
    }

    protected void handleLongClick() {
        if (this.mWifiController.canConfigWifi()) {
            if (!((SignalState) this.mState).enabled) {
                this.mController.setWifiEnabled(true);
                ((SignalState) this.mState).enabled = true;
            }
            showDetail(true);
            return;
        }
        this.mHost.startActivityDismissingKeyguard(new Intent("android.settings.WIFI_SETTINGS"));
    }

    protected void handleSecondaryClick() {
        if (this.mWifiController.canConfigWifi()) {
            if (!((SignalState) this.mState).enabled) {
                this.mController.setWifiEnabled(true);
                ((SignalState) this.mState).enabled = true;
            }
            showDetail(true);
            return;
        }
        this.mHost.startActivityDismissingKeyguard(new Intent("android.settings.WIFI_SETTINGS"));
    }

    protected void handleUpdateState(SignalState state, Object arg) {
        String signalContentDescription;
        state.visible = true;
        if (DEBUG) {
            Log.d(this.TAG, "handleUpdateState arg=" + arg);
        }
        CallbackInfo cb = (CallbackInfo) arg;
        if (cb == null) {
            cb = this.mSignalCallback.mInfo;
        }
        boolean wifiConnected = cb.enabled && cb.wifiSignalIconId > 0 && cb.enabledDesc != null;
        boolean wifiNotConnected = cb.wifiSignalIconId > 0 && cb.enabledDesc == null;
        if (state.enabled != cb.enabled) {
            this.mDetailAdapter.setItemsVisible(cb.enabled);
            fireToggleStateChanged(cb.enabled);
        }
        state.enabled = cb.enabled;
        state.connected = wifiConnected;
        state.activityIn = cb.enabled ? cb.activityIn : false;
        state.activityOut = cb.enabled ? cb.activityOut : false;
        state.filter = true;
        Resources r = this.mContext.getResources();
        if (!state.enabled) {
            state.icon = ResourceIcon.get(R.drawable.ic_qs_wifi_disabled);
            state.label = r.getString(R.string.quick_settings_wifi_label);
            signalContentDescription = r.getString(R.string.accessibility_wifi_off);
        } else if (wifiConnected) {
            state.icon = ResourceIcon.get(cb.wifiSignalIconId);
            state.label = removeDoubleQuotes(cb.enabledDesc);
            signalContentDescription = cb.wifiSignalContentDescription;
        } else if (wifiNotConnected) {
            state.icon = ResourceIcon.get(R.drawable.ic_qs_wifi_full_0);
            state.label = r.getString(R.string.quick_settings_wifi_label);
            signalContentDescription = r.getString(R.string.accessibility_no_wifi);
        } else {
            state.icon = ResourceIcon.get(R.drawable.ic_qs_wifi_no_network);
            state.label = r.getString(R.string.quick_settings_wifi_label);
            signalContentDescription = r.getString(R.string.accessibility_wifi_off);
        }
        state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_wifi, new Object[]{signalContentDescription});
        String wifiName = state.label;
        if (state.connected) {
            wifiName = r.getString(R.string.accessibility_wifi_name, new Object[]{state.label});
        }
        state.dualLabelContentDescription = wifiName;
    }

    public int getMetricsCategory() {
        return 126;
    }

    protected boolean shouldAnnouncementBeDelayed() {
        return this.mStateBeforeClick.enabled == ((SignalState) this.mState).enabled;
    }

    protected String composeChangeAnnouncement() {
        if (((SignalState) this.mState).enabled) {
            return this.mContext.getString(R.string.accessibility_quick_settings_wifi_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_wifi_changed_off);
    }

    private static String removeDoubleQuotes(String string) {
        if (string == null) {
            return null;
        }
        int length = string.length();
        if (length > 1 && string.charAt(0) == '\"' && string.charAt(length - 1) == '\"') {
            return string.substring(1, length - 1);
        }
        return string;
    }
}
