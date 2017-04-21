package com.android.systemui.statusbar;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import com.android.systemui.R;
import java.util.ArrayList;

public abstract class ExpandableView extends FrameLayout {
    private static Rect mClipRect = new Rect();
    private int mActualHeight;
    private boolean mActualHeightInitialized;
    private final int mBottomDecorHeight = resolveBottomDecorHeight();
    protected int mClipTopAmount;
    private int mClipTopOptimization;
    private boolean mDark;
    private ArrayList<View> mMatchParentViews = new ArrayList();
    protected int mMaxViewHeight = getResources().getDimensionPixelSize(R.dimen.notification_max_height);
    private int mMinClipTopAmount = 0;
    protected OnHeightChangedListener mOnHeightChangedListener;
    private boolean mWillBeGone;

    public interface OnHeightChangedListener {
        void onHeightChanged(ExpandableView expandableView, boolean z);

        void onReset(ExpandableView expandableView);
    }

    public abstract void performAddAnimation(long j, long j2);

    public abstract void performRemoveAnimation(long j, float f, Runnable runnable);

    public ExpandableView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected int resolveBottomDecorHeight() {
        return getResources().getDimensionPixelSize(R.dimen.notification_bottom_decor_height);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int ownMaxHeight = this.mMaxViewHeight;
        boolean hasFixedHeight = MeasureSpec.getMode(heightMeasureSpec) == 1073741824;
        if (hasFixedHeight) {
            ownMaxHeight = Math.min(MeasureSpec.getSize(heightMeasureSpec), ownMaxHeight);
        }
        int newHeightSpec = MeasureSpec.makeMeasureSpec(ownMaxHeight, Integer.MIN_VALUE);
        int maxChildHeight = 0;
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (!(child.getVisibility() == 8 || isChildInvisible(child))) {
                int childHeightSpec = newHeightSpec;
                LayoutParams layoutParams = child.getLayoutParams();
                if (layoutParams.height != -1) {
                    if (layoutParams.height >= 0) {
                        if (layoutParams.height > ownMaxHeight) {
                            childHeightSpec = MeasureSpec.makeMeasureSpec(ownMaxHeight, 1073741824);
                        } else {
                            childHeightSpec = MeasureSpec.makeMeasureSpec(layoutParams.height, 1073741824);
                        }
                    }
                    child.measure(getChildMeasureSpec(widthMeasureSpec, 0, layoutParams.width), childHeightSpec);
                    maxChildHeight = Math.max(maxChildHeight, child.getMeasuredHeight());
                } else {
                    this.mMatchParentViews.add(child);
                }
            }
        }
        int ownHeight = hasFixedHeight ? ownMaxHeight : Math.min(ownMaxHeight, maxChildHeight);
        newHeightSpec = MeasureSpec.makeMeasureSpec(ownHeight, 1073741824);
        for (View child2 : this.mMatchParentViews) {
            child2.measure(getChildMeasureSpec(widthMeasureSpec, 0, child2.getLayoutParams().width), newHeightSpec);
        }
        this.mMatchParentViews.clear();
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (canHaveBottomDecor()) {
            ownHeight += this.mBottomDecorHeight;
        }
        setMeasuredDimension(width, ownHeight);
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!this.mActualHeightInitialized && this.mActualHeight == 0) {
            int initialHeight = getInitialHeight();
            if (initialHeight != 0) {
                setContentHeight(initialHeight);
            }
        }
        updateClipping();
    }

    protected void resetActualHeight() {
        this.mActualHeight = 0;
        this.mActualHeightInitialized = false;
        requestLayout();
    }

    protected int getInitialHeight() {
        return getHeight();
    }

    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        if (filterMotionEvent(ev)) {
            return super.dispatchGenericMotionEvent(ev);
        }
        return false;
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (filterMotionEvent(ev)) {
            return super.dispatchTouchEvent(ev);
        }
        return false;
    }

    protected boolean filterMotionEvent(MotionEvent event) {
        if (event.getActionMasked() != 0 && event.getActionMasked() != 9 && event.getActionMasked() != 7) {
            return true;
        }
        if (event.getY() <= ((float) this.mClipTopAmount) || event.getY() >= ((float) this.mActualHeight)) {
            return false;
        }
        return true;
    }

    public void setActualHeight(int actualHeight, boolean notifyListeners) {
        this.mActualHeightInitialized = true;
        this.mActualHeight = actualHeight;
        updateClipping();
        if (notifyListeners) {
            notifyHeightChanged(false);
        }
    }

    public void setContentHeight(int contentHeight) {
        setActualHeight(getBottomDecorHeight() + contentHeight, true);
    }

    public int getActualHeight() {
        return this.mActualHeight;
    }

    public int getBottomDecorHeight() {
        return hasBottomDecor() ? this.mBottomDecorHeight : 0;
    }

    protected boolean canHaveBottomDecor() {
        return false;
    }

    protected boolean hasBottomDecor() {
        return false;
    }

    public int getMaxContentHeight() {
        return getHeight();
    }

    public int getMinHeight() {
        return getHeight();
    }

    public void setDimmed(boolean dimmed, boolean fade) {
    }

    public void setDark(boolean dark, boolean fade, long delay) {
        this.mDark = dark;
    }

    public boolean isDark() {
        return this.mDark;
    }

    public void setHideSensitiveForIntrinsicHeight(boolean hideSensitive) {
    }

    public void setHideSensitive(boolean hideSensitive, boolean animated, long delay, long duration) {
    }

    public int getIntrinsicHeight() {
        return getHeight();
    }

    public void setClipTopAmount(int clipTopAmount) {
        this.mClipTopAmount = clipTopAmount;
    }

    public int getClipTopAmount() {
        return this.mClipTopAmount;
    }

    public void setOnHeightChangedListener(OnHeightChangedListener listener) {
        this.mOnHeightChangedListener = listener;
    }

    public boolean isContentExpandable() {
        return false;
    }

    public void notifyHeightChanged(boolean needsAnimation) {
        if (this.mOnHeightChangedListener != null) {
            this.mOnHeightChangedListener.onHeightChanged(this, needsAnimation);
        }
    }

    public boolean isTransparent() {
        return false;
    }

    public void setBelowSpeedBump(boolean below) {
    }

    public void onHeightReset() {
        if (this.mOnHeightChangedListener != null) {
            this.mOnHeightChangedListener.onReset(this);
        }
    }

    public void getDrawingRect(Rect outRect) {
        super.getDrawingRect(outRect);
        outRect.left = (int) (((float) outRect.left) + getTranslationX());
        outRect.right = (int) (((float) outRect.right) + getTranslationX());
        outRect.bottom = (int) ((((float) outRect.top) + getTranslationY()) + ((float) getActualHeight()));
        outRect.top = (int) (((float) outRect.top) + (getTranslationY() + ((float) getClipTopAmount())));
    }

    public void getBoundsOnScreen(Rect outRect, boolean clipToParent) {
        super.getBoundsOnScreen(outRect, clipToParent);
        outRect.bottom = outRect.top + getActualHeight();
        outRect.top += getClipTopOptimization();
    }

    public int getContentHeight() {
        return this.mActualHeight - getBottomDecorHeight();
    }

    protected boolean isChildInvisible(View child) {
        return false;
    }

    public boolean areChildrenExpanded() {
        return false;
    }

    private void updateClipping() {
        int top = this.mClipTopOptimization;
        if (top >= getActualHeight()) {
            top = getActualHeight() - 1;
        }
        mClipRect.set(0, top, getWidth(), getActualHeight());
        setClipBounds(mClipRect);
    }

    public int getClipTopOptimization() {
        return this.mClipTopOptimization;
    }

    public void setClipTopOptimization(int clipTopOptimization) {
        this.mClipTopOptimization = clipTopOptimization;
        updateClipping();
    }

    public boolean willBeGone() {
        return this.mWillBeGone;
    }

    public void setWillBeGone(boolean willBeGone) {
        this.mWillBeGone = willBeGone;
    }

    public int getMinClipTopAmount() {
        return this.mMinClipTopAmount;
    }

    public void setMinClipTopAmount(int minClipTopAmount) {
        this.mMinClipTopAmount = minClipTopAmount;
    }
}
