package com.mediatek.wifi;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IWifiExt;

public class AdvancedWifiSettingsExt {
    private Activity mActivity;
    private IWifiExt mExt;
    private SettingsPreferenceFragment mFragment;
    private IntentFilter mIntentFilter;
    private Preference mIpAddressPref;
    private Preference mIpv6AddressPref;
    private Preference mMacAddressPref;
    private SwitchPreference mNotifyOpenNetworks;
    private final BroadcastReceiver mReceiver = new C07511();
    private WifiManager mWifiManager;

    class C07511 extends BroadcastReceiver {
        C07511() {
        }

        public void onReceive(Context context, Intent intent) {
            if ("android.net.wifi.WIFI_STATE_CHANGED".equals(intent.getAction())) {
                int state = intent.getIntExtra("wifi_state", 4);
                if (state == 3) {
                    AdvancedWifiSettingsExt.this.mNotifyOpenNetworks.setEnabled(true);
                } else if (state == 1) {
                    AdvancedWifiSettingsExt.this.mNotifyOpenNetworks.setEnabled(false);
                }
            }
        }
    }

    public AdvancedWifiSettingsExt(SettingsPreferenceFragment fragment) {
        Log.d("AdvancedWifiSettingsExt", "AdvancedWifiSettingsExt");
        this.mFragment = fragment;
        if (fragment != null) {
            this.mActivity = fragment.getActivity();
        }
    }

    public void onCreate() {
        Log.d("AdvancedWifiSettingsExt", "onCreate");
        this.mExt = UtilsExt.getWifiPlugin(this.mActivity);
        this.mIntentFilter = new IntentFilter("android.net.wifi.WIFI_STATE_CHANGED");
    }

    public void onActivityCreated(ContentResolver cr) {
        Log.d("AdvancedWifiSettingsExt", "onActivityCreated");
        this.mExt.initConnectView(this.mActivity, this.mFragment.getPreferenceScreen());
        this.mWifiManager = (WifiManager) this.mActivity.getSystemService("wifi");
        this.mExt.initPreference(cr);
        addWifiInfoPreference();
        this.mExt.initNetworkInfoView(this.mFragment.getPreferenceScreen());
    }

    public void onResume() {
        Log.d("AdvancedWifiSettingsExt", "onResume");
        initPreferences();
        refreshWifiInfo();
        this.mActivity.registerReceiver(this.mReceiver, this.mIntentFilter);
    }

    public void onPause() {
        this.mActivity.unregisterReceiver(this.mReceiver);
    }

    private void initPreferences() {
        this.mNotifyOpenNetworks = (SwitchPreference) this.mFragment.findPreference("notify_open_networks");
        ListPreference sleepPolicyPref = (ListPreference) this.mFragment.findPreference("sleep_policy");
        if (sleepPolicyPref != null) {
            this.mExt.setSleepPolicyPreference(sleepPolicyPref, this.mFragment.getResources().getStringArray(Utils.isWifiOnly(this.mActivity) ? R.array.wifi_sleep_policy_entries_wifi_only : R.array.wifi_sleep_policy_entries), this.mFragment.getResources().getStringArray(R.array.wifi_sleep_policy_values));
        }
    }

    private void refreshWifiInfo() {
        if (FeatureOption.MTK_DHCPV6C_WIFI) {
            String ipAddress = UtilsExt.getWifiIpAddresses();
            Log.d("AdvancedWifiSettingsExt", "refreshWifiInfo, the ipAddress is : " + ipAddress);
            if (ipAddress != null) {
                String[] ipAddresses = ipAddress.split(", ");
                int ipAddressesLength = ipAddresses.length;
                Log.d("AdvancedWifiSettingsExt", "ipAddressesLength is : " + ipAddressesLength);
                int i = 0;
                while (i < ipAddressesLength) {
                    if (ipAddresses[i].indexOf(":") == -1) {
                        Log.d("AdvancedWifiSettingsExt", "ipAddresses[i] is : " + ipAddresses[i]);
                        this.mIpAddressPref.setSummary(ipAddresses[i] == null ? this.mActivity.getString(R.string.status_unavailable) : ipAddresses[i]);
                        if (ipAddressesLength == 1) {
                            this.mFragment.getPreferenceScreen().removePreference(this.mIpv6AddressPref);
                        }
                    } else {
                        String ipSummary = "";
                        if (ipAddresses[i] == null) {
                            ipSummary = this.mActivity.getString(R.string.status_unavailable);
                        } else {
                            for (String str : ipAddresses[i].split("; ")) {
                                ipSummary = ipSummary + str + "\n";
                            }
                        }
                        this.mIpv6AddressPref.setSummary(ipSummary);
                        if (ipAddressesLength == 1) {
                            this.mFragment.getPreferenceScreen().removePreference(this.mIpAddressPref);
                        }
                    }
                    i++;
                }
            } else {
                this.mFragment.getPreferenceScreen().removePreference(this.mIpv6AddressPref);
                setDefaultIPAddress();
            }
        } else {
            setDefaultIPAddress();
        }
        this.mExt.refreshNetworkInfoView();
    }

    private void setDefaultIPAddress() {
        String ipAddress = Utils.getWifiIpAddresses(this.mActivity);
        Log.d("AdvancedWifiSettingsExt", "default ipAddress = " + ipAddress);
        if (this.mIpAddressPref != null) {
            Preference preference = this.mIpAddressPref;
            if (ipAddress == null) {
                ipAddress = this.mActivity.getString(R.string.status_unavailable);
            }
            preference.setSummary(ipAddress);
        }
    }

    private void addWifiInfoPreference() {
        this.mMacAddressPref = this.mFragment.findPreference("mac_address");
        this.mIpAddressPref = this.mFragment.findPreference("current_ip_address");
        if (this.mMacAddressPref != null && this.mIpAddressPref != null) {
            int order;
            PreferenceScreen screen = this.mFragment.getPreferenceScreen();
            int order2 = 0;
            for (int i = 0; i < screen.getPreferenceCount(); i++) {
                Preference preference = screen.getPreference(i);
                if (!("mac_address".equals(preference.getKey()) || "current_ip_address".equals(preference.getKey()))) {
                    order = order2 + 1;
                    preference.setOrder(order2);
                    order2 = order;
                }
            }
            order = order2 + 1;
            this.mMacAddressPref.setOrder(order2);
            if (this.mIpAddressPref != null) {
                order2 = order + 1;
                this.mIpAddressPref.setOrder(order);
            }
            if (FeatureOption.MTK_DHCPV6C_WIFI) {
                this.mIpAddressPref.setTitle(R.string.wifi_advanced_ipv4_address_title);
                this.mIpv6AddressPref = new Preference(this.mActivity, null, 16842893);
                this.mIpv6AddressPref.setTitle(R.string.wifi_advanced_ipv6_address_title);
                this.mIpv6AddressPref.setKey("current_ipv6_address");
                this.mFragment.getPreferenceScreen().addPreference(this.mIpv6AddressPref);
            }
        }
    }
}
