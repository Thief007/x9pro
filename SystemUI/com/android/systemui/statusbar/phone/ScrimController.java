package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.Color;
import android.view.View;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import com.android.systemui.R;
import com.android.systemui.statusbar.BackDropView;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.NotificationData.Entry;
import com.android.systemui.statusbar.ScrimView;
import com.android.systemui.statusbar.policy.HeadsUpManager.OnHeadsUpChangedListener;
import com.android.systemui.statusbar.stack.StackStateAnimator;

public class ScrimController implements OnPreDrawListener, OnHeadsUpChangedListener {
    public static final Interpolator KEYGUARD_FADE_OUT_INTERPOLATOR = new PathInterpolator(0.0f, 0.0f, 0.7f, 1.0f);
    private boolean mAnimateChange;
    private boolean mAnimateKeyguardFadingOut;
    private long mAnimationDelay;
    private BackDropView mBackDropView;
    private boolean mBouncerShowing;
    private float mCurrentBehindAlpha;
    private float mCurrentHeadsUpAlpha = 1.0f;
    private float mCurrentInFrontAlpha;
    private boolean mDarkenWhileDragging;
    private float mDozeBehindAlpha;
    private float mDozeInFrontAlpha;
    private boolean mDozing;
    private View mDraggedHeadsUpView;
    private long mDurationOverride = -1;
    private boolean mExpanding;
    private boolean mForceHideScrims;
    private float mFraction;
    private final View mHeadsUpScrim;
    private final Interpolator mInterpolator = new DecelerateInterpolator();
    private boolean mKeyguardShowing;
    private Runnable mOnAnimationFinished;
    private int mPinnedHeadsUpCount;
    private final ScrimView mScrimBehind;
    private final ScrimView mScrimInFront;
    private boolean mScrimSrcEnabled;
    private float mTopHeadsUpDragAmount;
    private final UnlockMethodCache mUnlockMethodCache;
    private boolean mUpdatePending;
    private boolean mWakeAndUnlocking;

    public ScrimController(ScrimView scrimBehind, ScrimView scrimInFront, View headsUpScrim, boolean scrimSrcEnabled) {
        this.mScrimBehind = scrimBehind;
        this.mScrimInFront = scrimInFront;
        this.mHeadsUpScrim = headsUpScrim;
        this.mUnlockMethodCache = UnlockMethodCache.getInstance(scrimBehind.getContext());
        this.mScrimSrcEnabled = scrimSrcEnabled;
        updateHeadsUpScrim(false);
    }

    public void setKeyguardShowing(boolean showing) {
        this.mKeyguardShowing = showing;
        scheduleUpdate();
    }

    public void onTrackingStarted() {
        boolean z = true;
        this.mExpanding = true;
        if (this.mUnlockMethodCache.canSkipBouncer()) {
            z = false;
        }
        this.mDarkenWhileDragging = z;
    }

    public void onExpandingFinished() {
        this.mExpanding = false;
    }

    public void setPanelExpansion(float fraction) {
        if (this.mFraction != fraction) {
            this.mFraction = fraction;
            scheduleUpdate();
            if (this.mPinnedHeadsUpCount != 0) {
                updateHeadsUpScrim(false);
            }
        }
    }

    public void setBouncerShowing(boolean showing) {
        this.mBouncerShowing = showing;
        this.mAnimateChange = !this.mExpanding;
        scheduleUpdate();
    }

    public void setWakeAndUnlocking() {
        this.mWakeAndUnlocking = true;
        scheduleUpdate();
    }

    public void animateKeyguardFadingOut(long delay, long duration, Runnable onAnimationFinished) {
        this.mWakeAndUnlocking = false;
        this.mAnimateKeyguardFadingOut = true;
        this.mDurationOverride = duration;
        this.mAnimationDelay = delay;
        this.mAnimateChange = true;
        this.mOnAnimationFinished = onAnimationFinished;
        scheduleUpdate();
    }

