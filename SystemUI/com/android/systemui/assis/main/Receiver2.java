package com.android.systemui.assis.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class Receiver2 extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        PackageAssistant.active(context);
    }
}
