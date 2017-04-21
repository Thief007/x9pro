package com.android.keyguard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardHostView.OnDismissAction;
import com.android.keyguard.KeyguardSecurityModel.SecurityMode;
import com.android.systemui.assis.app.MAIN.EVENT;
import com.mediatek.keyguard.AntiTheft.AntiTheftManager;
import com.mediatek.keyguard.Telephony.KeyguardSimPinPukMeView;
import com.mediatek.keyguard.VoiceWakeup.VoiceWakeupManager;

public class KeyguardSecurityContainer extends FrameLayout implements KeyguardSecurityView {
    private static /* synthetic */ int[] -com_android_keyguard_KeyguardSecurityModel$SecurityModeSwitchesValues;
    private KeyguardSecurityCallback mCallback;
    private SecurityMode mCurrentSecuritySelection;
    private LockPatternUtils mLockPatternUtils;
    private ViewGroup mNotificatonPanelView;
    private KeyguardSecurityCallback mNullCallback;
    private SecurityCallback mSecurityCallback;
    private KeyguardSecurityModel mSecurityModel;
    private KeyguardSecurityViewFlipper mSecurityViewFlipper;
    private final KeyguardUpdateMonitor mUpdateMonitor;

    public interface SecurityCallback {
        boolean dismiss(boolean z);

        void finish(boolean z);

        boolean hasOnDismissAction();

        void onSecurityModeChanged(SecurityMode securityMode, boolean z);

        void reset();

        void setOnDismissAction(OnDismissAction onDismissAction);

        void updateNavbarStatus();

        void userActivity();
    }

