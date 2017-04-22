package com.android.settings;

import android.preference.PreferenceActivity;
import com.android.internal.logging.MetricsLogger;

public abstract class InstrumentedPreferenceActivity extends PreferenceActivity {
    protected abstract int getMetricsCategory();

    protected void onResume() {
        super.onResume();
        MetricsLogger.visible(this, getMetricsCategory());
    }

    protected void onPause() {
        super.onPause();
        MetricsLogger.hidden(this, getMetricsCategory());
    }
}
