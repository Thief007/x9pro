package com.mediatek.settings.cdma;

import android.content.Context;
import android.content.Intent;
import android.net.NetworkTemplate;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.mediatek.internal.telephony.cdma.CdmaFeatureOptionUtils;
import com.mediatek.internal.telephony.uicc.SvlteUiccUtils;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.sim.TelephonyUtils;
import com.mediatek.telephony.TelephonyManagerEx;

public class CdmaUtils {
    public static boolean isCdmaCardType(int slotId) {
        boolean isCdmaCard = SvlteUiccUtils.getInstance().isRuimCsim(slotId);
        Log.d("CdmaUtils", "slotId = " + slotId + " isCdmaCard = " + isCdmaCard);
        return isCdmaCard;
    }

    public static int getExternalModemSlot() {
        return CdmaFeatureOptionUtils.getExternalModemSlot();
    }

    public static void fillTemplateForCdmaLte(NetworkTemplate template, int subId) {
        if (CdmaFeatureOptionUtils.isCdmaLteDcSupport()) {
            String svlteSubscriberId = TelephonyManagerEx.getDefault().getSubscriberIdForLteDcPhone(subId);
            if (!TextUtils.isEmpty(svlteSubscriberId) && svlteSubscriberId.length() > 0) {
                Log.d("CdmaUtils", "bf:" + template);
                template.addMatchSubscriberId(svlteSubscriberId);
                Log.d("CdmaUtils", "af:" + template);
            }
        }
    }

    public static void checkCdmaSimStatus(Context context, int simDetectNum) {
        Log.d("CdmaUtils", "startCdmaWaringDialog, simDetectNum = " + simDetectNum);
        boolean twoCdmaInsert = true;
        if (simDetectNum > 1) {
            for (int i = 0; i < simDetectNum; i++) {
                if (SvlteUiccUtils.getInstance().getSimType(i) != 2) {
                    twoCdmaInsert = false;
                }
            }
        } else {
            twoCdmaInsert = false;
        }
        Log.d("CdmaUtils", "twoCdmaInsert = " + twoCdmaInsert);
        if (twoCdmaInsert) {
            Intent intent = new Intent("com.mediatek.settings.cdma.TWO_CDMA_POPUP");
            intent.addFlags(402653184);
            intent.putExtra("dialog_type", 0);
            context.startActivity(intent);
        }
    }

    public static void startAlertCdmaDialog(Context context, int targetSubId, int actionType) {
        Intent intent = new Intent("com.mediatek.settings.cdma.TWO_CDMA_POPUP");
        intent.addFlags(402653184);
        intent.putExtra("dialog_type", 1);
        intent.putExtra("target_subid", targetSubId);
        intent.putExtra("action_type", actionType);
        context.startActivity(intent);
    }

    public static boolean isCdmaCardCompetion(Context context) {
        boolean isCdmaCard = true;
        boolean isCompetition = true;
        int simCount = 0;
        if (context != null) {
            simCount = TelephonyManager.from(context).getSimCount();
        }
        if (simCount == 2) {
            for (int i = 0; i < simCount; i++) {
                isCdmaCard = isCdmaCard ? SvlteUiccUtils.getInstance().getSimType(i) == 2 : false;
                SubscriptionInfo subscriptionInfo = SubscriptionManager.from(context).getActiveSubscriptionInfoForSimSlotIndex(i);
                if (subscriptionInfo == null) {
                    isCompetition = false;
                    break;
                }
                if (isCompetition) {
                    isCompetition = TelephonyManagerEx.getDefault().isInHomeNetwork(subscriptionInfo.getSubscriptionId());
                } else {
                    isCompetition = false;
                }
            }
        } else {
            isCdmaCard = false;
            isCompetition = false;
        }
        Log.d("CdmaUtils", "isCdmaCard: " + isCdmaCard + " isCompletition: " + isCompetition + " is Suppport SIM switch: " + FeatureOption.MTK_DISABLE_CAPABILITY_SWITCH);
        if (isCdmaCard && isCompetition && !FeatureOption.MTK_DISABLE_CAPABILITY_SWITCH) {
            return true;
        }
        return false;
    }

    public static boolean isCdmaCardCompetionForData(Context context) {
        return isCdmaCardCompetion(context);
    }

    public static boolean isCdmaCardCompetionForSms(Context context, int targetItem) {
        Log.d("CdmaUtils", "targetItem: " + targetItem);
        if (SubscriptionManager.isValidSubscriptionId(targetItem) && isCdmaCardCompetion(context) && TelephonyUtils.getMainCapabilitySlotId(context) != SubscriptionManager.getSlotId(targetItem)) {
            return true;
        }
        return false;
    }

