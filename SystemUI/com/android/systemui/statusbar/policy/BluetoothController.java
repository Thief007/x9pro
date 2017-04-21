package com.android.systemui.statusbar.policy;

import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import java.util.Collection;

public interface BluetoothController {

    public interface Callback {
        void onBluetoothDevicesChanged();

        void onBluetoothStateChange(boolean z);
    }

    void addStateChangedCallback(Callback callback);

    void connect(CachedBluetoothDevice cachedBluetoothDevice);

    void disconnect(CachedBluetoothDevice cachedBluetoothDevice);

    Collection<CachedBluetoothDevice> getDevices();

    String getLastDeviceName();

    boolean isBluetoothConnected();

    boolean isBluetoothConnecting();

    boolean isBluetoothEnabled();

    boolean isBluetoothSupported();

    void removeStateChangedCallback(Callback callback);

    void setBluetoothEnabled(boolean z);
}
