package com.mediatek.settingslib.ext;

import com.mediatek.common.PluginImpl;

@PluginImpl(interfaceName = "com.mediatek.settingslib.ext.IWifiLibExt")
public class DefaultWifiLibExt implements IWifiLibExt {
    public boolean shouldCheckNetworkCapabilities() {
        return true;
    }

    public void appendApSummary(StringBuilder summary, int autoJoinStatus, String connectFail, String disabled) {
        summary.append(connectFail);
    }
}
