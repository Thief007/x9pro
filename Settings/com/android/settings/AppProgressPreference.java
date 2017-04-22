package com.android.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;

public class AppProgressPreference extends TintablePreference {
    private int mProgress;

    public AppProgressPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_app);
        setWidgetLayoutResource(R.layout.widget_progress_bar);
    }

    public void setProgress(int amount) {
        this.mProgress = amount;
        notifyChanged();
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        ((ProgressBar) view.findViewById(16908301)).setProgress(this.mProgress);
    }
}
