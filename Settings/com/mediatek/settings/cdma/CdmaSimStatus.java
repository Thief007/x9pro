package com.mediatek.settings.cdma;

import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.cdma.CDMAPhone;
import com.android.settings.R;
import com.mediatek.internal.telephony.ltedc.svlte.SvltePhoneProxy;

public class CdmaSimStatus {
    private PreferenceActivity mActivity;
    private CDMAPhone mCdmaPhone;
    private String mDefaultText;
    private PreferenceScreen mPreferenceScreen;
    private ServiceState mServiceState;
    private SubscriptionInfo mSubInfo;
    private Phone mSvlteDcPhone;
    private TelephonyManager mTelephonyManager;

    public CdmaSimStatus(PreferenceActivity activity, SubscriptionInfo subInfo) {
        this.mActivity = activity;
        this.mSubInfo = subInfo;
        this.mPreferenceScreen = activity.getPreferenceScreen();
        this.mTelephonyManager = (TelephonyManager) activity.getSystemService("phone");
        this.mDefaultText = activity.getString(R.string.device_info_default);
    }

    public void setPhoneInfos(Phone phone) {
        int phoneType = 0;
        if (phone != null) {
            setServiceState(phone.getServiceState());
            phoneType = phone.getPhoneType();
        } else {
            Log.e("CdmaSimStatus", "No phone available");
        }
        Log.d("CdmaSimStatus", "setPhoneInfos phoneType = " + phoneType);
        if (phoneType == 2) {
            this.mCdmaPhone = (CDMAPhone) ((PhoneProxy) phone).getActivePhone();
            if (this.mCdmaPhone != null && (phone instanceof SvltePhoneProxy)) {
                this.mSvlteDcPhone = ((SvltePhoneProxy) phone).getLtePhone();
                Log.d("CdmaSimStatus", "mSvlteDcPhone = " + this.mSvlteDcPhone);
            }
        }
    }

    public void setSubscriptionInfo(SubscriptionInfo subInfo) {
        this.mSubInfo = subInfo;
        Log.d("CdmaSimStatus", "setSubscriptionInfo = " + this.mSubInfo);
    }

    public void updateCdmaPreference(PreferenceActivity activity, SubscriptionInfo subInfo) {
        int slotId = subInfo.getSimSlotIndex();
        PreferenceScreen prefScreen = activity.getPreferenceScreen();
        Log.d("CdmaSimStatus", "slotId = " + slotId);
        if (CdmaUtils.isCdmaCardType(slotId)) {
            boolean isAdded = prefScreen.findPreference("current_operators_mccmnc") != null;
            Log.d("CdmaSimStatus", "isAdded = " + isAdded);
            if (!isAdded) {
                activity.addPreferencesFromResource(R.xml.current_networkinfo_status);
            }
            setMccMnc();
            setCdmaSidNid();
            setCellId();
            return;
        }
        removeCdmaItems();
    }

    public void setServiceState(ServiceState state) {
        Log.d("CdmaSimStatus", "setServiceState with state = " + state);
        this.mServiceState = state;
    }

    public void updateNetworkType(String key, String networktype) {
        Log.d("CdmaSimStatus", "updateNetworkType with networktype = " + networktype);
        if (CdmaUtils.isCdmaCardType(this.mSubInfo.getSimSlotIndex()) && "LTE".equals(networktype) && this.mServiceState != null && this.mServiceState.getVoiceRegState() == 0) {
            String voiceNetworkName = renameNetworkTypeName(TelephonyManager.getNetworkTypeName(this.mServiceState.getVoiceNetworkType()));
            Log.d("CdmaSimStatus", "voiceNetworkName = " + voiceNetworkName);
            setSummaryText(key, voiceNetworkName + " , " + networktype);
        }
    }

