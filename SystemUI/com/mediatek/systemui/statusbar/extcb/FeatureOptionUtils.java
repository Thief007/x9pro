package com.mediatek.systemui.statusbar.extcb;

import android.os.SystemProperties;

public class FeatureOptionUtils {
    public static final String BUILD_TYPE = "ro.build.type";
    public static final String BUILD_TYPE_ENG = "eng";
    public static final String BUILD_TYPE_USER = "user";
    public static final String EVDO_DT_SUPPORT = "ril.evdo.dtsupport";
    public static final String MTK_C2K_SUPPORT = "ro.mtk_c2k_support";
    public static final String MTK_CT6M_SUPPORT = "ro.ct6m_support";
    public static final String MTK_IRAT_SUPPORT = "ro.c2k.irat.support";
    public static final String MTK_MD_IRAT_SUPPORT = "ro.c2k.md.irat.support";
    public static final String MTK_OP01_RCS_SUPPORT = "ro.mtk_op01_rcs";
    public static final String MTK_SVLTE_SUPPORT = "ro.mtk_svlte_support";
    public static final String SUPPORT_YES = "1";

    public static final boolean isCdmaLteDcSupport() {
        return isSupport(MTK_SVLTE_SUPPORT);
    }

    public static final boolean isCdmaApIratSupport() {
        return isCdmaIratSupport() && !isCdmaMdIratSupport();
    }

    public static final boolean isCdmaMdIratSupport() {
        return isSupport(MTK_MD_IRAT_SUPPORT);
    }

    public static final boolean isCdmaIratSupport() {
        return isSupport(MTK_IRAT_SUPPORT);
    }

    public static final boolean isMtkC2KSupport() {
        return isSupport(MTK_C2K_SUPPORT);
    }

    public static final boolean isOP01RcsSupport() {
        return isSupport(MTK_OP01_RCS_SUPPORT);
    }

    public static final boolean isMTK_CT6M_SUPPORT() {
        return isSupport(MTK_CT6M_SUPPORT);
    }

    public static final boolean isUserLoad() {
        return SystemProperties.get(BUILD_TYPE).equals(BUILD_TYPE_USER);
    }

    private static final boolean isSupport(String key) {
        return SystemProperties.get(key).equals(SUPPORT_YES);
    }
}
