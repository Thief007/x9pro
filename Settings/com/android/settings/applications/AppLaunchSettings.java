package com.android.settings.applications;

import android.app.AlertDialog;
import android.content.IntentFilter;
import android.content.pm.IntentFilterVerificationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import com.android.settings.DropDownPreference;
import com.android.settings.DropDownPreference.Callback;
import com.android.settings.R;
import com.android.settings.Utils;
import java.util.List;

public class AppLaunchSettings extends AppInfoWithHeader implements OnClickListener, OnPreferenceChangeListener {
    private AppDomainsPreference mAppDomainUrls;
    private DropDownPreference mAppLinkState;
    private ClearDefaultsPreference mClearDefaultsPreference;
    private boolean mHasDomainUrls;
    private PackageManager mPm;

    class C02371 implements Callback {
        C02371() {
        }

        public boolean onItemSelected(int pos, Object value) {
            return AppLaunchSettings.this.updateAppLinkState(((Integer) value).intValue());
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        boolean z = false;
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.installed_app_launch_settings);
        this.mPm = getActivity().getPackageManager();
        if ((this.mAppEntry.info.privateFlags & 16) != 0) {
            z = true;
        }
        this.mHasDomainUrls = z;
        List<IntentFilterVerificationInfo> iviList = this.mPm.getIntentFilterVerifications(this.mPackageName);
        List<IntentFilter> filters = this.mPm.getAllIntentFilters(this.mPackageName);
        this.mAppDomainUrls = (AppDomainsPreference) findPreference("app_launch_supported_domain_urls");
        CharSequence[] entries = getEntries(this.mPackageName, iviList, filters);
        this.mAppDomainUrls.setTitles(entries);
        this.mAppDomainUrls.setValues(new int[entries.length]);
        this.mClearDefaultsPreference = (ClearDefaultsPreference) findPreference("app_launch_clear_defaults");
        buildStateDropDown();
    }

    private void buildStateDropDown() {
        this.mAppLinkState = (DropDownPreference) findPreference("app_link_state");
        this.mAppLinkState.addItem((int) R.string.app_link_open_always, Integer.valueOf(2));
        this.mAppLinkState.addItem((int) R.string.app_link_open_ask, Integer.valueOf(4));
        this.mAppLinkState.addItem((int) R.string.app_link_open_never, Integer.valueOf(3));
        this.mAppLinkState.setEnabled(this.mHasDomainUrls);
        if (this.mHasDomainUrls) {
            int state = this.mPm.getIntentVerificationStatus(this.mPackageName, UserHandle.myUserId());
            DropDownPreference dropDownPreference = this.mAppLinkState;
            if (state == 0) {
                state = 4;
            }
            dropDownPreference.setSelectedValue(Integer.valueOf(state));
            this.mAppLinkState.setCallback(new C02371());
        }
    }

    private boolean updateAppLinkState(int newState) {
        int userId = UserHandle.myUserId();
        if (this.mPm.getIntentVerificationStatus(this.mPackageName, userId) == newState) {
            return false;
        }
        boolean success = this.mPm.updateIntentVerificationStatus(this.mPackageName, newState, userId);
        if (success) {
            success = newState == this.mPm.getIntentVerificationStatus(this.mPackageName, userId);
        } else {
            Log.e("AppLaunchSettings", "Couldn't update intent verification status!");
        }
        return success;
    }

    private CharSequence[] getEntries(String packageName, List<IntentFilterVerificationInfo> list, List<IntentFilter> list2) {
        ArraySet<String> result = Utils.getHandledDomains(this.mPm, packageName);
        return (CharSequence[]) result.toArray(new CharSequence[result.size()]);
    }

    protected boolean refreshUi() {
        this.mClearDefaultsPreference.setPackageName(this.mPackageName);
        this.mClearDefaultsPreference.setAppEntry(this.mAppEntry);
        return true;
    }

    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    public void onClick(View v) {
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }

    protected int getMetricsCategory() {
        return 17;
    }
}
