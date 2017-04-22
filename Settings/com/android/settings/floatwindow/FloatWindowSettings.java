package com.android.settings.floatwindow;

import android.app.ActionBar.LayoutParams;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.provider.Settings.System;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import com.android.settings.R;

public class FloatWindowSettings extends PreferenceActivity implements OnPreferenceClickListener, OnCheckedChangeListener {
    private String[] app_local = new String[]{"top_one", "left_one", "right_one", "bottom_one", "top_two", "left_two", "right_two", "bottom_two"};
    private Preference bottom_one_view;
    private Preference bottom_two_view;
    private Preference left_one_view;
    private Preference left_two_view;
    private Switch mActionBarSwitch;
    private Context mContext;
    private boolean mSwitchChecked;
    private PackageManager packageManager;
    private Preference right_one_view;
    private Preference right_two_view;
    private Preference top_one_view;
    private Preference top_two_view;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("FloatWindowSettings", "onCreate() :");
        this.mContext = this;
        addPreferencesFromResource(R.xml.v_float_window_settings);
        this.packageManager = getPackageManager();
        this.top_one_view = findPreference("top_one");
        this.left_one_view = findPreference("left_one");
        this.right_one_view = findPreference("right_one");
        this.bottom_one_view = findPreference("bottom_one");
        this.top_one_view.setOnPreferenceClickListener(this);
        this.left_one_view.setOnPreferenceClickListener(this);
        this.right_one_view.setOnPreferenceClickListener(this);
        this.bottom_one_view.setOnPreferenceClickListener(this);
        this.top_two_view = findPreference("top_two");
        this.left_two_view = findPreference("left_two");
        this.right_two_view = findPreference("right_two");
        this.bottom_two_view = findPreference("bottom_two");
        this.top_two_view.setOnPreferenceClickListener(this);
        this.left_two_view.setOnPreferenceClickListener(this);
        this.right_two_view.setOnPreferenceClickListener(this);
        this.bottom_two_view.setOnPreferenceClickListener(this);
        init();
    }

    public void onResume() {
        updateText(this.top_one_view.getKey(), this.top_one_view, "com.android.dialer&com.android.dialer.DialtactsActivity");
        updateText(this.left_one_view.getKey(), this.left_one_view, "com.android.mms&com.android.mms.ui.BootActivity");
        updateText(this.right_one_view.getKey(), this.right_one_view, "com.android.email&com.android.email.activity.Welcome");
        updateText(this.bottom_one_view.getKey(), this.bottom_one_view, "com.android.browser&com.android.browser.BrowserActivity");
        updateText(this.top_two_view.getKey(), this.top_two_view, null);
        updateText(this.left_two_view.getKey(), this.left_two_view, null);
        updateText(this.right_two_view.getKey(), this.right_two_view, null);
        updateText(this.bottom_two_view.getKey(), this.bottom_two_view, null);
        super.onResume();
    }

    public void updateText(String appLocal, Preference view, String defaultApp) {
        CharSequence string;
        String strCompontent = System.getString(this.mContext.getContentResolver(), appLocal);
        ActivityInfo activityInfo = null;
        if (strCompontent != null) {
            try {
                activityInfo = this.packageManager.getActivityInfo(new ComponentName(strCompontent.split("&")[0], strCompontent.split("&")[1]), 0);
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
        } else if (defaultApp != null) {
            activityInfo = this.packageManager.getActivityInfo(new ComponentName(defaultApp.split("&")[0], defaultApp.split("&")[1]), 0);
            System.putString(this.mContext.getContentResolver(), appLocal, defaultApp);
        }
        if (activityInfo == null) {
            string = getResources().getString(R.string.float_window_no_app_set);
        } else {
            string = activityInfo.loadLabel(this.packageManager);
        }
        view.setTitle(string);
    }

    public void init() {
        boolean z = true;
        this.mActionBarSwitch = new Switch(this);
        if (System.getInt(this.mContext.getContentResolver(), "apps_switch", 0) != 1) {
            z = false;
        }
        this.mSwitchChecked = z;
        this.mActionBarSwitch.setChecked(this.mSwitchChecked);
        this.mActionBarSwitch.setOnCheckedChangeListener(this);
        setCheckBoxEnabled(this.mActionBarSwitch.isChecked());
        if (activity instanceof PreferenceActivity) {
            PreferenceActivity preferenceActivity = (PreferenceActivity) activity;
            if (preferenceActivity.onIsHidingHeaders() || !preferenceActivity.onIsMultiPane()) {
                this.mActionBarSwitch.setPadding(0, 0, getResources().getDimensionPixelSize(R.dimen.action_bar_switch_padding), 0);
                if (getActionBar() != null) {
                    getActionBar().setDisplayOptions(16, 16);
                    getActionBar().setCustomView(this.mActionBarSwitch, new LayoutParams(-2, -2, 8388629));
                }
            }
        }
    }

    public boolean onPreferenceClick(Preference preference) {
        Log.d("FloatWindowSettings", "onPreferenceClick() :");
        Intent allAppsIntent = new Intent();
        allAppsIntent.putExtra("witch", preference.getKey());
        allAppsIntent.setClass(this.mContext, AllAppsListActivity.class);
        startActivity(allAppsIntent);
        return true;
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.d("FloatWindowSettings", "onCheckedChanged() :");
        System.putInt(this.mContext.getContentResolver(), "apps_switch", isChecked ? 1 : 0);
        setCheckBoxEnabled(isChecked);
        Intent intent = new Intent("show_float_apps_window");
        intent.putExtra("is_show", isChecked);
        this.mContext.sendBroadcast(intent);
    }

    private void setCheckBoxEnabled(boolean isEnabled) {
        this.top_one_view.setEnabled(isEnabled);
        this.left_one_view.setEnabled(isEnabled);
        this.right_one_view.setEnabled(isEnabled);
        this.bottom_one_view.setEnabled(isEnabled);
        this.top_two_view.setEnabled(isEnabled);
        this.left_two_view.setEnabled(isEnabled);
        this.right_two_view.setEnabled(isEnabled);
        this.bottom_two_view.setEnabled(isEnabled);
    }
}
