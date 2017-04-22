package com.mediatek.settingslib.ext;

public interface IWifiLibExt {
    void appendApSummary(StringBuilder stringBuilder, int i, String str, String str2);

    boolean shouldCheckNetworkCapabilities();
}
