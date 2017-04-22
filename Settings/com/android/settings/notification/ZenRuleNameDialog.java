package com.android.settings.notification;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.pm.ServiceInfo;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenModeConfig.EventInfo;
import android.service.notification.ZenModeConfig.ScheduleInfo;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.ArraySet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.android.settings.R;
import com.android.settings.notification.ServiceListing.Callback;
import java.util.List;

public abstract class ZenRuleNameDialog {
    private static final boolean DEBUG = ZenModeSettings.DEBUG;
    private final AlertDialog mDialog;
    private final EditText mEditText;
    private final ArraySet<String> mExistingNames;
    private final RuleInfo[] mExternalRules = new RuleInfo[3];
    private final boolean mIsNew;
    private final String mOriginalRuleName;
    private final ColorStateList mOriginalTint;
    private final ServiceListing mServiceListing;
    private final Callback mServiceListingCallback = new C04901();
    private final RadioGroup mTypes;
    private final View mWarning;
    private final ColorStateList mWarningTint;

    class C04901 implements Callback {
        C04901() {
        }

        public void onServicesReloaded(List<ServiceInfo> services) {
            if (ZenRuleNameDialog.DEBUG) {
                Log.d("ZenModeSettings", "Services reloaded: count=" + services.size());
            }
            RuleInfo[] -get1 = ZenRuleNameDialog.this.mExternalRules;
            ZenRuleNameDialog.this.mExternalRules[2] = null;
            ZenRuleNameDialog.this.mExternalRules[1] = null;
            -get1[0] = null;
            int i = 0;
            for (ServiceInfo si : services) {
                RuleInfo ri = ZenModeExternalRuleSettings.getRuleInfo(si);
                if (ri != null) {
                    ZenRuleNameDialog.this.mExternalRules[i] = ri;
                    i++;
                    if (i == ZenRuleNameDialog.this.mExternalRules.length) {
                        break;
                    }
                }
            }
            ZenRuleNameDialog.this.bindExternalRules();
        }
    }

    class C04912 implements OnClickListener {
        C04912() {
        }

        public void onClick(DialogInterface dialog, int which) {
            String newName = ZenRuleNameDialog.this.trimmedText();
            if (ZenRuleNameDialog.this.mIsNew || ZenRuleNameDialog.this.mOriginalRuleName == null || !ZenRuleNameDialog.this.mOriginalRuleName.equalsIgnoreCase(newName)) {
                ZenRuleNameDialog.this.onOk(newName, ZenRuleNameDialog.this.selectedRuleInfo());
            }
        }
    }

    class C04923 implements OnDismissListener {
        C04923() {
        }

        public void onDismiss(DialogInterface dialog) {
            if (ZenRuleNameDialog.this.mServiceListing != null) {
                ZenRuleNameDialog.this.mServiceListing.removeCallback(ZenRuleNameDialog.this.mServiceListingCallback);
            }
        }
    }

    class C04934 implements TextWatcher {
        C04934() {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void afterTextChanged(Editable s) {
            ZenRuleNameDialog.this.updatePositiveButtonAndWarning();
        }
    }

    public static class RuleInfo {
        public String caption;
        public ComponentName configurationActivity;
        public Uri defaultConditionId;
        public ComponentName serviceComponent;
        public String settingsAction;
    }

    public abstract void onOk(String str, RuleInfo ruleInfo);

