package com.mediatek.systemui.statusbar.extcb;

import android.util.Log;
import android.util.SparseArray;

public enum NetworkType {
    Type_G(0),
    Type_3G(1),
    Type_1X(2),
    Type_1X3G(3),
    Type_4G(4),
    Type_E(5);
    
    private static final String TAG = "NetworkType";
    private static NetworkType sDefaultNetworkType;
    private static final SparseArray<NetworkType> sNetworkTypeLookup = null;
    private int mTypeId;

    static {
        sNetworkTypeLookup = new SparseArray();
    }

    private NetworkType(int typeId) {
        this.mTypeId = typeId;
    }

    public int getTypeId() {
        return this.mTypeId;
    }

    public static final NetworkType get(int dataNetType) {
        NetworkType networkType = (NetworkType) sNetworkTypeLookup.get(dataNetType, sDefaultNetworkType);
        Log.d(TAG, "getNetworkType, dataNetType = " + dataNetType + " to NetworkType = " + networkType.name());
        return networkType;
    }

    public static final void mapNetworkTypeSets(boolean showAtLeast3G, boolean show4gForLte, boolean hspaDataDistinguishable) {
        sNetworkTypeLookup.clear();
        sNetworkTypeLookup.put(5, Type_1X3G);
        sNetworkTypeLookup.put(6, Type_1X3G);
        sNetworkTypeLookup.put(12, Type_1X3G);
        sNetworkTypeLookup.put(14, Type_1X3G);
        sNetworkTypeLookup.put(3, Type_3G);
        if (showAtLeast3G) {
            sNetworkTypeLookup.put(0, Type_3G);
            sNetworkTypeLookup.put(2, Type_3G);
            sNetworkTypeLookup.put(4, Type_3G);
            sNetworkTypeLookup.put(7, Type_3G);
            sDefaultNetworkType = Type_3G;
        } else {
            sNetworkTypeLookup.put(0, Type_G);
            sNetworkTypeLookup.put(2, Type_E);
            sNetworkTypeLookup.put(4, Type_1X);
            sNetworkTypeLookup.put(7, Type_1X);
            sDefaultNetworkType = Type_G;
        }
        sNetworkTypeLookup.put(8, Type_3G);
        sNetworkTypeLookup.put(9, Type_3G);
        sNetworkTypeLookup.put(10, Type_3G);
        sNetworkTypeLookup.put(15, Type_3G);
        sNetworkTypeLookup.put(13, Type_4G);
    }
}
