package com.android.settings;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

public class AppListPreferenceWithSettings extends AppListPreference {
    private ComponentName mSettingsComponent;
    private View mSettingsIcon;

    class C00481 implements OnClickListener {
        C00481() {
        }

        public void onClick(View v) {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.setComponent(AppListPreferenceWithSettings.this.mSettingsComponent);
            AppListPreferenceWithSettings.this.getContext().startActivity(new Intent(intent));
        }
    }

    public AppListPreferenceWithSettings(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.preference_widget_settings);
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        this.mSettingsIcon = view.findViewById(R.id.settings_button);
        this.mSettingsIcon.setOnClickListener(new C00481());
        ((ViewGroup) this.mSettingsIcon.getParent()).setPaddingRelative(0, 0, 0, 0);
        updateSettingsVisibility();
    }

    private void updateSettingsVisibility() {
        if (this.mSettingsIcon != null) {
            if (this.mSettingsComponent == null) {
                this.mSettingsIcon.setVisibility(8);
            } else {
                this.mSettingsIcon.setVisibility(0);
            }
        }
    }

    protected void setSettingsComponent(ComponentName settings) {
        this.mSettingsComponent = settings;
        updateSettingsVisibility();
    }
}
