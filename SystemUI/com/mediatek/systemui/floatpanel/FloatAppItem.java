package com.mediatek.systemui.floatpanel;

import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import com.mediatek.systemui.floatpanel.FloatAppAdapter.IconResizer;

public class FloatAppItem {
    public String className;
    public int container;
    public Bundle extras;
    public Drawable icon;
    public CharSequence label;
    public String packageName;
    public int position;
    public ResolveInfo resolveInfo;
    public boolean visible = true;

    FloatAppItem(PackageManager pm, ResolveInfo info, IconResizer resizer, int pos) {
        this.resolveInfo = info;
        this.label = this.resolveInfo.loadLabel(pm);
        ComponentInfo ci = this.resolveInfo.activityInfo;
        if (ci == null) {
            ci = this.resolveInfo.serviceInfo;
        }
        if (this.label == null && ci != null) {
            this.label = this.resolveInfo.activityInfo.name;
        }
        if (resizer != null) {
            this.icon = resizer.createIconThumbnail(this.resolveInfo.loadIcon(pm));
        }
        this.packageName = ci.applicationInfo.packageName;
        this.className = ci.name;
        this.position = pos;
    }

    public String toString() {
        return "FloatAppItem{" + this.packageName + "/" + this.className + ":vis = " + this.visible + ", pos = " + this.position + "}";
    }
}
