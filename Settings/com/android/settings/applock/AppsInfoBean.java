package com.android.settings.applock;

import android.graphics.drawable.Drawable;

public class AppsInfoBean {
    private Drawable appIcon;
    private String appLabel = null;
    private String appUnlock;
    private String packageName;
    private String pkgActivityName = null;

    public String getAppLabel() {
        return this.appLabel;
    }

    public void setAppLabel(CharSequence appLabel) {
        this.appLabel = appLabel.toString();
    }

    public Drawable getAppIcon() {
        return this.appIcon;
    }

    public void setAppIcon(Drawable appIcon) {
        this.appIcon = appIcon;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getPkgActivityName() {
        return this.pkgActivityName;
    }

    public void setPkgActivityName(String pkgActivityName) {
        this.pkgActivityName = pkgActivityName;
    }

    public void setAppUnlock(String appUnlock) {
        this.appUnlock = appUnlock;
    }
}
