package com.android.systemui.recents;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityOptions;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.MemInfoReader;
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
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class FineCloudsRecentsActivity extends Activity implements RecentsViewCallbacks, RecentsAppWidgetHostCallbacks, DebugOverlayViewCallbacks {
    Runnable mAfterPauseRunnable;
    private ActivityManager mAm;
    RecentsAppWidgetHost mAppWidgetHost;
    private CircleBackground mCircleBackground;
    private ImageView mCleanBgView;
    private ImageView mCleanView;
    RecentsConfiguration mConfig;
    DebugOverlayView mDebugOverlay;
    ViewStub mDebugOverlayStub;
    final DebugTrigger mDebugTrigger = new DebugTrigger(new Runnable() {
        public void run() {
            FineCloudsRecentsActivity.this.onDebugModeTriggered();
        }
    });
    private ImageView mDelTweenImg;
    View mEmptyView;
    ViewStub mEmptyViewStub;
    FinishRecentsRunnable mFinishLaunchHomeRunnable;
    private final MyHandler mHandler = new MyHandler();
    private boolean mIsClickCleanALLBtn = false;
    long mLastTabKeyEventTime;
    private LinearLayout mMemInfoLayout;
    private MemInfoReader mMemInfoReader;
    private long mMemTotal;
    private long mNewmemAvail;
    private long mOldMemAvail;
    private FrameLayout mRecentsRootLayout;
    RecentsView mRecentsView;
    FineCloudsRecentsResizeTaskDialog mResizeTaskDebugDialog;
    SystemBarScrimViews mScrimViews;
    RecentsAppWidgetHostView mSearchWidgetHostView;
    AppWidgetProviderInfo mSearchWidgetInfo;
    final BroadcastReceiver mServiceBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("action_hide_recents_activity")) {
                if (intent.getBooleanExtra("triggeredFromAltTab", false)) {
                    FineCloudsRecentsActivity.this.dismissRecentsToFocusedTaskOrHome(false);
                } else if (intent.getBooleanExtra("triggeredFromHomeKey", false)) {
                    FineCloudsRecentsActivity.this.dismissRecentsToHomeRaw(true);
                }
            } else if (action.equals("action_toggle_recents_activity")) {
                FineCloudsRecentsActivity.this.dismissRecentsToFocusedTaskOrHome(true);
            } else if (action.equals("action_start_enter_animation")) {
                FineCloudsRecentsActivity.this.onEnterAnimationTriggered();
                setResultCode(-1);
            }
        }
    };
    private RunningState mState;
    final BroadcastReceiver mSystemBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.SCREEN_OFF")) {
                FineCloudsRecentsActivity.this.dismissRecentsToHome(false);
            } else if (action.equals("android.search.action.GLOBAL_SEARCH_ACTIVITY_CHANGED")) {
                SystemServicesProxy ssp = RecentsTaskLoader.getInstance().getSystemServicesProxy();
                FineCloudsRecentsActivity.this.mSearchWidgetInfo = ssp.getOrBindSearchAppWidget(context, FineCloudsRecentsActivity.this.mAppWidgetHost);
                FineCloudsRecentsActivity.this.refreshSearchWidgetView();
            }
        }
    };
    private ImageView mclearAllRecent;
    private MemoryInfo memInfo;
    private Animation operatingAnim;
    private float zramCompressRatio = Process.getZramCompressRatio();

    class AnonymousClass6 implements OnTouchListener {
        public boolean onTouch(View v, MotionEvent event) {
            return true;
        }
    }

    class AnonymousClass7 implements OnClickListener {
        final /* synthetic */ FineCloudsRecentsActivity this$0;
        final /* synthetic */ View val$privateGuideView;

        public void onClick(View v) {
            this.val$privateGuideView.setVisibility(8);
            this.this$0.mRecentsRootLayout.removeView(this.val$privateGuideView);
        }
    }

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
                        FineCloudsRecentsActivity.this.startActivityAsUser(this.mLaunchIntent, this.mLaunchOpts.toBundle(), UserHandle.CURRENT);
                        return;
                    } else {
                        FineCloudsRecentsActivity.this.startActivityAsUser(this.mLaunchIntent, UserHandle.CURRENT);
                        return;
                    }
                } catch (Exception e) {
                    Console.logError(FineCloudsRecentsActivity.this, FineCloudsRecentsActivity.this.getString(R.string.recents_launch_error_message, new Object[]{"Home"}));
                    return;
                }
            }
            FineCloudsRecentsActivity.this.finish();
            FineCloudsRecentsActivity.this.overridePendingTransition(R.anim.recents_to_launcher_enter, R.anim.recents_to_launcher_exit);
        }
    }

    private class MyHandler extends Handler {
        private MyHandler() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1003:
                    FineCloudsRecentsActivity.this.startAnimation1();
                    return;
                case 1004:
                    FineCloudsRecentsActivity.this.startAnimation2();
                    return;
                case 1005:
                    FineCloudsRecentsActivity.this.stopAnimation3();
                    return;
                case 1006:
                    FineCloudsRecentsActivity.this.startAnimation1_2();
                    return;
                default:
                    return;
            }
        }
    }

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
        } else if (this.mEmptyView != null) {
            this.mEmptyView.setVisibility(8);
        }
        this.mCleanView.setVisibility(8);
        this.mCleanBgView.setVisibility(8);
        this.mCircleBackground.setVisibility(8);
        if (this.mRecentsView.getTaskCount() <= 0) {
            this.mclearAllRecent.setVisibility(8);
            this.mMemInfoLayout.setVisibility(8);
        } else {
            this.mclearAllRecent.setVisibility(8);
            this.mMemInfoLayout.setVisibility(0);
            this.mHandler.sendEmptyMessageDelayed(1003, 600);
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
        setContentView(R.layout.fineos_recents);
        this.mRecentsView = (RecentsView) findViewById(R.id.recents_view);
        this.mRecentsView.setCallbacks(this);
        this.mRecentsView.setSystemUiVisibility(1792);
        this.mEmptyViewStub = (ViewStub) findViewById(R.id.empty_view_stub);
        this.mDebugOverlayStub = (ViewStub) findViewById(R.id.debug_overlay_stub);
        this.mRecentsRootLayout = (FrameLayout) findViewById(R.id.recents_root);
        this.mclearAllRecent = (ImageView) findViewById(R.id.removeAll);
        this.mMemInfoLayout = (LinearLayout) findViewById(R.id.memInfoLayout);
        this.mclearAllRecent.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                FineCloudsRecentsActivity.this.startTweenAnimation();
                FineCloudsRecentsActivity.this.mRecentsView.dismissAllTaskView();
            }
        });
        this.mDelTweenImg = (ImageView) findViewById(R.id.removeAllTween);
        this.mCleanView = (ImageView) findViewById(R.id.clean_icon);
        this.mCleanBgView = (ImageView) findViewById(R.id.clean_icon_bg);
        this.mCircleBackground = (CircleBackground) findViewById(R.id.circle_background);
        this.mScrimViews = new SystemBarScrimViews(this, this.mConfig);
        inflateDebugOverlay();
        this.mSearchWidgetInfo = ssp.getOrBindSearchAppWidget(this, this.mAppWidgetHost);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.search.action.GLOBAL_SEARCH_ACTIVITY_CHANGED");
        registerReceiver(this.mSystemBroadcastReceiver, filter);
        initMemInfo();
    }

    private void startTweenAnimation() {
        this.mIsClickCleanALLBtn = true;
        this.operatingAnim = AnimationUtils.loadAnimation(this, R.anim.fineos_recnts_del_tween);
        this.operatingAnim.setInterpolator(new LinearInterpolator());
        if (this.operatingAnim != null) {
            this.mDelTweenImg.setVisibility(0);
            this.mDelTweenImg.startAnimation(this.operatingAnim);
        }
    }

    private void StopTweenAnimation() {
        if (this.operatingAnim != null) {
            this.mDelTweenImg.setVisibility(8);
            this.mDelTweenImg.clearAnimation();
        }
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
        StopTweenAnimation();
        stopAnimation3();
        dismissAndGoBack(this.mIsClickCleanALLBtn, false);
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
                        FineCloudsRecentsActivity.this.mAppWidgetHost.startListening(cb);
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

    private FineCloudsRecentsResizeTaskDialog getResizeTaskDebugDialog() {
        if (this.mResizeTaskDebugDialog == null) {
            this.mResizeTaskDebugDialog = new FineCloudsRecentsResizeTaskDialog(getFragmentManager(), this);
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

    protected void onResume() {
        super.onResume();
        setMemoryInfo();
    }

    public void dismissAndGoBack(boolean isDisToast, boolean isAllLocked) {
        if (isDisToast) {
            String cleartxt;
            this.mNewmemAvail = getTotalMemory();
            this.mNewmemAvail >>= 10;
            long clearMem = this.mNewmemAvail - this.mOldMemAvail;
            if (clearMem <= 0) {
                cleartxt = getString(R.string.fineos_clear_memrey_all_locked);
            } else {
                cleartxt = getString(R.string.fineos_clear_memrey_toast, new Object[]{Long.valueOf(clearMem)});
            }
            if (isInfoAppExist()) {
                if (clearMem <= 0) {
                    clearMem = 2;
                }
                cleartxt = String.format("%sM", new Object[]{Long.valueOf(clearMem)});
                Intent intent = new Intent("android.intent.action.MAIN");
                intent.addCategory("android.intent.category.LAUNCHER");
                intent.setComponent(new ComponentName("com.fineos.info", "com.fineos.info.RecentInfoActivity"));
                intent.putExtra("clear_text", cleartxt);
                startActivity(intent);
            } else {
                Toast toast = Toast.makeText(getApplicationContext(), cleartxt, EVENT.DYNAMIC_PACK_EVENT_BASE);
                toast.setGravity(17, 0, 360);
                toast.show();
            }
        }
        this.mIsClickCleanALLBtn = false;
    }

    public void setMemoryInfo() {
        ActivityManager am = (ActivityManager) getSystemService("activity");
        MemoryInfo memInfo = new MemoryInfo();
        am.getMemoryInfo(memInfo);
        TextView mFreeMemreyInfo = (TextView) findViewById(R.id.freeMemreyInfoId);
        this.mOldMemAvail = getTotalMemory();
        this.mOldMemAvail >>= 10;
        if (mFreeMemreyInfo != null) {
            mFreeMemreyInfo.setText(Long.toString(this.mOldMemAvail) + "M");
        }
        TextView mTotalFreeMemreyInfo = (TextView) findViewById(R.id.totalMemreyInfoId);
        long memTotal = memInfo.totalMem >> 20;
        if (mTotalFreeMemreyInfo != null) {
            mTotalFreeMemreyInfo.setText("/ " + Long.toString(memTotal) + "M");
        }
        this.mMemTotal = memTotal;
    }

    private long getTotalMemory() {
        long initial_memory = 0;
        try {
            BufferedReader localBufferedReader = new BufferedReader(new FileReader("/proc/meminfo"), 8192);
            while (true) {
                String str2 = localBufferedReader.readLine();
                if (str2 == null) {
                    break;
                }
                String[] arrayOfString = str2.split("\\s+");
                if (("MemFree:".equals(arrayOfString[0]) || "Buffers:".equals(arrayOfString[0]) || "Cached:".equals(arrayOfString[0])) && arrayOfString[1] != null) {
                    initial_memory += (long) Integer.valueOf(arrayOfString[1]).intValue();
                }
            }
            localBufferedReader.close();
        } catch (IOException e) {
        }
        return initial_memory;
    }

    public boolean isInfoAppExist() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.fineos.info", "com.fineos.info.RecentInfoActivity"));
        if (getPackageManager().queryIntentActivities(intent, 65536).size() == 0) {
            return false;
        }
        return true;
    }

    private void startAnimation1() {
        this.mCleanView.setVisibility(0);
        this.mCleanView.setImageResource(R.anim.clean_gif1);
        AnimationDrawable animaition = (AnimationDrawable) this.mCleanView.getDrawable();
        animaition.setOneShot(true);
        animaition.start();
        this.mHandler.sendEmptyMessageDelayed(1006, 300);
    }

    private void startAnimation1_2() {
        this.mCleanBgView.setVisibility(0);
        this.mCleanBgView.setColorFilter(getCleanBgColor());
        this.mCleanView.setImageResource(R.anim.clean_gif1_2);
        AnimationDrawable animaition = (AnimationDrawable) this.mCleanView.getDrawable();
        animaition.setOneShot(true);
        animaition.start();
        this.mHandler.sendEmptyMessageDelayed(1004, 500);
    }

    private void startAnimation2() {
        this.mCleanView.setImageResource(R.drawable.clean_focus);
        this.mCleanView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                FineCloudsRecentsActivity.this.mIsClickCleanALLBtn = true;
                FineCloudsRecentsActivity.this.mRecentsView.dismissAllTaskView();
                FineCloudsRecentsActivity.this.startAnimation3();
            }
        });
        this.mCircleBackground.setVisibility(0);
        this.mCircleBackground.startRippleAnimation();
    }

    private void startAnimation3() {
        this.mCircleBackground.setVisibility(8);
        this.mCleanView.setImageResource(R.drawable.clean_rotate);
        Animation animaition3 = AnimationUtils.loadAnimation(this, R.anim.clean_gif3);
        animaition3.setInterpolator(new LinearInterpolator());
        this.mCleanView.startAnimation(animaition3);
    }

    private void stopAnimation3() {
        this.mCleanView.clearAnimation();
    }

    private int getCleanBgColor() {
        int mem1;
        int mem2;
        Log.e("yfm", "mMemTotal = " + this.mMemTotal + ", mOldMemAvail = " + this.mOldMemAvail);
        if (this.mMemTotal >= 1800) {
            mem1 = 800;
            mem2 = 400;
        } else {
            mem1 = 400;
            mem2 = 250;
        }
        if (this.mOldMemAvail > ((long) mem1)) {
            return -16398848;
        }
        if (this.mOldMemAvail > ((long) mem2)) {
            return -797696;
        }
        return -1963264;
    }

    private void initMemInfo() {
        this.mMemInfoReader = new MemInfoReader();
        this.mState = RunningState.getInstance(this);
        this.memInfo = new MemoryInfo();
        this.mAm = (ActivityManager) getSystemService("activity");
    }
}
