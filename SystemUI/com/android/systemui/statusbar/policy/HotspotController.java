package com.android.systemui.statusbar.policy;

public interface HotspotController {

    public interface Callback {
        void onHotspotChanged(boolean z);
    }

    void addCallback(Callback callback);

    boolean isHotspotEnabled();

    boolean isHotspotSupported();

    void removeCallback(Callback callback);

    void setHotspotEnabled(boolean z);
}
