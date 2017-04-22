package com.android.settings.applock;

import android.content.Context;
import android.content.SharedPreferences.Editor;

public class Preferences {
    private static boolean bEnableEM = false;

    public static String getPassword(Context context) {
        return context.getSharedPreferences("Preferences", 0).getString("password", "12345");
    }

    public static void setPassword(Context context, String password) {
        Editor editor = context.getSharedPreferences("Preferences", 0).edit();
        editor.putString("password", password);
        editor.commit();
    }
}
