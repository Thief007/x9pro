package com.android.systemui.assis.task;

import android.os.Message;
import android.text.format.Time;
import com.android.systemui.assis.core.IBase;

public interface ITask extends IBase {
    boolean onMinTick(Time time);

    boolean processSystemMessage(Message message);
}
