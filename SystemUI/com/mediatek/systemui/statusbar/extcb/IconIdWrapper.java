package com.mediatek.systemui.statusbar.extcb;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;

public class IconIdWrapper implements Cloneable {
    private int mIconId;
    private Resources mResources;

    public IconIdWrapper() {
        this(null, 0);
    }

    public IconIdWrapper(int iconId) {
        this(null, iconId);
    }

    public IconIdWrapper(Resources resources, int iconId) {
        this.mResources = null;
        this.mIconId = 0;
        this.mResources = resources;
        this.mIconId = iconId;
    }

    public Resources getResources() {
        return this.mResources;
    }

    public void setResources(Resources resources) {
        this.mResources = resources;
    }

    public int getIconId() {
        return this.mIconId;
    }

    public void setIconId(int iconId) {
        this.mIconId = iconId;
    }

    public Drawable getDrawable() {
        if (this.mResources == null || this.mIconId == 0) {
            return null;
        }
        return this.mResources.getDrawable(this.mIconId);
    }

    public IconIdWrapper clone() {
        try {
            IconIdWrapper clone = (IconIdWrapper) super.clone();
            clone.mResources = this.mResources;
            clone.mIconId = this.mIconId;
            return clone;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public String toString() {
        if (getResources() == null) {
            return "IconIdWrapper [mResources == null, mIconId=" + this.mIconId + "]";
        }
        return "IconIdWrapper [mResources != null, mIconId=" + this.mIconId + "]";
    }

    public int hashCode() {
        return ((this.mIconId + 31) * 31) + (this.mResources == null ? 0 : this.mResources.hashCode());
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        IconIdWrapper other = (IconIdWrapper) obj;
        if (this.mIconId != other.mIconId) {
            return false;
        }
        if (this.mResources == null) {
            if (other.mResources != null) {
                return false;
            }
        } else if (!this.mResources.equals(other.mResources)) {
            return false;
        }
        return true;
    }

    public void copyFrom(IconIdWrapper icon) {
        this.mResources = icon.mResources;
        this.mIconId = icon.mIconId;
    }
}
