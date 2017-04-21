package com.android.systemui.qs.tiles;

import android.content.Context;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSTile.Host;
import com.android.systemui.qs.QSTile.Icon;
import com.android.systemui.qs.SecureSetting;
import com.android.systemui.qs.UsageTracker;

public class ColorInversionTile extends QSTile<BooleanState> {
    private final AnimationIcon mDisable = new AnimationIcon(R.drawable.ic_invert_colors_disable_animation);
    private final AnimationIcon mEnable = new AnimationIcon(R.drawable.ic_invert_colors_enable_animation);
    private boolean mListening;
    private final SecureSetting mSetting = new SecureSetting(this.mContext, this.mHandler, "accessibility_display_inversion_enabled") {
        protected void handleValueChanged(int value, boolean observedChange) {
            if (value != 0 || observedChange) {
                ColorInversionTile.this.mUsageTracker.trackUsage();
            }
            if (ColorInversionTile.this.mListening) {
                ColorInversionTile.this.handleRefreshState(Integer.valueOf(value));
            }
        }
    };
    private final UsageTracker mUsageTracker;

    public ColorInversionTile(Host host) {
        super(host);
        this.mUsageTracker = new UsageTracker(host.getContext(), "ColorInversionTileLastUsed", ColorInversionTile.class, R.integer.days_to_show_color_inversion_tile);
        if (!(this.mSetting.getValue() == 0 || this.mUsageTracker.isRecentlyUsed())) {
            this.mUsageTracker.trackUsage();
        }
        this.mUsageTracker.setListening(true);
        this.mSetting.setListening(true);
    }

    protected void handleDestroy() {
        super.handleDestroy();
        this.mUsageTracker.setListening(false);
        this.mSetting.setListening(false);
    }

    protected BooleanState newTileState() {
        return new BooleanState();
    }

    public void setListening(boolean listening) {
        this.mListening = listening;
    }

    protected void handleUserSwitch(int newUserId) {
        this.mSetting.setUserId(newUserId);
        handleRefreshState(Integer.valueOf(this.mSetting.getValue()));
    }

    protected void handleClick() {
        boolean z;
        int i = 0;
        Context context = this.mContext;
        int metricsCategory = getMetricsCategory();
        if (((BooleanState) this.mState).value) {
            z = false;
        } else {
            z = true;
        }
        MetricsLogger.action(context, metricsCategory, z);
        SecureSetting secureSetting = this.mSetting;
        if (!((BooleanState) this.mState).value) {
            i = 1;
        }
        secureSetting.setValue(i);
        this.mEnable.setAllowAnimation(true);
        this.mDisable.setAllowAnimation(true);
    }

    protected void handleLongClick() {
        if (!((BooleanState) this.mState).value) {
            this.mUsageTracker.showResetConfirmation(this.mContext.getString(R.string.quick_settings_reset_confirmation_title, new Object[]{((BooleanState) this.mState).label}), new Runnable() {
                public void run() {
                    ColorInversionTile.this.refreshState();
                }
            });
        }
    }

    protected void handleUpdateState(BooleanState state, Object arg) {
        Icon icon;
        boolean enabled = (arg instanceof Integer ? ((Integer) arg).intValue() : this.mSetting.getValue()) != 0;
        state.visible = !enabled ? this.mUsageTracker.isRecentlyUsed() : true;
        state.value = enabled;
        state.label = this.mContext.getString(R.string.quick_settings_inversion_label);
        if (enabled) {
            icon = this.mEnable;
        } else {
            icon = this.mDisable;
        }
        state.icon = icon;
    }

    public int getMetricsCategory() {
        return 116;
    }

    protected String composeChangeAnnouncement() {
        if (((BooleanState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_quick_settings_color_inversion_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_color_inversion_changed_off);
    }
}
