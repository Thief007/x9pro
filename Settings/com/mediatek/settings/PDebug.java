package com.mediatek.settings;

import android.os.SystemProperties;
import android.os.Trace;

public class PDebug {
    private static Boolean DEBUG;
    private static long TRACE_TAG;

    static {
        boolean z = true;
        DEBUG = Boolean.valueOf(true);
        TRACE_TAG = 0;
        if (SystemProperties.get("ap.performance.debug", "0").equals("0")) {
            z = false;
        }
        DEBUG = Boolean.valueOf(z);
        if (DEBUG.booleanValue()) {
            TRACE_TAG = 1 << ((int) Long.parseLong(SystemProperties.get("ap.performance.debug")));
        }
    }

    public static void Start(String msg) {
        if (DEBUG.booleanValue()) {
            Trace.traceCounter(TRACE_TAG, "P$" + msg, 1);
        }
    }

    public static void End(String msg) {
        if (DEBUG.booleanValue()) {
            Trace.traceCounter(TRACE_TAG, "P$" + msg, 0);
        }
    }
}
