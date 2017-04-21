package com.android.systemui.recents.misc;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import java.util.HashMap;
import java.util.Map;

public class Console {
    public static boolean Enabled = false;
    public static final Map<Object, Long> mTimeLogs = new HashMap();

    public static void logError(Context context, String msg) {
        Toast.makeText(context, msg, 0).show();
        Log.e("Recents", msg);
    }

    public static void logRawError(String msg, Exception e) {
        Log.e("Recents", msg, e);
    }
}
