package com.android.settings.accessibility;

import android.provider.Settings.Secure;
import com.android.settings.widget.ToggleSwitch;
import com.android.settings.widget.ToggleSwitch.OnBeforeCheckedChangeListener;

public class ToggleScreenMagnificationPreferenceFragment extends ToggleFeaturePreferenceFragment {

    class C02251 implements OnBeforeCheckedChangeListener {
        C02251() {
        }

        public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean checked) {
            ToggleScreenMagnificationPreferenceFragment.this.mSwitchBar.setCheckedInternal(checked);
            ToggleScreenMagnificationPreferenceFragment.this.getArguments().putBoolean("checked", checked);
            ToggleScreenMagnificationPreferenceFragment.this.onPreferenceToggled(ToggleScreenMagnificationPreferenceFragment.this.mPreferenceKey, checked);
            return false;
        }
    }

    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        Secure.putInt(getContentResolver(), "accessibility_display_magnification_enabled", enabled ? 1 : 0);
    }

    protected void onInstallSwitchBarToggleSwitch() {
        super.onInstallSwitchBarToggleSwitch();
        this.mToggleSwitch.setOnBeforeCheckedChangeListener(new C02251());
    }

    protected int getMetricsCategory() {
        return 7;
    }
}
