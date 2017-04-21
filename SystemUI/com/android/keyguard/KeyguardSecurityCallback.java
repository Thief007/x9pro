package com.android.keyguard;

import com.android.keyguard.KeyguardHostView.OnDismissAction;

public interface KeyguardSecurityCallback {
    void dismiss(boolean z);

    boolean hasOnDismissAction();

    void reportUnlockAttempt(boolean z, int i);

    void reset();

    void setOnDismissAction(OnDismissAction onDismissAction);

    void showBackupSecurity();

    void userActivity();
}
