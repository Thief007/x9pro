package com.mediatek.settingslib.bluetooth;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.os.ParcelUuid;
import android.util.Log;
import com.android.settingslib.bluetooth.BluetoothDeviceFilter.Filter;
import com.mediatek.bluetooth.BluetoothUuidEx;
import java.util.HashMap;

public final class BluetoothDeviceFilterEx {
    static final ParcelUuid[] BIP_PROFILE_UUIDS = new ParcelUuid[]{BluetoothUuidEx.BipResponder};
    static final ParcelUuid[] BPP_PROFILE_UUIDS = new ParcelUuid[]{BluetoothUuidEx.BppReceiver};
    static final ParcelUuid[] PRX_PROFILE_UUIDS = new ParcelUuid[]{BluetoothUuidEx.Proximity};
    private static final HashMap<Integer, Filter> mFilterMap = new C07471();

    static class C07471 extends HashMap<Integer, Filter> {
        C07471() {
            put(Integer.valueOf(5), new BPPFilter());
            put(Integer.valueOf(6), new BIPFilter());
            put(Integer.valueOf(8), new PrxmFilter());
        }
    }

    private static abstract class ClassUuidFilter implements Filter {
        abstract boolean matches(ParcelUuid[] parcelUuidArr, BluetoothClass bluetoothClass);

        private ClassUuidFilter() {
        }

        public boolean matches(BluetoothDevice device) {
            return matches(device.getUuids(), device.getBluetoothClass());
        }
    }

    private static final class BIPFilter extends ClassUuidFilter {
        private BIPFilter() {
            super();
        }

        boolean matches(ParcelUuid[] uuids, BluetoothClass btClass) {
            Log.d("BluetoothDeviceFilterEx", "Enter BIPFilter to matches");
            if (uuids != null && BluetoothUuid.containsAnyUuid(uuids, BluetoothDeviceFilterEx.BIP_PROFILE_UUIDS)) {
                return true;
            }
            boolean doesClassMatch;
            if (btClass != null) {
                doesClassMatch = btClass.doesClassMatch(6);
            } else {
                doesClassMatch = false;
            }
            return doesClassMatch;
        }
    }

    private static final class BPPFilter extends ClassUuidFilter {
        private BPPFilter() {
            super();
        }

        boolean matches(ParcelUuid[] uuids, BluetoothClass btClass) {
            Log.d("BluetoothDeviceFilterEx", "Enter BPPFilter to matches");
            if (uuids != null && BluetoothUuid.containsAnyUuid(uuids, BluetoothDeviceFilterEx.BPP_PROFILE_UUIDS)) {
                return true;
            }
            boolean doesClassMatch;
            if (btClass != null) {
                doesClassMatch = btClass.doesClassMatch(6);
            } else {
                doesClassMatch = false;
            }
            return doesClassMatch;
        }
    }

    private static final class PrxmFilter extends ClassUuidFilter {
        private PrxmFilter() {
            super();
        }

        boolean matches(ParcelUuid[] uuids, BluetoothClass btClass) {
            Log.d("BluetoothDeviceFilterEx", "Enter PrxmFilter to matches");
            if (uuids == null || !BluetoothUuid.containsAnyUuid(uuids, BluetoothDeviceFilterEx.PRX_PROFILE_UUIDS)) {
                return false;
            }
            return true;
        }
    }

    public static Filter getFilterEx(int filterType) {
        return (Filter) mFilterMap.get(Integer.valueOf(filterType));
    }
}
