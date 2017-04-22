package com.mediatek.settings;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.os.storage.IMountService.Stub;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings.System;
import android.util.Log;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.Utils;
import com.mediatek.bluetooth.BluetoothDun;
import com.mediatek.bluetooth.BluetoothDun.ServiceListener;
import com.mediatek.settings.ext.IApnSettingsExt;
import java.util.concurrent.atomic.AtomicReference;

public class TetherSettingsExt implements OnPreferenceClickListener, OnPreferenceChangeListener {
    private AtomicReference<BluetoothDun> mBluetoothDun = new AtomicReference();
    private BluetoothDun mBluetoothDunProxy;
    private String[] mBluetoothRegexs;
    private int mBtErrorIpv4;
    private int mBtErrorIpv6;
    private ConnectivityManager mConnectService;
    private Context mContext;
    private ServiceListener mDunServiceListener = new C07231();
    IApnSettingsExt mExt;
    private IMountService mMountService = null;
    private PreferenceScreen mPrfscreen;
    private Resources mResources;
    private ListPreference mTetherIpv6;
    private int mUsbErrorIpv4 = 0;
    private int mUsbErrorIpv6 = 16;
    public ListPreference mUsbTetherType;
    public Preference mWifiTether;

    class C07231 implements ServiceListener {
        C07231() {
        }

        public void onServiceConnected(BluetoothDun proxy) {
            TetherSettingsExt.this.mBluetoothDun.set(proxy);
        }

        public void onServiceDisconnected() {
            TetherSettingsExt.this.mBluetoothDun.set(null);
            TetherSettingsExt.this.mBluetoothDunProxy = null;
        }
    }

    public TetherSettingsExt(Context context) {
        Log.d("TetherSettingsExt", "TetherSettingsExt");
        this.mContext = context;
        initServices();
    }

    public void onCreate(PreferenceScreen screen) {
        Log.d("TetherSettingsExt", "onCreate");
        this.mPrfscreen = screen;
        initPreference(screen);
        this.mExt = UtilsExt.getApnSettingsPlugin(this.mContext);
        this.mExt.customizeTetherApnSettings(screen);
        this.mBluetoothDunProxy = new BluetoothDun(this.mContext, this.mDunServiceListener);
    }

    public void onStart(Activity activity, BroadcastReceiver receiver) {
        activity.registerReceiver(receiver, getIntentFilter());
        if (this.mUsbTetherType != null) {
            this.mUsbTetherType.setOnPreferenceChangeListener(this);
            int value = System.getInt(activity.getContentResolver(), "usb_tethering_type", 0);
            this.mUsbTetherType.setValue(String.valueOf(value));
            this.mUsbTetherType.setSummary(activity.getResources().getStringArray(R.array.usb_tether_type_entries)[value]);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        String key = preference.getKey();
        Log.d("TetherSettingsExt", "onPreferenceChange key=" + key);
        if ("usb_tethering_type".equals(key)) {
            int index = Integer.parseInt((String) value);
            System.putInt(this.mContext.getContentResolver(), "usb_tethering_type", index);
            this.mUsbTetherType.setSummary(this.mResources.getStringArray(R.array.usb_tether_type_entries)[index]);
            Log.d("TetherSettingsExt", "onPreferenceChange USB_TETHERING_TYPE value = " + index);
        } else if ("tethered_ipv6".equals(key)) {
            int ipv6Value = Integer.parseInt(String.valueOf(value));
            if (this.mConnectService != null) {
                this.mConnectService.setTetheringIpv6Enable(ipv6Value == 1);
            }
            this.mTetherIpv6.setValueIndex(ipv6Value);
            this.mTetherIpv6.setSummary(this.mResources.getStringArray(R.array.tethered_ipv6_entries)[ipv6Value]);
        }
        return true;
    }

    public void updateWifiTether(Preference enableWifiAp, Preference wifiApSettings, boolean wifiAvailable) {
        this.mPrfscreen.removePreference(enableWifiAp);
        this.mPrfscreen.removePreference(wifiApSettings);
        if (wifiAvailable && !Utils.isMonkeyRunning()) {
            if (!Utils.isWifiOnly(this.mContext)) {
                return;
            }
        }
        this.mPrfscreen.removePreference(this.mWifiTether);
    }

    private void initPreference(PreferenceScreen screen) {
        this.mWifiTether = createPreference(4, R.string.wifi_tethering_title, "wifi_tether_settings", screen);
        this.mWifiTether.setOrder(-100);
        if (FeatureOption.MTK_TETHERING_EEM_SUPPORT) {
            this.mUsbTetherType = (ListPreference) createPreference(3, R.string.usb_tether_type_title, "usb_tethering_type", screen);
            this.mUsbTetherType.setEntries(R.array.usb_tether_type_entries);
            this.mUsbTetherType.setEntryValues(R.array.usb_tether_type_values);
            this.mUsbTetherType.setPersistent(false);
            int order = -99;
            Preference usbTetherSettings = screen.findPreference("usb_tether_settings");
            if (usbTetherSettings != null) {
                order = usbTetherSettings.getOrder() + 1;
            }
            this.mUsbTetherType.setOrder(order);
        }
        if ((!(this.mConnectService.getTetherableUsbRegexs().length != 0) || Utils.isMonkeyRunning()) && this.mUsbTetherType != null) {
            Log.d("TetherSettingsExt", "remove mUsbTetherType");
            screen.removePreference(this.mUsbTetherType);
        }
        if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
            this.mTetherIpv6 = (ListPreference) createPreference(3, R.string.tethered_ipv6_title, "tethered_ipv6", screen);
            this.mTetherIpv6.setEntries(R.array.tethered_ipv6_entries);
            this.mTetherIpv6.setEntryValues(R.array.tethered_ipv6_values);
            this.mTetherIpv6.setPersistent(false);
            this.mTetherIpv6.setOnPreferenceChangeListener(this);
        }
    }

