package com.android.settings;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager.BackStackEntry;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceFragment.OnPreferenceStartFragmentCallback;
import android.preference.PreferenceManager.OnPreferenceTreeClickListener;
import android.preference.PreferenceScreen;
import android.provider.Settings.Global;
import android.text.TextUtils;
import android.transition.TransitionManager;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnActionExpandListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;
import com.android.internal.R;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import com.android.settings.ChooseLockPassword.ChooseLockPasswordFragment;
import com.android.settings.ChooseLockPattern.ChooseLockPatternFragment;
import com.android.settings.accessibility.AccessibilitySettings;
import com.android.settings.accessibility.CaptionPropertiesFragment;
import com.android.settings.accessibility.ToggleDaltonizerPreferenceFragment;
import com.android.settings.accounts.AccountSettings;
import com.android.settings.accounts.AccountSyncSettings;
import com.android.settings.applications.DrawOverlayDetails;
import com.android.settings.applications.InstalledAppDetails;
import com.android.settings.applications.ManageApplications;
import com.android.settings.applications.ManageAssist;
import com.android.settings.applications.ProcessStatsSummary;
import com.android.settings.applications.ProcessStatsUi;
import com.android.settings.applications.UsageAccessDetails;
import com.android.settings.applications.WriteSettingsDetails;
import com.android.settings.bluetooth.BluetoothSettings;
import com.android.settings.dashboard.DashboardCategory;
import com.android.settings.dashboard.DashboardSummary;
import com.android.settings.dashboard.DashboardTile;
import com.android.settings.dashboard.NoHomeDialogFragment;
import com.android.settings.dashboard.SearchResultsSummary;
import com.android.settings.deviceinfo.PrivateVolumeForget;
import com.android.settings.deviceinfo.PrivateVolumeSettings;
import com.android.settings.deviceinfo.PublicVolumeSettings;
import com.android.settings.deviceinfo.StorageSettings;
import com.android.settings.fuelgauge.BatterySaverSettings;
import com.android.settings.fuelgauge.PowerUsageDetail;
import com.android.settings.fuelgauge.PowerUsageSummary;
import com.android.settings.inputmethod.InputMethodAndLanguageSettings;
import com.android.settings.inputmethod.KeyboardLayoutPickerFragment;
import com.android.settings.inputmethod.SpellCheckersSettings;
import com.android.settings.inputmethod.UserDictionaryList;
import com.android.settings.location.LocationSettings;
import com.android.settings.nfc.AndroidBeam;
import com.android.settings.nfc.PaymentSettings;
import com.android.settings.notification.AppNotificationSettings;
import com.android.settings.notification.NotificationAccessSettings;
import com.android.settings.notification.NotificationSettings;
import com.android.settings.notification.NotificationStation;
import com.android.settings.notification.OtherSoundSettings;
import com.android.settings.notification.ZenAccessSettings;
import com.android.settings.notification.ZenModeAutomationSettings;
import com.android.settings.notification.ZenModeEventRuleSettings;
import com.android.settings.notification.ZenModeExternalRuleSettings;
import com.android.settings.notification.ZenModePrioritySettings;
import com.android.settings.notification.ZenModeScheduleRuleSettings;
import com.android.settings.notification.ZenModeSettings;
import com.android.settings.print.PrintJobSettingsFragment;
import com.android.settings.print.PrintSettingsFragment;
import com.android.settings.search.DynamicIndexableContentMonitor;
import com.android.settings.search.Index;
import com.android.settings.sim.SimSettings;
import com.android.settings.tts.TextToSpeechSettings;
import com.android.settings.users.UserSettings;
import com.android.settings.vpn2.VpnSettings;
import com.android.settings.wfd.WifiDisplaySettings;
import com.android.settings.widget.SwitchBar;
import com.android.settings.wifi.AdvancedWifiSettings;
import com.android.settings.wifi.SavedAccessPointsWifiSettings;
import com.android.settings.wifi.WifiSettings;
import com.android.settings.wifi.p2p.WifiP2pSettings;
import com.mediatek.audioprofile.AudioProfileSettings;
import com.mediatek.audioprofile.SoundEnhancement;
import com.mediatek.audioprofile.SubSelectSettings;
import com.mediatek.beam.BeamShareHistory;
import com.mediatek.hdmi.HdmiSettings;
import com.mediatek.hotknot.HotKnotAdapter;
import com.mediatek.nfc.NfcSettings;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.hotknot.HotKnotSettings;
import com.mediatek.settings.wfd.WfdSinkSurfaceFragment;
import com.mediatek.wifi.WifiGprsSelector;
import com.mediatek.wifi.hotspot.TetherWifiSettings;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.xmlpull.v1.XmlPullParserException;

