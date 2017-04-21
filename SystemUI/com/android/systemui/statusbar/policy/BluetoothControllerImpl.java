package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDevice.Callback;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;

public class BluetoothControllerImpl implements BluetoothController, BluetoothCallback, Callback {
    private static final boolean DEBUG = Log.isLoggable("BluetoothController", 3);
    private final ArrayList<BluetoothController.Callback> mCallbacks = new ArrayList();
    private int mConnectionState = 0;
    private boolean mEnabled;
    private final H mHandler = new H();
    private CachedBluetoothDevice mLastDevice;
    private final LocalBluetoothManager mLocalBluetoothManager;

    private final class H extends Handler {
        private H() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    firePairedDevicesChanged();
                    return;
                case 2:
                    fireStateChange();
                    return;
                default:
                    return;
            }
        }

        private void firePairedDevicesChanged() {
            for (BluetoothController.Callback cb : BluetoothControllerImpl.this.mCallbacks) {
                cb.onBluetoothDevicesChanged();
            }
        }

        private void fireStateChange() {
            for (BluetoothController.Callback cb : BluetoothControllerImpl.this.mCallbacks) {
                fireStateChange(cb);
            }
        }

        private void fireStateChange(BluetoothController.Callback cb) {
            cb.onBluetoothStateChange(BluetoothControllerImpl.this.mEnabled);
        }
    }

    public BluetoothControllerImpl(Context context, Looper bgLooper) {
        this.mLocalBluetoothManager = LocalBluetoothManager.getInstance(context, null);
        if (this.mLocalBluetoothManager != null) {
            this.mLocalBluetoothManager.getEventManager().setReceiverHandler(new Handler(bgLooper));
            this.mLocalBluetoothManager.getEventManager().registerCallback(this);
            onBluetoothStateChanged(this.mLocalBluetoothManager.getBluetoothAdapter().getBluetoothState());
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("BluetoothController state:");
        pw.print("  mLocalBluetoothManager=");
        pw.println(this.mLocalBluetoothManager);
        if (this.mLocalBluetoothManager != null) {
            pw.print("  mEnabled=");
            pw.println(this.mEnabled);
            pw.print("  mConnectionState=");
            pw.println(stateToString(this.mConnectionState));
            pw.print("  mLastDevice=");
            pw.println(this.mLastDevice);
            pw.print("  mCallbacks.size=");
            pw.println(this.mCallbacks.size());
            pw.println("  Bluetooth Devices:");
            for (CachedBluetoothDevice device : this.mLocalBluetoothManager.getCachedDeviceManager().getCachedDevicesCopy()) {
                pw.println("    " + getDeviceString(device));
            }
        }
    }

    private static String stateToString(int state) {
        switch (state) {
            case 0:
                return "DISCONNECTED";
            case 1:
                return "CONNECTING";
            case 2:
                return "CONNECTED";
            case 3:
                return "DISCONNECTING";
            default:
                return "UNKNOWN(" + state + ")";
        }
    }

    private String getDeviceString(CachedBluetoothDevice device) {
        return device.getName() + " " + device.getBondState() + " " + device.isConnected();
    }

    public void addStateChangedCallback(BluetoothController.Callback cb) {
        this.mCallbacks.add(cb);
        this.mHandler.sendEmptyMessage(2);
    }

    public void removeStateChangedCallback(BluetoothController.Callback cb) {
        this.mCallbacks.remove(cb);
    }

    public boolean isBluetoothEnabled() {
        return this.mEnabled;
    }

    public boolean isBluetoothConnected() {
        return this.mConnectionState == 2;
    }

    public boolean isBluetoothConnecting() {
        return this.mConnectionState == 1;
    }

    public void setBluetoothEnabled(boolean enabled) {
        if (this.mLocalBluetoothManager != null) {
            this.mLocalBluetoothManager.getBluetoothAdapter().setBluetoothEnabled(enabled);
        }
    }

    public boolean isBluetoothSupported() {
        return this.mLocalBluetoothManager != null;
    }

    public void connect(CachedBluetoothDevice device) {
        if (this.mLocalBluetoothManager != null && device != null) {
            device.connect(true);
        }
    }

    public void disconnect(CachedBluetoothDevice device) {
        if (this.mLocalBluetoothManager != null && device != null) {
            device.disconnect();
        }
    }

    public String getLastDeviceName() {
        return this.mLastDevice != null ? this.mLastDevice.getName() : null;
    }

    public Collection<CachedBluetoothDevice> getDevices() {
        if (this.mLocalBluetoothManager != null) {
            return this.mLocalBluetoothManager.getCachedDeviceManager().getCachedDevicesCopy();
        }
        return null;
    }

    private void updateConnected() {
        int state = this.mLocalBluetoothManager.getBluetoothAdapter().getConnectionState();
        if (state != this.mConnectionState) {
            this.mConnectionState = state;
            this.mHandler.sendEmptyMessage(2);
        }
        if (this.mLastDevice == null || !this.mLastDevice.isConnected()) {
            this.mLastDevice = null;
            for (CachedBluetoothDevice device : getDevices()) {
                if (device.isConnected()) {
                    this.mLastDevice = device;
                }
            }
            if (this.mLastDevice == null && this.mConnectionState == 2) {
                this.mConnectionState = 0;
                this.mHandler.sendEmptyMessage(2);
            }
        }
    }

    public void onBluetoothStateChanged(int bluetoothState) {
        this.mEnabled = bluetoothState == 12;
        this.mHandler.sendEmptyMessage(2);
    }

    public void onScanningStateChanged(boolean started) {
    }

    public void onDeviceAdded(CachedBluetoothDevice cachedDevice) {
        cachedDevice.registerCallback(this);
        updateConnected();
        this.mHandler.sendEmptyMessage(1);
    }

    public void onDeviceDeleted(CachedBluetoothDevice cachedDevice) {
        updateConnected();
        this.mHandler.sendEmptyMessage(1);
    }

    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState) {
        updateConnected();
        this.mHandler.sendEmptyMessage(1);
    }

    public void onDeviceAttributesChanged() {
        updateConnected();
        this.mHandler.sendEmptyMessage(1);
    }

    public void onConnectionStateChanged(CachedBluetoothDevice cachedDevice, int state) {
        this.mLastDevice = cachedDevice;
        updateConnected();
        this.mConnectionState = state;
        this.mHandler.sendEmptyMessage(2);
    }
}
