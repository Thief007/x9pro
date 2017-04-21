package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.CanvasProperty;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.DisplayListCanvas;
import android.view.RenderNodeAnimator;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.PhoneStatusBar;

public class KeyguardAffordanceView extends ImageView {
    private ValueAnimator mAlphaAnimator;
    private AnimatorListenerAdapter mAlphaEndListener;
    private final Interpolator mAppearInterpolator;
    private int mCenterX;
    private int mCenterY;
    private ValueAnimator mCircleAnimator;
    private int mCircleColor;
    private AnimatorListenerAdapter mCircleEndListener;
    private final Paint mCirclePaint;
    private float mCircleRadius;
    private float mCircleStartRadius;
    private float mCircleStartValue;
    private boolean mCircleWillBeHidden;
    private AnimatorListenerAdapter mClipEndListener;
    private final ArgbEvaluator mColorInterpolator;
    private final Interpolator mDisappearInterpolator;
    private boolean mFinishing;
    private final FlingAnimationUtils mFlingAnimationUtils;
    private CanvasProperty<Float> mHwCenterX;
    private CanvasProperty<Float> mHwCenterY;
    private CanvasProperty<Paint> mHwCirclePaint;
    private CanvasProperty<Float> mHwCircleRadius;
    private float mImageScale;
    private final int mInverseColor;
    private float mMaxCircleSize;
    private final int mMinBackgroundRadius;
    private final int mNormalColor;
    private Animator mPreviewClipper;
    private View mPreviewView;
    private float mRestingAlpha;
    private ValueAnimator mScaleAnimator;
    private AnimatorListenerAdapter mScaleEndListener;
    private boolean mSupportHardware;
    private int[] mTempPoint;

    public KeyguardAffordanceView(Context context) {
        this(context, null);
    }

