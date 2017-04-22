package com.android.settings.notification;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.service.notification.ZenModeConfig.ZenRule;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.notification.ZenRuleNameDialog.RuleInfo;

public class ZenModeExternalRuleSettings extends ZenModeRuleSettingsBase {
    private Preference mConfigure;
    private Preference mType;

    protected boolean setRule(ZenRule rule) {
        return rule != null;
    }

    protected String getZenModeDependency() {
        return null;
    }

    protected int getEnabledToastText() {
        return 0;
    }

    protected void onCreateInternal() {
        addPreferencesFromResource(R.xml.zen_mode_external_rule_settings);
        PreferenceScreen root = getPreferenceScreen();
        ServiceInfo si = ServiceListing.findService(this.mContext, ZenModeAutomationSettings.CONFIG, this.mRule.component);
        if (DEBUG) {
            Log.d("ZenModeSettings", "ServiceInfo: " + si);
        }
        final RuleInfo ri = getRuleInfo(si);
        if (DEBUG) {
            Log.d("ZenModeSettings", "RuleInfo: " + ri);
        }
        this.mType = root.findPreference("type");
        if (ri == null) {
            this.mType.setSummary(R.string.zen_mode_rule_type_unknown);
        } else {
            this.mType.setSummary(ri.caption);
        }
        this.mConfigure = root.findPreference("configure");
        if (ri == null || ri.configurationActivity == null) {
            this.mConfigure.setEnabled(false);
        } else {
            this.mConfigure.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    ZenModeExternalRuleSettings.this.startActivityForResult(new Intent().setComponent(ri.configurationActivity), 1);
                    return true;
                }
            });
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == -1 && data != null) {
            Uri conditionId = (Uri) data.getParcelableExtra("automatic.conditionId");
            if (conditionId != null && !conditionId.equals(this.mRule.conditionId)) {
                updateRule(conditionId);
            }
        }
    }

    public static RuleInfo getRuleInfo(ServiceInfo si) {
        if (si == null || si.metaData == null) {
            return null;
        }
        String ruleType = si.metaData.getString("automatic.ruleType");
        String defaultConditionId = si.metaData.getString("automatic.defaultConditionId");
        String configurationActivity = si.metaData.getString("automatic.configurationActivity");
        if (ruleType == null || ruleType.trim().isEmpty() || defaultConditionId == null) {
            return null;
        }
        RuleInfo ri = new RuleInfo();
        ri.serviceComponent = new ComponentName(si.packageName, si.name);
        ri.settingsAction = "android.settings.ZEN_MODE_EXTERNAL_RULE_SETTINGS";
        ri.caption = ruleType;
        ri.defaultConditionId = Uri.parse(defaultConditionId);
        if (configurationActivity != null) {
            ri.configurationActivity = ComponentName.unflattenFromString(configurationActivity);
        }
        return ri;
    }

    protected void updateControlsInternal() {
    }

    protected int getMetricsCategory() {
        return 145;
    }
}
