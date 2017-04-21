package com.android.systemui.assis.core;

public interface ITaskListener {
    void onExecuted(int i, Object obj);

    void onPreExecute(int i, Object obj);

    void onUpdate(int i, Object obj);
}
