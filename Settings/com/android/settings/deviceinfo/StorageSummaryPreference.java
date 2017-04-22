package com.android.settings.deviceinfo;

import android.content.Context;
import android.graphics.Color;
import android.preference.Preference;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.settings.R;

public class StorageSummaryPreference extends Preference {
    private int mPercent = -1;

    public StorageSummaryPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.storage_summary);
        setEnabled(false);
    }

    public void setPercent(int percent) {
        this.mPercent = percent;
    }

    protected void onBindView(View view) {
        ProgressBar progress = (ProgressBar) view.findViewById(16908301);
        if (this.mPercent != -1) {
            progress.setVisibility(0);
            progress.setProgress(this.mPercent);
        } else {
            progress.setVisibility(8);
        }
        ((TextView) view.findViewById(16908304)).setTextColor(Color.parseColor("#8a000000"));
        super.onBindView(view);
    }
}
