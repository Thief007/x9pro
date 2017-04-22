package com.mediatek.wifi.hotspot;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;
import android.widget.Spinner;
import com.android.settings.R;

public class WifiApWpsDialog extends AlertDialog implements OnClickListener, OnItemSelectedListener {
    private Context mContext;
    private View mView;
    private WifiManager mWifiManager;
    private int mWpsMode = 0;
    private Spinner mWpsModeSpinner;

    public WifiApWpsDialog(Context context) {
        super(context);
        this.mContext = context;
    }

    protected void onCreate(Bundle savedInstanceState) {
        Log.d("@M_WifiApWpsDialog", "onCreate, return dialog");
        this.mView = getLayoutInflater().inflate(R.layout.wifi_ap_wps_dialog, null);
        setView(this.mView);
        setInverseBackgroundForced(true);
        setTitle(R.string.wifi_ap_wps_dialog_title);
        this.mWpsModeSpinner = (Spinner) this.mView.findViewById(R.id.wps_mode);
        this.mWpsModeSpinner.setOnItemSelectedListener(this);
        setButton(-1, this.mContext.getString(R.string.wifi_connect), this);
        setButton(-2, this.mContext.getString(R.string.wifi_cancel), this);
        this.mWifiManager = (WifiManager) getContext().getApplicationContext().getSystemService("wifi");
        super.onCreate(savedInstanceState);
    }

    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == -1) {
            WpsInfo config = new WpsInfo();
            if (this.mWpsMode == 0) {
                config.setup = 0;
                config.BSSID = "any";
            } else if (this.mWpsMode == 1) {
                config.setup = 1;
                config.pin = ((EditText) this.mView.findViewById(R.id.pin_edit)).getText().toString();
            }
            this.mWifiManager.startApWps(config);
        }
    }

    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.equals(this.mWpsModeSpinner)) {
            this.mWpsMode = position;
            if (this.mWpsMode == 0) {
                this.mView.findViewById(R.id.type_pin_field).setVisibility(8);
            } else if (this.mWpsMode == 1) {
                this.mView.findViewById(R.id.type_pin_field).setVisibility(0);
            }
        }
    }

    public void onNothingSelected(AdapterView<?> adapterView) {
    }
}
