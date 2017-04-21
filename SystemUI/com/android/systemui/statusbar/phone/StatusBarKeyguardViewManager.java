package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.WindowManagerGlobal;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardHostView.OnDismissAction;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.ViewMediatorCallback;
import com.mediatek.keyguard.VoiceWakeup.VoiceWakeupManager;

public class StatusBarKeyguardViewManager {
    private static String TAG = "StatusBarKeyguardViewManager";
    private final boolean DEBUG = true;
    private OnDismissAction mAfterKeyguardGoneAction;
    private KeyguardBouncer mBouncer;
    private ViewGroup mContainer;
    private final Context mContext;
    private boolean mDeferScrimFadeOut;
    private boolean mDeviceInteractive = false;
    private boolean mDeviceWillWakeUp;
    private FingerprintUnlockController mFingerprintUnlockController;
    private boolean mFirstUpdate = true;
    private boolean mLastBouncerDismissible;
    private boolean mLastBouncerShowing;
    private boolean mLastDeferScrimFadeOut;
    private boolean mLastOccluded;
    private boolean mLastShowing;
    private LockPatternUtils mLockPatternUtils;
    private Runnable mMakeNavigationBarVisibleRunnable = new Runnable() {
        public void run() {
            Log.d(StatusBarKeyguardViewManager.TAG, "mMakeNavigationBarVisibleRunnable - set nav bar VISIBLE.");
            StatusBarKeyguardViewManager.this.mPhoneStatusBar.getNavigationBarView().setVisibility(0);
        }
    };
    private boolean mOccluded;
    private PhoneStatusBar mPhoneStatusBar;
    private boolean mScreenTurnedOn;
    private ScrimController mScrimController;
    private boolean mShowing;
    private StatusBarWindowManager mStatusBarWindowManager;
    private ViewMediatorCallback mViewMediatorCallback;

    public StatusBarKeyguardViewManager(Context context, ViewMediatorCallback callback, LockPatternUtils lockPatternUtils) {
        this.mContext = context;
        this.mViewMediatorCallback = callback;
        this.mLockPatternUtils = lockPatternUtils;
    }

    public void registerStatusBar(PhoneStatusBar phoneStatusBar, ViewGroup container, StatusBarWindowManager statusBarWindowManager, ScrimController scrimController, FingerprintUnlockController fingerprintUnlockController) {
        this.mPhoneStatusBar = phoneStatusBar;
        this.mContainer = container;
        this.mStatusBarWindowManager = statusBarWindowManager;
        this.mScrimController = scrimController;
        this.mFingerprintUnlockController = fingerprintUnlockController;
        this.mBouncer = new KeyguardBouncer(this.mContext, this.mViewMediatorCallback, this.mLockPatternUtils, this.mStatusBarWindowManager, container);
    }

    public void show(Bundle options) {
        Log.d(TAG, "show() is called.");
        this.mShowing = true;
        this.mStatusBarWindowManager.setKeyguardShowing(true);
        this.mScrimController.abortKeyguardFadingOut();
        reset();
    }

    private void showBouncerOrKeyguard() {
        Log.d(TAG, "showBouncerOrKeyguard() is called.");
        if (this.mBouncer.needsFullscreenBouncer()) {
            Log.d(TAG, "needsFullscreenBouncer() is true, show \"Bouncer\" view directly.");
            this.mPhoneStatusBar.hideKeyguard();
            this.mBouncer.show(true);
            return;
        }
        Log.d(TAG, "needsFullscreenBouncer() is false,show \"Notification Keyguard\" view.");
        this.mPhoneStatusBar.showKeyguard();
        this.mBouncer.hide(false);
        this.mBouncer.prepare();
    }

    private void showBouncer() {
        showBouncer(false);
    }

    private void showBouncer(boolean authenticated) {
        if (this.mShowing) {
            this.mBouncer.show(false, authenticated);
        }
        updateStates();
    }

    public void dismissWithAction(OnDismissAction r, Runnable cancelAction, boolean afterKeyguardGone) {
        if (this.mShowing) {
            if (afterKeyguardGone) {
                Log.d(TAG, "dismissWithAction() - afterKeyguardGone = true, call bouncer.show()");
                this.mBouncer.show(false);
                this.mAfterKeyguardGoneAction = r;
            } else {
                Log.d(TAG, "dismissWithAction() - afterKeyguardGone = false,call showWithDismissAction");
                this.mBouncer.showWithDismissAction(r);
            }
        }
        updateStates();
    }

