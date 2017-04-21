package com.mediatek.settingslib.wifi;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import com.mediatek.common.MPlugin;
import com.mediatek.settingslib.ext.DefaultWifiLibExt;
import com.mediatek.settingslib.ext.IWifiLibExt;

public class AccessPointExt {
    public static IWifiLibExt sWifiLibExt;

    public AccessPointExt(Context context) {
        getWifiPlugin(context);
    }

    public static int getSecurity(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(5)) {
            return 4;
        }
        if (config.allowedKeyManagement.get(6)) {
            return 5;
        }
        return -1;
    }

    public static int getSecurity(ScanResult result) {
        if (result.capabilities.contains("WAPI-PSK")) {
            return 4;
        }
        if (result.capabilities.contains("WAPI-CERT")) {
            return 5;
        }
        return -1;
    }

    public void appendApSummary(StringBuilder summary, int autoJoinStatus, String connectFail, String disabled) {
        sWifiLibExt.appendApSummary(summary, autoJoinStatus, connectFail, disabled);
    }

    public static IWifiLibExt getWifiPlugin(Context context) {
        if (sWifiLibExt == null) {
            sWifiLibExt = (IWifiLibExt) MPlugin.createInstance(IWifiLibExt.class.getName(), context);
            if (sWifiLibExt == null) {
                sWifiLibExt = new DefaultWifiLibExt();
            }
        }
        return sWifiLibExt;
    }
}
