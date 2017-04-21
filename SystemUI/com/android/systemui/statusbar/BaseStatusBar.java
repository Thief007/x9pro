package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManagerNative;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.TaskStackBuilder;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.service.dreams.IDreamManager;
import android.service.dreams.IDreamManager.Stub;
import android.service.notification.NotificationListenerService;
import android.service.notification.NotificationListenerService.RankingMap;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.IWindowManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerGlobal;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AnimationUtils;
import android.widget.DateTimeView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RemoteViews;
import android.widget.RemoteViews.OnClickHandler;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.statusbar.StatusBarIconList;
import com.android.internal.util.NotificationColorUtil;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardHostView.OnDismissAction;
import com.android.keyguard.KeyguardSecurityModel;
import com.android.keyguard.KeyguardSecurityModel.SecurityMode;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.R;
import com.android.systemui.RecentsComponent;
import com.android.systemui.SwipeHelper.LongPressListener;
import com.android.systemui.SystemUI;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.recents.Recents;
import com.android.systemui.statusbar.ActivatableNotificationView.OnActivatedListener;
import com.android.systemui.statusbar.CommandQueue.Callbacks;
import com.android.systemui.statusbar.ExpandableNotificationRow.ExpansionLogger;
import com.android.systemui.statusbar.NotificationData.Entry;
import com.android.systemui.statusbar.NotificationData.Environment;
import com.android.systemui.statusbar.phone.NavigationBarView;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.PreviewInflater;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import com.mediatek.multiwindow.MultiWindowProxy;
import com.mediatek.systemui.floatpanel.FloatPanelView;
import com.mediatek.systemui.statusbar.extcb.FeatureOptionUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class BaseStatusBar extends SystemUI implements Callbacks, OnActivatedListener, RecentsComponent.Callbacks, ExpansionLogger, Environment {
    public static final boolean DEBUG = Log.isLoggable("StatusBar", 3);
    public static final boolean ENABLE_CHILD_NOTIFICATIONS;
    protected AccessibilityManager mAccessibilityManager;
    private final BroadcastReceiver mAllUsersReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED".equals(intent.getAction()) && BaseStatusBar.this.isCurrentProfile(getSendingUserId())) {
                BaseStatusBar.this.mUsersAllowingPrivateNotifications.clear();
                BaseStatusBar.this.updateLockscreenNotificationSetting();
                BaseStatusBar.this.updateNotifications();
            }
        }
    };
    protected AssistManager mAssistManager;
    protected IStatusBarService mBarService;
    protected boolean mBouncerShowing;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.USER_SWITCHED".equals(action)) {
                BaseStatusBar.this.mCurrentUserId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                BaseStatusBar.this.updateCurrentProfilesCache();
                Log.v("StatusBar", "userId " + BaseStatusBar.this.mCurrentUserId + " is in the house");
                BaseStatusBar.this.updateLockscreenNotificationSetting();
                BaseStatusBar.this.userSwitched(BaseStatusBar.this.mCurrentUserId);
            } else if ("android.intent.action.USER_ADDED".equals(action)) {
                BaseStatusBar.this.updateCurrentProfilesCache();
            } else if ("android.intent.action.USER_PRESENT".equals(action)) {
                List recentTask = null;
                try {
                    recentTask = ActivityManagerNative.getDefault().getRecentTasks(1, 5, BaseStatusBar.this.mCurrentUserId);
                } catch (RemoteException e) {
                }
                if (recentTask != null && recentTask.size() > 0) {
                    UserInfo user = BaseStatusBar.this.mUserManager.getUserInfo(((RecentTaskInfo) recentTask.get(0)).userId);
                    if (user != null && user.isManagedProfile()) {
                        Toast toast = Toast.makeText(BaseStatusBar.this.mContext, R.string.managed_profile_foreground_toast, 0);
                        TextView text = (TextView) toast.getView().findViewById(16908299);
                        text.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.stat_sys_managed_profile_status, 0, 0, 0);
                        text.setCompoundDrawablePadding(BaseStatusBar.this.mContext.getResources().getDimensionPixelSize(R.dimen.managed_profile_toast_padding));
                        toast.show();
                    }
                }
            } else if ("com.android.systemui.statusbar.banner_action_cancel".equals(action) || "com.android.systemui.statusbar.banner_action_setup".equals(action)) {
                ((NotificationManager) BaseStatusBar.this.mContext.getSystemService("notification")).cancel(R.id.notification_hidden);
                Secure.putInt(BaseStatusBar.this.mContext.getContentResolver(), "show_note_about_notification_hiding", 0);
                if ("com.android.systemui.statusbar.banner_action_setup".equals(action)) {
                    BaseStatusBar.this.animateCollapsePanels(2, true);
                    BaseStatusBar.this.mContext.startActivity(new Intent("android.settings.ACTION_APP_NOTIFICATION_REDACTION").addFlags(268435456));
                }
            }
        }
    };
    protected CommandQueue mCommandQueue;
    protected final SparseArray<UserInfo> mCurrentProfiles = new SparseArray();
    protected int mCurrentUserId = 0;
    protected boolean mDeviceInteractive;
    protected DevicePolicyManager mDevicePolicyManager;
    private boolean mDeviceProvisioned = false;
    protected boolean mDisableNotificationAlerts = false;
    protected DismissView mDismissView;
    protected Display mDisplay;
    protected IDreamManager mDreamManager;
    private OnClickListener mEditButtonListner;
    protected EmptyShadeView mEmptyShadeView;
    private TimeInterpolator mFastOutLinearIn;
    private FloatPanelView mFloatPanelView;
    private float mFontScale;
    protected NotificationGroupManager mGroupManager = new NotificationGroupManager();
    protected H mHandler = createHandler();
    protected HeadsUpManager mHeadsUpManager;
    protected boolean mHeadsUpTicker = false;
    private boolean mIsSplitModeEnable = MultiWindowProxy.isSplitModeEnabled();
    protected NotificationOverflowContainer mKeyguardIconOverflowContainer;
    protected int mLayoutDirection = -1;
    private TimeInterpolator mLinearOutSlowIn;
    private Locale mLocale;
    private boolean mLockscreenPublicMode = false;
    private final ContentObserver mLockscreenSettingsObserver = new ContentObserver(this.mHandler) {
        public void onChange(boolean selfChange) {
            BaseStatusBar.this.mUsersAllowingPrivateNotifications.clear();
            BaseStatusBar.this.updateNotifications();
        }
    };
    protected NavigationBarView mNavigationBarView = null;
    private NotificationClicker mNotificationClicker = new NotificationClicker();
    private NotificationColorUtil mNotificationColorUtil;
    protected NotificationData mNotificationData;
    private NotificationGuts mNotificationGutsExposed;
    private final NotificationListenerService mNotificationListener = new NotificationListenerService() {
        public void onListenerConnected() {
            if (BaseStatusBar.DEBUG) {
                Log.d("StatusBar", "onListenerConnected");
            }
            final StatusBarNotification[] notifications = getActiveNotifications();
            final RankingMap currentRanking = getCurrentRanking();
            BaseStatusBar.this.mHandler.post(new Runnable() {
                public void run() {
                    for (StatusBarNotification sbn : notifications) {
                        BaseStatusBar.this.addNotification(sbn, currentRanking, null);
                    }
                }
            });
        }

        public void onNotificationPosted(final StatusBarNotification sbn, final RankingMap rankingMap) {
            if (BaseStatusBar.DEBUG) {
                Log.d("StatusBar", "onNotificationPosted: " + sbn);
            }
            if (sbn != null) {
                BaseStatusBar.this.mHandler.post(new Runnable() {
                    public void run() {
                        String key = sbn.getKey();
                        boolean isUpdate = BaseStatusBar.this.mNotificationData.get(key) != null;
                        if (BaseStatusBar.ENABLE_CHILD_NOTIFICATIONS || !BaseStatusBar.this.mGroupManager.isChildInGroupWithSummary(sbn)) {
                            if (isUpdate) {
                                BaseStatusBar.this.updateNotification(sbn, rankingMap);
                            } else {
                                BaseStatusBar.this.addNotification(sbn, rankingMap, null);
                            }
                            return;
                        }
                        if (BaseStatusBar.DEBUG) {
                            Log.d("StatusBar", "Ignoring group child due to existing summary: " + sbn);
                        }
                        if (isUpdate) {
                            BaseStatusBar.this.removeNotification(key, rankingMap);
                        } else {
                            BaseStatusBar.this.mNotificationData.updateRanking(rankingMap);
                        }
                    }
                });
            }
        }

        public void onNotificationRemoved(StatusBarNotification sbn, final RankingMap rankingMap) {
            if (BaseStatusBar.DEBUG) {
                Log.d("StatusBar", "onNotificationRemoved: " + sbn);
            }
            if (sbn != null) {
                final String key = sbn.getKey();
                BaseStatusBar.this.mHandler.post(new Runnable() {
                    public void run() {
                        BaseStatusBar.this.removeNotification(key, rankingMap);
                    }
                });
            }
        }

        public void onNotificationRankingUpdate(final RankingMap rankingMap) {
            if (BaseStatusBar.DEBUG) {
                Log.d("StatusBar", "onRankingUpdate");
            }
            if (rankingMap != null) {
                BaseStatusBar.this.mHandler.post(new Runnable() {
                    public void run() {
                        BaseStatusBar.this.updateNotificationRanking(rankingMap);
                    }
                });
            }
        }
    };
    private OnClickHandler mOnClickHandler = new OnClickHandler() {
        public boolean onClickHandler(View view, PendingIntent pendingIntent, Intent fillInIntent) {
            if (BaseStatusBar.DEBUG) {
                Log.v("StatusBar", "Notification click handler invoked for intent: " + pendingIntent);
            }
            logActionClick(view);
            try {
                ActivityManagerNative.getDefault().resumeAppSwitches();
            } catch (RemoteException e) {
            }
            if (!pendingIntent.isActivity()) {
                return super.onClickHandler(view, pendingIntent, fillInIntent);
            }
            final boolean keyguardShowing = BaseStatusBar.this.mStatusBarKeyguardViewManager.isShowing();
            final boolean afterKeyguardGone = PreviewInflater.wouldLaunchResolverActivity(BaseStatusBar.this.mContext, pendingIntent.getIntent(), BaseStatusBar.this.mCurrentUserId);
            final View view2 = view;
            final PendingIntent pendingIntent2 = pendingIntent;
            final Intent intent = fillInIntent;
            BaseStatusBar.this.dismissKeyguardThenExecute(new OnDismissAction() {
                public boolean onDismiss() {
                    boolean z;
                    if (keyguardShowing && !afterKeyguardGone) {
                        try {
                            ActivityManagerNative.getDefault().keyguardWaitingForActivityDrawn();
                            ActivityManagerNative.getDefault().resumeAppSwitches();
                        } catch (RemoteException e) {
                        }
                    }
                    boolean handled = AnonymousClass3.this.superOnClickHandler(view2, pendingIntent2, intent);
                    BaseStatusBar baseStatusBar = BaseStatusBar.this;
                    if (!keyguardShowing || afterKeyguardGone) {
                        z = false;
                    } else {
                        z = true;
                    }
                    baseStatusBar.overrideActivityPendingAppTransition(z);
                    if (handled) {
                        BaseStatusBar.this.animateCollapsePanels(2, true);
                        BaseStatusBar.this.visibilityChanged(false);
                        BaseStatusBar.this.mAssistManager.hideAssist();
                    }
                    return handled;
                }
            }, afterKeyguardGone);
            return true;
        }

        private void logActionClick(View view) {
            ViewParent parent = view.getParent();
            String key = getNotificationKeyForParent(parent);
            if (key == null) {
                Log.w("StatusBar", "Couldn't determine notification for click.");
                return;
            }
            int index = -1;
            if (view.getId() == 16909158 && parent != null && (parent instanceof ViewGroup)) {
                index = ((ViewGroup) parent).indexOfChild(view);
            }
            Log.d("StatusBar", "Clicked on button " + index + " for " + key);
            try {
                BaseStatusBar.this.mBarService.onNotificationActionClick(key, index);
            } catch (RemoteException e) {
            }
        }

        private String getNotificationKeyForParent(ViewParent parent) {
            while (parent != null) {
                if (parent instanceof ExpandableNotificationRow) {
                    return ((ExpandableNotificationRow) parent).getStatusBarNotification().getKey();
                }
                parent = parent.getParent();
            }
            return null;
        }

        private boolean superOnClickHandler(View view, PendingIntent pendingIntent, Intent fillInIntent) {
            return super.onClickHandler(view, pendingIntent, fillInIntent);
        }
    };
    PowerManager mPowerManager;
    private RecentsComponent mRecents;
    protected OnTouchListener mRecentsPreloadOnTouchListener = new OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction() & 255;
            if (action == 0) {
                BaseStatusBar.this.preloadRecents();
            } else if (action == 3) {
                BaseStatusBar.this.cancelPreloadingRecents();
            } else if (action == 1 && !v.isPressed()) {
                BaseStatusBar.this.cancelPreloadingRecents();
            }
            return false;
        }
    };
    protected int mRowMaxHeight;
    protected int mRowMinHeight;
    protected KeyguardSecurityModel mSecurityModel;
    protected final ContentObserver mSettingsObserver = new ContentObserver(this.mHandler) {
        public void onChange(boolean selfChange) {
            boolean provisioned = Global.getInt(BaseStatusBar.this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
            if (provisioned != BaseStatusBar.this.mDeviceProvisioned) {
                BaseStatusBar.this.mDeviceProvisioned = provisioned;
                BaseStatusBar.this.updateNotifications();
            }
            BaseStatusBar.this.setZenMode(Global.getInt(BaseStatusBar.this.mContext.getContentResolver(), "zen_mode", 0));
            BaseStatusBar.this.updateLockscreenNotificationSetting();
        }
    };
    protected boolean mShowLockscreenNotifications;
    protected NotificationStackScrollLayout mStackScroller;
    protected int mState;
    protected StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    protected boolean mUseHeadsUp = false;
    private UserManager mUserManager;
    private final SparseBooleanArray mUsersAllowingPrivateNotifications = new SparseBooleanArray();
    protected boolean mVisible;
    private boolean mVisibleToUser;
    protected WindowManager mWindowManager;
    protected IWindowManager mWindowManagerService;
    protected int mZenMode;

    protected class H extends Handler {
        protected H() {
        }

        public void handleMessage(Message m) {
            boolean z = true;
            switch (m.what) {
                case 1019:
                    BaseStatusBar baseStatusBar = BaseStatusBar.this;
                    if (m.arg1 <= 0) {
                        z = false;
                    }
                    baseStatusBar.showRecents(z);
                    return;
                case 1020:
                    BaseStatusBar baseStatusBar2 = BaseStatusBar.this;
                    boolean z2 = m.arg1 > 0;
                    if (m.arg2 <= 0) {
                        z = false;
                    }
                    baseStatusBar2.hideRecents(z2, z);
                    return;
                case 1021:
                    BaseStatusBar.this.toggleRecents();
                    return;
                case 1022:
                    BaseStatusBar.this.preloadRecents();
                    return;
                case 1023:
                    BaseStatusBar.this.cancelPreloadingRecents();
                    return;
                case 1024:
                    BaseStatusBar.this.showRecentsNextAffiliatedTask();
                    return;
                case 1025:
                    BaseStatusBar.this.showRecentsPreviousAffiliatedTask();
                    return;
                case 2029:
                    Log.d("StatusBar", "Toggle float panel.");
                    BaseStatusBar.this.toggleFloatPanel();
                    return;
                case 2030:
                    Log.d("StatusBar", "Close float panel.");
                    BaseStatusBar.this.closeFloatPanel();
                    return;
                default:
                    return;
            }
        }
    }

    private final class NotificationClicker implements OnClickListener {
        private NotificationClicker() {
        }

        public void onClick(View v) {
            if (v instanceof ExpandableNotificationRow) {
                final ExpandableNotificationRow row = (ExpandableNotificationRow) v;
                StatusBarNotification sbn = row.getStatusBarNotification();
                if (sbn == null) {
                    Log.e("StatusBar", "NotificationClicker called on an unclickable notification,");
                    return;
                }
                boolean wouldLaunchResolverActivity;
                final PendingIntent intent = sbn.getNotification().contentIntent;
                final String notificationKey = sbn.getKey();
                Log.d("StatusBar", "Clicked on content of " + notificationKey);
                final boolean keyguardShowing = BaseStatusBar.this.mStatusBarKeyguardViewManager.isShowing();
                if (intent.isActivity()) {
                    wouldLaunchResolverActivity = PreviewInflater.wouldLaunchResolverActivity(BaseStatusBar.this.mContext, intent.getIntent(), BaseStatusBar.this.mCurrentUserId);
                } else {
                    wouldLaunchResolverActivity = false;
                }
                BaseStatusBar.this.dismissKeyguardThenExecute(new OnDismissAction() {
                    public boolean onDismiss() {
                        if (BaseStatusBar.this.mHeadsUpManager != null && BaseStatusBar.this.mHeadsUpManager.isHeadsUp(notificationKey)) {
                            HeadsUpManager.setIsClickedNotification(row, true);
                            BaseStatusBar.this.mHeadsUpManager.releaseImmediately(notificationKey);
                        }
                        final boolean z = keyguardShowing;
                        final boolean z2 = wouldLaunchResolverActivity;
                        final PendingIntent pendingIntent = intent;
                        final String str = notificationKey;
                        new Thread() {
                            public void run() {
                                boolean z = false;
                                try {
                                    if (z && !z2) {
                                        ActivityManagerNative.getDefault().keyguardWaitingForActivityDrawn();
                                    }
                                    ActivityManagerNative.getDefault().resumeAppSwitches();
                                } catch (RemoteException e) {
                                }
                                if (pendingIntent != null) {
                                    try {
                                        pendingIntent.send();
                                    } catch (CanceledException e2) {
                                        Log.w("StatusBar", "Sending contentIntent failed: " + e2);
                                    }
                                    if (pendingIntent.isActivity()) {
                                        BaseStatusBar.this.mAssistManager.hideAssist();
                                        BaseStatusBar baseStatusBar = BaseStatusBar.this;
                                        if (z && !z2) {
                                            z = true;
                                        }
                                        baseStatusBar.overrideActivityPendingAppTransition(z);
                                    }
                                }
                                try {
                                    BaseStatusBar.this.mBarService.onNotificationClick(str);
                                } catch (RemoteException e3) {
                                }
                            }
                        }.start();
                        BaseStatusBar.this.animateCollapsePanels(2, true, true);
                        BaseStatusBar.this.visibilityChanged(false);
                        return true;
                    }
                }, wouldLaunchResolverActivity);
                return;
            }
            Log.e("StatusBar", "NotificationClicker called on a view that is not a notification row.");
        }

        public void register(ExpandableNotificationRow row, StatusBarNotification sbn) {
            if (sbn.getNotification().contentIntent != null) {
                row.setOnClickListener(this);
            } else {
                row.setOnClickListener(null);
            }
        }
    }

    public abstract void addNotification(StatusBarNotification statusBarNotification, RankingMap rankingMap, Entry entry);

    protected abstract void createAndAddWindows();

    protected abstract int getMaxKeyguardNotifications();

    protected abstract View getStatusBarView();

    protected abstract boolean isPanelFullyCollapsed();

    protected abstract boolean isSnoozedPackage(StatusBarNotification statusBarNotification);

    public abstract void maybeEscalateHeadsUp();

    protected abstract void refreshLayout(int i);

    public abstract void removeNotification(String str, RankingMap rankingMap);

    protected abstract void setAreThereNotifications();

    protected abstract void setHeadsUpUser(int i);

    protected abstract void updateHeadsUp(String str, Entry entry, boolean z, boolean z2);

    protected abstract void updateNotificationRanking(RankingMap rankingMap);

    protected abstract void updateNotifications();

    static {
        boolean z = false;
        if (Build.IS_DEBUGGABLE) {
            z = SystemProperties.getBoolean("debug.child_notifs", false);
        }
        ENABLE_CHILD_NOTIFICATIONS = z;
    }

    public boolean isDeviceProvisioned() {
        if (this.mDeviceProvisioned) {
            Log.d("StatusBar", "mDeviceProvisioned is true");
            return this.mDeviceProvisioned;
        }
        Log.d("StatusBar", "mDeviceProvisioned is false, so get DEVICE_PROVISIONED from db again !!");
        boolean provisioned = Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
        if (provisioned != this.mDeviceProvisioned) {
            Log.d("StatusBar", "mDeviceProvisioned is changed, re-call onchange!");
            this.mSettingsObserver.onChange(false);
        }
        return provisioned;
    }

    private void updateCurrentProfilesCache() {
        synchronized (this.mCurrentProfiles) {
            this.mCurrentProfiles.clear();
            if (this.mUserManager != null) {
                for (UserInfo user : this.mUserManager.getProfiles(this.mCurrentUserId)) {
                    this.mCurrentProfiles.put(user.id, user);
                }
            }
        }
    }

    public void start() {
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mWindowManagerService = WindowManagerGlobal.getWindowManagerService();
        this.mDisplay = this.mWindowManager.getDefaultDisplay();
        this.mDevicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        this.mNotificationColorUtil = NotificationColorUtil.getInstance(this.mContext);
        this.mNotificationData = new NotificationData(this);
        this.mAccessibilityManager = (AccessibilityManager) this.mContext.getSystemService("accessibility");
        this.mDreamManager = Stub.asInterface(ServiceManager.checkService("dreams"));
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("device_provisioned"), true, this.mSettingsObserver);
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("zen_mode"), false, this.mSettingsObserver);
        this.mContext.getContentResolver().registerContentObserver(Secure.getUriFor("lock_screen_show_notifications"), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Secure.getUriFor("lock_screen_allow_private_notifications"), true, this.mLockscreenSettingsObserver, -1);
        this.mBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
        this.mRecents = (RecentsComponent) getComponent(Recents.class);
        this.mRecents.setCallback(this);
        Configuration currentConfig = this.mContext.getResources().getConfiguration();
        this.mLocale = currentConfig.locale;
        this.mLayoutDirection = TextUtils.getLayoutDirectionFromLocale(this.mLocale);
        this.mFontScale = currentConfig.fontScale;
        this.mUserManager = (UserManager) this.mContext.getSystemService(FeatureOptionUtils.BUILD_TYPE_USER);
        this.mLinearOutSlowIn = AnimationUtils.loadInterpolator(this.mContext, 17563662);
        this.mFastOutLinearIn = AnimationUtils.loadInterpolator(this.mContext, 17563663);
        StatusBarIconList iconList = new StatusBarIconList();
        this.mCommandQueue = new CommandQueue(this, iconList);
        int[] switches = new int[8];
        ArrayList<IBinder> binders = new ArrayList();
        try {
            this.mBarService.registerStatusBar(this.mCommandQueue, iconList, switches, binders);
        } catch (RemoteException e) {
        }
        createAndAddWindows();
        this.mSettingsObserver.onChange(false);
        disable(switches[0], switches[6], false);
        setSystemUiVisibility(switches[1], -1);
        topAppWindowChanged(switches[2] != 0);
        setImeWindowStatus((IBinder) binders.get(0), switches[3], switches[4], switches[5] != 0);
        int N = iconList.size();
        int viewIndex = 0;
        for (int i = 0; i < N; i++) {
            StatusBarIcon icon = iconList.getIcon(i);
            if (icon != null) {
                addIcon(iconList.getSlot(i), i, viewIndex, icon);
                viewIndex++;
            }
        }
        try {
            this.mNotificationListener.registerAsSystemService(this.mContext, new ComponentName(this.mContext.getPackageName(), getClass().getCanonicalName()), -1);
        } catch (RemoteException e2) {
            Log.e("StatusBar", "Unable to register notification listener", e2);
        }
        if (DEBUG) {
            Log.d("StatusBar", String.format("init: icons=%d disabled=0x%08x lights=0x%08x menu=0x%08x imeButton=0x%08x", new Object[]{Integer.valueOf(iconList.size()), Integer.valueOf(switches[0]), Integer.valueOf(switches[1]), Integer.valueOf(switches[2]), Integer.valueOf(switches[3])}));
        }
        this.mCurrentUserId = ActivityManager.getCurrentUser();
        setHeadsUpUser(this.mCurrentUserId);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_SWITCHED");
        filter.addAction("android.intent.action.USER_ADDED");
        filter.addAction("android.intent.action.USER_PRESENT");
        filter.addAction("com.android.systemui.statusbar.banner_action_cancel");
        filter.addAction("com.android.systemui.statusbar.banner_action_setup");
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
        IntentFilter allUsersFilter = new IntentFilter();
        allUsersFilter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        this.mContext.registerReceiverAsUser(this.mAllUsersReceiver, UserHandle.ALL, allUsersFilter, null, null);
        updateCurrentProfilesCache();
    }

    protected void notifyUserAboutHiddenNotifications() {
        if (Secure.getInt(this.mContext.getContentResolver(), "show_note_about_notification_hiding", 1) != 0) {
            Log.d("StatusBar", "user hasn't seen notification about hidden notifications");
            if (new LockPatternUtils(this.mContext).isSecure(KeyguardUpdateMonitor.getCurrentUser())) {
                Log.d("StatusBar", "disabling lockecreen notifications and alerting the user");
                Secure.putInt(this.mContext.getContentResolver(), "lock_screen_show_notifications", 0);
                Secure.putInt(this.mContext.getContentResolver(), "lock_screen_allow_private_notifications", 0);
                String packageName = this.mContext.getPackageName();
                PendingIntent cancelIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent("com.android.systemui.statusbar.banner_action_cancel").setPackage(packageName), 268435456);
                PendingIntent setupIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent("com.android.systemui.statusbar.banner_action_setup").setPackage(packageName), 268435456);
                Resources res = this.mContext.getResources();
                ((NotificationManager) this.mContext.getSystemService("notification")).notify(R.id.notification_hidden, new Builder(this.mContext).setSmallIcon(R.drawable.ic_android).setContentTitle(this.mContext.getString(R.string.hidden_notifications_title)).setContentText(this.mContext.getString(R.string.hidden_notifications_text)).setPriority(1).setOngoing(true).setColor(this.mContext.getColor(17170521)).setContentIntent(setupIntent).addAction(R.drawable.ic_close, this.mContext.getString(R.string.hidden_notifications_cancel), cancelIntent).addAction(R.drawable.ic_settings, this.mContext.getString(R.string.hidden_notifications_setup), setupIntent).build());
            } else {
                Log.d("StatusBar", "insecure lockscreen, skipping notification");
                Secure.putInt(this.mContext.getContentResolver(), "show_note_about_notification_hiding", 0);
            }
        }
    }

    public void userSwitched(int newUserId) {
        setHeadsUpUser(newUserId);
    }

    public boolean isNotificationForCurrentProfiles(StatusBarNotification n) {
        int thisUserId = this.mCurrentUserId;
        int notificationUserId = n.getUserId();
        if (DEBUG) {
        }
        return isCurrentProfile(notificationUserId);
    }

    protected void setNotificationShown(StatusBarNotification n) {
        setNotificationsShown(new String[]{n.getKey()});
    }

    protected void setNotificationsShown(String[] keys) {
        try {
            this.mNotificationListener.setNotificationsShown(keys);
        } catch (RuntimeException e) {
            Log.d("StatusBar", "failed setNotificationsShown: ", e);
        }
    }

    protected boolean isCurrentProfile(int userId) {
        boolean z = true;
        synchronized (this.mCurrentProfiles) {
            if (userId != -1) {
                if (this.mCurrentProfiles.get(userId) == null) {
                    z = false;
                }
            }
        }
        return z;
    }

    public String getCurrentMediaNotificationKey() {
        return null;
    }

    public NotificationGroupManager getGroupManager() {
        return this.mGroupManager;
    }

    protected void dismissKeyguardThenExecute(OnDismissAction action, boolean afterKeyguardGone) {
        action.onDismiss();
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        Locale locale = this.mContext.getResources().getConfiguration().locale;
        int ld = TextUtils.getLayoutDirectionFromLocale(locale);
        float fontScale = newConfig.fontScale;
        if (locale.equals(this.mLocale) && ld == this.mLayoutDirection) {
            if (fontScale != this.mFontScale) {
            }
            cancelCloseFloatPanel();
            postCloseFloatPanel();
        }
        if (DEBUG) {
            Log.v("StatusBar", String.format("config changed locale/LD: %s (%d) -> %s (%d)", new Object[]{this.mLocale, Integer.valueOf(this.mLayoutDirection), locale, Integer.valueOf(ld)}));
        }
        this.mLocale = locale;
        this.mLayoutDirection = ld;
        refreshLayout(ld);
        cancelCloseFloatPanel();
        postCloseFloatPanel();
    }

    protected View updateNotificationVetoButton(View row, StatusBarNotification n) {
        View vetoButton = row.findViewById(R.id.veto);
        if (n.isClearable()) {
            final String _pkg = n.getPackageName();
            final String _tag = n.getTag();
            final int _id = n.getId();
            final int _userId = n.getUserId();
            vetoButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    v.announceForAccessibility(BaseStatusBar.this.mContext.getString(R.string.accessibility_notification_dismissed));
                    try {
                        BaseStatusBar.this.mBarService.onNotificationClear(_pkg, _tag, _id, _userId);
                    } catch (RemoteException e) {
                    }
                }
            });
            vetoButton.setVisibility(0);
        } else {
            vetoButton.setVisibility(8);
        }
        vetoButton.setImportantForAccessibility(2);
        return vetoButton;
    }

    protected void applyColorsAndBackgrounds(StatusBarNotification sbn, Entry entry) {
        boolean z = true;
        if (entry.getContentView().getId() == 16909162) {
            int color = sbn.getNotification().color;
            if (isMediaNotification(entry)) {
                ExpandableNotificationRow expandableNotificationRow = entry.row;
                if (color == 0) {
                    color = this.mContext.getColor(R.color.notification_material_background_media_default_color);
                }
                expandableNotificationRow.setTintColor(color);
            }
        } else if (entry.targetSdk >= 9 && entry.targetSdk < 21) {
            entry.row.setShowingLegacyBackground(true);
            entry.legacy = true;
        }
        if (entry.icon != null) {
            StatusBarIconView statusBarIconView = entry.icon;
            if (entry.targetSdk >= 21) {
                z = false;
            }
            statusBarIconView.setTag(R.id.icon_is_pre_L, Boolean.valueOf(z));
        }
    }

    public boolean isMediaNotification(Entry entry) {
        if (entry.getExpandedContentView() == null || entry.getExpandedContentView().findViewById(16909167) == null) {
            return false;
        }
        return true;
    }

    private void startAppOwnNotificationSettingsActivity(Intent intent, int notificationId, String notificationTag, int appUid) {
        intent.putExtra("notification_id", notificationId);
        intent.putExtra("notification_tag", notificationTag);
        startNotificationGutsIntent(intent, appUid);
    }

    private void startAppNotificationSettingsActivity(String packageName, int appUid) {
        Intent intent = new Intent("android.settings.APP_NOTIFICATION_SETTINGS");
        intent.putExtra("app_package", packageName);
        intent.putExtra("app_uid", appUid);
        startNotificationGutsIntent(intent, appUid);
    }

    private void startNotificationGutsIntent(final Intent intent, final int appUid) {
        final boolean keyguardShowing = this.mStatusBarKeyguardViewManager.isShowing();
        dismissKeyguardThenExecute(new OnDismissAction() {
            public boolean onDismiss() {
                final boolean z = keyguardShowing;
                final Intent intent = intent;
                final int i = appUid;
                AsyncTask.execute(new Runnable() {
                    public void run() {
                        try {
                            if (z) {
                                ActivityManagerNative.getDefault().keyguardWaitingForActivityDrawn();
                            }
                            TaskStackBuilder.create(BaseStatusBar.this.mContext).addNextIntentWithParentStack(intent).startActivities(null, new UserHandle(UserHandle.getUserId(i)));
                            BaseStatusBar.this.overrideActivityPendingAppTransition(z);
                        } catch (RemoteException e) {
                        }
                    }
                });
                BaseStatusBar.this.animateCollapsePanels(2, true);
                return true;
            }
        }, false);
    }

    private void bindGuts(ExpandableNotificationRow row) {
        row.inflateGuts();
        StatusBarNotification sbn = row.getStatusBarNotification();
        PackageManager pmUser = getPackageManagerForUser(sbn.getUser().getIdentifier());
        row.setTag(sbn.getPackageName());
        View guts = row.getGuts();
        final String pkg = sbn.getPackageName();
        String appname = pkg;
        Drawable drawable = null;
        int appUid = -1;
        try {
            ApplicationInfo info = pmUser.getApplicationInfo(pkg, 8704);
            if (info != null) {
                appname = String.valueOf(pmUser.getApplicationLabel(info));
                drawable = pmUser.getApplicationIcon(info);
                appUid = info.uid;
            }
        } catch (NameNotFoundException e) {
            drawable = pmUser.getDefaultActivityIcon();
        }
        ((ImageView) row.findViewById(16908294)).setImageDrawable(drawable);
        ((DateTimeView) row.findViewById(R.id.timestamp)).setTime(sbn.getPostTime());
        ((TextView) row.findViewById(R.id.pkgname)).setText(appname);
        View settingsButton = guts.findViewById(R.id.notification_inspect_item);
        View appSettingsButton = guts.findViewById(R.id.notification_inspect_app_provided_settings);
        if (appUid >= 0) {
            final int appUidF = appUid;
            settingsButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    MetricsLogger.action(BaseStatusBar.this.mContext, 205);
                    BaseStatusBar.this.startAppNotificationSettingsActivity(pkg, appUidF);
                }
            });
            Intent appSettingsQueryIntent = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.NOTIFICATION_PREFERENCES").setPackage(pkg);
            List<ResolveInfo> infos = pmUser.queryIntentActivities(appSettingsQueryIntent, 0);
            if (infos.size() > 0) {
                appSettingsButton.setVisibility(0);
                appSettingsButton.setContentDescription(this.mContext.getResources().getString(R.string.status_bar_notification_app_settings_title, new Object[]{appname}));
                final Intent appSettingsLaunchIntent = new Intent(appSettingsQueryIntent).setClassName(pkg, ((ResolveInfo) infos.get(0)).activityInfo.name);
                final StatusBarNotification statusBarNotification = sbn;
                appSettingsButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        MetricsLogger.action(BaseStatusBar.this.mContext, 206);
                        BaseStatusBar.this.startAppOwnNotificationSettingsActivity(appSettingsLaunchIntent, statusBarNotification.getId(), statusBarNotification.getTag(), appUidF);
                    }
                });
                return;
            }
            appSettingsButton.setVisibility(8);
            return;
        }
        settingsButton.setVisibility(8);
        appSettingsButton.setVisibility(8);
    }

    protected LongPressListener getNotificationLongClicker() {
        return new LongPressListener() {
            public boolean onLongPress(View v, int x, int y) {
                BaseStatusBar.this.dismissPopups();
                if (!(v instanceof ExpandableNotificationRow)) {
                    return false;
                }
                if (v.getWindowToken() == null) {
                    Log.e("StatusBar", "Trying to show notification guts, but not attached to window");
                    return false;
                }
                ExpandableNotificationRow row = (ExpandableNotificationRow) v;
                BaseStatusBar.this.bindGuts(row);
                NotificationGuts guts = row.getGuts();
                if (guts == null) {
                    return false;
                }
                if (guts.getVisibility() == 0) {
                    Log.e("StatusBar", "Trying to show notification guts, but already visible");
                    return false;
                }
                MetricsLogger.action(BaseStatusBar.this.mContext, 204);
                guts.setVisibility(0);
                Animator a = ViewAnimationUtils.createCircularReveal(guts, x, y, 0.0f, (float) Math.hypot((double) Math.max(guts.getWidth() - x, x), (double) Math.max(guts.getActualHeight() - y, y)));
                a.setDuration(400);
                a.setInterpolator(BaseStatusBar.this.mLinearOutSlowIn);
                a.start();
                BaseStatusBar.this.mNotificationGutsExposed = guts;
                return true;
            }
        };
    }

    public void dismissPopups() {
        if (this.mNotificationGutsExposed != null) {
            final NotificationGuts v = this.mNotificationGutsExposed;
            this.mNotificationGutsExposed = null;
            if (v.getWindowToken() != null) {
                int x = (v.getLeft() + v.getRight()) / 2;
                Animator a = ViewAnimationUtils.createCircularReveal(v, x, v.getTop() + (v.getActualHeight() / 2), (float) x, 0.0f);
                a.setDuration(200);
                a.setInterpolator(this.mFastOutLinearIn);
                a.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        v.setVisibility(8);
                    }
                });
                a.start();
            }
        }
    }

    public void showRecentApps(boolean triggeredFromAltTab) {
        int i;
        this.mHandler.removeMessages(1019);
        H h = this.mHandler;
        if (triggeredFromAltTab) {
            i = 1;
        } else {
            i = 0;
        }
        h.obtainMessage(1019, i, 0).sendToTarget();
    }

    public void hideRecentApps(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
        int i;
        int i2 = 1;
        this.mHandler.removeMessages(1020);
        H h = this.mHandler;
        if (triggeredFromAltTab) {
            i = 1;
        } else {
            i = 0;
        }
        if (!triggeredFromHomeKey) {
            i2 = 0;
        }
        h.obtainMessage(1020, i, i2).sendToTarget();
    }

    public void toggleRecentApps() {
        toggleRecents();
    }

    public void preloadRecentApps() {
        this.mHandler.removeMessages(1022);
        this.mHandler.sendEmptyMessage(1022);
    }

    public void cancelPreloadRecentApps() {
        this.mHandler.removeMessages(1023);
        this.mHandler.sendEmptyMessage(1023);
    }

    public void showNextAffiliatedTask() {
        this.mHandler.removeMessages(1024);
        this.mHandler.sendEmptyMessage(1024);
    }

    public void showPreviousAffiliatedTask() {
        this.mHandler.removeMessages(1025);
        this.mHandler.sendEmptyMessage(1025);
    }

    protected H createHandler() {
        return new H();
    }

    static void sendCloseSystemWindows(Context context, String reason) {
        if (ActivityManagerNative.isSystemReady()) {
            try {
                ActivityManagerNative.getDefault().closeSystemDialogs(reason);
            } catch (RemoteException e) {
            }
        }
    }

    protected void showRecents(boolean triggeredFromAltTab) {
        if (this.mRecents != null) {
            sendCloseSystemWindows(this.mContext, "recentapps");
            this.mRecents.showRecents(triggeredFromAltTab, getStatusBarView());
        }
    }

    protected void hideRecents(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
        if (this.mRecents != null) {
            this.mRecents.hideRecents(triggeredFromAltTab, triggeredFromHomeKey);
        }
    }

    protected void toggleRecents() {
        if (this.mRecents != null) {
            sendCloseSystemWindows(this.mContext, "recentapps");
            this.mRecents.toggleRecents(this.mDisplay, this.mLayoutDirection, getStatusBarView());
        }
    }

    protected void preloadRecents() {
        if (this.mRecents != null) {
            this.mRecents.preloadRecents();
        }
    }

    protected void cancelPreloadingRecents() {
        if (this.mRecents != null) {
            this.mRecents.cancelPreloadingRecents();
        }
    }

    protected void showRecentsNextAffiliatedTask() {
        if (this.mRecents != null) {
            this.mRecents.showNextAffiliatedTask();
        }
    }

    protected void showRecentsPreviousAffiliatedTask() {
        if (this.mRecents != null) {
            this.mRecents.showPrevAffiliatedTask();
        }
    }

    public void onVisibilityChanged(boolean visible) {
    }

    public void setLockscreenPublicMode(boolean publicMode) {
        this.mLockscreenPublicMode = publicMode;
    }

    public boolean isLockscreenPublicMode() {
        return this.mLockscreenPublicMode;
    }

    public boolean userAllowsPrivateNotificationsInPublic(int userHandle) {
        if (userHandle == -1) {
            return true;
        }
        if (this.mUsersAllowingPrivateNotifications.indexOfKey(userHandle) >= 0) {
            return this.mUsersAllowingPrivateNotifications.get(userHandle);
        }
        boolean z = Secure.getIntForUser(this.mContext.getContentResolver(), "lock_screen_allow_private_notifications", 0, userHandle) != 0 ? (this.mDevicePolicyManager.getKeyguardDisabledFeatures(null, userHandle) & 8) == 0 : false;
        this.mUsersAllowingPrivateNotifications.append(userHandle, z);
        return z;
    }

    public boolean shouldHideSensitiveContents(int userid) {
        return isLockscreenPublicMode() && !userAllowsPrivateNotificationsInPublic(userid);
    }

    protected void workAroundBadLayerDrawableOpacity(View v) {
    }

    protected boolean inflateViews(Entry entry, ViewGroup parent) {
        PackageManager pmUser = getPackageManagerForUser(entry.notification.getUser().getIdentifier());
        int maxHeight = this.mRowMaxHeight;
        StatusBarNotification sbn = entry.notification;
        RemoteViews contentView = sbn.getNotification().contentView;
        RemoteViews bigContentView = sbn.getNotification().bigContentView;
        RemoteViews headsUpContentView = sbn.getNotification().headsUpContentView;
        if (contentView == null) {
            return false;
        }
        View row;
        if (DEBUG) {
            Log.v("StatusBar", "publicNotification: " + sbn.getNotification().publicVersion);
        }
        Notification publicNotification = sbn.getNotification().publicVersion;
        boolean hasUserChangedExpansion = false;
        boolean userExpanded = false;
        boolean userLocked = false;
        if (entry.row != null) {
            row = entry.row;
            hasUserChangedExpansion = row.hasUserChangedExpansion();
            userExpanded = row.isUserExpanded();
            userLocked = row.isUserLocked();
            entry.reset();
            if (hasUserChangedExpansion) {
                row.setUserExpanded(userExpanded);
            }
        } else {
            ExpandableNotificationRow row2 = (ExpandableNotificationRow) ((LayoutInflater) this.mContext.getSystemService("layout_inflater")).inflate(R.layout.status_bar_notification_row, parent, false);
            row2.setExpansionLogger(this, entry.notification.getKey());
            row2.setGroupManager(this.mGroupManager);
        }
        workAroundBadLayerDrawableOpacity(row);
        updateNotificationVetoButton(row, sbn).setContentDescription(this.mContext.getString(R.string.accessibility_remove_notification));
        NotificationContentView contentContainer = row.getPrivateLayout();
        ViewGroup contentContainerPublic = row.getPublicLayout();
        row.setLayoutDirection(3);
        row.setDescendantFocusability(393216);
        this.mNotificationClicker.register(row, sbn);
        View bigContentViewLocal = null;
        View headsUpContentViewLocal = null;
        try {
            View contentViewLocal = contentView.apply(sbn.getPackageContext(this.mContext), contentContainer, this.mOnClickHandler);
            if (bigContentView != null) {
                bigContentViewLocal = bigContentView.apply(sbn.getPackageContext(this.mContext), contentContainer, this.mOnClickHandler);
            }
            if (headsUpContentView != null) {
                headsUpContentViewLocal = headsUpContentView.apply(sbn.getPackageContext(this.mContext), contentContainer, this.mOnClickHandler);
            }
            if (contentViewLocal != null) {
                contentViewLocal.setIsRootNamespace(true);
                contentContainer.setContractedChild(contentViewLocal);
            }
            if (bigContentViewLocal != null) {
                bigContentViewLocal.setIsRootNamespace(true);
                contentContainer.setExpandedChild(bigContentViewLocal);
            }
            if (headsUpContentViewLocal != null) {
                headsUpContentViewLocal.setIsRootNamespace(true);
                contentContainer.setHeadsUpChild(headsUpContentViewLocal);
            }
            View view = null;
            if (publicNotification != null) {
                try {
                    view = publicNotification.contentView.apply(sbn.getPackageContext(this.mContext), contentContainerPublic, this.mOnClickHandler);
                    if (view != null) {
                        view.setIsRootNamespace(true);
                        contentContainerPublic.setContractedChild(view);
                    }
                } catch (Throwable e) {
                    Log.e("StatusBar", "couldn't inflate public view for notification " + (sbn.getPackageName() + "/0x" + Integer.toHexString(sbn.getId())), e);
                    view = null;
                }
            }
            try {
                entry.targetSdk = pmUser.getApplicationInfo(sbn.getPackageName(), 0).targetSdkVersion;
            } catch (Throwable ex) {
                Log.e("StatusBar", "Failed looking up ApplicationInfo for " + sbn.getPackageName(), ex);
            }
            if (view == null) {
                view = LayoutInflater.from(this.mContext).inflate(R.layout.notification_public_default, contentContainerPublic, false);
                view.setIsRootNamespace(true);
                TextView title = (TextView) view.findViewById(R.id.title);
                try {
                    title.setText(pmUser.getApplicationLabel(pmUser.getApplicationInfo(entry.notification.getPackageName(), 0)));
                } catch (NameNotFoundException e2) {
                    title.setText(entry.notification.getPackageName());
                }
                ImageView icon = (ImageView) view.findViewById(R.id.icon);
                ImageView profileBadge = (ImageView) view.findViewById(R.id.profile_badge_line3);
                Drawable iconDrawable = StatusBarIconView.getIcon(this.mContext, new StatusBarIcon(entry.notification.getUser(), entry.notification.getPackageName(), entry.notification.getNotification().getSmallIcon(), entry.notification.getNotification().iconLevel, entry.notification.getNotification().number, entry.notification.getNotification().tickerText));
                icon.setImageDrawable(iconDrawable);
                if (entry.targetSdk >= 21 || this.mNotificationColorUtil.isGrayscaleIcon(iconDrawable)) {
                    icon.setBackgroundResource(17302761);
                    int padding = this.mContext.getResources().getDimensionPixelSize(17104993);
                    icon.setPadding(padding, padding, padding, padding);
                    if (sbn.getNotification().color != 0) {
                        icon.getBackground().setColorFilter(sbn.getNotification().color, Mode.SRC_ATOP);
                    }
                }
                if (profileBadge != null) {
                    Drawable profileDrawable = this.mContext.getPackageManager().getUserBadgeForDensity(entry.notification.getUser(), 0);
                    if (profileDrawable != null) {
                        profileBadge.setImageDrawable(profileDrawable);
                        profileBadge.setVisibility(0);
                    } else {
                        profileBadge.setVisibility(8);
                    }
                }
                View privateTime = contentViewLocal.findViewById(16908428);
                DateTimeView time = (DateTimeView) view.findViewById(R.id.time);
                if (privateTime != null && privateTime.getVisibility() == 0) {
                    time.setVisibility(0);
                    time.setTime(entry.notification.getNotification().when);
                }
                TextView text = (TextView) view.findViewById(R.id.text);
                if (text != null) {
                    text.setText(R.string.notification_hidden_text);
                    text.setTextAppearance(this.mContext, R.style.TextAppearance.Material.Notification.Parenthetical);
                }
                title.setPadding(0, Builder.calculateTopPadding(this.mContext, false, this.mContext.getResources().getConfiguration().fontScale), 0, 0);
                contentContainerPublic.setContractedChild(view);
                entry.autoRedacted = true;
            }
            entry.row = row;
            entry.row.setHeightRange(this.mRowMinHeight, maxHeight);
            entry.row.setOnActivatedListener(this);
            entry.row.setExpandable(bigContentViewLocal != null);
            applyColorsAndBackgrounds(sbn, entry);
            if (hasUserChangedExpansion) {
                row.setUserExpanded(userExpanded);
            }
            row.setUserLocked(userLocked);
            row.setStatusBarNotification(entry.notification);
            return true;
        } catch (Throwable e3) {
            Log.e("StatusBar", "couldn't inflate view for notification " + (sbn.getPackageName() + "/0x" + Integer.toHexString(sbn.getId())), e3);
            return false;
        }
    }

    public void startPendingIntentDismissingKeyguard(final PendingIntent intent) {
        if (isDeviceProvisioned()) {
            boolean wouldLaunchResolverActivity;
            final boolean keyguardShowing = this.mStatusBarKeyguardViewManager.isShowing();
            if (intent.isActivity()) {
                wouldLaunchResolverActivity = PreviewInflater.wouldLaunchResolverActivity(this.mContext, intent.getIntent(), this.mCurrentUserId);
            } else {
                wouldLaunchResolverActivity = false;
            }
            dismissKeyguardThenExecute(new OnDismissAction() {
                public boolean onDismiss() {
                    final boolean z = keyguardShowing;
                    final boolean z2 = wouldLaunchResolverActivity;
                    final PendingIntent pendingIntent = intent;
                    new Thread() {
                        public void run() {
                            boolean z = false;
                            try {
                                if (z && !z2) {
                                    ActivityManagerNative.getDefault().keyguardWaitingForActivityDrawn();
                                }
                                ActivityManagerNative.getDefault().resumeAppSwitches();
                            } catch (RemoteException e) {
                            }
                            try {
                                pendingIntent.send();
                            } catch (CanceledException e2) {
                                Log.w("StatusBar", "Sending intent failed: " + e2);
                            }
                            if (pendingIntent.isActivity()) {
                                BaseStatusBar.this.mAssistManager.hideAssist();
                                BaseStatusBar baseStatusBar = BaseStatusBar.this;
                                if (z && !z2) {
                                    z = true;
                                }
                                baseStatusBar.overrideActivityPendingAppTransition(z);
                            }
                        }
                    }.start();
                    BaseStatusBar.this.animateCollapsePanels(2, true, true);
                    BaseStatusBar.this.visibilityChanged(false);
                    return true;
                }
            }, wouldLaunchResolverActivity);
        }
    }

    public void animateCollapsePanels(int flags, boolean force) {
    }

    public void animateCollapsePanels(int flags, boolean force, boolean delayed) {
    }

    public void overrideActivityPendingAppTransition(boolean keyguardShowing) {
        if (keyguardShowing) {
            try {
                this.mWindowManagerService.overridePendingAppTransition(null, 0, 0, null);
            } catch (RemoteException e) {
                Log.w("StatusBar", "Error overriding app transition: " + e);
            }
        }
    }

    protected void visibilityChanged(boolean visible) {
        if (this.mVisible != visible) {
            this.mVisible = visible;
            if (!visible) {
                dismissPopups();
            }
        }
        updateVisibleToUser();
    }

    protected void updateVisibleToUser() {
        boolean oldVisibleToUser = this.mVisibleToUser;
        this.mVisibleToUser = this.mVisible ? this.mDeviceInteractive : false;
        if (oldVisibleToUser != this.mVisibleToUser) {
            handleVisibleToUserChanged(this.mVisibleToUser);
        }
    }

    protected void handleVisibleToUserChanged(boolean visibleToUser) {
        if (visibleToUser) {
            try {
                boolean pinnedHeadsUp = this.mHeadsUpManager.hasPinnedHeadsUp();
                if (!this.mShowLockscreenNotifications || this.mState != 1) {
                    if (!pinnedHeadsUp) {
                        if (this.mState != 0) {
                            if (this.mState == 2) {
                            }
                        }
                    }
                }
                int notificationLoad = this.mNotificationData.getActiveNotifications().size();
                if (!pinnedHeadsUp || !isPanelFullyCollapsed()) {
                    MetricsLogger.histogram(this.mContext, "note_load", notificationLoad);
                    return;
                }
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        this.mBarService.onPanelHidden();
    }

    public void clearNotificationEffects() {
        try {
            this.mBarService.clearNotificationEffects();
        } catch (RemoteException e) {
        }
    }

    void handleNotificationError(StatusBarNotification n, String message) {
        removeNotification(n.getKey(), null);
        try {
            this.mBarService.onNotificationError(n.getPackageName(), n.getTag(), n.getId(), n.getUid(), n.getInitialPid(), message, n.getUserId());
        } catch (RemoteException e) {
        }
    }

    protected StatusBarNotification removeNotificationViews(String key, RankingMap ranking) {
        Entry entry = this.mNotificationData.remove(key, ranking);
        if (entry == null) {
            Log.w("StatusBar", "removeNotification for unknown key: " + key);
            return null;
        }
        updateNotifications();
        return entry.notification;
    }

    protected Entry createNotificationViews(StatusBarNotification sbn) {
        if (DEBUG) {
            Log.d("StatusBar", "createNotificationViews(notification=" + sbn);
        }
        StatusBarIconView iconView = createIcon(sbn);
        if (iconView == null) {
            return null;
        }
        Entry entry = new Entry(sbn, iconView);
        if (inflateViews(entry, this.mStackScroller)) {
            return entry;
        }
        handleNotificationError(sbn, "Couldn't expand RemoteViews for: " + sbn);
        return null;
    }

    protected StatusBarIconView createIcon(StatusBarNotification sbn) {
        Notification n = sbn.getNotification();
        StatusBarIconView iconView = new StatusBarIconView(this.mContext, sbn.getPackageName() + "/0x" + Integer.toHexString(sbn.getId()), n);
        iconView.setScaleType(ScaleType.CENTER_INSIDE);
        Icon smallIcon = n.getSmallIcon();
        if (smallIcon == null) {
            handleNotificationError(sbn, "No small icon in notification from " + sbn.getPackageName());
            return null;
        }
        StatusBarIcon ic = new StatusBarIcon(sbn.getUser(), sbn.getPackageName(), smallIcon, n.iconLevel, n.number, n.tickerText);
        if (iconView.set(ic)) {
            return iconView;
        }
        handleNotificationError(sbn, "Couldn't create icon: " + ic);
        return null;
    }

    protected void addNotificationViews(Entry entry, RankingMap ranking) {
        if (entry != null) {
            this.mNotificationData.add(entry, ranking);
            updateNotifications();
        }
    }

    protected void updateRowStates() {
        int maxKeyguardNotifications = getMaxKeyguardNotifications();
        this.mKeyguardIconOverflowContainer.getIconsView().removeAllViews();
        ArrayList<Entry> activeNotifications = this.mNotificationData.getActiveNotifications();
        int N = activeNotifications.size();
        int visibleNotifications = 0;
        boolean onKeyguard = this.mState == 1;
        int i = 0;
        while (i < N) {
            Entry entry = (Entry) activeNotifications.get(i);
            if (onKeyguard) {
                entry.row.setExpansionDisabled(true);
            } else {
                entry.row.setExpansionDisabled(false);
                if (!entry.row.isUserLocked()) {
                    entry.row.setSystemExpanded(i == 0);
                }
            }
            boolean isInvisibleChild = !this.mGroupManager.isVisible(entry.notification);
            boolean showOnKeyguard = shouldShowOnKeyguard(entry.notification);
            if ((!isLockscreenPublicMode() || this.mShowLockscreenNotifications) && (!onKeyguard || (visibleNotifications < maxKeyguardNotifications && showOnKeyguard && !isInvisibleChild))) {
                boolean wasGone = entry.row.getVisibility() == 8;
                entry.row.setVisibility(0);
                if (!isInvisibleChild) {
                    if (wasGone) {
                        this.mStackScroller.generateAddAnimation(entry.row, true);
                    }
                    visibleNotifications++;
                }
            } else {
                entry.row.setVisibility(8);
                if (onKeyguard && showOnKeyguard && !isInvisibleChild) {
                    this.mKeyguardIconOverflowContainer.getIconsView().addNotification(entry);
                }
            }
            i++;
        }
        NotificationStackScrollLayout notificationStackScrollLayout = this.mStackScroller;
        boolean z = onKeyguard ? this.mKeyguardIconOverflowContainer.getIconsView().getChildCount() > 0 : false;
        notificationStackScrollLayout.updateOverflowContainerVisibility(z);
        this.mStackScroller.changeViewPosition(this.mDismissView, this.mStackScroller.getChildCount() - 1);
        this.mStackScroller.changeViewPosition(this.mEmptyShadeView, this.mStackScroller.getChildCount() - 2);
        this.mStackScroller.changeViewPosition(this.mKeyguardIconOverflowContainer, this.mStackScroller.getChildCount() - 3);
    }

    private boolean shouldShowOnKeyguard(StatusBarNotification sbn) {
        return this.mShowLockscreenNotifications && !this.mNotificationData.isAmbient(sbn.getKey());
    }

    protected void setZenMode(int mode) {
        if (isDeviceProvisioned()) {
            this.mZenMode = mode;
            updateNotifications();
        }
    }

    protected void setShowLockscreenNotifications(boolean show) {
        this.mShowLockscreenNotifications = show;
    }

    private void updateLockscreenNotificationSetting() {
        boolean show = Secure.getIntForUser(this.mContext.getContentResolver(), "lock_screen_show_notifications", 1, this.mCurrentUserId) != 0;
        boolean allowedByDpm = (this.mDevicePolicyManager.getKeyguardDisabledFeatures(null, this.mCurrentUserId) & 4) == 0;
        if (!show) {
            allowedByDpm = false;
        }
        setShowLockscreenNotifications(allowedByDpm);
    }

    public void updateNotification(StatusBarNotification notification, RankingMap ranking) {
        if (DEBUG) {
            Log.d("StatusBar", "updateNotification(" + notification + ")");
        }
        String key = notification.getKey();
        Entry entry = this.mNotificationData.get(key);
        if (entry != null) {
            StatusBarIcon ic;
            Notification n = notification.getNotification();
            if (DEBUG) {
                logUpdate(entry, n);
            }
            boolean applyInPlace = shouldApplyInPlace(entry, n);
            boolean shouldInterrupt = shouldInterrupt(entry, notification);
            boolean alertAgain = alertAgain(entry, n);
            entry.notification = notification;
            this.mGroupManager.onEntryUpdated(entry, entry.notification);
            boolean updateSuccessful = false;
            if (applyInPlace) {
                if (DEBUG) {
                    Log.d("StatusBar", "reusing notification for key: " + key);
                }
                try {
                    if (entry.icon != null) {
                        ic = new StatusBarIcon(notification.getUser(), notification.getPackageName(), n.getSmallIcon(), n.iconLevel, n.number, n.tickerText);
                        entry.icon.setNotification(n);
                        if (!entry.icon.set(ic)) {
                            handleNotificationError(notification, "Couldn't update icon: " + ic);
                            return;
                        }
                    }
                    updateNotificationViews(entry, notification);
                    updateSuccessful = true;
                } catch (RuntimeException e) {
                    Log.w("StatusBar", "Couldn't reapply views for package " + n.contentView.getPackage(), e);
                }
            }
            if (!updateSuccessful) {
                if (DEBUG) {
                    Log.d("StatusBar", "not reusing notification for key: " + key);
                }
                ic = new StatusBarIcon(notification.getUser(), notification.getPackageName(), n.getSmallIcon(), n.iconLevel, n.number, n.tickerText);
                entry.icon.setNotification(n);
                entry.icon.set(ic);
                inflateViews(entry, this.mStackScroller);
            }
            updateHeadsUp(key, entry, shouldInterrupt, alertAgain);
            this.mNotificationData.updateRanking(ranking);
            updateNotifications();
            updateNotificationVetoButton(entry.row, notification);
            if (DEBUG) {
                Log.d("StatusBar", "notification is " + (isNotificationForCurrentProfiles(notification) ? "" : "not ") + "for you");
            }
            setAreThereNotifications();
        }
    }

    private void logUpdate(Entry oldEntry, Notification n) {
        StatusBarNotification oldNotification = oldEntry.notification;
        Log.d("StatusBar", "old notification: when=" + oldNotification.getNotification().when + " ongoing=" + oldNotification.isOngoing() + " expanded=" + oldEntry.getContentView() + " contentView=" + oldNotification.getNotification().contentView + " bigContentView=" + oldNotification.getNotification().bigContentView + " publicView=" + oldNotification.getNotification().publicVersion + " rowParent=" + oldEntry.row.getParent());
        Log.d("StatusBar", "new notification: when=" + n.when + " ongoing=" + oldNotification.isOngoing() + " contentView=" + n.contentView + " bigContentView=" + n.bigContentView + " publicView=" + n.publicVersion);
    }

    private boolean shouldApplyInPlace(Entry entry, Notification n) {
        boolean contentsUnchanged;
        boolean bigContentsUnchanged;
        boolean headsUpContentsUnchanged;
        boolean publicUnchanged;
        StatusBarNotification oldNotification = entry.notification;
        RemoteViews oldContentView = oldNotification.getNotification().contentView;
        RemoteViews contentView = n.contentView;
        RemoteViews oldBigContentView = oldNotification.getNotification().bigContentView;
        RemoteViews bigContentView = n.bigContentView;
        RemoteViews oldHeadsUpContentView = oldNotification.getNotification().headsUpContentView;
        RemoteViews headsUpContentView = n.headsUpContentView;
        Notification oldPublicNotification = oldNotification.getNotification().publicVersion;
        RemoteViews remoteViews = oldPublicNotification != null ? oldPublicNotification.contentView : null;
        Notification publicNotification = n.publicVersion;
        RemoteViews remoteViews2 = publicNotification != null ? publicNotification.contentView : null;
        if (entry.getContentView() == null || contentView.getPackage() == null || oldContentView.getPackage() == null || !oldContentView.getPackage().equals(contentView.getPackage())) {
            contentsUnchanged = false;
        } else {
            contentsUnchanged = oldContentView.getLayoutId() == contentView.getLayoutId();
        }
        if (entry.getExpandedContentView() == null && bigContentView == null) {
            bigContentsUnchanged = true;
        } else if (entry.getExpandedContentView() == null || bigContentView == null || bigContentView.getPackage() == null || oldBigContentView.getPackage() == null || !oldBigContentView.getPackage().equals(bigContentView.getPackage())) {
            bigContentsUnchanged = false;
        } else {
            bigContentsUnchanged = oldBigContentView.getLayoutId() == bigContentView.getLayoutId();
        }
        if (oldHeadsUpContentView == null && headsUpContentView == null) {
            headsUpContentsUnchanged = true;
        } else if (oldHeadsUpContentView == null || headsUpContentView == null || headsUpContentView.getPackage() == null || oldHeadsUpContentView.getPackage() == null || !oldHeadsUpContentView.getPackage().equals(headsUpContentView.getPackage())) {
            headsUpContentsUnchanged = false;
        } else {
            headsUpContentsUnchanged = oldHeadsUpContentView.getLayoutId() == headsUpContentView.getLayoutId();
        }
        if (remoteViews == null && remoteViews2 == null) {
            publicUnchanged = true;
        } else if (remoteViews == null || remoteViews2 == null || remoteViews2.getPackage() == null || remoteViews.getPackage() == null || !remoteViews.getPackage().equals(remoteViews2.getPackage())) {
            publicUnchanged = false;
        } else {
            publicUnchanged = remoteViews.getLayoutId() == remoteViews2.getLayoutId();
        }
        return (contentsUnchanged && bigContentsUnchanged && headsUpContentsUnchanged) ? publicUnchanged : false;
    }

    private void updateNotificationViews(Entry entry, StatusBarNotification notification) {
        RemoteViews remoteViews;
        RemoteViews contentView = notification.getNotification().contentView;
        RemoteViews bigContentView = notification.getNotification().bigContentView;
        RemoteViews headsUpContentView = notification.getNotification().headsUpContentView;
        Notification publicVersion = notification.getNotification().publicVersion;
        if (publicVersion != null) {
            remoteViews = publicVersion.contentView;
        } else {
            remoteViews = null;
        }
        contentView.reapply(this.mContext, entry.getContentView(), this.mOnClickHandler);
        if (!(bigContentView == null || entry.getExpandedContentView() == null)) {
            bigContentView.reapply(notification.getPackageContext(this.mContext), entry.getExpandedContentView(), this.mOnClickHandler);
        }
        View headsUpChild = entry.getHeadsUpContentView();
        if (!(headsUpContentView == null || headsUpChild == null)) {
            headsUpContentView.reapply(notification.getPackageContext(this.mContext), headsUpChild, this.mOnClickHandler);
        }
        if (!(remoteViews == null || entry.getPublicContentView() == null)) {
            remoteViews.reapply(notification.getPackageContext(this.mContext), entry.getPublicContentView(), this.mOnClickHandler);
        }
        this.mNotificationClicker.register(entry.row, notification);
        entry.row.setStatusBarNotification(notification);
        entry.row.notifyContentUpdated();
        entry.row.resetHeight();
    }

    protected void notifyHeadsUpScreenOff() {
        maybeEscalateHeadsUp();
    }

    private boolean alertAgain(Entry oldEntry, Notification newNotification) {
        return oldEntry == null || !oldEntry.hasInterrupted() || (newNotification.flags & 8) == 0;
    }

    protected boolean shouldInterrupt(Entry entry) {
        return shouldInterrupt(entry, entry.notification);
    }

    protected boolean shouldInterrupt(Entry entry, StatusBarNotification sbn) {
        if (this.mNotificationData.shouldFilterOut(sbn)) {
            Log.d("StatusBar", "Skipping HUN check for " + sbn.getKey() + " since it's filtered out.");
            return false;
        } else if (isSnoozedPackage(sbn)) {
            Log.d("StatusBar", "Snoozed package notification: " + (sbn != null ? sbn.getNotification() : ""));
            return false;
        } else {
            boolean isTouchExplorationEnabled;
            Notification notification = sbn.getNotification();
            boolean isNoisy = ((notification.defaults & 1) == 0 && (notification.defaults & 2) == 0 && notification.sound == null) ? notification.vibrate != null : true;
            boolean isHighPriority = sbn.getScore() >= 10;
            boolean isFullscreen = notification.fullScreenIntent != null;
            boolean hasTicker = this.mHeadsUpTicker && !TextUtils.isEmpty(notification.tickerText);
            boolean isAllowed = notification.extras.getInt("headsup", 1) != 0;
            if (isFullscreen) {
                isTouchExplorationEnabled = this.mAccessibilityManager.isTouchExplorationEnabled();
            } else {
                isTouchExplorationEnabled = false;
            }
            boolean interrupt = ((isFullscreen || (isHighPriority && (isNoisy || hasTicker))) && isAllowed && !isTouchExplorationEnabled && !entry.hasJustLaunchedFullScreenIntent() && this.mPowerManager.isScreenOn() && (!this.mStatusBarKeyguardViewManager.isShowing() || this.mStatusBarKeyguardViewManager.isOccluded())) ? !this.mStatusBarKeyguardViewManager.isInputRestricted() : false;
            if (interrupt) {
                try {
                    if (!this.mDreamManager.isDreaming()) {
                        interrupt = true;
                        Log.d("StatusBar", "interrupt: " + interrupt);
                        return interrupt;
                    }
                } catch (RemoteException e) {
                    Log.d("StatusBar", "failed to query dream manager", e);
                }
            }
            interrupt = false;
            Log.d("StatusBar", "interrupt: " + interrupt);
            return interrupt;
        }
    }

    public void setBouncerShowing(boolean bouncerShowing) {
        this.mBouncerShowing = bouncerShowing;
    }

    public boolean isBouncerShowing() {
        return this.mBouncerShowing;
    }

    public void destroy() {
        this.mContext.unregisterReceiver(this.mBroadcastReceiver);
        try {
            this.mNotificationListener.unregisterAsSystemService();
        } catch (RemoteException e) {
        }
    }

    protected PackageManager getPackageManagerForUser(int userId) {
        Context contextForUser = this.mContext;
        if (userId >= 0) {
            try {
                contextForUser = this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 4, new UserHandle(userId));
            } catch (NameNotFoundException e) {
            }
        }
        return contextForUser.getPackageManager();
    }

    public void logNotificationExpansion(String key, boolean userAction, boolean expanded) {
        try {
            this.mBarService.onNotificationExpansionChanged(key, userAction, expanded);
        } catch (RemoteException e) {
        }
    }

    public boolean isKeyguardSecure() {
        if (this.mStatusBarKeyguardViewManager != null) {
            return this.mStatusBarKeyguardViewManager.isSecure();
        }
        Slog.w("StatusBar", "isKeyguardSecure() called before startKeyguard(), returning false", new Throwable());
        return false;
    }

    public void showAssistDisclosure() {
        if (this.mAssistManager != null) {
            this.mAssistManager.showDisclosure();
        }
    }

    public void startAssist(Bundle args) {
        if (this.mAssistManager != null) {
            this.mAssistManager.startAssist(args);
        }
    }

    public void showDefaultAccountStatus(int subId) {
    }

    public void hideDefaultAccountStatus() {
    }

    public boolean isFloatPanelOpened() {
        return this.mFloatPanelView != null ? this.mFloatPanelView.isShown() : false;
    }

    public void changgeFloatPanelFocus(boolean focus) {
        if (this.mFloatPanelView != null) {
            this.mWindowManager.updateViewLayout(this.mFloatPanelView, getFloatLayoutParams(this.mFloatPanelView.getLayoutParams(), focus));
        }
    }

    public void setExtensionButtonVisibility(int visibility) {
    }

    public void setFloatButtonVisibility(int visibility) {
    }

    public void setFloatModeButtonVisibility(int visibility) {
    }

    public void setSplitModeButtonVisibility(int visibility) {
    }

    public void setLineVisibility(int visibility) {
    }

    public void updateFloatButtonIcon(boolean flaotPanelOpen) {
    }

    public void toggleFloatApps() {
        this.mHandler.removeMessages(2029);
        this.mHandler.sendEmptyMessage(2029);
    }

    public void closeFloatPanel(int delayMillis) {
        this.mHandler.removeMessages(2030);
        this.mHandler.sendEmptyMessageDelayed(2030, (long) delayMillis);
    }

    public void cancelCloseFloatPanel() {
        this.mHandler.removeMessages(2030);
    }

    public void postCloseFloatPanel() {
        closeFloatPanel(6000);
    }

    protected LayoutParams getFloatLayoutParams(ViewGroup.LayoutParams layoutParams, boolean focus) {
        return null;
    }

    public void toggleFloatPanel() {
        LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
        if (this.mFloatPanelView == null) {
            this.mFloatPanelView = (FloatPanelView) inflater.inflate(R.layout.float_control_panel, null, false);
            this.mFloatPanelView.setBar(this);
            if (this.mEditButtonListner == null) {
                this.mEditButtonListner = new OnClickListener() {
                    public void onClick(View v) {
                        if (BaseStatusBar.this.mFloatPanelView != null) {
                            BaseStatusBar.this.mFloatPanelView.enterExtensionMode();
                            BaseStatusBar.this.postCloseFloatPanel();
                        }
                    }
                };
            }
            ((PhoneStatusBar) this).setNavigationBarEditFloatListener(this.mEditButtonListner);
        }
        Log.d("StatusBar", "toggleFloatPanel: mFloatPanelView = " + this.mFloatPanelView + ", shown = " + this.mFloatPanelView.isShown());
        boolean floatPanelOpen = this.mFloatPanelView.isShown();
        if (floatPanelOpen) {
            this.mFloatPanelView.setVisibility(4);
            if (this instanceof PhoneStatusBar) {
                ((PhoneStatusBar) this).updateNavigationBarIcon(0);
                ((PhoneStatusBar) this).updateFloatButtonIcon(floatPanelOpen);
            } else {
                this.mFloatPanelView.setSystemUiVisibility(0);
            }
            this.mWindowManager.removeView(this.mFloatPanelView);
            this.mFloatPanelView = null;
            cancelCloseFloatPanel();
            if (this.mNavigationBarView != null) {
                this.mNavigationBarView.refreshRestoreButton();
                return;
            }
            return;
        }
        this.mWindowManager.addView(this.mFloatPanelView, getFloatLayoutParams(this.mFloatPanelView.getLayoutParams(), true));
        this.mFloatPanelView.setVisibility(0);
        if (this instanceof PhoneStatusBar) {
            ((PhoneStatusBar) this).updateNavigationBarIcon(23068672);
            ((PhoneStatusBar) this).updateFloatButtonIcon(floatPanelOpen);
        } else {
            this.mFloatPanelView.setSystemUiVisibility(23068672);
        }
        setExtensionButtonVisibility(0);
        postCloseFloatPanel();
    }

    public void toggleFloatPanelScreenOff() {
        Log.d("StatusBar", "toggleFloatPanelScreenOff");
        this.mSecurityModel = new KeyguardSecurityModel(this.mContext);
        if (this.mSecurityModel.getSecurityMode() == SecurityMode.None) {
            closeFloatPanel();
        } else {
            closeFloatPanelNotShowFloatButton();
        }
    }

    public void closeFloatPanel() {
        Log.d("StatusBar", "closeFloatPanel: mFloatPanelView = " + this.mFloatPanelView + ",visible = " + 0 + ", shown = " + (this.mFloatPanelView != null ? Boolean.valueOf(this.mFloatPanelView.isShown()) : "null"));
        if (this.mFloatPanelView != null && this.mFloatPanelView.isShown()) {
            this.mFloatPanelView.setVisibility(4);
            if (this instanceof PhoneStatusBar) {
                ((PhoneStatusBar) this).updateNavigationBarIcon(0);
                ((PhoneStatusBar) this).updateFloatButtonIcon(true);
            } else {
                this.mFloatPanelView.setSystemUiVisibility(0);
            }
            this.mWindowManager.removeView(this.mFloatPanelView);
            this.mFloatPanelView = null;
            if (this.mNavigationBarView != null) {
                this.mNavigationBarView.refreshRestoreButton();
            }
        }
    }

    public void closeFloatPanelNotShowFloatButton() {
        Log.d("StatusBar", "closeFloatPanelNotShowFloatButton");
        if (this.mFloatPanelView == null || !this.mFloatPanelView.isShown()) {
            if (this instanceof PhoneStatusBar) {
                ((PhoneStatusBar) this).updateNavigationBarIcon(60817408);
                ((PhoneStatusBar) this).updateFloatButtonIcon(true);
            }
            setExtensionButtonVisibility(4);
            setFloatButtonVisibility(4);
            if (this.mIsSplitModeEnable) {
                setFloatModeButtonVisibility(4);
                setSplitModeButtonVisibility(4);
                setLineVisibility(4);
                return;
            }
            return;
        }
        this.mFloatPanelView.setVisibility(4);
        if (this instanceof PhoneStatusBar) {
            ((PhoneStatusBar) this).updateNavigationBarIcon(60817408);
            ((PhoneStatusBar) this).updateFloatButtonIcon(true);
            Log.d("StatusBar", "#updateNavigationBarIcon#");
        } else {
            this.mFloatPanelView.setSystemUiVisibility(60817408);
        }
        setExtensionButtonVisibility(4);
        setFloatButtonVisibility(4);
        if (this.mIsSplitModeEnable) {
            setFloatModeButtonVisibility(4);
            setSplitModeButtonVisibility(4);
            setLineVisibility(4);
        }
        this.mWindowManager.removeView(this.mFloatPanelView);
        this.mFloatPanelView = null;
    }
}
