package com.android.systemui.statusbar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import com.android.internal.app.IBatteryStats;
import com.android.internal.app.IBatteryStats.Stub;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitor.BatteryStatus;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.KeyguardIndicationTextView;
import com.android.systemui.statusbar.phone.LockIcon;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;

public class KeyguardIndicationController {
    private final IBatteryStats mBatteryInfo;
    private final Context mContext;
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1 && KeyguardIndicationController.this.mTransientIndication != null) {
                KeyguardIndicationController.this.mTransientIndication = null;
                KeyguardIndicationController.this.updateIndication();
            } else if (msg.what == 2) {
                KeyguardIndicationController.this.mLockIcon.setTransientFpError(false);
                KeyguardIndicationController.this.hideTransientIndication();
            }
        }
    };
    private final LockIcon mLockIcon;
    private String mMessageToShowOnScreenOn;
    private boolean mPowerCharged;
    private boolean mPowerPluggedIn;
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (KeyguardIndicationController.this.mVisible) {
                KeyguardIndicationController.this.updateIndication();
            }
        }
    };
    private String mRestingIndication;
    private StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private final KeyguardIndicationTextView mTextView;
    private String mTransientIndication;
    private int mTransientTextColor;
    KeyguardUpdateMonitorCallback mUpdateMonitor = new KeyguardUpdateMonitorCallback() {
        public void onRefreshBatteryInfo(BatteryStatus status) {
            boolean isChargingOrFull = status.status != 2 ? status.status == 5 : true;
            KeyguardIndicationController keyguardIndicationController = KeyguardIndicationController.this;
            if (!status.isPluggedIn()) {
                isChargingOrFull = false;
            }
            keyguardIndicationController.mPowerPluggedIn = isChargingOrFull;
            KeyguardIndicationController.this.mPowerCharged = status.isCharged();
            KeyguardIndicationController.this.updateIndication();
        }

        public void onFingerprintHelp(int msgId, String helpString) {
            KeyguardUpdateMonitor updateMonitor = KeyguardUpdateMonitor.getInstance(KeyguardIndicationController.this.mContext);
            if (updateMonitor.isUnlockingWithFingerprintAllowed()) {
                int errorColor = KeyguardIndicationController.this.mContext.getResources().getColor(R.color.system_warning_color, null);
                if (KeyguardIndicationController.this.mStatusBarKeyguardViewManager.isBouncerShowing()) {
                    KeyguardIndicationController.this.mStatusBarKeyguardViewManager.showBouncerMessage(helpString, errorColor);
                } else if (updateMonitor.isDeviceInteractive()) {
                    KeyguardIndicationController.this.mLockIcon.setTransientFpError(true);
                    KeyguardIndicationController.this.showTransientIndication(helpString, errorColor);
                    KeyguardIndicationController.this.mHandler.removeMessages(2);
                    KeyguardIndicationController.this.mHandler.sendMessageDelayed(KeyguardIndicationController.this.mHandler.obtainMessage(2), 1300);
                }
            }
        }

        public void onFingerprintError(int msgId, String errString) {
            KeyguardUpdateMonitor updateMonitor = KeyguardUpdateMonitor.getInstance(KeyguardIndicationController.this.mContext);
            if (updateMonitor.isUnlockingWithFingerprintAllowed() && msgId != 5) {
                int errorColor = KeyguardIndicationController.this.mContext.getResources().getColor(R.color.system_warning_color, null);
                if (KeyguardIndicationController.this.mStatusBarKeyguardViewManager.isBouncerShowing()) {
                    KeyguardIndicationController.this.mStatusBarKeyguardViewManager.showBouncerMessage(errString, errorColor);
                } else if (updateMonitor.isDeviceInteractive()) {
                    KeyguardIndicationController.this.showTransientIndication(errString, errorColor);
                    KeyguardIndicationController.this.mHandler.removeMessages(1);
                    KeyguardIndicationController.this.hideTransientIndicationDelayed(5000);
                } else {
                    KeyguardIndicationController.this.mMessageToShowOnScreenOn = errString;
                }
            }
        }

        public void onScreenTurnedOn() {
            if (KeyguardIndicationController.this.mMessageToShowOnScreenOn != null) {
                KeyguardIndicationController.this.showTransientIndication(KeyguardIndicationController.this.mMessageToShowOnScreenOn, KeyguardIndicationController.this.mContext.getResources().getColor(R.color.system_warning_color, null));
                KeyguardIndicationController.this.mHandler.removeMessages(1);
                KeyguardIndicationController.this.hideTransientIndicationDelayed(5000);
                KeyguardIndicationController.this.mMessageToShowOnScreenOn = null;
            }
        }

        public void onFingerprintRunningStateChanged(boolean running) {
            if (running) {
                KeyguardIndicationController.this.mMessageToShowOnScreenOn = null;
            }
        }
    };
    private boolean mVisible;

    public KeyguardIndicationController(Context context, KeyguardIndicationTextView textView, LockIcon lockIcon) {
        this.mContext = context;
        this.mTextView = textView;
        this.mLockIcon = lockIcon;
        this.mBatteryInfo = Stub.asInterface(ServiceManager.getService("batterystats"));
        KeyguardUpdateMonitor.getInstance(context).registerCallback(this.mUpdateMonitor);
        context.registerReceiverAsUser(this.mReceiver, UserHandle.OWNER, new IntentFilter("android.intent.action.TIME_TICK"), null, null);
    }

    public void setVisible(boolean visible) {
        this.mVisible = visible;
        this.mTextView.setVisibility(visible ? 0 : 8);
        if (visible) {
            hideTransientIndication();
            updateIndication();
        }
    }

    public void hideTransientIndicationDelayed(long delayMs) {
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), delayMs);
    }

    public void showTransientIndication(int transientIndication) {
        showTransientIndication(this.mContext.getResources().getString(transientIndication));
    }

    public void showTransientIndication(String transientIndication) {
        showTransientIndication(transientIndication, -1);
    }

    public void showTransientIndication(String transientIndication, int textColor) {
        this.mTransientIndication = transientIndication;
        this.mTransientTextColor = textColor;
        this.mHandler.removeMessages(1);
        updateIndication();
    }

    public void hideTransientIndication() {
        if (this.mTransientIndication != null) {
            this.mTransientIndication = null;
            this.mHandler.removeMessages(1);
            updateIndication();
        }
    }

    private void updateIndication() {
        if (this.mVisible) {
            this.mTextView.switchIndication(computeIndication());
            this.mTextView.setTextColor(computeColor());
        }
    }

    private int computeColor() {
        if (TextUtils.isEmpty(this.mTransientIndication)) {
            return -1;
        }
        return this.mTransientTextColor;
    }

    private String computeIndication() {
        if (!TextUtils.isEmpty(this.mTransientIndication)) {
            return this.mTransientIndication;
        }
        if (this.mPowerPluggedIn) {
            return computePowerIndication();
        }
        return this.mRestingIndication;
    }

    private String computePowerIndication() {
        if (this.mPowerCharged) {
            return this.mContext.getResources().getString(R.string.keyguard_charged);
        }
        try {
            long chargingTimeRemaining = this.mBatteryInfo.computeChargeTimeRemaining();
            if (chargingTimeRemaining > 0) {
                String chargingTimeFormatted = Formatter.formatShortElapsedTimeRoundingUpToMinutes(this.mContext, chargingTimeRemaining);
                return this.mContext.getResources().getString(R.string.keyguard_indication_charging_time, new Object[]{chargingTimeFormatted});
            }
        } catch (RemoteException e) {
            Log.e("KeyguardIndicationController", "Error calling IBatteryStats: ", e);
        }
        return this.mContext.getResources().getString(R.string.keyguard_plugged_in);
    }

    public void setStatusBarKeyguardViewManager(StatusBarKeyguardViewManager statusBarKeyguardViewManager) {
        this.mStatusBarKeyguardViewManager = statusBarKeyguardViewManager;
    }
}
