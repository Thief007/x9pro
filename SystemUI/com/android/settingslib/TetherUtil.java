package com.android.settingslib;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.SystemProperties;
import android.provider.Settings.Global;

public class TetherUtil {
    public static ComponentName TETHER_SERVICE = ComponentName.unflattenFromString(Resources.getSystem().getString(17039388));

    public static boolean setWifiTethering(boolean enable, Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService("wifi");
        ContentResolver cr = context.getContentResolver();
        int wifiState = wifiManager.getWifiState();
        if (enable && (wifiState == 2 || wifiState == 3)) {
            wifiManager.setWifiEnabled(false);
            Global.putInt(cr, "wifi_saved_state", 1);
        }
        boolean success = wifiManager.setWifiApEnabled(null, enable);
        if (!enable && Global.getInt(cr, "wifi_saved_state", 0) == 1) {
            wifiManager.setWifiEnabled(true);
            Global.putInt(cr, "wifi_saved_state", 0);
        }
        return success;
    }

    public static boolean isProvisioningNeeded(Context context) {
        boolean z = false;
        String[] provisionApp = context.getResources().getStringArray(17235989);
        if (SystemProperties.getBoolean("net.tethering.noprovisioning", false) || provisionApp == null) {
            return false;
        }
        if (provisionApp.length == 2) {
            z = true;
        }
        return z;
    }

    public static boolean isTetheringSupported(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
        if (ActivityManager.getCurrentUser() != 0) {
            return false;
        }
        return cm.isTetheringSupported();
    }
}
