package com.mediatek.settings.ext;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public interface IAppListExt {
    View addLayoutAppView(View view, TextView textView, TextView textView2, int i, Drawable drawable, ViewGroup viewGroup);

    void setAppListItem(String str, int i);
}
