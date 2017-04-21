package com.mediatek.keyguard.VoiceUnlock;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.AudioSystem;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardMessageArea;
import com.android.keyguard.KeyguardSecurityCallback;
import com.android.keyguard.KeyguardSecurityView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.R$id;
import com.android.keyguard.SecurityMessageDisplay;

public class KeyguardVoiceUnlockView extends LinearLayout implements KeyguardSecurityView {
    private BiometricSensorUnlock mBiometricUnlock;
    private Drawable mBouncerFrame;
    private View mEcaView;
    private boolean mIsBouncerVisibleToUser;
    private final Object mIsBouncerVisibleToUserLock;
    private KeyguardSecurityCallback mKeyguardSecurityCallback;
    private LockPatternUtils mLockPatternUtils;
    private SecurityMessageDisplay mSecurityMessageDisplay;
    KeyguardUpdateMonitorCallback mUpdateCallback;
    private View mVoiceUnlockAreaView;

    public KeyguardVoiceUnlockView(Context context) {
        this(context, null);
    }

    public KeyguardVoiceUnlockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mIsBouncerVisibleToUser = false;
        this.mIsBouncerVisibleToUserLock = new Object();
        this.mUpdateCallback = new KeyguardUpdateMonitorCallback() {
            public void onPhoneStateChanged(int phoneState) {
                KeyguardVoiceUnlockView.this.log("onPhoneStateChanged(" + phoneState + ")");
                if (phoneState == 1 && KeyguardVoiceUnlockView.this.mBiometricUnlock != null) {
                    KeyguardVoiceUnlockView.this.mBiometricUnlock.stopAndShowBackup();
                }
            }

            public void onUserSwitching(int userId) {
                KeyguardVoiceUnlockView.this.log("onUserSwitching(" + userId + ")");
                if (KeyguardVoiceUnlockView.this.mBiometricUnlock != null) {
                    KeyguardVoiceUnlockView.this.mBiometricUnlock.stop();
                }
            }

            public void onUserSwitchComplete(int userId) {
                Log.d("KeyguardVoiceUnlockView", "onUserSwitchComplete(" + userId + ")");
                if (KeyguardVoiceUnlockView.this.mBiometricUnlock != null) {
                    KeyguardVoiceUnlockView.this.maybeStartBiometricUnlock();
                }
            }

            public void onKeyguardVisibilityChanged(boolean showing) {
                Log.d("KeyguardVoiceUnlockView", "onKeyguardVisibilityChanged(" + showing + ")");
                KeyguardVoiceUnlockView.this.handleBouncerUserVisibilityChanged();
            }

            public void onKeyguardBouncerChanged(boolean bouncer) {
                Log.d("KeyguardVoiceUnlockView", "onKeyguardBouncerChanged(" + bouncer + ")");
                KeyguardVoiceUnlockView.this.handleBouncerUserVisibilityChanged();
            }

            public void onStartedWakingUp() {
                Log.d("KeyguardVoiceUnlockView", "onScreenTurnedOn()");
                KeyguardVoiceUnlockView.this.handleBouncerUserVisibilityChanged();
            }

            public void onFinishedGoingToSleep(int why) {
                Log.d("KeyguardVoiceUnlockView", "onScreenTurnedOff()");
                KeyguardVoiceUnlockView.this.handleBouncerUserVisibilityChanged();
            }
        };
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        initializeBiometricUnlockView();
        KeyguardMessageArea keyguardMessageArea = new KeyguardMessageArea(this.mContext);
        this.mSecurityMessageDisplay = KeyguardMessageArea.findSecurityMessageDisplay(this);
        this.mEcaView = findViewById(R$id.keyguard_selector_fade_container);
        View bouncerFrameView = findViewById(R$id.keyguard_bouncer_frame);
        if (bouncerFrameView != null) {
            this.mBouncerFrame = bouncerFrameView.getBackground();
        }
    }

    public void setKeyguardCallback(KeyguardSecurityCallback callback) {
        this.mKeyguardSecurityCallback = callback;
        ((VoiceUnlock) this.mBiometricUnlock).setKeyguardCallback(callback);
    }

    public void setLockPatternUtils(LockPatternUtils utils) {
        this.mLockPatternUtils = utils;
    }

    public void onDetachedFromWindow() {
        log("onDetachedFromWindow()");
        if (this.mBiometricUnlock != null) {
            this.mBiometricUnlock.stop();
        }
        KeyguardUpdateMonitor.getInstance(this.mContext).removeCallback(this.mUpdateCallback);
    }

    public void onPause() {
        log("onPause()");
        if (this.mBiometricUnlock != null) {
            this.mBiometricUnlock.stop();
        }
        this.mSecurityMessageDisplay.setMessage((CharSequence) " ", true);
        KeyguardUpdateMonitor.getInstance(this.mContext).removeCallback(this.mUpdateCallback);
    }

    public void onResume(int reason) {
        log("onResume()");
        synchronized (this.mIsBouncerVisibleToUserLock) {
            this.mIsBouncerVisibleToUser = isBouncerVisibleToUser();
        }
        if (!KeyguardUpdateMonitor.getInstance(this.mContext).isSwitchingUser()) {
            maybeStartBiometricUnlock();
        }
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mUpdateCallback);
    }

    public boolean needsInput() {
        return false;
    }

    private void initializeBiometricUnlockView() {
        log("initializeBiometricUnlockView()");
        this.mVoiceUnlockAreaView = findViewById(R$id.voice_unlock_view);
        if (this.mVoiceUnlockAreaView != null) {
            this.mBiometricUnlock = new VoiceUnlock(this.mContext, this);
            this.mBiometricUnlock.initializeView(this.mVoiceUnlockAreaView);
            return;
        }
        log("Couldn't find biometric unlock view");
    }

    private void maybeStartBiometricUnlock() {
        log("maybeStartBiometricUnlock() is called.");
        if (this.mBiometricUnlock != null) {
            boolean mediaPlaying;
            boolean isBouncerVisibleToUser;
            KeyguardUpdateMonitor monitor = KeyguardUpdateMonitor.getInstance(this.mContext);
            boolean backupIsTimedOut = monitor.getFailedUnlockAttempts() >= 4;
            if (AudioSystem.isStreamActive(3, 0)) {
                mediaPlaying = true;
            } else {
                mediaPlaying = AudioSystem.isStreamActive(4, 0);
            }
            synchronized (this.mIsBouncerVisibleToUserLock) {
                isBouncerVisibleToUser = this.mIsBouncerVisibleToUser;
            }
            if (!isBouncerVisibleToUser) {
                if (mediaPlaying) {
                    log("maybeStartBiometricUnlock() - isBouncerVisibleToUser is false && mediaPlaying is true, call mBiometricUnlock.stopAndShowBackup()");
                    this.mBiometricUnlock.stopAndShowBackup();
                } else {
                    log("maybeStartBiometricUnlock() - isBouncerVisibleToUser is false, call mBiometricUnlock.stop()");
                    this.mBiometricUnlock.stop();
                }
            } else if (monitor.getPhoneState() != 0 || !monitor.isAlternateUnlockEnabled() || monitor.getMaxBiometricUnlockAttemptsReached() || backupIsTimedOut || mediaPlaying) {
                log("maybeStartBiometricUnlock() - call stopAndShowBackup()");
                this.mBiometricUnlock.stopAndShowBackup();
            } else {
                this.mBiometricUnlock.start();
            }
        }
    }

    private boolean isBouncerVisibleToUser() {
        KeyguardUpdateMonitor updateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        if (updateMonitor.isKeyguardBouncer() && updateMonitor.isKeyguardVisible()) {
            return updateMonitor.isScreenOn();
        }
        return false;
    }

    private void handleBouncerUserVisibilityChanged() {
        boolean wasBouncerVisibleToUser;
        synchronized (this.mIsBouncerVisibleToUserLock) {
            wasBouncerVisibleToUser = this.mIsBouncerVisibleToUser;
            this.mIsBouncerVisibleToUser = isBouncerVisibleToUser();
        }
        log("wasBouncerVisibleToUser = " + wasBouncerVisibleToUser + " , mIsBouncerVisibleToUser = " + this.mIsBouncerVisibleToUser);
        if (this.mBiometricUnlock == null) {
            return;
        }
        if (wasBouncerVisibleToUser && !this.mIsBouncerVisibleToUser) {
            log("handleBouncerUserVisibilityChanged() - wasBouncerVisibleToUser && !mIsBouncerVisibleToUser");
            this.mBiometricUnlock.stop();
        } else if (!wasBouncerVisibleToUser && this.mIsBouncerVisibleToUser) {
            log("handleBouncerUserVisibilityChanged() - !wasBouncerVisibleToUser && mIsBouncerVisibleToUser");
            maybeStartBiometricUnlock();
        }
    }

    public void showPromptReason(int reason) {
    }

    public void showMessage(String message, int color) {
    }

    public void startAppearAnimation() {
    }

    public boolean startDisappearAnimation(Runnable finishRunnable) {
        return false;
    }

    private void log(String msg) {
        Log.d("KeyguardVoiceUnlockView", "KeyguardVoiceUnlockView: " + msg);
    }
}
