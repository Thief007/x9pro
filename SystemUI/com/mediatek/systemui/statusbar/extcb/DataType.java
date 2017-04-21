package com.mediatek.systemui.statusbar.extcb;

import android.util.Log;
import android.util.SparseArray;

public enum DataType {
    Type_1X(0),
    Type_3G(1),
    Type_4G(2),
    Type_E(3),
    Type_G(4),
    Type_H(5),
    Type_H_PLUS(6),
    Type_3G_PLUS(7),
    Type_4G_PLUS(8);
    
    private static final String TAG = "DataType";
    private static final SparseArray<DataType> sDataTypeLookup = null;
    private static DataType sDefaultDataType;
    private int mTypeId;

    static {
        sDataTypeLookup = new SparseArray();
    }

    private DataType(int typeId) {
        this.mTypeId = typeId;
    }

    public int getTypeId() {
        return this.mTypeId;
    }

    public static final DataType get(int dataNetType) {
        DataType dataType = (DataType) sDataTypeLookup.get(dataNetType, sDefaultDataType);
        Log.d(TAG, "getDataType, dataNetType = " + dataNetType + " to DataType = " + dataType.name());
        return dataType;
    }

    public static final void mapDataTypeSets(boolean showAtLeast3G, boolean show4gForLte, boolean hspaDataDistinguishable) {
        sDataTypeLookup.clear();
        sDataTypeLookup.put(5, Type_3G);
        sDataTypeLookup.put(6, Type_3G);
        sDataTypeLookup.put(12, Type_3G);
        sDataTypeLookup.put(14, Type_3G);
        sDataTypeLookup.put(3, Type_3G);
        if (showAtLeast3G) {
            sDataTypeLookup.put(0, Type_3G);
            sDataTypeLookup.put(2, Type_3G);
            sDataTypeLookup.put(4, Type_3G);
            sDataTypeLookup.put(7, Type_3G);
            sDefaultDataType = Type_3G;
        } else {
            sDataTypeLookup.put(2, Type_E);
            sDataTypeLookup.put(4, Type_1X);
            sDataTypeLookup.put(7, Type_1X);
            sDefaultDataType = Type_G;
        }
        if (hspaDataDistinguishable) {
            sDataTypeLookup.put(8, Type_H);
            sDataTypeLookup.put(9, Type_H);
            sDataTypeLookup.put(10, Type_H);
            sDataTypeLookup.put(15, Type_H_PLUS);
        } else {
            sDataTypeLookup.put(8, Type_3G);
            sDataTypeLookup.put(9, Type_3G);
            sDataTypeLookup.put(10, Type_3G);
            sDataTypeLookup.put(15, Type_3G);
        }
        sDataTypeLookup.put(13, Type_4G);
        sDataTypeLookup.put(139, Type_4G_PLUS);
    }
}
