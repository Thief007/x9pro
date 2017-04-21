package com.android.keyguard;

import android.content.Context;
import android.graphics.Rect;
import android.media.AudioSystem;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;
import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternChecker.OnCheckCallback;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.LockPatternView.Cell;
import com.android.internal.widget.LockPatternView.CellState;
import com.android.internal.widget.LockPatternView.DisplayMode;
import com.android.internal.widget.LockPatternView.OnPatternListener;
import com.android.keyguard.EmergencyButton.EmergencyButtonCallback;
import com.android.settingslib.animation.AppearAnimationCreator;
import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.settingslib.animation.DisappearAnimationUtils;
import java.util.List;

public class KeyguardPatternView extends LinearLayout implements KeyguardSecurityView, AppearAnimationCreator<CellState>, EmergencyButtonCallback {
    private final AppearAnimationUtils mAppearAnimationUtils;
    private KeyguardSecurityCallback mCallback;
    private Runnable mCancelPatternRunnable;
    private ViewGroup mContainer;
    private CountDownTimer mCountdownTimer;
    private final DisappearAnimationUtils mDisappearAnimationUtils;
    private int mDisappearYTranslation;
    private View mEcaView;
    private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private long mLastPokeTime;
    private LockPatternUtils mLockPatternUtils;
    private LockPatternView mLockPatternView;
    private AsyncTask<?, ?, ?> mPendingLockCheck;
    private KeyguardMessageArea mSecurityMessageDisplay;
    private Rect mTempRect;

    private class UnlockPatternListener implements OnPatternListener {
        private UnlockPatternListener() {
        }

        public void onPatternStart() {
            KeyguardPatternView.this.mLockPatternView.removeCallbacks(KeyguardPatternView.this.mCancelPatternRunnable);
            KeyguardPatternView.this.mSecurityMessageDisplay.setMessage((CharSequence) "", false);
        }

        public void onPatternCleared() {
        }

        public void onPatternCellAdded(List<Cell> list) {
            KeyguardPatternView.this.mCallback.userActivity();
        }

        public void onPatternDetected(List<Cell> pattern) {
            KeyguardPatternView.this.mLockPatternView.disableInput();
            if (KeyguardPatternView.this.mPendingLockCheck != null) {
                KeyguardPatternView.this.mPendingLockCheck.cancel(false);
            }
            if (pattern.size() < 4) {
                KeyguardPatternView.this.mLockPatternView.enableInput();
                onPatternChecked(false, 0, false);
                return;
            }
            KeyguardPatternView.this.mPendingLockCheck = LockPatternChecker.checkPattern(KeyguardPatternView.this.mLockPatternUtils, pattern, KeyguardUpdateMonitor.getCurrentUser(), new OnCheckCallback() {
                public void onChecked(boolean matched, int timeoutMs) {
                    KeyguardPatternView.this.mLockPatternView.enableInput();
                    KeyguardPatternView.this.mPendingLockCheck = null;
                    UnlockPatternListener.this.onPatternChecked(matched, timeoutMs, true);
                }
            });
            if (pattern.size() > 2) {
                KeyguardPatternView.this.mCallback.userActivity();
            }
        }

        private void onPatternChecked(boolean matched, int timeoutMs, boolean isValidPattern) {
            if (matched) {
                KeyguardPatternView.this.mCallback.reportUnlockAttempt(true, 0);
                KeyguardPatternView.this.mLockPatternView.setDisplayMode(DisplayMode.Correct);
                KeyguardPatternView.this.mCallback.dismiss(true);
                return;
            }
            KeyguardPatternView.this.mLockPatternView.setDisplayMode(DisplayMode.Wrong);
            if (isValidPattern) {
                KeyguardPatternView.this.mCallback.reportUnlockAttempt(false, timeoutMs);
                if (timeoutMs > 0) {
                    KeyguardPatternView.this.handleAttemptLockout(KeyguardPatternView.this.mLockPatternUtils.setLockoutAttemptDeadline(KeyguardUpdateMonitor.getCurrentUser(), timeoutMs));
                }
            }
            if (timeoutMs == 0) {
                KeyguardPatternView.this.mSecurityMessageDisplay.setMessage(R$string.kg_wrong_pattern, true);
                KeyguardPatternView.this.mLockPatternView.postDelayed(KeyguardPatternView.this.mCancelPatternRunnable, 2000);
            }
        }
    }

    public KeyguardPatternView(Context context) {
        this(context, null);
    }

