package com.mediatek.settings.sim;

import android.content.Context;
import android.preference.Preference;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import com.android.settings.R;
import com.mediatek.settings.FeatureOption;

public class RadioPowerPreference extends Preference {
    private RadioPowerController mController;
    private boolean mPowerEnabled = true;
    private boolean mPowerState;
    private Switch mRadioSwith = null;
    private int mSubId = -1;

    class C07391 implements OnCheckedChangeListener {
        C07391() {
        }

        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            boolean z = false;
            if (!RadioPowerPreference.this.mController.needRadioSwitch(RadioPowerPreference.this.mSubId, isChecked)) {
                Log.d("RadioPowerPreference", "no need switch, roll back check~~ current status: " + RadioPowerPreference.this.mRadioSwith.isChecked());
                RadioPowerPreference.this.setRadioOn(TelephonyUtils.isRadioOn(RadioPowerPreference.this.mSubId, RadioPowerPreference.this.getContext()));
            } else if (RadioPowerPreference.this.mController.setRadionOn(RadioPowerPreference.this.mSubId, isChecked)) {
                RadioPowerPreference.this.setRadioEnabled(false);
            } else {
                RadioPowerPreference radioPowerPreference = RadioPowerPreference.this;
                if (!isChecked) {
                    z = true;
                }
                radioPowerPreference.setRadioOn(z);
            }
        }
    }

    public RadioPowerPreference(Context context) {
        super(context);
        this.mController = RadioPowerController.getInstance(context);
        setWidgetLayoutResource(R.layout.radio_power_switch);
    }

    public void setRadioOn(boolean state) {
        this.mPowerState = state;
        if (this.mRadioSwith != null) {
            this.mRadioSwith.setChecked(state);
        }
    }

    public void setRadioEnabled(boolean enable) {
        this.mPowerEnabled = enable;
        if (this.mRadioSwith != null) {
            this.mRadioSwith.setEnabled(enable);
        }
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        this.mRadioSwith = (Switch) view.findViewById(R.id.radio_state);
        if (this.mRadioSwith != null) {
            if (FeatureOption.MTK_A1_FEATURE) {
                this.mRadioSwith.setVisibility(8);
            }
            this.mRadioSwith.setChecked(this.mPowerState);
            this.mRadioSwith.setEnabled(this.mPowerEnabled);
            this.mRadioSwith.setOnCheckedChangeListener(new C07391());
        }
    }

    public void setEnabled(boolean enabled) {
        this.mPowerEnabled = enabled;
        super.setEnabled(enabled);
    }

    public void bindRadioPowerState(int subId) {
        this.mSubId = subId;
        setRadioOn(TelephonyUtils.isRadioOn(subId, getContext()));
        setRadioEnabled(SubscriptionManager.isValidSubscriptionId(subId));
    }
}
