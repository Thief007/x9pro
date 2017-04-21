package com.android.systemui.statusbar;

import android.content.Context;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import com.android.systemui.R;

public abstract class ExpandableOutlineView extends ExpandableView {
    private boolean mCustomOutline;
    private float mOutlineAlpha = 1.0f;
    private final Rect mOutlineRect = new Rect();
    protected final int mRoundedRectCornerRadius = getResources().getDimensionPixelSize(R.dimen.notification_material_rounded_rect_radius);

    public ExpandableOutlineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOutlineProvider(new ViewOutlineProvider() {
            public void getOutline(View view, Outline outline) {
                if (ExpandableOutlineView.this.mCustomOutline) {
                    outline.setRoundRect(ExpandableOutlineView.this.mOutlineRect, (float) ExpandableOutlineView.this.mRoundedRectCornerRadius);
                } else {
                    outline.setRect(0, ExpandableOutlineView.this.mClipTopAmount, ExpandableOutlineView.this.getWidth(), Math.max(ExpandableOutlineView.this.getActualHeight(), ExpandableOutlineView.this.mClipTopAmount));
                }
                outline.setAlpha(ExpandableOutlineView.this.mOutlineAlpha);
            }
        });
    }

    public void setActualHeight(int actualHeight, boolean notifyListeners) {
        super.setActualHeight(actualHeight, notifyListeners);
        invalidateOutline();
    }

    public void setClipTopAmount(int clipTopAmount) {
        super.setClipTopAmount(clipTopAmount);
        invalidateOutline();
    }

    protected void setOutlineAlpha(float alpha) {
        this.mOutlineAlpha = alpha;
        invalidateOutline();
    }

    protected void setOutlineRect(RectF rect) {
        if (rect != null) {
            setOutlineRect(rect.left, rect.top, rect.right, rect.bottom);
            return;
        }
        this.mCustomOutline = false;
        setClipToOutline(false);
        invalidateOutline();
    }

    protected void setOutlineRect(float left, float top, float right, float bottom) {
        this.mCustomOutline = true;
        setClipToOutline(true);
        this.mOutlineRect.set((int) left, (int) top, (int) right, (int) bottom);
        this.mOutlineRect.bottom = (int) Math.max(top, (float) this.mOutlineRect.bottom);
        this.mOutlineRect.right = (int) Math.max(left, (float) this.mOutlineRect.right);
        invalidateOutline();
    }
}
