package com.p003v.common;

public class AppInfo {
    private Object mAppIcon;
    private String mAppName;
    private String mMainActivityName;
    private String mPackageName;

    public String getMainActivityName() {
        return this.mMainActivityName;
    }

    public void setMainActivityName(String mainActivityName) {
        this.mMainActivityName = mainActivityName;
    }

    public Object getAppIcon() {
        return this.mAppIcon;
    }

    public void setAppIcon(Object appIcon) {
        this.mAppIcon = appIcon;
    }

    public String getAppName() {
        return this.mAppName;
    }

    public void setAppName(String appName) {
        this.mAppName = appName;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public void setPackageName(String packageName) {
        this.mPackageName = packageName;
    }
}
