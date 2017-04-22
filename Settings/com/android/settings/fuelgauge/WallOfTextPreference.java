package com.android.settings.fuelgauge;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class WallOfTextPreference extends Preference {
    public WallOfTextPreference(Context context) {
        super(context);
    }

    public WallOfTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WallOfTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public WallOfTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        ((TextView) view.findViewById(16908304)).setMaxLines(20);
    }
}
