package com.android.systemui.recents.model;

import android.app.ActivityManager.TaskDescription;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.Task.TaskKey;

/* compiled from: RecentsTaskLoader */
class TaskResourceLoader implements Runnable {
    static boolean DEBUG = false;
    static String TAG = "TaskResourceLoader";
    DrawableLruCache mApplicationIconCache;
    boolean mCancelled;
    Context mContext;
    BitmapDrawable mDefaultApplicationIcon;
    Bitmap mDefaultThumbnail;
    TaskResourceLoadQueue mLoadQueue;
    HandlerThread mLoadThread = new HandlerThread("Recents-TaskResourceLoader", 10);
    Handler mLoadThreadHandler;
    Handler mMainThreadHandler = new Handler();
    SystemServicesProxy mSystemServicesProxy;
    BitmapLruCache mThumbnailCache;
    boolean mWaitingOnLoadQueue;

    public TaskResourceLoader(TaskResourceLoadQueue loadQueue, DrawableLruCache applicationIconCache, BitmapLruCache thumbnailCache, Bitmap defaultThumbnail, BitmapDrawable defaultApplicationIcon) {
        this.mLoadQueue = loadQueue;
        this.mApplicationIconCache = applicationIconCache;
        this.mThumbnailCache = thumbnailCache;
        this.mDefaultThumbnail = defaultThumbnail;
        this.mDefaultApplicationIcon = defaultApplicationIcon;
        this.mLoadThread.start();
        this.mLoadThreadHandler = new Handler(this.mLoadThread.getLooper());
        this.mLoadThreadHandler.post(this);
    }

    void start(Context context) {
        this.mContext = context;
        this.mCancelled = false;
        this.mSystemServicesProxy = new SystemServicesProxy(context);
        synchronized (this.mLoadThread) {
            this.mLoadThread.notifyAll();
        }
    }

    void stop() {
        this.mCancelled = true;
        this.mSystemServicesProxy = null;
        if (this.mWaitingOnLoadQueue) {
            this.mContext = null;
        }
    }

    public void run() {
        while (true) {
            if (this.mCancelled) {
                this.mContext = null;
                synchronized (this.mLoadThread) {
                    try {
                        this.mLoadThread.wait();
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            } else {
                RecentsConfiguration config = RecentsConfiguration.getInstance();
                SystemServicesProxy ssp = this.mSystemServicesProxy;
                if (ssp != null) {
                    final Task t = this.mLoadQueue.nextTask();
                    if (t != null) {
                        Drawable cachedIcon = (Drawable) this.mApplicationIconCache.get(t.key);
                        Bitmap cachedThumbnail = (Bitmap) this.mThumbnailCache.get(t.key);
                        if (cachedIcon == null) {
                            cachedIcon = getTaskDescriptionIcon(t.key, t.icon, t.iconFilename, ssp, this.mContext.getResources());
                            if (cachedIcon == null) {
                                ActivityInfo info = ssp.getActivityInfo(t.key.baseIntent.getComponent(), t.key.userId);
                                if (info != null) {
                                    if (DEBUG) {
                                        Log.d(TAG, "Loading icon: " + t.key);
                                    }
                                    cachedIcon = ssp.getActivityIcon(info, t.key.userId);
                                }
                            }
                            if (cachedIcon == null) {
                                cachedIcon = this.mDefaultApplicationIcon;
                            }
                            this.mApplicationIconCache.put(t.key, cachedIcon);
                        }
                        if (cachedThumbnail == null) {
                            if (config.svelteLevel < 3) {
                                if (DEBUG) {
                                    Log.d(TAG, "Loading thumbnail: " + t.key);
                                }
                                cachedThumbnail = ssp.getTaskThumbnail(t.key.id);
                            }
                            if (cachedThumbnail == null) {
                                cachedThumbnail = this.mDefaultThumbnail;
                            }
                            if (config.svelteLevel < 1) {
                                this.mThumbnailCache.put(t.key, cachedThumbnail);
                            }
                        }
                        if (!this.mCancelled) {
                            final Drawable newIcon = cachedIcon;
                            final Bitmap bitmap = cachedThumbnail == this.mDefaultThumbnail ? null : cachedThumbnail;
                            this.mMainThreadHandler.post(new Runnable() {
                                public void run() {
                                    t.notifyTaskDataLoaded(bitmap, newIcon);
                                }
                            });
                        }
                    }
                }
                if (!this.mCancelled && this.mLoadQueue.isEmpty()) {
                    synchronized (this.mLoadQueue) {
                        try {
                            this.mWaitingOnLoadQueue = true;
                            this.mLoadQueue.wait();
                            this.mWaitingOnLoadQueue = false;
                        } catch (InterruptedException ie2) {
                            ie2.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    Drawable getTaskDescriptionIcon(TaskKey taskKey, Bitmap iconBitmap, String iconFilename, SystemServicesProxy ssp, Resources res) {
        Bitmap tdIcon;
        if (iconBitmap != null) {
            tdIcon = iconBitmap;
        } else {
            tdIcon = TaskDescription.loadTaskDescriptionIcon(iconFilename);
        }
        if (tdIcon != null) {
            return ssp.getBadgedIcon(new BitmapDrawable(res, tdIcon), taskKey.userId);
        }
        return null;
    }
}
