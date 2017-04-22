package com.android.settings.notification;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenModeConfig.ZenRule;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.DropDownPreference;
import com.android.settings.DropDownPreference.Callback;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.notification.ZenRuleNameDialog.RuleInfo;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBar.OnSwitchChangeListener;

public abstract class ZenModeRuleSettingsBase extends ZenModeSettingsBase implements OnSwitchChangeListener {
    protected static final boolean DEBUG = ZenModeSettingsBase.DEBUG;
    protected Context mContext;
    private boolean mDeleting;
    protected boolean mDisableListeners;
    private Toast mEnabledToast;
    protected ZenRule mRule;
    private String mRuleId;
    private Preference mRuleName;
    private SwitchBar mSwitchBar;
    private DropDownPreference mZenMode;

    class C04781 implements OnPreferenceClickListener {
        C04781() {
        }

        public boolean onPreferenceClick(Preference preference) {
            ZenModeRuleSettingsBase.this.showRuleNameDialog();
            return true;
        }
    }

    class C04792 implements Callback {
        C04792() {
        }

        public boolean onItemSelected(int pos, Object value) {
            if (ZenModeRuleSettingsBase.this.mDisableListeners) {
                return true;
            }
            int zenMode = ((Integer) value).intValue();
            if (zenMode == ZenModeRuleSettingsBase.this.mRule.zenMode) {
                return true;
            }
            if (ZenModeRuleSettingsBase.DEBUG) {
                Log.d("ZenModeSettings", "onPrefChange zenMode=" + zenMode);
            }
            ZenModeRuleSettingsBase.this.mRule.zenMode = zenMode;
            ZenModeRuleSettingsBase.this.setZenModeConfig(ZenModeRuleSettingsBase.this.mConfig);
            return true;
        }
    }

    class C04814 implements OnClickListener {
        C04814() {
        }

        public void onClick(DialogInterface dialog, int which) {
            MetricsLogger.action(ZenModeRuleSettingsBase.this.mContext, 175);
            ZenModeRuleSettingsBase.this.mDeleting = true;
            ZenModeRuleSettingsBase.this.mConfig.automaticRules.remove(ZenModeRuleSettingsBase.this.mRuleId);
            ZenModeRuleSettingsBase.this.setZenModeConfig(ZenModeRuleSettingsBase.this.mConfig);
        }
    }

    protected abstract int getEnabledToastText();

    protected abstract String getZenModeDependency();

    protected abstract void onCreateInternal();

    protected abstract boolean setRule(ZenRule zenRule);

