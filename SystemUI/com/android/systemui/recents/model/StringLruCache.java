package com.android.systemui.recents.model;

class StringLruCache extends KeyStoreLruCache<String> {
    public StringLruCache(int cacheSize) {
        super(cacheSize);
    }
}
