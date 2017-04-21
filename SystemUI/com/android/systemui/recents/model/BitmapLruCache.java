package com.android.systemui.recents.model;

import android.graphics.Bitmap;

class BitmapLruCache extends KeyStoreLruCache<Bitmap> {
    public BitmapLruCache(int cacheSize) {
        super(cacheSize);
    }
}
