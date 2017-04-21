package com.android.systemui.recents.views;

import android.app.Activity;
import android.view.View;
import android.view.ViewPropertyAnimator;
import com.android.systemui.R;
import com.android.systemui.recents.RecentsConfiguration;

public class SystemBarScrimViews {
    RecentsConfiguration mConfig;
    boolean mHasNavBarScrim;
    boolean mHasStatusBarScrim;
    View mNavBarScrimView;
    boolean mShouldAnimateNavBarScrim;
    boolean mShouldAnimateStatusBarScrim;
    View mStatusBarScrimView;

    public SystemBarScrimViews(Activity activity, RecentsConfiguration config) {
        this.mConfig = config;
        this.mStatusBarScrimView = activity.findViewById(R.id.status_bar_scrim);
        this.mNavBarScrimView = activity.findViewById(R.id.nav_bar_scrim);
    }

    public void prepareEnterRecentsAnimation() {
        int i;
        int i2 = 0;
        this.mHasNavBarScrim = this.mConfig.hasNavBarScrim();
        this.mShouldAnimateNavBarScrim = this.mConfig.shouldAnimateNavBarScrim();
        this.mHasStatusBarScrim = this.mConfig.hasStatusBarScrim();
        this.mShouldAnimateStatusBarScrim = this.mConfig.shouldAnimateStatusBarScrim();
        View view = this.mNavBarScrimView;
        if (!this.mHasNavBarScrim || this.mShouldAnimateNavBarScrim) {
            i = 4;
        } else {
            i = 0;
        }
        view.setVisibility(i);
        View view2 = this.mStatusBarScrimView;
        if (!this.mHasStatusBarScrim || this.mShouldAnimateStatusBarScrim) {
            i2 = 4;
        }
        view2.setVisibility(i2);
    }

    public void startEnterRecentsAnimation() {
        int i;
        if (this.mHasStatusBarScrim && this.mShouldAnimateStatusBarScrim) {
            this.mStatusBarScrimView.setTranslationY((float) (-this.mStatusBarScrimView.getMeasuredHeight()));
            ViewPropertyAnimator translationY = this.mStatusBarScrimView.animate().translationY(0.0f);
            if (this.mConfig.launchedFromHome) {
                i = this.mConfig.transitionEnterFromHomeDelay;
            } else {
                i = this.mConfig.transitionEnterFromAppDelay;
            }
            translationY.setStartDelay((long) i).setDuration((long) this.mConfig.navBarScrimEnterDuration).setInterpolator(this.mConfig.quintOutInterpolator).withStartAction(new Runnable() {
                public void run() {
                    SystemBarScrimViews.this.mStatusBarScrimView.setVisibility(0);
                }
            }).start();
        }
        if (this.mHasNavBarScrim && this.mShouldAnimateNavBarScrim) {
            this.mNavBarScrimView.setTranslationY((float) this.mNavBarScrimView.getMeasuredHeight());
            translationY = this.mNavBarScrimView.animate().translationY(0.0f);
            if (this.mConfig.launchedFromHome) {
                i = this.mConfig.transitionEnterFromHomeDelay;
            } else {
                i = this.mConfig.transitionEnterFromAppDelay;
            }
            translationY.setStartDelay((long) i).setDuration((long) this.mConfig.navBarScrimEnterDuration).setInterpolator(this.mConfig.quintOutInterpolator).withStartAction(new Runnable() {
                public void run() {
                    SystemBarScrimViews.this.mNavBarScrimView.setVisibility(0);
                }
            }).start();
        }
    }

    public void startExitRecentsAnimation() {
        if (this.mHasStatusBarScrim && this.mShouldAnimateStatusBarScrim) {
            this.mStatusBarScrimView.animate().translationY((float) (-this.mStatusBarScrimView.getMeasuredHeight())).setStartDelay(0).setDuration((long) this.mConfig.taskViewExitToAppDuration).setInterpolator(this.mConfig.fastOutSlowInInterpolator).start();
        }
        if (this.mHasNavBarScrim && this.mShouldAnimateNavBarScrim) {
            this.mNavBarScrimView.animate().translationY((float) this.mNavBarScrimView.getMeasuredHeight()).setStartDelay(0).setDuration((long) this.mConfig.taskViewExitToAppDuration).setInterpolator(this.mConfig.fastOutSlowInInterpolator).start();
        }
    }
}
