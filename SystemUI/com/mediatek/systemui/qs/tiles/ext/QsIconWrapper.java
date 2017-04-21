package com.mediatek.systemui.qs.tiles.ext;

import android.content.Context;
import android.graphics.drawable.Drawable;
import com.android.systemui.qs.QSTile.Icon;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;

public class QsIconWrapper extends Icon {
    private final IconIdWrapper mIconWrapper;

    public QsIconWrapper(IconIdWrapper iconWrapper) {
        this.mIconWrapper = iconWrapper;
    }

    public Drawable getDrawable(Context context) {
        return this.mIconWrapper.getDrawable();
    }

    public int hashCode() {
        return this.mIconWrapper.hashCode();
    }

    public boolean equals(Object o) {
        return this.mIconWrapper.equals(o);
    }
}
