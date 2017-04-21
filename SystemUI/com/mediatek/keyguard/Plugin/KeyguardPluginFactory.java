package com.mediatek.keyguard.Plugin;

import android.content.Context;
import android.util.Log;
import com.mediatek.common.MPlugin;
import com.mediatek.keyguard.ext.DefaultCarrierTextExt;
import com.mediatek.keyguard.ext.DefaultEmergencyButtonExt;
import com.mediatek.keyguard.ext.DefaultKeyguardUtilExt;
import com.mediatek.keyguard.ext.DefaultOperatorSIMString;
import com.mediatek.keyguard.ext.ICarrierTextExt;
import com.mediatek.keyguard.ext.IEmergencyButtonExt;
import com.mediatek.keyguard.ext.IKeyguardUtilExt;
import com.mediatek.keyguard.ext.IOperatorSIMString;

public class KeyguardPluginFactory {
    private static ICarrierTextExt mCarrierTextExt = null;
    private static IEmergencyButtonExt mEmergencyButtonExt = null;
    private static IKeyguardUtilExt mKeyguardUtilExt = null;
    private static IOperatorSIMString mOperatorSIMString = null;

    public static synchronized IEmergencyButtonExt getEmergencyButtonExt(Context context) {
        IEmergencyButtonExt iEmergencyButtonExt;
        synchronized (KeyguardPluginFactory.class) {
            if (mEmergencyButtonExt == null) {
                mEmergencyButtonExt = (IEmergencyButtonExt) MPlugin.createInstance(IEmergencyButtonExt.class.getName(), context);
                Log.d("KeyguardPluginFactory", "getEmergencyButtonExt emergencyButtonExt= " + mEmergencyButtonExt);
                if (mEmergencyButtonExt == null) {
                    mEmergencyButtonExt = new DefaultEmergencyButtonExt();
                    Log.d("KeyguardPluginFactory", "getEmergencyButtonExt get DefaultEmergencyButtonExt = " + mEmergencyButtonExt);
                }
            }
            iEmergencyButtonExt = mEmergencyButtonExt;
        }
        return iEmergencyButtonExt;
    }

    public static synchronized ICarrierTextExt getCarrierTextExt(Context context) {
        ICarrierTextExt iCarrierTextExt;
        synchronized (KeyguardPluginFactory.class) {
            if (mCarrierTextExt == null) {
                mCarrierTextExt = (ICarrierTextExt) MPlugin.createInstance(ICarrierTextExt.class.getName(), context);
                Log.d("KeyguardPluginFactory", "getCarrierTextExt carrierTextExt= " + mCarrierTextExt);
                if (mCarrierTextExt == null) {
                    mCarrierTextExt = new DefaultCarrierTextExt();
                    Log.d("KeyguardPluginFactory", "getCarrierTextExt get DefaultCarrierTextExt = " + mCarrierTextExt);
                }
            }
            iCarrierTextExt = mCarrierTextExt;
        }
        return iCarrierTextExt;
    }

    public static synchronized IKeyguardUtilExt getKeyguardUtilExt(Context context) {
        IKeyguardUtilExt iKeyguardUtilExt;
        synchronized (KeyguardPluginFactory.class) {
            if (mKeyguardUtilExt == null) {
                mKeyguardUtilExt = (IKeyguardUtilExt) MPlugin.createInstance(IKeyguardUtilExt.class.getName(), context);
                Log.d("KeyguardPluginFactory", "getKeyguardUtilExt keyguardUtilExt= " + mKeyguardUtilExt);
                if (mKeyguardUtilExt == null) {
                    mKeyguardUtilExt = new DefaultKeyguardUtilExt();
                    Log.d("KeyguardPluginFactory", "getKeyguardUtilExt get DefaultKeyguardUtilExt = " + mKeyguardUtilExt);
                }
            }
            iKeyguardUtilExt = mKeyguardUtilExt;
        }
        return iKeyguardUtilExt;
    }

    public static synchronized IOperatorSIMString getOperatorSIMString(Context context) {
        IOperatorSIMString iOperatorSIMString;
        synchronized (KeyguardPluginFactory.class) {
            if (mOperatorSIMString == null) {
                mOperatorSIMString = (IOperatorSIMString) MPlugin.createInstance(IOperatorSIMString.class.getName(), context);
                Log.d("KeyguardPluginFactory", "getOperatorSIMString operatorSIMString= " + mOperatorSIMString);
                if (mOperatorSIMString == null) {
                    mOperatorSIMString = new DefaultOperatorSIMString();
                    Log.d("KeyguardPluginFactory", "getOperatorSIMString get DefaultOperatorSIMString = " + mOperatorSIMString);
                }
            }
            iOperatorSIMString = mOperatorSIMString;
        }
        return iOperatorSIMString;
    }
}
