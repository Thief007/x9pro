package com.android.settings.accessibility;

import android.provider.Settings.Global;
import com.android.settings.widget.ToggleSwitch;
import com.android.settings.widget.ToggleSwitch.OnBeforeCheckedChangeListener;

public class ToggleGlobalGesturePreferenceFragment extends ToggleFeaturePreferenceFragment {

    class C02241 implements OnBeforeCheckedChangeListener {
        C02241() {
        }

        public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean checked) {
            ToggleGlobalGesturePreferenceFragment.this.mSwitchBar.setCheckedInternal(checked);
            ToggleGlobalGesturePreferenceFragment.this.getArguments().putBoolean("checked", checked);
            ToggleGlobalGesturePreferenceFragment.this.onPreferenceToggled(ToggleGlobalGesturePreferenceFragment.this.mPreferenceKey, checked);
            return false;
        }
    }

    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        Global.putInt(getContentResolver(), "enable_accessibility_global_gesture_enabled", enabled ? 1 : 0);
    }

    protected void onInstallSwitchBarToggleSwitch() {
        super.onInstallSwitchBarToggleSwitch();
        this.mToggleSwitch.setOnBeforeCheckedChangeListener(new C02241());
    }

    protected int getMetricsCategory() {
        return 6;
    }
}
