package com.mediatek.wifi.hotspot;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.wifi.HotspotClient;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings.System;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.wifi.WifiApDialog;
import com.mediatek.custom.CustomProperties;
import java.util.List;

public class TetherWifiSettings extends SettingsPreferenceFragment implements OnClickListener, OnPreferenceChangeListener, OnButtonClickCallback {
    private static final String BANDWIDTH = "bandwidth_usage";
    private static final String BLOCKED_CATEGORY = "blocked_category";
    private static final int CONFIG_SUBTEXT = 2131428309;
    private static final String CONNECTED_CATEGORY = "connected_category";
    private static final int DIALOG_AP_CLIENT_DETAIL = 3;
    private static final int DIALOG_AP_SETTINGS = 2;
    private static final int DIALOG_WPS_CONNECT = 1;
    private static final String TAG = "TetherWifiSettings";
    private static final int WIFI_AP_AUTO_CHANNEL_TEXT = 2131427422;
    private static final int WIFI_AP_AUTO_CHANNEL_WIDTH_TEXT = 2131427423;
    private static final int WIFI_AP_FIX_CHANNEL_WIDTH_TEXT = 2131427424;
    private static final String WIFI_AP_SSID_AND_SECURITY = "wifi_ap_ssid_and_security";
    private static final String WIFI_AUTO_DISABLE = "wifi_auto_disable";
    private static final String WPS_CONNECT = "wps_connect";
    private Preference mBandwidth;
    private PreferenceCategory mBlockedCategory;
    private List<HotspotClient> mClientList;
    private PreferenceCategory mConnectedCategory;
    private Preference mCreateNetwork;
    private View mDetailView;
    private WifiApDialog mDialog;
    private IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new C07771();
    private String[] mSecurityType;
    private TetherWifiApEnabler mTetherWifiApEnabler;
    private ListPreference mWifiAutoDisable;
    private WifiConfiguration mWifiConfig = null;
    private WifiManager mWifiManager;
    private String[] mWifiRegexs;
    private Preference mWpsConnect;

