package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.util.Log;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.keyguard.KeyguardViewMediator;

public class FingerprintUnlockController extends KeyguardUpdateMonitorCallback {
    private DozeScrimController mDozeScrimController;
    private Handler mHandler = new Handler();
    private KeyguardViewMediator mKeyguardViewMediator;
    private int mMode;
    private int mPendingAuthenticatedUserId = -1;
    private PhoneStatusBar mPhoneStatusBar;
    private PowerManager mPowerManager;
    private final Runnable mReleaseFingerprintWakeLockRunnable = new Runnable() {
        public void run() {
            Log.i("FingerprintController", "fp wakelock: TIMEOUT!!");
            FingerprintUnlockController.this.releaseFingerprintWakeLock();
        }
    };
    private ScrimController mScrimController;
    private StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private StatusBarWindowManager mStatusBarWindowManager;
    private KeyguardUpdateMonitor mUpdateMonitor;
    private WakeLock mWakeLock;

    public FingerprintUnlockController(Context context, StatusBarWindowManager statusBarWindowManager, DozeScrimController dozeScrimController, KeyguardViewMediator keyguardViewMediator, ScrimController scrimController, PhoneStatusBar phoneStatusBar) {
        this.mPowerManager = (PowerManager) context.getSystemService(PowerManager.class);
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
        this.mUpdateMonitor.registerCallback(this);
        this.mStatusBarWindowManager = statusBarWindowManager;
        this.mDozeScrimController = dozeScrimController;
        this.mKeyguardViewMediator = keyguardViewMediator;
        this.mScrimController = scrimController;
        this.mPhoneStatusBar = phoneStatusBar;
    }

    public void setStatusBarKeyguardViewManager(StatusBarKeyguardViewManager statusBarKeyguardViewManager) {
        this.mStatusBarKeyguardViewManager = statusBarKeyguardViewManager;
    }

    private void releaseFingerprintWakeLock() {
        if (this.mWakeLock != null) {
            this.mHandler.removeCallbacks(this.mReleaseFingerprintWakeLockRunnable);
            Log.i("FingerprintController", "releasing fp wakelock");
            this.mWakeLock.release();
            this.mWakeLock = null;
        }
    }

    public void onFingerprintAcquired() {
        releaseFingerprintWakeLock();
        if (!this.mUpdateMonitor.isDeviceInteractive()) {
            this.mWakeLock = this.mPowerManager.newWakeLock(1, "wake-and-unlock wakelock");
            this.mWakeLock.acquire();
            Log.i("FingerprintController", "fingerprint acquired, grabbing fp wakelock");
            this.mHandler.postDelayed(this.mReleaseFingerprintWakeLockRunnable, 15000);
            if (this.mDozeScrimController.isPulsing()) {
                this.mStatusBarWindowManager.setForceDozeBrightness(true);
            }
        }
    }

    public void onFingerprintAuthenticated(int userId) {
        if (this.mUpdateMonitor.isGoingToSleep()) {
            this.mPendingAuthenticatedUserId = userId;
            return;
        }
        boolean wasDeviceInteractive = this.mUpdateMonitor.isDeviceInteractive();
        this.mMode = calculateMode();
        if (!wasDeviceInteractive) {
            Log.i("FingerprintController", "fp wakelock: Authenticated, waking up...");
            this.mPowerManager.wakeUp(SystemClock.uptimeMillis());
        }
        releaseFingerprintWakeLock();
        switch (this.mMode) {
            case 1:
                break;
            case 2:
                this.mPhoneStatusBar.updateMediaMetaData(false);
                break;
            case 3:
            case 5:
                if (!wasDeviceInteractive) {
                    this.mStatusBarKeyguardViewManager.notifyDeviceWakeUpRequested();
                }
                this.mStatusBarKeyguardViewManager.animateCollapsePanels(1.3f);
                break;
            case 6:
                this.mStatusBarKeyguardViewManager.notifyKeyguardAuthenticated(false);
                break;
        }
        this.mStatusBarWindowManager.setStatusBarFocusable(false);
        this.mDozeScrimController.abortPulsing();
        this.mKeyguardViewMediator.onWakeAndUnlocking();
        this.mScrimController.setWakeAndUnlocking();
        if (this.mPhoneStatusBar.getNavigationBarView() != null) {
            this.mPhoneStatusBar.getNavigationBarView().setWakeAndUnlocking(true);
        }
        if (this.mMode != 2) {
            this.mStatusBarWindowManager.setForceDozeBrightness(false);
        }
        this.mPhoneStatusBar.notifyFpAuthModeChanged();
    }

    public void onStartedGoingToSleep(int why) {
        this.mPendingAuthenticatedUserId = -1;
    }

    public void onFinishedGoingToSleep(int why) {
        if (this.mPendingAuthenticatedUserId != -1) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    FingerprintUnlockController.this.onFingerprintAuthenticated(FingerprintUnlockController.this.mPendingAuthenticatedUserId);
                }
            });
        }
        this.mPendingAuthenticatedUserId = -1;
    }

    public int getMode() {
        return this.mMode;
    }

    private int calculateMode() {
        boolean unlockingAllowed = this.mUpdateMonitor.isUnlockingWithFingerprintAllowed();
        if (this.mUpdateMonitor.isDeviceInteractive()) {
            if (this.mStatusBarKeyguardViewManager.isShowing()) {
                if (this.mStatusBarKeyguardViewManager.isBouncerShowing() && unlockingAllowed) {
                    return 6;
                }
                if (unlockingAllowed) {
                    return 5;
                }
                if (!this.mStatusBarKeyguardViewManager.isBouncerShowing()) {
                    return 3;
                }
            }
            return 0;
        } else if (!this.mStatusBarKeyguardViewManager.isShowing()) {
            return 4;
        } else {
            if (this.mDozeScrimController.isPulsing() && unlockingAllowed) {
                return 2;
            }
            if (unlockingAllowed) {
                return 1;
            }
            return 3;
        }
    }

    public void onFingerprintAuthFailed() {
        cleanup();
    }

    public void onFingerprintError(int msgId, String errString) {
        cleanup();
    }

    private void cleanup() {
        this.mMode = 0;
        releaseFingerprintWakeLock();
        this.mStatusBarWindowManager.setForceDozeBrightness(false);
        this.mPhoneStatusBar.notifyFpAuthModeChanged();
    }

    public void startKeyguardFadingAway() {
        this.mHandler.postDelayed(new Runnable() {
            public void run() {
                FingerprintUnlockController.this.mStatusBarWindowManager.setForceDozeBrightness(false);
            }
        }, 120);
    }

    public void finishKeyguardFadingAway() {
        this.mMode = 0;
        if (this.mPhoneStatusBar.getNavigationBarView() != null) {
            this.mPhoneStatusBar.getNavigationBarView().setWakeAndUnlocking(false);
        }
        this.mPhoneStatusBar.notifyFpAuthModeChanged();
    }
}
