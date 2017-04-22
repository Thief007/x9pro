package com.android.settings;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import com.android.settingslib.TetherUtil;
import java.io.Serializable;
import java.util.ArrayList;

public class TetherService extends Service {
    private static final boolean DEBUG = Log.isLoggable("TetherService", 3);
    private ArrayList<Integer> mCurrentTethers;
    private int mCurrentTypeIndex;
    private boolean mEnableWifiAfterCheck;
    private boolean mInProvisionCheck;
    private final BroadcastReceiver mReceiver = new C01921();

    class C01921 extends BroadcastReceiver {
        C01921() {
        }

        public void onReceive(Context context, Intent intent) {
            if (TetherService.DEBUG) {
                Log.d("TetherService", "Got provision result " + intent);
            }
            if (context.getResources().getString(17039387).equals(intent.getAction())) {
                TetherService.this.mInProvisionCheck = false;
                int checkType = ((Integer) TetherService.this.mCurrentTethers.get(TetherService.this.mCurrentTypeIndex)).intValue();
                if (intent.getIntExtra("EntitlementResult", 0) != -1) {
                    switch (checkType) {
                        case 0:
                            TetherService.this.disableWifiTethering();
                            break;
                        case 1:
                            TetherService.this.disableUsbTethering();
                            break;
                        case 2:
                            TetherService.this.disableBtTethering();
                            break;
                        default:
                            break;
                    }
                } else if (checkType == 0 && TetherService.this.mEnableWifiAfterCheck) {
                    TetherService.this.enableWifiTetheringIfNeeded();
                    TetherService.this.mEnableWifiAfterCheck = false;
                }
                TetherService tetherService = TetherService.this;
                if (tetherService.mCurrentTypeIndex = tetherService.mCurrentTypeIndex + 1 == TetherService.this.mCurrentTethers.size()) {
                    TetherService.this.stopSelf();
                } else {
                    TetherService.this.startProvisioning(TetherService.this.mCurrentTypeIndex);
                }
            }
        }
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        if (DEBUG) {
            Log.d("TetherService", "Creating WifiProvisionService");
        }
        registerReceiver(this.mReceiver, new IntentFilter(getResources().getString(17039387)), "android.permission.CONNECTIVITY_INTERNAL", null);
        this.mCurrentTethers = stringToTethers(getSharedPreferences("tetherPrefs", 0).getString("currentTethers", ""));
        this.mCurrentTypeIndex = 0;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        int type;
        if (intent.hasExtra("extraAddTetherType")) {
            type = intent.getIntExtra("extraAddTetherType", -1);
            if (!this.mCurrentTethers.contains(Integer.valueOf(type))) {
                if (DEBUG) {
                    Log.d("TetherService", "Adding tether " + type);
                }
                this.mCurrentTethers.add(Integer.valueOf(type));
            }
        }
        if (intent.hasExtra("extraRemTetherType")) {
            type = intent.getIntExtra("extraRemTetherType", -1);
            if (DEBUG) {
                Log.d("TetherService", "Removing tether " + type);
            }
            int index = this.mCurrentTethers.indexOf(Integer.valueOf(type));
            if (index >= 0) {
                this.mCurrentTethers.remove(index);
                if (index <= this.mCurrentTypeIndex && this.mCurrentTypeIndex > 0) {
                    this.mCurrentTypeIndex--;
                }
            }
            cancelAlarmIfNecessary();
        }
        if (intent.getBooleanExtra("extraSetAlarm", false) && this.mCurrentTethers.size() == 1) {
            scheduleAlarm();
        }
        if (intent.getBooleanExtra("extraEnableWifiTether", false)) {
            this.mEnableWifiAfterCheck = true;
        }
        if (intent.getBooleanExtra("extraRunProvision", false)) {
            startProvisioning(this.mCurrentTypeIndex);
        } else if (!this.mInProvisionCheck) {
            stopSelf();
            return 2;
        }
        return 1;
    }

