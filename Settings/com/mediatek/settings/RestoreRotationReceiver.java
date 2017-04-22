package com.mediatek.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings.System;
import android.util.Log;

public class RestoreRotationReceiver extends BroadcastReceiver {
    public static boolean sRestoreRetore = false;

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.v("RestoreRotationReceiver_IPO", action);
        if (action.equals("android.intent.action.BOOT_COMPLETED") || action.equals("android.intent.action.ACTION_BOOT_IPO") || action.equals("android.intent.action.USER_SWITCHED_FOR_MULTIUSER_APP")) {
            boolean z;
            if (System.getIntForUser(context.getContentResolver(), "accelerometer_rotation_restore", 0, -2) != 0) {
                z = true;
            } else {
                z = false;
            }
            sRestoreRetore = z;
            if (sRestoreRetore) {
                System.putIntForUser(context.getContentResolver(), "accelerometer_rotation", 1, -2);
                System.putIntForUser(context.getContentResolver(), "accelerometer_rotation_restore", 0, -2);
            }
        }
    }
}
