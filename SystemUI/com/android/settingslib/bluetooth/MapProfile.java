package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothMap;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import java.util.List;

public final class MapProfile implements LocalBluetoothProfile {
    static final ParcelUuid[] UUIDS = new ParcelUuid[]{BluetoothUuid.MAP, BluetoothUuid.MNS, BluetoothUuid.MAS};
    private static boolean V = true;
    private final CachedBluetoothDeviceManager mDeviceManager;
    private boolean mIsProfileReady;
    private final LocalBluetoothAdapter mLocalAdapter;
    private final LocalBluetoothProfileManager mProfileManager;
    private BluetoothMap mService;

    private final class MapServiceListener implements ServiceListener {
        private MapServiceListener() {
        }

        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (MapProfile.V) {
                Log.d("MapProfile", "Bluetooth service connected");
            }
            MapProfile.this.mService = (BluetoothMap) proxy;
            List<BluetoothDevice> deviceList = MapProfile.this.mService.getConnectedDevices();
            while (!deviceList.isEmpty()) {
                BluetoothDevice nextDevice = (BluetoothDevice) deviceList.remove(0);
                CachedBluetoothDevice device = MapProfile.this.mDeviceManager.findDevice(nextDevice);
                if (device == null) {
                    Log.w("MapProfile", "MapProfile found new device: " + nextDevice);
                    device = MapProfile.this.mDeviceManager.addDevice(MapProfile.this.mLocalAdapter, MapProfile.this.mProfileManager, nextDevice);
                }
                device.onProfileStateChanged(MapProfile.this, 2);
                device.refresh();
            }
            MapProfile.this.mProfileManager.callServiceConnectedListeners();
            MapProfile.this.mIsProfileReady = true;
        }

        public void onServiceDisconnected(int profile) {
            if (MapProfile.V) {
                Log.d("MapProfile", "Bluetooth service disconnected");
            }
            MapProfile.this.mProfileManager.callServiceDisconnectedListeners();
            MapProfile.this.mIsProfileReady = false;
        }
    }

    MapProfile(Context context, LocalBluetoothAdapter adapter, CachedBluetoothDeviceManager deviceManager, LocalBluetoothProfileManager profileManager) {
        this.mLocalAdapter = adapter;
        this.mDeviceManager = deviceManager;
        this.mProfileManager = profileManager;
        this.mLocalAdapter.getProfileProxy(context, new MapServiceListener(), 9);
    }

    public boolean isConnectable() {
        return true;
    }

    public boolean isAutoConnectable() {
        return true;
    }

    public boolean connect(BluetoothDevice device) {
        if (V) {
            Log.d("MapProfile", "connect() - should not get called");
        }
        return false;
    }

    public boolean disconnect(BluetoothDevice device) {
        if (this.mService == null) {
            return false;
        }
        List<BluetoothDevice> deviceList = this.mService.getConnectedDevices();
        if (deviceList.isEmpty() || !((BluetoothDevice) deviceList.get(0)).equals(device)) {
            return false;
        }
        if (this.mService.getPriority(device) > 100) {
            this.mService.setPriority(device, 100);
        }
        return this.mService.disconnect(device);
    }

    public int getConnectionStatus(BluetoothDevice device) {
        if (this.mService == null) {
            return 0;
        }
        int i;
        List<BluetoothDevice> deviceList = this.mService.getConnectedDevices();
        if (V) {
            Log.d("MapProfile", "getConnectionStatus: status is: " + this.mService.getConnectionState(device));
        }
        if (deviceList.isEmpty() || !((BluetoothDevice) deviceList.get(0)).equals(device)) {
            i = 0;
        } else {
            i = this.mService.getConnectionState(device);
        }
        return i;
    }

    public boolean isPreferred(BluetoothDevice device) {
        boolean z = false;
        if (this.mService == null) {
            return false;
        }
        if (this.mService.getPriority(device) > 0) {
            z = true;
        }
        return z;
    }

    public void setPreferred(BluetoothDevice device, boolean preferred) {
        if (this.mService != null) {
            if (!preferred) {
                this.mService.setPriority(device, 0);
            } else if (this.mService.getPriority(device) < 100) {
                this.mService.setPriority(device, 100);
            }
        }
    }

    public String toString() {
        return "MAP";
    }

    protected void finalize() {
        if (V) {
            Log.d("MapProfile", "finalize()");
        }
        if (this.mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(9, this.mService);
                this.mService = null;
            } catch (Throwable t) {
                Log.w("MapProfile", "Error cleaning up MAP proxy", t);
            }
        }
    }
}
