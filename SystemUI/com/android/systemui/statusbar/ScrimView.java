package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;

public class ScrimView extends View {
    private ValueAnimator mAlphaAnimator;
    private AnimatorUpdateListener mAlphaUpdateListener;
    private AnimatorListenerAdapter mClearAnimatorListener;
    private boolean mDrawAsSrc;
    private boolean mIsEmpty;
    private int mScrimColor;
    private float mViewAlpha;

    public ScrimView(Context context) {
        this(context, null);
    }

    public ScrimView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScrimView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ScrimView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mIsEmpty = true;
        this.mViewAlpha = 1.0f;
        this.mAlphaUpdateListener = new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                ScrimView.this.mViewAlpha = ((Float) animation.getAnimatedValue()).floatValue();
                ScrimView.this.invalidate();
            }
        };
        this.mClearAnimatorListener = new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                ScrimView.this.mAlphaAnimator = null;
            }
        };
    }

    protected void onDraw(Canvas canvas) {
        if (this.mDrawAsSrc || (!this.mIsEmpty && this.mViewAlpha > 0.0f)) {
            Mode mode = this.mDrawAsSrc ? Mode.SRC : Mode.SRC_OVER;
            int color = this.mScrimColor;
            canvas.drawColor(Color.argb((int) (((float) Color.alpha(color)) * this.mViewAlpha), Color.red(color), Color.green(color), Color.blue(color)), mode);
        }
    }

    public void setDrawAsSrc(boolean asSrc) {
        this.mDrawAsSrc = asSrc;
        invalidate();
    }

    public void setScrimColor(int color) {
        boolean z = false;
        if (color != this.mScrimColor) {
            if (Color.alpha(color) == 0) {
                z = true;
            }
            this.mIsEmpty = z;
            this.mScrimColor = color;
            invalidate();
        }
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public void animateViewAlpha(float alpha, long durationOut, Interpolator interpolator) {
        if (this.mAlphaAnimator != null) {
            this.mAlphaAnimator.cancel();
        }
        this.mAlphaAnimator = ValueAnimator.ofFloat(new float[]{this.mViewAlpha, alpha});
        this.mAlphaAnimator.addUpdateListener(this.mAlphaUpdateListener);
        this.mAlphaAnimator.addListener(this.mClearAnimatorListener);
        this.mAlphaAnimator.setInterpolator(interpolator);
        this.mAlphaAnimator.setDuration(durationOut);
        this.mAlphaAnimator.start();
    }
}
