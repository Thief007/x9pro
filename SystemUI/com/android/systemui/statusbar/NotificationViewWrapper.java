package com.android.systemui.statusbar;

import android.content.Context;
import android.view.View;

public abstract class NotificationViewWrapper {
    protected final View mView;

    public abstract void setDark(boolean z, boolean z2, long j);

    public static NotificationViewWrapper wrap(Context ctx, View v) {
        if (v.getId() != 16909162) {
            return new NotificationCustomViewWrapper(v);
        }
        if ("bigMediaNarrow".equals(v.getTag())) {
            return new NotificationBigMediaNarrowViewWrapper(ctx, v);
        }
        if ("media".equals(v.getTag())) {
            return new NotificationMediaViewWrapper(ctx, v);
        }
        if ("bigPicture".equals(v.getTag())) {
            return new NotificationBigMediaNarrowViewWrapper(ctx, v);
        }
        return new NotificationTemplateViewWrapper(ctx, v);
    }

    protected NotificationViewWrapper(View view) {
        this.mView = view;
    }

    public void notifyContentUpdated() {
    }

    public boolean needsRoundRectClipping() {
        return false;
    }
}
