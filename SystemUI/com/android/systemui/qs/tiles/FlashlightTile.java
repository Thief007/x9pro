package com.android.systemui.qs.tiles;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSTile.Host;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.android.systemui.statusbar.policy.FlashlightController.FlashlightListener;

public class FlashlightTile extends QSTile<BooleanState> implements FlashlightListener {
    private final AnimationIcon mDisable = new AnimationIcon(R.drawable.ic_signal_flashlight_disable_animation);
    private final AnimationIcon mEnable = new AnimationIcon(R.drawable.ic_signal_flashlight_enable_animation);
    private final FlashlightController mFlashlightController;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            FlashlightTile.this.handleClick();
        }
    };

    public FlashlightTile(Host host) {
        super(host);
        this.mFlashlightController = host.getFlashlightController();
        this.mFlashlightController.addListener(this);
        this.mContext.registerReceiver(this.receiver, new IntentFilter("com.android.vanzo.torch"));
    }

    protected void handleDestroy() {
        super.handleDestroy();
        this.mFlashlightController.removeListener(this);
    }

    protected BooleanState newTileState() {
        return new BooleanState();
    }

    public void setListening(boolean listening) {
    }

    protected void handleUserSwitch(int newUserId) {
    }

    protected void handleClick() {
        if (!ActivityManager.isUserAMonkey()) {
            MetricsLogger.action(this.mContext, getMetricsCategory(), !((BooleanState) this.mState).value);
            boolean newState = !((BooleanState) this.mState).value;
            refreshState(newState ? UserBoolean.USER_TRUE : UserBoolean.USER_FALSE);
            this.mFlashlightController.setFlashlight(newState);
        }
    }

    protected void handleUpdateState(BooleanState state, Object arg) {
        int onOrOffId;
        state.visible = this.mFlashlightController.isAvailable();
        state.label = this.mHost.getContext().getString(R.string.quick_settings_flashlight_label);
        if (arg instanceof UserBoolean) {
            boolean value = ((UserBoolean) arg).value;
            if (value != state.value) {
                state.value = value;
            } else {
                return;
            }
        }
        state.value = this.mFlashlightController.isEnabled();
        AnimationIcon icon = state.value ? this.mEnable : this.mDisable;
        icon.setAllowAnimation(arg instanceof UserBoolean ? ((UserBoolean) arg).userInitiated : false);
        state.icon = icon;
        if (state.value) {
            onOrOffId = R.string.accessibility_quick_settings_flashlight_on;
        } else {
            onOrOffId = R.string.accessibility_quick_settings_flashlight_off;
        }
        state.contentDescription = this.mContext.getString(onOrOffId);
    }

    public int getMetricsCategory() {
        return 119;
    }

    protected String composeChangeAnnouncement() {
        if (((BooleanState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_quick_settings_flashlight_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_flashlight_changed_off);
    }

    public void onFlashlightChanged(boolean enabled) {
        refreshState(enabled ? UserBoolean.BACKGROUND_TRUE : UserBoolean.BACKGROUND_FALSE);
    }

    public void onFlashlightError() {
        refreshState(UserBoolean.BACKGROUND_FALSE);
    }

    public void onFlashlightAvailabilityChanged(boolean available) {
        refreshState();
    }
}
