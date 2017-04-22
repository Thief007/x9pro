package com.mediatek.settings.ext;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiConfiguration;
import android.preference.ListPreference;
import android.preference.PreferenceScreen;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.mediatek.settings.ext.IWifiExt.Builder;

public class DefaultWifiExt implements IWifiExt {
    private static final String TAG = "DefaultWifiExt";
    private Context mContext;

    public DefaultWifiExt(Context context) {
        this.mContext = context;
    }

    public void setAPNetworkId(WifiConfiguration wifiConfig) {
    }

    public void setAPPriority(int apPriority) {
    }

    public void setPriorityView(LinearLayout priorityLayout, WifiConfiguration wifiConfig, boolean isEdit) {
    }

    public void setSecurityText(TextView view) {
    }

    public void addDisconnectButton(AlertDialog dialog, boolean edit, DetailedState state, WifiConfiguration wifiConfig) {
    }

    public int getPriority(int priority) {
        return priority;
    }

    public void setProxyText(TextView view) {
    }

    public void initConnectView(Activity activity, PreferenceScreen screen) {
    }

    public void initNetworkInfoView(PreferenceScreen screen) {
    }

    public void refreshNetworkInfoView() {
    }

    public void initPreference(ContentResolver contentResolver) {
    }

    public void setSleepPolicyPreference(ListPreference sleepPolicyPref, String[] sleepPolicyEntries, String[] sleepPolicyValues) {
    }

    public void hideWifiConfigInfo(Builder builder, Context context) {
    }

    public void setEapMethodArray(ArrayAdapter adapter, String ssid, int security) {
    }

    public int getEapMethodbySpinnerPos(int spinnerPos, String ssid, int security) {
        return spinnerPos;
    }

    public int getPosByEapMethod(int spinnerPos, String ssid, int security) {
        return spinnerPos;
    }
}
