package com.android.systemui.recents.misc;

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityManager.StackInfo;
import android.app.ActivityManager.TaskThumbnail;
import android.app.ActivityManagerNative;
import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.app.ITaskStackListener;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.util.Log;
import android.util.MutableBoolean;
import android.util.Pair;
import android.util.SparseArray;
import android.view.Display;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.app.AssistUtils;
import com.android.systemui.Prefs;
import com.android.systemui.R;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SystemServicesProxy {
    static final HandlerThread sBgThread = new HandlerThread("Recents-SystemServicesProxy", 10);
    static final Options sBitmapOptions = new Options();
    AccessibilityManager mAccm;
    ActivityManager mAm;
    ComponentName mAssistComponent;
    AssistUtils mAssistUtils;
    AppWidgetManager mAwm;
    Canvas mBgProtectionCanvas;
    Paint mBgProtectionPaint;
    Handler mBgThreadHandler;
    Display mDisplay;
    int mDummyThumbnailHeight;
    int mDummyThumbnailWidth;
    IActivityManager mIam = ActivityManagerNative.getDefault();
    IPackageManager mIpm;
    PackageManager mPm;
    String mRecentsPackage;
    WindowManager mWm;

    static {
        sBgThread.start();
        sBitmapOptions.inMutable = true;
    }

    public SystemServicesProxy(Context context) {
        this.mAccm = AccessibilityManager.getInstance(context);
        this.mAm = (ActivityManager) context.getSystemService("activity");
        this.mAwm = AppWidgetManager.getInstance(context);
        this.mPm = context.getPackageManager();
        this.mIpm = AppGlobals.getPackageManager();
        this.mAssistUtils = new AssistUtils(context);
        this.mWm = (WindowManager) context.getSystemService("window");
        this.mDisplay = this.mWm.getDefaultDisplay();
        this.mRecentsPackage = context.getPackageName();
        this.mBgThreadHandler = new Handler(sBgThread.getLooper());
        Resources res = context.getResources();
        this.mDummyThumbnailWidth = res.getDimensionPixelSize(17104898);
        this.mDummyThumbnailHeight = res.getDimensionPixelSize(17104897);
        this.mBgProtectionPaint = new Paint();
        this.mBgProtectionPaint.setXfermode(new PorterDuffXfermode(Mode.DST_ATOP));
        this.mBgProtectionPaint.setColor(-1);
        this.mBgProtectionCanvas = new Canvas();
        this.mAssistComponent = this.mAssistUtils.getAssistComponentForUser(UserHandle.myUserId());
    }

    public List<RecentTaskInfo> getRecentTasks(int numLatestTasks, int userId, boolean isTopTaskHome) {
        if (this.mAm == null) {
            return null;
        }
        List<RecentTaskInfo> tasks = this.mAm.getRecentTasksForUser(Math.max(10, numLatestTasks), 15, userId);
        if (tasks == null) {
            return new ArrayList();
        }
        boolean isFirstValidTask = true;
        Iterator<RecentTaskInfo> iter = tasks.iterator();
        while (iter.hasNext()) {
            if (!((((RecentTaskInfo) iter.next()).baseIntent.getFlags() & 8388608) == 8388608) || (!isTopTaskHome && isFirstValidTask)) {
                isFirstValidTask = false;
            } else {
                iter.remove();
            }
        }
        return tasks.subList(0, Math.min(tasks.size(), numLatestTasks));
    }

    private List<RunningTaskInfo> getRunningTasks(int numTasks) {
        if (this.mAm == null) {
            return null;
        }
        return this.mAm.getRunningTasks(numTasks);
    }

    public RunningTaskInfo getTopMostTask() {
        List<RunningTaskInfo> tasks = getRunningTasks(1);
        if (tasks == null || tasks.isEmpty()) {
            return null;
        }
        return (RunningTaskInfo) tasks.get(0);
    }

    public boolean isRecentsTopMost(RunningTaskInfo topTask, MutableBoolean isHomeTopMost) {
        if (topTask != null) {
            ComponentName topActivity = topTask.topActivity;
            if (topActivity.getPackageName().equals("com.android.systemui") && topActivity.getClassName().equals("com.android.systemui.recents.RecentsActivity")) {
                if (isHomeTopMost != null) {
                    isHomeTopMost.value = false;
                }
                return true;
            } else if (isHomeTopMost != null) {
                isHomeTopMost.value = isInHomeStack(topTask.id);
            }
        }
        return false;
    }

    public Rect getTaskBounds(int stackId) {
        StackInfo info = (StackInfo) getAllStackInfos().get(stackId);
        if (info != null) {
            return info.bounds;
        }
        return new Rect();
    }

    public void resizeTask(int taskId, Rect bounds) {
        if (this.mIam != null) {
            try {
                this.mIam.resizeTask(taskId, bounds);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public SparseArray<StackInfo> getAllStackInfos() {
        if (this.mIam == null) {
            return new SparseArray();
        }
        try {
            SparseArray<StackInfo> stacks = new SparseArray();
            List<StackInfo> infos = this.mIam.getAllStackInfos();
            int stackCount = infos.size();
            for (int i = 0; i < stackCount; i++) {
                StackInfo info = (StackInfo) infos.get(i);
                stacks.put(info.stackId, info);
            }
            return stacks;
        } catch (RemoteException e) {
            e.printStackTrace();
            return new SparseArray();
        }
    }

    public int getFocusedStack() {
        if (this.mIam == null) {
            return -1;
        }
        try {
            return this.mIam.getFocusedStackId();
        } catch (RemoteException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public boolean isInHomeStack(int taskId) {
        if (this.mAm == null) {
            return false;
        }
        return this.mAm.isInHomeStack(taskId);
    }

    public Bitmap getTaskThumbnail(int taskId) {
        if (this.mAm == null) {
            return null;
        }
        Bitmap thumbnail = getThumbnail(this.mAm, taskId);
        if (thumbnail != null) {
            thumbnail.setHasAlpha(false);
            if (Color.alpha(thumbnail.getPixel(0, 0)) == 0) {
                this.mBgProtectionCanvas.setBitmap(thumbnail);
                this.mBgProtectionCanvas.drawRect(0.0f, 0.0f, (float) thumbnail.getWidth(), (float) thumbnail.getHeight(), this.mBgProtectionPaint);
                this.mBgProtectionCanvas.setBitmap(null);
                Log.e("SystemServicesProxy", "Invalid screenshot detected from getTaskThumbnail()");
            }
        }
        return thumbnail;
    }

    public static Bitmap getThumbnail(ActivityManager activityManager, int taskId) {
        TaskThumbnail taskThumbnail = activityManager.getTaskThumbnail(taskId);
        if (taskThumbnail == null) {
            return null;
        }
        Bitmap thumbnail = taskThumbnail.mainThumbnail;
        ParcelFileDescriptor descriptor = taskThumbnail.thumbnailFileDescriptor;
        if (thumbnail == null && descriptor != null) {
            thumbnail = BitmapFactory.decodeFileDescriptor(descriptor.getFileDescriptor(), null, sBitmapOptions);
        }
        if (descriptor != null) {
            try {
                descriptor.close();
            } catch (IOException e) {
            }
        }
        return thumbnail;
    }

    public void moveTaskToFront(int taskId, ActivityOptions opts) {
        if (this.mAm != null) {
            if (opts != null) {
                this.mAm.moveTaskToFront(taskId, 1, opts.toBundle());
            } else {
                this.mAm.moveTaskToFront(taskId, 1);
            }
        }
    }

    public void removeTask(final int taskId) {
        if (this.mAm != null) {
            this.mBgThreadHandler.post(new Runnable() {
                public void run() {
                    SystemServicesProxy.this.mAm.removeTask(taskId);
                }
            });
        }
    }

    public ActivityInfo getActivityInfo(ComponentName cn, int userId) {
        if (this.mIpm == null) {
            return null;
        }
        try {
            return this.mIpm.getActivityInfo(cn, 128, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getActivityLabel(ActivityInfo info) {
        if (this.mPm == null) {
            return null;
        }
        return info.loadLabel(this.mPm).toString();
    }

    public String getApplicationLabel(Intent baseIntent, int userId) {
        String str = null;
        if (this.mPm == null) {
            return null;
        }
        ResolveInfo ri = this.mPm.resolveActivityAsUser(baseIntent, 0, userId);
        CharSequence loadLabel = ri != null ? ri.loadLabel(this.mPm) : null;
        if (loadLabel != null) {
            str = loadLabel.toString();
        }
        return str;
    }

    public String getContentDescription(Intent baseIntent, int userId, String activityLabel, Resources res) {
        String applicationLabel = getApplicationLabel(baseIntent, userId);
        if (applicationLabel == null) {
            return getBadgedLabel(activityLabel, userId);
        }
        String badgedApplicationLabel = getBadgedLabel(applicationLabel, userId);
        if (!applicationLabel.equals(activityLabel)) {
            badgedApplicationLabel = res.getString(R.string.accessibility_recents_task_header, new Object[]{badgedApplicationLabel, activityLabel});
        }
        return badgedApplicationLabel;
    }

    public Drawable getActivityIcon(ActivityInfo info, int userId) {
        if (this.mPm == null) {
            return null;
        }
        return getBadgedIcon(info.loadIcon(this.mPm), userId);
    }

    public Drawable getBadgedIcon(Drawable icon, int userId) {
        if (userId != UserHandle.myUserId()) {
            return this.mPm.getUserBadgedIcon(icon, new UserHandle(userId));
        }
        return icon;
    }

    public String getBadgedLabel(String label, int userId) {
        if (userId != UserHandle.myUserId()) {
            return this.mPm.getUserBadgedLabel(label, new UserHandle(userId)).toString();
        }
        return label;
    }

    public String getHomeActivityPackageName() {
        if (this.mPm == null) {
            return null;
        }
        ArrayList<ResolveInfo> homeActivities = new ArrayList();
        ComponentName defaultHomeActivity = this.mPm.getHomeActivities(homeActivities);
        if (defaultHomeActivity != null) {
            return defaultHomeActivity.getPackageName();
        }
        if (homeActivities.size() == 1) {
            ResolveInfo info = (ResolveInfo) homeActivities.get(0);
            if (info.activityInfo != null) {
                return info.activityInfo.packageName;
            }
        }
        return null;
    }

    public boolean isForegroundUserOwner() {
        boolean z = false;
        if (this.mAm == null) {
            return false;
        }
        ActivityManager activityManager = this.mAm;
        if (ActivityManager.getCurrentUser() == 0) {
            z = true;
        }
        return z;
    }

    public int getSearchAppWidgetId(Context context) {
        return Prefs.getInt(context, "searchAppWidgetId", -1);
    }

    public AppWidgetProviderInfo getOrBindSearchAppWidget(Context context, AppWidgetHost host) {
        int searchWidgetId = Prefs.getInt(context, "searchAppWidgetId", -1);
        AppWidgetProviderInfo searchWidgetInfo = this.mAwm.getAppWidgetInfo(searchWidgetId);
        AppWidgetProviderInfo resolvedSearchWidgetInfo = resolveSearchAppWidget();
        if (searchWidgetInfo == null || resolvedSearchWidgetInfo == null || !searchWidgetInfo.provider.equals(resolvedSearchWidgetInfo.provider)) {
            if (searchWidgetId != -1) {
                host.deleteAppWidgetId(searchWidgetId);
            }
            if (resolvedSearchWidgetInfo != null) {
                Pair<Integer, AppWidgetProviderInfo> widgetInfo = bindSearchAppWidget(host, resolvedSearchWidgetInfo);
                if (widgetInfo != null) {
                    Prefs.putInt(context, "searchAppWidgetId", ((Integer) widgetInfo.first).intValue());
                    Prefs.putString(context, "searchAppWidgetPackage", ((AppWidgetProviderInfo) widgetInfo.second).provider.getPackageName());
                    return (AppWidgetProviderInfo) widgetInfo.second;
                }
            }
            Prefs.remove(context, "searchAppWidgetId");
            Prefs.remove(context, "searchAppWidgetPackage");
            return null;
        }
        if (Prefs.getString(context, "searchAppWidgetPackage", null) == null) {
            Prefs.putString(context, "searchAppWidgetPackage", searchWidgetInfo.provider.getPackageName());
        }
        return searchWidgetInfo;
    }

    private AppWidgetProviderInfo resolveSearchAppWidget() {
        if (this.mAssistComponent == null) {
            return null;
        }
        for (AppWidgetProviderInfo info : this.mAwm.getInstalledProviders(4)) {
            if (info.provider.getPackageName().equals(this.mAssistComponent.getPackageName())) {
                return info;
            }
        }
        return null;
    }

    private Pair<Integer, AppWidgetProviderInfo> bindSearchAppWidget(AppWidgetHost host, AppWidgetProviderInfo resolvedSearchWidgetInfo) {
        if (this.mAwm == null || this.mAssistComponent == null) {
            return null;
        }
        int searchWidgetId = host.allocateAppWidgetId();
        Bundle opts = new Bundle();
        opts.putInt("appWidgetCategory", 4);
        if (this.mAwm.bindAppWidgetIdIfAllowed(searchWidgetId, resolvedSearchWidgetInfo.provider, opts)) {
            return new Pair(Integer.valueOf(searchWidgetId), resolvedSearchWidgetInfo);
        }
        host.deleteAppWidgetId(searchWidgetId);
        return null;
    }

    public boolean isTouchExplorationEnabled() {
        boolean z = false;
        if (this.mAccm == null) {
            return false;
        }
        if (this.mAccm.isEnabled()) {
            z = this.mAccm.isTouchExplorationEnabled();
        }
        return z;
    }

    public int getGlobalSetting(Context context, String setting) {
        return Global.getInt(context.getContentResolver(), setting, 0);
    }

    public int getSystemSetting(Context context, String setting) {
        return System.getInt(context.getContentResolver(), setting, 0);
    }

    public String getSystemProperty(String key) {
        return SystemProperties.get(key);
    }

    public Rect getWindowRect() {
        Rect windowRect = new Rect();
        if (this.mWm == null) {
            return windowRect;
        }
        Point p = new Point();
        this.mWm.getDefaultDisplay().getRealSize(p);
        windowRect.set(0, 0, p.x, p.y);
        return windowRect;
    }

    public boolean startActivityFromRecents(Context context, int taskId, String taskName, ActivityOptions options) {
        Bundle bundle = null;
        if (this.mIam != null) {
            try {
                IActivityManager iActivityManager = this.mIam;
                if (options != null) {
                    bundle = options.toBundle();
                }
                iActivityManager.startActivityFromRecents(taskId, bundle);
                return true;
            } catch (Exception e) {
                Console.logError(context, context.getString(R.string.recents_launch_error_message, new Object[]{taskName}));
            }
        }
        return false;
    }

    public void startInPlaceAnimationOnFrontMostApplication(ActivityOptions opts) {
        if (this.mIam != null) {
            try {
                this.mIam.startInPlaceAnimationOnFrontMostApplication(opts);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void registerTaskStackListener(ITaskStackListener listener) {
        if (this.mIam != null) {
            try {
                this.mIam.registerTaskStackListener(listener);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
