package com.mediatek.systemui.ext;

import com.mediatek.systemui.statusbar.util.FeatureOptions;

public class DefaultMobileIconExt implements IMobileIconExt {
    public int customizeWifiNetCondition(int netCondition) {
        if (FeatureOptions.MTK_CT6M_SUPPORT) {
            return 1;
        }
        return netCondition;
    }

    public int customizeMobileNetCondition(int netCondition) {
        if (FeatureOptions.MTK_CT6M_SUPPORT) {
            return 1;
        }
        return netCondition;
    }
}
