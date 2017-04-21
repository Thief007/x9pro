package com.mediatek.keyguard.VoiceUnlock;

import android.view.View;

public interface BiometricSensorUnlock {
    void initializeView(View view);

    boolean start();

    boolean stop();

    void stopAndShowBackup();
}
