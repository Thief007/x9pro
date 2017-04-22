package com.android.settings;

import android.content.Context;
import android.content.res.ColorStateList;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

public class TintablePreference extends Preference {
    private int mTintColor;

    public TintablePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setTint(int color) {
        this.mTintColor = color;
        notifyChanged();
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        if (this.mTintColor != 0) {
            ((ImageView) view.findViewById(16908294)).setImageTintList(ColorStateList.valueOf(this.mTintColor));
        }
    }
}
