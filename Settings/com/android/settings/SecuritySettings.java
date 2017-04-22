package com.android.settings;

import android.app.AlertDialog.Builder;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableData;
import android.provider.SearchIndexableResource;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.security.KeyStore;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ListView;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.TrustAgentUtils.TrustAgentComponentInfo;
import com.android.settings.fingerprint.FingerprintEnrollIntroduction;
import com.android.settings.fingerprint.FingerprintSettings;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.search.SearchIndexableRaw;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IDataProtectionExt;
import com.mediatek.settings.ext.IMdmPermissionControlExt;
import com.mediatek.settings.ext.IPermissionControlExt;
import com.mediatek.settings.ext.IPplSettingsEntryExt;
import com.mediatek.settings.ext.ISettingsMiscExt;
import java.util.ArrayList;
import java.util.List;

public class SecuritySettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener, OnClickListener, Indexable {
    private static final int MY_USER_ID = UserHandle.myUserId();
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new SecuritySearchIndexProvider();
    private static final String[] SWITCH_PREFERENCE_KEYS = new String[]{"lock_after_timeout", "visiblepattern", "power_button_instantly_locks", "show_password", "toggle_install_applications"};
    private static final Intent TRUST_AGENT_INTENT = new Intent("android.service.trust.TrustAgentService");
    private static IPermissionControlExt mPermCtrlExt;
    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private DevicePolicyManager mDPM;
    private IDataProtectionExt mDataProectExt;
    private ISettingsMiscExt mExt;
    private boolean mIsPrimary;
    private KeyStore mKeyStore;
    private ListPreference mLockAfter;
    private LockPatternUtils mLockPatternUtils;
    private IMdmPermissionControlExt mMdmPermCtrlExt;
    private Preference mOwnerInfoPref;
    private SwitchPreference mPowerButtonInstantlyLocks;
    private IPplSettingsEntryExt mPplExt;
    private Preference mResetCredentials;
    private Handler mScrollHandler = new Handler();
    private Runnable mScrollRunner = new C01791();
    private boolean mScrollToUnknownSources;
    private SwitchPreference mShowPassword;
    private SubscriptionManager mSubscriptionManager;
    private SwitchPreference mToggleAppInstallation;
    private Intent mTrustAgentClickIntent;
    private int mUnknownSourcesPosition;
    private SwitchPreference mVisiblePattern;
    private DialogInterface mWarnInstallApps;

    class C01791 implements Runnable {
        C01791() {
        }

        public void run() {
            ListView listView = SecuritySettings.this.getListView();
            listView.setItemChecked(SecuritySettings.this.mUnknownSourcesPosition - 1, true);
            listView.smoothScrollToPosition(SecuritySettings.this.mUnknownSourcesPosition - 1);
        }
    }

    class C01802 implements OnPreferenceClickListener {
        C01802() {
        }

        public boolean onPreferenceClick(Preference preference) {
            OwnerInfoSettings.show(SecuritySettings.this);
            return true;
        }
    }

    private static class SecuritySearchIndexProvider extends BaseSearchIndexProvider {
        boolean mIsPrimary;

