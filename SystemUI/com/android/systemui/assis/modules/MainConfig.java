package com.android.systemui.assis.modules;

import android.content.SharedPreferences;
import com.android.systemui.assis.app.LOG;
import com.android.systemui.assis.core.IStatistics;

public class MainConfig implements IMainConfig {
    private static final String PREF_FILE_NAME = "service_config";
    private static final String TAG = "Preference";
    private SharedPreferences mPreferences = null;

    public void onCreate(IStatistics shell) {
        LOG.I(TAG, "onCreate");
        this.mPreferences = shell.getAppletContext().getSharedPreferences(PREF_FILE_NAME, 0);
    }

    public void onDestroy() {
    }

    public void setValue(String key, String value) {
        this.mPreferences.edit().putString(key, value).commit();
    }

    public void setValue(String key, int value) {
        this.mPreferences.edit().putInt(key, value).commit();
    }

    public void setValue(String key, long value) {
        this.mPreferences.edit().putLong(key, value).commit();
    }

    public void setValue(String key, boolean value) {
        this.mPreferences.edit().putBoolean(key, value).commit();
    }

    public String getStringValue(String key, String defaultValue) {
        return this.mPreferences.getString(key, defaultValue);
    }

    public int getIntValue(String key, int defaultValue) {
        return this.mPreferences.getInt(key, defaultValue);
    }

    public long getLongValue(String key, long defaultValue) {
        return this.mPreferences.getLong(key, defaultValue);
    }

    public boolean getBooleanValue(String key, boolean defaultValue) {
        return this.mPreferences.getBoolean(key, defaultValue);
    }
}