    public void onDestroy() {
        if (this.mInProvisionCheck) {
            Log.e("TetherService", "TetherService getting destroyed while mid-provisioning" + this.mCurrentTethers.get(this.mCurrentTypeIndex));
        }
        getSharedPreferences("tetherPrefs", 0).edit().putString("currentTethers", tethersToString(this.mCurrentTethers)).commit();
        if (DEBUG) {
            Log.d("TetherService", "Destroying WifiProvisionService");
        }
        unregisterReceiver(this.mReceiver);
        super.onDestroy();
    }

    private ArrayList<Integer> stringToTethers(String tethersStr) {
        ArrayList<Integer> ret = new ArrayList();
        if (TextUtils.isEmpty(tethersStr)) {
            return ret;
        }
        String[] tethersSplit = tethersStr.split(",");
        for (String parseInt : tethersSplit) {
            ret.add(Integer.valueOf(Integer.parseInt(parseInt)));
        }
        return ret;
    }

    private String tethersToString(ArrayList<Integer> tethers) {
        StringBuffer buffer = new StringBuffer();
        int N = tethers.size();
        for (int i = 0; i < N; i++) {
            if (i != 0) {
                buffer.append(',');
            }
            buffer.append(tethers.get(i));
        }
        return buffer.toString();
    }

    private void enableWifiTetheringIfNeeded() {
        if (!TetherUtil.isWifiTetherEnabled(this)) {
            TetherUtil.setWifiTethering(true, this);
        }
    }

    private void disableWifiTethering() {
        TetherUtil.setWifiTethering(false, this);
    }

    private void disableUsbTethering() {
        ((ConnectivityManager) getSystemService("connectivity")).setUsbTethering(false);
    }

    private void disableBtTethering() {
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            adapter.getProfileProxy(this, new ServiceListener() {
                public void onServiceDisconnected(int profile) {
                }

                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    ((BluetoothPan) proxy).setBluetoothTethering(false);
                    adapter.closeProfileProxy(5, proxy);
                }
            }, 5);
        }
    }

    private void startProvisioning(int index) {
        String provisionAction = getResources().getString(17039386);
        if (DEBUG) {
            Log.d("TetherService", "Sending provisioning broadcast: " + provisionAction + " type: " + this.mCurrentTethers.get(index));
        }
        Intent intent = new Intent(provisionAction);
        intent.putExtra("TETHER_TYPE", (Serializable) this.mCurrentTethers.get(index));
        intent.setFlags(268435456);
        sendBroadcast(intent);
        this.mInProvisionCheck = true;
    }

    public static void scheduleRecheckAlarm(Context context, int type) {
        Intent intent = new Intent(context, TetherService.class);
        intent.putExtra("extraAddTetherType", type);
        intent.putExtra("extraSetAlarm", true);
        context.startService(intent);
    }

    private void scheduleAlarm() {
        Intent intent = new Intent(this, TetherService.class);
        intent.putExtra("extraRunProvision", true);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService("alarm");
        long periodMs = (long) (3600000 * getResources().getInteger(17694735));
        long firstTime = SystemClock.elapsedRealtime() + periodMs;
        if (DEBUG) {
            Log.d("TetherService", "Scheduling alarm at interval " + periodMs);
        }
        alarmManager.setRepeating(3, firstTime, periodMs, pendingIntent);
    }

    public static void cancelRecheckAlarmIfNecessary(Context context, int type) {
        Intent intent = new Intent(context, TetherService.class);
        intent.putExtra("extraRemTetherType", type);
        context.startService(intent);
    }

    private void cancelAlarmIfNecessary() {
        if (this.mCurrentTethers.size() != 0) {
            if (DEBUG) {
                Log.d("TetherService", "Tethering still active, not cancelling alarm");
            }
            return;
        }
        ((AlarmManager) getSystemService("alarm")).cancel(PendingIntent.getService(this, 0, new Intent(this, TetherService.class), 0));
        if (DEBUG) {
            Log.d("TetherService", "Tethering no longer active, canceling recheck");
        }
    }
}
