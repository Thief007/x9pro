package com.android.systemui.recents;

import android.app.Activity;
import android.app.ActivityOptions;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewStub;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.assis.app.MAIN.EVENT;
import com.android.systemui.recents.Constants.Values.App;
import com.android.systemui.recents.misc.Console;
import com.android.systemui.recents.misc.DebugTrigger;
import com.android.systemui.recents.misc.ReferenceCountedTrigger;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.RecentsTaskLoadPlan;
import com.android.systemui.recents.model.RecentsTaskLoadPlan.Options;
import com.android.systemui.recents.model.RecentsTaskLoader;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.views.DebugOverlayView;
import com.android.systemui.recents.views.DebugOverlayView.DebugOverlayViewCallbacks;
import com.android.systemui.recents.views.RecentsView;
import com.android.systemui.recents.views.RecentsView.RecentsViewCallbacks;
import com.android.systemui.recents.views.SystemBarScrimViews;
import com.android.systemui.recents.views.ViewAnimation$TaskViewEnterContext;
import com.android.systemui.recents.views.ViewAnimation$TaskViewExitContext;
import com.mediatek.multiwindow.MultiWindowProxy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class RecentsActivity extends Activity implements RecentsViewCallbacks, RecentsAppWidgetHostCallbacks, DebugOverlayViewCallbacks {
    private static final boolean FLOAT_WINDOW_SUPPORT = MultiWindowProxy.isSupported();
    Runnable mAfterPauseRunnable;
    RecentsAppWidgetHost mAppWidgetHost;
    RecentsConfiguration mConfig;
    DebugOverlayView mDebugOverlay;
    ViewStub mDebugOverlayStub;
    final DebugTrigger mDebugTrigger = new DebugTrigger(new Runnable() {
        public void run() {
            RecentsActivity.this.onDebugModeTriggered();
        }
    });
    View mEmptyView;
    ViewStub mEmptyViewStub;
    FinishRecentsRunnable mFinishLaunchHomeRunnable;
    long mLastTabKeyEventTime;
    RecentsView mRecentsView;
    RecentsResizeTaskDialog mResizeTaskDebugDialog;
    ImageView mRunningApps;
    SystemBarScrimViews mScrimViews;
    RecentsAppWidgetHostView mSearchWidgetHostView;
    AppWidgetProviderInfo mSearchWidgetInfo;
    final BroadcastReceiver mServiceBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("action_hide_recents_activity")) {
                if (intent.getBooleanExtra("triggeredFromAltTab", false)) {
                    RecentsActivity.this.dismissRecentsToFocusedTaskOrHome(false);
                } else if (intent.getBooleanExtra("triggeredFromHomeKey", false)) {
                    RecentsActivity.this.dismissRecentsToHomeRaw(true);
                }
            } else if (action.equals("action_toggle_recents_activity")) {
                RecentsActivity.this.dismissRecentsToFocusedTaskOrHome(true);
            } else if (action.equals("action_start_enter_animation")) {
                RecentsActivity.this.onEnterAnimationTriggered();
                setResultCode(-1);
            }
        }
    };
    final BroadcastReceiver mSystemBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.SCREEN_OFF")) {
                RecentsActivity.this.dismissRecentsToHome(false);
            } else if (action.equals("android.search.action.GLOBAL_SEARCH_ACTIVITY_CHANGED")) {
                SystemServicesProxy ssp = RecentsTaskLoader.getInstance().getSystemServicesProxy();
                RecentsActivity.this.mSearchWidgetInfo = ssp.getOrBindSearchAppWidget(context, RecentsActivity.this.mAppWidgetHost);
                RecentsActivity.this.refreshSearchWidgetView();
            }
        }
    };
    ImageView mTaskClear;

    class FinishRecentsRunnable implements Runnable {
        Intent mLaunchIntent;
        ActivityOptions mLaunchOpts;

        public FinishRecentsRunnable(Intent launchIntent, ActivityOptions opts) {
            this.mLaunchIntent = launchIntent;
            this.mLaunchOpts = opts;
        }

        public void run() {
            if (this.mLaunchIntent != null) {
                try {
                    if (this.mLaunchOpts != null) {
                        RecentsActivity.this.startActivityAsUser(this.mLaunchIntent, this.mLaunchOpts.toBundle(), UserHandle.CURRENT);
                    } else {
                        RecentsActivity.this.startActivityAsUser(this.mLaunchIntent, UserHandle.CURRENT);
                    }
                } catch (Exception e) {
                    Console.logError(RecentsActivity.this, RecentsActivity.this.getString(R.string.recents_launch_error_message, new Object[]{"Home"}));
                }
                if (RecentsActivity.FLOAT_WINDOW_SUPPORT) {
                    RecentsActivity.this.overridePendingTransition(R.anim.recents_to_launcher_enter, R.anim.recents_to_launcher_exit);
                    return;
                }
                return;
            }
            RecentsActivity.this.finish();
            RecentsActivity.this.overridePendingTransition(R.anim.recents_to_launcher_enter, R.anim.recents_to_launcher_exit);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void updateRecentsTasks() {
        int i;
        int i2;
        int i3;
        int taskCount;
        RecentsTaskLoader loader = RecentsTaskLoader.getInstance();
        RecentsTaskLoadPlan plan = Recents.consumeInstanceLoadPlan();
        if (plan == null) {
            plan = loader.createLoadPlan(this);
        }
        if (!plan.hasTasks()) {
            loader.preloadTasks(plan, this.mConfig.launchedFromHome);
        }
        Options loadOpts = new Options();
        loadOpts.runningTaskId = this.mConfig.launchedToTaskId;
        loadOpts.numVisibleTasks = this.mConfig.launchedNumVisibleTasks;
        loadOpts.numVisibleTaskThumbnails = this.mConfig.launchedNumVisibleThumbnails;
        loader.loadTasks(this, plan, loadOpts);
        ArrayList<TaskStack> stacks = plan.getAllTaskStacks();
        this.mConfig.launchedWithNoRecentTasks = !plan.hasTasks();
        if (!this.mConfig.launchedWithNoRecentTasks) {
            this.mRecentsView.setTaskStacks(stacks);
        }
        Intent homeIntent = new Intent("android.intent.action.MAIN", null);
        homeIntent.addCategory("android.intent.category.HOME");
        homeIntent.addFlags(270532608);
        if (this.mConfig.launchedFromSearchHome) {
            i = R.anim.recents_to_search_launcher_enter;
        } else {
            i = R.anim.recents_to_launcher_enter;
        }
        if (this.mConfig.launchedFromSearchHome) {
            i2 = R.anim.recents_to_search_launcher_exit;
        } else {
            i2 = R.anim.recents_to_launcher_exit;
        }
        this.mFinishLaunchHomeRunnable = new FinishRecentsRunnable(homeIntent, ActivityOptions.makeCustomAnimation(this, i, i2));
        int taskStackCount = stacks.size();
        int launchTaskIndexInStack = 0;
        if (this.mConfig.launchedToTaskId != -1) {
            for (i3 = 0; i3 < taskStackCount; i3++) {
                ArrayList<Task> tasks = ((TaskStack) stacks.get(i3)).getTasks();
                taskCount = tasks.size();
                for (int j = 0; j < taskCount; j++) {
                    Task t = (Task) tasks.get(j);
                    if (t.key.id == this.mConfig.launchedToTaskId) {
                        t.isLaunchTarget = true;
                        launchTaskIndexInStack = (tasks.size() - j) - 1;
                        break;
                    }
                }
            }
        }
        if (this.mConfig.launchedWithNoRecentTasks) {
            if (this.mEmptyView == null) {
                this.mEmptyView = this.mEmptyViewStub.inflate();
            }
            this.mEmptyView.setVisibility(0);
            this.mRecentsView.setSearchBarVisibility(8);
            if (this.mTaskClear != null) {
                this.mTaskClear.setVisibility(8);
            }
        } else {
            if (this.mEmptyView != null) {
                this.mEmptyView.setVisibility(8);
            }
            if (this.mRecentsView.hasValidSearchBar()) {
                this.mRecentsView.setSearchBarVisibility(0);
            } else {
                refreshSearchWidgetView();
            }
            if (this.mTaskClear != null) {
                this.mTaskClear.setVisibility(0);
            }
            if (this.mRunningApps != null) {
            }
        }
        this.mScrimViews.prepareEnterRecentsAnimation();
        if (this.mConfig.launchedWithAltTab) {
            MetricsLogger.count(this, "overview_trigger_alttab", 1);
        } else {
            MetricsLogger.count(this, "overview_trigger_nav_btn", 1);
        }
        if (this.mConfig.launchedFromAppWithThumbnail) {
            MetricsLogger.count(this, "overview_source_app", 1);
            MetricsLogger.histogram(this, "overview_source_app_index", launchTaskIndexInStack);
        } else {
            MetricsLogger.count(this, "overview_source_home", 1);
        }
        taskCount = 0;
        for (i3 = 0; i3 < stacks.size(); i3++) {
            taskCount += ((TaskStack) stacks.get(i3)).getTaskCount();
        }
        MetricsLogger.histogram(this, "overview_task_count", taskCount);
    }

    boolean dismissRecentsToFocusedTaskOrHome(boolean checkFilteredStackState) {
        SystemServicesProxy ssp = RecentsTaskLoader.getInstance().getSystemServicesProxy();
        if (!ssp.isRecentsTopMost(ssp.getTopMostTask(), null)) {
            return false;
        }
        if ((checkFilteredStackState && this.mRecentsView.unfilterFilteredStacks()) || this.mRecentsView.launchFocusedTask()) {
            return true;
        }
        if (this.mConfig.launchedFromHome) {
            dismissRecentsToHomeRaw(true);
            return true;
        } else if (this.mRecentsView.launchPreviousTask()) {
            return true;
        } else {
            dismissRecentsToHomeRaw(true);
            return true;
        }
    }

    void dismissRecentsToHomeRaw(boolean animated) {
        if (animated) {
            this.mRecentsView.startExitToHomeAnimation(new ViewAnimation$TaskViewExitContext(new ReferenceCountedTrigger(this, null, this.mFinishLaunchHomeRunnable, null)));
            return;
        }
        this.mFinishLaunchHomeRunnable.run();
    }

    void dismissRecentsToHomeWithoutTransitionAnimation() {
        finish();
        overridePendingTransition(0, 0);
    }

    boolean dismissRecentsToHome(boolean animated) {
        SystemServicesProxy ssp = RecentsTaskLoader.getInstance().getSystemServicesProxy();
        if (!ssp.isRecentsTopMost(ssp.getTopMostTask(), null)) {
            return false;
        }
        dismissRecentsToHomeRaw(animated);
        return true;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RecentsTaskLoader.initialize(this);
        SystemServicesProxy ssp = RecentsTaskLoader.getInstance().getSystemServicesProxy();
        this.mConfig = RecentsConfiguration.reinitialize(this, ssp);
        this.mAppWidgetHost = new RecentsAppWidgetHost(this, App.AppWidgetHostId);
        setContentView(R.layout.recents);
        this.mRecentsView = (RecentsView) findViewById(R.id.recents_view);
        this.mRecentsView.setCallbacks(this);
        this.mRecentsView.setSystemUiVisibility(1792);
        this.mEmptyViewStub = (ViewStub) findViewById(R.id.empty_view_stub);
        this.mDebugOverlayStub = (ViewStub) findViewById(R.id.debug_overlay_stub);
        this.mScrimViews = new SystemBarScrimViews(this, this.mConfig);
        inflateDebugOverlay();
        this.mSearchWidgetInfo = ssp.getOrBindSearchAppWidget(this, this.mAppWidgetHost);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.search.action.GLOBAL_SEARCH_ACTIVITY_CHANGED");
        registerReceiver(this.mSystemBroadcastReceiver, filter);
        this.mTaskClear = (ImageView) findViewById(R.id.funui_clear_task);
        if ("0".equals(SystemProperties.get("qemu.hw.mainkeys"))) {
            LayoutParams layoutParams = (LayoutParams) this.mTaskClear.getLayoutParams();
            layoutParams.bottomMargin = getResources().getDimensionPixelSize(17104920);
            this.mTaskClear.setLayoutParams(layoutParams);
        }
        this.mTaskClear.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                RecentsActivity.this.mRecentsView.clearAllTasks();
                RecentsActivity.this.mRecentsView.onRecentsHidden();
                RecentsActivity.this.onAllTaskViewsDismissed();
            }
        });
        this.mTaskClear.setVisibility(0);
        this.mRunningApps = (ImageView) findViewById(R.id.funui_running_apps);
        if ("0".equals(SystemProperties.get("qemu.hw.mainkeys"))) {
            layoutParams = (LayoutParams) this.mRunningApps.getLayoutParams();
            layoutParams.bottomMargin = getResources().getDimensionPixelSize(17104920);
            this.mRunningApps.setLayoutParams(layoutParams);
        }
        this.mRunningApps.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.RunningServices"));
                    RecentsActivity.this.startActivity(intent);
                } catch (Exception e) {
                    Log.e("yinjun", "Exception===" + e);
                }
            }
        });
        this.mRunningApps.setVisibility(8);
    }

    void inflateDebugOverlay() {
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (this.mDebugOverlay != null) {
            this.mDebugOverlay.clear();
        }
    }

    protected void onStart() {
        super.onStart();
        MetricsLogger.visible(this, 224);
        RecentsTaskLoader loader = RecentsTaskLoader.getInstance();
        Recents.notifyVisibilityChanged(this, loader.getSystemServicesProxy(), true);
        IntentFilter filter = new IntentFilter();
        filter.addAction("action_hide_recents_activity");
        filter.addAction("action_toggle_recents_activity");
        filter.addAction("action_start_enter_animation");
        registerReceiver(this.mServiceBroadcastReceiver, filter);
        loader.registerReceivers(this, this.mRecentsView);
        updateRecentsTasks();
        boolean wasLaunchedByAm = (this.mConfig.launchedFromHome || this.mConfig.launchedFromAppWithThumbnail) ? false : true;
        if (this.mConfig.launchedHasConfigurationChanged || wasLaunchedByAm) {
            onEnterAnimationTriggered();
        }
        if (!this.mConfig.launchedHasConfigurationChanged) {
            this.mRecentsView.disableLayersForOneFrame();
        }
    }

    protected void onPause() {
        super.onPause();
        if (this.mAfterPauseRunnable != null) {
            this.mRecentsView.post(this.mAfterPauseRunnable);
            this.mAfterPauseRunnable = null;
        }
    }

    protected void onStop() {
        super.onStop();
        MetricsLogger.hidden(this, 224);
        RecentsTaskLoader loader = RecentsTaskLoader.getInstance();
        Recents.notifyVisibilityChanged(this, loader.getSystemServicesProxy(), false);
        this.mRecentsView.onRecentsHidden();
        unregisterReceiver(this.mServiceBroadcastReceiver);
        loader.unregisterReceivers();
        this.mConfig.launchedFromHome = false;
        this.mConfig.launchedFromSearchHome = false;
        this.mConfig.launchedFromAppWithThumbnail = false;
        this.mConfig.launchedToTaskId = -1;
        this.mConfig.launchedWithAltTab = false;
        this.mConfig.launchedHasConfigurationChanged = false;
    }

    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(this.mSystemBroadcastReceiver);
        this.mAppWidgetHost.stopListening();
    }

    public void onEnterAnimationTriggered() {
        ViewAnimation$TaskViewEnterContext ctx = new ViewAnimation$TaskViewEnterContext(new ReferenceCountedTrigger(this, null, null, null));
        this.mRecentsView.startEnterRecentsAnimation(ctx);
        if (this.mSearchWidgetInfo != null) {
            final WeakReference<RecentsAppWidgetHostCallbacks> cbRef = new WeakReference(this);
            ctx.postAnimationTrigger.addLastDecrementRunnable(new Runnable() {
                public void run() {
                    RecentsAppWidgetHostCallbacks cb = (RecentsAppWidgetHostCallbacks) cbRef.get();
                    if (cb != null) {
                        RecentsActivity.this.mAppWidgetHost.startListening(cb);
                    }
                }
            });
        }
        this.mScrimViews.startEnterRecentsAnimation();
    }

    public void onTrimMemory(int level) {
        RecentsTaskLoader loader = RecentsTaskLoader.getInstance();
        if (loader != null) {
            loader.onTrimMemory(level);
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean z = false;
        switch (keyCode) {
            case 19:
                this.mRecentsView.focusNextTask(true);
                return true;
            case EVENT.DIALOG_CONFIRMED /*20*/:
                this.mRecentsView.focusNextTask(false);
                return true;
            case EVENT.TRY_LOAD_REPLACE_PACKAGES /*61*/:
                boolean hasRepKeyTimeElapsed = SystemClock.elapsedRealtime() - this.mLastTabKeyEventTime > ((long) this.mConfig.altTabKeyDelay);
                if (event.getRepeatCount() <= 0 || hasRepKeyTimeElapsed) {
                    boolean backward = event.isShiftPressed();
                    RecentsView recentsView = this.mRecentsView;
                    if (!backward) {
                        z = true;
                    }
                    recentsView.focusNextTask(z);
                    this.mLastTabKeyEventTime = SystemClock.elapsedRealtime();
                }
                return true;
            case 67:
            case 112:
                this.mRecentsView.dismissFocusedTask();
                MetricsLogger.histogram(this, "overview_task_dismissed_source", 0);
                return true;
            default:
                this.mDebugTrigger.onKeyEvent(keyCode);
                return super.onKeyDown(keyCode, event);
        }
    }

    public void onUserInteraction() {
        this.mRecentsView.onUserInteraction();
    }

    public void onBackPressed() {
        if (!this.mConfig.debugModeEnabled) {
            dismissRecentsToFocusedTaskOrHome(true);
        }
    }

    public void onDebugModeTriggered() {
        if (this.mConfig.developerOptionsEnabled) {
            if (Prefs.getBoolean(this, "debugModeEnabled", false)) {
                Prefs.remove(this, "debugModeEnabled");
                this.mConfig.debugModeEnabled = false;
                inflateDebugOverlay();
                if (this.mDebugOverlay != null) {
                    this.mDebugOverlay.disable();
                }
            } else {
                Prefs.putBoolean(this, "debugModeEnabled", true);
                this.mConfig.debugModeEnabled = true;
                inflateDebugOverlay();
                if (this.mDebugOverlay != null) {
                    this.mDebugOverlay.enable();
                }
            }
            Toast.makeText(this, "Debug mode (" + App.DebugModeVersion + ") " + (this.mConfig.debugModeEnabled ? "Enabled" : "Disabled") + ", please restart Recents now", 0).show();
        }
    }

    private RecentsResizeTaskDialog getResizeTaskDebugDialog() {
        if (this.mResizeTaskDebugDialog == null) {
            this.mResizeTaskDebugDialog = new RecentsResizeTaskDialog(getFragmentManager(), this);
        }
        return this.mResizeTaskDebugDialog;
    }

    public void onTaskResize(Task t) {
        getResizeTaskDebugDialog().showResizeTaskDialog(t, this.mRecentsView);
    }

    public void onExitToHomeAnimationTriggered() {
        this.mScrimViews.startExitRecentsAnimation();
    }

    public void onTaskViewClicked() {
        if (!FLOAT_WINDOW_SUPPORT) {
            return;
        }
        if (this.mFinishLaunchHomeRunnable != null) {
            this.mFinishLaunchHomeRunnable.run();
            return;
        }
        Intent homeIntent = new Intent("android.intent.action.MAIN", null);
        homeIntent.addCategory("android.intent.category.HOME");
        homeIntent.addFlags(270532608);
        startActivityAsUser(homeIntent, new UserHandle(-2));
        overridePendingTransition(R.anim.recents_to_launcher_enter, R.anim.recents_to_launcher_exit);
    }

    public void onTaskLaunchFailed() {
        dismissRecentsToHomeRaw(true);
    }

    public void onAllTaskViewsDismissed() {
        this.mFinishLaunchHomeRunnable.run();
    }

    public void onScreenPinningRequest() {
        Recents.startScreenPinning(this, RecentsTaskLoader.getInstance().getSystemServicesProxy());
        MetricsLogger.count(this, "overview_screen_pinned", 1);
    }

    public void runAfterPause(Runnable r) {
        this.mAfterPauseRunnable = r;
    }

    public void refreshSearchWidgetView() {
        if (this.mSearchWidgetInfo != null) {
            this.mSearchWidgetHostView = (RecentsAppWidgetHostView) this.mAppWidgetHost.createView(this, RecentsTaskLoader.getInstance().getSystemServicesProxy().getSearchAppWidgetId(this), this.mSearchWidgetInfo);
            Bundle opts = new Bundle();
            opts.putInt("appWidgetCategory", 4);
            this.mSearchWidgetHostView.updateAppWidgetOptions(opts);
            this.mSearchWidgetHostView.setPadding(0, 0, 0, 0);
            this.mRecentsView.setSearchBar(this.mSearchWidgetHostView);
            return;
        }
        this.mRecentsView.setSearchBar(null);
    }

    public void onPrimarySeekBarChanged(float progress) {
    }

    public void onSecondarySeekBarChanged(float progress) {
    }
}
