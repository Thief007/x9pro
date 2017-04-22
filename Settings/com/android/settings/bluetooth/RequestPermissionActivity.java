package com.android.settings.bluetooth;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import com.android.settings.R;
import com.android.settingslib.bluetooth.BluetoothDiscoverableTimeoutReceiver;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

public class RequestPermissionActivity extends Activity implements OnClickListener {
    private static int mRequestCode = 1;
    private AlertDialog mDialog;
    private boolean mEnableOnly;
    private LocalBluetoothAdapter mLocalAdapter;
    private boolean mNeededToEnableBluetooth;
    private final BroadcastReceiver mReceiver = new C03151();
    private int mTimeout = 120;
    private boolean mUserConfirmed;

    class C03151 extends BroadcastReceiver {
        C03151() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null && RequestPermissionActivity.this.mNeededToEnableBluetooth && "android.bluetooth.adapter.action.STATE_CHANGED".equals(intent.getAction()) && intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE) == 12 && RequestPermissionActivity.this.mUserConfirmed) {
                RequestPermissionActivity.this.proceedAndFinish();
            }
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (parseIntent()) {
            finish();
            return;
        }
        int btState = this.mLocalAdapter.getState();
        switch (btState) {
            case 10:
            case 11:
            case 13:
                registerReceiver(this.mReceiver, new IntentFilter("android.bluetooth.adapter.action.STATE_CHANGED"));
                Intent intent = new Intent();
                intent.setClass(this, RequestPermissionHelperActivity.class);
                intent.setFlags(67108864);
                if (this.mEnableOnly) {
                    intent.setAction("com.android.settings.bluetooth.ACTION_INTERNAL_REQUEST_BT_ON");
                } else {
                    intent.setAction("com.android.settings.bluetooth.ACTION_INTERNAL_REQUEST_BT_ON_AND_DISCOVERABLE");
                    intent.putExtra("android.bluetooth.adapter.extra.DISCOVERABLE_DURATION", this.mTimeout);
                }
                startActivityForResult(intent, mRequestCode);
                mRequestCode++;
                this.mNeededToEnableBluetooth = true;
                break;
            case 12:
                if (!this.mEnableOnly) {
                    createDialog();
                    break;
                } else {
                    proceedAndFinish();
                    break;
                }
            default:
                Log.e("RequestPermissionActivity", "Unknown adapter state: " + btState);
                break;
        }
    }

    private void createDialog() {
        Builder builder = new Builder(this);
        if (this.mNeededToEnableBluetooth) {
            builder.setMessage(getString(R.string.bluetooth_turning_on));
            builder.setCancelable(false);
        } else {
            if (this.mTimeout == 0) {
                builder.setMessage(getString(R.string.bluetooth_ask_lasting_discovery));
            } else {
                builder.setMessage(getString(R.string.bluetooth_ask_discovery, new Object[]{Integer.valueOf(this.mTimeout)}));
            }
            builder.setPositiveButton(getString(R.string.allow), this);
            builder.setNegativeButton(getString(R.string.deny), this);
        }
        this.mDialog = builder.create();
        this.mDialog.show();
        if (getResources().getBoolean(R.bool.auto_confirm_bluetooth_activation_dialog)) {
            onClick(null, -1);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != mRequestCode - 1) {
            Log.e("RequestPermissionActivity", "Unexpected onActivityResult " + requestCode + ' ' + resultCode);
            setResult(0);
            finish();
        } else if (resultCode != -1000) {
            setResult(resultCode);
            finish();
        } else {
            this.mUserConfirmed = true;
            if (this.mLocalAdapter.getBluetoothState() == 12) {
                proceedAndFinish();
            } else {
                createDialog();
            }
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case -2:
                setResult(0);
                finish();
                return;
            case -1:
                proceedAndFinish();
                return;
            default:
                return;
        }
    }

    private void proceedAndFinish() {
        int returnCode;
        if (this.mEnableOnly) {
            returnCode = -1;
        } else if (this.mLocalAdapter.setScanMode(23, this.mTimeout)) {
            long endTime = System.currentTimeMillis() + (((long) this.mTimeout) * 1000);
            LocalBluetoothPreferences.persistDiscoverableEndTimestamp(this, endTime);
            if (this.mTimeout > 0) {
                BluetoothDiscoverableTimeoutReceiver.setDiscoverableAlarm(this, endTime);
            }
            returnCode = this.mTimeout;
            if (returnCode < 1) {
                returnCode = 1;
            }
        } else {
            returnCode = 0;
        }
        if (this.mDialog != null) {
            this.mDialog.dismiss();
        }
        setResult(returnCode);
        finish();
    }

    private boolean parseIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.getAction().equals("android.bluetooth.adapter.action.REQUEST_ENABLE")) {
            this.mEnableOnly = true;
        } else if (intent == null || !intent.getAction().equals("android.bluetooth.adapter.action.REQUEST_DISCOVERABLE")) {
            Log.e("RequestPermissionActivity", "Error: this activity may be started only with intent android.bluetooth.adapter.action.REQUEST_ENABLE or android.bluetooth.adapter.action.REQUEST_DISCOVERABLE");
            setResult(0);
            return true;
        } else {
            this.mTimeout = intent.getIntExtra("android.bluetooth.adapter.extra.DISCOVERABLE_DURATION", 120);
            Log.d("RequestPermissionActivity", "Setting Bluetooth Discoverable Timeout = " + this.mTimeout);
            if (this.mTimeout < 0 || this.mTimeout > 3600) {
                this.mTimeout = 120;
            }
        }
        LocalBluetoothManager manager = Utils.getLocalBtManager(this);
        if (manager == null) {
            Log.e("RequestPermissionActivity", "Error: there's a problem starting Bluetooth");
            setResult(0);
            return true;
        }
        this.mLocalAdapter = manager.getBluetoothAdapter();
        return false;
    }

    protected void onDestroy() {
        super.onDestroy();
        if (this.mNeededToEnableBluetooth) {
            unregisterReceiver(this.mReceiver);
        }
    }

    public void onBackPressed() {
        setResult(0);
        try {
            super.onBackPressed();
        } catch (IllegalStateException e) {
            Log.e("RequestPermissionActivity", e.getMessage());
        }
    }
}
