package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.android.systemui.R;
import com.android.systemui.assis.app.MAIN.EVENT;
import com.android.systemui.statusbar.FlingAnimationUtils;
import com.android.systemui.statusbar.KeyguardAffordanceView;

public class KeyguardAffordanceHelper {
    private Runnable mAnimationEndRunnable = new Runnable() {
        public void run() {
            KeyguardAffordanceHelper.this.mCallback.onAnimationToSideEnded();
        }
    };
    private Interpolator mAppearInterpolator;
    private Callback mCallback;
    private KeyguardAffordanceView mCenterIcon;
    private final Context mContext;
    private Interpolator mDisappearInterpolator;
    private FlingAnimationUtils mFlingAnimationUtils;
    private AnimatorListenerAdapter mFlingEndListener = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animation) {
            KeyguardAffordanceHelper.this.mSwipeAnimator = null;
            KeyguardAffordanceHelper.this.mSwipingInProgress = false;
            KeyguardAffordanceHelper.this.mTargetedView = null;
        }
    };
    private int mHintGrowAmount;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private KeyguardAffordanceView mLeftIcon;
    private int mMinBackgroundRadius;
    private int mMinFlingVelocity;
    private int mMinTranslationAmount;
    private boolean mMotionCancelled;
    private KeyguardAffordanceView mRightIcon;
    private Animator mSwipeAnimator;
    private boolean mSwipingInProgress;
    private View mTargetedView;
    private int mTouchSlop;
    private boolean mTouchSlopExeeded;
    private int mTouchTargetSize;
    private float mTranslation;
    private float mTranslationOnDown;
    private VelocityTracker mVelocityTracker;

    public interface Callback {
        float getAffordanceFalsingFactor();

        KeyguardAffordanceView getCenterIcon();

        KeyguardAffordanceView getLeftIcon();

        View getLeftPreview();

        float getMaxTranslationDistance();

        KeyguardAffordanceView getRightIcon();

        View getRightPreview();

        void onAnimationToSideEnded();

        void onAnimationToSideStarted(boolean z, float f, float f2);

        void onIconClicked(boolean z);

        void onSwipingAborted();

        void onSwipingStarted(boolean z);
    }

    KeyguardAffordanceHelper(Callback callback, Context context) {
        this.mContext = context;
        this.mCallback = callback;
        initIcons();
        updateIcon(this.mLeftIcon, 0.0f, this.mLeftIcon.getRestingAlpha(), false, false, true);
        updateIcon(this.mCenterIcon, 0.0f, this.mCenterIcon.getRestingAlpha(), false, false, true);
        updateIcon(this.mRightIcon, 0.0f, this.mRightIcon.getRestingAlpha(), false, false, true);
        initDimens();
    }

    private void initDimens() {
        ViewConfiguration configuration = ViewConfiguration.get(this.mContext);
        this.mTouchSlop = configuration.getScaledPagingTouchSlop();
        this.mMinFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        this.mMinTranslationAmount = this.mContext.getResources().getDimensionPixelSize(R.dimen.keyguard_min_swipe_amount);
        this.mMinBackgroundRadius = this.mContext.getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_min_background_radius);
        this.mTouchTargetSize = this.mContext.getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_touch_target_size);
        this.mHintGrowAmount = this.mContext.getResources().getDimensionPixelSize(R.dimen.hint_grow_amount_sideways);
        this.mFlingAnimationUtils = new FlingAnimationUtils(this.mContext, 0.4f);
        this.mAppearInterpolator = AnimationUtils.loadInterpolator(this.mContext, 17563662);
        this.mDisappearInterpolator = AnimationUtils.loadInterpolator(this.mContext, 17563663);
    }

    private void initIcons() {
        this.mLeftIcon = this.mCallback.getLeftIcon();
        this.mCenterIcon = this.mCallback.getCenterIcon();
        this.mRightIcon = this.mCallback.getRightIcon();
        updatePreviews();
    }

    public void updatePreviews() {
        this.mLeftIcon.setPreviewView(this.mCallback.getLeftPreview());
        this.mRightIcon.setPreviewView(this.mCallback.getRightPreview());
    }

    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (this.mMotionCancelled && action != 0) {
            return false;
        }
        float y = event.getY();
        float x = event.getX();
        boolean isUp = false;
        switch (action) {
            case 0:
                View targetView = getIconAtPosition(x, y);
                if (targetView != null && (this.mTargetedView == null || this.mTargetedView == targetView)) {
                    if (this.mTargetedView != null) {
                        cancelAnimation();
                    } else {
                        this.mTouchSlopExeeded = false;
                    }
                    this.mCallback.onSwipingStarted(targetView == this.mRightIcon);
                    this.mSwipingInProgress = true;
                    this.mTargetedView = targetView;
                    this.mInitialTouchX = x;
                    this.mInitialTouchY = y;
                    this.mTranslationOnDown = this.mTranslation;
                    initVelocityTracker();
                    trackMovement(event);
                    this.mMotionCancelled = false;
                    break;
                }
                this.mMotionCancelled = true;
                return false;
            case 1:
                isUp = true;
                break;
            case 2:
                trackMovement(event);
                float distance = (float) Math.hypot((double) (x - this.mInitialTouchX), (double) (y - this.mInitialTouchY));
                if (!this.mTouchSlopExeeded && distance > ((float) this.mTouchSlop)) {
                    this.mTouchSlopExeeded = true;
                }
                if (this.mSwipingInProgress) {
                    if (this.mTargetedView == this.mRightIcon) {
                        distance = Math.min(0.0f, this.mTranslationOnDown - distance);
                    } else {
                        distance = Math.max(0.0f, distance + this.mTranslationOnDown);
                    }
                    setTranslation(distance, false, false);
                    break;
                }
                break;
            case 3:
                break;
            case 5:
                this.mMotionCancelled = true;
                endMotion(true, x, y);
                break;
        }
        boolean hintOnTheRight = this.mTargetedView == this.mRightIcon;
        trackMovement(event);
        endMotion(!isUp, x, y);
        if (!this.mTouchSlopExeeded && isUp) {
            this.mCallback.onIconClicked(hintOnTheRight);
        }
        return true;
    }

    private View getIconAtPosition(float x, float y) {
        if (leftSwipePossible() && isOnIcon(this.mLeftIcon, x, y)) {
            return this.mLeftIcon;
        }
        if (rightSwipePossible() && isOnIcon(this.mRightIcon, x, y)) {
            return this.mRightIcon;
        }
        return null;
    }

    public boolean isOnAffordanceIcon(float x, float y) {
        return !isOnIcon(this.mLeftIcon, x, y) ? isOnIcon(this.mRightIcon, x, y) : true;
    }

    private boolean isOnIcon(View icon, float x, float y) {
        return Math.hypot((double) (x - (icon.getX() + (((float) icon.getWidth()) / 2.0f))), (double) (y - (icon.getY() + (((float) icon.getHeight()) / 2.0f)))) <= ((double) (this.mTouchTargetSize / 2));
    }

    private void endMotion(boolean forceSnapBack, float lastX, float lastY) {
        if (this.mSwipingInProgress) {
            flingWithCurrentVelocity(forceSnapBack, lastX, lastY);
        } else {
            this.mTargetedView = null;
        }
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    private boolean rightSwipePossible() {
        return this.mRightIcon.getVisibility() == 0;
    }

    private boolean leftSwipePossible() {
        return this.mLeftIcon.getVisibility() == 0;
    }

    public void startHintAnimation(boolean right, Runnable onFinishedListener) {
        cancelAnimation();
        startHintAnimationPhase1(right, onFinishedListener);
    }

    private void startHintAnimationPhase1(final boolean right, final Runnable onFinishedListener) {
        KeyguardAffordanceView targetView = right ? this.mRightIcon : this.mLeftIcon;
        ValueAnimator animator = getAnimatorToRadius(right, this.mHintGrowAmount);
        animator.addListener(new AnimatorListenerAdapter() {
            private boolean mCancelled;

            public void onAnimationCancel(Animator animation) {
                this.mCancelled = true;
            }

            public void onAnimationEnd(Animator animation) {
                if (this.mCancelled) {
                    KeyguardAffordanceHelper.this.mSwipeAnimator = null;
                    KeyguardAffordanceHelper.this.mTargetedView = null;
                    onFinishedListener.run();
                    return;
                }
                KeyguardAffordanceHelper.this.startUnlockHintAnimationPhase2(right, onFinishedListener);
            }
        });
        animator.setInterpolator(this.mAppearInterpolator);
        animator.setDuration(200);
        animator.start();
        this.mSwipeAnimator = animator;
        this.mTargetedView = targetView;
    }

    private void startUnlockHintAnimationPhase2(boolean right, final Runnable onFinishedListener) {
        ValueAnimator animator = getAnimatorToRadius(right, 0);
        animator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                KeyguardAffordanceHelper.this.mSwipeAnimator = null;
                KeyguardAffordanceHelper.this.mTargetedView = null;
                onFinishedListener.run();
            }
        });
        animator.setInterpolator(this.mDisappearInterpolator);
        animator.setDuration(350);
        animator.setStartDelay(500);
        animator.start();
        this.mSwipeAnimator = animator;
    }

    private ValueAnimator getAnimatorToRadius(final boolean right, int radius) {
        final KeyguardAffordanceView targetView = right ? this.mRightIcon : this.mLeftIcon;
        ValueAnimator animator = ValueAnimator.ofFloat(new float[]{targetView.getCircleRadius(), (float) radius});
        animator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float newRadius = ((Float) animation.getAnimatedValue()).floatValue();
                targetView.setCircleRadiusWithoutAnimation(newRadius);
                float translation = KeyguardAffordanceHelper.this.getTranslationFromRadius(newRadius);
                KeyguardAffordanceHelper keyguardAffordanceHelper = KeyguardAffordanceHelper.this;
                if (right) {
                    translation = -translation;
                }
                keyguardAffordanceHelper.mTranslation = translation;
                KeyguardAffordanceHelper.this.updateIconsFromTranslation(targetView);
            }
        });
        return animator;
    }

    private void cancelAnimation() {
        if (this.mSwipeAnimator != null) {
            this.mSwipeAnimator.cancel();
        }
    }

    private void flingWithCurrentVelocity(boolean forceSnapBack, float lastX, float lastY) {
        float vel = getCurrentVelocity(lastX, lastY);
        boolean snapBack = isBelowFalsingThreshold();
        boolean velIsInWrongDirection = this.mTranslation * vel < 0.0f;
        snapBack |= Math.abs(vel) > ((float) this.mMinFlingVelocity) ? velIsInWrongDirection : 0;
        if ((snapBack ^ velIsInWrongDirection) != 0) {
            vel = 0.0f;
        }
        if (snapBack) {
            forceSnapBack = true;
        }
        fling(vel, forceSnapBack);
    }

    private boolean isBelowFalsingThreshold() {
        return Math.abs(this.mTranslation) < Math.abs(this.mTranslationOnDown) + ((float) getMinTranslationAmount());
    }

    private int getMinTranslationAmount() {
        return (int) (((float) this.mMinTranslationAmount) * this.mCallback.getAffordanceFalsingFactor());
    }

    private void fling(float vel, boolean snapBack) {
        float target;
        boolean z = true;
        if (this.mTranslation < 0.0f) {
            target = -this.mCallback.getMaxTranslationDistance();
        } else {
            target = this.mCallback.getMaxTranslationDistance();
        }
        if (snapBack) {
            target = 0.0f;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(new float[]{this.mTranslation, target});
        this.mFlingAnimationUtils.apply(animator, this.mTranslation, target, vel);
        animator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                KeyguardAffordanceHelper.this.mTranslation = ((Float) animation.getAnimatedValue()).floatValue();
            }
        });
        animator.addListener(this.mFlingEndListener);
        if (snapBack) {
            reset(true);
        } else {
            startFinishingCircleAnimation(0.375f * vel, this.mAnimationEndRunnable);
            Callback callback = this.mCallback;
            if (this.mTranslation >= 0.0f) {
                z = false;
            }
            callback.onAnimationToSideStarted(z, this.mTranslation, vel);
        }
        animator.start();
        this.mSwipeAnimator = animator;
        if (snapBack) {
            this.mCallback.onSwipingAborted();
        }
    }

    private void startFinishingCircleAnimation(float velocity, Runnable mAnimationEndRunnable) {
        (this.mTranslation > 0.0f ? this.mLeftIcon : this.mRightIcon).finishAnimation(velocity, mAnimationEndRunnable);
    }

    private void setTranslation(float translation, boolean isReset, boolean animateReset) {
        if (!rightSwipePossible()) {
            translation = Math.max(0.0f, translation);
        }
        if (!leftSwipePossible()) {
            translation = Math.min(0.0f, translation);
        }
        float absTranslation = Math.abs(translation);
        if (translation != this.mTranslation || isReset) {
            KeyguardAffordanceView targetView = translation > 0.0f ? this.mLeftIcon : this.mRightIcon;
            KeyguardAffordanceView otherView = translation > 0.0f ? this.mRightIcon : this.mLeftIcon;
            float alpha = absTranslation / ((float) getMinTranslationAmount());
            float fadeOutAlpha = Math.max(1.0f - alpha, 0.0f);
            boolean z = isReset ? animateReset : false;
            float radius = getRadiusFromTranslation(absTranslation);
            boolean isBelowFalsingThreshold = isReset ? isBelowFalsingThreshold() : false;
            if (isReset) {
                updateIcon(targetView, 0.0f, fadeOutAlpha * targetView.getRestingAlpha(), z, isBelowFalsingThreshold, false);
            } else {
                updateIcon(targetView, radius, alpha + (targetView.getRestingAlpha() * fadeOutAlpha), false, false, false);
            }
            updateIcon(otherView, 0.0f, fadeOutAlpha * otherView.getRestingAlpha(), z, isBelowFalsingThreshold, false);
            updateIcon(this.mCenterIcon, 0.0f, fadeOutAlpha * this.mCenterIcon.getRestingAlpha(), z, isBelowFalsingThreshold, false);
            this.mTranslation = translation;
        }
    }

    private void updateIconsFromTranslation(KeyguardAffordanceView targetView) {
        float alpha = Math.abs(this.mTranslation) / ((float) getMinTranslationAmount());
        float fadeOutAlpha = Math.max(0.0f, 1.0f - alpha);
        KeyguardAffordanceView otherView = targetView == this.mRightIcon ? this.mLeftIcon : this.mRightIcon;
        updateIconAlpha(targetView, (targetView.getRestingAlpha() * fadeOutAlpha) + alpha, false);
        updateIconAlpha(otherView, otherView.getRestingAlpha() * fadeOutAlpha, false);
        updateIconAlpha(this.mCenterIcon, this.mCenterIcon.getRestingAlpha() * fadeOutAlpha, false);
    }

    private float getTranslationFromRadius(float circleSize) {
        float translation = (circleSize - ((float) this.mMinBackgroundRadius)) / 0.25f;
        if (translation > 0.0f) {
            return ((float) this.mTouchSlop) + translation;
        }
        return 0.0f;
    }

    private float getRadiusFromTranslation(float translation) {
        if (translation <= ((float) this.mTouchSlop)) {
            return 0.0f;
        }
        return ((translation - ((float) this.mTouchSlop)) * 0.25f) + ((float) this.mMinBackgroundRadius);
    }

    public void animateHideLeftRightIcon() {
        cancelAnimation();
        updateIcon(this.mRightIcon, 0.0f, 0.0f, true, false, false);
        updateIcon(this.mLeftIcon, 0.0f, 0.0f, true, false, false);
    }

    private void updateIcon(KeyguardAffordanceView view, float circleRadius, float alpha, boolean animate, boolean slowRadiusAnimation, boolean force) {
        if (view.getVisibility() == 0 || force) {
            view.setCircleRadius(circleRadius, slowRadiusAnimation);
            updateIconAlpha(view, alpha, animate);
        }
    }

    private void updateIconAlpha(KeyguardAffordanceView view, float alpha, boolean animate) {
        float scale = getScale(alpha, view);
        view.setImageAlpha(Math.min(1.0f, alpha), animate);
        view.setImageScale(scale, animate);
    }

    private float getScale(float alpha, KeyguardAffordanceView icon) {
        return Math.min(((alpha / icon.getRestingAlpha()) * 0.2f) + 0.8f, 1.5f);
    }

    private void trackMovement(MotionEvent event) {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.addMovement(event);
        }
    }

    private void initVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
        }
        this.mVelocityTracker = VelocityTracker.obtain();
    }

    private float getCurrentVelocity(float lastX, float lastY) {
        if (this.mVelocityTracker == null) {
            return 0.0f;
        }
        this.mVelocityTracker.computeCurrentVelocity(EVENT.DYNAMIC_PACK_EVENT_BASE);
        float bX = lastX - this.mInitialTouchX;
        float bY = lastY - this.mInitialTouchY;
        float bLen = (float) Math.hypot((double) bX, (double) bY);
        float projectedVelocity = ((this.mVelocityTracker.getXVelocity() * bX) + (this.mVelocityTracker.getYVelocity() * bY)) / bLen;
        if (this.mTargetedView == this.mRightIcon) {
            projectedVelocity = -projectedVelocity;
        }
        return projectedVelocity;
    }

    public void onConfigurationChanged() {
        initDimens();
        initIcons();
    }

    public void onRtlPropertiesChanged() {
        initIcons();
    }

    public void reset(boolean animate) {
        cancelAnimation();
        setTranslation(0.0f, true, animate);
        this.mMotionCancelled = true;
        if (this.mSwipingInProgress) {
            this.mCallback.onSwipingAborted();
        }
        this.mSwipingInProgress = false;
    }
}
