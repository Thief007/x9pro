package com.android.settings;

import android.app.backup.IBackupManager;
import android.app.backup.IBackupManager.Stub;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings.Secure;
import android.util.Log;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISettingsMiscExt;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PrivacySettings extends SettingsPreferenceFragment implements Indexable {
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new PrivacySearchIndexProvider();
    private SwitchPreference mAutoRestore;
    private PreferenceScreen mBackup;
    private IBackupManager mBackupManager;
    private PreferenceScreen mConfigure;
    private boolean mEnabled;
    private ISettingsMiscExt mExt;
    private OnPreferenceChangeListener preferenceChangeListener = new C01571();

    class C01571 implements OnPreferenceChangeListener {
        C01571() {
        }

        public boolean onPreferenceChange(Preference preference, Object newValue) {
            boolean z = true;
            if (!(preference instanceof SwitchPreference)) {
                return true;
            }
            boolean nextValue = ((Boolean) newValue).booleanValue();
            boolean result = false;
            if (preference == PrivacySettings.this.mAutoRestore) {
                try {
                    PrivacySettings.this.mBackupManager.setAutoRestore(nextValue);
                    result = true;
                } catch (RemoteException e) {
                    SwitchPreference -get0 = PrivacySettings.this.mAutoRestore;
                    if (nextValue) {
                        z = false;
                    }
                    -get0.setChecked(z);
                }
            }
            return result;
        }
    }

    private static class PrivacySearchIndexProvider extends BaseSearchIndexProvider {
        boolean mIsPrimary;

        public PrivacySearchIndexProvider() {
            boolean z = false;
            if (UserHandle.myUserId() == 0) {
                z = true;
            }
            this.mIsPrimary = z;
        }

        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
            List<SearchIndexableResource> result = new ArrayList();
            if (!this.mIsPrimary) {
                return result;
            }
            SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.privacy_settings;
            result.add(sir);
            return result;
        }

        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonVisibleKeys = new ArrayList();
            PrivacySettings.getNonVisibleKeys(context, nonVisibleKeys);
            return nonVisibleKeys;
        }
    }

    protected int getMetricsCategory() {
        return 81;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mEnabled = Process.myUserHandle().isOwner();
        if (this.mEnabled) {
            addPreferencesFromResource(R.xml.privacy_settings);
            PreferenceScreen screen = getPreferenceScreen();
            this.mBackupManager = Stub.asInterface(ServiceManager.getService("backup"));
            this.mBackup = (PreferenceScreen) screen.findPreference("backup_data");
            this.mAutoRestore = (SwitchPreference) screen.findPreference("auto_restore");
            this.mAutoRestore.setOnPreferenceChangeListener(this.preferenceChangeListener);
            this.mConfigure = (PreferenceScreen) screen.findPreference("configure_account");
            this.mExt = UtilsExt.getMiscPlugin(getActivity());
            this.mExt.setFactoryResetTitle(getActivity());
            Set<String> keysToRemove = new HashSet();
            getNonVisibleKeys(getActivity(), keysToRemove);
            for (int i = screen.getPreferenceCount() - 1; i >= 0; i--) {
                Preference preference = screen.getPreference(i);
                if (keysToRemove.contains(preference.getKey())) {
                    screen.removePreference(preference);
                }
            }
            updateToggles();
            if (!FeatureOption.MTK_DRM_APP) {
                screen.removePreference(findPreference("drm_settings"));
            }
        }
    }

    public void onResume() {
        super.onResume();
        if (this.mEnabled) {
            updateToggles();
        }
    }

    private void updateToggles() {
        boolean z = true;
        ContentResolver res = getContentResolver();
        boolean z2 = false;
        Intent intent = null;
        String str = null;
        try {
            int i;
            z2 = this.mBackupManager.isBackupEnabled();
            String transport = this.mBackupManager.getCurrentTransport();
            intent = this.mBackupManager.getConfigurationIntent(transport);
            str = this.mBackupManager.getDestinationString(transport);
            PreferenceScreen preferenceScreen = this.mBackup;
            if (z2) {
                i = R.string.accessibility_feature_state_on;
            } else {
                i = R.string.accessibility_feature_state_off;
            }
            preferenceScreen.setSummary(i);
        } catch (RemoteException e) {
            this.mBackup.setEnabled(false);
        }
        SwitchPreference switchPreference = this.mAutoRestore;
        if (Secure.getInt(res, "backup_auto_restore", 1) != 1) {
            z = false;
        }
        switchPreference.setChecked(z);
        this.mAutoRestore.setEnabled(z2);
        this.mConfigure.setEnabled(intent != null ? z2 : false);
        this.mConfigure.setIntent(intent);
        setConfigureSummary(str);
    }

    private void setConfigureSummary(String summary) {
        if (summary != null) {
            this.mConfigure.setSummary(summary);
        } else {
            this.mConfigure.setSummary(R.string.backup_configure_account_default_summary);
        }
    }

    protected int getHelpResource() {
        return R.string.help_url_backup_reset;
    }

    private static void getNonVisibleKeys(Context context, Collection<String> nonVisibleKeys) {
        boolean isServiceActive = false;
        try {
            isServiceActive = Stub.asInterface(ServiceManager.getService("backup")).isBackupServiceActive(UserHandle.myUserId());
        } catch (RemoteException e) {
            Log.w("PrivacySettings", "Failed querying backup manager service activity status. Assuming it is inactive.");
        }
        boolean vendorSpecific = context.getPackageManager().resolveContentProvider("com.google.settings", 0) == null;
        if (vendorSpecific || r2) {
            nonVisibleKeys.add("backup_inactive");
        }
        if (vendorSpecific || !r2) {
            nonVisibleKeys.add("backup_data");
            nonVisibleKeys.add("auto_restore");
            nonVisibleKeys.add("configure_account");
        }
        if (UserManager.get(context).hasUserRestriction("no_factory_reset")) {
            nonVisibleKeys.add("factory_reset");
        }
        if (UserManager.get(context).hasUserRestriction("no_network_reset")) {
            nonVisibleKeys.add("network_reset");
        }
    }
}