    public void updateSignalStrength(SignalStrength signal, Preference preference) {
        if (CdmaUtils.isCdmaCardType(this.mSubInfo.getSimSlotIndex()) && !signal.isGsm() && isRegisterUnderLteNetwork()) {
            setCdmaSignalStrength(signal, preference);
            int lteSignalDbm = signal.getLteDbm();
            int lteSignalAsu = signal.getLteAsuLevel();
            if (-1 == lteSignalDbm) {
                lteSignalDbm = 0;
            }
            if (-1 == lteSignalAsu) {
                lteSignalAsu = 0;
            }
            Log.d("CdmaSimStatus", "lteSignalDbm = " + lteSignalDbm + " lteSignalAsu = " + lteSignalAsu);
            Log.d("CdmaSimStatus", "cdmaSignal = " + preference.getSummary().toString() + " lteSignal = " + this.mActivity.getString(R.string.sim_signal_strength, new Object[]{Integer.valueOf(lteSignalDbm), Integer.valueOf(lteSignalAsu)}));
            String summary = this.mActivity.getString(R.string.status_cdma_signal_strength, new Object[]{cdmaSignal, lteSignal});
            Log.d("CdmaSimStatus", "summary = " + summary);
            preference.setSummary(summary);
        }
    }

    private void setCdmaSignalStrength(SignalStrength signalStrength, Preference preference) {
        Log.d("CdmaSimStatus", "setCdmaSignalStrength() for 1x cdma network type");
        if ("CDMA 1x".equals(getNetworkType())) {
            int signalDbm = signalStrength.getCdmaDbm();
            int signalAsu = signalStrength.getCdmaAsuLevel();
            if (-1 == signalDbm) {
                signalDbm = 0;
            }
            if (-1 == signalAsu) {
                signalAsu = 0;
            }
            Log.d("CdmaSimStatus", "Cdma 1x signalDbm = " + signalDbm + " signalAsu = " + signalAsu);
            preference.setSummary(this.mActivity.getString(R.string.sim_signal_strength, new Object[]{Integer.valueOf(signalDbm), Integer.valueOf(signalAsu)}));
        }
    }

    private String getNetworkType() {
        String networktype = null;
        int actualDataNetworkType = this.mTelephonyManager.getDataNetworkType(this.mSubInfo.getSubscriptionId());
        int actualVoiceNetworkType = this.mTelephonyManager.getVoiceNetworkType(this.mSubInfo.getSubscriptionId());
        Log.d("CdmaSimStatus", "actualDataNetworkType = " + actualDataNetworkType + "actualVoiceNetworkType = " + actualVoiceNetworkType);
        TelephonyManager telephonyManager;
        if (actualDataNetworkType != 0) {
            telephonyManager = this.mTelephonyManager;
            networktype = TelephonyManager.getNetworkTypeName(actualDataNetworkType);
        } else if (actualVoiceNetworkType != 0) {
            telephonyManager = this.mTelephonyManager;
            networktype = TelephonyManager.getNetworkTypeName(actualVoiceNetworkType);
        }
        Log.d("CdmaSimStatus", "getNetworkType() networktype = " + networktype);
        return renameNetworkTypeName(networktype);
    }

    private ServiceState getServiceState() {
        ServiceState serviceState;
        if (this.mSvlteDcPhone != null) {
            serviceState = this.mSvlteDcPhone.getServiceState();
            Log.d("CdmaSimStatus", "mSvlteDcPhone serviceState = " + serviceState);
            return serviceState;
        } else if (this.mCdmaPhone == null) {
            return null;
        } else {
            serviceState = this.mCdmaPhone.getServiceState();
            Log.d("CdmaSimStatus", "mCdmaPhone serviceState = " + serviceState);
            return serviceState;
        }
    }

    private boolean isRegisterUnderLteNetwork() {
        return isRegisterUnderLteNetwork(getServiceState());
    }

