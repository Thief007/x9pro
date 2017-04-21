package com.mediatek.systemui.statusbar.policy;

import com.mediatek.hotknot.HotKnotAdapter;

public interface HotKnotController {
    HotKnotAdapter getAdapter();

    boolean isHotKnotOn();
}
