package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioAttributes.Builder;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaController.Callback;
import android.media.session.MediaSession.Token;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.Vibrator;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.service.notification.NotificationListenerService.RankingMap;
import android.service.notification.StatusBarNotification;
import android.util.ArraySet;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ThreadedRenderer;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerGlobal;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.statusbar.NotificationVisibility;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.keyguard.KeyguardHostView.OnDismissAction;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.BatteryMeterView;
import com.android.systemui.DemoMode;
import com.android.systemui.EventLogTags;
import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.assis.app.MAIN.EVENT;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.doze.DozeHost;
import com.android.systemui.doze.DozeHost.PulseCallback;
import com.android.systemui.doze.DozeLog;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.qs.QSTile.Host;
import com.android.systemui.recents.ScreenPinningRequest;
import com.android.systemui.statusbar.ActivatableNotificationView;
import com.android.systemui.statusbar.BackDropView;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.DismissView;
import com.android.systemui.statusbar.DragDownHelper.DragDownCallback;
import com.android.systemui.statusbar.EmptyShadeView;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.GestureRecorder;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.android.systemui.statusbar.NotificationData.Entry;
import com.android.systemui.statusbar.NotificationOverflowContainer;
import com.android.systemui.statusbar.ScrimView;
import com.android.systemui.statusbar.SignalClusterView;
import com.android.systemui.statusbar.SpeedBumpView;
import com.android.systemui.statusbar.phone.NavigationBarView.OnVerticalChangedListener;
import com.android.systemui.statusbar.phone.UnlockMethodCache.OnUnlockMethodChangedListener;
import com.android.systemui.statusbar.policy.AccessibilityController;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback;
import com.android.systemui.statusbar.policy.BluetoothControllerImpl;
import com.android.systemui.statusbar.policy.BrightnessMirrorController;
import com.android.systemui.statusbar.policy.CastControllerImpl;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.HeadsUpManager.HeadsUpEntry;
import com.android.systemui.statusbar.policy.HeadsUpManager.OnHeadsUpChangedListener;
import com.android.systemui.statusbar.policy.HotspotControllerImpl;
import com.android.systemui.statusbar.policy.KeyButtonView;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.KeyguardUserSwitcher;
import com.android.systemui.statusbar.policy.LocationControllerImpl;
import com.android.systemui.statusbar.policy.NetworkControllerImpl;
import com.android.systemui.statusbar.policy.NextAlarmController;
import com.android.systemui.statusbar.policy.PreviewInflater;
import com.android.systemui.statusbar.policy.RotationLockControllerImpl;
import com.android.systemui.statusbar.policy.SecurityControllerImpl;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.UserSwitcherController;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout.OnChildLocationsChangedListener;
import com.android.systemui.volume.VolumeComponent;
import com.mediatek.common.multiwindow.IMWSystemUiCallback.Stub;
import com.mediatek.multiwindow.MultiWindowProxy;
import com.mediatek.systemui.ext.IStatusBarPlmnPlugin;
import com.mediatek.systemui.statusbar.defaultaccount.DefaultAccountStatus;
import com.mediatek.systemui.statusbar.extcb.PluginFactory;
import com.mediatek.systemui.statusbar.policy.AudioProfileControllerImpl;
import com.mediatek.systemui.statusbar.policy.DataConnectionControllerImpl;
import com.mediatek.systemui.statusbar.policy.HotKnotControllerImpl;
import com.mediatek.systemui.statusbar.policy.ScreenRecordControllerImpl;
import com.mediatek.systemui.statusbar.policy.SuperScreenShotControllerImpl;
import com.mediatek.systemui.statusbar.util.FeatureOptions;
import com.mediatek.systemui.statusbar.util.SIMHelper;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class PhoneStatusBar extends BaseStatusBar implements DemoMode, DragDownCallback, ActivityStarter, OnUnlockMethodChangedListener, OnHeadsUpChangedListener {
    public static final Interpolator ALPHA_IN = new PathInterpolator(0.4f, 0.0f, 1.0f, 1.0f);
    public static final Interpolator ALPHA_OUT = new PathInterpolator(0.0f, 0.0f, 0.8f, 1.0f);
    private static final boolean ONLY_CORE_APPS;
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new Builder().setContentType(4).setUsage(13).build();
    private int defaultShowBattery = 0;
    int[] mAbsPos = new int[2];
    AccessibilityController mAccessibilityController;
    private final Runnable mAnimateCollapsePanels = new Runnable() {
        public void run() {
            PhoneStatusBar.this.animateCollapsePanels();
        }
    };
    AudioProfileControllerImpl mAudioProfileController;
    private final Runnable mAutohide = new Runnable() {
        public void run() {
            int requested = PhoneStatusBar.this.mSystemUiVisibility & -201326593;
            if (PhoneStatusBar.this.mSystemUiVisibility != requested) {
                PhoneStatusBar.this.notifyUiVisibilityChanged(requested);
            }
        }
    };
    private boolean mAutohideSuspended;
    private BackDropView mBackdrop;
    private ImageView mBackdropBack;
    private ImageView mBackdropFront;
    private Interpolator mBackdropInterpolator = new AccelerateDecelerateInterpolator();
    BatteryController mBatteryController;
    private TextView mBatteryLevel;
    private BatteryMeterView mBatteryMeterView;
    BluetoothControllerImpl mBluetoothController;
    BrightnessMirrorController mBrightnessMirrorController;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            boolean z = true;
            Log.v("PhoneStatusBar", "onReceive: " + intent);
            String action = intent.getAction();
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(action)) {
                if (PhoneStatusBar.this.isCurrentProfile(getSendingUserId())) {
                    int flags = 0;
                    String reason = intent.getStringExtra("reason");
                    if (reason != null && reason.equals("recentapps")) {
                        flags = 2;
                    }
                    PhoneStatusBar.this.animateCollapsePanels(flags);
                }
            } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
                PhoneStatusBar.this.notifyNavigationBarScreenOn(false);
                PhoneStatusBar.this.notifyHeadsUpScreenOff();
                PhoneStatusBar.this.finishBarAnimations();
                PhoneStatusBar.this.resetUserExpandedStates();
            } else if ("android.intent.action.SCREEN_ON".equals(action)) {
                PhoneStatusBar.this.notifyNavigationBarScreenOn(true);
            } else if ("com.intent.action.BATTERY_PERCENTAGE_SWITCH".equals(action)) {
                PhoneStatusBar phoneStatusBar = PhoneStatusBar.this;
                if (intent.getIntExtra("state", 0) != 1) {
                    z = false;
                }
                phoneStatusBar.showBatterPercent = z;
                if (PhoneStatusBar.this.showBatterPercent) {
                    PhoneStatusBar.this.mBatteryLevel.setVisibility(0);
                    PhoneStatusBar.this.mBatteryLevel.setText(NumberFormat.getPercentInstance().format(((double) PhoneStatusBar.this.mBatteryMeterView.getLevel()) / 100.0d));
                    return;
                }
                PhoneStatusBar.this.mBatteryLevel.setVisibility(8);
            }
        }
    };
    CastControllerImpl mCastController;
    private final Runnable mCheckBarModes = new Runnable() {
        public void run() {
            PhoneStatusBar.this.checkBarModes();
        }
    };
    Point mCurrentDisplaySize = new Point();
    private final ArraySet<NotificationVisibility> mCurrentlyVisibleNotifications = new ArraySet();
    private View mCustomizeCarrierLabel = null;
    DataConnectionControllerImpl mDataConnectionController;
    private boolean mDemoMode;
    private boolean mDemoModeAllowed;
    private BroadcastReceiver mDemoReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.v("PhoneStatusBar", "onReceive: " + intent);
            String action = intent.getAction();
            if ("com.android.systemui.demo".equals(action)) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    String command = bundle.getString("command", "").trim().toLowerCase();
                    if (command.length() > 0) {
                        try {
                            PhoneStatusBar.this.dispatchDemoCommand(command, bundle);
                        } catch (Throwable t) {
                            Log.w("PhoneStatusBar", "Error running demo command, intent=" + intent, t);
                        }
                    }
                }
            } else if (!"fake_artwork".equals(action)) {
            }
        }
    };
    int mDisabled1 = 0;
    int mDisabled2 = 0;
    private int mDisabledUnmodified1;
    private int mDisabledUnmodified2;
    Display mDisplay;
    DisplayMetrics mDisplayMetrics = new DisplayMetrics();
    private DozeScrimController mDozeScrimController;
    private DozeServiceHost mDozeServiceHost;
    private boolean mDozing;
    private boolean mDozingRequested;
    private ExpandableNotificationRow mDraggedDownRow;
    View mExpandedContents;
    boolean mExpandedVisible;
    FingerprintUnlockController mFingerprintUnlockController;
    FlashlightController mFlashlightController;
    private OnClickListener mFloatClickListener = new OnClickListener() {
        public void onClick(View v) {
            PhoneStatusBar.this.mNavigationBarView.getExtensionButton().setTextSize(15.0f);
            PhoneStatusBar.this.awakenDreams();
            PhoneStatusBar.this.toggleFloatApps();
            if (PhoneStatusBar.this.mIsSplitModeEnable) {
                PhoneStatusBar.this.updateFloatModeButton(!PhoneStatusBar.this.mIsSplitModeOn);
                PhoneStatusBar.this.updateSpilitModeButton(PhoneStatusBar.this.mIsSplitModeOn);
            }
        }
    };
    private OnClickListener mFloatModeClickListener = new OnClickListener() {
        public void onClick(View v) {
            Log.v("PhoneStatusBar", "onClick, mFloatModeClickListener=");
            if (PhoneStatusBar.this.mIsSplitModeOn) {
                MultiWindowProxy.getInstance().switchToFloatingMode();
                PhoneStatusBar.this.mIsSplitModeOn = MultiWindowProxy.isSplitMode();
                PhoneStatusBar.this.updateFloatModeButton(!PhoneStatusBar.this.mIsSplitModeOn);
                PhoneStatusBar.this.updateSpilitModeButton(PhoneStatusBar.this.mIsSplitModeOn);
            }
            PhoneStatusBar.this.closeFloatPanel();
        }
    };
    private final GestureRecorder mGestureRec = null;
    private HandlerThread mHandlerThread;
    StatusBarHeaderView mHeader;
    private HashSet<Entry> mHeadsUpEntriesToRemoveOnSwitch = new HashSet();
    private final ContentObserver mHeadsUpObserver = new ContentObserver(this.mHandler) {
        public void onChange(boolean selfChange) {
            boolean z = false;
            boolean wasUsing = PhoneStatusBar.this.mUseHeadsUp;
            PhoneStatusBar phoneStatusBar = PhoneStatusBar.this;
            boolean z2 = PhoneStatusBar.this.mDisableNotificationAlerts ? false : Global.getInt(PhoneStatusBar.this.mContext.getContentResolver(), "heads_up_notifications_enabled", 0) != 0;
            phoneStatusBar.mUseHeadsUp = z2;
            PhoneStatusBar phoneStatusBar2 = PhoneStatusBar.this;
            if (PhoneStatusBar.this.mUseHeadsUp && Global.getInt(PhoneStatusBar.this.mContext.getContentResolver(), "ticker_gets_heads_up", 0) != 0) {
                z = true;
            }
            phoneStatusBar2.mHeadsUpTicker = z;
            Log.d("PhoneStatusBar", "heads up is " + (PhoneStatusBar.this.mUseHeadsUp ? "enabled" : "disabled"));
            if (wasUsing != PhoneStatusBar.this.mUseHeadsUp && !PhoneStatusBar.this.mUseHeadsUp) {
                Log.d("PhoneStatusBar", "dismissing any existing heads up notification on disable event");
                PhoneStatusBar.this.mHeadsUpManager.releaseAllImmediately();
            }
        }
    };
    private Runnable mHideBackdropFront = new Runnable() {
        public void run() {
            PhoneStatusBar.this.mBackdropFront.setVisibility(4);
            PhoneStatusBar.this.mBackdropFront.animate().cancel();
            PhoneStatusBar.this.mBackdropFront.setImageDrawable(null);
        }
    };
    private final OnTouchListener mHomeActionListener = new OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case 1:
                case 3:
                    PhoneStatusBar.this.awakenDreams();
                    break;
            }
            return false;
        }
    };
    HotKnotControllerImpl mHotKnotController;
    HotspotControllerImpl mHotspotController;
    StatusBarIconController mIconController;
    PhoneStatusBarPolicy mIconPolicy;
    private int mInteractingWindows;
    private boolean mIsSplitModeEnable = MultiWindowProxy.isSplitModeEnabled();
    private boolean mIsSplitModeOn = MultiWindowProxy.isSplitMode();
    KeyguardBottomAreaView mKeyguardBottomArea;
    private boolean mKeyguardFadingAway;
    private long mKeyguardFadingAwayDelay;
    private long mKeyguardFadingAwayDuration;
    KeyguardIndicationController mKeyguardIndicationController;
    int mKeyguardMaxNotificationCount;
    KeyguardMonitor mKeyguardMonitor;
    KeyguardStatusBarView mKeyguardStatusBar;
    View mKeyguardStatusView;
    KeyguardUserSwitcher mKeyguardUserSwitcher;
    private ViewMediatorCallback mKeyguardViewMediatorCallback;
    private int mLastDispatchedSystemUiVisibility = -1;
    private long mLastLockToAppLongPress;
    private int mLastLoggedStateFingerprint;
    private long mLastVisibilityReportUptimeMs;
    private RankingMap mLatestRankingMap;
    private Runnable mLaunchTransitionEndRunnable;
    private boolean mLaunchTransitionFadingAway;
    boolean mLeaveOpenOnKeyguardHide;
    private Interpolator mLinearInterpolator = new LinearInterpolator();
    LocationControllerImpl mLocationController;
    private OnLongClickListener mLongPressBackRecentsListener = new OnLongClickListener() {
        public boolean onLongClick(View v) {
            PhoneStatusBar.this.handleLongPressBackRecents(v);
            return true;
        }
    };
    private final OnLongClickListener mLongPressHomeListener = new OnLongClickListener() {
        public boolean onLongClick(View v) {
            if (PhoneStatusBar.this.shouldDisableNavbarGestures()) {
                return false;
            }
            PhoneStatusBar.this.mAssistManager.startAssist(new Bundle());
            PhoneStatusBar.this.awakenDreams();
            if (PhoneStatusBar.this.mNavigationBarView != null) {
                PhoneStatusBar.this.mNavigationBarView.abortCurrentGesture();
            }
            return true;
        }
    };
    private MediaController mMediaController;
    private Callback mMediaListener = new Callback() {
        public void onPlaybackStateChanged(PlaybackState state) {
            super.onPlaybackStateChanged(state);
            if (state != null && !PhoneStatusBar.this.isPlaybackActive(state.getState())) {
                PhoneStatusBar.this.clearCurrentMediaNotification();
                PhoneStatusBar.this.updateMediaMetaData(true);
            }
        }

        public void onMetadataChanged(MediaMetadata metadata) {
            super.onMetadataChanged(metadata);
            PhoneStatusBar.this.mMediaMetadata = metadata;
            PhoneStatusBar.this.updateMediaMetaData(true);
        }
    };
    private MediaMetadata mMediaMetadata;
    private String mMediaNotificationKey;
    private MediaSessionManager mMediaSessionManager;
    int mNaturalBarHeight = -1;
    private int mNavigationBarMode;
    private int mNavigationBarWindowState = 0;
    private int mNavigationIconHints = 0;
    NetworkControllerImpl mNetworkController;
    NextAlarmController mNextAlarmController;
    private boolean mNoAnimationOnNextBarModeChange;
    private final OnChildLocationsChangedListener mNotificationLocationsChangedListener = new OnChildLocationsChangedListener() {
        public void onChildLocationsChanged(NotificationStackScrollLayout stackScrollLayout) {
            if (!PhoneStatusBar.this.mHandler.hasCallbacks(PhoneStatusBar.this.mVisibilityReporter)) {
                PhoneStatusBar.this.mHandler.postAtTime(PhoneStatusBar.this.mVisibilityReporter, PhoneStatusBar.this.mLastVisibilityReportUptimeMs + 500);
            }
        }
    };
    NotificationPanelView mNotificationPanel;
    private final OnChildLocationsChangedListener mOnChildLocationsChangedListener = new OnChildLocationsChangedListener() {
        public void onChildLocationsChanged(NotificationStackScrollLayout stackScrollLayout) {
            PhoneStatusBar.this.userActivity();
        }
    };
    private final OnClickListener mOverflowClickListener = new OnClickListener() {
        public void onClick(View v) {
            PhoneStatusBar.this.goToLockedShade(null);
        }
    };
    int mPixelFormat;
    ArrayList<Runnable> mPostCollapseRunnables = new ArrayList();
    private QSPanel mQSPanel;
    Object mQueueLock = new Object();
    private OnClickListener mRecentsClickListener = new OnClickListener() {
        public void onClick(View v) {
            PhoneStatusBar.this.awakenDreams();
            PhoneStatusBar.this.toggleRecentApps();
        }
    };
    RotationLockControllerImpl mRotationLockController;
    private ScreenPinningRequest mScreenPinningRequest;
    ScreenRecordControllerImpl mScreenRecordController;
    private ScrimController mScrimController;
    private boolean mScrimSrcModeEnabled;
    SecurityControllerImpl mSecurityController;
    private final ShadeUpdates mShadeUpdates = new ShadeUpdates();
    private OnClickListener mSplitModeClickListener = new OnClickListener() {
        public void onClick(View v) {
            if (!PhoneStatusBar.this.mIsSplitModeOn) {
                MultiWindowProxy.getInstance().switchToSplitMode();
                PhoneStatusBar.this.mIsSplitModeOn = MultiWindowProxy.isSplitMode();
                PhoneStatusBar.this.updateFloatModeButton(!PhoneStatusBar.this.mIsSplitModeOn);
                PhoneStatusBar.this.updateSpilitModeButton(PhoneStatusBar.this.mIsSplitModeOn);
            }
            PhoneStatusBar.this.closeFloatPanel();
        }
    };
    private PorterDuffXfermode mSrcOverXferMode = new PorterDuffXfermode(Mode.SRC_OVER);
    private PorterDuffXfermode mSrcXferMode = new PorterDuffXfermode(Mode.SRC);
    Runnable mStartTracing = new Runnable() {
        public void run() {
            PhoneStatusBar.this.vibrate();
            SystemClock.sleep(250);
            Log.d("PhoneStatusBar", "startTracing");
            Debug.startMethodTracing("/data/statusbar-traces/trace");
            PhoneStatusBar.this.mHandler.postDelayed(PhoneStatusBar.this.mStopTracing, 10000);
        }
    };
    private int mStatusBarMode;
    private IStatusBarPlmnPlugin mStatusBarPlmnPlugin = null;
    PhoneStatusBarView mStatusBarView;
    StatusBarWindowView mStatusBarWindow;
    private StatusBarWindowManager mStatusBarWindowManager;
    private int mStatusBarWindowState = 0;
    Runnable mStopTracing = new Runnable() {
        public void run() {
            Debug.stopMethodTracing();
            Log.d("PhoneStatusBar", "stopTracing");
            PhoneStatusBar.this.vibrate();
        }
    };
    SuperScreenShotControllerImpl mSuperScreenShotController;
    int mSystemUiVisibility = 0;
    private HashMap<ExpandableNotificationRow, List<ExpandableNotificationRow>> mTmpChildOrderMap = new HashMap();
    boolean mTracking;
    int mTrackingPosition;
    private UnlockMethodCache mUnlockMethodCache;
    UserInfoController mUserInfoController;
    private boolean mUserSetup = false;
    private ContentObserver mUserSetupObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            boolean userSetup = Secure.getIntForUser(PhoneStatusBar.this.mContext.getContentResolver(), "user_setup_complete", 0, PhoneStatusBar.this.mCurrentUserId) != 0;
            if (userSetup != PhoneStatusBar.this.mUserSetup) {
                PhoneStatusBar.this.mUserSetup = userSetup;
                if (!(PhoneStatusBar.this.mUserSetup || PhoneStatusBar.this.mStatusBarView == null)) {
                    PhoneStatusBar.this.animateCollapseQuickSettings();
                }
                if (PhoneStatusBar.this.mKeyguardBottomArea != null) {
                    PhoneStatusBar.this.mKeyguardBottomArea.setUserSetupComplete(PhoneStatusBar.this.mUserSetup);
                }
            }
            if (PhoneStatusBar.this.mIconPolicy != null) {
                PhoneStatusBar.this.mIconPolicy.setCurrentUserSetup(PhoneStatusBar.this.mUserSetup);
            }
        }
    };
    UserSwitcherController mUserSwitcherController;
    private final Runnable mVisibilityReporter = new Runnable() {
        private final ArraySet<NotificationVisibility> mTmpCurrentlyVisibleNotifications = new ArraySet();
        private final ArraySet<NotificationVisibility> mTmpNewlyVisibleNotifications = new ArraySet();
        private final ArraySet<NotificationVisibility> mTmpNoLongerVisibleNotifications = new ArraySet();

        public void run() {
            PhoneStatusBar.this.mLastVisibilityReportUptimeMs = SystemClock.uptimeMillis();
            String mediaKey = PhoneStatusBar.this.getCurrentMediaNotificationKey();
            ArrayList<Entry> activeNotifications = PhoneStatusBar.this.mNotificationData.getActiveNotifications();
            int N = activeNotifications.size();
            for (int i = 0; i < N; i++) {
                Entry entry = (Entry) activeNotifications.get(i);
                String key = entry.notification.getKey();
                boolean isVisible = (PhoneStatusBar.this.mStackScroller.getChildLocation(entry.row) & 9) != 0;
                NotificationVisibility visObj = NotificationVisibility.obtain(key, i, isVisible);
                boolean previouslyVisible = PhoneStatusBar.this.mCurrentlyVisibleNotifications.contains(visObj);
                if (isVisible) {
                    this.mTmpCurrentlyVisibleNotifications.add(visObj);
                    if (!previouslyVisible) {
                        this.mTmpNewlyVisibleNotifications.add(visObj);
                    }
                } else {
                    visObj.recycle();
                }
            }
            this.mTmpNoLongerVisibleNotifications.addAll(PhoneStatusBar.this.mCurrentlyVisibleNotifications);
            this.mTmpNoLongerVisibleNotifications.removeAll(this.mTmpCurrentlyVisibleNotifications);
            PhoneStatusBar.this.logNotificationVisibilityChanges(this.mTmpNewlyVisibleNotifications, this.mTmpNoLongerVisibleNotifications);
            PhoneStatusBar.this.recycleAllVisibilityObjects(PhoneStatusBar.this.mCurrentlyVisibleNotifications);
            PhoneStatusBar.this.mCurrentlyVisibleNotifications.addAll(this.mTmpCurrentlyVisibleNotifications);
            PhoneStatusBar.this.recycleAllVisibilityObjects(this.mTmpNoLongerVisibleNotifications);
            this.mTmpCurrentlyVisibleNotifications.clear();
            this.mTmpNewlyVisibleNotifications.clear();
            this.mTmpNoLongerVisibleNotifications.clear();
        }
    };
    VolumeComponent mVolumeComponent;
    private boolean mWaitingForKeyguardExit;
    private boolean mWakeUpComingFromTouch;
    private PointF mWakeUpTouchLocation;
    ZenModeController mZenModeController;
    private boolean showBatterPercent = true;

    private final class DozeServiceHost extends KeyguardUpdateMonitorCallback implements DozeHost {
        private final ArrayList<DozeHost.Callback> mCallbacks;
        private final H mHandler;
        private boolean mNotificationLightOn;

        private final class H extends Handler {
            private H() {
            }

            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        DozeServiceHost.this.handleStartDozing((Runnable) msg.obj);
                        return;
                    case 2:
                        DozeServiceHost.this.handlePulseWhileDozing((PulseCallback) msg.obj, msg.arg1);
                        return;
                    case 3:
                        DozeServiceHost.this.handleStopDozing();
                        return;
                    default:
                        return;
                }
            }
        }

        private DozeServiceHost() {
            this.mCallbacks = new ArrayList();
            this.mHandler = new H();
        }

        public String toString() {
            return "PSB.DozeServiceHost[mCallbacks=" + this.mCallbacks.size() + "]";
        }

        public void firePowerSaveChanged(boolean active) {
            for (DozeHost.Callback callback : this.mCallbacks) {
                callback.onPowerSaveChanged(active);
            }
        }

        public void fireBuzzBeepBlinked() {
            for (DozeHost.Callback callback : this.mCallbacks) {
                callback.onBuzzBeepBlinked();
            }
        }

        public void fireNotificationLight(boolean on) {
            this.mNotificationLightOn = on;
            for (DozeHost.Callback callback : this.mCallbacks) {
                callback.onNotificationLight(on);
            }
        }

        public void fireNewNotifications() {
            for (DozeHost.Callback callback : this.mCallbacks) {
                callback.onNewNotifications();
            }
        }

        public void addCallback(DozeHost.Callback callback) {
            this.mCallbacks.add(callback);
        }

        public void removeCallback(DozeHost.Callback callback) {
            this.mCallbacks.remove(callback);
        }

        public void startDozing(Runnable ready) {
            this.mHandler.obtainMessage(1, ready).sendToTarget();
        }

        public void pulseWhileDozing(PulseCallback callback, int reason) {
            this.mHandler.obtainMessage(2, reason, 0, callback).sendToTarget();
        }

        public void stopDozing() {
            this.mHandler.obtainMessage(3).sendToTarget();
        }

        public boolean isPowerSaveActive() {
            return PhoneStatusBar.this.mBatteryController != null ? PhoneStatusBar.this.mBatteryController.isPowerSave() : false;
        }

        public boolean isPulsingBlocked() {
            return PhoneStatusBar.this.mFingerprintUnlockController.getMode() == 1;
        }

        public boolean isNotificationLightOn() {
            return this.mNotificationLightOn;
        }

        private void handleStartDozing(Runnable ready) {
            if (!PhoneStatusBar.this.mDozingRequested) {
                PhoneStatusBar.this.mDozingRequested = true;
                DozeLog.traceDozing(PhoneStatusBar.this.mContext, PhoneStatusBar.this.mDozing);
                PhoneStatusBar.this.updateDozing();
            }
            ready.run();
        }

        private void handlePulseWhileDozing(PulseCallback callback, int reason) {
            PhoneStatusBar.this.mDozeScrimController.pulse(callback, reason);
        }

        private void handleStopDozing() {
            if (PhoneStatusBar.this.mDozingRequested) {
                PhoneStatusBar.this.mDozingRequested = false;
                DozeLog.traceDozing(PhoneStatusBar.this.mContext, PhoneStatusBar.this.mDozing);
                PhoneStatusBar.this.updateDozing();
            }
        }
    }

    private static class FastColorDrawable extends Drawable {
        private final int mColor;

        public FastColorDrawable(int color) {
            this.mColor = -16777216 | color;
        }

        public void draw(Canvas canvas) {
            canvas.drawColor(this.mColor, Mode.SRC);
        }

        public void setAlpha(int alpha) {
        }

        public void setColorFilter(ColorFilter colorFilter) {
        }

        public int getOpacity() {
            return -1;
        }

        public void setBounds(int left, int top, int right, int bottom) {
        }

        public void setBounds(Rect bounds) {
        }
    }

    private class H extends H {
        private H() {
            super();
        }

        public void handleMessage(Message m) {
            super.handleMessage(m);
            switch (m.what) {
                case EVENT.DYNAMIC_PACK_EVENT_BASE /*1000*/:
                    PhoneStatusBar.this.animateExpandNotificationsPanel();
                    return;
                case 1001:
                    PhoneStatusBar.this.animateCollapsePanels();
                    return;
                case 1002:
                    PhoneStatusBar.this.animateExpandSettingsPanel();
                    return;
                case 1003:
                    PhoneStatusBar.this.onLaunchTransitionTimeout();
                    return;
                default:
                    return;
            }
        }
    }

    public class MWSystemUiCallback extends Stub {
        public void showRestoreButton(boolean flag) {
            PhoneStatusBar.this.showRestoreButtonInner(flag);
        }
    }

    private final class ShadeUpdates {
        private final ArraySet<String> mNewVisibleNotifications;
        private final ArraySet<String> mVisibleNotifications;

        private ShadeUpdates() {
            this.mVisibleNotifications = new ArraySet();
            this.mNewVisibleNotifications = new ArraySet();
        }

        public void check() {
            this.mNewVisibleNotifications.clear();
            ArrayList<Entry> activeNotifications = PhoneStatusBar.this.mNotificationData.getActiveNotifications();
            for (int i = 0; i < activeNotifications.size(); i++) {
                Entry entry = (Entry) activeNotifications.get(i);
                boolean visible = entry.row != null ? entry.row.getVisibility() == 0 : false;
                if (visible) {
                    this.mNewVisibleNotifications.add(entry.key + entry.notification.getPostTime());
                }
            }
            boolean updates = !this.mVisibleNotifications.containsAll(this.mNewVisibleNotifications);
            this.mVisibleNotifications.clear();
            this.mVisibleNotifications.addAll(this.mNewVisibleNotifications);
            if (updates && PhoneStatusBar.this.mDozeServiceHost != null) {
                PhoneStatusBar.this.mDozeServiceHost.fireNewNotifications();
            }
        }
    }

    public void setInteracting(int r1, boolean r2) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: com.android.systemui.statusbar.phone.PhoneStatusBar.setInteracting(int, boolean):void
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:59)
	at jadx.core.ProcessClass.process(ProcessClass.java:42)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:306)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 6 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.PhoneStatusBar.setInteracting(int, boolean):void");
    }

    public void setSystemUiVisibility(int r1, int r2) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: com.android.systemui.statusbar.phone.PhoneStatusBar.setSystemUiVisibility(int, int):void
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:59)
	at jadx.core.ProcessClass.process(ProcessClass.java:42)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:306)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 6 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.PhoneStatusBar.setSystemUiVisibility(int, int):void");
    }

    static {
        boolean isOnlyCoreApps;
        try {
            isOnlyCoreApps = IPackageManager.Stub.asInterface(ServiceManager.getService("package")).isOnlyCoreApps();
        } catch (RemoteException e) {
            isOnlyCoreApps = false;
        }
        ONLY_CORE_APPS = isOnlyCoreApps;
    }

    private void recycleAllVisibilityObjects(ArraySet<NotificationVisibility> array) {
        int N = array.size();
        for (int i = 0; i < N; i++) {
            ((NotificationVisibility) array.valueAt(i)).recycle();
        }
        array.clear();
    }

    public void start() {
        this.mDisplay = ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay();
        updateDisplaySize();
        this.mScrimSrcModeEnabled = this.mContext.getResources().getBoolean(R.bool.config_status_bar_scrim_behind_use_src);
        super.start();
        this.mMediaSessionManager = (MediaSessionManager) this.mContext.getSystemService("media_session");
        addNavigationBar();
        this.mIconPolicy = new PhoneStatusBarPolicy(this.mContext, this.mCastController, this.mHotspotController, this.mUserInfoController, this.mBluetoothController);
        this.mIconPolicy.setCurrentUserSetup(this.mUserSetup);
        this.mSettingsObserver.onChange(false);
        this.mHeadsUpObserver.onChange(true);
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("heads_up_notifications_enabled"), true, this.mHeadsUpObserver);
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("ticker_gets_heads_up"), true, this.mHeadsUpObserver);
        this.mUnlockMethodCache = UnlockMethodCache.getInstance(this.mContext);
        this.mUnlockMethodCache.addListener(this);
        startKeyguard();
        this.mDozeServiceHost = new DozeServiceHost();
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mDozeServiceHost);
        putComponent(DozeHost.class, this.mDozeServiceHost);
        putComponent(PhoneStatusBar.class, this);
        if (MultiWindowProxy.isSupported()) {
            registerMWProxyAgain();
        }
        setControllerUsers();
        notifyUserAboutHiddenNotifications();
        this.mScreenPinningRequest = new ScreenPinningRequest(this.mContext);
    }

    protected PhoneStatusBarView makeStatusBarView() {
        Context context = this.mContext;
        Resources res = context.getResources();
        updateDisplaySize();
        updateResources();
        this.mStatusBarWindow = (StatusBarWindowView) View.inflate(context, R.layout.super_status_bar, null);
        this.mStatusBarWindow.setService(this);
        this.mStatusBarWindow.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                PhoneStatusBar.this.checkUserAutohide(v, event);
                if (event.getAction() == 0 && PhoneStatusBar.this.mExpandedVisible) {
                    PhoneStatusBar.this.animateCollapsePanels();
                }
                return PhoneStatusBar.this.mStatusBarWindow.onTouchEvent(event);
            }
        });
        this.mStatusBarView = (PhoneStatusBarView) this.mStatusBarWindow.findViewById(R.id.status_bar);
        this.mStatusBarView.setBar(this);
        this.mStatusBarView.setPanelHolder((PanelHolder) this.mStatusBarWindow.findViewById(R.id.panel_holder));
        this.mNotificationPanel = (NotificationPanelView) this.mStatusBarWindow.findViewById(R.id.notification_panel);
        this.mNotificationPanel.setStatusBar(this);
        if (!(ActivityManager.isHighEndGfx() || FeatureOptions.LOW_RAM_SUPPORT)) {
            this.mStatusBarWindow.setBackground(null);
            this.mNotificationPanel.setBackground(new FastColorDrawable(context.getColor(R.color.notification_panel_solid_background)));
        }
        this.mHeadsUpManager = new HeadsUpManager(context, this.mStatusBarWindow);
        this.mHeadsUpManager.setBar(this);
        this.mHeadsUpManager.addListener(this);
        this.mHeadsUpManager.addListener(this.mNotificationPanel);
        this.mNotificationPanel.setHeadsUpManager(this.mHeadsUpManager);
        this.mNotificationData.setHeadsUpManager(this.mHeadsUpManager);
        try {
            boolean showNav = this.mWindowManagerService.hasNavigationBar();
            Log.v("PhoneStatusBar", "hasNavigationBar=" + showNav);
            if (showNav) {
                int layoutId = R.layout.navigation_bar;
                if (MultiWindowProxy.isSupported()) {
                    layoutId = R.layout.navigation_bar_float_window;
                }
                this.mNavigationBarView = (NavigationBarView) View.inflate(context, layoutId, null);
                this.mNavigationBarView.setDisabledFlags(this.mDisabled1);
                this.mNavigationBarView.setBar(this);
                this.mNavigationBarView.setOnVerticalChangedListener(new OnVerticalChangedListener() {
                    public void onVerticalChanged(boolean isVertical) {
                        if (PhoneStatusBar.this.mAssistManager != null) {
                            PhoneStatusBar.this.mAssistManager.onConfigurationChanged();
                        }
                        PhoneStatusBar.this.mNotificationPanel.setQsScrimEnabled(!isVertical);
                    }
                });
                this.mNavigationBarView.setOnTouchListener(new OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent event) {
                        PhoneStatusBar.this.checkUserAutohide(v, event);
                        return false;
                    }
                });
            }
        } catch (RemoteException e) {
        }
        this.mAssistManager = new AssistManager(this, context);
        this.mAssistManager.onConfigurationChanged();
        this.mPixelFormat = -1;
        this.mStackScroller = (NotificationStackScrollLayout) this.mStatusBarWindow.findViewById(R.id.notification_stack_scroller);
        this.mStackScroller.setLongPressListener(getNotificationLongClicker());
        this.mStackScroller.setPhoneStatusBar(this);
        this.mStackScroller.setGroupManager(this.mGroupManager);
        this.mStackScroller.setHeadsUpManager(this.mHeadsUpManager);
        this.mGroupManager.setOnGroupChangeListener(this.mStackScroller);
        this.mKeyguardIconOverflowContainer = (NotificationOverflowContainer) LayoutInflater.from(this.mContext).inflate(R.layout.status_bar_notification_keyguard_overflow, this.mStackScroller, false);
        this.mKeyguardIconOverflowContainer.setOnActivatedListener(this);
        this.mKeyguardIconOverflowContainer.setOnClickListener(this.mOverflowClickListener);
        this.mStackScroller.setOverflowContainer(this.mKeyguardIconOverflowContainer);
        this.mStackScroller.setSpeedBumpView((SpeedBumpView) LayoutInflater.from(this.mContext).inflate(R.layout.status_bar_notification_speed_bump, this.mStackScroller, false));
        this.mEmptyShadeView = (EmptyShadeView) LayoutInflater.from(this.mContext).inflate(R.layout.status_bar_no_notifications, this.mStackScroller, false);
        this.mStackScroller.setEmptyShadeView(this.mEmptyShadeView);
        this.mDismissView = (DismissView) LayoutInflater.from(this.mContext).inflate(R.layout.status_bar_notification_dismiss_all, this.mStackScroller, false);
        this.mDismissView.setOnButtonClickListener(new OnClickListener() {
            public void onClick(View v) {
                MetricsLogger.action(PhoneStatusBar.this.mContext, 148);
                PhoneStatusBar.this.clearAllNotifications();
            }
        });
        this.mStackScroller.setDismissView(this.mDismissView);
        this.mExpandedContents = this.mStackScroller;
        this.mBackdrop = (BackDropView) this.mStatusBarWindow.findViewById(R.id.backdrop);
        this.mBackdropFront = (ImageView) this.mBackdrop.findViewById(R.id.backdrop_front);
        this.mBackdropBack = (ImageView) this.mBackdrop.findViewById(R.id.backdrop_back);
        this.mScrimController = new ScrimController((ScrimView) this.mStatusBarWindow.findViewById(R.id.scrim_behind), (ScrimView) this.mStatusBarWindow.findViewById(R.id.scrim_in_front), this.mStatusBarWindow.findViewById(R.id.heads_up_scrim), this.mScrimSrcModeEnabled);
        this.mHeadsUpManager.addListener(this.mScrimController);
        this.mStackScroller.setScrimController(this.mScrimController);
        this.mScrimController.setBackDropView(this.mBackdrop);
        this.mStatusBarView.setScrimController(this.mScrimController);
        this.mDozeScrimController = new DozeScrimController(this.mScrimController, context);
        this.mHeader = (StatusBarHeaderView) this.mStatusBarWindow.findViewById(R.id.header);
        this.mHeader.setActivityStarter(this);
        this.mKeyguardStatusBar = (KeyguardStatusBarView) this.mStatusBarWindow.findViewById(R.id.keyguard_header);
        this.mKeyguardStatusView = this.mStatusBarWindow.findViewById(R.id.keyguard_status_view);
        this.mKeyguardBottomArea = (KeyguardBottomAreaView) this.mStatusBarWindow.findViewById(R.id.keyguard_bottom_area);
        this.mKeyguardBottomArea.setActivityStarter(this);
        this.mKeyguardBottomArea.setAssistManager(this.mAssistManager);
        this.mKeyguardIndicationController = new KeyguardIndicationController(this.mContext, (KeyguardIndicationTextView) this.mStatusBarWindow.findViewById(R.id.keyguard_indication_text), this.mKeyguardBottomArea.getLockIcon());
        this.mKeyguardBottomArea.setKeyguardIndicationController(this.mKeyguardIndicationController);
        setAreThereNotifications();
        this.mIconController = new StatusBarIconController(this.mContext, this.mStatusBarView, this.mKeyguardStatusBar, this);
        this.mHandlerThread = new HandlerThread("PhoneStatusBar", 10);
        this.mHandlerThread.start();
        this.mSuperScreenShotController = new SuperScreenShotControllerImpl(this.mContext);
        this.mScreenRecordController = new ScreenRecordControllerImpl(this.mContext);
        this.mLocationController = new LocationControllerImpl(this.mContext, this.mHandlerThread.getLooper());
        this.mBatteryController = new BatteryController(this.mContext);
        this.mBatteryLevel = (TextView) this.mStatusBarView.findViewById(R.id.battery_level);
        this.mBatteryController.addStateChangedCallback(new BatteryStateChangeCallback() {
            public void onPowerSaveChanged() {
                PhoneStatusBar.this.mHandler.post(PhoneStatusBar.this.mCheckBarModes);
                if (PhoneStatusBar.this.mDozeServiceHost != null) {
                    PhoneStatusBar.this.mDozeServiceHost.firePowerSaveChanged(PhoneStatusBar.this.mBatteryController.isPowerSave());
                }
            }

            public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
                if (PhoneStatusBar.this.mBatteryLevel != null && PhoneStatusBar.this.showBatterPercent && System.getInt(PhoneStatusBar.this.mContext.getContentResolver(), "status_bar_show_battery_percent", PhoneStatusBar.this.defaultShowBattery) != 0) {
                    PhoneStatusBar.this.mBatteryLevel.setVisibility(0);
                    PhoneStatusBar.this.mBatteryLevel.setText(NumberFormat.getPercentInstance().format(((double) level) / 100.0d));
                } else if (PhoneStatusBar.this.mBatteryLevel != null) {
                    PhoneStatusBar.this.mBatteryLevel.setVisibility(8);
                }
            }
        });
        this.mNetworkController = new NetworkControllerImpl(this.mContext, this.mHandlerThread.getLooper());
        this.mHotspotController = new HotspotControllerImpl(this.mContext);
        this.mBluetoothController = new BluetoothControllerImpl(this.mContext, this.mHandlerThread.getLooper());
        this.mSecurityController = new SecurityControllerImpl(this.mContext);
        if (SIMHelper.isMtkHotKnotSupport()) {
            Log.d("PhoneStatusBar", "makeStatusBarView : HotKnotControllerImpl");
            this.mHotKnotController = new HotKnotControllerImpl(this.mContext);
        } else {
            this.mHotKnotController = null;
        }
        if (SIMHelper.isMtkAudioProfilesSupport()) {
            Log.d("PhoneStatusBar", "makeStatusBarView : AudioProfileControllerImpl");
            this.mAudioProfileController = new AudioProfileControllerImpl(this.mContext);
        } else {
            this.mAudioProfileController = null;
        }
        SIMHelper.setContext(this.mContext);
        if (SIMHelper.isWifiOnlyDevice()) {
            this.mDataConnectionController = null;
        } else {
            Log.d("PhoneStatusBar", "makeStatusBarView : DataConnectionControllerImpl");
            this.mDataConnectionController = new DataConnectionControllerImpl(this.mContext);
        }
        if (this.mContext.getResources().getBoolean(R.bool.config_showRotationLock)) {
            this.mRotationLockController = new RotationLockControllerImpl(this.mContext);
        }
        this.mUserInfoController = new UserInfoController(this.mContext);
        this.mVolumeComponent = (VolumeComponent) getComponent(VolumeComponent.class);
        if (this.mVolumeComponent != null) {
            this.mZenModeController = this.mVolumeComponent.getZenController();
        }
        Log.d("PhoneStatusBar", "makeStatusBarView : CastControllerImpl +");
        this.mCastController = new CastControllerImpl(this.mContext);
        Log.d("PhoneStatusBar", "makeStatusBarView : CastControllerImpl -");
        SignalClusterView signalCluster = (SignalClusterView) this.mStatusBarView.findViewById(R.id.signal_cluster);
        SignalClusterView signalClusterKeyguard = (SignalClusterView) this.mKeyguardStatusBar.findViewById(R.id.signal_cluster);
        SignalClusterView signalClusterQs = (SignalClusterView) this.mHeader.findViewById(R.id.signal_cluster);
        this.mNetworkController.addSignalCallback(signalCluster);
        this.mNetworkController.addSignalCallback(signalClusterKeyguard);
        this.mNetworkController.addSignalCallback(signalClusterQs);
        signalCluster.setSecurityController(this.mSecurityController);
        signalCluster.setNetworkController(this.mNetworkController);
        signalClusterKeyguard.setSecurityController(this.mSecurityController);
        signalClusterKeyguard.setNetworkController(this.mNetworkController);
        signalClusterQs.setSecurityController(this.mSecurityController);
        signalClusterQs.setNetworkController(this.mNetworkController);
        if (this.mNetworkController.hasVoiceCallingFeature()) {
            this.mNetworkController.addEmergencyListener(this.mHeader);
        }
        this.mStatusBarPlmnPlugin = PluginFactory.getStatusBarPlmnPlugin(context);
        if (supportCustomizeCarrierLabel()) {
            this.mCustomizeCarrierLabel = this.mStatusBarPlmnPlugin.customizeCarrierLabel(this.mNotificationPanel, null);
        }
        this.mFlashlightController = new FlashlightController(this.mContext);
        this.mKeyguardBottomArea.setFlashlightController(this.mFlashlightController);
        this.mKeyguardBottomArea.setPhoneStatusBar(this);
        this.mKeyguardBottomArea.setUserSetupComplete(this.mUserSetup);
        this.mAccessibilityController = new AccessibilityController(this.mContext);
        this.mKeyguardBottomArea.setAccessibilityController(this.mAccessibilityController);
        this.mNextAlarmController = new NextAlarmController(this.mContext);
        this.mKeyguardMonitor = new KeyguardMonitor(this.mContext);
        if (UserSwitcherController.isUserSwitcherAvailable(UserManager.get(this.mContext))) {
            this.mUserSwitcherController = new UserSwitcherController(this.mContext, this.mKeyguardMonitor, this.mHandler);
        }
        this.mKeyguardUserSwitcher = new KeyguardUserSwitcher(this.mContext, (ViewStub) this.mStatusBarWindow.findViewById(R.id.keyguard_user_switcher), this.mKeyguardStatusBar, this.mNotificationPanel, this.mUserSwitcherController);
        this.mQSPanel = (QSPanel) this.mStatusBarWindow.findViewById(R.id.quick_settings_panel);
        if (this.mQSPanel != null) {
            final QSTileHost qsh = new QSTileHost(this.mContext, this, this.mBluetoothController, this.mLocationController, this.mRotationLockController, this.mNetworkController, this.mZenModeController, this.mHotspotController, this.mCastController, this.mFlashlightController, this.mUserSwitcherController, this.mKeyguardMonitor, this.mSecurityController, this.mHotKnotController, this.mDataConnectionController, this.mSuperScreenShotController, this.mScreenRecordController, this.mAudioProfileController);
            this.mQSPanel.setHost(qsh);
            this.mQSPanel.setTiles(qsh.getTiles());
            this.mBrightnessMirrorController = new BrightnessMirrorController(this.mStatusBarWindow);
            this.mQSPanel.setBrightnessMirror(this.mBrightnessMirrorController);
            this.mHeader.setQSPanel(this.mQSPanel);
            qsh.setCallback(new Host.Callback() {
                public void onTilesChanged() {
                    PhoneStatusBar.this.mQSPanel.setTiles(qsh.getTiles());
                }
            });
        }
        this.mHeader.setUserInfoController(this.mUserInfoController);
        this.mKeyguardStatusBar.setUserInfoController(this.mUserInfoController);
        this.mKeyguardStatusBar.setUserSwitcherController(this.mUserSwitcherController);
        this.mUserInfoController.reloadUserInfo();
        this.mHeader.setBatteryController(this.mBatteryController);
        ((BatteryMeterView) this.mStatusBarView.findViewById(R.id.battery)).setBatteryController(this.mBatteryController);
        this.mKeyguardStatusBar.setBatteryController(this.mBatteryController);
        this.mHeader.setNextAlarmController(this.mNextAlarmController);
        this.mBatteryMeterView = (BatteryMeterView) this.mStatusBarView.findViewById(R.id.battery);
        if (this.mBatteryLevel != null && this.showBatterPercent && System.getInt(this.mContext.getContentResolver(), "status_bar_show_battery_percent", this.defaultShowBattery) != 0) {
            this.mBatteryLevel.setVisibility(0);
            this.mBatteryLevel.setText(NumberFormat.getPercentInstance().format(((double) this.mBatteryMeterView.getLevel()) / 100.0d));
        } else if (this.mBatteryLevel != null) {
            this.mBatteryLevel.setVisibility(8);
        }
        this.mBroadcastReceiver.onReceive(this.mContext, new Intent(((PowerManager) this.mContext.getSystemService("power")).isScreenOn() ? "android.intent.action.SCREEN_ON" : "android.intent.action.SCREEN_OFF"));
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("com.intent.action.BATTERY_PERCENTAGE_SWITCH");
        context.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, filter, null, null);
        IntentFilter demoFilter = new IntentFilter();
        demoFilter.addAction("com.android.systemui.demo");
        context.registerReceiverAsUser(this.mDemoReceiver, UserHandle.ALL, demoFilter, "android.permission.DUMP", null);
        resetUserSetupObserver();
        ThreadedRenderer.overrideProperty("disableProfileBars", "true");
        ThreadedRenderer.overrideProperty("ambientRatio", String.valueOf(1.5f));
        this.mStatusBarPlmnPlugin.addPlmn((LinearLayout) this.mStatusBarView.findViewById(R.id.status_bar_contents), this.mContext);
        return this.mStatusBarView;
    }

    private void clearAllNotifications() {
        int numChildren = this.mStackScroller.getChildCount();
        ArrayList<View> viewsToHide = new ArrayList(numChildren);
        for (int i = 0; i < numChildren; i++) {
            View child = this.mStackScroller.getChildAt(i);
            if (child instanceof ExpandableNotificationRow) {
                if (this.mStackScroller.canChildBeDismissed(child) && child.getVisibility() == 0) {
                    viewsToHide.add(child);
                }
                ExpandableNotificationRow row = (ExpandableNotificationRow) child;
                List<ExpandableNotificationRow> children = row.getNotificationChildren();
                if (row.areChildrenExpanded() && children != null) {
                    for (ExpandableNotificationRow childRow : children) {
                        if (childRow.getVisibility() == 0) {
                            viewsToHide.add(childRow);
                        }
                    }
                }
            }
        }
        if (viewsToHide.isEmpty()) {
            animateCollapsePanels(0);
            return;
        }
        addPostCollapseAction(new Runnable() {
            public void run() {
                PhoneStatusBar.this.mStackScroller.setDismissAllInProgress(false);
                try {
                    PhoneStatusBar.this.mBarService.onClearAllNotifications(PhoneStatusBar.this.mCurrentUserId);
                } catch (Exception e) {
                }
            }
        });
        performDismissAllAnimations(viewsToHide);
    }

    private void performDismissAllAnimations(ArrayList<View> hideAnimatedList) {
        Runnable animationFinishAction = new Runnable() {
            public void run() {
                PhoneStatusBar.this.animateCollapsePanels(0);
            }
        };
        this.mStackScroller.setDismissAllInProgress(true);
        int currentDelay = 140;
        int totalDelay = 180;
        for (int i = hideAnimatedList.size() - 1; i >= 0; i--) {
            View view = (View) hideAnimatedList.get(i);
            Runnable endRunnable = null;
            if (i == 0) {
                endRunnable = animationFinishAction;
            }
            this.mStackScroller.dismissViewAnimated(view, endRunnable, totalDelay, 260);
            currentDelay = Math.max(50, currentDelay - 10);
            totalDelay += currentDelay;
        }
    }

    protected void setZenMode(int mode) {
        super.setZenMode(mode);
        if (this.mIconPolicy != null) {
            this.mIconPolicy.setZenMode(mode);
        }
    }

    private void startKeyguard() {
        KeyguardViewMediator keyguardViewMediator = (KeyguardViewMediator) getComponent(KeyguardViewMediator.class);
        this.mFingerprintUnlockController = new FingerprintUnlockController(this.mContext, this.mStatusBarWindowManager, this.mDozeScrimController, keyguardViewMediator, this.mScrimController, this);
        this.mStatusBarKeyguardViewManager = keyguardViewMediator.registerStatusBar(this, this.mStatusBarWindow, this.mStatusBarWindowManager, this.mScrimController, this.mFingerprintUnlockController);
        this.mKeyguardIndicationController.setStatusBarKeyguardViewManager(this.mStatusBarKeyguardViewManager);
        this.mFingerprintUnlockController.setStatusBarKeyguardViewManager(this.mStatusBarKeyguardViewManager);
        this.mKeyguardViewMediatorCallback = keyguardViewMediator.getViewMediatorCallback();
    }

    protected View getStatusBarView() {
        return this.mStatusBarView;
    }

    public StatusBarWindowView getStatusBarWindow() {
        return this.mStatusBarWindow;
    }

    public int getStatusBarHeight() {
        if (this.mNaturalBarHeight < 0) {
            this.mNaturalBarHeight = this.mContext.getResources().getDimensionPixelSize(17104919);
        }
        return this.mNaturalBarHeight;
    }

    private void awakenDreams() {
        if (this.mDreamManager != null) {
            try {
                this.mDreamManager.awaken();
            } catch (RemoteException e) {
            }
        }
    }

    private void prepareNavigationBarView() {
        this.mNavigationBarView.reorient();
        this.mNavigationBarView.getRecentsButton().setOnClickListener(this.mRecentsClickListener);
        this.mNavigationBarView.getRecentsButton().setOnTouchListener(this.mRecentsPreloadOnTouchListener);
        this.mNavigationBarView.getRecentsButton().setLongClickable(true);
        this.mNavigationBarView.getRecentsButton().setOnLongClickListener(this.mLongPressBackRecentsListener);
        this.mNavigationBarView.getBackButton().setLongClickable(true);
        this.mNavigationBarView.getBackButton().setOnLongClickListener(this.mLongPressBackRecentsListener);
        this.mNavigationBarView.getHomeButton().setOnTouchListener(this.mHomeActionListener);
        this.mNavigationBarView.getHomeButton().setOnLongClickListener(this.mLongPressHomeListener);
        this.mAssistManager.onConfigurationChanged();
        if (MultiWindowProxy.isSupported()) {
            this.mNavigationBarView.getFloatButton().setOnClickListener(this.mFloatClickListener);
            if (this.mIsSplitModeEnable) {
                this.mNavigationBarView.getFloatModeButton().setOnClickListener(this.mFloatModeClickListener);
                this.mNavigationBarView.getSplitModeButton().setOnClickListener(this.mSplitModeClickListener);
            }
            MultiWindowProxy.getInstance().setSystemUiCallback(new MWSystemUiCallback());
        }
    }

    private void addNavigationBar() {
        Log.v("PhoneStatusBar", "addNavigationBar: about to add " + this.mNavigationBarView);
        if (this.mNavigationBarView != null) {
            prepareNavigationBarView();
            this.mWindowManager.addView(this.mNavigationBarView, getNavigationBarLayoutParams());
        }
    }

    private void repositionNavigationBar() {
        if (this.mNavigationBarView != null && this.mNavigationBarView.isAttachedToWindow()) {
            prepareNavigationBarView();
            this.mWindowManager.updateViewLayout(this.mNavigationBarView, getNavigationBarLayoutParams());
        }
    }

    private void notifyNavigationBarScreenOn(boolean screenOn) {
        if (this.mNavigationBarView != null) {
            this.mNavigationBarView.notifyScreenOn(screenOn);
        }
    }

    private LayoutParams getNavigationBarLayoutParams() {
        LayoutParams lp = new LayoutParams(-1, -1, 2019, 8650856, -3);
        if (ActivityManager.isHighEndGfx()) {
            lp.flags |= 16777216;
        }
        lp.setTitle("NavigationBar");
        lp.windowAnimations = 0;
        return lp;
    }

    public void addIcon(String slot, int index, int viewIndex, StatusBarIcon icon) {
        this.mIconController.addSystemIcon(slot, index, viewIndex, icon);
    }

    public void updateIcon(String slot, int index, int viewIndex, StatusBarIcon old, StatusBarIcon icon) {
        this.mIconController.updateSystemIcon(slot, index, viewIndex, old, icon);
    }

    public void removeIcon(String slot, int index, int viewIndex) {
        this.mIconController.removeSystemIcon(slot, index, viewIndex);
    }

    public void addNotification(StatusBarNotification notification, RankingMap ranking, Entry oldEntry) {
        Log.d("PhoneStatusBar", "addNotification key=" + notification.getKey());
        Entry shadeEntry = createNotificationViews(notification);
        if (shadeEntry != null) {
            boolean shouldInterrupt = this.mUseHeadsUp ? shouldInterrupt(shadeEntry) : false;
            if (shouldInterrupt) {
                this.mHeadsUpManager.showNotification(shadeEntry);
                setNotificationShown(notification);
            }
            if (!(shouldInterrupt || notification.getNotification().fullScreenIntent == null)) {
                awakenDreams();
                Log.d("PhoneStatusBar", "Notification has fullScreenIntent; sending fullScreenIntent");
                try {
                    EventLog.writeEvent(36002, notification.getKey());
                    notification.getNotification().fullScreenIntent.send();
                    shadeEntry.notifyFullScreenIntentLaunched();
                    MetricsLogger.count(this.mContext, "note_fullscreen", 1);
                } catch (CanceledException e) {
                }
            }
            addNotificationViews(shadeEntry, ranking);
            setAreThereNotifications();
        }
    }

    protected void updateNotificationRanking(RankingMap ranking) {
        this.mNotificationData.updateRanking(ranking);
        updateNotifications();
    }

    public void removeNotification(String key, RankingMap ranking) {
        boolean deferRemoval = false;
        if (this.mHeadsUpManager.isHeadsUp(key)) {
            deferRemoval = !this.mHeadsUpManager.removeNotification(key);
        }
        if (key.equals(this.mMediaNotificationKey)) {
            clearCurrentMediaNotification();
            updateMediaMetaData(true);
        }
        if (deferRemoval) {
            this.mLatestRankingMap = ranking;
            this.mHeadsUpEntriesToRemoveOnSwitch.add(this.mHeadsUpManager.getEntry(key));
            return;
        }
        if (!(removeNotificationViews(key, ranking) == null || hasActiveNotifications() || this.mNotificationPanel.isTracking() || this.mNotificationPanel.isQsExpanded())) {
            if (this.mState == 0) {
                animateCollapsePanels();
            } else if (this.mState == 2) {
                goToKeyguard();
            }
        }
        setAreThereNotifications();
    }

    protected void refreshLayout(int layoutDirection) {
        if (this.mNavigationBarView != null) {
            this.mNavigationBarView.setLayoutDirection(layoutDirection);
        }
    }

    private void updateNotificationShade() {
        if (this.mStackScroller != null) {
            if (isCollapsing()) {
                addPostCollapseAction(new Runnable() {
                    public void run() {
                        PhoneStatusBar.this.updateNotificationShade();
                    }
                });
                return;
            }
            int i;
            View child;
            ArrayList<Entry> activeNotifications = this.mNotificationData.getActiveNotifications();
            ArrayList<ExpandableNotificationRow> arrayList = new ArrayList(activeNotifications.size());
            int N = activeNotifications.size();
            for (i = 0; i < N; i++) {
                Entry ent = (Entry) activeNotifications.get(i);
                boolean packageHasVisibilityOverride = ((ent.notification.getNotification().visibility == 0) && (!userAllowsPrivateNotificationsInPublic(ent.notification.getUserId()))) ? true : packageHasVisibilityOverride(ent.notification.getKey());
                boolean isLockscreenPublicMode = packageHasVisibilityOverride ? isLockscreenPublicMode() : false;
                ent.row.setSensitive(packageHasVisibilityOverride);
                if (ent.autoRedacted && ent.legacy) {
                    if (isLockscreenPublicMode) {
                        ent.row.setShowingLegacyBackground(false);
                    } else {
                        ent.row.setShowingLegacyBackground(true);
                    }
                }
                if (this.mGroupManager.isChildInGroupWithSummary(ent.row.getStatusBarNotification())) {
                    ExpandableNotificationRow summary = this.mGroupManager.getGroupSummary(ent.row.getStatusBarNotification());
                    List<ExpandableNotificationRow> orderedChildren = (List) this.mTmpChildOrderMap.get(summary);
                    if (orderedChildren == null) {
                        orderedChildren = new ArrayList();
                        this.mTmpChildOrderMap.put(summary, orderedChildren);
                    }
                    orderedChildren.add(ent.row);
                } else {
                    arrayList.add(ent.row);
                }
            }
            ArrayList<View> toRemove = new ArrayList();
            for (i = 0; i < this.mStackScroller.getChildCount(); i++) {
                child = this.mStackScroller.getChildAt(i);
                if (!arrayList.contains(child) && (child instanceof ExpandableNotificationRow)) {
                    toRemove.add(child);
                }
            }
            for (View remove : toRemove) {
                this.mStackScroller.removeView(remove);
            }
            for (i = 0; i < arrayList.size(); i++) {
                View v = (View) arrayList.get(i);
                if (v.getParent() == null) {
                    this.mStackScroller.addView(v);
                }
            }
            int j = 0;
            for (i = 0; i < this.mStackScroller.getChildCount(); i++) {
                child = this.mStackScroller.getChildAt(i);
                if (child instanceof ExpandableNotificationRow) {
                    View targetChild = (ExpandableNotificationRow) arrayList.get(j);
                    if (child != targetChild) {
                        this.mStackScroller.changeViewPosition(targetChild, i);
                    }
                    j++;
                }
            }
            updateNotificationShadeForChildren();
            this.mTmpChildOrderMap.clear();
            updateRowStates();
            updateSpeedbump();
            updateClearAll();
            updateEmptyShadeView();
            updateQsExpansionEnabled();
            this.mShadeUpdates.check();
        }
    }

    private void updateQsExpansionEnabled() {
        boolean z = false;
        NotificationPanelView notificationPanelView = this.mNotificationPanel;
        if (isDeviceProvisioned()) {
            if (this.mUserSetup || this.mUserSwitcherController == null || !this.mUserSwitcherController.isSimpleUserSwitcher()) {
                if ((this.mDisabled2 & 1) == 0 && !ONLY_CORE_APPS) {
                    z = true;
                }
            }
        }
        notificationPanelView.setQsExpansionEnabled(z);
    }

    private void updateNotificationShadeForChildren() {
        ArrayList<ExpandableNotificationRow> toRemove = new ArrayList();
        boolean orderChanged = false;
        for (int i = 0; i < this.mStackScroller.getChildCount(); i++) {
            View view = this.mStackScroller.getChildAt(i);
            if (view instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow parent = (ExpandableNotificationRow) view;
                List<ExpandableNotificationRow> children = parent.getNotificationChildren();
                List<ExpandableNotificationRow> orderedChildren = (List) this.mTmpChildOrderMap.get(parent);
                if (children != null) {
                    toRemove.clear();
                    for (ExpandableNotificationRow childRow : children) {
                        if (orderedChildren == null || !orderedChildren.contains(childRow)) {
                            toRemove.add(childRow);
                        }
                    }
                    for (ExpandableNotificationRow remove : toRemove) {
                        parent.removeChildNotification(remove);
                        this.mStackScroller.notifyGroupChildRemoved(remove);
                    }
                }
                int childIndex = 0;
                while (orderedChildren != null && childIndex < orderedChildren.size()) {
                    ExpandableNotificationRow childView = (ExpandableNotificationRow) orderedChildren.get(childIndex);
                    if (children == null || !children.contains(childView)) {
                        parent.addChildNotification(childView, childIndex);
                        this.mStackScroller.notifyGroupChildAdded(childView);
                    }
                    childIndex++;
                }
                orderChanged |= parent.applyChildOrder(orderedChildren);
            }
        }
        if (orderChanged) {
            this.mStackScroller.generateChildOrderChangedEvent();
        }
    }

    private boolean packageHasVisibilityOverride(String key) {
        return this.mNotificationData.getVisibilityOverride(key) != -1000;
    }

    private void updateClearAll() {
        boolean hasActiveClearableNotifications;
        if (this.mState != 1) {
            hasActiveClearableNotifications = this.mNotificationData.hasActiveClearableNotifications();
        } else {
            hasActiveClearableNotifications = false;
        }
        this.mStackScroller.updateDismissView(hasActiveClearableNotifications);
    }

    private void updateEmptyShadeView() {
        boolean showEmptyShade = this.mState != 1 ? this.mNotificationData.getActiveNotifications().size() == 0 : false;
        this.mNotificationPanel.setShadeEmpty(showEmptyShade);
    }

    private void updateSpeedbump() {
        int speedbumpIndex = -1;
        int currentIndex = 0;
        ArrayList<Entry> activeNotifications = this.mNotificationData.getActiveNotifications();
        int N = activeNotifications.size();
        for (int i = 0; i < N; i++) {
            Entry entry = (Entry) activeNotifications.get(i);
            if (!(!isTopLevelChild(entry))) {
                if (entry.row.getVisibility() != 8 && this.mNotificationData.isAmbient(entry.key)) {
                    speedbumpIndex = currentIndex;
                    break;
                }
                currentIndex++;
            }
        }
        this.mStackScroller.updateSpeedBumpIndex(speedbumpIndex);
    }

    public static boolean isTopLevelChild(Entry entry) {
        return entry.row.getParent() instanceof NotificationStackScrollLayout;
    }

    protected void updateNotifications() {
        this.mNotificationData.filterAndSort();
        updateNotificationShade();
        this.mIconController.updateNotificationIcons(this.mNotificationData);
    }

    protected void updateRowStates() {
        super.updateRowStates();
        this.mNotificationPanel.notifyVisibleChildrenChanged();
    }

    protected void setAreThereNotifications() {
        boolean z;
        int i = 1;
        final View nlo = this.mStatusBarView.findViewById(R.id.notification_lights_out);
        boolean showDot = hasActiveNotifications() && !areLightsOn();
        if (nlo.getAlpha() == 1.0f) {
            z = true;
        } else {
            z = false;
        }
        if (showDot != z) {
            if (showDot) {
                nlo.setAlpha(0.0f);
                nlo.setVisibility(0);
            }
            ViewPropertyAnimator animate = nlo.animate();
            if (!showDot) {
                i = 0;
            }
            animate.alpha((float) i).setDuration((long) (showDot ? 750 : 250)).setInterpolator(new AccelerateInterpolator(2.0f)).setListener(showDot ? null : new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator _a) {
                    nlo.setVisibility(8);
                }
            }).start();
        }
        findAndUpdateMediaNotifications();
        updateCarrierLabelVisibility(false);
    }

    public void findAndUpdateMediaNotifications() {
        boolean metaDataChanged = false;
        synchronized (this.mNotificationData) {
            int i;
            MediaController aController;
            ArrayList<Entry> activeNotifications = this.mNotificationData.getActiveNotifications();
            int N = activeNotifications.size();
            Entry entry = null;
            MediaController controller = null;
            for (i = 0; i < N; i++) {
                Entry entry2 = (Entry) activeNotifications.get(i);
                if (isMediaNotification(entry2)) {
                    Token token = (Token) entry2.notification.getNotification().extras.getParcelable("android.mediaSession");
                    if (token != null) {
                        aController = new MediaController(this.mContext, token);
                        if (3 == getMediaControllerPlaybackState(aController)) {
                            entry = entry2;
                            controller = aController;
                            break;
                        }
                    } else {
                        continue;
                    }
                }
            }
            if (entry == null && this.mMediaSessionManager != null) {
                for (MediaController aController2 : this.mMediaSessionManager.getActiveSessionsForUser(null, -1)) {
                    if (3 == getMediaControllerPlaybackState(aController2)) {
                        String pkg = aController2.getPackageName();
                        for (i = 0; i < N; i++) {
                            entry2 = (Entry) activeNotifications.get(i);
                            if (entry2.notification.getPackageName().equals(pkg)) {
                                controller = aController2;
                                entry = entry2;
                                break;
                            }
                        }
                    }
                }
            }
            if (controller != null) {
                if (!sameSessions(this.mMediaController, controller)) {
                    clearCurrentMediaNotification();
                    this.mMediaController = controller;
                    this.mMediaController.registerCallback(this.mMediaListener);
                    this.mMediaMetadata = this.mMediaController.getMetadata();
                    if (entry != null) {
                        this.mMediaNotificationKey = entry.notification.getKey();
                    }
                    metaDataChanged = true;
                }
            }
        }
        if (metaDataChanged) {
            updateNotifications();
        }
        updateMediaMetaData(metaDataChanged);
    }

    private int getMediaControllerPlaybackState(MediaController controller) {
        if (controller != null) {
            PlaybackState playbackState = controller.getPlaybackState();
            if (playbackState != null) {
                return playbackState.getState();
            }
        }
        return 0;
    }

    private boolean isPlaybackActive(int state) {
        return (state == 1 || state == 7 || state == 0) ? false : true;
    }

    private void clearCurrentMediaNotification() {
        this.mMediaNotificationKey = null;
        this.mMediaMetadata = null;
        if (this.mMediaController != null) {
            this.mMediaController.unregisterCallback(this.mMediaListener);
        }
        this.mMediaController = null;
    }

    private boolean sameSessions(MediaController a, MediaController b) {
        if (a == b) {
            return true;
        }
        if (a == null) {
            return false;
        }
        return a.controlsSameSession(b);
    }

    public void updateMediaMetaData(boolean metaDataChanged) {
        if (this.mBackdrop != null) {
            if (this.mLaunchTransitionFadingAway) {
                this.mBackdrop.setVisibility(4);
                return;
            }
            Bitmap bitmap = null;
            if (this.mMediaMetadata != null) {
                bitmap = this.mMediaMetadata.getBitmap("android.media.metadata.ART");
                if (bitmap == null) {
                    bitmap = this.mMediaMetadata.getBitmap("android.media.metadata.ALBUM_ART");
                }
            }
            if ((bitmap != null) && (this.mState == 1 || this.mState == 2)) {
                if (this.mBackdrop.getVisibility() != 0) {
                    this.mBackdrop.setVisibility(0);
                    this.mBackdrop.animate().alpha(1.0f);
                    metaDataChanged = true;
                }
                if (metaDataChanged) {
                    if (this.mBackdropBack.getDrawable() != null) {
                        this.mBackdropFront.setImageDrawable(this.mBackdropBack.getDrawable().getConstantState().newDrawable().mutate());
                        if (this.mScrimSrcModeEnabled) {
                            this.mBackdropFront.getDrawable().mutate().setXfermode(this.mSrcOverXferMode);
                        }
                        this.mBackdropFront.setAlpha(1.0f);
                        this.mBackdropFront.setVisibility(0);
                    } else {
                        this.mBackdropFront.setVisibility(4);
                    }
                    this.mBackdropBack.setImageBitmap(bitmap);
                    if (this.mScrimSrcModeEnabled) {
                        this.mBackdropBack.getDrawable().mutate().setXfermode(this.mSrcXferMode);
                    }
                    if (this.mBackdropFront.getVisibility() == 0) {
                        this.mBackdropFront.animate().setDuration(250).alpha(0.0f).withEndAction(this.mHideBackdropFront);
                    }
                }
            } else if (this.mBackdrop.getVisibility() != 8) {
                this.mBackdrop.animate().alpha(0.002f).setInterpolator(this.mBackdropInterpolator).setDuration(300).setStartDelay(0).withEndAction(new Runnable() {
                    public void run() {
                        PhoneStatusBar.this.mBackdrop.setVisibility(8);
                        PhoneStatusBar.this.mBackdropFront.animate().cancel();
                        PhoneStatusBar.this.mBackdropBack.animate().cancel();
                        PhoneStatusBar.this.mHandler.post(PhoneStatusBar.this.mHideBackdropFront);
                    }
                });
                if (this.mKeyguardFadingAway) {
                    this.mBackdrop.animate().setDuration(this.mKeyguardFadingAwayDuration / 2).setStartDelay(this.mKeyguardFadingAwayDelay).setInterpolator(this.mLinearInterpolator).start();
                }
            }
        }
    }

    private int adjustDisableFlags(int state) {
        if (this.mLaunchTransitionFadingAway || this.mKeyguardFadingAway) {
            return state;
        }
        if (this.mExpandedVisible || this.mBouncerShowing || this.mWaitingForKeyguardExit) {
            return (state | 131072) | 1048576;
        }
        return state;
    }

    public void disable(int state1, int state2, boolean animate) {
        String str;
        animate &= this.mStatusBarWindowState != 2 ? 1 : 0;
        this.mDisabledUnmodified1 = state1;
        this.mDisabledUnmodified2 = state2;
        state1 = adjustDisableFlags(state1);
        int diff1 = state1 ^ this.mDisabled1;
        this.mDisabled1 = state1;
        int diff2 = state2 ^ this.mDisabled2;
        this.mDisabled2 = state2;
        Log.d("PhoneStatusBar", String.format("disable1: 0x%08x -> 0x%08x (diff1: 0x%08x)", new Object[]{Integer.valueOf(old1), Integer.valueOf(state1), Integer.valueOf(diff1)}));
        Log.d("PhoneStatusBar", String.format("disable2: 0x%08x -> 0x%08x (diff2: 0x%08x)", new Object[]{Integer.valueOf(old2), Integer.valueOf(state2), Integer.valueOf(diff2)}));
        StringBuilder flagdbg = new StringBuilder();
        flagdbg.append("disable: < ");
        flagdbg.append((65536 & state1) != 0 ? "EXPAND" : "expand");
        flagdbg.append((65536 & diff1) != 0 ? "* " : " ");
        flagdbg.append((131072 & state1) != 0 ? "ICONS" : "icons");
        flagdbg.append((131072 & diff1) != 0 ? "* " : " ");
        flagdbg.append((262144 & state1) != 0 ? "ALERTS" : "alerts");
        flagdbg.append((262144 & diff1) != 0 ? "* " : " ");
        flagdbg.append((1048576 & state1) != 0 ? "SYSTEM_INFO" : "system_info");
        flagdbg.append((1048576 & diff1) != 0 ? "* " : " ");
        flagdbg.append((4194304 & state1) != 0 ? "BACK" : "back");
        flagdbg.append((4194304 & diff1) != 0 ? "* " : " ");
        flagdbg.append((2097152 & state1) != 0 ? "HOME" : "home");
        flagdbg.append((2097152 & diff1) != 0 ? "* " : " ");
        flagdbg.append((16777216 & state1) != 0 ? "RECENT" : "recent");
        flagdbg.append((16777216 & diff1) != 0 ? "* " : " ");
        flagdbg.append((8388608 & state1) != 0 ? "CLOCK" : "clock");
        flagdbg.append((8388608 & diff1) != 0 ? "* " : " ");
        flagdbg.append((33554432 & state1) != 0 ? "SEARCH" : "search");
        flagdbg.append((33554432 & diff1) != 0 ? "* " : " ");
        if ((state2 & 1) != 0) {
            str = "QUICK_SETTINGS";
        } else {
            str = "quick_settings";
        }
        flagdbg.append(str);
        flagdbg.append((diff2 & 1) != 0 ? "* " : " ");
        flagdbg.append(">");
        Log.d("PhoneStatusBar", flagdbg.toString());
        if ((1048576 & diff1) != 0) {
            if ((1048576 & state1) != 0) {
                this.mIconController.hideSystemIconArea(animate);
                this.mStatusBarPlmnPlugin.setPlmnVisibility(8);
            } else {
                this.mIconController.showSystemIconArea(animate);
                this.mStatusBarPlmnPlugin.setPlmnVisibility(0);
            }
        }
        if ((8388608 & diff1) != 0) {
            this.mIconController.setClockVisibility((8388608 & state1) == 0);
        }
        if (!((65536 & diff1) == 0 || (65536 & state1) == 0)) {
            animateCollapsePanels();
        }
        if ((56623104 & diff1) != 0) {
            if (this.mNavigationBarView != null) {
                this.mNavigationBarView.setDisabledFlags(state1);
            }
            if ((16777216 & state1) != 0) {
                this.mHandler.removeMessages(1020);
                this.mHandler.sendEmptyMessage(1020);
            }
        }
        if ((131072 & diff1) != 0) {
            if ((131072 & state1) != 0) {
                this.mIconController.hideNotificationIconArea(animate);
            } else {
                this.mIconController.showNotificationIconArea(animate);
            }
        }
        if ((262144 & diff1) != 0) {
            this.mDisableNotificationAlerts = (262144 & state1) != 0;
            this.mHeadsUpObserver.onChange(true);
        }
        if ((diff2 & 1) != 0) {
            updateQsExpansionEnabled();
        }
    }

    protected H createHandler() {
        return new H();
    }

    public void startActivity(Intent intent, boolean dismissShade) {
        startActivityDismissingKeyguard(intent, false, dismissShade);
    }

    public void startActivity(Intent intent, boolean dismissShade, ActivityStarter.Callback callback) {
        startActivityDismissingKeyguard(intent, false, dismissShade, callback);
    }

    public void preventNextAnimation() {
        overrideActivityPendingAppTransition(true);
    }

    public void setQsExpanded(boolean expanded) {
        int i;
        this.mStatusBarWindowManager.setQsExpanded(expanded);
        View view = this.mKeyguardStatusView;
        if (expanded) {
            i = 4;
        } else {
            i = 0;
        }
        view.setImportantForAccessibility(i);
    }

    public boolean isGoingToNotificationShade() {
        return this.mLeaveOpenOnKeyguardHide;
    }

    public boolean isWakeUpComingFromTouch() {
        return this.mWakeUpComingFromTouch;
    }

    public boolean isFalsingThresholdNeeded() {
        return getBarState() == 1;
    }

    public boolean isDozing() {
        return this.mDozing;
    }

    public String getCurrentMediaNotificationKey() {
        return this.mMediaNotificationKey;
    }

    public boolean isScrimSrcModeEnabled() {
        return this.mScrimSrcModeEnabled;
    }

    public void onKeyguardViewManagerStatesUpdated() {
        logStateToEventlog();
    }

    public void onUnlockMethodStateChanged() {
        logStateToEventlog();
    }

    public void onHeadsUpPinnedModeChanged(boolean inPinnedMode) {
        if (inPinnedMode) {
            this.mStatusBarWindowManager.setHeadsUpShowing(true);
            this.mStatusBarWindowManager.setForceStatusBarVisible(true);
            if (this.mNotificationPanel.isFullyCollapsed()) {
                this.mNotificationPanel.requestLayout();
                this.mStatusBarWindowManager.setForceWindowCollapsed(true);
                this.mNotificationPanel.post(new Runnable() {
                    public void run() {
                        PhoneStatusBar.this.mStatusBarWindowManager.setForceWindowCollapsed(false);
                    }
                });
            }
        } else if (!this.mNotificationPanel.isFullyCollapsed() || this.mNotificationPanel.isTracking()) {
            this.mStatusBarWindowManager.setHeadsUpShowing(false);
        } else {
            this.mHeadsUpManager.setHeadsUpGoingAway(true);
            this.mStackScroller.runAfterAnimationFinished(new Runnable() {
                public void run() {
                    if (!PhoneStatusBar.this.mHeadsUpManager.hasPinnedHeadsUp()) {
                        PhoneStatusBar.this.mStatusBarWindowManager.setHeadsUpShowing(false);
                        PhoneStatusBar.this.mHeadsUpManager.setHeadsUpGoingAway(false);
                    }
                }
            });
        }
    }

    public void onHeadsUpPinned(ExpandableNotificationRow headsUp) {
        dismissVolumeDialog();
    }

    public void onHeadsUpUnPinned(ExpandableNotificationRow headsUp) {
    }

    public void onHeadsUpStateChanged(Entry entry, boolean isHeadsUp) {
        if (isHeadsUp || !this.mHeadsUpEntriesToRemoveOnSwitch.contains(entry)) {
            updateNotificationRanking(null);
            return;
        }
        removeNotification(entry.key, this.mLatestRankingMap);
        this.mHeadsUpEntriesToRemoveOnSwitch.remove(entry);
        if (this.mHeadsUpEntriesToRemoveOnSwitch.isEmpty()) {
            this.mLatestRankingMap = null;
        }
    }

    protected void updateHeadsUp(String key, Entry entry, boolean shouldInterrupt, boolean alertAgain) {
        if (isHeadsUp(key)) {
            if (shouldInterrupt) {
                this.mHeadsUpManager.updateNotification(entry, alertAgain);
            } else {
                this.mHeadsUpManager.removeNotification(key);
            }
        } else if (this.mUseHeadsUp && shouldInterrupt && alertAgain) {
            this.mHeadsUpManager.showNotification(entry);
        }
    }

    protected void setHeadsUpUser(int newUserId) {
        if (this.mHeadsUpManager != null) {
            this.mHeadsUpManager.setUser(newUserId);
        }
    }

    public boolean isHeadsUp(String key) {
        return this.mHeadsUpManager.isHeadsUp(key);
    }

    protected boolean isSnoozedPackage(StatusBarNotification sbn) {
        return this.mHeadsUpManager.isSnoozed(sbn.getPackageName());
    }

    public boolean isKeyguardCurrentlySecure() {
        return !this.mUnlockMethodCache.canSkipBouncer();
    }

    public void setPanelExpanded(boolean isExpanded) {
        this.mStatusBarWindowManager.setPanelExpanded(isExpanded);
    }

    public void maybeEscalateHeadsUp() {
        for (HeadsUpEntry entry : this.mHeadsUpManager.getSortedEntries()) {
            StatusBarNotification sbn = entry.entry.notification;
            Notification notification = sbn.getNotification();
            if (notification.fullScreenIntent != null) {
                Log.d("PhoneStatusBar", "converting a heads up to fullScreen");
                try {
                    EventLog.writeEvent(36003, sbn.getKey());
                    notification.fullScreenIntent.send();
                    entry.entry.notifyFullScreenIntentLaunched();
                } catch (CanceledException e) {
                }
            }
        }
        this.mHeadsUpManager.releaseAllImmediately();
    }

    boolean panelsEnabled() {
        return (this.mDisabled1 & 65536) == 0 && !ONLY_CORE_APPS;
    }

    void makeExpandedVisible(boolean force) {
        boolean z = false;
        if (force || (!this.mExpandedVisible && panelsEnabled())) {
            this.mExpandedVisible = true;
            if (this.mNavigationBarView != null) {
                this.mNavigationBarView.setSlippery(true);
            }
            updateCarrierLabelVisibility(true);
            this.mStatusBarWindowManager.setPanelVisible(true);
            visibilityChanged(true);
            this.mWaitingForKeyguardExit = false;
            int i = this.mDisabledUnmodified1;
            int i2 = this.mDisabledUnmodified2;
            if (!force) {
                z = true;
            }
            disable(i, i2, z);
            setInteracting(1, true);
        }
    }

    public void animateCollapsePanels() {
        animateCollapsePanels(0);
    }

    public void postAnimateCollapsePanels() {
        this.mHandler.post(this.mAnimateCollapsePanels);
    }

    public void animateCollapsePanels(int flags) {
        animateCollapsePanels(flags, false, false, 1.0f);
    }

    public void animateCollapsePanels(int flags, boolean force) {
        animateCollapsePanels(flags, force, false, 1.0f);
    }

    public void animateCollapsePanels(int flags, boolean force, boolean delayed) {
        animateCollapsePanels(flags, force, delayed, 1.0f);
    }

    public void animateCollapsePanels(int flags, boolean force, boolean delayed, float speedUpFactor) {
        if (force || !(this.mState == 1 || this.mState == 2)) {
            if ((flags & 2) == 0 && !this.mHandler.hasMessages(1020)) {
                this.mHandler.removeMessages(1020);
                this.mHandler.sendEmptyMessage(1020);
            }
            if (this.mStatusBarWindow != null) {
                this.mStatusBarWindowManager.setStatusBarFocusable(false);
                this.mStatusBarWindow.cancelExpandHelper();
                this.mStatusBarView.collapseAllPanels(true, delayed, speedUpFactor);
            }
            return;
        }
        runPostCollapseRunnables();
    }

    private void runPostCollapseRunnables() {
        ArrayList<Runnable> clonedList = new ArrayList(this.mPostCollapseRunnables);
        this.mPostCollapseRunnables.clear();
        int size = clonedList.size();
        for (int i = 0; i < size; i++) {
            ((Runnable) clonedList.get(i)).run();
        }
    }

    public void animateExpandNotificationsPanel() {
        if (panelsEnabled()) {
            this.mNotificationPanel.expand();
        }
    }

    public void animateExpandSettingsPanel() {
        if (panelsEnabled() && this.mUserSetup) {
            this.mNotificationPanel.expandWithQs();
        }
    }

    public void animateCollapseQuickSettings() {
        if (this.mState == 0) {
            this.mStatusBarView.collapseAllPanels(true, false, 1.0f);
        }
    }

    void makeExpandedInvisible() {
        if (this.mExpandedVisible && this.mStatusBarWindow != null) {
            this.mStatusBarView.collapseAllPanels(false, false, 1.0f);
            this.mNotificationPanel.closeQs();
            this.mExpandedVisible = false;
            if (this.mNavigationBarView != null) {
                this.mNavigationBarView.setSlippery(false);
            }
            visibilityChanged(false);
            this.mStatusBarWindowManager.setPanelVisible(false);
            this.mStatusBarWindowManager.setForceStatusBarVisible(false);
            dismissPopups();
            runPostCollapseRunnables();
            setInteracting(1, false);
            showBouncer();
            disable(this.mDisabledUnmodified1, this.mDisabledUnmodified2, true);
            if (!this.mStatusBarKeyguardViewManager.isShowing()) {
                WindowManagerGlobal.getInstance().trimMemory(20);
            }
        }
    }

    public boolean interceptTouchEvent(MotionEvent event) {
        if (event.getAction() != 2) {
            Log.d("PhoneStatusBar", String.format("panel: %s at (%f, %f) mDisabled1=0x%08x mDisabled2=0x%08x", new Object[]{MotionEvent.actionToString(event.getAction()), Float.valueOf(event.getRawX()), Float.valueOf(event.getRawY()), Integer.valueOf(this.mDisabled1), Integer.valueOf(this.mDisabled2)}));
        }
        if (this.mStatusBarWindowState == 0) {
            boolean upOrCancel = event.getAction() != 1 ? event.getAction() == 3 : true;
            if (!upOrCancel || this.mExpandedVisible) {
                setInteracting(1, true);
            } else {
                setInteracting(1, false);
            }
        }
        return false;
    }

    public GestureRecorder getGestureRecorder() {
        return this.mGestureRec;
    }

    private void setNavigationIconHints(int hints) {
        if (hints != this.mNavigationIconHints) {
            this.mNavigationIconHints = hints;
            if (this.mNavigationBarView != null) {
                this.mNavigationBarView.setNavigationIconHints(hints);
            }
            checkBarModes();
        }
    }

    public void setWindowState(int window, int state) {
        boolean showing = state == 0;
        if (!(this.mStatusBarWindow == null || window != 1 || this.mStatusBarWindowState == state)) {
            this.mStatusBarWindowState = state;
            if (!showing && this.mState == 0) {
                this.mStatusBarView.collapseAllPanels(false, false, 1.0f);
            }
        }
        if (this.mNavigationBarView != null && window == 2 && this.mNavigationBarWindowState != state) {
            this.mNavigationBarWindowState = state;
        }
    }

    public void buzzBeepBlinked() {
        if (this.mDozeServiceHost != null) {
            this.mDozeServiceHost.fireBuzzBeepBlinked();
        }
    }

    public void notificationLightOff() {
        if (this.mDozeServiceHost != null) {
            this.mDozeServiceHost.fireNotificationLight(false);
        }
    }

    public void notificationLightPulse(int argb, int onMillis, int offMillis) {
        if (this.mDozeServiceHost != null) {
            this.mDozeServiceHost.fireNotificationLight(true);
        }
    }

    private int computeBarMode(int oldVis, int newVis, BarTransitions transitions, int transientFlag, int translucentFlag) {
        int oldMode = barMode(oldVis, transientFlag, translucentFlag);
        int newMode = barMode(newVis, transientFlag, translucentFlag);
        if (oldMode == newMode) {
            return -1;
        }
        return newMode;
    }

    private int barMode(int vis, int transientFlag, int translucentFlag) {
        if ((vis & transientFlag) != 0) {
            return 1;
        }
        if ((vis & translucentFlag) != 0) {
            return 2;
        }
        if ((vis & 32769) == 32769) {
            return 6;
        }
        if ((32768 & vis) != 0) {
            return 4;
        }
        if ((vis & 1) != 0) {
            return 3;
        }
        return 0;
    }

    private void checkBarModes() {
        if (!this.mDemoMode) {
            checkBarMode(this.mStatusBarMode, this.mStatusBarWindowState, this.mStatusBarView.getBarTransitions(), this.mNoAnimationOnNextBarModeChange);
            if (this.mNavigationBarView != null) {
                checkBarMode(this.mNavigationBarMode, this.mNavigationBarWindowState, this.mNavigationBarView.getBarTransitions(), this.mNoAnimationOnNextBarModeChange);
            }
            this.mNoAnimationOnNextBarModeChange = false;
        }
    }

    private void checkBarMode(int mode, int windowState, BarTransitions transitions, boolean noAnimation) {
        boolean powerSave = this.mBatteryController.isPowerSave();
        boolean anim = (noAnimation || !this.mDeviceInteractive || windowState == 2) ? false : !powerSave;
        if (powerSave && getBarState() == 0) {
            mode = 5;
        }
        transitions.transitionTo(mode, anim);
    }

    private void finishBarAnimations() {
        this.mStatusBarView.getBarTransitions().finishAnimations();
        if (this.mNavigationBarView != null) {
            this.mNavigationBarView.getBarTransitions().finishAnimations();
        }
    }

    private void dismissVolumeDialog() {
        if (this.mVolumeComponent != null) {
            this.mVolumeComponent.dismissNow();
        }
    }

    private void resumeSuspendedAutohide() {
        if (this.mAutohideSuspended) {
            scheduleAutohide();
            this.mHandler.postDelayed(this.mCheckBarModes, 500);
        }
    }

    private void suspendAutohide() {
        boolean z = false;
        this.mHandler.removeCallbacks(this.mAutohide);
        this.mHandler.removeCallbacks(this.mCheckBarModes);
        if ((this.mSystemUiVisibility & 201326592) != 0) {
            z = true;
        }
        this.mAutohideSuspended = z;
    }

    private void cancelAutohide() {
        this.mAutohideSuspended = false;
        this.mHandler.removeCallbacks(this.mAutohide);
    }

    private void scheduleAutohide() {
        cancelAutohide();
        this.mHandler.postDelayed(this.mAutohide, 3000);
    }

    private void checkUserAutohide(View v, MotionEvent event) {
        if ((this.mSystemUiVisibility & 201326592) != 0 && event.getAction() == 4 && event.getX() == 0.0f && event.getY() == 0.0f) {
            userAutohide();
        }
    }

    private void userAutohide() {
        cancelAutohide();
        this.mHandler.postDelayed(this.mAutohide, 350);
    }

    private boolean areLightsOn() {
        return (this.mSystemUiVisibility & 1) == 0;
    }

    public void setLightsOn(boolean on) {
        Log.v("PhoneStatusBar", "setLightsOn(" + on + ")");
        if (on) {
            setSystemUiVisibility(0, 1);
        } else {
            setSystemUiVisibility(1, 1);
        }
    }

    private void notifyUiVisibilityChanged(int vis) {
        try {
            if (this.mLastDispatchedSystemUiVisibility != vis) {
                this.mWindowManagerService.statusBarVisibilityChanged(vis);
                this.mLastDispatchedSystemUiVisibility = vis;
            }
        } catch (RemoteException e) {
        }
    }

    public void topAppWindowChanged(boolean showMenu) {
        Log.d("PhoneStatusBar", (showMenu ? "showing" : "hiding") + " the MENU button");
        if (this.mNavigationBarView != null) {
            this.mNavigationBarView.setMenuVisibility(showMenu);
        }
        if (showMenu) {
            setLightsOn(true);
        }
    }

    public void setImeWindowStatus(IBinder token, int vis, int backDisposition, boolean showImeSwitcher) {
        boolean imeShown = (vis & 2) != 0;
        int flags = this.mNavigationIconHints;
        if (backDisposition == 2 || imeShown) {
            flags |= 1;
        } else {
            flags &= -2;
        }
        if (showImeSwitcher) {
            flags |= 2;
        } else {
            flags &= -3;
        }
        setNavigationIconHints(flags);
    }

    public static String viewInfo(View v) {
        return "[(" + v.getLeft() + "," + v.getTop() + ")(" + v.getRight() + "," + v.getBottom() + ") " + v.getWidth() + "x" + v.getHeight() + "]";
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        synchronized (this.mQueueLock) {
            pw.println("Current Status Bar state:");
            pw.println("  mExpandedVisible=" + this.mExpandedVisible + ", mTrackingPosition=" + this.mTrackingPosition);
            pw.println("  mTracking=" + this.mTracking);
            pw.println("  mDisplayMetrics=" + this.mDisplayMetrics);
            pw.println("  mStackScroller: " + viewInfo(this.mStackScroller));
            pw.println("  mStackScroller: " + viewInfo(this.mStackScroller) + " scroll " + this.mStackScroller.getScrollX() + "," + this.mStackScroller.getScrollY());
        }
        pw.print("  mInteractingWindows=");
        pw.println(this.mInteractingWindows);
        pw.print("  mStatusBarWindowState=");
        pw.println(StatusBarManager.windowStateToString(this.mStatusBarWindowState));
        pw.print("  mStatusBarMode=");
        pw.println(BarTransitions.modeToString(this.mStatusBarMode));
        pw.print("  mDozing=");
        pw.println(this.mDozing);
        pw.print("  mZenMode=");
        pw.println(Global.zenModeToString(this.mZenMode));
        pw.print("  mUseHeadsUp=");
        pw.println(this.mUseHeadsUp);
        dumpBarTransitions(pw, "mStatusBarView", this.mStatusBarView.getBarTransitions());
        if (this.mNavigationBarView != null) {
            pw.print("  mNavigationBarWindowState=");
            pw.println(StatusBarManager.windowStateToString(this.mNavigationBarWindowState));
            pw.print("  mNavigationBarMode=");
            pw.println(BarTransitions.modeToString(this.mNavigationBarMode));
            dumpBarTransitions(pw, "mNavigationBarView", this.mNavigationBarView.getBarTransitions());
        }
        pw.print("  mNavigationBarView=");
        if (this.mNavigationBarView == null) {
            pw.println("null");
        } else {
            this.mNavigationBarView.dump(fd, pw, args);
        }
        pw.print("  mMediaSessionManager=");
        pw.println(this.mMediaSessionManager);
        pw.print("  mMediaNotificationKey=");
        pw.println(this.mMediaNotificationKey);
        pw.print("  mMediaController=");
        pw.print(this.mMediaController);
        if (this.mMediaController != null) {
            pw.print(" state=" + this.mMediaController.getPlaybackState());
        }
        pw.println();
        pw.print("  mMediaMetadata=");
        pw.print(this.mMediaMetadata);
        if (this.mMediaMetadata != null) {
            pw.print(" title=" + this.mMediaMetadata.getText("android.media.metadata.TITLE"));
        }
        pw.println();
        pw.println("  Panels: ");
        if (this.mNotificationPanel != null) {
            pw.println("    mNotificationPanel=" + this.mNotificationPanel + " params=" + this.mNotificationPanel.getLayoutParams().debug(""));
            pw.print("      ");
            this.mNotificationPanel.dump(fd, pw, args);
        }
        DozeLog.dump(pw);
        synchronized (this.mNotificationData) {
            this.mNotificationData.dump(pw, "  ");
        }
        this.mIconController.dump(pw);
        if (this.mStatusBarWindowManager != null) {
            this.mStatusBarWindowManager.dump(fd, pw, args);
        }
        if (this.mNetworkController != null) {
            this.mNetworkController.dump(fd, pw, args);
        }
        if (this.mBluetoothController != null) {
            this.mBluetoothController.dump(fd, pw, args);
        }
        if (this.mHotspotController != null) {
            this.mHotspotController.dump(fd, pw, args);
        }
        if (this.mCastController != null) {
            this.mCastController.dump(fd, pw, args);
        }
        if (this.mUserSwitcherController != null) {
            this.mUserSwitcherController.dump(fd, pw, args);
        }
        if (this.mBatteryController != null) {
            this.mBatteryController.dump(fd, pw, args);
        }
        if (this.mNextAlarmController != null) {
            this.mNextAlarmController.dump(fd, pw, args);
        }
        if (this.mAssistManager != null) {
            this.mAssistManager.dump(fd, pw, args);
        }
        if (this.mSecurityController != null) {
            this.mSecurityController.dump(fd, pw, args);
        }
        if (this.mHeadsUpManager != null) {
            this.mHeadsUpManager.dump(fd, pw, args);
        } else {
            pw.println("  mHeadsUpManager: null");
        }
        if (KeyguardUpdateMonitor.getInstance(this.mContext) != null) {
            KeyguardUpdateMonitor.getInstance(this.mContext).dump(fd, pw, args);
        }
        pw.println("SharedPreferences:");
        for (Map.Entry<String, ?> entry : Prefs.getAll(this.mContext).entrySet()) {
            pw.print("  ");
            pw.print((String) entry.getKey());
            pw.print("=");
            pw.println(entry.getValue());
        }
    }

    private static void dumpBarTransitions(PrintWriter pw, String var, BarTransitions transitions) {
        pw.print("  ");
        pw.print(var);
        pw.print(".BarTransitions.mMode=");
        pw.println(BarTransitions.modeToString(transitions.getMode()));
    }

    public void createAndAddWindows() {
        addStatusBarWindow();
    }

    private void addStatusBarWindow() {
        makeStatusBarView();
        this.mStatusBarWindowManager = new StatusBarWindowManager(this.mContext);
        this.mStatusBarWindowManager.add(this.mStatusBarWindow, getStatusBarHeight());
    }

    void updateDisplaySize() {
        this.mDisplay.getMetrics(this.mDisplayMetrics);
        this.mDisplay.getSize(this.mCurrentDisplaySize);
    }

    float getDisplayDensity() {
        return this.mDisplayMetrics.density;
    }

    public void startActivityDismissingKeyguard(Intent intent, boolean onlyProvisioned, boolean dismissShade) {
        startActivityDismissingKeyguard(intent, onlyProvisioned, dismissShade, null);
    }

    public void startActivityDismissingKeyguard(Intent intent, boolean onlyProvisioned, boolean dismissShade, final ActivityStarter.Callback callback) {
        if (!onlyProvisioned || isDeviceProvisioned()) {
            final boolean afterKeyguardGone = PreviewInflater.wouldLaunchResolverActivity(this.mContext, intent, this.mCurrentUserId);
            final boolean keyguardShowing = this.mStatusBarKeyguardViewManager.isShowing();
            final Intent intent2 = intent;
            final ActivityStarter.Callback callback2 = callback;
            executeRunnableDismissingKeyguard(new Runnable() {
                public void run() {
                    PhoneStatusBar.this.mAssistManager.hideAssist();
                    intent2.setFlags(335544320);
                    int result = -6;
                    try {
                        result = ActivityManagerNative.getDefault().startActivityAsUser(null, PhoneStatusBar.this.mContext.getBasePackageName(), intent2, intent2.resolveTypeIfNeeded(PhoneStatusBar.this.mContext.getContentResolver()), null, null, 0, 268435456, null, null, UserHandle.CURRENT.getIdentifier());
                    } catch (RemoteException e) {
                        Log.w("PhoneStatusBar", "Unable to start activity", e);
                    }
                    PhoneStatusBar phoneStatusBar = PhoneStatusBar.this;
                    boolean z = keyguardShowing && !afterKeyguardGone;
                    phoneStatusBar.overrideActivityPendingAppTransition(z);
                    if (callback2 != null) {
                        callback2.onActivityStarted(result);
                    }
                }
            }, new Runnable() {
                public void run() {
                    if (callback != null) {
                        callback.onActivityStarted(-6);
                    }
                }
            }, dismissShade, afterKeyguardGone);
        }
    }

    public void executeRunnableDismissingKeyguard(Runnable runnable, Runnable cancelAction, boolean dismissShade, boolean afterKeyguardGone) {
        final boolean keyguardShowing = this.mStatusBarKeyguardViewManager.isShowing();
        final boolean z = dismissShade;
        final boolean z2 = afterKeyguardGone;
        final Runnable runnable2 = runnable;
        dismissKeyguardThenExecute(new OnDismissAction() {
            public boolean onDismiss() {
                final boolean z = keyguardShowing;
                final boolean z2 = z2;
                final Runnable runnable = runnable2;
                AsyncTask.execute(new Runnable() {
                    public void run() {
                        try {
                            if (z && !z2) {
                                ActivityManagerNative.getDefault().keyguardWaitingForActivityDrawn();
                            }
                            if (runnable != null) {
                                runnable.run();
                            }
                        } catch (RemoteException e) {
                        }
                    }
                });
                if (z) {
                    PhoneStatusBar.this.animateCollapsePanels(2, true, true);
                }
                return true;
            }
        }, cancelAction, afterKeyguardGone);
    }

    private void resetUserExpandedStates() {
        ArrayList<Entry> activeNotifications = this.mNotificationData.getActiveNotifications();
        int notificationCount = activeNotifications.size();
        for (int i = 0; i < notificationCount; i++) {
            Entry entry = (Entry) activeNotifications.get(i);
            if (entry.row != null) {
                entry.row.resetUserExpansion();
            }
        }
    }

    protected void dismissKeyguardThenExecute(OnDismissAction action, boolean afterKeyguardGone) {
        dismissKeyguardThenExecute(action, null, afterKeyguardGone);
    }

    private void dismissKeyguardThenExecute(OnDismissAction action, Runnable cancelAction, boolean afterKeyguardGone) {
        if (this.mStatusBarKeyguardViewManager.isShowing()) {
            this.mStatusBarKeyguardViewManager.dismissWithAction(action, cancelAction, afterKeyguardGone);
        } else {
            action.onDismiss();
        }
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.v("PhoneStatusBar", "configuration changed: " + this.mContext.getResources().getConfiguration());
        updateDisplaySize();
        updateResources();
        repositionNavigationBar();
        updateRowStates();
        this.mIconController.updateResources();
        this.mScreenPinningRequest.onConfigurationChanged();
        this.mNetworkController.onConfigurationChanged();
        if (MultiWindowProxy.isSupported()) {
            updateFloatButtonIconOnly(isFloatPanelOpened());
            updateFloatModeButton(!this.mIsSplitModeOn);
            updateSpilitModeButton(this.mIsSplitModeOn);
        }
    }

    public void userSwitched(int newUserId) {
        super.userSwitched(newUserId);
        animateCollapsePanels();
        updatePublicMode();
        updateNotifications();
        resetUserSetupObserver();
        setControllerUsers();
        this.mAssistManager.onUserSwitched(newUserId);
    }

    private void setControllerUsers() {
        if (this.mZenModeController != null) {
            this.mZenModeController.setUserId(this.mCurrentUserId);
        }
        if (this.mSecurityController != null) {
            this.mSecurityController.onUserSwitched(this.mCurrentUserId);
        }
    }

    private void resetUserSetupObserver() {
        this.mContext.getContentResolver().unregisterContentObserver(this.mUserSetupObserver);
        this.mUserSetupObserver.onChange(false);
        this.mContext.getContentResolver().registerContentObserver(Secure.getUriFor("user_setup_complete"), true, this.mUserSetupObserver, this.mCurrentUserId);
    }

    void updateResources() {
        if (this.mQSPanel != null) {
            this.mQSPanel.updateResources();
        }
        loadDimens();
        if (this.mNotificationPanel != null) {
            this.mNotificationPanel.updateResources();
        }
        if (this.mBrightnessMirrorController != null) {
            this.mBrightnessMirrorController.updateResources();
        }
    }

    protected void loadDimens() {
        Resources res = this.mContext.getResources();
        this.mNaturalBarHeight = res.getDimensionPixelSize(17104919);
        this.mRowMinHeight = res.getDimensionPixelSize(R.dimen.notification_min_height);
        this.mRowMaxHeight = res.getDimensionPixelSize(R.dimen.notification_max_height);
        this.mKeyguardMaxNotificationCount = res.getInteger(R.integer.keyguard_max_notification_count);
        Log.v("PhoneStatusBar", "updateResources");
    }

    protected void handleVisibleToUserChanged(boolean visibleToUser) {
        if (visibleToUser) {
            super.handleVisibleToUserChanged(visibleToUser);
            startNotificationLogging();
            return;
        }
        stopNotificationLogging();
        super.handleVisibleToUserChanged(visibleToUser);
    }

    private void stopNotificationLogging() {
        if (!this.mCurrentlyVisibleNotifications.isEmpty()) {
            logNotificationVisibilityChanges(Collections.emptyList(), this.mCurrentlyVisibleNotifications);
            recycleAllVisibilityObjects(this.mCurrentlyVisibleNotifications);
        }
        this.mHandler.removeCallbacks(this.mVisibilityReporter);
        this.mStackScroller.setChildLocationsChangedListener(null);
    }

    private void startNotificationLogging() {
        this.mStackScroller.setChildLocationsChangedListener(this.mNotificationLocationsChangedListener);
        this.mNotificationLocationsChangedListener.onChildLocationsChanged(this.mStackScroller);
    }

    private void logNotificationVisibilityChanges(Collection<NotificationVisibility> newlyVisible, Collection<NotificationVisibility> noLongerVisible) {
        if (!newlyVisible.isEmpty() || !noLongerVisible.isEmpty()) {
            NotificationVisibility[] newlyVisibleAr = (NotificationVisibility[]) newlyVisible.toArray(new NotificationVisibility[newlyVisible.size()]);
            try {
                this.mBarService.onNotificationVisibilityChanged(newlyVisibleAr, (NotificationVisibility[]) noLongerVisible.toArray(new NotificationVisibility[noLongerVisible.size()]));
            } catch (RemoteException e) {
            }
            int N = newlyVisible.size();
            if (N > 0) {
                String[] newlyVisibleKeyAr = new String[N];
                for (int i = 0; i < N; i++) {
                    newlyVisibleKeyAr[i] = newlyVisibleAr[i].key;
                }
                setNotificationsShown(newlyVisibleKeyAr);
            }
        }
    }

    private void logStateToEventlog() {
        int i = 1;
        boolean isShowing = this.mStatusBarKeyguardViewManager.isShowing();
        boolean isOccluded = this.mStatusBarKeyguardViewManager.isOccluded();
        boolean isBouncerShowing = this.mStatusBarKeyguardViewManager.isBouncerShowing();
        boolean isSecure = this.mUnlockMethodCache.isMethodSecure();
        boolean canSkipBouncer = this.mUnlockMethodCache.canSkipBouncer();
        int stateFingerprint = getLoggingFingerprint(this.mState, isShowing, isOccluded, isBouncerShowing, isSecure, canSkipBouncer);
        if (stateFingerprint != this.mLastLoggedStateFingerprint) {
            int i2;
            int i3;
            int i4;
            int i5 = this.mState;
            int i6 = isShowing ? 1 : 0;
            if (isOccluded) {
                i2 = 1;
            } else {
                i2 = 0;
            }
            if (isBouncerShowing) {
                i3 = 1;
            } else {
                i3 = 0;
            }
            if (isSecure) {
                i4 = 1;
            } else {
                i4 = 0;
            }
            if (!canSkipBouncer) {
                i = 0;
            }
            EventLogTags.writeSysuiStatusBarState(i5, i6, i2, i3, i4, i);
            this.mLastLoggedStateFingerprint = stateFingerprint;
        }
    }

    private static int getLoggingFingerprint(int statusBarState, boolean keyguardShowing, boolean keyguardOccluded, boolean bouncerShowing, boolean secure, boolean currentlyInsecure) {
        int i;
        int i2 = 1;
        int i3 = statusBarState & 255;
        if (keyguardShowing) {
            i = 1;
        } else {
            i = 0;
        }
        i3 |= i << 8;
        if (keyguardOccluded) {
            i = 1;
        } else {
            i = 0;
        }
        i3 |= i << 9;
        if (bouncerShowing) {
            i = 1;
        } else {
            i = 0;
        }
        i3 |= i << 10;
        if (secure) {
            i = 1;
        } else {
            i = 0;
        }
        i = (i << 11) | i3;
        if (!currentlyInsecure) {
            i2 = 0;
        }
        return (i2 << 12) | i;
    }

    void vibrate() {
        ((Vibrator) this.mContext.getSystemService("vibrator")).vibrate(250, VIBRATION_ATTRIBUTES);
    }

    public boolean shouldDisableNavbarGestures() {
        return (isDeviceProvisioned() && (this.mDisabled1 & 33554432) == 0) ? false : true;
    }

    public void postStartActivityDismissingKeyguard(final PendingIntent intent) {
        this.mHandler.post(new Runnable() {
            public void run() {
                PhoneStatusBar.this.startPendingIntentDismissingKeyguard(intent);
            }
        });
    }

    public void postStartActivityDismissingKeyguard(final Intent intent, int delay) {
        this.mHandler.postDelayed(new Runnable() {
            public void run() {
                PhoneStatusBar.this.handleStartActivityDismissingKeyguard(intent, true);
            }
        }, (long) delay);
    }

    private void handleStartActivityDismissingKeyguard(Intent intent, boolean onlyProvisioned) {
        startActivityDismissingKeyguard(intent, onlyProvisioned, true);
    }

    public void destroy() {
        super.destroy();
        if (this.mStatusBarWindow != null) {
            this.mWindowManager.removeViewImmediate(this.mStatusBarWindow);
            this.mStatusBarWindow = null;
        }
        if (this.mNavigationBarView != null) {
            this.mWindowManager.removeViewImmediate(this.mNavigationBarView);
            this.mNavigationBarView = null;
        }
        if (this.mHandlerThread != null) {
            this.mHandlerThread.quitSafely();
            this.mHandlerThread = null;
        }
        this.mContext.unregisterReceiver(this.mBroadcastReceiver);
        this.mContext.unregisterReceiver(this.mDemoReceiver);
        this.mAssistManager.destroy();
        SignalClusterView signalClusterKeyguard = (SignalClusterView) this.mKeyguardStatusBar.findViewById(R.id.signal_cluster);
        SignalClusterView signalClusterQs = (SignalClusterView) this.mHeader.findViewById(R.id.signal_cluster);
        this.mNetworkController.removeSignalCallback((SignalClusterView) this.mStatusBarView.findViewById(R.id.signal_cluster));
        this.mNetworkController.removeSignalCallback(signalClusterKeyguard);
        this.mNetworkController.removeSignalCallback(signalClusterQs);
        if (this.mQSPanel != null && this.mQSPanel.getHost() != null) {
            this.mQSPanel.getHost().destroy();
        }
    }

    public void dispatchDemoCommand(String command, Bundle args) {
        if (!this.mDemoModeAllowed) {
            boolean z;
            if (Global.getInt(this.mContext.getContentResolver(), "sysui_demo_allowed", 0) != 0) {
                z = true;
            } else {
                z = false;
            }
            this.mDemoModeAllowed = z;
        }
        if (this.mDemoModeAllowed) {
            if (command.equals("enter")) {
                this.mDemoMode = true;
            } else if (command.equals("exit")) {
                this.mDemoMode = false;
                checkBarModes();
            } else if (!this.mDemoMode) {
                dispatchDemoCommand("enter", new Bundle());
            }
            boolean equals = !command.equals("enter") ? command.equals("exit") : true;
            if ((equals || command.equals("volume")) && this.mVolumeComponent != null) {
                this.mVolumeComponent.dispatchDemoCommand(command, args);
            }
            if (equals || command.equals("clock")) {
                dispatchDemoCommandToView(command, args, R.id.clock);
            }
            if (equals || command.equals("battery")) {
                dispatchDemoCommandToView(command, args, R.id.battery);
            }
            if (equals || command.equals("status")) {
                this.mIconController.dispatchDemoCommand(command, args);
            }
            if (this.mNetworkController != null && (equals || command.equals("network"))) {
                this.mNetworkController.dispatchDemoCommand(command, args);
            }
            if (equals || command.equals("notifications")) {
                View view;
                if (this.mStatusBarView == null) {
                    view = null;
                } else {
                    view = this.mStatusBarView.findViewById(R.id.notification_icon_area);
                }
                if (view != null) {
                    int vis = (this.mDemoMode && "false".equals(args.getString("visible"))) ? 4 : 0;
                    view.setVisibility(vis);
                }
            }
            if (command.equals("bars")) {
                int barMode;
                String mode = args.getString("mode");
                if ("opaque".equals(mode)) {
                    barMode = 0;
                } else if ("translucent".equals(mode)) {
                    barMode = 2;
                } else if ("semi-transparent".equals(mode)) {
                    barMode = 1;
                } else if ("transparent".equals(mode)) {
                    barMode = 4;
                } else if ("warning".equals(mode)) {
                    barMode = 5;
                } else {
                    barMode = -1;
                }
                if (barMode != -1) {
                    if (this.mStatusBarView != null) {
                        this.mStatusBarView.getBarTransitions().transitionTo(barMode, true);
                    }
                    if (this.mNavigationBarView != null) {
                        this.mNavigationBarView.getBarTransitions().transitionTo(barMode, true);
                    }
                }
            }
        }
    }

    private void dispatchDemoCommandToView(String command, Bundle args, int id) {
        if (this.mStatusBarView != null) {
            View v = this.mStatusBarView.findViewById(id);
            if (v instanceof DemoMode) {
                ((DemoMode) v).dispatchDemoCommand(command, args);
            }
        }
    }

    public int getBarState() {
        return this.mState;
    }

    protected boolean isPanelFullyCollapsed() {
        return this.mNotificationPanel.isFullyCollapsed();
    }

    public void showKeyguard() {
        if (this.mLaunchTransitionFadingAway) {
            this.mNotificationPanel.animate().cancel();
            onLaunchTransitionFadingEnded();
        }
        this.mHandler.removeMessages(1003);
        setBarState(1);
        updateKeyguardState(false, false);
        if (!this.mDeviceInteractive) {
            this.mNotificationPanel.setTouchDisabled(true);
        }
        instantExpandNotificationsPanel();
        this.mLeaveOpenOnKeyguardHide = false;
        if (this.mDraggedDownRow != null) {
            this.mDraggedDownRow.setUserLocked(false);
            this.mDraggedDownRow.notifyHeightChanged(false);
            this.mDraggedDownRow = null;
        }
        this.mAssistManager.onLockscreenShown();
    }

    private void onLaunchTransitionFadingEnded() {
        this.mNotificationPanel.setAlpha(1.0f);
        runLaunchTransitionEndRunnable();
        this.mLaunchTransitionFadingAway = false;
        this.mScrimController.forceHideScrims(false);
        updateMediaMetaData(true);
    }

    public boolean isCollapsing() {
        return this.mNotificationPanel.isCollapsing();
    }

    public void addPostCollapseAction(Runnable r) {
        this.mPostCollapseRunnables.add(r);
    }

    public boolean isInLaunchTransition() {
        if (this.mNotificationPanel.isLaunchTransitionRunning()) {
            return true;
        }
        return this.mNotificationPanel.isLaunchTransitionFinished();
    }

    public void fadeKeyguardAfterLaunchTransition(final Runnable beforeFading, Runnable endRunnable) {
        this.mHandler.removeMessages(1003);
        this.mLaunchTransitionEndRunnable = endRunnable;
        Runnable hideRunnable = new Runnable() {
            public void run() {
                PhoneStatusBar.this.mLaunchTransitionFadingAway = true;
                if (beforeFading != null) {
                    beforeFading.run();
                }
                PhoneStatusBar.this.mScrimController.forceHideScrims(true);
                PhoneStatusBar.this.updateMediaMetaData(false);
                PhoneStatusBar.this.mNotificationPanel.setAlpha(1.0f);
                PhoneStatusBar.this.mNotificationPanel.animate().alpha(0.0f).setStartDelay(100).setDuration(300).withLayer().withEndAction(new Runnable() {
                    public void run() {
                        PhoneStatusBar.this.onLaunchTransitionFadingEnded();
                    }
                });
                PhoneStatusBar.this.mIconController.appTransitionStarting(SystemClock.uptimeMillis(), 120);
            }
        };
        if (this.mNotificationPanel.isLaunchTransitionRunning()) {
            this.mNotificationPanel.setLaunchTransitionEndRunnable(hideRunnable);
        } else {
            hideRunnable.run();
        }
    }

    public void fadeKeyguardWhilePulsing() {
        this.mNotificationPanel.animate().alpha(0.0f).setStartDelay(0).setDuration(120).setInterpolator(ScrimController.KEYGUARD_FADE_OUT_INTERPOLATOR).withLayer().withEndAction(new Runnable() {
            public void run() {
                PhoneStatusBar.this.mNotificationPanel.setAlpha(1.0f);
                PhoneStatusBar.this.hideKeyguard();
            }
        }).start();
    }

    public void startLaunchTransitionTimeout() {
        this.mHandler.sendEmptyMessageDelayed(1003, 5000);
    }

    private void onLaunchTransitionTimeout() {
        Log.w("PhoneStatusBar", "Launch transition: Timeout!");
        this.mNotificationPanel.resetViews();
    }

    private void runLaunchTransitionEndRunnable() {
        if (this.mLaunchTransitionEndRunnable != null) {
            Runnable r = this.mLaunchTransitionEndRunnable;
            this.mLaunchTransitionEndRunnable = null;
            r.run();
        }
    }

    public boolean hideKeyguard() {
        boolean staying = this.mLeaveOpenOnKeyguardHide;
        setBarState(0);
        if (this.mLeaveOpenOnKeyguardHide) {
            this.mLeaveOpenOnKeyguardHide = false;
            this.mNotificationPanel.animateToFullShade(calculateGoingToFullShadeDelay());
            if (this.mDraggedDownRow != null) {
                this.mDraggedDownRow.setUserLocked(false);
                this.mDraggedDownRow = null;
            }
        } else {
            instantCollapseNotificationPanel();
        }
        updateKeyguardState(staying, false);
        if (this.mQSPanel != null) {
            this.mQSPanel.refreshAllTiles();
        }
        this.mHandler.removeMessages(1003);
        return staying;
    }

    public long calculateGoingToFullShadeDelay() {
        return this.mKeyguardFadingAwayDelay + this.mKeyguardFadingAwayDuration;
    }

    public void keyguardGoingAway() {
        this.mIconController.appTransitionPending();
    }

    public void setKeyguardFadingAway(long startTime, long delay, long fadeoutDuration) {
        boolean z = true;
        this.mKeyguardFadingAway = true;
        this.mKeyguardFadingAwayDelay = delay;
        this.mKeyguardFadingAwayDuration = fadeoutDuration;
        this.mWaitingForKeyguardExit = false;
        this.mIconController.appTransitionStarting((startTime + fadeoutDuration) - 120, 120);
        int i = this.mDisabledUnmodified1;
        int i2 = this.mDisabledUnmodified2;
        if (fadeoutDuration <= 0) {
            z = false;
        }
        disable(i, i2, z);
    }

    public boolean isKeyguardFadingAway() {
        return this.mKeyguardFadingAway;
    }

    public void finishKeyguardFadingAway() {
        this.mKeyguardFadingAway = false;
    }

    public void stopWaitingForKeyguardExit() {
        this.mWaitingForKeyguardExit = false;
    }

    private void updatePublicMode() {
        setLockscreenPublicMode(this.mStatusBarKeyguardViewManager.isShowing() ? this.mStatusBarKeyguardViewManager.isSecure(this.mCurrentUserId) : false);
    }

    private void updateKeyguardState(boolean goingToFullShade, boolean fromShadeLocked) {
        if (this.mState == 1) {
            this.mKeyguardIndicationController.setVisible(true);
            this.mNotificationPanel.resetViews();
            this.mKeyguardUserSwitcher.setKeyguard(true, fromShadeLocked);
            this.mStatusBarView.removePendingHideExpandedRunnables();
        } else {
            this.mKeyguardIndicationController.setVisible(false);
            KeyguardUserSwitcher keyguardUserSwitcher = this.mKeyguardUserSwitcher;
            if (goingToFullShade || this.mState == 2) {
                fromShadeLocked = true;
            }
            keyguardUserSwitcher.setKeyguard(false, fromShadeLocked);
        }
        if (this.mState == 1 || this.mState == 2) {
            this.mScrimController.setKeyguardShowing(true);
            this.mIconPolicy.setKeyguardShowing(true);
        } else {
            this.mScrimController.setKeyguardShowing(false);
            this.mIconPolicy.setKeyguardShowing(false);
        }
        this.mNotificationPanel.setBarState(this.mState, this.mKeyguardFadingAway, goingToFullShade);
        updateDozingState();
        updatePublicMode();
        updateStackScrollerState(goingToFullShade);
        updateNotifications();
        checkBarModes();
        updateCarrierLabelVisibility(false);
        updateMediaMetaData(false);
        this.mKeyguardMonitor.notifyKeyguardState(this.mStatusBarKeyguardViewManager.isShowing(), this.mStatusBarKeyguardViewManager.isSecure());
    }

    private void updateDozingState() {
        boolean z = false;
        boolean isPulsing = !this.mDozing ? this.mDozeScrimController.isPulsing() : false;
        this.mNotificationPanel.setDozing(this.mDozing, isPulsing);
        this.mStackScroller.setDark(this.mDozing, isPulsing, this.mWakeUpTouchLocation);
        this.mScrimController.setDozing(this.mDozing);
        DozeScrimController dozeScrimController = this.mDozeScrimController;
        if (this.mDozing && this.mFingerprintUnlockController.getMode() != 2) {
            z = true;
        }
        dozeScrimController.setDozing(z, isPulsing);
    }

    public void updateStackScrollerState(boolean goingToFullShade) {
        boolean z = true;
        if (this.mStackScroller != null) {
            boolean onKeyguard = this.mState == 1;
            this.mStackScroller.setHideSensitive(isLockscreenPublicMode(), goingToFullShade);
            this.mStackScroller.setDimmed(onKeyguard, false);
            NotificationStackScrollLayout notificationStackScrollLayout = this.mStackScroller;
            if (onKeyguard) {
                z = false;
            }
            notificationStackScrollLayout.setExpandingEnabled(z);
            ActivatableNotificationView activatedChild = this.mStackScroller.getActivatedChild();
            this.mStackScroller.setActivatedChild(null);
            if (activatedChild != null) {
                activatedChild.makeInactive(false);
            }
        }
    }

    public void userActivity() {
        if (this.mState == 1) {
            this.mKeyguardViewMediatorCallback.userActivity();
        }
    }

    public boolean interceptMediaKey(KeyEvent event) {
        if (this.mState == 1) {
            return this.mStatusBarKeyguardViewManager.interceptMediaKey(event);
        }
        return false;
    }

    public boolean onMenuPressed() {
        return this.mState == 1 ? this.mStatusBarKeyguardViewManager.onMenuPressed() : false;
    }

    public boolean onBackPressed() {
        if (this.mStatusBarKeyguardViewManager.onBackPressed()) {
            return true;
        }
        if (this.mNotificationPanel.isQsExpanded()) {
            if (this.mNotificationPanel.isQsDetailShowing()) {
                this.mNotificationPanel.closeQsDetail();
            } else {
                this.mNotificationPanel.animateCloseQs();
            }
            return true;
        } else if (this.mState == 1 || this.mState == 2) {
            return false;
        } else {
            animateCollapsePanels();
            return true;
        }
    }

    public boolean onSpacePressed() {
        if (!this.mDeviceInteractive || (this.mState != 1 && this.mState != 2)) {
            return false;
        }
        animateCollapsePanels(2, true);
        return true;
    }

    private void showBouncer() {
        if (this.mState == 1 || this.mState == 2) {
            this.mWaitingForKeyguardExit = this.mStatusBarKeyguardViewManager.isShowing();
            this.mStatusBarKeyguardViewManager.dismiss();
        }
    }

    private void instantExpandNotificationsPanel() {
        makeExpandedVisible(true);
        this.mNotificationPanel.instantExpand();
    }

    private void instantCollapseNotificationPanel() {
        this.mNotificationPanel.instantCollapse();
    }

    public void onActivated(ActivatableNotificationView view) {
        EventLogTags.writeSysuiLockscreenGesture(7, 0, 0);
        this.mKeyguardIndicationController.showTransientIndication((int) R.string.notification_tap_again);
        ActivatableNotificationView previousView = this.mStackScroller.getActivatedChild();
        if (previousView != null) {
            previousView.makeInactive(true);
        }
        this.mStackScroller.setActivatedChild(view);
    }

    public void setBarState(int state) {
        if (state != this.mState && this.mVisible && (state == 2 || (state == 0 && isGoingToNotificationShade()))) {
            clearNotificationEffects();
        }
        this.mState = state;
        this.mGroupManager.setStatusBarState(state);
        this.mStatusBarWindowManager.setStatusBarState(state);
        updateDozing();
    }

    public void onActivationReset(ActivatableNotificationView view) {
        if (view == this.mStackScroller.getActivatedChild()) {
            this.mKeyguardIndicationController.hideTransientIndication();
            this.mStackScroller.setActivatedChild(null);
        }
    }

    public void onTrackingStarted() {
        runPostCollapseRunnables();
    }

    public void onClosingFinished() {
        runPostCollapseRunnables();
    }

    public void onUnlockHintStarted() {
        this.mKeyguardIndicationController.showTransientIndication((int) R.string.keyguard_unlock);
    }

    public void onHintFinished() {
        this.mKeyguardIndicationController.hideTransientIndicationDelayed(1200);
    }

    public void onCameraHintStarted() {
        this.mKeyguardIndicationController.showTransientIndication((int) R.string.camera_hint);
    }

    public void onVoiceAssistHintStarted() {
        this.mKeyguardIndicationController.showTransientIndication((int) R.string.voice_hint);
    }

    public void onPhoneHintStarted() {
        this.mKeyguardIndicationController.showTransientIndication((int) R.string.phone_hint);
    }

    public void onTrackingStopped(boolean expand) {
        if ((this.mState == 1 || this.mState == 2) && !expand && !this.mUnlockMethodCache.canSkipBouncer()) {
            showBouncer();
        }
    }

    protected int getMaxKeyguardNotifications() {
        return this.mKeyguardMaxNotificationCount;
    }

    public NavigationBarView getNavigationBarView() {
        return this.mNavigationBarView;
    }

    public boolean onDraggedDown(View startingChild, int dragLengthY) {
        if (!hasActiveNotifications()) {
            return false;
        }
        EventLogTags.writeSysuiLockscreenGesture(2, (int) (((float) dragLengthY) / this.mDisplayMetrics.density), 0);
        goToLockedShade(startingChild);
        return true;
    }

    public void onDragDownReset() {
        this.mStackScroller.setDimmed(true, true);
    }

    public void onThresholdReached() {
        this.mStackScroller.setDimmed(false, true);
    }

    public void onTouchSlopExceeded() {
        this.mStackScroller.removeLongPressCallback();
    }

    public void setEmptyDragAmount(float amount) {
        this.mNotificationPanel.setEmptyDragAmount(amount);
    }

    public void goToLockedShade(View expandView) {
        ExpandableNotificationRow expandableNotificationRow = null;
        if (expandView instanceof ExpandableNotificationRow) {
            expandableNotificationRow = (ExpandableNotificationRow) expandView;
            expandableNotificationRow.setUserExpanded(true);
        }
        boolean fullShadeNeedsBouncer = userAllowsPrivateNotificationsInPublic(this.mCurrentUserId) ? !this.mShowLockscreenNotifications : true;
        if (isLockscreenPublicMode() && fullShadeNeedsBouncer) {
            this.mLeaveOpenOnKeyguardHide = true;
            showBouncer();
            this.mDraggedDownRow = expandableNotificationRow;
            return;
        }
        this.mNotificationPanel.animateToFullShade(0);
        setBarState(2);
        updateKeyguardState(false, false);
        if (expandableNotificationRow != null) {
            expandableNotificationRow.setUserLocked(false);
        }
    }

    public void goToKeyguard() {
        if (this.mState == 2) {
            this.mStackScroller.onGoToKeyguard();
            setBarState(1);
            updateKeyguardState(false, true);
        }
    }

    public long getKeyguardFadingAwayDelay() {
        return this.mKeyguardFadingAwayDelay;
    }

    public long getKeyguardFadingAwayDuration() {
        return this.mKeyguardFadingAwayDuration;
    }

    public void setBouncerShowing(boolean bouncerShowing) {
        super.setBouncerShowing(bouncerShowing);
        this.mStatusBarView.setBouncerShowing(bouncerShowing);
        disable(this.mDisabledUnmodified1, this.mDisabledUnmodified2, true);
    }

    public void onFinishedGoingToSleep() {
        this.mDeviceInteractive = false;
        this.mWakeUpComingFromTouch = false;
        this.mWakeUpTouchLocation = null;
        this.mStackScroller.setAnimationsEnabled(false);
        updateVisibleToUser();
    }

    public void onStartedWakingUp() {
        this.mDeviceInteractive = true;
        this.mStackScroller.setAnimationsEnabled(true);
        this.mNotificationPanel.setTouchDisabled(false);
        updateVisibleToUser();
    }

    public void onScreenTurningOn() {
        this.mNotificationPanel.onScreenTurningOn();
    }

    public void onScreenTurnedOn() {
        this.mDozeScrimController.onScreenTurnedOn();
    }

    private void handleLongPressBackRecents(View v) {
        boolean sendBackLongPress = false;
        try {
            IActivityManager activityManager = ActivityManagerNative.getDefault();
            boolean isAccessiblityEnabled = this.mAccessibilityManager.isEnabled();
            if (activityManager.isInLockTaskMode() && !isAccessiblityEnabled) {
                long time = System.currentTimeMillis();
                if (time - this.mLastLockToAppLongPress < 200) {
                    activityManager.stopLockTaskModeOnCurrent();
                    this.mNavigationBarView.setDisabledFlags(this.mDisabled1, true);
                } else if (v.getId() == R.id.back && !this.mNavigationBarView.getRecentsButton().isPressed()) {
                    sendBackLongPress = true;
                }
                this.mLastLockToAppLongPress = time;
            } else if (v.getId() == R.id.back) {
                sendBackLongPress = true;
            } else if (isAccessiblityEnabled && activityManager.isInLockTaskMode()) {
                activityManager.stopLockTaskModeOnCurrent();
                this.mNavigationBarView.setDisabledFlags(this.mDisabled1, true);
            }
            if (sendBackLongPress) {
                KeyButtonView keyButtonView = (KeyButtonView) v;
                keyButtonView.sendEvent(0, 128);
                keyButtonView.sendAccessibilityEvent(2);
            }
        } catch (RemoteException e) {
            Log.d("PhoneStatusBar", "Unable to reach activity manager", e);
        }
    }

    protected void showRecents(boolean triggeredFromAltTab) {
        this.mSystemUiVisibility |= 16384;
        notifyUiVisibilityChanged(this.mSystemUiVisibility);
        super.showRecents(triggeredFromAltTab);
    }

    protected void hideRecents(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
        this.mSystemUiVisibility &= -16385;
        notifyUiVisibilityChanged(this.mSystemUiVisibility);
        super.hideRecents(triggeredFromAltTab, triggeredFromHomeKey);
    }

    protected void toggleRecents() {
        this.mSystemUiVisibility ^= 16384;
        notifyUiVisibilityChanged(this.mSystemUiVisibility);
        super.toggleRecents();
    }

    public void onVisibilityChanged(boolean visible) {
        if (visible) {
            this.mSystemUiVisibility |= 16384;
        } else {
            this.mSystemUiVisibility &= -16385;
        }
        notifyUiVisibilityChanged(this.mSystemUiVisibility);
    }

    public void showScreenPinningRequest() {
        if (!this.mKeyguardMonitor.isShowing()) {
            showScreenPinningRequest(true);
        }
    }

    public void showScreenPinningRequest(boolean allowCancel) {
        this.mScreenPinningRequest.showPrompt(allowCancel);
    }

    public boolean hasActiveNotifications() {
        return !this.mNotificationData.getActiveNotifications().isEmpty();
    }

    public void wakeUpIfDozing(long time, MotionEvent event) {
        if (this.mDozing && this.mDozeScrimController.isPulsing()) {
            ((PowerManager) this.mContext.getSystemService("power")).wakeUp(time, "com.android.systemui:NODOZE");
            this.mWakeUpComingFromTouch = true;
            this.mWakeUpTouchLocation = new PointF(event.getX(), event.getY());
            this.mNotificationPanel.setTouchDisabled(false);
            this.mStatusBarKeyguardViewManager.notifyDeviceWakeUpRequested();
        }
    }

    public void appTransitionPending() {
        if (!this.mKeyguardFadingAway) {
            this.mIconController.appTransitionPending();
        }
    }

    public void appTransitionCancelled() {
        this.mIconController.appTransitionCancelled();
    }

    public void appTransitionStarting(long startTime, long duration) {
        if (!this.mKeyguardFadingAway) {
            this.mIconController.appTransitionStarting(startTime, duration);
        }
        if (this.mIconPolicy != null) {
            this.mIconPolicy.appTransitionStarting(startTime, duration);
        }
    }

    public void notifyFpAuthModeChanged() {
        updateDozing();
    }

    private void updateDozing() {
        boolean z = true;
        if (!((this.mDozingRequested && this.mState == 1) || this.mFingerprintUnlockController.getMode() == 2)) {
            z = false;
        }
        this.mDozing = z;
        updateDozingState();
    }

    public void showDefaultAccountStatus(int subId) {
        this.mNetworkController.showDefaultAccountStatus(new DefaultAccountStatus(subId));
    }

    public void hideDefaultAccountStatus() {
        this.mNetworkController.showDefaultAccountStatus(null);
    }

    private final boolean supportCustomizeCarrierLabel() {
        if (this.mStatusBarPlmnPlugin == null || !this.mStatusBarPlmnPlugin.supportCustomizeCarrierLabel() || this.mNetworkController == null) {
            return false;
        }
        return this.mNetworkController.hasMobileDataFeature();
    }

    private final void updateCustomizeCarrierLabelVisibility(boolean force) {
        Log.d("PhoneStatusBar", "updateCustomizeCarrierLabelVisibility(), force = " + force + ", mState = " + this.mState);
        boolean makeVisible = this.mStackScroller.getVisibility() == 0 ? this.mState != 1 : false;
        this.mStatusBarPlmnPlugin.updateCarrierLabelVisibility(force, makeVisible);
    }

    protected void updateCarrierLabelVisibility(boolean force) {
        if (supportCustomizeCarrierLabel()) {
            if (this.mState != 1 && !this.mNotificationPanel.isPanelVisibleBecauseOfHeadsUp()) {
                updateCustomizeCarrierLabelVisibility(force);
            } else if (this.mCustomizeCarrierLabel != null) {
                this.mCustomizeCarrierLabel.setVisibility(8);
            }
        }
    }

    public void registerMWProxyAgain() {
        IntentFilter filter = new IntentFilter("android.intent.action.BOOT_COMPLETED");
        filter.setPriority(EVENT.DYNAMIC_PACK_EVENT_BASE);
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                Log.v("PhoneStatusBar", "Receive ACTION_BOOT_COMPLETED");
                if (MultiWindowProxy.isSupported()) {
                    Log.v("PhoneStatusBar", "When bootCompleted Make Sure Multi-window setSystemUiCallback ");
                    MultiWindowProxy.getInstance().setSystemUiCallback(new MWSystemUiCallback());
                }
            }
        }, filter);
    }

    public void updateNavigationBarIcon(int state) {
        if (this.mNavigationBarView != null) {
            this.mNavigationBarView.setDisabledFlags(state);
        }
    }

    public void updateFloatButtonIcon(boolean flaotPanelOpen) {
        if (this.mNavigationBarView == null) {
            return;
        }
        if (flaotPanelOpen) {
            this.mNavigationBarView.getFloatButton().setImageResource(R.drawable.ic_sysbar_up);
            checkBarModes();
            return;
        }
        this.mNavigationBarView.getFloatButton().setImageResource(R.drawable.ic_sysbar_down);
        this.mNavigationBarView.getBarTransitions().transitionTo(0, true);
    }

    public void updateFloatModeButton(boolean modeclick) {
        if (this.mNavigationBarView == null) {
            return;
        }
        if (modeclick) {
            this.mNavigationBarView.getFloatModeButton().setImageResource(R.drawable.ic_sysbar_float_mode_on);
        } else {
            this.mNavigationBarView.getFloatModeButton().setImageResource(R.drawable.ic_sysbar_float_mode_off);
        }
    }

    public void updateSpilitModeButton(boolean modeclick) {
        if (this.mNavigationBarView == null) {
            return;
        }
        if (modeclick) {
            this.mNavigationBarView.getSplitModeButton().setImageResource(R.drawable.ic_sysbar_split_mode_on);
        } else {
            this.mNavigationBarView.getSplitModeButton().setImageResource(R.drawable.ic_sysbar_split_mode_off);
        }
    }

    public void updateFloatButtonIconOnly(boolean floatPanelOpen) {
        Log.v("PhoneStatusBar", "updateFloatButtonIconOnly, floatPanelOpen=" + floatPanelOpen);
        if (this.mNavigationBarView == null) {
            return;
        }
        if (floatPanelOpen) {
            this.mNavigationBarView.getFloatButton().setImageResource(R.drawable.ic_sysbar_down);
        } else {
            this.mNavigationBarView.getFloatButton().setImageResource(R.drawable.ic_sysbar_up);
        }
    }

    public void setNavigationBarEditFloatListener(OnClickListener listener) {
        if (this.mNavigationBarView != null) {
            this.mNavigationBarView.getExtensionButton().setOnClickListener(listener);
        }
    }

    public void setFloatModeButtonVisibility(int visibility) {
        if (this.mNavigationBarView != null) {
            this.mNavigationBarView.getFloatModeButton().setVisibility(visibility);
        }
    }

    public void setSplitModeButtonVisibility(int visibility) {
        if (this.mNavigationBarView != null) {
            this.mNavigationBarView.getSplitModeButton().setVisibility(visibility);
        }
    }

    public void setLineVisibility(int visibility) {
        if (this.mNavigationBarView != null) {
            this.mNavigationBarView.getLineView().setVisibility(visibility);
        }
    }

    public void setExtensionButtonVisibility(int visibility) {
        if (this.mNavigationBarView != null) {
            this.mNavigationBarView.getExtensionButton().setText(17040146);
            this.mNavigationBarView.getExtensionButton().setVisibility(visibility);
        }
    }

    public void setFloatButtonVisibility(int visibility) {
        if (this.mNavigationBarView != null && this.mNavigationBarView.getFloatButton() != null) {
            this.mNavigationBarView.getFloatButton().setVisibility(visibility);
        }
    }

    protected LayoutParams getFloatLayoutParams(ViewGroup.LayoutParams layoutParams, boolean focus) {
        int i;
        if (false) {
            i = -1;
        } else {
            i = -3;
        }
        LayoutParams lp = new LayoutParams(-1, -2, 2003, 8815392, i);
        lp.privateFlags |= 16;
        if (!focus) {
            lp.flags |= 8;
        }
        if (ActivityManager.isHighEndGfx()) {
            lp.flags |= 16777216;
        } else {
            lp.flags |= 2;
            lp.dimAmount = 0.75f;
        }
        lp.gravity = 83;
        lp.setTitle("FloatPanel");
        lp.windowAnimations = 16974571;
        lp.softInputMode = 49;
        return lp;
    }

    public void showRestoreButtonInner(boolean flag) {
        this.mNavigationBarView.showRestoreButton(flag);
    }
}
