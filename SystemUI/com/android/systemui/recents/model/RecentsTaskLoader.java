package com.android.systemui.recents.model;

import android.app.ActivityManager.TaskDescription;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import com.android.systemui.R;
import com.android.systemui.assis.app.MAIN.EVENT;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.RecentsPackageMonitor.PackageCallbacks;
import com.android.systemui.recents.model.RecentsTaskLoadPlan.Options;
import com.android.systemui.recents.model.Task.TaskKey;

public class RecentsTaskLoader {
    static int INVALID_TASK_ID = -1;
    static RecentsTaskLoader sInstance;
    StringLruCache mActivityLabelCache;
    DrawableLruCache mApplicationIconCache;
    StringLruCache mContentDescriptionCache;
    BitmapDrawable mDefaultApplicationIcon;
    Bitmap mDefaultThumbnail = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
    TaskResourceLoadQueue mLoadQueue;
    TaskResourceLoader mLoader;
    int mMaxIconCacheSize;
    int mMaxThumbnailCacheSize;
    int mNumVisibleTasksLoaded;
    int mNumVisibleThumbnailsLoaded;
    RecentsPackageMonitor mPackageMonitor;
    SystemServicesProxy mSystemServicesProxy;
    BitmapLruCache mThumbnailCache;

    private RecentsTaskLoader(Context context) {
        this.mMaxThumbnailCacheSize = context.getResources().getInteger(R.integer.config_recents_max_thumbnail_count);
        this.mMaxIconCacheSize = context.getResources().getInteger(R.integer.config_recents_max_icon_count);
        int iconCacheSize = this.mMaxIconCacheSize;
        int thumbnailCacheSize = this.mMaxThumbnailCacheSize;
        Bitmap icon = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
        icon.eraseColor(0);
        this.mDefaultThumbnail.setHasAlpha(false);
        this.mDefaultThumbnail.eraseColor(-1);
        this.mDefaultApplicationIcon = new BitmapDrawable(context.getResources(), icon);
        this.mSystemServicesProxy = new SystemServicesProxy(context);
        this.mPackageMonitor = new RecentsPackageMonitor();
        this.mLoadQueue = new TaskResourceLoadQueue();
        this.mApplicationIconCache = new DrawableLruCache(iconCacheSize);
        this.mThumbnailCache = new BitmapLruCache(thumbnailCacheSize);
        this.mActivityLabelCache = new StringLruCache(100);
        this.mContentDescriptionCache = new StringLruCache(100);
        this.mLoader = new TaskResourceLoader(this.mLoadQueue, this.mApplicationIconCache, this.mThumbnailCache, this.mDefaultThumbnail, this.mDefaultApplicationIcon);
    }

    public static RecentsTaskLoader initialize(Context context) {
        if (sInstance == null) {
            sInstance = new RecentsTaskLoader(context);
        }
        return sInstance;
    }

    public static RecentsTaskLoader getInstance() {
        return sInstance;
    }

    public SystemServicesProxy getSystemServicesProxy() {
        return this.mSystemServicesProxy;
    }

    public String getAndUpdateActivityLabel(TaskKey taskKey, TaskDescription td, SystemServicesProxy ssp, ActivityInfoHandle infoHandle) {
        if (td != null && td.getLabel() != null) {
            return td.getLabel();
        }
        String label = (String) this.mActivityLabelCache.getAndInvalidateIfModified(taskKey);
        if (label != null) {
            return label;
        }
        if (infoHandle.info == null) {
            infoHandle.info = ssp.getActivityInfo(taskKey.baseIntent.getComponent(), taskKey.userId);
        }
        if (infoHandle.info != null) {
            label = ssp.getActivityLabel(infoHandle.info);
            this.mActivityLabelCache.put(taskKey, label);
            return label;
        }
        Log.w("RecentsTaskLoader", "Missing ActivityInfo for " + taskKey.baseIntent.getComponent() + " u=" + taskKey.userId);
        return "";
    }

    public String getAndUpdateContentDescription(TaskKey taskKey, String activityLabel, SystemServicesProxy ssp, Resources res) {
        String label = (String) this.mContentDescriptionCache.getAndInvalidateIfModified(taskKey);
        if (label != null) {
            return label;
        }
        if (activityLabel.isEmpty()) {
            return "";
        }
        label = ssp.getContentDescription(taskKey.baseIntent, taskKey.userId, activityLabel, res);
        if (label != null) {
            this.mContentDescriptionCache.put(taskKey, label);
            return label;
        }
        Log.w("RecentsTaskLoader", "Missing content description for " + taskKey.baseIntent.getComponent() + " u=" + taskKey.userId);
        return "";
    }

    public Drawable getAndUpdateActivityIcon(TaskKey taskKey, TaskDescription td, SystemServicesProxy ssp, Resources res, ActivityInfoHandle infoHandle, boolean loadIfNotCached) {
        Drawable icon = (Drawable) this.mApplicationIconCache.getAndInvalidateIfModified(taskKey);
        if (icon != null) {
            return icon;
        }
        if (loadIfNotCached) {
            Drawable tdDrawable = this.mLoader.getTaskDescriptionIcon(taskKey, td.getInMemoryIcon(), td.getIconFilename(), ssp, res);
            if (tdDrawable != null) {
                this.mApplicationIconCache.put(taskKey, tdDrawable);
                return tdDrawable;
            }
            if (infoHandle.info == null) {
                infoHandle.info = ssp.getActivityInfo(taskKey.baseIntent.getComponent(), taskKey.userId);
            }
            if (infoHandle.info != null) {
                icon = ssp.getActivityIcon(infoHandle.info, taskKey.userId);
                if (icon != null) {
                    this.mApplicationIconCache.put(taskKey, icon);
                    return icon;
                }
            }
        }
        return null;
    }

