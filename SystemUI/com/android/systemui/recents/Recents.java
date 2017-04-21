package com.android.systemui.recents;

import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityOptions;
import android.app.ActivityOptions.OnAnimationStartedListener;
import android.app.ITaskStackListener.Stub;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.util.MutableBoolean;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.RecentsComponent;
import com.android.systemui.RecentsComponent.Callbacks;
import com.android.systemui.SystemUI;
import com.android.systemui.recents.Constants.Values.App;
import com.android.systemui.recents.misc.Console;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.RecentsTaskLoadPlan;
import com.android.systemui.recents.model.RecentsTaskLoadPlan.Options;
import com.android.systemui.recents.model.RecentsTaskLoader;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.Task.TaskKey;
import com.android.systemui.recents.model.TaskGrouping;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.views.TaskStackView;
import com.android.systemui.recents.views.TaskStackViewLayoutAlgorithm;
import com.android.systemui.recents.views.TaskStackViewLayoutAlgorithm.VisibilityReport;
import com.android.systemui.recents.views.TaskViewHeader;
import com.android.systemui.recents.views.TaskViewTransform;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import java.util.ArrayList;

public class Recents extends SystemUI implements OnAnimationStartedListener, RecentsComponent {
    static Recents sInstance;
    static RecentsTaskLoadPlan sInstanceLoadPlan;
    static Callbacks sRecentsComponentCallbacks;
    RecentsAppWidgetHost mAppWidgetHost;
    boolean mBootCompleted;
    boolean mCanReuseTaskStackViews = true;
    RecentsConfiguration mConfig;
    TaskStackView mDummyStackView;
    Handler mHandler;
    TaskViewHeader mHeaderBar;
    final Object mHeaderBarLock = new Object();
    LayoutInflater mInflater;
    long mLastToggleTime;
    int mNavBarHeight;
    int mNavBarWidth;
    RecentsOwnerEventProxyReceiver mProxyBroadcastReceiver;
    boolean mStartAnimationTriggered;
    int mStatusBarHeight;
    Rect mSystemInsets = new Rect();
    SystemServicesProxy mSystemServicesProxy;
    Rect mTaskStackBounds = new Rect();
    TaskStackListenerImpl mTaskStackListener;
    Bitmap mThumbnailTransitionBitmapCache;
    Task mThumbnailTransitionBitmapCacheKey;
    TaskViewTransform mTmpTransform = new TaskViewTransform();
    boolean mTriggeredFromAltTab;
    Rect mWindowRect = new Rect();

