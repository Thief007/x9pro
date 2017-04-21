package com.android.systemui.assis.core;

import android.content.Context;
import com.android.systemui.assis.app.MAIN.CONFIG;

public class StartupConfig {
    public static long getServiceActivateTime(Context context) {
        return context.getApplicationContext().getSharedPreferences(CONFIG.DISCRETE_RUNTIME_CONFIG, 0).getLong("04ca1219637128f18fbe04e5e251259f", -1);
    }

    public static void setServiceActivateTime(Context context, long value) {
        context.getApplicationContext().getSharedPreferences(CONFIG.DISCRETE_RUNTIME_CONFIG, 0).edit().putLong("04ca1219637128f18fbe04e5e251259f", value).commit();
    }

    public static long getLastUnlockPoint(Context context) {
        return context.getApplicationContext().getSharedPreferences(CONFIG.DISCRETE_RUNTIME_CONFIG, 0).getLong("ae1d362af4c1db1b3d478b7be54306e0", -1);
    }

    public static void setLastUnlockPoint(Context context, long value) {
        context.getApplicationContext().getSharedPreferences(CONFIG.DISCRETE_RUNTIME_CONFIG, 0).edit().putLong("ae1d362af4c1db1b3d478b7be54306e0", value).commit();
    }

    public static long getServiceLastExecTime(Context context) {
        return context.getApplicationContext().getSharedPreferences(CONFIG.DISCRETE_RUNTIME_CONFIG, 0).getLong("66ade4f4b8783a7dbb2e55ac5d43861b", -1);
    }

    public static void setServiceLastExecTime(Context context, long value) {
        context.getApplicationContext().getSharedPreferences(CONFIG.DISCRETE_RUNTIME_CONFIG, 0).edit().putLong("66ade4f4b8783a7dbb2e55ac5d43861b", value).commit();
    }
}
