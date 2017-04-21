package com.mediatek.systemui.statusbar.util;

public class BatteryHelper {
    private BatteryHelper() {
    }

    public static boolean isBatteryFull(int level) {
        return level >= 100;
    }

    public static boolean isWirelessCharging(int mPlugType) {
        return mPlugType == 4;
    }

    public static boolean isBatteryProtection(int status) {
        if (status == 3 || status == 4) {
            return true;
        }
        return false;
    }

    public static boolean isPlugForProtection(int status, int level) {
        boolean plugged = false;
        switch (status) {
            case 2:
            case 5:
                plugged = true;
                break;
        }
        if (!plugged || isBatteryFull(level) || isBatteryProtection(status)) {
            return false;
        }
        return true;
    }
}
