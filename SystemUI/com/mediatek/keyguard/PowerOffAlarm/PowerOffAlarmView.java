package com.mediatek.keyguard.PowerOffAlarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.IWindowManager;
import android.view.IWindowManager.Stub;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardSecurityCallback;
import com.android.keyguard.KeyguardSecurityView;
import com.android.keyguard.R$drawable;
import com.android.keyguard.R$id;
import com.mediatek.keyguard.PowerOffAlarm.multiwaveview.GlowPadView;
import com.mediatek.keyguard.PowerOffAlarm.multiwaveview.GlowPadView.OnTriggerListener;

public class PowerOffAlarmView extends RelativeLayout implements KeyguardSecurityView, OnTriggerListener {
    private final int DELAY_TIME_SECONDS;
    private KeyguardSecurityCallback mCallback;
    private Context mContext;
    private int mFailedPatternAttemptsSinceLastTimeout;
    private GlowPadView mGlowPadView;
    private final Handler mHandler;
    private boolean mIsDocked;
    private boolean mIsRegistered;
    private LockPatternUtils mLockPatternUtils;
    private boolean mPingEnabled;
    private final BroadcastReceiver mReceiver;
    private TextView mTitleView;
    private int mTotalFailedPatternAttempts;

    public PowerOffAlarmView(Context context) {
        this(context, null);
    }

