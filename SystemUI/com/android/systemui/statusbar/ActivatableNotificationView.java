package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewConfiguration;
import android.view.ViewPropertyAnimator;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;
import com.android.systemui.R;

public abstract class ActivatableNotificationView extends ExpandableOutlineView {
    private static final Interpolator ACTIVATE_INVERSE_ALPHA_INTERPOLATOR = new PathInterpolator(0.0f, 0.0f, 0.5f, 1.0f);
    private static final Interpolator ACTIVATE_INVERSE_INTERPOLATOR = new PathInterpolator(0.6f, 0.0f, 0.5f, 1.0f);
    private boolean mActivated;
    private float mAnimationTranslationY;
    private float mAppearAnimationFraction = -1.0f;
    private RectF mAppearAnimationRect = new RectF();
    private float mAppearAnimationTranslation;
    private ValueAnimator mAppearAnimator;
    private ObjectAnimator mBackgroundAnimator;
    private NotificationBackgroundView mBackgroundDimmed;
    private NotificationBackgroundView mBackgroundNormal;
    private int mBgTint = 0;
    private Interpolator mCurrentAlphaInterpolator;
    private Interpolator mCurrentAppearInterpolator;
    private boolean mDark;
    private boolean mDimmed;
    private float mDownX;
    private float mDownY;
    private boolean mDrawingAppearAnimation;
    protected final Interpolator mFastOutSlowInInterpolator;
    private boolean mIsBelowSpeedBump;
    private final int mLegacyColor;
    private final Interpolator mLinearInterpolator;
    private final Interpolator mLinearOutSlowInInterpolator;
    private final int mLowPriorityColor;
    private final int mLowPriorityRippleColor;
    private final int mNormalColor;
    protected final int mNormalRippleColor;
    private OnActivatedListener mOnActivatedListener;
    private boolean mShowingLegacyBackground;
    private final Interpolator mSlowOutFastInInterpolator;
    private final Interpolator mSlowOutLinearInInterpolator;
    private final Runnable mTapTimeoutRunnable = new Runnable() {
        public void run() {
            ActivatableNotificationView.this.makeInactive(true);
        }
    };
    private final int mTintedRippleColor;
    private final float mTouchSlop;

    public interface OnActivatedListener {
        void onActivated(ActivatableNotificationView activatableNotificationView);

        void onActivationReset(ActivatableNotificationView activatableNotificationView);
    }

    protected abstract View getContentView();

