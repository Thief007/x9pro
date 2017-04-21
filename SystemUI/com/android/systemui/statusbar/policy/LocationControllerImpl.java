package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.AppOpsManager.OpEntry;
import android.app.AppOpsManager.PackageOps;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Secure;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.LocationController.LocationSettingsChangeCallback;
import com.mediatek.systemui.statusbar.extcb.FeatureOptionUtils;
import java.util.ArrayList;
import java.util.List;

public class LocationControllerImpl extends BroadcastReceiver implements LocationController {
    private static final int[] mHighPowerRequestAppOpArray = new int[]{42};
    private AppOpsManager mAppOpsManager;
    private boolean mAreActiveLocationRequests;
    private Context mContext;
    private final H mHandler = new H();
    private ArrayList<LocationSettingsChangeCallback> mSettingsChangeCallbacks = new ArrayList();
    private StatusBarManager mStatusBarManager;

    private final class H extends Handler {
        private H() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    locationSettingsChanged();
                    return;
                default:
                    return;
            }
        }

        private void locationSettingsChanged() {
            boolean isEnabled = LocationControllerImpl.this.isLocationEnabled();
            for (LocationSettingsChangeCallback cb : LocationControllerImpl.this.mSettingsChangeCallbacks) {
                cb.onLocationSettingsChanged(isEnabled);
            }
        }
    }

    public LocationControllerImpl(Context context, Looper bgLooper) {
        this.mContext = context;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.location.HIGH_POWER_REQUEST_CHANGE");
        filter.addAction("android.location.MODE_CHANGED");
        context.registerReceiverAsUser(this, UserHandle.ALL, filter, null, new Handler(bgLooper));
        this.mAppOpsManager = (AppOpsManager) context.getSystemService("appops");
        this.mStatusBarManager = (StatusBarManager) context.getSystemService("statusbar");
        updateActiveLocationRequests();
        refreshViews();
    }

    public void addSettingsChangedCallback(LocationSettingsChangeCallback cb) {
        this.mSettingsChangeCallbacks.add(cb);
        this.mHandler.sendEmptyMessage(1);
    }

    public void removeSettingsChangedCallback(LocationSettingsChangeCallback cb) {
        this.mSettingsChangeCallbacks.remove(cb);
    }

    public boolean setLocationEnabled(boolean enabled) {
        int currentUserId = ActivityManager.getCurrentUser();
        if (isUserLocationRestricted(currentUserId)) {
            return false;
        }
        return Secure.putIntForUser(this.mContext.getContentResolver(), "location_mode", enabled ? 3 : 0, currentUserId);
    }

    public boolean isLocationEnabled() {
        if (Secure.getIntForUser(this.mContext.getContentResolver(), "location_mode", 0, ActivityManager.getCurrentUser()) != 0) {
            return true;
        }
        return false;
    }

    private boolean isUserLocationRestricted(int userId) {
        return ((UserManager) this.mContext.getSystemService(FeatureOptionUtils.BUILD_TYPE_USER)).hasUserRestriction("no_share_location", new UserHandle(userId));
    }

    private boolean areActiveHighPowerLocationRequests() {
        List<PackageOps> packages = this.mAppOpsManager.getPackagesForOps(mHighPowerRequestAppOpArray);
        if (packages != null) {
            int numPackages = packages.size();
            for (int packageInd = 0; packageInd < numPackages; packageInd++) {
                List<OpEntry> opEntries = ((PackageOps) packages.get(packageInd)).getOps();
                if (opEntries != null) {
                    int numOps = opEntries.size();
                    for (int opInd = 0; opInd < numOps; opInd++) {
                        OpEntry opEntry = (OpEntry) opEntries.get(opInd);
                        if (opEntry.getOp() == 42 && opEntry.isRunning()) {
                            return true;
                        }
                    }
                    continue;
                }
            }
        }
        return false;
    }

    private void refreshViews() {
        if (this.mAreActiveLocationRequests) {
            this.mStatusBarManager.setIcon("location", R.drawable.stat_sys_location, 0, this.mContext.getString(R.string.accessibility_location_active));
        } else {
            this.mStatusBarManager.removeIcon("location");
        }
    }

    private void updateActiveLocationRequests() {
        boolean hadActiveLocationRequests = this.mAreActiveLocationRequests;
        this.mAreActiveLocationRequests = areActiveHighPowerLocationRequests();
        if (this.mAreActiveLocationRequests != hadActiveLocationRequests) {
            refreshViews();
        }
    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("android.location.HIGH_POWER_REQUEST_CHANGE".equals(action)) {
            updateActiveLocationRequests();
        } else if ("android.location.MODE_CHANGED".equals(action)) {
            this.mHandler.sendEmptyMessage(1);
        }
    }
}
