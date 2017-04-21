package com.android.keyguard;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.SystemProperties;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import com.mediatek.systemui.statusbar.extcb.FeatureOptionUtils;

public class KeyguardUtils {
    private static final boolean mIsMediatekSimMeLockSupport = SystemProperties.get("ro.sim_me_lock_mode", "0").equals("0");
    private static final boolean mIsOwnerSdcardOnlySupport = SystemProperties.get("ro.mtk_owner_sdcard_support").equals(FeatureOptionUtils.SUPPORT_YES);
    private static final boolean mIsPrivacyProtectionLockSupport = SystemProperties.get("ro.mtk_privacy_protection_lock").equals(FeatureOptionUtils.SUPPORT_YES);
    private static final boolean mIsVoiceUnlockSupport = SystemProperties.get("ro.mtk_voice_unlock_support").equals(FeatureOptionUtils.SUPPORT_YES);
    private static boolean sMtkSmartbookSupport = SystemProperties.get("ro.mtk_smartbook_support").equals(FeatureOptionUtils.SUPPORT_YES);
    private static int sPhoneCount = 0;
    private SubscriptionManager mSubscriptionManager;
    private KeyguardUpdateMonitor mUpdateMonitor;

    public KeyguardUtils(Context context) {
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
        this.mSubscriptionManager = SubscriptionManager.from(context);
    }

    public String getOptrNameUsingPhoneId(int phoneId, Context context) {
        SubscriptionInfo info = this.mSubscriptionManager.getActiveSubscriptionInfo(getSubIdUsingPhoneId(phoneId));
        if (info == null) {
            Log.d("KeyguardUtils", "getOptrNameUsingPhoneId, return null");
        } else {
            Log.d("KeyguardUtils", "getOptrNameUsingPhoneId mDisplayName=" + info.getDisplayName());
            if (info.getDisplayName() != null) {
                return info.getDisplayName().toString();
            }
        }
        return null;
    }

    public Bitmap getOptrBitmapUsingPhoneId(int phoneId, Context context) {
        SubscriptionInfo info = this.mSubscriptionManager.getActiveSubscriptionInfo(getSubIdUsingPhoneId(phoneId));
        if (info != null) {
            return info.createIconBitmap(context);
        }
        Log.d("KeyguardUtils", "getOptrBitmapUsingPhoneId, return null");
        return null;
    }

    public static final boolean isPrivacyProtectionLockSupport() {
        return mIsPrivacyProtectionLockSupport;
    }

    public static final boolean isVoiceWakeupSupport(Context context) {
        boolean z = false;
        AudioManager am = (AudioManager) context.getSystemService("audio");
        if (am == null) {
            Log.d("KeyguardUtils", "isVoiceWakeupSupport() - get AUDIO_SERVICE fails, return false.");
            return false;
        }
        String val = am.getParameters("MTK_VOW_SUPPORT");
        if (val != null) {
            z = val.equalsIgnoreCase("MTK_VOW_SUPPORT=true");
        }
        return z;
    }

    public static final boolean isMediatekSimMeLockSupport() {
        return mIsMediatekSimMeLockSupport;
    }

    public static void requestImeStatusRefresh(Context context) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService("input_method");
        if (imm != null) {
            Log.d("KeyguardUtils", "call imm.requestImeStatusRefresh()");
            imm.refreshImeWindowVisibility();
        }
    }

    public static boolean isFlightModePowerOffMd() {
        boolean powerOffMd = SystemProperties.get("ro.mtk_flight_mode_power_off_md").equals(FeatureOptionUtils.SUPPORT_YES);
        Log.d("KeyguardUtils", "powerOffMd = " + powerOffMd);
        return powerOffMd;
    }

    public static int getNumOfPhone() {
        int i = 4;
        if (sPhoneCount == 0) {
            sPhoneCount = TelephonyManager.getDefault().getPhoneCount();
            if (sPhoneCount <= 4) {
                i = sPhoneCount;
            }
            sPhoneCount = i;
        }
        return sPhoneCount;
    }

    public static boolean isValidPhoneId(int phoneId) {
        if (phoneId == Integer.MAX_VALUE || phoneId < 0 || phoneId >= getNumOfPhone()) {
            return false;
        }
        return true;
    }

    public static int getPhoneIdUsingSubId(int subId) {
        Log.e("KeyguardUtils", "getPhoneIdUsingSubId: subId = " + subId);
        int phoneId = SubscriptionManager.getPhoneId(subId);
        if (phoneId < 0 || phoneId >= getNumOfPhone()) {
            Log.e("KeyguardUtils", "getPhoneIdUsingSubId: invalid phonId = " + phoneId);
        } else {
            Log.e("KeyguardUtils", "getPhoneIdUsingSubId: get phone ID = " + phoneId);
        }
        return phoneId;
    }

    public static int getSubIdUsingPhoneId(int phoneId) {
        int subId = SubscriptionManager.getSubIdUsingPhoneId(phoneId);
        Log.d("KeyguardUtils", "getSubIdUsingPhoneId(phoneId = " + phoneId + ") = " + subId);
        return subId;
    }
}
