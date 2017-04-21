package com.android.keyguard;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AlarmManager;
import android.app.IUserSwitchObserver;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.app.trust.TrustManager;
import android.app.trust.TrustManager.TrustListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.AuthenticationCallback;
import android.hardware.fingerprint.FingerprintManager.AuthenticationResult;
import android.hardware.fingerprint.FingerprintManager.LockoutResetCallback;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.IRemoteCallback;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.TelephonyManager;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import com.android.internal.telephony.IccCardConstants.CardType;
import com.android.internal.telephony.IccCardConstants.State;
import com.android.internal.widget.LockPatternUtils;
import com.android.systemui.assis.app.MAIN.EVENT;
import com.google.android.collect.Lists;
import com.mediatek.internal.telephony.ITelephonyEx.Stub;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class KeyguardUpdateMonitor implements TrustListener {
    private static /* synthetic */ int[] -com_android_internal_telephony_IccCardConstants$StateSwitchesValues;
    private static int sCurrentUser;
    private static KeyguardUpdateMonitor sInstance;
    private boolean isKeyguardBouncerShow = false;
    private AlarmManager mAlarmManager;
    private boolean mAlternateUnlockEnabled;
    private AuthenticationCallback mAuthenticationCallback = new AuthenticationCallback() {
        public void onAuthenticationFailed() {
            KeyguardUpdateMonitor.this.handleFingerprintAuthFailed();
        }

        public void onAuthenticationSucceeded(AuthenticationResult result) {
            KeyguardUpdateMonitor.this.handleFingerprintAuthenticated();
        }

        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
            KeyguardUpdateMonitor.this.handleFingerprintHelp(helpMsgId, helpString.toString());
        }

        public void onAuthenticationError(int errMsgId, CharSequence errString) {
            KeyguardUpdateMonitor.this.handleFingerprintError(errMsgId, errString.toString());
        }

        public void onAuthenticationAcquired(int acquireInfo) {
            KeyguardUpdateMonitor.this.handleFingerprintAcquired(acquireInfo);
        }
    };
    private BatteryStatus mBatteryStatus;
    private boolean mBootCompleted;
    private boolean mBouncer;
    private final BroadcastReceiver mBroadcastAllReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.app.action.NEXT_ALARM_CLOCK_CHANGED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(301);
            } else if ("android.intent.action.USER_INFO_CHANGED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(317, intent.getIntExtra("android.intent.extra.user_handle", getSendingUserId()), 0));
            } else if ("com.android.facelock.FACE_UNLOCK_STARTED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(327, 1, getSendingUserId()));
            } else if ("com.android.facelock.FACE_UNLOCK_STOPPED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(327, 0, getSendingUserId()));
            } else if ("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(309);
            }
        }
    };
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("KeyguardUpdateMonitor", "received broadcast " + action);
            if ("android.intent.action.TIME_TICK".equals(action) || "android.intent.action.TIME_SET".equals(action) || "android.intent.action.TIMEZONE_CHANGED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(301);
            } else if ("android.provider.Telephony.SPN_STRINGS_UPDATED".equals(action)) {
                subId = intent.getIntExtra("subscription", -1);
                Log.d("KeyguardUpdateMonitor", "SPN_STRINGS_UPDATED_ACTION, sub Id = " + subId);
                int phoneId = KeyguardUtils.getPhoneIdUsingSubId(subId);
                if (KeyguardUtils.isValidPhoneId(phoneId)) {
                    KeyguardUpdateMonitor.this.mTelephonyPlmn.put(Integer.valueOf(phoneId), KeyguardUpdateMonitor.this.getTelephonyPlmnFrom(intent));
                    KeyguardUpdateMonitor.this.mTelephonySpn.put(Integer.valueOf(phoneId), KeyguardUpdateMonitor.this.getTelephonySpnFrom(intent));
                    KeyguardUpdateMonitor.this.mTelephonyCsgId.put(Integer.valueOf(phoneId), KeyguardUpdateMonitor.this.getTelephonyCsgIdFrom(intent));
                    KeyguardUpdateMonitor.this.mTelephonyHnbName.put(Integer.valueOf(phoneId), KeyguardUpdateMonitor.this.getTelephonyHnbNameFrom(intent));
                    Log.d("KeyguardUpdateMonitor", "SPN_STRINGS_UPDATED_ACTION, update phoneId=" + phoneId + ", plmn=" + KeyguardUpdateMonitor.this.mTelephonyPlmn.get(Integer.valueOf(phoneId)) + ", spn=" + KeyguardUpdateMonitor.this.mTelephonySpn.get(Integer.valueOf(phoneId)) + ", csgId=" + KeyguardUpdateMonitor.this.mTelephonyCsgId.get(Integer.valueOf(phoneId)) + ", hnbName=" + KeyguardUpdateMonitor.this.mTelephonyHnbName.get(Integer.valueOf(phoneId)));
                    KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(303, Integer.valueOf(phoneId)));
                    return;
                }
                Log.d("KeyguardUpdateMonitor", "SPN_STRINGS_UPDATED_ACTION, invalid phoneId = " + phoneId);
            } else if ("android.intent.action.BATTERY_CHANGED".equals(action)) {
                int status = intent.getIntExtra("status", 1);
                int plugged = intent.getIntExtra("plugged", 0);
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(302, new BatteryStatus(status, intent.getIntExtra("level", 0), plugged, intent.getIntExtra("health", 1))));
            } else if ("android.intent.action.SIM_STATE_CHANGED".equals(action) || "mediatek.intent.action.ACTION_UNLOCK_SIM_LOCK".equals(action)) {
                String stateExtra = intent.getStringExtra("ss");
                SimData simArgs = SimData.fromIntent(intent);
                Log.v("KeyguardUpdateMonitor", "action=" + action + ", state=" + stateExtra + ", slotId=" + simArgs.phoneId + ", subId=" + simArgs.subId + ", simArgs.simState = " + simArgs.simState);
                if ("mediatek.intent.action.ACTION_UNLOCK_SIM_LOCK".equals(action)) {
                    Log.d("KeyguardUpdateMonitor", "ACTION_UNLOCK_SIM_LOCK, set sim state as UNKNOWN");
                    KeyguardUpdateMonitor.this.mSimStateOfPhoneId.put(Integer.valueOf(simArgs.phoneId), State.UNKNOWN);
                }
                KeyguardUpdateMonitor.this.proceedToHandleSimStateChanged(simArgs);
            } else if ("android.media.RINGER_MODE_CHANGED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(305, intent.getIntExtra("android.media.EXTRA_RINGER_MODE", -1), 0));
            } else if ("android.intent.action.PHONE_STATE".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(306, intent.getStringExtra("state")));
            } else if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
                KeyguardUpdateMonitor.this.dispatchBootCompleted();
            } else if ("android.intent.action.AIRPLANE_MODE".equals(action)) {
                boolean state = intent.getBooleanExtra("state", false);
                Log.d("KeyguardUpdateMonitor", "Receive ACTION_AIRPLANE_MODE_CHANGED, state = " + state);
                Message msg = new Message();
                msg.what = 1015;
                msg.obj = new Boolean(state);
                KeyguardUpdateMonitor.this.mHandler.sendMessage(msg);
            } else if ("android.intent.action.CDMA_CARD_TYPE".equals(action)) {
                Log.d("KeyguardUpdateMonitor", "Receive ACTION_CDMA_CARD_TYPE");
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(1016, Boolean.valueOf(CardType.LOCKED_CARD == ((CardType) intent.getExtra("cdma_card_type")))));
            } else if ("android.intent.action.SERVICE_STATE".equals(action)) {
                ServiceState serviceState = ServiceState.newFromBundle(intent.getExtras());
                subId = intent.getIntExtra("subscription", -1);
                Log.v("KeyguardUpdateMonitor", "action " + action + " serviceState=" + serviceState + " subId=" + subId);
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(330, subId, 0, serviceState));
            }
        }
    };
    private final ArrayList<WeakReference<KeyguardUpdateMonitorCallback>> mCallbacks = Lists.newArrayList();
    private final Context mContext;
    private boolean mDeviceInteractive;
    private boolean mDeviceProvisioned;
    private ContentObserver mDeviceProvisionedObserver;
    private DisplayClientState mDisplayClientState = new DisplayClientState();
    private SparseIntArray mFailedAttempts = new SparseIntArray();
    private int mFailedBiometricUnlockAttempts = 0;
    private boolean mFingerprintAlreadyAuthenticated;
    private CancellationSignal mFingerprintCancelSignal;
    private int mFingerprintFailedAttempts = 0;
    private int mFingerprintRunningState = 0;
    private FingerprintManager mFpm;
    private boolean mGoingToSleep;
    final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            boolean z = false;
            switch (msg.what) {
                case 301:
                    KeyguardUpdateMonitor.this.handleTimeUpdate();
                    return;
                case 302:
                    KeyguardUpdateMonitor.this.handleBatteryUpdate((BatteryStatus) msg.obj);
                    return;
                case 303:
                    KeyguardUpdateMonitor.this.handleCarrierInfoUpdate(((Integer) msg.obj).intValue());
                    return;
                case 304:
                    KeyguardUpdateMonitor.this.handleSimStateChange((SimData) msg.obj);
                    return;
                case 305:
                    KeyguardUpdateMonitor.this.handleRingerModeChange(msg.arg1);
                    return;
                case 306:
                    KeyguardUpdateMonitor.this.handlePhoneStateChanged();
                    return;
                case 308:
                    KeyguardUpdateMonitor.this.handleDeviceProvisioned();
                    return;
                case 309:
                    KeyguardUpdateMonitor.this.handleDevicePolicyManagerStateChanged();
                    return;
                case 310:
                    KeyguardUpdateMonitor.this.handleUserSwitching(msg.arg1, (IRemoteCallback) msg.obj);
                    return;
                case 312:
                    KeyguardUpdateMonitor.this.handleKeyguardReset();
                    return;
                case 313:
                    KeyguardUpdateMonitor.this.handleBootCompleted();
                    return;
                case 314:
                    KeyguardUpdateMonitor.this.handleUserSwitchComplete(msg.arg1);
                    return;
                case 317:
                    KeyguardUpdateMonitor.this.handleUserInfoChanged(msg.arg1);
                    return;
                case 318:
                    KeyguardUpdateMonitor.this.handleReportEmergencyCallAction();
                    return;
                case 319:
                    KeyguardUpdateMonitor.this.handleStartedWakingUp();
                    return;
                case 320:
                    KeyguardUpdateMonitor.this.handleFinishedGoingToSleep(msg.arg1);
                    return;
                case 321:
                    KeyguardUpdateMonitor.this.handleStartedGoingToSleep(msg.arg1);
                    return;
                case 322:
                    KeyguardUpdateMonitor.this.handleKeyguardBouncerChanged(msg.arg1);
                    return;
                case 327:
                    KeyguardUpdateMonitor keyguardUpdateMonitor = KeyguardUpdateMonitor.this;
                    if (msg.arg1 != 0) {
                        z = true;
                    }
                    keyguardUpdateMonitor.handleFaceUnlockStateChanged(z, msg.arg2);
                    return;
                case 328:
                    KeyguardUpdateMonitor.this.handleSimSubscriptionInfoChanged();
                    return;
                case 329:
                    KeyguardUpdateMonitor.this.handleAirplaneModeChanged();
                    return;
                case 330:
                    KeyguardUpdateMonitor.this.handleServiceStateChange(msg.arg1, (ServiceState) msg.obj);
                    return;
                case 331:
                    KeyguardUpdateMonitor.this.handleScreenTurnedOn();
                    return;
                case 332:
                    KeyguardUpdateMonitor.this.handleScreenTurnedOff();
                    return;
                case 1015:
                    Log.d("KeyguardUpdateMonitor", "MSG_AIRPLANE_MODE_UPDATE, msg.obj=" + ((Boolean) msg.obj));
                    KeyguardUpdateMonitor.this.handleAirPlaneModeUpdate(((Boolean) msg.obj).booleanValue());
                    return;
                case 1016:
                    KeyguardUpdateMonitor.this.handleCDMACardTypeUpdate(((Boolean) msg.obj).booleanValue());
                    return;
                default:
                    return;
            }
        }
    };
    private boolean mKeyguardIsVisible;
    private final LockoutResetCallback mLockoutResetCallback = new LockoutResetCallback() {
        public void onLockoutReset() {
            KeyguardUpdateMonitor.this.handleFingerprintLockoutReset();
        }
    };
    private boolean mNewClientRegUpdateMonitor = false;
    private int mPhoneState;
    private int mPinPukMeDismissFlag = 0;
    private int mRingMode;
    private boolean mScreenOn;
    HashMap<Integer, ServiceState> mServiceStates = new HashMap();
    private boolean mShowing = true;
    HashMap<Integer, SimData> mSimDatas = new HashMap();
    private HashMap<Integer, Integer> mSimMeCategory = new HashMap();
    private HashMap<Integer, Integer> mSimMeLeftRetryCount = new HashMap();
    private HashMap<Integer, State> mSimStateOfPhoneId = new HashMap();
    private ArraySet<Integer> mStrongAuthNotTimedOut = new ArraySet();
    private final BroadcastReceiver mStrongAuthTimeoutReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("com.android.systemui.ACTION_STRONG_AUTH_TIMEOUT".equals(intent.getAction())) {
                int userId = intent.getIntExtra("com.android.systemui.USER_ID", -1);
                KeyguardUpdateMonitor.this.mStrongAuthNotTimedOut.remove(Integer.valueOf(userId));
                KeyguardUpdateMonitor.this.notifyStrongAuthStateChanged(userId);
            }
        }
    };
    private final StrongAuthTracker mStrongAuthTracker = new StrongAuthTracker();
    private List<SubscriptionInfo> mSubscriptionInfo;
    private OnSubscriptionsChangedListener mSubscriptionListener = new OnSubscriptionsChangedListener() {
        public void onSubscriptionsChanged() {
            Log.d("KeyguardUpdateMonitor", "onSubscriptionsChanged() is called.");
            KeyguardUpdateMonitor.this.mHandler.removeMessages(328);
            KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(328);
        }
    };
    private SubscriptionManager mSubscriptionManager;
    private boolean mSwitchingUser;
    private HashMap<Integer, CharSequence> mTelephonyCsgId = new HashMap();
    private HashMap<Integer, CharSequence> mTelephonyHnbName = new HashMap();
    private HashMap<Integer, CharSequence> mTelephonyPlmn = new HashMap();
    private HashMap<Integer, CharSequence> mTelephonySpn = new HashMap();
    private TrustManager mTrustManager;
    private SparseBooleanArray mUserFaceUnlockRunning = new SparseBooleanArray();
    private SparseBooleanArray mUserFingerprintAuthenticated = new SparseBooleanArray();
    private SparseBooleanArray mUserHasTrust = new SparseBooleanArray();
    private SparseBooleanArray mUserTrustIsManaged = new SparseBooleanArray();

    public static class BatteryStatus {
        public final int health;
        public final int level;
        public final int plugged;
        public final int status;

        public BatteryStatus(int status, int level, int plugged, int health) {
            this.status = status;
            this.level = level;
            this.plugged = plugged;
            this.health = health;
        }

        public boolean isPluggedIn() {
            if (this.plugged == 1 || this.plugged == 2 || this.plugged == 4) {
                return true;
            }
            return false;
        }

        public boolean isCharged() {
            return this.status == 5 || this.level >= 100;
        }

        public boolean isBatteryLow() {
            return this.level < 16;
        }
    }

    static class DisplayClientState {
        DisplayClientState() {
        }
    }

    static class SimData {
        public int phoneId = 0;
        public int simMECategory = 0;
        public final State simState;
        public int subId;

        SimData(State state, int phoneId, int subId) {
            this.simState = state;
            this.phoneId = phoneId;
            this.subId = subId;
        }

        SimData(State state, int phoneId, int subId, int meCategory) {
            this.simState = state;
            this.phoneId = phoneId;
            this.subId = subId;
            this.simMECategory = meCategory;
        }

        static SimData fromIntent(Intent intent) {
            if ("android.intent.action.SIM_STATE_CHANGED".equals(intent.getAction()) || "mediatek.intent.action.ACTION_UNLOCK_SIM_LOCK".equals(intent.getAction())) {
                State state;
                int meCategory = 0;
                String stateExtra = intent.getStringExtra("ss");
                int phoneId = intent.getIntExtra("slot", 0);
                int subId = intent.getIntExtra("subscription", -1);
                if ("ABSENT".equals(stateExtra)) {
                    if ("PERM_DISABLED".equals(intent.getStringExtra("reason"))) {
                        state = State.PERM_DISABLED;
                    } else {
                        state = State.ABSENT;
                    }
                } else if ("READY".equals(stateExtra)) {
                    state = State.READY;
                } else if ("LOCKED".equals(stateExtra)) {
                    String lockedReason = intent.getStringExtra("reason");
                    Log.d("KeyguardUpdateMonitor", "INTENT_VALUE_ICC_LOCKED, lockedReason=" + lockedReason);
                    if ("PIN".equals(lockedReason)) {
                        state = State.PIN_REQUIRED;
                    } else if ("PUK".equals(lockedReason)) {
                        state = State.PUK_REQUIRED;
                    } else if ("NETWORK".equals(lockedReason)) {
                        meCategory = 0;
                        state = State.NETWORK_LOCKED;
                    } else if ("NETWORK_SUBSET".equals(lockedReason)) {
                        meCategory = 1;
                        state = State.NETWORK_LOCKED;
                    } else if ("SERVICE_PROVIDER".equals(lockedReason)) {
                        meCategory = 2;
                        state = State.NETWORK_LOCKED;
                    } else if ("CORPORATE".equals(lockedReason)) {
                        meCategory = 3;
                        state = State.NETWORK_LOCKED;
                    } else if ("SIM".equals(lockedReason)) {
                        meCategory = 4;
                        state = State.NETWORK_LOCKED;
                    } else {
                        state = State.UNKNOWN;
                    }
                } else if ("NETWORK".equals(stateExtra)) {
                    state = State.NETWORK_LOCKED;
                } else if ("LOADED".equals(stateExtra) || "IMSI".equals(stateExtra)) {
                    state = State.READY;
                } else if ("NOT_READY".equals(stateExtra)) {
                    state = State.NOT_READY;
                } else {
                    state = State.UNKNOWN;
                }
                return new SimData(state, phoneId, subId, meCategory);
            }
            throw new IllegalArgumentException("only handles intent ACTION_SIM_STATE_CHANGED");
        }

        public String toString() {
            return this.simState.toString();
        }
    }

    public class StrongAuthTracker extends com.android.internal.widget.LockPatternUtils.StrongAuthTracker {
        public boolean isUnlockingWithFingerprintAllowed() {
            return isFingerprintAllowedForUser(KeyguardUpdateMonitor.getCurrentUser());
        }

        public boolean hasUserAuthenticatedSinceBoot() {
            if ((getStrongAuthForUser(KeyguardUpdateMonitor.getCurrentUser()) & 1) == 0) {
                return true;
            }
            return false;
        }

        public void onStrongAuthRequiredChanged(int userId) {
            KeyguardUpdateMonitor.this.notifyStrongAuthStateChanged(userId);
        }
    }

    private class simMeStatusQueryThread extends Thread {
        SimData simArgs;

        simMeStatusQueryThread(SimData simArgs) {
            this.simArgs = simArgs;
        }

        public void run() {
            try {
                KeyguardUpdateMonitor.this.mSimMeCategory.put(Integer.valueOf(this.simArgs.phoneId), Integer.valueOf(this.simArgs.simMECategory));
                Log.d("KeyguardUpdateMonitor", "queryNetworkLock, phoneId =" + this.simArgs.phoneId + ", simMECategory =" + this.simArgs.simMECategory);
                if (this.simArgs.simMECategory >= 0 && this.simArgs.simMECategory <= 5) {
                    Bundle bundle = Stub.asInterface(ServiceManager.getService("phoneEx")).queryNetworkLock(KeyguardUtils.getSubIdUsingPhoneId(this.simArgs.phoneId), this.simArgs.simMECategory);
                    boolean query_result = bundle.getBoolean("com.mediatek.phone.QUERY_SIMME_LOCK_RESULT", false);
                    Log.d("KeyguardUpdateMonitor", "queryNetworkLock, query_result =" + query_result);
                    if (query_result) {
                        KeyguardUpdateMonitor.this.mSimMeLeftRetryCount.put(Integer.valueOf(this.simArgs.phoneId), Integer.valueOf(bundle.getInt("com.mediatek.phone.SIMME_LOCK_LEFT_COUNT", 5)));
                    } else {
                        Log.e("KeyguardUpdateMonitor", "queryIccNetworkLock result fail");
                    }
                    KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(304, this.simArgs));
                }
            } catch (Exception e) {
                Log.e("KeyguardUpdateMonitor", "queryIccNetworkLock got exception: " + e.getMessage());
            }
        }
    }

    private static /* synthetic */ int[] -getcom_android_internal_telephony_IccCardConstants$StateSwitchesValues() {
        if (-com_android_internal_telephony_IccCardConstants$StateSwitchesValues != null) {
            return -com_android_internal_telephony_IccCardConstants$StateSwitchesValues;
        }
        int[] iArr = new int[State.values().length];
        try {
            iArr[State.ABSENT.ordinal()] = 4;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[State.CARD_IO_ERROR.ordinal()] = 5;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[State.NETWORK_LOCKED.ordinal()] = 1;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[State.NOT_READY.ordinal()] = 6;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[State.PERM_DISABLED.ordinal()] = 7;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[State.PIN_REQUIRED.ordinal()] = 2;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[State.PUK_REQUIRED.ordinal()] = 3;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[State.READY.ordinal()] = 8;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[State.UNKNOWN.ordinal()] = 9;
        } catch (NoSuchFieldError e9) {
        }
        -com_android_internal_telephony_IccCardConstants$StateSwitchesValues = iArr;
        return iArr;
    }

    public void setPinPukMeDismissFlagOfPhoneId(int r1, boolean r2) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: com.android.keyguard.KeyguardUpdateMonitor.setPinPukMeDismissFlagOfPhoneId(int, boolean):void
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:59)
	at jadx.core.ProcessClass.process(ProcessClass.java:42)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:306)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 6 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.keyguard.KeyguardUpdateMonitor.setPinPukMeDismissFlagOfPhoneId(int, boolean):void");
    }

    public static synchronized void setCurrentUser(int currentUser) {
        synchronized (KeyguardUpdateMonitor.class) {
            sCurrentUser = currentUser;
        }
    }

    public static synchronized int getCurrentUser() {
        int i;
        synchronized (KeyguardUpdateMonitor.class) {
            i = sCurrentUser;
        }
        return i;
    }

    public void onTrustChanged(boolean enabled, int userId, int flags) {
        this.mUserHasTrust.put(userId, enabled);
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (cb != null) {
                cb.onTrustChanged(userId);
                if (enabled && flags != 0) {
                    cb.onTrustGrantedWithFlags(flags, userId);
                }
            }
        }
    }

    protected void handleSimSubscriptionInfoChanged() {
        int i;
        int j;
        Log.v("KeyguardUpdateMonitor", "onSubscriptionInfoChanged()");
        List<SubscriptionInfo> sil = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        if (sil != null) {
            for (SubscriptionInfo subInfo : sil) {
                Log.v("KeyguardUpdateMonitor", "SubInfo:" + subInfo);
            }
        } else {
            Log.v("KeyguardUpdateMonitor", "onSubscriptionInfoChanged: list is null");
        }
        List<SubscriptionInfo> subscriptionInfos = getSubscriptionInfo(true);
        ArrayList<SubscriptionInfo> changedSubscriptions = new ArrayList();
        Log.d("KeyguardUpdateMonitor", "handleSimSubscriptionInfoChanged() - call refreshSimState()");
        for (i = 0; i < subscriptionInfos.size(); i++) {
            SubscriptionInfo info = (SubscriptionInfo) subscriptionInfos.get(i);
            if (refreshSimState(info.getSubscriptionId(), info.getSimSlotIndex())) {
                changedSubscriptions.add(info);
            }
        }
        Log.d("KeyguardUpdateMonitor", "handleSimSubscriptionInfoChanged() - call onSimStateChangedUsingPhoneId() & onRefreshCarrierInfo().");
        for (i = 0; i < changedSubscriptions.size(); i++) {
            int subId = ((SubscriptionInfo) changedSubscriptions.get(i)).getSubscriptionId();
            int phoneId = ((SubscriptionInfo) changedSubscriptions.get(i)).getSimSlotIndex();
            Log.d("KeyguardUpdateMonitor", "handleSimSubscriptionInfoChanged() - call callbacks for subId = " + subId + " & phoneId = " + phoneId);
            for (j = 0; j < this.mCallbacks.size(); j++) {
                KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(j)).get();
                if (cb != null) {
                    cb.onSimStateChangedUsingPhoneId(phoneId, (State) this.mSimStateOfPhoneId.get(Integer.valueOf(phoneId)));
                }
            }
        }
        for (j = 0; j < this.mCallbacks.size(); j++) {
            cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(j)).get();
            if (cb != null) {
                cb.onRefreshCarrierInfo();
            }
        }
    }

    private void handleAirplaneModeChanged() {
        for (int j = 0; j < this.mCallbacks.size(); j++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(j)).get();
            if (cb != null) {
                cb.onRefreshCarrierInfo();
            }
        }
    }

    List<SubscriptionInfo> getSubscriptionInfo(boolean forceReload) {
        List<SubscriptionInfo> sil = this.mSubscriptionInfo;
        if (sil == null || forceReload || (sil != null && sil.size() == 0)) {
            Log.d("KeyguardUpdateMonitor", "getSubscriptionInfo() - call SubscriptionManager.getActiveSubscriptionInfoList()");
            sil = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        }
        if (sil == null) {
            Log.d("KeyguardUpdateMonitor", "getSubscriptionInfo() - SubMgr returns empty list.");
            this.mSubscriptionInfo = new ArrayList();
        } else {
            this.mSubscriptionInfo = sil;
        }
        Log.d("KeyguardUpdateMonitor", "getSubscriptionInfo() - mSubscriptionInfo.size = " + this.mSubscriptionInfo.size());
        return this.mSubscriptionInfo;
    }

    public void onTrustManagedChanged(boolean managed, int userId) {
        this.mUserTrustIsManaged.put(userId, managed);
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (cb != null) {
                cb.onTrustManagedChanged(userId);
            }
        }
    }

    private void onFingerprintAuthenticated(int userId) {
        this.mUserFingerprintAuthenticated.put(userId, true);
        this.mFingerprintAlreadyAuthenticated = isUnlockingWithFingerprintAllowed();
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (cb != null) {
                cb.onFingerprintAuthenticated(userId);
            }
        }
    }

    private void handleFingerprintAuthFailed() {
        this.mFingerprintFailedAttempts++;
        int i;
        KeyguardUpdateMonitorCallback cb;
        if (this.mFingerprintFailedAttempts <= 3 || this.isKeyguardBouncerShow) {
            for (i = 0; i < this.mCallbacks.size(); i++) {
                cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
                if (cb != null) {
                    cb.onFingerprintAuthFailed();
                }
            }
            KeyguardManager keyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
            PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
            if (!pm.isScreenOn()) {
                pm.wakeUp(SystemClock.uptimeMillis());
            }
            handleFingerprintHelp(-1, this.mContext.getString(R$string.fingerprint_not_recognized));
            return;
        }
        this.isKeyguardBouncerShow = true;
        this.mFingerprintFailedAttempts = 0;
        try {
            int userId = ActivityManagerNative.getDefault().getCurrentUser().id;
            for (i = 0; i < this.mCallbacks.size(); i++) {
                cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
                if (cb != null) {
                    cb.onFingerprintAuthenticated(userId);
                }
            }
        } catch (RemoteException e) {
            Log.e("KeyguardUpdateMonitor", "Failed to get current user id: ", e);
        }
    }

    private void handleFingerprintAcquired(int acquireInfo) {
        if (acquireInfo == 0) {
            for (int i = 0; i < this.mCallbacks.size(); i++) {
                KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
                if (cb != null) {
                    cb.onFingerprintAcquired();
                }
            }
        }
    }

    private void handleFingerprintAuthenticated() {
        try {
            int userId = ActivityManagerNative.getDefault().getCurrentUser().id;
            if (isFingerprintDisabled(userId)) {
                Log.d("KeyguardUpdateMonitor", "Fingerprint disabled by DPM for userId: " + userId);
                return;
            }
            onFingerprintAuthenticated(userId);
            setFingerprintRunningState(0);
        } catch (RemoteException e) {
            Log.e("KeyguardUpdateMonitor", "Failed to get current user id: ", e);
        } finally {
            setFingerprintRunningState(0);
        }
    }

    private void handleFingerprintHelp(int msgId, String helpString) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (cb != null) {
                cb.onFingerprintHelp(msgId, helpString);
            }
        }
    }

    private void handleFingerprintError(int msgId, String errString) {
        if (msgId == 5 && this.mFingerprintRunningState == 3) {
            setFingerprintRunningState(0);
            startListeningForFingerprint();
        } else {
            setFingerprintRunningState(0);
        }
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (cb != null) {
                cb.onFingerprintError(msgId, errString);
            }
        }
    }

    private void handleFingerprintLockoutReset() {
        updateFingerprintListeningState();
    }

    private void setFingerprintRunningState(int fingerprintRunningState) {
        boolean wasRunning = this.mFingerprintRunningState == 1;
        boolean isRunning = fingerprintRunningState == 1;
        this.mFingerprintRunningState = fingerprintRunningState;
        if (wasRunning != isRunning) {
            notifyFingerprintRunningStateChanged();
        }
    }

    private void notifyFingerprintRunningStateChanged() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (cb != null) {
                cb.onFingerprintRunningStateChanged(isFingerprintDetectionRunning());
            }
        }
    }

    private void handleFaceUnlockStateChanged(boolean running, int userId) {
        Log.d("KeyguardUpdateMonitor", "handleFaceUnlockStateChanged(running = " + running + " , userId = " + userId);
        this.mUserFaceUnlockRunning.put(userId, running);
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (cb != null) {
                cb.onFaceUnlockStateChanged(running, userId);
            }
        }
    }

    public boolean isFaceUnlockRunning(int userId) {
        return this.mUserFaceUnlockRunning.get(userId);
    }

    public boolean isFingerprintDetectionRunning() {
        return this.mFingerprintRunningState == 1;
    }

    private boolean isTrustDisabled(int userId) {
        return isSimPinSecure();
    }

    private boolean isFingerprintDisabled(int userId) {
        DevicePolicyManager dpm = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        if (dpm == null || (dpm.getKeyguardDisabledFeatures(null, userId) & 32) == 0) {
            return isSimPinSecure();
        }
        return true;
    }

    public boolean getUserCanSkipBouncer(int userId) {
        if (getUserHasTrust(userId)) {
            return true;
        }
        if (this.mUserFingerprintAuthenticated.get(userId)) {
            return isUnlockingWithFingerprintAllowed();
        }
        return false;
    }

    public boolean getUserHasTrust(int userId) {
        return !isTrustDisabled(userId) ? this.mUserHasTrust.get(userId) : false;
    }

    public boolean getUserTrustIsManaged(int userId) {
        return this.mUserTrustIsManaged.get(userId) && !isTrustDisabled(userId);
    }

    public boolean isUnlockingWithFingerprintAllowed() {
        if (!this.mStrongAuthTracker.isUnlockingWithFingerprintAllowed() || hasFingerprintUnlockTimedOut(sCurrentUser)) {
            return false;
        }
        return true;
    }

    public StrongAuthTracker getStrongAuthTracker() {
        return this.mStrongAuthTracker;
    }

    public boolean hasFingerprintUnlockTimedOut(int userId) {
        return !this.mStrongAuthNotTimedOut.contains(Integer.valueOf(userId));
    }

    public void reportSuccessfulStrongAuthUnlockAttempt() {
        this.mStrongAuthNotTimedOut.add(Integer.valueOf(sCurrentUser));
        scheduleStrongAuthTimeout();
        if (this.mFpm != null) {
            this.mFpm.resetTimeout(null);
        }
    }

    private void scheduleStrongAuthTimeout() {
        long when = SystemClock.elapsedRealtime() + 259200000;
        Intent intent = new Intent("com.android.systemui.ACTION_STRONG_AUTH_TIMEOUT");
        intent.putExtra("com.android.systemui.USER_ID", sCurrentUser);
        this.mAlarmManager.set(3, when, PendingIntent.getBroadcast(this.mContext, sCurrentUser, intent, 268435456));
        notifyStrongAuthStateChanged(sCurrentUser);
    }

    private void notifyStrongAuthStateChanged(int userId) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (cb != null) {
                cb.onStrongAuthStateChanged(userId);
            }
        }
    }

    private void proceedToHandleSimStateChanged(SimData simArgs) {
        if (State.NETWORK_LOCKED == simArgs.simState && KeyguardUtils.isMediatekSimMeLockSupport()) {
            new simMeStatusQueryThread(simArgs).start();
        } else {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(304, simArgs));
        }
    }

    public static KeyguardUpdateMonitor getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new KeyguardUpdateMonitor(context);
        }
        return sInstance;
    }

    protected void handleStartedWakingUp() {
        Log.d("KeyguardUpdateMonitor", "handleStartedWakingUp");
        updateFingerprintListeningState();
        int count = this.mCallbacks.size();
        for (int i = 0; i < count; i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (cb != null) {
                cb.onStartedWakingUp();
            }
        }
    }

    protected void handleStartedGoingToSleep(int arg1) {
        clearFingerprintRecognized();
        int count = this.mCallbacks.size();
        for (int i = 0; i < count; i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (cb != null) {
                cb.onStartedGoingToSleep(arg1);
            }
        }
        this.mGoingToSleep = true;
        this.mFingerprintAlreadyAuthenticated = false;
        updateFingerprintListeningState();
    }

    protected void handleFinishedGoingToSleep(int arg1) {
        this.mGoingToSleep = false;
        int count = this.mCallbacks.size();
        for (int i = 0; i < count; i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (cb != null) {
                cb.onFinishedGoingToSleep(arg1);
            }
        }
        this.mFingerprintAlreadyAuthenticated = false;
        updateFingerprintListeningState();
    }

    private void handleScreenTurnedOn() {
        int count = this.mCallbacks.size();
        for (int i = 0; i < count; i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (cb != null) {
                cb.onScreenTurnedOn();
            }
        }
    }

    private void handleScreenTurnedOff() {
        this.mFingerprintFailedAttempts = 0;
        int count = this.mCallbacks.size();
        for (int i = 0; i < count; i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (cb != null) {
                cb.onScreenTurnedOff();
            }
        }
    }

    private void handleUserInfoChanged(int userId) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (cb != null) {
                cb.onUserInfoChanged(userId);
            }
        }
    }

    private KeyguardUpdateMonitor(Context context) {
        this.mContext = context;
        this.mSubscriptionManager = SubscriptionManager.from(context);
        this.mAlarmManager = (AlarmManager) context.getSystemService(AlarmManager.class);
        this.mDeviceProvisioned = isDeviceProvisionedInSettingsDb();
        Log.d("KeyguardUpdateMonitor", "mDeviceProvisioned is:" + this.mDeviceProvisioned);
        if (!this.mDeviceProvisioned) {
            watchForDeviceProvisioning();
        }
        this.mBatteryStatus = new BatteryStatus(1, 100, 0, 0);
        initMembers();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.TIME_TICK");
        filter.addAction("android.intent.action.TIME_SET");
        filter.addAction("android.intent.action.BATTERY_CHANGED");
        filter.addAction("android.intent.action.TIMEZONE_CHANGED");
        filter.addAction("android.intent.action.AIRPLANE_MODE");
        filter.addAction("android.intent.action.SIM_STATE_CHANGED");
        filter.addAction("android.intent.action.SERVICE_STATE");
        filter.addAction("android.intent.action.PHONE_STATE");
        filter.addAction("android.media.RINGER_MODE_CHANGED");
        filter.addAction("mediatek.intent.action.ACTION_UNLOCK_SIM_LOCK");
        filter.addAction("android.intent.action.AIRPLANE_MODE");
        filter.addAction("android.intent.action.CDMA_CARD_TYPE");
        context.registerReceiver(this.mBroadcastReceiver, filter);
        IntentFilter bootCompleteFilter = new IntentFilter();
        bootCompleteFilter.setPriority(EVENT.DYNAMIC_PACK_EVENT_BASE);
        bootCompleteFilter.addAction("android.intent.action.BOOT_COMPLETED");
        context.registerReceiver(this.mBroadcastReceiver, bootCompleteFilter);
        IntentFilter allUserFilter = new IntentFilter();
        allUserFilter.addAction("android.intent.action.USER_INFO_CHANGED");
        allUserFilter.addAction("android.app.action.NEXT_ALARM_CLOCK_CHANGED");
        allUserFilter.addAction("com.android.facelock.FACE_UNLOCK_STARTED");
        allUserFilter.addAction("com.android.facelock.FACE_UNLOCK_STOPPED");
        allUserFilter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        context.registerReceiverAsUser(this.mBroadcastAllReceiver, UserHandle.ALL, allUserFilter, null, null);
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mSubscriptionListener);
        try {
            ActivityManagerNative.getDefault().registerUserSwitchObserver(new IUserSwitchObserver.Stub() {
                public void onUserSwitching(int newUserId, IRemoteCallback reply) {
                    KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(310, newUserId, 0, reply));
                }

                public void onUserSwitchComplete(int newUserId) throws RemoteException {
                    KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(314, newUserId, 0));
                }

                public void onForegroundProfileSwitch(int newProfileId) {
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        IntentFilter strongAuthTimeoutFilter = new IntentFilter();
        strongAuthTimeoutFilter.addAction("com.android.systemui.ACTION_STRONG_AUTH_TIMEOUT");
        context.registerReceiver(this.mStrongAuthTimeoutReceiver, strongAuthTimeoutFilter, "com.android.systemui.permission.SELF", null);
        this.mTrustManager = (TrustManager) context.getSystemService("trust");
        this.mTrustManager.registerTrustListener(this);
        new LockPatternUtils(context).registerStrongAuthTracker(this.mStrongAuthTracker);
        this.mFpm = (FingerprintManager) context.getSystemService("fingerprint");
        updateFingerprintListeningState();
        if (this.mFpm != null) {
            this.mFpm.addLockoutResetCallback(this.mLockoutResetCallback);
        }
    }

    private void updateFingerprintListeningState() {
        boolean shouldListenForFingerprint = shouldListenForFingerprint();
        boolean switchFinger = SystemProperties.getInt("persist.sys.fp_switch", 1) == 1;
        if (this.mFingerprintRunningState == 1 && !shouldListenForFingerprint) {
            stopListeningForFingerprint();
        } else if (this.mFingerprintRunningState != 1 && shouldListenForFingerprint && switchFinger) {
            startListeningForFingerprint();
        }
    }

    private boolean shouldListenForFingerprint() {
        if ((!this.mKeyguardIsVisible && this.mDeviceInteractive && !this.mBouncer && !this.mGoingToSleep) || this.mSwitchingUser || this.mFingerprintAlreadyAuthenticated || isFingerprintDisabled(getCurrentUser())) {
            return false;
        }
        return true;
    }

    private void startListeningForFingerprint() {
        if (this.mFingerprintRunningState == 2) {
            setFingerprintRunningState(3);
            return;
        }
        Log.v("KeyguardUpdateMonitor", "startListeningForFingerprint()");
        int userId = ActivityManager.getCurrentUser();
        if (isUnlockWithFingerprintPossible(userId)) {
            if (this.mFingerprintCancelSignal != null) {
                this.mFingerprintCancelSignal.cancel();
            }
            this.mFingerprintCancelSignal = new CancellationSignal();
            this.mFpm.authenticate(null, this.mFingerprintCancelSignal, 0, this.mAuthenticationCallback, null, userId);
            setFingerprintRunningState(1);
        }
    }

    public boolean isUnlockWithFingerprintPossible(int userId) {
        if (this.mFpm == null || !this.mFpm.isHardwareDetected() || isFingerprintDisabled(userId) || this.mFpm.getEnrolledFingerprints(userId).size() <= 0) {
            return false;
        }
        return true;
    }

    private void stopListeningForFingerprint() {
        Log.v("KeyguardUpdateMonitor", "stopListeningForFingerprint()");
        if (this.mFingerprintRunningState == 1) {
            this.mFingerprintCancelSignal.cancel();
            this.mFingerprintCancelSignal = null;
            setFingerprintRunningState(2);
        }
        if (this.mFingerprintRunningState == 3) {
            setFingerprintRunningState(2);
        }
    }

    private boolean isDeviceProvisionedInSettingsDb() {
        return Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
    }

    private void watchForDeviceProvisioning() {
        this.mDeviceProvisionedObserver = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                KeyguardUpdateMonitor.this.mDeviceProvisioned = KeyguardUpdateMonitor.this.isDeviceProvisionedInSettingsDb();
                if (KeyguardUpdateMonitor.this.mDeviceProvisioned) {
                    KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(308);
                }
                Log.d("KeyguardUpdateMonitor", "DEVICE_PROVISIONED state = " + KeyguardUpdateMonitor.this.mDeviceProvisioned);
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("device_provisioned"), false, this.mDeviceProvisionedObserver);
        boolean provisioned = isDeviceProvisionedInSettingsDb();
        if (provisioned != this.mDeviceProvisioned) {
            this.mDeviceProvisioned = provisioned;
            if (this.mDeviceProvisioned) {
                this.mHandler.sendEmptyMessage(308);
            }
        }
    }

    protected void handleDevicePolicyManagerStateChanged() {
        updateFingerprintListeningState();
        for (int i = this.mCallbacks.size() - 1; i >= 0; i--) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (cb != null) {
                cb.onDevicePolicyManagerStateChanged();
            }
        }
    }

    protected void handleUserSwitching(int userId, IRemoteCallback reply) {
        this.mSwitchingUser = true;
        updateFingerprintListeningState();
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (cb != null) {
                cb.onUserSwitching(userId);
            }
        }
        try {
            reply.sendResult(null);
        } catch (RemoteException e) {
        }
    }

    protected void handleUserSwitchComplete(int userId) {
        this.mSwitchingUser = false;
        updateFingerprintListeningState();
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (cb != null) {
                cb.onUserSwitchComplete(userId);
            }
        }
    }

    public void dispatchBootCompleted() {
        this.mHandler.sendEmptyMessage(313);
    }

    protected void handleBootCompleted() {
        if (!this.mBootCompleted) {
            this.mBootCompleted = true;
            for (int i = 0; i < this.mCallbacks.size(); i++) {
                KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
                if (cb != null) {
                    cb.onBootCompleted();
                }
            }
        }
    }

    protected void handleDeviceProvisioned() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (cb != null) {
                cb.onDeviceProvisioned();
            }
        }
        if (this.mDeviceProvisionedObserver != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mDeviceProvisionedObserver);
            this.mDeviceProvisionedObserver = null;
        }
    }

    protected void handlePhoneStateChanged() {
        int i;
        Log.d("KeyguardUpdateMonitor", "handlePhoneStateChanged");
        this.mPhoneState = 0;
        for (i = 0; i < KeyguardUtils.getNumOfPhone(); i++) {
            int callState = TelephonyManager.getDefault().getCallState(KeyguardUtils.getSubIdUsingPhoneId(i));
            if (callState == 2) {
                this.mPhoneState = callState;
            } else if (callState == 1 && this.mPhoneState == 0) {
                this.mPhoneState = callState;
            }
        }
        Log.d("KeyguardUpdateMonitor", "handlePhoneStateChanged() - mPhoneState = " + this.mPhoneState);
        for (i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (cb != null) {
                cb.onPhoneStateChanged(this.mPhoneState);
            }
        }
    }

    protected void handleRingerModeChange(int mode) {
        Log.d("KeyguardUpdateMonitor", "handleRingerModeChange(" + mode + ")");
        this.mRingMode = mode;
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (cb != null) {
                cb.onRingerModeChanged(mode);
            }
        }
    }

    private void handleTimeUpdate() {
        Log.d("KeyguardUpdateMonitor", "handleTimeUpdate");
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (cb != null) {
                cb.onTimeChanged();
            }
        }
    }

    private void handleBatteryUpdate(BatteryStatus status) {
        Log.d("KeyguardUpdateMonitor", "handleBatteryUpdate");
        boolean batteryUpdateInteresting = isBatteryUpdateInteresting(this.mBatteryStatus, status);
        this.mBatteryStatus = status;
        if (batteryUpdateInteresting) {
            for (int i = 0; i < this.mCallbacks.size(); i++) {
                KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
                if (cb != null) {
                    cb.onRefreshBatteryInfo(status);
                }
            }
        }
    }

    private void handleCarrierInfoUpdate(int phoneId) {
    }

    private void printState() {
        for (int i = 0; i < KeyguardUtils.getNumOfPhone(); i++) {
            Log.d("KeyguardUpdateMonitor", "Phone# " + i + ", state = " + this.mSimStateOfPhoneId.get(Integer.valueOf(i)));
        }
    }

    private void handleSimStateChange(SimData simArgs) {
        State state = simArgs.simState;
        Log.d("KeyguardUpdateMonitor", "handleSimStateChange: intentValue = " + simArgs + " " + "state resolved to " + state.toString() + " phoneId=" + simArgs.phoneId);
        if (state == State.UNKNOWN) {
            return;
        }
        if (state == State.NETWORK_LOCKED || state != this.mSimStateOfPhoneId.get(Integer.valueOf(simArgs.phoneId))) {
            this.mSimStateOfPhoneId.put(Integer.valueOf(simArgs.phoneId), state);
            int phoneId = simArgs.phoneId;
            Log.d("KeyguardUpdateMonitor", "handleSimStateChange phoneId = " + phoneId);
            printState();
            for (int i = 0; i < this.mCallbacks.size(); i++) {
                KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
                if (cb != null) {
                    cb.onSimStateChangedUsingPhoneId(phoneId, state);
                }
            }
        }
    }

    private void handleServiceStateChange(int subId, ServiceState serviceState) {
        Log.d("KeyguardUpdateMonitor", "handleServiceStateChange(subId=" + subId + ", serviceState=" + serviceState);
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            this.mServiceStates.put(Integer.valueOf(subId), serviceState);
            for (int j = 0; j < this.mCallbacks.size(); j++) {
                KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(j)).get();
                if (cb != null) {
                    cb.onRefreshCarrierInfo();
                }
            }
            return;
        }
        Log.w("KeyguardUpdateMonitor", "invalid subId in handleServiceStateChange()");
    }

    public void onKeyguardVisibilityChanged(boolean showing) {
        Log.d("KeyguardUpdateMonitor", "onKeyguardVisibilityChanged(" + showing + ")");
        this.mKeyguardIsVisible = showing;
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (cb != null) {
                cb.onKeyguardVisibilityChangedRaw(showing);
            }
        }
        if (!showing) {
            this.mFingerprintAlreadyAuthenticated = false;
        }
        updateFingerprintListeningState();
    }

    private void handleKeyguardReset() {
        Log.d("KeyguardUpdateMonitor", "handleKeyguardReset");
        updateFingerprintListeningState();
    }

    private void handleKeyguardBouncerChanged(int bouncer) {
        Log.d("KeyguardUpdateMonitor", "handleKeyguardBouncerChanged(" + bouncer + ")");
        boolean isBouncer = bouncer == 1;
        this.mBouncer = isBouncer;
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (cb != null) {
                cb.onKeyguardBouncerChanged(isBouncer);
            }
        }
        if (!isBouncer) {
            this.mFingerprintAlreadyAuthenticated = false;
        }
        updateFingerprintListeningState();
    }

    private void handleReportEmergencyCallAction() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (cb != null) {
                cb.onEmergencyCallAction();
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean isBatteryUpdateInteresting(BatteryStatus old, BatteryStatus current) {
        boolean nowPluggedIn = current.isPluggedIn();
        boolean wasPluggedIn = old.isPluggedIn();
        boolean stateChangedWhilePluggedIn = (wasPluggedIn && nowPluggedIn) ? old.status != current.status : false;
        if (wasPluggedIn != nowPluggedIn || stateChangedWhilePluggedIn || old.level != current.level) {
            return true;
        }
        if (nowPluggedIn || !current.isBatteryLow() || current.level == old.level) {
            return false;
        }
        return true;
    }

    private CharSequence getTelephonyPlmnFrom(Intent intent) {
        if (!intent.getBooleanExtra("showPlmn", false)) {
            return null;
        }
        String plmn = intent.getStringExtra("plmn");
        if (plmn == null) {
            plmn = getDefaultPlmn();
        }
        return plmn;
    }

    public CharSequence getDefaultPlmn() {
        return this.mContext.getResources().getText(R$string.keyguard_carrier_default);
    }

    private CharSequence getTelephonySpnFrom(Intent intent) {
        if (intent.getBooleanExtra("showSpn", false)) {
            String spn = intent.getStringExtra("spn");
            if (spn != null) {
                return spn;
            }
        }
        return null;
    }

    public void removeCallback(KeyguardUpdateMonitorCallback callback) {
        Log.v("KeyguardUpdateMonitor", "*** unregister callback for " + callback);
        for (int i = this.mCallbacks.size() - 1; i >= 0; i--) {
            if (((WeakReference) this.mCallbacks.get(i)).get() == callback) {
                this.mCallbacks.remove(i);
            }
        }
    }

    public void registerCallback(KeyguardUpdateMonitorCallback callback) {
        Log.v("KeyguardUpdateMonitor", "*** register callback for " + callback);
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            if (((WeakReference) this.mCallbacks.get(i)).get() == callback) {
                Log.e("KeyguardUpdateMonitor", "Object tried to add another callback", new Exception("Called by"));
                return;
            }
        }
        this.mCallbacks.add(new WeakReference(callback));
        removeCallback(null);
        sendUpdates(callback);
        this.mNewClientRegUpdateMonitor = true;
    }

    private void sendUpdates(KeyguardUpdateMonitorCallback callback) {
        callback.onRefreshBatteryInfo(this.mBatteryStatus);
        callback.onTimeChanged();
        callback.onRingerModeChanged(this.mRingMode);
        callback.onPhoneStateChanged(this.mPhoneState);
        callback.onRefreshCarrierInfo();
        callback.onClockVisibilityChanged();
        for (int phoneId = 0; phoneId < KeyguardUtils.getNumOfPhone(); phoneId++) {
            callback.onSimStateChangedUsingPhoneId(phoneId, (State) this.mSimStateOfPhoneId.get(Integer.valueOf(phoneId)));
        }
    }

    public void sendKeyguardReset() {
        this.mHandler.obtainMessage(312).sendToTarget();
    }

    public void sendKeyguardBouncerChanged(boolean showingBouncer) {
        Log.d("KeyguardUpdateMonitor", "sendKeyguardBouncerChanged(" + showingBouncer + ")");
        Message message = this.mHandler.obtainMessage(322);
        this.isKeyguardBouncerShow = showingBouncer;
        message.arg1 = showingBouncer ? 1 : 0;
        message.sendToTarget();
    }

    public State getSimStateOfPhoneId(int phoneId) {
        return (State) this.mSimStateOfPhoneId.get(Integer.valueOf(phoneId));
    }

    public void reportSimUnlocked(int phoneId) {
        handleSimStateChange(new SimData(State.READY, phoneId, KeyguardUtils.getSubIdUsingPhoneId(phoneId)));
    }

    public void reportEmergencyCallAction(boolean bypassHandler) {
        if (bypassHandler) {
            handleReportEmergencyCallAction();
        } else {
            this.mHandler.obtainMessage(318).sendToTarget();
        }
    }

    public boolean isDeviceProvisioned() {
        boolean z = false;
        if (this.mDeviceProvisioned) {
            Log.d("KeyguardUpdateMonitor", "mDeviceProvisioned == true");
            return this.mDeviceProvisioned;
        }
        Log.d("KeyguardUpdateMonitor", "isDeviceProvisioned get DEVICE_PROVISIONED from db again !!");
        if (Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0) {
            z = true;
        }
        return z;
    }

    public void clearFailedUnlockAttempts() {
        this.mFailedAttempts.delete(sCurrentUser);
        this.mFailedBiometricUnlockAttempts = 0;
    }

    public int getFailedUnlockAttempts() {
        return this.mFailedAttempts.get(sCurrentUser, 0);
    }

    public void reportFailedStrongAuthUnlockAttempt() {
        this.mFailedAttempts.put(sCurrentUser, getFailedUnlockAttempts() + 1);
    }

    public void clearFingerprintRecognized() {
        this.mUserFingerprintAuthenticated.clear();
    }

    public boolean isSimPinVoiceSecure() {
        return isSimPinSecure();
    }

    private boolean refreshSimState(int subId, int slotId) {
        State state;
        Log.d("KeyguardUpdateMonitor", "refreshSimState() - sub = " + subId + " phone = " + slotId);
        int simState = TelephonyManager.from(this.mContext).getSimState(slotId);
        try {
            state = State.intToState(simState);
        } catch (IllegalArgumentException e) {
            Log.w("KeyguardUpdateMonitor", "Unknown sim state: " + simState);
            state = State.UNKNOWN;
        }
        State oriState = (State) this.mSimStateOfPhoneId.get(Integer.valueOf(slotId));
        boolean changed = oriState != state;
        if (changed) {
            this.mSimStateOfPhoneId.put(Integer.valueOf(slotId), state);
        }
        Log.d("KeyguardUpdateMonitor", "refreshSimState() - phoneId = " + slotId + ", ori-state = " + oriState + ", new-state = " + state + ", changed = " + changed);
        return changed;
    }

    public boolean isSimPinSecure() {
        for (int phoneId = 0; phoneId < KeyguardUtils.getNumOfPhone(); phoneId++) {
            if (isSimPinSecure(phoneId)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSimPinSecure(int phoneId) {
        State simState = (State) this.mSimStateOfPhoneId.get(Integer.valueOf(phoneId));
        if (!(simState == State.PIN_REQUIRED || simState == State.PUK_REQUIRED)) {
            if (simState != State.NETWORK_LOCKED || !KeyguardUtils.isMediatekSimMeLockSupport()) {
                return false;
            }
        }
        if (getPinPukMeDismissFlagOfPhoneId(phoneId)) {
            return false;
        }
        return true;
    }

    public void dispatchStartedWakingUp() {
        synchronized (this) {
            this.mDeviceInteractive = true;
        }
        this.mHandler.sendEmptyMessage(319);
    }

    public void dispatchStartedGoingToSleep(int why) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(321, why, 0));
    }

    public void dispatchFinishedGoingToSleep(int why) {
        synchronized (this) {
            this.mDeviceInteractive = false;
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(320, why, 0));
    }

    public void dispatchScreenTurnedOn() {
        synchronized (this) {
            this.mScreenOn = true;
        }
        this.mHandler.sendEmptyMessage(331);
    }

    public void dispatchScreenTurnedOff() {
        synchronized (this) {
            this.mScreenOn = false;
        }
        this.mHandler.sendEmptyMessage(332);
    }

    public boolean isDeviceInteractive() {
        return this.mDeviceInteractive;
    }

    public boolean isGoingToSleep() {
        return this.mGoingToSleep;
    }

    public SubscriptionInfo getSubscriptionInfoForSubId(int subId) {
        return getSubscriptionInfoForSubId(subId, false);
    }

    public SubscriptionInfo getSubscriptionInfoForSubId(int subId, boolean forceReload) {
        List<SubscriptionInfo> list = getSubscriptionInfo(forceReload);
        for (int i = 0; i < list.size(); i++) {
            SubscriptionInfo info = (SubscriptionInfo) list.get(i);
            if (subId == info.getSubscriptionId()) {
                return info;
            }
        }
        return null;
    }

    public int getSimPinLockPhoneId() {
        int phoneId = 0;
        while (phoneId < KeyguardUtils.getNumOfPhone()) {
            Log.d("KeyguardUpdateMonitor", "getSimPinLockSubId, phoneId=" + phoneId + " mSimStateOfPhoneId.get(phoneId)=" + this.mSimStateOfPhoneId.get(Integer.valueOf(phoneId)));
            if (this.mSimStateOfPhoneId.get(Integer.valueOf(phoneId)) == State.PIN_REQUIRED && !getPinPukMeDismissFlagOfPhoneId(phoneId)) {
                return phoneId;
            }
            phoneId++;
        }
        return -1;
    }

    public int getSimPukLockPhoneId() {
        int phoneId = 0;
        while (phoneId < KeyguardUtils.getNumOfPhone()) {
            Log.d("KeyguardUpdateMonitor", "getSimPukLockSubId, phoneId=" + phoneId + " mSimStateOfSub.get(phoneId)=" + this.mSimStateOfPhoneId.get(Integer.valueOf(phoneId)));
            if (this.mSimStateOfPhoneId.get(Integer.valueOf(phoneId)) == State.PUK_REQUIRED && !getPinPukMeDismissFlagOfPhoneId(phoneId) && getRetryPukCountOfPhoneId(phoneId) != 0) {
                return phoneId;
            }
            phoneId++;
        }
        return -1;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("KeyguardUpdateMonitor state:");
        pw.println("  SIM States:");
        for (SimData data : this.mSimDatas.values()) {
            pw.println("    " + data.toString());
        }
        pw.println("  Subs:");
        if (this.mSubscriptionInfo != null) {
            for (int i = 0; i < this.mSubscriptionInfo.size(); i++) {
                pw.println("    " + this.mSubscriptionInfo.get(i));
            }
        }
        pw.println("  Service states:");
        for (Integer intValue : this.mServiceStates.keySet()) {
            int subId = intValue.intValue();
            pw.println("    " + subId + "=" + this.mServiceStates.get(Integer.valueOf(subId)));
        }
    }

    private void initMembers() {
        Log.d("KeyguardUpdateMonitor", "initMembers() - NumOfPhone=" + KeyguardUtils.getNumOfPhone());
        for (int i = 0; i < KeyguardUtils.getNumOfPhone(); i++) {
            this.mSimStateOfPhoneId.put(Integer.valueOf(i), State.UNKNOWN);
            this.mTelephonyPlmn.put(Integer.valueOf(i), getDefaultPlmn());
            this.mTelephonyCsgId.put(Integer.valueOf(i), "");
            this.mTelephonyHnbName.put(Integer.valueOf(i), "");
            this.mSimMeCategory.put(Integer.valueOf(i), Integer.valueOf(0));
            this.mSimMeLeftRetryCount.put(Integer.valueOf(i), Integer.valueOf(5));
        }
    }

    public boolean getPinPukMeDismissFlagOfPhoneId(int phoneId) {
        int flag2Check = 1 << phoneId;
        return (this.mPinPukMeDismissFlag & flag2Check) == flag2Check;
    }

    public int getRetryPukCountOfPhoneId(int phoneId) {
        if (phoneId == 3) {
            return SystemProperties.getInt("gsm.sim.retry.puk1.4", -1);
        }
        if (phoneId == 2) {
            return SystemProperties.getInt("gsm.sim.retry.puk1.3", -1);
        }
        if (phoneId == 1) {
            return SystemProperties.getInt("gsm.sim.retry.puk1.2", -1);
        }
        return SystemProperties.getInt("gsm.sim.retry.puk1", -1);
    }

    public int getSimMeCategoryOfPhoneId(int phoneId) {
        return ((Integer) this.mSimMeCategory.get(Integer.valueOf(phoneId))).intValue();
    }

    public int getSimMeLeftRetryCountOfPhoneId(int phoneId) {
        return ((Integer) this.mSimMeLeftRetryCount.get(Integer.valueOf(phoneId))).intValue();
    }

    public void minusSimMeLeftRetryCountOfPhoneId(int phoneId) {
        int simMeRetryCount = ((Integer) this.mSimMeLeftRetryCount.get(Integer.valueOf(phoneId))).intValue();
        if (simMeRetryCount > 0) {
            this.mSimMeLeftRetryCount.put(Integer.valueOf(phoneId), Integer.valueOf(simMeRetryCount - 1));
        }
    }

    private CharSequence getTelephonyHnbNameFrom(Intent intent) {
        return intent.getStringExtra("hnbName");
    }

    private CharSequence getTelephonyCsgIdFrom(Intent intent) {
        return intent.getStringExtra("csgId");
    }

    private void handleAirPlaneModeUpdate(boolean airPlaneModeEnabled) {
        int i;
        if (!airPlaneModeEnabled) {
            Log.d("KeyguardUpdateMonitor", "Force to send sim pin/puk/me lock again if needed.");
            for (i = 0; i < KeyguardUtils.getNumOfPhone(); i++) {
                setPinPukMeDismissFlagOfPhoneId(i, false);
                Log.d("KeyguardUpdateMonitor", "setPinPukMeDismissFlagOfPhoneId false: " + i);
            }
            for (int phoneId = 0; phoneId < KeyguardUtils.getNumOfPhone(); phoneId++) {
                Log.d("KeyguardUpdateMonitor", "phoneId = " + phoneId + " state=" + this.mSimStateOfPhoneId.get(Integer.valueOf(phoneId)));
                switch (-getcom_android_internal_telephony_IccCardConstants$StateSwitchesValues()[((State) this.mSimStateOfPhoneId.get(Integer.valueOf(phoneId))).ordinal()]) {
                    case 1:
                    case 2:
                    case 3:
                        State oriState = (State) this.mSimStateOfPhoneId.get(Integer.valueOf(phoneId));
                        this.mSimStateOfPhoneId.put(Integer.valueOf(phoneId), State.UNKNOWN);
                        int meCategory = 0;
                        if (this.mSimMeCategory.get(Integer.valueOf(phoneId)) != null) {
                            meCategory = ((Integer) this.mSimMeCategory.get(Integer.valueOf(phoneId))).intValue();
                        }
                        SimData simData = new SimData(oriState, phoneId, KeyguardUtils.getSubIdUsingPhoneId(phoneId), meCategory);
                        Log.v("KeyguardUpdateMonitor", "SimData state=" + simData.simState + ", phoneId=" + simData.phoneId + ", subId=" + simData.subId + ", SimData.simMECategory = " + simData.simMECategory);
                        proceedToHandleSimStateChanged(simData);
                        break;
                    default:
                        break;
                }
            }
        } else if (airPlaneModeEnabled && KeyguardUtils.isFlightModePowerOffMd()) {
            Log.d("KeyguardUpdateMonitor", "Air mode is on, supress all SIM PIN/PUK/ME Lock views.");
            for (i = 0; i < KeyguardUtils.getNumOfPhone(); i++) {
                setPinPukMeDismissFlagOfPhoneId(i, true);
                Log.d("KeyguardUpdateMonitor", "setPinPukMeDismissFlagOfPhoneId true: " + i);
            }
        }
        for (i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (cb != null) {
                cb.onAirPlaneModeChanged(airPlaneModeEnabled);
                cb.onRefreshCarrierInfo();
            }
        }
    }

    private void handleCDMACardTypeUpdate(boolean isLockedCard) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (cb != null) {
                cb.onCDMACardTypeChanges(isLockedCard);
            }
        }
    }

    public boolean isAlternateUnlockEnabled() {
        return this.mAlternateUnlockEnabled;
    }

    public boolean isSwitchingUser() {
        return this.mSwitchingUser;
    }

    public boolean isKeyguardBouncer() {
        return this.mBouncer;
    }

    public boolean isKeyguardVisible() {
        return this.mKeyguardIsVisible;
    }

    public int getPhoneState() {
        return this.mPhoneState;
    }

    public void reportFailedBiometricUnlockAttempt() {
        this.mFailedBiometricUnlockAttempts++;
    }

    public boolean getMaxBiometricUnlockAttemptsReached() {
        return this.mFailedBiometricUnlockAttempts >= 3;
    }

    public void setAlternateUnlockEnabled(boolean enabled) {
        Log.d("KeyguardUpdateMonitor", "setAlternateUnlockEnabled(enabled = " + enabled + ")");
        this.mAlternateUnlockEnabled = enabled;
    }

    public boolean isScreenOn() {
        return this.mScreenOn;
    }
}
