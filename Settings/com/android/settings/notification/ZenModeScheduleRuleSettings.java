package com.android.settings.notification;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenModeConfig.ScheduleInfo;
import android.service.notification.ZenModeConfig.ZenRule;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.TimePicker;
import com.android.settings.R;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

public class ZenModeScheduleRuleSettings extends ZenModeRuleSettingsBase {
    private final SimpleDateFormat mDayFormat = new SimpleDateFormat("EEE");
    private Preference mDays;
    private TimePickerPreference mEnd;
    private ScheduleInfo mSchedule;
    private TimePickerPreference mStart;

    class C04831 implements OnPreferenceClickListener {
        C04831() {
        }

        public boolean onPreferenceClick(Preference preference) {
            ZenModeScheduleRuleSettings.this.showDaysDialog();
            return true;
        }
    }

    class C04842 implements Callback {
        C04842() {
        }

        public boolean onSetTime(int hour, int minute) {
            if (ZenModeScheduleRuleSettings.this.mDisableListeners) {
                return true;
            }
            if (!ZenModeConfig.isValidHour(hour) || !ZenModeConfig.isValidMinute(minute)) {
                return false;
            }
            if (hour == ZenModeScheduleRuleSettings.this.mSchedule.startHour && minute == ZenModeScheduleRuleSettings.this.mSchedule.startMinute) {
                return true;
            }
            if (ZenModeScheduleRuleSettings.DEBUG) {
                Log.d("ZenModeSettings", "onPrefChange start h=" + hour + " m=" + minute);
            }
            ZenModeScheduleRuleSettings.this.mSchedule.startHour = hour;
            ZenModeScheduleRuleSettings.this.mSchedule.startMinute = minute;
            ZenModeScheduleRuleSettings.this.updateRule(ZenModeConfig.toScheduleConditionId(ZenModeScheduleRuleSettings.this.mSchedule));
            return true;
        }
    }

    class C04853 implements Callback {
        C04853() {
        }

        public boolean onSetTime(int hour, int minute) {
            if (ZenModeScheduleRuleSettings.this.mDisableListeners) {
                return true;
            }
            if (!ZenModeConfig.isValidHour(hour) || !ZenModeConfig.isValidMinute(minute)) {
                return false;
            }
            if (hour == ZenModeScheduleRuleSettings.this.mSchedule.endHour && minute == ZenModeScheduleRuleSettings.this.mSchedule.endMinute) {
                return true;
            }
            if (ZenModeScheduleRuleSettings.DEBUG) {
                Log.d("ZenModeSettings", "onPrefChange end h=" + hour + " m=" + minute);
            }
            ZenModeScheduleRuleSettings.this.mSchedule.endHour = hour;
            ZenModeScheduleRuleSettings.this.mSchedule.endMinute = minute;
            ZenModeScheduleRuleSettings.this.updateRule(ZenModeConfig.toScheduleConditionId(ZenModeScheduleRuleSettings.this.mSchedule));
            return true;
        }
    }

    class C04875 implements OnDismissListener {
        C04875() {
        }

        public void onDismiss(DialogInterface dialog) {
            ZenModeScheduleRuleSettings.this.updateDays();
        }
    }

    private static class TimePickerPreference extends Preference {
        private Callback mCallback;
        private final Context mContext;
        private int mHourOfDay;
        private int mMinute;
        private int mSummaryFormat;

        public interface Callback {
            boolean onSetTime(int i, int i2);
        }

        public static class TimePickerFragment extends DialogFragment implements OnTimeSetListener {
            public TimePickerPreference pref;

            public Dialog onCreateDialog(Bundle savedInstanceState) {
                boolean usePref = this.pref != null && this.pref.mHourOfDay >= 0 && this.pref.mMinute >= 0;
                Calendar c = Calendar.getInstance();
                return new TimePickerDialog(getActivity(), this, usePref ? this.pref.mHourOfDay : c.get(11), usePref ? this.pref.mMinute : c.get(12), DateFormat.is24HourFormat(getActivity()));
            }

            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                if (this.pref != null) {
                    this.pref.setTime(hourOfDay, minute);
                }
            }
        }

