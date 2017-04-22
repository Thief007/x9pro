package com.mediatek.settings.ext;

import com.mediatek.settings.ext.IRcseOnlyApnExt.OnRcseOnlyApnStateChangedListener;

public class DefaultRcseOnlyApnExt implements IRcseOnlyApnExt {
    public boolean isRcseOnlyApnEnabled(String type) {
        return true;
    }

    public void onCreate(OnRcseOnlyApnStateChangedListener listener, int subId) {
    }

    public void onDestory() {
    }
}
