package com.android.systemui.recents.views;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.DozeTrigger;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.RecentsPackageMonitor;
import com.android.systemui.recents.model.RecentsPackageMonitor.PackageCallbacks;
import com.android.systemui.recents.model.RecentsTaskLoader;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.model.TaskStack.TaskStackCallbacks;
import com.android.systemui.recents.views.TaskStackViewLayoutAlgorithm.VisibilityReport;
import com.android.systemui.recents.views.TaskStackViewScroller.TaskStackViewScrollerCallbacks;
import com.android.systemui.recents.views.ViewPool.ViewPoolConsumer;
import com.android.systemui.statusbar.DismissView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class TaskStackView extends FrameLayout implements TaskStackCallbacks, TaskViewCallbacks, TaskStackViewScrollerCallbacks, ViewPoolConsumer<TaskView, Task>, PackageCallbacks {
    int mAllTaskCount = 0;
    boolean mAwaitingFirstLayout = true;
    TaskStackViewCallbacks mCb;
    int mClearTaskCount = 0;
    RecentsConfiguration mConfig;
    ArrayList<TaskViewTransform> mCurrentTaskTransforms = new ArrayList();
    DebugOverlayView mDebugOverlay;
    DismissView mDismissAllButton;
    boolean mDismissAllButtonAnimating;
    TaskStackViewFilterAlgorithm mFilterAlgorithm;
    int mFocusedTaskIndex = -1;
    List<TaskView> mImmutableTaskViews = new ArrayList();
    LayoutInflater mInflater;
    boolean mLayersDisabled;
    TaskStackViewLayoutAlgorithm mLayoutAlgorithm;
    int mNumTasks = 0;
    int mPrevAccessibilityFocusedIndex = -1;
    AnimatorUpdateListener mRequestUpdateClippingListener = new AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator animation) {
            TaskStackView.this.requestUpdateStackViewsClip();
        }
    };
    TaskStack mStack;
    TaskStackViewScroller mStackScroller;
    int mStackViewsAnimationDuration;
    boolean mStackViewsClipDirty = true;
    boolean mStackViewsDirty = true;
    boolean mStartEnterAnimationCompleted;
    ViewAnimation$TaskViewEnterContext mStartEnterAnimationContext;
    boolean mStartEnterAnimationRequestedAfterLayout;
    Rect mTaskStackBounds = new Rect();
    ArrayList<TaskView> mTaskViews = new ArrayList();
    float[] mTmpCoord = new float[2];
    Matrix mTmpMatrix = new Matrix();
    Rect mTmpRect = new Rect();
    HashMap<Task, TaskView> mTmpTaskViewMap = new HashMap();
    TaskViewTransform mTmpTransform = new TaskViewTransform();
    int[] mTmpVisibleRange = new int[2];
    TaskStackViewTouchHandler mTouchHandler;
    DozeTrigger mUIDozeTrigger;
    ViewPool<TaskView, Task> mViewPool;
    private MyHandler myHandler;

    interface TaskStackViewCallbacks {
        void onAllTaskViewsDismissed(ArrayList<Task> arrayList);

        void onTaskResize(Task task);

        void onTaskViewAppInfoClicked(Task task);

        void onTaskViewClicked(TaskStackView taskStackView, TaskView taskView, TaskStack taskStack, Task task, boolean z);

        void onTaskViewDismissed(Task task);
    }

    class MyHandler extends Handler {
        MyHandler() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    TaskStackView taskStackView = TaskStackView.this;
                    taskStackView.mAllTaskCount--;
                    TaskStackView.this.resetFocusedTask();
                    TaskStackView.this.dismissForntTask(TaskStackView.this.mAllTaskCount, true, false);
                    taskStackView = TaskStackView.this;
                    taskStackView.mClearTaskCount++;
                    if (TaskStackView.this.mClearTaskCount >= TaskStackView.this.mNumTasks) {
                        TaskStackView.this.removeTask();
                        TaskStackView.this.mCb.onAllTaskViewsDismissed(null);
                        break;
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    }

    public TaskStackView(Context context, TaskStack stack) {
        super(context);
        setStack(stack);
        this.mConfig = RecentsConfiguration.getInstance();
        this.mViewPool = new ViewPool(context, this);
        this.mInflater = LayoutInflater.from(context);
        this.mLayoutAlgorithm = new TaskStackViewLayoutAlgorithm(this.mConfig);
        this.mFilterAlgorithm = new TaskStackViewFilterAlgorithm(this.mConfig, this, this.mViewPool);
        this.mStackScroller = new TaskStackViewScroller(context, this.mConfig, this.mLayoutAlgorithm);
        this.mStackScroller.setCallbacks(this);
        this.mTouchHandler = new TaskStackViewTouchHandler(context, this, this.mConfig, this.mStackScroller);
        this.mUIDozeTrigger = new DozeTrigger(this.mConfig.taskBarDismissDozeDelaySeconds, new Runnable() {
            public void run() {
                List<TaskView> taskViews = TaskStackView.this.getTaskViews();
                int taskViewCount = taskViews.size();
                for (int i = 0; i < taskViewCount; i++) {
                    ((TaskView) taskViews.get(i)).startNoUserInteractionAnimation();
                }
            }
        });
        setImportantForAccessibility(1);
    }

    void setCallbacks(TaskStackViewCallbacks cb) {
        this.mCb = cb;
    }

    void setStack(TaskStack stack) {
        this.mStack = stack;
        if (this.mStack != null) {
            this.mStack.setCallbacks(this);
        }
        requestLayout();
    }

    TaskStack getStack() {
        return this.mStack;
    }

    public void setDebugOverlay(DebugOverlayView overlay) {
        this.mDebugOverlay = overlay;
    }

    void updateTaskViewsList() {
        this.mTaskViews.clear();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View v = getChildAt(i);
            if (v instanceof TaskView) {
                this.mTaskViews.add((TaskView) v);
            }
        }
        this.mImmutableTaskViews = Collections.unmodifiableList(this.mTaskViews);
    }

    List<TaskView> getTaskViews() {
        return this.mImmutableTaskViews;
    }

    void reset() {
        resetFocusedTask();
        List<TaskView> taskViews = getTaskViews();
        for (int i = taskViews.size() - 1; i >= 0; i--) {
            this.mViewPool.returnViewToPool((TaskView) taskViews.get(i));
        }
        if (this.mViewPool != null) {
            Iterator<TaskView> iter = this.mViewPool.poolViewIterator();
            if (iter != null) {
                while (iter.hasNext()) {
                    ((TaskView) iter.next()).reset();
                }
            }
        }
        this.mStack.reset();
        this.mStackViewsDirty = true;
        this.mStackViewsClipDirty = true;
        this.mAwaitingFirstLayout = true;
        this.mPrevAccessibilityFocusedIndex = -1;
        if (this.mUIDozeTrigger != null) {
            this.mUIDozeTrigger.stopDozing();
            this.mUIDozeTrigger.resetTrigger();
        }
        this.mStackScroller.reset();
    }

    void requestSynchronizeStackViewsWithModel() {
        requestSynchronizeStackViewsWithModel(0);
    }

    void requestSynchronizeStackViewsWithModel(int duration) {
        if (!this.mStackViewsDirty) {
            invalidate();
            this.mStackViewsDirty = true;
        }
        if (this.mAwaitingFirstLayout) {
            this.mStackViewsAnimationDuration = 0;
        } else {
            this.mStackViewsAnimationDuration = Math.max(this.mStackViewsAnimationDuration, duration);
        }
    }

    void requestUpdateStackViewsClip() {
        if (!this.mStackViewsClipDirty) {
            invalidate();
            this.mStackViewsClipDirty = true;
        }
    }

    public TaskView getChildViewForTask(Task t) {
        List<TaskView> taskViews = getTaskViews();
        int taskViewCount = taskViews.size();
        for (int i = 0; i < taskViewCount; i++) {
            TaskView tv = (TaskView) taskViews.get(i);
            if (tv.getTask() == t) {
                return tv;
            }
        }
        return null;
    }

    public TaskStackViewLayoutAlgorithm getStackAlgorithm() {
        return this.mLayoutAlgorithm;
    }

    private boolean updateStackTransforms(ArrayList<TaskViewTransform> taskTransforms, ArrayList<Task> tasks, float stackScroll, int[] visibleRangeOut, boolean boundTranslationsToRect) {
        int i;
        int taskTransformCount = taskTransforms.size();
        int taskCount = tasks.size();
        int frontMostVisibleIndex = -1;
        int backMostVisibleIndex = -1;
        if (taskTransformCount < taskCount) {
            for (i = taskTransformCount; i < taskCount; i++) {
                taskTransforms.add(new TaskViewTransform());
            }
        } else if (taskTransformCount > taskCount) {
            taskTransforms.subList(0, taskCount);
        }
        TaskViewTransform prevTransform = null;
        i = taskCount - 1;
        while (i >= 0) {
            TaskViewTransform transform = this.mLayoutAlgorithm.getStackTransform((Task) tasks.get(i), stackScroll, (TaskViewTransform) taskTransforms.get(i), prevTransform);
            if (transform.visible) {
                if (frontMostVisibleIndex < 0) {
                    frontMostVisibleIndex = i;
                }
                backMostVisibleIndex = i;
            } else if (backMostVisibleIndex != -1) {
                while (i >= 0) {
                    ((TaskViewTransform) taskTransforms.get(i)).reset();
                    i--;
                }
                if (visibleRangeOut != null) {
                    visibleRangeOut[0] = frontMostVisibleIndex;
                    visibleRangeOut[1] = backMostVisibleIndex;
                }
                return frontMostVisibleIndex == -1 && backMostVisibleIndex != -1;
            }
            if (boundTranslationsToRect) {
                transform.translationY = Math.min(transform.translationY, this.mLayoutAlgorithm.mViewRect.bottom);
            }
            prevTransform = transform;
            i--;
        }
        if (visibleRangeOut != null) {
            visibleRangeOut[0] = frontMostVisibleIndex;
            visibleRangeOut[1] = backMostVisibleIndex;
        }
        if (frontMostVisibleIndex == -1) {
        }
    }

    boolean synchronizeStackViewsWithModel() {
        if (!this.mStackViewsDirty) {
            return false;
        }
        SystemServicesProxy ssp = RecentsTaskLoader.getInstance().getSystemServicesProxy();
        ArrayList<Task> tasks = this.mStack.getTasks();
        float stackScroll = this.mStackScroller.getStackScroll();
        int[] visibleRange = this.mTmpVisibleRange;
        boolean isValidVisibleRange = updateStackTransforms(this.mCurrentTaskTransforms, tasks, stackScroll, visibleRange, false);
        if (this.mDebugOverlay != null) {
            this.mDebugOverlay.setText("vis[" + visibleRange[1] + "-" + visibleRange[0] + "]");
        }
        this.mTmpTaskViewMap.clear();
        List<TaskView> taskViews = getTaskViews();
        int reaquireAccessibilityFocus = 0;
        int i = taskViews.size() - 1;
        while (i >= 0) {
            TaskView tv = (TaskView) taskViews.get(i);
            Task task = tv.getTask();
            int taskIndex = this.mStack.indexOfTask(task);
            if (visibleRange[1] > taskIndex || taskIndex > visibleRange[0]) {
                this.mViewPool.returnViewToPool(tv);
                reaquireAccessibilityFocus |= i == this.mPrevAccessibilityFocusedIndex ? 1 : 0;
                if (task == this.mStack.getFrontMostTask()) {
                    hideDismissAllButton(null);
                }
            } else {
                this.mTmpTaskViewMap.put(task, tv);
            }
            i--;
        }
        i = visibleRange[0];
        while (isValidVisibleRange && i >= visibleRange[1]) {
            task = (Task) tasks.get(i);
            TaskViewTransform transform = (TaskViewTransform) this.mCurrentTaskTransforms.get(i);
            tv = (TaskView) this.mTmpTaskViewMap.get(task);
            taskIndex = this.mStack.indexOfTask(task);
            if (tv == null) {
                tv = (TaskView) this.mViewPool.pickUpViewFromPool(task, task);
                if (this.mLayersDisabled) {
                    tv.disableLayersForOneFrame();
                }
                if (this.mStackViewsAnimationDuration > 0) {
                    if (Float.compare(transform.p, 0.0f) <= 0) {
                        this.mLayoutAlgorithm.getStackTransform(0.0f, 0.0f, this.mTmpTransform, null);
                    } else {
                        this.mLayoutAlgorithm.getStackTransform(1.0f, 0.0f, this.mTmpTransform, null);
                    }
                    tv.updateViewPropertiesToTaskTransform(this.mTmpTransform, 0);
                }
                if (!this.mAwaitingFirstLayout && task == this.mStack.getFrontMostTask()) {
                    showDismissAllButton();
                }
            }
            tv.updateViewPropertiesToTaskTransform((TaskViewTransform) this.mCurrentTaskTransforms.get(taskIndex), this.mStackViewsAnimationDuration, this.mRequestUpdateClippingListener);
            if (reaquireAccessibilityFocus != 0) {
                taskViews = getTaskViews();
                int taskViewCount = taskViews.size();
                if (taskViewCount > 0 && ssp.isTouchExplorationEnabled() && this.mPrevAccessibilityFocusedIndex != -1) {
                    int indexOfTask = this.mStack.indexOfTask(((TaskView) taskViews.get(taskViewCount - 1)).getTask());
                    if (this.mPrevAccessibilityFocusedIndex != indexOfTask) {
                        tv.requestAccessibilityFocus();
                        this.mPrevAccessibilityFocusedIndex = indexOfTask;
                    }
                }
            }
            i--;
        }
        this.mStackViewsAnimationDuration = 0;
        this.mStackViewsDirty = false;
        this.mStackViewsClipDirty = true;
        return true;
    }

    void clipTaskViews() {
        List<TaskView> taskViews = getTaskViews();
        int taskViewCount = taskViews.size();
        for (int i = 0; i < taskViewCount - 1; i++) {
            TaskView tv = (TaskView) taskViews.get(i);
            View nextTv = null;
            int clipBottom = 0;
            if (tv.shouldClipViewInStack()) {
                int nextIndex = i;
                while (nextIndex < taskViewCount - 1) {
                    nextIndex++;
                    View tmpTv = (TaskView) taskViews.get(nextIndex);
                    if (tmpTv != null && tmpTv.shouldClipViewInStack()) {
                        nextTv = tmpTv;
                        break;
                    }
                }
                if (nextTv != null) {
                    float[] fArr = this.mTmpCoord;
                    this.mTmpCoord[1] = 0.0f;
                    fArr[0] = 0.0f;
                    Utilities.mapCoordInDescendentToSelf(nextTv, this, this.mTmpCoord, false);
                    Utilities.mapCoordInSelfToDescendent(tv, this, this.mTmpCoord, this.mTmpMatrix);
                    clipBottom = (int) Math.floor((double) (((((float) tv.getMeasuredHeight()) - this.mTmpCoord[1]) - ((float) nextTv.getPaddingTop())) - 1.0f));
                }
            }
            tv.getViewBounds().setClipBottom(clipBottom);
        }
        if (taskViewCount > 0) {
            ((TaskView) taskViews.get(taskViewCount - 1)).getViewBounds().setClipBottom(0);
        }
        this.mStackViewsClipDirty = false;
    }

    public void setStackInsetRect(Rect r) {
        this.mTaskStackBounds.set(r);
    }

    void updateMinMaxScroll(boolean boundScrollToNewMinMax, boolean launchedWithAltTab, boolean launchedFromHome) {
        this.mLayoutAlgorithm.computeMinMaxScroll(this.mStack.getTasks(), launchedWithAltTab, launchedFromHome);
        if (boundScrollToNewMinMax) {
            this.mStackScroller.boundScroll();
        }
    }

    public TaskStackViewScroller getScroller() {
        return this.mStackScroller;
    }

    void focusTask(int taskIndex, boolean scrollToNewPosition, final boolean animateFocusedState) {
        if (taskIndex != this.mFocusedTaskIndex && taskIndex >= 0 && taskIndex < this.mStack.getTaskCount()) {
            this.mFocusedTaskIndex = taskIndex;
            this.mPrevAccessibilityFocusedIndex = taskIndex;
            final Task t = (Task) this.mStack.getTasks().get(this.mFocusedTaskIndex);
            Runnable postScrollRunnable = new Runnable() {
                public void run() {
                    TaskView tv = TaskStackView.this.getChildViewForTask(t);
                    if (tv != null) {
                        tv.setFocusedTask(animateFocusedState);
                        tv.requestAccessibilityFocus();
                    }
                }
            };
            if (scrollToNewPosition) {
                this.mStackScroller.animateScroll(this.mStackScroller.getStackScroll(), this.mStackScroller.getBoundedStackScroll(this.mLayoutAlgorithm.getStackScrollForTask(t) - 0.5f), postScrollRunnable);
            } else if (postScrollRunnable != null) {
                postScrollRunnable.run();
            }
        }
    }

    public boolean ensureFocusedTask(boolean findClosestToCenter) {
        if (this.mFocusedTaskIndex < 0) {
            List<TaskView> taskViews = getTaskViews();
            int taskViewCount = taskViews.size();
            if (findClosestToCenter) {
                int x = this.mLayoutAlgorithm.mStackVisibleRect.centerX();
                int y = this.mLayoutAlgorithm.mStackVisibleRect.centerY();
                for (int i = taskViewCount - 1; i >= 0; i--) {
                    TaskView tv = (TaskView) taskViews.get(i);
                    tv.getHitRect(this.mTmpRect);
                    if (this.mTmpRect.contains(x, y)) {
                        this.mFocusedTaskIndex = this.mStack.indexOfTask(tv.getTask());
                        this.mPrevAccessibilityFocusedIndex = this.mFocusedTaskIndex;
                        break;
                    }
                }
            }
            if (this.mFocusedTaskIndex < 0 && taskViewCount > 0) {
                this.mFocusedTaskIndex = this.mStack.indexOfTask(((TaskView) taskViews.get(taskViewCount - 1)).getTask());
                this.mPrevAccessibilityFocusedIndex = this.mFocusedTaskIndex;
            }
        }
        if (this.mFocusedTaskIndex >= 0) {
            return true;
        }
        return false;
    }

    public void focusNextTask(boolean forward, boolean animateFocusedState) {
        int numTasks = this.mStack.getTaskCount();
        if (numTasks != 0) {
            int newIndex = this.mFocusedTaskIndex + (forward ? -1 : 1);
            if (newIndex >= 0 && newIndex <= numTasks - 1) {
                focusTask(Math.max(0, Math.min(numTasks - 1, newIndex)), true, animateFocusedState);
            }
        }
    }

    public void dismissFocusedTask() {
        if (this.mFocusedTaskIndex < 0 || this.mFocusedTaskIndex >= this.mStack.getTaskCount()) {
            this.mFocusedTaskIndex = -1;
        } else {
            getChildViewForTask((Task) this.mStack.getTasks().get(this.mFocusedTaskIndex)).dismissTask();
        }
    }

    void resetFocusedTask() {
        if (this.mFocusedTaskIndex >= 0 && this.mFocusedTaskIndex < this.mStack.getTaskCount()) {
            TaskView tv = getChildViewForTask((Task) this.mStack.getTasks().get(this.mFocusedTaskIndex));
            if (tv != null) {
                tv.unsetFocusedTask();
            }
        }
        this.mFocusedTaskIndex = -1;
        this.mPrevAccessibilityFocusedIndex = -1;
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        List<TaskView> taskViews = getTaskViews();
        int taskViewCount = taskViews.size();
        if (taskViewCount > 0) {
            TaskView frontMostTask = (TaskView) taskViews.get(taskViewCount - 1);
            event.setFromIndex(this.mStack.indexOfTask(((TaskView) taskViews.get(0)).getTask()));
            event.setToIndex(this.mStack.indexOfTask(frontMostTask.getTask()));
            event.setContentDescription(frontMostTask.getTask().activityLabel);
        }
        event.setItemCount(this.mStack.getTaskCount());
        event.setScrollY(this.mStackScroller.mScroller.getCurrY());
        event.setMaxScrollY(this.mStackScroller.progressToScrollRange(this.mLayoutAlgorithm.mMaxScrollP));
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        if (getTaskViews().size() > 1 && this.mPrevAccessibilityFocusedIndex != -1) {
            info.setScrollable(true);
            if (this.mPrevAccessibilityFocusedIndex > 0) {
                info.addAction(4096);
            }
            if (this.mPrevAccessibilityFocusedIndex < this.mStack.getTaskCount() - 1) {
                info.addAction(8192);
            }
        }
    }

    public CharSequence getAccessibilityClassName() {
        return TaskStackView.class.getName();
    }

    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (super.performAccessibilityAction(action, arguments)) {
            return true;
        }
        if (ensureFocusedTask(false)) {
            switch (action) {
                case 4096:
                    if (this.mPrevAccessibilityFocusedIndex > 0) {
                        focusNextTask(true, false);
                        return true;
                    }
                    break;
                case 8192:
                    if (this.mPrevAccessibilityFocusedIndex < this.mStack.getTaskCount() - 1) {
                        focusNextTask(false, false);
                        return true;
                    }
                    break;
            }
        }
        return false;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return this.mTouchHandler.onInterceptTouchEvent(ev);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        return this.mTouchHandler.onTouchEvent(ev);
    }

    public boolean onGenericMotionEvent(MotionEvent ev) {
        return this.mTouchHandler.onGenericMotionEvent(ev);
    }

    Rect getTouchableRegion() {
        return this.mTaskStackBounds;
    }

    public void computeScroll() {
        this.mStackScroller.computeScroll();
        synchronizeStackViewsWithModel();
        clipTaskViews();
        updateDismissButtonPosition();
        sendAccessibilityEvent(4096);
    }

    public void computeRects(int windowWidth, int windowHeight, Rect taskStackBounds, boolean launchedWithAltTab, boolean launchedFromHome) {
        this.mLayoutAlgorithm.computeRects(windowWidth, windowHeight, taskStackBounds);
        updateMinMaxScroll(false, launchedWithAltTab, launchedFromHome);
    }

    public void updateMinMaxScrollForStack(TaskStack stack, boolean launchedWithAltTab, boolean launchedFromHome) {
        this.mStack = stack;
        updateMinMaxScroll(false, launchedWithAltTab, launchedFromHome);
    }

    public VisibilityReport computeStackVisibilityReport() {
        return this.mLayoutAlgorithm.computeStackVisibilityReport(this.mStack.getTasks());
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        Rect taskStackBounds = new Rect(this.mTaskStackBounds);
        taskStackBounds.bottom -= this.mConfig.systemInsets.bottom;
        computeRects(width, height, taskStackBounds, this.mConfig.launchedWithAltTab, this.mConfig.launchedFromHome);
        if (this.mAwaitingFirstLayout) {
            this.mStackScroller.setStackScrollToInitialState();
            requestSynchronizeStackViewsWithModel();
            synchronizeStackViewsWithModel();
        }
        List<TaskView> taskViews = getTaskViews();
        int taskViewCount = taskViews.size();
        for (int i = 0; i < taskViewCount; i++) {
            TaskView tv = (TaskView) taskViews.get(i);
            if (tv.getBackground() != null) {
                tv.getBackground().getPadding(this.mTmpRect);
            } else {
                this.mTmpRect.setEmpty();
            }
            tv.measure(MeasureSpec.makeMeasureSpec((this.mLayoutAlgorithm.mTaskRect.width() + this.mTmpRect.left) + this.mTmpRect.right, 1073741824), MeasureSpec.makeMeasureSpec((this.mLayoutAlgorithm.mTaskRect.height() + this.mTmpRect.top) + this.mTmpRect.bottom, 1073741824));
        }
        if (this.mDismissAllButton != null) {
            this.mDismissAllButton.measure(MeasureSpec.makeMeasureSpec(this.mLayoutAlgorithm.mTaskRect.width(), 1073741824), MeasureSpec.makeMeasureSpec(this.mConfig.dismissAllButtonSizePx, 1073741824));
        }
        setMeasuredDimension(width, height);
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        List<TaskView> taskViews = getTaskViews();
        int taskViewCount = taskViews.size();
        for (int i = 0; i < taskViewCount; i++) {
            TaskView tv = (TaskView) taskViews.get(i);
            if (tv.getBackground() != null) {
                tv.getBackground().getPadding(this.mTmpRect);
            } else {
                this.mTmpRect.setEmpty();
            }
            tv.layout(this.mLayoutAlgorithm.mTaskRect.left - this.mTmpRect.left, this.mLayoutAlgorithm.mTaskRect.top - this.mTmpRect.top, this.mLayoutAlgorithm.mTaskRect.right + this.mTmpRect.right, this.mLayoutAlgorithm.mTaskRect.bottom + this.mTmpRect.bottom);
        }
        if (this.mDismissAllButton != null) {
            this.mDismissAllButton.layout(this.mLayoutAlgorithm.mTaskRect.left, 0, this.mLayoutAlgorithm.mTaskRect.left + this.mDismissAllButton.getMeasuredWidth(), this.mDismissAllButton.getMeasuredHeight());
        }
        if (this.mAwaitingFirstLayout) {
            this.mAwaitingFirstLayout = false;
            onFirstLayout();
        }
    }

    void onFirstLayout() {
        int i;
        int offscreenY = this.mLayoutAlgorithm.mViewRect.bottom - (this.mLayoutAlgorithm.mTaskRect.top - this.mLayoutAlgorithm.mViewRect.top);
        Task launchTargetTask = null;
        List<TaskView> taskViews = getTaskViews();
        int taskViewCount = taskViews.size();
        for (i = taskViewCount - 1; i >= 0; i--) {
            Task task = ((TaskView) taskViews.get(i)).getTask();
            if (task.isLaunchTarget) {
                launchTargetTask = task;
                break;
            }
        }
        for (i = taskViewCount - 1; i >= 0; i--) {
            boolean isTaskAboveTask;
            TaskView tv = (TaskView) taskViews.get(i);
            task = tv.getTask();
            if (launchTargetTask != null) {
                isTaskAboveTask = launchTargetTask.group.isTaskAboveTask(task, launchTargetTask);
            } else {
                isTaskAboveTask = false;
            }
            tv.prepareEnterRecentsAnimation(task.isLaunchTarget, isTaskAboveTask, offscreenY);
        }
        if (this.mStartEnterAnimationRequestedAfterLayout) {
            startEnterRecentsAnimation(this.mStartEnterAnimationContext);
            this.mStartEnterAnimationRequestedAfterLayout = false;
            this.mStartEnterAnimationContext = null;
        }
        if (this.mConfig.launchedWithAltTab) {
            if (this.mConfig.launchedFromAppWithThumbnail) {
                focusTask(Math.max(0, this.mStack.getTaskCount() - 2), false, this.mConfig.launchedHasConfigurationChanged);
            } else {
                focusTask(Math.max(0, this.mStack.getTaskCount() - 1), false, this.mConfig.launchedHasConfigurationChanged);
            }
        }
        if (!this.mConfig.multiStackEnabled) {
            this.mUIDozeTrigger.startDozing();
        }
    }

    public void startEnterRecentsAnimation(ViewAnimation$TaskViewEnterContext ctx) {
        if (this.mAwaitingFirstLayout) {
            this.mStartEnterAnimationRequestedAfterLayout = true;
            this.mStartEnterAnimationContext = ctx;
            return;
        }
        if (this.mStack.getTaskCount() > 0) {
            int i;
            Task task;
            Task launchTargetTask = null;
            List<TaskView> taskViews = getTaskViews();
            int taskViewCount = taskViews.size();
            for (i = taskViewCount - 1; i >= 0; i--) {
                task = ((TaskView) taskViews.get(i)).getTask();
                if (task.isLaunchTarget) {
                    launchTargetTask = task;
                    break;
                }
            }
            for (i = taskViewCount - 1; i >= 0; i--) {
                boolean isTaskAboveTask;
                TaskView tv = (TaskView) taskViews.get(i);
                task = tv.getTask();
                ctx.currentTaskTransform = new TaskViewTransform();
                ctx.currentStackViewIndex = i;
                ctx.currentStackViewCount = taskViewCount;
                ctx.currentTaskRect = this.mLayoutAlgorithm.mTaskRect;
                if (launchTargetTask != null) {
                    isTaskAboveTask = launchTargetTask.group.isTaskAboveTask(task, launchTargetTask);
                } else {
                    isTaskAboveTask = false;
                }
                ctx.currentTaskOccludesLaunchTarget = isTaskAboveTask;
                ctx.updateListener = this.mRequestUpdateClippingListener;
                this.mLayoutAlgorithm.getStackTransform(task, this.mStackScroller.getStackScroll(), ctx.currentTaskTransform, null);
                tv.startEnterRecentsAnimation(ctx);
            }
            ctx.postAnimationTrigger.addLastDecrementRunnable(new Runnable() {
                public void run() {
                    TaskStackView.this.mStartEnterAnimationCompleted = true;
                    TaskStackView.this.mUIDozeTrigger.poke();
                    SystemServicesProxy ssp = RecentsTaskLoader.getInstance().getSystemServicesProxy();
                    List<TaskView> taskViews = TaskStackView.this.getTaskViews();
                    int taskViewCount = taskViews.size();
                    if (taskViewCount > 0 && ssp.isTouchExplorationEnabled()) {
                        TaskView tv = (TaskView) taskViews.get(taskViewCount - 1);
                        tv.requestAccessibilityFocus();
                        TaskStackView.this.mPrevAccessibilityFocusedIndex = TaskStackView.this.mStack.indexOfTask(tv.getTask());
                    }
                    ArrayList<Task> tasks = TaskStackView.this.mStack.getTasks();
                    if (TaskStackView.this.mConfig.launchedWithAltTab && !TaskStackView.this.mConfig.launchedHasConfigurationChanged && TaskStackView.this.mFocusedTaskIndex >= 0 && TaskStackView.this.mFocusedTaskIndex < tasks.size()) {
                        tv = TaskStackView.this.getChildViewForTask((Task) tasks.get(TaskStackView.this.mFocusedTaskIndex));
                        if (tv != null) {
                            tv.setFocusedTask(true);
                        }
                    }
                    TaskStackView.this.showDismissAllButton();
                }
            });
        }
    }

    public void startExitToHomeAnimation(ViewAnimation$TaskViewExitContext ctx) {
        this.mStackScroller.stopScroller();
        this.mStackScroller.stopBoundScrollAnimation();
        ctx.offscreenTranslationY = this.mLayoutAlgorithm.mViewRect.bottom - (this.mLayoutAlgorithm.mTaskRect.top - this.mLayoutAlgorithm.mViewRect.top);
        hideDismissAllButton(null);
        List<TaskView> taskViews = getTaskViews();
        int taskViewCount = taskViews.size();
        for (int i = 0; i < taskViewCount; i++) {
            ((TaskView) taskViews.get(i)).startExitToHomeAnimation(ctx);
        }
    }

    public void startLaunchTaskAnimation(TaskView tv, Runnable r, boolean lockToTask) {
        Task launchTargetTask = tv.getTask();
        List<TaskView> taskViews = getTaskViews();
        int taskViewCount = taskViews.size();
        for (int i = 0; i < taskViewCount; i++) {
            TaskView t = (TaskView) taskViews.get(i);
            if (t == tv) {
                t.setClipViewInStack(false);
                t.startLaunchTaskAnimation(r, true, true, lockToTask);
            } else {
                t.startLaunchTaskAnimation(null, false, launchTargetTask.group.isTaskAboveTask(t.getTask(), launchTargetTask), lockToTask);
            }
        }
    }

    void showDismissAllButton() {
        if (this.mDismissAllButton != null) {
            if (!this.mDismissAllButtonAnimating && this.mDismissAllButton.getVisibility() == 0) {
                if (Float.compare(this.mDismissAllButton.getAlpha(), 0.0f) == 0) {
                }
            }
            this.mDismissAllButtonAnimating = true;
            this.mDismissAllButton.setVisibility(0);
            this.mDismissAllButton.showClearButton();
            this.mDismissAllButton.findViewById(R.id.dismiss_text).setAlpha(1.0f);
            this.mDismissAllButton.setAlpha(0.0f);
            this.mDismissAllButton.animate().alpha(1.0f).setDuration(250).withEndAction(new Runnable() {
                public void run() {
                    TaskStackView.this.mDismissAllButtonAnimating = false;
                }
            }).start();
        }
    }

    void hideDismissAllButton(final Runnable postAnimRunnable) {
        if (this.mDismissAllButton != null) {
            this.mDismissAllButtonAnimating = true;
            this.mDismissAllButton.animate().alpha(0.0f).setDuration(200).withEndAction(new Runnable() {
                public void run() {
                    TaskStackView.this.mDismissAllButtonAnimating = false;
                    TaskStackView.this.mDismissAllButton.setVisibility(8);
                    if (postAnimRunnable != null) {
                        postAnimRunnable.run();
                    }
                }
            }).start();
        }
    }

    void updateDismissButtonPosition() {
        if (this.mDismissAllButton != null && this.mStack.getTaskCount() > 0) {
            float[] fArr = this.mTmpCoord;
            this.mTmpCoord[1] = 0.0f;
            fArr[0] = 0.0f;
            TaskView tv = getChildViewForTask(this.mStack.getFrontMostTask());
            TaskViewTransform transform = (TaskViewTransform) this.mCurrentTaskTransforms.get(this.mStack.getTaskCount() - 1);
            if (tv != null && transform.visible) {
                Utilities.mapCoordInDescendentToSelf(tv, this, this.mTmpCoord, false);
                this.mDismissAllButton.setTranslationY(this.mTmpCoord[1] + (tv.getScaleY() * ((float) tv.getHeight())));
                this.mDismissAllButton.setTranslationX(((float) (-(this.mLayoutAlgorithm.mStackRect.width() - transform.rect.width()))) / 2.0f);
            }
        }
    }

    void onRecentsHidden() {
        reset();
    }

    public boolean isTransformedTouchPointInView(float x, float y, View child) {
        return isTransformedTouchPointInView(x, y, child, null);
    }

    void onUserInteraction() {
        this.mUIDozeTrigger.poke();
    }

    protected void dispatchDraw(Canvas canvas) {
        this.mLayersDisabled = false;
        super.dispatchDraw(canvas);
    }

    public void disableLayersForOneFrame() {
        this.mLayersDisabled = true;
        List<TaskView> taskViews = getTaskViews();
        for (int i = 0; i < taskViews.size(); i++) {
            ((TaskView) taskViews.get(i)).disableLayersForOneFrame();
        }
    }

    public void onStackTaskAdded(TaskStack stack, Task t) {
        requestSynchronizeStackViewsWithModel();
    }

    public void onStackTaskRemoved(TaskStack stack, Task removedTask, Task newFrontMostTask) {
        TaskView tv = getChildViewForTask(removedTask);
        if (tv != null) {
            this.mViewPool.returnViewToPool(tv);
        }
        Task anchorTask = null;
        float prevAnchorTaskScroll = 0.0f;
        boolean pullStackForward = stack.getTaskCount() > 0;
        if (pullStackForward) {
            anchorTask = this.mStack.getFrontMostTask();
            prevAnchorTaskScroll = this.mLayoutAlgorithm.getStackScrollForTask(anchorTask);
        }
        updateMinMaxScroll(true, this.mConfig.launchedWithAltTab, this.mConfig.launchedFromHome);
        if (pullStackForward) {
            this.mStackScroller.setStackScroll(this.mStackScroller.getStackScroll() + (this.mLayoutAlgorithm.getStackScrollForTask(anchorTask) - prevAnchorTaskScroll));
            this.mStackScroller.boundScroll();
        }
        requestSynchronizeStackViewsWithModel(200);
        if (newFrontMostTask != null) {
            TaskView frontTv = getChildViewForTask(newFrontMostTask);
            if (frontTv != null) {
                frontTv.onTaskBound(newFrontMostTask);
                frontTv.fadeInActionButton(0, this.mConfig.taskViewEnterFromAppDuration);
            }
        }
        if (this.mStack.getTaskCount() == 0) {
            boolean shouldFinishActivity = true;
            if (this.mStack.hasFilteredTasks()) {
                this.mStack.unfilterTasks();
                shouldFinishActivity = this.mStack.getTaskCount() == 0;
            }
            if (shouldFinishActivity) {
                this.mCb.onAllTaskViewsDismissed(null);
            }
        } else {
            showDismissAllButton();
        }
        this.mCb.onTaskViewDismissed(removedTask);
    }

    public void onStackUnfiltered(TaskStack newStack, ArrayList<Task> arrayList) {
    }

    public TaskView createView(Context context) {
        return (TaskView) this.mInflater.inflate(R.layout.recents_task_view, this, false);
    }

    public void prepareViewToEnterPool(TaskView tv) {
        Task task = tv.getTask();
        if (tv.isAccessibilityFocused()) {
            tv.clearAccessibilityFocus();
        }
        RecentsTaskLoader.getInstance().unloadTaskData(task);
        detachViewFromParent(tv);
        updateTaskViewsList();
        tv.resetViewProperties();
        tv.setClipViewInStack(false);
    }

    public void prepareViewToLeavePool(TaskView tv, Task task, boolean isNewView) {
        boolean requiresRelayout = tv.getWidth() <= 0 && !isNewView;
        tv.onTaskBound(task);
        RecentsTaskLoader.getInstance().loadTaskData(task);
        if (this.mConfig.multiStackEnabled || this.mUIDozeTrigger.hasTriggered()) {
            tv.setNoUserInteractionState();
        }
        if (this.mStartEnterAnimationCompleted) {
            tv.enableFocusAnimations();
        }
        int insertIndex = -1;
        int taskIndex = this.mStack.indexOfTask(task);
        if (taskIndex != -1) {
            List<TaskView> taskViews = getTaskViews();
            int taskViewCount = taskViews.size();
            for (int i = 0; i < taskViewCount; i++) {
                if (taskIndex < this.mStack.indexOfTask(((TaskView) taskViews.get(i)).getTask())) {
                    insertIndex = i + 0;
                    break;
                }
            }
        }
        if (isNewView) {
            addView(tv, insertIndex);
        } else {
            attachViewToParent(tv, insertIndex, tv.getLayoutParams());
            if (requiresRelayout) {
                tv.requestLayout();
            }
        }
        updateTaskViewsList();
        tv.setCallbacks(this);
        tv.setTouchEnabled(true);
        tv.setClipViewInStack(true);
    }

    public boolean hasPreferredData(TaskView tv, Task preferredData) {
        return tv.getTask() == preferredData;
    }

    public void onTaskViewAppInfoClicked(TaskView tv) {
        if (this.mCb != null) {
            this.mCb.onTaskViewAppInfoClicked(tv.getTask());
            MetricsLogger.count(getContext(), "overview_app_info", 1);
        }
    }

    public void onTaskViewClicked(TaskView tv, Task task, boolean lockToTask) {
        this.mUIDozeTrigger.stopDozing();
        if (this.mCb != null) {
            this.mCb.onTaskViewClicked(this, tv, this.mStack, task, lockToTask);
        }
    }

    public void onTaskViewDismissed(TaskView tv) {
        Task task = tv.getTask();
        int taskIndex = this.mStack.indexOfTask(task);
        boolean taskWasFocused = tv.isFocusedTask();
        tv.announceForAccessibility(getContext().getString(R.string.accessibility_recents_item_dismissed, new Object[]{tv.getTask().activityLabel}));
        this.mStack.removeTask(task);
        if (taskWasFocused) {
            ArrayList<Task> tasks = this.mStack.getTasks();
            int nextTaskIndex = Math.min(tasks.size() - 1, taskIndex - 1);
            if (nextTaskIndex >= 0) {
                TaskView nextTv = getChildViewForTask((Task) tasks.get(nextTaskIndex));
                if (nextTv != null) {
                    nextTv.setFocusedTask(this.mConfig.launchedWithAltTab);
                }
            }
        }
    }

    public void onTaskViewClipStateChanged(TaskView tv) {
        if (!this.mStackViewsDirty) {
            invalidate();
        }
    }

    public void onTaskViewFocusChanged(TaskView tv, boolean focused) {
        if (focused) {
            this.mFocusedTaskIndex = this.mStack.indexOfTask(tv.getTask());
        }
    }

    public void onTaskResize(TaskView tv) {
        if (this.mCb != null) {
            this.mCb.onTaskResize(tv.getTask());
        }
    }

    public void onScrollChanged(float p) {
        this.mUIDozeTrigger.poke();
        requestSynchronizeStackViewsWithModel();
        postInvalidateOnAnimation();
    }

    public void onPackagesChanged(RecentsPackageMonitor monitor, String packageName, int userId) {
        HashSet<ComponentName> removedComponents = monitor.computeComponentsRemoved(this.mStack.getTaskKeys(), packageName, userId);
        ArrayList<Task> tasks = this.mStack.getTasks();
        for (int i = tasks.size() - 1; i >= 0; i--) {
            final Task t = (Task) tasks.get(i);
            if (removedComponents.contains(t.key.baseIntent.getComponent())) {
                TaskView tv = getChildViewForTask(t);
                if (tv != null) {
                    tv.startDeleteTaskAnimation(new Runnable() {
                        public void run() {
                            TaskStackView.this.mStack.removeTask(t);
                        }
                    }, 0);
                } else {
                    this.mStack.removeTask(t);
                }
            }
        }
    }

    public boolean isLockApp(String packageName) {
        return Boolean.valueOf(this.mContext.getSharedPreferences("FineosRecentsApp", 0).getBoolean(packageName, false)).booleanValue();
    }

    public void dismissAllTaskView() {
        this.mAllTaskCount = this.mStack.getTaskCount();
        if (this.mAllTaskCount > 0) {
            this.mClearTaskCount = 0;
            resetFocusedTask();
            setForntFocusTask(this.mAllTaskCount - 1, true, false);
            this.myHandler = new MyHandler();
            this.mNumTasks = this.mStack.getTaskCount();
            int c = 0;
            if (this.mNumTasks >= 6) {
                this.mNumTasks = 6;
            }
            for (int i = 0; i < this.mNumTasks; i++) {
                Message message = new Message();
                message.what = 0;
                c++;
                this.myHandler.sendMessageDelayed(message, (long) (c * 100));
            }
        }
    }

    public int getTaskCount() {
        return this.mStack.getTaskCount();
    }

    void dismissForntTask(int taskIndex, boolean scrollToNewPosition, boolean animateFocusedState) {
        if (taskIndex != this.mFocusedTaskIndex && taskIndex >= 0 && taskIndex < this.mStack.getTaskCount()) {
            this.mFocusedTaskIndex = taskIndex;
            Task t = (Task) this.mStack.getTasks().get(taskIndex);
            TaskView tv = getChildViewForTask(t);
            if (!isLockApp(t.key.baseIntent.getComponent().getPackageName())) {
                Runnable postScrollRunnable = null;
                if (tv != null) {
                    tv.dismissTask();
                } else {
                    postScrollRunnable = new Runnable() {
                        public void run() {
                            TaskView tv = TaskStackView.this.getChildViewForTask((Task) TaskStackView.this.mStack.getTasks().get(TaskStackView.this.mFocusedTaskIndex));
                            if (tv != null) {
                                tv.dismissTask();
                            }
                        }
                    };
                }
                if (scrollToNewPosition) {
                    this.mStackScroller.animateScroll(this.mStackScroller.getStackScroll(), this.mStackScroller.getBoundedStackScroll(this.mLayoutAlgorithm.getStackScrollForTask(t) - 0.5f), postScrollRunnable);
                } else if (postScrollRunnable != null) {
                    postScrollRunnable.run();
                }
            }
        }
    }

    void setForntFocusTask(int taskIndex, boolean scrollToNewPosition, final boolean animateFocusedState) {
        if (taskIndex != this.mFocusedTaskIndex && taskIndex >= 0 && taskIndex < this.mStack.getTaskCount()) {
            this.mFocusedTaskIndex = taskIndex;
            Task t = (Task) this.mStack.getTasks().get(taskIndex);
            TaskView tv = getChildViewForTask(t);
            Runnable postScrollRunnable = null;
            if (tv != null) {
                tv.setFocusedTask(animateFocusedState);
            } else {
                postScrollRunnable = new Runnable() {
                    public void run() {
                        TaskView tv = TaskStackView.this.getChildViewForTask((Task) TaskStackView.this.mStack.getTasks().get(TaskStackView.this.mFocusedTaskIndex));
                        if (tv != null) {
                            tv.setFocusedTask(animateFocusedState);
                        }
                    }
                };
            }
            if (scrollToNewPosition) {
                this.mStackScroller.animateScroll(this.mStackScroller.getStackScroll(), this.mStackScroller.getBoundedStackScroll(this.mLayoutAlgorithm.getStackScrollForTask(t) - 0.5f), postScrollRunnable);
            } else if (postScrollRunnable != null) {
                postScrollRunnable.run();
            }
        }
    }

    public void removeTask() {
        int stackCount = this.mStack.getTaskCount();
        for (int i = 0; i < stackCount; i++) {
            Task t = (Task) this.mStack.getTasks().get(i);
            if (!isLockApp(t.key.baseIntent.getComponent().getPackageName())) {
                this.mCb.onTaskViewDismissed(t);
            }
        }
    }
}
