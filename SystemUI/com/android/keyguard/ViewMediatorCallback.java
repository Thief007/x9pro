package com.android.keyguard;

import android.os.Bundle;

public interface ViewMediatorCallback {
    void adjustStatusBarLocked();

    void dismiss(boolean z);

    int getBouncerPromptReason();

    void hideLocked();

    boolean isInputRestricted();

    boolean isKeyguardDoneOnGoing();

    boolean isKeyguardExternallyEnabled();

    boolean isScreenOn();

    boolean isSecure();

    boolean isShowing();

    void keyguardDone(boolean z);

    void keyguardDoneDrawing();

    void keyguardDonePending(boolean z);

    void keyguardGone();

    void playTrustedSound();

    void readyForKeyguardDone();

    void resetKeyguard();

    void resetStateLocked();

    void setNeedsInput(boolean z);

    void setSuppressPlaySoundFlag();

    void showLocked(Bundle bundle);

    void updateAntiTheftLocked();

    void updateNavbarStatus();

    void userActivity();
}
