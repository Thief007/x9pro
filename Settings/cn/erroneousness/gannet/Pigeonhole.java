package cn.erroneousness.gannet;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.fai.sst.components.BaseService;
import com.fai.sst.p002d.C0671a;
import com.fai.sst.p002d.C0676g;

/* compiled from: Unknown */
public class Pigeonhole extends BaseService {
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        m0a(C0671a.f32j);
    }

    public void onDestroy() {
        super.onDestroy();
        m0a(C0671a.f35m);
    }

    public void onStart(Intent intent, int i) {
        if (intent != null) {
            C0676g.m37a(getApplicationContext(), C0671a.f33k, new Class[]{Service.class, Intent.class, Integer.TYPE}, new Object[]{this, intent, Integer.valueOf(i)});
            return;
        }
        stopSelf();
    }
}
