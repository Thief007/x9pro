package com.mediatek.keyguard.PowerOffAlarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.ViewMediatorCallback;
import com.mediatek.systemui.statusbar.extcb.FeatureOptionUtils;

public class PowerOffAlarmManager {
    private static PowerOffAlarmManager sInstance;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.LAUNCH_POWEROFF_ALARM".equals(action)) {
                Log.d("PowerOffAlarmManager", "LAUNCH_PWROFF_ALARM: " + action);
                PowerOffAlarmManager.this.mHandler.sendEmptyMessageDelayed(115, 1500);
            } else if ("android.intent.action.normal.boot".equals(action)) {
                Log.d("PowerOffAlarmManager", "NORMAL_BOOT_ACTION: " + action);
                PowerOffAlarmManager.this.mHandler.sendEmptyMessageDelayed(116, 2500);
            } else if ("android.intent.action.normal.shutdown".equals(action)) {
                Log.w("PowerOffAlarmManager", "ACTION_SHUTDOWN: " + action);
                PowerOffAlarmManager.this.mHandler.postDelayed(new Runnable() {
                    public void run() {
                        PowerOffAlarmManager.this.mViewMediatorCallback.hideLocked();
                    }
                }, 1500);
            }
        }
    };
    private Context mContext;
    private Handler mHandler = new Handler(Looper.myLooper(), null, true) {
        private String getMessageString(Message message) {
            switch (message.what) {
                case 115:
                    return "ALARM_BOOT";
                case 116:
                    return "RESHOW_KEYGUARD_LOCK";
                default:
                    return null;
            }
        }

        public void handleMessage(Message msg) {
            Log.d("PowerOffAlarmManager", "handleMessage enter msg name=" + getMessageString(msg));
            switch (msg.what) {
                case 115:
                    PowerOffAlarmManager.this.handleAlarmBoot();
                    break;
                case 116:
                    PowerOffAlarmManager.this.mViewMediatorCallback.setSuppressPlaySoundFlag();
                    PowerOffAlarmManager.this.mViewMediatorCallback.hideLocked();
                    postDelayed(new Runnable() {
                        public void run() {
                            if (!PowerOffAlarmManager.this.mLockPatternUtils.isLockScreenDisabled(KeyguardUpdateMonitor.getCurrentUser()) || PowerOffAlarmManager.this.mViewMediatorCallback.isSecure()) {
                                PowerOffAlarmManager.this.mViewMediatorCallback.setSuppressPlaySoundFlag();
                                PowerOffAlarmManager.this.mViewMediatorCallback.showLocked(null);
                            }
                        }
                    }, 2000);
                    postDelayed(new Runnable() {
                        public void run() {
                            PowerOffAlarmManager.this.mContext.sendBroadcast(new Intent("android.intent.action.normal.boot.done"));
                        }
                    }, 4000);
                    break;
            }
            Log.d("PowerOffAlarmManager", "handleMessage exit msg name=" + getMessageString(msg));
        }
    };
    private LockPatternUtils mLockPatternUtils;
    private boolean mNeedToShowAlarmView = false;
    private Runnable mSendRemoveIPOWinBroadcastRunnable = new Runnable() {
        public void run() {
            Log.d("PowerOffAlarmManager", "sendRemoveIPOWinBroadcast ... ");
            PowerOffAlarmManager.this.mContext.sendBroadcast(new Intent("alarm.boot.remove.ipowin"));
        }
    };
    private boolean mSystemReady = false;
    private ViewMediatorCallback mViewMediatorCallback;

    public PowerOffAlarmManager(Context context, ViewMediatorCallback viewMediatorCallback, LockPatternUtils lockPatternUtils) {
        this.mContext = context;
        this.mViewMediatorCallback = viewMediatorCallback;
        this.mLockPatternUtils = lockPatternUtils;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.normal.shutdown");
        filter.addAction("android.intent.action.LAUNCH_POWEROFF_ALARM");
        filter.addAction("android.intent.action.normal.boot");
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
    }

    public static PowerOffAlarmManager getInstance(Context context, ViewMediatorCallback viewMediatorCallback, LockPatternUtils lockPatternUtils) {
        if (sInstance == null) {
            sInstance = new PowerOffAlarmManager(context, viewMediatorCallback, lockPatternUtils);
        }
        return sInstance;
    }

    private void handleAlarmBoot() {
        Log.d("PowerOffAlarmManager", "handleAlarmBoot");
        this.mNeedToShowAlarmView = true;
        maybeShowAlarmView();
    }

    public void startAlarm() {
        startAlarmService();
        this.mHandler.postDelayed(this.mSendRemoveIPOWinBroadcastRunnable, 1500);
    }

    private void startAlarmService() {
        Intent in = new Intent("com.android.deskclock.START_ALARM");
        in.putExtra("isAlarmBoot", true);
        in.setPackage("com.android.deskclock");
        this.mContext.startService(in);
    }

    public static boolean isAlarmBoot() {
        String bootReason = SystemProperties.get("sys.boot.reason");
        if (bootReason == null || !bootReason.equals(FeatureOptionUtils.SUPPORT_YES)) {
            return false;
        }
        return true;
    }

    public void onSystemReady() {
        this.mSystemReady = true;
        maybeShowAlarmView();
    }

    private void maybeShowAlarmView() {
        if (this.mSystemReady && this.mNeedToShowAlarmView) {
            this.mNeedToShowAlarmView = false;
            Log.d("PowerOffAlarmManager", "maybeShowAlarmView start to showLocked");
            if (this.mViewMediatorCallback.isShowing()) {
                this.mViewMediatorCallback.setSuppressPlaySoundFlag();
                this.mViewMediatorCallback.hideLocked();
            }
            this.mViewMediatorCallback.showLocked(null);
        }
    }
}
