package com.mediatek.keyguard.ext;

import android.content.Intent;
import android.util.Log;
import com.mediatek.common.PluginImpl;

@PluginImpl(interfaceName = "com.mediatek.keyguard.ext.IEmergencyButtonExt")
public class DefaultEmergencyButtonExt implements IEmergencyButtonExt {
    private static final boolean DEBUG = true;
    private static final String TAG = "DefaultEmergencyButtonExt";

    public boolean showEccByServiceState(boolean[] isServiceSupportEcc, int slotId) {
        int simSlotCount = isServiceSupportEcc.length;
        for (int i = 0; i < simSlotCount; i++) {
            Log.d(TAG, "showEccByServiceState i = " + i + " isServiceSupportEcc[i] = " + isServiceSupportEcc[i]);
            if (isServiceSupportEcc[i]) {
                return DEBUG;
            }
        }
        return false;
    }

    public void customizeEmergencyIntent(Intent intent, int slotId) {
    }

    public boolean showEccInNonSecureUnlock() {
        Log.d(TAG, "showEccInNonSecureUnlock return false");
        return false;
    }
}
