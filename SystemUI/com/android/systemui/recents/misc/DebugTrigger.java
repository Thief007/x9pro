package com.android.systemui.recents.misc;

import android.os.Handler;

public class DebugTrigger {
    Handler mHandler = new Handler();
    int mLastKeyCode;
    long mLastKeyCodeTime;
    Runnable mTriggeredRunnable;

    public DebugTrigger(Runnable triggeredRunnable) {
        this.mTriggeredRunnable = triggeredRunnable;
    }

    void reset() {
        this.mLastKeyCode = 0;
        this.mLastKeyCodeTime = 0;
    }

    public void onKeyEvent(int keyCode) {
    }
}
