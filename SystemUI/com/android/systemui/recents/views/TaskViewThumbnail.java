package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.view.View;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;

public class TaskViewThumbnail extends View {
    RectF mBitmapRect;
    BitmapShader mBitmapShader;
    Rect mClipRect;
    RecentsConfiguration mConfig;
    float mDimAlpha;
    Paint mDrawPaint;
    boolean mInvisible;
    RectF mLayoutRect;
    LightingColorFilter mLightingColorFilter;
    Matrix mScaleMatrix;
    View mTaskBar;
    float mThumbnailAlpha;
    ValueAnimator mThumbnailAlphaAnimator;
    AnimatorUpdateListener mThumbnailAlphaUpdateListener;

    public TaskViewThumbnail(Context context) {
        this(context, null);
    }

    public TaskViewThumbnail(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TaskViewThumbnail(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TaskViewThumbnail(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mScaleMatrix = new Matrix();
        this.mDrawPaint = new Paint();
        this.mBitmapRect = new RectF();
        this.mLayoutRect = new RectF();
        this.mLightingColorFilter = new LightingColorFilter(-1, 0);
        this.mThumbnailAlphaUpdateListener = new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                TaskViewThumbnail.this.mThumbnailAlpha = ((Float) animation.getAnimatedValue()).floatValue();
                TaskViewThumbnail.this.updateThumbnailPaintFilter();
            }
        };
        this.mClipRect = new Rect();
        this.mConfig = RecentsConfiguration.getInstance();
        this.mDrawPaint.setColorFilter(this.mLightingColorFilter);
        this.mDrawPaint.setFilterBitmap(true);
        this.mDrawPaint.setAntiAlias(true);
    }

    protected void onFinishInflate() {
        this.mThumbnailAlpha = this.mConfig.taskViewThumbnailAlpha;
        updateThumbnailPaintFilter();
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            this.mLayoutRect.set(0.0f, 0.0f, (float) getWidth(), (float) getHeight());
            updateThumbnailScale();
        }
    }

    protected void onDraw(Canvas canvas) {
        if (!this.mInvisible) {
            canvas.drawRoundRect(0.0f, 0.0f, (float) getWidth(), (float) getHeight(), (float) this.mConfig.taskViewRoundedCornerRadiusPx, (float) this.mConfig.taskViewRoundedCornerRadiusPx, this.mDrawPaint);
        }
    }

    void setThumbnail(Bitmap bm) {
        if (bm != null) {
            this.mBitmapShader = new BitmapShader(bm, TileMode.CLAMP, TileMode.CLAMP);
            this.mDrawPaint.setShader(this.mBitmapShader);
            this.mBitmapRect.set(0.0f, 0.0f, (float) bm.getWidth(), (float) bm.getHeight());
            updateThumbnailScale();
        } else {
            this.mBitmapShader = null;
            this.mDrawPaint.setShader(null);
        }
        updateThumbnailPaintFilter();
    }

    void updateThumbnailPaintFilter() {
        if (!this.mInvisible) {
            int mul = (int) (((1.0f - this.mDimAlpha) * this.mThumbnailAlpha) * 255.0f);
            int add = (int) (((1.0f - this.mDimAlpha) * (1.0f - this.mThumbnailAlpha)) * 255.0f);
            if (this.mBitmapShader != null) {
                this.mLightingColorFilter.setColorMultiply(Color.argb(255, mul, mul, mul));
                this.mLightingColorFilter.setColorAdd(Color.argb(0, add, add, add));
                this.mDrawPaint.setColorFilter(this.mLightingColorFilter);
                this.mDrawPaint.setColor(-1);
            } else {
                int grey = mul + add;
                this.mDrawPaint.setColorFilter(null);
                this.mDrawPaint.setColor(Color.argb(255, grey, grey, grey));
            }
            invalidate();
        }
    }

    void updateThumbnailScale() {
        if (this.mBitmapShader != null) {
            this.mScaleMatrix.setRectToRect(this.mBitmapRect, this.mLayoutRect, ScaleToFit.FILL);
            this.mBitmapShader.setLocalMatrix(this.mScaleMatrix);
        }
    }

    void updateClipToTaskBar(View taskBar) {
        this.mTaskBar = taskBar;
        this.mClipRect.set(0, (int) Math.max(0.0f, (taskBar.getTranslationY() + ((float) taskBar.getMeasuredHeight())) - 1.0f), getMeasuredWidth(), getMeasuredHeight());
        setClipBounds(this.mClipRect);
    }

    void updateThumbnailVisibility(int clipBottom) {
        boolean invisible = this.mTaskBar != null && getHeight() - clipBottom <= this.mTaskBar.getHeight();
        if (invisible != this.mInvisible) {
            this.mInvisible = invisible;
            if (!this.mInvisible) {
                updateThumbnailPaintFilter();
            }
            invalidate();
        }
    }

    public void setDimAlpha(float dimAlpha) {
        this.mDimAlpha = dimAlpha;
        updateThumbnailPaintFilter();
    }

    void rebindToTask(Task t) {
        if (t.thumbnail != null) {
            setThumbnail(t.thumbnail);
        } else {
            setThumbnail(null);
        }
    }

    void unbindFromTask() {
        setThumbnail(null);
    }

    void onFocusChanged(boolean focused) {
        if (focused) {
            if (Float.compare(getAlpha(), 1.0f) != 0) {
                startFadeAnimation(1.0f, 0, 150, null);
            }
        } else if (Float.compare(getAlpha(), this.mConfig.taskViewThumbnailAlpha) != 0) {
            startFadeAnimation(this.mConfig.taskViewThumbnailAlpha, 0, 150, null);
        }
    }

    void prepareEnterRecentsAnimation(boolean isTaskViewLaunchTargetTask) {
        if (isTaskViewLaunchTargetTask) {
            this.mThumbnailAlpha = 1.0f;
        } else {
            this.mThumbnailAlpha = this.mConfig.taskViewThumbnailAlpha;
        }
        updateThumbnailPaintFilter();
    }

    void startLaunchTaskAnimation(Runnable postAnimRunnable) {
        startFadeAnimation(1.0f, 0, this.mConfig.taskViewExitToAppDuration, postAnimRunnable);
    }

    void startFadeAnimation(float finalAlpha, int delay, int duration, final Runnable postAnimRunnable) {
        Utilities.cancelAnimationWithoutCallbacks(this.mThumbnailAlphaAnimator);
        this.mThumbnailAlphaAnimator = ValueAnimator.ofFloat(new float[]{this.mThumbnailAlpha, finalAlpha});
        this.mThumbnailAlphaAnimator.setStartDelay((long) delay);
        this.mThumbnailAlphaAnimator.setDuration((long) duration);
        this.mThumbnailAlphaAnimator.setInterpolator(this.mConfig.fastOutSlowInInterpolator);
        this.mThumbnailAlphaAnimator.addUpdateListener(this.mThumbnailAlphaUpdateListener);
        if (postAnimRunnable != null) {
            this.mThumbnailAlphaAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    postAnimRunnable.run();
                }
            });
        }
        this.mThumbnailAlphaAnimator.start();
    }
}
