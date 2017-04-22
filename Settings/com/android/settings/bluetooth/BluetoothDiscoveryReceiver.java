package com.android.settings.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public final class BluetoothDiscoveryReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.v("BluetoothDiscoveryReceiver", "Received: " + action);
        if (action.equals("android.bluetooth.adapter.action.DISCOVERY_STARTED") || action.equals("android.bluetooth.adapter.action.DISCOVERY_FINISHED")) {
            LocalBluetoothPreferences.persistDiscoveringTimestamp(context);
        }
    }
}