    public static boolean isCdmaCardCompetionForCalls(Context context, int targetItem) {
        boolean shouldDisplay = false;
        int subId = -1;
        if (context != null) {
            subId = TelephonyUtils.phoneAccountHandleTosubscriptionId(context, targetItem < 1 ? null : (PhoneAccountHandle) TelecomManager.from(context).getCallCapablePhoneAccounts().get(targetItem - 1));
        }
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            shouldDisplay = TelephonyUtils.getMainCapabilitySlotId(context) != SubscriptionManager.getSlotId(subId);
        }
        Log.d("CdmaUtils", "shouldDisplay: " + shouldDisplay + " targetItem: " + targetItem);
        if (shouldDisplay) {
            return isCdmaCardCompetion(context);
        }
        return false;
    }

    public static boolean isSwitchCdmaCardToGsmCard(Context context, int targetSubId) {
        boolean isGsmCardForTarget = false;
        boolean isCdmaCardForMainCapability = false;
        if (!(FeatureOption.MTK_C2K_SLOT2_SUPPORT || !SubscriptionManager.isValidSubscriptionId(targetSubId) || FeatureOption.MTK_DISABLE_CAPABILITY_SWITCH)) {
            isCdmaCardForMainCapability = SvlteUiccUtils.getInstance().getSimType(TelephonyUtils.getMainCapabilitySlotId(context)) == 2;
            isGsmCardForTarget = SvlteUiccUtils.getInstance().getSimType(SubscriptionManager.getSlotId(targetSubId)) == 1;
        }
        Log.d("CdmaUtils", "isGsmCardForTarget: " + isGsmCardForTarget + " isCdmaCardForMainCapability: " + isCdmaCardForMainCapability + " sim switch is support: " + FeatureOption.MTK_DISABLE_CAPABILITY_SWITCH);
        return isGsmCardForTarget ? isCdmaCardForMainCapability : false;
    }

    public static boolean shouldSwitchCapability(Context context, int targetSubId) {
        boolean isCdmaCardForTarget = false;
        boolean isGsmCardForMainCapability = false;
        if (!(FeatureOption.MTK_C2K_SLOT2_SUPPORT || !SubscriptionManager.isValidSubscriptionId(targetSubId) || FeatureOption.MTK_DISABLE_CAPABILITY_SWITCH)) {
            isGsmCardForMainCapability = SvlteUiccUtils.getInstance().getSimType(TelephonyUtils.getMainCapabilitySlotId(context)) == 1;
            isCdmaCardForTarget = SvlteUiccUtils.getInstance().getSimType(SubscriptionManager.getSlotId(targetSubId)) == 2;
        }
        Log.d("CdmaUtils", "isCdmaCardForTarget: " + isCdmaCardForTarget + " isGsmCardForMainCapability: " + isGsmCardForMainCapability + " sim switch is support: " + FeatureOption.MTK_DISABLE_CAPABILITY_SWITCH);
        return isCdmaCardForTarget ? isGsmCardForMainCapability : false;
    }

    public static boolean shouldSwichCapabilityForCalls(Context context, PhoneAccountHandle handle) {
        boolean isCdmaCardForTarget = false;
        boolean isGsmCardForMainCapability = false;
        int subId = -1;
        if (context != null) {
            subId = TelephonyUtils.phoneAccountHandleTosubscriptionId(context, handle);
        }
        if (!(!SubscriptionManager.isValidSubscriptionId(subId) || FeatureOption.MTK_C2K_SLOT2_SUPPORT || FeatureOption.MTK_DISABLE_CAPABILITY_SWITCH)) {
            isGsmCardForMainCapability = SvlteUiccUtils.getInstance().getSimType(TelephonyUtils.getMainCapabilitySlotId(context)) == 1;
            isCdmaCardForTarget = SvlteUiccUtils.getInstance().getSimType(SubscriptionManager.getSlotId(subId)) == 2;
        }
        Log.d("CdmaUtils", "isCdmaCardForTarget: " + isCdmaCardForTarget + " isGsmCardForMainCapability: " + isGsmCardForMainCapability + " sim switch is support: " + FeatureOption.MTK_DISABLE_CAPABILITY_SWITCH);
        return isCdmaCardForTarget ? isGsmCardForMainCapability : false;
    }

    public static boolean isCdamCardAndGsmCard(Context context) {
        boolean isCdmaCard = false;
        boolean isGsmCard = false;
        int simCount = 0;
        if (context != null) {
            simCount = TelephonyManager.from(context).getSimCount();
        }
        for (int i = 0; i < simCount; i++) {
            if (SvlteUiccUtils.getInstance().getSimType(i) == 2) {
                isCdmaCard = true;
            } else if (SvlteUiccUtils.getInstance().getSimType(i) == 1) {
                isGsmCard = true;
            }
        }
        Log.d("CdmaUtils", "isCdmaCard: " + isCdmaCard + " isGsmCard: " + isGsmCard + " solution2 support: " + FeatureOption.MTK_C2K_SLOT2_SUPPORT + " sim switch is support: " + FeatureOption.MTK_DISABLE_CAPABILITY_SWITCH);
        if (!isCdmaCard || !isGsmCard || FeatureOption.MTK_C2K_SLOT2_SUPPORT || FeatureOption.MTK_DISABLE_CAPABILITY_SWITCH) {
            return false;
        }
        return true;
    }
}
