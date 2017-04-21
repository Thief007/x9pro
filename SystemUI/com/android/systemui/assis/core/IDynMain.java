package com.android.systemui.assis.core;

import com.android.systemui.assis.task.ITask;

public interface IDynMain extends ITask {
    String getSubVersion();

    String getVersionCode();
}
