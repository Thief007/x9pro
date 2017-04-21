package com.android.keyguard;

public interface SecurityMessageDisplay {
    void setMessage(int i, boolean z);

    void setMessage(int i, boolean z, Object... objArr);

    void setMessage(CharSequence charSequence, boolean z);

    void setNextMessageColor(int i);

    void setTimeout(int i);
}
