package com.android.systemui.assis.app;

import android.util.Log;

public class LOG {
    public static boolean DEBUG = false;

    public static void switchEngMode(boolean mode) {
        DEBUG = mode;
    }

    public static void E(String _tag, String _content) {
        if (DEBUG) {
            Log.e(_tag, _content);
        }
    }

    public static void I(String _tag, String _content) {
        if (DEBUG) {
            Log.i(_tag, _content);
        }
    }

    public static void V(String _tag, String _content) {
        if (DEBUG) {
            Log.v(_tag, _content);
        }
    }

    public static void D(String _tag, String _content) {
        if (DEBUG) {
            Log.d(_tag, _content);
        }
    }

    public static void W(String _tag, String _content) {
        if (DEBUG) {
            Log.w(_tag, _content);
        }
    }
}
