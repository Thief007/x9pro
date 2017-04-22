package com.android.settings.fuelgauge;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;
import com.android.settings.AppProgressPreference;
import com.android.settings.Utils;

public class PowerGaugePreference extends AppProgressPreference {
    private final CharSequence mContentDescription;
    private BatteryEntry mInfo;

    public PowerGaugePreference(Context context, Drawable icon, CharSequence contentDescription, BatteryEntry info) {
        super(context, null);
        if (icon == null) {
            icon = new ColorDrawable(0);
        }
        setIcon(icon);
        this.mInfo = info;
        this.mContentDescription = contentDescription;
    }

    public void setPercent(double percentOfMax, double percentOfTotal) {
        setProgress((int) Math.ceil(percentOfMax));
        setSummary(Utils.formatPercentage((int) (0.5d + percentOfTotal)));
    }

    BatteryEntry getInfo() {
        return this.mInfo;
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        if (this.mContentDescription != null) {
            ((TextView) view.findViewById(16908310)).setContentDescription(this.mContentDescription);
        }
    }
}
