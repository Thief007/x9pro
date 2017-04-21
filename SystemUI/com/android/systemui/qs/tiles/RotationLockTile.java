package com.android.systemui.qs.tiles;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSTile.Host;
import com.android.systemui.statusbar.policy.RotationLockController;
import com.android.systemui.statusbar.policy.RotationLockController.RotationLockControllerCallback;

public class RotationLockTile extends QSTile<BooleanState> {
    private final AnimationIcon mAutoToLandscape = new AnimationIcon(R.drawable.ic_landscape_from_auto_rotate_animation);
    private final AnimationIcon mAutoToPortrait = new AnimationIcon(R.drawable.ic_portrait_from_auto_rotate_animation);
    private final RotationLockControllerCallback mCallback = new RotationLockControllerCallback() {
        public void onRotationLockStateChanged(boolean rotationLocked, boolean affordanceVisible) {
            Object obj;
            RotationLockTile rotationLockTile = RotationLockTile.this;
            if (rotationLocked) {
                obj = UserBoolean.BACKGROUND_TRUE;
            } else {
                obj = UserBoolean.BACKGROUND_FALSE;
            }
            rotationLockTile.refreshState(obj);
        }
    };
    private final RotationLockController mController;
    private final AnimationIcon mLandscapeToAuto = new AnimationIcon(R.drawable.ic_landscape_to_auto_rotate_animation);
    private final AnimationIcon mPortraitToAuto = new AnimationIcon(R.drawable.ic_portrait_to_auto_rotate_animation);

    public RotationLockTile(Host host) {
        super(host);
        this.mController = host.getRotationLockController();
    }

    protected BooleanState newTileState() {
        return new BooleanState();
    }

    public void setListening(boolean listening) {
        if (this.mController != null) {
            if (listening) {
                this.mController.addRotationLockControllerCallback(this.mCallback);
            } else {
                this.mController.removeRotationLockControllerCallback(this.mCallback);
            }
        }
    }

    protected void handleClick() {
        if (this.mController != null) {
            Object obj;
            MetricsLogger.action(this.mContext, getMetricsCategory(), !((BooleanState) this.mState).value);
            boolean newState = !((BooleanState) this.mState).value;
            this.mController.setRotationLocked(newState);
            if (newState) {
                obj = UserBoolean.USER_TRUE;
            } else {
                obj = UserBoolean.USER_FALSE;
            }
            refreshState(obj);
        }
    }

    protected void handleUpdateState(BooleanState state, Object arg) {
        if (this.mController != null) {
            boolean rotationLocked;
            if (arg != null) {
                rotationLocked = ((UserBoolean) arg).value;
            } else {
                rotationLocked = this.mController.isRotationLocked();
            }
            boolean z = arg != null ? ((UserBoolean) arg).userInitiated : false;
            state.visible = this.mController.isRotationLockAffordanceVisible();
            if (state.value != rotationLocked || state.contentDescription == null) {
                AnimationIcon icon;
                state.value = rotationLocked;
                boolean portrait = isCurrentOrientationLockPortrait();
                if (rotationLocked) {
                    int label;
                    if (portrait) {
                        label = R.string.quick_settings_rotation_locked_portrait_label;
                    } else {
                        label = R.string.quick_settings_rotation_locked_landscape_label;
                    }
                    state.label = this.mContext.getString(label);
                    icon = portrait ? this.mAutoToPortrait : this.mAutoToLandscape;
                } else {
                    state.label = this.mContext.getString(R.string.quick_settings_rotation_unlocked_label);
                    icon = portrait ? this.mPortraitToAuto : this.mLandscapeToAuto;
                }
                icon.setAllowAnimation(z);
                state.icon = icon;
                state.contentDescription = getAccessibilityString(rotationLocked, R.string.accessibility_rotation_lock_on_portrait, R.string.accessibility_rotation_lock_on_landscape, R.string.accessibility_rotation_lock_off);
            }
        }
    }

    private boolean isCurrentOrientationLockPortrait() {
        boolean z = true;
        int lockOrientation = this.mController.getRotationLockOrientation();
        if (lockOrientation == 0) {
            if (this.mContext.getResources().getConfiguration().orientation == 2) {
                z = false;
            }
            return z;
        }
        if (lockOrientation == 2) {
            z = false;
        }
        return z;
    }

    public int getMetricsCategory() {
        return 123;
    }

    private String getAccessibilityString(boolean locked, int idWhenPortrait, int idWhenLandscape, int idWhenOff) {
        int stringID = locked ? isCurrentOrientationLockPortrait() ? idWhenPortrait : idWhenLandscape : idWhenOff;
        return this.mContext.getString(stringID);
    }

    protected String composeChangeAnnouncement() {
        return getAccessibilityString(((BooleanState) this.mState).value, R.string.accessibility_rotation_lock_on_portrait_changed, R.string.accessibility_rotation_lock_on_landscape_changed, R.string.accessibility_rotation_lock_off_changed);
    }
}
