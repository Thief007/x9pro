package com.android.systemui.statusbar;

import android.content.Context;
import android.view.View;

public class NotificationBigMediaNarrowViewWrapper extends NotificationMediaViewWrapper {
    protected NotificationBigMediaNarrowViewWrapper(Context ctx, View view) {
        super(ctx, view);
    }

    public boolean needsRoundRectClipping() {
        return true;
    }
}
