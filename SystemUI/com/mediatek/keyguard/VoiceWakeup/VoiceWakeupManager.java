package com.mediatek.keyguard.VoiceWakeup;

import android.app.ActivityManagerNative;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings.System;
import android.util.Log;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardHostView.OnDismissAction;
import com.android.keyguard.KeyguardSecurityModel;
import com.android.keyguard.KeyguardSecurityModel.SecurityMode;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.KeyguardUtils;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.assis.app.MAIN.EVENT;
import com.mediatek.common.voicecommand.IVoiceCommandListener;
import com.mediatek.common.voicecommand.IVoiceCommandListener.Stub;
import com.mediatek.common.voicecommand.IVoiceCommandManagerService;
import com.mediatek.keyguard.AntiTheft.AntiTheftManager;

public class VoiceWakeupManager implements OnDismissAction {
    private static boolean delayToLightUpScreen = false;
    private static VoiceWakeupManager sInstance = null;
    private final String ACTION_VOICE_WAKEUP_LAUNCH_INSECURECAMERA_ANYONE = "com.android.keyguard.VoiceWakeupManager.LAUNCH_INSEC_CAMERA_ANYONE";
    private final String ACTION_VOICE_WAKEUP_LAUNCH_INSECURECAMERA_OWNER_ONLY = "com.android.keyguard.VoiceWakeupManager.LAUNCH_INSEC_CAMERA_OWNER";
    private final String ACTION_VOICE_WAKEUP_LAUNCH_MMS_ANYONE = "com.android.keyguard.VoiceWakeupManager.LAUNCH_MMS_ANYONE";
    private final String ACTION_VOICE_WAKEUP_LAUNCH_MMS_OWNER_ONLY = "com.android.keyguard.VoiceWakeupManager.LAUNCH_MMS_OWNER";
    private final String ACTION_VOICE_WAKEUP_LAUNCH_SECURECAMERA_ANYONE = "com.android.keyguard.VoiceWakeupManager.LAUNCH_SEC_CAMERA_ANYONE";
    private final String ACTION_VOICE_WAKEUP_LAUNCH_SECURECAMERA_OWNER_ONLY = "com.android.keyguard.VoiceWakeupManager.LAUNCH_SEC_CAMERA_OWNER";
    private final int COMMAND_ID_LAUNCH_INSECURECAMERA = 2;
    private final int COMMAND_ID_LAUNCH_MMS = 3;
    private final int COMMAND_ID_LAUNCH_SECURECAMERA = 1;
    private final int MSG_VOICE_WAKEUP_LAUNCH_INSECURECAMERA_ANYONE = 1003;
    private final int MSG_VOICE_WAKEUP_LAUNCH_INSECURECAMERA_OWNER_ONLY = 1001;
    private final int MSG_VOICE_WAKEUP_LAUNCH_MMS_ANYONE = 1005;
    private final int MSG_VOICE_WAKEUP_LAUNCH_MMS_OWNER_ONLY = 1004;
    private final int MSG_VOICE_WAKEUP_LAUNCH_SECURECAMERA_ANYONE = 1002;
    private final int MSG_VOICE_WAKEUP_LAUNCH_SECURECAMERA_OWNER_ONLY = EVENT.DYNAMIC_PACK_EVENT_BASE;
    private boolean isRegistered = false;
    private LimitedModeApp[] limitedApps = new LimitedModeApp[]{new LimitedModeApp("com.android.gallery3d/com.android.camera.CameraLauncher", "android.media.action.STILL_IMAGE_CAMERA_SECURE")};
    private final BroadcastReceiver mBroadcastReceiverForTest = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int what = -1;
            if ("com.android.keyguard.VoiceWakeupManager.LAUNCH_SEC_CAMERA_OWNER".equals(action)) {
                what = EVENT.DYNAMIC_PACK_EVENT_BASE;
            } else if ("com.android.keyguard.VoiceWakeupManager.LAUNCH_INSEC_CAMERA_OWNER".equals(action)) {
                what = 1001;
            } else if ("com.android.keyguard.VoiceWakeupManager.LAUNCH_SEC_CAMERA_ANYONE".equals(action)) {
                what = 1002;
            } else if ("com.android.keyguard.VoiceWakeupManager.LAUNCH_INSEC_CAMERA_ANYONE".equals(action)) {
                what = 1003;
            } else if ("com.android.keyguard.VoiceWakeupManager.LAUNCH_MMS_OWNER".equals(action)) {
                what = 1004;
            } else if ("com.android.keyguard.VoiceWakeupManager.LAUNCH_MMS_ANYONE".equals(action)) {
                what = 1005;
            }
            VoiceWakeupManager.this.mVoiceCommandHandlerForTest.obtainMessage(what).sendToTarget();
        }
    };
    private Context mContext = null;
    private Handler mHandler;
    private boolean mIsDismissAndLaunchApp = false;
    private String mLaunchApp;
    private LockPatternUtils mLockPatternUtils;
    private PowerManager mPM;
    private String mPkgName;
    private KeyguardUpdateMonitorCallback mUpdateCallback = new KeyguardUpdateMonitorCallback() {
        public void onKeyguardVisibilityChanged(boolean showing) {
            VoiceWakeupManager.this.log("onKeyguardVisibilityChanged(" + showing + ")");
            if (VoiceWakeupManager.delayToLightUpScreen && !showing) {
                VoiceWakeupManager.this.lightUpScreen();
                VoiceWakeupManager.delayToLightUpScreen = false;
                VoiceWakeupManager.this.mIsDismissAndLaunchApp = false;
            } else if (VoiceWakeupManager.this.mIsDismissAndLaunchApp && VoiceWakeupManager.this.mPM.isScreenOn() && !showing) {
                VoiceWakeupManager.this.log("onKeyguardVisibilityChanged() : Keyguard is hidden now, set mIsDismissAndLaunchApp = false(ex:phone call screen shows)");
                VoiceWakeupManager.this.mIsDismissAndLaunchApp = false;
            }
        }

        public void onFinishedGoingToSleep(int why) {
            VoiceWakeupManager.this.log("onFinishedGoingToSleep - we should reset mIsDismissAndLaunchApp when screen is off.");
            VoiceWakeupManager.this.mIsDismissAndLaunchApp = false;
            VoiceWakeupManager.this.start();
        }
    };
    private IVoiceCommandManagerService mVCmdMgrService;
    private ViewMediatorCallback mViewMediatorCallback;
    private IVoiceCommandListener mVoiceCallback = new Stub() {
        public void onVoiceCommandNotified(int mainAction, int subAction, Bundle extraData) throws RemoteException {
            int result = extraData.getInt("Result");
            VoiceWakeupManager.this.log("onNotified result=" + result + " mainAction = " + mainAction + " subAction = " + subAction);
            if (result == 1 && mainAction == 6 && subAction == 4) {
                Message.obtain(VoiceWakeupManager.this.mVoiceCommandHandler, mainAction, subAction, 0, extraData).sendToTarget();
            }
        }
    };
    private Handler mVoiceCommandHandler = new Handler() {
        public void handleMessage(Message msg) {
            VoiceWakeupManager.this.handleVoiceCommandNotified((Bundle) msg.obj, false);
        }
    };
    private Handler mVoiceCommandHandlerForTest = new Handler() {
        public void handleMessage(Message msg) {
            int commandId = -1;
            int isUserDependentMode = 2;
            switch (msg.what) {
                case EVENT.DYNAMIC_PACK_EVENT_BASE /*1000*/:
                    commandId = 1;
                    isUserDependentMode = 2;
                    break;
                case 1001:
                    commandId = 2;
                    isUserDependentMode = 2;
                    break;
                case 1002:
                    commandId = 1;
                    isUserDependentMode = 1;
                    break;
                case 1003:
                    commandId = 2;
                    isUserDependentMode = 1;
                    break;
                case 1004:
                    commandId = 3;
                    isUserDependentMode = 2;
                    break;
                case 1005:
                    commandId = 3;
                    isUserDependentMode = 1;
                    break;
                default:
                    VoiceWakeupManager.this.log("handleMessage() : msg.what is invalid!");
                    break;
            }
            Bundle data = new Bundle();
            data.putInt("Result_Info", commandId);
            data.putInt("Reslut_Info1", isUserDependentMode);
            VoiceWakeupManager.this.handleVoiceCommandNotified(data, true);
        }
    };
    private ServiceConnection mVoiceServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            VoiceWakeupManager.this.log("onServiceConnected   ");
            VoiceWakeupManager.this.mVCmdMgrService = IVoiceCommandManagerService.Stub.asInterface(service);
            VoiceWakeupManager.this.registerVoiceCommand(VoiceWakeupManager.this.mPkgName);
        }

        public void onServiceDisconnected(ComponentName name) {
            VoiceWakeupManager.this.log("onServiceDisconnected  ");
            VoiceWakeupManager.this.isRegistered = false;
            VoiceWakeupManager.this.mVCmdMgrService = null;
        }
    };
    private KeyguardSecurityModel securityModel;

    private class LimitedModeApp {
        public String limtedModeAppName;
        public String normalModeAppName;

        public LimitedModeApp(String normalName, String limitedName) {
            this.normalModeAppName = normalName;
            this.limtedModeAppName = limitedName;
        }
    }

    public VoiceWakeupManager() {
        Log.d("VoiceWakeupManager", "constructor is called.");
    }

    public static VoiceWakeupManager getInstance() {
        Log.d("VoiceWakeupManager", "getInstance(...) is called.");
        if (sInstance == null) {
            Log.d("VoiceWakeupManager", "getInstance(...) create one.");
            sInstance = new VoiceWakeupManager();
        }
        return sInstance;
    }

    private boolean checkIfVowSupport(Context context) {
        if (context == null) {
            log("checkIfVowSupport() - context is still null, bypass the check...");
            return false;
        } else if (KeyguardUtils.isVoiceWakeupSupport(context)) {
            log("MTK_VOW_SUPPORT is enabled in this load.");
            return true;
        } else {
            log("MTK_VOW_SUPPORT is NOT enabled in this load.");
            return false;
        }
    }

    public void init(Context context, ViewMediatorCallback viewMediatorCallback) {
        log("init() is called.");
        this.mContext = context;
        if (checkIfVowSupport(context)) {
            this.mLockPatternUtils = new LockPatternUtils(context);
            this.mViewMediatorCallback = viewMediatorCallback;
            this.securityModel = new KeyguardSecurityModel(this.mContext);
            this.mPkgName = this.mContext.getPackageName();
            this.mHandler = new Handler();
            this.mPM = (PowerManager) this.mContext.getSystemService("power");
            KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mUpdateCallback);
            registerBroadcastReceiverForTest();
            start();
        }
    }

    public void start() {
        log("start()");
        if (checkIfVowSupport(this.mContext)) {
            log("register to service");
            if (this.mVCmdMgrService == null) {
                bindVoiceService(this.mContext);
            } else {
                registerVoiceCommand(this.mPkgName);
            }
        }
    }

    private void sendVoiceCommand(String pkgName, int mainAction, int subAction, Bundle extraData) {
        if (this.isRegistered) {
            try {
                if (this.mVCmdMgrService.sendCommand(pkgName, mainAction, subAction, extraData) != 0) {
                    log("send voice Command fail ");
                    return;
                } else {
                    log("send voice Command success ");
                    return;
                }
            } catch (RemoteException e) {
                this.isRegistered = false;
                this.mVCmdMgrService = null;
                log("send voice Command RemoteException =  " + e.getMessage());
                return;
            }
        }
        log("didn't register , can not send voice Command  ");
    }

    private void registerVoiceCommand(String pkgName) {
        if (this.isRegistered) {
            log("register voiceCommand success ");
        } else {
            try {
                int errorid = this.mVCmdMgrService.registerListener(pkgName, this.mVoiceCallback);
                if (errorid == 0) {
                    this.isRegistered = true;
                    log("register voiceCommand successfuly, now send VOICE_WAKEUP_START");
                    sendVoiceCommand(this.mPkgName, 6, 1, null);
                } else {
                    log("register voiceCommand fail errorid=" + errorid + " with pkgName=" + pkgName);
                }
            } catch (RemoteException e) {
                this.isRegistered = false;
                this.mVCmdMgrService = null;
                log("register voiceCommand RemoteException =  " + e.getMessage());
            }
        }
        log("register voiceCommand end ");
    }

    private void bindVoiceService(Context context) {
        log("bindVoiceService begin  ");
        Intent mVoiceServiceIntent = new Intent();
        mVoiceServiceIntent.setAction("com.mediatek.voicecommand");
        mVoiceServiceIntent.addCategory("com.mediatek.nativeservice");
        mVoiceServiceIntent.setPackage("com.mediatek.voicecommand");
        context.bindService(mVoiceServiceIntent, this.mVoiceServiceConnection, 1);
    }

    private void handleVoiceCommandNotified(Bundle data, boolean calledFromTest) {
        int commandId = data.getInt("Result_Info");
        boolean isUserDependentMode = data.getInt("Reslut_Info1") == 2;
        log("data.getInt(VoiceCommandListener.ACTION_EXTRA_RESULT_INFO1) = " + data.getInt("Reslut_Info1"));
        log("handleVoiceCommandNotified() commandId = " + commandId + " isUserDependentMode = " + isUserDependentMode);
        doLaunchAppAndDismissKeyguard(commandId, isUserDependentMode, calledFromTest);
    }

    public boolean isDismissAndLaunchApp() {
        log("isDismissAndLaunchApp() mIsDismissAndLaunchApp = " + this.mIsDismissAndLaunchApp);
        return this.mIsDismissAndLaunchApp;
    }

    private String getLimtiedModeActionNameOfApp(String appName) {
        for (int i = 0; i < this.limitedApps.length; i++) {
            if (appName.equals(this.limitedApps[i].normalModeAppName)) {
                return this.limitedApps[i].limtedModeAppName;
            }
        }
        return null;
    }

    private String getLaunchAppNameFromSettings(int commandId, boolean calledFromTest) {
        String appName = null;
        if (!calledFromTest) {
            appName = System.getVoiceCommandValue(this.mContext.getContentResolver(), System.BASE_VOICE_WAKEUP_COMMAND_KEY, commandId);
        } else if (commandId == 1) {
            appName = "com.android.gallery3d/com.android.camera.SecureCameraActivity";
        } else if (commandId == 2) {
            appName = "com.android.gallery3d/com.android.camera.CameraLauncher";
        } else if (commandId == 3) {
            appName = "com.android.dialer/.DialtactsActivity";
        } else {
            log("getLaunchAppNameFromSettings() : wrong commandId = " + commandId);
        }
        Log.d("VoiceWakeupManager", "getLaunchAppNameFromSettings() - appName = " + appName);
        return appName;
    }

    private void doLaunchAppAndDismissKeyguard(int commandId, boolean isUserDependentMode, boolean calledFromTest) {
        this.mIsDismissAndLaunchApp = false;
        this.mLaunchApp = getLaunchAppNameFromSettings(commandId, calledFromTest);
        if (this.mLaunchApp == null) {
            Log.d("VoiceWakeupManager", "AppName does not exist in Setting DB, give it a default value.");
            this.mLaunchApp = "com.android.contacts/com.android.contacts.activities.PeopleActivity";
        }
        AntiTheftManager.getInstance(null, null, null);
        boolean isAntitheftMode = AntiTheftManager.isAntiTheftLocked();
        boolean isKeyguardExternallyDisabled = !this.mViewMediatorCallback.isKeyguardExternallyEnabled();
        if (isAntitheftMode || isKeyguardExternallyDisabled) {
            log("Give up launching since isAntitheftMode = " + isAntitheftMode + " isKeyguardExternallyDisabled = " + isKeyguardExternallyDisabled);
        } else if (this.mPM.isScreenOn()) {
            log("Give up launching since screen is on but we do not allow this case.");
        } else {
            this.mIsDismissAndLaunchApp = true;
            boolean isInLaterLocked = (this.mLockPatternUtils.isLockScreenDisabled(KeyguardUpdateMonitor.getCurrentUser()) || this.mPM.isScreenOn()) ? false : !this.mViewMediatorCallback.isShowing();
            if (isInLaterLocked) {
                log("doLaunchAppAndDismissKeyguard() : call showLocked() due to keyguard isin the later locked status");
                this.mViewMediatorCallback.showLocked(null);
            }
            if (this.mViewMediatorCallback.isShowing() || this.mPM.isScreenOn()) {
            }
            if (this.mLockPatternUtils.isLockScreenDisabled(KeyguardUpdateMonitor.getCurrentUser()) && this.securityModel.getSecurityMode() == SecurityMode.None) {
                log("doLaunchAppAndDismissKeyguard() : Keyguard is DISABLED, launch full-access mode APP and dismiss keyguard.");
                ComponentName cn = ComponentName.unflattenFromString(this.mLaunchApp);
                Intent intent = new Intent();
                intent.setComponent(cn);
                intent.setAction("android.intent.action.MAIN");
                launchApp(intent);
                lightUpScreen();
                this.mIsDismissAndLaunchApp = false;
            } else if (this.mLockPatternUtils.isSecure(KeyguardUpdateMonitor.getCurrentUser()) || this.securityModel.getSecurityMode() != SecurityMode.None) {
                log("doLaunchAppAndDismissKeyguard() : Keyguard is secured.");
                if (!isUserDependentMode) {
                    String limitedModeName = getLimtiedModeActionNameOfApp(this.mLaunchApp);
                    if (limitedModeName != null) {
                        log("doLaunchAppAndDismissKeyguard() : isUserDependentMode = FALSE & APP has limited mode, launch limited-access mode APP");
                        KeyguardUpdateMonitor.getInstance(this.mContext).setAlternateUnlockEnabled(false);
                        launchApp(new Intent(limitedModeName).addFlags(8388608));
                        delayToLightUpScreen = true;
                    } else {
                        log("doLaunchAppAndDismissKeyguard() : isUserDependentMode = FALSE & APP does not have limited mode, light up to request the password");
                        lightUpScreen();
                        this.mViewMediatorCallback.dismiss(false);
                    }
                } else if (isSimPinPukMeModeNow()) {
                    log("doLaunchAppAndDismissKeyguard() : isUserDependentMode = TRUE but SIM PIN/PUK/ME screen shows, light up to request the password.");
                    lightUpScreen();
                } else {
                    log("doLaunchAppAndDismissKeyguard() : isUserDependentMode = TRUE, launch full-access mode APP and dismiss keyguard.");
                    this.mViewMediatorCallback.dismiss(true);
                }
            } else {
                log("doLaunchAppAndDismissKeyguard() : Keyguard is SLIDE mode, launch full-access mode APP and dismiss keyguard.");
                if (ComponentName.unflattenFromString(this.mLaunchApp).getClassName().indexOf("VoiceSearchActivity") != -1) {
                    lightUpScreen();
                }
                this.mViewMediatorCallback.dismiss(true);
            }
        }
    }

    private void dismissKeyguardOnNextActivity() {
        try {
            ActivityManagerNative.getDefault().keyguardWaitingForActivityDrawn();
        } catch (RemoteException e) {
            Log.w("VoiceWakeupManager", "can't dismiss keyguard on launch");
        }
    }

    private void launchApp(final Intent intent) {
        log("launchApp() enters.");
        dismissKeyguardOnNextActivity();
        intent.setFlags(872415232);
        this.mHandler.post(new Runnable() {
            public void run() {
                try {
                    VoiceWakeupManager.this.mContext.startActivityAsUser(intent, new UserHandle(-2));
                    VoiceWakeupManager.this.log("startActivity intent = " + intent.toString());
                } catch (ActivityNotFoundException e) {
                    VoiceWakeupManager.this.log("Activity not found for intent + " + intent.getAction());
                }
            }
        });
    }

    public boolean onDismiss() {
        log("onDismiss() is called.");
        if (!checkIfVowSupport(this.mContext)) {
            return false;
        }
        ComponentName cn = ComponentName.unflattenFromString(this.mLaunchApp);
        Intent intent = new Intent();
        intent.setComponent(cn);
        intent.setAction("android.intent.action.MAIN");
        launchApp(intent);
        return true;
    }

    private boolean isSimPinPukMeModeNow() {
        if (this.securityModel.getSecurityMode() == SecurityMode.SimPinPukMe1 || this.securityModel.getSecurityMode() == SecurityMode.SimPinPukMe2 || this.securityModel.getSecurityMode() == SecurityMode.SimPinPukMe3 || this.securityModel.getSecurityMode() == SecurityMode.SimPinPukMe4) {
            return true;
        }
        return false;
    }

    private void lightUpScreen() {
        log("lightUpScreen() is called.");
        if (this.mIsDismissAndLaunchApp && !this.mPM.isScreenOn()) {
            log("lightUpScreen(), call PowerManager.wakeUp()");
            this.mPM.wakeUp(SystemClock.uptimeMillis());
        }
    }

    public void notifyKeyguardIsGone() {
        log("notifyKeyguardGoneAndLightUpScreen() enters");
        if (checkIfVowSupport(this.mContext)) {
            lightUpScreen();
            this.mIsDismissAndLaunchApp = false;
        }
    }

    public void notifySecurityModeChange(SecurityMode currentMode, SecurityMode nextMode) {
        if (checkIfVowSupport(this.mContext)) {
            log("notifySecurityModeChange curr = " + currentMode + ", next = " + nextMode);
            log("notifySecurityModeChange original mIsDismissAndLaunchApp = " + this.mIsDismissAndLaunchApp);
            if (this.mPM.isScreenOn() && this.mIsDismissAndLaunchApp && (nextMode == SecurityMode.AlarmBoot || nextMode == SecurityMode.AntiTheft)) {
                log("notifySecurityModeChange(): mIsDismissAndLaunchApp = false");
                this.mIsDismissAndLaunchApp = false;
            }
        }
    }

    private void log(String msg) {
        Log.d("VoiceWakeupManager", msg);
    }

    private void registerBroadcastReceiverForTest() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.android.keyguard.VoiceWakeupManager.LAUNCH_SEC_CAMERA_OWNER");
        filter.addAction("com.android.keyguard.VoiceWakeupManager.LAUNCH_INSEC_CAMERA_OWNER");
        filter.addAction("com.android.keyguard.VoiceWakeupManager.LAUNCH_SEC_CAMERA_ANYONE");
        filter.addAction("com.android.keyguard.VoiceWakeupManager.LAUNCH_INSEC_CAMERA_ANYONE");
        filter.addAction("com.android.keyguard.VoiceWakeupManager.LAUNCH_MMS_OWNER");
        filter.addAction("com.android.keyguard.VoiceWakeupManager.LAUNCH_MMS_ANYONE");
        this.mContext.registerReceiver(this.mBroadcastReceiverForTest, filter);
    }
}
