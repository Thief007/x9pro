package com.android.systemui.assis.modules;

import com.android.systemui.assis.core.IBase;

public interface IMainConfig extends IBase {
    boolean getBooleanValue(String str, boolean z);

    int getIntValue(String str, int i);

    long getLongValue(String str, long j);

    String getStringValue(String str, String str2);

    void setValue(String str, int i);

    void setValue(String str, long j);

    void setValue(String str, String str2);

    void setValue(String str, boolean z);
}
