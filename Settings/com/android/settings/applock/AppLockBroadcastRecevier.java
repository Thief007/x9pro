package com.android.settings.applock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AppLockBroadcastRecevier extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        Intent intentService = new Intent();
        intentService.setClass(context, FpService.class);
        context.startService(intentService);
    }
}
