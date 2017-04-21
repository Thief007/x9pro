package com.mediatek.systemui.statusbar.util;

import android.os.SystemProperties;
import com.mediatek.systemui.statusbar.extcb.FeatureOptionUtils;

public class FeatureOptions {
    public static final boolean LOW_RAM_SUPPORT = isPropertyEnabledBoolean("ro.config.low_ram");
    public static final boolean MTK_A1_SUPPORT = isPropertyEnabledInt("ro.mtk_a1_feature");
    public static final boolean MTK_CT6M_SUPPORT = isPropertyEnabledInt(FeatureOptionUtils.MTK_CT6M_SUPPORT);
    public static final boolean MTK_CTA_SET = isPropertyEnabledInt("ro.mtk_cta_set");

    private static boolean isPropertyEnabledBoolean(String propertyString) {
        return "true".equals(SystemProperties.get(propertyString, "true"));
    }

    private static boolean isPropertyEnabledInt(String propertyString) {
        return FeatureOptionUtils.SUPPORT_YES.equals(SystemProperties.get(propertyString));
    }
}
