package com.android.systemui.recents.model;

import android.graphics.Rect;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task.TaskKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TaskStack {
    public final Rect displayBounds;
    public final int id;
    HashMap<Integer, TaskGrouping> mAffinitiesGroups;
    TaskStackCallbacks mCb;
    ArrayList<TaskGrouping> mGroups;
    FilteredTaskList mTaskList;
    public final Rect stackBounds;

    public interface TaskStackCallbacks {
        void onStackTaskAdded(TaskStack taskStack, Task task);

        void onStackTaskRemoved(TaskStack taskStack, Task task, Task task2);

        void onStackUnfiltered(TaskStack taskStack, ArrayList<Task> arrayList);
    }

    public TaskStack() {
        this(0);
    }

    public TaskStack(int stackId) {
        this.stackBounds = new Rect();
        this.displayBounds = new Rect();
        this.mTaskList = new FilteredTaskList();
        this.mGroups = new ArrayList();
        this.mAffinitiesGroups = new HashMap();
        this.id = stackId;
    }

    public void setCallbacks(TaskStackCallbacks cb) {
        this.mCb = cb;
    }

    public void setBounds(Rect stackBounds, Rect displayBounds) {
        this.stackBounds.set(stackBounds);
        this.displayBounds.set(displayBounds);
    }

    public void reset() {
        this.mCb = null;
        this.mTaskList.reset();
        this.mGroups.clear();
        this.mAffinitiesGroups.clear();
    }

    void removeTaskImpl(Task t) {
        this.mTaskList.remove(t);
        TaskGrouping group = t.group;
        group.removeTask(t);
        if (group.getTaskCount() == 0) {
            removeGroup(group);
        }
        t.lockToThisTask = false;
    }

    public void removeTask(Task t) {
        if (this.mTaskList.contains(t)) {
            removeTaskImpl(t);
            Task newFrontMostTask = getFrontMostTask();
            if (newFrontMostTask != null && newFrontMostTask.lockToTaskEnabled) {
                newFrontMostTask.lockToThisTask = true;
            }
            if (this.mCb != null) {
                this.mCb.onStackTaskRemoved(this, t, newFrontMostTask);
            }
        }
    }

    public void setTasks(List<Task> tasks) {
        ArrayList<Task> taskList = this.mTaskList.getTasks();
        for (int i = taskList.size() - 1; i >= 0; i--) {
            Task t = (Task) taskList.get(i);
            removeTaskImpl(t);
            if (this.mCb != null) {
                this.mCb.onStackTaskRemoved(this, t, null);
            }
        }
        this.mTaskList.set(tasks);
        for (Task t2 : tasks) {
            if (this.mCb != null) {
                this.mCb.onStackTaskAdded(this, t2);
            }
        }
    }

    public Task getFrontMostTask() {
        if (this.mTaskList.size() == 0) {
            return null;
        }
        return (Task) this.mTaskList.getTasks().get(this.mTaskList.size() - 1);
    }

    public ArrayList<TaskKey> getTaskKeys() {
        ArrayList<TaskKey> taskKeys = new ArrayList();
        ArrayList<Task> tasks = this.mTaskList.getTasks();
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            taskKeys.add(((Task) tasks.get(i)).key);
        }
        return taskKeys;
    }

    public ArrayList<Task> getTasks() {
        return this.mTaskList.getTasks();
    }

    public int getTaskCount() {
        return this.mTaskList.size();
    }

    public int indexOfTask(Task t) {
        return this.mTaskList.indexOf(t);
    }

    public Task findTaskWithId(int taskId) {
        ArrayList<Task> tasks = this.mTaskList.getTasks();
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            Task task = (Task) tasks.get(i);
            if (task.key.id == taskId) {
                return task;
            }
        }
        return null;
    }

    public void unfilterTasks() {
        ArrayList<Task> oldStack = new ArrayList(this.mTaskList.getTasks());
        this.mTaskList.removeFilter();
        if (this.mCb != null) {
            this.mCb.onStackUnfiltered(this, oldStack);
        }
    }

    public boolean hasFilteredTasks() {
        return this.mTaskList.hasFilter();
    }

    public void addGroup(TaskGrouping group) {
        this.mGroups.add(group);
        this.mAffinitiesGroups.put(Integer.valueOf(group.affiliation), group);
    }

    public void removeGroup(TaskGrouping group) {
        this.mGroups.remove(group);
        this.mAffinitiesGroups.remove(Integer.valueOf(group.affiliation));
    }

    public TaskGrouping getGroupWithAffiliation(int affiliation) {
        return (TaskGrouping) this.mAffinitiesGroups.get(Integer.valueOf(affiliation));
    }

    public void createAffiliatedGroupings(RecentsConfiguration config) {
        int i;
        HashMap<TaskKey, Task> tasksMap = new HashMap();
        ArrayList<Task> tasks = this.mTaskList.getTasks();
        int taskCount = tasks.size();
        for (i = 0; i < taskCount; i++) {
            int affiliation;
            TaskGrouping group;
            Task t = (Task) tasks.get(i);
            if (t.taskAffiliation > 0) {
                affiliation = t.taskAffiliation;
            } else {
                affiliation = 65536 + t.key.id;
            }
            if (this.mAffinitiesGroups.containsKey(Integer.valueOf(affiliation))) {
                group = getGroupWithAffiliation(affiliation);
            } else {
                group = new TaskGrouping(affiliation);
                addGroup(group);
            }
            group.addTask(t);
            tasksMap.put(t.key, t);
        }
        float minAlpha = config.taskBarViewAffiliationColorMinAlpha;
        int taskGroupCount = this.mGroups.size();
        for (i = 0; i < taskGroupCount; i++) {
            group = (TaskGrouping) this.mGroups.get(i);
            taskCount = group.getTaskCount();
            if (taskCount > 1) {
                int affiliationColor = ((Task) tasksMap.get(group.mTaskKeys.get(0))).taskAffiliationColor;
                float alphaStep = (1.0f - minAlpha) / ((float) taskCount);
                float alpha = 1.0f;
                for (int j = 0; j < taskCount; j++) {
                    ((Task) tasksMap.get(group.mTaskKeys.get(j))).colorPrimary = Utilities.getColorWithOverlay(affiliationColor, -1, alpha);
                    alpha -= alphaStep;
                }
            }
        }
    }

    public String toString() {
        String str = "Tasks:\n";
        for (Task t : this.mTaskList.getTasks()) {
            str = str + "  " + t.toString() + "\n";
        }
        return str;
    }
}
