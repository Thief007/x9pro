package com.mediatek.systemui.qs.tiles.ext;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SubscriptionManager;
import android.util.Log;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSTile.Host;
import com.android.systemui.qs.QSTile.Icon;
import com.mediatek.systemui.ext.IQuickSettingsPlugin;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;
import com.mediatek.systemui.statusbar.extcb.PluginFactory;

public class DualSimSettingsTile extends QSTile<BooleanState> {
    private static final Intent DUAL_SIM_SETTINGS = new Intent().setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$SimSettingsActivity"));
    private static final String TAG = "DualSimSettingsTile";
    private final Icon mDualSimSettingsIcon = new QsIconWrapper(this.mIconIdWrapper);
    private final IconIdWrapper mIconIdWrapper = new IconIdWrapper();
    private BroadcastReceiver mSimStateIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(DualSimSettingsTile.TAG, "onReceive action is " + action);
            if (action.equals("android.intent.action.SIM_STATE_CHANGED")) {
                String stateExtra = intent.getStringExtra("ss");
                Log.d(DualSimSettingsTile.TAG, "onReceive action is " + action + " stateExtra=" + stateExtra);
                if ("ABSENT".equals(stateExtra)) {
                    DualSimSettingsTile.this.handleRefreshState(Boolean.valueOf(false));
                } else {
                    DualSimSettingsTile.this.handleRefreshState(Boolean.valueOf(true));
                }
            }
        }
    };

    public DualSimSettingsTile(Host host) {
        super(host);
        registerSimStateReceiver();
    }

    protected BooleanState newTileState() {
        return new BooleanState();
    }

    public void setListening(boolean listening) {
    }

    public int getMetricsCategory() {
        return 111;
    }

    protected void handleClick() {
        long subId = (long) SubscriptionManager.getDefaultDataSubId();
        Log.d(TAG, "handleClick, " + DUAL_SIM_SETTINGS);
        DUAL_SIM_SETTINGS.putExtra("subscription", subId);
        this.mHost.startActivityDismissingKeyguard(DUAL_SIM_SETTINGS);
        refreshState();
    }

    protected void handleUpdateState(BooleanState state, Object arg) {
        state.visible = true;
        Boolean simInserted = (Boolean) arg;
        Log.d(TAG, "handleUpdateState,  simInserted=" + simInserted);
        IQuickSettingsPlugin quickSettingsPlugin = PluginFactory.getQuickSettingsPlugin(this.mContext);
        if (simInserted == null || !simInserted.booleanValue()) {
            state.label = quickSettingsPlugin.customizeDualSimSettingsTile(true, this.mIconIdWrapper, "");
        } else {
            state.label = quickSettingsPlugin.customizeDualSimSettingsTile(false, this.mIconIdWrapper, "");
        }
        state.icon = this.mDualSimSettingsIcon;
    }

    private void registerSimStateReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SIM_STATE_CHANGED");
        this.mContext.registerReceiver(this.mSimStateIntentReceiver, filter);
    }
}