    public ZenRuleNameDialog(Context context, ServiceListing serviceListing, String ruleName, ArraySet<String> existingNames) {
        boolean z;
        this.mServiceListing = serviceListing;
        if (ruleName == null) {
            z = true;
        } else {
            z = false;
        }
        this.mIsNew = z;
        this.mOriginalRuleName = ruleName;
        this.mWarningTint = ColorStateList.valueOf(context.getColor(R.color.zen_rule_name_warning));
        View v = LayoutInflater.from(context).inflate(R.layout.zen_rule_name, null, false);
        this.mEditText = (EditText) v.findViewById(R.id.rule_name);
        this.mWarning = v.findViewById(R.id.rule_name_warning);
        if (!this.mIsNew) {
            this.mEditText.setText(ruleName);
        }
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(16843829, outValue, true);
        this.mOriginalTint = ColorStateList.valueOf(outValue.data);
        this.mEditText.setSelectAllOnFocus(true);
        this.mTypes = (RadioGroup) v.findViewById(R.id.rule_types);
        if (this.mServiceListing != null) {
            bindType(R.id.rule_type_schedule, defaultNewSchedule());
            bindType(R.id.rule_type_event, defaultNewEvent());
            bindExternalRules();
            this.mServiceListing.addCallback(this.mServiceListingCallback);
            this.mServiceListing.reload();
        } else {
            this.mTypes.setVisibility(8);
        }
        this.mDialog = new Builder(context).setTitle(this.mIsNew ? R.string.zen_mode_add_rule : R.string.zen_mode_rule_name).setView(v).setPositiveButton(R.string.okay, new C04912()).setOnDismissListener(new C04923()).setNegativeButton(R.string.cancel, null).create();
        this.mEditText.addTextChangedListener(new C04934());
        this.mExistingNames = new ArraySet(existingNames.size());
        for (String existingName : existingNames) {
            this.mExistingNames.add(existingName.toLowerCase());
        }
    }

    public void show() {
        this.mDialog.show();
        updatePositiveButtonAndWarning();
    }

    private void bindType(int id, RuleInfo ri) {
        RadioButton rb = (RadioButton) this.mTypes.findViewById(id);
        if (ri == null) {
            rb.setVisibility(8);
            return;
        }
        rb.setVisibility(0);
        if (ri.caption != null) {
            rb.setText(ri.caption);
        }
        rb.setTag(ri);
    }

    private RuleInfo selectedRuleInfo() {
        int id = this.mTypes.getCheckedRadioButtonId();
        if (id == -1) {
            return null;
        }
        return (RuleInfo) ((RadioButton) this.mTypes.findViewById(id)).getTag();
    }

    private String trimmedText() {
        return this.mEditText.getText() == null ? null : this.mEditText.getText().toString().trim();
    }

    private void updatePositiveButtonAndWarning() {
        ColorStateList colorStateList;
        String name = trimmedText();
        boolean validName = !TextUtils.isEmpty(name) ? !name.equalsIgnoreCase(this.mOriginalRuleName) ? !this.mExistingNames.contains(name.toLowerCase()) : true : false;
        this.mDialog.getButton(-1).setEnabled(validName);
        boolean showWarning = (TextUtils.isEmpty(name) || validName) ? false : true;
        this.mWarning.setVisibility(showWarning ? 0 : 4);
        EditText editText = this.mEditText;
        if (showWarning) {
            colorStateList = this.mWarningTint;
        } else {
            colorStateList = this.mOriginalTint;
        }
        editText.setBackgroundTintList(colorStateList);
    }

    private static RuleInfo defaultNewSchedule() {
        ScheduleInfo schedule = new ScheduleInfo();
        schedule.days = ZenModeConfig.ALL_DAYS;
        schedule.startHour = 22;
        schedule.endHour = 7;
        RuleInfo rt = new RuleInfo();
        rt.settingsAction = "android.settings.ZEN_MODE_SCHEDULE_RULE_SETTINGS";
        rt.defaultConditionId = ZenModeConfig.toScheduleConditionId(schedule);
        return rt;
    }

    private static RuleInfo defaultNewEvent() {
        EventInfo event = new EventInfo();
        event.calendar = null;
        event.reply = 0;
        RuleInfo rt = new RuleInfo();
        rt.settingsAction = "android.settings.ZEN_MODE_EVENT_RULE_SETTINGS";
        rt.defaultConditionId = ZenModeConfig.toEventConditionId(event);
        return rt;
    }

    private void bindExternalRules() {
        bindType(R.id.rule_type_3, this.mExternalRules[0]);
        bindType(R.id.rule_type_4, this.mExternalRules[1]);
        bindType(R.id.rule_type_5, this.mExternalRules[2]);
    }
}
