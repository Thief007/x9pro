package com.android.keyguard;

import android.content.Context;
import android.util.Log;
import com.android.internal.telephony.IccCardConstants.State;
import com.android.internal.widget.LockPatternUtils;
import com.mediatek.keyguard.AntiTheft.AntiTheftManager;
import com.mediatek.keyguard.PowerOffAlarm.PowerOffAlarmManager;

public class KeyguardSecurityModel {
    private static /* synthetic */ int[] -com_android_keyguard_KeyguardSecurityModel$SecurityModeSwitchesValues;
    private final Context mContext;
    private final boolean mIsPukScreenAvailable = this.mContext.getResources().getBoolean(17956931);
    private LockPatternUtils mLockPatternUtils;

    public enum SecurityMode {
        Invalid,
        None,
        Pattern,
        Password,
        PIN,
        SimPinPukMe1,
        SimPinPukMe2,
        SimPinPukMe3,
        SimPinPukMe4,
        AlarmBoot,
        Biometric,
        Voice,
        AntiTheft
    }

    private static /* synthetic */ int[] -getcom_android_keyguard_KeyguardSecurityModel$SecurityModeSwitchesValues() {
        if (-com_android_keyguard_KeyguardSecurityModel$SecurityModeSwitchesValues != null) {
            return -com_android_keyguard_KeyguardSecurityModel$SecurityModeSwitchesValues;
        }
        int[] iArr = new int[SecurityMode.values().length];
        try {
            iArr[SecurityMode.AlarmBoot.ordinal()] = 7;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[SecurityMode.AntiTheft.ordinal()] = 8;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[SecurityMode.Biometric.ordinal()] = 1;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[SecurityMode.Invalid.ordinal()] = 9;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[SecurityMode.None.ordinal()] = 10;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[SecurityMode.PIN.ordinal()] = 11;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[SecurityMode.Password.ordinal()] = 12;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[SecurityMode.Pattern.ordinal()] = 13;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[SecurityMode.SimPinPukMe1.ordinal()] = 2;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[SecurityMode.SimPinPukMe2.ordinal()] = 3;
        } catch (NoSuchFieldError e10) {
        }
        try {
            iArr[SecurityMode.SimPinPukMe3.ordinal()] = 4;
        } catch (NoSuchFieldError e11) {
        }
        try {
            iArr[SecurityMode.SimPinPukMe4.ordinal()] = 5;
        } catch (NoSuchFieldError e12) {
        }
        try {
            iArr[SecurityMode.Voice.ordinal()] = 6;
        } catch (NoSuchFieldError e13) {
        }
        -com_android_keyguard_KeyguardSecurityModel$SecurityModeSwitchesValues = iArr;
        return iArr;
    }

    public KeyguardSecurityModel(Context context) {
        this.mContext = context;
        this.mLockPatternUtils = new LockPatternUtils(context);
    }

    void setLockPatternUtils(LockPatternUtils utils) {
        this.mLockPatternUtils = utils;
    }

    public SecurityMode getSecurityMode() {
        Log.d("KeyguardSecurityModel", "getSecurityMode() is called.");
        KeyguardUpdateMonitor monitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        SecurityMode mode = SecurityMode.None;
        if (PowerOffAlarmManager.isAlarmBoot()) {
            mode = SecurityMode.AlarmBoot;
        } else {
            int i = 0;
            while (i < KeyguardUtils.getNumOfPhone()) {
                if (!isPinPukOrMeRequiredOfPhoneId(i)) {
                    i++;
                } else if (i == 0) {
                    mode = SecurityMode.SimPinPukMe1;
                } else if (1 == i) {
                    mode = SecurityMode.SimPinPukMe2;
                } else if (2 == i) {
                    mode = SecurityMode.SimPinPukMe3;
                } else if (3 == i) {
                    mode = SecurityMode.SimPinPukMe4;
                }
            }
        }
        if (AntiTheftManager.isAntiTheftPriorToSecMode(mode)) {
            Log.d("KeyguardSecurityModel", "should show AntiTheft!");
            mode = SecurityMode.AntiTheft;
        }
        if (mode == SecurityMode.None) {
            int security = this.mLockPatternUtils.getActivePasswordQuality(KeyguardUpdateMonitor.getCurrentUser());
            Log.d("KeyguardSecurityModel", "getSecurityMode() - security = " + security);
            switch (security) {
                case 0:
                    return SecurityMode.None;
                case 65536:
                    return SecurityMode.Pattern;
                case 131072:
                case 196608:
                    return SecurityMode.PIN;
                case 262144:
                case 327680:
                case 393216:
                    return SecurityMode.Password;
                default:
                    throw new IllegalStateException("Unknown security quality:" + security);
            }
        }
        Log.d("KeyguardSecurityModel", "getSecurityMode() - mode = " + mode);
        return mode;
    }

