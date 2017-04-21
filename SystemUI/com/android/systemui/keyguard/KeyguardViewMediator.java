package com.android.systemui.keyguard;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.IActivityManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.app.StatusBarManager;
import android.app.trust.TrustManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.telephony.TelephonyManager;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import android.view.IWindowManager;
import android.view.ViewGroup;
import android.view.WindowManagerGlobal;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.android.internal.policy.IKeyguardDrawnCallback;
import com.android.internal.policy.IKeyguardExitCallback;
import com.android.internal.policy.IKeyguardStateCallback;
import com.android.internal.telephony.IccCardConstants.State;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardDisplayManager;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.KeyguardUtils;
import com.android.keyguard.R$styleable;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.R;
import com.android.systemui.SystemUI;
import com.android.systemui.assis.app.MAIN.EVENT;
import com.android.systemui.statusbar.phone.FingerprintUnlockController;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.phone.ScrimController;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.android.systemui.statusbar.phone.StatusBarWindowManager;
import com.mediatek.keyguard.AntiTheft.AntiTheftManager;
import com.mediatek.keyguard.Plugin.KeyguardPluginFactory;
import com.mediatek.keyguard.PowerOffAlarm.PowerOffAlarmManager;
import com.mediatek.keyguard.Telephony.KeyguardDialogManager;
import com.mediatek.keyguard.Telephony.KeyguardDialogManager.DialogShowCallBack;
import com.mediatek.keyguard.VoiceWakeup.VoiceWakeupManager;
import com.mediatek.systemui.statusbar.extcb.FeatureOptionUtils;
import com.mediatek.systemui.statusbar.extcb.SvLteController;
import java.util.ArrayList;

