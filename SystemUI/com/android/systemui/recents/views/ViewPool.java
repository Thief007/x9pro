package com.android.systemui.recents.views;

import android.content.Context;
import java.util.Iterator;
import java.util.LinkedList;

public class ViewPool<V, T> {
    Context mContext;
    LinkedList<V> mPool = new LinkedList();
    ViewPoolConsumer<V, T> mViewCreator;

    public interface ViewPoolConsumer<V, T> {
        V createView(Context context);

        boolean hasPreferredData(V v, T t);

        void prepareViewToEnterPool(V v);

        void prepareViewToLeavePool(V v, T t, boolean z);
    }

    public ViewPool(Context context, ViewPoolConsumer<V, T> viewCreator) {
        this.mContext = context;
        this.mViewCreator = viewCreator;
    }

    void returnViewToPool(V v) {
        this.mViewCreator.prepareViewToEnterPool(v);
        this.mPool.push(v);
    }

    V pickUpViewFromPool(T preferredData, T prepareData) {
        V v = null;
        boolean isNewView = false;
        if (this.mPool.isEmpty()) {
            v = this.mViewCreator.createView(this.mContext);
            isNewView = true;
        } else {
            Iterator<V> iter = this.mPool.iterator();
            while (iter.hasNext()) {
                V vpv = iter.next();
                if (this.mViewCreator.hasPreferredData(vpv, preferredData)) {
                    v = vpv;
                    iter.remove();
                    break;
                }
            }
            if (v == null) {
                v = this.mPool.pop();
            }
        }
        this.mViewCreator.prepareViewToLeavePool(v, prepareData, isNewView);
        return v;
    }

    Iterator<V> poolViewIterator() {
        if (this.mPool != null) {
            return this.mPool.iterator();
        }
        return null;
    }
}
