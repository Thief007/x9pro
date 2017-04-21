package com.android.systemui.recents.views;

import android.app.ActivityOptions;
import android.app.ActivityOptions.OnAnimationStartedListener;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.IRemoteCallback.Stub;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.IWindowManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.WindowInsets;
import android.view.WindowManagerGlobal;
import android.widget.FrameLayout;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.recents.RecentsAppWidgetHostView;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.RecentsPackageMonitor;
import com.android.systemui.recents.model.RecentsPackageMonitor.PackageCallbacks;
import com.android.systemui.recents.model.RecentsTaskLoader;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import java.util.ArrayList;
import java.util.List;

public class RecentsView extends FrameLayout implements TaskStackViewCallbacks, PackageCallbacks {
    RecentsViewCallbacks mCb;
    RecentsConfiguration mConfig;
    DebugOverlayView mDebugOverlay;
    LayoutInflater mInflater;
    RecentsViewLayoutAlgorithm mLayoutAlgorithm;
    RecentsAppWidgetHostView mSearchBar;
    ArrayList<TaskStack> mStacks;
    List<TaskStackView> mTaskStackViews;

    public interface RecentsViewCallbacks {
        void onAllTaskViewsDismissed();

        void onExitToHomeAnimationTriggered();

        void onScreenPinningRequest();

        void onTaskLaunchFailed();

        void onTaskResize(Task task);

        void onTaskViewClicked();

        void runAfterPause(Runnable runnable);
    }

    public RecentsView(Context context) {
        super(context);
        this.mTaskStackViews = new ArrayList();
    }

