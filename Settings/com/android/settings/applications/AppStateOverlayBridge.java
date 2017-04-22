package com.android.settings.applications;

import android.content.Context;
import com.android.settings.applications.AppStateAppOpsBridge.PermissionState;
import com.android.settings.applications.AppStateBaseBridge.Callback;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.AppFilter;

public class AppStateOverlayBridge extends AppStateAppOpsBridge {
    public static final AppFilter FILTER_SYSTEM_ALERT_WINDOW = new C02451();
    private static final String[] PM_PERMISSION = new String[]{"android.permission.SYSTEM_ALERT_WINDOW"};

    static class C02451 implements AppFilter {
        C02451() {
        }

        public void init() {
        }

        public boolean filterApp(AppEntry info) {
            return info.extraInfo != null;
        }
    }

    public static class OverlayState extends PermissionState {
        public OverlayState(PermissionState permissionState) {
            super(permissionState.packageName, permissionState.userHandle);
            this.packageInfo = permissionState.packageInfo;
            this.appOpMode = permissionState.appOpMode;
            this.permissionDeclared = permissionState.permissionDeclared;
            this.staticPermissionGranted = permissionState.staticPermissionGranted;
        }
    }

    public AppStateOverlayBridge(Context context, ApplicationsState appState, Callback callback) {
        super(context, appState, callback, 24, PM_PERMISSION);
    }

    protected void updateExtraInfo(AppEntry app, String pkg, int uid) {
        app.extraInfo = getOverlayInfo(pkg, uid);
    }

    public OverlayState getOverlayInfo(String pkg, int uid) {
        return new OverlayState(super.getPermissionInfo(pkg, uid));
    }

    public int getNumberOfPackagesWithPermission() {
        return super.getNumPackagesDeclaredPermission();
    }

    public int getNumberOfPackagesCanDrawOverlay() {
        return super.getNumPackagesAllowedByAppOps();
    }
}
