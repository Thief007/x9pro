package com.android.systemui.recents.model;

import android.util.LruCache;
import com.android.systemui.recents.model.Task.TaskKey;
import java.util.HashMap;

public class KeyStoreLruCache<V> {
    LruCache<Integer, V> mCache;
    HashMap<Integer, TaskKey> mTaskKeys = new HashMap();

    public KeyStoreLruCache(int cacheSize) {
        this.mCache = new LruCache<Integer, V>(cacheSize) {
            protected void entryRemoved(boolean evicted, Integer taskId, V v, V v2) {
                KeyStoreLruCache.this.mTaskKeys.remove(taskId);
            }
        };
    }

    final V get(TaskKey key) {
        return this.mCache.get(Integer.valueOf(key.id));
    }

    final V getAndInvalidateIfModified(TaskKey key) {
        TaskKey lastKey = (TaskKey) this.mTaskKeys.get(Integer.valueOf(key.id));
        if (lastKey == null || lastKey.lastActiveTime >= key.lastActiveTime) {
            return this.mCache.get(Integer.valueOf(key.id));
        }
        remove(key);
        return null;
    }

    final void put(TaskKey key, V value) {
        this.mCache.put(Integer.valueOf(key.id), value);
        this.mTaskKeys.put(Integer.valueOf(key.id), key);
    }

    final void remove(TaskKey key) {
        this.mCache.remove(Integer.valueOf(key.id));
        this.mTaskKeys.remove(Integer.valueOf(key.id));
    }

    final void evictAll() {
        this.mCache.evictAll();
        this.mTaskKeys.clear();
    }

    final void trimToSize(int cacheSize) {
        this.mCache.resize(cacheSize);
    }
}