public class SettingsActivity extends Activity implements OnPreferenceTreeClickListener, OnPreferenceStartFragmentCallback, ButtonBarHandler, OnBackStackChangedListener, OnQueryTextListener, OnCloseListener, OnActionExpandListener {
    private static final String[] ENTRY_FRAGMENTS = new String[]{WirelessSettings.class.getName(), WifiSettings.class.getName(), AdvancedWifiSettings.class.getName(), SavedAccessPointsWifiSettings.class.getName(), BluetoothSettings.class.getName(), SimSettings.class.getName(), TetherSettings.class.getName(), WifiP2pSettings.class.getName(), VpnSettings.class.getName(), DateTimeSettings.class.getName(), LocalePicker.class.getName(), InputMethodAndLanguageSettings.class.getName(), SpellCheckersSettings.class.getName(), UserDictionaryList.class.getName(), UserDictionarySettings.class.getName(), HomeSettings.class.getName(), DisplaySettings.class.getName(), DeviceInfoSettings.class.getName(), ManageApplications.class.getName(), ManageAssist.class.getName(), ProcessStatsUi.class.getName(), NotificationStation.class.getName(), LocationSettings.class.getName(), SecuritySettings.class.getName(), UsageAccessDetails.class.getName(), PrivacySettings.class.getName(), DeviceAdminSettings.class.getName(), AccessibilitySettings.class.getName(), CaptionPropertiesFragment.class.getName(), ToggleDaltonizerPreferenceFragment.class.getName(), TextToSpeechSettings.class.getName(), StorageSettings.class.getName(), PrivateVolumeForget.class.getName(), PrivateVolumeSettings.class.getName(), PublicVolumeSettings.class.getName(), DevelopmentSettings.class.getName(), AndroidBeam.class.getName(), WifiDisplaySettings.class.getName(), PowerUsageSummary.class.getName(), AccountSyncSettings.class.getName(), AccountSettings.class.getName(), CryptKeeperSettings.class.getName(), DataUsageSummary.class.getName(), DreamSettings.class.getName(), UserSettings.class.getName(), NotificationAccessSettings.class.getName(), ZenAccessSettings.class.getName(), PrintSettingsFragment.class.getName(), PrintJobSettingsFragment.class.getName(), TrustedCredentialsSettings.class.getName(), PaymentSettings.class.getName(), KeyboardLayoutPickerFragment.class.getName(), ZenModeSettings.class.getName(), NotificationSettings.class.getName(), ChooseLockPasswordFragment.class.getName(), ChooseLockPatternFragment.class.getName(), InstalledAppDetails.class.getName(), BatterySaverSettings.class.getName(), AppNotificationSettings.class.getName(), OtherSoundSettings.class.getName(), ApnSettings.class.getName(), WifiCallingSettings.class.getName(), ZenModePrioritySettings.class.getName(), ZenModeAutomationSettings.class.getName(), ZenModeScheduleRuleSettings.class.getName(), ZenModeEventRuleSettings.class.getName(), ZenModeExternalRuleSettings.class.getName(), ProcessStatsUi.class.getName(), PowerUsageDetail.class.getName(), ProcessStatsSummary.class.getName(), DrawOverlayDetails.class.getName(), WriteSettingsDetails.class.getName(), WfdSinkSurfaceFragment.class.getName(), HotKnotSettings.class.getName(), AudioProfileSettings.class.getName(), SubSelectSettings.class.getName(), SoundEnhancement.class.getName(), NfcSettings.class.getName(), BeamShareHistory.class.getName(), HdmiSettings.class.getName(), TetherWifiSettings.class.getName(), WifiGprsSelector.class.getName()};
    private static final String[] LIKE_SHORTCUT_INTENT_ACTION_ARRAY = new String[]{"android.settings.APPLICATION_DETAILS_SETTINGS"};
    private static ISettingsMiscExt mExt;
    private static boolean sShowNoHomeNotice = false;
    private int[] SETTINGS_FOR_RESTRICTED = new int[]{R.id.wireless_section, R.id.wifi_settings, R.id.bluetooth_settings, R.id.data_usage_settings, R.id.sim_settings, R.id.wireless_settings, R.id.device_section, R.id.notification_settings, R.id.display_settings, R.id.storage_settings, R.id.application_settings, R.id.battery_settings, R.id.personal_section, R.id.location_settings, R.id.fp_settings, R.id.save_battery_settings, R.id.otouchpad_settings, R.id.gesture_settings, R.id.smartwake_settings, R.id.hardkey_settings, R.id.qrcode_settings, R.id.security_settings, R.id.language_settings, R.id.user_settings, R.id.account_settings, R.id.system_section, R.id.date_time_settings, R.id.about_settings, R.id.accessibility_settings, R.id.print_settings, R.id.nfc_payment_settings, R.id.home_settings, R.id.dashboard, R.id.power_settings, R.id.hotknot_settings, R.id.hetcomm_settings};
    private ActionBar mActionBar;
    private BroadcastReceiver mBatteryInfoReceiver = new C01821();
    private boolean mBatteryPresent = true;
    private ArrayList<DashboardCategory> mCategories = new ArrayList();
    private ViewGroup mContent;
    private SharedPreferences mDevelopmentPreferences;
    private OnSharedPreferenceChangeListener mDevelopmentPreferencesListener;
    private boolean mDisplayHomeAsUpEnabled;
    private boolean mDisplaySearch;
    private final DynamicIndexableContentMonitor mDynamicIndexableContentMonitor = new DynamicIndexableContentMonitor();
    private String mFragmentClass;
    private Handler mHandler = new C01832();
    private int mHomeActivitiesCount = 1;
    private CharSequence mInitialTitle;
    private int mInitialTitleResId;
    private boolean mIsShortcut;
    private boolean mIsShowingDashboard;
    private boolean mNeedToRevertToInitialFragment = false;
    private Button mNextButton;
    private Intent mResultIntentData;
    private MenuItem mSearchMenuItem;
    private boolean mSearchMenuItemExpanded = false;
    private String mSearchQuery;
    private SearchResultsSummary mSearchResultsFragment;
    private SearchView mSearchView;
    private SwitchBar mSwitchBar;

