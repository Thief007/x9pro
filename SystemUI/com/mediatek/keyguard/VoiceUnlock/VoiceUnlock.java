package com.mediatek.keyguard.VoiceUnlock;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings.System;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardHostView.OnDismissAction;
import com.android.keyguard.KeyguardMessageArea;
import com.android.keyguard.KeyguardSecurityCallback;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.R$drawable;
import com.android.keyguard.R$id;
import com.android.keyguard.R$string;
import com.android.keyguard.SecurityMessageDisplay;
import com.android.systemui.assis.app.MAIN.EVENT;
import com.mediatek.common.voicecommand.IVoiceCommandListener;
import com.mediatek.common.voicecommand.IVoiceCommandListener.Stub;
import com.mediatek.common.voicecommand.IVoiceCommandManagerService;

public class VoiceUnlock implements BiometricSensorUnlock, Callback, OnDismissAction {
    private final int BACKUP_LOCK_TIMEOUT = 5000;
    private final long TIMEOUT_AFTER_UNLOCK_FAIL = 3000;
    private ImageView mCancel;
    private final Context mContext;
    private Handler mHandler;
    private Handler mHideHandler;
    private Runnable mHideRunnable;
    private Handler mIntensityHandler;
    private Runnable mIntensityRunnable;
    private boolean mIsRegistered = false;
    private volatile boolean mIsRunning = false;
    KeyguardSecurityCallback mKeyguardScreenCallback;
    private String mLaunchApp;
    private final LockPatternUtils mLockPatternUtils;
    private String mPkgName;
    private SecurityMessageDisplay mSecurityMessageDisplay;
    private View mUnlockView;
    private IVoiceCommandManagerService mVCmdMgrService;
    private IVoiceCommandListener mVoiceCallback = new Stub() {
        public void onVoiceCommandNotified(int mainAction, int subAction, Bundle extraData) throws RemoteException {
            Message.obtain(VoiceUnlock.this.mVoiceCommandHandler, mainAction, subAction, 0, extraData).sendToTarget();
        }
    };
    private Handler mVoiceCommandHandler = new Handler() {
        public void handleMessage(Message msg) {
            VoiceUnlock.this.handleVoiceCommandNotified(msg.what, msg.arg1, (Bundle) msg.obj);
        }
    };
    private ServiceConnection mVoiceSerConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            VoiceUnlock.this.mVCmdMgrService = IVoiceCommandManagerService.Stub.asInterface(service);
            VoiceUnlock.this.registerVoiceCommand(VoiceUnlock.this.mPkgName);
            VoiceUnlock.this.log("onServiceConnected   ");
            VoiceUnlock.this.startRecognize();
        }

        public void onServiceDisconnected(ComponentName name) {
            VoiceUnlock.this.log("onServiceDisconnected   ");
            VoiceUnlock.this.mIsRegistered = false;
            VoiceUnlock.this.mVCmdMgrService = null;
        }
    };
    private View mVoiceUnlockView;
    private ImageView mWave;
    private AnimationDrawable mWaveAnim;

    private void handleVoiceCommandNotified(int mainAction, int subAction, Bundle extraData) {
        int result = extraData.getInt("Result");
        log("onNotified result=" + result + " mainAction = " + mainAction + " subAction = " + subAction);
        if (result == 1) {
            switch (subAction) {
                case 1:
                    log("onNotified RECOGNIZE_START");
                    this.mHandler.obtainMessage(0).sendToTarget();
                    return;
                case 2:
                    int intensity = extraData.getInt("Result_Info");
                    log("onNotified RECOGNIZE_INTENSITY intensity = " + intensity);
                    this.mHandler.removeMessages(4);
                    this.mHandler.obtainMessage(4, intensity, 0).sendToTarget();
                    return;
                case 3:
                    int verifyResult = extraData.getInt("Result_Info");
                    log("onNotified RECOGNIZE_NOTIFY verifyResult = " + verifyResult);
                    if (verifyResult == 1) {
                        int commandId = extraData.getInt("Reslut_Info1");
                        log("onNotified RECOGNIZE_NOTIFY commandId = " + commandId);
                        this.mHandler.obtainMessage(1, commandId, 0).sendToTarget();
                        return;
                    }
                    this.mHandler.obtainMessage(2, verifyResult, 0).sendToTarget();
                    return;
                default:
                    return;
            }
        } else if (result == 10) {
            this.mHandler.obtainMessage(3).sendToTarget();
        }
    }

    public VoiceUnlock(Context context, View unlockView) {
        this.mContext = context;
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mUnlockView = unlockView;
        this.mPkgName = this.mContext.getPackageName();
        if (unlockView != null) {
            KeyguardMessageArea keyguardMessageArea = new KeyguardMessageArea(this.mContext);
            this.mSecurityMessageDisplay = KeyguardMessageArea.findSecurityMessageDisplay(unlockView);
        }
        this.mHandler = new Handler(this);
        this.mIntensityHandler = new Handler();
        this.mIntensityRunnable = new Runnable() {
            public void run() {
                if (VoiceUnlock.this.mVCmdMgrService != null) {
                    VoiceUnlock.this.log("sendCommand RECOGNIZE_INTENSITY");
                    VoiceUnlock.this.sendVoiceCommand(VoiceUnlock.this.mPkgName, 4, 2, null);
                }
                VoiceUnlock.this.mIntensityHandler.postDelayed(this, 90);
            }
        };
        this.mHideHandler = new Handler();
        this.mHideRunnable = new Runnable() {
            public void run() {
                VoiceUnlock.this.stop();
                VoiceUnlock.this.pokeWakelock(5000);
            }
        };
    }

    private void handleVoiceServiceReady() {
        pokeWakelock(10000);
        this.mWaveAnim = (AnimationDrawable) this.mWave.getBackground();
        if (this.mWaveAnim.isRunning()) {
            this.mWaveAnim.stop();
        }
        this.mWaveAnim.start();
        int durationTime = 0;
        for (int i = 0; i < this.mWaveAnim.getNumberOfFrames(); i++) {
            durationTime += this.mWaveAnim.getDuration(i);
        }
        this.mIntensityHandler.postDelayed(this.mIntensityRunnable, (long) durationTime);
    }

    private void handleVoiceCommandPass(int commandId) {
        stop();
        switch (commandId) {
            case 1:
                this.mLaunchApp = System.getStringForUser(this.mContext.getContentResolver(), "voice_unlock_and_launch1", -2);
                break;
            case 2:
                this.mLaunchApp = System.getStringForUser(this.mContext.getContentResolver(), "voice_unlock_and_launch2", -2);
                break;
            case 3:
                this.mLaunchApp = System.getStringForUser(this.mContext.getContentResolver(), "voice_unlock_and_launch3", -2);
                break;
            default:
                this.mLaunchApp = null;
                break;
        }
        log("handleVoiceCommandPass commandId = " + commandId + " appName = " + this.mLaunchApp);
        if (!this.mKeyguardScreenCallback.hasOnDismissAction()) {
            log("onDismissAction is null, set voice unlock dismiss action");
            this.mKeyguardScreenCallback.setOnDismissAction(this);
        }
        this.mKeyguardScreenCallback.reportUnlockAttempt(true, 0);
        this.mKeyguardScreenCallback.dismiss(true);
        pokeWakelock(10000);
    }

    private void reportFailedBiometricUnlockAttempt() {
        log("handleReportFailedAttempt()");
        KeyguardUpdateMonitor.getInstance(this.mContext).setAlternateUnlockEnabled(false);
        this.mKeyguardScreenCallback.reportUnlockAttempt(false, 0);
        this.mHideHandler.postDelayed(this.mHideRunnable, 5000);
    }

    private void handleVoiceCommandFail(int type) {
        switch (type) {
            case 0:
                this.mSecurityMessageDisplay.setMessage(R$string.voice_unlock_service_error, true);
                break;
            case 1:
                this.mSecurityMessageDisplay.setMessage(R$string.voice_unlock_password_wrong, true);
                reportFailedBiometricUnlockAttempt();
                break;
            case 2:
                this.mSecurityMessageDisplay.setMessage(R$string.voice_unlock_noisy, true);
                reportFailedBiometricUnlockAttempt();
                break;
            case 3:
                this.mSecurityMessageDisplay.setMessage(R$string.voice_unlock_weak, true);
                reportFailedBiometricUnlockAttempt();
                break;
        }
        this.mHandler.sendEmptyMessageDelayed(5, 3000);
    }

    private void handleUpdateIntensity(int intensity) {
        log("updateIntensity intensity = " + intensity);
        intensity -= 200;
        if (intensity < 128) {
            this.mWave.setImageLevel(0);
        } else if (intensity < 256) {
            this.mWave.setImageLevel(1);
        } else if (intensity < 512) {
            this.mWave.setImageLevel(2);
        } else if (intensity < 1024) {
            this.mWave.setImageLevel(3);
        } else if (intensity < 2048) {
            this.mWave.setImageLevel(4);
        }
    }

    private void handleCancel() {
        log("handleCancel()");
        KeyguardUpdateMonitor.getInstance(this.mContext).setAlternateUnlockEnabled(false);
        this.mKeyguardScreenCallback.showBackupSecurity();
        stop();
        this.mKeyguardScreenCallback.userActivity();
    }

    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case 0:
                handleVoiceServiceReady();
                break;
            case 1:
                handleVoiceCommandPass(msg.arg1);
                break;
            case 2:
                switch (msg.arg1) {
                    case 0:
                    case EVENT.CLOSE_CLIENT /*100*/:
                        handleVoiceCommandFail(1);
                        break;
                    case 2:
                        handleVoiceCommandFail(2);
                        break;
                    case 3:
                        handleVoiceCommandFail(3);
                        break;
                    default:
                        break;
                }
            case 3:
                handleVoiceCommandFail(0);
                break;
            case 4:
                handleUpdateIntensity(msg.arg1);
                break;
            case 5:
                handleCancel();
                break;
            default:
                log("Unhandled message");
                return false;
        }
        return true;
    }

    public void initializeView(View voiceUnlockView) {
        log("initializeView()");
        this.mVoiceUnlockView = voiceUnlockView;
        this.mWave = (ImageView) voiceUnlockView.findViewById(R$id.voiceLockWave);
        this.mWave.setBackgroundResource(R$drawable.mtk_voice_wave_anim);
        this.mWave.setImageResource(R$drawable.mtk_voice_wave);
        this.mWave.setImageLevel(0);
        this.mCancel = (ImageView) voiceUnlockView.findViewById(R$id.voiceLockCancel);
        this.mCancel.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Log.d("VoiceUnlock", "Press cancel key of voice unlock view.");
                VoiceUnlock.this.mHandler.obtainMessage(5).sendToTarget();
            }
        });
    }

    public void stopAndShowBackup() {
        log("stopAndShowBackup()");
        this.mHandler.obtainMessage(5).sendToTarget();
    }

    public boolean start() {
        log("start()");
        if (this.mHandler.getLooper() != Looper.myLooper()) {
            log("start() called off of the UI thread");
        }
        if (this.mIsRunning) {
            log("start() called when already running");
        }
        this.mHideHandler.removeCallbacks(this.mHideRunnable);
        startUi();
        log("register to service");
        if (this.mVCmdMgrService == null) {
            bindVoiceService(this.mContext);
        } else {
            registerVoiceCommand(this.mPkgName);
        }
        log("start() mIsRunning = true");
        this.mIsRunning = true;
        return true;
    }

    public boolean stop() {
        log("stop()");
        if (this.mHandler.getLooper() != Looper.myLooper()) {
            log("stop() called off of the UI thread");
        }
        this.mIntensityHandler.removeCallbacks(this.mIntensityRunnable);
        this.mHandler.removeMessages(5);
        boolean mWasRunning = this.mIsRunning;
        stopUi();
        if (this.mVCmdMgrService != null) {
            log("unregister to service");
            unregisterVoicecommand(this.mPkgName);
        }
        log("stop() mIsRunning  = false");
        this.mIsRunning = false;
        return mWasRunning;
    }

    public boolean onDismiss() {
        if (this.mLaunchApp == null) {
            return false;
        }
        ComponentName cn = ComponentName.unflattenFromString(this.mLaunchApp);
        log("onDismiss cn = " + cn.toString());
        final Intent intent = new Intent();
        intent.setComponent(cn);
        intent.setAction("android.intent.action.MAIN");
        intent.setFlags(872415232);
        try {
            ActivityManagerNative.getDefault().keyguardWaitingForActivityDrawn();
        } catch (RemoteException e) {
            log("can't dismiss keyguard on launch");
        }
        this.mHandler.post(new Runnable() {
            public void run() {
                try {
                    VoiceUnlock.this.mContext.startActivityAsUser(intent, new UserHandle(ActivityManager.getCurrentUser()));
                    VoiceUnlock.this.log("startActivity intent = " + intent.toString());
                } catch (ActivityNotFoundException e) {
                    VoiceUnlock.this.log("Activity not found for intent + " + intent.getAction());
                }
            }
        });
        return true;
    }

    private void registerVoiceCommand(String pkgName) {
        log("registerVoiceCommand() is called.");
        if (this.mIsRegistered) {
            log("registerVoiceCommand() - commands have been already registered.");
        } else {
            try {
                int errorid = this.mVCmdMgrService.registerListener(pkgName, this.mVoiceCallback);
                if (errorid == 0) {
                    this.mIsRegistered = true;
                    log("registerVoiceCommand() - register command successfuly.");
                } else {
                    log("register voiceCommand fail errorid=" + errorid + " with pkgName=" + pkgName);
                }
            } catch (RemoteException e) {
                this.mIsRegistered = false;
                this.mVCmdMgrService = null;
                log("register voiceCommand RemoteException =  " + e.getMessage());
            }
        }
        log("registerVoiceCommand() ends.");
    }

    private void unregisterVoicecommand(String pkgName) {
        if (this.mVCmdMgrService != null) {
            try {
                if (this.mVCmdMgrService.unregisterListener(pkgName, this.mVoiceCallback) == 0) {
                    this.mIsRegistered = false;
                }
            } catch (RemoteException e) {
                log("unregisteVoiceCmd voiceCommand RemoteException = " + e.getMessage());
                this.mIsRegistered = false;
                this.mVCmdMgrService = null;
            }
            log("unregisteVoiceCmd end ");
            this.mContext.unbindService(this.mVoiceSerConnection);
            this.mVCmdMgrService = null;
            this.mIsRegistered = false;
        }
    }

    private void sendVoiceCommand(String pkgName, int mainAction, int subAction, Bundle extraData) {
        if (this.mIsRegistered) {
            try {
                if (this.mVCmdMgrService.sendCommand(pkgName, mainAction, subAction, extraData) != 0) {
                    log("send voice Command fail ");
                    return;
                } else {
                    log("send voice Command success ");
                    return;
                }
            } catch (RemoteException e) {
                this.mIsRegistered = false;
                this.mVCmdMgrService = null;
                log("send voice Command RemoteException =  " + e.getMessage());
                return;
            }
        }
        log("didn't register , can not send voice Command  ");
    }

    private void bindVoiceService(Context context) {
        log("bindVoiceService() enters.");
        Intent mVoiceServiceIntent = new Intent();
        mVoiceServiceIntent.setAction("com.mediatek.voicecommand");
        mVoiceServiceIntent.addCategory("com.mediatek.nativeservice");
        mVoiceServiceIntent.setPackage("com.mediatek.voicecommand");
        context.bindServiceAsUser(mVoiceServiceIntent, this.mVoiceSerConnection, 1, new UserHandle(ActivityManager.getCurrentUser()));
        log("bindVoiceService() leaves.");
    }

    private void startRecognize() {
        if (this.mVCmdMgrService != null) {
            log("sendCommand RECOGNIZE_START");
            sendVoiceCommand(this.mPkgName, 4, 1, null);
        }
    }

    public void startUi() {
        log("startUi()");
        if (this.mHandler.getLooper() != Looper.myLooper()) {
            log("startUi() called off of the UI thread");
        }
        if (this.mVoiceUnlockView != null) {
            this.mVoiceUnlockView.setVisibility(0);
        }
    }

    public void stopUi() {
        log("stopUi()");
        if (this.mVoiceUnlockView != null) {
            this.mVoiceUnlockView.setVisibility(4);
        } else {
            log("mVoiceUnlockView is null in stopUi()");
        }
    }

    public void setKeyguardCallback(KeyguardSecurityCallback keyguardScreenCallback) {
        this.mKeyguardScreenCallback = keyguardScreenCallback;
    }

    public void pokeWakelock(int millis) {
        if (((PowerManager) this.mContext.getSystemService("power")).isScreenOn()) {
            this.mKeyguardScreenCallback.userActivity();
        }
    }

    private void log(String msg) {
        Log.d("VoiceUnlock", msg);
    }
}
