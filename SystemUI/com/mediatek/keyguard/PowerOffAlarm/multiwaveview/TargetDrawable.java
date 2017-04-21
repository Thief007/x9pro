package com.mediatek.keyguard.PowerOffAlarm.multiwaveview;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;

public class TargetDrawable {
    public static final int[] STATE_ACTIVE = new int[]{16842910, 16842914};
    public static final int[] STATE_FOCUSED = new int[]{16842910, -16842914, 16842908};
    public static final int[] STATE_INACTIVE = new int[]{16842910, -16842914};
    private float mAlpha = 1.0f;
    private Drawable mDrawable;
    private boolean mEnabled = true;
    private int mNumDrawables = 1;
    private float mPositionX = 0.0f;
    private float mPositionY = 0.0f;
    private final int mResourceId;
    private float mScaleX = 1.0f;
    private float mScaleY = 1.0f;
    private float mTranslationX = 0.0f;
    private float mTranslationY = 0.0f;

    public TargetDrawable(Resources res, int resId, int count) {
        this.mResourceId = resId;
        setDrawable(res, resId);
        this.mNumDrawables = count;
    }

    public void setDrawable(Resources res, int resId) {
        Drawable drawable = null;
        Drawable drawable2 = resId == 0 ? null : res.getDrawable(resId);
        if (drawable2 != null) {
            drawable = drawable2.mutate();
        }
        this.mDrawable = drawable;
        resizeDrawables();
        setState(STATE_INACTIVE);
    }

    public void setState(int[] state) {
        if (this.mDrawable instanceof StateListDrawable) {
            this.mDrawable.setState(state);
        }
    }

    public boolean isEnabled() {
        return this.mDrawable != null ? this.mEnabled : false;
    }

    private void resizeDrawables() {
        if (this.mDrawable instanceof StateListDrawable) {
            int i;
            StateListDrawable d = this.mDrawable;
            int maxWidth = 0;
            int maxHeight = 0;
            for (i = 0; i < this.mNumDrawables; i++) {
                d.selectDrawable(i);
                Drawable childDrawable = d.getCurrent();
                maxWidth = Math.max(maxWidth, childDrawable.getIntrinsicWidth());
                maxHeight = Math.max(maxHeight, childDrawable.getIntrinsicHeight());
            }
            d.setBounds(0, 0, maxWidth, maxHeight);
            for (i = 0; i < this.mNumDrawables; i++) {
                d.selectDrawable(i);
                d.getCurrent().setBounds(0, 0, maxWidth, maxHeight);
            }
        } else if (this.mDrawable != null) {
            this.mDrawable.setBounds(0, 0, this.mDrawable.getIntrinsicWidth(), this.mDrawable.getIntrinsicHeight());
        }
    }

    public void setX(float x) {
        this.mTranslationX = x;
    }

    public void setY(float y) {
        this.mTranslationY = y;
    }

    public void setAlpha(float alpha) {
        this.mAlpha = alpha;
    }

    public float getX() {
        return this.mTranslationX;
    }

    public float getY() {
        return this.mTranslationY;
    }

    public void setPositionX(float x) {
        this.mPositionX = x;
    }

    public void setPositionY(float y) {
        this.mPositionY = y;
    }

    public int getWidth() {
        return this.mDrawable != null ? this.mDrawable.getIntrinsicWidth() : 0;
    }

    public int getHeight() {
        return this.mDrawable != null ? this.mDrawable.getIntrinsicHeight() : 0;
    }

    public void draw(Canvas canvas) {
        if (this.mDrawable != null && this.mEnabled) {
            canvas.save(1);
            canvas.scale(this.mScaleX, this.mScaleY, this.mPositionX, this.mPositionY);
            canvas.translate(this.mTranslationX + this.mPositionX, this.mTranslationY + this.mPositionY);
            canvas.translate(((float) getWidth()) * -0.5f, ((float) getHeight()) * -0.5f);
            this.mDrawable.setAlpha(Math.round(this.mAlpha * 255.0f));
            this.mDrawable.draw(canvas);
            canvas.restore();
        }
    }

    public int getResourceId() {
        return this.mResourceId;
    }
}
