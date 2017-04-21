package com.android.systemui.assis.app;

import android.os.Environment;
import android.os.StatFs;

public class SystemEnvironment {
    private static final String TAG = "SystemEnvironment";

    public static long getSDFreeSize() {
        if (Environment.getExternalStorageState() == null) {
            return 0;
        }
        StatFs sf = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long blockSize = (long) sf.getBlockSize();
        long freeBlocks = (long) sf.getAvailableBlocks();
        LOG.I(TAG, "SD空间可用大小:" + (freeBlocks * blockSize) + "byte");
        return freeBlocks * blockSize;
    }

    public static long getSystemFreeSize() {
        StatFs sf = new StatFs(Environment.getRootDirectory().getPath());
        long blockSize = (long) sf.getBlockSize();
        long availCount = (long) sf.getAvailableBlocks();
        LOG.I(TAG, "系统空间可用大小:" + (availCount * blockSize) + "byte");
        return availCount * blockSize;
    }
}