    public PowerOffAlarmView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.DELAY_TIME_SECONDS = 7;
        this.mFailedPatternAttemptsSinceLastTimeout = 0;
        this.mTotalFailedPatternAttempts = 0;
        this.mTitleView = null;
        this.mIsRegistered = false;
        this.mIsDocked = false;
        this.mPingEnabled = true;
        this.mHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 99:
                        if (PowerOffAlarmView.this.mTitleView != null) {
                            PowerOffAlarmView.this.mTitleView.setText(msg.getData().getString("label"));
                            return;
                        }
                        return;
                    case 101:
                        PowerOffAlarmView.this.triggerPing();
                        return;
                    default:
                        return;
                }
            }
        };
        this.mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.v("PowerOffAlarmView", "receive action : " + action);
                if ("update.power.off.alarm.label".equals(action)) {
                    Message msg = new Message();
                    msg.what = 99;
                    Bundle data = new Bundle();
                    data.putString("label", intent.getStringExtra("label"));
                    msg.setData(data);
                    PowerOffAlarmView.this.mHandler.sendMessage(msg);
                } else if (PowerOffAlarmManager.isAlarmBoot()) {
                    PowerOffAlarmView.this.snooze();
                }
            }
        };
        this.mContext = context;
    }

    public void setKeyguardCallback(KeyguardSecurityCallback callback) {
        this.mCallback = callback;
    }

    public void setLockPatternUtils(LockPatternUtils utils) {
        this.mLockPatternUtils = utils;
    }

    protected void onFinishInflate() {
        LockPatternUtils lockPatternUtils;
        super.onFinishInflate();
        Log.w("PowerOffAlarmView", "onFinishInflate ... ");
        setKeepScreenOn(true);
        this.mTitleView = (TextView) findViewById(R$id.alertTitle);
        this.mGlowPadView = (GlowPadView) findViewById(R$id.glow_pad_view);
        this.mGlowPadView.setOnTriggerListener(this);
        setFocusableInTouchMode(true);
        triggerPing();
        Intent dockStatus = this.mContext.registerReceiver(null, new IntentFilter("android.intent.action.DOCK_EVENT"));
        if (dockStatus != null) {
            boolean z;
            if (dockStatus.getIntExtra("android.intent.extra.DOCK_STATE", -1) != 0) {
                z = true;
            } else {
                z = false;
            }
            this.mIsDocked = z;
        }
        IntentFilter filter = new IntentFilter("alarm_killed");
        filter.addAction("com.android.deskclock.ALARM_SNOOZE");
        filter.addAction("com.android.deskclock.ALARM_DISMISS");
        filter.addAction("update.power.off.alarm.label");
        this.mContext.registerReceiver(this.mReceiver, filter);
        if (this.mLockPatternUtils == null) {
            lockPatternUtils = new LockPatternUtils(this.mContext);
        } else {
            lockPatternUtils = this.mLockPatternUtils;
        }
        this.mLockPatternUtils = lockPatternUtils;
        enableEventDispatching(true);
    }

    public void onTrigger(View v, int target) {
        int resId = this.mGlowPadView.getResourceIdForTarget(target);
        if (resId == R$drawable.mtk_ic_alarm_alert_snooze) {
            snooze();
        } else if (resId == R$drawable.mtk_ic_alarm_alert_dismiss_pwroff) {
            powerOff();
        } else if (resId == R$drawable.mtk_ic_alarm_alert_dismiss_pwron) {
            powerOn();
        } else {
            Log.e("PowerOffAlarmView", "Trigger detected on unhandled resource. Skipping.");
        }
    }

    private void triggerPing() {
        if (this.mPingEnabled) {
            this.mGlowPadView.ping();
            this.mHandler.sendEmptyMessageDelayed(101, 1200);
        }
    }

    private void snooze() {
        Log.d("PowerOffAlarmView", "snooze selected");
        sendBR("com.android.deskclock.SNOOZE_ALARM");
    }

    private void powerOn() {
        enableEventDispatching(false);
        Log.d("PowerOffAlarmView", "powerOn selected");
        sendBR("com.android.deskclock.POWER_ON_ALARM");
        sendBR("android.intent.action.normal.boot");
    }

    private void powerOff() {
        Log.d("PowerOffAlarmView", "powerOff selected");
        sendBR("com.android.deskclock.DISMISS_ALARM");
    }

    public boolean onTouchEvent(MotionEvent ev) {
        return super.onTouchEvent(ev);
    }

    public boolean needsInput() {
        return false;
    }

    public void onPause() {
    }

    public void onResume(int reason) {
        reset();
        Log.v("PowerOffAlarmView", "onResume");
    }

    public void onDetachedFromWindow() {
        Log.v("PowerOffAlarmView", "onDetachedFromWindow ....");
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    public void showPromptReason(int reason) {
    }

    public void showMessage(String message, int color) {
    }

    private void enableEventDispatching(boolean flag) {
        try {
            IWindowManager wm = Stub.asInterface(ServiceManager.getService("window"));
            if (wm != null) {
                wm.setEventDispatching(flag);
            }
        } catch (RemoteException e) {
            Log.w("PowerOffAlarmView", e.toString());
        }
    }

    private void sendBR(String action) {
        Log.w("PowerOffAlarmView", "send BR: " + action);
        this.mContext.sendBroadcast(new Intent(action));
    }

    public void onGrabbed(View v, int handle) {
    }

    public void onReleased(View v, int handle) {
    }

    public void onGrabbedStateChange(View v, int handle) {
    }

    public void onFinishFinalAnimation() {
    }

    public void reset() {
    }

    public void startAppearAnimation() {
    }

    public boolean startDisappearAnimation(Runnable finishRunnable) {
        return false;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (PowerOffAlarmManager.isAlarmBoot()) {
            switch (keyCode) {
                case 24:
                    Log.d("PowerOffAlarmView", "onKeyDown() - KeyEvent.KEYCODE_VOLUME_UP, do nothing.");
                    return true;
                case 25:
                    Log.d("PowerOffAlarmView", "onKeyDown() - KeyEvent.KEYCODE_VOLUME_DOWN, do nothing.");
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (PowerOffAlarmManager.isAlarmBoot()) {
            switch (keyCode) {
                case 24:
                    Log.d("PowerOffAlarmView", "onKeyUp() - KeyEvent.KEYCODE_VOLUME_UP, do nothing.");
                    return true;
                case 25:
                    Log.d("PowerOffAlarmView", "onKeyUp() - KeyEvent.KEYCODE_VOLUME_DOWN, do nothing.");
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
