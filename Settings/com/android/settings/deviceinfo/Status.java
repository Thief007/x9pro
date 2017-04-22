package com.android.settings.deviceinfo;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.Preference;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListAdapter;
import android.widget.Toast;
import com.android.internal.util.ArrayUtils;
import com.android.settings.InstrumentedPreferenceActivity;
import com.android.settings.R;
import com.android.settings.Utils;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISettingsMiscExt;
import java.lang.ref.WeakReference;

public class Status extends InstrumentedPreferenceActivity {
    private static final String[] CONNECTIVITY_INTENTS = new String[]{"android.bluetooth.adapter.action.STATE_CHANGED", "android.net.conn.CONNECTIVITY_CHANGE", "android.net.wifi.LINK_CONFIGURATION_CHANGED", "android.net.wifi.STATE_CHANGE"};
    private BroadcastReceiver mBatteryInfoReceiver = new C03391();
    private Preference mBatteryLevel;
    private Preference mBatteryStatus;
    private Preference mBtAddress;
    private ConnectivityManager mCM;
    private IntentFilter mConnectivityIntentFilter;
    private final BroadcastReceiver mConnectivityReceiver = new C03402();
    private ISettingsMiscExt mExt;
    private Handler mHandler;
    private Preference mIpAddress;
    private Resources mRes;
    private String mUnavailable;
    private String mUnknown;
    private Preference mUptime;
    private Preference mWifiMacAddress;
    private WifiManager mWifiManager;
    private Preference mWimaxMacAddress;