    class C07771 extends BroadcastReceiver {
        C07771() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("@M_TetherWifiSettings", "receive action: " + action);
            if ("android.net.wifi.WIFI_HOTSPOT_CLIENTS_CHANGED".equals(action)) {
                TetherWifiSettings.this.handleWifiApClientsChanged();
            } else if ("android.net.wifi.WIFI_WPS_CHECK_PIN_FAIL".equals(action)) {
                Toast.makeText(context, R.string.wifi_tether_wps_pin_error, 1).show();
            } else if ("android.net.wifi.WIFI_HOTSPOT_OVERLAP".equals(action)) {
                Toast.makeText(context, R.string.wifi_wps_failed_overlap, 1).show();
            } else if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(action)) {
                TetherWifiSettings.this.handleWifiApStateChanged(intent.getIntExtra("wifi_state", 14));
            }
        }
    }

    class C07782 implements OnClickListener {
        C07782() {
        }

        public void onClick(DialogInterface dialog, int which) {
        }
    }

    protected int getMetricsCategory() {
        return 100007;
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.tether_wifi_prefs);
        SettingsActivity activity = (SettingsActivity) getActivity();
        this.mWifiAutoDisable = (ListPreference) findPreference(WIFI_AUTO_DISABLE);
        Preference wifiApSettings = findPreference(WIFI_AP_SSID_AND_SECURITY);
        this.mWpsConnect = findPreference(WPS_CONNECT);
        this.mWpsConnect.setEnabled(false);
        this.mBandwidth = findPreference(BANDWIDTH);
        this.mBandwidth.setEnabled(false);
        ConnectivityManager cm = (ConnectivityManager) getSystemService("connectivity");
        initWifiTethering();
        this.mIntentFilter = new IntentFilter("android.net.wifi.WIFI_HOTSPOT_CLIENTS_CHANGED");
        this.mIntentFilter.addAction("android.net.wifi.WIFI_WPS_CHECK_PIN_FAIL");
        this.mIntentFilter.addAction("android.net.wifi.WIFI_HOTSPOT_OVERLAP");
        this.mIntentFilter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        this.mConnectedCategory = (PreferenceCategory) findPreference(CONNECTED_CATEGORY);
        this.mBlockedCategory = (PreferenceCategory) findPreference(BLOCKED_CATEGORY);
        this.mDetailView = getActivity().getLayoutInflater().inflate(R.layout.wifi_ap_client_dialog, null);
    }

    public void onDestroyView() {
        super.onDestroyView();
    }

    public void onStart() {
        super.onStart();
        this.mTetherWifiApEnabler = createTetherWifiApEnabler();
    }

    public void onStop() {
        super.onStop();
        if (this.mTetherWifiApEnabler != null) {
            this.mTetherWifiApEnabler.teardownSwitchBar();
        }
    }

    TetherWifiApEnabler createTetherWifiApEnabler() {
        SettingsActivity activity = (SettingsActivity) getActivity();
        return new TetherWifiApEnabler(activity.getSwitchBar(), activity);
    }

    private void initWifiTethering() {
        Activity activity = getActivity();
        this.mWifiManager = (WifiManager) activity.getApplicationContext().getSystemService("wifi");
        this.mWifiConfig = this.mWifiManager.getWifiApConfiguration();
        this.mSecurityType = getResources().getStringArray(R.array.wifi_ap_security);
        this.mCreateNetwork = findPreference(WIFI_AP_SSID_AND_SECURITY);
        if (this.mWifiConfig == null) {
            String s = CustomProperties.getString("wlan", "SSID", activity.getString(17040282));
            this.mCreateNetwork.setSummary(String.format(activity.getString(R.string.wifi_tether_configure_subtext), new Object[]{s, this.mSecurityType[0]}));
            return;
        }
        Log.d("@M_TetherWifiSettings", "index = " + WifiApDialog.getSecurityTypeIndex(this.mWifiConfig));
        this.mCreateNetwork.setSummary(String.format(activity.getString(R.string.wifi_tether_configure_subtext), new Object[]{this.mWifiConfig.SSID, this.mSecurityType[index]}));
    }

    public void onResume() {
        super.onResume();
        if (this.mTetherWifiApEnabler != null) {
            this.mTetherWifiApEnabler.resume();
        }
        if (this.mWifiAutoDisable != null) {
            this.mWifiAutoDisable.setOnPreferenceChangeListener(this);
            this.mWifiAutoDisable.setValue(String.valueOf(System.getInt(getContentResolver(), "wifi_hotspot_auto_disable", 1)));
        }
        getActivity().registerReceiver(this.mReceiver, this.mIntentFilter);
        handleWifiApClientsChanged();
    }

    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(this.mReceiver);
        if (this.mTetherWifiApEnabler != null) {
            this.mTetherWifiApEnabler.pause();
        }
        if (this.mWifiAutoDisable != null) {
            this.mWifiAutoDisable.setOnPreferenceChangeListener(null);
        }
    }

    public Dialog onCreateDialog(int id) {
        if (id == 2) {
            this.mDialog = new WifiApDialog(getActivity(), this, this.mWifiConfig);
            return this.mDialog;
        } else if (id == 1) {
            Dialog d = new WifiApWpsDialog(getActivity());
            Log.d("@M_TetherWifiSettings", "onCreateDialog, return dialog");
            return d;
        } else if (id != 3) {
            return null;
        } else {
            ViewParent parent = this.mDetailView.getParent();
            if (parent != null && (parent instanceof ViewGroup)) {
                ((ViewGroup) parent).removeView(this.mDetailView);
            }
            return new Builder(getActivity()).setTitle(R.string.wifi_ap_client_details_title).setView(this.mDetailView).setNegativeButton(17039360, new C07782()).create();
        }
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        String key = preference.getKey();
        Log.d("@M_TetherWifiSettings", "onPreferenceChange key=" + key);
        if (WIFI_AUTO_DISABLE.equals(key)) {
            System.putInt(getContentResolver(), "wifi_hotspot_auto_disable", Integer.parseInt((String) value));
            Log.d("@M_TetherWifiSettings", "onPreferenceChange auto disable value=" + Integer.parseInt((String) value));
        }
        return true;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        ConnectivityManager cm = (ConnectivityManager) getSystemService("connectivity");
        if (preference == this.mCreateNetwork) {
            showDialog(2);
        } else if (preference == this.mWpsConnect) {
            showDialog(1);
        } else if (preference instanceof ButtonPreference) {
            removeDialog(3);
            ButtonPreference client = (ButtonPreference) preference;
            ((TextView) this.mDetailView.findViewById(R.id.mac_address)).setText(client.getMacAddress());
            if (client.isBlocked()) {
                this.mDetailView.findViewById(R.id.ip_filed).setVisibility(8);
            } else {
                this.mDetailView.findViewById(R.id.ip_filed).setVisibility(0);
                String ipAddr = this.mWifiManager.getClientIp(client.getMacAddress());
                Log.d("@M_TetherWifiSettings", "connected client ip address is:" + ipAddr);
                ((TextView) this.mDetailView.findViewById(R.id.ip_address)).setText(ipAddr);
            }
            showDialog(3);
        }
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
                if (WifiApDialog.getSecurityTypeIndex(this.mWifiConfig) == 0) {
                    Toast.makeText(getActivity(), R.string.security_not_set, 1).show();
                }
                this.mCreateNetwork.setSummary(String.format(getActivity().getString(R.string.wifi_tether_configure_subtext), new Object[]{this.mWifiConfig.SSID, this.mSecurityType[index]}));
            }
        }
    }

    public void onClick(View v, HotspotClient client) {
        if (v.getId() == R.id.preference_button && client != null) {
            if (client.isBlocked) {
                Log.d("@M_TetherWifiSettings", "onClick,client is blocked, unblock now");
                this.mWifiManager.unblockClient(client);
            } else {
                Log.d("@M_TetherWifiSettings", "onClick,client isn't blocked, block now");
                this.mWifiManager.blockClient(client);
            }
            handleWifiApClientsChanged();
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (this.mDialog != null) {
            this.mDialog.closeSpinnerDialog();
        }
    }

    private void handleWifiApClientsChanged() {
        this.mConnectedCategory.removeAll();
        this.mBlockedCategory.removeAll();
        this.mClientList = this.mWifiManager.getHotspotClients();
        if (this.mClientList != null) {
            Preference preference;
            Log.d("@M_TetherWifiSettings", "client number is " + this.mClientList.size());
            for (HotspotClient client : this.mClientList) {
                ButtonPreference preference2 = new ButtonPreference(getActivity(), client, this);
                preference2.setMacAddress(client.deviceAddress);
                if (client.isBlocked) {
                    preference2.setButtonText(getResources().getString(R.string.wifi_ap_client_unblock_title));
                    this.mBlockedCategory.addPreference(preference2);
                    Log.d("@M_TetherWifiSettings", "blocked client MAC is " + client.deviceAddress);
                } else {
                    preference2.setButtonText(getResources().getString(R.string.wifi_ap_client_block_title));
                    this.mConnectedCategory.addPreference(preference2);
                    Log.d("@M_TetherWifiSettings", "connected client MAC is " + client.deviceAddress);
                }
            }
            if (this.mConnectedCategory.getPreferenceCount() == 0) {
                preference = new Preference(getActivity());
                preference.setTitle(R.string.wifi_ap_no_connected);
                this.mConnectedCategory.addPreference(preference);
            }
            if (this.mBlockedCategory.getPreferenceCount() == 0) {
                preference = new Preference(getActivity());
                preference.setTitle(R.string.wifi_ap_no_blocked);
                this.mBlockedCategory.addPreference(preference);
            }
        }
    }

    private void handleWifiApStateChanged(int state) {
        switch (state) {
            case 10:
                setPreferenceState(false);
                removeDialog(1);
                return;
            case 11:
                setPreferenceState(false);
                removeDialog(1);
                return;
            case 12:
                setPreferenceState(false);
                return;
            case 13:
                setPreferenceState(true);
                return;
            default:
                return;
        }
    }

    private void setPreferenceState(boolean enabled) {
        Log.d("@M_TetherWifiSettings", "setPreferenceState, enabled = " + enabled);
        this.mBandwidth.setEnabled(enabled);
        this.mWpsConnect.setEnabled(enabled);
    }
}
