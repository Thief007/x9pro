package com.mediatek.settings.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.preference.PreferenceScreen;
import android.util.Log;

public class DefaultStatusBarPlmnDisplayExt extends ContextWrapper implements IStatusBarPlmnDisplayExt {
    static final String TAG = "DefaultStatusBarPlmnDisplayExt";

    public DefaultStatusBarPlmnDisplayExt(Context context) {
        super(context);
        Log.d("@M_DefaultStatusBarPlmnDisplayExt", "Into DefaultStatusBarPlmnPlugin");
    }

    public void createCheckBox(PreferenceScreen pref, int j) {
        Log.d("@M_DefaultStatusBarPlmnDisplayExt", "Into Default createCheckBox");
    }
}
