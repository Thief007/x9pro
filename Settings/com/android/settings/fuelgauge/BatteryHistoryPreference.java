package com.android.settings.fuelgauge;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryStats;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.R;
import com.android.settings.SettingsActivity;

public class BatteryHistoryPreference extends Preference {
    private Intent mBatteryBroadcast;
    private BatteryHistoryChart mChart;
    private BatteryStatsHelper mHelper;
    private BatteryStats mStats;

    public BatteryHistoryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void performClick(PreferenceScreen preferenceScreen) {
        if (isEnabled()) {
            this.mHelper.storeStatsHistoryInFile("tmp_bat_history.bin");
            Bundle args = new Bundle();
            args.putString("stats", "tmp_bat_history.bin");
            args.putParcelable("broadcast", this.mHelper.getBatteryBroadcast());
            if (getContext() instanceof SettingsActivity) {
                ((SettingsActivity) getContext()).startPreferencePanel(BatteryHistoryDetail.class.getName(), args, R.string.history_details_title, null, null, 0);
            }
        }
    }

    public void setStats(BatteryStatsHelper batteryStats) {
        this.mChart = null;
        this.mHelper = batteryStats;
        this.mStats = batteryStats.getStats();
        this.mBatteryBroadcast = batteryStats.getBatteryBroadcast();
        if (getLayoutResource() != R.layout.battery_history_chart) {
            setLayoutResource(R.layout.battery_history_chart);
        }
        notifyChanged();
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        if (this.mStats != null) {
            BatteryHistoryChart chart = (BatteryHistoryChart) view.findViewById(R.id.battery_history_chart);
            if (this.mChart == null) {
                chart.setStats(this.mStats, this.mBatteryBroadcast);
                this.mChart = chart;
            } else {
                ViewGroup parent = (ViewGroup) chart.getParent();
                int index = parent.indexOfChild(chart);
                parent.removeViewAt(index);
                if (this.mChart.getParent() != null) {
                    ((ViewGroup) this.mChart.getParent()).removeView(this.mChart);
                }
                parent.addView(this.mChart, index);
            }
        }
    }
}
