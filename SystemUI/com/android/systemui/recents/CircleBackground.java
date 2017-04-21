package com.android.systemui.recents;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.RelativeLayout;

public class CircleBackground extends RelativeLayout {
    private boolean animationRunning = false;
    ValueAnimator mAnimator = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
    RippleView mRippleView;
    private Paint paint;

    private class RippleView extends View {
        private float mProgress;

        public RippleView(Context context) {
            super(context);
        }

        protected void onDraw(Canvas canvas) {
            int radius = (int) ((((float) 11) * this.mProgress) + 57.0f);
            int intAlpha = (int) (255.0f * ((-1.0f * this.mProgress) + 1.0f));
            int center = Math.min(getWidth(), getHeight()) / 2;
            CircleBackground.this.paint.setStrokeWidth((float) ((int) ((((float) -4) * this.mProgress) + 6.0f)));
            CircleBackground.this.paint.setColor(Color.argb(intAlpha, 255, 255, 255));
            canvas.drawCircle((float) center, (float) center, (float) radius, CircleBackground.this.paint);
        }

        public void setProgress(float progress) {
            this.mProgress = progress;
        }
    }

    public CircleBackground(Context context) {
        super(context);
    }

    public CircleBackground(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CircleBackground(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        this.paint = new Paint();
        this.paint.setAntiAlias(true);
        this.paint.setStyle(Style.STROKE);
        this.mRippleView = new RippleView(getContext());
        addView(this.mRippleView);
        this.mAnimator = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        this.mAnimator.setTarget(this.mRippleView);
        this.mAnimator.setDuration(1500);
        this.mAnimator.setRepeatCount(-1);
        this.mAnimator.setRepeatMode(1);
        this.mAnimator.setInterpolator(new DecelerateInterpolator());
        this.mAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                CircleBackground.this.mRippleView.setProgress(((Float) animation.getAnimatedValue()).floatValue());
                CircleBackground.this.mRippleView.invalidate();
            }
        });
    }

    public void startRippleAnimation() {
        if (!isRippleAnimationRunning()) {
            this.mAnimator.start();
            this.animationRunning = true;
        }
    }

    public boolean isRippleAnimationRunning() {
        return this.animationRunning;
    }
}