    class C03391 extends BroadcastReceiver {
        C03391() {
        }

        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.BATTERY_CHANGED".equals(intent.getAction())) {
                Status.this.mBatteryLevel.setSummary(Utils.getBatteryPercentage(intent));
                Status.this.mBatteryStatus.setSummary(Utils.getBatteryStatus(Status.this.getResources(), intent));
            }
        }
    }

    class C03402 extends BroadcastReceiver {
        C03402() {
        }

        public void onReceive(Context context, Intent intent) {
            if (ArrayUtils.contains(Status.CONNECTIVITY_INTENTS, intent.getAction())) {
                Status.this.mHandler.sendEmptyMessage(600);
            }
        }
    }

    class C03413 implements OnItemLongClickListener {
        C03413() {
        }

        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            ((ClipboardManager) Status.this.getSystemService("clipboard")).setText(((Preference) ((ListAdapter) parent.getAdapter()).getItem(position)).getSummary());
            Toast.makeText(Status.this, 17040145, 0).show();
            return true;
        }
    }

    private static class MyHandler extends Handler {
        private WeakReference<Status> mStatus;

        public MyHandler(Status activity) {
            this.mStatus = new WeakReference(activity);
        }

        public void handleMessage(Message msg) {
            Status status = (Status) this.mStatus.get();
            if (status != null) {
                switch (msg.what) {
                    case 500:
                        status.updateTimes();
                        sendEmptyMessageDelayed(500, 1000);
                        break;
                    case 600:
                        status.updateConnectivity();
                        break;
                }
            }
        }
    }

    private boolean hasBluetooth() {
        return BluetoothAdapter.getDefaultAdapter() != null;
    }

    private boolean hasWimax() {
        return this.mCM.getNetworkInfo(6) != null;
    }

    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mExt = UtilsExt.getMiscPlugin(this);
        this.mHandler = new MyHandler(this);
        this.mCM = (ConnectivityManager) getSystemService("connectivity");
        this.mWifiManager = (WifiManager) getSystemService("wifi");
        addPreferencesFromResource(R.xml.device_info_status);
        this.mBatteryLevel = findPreference("battery_level");
        this.mBatteryStatus = findPreference("battery_status");
        this.mBtAddress = findPreference("bt_address");
        this.mWifiMacAddress = findPreference("wifi_mac_address");
        this.mWimaxMacAddress = findPreference("wimax_mac_address");
        this.mIpAddress = findPreference("wifi_ip_address");
        changeSimTitle();
        this.mRes = getResources();
        this.mUnknown = this.mRes.getString(R.string.device_info_default);
        this.mUnavailable = this.mRes.getString(R.string.status_unavailable);
        this.mUptime = findPreference("up_time");
        if (!hasBluetooth()) {
            getPreferenceScreen().removePreference(this.mBtAddress);
            this.mBtAddress = null;
        }
        if (!hasWimax()) {
            getPreferenceScreen().removePreference(this.mWimaxMacAddress);
            this.mWimaxMacAddress = null;
        }
        this.mConnectivityIntentFilter = new IntentFilter();
        for (String intent : CONNECTIVITY_INTENTS) {
            this.mConnectivityIntentFilter.addAction(intent);
        }
        updateConnectivity();
        String serial = Build.SERIAL;
        if (serial == null || serial.equals("")) {
            removePreferenceFromScreen("serial_number");
        } else {
            setSummaryText("serial_number", serial);
        }
        if (Utils.isWifiOnly(this) || UserHandle.myUserId() != 0) {
            removePreferenceFromScreen("sim_status");
            removePreferenceFromScreen("imei_info");
        }
        getListView().setOnItemLongClickListener(new C03413());
    }

    protected int getMetricsCategory() {
        return 44;
    }

    private void changeSimTitle() {
        findPreference("sim_status").setTitle(this.mExt.customizeSimDisplayString(findPreference("sim_status").getTitle().toString(), -1));
    }

    protected void onResume() {
        super.onResume();
        registerReceiver(this.mConnectivityReceiver, this.mConnectivityIntentFilter, "android.permission.CHANGE_NETWORK_STATE", null);
        registerReceiver(this.mBatteryInfoReceiver, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        this.mHandler.sendEmptyMessage(500);
    }

    public void onPause() {
        super.onPause();
        unregisterReceiver(this.mBatteryInfoReceiver);
        unregisterReceiver(this.mConnectivityReceiver);
        this.mHandler.removeMessages(500);
    }

    private void removePreferenceFromScreen(String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    private void setSummaryText(String preference, String text) {
        if (TextUtils.isEmpty(text)) {
            text = this.mUnknown;
        }
        if (findPreference(preference) != null) {
            findPreference(preference).setSummary(text);
        }
    }

    private void setWimaxStatus() {
        if (this.mWimaxMacAddress != null) {
            this.mWimaxMacAddress.setSummary(SystemProperties.get("net.wimax.mac.address", this.mUnavailable));
        }
    }

    private void setWifiStatus() {
        WifiInfo wifiInfo = this.mWifiManager.getConnectionInfo();
        CharSequence macAddress = wifiInfo == null ? null : wifiInfo.getMacAddress();
        Preference preference = this.mWifiMacAddress;
        if (TextUtils.isEmpty(macAddress)) {
            macAddress = this.mUnavailable;
        }
        preference.setSummary(macAddress);
    }

    private void setIpAddressStatus() {
        String ipAddress = Utils.getDefaultIpAddresses(this.mCM);
        if (ipAddress != null) {
            this.mIpAddress.setSummary(ipAddress);
        } else {
            this.mIpAddress.setSummary(this.mUnavailable);
        }
    }

    private void setBtStatus() {
        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        if (bluetooth != null && this.mBtAddress != null) {
            Object address = bluetooth.isEnabled() ? bluetooth.getAddress() : null;
            if (TextUtils.isEmpty(address)) {
                this.mBtAddress.setSummary(this.mUnavailable);
            } else {
                this.mBtAddress.setSummary(address.toLowerCase());
            }
        }
    }

    void updateConnectivity() {
        setWimaxStatus();
        setWifiStatus();
        setBtStatus();
        setIpAddressStatus();
    }

    void updateTimes() {
        long at = SystemClock.uptimeMillis() / 1000;
        long ut = SystemClock.elapsedRealtime() / 1000;
        if (ut == 0) {
            ut = 1;
        }
        this.mUptime.setSummary(convert(ut));
    }

    private String pad(int n) {
        if (n >= 10) {
            return String.valueOf(n);
        }
        return "0" + String.valueOf(n);
    }

    private String convert(long t) {
        return ((int) (t / 3600)) + ":" + pad((int) ((t / 60) % 60)) + ":" + pad((int) (t % 60));
    }
}
