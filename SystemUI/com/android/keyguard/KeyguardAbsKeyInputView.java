package com.android.keyguard;

import android.content.Context;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternChecker.OnCheckCallback;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.EmergencyButton.EmergencyButtonCallback;

public abstract class KeyguardAbsKeyInputView extends LinearLayout implements KeyguardSecurityView, EmergencyButtonCallback {
    protected KeyguardSecurityCallback mCallback;
    protected View mEcaView;
    protected boolean mEnableHaptics;
    protected LockPatternUtils mLockPatternUtils;
    protected AsyncTask<?, ?, ?> mPendingLockCheck;
    protected SecurityMessageDisplay mSecurityMessageDisplay;

    protected abstract String getPasswordText();

    protected abstract int getPasswordTextViewId();

    protected abstract int getPromtReasonStringRes(int i);

    protected abstract void resetPasswordText(boolean z);

    protected abstract void resetState();

    protected abstract void setPasswordEntryEnabled(boolean z);

    protected abstract void setPasswordEntryInputEnabled(boolean z);

    public KeyguardAbsKeyInputView(Context context) {
        this(context, null);
    }

    public KeyguardAbsKeyInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setKeyguardCallback(KeyguardSecurityCallback callback) {
        this.mCallback = callback;
    }

    public void setLockPatternUtils(LockPatternUtils utils) {
        this.mLockPatternUtils = utils;
        this.mEnableHaptics = this.mLockPatternUtils.isTactileFeedbackEnabled();
    }

    public void reset() {
        resetPasswordText(false);
        long deadline = this.mLockPatternUtils.getLockoutAttemptDeadline(KeyguardUpdateMonitor.getCurrentUser());
        if (shouldLockout(deadline)) {
            handleAttemptLockout(deadline);
        } else {
            resetState();
        }
    }

    protected boolean shouldLockout(long deadline) {
        return deadline != 0;
    }

    protected void onFinishInflate() {
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mSecurityMessageDisplay = KeyguardMessageArea.findSecurityMessageDisplay(this);
        this.mEcaView = findViewById(R$id.keyguard_selector_fade_container);
        EmergencyButton button = (EmergencyButton) findViewById(R$id.emergency_call_button);
        if (button != null) {
            button.setCallback(this);
        }
    }

    public void onEmergencyButtonClickedWhenInCall() {
        this.mCallback.reset();
    }

    protected int getWrongPasswordStringId() {
        return R$string.kg_wrong_password;
    }

    protected void verifyPasswordAndUnlock() {
        String entry = getPasswordText();
        setPasswordEntryInputEnabled(false);
        if (this.mPendingLockCheck != null) {
            this.mPendingLockCheck.cancel(false);
        }
        if (entry.length() <= 3) {
            setPasswordEntryInputEnabled(true);
            onPasswordChecked(false, 0, false);
            return;
        }
        this.mPendingLockCheck = LockPatternChecker.checkPassword(this.mLockPatternUtils, entry, KeyguardUpdateMonitor.getCurrentUser(), new OnCheckCallback() {
            public void onChecked(boolean matched, int timeoutMs) {
                KeyguardAbsKeyInputView.this.setPasswordEntryInputEnabled(true);
                KeyguardAbsKeyInputView.this.mPendingLockCheck = null;
                KeyguardAbsKeyInputView.this.onPasswordChecked(matched, timeoutMs, true);
            }
        });
    }

    private void onPasswordChecked(boolean matched, int timeoutMs, boolean isValidPassword) {
        if (matched) {
            this.mCallback.reportUnlockAttempt(true, 0);
            this.mCallback.dismiss(true);
        } else {
            if (isValidPassword) {
                this.mCallback.reportUnlockAttempt(false, timeoutMs);
                if (timeoutMs > 0) {
                    handleAttemptLockout(this.mLockPatternUtils.setLockoutAttemptDeadline(KeyguardUpdateMonitor.getCurrentUser(), timeoutMs));
                }
            }
            if (timeoutMs == 0) {
                this.mSecurityMessageDisplay.setMessage(getWrongPasswordStringId(), true);
            }
        }
        resetPasswordText(true);
    }

    protected void handleAttemptLockout(long elapsedRealtimeDeadline) {
        setPasswordEntryEnabled(false);
        new CountDownTimer(elapsedRealtimeDeadline - SystemClock.elapsedRealtime(), 1000) {
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                KeyguardAbsKeyInputView.this.mSecurityMessageDisplay.setMessage(R$string.kg_too_many_failed_attempts_countdown, true, Integer.valueOf(secondsRemaining));
            }

            public void onFinish() {
                KeyguardAbsKeyInputView.this.mSecurityMessageDisplay.setMessage((CharSequence) "", false);
                KeyguardAbsKeyInputView.this.resetState();
            }
        }.start();
    }

    protected void onUserInput() {
        if (this.mCallback != null) {
            this.mCallback.userActivity();
        }
        this.mSecurityMessageDisplay.setMessage((CharSequence) "", false);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        onUserInput();
        return false;
    }

    public boolean needsInput() {
        return false;
    }

    public void onPause() {
        if (this.mPendingLockCheck != null) {
            this.mPendingLockCheck.cancel(false);
            this.mPendingLockCheck = null;
        }
    }

    public void onResume(int reason) {
        reset();
    }

    public void showPromptReason(int reason) {
        if (reason != 0) {
            int promtReasonStringRes = getPromtReasonStringRes(reason);
            if (promtReasonStringRes != 0) {
                this.mSecurityMessageDisplay.setMessage(promtReasonStringRes, true);
            }
        }
    }

    public void showMessage(String message, int color) {
        this.mSecurityMessageDisplay.setNextMessageColor(color);
        this.mSecurityMessageDisplay.setMessage((CharSequence) message, true);
    }

    public void doHapticKeyClick() {
        if (this.mEnableHaptics) {
            performHapticFeedback(1, 3);
        }
    }

    public boolean startDisappearAnimation(Runnable finishRunnable) {
        return false;
    }
}
