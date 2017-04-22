package com.android.settingslib;

import android.content.Context;
import android.provider.Settings.Global;

public class WirelessUtils {
    public static boolean isRadioAllowed(Context context, String type) {
        if (!isAirplaneModeOn(context)) {
            return true;
        }
        String toggleable = Global.getString(context.getContentResolver(), "airplane_mode_toggleable_radios");
        return toggleable != null ? toggleable.contains(type) : false;
    }

    public static boolean isAirplaneModeOn(Context context) {
        return Global.getInt(context.getContentResolver(), "airplane_mode_on", 0) != 0;
    }
}
