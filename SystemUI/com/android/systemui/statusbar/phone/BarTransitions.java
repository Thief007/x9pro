package com.android.systemui.statusbar.phone;

import android.animation.TimeInterpolator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.View;
import android.view.animation.LinearInterpolator;
import com.android.systemui.R;
import com.mediatek.systemui.statusbar.util.FeatureOptions;

public class BarTransitions {
    public static final boolean HIGH_END = ActivityManager.isHighEndGfx();
    private final BarBackgroundDrawable mBarBackground;
    private int mMode;
    private final String mTag;
    private final View mView;

    private static class BarBackgroundDrawable extends Drawable {
        private boolean mAnimating;
        private int mColor;
        private int mColorStart;
        private long mEndTime;
        private final Drawable mGradient;
        private int mGradientAlpha;
        private int mGradientAlphaStart;
        private final TimeInterpolator mInterpolator;
        private int mMode = -1;
        private final int mOpaque;
        private final int mSemiTransparent;
        private long mStartTime;
        private final int mTransparent;
        private final int mWarning;

        public BarBackgroundDrawable(Context context, int gradientResourceId) {
            Resources res = context.getResources();
            this.mOpaque = context.getColor(R.color.system_bar_background_opaque);
            this.mSemiTransparent = context.getColor(R.color.system_bar_background_semi_transparent);
            this.mTransparent = context.getColor(R.color.system_bar_background_transparent);
            this.mWarning = context.getColor(17170522);
            this.mGradient = context.getDrawable(gradientResourceId);
            this.mInterpolator = new LinearInterpolator();
        }

        public void setAlpha(int alpha) {
        }

        public void setColorFilter(ColorFilter colorFilter) {
        }

        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);
            this.mGradient.setBounds(bounds);
        }

        public void applyModeBackground(int oldMode, int newMode, boolean animate) {
            if (this.mMode != newMode) {
                this.mMode = newMode;
                this.mAnimating = animate;
                if (animate) {
                    long now = SystemClock.elapsedRealtime();
                    this.mStartTime = now;
                    this.mEndTime = 200 + now;
                    this.mGradientAlphaStart = this.mGradientAlpha;
                    this.mColorStart = this.mColor;
                }
                invalidateSelf();
            }
        }

        public int getOpacity() {
            return -3;
        }

        public void finishAnimation() {
            if (this.mAnimating) {
                this.mAnimating = false;
                invalidateSelf();
            }
        }

        public void draw(Canvas canvas) {
            int targetColor;
            if (this.mMode == 5) {
                targetColor = this.mWarning;
            } else if (this.mMode == 2) {
                targetColor = this.mSemiTransparent;
            } else if (this.mMode == 1) {
                targetColor = this.mSemiTransparent;
            } else if (this.mMode == 4 || this.mMode == 6) {
                targetColor = this.mTransparent;
            } else {
                targetColor = this.mOpaque;
            }
            if (this.mAnimating) {
                long now = SystemClock.elapsedRealtime();
                if (now >= this.mEndTime) {
                    this.mAnimating = false;
                    this.mColor = targetColor;
                    this.mGradientAlpha = 0;
                } else {
                    float v = Math.max(0.0f, Math.min(this.mInterpolator.getInterpolation(((float) (now - this.mStartTime)) / ((float) (this.mEndTime - this.mStartTime))), 1.0f));
                    this.mGradientAlpha = (int) ((v * 0.0f) + (((float) this.mGradientAlphaStart) * (1.0f - v)));
                    this.mColor = Color.argb((int) ((((float) Color.alpha(targetColor)) * v) + (((float) Color.alpha(this.mColorStart)) * (1.0f - v))), (int) ((((float) Color.red(targetColor)) * v) + (((float) Color.red(this.mColorStart)) * (1.0f - v))), (int) ((((float) Color.green(targetColor)) * v) + (((float) Color.green(this.mColorStart)) * (1.0f - v))), (int) ((((float) Color.blue(targetColor)) * v) + (((float) Color.blue(this.mColorStart)) * (1.0f - v))));
                }
            } else {
                this.mColor = targetColor;
                this.mGradientAlpha = 0;
            }
            if (this.mGradientAlpha > 0) {
                this.mGradient.setAlpha(this.mGradientAlpha);
                this.mGradient.draw(canvas);
            }
            if (Color.alpha(this.mColor) > 0) {
                canvas.drawColor(this.mColor);
            }
            if (this.mAnimating) {
                invalidateSelf();
            }
        }
    }

    public BarTransitions(View view, int gradientResourceId) {
        this.mTag = "BarTransitions." + view.getClass().getSimpleName();
        this.mView = view;
        this.mBarBackground = new BarBackgroundDrawable(this.mView.getContext(), gradientResourceId);
        if (HIGH_END || FeatureOptions.LOW_RAM_SUPPORT) {
            this.mView.setBackground(this.mBarBackground);
        }
    }

    public int getMode() {
        return this.mMode;
    }

    public void transitionTo(int mode, boolean animate) {
        if (!HIGH_END) {
            if (!(mode == 1 || mode == 2)) {
                if (mode == 4) {
                }
            }
            if (!FeatureOptions.LOW_RAM_SUPPORT) {
                mode = 0;
            }
        }
        if (!(HIGH_END || mode != 6 || FeatureOptions.LOW_RAM_SUPPORT)) {
            mode = 3;
        }
        if (this.mMode != mode) {
            int oldMode = this.mMode;
            this.mMode = mode;
            onTransition(oldMode, this.mMode, animate);
        }
    }

    protected void onTransition(int oldMode, int newMode, boolean animate) {
        if (HIGH_END || FeatureOptions.LOW_RAM_SUPPORT) {
            applyModeBackground(oldMode, newMode, animate);
        }
    }

    protected void applyModeBackground(int oldMode, int newMode, boolean animate) {
        this.mBarBackground.applyModeBackground(oldMode, newMode, animate);
    }

    public static String modeToString(int mode) {
        if (mode == 0) {
            return "MODE_OPAQUE";
        }
        if (mode == 1) {
            return "MODE_SEMI_TRANSPARENT";
        }
        if (mode == 2) {
            return "MODE_TRANSLUCENT";
        }
        if (mode == 3) {
            return "MODE_LIGHTS_OUT";
        }
        if (mode == 4) {
            return "MODE_TRANSPARENT";
        }
        if (mode == 5) {
            return "MODE_WARNING";
        }
        if (mode == 6) {
            return "MODE_LIGHTS_OUT_TRANSPARENT";
        }
        throw new IllegalArgumentException("Unknown mode " + mode);
    }

    public void finishAnimations() {
        this.mBarBackground.finishAnimation();
    }

    protected boolean isLightsOut(int mode) {
        return mode == 3 || mode == 6;
    }
}
