package com.android.systemui.recents.views;

import android.graphics.Outline;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewOutlineProvider;
import com.android.systemui.recents.RecentsConfiguration;

public class AnimateableViewBounds extends ViewOutlineProvider {
    float mAlpha = 1.0f;
    Rect mClipBounds = new Rect();
    Rect mClipRect = new Rect();
    RecentsConfiguration mConfig = RecentsConfiguration.getInstance();
    int mCornerRadius;
    final float mMinAlpha = 0.25f;
    TaskView mSourceView;

    public AnimateableViewBounds(TaskView source, int cornerRadius) {
        this.mSourceView = source;
        this.mCornerRadius = cornerRadius;
        setClipBottom(getClipBottom());
    }

    public void getOutline(View view, Outline outline) {
        outline.setAlpha((this.mAlpha / 0.75f) + 0.25f);
        outline.setRoundRect(this.mClipRect.left, this.mClipRect.top, this.mSourceView.getWidth() - this.mClipRect.right, this.mSourceView.getHeight() - this.mClipRect.bottom, (float) this.mCornerRadius);
    }

    void setAlpha(float alpha) {
        if (Float.compare(alpha, this.mAlpha) != 0) {
            this.mAlpha = alpha;
            this.mSourceView.invalidateOutline();
        }
    }

    public void setClipBottom(int bottom) {
        if (bottom != this.mClipRect.bottom) {
            this.mClipRect.bottom = bottom;
            this.mSourceView.invalidateOutline();
            updateClipBounds();
            if (!this.mConfig.useHardwareLayers) {
                this.mSourceView.mThumbnailView.updateThumbnailVisibility(bottom - this.mSourceView.getPaddingBottom());
            }
        }
    }

    public int getClipBottom() {
        return this.mClipRect.bottom;
    }

    private void updateClipBounds() {
        this.mClipBounds.set(this.mClipRect.left, this.mClipRect.top, this.mSourceView.getWidth() - this.mClipRect.right, this.mSourceView.getHeight() - this.mClipRect.bottom);
        this.mSourceView.setClipBounds(this.mClipBounds);
    }
}