    private Preference createPreference(int type, int titleRes, String key, PreferenceGroup screen) {
        Preference preference = null;
        switch (type) {
            case 0:
                preference = new PreferenceCategory(this.mContext);
                break;
            case 1:
                preference = new Preference(this.mContext);
                break;
            case 2:
                preference = new CheckBoxPreference(this.mContext);
                preference.setOnPreferenceClickListener(this);
                break;
            case 3:
                preference = new ListPreference(this.mContext);
                preference.setOnPreferenceClickListener(this);
                break;
            case 4:
                preference = new Preference(this.mContext);
                break;
        }
        preference.setKey(key);
        preference.setTitle(titleRes);
        screen.addPreference(preference);
        return preference;
    }

    public IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("mediatek.intent.action.USB_DATA_STATE");
        filter.addAction("android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED");
        filter.addAction("android.bluetooth.profilemanager.action.STATE_CHANGED");
        filter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        filter.addAction("action.wifi.tethered_switch");
        return filter;
    }

    public boolean onPreferenceClick(Preference preference) {
        if (preference == this.mWifiTether) {
            try {
                this.mContext.startActivity(new Intent("mediatek.intent.action.WIFI_TETHER"));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this.mContext, R.string.launch_error, 0).show();
            }
        }
        return true;
    }

    private synchronized void initServices() {
        if (this.mMountService == null) {
            IBinder service = ServiceManager.getService("mount");
            if (service != null) {
                this.mMountService = Stub.asInterface(service);
            } else {
                Log.e("TetherSettingsExt", "Can't get mount service");
            }
        }
        this.mConnectService = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        this.mResources = this.mContext.getResources();
        this.mBluetoothRegexs = this.mConnectService.getTetherableBluetoothRegexs();
    }

    public boolean isUMSEnabled() {
        if (this.mMountService == null) {
            Log.d("TetherSettingsExt", " mMountService is null, return");
            return false;
        }
        try {
            return this.mMountService.isUsbMassStorageEnabled();
        } catch (RemoteException e) {
            Log.e("TetherSettingsExt", "Util:RemoteException when isUsbMassStorageEnabled: " + e);
            return false;
        } catch (UnsupportedOperationException e2) {
            Log.e("TetherSettingsExt", "this device doesn't support UMS");
            return false;
        }
    }

    public void updateIpv6Preference(SwitchPreference usbTether, SwitchPreference bluetoothTether, WifiManager wifiManager) {
        boolean z = false;
        if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT && this.mTetherIpv6 != null && wifiManager != null) {
            ListPreference listPreference = this.mTetherIpv6;
            if (!(usbTether.isChecked() || bluetoothTether.isChecked() || wifiManager.isWifiApEnabled())) {
                z = true;
            }
            listPreference.setEnabled(z);
            if (this.mConnectService != null) {
                int ipv6Value = this.mConnectService.getTetheringIpv6Enable() ? 1 : 0;
                this.mTetherIpv6.setValueIndex(ipv6Value);
                this.mTetherIpv6.setSummary(this.mResources.getStringArray(R.array.tethered_ipv6_entries)[ipv6Value]);
            }
        }
    }

    public void updateBTPrfSummary(Preference pref, String originSummary) {
        if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
            pref.setSummary(originSummary + getIPV6String(this.mBtErrorIpv4, this.mBtErrorIpv6));
        } else {
            pref.setSummary(originSummary);
        }
    }

    public void updateUSBPrfSummary(Preference pref, String originSummary, boolean usbTethered, boolean usbAvailable) {
        if (usbTethered) {
            if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
                pref.setSummary(originSummary + getIPV6String(this.mUsbErrorIpv4, this.mUsbErrorIpv6));
            } else {
                pref.setSummary(originSummary);
            }
        }
        if (!usbAvailable || !FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
            return;
        }
        if (this.mUsbErrorIpv4 == 0 || this.mUsbErrorIpv6 == 16) {
            pref.setSummary(R.string.usb_tethering_available_subtext);
        } else {
            pref.setSummary(R.string.usb_tethering_errored_subtext);
        }
    }

    public void updateUsbTypeListState(boolean state) {
        if (this.mUsbTetherType != null) {
            Log.d("TetherSettingsExt", "set USB Tether Type state = " + state);
            this.mUsbTetherType.setEnabled(state);
        }
    }

    public void getBTErrorCode(String[] available) {
        if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
            this.mBtErrorIpv4 = 0;
            this.mBtErrorIpv6 = 16;
            for (String s : available) {
                for (String regex : this.mBluetoothRegexs) {
                    if (s.matches(regex) && this.mConnectService != null) {
                        if (this.mBtErrorIpv4 == 0) {
                            this.mBtErrorIpv4 = this.mConnectService.getLastTetherError(s) & 15;
                        }
                        if (this.mBtErrorIpv6 == 16) {
                            this.mBtErrorIpv6 = this.mConnectService.getLastTetherError(s) & 240;
                        }
                    }
                }
            }
        }
    }

    public String getIPV6String(int errorIpv4, int errorIpv6) {
        String text = "";
        if (this.mTetherIpv6 == null || !"1".equals(this.mTetherIpv6.getValue())) {
            return text;
        }
        Log.d("TetherSettingsExt", "[errorIpv4 =" + errorIpv4 + "];" + "[errorIpv6 =" + errorIpv6 + "];");
        if (errorIpv4 == 0 && errorIpv6 == 32) {
            return this.mResources.getString(R.string.tethered_ipv4v6);
        }
        if (errorIpv4 == 0) {
            return this.mResources.getString(R.string.tethered_ipv4);
        }
        if (errorIpv6 == 32) {
            return this.mResources.getString(R.string.tethered_ipv6);
        }
        return text;
    }

    public int getUSBErrorCode(String[] available, String[] tethered, String[] usbRegexs) {
        int usbError = 0;
        if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
            this.mUsbErrorIpv4 = 0;
            this.mUsbErrorIpv6 = 16;
        }
        for (String s : available) {
            for (String regex : usbRegexs) {
                if (s.matches(regex) && this.mConnectService != null) {
                    if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
                        if (this.mUsbErrorIpv4 == 0) {
                            this.mUsbErrorIpv4 = this.mConnectService.getLastTetherError(s) & 15;
                        }
                        if (this.mUsbErrorIpv6 == 16) {
                            this.mUsbErrorIpv6 = this.mConnectService.getLastTetherError(s) & 240;
                        }
                    } else if (usbError == 0) {
                        usbError = this.mConnectService.getLastTetherError(s);
                    }
                }
            }
        }
        if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
            for (String s2 : tethered) {
                for (String regex2 : usbRegexs) {
                    if (s2.matches(regex2) && this.mConnectService != null && this.mUsbErrorIpv6 == 16) {
                        this.mUsbErrorIpv6 = this.mConnectService.getLastTetherError(s2) & 240;
                    }
                }
            }
        }
        return usbError;
    }

    public void updateBtTetherState(SwitchPreference btPrf) {
        BluetoothDun dun = BluetoothDunGetProxy();
        if (dun == null || !dun.isTetheringOn() || btPrf == null) {
            btPrf.setChecked(false);
        } else {
            btPrf.setChecked(true);
        }
    }

    public void updateBtDunTether(boolean state) {
        BluetoothDun bluetoothDun = BluetoothDunGetProxy();
        if (bluetoothDun != null) {
            bluetoothDun.setBluetoothTethering(state);
        }
    }

    public BluetoothDun BluetoothDunGetProxy() {
        BluetoothDun Dun = (BluetoothDun) this.mBluetoothDun.get();
        if (Dun != null) {
            return Dun;
        }
        if (this.mBluetoothDunProxy != null) {
            this.mBluetoothDun.set(this.mBluetoothDunProxy);
        } else {
            this.mBluetoothDunProxy = new BluetoothDun(this.mContext, this.mDunServiceListener);
        }
        return this.mBluetoothDunProxy;
    }
}
