package com.android.settings.notification;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenModeConfig.EventInfo;
import android.service.notification.ZenModeConfig.ScheduleInfo;
import android.service.notification.ZenModeConfig.ZenRule;
import android.text.format.DateFormat;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.notification.ServiceListing.Callback;
import com.android.settings.notification.ZenRuleNameDialog.RuleInfo;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public class ZenModeAutomationSettings extends ZenModeSettingsBase {
    static final Config CONFIG = getConditionProviderConfig();
    private static final Comparator<ZenRuleInfo> RULE_COMPARATOR = new C04622();
    private final Calendar mCalendar = Calendar.getInstance();
    private final SimpleDateFormat mDayFormat = new SimpleDateFormat("EEE");
    private ServiceListing mServiceListing;
    private final Callback mServiceListingCallback = new C04611();

    class C04611 implements Callback {
        C04611() {
        }

        public void onServicesReloaded(List<ServiceInfo> services) {
            for (ServiceInfo service : services) {
                RuleInfo ri = ZenModeExternalRuleSettings.getRuleInfo(service);
                if (!(ri == null || ri.serviceComponent == null || ri.settingsAction != "android.settings.ZEN_MODE_EXTERNAL_RULE_SETTINGS" || ZenModeAutomationSettings.this.mServiceListing.isEnabled(ri.serviceComponent))) {
                    Log.i("ZenModeSettings", "Enabling external condition provider: " + ri.serviceComponent);
                    ZenModeAutomationSettings.this.mServiceListing.setEnabled(ri.serviceComponent, true);
                }
            }
        }
    }

    static class C04622 implements Comparator<ZenRuleInfo> {
        C04622() {
        }

        public int compare(ZenRuleInfo lhs, ZenRuleInfo rhs) {
            return key(lhs).compareTo(key(rhs));
        }

        private String key(ZenRuleInfo zri) {
            int type;
            ZenRule rule = zri.rule;
            if (ZenModeConfig.isValidScheduleConditionId(rule.conditionId)) {
                type = 1;
            } else if (ZenModeConfig.isValidEventConditionId(rule.conditionId)) {
                type = 2;
            } else {
                type = 3;
            }
            return type + rule.name;
        }
    }

    class C04655 implements OnPreferenceClickListener {
        C04655() {
        }

        public boolean onPreferenceClick(Preference preference) {
            MetricsLogger.action(ZenModeAutomationSettings.this.mContext, 172);
            ZenModeAutomationSettings.this.showAddRuleDialog();
            return true;
        }
    }

    private static class ZenRuleInfo {
        String id;
        ZenRule rule;

        private ZenRuleInfo() {
        }
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.zen_mode_automation_settings);
        this.mServiceListing = new ServiceListing(this.mContext, CONFIG);
        this.mServiceListing.addCallback(this.mServiceListingCallback);
        this.mServiceListing.reload();
        this.mServiceListing.setListening(true);
    }

    public void onDestroy() {
        super.onDestroy();
        this.mServiceListing.setListening(false);
        this.mServiceListing.removeCallback(this.mServiceListingCallback);
    }

    protected void onZenModeChanged() {
    }

    protected void onZenModeConfigChanged() {
        updateControls();
    }

    public void onResume() {
        super.onResume();
        updateControls();
    }

    private void showAddRuleDialog() {
        new ZenRuleNameDialog(this.mContext, this.mServiceListing, null, this.mConfig.getAutomaticRuleNames()) {
            public void onOk(String ruleName, RuleInfo ri) {
                MetricsLogger.action(ZenModeAutomationSettings.this.mContext, 173);
                ZenRule rule = new ZenRule();
                rule.name = ruleName;
                rule.enabled = true;
                rule.zenMode = 1;
                rule.conditionId = ri.defaultConditionId;
                rule.component = ri.serviceComponent;
                ZenModeConfig newConfig = ZenModeAutomationSettings.this.mConfig.copy();
                String ruleId = newConfig.newRuleId();
                newConfig.automaticRules.put(ruleId, rule);
                if (ZenModeAutomationSettings.this.setZenModeConfig(newConfig)) {
                    ZenModeAutomationSettings.this.showRule(ri.settingsAction, ri.configurationActivity, ruleId, rule.name);
                }
            }
        }.show();
    }

    private void showRule(String settingsAction, ComponentName configurationActivity, String ruleId, String ruleName) {
        if (DEBUG) {
            Log.d("ZenModeSettings", "showRule " + ruleId + " name=" + ruleName);
        }
        this.mContext.startActivity(new Intent(settingsAction).addFlags(67108864).putExtra("rule_id", ruleId));
    }

    private ZenRuleInfo[] sortedRules() {
        ZenRuleInfo[] rt = new ZenRuleInfo[this.mConfig.automaticRules.size()];
        for (int i = 0; i < rt.length; i++) {
            ZenRuleInfo zri = new ZenRuleInfo();
            zri.id = (String) this.mConfig.automaticRules.keyAt(i);
            zri.rule = (ZenRule) this.mConfig.automaticRules.valueAt(i);
            rt[i] = zri;
        }
        Arrays.sort(rt, RULE_COMPARATOR);
        return rt;
    }

    private void updateControls() {
        PreferenceScreen root = getPreferenceScreen();
        root.removeAll();
        if (this.mConfig != null) {
            Preference p;
            ZenRuleInfo[] sortedRules = sortedRules();
            for (int i = 0; i < sortedRules.length; i++) {
                int i2;
                final String id = sortedRules[i].id;
                final ZenRule rule = sortedRules[i].rule;
                final boolean isSchedule = ZenModeConfig.isValidScheduleConditionId(rule.conditionId);
                final boolean isEvent = ZenModeConfig.isValidEventConditionId(rule.conditionId);
                p = new Preference(this.mContext);
                if (isSchedule) {
                    i2 = R.drawable.ic_schedule;
                } else if (isEvent) {
                    i2 = R.drawable.ic_event;
                } else {
                    i2 = R.drawable.ic_label;
                }
                p.setIcon(i2);
                p.setTitle(rule.name);
                p.setSummary(computeRuleSummary(rule));
                p.setPersistent(false);
                p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        String action;
                        if (isSchedule) {
                            action = "android.settings.ZEN_MODE_SCHEDULE_RULE_SETTINGS";
                        } else if (isEvent) {
                            action = "android.settings.ZEN_MODE_EVENT_RULE_SETTINGS";
                        } else {
                            action = "android.settings.ZEN_MODE_EXTERNAL_RULE_SETTINGS";
                        }
                        ZenModeAutomationSettings.this.showRule(action, null, id, rule.name);
                        return true;
                    }
                });
                root.addPreference(p);
            }
            p = new Preference(this.mContext);
            p.setIcon(R.drawable.ic_add);
            p.setTitle(R.string.zen_mode_add_rule);
            p.setPersistent(false);
            p.setOnPreferenceClickListener(new C04655());
            root.addPreference(p);
        }
    }

    protected int getMetricsCategory() {
        return 142;
    }

    private String computeRuleSummary(ZenRule rule) {
        if (rule == null || !rule.enabled) {
            return getString(R.string.switch_off_text);
        }
        String mode = computeZenModeCaption(getResources(), rule.zenMode);
        String summary = getString(R.string.switch_on_text);
        ScheduleInfo schedule = ZenModeConfig.tryParseScheduleConditionId(rule.conditionId);
        EventInfo event = ZenModeConfig.tryParseEventConditionId(rule.conditionId);
        if (schedule != null) {
            summary = computeScheduleRuleSummary(schedule);
        } else if (event != null) {
            summary = computeEventRuleSummary(event);
        }
        return getString(R.string.zen_mode_rule_summary_combination, new Object[]{summary, mode});
    }

    private String computeScheduleRuleSummary(ScheduleInfo schedule) {
        String days = computeContiguousDayRanges(schedule.days);
        String start = getTime(schedule.startHour, schedule.startMinute);
        String end = getTime(schedule.endHour, schedule.endMinute);
        String time = getString(R.string.summary_range_verbal_combination, new Object[]{start, end});
        return getString(R.string.zen_mode_rule_summary_combination, new Object[]{days, time});
    }

    private String computeEventRuleSummary(EventInfo event) {
        String calendar = getString(R.string.zen_mode_event_rule_summary_calendar_template, new Object[]{computeCalendarName(event)});
        String reply = getString(R.string.zen_mode_event_rule_summary_reply_template, new Object[]{getString(computeReply(event))});
        return getString(R.string.zen_mode_rule_summary_combination, new Object[]{calendar, reply});
    }

    private String computeCalendarName(EventInfo event) {
        if (event.calendar != null) {
            return event.calendar;
        }
        return getString(R.string.zen_mode_event_rule_summary_any_calendar);
    }

    private int computeReply(EventInfo event) {
        switch (event.reply) {
            case 0:
                return R.string.zen_mode_event_rule_reply_any_except_no;
            case 1:
                return R.string.zen_mode_event_rule_reply_yes_or_maybe;
            case 2:
                return R.string.zen_mode_event_rule_reply_yes;
            default:
                throw new IllegalArgumentException("Bad reply: " + event.reply);
        }
    }

    private String getTime(int hour, int minute) {
        this.mCalendar.set(11, hour);
        this.mCalendar.set(12, minute);
        return DateFormat.getTimeFormat(this.mContext).format(this.mCalendar.getTime());
    }

    private String computeContiguousDayRanges(int[] days) {
        TreeSet<Integer> daySet = new TreeSet();
        int i = 0;
        while (days != null && i < days.length) {
            daySet.add(Integer.valueOf(days[i]));
            i++;
        }
        if (daySet.isEmpty()) {
            return getString(R.string.zen_mode_schedule_rule_days_none);
        }
        int N = ZenModeConfig.ALL_DAYS.length;
        if (daySet.size() == N) {
            return getString(R.string.zen_mode_schedule_rule_days_all);
        }
        String rt = null;
        i = 0;
        while (i < N) {
            int startDay = ZenModeConfig.ALL_DAYS[i];
            if (daySet.contains(Integer.valueOf(startDay))) {
                int end = 0;
                while (daySet.contains(Integer.valueOf(ZenModeConfig.ALL_DAYS[((i + end) + 1) % N]))) {
                    end++;
                }
                if (!(i == 0 ? daySet.contains(Integer.valueOf(ZenModeConfig.ALL_DAYS[N - 1])) : false)) {
                    String v;
                    if (end == 0) {
                        v = dayString(startDay);
                    } else {
                        v = getString(R.string.summary_range_symbol_combination, new Object[]{dayString(startDay), dayString(ZenModeConfig.ALL_DAYS[(i + end) % N])});
                    }
                    rt = rt == null ? v : getString(R.string.summary_divider_text, new Object[]{rt, v});
                }
                i += end;
            }
            i++;
        }
        return rt;
    }

    private String dayString(int day) {
        this.mCalendar.set(7, day);
        return this.mDayFormat.format(this.mCalendar.getTime());
    }

    private static Config getConditionProviderConfig() {
        Config c = new Config();
        c.tag = "ZenModeSettings";
        c.setting = "enabled_condition_providers";
        c.intentAction = "android.service.notification.ConditionProviderService";
        c.permission = "android.permission.BIND_CONDITION_PROVIDER_SERVICE";
        c.noun = "condition provider";
        return c;
    }

    private static String computeZenModeCaption(Resources res, int zenMode) {
        switch (zenMode) {
            case 1:
                return res.getString(R.string.zen_mode_option_important_interruptions);
            case 2:
                return res.getString(R.string.zen_mode_option_no_interruptions);
            case 3:
                return res.getString(R.string.zen_mode_option_alarms);
            default:
                return null;
        }
    }
}
