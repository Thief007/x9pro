package com.android.systemui.statusbar.phone;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemProperties;
import android.util.Log;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTile.Host;
import com.android.systemui.qs.QSTile.Host.Callback;
import com.android.systemui.qs.tiles.AirplaneModeTile;
import com.android.systemui.qs.tiles.BluetoothTile;
import com.android.systemui.qs.tiles.CastTile;
import com.android.systemui.qs.tiles.CellularTile;
import com.android.systemui.qs.tiles.ColorInversionTile;
import com.android.systemui.qs.tiles.DndTile;
import com.android.systemui.qs.tiles.FlashlightTile;
import com.android.systemui.qs.tiles.HotspotTile;
import com.android.systemui.qs.tiles.IntentTile;
import com.android.systemui.qs.tiles.LocationTile;
import com.android.systemui.qs.tiles.RotationLockTile;
import com.android.systemui.qs.tiles.WifiTile;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.CastController;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.RotationLockController;
import com.android.systemui.statusbar.policy.SecurityController;
import com.android.systemui.statusbar.policy.UserSwitcherController;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerService.Tunable;
import com.mediatek.systemui.ext.IQuickSettingsPlugin;
import com.mediatek.systemui.qs.tiles.AudioProfileTile;
import com.mediatek.systemui.qs.tiles.HotKnotTile;
import com.mediatek.systemui.qs.tiles.ScreenRecordTile;
import com.mediatek.systemui.qs.tiles.SuperScreenShotTile;
import com.mediatek.systemui.qs.tiles.ext.ApnSettingsTile;
import com.mediatek.systemui.qs.tiles.ext.DualSimSettingsTile;
import com.mediatek.systemui.qs.tiles.ext.MobileDataTile;
import com.mediatek.systemui.qs.tiles.ext.SimDataConnectionTile;
import com.mediatek.systemui.statusbar.extcb.FeatureOptionUtils;
import com.mediatek.systemui.statusbar.extcb.PluginFactory;
import com.mediatek.systemui.statusbar.policy.AudioProfileController;
import com.mediatek.systemui.statusbar.policy.DataConnectionController;
import com.mediatek.systemui.statusbar.policy.HotKnotController;
import com.mediatek.systemui.statusbar.policy.ScreenRecordController;
import com.mediatek.systemui.statusbar.policy.SuperScreenShotController;
import com.mediatek.systemui.statusbar.util.SIMHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

public class QSTileHost implements Host, Tunable {
    private static final boolean DEBUG = Log.isLoggable("QSTileHost", 3);
    private final AudioProfileController mAudioProfile;
    private final BluetoothController mBluetooth;
    private Callback mCallback;
    private final CastController mCast;
    private final Context mContext;
    private final DataConnectionController mDataConnection;
    private final FlashlightController mFlashlight;
    private final HotKnotController mHotKnot;
    private final HotspotController mHotspot;
    private final KeyguardMonitor mKeyguard;
    private final LocationController mLocation;
    private final Looper mLooper;
    private final NetworkController mNetwork;
    private final RotationLockController mRotation;
    private final ScreenRecordController mScreenRecord;
    private final SecurityController mSecurity;
    private final PhoneStatusBar mStatusBar;
    private final SuperScreenShotController mSuperScreenShot;
    protected final ArrayList<String> mTileSpecs = new ArrayList();
    private final LinkedHashMap<String, QSTile<?>> mTiles = new LinkedHashMap();
    private final UserSwitcherController mUserSwitcherController;
    private final ZenModeController mZen;

    public QSTileHost(Context context, PhoneStatusBar statusBar, BluetoothController bluetooth, LocationController location, RotationLockController rotation, NetworkController network, ZenModeController zen, HotspotController hotspot, CastController cast, FlashlightController flashlight, UserSwitcherController userSwitcher, KeyguardMonitor keyguard, SecurityController security, HotKnotController hotknot, DataConnectionController dataconnection, SuperScreenShotController superscreenshot, ScreenRecordController screenrecord, AudioProfileController audioprofile) {
        this.mContext = context;
        this.mStatusBar = statusBar;
        this.mBluetooth = bluetooth;
        this.mLocation = location;
        this.mRotation = rotation;
        this.mNetwork = network;
        this.mZen = zen;
        this.mHotspot = hotspot;
        this.mCast = cast;
        this.mFlashlight = flashlight;
        this.mUserSwitcherController = userSwitcher;
        this.mKeyguard = keyguard;
        this.mSecurity = security;
        this.mHotKnot = hotknot;
        this.mAudioProfile = audioprofile;
        this.mDataConnection = dataconnection;
        this.mSuperScreenShot = superscreenshot;
        this.mScreenRecord = screenrecord;
        HandlerThread ht = new HandlerThread(QSTileHost.class.getSimpleName(), 10);
        ht.start();
        this.mLooper = ht.getLooper();
        TunerService.get(this.mContext).addTunable((Tunable) this, "sysui_qs_tiles");
    }

