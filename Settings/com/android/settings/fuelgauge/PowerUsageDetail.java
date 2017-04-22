package com.android.settings.fuelgauge;

import android.app.ActivityManager;
import android.app.ApplicationErrorReport;
import android.app.ApplicationErrorReport.BatteryInfo;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.BatteryStats.Uid;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.provider.Settings.Global;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.android.internal.os.BatterySipper.DrainType;
import com.android.internal.os.BatteryStatsHelper;
import com.android.internal.util.FastPrintWriter;
import com.android.settings.AppHeader;
import com.android.settings.DisplaySettings;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.WirelessSettings;
import com.android.settings.applications.AppInfoWithHeader;
import com.android.settings.applications.InstalledAppDetails;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.bluetooth.BluetoothSettings;
import com.android.settings.location.LocationSettings;
import com.android.settings.wifi.WifiSettings;
import com.android.setupwizardlib.R$styleable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class PowerUsageDetail extends PowerUsageBase implements OnClickListener {
    private static /* synthetic */ int[] -com_android_internal_os_BatterySipper$DrainTypeSwitchesValues;
    private static int[] sDrainTypeDesciptions = new int[]{R.string.battery_desc_standby, R.string.battery_desc_radio, R.string.battery_desc_voice, R.string.battery_desc_wifi, R.string.battery_desc_bluetooth, R.string.battery_desc_flashlight, R.string.battery_desc_display, R.string.battery_desc_apps, R.string.battery_desc_users, R.string.battery_desc_unaccounted, R.string.battery_desc_overcounted, R.string.battery_desc_camera};
    ApplicationInfo mApp;
    private final BroadcastReceiver mCheckKillProcessesReceiver = new C03991();
    private PreferenceCategory mControlsParent;
    private PreferenceCategory mDetailsParent;
    private DevicePolicyManager mDpm;
    private DrainType mDrainType;
    private Button mForceStopButton;
    private Preference mHighPower;
    ComponentName mInstaller;
    private PreferenceCategory mMessagesParent;
    private double mNoCoverage;
    private String[] mPackages;
    private PreferenceCategory mPackagesParent;
    private PackageManager mPm;
    private Button mReportButton;
    private boolean mShowLocationButton;
    private long mStartTime;
    private int[] mTypes;
    private int mUid;
    private int mUsageSince;
    private boolean mUsesGps;
    private double[] mValues;

    class C03991 extends BroadcastReceiver {
        C03991() {
        }

        public void onReceive(Context context, Intent intent) {
            boolean z = false;
            Button -get0 = PowerUsageDetail.this.mForceStopButton;
            if (getResultCode() != 0) {
                z = true;
            }
            -get0.setEnabled(z);
        }
    }

    class C04002 implements OnPreferenceClickListener {
        C04002() {
        }

        public boolean onPreferenceClick(Preference preference) {
            HighPowerDetail.show(PowerUsageDetail.this, PowerUsageDetail.this.mApp.packageName, 0, false);
            return true;
        }
    }

    private static /* synthetic */ int[] m8x12ff44d2() {
        if (-com_android_internal_os_BatterySipper$DrainTypeSwitchesValues != null) {
            return -com_android_internal_os_BatterySipper$DrainTypeSwitchesValues;
        }
        int[] iArr = new int[DrainType.values().length];
        try {
            iArr[DrainType.APP.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[DrainType.BLUETOOTH.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[DrainType.CAMERA.ordinal()] = 9;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[DrainType.CELL.ordinal()] = 3;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[DrainType.FLASHLIGHT.ordinal()] = 10;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[DrainType.IDLE.ordinal()] = 11;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[DrainType.OVERCOUNTED.ordinal()] = 4;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[DrainType.PHONE.ordinal()] = 12;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[DrainType.SCREEN.ordinal()] = 5;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[DrainType.UNACCOUNTED.ordinal()] = 6;
        } catch (NoSuchFieldError e10) {
        }
        try {
            iArr[DrainType.USER.ordinal()] = 7;
        } catch (NoSuchFieldError e11) {
        }
        try {
            iArr[DrainType.WIFI.ordinal()] = 8;
        } catch (NoSuchFieldError e12) {
        }
        -com_android_internal_os_BatterySipper$DrainTypeSwitchesValues = iArr;
        return iArr;
    }

    public static void startBatteryDetailPage(SettingsActivity caller, BatteryStatsHelper helper, int statsType, BatteryEntry entry, boolean showLocationButton) {
        int[] types;
        double[] values;
        helper.getStats();
        int dischargeAmount = helper.getStats().getDischargeAmount(statsType);
        Bundle args = new Bundle();
        args.putString("title", entry.name);
        args.putInt("percent", (int) (((entry.sipper.totalPowerMah * ((double) dischargeAmount)) / helper.getTotalPower()) + 0.5d));
        args.putInt("gauge", (int) Math.ceil((entry.sipper.totalPowerMah * 100.0d) / helper.getMaxPower()));
        args.putLong("duration", helper.getStatsPeriod());
        args.putString("iconPackage", entry.defaultPackageName);
        args.putInt("iconId", entry.iconId);
        args.putDouble("noCoverage", entry.sipper.noCoveragePercent);
        if (entry.sipper.uidObj != null) {
            args.putInt("uid", entry.sipper.uidObj.getUid());
        }
        args.putSerializable("drainType", entry.sipper.drainType);
        args.putBoolean("showLocationButton", showLocationButton);
        int userId = UserHandle.myUserId();
        switch (m8x12ff44d2()[entry.sipper.drainType.ordinal()]) {
            case 1:
            case R$styleable.SuwSetupWizardLayout_suwIllustrationHorizontalTile /*7*/:
                Uid uid = entry.sipper.uidObj;
                types = new int[]{R.string.usage_type_cpu, R.string.usage_type_cpu_foreground, R.string.usage_type_wake_lock, R.string.usage_type_gps, R.string.usage_type_wifi_running, R.string.usage_type_data_recv, R.string.usage_type_data_send, R.string.usage_type_radio_active, R.string.usage_type_data_wifi_recv, R.string.usage_type_data_wifi_send, R.string.usage_type_audio, R.string.usage_type_video, R.string.usage_type_camera, R.string.usage_type_flashlight, R.string.usage_type_computed_power};
                values = new double[]{(double) entry.sipper.cpuTimeMs, (double) entry.sipper.cpuFgTimeMs, (double) entry.sipper.wakeLockTimeMs, (double) entry.sipper.gpsTimeMs, (double) entry.sipper.wifiRunningTimeMs, (double) entry.sipper.mobileRxPackets, (double) entry.sipper.mobileTxPackets, (double) entry.sipper.mobileActive, (double) entry.sipper.wifiRxPackets, (double) entry.sipper.wifiTxPackets, 0.0d, 0.0d, (double) entry.sipper.cameraTimeMs, (double) entry.sipper.flashlightTimeMs, entry.sipper.totalPowerMah};
                if (entry.sipper.drainType == DrainType.APP) {
                    Writer result = new StringWriter();
                    PrintWriter printWriter = new FastPrintWriter(result, false, 1024);
                    helper.getStats().dumpLocked(caller, printWriter, "", helper.getStatsType(), uid.getUid());
                    printWriter.flush();
                    args.putString("report_details", result.toString());
                    result = new StringWriter();
                    printWriter = new FastPrintWriter(result, false, 1024);
                    helper.getStats().dumpCheckinLocked(caller, printWriter, helper.getStatsType(), uid.getUid());
                    printWriter.flush();
                    args.putString("report_checkin_details", result.toString());
                    if (uid.getUid() != 0) {
                        userId = UserHandle.getUserId(uid.getUid());
                        break;
                    }
                }
                break;
            case 2:
                types = new int[]{R.string.usage_type_on_time, R.string.usage_type_cpu, R.string.usage_type_cpu_foreground, R.string.usage_type_wake_lock, R.string.usage_type_data_recv, R.string.usage_type_data_send, R.string.usage_type_data_wifi_recv, R.string.usage_type_data_wifi_send, R.string.usage_type_computed_power};
                values = new double[]{(double) entry.sipper.usageTimeMs, (double) entry.sipper.cpuTimeMs, (double) entry.sipper.cpuFgTimeMs, (double) entry.sipper.wakeLockTimeMs, (double) entry.sipper.mobileRxPackets, (double) entry.sipper.mobileTxPackets, (double) entry.sipper.wifiRxPackets, (double) entry.sipper.wifiTxPackets, entry.sipper.totalPowerMah};
                break;
            case 3:
                types = new int[]{R.string.usage_type_on_time, R.string.usage_type_no_coverage, R.string.usage_type_radio_active, R.string.usage_type_computed_power};
                values = new double[]{(double) entry.sipper.usageTimeMs, entry.sipper.noCoveragePercent, (double) entry.sipper.mobileActive, entry.sipper.totalPowerMah};
                break;
            case 4:
                types = new int[]{R.string.usage_type_total_battery_capacity, R.string.usage_type_computed_power, R.string.usage_type_actual_power};
                values = new double[]{helper.getPowerProfile().getBatteryCapacity(), helper.getComputedPower(), helper.getMaxDrainedPower()};
                break;
            case R$styleable.SuwSetupWizardLayout_suwIllustrationAspectRatio /*6*/:
                types = new int[]{R.string.usage_type_total_battery_capacity, R.string.usage_type_computed_power, R.string.usage_type_actual_power};
                values = new double[]{helper.getPowerProfile().getBatteryCapacity(), helper.getComputedPower(), helper.getMinDrainedPower()};
                break;
            case R$styleable.SuwSetupWizardLayout_suwIllustrationImage /*8*/:
                types = new int[]{R.string.usage_type_wifi_running, R.string.usage_type_cpu, R.string.usage_type_cpu_foreground, R.string.usage_type_wake_lock, R.string.usage_type_data_recv, R.string.usage_type_data_send, R.string.usage_type_data_wifi_recv, R.string.usage_type_data_wifi_send, R.string.usage_type_computed_power};
                values = new double[]{(double) entry.sipper.wifiRunningTimeMs, (double) entry.sipper.cpuTimeMs, (double) entry.sipper.cpuFgTimeMs, (double) entry.sipper.wakeLockTimeMs, (double) entry.sipper.mobileRxPackets, (double) entry.sipper.mobileTxPackets, (double) entry.sipper.wifiRxPackets, (double) entry.sipper.wifiTxPackets, entry.sipper.totalPowerMah};
                break;
            default:
                types = new int[]{R.string.usage_type_on_time, R.string.usage_type_computed_power};
                values = new double[]{(double) entry.sipper.usageTimeMs, entry.sipper.totalPowerMah};
                break;
        }
        args.putIntArray("types", types);
        args.putDoubleArray("values", values);
        caller.startPreferencePanelAsUser(PowerUsageDetail.class.getName(), args, R.string.details_title, null, new UserHandle(userId));
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mPm = getActivity().getPackageManager();
        this.mDpm = (DevicePolicyManager) getActivity().getSystemService("device_policy");
        addPreferencesFromResource(R.xml.power_usage_details);
        this.mDetailsParent = (PreferenceCategory) findPreference("details_parent");
        this.mControlsParent = (PreferenceCategory) findPreference("controls_parent");
        this.mMessagesParent = (PreferenceCategory) findPreference("messages_parent");
        this.mPackagesParent = (PreferenceCategory) findPreference("packages_parent");
        createDetails();
    }

    protected int getMetricsCategory() {
        return 53;
    }

    public void onResume() {
        super.onResume();
        this.mStartTime = Process.getElapsedCpuTime();
        checkForceStop();
        if (this.mHighPower != null) {
            this.mHighPower.setSummary(HighPowerDetail.getSummary(getActivity(), this.mApp.packageName));
        }
        setupHeader();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (this.mHighPower != null) {
            this.mHighPower.setSummary(HighPowerDetail.getSummary(getActivity(), this.mApp.packageName));
        }
    }

    private void createDetails() {
        Bundle args = getArguments();
        Context context = getActivity();
        this.mUsageSince = args.getInt("since", 1);
        this.mUid = args.getInt("uid", 0);
        this.mPackages = context.getPackageManager().getPackagesForUid(this.mUid);
        this.mDrainType = (DrainType) args.getSerializable("drainType");
        this.mNoCoverage = args.getDouble("noCoverage", 0.0d);
        this.mShowLocationButton = args.getBoolean("showLocationButton");
        this.mTypes = args.getIntArray("types");
        this.mValues = args.getDoubleArray("values");
        LayoutPreference twoButtons = (LayoutPreference) findPreference("two_buttons");
        this.mForceStopButton = (Button) twoButtons.findViewById(R.id.left_button);
        this.mReportButton = (Button) twoButtons.findViewById(R.id.right_button);
        this.mForceStopButton.setEnabled(false);
        if (this.mUid >= 10000) {
            this.mForceStopButton.setText(R.string.force_stop);
            this.mForceStopButton.setTag(Integer.valueOf(7));
            this.mForceStopButton.setOnClickListener(this);
            this.mReportButton.setText(17040225);
            this.mReportButton.setTag(Integer.valueOf(8));
            this.mReportButton.setOnClickListener(this);
            if (this.mPackages == null || this.mPackages.length <= 0) {
                Log.d("PowerUsageDetail", "No packages!!");
            } else {
                try {
                    this.mApp = context.getPackageManager().getApplicationInfo(this.mPackages[0], 0);
                } catch (NameNotFoundException e) {
                }
            }
            if (Global.getInt(context.getContentResolver(), "send_action_app_error", 0) != 0) {
                boolean z;
                if (this.mApp != null) {
                    this.mInstaller = ApplicationErrorReport.getErrorReportReceiver(context, this.mPackages[0], this.mApp.flags);
                }
                Button button = this.mReportButton;
                if (this.mInstaller != null) {
                    z = true;
                } else {
                    z = false;
                }
                button.setEnabled(z);
            } else {
                removePreference("two_buttons");
            }
            if (this.mApp == null || !PowerWhitelistBackend.getInstance().isWhitelisted(this.mApp.packageName)) {
                this.mControlsParent.removePreference(findPreference("high_power"));
            } else {
                this.mHighPower = findPreference("high_power");
                this.mHighPower.setOnPreferenceClickListener(new C04002());
            }
        } else {
            removePreference("two_buttons");
            this.mControlsParent.removePreference(findPreference("high_power"));
        }
        refreshStats();
        fillDetailsSection();
        fillPackagesSection(this.mUid);
        fillControlsSection(this.mUid);
        fillMessagesSection(this.mUid);
    }

    private void setupHeader() {
        int i = 0;
        Intent intent = null;
        Bundle args = getArguments();
        CharSequence title = args.getString("title");
        String pkg = args.getString("iconPackage");
        int iconId = args.getInt("iconId", 0);
        Drawable appIcon = null;
        if (!TextUtils.isEmpty(pkg)) {
            try {
                PackageManager pm = getActivity().getPackageManager();
                ApplicationInfo ai = pm.getPackageInfo(pkg, 0).applicationInfo;
                if (ai != null) {
                    appIcon = ai.loadIcon(pm);
                }
            } catch (NameNotFoundException e) {
            }
        } else if (iconId != 0) {
            appIcon = getActivity().getDrawable(iconId);
        }
        if (appIcon == null) {
            appIcon = getActivity().getPackageManager().getDefaultActivityIcon();
        }
        if (pkg == null && this.mPackages != null) {
            pkg = this.mPackages[0];
        }
        if (pkg != null) {
            intent = AppInfoWithHeader.getInfoIntent(this, pkg);
        }
        if (this.mDrainType != DrainType.APP) {
            i = 17170443;
        }
        AppHeader.createAppHeader((SettingsPreferenceFragment) this, appIcon, title, intent, i);
    }

    public void onClick(View v) {
        doAction(((Integer) v.getTag()).intValue());
    }

    private void startApplicationDetailsActivity() {
        Bundle args = new Bundle();
        args.putString("package", this.mPackages[0]);
        ((SettingsActivity) getActivity()).startPreferencePanel(InstalledAppDetails.class.getName(), args, R.string.application_info_label, null, null, 0);
    }

    private void doAction(int action) {
        SettingsActivity sa = (SettingsActivity) getActivity();
        switch (action) {
            case 1:
                sa.startPreferencePanel(DisplaySettings.class.getName(), null, R.string.display_settings_title, null, null, 0);
                return;
            case 2:
                sa.startPreferencePanel(WifiSettings.class.getName(), null, R.string.wifi_settings, null, null, 0);
                return;
            case 3:
                sa.startPreferencePanel(BluetoothSettings.class.getName(), null, R.string.bluetooth_settings, null, null, 0);
                return;
            case 4:
                sa.startPreferencePanel(WirelessSettings.class.getName(), null, R.string.radio_controls_title, null, null, 0);
                return;
            case R$styleable.SuwSetupWizardLayout_suwIllustration /*5*/:
                startApplicationDetailsActivity();
                return;
            case R$styleable.SuwSetupWizardLayout_suwIllustrationAspectRatio /*6*/:
                sa.startPreferencePanel(LocationSettings.class.getName(), null, R.string.location_settings_title, null, null, 0);
                return;
            case R$styleable.SuwSetupWizardLayout_suwIllustrationHorizontalTile /*7*/:
                killProcesses();
                return;
            case R$styleable.SuwSetupWizardLayout_suwIllustrationImage /*8*/:
                reportBatteryUse();
                return;
            default:
                return;
        }
    }

    private void fillDetailsSection() {
        if (this.mTypes != null && this.mValues != null) {
            for (int i = 0; i < this.mTypes.length; i++) {
                if (this.mValues[i] > 0.0d) {
                    String value;
                    String label = getString(this.mTypes[i]);
                    switch (this.mTypes[i]) {
                        case R.string.usage_type_gps:
                            this.mUsesGps = true;
                            break;
                        case R.string.usage_type_data_send:
                        case R.string.usage_type_data_recv:
                        case R.string.usage_type_data_wifi_send:
                        case R.string.usage_type_data_wifi_recv:
                            value = Long.toString((long) this.mValues[i]);
                            break;
                        case R.string.usage_type_no_coverage:
                            value = Utils.formatPercentage((int) Math.floor(this.mValues[i]));
                            break;
                        case R.string.usage_type_total_battery_capacity:
                        case R.string.usage_type_computed_power:
                        case R.string.usage_type_actual_power:
                            value = getActivity().getString(R.string.mah, new Object[]{Long.valueOf((long) this.mValues[i])});
                            break;
                    }
                    value = Utils.formatElapsedTime(getActivity(), this.mValues[i], true);
                    addHorizontalPreference(this.mDetailsParent, label, value);
                }
            }
        }
    }

    private void addHorizontalPreference(PreferenceCategory parent, CharSequence title, CharSequence summary) {
        Preference pref = new Preference(getActivity());
        pref.setLayoutResource(R.layout.horizontal_preference);
        pref.setTitle(title);
        pref.setSummary(summary);
        pref.setSelectable(false);
        parent.addPreference(pref);
    }

    private void fillControlsSection(int uid) {
        PackageManager pm = getActivity().getPackageManager();
        String[] packages = pm.getPackagesForUid(uid);
        PackageInfo pi = null;
        if (packages != null) {
            try {
                pi = pm.getPackageInfo(packages[0], 0);
            } catch (NameNotFoundException e) {
            }
        } else {
            pi = null;
        }
        if (pi != null) {
            ApplicationInfo applicationInfo = pi.applicationInfo;
        }
        boolean removeHeader = true;
        switch (m8x12ff44d2()[this.mDrainType.ordinal()]) {
            case 1:
                if (packages != null && packages.length == 1) {
                    addControl(R.string.battery_action_app_details, R.string.battery_sugg_apps_info, 5);
                    removeHeader = false;
                }
                if (this.mUsesGps && this.mShowLocationButton) {
                    addControl(R.string.location_settings_title, R.string.battery_sugg_apps_gps, 6);
                    removeHeader = false;
                    break;
                }
            case 2:
                addControl(R.string.bluetooth_settings, R.string.battery_sugg_bluetooth_basic, 3);
                removeHeader = false;
                break;
            case 3:
                if (this.mNoCoverage > 10.0d) {
                    addControl(R.string.radio_controls_title, R.string.battery_sugg_radio, 4);
                    removeHeader = false;
                    break;
                }
                break;
            case R$styleable.SuwSetupWizardLayout_suwIllustration /*5*/:
                addControl(R.string.display_settings, R.string.battery_sugg_display, 1);
                removeHeader = false;
                break;
            case R$styleable.SuwSetupWizardLayout_suwIllustrationImage /*8*/:
                addControl(R.string.wifi_settings, R.string.battery_sugg_wifi, 2);
                removeHeader = false;
                break;
        }
        if (removeHeader) {
            this.mControlsParent.setTitle(null);
        }
    }

    private void addControl(int pageSummary, int actionTitle, final int action) {
        Preference pref = new Preference(getActivity());
        pref.setTitle(actionTitle);
        pref.setLayoutResource(R.layout.horizontal_preference);
        pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                PowerUsageDetail.this.doAction(action);
                return true;
            }
        });
        this.mControlsParent.addPreference(pref);
    }

    private void fillMessagesSection(int uid) {
        boolean removeHeader = true;
        switch (m8x12ff44d2()[this.mDrainType.ordinal()]) {
            case R$styleable.SuwSetupWizardLayout_suwIllustrationAspectRatio /*6*/:
                addMessage(R.string.battery_msg_unaccounted);
                removeHeader = false;
                break;
        }
        if (removeHeader) {
            this.mMessagesParent.setTitle(null);
        }
    }

    private void addMessage(int message) {
        addHorizontalPreference(this.mMessagesParent, getString(message), null);
    }

    private void removePackagesSection() {
        getPreferenceScreen().removePreference(this.mPackagesParent);
    }

    private void killProcesses() {
        if (this.mPackages != null) {
            ActivityManager am = (ActivityManager) getActivity().getSystemService("activity");
            int userId = UserHandle.getUserId(this.mUid);
            for (String forceStopPackageAsUser : this.mPackages) {
                am.forceStopPackageAsUser(forceStopPackageAsUser, userId);
            }
            checkForceStop();
        }
    }

    private void checkForceStop() {
        if (this.mPackages == null || this.mUid < 10000) {
            this.mForceStopButton.setEnabled(false);
            return;
        }
        for (String packageHasActiveAdmins : this.mPackages) {
            if (this.mDpm.packageHasActiveAdmins(packageHasActiveAdmins)) {
                this.mForceStopButton.setEnabled(false);
                return;
            }
        }
        int i = 0;
        while (i < this.mPackages.length) {
            try {
                if ((this.mPm.getApplicationInfo(this.mPackages[i], 0).flags & 2097152) == 0) {
                    this.mForceStopButton.setEnabled(true);
                    break;
                }
                i++;
            } catch (NameNotFoundException e) {
            }
        }
        Intent intent = new Intent("android.intent.action.QUERY_PACKAGE_RESTART", Uri.fromParts("package", this.mPackages[0], null));
        intent.putExtra("android.intent.extra.PACKAGES", this.mPackages);
        intent.putExtra("android.intent.extra.UID", this.mUid);
        intent.putExtra("android.intent.extra.user_handle", UserHandle.getUserId(this.mUid));
        getActivity().sendOrderedBroadcast(intent, null, this.mCheckKillProcessesReceiver, null, 0, null, null);
    }

    private void reportBatteryUse() {
        boolean z = false;
        if (this.mPackages != null) {
            ApplicationErrorReport report = new ApplicationErrorReport();
            report.type = 3;
            report.packageName = this.mPackages[0];
            report.installerPackageName = this.mInstaller.getPackageName();
            report.processName = this.mPackages[0];
            report.time = System.currentTimeMillis();
            if ((this.mApp.flags & 1) != 0) {
                z = true;
            }
            report.systemApp = z;
            Bundle args = getArguments();
            BatteryInfo batteryInfo = new BatteryInfo();
            batteryInfo.usagePercent = args.getInt("percent", 1);
            batteryInfo.durationMicros = args.getLong("duration", 0);
            batteryInfo.usageDetails = args.getString("report_details");
            batteryInfo.checkinDetails = args.getString("report_checkin_details");
            report.batteryInfo = batteryInfo;
            Intent result = new Intent("android.intent.action.APP_ERROR");
            result.setComponent(this.mInstaller);
            result.putExtra("android.intent.extra.BUG_REPORT", report);
            result.addFlags(268435456);
            startActivity(result);
        }
    }

    private void fillPackagesSection(int uid) {
        if (uid < 1) {
            removePackagesSection();
        } else if (this.mPackages == null || this.mPackages.length < 2) {
            removePackagesSection();
        } else {
            PackageManager pm = getPackageManager();
            for (int i = 0; i < this.mPackages.length; i++) {
                try {
                    CharSequence label = pm.getApplicationInfo(this.mPackages[i], 0).loadLabel(pm);
                    if (label != null) {
                        this.mPackages[i] = label.toString();
                    }
                    addHorizontalPreference(this.mPackagesParent, this.mPackages[i], null);
                } catch (NameNotFoundException e) {
                }
            }
        }
    }
}