    public RecentsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecentsView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RecentsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mTaskStackViews = new ArrayList();
        this.mConfig = RecentsConfiguration.getInstance();
        this.mInflater = LayoutInflater.from(context);
        this.mLayoutAlgorithm = new RecentsViewLayoutAlgorithm(this.mConfig);
    }

    public void setCallbacks(RecentsViewCallbacks cb) {
        this.mCb = cb;
    }

    public void setDebugOverlay(DebugOverlayView overlay) {
        this.mDebugOverlay = overlay;
    }

    public void setTaskStacks(ArrayList<TaskStack> stacks) {
        int i;
        int numStacks = stacks.size();
        int numTaskStacksToKeep = 0;
        if (this.mConfig.launchedReuseTaskStackViews) {
            numTaskStacksToKeep = Math.min(this.mTaskStackViews.size(), numStacks);
        }
        for (i = this.mTaskStackViews.size() - 1; i >= numTaskStacksToKeep; i--) {
            removeView((View) this.mTaskStackViews.remove(i));
        }
        for (i = 0; i < numTaskStacksToKeep; i++) {
            TaskStackView tsv = (TaskStackView) this.mTaskStackViews.get(i);
            tsv.reset();
            tsv.setStack((TaskStack) stacks.get(i));
        }
        this.mStacks = stacks;
        for (i = this.mTaskStackViews.size(); i < numStacks; i++) {
            TaskStackView stackView = new TaskStackView(getContext(), (TaskStack) stacks.get(i));
            stackView.setCallbacks(this);
            addView(stackView);
            this.mTaskStackViews.add(stackView);
        }
        if (this.mConfig.debugModeEnabled) {
            for (i = this.mTaskStackViews.size() - 1; i >= 0; i--) {
                ((TaskStackView) this.mTaskStackViews.get(i)).setDebugOverlay(this.mDebugOverlay);
            }
        }
        requestLayout();
    }

    List<TaskStackView> getTaskStackViews() {
        return this.mTaskStackViews;
    }

    public Task getNextTaskOrTopTask(Task taskToSearch) {
        Task returnTask = null;
        boolean found = false;
        List<TaskStackView> stackViews = getTaskStackViews();
        for (int i = stackViews.size() - 1; i >= 0; i--) {
            ArrayList<Task> taskList = ((TaskStackView) stackViews.get(i)).getStack().getTasks();
            for (int j = taskList.size() - 1; j >= 0; j--) {
                Task task = (Task) taskList.get(j);
                if (found) {
                    return task;
                }
                if (returnTask == null) {
                    returnTask = task;
                }
                if (task == taskToSearch) {
                    found = true;
                }
            }
        }
        return returnTask;
    }

    public boolean launchFocusedTask() {
        List<TaskStackView> stackViews = getTaskStackViews();
        int stackCount = stackViews.size();
        for (int i = 0; i < stackCount; i++) {
            TaskStackView stackView = (TaskStackView) stackViews.get(i);
            TaskStack stack = stackView.getStack();
            List<TaskView> taskViews = stackView.getTaskViews();
            int taskViewCount = taskViews.size();
            for (int j = 0; j < taskViewCount; j++) {
                TaskView tv = (TaskView) taskViews.get(j);
                Task task = tv.getTask();
                if (tv.isFocusedTask()) {
                    onTaskViewClicked(stackView, tv, stack, task, false);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean launchTask(Task task) {
        List<TaskStackView> stackViews = getTaskStackViews();
        int stackCount = stackViews.size();
        for (int i = 0; i < stackCount; i++) {
            TaskStackView stackView = (TaskStackView) stackViews.get(i);
            TaskStack stack = stackView.getStack();
            List<TaskView> taskViews = stackView.getTaskViews();
            int taskViewCount = taskViews.size();
            for (int j = 0; j < taskViewCount; j++) {
                TaskView tv = (TaskView) taskViews.get(j);
                if (tv.getTask() == task) {
                    onTaskViewClicked(stackView, tv, stack, task, false);
                    return true;
                }
            }
        }
        return false;
    }

    public void clearAllTasks() {
        if (this.mStacks != null && !this.mStacks.isEmpty()) {
            int numStacks = this.mStacks.size();
            for (int i = 0; i < numStacks; i++) {
                ArrayList<Task> tasks = ((TaskStack) this.mStacks.get(i)).getTasks();
                Log.i("yinjun", "tasks====" + tasks.size());
                for (int j = 0; j < tasks.size(); j++) {
                    onTaskViewDismissed((Task) tasks.get(j));
                }
            }
        }
    }

    public boolean launchPreviousTask() {
        List<TaskStackView> stackViews = getTaskStackViews();
        int stackCount = stackViews.size();
        for (int i = 0; i < stackCount; i++) {
            TaskStackView stackView = (TaskStackView) stackViews.get(i);
            TaskStack stack = stackView.getStack();
            ArrayList<Task> tasks = stack.getTasks();
            if (!tasks.isEmpty()) {
                int taskCount = tasks.size();
                for (int j = 0; j < taskCount; j++) {
                    if (((Task) tasks.get(j)).isLaunchTarget) {
                        Task task = (Task) tasks.get(j);
                        onTaskViewClicked(stackView, stackView.getChildViewForTask(task), stack, task, false);
                        return true;
                    }
                }
                continue;
            }
        }
        return false;
    }

    public void startEnterRecentsAnimation(ViewAnimation$TaskViewEnterContext ctx) {
        ctx.postAnimationTrigger.increment();
        List<TaskStackView> stackViews = getTaskStackViews();
        int stackCount = stackViews.size();
        for (int i = 0; i < stackCount; i++) {
            ((TaskStackView) stackViews.get(i)).startEnterRecentsAnimation(ctx);
        }
        ctx.postAnimationTrigger.decrement();
    }

    public void startExitToHomeAnimation(ViewAnimation$TaskViewExitContext ctx) {
        ctx.postAnimationTrigger.increment();
        List<TaskStackView> stackViews = getTaskStackViews();
        int stackCount = stackViews.size();
        for (int i = 0; i < stackCount; i++) {
            ((TaskStackView) stackViews.get(i)).startExitToHomeAnimation(ctx);
        }
        ctx.postAnimationTrigger.decrement();
        this.mCb.onExitToHomeAnimationTriggered();
    }

    public void setSearchBar(RecentsAppWidgetHostView searchBar) {
        if (this.mSearchBar != null && indexOfChild(this.mSearchBar) > -1) {
            removeView(this.mSearchBar);
        }
        if (searchBar != null) {
            this.mSearchBar = searchBar;
            addView(this.mSearchBar);
        }
    }

    public boolean hasValidSearchBar() {
        return (this.mSearchBar == null || this.mSearchBar.isReinflateRequired()) ? false : true;
    }

    public void setSearchBarVisibility(int visibility) {
        if (this.mSearchBar != null) {
            this.mSearchBar.setVisibility(visibility);
            this.mSearchBar.bringToFront();
        }
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        Rect searchBarSpaceBounds = new Rect();
        if (this.mSearchBar != null) {
            this.mConfig.getSearchBarBounds(width, height, this.mConfig.systemInsets.top, searchBarSpaceBounds);
            this.mSearchBar.measure(MeasureSpec.makeMeasureSpec(searchBarSpaceBounds.width(), 1073741824), MeasureSpec.makeMeasureSpec(searchBarSpaceBounds.height(), 1073741824));
        }
        Rect taskStackBounds = new Rect();
        this.mConfig.getAvailableTaskStackBounds(width, height, this.mConfig.systemInsets.top, this.mConfig.systemInsets.right, searchBarSpaceBounds, taskStackBounds);
        List<TaskStackView> stackViews = getTaskStackViews();
        List<Rect> stackViewsBounds = this.mLayoutAlgorithm.computeStackRects(stackViews, taskStackBounds);
        int stackCount = stackViews.size();
        for (int i = 0; i < stackCount; i++) {
            TaskStackView stackView = (TaskStackView) stackViews.get(i);
            if (stackView.getVisibility() != 8) {
                stackView.setStackInsetRect((Rect) stackViewsBounds.get(i));
                stackView.measure(widthMeasureSpec, heightMeasureSpec);
            }
        }
        setMeasuredDimension(width, height);
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.mSearchBar != null) {
            Rect searchBarSpaceBounds = new Rect();
            this.mConfig.getSearchBarBounds(getMeasuredWidth(), getMeasuredHeight(), this.mConfig.systemInsets.top, searchBarSpaceBounds);
            this.mSearchBar.layout(searchBarSpaceBounds.left, searchBarSpaceBounds.top, searchBarSpaceBounds.right, searchBarSpaceBounds.bottom);
        }
        List<TaskStackView> stackViews = getTaskStackViews();
        int stackCount = stackViews.size();
        for (int i = 0; i < stackCount; i++) {
            TaskStackView stackView = (TaskStackView) stackViews.get(i);
            if (stackView.getVisibility() != 8) {
                stackView.layout(left, top, stackView.getMeasuredWidth() + left, stackView.getMeasuredHeight() + top);
            }
        }
    }

    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        this.mConfig.updateSystemInsets(insets.getSystemWindowInsets());
        requestLayout();
        return insets.consumeSystemWindowInsets();
    }

    public void onUserInteraction() {
        List<TaskStackView> stackViews = getTaskStackViews();
        int stackCount = stackViews.size();
        for (int i = 0; i < stackCount; i++) {
            ((TaskStackView) stackViews.get(i)).onUserInteraction();
        }
    }

    public void focusNextTask(boolean forward) {
        List<TaskStackView> stackViews = getTaskStackViews();
        if (!stackViews.isEmpty()) {
            ((TaskStackView) stackViews.get(0)).focusNextTask(forward, true);
        }
    }

    public void dismissFocusedTask() {
        List<TaskStackView> stackViews = getTaskStackViews();
        if (!stackViews.isEmpty()) {
            ((TaskStackView) stackViews.get(0)).dismissFocusedTask();
        }
    }

    public boolean unfilterFilteredStacks() {
        if (this.mStacks == null) {
            return false;
        }
        boolean stacksUnfiltered = false;
        int numStacks = this.mStacks.size();
        for (int i = 0; i < numStacks; i++) {
            TaskStack stack = (TaskStack) this.mStacks.get(i);
            if (stack.hasFilteredTasks()) {
                stack.unfilterTasks();
                stacksUnfiltered = true;
            }
        }
        return stacksUnfiltered;
    }

    public void disableLayersForOneFrame() {
        List<TaskStackView> stackViews = getTaskStackViews();
        for (int i = 0; i < stackViews.size(); i++) {
            ((TaskStackView) stackViews.get(i)).disableLayersForOneFrame();
        }
    }

    private void postDrawHeaderThumbnailTransitionRunnable(TaskView tv, int offsetX, int offsetY, TaskViewTransform transform, OnAnimationStartedListener animStartedListener) {
        final TaskView taskView = tv;
        final int i = offsetX;
        final int i2 = offsetY;
        final TaskViewTransform taskViewTransform = transform;
        final OnAnimationStartedListener onAnimationStartedListener = animStartedListener;
        this.mCb.runAfterPause(new Runnable() {
            public void run() {
                if (taskView.isFocusedTask()) {
                    taskView.unsetFocusedTask();
                }
                float scale = taskView.getScaleX();
                Bitmap b = Bitmap.createBitmap((int) (((float) taskView.mHeaderView.getMeasuredWidth()) * scale), (int) (((float) taskView.mHeaderView.getMeasuredHeight()) * scale), Config.ARGB_8888);
                Canvas c = new Canvas(b);
                c.scale(taskView.getScaleX(), taskView.getScaleY());
                taskView.mHeaderView.draw(c);
                c.setBitmap(null);
                b = b.createAshmemBitmap();
                int[] pts = new int[2];
                taskView.getLocationOnScreen(pts);
                try {
                    IWindowManager windowManagerService = WindowManagerGlobal.getWindowManagerService();
                    int i = pts[0] + i;
                    int i2 = pts[1] + i2;
                    int width = taskViewTransform.rect.width();
                    int height = taskViewTransform.rect.height();
                    final OnAnimationStartedListener onAnimationStartedListener = onAnimationStartedListener;
                    windowManagerService.overridePendingAppTransitionAspectScaledThumb(b, i, i2, width, height, new Stub() {
                        public void sendResult(Bundle data) throws RemoteException {
                            RecentsView recentsView = RecentsView.this;
                            final OnAnimationStartedListener onAnimationStartedListener = onAnimationStartedListener;
                            recentsView.post(new Runnable() {
                                public void run() {
                                    if (onAnimationStartedListener != null) {
                                        onAnimationStartedListener.onAnimationStarted();
                                    }
                                }
                            });
                        }
                    }, true);
                } catch (RemoteException e) {
                    Log.w("RecentsView", "Error overriding app transition", e);
                }
            }
        });
    }

    public void onTaskViewClicked(TaskStackView stackView, TaskView tv, TaskStack stack, Task task, boolean lockToTask) {
        View sourceView;
        if (this.mCb != null) {
            this.mCb.onTaskViewClicked();
        }
        TaskViewTransform transform = new TaskViewTransform();
        int offsetX = 0;
        int offsetY = 0;
        float stackScroll = stackView.getScroller().getStackScroll();
        if (tv == null) {
            sourceView = stackView;
            transform = stackView.getStackAlgorithm().getStackTransform(task, stackScroll, transform, null);
            offsetX = transform.rect.left;
            offsetY = this.mConfig.displayRect.height();
        } else {
            sourceView = tv.mThumbnailView;
            transform = stackView.getStackAlgorithm().getStackTransform(task, stackScroll, transform, null);
        }
        SystemServicesProxy ssp = RecentsTaskLoader.getInstance().getSystemServicesProxy();
        ActivityOptions opts = null;
        if (task.thumbnail != null && task.thumbnail.getWidth() > 0 && task.thumbnail.getHeight() > 0) {
            OnAnimationStartedListener onAnimationStartedListener = null;
            if (lockToTask) {
                onAnimationStartedListener = new OnAnimationStartedListener() {
                    boolean mTriggered = false;

                    public void onAnimationStarted() {
                        if (!this.mTriggered) {
                            RecentsView.this.postDelayed(new Runnable() {
                                public void run() {
                                    RecentsView.this.mCb.onScreenPinningRequest();
                                }
                            }, 350);
                            this.mTriggered = true;
                        }
                    }
                };
            }
            if (tv != null) {
                postDrawHeaderThumbnailTransitionRunnable(tv, offsetX, offsetY, transform, onAnimationStartedListener);
            }
            if (this.mConfig.multiStackEnabled) {
                opts = ActivityOptions.makeCustomAnimation(sourceView.getContext(), R.anim.recents_from_unknown_enter, R.anim.recents_from_unknown_exit, sourceView.getHandler(), onAnimationStartedListener);
            } else {
                opts = ActivityOptions.makeThumbnailAspectScaleUpAnimation(sourceView, Bitmap.createBitmap(1, 1, Config.ALPHA_8).createAshmemBitmap(), offsetX, offsetY, transform.rect.width(), transform.rect.height(), sourceView.getHandler(), onAnimationStartedListener);
            }
        }
        final ActivityOptions launchOpts = opts;
        final Task task2 = task;
        final SystemServicesProxy systemServicesProxy = ssp;
        final boolean z = lockToTask;
        Runnable launchRunnable = new Runnable() {
            public void run() {
                if (task2.isActive) {
                    systemServicesProxy.moveTaskToFront(task2.key.id, launchOpts);
                } else if (!systemServicesProxy.startActivityFromRecents(RecentsView.this.getContext(), task2.key.id, task2.activityLabel, launchOpts)) {
                    RecentsView.this.onTaskViewDismissed(task2);
                    if (RecentsView.this.mCb != null) {
                        RecentsView.this.mCb.onTaskLaunchFailed();
                    }
                    MetricsLogger.count(RecentsView.this.getContext(), "overview_task_launch_failed", 1);
                } else if (launchOpts == null && z) {
                    RecentsView.this.mCb.onScreenPinningRequest();
                }
            }
        };
        int taskIndexFromFront = 0;
        int taskIndex = stack.indexOfTask(task);
        if (taskIndex > -1) {
            taskIndexFromFront = (stack.getTaskCount() - taskIndex) - 1;
        }
        MetricsLogger.histogram(getContext(), "overview_task_launch_index", taskIndexFromFront);
        if (tv == null) {
            launchRunnable.run();
        } else if (task.group == null || task.group.isFrontMostTask(task)) {
            stackView.startLaunchTaskAnimation(tv, null, lockToTask);
            launchRunnable.run();
        } else {
            stackView.startLaunchTaskAnimation(tv, launchRunnable, lockToTask);
        }
    }

    public void onTaskViewAppInfoClicked(Task t) {
        Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.fromParts("package", t.key.baseIntent.getComponent().getPackageName(), null));
        intent.setComponent(intent.resolveActivity(getContext().getPackageManager()));
        TaskStackBuilder.create(getContext()).addNextIntentWithParentStack(intent).startActivities(null, new UserHandle(t.key.userId));
    }

    public void onTaskViewDismissed(Task t) {
        RecentsTaskLoader loader = RecentsTaskLoader.getInstance();
        loader.deleteTaskData(t, false);
        loader.getSystemServicesProxy().removeTask(t.key.id);
    }

    public void onAllTaskViewsDismissed(ArrayList<Task> removedTasks) {
        if (removedTasks != null) {
            int taskCount = removedTasks.size();
            for (int i = 0; i < taskCount; i++) {
                onTaskViewDismissed((Task) removedTasks.get(i));
            }
        }
        this.mCb.onAllTaskViewsDismissed();
        MetricsLogger.count(getContext(), "overview_task_all_dismissed", 1);
    }

    public void onRecentsHidden() {
        List<TaskStackView> stackViews = getTaskStackViews();
        int stackCount = stackViews.size();
        for (int i = 0; i < stackCount; i++) {
            ((TaskStackView) stackViews.get(i)).onRecentsHidden();
        }
    }

    public void onTaskResize(Task t) {
        if (this.mCb != null) {
            this.mCb.onTaskResize(t);
        }
    }

    public void onPackagesChanged(RecentsPackageMonitor monitor, String packageName, int userId) {
        List<TaskStackView> stackViews = getTaskStackViews();
        int stackCount = stackViews.size();
        for (int i = 0; i < stackCount; i++) {
            ((TaskStackView) stackViews.get(i)).onPackagesChanged(monitor, packageName, userId);
        }
    }

    public void dismissAllTaskView() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child != this.mSearchBar) {
                ((TaskStackView) child).dismissAllTaskView();
                break;
            }
        }
        if (childCount <= 0) {
            Log.d("RecentsView", "dismissFocusedTask : no child, nothing to dismiss : " + childCount);
        }
    }

    public int getTaskCount() {
        int taskCount = 0;
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child != this.mSearchBar) {
                taskCount = ((TaskStackView) child).getTaskCount();
                break;
            }
        }
        if (childCount <= 0) {
            Log.d("RecentsView", "dismissFocusedTask : no child, nothing to dismiss : " + childCount);
        }
        return taskCount;
    }
}
