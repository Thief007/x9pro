package com.mediatek.systemui.floatpanel;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.DragEvent;
import android.widget.HorizontalScrollView;

public class CustomizedHorizontalScrollView extends HorizontalScrollView {
    private static final SparseArray<String> DRAG_EVENT_ACTION = new SparseArray();
    private int mDragDirection;
    private float mDragWeight;
    private boolean mEdgeScrollForceStop;
    private Handler mHandler;
    private boolean mInDragState;
    private int mLastDraggingPositionX;
    private ScrollRunnable mScrollRunnable;
    private int mScrollerState;
    private int mSmoothScrollAmountAtEdge;
    private int mTouchSlot;

    private class ScrollRunnable implements Runnable {
        public void run() {
            int flag = CustomizedHorizontalScrollView.this.mDragDirection == 1 ? 1 : -1;
            Log.d("CustomizedHorizontalScrollView", "ScrollRunnable run, mDragDirection=" + CustomizedHorizontalScrollView.this.mDragDirection + ", falg=" + flag + ", mLastDraggingPositionX=" + CustomizedHorizontalScrollView.this.mLastDraggingPositionX + ", mScrollerState=" + CustomizedHorizontalScrollView.this.mScrollerState + ", mEdgeScrollForceStop=" + CustomizedHorizontalScrollView.this.mEdgeScrollForceStop + ",mInDragState = " + CustomizedHorizontalScrollView.this.mInDragState);
            int scrollDistance = (int) (((float) (CustomizedHorizontalScrollView.this.mSmoothScrollAmountAtEdge * flag)) + (CustomizedHorizontalScrollView.this.mDragWeight * CustomizedHorizontalScrollView.this.mDragWeight));
            if (CustomizedHorizontalScrollView.this.mScrollerState != 0) {
                CustomizedHorizontalScrollView.this.mScrollerState = 0;
                if (!CustomizedHorizontalScrollView.this.mEdgeScrollForceStop) {
                    Log.d("CustomizedHorizontalScrollView", "ScrollRunnable run smooth scroll scrollDistance=" + scrollDistance);
                    CustomizedHorizontalScrollView.this.smoothScrollBy(scrollDistance, 0);
                }
            } else {
                Log.d("CustomizedHorizontalScrollView", "ScrollRunnable run idle state, mScrollerState=" + CustomizedHorizontalScrollView.this.mScrollerState + ", mEdgeScrollForceStop=" + CustomizedHorizontalScrollView.this.mEdgeScrollForceStop);
            }
            if (CustomizedHorizontalScrollView.this.mInDragState) {
                CustomizedHorizontalScrollView.this.scrollIfNeeded(CustomizedHorizontalScrollView.this.mLastDraggingPositionX, CustomizedHorizontalScrollView.this.mLastDraggingPositionX);
            }
        }
    }

    public CustomizedHorizontalScrollView(Context context) {
        this(context, null);
    }

    public CustomizedHorizontalScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomizedHorizontalScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        DisplayMetrics metric = getResources().getDisplayMetrics();
        this.mSmoothScrollAmountAtEdge = (int) ((metric.density * 200.0f) + 0.5f);
        this.mTouchSlot = (int) ((metric.density * 75.0f) + 0.5f);
        this.mDragDirection = 0;
        this.mScrollerState = 0;
        this.mScrollRunnable = new ScrollRunnable();
        this.mHandler = new Handler();
        this.mInDragState = false;
    }

    public boolean dispatchDragEvent(DragEvent event) {
        int action = event.getAction();
        int x = Math.round(event.getX());
        Log.d("CustomizedHorizontalScrollView", "dispatchDragEvent event action:" + ((String) DRAG_EVENT_ACTION.get(action)) + ",x:" + x + ",y:" + Math.round(event.getY()));
        boolean scrolling = false;
        switch (action) {
            case 1:
                this.mLastDraggingPositionX = x;
                break;
            case 2:
                scrolling = scrollIfNeeded(this.mLastDraggingPositionX, x);
                this.mLastDraggingPositionX = x;
                break;
            case 4:
                this.mHandler.removeCallbacks(this.mScrollRunnable);
                this.mScrollerState = 0;
                break;
            case 5:
                this.mInDragState = true;
                this.mLastDraggingPositionX = x;
                break;
            case 6:
                this.mInDragState = false;
                this.mHandler.removeCallbacks(this.mScrollRunnable);
                this.mScrollerState = 0;
                break;
        }
        if (scrolling) {
            return true;
        }
        return super.dispatchDragEvent(event);
    }

    private boolean scrollIfNeeded(int oldX, int newX) {
        boolean z = true;
        Log.d("CustomizedHorizontalScrollView", "scrollIfNeeded oldX=" + oldX + ", newX=" + newX + ", container.left=" + getLeft() + ", container.right=" + getRight() + ", mScrollerState=" + this.mScrollerState + ", mTouchSlot=" + this.mTouchSlot + ", canScrollHorizontally(1)=" + canScrollHorizontally(1) + ", canScrollHorizontally(0)=" + canScrollHorizontally(0));
        if (newX >= 0) {
            if (newX >= oldX && this.mTouchSlot + newX >= getRight() && this.mScrollerState == 0) {
                Log.d("CustomizedHorizontalScrollView", "scrollIfNeeded scroll right");
                this.mScrollerState = 1;
                this.mDragDirection = 1;
                this.mHandler.postDelayed(this.mScrollRunnable, 250);
                return true;
            } else if (oldX < newX || newX - this.mTouchSlot > getLeft() || this.mScrollerState != 0) {
                if ((newX < oldX || this.mTouchSlot + newX < getRight()) && (oldX < newX || newX - this.mTouchSlot > getLeft())) {
                    this.mEdgeScrollForceStop = true;
                } else {
                    this.mEdgeScrollForceStop = false;
                }
                if (this.mDragDirection == 1) {
                    if (newX >= oldX) {
                        z = false;
                    } else if (newX + 10 >= oldX) {
                        z = false;
                    }
                    this.mEdgeScrollForceStop = z;
                } else if (this.mDragDirection == 0) {
                    if (newX <= oldX) {
                        z = false;
                    } else if (newX + 10 <= oldX) {
                        z = false;
                    }
                    this.mEdgeScrollForceStop = z;
                }
            } else {
                Log.d("CustomizedHorizontalScrollView", "scrollIfNeeded scroll left");
                this.mScrollerState = 1;
                this.mDragDirection = 0;
                this.mHandler.postDelayed(this.mScrollRunnable, 250);
                return true;
            }
        }
        return false;
    }

    static {
        DRAG_EVENT_ACTION.put(1, "ACTION_DRAG_STARTED");
        DRAG_EVENT_ACTION.put(5, "ACTION_DRAG_ENTERED");
        DRAG_EVENT_ACTION.put(2, "ACTION_DRAG_LOCATION");
        DRAG_EVENT_ACTION.put(6, "ACTION_DRAG_EXITED");
        DRAG_EVENT_ACTION.put(3, "ACTION_DROP");
        DRAG_EVENT_ACTION.put(4, "ACTION_DRAG_ENDED");
    }
}
