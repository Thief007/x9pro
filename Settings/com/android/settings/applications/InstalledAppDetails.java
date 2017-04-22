package com.android.settings.applications;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.icu.text.ListFormatter;
import android.net.INetworkStatsService.Stub;
import android.net.INetworkStatsSession;
import android.net.NetworkTemplate;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.DataUsageSummary;
import com.android.settings.DataUsageSummary.AppItem;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.applications.PermissionsSummaryHelper.PermissionsResultCallback;
import com.android.settings.fuelgauge.BatteryEntry;
import com.android.settings.fuelgauge.PowerUsageDetail;
import com.android.settings.net.ChartData;
import com.android.settings.net.ChartDataLoader;
import com.android.settings.notification.AppNotificationSettings;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.notification.NotificationBackend.AppRow;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.mediatek.common.mom.MobileManagerUtils;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IAppsExt;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class InstalledAppDetails extends AppInfoBase implements OnClickListener, OnPreferenceClickListener {
    private final NotificationBackend mBackend = new NotificationBackend();
    private BatteryStatsHelper mBatteryHelper;
    private Preference mBatteryPreference;
    private ChartData mChartData;
    private final BroadcastReceiver mCheckKillProcessesReceiver = new C02552();
    private final LoaderCallbacks<ChartData> mDataCallbacks = new C02541();
    private Preference mDataPreference;
    private boolean mDisableAfterUninstall;
    private Button mForceStopButton;
    private LayoutPreference mHeader;
    private final HashSet<String> mHomePackages = new HashSet();
    private boolean mInitialized;
    private Preference mLaunchPreference;
    private Preference mMemoryPreference;
    private Preference mNotificationPreference;
    private final PermissionsResultCallback mPermissionCallback = new C02563();
    private BroadcastReceiver mPermissionReceiver;
    private Preference mPermissionsPreference;
    private boolean mShowUninstalled;
    private BatterySipper mSipper;
    protected ProcStatsPackageEntry mStats;
    protected ProcStatsData mStatsManager;
    private INetworkStatsSession mStatsSession;
    private Preference mStoragePreference;
    private Button mUninstallButton;
    private boolean mUpdatedSysApp = false;

    class C02541 implements LoaderCallbacks<ChartData> {
        C02541() {
        }

        public Loader<ChartData> onCreateLoader(int id, Bundle args) {
            return new ChartDataLoader(InstalledAppDetails.this.getActivity(), InstalledAppDetails.this.mStatsSession, args);
        }

        public void onLoadFinished(Loader<ChartData> loader, ChartData data) {
            InstalledAppDetails.this.mChartData = data;
            InstalledAppDetails.this.mDataPreference.setSummary(InstalledAppDetails.this.getDataSummary());
        }

        public void onLoaderReset(Loader<ChartData> loader) {
        }
    }

    class C02552 extends BroadcastReceiver {
        C02552() {
        }

        public void onReceive(Context context, Intent intent) {
            boolean z = false;
            InstalledAppDetails installedAppDetails = InstalledAppDetails.this;
            if (getResultCode() != 0) {
                z = true;
            }
            installedAppDetails.updateForceStopButton(z);
        }
    }

    class C02563 implements PermissionsResultCallback {
        C02563() {
        }

        public void onPermissionSummaryResult(int[] counts, CharSequence[] groupLabels) {
            if (InstalledAppDetails.this.getActivity() != null) {
                InstalledAppDetails.this.mPermissionReceiver = null;
                Resources res = InstalledAppDetails.this.getResources();
                CharSequence summary = null;
                boolean enabled = false;
                if (counts != null) {
                    int totalCount = counts[1];
                    int additionalCounts = counts[2];
                    if (totalCount == 0) {
                        summary = res.getString(R.string.runtime_permissions_summary_no_permissions_requested);
                    } else {
                        enabled = true;
                        ArrayList<CharSequence> list = new ArrayList(Arrays.asList(groupLabels));
                        if (additionalCounts > 0) {
                            list.add(res.getQuantityString(R.plurals.runtime_permissions_additional_count, additionalCounts, new Object[]{Integer.valueOf(additionalCounts)}));
                        }
                        if (list.size() == 0) {
                            summary = res.getString(R.string.runtime_permissions_summary_no_permissions_granted);
                        } else {
                            summary = ListFormatter.getInstance().format(list);
                        }
                    }
                }
                InstalledAppDetails.this.mPermissionsPreference.setSummary(summary);
                InstalledAppDetails.this.mPermissionsPreference.setEnabled(enabled);
            }
        }
    }

    class C02574 implements DialogInterface.OnClickListener {
        C02574() {
        }

        public void onClick(DialogInterface dialog, int which) {
            new DisableChanger(InstalledAppDetails.this, InstalledAppDetails.this.mAppEntry.info, 3).execute(new Object[]{null});
        }
    }

    class C02585 implements DialogInterface.OnClickListener {
        C02585() {
        }

        public void onClick(DialogInterface dialog, int which) {
            InstalledAppDetails.this.uninstallPkg(InstalledAppDetails.this.mAppEntry.info.packageName, false, true);
        }
    }

    class C02596 implements DialogInterface.OnClickListener {
        C02596() {
        }

        public void onClick(DialogInterface dialog, int which) {
            InstalledAppDetails.this.forceStopPackage(InstalledAppDetails.this.mAppEntry.info.packageName);
        }
    }

    class C02607 implements DialogInterface.OnClickListener {
        C02607() {
        }

        public void onClick(DialogInterface dialog, int which) {
            InstalledAppDetails.this.uninstallPkg(InstalledAppDetails.this.mAppEntry.info.packageName, false, false);
        }
    }

    private class BatteryUpdater extends AsyncTask<Void, Void, Void> {
        private BatteryUpdater() {
        }

        protected Void doInBackground(Void... params) {
            InstalledAppDetails.this.mBatteryHelper.create((Bundle) null);
            InstalledAppDetails.this.mBatteryHelper.refreshStats(0, InstalledAppDetails.this.mUserManager.getUserProfiles());
            List<BatterySipper> usageList = InstalledAppDetails.this.mBatteryHelper.getUsageList();
            int N = usageList.size();
            for (int i = 0; i < N; i++) {
                BatterySipper sipper = (BatterySipper) usageList.get(i);
                if (sipper.getUid() == InstalledAppDetails.this.mPackageInfo.applicationInfo.uid) {
                    InstalledAppDetails.this.mSipper = sipper;
                    break;
                }
            }
            return null;
        }

        protected void onPostExecute(Void result) {
            if (InstalledAppDetails.this.getActivity() != null) {
                InstalledAppDetails.this.refreshUi();
            }
        }
    }

    private static class DisableChanger extends AsyncTask<Object, Object, Object> {
        final WeakReference<InstalledAppDetails> mActivity;
        final ApplicationInfo mInfo;
        final PackageManager mPm;
        final int mState;

        DisableChanger(InstalledAppDetails activity, ApplicationInfo info, int state) {
            this.mPm = activity.mPm;
            this.mActivity = new WeakReference(activity);
            this.mInfo = info;
            this.mState = state;
        }

        protected Object doInBackground(Object... params) {
            this.mPm.setApplicationEnabledSetting(this.mInfo.packageName, this.mState, 0);
            return null;
        }
    }

    private class MemoryUpdater extends AsyncTask<Void, Void, ProcStatsPackageEntry> {
        private MemoryUpdater() {
        }

        protected ProcStatsPackageEntry doInBackground(Void... params) {
            if (InstalledAppDetails.this.getActivity() == null || InstalledAppDetails.this.mPackageInfo == null) {
                return null;
            }
            if (InstalledAppDetails.this.mStatsManager == null) {
                InstalledAppDetails.this.mStatsManager = new ProcStatsData(InstalledAppDetails.this.getActivity(), false);
                InstalledAppDetails.this.mStatsManager.setDuration(ProcessStatsBase.sDurations[0]);
            }
            InstalledAppDetails.this.mStatsManager.refreshStats(true);
            for (ProcStatsPackageEntry pkgEntry : InstalledAppDetails.this.mStatsManager.getEntries()) {
                for (ProcStatsEntry entry : pkgEntry.mEntries) {
                    if (entry.mUid == InstalledAppDetails.this.mPackageInfo.applicationInfo.uid) {
                        pkgEntry.updateMetrics();
                        return pkgEntry;
                    }
                }
            }
            return null;
        }

        protected void onPostExecute(ProcStatsPackageEntry entry) {
            if (InstalledAppDetails.this.getActivity() != null) {
                if (entry != null) {
                    InstalledAppDetails.this.mStats = entry;
                    InstalledAppDetails.this.mMemoryPreference.setEnabled(true);
                    double amount = Math.max(entry.mRunWeight, entry.mBgWeight) * InstalledAppDetails.this.mStatsManager.getMemInfo().weightToRam;
                    InstalledAppDetails.this.mMemoryPreference.setSummary(InstalledAppDetails.this.getString(R.string.memory_use_summary, new Object[]{Formatter.formatShortFileSize(InstalledAppDetails.this.getContext(), (long) amount)}));
                } else {
                    InstalledAppDetails.this.mMemoryPreference.setEnabled(false);
                    InstalledAppDetails.this.mMemoryPreference.setSummary(InstalledAppDetails.this.getString(R.string.no_memory_use_summary));
                }
            }
        }
    }

    private boolean handleDisableable(Button button) {
        if (this.mHomePackages.contains(this.mAppEntry.info.packageName) || Utils.isSystemPackage(this.mPm, this.mPackageInfo)) {
            button.setText(R.string.disable_text);
            return false;
        } else if (UtilsExt.disableAppList != null && UtilsExt.disableAppList.contains(this.mAppEntry.info.packageName)) {
            Log.d(TAG, "mDisableAppsList contains :" + this.mAppEntry.info.packageName);
            button.setText(R.string.disable_text);
            return false;
        } else if (!this.mAppEntry.info.enabled || isDisabledUntilUsed()) {
            button.setText(R.string.enable_text);
            return true;
        } else {
            button.setText(R.string.disable_text);
            return true;
        }
    }

    private boolean isDisabledUntilUsed() {
        return this.mAppEntry.info.enabledSetting == 4;
    }

    private void initUninstallButtons() {
        boolean isBundled = (this.mAppEntry.info.flags & 1) != 0;
        boolean z = true;
        if (isBundled) {
            z = handleDisableable(this.mUninstallButton);
        } else {
            if ((this.mPackageInfo.applicationInfo.flags & 8388608) == 0 && this.mUserManager.getUsers().size() >= 2) {
                z = false;
            }
            this.mUninstallButton.setText(R.string.uninstall_text);
        }
        if (this.mDpm.packageHasActiveAdmins(this.mPackageInfo.packageName)) {
            z = false;
        }
        if (isProfileOrDeviceOwner(this.mPackageInfo.packageName)) {
            z = false;
        }
        if (z && this.mHomePackages.contains(this.mPackageInfo.packageName)) {
            if (isBundled) {
                z = false;
            } else {
                ComponentName currentDefaultHome = this.mPm.getHomeActivities(new ArrayList());
                z = currentDefaultHome == null ? this.mHomePackages.size() > 1 : !this.mPackageInfo.packageName.equals(currentDefaultHome.getPackageName());
            }
        }
        if (this.mAppControlRestricted) {
            z = false;
        }
        this.mUninstallButton.setEnabled(z);
        if (z) {
            this.mUninstallButton.setOnClickListener(this);
        }
    }

    private boolean isProfileOrDeviceOwner(String packageName) {
        List<UserInfo> userInfos = this.mUserManager.getUsers();
        DevicePolicyManager dpm = (DevicePolicyManager) getContext().getSystemService("device_policy");
        if (packageName.equals(dpm.getDeviceOwner())) {
            return true;
        }
        for (UserInfo userInfo : userInfos) {
            ComponentName cn = dpm.getProfileOwnerAsUser(userInfo.id);
            if (cn != null && cn.getPackageName().equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (Utils.isMonkeyRunning()) {
            Log.d(TAG, "monkey is running, finish");
            getActivity().finish();
        }
        setHasOptionsMenu(true);
        addPreferencesFromResource(R.xml.installed_app_details);
        if (Utils.isBandwidthControlEnabled()) {
            try {
                this.mStatsSession = Stub.asInterface(ServiceManager.getService("netstats")).openSession();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
        removePreference("data_settings");
        this.mBatteryHelper = new BatteryStatsHelper(getActivity(), true);
    }

    protected int getMetricsCategory() {
        return 20;
    }

    public void onResume() {
        super.onResume();
        if (!this.mFinishing) {
            this.mState.requestSize(this.mPackageName, this.mUserId);
            AppItem app = new AppItem(this.mAppEntry.info.uid);
            app.addUid(this.mAppEntry.info.uid);
            if (this.mStatsSession != null) {
                getLoaderManager().restartLoader(2, ChartDataLoader.buildArgs(getTemplate(getContext()), app), this.mDataCallbacks);
            }
            new BatteryUpdater().execute(new Void[0]);
            new MemoryUpdater().execute(new Void[0]);
        }
    }

    public void onPause() {
        getLoaderManager().destroyLoader(2);
        super.onPause();
    }

    public void onDestroy() {
        TrafficStats.closeQuietly(this.mStatsSession);
        if (this.mPermissionReceiver != null) {
            getContext().unregisterReceiver(this.mPermissionReceiver);
            this.mPermissionReceiver = null;
        }
        super.onDestroy();
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (!this.mFinishing) {
            handleHeader();
            this.mNotificationPreference = findPreference("notification_settings");
            this.mNotificationPreference.setOnPreferenceClickListener(this);
            this.mStoragePreference = findPreference("storage_settings");
            this.mStoragePreference.setOnPreferenceClickListener(this);
            this.mPermissionsPreference = findPreference("permission_settings");
            this.mPermissionsPreference.setOnPreferenceClickListener(this);
            this.mDataPreference = findPreference("data_settings");
            if (this.mDataPreference != null) {
                this.mDataPreference.setOnPreferenceClickListener(this);
            }
            this.mBatteryPreference = findPreference("battery");
            this.mBatteryPreference.setEnabled(false);
            this.mBatteryPreference.setOnPreferenceClickListener(this);
            this.mMemoryPreference = findPreference("memory");
            this.mMemoryPreference.setOnPreferenceClickListener(this);
            this.mLaunchPreference = findPreference("preferred_settings");
            if (this.mAppEntry == null || this.mAppEntry.info == null) {
                this.mLaunchPreference.setEnabled(false);
            } else if ((this.mAppEntry.info.flags & 8388608) == 0 || !this.mAppEntry.info.enabled) {
                this.mLaunchPreference.setEnabled(false);
            } else {
                this.mLaunchPreference.setOnPreferenceClickListener(this);
            }
            IAppsExt appExt = UtilsExt.getAppsExtPlugin(getActivity());
            if (!(this.mAppEntry == null || this.mAppEntry.info == null)) {
                appExt.launchApp(getPreferenceScreen(), this.mAppEntry.info.packageName);
            }
            if (!UtilsExt.isGmsBuild(getActivity()) && MobileManagerUtils.isSupported()) {
                removePreference("permission_settings");
            }
            if (!(UtilsExt.isGmsBuild(getActivity()) || !FeatureOption.MTK_RUNTIME_PERMISSION_SUPPORT || this.mAppEntry == null || this.mAppEntry.info == null || (this.mAppEntry.info.flags & 1) == 0 || (this.mAppEntry.info.flagsEx & 1) != 0)) {
                removePreference("permission_settings");
            }
        }
    }

    private void handleHeader() {
        this.mHeader = (LayoutPreference) findPreference("header_view");
        View btnPanel = this.mHeader.findViewById(R.id.control_buttons_panel);
        this.mForceStopButton = (Button) btnPanel.findViewById(R.id.right_button);
        this.mForceStopButton.setText(R.string.force_stop);
        this.mUninstallButton = (Button) btnPanel.findViewById(R.id.left_button);
        this.mForceStopButton.setEnabled(false);
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, 2, 0, R.string.app_factory_reset).setShowAsAction(0);
        menu.add(0, 1, 1, R.string.uninstall_all_users_text).setShowAsAction(0);
    }

    public void onPrepareOptionsMenu(Menu menu) {
        boolean z = false;
        if (!this.mFinishing) {
            boolean showIt = true;
            if (this.mUpdatedSysApp) {
                showIt = false;
            } else if (this.mAppEntry == null) {
                showIt = false;
            } else if ((this.mAppEntry.info.flags & 1) != 0) {
                showIt = false;
            } else if (this.mPackageInfo == null || this.mDpm.packageHasActiveAdmins(this.mPackageInfo.packageName)) {
                showIt = false;
            } else if (UserHandle.myUserId() != 0) {
                showIt = false;
            } else if (this.mUserManager.getUsers().size() < 2) {
                showIt = false;
            }
            menu.findItem(1).setVisible(showIt);
            if (!(this.mAppEntry == null || this.mAppEntry.info == null)) {
                boolean z2;
                if ((this.mAppEntry.info.flags & 128) != 0) {
                    z2 = true;
                } else {
                    z2 = false;
                }
                this.mUpdatedSysApp = z2;
            }
            MenuItem findItem = menu.findItem(2);
            if (this.mUpdatedSysApp && !this.mAppControlRestricted) {
                z = true;
            }
            findItem.setVisible(z);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                uninstallPkg(this.mAppEntry.info.packageName, true, false);
                return true;
            case 2:
                showDialogInner(4, 0);
                return true;
            default:
                return false;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (this.mDisableAfterUninstall) {
                this.mDisableAfterUninstall = false;
                try {
                    if ((getActivity().getPackageManager().getApplicationInfo(this.mAppEntry.info.packageName, 8704).flags & 128) == 0) {
                        new DisableChanger(this, this.mAppEntry.info, 3).execute(new Object[]{null});
                    }
                } catch (NameNotFoundException e) {
                }
            }
            if (!refreshUi()) {
                setIntentAndFinish(true, true);
            }
        }
    }

    private void setAppLabelAndIcon(PackageInfo pkgInfo) {
        CharSequence charSequence = null;
        View appSnippet = this.mHeader.findViewById(R.id.app_snippet);
        this.mState.ensureIcon(this.mAppEntry);
        CharSequence charSequence2 = this.mAppEntry.label;
        Drawable drawable = this.mAppEntry.icon;
        if (pkgInfo != null) {
            charSequence = pkgInfo.versionName;
        }
        setupAppSnippet(appSnippet, charSequence2, drawable, charSequence);
    }

    private boolean signaturesMatch(String pkg1, String pkg2) {
        if (!(pkg1 == null || pkg2 == null)) {
            try {
                if (this.mPm.checkSignatures(pkg1, pkg2) >= 0) {
                    return true;
                }
            } catch (Exception e) {
            }
        }
        return false;
    }

    protected boolean refreshUi() {
        retrieveAppEntry();
        if (this.mAppEntry == null) {
            return false;
        }
        if (this.mPackageInfo == null) {
            return false;
        }
        List<ResolveInfo> homeActivities = new ArrayList();
        this.mPm.getHomeActivities(homeActivities);
        this.mHomePackages.clear();
        for (int i = 0; i < homeActivities.size(); i++) {
            ResolveInfo ri = (ResolveInfo) homeActivities.get(i);
            String activityPkg = ri.activityInfo.packageName;
            this.mHomePackages.add(activityPkg);
            Bundle metadata = ri.activityInfo.metaData;
            if (metadata != null) {
                String metaPkg = metadata.getString("android.app.home.alternate");
                if (signaturesMatch(metaPkg, activityPkg)) {
                    this.mHomePackages.add(metaPkg);
                }
            }
        }
        checkForceStop();
        setAppLabelAndIcon(this.mPackageInfo);
        initUninstallButtons();
        Activity context = getActivity();
        this.mStoragePreference.setSummary(AppStorageSettings.getSummary(this.mAppEntry, context));
        if (this.mPermissionReceiver != null) {
            getContext().unregisterReceiver(this.mPermissionReceiver);
        }
        this.mPermissionReceiver = PermissionsSummaryHelper.getPermissionSummary(getContext(), this.mPackageName, this.mPermissionCallback);
        this.mLaunchPreference.setSummary(Utils.getLaunchByDeafaultSummary(this.mAppEntry, this.mUsbManager, this.mPm, context));
        this.mNotificationPreference.setSummary(getNotificationSummary(this.mAppEntry, context, this.mBackend));
        if (this.mDataPreference != null) {
            this.mDataPreference.setSummary(getDataSummary());
        }
        updateBattery();
        if (this.mInitialized) {
            try {
                ApplicationInfo ainfo = context.getPackageManager().getApplicationInfo(this.mAppEntry.info.packageName, 8704);
                if (!this.mShowUninstalled) {
                    return (ainfo.flags & 8388608) != 0;
                }
            } catch (NameNotFoundException e) {
                return false;
            }
        }
        boolean z;
        this.mInitialized = true;
        if ((this.mAppEntry.info.flags & 8388608) == 0) {
            z = true;
        } else {
            z = false;
        }
        this.mShowUninstalled = z;
        return true;
    }

    private void updateBattery() {
        if (this.mSipper != null) {
            this.mBatteryPreference.setEnabled(true);
            int percentOfMax = (int) (((this.mSipper.totalPowerMah / this.mBatteryHelper.getTotalPower()) * ((double) this.mBatteryHelper.getStats().getDischargeAmount(0))) + 0.5d);
            this.mBatteryPreference.setSummary(getString(R.string.battery_summary, new Object[]{Integer.valueOf(percentOfMax)}));
            return;
        }
        this.mBatteryPreference.setEnabled(false);
        this.mBatteryPreference.setSummary(getString(R.string.no_battery_summary));
    }

    private CharSequence getDataSummary() {
        if (this.mChartData == null) {
            return getString(R.string.computing_size);
        }
        if (this.mChartData.detail.getTotalBytes() == 0) {
            return getString(R.string.no_data_usage);
        }
        Context context = getActivity();
        return getString(R.string.data_summary_format, new Object[]{Formatter.formatFileSize(context, totalBytes), DateUtils.formatDateTime(context, this.mChartData.detail.getStart(), 65552)});
    }

    protected AlertDialog createDialog(int id, int errorCode) {
        switch (id) {
            case 1:
                return new Builder(getActivity()).setTitle(getActivity().getText(R.string.force_stop_dlg_title)).setMessage(getActivity().getText(R.string.force_stop_dlg_text)).setPositiveButton(R.string.dlg_ok, new C02596()).setNegativeButton(R.string.dlg_cancel, null).create();
            case 2:
                return new Builder(getActivity()).setMessage(getActivity().getText(R.string.app_disable_dlg_text)).setPositiveButton(R.string.app_disable_dlg_positive, new C02574()).setNegativeButton(R.string.dlg_cancel, null).create();
            case 3:
                return new Builder(getActivity()).setMessage(getActivity().getText(R.string.app_special_disable_dlg_text)).setPositiveButton(R.string.app_disable_dlg_positive, new C02585()).setNegativeButton(R.string.dlg_cancel, null).create();
            case 4:
                return new Builder(getActivity()).setTitle(getActivity().getText(R.string.app_factory_reset_dlg_title)).setMessage(getActivity().getText(R.string.app_factory_reset_dlg_text)).setPositiveButton(R.string.dlg_ok, new C02607()).setNegativeButton(R.string.dlg_cancel, null).create();
            default:
                return null;
        }
    }

    private void uninstallPkg(String packageName, boolean allUsers, boolean andDisable) {
        Intent uninstallIntent = new Intent("android.intent.action.UNINSTALL_PACKAGE", Uri.parse("package:" + packageName));
        uninstallIntent.putExtra("android.intent.extra.UNINSTALL_ALL_USERS", allUsers);
        startActivityForResult(uninstallIntent, 0);
        this.mDisableAfterUninstall = andDisable;
    }

    private void forceStopPackage(String pkgName) {
        ((ActivityManager) getActivity().getSystemService("activity")).forceStopPackage(pkgName);
        int userId = UserHandle.getUserId(this.mAppEntry.info.uid);
        this.mState.invalidatePackage(pkgName, userId);
        AppEntry newEnt = this.mState.getEntry(pkgName, userId);
        if (newEnt != null) {
            this.mAppEntry = newEnt;
        }
        checkForceStop();
    }

    private void updateForceStopButton(boolean enabled) {
        if (this.mAppControlRestricted) {
            this.mForceStopButton.setEnabled(false);
            return;
        }
        this.mForceStopButton.setEnabled(enabled);
        this.mForceStopButton.setOnClickListener(this);
    }

    private void checkForceStop() {
        if (this.mDpm.packageHasActiveAdmins(this.mPackageInfo.packageName)) {
            updateForceStopButton(false);
        } else if ((this.mAppEntry.info.flags & 2097152) == 0) {
            updateForceStopButton(true);
        } else {
            Intent intent = new Intent("android.intent.action.QUERY_PACKAGE_RESTART", Uri.fromParts("package", this.mAppEntry.info.packageName, null));
            intent.putExtra("android.intent.extra.PACKAGES", new String[]{this.mAppEntry.info.packageName});
            intent.putExtra("android.intent.extra.UID", this.mAppEntry.info.uid);
            intent.putExtra("android.intent.extra.user_handle", UserHandle.getUserId(this.mAppEntry.info.uid));
            getActivity().sendOrderedBroadcastAsUser(intent, UserHandle.CURRENT, null, this.mCheckKillProcessesReceiver, null, 0, null, null);
        }
    }

    private void startManagePermissionsActivity() {
        Intent intent = new Intent("android.intent.action.MANAGE_APP_PERMISSIONS");
        intent.putExtra("android.intent.extra.PACKAGE_NAME", this.mAppEntry.info.packageName);
        intent.putExtra("hideInfoButton", true);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w("InstalledAppDetails", "No app can handle android.intent.action.MANAGE_APP_PERMISSIONS");
        }
    }

    private void startAppInfoFragment(Class<?> fragment, CharSequence title) {
        Bundle args = new Bundle();
        args.putString("package", this.mAppEntry.info.packageName);
        args.putInt("uid", this.mAppEntry.info.uid);
        args.putBoolean("hideInfoButton", true);
        ((SettingsActivity) getActivity()).startPreferencePanel(fragment.getName(), args, -1, title, this, 1);
    }

    public void onClick(View v) {
        String packageName = this.mAppEntry.info.packageName;
        if (v == this.mUninstallButton) {
            if ((this.mAppEntry.info.flags & 1) != 0) {
                if (!this.mAppEntry.info.enabled || isDisabledUntilUsed()) {
                    new DisableChanger(this, this.mAppEntry.info, 0).execute(new Object[]{null});
                } else if (this.mUpdatedSysApp) {
                    showDialogInner(3, 0);
                } else {
                    showDialogInner(2, 0);
                }
            } else if ((this.mAppEntry.info.flags & 8388608) == 0) {
                uninstallPkg(packageName, true, false);
            } else {
                uninstallPkg(packageName, false, false);
            }
        } else if (v == this.mForceStopButton) {
            showDialogInner(1, 0);
        }
    }

    public boolean onPreferenceClick(Preference preference) {
        if (preference == this.mStoragePreference) {
            startAppInfoFragment(AppStorageSettings.class, this.mStoragePreference.getTitle());
        } else if (preference == this.mNotificationPreference) {
            startAppInfoFragment(AppNotificationSettings.class, getString(R.string.app_notifications_title));
        } else if (preference == this.mPermissionsPreference) {
            startManagePermissionsActivity();
        } else if (preference == this.mLaunchPreference) {
            startAppInfoFragment(AppLaunchSettings.class, this.mLaunchPreference.getTitle());
        } else if (preference == this.mMemoryPreference) {
            ProcessStatsBase.launchMemoryDetail((SettingsActivity) getActivity(), this.mStatsManager.getMemInfo(), this.mStats);
        } else if (preference == this.mDataPreference) {
            Bundle args = new Bundle();
            args.putString("showAppImmediatePkg", this.mAppEntry.info.packageName);
            ((SettingsActivity) getActivity()).startPreferencePanel(DataUsageSummary.class.getName(), args, -1, getString(R.string.app_data_usage), this, 1);
        } else if (preference != this.mBatteryPreference) {
            return false;
        } else {
            PowerUsageDetail.startBatteryDetailPage((SettingsActivity) getActivity(), this.mBatteryHelper, 0, new BatteryEntry(getActivity(), null, this.mUserManager, this.mSipper), true);
        }
        return true;
    }

    public static void setupAppSnippet(View appSnippet, CharSequence label, Drawable icon, CharSequence versionName) {
        LayoutInflater.from(appSnippet.getContext()).inflate(R.layout.widget_text_views, (ViewGroup) appSnippet.findViewById(16908312));
        ((ImageView) appSnippet.findViewById(16908294)).setImageDrawable(icon);
        ((TextView) appSnippet.findViewById(16908310)).setText(label);
        TextView appVersion = (TextView) appSnippet.findViewById(R.id.widget_text1);
        if (TextUtils.isEmpty(versionName)) {
            appVersion.setVisibility(4);
            return;
        }
        appVersion.setSelected(true);
        appVersion.setVisibility(0);
        appVersion.setText(appSnippet.getContext().getString(R.string.version_text, new Object[]{String.valueOf(versionName)}));
    }

    private static NetworkTemplate getTemplate(Context context) {
        if (DataUsageSummary.hasReadyMobileRadio(context)) {
            return NetworkTemplate.buildTemplateMobileWildcard();
        }
        if (DataUsageSummary.hasWifiRadio(context)) {
            return NetworkTemplate.buildTemplateWifiWildcard();
        }
        return NetworkTemplate.buildTemplateEthernet();
    }

    public static CharSequence getNotificationSummary(AppEntry appEntry, Context context, NotificationBackend backend) {
        return getNotificationSummary(backend.loadAppRow(context.getPackageManager(), appEntry.info), context);
    }

    public static CharSequence getNotificationSummary(AppRow appRow, Context context) {
        if (appRow.banned) {
            return context.getString(R.string.notifications_disabled);
        }
        ArrayList<CharSequence> notifSummary = new ArrayList();
        if (appRow.priority) {
            notifSummary.add(context.getString(R.string.notifications_priority));
        }
        if (appRow.sensitive) {
            notifSummary.add(context.getString(R.string.notifications_sensitive));
        }
        if (!appRow.peekable) {
            notifSummary.add(context.getString(R.string.notifications_no_peeking));
        }
        switch (notifSummary.size()) {
            case 1:
                return (CharSequence) notifSummary.get(0);
            case 2:
                return context.getString(R.string.notifications_two_items, new Object[]{notifSummary.get(0), notifSummary.get(1)});
            case 3:
                return context.getString(R.string.notifications_three_items, new Object[]{notifSummary.get(0), notifSummary.get(1), notifSummary.get(2)});
            default:
                return context.getString(R.string.notifications_enabled);
        }
    }
}
