package com.android.keyguard;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import com.android.keyguard.KeyguardSecurityModel.SecurityMode;
import com.mediatek.keyguard.AntiTheft.AntiTheftManager;
import java.lang.ref.WeakReference;

public class KeyguardMessageArea extends TextView implements SecurityMessageDisplay {
    private static final Object ANNOUNCE_TOKEN = new Object();
    private final Runnable mClearMessageRunnable;
    private final int mDefaultColor;
    private final Handler mHandler;
    private KeyguardUpdateMonitorCallback mInfoCallback;
    CharSequence mMessage;
    private int mNextMessageColor;
    private KeyguardSecurityModel mSecurityModel;
    private CharSequence mSeparator;
    long mTimeout;
    private final KeyguardUpdateMonitor mUpdateMonitor;

    private static class AnnounceRunnable implements Runnable {
        private final WeakReference<View> mHost;
        private final CharSequence mTextToAnnounce;

        AnnounceRunnable(View host, CharSequence textToAnnounce) {
            this.mHost = new WeakReference(host);
            this.mTextToAnnounce = textToAnnounce;
        }

        public void run() {
            View host = (View) this.mHost.get();
            if (host != null) {
                host.announceForAccessibility(this.mTextToAnnounce);
            }
        }
    }

    public KeyguardMessageArea(Context context) {
        this(context, null);
    }

    public KeyguardMessageArea(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mTimeout = 5000;
        this.mNextMessageColor = -1;
        this.mClearMessageRunnable = new Runnable() {
            public void run() {
                KeyguardMessageArea.this.mMessage = null;
                KeyguardMessageArea.this.update();
            }
        };
        this.mInfoCallback = new KeyguardUpdateMonitorCallback() {
            public void onFinishedGoingToSleep(int why) {
                KeyguardMessageArea.this.setSelected(false);
            }

            public void onStartedWakingUp() {
                KeyguardMessageArea.this.setSelected(true);
            }
        };
        setLayerType(2, null);
        this.mSecurityModel = new KeyguardSecurityModel(context);
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(getContext());
        this.mUpdateMonitor.registerCallback(this.mInfoCallback);
        this.mHandler = new Handler(Looper.myLooper());
        this.mSeparator = getResources().getString(17040612);
        this.mDefaultColor = getCurrentTextColor();
        update();
    }

    public void setNextMessageColor(int color) {
        this.mNextMessageColor = color;
    }

    public void setMessage(CharSequence msg, boolean important) {
        if (TextUtils.isEmpty(msg) || !important) {
            clearMessage();
        } else {
            securityMessageChanged(msg);
        }
    }

    public void setMessage(int resId, boolean important) {
        if (resId == 0 || !important) {
            clearMessage();
        } else {
            securityMessageChanged(getContext().getResources().getText(resId));
        }
    }

    public void setMessage(int resId, boolean important, Object... formatArgs) {
        if (resId == 0 || !important) {
            clearMessage();
        } else {
            securityMessageChanged(getContext().getString(resId, formatArgs));
        }
    }

    public void setTimeout(int timeoutMs) {
        this.mTimeout = (long) timeoutMs;
    }

    public static SecurityMessageDisplay findSecurityMessageDisplay(View v) {
        KeyguardMessageArea messageArea = (KeyguardMessageArea) v.findViewById(R$id.keyguard_message_area);
        if (messageArea != null) {
            return messageArea;
        }
        throw new RuntimeException("Can't find keyguard_message_area in " + v.getClass());
    }

    protected void onFinishInflate() {
        setSelected(KeyguardUpdateMonitor.getInstance(this.mContext).isDeviceInteractive());
    }

    private void securityMessageChanged(CharSequence message) {
        this.mMessage = message;
        update();
        this.mHandler.removeCallbacks(this.mClearMessageRunnable);
        if (this.mTimeout > 0) {
            this.mHandler.postDelayed(this.mClearMessageRunnable, this.mTimeout);
        }
        this.mHandler.removeCallbacksAndMessages(ANNOUNCE_TOKEN);
        this.mHandler.postAtTime(new AnnounceRunnable(this, getText()), ANNOUNCE_TOKEN, SystemClock.uptimeMillis() + 250);
    }

    private void clearMessage() {
        this.mHandler.removeCallbacks(this.mClearMessageRunnable);
        this.mHandler.post(this.mClearMessageRunnable);
    }

    private void update() {
        CharSequence status = this.mMessage;
        setVisibility(TextUtils.isEmpty(status) ? 4 : 0);
        if (this.mSecurityModel.getSecurityMode() == SecurityMode.AntiTheft) {
            setText(AntiTheftManager.getAntiTheftMessageAreaText(status, this.mSeparator));
        } else {
            setText(status);
        }
        int color = this.mDefaultColor;
        if (this.mNextMessageColor != -1) {
            color = this.mNextMessageColor;
            this.mNextMessageColor = -1;
        }
        setTextColor(color);
    }
}
