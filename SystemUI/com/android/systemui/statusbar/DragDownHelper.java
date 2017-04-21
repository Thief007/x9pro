package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.android.systemui.ExpandHelper.Callback;
import com.android.systemui.R;

public class DragDownHelper {
    private Callback mCallback;
    private DragDownCallback mDragDownCallback;
    private boolean mDraggedFarEnough;
    private boolean mDraggingDown;
    private View mHost;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private Interpolator mInterpolator;
    private float mLastHeight;
    private int mMinDragDistance;
    private ExpandableView mStartingChild;
    private final int[] mTemp2 = new int[2];
    private float mTouchSlop;

    public interface DragDownCallback {
        void onDragDownReset();

        boolean onDraggedDown(View view, int i);

        void onThresholdReached();

        void onTouchSlopExceeded();

        void setEmptyDragAmount(float f);
    }

    public DragDownHelper(Context context, View host, Callback callback, DragDownCallback dragDownCallback) {
        this.mMinDragDistance = context.getResources().getDimensionPixelSize(R.dimen.keyguard_drag_down_min_distance);
        this.mInterpolator = AnimationUtils.loadInterpolator(context, 17563661);
        this.mTouchSlop = (float) ViewConfiguration.get(context).getScaledTouchSlop();
        this.mCallback = callback;
        this.mDragDownCallback = dragDownCallback;
        this.mHost = host;
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case 0:
                this.mDraggedFarEnough = false;
                this.mDraggingDown = false;
                this.mStartingChild = null;
                this.mInitialTouchY = y;
                this.mInitialTouchX = x;
                break;
            case 2:
                float h = y - this.mInitialTouchY;
                if (h > this.mTouchSlop && h > Math.abs(x - this.mInitialTouchX)) {
                    this.mDraggingDown = true;
                    captureStartingChild(this.mInitialTouchX, this.mInitialTouchY);
                    this.mInitialTouchY = y;
                    this.mInitialTouchX = x;
                    this.mDragDownCallback.onTouchSlopExceeded();
                    return true;
                }
        }
        return false;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (!this.mDraggingDown) {
            return false;
        }
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case 1:
                if (this.mDraggedFarEnough && this.mDragDownCallback.onDraggedDown(this.mStartingChild, (int) (y - this.mInitialTouchY))) {
                    if (this.mStartingChild == null) {
                        this.mDragDownCallback.setEmptyDragAmount(0.0f);
                    }
                    this.mDraggingDown = false;
                    break;
                }
                stopDragging();
                return false;
            case 2:
                this.mLastHeight = y - this.mInitialTouchY;
                captureStartingChild(this.mInitialTouchX, this.mInitialTouchY);
                if (this.mStartingChild != null) {
                    handleExpansion(this.mLastHeight, this.mStartingChild);
                } else {
                    this.mDragDownCallback.setEmptyDragAmount(this.mLastHeight);
                }
                if (this.mLastHeight > ((float) this.mMinDragDistance)) {
                    if (!this.mDraggedFarEnough) {
                        this.mDraggedFarEnough = true;
                        this.mDragDownCallback.onThresholdReached();
                    }
                } else if (this.mDraggedFarEnough) {
                    this.mDraggedFarEnough = false;
                    this.mDragDownCallback.onDragDownReset();
                }
                return true;
            case 3:
                stopDragging();
                return false;
        }
        return false;
    }

    private void captureStartingChild(float x, float y) {
        if (this.mStartingChild == null) {
            this.mStartingChild = findView(x, y);
            if (this.mStartingChild != null) {
                this.mCallback.setUserLockedChild(this.mStartingChild, true);
            }
        }
    }

    private void handleExpansion(float heightDelta, ExpandableView child) {
        float rubberbandFactor;
        if (heightDelta < 0.0f) {
            heightDelta = 0.0f;
        }
        boolean expandable = child.isContentExpandable();
        if (expandable) {
            rubberbandFactor = 0.5f;
        } else {
            rubberbandFactor = 0.15f;
        }
        float rubberband = heightDelta * rubberbandFactor;
        if (expandable && ((float) child.getMinHeight()) + rubberband > ((float) child.getMaxContentHeight())) {
            rubberband -= ((((float) child.getMinHeight()) + rubberband) - ((float) child.getMaxContentHeight())) * 0.85f;
        }
        child.setContentHeight((int) (((float) child.getMinHeight()) + rubberband));
    }

    private void cancelExpansion(final ExpandableView child) {
        if (child.getContentHeight() != child.getMinHeight()) {
            ObjectAnimator anim = ObjectAnimator.ofInt(child, "contentHeight", new int[]{child.getContentHeight(), child.getMinHeight()});
            anim.setInterpolator(this.mInterpolator);
            anim.setDuration(375);
            anim.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    DragDownHelper.this.mCallback.setUserLockedChild(child, false);
                }
            });
            anim.start();
        }
    }

    private void cancelExpansion() {
        ValueAnimator anim = ValueAnimator.ofFloat(new float[]{this.mLastHeight, 0.0f});
        anim.setInterpolator(this.mInterpolator);
        anim.setDuration(375);
        anim.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                DragDownHelper.this.mDragDownCallback.setEmptyDragAmount(((Float) animation.getAnimatedValue()).floatValue());
            }
        });
        anim.start();
    }

    private void stopDragging() {
        if (this.mStartingChild != null) {
            cancelExpansion(this.mStartingChild);
        } else {
            cancelExpansion();
        }
        this.mDraggingDown = false;
        this.mDragDownCallback.onDragDownReset();
    }

    private ExpandableView findView(float x, float y) {
        this.mHost.getLocationOnScreen(this.mTemp2);
        return this.mCallback.getChildAtRawPosition(x + ((float) this.mTemp2[0]), y + ((float) this.mTemp2[1]));
    }
}
