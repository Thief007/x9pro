package com.android.systemui.recents.views;

import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.model.Task;

public class TaskStackViewFilterAlgorithm {
    RecentsConfiguration mConfig;
    TaskStackView mStackView;
    ViewPool<TaskView, Task> mViewPool;

    public TaskStackViewFilterAlgorithm(RecentsConfiguration config, TaskStackView stackView, ViewPool<TaskView, Task> viewPool) {
        this.mConfig = config;
        this.mStackView = stackView;
        this.mViewPool = viewPool;
    }
}