    class RecentsOwnerEventProxyReceiver extends BroadcastReceiver {
        RecentsOwnerEventProxyReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("action_notify_recents_visibility_change")) {
                Recents.visibilityChanged(intent.getBooleanExtra("recentsVisibility", false));
            } else if (action.equals("action_screen_pinning_request")) {
                Recents.onStartScreenPinning(context);
            }
        }
    }

    class TaskStackListenerImpl extends Stub implements Runnable {
        Handler mHandler;

        public TaskStackListenerImpl(Handler handler) {
            this.mHandler = handler;
        }

        public void onTaskStackChanged() {
            this.mHandler.removeCallbacks(this);
            this.mHandler.post(this);
        }

        public void run() {
            if (!Recents.this.mConfig.multiStackEnabled && RecentsConfiguration.getInstance().svelteLevel == 0) {
                RecentsTaskLoader loader = RecentsTaskLoader.getInstance();
                RunningTaskInfo runningTaskInfo = loader.getSystemServicesProxy().getTopMostTask();
                RecentsTaskLoadPlan plan = loader.createLoadPlan(Recents.this.mContext);
                loader.preloadTasks(plan, true);
                Options launchOpts = new Options();
                if (runningTaskInfo != null) {
                    launchOpts.runningTaskId = runningTaskInfo.id;
                }
                launchOpts.numVisibleTasks = 2;
                launchOpts.numVisibleTaskThumbnails = 2;
                launchOpts.onlyLoadForCache = true;
                launchOpts.onlyLoadPausedActivities = true;
                loader.loadTasks(Recents.this.mContext, plan, launchOpts);
            }
        }
    }

    public static Recents getInstanceAndStartIfNeeded(Context ctx) {
        if (sInstance == null) {
            sInstance = new Recents();
            sInstance.mContext = ctx;
            sInstance.start();
            sInstance.onBootCompleted();
        }
        return sInstance;
    }

    static Intent createLocalBroadcastIntent(Context context, String action) {
        Intent intent = new Intent(action);
        intent.setPackage(context.getPackageName());
        intent.addFlags(335544320);
        return intent;
    }

    @ProxyFromPrimaryToCurrentUser
    public void start() {
        if (sInstance == null) {
            sInstance = this;
        }
        RecentsTaskLoader.initialize(this.mContext);
        this.mInflater = LayoutInflater.from(this.mContext);
        this.mSystemServicesProxy = new SystemServicesProxy(this.mContext);
        this.mHandler = new Handler();
        this.mTaskStackBounds = new Rect();
        this.mAppWidgetHost = new RecentsAppWidgetHost(this.mContext, App.AppWidgetHostId);
        this.mTaskStackListener = new TaskStackListenerImpl(this.mHandler);
        this.mSystemServicesProxy.registerTaskStackListener(this.mTaskStackListener);
        if (this.mSystemServicesProxy.isForegroundUserOwner()) {
            this.mProxyBroadcastReceiver = new RecentsOwnerEventProxyReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction("action_notify_recents_visibility_change");
            filter.addAction("action_screen_pinning_request");
            if ("tablet".equals(SystemProperties.get("ro.build.characteristics"))) {
                this.mContext.getApplicationContext().registerReceiverAsUser(this.mProxyBroadcastReceiver, UserHandle.CURRENT, filter, null, this.mHandler);
            } else {
                this.mContext.registerReceiverAsUser(this.mProxyBroadcastReceiver, UserHandle.CURRENT, filter, null, this.mHandler);
            }
        }
        TaskStackViewLayoutAlgorithm.initializeCurve();
        reloadHeaderBarLayout();
        RecentsTaskLoader loader = RecentsTaskLoader.getInstance();
        RecentsTaskLoadPlan plan = loader.createLoadPlan(this.mContext);
        loader.preloadTasks(plan, true);
        Options launchOpts = new Options();
        launchOpts.numVisibleTasks = loader.getApplicationIconCacheSize();
        launchOpts.numVisibleTaskThumbnails = loader.getThumbnailCacheSize();
        launchOpts.onlyLoadForCache = true;
        loader.loadTasks(this.mContext, plan, launchOpts);
        putComponent(Recents.class, this);
    }

    public void onBootCompleted() {
        this.mBootCompleted = true;
    }

    @ProxyFromPrimaryToCurrentUser
    public void showRecents(boolean triggeredFromAltTab, View statusBarView) {
        if (isDeviceProvisioned()) {
            if (this.mSystemServicesProxy.isForegroundUserOwner()) {
                showRecentsInternal(triggeredFromAltTab);
            } else {
                Intent intent = createLocalBroadcastIntent(this.mContext, "com.android.systemui.recents.action.SHOW_RECENTS_FOR_USER");
                intent.putExtra("triggeredFromAltTab", triggeredFromAltTab);
                this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
            }
        }
    }

    void showRecentsInternal(boolean triggeredFromAltTab) {
        this.mTriggeredFromAltTab = triggeredFromAltTab;
        try {
            startRecentsActivity();
        } catch (ActivityNotFoundException e) {
            Console.logRawError("Failed to launch RecentAppsIntent", e);
        }
    }

    @ProxyFromPrimaryToCurrentUser
    public void hideRecents(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
        if (isDeviceProvisioned()) {
            if (this.mSystemServicesProxy.isForegroundUserOwner()) {
                hideRecentsInternal(triggeredFromAltTab, triggeredFromHomeKey);
            } else {
                Intent intent = createLocalBroadcastIntent(this.mContext, "com.android.systemui.recents.action.HIDE_RECENTS_FOR_USER");
                intent.putExtra("triggeredFromAltTab", triggeredFromAltTab);
                intent.putExtra("triggeredFromHomeKey", triggeredFromHomeKey);
                this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
            }
        }
    }

    void hideRecentsInternal(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
        if (this.mBootCompleted) {
            Intent intent = createLocalBroadcastIntent(this.mContext, "action_hide_recents_activity");
            intent.putExtra("triggeredFromAltTab", triggeredFromAltTab);
            intent.putExtra("triggeredFromHomeKey", triggeredFromHomeKey);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
        }
    }

    @ProxyFromPrimaryToCurrentUser
    public void toggleRecents(Display display, int layoutDirection, View statusBarView) {
        if (isDeviceProvisioned()) {
            if (this.mSystemServicesProxy.isForegroundUserOwner()) {
                toggleRecentsInternal();
            } else {
                this.mContext.sendBroadcastAsUser(createLocalBroadcastIntent(this.mContext, "com.android.systemui.recents.action.TOGGLE_RECENTS_FOR_USER"), UserHandle.CURRENT);
            }
        }
    }

    void toggleRecentsInternal() {
        this.mTriggeredFromAltTab = false;
        try {
            toggleRecentsActivity();
        } catch (ActivityNotFoundException e) {
            Console.logRawError("Failed to launch RecentAppsIntent", e);
        }
    }

    @ProxyFromPrimaryToCurrentUser
    public void preloadRecents() {
        if (isDeviceProvisioned()) {
            if (this.mSystemServicesProxy.isForegroundUserOwner()) {
                preloadRecentsInternal();
            } else {
                this.mContext.sendBroadcastAsUser(createLocalBroadcastIntent(this.mContext, "com.android.systemui.recents.action.PRELOAD_RECENTS_FOR_USER"), UserHandle.CURRENT);
            }
        }
    }

    void preloadRecentsInternal() {
        RunningTaskInfo topTask = this.mSystemServicesProxy.getTopMostTask();
        MutableBoolean topTaskHome = new MutableBoolean(true);
        RecentsTaskLoader loader = RecentsTaskLoader.getInstance();
        sInstanceLoadPlan = loader.createLoadPlan(this.mContext);
        if (topTask != null && !this.mSystemServicesProxy.isRecentsTopMost(topTask, topTaskHome)) {
            sInstanceLoadPlan.preloadRawTasks(topTaskHome.value);
            loader.preloadTasks(sInstanceLoadPlan, topTaskHome.value);
            TaskStack top = (TaskStack) sInstanceLoadPlan.getAllTaskStacks().get(0);
            if (top.getTaskCount() > 0) {
                preCacheThumbnailTransitionBitmapAsync(topTask, top, this.mDummyStackView, topTaskHome.value);
            }
        }
    }

    public void cancelPreloadingRecents() {
    }

    void showRelativeAffiliatedTask(boolean showNextTask) {
        TaskStack focusedStack;
        int focusedStackId = this.mSystemServicesProxy.getFocusedStack();
        RecentsTaskLoader loader = RecentsTaskLoader.getInstance();
        RecentsTaskLoadPlan plan = loader.createLoadPlan(this.mContext);
        loader.preloadTasks(plan, true);
        if (!this.mConfig.multiStackEnabled) {
            focusedStack = (TaskStack) plan.getAllTaskStacks().get(0);
        } else if (focusedStackId >= 0) {
            focusedStack = plan.getTaskStack(focusedStackId);
        } else {
            return;
        }
        if (focusedStack != null && focusedStack.getTaskCount() != 0) {
            RunningTaskInfo runningTask = this.mSystemServicesProxy.getTopMostTask();
            if (runningTask != null && !this.mSystemServicesProxy.isInHomeStack(runningTask.id)) {
                ArrayList<Task> tasks = focusedStack.getTasks();
                Task task = null;
                ActivityOptions activityOptions = null;
                int taskCount = tasks.size();
                int numAffiliatedTasks = 0;
                for (int i = 0; i < taskCount; i++) {
                    Task task2 = (Task) tasks.get(i);
                    if (task2.key.id == runningTask.id) {
                        TaskKey toTaskKey;
                        TaskGrouping group = task2.group;
                        if (showNextTask) {
                            toTaskKey = group.getNextTaskInGroup(task2);
                            activityOptions = ActivityOptions.makeCustomAnimation(this.mContext, R.anim.recents_launch_next_affiliated_task_target, R.anim.recents_launch_next_affiliated_task_source);
                        } else {
                            toTaskKey = group.getPrevTaskInGroup(task2);
                            activityOptions = ActivityOptions.makeCustomAnimation(this.mContext, R.anim.recents_launch_prev_affiliated_task_target, R.anim.recents_launch_prev_affiliated_task_source);
                        }
                        if (toTaskKey != null) {
                            task = focusedStack.findTaskWithId(toTaskKey.id);
                        }
                        numAffiliatedTasks = group.getTaskCount();
                        if (task != null) {
                            if (numAffiliatedTasks > 1) {
                                if (showNextTask) {
                                    this.mSystemServicesProxy.startInPlaceAnimationOnFrontMostApplication(ActivityOptions.makeCustomInPlaceAnimation(this.mContext, R.anim.recents_launch_prev_affiliated_task_bounce));
                                } else {
                                    this.mSystemServicesProxy.startInPlaceAnimationOnFrontMostApplication(ActivityOptions.makeCustomInPlaceAnimation(this.mContext, R.anim.recents_launch_next_affiliated_task_bounce));
                                }
                            }
                        }
                        MetricsLogger.count(this.mContext, "overview_affiliated_task_launch", 1);
                        if (task.isActive) {
                            this.mSystemServicesProxy.startActivityFromRecents(this.mContext, task.key.id, task.activityLabel, activityOptions);
                        } else {
                            this.mSystemServicesProxy.moveTaskToFront(task.key.id, activityOptions);
                        }
                        return;
                    }
                }
                if (task != null) {
                    MetricsLogger.count(this.mContext, "overview_affiliated_task_launch", 1);
                    if (task.isActive) {
                        this.mSystemServicesProxy.startActivityFromRecents(this.mContext, task.key.id, task.activityLabel, activityOptions);
                    } else {
                        this.mSystemServicesProxy.moveTaskToFront(task.key.id, activityOptions);
                    }
                    return;
                }
                if (numAffiliatedTasks > 1) {
                    if (showNextTask) {
                        this.mSystemServicesProxy.startInPlaceAnimationOnFrontMostApplication(ActivityOptions.makeCustomInPlaceAnimation(this.mContext, R.anim.recents_launch_prev_affiliated_task_bounce));
                    } else {
                        this.mSystemServicesProxy.startInPlaceAnimationOnFrontMostApplication(ActivityOptions.makeCustomInPlaceAnimation(this.mContext, R.anim.recents_launch_next_affiliated_task_bounce));
                    }
                }
            }
        }
    }

    public void showNextAffiliatedTask() {
        if (isDeviceProvisioned()) {
            MetricsLogger.count(this.mContext, "overview_affiliated_task_next", 1);
            showRelativeAffiliatedTask(true);
        }
    }

    public void showPrevAffiliatedTask() {
        if (isDeviceProvisioned()) {
            MetricsLogger.count(this.mContext, "overview_affiliated_task_prev", 1);
            showRelativeAffiliatedTask(false);
        }
    }

    @ProxyFromPrimaryToCurrentUser
    public void onConfigurationChanged(Configuration newConfig) {
        if (this.mSystemServicesProxy.isForegroundUserOwner()) {
            configurationChanged();
            return;
        }
        this.mContext.sendBroadcastAsUser(createLocalBroadcastIntent(this.mContext, "com.android.systemui.recents.action.CONFIG_CHANGED_FOR_USER"), UserHandle.CURRENT);
    }

    void configurationChanged() {
        this.mCanReuseTaskStackViews = false;
        reloadHeaderBarLayout();
    }

    void reloadHeaderBarLayout() {
        int i;
        Resources res = this.mContext.getResources();
        this.mWindowRect = this.mSystemServicesProxy.getWindowRect();
        this.mStatusBarHeight = res.getDimensionPixelSize(17104919);
        this.mNavBarHeight = res.getDimensionPixelSize(17104920);
        this.mNavBarWidth = res.getDimensionPixelSize(17104922);
        this.mConfig = RecentsConfiguration.reinitialize(this.mContext, this.mSystemServicesProxy);
        this.mConfig.updateOnConfigurationChange();
        Rect searchBarBounds = new Rect();
        if (this.mSystemServicesProxy.getOrBindSearchAppWidget(this.mContext, this.mAppWidgetHost) != null) {
            this.mConfig.getSearchBarBounds(this.mWindowRect.width(), this.mWindowRect.height(), this.mStatusBarHeight, searchBarBounds);
        }
        RecentsConfiguration recentsConfiguration = this.mConfig;
        int width = this.mWindowRect.width();
        int height = this.mWindowRect.height();
        int i2 = this.mStatusBarHeight;
        if (this.mConfig.hasTransposedNavBar) {
            i = this.mNavBarWidth;
        } else {
            i = 0;
        }
        recentsConfiguration.getAvailableTaskStackBounds(width, height, i2, i, searchBarBounds, this.mTaskStackBounds);
        if (this.mConfig.isLandscape && this.mConfig.hasTransposedNavBar) {
            this.mSystemInsets.set(0, this.mStatusBarHeight, this.mNavBarWidth, 0);
        } else {
            this.mSystemInsets.set(0, this.mStatusBarHeight, 0, this.mNavBarHeight);
        }
        this.mDummyStackView = new TaskStackView(this.mContext, new TaskStack());
        TaskStackViewLayoutAlgorithm algo = this.mDummyStackView.getStackAlgorithm();
        Rect taskStackBounds = new Rect(this.mTaskStackBounds);
        taskStackBounds.bottom -= this.mSystemInsets.bottom;
        algo.computeRects(this.mWindowRect.width(), this.mWindowRect.height(), taskStackBounds);
        Rect taskViewSize = algo.getUntransformedTaskViewSize();
        int taskBarHeight = res.getDimensionPixelSize(R.dimen.recents_task_bar_height);
        synchronized (this.mHeaderBarLock) {
            this.mHeaderBar = (TaskViewHeader) this.mInflater.inflate(R.layout.recents_task_view_header, null, false);
            this.mHeaderBar.measure(MeasureSpec.makeMeasureSpec(taskViewSize.width(), 1073741824), MeasureSpec.makeMeasureSpec(taskBarHeight, 1073741824));
            this.mHeaderBar.layout(0, 0, taskViewSize.width(), taskBarHeight);
        }
    }

    void toggleRecentsActivity() {
        if (SystemClock.elapsedRealtime() - this.mLastToggleTime >= 350) {
            RunningTaskInfo topTask = this.mSystemServicesProxy.getTopMostTask();
            MutableBoolean isTopTaskHome = new MutableBoolean(true);
            if (topTask == null || !this.mSystemServicesProxy.isRecentsTopMost(topTask, isTopTaskHome)) {
                startRecentsActivity(topTask, isTopTaskHome.value);
                return;
            }
            this.mContext.sendBroadcastAsUser(createLocalBroadcastIntent(this.mContext, "action_toggle_recents_activity"), UserHandle.CURRENT);
            this.mLastToggleTime = SystemClock.elapsedRealtime();
        }
    }

    void startRecentsActivity() {
        RunningTaskInfo topTask = this.mSystemServicesProxy.getTopMostTask();
        MutableBoolean isTopTaskHome = new MutableBoolean(true);
        if (topTask == null || !this.mSystemServicesProxy.isRecentsTopMost(topTask, isTopTaskHome)) {
            startRecentsActivity(topTask, isTopTaskHome.value);
        }
    }

    ActivityOptions getUnknownTransitionActivityOptions() {
        this.mStartAnimationTriggered = false;
        return ActivityOptions.makeCustomAnimation(this.mContext, R.anim.recents_from_unknown_enter, R.anim.recents_from_unknown_exit, this.mHandler, this);
    }

    ActivityOptions getHomeTransitionActivityOptions(boolean fromSearchHome) {
        this.mStartAnimationTriggered = false;
        if (fromSearchHome) {
            return ActivityOptions.makeCustomAnimation(this.mContext, R.anim.recents_from_search_launcher_enter, R.anim.recents_from_search_launcher_exit, this.mHandler, this);
        }
        return ActivityOptions.makeCustomAnimation(this.mContext, R.anim.recents_from_launcher_enter, R.anim.recents_from_launcher_exit, this.mHandler, this);
    }

    ActivityOptions getThumbnailTransitionActivityOptions(RunningTaskInfo topTask, TaskStack stack, TaskStackView stackView) {
        Bitmap thumbnail;
        Task toTask = new Task();
        TaskViewTransform toTransform = getThumbnailTransitionTransform(stack, stackView, topTask.id, toTask);
        Rect toTaskRect = toTransform.rect;
        if (this.mThumbnailTransitionBitmapCacheKey == null || this.mThumbnailTransitionBitmapCacheKey.key == null || !this.mThumbnailTransitionBitmapCacheKey.key.equals(toTask.key)) {
            preloadIcon(topTask);
            thumbnail = drawThumbnailTransitionBitmap(toTask, toTransform);
        } else {
            thumbnail = this.mThumbnailTransitionBitmapCache;
            this.mThumbnailTransitionBitmapCacheKey = null;
            this.mThumbnailTransitionBitmapCache = null;
        }
        if (thumbnail == null) {
            return getUnknownTransitionActivityOptions();
        }
        this.mStartAnimationTriggered = false;
        return ActivityOptions.makeThumbnailAspectScaleDownAnimation(this.mDummyStackView, thumbnail, toTaskRect.left, toTaskRect.top, toTaskRect.width(), toTaskRect.height(), this.mHandler, this);
    }

    void preloadIcon(RunningTaskInfo task) {
        Options launchOpts = new Options();
        launchOpts.runningTaskId = task.id;
        launchOpts.loadThumbnails = false;
        launchOpts.onlyLoadForCache = true;
        RecentsTaskLoader.getInstance().loadTasks(this.mContext, sInstanceLoadPlan, launchOpts);
    }

    void preCacheThumbnailTransitionBitmapAsync(RunningTaskInfo topTask, TaskStack stack, TaskStackView stackView, boolean isTopTaskHome) {
        preloadIcon(topTask);
        this.mDummyStackView.updateMinMaxScrollForStack(stack, this.mTriggeredFromAltTab, isTopTaskHome);
        final Task toTask = new Task();
        final TaskViewTransform toTransform = getThumbnailTransitionTransform(stack, stackView, topTask.id, toTask);
        new AsyncTask<Void, Void, Bitmap>() {
            protected Bitmap doInBackground(Void... params) {
                return Recents.this.drawThumbnailTransitionBitmap(toTask, toTransform);
            }

            protected void onPostExecute(Bitmap bitmap) {
                Recents.this.mThumbnailTransitionBitmapCache = bitmap;
                Recents.this.mThumbnailTransitionBitmapCacheKey = toTask;
            }
        }.execute(new Void[0]);
    }

    Bitmap drawThumbnailTransitionBitmap(Task toTask, TaskViewTransform toTransform) {
        if (toTransform == null || toTask.key == null) {
            return null;
        }
        Bitmap thumbnail;
        synchronized (this.mHeaderBarLock) {
            thumbnail = Bitmap.createBitmap((int) (((float) this.mHeaderBar.getMeasuredWidth()) * toTransform.scale), (int) (((float) this.mHeaderBar.getMeasuredHeight()) * toTransform.scale), Config.ARGB_8888);
            Canvas c = new Canvas(thumbnail);
            c.scale(toTransform.scale, toTransform.scale);
            this.mHeaderBar.rebindToTask(toTask);
            this.mHeaderBar.draw(c);
            c.setBitmap(null);
        }
        return thumbnail.createAshmemBitmap();
    }

    TaskViewTransform getThumbnailTransitionTransform(TaskStack stack, TaskStackView stackView, int runningTaskId, Task runningTaskOut) {
        Task task = null;
        ArrayList<Task> tasks = stack.getTasks();
        if (runningTaskId != -1) {
            for (int i = tasks.size() - 1; i >= 0; i--) {
                Task t = (Task) tasks.get(i);
                if (t.key.id == runningTaskId) {
                    task = t;
                    runningTaskOut.copyFrom(t);
                    break;
                }
            }
        }
        if (task == null) {
            task = (Task) tasks.get(tasks.size() - 1);
            runningTaskOut.copyFrom(task);
        }
        stackView.getScroller().setStackScrollToInitialState();
        this.mTmpTransform = stackView.getStackAlgorithm().getStackTransform(task, stackView.getScroller().getStackScroll(), this.mTmpTransform, null);
        return this.mTmpTransform;
    }

    void startRecentsActivity(RunningTaskInfo topTask, boolean isTopTaskHome) {
        RecentsTaskLoader loader = RecentsTaskLoader.getInstance();
        RecentsConfiguration.reinitialize(this.mContext, this.mSystemServicesProxy);
        if (sInstanceLoadPlan == null) {
            sInstanceLoadPlan = loader.createLoadPlan(this.mContext);
        }
        if (this.mConfig.multiStackEnabled) {
            loader.preloadTasks(sInstanceLoadPlan, true);
            this.mDummyStackView.updateMinMaxScrollForStack((TaskStack) sInstanceLoadPlan.getAllTaskStacks().get(0), this.mTriggeredFromAltTab, true);
            startAlternateRecentsActivity(topTask, getUnknownTransitionActivityOptions(), true, false, false, this.mDummyStackView.computeStackVisibilityReport());
            return;
        }
        if (!sInstanceLoadPlan.hasTasks()) {
            loader.preloadTasks(sInstanceLoadPlan, isTopTaskHome);
        }
        TaskStack stack = (TaskStack) sInstanceLoadPlan.getAllTaskStacks().get(0);
        this.mDummyStackView.updateMinMaxScrollForStack(stack, this.mTriggeredFromAltTab, isTopTaskHome);
        VisibilityReport stackVr = this.mDummyStackView.computeStackVisibilityReport();
        boolean hasRecentTasks = stack.getTaskCount() > 0;
        boolean z = (topTask == null || isTopTaskHome) ? false : hasRecentTasks;
        if (z) {
            ActivityOptions opts = getThumbnailTransitionActivityOptions(topTask, stack, this.mDummyStackView);
            if (opts != null) {
                startAlternateRecentsActivity(topTask, opts, false, false, true, stackVr);
            } else {
                z = false;
            }
        }
        if (!z) {
            if (hasRecentTasks) {
                boolean equals;
                String homeActivityPackage = this.mSystemServicesProxy.getHomeActivityPackageName();
                String searchWidgetPackage = Prefs.getString(this.mContext, "searchAppWidgetPackage", null);
                if (homeActivityPackage != null) {
                    equals = homeActivityPackage.equals(searchWidgetPackage);
                } else {
                    equals = false;
                }
                startAlternateRecentsActivity(topTask, getHomeTransitionActivityOptions(equals), true, equals, false, stackVr);
            } else {
                startAlternateRecentsActivity(topTask, getUnknownTransitionActivityOptions(), true, false, false, stackVr);
            }
        }
        this.mLastToggleTime = SystemClock.elapsedRealtime();
    }

    void startAlternateRecentsActivity(RunningTaskInfo topTask, ActivityOptions opts, boolean fromHome, boolean fromSearchHome, boolean fromThumbnail, VisibilityReport vr) {
        RecentsConfiguration recentsConfiguration = this.mConfig;
        if (fromSearchHome) {
            fromHome = true;
        }
        recentsConfiguration.launchedFromHome = fromHome;
        this.mConfig.launchedFromSearchHome = fromSearchHome;
        this.mConfig.launchedFromAppWithThumbnail = fromThumbnail;
        this.mConfig.launchedToTaskId = topTask != null ? topTask.id : -1;
        this.mConfig.launchedWithAltTab = this.mTriggeredFromAltTab;
        this.mConfig.launchedReuseTaskStackViews = this.mCanReuseTaskStackViews;
        this.mConfig.launchedNumVisibleTasks = vr.numVisibleTasks;
        this.mConfig.launchedNumVisibleThumbnails = vr.numVisibleThumbnails;
        this.mConfig.launchedHasConfigurationChanged = false;
        Intent intent = new Intent("com.android.systemui.recents.SHOW_RECENTS");
        intent.setClassName("com.android.systemui", "com.android.systemui.recents.RecentsActivity");
        intent.setFlags(276840448);
        if (opts != null) {
            this.mContext.startActivityAsUser(intent, opts.toBundle(), UserHandle.CURRENT);
        } else {
            this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        }
        this.mCanReuseTaskStackViews = true;
    }

    public void setCallback(Callbacks cb) {
        sRecentsComponentCallbacks = cb;
    }

    @ProxyFromAnyToPrimaryUser
    public static void notifyVisibilityChanged(Context context, SystemServicesProxy ssp, boolean visible) {
        if (ssp.isForegroundUserOwner()) {
            visibilityChanged(visible);
            return;
        }
        Intent intent = createLocalBroadcastIntent(context, "action_notify_recents_visibility_change");
        intent.putExtra("recentsVisibility", visible);
        context.sendBroadcastAsUser(intent, UserHandle.OWNER);
    }

    static void visibilityChanged(boolean visible) {
        if (sRecentsComponentCallbacks != null) {
            sRecentsComponentCallbacks.onVisibilityChanged(visible);
        }
    }

    @ProxyFromAnyToPrimaryUser
    public static void startScreenPinning(Context context, SystemServicesProxy ssp) {
        if (ssp.isForegroundUserOwner()) {
            onStartScreenPinning(context);
        } else {
            context.sendBroadcastAsUser(createLocalBroadcastIntent(context, "action_screen_pinning_request"), UserHandle.OWNER);
        }
    }

    static void onStartScreenPinning(Context context) {
        PhoneStatusBar statusBar = (PhoneStatusBar) getInstanceAndStartIfNeeded(context).mContext.getComponent(PhoneStatusBar.class);
        if (statusBar != null) {
            statusBar.showScreenPinningRequest(false);
        }
    }

    private boolean isDeviceProvisioned() {
        return Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
    }

    public static RecentsTaskLoadPlan consumeInstanceLoadPlan() {
        RecentsTaskLoadPlan plan = sInstanceLoadPlan;
        sInstanceLoadPlan = null;
        return plan;
    }

    public void onAnimationStarted() {
        if (!this.mStartAnimationTriggered) {
            BroadcastReceiver fallbackReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    if (getResultCode() == -1) {
                        Recents.this.mStartAnimationTriggered = true;
                    } else {
                        Recents.this.mHandler.postDelayed(new Runnable() {
                            public void run() {
                                Recents.this.onAnimationStarted();
                            }
                        }, 25);
                    }
                }
            };
            this.mContext.sendOrderedBroadcastAsUser(createLocalBroadcastIntent(this.mContext, "action_start_enter_animation"), UserHandle.CURRENT, null, fallbackReceiver, null, 0, null, null);
        }
    }
}