    public boolean isPinPukOrMeRequiredOfPhoneId(int phoneId) {
        boolean z = false;
        KeyguardUpdateMonitor updateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        if (updateMonitor == null) {
            return false;
        }
        State simState = updateMonitor.getSimStateOfPhoneId(phoneId);
        Log.d("KeyguardSecurityModel", "isPinPukOrMeRequiredOfSubId() - phoneId = " + phoneId + ", simState = " + simState);
        if ((simState == State.PIN_REQUIRED && !updateMonitor.getPinPukMeDismissFlagOfPhoneId(phoneId)) || (simState == State.PUK_REQUIRED && !updateMonitor.getPinPukMeDismissFlagOfPhoneId(phoneId) && updateMonitor.getRetryPukCountOfPhoneId(phoneId) != 0)) {
            z = true;
        } else if (!(simState != State.NETWORK_LOCKED || updateMonitor.getPinPukMeDismissFlagOfPhoneId(phoneId) || updateMonitor.getSimMeLeftRetryCountOfPhoneId(phoneId) == 0)) {
            z = KeyguardUtils.isMediatekSimMeLockSupport();
        }
        return z;
    }

    int getPhoneIdUsingSecurityMode(SecurityMode mode) {
        if (isSimPinPukSecurityMode(mode)) {
            return mode.ordinal() - SecurityMode.SimPinPukMe1.ordinal();
        }
        return -1;
    }

    boolean isSimPinPukSecurityMode(SecurityMode mode) {
        switch (-getcom_android_keyguard_KeyguardSecurityModel$SecurityModeSwitchesValues()[mode.ordinal()]) {
            case 2:
            case 3:
            case 4:
            case 5:
                return true;
            default:
                return false;
        }
    }

    boolean isBiometricUnlockEnabled() {
        return this.mLockPatternUtils.usingBiometricWeak();
    }

    private boolean isBiometricUnlockSuppressed() {
        KeyguardUpdateMonitor monitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        boolean backupIsTimedOut = monitor.getFailedUnlockAttempts() >= 4;
        if (monitor.getMaxBiometricUnlockAttemptsReached() || backupIsTimedOut || !monitor.isAlternateUnlockEnabled() || monitor.getPhoneState() != 0) {
            return true;
        }
        return false;
    }

    SecurityMode getAlternateFor(SecurityMode mode) {
        if (!isBiometricUnlockSuppressed() && (mode == SecurityMode.Password || mode == SecurityMode.PIN || mode == SecurityMode.Pattern)) {
            if (isBiometricUnlockEnabled()) {
                return SecurityMode.Biometric;
            }
            if (this.mLockPatternUtils.usingVoiceWeak()) {
                return SecurityMode.Voice;
            }
        }
        return mode;
    }

    SecurityMode getBackupSecurityMode(SecurityMode mode) {
        switch (-getcom_android_keyguard_KeyguardSecurityModel$SecurityModeSwitchesValues()[mode.ordinal()]) {
            case 1:
            case 6:
                return getSecurityMode();
            default:
                return mode;
        }
    }
}
