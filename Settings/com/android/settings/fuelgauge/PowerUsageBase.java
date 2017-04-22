package com.android.settings.fuelgauge;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public abstract class PowerUsageBase extends SettingsPreferenceFragment {
    private BroadcastReceiver mBatteryInfoReceiver = new C03982();
    private String mBatteryLevel;
    private String mBatteryStatus;
    private final Handler mHandler = new C03971();
    protected BatteryStatsHelper mStatsHelper;
    protected UserManager mUm;

    class C03971 extends Handler {
        C03971() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    PowerUsageBase.this.mStatsHelper.clearStats();
                    PowerUsageBase.this.refreshStats();
                    return;
                default:
                    return;
            }
        }
    }

    class C03982 extends BroadcastReceiver {
        C03982() {
        }

        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.BATTERY_CHANGED".equals(intent.getAction()) && PowerUsageBase.this.updateBatteryStatus(intent) && !PowerUsageBase.this.mHandler.hasMessages(100)) {
                PowerUsageBase.this.mHandler.sendEmptyMessageDelayed(100, 500);
            }
        }
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mUm = (UserManager) activity.getSystemService("user");
        this.mStatsHelper = new BatteryStatsHelper(activity, true);
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mStatsHelper.create(icicle);
        setHasOptionsMenu(true);
    }

    public void onStart() {
        super.onStart();
        this.mStatsHelper.clearStats();
    }

    public void onResume() {
        super.onResume();
        BatteryStatsHelper.dropFile(getActivity(), "tmp_bat_history.bin");
        updateBatteryStatus(getActivity().registerReceiver(this.mBatteryInfoReceiver, new IntentFilter("android.intent.action.BATTERY_CHANGED")));
        if (this.mHandler.hasMessages(100)) {
            this.mHandler.removeMessages(100);
            this.mStatsHelper.clearStats();
        }
    }

    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(this.mBatteryInfoReceiver);
    }

    public void onStop() {
        super.onStop();
        this.mHandler.removeMessages(100);
    }

    public void onDestroy() {
        super.onDestroy();
        if (getActivity().isChangingConfigurations()) {
            this.mStatsHelper.storeState();
        }
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.add(0, 2, 0, R.string.menu_stats_refresh).setIcon(17302501).setAlphabeticShortcut('r').setShowAsAction(5);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 2:
                this.mStatsHelper.clearStats();
                refreshStats();
                this.mHandler.removeMessages(100);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void refreshStats() {
        this.mStatsHelper.refreshStats(0, this.mUm.getUserProfiles());
    }

    protected void updatePreference(BatteryHistoryPreference historyPref) {
        historyPref.setStats(this.mStatsHelper);
    }

    private boolean updateBatteryStatus(Intent intent) {
        if (intent != null) {
            String batteryLevel = Utils.getBatteryPercentage(intent);
            String batteryStatus = Utils.getBatteryStatus(getResources(), intent);
            if (!(batteryLevel.equals(this.mBatteryLevel) && batteryStatus.equals(this.mBatteryStatus))) {
                this.mBatteryLevel = batteryLevel;
                this.mBatteryStatus = batteryStatus;
                return true;
            }
        }
        return false;
    }
}