    class C01821 extends BroadcastReceiver {
        C01821() {
        }

        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.BATTERY_CHANGED".equals(intent.getAction())) {
                boolean batteryPresent = Utils.isBatteryPresent(intent);
                if (SettingsActivity.this.mBatteryPresent != batteryPresent) {
                    SettingsActivity.this.mBatteryPresent = batteryPresent;
                    SettingsActivity.this.invalidateCategories(true);
                }
            }
        }
    }

    class C01832 extends Handler {
        C01832() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (msg.getData().getBoolean("msg_data_force_refresh")) {
                        SettingsActivity.this.buildDashboardCategories(SettingsActivity.this.mCategories);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    class C01843 implements OnClickListener {
        C01843() {
        }

        public void onClick(View v) {
            SettingsActivity.this.setResult(0, SettingsActivity.this.getResultIntentData());
            SettingsActivity.this.finish();
        }
    }

    class C01854 implements OnClickListener {
        C01854() {
        }

        public void onClick(View v) {
            SettingsActivity.this.setResult(-1, SettingsActivity.this.getResultIntentData());
            SettingsActivity.this.finish();
        }
    }

    class C01865 implements OnClickListener {
        C01865() {
        }

        public void onClick(View v) {
            SettingsActivity.this.setResult(-1, SettingsActivity.this.getResultIntentData());
            SettingsActivity.this.finish();
        }
    }

    class C01876 implements OnSharedPreferenceChangeListener {
        C01876() {
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            SettingsActivity.this.invalidateCategories(true);
        }
    }

    public SwitchBar getSwitchBar() {
        return this.mSwitchBar;
    }

    public List<DashboardCategory> getDashboardCategories(boolean forceRefresh) {
        if (forceRefresh || this.mCategories.size() == 0) {
            buildDashboardCategories(this.mCategories);
        }
        return this.mCategories;
    }

    public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref) {
        int titleRes = pref.getTitleRes();
        if (pref.getFragment().equals(WallpaperTypeSettings.class.getName())) {
            titleRes = R.string.wallpaper_settings_fragment_title;
        } else if (!pref.getFragment().equals(OwnerInfoSettings.class.getName()) || UserHandle.myUserId() == 0) {
            if (pref.getFragment().equals(HdmiSettings.class.getName())) {
                titleRes = -1;
            }
        } else if (UserManager.get(this).isLinkedUser()) {
            titleRes = R.string.profile_info_settings_title;
        } else {
            titleRes = R.string.user_info_settings_title;
        }
        startPreferencePanel(pref.getFragment(), pref.getExtras(), titleRes, pref.getTitle(), null, 0);
        return true;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return false;
    }

    private void invalidateCategories(boolean forceRefresh) {
        if (!this.mHandler.hasMessages(1)) {
            Message msg = new Message();
            msg.what = 1;
            msg.getData().putBoolean("msg_data_force_refresh", forceRefresh);
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Index.getInstance(this).update();
    }

    protected void onStart() {
        super.onStart();
        if (this.mNeedToRevertToInitialFragment) {
            revertToInitialFragment();
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        if (!this.mDisplaySearch) {
            return false;
        }
        getMenuInflater().inflate(R.menu.options_menu, menu);
        String query = this.mSearchQuery;
        this.mSearchMenuItem = menu.findItem(R.id.search);
        this.mSearchView = (SearchView) this.mSearchMenuItem.getActionView();
        if (this.mSearchMenuItem == null || this.mSearchView == null) {
            return false;
        }
        if (this.mSearchResultsFragment != null) {
            this.mSearchResultsFragment.setSearchView(this.mSearchView);
        }
        this.mSearchMenuItem.setOnActionExpandListener(this);
        this.mSearchView.setOnQueryTextListener(this);
        this.mSearchView.setOnCloseListener(this);
        if (this.mSearchMenuItemExpanded) {
            this.mSearchMenuItem.expandActionView();
        }
        this.mSearchView.setQuery(query, true);
        return true;
    }

    private static boolean isShortCutIntent(Intent intent) {
        Set<String> categories = intent.getCategories();
        return categories != null ? categories.contains("com.android.settings.SHORTCUT") : false;
    }

    private static boolean isLikeShortCutIntent(Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return false;
        }
        for (String equals : LIKE_SHORTCUT_INTENT_ACTION_ARRAY) {
            if (equals.equals(action)) {
                return true;
            }
        }
        return false;
    }

    protected void onCreate(Bundle savedState) {
        boolean z;
        boolean isSubSettings;
        super.onCreate(savedState);
        mExt = UtilsExt.getMiscPlugin(this);
        getMetaData();
        Intent intent = getIntent();
        if (intent.hasExtra("settings:ui_options")) {
            getWindow().setUiOptions(intent.getIntExtra("settings:ui_options", 0));
        }
        this.mDevelopmentPreferences = getSharedPreferences("development", 0);
        String initialFragmentName = intent.getStringExtra(":settings:show_fragment");
        if (isShortCutIntent(intent) || isLikeShortCutIntent(intent)) {
            z = true;
        } else {
            z = intent.getBooleanExtra(":settings:show_fragment_as_shortcut", false);
        }
        this.mIsShortcut = z;
        this.mIsShowingDashboard = intent.getComponent().getClassName().equals(Settings.class.getName());
        if (this instanceof SubSettings) {
            isSubSettings = true;
        } else {
            isSubSettings = intent.getBooleanExtra(":settings:show_fragment_as_subsetting", false);
        }
        if (isSubSettings) {
            int themeResId = getThemeResId();
            if (!(themeResId == R.style.Theme.DialogWhenLarge || themeResId == R.style.Theme.SubSettingsDialogWhenLarge)) {
                setTheme(R.style.Theme.SubSettings);
            }
        }
        setContentView(this.mIsShowingDashboard ? R.layout.settings_main_dashboard : R.layout.settings_main_prefs);
        this.mContent = (ViewGroup) findViewById(R.id.main_content);
        getFragmentManager().addOnBackStackChangedListener(this);
        if (this.mIsShowingDashboard) {
            if (Utils.isLowStorage(this)) {
                Log.w("Settings", "Cannot update the Indexer as we are running low on storage space!");
            } else {
                Index.getInstance(getApplicationContext()).update();
            }
        }
        if (savedState != null) {
            this.mSearchMenuItemExpanded = savedState.getBoolean(":settings:search_menu_expanded");
            this.mSearchQuery = savedState.getString(":settings:search_query");
            setTitleFromIntent(intent);
            ArrayList<DashboardCategory> categories = savedState.getParcelableArrayList(":settings:categories");
            if (categories != null) {
                this.mCategories.clear();
                this.mCategories.addAll(categories);
                setTitleFromBackStack();
            }
            this.mDisplayHomeAsUpEnabled = savedState.getBoolean(":settings:show_home_as_up");
            this.mDisplaySearch = savedState.getBoolean(":settings:show_search");
            this.mHomeActivitiesCount = savedState.getInt(":settings:home_activities_count", 1);
        } else if (this.mIsShowingDashboard) {
            this.mDisplayHomeAsUpEnabled = false;
            this.mDisplaySearch = true;
            this.mInitialTitleResId = R.string.dashboard_title;
            switchToFragment(DashboardSummary.class.getName(), null, false, false, this.mInitialTitleResId, this.mInitialTitle, false);
        } else {
            this.mDisplaySearch = false;
            if (this.mIsShortcut) {
                this.mDisplayHomeAsUpEnabled = isSubSettings;
            } else if (isSubSettings) {
                this.mDisplayHomeAsUpEnabled = true;
            } else {
                this.mDisplayHomeAsUpEnabled = false;
            }
            setTitleFromIntent(intent);
            switchToFragment(initialFragmentName, intent.getBundleExtra(":settings:show_fragment_args"), true, false, this.mInitialTitleResId, this.mInitialTitle, false);
        }
        this.mActionBar = getActionBar();
        if (this.mActionBar != null) {
            this.mActionBar.setDisplayHomeAsUpEnabled(this.mDisplayHomeAsUpEnabled);
            this.mActionBar.setHomeButtonEnabled(this.mDisplayHomeAsUpEnabled);
        }
        this.mSwitchBar = (SwitchBar) findViewById(R.id.switch_bar);
        if (intent.getBooleanExtra("extra_prefs_show_button_bar", false)) {
            View buttonBar = findViewById(R.id.button_bar);
            if (buttonBar != null) {
                String buttonText;
                buttonBar.setVisibility(0);
                Button backButton = (Button) findViewById(R.id.back_button);
                backButton.setOnClickListener(new C01843());
                Button skipButton = (Button) findViewById(R.id.skip_button);
                skipButton.setOnClickListener(new C01854());
                this.mNextButton = (Button) findViewById(R.id.next_button);
                this.mNextButton.setOnClickListener(new C01865());
                if (intent.hasExtra("extra_prefs_set_next_text")) {
                    buttonText = intent.getStringExtra("extra_prefs_set_next_text");
                    if (TextUtils.isEmpty(buttonText)) {
                        this.mNextButton.setVisibility(8);
                    } else {
                        this.mNextButton.setText(buttonText);
                    }
                }
                if (intent.hasExtra("extra_prefs_set_back_text")) {
                    buttonText = intent.getStringExtra("extra_prefs_set_back_text");
                    if (TextUtils.isEmpty(buttonText)) {
                        backButton.setVisibility(8);
                    } else {
                        backButton.setText(buttonText);
                    }
                }
                if (intent.getBooleanExtra("extra_prefs_show_skip", false)) {
                    skipButton.setVisibility(0);
                }
            }
        }
        this.mHomeActivitiesCount = getHomeActivitiesCount();
    }

    private int getHomeActivitiesCount() {
        ArrayList<ResolveInfo> homeApps = new ArrayList();
        getPackageManager().getHomeActivities(homeApps);
        return homeApps.size();
    }

    private void setTitleFromIntent(Intent intent) {
        int initialTitleResId = intent.getIntExtra(":settings:show_fragment_title_resid", -1);
        if (initialTitleResId > 0) {
            this.mInitialTitle = null;
            this.mInitialTitleResId = initialTitleResId;
            String initialTitleResPackageName = intent.getStringExtra(":settings:show_fragment_title_res_package_name");
            if (initialTitleResPackageName != null) {
                try {
                    this.mInitialTitle = createPackageContextAsUser(initialTitleResPackageName, 0, new UserHandle(UserHandle.myUserId())).getResources().getText(this.mInitialTitleResId);
                    setTitle(this.mInitialTitle);
                    this.mInitialTitleResId = -1;
                } catch (NameNotFoundException e) {
                    Log.w("Settings", "Could not find package" + initialTitleResPackageName);
                }
            } else {
                setTitle(this.mInitialTitleResId);
            }
        } else {
            this.mInitialTitleResId = -1;
            String initialTitle = intent.getStringExtra(":settings:show_fragment_title");
            if (initialTitle == null) {
                initialTitle = getTitle();
            }
            this.mInitialTitle = initialTitle;
            setTitle(this.mInitialTitle);
        }
    }

    public void onBackStackChanged() {
        setTitleFromBackStack();
    }

    private int setTitleFromBackStack() {
        int count = getFragmentManager().getBackStackEntryCount();
        if (count == 0) {
            if (this.mInitialTitleResId > 0) {
                setTitle(this.mInitialTitleResId);
            } else {
                setTitle(this.mInitialTitle);
            }
            return 0;
        }
        setTitleFromBackStackEntry(getFragmentManager().getBackStackEntryAt(count - 1));
        return count;
    }

    private void setTitleFromBackStackEntry(BackStackEntry bse) {
        CharSequence title;
        int titleRes = bse.getBreadCrumbTitleRes();
        if (titleRes > 0) {
            title = getText(titleRes);
        } else {
            title = bse.getBreadCrumbTitle();
        }
        if (title != null) {
            setTitle(title);
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.mCategories.size() > 0) {
            outState.putParcelableArrayList(":settings:categories", this.mCategories);
        }
        outState.putBoolean(":settings:show_home_as_up", this.mDisplayHomeAsUpEnabled);
        outState.putBoolean(":settings:show_search", this.mDisplaySearch);
        if (this.mDisplaySearch) {
            outState.putBoolean(":settings:search_menu_expanded", this.mSearchMenuItem != null ? this.mSearchMenuItem.isActionViewExpanded() : false);
            outState.putString(":settings:search_query", this.mSearchView != null ? this.mSearchView.getQuery().toString() : "");
        }
        outState.putInt(":settings:home_activities_count", this.mHomeActivitiesCount);
    }

    public void onResume() {
        super.onResume();
        if (this.mIsShowingDashboard) {
            MetricsLogger.visible(this, 1);
        }
        int newHomeActivityCount = getHomeActivitiesCount();
        if (newHomeActivityCount != this.mHomeActivitiesCount) {
            this.mHomeActivitiesCount = newHomeActivityCount;
            invalidateCategories(true);
        }
        this.mDevelopmentPreferencesListener = new C01876();
        this.mDevelopmentPreferences.registerOnSharedPreferenceChangeListener(this.mDevelopmentPreferencesListener);
        registerReceiver(this.mBatteryInfoReceiver, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        this.mDynamicIndexableContentMonitor.register(this);
        if (this.mDisplaySearch && !TextUtils.isEmpty(this.mSearchQuery)) {
            onQueryTextSubmit(this.mSearchQuery);
        }
    }

    public void onPause() {
        super.onPause();
        if (this.mIsShowingDashboard) {
            MetricsLogger.hidden(this, 1);
        }
        unregisterReceiver(this.mBatteryInfoReceiver);
        this.mDynamicIndexableContentMonitor.unregister();
    }

    public void onDestroy() {
        super.onDestroy();
        this.mDevelopmentPreferences.unregisterOnSharedPreferenceChangeListener(this.mDevelopmentPreferencesListener);
        this.mDevelopmentPreferencesListener = null;
    }

    protected boolean isValidFragment(String fragmentName) {
        for (String equals : ENTRY_FRAGMENTS) {
            if (equals.equals(fragmentName)) {
                return true;
            }
        }
        return false;
    }

    public Intent getIntent() {
        Intent superIntent = super.getIntent();
        String startingFragment = getStartingFragmentClass(superIntent);
        if (startingFragment == null) {
            return superIntent;
        }
        Intent modIntent = new Intent(superIntent);
        modIntent.putExtra(":settings:show_fragment", startingFragment);
        Bundle args = superIntent.getExtras();
        if (args != null) {
            args = new Bundle(args);
        } else {
            args = new Bundle();
        }
        args.putParcelable("intent", superIntent);
        modIntent.putExtra(":settings:show_fragment_args", args);
        return modIntent;
    }

    private String getStartingFragmentClass(Intent intent) {
        if (this.mFragmentClass != null) {
            return this.mFragmentClass;
        }
        String intentClass = intent.getComponent().getClassName();
        if (intentClass.equals(getClass().getName())) {
            return null;
        }
        if ("com.android.settings.ManageApplications".equals(intentClass) || "com.android.settings.RunningServices".equals(intentClass) || "com.android.settings.applications.StorageUse".equals(intentClass)) {
            intentClass = ManageApplications.class.getName();
        }
        return intentClass;
    }

    public void startPreferencePanel(String fragmentClass, Bundle args, int titleRes, CharSequence titleText, Fragment resultTo, int resultRequestCode) {
        CharSequence title = null;
        if (titleRes < 0) {
            if (titleText != null) {
                title = titleText.toString();
            } else {
                title = "";
            }
        }
        Utils.startWithFragment((Context) this, fragmentClass, args, resultTo, resultRequestCode, titleRes, title, this.mIsShortcut);
    }

    public void startPreferencePanelAsUser(String fragmentClass, Bundle args, int titleRes, CharSequence titleText, UserHandle userHandle) {
        if (userHandle.getIdentifier() == UserHandle.myUserId()) {
            startPreferencePanel(fragmentClass, args, titleRes, titleText, null, 0);
            return;
        }
        CharSequence title = null;
        if (titleRes < 0) {
            if (titleText != null) {
                title = titleText.toString();
            } else {
                title = "";
            }
        }
        Utils.startWithFragmentAsUser(this, fragmentClass, args, titleRes, title, this.mIsShortcut, userHandle);
    }

    public void finishPreferencePanel(Fragment caller, int resultCode, Intent resultData) {
        setResult(resultCode, resultData);
        finish();
    }

    public void startPreferenceFragment(Fragment fragment, boolean push) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.main_content, fragment);
        if (push) {
            transaction.setTransition(4097);
            transaction.addToBackStack(":settings:prefs");
        } else {
            transaction.setTransition(4099);
        }
        transaction.commitAllowingStateLoss();
    }

    private Fragment switchToFragment(String fragmentName, Bundle args, boolean validate, boolean addToBackStack, int titleResId, CharSequence title, boolean withTransition) {
        if (!validate || isValidFragment(fragmentName)) {
            Fragment f = Fragment.instantiate(this, fragmentName, args);
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.main_content, f);
            if (withTransition) {
                TransitionManager.beginDelayedTransition(this.mContent);
            }
            if (addToBackStack) {
                transaction.addToBackStack(":settings:prefs");
            }
            if (titleResId > 0) {
                transaction.setBreadCrumbTitle(titleResId);
            } else if (title != null) {
                transaction.setBreadCrumbTitle(title);
            }
            transaction.commitAllowingStateLoss();
            getFragmentManager().executePendingTransactions();
            return f;
        }
        throw new IllegalArgumentException("Invalid fragment for this activity: " + fragmentName);
    }

    private void buildDashboardCategories(List<DashboardCategory> categories) {
        categories.clear();
        loadCategoriesFromResource(R.xml.dashboard_categories, categories, this);
        updateTilesList(categories);
    }

    public static void loadCategoriesFromResource(int resid, List<DashboardCategory> target, Context context) {
        XmlResourceParser xmlResourceParser = null;
        try {
            int type;
            xmlResourceParser = context.getResources().getXml(resid);
            AttributeSet attrs = Xml.asAttributeSet(xmlResourceParser);
            do {
                type = xmlResourceParser.next();
                if (type == 1) {
                    break;
                }
            } while (type != 2);
            String nodeName = xmlResourceParser.getName();
            if ("dashboard-categories".equals(nodeName)) {
                Bundle curBundle = null;
                int outerDepth = xmlResourceParser.getDepth();
                while (true) {
                    type = xmlResourceParser.next();
                    if (type == 1 || (type == 3 && xmlResourceParser.getDepth() <= outerDepth)) {
                        if (xmlResourceParser != null) {
                            xmlResourceParser.close();
                            return;
                        }
                        return;
                    } else if (!(type == 3 || type == 4)) {
                        if ("dashboard-category".equals(xmlResourceParser.getName())) {
                            DashboardCategory category = new DashboardCategory();
                            TypedArray sa = context.obtainStyledAttributes(attrs, R.styleable.PreferenceHeader);
                            category.id = (long) sa.getResourceId(1, -1);
                            TypedValue tv = sa.peekValue(2);
                            if (tv != null && tv.type == 3) {
                                if (tv.resourceId != 0) {
                                    category.titleRes = tv.resourceId;
                                } else {
                                    category.title = tv.string;
                                }
                            }
                            sa.recycle();
                            sa = context.obtainStyledAttributes(attrs, R.styleable.Preference);
                            tv = sa.peekValue(6);
                            if (tv != null && tv.type == 3) {
                                if (tv.resourceId != 0) {
                                    category.key = context.getString(tv.resourceId);
                                } else {
                                    category.key = tv.string.toString();
                                }
                            }
                            sa.recycle();
                            int innerDepth = xmlResourceParser.getDepth();
                            while (true) {
                                type = xmlResourceParser.next();
                                if (type == 1 || (type == 3 && xmlResourceParser.getDepth() <= innerDepth)) {
                                    target.add(category);
                                } else if (!(type == 3 || type == 4)) {
                                    String innerNodeName = xmlResourceParser.getName();
                                    if (innerNodeName.equals("dashboard-tile")) {
                                        DashboardTile tile = new DashboardTile();
                                        sa = context.obtainStyledAttributes(attrs, R.styleable.PreferenceHeader);
                                        tile.id = (long) sa.getResourceId(1, -1);
                                        tv = sa.peekValue(2);
                                        if (tv != null && tv.type == 3) {
                                            if (tv.resourceId != 0) {
                                                tile.titleRes = tv.resourceId;
                                            } else {
                                                tile.title = tv.string;
                                            }
                                        }
                                        tv = sa.peekValue(3);
                                        if (tv != null && tv.type == 3) {
                                            if (tv.resourceId != 0) {
                                                tile.summaryRes = tv.resourceId;
                                            } else {
                                                tile.summary = tv.string;
                                            }
                                        }
                                        tile.iconRes = sa.getResourceId(0, 0);
                                        tile.fragment = sa.getString(4);
                                        sa.recycle();
                                        if (curBundle == null) {
                                            curBundle = new Bundle();
                                        }
                                        int innerDepth2 = xmlResourceParser.getDepth();
                                        while (true) {
                                            type = xmlResourceParser.next();
                                            if (type != 1 && (type != 3 || xmlResourceParser.getDepth() > innerDepth2)) {
                                                if (!(type == 3 || type == 4)) {
                                                    String innerNodeName2 = xmlResourceParser.getName();
                                                    if (innerNodeName2.equals("extra")) {
                                                        context.getResources().parseBundleExtra("extra", attrs, curBundle);
                                                        XmlUtils.skipCurrentTag(xmlResourceParser);
                                                    } else if (innerNodeName2.equals("intent")) {
                                                        tile.intent = Intent.parseIntent(context.getResources(), xmlResourceParser, attrs);
                                                    } else {
                                                        XmlUtils.skipCurrentTag(xmlResourceParser);
                                                    }
                                                }
                                            }
                                        }
                                        if (curBundle.size() > 0) {
                                            tile.fragmentArguments = curBundle;
                                            curBundle = null;
                                        }
                                        if (tile.id != 2131952350 || Utils.showSimCardTile(context)) {
                                            category.addTile(tile);
                                            if (mExt != null) {
                                                mExt.addCustomizedItem(category, Boolean.valueOf(tile.id == 2131952351));
                                            }
                                        }
                                        if (tile.id == 2131952357 && FeatureOption.MTK_AUDIO_PROFILES) {
                                            tile.fragment = "com.mediatek.audioprofile.AudioProfileSettings";
                                        }
                                    } else if (innerNodeName.equals("external-tiles")) {
                                        category.externalIndex = category.getTilesCount();
                                    } else {
                                        XmlUtils.skipCurrentTag(xmlResourceParser);
                                    }
                                }
                            }
                            target.add(category);
                        } else {
                            XmlUtils.skipCurrentTag(xmlResourceParser);
                        }
                    }
                }
                if (xmlResourceParser != null) {
                    xmlResourceParser.close();
                    return;
                }
                return;
            }
            throw new RuntimeException("XML document must start with <preference-categories> tag; found" + nodeName + " at " + xmlResourceParser.getPositionDescription());
        } catch (XmlPullParserException e) {
            throw new RuntimeException("Error parsing categories", e);
        } catch (IOException e2) {
            throw new RuntimeException("Error parsing categories", e2);
        } catch (Throwable th) {
            if (xmlResourceParser != null) {
                xmlResourceParser.close();
            }
        }
    }

    private void updateTilesList(List<DashboardCategory> target) {
        boolean z;
        String CUSTOM_DEVELOPMENT_PROP = "ro.init.development";
        if (SystemProperties.getInt("ro.init.development", 2) == 0) {
            z = false;
        } else if (SystemProperties.getInt("ro.init.development", 2) == 1) {
            z = true;
        } else {
            z = this.mDevelopmentPreferences.getBoolean("show", Build.TYPE.equals("eng"));
        }
        UserManager um = (UserManager) getSystemService("user");
        int size = target.size();
        for (int i = 0; i < size; i++) {
            DashboardCategory category = (DashboardCategory) target.get(i);
            int id = (int) category.id;
            int n = category.getTilesCount() - 1;
            while (n >= 0) {
                DashboardTile tile = category.getTile(n);
                boolean removeTile = false;
                id = (int) tile.id;
                if (id == R.id.operator_settings || id == R.id.manufacturer_settings) {
                    if (!Utils.updateTileToSpecificActivityFromMetaDataOrRemove(this, tile)) {
                        removeTile = true;
                    }
                } else if (id == R.id.wifi_settings) {
                    if (!getPackageManager().hasSystemFeature("android.hardware.wifi")) {
                        removeTile = true;
                    }
                } else if (id == R.id.bluetooth_settings) {
                    if (!getPackageManager().hasSystemFeature("android.hardware.bluetooth")) {
                        removeTile = true;
                    }
                } else if (id == R.id.gesture_settings) {
                    if ((new File("/sys/bus/platform/drivers/als_ps/ps").exists() ? 1 : null) == null) {
                        removeTile = true;
                    }
                } else if (id != R.id.smartwake_settings) {
                    if (id == R.id.otouchpad_settings) {
                        removeTile = true;
                    } else if (id == R.id.hardkey_settings) {
                        removeTile = true;
                    } else if (id != R.id.qrcode_settings) {
                        if (id == R.id.data_usage_settings) {
                            if (!Utils.isBandwidthControlEnabled()) {
                                removeTile = true;
                            }
                        } else if (id == R.id.battery_settings) {
                            if (!this.mBatteryPresent) {
                                removeTile = true;
                            }
                        } else if (id == R.id.home_settings) {
                            if (!updateHomeSettingTiles(tile)) {
                                removeTile = true;
                            }
                        } else if (id == R.id.user_settings) {
                            if (((UserManager) getSystemService("user")).getUserCount() > 1) {
                            }
                            if (!UserManager.supportsMultipleUsers() || Utils.isMonkeyRunning()) {
                                removeTile = true;
                            }
                        } else if (id == R.id.nfc_payment_settings) {
                            if (getPackageManager().hasSystemFeature("android.hardware.nfc")) {
                                NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
                                int hceFlg = Global.getInt(getContentResolver(), "nfc_hce_on", 0);
                                if (!(adapter != null && adapter.isEnabled() && getPackageManager().hasSystemFeature("android.hardware.nfc.hce") && hceFlg == 1)) {
                                    removeTile = true;
                                }
                            } else {
                                removeTile = true;
                            }
                        } else if (id == R.id.print_settings) {
                            if (!getPackageManager().hasSystemFeature("android.software.print")) {
                                removeTile = true;
                            }
                        } else if (id == R.id.development_settings) {
                            if (!z || um.hasUserRestriction("no_debugging_features")) {
                                removeTile = true;
                            }
                        } else if (id == R.id.power_settings) {
                            List<ResolveInfo> apps = getPackageManager().queryIntentActivities(new Intent("com.android.settings.SCHEDULE_POWER_ON_OFF_SETTING"), 0);
                            if (apps == null || apps.size() == 0) {
                                Log.d("Settings", "apps is null or app size is 0, remove SchedulePowerOnOff");
                                category.removeTile(n);
                            } else {
                                Log.d("Settings", "apps.size()=" + apps.size());
                                if (UserHandle.myUserId() != 0) {
                                    category.removeTile(n);
                                }
                            }
                        } else if (id == R.id.hotknot_settings) {
                            if (HotKnotAdapter.getDefaultAdapter(this) == null) {
                                Log.d("Settings", "HotKnotAdapter is null, remove hotknot_settings");
                                category.removeTile(n);
                            }
                        } else if (id == R.id.privacy_settings) {
                            mExt.setFactoryResetTitle(tile);
                        } else if (id == R.id.hetcomm_settings && !UtilsExt.isPackageExist(this, "com.mediatek.hetcomm")) {
                            removeTile = true;
                        }
                    }
                }
                if (!(UserHandle.myUserId() == 0 || ArrayUtils.contains(this.SETTINGS_FOR_RESTRICTED, id))) {
                    removeTile = true;
                }
                if (removeTile && n < category.getTilesCount()) {
                    category.removeTile(n);
                }
                n--;
            }
        }
        addExternalTiles(target);
    }

    private void addExternalTiles(List<DashboardCategory> target) {
        if (Global.getInt(getContentResolver(), "device_provisioned", 0) != 0) {
            Map<Pair<String, String>, DashboardTile> addedCache = new ArrayMap();
            for (UserHandle user : UserManager.get(this).getUserProfiles()) {
                addExternalTiles(target, user, addedCache);
            }
        }
    }

    private void addExternalTiles(List<DashboardCategory> target, UserHandle user, Map<Pair<String, String>, DashboardTile> addedCache) {
        for (ResolveInfo resolved : getPackageManager().queryIntentActivitiesAsUser(new Intent("com.android.settings.action.EXTRA_SETTINGS"), 128, user.getIdentifier())) {
            if (resolved.system) {
                ActivityInfo activityInfo = resolved.activityInfo;
                Bundle metaData = activityInfo.metaData;
                if (metaData == null || !metaData.containsKey("com.android.settings.category")) {
                    Log.w("Settings", "Found " + resolved.activityInfo.name + " for action " + "com.android.settings.action.EXTRA_SETTINGS" + " missing metadata " + (metaData == null ? "" : "com.android.settings.category"));
                } else {
                    String categoryKey = metaData.getString("com.android.settings.category");
                    DashboardCategory category = getCategory(target, categoryKey);
                    if (category == null) {
                        Log.w("Settings", "Activity " + resolved.activityInfo.name + " has unknown " + "category key " + categoryKey);
                    } else {
                        Pair<String, String> key = new Pair(activityInfo.packageName, activityInfo.name);
                        DashboardTile tile = (DashboardTile) addedCache.get(key);
                        if (tile == null) {
                            tile = new DashboardTile();
                            tile.intent = new Intent().setClassName(activityInfo.packageName, activityInfo.name);
                            Utils.updateTileToSpecificActivityFromMetaDataOrRemove(this, tile);
                            if (category.externalIndex == -1) {
                                category.addTile(tile);
                            } else {
                                category.addTile(category.externalIndex, tile);
                            }
                            addedCache.put(key, tile);
                        }
                        tile.userHandle.add(user);
                    }
                }
            }
        }
    }

    private DashboardCategory getCategory(List<DashboardCategory> target, String categoryKey) {
        for (DashboardCategory category : target) {
            if (categoryKey.equals(category.key)) {
                return category;
            }
        }
        return null;
    }

    private boolean updateHomeSettingTiles(DashboardTile tile) {
        SharedPreferences sp = getSharedPreferences("home_prefs", 0);
        if (sp.getBoolean("do_show", false)) {
            return true;
        }
        try {
            this.mHomeActivitiesCount = getHomeActivitiesCount();
            if (this.mHomeActivitiesCount < 2) {
                if (sShowNoHomeNotice) {
                    sShowNoHomeNotice = false;
                    NoHomeDialogFragment.show(this);
                }
                return false;
            }
            if (tile.fragmentArguments == null) {
                tile.fragmentArguments = new Bundle();
            }
            tile.fragmentArguments.putBoolean("show", true);
            sp.edit().putBoolean("do_show", true).apply();
            return true;
        } catch (Exception e) {
            Log.w("Settings", "Problem looking up home activity!", e);
        }
    }

    private void getMetaData() {
        try {
            ActivityInfo ai = getPackageManager().getActivityInfo(getComponentName(), 128);
            if (ai != null && ai.metaData != null) {
                this.mFragmentClass = ai.metaData.getString("com.android.settings.FRAGMENT_CLASS");
            }
        } catch (NameNotFoundException e) {
            Log.d("Settings", "Cannot get Metadata for: " + getComponentName().toString());
        }
    }

    public boolean hasNextButton() {
        return this.mNextButton != null;
    }

    public Button getNextButton() {
        return this.mNextButton;
    }

    public boolean shouldUpRecreateTask(Intent targetIntent) {
        return super.shouldUpRecreateTask(new Intent(this, SettingsActivity.class));
    }

    public static void requestHomeNotice() {
        sShowNoHomeNotice = true;
    }

    public boolean onQueryTextSubmit(String query) {
        switchToSearchResultsFragmentIfNeeded();
        this.mSearchQuery = query;
        return this.mSearchResultsFragment.onQueryTextSubmit(query);
    }

    public boolean onQueryTextChange(String newText) {
        this.mSearchQuery = newText;
        if (this.mSearchResultsFragment == null) {
            return false;
        }
        return this.mSearchResultsFragment.onQueryTextChange(newText);
    }

    public boolean onClose() {
        return false;
    }

    public boolean onMenuItemActionExpand(MenuItem item) {
        if (item.getItemId() == this.mSearchMenuItem.getItemId()) {
            switchToSearchResultsFragmentIfNeeded();
        }
        return true;
    }

    public boolean onMenuItemActionCollapse(MenuItem item) {
        if (item.getItemId() == this.mSearchMenuItem.getItemId() && this.mSearchMenuItemExpanded) {
            revertToInitialFragment();
        }
        return true;
    }

    private void switchToSearchResultsFragmentIfNeeded() {
        if (this.mSearchResultsFragment == null) {
            Fragment current = getFragmentManager().findFragmentById(R.id.main_content);
            if (current == null || !(current instanceof SearchResultsSummary)) {
                this.mSearchResultsFragment = (SearchResultsSummary) switchToFragment(SearchResultsSummary.class.getName(), null, false, true, R.string.search_results_title, null, true);
            } else {
                this.mSearchResultsFragment = (SearchResultsSummary) current;
            }
            this.mSearchResultsFragment.setSearchView(this.mSearchView);
            this.mSearchMenuItemExpanded = true;
        }
    }

    public void needToRevertToInitialFragment() {
        this.mNeedToRevertToInitialFragment = true;
    }

    private void revertToInitialFragment() {
        this.mNeedToRevertToInitialFragment = false;
        this.mSearchResultsFragment = null;
        this.mSearchMenuItemExpanded = false;
        getFragmentManager().popBackStackImmediate(":settings:prefs", 1);
        if (this.mSearchMenuItem != null) {
            this.mSearchMenuItem.collapseActionView();
        }
    }

    public Intent getResultIntentData() {
        return this.mResultIntentData;
    }

    public void setResultIntentData(Intent resultIntentData) {
        this.mResultIntentData = resultIntentData;
    }
}
