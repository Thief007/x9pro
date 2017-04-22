package com.mediatek.settings.ext;

public interface IRcseOnlyApnExt {

    public interface OnRcseOnlyApnStateChangedListener {
        void OnRcseOnlyApnStateChanged();
    }

    boolean isRcseOnlyApnEnabled(String str);

    void onCreate(OnRcseOnlyApnStateChangedListener onRcseOnlyApnStateChangedListener, int i);

    void onDestory();
}
