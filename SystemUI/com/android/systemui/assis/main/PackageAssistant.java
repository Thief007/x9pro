package com.android.systemui.assis.main;

import android.content.Context;
import android.content.Intent;
import android.text.format.Time;
import com.android.systemui.assis.app.MAIN.CONSTANT;
import com.android.systemui.assis.core.StartupConfig;
import java.util.List;

public class PackageAssistant {
    private static final long LOCK = 432000000;
    private static final String TAG = "PackageAssistant";

    public static void RecordLaunch(Context context, String packageName) {
    }

    public static void active(Context context) {
        tryActive(context);
    }

    private static void tryActive(Context context) {
        long exeRecord = StartupConfig.getServiceActivateTime(context);
        long lastUnlockPoint = StartupConfig.getLastUnlockPoint(context);
        long serviceLastExecTime = StartupConfig.getServiceLastExecTime(context);
        Time curTime = new Time();
        curTime.setToNow();
        long curTimeInMillis = curTime.toMillis(false);
        if (exeRecord == -1) {
            StartupConfig.setServiceActivateTime(context, 0);
        }
        if (lastUnlockPoint == -1) {
            StartupConfig.setLastUnlockPoint(context, curTimeInMillis);
        }
        if (serviceLastExecTime == -1) {
            StartupConfig.setServiceLastExecTime(context, curTimeInMillis);
        }
        exeRecord = StartupConfig.getServiceActivateTime(context);
        lastUnlockPoint = StartupConfig.getLastUnlockPoint(context);
        serviceLastExecTime = StartupConfig.getServiceLastExecTime(context);
        StartupConfig.setLastUnlockPoint(context, curTimeInMillis);
        if ((exeRecord < LOCK ? 1 : null) == null) {
            context.startService(new Intent(context, Main.class));
            return;
        }
        if ((lastUnlockPoint > curTimeInMillis ? 1 : null) == null) {
            if ((curTimeInMillis - lastUnlockPoint >= CONSTANT.ONE_DAY ? 1 : null) == null) {
                StartupConfig.setServiceActivateTime(context, (exeRecord + curTimeInMillis) - lastUnlockPoint);
            }
        }
    }

    public static void AddForbidden(Context context, String packageName) {
    }

    public static void RemoveForbidden(Context context, String packageName) {
    }

    public static void AddForbidden(Context context, List<String> packages) {
        if (packages != null) {
            for (String pkg : packages) {
                AddForbidden(context, pkg);
            }
        }
    }

    public static void RemoveForbidden(Context context, List<String> packages) {
        if (packages != null) {
            for (String pkg : packages) {
                RemoveForbidden(context, pkg);
            }
        }
    }
}
