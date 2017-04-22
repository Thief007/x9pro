package com.mediatek.settings.ext;

import android.content.Context;
import android.preference.ListPreference;

public interface ISmsPreferenceExt {
    boolean canSetSummary();

    void createBroadcastReceiver(Context context, ListPreference listPreference);

    void deregisterBroadcastReceiver(Context context);

    boolean getBroadcastIntent(Context context, String str);
}