    public void abortKeyguardFadingOut() {
        if (this.mAnimateKeyguardFadingOut) {
            endAnimateKeyguardFadingOut(true);
        }
    }

    public void animateGoingToFullShade(long delay, long duration) {
        this.mDurationOverride = duration;
        this.mAnimationDelay = delay;
        this.mAnimateChange = true;
        scheduleUpdate();
    }

    public void setDozing(boolean dozing) {
        if (this.mDozing != dozing) {
            this.mDozing = dozing;
            scheduleUpdate();
        }
    }

    public void setDozeInFrontAlpha(float alpha) {
        this.mDozeInFrontAlpha = alpha;
        updateScrimColor(this.mScrimInFront);
    }

    public void setDozeBehindAlpha(float alpha) {
        this.mDozeBehindAlpha = alpha;
        updateScrimColor(this.mScrimBehind);
    }

    public float getDozeBehindAlpha() {
        return this.mDozeBehindAlpha;
    }

    public float getDozeInFrontAlpha() {
        return this.mDozeInFrontAlpha;
    }

    private void scheduleUpdate() {
        if (!this.mUpdatePending) {
            this.mScrimBehind.invalidate();
            this.mScrimBehind.getViewTreeObserver().addOnPreDrawListener(this);
            this.mUpdatePending = true;
        }
    }

    private void updateScrims() {
        if (this.mAnimateKeyguardFadingOut || this.mForceHideScrims) {
            setScrimInFrontColor(0.0f);
            setScrimBehindColor(0.0f);
        } else if (this.mWakeAndUnlocking) {
            if (this.mDozing) {
                setScrimInFrontColor(0.0f);
                setScrimBehindColor(1.0f);
            } else {
                setScrimInFrontColor(1.0f);
                setScrimBehindColor(0.0f);
            }
        } else if (this.mKeyguardShowing || this.mBouncerShowing) {
            updateScrimKeyguard();
        } else {
            updateScrimNormal();
            setScrimInFrontColor(0.0f);
        }
        this.mAnimateChange = false;
    }

    private void updateScrimKeyguard() {
        float fraction;
        if (this.mExpanding && this.mDarkenWhileDragging) {
            float behindFraction = Math.max(0.0f, Math.min(this.mFraction, 1.0f));
            fraction = (float) Math.pow((double) (1.0f - behindFraction), 0.800000011920929d);
            behindFraction = (float) Math.pow((double) behindFraction, 0.800000011920929d);
            setScrimInFrontColor(fraction * 0.75f);
            setScrimBehindColor(0.45f * behindFraction);
        } else if (this.mBouncerShowing) {
            setScrimInFrontColor(0.75f);
            setScrimBehindColor(0.0f);
        } else {
            fraction = Math.max(0.0f, Math.min(this.mFraction, 1.0f));
            setScrimInFrontColor(0.0f);
            setScrimBehindColor((0.24999999f * fraction) + 0.2f);
        }
    }

    private void updateScrimNormal() {
        float frac = (1.2f * this.mFraction) - 0.2f;
        if (frac <= 0.0f) {
            setScrimBehindColor(0.0f);
        } else {
            setScrimBehindColor(0.62f * ((float) (1.0d - ((1.0d - Math.cos(Math.pow((double) (1.0f - frac), 2.0d) * 3.141590118408203d)) * 0.5d))));
        }
    }

    private void setScrimBehindColor(float alpha) {
        setScrimColor(this.mScrimBehind, alpha);
    }

    private void setScrimInFrontColor(float alpha) {
        boolean z = false;
        setScrimColor(this.mScrimInFront, alpha);
        if (alpha == 0.0f) {
            this.mScrimInFront.setClickable(false);
            return;
        }
        ScrimView scrimView = this.mScrimInFront;
        if (!this.mDozing) {
            z = true;
        }
        scrimView.setClickable(z);
    }

