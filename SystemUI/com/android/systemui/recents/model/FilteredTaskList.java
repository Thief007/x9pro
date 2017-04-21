package com.android.systemui.recents.model;

import com.android.systemui.recents.model.Task.TaskKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/* compiled from: TaskStack */
class FilteredTaskList {
    TaskFilter mFilter;
    ArrayList<Task> mFilteredTasks = new ArrayList();
    HashMap<TaskKey, Integer> mTaskIndices = new HashMap();
    ArrayList<Task> mTasks = new ArrayList();

    FilteredTaskList() {
    }

    void reset() {
        this.mTasks.clear();
        this.mFilteredTasks.clear();
        this.mTaskIndices.clear();
        this.mFilter = null;
    }

    void removeFilter() {
        this.mFilter = null;
        updateFilteredTasks();
    }

    void set(List<Task> tasks) {
        this.mTasks.clear();
        this.mTasks.addAll(tasks);
        updateFilteredTasks();
    }

    boolean remove(Task t) {
        if (!this.mFilteredTasks.contains(t)) {
            return false;
        }
        boolean removed = this.mTasks.remove(t);
        updateFilteredTasks();
        return removed;
    }

    int indexOf(Task t) {
        if (this.mTaskIndices.containsKey(t.key)) {
            return ((Integer) this.mTaskIndices.get(t.key)).intValue();
        }
        return -1;
    }

    int size() {
        return this.mFilteredTasks.size();
    }

    boolean contains(Task t) {
        return this.mTaskIndices.containsKey(t.key);
    }

    private void updateFilteredTasks() {
        this.mFilteredTasks.clear();
        if (this.mFilter != null) {
            int taskCount = this.mTasks.size();
            for (int i = 0; i < taskCount; i++) {
                Task t = (Task) this.mTasks.get(i);
                if (this.mFilter.acceptTask(t, i)) {
                    this.mFilteredTasks.add(t);
                }
            }
        } else {
            this.mFilteredTasks.addAll(this.mTasks);
        }
        updateFilteredTaskIndices();
    }

    private void updateFilteredTaskIndices() {
        this.mTaskIndices.clear();
        int taskCount = this.mFilteredTasks.size();
        for (int i = 0; i < taskCount; i++) {
            this.mTaskIndices.put(((Task) this.mFilteredTasks.get(i)).key, Integer.valueOf(i));
        }
    }

    boolean hasFilter() {
        return this.mFilter != null;
    }

    ArrayList<Task> getTasks() {
        return this.mFilteredTasks;
    }
}