    public ActivatableNotificationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mTouchSlop = (float) ViewConfiguration.get(context).getScaledTouchSlop();
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, 17563661);
        this.mSlowOutFastInInterpolator = new PathInterpolator(0.8f, 0.0f, 0.6f, 1.0f);
        this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, 17563662);
        this.mSlowOutLinearInInterpolator = new PathInterpolator(0.8f, 0.0f, 1.0f, 1.0f);
        this.mLinearInterpolator = new LinearInterpolator();
        setClipChildren(false);
        setClipToPadding(false);
        this.mLegacyColor = context.getColor(R.color.notification_legacy_background_color);
        this.mNormalColor = context.getColor(R.color.notification_material_background_color);
        this.mLowPriorityColor = context.getColor(R.color.notification_material_background_low_priority_color);
        this.mTintedRippleColor = context.getColor(R.color.notification_ripple_tinted_color);
        this.mLowPriorityRippleColor = context.getColor(R.color.notification_ripple_color_low_priority);
        this.mNormalRippleColor = context.getColor(R.color.notification_ripple_untinted_color);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mBackgroundNormal = (NotificationBackgroundView) findViewById(R.id.backgroundNormal);
        this.mBackgroundDimmed = (NotificationBackgroundView) findViewById(R.id.backgroundDimmed);
        this.mBackgroundNormal.setCustomBackground((int) R.drawable.notification_material_bg);
        this.mBackgroundDimmed.setCustomBackground((int) R.drawable.notification_material_bg_dim);
        updateBackground();
        updateBackgroundTint();
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (this.mDimmed) {
            return handleTouchEventDimmed(event);
        }
        return super.onTouchEvent(event);
    }

    public void drawableHotspotChanged(float x, float y) {
        if (!this.mDimmed) {
            this.mBackgroundNormal.drawableHotspotChanged(x, y);
        }
    }

    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (this.mDimmed) {
            this.mBackgroundDimmed.setState(getDrawableState());
        } else {
            this.mBackgroundNormal.setState(getDrawableState());
        }
    }

    private boolean handleTouchEventDimmed(MotionEvent event) {
        switch (event.getActionMasked()) {
            case 0:
                this.mDownX = event.getX();
                this.mDownY = event.getY();
                if (this.mDownY > ((float) getActualHeight())) {
                    return false;
                }
                break;
            case 1:
                if (isWithinTouchSlop(event)) {
                    if (this.mActivated) {
                        if (performClick()) {
                            removeCallbacks(this.mTapTimeoutRunnable);
                            break;
                        }
                    }
                    makeActive();
                    postDelayed(this.mTapTimeoutRunnable, 1200);
                    break;
                }
                makeInactive(true);
                break;
                break;
            case 2:
                if (!isWithinTouchSlop(event)) {
                    makeInactive(true);
                    return false;
                }
                break;
            case 3:
                makeInactive(true);
                break;
        }
        return true;
    }

    private void makeActive() {
        startActivateAnimation(false);
        this.mActivated = true;
        if (this.mOnActivatedListener != null) {
            this.mOnActivatedListener.onActivated(this);
        }
    }

    private void startActivateAnimation(boolean reverse) {
        float f = 0.0f;
        if (isAttachedToWindow()) {
            Animator animator;
            Interpolator interpolator;
            Interpolator alphaInterpolator;
            int widthHalf = this.mBackgroundNormal.getWidth() / 2;
            int heightHalf = this.mBackgroundNormal.getActualHeight() / 2;
            float radius = (float) Math.sqrt((double) ((widthHalf * widthHalf) + (heightHalf * heightHalf)));
            if (reverse) {
                animator = ViewAnimationUtils.createCircularReveal(this.mBackgroundNormal, widthHalf, heightHalf, radius, 0.0f);
            } else {
                animator = ViewAnimationUtils.createCircularReveal(this.mBackgroundNormal, widthHalf, heightHalf, 0.0f, radius);
            }
            this.mBackgroundNormal.setVisibility(0);
            if (reverse) {
                interpolator = ACTIVATE_INVERSE_INTERPOLATOR;
                alphaInterpolator = ACTIVATE_INVERSE_ALPHA_INTERPOLATOR;
            } else {
                interpolator = this.mLinearOutSlowInInterpolator;
                alphaInterpolator = this.mLinearOutSlowInInterpolator;
            }
            animator.setInterpolator(interpolator);
            animator.setDuration(220);
            if (reverse) {
                this.mBackgroundNormal.setAlpha(1.0f);
                animator.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        if (ActivatableNotificationView.this.mDimmed) {
                            ActivatableNotificationView.this.mBackgroundNormal.setVisibility(4);
                        }
                    }
                });
                animator.start();
            } else {
                this.mBackgroundNormal.setAlpha(0.4f);
                animator.start();
            }
            ViewPropertyAnimator animate = this.mBackgroundNormal.animate();
            if (!reverse) {
                f = 1.0f;
            }
            animate.alpha(f).setInterpolator(alphaInterpolator).setDuration(220);
        }
    }

    public void makeInactive(boolean animate) {
        if (this.mActivated) {
            if (this.mDimmed) {
                if (animate) {
                    startActivateAnimation(true);
                } else {
                    this.mBackgroundNormal.setVisibility(4);
                }
            }
            this.mActivated = false;
        }
        if (this.mOnActivatedListener != null) {
            this.mOnActivatedListener.onActivationReset(this);
        }
        removeCallbacks(this.mTapTimeoutRunnable);
    }

    private boolean isWithinTouchSlop(MotionEvent event) {
        if (Math.abs(event.getX() - this.mDownX) >= this.mTouchSlop || Math.abs(event.getY() - this.mDownY) >= this.mTouchSlop) {
            return false;
        }
        return true;
    }

    public void setDimmed(boolean dimmed, boolean fade) {
        if (this.mDimmed != dimmed) {
            this.mDimmed = dimmed;
            if (fade) {
                fadeDimmedBackground();
            } else {
                updateBackground();
            }
        }
    }

    public void setDark(boolean dark, boolean fade, long delay) {
        super.setDark(dark, fade, delay);
        if (this.mDark != dark) {
            float f;
            this.mDark = dark;
            if (dark || !fade) {
                updateBackground();
            } else {
                if (this.mActivated) {
                    this.mBackgroundDimmed.setVisibility(0);
                    this.mBackgroundNormal.setVisibility(0);
                } else if (this.mDimmed) {
                    this.mBackgroundDimmed.setVisibility(0);
                    this.mBackgroundNormal.setVisibility(4);
                } else {
                    this.mBackgroundDimmed.setVisibility(4);
                    this.mBackgroundNormal.setVisibility(0);
                }
                fadeInFromDark(delay);
            }
            if (dark) {
                f = 0.0f;
            } else {
                f = 1.0f;
            }
            setOutlineAlpha(f);
        }
    }

    public void setShowingLegacyBackground(boolean showing) {
        this.mShowingLegacyBackground = showing;
        updateBackgroundTint();
    }

    public void setBelowSpeedBump(boolean below) {
        super.setBelowSpeedBump(below);
        if (below != this.mIsBelowSpeedBump) {
            this.mIsBelowSpeedBump = below;
            updateBackgroundTint();
        }
    }

    public void setTintColor(int color) {
        this.mBgTint = color;
        updateBackgroundTint();
    }

    private void updateBackgroundTint() {
        int color = getBgColor();
        int rippleColor = getRippleColor();
        if (color == this.mNormalColor) {
            color = 0;
        }
        this.mBackgroundDimmed.setTint(color);
        this.mBackgroundNormal.setTint(color);
        this.mBackgroundDimmed.setRippleColor(rippleColor);
        this.mBackgroundNormal.setRippleColor(rippleColor);
    }

    private void fadeInFromDark(long delay) {
        final View background = this.mDimmed ? this.mBackgroundDimmed : this.mBackgroundNormal;
        background.setAlpha(0.0f);
        background.setPivotX(((float) this.mBackgroundDimmed.getWidth()) / 2.0f);
        background.setPivotY(((float) getActualHeight()) / 2.0f);
        background.setScaleX(0.93f);
        background.setScaleY(0.93f);
        background.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(170).setStartDelay(delay).setInterpolator(this.mLinearOutSlowInInterpolator).setListener(new AnimatorListenerAdapter() {
            public void onAnimationCancel(Animator animation) {
                background.setScaleX(1.0f);
                background.setScaleY(1.0f);
                background.setAlpha(1.0f);
            }
        }).start();
    }

    private void fadeDimmedBackground() {
        this.mBackgroundDimmed.animate().cancel();
        this.mBackgroundNormal.animate().cancel();
        if (this.mDimmed) {
            this.mBackgroundDimmed.setVisibility(0);
        } else {
            this.mBackgroundNormal.setVisibility(0);
        }
        float startAlpha = this.mDimmed ? 1.0f : 0.0f;
        float endAlpha = this.mDimmed ? 0.0f : 1.0f;
        int duration = 220;
        if (this.mBackgroundAnimator != null) {
            startAlpha = ((Float) this.mBackgroundAnimator.getAnimatedValue()).floatValue();
            duration = (int) this.mBackgroundAnimator.getCurrentPlayTime();
            this.mBackgroundAnimator.removeAllListeners();
            this.mBackgroundAnimator.cancel();
            if (duration <= 0) {
                updateBackground();
                return;
            }
        }
        this.mBackgroundNormal.setAlpha(startAlpha);
        this.mBackgroundAnimator = ObjectAnimator.ofFloat(this.mBackgroundNormal, View.ALPHA, new float[]{startAlpha, endAlpha});
        this.mBackgroundAnimator.setInterpolator(this.mFastOutSlowInInterpolator);
        this.mBackgroundAnimator.setDuration((long) duration);
        this.mBackgroundAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                if (ActivatableNotificationView.this.mDimmed) {
                    ActivatableNotificationView.this.mBackgroundNormal.setVisibility(4);
                } else {
                    ActivatableNotificationView.this.mBackgroundDimmed.setVisibility(4);
                }
                ActivatableNotificationView.this.mBackgroundAnimator = null;
            }
        });
        this.mBackgroundAnimator.start();
    }

    private void updateBackground() {
        cancelFadeAnimations();
        if (this.mDark) {
            this.mBackgroundDimmed.setVisibility(4);
            this.mBackgroundNormal.setVisibility(4);
        } else if (this.mDimmed) {
            this.mBackgroundDimmed.setVisibility(0);
            this.mBackgroundNormal.setVisibility(4);
        } else {
            this.mBackgroundDimmed.setVisibility(4);
            this.mBackgroundNormal.setVisibility(0);
            this.mBackgroundNormal.setAlpha(1.0f);
            removeCallbacks(this.mTapTimeoutRunnable);
        }
    }

    private void cancelFadeAnimations() {
        if (this.mBackgroundAnimator != null) {
            this.mBackgroundAnimator.cancel();
        }
        this.mBackgroundDimmed.animate().cancel();
        this.mBackgroundNormal.animate().cancel();
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        setPivotX((float) (getWidth() / 2));
    }

    public void setActualHeight(int actualHeight, boolean notifyListeners) {
        super.setActualHeight(actualHeight, notifyListeners);
        setPivotY((float) (actualHeight / 2));
        this.mBackgroundNormal.setActualHeight(actualHeight);
        this.mBackgroundDimmed.setActualHeight(actualHeight);
    }

    public void setClipTopAmount(int clipTopAmount) {
        super.setClipTopAmount(clipTopAmount);
        this.mBackgroundNormal.setClipTopAmount(clipTopAmount);
        this.mBackgroundDimmed.setClipTopAmount(clipTopAmount);
    }

    public void performRemoveAnimation(long duration, float translationDirection, Runnable onFinishedRunnable) {
        enableAppearDrawing(true);
        if (this.mDrawingAppearAnimation) {
            startAppearAnimation(false, translationDirection, 0, duration, onFinishedRunnable);
        } else if (onFinishedRunnable != null) {
            onFinishedRunnable.run();
        }
    }

    public void performAddAnimation(long delay, long duration) {
        enableAppearDrawing(true);
        if (this.mDrawingAppearAnimation) {
            startAppearAnimation(true, -1.0f, delay, duration, null);
        }
    }

    private void startAppearAnimation(boolean isAppearing, float translationDirection, long delay, long duration, final Runnable onFinishedRunnable) {
        float targetValue;
        cancelAppearAnimation();
        this.mAnimationTranslationY = ((float) getActualHeight()) * translationDirection;
        if (this.mAppearAnimationFraction == -1.0f) {
            if (isAppearing) {
                this.mAppearAnimationFraction = 0.0f;
                this.mAppearAnimationTranslation = this.mAnimationTranslationY;
            } else {
                this.mAppearAnimationFraction = 1.0f;
                this.mAppearAnimationTranslation = 0.0f;
            }
        }
        if (isAppearing) {
            this.mCurrentAppearInterpolator = this.mSlowOutFastInInterpolator;
            this.mCurrentAlphaInterpolator = this.mLinearOutSlowInInterpolator;
            targetValue = 1.0f;
        } else {
            this.mCurrentAppearInterpolator = this.mFastOutSlowInInterpolator;
            this.mCurrentAlphaInterpolator = this.mSlowOutLinearInInterpolator;
            targetValue = 0.0f;
        }
        this.mAppearAnimator = ValueAnimator.ofFloat(new float[]{this.mAppearAnimationFraction, targetValue});
        this.mAppearAnimator.setInterpolator(this.mLinearInterpolator);
        this.mAppearAnimator.setDuration((long) (((float) duration) * Math.abs(this.mAppearAnimationFraction - targetValue)));
        this.mAppearAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                ActivatableNotificationView.this.mAppearAnimationFraction = ((Float) animation.getAnimatedValue()).floatValue();
                ActivatableNotificationView.this.updateAppearAnimationAlpha();
                ActivatableNotificationView.this.updateAppearRect();
                ActivatableNotificationView.this.invalidate();
            }
        });
        if (delay > 0) {
            updateAppearAnimationAlpha();
            updateAppearRect();
            this.mAppearAnimator.setStartDelay(delay);
        }
        this.mAppearAnimator.addListener(new AnimatorListenerAdapter() {
            private boolean mWasCancelled;

            public void onAnimationEnd(Animator animation) {
                if (onFinishedRunnable != null) {
                    onFinishedRunnable.run();
                }
                if (!this.mWasCancelled) {
                    ActivatableNotificationView.this.mAppearAnimationFraction = -1.0f;
                    ActivatableNotificationView.this.setOutlineRect(null);
                    ActivatableNotificationView.this.enableAppearDrawing(false);
                }
            }

            public void onAnimationStart(Animator animation) {
                this.mWasCancelled = false;
            }

            public void onAnimationCancel(Animator animation) {
                this.mWasCancelled = true;
            }
        });
        this.mAppearAnimator.start();
    }

    private void cancelAppearAnimation() {
        if (this.mAppearAnimator != null) {
            this.mAppearAnimator.cancel();
        }
    }

    public void cancelAppearDrawing() {
        cancelAppearAnimation();
        enableAppearDrawing(false);
    }

    private void updateAppearRect() {
        float bottom;
        float top;
        float inverseFraction = 1.0f - this.mAppearAnimationFraction;
        float translateYTotalAmount = this.mCurrentAppearInterpolator.getInterpolation(inverseFraction) * this.mAnimationTranslationY;
        this.mAppearAnimationTranslation = translateYTotalAmount;
        float left = (((float) getWidth()) * 0.475f) * this.mCurrentAppearInterpolator.getInterpolation(Math.min(1.0f, Math.max(0.0f, (inverseFraction - 0.0f) / 0.8f)));
        float right = ((float) getWidth()) - left;
        float heightFraction = this.mCurrentAppearInterpolator.getInterpolation(Math.max(0.0f, (inverseFraction - 0.0f) / 1.0f));
        int actualHeight = getActualHeight();
        if (this.mAnimationTranslationY > 0.0f) {
            bottom = (((float) actualHeight) - ((this.mAnimationTranslationY * heightFraction) * 0.1f)) - translateYTotalAmount;
            top = bottom * heightFraction;
        } else {
            top = (((((float) actualHeight) + this.mAnimationTranslationY) * heightFraction) * 0.1f) - translateYTotalAmount;
            bottom = (((float) actualHeight) * (1.0f - heightFraction)) + (top * heightFraction);
        }
        this.mAppearAnimationRect.set(left, top, right, bottom);
        setOutlineRect(left, this.mAppearAnimationTranslation + top, right, this.mAppearAnimationTranslation + bottom);
    }

    private void updateAppearAnimationAlpha() {
        setContentAlpha(this.mCurrentAlphaInterpolator.getInterpolation(Math.min(1.0f, this.mAppearAnimationFraction / 1.0f)));
    }

    private void setContentAlpha(float contentAlpha) {
        View contentView = getContentView();
        if (contentView.hasOverlappingRendering()) {
            int layerType;
            if (contentAlpha == 0.0f || contentAlpha == 1.0f) {
                layerType = 0;
            } else {
                layerType = 2;
            }
            if (contentView.getLayerType() != layerType) {
                contentView.setLayerType(layerType, null);
            }
        }
        contentView.setAlpha(contentAlpha);
    }

    private int getBgColor() {
        if (this.mBgTint != 0) {
            return this.mBgTint;
        }
        if (this.mShowingLegacyBackground) {
            return this.mLegacyColor;
        }
        if (this.mIsBelowSpeedBump) {
            return this.mLowPriorityColor;
        }
        return this.mNormalColor;
    }

    protected int getRippleColor() {
        if (this.mBgTint != 0) {
            return this.mTintedRippleColor;
        }
        if (this.mShowingLegacyBackground) {
            return this.mTintedRippleColor;
        }
        if (this.mIsBelowSpeedBump) {
            return this.mLowPriorityRippleColor;
        }
        return this.mNormalRippleColor;
    }

    private void enableAppearDrawing(boolean enable) {
        if (enable != this.mDrawingAppearAnimation) {
            this.mDrawingAppearAnimation = enable;
            if (!enable) {
                setContentAlpha(1.0f);
            }
            invalidate();
        }
    }

    protected void dispatchDraw(Canvas canvas) {
        if (this.mDrawingAppearAnimation) {
            canvas.save();
            canvas.translate(0.0f, this.mAppearAnimationTranslation);
        }
        super.dispatchDraw(canvas);
        if (this.mDrawingAppearAnimation) {
            canvas.restore();
        }
    }

    public void setOnActivatedListener(OnActivatedListener onActivatedListener) {
        this.mOnActivatedListener = onActivatedListener;
    }

    public void reset() {
        setTintColor(0);
        setShowingLegacyBackground(false);
        setBelowSpeedBump(false);
    }
}
