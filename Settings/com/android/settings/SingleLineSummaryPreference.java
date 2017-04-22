package com.android.settings;

import android.content.Context;
import android.preference.Preference;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class SingleLineSummaryPreference extends Preference {
    public SingleLineSummaryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        TextView summaryView = (TextView) view.findViewById(16908304);
        summaryView.setSingleLine();
        summaryView.setEllipsize(TruncateAt.END);
    }
}
