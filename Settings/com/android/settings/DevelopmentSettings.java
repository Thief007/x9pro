package com.android.settings;

import android.app.ActivityManagerNative;
import android.app.AlertDialog.Builder;
import android.app.AppOpsManager;
import android.app.AppOpsManager.OpEntry;
import android.app.AppOpsManager.PackageOps;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.app.backup.IBackupManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.hardware.usb.IUsbManager;
import android.hardware.usb.UsbManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.Log;
import android.view.IWindowManager;
import android.view.IWindowManager.Stub;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import com.android.settings.fuelgauge.InactiveApps;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBar.OnSwitchChangeListener;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IDevExt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class DevelopmentSettings extends SettingsPreferenceFragment implements OnClickListener, OnDismissListener, OnPreferenceChangeListener, OnSwitchChangeListener, Indexable {
    private static String DEFAULT_LOG_RING_BUFFER_SIZE_IN_BYTES = "262144";
    private static final int[] MOCK_LOCATION_APP_OPS = new int[]{58};
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new C01112();
    private Dialog mAdbDialog;
    private Dialog mAdbKeysDialog;
    private final ArrayList<Preference> mAllPrefs = new ArrayList();
    private ListPreference mAnimatorDurationScale;
    private ListPreference mAppProcessLimit;
    private IBackupManager mBackupManager;
    private SwitchPreference mBtHciSnoopLog;
    private Preference mBugreport;
    private SwitchPreference mBugreportInPower;
    private Preference mClearAdbKeys;
    private String mDebugApp;
    private Preference mDebugAppPref;
    private ListPreference mDebugHwOverdraw;
    private SwitchPreference mDebugLayout;
    private SwitchPreference mDebugViewAttributes;
    private boolean mDialogClicked;
    private SwitchPreference mDisableOverlays;
    private final HashSet<Preference> mDisabledPrefs = new HashSet();
    private boolean mDontPokeProperties;
    private DevicePolicyManager mDpm;
    private SwitchPreference mEnableAdb;
    private Dialog mEnableDialog;
    private SwitchPreference mEnableMultiWindow;
    private SwitchPreference mEnableOemUnlock;
    private SwitchPreference mEnableTerminal;
    private IDevExt mExt;
    private SwitchPreference mForceHardwareUi;
    private SwitchPreference mForceMsaa;
    private SwitchPreference mForceRtlLayout;
    private boolean mHaveDebugSettings;
    private SwitchPreference mImmediatelyDestroyActivities;
    private SwitchPreference mKeepScreenOn;
    private boolean mLastEnabledState;
    private SwitchPreference mLegacyDhcpClient;
    private ListPreference mLogdSize;
    private SwitchPreference mMobileDataAlwaysOn;
    private String mMockLocationApp;
    private Preference mMockLocationAppPref;
    private ListPreference mOpenGLTraces;
    private ListPreference mOverlayDisplayDevices;
    private PreferenceScreen mPassword;
    private SwitchPreference mPointerLocation;
    private final ArrayList<SwitchPreference> mResetSwitchPrefs = new ArrayList();
    private SwitchPreference mShowAllANRs;
    private SwitchPreference mShowCpuUsage;
    private SwitchPreference mShowHwLayersUpdates;
    private SwitchPreference mShowHwScreenUpdates;
    private ListPreference mShowNonRectClip;
    private SwitchPreference mShowScreenUpdates;
    private SwitchPreference mShowTouches;
    private ListPreference mSimulateColorSpace;
    private SwitchPreference mStrictMode;
    private SwitchBar mSwitchBar;
    private ListPreference mTrackFrameTime;
    private ListPreference mTransitionAnimationScale;
    private SwitchPreference mUSBAudio;
    private UserManager mUm;
    private boolean mUnavailable;
    private ListPreference mUsbConfiguration;
    private BroadcastReceiver mUsbReceiver = new C01101();
    private SwitchPreference mVerifyAppsOverUsb;
    private SwitchPreference mWaitForDebugger;
    private SwitchPreference mWifiAggressiveHandover;
    private SwitchPreference mWifiAllowScansWithTraffic;
    private SwitchPreference mWifiDisplayCertification;
    private WifiManager mWifiManager;
    private SwitchPreference mWifiVerboseLogging;
    private ListPreference mWindowAnimationScale;
    private IWindowManager mWindowManager;

    class C01101 extends BroadcastReceiver {
        C01101() {
        }

        public void onReceive(Context context, Intent intent) {
            DevelopmentSettings.this.updateUsbConfigurationValues();
        }
    }

    static class C01112 extends BaseSearchIndexProvider {
        C01112() {
        }

        private boolean isShowingDeveloperOptions(Context context) {
            return context.getSharedPreferences("development", 0).getBoolean("show", Build.TYPE.equals("eng"));
        }

        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
            if (!isShowingDeveloperOptions(context)) {
                return null;
            }
            new SearchIndexableResource(context).xmlResId = R.xml.development_prefs;
            return Arrays.asList(new SearchIndexableResource[]{sir});
        }

        public List<String> getNonIndexableKeys(Context context) {
            if (!isShowingDeveloperOptions(context)) {
                return null;
            }
            List<String> keys = new ArrayList();
            if (!DevelopmentSettings.showEnableOemUnlockPreference()) {
                keys.add("oem_unlock_enable");
            }
            if (!DevelopmentSettings.showEnableMultiWindowPreference()) {
                keys.add("enable_multi_window");
            }
            return keys;
        }
    }

    class C01123 implements OnClickListener {
        C01123() {
        }

        public void onClick(DialogInterface dialog, int which) {
            if (which == -1) {
                Utils.setOemUnlockEnabled(DevelopmentSettings.this.getActivity(), true);
                DevelopmentSettings.this.updateAllOptions();
                return;
            }
            DevelopmentSettings.this.mEnableOemUnlock.setChecked(false);
        }
    }

    class C01134 implements OnClickListener {
        C01134() {
        }

        public void onClick(DialogInterface dialog, int which) {
            DevelopmentSettings.this.setEnableMultiWindow(which == -1);
            DevelopmentSettings.this.updateAllOptions();
        }
    }

    static class SystemPropPoker extends AsyncTask<Void, Void, Void> {
        SystemPropPoker() {
        }

        protected Void doInBackground(Void... params) {
            try {
                for (String service : ServiceManager.listServices()) {
                    IBinder obj = ServiceManager.checkService(service);
                    if (obj != null) {
                        Parcel data = Parcel.obtain();
                        try {
                            obj.transact(1599295570, data, null, 0);
                        } catch (RemoteException e) {
                        } catch (Exception e2) {
                            Log.i("DevelopmentSettings", "Someone wrote a bad service '" + service + "' that doesn't like to be poked: " + e2);
                        }
                        data.recycle();
                    }
                }
                return null;
            } catch (RemoteException e3) {
                return null;
            }
        }
    }

    protected int getMetricsCategory() {
        return 39;
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mWindowManager = Stub.asInterface(ServiceManager.getService("window"));
        this.mBackupManager = IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));
        this.mDpm = (DevicePolicyManager) getActivity().getSystemService("device_policy");
        this.mUm = (UserManager) getSystemService("user");
        this.mWifiManager = (WifiManager) getSystemService("wifi");
        if (Process.myUserHandle().getIdentifier() != 0 || this.mUm.hasUserRestriction("no_debugging_features") || Global.getInt(getActivity().getContentResolver(), "device_provisioned", 0) == 0) {
            this.mUnavailable = true;
            setPreferenceScreen(new PreferenceScreen(getActivity(), null));
            return;
        }
        addPreferencesFromResource(R.xml.development_prefs);
        PreferenceGroup debugDebuggingCategory = (PreferenceGroup) findPreference("debug_debugging_category");
        this.mEnableAdb = findAndInitSwitchPref("enable_adb");
        this.mClearAdbKeys = findPreference("clear_adb_keys");
        if (!(SystemProperties.getBoolean("ro.adb.secure", false) || debugDebuggingCategory == null)) {
            debugDebuggingCategory.removePreference(this.mClearAdbKeys);
        }
        this.mAllPrefs.add(this.mClearAdbKeys);
        this.mEnableTerminal = findAndInitSwitchPref("enable_terminal");
        if (!isPackageInstalled(getActivity(), "com.android.terminal")) {
            debugDebuggingCategory.removePreference(this.mEnableTerminal);
            this.mEnableTerminal = null;
        }
        this.mBugreport = findPreference("bugreport");
        this.mBugreportInPower = findAndInitSwitchPref("bugreport_in_power");
        this.mKeepScreenOn = findAndInitSwitchPref("keep_screen_on");
        this.mBtHciSnoopLog = findAndInitSwitchPref("bt_hci_snoop_log");
        this.mEnableOemUnlock = findAndInitSwitchPref("oem_unlock_enable");
        if (!showEnableOemUnlockPreference()) {
            removePreference(this.mEnableOemUnlock);
            this.mEnableOemUnlock = null;
        }
        this.mDebugViewAttributes = findAndInitSwitchPref("debug_view_attributes");
        this.mPassword = (PreferenceScreen) findPreference("local_backup_password");
        this.mAllPrefs.add(this.mPassword);
        if (!Process.myUserHandle().equals(UserHandle.OWNER)) {
            disableForUser(this.mEnableAdb);
            disableForUser(this.mClearAdbKeys);
            disableForUser(this.mEnableTerminal);
            disableForUser(this.mPassword);
        }
        this.mDebugAppPref = findPreference("debug_app");
        this.mAllPrefs.add(this.mDebugAppPref);
        this.mWaitForDebugger = findAndInitSwitchPref("wait_for_debugger");
        this.mMockLocationAppPref = findPreference("mock_location_app");
        this.mAllPrefs.add(this.mMockLocationAppPref);
        this.mVerifyAppsOverUsb = findAndInitSwitchPref("verify_apps_over_usb");
        if (!showVerifierSetting()) {
            if (debugDebuggingCategory != null) {
                debugDebuggingCategory.removePreference(this.mVerifyAppsOverUsb);
            } else {
                this.mVerifyAppsOverUsb.setEnabled(false);
            }
        }
        this.mStrictMode = findAndInitSwitchPref("strict_mode");
        this.mPointerLocation = findAndInitSwitchPref("pointer_location");
        this.mShowTouches = findAndInitSwitchPref("show_touches");
        this.mShowScreenUpdates = findAndInitSwitchPref("show_screen_updates");
        this.mDisableOverlays = findAndInitSwitchPref("disable_overlays");
        this.mShowCpuUsage = findAndInitSwitchPref("show_cpu_usage");
        this.mForceHardwareUi = findAndInitSwitchPref("force_hw_ui");
        this.mForceMsaa = findAndInitSwitchPref("force_msaa");
        this.mTrackFrameTime = addListPreference("track_frame_time");
        this.mShowNonRectClip = addListPreference("show_non_rect_clip");
        this.mShowHwScreenUpdates = findAndInitSwitchPref("show_hw_screen_udpates");
        this.mShowHwLayersUpdates = findAndInitSwitchPref("show_hw_layers_udpates");
        this.mDebugLayout = findAndInitSwitchPref("debug_layout");
        this.mForceRtlLayout = findAndInitSwitchPref("force_rtl_layout_all_locales");
        this.mDebugHwOverdraw = addListPreference("debug_hw_overdraw");
        this.mWifiDisplayCertification = findAndInitSwitchPref("wifi_display_certification");
        this.mWifiVerboseLogging = findAndInitSwitchPref("wifi_verbose_logging");
        this.mWifiAggressiveHandover = findAndInitSwitchPref("wifi_aggressive_handover");
        this.mWifiAllowScansWithTraffic = findAndInitSwitchPref("wifi_allow_scan_with_traffic");
        this.mLegacyDhcpClient = findAndInitSwitchPref("legacy_dhcp_client");
        this.mMobileDataAlwaysOn = findAndInitSwitchPref("mobile_data_always_on");
        this.mLogdSize = addListPreference("select_logd_size");
        this.mUsbConfiguration = addListPreference("select_usb_configuration");
        this.mWindowAnimationScale = addListPreference("window_animation_scale");
        this.mTransitionAnimationScale = addListPreference("transition_animation_scale");
        this.mAnimatorDurationScale = addListPreference("animator_duration_scale");
        this.mOverlayDisplayDevices = addListPreference("overlay_display_devices");
        this.mEnableMultiWindow = findAndInitSwitchPref("enable_multi_window");
        if (!showEnableMultiWindowPreference()) {
            PreferenceGroup drawingGroup = (PreferenceGroup) findPreference("debug_drawing_category");
            if (drawingGroup != null) {
                drawingGroup.removePreference(this.mEnableMultiWindow);
            } else {
                this.mEnableMultiWindow.setEnabled(false);
            }
            removePreference(this.mEnableMultiWindow);
            this.mEnableMultiWindow = null;
        }
        this.mOpenGLTraces = addListPreference("enable_opengl_traces");
        this.mSimulateColorSpace = addListPreference("simulate_color_space");
        this.mUSBAudio = findAndInitSwitchPref("usb_audio");
        this.mImmediatelyDestroyActivities = (SwitchPreference) findPreference("immediately_destroy_activities");
        this.mAllPrefs.add(this.mImmediatelyDestroyActivities);
        this.mResetSwitchPrefs.add(this.mImmediatelyDestroyActivities);
        this.mAppProcessLimit = addListPreference("app_process_limit");
        this.mShowAllANRs = (SwitchPreference) findPreference("show_all_anrs");
        this.mAllPrefs.add(this.mShowAllANRs);
        this.mResetSwitchPrefs.add(this.mShowAllANRs);
        Preference hdcpChecking = findPreference("hdcp_checking");
        if (hdcpChecking != null) {
            this.mAllPrefs.add(hdcpChecking);
            removePreferenceForProduction(hdcpChecking);
        }
        this.mExt = UtilsExt.getDevExtPlugin(getActivity());
    }

    private ListPreference addListPreference(String prefKey) {
        ListPreference pref = (ListPreference) findPreference(prefKey);
        this.mAllPrefs.add(pref);
        pref.setOnPreferenceChangeListener(this);
        return pref;
    }

    private void disableForUser(Preference pref) {
        if (pref != null) {
            pref.setEnabled(false);
            this.mDisabledPrefs.add(pref);
        }
    }

    private SwitchPreference findAndInitSwitchPref(String key) {
        SwitchPreference pref = (SwitchPreference) findPreference(key);
        if (pref == null) {
            throw new IllegalArgumentException("Cannot find preference with key = " + key);
        }
        this.mAllPrefs.add(pref);
        this.mResetSwitchPrefs.add(pref);
        return pref;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        if (this.mUnavailable) {
            this.mSwitchBar.setEnabled(false);
        } else {
            this.mSwitchBar.addOnSwitchChangeListener(this);
        }
    }

    private boolean removePreferenceForProduction(Preference preference) {
        if (!"user".equals(Build.TYPE)) {
            return false;
        }
        removePreference(preference);
        return true;
    }

    private void removePreference(Preference preference) {
        getPreferenceScreen().removePreference(preference);
        this.mAllPrefs.remove(preference);
        this.mResetSwitchPrefs.remove(preference);
    }

    private void setPrefsEnabledState(boolean enabled) {
        for (int i = 0; i < this.mAllPrefs.size(); i++) {
            Preference pref = (Preference) this.mAllPrefs.get(i);
            boolean z = enabled && !this.mDisabledPrefs.contains(pref);
            pref.setEnabled(z);
        }
        updateAllOptions();
    }

    public void onResume() {
        boolean z = false;
        super.onResume();
        if (this.mUnavailable) {
            TextView emptyView = (TextView) getView().findViewById(16908292);
            getListView().setEmptyView(emptyView);
            if (emptyView != null) {
                emptyView.setText(R.string.development_settings_not_available);
            }
            return;
        }
        boolean isChecked;
        if (this.mDpm.getMaximumTimeToLock(null) > 0) {
            this.mDisabledPrefs.add(this.mKeepScreenOn);
        } else {
            this.mDisabledPrefs.remove(this.mKeepScreenOn);
        }
        if (Global.getInt(getActivity().getContentResolver(), "development_settings_enabled", 0) != 0) {
            z = true;
        }
        this.mLastEnabledState = z;
        if (this.mEnableDialog == null || !this.mEnableDialog.isShowing()) {
            isChecked = this.mLastEnabledState;
        } else {
            isChecked = true;
        }
        this.mSwitchBar.setChecked(isChecked);
        setPrefsEnabledState(this.mLastEnabledState);
        this.mSwitchBar.show();
        this.mExt.customUSBPreference(this.mEnableAdb);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.hardware.usb.action.USB_STATE");
        getActivity().registerReceiver(this.mUsbReceiver, filter);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public void onDestroyView() {
        super.onDestroyView();
        if (!this.mUnavailable) {
            this.mSwitchBar.removeOnSwitchChangeListener(this);
            this.mSwitchBar.hide();
            getActivity().unregisterReceiver(this.mUsbReceiver);
        }
    }

    void updateSwitchPreference(SwitchPreference switchPreference, boolean value) {
        switchPreference.setChecked(value);
        this.mHaveDebugSettings |= value;
    }

    private void updateAllOptions() {
        SwitchPreference switchPreference;
        boolean z;
        boolean z2 = true;
        Context context = getActivity();
        ContentResolver cr = context.getContentResolver();
        this.mHaveDebugSettings = false;
        boolean isChecked = (this.mAdbDialog == null || !this.mAdbDialog.isShowing()) ? Global.getInt(cr, "adb_enabled", 0) != 0 : true;
        updateSwitchPreference(this.mEnableAdb, isChecked);
        this.mExt.customUSBPreference(this.mEnableAdb);
        if (this.mEnableTerminal != null) {
            switchPreference = this.mEnableTerminal;
            if (context.getPackageManager().getApplicationEnabledSetting("com.android.terminal") == 1) {
                z = true;
            } else {
                z = false;
            }
            updateSwitchPreference(switchPreference, z);
        }
        switchPreference = this.mBugreportInPower;
        if (Secure.getInt(cr, "bugreport_in_power_menu", 0) != 0) {
            z = true;
        } else {
            z = false;
        }
        updateSwitchPreference(switchPreference, z);
        switchPreference = this.mKeepScreenOn;
        if (Global.getInt(cr, "stay_on_while_plugged_in", 0) != 0) {
            z = true;
        } else {
            z = false;
        }
        updateSwitchPreference(switchPreference, z);
        switchPreference = this.mBtHciSnoopLog;
        if (Secure.getInt(cr, "bluetooth_hci_log", 0) != 0) {
            z = true;
        } else {
            z = false;
        }
        updateSwitchPreference(switchPreference, z);
        if (this.mEnableOemUnlock != null) {
            updateSwitchPreference(this.mEnableOemUnlock, Utils.isOemUnlockEnabled(getActivity()));
        }
        SwitchPreference switchPreference2 = this.mDebugViewAttributes;
        if (Global.getInt(cr, "debug_view_attributes", 0) == 0) {
            z2 = false;
        }
        updateSwitchPreference(switchPreference2, z2);
        updateHdcpValues();
        updatePasswordSummary();
        updateDebuggerOptions();
        updateMockLocation();
        updateStrictModeVisualOptions();
        updatePointerLocationOptions();
        updateShowTouchesOptions();
        updateFlingerOptions();
        updateCpuUsageOptions();
        updateHardwareUiOptions();
        updateMsaaOptions();
        updateTrackFrameTimeOptions();
        updateShowNonRectClipOptions();
        updateShowHwScreenUpdatesOptions();
        updateShowHwLayersUpdatesOptions();
        updateDebugHwOverdrawOptions();
        updateDebugLayoutOptions();
        updateAnimationScaleOptions();
        updateOverlayDisplayDevicesOptions();
        if (this.mEnableMultiWindow != null) {
            updateSwitchPreference(this.mEnableMultiWindow, SystemProperties.getBoolean("persist.sys.debug.multi_window", false));
        }
        updateOpenGLTracesOptions();
        updateImmediatelyDestroyActivitiesOptions();
        updateAppProcessLimitOptions();
        updateShowAllANRsOptions();
        updateVerifyAppsOverUsbOptions();
        updateBugreportOptions();
        updateForceRtlOptions();
        updateLogdSizeValues();
        updateWifiDisplayCertificationOptions();
        updateWifiVerboseLoggingOptions();
        updateWifiAggressiveHandoverOptions();
        updateWifiAllowScansWithTrafficOptions();
        updateLegacyDhcpClientOptions();
        updateMobileDataAlwaysOnOptions();
        updateSimulateColorSpace();
        updateUSBAudioOptions();
    }

    private void resetDangerousOptions() {
        this.mDontPokeProperties = true;
        for (int i = 0; i < this.mResetSwitchPrefs.size(); i++) {
            SwitchPreference cb = (SwitchPreference) this.mResetSwitchPrefs.get(i);
            if (cb.isChecked()) {
                cb.setChecked(false);
                onPreferenceTreeClick(null, cb);
            }
        }
        resetDebuggerOptions();
        writeLogdSizeOption(null);
        writeAnimationScaleOption(0, this.mWindowAnimationScale, null);
        writeAnimationScaleOption(1, this.mTransitionAnimationScale, null);
        writeAnimationScaleOption(2, this.mAnimatorDurationScale, null);
        if (usingDevelopmentColorSpace()) {
            writeSimulateColorSpace(Integer.valueOf(-1));
        }
        writeOverlayDisplayDevicesOptions(null);
        writeAppProcessLimitOptions(null);
        this.mHaveDebugSettings = false;
        updateAllOptions();
        this.mDontPokeProperties = false;
        pokeSystemProperties();
    }

    private void updateHdcpValues() {
        ListPreference hdcpChecking = (ListPreference) findPreference("hdcp_checking");
        if (hdcpChecking != null) {
            String currentValue = SystemProperties.get("persist.sys.hdcp_checking");
            String[] values = getResources().getStringArray(R.array.hdcp_checking_values);
            String[] summaries = getResources().getStringArray(R.array.hdcp_checking_summaries);
            int index = 1;
            for (int i = 0; i < values.length; i++) {
                if (currentValue.equals(values[i])) {
                    index = i;
                    break;
                }
            }
            hdcpChecking.setValue(values[index]);
            hdcpChecking.setSummary(summaries[index]);
            hdcpChecking.setOnPreferenceChangeListener(this);
        }
    }

    private void updatePasswordSummary() {
        try {
            if (this.mBackupManager.hasBackupPassword()) {
                this.mPassword.setSummary(R.string.local_backup_password_summary_change);
            } else {
                this.mPassword.setSummary(R.string.local_backup_password_summary_none);
            }
        } catch (RemoteException e) {
        }
    }

    private void writeBtHciSnoopLogOptions() {
        BluetoothAdapter.getDefaultAdapter().configHciSnoopLog(this.mBtHciSnoopLog.isChecked());
        Secure.putInt(getActivity().getContentResolver(), "bluetooth_hci_log", this.mBtHciSnoopLog.isChecked() ? 1 : 0);
    }

    private void writeDebuggerOptions() {
        try {
            ActivityManagerNative.getDefault().setDebugApp(this.mDebugApp, this.mWaitForDebugger.isChecked(), true);
        } catch (RemoteException e) {
        }
    }

    private void writeMockLocation() {
        AppOpsManager appOpsManager = (AppOpsManager) getSystemService("appops");
        List<PackageOps> packageOps = appOpsManager.getPackagesForOps(MOCK_LOCATION_APP_OPS);
        if (packageOps != null) {
            for (PackageOps packageOp : packageOps) {
                if (((OpEntry) packageOp.getOps().get(0)).getMode() != 2) {
                    String oldMockLocationApp = packageOp.getPackageName();
                    try {
                        appOpsManager.setMode(58, getActivity().getPackageManager().getApplicationInfo(oldMockLocationApp, 512).uid, oldMockLocationApp, 2);
                    } catch (NameNotFoundException e) {
                    }
                }
            }
        }
        if (!TextUtils.isEmpty(this.mMockLocationApp)) {
            try {
                appOpsManager.setMode(58, getActivity().getPackageManager().getApplicationInfo(this.mMockLocationApp, 512).uid, this.mMockLocationApp, 0);
            } catch (NameNotFoundException e2) {
            }
        }
    }

    private static void resetDebuggerOptions() {
        try {
            ActivityManagerNative.getDefault().setDebugApp(null, false, true);
        } catch (RemoteException e) {
        }
    }

    private void updateDebuggerOptions() {
        boolean z;
        this.mDebugApp = Global.getString(getActivity().getContentResolver(), "debug_app");
        SwitchPreference switchPreference = this.mWaitForDebugger;
        if (Global.getInt(getActivity().getContentResolver(), "wait_for_debugger", 0) != 0) {
            z = true;
        } else {
            z = false;
        }
        updateSwitchPreference(switchPreference, z);
        if (this.mDebugApp == null || this.mDebugApp.length() <= 0) {
            this.mDebugAppPref.setSummary(getResources().getString(R.string.debug_app_not_set));
            this.mWaitForDebugger.setEnabled(false);
            return;
        }
        String label;
        try {
            CharSequence lab = getActivity().getPackageManager().getApplicationLabel(getActivity().getPackageManager().getApplicationInfo(this.mDebugApp, 512));
            label = lab != null ? lab.toString() : this.mDebugApp;
        } catch (NameNotFoundException e) {
            label = this.mDebugApp;
        }
        this.mDebugAppPref.setSummary(getResources().getString(R.string.debug_app_set, new Object[]{label}));
        this.mWaitForDebugger.setEnabled(true);
        this.mHaveDebugSettings = true;
    }

    private void updateMockLocation() {
        List<PackageOps> packageOps = ((AppOpsManager) getSystemService("appops")).getPackagesForOps(MOCK_LOCATION_APP_OPS);
        if (packageOps != null) {
            for (PackageOps packageOp : packageOps) {
                if (((OpEntry) packageOp.getOps().get(0)).getMode() == 0) {
                    this.mMockLocationApp = ((PackageOps) packageOps.get(0)).getPackageName();
                    break;
                }
            }
        }
        if (TextUtils.isEmpty(this.mMockLocationApp)) {
            this.mMockLocationAppPref.setSummary(getString(R.string.mock_location_app_not_set));
            return;
        }
        String label = this.mMockLocationApp;
        try {
            CharSequence appLabel = getPackageManager().getApplicationLabel(getActivity().getPackageManager().getApplicationInfo(this.mMockLocationApp, 512));
            if (appLabel != null) {
                label = appLabel.toString();
            }
        } catch (NameNotFoundException e) {
        }
        this.mMockLocationAppPref.setSummary(getString(R.string.mock_location_app_set, new Object[]{label}));
        this.mHaveDebugSettings = true;
    }

    private void updateVerifyAppsOverUsbOptions() {
        boolean z = true;
        SwitchPreference switchPreference = this.mVerifyAppsOverUsb;
        if (Global.getInt(getActivity().getContentResolver(), "verifier_verify_adb_installs", 1) == 0) {
            z = false;
        }
        updateSwitchPreference(switchPreference, z);
        this.mVerifyAppsOverUsb.setEnabled(enableVerifierSetting());
    }

    private void writeVerifyAppsOverUsbOptions() {
        Global.putInt(getActivity().getContentResolver(), "verifier_verify_adb_installs", this.mVerifyAppsOverUsb.isChecked() ? 1 : 0);
    }

    private boolean enableVerifierSetting() {
        ContentResolver cr = getActivity().getContentResolver();
        if (Global.getInt(cr, "adb_enabled", 0) == 0 || Global.getInt(cr, "package_verifier_enable", 1) == 0) {
            return false;
        }
        PackageManager pm = getActivity().getPackageManager();
        Intent verification = new Intent("android.intent.action.PACKAGE_NEEDS_VERIFICATION");
        verification.setType("application/vnd.android.package-archive");
        verification.addFlags(1);
        return pm.queryBroadcastReceivers(verification, 0).size() != 0;
    }

    private boolean showVerifierSetting() {
        return Global.getInt(getActivity().getContentResolver(), "verifier_setting_visible", 1) > 0;
    }

    private static boolean showEnableOemUnlockPreference() {
        return !SystemProperties.get("ro.frp.pst").equals("");
    }

    private static boolean showEnableMultiWindowPreference() {
        return !"user".equals(Build.TYPE);
    }

    private void setEnableMultiWindow(boolean value) {
        SystemProperties.set("persist.sys.debug.multi_window", String.valueOf(value));
        pokeSystemProperties();
    }

    private void updateBugreportOptions() {
        ComponentName bugreportStorageProviderComponentName = new ComponentName("com.android.shell", "com.android.shell.BugreportStorageProvider");
        if ("user".equals(Build.TYPE)) {
            ContentResolver resolver = getActivity().getContentResolver();
            if (Global.getInt(resolver, "adb_enabled", 0) != 0) {
                this.mBugreport.setEnabled(true);
                this.mBugreportInPower.setEnabled(true);
                getPackageManager().setComponentEnabledSetting(bugreportStorageProviderComponentName, 1, 0);
                return;
            }
            this.mBugreport.setEnabled(false);
            this.mBugreportInPower.setEnabled(false);
            this.mBugreportInPower.setChecked(false);
            Secure.putInt(resolver, "bugreport_in_power_menu", 0);
            getPackageManager().setComponentEnabledSetting(bugreportStorageProviderComponentName, 0, 0);
            return;
        }
        this.mBugreportInPower.setEnabled(true);
        getPackageManager().setComponentEnabledSetting(bugreportStorageProviderComponentName, 1, 0);
    }

    private static int currentStrictModeActiveIndex() {
        if (TextUtils.isEmpty(SystemProperties.get("persist.sys.strictmode.visual"))) {
            return 0;
        }
        return SystemProperties.getBoolean("persist.sys.strictmode.visual", false) ? 1 : 2;
    }

    private void writeStrictModeVisualOptions() {
        try {
            this.mWindowManager.setStrictModeVisualIndicatorPreference(this.mStrictMode.isChecked() ? "1" : "");
        } catch (RemoteException e) {
        }
    }

    private void updateStrictModeVisualOptions() {
        boolean z = true;
        SwitchPreference switchPreference = this.mStrictMode;
        if (currentStrictModeActiveIndex() != 1) {
            z = false;
        }
        updateSwitchPreference(switchPreference, z);
    }

    private void writePointerLocationOptions() {
        System.putInt(getActivity().getContentResolver(), "pointer_location", this.mPointerLocation.isChecked() ? 1 : 0);
    }

    private void updatePointerLocationOptions() {
        boolean z = false;
        SwitchPreference switchPreference = this.mPointerLocation;
        if (System.getInt(getActivity().getContentResolver(), "pointer_location", 0) != 0) {
            z = true;
        }
        updateSwitchPreference(switchPreference, z);
    }

    private void writeShowTouchesOptions() {
        System.putInt(getActivity().getContentResolver(), "show_touches", this.mShowTouches.isChecked() ? 1 : 0);
    }

    private void updateShowTouchesOptions() {
        boolean z = false;
        SwitchPreference switchPreference = this.mShowTouches;
        if (System.getInt(getActivity().getContentResolver(), "show_touches", 0) != 0) {
            z = true;
        }
        updateSwitchPreference(switchPreference, z);
    }

    private void updateFlingerOptions() {
        boolean z = true;
        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger != null) {
                boolean z2;
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                flinger.transact(1010, data, reply, 0);
                int showCpu = reply.readInt();
                int enableGL = reply.readInt();
                int showUpdates = reply.readInt();
                SwitchPreference switchPreference = this.mShowScreenUpdates;
                if (showUpdates != 0) {
                    z2 = true;
                } else {
                    z2 = false;
                }
                updateSwitchPreference(switchPreference, z2);
                int showBackground = reply.readInt();
                int disableOverlays = reply.readInt();
                SwitchPreference switchPreference2 = this.mDisableOverlays;
                if (disableOverlays == 0) {
                    z = false;
                }
                updateSwitchPreference(switchPreference2, z);
                reply.recycle();
                data.recycle();
            }
        } catch (RemoteException e) {
        }
    }

    private void writeShowUpdatesOption() {
        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger != null) {
                Parcel data = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                data.writeInt(this.mShowScreenUpdates.isChecked() ? 1 : 0);
                flinger.transact(1002, data, null, 0);
                data.recycle();
                updateFlingerOptions();
            }
        } catch (RemoteException e) {
        }
    }

    private void writeDisableOverlaysOption() {
        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger != null) {
                Parcel data = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                data.writeInt(this.mDisableOverlays.isChecked() ? 1 : 0);
                flinger.transact(1008, data, null, 0);
                data.recycle();
                updateFlingerOptions();
            }
        } catch (RemoteException e) {
        }
    }

    private void updateHardwareUiOptions() {
        updateSwitchPreference(this.mForceHardwareUi, SystemProperties.getBoolean("persist.sys.ui.hw", false));
    }

    private void writeHardwareUiOptions() {
        SystemProperties.set("persist.sys.ui.hw", this.mForceHardwareUi.isChecked() ? "true" : "false");
        pokeSystemProperties();
    }

    private void updateMsaaOptions() {
        updateSwitchPreference(this.mForceMsaa, SystemProperties.getBoolean("debug.egl.force_msaa", false));
    }

    private void writeMsaaOptions() {
        SystemProperties.set("debug.egl.force_msaa", this.mForceMsaa.isChecked() ? "true" : "false");
        pokeSystemProperties();
    }

    private void updateTrackFrameTimeOptions() {
        String value = SystemProperties.get("debug.hwui.profile");
        if (value == null) {
            value = "";
        }
        CharSequence[] values = this.mTrackFrameTime.getEntryValues();
        for (int i = 0; i < values.length; i++) {
            if (value.contentEquals(values[i])) {
                this.mTrackFrameTime.setValueIndex(i);
                this.mTrackFrameTime.setSummary(this.mTrackFrameTime.getEntries()[i]);
                return;
            }
        }
        this.mTrackFrameTime.setValueIndex(0);
        this.mTrackFrameTime.setSummary(this.mTrackFrameTime.getEntries()[0]);
    }

    private void writeTrackFrameTimeOptions(Object newValue) {
        SystemProperties.set("debug.hwui.profile", newValue == null ? "" : newValue.toString());
        pokeSystemProperties();
        updateTrackFrameTimeOptions();
    }

    private void updateShowNonRectClipOptions() {
        String value = SystemProperties.get("debug.hwui.show_non_rect_clip");
        if (value == null) {
            value = "hide";
        }
        CharSequence[] values = this.mShowNonRectClip.getEntryValues();
        for (int i = 0; i < values.length; i++) {
            if (value.contentEquals(values[i])) {
                this.mShowNonRectClip.setValueIndex(i);
                this.mShowNonRectClip.setSummary(this.mShowNonRectClip.getEntries()[i]);
                return;
            }
        }
        this.mShowNonRectClip.setValueIndex(0);
        this.mShowNonRectClip.setSummary(this.mShowNonRectClip.getEntries()[0]);
    }

    private void writeShowNonRectClipOptions(Object newValue) {
        SystemProperties.set("debug.hwui.show_non_rect_clip", newValue == null ? "" : newValue.toString());
        pokeSystemProperties();
        updateShowNonRectClipOptions();
    }

    private void updateShowHwScreenUpdatesOptions() {
        updateSwitchPreference(this.mShowHwScreenUpdates, SystemProperties.getBoolean("debug.hwui.show_dirty_regions", false));
    }

    private void writeShowHwScreenUpdatesOptions() {
        SystemProperties.set("debug.hwui.show_dirty_regions", this.mShowHwScreenUpdates.isChecked() ? "true" : null);
        pokeSystemProperties();
    }

    private void updateShowHwLayersUpdatesOptions() {
        updateSwitchPreference(this.mShowHwLayersUpdates, SystemProperties.getBoolean("debug.hwui.show_layers_updates", false));
    }

    private void writeShowHwLayersUpdatesOptions() {
        SystemProperties.set("debug.hwui.show_layers_updates", this.mShowHwLayersUpdates.isChecked() ? "true" : null);
        pokeSystemProperties();
    }

    private void updateDebugHwOverdrawOptions() {
        String value = SystemProperties.get("debug.hwui.overdraw");
        if (value == null) {
            value = "";
        }
        CharSequence[] values = this.mDebugHwOverdraw.getEntryValues();
        for (int i = 0; i < values.length; i++) {
            if (value.contentEquals(values[i])) {
                this.mDebugHwOverdraw.setValueIndex(i);
                this.mDebugHwOverdraw.setSummary(this.mDebugHwOverdraw.getEntries()[i]);
                return;
            }
        }
        this.mDebugHwOverdraw.setValueIndex(0);
        this.mDebugHwOverdraw.setSummary(this.mDebugHwOverdraw.getEntries()[0]);
    }

    private void writeDebugHwOverdrawOptions(Object newValue) {
        SystemProperties.set("debug.hwui.overdraw", newValue == null ? "" : newValue.toString());
        pokeSystemProperties();
        updateDebugHwOverdrawOptions();
    }

    private void updateDebugLayoutOptions() {
        updateSwitchPreference(this.mDebugLayout, SystemProperties.getBoolean("debug.layout", false));
    }

    private void writeDebugLayoutOptions() {
        SystemProperties.set("debug.layout", this.mDebugLayout.isChecked() ? "true" : "false");
        pokeSystemProperties();
    }

    private void updateSimulateColorSpace() {
        ContentResolver cr = getContentResolver();
        if (Secure.getInt(cr, "accessibility_display_daltonizer_enabled", 0) != 0) {
            String mode = Integer.toString(Secure.getInt(cr, "accessibility_display_daltonizer", -1));
            this.mSimulateColorSpace.setValue(mode);
            if (this.mSimulateColorSpace.findIndexOfValue(mode) < 0) {
                this.mSimulateColorSpace.setSummary(getString(R.string.daltonizer_type_overridden, new Object[]{getString(R.string.accessibility_display_daltonizer_preference_title)}));
                return;
            } else {
                this.mSimulateColorSpace.setSummary("%s");
                return;
            }
        }
        this.mSimulateColorSpace.setValue(Integer.toString(-1));
    }

    private boolean usingDevelopmentColorSpace() {
        ContentResolver cr = getContentResolver();
        if (Secure.getInt(cr, "accessibility_display_daltonizer_enabled", 0) != 0) {
            if (this.mSimulateColorSpace.findIndexOfValue(Integer.toString(Secure.getInt(cr, "accessibility_display_daltonizer", -1))) >= 0) {
                return true;
            }
        }
        return false;
    }

    private void writeSimulateColorSpace(Object value) {
        ContentResolver cr = getContentResolver();
        int newMode = Integer.parseInt(value.toString());
        if (newMode < 0) {
            Secure.putInt(cr, "accessibility_display_daltonizer_enabled", 0);
            return;
        }
        Secure.putInt(cr, "accessibility_display_daltonizer_enabled", 1);
        Secure.putInt(cr, "accessibility_display_daltonizer", newMode);
    }

    private void updateUSBAudioOptions() {
        boolean z = false;
        SwitchPreference switchPreference = this.mUSBAudio;
        if (Secure.getInt(getContentResolver(), "usb_audio_automatic_routing_disabled", 0) != 0) {
            z = true;
        }
        updateSwitchPreference(switchPreference, z);
    }

    private void writeUSBAudioOptions() {
        Secure.putInt(getContentResolver(), "usb_audio_automatic_routing_disabled", this.mUSBAudio.isChecked() ? 1 : 0);
    }

    private void updateForceRtlOptions() {
        boolean z = false;
        SwitchPreference switchPreference = this.mForceRtlLayout;
        if (Global.getInt(getActivity().getContentResolver(), "debug.force_rtl", 0) != 0) {
            z = true;
        }
        updateSwitchPreference(switchPreference, z);
    }

    private void writeForceRtlOptions() {
        boolean value = this.mForceRtlLayout.isChecked();
        Global.putInt(getActivity().getContentResolver(), "debug.force_rtl", value ? 1 : 0);
        SystemProperties.set("debug.force_rtl", value ? "1" : "0");
        LocalePicker.updateLocale(getActivity().getResources().getConfiguration().locale);
    }

    private void updateWifiDisplayCertificationOptions() {
        boolean z = false;
        SwitchPreference switchPreference = this.mWifiDisplayCertification;
        if (Global.getInt(getActivity().getContentResolver(), "wifi_display_certification_on", 0) != 0) {
            z = true;
        }
        updateSwitchPreference(switchPreference, z);
    }

    private void writeWifiDisplayCertificationOptions() {
        Global.putInt(getActivity().getContentResolver(), "wifi_display_certification_on", this.mWifiDisplayCertification.isChecked() ? 1 : 0);
    }

    private void updateWifiVerboseLoggingOptions() {
        updateSwitchPreference(this.mWifiVerboseLogging, this.mWifiManager.getVerboseLoggingLevel() > 0);
    }

    private void writeWifiVerboseLoggingOptions() {
        this.mWifiManager.enableVerboseLogging(this.mWifiVerboseLogging.isChecked() ? 1 : 0);
    }

    private void updateWifiAggressiveHandoverOptions() {
        updateSwitchPreference(this.mWifiAggressiveHandover, this.mWifiManager.getAggressiveHandover() > 0);
    }

    private void writeWifiAggressiveHandoverOptions() {
        this.mWifiManager.enableAggressiveHandover(this.mWifiAggressiveHandover.isChecked() ? 1 : 0);
    }

    private void updateWifiAllowScansWithTrafficOptions() {
        updateSwitchPreference(this.mWifiAllowScansWithTraffic, this.mWifiManager.getAllowScansWithTraffic() > 0);
    }

    private void writeWifiAllowScansWithTrafficOptions() {
        this.mWifiManager.setAllowScansWithTraffic(this.mWifiAllowScansWithTraffic.isChecked() ? 1 : 0);
    }

    private void updateLegacyDhcpClientOptions() {
        boolean z = false;
        SwitchPreference switchPreference = this.mLegacyDhcpClient;
        if (Global.getInt(getActivity().getContentResolver(), "legacy_dhcp_client", 0) != 0) {
            z = true;
        }
        updateSwitchPreference(switchPreference, z);
    }

    private void writeLegacyDhcpClientOptions() {
        Global.putInt(getActivity().getContentResolver(), "legacy_dhcp_client", this.mLegacyDhcpClient.isChecked() ? 1 : 0);
    }

    private void updateMobileDataAlwaysOnOptions() {
        boolean z = false;
        SwitchPreference switchPreference = this.mMobileDataAlwaysOn;
        if (Global.getInt(getActivity().getContentResolver(), "mobile_data_always_on", 0) != 0) {
            z = true;
        }
        updateSwitchPreference(switchPreference, z);
    }

    private void writeMobileDataAlwaysOnOptions() {
        Global.putInt(getActivity().getContentResolver(), "mobile_data_always_on", this.mMobileDataAlwaysOn.isChecked() ? 1 : 0);
    }

    private void updateLogdSizeValues() {
        if (this.mLogdSize != null) {
            String currentValue = SystemProperties.get("persist.logd.size");
            if (currentValue == null) {
                currentValue = SystemProperties.get("ro.logd.size");
                if (currentValue == null) {
                    currentValue = "256K";
                }
            }
            String[] values = getResources().getStringArray(R.array.select_logd_size_values);
            String[] titles = getResources().getStringArray(R.array.select_logd_size_titles);
            if (SystemProperties.get("ro.config.low_ram").equals("true")) {
                this.mLogdSize.setEntries(R.array.select_logd_size_lowram_titles);
                titles = getResources().getStringArray(R.array.select_logd_size_lowram_titles);
            }
            String[] summaries = getResources().getStringArray(R.array.select_logd_size_summaries);
            int index = 1;
            int i = 0;
            while (i < titles.length) {
                if (currentValue.equals(values[i]) || currentValue.equals(titles[i])) {
                    index = i;
                    break;
                }
                i++;
            }
            this.mLogdSize.setValue(values[index]);
            this.mLogdSize.setSummary(summaries[index]);
            this.mLogdSize.setOnPreferenceChangeListener(this);
        }
    }

    private void writeLogdSizeOption(Object newValue) {
        String currentValue = SystemProperties.get("ro.logd.size");
        if (currentValue != null) {
            DEFAULT_LOG_RING_BUFFER_SIZE_IN_BYTES = currentValue;
        }
        String size = newValue != null ? newValue.toString() : DEFAULT_LOG_RING_BUFFER_SIZE_IN_BYTES;
        SystemProperties.set("persist.logd.size", size);
        pokeSystemProperties();
        try {
            Runtime.getRuntime().exec("logcat -b all -G " + size).waitFor();
            Log.i("DevelopmentSettings", "Logcat ring buffer sizes set to: " + size);
        } catch (Exception e) {
            Log.w("DevelopmentSettings", "Cannot set logcat ring buffer sizes", e);
        }
        updateLogdSizeValues();
    }

    private void updateUsbConfigurationValues() {
        if (this.mUsbConfiguration != null) {
            UsbManager manager = (UsbManager) getSystemService("usb");
            String[] values = getResources().getStringArray(R.array.usb_configuration_values);
            String[] titles = getResources().getStringArray(R.array.usb_configuration_titles);
            int index = 0;
            for (int i = 0; i < titles.length; i++) {
                if (manager.isFunctionEnabled(values[i])) {
                    index = i;
                    break;
                }
            }
            this.mUsbConfiguration.setValue(values[index]);
            this.mUsbConfiguration.setSummary(titles[index]);
            this.mUsbConfiguration.setOnPreferenceChangeListener(this);
        }
    }

    private void writeUsbConfigurationOption(Object newValue) {
        UsbManager manager = (UsbManager) getActivity().getSystemService("usb");
        String function = newValue.toString();
        manager.setCurrentFunction(function);
        if (function.equals("none")) {
            manager.setUsbDataUnlocked(false);
        } else {
            manager.setUsbDataUnlocked(true);
        }
    }

    private void updateCpuUsageOptions() {
        boolean z = false;
        SwitchPreference switchPreference = this.mShowCpuUsage;
        if (Global.getInt(getActivity().getContentResolver(), "show_processes", 0) != 0) {
            z = true;
        }
        updateSwitchPreference(switchPreference, z);
    }

    private void writeCpuUsageOptions() {
        boolean value = this.mShowCpuUsage.isChecked();
        Global.putInt(getActivity().getContentResolver(), "show_processes", value ? 1 : 0);
        Intent service = new Intent().setClassName("com.android.systemui", "com.android.systemui.LoadAverageService");
        if (value) {
            getActivity().startService(service);
        } else {
            getActivity().stopService(service);
        }
    }

    private void writeImmediatelyDestroyActivitiesOptions() {
        try {
            ActivityManagerNative.getDefault().setAlwaysFinish(this.mImmediatelyDestroyActivities.isChecked());
        } catch (RemoteException e) {
        }
    }

    private void updateImmediatelyDestroyActivitiesOptions() {
        boolean z = false;
        SwitchPreference switchPreference = this.mImmediatelyDestroyActivities;
        if (Global.getInt(getActivity().getContentResolver(), "always_finish_activities", 0) != 0) {
            z = true;
        }
        updateSwitchPreference(switchPreference, z);
    }

    private void updateAnimationScaleValue(int which, ListPreference pref) {
        try {
            float scale = this.mWindowManager.getAnimationScale(which);
            if (scale != 1.0f) {
                this.mHaveDebugSettings = true;
            }
            CharSequence[] values = pref.getEntryValues();
            for (int i = 0; i < values.length; i++) {
                if (scale <= Float.parseFloat(values[i].toString())) {
                    pref.setValueIndex(i);
                    pref.setSummary(pref.getEntries()[i]);
                    return;
                }
            }
            pref.setValueIndex(values.length - 1);
            pref.setSummary(pref.getEntries()[0]);
        } catch (RemoteException e) {
        }
    }

    private void updateAnimationScaleOptions() {
        updateAnimationScaleValue(0, this.mWindowAnimationScale);
        updateAnimationScaleValue(1, this.mTransitionAnimationScale);
        updateAnimationScaleValue(2, this.mAnimatorDurationScale);
    }

    private void writeAnimationScaleOption(int which, ListPreference pref, Object newValue) {
        float scale;
        if (newValue != null) {
            try {
                scale = Float.parseFloat(newValue.toString());
            } catch (RemoteException e) {
                return;
            }
        }
        scale = 1.0f;
        this.mWindowManager.setAnimationScale(which, scale);
        updateAnimationScaleValue(which, pref);
    }

    private void updateOverlayDisplayDevicesOptions() {
        String value = Global.getString(getActivity().getContentResolver(), "overlay_display_devices");
        if (value == null) {
            value = "";
        }
        CharSequence[] values = this.mOverlayDisplayDevices.getEntryValues();
        for (int i = 0; i < values.length; i++) {
            if (value.contentEquals(values[i])) {
                this.mOverlayDisplayDevices.setValueIndex(i);
                this.mOverlayDisplayDevices.setSummary(this.mOverlayDisplayDevices.getEntries()[i]);
                return;
            }
        }
        this.mOverlayDisplayDevices.setValueIndex(0);
        this.mOverlayDisplayDevices.setSummary(this.mOverlayDisplayDevices.getEntries()[0]);
    }

    private void writeOverlayDisplayDevicesOptions(Object newValue) {
        Global.putString(getActivity().getContentResolver(), "overlay_display_devices", (String) newValue);
        updateOverlayDisplayDevicesOptions();
    }

    private void updateOpenGLTracesOptions() {
        String value = SystemProperties.get("debug.egl.trace");
        if (value == null) {
            value = "";
        }
        CharSequence[] values = this.mOpenGLTraces.getEntryValues();
        for (int i = 0; i < values.length; i++) {
            if (value.contentEquals(values[i])) {
                this.mOpenGLTraces.setValueIndex(i);
                this.mOpenGLTraces.setSummary(this.mOpenGLTraces.getEntries()[i]);
                return;
            }
        }
        this.mOpenGLTraces.setValueIndex(0);
        this.mOpenGLTraces.setSummary(this.mOpenGLTraces.getEntries()[0]);
    }

    private void writeOpenGLTracesOptions(Object newValue) {
        SystemProperties.set("debug.egl.trace", newValue == null ? "" : newValue.toString());
        pokeSystemProperties();
        updateOpenGLTracesOptions();
    }

    private void updateAppProcessLimitOptions() {
        try {
            int limit = ActivityManagerNative.getDefault().getProcessLimit();
            CharSequence[] values = this.mAppProcessLimit.getEntryValues();
            for (int i = 0; i < values.length; i++) {
                if (Integer.parseInt(values[i].toString()) >= limit) {
                    if (i != 0) {
                        this.mHaveDebugSettings = true;
                    }
                    this.mAppProcessLimit.setValueIndex(i);
                    this.mAppProcessLimit.setSummary(this.mAppProcessLimit.getEntries()[i]);
                    return;
                }
            }
            this.mAppProcessLimit.setValueIndex(0);
            this.mAppProcessLimit.setSummary(this.mAppProcessLimit.getEntries()[0]);
        } catch (RemoteException e) {
        }
    }

    private void writeAppProcessLimitOptions(Object newValue) {
        int limit;
        if (newValue != null) {
            try {
                limit = Integer.parseInt(newValue.toString());
            } catch (RemoteException e) {
                return;
            }
        }
        limit = -1;
        ActivityManagerNative.getDefault().setProcessLimit(limit);
        updateAppProcessLimitOptions();
    }

    private void writeShowAllANRsOptions() {
        Secure.putInt(getActivity().getContentResolver(), "anr_show_background", this.mShowAllANRs.isChecked() ? 1 : 0);
    }

    private void updateShowAllANRsOptions() {
        boolean z = false;
        SwitchPreference switchPreference = this.mShowAllANRs;
        if (Secure.getInt(getActivity().getContentResolver(), "anr_show_background", 0) != 0) {
            z = true;
        }
        updateSwitchPreference(switchPreference, z);
    }

    private void confirmEnableOemUnlock() {
        OnClickListener onConfirmListener = new C01123();
        new Builder(getActivity()).setTitle(R.string.confirm_enable_oem_unlock_title).setMessage(R.string.confirm_enable_oem_unlock_text).setPositiveButton(R.string.enable_text, onConfirmListener).setNegativeButton(17039360, onConfirmListener).create().show();
    }

    private void confirmEnableMultiWindowMode() {
        OnClickListener onConfirmListener = new C01134();
        new Builder(getActivity()).setTitle(R.string.confirm_enable_multi_window_title).setMessage(R.string.confirm_enable_multi_window_text).setPositiveButton(R.string.enable_text, onConfirmListener).setNegativeButton(17039360, onConfirmListener).create().show();
    }

    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (switchView == this.mSwitchBar.getSwitch() && isChecked != this.mLastEnabledState) {
            if (isChecked) {
                this.mDialogClicked = false;
                if (this.mEnableDialog != null) {
                    dismissDialogs();
                }
                this.mEnableDialog = new Builder(getActivity()).setMessage(getActivity().getResources().getString(R.string.dev_settings_warning_message)).setTitle(R.string.dev_settings_warning_title).setPositiveButton(17039379, this).setNegativeButton(17039369, this).show();
                this.mEnableDialog.setOnDismissListener(this);
            } else {
                resetDangerousOptions();
                Global.putInt(getActivity().getContentResolver(), "development_settings_enabled", 0);
                this.mLastEnabledState = isChecked;
                setPrefsEnabledState(this.mLastEnabledState);
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1000) {
            if (resultCode == -1) {
                this.mDebugApp = data.getAction();
                writeDebuggerOptions();
                updateDebuggerOptions();
            }
        } else if (requestCode == 1001) {
            if (resultCode == -1) {
                this.mMockLocationApp = data.getAction();
                writeMockLocation();
                updateMockLocation();
            }
        } else if (requestCode != 0) {
            super.onActivityResult(requestCode, resultCode, data);
        } else if (resultCode != -1) {
        } else {
            if (this.mEnableOemUnlock.isChecked()) {
                confirmEnableOemUnlock();
            } else {
                Utils.setOemUnlockEnabled(getActivity(), false);
            }
        }
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        int i = 1;
        if (Utils.isMonkeyRunning()) {
            return false;
        }
        if (preference == this.mEnableAdb) {
            if (this.mEnableAdb.isChecked()) {
                this.mDialogClicked = false;
                if (this.mAdbDialog != null) {
                    dismissDialogs();
                }
                this.mAdbDialog = new Builder(getActivity()).setMessage(getActivity().getResources().getString(R.string.adb_warning_message)).setTitle(R.string.adb_warning_title).setPositiveButton(17039379, this).setNegativeButton(17039369, this).show();
                this.mAdbDialog.setOnDismissListener(this);
            } else {
                Global.putInt(getActivity().getContentResolver(), "adb_enabled", 0);
                this.mVerifyAppsOverUsb.setEnabled(false);
                this.mVerifyAppsOverUsb.setChecked(false);
                onPreferenceTreeClick(null, this.mVerifyAppsOverUsb);
                updateBugreportOptions();
            }
        } else if (preference == this.mClearAdbKeys) {
            if (this.mAdbKeysDialog != null) {
                dismissDialogs();
            }
            this.mAdbKeysDialog = new Builder(getActivity()).setMessage(R.string.adb_keys_warning_message).setPositiveButton(17039370, this).setNegativeButton(17039360, null).show();
        } else if (preference == this.mEnableTerminal) {
            PackageManager pm = getActivity().getPackageManager();
            String str = "com.android.terminal";
            if (!this.mEnableTerminal.isChecked()) {
                i = 0;
            }
            pm.setApplicationEnabledSetting(str, i, 0);
        } else if (preference == this.mBugreportInPower) {
            r4 = getActivity().getContentResolver();
            r5 = "bugreport_in_power_menu";
            if (!this.mBugreportInPower.isChecked()) {
                i = 0;
            }
            Secure.putInt(r4, r5, i);
        } else if (preference == this.mKeepScreenOn) {
            r4 = getActivity().getContentResolver();
            r5 = "stay_on_while_plugged_in";
            if (this.mKeepScreenOn.isChecked()) {
                i = 3;
            } else {
                i = 0;
            }
            Global.putInt(r4, r5, i);
        } else if (preference == this.mBtHciSnoopLog) {
            writeBtHciSnoopLogOptions();
        } else if (preference == this.mEnableOemUnlock) {
            if (!showKeyguardConfirmation(getResources(), 0)) {
                if (this.mEnableOemUnlock.isChecked()) {
                    confirmEnableOemUnlock();
                } else {
                    Utils.setOemUnlockEnabled(getActivity(), false);
                }
            }
        } else if (preference == this.mMockLocationAppPref) {
            intent = new Intent(getActivity(), AppPicker.class);
            intent.putExtra("com.android.settings.extra.REQUESTIING_PERMISSION", "android.permission.ACCESS_MOCK_LOCATION");
            startActivityForResult(intent, 1001);
        } else if (preference == this.mDebugViewAttributes) {
            r4 = getActivity().getContentResolver();
            r5 = "debug_view_attributes";
            if (!this.mDebugViewAttributes.isChecked()) {
                i = 0;
            }
            Global.putInt(r4, r5, i);
        } else if (preference == this.mDebugAppPref) {
            intent = new Intent(getActivity(), AppPicker.class);
            intent.putExtra("com.android.settings.extra.DEBUGGABLE", true);
            startActivityForResult(intent, 1000);
        } else if (preference == this.mWaitForDebugger) {
            writeDebuggerOptions();
        } else if (preference == this.mVerifyAppsOverUsb) {
            writeVerifyAppsOverUsbOptions();
        } else if (preference == this.mStrictMode) {
            writeStrictModeVisualOptions();
        } else if (preference == this.mPointerLocation) {
            writePointerLocationOptions();
        } else if (preference == this.mShowTouches) {
            writeShowTouchesOptions();
        } else if (preference == this.mShowScreenUpdates) {
            writeShowUpdatesOption();
        } else if (preference == this.mDisableOverlays) {
            writeDisableOverlaysOption();
        } else if (preference == this.mEnableMultiWindow) {
            if (this.mEnableMultiWindow.isChecked()) {
                confirmEnableMultiWindowMode();
            } else {
                setEnableMultiWindow(false);
            }
        } else if (preference == this.mShowCpuUsage) {
            writeCpuUsageOptions();
        } else if (preference == this.mImmediatelyDestroyActivities) {
            writeImmediatelyDestroyActivitiesOptions();
        } else if (preference == this.mShowAllANRs) {
            writeShowAllANRsOptions();
        } else if (preference == this.mForceHardwareUi) {
            writeHardwareUiOptions();
        } else if (preference == this.mForceMsaa) {
            writeMsaaOptions();
        } else if (preference == this.mShowHwScreenUpdates) {
            writeShowHwScreenUpdatesOptions();
        } else if (preference == this.mShowHwLayersUpdates) {
            writeShowHwLayersUpdatesOptions();
        } else if (preference == this.mDebugLayout) {
            writeDebugLayoutOptions();
        } else if (preference == this.mForceRtlLayout) {
            writeForceRtlOptions();
        } else if (preference == this.mWifiDisplayCertification) {
            writeWifiDisplayCertificationOptions();
        } else if (preference == this.mWifiVerboseLogging) {
            writeWifiVerboseLoggingOptions();
        } else if (preference == this.mWifiAggressiveHandover) {
            writeWifiAggressiveHandoverOptions();
        } else if (preference == this.mWifiAllowScansWithTraffic) {
            writeWifiAllowScansWithTrafficOptions();
        } else if (preference == this.mLegacyDhcpClient) {
            writeLegacyDhcpClientOptions();
        } else if (preference == this.mMobileDataAlwaysOn) {
            writeMobileDataAlwaysOnOptions();
        } else if (preference == this.mUSBAudio) {
            writeUSBAudioOptions();
        } else if (!"inactive_apps".equals(preference.getKey())) {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        } else {
            startInactiveAppsFragment();
        }
        return false;
    }

    private void startInactiveAppsFragment() {
        ((SettingsActivity) getActivity()).startPreferencePanel(InactiveApps.class.getName(), null, R.string.inactive_apps_title, null, null, 0);
    }

    private boolean showKeyguardConfirmation(Resources resources, int requestCode) {
        return new ChooseLockSettingsHelper(getActivity(), this).launchConfirmationActivity(requestCode, resources.getString(R.string.oem_unlock_enable));
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if ("hdcp_checking".equals(preference.getKey())) {
            SystemProperties.set("persist.sys.hdcp_checking", newValue.toString());
            updateHdcpValues();
            pokeSystemProperties();
            return true;
        } else if (preference == this.mLogdSize) {
            writeLogdSizeOption(newValue);
            return true;
        } else if (preference == this.mUsbConfiguration) {
            writeUsbConfigurationOption(newValue);
            return true;
        } else if (preference == this.mWindowAnimationScale) {
            writeAnimationScaleOption(0, this.mWindowAnimationScale, newValue);
            return true;
        } else if (preference == this.mTransitionAnimationScale) {
            writeAnimationScaleOption(1, this.mTransitionAnimationScale, newValue);
            return true;
        } else if (preference == this.mAnimatorDurationScale) {
            writeAnimationScaleOption(2, this.mAnimatorDurationScale, newValue);
            return true;
        } else if (preference == this.mOverlayDisplayDevices) {
            writeOverlayDisplayDevicesOptions(newValue);
            return true;
        } else if (preference == this.mOpenGLTraces) {
            writeOpenGLTracesOptions(newValue);
            return true;
        } else if (preference == this.mTrackFrameTime) {
            writeTrackFrameTimeOptions(newValue);
            return true;
        } else if (preference == this.mDebugHwOverdraw) {
            writeDebugHwOverdrawOptions(newValue);
            return true;
        } else if (preference == this.mShowNonRectClip) {
            writeShowNonRectClipOptions(newValue);
            return true;
        } else if (preference == this.mAppProcessLimit) {
            writeAppProcessLimitOptions(newValue);
            return true;
        } else if (preference != this.mSimulateColorSpace) {
            return false;
        } else {
            writeSimulateColorSpace(newValue);
            return true;
        }
    }

    private void dismissDialogs() {
        if (this.mAdbDialog != null) {
            this.mAdbDialog.dismiss();
            this.mAdbDialog = null;
        }
        if (this.mAdbKeysDialog != null) {
            this.mAdbKeysDialog.dismiss();
            this.mAdbKeysDialog = null;
        }
        if (this.mEnableDialog != null) {
            this.mEnableDialog.dismiss();
            this.mEnableDialog = null;
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        if (dialog == this.mAdbDialog) {
            if (which == -1) {
                this.mDialogClicked = true;
                Global.putInt(getActivity().getContentResolver(), "adb_enabled", 1);
                this.mVerifyAppsOverUsb.setEnabled(true);
                updateVerifyAppsOverUsbOptions();
                updateBugreportOptions();
                return;
            }
            this.mEnableAdb.setChecked(false);
        } else if (dialog == this.mAdbKeysDialog) {
            if (which == -1) {
                try {
                    IUsbManager.Stub.asInterface(ServiceManager.getService("usb")).clearUsbDebuggingKeys();
                } catch (RemoteException e) {
                    Log.e("DevelopmentSettings", "Unable to clear adb keys", e);
                }
            }
        } else if (dialog != this.mEnableDialog) {
        } else {
            if (which == -1) {
                this.mDialogClicked = true;
                Global.putInt(getActivity().getContentResolver(), "development_settings_enabled", 1);
                this.mLastEnabledState = true;
                setPrefsEnabledState(this.mLastEnabledState);
                return;
            }
            this.mSwitchBar.setChecked(false);
        }
    }

    public void onDismiss(DialogInterface dialog) {
        if (dialog == this.mAdbDialog) {
            if (!this.mDialogClicked) {
                this.mEnableAdb.setChecked(false);
            }
            this.mAdbDialog = null;
        } else if (dialog == this.mEnableDialog) {
            if (!this.mDialogClicked) {
                this.mSwitchBar.setChecked(false);
            }
            this.mEnableDialog = null;
        }
    }

    public void onDestroy() {
        dismissDialogs();
        super.onDestroy();
    }

    void pokeSystemProperties() {
        if (!this.mDontPokeProperties) {
            new SystemPropPoker().execute(new Void[0]);
        }
    }

    private static boolean isPackageInstalled(Context context, String packageName) {
        boolean z = false;
        try {
            if (context.getPackageManager().getPackageInfo(packageName, 0) != null) {
                z = true;
            }
            return z;
        } catch (NameNotFoundException e) {
            return false;
        }
    }
}