    protected abstract void updateControlsInternal();

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mContext = getActivity();
        Intent intent = getActivity().getIntent();
        if (DEBUG) {
            Log.d("ZenModeSettings", "onCreate getIntent()=" + intent);
        }
        if (intent == null) {
            Log.w("ZenModeSettings", "No intent");
            toastAndFinish();
            return;
        }
        this.mRuleId = intent.getStringExtra("rule_id");
        if (DEBUG) {
            Log.d("ZenModeSettings", "mRuleId=" + this.mRuleId);
        }
        if (!refreshRuleOrFinish()) {
            setHasOptionsMenu(true);
            onCreateInternal();
            PreferenceScreen root = getPreferenceScreen();
            this.mRuleName = root.findPreference("rule_name");
            this.mRuleName.setOnPreferenceClickListener(new C04781());
            this.mZenMode = (DropDownPreference) root.findPreference("zen_mode");
            this.mZenMode.addItem((int) R.string.zen_mode_option_important_interruptions, Integer.valueOf(1));
            this.mZenMode.addItem((int) R.string.zen_mode_option_alarms, Integer.valueOf(3));
            this.mZenMode.addItem((int) R.string.zen_mode_option_no_interruptions, Integer.valueOf(2));
            this.mZenMode.setCallback(new C04792());
            this.mZenMode.setOrder(10);
            this.mZenMode.setDependency(getZenModeDependency());
        }
    }

    public void onResume() {
        super.onResume();
        updateControls();
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        this.mSwitchBar.addOnSwitchChangeListener(this);
        this.mSwitchBar.show();
    }

    public void onDestroyView() {
        super.onDestroyView();
        this.mSwitchBar.removeOnSwitchChangeListener(this);
        this.mSwitchBar.hide();
    }

    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (DEBUG) {
            Log.d("ZenModeSettings", "onSwitchChanged " + isChecked);
        }
        if (!this.mDisableListeners) {
            boolean enabled = isChecked;
            if (enabled != this.mRule.enabled) {
                MetricsLogger.action(this.mContext, 176, enabled);
                if (DEBUG) {
                    Log.d("ZenModeSettings", "onSwitchChanged enabled=" + enabled);
                }
                this.mRule.enabled = enabled;
                this.mRule.snoozing = false;
                setZenModeConfig(this.mConfig);
                if (enabled) {
                    int toastText = getEnabledToastText();
                    if (toastText != 0) {
                        this.mEnabledToast = Toast.makeText(this.mContext, toastText, 0);
                        this.mEnabledToast.show();
                    }
                } else if (this.mEnabledToast != null) {
                    this.mEnabledToast.cancel();
                }
            }
        }
    }

    protected void updateRule(Uri newConditionId) {
        this.mRule.conditionId = newConditionId;
        this.mRule.condition = null;
        this.mRule.snoozing = false;
        setZenModeConfig(this.mConfig);
    }

    protected void onZenModeChanged() {
    }

    protected void onZenModeConfigChanged() {
        if (!refreshRuleOrFinish()) {
            updateControls();
        }
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (DEBUG) {
            Log.d("ZenModeSettings", "onCreateOptionsMenu");
        }
        inflater.inflate(R.menu.zen_mode_rule, menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (DEBUG) {
            Log.d("ZenModeSettings", "onOptionsItemSelected " + item.getItemId());
        }
        if (item.getItemId() != R.id.delete) {
            return super.onOptionsItemSelected(item);
        }
        MetricsLogger.action(this.mContext, 174);
        showDeleteRuleDialog();
        return true;
    }

    private void showRuleNameDialog() {
        new ZenRuleNameDialog(this.mContext, null, this.mRule.name, this.mConfig.getAutomaticRuleNames()) {
            public void onOk(String ruleName, RuleInfo type) {
                ZenModeConfig newConfig = ZenModeRuleSettingsBase.this.mConfig.copy();
                ZenRule rule = (ZenRule) newConfig.automaticRules.get(ZenModeRuleSettingsBase.this.mRuleId);
                if (rule != null) {
                    rule.name = ruleName;
                    ZenModeRuleSettingsBase.this.setZenModeConfig(newConfig);
                }
            }
        }.show();
    }

    private boolean refreshRuleOrFinish() {
        this.mRule = (ZenRule) this.mConfig.automaticRules.get(this.mRuleId);
        if (DEBUG) {
            Log.d("ZenModeSettings", "mRule=" + this.mRule);
        }
        if (setRule(this.mRule)) {
            return false;
        }
        toastAndFinish();
        return true;
    }

    private void showDeleteRuleDialog() {
        View messageView = new Builder(this.mContext).setMessage(getString(R.string.zen_mode_delete_rule_confirmation, new Object[]{this.mRule.name})).setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.zen_mode_delete_rule_button, new C04814()).show().findViewById(16908299);
        if (messageView != null) {
            messageView.setTextDirection(5);
        }
    }

    private void toastAndFinish() {
        if (!this.mDeleting) {
            Toast.makeText(this.mContext, R.string.zen_mode_rule_not_found_text, 0).show();
        }
        getActivity().finish();
    }

    private void updateRuleName() {
        getActivity().setTitle(this.mRule.name);
        this.mRuleName.setSummary(this.mRule.name);
    }

    private void updateControls() {
        this.mDisableListeners = true;
        updateRuleName();
        updateControlsInternal();
        this.mZenMode.setSelectedValue(Integer.valueOf(this.mRule.zenMode));
        if (this.mSwitchBar != null) {
            this.mSwitchBar.setChecked(this.mRule.enabled);
        }
        this.mDisableListeners = false;
    }
}