    public void reset() {
        Log.d(TAG, "reset() is called, mShowing = " + this.mShowing + " ,mOccluded = " + this.mOccluded);
        if (this.mShowing) {
            if (this.mOccluded) {
                this.mPhoneStatusBar.hideKeyguard();
                this.mPhoneStatusBar.stopWaitingForKeyguardExit();
                this.mBouncer.hide(false);
            } else {
                showBouncerOrKeyguard();
            }
            KeyguardUpdateMonitor.getInstance(this.mContext).sendKeyguardReset();
            updateStates();
        }
    }

    public void onFinishedGoingToSleep() {
        this.mDeviceInteractive = false;
        this.mPhoneStatusBar.onFinishedGoingToSleep();
        this.mBouncer.onScreenTurnedOff();
    }

    public void onStartedWakingUp() {
        this.mDeviceInteractive = true;
        this.mDeviceWillWakeUp = false;
        this.mPhoneStatusBar.onStartedWakingUp();
    }

    public void onScreenTurningOn() {
        this.mPhoneStatusBar.onScreenTurningOn();
    }

    public void onScreenTurnedOn() {
        this.mScreenTurnedOn = true;
        if (this.mDeferScrimFadeOut) {
            this.mDeferScrimFadeOut = false;
            animateScrimControllerKeyguardFadingOut(0, 200);
            updateStates();
        }
        this.mPhoneStatusBar.onScreenTurnedOn();
    }

    public void onScreenTurnedOff() {
        this.mScreenTurnedOn = false;
    }

    public void notifyDeviceWakeUpRequested() {
        this.mDeviceWillWakeUp = !this.mDeviceInteractive;
    }

    public void verifyUnlock() {
        show(null);
        dismiss();
    }

    public void setNeedsInput(boolean needsInput) {
        Log.d(TAG, "setNeedsInput() - needsInput = " + needsInput);
        this.mStatusBarWindowManager.setKeyguardNeedsInput(needsInput);
    }

    public void setOccluded(boolean occluded) {
        if (occluded && !this.mOccluded && this.mShowing && this.mPhoneStatusBar.isInLaunchTransition()) {
            this.mOccluded = true;
            this.mPhoneStatusBar.fadeKeyguardAfterLaunchTransition(null, new Runnable() {
                public void run() {
                    if (StatusBarKeyguardViewManager.this.mOccluded) {
                        Log.d(StatusBarKeyguardViewManager.TAG, "setOccluded.run() - setKeyguardOccluded(true)");
                        StatusBarKeyguardViewManager.this.mStatusBarWindowManager.setKeyguardOccluded(true);
                        StatusBarKeyguardViewManager.this.reset();
                        return;
                    }
                    Log.d(StatusBarKeyguardViewManager.TAG, "setOccluded.run() - mOccluded was set to false");
                }
            });
            return;
        }
        this.mOccluded = occluded;
        Log.d(TAG, "setOccluded() - setKeyguardOccluded(" + occluded + ")");
        this.mStatusBarWindowManager.setKeyguardOccluded(occluded);
        reset();
    }

    public boolean isOccluded() {
        return this.mOccluded;
    }

    public void startPreHideAnimation(Runnable finishRunnable) {
        if (this.mBouncer.isShowing()) {
            this.mBouncer.startPreHideAnimation(finishRunnable);
        } else if (finishRunnable != null) {
            finishRunnable.run();
        }
    }