    public void destroy() {
        TunerService.get(this.mContext).removeTunable(this);
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    public Collection<QSTile<?>> getTiles() {
        return this.mTiles.values();
    }

    public void startActivityDismissingKeyguard(Intent intent) {
        this.mStatusBar.postStartActivityDismissingKeyguard(intent, 0);
    }

    public void startActivityDismissingKeyguard(PendingIntent intent) {
        this.mStatusBar.postStartActivityDismissingKeyguard(intent);
    }

    public void warn(String message, Throwable t) {
    }

    public void collapsePanels() {
        this.mStatusBar.postAnimateCollapsePanels();
    }

    public Looper getLooper() {
        return this.mLooper;
    }

    public Context getContext() {
        return this.mContext;
    }

    public BluetoothController getBluetoothController() {
        return this.mBluetooth;
    }

    public LocationController getLocationController() {
        return this.mLocation;
    }

    public RotationLockController getRotationLockController() {
        return this.mRotation;
    }

    public NetworkController getNetworkController() {
        return this.mNetwork;
    }

    public ZenModeController getZenModeController() {
        return this.mZen;
    }

    public HotspotController getHotspotController() {
        return this.mHotspot;
    }

    public CastController getCastController() {
        return this.mCast;
    }

    public FlashlightController getFlashlightController() {
        return this.mFlashlight;
    }

    public KeyguardMonitor getKeyguardMonitor() {
        return this.mKeyguard;
    }

    public UserSwitcherController getUserSwitcherController() {
        return this.mUserSwitcherController;
    }

    public SecurityController getSecurityController() {
        return this.mSecurity;
    }

    public HotKnotController getHotKnotController() {
        return this.mHotKnot;
    }

    public void onTuningChanged(String key, String newValue) {
        if ("sysui_qs_tiles".equals(key)) {
            if (DEBUG) {
                Log.d("QSTileHost", "Recreating tiles");
            }
            List<String> tileSpecs = loadTileSpecs(newValue);
            if (!tileSpecs.equals(this.mTileSpecs)) {
                for (Entry<String, QSTile<?>> tile : this.mTiles.entrySet()) {
                    if (!tileSpecs.contains(tile.getKey())) {
                        if (DEBUG) {
                            Log.d("QSTileHost", "Destroying tile: " + ((String) tile.getKey()));
                        }
                        ((QSTile) tile.getValue()).destroy();
                    }
                }
                LinkedHashMap<String, QSTile<?>> newTiles = new LinkedHashMap();
                for (String tileSpec : tileSpecs) {
                    if (this.mTiles.containsKey(tileSpec)) {
                        newTiles.put(tileSpec, (QSTile) this.mTiles.get(tileSpec));
                    } else {
                        if (DEBUG) {
                            Log.d("QSTileHost", "Creating tile: " + tileSpec);
                        }
                        try {
                            newTiles.put(tileSpec, createTile(tileSpec));
                        } catch (Throwable t) {
                            Log.w("QSTileHost", "Error creating tile for spec: " + tileSpec, t);
                        }
                    }
                }
                this.mTileSpecs.clear();
                this.mTileSpecs.addAll(tileSpecs);
                this.mTiles.clear();
                this.mTiles.putAll(newTiles);
                if (this.mCallback != null) {
                    this.mCallback.onTilesChanged();
                }
            }
        }
    }

    protected QSTile<?> createTile(String tileSpec) {
        IQuickSettingsPlugin quickSettingsPlugin = PluginFactory.getQuickSettingsPlugin(this.mContext);
        if (tileSpec.equals("wifi")) {
            return new WifiTile(this);
        }
        if (tileSpec.equals("bt")) {
            return new BluetoothTile(this);
        }
        if (tileSpec.equals("inversion")) {
            return new ColorInversionTile(this);
        }
        if (tileSpec.equals("cell")) {
            return new CellularTile(this);
        }
        if (tileSpec.equals("airplane")) {
            return new AirplaneModeTile(this);
        }
        if (tileSpec.equals("dnd")) {
            return new DndTile(this);
        }
        if (tileSpec.equals("rotation")) {
            return new RotationLockTile(this);
        }
        if (tileSpec.equals("flashlight")) {
            return new FlashlightTile(this);
        }
        if (tileSpec.equals("location")) {
            return new LocationTile(this);
        }
        if (tileSpec.equals("cast")) {
            return new CastTile(this);
        }
        if (tileSpec.equals("hotspot")) {
            return new HotspotTile(this);
        }
        if (tileSpec.equals("superscreenshot")) {
            return new SuperScreenShotTile(this);
        }
        if (tileSpec.equals("screenrecord")) {
            return new ScreenRecordTile(this);
        }
        if (tileSpec.equals("hotknot") && SIMHelper.isMtkHotKnotSupport()) {
            return new HotKnotTile(this);
        }
        if (tileSpec.equals("audioprofile") && SIMHelper.isMtkAudioProfilesSupport()) {
            return new AudioProfileTile(this);
        }
        if (tileSpec.equals("dataconnection") && !SIMHelper.isWifiOnlyDevice()) {
            return new MobileDataTile(this);
        }
        if (tileSpec.equals("simdataconnection") && !SIMHelper.isWifiOnlyDevice() && quickSettingsPlugin.customizeAddQSTile(new SimDataConnectionTile(this)) != null) {
            return (SimDataConnectionTile) quickSettingsPlugin.customizeAddQSTile(new SimDataConnectionTile(this));
        }
        if (tileSpec.equals("dulsimsettings") && !SIMHelper.isWifiOnlyDevice() && quickSettingsPlugin.customizeAddQSTile(new DualSimSettingsTile(this)) != null) {
            return (DualSimSettingsTile) quickSettingsPlugin.customizeAddQSTile(new DualSimSettingsTile(this));
        }
        if (tileSpec.equals("apnsettings") && !SIMHelper.isWifiOnlyDevice() && quickSettingsPlugin.customizeAddQSTile(new ApnSettingsTile(this)) != null) {
            return (ApnSettingsTile) quickSettingsPlugin.customizeAddQSTile(new ApnSettingsTile(this));
        }
        if (tileSpec.startsWith("intent(")) {
            return IntentTile.create(this, tileSpec);
        }
        throw new IllegalArgumentException("Bad tile spec: " + tileSpec);
    }

    protected List<String> loadTileSpecs(String tileList) {
        String defaultTileList;
        Resources res = this.mContext.getResources();
        if (SystemProperties.get("ro.mtk_wfd_support").equals(FeatureOptionUtils.SUPPORT_YES)) {
            defaultTileList = res.getString(R.string.quick_settings_tiles_default);
        } else {
            defaultTileList = res.getString(R.string.quick_settings_tiles_default_swm);
        }
        defaultTileList = PluginFactory.getQuickSettingsPlugin(this.mContext).customizeQuickSettingsTileOrder(defaultTileList + "," + res.getString(R.string.quick_settings_tiles_extra));
        Log.d("QSTileHost", "loadTileSpecs() default tile list: " + defaultTileList);
        if (tileList == null) {
            tileList = res.getString(R.string.quick_settings_tiles);
            if (DEBUG) {
                Log.d("QSTileHost", "Loaded tile specs from config: " + tileList);
            }
        } else if (DEBUG) {
            Log.d("QSTileHost", "Loaded tile specs from setting: " + tileList);
        }
        ArrayList<String> tiles = new ArrayList();
        boolean addedDefault = false;
        for (String tile : tileList.split(",")) {
            String tile2 = tile2.trim();
            if (!tile2.isEmpty()) {
                if (!tile2.equals("default")) {
                    tiles.add(tile2);
                } else if (!addedDefault) {
                    tiles.addAll(Arrays.asList(defaultTileList.split(",")));
                    addedDefault = true;
                }
            }
        }
        return tiles;
    }
}
