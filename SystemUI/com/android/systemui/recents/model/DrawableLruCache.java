package com.android.systemui.recents.model;

import android.graphics.drawable.Drawable;

class DrawableLruCache extends KeyStoreLruCache<Drawable> {
    public DrawableLruCache(int cacheSize) {
        super(cacheSize);
    }
}