    public void hide(long startTime, long fadeoutDuration) {
        Log.d(TAG, "hide() is called.");
        this.mShowing = false;
        long delay = Math.max(0, (-48 + startTime) - SystemClock.uptimeMillis());
        if (this.mPhoneStatusBar.isInLaunchTransition()) {
            this.mPhoneStatusBar.fadeKeyguardAfterLaunchTransition(new Runnable() {
                public void run() {
                    StatusBarKeyguardViewManager.this.mStatusBarWindowManager.setKeyguardShowing(false);
                    StatusBarKeyguardViewManager.this.mStatusBarWindowManager.setKeyguardFadingAway(true);
                    StatusBarKeyguardViewManager.this.mBouncer.hide(true);
                    StatusBarKeyguardViewManager.this.updateStates();
                    StatusBarKeyguardViewManager.this.mScrimController.animateKeyguardFadingOut(100, 300, null);
                }
            }, new Runnable() {
                public void run() {
                    StatusBarKeyguardViewManager.this.mPhoneStatusBar.hideKeyguard();
                    StatusBarKeyguardViewManager.this.mStatusBarWindowManager.setKeyguardFadingAway(false);
                    StatusBarKeyguardViewManager.this.mViewMediatorCallback.keyguardGone();
                    StatusBarKeyguardViewManager.this.executeAfterKeyguardGoneAction();
                }
            });
            return;
        }
        if (this.mFingerprintUnlockController.getMode() == 2) {
            this.mFingerprintUnlockController.startKeyguardFadingAway();
            this.mPhoneStatusBar.setKeyguardFadingAway(startTime, 0, 250);
            this.mPhoneStatusBar.fadeKeyguardWhilePulsing();
            animateScrimControllerKeyguardFadingOut(0, 250);
        } else {
            this.mFingerprintUnlockController.startKeyguardFadingAway();
            this.mPhoneStatusBar.setKeyguardFadingAway(startTime, delay, fadeoutDuration);
            if (this.mPhoneStatusBar.hideKeyguard()) {
                this.mScrimController.animateGoingToFullShade(delay, fadeoutDuration);
                this.mPhoneStatusBar.finishKeyguardFadingAway();
            } else {
                this.mStatusBarWindowManager.setKeyguardFadingAway(true);
                if (this.mFingerprintUnlockController.getMode() != 1) {
                    animateScrimControllerKeyguardFadingOut(delay, fadeoutDuration);
                } else if (this.mScreenTurnedOn) {
                    animateScrimControllerKeyguardFadingOut(0, 200);
                } else {
                    this.mDeferScrimFadeOut = true;
                }
            }
        }
        this.mStatusBarWindowManager.setKeyguardShowing(false);
        this.mBouncer.hide(true);
        this.mViewMediatorCallback.keyguardGone();
        executeAfterKeyguardGoneAction();
        updateStates();
    }

    private void animateScrimControllerKeyguardFadingOut(long delay, long duration) {
        Trace.asyncTraceBegin(8, "Fading out", 0);
        this.mScrimController.animateKeyguardFadingOut(delay, duration, new Runnable() {
            public void run() {
                StatusBarKeyguardViewManager.this.mStatusBarWindowManager.setKeyguardFadingAway(false);
                StatusBarKeyguardViewManager.this.mPhoneStatusBar.finishKeyguardFadingAway();
                StatusBarKeyguardViewManager.this.mFingerprintUnlockController.finishKeyguardFadingAway();
                WindowManagerGlobal.getInstance().trimMemory(20);
                Trace.asyncTraceEnd(8, "Fading out", 0);
            }
        });
    }

    private void executeAfterKeyguardGoneAction() {
        if (this.mAfterKeyguardGoneAction != null) {
            Log.d(TAG, "executeAfterKeyguardGoneAction() is called");
            this.mAfterKeyguardGoneAction.onDismiss();
            this.mAfterKeyguardGoneAction = null;
        }
    }

    public void dismiss() {
        dismiss(false);
    }

    public void dismiss(boolean authenticated) {
        Log.d(TAG, "dismiss(authenticated = " + authenticated + ") is called." + " mScreenOn = " + this.mDeviceInteractive);
        if (this.mDeviceInteractive || VoiceWakeupManager.getInstance().isDismissAndLaunchApp()) {
            showBouncer(authenticated);
        }
        if (this.mDeviceInteractive || this.mDeviceWillWakeUp) {
            showBouncer();
        }
    }

    public boolean isSecure() {
        return this.mBouncer.isSecure();
    }

    public boolean isShowing() {
        return this.mShowing;
    }

    public boolean onBackPressed() {
        Log.d(TAG, "onBackPressed()");
        if (this.mBouncer.isShowing()) {
            Log.d(TAG, "onBackPressed() - reset & return true");
            reset();
            this.mAfterKeyguardGoneAction = null;
            return true;
        }
        Log.d(TAG, "onBackPressed() - reset & return false");
        return false;
    }

    public boolean isBouncerShowing() {
        return this.mBouncer.isShowing();
    }

    private long getNavBarShowDelay() {
        if (this.mPhoneStatusBar.isKeyguardFadingAway()) {
            return this.mPhoneStatusBar.getKeyguardFadingAwayDelay();
        }
        return 320;
    }

