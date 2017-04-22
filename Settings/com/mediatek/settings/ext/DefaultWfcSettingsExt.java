package com.mediatek.settings.ext;

import android.content.Context;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import com.android.ims.ImsManager;

public class DefaultWfcSettingsExt implements IWfcSettingsExt {
    public static final int CONFIG_CHANGE = 4;
    public static final int CREATE = 2;
    public static final int DESTROY = 3;
    public static final int PAUSE = 1;
    public static final int RESUME = 0;
    private static final String TAG = "DefaultWfcSettingsExt";

    public void initPlugin(PreferenceFragment pf) {
    }

    public String getWfcSummary(Context context, int defaultSummaryResId) {
        return context.getResources().getString(defaultSummaryResId);
    }

    public void onWirelessSettingsEvent(int event) {
    }

    public void onWfcSettingsEvent(int event) {
    }

    public void addOtherCustomPreference() {
    }

    public void updateWfcModePreference(PreferenceScreen root, ListPreference wfcModePref, boolean wfcEnabled, int wfcMode) {
        Log.d(TAG, "wfcEnabled:" + wfcEnabled + "wfcMode:" + wfcMode);
        if (wfcModePref != null) {
            wfcModePref.setSummary(getWfcModeSummary(root.getContext(), wfcMode));
            wfcModePref.setEnabled(wfcEnabled);
            if (wfcEnabled) {
                root.addPreference(wfcModePref);
            } else {
                root.removePreference(wfcModePref);
            }
        }
    }

    public boolean showWfcTetheringAlertDialog(Context context) {
        return false;
    }

    public void customizedWfcPreference(Context context, PreferenceScreen preferenceScreen) {
    }

    private int getWfcModeSummary(Context context, int wfcMode) {
        if (!ImsManager.isWfcEnabledByUser(context)) {
            return 17039561;
        }
        switch (wfcMode) {
            case 0:
                return 17039564;
            case 1:
                return 17039563;
            case 2:
                return 17039562;
            default:
                Log.e(TAG, "Unexpected WFC mode value: " + wfcMode);
                return 17039561;
        }
    }
}
