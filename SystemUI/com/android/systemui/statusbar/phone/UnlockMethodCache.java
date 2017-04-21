package com.android.systemui.statusbar.phone;

import android.content.Context;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import java.util.ArrayList;

public class UnlockMethodCache {
    private static UnlockMethodCache sInstance;
    private final KeyguardUpdateMonitorCallback mCallback = new KeyguardUpdateMonitorCallback() {
        public void onUserSwitchComplete(int userId) {
            UnlockMethodCache.this.update(false);
        }

        public void onTrustChanged(int userId) {
            UnlockMethodCache.this.update(false);
        }

        public void onTrustManagedChanged(int userId) {
            UnlockMethodCache.this.update(false);
        }

        public void onStartedWakingUp() {
            UnlockMethodCache.this.update(false);
        }

        public void onFingerprintAuthenticated(int userId) {
            if (UnlockMethodCache.this.mKeyguardUpdateMonitor.isUnlockingWithFingerprintAllowed()) {
                UnlockMethodCache.this.update(false);
            }
        }

        public void onFaceUnlockStateChanged(boolean running, int userId) {
            UnlockMethodCache.this.update(false);
        }

        public void onStrongAuthStateChanged(int userId) {
            UnlockMethodCache.this.update(false);
        }
    };
    private boolean mCanSkipBouncer;
    private boolean mFaceUnlockRunning;
    private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private final ArrayList<OnUnlockMethodChangedListener> mListeners = new ArrayList();
    private final LockPatternUtils mLockPatternUtils;
    private boolean mSecure;
    private boolean mTrustManaged;
    private boolean mTrusted;

    public interface OnUnlockMethodChangedListener {
        void onUnlockMethodStateChanged();
    }

    private UnlockMethodCache(Context ctx) {
        this.mLockPatternUtils = new LockPatternUtils(ctx);
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(ctx);
        KeyguardUpdateMonitor.getInstance(ctx).registerCallback(this.mCallback);
        update(true);
    }

    public static UnlockMethodCache getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new UnlockMethodCache(context);
        }
        return sInstance;
    }

    public boolean isMethodSecure() {
        return this.mSecure;
    }

    public boolean isTrusted() {
        return this.mTrusted;
    }

    public boolean canSkipBouncer() {
        return this.mCanSkipBouncer;
    }

    public void addListener(OnUnlockMethodChangedListener listener) {
        this.mListeners.add(listener);
    }

    private void update(boolean updateAlways) {
        boolean z;
        int user = KeyguardUpdateMonitor.getCurrentUser();
        boolean secure = this.mLockPatternUtils.isSecure(user);
        boolean userCanSkipBouncer = secure ? this.mKeyguardUpdateMonitor.getUserCanSkipBouncer(user) : true;
        boolean trustManaged = this.mKeyguardUpdateMonitor.getUserTrustIsManaged(user);
        boolean trusted = this.mKeyguardUpdateMonitor.getUserHasTrust(user);
        if (this.mKeyguardUpdateMonitor.isFaceUnlockRunning(user)) {
            z = trustManaged;
        } else {
            z = false;
        }
        boolean changed = (secure == this.mSecure && userCanSkipBouncer == this.mCanSkipBouncer && trustManaged == this.mTrustManaged) ? z != this.mFaceUnlockRunning : true;
        if (changed || updateAlways) {
            this.mSecure = secure;
            this.mCanSkipBouncer = userCanSkipBouncer;
            this.mTrusted = trusted;
            this.mTrustManaged = trustManaged;
            this.mFaceUnlockRunning = z;
            notifyListeners();
        }
    }

    private void notifyListeners() {
        for (OnUnlockMethodChangedListener listener : this.mListeners) {
            listener.onUnlockMethodStateChanged();
        }
    }

    public boolean isTrustManaged() {
        return this.mTrustManaged;
    }

    public boolean isFaceUnlockRunning() {
        return this.mFaceUnlockRunning;
    }
}
