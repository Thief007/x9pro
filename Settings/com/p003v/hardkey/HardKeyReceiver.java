package com.p003v.hardkey;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class HardKeyReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null && !action.equals("android.intent.action.BOOT_COMPLETED")) {
        }
    }
}
