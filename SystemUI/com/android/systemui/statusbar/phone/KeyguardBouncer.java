package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardHostView;
import com.android.keyguard.KeyguardHostView.OnDismissAction;
import com.android.keyguard.KeyguardSecurityModel;
import com.android.keyguard.KeyguardSecurityModel.SecurityMode;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.DejankUtils;
import com.android.systemui.R;
import com.mediatek.keyguard.PowerOffAlarm.PowerOffAlarmManager;

public class KeyguardBouncer {
    private final boolean DEBUG = true;
    private final String TAG = "KeyguardBouncer";
    private int mBouncerPromptReason;
    private ViewMediatorCallback mCallback;
    private ViewGroup mContainer;
    private Context mContext;
    private KeyguardHostView mKeyguardView;
    private LockPatternUtils mLockPatternUtils;
    private ViewGroup mNotificationPanel;
    private ViewGroup mRoot;
    private KeyguardSecurityModel mSecurityModel;
    private final Runnable mShowRunnable = new Runnable() {
        public void run() {
            Log.d("KeyguardBouncer", "mShowRunnable.run() is called.");
            KeyguardBouncer.this.mRoot.setVisibility(0);
            KeyguardBouncer.this.mKeyguardView.onResume();
            KeyguardBouncer.this.showPromptReason(KeyguardBouncer.this.mBouncerPromptReason);
            KeyguardBouncer.this.mKeyguardView.startAppearAnimation();
            KeyguardBouncer.this.mShowingSoon = false;
            KeyguardBouncer.this.mKeyguardView.sendAccessibilityEvent(32);
        }
    };
    private boolean mShowingSoon;
    private KeyguardUpdateMonitorCallback mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
        public void onStrongAuthStateChanged(int userId) {
            KeyguardBouncer.this.mBouncerPromptReason = KeyguardBouncer.this.mCallback.getBouncerPromptReason();
        }
    };
    private StatusBarWindowManager mWindowManager;

    public KeyguardBouncer(Context context, ViewMediatorCallback callback, LockPatternUtils lockPatternUtils, StatusBarWindowManager windowManager, ViewGroup container) {
        this.mContext = context;
        this.mCallback = callback;
        this.mLockPatternUtils = lockPatternUtils;
        this.mContainer = container;
        this.mWindowManager = windowManager;
        this.mNotificationPanel = (ViewGroup) this.mContainer.findViewById(R.id.notification_panel);
        this.mSecurityModel = new KeyguardSecurityModel(this.mContext);
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mUpdateMonitorCallback);
    }

    public void show(boolean resetSecuritySelection) {
        show(resetSecuritySelection, false);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void show(boolean resetSecuritySelection, boolean authenticated) {
        Log.d("KeyguardBouncer", "show(resetSecuritySelection = " + resetSecuritySelection);
        if (PowerOffAlarmManager.isAlarmBoot()) {
            Log.d("KeyguardBouncer", "show() - this is alarm boot, just re-inflate.");
            if (!(this.mKeyguardView == null || this.mRoot == null)) {
                Log.d("KeyguardBouncer", "show() - before re-inflate, we should pause current view.");
                this.mKeyguardView.onPause();
            }
            inflateView();
        } else {
            ensureView();
        }
        if (resetSecuritySelection) {
            this.mKeyguardView.showPrimarySecurityScreen();
        }
        if (!(this.mRoot.getVisibility() == 0 || this.mShowingSoon || this.mKeyguardView.dismiss(authenticated))) {
            Log.d("KeyguardBouncer", "show() - try to dismiss \"Bouncer\" directly.");
            this.mShowingSoon = true;
            DejankUtils.postAfterTraversal(this.mShowRunnable);
        }
    }

    public void showPromptReason(int reason) {
        this.mKeyguardView.showPromptReason(reason);
    }

    public void showMessage(String message, int color) {
        this.mKeyguardView.showMessage(message, color);
    }

    private void cancelShowRunnable() {
        DejankUtils.removeCallbacks(this.mShowRunnable);
        this.mShowingSoon = false;
    }

    public void showWithDismissAction(OnDismissAction r) {
        ensureView();
        this.mKeyguardView.setOnDismissAction(r);
        show(false);
    }

    public void hide(boolean destroyView) {
        Log.d("KeyguardBouncer", "hide() is called, destroyView = " + destroyView);
        cancelShowRunnable();
        if (this.mKeyguardView != null) {
            this.mKeyguardView.cancelDismissAction();
            this.mKeyguardView.cleanUp();
        }
        if (destroyView) {
            Log.d("KeyguardBouncer", "call removeView()");
            removeView();
        } else if (this.mRoot != null) {
            Log.d("KeyguardBouncer", "just set keyguard Invisible.");
            this.mRoot.setVisibility(4);
        }
        Log.d("KeyguardBouncer", "hide() - user has left keyguard, setAlternateUnlockEnabled(true)");
        KeyguardUpdateMonitor.getInstance(this.mContext).setAlternateUnlockEnabled(true);
    }

    public void startPreHideAnimation(Runnable runnable) {
        if (this.mKeyguardView != null) {
            this.mKeyguardView.startDisappearAnimation(runnable);
        } else if (runnable != null) {
            runnable.run();
        }
    }

    public void onScreenTurnedOff() {
        if (this.mKeyguardView != null && this.mRoot != null && this.mRoot.getVisibility() == 0) {
            this.mKeyguardView.onScreenTurnedOff();
            this.mKeyguardView.onPause();
        }
    }

    public boolean isShowing() {
        if (this.mShowingSoon) {
            return true;
        }
        return this.mRoot != null && this.mRoot.getVisibility() == 0;
    }

    public void prepare() {
        boolean wasInitialized = this.mRoot != null;
        ensureView();
        if (wasInitialized) {
            this.mKeyguardView.showPrimarySecurityScreen();
        }
        this.mBouncerPromptReason = this.mCallback.getBouncerPromptReason();
    }

    private void ensureView() {
        if (this.mRoot == null) {
            inflateView();
        }
    }

    private void inflateView() {
        Log.d("KeyguardBouncer", "inflateView() is called, we force to re-inflate the \"Bouncer\" view.");
        removeView();
        this.mRoot = (ViewGroup) LayoutInflater.from(this.mContext).inflate(R.layout.keyguard_bouncer, null);
        this.mKeyguardView = (KeyguardHostView) this.mRoot.findViewById(R.id.keyguard_host_view);
        this.mKeyguardView.setLockPatternUtils(this.mLockPatternUtils);
        this.mKeyguardView.setViewMediatorCallback(this.mCallback);
        this.mKeyguardView.setNotificationPanelView(this.mNotificationPanel);
        this.mContainer.addView(this.mRoot, this.mContainer.getChildCount());
        this.mRoot.setVisibility(4);
        this.mRoot.setSystemUiVisibility(2097152);
    }

    private void removeView() {
        if (this.mRoot != null && this.mRoot.getParent() == this.mContainer) {
            Log.d("KeyguardBouncer", "removeView() - really remove all views.");
            this.mContainer.removeView(this.mRoot);
            this.mRoot = null;
        }
    }

    public boolean needsFullscreenBouncer() {
        ensureView();
        SecurityMode mode = this.mSecurityModel.getSecurityMode();
        if (mode == SecurityMode.SimPinPukMe1 || mode == SecurityMode.SimPinPukMe2 || mode == SecurityMode.SimPinPukMe3 || mode == SecurityMode.SimPinPukMe4 || mode == SecurityMode.AntiTheft || mode == SecurityMode.AlarmBoot) {
            return true;
        }
        return false;
    }

    public boolean isFullscreenBouncer() {
        boolean z = true;
        if (this.mKeyguardView == null) {
            return false;
        }
        SecurityMode mode = this.mKeyguardView.getCurrentSecurityMode();
        if (!(mode == SecurityMode.SimPinPukMe1 || mode == SecurityMode.SimPinPukMe2 || mode == SecurityMode.SimPinPukMe3 || mode == SecurityMode.SimPinPukMe4 || mode == SecurityMode.AntiTheft || mode == SecurityMode.AlarmBoot)) {
            z = false;
        }
        return z;
    }

    public boolean isSecure() {
        return this.mKeyguardView == null || this.mKeyguardView.getSecurityMode() != SecurityMode.None;
    }

    public boolean onMenuPressed() {
        ensureView();
        if (!this.mKeyguardView.handleMenuKey()) {
            return false;
        }
        this.mRoot.setVisibility(0);
        this.mKeyguardView.requestFocus();
        this.mKeyguardView.onResume();
        return true;
    }

    public boolean interceptMediaKey(KeyEvent event) {
        ensureView();
        return this.mKeyguardView.interceptMediaKey(event);
    }

    public void notifyKeyguardAuthenticated(boolean strongAuth) {
        ensureView();
        this.mKeyguardView.finish(strongAuth);
    }
}
