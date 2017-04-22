package com.mediatek.settings.cdma;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.internal.telephony.ITelephonyEx.Stub;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteModeController;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteUtils;
import com.mediatek.telephony.TelephonyManagerEx;

public class CdmaApnSetting {
    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static String customizeQuerySelectionforCdma(String where, String numeric, int subId) {
        String result = where;
        String tail = " AND NOT (type='ia' AND (apn=\"\" OR apn IS NULL))";
        int slotId = SubscriptionManager.getSlotId(subId);
        if (CdmaUtils.isCdmaCardType(slotId) && SvlteModeController.getActiveSvlteModeSlotId() == slotId) {
            String sqlStr = "";
            String apn = "";
            String sourceType = "";
            try {
                ITelephonyEx telephonyEx = Stub.asInterface(ServiceManager.getService("phoneEx"));
                String mvnoType = telephonyEx.getMvnoMatchType(subId);
                sqlStr = " mvno_type='" + replaceNull(mvnoType) + "'" + " and mvno_match_data='" + replaceNull(telephonyEx.getMvnoPattern(subId, mvnoType)) + "'";
            } catch (RemoteException e) {
                Log.d("CdmaApnSetting", "RemoteException " + e);
            }
            Log.d("CdmaApnSetting", "subId = " + subId + " slotId = " + slotId);
            String plmnNumeric = SystemProperties.get("gsm.operator.numeric");
            Log.d("CdmaApnSetting", "plmnNumeric = " + plmnNumeric + " numeric = " + numeric);
            if (numeric != null) {
                boolean z;
                if (!numeric.contains("46011")) {
                }
                numeric = getApnNumeric(numeric, subId);
                if (numeric.contains("46011")) {
                    z = true;
                } else {
                    z = numeric.contains("46003");
                }
                if (!z) {
                    sqlStr = sqlStr + " and apn <> 'ctwap'";
                }
                Log.d("CdmaApnSetting", "final numeric = " + numeric);
                result = "numeric='" + numeric + "' and " + ("((" + sqlStr + ")" + " or (sourceType = '1'))") + tail;
                Log.d("CdmaApnSetting", "customizeQuerySelectionforCdma result=" + result);
                return result;
            }
            if (!(plmnNumeric == null || plmnNumeric.length() < 3 || plmnNumeric.startsWith("460"))) {
                if (!numeric.startsWith("455")) {
                    Log.d("CdmaApnSetting", "ROAMING");
                    result = "numeric='" + numeric + "' and " + "((" + sqlStr + (apn + " and apn <> 'ctwap'") + ")" + " or (sourceType = '1'))" + tail;
                    Log.d("CdmaApnSetting", "customizeQuerySelectionforCdma roaming result=" + result);
                    return result;
                }
            }
            return result;
        }
        Log.d("CdmaApnSetting", "insert card is not CDMA card, just return");
        return result;
    }

    public static int getNetworkType(int subId) {
        int pstype = 0;
        try {
            ITelephonyEx tphony = Stub.asInterface(ServiceManager.getService("phoneEx"));
            if (tphony != null) {
                Bundle bd = tphony.getSvlteServiceState(subId);
                if (bd != null) {
                    ServiceState ss = ServiceState.newFromBundle(bd);
                    if (ss != null) {
                        Log.d("CdmaApnSetting", "ss = " + ss);
                        pstype = ss.getDataNetworkType();
                    }
                }
            }
        } catch (RemoteException e) {
            Log.d("CdmaApnSetting", "RemoteException " + e);
        }
        Log.d("CdmaApnSetting", "pstype = " + pstype);
        return pstype;
    }

    public static int getPreferredSubId(Context context, int subId) {
        if (context == null) {
            return subId;
        }
        int c2kSlot = SvlteModeController.getActiveSvlteModeSlotId();
        int svlteRatMode = Global.getInt(context.getContentResolver(), TelephonyManagerEx.getDefault().getCdmaRatModeKey(subId), 0);
        if (SubscriptionManager.getSlotId(subId) == c2kSlot && svlteRatMode != 2) {
            String numeric = TelephonyManager.getDefault().getSimOperator(subId);
            if (numeric != null && (numeric.equals("46003") || numeric.equals("46011"))) {
                numeric = getApnNumeric(numeric, subId);
                if (numeric != null && numeric.equals("46011")) {
                    subId = SvlteUtils.getLteDcSubId(c2kSlot);
                    Log.d("CdmaApnSetting", "getPreferredSubId subId will use LTE_DC_SUB_ID");
                }
            }
        }
        return subId;
    }

    private static String getApnNumeric(String numeric, int subId) {
        int pstype = getNetworkType(subId);
        String plmnNumeric = SystemProperties.get("gsm.operator.numeric");
        if (plmnNumeric.contains("46011") && plmnNumeric.contains("46003")) {
            if (pstype == 13 || pstype == 14) {
                return "46011";
            }
            return "46003";
        } else if (plmnNumeric.contains("46011")) {
            return "46011";
        } else {
            if (!plmnNumeric.contains("46003")) {
                numeric = TelephonyManager.getDefault().getNetworkOperatorForSubscription(subId);
                if (numeric == null) {
                    numeric = "46003";
                }
                Log.d("CdmaApnSetting", "plmnNumeric not contains 46003 or 46011, as ROAMING mumeric: " + numeric);
                return numeric;
            } else if (pstype == 14) {
                return "46011";
            } else {
                return "46003";
            }
        }
    }

    private static String replaceNull(String origString) {
        if (origString == null) {
            return "";
        }
        return origString;
    }
}
