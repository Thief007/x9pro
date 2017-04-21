package com.android.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.GlobalSetting;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSTile.Host;
import com.android.systemui.qs.QSTile.Icon;
import com.android.systemui.qs.QSTile.ResourceIcon;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.internal.telephony.ITelephonyEx.Stub;

public class AirplaneModeTile extends QSTile<BooleanState> {
    private AnimationHandler mAnimHandler = new AnimationHandler();
    private Icon[] mAnimMembers = new Icon[]{ResourceIcon.get(R.drawable.ic_signal_airplane_swiching_2), ResourceIcon.get(R.drawable.ic_signal_airplane_swiching_3)};
    private int mCount;
    private final AnimationIcon mDisable = new AnimationIcon(R.drawable.ic_signal_airplane_disable_animation);
    private final AnimationIcon mEnable = new AnimationIcon(R.drawable.ic_signal_airplane_enable_animation);
    private boolean mListening;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (!"android.intent.action.AIRPLANE_MODE".equals(intent.getAction()) && "com.mediatek.intent.action.AIRPLANE_CHANGE_DONE".equals(intent.getAction())) {
                Log.d(AirplaneModeTile.this.TAG, "onReceive() AIRPLANE_CHANGE_DONE,  mAirplaneModeOn= " + intent.getBooleanExtra("airplaneMode", false));
                AirplaneModeTile.this.stopAnimation();
                AirplaneModeTile.this.refreshState();
            }
        }
    };
    private final GlobalSetting mSetting = new GlobalSetting(this.mContext, this.mHandler, "airplane_mode_on") {
        protected void handleValueChanged(int value) {
            AirplaneModeTile.this.handleRefreshState(Integer.valueOf(value));
        }
    };
    private boolean mSwitching;

    private class AnimationHandler extends Handler {
        private AnimationHandler() {
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d(AirplaneModeTile.this.TAG, "AnimationHandler handleMessage()");
            AirplaneModeTile.this.refreshState();
            AirplaneModeTile.this.mAnimHandler.sendEmptyMessageDelayed(0, 400);
            AirplaneModeTile airplaneModeTile = AirplaneModeTile.this;
            int -get2 = airplaneModeTile.mCount;
            airplaneModeTile.mCount = -get2 + 1;
            if (-get2 >= 100) {
                AirplaneModeTile.this.mCount = 0;
            }
        }
    }

    public AirplaneModeTile(Host host) {
        super(host);
    }

    protected BooleanState newTileState() {
        return new BooleanState();
    }

    public void handleClick() {
        boolean z = false;
        if (!this.mSwitching) {
            boolean z2;
            startAnimation();
            Context context = this.mContext;
            int metricsCategory = getMetricsCategory();
            if (((BooleanState) this.mState).value) {
                z2 = false;
            } else {
                z2 = true;
            }
            MetricsLogger.action(context, metricsCategory, z2);
            if (!((BooleanState) this.mState).value) {
                z = true;
            }
            setEnabled(z);
            this.mDisable.setAllowAnimation(true);
        }
    }

    private void setEnabled(boolean enabled) {
        Log.d(this.TAG, "setEnabled = " + enabled);
        ((ConnectivityManager) this.mContext.getSystemService("connectivity")).setAirplaneMode(enabled);
    }

    protected void handleUpdateState(BooleanState state, Object arg) {
        boolean airplaneMode = (arg instanceof Integer ? ((Integer) arg).intValue() : this.mSetting.getValue()) != 0;
        state.value = airplaneMode;
        state.visible = true;
        state.label = this.mContext.getString(R.string.airplane_mode);
        if (airplaneMode) {
            state.icon = this.mEnable;
            state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_airplane_on);
        } else {
            state.icon = this.mDisable;
            state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_airplane_off);
        }
        handleAnimationState(state, arg);
    }

    public int getMetricsCategory() {
        return 112;
    }

    protected String composeChangeAnnouncement() {
        if (((BooleanState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_quick_settings_airplane_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_airplane_changed_off);
    }

    public void setListening(boolean listening) {
        if (this.mListening != listening) {
            this.mListening = listening;
            if (listening) {
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.AIRPLANE_MODE");
                filter.addAction("com.mediatek.intent.action.AIRPLANE_CHANGE_DONE");
                this.mContext.registerReceiver(this.mReceiver, filter);
                if (!isAirplanemodeAvailableNow()) {
                    Log.d(this.TAG, "setListening() Airplanemode not Available, start anim.");
                    startAnimation();
                }
            } else {
                this.mContext.unregisterReceiver(this.mReceiver);
                stopAnimation();
            }
            this.mSetting.setListening(listening);
        }
    }

    private void startAnimation() {
        stopAnimation();
        this.mSwitching = true;
        this.mAnimHandler.sendEmptyMessage(0);
        Log.d(this.TAG, "startAnimation()");
    }

    private void stopAnimation() {
        this.mSwitching = false;
        this.mCount = 0;
        if (this.mAnimHandler.hasMessages(0)) {
            this.mAnimHandler.removeMessages(0);
        }
        Log.d(this.TAG, "stopAnimation()");
    }

    private void handleAnimationState(BooleanState state, Object arg) {
        Log.d(this.TAG, "handleAnimationState() mSwitching= " + this.mSwitching + ", mCount= " + this.mCount);
        if (this.mSwitching) {
            state.icon = this.mAnimMembers[this.mCount % 2];
        }
    }

    private boolean isAirplanemodeAvailableNow() {
        ITelephonyEx telephonyEx = Stub.asInterface(ServiceManager.getService("phoneEx"));
        boolean isAvailable = false;
        if (telephonyEx != null) {
            try {
                isAvailable = telephonyEx.isAirplanemodeAvailableNow();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        Log.d(this.TAG, "isAirplaneModeAvailable = " + isAvailable);
        return isAvailable;
    }
}
