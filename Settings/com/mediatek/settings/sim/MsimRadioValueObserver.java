package com.mediatek.settings.sim;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings.System;
import android.util.Log;

public class MsimRadioValueObserver {
    private ContentResolver mContentObserver;
    private Context mContext;
    private Listener mListener;
    private ContentObserver mMsimModeValue = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            int mSimMode = System.getInt(MsimRadioValueObserver.this.mContext.getContentResolver(), "msim_mode_setting", -1);
            Log.d("MsimRadioValueObserver", "onChange, mSimMode: " + mSimMode);
            if (MsimRadioValueObserver.this.mListener != null) {
                MsimRadioValueObserver.this.mListener.onChange(mSimMode, selfChange);
            } else {
                Log.d("MsimRadioValueObserver", "mListener has been ungistered");
            }
        }
    };

    public interface Listener {
        void onChange(int i, boolean z);
    }

    public MsimRadioValueObserver(Context context) {
        this.mContext = context;
        this.mContentObserver = this.mContext.getContentResolver();
    }

    public void registerMsimObserver(Listener listener) {
        this.mListener = listener;
        registerContentObserver();
    }

    public void ungisterMsimObserver() {
        this.mListener = null;
        unregisterContentObserver();
    }

    private void registerContentObserver() {
        if (this.mContentObserver != null) {
            this.mContentObserver.registerContentObserver(System.getUriFor("msim_mode_setting"), false, this.mMsimModeValue);
        } else {
            Log.d("MsimRadioValueObserver", "observer is null");
        }
    }

    private void unregisterContentObserver() {
        if (this.mContentObserver != null) {
            this.mContentObserver.unregisterContentObserver(this.mMsimModeValue);
        } else {
            Log.d("MsimRadioValueObserver", "observer is null");
        }
    }
}
