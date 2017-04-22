package com.android.settings.applications;

import com.android.settings.applications.AppStateBaseBridge.Callback;
import com.android.settings.fuelgauge.PowerWhitelistBackend;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.AppFilter;
import com.android.settingslib.applications.ApplicationsState.CompoundFilter;
import java.util.ArrayList;

public class AppStatePowerBridge extends AppStateBaseBridge {
    public static final AppFilter FILTER_POWER_WHITELISTED = new CompoundFilter(ApplicationsState.FILTER_PERSONAL_WITHOUT_DISABLED_UNTIL_USED, new C02461());
    private final PowerWhitelistBackend mBackend = PowerWhitelistBackend.getInstance();

    static class C02461 implements AppFilter {
        C02461() {
        }

        public void init() {
        }

        public boolean filterApp(AppEntry info) {
            return info.extraInfo == Boolean.TRUE;
        }
    }

    public static class HighPowerState {
    }

    public AppStatePowerBridge(ApplicationsState appState, Callback callback) {
        super(appState, callback);
    }

    protected void loadAllExtraInfo() {
        ArrayList<AppEntry> apps = this.mAppSession.getAllApps();
        int N = apps.size();
        for (int i = 0; i < N; i++) {
            AppEntry app = (AppEntry) apps.get(i);
            app.extraInfo = this.mBackend.isWhitelisted(app.info.packageName) ? Boolean.TRUE : Boolean.FALSE;
        }
    }

    protected void updateExtraInfo(AppEntry app, String pkg, int uid) {
        app.extraInfo = this.mBackend.isWhitelisted(pkg) ? Boolean.TRUE : Boolean.FALSE;
    }
}
