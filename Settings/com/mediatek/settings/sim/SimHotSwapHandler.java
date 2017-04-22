package com.mediatek.settings.sim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SubscriptionManager;
import android.util.Log;
import java.util.Arrays;

public class SimHotSwapHandler {
    private Context mContext;
    private OnSimHotSwapListener mListener;
    private BroadcastReceiver mSubReceiver = new C07401();
    private int[] mSubscriptionIdListCache;
    private SubscriptionManager mSubscriptionManager;

    public interface OnSimHotSwapListener {
        void onSimHotSwap();
    }

    class C07401 extends BroadcastReceiver {
        C07401() {
        }

        public void onReceive(Context context, Intent intent) {
            SimHotSwapHandler.this.handleHotSwap();
        }
    }

    public SimHotSwapHandler(Context context) {
        this.mContext = context;
        this.mSubscriptionManager = SubscriptionManager.from(context);
        this.mSubscriptionIdListCache = this.mSubscriptionManager.getActiveSubscriptionIdList();
        print("Cache list: ", this.mSubscriptionIdListCache);
    }

    public void registerOnSimHotSwap(OnSimHotSwapListener listener) {
        if (this.mContext != null) {
            this.mContext.registerReceiver(this.mSubReceiver, new IntentFilter("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED"));
            this.mListener = listener;
        }
    }

    public void unregisterOnSimHotSwap() {
        if (this.mContext != null) {
            this.mContext.unregisterReceiver(this.mSubReceiver);
        }
        this.mListener = null;
    }

    private void handleHotSwap() {
        int[] subscriptionIdListCurrent = this.mSubscriptionManager.getActiveSubscriptionIdList();
        print("handleHotSwap, current subId list: ", subscriptionIdListCurrent);
        boolean isEqual = Arrays.equals(this.mSubscriptionIdListCache, subscriptionIdListCurrent);
        Log.d("SimHotSwapHandler", "isEqual: " + isEqual);
        if (!isEqual && this.mListener != null) {
            this.mListener.onSimHotSwap();
        }
    }

    private void print(String msg, int[] lists) {
        if (lists != null) {
            for (int i : lists) {
                Log.d("SimHotSwapHandler", msg + i);
            }
            return;
        }
        Log.d("SimHotSwapHandler", msg + "is null");
    }
}
