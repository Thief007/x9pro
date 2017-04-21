package com.android.systemui.statusbar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View.MeasureSpec;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.android.systemui.R;

public class SpeedBumpView extends ExpandableView {
    private final Interpolator mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(getContext(), 17563661);
    private boolean mIsVisible = true;
    private AlphaOptimizedView mLine;
    private final int mSpeedBumpHeight = getResources().getDimensionPixelSize(R.dimen.speed_bump_height);

    public SpeedBumpView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mLine = (AlphaOptimizedView) findViewById(R.id.speedbump_line);
    }

    protected int getInitialHeight() {
        return this.mSpeedBumpHeight;
    }

    public int getIntrinsicHeight() {
        return this.mSpeedBumpHeight;
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        this.mLine.setPivotX((float) (this.mLine.getWidth() / 2));
        this.mLine.setPivotY((float) (this.mLine.getHeight() / 2));
        setOutlineProvider(null);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), this.mSpeedBumpHeight);
    }

    public boolean isTransparent() {
        return true;
    }

    public void performVisibilityAnimation(boolean nowVisible, long delay) {
        animateDivider(nowVisible, delay, null);
    }

    public void animateDivider(boolean nowVisible, long delay, Runnable onFinishedRunnable) {
        if (nowVisible != this.mIsVisible) {
            float endValue = nowVisible ? 1.0f : 0.0f;
            this.mLine.animate().alpha(endValue).setStartDelay(delay).scaleX(endValue).scaleY(endValue).setInterpolator(this.mFastOutSlowInInterpolator).withEndAction(onFinishedRunnable);
            this.mIsVisible = nowVisible;
        } else if (onFinishedRunnable != null) {
            onFinishedRunnable.run();
        }
    }

    public void setInvisible() {
        this.mLine.setAlpha(0.0f);
        this.mLine.setScaleX(0.0f);
        this.mLine.setScaleY(0.0f);
        this.mIsVisible = false;
    }

    public void performRemoveAnimation(long duration, float translationDirection, Runnable onFinishedRunnable) {
        performVisibilityAnimation(false, 0);
    }

    public void performAddAnimation(long delay, long duration) {
        performVisibilityAnimation(true, delay);
    }
}
