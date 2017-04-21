package com.android.systemui.statusbar;

import android.content.Context;
import android.view.View;

public class NotificationMediaViewWrapper extends NotificationTemplateViewWrapper {
    protected NotificationMediaViewWrapper(Context ctx, View view) {
        super(ctx, view);
    }

    public void setDark(boolean dark, boolean fade, long delay) {
        setPictureGrayscale(dark, fade, delay);
    }
}
