package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import com.android.systemui.R;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Field;

public class StatusBarWindowManager {
    private int mBarHeight;
    private final Context mContext;
    private final State mCurrentState = new State();
    private final boolean mKeyguardScreenRotation;
    private LayoutParams mLp;
    private LayoutParams mLpChanged;
    private final float mScreenBrightnessDoze;
    private View mStatusBarView;
    private final WindowManager mWindowManager;

    private static class State {
        boolean bouncerShowing;
        boolean forceCollapsed;
        boolean forceDozeBrightness;
        boolean forceStatusBarVisible;
        boolean headsUpShowing;
        boolean keyguardFadingAway;
        boolean keyguardNeedsInput;
        boolean keyguardOccluded;
        boolean keyguardShowing;
        boolean panelExpanded;
        boolean panelVisible;
        boolean qsExpanded;
        boolean statusBarFocusable;
        int statusBarState;

        private State() {
        }

        private boolean isKeyguardShowingAndNotOccluded() {
            return this.keyguardShowing && !this.keyguardOccluded;
        }

        public String toString() {
            StringBuilder result = new StringBuilder();
            String newLine = "\n";
            result.append("Window State {");
            result.append(newLine);
            for (Field field : getClass().getDeclaredFields()) {
                result.append("  ");
                try {
                    result.append(field.getName());
                    result.append(": ");
                    result.append(field.get(this));
                } catch (IllegalAccessException e) {
                }
                result.append(newLine);
            }
            result.append("}");
            return result.toString();
        }
    }

    public StatusBarWindowManager(Context context) {
        this.mContext = context;
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        this.mKeyguardScreenRotation = shouldEnableKeyguardScreenRotation();
        this.mScreenBrightnessDoze = ((float) this.mContext.getResources().getInteger(17694814)) / 255.0f;
    }

    private boolean shouldEnableKeyguardScreenRotation() {
        Resources res = this.mContext.getResources();
        if (SystemProperties.getBoolean("lockscreen.rot_override", false)) {
            return true;
        }
        return res.getBoolean(R.bool.config_enableLockScreenRotation);
    }

    public void add(View statusBarView, int barHeight) {
        this.mLp = new LayoutParams(-1, barHeight, 2000, -2138832824, -3);
        LayoutParams layoutParams = this.mLp;
        layoutParams.flags |= 16777216;
        this.mLp.gravity = 48;
        this.mLp.softInputMode = 16;
        this.mLp.setTitle("StatusBar");
        this.mLp.packageName = this.mContext.getPackageName();
        this.mStatusBarView = statusBarView;
        this.mBarHeight = barHeight;
        this.mWindowManager.addView(this.mStatusBarView, this.mLp);
        this.mLpChanged = new LayoutParams();
        this.mLpChanged.copyFrom(this.mLp);
    }

    private void applyKeyguardFlags(State state) {
        if (state.keyguardShowing) {
            LayoutParams layoutParams = this.mLpChanged;
            layoutParams.flags |= 1048576;
            layoutParams = this.mLpChanged;
            layoutParams.privateFlags |= 1024;
            return;
        }
        layoutParams = this.mLpChanged;
        layoutParams.flags &= -1048577;
        layoutParams = this.mLpChanged;
        layoutParams.privateFlags &= -1025;
    }

    private void adjustScreenOrientation(State state) {
        if (!state.isKeyguardShowingAndNotOccluded()) {
            this.mLpChanged.screenOrientation = -1;
        } else if (this.mKeyguardScreenRotation) {
            this.mLpChanged.screenOrientation = 2;
        } else {
            this.mLpChanged.screenOrientation = 5;
        }
    }

    private void applyFocusableFlag(State state) {
        boolean z = state.statusBarFocusable ? state.panelExpanded : false;
        LayoutParams layoutParams;
        if (state.keyguardShowing && state.keyguardNeedsInput && state.bouncerShowing) {
            layoutParams = this.mLpChanged;
            layoutParams.flags &= -9;
            layoutParams = this.mLpChanged;
            layoutParams.flags &= -131073;
        } else if (state.isKeyguardShowingAndNotOccluded() || z) {
            layoutParams = this.mLpChanged;
            layoutParams.flags &= -9;
            layoutParams = this.mLpChanged;
            layoutParams.flags |= 131072;
        } else {
            layoutParams = this.mLpChanged;
            layoutParams.flags |= 8;
            layoutParams = this.mLpChanged;
            layoutParams.flags &= -131073;
        }
    }

    private void applyHeight(State state) {
        if (isExpanded(state)) {
            this.mLpChanged.height = -1;
            return;
        }
        this.mLpChanged.height = this.mBarHeight;
    }

    private boolean isExpanded(State state) {
        if (state.forceCollapsed) {
            return false;
        }
        if (state.isKeyguardShowingAndNotOccluded() || state.panelVisible || state.keyguardFadingAway || state.bouncerShowing) {
            return true;
        }
        return state.headsUpShowing;
    }