    private void setScrimColor(View scrim, float alpha) {
        ValueAnimator runningAnim = (ValueAnimator) scrim.getTag(R.id.scrim);
        Float target = (Float) scrim.getTag(R.id.scrim_target);
        if (!(runningAnim == null || target == null)) {
            if (alpha != target.floatValue()) {
                runningAnim.cancel();
            } else {
                return;
            }
        }
        if (this.mAnimateChange) {
            startScrimAnimation(scrim, alpha);
        } else {
            setCurrentScrimAlpha(scrim, alpha);
            updateScrimColor(scrim);
        }
    }

    private float getDozeAlpha(View scrim) {
        return scrim == this.mScrimBehind ? this.mDozeBehindAlpha : this.mDozeInFrontAlpha;
    }

    private float getCurrentScrimAlpha(View scrim) {
        if (scrim == this.mScrimBehind) {
            return this.mCurrentBehindAlpha;
        }
        if (scrim == this.mScrimInFront) {
            return this.mCurrentInFrontAlpha;
        }
        return this.mCurrentHeadsUpAlpha;
    }

    private void setCurrentScrimAlpha(View scrim, float alpha) {
        if (scrim == this.mScrimBehind) {
            this.mCurrentBehindAlpha = alpha;
        } else if (scrim == this.mScrimInFront) {
            this.mCurrentInFrontAlpha = alpha;
        } else {
            this.mCurrentHeadsUpAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
        }
    }

    private void updateScrimColor(View scrim) {
        float alpha1 = getCurrentScrimAlpha(scrim);
        if (scrim instanceof ScrimView) {
            ((ScrimView) scrim).setScrimColor(Color.argb((int) (255.0f * (1.0f - ((1.0f - alpha1) * (1.0f - getDozeAlpha(scrim))))), 0, 0, 0));
        } else {
            scrim.setAlpha(alpha1);
        }
    }