        public SecuritySearchIndexProvider() {
            boolean z = false;
            if (SecuritySettings.MY_USER_ID == 0) {
                z = true;
            }
            this.mIsPrimary = z;
        }

        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
            List<SearchIndexableResource> result = new ArrayList();
            int resId = SecuritySettings.getResIdForLockUnlockScreen(context, new LockPatternUtils(context));
            SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = resId;
            result.add(sir);
            if (this.mIsPrimary) {
                switch (((DevicePolicyManager) context.getSystemService("device_policy")).getStorageEncryptionStatus()) {
                    case 1:
                        resId = R.xml.security_settings_unencrypted;
                        break;
                    case 3:
                        resId = R.xml.security_settings_encrypted;
                        break;
                }
                sir = new SearchIndexableResource(context);
                sir.xmlResId = resId;
                result.add(sir);
            }
            sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.security_settings_misc;
            result.add(sir);
            return result;
        }

        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> result = new ArrayList();
            Resources res = context.getResources();
            String screenTitle = res.getString(R.string.security_settings_title);
            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = screenTitle;
            data.screenTitle = screenTitle;
            result.add(data);
            if (!this.mIsPrimary) {
                int resId = UserManager.get(context).isLinkedUser() ? R.string.profile_info_settings_title : R.string.user_info_settings_title;
                data = new SearchIndexableRaw(context);
                data.title = res.getString(resId);
                data.screenTitle = screenTitle;
                result.add(data);
            }
            if (((FingerprintManager) context.getSystemService("fingerprint")).isHardwareDetected()) {
                data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.security_settings_fingerprint_preference_title);
                data.screenTitle = screenTitle;
                result.add(data);
                data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.fingerprint_manage_category_title);
                data.screenTitle = screenTitle;
                result.add(data);
            }
            if (!((UserManager) context.getSystemService("user")).hasUserRestriction("no_config_credentials")) {
                int storageSummaryRes;
                if (KeyStore.getInstance().isHardwareBacked()) {
                    storageSummaryRes = R.string.credential_storage_type_hardware;
                } else {
                    storageSummaryRes = R.string.credential_storage_type_software;
                }
                data = new SearchIndexableRaw(context);
                data.title = res.getString(storageSummaryRes);
                data.screenTitle = screenTitle;
                result.add(data);
            }
            LockPatternUtils lockPatternUtils = new LockPatternUtils(context);
            if (lockPatternUtils.isSecure(SecuritySettings.MY_USER_ID)) {
                ArrayList<TrustAgentComponentInfo> agents = SecuritySettings.getActiveTrustAgents(context.getPackageManager(), lockPatternUtils, (DevicePolicyManager) context.getSystemService(DevicePolicyManager.class));
                for (int i = 0; i < agents.size(); i++) {
                    TrustAgentComponentInfo agent = (TrustAgentComponentInfo) agents.get(i);
                    data = new SearchIndexableRaw(context);
                    data.title = agent.title;
                    data.screenTitle = screenTitle;
                    result.add(data);
                }
            }
            if (SecuritySettings.mPermCtrlExt == null) {
                Log.d("SecuritySettings", "mPermCtrlExt init firstly");
                SecuritySettings.mPermCtrlExt = UtilsExt.getPermControlExtPlugin(context);
            }
            List<SearchIndexableData> permList = SecuritySettings.mPermCtrlExt.getRawDataToIndex(enabled);
            Log.d("SecuritySettings", "permList = " + permList);
            if (permList != null) {
                for (SearchIndexableData permdata : permList) {
                    Log.d("SecuritySettings", "add perm data ");
                    SearchIndexableRaw indexablePerm = new SearchIndexableRaw(context);
                    String orign = permdata.toString();
                    String title = orign.substring(orign.indexOf("title:") + "title:".length());
                    Log.d("SecuritySettings", " title = " + title);
                    indexablePerm.title = title;
                    indexablePerm.intentAction = "com.mediatek.security.PERMISSION_CONTROL";
                    result.add(indexablePerm);
                }
            }
            return result;
        }

        public List<String> getNonIndexableKeys(Context context) {
            List<String> keys = new ArrayList();
            LockPatternUtils lockPatternUtils = new LockPatternUtils(context);
            int resId = SecuritySettings.getResIdForLockUnlockScreen(context, lockPatternUtils);
            TelephonyManager tm = TelephonyManager.getDefault();
            if (!(this.mIsPrimary && tm.hasIccCard())) {
                keys.add("sim_lock");
            }
            if (((UserManager) context.getSystemService("user")).hasUserRestriction("no_config_credentials")) {
                keys.add("credentials_management");
            }
            if (!lockPatternUtils.isSecure(SecuritySettings.MY_USER_ID)) {
                keys.add("trust_agent");
                keys.add("manage_trust_agents");
            }
            return keys;
        }
    }

    protected int getMetricsCategory() {
        return 87;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mSubscriptionManager = SubscriptionManager.from(getActivity());
        this.mLockPatternUtils = new LockPatternUtils(getActivity());
        this.mDPM = (DevicePolicyManager) getSystemService("device_policy");
        this.mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
        if (savedInstanceState != null && savedInstanceState.containsKey("trust_agent_click_intent")) {
            this.mTrustAgentClickIntent = (Intent) savedInstanceState.getParcelable("trust_agent_click_intent");
        }
        setWhetherNeedScroll();
        initPlugin();
    }

    private static int getResIdForLockUnlockScreen(Context context, LockPatternUtils lockPatternUtils) {
        if (lockPatternUtils.isSecure(MY_USER_ID)) {
            if (lockPatternUtils.usingVoiceWeak()) {
                return R.xml.security_settings_voice_weak;
            }
            switch (lockPatternUtils.getKeyguardStoredPasswordQuality(MY_USER_ID)) {
                case 65536:
                    return R.xml.security_settings_pattern;
                case 131072:
                case 196608:
                    return R.xml.security_settings_pin;
                case 262144:
                case 327680:
                case 393216:
                    return R.xml.security_settings_password;
                default:
                    return 0;
            }
        } else if (lockPatternUtils.isLockScreenDisabled(MY_USER_ID)) {
            return R.xml.security_settings_lockscreen;
        } else {
            return R.xml.security_settings_chooser;
        }
    }

    private PreferenceScreen createPreferenceHierarchy() {
        ApplicationInfo applicationInfo;
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.security_settings);
        root = getPreferenceScreen();
        int resid = getResIdForLockUnlockScreen(getActivity(), this.mLockPatternUtils);
        addPreferencesFromResource(resid);
        this.mIsPrimary = MY_USER_ID == 0;
        this.mOwnerInfoPref = findPreference("owner_info_settings");
        if (this.mOwnerInfoPref != null) {
            this.mOwnerInfoPref.setOnPreferenceClickListener(new C01802());
        }
        boolean isEMMC = FeatureOption.MTK_EMMC_SUPPORT && !FeatureOption.MTK_CACHE_MERGE_SUPPORT;
        if (FeatureOption.MTK_NAND_FTL_SUPPORT || (isEMMC && this.mIsPrimary)) {
            if (LockPatternUtils.isDeviceEncryptionEnabled()) {
                addPreferencesFromResource(R.xml.security_settings_encrypted);
            } else {
                addPreferencesFromResource(R.xml.security_settings_unencrypted);
            }
        }
        PreferenceGroup securityCategory = (PreferenceGroup) root.findPreference("security_category");
        if (securityCategory != null) {
            maybeAddFingerprintPreference(securityCategory);
            addTrustAgentSettings(securityCategory);
        }
        this.mLockAfter = (ListPreference) root.findPreference("lock_after_timeout");
        if (this.mLockAfter != null) {
            setupLockAfterPreference();
            updateLockAfterPreferenceSummary();
        }
        this.mVisiblePattern = (SwitchPreference) root.findPreference("visiblepattern");
        this.mPowerButtonInstantlyLocks = (SwitchPreference) root.findPreference("power_button_instantly_locks");
        Preference trustAgentPreference = root.findPreference("trust_agent");
        if (!(this.mPowerButtonInstantlyLocks == null || trustAgentPreference == null || trustAgentPreference.getTitle().length() <= 0)) {
            this.mPowerButtonInstantlyLocks.setSummary(getString(R.string.lockpattern_settings_power_button_instantly_locks_summary, new Object[]{trustAgentPreference.getTitle()}));
        }
        if (!(resid != R.xml.security_settings_voice_weak || this.mLockPatternUtils.getKeyguardStoredPasswordQuality(MY_USER_ID) == 65536 || securityCategory == null || this.mVisiblePattern == null)) {
            securityCategory.removePreference(root.findPreference("visiblepattern"));
        }
        addPreferencesFromResource(R.xml.security_settings_misc);
        changeSimTitle();
        TelephonyManager tm = TelephonyManager.getDefault();
        PersistableBundle b = ((CarrierConfigManager) getActivity().getSystemService("carrier_config")).getConfig();
        if (this.mIsPrimary && isSimIccReady() && !b.getBoolean("hide_sim_lock_settings_bool")) {
            root.findPreference("sim_lock").setEnabled(isSimReady());
        } else {
            root.removePreference(root.findPreference("sim_lock"));
        }
        if (System.getInt(getContentResolver(), "lock_to_app_enabled", 0) != 0) {
            root.findPreference("screen_pinning_settings").setSummary(getResources().getString(R.string.switch_on_text));
        }
        this.mShowPassword = (SwitchPreference) root.findPreference("show_password");
        this.mResetCredentials = root.findPreference("credentials_reset");
        UserManager um = (UserManager) getActivity().getSystemService("user");
        this.mKeyStore = KeyStore.getInstance();
        if (um.hasUserRestriction("no_config_credentials")) {
            PreferenceGroup credentialsManager = (PreferenceGroup) root.findPreference("credentials_management");
            credentialsManager.removePreference(root.findPreference("credentials_reset"));
            credentialsManager.removePreference(root.findPreference("credentials_install"));
            credentialsManager.removePreference(root.findPreference("credential_storage_type"));
        } else {
            int storageSummaryRes;
            Preference credentialStorageType = root.findPreference("credential_storage_type");
            if (this.mKeyStore.isHardwareBacked()) {
                storageSummaryRes = R.string.credential_storage_type_hardware;
            } else {
                storageSummaryRes = R.string.credential_storage_type_software;
            }
            credentialStorageType.setSummary(storageSummaryRes);
        }
        PreferenceGroup deviceAdminCategory = (PreferenceGroup) root.findPreference("device_admin_category");
        this.mToggleAppInstallation = (SwitchPreference) findPreference("toggle_install_applications");
        this.mToggleAppInstallation.setChecked(isNonMarketAppsAllowed());
        this.mToggleAppInstallation.setEnabled(!um.getUserInfo(MY_USER_ID).isRestricted());
        if (um.hasUserRestriction("no_install_unknown_sources") || um.hasUserRestriction("no_install_apps")) {
            this.mToggleAppInstallation.setEnabled(false);
        }
        PreferenceGroup advancedCategory = (PreferenceGroup) root.findPreference("advanced_security");
        if (advancedCategory != null) {
            Preference manageAgents = advancedCategory.findPreference("manage_trust_agents");
            if (!(manageAgents == null || this.mLockPatternUtils.isSecure(MY_USER_ID))) {
                manageAgents.setEnabled(false);
                manageAgents.setSummary(R.string.disabled_because_no_backup_security);
            }
        }
        Index.getInstance(getActivity()).updateFromClassNameResource(SecuritySettings.class.getName(), true, true);
        for (String findPreference : SWITCH_PREFERENCE_KEYS) {
            Preference pref = findPreference(findPreference);
            if (pref != null) {
                pref.setOnPreferenceChangeListener(this);
            }
        }
        addPluginEntrance(deviceAdminCategory);
        try {
            applicationInfo = getPackageManager().getApplicationInfo("net.argusmobile.argus", 0);
        } catch (Throwable th) {
            applicationInfo = null;
        }
        if (applicationInfo == null) {
            deviceAdminCategory.removePreference(root.findPreference("argus_anti_theft"));
        }
        return root;
    }

    private void maybeAddFingerprintPreference(PreferenceGroup securityCategory) {
        FingerprintManager fpm = (FingerprintManager) getActivity().getSystemService("fingerprint");
        if (fpm.isHardwareDetected()) {
            String clazz;
            Preference fingerprintPreference = new Preference(securityCategory.getContext());
            fingerprintPreference.setKey("fingerprint_settings");
            fingerprintPreference.setTitle(R.string.security_settings_fingerprint_preference_title);
            Intent intent = new Intent();
            List<Fingerprint> items = fpm.getEnrolledFingerprints();
            int fingerprintCount = items != null ? items.size() : 0;
            if (fingerprintCount > 0) {
                fingerprintPreference.setSummary(getResources().getQuantityString(R.plurals.security_settings_fingerprint_preference_summary, fingerprintCount, new Object[]{Integer.valueOf(fingerprintCount)}));
                clazz = FingerprintSettings.class.getName();
            } else {
                fingerprintPreference.setSummary(R.string.security_settings_fingerprint_preference_summary_none);
                clazz = FingerprintEnrollIntroduction.class.getName();
            }
            intent.setClassName("com.android.settings", clazz);
            fingerprintPreference.setIntent(intent);
            return;
        }
        Log.v("SecuritySettings", "No fingerprint hardware detected!!");
    }

    private void addTrustAgentSettings(PreferenceGroup securityCategory) {
        boolean hasSecurity = this.mLockPatternUtils.isSecure(MY_USER_ID);
        ArrayList<TrustAgentComponentInfo> agents = getActiveTrustAgents(getPackageManager(), this.mLockPatternUtils, this.mDPM);
        for (int i = 0; i < agents.size(); i++) {
            TrustAgentComponentInfo agent = (TrustAgentComponentInfo) agents.get(i);
            Preference trustAgentPreference = new Preference(securityCategory.getContext());
            trustAgentPreference.setKey("trust_agent");
            trustAgentPreference.setTitle(agent.title);
            trustAgentPreference.setSummary(agent.summary);
            Intent intent = new Intent();
            intent.setComponent(agent.componentName);
            intent.setAction("android.intent.action.MAIN");
            trustAgentPreference.setIntent(intent);
            securityCategory.addPreference(trustAgentPreference);
            if (agent.disabledByAdministrator) {
                trustAgentPreference.setEnabled(false);
                trustAgentPreference.setSummary(R.string.trust_agent_disabled_device_admin);
            } else if (!hasSecurity) {
                trustAgentPreference.setEnabled(false);
                trustAgentPreference.setSummary(R.string.disabled_because_no_backup_security);
            }
        }
    }

    private boolean isSimIccReady() {
        TelephonyManager tm = TelephonyManager.getDefault();
        List<SubscriptionInfo> subInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subInfoList != null) {
            for (SubscriptionInfo subInfo : subInfoList) {
                if (tm.hasIccCard(subInfo.getSimSlotIndex())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSimReady() {
        List<SubscriptionInfo> subInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subInfoList != null) {
            for (SubscriptionInfo subInfo : subInfoList) {
                int simState = TelephonyManager.getDefault().getSimState(subInfo.getSimSlotIndex());
                if (simState != 1 && simState != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ArrayList<TrustAgentComponentInfo> getActiveTrustAgents(PackageManager pm, LockPatternUtils utils, DevicePolicyManager dpm) {
        ArrayList<TrustAgentComponentInfo> result = new ArrayList();
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(TRUST_AGENT_INTENT, 128);
        List<ComponentName> enabledTrustAgents = utils.getEnabledTrustAgents(MY_USER_ID);
        boolean disableTrustAgents = (dpm.getKeyguardDisabledFeatures(null) & 16) != 0;
        if (!(enabledTrustAgents == null || enabledTrustAgents.isEmpty())) {
            for (int i = 0; i < resolveInfos.size(); i++) {
                ResolveInfo resolveInfo = (ResolveInfo) resolveInfos.get(i);
                if (resolveInfo.serviceInfo != null && TrustAgentUtils.checkProvidePermission(resolveInfo, pm)) {
                    TrustAgentComponentInfo trustAgentComponentInfo = TrustAgentUtils.getSettingsComponent(pm, resolveInfo);
                    if (!(trustAgentComponentInfo.componentName == null || !enabledTrustAgents.contains(TrustAgentUtils.getComponentName(resolveInfo)) || TextUtils.isEmpty(trustAgentComponentInfo.title))) {
                        if (disableTrustAgents && dpm.getTrustAgentConfiguration(null, TrustAgentUtils.getComponentName(resolveInfo)) == null) {
                            trustAgentComponentInfo.disabledByAdministrator = true;
                        }
                        result.add(trustAgentComponentInfo);
                    }
                }
            }
        }
        return result;
    }

    private boolean isNonMarketAppsAllowed() {
        return Global.getInt(getContentResolver(), "install_non_market_apps", 0) > 0;
    }

    private void setNonMarketAppsAllowed(boolean enabled) {
        if (!((UserManager) getActivity().getSystemService("user")).hasUserRestriction("no_install_unknown_sources")) {
            Global.putInt(getContentResolver(), "install_non_market_apps", enabled ? 1 : 0);
        }
    }

    private void warnAppInstallation() {
        this.mWarnInstallApps = new Builder(getActivity()).setTitle(getResources().getString(R.string.error_title)).setIcon(17301543).setMessage(getResources().getString(R.string.install_all_warning)).setPositiveButton(17039379, this).setNegativeButton(17039369, this).show();
    }

    public void onClick(DialogInterface dialog, int which) {
        if (dialog == this.mWarnInstallApps) {
            boolean turnOn = which == -1;
            setNonMarketAppsAllowed(turnOn);
            if (this.mToggleAppInstallation != null) {
                this.mToggleAppInstallation.setChecked(turnOn);
            }
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (this.mWarnInstallApps != null) {
            this.mWarnInstallApps.dismiss();
        }
    }

    private void setupLockAfterPreference() {
        this.mLockAfter.setValue(String.valueOf(Secure.getLong(getContentResolver(), "lock_screen_lock_after_timeout", 5000)));
        this.mLockAfter.setOnPreferenceChangeListener(this);
        long adminTimeout = this.mDPM != null ? this.mDPM.getMaximumTimeToLock(null) : 0;
        long displayTimeout = (long) Math.max(0, System.getInt(getContentResolver(), "screen_off_timeout", 0));
        if (adminTimeout > 0) {
            disableUnusableTimeouts(Math.max(0, adminTimeout - displayTimeout));
        }
    }

    private void updateLockAfterPreferenceSummary() {
        long currentTimeout = Secure.getLong(getContentResolver(), "lock_screen_lock_after_timeout", 5000);
        CharSequence[] entries = this.mLockAfter.getEntries();
        CharSequence[] values = this.mLockAfter.getEntryValues();
        int best = 0;
        for (int i = 0; i < values.length; i++) {
            if (currentTimeout >= Long.valueOf(values[i].toString()).longValue()) {
                best = i;
            }
        }
        Preference preference = getPreferenceScreen().findPreference("trust_agent");
        if (preference == null || preference.getTitle().length() <= 0) {
            this.mLockAfter.setSummary(getString(R.string.lock_after_timeout_summary, new Object[]{entries[best]}));
        } else if (Long.valueOf(values[best].toString()).longValue() == 0) {
            this.mLockAfter.setSummary(getString(R.string.lock_immediately_summary_with_exception, new Object[]{preference.getTitle()}));
        } else {
            this.mLockAfter.setSummary(getString(R.string.lock_after_timeout_summary_with_exception, new Object[]{entries[best], preference.getTitle()}));
        }
        this.mLockAfter.setValue(String.valueOf(currentTimeout));
    }

    private void disableUnusableTimeouts(long maxTimeout) {
        boolean z;
        CharSequence[] entries = this.mLockAfter.getEntries();
        CharSequence[] values = this.mLockAfter.getEntryValues();
        ArrayList<CharSequence> revisedEntries = new ArrayList();
        ArrayList<CharSequence> revisedValues = new ArrayList();
        for (int i = 0; i < values.length; i++) {
            if (Long.valueOf(values[i].toString()).longValue() <= maxTimeout) {
                revisedEntries.add(entries[i]);
                revisedValues.add(values[i]);
            }
        }
        if (!(revisedEntries.size() == entries.length && revisedValues.size() == values.length)) {
            this.mLockAfter.setEntries((CharSequence[]) revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
            this.mLockAfter.setEntryValues((CharSequence[]) revisedValues.toArray(new CharSequence[revisedValues.size()]));
            int userPreference = Integer.valueOf(this.mLockAfter.getValue()).intValue();
            if (((long) userPreference) <= maxTimeout) {
                this.mLockAfter.setValue(String.valueOf(userPreference));
            }
        }
        ListPreference listPreference = this.mLockAfter;
        if (revisedEntries.size() > 0) {
            z = true;
        } else {
            z = false;
        }
        listPreference.setEnabled(z);
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.mTrustAgentClickIntent != null) {
            outState.putParcelable("trust_agent_click_intent", this.mTrustAgentClickIntent);
        }
    }

    public void onResume() {
        boolean z = false;
        super.onResume();
        createPreferenceHierarchy();
        LockPatternUtils lockPatternUtils = this.mChooseLockSettingsHelper.utils();
        if (this.mVisiblePattern != null) {
            this.mVisiblePattern.setChecked(lockPatternUtils.isVisiblePatternEnabled(MY_USER_ID));
        }
        if (this.mPowerButtonInstantlyLocks != null) {
            this.mPowerButtonInstantlyLocks.setChecked(lockPatternUtils.getPowerButtonInstantlyLocks(MY_USER_ID));
        }
        if (this.mShowPassword != null) {
            this.mShowPassword.setChecked(System.getInt(getContentResolver(), "show_password", 1) != 0);
        }
        if (this.mResetCredentials != null) {
            Preference preference = this.mResetCredentials;
            if (!this.mKeyStore.isEmpty()) {
                z = true;
            }
            preference.setEnabled(z);
        }
        updateOwnerInfo();
        ScrollToUnknownSources();
    }

    public void updateOwnerInfo() {
        if (this.mOwnerInfoPref != null) {
            CharSequence ownerInfo;
            Preference preference = this.mOwnerInfoPref;
            if (this.mLockPatternUtils.isOwnerInfoEnabled(MY_USER_ID)) {
                ownerInfo = this.mLockPatternUtils.getOwnerInfo(MY_USER_ID);
            } else {
                ownerInfo = getString(R.string.owner_info_settings_summary);
            }
            preference.setSummary(ownerInfo);
        }
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();
        if ("unlock_set_or_change".equals(key)) {
            startFragment(this, "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment", R.string.lock_settings_picker_title, 123, null);
        } else if ("trust_agent".equals(key)) {
            ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(getActivity(), this);
            this.mTrustAgentClickIntent = preference.getIntent();
            if (!(helper.launchConfirmationActivity(126, preference.getTitle()) || this.mTrustAgentClickIntent == null)) {
                startActivity(this.mTrustAgentClickIntent);
                this.mTrustAgentClickIntent = null;
            }
        } else if (!"argus_anti_theft".equals(key)) {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        } else {
            try {
                Intent intent = new Intent();
                intent.setClassName("net.argusmobile.argus", "net.argusmobile.argus.activities.MainActivity");
                intent.setAction("android.intent.action.VIEW");
                startActivity(intent);
            } catch (Throwable th) {
            }
        }
        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 126 && resultCode == -1) {
            if (this.mTrustAgentClickIntent != null) {
                startActivity(this.mTrustAgentClickIntent);
                this.mTrustAgentClickIntent = null;
            }
            return;
        }
        createPreferenceHierarchy();
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        String key = preference.getKey();
        LockPatternUtils lockPatternUtils = this.mChooseLockSettingsHelper.utils();
        if ("lock_after_timeout".equals(key)) {
            try {
                Secure.putInt(getContentResolver(), "lock_screen_lock_after_timeout", Integer.parseInt((String) value));
            } catch (NumberFormatException e) {
                Log.e("SecuritySettings", "could not persist lockAfter timeout setting", e);
            }
            updateLockAfterPreferenceSummary();
            return true;
        } else if ("visiblepattern".equals(key)) {
            lockPatternUtils.setVisiblePatternEnabled(((Boolean) value).booleanValue(), MY_USER_ID);
            return true;
        } else if ("power_button_instantly_locks".equals(key)) {
            this.mLockPatternUtils.setPowerButtonInstantlyLocks(((Boolean) value).booleanValue(), MY_USER_ID);
            return true;
        } else if ("show_password".equals(key)) {
            int i;
            ContentResolver contentResolver = getContentResolver();
            String str = "show_password";
            if (((Boolean) value).booleanValue()) {
                i = 1;
            } else {
                i = 0;
            }
            System.putInt(contentResolver, str, i);
            lockPatternUtils.setVisiblePasswordEnabled(((Boolean) value).booleanValue(), MY_USER_ID);
            return true;
        } else if (!"toggle_install_applications".equals(key)) {
            return true;
        } else {
            if (((Boolean) value).booleanValue()) {
                this.mToggleAppInstallation.setChecked(false);
                warnAppInstallation();
                return false;
            }
            setNonMarketAppsAllowed(false);
            return true;
        }
    }

    protected int getHelpResource() {
        return R.string.help_url_security;
    }

    private void setWhetherNeedScroll() {
        if ("android.settings.SECURITY_SETTINGS".equals(getActivity().getIntent().getAction())) {
            this.mScrollToUnknownSources = true;
        }
    }

    private void ScrollToUnknownSources() {
        if (this.mScrollToUnknownSources) {
            this.mScrollToUnknownSources = false;
            this.mUnknownSourcesPosition = 0;
            findPreferencePosition("toggle_install_applications", getPreferenceScreen());
            this.mScrollHandler.postDelayed(this.mScrollRunner, 500);
        }
    }

    private Preference findPreferencePosition(CharSequence key, PreferenceGroup root) {
        if (key.equals(root.getKey())) {
            return root;
        }
        this.mUnknownSourcesPosition++;
        int preferenceCount = root.getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            Preference preference = root.getPreference(i);
            String curKey = preference.getKey();
            if (curKey != null && curKey.equals(key)) {
                return preference;
            }
            if (preference instanceof PreferenceGroup) {
                Preference returnedPreference = findPreferencePosition(key, (PreferenceGroup) preference);
                if (returnedPreference != null) {
                    return returnedPreference;
                }
            } else {
                this.mUnknownSourcesPosition++;
            }
        }
        return null;
    }

    public void onDestroyView() {
        super.onDestroyView();
        this.mScrollHandler.removeCallbacks(this.mScrollRunner);
    }

    private void initPlugin() {
        mPermCtrlExt = UtilsExt.getPermControlExtPlugin(getActivity());
        this.mPplExt = UtilsExt.getPrivacyProtectionLockExtPlugin(getActivity());
        this.mMdmPermCtrlExt = UtilsExt.getMdmPermControlExtPlugin(getActivity());
        this.mDataProectExt = UtilsExt.getDataProectExtPlugin(getActivity());
        this.mExt = UtilsExt.getMiscPlugin(getActivity());
    }

    private void addPluginEntrance(PreferenceGroup deviceAdminCategory) {
        mPermCtrlExt.addAutoBootPrf(deviceAdminCategory);
        mPermCtrlExt.addPermSwitchPrf(deviceAdminCategory);
        mPermCtrlExt.enablerResume();
        this.mDataProectExt.addDataPrf(deviceAdminCategory);
        this.mPplExt.addPplPrf(deviceAdminCategory);
        this.mPplExt.enablerResume();
        this.mMdmPermCtrlExt.addMdmPermCtrlPrf(deviceAdminCategory);
    }

    public void onPause() {
        super.onPause();
        mPermCtrlExt.enablerPause();
        this.mPplExt.enablerPause();
    }

    private void changeSimTitle() {
        findPreference("sim_lock").setTitle(this.mExt.customizeSimDisplayString(findPreference("sim_lock").getTitle().toString(), -1));
        findPreference("sim_lock_settings").setTitle(this.mExt.customizeSimDisplayString(findPreference("sim_lock_settings").getTitle().toString(), -1));
    }
}