    public KeyguardAffordanceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyguardAffordanceView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public KeyguardAffordanceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mTempPoint = new int[2];
        this.mImageScale = 1.0f;
        this.mRestingAlpha = 0.5f;
        this.mClipEndListener = new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                KeyguardAffordanceView.this.mPreviewClipper = null;
            }
        };
        this.mCircleEndListener = new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                KeyguardAffordanceView.this.mCircleAnimator = null;
            }
        };
        this.mScaleEndListener = new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                KeyguardAffordanceView.this.mScaleAnimator = null;
            }
        };
        this.mAlphaEndListener = new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                KeyguardAffordanceView.this.mAlphaAnimator = null;
            }
        };
        this.mCirclePaint = new Paint();
        this.mCirclePaint.setAntiAlias(true);
        this.mCircleColor = -1;
        this.mCirclePaint.setColor(this.mCircleColor);
        this.mNormalColor = -1;
        this.mInverseColor = -16777216;
        this.mMinBackgroundRadius = this.mContext.getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_min_background_radius);
        this.mAppearInterpolator = AnimationUtils.loadInterpolator(this.mContext, 17563662);
        this.mDisappearInterpolator = AnimationUtils.loadInterpolator(this.mContext, 17563663);
        this.mColorInterpolator = new ArgbEvaluator();
        this.mFlingAnimationUtils = new FlingAnimationUtils(this.mContext, 0.3f);
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        this.mCenterX = getWidth() / 2;
        this.mCenterY = getHeight() / 2;
        this.mMaxCircleSize = getMaxCircleSize();
    }

    protected void onDraw(Canvas canvas) {
        this.mSupportHardware = canvas.isHardwareAccelerated();
        drawBackgroundCircle(canvas);
        canvas.save();
        canvas.scale(this.mImageScale, this.mImageScale, (float) (getWidth() / 2), (float) (getHeight() / 2));
        super.onDraw(canvas);
        canvas.restore();
    }

    public void setPreviewView(View v) {
        this.mPreviewView = v;
        if (this.mPreviewView != null) {
            this.mPreviewView.setVisibility(4);
        }
    }

    private void updateIconColor() {
        getDrawable().mutate().setColorFilter(((Integer) this.mColorInterpolator.evaluate(Math.min(1.0f, this.mCircleRadius / ((float) this.mMinBackgroundRadius)), Integer.valueOf(this.mNormalColor), Integer.valueOf(this.mInverseColor))).intValue(), Mode.SRC_ATOP);
    }

    private void drawBackgroundCircle(Canvas canvas) {
        if (this.mCircleRadius <= 0.0f) {
            return;
        }
        if (this.mFinishing && this.mSupportHardware) {
            ((DisplayListCanvas) canvas).drawCircle(this.mHwCenterX, this.mHwCenterY, this.mHwCircleRadius, this.mHwCirclePaint);
            return;
        }
        updateCircleColor();
        canvas.drawCircle((float) this.mCenterX, (float) this.mCenterY, this.mCircleRadius, this.mCirclePaint);
    }

    private void updateCircleColor() {
        float fraction = 0.5f + (Math.max(0.0f, Math.min(1.0f, (this.mCircleRadius - ((float) this.mMinBackgroundRadius)) / (((float) this.mMinBackgroundRadius) * 0.5f))) * 0.5f);
        if (this.mPreviewView != null && this.mPreviewView.getVisibility() == 0) {
            fraction *= 1.0f - (Math.max(0.0f, this.mCircleRadius - this.mCircleStartRadius) / (this.mMaxCircleSize - this.mCircleStartRadius));
        }
        this.mCirclePaint.setColor(Color.argb((int) (((float) Color.alpha(this.mCircleColor)) * fraction), Color.red(this.mCircleColor), Color.green(this.mCircleColor), Color.blue(this.mCircleColor)));
    }

    public void finishAnimation(float velocity, final Runnable mAnimationEndRunnable) {
        Animator animatorToRadius;
        cancelAnimator(this.mCircleAnimator);
        cancelAnimator(this.mPreviewClipper);
        this.mFinishing = true;
        this.mCircleStartRadius = this.mCircleRadius;
        float maxCircleSize = getMaxCircleSize();
        if (this.mSupportHardware) {
            initHwProperties();
            animatorToRadius = getRtAnimatorToRadius(maxCircleSize);
        } else {
            animatorToRadius = getAnimatorToRadius(maxCircleSize);
        }
        this.mFlingAnimationUtils.applyDismissing(animatorToRadius, this.mCircleRadius, maxCircleSize, velocity, maxCircleSize);
        animatorToRadius.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                mAnimationEndRunnable.run();
                KeyguardAffordanceView.this.mFinishing = false;
            }
        });
        animatorToRadius.start();
        setImageAlpha(0.0f, true);
        if (this.mPreviewView != null) {
            this.mPreviewView.setVisibility(0);
            this.mPreviewClipper = ViewAnimationUtils.createCircularReveal(this.mPreviewView, getLeft() + this.mCenterX, getTop() + this.mCenterY, this.mCircleRadius, maxCircleSize);
            this.mFlingAnimationUtils.applyDismissing(this.mPreviewClipper, this.mCircleRadius, maxCircleSize, velocity, maxCircleSize);
            this.mPreviewClipper.addListener(this.mClipEndListener);
            this.mPreviewClipper.start();
            if (this.mSupportHardware) {
                startRtCircleFadeOut(animatorToRadius.getDuration());
            }
        }
    }

    private void startRtCircleFadeOut(long duration) {
        RenderNodeAnimator animator = new RenderNodeAnimator(this.mHwCirclePaint, 1, 0.0f);
        animator.setDuration(duration);
        animator.setInterpolator(PhoneStatusBar.ALPHA_OUT);
        animator.setTarget(this);
        animator.start();
    }

    private Animator getRtAnimatorToRadius(float circleRadius) {
        RenderNodeAnimator animator = new RenderNodeAnimator(this.mHwCircleRadius, circleRadius);
        animator.setTarget(this);
        return animator;
    }

    private void initHwProperties() {
        this.mHwCenterX = CanvasProperty.createFloat((float) this.mCenterX);
        this.mHwCenterY = CanvasProperty.createFloat((float) this.mCenterY);
        this.mHwCirclePaint = CanvasProperty.createPaint(this.mCirclePaint);
        this.mHwCircleRadius = CanvasProperty.createFloat(this.mCircleRadius);
    }

    private float getMaxCircleSize() {
        getLocationInWindow(this.mTempPoint);
        float width = (float) (this.mTempPoint[0] + this.mCenterX);
        return (float) Math.hypot((double) Math.max(((float) getRootView().getWidth()) - width, width), (double) ((float) (this.mTempPoint[1] + this.mCenterY)));
    }

    public void setCircleRadius(float circleRadius, boolean slowAnimation) {
        setCircleRadius(circleRadius, slowAnimation, false);
    }

    public void setCircleRadiusWithoutAnimation(float circleRadius) {
        cancelAnimator(this.mCircleAnimator);
        setCircleRadius(circleRadius, false, true);
    }

    private void setCircleRadius(float circleRadius, boolean slowAnimation, boolean noAnimation) {
        boolean radiusHidden = (this.mCircleAnimator == null || !this.mCircleWillBeHidden) ? this.mCircleAnimator == null && this.mCircleRadius == 0.0f : true;
        boolean nowHidden = circleRadius == 0.0f;
        boolean radiusNeedsAnimation = (radiusHidden == nowHidden || noAnimation) ? false : true;
        if (radiusNeedsAnimation) {
            Interpolator interpolator;
            cancelAnimator(this.mCircleAnimator);
            cancelAnimator(this.mPreviewClipper);
            ValueAnimator animator = getAnimatorToRadius(circleRadius);
            if (circleRadius == 0.0f) {
                interpolator = this.mDisappearInterpolator;
            } else {
                interpolator = this.mAppearInterpolator;
            }
            animator.setInterpolator(interpolator);
            long duration = 250;
            if (!slowAnimation) {
                duration = Math.min((long) (80.0f * (Math.abs(this.mCircleRadius - circleRadius) / ((float) this.mMinBackgroundRadius))), 200);
            }
            animator.setDuration(duration);
            animator.start();
            if (this.mPreviewView != null && this.mPreviewView.getVisibility() == 0) {
                this.mPreviewView.setVisibility(0);
                this.mPreviewClipper = ViewAnimationUtils.createCircularReveal(this.mPreviewView, getLeft() + this.mCenterX, getTop() + this.mCenterY, this.mCircleRadius, circleRadius);
                this.mPreviewClipper.setInterpolator(interpolator);
                this.mPreviewClipper.setDuration(duration);
                this.mPreviewClipper.addListener(this.mClipEndListener);
                this.mPreviewClipper.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        KeyguardAffordanceView.this.mPreviewView.setVisibility(4);
                    }
                });
                this.mPreviewClipper.start();
            }
        } else if (this.mCircleAnimator == null) {
            this.mCircleRadius = circleRadius;
            updateIconColor();
            invalidate();
            if (nowHidden && this.mPreviewView != null) {
                this.mPreviewView.setVisibility(4);
            }
        } else if (!this.mCircleWillBeHidden) {
            float diff = circleRadius - ((float) this.mMinBackgroundRadius);
            this.mCircleAnimator.getValues()[0].setFloatValues(new float[]{this.mCircleStartValue + diff, circleRadius});
            this.mCircleAnimator.setCurrentPlayTime(this.mCircleAnimator.getCurrentPlayTime());
        }
    }

    private ValueAnimator getAnimatorToRadius(float circleRadius) {
        boolean z = true;
        ValueAnimator animator = ValueAnimator.ofFloat(new float[]{this.mCircleRadius, circleRadius});
        this.mCircleAnimator = animator;
        this.mCircleStartValue = this.mCircleRadius;
        if (circleRadius != 0.0f) {
            z = false;
        }
        this.mCircleWillBeHidden = z;
        animator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                KeyguardAffordanceView.this.mCircleRadius = ((Float) animation.getAnimatedValue()).floatValue();
                KeyguardAffordanceView.this.updateIconColor();
                KeyguardAffordanceView.this.invalidate();
            }
        });
        animator.addListener(this.mCircleEndListener);
        return animator;
    }

    private void cancelAnimator(Animator animator) {
        if (animator != null) {
            animator.cancel();
        }
    }

    public void setImageScale(float imageScale, boolean animate) {
        setImageScale(imageScale, animate, -1, null);
    }

    public void setImageScale(float imageScale, boolean animate, long duration, Interpolator interpolator) {
        cancelAnimator(this.mScaleAnimator);
        if (animate) {
            ValueAnimator animator = ValueAnimator.ofFloat(new float[]{this.mImageScale, imageScale});
            this.mScaleAnimator = animator;
            animator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    KeyguardAffordanceView.this.mImageScale = ((Float) animation.getAnimatedValue()).floatValue();
                    KeyguardAffordanceView.this.invalidate();
                }
            });
            animator.addListener(this.mScaleEndListener);
            if (interpolator == null) {
                if (imageScale == 0.0f) {
                    interpolator = this.mDisappearInterpolator;
                } else {
                    interpolator = this.mAppearInterpolator;
                }
            }
            animator.setInterpolator(interpolator);
            if (duration == -1) {
                duration = (long) (200.0f * Math.min(1.0f, Math.abs(this.mImageScale - imageScale) / 0.19999999f));
            }
            animator.setDuration(duration);
            animator.start();
            return;
        }
        this.mImageScale = imageScale;
        invalidate();
    }

    public void setRestingAlpha(float alpha) {
        this.mRestingAlpha = alpha;
        setImageAlpha(alpha, false);
    }

    public float getRestingAlpha() {
        return this.mRestingAlpha;
    }

    public void setImageAlpha(float alpha, boolean animate) {
        setImageAlpha(alpha, animate, -1, null, null);
    }

    public void setImageAlpha(float alpha, boolean animate, long duration, Interpolator interpolator, Runnable runnable) {
        cancelAnimator(this.mAlphaAnimator);
        int endAlpha = (int) (alpha * 255.0f);
        final Drawable background = getBackground();
        if (animate) {
            ValueAnimator animator = ValueAnimator.ofInt(new int[]{getImageAlpha(), endAlpha});
            this.mAlphaAnimator = animator;
            animator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    int alpha = ((Integer) animation.getAnimatedValue()).intValue();
                    if (background != null) {
                        background.mutate().setAlpha(alpha);
                    }
                    KeyguardAffordanceView.this.setImageAlpha(alpha);
                }
            });
            animator.addListener(this.mAlphaEndListener);
            if (interpolator == null) {
                if (alpha == 0.0f) {
                    interpolator = this.mDisappearInterpolator;
                } else {
                    interpolator = this.mAppearInterpolator;
                }
            }
            animator.setInterpolator(interpolator);
            if (duration == -1) {
                duration = (long) (200.0f * Math.min(1.0f, ((float) Math.abs(currentAlpha - endAlpha)) / 255.0f));
            }
            animator.setDuration(duration);
            if (runnable != null) {
                animator.addListener(getEndListener(runnable));
            }
            animator.start();
            return;
        }
        if (background != null) {
            background.mutate().setAlpha(endAlpha);
        }
        setImageAlpha(endAlpha);
    }

    private AnimatorListener getEndListener(final Runnable runnable) {
        return new AnimatorListenerAdapter() {
            boolean mCancelled;

            public void onAnimationCancel(Animator animation) {
                this.mCancelled = true;
            }

            public void onAnimationEnd(Animator animation) {
                if (!this.mCancelled) {
                    runnable.run();
                }
            }
        };
    }

    public float getCircleRadius() {
        return this.mCircleRadius;
    }

    public boolean performClick() {
        if (isClickable()) {
            return super.performClick();
        }
        return false;
    }
}
