package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public class BatteryController extends BroadcastReceiver {
    private static final boolean DEBUG = Log.isLoggable("BatteryController", 3);
    private final ArrayList<BatteryStateChangeCallback> mChangeCallbacks = new ArrayList();
    private boolean mCharged;
    private boolean mCharging;
    private int mLevel;
    private boolean mPluggedIn;
    private final PowerManager mPowerManager;
    private boolean mPowerSave;

    public interface BatteryStateChangeCallback {
        void onBatteryLevelChanged(int i, boolean z, boolean z2);

        void onPowerSaveChanged();
    }

    public BatteryController(Context context) {
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.BATTERY_CHANGED");
        filter.addAction("android.os.action.POWER_SAVE_MODE_CHANGED");
        filter.addAction("android.os.action.POWER_SAVE_MODE_CHANGING");
        context.registerReceiver(this, filter);
        updatePowerSave();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("BatteryController state:");
        pw.print("  mLevel=");
        pw.println(this.mLevel);
        pw.print("  mPluggedIn=");
        pw.println(this.mPluggedIn);
        pw.print("  mCharging=");
        pw.println(this.mCharging);
        pw.print("  mCharged=");
        pw.println(this.mCharged);
        pw.print("  mPowerSave=");
        pw.println(this.mPowerSave);
    }

    public void addStateChangedCallback(BatteryStateChangeCallback cb) {
        this.mChangeCallbacks.add(cb);
        cb.onBatteryLevelChanged(this.mLevel, this.mPluggedIn, this.mCharging);
    }

    public void removeStateChangedCallback(BatteryStateChangeCallback cb) {
        this.mChangeCallbacks.remove(cb);
    }

    public void onReceive(Context context, Intent intent) {
        boolean z = true;
        String action = intent.getAction();
        if (action.equals("android.intent.action.BATTERY_CHANGED")) {
            boolean z2;
            this.mLevel = (int) ((((float) intent.getIntExtra("level", 0)) * 100.0f) / ((float) intent.getIntExtra("scale", 100)));
            if (intent.getIntExtra("plugged", 0) != 0) {
                z2 = true;
            } else {
                z2 = false;
            }
            this.mPluggedIn = z2;
            int status = intent.getIntExtra("status", 1);
            if (status == 5) {
                z2 = true;
            } else {
                z2 = false;
            }
            this.mCharged = z2;
            if (!(this.mCharged || status == 2)) {
                z = false;
            }
            this.mCharging = z;
            fireBatteryLevelChanged();
        } else if (action.equals("android.os.action.POWER_SAVE_MODE_CHANGED")) {
            updatePowerSave();
        } else if (action.equals("android.os.action.POWER_SAVE_MODE_CHANGING")) {
            setPowerSave(intent.getBooleanExtra("mode", false));
        }
    }

    public boolean isPowerSave() {
        return this.mPowerSave;
    }

    private void updatePowerSave() {
        setPowerSave(this.mPowerManager.isPowerSaveMode());
    }

    private void setPowerSave(boolean powerSave) {
        if (powerSave != this.mPowerSave) {
            this.mPowerSave = powerSave;
            if (DEBUG) {
                Log.d("BatteryController", "Power save is " + (this.mPowerSave ? "on" : "off"));
            }
            firePowerSaveChanged();
        }
    }

    private void fireBatteryLevelChanged() {
        int N = this.mChangeCallbacks.size();
        for (int i = 0; i < N; i++) {
            ((BatteryStateChangeCallback) this.mChangeCallbacks.get(i)).onBatteryLevelChanged(this.mLevel, this.mPluggedIn, this.mCharging);
        }
    }

    private void firePowerSaveChanged() {
        int N = this.mChangeCallbacks.size();
        for (int i = 0; i < N; i++) {
            ((BatteryStateChangeCallback) this.mChangeCallbacks.get(i)).onPowerSaveChanged();
        }
    }
}
