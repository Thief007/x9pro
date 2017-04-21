package com.android.systemui.statusbar;

import android.view.View;
import com.android.systemui.ViewInvertHelper;

public class NotificationCustomViewWrapper extends NotificationViewWrapper {
    private final ViewInvertHelper mInvertHelper;

    protected NotificationCustomViewWrapper(View view) {
        super(view);
        this.mInvertHelper = new ViewInvertHelper(view, 700);
    }

    public void setDark(boolean dark, boolean fade, long delay) {
        if (fade) {
            this.mInvertHelper.fade(dark, delay);
        } else {
            this.mInvertHelper.update(dark);
        }
    }

    public boolean needsRoundRectClipping() {
        return true;
    }
}
