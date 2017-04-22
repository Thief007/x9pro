package com.android.settings;

import android.preference.PreferenceFragment;
import com.android.internal.logging.MetricsLogger;

public abstract class InstrumentedFragment extends PreferenceFragment {
    protected abstract int getMetricsCategory();

    public void onResume() {
        super.onResume();
        MetricsLogger.visible(getActivity(), getMetricsCategory());
    }

    public void onPause() {
        super.onPause();
        MetricsLogger.hidden(getActivity(), getMetricsCategory());
    }
}