    public void updateStates() {
        int vis = this.mContainer.getSystemUiVisibility();
        boolean showing = this.mShowing;
        boolean occluded = this.mOccluded;
        boolean bouncerShowing = this.mBouncer.isShowing();
        boolean bouncerDismissible = !this.mBouncer.isFullscreenBouncer();
        boolean deferScrimFadeOut = this.mDeferScrimFadeOut;
        Object obj = (bouncerDismissible || !showing) ? 1 : null;
        Object obj2 = (this.mLastBouncerDismissible || !this.mLastShowing) ? 1 : null;
        if (obj != obj2 || this.mFirstUpdate) {
            if (bouncerDismissible || !showing) {
                this.mContainer.setSystemUiVisibility(-4194305 & vis);
            } else {
                this.mContainer.setSystemUiVisibility(4194304 | vis);
            }
        }
        boolean navBarVisible = !deferScrimFadeOut ? (!showing || occluded) ? true : bouncerShowing : false;
        boolean lastNavBarVisible = !this.mLastDeferScrimFadeOut ? (!this.mLastShowing || this.mLastOccluded) ? true : this.mLastBouncerShowing : false;
        if (navBarVisible != lastNavBarVisible || this.mFirstUpdate) {
            Log.d(TAG, "updateStates() - showing = " + showing + ", mLastShowing = " + this.mLastShowing + "\nupdateStates() - occluded = " + occluded + "mLastOccluded = " + this.mLastOccluded + "\nupdateStates() - bouncerShowing = " + bouncerShowing + ", mLastBouncerShowing = " + this.mLastBouncerShowing + "\nupdateStates() - mFirstUpdate = " + this.mFirstUpdate);
            if (this.mPhoneStatusBar.getNavigationBarView() != null) {
                if (navBarVisible) {
                    long delay = getNavBarShowDelay();
                    if (delay == 0) {
                        this.mMakeNavigationBarVisibleRunnable.run();
                    } else {
                        this.mContainer.postOnAnimationDelayed(this.mMakeNavigationBarVisibleRunnable, delay);
                    }
                } else {
                    Log.d(TAG, "updateStates() - set nav bar GONE for showing notification keyguard.");
                    this.mContainer.removeCallbacks(this.mMakeNavigationBarVisibleRunnable);
                    this.mPhoneStatusBar.getNavigationBarView().setVisibility(8);
                }
            }
        }
        if (bouncerShowing != this.mLastBouncerShowing || this.mFirstUpdate) {
            Log.d(TAG, "updateStates() - setBouncerShowing(" + bouncerShowing + ")");
            this.mStatusBarWindowManager.setBouncerShowing(bouncerShowing);
            this.mPhoneStatusBar.setBouncerShowing(bouncerShowing);
            this.mScrimController.setBouncerShowing(bouncerShowing);
        }
        KeyguardUpdateMonitor updateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        obj = (!showing || occluded) ? null : 1;
        obj2 = (!this.mLastShowing || this.mLastOccluded) ? null : 1;
        if (obj != obj2 || this.mFirstUpdate) {
            boolean z = showing && !occluded;
            updateMonitor.onKeyguardVisibilityChanged(z);
        }
        if (bouncerShowing != this.mLastBouncerShowing || this.mFirstUpdate) {
            updateMonitor.sendKeyguardBouncerChanged(bouncerShowing);
        }
        this.mFirstUpdate = false;
        this.mLastShowing = showing;
        this.mLastOccluded = occluded;
        this.mLastDeferScrimFadeOut = deferScrimFadeOut;
        this.mLastBouncerShowing = bouncerShowing;
        this.mLastBouncerDismissible = bouncerDismissible;
        this.mPhoneStatusBar.onKeyguardViewManagerStatesUpdated();
    }

    public boolean onMenuPressed() {
        return this.mBouncer.onMenuPressed();
    }

    public boolean interceptMediaKey(KeyEvent event) {
        return this.mBouncer.interceptMediaKey(event);
    }

    public void onActivityDrawn() {
        if (this.mPhoneStatusBar.isCollapsing()) {
            this.mPhoneStatusBar.addPostCollapseAction(new Runnable() {
                public void run() {
                    StatusBarKeyguardViewManager.this.mViewMediatorCallback.readyForKeyguardDone();
                }
            });
        } else {
            this.mViewMediatorCallback.readyForKeyguardDone();
        }
    }

    public boolean shouldDisableWindowAnimationsForUnlock() {
        return this.mPhoneStatusBar.isInLaunchTransition();
    }

    public boolean isGoingToNotificationShade() {
        return this.mPhoneStatusBar.isGoingToNotificationShade();
    }

    public boolean isSecure(int userId) {
        return !this.mBouncer.isSecure() ? this.mLockPatternUtils.isSecure(userId) : true;
    }

    public boolean isInputRestricted() {
        return this.mViewMediatorCallback.isInputRestricted();
    }

    public void keyguardGoingAway() {
        this.mPhoneStatusBar.keyguardGoingAway();
    }

    public void animateCollapsePanels(float speedUpFactor) {
        this.mPhoneStatusBar.animateCollapsePanels(0, true, false, speedUpFactor);
    }

    public void notifyKeyguardAuthenticated(boolean strongAuth) {
        this.mBouncer.notifyKeyguardAuthenticated(strongAuth);
    }

    public void showBouncerMessage(String message, int color) {
        this.mBouncer.showMessage(message, color);
    }
}
