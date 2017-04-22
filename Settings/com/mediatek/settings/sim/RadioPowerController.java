package com.mediatek.settings.sim;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.SubscriptionManager;
import android.util.Log;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.ITelephony.Stub;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISimManagementExt;

public class RadioPowerController {
    private static RadioPowerController sInstance = null;
    private Context mContext;
    private ISimManagementExt mExt = UtilsExt.getSimManagmentExtPlugin(this.mContext);

    private void updateRadioMsimDb(int r1, boolean r2) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: com.mediatek.settings.sim.RadioPowerController.updateRadioMsimDb(int, boolean):void
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:59)
	at jadx.core.ProcessClass.process(ProcessClass.java:42)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:306)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 6 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: com.mediatek.settings.sim.RadioPowerController.updateRadioMsimDb(int, boolean):void");
    }

    private RadioPowerController(Context context) {
        this.mContext = context;
    }

    private static synchronized void createInstance(Context context) {
        synchronized (RadioPowerController.class) {
            if (sInstance == null) {
                sInstance = new RadioPowerController(context);
            }
        }
    }

    public static RadioPowerController getInstance(Context context) {
        if (sInstance == null) {
            createInstance(context);
        }
        return sInstance;
    }

    public boolean needRadioSwitch(int subId, boolean turnOn) {
        boolean needSwitch = false;
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            boolean isRadioOn = TelephonyUtils.isRadioOn(subId, this.mContext);
            Log.d("RadioPowerController", "needRadioSwitch: subId: " + subId + ", isRadioOn: " + isRadioOn);
            if (isRadioOn != turnOn) {
                needSwitch = true;
            }
        }
        Log.d("RadioPowerController", "needRadioSwitch(" + subId + ")" + " : " + needSwitch + ", turnOn: " + turnOn);
        return needSwitch;
    }

    public boolean setRadionOn(int subId, boolean turnOn) {
        Log.d("RadioPowerController", "setRadionOn, turnOn: " + turnOn + ", subId = " + subId);
        boolean isSuccessful = false;
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return false;
        }
        ITelephony telephony = Stub.asInterface(ServiceManager.getService("phone"));
        if (telephony != null) {
            try {
                isSuccessful = telephony.setRadioForSubscriber(subId, turnOn);
                if (isSuccessful) {
                    updateRadioMsimDb(subId, turnOn);
                    this.mExt.setRadioPowerState(subId, turnOn);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            Log.d("RadioPowerController", "telephony is null");
        }
        Log.d("RadioPowerController", "setRadionOn, isSuccessful: " + isSuccessful);
        return isSuccessful;
    }
}
