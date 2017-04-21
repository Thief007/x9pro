package com.mediatek.keyguard.ext;

import android.content.Intent;

public interface IEmergencyButtonExt {
    void customizeEmergencyIntent(Intent intent, int i);

    boolean showEccByServiceState(boolean[] zArr, int i);

    boolean showEccInNonSecureUnlock();
}
