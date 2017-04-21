package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.AccelerateInterpolator;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.IStatusBarService.Stub;
import com.android.systemui.R;

public final class NavigationBarTransitions extends BarTransitions {
    private final IStatusBarService mBarService;
    private boolean mLightsOut;
    private final OnTouchListener mLightsOutListener = new OnTouchListener() {
        public boolean onTouch(View v, MotionEvent ev) {
            if (ev.getAction() == 0) {
                NavigationBarTransitions.this.applyLightsOut(false, false, false);
                try {
                    NavigationBarTransitions.this.mBarService.setSystemUiVisibility(0, 1, "LightsOutListener");
                } catch (RemoteException e) {
                }
            }
            return false;
        }
    };
    private final NavigationBarView mView;

    public NavigationBarTransitions(NavigationBarView view) {
        super(view, R.drawable.nav_background);
        this.mView = view;
        this.mBarService = Stub.asInterface(ServiceManager.getService("statusbar"));
    }

    public void init() {
        applyModeBackground(-1, getMode(), false);
        applyMode(getMode(), false, true);
    }

    protected void onTransition(int oldMode, int newMode, boolean animate) {
        super.onTransition(oldMode, newMode, animate);
        applyMode(newMode, animate, false);
    }

    private void applyMode(int mode, boolean animate, boolean force) {
        applyLightsOut(isLightsOut(mode), animate, force);
    }

    private void applyLightsOut(boolean lightsOut, boolean animate, boolean force) {
        int i = 0;
        if (force || lightsOut != this.mLightsOut) {
            this.mLightsOut = lightsOut;
            View navButtons = this.mView.getCurrentView().findViewById(R.id.nav_buttons);
            final View lowLights = this.mView.getCurrentView().findViewById(R.id.lights_out);
            navButtons.animate().cancel();
            lowLights.animate().cancel();
            float navButtonsAlpha = lightsOut ? 0.0f : 1.0f;
            float lowLightsAlpha = lightsOut ? 1.0f : 0.0f;
            if (animate) {
                int duration = lightsOut ? 750 : 250;
                navButtons.animate().alpha(navButtonsAlpha).setDuration((long) duration).start();
                lowLights.setOnTouchListener(this.mLightsOutListener);
                if (lowLights.getVisibility() == 8) {
                    lowLights.setAlpha(0.0f);
                    lowLights.setVisibility(0);
                }
                lowLights.animate().alpha(lowLightsAlpha).setDuration((long) duration).setInterpolator(new AccelerateInterpolator(2.0f)).setListener(lightsOut ? null : new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator _a) {
                        lowLights.setVisibility(8);
                    }
                }).start();
            } else {
                navButtons.setAlpha(navButtonsAlpha);
                lowLights.setAlpha(lowLightsAlpha);
                if (!lightsOut) {
                    i = 8;
                }
                lowLights.setVisibility(i);
            }
        }
    }
}
