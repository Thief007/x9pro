package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.R;
import com.android.systemui.statusbar.KeyguardAffordanceView;
import com.android.systemui.statusbar.policy.AccessibilityController;

public class LockIcon extends KeyguardAffordanceView {
    private AccessibilityController mAccessibilityController;
    private boolean mDeviceInteractive;
    private boolean mHasFingerPrintIcon;
    private boolean mLastDeviceInteractive;
    private boolean mLastScreenOn;
    private int mLastState = 0;
    private boolean mScreenOn;
    private boolean mTransientFpError;
    private final TrustDrawable mTrustDrawable;
    private final UnlockMethodCache mUnlockMethodCache;

    private static class IntrinsicSizeDrawable extends InsetDrawable {
        private final int mIntrinsicHeight;
        private final int mIntrinsicWidth;

        public IntrinsicSizeDrawable(Drawable drawable, int intrinsicWidth, int intrinsicHeight) {
            super(drawable, 0);
            this.mIntrinsicWidth = intrinsicWidth;
            this.mIntrinsicHeight = intrinsicHeight;
        }

        public int getIntrinsicWidth() {
            return this.mIntrinsicWidth;
        }

        public int getIntrinsicHeight() {
            return this.mIntrinsicHeight;
        }
    }

    public LockIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mTrustDrawable = new TrustDrawable(context);
        setBackground(this.mTrustDrawable);
        this.mUnlockMethodCache = UnlockMethodCache.getInstance(context);
    }

    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (isShown()) {
            this.mTrustDrawable.start();
        } else {
            this.mTrustDrawable.stop();
        }
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mTrustDrawable.stop();
    }

    public void setTransientFpError(boolean transientFpError) {
        this.mTransientFpError = transientFpError;
        update();
    }

    public void setDeviceInteractive(boolean deviceInteractive) {
        this.mDeviceInteractive = deviceInteractive;
        update();
    }

    public void setScreenOn(boolean screenOn) {
        this.mScreenOn = screenOn;
        update();
    }

    public void update() {
        boolean isDeviceInteractive;
        boolean trustManaged;
        AnimatedVectorDrawable animatedVectorDrawable;
        int dimensionPixelSize;
        if (isShown()) {
            isDeviceInteractive = KeyguardUpdateMonitor.getInstance(this.mContext).isDeviceInteractive();
        } else {
            isDeviceInteractive = false;
        }
        if (isDeviceInteractive) {
            this.mTrustDrawable.start();
        } else {
            this.mTrustDrawable.stop();
        }
        int state = getState();
        boolean anyFingerprintIcon = state == 3 || state == 4;
        boolean useAdditionalPadding = anyFingerprintIcon;
        boolean z = anyFingerprintIcon;
        if (state == this.mLastState && this.mDeviceInteractive == this.mLastDeviceInteractive) {
            if (this.mScreenOn != this.mLastScreenOn) {
            }
            trustManaged = this.mUnlockMethodCache.isTrustManaged() && !z;
            this.mTrustDrawable.setTrustManaged(trustManaged);
            updateClickability();
        }
        boolean isAnim = true;
        int iconRes = getAnimationResForTransition(this.mLastState, state, this.mLastDeviceInteractive, this.mDeviceInteractive, this.mLastScreenOn, this.mScreenOn);
        if (iconRes == R.drawable.lockscreen_fingerprint_draw_off_animation) {
            anyFingerprintIcon = true;
            useAdditionalPadding = true;
            z = true;
        } else if (iconRes == R.drawable.trusted_state_to_error_animation) {
            anyFingerprintIcon = true;
            useAdditionalPadding = false;
            z = true;
        } else if (iconRes == R.drawable.error_to_trustedstate_animation) {
            anyFingerprintIcon = true;
            useAdditionalPadding = false;
            z = false;
        }
        if (iconRes == -1) {
            iconRes = getIconForState(state, this.mScreenOn, this.mDeviceInteractive);
            isAnim = false;
        }
        Drawable icon = this.mContext.getDrawable(iconRes);
        if (icon instanceof AnimatedVectorDrawable) {
            animatedVectorDrawable = (AnimatedVectorDrawable) icon;
        } else {
            animatedVectorDrawable = null;
        }
        int iconHeight = getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_icon_height);
        int iconWidth = getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_icon_width);
        if (!(anyFingerprintIcon || (icon.getIntrinsicHeight() == iconHeight && icon.getIntrinsicWidth() == iconWidth))) {
            icon = new IntrinsicSizeDrawable(icon, iconWidth, iconHeight);
        }
        if (useAdditionalPadding) {
            dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.fingerprint_icon_additional_padding);
        } else {
            dimensionPixelSize = 0;
        }
        setPaddingRelative(0, 0, 0, dimensionPixelSize);
        setRestingAlpha(anyFingerprintIcon ? 1.0f : 0.5f);
        setImageDrawable(icon);
        Resources resources = getResources();
        if (anyFingerprintIcon) {
            dimensionPixelSize = R.string.accessibility_unlock_button_fingerprint;
        } else {
            dimensionPixelSize = R.string.accessibility_unlock_button;
        }
        setContentDescription(resources.getString(dimensionPixelSize));
        this.mHasFingerPrintIcon = anyFingerprintIcon;
        if (animatedVectorDrawable != null && isAnim) {
            animatedVectorDrawable.start();
        }
        this.mLastState = state;
        this.mLastDeviceInteractive = this.mDeviceInteractive;
        this.mLastScreenOn = this.mScreenOn;
        if (!this.mUnlockMethodCache.isTrustManaged()) {
        }
        this.mTrustDrawable.setTrustManaged(trustManaged);
        updateClickability();
    }

    private void updateClickability() {
        if (this.mAccessibilityController != null) {
            boolean clickToUnlock = this.mAccessibilityController.isTouchExplorationEnabled();
            boolean clickToForceLock = this.mUnlockMethodCache.isTrustManaged() ? !this.mAccessibilityController.isAccessibilityEnabled() : false;
            boolean longClickToForceLock = this.mUnlockMethodCache.isTrustManaged() ? !clickToForceLock : false;
            if (clickToForceLock) {
                clickToUnlock = true;
            }
            setClickable(clickToUnlock);
            setLongClickable(longClickToForceLock);
            setFocusable(this.mAccessibilityController.isAccessibilityEnabled());
        }
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        if (this.mHasFingerPrintIcon) {
            info.setClassName(LockIcon.class.getName());
            info.addAction(new AccessibilityAction(16, getContext().getString(R.string.accessibility_unlock_without_fingerprint)));
        }
    }

    public void setAccessibilityController(AccessibilityController accessibilityController) {
        this.mAccessibilityController = accessibilityController;
    }

    private int getIconForState(int state, boolean screenOn, boolean deviceInteractive) {
        switch (state) {
            case 0:
                return R.drawable.ic_lock_24dp;
            case 1:
                return R.drawable.ic_lock_open_24dp;
            case 2:
                return 17302258;
            case 3:
                int i;
                if (screenOn && deviceInteractive) {
                    i = R.drawable.ic_fingerprint;
                } else {
                    i = R.drawable.lockscreen_fingerprint_draw_on_animation;
                }
                return i;
            case 4:
                return R.drawable.ic_fingerprint_error;
            default:
                throw new IllegalArgumentException();
        }
    }

    private int getAnimationResForTransition(int oldState, int newState, boolean oldDeviceInteractive, boolean deviceInteractive, boolean oldScreenOn, boolean screenOn) {
        if (oldState == 3 && newState == 4) {
            return R.drawable.lockscreen_fingerprint_fp_to_error_state_animation;
        }
        if (oldState == 1 && newState == 4) {
            return R.drawable.trusted_state_to_error_animation;
        }
        if (oldState == 4 && newState == 1) {
            return R.drawable.error_to_trustedstate_animation;
        }
        if (oldState == 4 && newState == 3) {
            return R.drawable.lockscreen_fingerprint_error_state_to_fp_animation;
        }
        if (oldState == 3 && newState == 1 && !this.mUnlockMethodCache.isTrusted()) {
            return R.drawable.lockscreen_fingerprint_draw_off_animation;
        }
        if (newState == 3 && ((!oldScreenOn && screenOn && deviceInteractive) || (screenOn && !oldDeviceInteractive && deviceInteractive))) {
            return R.drawable.lockscreen_fingerprint_draw_on_animation;
        }
        return -1;
    }

    private int getState() {
        KeyguardUpdateMonitor updateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        boolean fingerprintRunning = updateMonitor.isFingerprintDetectionRunning();
        boolean unlockingAllowed = updateMonitor.isUnlockingWithFingerprintAllowed();
        if (this.mTransientFpError) {
            return 4;
        }
        if (this.mUnlockMethodCache.canSkipBouncer()) {
            return 1;
        }
        if (fingerprintRunning && unlockingAllowed) {
            return 3;
        }
        if (this.mUnlockMethodCache.isFaceUnlockRunning()) {
            return 2;
        }
        return 0;
    }
}