public class KeyguardViewMediator extends SystemUI {
    private static final Intent USER_PRESENT_INTENT = new Intent("android.intent.action.USER_PRESENT").addFlags(603979776);
    private static boolean mKeyguardDoneOnGoing = false;
    private static final boolean sIsUserBuild = SystemProperties.get(FeatureOptionUtils.BUILD_TYPE).equals(FeatureOptionUtils.BUILD_TYPE_USER);
    private AlarmManager mAlarmManager;
    private AntiTheftManager mAntiTheftManager;
    private AudioManager mAudioManager;
    private boolean mBootCompleted;
    private boolean mBootSendUserPresent;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.GESTURE_U".equals(action)) {
                KeyguardViewMediator.this.mGestureHandler.sendEmptyMessageDelayed(-1, 0);
            } else if ("android.intent.action.GESTURE_UP".equals(action)) {
                KeyguardViewMediator.this.mGestureHandler.sendEmptyMessageDelayed(-1, 0);
                if (!KeyguardViewMediator.this.isSecure()) {
                    KeyguardViewMediator.this.mHandler.sendEmptyMessageDelayed(3, 100);
                }
            } else if ("android.intent.action.GESTURE_DOWN".equals(action)) {
                KeyguardViewMediator.this.mContext.sendBroadcast(new Intent("com.android.music.musicservicecommand.pause"), null);
            } else if ("android.intent.action.GESTURE_RIGHT".equals(action)) {
                KeyguardViewMediator.this.mContext.sendBroadcast(new Intent("com.android.music.musicservicecommand.next"), null);
            } else if ("android.intent.action.GESTURE_LEFT".equals(action)) {
                KeyguardViewMediator.this.mContext.sendBroadcast(new Intent("com.android.music.musicservicecommand.previous"), null);
            } else if ("android.intent.action.GESTURE_E".equals(action)) {
                KeyguardViewMediator.this.mGestureHandler.sendEmptyMessageDelayed(-1, 300);
                if (!KeyguardViewMediator.this.isSecure()) {
                    KeyguardViewMediator.this.mHandler.sendEmptyMessageDelayed(3, 800);
                    KeyguardViewMediator.this.mGestureHandler.sendEmptyMessageDelayed(1, 1500);
                }
            } else if ("android.intent.action.GESTURE_C".equals(action)) {
                KeyguardViewMediator.this.mGestureHandler.sendEmptyMessageDelayed(-1, 300);
                if (!KeyguardViewMediator.this.isSecure()) {
                    KeyguardViewMediator.this.mHandler.sendEmptyMessageDelayed(3, 800);
                    KeyguardViewMediator.this.mGestureHandler.sendEmptyMessageDelayed(0, 1500);
                }
            } else if ("android.intent.action.GESTURE_W".equals(action)) {
                KeyguardViewMediator.this.mGestureHandler.sendEmptyMessageDelayed(-1, 300);
                if (!KeyguardViewMediator.this.isSecure()) {
                    KeyguardViewMediator.this.mHandler.sendEmptyMessageDelayed(3, 800);
                    KeyguardViewMediator.this.mGestureHandler.sendEmptyMessageDelayed(2, 1500);
                }
            } else if ("android.intent.action.GESTURE_O".equals(action)) {
                KeyguardViewMediator.this.mGestureHandler.sendEmptyMessageDelayed(-1, 300);
                if (!KeyguardViewMediator.this.isSecure()) {
                    KeyguardViewMediator.this.mHandler.sendEmptyMessageDelayed(3, 800);
                    KeyguardViewMediator.this.mGestureHandler.sendEmptyMessageDelayed(4, 1500);
                }
            } else if ("android.intent.action.GESTURE_M".equals(action)) {
                KeyguardViewMediator.this.mGestureHandler.sendEmptyMessageDelayed(-1, 300);
                if (!KeyguardViewMediator.this.isSecure()) {
                    KeyguardViewMediator.this.mHandler.sendEmptyMessageDelayed(3, 800);
                    KeyguardViewMediator.this.mGestureHandler.sendEmptyMessageDelayed(3, 1500);
                }
            }
            if ("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_KEYGUARD".equals(action)) {
                int sequence = intent.getIntExtra("seq", 0);
                Log.d("KeyguardViewMediator", "received DELAYED_KEYGUARD_ACTION with seq = " + sequence + ", mDelayedShowingSequence = " + KeyguardViewMediator.this.mDelayedShowingSequence);
                synchronized (KeyguardViewMediator.this) {
                    if (KeyguardViewMediator.this.mDelayedShowingSequence == sequence) {
                        KeyguardViewMediator.this.mSuppressNextLockSound = true;
                        KeyguardViewMediator.this.doKeyguardLocked(null);
                    }
                }
            } else if ("android.intent.action.ACTION_PRE_SHUTDOWN".equals(action)) {
                Log.w("KeyguardViewMediator", "PRE_SHUTDOWN: " + action);
                KeyguardViewMediator.this.mSuppressNextLockSound = true;
            } else if ("android.intent.action.ACTION_SHUTDOWN_IPO".equals(action)) {
                Log.w("KeyguardViewMediator", "IPO_SHUTDOWN: " + action);
                KeyguardViewMediator.this.mIsIPOShutDown = true;
                KeyguardViewMediator.this.mHandler.sendEmptyMessageDelayed(1002, 4000);
            } else if ("android.intent.action.ACTION_PREBOOT_IPO".equals(action)) {
                Log.w("KeyguardViewMediator", "IPO_BOOTUP: " + action);
                KeyguardViewMediator.this.mIsIPOShutDown = false;
                for (int i = 0; i < KeyguardUtils.getNumOfPhone(); i++) {
                    KeyguardViewMediator.this.mUpdateMonitor.setPinPukMeDismissFlagOfPhoneId(i, false);
                    Log.d("KeyguardViewMediator", "setPinPukMeDismissFlagOfPhoneId false: " + i);
                }
            }
        }
    };
    private int mDelayedShowingSequence;
    private boolean mDeviceInteractive;
    private KeyguardDialogManager mDialogManager;
    private IKeyguardDrawnCallback mDrawnCallback;
    private IKeyguardExitCallback mExitSecureCallback;
    private boolean mExternallyEnabled = true;
    private Handler mGestureHandler = new Handler(Looper.myLooper(), null, true) {
        public void handleMessage(Message msg) {
            ComponentName componetName;
            switch (msg.what) {
                case SvLteController.PS_SERVICE_UNKNOWN /*-1*/:
                    KeyguardViewMediator.this.mPM.wakeUp(SystemClock.uptimeMillis());
                    return;
                case 0:
                    componetName = KeyguardViewMediator.this.getGestureAppName(0);
                    if (KeyguardViewMediator.this.isApkExist(KeyguardViewMediator.this.mContext, componetName.getPackageName())) {
                        KeyguardViewMediator.this.launchGestureApp(componetName);
                        return;
                    }
                    return;
                case 1:
                    componetName = KeyguardViewMediator.this.getGestureAppName(1);
                    if (KeyguardViewMediator.this.isApkExist(KeyguardViewMediator.this.mContext, componetName.getPackageName())) {
                        KeyguardViewMediator.this.launchGestureApp(componetName);
                        return;
                    }
                    return;
                case 2:
                    componetName = KeyguardViewMediator.this.getGestureAppName(2);
                    if (KeyguardViewMediator.this.isApkExist(KeyguardViewMediator.this.mContext, componetName.getPackageName())) {
                        KeyguardViewMediator.this.launchGestureApp(componetName);
                        return;
                    }
                    return;
                case 3:
                    componetName = KeyguardViewMediator.this.getGestureAppName(3);
                    if (KeyguardViewMediator.this.isApkExist(KeyguardViewMediator.this.mContext, componetName.getPackageName())) {
                        KeyguardViewMediator.this.launchGestureApp(componetName);
                        return;
                    }
                    return;
                case 4:
                    componetName = KeyguardViewMediator.this.getGestureAppName(4);
                    if (KeyguardViewMediator.this.isApkExist(KeyguardViewMediator.this.mContext, componetName.getPackageName())) {
                        KeyguardViewMediator.this.launchGestureApp(componetName);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    };
    private boolean mGoingToSleep;
    private Handler mHandler = new Handler(Looper.myLooper(), null, true) {
        public void handleMessage(Message msg) {
            boolean z = true;
            KeyguardViewMediator keyguardViewMediator;
            switch (msg.what) {
                case 2:
                    KeyguardViewMediator.this.handleShow((Bundle) msg.obj);
                    return;
                case 3:
                    KeyguardViewMediator.this.handleHide();
                    return;
                case 4:
                    KeyguardViewMediator.this.handleReset();
                    return;
                case 5:
                    KeyguardViewMediator.this.handleVerifyUnlock();
                    return;
                case 6:
                    KeyguardViewMediator.this.handleNotifyFinishedGoingToSleep();
                    return;
                case 7:
                    KeyguardViewMediator.this.handleNotifyScreenTurningOn((IKeyguardDrawnCallback) msg.obj);
                    return;
                case 9:
                    keyguardViewMediator = KeyguardViewMediator.this;
                    if (msg.arg1 == 0) {
                        z = false;
                    }
                    keyguardViewMediator.handleKeyguardDone(z);
                    return;
                case 10:
                    KeyguardViewMediator.this.handleKeyguardDoneDrawing();
                    return;
                case R$styleable.GlowPadView_feedbackCount /*12*/:
                    keyguardViewMediator = KeyguardViewMediator.this;
                    if (msg.arg1 == 0) {
                        z = false;
                    }
                    keyguardViewMediator.handleSetOccluded(z);
                    return;
                case R$styleable.GlowPadView_alwaysTrackFinger /*13*/:
                    synchronized (KeyguardViewMediator.this) {
                        Log.d("KeyguardViewMediator", "doKeyguardLocked, because:KEYGUARD_TIMEOUT");
                        KeyguardViewMediator.this.doKeyguardLocked((Bundle) msg.obj);
                    }
                    return;
                case 17:
                    KeyguardViewMediator.this.handleDismiss(((Boolean) msg.obj).booleanValue());
                    return;
                case 18:
                    StartKeyguardExitAnimParams params = msg.obj;
                    KeyguardViewMediator.this.handleStartKeyguardExitAnimation(params.startTime, params.fadeoutDuration);
                    return;
                case 19:
                    break;
                case EVENT.DIALOG_CONFIRMED /*20*/:
                    Log.w("KeyguardViewMediator", "Timeout while waiting for activity drawn!");
                    break;
                case EVENT.DIALOG_CANCELLED /*21*/:
                    KeyguardViewMediator.this.handleNotifyStartedWakingUp();
                    return;
                case 22:
                    KeyguardViewMediator.this.handleNotifyScreenTurnedOn();
                    return;
                case 23:
                    KeyguardViewMediator.this.handleNotifyScreenTurnedOff();
                    return;
                default:
                    return;
            }
            KeyguardViewMediator.this.handleOnActivityDrawn();
        }
    };
    private Animation mHideAnimation;
    private boolean mHideAnimationRun = false;
    private boolean mHiding;
    private boolean mInputRestricted;
    private boolean mIsIPOShutDown = false;
    private KeyguardDisplayManager mKeyguardDisplayManager;
    private boolean mKeyguardDonePending = false;
    private final Runnable mKeyguardGoingAwayRunnable = new Runnable() {
        public void run() {
            try {
                boolean z;
                KeyguardViewMediator.this.mStatusBarKeyguardViewManager.keyguardGoingAway();
                IActivityManager iActivityManager = ActivityManagerNative.getDefault();
                if (KeyguardViewMediator.this.mStatusBarKeyguardViewManager.shouldDisableWindowAnimationsForUnlock()) {
                    z = true;
                } else {
                    z = KeyguardViewMediator.this.mWakeAndUnlocking;
                }
                iActivityManager.keyguardGoingAway(z, KeyguardViewMediator.this.mStatusBarKeyguardViewManager.isGoingToNotificationShade());
            } catch (RemoteException e) {
                Log.e("KeyguardViewMediator", "Error while calling WindowManager", e);
            }
        }
    };
    private final ArrayList<IKeyguardStateCallback> mKeyguardStateCallbacks = new ArrayList();
    private LockPatternUtils mLockPatternUtils;
    private int mLockSoundId;
    private int mLockSoundStreamId;
    private float mLockSoundVolume;
    private SoundPool mLockSounds;
    private boolean mNeedToReshowWhenReenabled = false;
    private boolean mOccluded = false;
    private PowerManager mPM;
    private boolean mPendingLock;
    private boolean mPendingReset;
    private String mPhoneState = TelephonyManager.EXTRA_STATE_IDLE;
    private PowerOffAlarmManager mPowerOffAlarmManager;
    private boolean mReadyToShow = false;
    private SearchManager mSearchManager;
    private WakeLock mShowKeyguardWakeLock;
    private boolean mShowing;
    private StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private StatusBarManager mStatusBarManager;
    private boolean mSuppressNextLockSound = true;
    private boolean mSwitchingUser;
    private boolean mSystemReady;
    private TrustManager mTrustManager;
    private int mTrustedSoundId;
    private int mUiSoundsStreamType;
    private int mUnlockSoundId;
    KeyguardUpdateMonitorCallback mUpdateCallback = new KeyguardUpdateMonitorCallback() {
        private static /* synthetic */ int[] -com_android_internal_telephony_IccCardConstants$StateSwitchesValues;

        private static /* synthetic */ int[] -getcom_android_internal_telephony_IccCardConstants$StateSwitchesValues() {
            if (-com_android_internal_telephony_IccCardConstants$StateSwitchesValues != null) {
                return -com_android_internal_telephony_IccCardConstants$StateSwitchesValues;
            }
            int[] iArr = new int[State.values().length];
            try {
                iArr[State.ABSENT.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[State.CARD_IO_ERROR.ordinal()] = 8;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[State.NETWORK_LOCKED.ordinal()] = 2;
            } catch (NoSuchFieldError e3) {
            }
            try {
                iArr[State.NOT_READY.ordinal()] = 3;
            } catch (NoSuchFieldError e4) {
            }
            try {
                iArr[State.PERM_DISABLED.ordinal()] = 4;
            } catch (NoSuchFieldError e5) {
            }
            try {
                iArr[State.PIN_REQUIRED.ordinal()] = 5;
            } catch (NoSuchFieldError e6) {
            }
            try {
                iArr[State.PUK_REQUIRED.ordinal()] = 6;
            } catch (NoSuchFieldError e7) {
            }
            try {
                iArr[State.READY.ordinal()] = 7;
            } catch (NoSuchFieldError e8) {
            }
            try {
                iArr[State.UNKNOWN.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            -com_android_internal_telephony_IccCardConstants$StateSwitchesValues = iArr;
            return iArr;
        }

        public void onUserSwitching(int userId) {
            synchronized (KeyguardViewMediator.this) {
                KeyguardViewMediator.this.mSwitchingUser = true;
                KeyguardViewMediator.this.resetKeyguardDonePendingLocked();
                KeyguardViewMediator.this.resetStateLocked();
                KeyguardViewMediator.this.adjustStatusBarLocked();
            }
        }

        public void onUserSwitchComplete(int userId) {
            KeyguardViewMediator.this.mSwitchingUser = false;
            if (userId != 0) {
                UserInfo info = UserManager.get(KeyguardViewMediator.this.mContext).getUserInfo(userId);
                if (info != null && info.isGuest()) {
                    KeyguardViewMediator.this.dismiss();
                }
            }
        }

        public void onUserInfoChanged(int userId) {
        }

        public void onPhoneStateChanged(int phoneState) {
            synchronized (KeyguardViewMediator.this) {
                if (phoneState == 0) {
                    if (!KeyguardViewMediator.this.mDeviceInteractive) {
                        if (KeyguardViewMediator.this.mExternallyEnabled) {
                            Log.d("KeyguardViewMediator", "screen is off and call ended, let's make sure the keyguard is showing");
                        }
                    }
                }
            }
        }

        public void onClockVisibilityChanged() {
            KeyguardViewMediator.this.adjustStatusBarLocked();
        }

        public void onDeviceProvisioned() {
            KeyguardViewMediator.this.sendUserPresentBroadcast();
        }

        public void onSimStateChangedUsingPhoneId(int phoneId, State simState) {
            Log.d("KeyguardViewMediator", "onSimStateChangedUsingSubId: " + simState + ", phoneId=" + phoneId);
            switch (AnonymousClass1.-getcom_android_internal_telephony_IccCardConstants$StateSwitchesValues()[simState.ordinal()]) {
                case 1:
                case 3:
                    synchronized (this) {
                        if (KeyguardViewMediator.this.shouldWaitForProvisioning()) {
                            if (!KeyguardViewMediator.this.mShowing) {
                                Log.d("KeyguardViewMediator", "ICC_ABSENT isn't showing, we need to show the keyguard since the device isn't provisioned yet.");
                                KeyguardViewMediator.this.doKeyguardLocked(null);
                                break;
                            }
                            KeyguardViewMediator.this.resetStateLocked();
                            break;
                        }
                    }
                    break;
                case 2:
                case 5:
                case 6:
                    synchronized (this) {
                        if (simState != State.NETWORK_LOCKED || KeyguardUtils.isMediatekSimMeLockSupport()) {
                            if (!KeyguardViewMediator.this.isSystemEncrypted()) {
                                if (KeyguardViewMediator.this.mUpdateMonitor.getRetryPukCountOfPhoneId(phoneId) != 0) {
                                    if (State.NETWORK_LOCKED != simState || KeyguardViewMediator.this.mUpdateMonitor.getSimMeLeftRetryCountOfPhoneId(phoneId) != 0) {
                                        if (KeyguardViewMediator.this.isShowing()) {
                                            if (!KeyguardViewMediator.mKeyguardDoneOnGoing) {
                                                KeyguardViewMediator.this.removeKeyguardDoneMsg();
                                                KeyguardViewMediator.this.resetStateLocked();
                                                break;
                                            }
                                            Log.d("KeyguardViewMediator", "mKeyguardDoneOnGoing is true");
                                            Log.d("KeyguardViewMediator", "Give a buffer time for system to set mShowing = false,");
                                            Log.d("KeyguardViewMediator", "or we still cannot show the keyguard.");
                                            KeyguardViewMediator.this.doKeyguardLaterLocked();
                                            break;
                                        }
                                        Log.d("KeyguardViewMediator", "!isShowing() = true");
                                        Log.d("KeyguardViewMediator", "INTENT_VALUE_ICC_LOCKED and keygaurd isn't showing; need to show keyguard so user can enter sim pin");
                                        KeyguardViewMediator.this.doKeyguardLocked(null);
                                        break;
                                    }
                                    Log.d("KeyguardViewMediator", "SIM ME lock retrycount is 0, only to show dialog");
                                    KeyguardViewMediator.this.mDialogManager.requestShowDialog(new MeLockedDialogCallback());
                                    break;
                                }
                                KeyguardViewMediator.this.mDialogManager.requestShowDialog(new InvalidDialogCallback());
                                break;
                            }
                            Log.d("KeyguardViewMediator", "Currently system needs to be decrypted. Not show.");
                            break;
                        }
                        Log.d("KeyguardViewMediator", "Get NETWORK_LOCKED but not support ME lock. Not show.");
                        break;
                    }
                    break;
                case 4:
                    synchronized (this) {
                        if (!KeyguardViewMediator.this.mShowing) {
                            Log.d("KeyguardViewMediator", "PERM_DISABLED and keygaurd isn't showing.");
                            KeyguardViewMediator.this.doKeyguardLocked(null);
                            break;
                        }
                        Log.d("KeyguardViewMediator", "PERM_DISABLED, resetStateLocked toshow permanently disabled message in lockscreen.");
                        KeyguardViewMediator.this.resetStateLocked();
                        break;
                    }
                    break;
                case 7:
                    break;
                default:
                    Log.v("KeyguardViewMediator", "Ignoring state: " + simState);
                    break;
            }
            try {
                int size = KeyguardViewMediator.this.mKeyguardStateCallbacks.size();
                boolean simPinSecure = KeyguardViewMediator.this.mUpdateMonitor.isSimPinSecure();
                for (int i = 0; i < size; i++) {
                    ((IKeyguardStateCallback) KeyguardViewMediator.this.mKeyguardStateCallbacks.get(i)).onSimSecureStateChanged(simPinSecure);
                }
            } catch (RemoteException e) {
                Slog.w("KeyguardViewMediator", "Failed to call onSimSecureStateChanged", e);
            }
        }
    };
    private KeyguardUpdateMonitor mUpdateMonitor;
    ViewMediatorCallback mViewMediatorCallback = new ViewMediatorCallback() {
        public void userActivity() {
            KeyguardViewMediator.this.userActivity();
        }

        public void keyguardDone(boolean strongAuth) {
            if (!KeyguardViewMediator.this.mKeyguardDonePending) {
                KeyguardViewMediator.this.keyguardDone(true);
            }
            if (strongAuth) {
                KeyguardViewMediator.this.mUpdateMonitor.reportSuccessfulStrongAuthUnlockAttempt();
            }
        }

        public void keyguardDoneDrawing() {
            KeyguardViewMediator.this.mHandler.sendEmptyMessage(10);
        }

        public void setNeedsInput(boolean needsInput) {
            KeyguardViewMediator.this.mStatusBarKeyguardViewManager.setNeedsInput(needsInput);
        }

        public void keyguardDonePending(boolean strongAuth) {
            KeyguardViewMediator.this.mKeyguardDonePending = true;
            KeyguardViewMediator.this.mHideAnimationRun = true;
            KeyguardViewMediator.this.mStatusBarKeyguardViewManager.startPreHideAnimation(null);
            KeyguardViewMediator.this.mHandler.sendEmptyMessageDelayed(20, 3000);
            if (strongAuth) {
                KeyguardViewMediator.this.mUpdateMonitor.reportSuccessfulStrongAuthUnlockAttempt();
            }
        }

        public void keyguardGone() {
            if (KeyguardViewMediator.this.mKeyguardDisplayManager != null) {
                Log.d("KeyguardViewMediator", "keyguard gone, call mKeyguardDisplayManager.hide()");
                KeyguardViewMediator.this.mKeyguardDisplayManager.hide();
            } else {
                Log.d("KeyguardViewMediator", "keyguard gone, mKeyguardDisplayManager is null");
            }
            KeyguardViewMediator.this.mVoiceWakeupManager.notifyKeyguardIsGone();
        }

        public void readyForKeyguardDone() {
            if (KeyguardViewMediator.this.mKeyguardDonePending) {
                KeyguardViewMediator.this.keyguardDone(true);
            }
        }

        public void resetKeyguard() {
            resetStateLocked();
        }

        public void playTrustedSound() {
            KeyguardViewMediator.this.playTrustedSound();
        }

        public boolean isInputRestricted() {
            return KeyguardViewMediator.this.isInputRestricted();
        }

        public boolean isScreenOn() {
            return KeyguardViewMediator.this.mDeviceInteractive;
        }

        public int getBouncerPromptReason() {
            int currentUser = ActivityManager.getCurrentUser();
            if ((KeyguardViewMediator.this.mUpdateMonitor.getUserTrustIsManaged(currentUser) || KeyguardViewMediator.this.mUpdateMonitor.isUnlockWithFingerprintPossible(currentUser)) && !KeyguardViewMediator.this.mUpdateMonitor.getStrongAuthTracker().hasUserAuthenticatedSinceBoot()) {
                return 1;
            }
            if (KeyguardViewMediator.this.mUpdateMonitor.isUnlockWithFingerprintPossible(currentUser) && KeyguardViewMediator.this.mUpdateMonitor.hasFingerprintUnlockTimedOut(currentUser)) {
                return 2;
            }
            return 0;
        }

        public void dismiss(boolean authenticated) {
            KeyguardViewMediator.this.dismiss(authenticated);
        }

        public boolean isShowing() {
            return KeyguardViewMediator.this.isShowing();
        }

        public void showLocked(Bundle options) {
            KeyguardViewMediator.this.showLocked(options);
        }

        public void resetStateLocked() {
            KeyguardViewMediator.this.resetStateLocked();
        }

        public void adjustStatusBarLocked() {
            KeyguardViewMediator.this.adjustStatusBarLocked();
        }

        public void hideLocked() {
            KeyguardViewMediator.this.hideLocked();
        }

        public boolean isSecure() {
            return KeyguardViewMediator.this.isSecure();
        }

        public void setSuppressPlaySoundFlag() {
            KeyguardViewMediator.this.setSuppressPlaySoundFlag();
        }

        public void updateNavbarStatus() {
            KeyguardViewMediator.this.updateNavbarStatus();
        }

        public boolean isKeyguardDoneOnGoing() {
            return KeyguardViewMediator.this.isKeyguardDoneOnGoing();
        }

        public void updateAntiTheftLocked() {
            KeyguardViewMediator.this.updateAntiTheftLocked();
        }

        public boolean isKeyguardExternallyEnabled() {
            return KeyguardViewMediator.this.isKeyguardExternallyEnabled();
        }
    };
    private VoiceWakeupManager mVoiceWakeupManager;
    private IWindowManager mWM;
    private boolean mWaitingUntilKeyguardVisible = false;
    private boolean mWakeAndUnlocking;

    private class InvalidDialogCallback implements DialogShowCallBack {
        private InvalidDialogCallback() {
        }

        public void show() {
            KeyguardViewMediator.this.createDialog(KeyguardViewMediator.this.mContext.getString(R.string.invalid_sim_title), KeyguardViewMediator.this.mContext.getString(R.string.invalid_sim_message)).show();
        }
    }

    private class MeLockedDialogCallback implements DialogShowCallBack {
        private MeLockedDialogCallback() {
        }

        public void show() {
            KeyguardViewMediator.this.createDialog(null, KeyguardViewMediator.this.mContext.getString(R.string.simlock_slot_locked_message)).show();
        }
    }

    private static class StartKeyguardExitAnimParams {
        long fadeoutDuration;
        long startTime;

        private StartKeyguardExitAnimParams(long startTime, long fadeoutDuration) {
            this.startTime = startTime;
            this.fadeoutDuration = fadeoutDuration;
        }
    }

    public void userActivity() {
        this.mPM.userActivity(SystemClock.uptimeMillis(), false);
    }

    private void setupLocked() {
        boolean z;
        this.mPM = (PowerManager) this.mContext.getSystemService("power");
        this.mWM = WindowManagerGlobal.getWindowManagerService();
        this.mTrustManager = (TrustManager) this.mContext.getSystemService("trust");
        this.mShowKeyguardWakeLock = this.mPM.newWakeLock(1, "show keyguard");
        this.mShowKeyguardWakeLock.setReferenceCounted(false);
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_KEYGUARD");
        filter.addAction("android.intent.action.ACTION_PRE_SHUTDOWN");
        filter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
        filter.addAction("android.intent.action.ACTION_PREBOOT_IPO");
        filter.addAction("android.intent.action.GESTURE_U");
        filter.addAction("android.intent.action.GESTURE_UP");
        filter.addAction("android.intent.action.GESTURE_DOWN");
        filter.addAction("android.intent.action.GESTURE_RIGHT");
        filter.addAction("android.intent.action.GESTURE_LEFT");
        filter.addAction("android.intent.action.GESTURE_C");
        filter.addAction("android.intent.action.GESTURE_E");
        filter.addAction("android.intent.action.GESTURE_W");
        filter.addAction("android.intent.action.GESTURE_M");
        filter.addAction("android.intent.action.GESTURE_O");
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
        this.mKeyguardDisplayManager = new KeyguardDisplayManager(this.mContext);
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        KeyguardUpdateMonitor.setCurrentUser(ActivityManager.getCurrentUser());
        if (shouldWaitForProvisioning() || this.mLockPatternUtils.isLockScreenDisabled(KeyguardUpdateMonitor.getCurrentUser())) {
            z = false;
        } else {
            z = true;
        }
        setShowingLocked(z);
        updateInputRestrictedLocked();
        this.mTrustManager.reportKeyguardShowingChanged();
        this.mStatusBarKeyguardViewManager = new StatusBarKeyguardViewManager(this.mContext, this.mViewMediatorCallback, this.mLockPatternUtils);
        ContentResolver cr = this.mContext.getContentResolver();
        this.mDeviceInteractive = this.mPM.isInteractive();
        this.mLockSounds = new SoundPool(1, 1, 0);
        String soundPath = Global.getString(cr, "lock_sound");
        if (soundPath != null) {
            this.mLockSoundId = this.mLockSounds.load(soundPath, 1);
        }
        if (soundPath == null || this.mLockSoundId == 0) {
            Log.w("KeyguardViewMediator", "failed to load lock sound from " + soundPath);
        }
        soundPath = Global.getString(cr, "unlock_sound");
        if (soundPath != null) {
            this.mUnlockSoundId = this.mLockSounds.load(soundPath, 1);
        }
        if (soundPath == null || this.mUnlockSoundId == 0) {
            Log.w("KeyguardViewMediator", "failed to load unlock sound from " + soundPath);
        }
        soundPath = Global.getString(cr, "trusted_sound");
        if (soundPath != null) {
            this.mTrustedSoundId = this.mLockSounds.load(soundPath, 1);
        }
        if (soundPath == null || this.mTrustedSoundId == 0) {
            Log.w("KeyguardViewMediator", "failed to load trusted sound from " + soundPath);
        }
        this.mLockSoundVolume = (float) Math.pow(10.0d, (double) (((float) this.mContext.getResources().getInteger(17694725)) / 20.0f));
        this.mHideAnimation = AnimationUtils.loadAnimation(this.mContext, 17432626);
        this.mDialogManager = KeyguardDialogManager.getInstance(this.mContext);
        this.mAntiTheftManager = AntiTheftManager.getInstance(this.mContext, this.mViewMediatorCallback, this.mLockPatternUtils);
        this.mAntiTheftManager.doAntiTheftLockCheck();
        this.mPowerOffAlarmManager = PowerOffAlarmManager.getInstance(this.mContext, this.mViewMediatorCallback, this.mLockPatternUtils);
        this.mVoiceWakeupManager = VoiceWakeupManager.getInstance();
        this.mVoiceWakeupManager.init(this.mContext, this.mViewMediatorCallback);
    }

    public void start() {
        synchronized (this) {
            setupLocked();
        }
        putComponent(KeyguardViewMediator.class, this);
    }

    public void onSystemReady() {
        this.mSearchManager = (SearchManager) this.mContext.getSystemService("search");
        synchronized (this) {
            Log.d("KeyguardViewMediator", "onSystemReady");
            AntiTheftManager.checkPplStatus();
            this.mSystemReady = true;
            doKeyguardLocked(null);
            this.mUpdateMonitor.registerCallback(this.mUpdateCallback);
            this.mPowerOffAlarmManager.onSystemReady();
        }
        maybeSendUserPresentBroadcast();
    }

    public void onStartedGoingToSleep(int why) {
        Log.d("KeyguardViewMediator", "onStartedGoingToSleep(" + why + ")");
        synchronized (this) {
            this.mDeviceInteractive = false;
            this.mGoingToSleep = true;
            int currentUser = KeyguardUpdateMonitor.getCurrentUser();
            boolean lockImmediately = !this.mLockPatternUtils.getPowerButtonInstantlyLocks(currentUser) ? !this.mLockPatternUtils.isSecure(currentUser) : true;
            long timeout = getLockTimeout();
            boolean lockWhenTimeout = KeyguardPluginFactory.getKeyguardUtilExt(this.mContext).lockImmediatelyWhenScreenTimeout();
            Log.d("KeyguardViewMediator", "onStartedGoingToSleep(" + why + ") ---ScreenOff mScreenOn = false; After--boolean lockImmediately=" + lockImmediately + ", mExitSecureCallback=" + this.mExitSecureCallback + ", mShowing=" + this.mShowing + ", mIsIPOShutDown = " + this.mIsIPOShutDown);
            if (this.mExitSecureCallback != null) {
                Log.d("KeyguardViewMediator", "pending exit secure callback cancelled");
                try {
                    this.mExitSecureCallback.onKeyguardExitResult(false);
                } catch (RemoteException e) {
                    Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(false)", e);
                }
                this.mExitSecureCallback = null;
                if (!this.mExternallyEnabled) {
                    hideLocked();
                }
            } else if (this.mShowing) {
                this.mPendingReset = true;
            } else if ((why == 3 && timeout > 0 && !lockWhenTimeout) || (why == 2 && !lockImmediately && !this.mIsIPOShutDown)) {
                doKeyguardLaterLocked(timeout);
            } else if (why == 4) {
                Log.d("KeyguardViewMediator", "Screen off because PROX_SENSOR, do not draw lock view.");
            } else if (!this.mLockPatternUtils.isLockScreenDisabled(currentUser)) {
                this.mPendingLock = true;
            }
            if (this.mPendingLock) {
                playSounds(true);
            }
        }
        KeyguardUpdateMonitor.getInstance(this.mContext).dispatchStartedGoingToSleep(why);
    }

    public void onFinishedGoingToSleep(int why) {
        Log.d("KeyguardViewMediator", "onFinishedGoingToSleep(" + why + ")");
        synchronized (this) {
            this.mDeviceInteractive = false;
            this.mGoingToSleep = false;
            resetKeyguardDonePendingLocked();
            this.mHideAnimationRun = false;
            notifyFinishedGoingToSleep();
            if (this.mPendingReset) {
                resetStateLocked();
                this.mPendingReset = false;
            }
            if (this.mPendingLock) {
                doKeyguardLocked(null);
                this.mPendingLock = false;
            }
        }
        KeyguardUpdateMonitor.getInstance(this.mContext).dispatchFinishedGoingToSleep(why);
    }

    private long getLockTimeout() {
        ContentResolver cr = this.mContext.getContentResolver();
        long displayTimeout = (long) System.getInt(cr, "screen_off_timeout", 30000);
        long lockAfterTimeout = (long) Secure.getInt(cr, "lock_screen_lock_after_timeout", 5000);
        long policyTimeout = this.mLockPatternUtils.getDevicePolicyManager().getMaximumTimeToLock(null, KeyguardUpdateMonitor.getCurrentUser());
        if (policyTimeout > 0) {
            return Math.min(policyTimeout - Math.max(displayTimeout, 0), lockAfterTimeout);
        }
        return lockAfterTimeout;
    }

    private void doKeyguardLaterLocked() {
        long timeout = getLockTimeout();
        if (timeout == 0) {
            doKeyguardLocked(null);
        } else {
            doKeyguardLaterLocked(timeout);
        }
    }

    private void doKeyguardLaterLocked(long timeout) {
        long when = SystemClock.elapsedRealtime() + timeout;
        Intent intent = new Intent("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_KEYGUARD");
        intent.putExtra("seq", this.mDelayedShowingSequence);
        this.mAlarmManager.set(2, when, PendingIntent.getBroadcast(this.mContext, 0, intent, 268435456));
        Log.d("KeyguardViewMediator", "setting alarm to turn off keyguard, seq = " + this.mDelayedShowingSequence);
    }

    private void cancelDoKeyguardLaterLocked() {
        this.mDelayedShowingSequence++;
    }

    public void onStartedWakingUp() {
        synchronized (this) {
            this.mDeviceInteractive = true;
            cancelDoKeyguardLaterLocked();
            Log.d("KeyguardViewMediator", "onStartedWakingUp, seq = " + this.mDelayedShowingSequence);
            notifyStartedWakingUp();
        }
        KeyguardUpdateMonitor.getInstance(this.mContext).dispatchStartedWakingUp();
        maybeSendUserPresentBroadcast();
    }

    public void onScreenTurningOn(IKeyguardDrawnCallback callback) {
        notifyScreenOn(callback);
    }

    public void onScreenTurnedOn() {
        notifyScreenTurnedOn();
        this.mUpdateMonitor.dispatchScreenTurnedOn();
    }

    public void onScreenTurnedOff() {
        notifyScreenTurnedOff();
        this.mUpdateMonitor.dispatchScreenTurnedOff();
    }

    private void maybeSendUserPresentBroadcast() {
        if (this.mSystemReady && this.mLockPatternUtils.isLockScreenDisabled(KeyguardUpdateMonitor.getCurrentUser())) {
            sendUserPresentBroadcast();
        }
    }

    public void onDreamingStarted() {
        synchronized (this) {
            if (this.mDeviceInteractive && this.mLockPatternUtils.isSecure(KeyguardUpdateMonitor.getCurrentUser())) {
                doKeyguardLaterLocked();
            }
        }
    }

    public void onDreamingStopped() {
        synchronized (this) {
            if (this.mDeviceInteractive) {
                cancelDoKeyguardLaterLocked();
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setKeyguardEnabled(boolean enabled) {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "setKeyguardEnabled(" + enabled + ")");
            this.mExternallyEnabled = enabled;
            if (enabled || !this.mShowing) {
                if (enabled) {
                    if (this.mNeedToReshowWhenReenabled) {
                        Log.d("KeyguardViewMediator", "previously hidden, reshowing, reenabling status bar expansion");
                        this.mNeedToReshowWhenReenabled = false;
                        updateInputRestrictedLocked();
                        if (this.mExitSecureCallback != null) {
                            Log.d("KeyguardViewMediator", "onKeyguardExitResult(false), resetting");
                            try {
                                this.mExitSecureCallback.onKeyguardExitResult(false);
                            } catch (RemoteException e) {
                                Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(false)", e);
                            }
                            this.mExitSecureCallback = null;
                            resetStateLocked();
                        } else {
                            showLocked(null);
                            this.mWaitingUntilKeyguardVisible = true;
                            this.mHandler.sendEmptyMessageDelayed(10, 2000);
                            Log.d("KeyguardViewMediator", "waiting until mWaitingUntilKeyguardVisible is false");
                            while (this.mWaitingUntilKeyguardVisible) {
                                try {
                                    wait();
                                } catch (InterruptedException e2) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                            Log.d("KeyguardViewMediator", "done waiting for mWaitingUntilKeyguardVisible");
                        }
                    }
                }
            } else if (this.mExitSecureCallback != null) {
                Log.d("KeyguardViewMediator", "in process of verifyUnlock request, ignoring");
            } else {
                Log.d("KeyguardViewMediator", "remembering to reshow, hiding keyguard, disabling status bar expansion");
                this.mNeedToReshowWhenReenabled = true;
                updateInputRestrictedLocked();
                hideLocked();
            }
        }
    }

    public void verifyUnlock(IKeyguardExitCallback callback) {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "verifyUnlock");
            if (shouldWaitForProvisioning()) {
                Log.d("KeyguardViewMediator", "ignoring because device isn't provisioned");
                try {
                    callback.onKeyguardExitResult(false);
                } catch (RemoteException e) {
                    Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(false)", e);
                }
            } else if (this.mExternallyEnabled) {
                Log.w("KeyguardViewMediator", "verifyUnlock called when not externally disabled");
                try {
                    callback.onKeyguardExitResult(false);
                } catch (RemoteException e2) {
                    Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(false)", e2);
                }
            } else if (this.mExitSecureCallback != null) {
                try {
                    callback.onKeyguardExitResult(false);
                } catch (RemoteException e22) {
                    Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(false)", e22);
                }
            } else {
                this.mExitSecureCallback = callback;
                verifyUnlockLocked();
            }
        }
    }

    public boolean isShowingAndNotOccluded() {
        return this.mShowing && !this.mOccluded;
    }

    public void setOccluded(boolean isOccluded) {
        Log.d("KeyguardViewMediator", "setOccluded " + isOccluded);
        if (this.mOccluded != isOccluded) {
            int i;
            Log.d("KeyguardViewMediator", "setOccluded, mOccluded=" + this.mOccluded + ", isOccluded=" + isOccluded);
            this.mOccluded = isOccluded;
            this.mHandler.removeMessages(12);
            Handler handler = this.mHandler;
            if (isOccluded) {
                i = 1;
            } else {
                i = 0;
            }
            this.mHandler.sendMessage(handler.obtainMessage(12, i, 0));
        }
    }

    private void handleSetOccluded(boolean isOccluded) {
        synchronized (this) {
            if (this.mHiding && isOccluded) {
                startKeyguardExitAnimation(0, 0);
            }
            this.mStatusBarKeyguardViewManager.setOccluded(isOccluded);
            updateActivityLockScreenState();
            adjustStatusBarLocked();
        }
    }

    public void doKeyguardTimeout(Bundle options) {
        this.mHandler.removeMessages(13);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(13, options));
    }

    public boolean isInputRestricted() {
        if (!sIsUserBuild) {
            Log.d("KeyguardViewMediator", "isInputRestricted: showing=" + this.mShowing + ", needReshow=" + this.mNeedToReshowWhenReenabled);
        }
        return !this.mShowing ? this.mNeedToReshowWhenReenabled : true;
    }

    private void updateInputRestricted() {
        synchronized (this) {
            updateInputRestrictedLocked();
            Log.d("KeyguardViewMediator", "isInputRestricted: showing=" + this.mShowing + ", needReshow=" + this.mNeedToReshowWhenReenabled + ", provisioned=" + this.mUpdateMonitor.isDeviceProvisioned());
        }
    }

    private void updateInputRestrictedLocked() {
        boolean inputRestricted = isInputRestricted();
        if (this.mInputRestricted != inputRestricted) {
            this.mInputRestricted = inputRestricted;
            for (int i = this.mKeyguardStateCallbacks.size() - 1; i >= 0; i--) {
                try {
                    ((IKeyguardStateCallback) this.mKeyguardStateCallbacks.get(i)).onInputRestrictedStateChanged(inputRestricted);
                } catch (RemoteException e) {
                    Slog.w("KeyguardViewMediator", "Failed to call onDeviceProvisioned", e);
                    if (e instanceof DeadObjectException) {
                        this.mKeyguardStateCallbacks.remove(i);
                    }
                }
            }
        }
    }

    private void doKeyguardLocked(Bundle options) {
        if (!this.mExternallyEnabled || PowerOffAlarmManager.isAlarmBoot()) {
            Log.d("KeyguardViewMediator", "doKeyguard: not showing because externally disabled");
            Log.d("KeyguardViewMediator", "doKeyguard : externally disabled reason..mExternallyEnabled = " + this.mExternallyEnabled);
            Log.d("KeyguardViewMediator", "doKeyguard : externally disabled reason..PowerOffAlarmManager.isAlarmBoot() = " + PowerOffAlarmManager.isAlarmBoot());
        } else if (this.mStatusBarKeyguardViewManager.isShowing()) {
            Log.d("KeyguardViewMediator", "doKeyguard: not showing because it is already showing");
            resetStateLocked();
            Log.d("KeyguardViewMediator", "doKeyguard: not showing because it is already showing");
        } else {
            Log.d("KeyguardViewMediator", "doKeyguard: get keyguard.no_require_sim property before");
            boolean requireSim = !SystemProperties.getBoolean("keyguard.no_require_sim", true);
            Log.d("KeyguardViewMediator", "doKeyguard: get requireSim=" + requireSim);
            boolean provisioned = this.mUpdateMonitor.isDeviceProvisioned();
            boolean lockedOrMissing = false;
            for (int i = 0; i < KeyguardUtils.getNumOfPhone(); i++) {
                if (isSimLockedOrMissing(i, requireSim)) {
                    lockedOrMissing = true;
                    break;
                }
            }
            boolean antiTheftLocked = AntiTheftManager.isAntiTheftLocked();
            Log.d("KeyguardViewMediator", "lockedOrMissing is " + lockedOrMissing + ", requireSim=" + requireSim + ", provisioned=" + provisioned + ", antiTheftLocked=" + antiTheftLocked);
            if (!lockedOrMissing && shouldWaitForProvisioning() && !antiTheftLocked) {
                Log.d("KeyguardViewMediator", "doKeyguard: not showing because device isn't provisioned and the sim is not locked or missing");
            } else if (this.mLockPatternUtils.isLockScreenDisabled(KeyguardUpdateMonitor.getCurrentUser()) && !lockedOrMissing && !antiTheftLocked) {
                Log.d("KeyguardViewMediator", "doKeyguard: not showing because lockscreen is off");
            } else if (this.mLockPatternUtils.checkVoldPassword(KeyguardUpdateMonitor.getCurrentUser()) && isSystemEncrypted()) {
                Log.d("KeyguardViewMediator", "Not showing lock screen since just decrypted");
                setShowingLocked(false);
                hideLocked();
                this.mUpdateMonitor.reportSuccessfulStrongAuthUnlockAttempt();
            } else {
                Log.d("KeyguardViewMediator", "doKeyguard: showing the lock screen");
                showLocked(options);
            }
        }
    }

    private boolean shouldWaitForProvisioning() {
        return (this.mUpdateMonitor.isDeviceProvisioned() || isSecure()) ? false : true;
    }

    private boolean isSimLockedOrMissing(int phoneId, boolean requireSim) {
        State state = this.mUpdateMonitor.getSimStateOfPhoneId(phoneId);
        if (this.mUpdateMonitor.isSimPinSecure(phoneId)) {
            return true;
        }
        if (state == State.ABSENT || state == State.PERM_DISABLED) {
            return requireSim;
        }
        return false;
    }

    public void handleDismiss(boolean authenticated) {
        if (this.mShowing && !this.mOccluded) {
            this.mStatusBarKeyguardViewManager.dismiss(authenticated);
        }
    }

    public void dismiss() {
        dismiss(false);
    }

    public void dismiss(boolean authenticated) {
        Log.d("KeyguardViewMediator", "dismiss, authenticated = " + authenticated);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(17, new Boolean(authenticated)));
    }

    private void resetStateLocked() {
        Log.e("KeyguardViewMediator", "resetStateLocked");
        this.mHandler.sendMessage(this.mHandler.obtainMessage(4));
    }

    private void verifyUnlockLocked() {
        Log.d("KeyguardViewMediator", "verifyUnlockLocked");
        this.mHandler.sendEmptyMessage(5);
    }

    private void notifyFinishedGoingToSleep() {
        Log.d("KeyguardViewMediator", "notifyFinishedGoingToSleep");
        this.mHandler.sendEmptyMessage(6);
    }

    private void notifyStartedWakingUp() {
        Log.d("KeyguardViewMediator", "notifyStartedWakingUp");
        this.mHandler.sendEmptyMessage(21);
    }

    private void notifyScreenOn(IKeyguardDrawnCallback callback) {
        Log.d("KeyguardViewMediator", "notifyScreenOn");
        this.mHandler.sendMessage(this.mHandler.obtainMessage(7, callback));
    }

    private void notifyScreenTurnedOn() {
        Log.d("KeyguardViewMediator", "notifyScreenTurnedOn");
        this.mHandler.sendMessage(this.mHandler.obtainMessage(22));
    }

    private void notifyScreenTurnedOff() {
        Log.d("KeyguardViewMediator", "notifyScreenTurnedOff");
        this.mHandler.sendMessage(this.mHandler.obtainMessage(23));
    }

    private void showLocked(Bundle options) {
        Log.d("KeyguardViewMediator", "showLocked");
        setReadyToShow(true);
        updateActivityLockScreenState();
        this.mShowKeyguardWakeLock.acquire();
        this.mHandler.sendMessage(this.mHandler.obtainMessage(2, options));
    }

    private void hideLocked() {
        Log.d("KeyguardViewMediator", "hideLocked");
        this.mHandler.sendMessage(this.mHandler.obtainMessage(3));
    }

    public boolean isSecure() {
        if (this.mLockPatternUtils.isSecure(KeyguardUpdateMonitor.getCurrentUser()) || KeyguardUpdateMonitor.getInstance(this.mContext).isSimPinSecure()) {
            return true;
        }
        return AntiTheftManager.isAntiTheftLocked();
    }

    public void setCurrentUser(int newUserId) {
        KeyguardUpdateMonitor.setCurrentUser(newUserId);
    }

    private ComponentName getGestureAppName(int gesture) {
        String packageName = null;
        String activityName = null;
        String defaultAppName;
        switch (gesture) {
            case 0:
                defaultAppName = SystemProperties.get("persist.sys.smartwake_c_name", "com.mediatek.camera&com.android.camera.CameraLauncher");
                packageName = defaultAppName.split("&")[0];
                activityName = defaultAppName.split("&")[1];
                break;
            case 1:
                defaultAppName = SystemProperties.get("persist.sys.smartwake_e_name", "com.android.browser&com.android.browser.BrowserActivity");
                packageName = defaultAppName.split("&")[0];
                activityName = defaultAppName.split("&")[1];
                break;
            case 2:
                defaultAppName = SystemProperties.get("persist.sys.smartwake_w_name", "com.mediatek.filemanager&com.mediatek.filemanager.FileManagerOperationActivity");
                packageName = defaultAppName.split("&")[0];
                activityName = defaultAppName.split("&")[1];
                break;
            case 3:
                if (isApkExist(this.mContext, "com.android.music")) {
                    defaultAppName = SystemProperties.get("persist.sys.smartwake_m_name", "com.android.music&com.android.music.MusicBrowserActivity");
                } else {
                    defaultAppName = SystemProperties.get("persist.sys.smartwake_m_name", "com.v5music&com.v5music.view.activity.SplashActivity");
                }
                packageName = defaultAppName.split("&")[0];
                activityName = defaultAppName.split("&")[1];
                break;
            case 4:
                defaultAppName = SystemProperties.get("persist.sys.smartwake_o_name", "com.android.dialer&com.android.dialer.DialtactsActivity");
                packageName = defaultAppName.split("&")[0];
                activityName = defaultAppName.split("&")[1];
                break;
        }
        return new ComponentName(packageName, activityName);
    }

    private void launchGestureApp(ComponentName componetName) {
        Intent intent = new Intent();
        intent.setComponent(componetName);
        intent.setAction("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        intent.setFlags(2097152);
        intent.addFlags(268435456);
        this.mContext.startActivity(intent);
    }

    private boolean isApkExist(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 1);
            Log.d("tanglei", "isApkExist = true");
            return true;
        } catch (NameNotFoundException e) {
            Log.w("tanglei", "Apk not found " + packageName);
            return false;
        }
    }

    public void keyguardDone(boolean authenticated) {
        Log.d("KeyguardViewMediator", "keyguardDone(" + authenticated + ")");
        EventLog.writeEvent(70000, 2);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(9, Integer.valueOf(authenticated ? 1 : 0)));
    }

    private void handleKeyguardDone(boolean authenticated) {
        Log.d("KeyguardViewMediator", "handleKeyguardDone, authenticated=" + authenticated);
        synchronized (this) {
            resetKeyguardDonePendingLocked();
        }
        if (AntiTheftManager.isAntiTheftLocked()) {
            Log.d("KeyguardViewMediator", "handleKeyguardDone() - Skip keyguard done! antitheft = " + AntiTheftManager.isAntiTheftLocked() + " or sim = " + this.mUpdateMonitor.isSimPinSecure());
            return;
        }
        Log.d("KeyguardViewMediator", "set mKeyguardDoneOnGoing = true");
        mKeyguardDoneOnGoing = true;
        if (authenticated) {
            this.mUpdateMonitor.clearFailedUnlockAttempts();
        }
        this.mUpdateMonitor.clearFingerprintRecognized();
        if (this.mGoingToSleep) {
            Log.i("KeyguardViewMediator", "Device is going to sleep, aborting keyguardDone");
            return;
        }
        if (this.mExitSecureCallback != null) {
            try {
                this.mExitSecureCallback.onKeyguardExitResult(authenticated);
            } catch (RemoteException e) {
                Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(" + authenticated + ")", e);
            }
            this.mExitSecureCallback = null;
            if (authenticated) {
                this.mExternallyEnabled = true;
                this.mNeedToReshowWhenReenabled = false;
                updateInputRestricted();
            }
        }
        this.mSuppressNextLockSound = false;
        handleHide();
    }

    private void sendUserPresentBroadcast() {
        synchronized (this) {
            if (this.mBootCompleted) {
                for (UserInfo ui : ((UserManager) this.mContext.getSystemService(FeatureOptionUtils.BUILD_TYPE_USER)).getProfiles(new UserHandle(KeyguardUpdateMonitor.getCurrentUser()).getIdentifier())) {
                    this.mContext.sendBroadcastAsUser(USER_PRESENT_INTENT, ui.getUserHandle());
                }
            } else {
                this.mBootSendUserPresent = true;
            }
        }
    }

    private void handleKeyguardDoneDrawing() {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleKeyguardDoneDrawing");
            if (this.mWaitingUntilKeyguardVisible) {
                Log.d("KeyguardViewMediator", "handleKeyguardDoneDrawing: notifying mWaitingUntilKeyguardVisible");
                this.mWaitingUntilKeyguardVisible = false;
                notifyAll();
                this.mHandler.removeMessages(10);
            }
        }
    }

    private void playSounds(boolean locked) {
        Log.d("KeyguardViewMediator", "playSounds(locked = " + locked + "), mSuppressNextLockSound =" + this.mSuppressNextLockSound);
        if (this.mSuppressNextLockSound) {
            this.mSuppressNextLockSound = false;
            return;
        }
        int i;
        if (locked) {
            i = this.mLockSoundId;
        } else {
            i = this.mUnlockSoundId;
        }
        playSound(i);
    }

    private void playSound(int soundId) {
        if (soundId != 0 && System.getInt(this.mContext.getContentResolver(), "lockscreen_sounds_enabled", 1) == 1) {
            this.mLockSounds.stop(this.mLockSoundStreamId);
            if (this.mAudioManager == null) {
                this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
                if (this.mAudioManager != null) {
                    this.mUiSoundsStreamType = this.mAudioManager.getUiSoundsStreamType();
                } else {
                    return;
                }
            }
            if (!this.mAudioManager.isStreamMute(this.mUiSoundsStreamType)) {
                this.mLockSoundStreamId = this.mLockSounds.play(soundId, this.mLockSoundVolume, this.mLockSoundVolume, 1, 0, 1.0f);
            }
        }
    }

    private void playTrustedSound() {
        if (!this.mSuppressNextLockSound) {
            playSound(this.mTrustedSoundId);
        }
    }

    private void setReadyToShow(boolean readyToShow) {
        this.mReadyToShow = readyToShow;
        Log.d("KeyguardViewMediator", "mReadyToShow set as " + this.mReadyToShow);
    }

    private void updateActivityLockScreenState() {
        boolean z = false;
        try {
            boolean z2;
            String str = "KeyguardViewMediator";
            StringBuilder append = new StringBuilder().append("updateActivityLockScreenState() - mShowing = ").append(this.mShowing).append(" mReadyToShow = ").append(this.mReadyToShow).append(" !mOccluded = ");
            if (this.mOccluded) {
                z2 = false;
            } else {
                z2 = true;
            }
            Log.d(str, append.append(z2).toString());
            IActivityManager iActivityManager = ActivityManagerNative.getDefault();
            if ((this.mShowing || this.mReadyToShow) && !this.mOccluded) {
                z = true;
            }
            iActivityManager.setLockScreenShown(z);
        } catch (RemoteException e) {
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleShow(Bundle options) {
        synchronized (this) {
            if (this.mSystemReady) {
                Log.d("KeyguardViewMediator", "handleShow");
                setShowingLocked(true);
                this.mStatusBarKeyguardViewManager.show(options);
                this.mHiding = false;
                this.mWakeAndUnlocking = false;
                resetKeyguardDonePendingLocked();
                setReadyToShow(false);
                this.mHideAnimationRun = false;
                updateActivityLockScreenState();
                adjustStatusBarLocked();
                userActivity();
                this.mHandler.postDelayed(new Runnable() {
                    public void run() {
                        try {
                            ActivityManagerNative.getDefault().closeSystemDialogs("lock");
                        } catch (RemoteException e) {
                            Log.e("KeyguardViewMediator", "handleShow() - error in closeSystemDialogs()");
                        }
                    }
                }, 500);
                if (PowerOffAlarmManager.isAlarmBoot()) {
                    this.mPowerOffAlarmManager.startAlarm();
                }
                this.mShowKeyguardWakeLock.release();
                Log.d("KeyguardViewMediator", "handleShow exit");
            } else {
                Log.d("KeyguardViewMediator", "ignoring handleShow because system is not ready.");
                setReadyToShow(false);
                updateActivityLockScreenState();
            }
        }
    }

    private void handleHide() {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleHide");
            this.mHiding = true;
            if (!this.mShowing || this.mOccluded) {
                handleStartKeyguardExitAnimation(SystemClock.uptimeMillis() + this.mHideAnimation.getStartOffset(), this.mHideAnimation.getDuration());
            } else if (this.mHideAnimationRun) {
                this.mKeyguardGoingAwayRunnable.run();
            } else {
                this.mStatusBarKeyguardViewManager.startPreHideAnimation(this.mKeyguardGoingAwayRunnable);
            }
        }
    }

    private void handleOnActivityDrawn() {
        Log.d("KeyguardViewMediator", "handleOnActivityDrawn: mKeyguardDonePending=" + this.mKeyguardDonePending);
        if (this.mKeyguardDonePending) {
            this.mStatusBarKeyguardViewManager.onActivityDrawn();
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleStartKeyguardExitAnimation(long startTime, long fadeoutDuration) {
        boolean z = false;
        Log.d("KeyguardViewMediator", "handleStartKeyguardExitAnimation() is called.");
        synchronized (this) {
            if (this.mHiding) {
                this.mHiding = false;
                if (TelephonyManager.EXTRA_STATE_IDLE.equals(this.mPhoneState) && this.mShowing) {
                    playSounds(false);
                }
                setShowingLocked(false);
                this.mStatusBarKeyguardViewManager.hide(startTime, fadeoutDuration);
                resetKeyguardDonePendingLocked();
                this.mHideAnimationRun = false;
                updateActivityLockScreenState();
                adjustStatusBarLocked();
                sendUserPresentBroadcast();
                if (this.mWakeAndUnlocking && this.mDrawnCallback != null) {
                    notifyDrawn(this.mDrawnCallback);
                }
            } else {
                String str = "KeyguardViewMediator";
                StringBuilder append = new StringBuilder().append("handleStartKeyguardExitAnimation() - returns, !mHiding = ");
                if (!this.mHiding) {
                    z = true;
                }
                Log.d(str, append.append(z).toString());
            }
        }
    }

    void adjustStatusBarLocked() {
        if (this.mStatusBarManager == null) {
            this.mStatusBarManager = (StatusBarManager) this.mContext.getSystemService("statusbar");
        }
        if (this.mStatusBarManager == null) {
            Log.w("KeyguardViewMediator", "Could not get status bar manager");
            return;
        }
        boolean isSecure = isSecure();
        int flags = 0;
        if (this.mShowing) {
            flags = 16777216;
            if (PowerOffAlarmManager.isAlarmBoot()) {
                flags = 16777216 | 33554432;
            }
        }
        if (isShowingAndNotOccluded()) {
            flags |= 2097152;
        }
        Log.d("KeyguardViewMediator", "adjustStatusBarLocked: mShowing=" + this.mShowing + " mOccluded=" + this.mOccluded + " isSecure=" + isSecure() + " --> flags=0x" + Integer.toHexString(flags));
        if (!(this.mContext instanceof Activity)) {
            this.mStatusBarManager.disable(flags);
        }
    }

    private void handleReset() {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleReset");
            this.mStatusBarKeyguardViewManager.reset();
            adjustStatusBarLocked();
        }
    }

    private void handleVerifyUnlock() {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleVerifyUnlock");
            setShowingLocked(true);
            this.mStatusBarKeyguardViewManager.verifyUnlock();
            updateActivityLockScreenState();
        }
    }

    private void handleNotifyFinishedGoingToSleep() {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleNotifyFinishedGoingToSleep");
            this.mStatusBarKeyguardViewManager.onFinishedGoingToSleep();
        }
    }

    private void handleNotifyStartedWakingUp() {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleNotifyWakingUp");
            this.mStatusBarKeyguardViewManager.onStartedWakingUp();
        }
    }

    private void handleNotifyScreenTurningOn(IKeyguardDrawnCallback callback) {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleNotifyScreenTurningOn");
            this.mStatusBarKeyguardViewManager.onScreenTurningOn();
            if (callback != null) {
                if (this.mWakeAndUnlocking) {
                    this.mDrawnCallback = callback;
                } else {
                    notifyDrawn(callback);
                }
            }
        }
    }

    private void handleNotifyScreenTurnedOn() {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleNotifyScreenTurnedOn");
            this.mStatusBarKeyguardViewManager.onScreenTurnedOn();
        }
    }

    private void handleNotifyScreenTurnedOff() {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleNotifyScreenTurnedOff");
            this.mStatusBarKeyguardViewManager.onScreenTurnedOff();
            this.mWakeAndUnlocking = false;
        }
    }

    private void notifyDrawn(IKeyguardDrawnCallback callback) {
        try {
            callback.onDrawn();
        } catch (RemoteException e) {
            Slog.w("KeyguardViewMediator", "Exception calling onDrawn():", e);
        }
    }

    private void resetKeyguardDonePendingLocked() {
        this.mKeyguardDonePending = false;
        this.mHandler.removeMessages(20);
    }

    public void onBootCompleted() {
        Log.d("KeyguardViewMediator", "onBootCompleted() is called");
        this.mUpdateMonitor.dispatchBootCompleted();
        synchronized (this) {
            this.mBootCompleted = true;
            if (this.mBootSendUserPresent) {
                sendUserPresentBroadcast();
            }
        }
    }

    public void onWakeAndUnlocking() {
        this.mWakeAndUnlocking = true;
        keyguardDone(true);
    }

    public StatusBarKeyguardViewManager registerStatusBar(PhoneStatusBar phoneStatusBar, ViewGroup container, StatusBarWindowManager statusBarWindowManager, ScrimController scrimController, FingerprintUnlockController fingerprintUnlockController) {
        this.mStatusBarKeyguardViewManager.registerStatusBar(phoneStatusBar, container, statusBarWindowManager, scrimController, fingerprintUnlockController);
        return this.mStatusBarKeyguardViewManager;
    }

    public void startKeyguardExitAnimation(long startTime, long fadeoutDuration) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(18, new StartKeyguardExitAnimParams(startTime, fadeoutDuration)));
    }

    public void onActivityDrawn() {
        this.mHandler.sendEmptyMessage(19);
    }

    public ViewMediatorCallback getViewMediatorCallback() {
        return this.mViewMediatorCallback;
    }

    private void setShowingLocked(boolean showing) {
        Log.d("KeyguardViewMediator", "setShowingLocked() - showing = " + showing + ", mShowing = " + this.mShowing);
        if (showing != this.mShowing) {
            this.mShowing = showing;
            for (int i = this.mKeyguardStateCallbacks.size() - 1; i >= 0; i--) {
                try {
                    ((IKeyguardStateCallback) this.mKeyguardStateCallbacks.get(i)).onShowingStateChanged(showing);
                } catch (RemoteException e) {
                    Slog.w("KeyguardViewMediator", "Failed to call onShowingStateChanged", e);
                    if (e instanceof DeadObjectException) {
                        this.mKeyguardStateCallbacks.remove(i);
                    }
                }
            }
            updateInputRestrictedLocked();
            this.mTrustManager.reportKeyguardShowingChanged();
        }
    }

    public void addStateMonitorCallback(IKeyguardStateCallback callback) {
        synchronized (this) {
            this.mKeyguardStateCallbacks.add(callback);
            try {
                callback.onSimSecureStateChanged(this.mUpdateMonitor.isSimPinSecure());
                callback.onShowingStateChanged(this.mShowing);
                callback.onInputRestrictedStateChanged(this.mInputRestricted);
                callback.onAntiTheftStateChanged(AntiTheftManager.isAntiTheftLocked());
            } catch (RemoteException e) {
                Slog.w("KeyguardViewMediator", "Failed to call onShowingStateChanged or onSimSecureStateChanged or onInputRestrictedStateChanged", e);
            }
        }
    }

    private void removeKeyguardDoneMsg() {
        this.mHandler.removeMessages(9);
    }

    private AlertDialog createDialog(String title, String message) {
        AlertDialog dialog = new Builder(this.mContext).setTitle(title).setIcon(17301543).setCancelable(false).setMessage(message).setNegativeButton(R.string.ok, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                KeyguardViewMediator.this.mDialogManager.reportDialogClose();
                Log.d("KeyguardViewMediator", "invalid sim card ,reportCloseDialog");
            }
        }).create();
        dialog.getWindow().setType(2003);
        return dialog;
    }

    void setSuppressPlaySoundFlag() {
        this.mSuppressNextLockSound = true;
    }

    public boolean isKeyguardDoneOnGoing() {
        return mKeyguardDoneOnGoing;
    }

    public boolean isShowing() {
        return this.mShowing;
    }

    void updateNavbarStatus() {
        Log.d("KeyguardViewMediator", "updateNavbarStatus() is called.");
        this.mStatusBarKeyguardViewManager.updateStates();
    }

    public void updateAntiTheftLocked() {
        boolean isAntiTheftLocked = AntiTheftManager.isAntiTheftLocked();
        Log.d("KeyguardViewMediator", "updateAntiTheftLocked() - isAntiTheftLocked = " + isAntiTheftLocked);
        try {
            int size = this.mKeyguardStateCallbacks.size();
            for (int i = 0; i < size; i++) {
                ((IKeyguardStateCallback) this.mKeyguardStateCallbacks.get(i)).onAntiTheftStateChanged(isAntiTheftLocked);
            }
        } catch (RemoteException e) {
            Slog.w("KeyguardViewMediator", "Failed to call onAntiTheftStateChanged", e);
        }
    }

    private boolean isSystemEncrypted() {
        boolean bRet = true;
        String state = SystemProperties.get("ro.crypto.state");
        String decrypt = SystemProperties.get("vold.decrypt");
        if ("unencrypted".equals(state)) {
            if ("".equals(decrypt)) {
                bRet = false;
            }
        } else if (!"".equals(state) && "encrypted".equals(state) && "trigger_restart_framework".equals(decrypt)) {
            bRet = false;
        }
        Log.d("KeyguardViewMediator", "ro.crypto.state=" + state + " vold.decrypt=" + decrypt + " sysEncrypted=" + bRet);
        return bRet;
    }

    public boolean isKeyguardExternallyEnabled() {
        return this.mExternallyEnabled;
    }
}
