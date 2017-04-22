package com.android.settings.applications;

import android.content.pm.PackageManager;
import com.android.settings.applications.AppStateBaseBridge.Callback;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.notification.NotificationBackend.AppRow;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.AppFilter;
import java.util.ArrayList;

public class AppStateNotificationBridge extends AppStateBaseBridge {
    public static final AppFilter FILTER_APP_NOTIFICATION_BLOCKED = new C02411();
    public static final AppFilter FILTER_APP_NOTIFICATION_NO_PEEK = new C02444();
    public static final AppFilter FILTER_APP_NOTIFICATION_PRIORITY = new C02422();
    public static final AppFilter FILTER_APP_NOTIFICATION_SENSITIVE = new C02433();
    private final NotificationBackend mNotifBackend;
    private final PackageManager mPm;

    static class C02411 implements AppFilter {
        C02411() {
        }

        public void init() {
        }

        public boolean filterApp(AppEntry info) {
            return info.extraInfo != null ? ((AppRow) info.extraInfo).banned : false;
        }
    }

    static class C02422 implements AppFilter {
        C02422() {
        }

        public void init() {
        }

        public boolean filterApp(AppEntry info) {
            return info.extraInfo != null ? ((AppRow) info.extraInfo).priority : false;
        }
    }

    static class C02433 implements AppFilter {
        C02433() {
        }

        public void init() {
        }

        public boolean filterApp(AppEntry info) {
            return info.extraInfo != null ? ((AppRow) info.extraInfo).sensitive : false;
        }
    }

    static class C02444 implements AppFilter {
        C02444() {
        }

        public void init() {
        }

        public boolean filterApp(AppEntry info) {
            return (info.extraInfo == null || ((AppRow) info.extraInfo).peekable) ? false : true;
        }
    }

    public AppStateNotificationBridge(PackageManager pm, ApplicationsState appState, Callback callback, NotificationBackend notifBackend) {
        super(appState, callback);
        this.mPm = pm;
        this.mNotifBackend = notifBackend;
    }

    protected void loadAllExtraInfo() {
        ArrayList<AppEntry> apps = this.mAppSession.getAllApps();
        int N = apps.size();
        for (int i = 0; i < N; i++) {
            AppEntry app = (AppEntry) apps.get(i);
            app.extraInfo = this.mNotifBackend.loadAppRow(this.mPm, app.info);
        }
    }

    protected void updateExtraInfo(AppEntry app, String pkg, int uid) {
        app.extraInfo = this.mNotifBackend.loadAppRow(this.mPm, app.info);
    }
}
