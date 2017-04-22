package com.android.settings.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManagerNative;
import android.app.AlertDialog.Builder;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.text.TextUtils.SimpleStringSplitter;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.content.PackageMonitor;
import com.android.internal.view.RotationPolicy;
import com.android.internal.view.RotationPolicy.RotationPolicyListener;
import com.android.settings.DialogCreatable;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.search.SearchIndexableRaw;
import com.mediatek.settings.FeatureOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AccessibilitySettings extends SettingsPreferenceFragment implements DialogCreatable, OnPreferenceChangeListener, Indexable {
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new C02155();
    static final Set<ComponentName> sInstalledServices = new HashSet();
    static final SimpleStringSplitter sStringColonSplitter = new SimpleStringSplitter(':');
    private PreferenceScreen mCaptioningPreferenceScreen;
    private final Configuration mCurConfig = new Configuration();
    private PreferenceScreen mDisplayDaltonizerPreferenceScreen;
    private PreferenceScreen mDisplayMagnificationPreferenceScreen;
    private DevicePolicyManager mDpm;
    private PreferenceScreen mGlobalGesturePreferenceScreen;
    private final Handler mHandler = new Handler();
    private SwitchPreference mIpoSetting;
    private float mLargeFontScaleTablet = 1.5f;
    private int mLongPressTimeoutDefault;
    private final Map<String, String> mLongPressTimeoutValuetoTitleMap = new HashMap();
    private Preference mNoServicesMessagePreference;
    private final RotationPolicyListener mRotationPolicyListener = new C02144();
    private ListPreference mSelectLongPressTimeoutPreference;
    private PreferenceCategory mServicesCategory;
    private final SettingsContentObserver mSettingsContentObserver = new SettingsContentObserver(this.mHandler) {
        public void onChange(boolean selfChange, Uri uri) {
            AccessibilitySettings.this.loadInstalledServices();
            AccessibilitySettings.this.updateAllPreferences();
        }
    };
    private final PackageMonitor mSettingsPackageMonitor = new C02122();
    private PreferenceCategory mSystemsCategory;
    private SwitchPreference mToggleHighTextContrastPreference;
    private SwitchPreference mToggleHomeTorchPreference;
    private SwitchPreference mToggleInversionPreference;
    private SwitchPreference mToggleLargeTextPreference;
    private SwitchPreference mToggleLockScreenRotationPreference;
    private SwitchPreference mTogglePowerButtonEndsCallPreference;
    private SwitchPreference mToggleSpeakPasswordPreference;
    private final Runnable mUpdateRunnable = new C02111();

    class C02111 implements Runnable {
        C02111() {
        }

        public void run() {
            AccessibilitySettings.this.loadInstalledServices();
            AccessibilitySettings.this.updateServicesPreferences();
        }
    }

    class C02122 extends PackageMonitor {
        C02122() {
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
            AccessibilitySettings.this.mHandler.postDelayed(AccessibilitySettings.this.mUpdateRunnable, 1000);
        }
    }

    class C02144 extends RotationPolicyListener {
        C02144() {
        }

        public void onChange() {
            AccessibilitySettings.this.updateLockScreenRotationCheckbox();
        }
    }

    static class C02155 extends BaseSearchIndexProvider {
        C02155() {
        }

        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> indexables = new ArrayList();
            PackageManager packageManager = context.getPackageManager();
            AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService("accessibility");
            String screenTitle = context.getResources().getString(R.string.accessibility_services_title);
            List<AccessibilityServiceInfo> services = accessibilityManager.getInstalledAccessibilityServiceList();
            int serviceCount = services.size();
            for (int i = 0; i < serviceCount; i++) {
                AccessibilityServiceInfo service = (AccessibilityServiceInfo) services.get(i);
                if (!(service == null || service.getResolveInfo() == null)) {
                    ServiceInfo serviceInfo = service.getResolveInfo().serviceInfo;
                    ComponentName componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
                    SearchIndexableRaw indexable = new SearchIndexableRaw(context);
                    indexable.key = componentName.flattenToString();
                    indexable.title = service.getResolveInfo().loadLabel(packageManager).toString();
                    indexable.summaryOn = context.getString(R.string.accessibility_feature_state_on);
                    indexable.summaryOff = context.getString(R.string.accessibility_feature_state_off);
                    indexable.screenTitle = screenTitle;
                    indexables.add(indexable);
                }
            }
            return indexables;
        }

        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
            List<SearchIndexableResource> indexables = new ArrayList();
            SearchIndexableResource indexable = new SearchIndexableResource(context);
            indexable.xmlResId = R.xml.accessibility_settings;
            indexables.add(indexable);
            return indexables;
        }
    }

    protected int getMetricsCategory() {
        return 2;
    }

    protected int getHelpResource() {
        return R.string.help_uri_accessibility;
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.accessibility_settings);
        initializeAllPreferences();
        this.mDpm = (DevicePolicyManager) getActivity().getSystemService("device_policy");
    }

    public void onResume() {
        super.onResume();
        loadInstalledServices();
        updateAllPreferences();
        this.mSettingsPackageMonitor.register(getActivity(), getActivity().getMainLooper(), false);
        this.mSettingsContentObserver.register(getContentResolver());
        if (RotationPolicy.isRotationSupported(getActivity())) {
            RotationPolicy.registerRotationPolicyListener(getActivity(), this.mRotationPolicyListener);
        }
    }

    public void onPause() {
        this.mSettingsPackageMonitor.unregister();
        this.mSettingsContentObserver.unregister(getContentResolver());
        if (RotationPolicy.isRotationSupported(getActivity())) {
            RotationPolicy.unregisterRotationPolicyListener(getActivity(), this.mRotationPolicyListener);
        }
        super.onPause();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int i = 0;
        if (this.mSelectLongPressTimeoutPreference == preference) {
            handleLongPressTimeoutPreferenceChange((String) newValue);
            return true;
        } else if (this.mToggleInversionPreference == preference) {
            handleToggleInversionPreferenceChange(((Boolean) newValue).booleanValue());
            return true;
        } else if (this.mIpoSetting == preference) {
            ContentResolver contentResolver = getContentResolver();
            String str = "ipo_setting";
            if (((Boolean) newValue).booleanValue()) {
                i = 1;
            }
            System.putInt(contentResolver, str, i);
            return true;
        } else if (this.mToggleHomeTorchPreference != preference) {
            return false;
        } else {
            SystemProperties.set("persist.sys.home_torch", ((Boolean) newValue).booleanValue() ? "true" : "false");
            if (((Boolean) newValue).booleanValue()) {
                Log.i("fenghaitao", "==mToggleHomeTorchPreference===" + ((Boolean) newValue));
                new Builder(getActivity()).setTitle(R.string.accessibility_toggle_home_torch_preference_title).setMessage(R.string.alertdialog_torch_message).setPositiveButton(R.string.okay, null).create().show();
            }
            return true;
        }
    }

    private void handleLongPressTimeoutPreferenceChange(String stringValue) {
        Secure.putInt(getContentResolver(), "long_press_timeout", Integer.parseInt(stringValue));
        this.mSelectLongPressTimeoutPreference.setSummary((CharSequence) this.mLongPressTimeoutValuetoTitleMap.get(stringValue));
    }

    private void handleToggleInversionPreferenceChange(boolean checked) {
        Secure.putInt(getContentResolver(), "accessibility_display_inversion_enabled", checked ? 1 : 0);
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (this.mToggleLargeTextPreference == preference) {
            handleToggleLargeTextPreferenceClick();
            return true;
        } else if (this.mToggleHighTextContrastPreference == preference) {
            handleToggleTextContrastPreferenceClick();
            return true;
        } else if (this.mTogglePowerButtonEndsCallPreference == preference) {
            handleTogglePowerButtonEndsCallPreferenceClick();
            return true;
        } else if (this.mToggleLockScreenRotationPreference == preference) {
            handleLockScreenRotationPreferenceClick();
            return true;
        } else if (this.mToggleSpeakPasswordPreference == preference) {
            handleToggleSpeakPasswordPreferenceClick();
            return true;
        } else if (this.mGlobalGesturePreferenceScreen == preference) {
            handleToggleEnableAccessibilityGesturePreferenceClick();
            return true;
        } else if (this.mDisplayMagnificationPreferenceScreen != preference) {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        } else {
            handleDisplayMagnificationPreferenceScreenClick();
            return true;
        }
    }

    private void handleToggleLargeTextPreferenceClick() {
        float fontScale = 1.3f;
        if (FeatureOption.MTK_PRODUCT_IS_TABLET) {
            String[] array = getResources().getStringArray(R.array.entryvalues_font_size);
            if (array.length > 0) {
                this.mLargeFontScaleTablet = Float.parseFloat(array[array.length - 1]);
            }
            fontScale = this.mLargeFontScaleTablet;
        }
        try {
            Configuration configuration = this.mCurConfig;
            if (!this.mToggleLargeTextPreference.isChecked()) {
                fontScale = 1.0f;
            }
            configuration.fontScale = fontScale;
            ActivityManagerNative.getDefault().updatePersistentConfiguration(this.mCurConfig);
        } catch (RemoteException e) {
        }
    }

    private void handleToggleTextContrastPreferenceClick() {
        Secure.putInt(getContentResolver(), "high_text_contrast_enabled", this.mToggleHighTextContrastPreference.isChecked() ? 1 : 0);
    }

    private void handleTogglePowerButtonEndsCallPreferenceClick() {
        int i;
        ContentResolver contentResolver = getContentResolver();
        String str = "incall_power_button_behavior";
        if (this.mTogglePowerButtonEndsCallPreference.isChecked()) {
            i = 2;
        } else {
            i = 1;
        }
        Secure.putInt(contentResolver, str, i);
    }

    private void handleLockScreenRotationPreferenceClick() {
        RotationPolicy.setRotationLockForAccessibility(getActivity(), !this.mToggleLockScreenRotationPreference.isChecked());
    }

    private void handleToggleSpeakPasswordPreferenceClick() {
        Secure.putInt(getContentResolver(), "speak_password", this.mToggleSpeakPasswordPreference.isChecked() ? 1 : 0);
    }

    private void handleToggleEnableAccessibilityGesturePreferenceClick() {
        boolean z = true;
        Bundle extras = this.mGlobalGesturePreferenceScreen.getExtras();
        extras.putString("title", getString(R.string.accessibility_global_gesture_preference_title));
        extras.putString("summary", getString(R.string.accessibility_global_gesture_preference_description));
        String str = "checked";
        if (Global.getInt(getContentResolver(), "enable_accessibility_global_gesture_enabled", 0) != 1) {
            z = false;
        }
        extras.putBoolean(str, z);
        super.onPreferenceTreeClick(this.mGlobalGesturePreferenceScreen, this.mGlobalGesturePreferenceScreen);
    }

    private void handleDisplayMagnificationPreferenceScreenClick() {
        boolean z = true;
        Bundle extras = this.mDisplayMagnificationPreferenceScreen.getExtras();
        extras.putString("title", getString(R.string.accessibility_screen_magnification_title));
        extras.putCharSequence("summary", getActivity().getResources().getText(R.string.accessibility_screen_magnification_summary));
        String str = "checked";
        if (Secure.getInt(getContentResolver(), "accessibility_display_magnification_enabled", 0) != 1) {
            z = false;
        }
        extras.putBoolean(str, z);
        super.onPreferenceTreeClick(this.mDisplayMagnificationPreferenceScreen, this.mDisplayMagnificationPreferenceScreen);
    }

    private void initializeAllPreferences() {
        this.mServicesCategory = (PreferenceCategory) findPreference("services_category");
        this.mSystemsCategory = (PreferenceCategory) findPreference("system_category");
        this.mToggleHomeTorchPreference = (SwitchPreference) findPreference("toggle_home_torch_preference");
        this.mToggleHomeTorchPreference.setOnPreferenceChangeListener(this);
        this.mToggleLargeTextPreference = (SwitchPreference) findPreference("toggle_large_text_preference");
        this.mToggleHighTextContrastPreference = (SwitchPreference) findPreference("toggle_high_text_contrast_preference");
        this.mToggleInversionPreference = (SwitchPreference) findPreference("toggle_inversion_preference");
        this.mToggleInversionPreference.setOnPreferenceChangeListener(this);
        this.mTogglePowerButtonEndsCallPreference = (SwitchPreference) findPreference("toggle_power_button_ends_call_preference");
        if (!(KeyCharacterMap.deviceHasKey(26) && Utils.isVoiceCapable(getActivity()))) {
            this.mSystemsCategory.removePreference(this.mTogglePowerButtonEndsCallPreference);
        }
        this.mToggleLockScreenRotationPreference = (SwitchPreference) findPreference("toggle_lock_screen_rotation_preference");
        if (!RotationPolicy.isRotationSupported(getActivity())) {
            this.mSystemsCategory.removePreference(this.mToggleLockScreenRotationPreference);
        }
        this.mSystemsCategory.removePreference(findPreference("screen_float_window_preference"));
        this.mToggleSpeakPasswordPreference = (SwitchPreference) findPreference("toggle_speak_password_preference");
        this.mSelectLongPressTimeoutPreference = (ListPreference) findPreference("select_long_press_timeout_preference");
        this.mSelectLongPressTimeoutPreference.setOnPreferenceChangeListener(this);
        if (this.mLongPressTimeoutValuetoTitleMap.size() == 0) {
            String[] timeoutValues = getResources().getStringArray(R.array.long_press_timeout_selector_values);
            this.mLongPressTimeoutDefault = Integer.parseInt(timeoutValues[0]);
            String[] timeoutTitles = getResources().getStringArray(R.array.long_press_timeout_selector_titles);
            int timeoutValueCount = timeoutValues.length;
            for (int i = 0; i < timeoutValueCount; i++) {
                this.mLongPressTimeoutValuetoTitleMap.put(timeoutValues[i], timeoutTitles[i]);
            }
        }
        this.mCaptioningPreferenceScreen = (PreferenceScreen) findPreference("captioning_preference_screen");
        this.mDisplayMagnificationPreferenceScreen = (PreferenceScreen) findPreference("screen_magnification_preference_screen");
        this.mDisplayDaltonizerPreferenceScreen = (PreferenceScreen) findPreference("daltonizer_preference_screen");
        this.mGlobalGesturePreferenceScreen = (PreferenceScreen) findPreference("enable_global_gesture_preference_screen");
        int longPressOnPowerBehavior = getActivity().getResources().getInteger(17694792);
        if (!(KeyCharacterMap.deviceHasKey(26) && longPressOnPowerBehavior == 1)) {
            this.mSystemsCategory.removePreference(this.mGlobalGesturePreferenceScreen);
        }
        this.mIpoSetting = (SwitchPreference) findPreference("ipo_setting");
        this.mIpoSetting.setOnPreferenceChangeListener(this);
        if (!FeatureOption.MTK_IPO_SUPPORT || UserHandle.myUserId() != 0) {
            this.mSystemsCategory.removePreference(this.mIpoSetting);
        }
    }

    private void updateAllPreferences() {
        updateServicesPreferences();
        updateSystemPreferences();
    }

    private void updateServicesPreferences() {
        this.mServicesCategory.removeAll();
        List<AccessibilityServiceInfo> installedServices = AccessibilityManager.getInstance(getActivity()).getInstalledAccessibilityServiceList();
        Set<ComponentName> enabledServices = AccessibilityUtils.getEnabledServicesFromSettings(getActivity());
        List<String> permittedServices = this.mDpm.getPermittedAccessibilityServices(UserHandle.myUserId());
        boolean accessibilityEnabled = Secure.getInt(getContentResolver(), "accessibility_enabled", 0) == 1;
        int count = installedServices.size();
        for (int i = 0; i < count; i++) {
            boolean contains;
            String serviceEnabledString;
            String summaryString;
            AccessibilityServiceInfo info = (AccessibilityServiceInfo) installedServices.get(i);
            PreferenceScreen preference = getPreferenceManager().createPreferenceScreen(getActivity());
            String title = info.getResolveInfo().loadLabel(getPackageManager()).toString();
            ServiceInfo serviceInfo = info.getResolveInfo().serviceInfo;
            ComponentName componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
            preference.setKey(componentName.flattenToString());
            preference.setTitle(title);
            if (accessibilityEnabled) {
                contains = enabledServices.contains(componentName);
            } else {
                contains = false;
            }
            if (contains) {
                serviceEnabledString = getString(R.string.accessibility_feature_state_on);
            } else {
                serviceEnabledString = getString(R.string.accessibility_feature_state_off);
            }
            boolean contains2 = permittedServices != null ? permittedServices.contains(serviceInfo.packageName) : true;
            preference.setEnabled(!contains2 ? contains : true);
            if (contains2) {
                summaryString = serviceEnabledString;
            } else {
                summaryString = getString(R.string.accessibility_feature_or_input_method_not_allowed);
            }
            preference.setSummary(summaryString);
            preference.setOrder(i);
            preference.setFragment(ToggleAccessibilityServicePreferenceFragment.class.getName());
            preference.setPersistent(true);
            Bundle extras = preference.getExtras();
            extras.putString("preference_key", preference.getKey());
            extras.putBoolean("checked", contains);
            extras.putString("title", title);
            String description = info.loadDescription(getPackageManager());
            if (TextUtils.isEmpty(description)) {
                description = getString(R.string.accessibility_service_default_description);
            }
            extras.putString("summary", description);
            String settingsClassName = info.getSettingsActivityName();
            if (!TextUtils.isEmpty(settingsClassName)) {
                extras.putString("settings_title", getString(R.string.accessibility_menu_item_settings));
                extras.putString("settings_component_name", new ComponentName(info.getResolveInfo().serviceInfo.packageName, settingsClassName).flattenToString());
            }
            extras.putParcelable("component_name", componentName);
            this.mServicesCategory.addPreference(preference);
        }
        if (this.mServicesCategory.getPreferenceCount() == 0) {
            if (this.mNoServicesMessagePreference == null) {
                this.mNoServicesMessagePreference = new Preference(getActivity());
                this.mNoServicesMessagePreference.setPersistent(false);
                this.mNoServicesMessagePreference.setLayoutResource(R.layout.text_description_preference);
                this.mNoServicesMessagePreference.setSelectable(false);
                this.mNoServicesMessagePreference.setSummary(getString(R.string.accessibility_no_services_installed));
            }
            this.mServicesCategory.addPreference(this.mNoServicesMessagePreference);
        }
    }

    private void updateSystemPreferences() {
        boolean z;
        try {
            this.mCurConfig.updateFrom(ActivityManagerNative.getDefault().getConfiguration());
        } catch (RemoteException e) {
        }
        float updateFontScale = 1.3f;
        if (FeatureOption.MTK_PRODUCT_IS_TABLET) {
            String[] array = getResources().getStringArray(R.array.entryvalues_font_size);
            if (array.length > 0) {
                this.mLargeFontScaleTablet = Float.parseFloat(array[array.length - 1]);
            }
            updateFontScale = this.mLargeFontScaleTablet;
        }
        this.mToggleHomeTorchPreference.setChecked(SystemProperties.getBoolean("persist.sys.home_torch", true));
        SwitchPreference switchPreference = this.mToggleLargeTextPreference;
        if (this.mCurConfig.fontScale == updateFontScale) {
            z = true;
        } else {
            z = false;
        }
        switchPreference.setChecked(z);
        switchPreference = this.mToggleHighTextContrastPreference;
        if (Secure.getInt(getContentResolver(), "high_text_contrast_enabled", 0) == 1) {
            z = true;
        } else {
            z = false;
        }
        switchPreference.setChecked(z);
        switchPreference = this.mToggleInversionPreference;
        if (Secure.getInt(getContentResolver(), "accessibility_display_inversion_enabled", 0) == 1) {
            z = true;
        } else {
            z = false;
        }
        switchPreference.setChecked(z);
        if (KeyCharacterMap.deviceHasKey(26) && Utils.isVoiceCapable(getActivity())) {
            this.mTogglePowerButtonEndsCallPreference.setChecked(Secure.getInt(getContentResolver(), "incall_power_button_behavior", 1) == 2);
        }
        updateLockScreenRotationCheckbox();
        this.mToggleSpeakPasswordPreference.setChecked(Secure.getInt(getContentResolver(), "speak_password", 0) != 0);
        String value = String.valueOf(Secure.getInt(getContentResolver(), "long_press_timeout", this.mLongPressTimeoutDefault));
        this.mSelectLongPressTimeoutPreference.setValue(value);
        this.mSelectLongPressTimeoutPreference.setSummary((CharSequence) this.mLongPressTimeoutValuetoTitleMap.get(value));
        updateFeatureSummary("accessibility_captioning_enabled", this.mCaptioningPreferenceScreen);
        updateFeatureSummary("accessibility_display_magnification_enabled", this.mDisplayMagnificationPreferenceScreen);
        updateFeatureSummary("accessibility_display_daltonizer_enabled", this.mDisplayDaltonizerPreferenceScreen);
        if (Global.getInt(getContentResolver(), "enable_accessibility_global_gesture_enabled", 0) == 1) {
            this.mGlobalGesturePreferenceScreen.setSummary(R.string.accessibility_global_gesture_preference_summary_on);
        } else {
            this.mGlobalGesturePreferenceScreen.setSummary(R.string.accessibility_global_gesture_preference_summary_off);
        }
        boolean ipoSettingEnabled = System.getInt(getContentResolver(), "ipo_setting", 1) == 1;
        if (this.mIpoSetting != null) {
            this.mIpoSetting.setChecked(ipoSettingEnabled);
        }
    }

    private void updateFeatureSummary(String prefKey, Preference pref) {
        int i;
        if (Secure.getInt(getContentResolver(), prefKey, 0) == 1) {
            i = R.string.accessibility_feature_state_on;
        } else {
            i = R.string.accessibility_feature_state_off;
        }
        pref.setSummary(i);
    }

    private void updateLockScreenRotationCheckbox() {
        Context context = getActivity();
        if (context != null) {
            this.mToggleLockScreenRotationPreference.setChecked(!RotationPolicy.isRotationLocked(context));
        }
    }

    private void loadInstalledServices() {
        Set<ComponentName> installedServices = sInstalledServices;
        installedServices.clear();
        List<AccessibilityServiceInfo> installedServiceInfos = AccessibilityManager.getInstance(getActivity()).getInstalledAccessibilityServiceList();
        if (installedServiceInfos != null) {
            int installedServiceInfoCount = installedServiceInfos.size();
            for (int i = 0; i < installedServiceInfoCount; i++) {
                ResolveInfo resolveInfo = ((AccessibilityServiceInfo) installedServiceInfos.get(i)).getResolveInfo();
                installedServices.add(new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name));
            }
        }
    }
}
