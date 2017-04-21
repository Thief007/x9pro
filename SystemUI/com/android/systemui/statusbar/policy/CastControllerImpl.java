package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.display.DisplayManager;
import android.hardware.display.WifiDisplayStatus;
import android.media.MediaRouter;
import android.media.MediaRouter.RouteInfo;
import android.media.MediaRouter.SimpleCallback;
import android.media.projection.MediaProjectionInfo;
import android.media.projection.MediaProjectionManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.CastController.Callback;
import com.android.systemui.statusbar.policy.CastController.CastDevice;
import com.mediatek.systemui.statusbar.extcb.FeatureOptionUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class CastControllerImpl implements CastController {
    private boolean mCallbackRegistered;
    private final ArrayList<Callback> mCallbacks = new ArrayList();
    private final Context mContext;
    private boolean mDiscovering;
    private final Object mDiscoveringLock = new Object();
    private final DisplayManager mDisplayManager;
    private final SimpleCallback mMediaCallback = new SimpleCallback() {
        public void onRouteAdded(MediaRouter router, RouteInfo route) {
            Log.d("CastController", "onRouteAdded: " + CastControllerImpl.routeToString(route));
            CastControllerImpl.this.updateRemoteDisplays();
        }

        public void onRouteChanged(MediaRouter router, RouteInfo route) {
            Log.d("CastController", "onRouteChanged: " + CastControllerImpl.routeToString(route));
            CastControllerImpl.this.updateRemoteDisplays();
        }

        public void onRouteRemoved(MediaRouter router, RouteInfo route) {
            Log.d("CastController", "onRouteRemoved: " + CastControllerImpl.routeToString(route));
            CastControllerImpl.this.updateRemoteDisplays();
        }

        public void onRouteSelected(MediaRouter router, int type, RouteInfo route) {
            Log.d("CastController", "onRouteSelected(" + type + "): " + CastControllerImpl.routeToString(route));
            CastControllerImpl.this.updateRemoteDisplays();
        }

        public void onRouteUnselected(MediaRouter router, int type, RouteInfo route) {
            Log.d("CastController", "onRouteUnselected(" + type + "): " + CastControllerImpl.routeToString(route));
            CastControllerImpl.this.updateRemoteDisplays();
        }
    };
    private final MediaRouter mMediaRouter;
    private WifiP2pDevice mP2pDevice;
    private MediaProjectionInfo mProjection;
    private final MediaProjectionManager.Callback mProjectionCallback = new MediaProjectionManager.Callback() {
        public void onStart(MediaProjectionInfo info) {
            CastControllerImpl.this.setProjection(info, true);
        }

        public void onStop(MediaProjectionInfo info) {
            CastControllerImpl.this.setProjection(info, false);
        }
    };
    private final Object mProjectionLock = new Object();
    private final MediaProjectionManager mProjectionManager;
    private final ArrayMap<String, RouteInfo> mRoutes = new ArrayMap();
    private final BroadcastReceiver mWfdReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("CastController", "onReceive(" + action + ")");
            if ("android.net.wifi.p2p.THIS_DEVICE_CHANGED".equals(action)) {
                CastControllerImpl.this.mP2pDevice = (WifiP2pDevice) intent.getParcelableExtra("wifiP2pDevice");
                CastControllerImpl.this.fireOnWifiP2pDeviceChanged();
            } else if ("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED".equals(action)) {
                CastControllerImpl.this.fireOnWfdStatusChanged();
            }
        }
    };
    private final boolean mWfdSinkSupport;
    private final boolean mWfdSinkUibcSupport;

    public CastControllerImpl(Context context) {
        this.mContext = context;
        this.mMediaRouter = (MediaRouter) context.getSystemService("media_router");
        this.mProjectionManager = (MediaProjectionManager) context.getSystemService("media_projection");
        this.mProjection = this.mProjectionManager.getActiveProjectionInfo();
        this.mProjectionManager.addCallback(this.mProjectionCallback, new Handler());
        this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
        this.mWfdSinkSupport = SystemProperties.get("ro.mtk_wfd_sink_support").equals(FeatureOptionUtils.SUPPORT_YES);
        this.mWfdSinkUibcSupport = SystemProperties.get("ro.mtk_wfd_sink_uibc_support").equals(FeatureOptionUtils.SUPPORT_YES);
        Log.d("CastController", "new CastController()");
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("CastController state:");
        pw.print("  mDiscovering=");
        pw.println(this.mDiscovering);
        pw.print("  mCallbackRegistered=");
        pw.println(this.mCallbackRegistered);
        pw.print("  mCallbacks.size=");
        pw.println(this.mCallbacks.size());
        pw.print("  mRoutes.size=");
        pw.println(this.mRoutes.size());
        for (int i = 0; i < this.mRoutes.size(); i++) {
            RouteInfo route = (RouteInfo) this.mRoutes.valueAt(i);
            pw.print("    ");
            pw.println(routeToString(route));
        }
        pw.print("  mProjection=");
        pw.println(this.mProjection);
    }

    public void addCallback(Callback callback) {
        this.mCallbacks.add(callback);
        fireOnCastDevicesChanged(callback);
        synchronized (this.mDiscoveringLock) {
            handleDiscoveryChangeLocked();
        }
        Log.d("CastController", "addCallback");
        if (isWfdSinkSupported()) {
            fireOnWfdStatusChanged(callback);
            fireOnWifiP2pDeviceChanged(callback);
        }
    }

    public void removeCallback(Callback callback) {
        Log.d("CastController", "removeCallback");
        this.mCallbacks.remove(callback);
        synchronized (this.mDiscoveringLock) {
            handleDiscoveryChangeLocked();
        }
    }

    public void setDiscovering(boolean request) {
        synchronized (this.mDiscoveringLock) {
            if (this.mDiscovering == request) {
                return;
            }
            this.mDiscovering = request;
            Log.d("CastController", "setDiscovering: " + request);
            handleDiscoveryChangeLocked();
        }
    }

    private void handleDiscoveryChangeLocked() {
        if (this.mCallbackRegistered) {
            this.mMediaRouter.removeCallback(this.mMediaCallback);
            this.mCallbackRegistered = false;
        }
        if (this.mDiscovering) {
            this.mMediaRouter.addCallback(4, this.mMediaCallback, 1);
            this.mCallbackRegistered = true;
        } else if (this.mCallbacks.size() != 0) {
            this.mMediaRouter.addCallback(4, this.mMediaCallback, 8);
            this.mCallbackRegistered = true;
        }
    }

    public void setCurrentUserId(int currentUserId) {
        this.mMediaRouter.rebindAsUser(currentUserId);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Set<CastDevice> getCastDevices() {
        boolean z;
        ArraySet<CastDevice> devices = new ArraySet();
        String str = "CastController";
        StringBuilder append = new StringBuilder().append("getCastDevices: ");
        if (this.mProjection != null) {
            z = true;
        } else {
            z = false;
        }
        Log.d(str, append.append(z).toString());
        synchronized (this.mProjectionLock) {
            if (this.mProjection != null) {
                CastDevice device = new CastDevice();
                device.id = this.mProjection.getPackageName();
                device.name = getAppName(this.mProjection.getPackageName());
                device.description = this.mContext.getString(R.string.quick_settings_casting);
                device.state = 2;
                device.tag = this.mProjection;
                devices.add(device);
                return devices;
            }
        }
    }

    public void startCasting(CastDevice device) {
        if (device != null && device.tag != null) {
            RouteInfo route = device.tag;
            Log.d("CastController", "startCasting: " + routeToString(route));
            this.mMediaRouter.selectRoute(4, route);
        }
    }

    public void stopCasting(CastDevice device) {
        boolean isProjection = device.tag instanceof MediaProjectionInfo;
        Log.d("CastController", "stopCasting isProjection=" + isProjection);
        if (isProjection) {
            MediaProjectionInfo projection = device.tag;
            if (Objects.equals(this.mProjectionManager.getActiveProjectionInfo(), projection)) {
                this.mProjectionManager.stopActiveProjection();
                return;
            } else {
                Log.w("CastController", "Projection is no longer active: " + projection);
                return;
            }
        }
        this.mMediaRouter.getDefaultRoute().select();
    }

    public boolean isWfdSinkSupported() {
        return this.mWfdSinkSupport;
    }

    public boolean isNeedShowWfdSink() {
        boolean ret = false;
        if (isWfdSinkSupported()) {
            WifiDisplayStatus wifiDisplayStatus = this.mDisplayManager.getWifiDisplayStatus();
            ret = wifiDisplayStatus != null ? wifiDisplayStatus.getFeatureState() == 3 : false;
        }
        Log.d("CastController", "needAddWfdSink: " + ret);
        return ret;
    }

    public void updateWfdFloatMenu(boolean start) {
        Log.d("CastController", "updateWfdFloatMenu: " + start);
        if (isWfdSinkSupported() && this.mWfdSinkUibcSupport) {
            Intent intent = new Intent();
            intent.setClassName("com.mediatek.floatmenu", "com.mediatek.floatmenu.FloatMenuService");
            if (start) {
                this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
            } else {
                this.mContext.stopServiceAsUser(intent, UserHandle.CURRENT);
            }
        }
    }

    public WifiP2pDevice getWifiP2pDev() {
        return this.mP2pDevice;
    }

    public void setListening(boolean listening) {
        Log.d("CastController", "register listener: " + listening);
        if (isWfdSinkSupported()) {
            if (listening) {
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED");
                filter.addAction("android.net.wifi.p2p.THIS_DEVICE_CHANGED");
                this.mContext.registerReceiver(this.mWfdReceiver, filter);
            } else {
                this.mContext.unregisterReceiver(this.mWfdReceiver);
            }
        }
    }

    private void setProjection(MediaProjectionInfo projection, boolean started) {
        boolean changed = false;
        MediaProjectionInfo oldProjection = this.mProjection;
        synchronized (this.mProjectionLock) {
            boolean isCurrent = Objects.equals(projection, this.mProjection);
            if (started && !isCurrent) {
                this.mProjection = projection;
                changed = true;
            } else if (!started && isCurrent) {
                this.mProjection = null;
                changed = true;
            }
        }
        if (changed) {
            Log.d("CastController", "setProjection: " + oldProjection + " -> " + this.mProjection);
            fireOnCastDevicesChanged();
        }
    }

    private String getAppName(String packageName) {
        PackageManager pm = this.mContext.getPackageManager();
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            if (appInfo != null) {
                CharSequence label = appInfo.loadLabel(pm);
                if (!TextUtils.isEmpty(label)) {
                    return label.toString();
                }
            }
            Log.w("CastController", "No label found for package: " + packageName);
        } catch (NameNotFoundException e) {
            Log.w("CastController", "Error getting appName for package: " + packageName, e);
        }
        return packageName;
    }

    private void updateRemoteDisplays() {
        synchronized (this.mRoutes) {
            this.mRoutes.clear();
            int n = this.mMediaRouter.getRouteCount();
            Log.d("CastController", "getRouteCount: " + n);
            for (int i = 0; i < n; i++) {
                RouteInfo route = this.mMediaRouter.getRouteAt(i);
                if (route.isEnabled() && route.matchesTypes(4)) {
                    ensureTagExists(route);
                    this.mRoutes.put(route.getTag().toString(), route);
                }
            }
            RouteInfo selected = this.mMediaRouter.getSelectedRoute(4);
            if (!(selected == null || selected.isDefault())) {
                ensureTagExists(selected);
                this.mRoutes.put(selected.getTag().toString(), selected);
            }
        }
        fireOnCastDevicesChanged();
    }

    private void ensureTagExists(RouteInfo route) {
        if (route.getTag() == null) {
            route.setTag(UUID.randomUUID().toString());
        }
    }

    private void fireOnCastDevicesChanged() {
        for (Callback callback : this.mCallbacks) {
            fireOnCastDevicesChanged(callback);
        }
    }

    private void fireOnCastDevicesChanged(Callback callback) {
        callback.onCastDevicesChanged();
    }

    private void fireOnWfdStatusChanged() {
        for (Callback callback : this.mCallbacks) {
            fireOnWfdStatusChanged(callback);
        }
    }

    private void fireOnWfdStatusChanged(Callback callback) {
        callback.onWfdStatusChanged(this.mDisplayManager.getWifiDisplayStatus(), this.mDisplayManager.isSinkEnabled());
    }

    private void fireOnWifiP2pDeviceChanged() {
        for (Callback callback : this.mCallbacks) {
            fireOnWifiP2pDeviceChanged(callback);
        }
    }

    private void fireOnWifiP2pDeviceChanged(Callback callback) {
        callback.onWifiP2pDeviceChanged(this.mP2pDevice);
    }

    private static String routeToString(RouteInfo route) {
        if (route == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder().append(route.getName()).append('/').append(route.getDescription()).append('@').append(route.getDeviceAddress()).append(",status=").append(route.getStatus());
        if (route.isDefault()) {
            sb.append(",default");
        }
        if (route.isEnabled()) {
            sb.append(",enabled");
        }
        if (route.isConnecting()) {
            sb.append(",connecting");
        }
        if (route.isSelected()) {
            sb.append(",selected");
        }
        return sb.append(",id=").append(route.getTag()).toString();
    }
}
