package com.mediatek.systemui.statusbar.extcb;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.telephony.PreciseDataConnectionState;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.mediatek.systemui.statusbar.util.SIMHelper;
import com.mediatek.telephony.TelephonyManagerEx;
import java.util.HashSet;

public class SvLteController {
    private static /* synthetic */ int[] -com_mediatek_systemui_statusbar_extcb_NetworkTypeSwitchesValues = null;
    public static final String ACTION_IRAT_PS_TYPE_CHANGED = "com.mediatek.action.irat.ps.type.changed";
    private static final boolean DEBUG = true;
    public static final String EXTRA_PS_TYPE = "extra_ps_type";
    private static final int LTE_SLOT = 0;
    private static final String MTK_SRLTE_SUPPORT = "ro.mtk_srlte_support";
    private static final String MTK_SVLTE_SUPPORT = "ro.mtk_svlte_support";
    private static final String[] PROPERTY_RIL_FULL_UICC_TYPE = new String[]{"gsm.ril.fulluicctype", "gsm.ril.fulluicctype.2", "gsm.ril.fulluicctype.3", "gsm.ril.fulluicctype.4"};
    public static final int PS_SERVICE_ON_CDMA = 0;
    public static final int PS_SERVICE_ON_LTE = 1;
    public static final int PS_SERVICE_UNKNOWN = -1;
    private static final String TAG = "SvLteController";
    private final Context mContext;
    private int mDataActivity;
    private int mDataNetType;
    private int mDataState;
    private HashSet<String> mPreciseDataConnectedState = new HashSet();
    private int mPsType = -1;
    private ServiceState mServiceState;
    private SignalStrength mSignalStrength;
    private final SubscriptionInfo mSubscriptionInfo;