    public Bitmap getAndUpdateThumbnail(TaskKey taskKey, SystemServicesProxy ssp, boolean loadIfNotCached) {
        Bitmap thumbnail = (Bitmap) this.mThumbnailCache.getAndInvalidateIfModified(taskKey);
        if (thumbnail != null) {
            return thumbnail;
        }
        if (RecentsConfiguration.getInstance().svelteLevel < 3 && loadIfNotCached) {
            thumbnail = ssp.getTaskThumbnail(taskKey.id);
            if (thumbnail != null) {
                this.mThumbnailCache.put(taskKey, thumbnail);
                return thumbnail;
            }
        }
        return null;
    }

    public int getActivityPrimaryColor(TaskDescription td, RecentsConfiguration config) {
        if (td == null || td.getPrimaryColor() == 0) {
            return config.taskBarViewDefaultBackgroundColor;
        }
        return td.getPrimaryColor();
    }

    public int getApplicationIconCacheSize() {
        return this.mMaxIconCacheSize;
    }

    public int getThumbnailCacheSize() {
        return this.mMaxThumbnailCacheSize;
    }

    public RecentsTaskLoadPlan createLoadPlan(Context context) {
        return new RecentsTaskLoadPlan(context, RecentsConfiguration.getInstance(), this.mSystemServicesProxy);
    }

    public void preloadTasks(RecentsTaskLoadPlan plan, boolean isTopTaskHome) {
        plan.preloadPlan(this, isTopTaskHome);
    }

    public void loadTasks(Context context, RecentsTaskLoadPlan plan, Options opts) {
        if (opts == null) {
            throw new RuntimeException("Requires load options");
        }
        plan.executePlan(opts, this, this.mLoadQueue);
        if (!opts.onlyLoadForCache) {
            this.mNumVisibleTasksLoaded = opts.numVisibleTasks;
            this.mNumVisibleThumbnailsLoaded = opts.numVisibleTaskThumbnails;
            this.mLoader.start(context);
        }
    }

    public void loadTaskData(Task t) {
        Drawable applicationIcon = (Drawable) this.mApplicationIconCache.getAndInvalidateIfModified(t.key);
        Bitmap thumbnail = (Bitmap) this.mThumbnailCache.getAndInvalidateIfModified(t.key);
        boolean requiresLoad = applicationIcon == null || thumbnail == null;
        if (applicationIcon == null) {
            applicationIcon = this.mDefaultApplicationIcon;
        }
        if (requiresLoad) {
            this.mLoadQueue.addTask(t);
        }
        if (thumbnail == this.mDefaultThumbnail) {
            thumbnail = null;
        }
        t.notifyTaskDataLoaded(thumbnail, applicationIcon);
    }

    public void unloadTaskData(Task t) {
        this.mLoadQueue.removeTask(t);
        t.notifyTaskDataUnloaded(null, this.mDefaultApplicationIcon);
    }

    public void deleteTaskData(Task t, boolean notifyTaskDataUnloaded) {
        this.mLoadQueue.removeTask(t);
        this.mThumbnailCache.remove(t.key);
        this.mApplicationIconCache.remove(t.key);
        if (notifyTaskDataUnloaded) {
            t.notifyTaskDataUnloaded(null, this.mDefaultApplicationIcon);
        }
    }

    void stopLoader() {
        this.mLoader.stop();
        this.mLoadQueue.clearTasks();
    }

    public void registerReceivers(Context context, PackageCallbacks cb) {
        this.mPackageMonitor.register(context, cb);
    }

    public void unregisterReceivers() {
        this.mPackageMonitor.unregister();
    }

    public void onTrimMemory(int level) {
        RecentsConfiguration config = RecentsConfiguration.getInstance();
        switch (level) {
            case 5:
            case EVENT.CHECK_DYNAMIC_PACK /*40*/:
                this.mThumbnailCache.trimToSize(Math.max(1, this.mMaxThumbnailCacheSize / 2));
                this.mApplicationIconCache.trimToSize(Math.max(1, this.mMaxIconCacheSize / 2));
                return;
            case 10:
            case EVENT.CHECK_PREINSTALLED_APPLETS /*60*/:
                this.mThumbnailCache.trimToSize(Math.max(1, this.mMaxThumbnailCacheSize / 4));
                this.mApplicationIconCache.trimToSize(Math.max(1, this.mMaxIconCacheSize / 4));
                return;
            case 15:
            case 80:
                this.mThumbnailCache.evictAll();
                this.mApplicationIconCache.evictAll();
                this.mActivityLabelCache.evictAll();
                this.mContentDescriptionCache.evictAll();
                return;
            case EVENT.DIALOG_CONFIRMED /*20*/:
                stopLoader();
                if (config.svelteLevel == 0) {
                    this.mThumbnailCache.trimToSize(Math.max(this.mNumVisibleTasksLoaded, this.mMaxThumbnailCacheSize / 2));
                } else if (config.svelteLevel == 1) {
                    this.mThumbnailCache.trimToSize(this.mNumVisibleThumbnailsLoaded);
                } else if (config.svelteLevel >= 2) {
                    this.mThumbnailCache.evictAll();
                }
                this.mApplicationIconCache.trimToSize(Math.max(this.mNumVisibleTasksLoaded, this.mMaxIconCacheSize / 2));
                return;
            default:
                return;
        }
    }
}
