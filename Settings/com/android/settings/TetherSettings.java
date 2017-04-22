package com.android.settings;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.Log;
import android.widget.TextView;
import com.android.settings.wifi.WifiApDialog;
import com.android.settings.wifi.WifiApEnabler;
import com.android.settingslib.TetherUtil;
import com.mediatek.bluetooth.BluetoothDun;
import com.mediatek.settings.TetherSettingsExt;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class TetherSettings extends SettingsPreferenceFragment implements OnClickListener, OnPreferenceChangeListener {
    private boolean mBluetoothEnableForTether;
    private AtomicReference<BluetoothPan> mBluetoothPan = new AtomicReference();
    private String[] mBluetoothRegexs;
    private SwitchPreference mBluetoothTether;
    private Preference mCreateNetwork;
    private WifiApDialog mDialog;
    private SwitchPreference mEnableWifiAp;
    private boolean mIsPcKnowMe = true;
    private boolean mMassStorageActive;
    private ServiceListener mProfileServiceListener = new C01941();
    private String[] mProvisionApp;
    private String[] mSecurityType;
    private BroadcastReceiver mTetherChangeReceiver;
    private int mTetherChoice = -1;
    private TetherSettingsExt mTetherSettingsExt;
    private UserManager mUm;
    private boolean mUnavailable;
    private boolean mUsbConfigured;
    private boolean mUsbConnected;
    private boolean mUsbHwDisconnected;
    private String[] mUsbRegexs;
    private SwitchPreference mUsbTether;
    private boolean mUsbTetherCheckEnable = false;
    private boolean mUsbTetherDone = true;
    private boolean mUsbTetherFail = false;
    private boolean mUsbTethering = false;
    private boolean mUsbUnTetherDone = true;
    private WifiApEnabler mWifiApEnabler;
    private WifiConfiguration mWifiConfig = null;
    private WifiManager mWifiManager;
    private String[] mWifiRegexs;

    class C01941 implements ServiceListener {
        C01941() {
        }

        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            TetherSettings.this.mBluetoothPan.set((BluetoothPan) proxy);
        }

        public void onServiceDisconnected(int profile) {
            TetherSettings.this.mBluetoothPan.set(null);
        }
    }

    private class TetherChangeReceiver extends BroadcastReceiver {
        private TetherChangeReceiver() {
        }

        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            Log.d("TetherSettings", "TetherChangeReceiver - onReceive, action is " + action);
            if (action.equals("android.net.conn.TETHER_STATE_CHANGED")) {
                ArrayList<String> available = intent.getStringArrayListExtra("availableArray");
                ArrayList<String> active = intent.getStringArrayListExtra("activeArray");
                ArrayList<String> errored = intent.getStringArrayListExtra("erroredArray");
                TetherSettings.this.mUsbUnTetherDone = intent.getBooleanExtra("UnTetherDone", false);
                TetherSettings.this.mUsbTetherDone = intent.getBooleanExtra("TetherDone", false);
                TetherSettings.this.mUsbTetherFail = intent.getBooleanExtra("TetherFail", false);
                Log.d("TetherSettings", "mUsbUnTetherDone? :" + TetherSettings.this.mUsbUnTetherDone + " , mUsbTetherDonel? :" + TetherSettings.this.mUsbTetherDone + " , tether fail? :" + TetherSettings.this.mUsbTetherFail);
                TetherSettings.this.updateState((String[]) available.toArray(new String[available.size()]), (String[]) active.toArray(new String[active.size()]), (String[]) errored.toArray(new String[errored.size()]));
            } else if (action.equals("android.intent.action.MEDIA_SHARED")) {
                TetherSettings.this.mMassStorageActive = true;
                TetherSettings.this.updateState();
            } else if (action.equals("android.intent.action.MEDIA_UNSHARED")) {
                TetherSettings.this.mMassStorageActive = false;
                TetherSettings.this.updateState();
            } else if (action.equals("android.hardware.usb.action.USB_STATE")) {
                TetherSettings.this.mUsbConnected = intent.getBooleanExtra("connected", false);
                TetherSettings.this.mUsbConfigured = intent.getBooleanExtra("configured", false);
                TetherSettings.this.mUsbHwDisconnected = intent.getBooleanExtra("USB_HW_DISCONNECTED", false);
                TetherSettings.this.mIsPcKnowMe = intent.getBooleanExtra("USB_IS_PC_KNOW_ME", true);
                Log.d("TetherSettings", "TetherChangeReceiver - ACTION_USB_STATE mUsbConnected: " + TetherSettings.this.mUsbConnected + ", mUsbConfigured:  " + TetherSettings.this.mUsbConfigured + ", mUsbHwDisconnected: " + TetherSettings.this.mUsbHwDisconnected);
                TetherSettings.this.updateState();
            } else if (action.equals("android.bluetooth.adapter.action.STATE_CHANGED")) {
                if (TetherSettings.this.mBluetoothEnableForTether) {
                    switch (intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE)) {
                        case Integer.MIN_VALUE:
                        case 10:
                            TetherSettings.this.mBluetoothEnableForTether = false;
                            break;
                        case 12:
                            BluetoothPan bluetoothPan = (BluetoothPan) TetherSettings.this.mBluetoothPan.get();
                            if (bluetoothPan != null) {
                                bluetoothPan.setBluetoothTethering(true);
                                TetherSettings.this.mBluetoothEnableForTether = false;
                            }
                            BluetoothDun bluetoothDun = TetherSettings.this.mTetherSettingsExt.BluetoothDunGetProxy();
                            if (bluetoothDun != null) {
                                bluetoothDun.setBluetoothTethering(true);
                                TetherSettings.this.mBluetoothEnableForTether = false;
                                break;
                            }
                            break;
                    }
                }
                TetherSettings.this.updateState();
            }
            TetherSettings.this.onReceiveExt(action, intent);
        }
    }

    protected int getMetricsCategory() {
        return 90;
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (icicle != null) {
            this.mTetherChoice = icicle.getInt("TETHER_TYPE");
        }
        addPreferencesFromResource(R.xml.tether_prefs);
        this.mUm = (UserManager) getSystemService("user");
        if (this.mUm.hasUserRestriction("no_config_tethering") || UserHandle.myUserId() != 0) {
            this.mUnavailable = true;
            setPreferenceScreen(new PreferenceScreen(getActivity(), null));
            return;
        }
        this.mTetherSettingsExt = new TetherSettingsExt(getActivity());
        this.mTetherSettingsExt.onCreate(getPreferenceScreen());
        Activity activity = getActivity();
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            adapter.getProfileProxy(activity.getApplicationContext(), this.mProfileServiceListener, 5);
        }
        this.mEnableWifiAp = (SwitchPreference) findPreference("enable_wifi_ap");
        Preference wifiApSettings = findPreference("wifi_ap_ssid_and_security");
        this.mUsbTether = (SwitchPreference) findPreference("usb_tether_settings");
        this.mBluetoothTether = (SwitchPreference) findPreference("enable_bluetooth_tethering");
        ConnectivityManager cm = (ConnectivityManager) getSystemService("connectivity");
        this.mUsbRegexs = cm.getTetherableUsbRegexs();
        this.mWifiRegexs = cm.getTetherableWifiRegexs();
        this.mBluetoothRegexs = cm.getTetherableBluetoothRegexs();
        boolean usbAvailable = this.mUsbRegexs.length != 0;
        boolean wifiAvailable = this.mWifiRegexs.length != 0;
        boolean bluetoothAvailable = this.mBluetoothRegexs.length != 0;
        if (!usbAvailable || Utils.isMonkeyRunning()) {
            getPreferenceScreen().removePreference(this.mUsbTether);
        }
        if (!wifiAvailable || Utils.isMonkeyRunning()) {
            getPreferenceScreen().removePreference(this.mEnableWifiAp);
            getPreferenceScreen().removePreference(wifiApSettings);
        } else {
            initWifiTethering();
        }
        this.mWifiApEnabler = null;
        this.mTetherSettingsExt.updateWifiTether(this.mEnableWifiAp, wifiApSettings, wifiAvailable);
        if (bluetoothAvailable) {
            BluetoothPan pan = (BluetoothPan) this.mBluetoothPan.get();
            if (pan == null || !pan.isTetheringOn()) {
                this.mBluetoothTether.setChecked(false);
            } else {
                this.mBluetoothTether.setChecked(true);
            }
            this.mTetherSettingsExt.updateBtTetherState(this.mBluetoothTether);
        } else {
            getPreferenceScreen().removePreference(this.mBluetoothTether);
        }
        this.mProvisionApp = getResources().getStringArray(17235989);
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt("TETHER_TYPE", this.mTetherChoice);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void initWifiTethering() {
        Activity activity = getActivity();
        this.mWifiManager = (WifiManager) getSystemService("wifi");
        this.mWifiConfig = this.mWifiManager.getWifiApConfiguration();
        this.mSecurityType = getResources().getStringArray(R.array.wifi_ap_security);
        this.mCreateNetwork = findPreference("wifi_ap_ssid_and_security");
        if (this.mWifiConfig == null) {
            String s = activity.getString(17040282);
            this.mCreateNetwork.setSummary(String.format(activity.getString(R.string.wifi_tether_configure_subtext), new Object[]{s, this.mSecurityType[0]}));
            return;
        }
        int index = WifiApDialog.getSecurityTypeIndex(this.mWifiConfig);
        this.mCreateNetwork.setSummary(String.format(activity.getString(R.string.wifi_tether_configure_subtext), new Object[]{this.mWifiConfig.SSID, this.mSecurityType[index]}));
    }

    public Dialog onCreateDialog(int id) {
        if (id != 1) {
            return null;
        }
        this.mDialog = new WifiApDialog(getActivity(), this, this.mWifiConfig);
        return this.mDialog;
    }

    public void onStart() {
        super.onStart();
        if (this.mUnavailable) {
            TextView emptyView = (TextView) getView().findViewById(16908292);
            getListView().setEmptyView(emptyView);
            if (emptyView != null) {
                emptyView.setText(R.string.tethering_settings_not_available);
            }
            return;
        }
        Activity activity = getActivity();
        this.mMassStorageActive = this.mTetherSettingsExt.isUMSEnabled();
        Log.d("TetherSettings", "mMassStorageActive = " + this.mMassStorageActive);
        this.mTetherChangeReceiver = new TetherChangeReceiver();
        Intent intent = activity.registerReceiver(this.mTetherChangeReceiver, new IntentFilter("android.net.conn.TETHER_STATE_CHANGED"));
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.hardware.usb.action.USB_STATE");
        activity.registerReceiver(this.mTetherChangeReceiver, filter);
        filter = new IntentFilter();
        filter.addAction("android.intent.action.MEDIA_SHARED");
        filter.addAction("android.intent.action.MEDIA_UNSHARED");
        filter.addDataScheme("file");
        activity.registerReceiver(this.mTetherChangeReceiver, filter);
        filter = new IntentFilter();
        filter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        activity.registerReceiver(this.mTetherChangeReceiver, filter);
        this.mTetherSettingsExt.onStart(activity, this.mTetherChangeReceiver);
        if (intent != null) {
            this.mTetherChangeReceiver.onReceive(activity, intent);
        }
        if (this.mWifiApEnabler != null) {
            this.mEnableWifiAp.setOnPreferenceChangeListener(this);
            this.mWifiApEnabler.resume();
        }
        updateState();
    }

    public void onStop() {
        super.onStop();
        if (!this.mUnavailable) {
            getActivity().unregisterReceiver(this.mTetherChangeReceiver);
            this.mTetherChangeReceiver = null;
            if (this.mWifiApEnabler != null) {
                this.mEnableWifiAp.setOnPreferenceChangeListener(null);
                this.mWifiApEnabler.pause();
            }
        }
    }

    private void updateState() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService("connectivity");
        updateState(cm.getTetherableIfaces(), cm.getTetheredIfaces(), cm.getTetheringErroredIfaces());
    }

    private void updateState(String[] available, String[] tethered, String[] errored) {
        if (!updateStateExt(available, tethered, errored)) {
            updateUsbState(available, tethered, errored);
            updateBluetoothState(available, tethered, errored);
            if (!Utils.isMonkeyRunning()) {
                this.mTetherSettingsExt.updateIpv6Preference(this.mUsbTether, this.mBluetoothTether, this.mWifiManager);
            }
        }
    }

    private void updateUsbState(String[] available, String[] tethered, String[] errored) {
        ConnectivityManager cm = (ConnectivityManager) getSystemService("connectivity");
        boolean usbAvailable = this.mUsbConnected && !this.mMassStorageActive;
        int usbError = 0;
        for (String s : available) {
            for (String regex : this.mUsbRegexs) {
                if (s.matches(regex) && usbError == 0) {
                    usbError = cm.getLastTetherError(s);
                }
            }
        }
        boolean usbTethered = false;
        for (String s2 : tethered) {
            for (String regex2 : this.mUsbRegexs) {
                if (s2.matches(regex2)) {
                    usbTethered = true;
                }
            }
        }
        boolean usbErrored = false;
        for (String s22 : errored) {
            for (String regex22 : this.mUsbRegexs) {
                if (s22.matches(regex22)) {
                    usbErrored = true;
                }
            }
        }
        usbError = this.mTetherSettingsExt.getUSBErrorCode(available, tethered, this.mUsbRegexs);
        Log.d("TetherSettings", "updateUsbState - usbTethered : " + usbTethered + " usbErrored: " + usbErrored + " usbAvailable: " + usbAvailable);
        if (usbTethered) {
            Log.d("TetherSettings", "updateUsbState: usbTethered ! mUsbTether checkbox setEnabled & checked ");
            this.mUsbTether.setEnabled(true);
            this.mUsbTether.setChecked(true);
            this.mTetherSettingsExt.updateUSBPrfSummary(this.mUsbTether, getString(R.string.usb_tethering_active_subtext), usbTethered, usbAvailable);
            this.mUsbTethering = false;
            this.mTetherSettingsExt.updateUsbTypeListState(false);
            Log.d("TetherSettings", "updateUsbState - usbTethered - mUsbTetherCheckEnable: " + this.mUsbTetherCheckEnable);
        } else if (usbAvailable) {
            if (usbError == 0) {
                this.mUsbTether.setSummary(R.string.usb_tethering_available_subtext);
            } else {
                this.mUsbTether.setSummary(R.string.usb_tethering_errored_subtext);
            }
            this.mTetherSettingsExt.updateUSBPrfSummary(this.mUsbTether, null, usbTethered, usbAvailable);
            if (this.mUsbTetherCheckEnable) {
                Log.d("TetherSettings", "updateUsbState - mUsbTetherCheckEnable, mUsbTether checkbox setEnabled, and set unchecked ");
                this.mUsbTether.setEnabled(true);
                this.mUsbTether.setChecked(false);
                this.mUsbTethering = false;
                this.mTetherSettingsExt.updateUsbTypeListState(true);
            }
            Log.d("TetherSettings", "updateUsbState - usbAvailable - mUsbConfigured:  " + this.mUsbConfigured + " mUsbTethering: " + this.mUsbTethering + " mUsbTetherCheckEnable: " + this.mUsbTetherCheckEnable);
        } else if (usbErrored) {
            this.mUsbTether.setSummary(R.string.usb_tethering_errored_subtext);
            this.mUsbTether.setEnabled(false);
            this.mUsbTether.setChecked(false);
            this.mUsbTethering = false;
        } else if (this.mMassStorageActive) {
            this.mUsbTether.setSummary(R.string.usb_tethering_storage_active_subtext);
            this.mUsbTether.setEnabled(false);
            this.mUsbTether.setChecked(false);
            this.mUsbTethering = false;
        } else {
            if (this.mUsbHwDisconnected || !(this.mUsbHwDisconnected || this.mUsbConnected || this.mUsbConfigured)) {
                this.mUsbTether.setSummary(R.string.usb_tethering_unavailable_subtext);
                this.mUsbTether.setEnabled(false);
                this.mUsbTether.setChecked(false);
                this.mUsbTethering = false;
            } else {
                Log.d("TetherSettings", "updateUsbState - else, mUsbTether checkbox setEnabled, and set unchecked ");
                this.mUsbTether.setSummary(R.string.usb_tethering_available_subtext);
                this.mUsbTether.setEnabled(true);
                this.mUsbTether.setChecked(false);
                this.mUsbTethering = false;
                this.mTetherSettingsExt.updateUsbTypeListState(true);
            }
            Log.d("TetherSettings", "updateUsbState- usbAvailable- mUsbHwDisconnected:" + this.mUsbHwDisconnected);
        }
    }

    private void updateBluetoothState(String[] available, String[] tethered, String[] errored) {
        this.mTetherSettingsExt.getBTErrorCode(available);
        boolean bluetoothErrored = false;
        for (String s : errored) {
            for (String regex : this.mBluetoothRegexs) {
                if (s.matches(regex)) {
                    bluetoothErrored = true;
                }
            }
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            int btState = adapter.getState();
            Log.d("TetherSettings", "btState = " + btState);
            if (btState == 13) {
                this.mBluetoothTether.setEnabled(false);
                this.mBluetoothTether.setSummary(R.string.bluetooth_turning_off);
            } else if (btState == 11) {
                this.mBluetoothTether.setEnabled(false);
                this.mBluetoothTether.setSummary(R.string.bluetooth_turning_on);
            } else {
                BluetoothPan bluetoothPan = (BluetoothPan) this.mBluetoothPan.get();
                BluetoothDun bluetoothDun = this.mTetherSettingsExt.BluetoothDunGetProxy();
                if (btState != 12 || ((bluetoothPan == null || !bluetoothPan.isTetheringOn()) && (bluetoothDun == null || !bluetoothDun.isTetheringOn()))) {
                    this.mBluetoothTether.setEnabled(true);
                    this.mBluetoothTether.setChecked(false);
                    this.mBluetoothTether.setSummary(R.string.bluetooth_tethering_off_subtext);
                } else {
                    this.mBluetoothTether.setChecked(true);
                    this.mBluetoothTether.setEnabled(true);
                    int bluetoothTethered = 0;
                    if (bluetoothPan != null && bluetoothPan.isTetheringOn()) {
                        bluetoothTethered = bluetoothPan.getConnectedDevices().size();
                        Log.d("TetherSettings", "bluetooth Tethered PAN devices = " + bluetoothTethered);
                    }
                    if (bluetoothDun != null && bluetoothDun.isTetheringOn()) {
                        bluetoothTethered += bluetoothDun.getConnectedDevices().size();
                        Log.d("TetherSettings", "bluetooth tethered total devices = " + bluetoothTethered);
                    }
                    if (bluetoothTethered > 1) {
                        this.mTetherSettingsExt.updateBTPrfSummary(this.mBluetoothTether, getString(R.string.bluetooth_tethering_devices_connected_subtext, new Object[]{Integer.valueOf(bluetoothTethered)}));
                    } else if (bluetoothTethered == 1) {
                        this.mTetherSettingsExt.updateBTPrfSummary(this.mBluetoothTether, getString(R.string.bluetooth_tethering_device_connected_subtext));
                    } else if (bluetoothErrored) {
                        this.mBluetoothTether.setSummary(R.string.bluetooth_tethering_errored_subtext);
                    } else {
                        this.mTetherSettingsExt.updateBTPrfSummary(this.mBluetoothTether, getString(R.string.bluetooth_tethering_available_subtext));
                    }
                }
            }
        }
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        this.mTetherSettingsExt.onPreferenceChange(preference, value);
        return true;
    }

    public static boolean isProvisioningNeededButUnavailable(Context context) {
        if (!TetherUtil.isProvisioningNeeded(context) || isIntentAvailable(context)) {
            return false;
        }
        return true;
    }

    private static boolean isIntentAvailable(Context context) {
        String[] provisionApp = context.getResources().getStringArray(17235989);
        PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setClassName(provisionApp[0], provisionApp[1]);
        if (packageManager.queryIntentActivities(intent, 65536).size() > 0) {
            return true;
        }
        return false;
    }

    private void startProvisioningIfNecessary(int choice) {
        this.mTetherChoice = choice;
        if (TetherUtil.isProvisioningNeeded(getActivity())) {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.setClassName(this.mProvisionApp[0], this.mProvisionApp[1]);
            intent.putExtra("TETHER_TYPE", this.mTetherChoice);
            startActivityForResult(intent, 0);
            return;
        }
        startTethering();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode != 0) {
            return;
        }
        if (resultCode == -1) {
            TetherService.scheduleRecheckAlarm(getActivity(), this.mTetherChoice);
            startTethering();
            return;
        }
        switch (this.mTetherChoice) {
            case 1:
                this.mUsbTether.setChecked(false);
                break;
            case 2:
                this.mBluetoothTether.setChecked(false);
                break;
        }
        this.mTetherChoice = -1;
    }

    private void startTethering() {
        switch (this.mTetherChoice) {
            case 0:
                this.mWifiApEnabler.setSoftapEnabled(true);
                return;
            case 1:
                setUsbTethering(true);
                return;
            case 2:
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (adapter.getState() == 10) {
                    this.mBluetoothEnableForTether = true;
                    adapter.enable();
                    this.mBluetoothTether.setSummary(R.string.bluetooth_turning_on);
                    this.mBluetoothTether.setEnabled(false);
                    return;
                }
                BluetoothPan bluetoothPan = (BluetoothPan) this.mBluetoothPan.get();
                if (bluetoothPan != null) {
                    bluetoothPan.setBluetoothTethering(true);
                }
                this.mTetherSettingsExt.updateBtDunTether(true);
                this.mTetherSettingsExt.updateBTPrfSummary(this.mBluetoothTether, getString(R.string.bluetooth_tethering_available_subtext));
                return;
            default:
                return;
        }
    }

    private void setUsbTethering(boolean enabled) {
        if (((ConnectivityManager) getSystemService("connectivity")).setUsbTethering(enabled) != 0) {
            this.mUsbTether.setChecked(false);
            this.mUsbTether.setSummary(R.string.usb_tethering_errored_subtext);
            return;
        }
        this.mUsbTether.setSummary("");
    }

    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        ConnectivityManager cm = (ConnectivityManager) getSystemService("connectivity");
        if (preference == this.mUsbTether) {
            if (this.mUsbTethering) {
                return true;
            }
            boolean newState = this.mUsbTether.isChecked();
            this.mUsbTether.setEnabled(false);
            this.mTetherSettingsExt.updateUsbTypeListState(false);
            this.mUsbTethering = true;
            this.mUsbTetherCheckEnable = false;
            if (newState) {
                this.mUsbTetherDone = false;
            } else {
                this.mUsbUnTetherDone = false;
            }
            this.mUsbTetherFail = false;
            Log.d("TetherSettings", "onPreferenceTreeClick - setusbTethering(" + newState + ") mUsbTethering:  " + this.mUsbTethering);
            if (newState) {
                startProvisioningIfNecessary(1);
            } else {
                if (TetherUtil.isProvisioningNeeded(getActivity())) {
                    TetherService.cancelRecheckAlarmIfNecessary(getActivity(), 1);
                }
                setUsbTethering(newState);
            }
        } else if (preference == this.mBluetoothTether) {
            if (this.mBluetoothTether.isChecked()) {
                startProvisioningIfNecessary(2);
            } else {
                if (TetherUtil.isProvisioningNeeded(getActivity())) {
                    TetherService.cancelRecheckAlarmIfNecessary(getActivity(), 2);
                }
                boolean errored = false;
                String bluetoothIface = findIface(cm.getTetheredIfaces(), this.mBluetoothRegexs);
                if (!(bluetoothIface == null || cm.untether(bluetoothIface) == 0)) {
                    errored = true;
                }
                BluetoothPan bluetoothPan = (BluetoothPan) this.mBluetoothPan.get();
                if (bluetoothPan != null) {
                    bluetoothPan.setBluetoothTethering(false);
                }
                this.mTetherSettingsExt.updateBtDunTether(false);
                if (errored) {
                    this.mBluetoothTether.setSummary(R.string.bluetooth_tethering_errored_subtext);
                } else {
                    this.mBluetoothTether.setSummary(R.string.bluetooth_tethering_off_subtext);
                }
            }
            if (!Utils.isMonkeyRunning()) {
                this.mTetherSettingsExt.updateIpv6Preference(this.mUsbTether, this.mBluetoothTether, this.mWifiManager);
            }
        } else if (preference == this.mCreateNetwork) {
            showDialog(1);
        }
        this.mTetherSettingsExt.onPreferenceClick(preference);
        return super.onPreferenceTreeClick(screen, preference);
    }

    private static String findIface(String[] ifaces, String[] regexes) {
        for (String iface : ifaces) {
            for (String regex : regexes) {
                if (iface.matches(regex)) {
                    return iface;
                }
            }
        }
        return null;
    }

    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == -1) {
            this.mWifiConfig = this.mDialog.getConfig();
            if (this.mWifiConfig != null) {
                if (this.mWifiManager.getWifiApState() == 13) {
                    this.mWifiManager.setWifiApEnabled(null, false);
                    this.mWifiManager.setWifiApEnabled(this.mWifiConfig, true);
                } else {
                    this.mWifiManager.setWifiApConfiguration(this.mWifiConfig);
                }
                int index = WifiApDialog.getSecurityTypeIndex(this.mWifiConfig);
                this.mCreateNetwork.setSummary(String.format(getActivity().getString(R.string.wifi_tether_configure_subtext), new Object[]{this.mWifiConfig.SSID, this.mSecurityType[index]}));
            }
        }
    }

    public int getHelpResource() {
        return R.string.help_url_tether;
    }

    private boolean updateStateExt(String[] available, String[] tethered, String[] errored) {
        Log.d("TetherSettings", "=======> updateState - mUsbConnected: " + this.mUsbConnected + ", mUsbConfigured:  " + this.mUsbConfigured + ", mUsbHwDisconnected: " + this.mUsbHwDisconnected + ", checked: " + this.mUsbTether.isChecked() + ", mUsbUnTetherDone: " + this.mUsbUnTetherDone + ", mUsbTetherDone: " + this.mUsbTetherDone + ", tetherfail: " + this.mUsbTetherFail + ", mIsPcKnowMe: " + this.mIsPcKnowMe);
        if (this.mUsbTether.isChecked()) {
            if (!this.mUsbConnected || !this.mUsbConfigured || this.mUsbHwDisconnected) {
                this.mUsbTetherCheckEnable = false;
            } else if (this.mUsbTetherFail || this.mUsbTetherDone || !this.mIsPcKnowMe) {
                this.mUsbTetherCheckEnable = true;
            }
        } else if (!this.mUsbConnected || this.mUsbHwDisconnected) {
            this.mUsbTetherCheckEnable = false;
        } else if (this.mUsbUnTetherDone || this.mUsbTetherFail) {
            this.mUsbTetherCheckEnable = true;
        }
        return false;
    }

    private void onReceiveExt(String action, Intent intent) {
        if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(action)) {
            int state = intent.getIntExtra("wifi_state", 14);
            if ((state == 13 || state == 11) && !Utils.isMonkeyRunning()) {
                this.mTetherSettingsExt.updateIpv6Preference(this.mUsbTether, this.mBluetoothTether, this.mWifiManager);
            }
        } else if (action.equals("android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED") || action.equals("android.bluetooth.profilemanager.action.STATE_CHANGED")) {
            updateState();
        } else if ("action.wifi.tethered_switch".equals(action) && !Utils.isMonkeyRunning()) {
            this.mTetherSettingsExt.updateIpv6Preference(this.mUsbTether, this.mBluetoothTether, this.mWifiManager);
        }
    }
}