    private void startScrimAnimation(final View scrim, float target) {
        ValueAnimator anim = ValueAnimator.ofFloat(new float[]{getCurrentScrimAlpha(scrim), target});
        anim.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                ScrimController.this.setCurrentScrimAlpha(scrim, ((Float) animation.getAnimatedValue()).floatValue());
                ScrimController.this.updateScrimColor(scrim);
            }
        });
        anim.setInterpolator(getInterpolator());
        anim.setStartDelay(this.mAnimationDelay);
        anim.setDuration(this.mDurationOverride != -1 ? this.mDurationOverride : 220);
        anim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                if (ScrimController.this.mOnAnimationFinished != null) {
                    ScrimController.this.mOnAnimationFinished.run();
                    ScrimController.this.mOnAnimationFinished = null;
                }
                scrim.setTag(R.id.scrim, null);
                scrim.setTag(R.id.scrim_target, null);
            }
        });
        anim.start();
        scrim.setTag(R.id.scrim, anim);
        scrim.setTag(R.id.scrim_target, Float.valueOf(target));
    }

    private Interpolator getInterpolator() {
        return this.mAnimateKeyguardFadingOut ? KEYGUARD_FADE_OUT_INTERPOLATOR : this.mInterpolator;
    }

    public boolean onPreDraw() {
        this.mScrimBehind.getViewTreeObserver().removeOnPreDrawListener(this);
        this.mUpdatePending = false;
        updateScrims();
        this.mDurationOverride = -1;
        this.mAnimationDelay = 0;
        endAnimateKeyguardFadingOut(false);
        return true;
    }

    private void endAnimateKeyguardFadingOut(boolean force) {
        this.mAnimateKeyguardFadingOut = false;
        if ((force || !(isAnimating(this.mScrimInFront) || isAnimating(this.mScrimBehind))) && this.mOnAnimationFinished != null) {
            this.mOnAnimationFinished.run();
            this.mOnAnimationFinished = null;
        }
    }

    private boolean isAnimating(View scrim) {
        return scrim.getTag(R.id.scrim) != null;
    }

    public void setBackDropView(BackDropView backDropView) {
        this.mBackDropView = backDropView;
        this.mBackDropView.setOnVisibilityChangedRunnable(new Runnable() {
            public void run() {
                ScrimController.this.updateScrimBehindDrawingMode();
            }
        });
        updateScrimBehindDrawingMode();
    }

    private void updateScrimBehindDrawingMode() {
        this.mScrimBehind.setDrawAsSrc(this.mBackDropView.getVisibility() != 0 ? this.mScrimSrcEnabled : false);
    }

    public void onHeadsUpPinnedModeChanged(boolean inPinnedMode) {
    }

    public void onHeadsUpPinned(ExpandableNotificationRow headsUp) {
        this.mPinnedHeadsUpCount++;
        updateHeadsUpScrim(true);
    }

    public void onHeadsUpUnPinned(ExpandableNotificationRow headsUp) {
        this.mPinnedHeadsUpCount--;
        if (headsUp == this.mDraggedHeadsUpView) {
            this.mDraggedHeadsUpView = null;
            this.mTopHeadsUpDragAmount = 0.0f;
        }
        updateHeadsUpScrim(true);
    }

    public void onHeadsUpStateChanged(Entry entry, boolean isHeadsUp) {
    }

    private void updateHeadsUpScrim(boolean animate) {
        float alpha = calculateHeadsUpAlpha();
        ValueAnimator previousAnimator = (ValueAnimator) StackStateAnimator.getChildTag(this.mHeadsUpScrim, R.id.scrim);
        float animEndValue = -1.0f;
        if (previousAnimator != null) {
            if (animate || alpha == this.mCurrentHeadsUpAlpha) {
                previousAnimator.cancel();
            } else {
                animEndValue = ((Float) StackStateAnimator.getChildTag(this.mHeadsUpScrim, R.id.hun_scrim_alpha_end)).floatValue();
            }
        }
        if (alpha != this.mCurrentHeadsUpAlpha && alpha != animEndValue) {
            if (animate) {
                startScrimAnimation(this.mHeadsUpScrim, alpha);
                this.mHeadsUpScrim.setTag(R.id.hun_scrim_alpha_start, Float.valueOf(this.mCurrentHeadsUpAlpha));
                this.mHeadsUpScrim.setTag(R.id.hun_scrim_alpha_end, Float.valueOf(alpha));
            } else if (previousAnimator != null) {
                float newStartValue = ((Float) StackStateAnimator.getChildTag(this.mHeadsUpScrim, R.id.hun_scrim_alpha_start)).floatValue() + (alpha - ((Float) StackStateAnimator.getChildTag(this.mHeadsUpScrim, R.id.hun_scrim_alpha_end)).floatValue());
                previousAnimator.getValues()[0].setFloatValues(new float[]{newStartValue, alpha});
                this.mHeadsUpScrim.setTag(R.id.hun_scrim_alpha_start, Float.valueOf(newStartValue));
                this.mHeadsUpScrim.setTag(R.id.hun_scrim_alpha_end, Float.valueOf(alpha));
                previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
            } else {
                setCurrentScrimAlpha(this.mHeadsUpScrim, alpha);
                updateScrimColor(this.mHeadsUpScrim);
            }
        }
    }

    public void setTopHeadsUpDragAmount(View draggedHeadsUpView, float topHeadsUpDragAmount) {
        this.mTopHeadsUpDragAmount = topHeadsUpDragAmount;
        this.mDraggedHeadsUpView = draggedHeadsUpView;
        updateHeadsUpScrim(false);
    }

    private float calculateHeadsUpAlpha() {
        float alpha;
        if (this.mPinnedHeadsUpCount >= 2) {
            alpha = 1.0f;
        } else if (this.mPinnedHeadsUpCount == 0) {
            alpha = 0.0f;
        } else {
            alpha = 1.0f - this.mTopHeadsUpDragAmount;
        }
        return alpha * Math.max(1.0f - this.mFraction, 0.0f);
    }

    public void forceHideScrims(boolean hide) {
        this.mForceHideScrims = hide;
        this.mAnimateChange = false;
        scheduleUpdate();
    }
}
