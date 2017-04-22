package com.android.settings.applications;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.android.internal.app.ProcessStats;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.ProcStatsData.MemInfo;

public abstract class ProcessStatsBase extends SettingsPreferenceFragment implements OnItemSelectedListener {
    private static final long DURATION_QUANTUM = ProcessStats.COMMIT_PERIOD;
    protected static int[] sDurationLabels = new int[]{R.string.menu_duration_3h, R.string.menu_duration_6h, R.string.menu_duration_12h, R.string.menu_duration_1d};
    protected static long[] sDurations = new long[]{10800000 - (DURATION_QUANTUM / 2), 21600000 - (DURATION_QUANTUM / 2), 43200000 - (DURATION_QUANTUM / 2), 86400000 - (DURATION_QUANTUM / 2)};
    protected int mDurationIndex;
    private ArrayAdapter<String> mFilterAdapter;
    private Spinner mFilterSpinner;
    private ViewGroup mSpinnerHeader;
    protected ProcStatsData mStatsManager;

    public abstract void refreshUi();

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Bundle args = getArguments();
        Context activity = getActivity();
        boolean z = icicle == null ? args != null ? args.getBoolean("transfer_stats", false) : false : true;
        this.mStatsManager = new ProcStatsData(activity, z);
        int i = icicle != null ? icicle.getInt("duration_index") : args != null ? args.getInt("duration_index") : 0;
        this.mDurationIndex = i;
        this.mStatsManager.setDuration(icicle != null ? icicle.getLong("duration", sDurations[0]) : sDurations[0]);
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("duration", this.mStatsManager.getDuration());
        outState.putInt("duration_index", this.mDurationIndex);
    }

    public void onResume() {
        super.onResume();
        this.mStatsManager.refreshStats(false);
        refreshUi();
    }

    public void onDestroy() {
        super.onDestroy();
        if (getActivity().isChangingConfigurations()) {
            this.mStatsManager.xferStats();
        }
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.mSpinnerHeader = (ViewGroup) setPinnedHeaderView((int) R.layout.apps_filter_spinner);
        this.mFilterSpinner = (Spinner) this.mSpinnerHeader.findViewById(R.id.filter_spinner);
        this.mFilterAdapter = new ArrayAdapter(getActivity(), R.layout.filter_spinner_item);
        this.mFilterAdapter.setDropDownViewResource(17367049);
        for (int i = 0; i < 4; i++) {
            this.mFilterAdapter.add(getString(sDurationLabels[i]));
        }
        this.mFilterSpinner.setAdapter(this.mFilterAdapter);
        this.mFilterSpinner.setSelection(this.mDurationIndex);
        this.mFilterSpinner.setOnItemSelectedListener(this);
    }

    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
        this.mDurationIndex = position;
        this.mStatsManager.setDuration(sDurations[position]);
        refreshUi();
    }

    public void onNothingSelected(AdapterView<?> adapterView) {
        this.mFilterSpinner.setSelection(0);
    }

    public static void launchMemoryDetail(SettingsActivity activity, MemInfo memInfo, ProcStatsPackageEntry entry) {
        Bundle args = new Bundle();
        args.putParcelable("package_entry", entry);
        args.putDouble("weight_to_ram", memInfo.weightToRam);
        args.putLong("total_time", memInfo.memTotalTime);
        args.putDouble("max_memory_usage", memInfo.usedWeight * memInfo.weightToRam);
        args.putDouble("total_scale", memInfo.totalScale);
        activity.startPreferencePanel(ProcessStatsDetail.class.getName(), args, R.string.memory_usage, null, null, 0);
    }
}
