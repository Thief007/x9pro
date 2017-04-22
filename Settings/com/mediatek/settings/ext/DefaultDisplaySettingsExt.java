package com.mediatek.settings.ext;

import android.content.Context;
import android.preference.PreferenceScreen;

public class DefaultDisplaySettingsExt implements IDisplaySettingsExt {
    private static final String TAG = "DefaultDisplaySettingsExt";
    private Context mContext;

    public DefaultDisplaySettingsExt(Context context) {
        this.mContext = context;
    }

    public void addPreference(Context context, PreferenceScreen screen) {
    }
}
