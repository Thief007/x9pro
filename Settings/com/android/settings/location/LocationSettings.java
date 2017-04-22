package com.android.settings.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Switch;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBar.OnSwitchChangeListener;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISettingsMiscExt;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LocationSettings extends LocationSettingsBase implements OnSwitchChangeListener {
    private SettingsInjector injector;
    private PreferenceCategory mCategoryRecentLocationRequests;
    private ISettingsMiscExt mExt;
    private Preference mLocationMode;
    private UserHandle mManagedProfile;
    private Preference mManagedProfilePreference;
    private BroadcastReceiver mReceiver;
    private Switch mSwitch;
    private SwitchBar mSwitchBar;
    private UserManager mUm;
    private boolean mValidListener = false;

    class C04161 implements Comparator<Preference> {
        C04161() {
        }

        public int compare(Preference lhs, Preference rhs) {
            return lhs.getTitle().toString().compareTo(rhs.getTitle().toString());
        }
    }

    class C04183 extends BroadcastReceiver {
        C04183() {
        }

        public void onReceive(Context context, Intent intent) {
            if (Log.isLoggable("LocationSettings", 3)) {
                Log.d("LocationSettings", "Received settings change intent: " + intent);
            }
            LocationSettings.this.injector.reloadStatusMessages();
        }
    }

    protected int getMetricsCategory() {
        return 63;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        SettingsActivity activity = (SettingsActivity) getActivity();
        this.mUm = (UserManager) activity.getSystemService("user");
        this.mSwitchBar = activity.getSwitchBar();
        this.mSwitch = this.mSwitchBar.getSwitch();
        this.mSwitchBar.show();
        this.mExt = UtilsExt.getMiscPlugin(activity);
    }

    public void onDestroyView() {
        super.onDestroyView();
        this.mSwitchBar.hide();
    }

    public void onResume() {
        super.onResume();
        createPreferenceHierarchy();
        if (!this.mValidListener) {
            this.mSwitchBar.addOnSwitchChangeListener(this);
            this.mValidListener = true;
        }
    }

    public void onPause() {
        try {
            getActivity().unregisterReceiver(this.mReceiver);
        } catch (RuntimeException e) {
            if (Log.isLoggable("LocationSettings", 2)) {
                Log.v("LocationSettings", "Swallowing " + e);
            }
        }
        if (this.mValidListener) {
            this.mSwitchBar.removeOnSwitchChangeListener(this);
            this.mValidListener = false;
        }
        super.onPause();
    }

    private void addPreferencesSorted(List<Preference> prefs, PreferenceGroup container) {
        Collections.sort(prefs, new C04161());
        for (Preference entry : prefs) {
            container.addPreference(entry);
        }
    }

    private PreferenceScreen createPreferenceHierarchy() {
        final SettingsActivity activity = (SettingsActivity) getActivity();
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.location_settings);
        root = getPreferenceScreen();
        setupManagedProfileCategory(root);
        this.mLocationMode = root.findPreference("location_mode");
        this.mLocationMode.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                activity.startPreferencePanel(LocationMode.class.getName(), null, R.string.location_mode_screen_title, null, LocationSettings.this, 0);
                return true;
            }
        });
        this.mExt.initCustomizedLocationSettings(root, this.mLocationMode.getOrder() + 1);
        this.mCategoryRecentLocationRequests = (PreferenceCategory) root.findPreference("recent_location_requests");
        List<Preference> recentLocationRequests = new RecentLocationApps(activity).getAppList();
        if (recentLocationRequests.size() > 0) {
            addPreferencesSorted(recentLocationRequests, this.mCategoryRecentLocationRequests);
        } else {
            Preference banner = new Preference(activity);
            banner.setLayoutResource(R.layout.location_list_no_item);
            banner.setTitle(R.string.location_no_recent_apps);
            banner.setSelectable(false);
            this.mCategoryRecentLocationRequests.addPreference(banner);
        }
        boolean lockdownOnLocationAccess = false;
        if (this.mManagedProfile != null && this.mUm.hasUserRestriction("no_share_location", this.mManagedProfile)) {
            lockdownOnLocationAccess = true;
        }
        addLocationServices(activity, root, lockdownOnLocationAccess);
        refreshLocationMode();
        return root;
    }

    private void setupManagedProfileCategory(PreferenceScreen root) {
        this.mManagedProfile = Utils.getManagedProfile(this.mUm);
        if (this.mManagedProfile == null) {
            root.removePreference(root.findPreference("managed_profile_location_category"));
            this.mManagedProfilePreference = null;
            return;
        }
        this.mManagedProfilePreference = root.findPreference("managed_profile_location_switch");
        this.mManagedProfilePreference.setOnPreferenceClickListener(null);
    }

    private void changeManagedProfileLocationAccessStatus(boolean enabled, int summaryResId) {
        if (this.mManagedProfilePreference != null) {
            this.mManagedProfilePreference.setEnabled(enabled);
            this.mManagedProfilePreference.setSummary(summaryResId);
        }
    }

    private void addLocationServices(Context context, PreferenceScreen root, boolean lockdownOnLocationAccess) {
        PreferenceCategory categoryLocationServices = (PreferenceCategory) root.findPreference("location_services");
        this.injector = new SettingsInjector(context);
        List<Preference> locationServices = this.injector.getInjectedSettings(lockdownOnLocationAccess ? UserHandle.myUserId() : -2);
        this.mReceiver = new C04183();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.location.InjectedSettingChanged");
        context.registerReceiver(this.mReceiver, filter);
        if (locationServices.size() > 0) {
            addPreferencesSorted(locationServices, categoryLocationServices);
        } else {
            root.removePreference(categoryLocationServices);
        }
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, 1, 0, R.string.location_menu_scanning);
        super.onCreateOptionsMenu(menu, inflater);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        SettingsActivity activity = (SettingsActivity) getActivity();
        switch (item.getItemId()) {
            case 1:
                activity.startPreferencePanel(ScanningSettings.class.getName(), null, R.string.location_scanning_screen_title, null, this, 0);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public int getHelpResource() {
        return R.string.help_url_location_access;
    }

    public void onModeChanged(int mode, boolean restricted) {
        boolean z;
        switch (mode) {
            case 0:
                this.mLocationMode.setSummary(R.string.location_mode_location_off_title);
                break;
            case 1:
                this.mLocationMode.setSummary(R.string.location_mode_sensors_only_title);
                break;
            case 2:
                this.mLocationMode.setSummary(R.string.location_mode_battery_saving_title);
                break;
            case 3:
                this.mLocationMode.setSummary(R.string.location_mode_high_accuracy_title);
                break;
        }
        boolean enabled = mode != 0;
        SwitchBar switchBar = this.mSwitchBar;
        if (restricted) {
            z = false;
        } else {
            z = true;
        }
        switchBar.setEnabled(z);
        Preference preference = this.mLocationMode;
        if (!enabled || restricted) {
            z = false;
        } else {
            z = true;
        }
        preference.setEnabled(z);
        this.mExt.updateCustomizedLocationSettings();
        this.mCategoryRecentLocationRequests.setEnabled(enabled);
        if (enabled != this.mSwitch.isChecked()) {
            if (this.mValidListener) {
                this.mSwitchBar.removeOnSwitchChangeListener(this);
            }
            this.mSwitch.setChecked(enabled);
            if (this.mValidListener) {
                this.mSwitchBar.addOnSwitchChangeListener(this);
            }
        }
        if (this.mManagedProfilePreference != null) {
            if (this.mUm.hasUserRestriction("no_share_location", this.mManagedProfile)) {
                changeManagedProfileLocationAccessStatus(false, R.string.managed_profile_location_switch_lockdown);
            } else if (enabled) {
                changeManagedProfileLocationAccessStatus(true, R.string.switch_on_text);
            } else {
                changeManagedProfileLocationAccessStatus(false, R.string.switch_off_text);
            }
        }
        this.injector.reloadStatusMessages();
    }

    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (isChecked) {
            setLocationMode(3);
        } else {
            setLocationMode(0);
        }
    }
}
