package com.mediatek.keyguard.AntiTheft;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardSecurityCallback;
import com.android.keyguard.KeyguardSecurityModel;
import com.android.keyguard.KeyguardSecurityModel.SecurityMode;
import com.android.keyguard.KeyguardUtils;
import com.android.keyguard.R$id;
import com.android.keyguard.R$layout;
import com.android.keyguard.R$string;
import com.android.keyguard.ViewMediatorCallback;
import com.mediatek.common.dm.DmAgent;
import com.mediatek.common.ppl.IPplManager;
import com.mediatek.common.ppl.IPplManager.Stub;
import com.mediatek.internal.telephony.ppl.IPplAgent;

public class AntiTheftManager {
    private static /* synthetic */ int[] -com_android_keyguard_KeyguardSecurityModel$SecurityModeSwitchesValues;
    private static boolean mAntiTheftAutoTestNotShowUI = false;
    private static int mAntiTheftLockEnabled = 0;
    private static Context mContext;
    private static int mDismissable = 0;
    private static IPplManager mIPplManager;
    private static int mKeypadNeeded = 0;
    private static AntiTheftManager sInstance;
    private final int MSG_ARG_LOCK = 0;
    private final int MSG_ARG_UNLOCK = 1;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("AntiTheftManager", "handleAntiTheftViewUpdate() - action = " + action);
            if ("com.mediatek.dm.LAWMO_LOCK".equals(action)) {
                Log.d("AntiTheftManager", "receive OMADM_LAWMO_LOCK");
                AntiTheftManager.this.sendAntiTheftUpdateMsg(1, 0);
            } else if ("com.mediatek.dm.LAWMO_UNLOCK".equals(action)) {
                Log.d("AntiTheftManager", "receive OMADM_LAWMO_UNLOCK");
                AntiTheftManager.this.sendAntiTheftUpdateMsg(1, 1);
            } else if ("com.mediatek.ppl.NOTIFY_LOCK".equals(action)) {
                Log.d("AntiTheftManager", "receive PPL_LOCK");
                if (AntiTheftManager.isSystemEncrypted()) {
                    Log.d("AntiTheftManager", "Currently system needs to be decrypted. Not show PPL.");
                    return;
                }
                AntiTheftManager.this.sendAntiTheftUpdateMsg(2, 0);
            } else if ("com.mediatek.ppl.NOTIFY_UNLOCK".equals(action)) {
                Log.d("AntiTheftManager", "receive PPL_UNLOCK");
                AntiTheftManager.this.sendAntiTheftUpdateMsg(2, 1);
            } else if ("android.intent.action.ACTION_PREBOOT_IPO".equals(action)) {
                AntiTheftManager.this.doBindAntiThftLockServices();
            }
        }
    };
    private Handler mHandler = new Handler(Looper.myLooper(), null, true) {
        public void handleMessage(Message msg) {
            boolean z = false;
            switch (msg.what) {
                case 1001:
                    AntiTheftManager antiTheftManager = AntiTheftManager.this;
                    int i = msg.arg1;
                    if (msg.arg2 == 0) {
                        z = true;
                    }
                    antiTheftManager.handleAntiTheftViewUpdate(i, z);
                    return;
                default:
                    return;
            }
        }
    };
    protected KeyguardSecurityCallback mKeyguardSecurityCallback;
    private LockPatternUtils mLockPatternUtils;
    private ServiceConnection mPplServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i("AntiTheftManager", "onServiceConnected() -- PPL");
            AntiTheftManager.mIPplManager = Stub.asInterface(service);
        }

        public void onServiceDisconnected(ComponentName name) {
            Log.i("AntiTheftManager", "onServiceDisconnected()");
            AntiTheftManager.mIPplManager = null;
        }
    };
    private KeyguardSecurityModel mSecurityModel;
    private ViewMediatorCallback mViewMediatorCallback;

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
            iArr[SecurityMode.AntiTheft.ordinal()] = 6;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[SecurityMode.Biometric.ordinal()] = 7;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[SecurityMode.Invalid.ordinal()] = 8;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[SecurityMode.None.ordinal()] = 9;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[SecurityMode.PIN.ordinal()] = 10;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[SecurityMode.Password.ordinal()] = 11;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[SecurityMode.Pattern.ordinal()] = 12;
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
            iArr[SecurityMode.Voice.ordinal()] = 13;
        } catch (NoSuchFieldError e13) {
        }
        -com_android_keyguard_KeyguardSecurityModel$SecurityModeSwitchesValues = iArr;
        return iArr;
    }

    private void setAntiTheftLocked(int r1, boolean r2) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: com.mediatek.keyguard.AntiTheft.AntiTheftManager.setAntiTheftLocked(int, boolean):void
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
        throw new UnsupportedOperationException("Method not decompiled: com.mediatek.keyguard.AntiTheft.AntiTheftManager.setAntiTheftLocked(int, boolean):void");
    }

    public static void setDismissable(int r1, boolean r2) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: com.mediatek.keyguard.AntiTheft.AntiTheftManager.setDismissable(int, boolean):void
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
        throw new UnsupportedOperationException("Method not decompiled: com.mediatek.keyguard.AntiTheft.AntiTheftManager.setDismissable(int, boolean):void");
    }

    public static void setKeypadNeeded(int r1, boolean r2) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: com.mediatek.keyguard.AntiTheft.AntiTheftManager.setKeypadNeeded(int, boolean):void
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
        throw new UnsupportedOperationException("Method not decompiled: com.mediatek.keyguard.AntiTheft.AntiTheftManager.setKeypadNeeded(int, boolean):void");
    }

    public AntiTheftManager(Context context, ViewMediatorCallback viewMediatorCallback, LockPatternUtils lockPatternUtils) {
        Log.d("AntiTheftManager", "AntiTheftManager() is called.");
        mContext = context;
        this.mViewMediatorCallback = viewMediatorCallback;
        this.mLockPatternUtils = lockPatternUtils;
        this.mSecurityModel = new KeyguardSecurityModel(mContext);
        IntentFilter filter = new IntentFilter();
        setKeypadNeeded(1, false);
        setDismissable(1, false);
        filter.addAction("com.mediatek.dm.LAWMO_LOCK");
        filter.addAction("com.mediatek.dm.LAWMO_UNLOCK");
        if (KeyguardUtils.isPrivacyProtectionLockSupport()) {
            Log.d("AntiTheftManager", "MTK_PRIVACY_PROTECTION_LOCK is enabled.");
            setKeypadNeeded(2, true);
            setDismissable(2, true);
            filter.addAction("com.mediatek.ppl.NOTIFY_LOCK");
            filter.addAction("com.mediatek.ppl.NOTIFY_UNLOCK");
        }
        filter.addAction("android.intent.action.ACTION_PREBOOT_IPO");
        mContext.registerReceiver(this.mBroadcastReceiver, filter);
    }

    public static AntiTheftManager getInstance(Context context, ViewMediatorCallback viewMediatorCallback, LockPatternUtils lockPatternUtils) {
        Log.d("AntiTheftManager", "getInstance(...) is called.");
        if (sInstance == null) {
            Log.d("AntiTheftManager", "getInstance(...) create one.");
            sInstance = new AntiTheftManager(context, viewMediatorCallback, lockPatternUtils);
        }
        return sInstance;
    }

    public static String getAntiTheftModeName(int mode) {
        switch (mode) {
            case 0:
                return "AntiTheftMode.None";
            case 1:
                return "AntiTheftMode.DmLock";
            case 2:
                return "AntiTheftMode.PplLock";
            default:
                return "AntiTheftMode.None";
        }
    }

    public static int getCurrentAntiTheftMode() {
        Log.d("AntiTheftManager", "getCurrentAntiTheftMode() is called.");
        if (!isAntiTheftLocked()) {
            return 0;
        }
        for (int shift = 0; shift < 32; shift++) {
            int mode = mAntiTheftLockEnabled & (1 << shift);
            if (mode != 0) {
                return mode;
            }
        }
        return 0;
    }

    public static boolean isKeypadNeeded() {
        int mode = getCurrentAntiTheftMode();
        Log.d("AntiTheftManager", "getCurrentAntiTheftMode() = " + getAntiTheftModeName(mode));
        boolean needKeypad = (mKeypadNeeded & mode) != 0;
        Log.d("AntiTheftManager", "isKeypadNeeded() = " + needKeypad);
        return needKeypad;
    }

    public static boolean isAntiTheftLocked() {
        return mAntiTheftLockEnabled != 0;
    }

    private static boolean isNeedUpdate(int lockMode, boolean enable) {
        if (enable && (mAntiTheftLockEnabled & lockMode) != 0) {
            Log.d("AntiTheftManager", "isNeedUpdate() - lockMode( " + lockMode + " ) is already enabled, no need update");
            return false;
        } else if (enable || (mAntiTheftLockEnabled & lockMode) != 0) {
            return true;
        } else {
            Log.d("AntiTheftManager", "isNeedUpdate() - lockMode( " + lockMode + " ) is already disabled, no need update");
            return false;
        }
    }

    public static boolean isDismissable() {
        int mode = getCurrentAntiTheftMode();
        if (mode == 0) {
            return true;
        }
        if ((mDismissable & mode) != 0) {
            return true;
        }
        return false;
    }

    public static boolean isAntiTheftPriorToSecMode(SecurityMode mode) {
        int currentAntiTheftType = getCurrentAntiTheftMode();
        if (!isAntiTheftLocked()) {
            return false;
        }
        if (currentAntiTheftType == 1) {
            return true;
        }
        switch (-getcom_android_keyguard_KeyguardSecurityModel$SecurityModeSwitchesValues()[mode.ordinal()]) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                return false;
            default:
                return true;
        }
    }

    public static int getAntiTheftViewId() {
        return R$id.keyguard_antitheft_lock_view;
    }

    public static int getAntiTheftLayoutId() {
        return R$layout.mtk_keyguard_anti_theft_lock_view;
    }

    public static int getPrompt() {
        if (getCurrentAntiTheftMode() == 1) {
            return R$string.dm_prompt;
        }
        return R$string.ppl_prompt;
    }

    public static String getAntiTheftMessageAreaText(CharSequence text, CharSequence seperator) {
        StringBuilder b = new StringBuilder();
        if (!(text == null || text.length() <= 0 || text.toString().equals("AntiTheft Noneed Print Text"))) {
            b.append(text);
            b.append(seperator);
        }
        b.append(mContext.getText(getPrompt()));
        return b.toString();
    }

    public boolean checkPassword(String pw) {
        boolean unlockSuccess = false;
        int mode = getCurrentAntiTheftMode();
        Log.d("AntiTheftManager", "checkPassword, mode is " + getAntiTheftModeName(mode));
        switch (mode) {
            case 2:
                unlockSuccess = doPplCheckPassword(pw);
                break;
        }
        Log.d("AntiTheftManager", "checkPassword, unlockSuccess is " + unlockSuccess);
        return unlockSuccess;
    }

    private void sendAntiTheftUpdateMsg(int antiTheftLockType, int lock) {
        Message msg = this.mHandler.obtainMessage(1001);
        msg.arg1 = antiTheftLockType;
        msg.arg2 = lock;
        msg.sendToTarget();
    }

    private void handleAntiTheftViewUpdate(int antiTheftLockType, boolean lock) {
        boolean z = true;
        if (isNeedUpdate(antiTheftLockType, lock)) {
            setAntiTheftLocked(antiTheftLockType, lock);
            if (lock) {
                String str = "AntiTheftManager";
                StringBuilder append = new StringBuilder().append("handleAntiTheftViewUpdate() - locked, !isShowing = ");
                if (this.mViewMediatorCallback.isShowing()) {
                    z = false;
                }
                Log.d(str, append.append(z).append(" isKeyguardDoneOnGoing = ").append(this.mViewMediatorCallback.isKeyguardDoneOnGoing()).toString());
                if (!this.mViewMediatorCallback.isShowing() || this.mViewMediatorCallback.isKeyguardDoneOnGoing()) {
                    this.mViewMediatorCallback.showLocked(null);
                } else if (isAntiTheftPriorToSecMode(this.mSecurityModel.getSecurityMode())) {
                    Log.d("AntiTheftManager", "handleAntiTheftViewUpdate() - call resetStateLocked().");
                    this.mViewMediatorCallback.resetStateLocked();
                } else {
                    Log.d("AntiTheftManager", "No need to reset the security view to show AntiTheft,since current view should show above antitheft view.");
                }
            } else if (this.mKeyguardSecurityCallback != null) {
                this.mKeyguardSecurityCallback.dismiss(true);
            } else {
                Log.d("AntiTheftManager", "mKeyguardSecurityCallback is null !");
            }
            adjustStatusBarLocked();
        }
    }

    public void doBindAntiThftLockServices() {
        Log.d("AntiTheftManager", "doBindAntiThftLockServices() is called.");
        if (KeyguardUtils.isPrivacyProtectionLockSupport()) {
            bindPplService();
        }
    }

    public void doAntiTheftLockCheck() {
        if ("unencrypted".equalsIgnoreCase(SystemProperties.get("ro.crypto.state", "unsupported"))) {
            doPplLockCheck();
            doDmLockCheck();
        }
    }

    private void doDmLockCheck() {
        try {
            IBinder binder = ServiceManager.getService("DmAgent");
            if (binder != null) {
                boolean flag = DmAgent.Stub.asInterface(binder).isLockFlagSet();
                Log.i("AntiTheftManager", "dmCheckLocked, the lock flag is:" + flag);
                setAntiTheftLocked(1, flag);
                return;
            }
            Log.i("AntiTheftManager", "dmCheckLocked, DmAgent doesn't exit");
        } catch (RemoteException e) {
            Log.e("AntiTheftManager", "doDmLockCheck() - error in get DMAgent service.");
        }
    }

    private void doPplLockCheck() {
        if (mAntiTheftLockEnabled == 2) {
            setAntiTheftLocked(2, true);
        }
    }

    public static void checkPplStatus() {
        boolean isUnEncrypted = !isSystemEncrypted();
        try {
            IBinder binder = ServiceManager.getService("PPLAgent");
            if (binder != null) {
                boolean flag = IPplAgent.Stub.asInterface(binder).needLock() == 1;
                Log.i("AntiTheftManager", "PplCheckLocked, the lock flag is:" + (flag ? isUnEncrypted : false));
                if (flag && isUnEncrypted) {
                    mAntiTheftLockEnabled |= 2;
                    return;
                }
                return;
            }
            Log.i("AntiTheftManager", "PplCheckLocked, PPLAgent doesn't exit");
        } catch (RemoteException e) {
            Log.e("AntiTheftManager", "doPplLockCheck() - error in get PPLAgent service.");
        }
    }

    private void bindPplService() {
        Log.e("AntiTheftManager", "binPplService() is called.");
        if (mIPplManager == null) {
            try {
                Intent intent = new Intent("com.mediatek.ppl.service");
                intent.setClassName("com.mediatek.ppl", "com.mediatek.ppl.PplService");
                mContext.bindService(intent, this.mPplServiceConnection, 1);
                return;
            } catch (SecurityException e) {
                Log.e("AntiTheftManager", "bindPplService() - error in bind ppl service.");
                return;
            }
        }
        Log.d("AntiTheftManager", "bindPplService() -- the ppl service is already bound.");
    }

    private boolean doPplCheckPassword(String pw) {
        boolean unlockSuccess = false;
        if (mIPplManager != null) {
            try {
                unlockSuccess = mIPplManager.unlock(pw);
                Log.i("AntiTheftManager", "doPplCheckPassword, unlockSuccess is " + unlockSuccess);
                if (unlockSuccess) {
                    setAntiTheftLocked(2, false);
                }
            } catch (RemoteException e) {
            }
        } else {
            Log.i("AntiTheftManager", "doPplCheckPassword() mIPplManager == null !!??");
        }
        return unlockSuccess;
    }

    public void adjustStatusBarLocked() {
        this.mViewMediatorCallback.adjustStatusBarLocked();
    }

    public void setSecurityViewCallback(KeyguardSecurityCallback callback) {
        Log.d("AntiTheftManager", "setSecurityViewCallback(" + callback + ")");
        this.mKeyguardSecurityCallback = callback;
    }

    private static boolean isSystemEncrypted() {
        String state = SystemProperties.get("ro.crypto.state");
        String decrypt = SystemProperties.get("vold.decrypt");
        if ("unencrypted".equals(state)) {
            if ("".equals(decrypt)) {
                return false;
            }
            return true;
        } else if (!"".equals(state) && "encrypted".equals(state) && "trigger_restart_framework".equals(decrypt)) {
            return false;
        } else {
            return true;
        }
    }
}
