package com.android.settings.wifi;

import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.text.Editable;
import android.util.Log;
import android.widget.EditText;
import com.android.settings.R;

public class WifiAPITest extends PreferenceActivity implements OnPreferenceClickListener {
    private Preference mWifiDisableNetwork;
    private Preference mWifiDisconnect;
    private Preference mWifiEnableNetwork;
    private WifiManager mWifiManager;
    private int netid;

    class C06022 implements OnClickListener {
        C06022() {
        }

        public void onClick(DialogInterface dialog, int whichButton) {
        }
    }

    class C06044 implements OnClickListener {
        C06044() {
        }

        public void onClick(DialogInterface dialog, int whichButton) {
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onCreatePreferences();
        this.mWifiManager = (WifiManager) getSystemService("wifi");
    }

    private void onCreatePreferences() {
        addPreferencesFromResource(R.layout.wifi_api_test);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        this.mWifiDisconnect = preferenceScreen.findPreference("disconnect");
        this.mWifiDisconnect.setOnPreferenceClickListener(this);
        this.mWifiDisableNetwork = preferenceScreen.findPreference("disable_network");
        this.mWifiDisableNetwork.setOnPreferenceClickListener(this);
        this.mWifiEnableNetwork = preferenceScreen.findPreference("enable_network");
        this.mWifiEnableNetwork.setOnPreferenceClickListener(this);
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);
        return false;
    }

    public boolean onPreferenceClick(Preference pref) {
        if (pref == this.mWifiDisconnect) {
            this.mWifiManager.disconnect();
        } else if (pref == this.mWifiDisableNetwork) {
            alert = new Builder(this);
            alert.setTitle("Input");
            alert.setMessage("Enter Network ID");
            input = new EditText(this);
            input.setInputType(2);
            alert.setView(input);
            alert.setPositiveButton("Ok", new OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    WifiAPITest.this.netid = Integer.parseInt(input.getText().toString());
                    WifiAPITest.this.mWifiManager.disableNetwork(WifiAPITest.this.netid);
                }
            });
            alert.setNegativeButton("Cancel", new C06022());
            alert.show();
        } else if (pref == this.mWifiEnableNetwork) {
            alert = new Builder(this);
            alert.setTitle("Input");
            alert.setMessage("Enter Network ID");
            input = new EditText(this);
            input.setInputType(2);
            alert.setView(input);
            alert.setPositiveButton("Ok", new OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Editable value = input.getText();
                    if (value != null && value.toString().length() != 0 && value.toString().replaceAll("[a-zA-Z]", "").replaceAll("\r", "").replaceAll("\n", "").trim() != null && !value.toString().replaceAll("[a-zA-Z]", "").replaceAll("\r", "").replaceAll("\n", "").trim().equals("")) {
                        WifiAPITest.this.netid = Integer.parseInt(value.toString().trim());
                        Log.d("yjp", "value.toString() :" + value.toString().trim());
                        WifiAPITest.this.mWifiManager.enableNetwork(WifiAPITest.this.netid, false);
                    }
                }
            });
            alert.setNegativeButton("Cancel", new C06044());
            alert.show();
        }
        return true;
    }
}
