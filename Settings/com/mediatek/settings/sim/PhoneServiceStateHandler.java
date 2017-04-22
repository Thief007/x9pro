package com.mediatek.settings.sim;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PhoneServiceStateHandler {
    private Listener mListenerCallBack;
    private Map<Integer, PhoneStateListener> mListeners = new ConcurrentHashMap();
    private int[] mSubs;
    private SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelephonyManager;

    public interface Listener {
        void onServiceStateChanged(ServiceState serviceState, int i);
    }

    public PhoneServiceStateHandler(Context context) {
        this.mTelephonyManager = TelephonyManager.from(context);
        this.mSubscriptionManager = SubscriptionManager.from(context);
        this.mSubs = this.mSubscriptionManager.getActiveSubscriptionIdList();
    }

    public void registerOnPhoneServiceStateChange(Listener listener) {
        this.mListenerCallBack = listener;
        registerPhoneStateListener();
    }

    public void unregisterOnPhoneServiceStateChange() {
        this.mListenerCallBack = null;
        unregisterPhoneStateListener();
    }

    private void registerPhoneStateListener() {
        for (int valueOf : this.mSubs) {
            registerPhoneStateListener(Integer.valueOf(valueOf).intValue());
        }
    }

    private void registerPhoneStateListener(int subId) {
        Log.d("PhoneServiceStateHandler", "Register PhoneStateListener, subId : " + subId);
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            PhoneStateListener phoneStateListener = getPhoneStateListener(subId);
            this.mListeners.put(Integer.valueOf(subId), phoneStateListener);
            this.mTelephonyManager.listen(phoneStateListener, 1);
            return;
        }
        Log.d("PhoneServiceStateHandler", "invalid subId: " + subId);
    }

    private PhoneStateListener getPhoneStateListener(final int subId) {
        return new PhoneStateListener(subId) {
            public void onServiceStateChanged(ServiceState state) {
                Log.d("PhoneServiceStateHandler", "PhoneStateListener:onServiceStateChanged: subId: " + subId + ", state: " + state);
                if (PhoneServiceStateHandler.this.mListenerCallBack != null) {
                    PhoneServiceStateHandler.this.mListenerCallBack.onServiceStateChanged(state, subId);
                }
            }
        };
    }

    private void unregisterPhoneStateListener() {
        for (Integer intValue : this.mListeners.keySet()) {
            unregisterPhoneStateListener(intValue.intValue());
        }
    }

    private void unregisterPhoneStateListener(int subId) {
        Log.d("PhoneServiceStateHandler", "Register unregisterPhoneStateListener subId : " + subId);
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            this.mTelephonyManager.listen((PhoneStateListener) this.mListeners.get(Integer.valueOf(subId)), 0);
            this.mListeners.remove(Integer.valueOf(subId));
            return;
        }
        Log.d("PhoneServiceStateHandler", "invalid subId: " + subId);
    }
}
