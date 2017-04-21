package com.mediatek.keyguard.AntiTheft;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import com.android.keyguard.EmergencyCarrierArea;
import com.android.keyguard.KeyguardMessageArea;
import com.android.keyguard.KeyguardPinBasedInputView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.R$id;
import com.android.keyguard.R$string;
import com.android.keyguard.SecurityMessageDisplay;

public class KeyguardAntiTheftLockView extends KeyguardPinBasedInputView {
    private static int mReportUnlockAttemptTimeout = 30000;
    private AntiTheftManager mAntiTheftManager;
    private ViewGroup mBouncerFrameView;
    private Context mContext;
    private SecurityMessageDisplay mSecurityMessageDisplay;

    public KeyguardAntiTheftLockView(Context context) {
        this(context, null);
    }

    public KeyguardAntiTheftLockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mAntiTheftManager = AntiTheftManager.getInstance(null, null, null);
    }

    protected void resetState() {
        super.resetState();
        updateKeypadVisibility();
    }

    protected int getPasswordTextViewId() {
        return R$id.antiTheftPinEntry;
    }

    protected void verifyPasswordAndUnlock() {
        String entry = getPasswordText();
        boolean isLockOut = false;
        Log.d("KeyguardAntiTheftLockView", "verifyPasswordAndUnlock is called.");
        if (AntiTheftManager.getInstance(null, null, null).checkPassword(entry)) {
            this.mCallback.reportUnlockAttempt(true, mReportUnlockAttemptTimeout);
            this.mCallback.dismiss(true);
            AntiTheftManager.getInstance(null, null, null).adjustStatusBarLocked();
        } else if (entry.length() > 3) {
            Log.d("KeyguardAntiTheftLockView", "verifyPasswordAndUnlock fail");
            this.mCallback.reportUnlockAttempt(false, mReportUnlockAttemptTimeout);
            if (KeyguardUpdateMonitor.getInstance(this.mContext).getFailedUnlockAttempts() % 5 == 0) {
                handleAttemptLockout(this.mLockPatternUtils.setLockoutAttemptDeadline(KeyguardUpdateMonitor.getCurrentUser(), mReportUnlockAttemptTimeout));
                isLockOut = true;
            }
            this.mSecurityMessageDisplay.setMessage(getWrongPasswordStringId(), true);
        }
        setPasswordEntryEnabled(true);
        resetPasswordText(true);
        if (isLockOut) {
            setPasswordEntryEnabled(false);
        }
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        Log.d("KeyguardAntiTheftLockView", "onFinishInflate() is called");
        this.mBouncerFrameView = (ViewGroup) findViewById(R$id.keyguard_bouncer_frame);
        this.mSecurityMessageDisplay = KeyguardMessageArea.findSecurityMessageDisplay(this);
        if (!AntiTheftManager.isKeypadNeeded()) {
            Log.d("KeyguardAntiTheftLockView", "onFinishInflate, not need keypad");
            this.mBouncerFrameView.setVisibility(4);
        }
        AntiTheftManager.getInstance(null, null, null).doBindAntiThftLockServices();
        if (this.mEcaView instanceof EmergencyCarrierArea) {
            ((EmergencyCarrierArea) this.mEcaView).setCarrierTextVisible(true);
        }
    }

    public int getWrongPasswordStringId() {
        return R$string.kg_wrong_pin;
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        AntiTheftManager.getInstance(null, null, null).doBindAntiThftLockServices();
    }

    public void onDetachedFromWindow() {
        Log.d("KeyguardAntiTheftLockView", "onDetachedFromWindow() is called.");
        super.onDetachedFromWindow();
        this.mAntiTheftManager.setSecurityViewCallback(null);
    }

    public void onPause() {
        Log.d("KeyguardAntiTheftLockView", "onPause");
    }

    public void onResume(int reason) {
        super.onResume(reason);
        Log.d("KeyguardAntiTheftLockView", "onResume");
        this.mSecurityMessageDisplay.setMessage((CharSequence) "AntiTheft Noneed Print Text", true);
        AntiTheftManager.getInstance(null, null, null).doBindAntiThftLockServices();
        this.mAntiTheftManager.setSecurityViewCallback(this.mCallback);
        updateKeypadVisibility();
    }

    public void startAppearAnimation() {
    }

    public boolean startDisappearAnimation(Runnable finishRunnable) {
        return false;
    }

    private void updateKeypadVisibility() {
        if (AntiTheftManager.isKeypadNeeded()) {
            this.mBouncerFrameView.setVisibility(0);
        } else {
            this.mBouncerFrameView.setVisibility(4);
        }
    }
}
