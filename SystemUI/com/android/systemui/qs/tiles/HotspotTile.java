package com.android.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.GlobalSetting;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSTile.Host;
import com.android.systemui.qs.QSTile.Icon;
import com.android.systemui.qs.UsageTracker;
import com.android.systemui.statusbar.policy.HotspotController;

public class HotspotTile extends QSTile<BooleanState> {
    private final Callback mCallback = new Callback();
    private final HotspotController mController;
    private final AnimationIcon mDisable = new AnimationIcon(R.drawable.ic_hotspot_disable_animation);
    private final AnimationIcon mEnable = new AnimationIcon(R.drawable.ic_hotspot_enable_animation);
    private boolean mListening;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.AIRPLANE_MODE".equals(intent.getAction())) {
                HotspotTile.this.refreshState();
            }
        }
    };
    private final GlobalSetting mSetting;
    private final UsageTracker mUsageTracker;

    public static class APChangedReceiver extends BroadcastReceiver {
        private UsageTracker mUsageTracker;

        public void onReceive(Context context, Intent intent) {
            if (this.mUsageTracker == null) {
                this.mUsageTracker = HotspotTile.newUsageTracker(context);
            }
            this.mUsageTracker.trackUsage();
        }
    }

    private final class Callback implements com.android.systemui.statusbar.policy.HotspotController.Callback {
        private Callback() {
        }

        public void onHotspotChanged(boolean enabled) {
            HotspotTile.this.refreshState(Boolean.valueOf(enabled));
        }
    }

    public HotspotTile(Host host) {
        super(host);
        this.mController = host.getHotspotController();
        this.mUsageTracker = newUsageTracker(host.getContext());
        this.mUsageTracker.setListening(true);
        this.mSetting = new GlobalSetting(this.mContext, this.mHandler, "airplane_mode_on") {
            protected void handleValueChanged(int value) {
                HotspotTile.this.handleRefreshState(Integer.valueOf(value));
            }
        };
    }

    protected void handleDestroy() {
        super.handleDestroy();
        this.mUsageTracker.setListening(false);
    }

    protected BooleanState newTileState() {
        return new BooleanState();
    }

    public void setListening(boolean listening) {
        if (this.mListening != listening) {
            this.mListening = listening;
            if (listening) {
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.AIRPLANE_MODE");
                this.mContext.registerReceiver(this.mReceiver, filter);
            } else {
                this.mContext.unregisterReceiver(this.mReceiver);
            }
            this.mSetting.setListening(listening);
            if (listening) {
                this.mController.addCallback(this.mCallback);
            } else {
                this.mController.removeCallback(this.mCallback);
            }
        }
    }

    protected void handleClick() {
        boolean z = false;
        boolean airplaneMode = this.mSetting.getValue() != 0;
        Log.d(this.TAG, "handleClick(), airplaneMode= " + airplaneMode);
        if (airplaneMode) {
            refreshState();
            return;
        }
        boolean isEnabled = Boolean.valueOf(((BooleanState) this.mState).value).booleanValue();
        MetricsLogger.action(this.mContext, getMetricsCategory(), !isEnabled);
        HotspotController hotspotController = this.mController;
        if (!isEnabled) {
            z = true;
        }
        hotspotController.setHotspotEnabled(z);
        this.mEnable.setAllowAnimation(true);
        this.mDisable.setAllowAnimation(true);
    }

    protected void handleLongClick() {
        if (!((BooleanState) this.mState).value) {
            this.mUsageTracker.showResetConfirmation(this.mContext.getString(R.string.quick_settings_reset_confirmation_title, new Object[]{((BooleanState) this.mState).label}), new Runnable() {
                public void run() {
                    HotspotTile.this.refreshState();
                }
            });
        }
    }

    protected void handleUpdateState(BooleanState state, Object arg) {
        boolean isRecentlyUsed;
        Icon icon;
        if (this.mController.isHotspotSupported()) {
            isRecentlyUsed = this.mUsageTracker.isRecentlyUsed();
        } else {
            isRecentlyUsed = false;
        }
        state.visible = isRecentlyUsed;
        state.label = this.mContext.getString(R.string.quick_settings_hotspot_label);
        if (arg instanceof Boolean) {
            state.value = ((Boolean) arg).booleanValue();
        } else {
            state.value = this.mController.isHotspotEnabled();
        }
        boolean airplaneMode = (arg instanceof Integer ? ((Integer) arg).intValue() : this.mSetting.getValue()) != 0;
        if (DEBUG) {
            Log.d(this.TAG, "handleUpdateState(), airplaneMode= " + airplaneMode);
        }
        if (airplaneMode) {
            state.value = false;
        }
        if (state.visible && state.value) {
            icon = this.mEnable;
        } else {
            icon = this.mDisable;
        }
        state.icon = icon;
    }

    public int getMetricsCategory() {
        return 120;
    }

    protected String composeChangeAnnouncement() {
        if (((BooleanState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_quick_settings_hotspot_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_hotspot_changed_off);
    }

    private static UsageTracker newUsageTracker(Context context) {
        return new UsageTracker(context, "HotspotTileLastUsed", HotspotTile.class, R.integer.days_to_show_hotspot_tile);
    }
}
