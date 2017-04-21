package com.android.systemui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.util.Log;

public class StartTorchBroadcast extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        if (SystemProperties.getBoolean("persist.sys.home_torch", true)) {
            Log.i("fenghaitao", "====广播启动===");
            Intent torchIntent = new Intent();
            torchIntent.setClass(context, CustomTorchActivity.class);
            torchIntent.addFlags(268435456);
            context.startActivity(torchIntent);
        }
    }
}
