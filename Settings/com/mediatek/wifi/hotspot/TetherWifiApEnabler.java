package com.mediatek.wifi.hotspot;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.preference.SwitchPreference;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.util.Log;
import android.widget.Switch;
import com.android.settings.R;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBar.OnSwitchChangeListener;
import com.android.settingslib.TetherUtil;
import com.mediatek.custom.CustomProperties;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.TetherSettingsExt;
import java.util.ArrayList;

public class TetherWifiApEnabler extends Fragment implements OnSwitchChangeListener {
    ConnectivityManager mCm;
    private Context mContext;
    private IntentFilter mIntentFilter;
    private CharSequence mOriginalSummary;
    private String[] mProvisionApp;
    private final BroadcastReceiver mReceiver = new C07761();
    private boolean mStateMachineEvent;
    private SwitchPreference mSwitch;
    private SwitchBar mSwitchBar;
    private int mTetherChoice = -1;
    private TetherSettingsExt mTetherSettingsEx;
    private WifiManager mWifiManager;
    private String[] mWifiRegexs;

    class C07761 extends BroadcastReceiver {
        C07761() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(action)) {
                TetherWifiApEnabler.this.handleWifiApStateChanged(intent.getIntExtra("wifi_state", 14));
            } else if ("android.net.conn.TETHER_STATE_CHANGED".equals(action)) {
                ArrayList<String> available = intent.getStringArrayListExtra("availableArray");
                ArrayList<String> active = intent.getStringArrayListExtra("activeArray");
                ArrayList<String> errored = intent.getStringArrayListExtra("erroredArray");
                if (available != null && active != null && errored != null) {
                    if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
                        TetherWifiApEnabler.this.updateTetherStateForIpv6(available.toArray(), active.toArray(), errored.toArray());
                    } else {
                        TetherWifiApEnabler.this.updateTetherState(available.toArray(), active.toArray(), errored.toArray());
                    }
                }
            } else if ("android.intent.action.AIRPLANE_MODE".equals(action)) {
                TetherWifiApEnabler.this.enableWifiSwitch();
            }
        }
    }

    public TetherWifiApEnabler(SwitchBar switchBar, Context context) {
        this.mContext = context;
        this.mSwitchBar = switchBar;
        setupSwitchBar();
        init(context);
        commitFragment();
    }

    public void setupSwitchBar() {
        this.mSwitchBar.addOnSwitchChangeListener(this);
        this.mSwitchBar.show();
    }

    public void teardownSwitchBar() {
        this.mSwitchBar.removeOnSwitchChangeListener(this);
        this.mSwitchBar.hide();
    }

    public void init(Context context) {
        this.mWifiManager = (WifiManager) context.getApplicationContext().getSystemService("wifi");
        this.mCm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        this.mWifiRegexs = this.mCm.getTetherableWifiRegexs();
        this.mIntentFilter = new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED");
        this.mIntentFilter.addAction("android.net.conn.TETHER_STATE_CHANGED");
        this.mIntentFilter.addAction("android.intent.action.AIRPLANE_MODE");
        this.mProvisionApp = this.mContext.getResources().getStringArray(17235989);
    }

    public void resume() {
        this.mContext.registerReceiver(this.mReceiver, this.mIntentFilter);
        enableWifiSwitch();
    }

    public void pause() {
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    private void enableWifiSwitch() {
        if (Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) != 0) {
            if (this.mSwitchBar == null) {
                this.mSwitch.setSummary(this.mOriginalSummary);
            }
            setSwitchEnabled(false);
            return;
        }
        setSwitchEnabled(true);
    }

    public void setSoftapEnabled(boolean enable) {
        if (TetherUtil.setWifiTethering(enable, this.mContext)) {
            setSwitchEnabled(false);
        } else {
            this.mSwitch.setSummary(R.string.wifi_error);
        }
    }

    public void updateConfigSummary(WifiConfiguration wifiConfig) {
        String s = CustomProperties.getString("wlan", "SSID", this.mContext.getString(17040282));
        if (this.mSwitchBar == null) {
            SwitchPreference switchPreference = this.mSwitch;
            String string = this.mContext.getString(R.string.wifi_tether_enabled_subtext);
            Object[] objArr = new Object[1];
            if (wifiConfig != null) {
                s = wifiConfig.SSID;
            }
            objArr[0] = s;
            switchPreference.setSummary(String.format(string, objArr));
        }
    }

    private void updateTetherStateForIpv6(Object[] available, Object[] tethered, Object[] errored) {
        boolean wifiTethered = false;
        boolean wifiErrored = false;
        int wifiErrorIpv4 = 0;
        int wifiErrorIpv6 = 16;
        for (String s : available) {
            for (String regex : this.mWifiRegexs) {
                String s2;
                if (s2.matches(regex)) {
                    if (wifiErrorIpv4 == 0) {
                        wifiErrorIpv4 = this.mCm.getLastTetherError(s2) & 15;
                    }
                    if (wifiErrorIpv6 == 16) {
                        wifiErrorIpv6 = this.mCm.getLastTetherError(s2) & 240;
                    }
                }
            }
        }
        for (Object o : tethered) {
            s2 = (String) o;
            for (String regex2 : this.mWifiRegexs) {
                if (s2.matches(regex2)) {
                    wifiTethered = true;
                    if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT && wifiErrorIpv6 == 16) {
                        wifiErrorIpv6 = this.mCm.getLastTetherError(s2) & 240;
                    }
                }
            }
        }
        for (Object o2 : errored) {
            s2 = (String) o2;
            for (String regex22 : this.mWifiRegexs) {
                if (s2.matches(regex22)) {
                    wifiErrored = true;
                }
            }
        }
        if (wifiTethered) {
            WifiConfiguration wifiConfig = this.mWifiManager.getWifiApConfiguration();
            updateConfigSummary(wifiConfig);
            s2 = this.mContext.getString(17040282);
            String string = this.mContext.getString(R.string.wifi_tether_enabled_subtext);
            Object[] objArr = new Object[1];
            if (wifiConfig != null) {
                s2 = wifiConfig.SSID;
            }
            objArr[0] = s2;
            String tetheringActive = String.format(string, objArr);
            if (this.mTetherSettingsEx != null && this.mSwitchBar == null) {
                this.mSwitch.setSummary(tetheringActive + this.mTetherSettingsEx.getIPV6String(wifiErrorIpv4, wifiErrorIpv6));
            }
        } else if (wifiErrored && this.mSwitchBar == null) {
            this.mSwitch.setSummary(R.string.wifi_error);
        }
    }

    private void updateTetherState(Object[] available, Object[] tethered, Object[] errored) {
        String s;
        boolean wifiTethered = false;
        boolean wifiErrored = false;
        for (String s2 : tethered) {
            for (String regex : this.mWifiRegexs) {
                if (s2.matches(regex)) {
                    wifiTethered = true;
                }
            }
        }
        for (Object o : errored) {
            s2 = (String) o;
            for (String regex2 : this.mWifiRegexs) {
                if (s2.matches(regex2)) {
                    wifiErrored = true;
                }
            }
        }
        if (wifiTethered) {
            updateConfigSummary(this.mWifiManager.getWifiApConfiguration());
        } else if (wifiErrored && this.mSwitchBar == null) {
            this.mSwitch.setSummary(R.string.wifi_error);
        }
    }

    private void handleWifiApStateChanged(int state) {
        switch (state) {
            case 10:
                setSwitchChecked(false);
                setSwitchEnabled(false);
                if (this.mSwitchBar == null) {
                    Log.d("TetherWifiApEnabler", "wifi_stopping");
                    this.mSwitch.setSummary(R.string.wifi_tether_stopping);
                    return;
                }
                return;
            case 11:
                Log.i("WifiHotspotPerformanceTest", "[Performance test][Settings][wifi hotspot] wifi hotspot turn off end [" + System.currentTimeMillis() + "]");
                setSwitchChecked(false);
                setSwitchEnabled(true);
                if (this.mSwitchBar == null) {
                    this.mSwitch.setSummary(this.mOriginalSummary);
                }
                enableWifiSwitch();
                return;
            case 12:
                setSwitchEnabled(false);
                setStartTime(false);
                if (this.mSwitchBar == null) {
                    this.mSwitch.setSummary(R.string.wifi_tether_starting);
                    return;
                }
                return;
            case 13:
                Log.i("WifiHotspotPerformanceTest", "[Performance test][Settings][wifi hotspot] wifi hotspot turn on end [" + System.currentTimeMillis() + "]");
                setSwitchChecked(true);
                setSwitchEnabled(true);
                setStartTime(true);
                return;
            default:
                enableWifiSwitch();
                return;
        }
    }

    private void setSwitchChecked(boolean checked) {
        this.mStateMachineEvent = true;
        if (this.mSwitchBar != null) {
            this.mSwitchBar.setChecked(checked);
        }
        sendBroadcast();
        Log.d("TetherWifiApEnabler", "setSwitchChecked checked = " + checked);
        this.mStateMachineEvent = false;
    }

    private void setSwitchEnabled(boolean enabled) {
        this.mStateMachineEvent = true;
        if (this.mSwitchBar != null) {
            this.mSwitchBar.setEnabled(enabled);
        }
        this.mStateMachineEvent = false;
    }

    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        sendBroadcast();
        if (!this.mStateMachineEvent) {
            Log.d("TetherWifiApEnabler", "onSwitchChanged, hotspot switch isChecked:" + isChecked);
            if (isChecked) {
                startProvisioningIfNecessary(0);
            } else {
                setSoftapEnabled(false);
            }
        }
    }

    boolean isProvisioningNeeded() {
        if (this.mProvisionApp == null || this.mProvisionApp.length != 2) {
            return false;
        }
        return true;
    }

    private void startProvisioningIfNecessary(int choice) {
        this.mTetherChoice = choice;
        if (isProvisioningNeeded()) {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.setClassName(this.mProvisionApp[0], this.mProvisionApp[1]);
            startActivityForResult(intent, 0);
            Log.d("TetherWifiApEnabler", "startProvisioningIfNecessary, startActivityForResult");
            return;
        }
        startTethering();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == 0 && resultCode == -1) {
            startTethering();
        }
    }

    private void startTethering() {
        if (this.mTetherChoice == 0) {
            Log.d("TetherWifiApEnabler", "startTethering, setSoftapEnabled");
            setSoftapEnabled(true);
        }
    }

    private void setStartTime(boolean enable) {
        long startTime = System.getLong(this.mContext.getContentResolver(), "wifi_hotspot_start_time", 0);
        if (!enable) {
            Log.d("TetherWifiApEnabler", "disable value: " + 0);
            System.putLong(this.mContext.getContentResolver(), "wifi_hotspot_start_time", 0);
        } else if (startTime == 0) {
            System.putLong(this.mContext.getContentResolver(), "wifi_hotspot_start_time", System.currentTimeMillis());
            Log.d("TetherWifiApEnabler", "enable value: " + System.currentTimeMillis());
        }
    }

    private void commitFragment() {
        if (this.mContext != null) {
            FragmentTransaction ft = ((Activity) this.mContext).getFragmentManager().beginTransaction();
            ft.add(this, "TetherWifiApEnabler");
            ft.commitAllowingStateLoss();
        }
    }

    private void sendBroadcast() {
        this.mContext.sendBroadcast(new Intent("action.wifi.tethered_switch"));
    }
}