    private static /* synthetic */ int[] -getcom_android_keyguard_KeyguardSecurityModel$SecurityModeSwitchesValues() {
        if (-com_android_keyguard_KeyguardSecurityModel$SecurityModeSwitchesValues != null) {
            return -com_android_keyguard_KeyguardSecurityModel$SecurityModeSwitchesValues;
        }
        int[] iArr = new int[SecurityMode.values().length];
        try {
            iArr[SecurityMode.AlarmBoot.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[SecurityMode.AntiTheft.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[SecurityMode.Biometric.ordinal()] = 13;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[SecurityMode.Invalid.ordinal()] = 3;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[SecurityMode.None.ordinal()] = 4;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[SecurityMode.PIN.ordinal()] = 5;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[SecurityMode.Password.ordinal()] = 6;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[SecurityMode.Pattern.ordinal()] = 7;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[SecurityMode.SimPinPukMe1.ordinal()] = 8;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[SecurityMode.SimPinPukMe2.ordinal()] = 9;
        } catch (NoSuchFieldError e10) {
        }
        try {
            iArr[SecurityMode.SimPinPukMe3.ordinal()] = 10;
        } catch (NoSuchFieldError e11) {
        }
        try {
            iArr[SecurityMode.SimPinPukMe4.ordinal()] = 11;
        } catch (NoSuchFieldError e12) {
        }
        try {
            iArr[SecurityMode.Voice.ordinal()] = 12;
        } catch (NoSuchFieldError e13) {
        }
        -com_android_keyguard_KeyguardSecurityModel$SecurityModeSwitchesValues = iArr;
        return iArr;
    }

    public KeyguardSecurityContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyguardSecurityContainer(Context context) {
        this(context, null, 0);
    }

    public KeyguardSecurityContainer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mCurrentSecuritySelection = SecurityMode.Invalid;
        this.mCallback = new KeyguardSecurityCallback() {
            public void userActivity() {
                if (KeyguardSecurityContainer.this.mSecurityCallback != null) {
                    KeyguardSecurityContainer.this.mSecurityCallback.userActivity();
                }
            }

            public void dismiss(boolean authenticated) {
                KeyguardSecurityContainer.this.mSecurityCallback.dismiss(authenticated);
            }

            public void reportUnlockAttempt(boolean success, int timeoutMs) {
                KeyguardUpdateMonitor monitor = KeyguardUpdateMonitor.getInstance(KeyguardSecurityContainer.this.mContext);
                if (success) {
                    monitor.clearFailedUnlockAttempts();
                    KeyguardSecurityContainer.this.mLockPatternUtils.reportSuccessfulPasswordAttempt(KeyguardUpdateMonitor.getCurrentUser());
                } else if (KeyguardSecurityContainer.this.mCurrentSecuritySelection == SecurityMode.Biometric || KeyguardSecurityContainer.this.mCurrentSecuritySelection == SecurityMode.Voice) {
                    monitor.reportFailedBiometricUnlockAttempt();
                } else {
                    KeyguardSecurityContainer.this.reportFailedUnlockAttempt(timeoutMs);
                }
            }

            public void reset() {
                KeyguardSecurityContainer.this.mSecurityCallback.reset();
            }

            public boolean hasOnDismissAction() {
                return KeyguardSecurityContainer.this.mSecurityCallback.hasOnDismissAction();
            }

            public void setOnDismissAction(OnDismissAction action) {
                KeyguardSecurityContainer.this.mSecurityCallback.setOnDismissAction(action);
            }

            public void showBackupSecurity() {
                KeyguardSecurityContainer.this.showBackupSecurityScreen();
            }
        };
        this.mNullCallback = new KeyguardSecurityCallback() {
            public void userActivity() {
            }

            public void reportUnlockAttempt(boolean success, int timeoutMs) {
            }

            public void dismiss(boolean securityVerified) {
            }

            public void reset() {
            }

            public void setOnDismissAction(OnDismissAction action) {
            }

            public boolean hasOnDismissAction() {
                return false;
            }

            public void showBackupSecurity() {
            }
        };
        this.mSecurityModel = new KeyguardSecurityModel(context);
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
    }

    public void setSecurityCallback(SecurityCallback callback) {
        this.mSecurityCallback = callback;
    }

    public void onResume(int reason) {
        Log.d("KeyguardSecurityView", "onResume(reason = " + reason + ")");
        Log.d("KeyguardSecurityView", "onResume(mCurrentSecuritySelection = " + this.mCurrentSecuritySelection + ")");
        if (this.mCurrentSecuritySelection != SecurityMode.None) {
            getSecurityView(this.mCurrentSecuritySelection).onResume(reason);
        }
    }

    public void onPause() {
        Log.d("KeyguardSecurityView", "onPause()");
        if (this.mCurrentSecuritySelection != SecurityMode.None) {
            getSecurityView(this.mCurrentSecuritySelection).onPause();
        }
    }

    public void startAppearAnimation() {
        if (this.mCurrentSecuritySelection != SecurityMode.None) {
            getSecurityView(this.mCurrentSecuritySelection).startAppearAnimation();
        }
    }

    public boolean startDisappearAnimation(Runnable onFinishRunnable) {
        if (this.mCurrentSecuritySelection != SecurityMode.None) {
            return getSecurityView(this.mCurrentSecuritySelection).startDisappearAnimation(onFinishRunnable);
        }
        return false;
    }

    public CharSequence getCurrentSecurityModeContentDescription() {
        View v = (View) getSecurityView(this.mCurrentSecuritySelection);
        if (v != null) {
            return v.getContentDescription();
        }
        return "";
    }

    private KeyguardSecurityView getSecurityView(SecurityMode securityMode) {
        int securityViewIdForMode = getSecurityViewIdForMode(securityMode);
        KeyguardSecurityView keyguardSecurityView = null;
        int children = this.mSecurityViewFlipper.getChildCount();
        for (int child = 0; child < children; child++) {
            if (this.mSecurityViewFlipper.getChildAt(child).getId() == securityViewIdForMode) {
                keyguardSecurityView = (KeyguardSecurityView) this.mSecurityViewFlipper.getChildAt(child);
                break;
            }
        }
        int layoutId = getLayoutIdFor(securityMode);
        if (keyguardSecurityView == null && layoutId != 0) {
            LayoutInflater inflater = LayoutInflater.from(this.mContext);
            Log.v("KeyguardSecurityView", "inflating id = " + layoutId);
            View v = inflater.inflate(layoutId, this.mSecurityViewFlipper, false);
            keyguardSecurityView = (KeyguardSecurityView) v;
            if (keyguardSecurityView instanceof KeyguardSimPinPukMeView) {
                ((KeyguardSimPinPukMeView) keyguardSecurityView).setPhoneId(this.mSecurityModel.getPhoneIdUsingSecurityMode(securityMode));
            }
            this.mSecurityViewFlipper.addView(v);
            updateSecurityView(v);
        } else if (!(keyguardSecurityView == null || !(keyguardSecurityView instanceof KeyguardSimPinPukMeView) || securityMode == this.mCurrentSecuritySelection)) {
            Log.i("KeyguardSecurityView", "getSecurityView, here, we will refresh the layout");
            ((KeyguardSimPinPukMeView) keyguardSecurityView).setPhoneId(this.mSecurityModel.getPhoneIdUsingSecurityMode(securityMode));
        }
        return keyguardSecurityView;
    }

    private void updateSecurityView(View view) {
        if (view instanceof KeyguardSecurityView) {
            KeyguardSecurityView ksv = (KeyguardSecurityView) view;
            ksv.setKeyguardCallback(this.mCallback);
            ksv.setLockPatternUtils(this.mLockPatternUtils);
            return;
        }
        Log.w("KeyguardSecurityView", "View " + view + " is not a KeyguardSecurityView");
    }

    protected void onFinishInflate() {
        this.mSecurityViewFlipper = (KeyguardSecurityViewFlipper) findViewById(R$id.view_flipper);
        this.mSecurityViewFlipper.setLockPatternUtils(this.mLockPatternUtils);
    }

    public void setLockPatternUtils(LockPatternUtils utils) {
        this.mLockPatternUtils = utils;
        this.mSecurityModel.setLockPatternUtils(utils);
        this.mSecurityViewFlipper.setLockPatternUtils(this.mLockPatternUtils);
    }

    private void showDialog(String title, String message) {
        AlertDialog dialog = new Builder(this.mContext).setTitle(title).setMessage(message).setNeutralButton(R$string.ok, null).create();
        if (!(this.mContext instanceof Activity)) {
            dialog.getWindow().setType(2009);
        }
        dialog.show();
    }

    private void showTimeoutDialog(int timeoutMs) {
        int timeoutInSeconds = timeoutMs / EVENT.DYNAMIC_PACK_EVENT_BASE;
        int messageId = 0;
        switch (-getcom_android_keyguard_KeyguardSecurityModel$SecurityModeSwitchesValues()[this.mSecurityModel.getSecurityMode().ordinal()]) {
            case 5:
                messageId = R$string.kg_too_many_failed_pin_attempts_dialog_message;
                break;
            case 6:
                messageId = R$string.kg_too_many_failed_password_attempts_dialog_message;
                break;
            case 7:
                messageId = R$string.kg_too_many_failed_pattern_attempts_dialog_message;
                break;
        }
        if (messageId != 0) {
            showDialog(null, this.mContext.getString(messageId, new Object[]{Integer.valueOf(KeyguardUpdateMonitor.getInstance(this.mContext).getFailedUnlockAttempts()), Integer.valueOf(timeoutInSeconds)}));
        }
    }

    private void showAlmostAtWipeDialog(int attempts, int remaining, int userType) {
        String message = null;
        switch (userType) {
            case 1:
                message = this.mContext.getString(R$string.kg_failed_attempts_almost_at_wipe, new Object[]{Integer.valueOf(attempts), Integer.valueOf(remaining)});
                break;
            case 2:
                message = this.mContext.getString(R$string.kg_failed_attempts_almost_at_erase_profile, new Object[]{Integer.valueOf(attempts), Integer.valueOf(remaining)});
                break;
            case 3:
                message = this.mContext.getString(R$string.kg_failed_attempts_almost_at_erase_user, new Object[]{Integer.valueOf(attempts), Integer.valueOf(remaining)});
                break;
        }
        showDialog(null, message);
    }

    private void showWipeDialog(int attempts, int userType) {
        String message = null;
        switch (userType) {
            case 1:
                message = this.mContext.getString(R$string.kg_failed_attempts_now_wiping, new Object[]{Integer.valueOf(attempts)});
                break;
            case 2:
                message = this.mContext.getString(R$string.kg_failed_attempts_now_erasing_profile, new Object[]{Integer.valueOf(attempts)});
                break;
            case 3:
                message = this.mContext.getString(R$string.kg_failed_attempts_now_erasing_user, new Object[]{Integer.valueOf(attempts)});
                break;
        }
        showDialog(null, message);
    }

    private void reportFailedUnlockAttempt(int timeoutMs) {
        int remainingBeforeWipe;
        KeyguardUpdateMonitor monitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        int failedAttempts = monitor.getFailedUnlockAttempts() + 1;
        Log.d("KeyguardSecurityView", "reportFailedPatternAttempt: #" + failedAttempts);
        SecurityMode mode = this.mSecurityModel.getSecurityMode();
        int currentUser = KeyguardUpdateMonitor.getCurrentUser();
        DevicePolicyManager dpm = this.mLockPatternUtils.getDevicePolicyManager();
        int failedAttemptsBeforeWipe = dpm.getMaximumFailedPasswordsForWipe(null, currentUser);
        if (failedAttemptsBeforeWipe > 0) {
            remainingBeforeWipe = failedAttemptsBeforeWipe - failedAttempts;
        } else {
            remainingBeforeWipe = Integer.MAX_VALUE;
        }
        if (remainingBeforeWipe < 5) {
            int expiringUser = dpm.getProfileWithMinimumFailedPasswordsForWipe(currentUser);
            int userType = 1;
            if (expiringUser == currentUser) {
                if (expiringUser != 0) {
                    userType = 3;
                }
            } else if (expiringUser != -10000) {
                userType = 2;
            }
            if (remainingBeforeWipe > 0) {
                showAlmostAtWipeDialog(failedAttempts, remainingBeforeWipe, userType);
            } else {
                Slog.i("KeyguardSecurityView", "Too many unlock attempts; user " + expiringUser + " will be wiped!");
                showWipeDialog(failedAttempts, userType);
            }
        }
        monitor.reportFailedStrongAuthUnlockAttempt();
        this.mLockPatternUtils.reportFailedPasswordAttempt(KeyguardUpdateMonitor.getCurrentUser());
        if (timeoutMs > 0) {
            showTimeoutDialog(timeoutMs);
        }
    }

    void showPrimarySecurityScreen(boolean turningOff) {
        SecurityMode securityMode = this.mSecurityModel.getSecurityMode();
        Log.v("KeyguardSecurityView", "showPrimarySecurityScreen(turningOff=" + turningOff + ")");
        Log.v("KeyguardSecurityView", "showPrimarySecurityScreen(securityMode=" + securityMode + ")");
        if (this.mSecurityModel.isSimPinPukSecurityMode(this.mCurrentSecuritySelection)) {
            Log.d("KeyguardSecurityView", "showPrimarySecurityScreen() - current is " + this.mCurrentSecuritySelection);
            int phoneId = this.mSecurityModel.getPhoneIdUsingSecurityMode(this.mCurrentSecuritySelection);
            Log.d("KeyguardSecurityView", "showPrimarySecurityScreen() - phoneId of currentView is " + phoneId);
            boolean isCurrentModeSimPinSecure = this.mUpdateMonitor.isSimPinSecure(phoneId);
            Log.d("KeyguardSecurityView", "showPrimarySecurityScreen() - isCurrentModeSimPinSecure = " + isCurrentModeSimPinSecure);
            if (isCurrentModeSimPinSecure) {
                Log.d("KeyguardSecurityView", "Skip show security because it already shows SimPinPukMeView");
                return;
            }
            Log.d("KeyguardSecurityView", "showPrimarySecurityScreen() - since current simpinview not secured, we should call showSecurityScreen() to set correct PhoneId for next view.");
        }
        if (!turningOff && KeyguardUpdateMonitor.getInstance(this.mContext).isAlternateUnlockEnabled()) {
            Log.d("KeyguardSecurityView", "showPrimarySecurityScreen() - will be call getAlternateFor");
            securityMode = this.mSecurityModel.getAlternateFor(securityMode);
        }
        showSecurityScreen(securityMode);
    }

    private void showBackupSecurityScreen() {
        Log.d("KeyguardSecurityView", "showBackupSecurity()");
        showSecurityScreen(this.mSecurityModel.getBackupSecurityMode(this.mCurrentSecuritySelection));
    }

    boolean showNextSecurityScreenOrFinish(boolean authenticated) {
        Log.d("KeyguardSecurityView", "showNextSecurityScreenOrFinish(" + authenticated + ")");
        Log.d("KeyguardSecurityView", "showNext.. mCurrentSecuritySelection = " + this.mCurrentSecuritySelection);
        boolean finish = false;
        boolean strongAuth = false;
        if (!this.mUpdateMonitor.getUserCanSkipBouncer(KeyguardUpdateMonitor.getCurrentUser())) {
            SecurityMode securityMode;
            if (SecurityMode.None != this.mCurrentSecuritySelection) {
                if (authenticated) {
                    Log.d("KeyguardSecurityView", "showNextSecurityScreenOrFinish() - authenticated is True, and mCurrentSecuritySelection = " + this.mCurrentSecuritySelection);
                    securityMode = this.mSecurityModel.getSecurityMode();
                    Log.v("KeyguardSecurityView", "securityMode = " + securityMode);
                    Log.d("KeyguardSecurityView", "mCurrentSecuritySelection: " + this.mCurrentSecuritySelection);
                    switch (-getcom_android_keyguard_KeyguardSecurityModel$SecurityModeSwitchesValues()[this.mCurrentSecuritySelection.ordinal()]) {
                        case 2:
                            SecurityMode nextMode = this.mSecurityModel.getSecurityMode();
                            Log.v("KeyguardSecurityView", "now is Antitheft, next securityMode = " + nextMode);
                            if (nextMode == SecurityMode.None) {
                                finish = true;
                                break;
                            }
                            showSecurityScreen(nextMode);
                            break;
                        case 5:
                        case 6:
                        case 7:
                            strongAuth = true;
                            finish = true;
                            break;
                        case 8:
                        case 9:
                        case 10:
                        case 11:
                            if (securityMode == SecurityMode.None) {
                                finish = true;
                                break;
                            }
                            showSecurityScreen(securityMode);
                            break;
                        case R$styleable.GlowPadView_feedbackCount /*12*/:
                            if (!this.mSecurityModel.isSimPinPukSecurityMode(securityMode)) {
                                finish = true;
                                break;
                            }
                            showSecurityScreen(securityMode);
                            break;
                        default:
                            Log.v("KeyguardSecurityView", "Bad security screen " + this.mCurrentSecuritySelection + ", fail safe");
                            showPrimarySecurityScreen(false);
                            break;
                    }
                }
            }
            securityMode = this.mSecurityModel.getSecurityMode();
            if (SecurityMode.None == securityMode) {
                Log.d("KeyguardSecurityView", "showNextSecurityScreenOrFinish() - securityMode is None, just finish.");
                finish = true;
            } else {
                Log.d("KeyguardSecurityView", "showNextSecurityScreenOrFinish()- switch to the alternate security view for None mode.");
                showSecurityScreen(securityMode);
            }
        } else {
            finish = true;
        }
        this.mSecurityCallback.updateNavbarStatus();
        if (finish) {
            this.mSecurityCallback.finish(strongAuth);
            Log.d("KeyguardSecurityView", "finish ");
        }
        Log.d("KeyguardSecurityView", "showNextSecurityScreenOrFinish() - return finish = " + finish);
        return finish;
    }

    private void showSecurityScreen(SecurityMode securityMode) {
        Log.d("KeyguardSecurityView", "showSecurityScreen(" + securityMode + ")");
        if (securityMode != this.mCurrentSecuritySelection || securityMode == SecurityMode.AntiTheft) {
            VoiceWakeupManager.getInstance().notifySecurityModeChange(this.mCurrentSecuritySelection, securityMode);
            Log.d("KeyguardSecurityView", "showSecurityScreen() - get oldview for" + this.mCurrentSecuritySelection);
            KeyguardSecurityView oldView = getSecurityView(this.mCurrentSecuritySelection);
            Log.d("KeyguardSecurityView", "showSecurityScreen() - get newview for" + securityMode);
            KeyguardSecurityView newView = getSecurityView(securityMode);
            if (oldView != null) {
                oldView.onPause();
                Log.d("KeyguardSecurityView", "showSecurityScreen() - oldview.setKeyguardCallback(mNullCallback)");
                oldView.setKeyguardCallback(this.mNullCallback);
            }
            if (securityMode != SecurityMode.None) {
                newView.setKeyguardCallback(this.mCallback);
                Log.d("KeyguardSecurityView", "showSecurityScreen() - newview.setKeyguardCallback(mCallback)");
                newView.onResume(2);
            }
            int childCount = this.mSecurityViewFlipper.getChildCount();
            int securityViewIdForMode = getSecurityViewIdForMode(securityMode);
            for (int i = 0; i < childCount; i++) {
                if (this.mSecurityViewFlipper.getChildAt(i).getId() == securityViewIdForMode) {
                    this.mSecurityViewFlipper.setDisplayedChild(i);
                    break;
                }
            }
            Log.d("KeyguardSecurityView", "Before update, mCurrentSecuritySelection = " + this.mCurrentSecuritySelection);
            this.mCurrentSecuritySelection = securityMode;
            Log.d("KeyguardSecurityView", "After update, mCurrentSecuritySelection = " + this.mCurrentSecuritySelection);
            this.mSecurityCallback.onSecurityModeChanged(securityMode, securityMode != SecurityMode.None ? newView.needsInput() : false);
        }
    }

    private int getSecurityViewIdForMode(SecurityMode securityMode) {
        switch (-getcom_android_keyguard_KeyguardSecurityModel$SecurityModeSwitchesValues()[securityMode.ordinal()]) {
            case 1:
                return R$id.power_off_alarm_view;
            case 2:
                return AntiTheftManager.getAntiTheftViewId();
            case 5:
                return R$id.keyguard_pin_view;
            case 6:
                return R$id.keyguard_password_view;
            case 7:
                return R$id.keyguard_pattern_view;
            case 8:
            case 9:
            case 10:
            case 11:
                return R$id.keyguard_sim_pin_puk_me_view;
            case R$styleable.GlowPadView_feedbackCount /*12*/:
                return R$id.voice_unlock_view;
            default:
                return 0;
        }
    }

    private int getLayoutIdFor(SecurityMode securityMode) {
        Log.d("KeyguardSecurityView", "getLayoutIdFor, SecurityMode-->" + securityMode);
        switch (-getcom_android_keyguard_KeyguardSecurityModel$SecurityModeSwitchesValues()[securityMode.ordinal()]) {
            case 1:
                return R$layout.mtk_power_off_alarm_view;
            case 2:
                return AntiTheftManager.getAntiTheftLayoutId();
            case 5:
                return R$layout.keyguard_pin_view;
            case 6:
                return R$layout.keyguard_password_view;
            case 7:
                return R$layout.keyguard_pattern_view;
            case 8:
            case 9:
            case 10:
            case 11:
                return R$layout.mtk_keyguard_sim_pin_puk_me_view;
            case R$styleable.GlowPadView_feedbackCount /*12*/:
                return R$layout.mtk_voice_unlock_view;
            default:
                return 0;
        }
    }

    public SecurityMode getSecurityMode() {
        return this.mSecurityModel.getSecurityMode();
    }

    public SecurityMode getCurrentSecurityMode() {
        return this.mCurrentSecuritySelection;
    }

    public boolean needsInput() {
        return this.mSecurityViewFlipper.needsInput();
    }

    public void setKeyguardCallback(KeyguardSecurityCallback callback) {
        this.mSecurityViewFlipper.setKeyguardCallback(callback);
    }

    public void showPromptReason(int reason) {
        if (this.mCurrentSecuritySelection != SecurityMode.None) {
            getSecurityView(this.mCurrentSecuritySelection).showPromptReason(reason);
        }
    }

    public void showMessage(String message, int color) {
        if (this.mCurrentSecuritySelection != SecurityMode.None) {
            getSecurityView(this.mCurrentSecuritySelection).showMessage(message, color);
        }
    }

    public void setNotificationPanelView(ViewGroup notificationPanelView) {
        this.mNotificatonPanelView = notificationPanelView;
    }

    public void onScreenTurnedOff() {
        Log.d("KeyguardSecurityView", "onScreenTurnedOff");
    }
}