    private void applyFitsSystemWindows(State state) {
        this.mStatusBarView.setFitsSystemWindows(!state.isKeyguardShowingAndNotOccluded());
    }

    private void applyUserActivityTimeout(State state) {
        if (state.isKeyguardShowingAndNotOccluded() && state.statusBarState == 1 && !state.qsExpanded) {
            this.mLpChanged.userActivityTimeout = 10000;
        } else {
            this.mLpChanged.userActivityTimeout = -1;
        }
    }

    private void applyInputFeatures(State state) {
        if (state.isKeyguardShowingAndNotOccluded() && state.statusBarState == 1 && !state.qsExpanded) {
            LayoutParams layoutParams = this.mLpChanged;
            layoutParams.inputFeatures |= 4;
            return;
        }
        layoutParams = this.mLpChanged;
        layoutParams.inputFeatures &= -5;
    }

    private void apply(State state) {
        applyKeyguardFlags(state);
        applyForceStatusBarVisibleFlag(state);
        applyFocusableFlag(state);
        adjustScreenOrientation(state);
        applyHeight(state);
        applyUserActivityTimeout(state);
        applyInputFeatures(state);
        applyFitsSystemWindows(state);
        applyModalFlag(state);
        applyBrightness(state);
        if (this.mLp.copyFrom(this.mLpChanged) != 0) {
            this.mWindowManager.updateViewLayout(this.mStatusBarView, this.mLp);
        }
    }

    private void applyForceStatusBarVisibleFlag(State state) {
        if (state.forceStatusBarVisible) {
            LayoutParams layoutParams = this.mLpChanged;
            layoutParams.privateFlags |= 4096;
            return;
        }
        layoutParams = this.mLpChanged;
        layoutParams.privateFlags &= -4097;
    }

    private void applyModalFlag(State state) {
        if (state.headsUpShowing) {
            LayoutParams layoutParams = this.mLpChanged;
            layoutParams.flags |= 32;
            return;
        }
        layoutParams = this.mLpChanged;
        layoutParams.flags &= -33;
    }

    private void applyBrightness(State state) {
        if (state.forceDozeBrightness) {
            this.mLpChanged.screenBrightness = this.mScreenBrightnessDoze;
            return;
        }
        this.mLpChanged.screenBrightness = -1.0f;
    }

    public void setKeyguardShowing(boolean showing) {
        this.mCurrentState.keyguardShowing = showing;
        apply(this.mCurrentState);
    }

    public void setKeyguardOccluded(boolean occluded) {
        this.mCurrentState.keyguardOccluded = occluded;
        apply(this.mCurrentState);
    }

    public void setKeyguardNeedsInput(boolean needsInput) {
        this.mCurrentState.keyguardNeedsInput = needsInput;
        apply(this.mCurrentState);
    }

    public void setPanelVisible(boolean visible) {
        this.mCurrentState.panelVisible = visible;
        this.mCurrentState.statusBarFocusable = visible;
        apply(this.mCurrentState);
    }

    public void setStatusBarFocusable(boolean focusable) {
        this.mCurrentState.statusBarFocusable = focusable;
        apply(this.mCurrentState);
    }

    public void setBouncerShowing(boolean showing) {
        this.mCurrentState.bouncerShowing = showing;
        apply(this.mCurrentState);
    }

    public void setKeyguardFadingAway(boolean keyguardFadingAway) {
        this.mCurrentState.keyguardFadingAway = keyguardFadingAway;
        apply(this.mCurrentState);
    }

    public void setQsExpanded(boolean expanded) {
        this.mCurrentState.qsExpanded = expanded;
        apply(this.mCurrentState);
    }

    public void setHeadsUpShowing(boolean showing) {
        this.mCurrentState.headsUpShowing = showing;
        apply(this.mCurrentState);
    }

    public void setStatusBarState(int state) {
        this.mCurrentState.statusBarState = state;
        apply(this.mCurrentState);
    }

    public void setForceStatusBarVisible(boolean forceStatusBarVisible) {
        this.mCurrentState.forceStatusBarVisible = forceStatusBarVisible;
        apply(this.mCurrentState);
    }

    public void setForceWindowCollapsed(boolean force) {
        this.mCurrentState.forceCollapsed = force;
        apply(this.mCurrentState);
    }

    public void setPanelExpanded(boolean isExpanded) {
        this.mCurrentState.panelExpanded = isExpanded;
        apply(this.mCurrentState);
    }

    public void setForceDozeBrightness(boolean forceDozeBrightness) {
        this.mCurrentState.forceDozeBrightness = forceDozeBrightness;
        apply(this.mCurrentState);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("StatusBarWindowManager state:");
        pw.println(this.mCurrentState);
    }
}
