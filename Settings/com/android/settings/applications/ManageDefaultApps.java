package com.android.settings.applications;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.SearchIndexableResource;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.content.PackageMonitor;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable;
import com.android.settings.search.Indexable.SearchIndexProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ManageDefaultApps extends SettingsPreferenceFragment implements OnPreferenceClickListener, Indexable {
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new C02663();
    private static final String TAG = ManageDefaultApps.class.getSimpleName();
    private DefaultBrowserPreference mDefaultBrowserPreference;
    private final Handler mHandler = new Handler();
    private final PackageMonitor mPackageMonitor = new C02652();
    private PackageManager mPm;
    private final Runnable mUpdateRunnable = new C02641();
    private int myUserId;

    class C02641 implements Runnable {
        C02641() {
        }

        public void run() {
            ManageDefaultApps.this.updateDefaultBrowserPreference();
        }
    }

    class C02652 extends PackageMonitor {
        C02652() {
        }

        public void onPackageAdded(String packageName, int uid) {
            sendUpdate();
        }

        public void onPackageAppeared(String packageName, int reason) {
            sendUpdate();
        }

        public void onPackageDisappeared(String packageName, int reason) {
            sendUpdate();
        }

        public void onPackageRemoved(String packageName, int uid) {
            sendUpdate();
        }

        private void sendUpdate() {
            ManageDefaultApps.this.mHandler.postDelayed(ManageDefaultApps.this.mUpdateRunnable, 500);
        }
    }

    static class C02663 extends BaseSearchIndexProvider {
        C02663() {
        }

        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
            new SearchIndexableResource(context).xmlResId = R.xml.default_apps;
            return Arrays.asList(new SearchIndexableResource[]{sir});
        }

        public List<String> getNonIndexableKeys(Context context) {
            ArrayList<String> result = new ArrayList();
            boolean isRestrictedUser = UserManager.get(context).getUserInfo(UserHandle.myUserId()).isRestricted();
            if (!DefaultSmsPreference.isAvailable(context) || isRestrictedUser) {
                result.add("default_sms_app");
            }
            if (!DefaultEmergencyPreference.isAvailable(context)) {
                result.add("default_emergency_app");
            }
            return result;
        }
    }

    class C02674 implements OnPreferenceChangeListener {
        C02674() {
        }

        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (newValue == null) {
                return false;
            }
            CharSequence packageName = (CharSequence) newValue;
            if (TextUtils.isEmpty(packageName)) {
                return false;
            }
            boolean result = ManageDefaultApps.this.mPm.setDefaultBrowserPackageName(packageName.toString(), ManageDefaultApps.this.myUserId);
            if (result) {
                ManageDefaultApps.this.mDefaultBrowserPreference.setValue(packageName.toString());
                ManageDefaultApps.this.mDefaultBrowserPreference.setSummary(ManageDefaultApps.this.mDefaultBrowserPreference.getEntry());
            }
            return result;
        }
    }

    private void updateDefaultBrowserPreference() {
        this.mDefaultBrowserPreference.refreshBrowserApps();
        PackageManager pm = getPackageManager();
        String packageName = pm.getDefaultBrowserPackageName(UserHandle.myUserId());
        if (TextUtils.isEmpty(packageName)) {
            this.mDefaultBrowserPreference.setSummary(R.string.default_browser_title_none);
            Log.d(TAG, "Cannot set empty default Browser value!");
            return;
        }
        Intent intent = new Intent();
        intent.setPackage(packageName);
        intent.setAction("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.BROWSABLE");
        intent.setData(Uri.parse("http:"));
        ResolveInfo info = this.mPm.resolveActivityAsUser(intent, 0, this.myUserId);
        if (info != null) {
            this.mDefaultBrowserPreference.setValue(packageName);
            this.mDefaultBrowserPreference.setSummary(info.loadLabel(pm));
            return;
        }
        this.mDefaultBrowserPreference.setSummary(R.string.default_browser_title_none);
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.default_apps);
        this.mPm = getPackageManager();
        this.myUserId = UserHandle.myUserId();
        this.mDefaultBrowserPreference = (DefaultBrowserPreference) findPreference("default_browser");
        this.mDefaultBrowserPreference.setOnPreferenceChangeListener(new C02674());
        if (UserManager.get(getActivity()).getUserInfo(this.myUserId).isRestricted() || !DefaultSmsPreference.isAvailable(getActivity())) {
            removePreference("default_sms_app");
        }
        if (!DefaultPhonePreference.isAvailable(getActivity())) {
            removePreference("default_phone_app");
        }
        if (!DefaultEmergencyPreference.isAvailable(getActivity())) {
            removePreference("default_emergency_app");
        }
        if (DefaultEmergencyPreference.isCapable(getActivity())) {
            Index.getInstance(getActivity()).updateFromClassNameResource(ManageDefaultApps.class.getName(), true, true);
        }
    }

    public void onResume() {
        super.onResume();
        updateDefaultBrowserPreference();
        this.mPackageMonitor.register(getActivity(), getActivity().getMainLooper(), false);
    }

    public void onPause() {
        super.onPause();
        this.mPackageMonitor.unregister();
    }

    protected int getMetricsCategory() {
        return 181;
    }

    public boolean onPreferenceClick(Preference preference) {
        return false;
    }
}
