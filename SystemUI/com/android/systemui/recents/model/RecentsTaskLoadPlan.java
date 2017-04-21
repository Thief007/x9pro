package com.android.systemui.recents.model;

import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManager.StackInfo;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.util.Log;
import android.util.SparseArray;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.Task.ComponentNameKey;
import com.android.systemui.recents.model.Task.TaskKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class RecentsTaskLoadPlan {
    static boolean DEBUG = false;
    static String TAG = "RecentsTaskLoadPlan";
    HashMap<ComponentNameKey, ActivityInfoHandle> mActivityInfoCache = new HashMap();
    RecentsConfiguration mConfig;
    Context mContext;
    List<RecentTaskInfo> mRawTasks;
    SparseArray<TaskStack> mStacks = new SparseArray();
    SystemServicesProxy mSystemServicesProxy;

    public static class Options {
        public boolean loadIcons = true;
        public boolean loadThumbnails = true;
        public int numVisibleTaskThumbnails = 0;
        public int numVisibleTasks = 0;
        public boolean onlyLoadForCache = false;
        public boolean onlyLoadPausedActivities = false;
        public int runningTaskId = -1;
    }

    RecentsTaskLoadPlan(Context context, RecentsConfiguration config, SystemServicesProxy ssp) {
        this.mContext = context;
        this.mConfig = config;
        this.mSystemServicesProxy = ssp;
    }

    public synchronized void preloadRawTasks(boolean isTopTaskHome) {
        this.mRawTasks = this.mSystemServicesProxy.getRecentTasks(this.mConfig.maxNumTasksToLoad, UserHandle.CURRENT.getIdentifier(), isTopTaskHome);
        Collections.reverse(this.mRawTasks);
        if (DEBUG) {
            Log.d(TAG, "preloadRawTasks, tasks: " + this.mRawTasks.size());
        }
    }

    synchronized void preloadPlan(RecentsTaskLoader loader, boolean isTopTaskHome) {
        if (DEBUG) {
            Log.d(TAG, "preloadPlan");
        }
        this.mActivityInfoCache.clear();
        Rect displayBounds = this.mSystemServicesProxy.getWindowRect();
        Resources res = this.mContext.getResources();
        SparseArray<ArrayList<Task>> stacksTasks = new SparseArray();
        if (this.mRawTasks == null) {
            preloadRawTasks(isTopTaskHome);
        }
        int taskCount = this.mRawTasks.size();
        int i = 0;
        while (i < taskCount) {
            ActivityInfoHandle infoHandle;
            Bitmap inMemoryIcon;
            String iconFilename;
            RecentTaskInfo t = (RecentTaskInfo) this.mRawTasks.get(i);
            TaskKey taskKey = new TaskKey(t.persistentId, t.stackId, t.baseIntent, t.userId, t.firstActiveTime, t.lastActiveTime);
            ComponentNameKey cnKey = taskKey.getComponentNameKey();
            boolean hadCachedActivityInfo = false;
            if (this.mActivityInfoCache.containsKey(cnKey)) {
                infoHandle = (ActivityInfoHandle) this.mActivityInfoCache.get(cnKey);
                hadCachedActivityInfo = true;
            } else {
                infoHandle = new ActivityInfoHandle();
            }
            String activityLabel = loader.getAndUpdateActivityLabel(taskKey, t.taskDescription, this.mSystemServicesProxy, infoHandle);
            String contentDescription = loader.getAndUpdateContentDescription(taskKey, activityLabel, this.mSystemServicesProxy, res);
            Drawable activityIcon = loader.getAndUpdateActivityIcon(taskKey, t.taskDescription, this.mSystemServicesProxy, res, infoHandle, false);
            int activityColor = loader.getActivityPrimaryColor(t.taskDescription, this.mConfig);
            if (!(hadCachedActivityInfo || infoHandle.info == null)) {
                this.mActivityInfoCache.put(cnKey, infoHandle);
            }
            if (t.taskDescription != null) {
                inMemoryIcon = t.taskDescription.getInMemoryIcon();
            } else {
                inMemoryIcon = null;
            }
            if (t.taskDescription != null) {
                iconFilename = t.taskDescription.getIconFilename();
            } else {
                iconFilename = null;
            }
            Task task = new Task(taskKey, t.id != RecentsTaskLoader.INVALID_TASK_ID, t.affiliatedTaskId, t.affiliatedTaskColor, activityLabel, contentDescription, activityIcon, activityColor, i == taskCount + -1, this.mConfig.lockToAppEnabled, inMemoryIcon, iconFilename);
            task.thumbnail = loader.getAndUpdateThumbnail(taskKey, this.mSystemServicesProxy, false);
            if (DEBUG) {
                Log.d(TAG, "\tthumbnail: " + taskKey + ", " + task.thumbnail);
            }
            ArrayList<Task> stackTasks = this.mConfig.multiStackEnabled ? (ArrayList) stacksTasks.get(0) : (ArrayList) stacksTasks.get(0);
            if (stackTasks == null) {
                stackTasks = new ArrayList();
                stacksTasks.put(0, stackTasks);
            }
            stackTasks.add(task);
            i++;
        }
        SparseArray<StackInfo> stackInfos = this.mSystemServicesProxy.getAllStackInfos();
        this.mStacks.clear();
        int stackCount = stacksTasks.size();
        for (i = 0; i < stackCount; i++) {
            int stackId = stacksTasks.keyAt(i);
            StackInfo info = (StackInfo) stackInfos.get(stackId);
            stackTasks = (ArrayList) stacksTasks.valueAt(i);
            TaskStack taskStack = new TaskStack(stackId);
            taskStack.setBounds(displayBounds, displayBounds);
            taskStack.setTasks(stackTasks);
            taskStack.createAffiliatedGroupings(this.mConfig);
            this.mStacks.put(stackId, taskStack);
        }
    }

    synchronized void executePlan(Options opts, RecentsTaskLoader loader, TaskResourceLoadQueue loadQueue) {
        if (DEBUG) {
            Log.d(TAG, "executePlan, # tasks: " + opts.numVisibleTasks + ", # thumbnails: " + opts.numVisibleTaskThumbnails + ", running task id: " + opts.runningTaskId);
        }
        Resources res = this.mContext.getResources();
        int stackCount = this.mStacks.size();
        for (int j = 0; j < stackCount; j++) {
            ArrayList<Task> tasks = ((TaskStack) this.mStacks.valueAt(j)).getTasks();
            int taskCount = tasks.size();
            int i = 0;
            while (i < taskCount) {
                ActivityInfoHandle infoHandle;
                RecentTaskInfo t = (RecentTaskInfo) this.mRawTasks.get(i);
                Task task = (Task) tasks.get(i);
                TaskKey taskKey = task.key;
                ComponentNameKey cnKey = taskKey.getComponentNameKey();
                boolean hadCachedActivityInfo = false;
                if (this.mActivityInfoCache.containsKey(cnKey)) {
                    infoHandle = (ActivityInfoHandle) this.mActivityInfoCache.get(cnKey);
                    hadCachedActivityInfo = true;
                } else {
                    infoHandle = new ActivityInfoHandle();
                }
                boolean isRunningTask = task.key.id == opts.runningTaskId;
                boolean isVisibleTask = i >= taskCount - opts.numVisibleTasks;
                boolean isVisibleThumbnail = i >= taskCount - opts.numVisibleTaskThumbnails;
                if (!opts.onlyLoadPausedActivities || !isRunningTask) {
                    if (opts.loadIcons && ((isRunningTask || isVisibleTask) && task.activityIcon == null)) {
                        if (DEBUG) {
                            Log.d(TAG, "\tLoading icon: " + taskKey);
                        }
                        task.activityIcon = loader.getAndUpdateActivityIcon(taskKey, t.taskDescription, this.mSystemServicesProxy, res, infoHandle, true);
                    }
                    if (opts.loadThumbnails && ((isRunningTask || isVisibleThumbnail) && (task.thumbnail == null || isRunningTask))) {
                        if (DEBUG) {
                            Log.d(TAG, "\tLoading thumbnail: " + taskKey);
                        }
                        if (this.mConfig.svelteLevel <= 1) {
                            task.thumbnail = loader.getAndUpdateThumbnail(taskKey, this.mSystemServicesProxy, true);
                        } else if (this.mConfig.svelteLevel == 2) {
                            loadQueue.addTask(task);
                        }
                    }
                    if (!(hadCachedActivityInfo || infoHandle.info == null)) {
                        this.mActivityInfoCache.put(cnKey, infoHandle);
                    }
                }
                i++;
            }
        }
    }

    public ArrayList<TaskStack> getAllTaskStacks() {
        ArrayList<TaskStack> stacks = new ArrayList();
        int stackCount = this.mStacks.size();
        for (int i = 0; i < stackCount; i++) {
            stacks.add((TaskStack) this.mStacks.valueAt(i));
        }
        if (stacks.isEmpty()) {
            stacks.add(new TaskStack());
        }
        return stacks;
    }

    public TaskStack getTaskStack(int stackId) {
        return (TaskStack) this.mStacks.get(stackId);
    }

    public boolean hasTasks() {
        int stackCount = this.mStacks.size();
        for (int i = 0; i < stackCount; i++) {
            if (((TaskStack) this.mStacks.valueAt(i)).getTaskCount() > 0) {
                return true;
            }
        }
        return false;
    }
}
