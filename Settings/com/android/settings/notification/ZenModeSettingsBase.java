package com.android.settings.notification;

import android.app.NotificationManager;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings.Global;
import android.service.notification.ZenModeConfig;
import android.util.Log;
import com.android.settings.RestrictedSettingsFragment;
import java.util.Objects;

public abstract class ZenModeSettingsBase extends RestrictedSettingsFragment {
    protected static final boolean DEBUG = Log.isLoggable("ZenModeSettings", 3);
    protected ZenModeConfig mConfig;
    protected Context mContext;
    private final Handler mHandler = new Handler();
    private final SettingsObserver mSettingsObserver = new SettingsObserver();
    protected int mZenMode;

    private final class SettingsObserver extends ContentObserver {
        private final Uri ZEN_MODE_CONFIG_ETAG_URI;
        private final Uri ZEN_MODE_URI;

        private SettingsObserver() {
            super(ZenModeSettingsBase.this.mHandler);
            this.ZEN_MODE_URI = Global.getUriFor("zen_mode");
            this.ZEN_MODE_CONFIG_ETAG_URI = Global.getUriFor("zen_mode_config_etag");
        }

        public void register() {
            ZenModeSettingsBase.this.getContentResolver().registerContentObserver(this.ZEN_MODE_URI, false, this);
            ZenModeSettingsBase.this.getContentResolver().registerContentObserver(this.ZEN_MODE_CONFIG_ETAG_URI, false, this);
        }

        public void unregister() {
            ZenModeSettingsBase.this.getContentResolver().unregisterContentObserver(this);
        }

        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (this.ZEN_MODE_URI.equals(uri)) {
                ZenModeSettingsBase.this.updateZenMode(true);
            }
            if (this.ZEN_MODE_CONFIG_ETAG_URI.equals(uri)) {
                ZenModeSettingsBase.this.updateZenModeConfig(true);
            }
        }
    }

    protected abstract void onZenModeChanged();

    protected abstract void onZenModeConfigChanged();

    public ZenModeSettingsBase() {
        super("no_adjust_volume");
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mContext = getActivity();
        updateZenMode(false);
        updateZenModeConfig(false);
        if (DEBUG) {
            Log.d("ZenModeSettings", "Loaded mConfig=" + this.mConfig);
        }
    }

    public void onResume() {
        super.onResume();
        updateZenMode(true);
        updateZenModeConfig(true);
        this.mSettingsObserver.register();
        if (isUiRestricted()) {
            finish();
        }
    }

    public void onPause() {
        super.onPause();
        this.mSettingsObserver.unregister();
    }

    private void updateZenMode(boolean fireChanged) {
        int zenMode = Global.getInt(getContentResolver(), "zen_mode", this.mZenMode);
        if (zenMode != this.mZenMode) {
            this.mZenMode = zenMode;
            if (DEBUG) {
                Log.d("ZenModeSettings", "updateZenMode mZenMode=" + this.mZenMode);
            }
            if (fireChanged) {
                onZenModeChanged();
            }
        }
    }

    private void updateZenModeConfig(boolean fireChanged) {
        ZenModeConfig config = getZenModeConfig();
        if (!Objects.equals(config, this.mConfig)) {
            this.mConfig = config;
            if (DEBUG) {
                Log.d("ZenModeSettings", "updateZenModeConfig mConfig=" + this.mConfig);
            }
            if (fireChanged) {
                onZenModeConfigChanged();
            }
        }
    }

    protected boolean setZenModeConfig(ZenModeConfig config) {
        boolean success = NotificationManager.from(this.mContext).setZenModeConfig(config, getClass().getSimpleName());
        if (success) {
            this.mConfig = config;
            if (DEBUG) {
                Log.d("ZenModeSettings", "Saved mConfig=" + this.mConfig);
            }
            onZenModeConfigChanged();
        }
        return success;
    }

    protected static boolean isScheduleSupported(Context context) {
        return NotificationManager.from(context).isSystemConditionProviderEnabled("schedule");
    }

    private ZenModeConfig getZenModeConfig() {
        return NotificationManager.from(this.mContext).getZenModeConfig();
    }
}