    private static /* synthetic */ int[] -getcom_mediatek_systemui_statusbar_extcb_NetworkTypeSwitchesValues() {
        if (-com_mediatek_systemui_statusbar_extcb_NetworkTypeSwitchesValues != null) {
            return -com_mediatek_systemui_statusbar_extcb_NetworkTypeSwitchesValues;
        }
        int[] iArr = new int[NetworkType.values().length];
        try {
            iArr[NetworkType.Type_1X.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[NetworkType.Type_1X3G.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[NetworkType.Type_3G.ordinal()] = 4;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[NetworkType.Type_4G.ordinal()] = 3;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[NetworkType.Type_E.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[NetworkType.Type_G.ordinal()] = 6;
        } catch (NoSuchFieldError e6) {
        }
        -com_mediatek_systemui_statusbar_extcb_NetworkTypeSwitchesValues = iArr;
        return iArr;
    }

    public SvLteController(Context context, SubscriptionInfo info) {
        this.mContext = context;
        this.mSubscriptionInfo = info;
    }

    public static int getSvlteSlot() {
        return 0;
    }

    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
        this.mSignalStrength = signalStrength;
    }

    public void onServiceStateChanged(ServiceState state) {
        this.mServiceState = state;
    }

    public void onDataConnectionStateChanged(int state, int networkType) {
        this.mDataState = state;
        this.mDataNetType = networkType;
    }

    public void onDataActivity(int direction) {
        this.mDataActivity = direction;
    }

    public void onPreciseDataConnectionStateChanged(PreciseDataConnectionState state) {
        Log.d(TAG, "onPreciseDataConnectionStateChanged: state = " + state.toString());
        String apnType = state.getDataConnectionAPNType();
        int dataState = state.getDataConnectionState();
        synchronized (this.mPreciseDataConnectedState) {
            if (dataState == 2) {
                if (!this.mPreciseDataConnectedState.contains(apnType)) {
                    this.mPreciseDataConnectedState.add(apnType);
                    Log.d(TAG, "onPreciseDataConnectionStateChanged: put apnType: " + apnType + ", dataState: " + dataState + " into mPreciseDataConnectedState");
                }
            } else if (this.mPreciseDataConnectedState.contains(apnType)) {
                this.mPreciseDataConnectedState.remove(apnType);
                Log.d(TAG, "onPreciseDataConnectionStateChanged: remove apnType: " + apnType + ", dataState: " + dataState + " from mPreciseDataConnectedState");
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isShowDataActivityIcon() {
        synchronized (this.mPreciseDataConnectedState) {
            if (this.mPreciseDataConnectedState.size() == 1) {
                if (this.mPreciseDataConnectedState.contains("ims") || this.mPreciseDataConnectedState.contains("emergency")) {
                    Log.d(TAG, "isShowDataActivityIcon(), return false");
                    return false;
                }
            } else if (this.mPreciseDataConnectedState.size() == 2 && this.mPreciseDataConnectedState.contains("ims") && this.mPreciseDataConnectedState.contains("emergency")) {
                Log.d(TAG, "isShowDataActivityIcon(), return false");
                return false;
            }
        }
    }

    public void cleanPhoneState() {
        this.mSignalStrength = null;
        this.mServiceState = null;
        this.mDataNetType = 0;
        this.mDataState = 0;
    }

    public int getDataState() {
        return this.mDataState;
    }

    public int getDataNetType() {
        return this.mDataNetType;
    }

    public ServiceState getServiceState() {
        return this.mServiceState;
    }

    public SignalStrength getSignalStrength() {
        return this.mSignalStrength;
    }

    public int getSignalStrengthLevel(NetworkType networkType, boolean alwaysShowCdmaRssi) {
        if (this.mSignalStrength == null || networkType == null) {
            return 0;
        }
        switch (-getcom_mediatek_systemui_statusbar_extcb_NetworkTypeSwitchesValues()[networkType.ordinal()]) {
            case 1:
                return this.mSignalStrength.getCdmaLevel();
            case 2:
                if (alwaysShowCdmaRssi) {
                    return this.mSignalStrength.getCdmaLevel();
                }
                return this.mSignalStrength.getEvdoLevel();
            case 3:
                return this.mSignalStrength.getLteLevel();
            default:
                return this.mSignalStrength.getLevel();
        }
    }

    public int getPsType() {
        return this.mPsType;
    }

    public void setPsType(int psType) {
        this.mPsType = psType;
    }

    public boolean hasService() {
        return hasServiceInSvlte(this.mServiceState);
    }

    public boolean isEmergencyOnly() {
        boolean isEmergencyOnly = this.mServiceState != null ? this.mServiceState.isEmergencyOnly() : false;
        Log.d(TAG, "isOnlyEmergency, slotId = " + this.mSubscriptionInfo.getSimSlotIndex() + ", isOnlyEmergency = " + isEmergencyOnly);
        return isEmergencyOnly;
    }

    public boolean isOffline(NetworkType networkType) {
        boolean isOffline;
        boolean isEmergencyOnly = isEmergencyOnly();
        boolean isRadioOn = SIMHelper.isRadioOn(this.mSubscriptionInfo.getSubscriptionId());
        boolean extraSubRadioOn = (this.mServiceState == null || this.mServiceState.getDataRegState() == 3) ? false : DEBUG;
        if (isEmergencyOnly) {
            isOffline = DEBUG;
            if (networkType == NetworkType.Type_4G && isShow4GDataOnlyForLTE()) {
                isOffline = false;
                Log.d(TAG, "SvLteController.isOffline,networkType: " + networkType + ", set isOffline false");
            }
        } else {
            isOffline = (isRadioOn || extraSubRadioOn) ? false : DEBUG;
        }
        Log.d(TAG, "isOffline(), slotId = " + this.mSubscriptionInfo.getSimSlotIndex() + ", isOffline = " + isOffline + ", isEmergencyOnly = " + isEmergencyOnly + ", isRadioOn = " + isRadioOn + ", extraSubRadioOn = " + extraSubRadioOn + ", mServiceState = " + this.mServiceState);
        return isOffline;
    }

    public boolean isDataConnected() {
        boolean bSvlteDataConnected = false;
        if (this.mServiceState != null) {
            bSvlteDataConnected = this.mServiceState.getDataRegState() == 0 ? this.mDataState == 2 ? DEBUG : false : false;
        }
        Log.d(TAG, "isSvlteDataConnected, bSvlteDataConnected=" + bSvlteDataConnected + " serviceState=" + this.mServiceState);
        return bSvlteDataConnected;
    }

    public int getDataNetTypeWithLTEService(int oneSubDataNetType) {
        int tempDataNetType;
        int retDataNetType = oneSubDataNetType;
        ServiceState lteServiceState = this.mServiceState;
        if (lteServiceState != null) {
            tempDataNetType = getNWTypeByPriority(lteServiceState.getVoiceNetworkType(), lteServiceState.getDataNetworkType());
        } else {
            tempDataNetType = this.mDataNetType;
        }
        Log.d(TAG, "getDataNetTypeWithLTEService, lteServiceState =" + lteServiceState + " mDataNetType=" + this.mDataNetType + " tempDataNetType=" + tempDataNetType);
        retDataNetType = getNWTypeByPriority(oneSubDataNetType, tempDataNetType);
        if (!(lteServiceState == null || lteServiceState.getRoaming() || !isApIratSupport() || this.mPsType == -1)) {
            retDataNetType = lteServiceState.getDataNetworkType();
        }
        Log.d(TAG, "getDataNetTypeWithLTEService, oneSubDataNetType=" + oneSubDataNetType + " retDataNetType=" + retDataNetType);
        return retDataNetType;
    }

    public boolean isShow4GDataOnlyForLTE() {
        boolean isShow4GDataOnly = false;
        if (this.mServiceState != null) {
            if (this.mServiceState.getVoiceRegState() != 0 && this.mServiceState.getDataRegState() == 0) {
                isShow4GDataOnly = DEBUG;
            } else if (this.mServiceState.getVoiceRegState() != 0) {
                isShow4GDataOnly = is4GDataOnlyMode(this.mContext);
            }
        }
        Log.d(TAG, "isShow4GDataOnlyForLTE: isShow4GDataOnlyForLTE = " + isShow4GDataOnly + ", mServiceState=" + this.mServiceState);
        return isShow4GDataOnly;
    }

    public static final boolean isMediatekSVLteDcSupport() {
        if (FeatureOptionUtils.SUPPORT_YES.equals(SystemProperties.get("ro.mtk_svlte_support"))) {
            return DEBUG;
        }
        return false;
    }

    public static final boolean isMediatekSRLteDcSupport() {
        if (FeatureOptionUtils.SUPPORT_YES.equals(SystemProperties.get(MTK_SRLTE_SUPPORT))) {
            return DEBUG;
        }
        return false;
    }

    public static final boolean isMediatekSVLteDcSupport(int slotId) {
        return isMediatekSVLteDcSupport() ? isSvlteSlot(slotId) : false;
    }

    public static final boolean isMediatekSVLteDcSupport(SubscriptionInfo info) {
        return (info == null || !isMediatekSVLteDcSupport()) ? false : isSvlteSlot(info.getSimSlotIndex());
    }

    public static final boolean isMediatekSRLteDcSupport(SubscriptionInfo info) {
        return (info == null || !isMediatekSRLteDcSupport()) ? false : isSvlteSlot(info.getSimSlotIndex());
    }

    public static final boolean isApIratSupport() {
        if (!SystemProperties.get("ro.mtk_svlte_support").equals(FeatureOptionUtils.SUPPORT_YES) || SystemProperties.get(FeatureOptionUtils.MTK_MD_IRAT_SUPPORT).equals(FeatureOptionUtils.SUPPORT_YES)) {
            return false;
        }
        return DEBUG;
    }

    public static final boolean isSvlteSlot(int slotId) {
        return slotId == getSvlteSlot() ? DEBUG : false;
    }

    public static final boolean hasServiceInSvlte(ServiceState ss) {
        boolean z = DEBUG;
        if (ss == null) {
            return false;
        }
        switch (ss.getVoiceRegState()) {
            case 1:
            case 2:
            case 3:
                if (ss.getDataRegState() != 0) {
                    z = false;
                }
                return z;
            default:
                return DEBUG;
        }
    }

    public final boolean is4GDataEnabled(Context context) {
        int svlteRatMode = getSvlteRatMode(context);
        boolean is4GDataEnabled = svlteRatMode != 0 ? svlteRatMode == 2 ? DEBUG : false : DEBUG;
        Log.d(TAG, "is4GDataEnabled(), is4GDataEnabled=" + is4GDataEnabled);
        return is4GDataEnabled;
    }

    public final boolean is4GDataOnlyMode(Context context) {
        boolean is4GDataOnly = getSvlteRatMode(context) == 2 ? DEBUG : false;
        Log.d(TAG, "is4GDataOnlyMode, is4GDataOnly=" + is4GDataOnly);
        return is4GDataOnly;
    }

    private final int getSvlteRatMode(Context context) {
        int svlteRatMode = Global.getInt(context.getContentResolver(), TelephonyManagerEx.getDefault().getCdmaRatModeKey(this.mSubscriptionInfo.getSubscriptionId()), 0);
        Log.d(TAG, "getSvlteRatMode(), svlteRatMode = " + svlteRatMode);
        return svlteRatMode;
    }

    private static final boolean is4GUiccCard() {
        boolean is4GUiccCard = false;
        String cardType = SystemProperties.get(PROPERTY_RIL_FULL_UICC_TYPE[0]);
        String[] appType = cardType.split(",");
        for (Object equals : appType) {
            if ("USIM".equals(equals)) {
                is4GUiccCard = DEBUG;
                break;
            }
        }
        Log.d(TAG, "is4GUiccCard cardType=" + cardType + ", is4GUiccCard=" + is4GUiccCard);
        return is4GUiccCard;
    }

    private static final int getNWTypeByPriority(int cs, int ps) {
        if (TelephonyManager.getNetworkClass(cs) > TelephonyManager.getNetworkClass(ps)) {
            return cs;
        }
        return ps;
    }

    private final boolean isBehaviorSet(BehaviorSet behaviorSet) {
        return PluginFactory.getStatusBarPlugin(this.mContext).customizeBehaviorSet() == behaviorSet ? DEBUG : false;
    }
}