    public KeyguardPatternView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mCountdownTimer = null;
        this.mLastPokeTime = -7000;
        this.mCancelPatternRunnable = new Runnable() {
            public void run() {
                KeyguardPatternView.this.mLockPatternView.clearPattern();
            }
        };
        this.mTempRect = new Rect();
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mAppearAnimationUtils = new AppearAnimationUtils(context, 220, 1.5f, 2.0f, AnimationUtils.loadInterpolator(this.mContext, 17563662));
        this.mDisappearAnimationUtils = new DisappearAnimationUtils(context, 125, 1.2f, 0.6f, AnimationUtils.loadInterpolator(this.mContext, 17563663));
        this.mDisappearYTranslation = getResources().getDimensionPixelSize(R$dimen.disappear_y_translation);
    }

    public void setKeyguardCallback(KeyguardSecurityCallback callback) {
        this.mCallback = callback;
    }

    public void setLockPatternUtils(LockPatternUtils utils) {
        this.mLockPatternUtils = utils;
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mLockPatternUtils = this.mLockPatternUtils == null ? new LockPatternUtils(this.mContext) : this.mLockPatternUtils;
        this.mLockPatternView = (LockPatternView) findViewById(R$id.lockPatternView);
        this.mLockPatternView.setSaveEnabled(false);
        this.mLockPatternView.setOnPatternListener(new UnlockPatternListener());
        this.mLockPatternView.setInStealthMode(!this.mLockPatternUtils.isVisiblePatternEnabled(KeyguardUpdateMonitor.getCurrentUser()));
        this.mLockPatternView.setTactileFeedbackEnabled(this.mLockPatternUtils.isTactileFeedbackEnabled());
        this.mSecurityMessageDisplay = (KeyguardMessageArea) KeyguardMessageArea.findSecurityMessageDisplay(this);
        this.mEcaView = findViewById(R$id.keyguard_selector_fade_container);
        this.mContainer = (ViewGroup) findViewById(R$id.container);
        EmergencyButton button = (EmergencyButton) findViewById(R$id.emergency_call_button);
        if (button != null) {
            button.setCallback(this);
        }
    }

    public void onEmergencyButtonClickedWhenInCall() {
        this.mCallback.reset();
    }

    public boolean onTouchEvent(MotionEvent ev) {
        boolean result = super.onTouchEvent(ev);
        long elapsed = SystemClock.elapsedRealtime() - this.mLastPokeTime;
        if (result && elapsed > 6900) {
            this.mLastPokeTime = SystemClock.elapsedRealtime();
        }
        this.mTempRect.set(0, 0, 0, 0);
        offsetRectIntoDescendantCoords(this.mLockPatternView, this.mTempRect);
        ev.offsetLocation((float) this.mTempRect.left, (float) this.mTempRect.top);
        if (this.mLockPatternView.dispatchTouchEvent(ev)) {
            result = true;
        }
        ev.offsetLocation((float) (-this.mTempRect.left), (float) (-this.mTempRect.top));
        return result;
    }

    public void reset() {
        this.mLockPatternView.enableInput();
        this.mLockPatternView.setEnabled(true);
        this.mLockPatternView.clearPattern();
        long deadline = this.mLockPatternUtils.getLockoutAttemptDeadline(KeyguardUpdateMonitor.getCurrentUser());
        if (deadline != 0) {
            handleAttemptLockout(deadline);
        } else {
            displayDefaultSecurityMessage();
        }
    }

    private void displayDefaultSecurityMessage() {
        if (this.mKeyguardUpdateMonitor.getMaxBiometricUnlockAttemptsReached()) {
            LockPatternUtils lockPatternUtils = this.mLockPatternUtils;
            KeyguardUpdateMonitor keyguardUpdateMonitor = this.mKeyguardUpdateMonitor;
            if (lockPatternUtils.usingVoiceWeak(KeyguardUpdateMonitor.getCurrentUser())) {
                this.mSecurityMessageDisplay.setMessage(R$string.voiceunlock_multiple_failures, true);
                this.mKeyguardUpdateMonitor.setAlternateUnlockEnabled(false);
                return;
            }
            return;
        }
        this.mSecurityMessageDisplay.setMessage(R$string.kg_pattern_instructions, false);
    }

    private void handleAttemptLockout(long elapsedRealtimeDeadline) {
        this.mLockPatternView.clearPattern();
        this.mLockPatternView.setEnabled(false);
        this.mCountdownTimer = new CountDownTimer(elapsedRealtimeDeadline - SystemClock.elapsedRealtime(), 1000) {
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                KeyguardPatternView.this.mSecurityMessageDisplay.setMessage(R$string.kg_too_many_failed_attempts_countdown, true, Integer.valueOf(secondsRemaining));
            }

            public void onFinish() {
                KeyguardPatternView.this.mLockPatternView.setEnabled(true);
                KeyguardPatternView.this.displayDefaultSecurityMessage();
            }
        }.start();
    }

    public boolean needsInput() {
        return false;
    }

    public void onPause() {
        if (this.mCountdownTimer != null) {
            this.mCountdownTimer.cancel();
            this.mCountdownTimer = null;
        }
        if (this.mPendingLockCheck != null) {
            this.mPendingLockCheck.cancel(false);
            this.mPendingLockCheck = null;
        }
    }

    public void onResume(int reason) {
        reset();
        boolean mediaPlaying = AudioSystem.isStreamActive(3, 0);
        if (this.mLockPatternUtils.usingVoiceWeak() && mediaPlaying) {
            this.mSecurityMessageDisplay.setMessage(R$string.voice_unlock_media_playing, true);
        }
    }

    public void showPromptReason(int reason) {
        switch (reason) {
            case 1:
                this.mSecurityMessageDisplay.setMessage(R$string.kg_prompt_reason_restart_pattern, true);
                return;
            case 2:
                this.mSecurityMessageDisplay.setMessage(R$string.kg_prompt_reason_timeout_pattern, true);
                return;
            default:
                return;
        }
    }

    public void showMessage(String message, int color) {
        this.mSecurityMessageDisplay.setNextMessageColor(color);
        this.mSecurityMessageDisplay.setMessage((CharSequence) message, true);
    }

    public void startAppearAnimation() {
        enableClipping(false);
        setAlpha(1.0f);
        setTranslationY(this.mAppearAnimationUtils.getStartTranslation());
        AppearAnimationUtils.startTranslationYAnimation(this, 0, 500, 0.0f, this.mAppearAnimationUtils.getInterpolator());
        this.mAppearAnimationUtils.startAnimation2d(this.mLockPatternView.getCellStates(), new Runnable() {
            public void run() {
                KeyguardPatternView.this.enableClipping(true);
            }
        }, this);
        if (!TextUtils.isEmpty(this.mSecurityMessageDisplay.getText())) {
            this.mAppearAnimationUtils.createAnimation(this.mSecurityMessageDisplay, 0, 220, this.mAppearAnimationUtils.getStartTranslation(), true, this.mAppearAnimationUtils.getInterpolator(), null);
        }
    }

    public boolean startDisappearAnimation(final Runnable finishRunnable) {
        this.mLockPatternView.clearPattern();
        enableClipping(false);
        setTranslationY(0.0f);
        AppearAnimationUtils.startTranslationYAnimation(this, 0, 300, -this.mDisappearAnimationUtils.getStartTranslation(), this.mDisappearAnimationUtils.getInterpolator());
        this.mDisappearAnimationUtils.startAnimation2d(this.mLockPatternView.getCellStates(), new Runnable() {
            public void run() {
                KeyguardPatternView.this.enableClipping(true);
                if (finishRunnable != null) {
                    finishRunnable.run();
                }
            }
        }, this);
        if (!TextUtils.isEmpty(this.mSecurityMessageDisplay.getText())) {
            this.mDisappearAnimationUtils.createAnimation(this.mSecurityMessageDisplay, 0, 200, (-this.mDisappearAnimationUtils.getStartTranslation()) * 3.0f, false, this.mDisappearAnimationUtils.getInterpolator(), null);
        }
        return true;
    }

    private void enableClipping(boolean enable) {
        setClipChildren(enable);
        this.mContainer.setClipToPadding(enable);
        this.mContainer.setClipChildren(enable);
    }

    public void createAnimation(CellState animatedCell, long delay, long duration, float translationY, boolean appearing, Interpolator interpolator, Runnable finishListener) {
        this.mLockPatternView.startCellStateAnimation(animatedCell, 1.0f, appearing ? 1.0f : 0.0f, appearing ? translationY : 0.0f, appearing ? 0.0f : translationY, appearing ? 0.0f : 1.0f, 1.0f, delay, duration, interpolator, finishListener);
        if (finishListener != null) {
            this.mAppearAnimationUtils.createAnimation(this.mEcaView, delay, duration, translationY, appearing, interpolator, null);
        }
    }

    public boolean hasOverlappingRendering() {
        return false;
    }
}
