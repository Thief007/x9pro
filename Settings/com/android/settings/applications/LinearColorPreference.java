package com.android.settings.applications;

import android.preference.Preference;
import android.view.View;
import com.android.settings.R;
import com.android.settings.applications.LinearColorBar.OnRegionTappedListener;

public class LinearColorPreference extends Preference {
    int mColoredRegions;
    int mGreenColor;
    float mGreenRatio;
    OnRegionTappedListener mOnRegionTappedListener;
    int mRedColor;
    float mRedRatio;
    int mYellowColor;
    float mYellowRatio;

    protected void onBindView(View view) {
        super.onBindView(view);
        LinearColorBar colors = (LinearColorBar) view.findViewById(R.id.linear_color_bar);
        colors.setShowIndicator(false);
        colors.setColors(this.mRedColor, this.mYellowColor, this.mGreenColor);
        colors.setRatios(this.mRedRatio, this.mYellowRatio, this.mGreenRatio);
        colors.setColoredRegions(this.mColoredRegions);
        colors.setOnRegionTappedListener(this.mOnRegionTappedListener);
    }
}
