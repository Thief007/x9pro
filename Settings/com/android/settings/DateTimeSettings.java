package com.android.settings;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog.Builder;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.DatePicker;
import android.widget.TimePicker;
import com.android.settingslib.datetime.ZoneGetter;
import java.util.Calendar;
import java.util.Date;

public class DateTimeSettings extends SettingsPreferenceFragment implements OnSharedPreferenceChangeListener, OnTimeSetListener, OnDateSetListener, OnClickListener, OnCancelListener {
    private ListPreference mAutoTimePref;
    private SwitchPreference mAutoTimeZonePref;
    private Preference mDatePref;
    private Calendar mDummyDate;
    private BroadcastReceiver mIntentReceiver = new C01081();
    private Preference mTime24Pref;
    private Preference mTimePref;
    private Preference mTimeZone;

    class C01081 extends BroadcastReceiver {
        C01081() {
        }

        public void onReceive(Context context, Intent intent) {
            Activity activity = DateTimeSettings.this.getActivity();
            if (activity != null) {
                DateTimeSettings.this.updateTimeAndDateDisplay(activity);
            }
        }
    }

    protected int getMetricsCategory() {
        return 38;
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.date_time_prefs);
        initUI();
    }

    private void initUI() {
        boolean z;
        boolean z2 = false;
        boolean autoTimeEnabled = getAutoState("auto_time");
        boolean autoTimeZoneEnabled = getAutoState("auto_time_zone");
        this.mAutoTimePref = (ListPreference) findPreference("auto_time_list");
        if (((DevicePolicyManager) getSystemService("device_policy")).getAutoTimeRequired()) {
            this.mAutoTimePref.setEnabled(false);
        }
        boolean isFirstRun = getActivity().getIntent().getBooleanExtra("firstRun", false);
        this.mDummyDate = Calendar.getInstance();
        boolean autoTimeGpsEnabled = getAutoState("auto_time_gps");
        if (autoTimeEnabled) {
            this.mAutoTimePref.setValueIndex(0);
        } else if (autoTimeGpsEnabled) {
            this.mAutoTimePref.setValueIndex(1);
        } else {
            this.mAutoTimePref.setValueIndex(2);
        }
        this.mAutoTimePref.setSummary(this.mAutoTimePref.getValue());
        this.mAutoTimeZonePref = (SwitchPreference) findPreference("auto_zone");
        if (Utils.isWifiOnly(getActivity()) || isFirstRun) {
            getPreferenceScreen().removePreference(this.mAutoTimeZonePref);
            autoTimeZoneEnabled = false;
        }
        this.mAutoTimeZonePref.setChecked(autoTimeZoneEnabled);
        this.mTimePref = findPreference("time");
        this.mTime24Pref = findPreference("24 hour");
        this.mTimeZone = findPreference("timezone");
        this.mDatePref = findPreference("date");
        boolean z3 = !autoTimeEnabled ? autoTimeGpsEnabled : true;
        Preference preference = this.mTimePref;
        if (z3) {
            z = false;
        } else {
            z = true;
        }
        preference.setEnabled(z);
        preference = this.mDatePref;
        if (z3) {
            z = false;
        } else {
            z = true;
        }
        preference.setEnabled(z);
        Preference preference2 = this.mTimeZone;
        if (!autoTimeZoneEnabled) {
            z2 = true;
        }
        preference2.setEnabled(z2);
    }

    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        ((SwitchPreference) this.mTime24Pref).setChecked(is24Hour());
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.TIME_TICK");
        filter.addAction("android.intent.action.TIME_SET");
        filter.addAction("android.intent.action.TIMEZONE_CHANGED");
        getActivity().registerReceiver(this.mIntentReceiver, filter, null, null);
        updateTimeAndDateDisplay(getActivity());
    }

    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(this.mIntentReceiver);
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    public void updateTimeAndDateDisplay(Context context) {
        Calendar now = Calendar.getInstance();
        this.mDummyDate.setTimeZone(now.getTimeZone());
        this.mDummyDate.set(now.get(1), 11, 31, 13, 0, 0);
        Date dummyDate = this.mDummyDate.getTime();
        this.mDatePref.setSummary(DateFormat.getLongDateFormat(context).format(now.getTime()));
        this.mTimePref.setSummary(DateFormat.getTimeFormat(getActivity()).format(now.getTime()));
        this.mTimeZone.setSummary(ZoneGetter.getTimeZoneOffsetAndName(now.getTimeZone(), now.getTime()));
        this.mTime24Pref.setSummary(DateFormat.getTimeFormat(getActivity()).format(dummyDate));
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        Activity activity = getActivity();
        if (activity != null) {
            setDate(activity, year, month, day);
            updateTimeAndDateDisplay(activity);
        }
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        Activity activity = getActivity();
        if (activity != null) {
            setTime(activity, hourOfDay, minute);
            updateTimeAndDateDisplay(activity);
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        boolean z = false;
        Preference preference;
        if (key.equals("auto_time_list")) {
            boolean z2;
            String value = this.mAutoTimePref.getValue();
            int index = this.mAutoTimePref.findIndexOfValue(value);
            this.mAutoTimePref.setSummary(value);
            boolean autoEnabled = true;
            if (index == 0) {
                Global.putInt(getContentResolver(), "auto_time", 1);
                Global.putInt(getContentResolver(), "auto_time_gps", 0);
            } else if (index == 1) {
                showDialog(2);
                setOnCancelListener(this);
            } else {
                Global.putInt(getContentResolver(), "auto_time", 0);
                Global.putInt(getContentResolver(), "auto_time_gps", 0);
                autoEnabled = false;
            }
            Preference preference2 = this.mTimePref;
            if (autoEnabled) {
                z2 = false;
            } else {
                z2 = true;
            }
            preference2.setEnabled(z2);
            preference = this.mDatePref;
            if (!autoEnabled) {
                z = true;
            }
            preference.setEnabled(z);
        } else if (key.equals("auto_zone")) {
            int i;
            boolean autoZoneEnabled = preferences.getBoolean(key, true);
            ContentResolver contentResolver = getContentResolver();
            String str = "auto_time_zone";
            if (autoZoneEnabled) {
                i = 1;
            } else {
                i = 0;
            }
            Global.putInt(contentResolver, str, i);
            preference = this.mTimeZone;
            if (!autoZoneEnabled) {
                z = true;
            }
            preference.setEnabled(z);
        }
    }

    public Dialog onCreateDialog(int id) {
        Calendar calendar = Calendar.getInstance();
        switch (id) {
            case 0:
                DatePickerDialog d = new DatePickerDialog(getActivity(), this, calendar.get(1), calendar.get(2), calendar.get(5));
                configureDatePicker(d.getDatePicker());
                return d;
            case 1:
                return new TimePickerDialog(getActivity(), this, calendar.get(11), calendar.get(12), DateFormat.is24HourFormat(getActivity()));
            case 2:
                int msg;
                if (Secure.isLocationProviderEnabled(getContentResolver(), "gps")) {
                    msg = R.string.gps_time_sync_attention_gps_on;
                } else {
                    msg = R.string.gps_time_sync_attention_gps_off;
                }
                return new Builder(getActivity()).setMessage(getActivity().getResources().getString(msg)).setTitle(R.string.proxy_error).setIcon(17301543).setPositiveButton(17039379, this).setNegativeButton(17039369, this).create();
            default:
                throw new IllegalArgumentException();
        }
    }

    static void configureDatePicker(DatePicker datePicker) {
        Calendar t = Calendar.getInstance();
        t.clear();
        t.set(1970, 0, 1);
        datePicker.setMinDate(t.getTimeInMillis());
        t.clear();
        t.set(2037, 11, 31);
        datePicker.setMaxDate(t.getTimeInMillis());
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == this.mDatePref) {
            showDialog(0);
        } else if (preference == this.mTimePref) {
            removeDialog(1);
            showDialog(1);
        } else if (preference == this.mTime24Pref) {
            boolean is24Hour = ((SwitchPreference) this.mTime24Pref).isChecked();
            set24Hour(is24Hour);
            updateTimeAndDateDisplay(getActivity());
            timeUpdated(is24Hour);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        updateTimeAndDateDisplay(getActivity());
    }

    private void timeUpdated(boolean is24Hour) {
        Intent timeChanged = new Intent("android.intent.action.TIME_SET");
        timeChanged.putExtra("android.intent.extra.TIME_PREF_24_HOUR_FORMAT", is24Hour);
        getActivity().sendBroadcast(timeChanged);
    }

    private boolean is24Hour() {
        return DateFormat.is24HourFormat(getActivity());
    }

    private void set24Hour(boolean is24Hour) {
        System.putString(getContentResolver(), "time_12_24", is24Hour ? "24" : "12");
    }

    private boolean getAutoState(String name) {
        boolean z = false;
        try {
            if (Global.getInt(getContentResolver(), name) > 0) {
                z = true;
            }
            return z;
        } catch (SettingNotFoundException e) {
            return false;
        }
    }

    static void setDate(Context context, int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(1, year);
        c.set(2, month);
        c.set(5, day);
        long when = c.getTimeInMillis();
        if (when / 1000 < 2147483647L) {
            ((AlarmManager) context.getSystemService("alarm")).setTime(when);
        }
    }

    static void setTime(Context context, int hourOfDay, int minute) {
        Calendar c = Calendar.getInstance();
        c.set(11, hourOfDay);
        c.set(12, minute);
        c.set(13, 0);
        c.set(14, 0);
        long when = c.getTimeInMillis();
        if (when / 1000 < 2147483647L) {
            ((AlarmManager) context.getSystemService("alarm")).setTime(when);
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == -1) {
            Log.d("DateTimeSettings", "Enable GPS time sync");
            if (!Secure.isLocationProviderEnabled(getContentResolver(), "gps")) {
                Secure.setLocationProviderEnabled(getContentResolver(), "gps", true);
            }
            Global.putInt(getContentResolver(), "auto_time", 0);
            Global.putInt(getContentResolver(), "auto_time_gps", 1);
            this.mAutoTimePref.setValueIndex(1);
            this.mAutoTimePref.setSummary(this.mAutoTimePref.getValue());
        } else if (which == -2) {
            Log.d("DateTimeSettings", "DialogInterface.BUTTON_NEGATIVE");
            reSetAutoTimePref();
        }
    }

    private void reSetAutoTimePref() {
        Log.d("DateTimeSettings", "reset AutoTimePref as cancel the selection");
        boolean autoTimeEnabled = getAutoState("auto_time");
        boolean autoTimeGpsEnabled = getAutoState("auto_time_gps");
        if (autoTimeEnabled) {
            this.mAutoTimePref.setValueIndex(0);
        } else if (autoTimeGpsEnabled) {
            this.mAutoTimePref.setValueIndex(1);
        } else {
            this.mAutoTimePref.setValueIndex(2);
        }
        this.mAutoTimePref.setSummary(this.mAutoTimePref.getValue());
    }

    public void onCancel(DialogInterface arg0) {
        Log.d("DateTimeSettings", "onCancel Dialog");
        reSetAutoTimePref();
    }
}
