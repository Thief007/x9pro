package com.android.systemui.qs.tiles;

import android.content.Context;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSTile.Host;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.LocationController.LocationSettingsChangeCallback;

public class LocationTile extends QSTile<BooleanState> {
    private final Callback mCallback = new Callback();
    private final LocationController mController;
    private final AnimationIcon mDisable = new AnimationIcon(R.drawable.ic_signal_location_disable_animation);
    private final AnimationIcon mEnable = new AnimationIcon(R.drawable.ic_signal_location_enable_animation);
    private final KeyguardMonitor mKeyguard;

    private final class Callback implements LocationSettingsChangeCallback, com.android.systemui.statusbar.policy.KeyguardMonitor.Callback {
        private Callback() {
        }

        public void onLocationSettingsChanged(boolean enabled) {
            LocationTile.this.refreshState();
        }

        public void onKeyguardChanged() {
            LocationTile.this.refreshState();
        }
    }

    public LocationTile(Host host) {
        super(host);
        this.mController = host.getLocationController();
        this.mKeyguard = host.getKeyguardMonitor();
    }

    protected BooleanState newTileState() {
        return new BooleanState();
    }

    public void setListening(boolean listening) {
        if (listening) {
            this.mController.addSettingsChangedCallback(this.mCallback);
            this.mKeyguard.addCallback(this.mCallback);
            return;
        }
        this.mController.removeSettingsChangedCallback(this.mCallback);
        this.mKeyguard.removeCallback(this.mCallback);
    }

    protected void handleClick() {
        boolean z;
        boolean z2 = false;
        boolean wasEnabled = Boolean.valueOf(((BooleanState) this.mState).value).booleanValue();
        Context context = this.mContext;
        int metricsCategory = getMetricsCategory();
        if (wasEnabled) {
            z = false;
        } else {
            z = true;
        }
        MetricsLogger.action(context, metricsCategory, z);
        LocationController locationController = this.mController;
        if (!wasEnabled) {
            z2 = true;
        }
        locationController.setLocationEnabled(z2);
        this.mEnable.setAllowAnimation(true);
        this.mDisable.setAllowAnimation(true);
    }

    protected void handleUpdateState(BooleanState state, Object arg) {
        boolean locationEnabled = this.mController.isLocationEnabled();
        state.visible = !this.mKeyguard.isShowing();
        state.value = locationEnabled;
        if (locationEnabled) {
            state.icon = this.mEnable;
            state.label = this.mContext.getString(R.string.quick_settings_location_label);
            state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_location_on);
            return;
        }
        state.icon = this.mDisable;
        state.label = this.mContext.getString(R.string.quick_settings_location_label);
        state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_location_off);
    }

    public int getMetricsCategory() {
        return 122;
    }

    protected String composeChangeAnnouncement() {
        if (((BooleanState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_quick_settings_location_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_location_changed_off);
    }
}
