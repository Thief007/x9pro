package com.android.settings.applications;

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.format.Formatter.BytesResult;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.Utils;

public class ProcessStatsSummary extends ProcessStatsBase implements OnPreferenceClickListener {
    private Preference mAppListPreference;
    private Preference mAverageUsed;
    private LinearColorBar mColors;
    private Preference mFree;
    private LayoutPreference mHeader;
    private TextView mMemStatus;
    private Preference mPerformance;
    private Preference mTotalMemory;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.process_stats_summary);
        this.mHeader = (LayoutPreference) findPreference("status_header");
        this.mMemStatus = (TextView) this.mHeader.findViewById(R.id.memory_state);
        this.mColors = (LinearColorBar) this.mHeader.findViewById(R.id.color_bar);
        this.mPerformance = findPreference("performance");
        this.mTotalMemory = findPreference("total_memory");
        this.mAverageUsed = findPreference("average_used");
        this.mFree = findPreference("free");
        this.mAppListPreference = findPreference("apps_list");
        this.mAppListPreference.setOnPreferenceClickListener(this);
    }

    public void refreshUi() {
        CharSequence memString;
        Context context = getContext();
        int memColor = context.getColor(R.color.running_processes_apps_ram);
        this.mColors.setColors(memColor, memColor, context.getColor(R.color.running_processes_free_ram));
        double usedRam = this.mStatsManager.getMemInfo().realUsedRam;
        double freeRam = 2.147483648E9d - usedRam;
        BytesResult usedResult = Formatter.formatBytes(context.getResources(), (long) usedRam, 1);
        String totalString = Formatter.formatShortFileSize(context, 2147483648L);
        String freeString = Formatter.formatShortFileSize(context, (long) freeRam);
        CharSequence[] memStatesStr = getResources().getTextArray(R.array.ram_states);
        int memState = this.mStatsManager.getMemState();
        if (memState < 0 || memState >= memStatesStr.length - 1) {
            memString = memStatesStr[memStatesStr.length - 1];
        } else {
            memString = memStatesStr[memState];
        }
        this.mMemStatus.setText(TextUtils.expandTemplate(getText(R.string.storage_size_large), new CharSequence[]{usedResult.value, usedResult.units}));
        float usedRatio = (float) (usedRam / 2.147483648E9d);
        this.mColors.setRatios(usedRatio, 0.0f, 1.0f - usedRatio);
        this.mPerformance.setSummary(memString);
        this.mTotalMemory.setSummary(totalString);
        this.mAverageUsed.setSummary(Utils.formatPercentage((long) usedRam, 2147483648L));
        this.mFree.setSummary(freeString);
        String durationString = getString(sDurationLabels[this.mDurationIndex]);
        int numApps = this.mStatsManager.getEntries().size();
        this.mAppListPreference.setSummary(getResources().getQuantityString(R.plurals.memory_usage_apps_summary, numApps, new Object[]{Integer.valueOf(numApps), durationString}));
    }

    protected int getMetricsCategory() {
        return 202;
    }

    public boolean onPreferenceClick(Preference preference) {
        if (preference != this.mAppListPreference) {
            return false;
        }
        Bundle args = new Bundle();
        args.putBoolean("transfer_stats", true);
        args.putInt("duration_index", this.mDurationIndex);
        this.mStatsManager.xferStats();
        startFragment(this, ProcessStatsUi.class.getName(), R.string.app_list_memory_use, 0, args);
        return true;
    }
}
