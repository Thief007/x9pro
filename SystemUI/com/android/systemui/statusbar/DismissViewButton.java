package com.android.systemui.statusbar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.Button;
import com.android.systemui.R;

public class DismissViewButton extends Button {
    private Drawable mActiveDrawable;
    private AnimatedVectorDrawable mAnimatedDismissDrawable;
    private final Drawable mStaticDismissDrawable;

    public DismissViewButton(Context context) {
        this(context, null);
    }

    public DismissViewButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DismissViewButton(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public DismissViewButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mAnimatedDismissDrawable = (AnimatedVectorDrawable) getContext().getDrawable(R.drawable.dismiss_all_shape_animation).mutate();
        this.mAnimatedDismissDrawable.setCallback(this);
        this.mAnimatedDismissDrawable.setBounds(0, 0, this.mAnimatedDismissDrawable.getIntrinsicWidth(), this.mAnimatedDismissDrawable.getIntrinsicHeight());
        this.mStaticDismissDrawable = getContext().getDrawable(R.drawable.dismiss_all_shape);
        this.mStaticDismissDrawable.setBounds(0, 0, this.mStaticDismissDrawable.getIntrinsicWidth(), this.mStaticDismissDrawable.getIntrinsicHeight());
        this.mStaticDismissDrawable.setCallback(this);
        this.mActiveDrawable = this.mStaticDismissDrawable;
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        int drawableHeight = this.mActiveDrawable.getBounds().height();
        boolean isRtl = getLayoutDirection() == 1;
        canvas.translate((float) (isRtl ? (getWidth() / 2) + (drawableHeight / 2) : (getWidth() / 2) - (drawableHeight / 2)), (((float) getHeight()) / 2.0f) + (((float) drawableHeight) / 2.0f));
        canvas.scale(isRtl ? -1.0f : 1.0f, -1.0f);
        this.mActiveDrawable.draw(canvas);
        canvas.restore();
    }

    public boolean performClick() {
        if (!this.mAnimatedDismissDrawable.isRunning()) {
            this.mActiveDrawable = this.mAnimatedDismissDrawable;
            this.mAnimatedDismissDrawable.start();
        }
        return super.performClick();
    }

    protected boolean verifyDrawable(Drawable who) {
        if (super.verifyDrawable(who) || who == this.mAnimatedDismissDrawable || who == this.mStaticDismissDrawable) {
            return true;
        }
        return false;
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public void getDrawingRect(Rect outRect) {
        super.getDrawingRect(outRect);
        float translationX = ((ViewGroup) this.mParent).getTranslationX();
        float translationY = ((ViewGroup) this.mParent).getTranslationY();
        outRect.left = (int) (((float) outRect.left) + translationX);
        outRect.right = (int) (((float) outRect.right) + translationX);
        outRect.top = (int) (((float) outRect.top) + translationY);
        outRect.bottom = (int) (((float) outRect.bottom) + translationY);
    }

    public void showButton() {
        this.mActiveDrawable = this.mStaticDismissDrawable;
        invalidate();
    }

    public boolean isButtonStatic() {
        return this.mActiveDrawable == this.mStaticDismissDrawable;
    }
}
