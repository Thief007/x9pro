package com.android.settings.applications;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog.Builder;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.text.format.Formatter;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.android.settings.AppHeader;
import com.android.settings.CancellablePreference;
import com.android.settings.CancellablePreference.OnCancelListener;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.ProcStatsEntry.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ProcessStatsDetail extends SettingsPreferenceFragment {
    static final Comparator<ProcStatsEntry> sEntryCompare = new C02731();
    static final Comparator<Service> sServiceCompare = new C02742();
    static final Comparator<PkgService> sServicePkgCompare = new C02753();
    private ProcStatsPackageEntry mApp;
    private LinearColorBar mColorBar;
    private DevicePolicyManager mDpm;
    private MenuItem mForceStop;
    private double mMaxMemoryUsage;
    private long mOnePercentTime;
    private PackageManager mPm;
    private PreferenceCategory mProcGroup;
    private final ArrayMap<ComponentName, CancellablePreference> mServiceMap = new ArrayMap();
    private double mTotalScale;
    private long mTotalTime;
    private double mWeightToRam;

    static class C02731 implements Comparator<ProcStatsEntry> {
        C02731() {
        }

        public int compare(ProcStatsEntry lhs, ProcStatsEntry rhs) {
            if (lhs.mRunWeight < rhs.mRunWeight) {
                return 1;
            }
            if (lhs.mRunWeight > rhs.mRunWeight) {
                return -1;
            }
            return 0;
        }
    }

    static class C02742 implements Comparator<Service> {
        C02742() {
        }

        public int compare(Service lhs, Service rhs) {
            if (lhs.mDuration < rhs.mDuration) {
                return 1;
            }
            if (lhs.mDuration > rhs.mDuration) {
                return -1;
            }
            return 0;
        }
    }

    static class C02753 implements Comparator<PkgService> {
        C02753() {
        }

        public int compare(PkgService lhs, PkgService rhs) {
            if (lhs.mDuration < rhs.mDuration) {
                return 1;
            }
            if (lhs.mDuration > rhs.mDuration) {
                return -1;
            }
            return 0;
        }
    }

    static class PkgService {
        long mDuration;
        final ArrayList<Service> mServices = new ArrayList();

        PkgService() {
        }
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mPm = getActivity().getPackageManager();
        this.mDpm = (DevicePolicyManager) getActivity().getSystemService("device_policy");
        Bundle args = getArguments();
        this.mApp = (ProcStatsPackageEntry) args.getParcelable("package_entry");
        this.mApp.retrieveUiData(getActivity(), this.mPm);
        this.mWeightToRam = args.getDouble("weight_to_ram");
        this.mTotalTime = args.getLong("total_time");
        this.mMaxMemoryUsage = args.getDouble("max_memory_usage");
        this.mTotalScale = args.getDouble("total_scale");
        this.mOnePercentTime = this.mTotalTime / 100;
        this.mServiceMap.clear();
        createDetails();
        setHasOptionsMenu(true);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        Intent intent = null;
        super.onViewCreated(view, savedInstanceState);
        Drawable loadIcon = this.mApp.mUiTargetApp != null ? this.mApp.mUiTargetApp.loadIcon(this.mPm) : new ColorDrawable(0);
        CharSequence charSequence = this.mApp.mUiLabel;
        if (!this.mApp.mPackage.equals("os")) {
            intent = AppInfoWithHeader.getInfoIntent(this, this.mApp.mPackage);
        }
        AppHeader.createAppHeader(this, loadIcon, charSequence, intent);
    }

    protected int getMetricsCategory() {
        return 21;
    }

    public void onResume() {
        super.onResume();
        checkForceStop();
        updateRunningServices();
    }

    private void updateRunningServices() {
        int i;
        List<RunningServiceInfo> runningServices = ((ActivityManager) getActivity().getSystemService("activity")).getRunningServices(Integer.MAX_VALUE);
        int N = this.mServiceMap.size();
        for (i = 0; i < N; i++) {
            ((CancellablePreference) this.mServiceMap.valueAt(i)).setCancellable(false);
        }
        N = runningServices.size();
        for (i = 0; i < N; i++) {
            RunningServiceInfo runningService = (RunningServiceInfo) runningServices.get(i);
            if ((runningService.started || runningService.clientLabel != 0) && (runningService.flags & 8) == 0) {
                final ComponentName service = runningService.service;
                CancellablePreference pref = (CancellablePreference) this.mServiceMap.get(service);
                if (pref != null) {
                    pref.setOnCancelListener(new OnCancelListener() {
                        public void onCancel(CancellablePreference preference) {
                            ProcessStatsDetail.this.stopService(service.getPackageName(), service.getClassName());
                        }
                    });
                    pref.setCancellable(true);
                }
            }
        }
    }

    private void createDetails() {
        addPreferencesFromResource(R.xml.app_memory_settings);
        this.mProcGroup = (PreferenceCategory) findPreference("processes");
        fillProcessesSection();
        LayoutPreference headerLayout = (LayoutPreference) findPreference("status_header");
        double avgRam = ((this.mApp.mRunWeight > this.mApp.mBgWeight ? 1 : (this.mApp.mRunWeight == this.mApp.mBgWeight ? 0 : -1)) > 0 ? this.mApp.mRunWeight : this.mApp.mBgWeight) * this.mWeightToRam;
        float avgRatio = (float) (avgRam / this.mMaxMemoryUsage);
        float remainingRatio = 1.0f - avgRatio;
        this.mColorBar = (LinearColorBar) headerLayout.findViewById(R.id.color_bar);
        Context context = getActivity();
        this.mColorBar.setColors(context.getColor(R.color.memory_max_use), 0, context.getColor(R.color.memory_remaining));
        this.mColorBar.setRatios(avgRatio, 0.0f, remainingRatio);
        ((TextView) headerLayout.findViewById(R.id.memory_state)).setText(Formatter.formatShortFileSize(getContext(), (long) avgRam));
        findPreference("frequency").setSummary(ProcStatsPackageEntry.getFrequency(((float) Math.max(this.mApp.mRunDuration, this.mApp.mBgDuration)) / ((float) this.mTotalTime), getActivity()));
        findPreference("max_usage").setSummary(Formatter.formatShortFileSize(getContext(), (long) ((((double) Math.max(this.mApp.mMaxBgMem, this.mApp.mMaxRunMem)) * this.mTotalScale) * 1024.0d)));
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        this.mForceStop = menu.add(0, 1, 0, R.string.force_stop);
        checkForceStop();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                killProcesses();
                return true;
            default:
                return false;
        }
    }

    private void fillProcessesSection() {
        int ie;
        this.mProcGroup.removeAll();
        ArrayList<ProcStatsEntry> entries = new ArrayList();
        for (ie = 0; ie < this.mApp.mEntries.size(); ie++) {
            ProcStatsEntry entry = (ProcStatsEntry) this.mApp.mEntries.get(ie);
            if (entry.mPackage.equals("os")) {
                entry.mLabel = entry.mName;
            } else {
                entry.mLabel = getProcessName(this.mApp.mUiLabel, entry);
            }
            entries.add(entry);
        }
        Collections.sort(entries, sEntryCompare);
        for (ie = 0; ie < entries.size(); ie++) {
            entry = (ProcStatsEntry) entries.get(ie);
            Preference processPref = new Preference(getActivity());
            processPref.setTitle(entry.mLabel);
            processPref.setSelectable(false);
            long duration = Math.max(entry.mRunDuration, entry.mBgDuration);
            String memoryString = Formatter.formatShortFileSize(getActivity(), Math.max((long) (entry.mRunWeight * this.mWeightToRam), (long) (entry.mBgWeight * this.mWeightToRam)));
            CharSequence frequency = ProcStatsPackageEntry.getFrequency(((float) duration) / ((float) this.mTotalTime), getActivity());
            processPref.setSummary(getString(R.string.memory_use_running_format, new Object[]{memoryString, frequency}));
            this.mProcGroup.addPreference(processPref);
        }
        if (this.mProcGroup.getPreferenceCount() < 2) {
            getPreferenceScreen().removePreference(this.mProcGroup);
        }
    }

    private static String capitalize(String processName) {
        char c = processName.charAt(0);
        if (Character.isLowerCase(c)) {
            return Character.toUpperCase(c) + processName.substring(1);
        }
        return processName;
    }

    private static String getProcessName(String appLabel, ProcStatsEntry entry) {
        String processName = entry.mName;
        if (processName.contains(":")) {
            return capitalize(processName.substring(processName.lastIndexOf(58) + 1));
        }
        if (!processName.startsWith(entry.mPackage)) {
            return processName;
        }
        if (processName.length() == entry.mPackage.length()) {
            return appLabel;
        }
        int start = entry.mPackage.length();
        if (processName.charAt(start) == '.') {
            start++;
        }
        return capitalize(processName.substring(start));
    }

    private void stopService(String pkg, String name) {
        try {
            if ((getActivity().getPackageManager().getApplicationInfo(pkg, 0).flags & 1) != 0) {
                showStopServiceDialog(pkg, name);
            } else {
                doStopService(pkg, name);
            }
        } catch (NameNotFoundException e) {
            Log.e("ProcessStatsDetail", "Can't find app " + pkg, e);
        }
    }

    private void showStopServiceDialog(final String pkg, final String name) {
        new Builder(getActivity()).setTitle(R.string.runningservicedetails_stop_dlg_title).setMessage(R.string.runningservicedetails_stop_dlg_text).setPositiveButton(R.string.dlg_ok, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                ProcessStatsDetail.this.doStopService(pkg, name);
            }
        }).setNegativeButton(R.string.dlg_cancel, null).show();
    }

    private void doStopService(String pkg, String name) {
        getActivity().stopService(new Intent().setClassName(pkg, name));
        updateRunningServices();
    }

    private void killProcesses() {
        ActivityManager am = (ActivityManager) getActivity().getSystemService("activity");
        for (int i = 0; i < this.mApp.mEntries.size(); i++) {
            ProcStatsEntry ent = (ProcStatsEntry) this.mApp.mEntries.get(i);
            for (int j = 0; j < ent.mPackages.size(); j++) {
                am.forceStopPackage((String) ent.mPackages.get(j));
            }
        }
    }

    private void checkForceStop() {
        if (this.mForceStop != null) {
            if (((ProcStatsEntry) this.mApp.mEntries.get(0)).mUid < 10000) {
                this.mForceStop.setVisible(false);
                return;
            }
            boolean isStarted = false;
            for (int i = 0; i < this.mApp.mEntries.size(); i++) {
                ProcStatsEntry ent = (ProcStatsEntry) this.mApp.mEntries.get(i);
                for (int j = 0; j < ent.mPackages.size(); j++) {
                    String pkg = (String) ent.mPackages.get(j);
                    if (this.mDpm.packageHasActiveAdmins(pkg)) {
                        this.mForceStop.setEnabled(false);
                        return;
                    }
                    try {
                        if ((this.mPm.getApplicationInfo(pkg, 0).flags & 2097152) == 0) {
                            isStarted = true;
                        }
                    } catch (NameNotFoundException e) {
                    }
                }
            }
            if (isStarted) {
                this.mForceStop.setVisible(true);
            }
        }
    }
}
