package com.mediatek.systemui.statusbar.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.ITelephony.Stub;
import com.mediatek.systemui.statusbar.extcb.FeatureOptionUtils;
import java.util.List;

public class SIMHelper {
    private static boolean bMtkAudioProfilesSupport;
    private static boolean bMtkGemini3GSwitchSupport = SystemProperties.get("ro.mtk_gemini_3g_switch").equals(FeatureOptionUtils.SUPPORT_YES);
    private static boolean bMtkGeminiSupport = SystemProperties.get("ro.mtk_gemini_support").equals(FeatureOptionUtils.SUPPORT_YES);
    private static boolean bMtkHotKnotSupport = SystemProperties.get("ro.mtk_hotknot_support").equals(FeatureOptionUtils.SUPPORT_YES);
    public static Context sContext;
    private static List<SubscriptionInfo> sSimInfos;

    private SIMHelper() {
    }

    public static SubscriptionInfo getSubInfoBySlot(Context context, int slotId) {
        if (sSimInfos == null || sSimInfos.size() == 0) {
            Log.d("@M_SIMHelper", "getSubInfoBySlot, SubscriptionInfo is null");
            return null;
        }
        for (SubscriptionInfo info : sSimInfos) {
            if (info.getSimSlotIndex() == slotId) {
                return info;
            }
        }
        return null;
    }

    public static void updateSIMInfos(Context context) {
        sSimInfos = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
    }

    public static int getFirstSubInSlot(int slotId) {
        int[] subIds = SubscriptionManager.getSubId(slotId);
        if (subIds != null && subIds.length > 0) {
            return subIds[0];
        }
        Log.d("SIMHelper", "Cannot get first sub in slot: " + slotId);
        return -1;
    }

    public static int getSlotCount() {
        return TelephonyManager.getDefault().getPhoneCount();
    }

    public static boolean isSimInsertedBySlot(Context context, int slotId) {
        if (sSimInfos == null) {
            Log.d("@M_SIMHelper", "isSimInsertedBySlot, SubscriptionInfo is null");
            return false;
        } else if (slotId > getSlotCount() - 1 || getSubInfoBySlot(context, slotId) == null) {
            return false;
        } else {
            return true;
        }
    }

    static {
        boolean z = false;
        if (SystemProperties.get("ro.mtk_audio_profiles").equals(FeatureOptionUtils.SUPPORT_YES) && !SystemProperties.get("ro.mtk_a1_feature").equals(FeatureOptionUtils.SUPPORT_YES)) {
            z = true;
        }
        bMtkAudioProfilesSupport = z;
    }

    public static final boolean isMtkHotKnotSupport() {
        Log.d("@M_SIMHelper", "isMtkHotKnotSupport, bMtkHotKnotSupport = " + bMtkHotKnotSupport);
        return bMtkHotKnotSupport;
    }

    public static final boolean isMtkAudioProfilesSupport() {
        return bMtkAudioProfilesSupport;
    }

    public static void setContext(Context context) {
        sContext = context;
    }

    public static boolean isWifiOnlyDevice() {
        if (((ConnectivityManager) sContext.getSystemService("connectivity")).isNetworkSupported(0)) {
            return false;
        }
        return true;
    }

    public static boolean isRadioOn(int subId) {
        ITelephony telephony = Stub.asInterface(ServiceManager.getService("phone"));
        if (telephony != null) {
            try {
                return telephony.isRadioOnForSubscriber(subId, sContext.getPackageName());
            } catch (RemoteException e) {
                Log.e("SIMHelper", "mTelephony exception");
            }
        }
        return false;
    }
}
