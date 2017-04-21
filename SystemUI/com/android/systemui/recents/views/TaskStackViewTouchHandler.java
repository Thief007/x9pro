package com.android.systemui.recents.views;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.assis.app.MAIN.EVENT;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.views.SwipeHelper.Callback;
import java.util.List;

class TaskStackViewTouchHandler implements Callback {
    static int INACTIVE_POINTER_ID = -1;
    int mActivePointerId = INACTIVE_POINTER_ID;
    TaskView mActiveTaskView = null;
    RecentsConfiguration mConfig;
    int mInitialMotionX;
    int mInitialMotionY;
    float mInitialP;
    boolean mInterceptedBySwipeHelper;
    boolean mIsScrolling;
    int mLastMotionX;
    int mLastMotionY;
    float mLastP;
    int mMaximumVelocity;
    int mMinimumVelocity;
    float mPagingTouchSlop;
    int mScrollTouchSlop;
    TaskStackViewScroller mScroller;
    TaskStackView mSv;
    SwipeHelper mSwipeHelper;
    float mTotalPMotion;
    VelocityTracker mVelocityTracker;
    final int mWindowTouchSlop;

    public TaskStackViewTouchHandler(Context context, TaskStackView sv, RecentsConfiguration config, TaskStackViewScroller scroller) {
        ViewConfiguration configuration = ViewConfiguration.get(context);
        this.mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        this.mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        this.mScrollTouchSlop = configuration.getScaledTouchSlop();
        this.mPagingTouchSlop = (float) configuration.getScaledPagingTouchSlop();
        this.mWindowTouchSlop = configuration.getScaledWindowTouchSlop();
        this.mSv = sv;
        this.mScroller = scroller;
        this.mConfig = config;
        this.mSwipeHelper = new SwipeHelper(0, this, context.getResources().getDisplayMetrics().density, this.mPagingTouchSlop);
        this.mSwipeHelper.setMinAlpha(1.0f);
    }

