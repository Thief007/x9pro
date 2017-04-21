package com.android.systemui.qs.tiles;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTile.DetailAdapter;
import com.android.systemui.qs.QSTile.Host;
import com.android.systemui.qs.QSTile.Icon;
import com.android.systemui.qs.QSTile.ResourceIcon;
import com.android.systemui.qs.QSTile.SignalState;
import com.android.systemui.qs.QSTileView;
import com.android.systemui.qs.SignalTileView;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.NetworkController.MobileDataController;
import com.android.systemui.statusbar.policy.NetworkController.MobileDataController.DataUsageInfo;
import com.android.systemui.statusbar.policy.SignalCallbackAdapter;
import com.mediatek.systemui.ext.IQuickSettingsPlugin;
import com.mediatek.systemui.statusbar.extcb.PluginFactory;

public class CellularTile extends QSTile<SignalState> {
    private static final Intent CELLULAR_SETTINGS = new Intent().setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity"));
    private final NetworkController mController;
    private final MobileDataController mDataController;
    private final CellularDetailAdapter mDetailAdapter;
    private boolean mDisplayDataUsage;
    private Icon mIcon;
    private IQuickSettingsPlugin mQuickSettingsPlugin;
    private final CellSignalCallback mSignalCallback = new CellSignalCallback();

    private static final class CallbackInfo {
        boolean activityIn;
        boolean activityOut;
        boolean airplaneModeEnabled;
        String dataContentDescription;
        int dataTypeIconId;
        boolean enabled;
        String enabledDesc;
        boolean isDataTypeIconWide;
        int mobileSignalIconId;
        boolean noSim;
        String signalContentDescription;
        boolean wifiEnabled;

        private CallbackInfo() {
        }
    }

    private final class CellSignalCallback extends SignalCallbackAdapter {
        private final CallbackInfo mInfo;

        private CellSignalCallback() {
            this.mInfo = new CallbackInfo();
        }

        public void setWifiIndicators(boolean enabled, IconState statusIcon, IconState qsIcon, boolean activityIn, boolean activityOut, String description) {
            this.mInfo.wifiEnabled = enabled;
            CellularTile.this.refreshState(this.mInfo);
        }

        public void setMobileDataIndicators(IconState statusIcon, IconState qsIcon, int statusType, int networkIcon, int qsType, boolean activityIn, boolean activityOut, int dataActivity, int primarySimIcon, String typeContentDescription, String description, boolean isWide, int subId) {
            if (qsIcon == null) {
                Log.d("CellularTile", "setMobileDataIndicator qsIcon = null, Not data sim, don't display");
                return;
            }
            this.mInfo.enabled = qsIcon.visible;
            this.mInfo.mobileSignalIconId = qsIcon.icon;
            this.mInfo.signalContentDescription = qsIcon.contentDescription;
            this.mInfo.dataTypeIconId = qsType;
            this.mInfo.dataContentDescription = typeContentDescription;
            this.mInfo.activityIn = activityIn;
            this.mInfo.activityOut = activityOut;
            this.mInfo.enabledDesc = description;
            CallbackInfo callbackInfo = this.mInfo;
            if (qsType == 0) {
                isWide = false;
            }
            callbackInfo.isDataTypeIconWide = isWide;
            Log.d("CellularTile", "setMobileDataIndicators info.enabled = " + this.mInfo.enabled + " mInfo.mobileSignalIconId = " + this.mInfo.mobileSignalIconId + " mInfo.signalContentDescription = " + this.mInfo.signalContentDescription + " mInfo.dataTypeIconId = " + this.mInfo.dataTypeIconId + " mInfo.dataContentDescription = " + this.mInfo.dataContentDescription + " mInfo.activityIn = " + this.mInfo.activityIn + " mInfo.activityOut = " + this.mInfo.activityOut + " mInfo.enabledDesc = " + this.mInfo.enabledDesc + " mInfo.isDataTypeIconWide = " + this.mInfo.isDataTypeIconWide);
            CellularTile.this.refreshState(this.mInfo);
        }

        public void setNoSims(boolean show) {
            Log.d("CellularTile", "setNoSims, noSim = " + show);
            this.mInfo.noSim = show;
            if (this.mInfo.noSim) {
                this.mInfo.mobileSignalIconId = 0;
                this.mInfo.dataTypeIconId = 0;
                this.mInfo.enabled = true;
                this.mInfo.enabledDesc = CellularTile.this.mContext.getString(R.string.keyguard_missing_sim_message_short);
                this.mInfo.signalContentDescription = this.mInfo.enabledDesc;
            }
            CellularTile.this.refreshState(this.mInfo);
        }

        public void setIsAirplaneMode(IconState icon) {
            this.mInfo.airplaneModeEnabled = icon.visible;
            CellularTile.this.refreshState(this.mInfo);
        }

        public void setMobileDataEnabled(boolean enabled) {
            CellularTile.this.mDetailAdapter.setMobileDataEnabled(enabled);
        }
    }

    private final class CellularDetailAdapter implements DetailAdapter {
        private CellularDetailAdapter() {
        }

        public int getTitle() {
            return R.string.quick_settings_cellular_detail_title;
        }

        public Boolean getToggleState() {
            if (CellularTile.this.mDataController.isMobileDataSupported()) {
                return Boolean.valueOf(CellularTile.this.mDataController.isMobileDataEnabled());
            }
            return null;
        }

        public Intent getSettingsIntent() {
            return CellularTile.CELLULAR_SETTINGS;
        }

        public void setToggleState(boolean state) {
            MetricsLogger.action(CellularTile.this.mContext, 155, state);
            CellularTile.this.mDataController.setMobileDataEnabled(state);
        }