        public TimePickerPreference(Context context, final FragmentManager mgr) {
            super(context);
            this.mContext = context;
            setPersistent(false);
            setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    TimePickerFragment frag = new TimePickerFragment();
                    frag.pref = TimePickerPreference.this;
                    frag.show(mgr, TimePickerPreference.class.getName());
                    return true;
                }
            });
        }

        public void setCallback(Callback callback) {
            this.mCallback = callback;
        }

        public void setSummaryFormat(int resId) {
            this.mSummaryFormat = resId;
            updateSummary();
        }

        public void setTime(int hourOfDay, int minute) {
            if (this.mCallback == null || this.mCallback.onSetTime(hourOfDay, minute)) {
                this.mHourOfDay = hourOfDay;
                this.mMinute = minute;
                updateSummary();
            }
        }

        private void updateSummary() {
            Calendar c = Calendar.getInstance();
            c.set(11, this.mHourOfDay);
            c.set(12, this.mMinute);
            String time = DateFormat.getTimeFormat(this.mContext).format(c.getTime());
            if (this.mSummaryFormat != 0) {
                time = this.mContext.getResources().getString(this.mSummaryFormat, new Object[]{time});
            }
            setSummary(time);
        }
    }

    protected boolean setRule(ZenRule rule) {
        ScheduleInfo scheduleInfo = null;
        if (rule != null) {
            scheduleInfo = ZenModeConfig.tryParseScheduleConditionId(rule.conditionId);
        }
        this.mSchedule = scheduleInfo;
        return this.mSchedule != null;
    }

    protected String getZenModeDependency() {
        return this.mDays.getKey();
    }

    protected int getEnabledToastText() {
        return R.string.zen_schedule_rule_enabled_toast;
    }

    protected void onCreateInternal() {
        addPreferencesFromResource(R.xml.zen_mode_schedule_rule_settings);
        PreferenceScreen root = getPreferenceScreen();
        this.mDays = root.findPreference("days");
        this.mDays.setOnPreferenceClickListener(new C04831());
        FragmentManager mgr = getFragmentManager();
        this.mStart = new TimePickerPreference(this.mContext, mgr);
        this.mStart.setKey("start_time");
        this.mStart.setTitle(R.string.zen_mode_start_time);
        this.mStart.setCallback(new C04842());
        root.addPreference(this.mStart);
        this.mStart.setDependency(this.mDays.getKey());
        this.mEnd = new TimePickerPreference(this.mContext, mgr);
        this.mEnd.setKey("end_time");
        this.mEnd.setTitle(R.string.zen_mode_end_time);
        this.mEnd.setCallback(new C04853());
        root.addPreference(this.mEnd);
        this.mEnd.setDependency(this.mDays.getKey());
    }

    private void updateDays() {
        int[] days = this.mSchedule.days;
        if (days != null && days.length > 0) {
            StringBuilder sb = new StringBuilder();
            Calendar c = Calendar.getInstance();
            for (int day : ZenModeScheduleDaysSelection.DAYS) {
                int j = 0;
                while (j < days.length) {
                    if (day == days[j]) {
                        c.set(7, day);
                        if (sb.length() > 0) {
                            sb.append(this.mContext.getString(R.string.summary_divider_text));
                        }
                        sb.append(this.mDayFormat.format(c.getTime()));
                    } else {
                        j++;
                    }
                }
            }
            if (sb.length() > 0) {
                this.mDays.setSummary(sb);
                this.mDays.notifyDependencyChange(false);
                return;
            }
        }
        this.mDays.setSummary(R.string.zen_mode_schedule_rule_days_none);
        this.mDays.notifyDependencyChange(true);
    }

    private void updateEndSummary() {
        this.mEnd.setSummaryFormat((this.mSchedule.startHour * 60) + this.mSchedule.startMinute >= (this.mSchedule.endHour * 60) + this.mSchedule.endMinute ? R.string.zen_mode_end_time_next_day_summary_format : 0);
    }

    protected void updateControlsInternal() {
        updateDays();
        this.mStart.setTime(this.mSchedule.startHour, this.mSchedule.startMinute);
        this.mEnd.setTime(this.mSchedule.endHour, this.mSchedule.endMinute);
        updateEndSummary();
    }

    protected int getMetricsCategory() {
        return 144;
    }

    private void showDaysDialog() {
        new Builder(this.mContext).setTitle(R.string.zen_mode_schedule_rule_days).setView(new ZenModeScheduleDaysSelection(this.mContext, this.mSchedule.days) {
            protected void onChanged(int[] days) {
                if (!ZenModeScheduleRuleSettings.this.mDisableListeners && !Arrays.equals(days, ZenModeScheduleRuleSettings.this.mSchedule.days)) {
                    if (ZenModeScheduleRuleSettings.DEBUG) {
                        Log.d("ZenModeSettings", "days.onChanged days=" + Arrays.asList(new int[][]{days}));
                    }
                    ZenModeScheduleRuleSettings.this.mSchedule.days = days;
                    ZenModeScheduleRuleSettings.this.updateRule(ZenModeConfig.toScheduleConditionId(ZenModeScheduleRuleSettings.this.mSchedule));
                }
            }
        }).setOnDismissListener(new C04875()).setPositiveButton(R.string.done_button, null).show();
    }
}
