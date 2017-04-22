package com.prunellidae;

import android.content.Intent;
import android.os.IBinder;
import com.fai.sst.components.BaseService;
import com.fai.sst.p002d.C0671a;

/* compiled from: Unknown */
public class Offered extends BaseService {
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        m0a(C0671a.f36n);
    }

    public void onDestroy() {
        super.onDestroy();
        m0a(C0671a.f39q);
    }

    public int onStartCommand(Intent intent, int i, int i2) {
        m0a(C0671a.f37o);
        return super.onStartCommand(intent, i, i2);
    }
}
