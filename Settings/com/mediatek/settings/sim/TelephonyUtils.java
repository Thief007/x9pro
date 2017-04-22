package com.mediatek.settings.sim;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings.System;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.RadioAccessFamily;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.ITelephony.Stub;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.settings.FeatureOption;
import java.util.Iterator;

public class TelephonyUtils {
    public static boolean isAirplaneModeOn(Context context) {
        return System.getInt(context.getContentResolver(), "airplane_mode_on", 0) != 0;
    }

    public static boolean isRadioOn(int subId, Context context) {
        ITelephony phone = Stub.asInterface(ServiceManager.getService("phone"));
        boolean isOn = false;
        if (phone == null) {
            Log.d("TelephonyUtils", "phone is null");
        } else if (subId == -1) {
            isOn = false;
        } else {
            try {
                isOn = phone.isRadioOnForSubscriber(subId, context.getPackageName());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        Log.d("TelephonyUtils", "isOn = " + isOn + ", subId: " + subId);
        return isOn;
    }

    public static boolean isCapabilitySwitching() {
        ITelephonyEx telephonyEx = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
        boolean isSwitching = false;
        if (telephonyEx != null) {
            try {
                isSwitching = telephonyEx.isCapabilitySwitching();
            } catch (RemoteException e) {
                Log.e("TelephonyUtils", "RemoteException = " + e);
            }
        } else {
            Log.d("TelephonyUtils", "mTelephonyEx is null, returen false");
        }
        Log.d("TelephonyUtils", "isSwitching = " + isSwitching);
        return isSwitching;
    }

    public static int phoneAccountHandleTosubscriptionId(Context context, PhoneAccountHandle handle) {
        int subId = -1;
        if (handle != null) {
            subId = TelephonyManager.from(context).getSubIdForPhoneAccount(TelecomManager.from(context).getPhoneAccount(handle));
        }
        Log.d("TelephonyUtils", "PhoneAccountHandleTosubscriptionId()... subId: " + subId);
        return subId;
    }

    public static PhoneAccountHandle subscriptionIdToPhoneAccountHandle(Context context, int subId) {
        TelecomManager telecomManager = TelecomManager.from(context);
        TelephonyManager telephonyManager = TelephonyManager.from(context);
        Iterator<PhoneAccountHandle> phoneAccounts = telecomManager.getCallCapablePhoneAccounts().listIterator();
        while (phoneAccounts.hasNext()) {
            PhoneAccountHandle phoneAccountHandle = (PhoneAccountHandle) phoneAccounts.next();
            if (subId == telephonyManager.getSubIdForPhoneAccount(telecomManager.getPhoneAccount(phoneAccountHandle))) {
                return phoneAccountHandle;
            }
        }
        return null;
    }

    public static int getMainCapabilitySlotId(Context context) {
        ITelephonyEx iTelEx = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
        int phoneId = -1;
        if (iTelEx != null) {
            try {
                phoneId = iTelEx.getMainCapabilityPhoneId();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        int slotId = SubscriptionManager.getSlotId(SubscriptionManager.getSubIdUsingPhoneId(phoneId));
        Log.d("TelephonyUtils", "getMainCapabilitySlotId()... slotId: " + slotId);
        return slotId;
    }

    public static boolean setRadioCapability(Context context, int targetSubId) {
        int phoneNum = TelephonyManager.from(context).getPhoneCount();
        boolean isSwitchSuccess = true;
        Log.d("TelephonyUtils", "setRadioCapability()...  targetSubId: " + targetSubId);
        String curr3GSim = SystemProperties.get("persist.radio.simswitch", "");
        Log.d("TelephonyUtils", "current 3G Sim = " + curr3GSim);
        if (curr3GSim == null || curr3GSim.equals("") || Integer.parseInt(curr3GSim) != SubscriptionManager.getPhoneId(targetSubId) + 1) {
            try {
                ITelephony iTel = Stub.asInterface(ServiceManager.getService("phone"));
                ITelephonyEx iTelEx = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
                if (iTel == null) {
                    Log.e("TelephonyUtils", "Can not get phone service");
                    return false;
                }
                boolean isLteSupport = FeatureOption.MTK_LTE_SUPPORT;
                RadioAccessFamily[] rafs = new RadioAccessFamily[phoneNum];
                for (int phoneId = 0; phoneId < phoneNum; phoneId++) {
                    int raf = iTel.getRadioAccessFamily(phoneId, context.getPackageName());
                    int id = SubscriptionManager.getSubIdUsingPhoneId(phoneId);
                    Log.d("TelephonyUtils", " phoneId=" + phoneId + " subId=" + id + " RAF=" + raf);
                    raf |= 65536;
                    if (id == targetSubId) {
                        raf |= 8;
                        if (isLteSupport) {
                            raf |= 16384;
                        }
                    } else {
                        raf &= -9;
                        if (isLteSupport) {
                            raf &= -16385;
                        }
                    }
                    Log.d("TelephonyUtils", " newRAF=" + raf);
                    rafs[phoneId] = new RadioAccessFamily(phoneId, raf);
                }
                if (!iTelEx.setRadioCapability(rafs)) {
                    Log.d("TelephonyUtils", "Set phone rat fail!!!");
                    isSwitchSuccess = false;
                }
                return isSwitchSuccess;
            } catch (RemoteException ex) {
                Log.d("TelephonyUtils", "Set phone rat fail!!!");
                ex.printStackTrace();
                isSwitchSuccess = false;
            }
        } else {
            Log.d("TelephonyUtils", "Current 3G phone equals target phone, don't trigger switch");
            return true;
        }
    }

    public static void setDefaultDataSubIdWithoutCapabilitySwitch(Context context, int subId) {
        SubscriptionManager.from(context).setDefaultDataSubIdWithoutCapabilitySwitch(subId);
    }
}
