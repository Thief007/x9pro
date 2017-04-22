package com.mediatek.settings.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class DefaultAppListExt extends ContextWrapper implements IAppListExt {
    private static final String TAG = "DefaultAppListExt";

    public DefaultAppListExt(Context context) {
        super(context);
        Log.i(TAG, "constructor\n");
    }

    public View addLayoutAppView(View view, TextView textView, TextView defaultLabel, int position, Drawable image, ViewGroup parent) {
        return view;
    }

    public void setAppListItem(String packageName, int position) {
    }
}