    private boolean isRegisterUnderLteNetwork(ServiceState serviceState) {
        Log.d("CdmaSimStatus", "isRegisterUnderLteNetwork with serviceState = " + serviceState);
        boolean isLteNetwork = false;
        if (serviceState != null && serviceState.getDataNetworkType() == 13 && serviceState.getDataRegState() == 0) {
            isLteNetwork = true;
        }
        Log.d("CdmaSimStatus", "isLteNetwork = " + isLteNetwork);
        return isLteNetwork;
    }

    private void removeCdmaItems() {
        removePreferenceFromScreen("current_operators_mccmnc");
        removePreferenceFromScreen("current_sidnid");
        removePreferenceFromScreen("current_cellid");
    }

    private void setMccMnc() {
        String numeric;
        if (isRegisterUnderLteNetwork()) {
            numeric = getMccMncProperty(this.mSvlteDcPhone);
        } else {
            numeric = getMccMncProperty(this.mCdmaPhone);
        }
        Log.d("CdmaSimStatus", "setMccMnc, numeric=" + numeric);
        if (numeric.length() > 3) {
            String mcc = numeric.substring(0, 3);
            String mccmnc = mcc + "," + numeric.substring(3);
            Log.d("CdmaSimStatus", "mccmnc = " + mccmnc);
            setSummaryText("current_operators_mccmnc", mccmnc);
        }
    }

    private String getMccMncProperty(Phone phone) {
        int phoneId = 0;
        if (phone != null) {
            phoneId = phone.getPhoneId();
        }
        TelephonyManager telephonyManager = this.mTelephonyManager;
        String value = TelephonyManager.getTelephonyProperty(phoneId, "gsm.operator.numeric", "");
        Log.d("CdmaSimStatus", "value = " + value);
        return value;
    }

    private void setCdmaSidNid() {
        if (this.mCdmaPhone != null) {
            String sid = this.mCdmaPhone.getSid();
            String sidnid = sid + "," + this.mCdmaPhone.getNid();
            Log.d("CdmaSimStatus", "sidnid = " + sidnid);
            setSummaryText("current_sidnid", sidnid);
        }
    }

    private void setCellId() {
        if (this.mCdmaPhone != null) {
            String cellId = Integer.toString(((CdmaCellLocation) this.mCdmaPhone.getCellLocation()).getBaseStationId());
            Log.d("CdmaSimStatus", "cellId = " + cellId);
            setSummaryText("current_cellid", cellId);
        }
    }

    private void setSummaryText(String key, String text) {
        if (TextUtils.isEmpty(text)) {
            text = this.mDefaultText;
        }
        Preference preference = this.mActivity.findPreference(key);
        if (preference != null) {
            preference.setSummary(text);
        }
    }

    static String renameNetworkTypeName(String netWorkTypeName) {
        Log.d("CdmaSimStatus", "renameNetworkTypeNameForCTSpec, netWorkTypeName=" + netWorkTypeName);
        if ("CDMA - EvDo rev. 0".equals(netWorkTypeName) || "CDMA - EvDo rev. A".equals(netWorkTypeName) || "CDMA - EvDo rev. B".equals(netWorkTypeName)) {
            return "CDMA EVDO";
        }
        if ("CDMA - 1xRTT".equals(netWorkTypeName)) {
            return "CDMA 1x";
        }
        if ("GPRS".equals(netWorkTypeName) || "EDGE".equals(netWorkTypeName)) {
            return "GSM";
        }
        if ("HSDPA".equals(netWorkTypeName) || "HSUPA".equals(netWorkTypeName) || "HSPA".equals(netWorkTypeName) || "HSPA+".equals(netWorkTypeName) || "UMTS".equals(netWorkTypeName)) {
            return "WCDMA";
        }
        if ("CDMA - eHRPD".equals(netWorkTypeName)) {
            return "eHRPD";
        }
        return netWorkTypeName;
    }

    private void removePreferenceFromScreen(String key) {
        Preference pref = this.mActivity.findPreference(key);
        if (pref != null) {
            this.mPreferenceScreen.removePreference(pref);
        }
    }
}