    void initOrResetVelocityTracker() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        } else {
            this.mVelocityTracker.clear();
        }
    }

    void initVelocityTrackerIfNotExists() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
    }

    void recycleVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    TaskView findViewAtPoint(int x, int y) {
        List<TaskView> taskViews = this.mSv.getTaskViews();
        for (int i = taskViews.size() - 1; i >= 0; i--) {
            TaskView tv = (TaskView) taskViews.get(i);
            if (tv.getVisibility() == 0 && this.mSv.isTransformedTouchPointInView((float) x, (float) y, tv)) {
                return tv;
            }
        }
        return null;
    }

    MotionEvent createMotionEventForStackScroll(MotionEvent ev) {
        MotionEvent pev = MotionEvent.obtainNoHistory(ev);
        pev.setLocation(0.0f, (float) this.mScroller.progressToScrollRange(this.mScroller.getStackScroll()));
        return pev;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!(this.mSv.getTaskViews().size() > 0)) {
            return false;
        }
        int action = ev.getAction();
        if (this.mConfig.multiStackEnabled && (action & 255) == 0 && !this.mSv.getTouchableRegion().contains((int) ev.getX(), (int) ev.getY())) {
            return false;
        }
        this.mInterceptedBySwipeHelper = this.mSwipeHelper.onInterceptTouchEvent(ev);
        if (this.mInterceptedBySwipeHelper) {
            return true;
        }
        boolean z;
        boolean isRunning = !this.mScroller.isScrolling() ? this.mScroller.mScrollAnimator != null ? this.mScroller.mScrollAnimator.isRunning() : false : true;
        switch (action & 255) {
            case 0:
                int x = (int) ev.getX();
                this.mLastMotionX = x;
                this.mInitialMotionX = x;
                x = (int) ev.getY();
                this.mLastMotionY = x;
                this.mInitialMotionY = x;
                float screenYToCurveProgress = this.mSv.mLayoutAlgorithm.screenYToCurveProgress(this.mLastMotionY);
                this.mLastP = screenYToCurveProgress;
                this.mInitialP = screenYToCurveProgress;
                this.mActivePointerId = ev.getPointerId(0);
                this.mActiveTaskView = findViewAtPoint(this.mLastMotionX, this.mLastMotionY);
                this.mScroller.stopScroller();
                this.mScroller.stopBoundScrollAnimation();
                initOrResetVelocityTracker();
                this.mVelocityTracker.addMovement(createMotionEventForStackScroll(ev));
                break;
            case 1:
            case 3:
                this.mScroller.animateBoundScroll();
                this.mIsScrolling = false;
                this.mActivePointerId = INACTIVE_POINTER_ID;
                this.mActiveTaskView = null;
                this.mTotalPMotion = 0.0f;
                recycleVelocityTracker();
                break;
            case 2:
                if (this.mActivePointerId != INACTIVE_POINTER_ID) {
                    initVelocityTrackerIfNotExists();
                    this.mVelocityTracker.addMovement(createMotionEventForStackScroll(ev));
                    int activePointerIndex = ev.findPointerIndex(this.mActivePointerId);
                    if (activePointerIndex >= 0) {
                        int y = (int) ev.getY(activePointerIndex);
                        int x2 = (int) ev.getX(activePointerIndex);
                        if (Math.abs(y - this.mInitialMotionY) > this.mScrollTouchSlop) {
                            this.mIsScrolling = true;
                            ViewParent parent = this.mSv.getParent();
                            if (parent != null) {
                                parent.requestDisallowInterceptTouchEvent(true);
                            }
                        }
                        this.mLastMotionX = x2;
                        this.mLastMotionY = y;
                        this.mLastP = this.mSv.mLayoutAlgorithm.screenYToCurveProgress(this.mLastMotionY);
                        break;
                    }
                    Log.d("TaskStackViewTouchHandler", "findPointerIndex failed");
                    this.mActivePointerId = INACTIVE_POINTER_ID;
                    break;
                }
                break;
            case 6:
                int pointerIndex = ev.getActionIndex();
                Log.d("TaskStackViewTouchHandler", "Ignore multi-touch " + pointerIndex + "(" + ev.getPointerId(pointerIndex) + ")");
                break;
        }
        if (isRunning) {
            z = true;
        } else {
            z = this.mIsScrolling;
        }
        return z;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (!(this.mSv.getTaskViews().size() > 0)) {
            return false;
        }
        int action = ev.getAction();
        if (this.mConfig.multiStackEnabled && (action & 255) == 0 && !this.mSv.getTouchableRegion().contains((int) ev.getX(), (int) ev.getY())) {
            return false;
        }
        if (this.mInterceptedBySwipeHelper && this.mSwipeHelper.onTouchEvent(ev)) {
            return true;
        }
        initVelocityTrackerIfNotExists();
        ViewParent parent;
        switch (action & 255) {
            case 0:
                int x = (int) ev.getX();
                this.mLastMotionX = x;
                this.mInitialMotionX = x;
                x = (int) ev.getY();
                this.mLastMotionY = x;
                this.mInitialMotionY = x;
                float screenYToCurveProgress = this.mSv.mLayoutAlgorithm.screenYToCurveProgress(this.mLastMotionY);
                this.mLastP = screenYToCurveProgress;
                this.mInitialP = screenYToCurveProgress;
                this.mActivePointerId = ev.getPointerId(0);
                this.mActiveTaskView = findViewAtPoint(this.mLastMotionX, this.mLastMotionY);
                this.mScroller.stopScroller();
                this.mScroller.stopBoundScrollAnimation();
                initOrResetVelocityTracker();
                this.mVelocityTracker.addMovement(createMotionEventForStackScroll(ev));
                parent = this.mSv.getParent();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                    break;
                }
                break;
            case 1:
                this.mVelocityTracker.computeCurrentVelocity(EVENT.DYNAMIC_PACK_EVENT_BASE, (float) this.mMaximumVelocity);
                int velocity = (int) this.mVelocityTracker.getYVelocity(this.mActivePointerId);
                if (this.mIsScrolling && Math.abs(velocity) > this.mMinimumVelocity) {
                    this.mScroller.mScroller.fling(0, this.mScroller.progressToScrollRange(this.mScroller.getStackScroll()), 0, velocity, 0, 0, this.mScroller.progressToScrollRange(this.mSv.mLayoutAlgorithm.mMinScrollP), this.mScroller.progressToScrollRange(this.mSv.mLayoutAlgorithm.mMaxScrollP), 0, ((int) (Math.min(1.0f, Math.abs(((float) velocity) / ((float) this.mMaximumVelocity))) * 96.0f)) + 32);
                    this.mSv.invalidate();
                } else if (this.mIsScrolling && this.mScroller.isScrollOutOfBounds()) {
                    this.mScroller.animateBoundScroll();
                } else if (this.mActiveTaskView == null) {
                    maybeHideRecentsFromBackgroundTap((int) ev.getX(), (int) ev.getY());
                }
                this.mActivePointerId = INACTIVE_POINTER_ID;
                this.mIsScrolling = false;
                this.mTotalPMotion = 0.0f;
                recycleVelocityTracker();
                break;
            case 2:
                if (this.mActivePointerId != INACTIVE_POINTER_ID) {
                    this.mVelocityTracker.addMovement(createMotionEventForStackScroll(ev));
                    int activePointerIndex = ev.findPointerIndex(this.mActivePointerId);
                    int x2 = (int) ev.getX(activePointerIndex);
                    int y = (int) ev.getY(activePointerIndex);
                    int yTotal = Math.abs(y - this.mInitialMotionY);
                    float deltaP = this.mLastP - this.mSv.mLayoutAlgorithm.screenYToCurveProgress(y);
                    if (!this.mIsScrolling && yTotal > this.mScrollTouchSlop) {
                        this.mIsScrolling = true;
                        parent = this.mSv.getParent();
                        if (parent != null) {
                            parent.requestDisallowInterceptTouchEvent(true);
                        }
                    }
                    if (this.mIsScrolling) {
                        float curStackScroll = this.mScroller.getStackScroll();
                        float overScrollAmount = this.mScroller.getScrollAmountOutOfBounds(curStackScroll + deltaP);
                        if (Float.compare(overScrollAmount, 0.0f) != 0) {
                            float maxOverScroll = this.mConfig.taskStackOverscrollPct;
                            deltaP *= 1.0f - (Math.min(maxOverScroll, overScrollAmount) / maxOverScroll);
                        }
                        this.mScroller.setStackScroll(curStackScroll + deltaP);
                    }
                    this.mLastMotionX = x2;
                    this.mLastMotionY = y;
                    this.mLastP = this.mSv.mLayoutAlgorithm.screenYToCurveProgress(this.mLastMotionY);
                    this.mTotalPMotion += Math.abs(deltaP);
                    break;
                }
                break;
            case 3:
                if (this.mScroller.isScrollOutOfBounds()) {
                    this.mScroller.animateBoundScroll();
                }
                this.mActivePointerId = INACTIVE_POINTER_ID;
                this.mIsScrolling = false;
                this.mTotalPMotion = 0.0f;
                recycleVelocityTracker();
                break;
            case 5:
                int index = ev.getActionIndex();
                this.mActivePointerId = ev.getPointerId(index);
                this.mLastMotionX = (int) ev.getX(index);
                this.mLastMotionY = (int) ev.getY(index);
                this.mLastP = this.mSv.mLayoutAlgorithm.screenYToCurveProgress(this.mLastMotionY);
                break;
            case 6:
                int pointerIndex = ev.getActionIndex();
                if (ev.getPointerId(pointerIndex) == this.mActivePointerId) {
                    int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    this.mActivePointerId = ev.getPointerId(newPointerIndex);
                    this.mLastMotionX = (int) ev.getX(newPointerIndex);
                    this.mLastMotionY = (int) ev.getY(newPointerIndex);
                    this.mLastP = this.mSv.mLayoutAlgorithm.screenYToCurveProgress(this.mLastMotionY);
                    this.mVelocityTracker.clear();
                    break;
                }
                break;
        }
        return true;
    }

    void maybeHideRecentsFromBackgroundTap(int x, int y) {
        int dx = Math.abs(this.mInitialMotionX - x);
        int dy = Math.abs(this.mInitialMotionY - y);
        if (dx <= this.mScrollTouchSlop && dy <= this.mScrollTouchSlop) {
            int shiftedX = x;
            if (x > this.mSv.getTouchableRegion().centerX()) {
                shiftedX -= this.mWindowTouchSlop;
            } else {
                shiftedX += this.mWindowTouchSlop;
            }
            if (findViewAtPoint(shiftedX, y) == null) {
                Recents.getInstanceAndStartIfNeeded(this.mSv.getContext()).hideRecents(false, true);
            }
        }
    }

    public boolean onGenericMotionEvent(MotionEvent ev) {
        if ((ev.getSource() & 2) == 2) {
            switch (ev.getAction() & 255) {
                case 8:
                    if (ev.getAxisValue(9) > 0.0f) {
                        if (this.mSv.ensureFocusedTask(true)) {
                            this.mSv.focusNextTask(true, false);
                        }
                    } else if (this.mSv.ensureFocusedTask(true)) {
                        this.mSv.focusNextTask(false, false);
                    }
                    return true;
            }
        }
        return false;
    }

    public View getChildAtPosition(MotionEvent ev) {
        return findViewAtPoint((int) ev.getX(), (int) ev.getY());
    }

    public boolean canChildBeDismissed(View v) {
        return true;
    }

    public void onBeginDrag(View v) {
        TaskView tv = (TaskView) v;
        tv.setClipViewInStack(false);
        tv.setTouchEnabled(false);
        ViewParent parent = this.mSv.getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
        this.mSv.hideDismissAllButton(null);
    }

    public void onSwipeChanged(View v, float delta) {
    }

    public void onChildDismissed(View v) {
        TaskView tv = (TaskView) v;
        tv.setClipViewInStack(true);
        tv.setTouchEnabled(true);
        this.mSv.onTaskViewDismissed(tv);
        MetricsLogger.histogram(tv.getContext(), "overview_task_dismissed_source", 1);
    }

    public void onSnapBackCompleted(View v) {
        TaskView tv = (TaskView) v;
        tv.setClipViewInStack(true);
        tv.setTouchEnabled(true);
        this.mSv.showDismissAllButton();
    }

    public void onDragCancelled(View v) {
    }
}
