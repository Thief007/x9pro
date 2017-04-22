package com.android.settings;

import android.content.Context;
import android.preference.PreferenceCategory;
import android.view.View;

public class AccessiblePreferenceCategory extends PreferenceCategory {
    private String mContentDescription;

    public AccessiblePreferenceCategory(Context context) {
        super(context);
    }

    public void setContentDescription(String contentDescription) {
        this.mContentDescription = contentDescription;
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        view.setContentDescription(this.mContentDescription);
    }
}
