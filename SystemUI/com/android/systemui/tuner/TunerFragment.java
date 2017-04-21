package com.android.systemui.tuner;

import android.app.AlertDialog.Builder;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.SwitchPreference;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.tuner.TunerService.Tunable;

public class TunerFragment extends PreferenceFragment {
    private SwitchPreference mBatteryPct;
    private final OnPreferenceChangeListener mBatteryPctChange = new OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            boolean v = ((Boolean) newValue).booleanValue();
            MetricsLogger.action(TunerFragment.this.getContext(), 237, v);
            System.putInt(TunerFragment.this.getContext().getContentResolver(), "status_bar_show_battery_percent", v ? 1 : 0);
            return true;
        }
    };
    private final SettingObserver mSettingObserver = new SettingObserver();

    private final class SettingObserver extends ContentObserver {
        public SettingObserver() {
            super(new Handler());
        }

        public void onChange(boolean selfChange, Uri uri, int userId) {
            super.onChange(selfChange, uri, userId);
            TunerFragment.this.updateBatteryPct();
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.tuner_prefs);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        setHasOptionsMenu(true);
        findPreference("qs_tuner").setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                FragmentTransaction ft = TunerFragment.this.getFragmentManager().beginTransaction();
                ft.replace(16908290, new QsTuner(), "QsTuner");
                ft.addToBackStack(null);
                ft.commit();
                return true;
            }
        });
        findPreference("demo_mode").setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                FragmentTransaction ft = TunerFragment.this.getFragmentManager().beginTransaction();
                ft.replace(16908290, new DemoModeFragment(), "DemoMode");
                ft.addToBackStack(null);
                ft.commit();
                return true;
            }
        });
        this.mBatteryPct = (SwitchPreference) findPreference("battery_pct");
        if (Secure.getInt(getContext().getContentResolver(), "seen_tuner_warning", 0) == 0) {
            new Builder(getContext()).setTitle(R.string.tuner_warning_title).setMessage(R.string.tuner_warning).setPositiveButton(R.string.got_it, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Secure.putInt(TunerFragment.this.getContext().getContentResolver(), "seen_tuner_warning", 1);
                }
            }).show();
        }
    }

    public void onResume() {
        super.onResume();
        updateBatteryPct();
        getContext().getContentResolver().registerContentObserver(System.getUriFor("status_bar_show_battery_percent"), false, this.mSettingObserver);
        registerPrefs(getPreferenceScreen());
        MetricsLogger.visibility(getContext(), 227, true);
    }

    public void onPause() {
        super.onPause();
        getContext().getContentResolver().unregisterContentObserver(this.mSettingObserver);
        unregisterPrefs(getPreferenceScreen());
        MetricsLogger.visibility(getContext(), 227, false);
    }

    private void registerPrefs(PreferenceGroup group) {
        TunerService tunerService = TunerService.get(getContext());
        int N = group.getPreferenceCount();
        for (int i = 0; i < N; i++) {
            Preference pref = group.getPreference(i);
            if (pref instanceof StatusBarSwitch) {
                tunerService.addTunable((Tunable) pref, "icon_blacklist");
            } else if (pref instanceof PreferenceGroup) {
                registerPrefs((PreferenceGroup) pref);
            }
        }
    }

    private void unregisterPrefs(PreferenceGroup group) {
        TunerService tunerService = TunerService.get(getContext());
        int N = group.getPreferenceCount();
        for (int i = 0; i < N; i++) {
            Preference pref = group.getPreference(i);
            if (pref instanceof Tunable) {
                tunerService.removeTunable((Tunable) pref);
            } else if (pref instanceof PreferenceGroup) {
                registerPrefs((PreferenceGroup) pref);
            }
        }
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, 2, 0, R.string.remove_from_settings);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 2:
                TunerService.showResetRequest(getContext(), new Runnable() {
                    public void run() {
                        TunerFragment.this.getActivity().finish();
                    }
                });
                return true;
            case 16908332:
                getActivity().finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateBatteryPct() {
        boolean z = false;
        this.mBatteryPct.setOnPreferenceChangeListener(null);
        SwitchPreference switchPreference = this.mBatteryPct;
        if (System.getInt(getContext().getContentResolver(), "status_bar_show_battery_percent", 0) != 0) {
            z = true;
        }
        switchPreference.setChecked(z);
        this.mBatteryPct.setOnPreferenceChangeListener(this.mBatteryPctChange);
    }
}