        public int getMetricsCategory() {
            return 117;
        }

        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            DataUsageDetailView v;
            if (convertView != null) {
                v = convertView;
            } else {
                v = LayoutInflater.from(CellularTile.this.mContext).inflate(R.layout.data_usage, parent, false);
            }
            v = v;
            DataUsageInfo info = CellularTile.this.mDataController.getDataUsageInfo();
            if (info == null) {
                return v;
            }
            v.bind(info);
            return v;
        }

        public void setMobileDataEnabled(boolean enabled) {
            CellularTile.this.fireToggleStateChanged(enabled);
        }
    }

    public CellularTile(Host host) {
        super(host);
        this.mController = host.getNetworkController();
        this.mDataController = this.mController.getMobileDataController();
        this.mDetailAdapter = new CellularDetailAdapter();
        this.mQuickSettingsPlugin = PluginFactory.getQuickSettingsPlugin(this.mContext);
        this.mDisplayDataUsage = this.mQuickSettingsPlugin.customizeDisplayDataUsage(false);
        this.mIcon = ResourceIcon.get(R.drawable.ic_qs_data_usage);
    }

    protected SignalState newTileState() {
        return new SignalState();
    }

    public DetailAdapter getDetailAdapter() {
        return this.mDetailAdapter;
    }

    public void setListening(boolean listening) {
        if (listening) {
            this.mController.addSignalCallback(this.mSignalCallback);
        } else {
            this.mController.removeSignalCallback(this.mSignalCallback);
        }
    }

    public QSTileView createTileView(Context context) {
        return new SignalTileView(context);
    }

    protected void handleClick() {
        MetricsLogger.action(this.mContext, getMetricsCategory());
        if (this.mDataController.isMobileDataSupported() && this.mDataController.isDefaultDataSimExist()) {
            showDetail(true);
        } else {
            this.mHost.startActivityDismissingKeyguard(CELLULAR_SETTINGS);
        }
    }

    protected void handleUpdateState(SignalState state, Object arg) {
        if (this.mDisplayDataUsage) {
            Log.i("CellularTile", "customize datausage, displayDataUsage = " + this.mDisplayDataUsage);
            state.visible = true;
            state.icon = this.mIcon;
            state.label = this.mContext.getString(R.string.data_usage);
            state.contentDescription = this.mContext.getString(R.string.data_usage);
            return;
        }
        state.visible = this.mController.hasMobileDataFeature();
        if (state.visible) {
            int iconId;
            boolean z;
            String removeTrailingPeriod;
            String signalContentDesc;
            String dataContentDesc;
            CallbackInfo cb = (CallbackInfo) arg;
            if (cb == null) {
                cb = this.mSignalCallback.mInfo;
            }
            Resources r = this.mContext.getResources();
            if (cb.noSim) {
                iconId = R.drawable.ic_qs_no_sim;
            } else if (!cb.enabled || cb.airplaneModeEnabled) {
                iconId = R.drawable.ic_qs_signal_disabled;
            } else if (cb.mobileSignalIconId > 0) {
                iconId = cb.mobileSignalIconId;
            } else {
                iconId = R.drawable.ic_qs_signal_no_signal;
            }
            state.icon = ResourceIcon.get(iconId);
            state.isOverlayIconWide = cb.isDataTypeIconWide;
            if (cb.noSim) {
                z = false;
            } else {
                z = true;
            }
            state.autoMirrorDrawable = z;
            int i = (!cb.enabled || cb.dataTypeIconId <= 0 || cb.airplaneModeEnabled) ? 0 : cb.dataTypeIconId;
            state.overlayIconId = i;
            if (iconId != R.drawable.ic_qs_no_sim) {
                z = true;
            } else {
                z = false;
            }
            state.filter = z;
            if (cb.enabled) {
                z = cb.activityIn;
            } else {
                z = false;
            }
            state.activityIn = z;
            if (cb.enabled) {
                z = cb.activityOut;
            } else {
                z = false;
            }
            state.activityOut = z;
            if (cb.enabled) {
                removeTrailingPeriod = removeTrailingPeriod(cb.enabledDesc);
            } else {
                removeTrailingPeriod = r.getString(R.string.quick_settings_rssi_emergency_only);
            }
            state.label = removeTrailingPeriod;
            if (!cb.enabled || cb.mobileSignalIconId <= 0) {
                signalContentDesc = r.getString(R.string.accessibility_no_signal);
            } else {
                signalContentDesc = cb.signalContentDescription;
            }
            if (!cb.enabled || cb.dataTypeIconId <= 0 || cb.wifiEnabled) {
                dataContentDesc = r.getString(R.string.accessibility_no_data);
            } else {
                dataContentDesc = cb.dataContentDescription;
            }
            state.contentDescription = r.getString(R.string.accessibility_quick_settings_mobile, new Object[]{signalContentDesc, dataContentDesc, state.label});
            if (!(TelephonyManager.from(this.mContext).getNetworkOperator() == null || cb.noSim || this.mDataController.isDefaultDataSimExist())) {
                Log.d("CellularTile", "Default data sim not exist");
                state.icon = ResourceIcon.get(R.drawable.ic_qs_data_sim_not_set);
                state.label = r.getString(R.string.quick_settings_data_sim_notset);
                state.overlayIconId = 0;
                state.filter = true;
                state.activityIn = false;
                state.activityOut = false;
            }
        }
    }

    public int getMetricsCategory() {
        return 115;
    }

    public static String removeTrailingPeriod(String string) {
        if (string == null) {
            return null;
        }
        int length = string.length();
        if (string.endsWith(".")) {
            return string.substring(0, length - 1);
        }
        return string;
    }
}
