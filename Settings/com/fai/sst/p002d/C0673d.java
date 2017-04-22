package com.fai.sst.p002d;

import android.content.Context;
import android.os.Handler;
import com.fai.sst.p000a.C0669a;
import com.fai.sst.p001c.C0670f;

/* compiled from: Unknown */
public class C0673d {
    private static final Class f47a = null;
    private static boolean f48d = false;
    private static Context f49e;
    private static Object f50f;
    private static Object f51g;
    private final Handler f52h = new C0670f(this);
    private final C0669a f53i;

    public C0673d(Context context, C0669a c0669a) {
        C0673d.m23a(context);
        this.f53i = c0669a;
    }

    public static void m23a(Context context) {
        if (f49e == null) {
            f49e = context;
        }
    }

    private Thread m30h() {
        return new C0674e(this);
    }

    public final void m31a() {
        if (this.f53i != null) {
            if (f51g != null) {
                m32c();
            } else if (f48d) {
                this.f52h.sendEmptyMessageDelayed(0, 4000);
            } else {
                try {
                    f48d = true;
                    m30h().start();
                } catch (Exception e) {
                    f48d = false;
                    e.printStackTrace();
                }
            }
        }
    }

    public void m32c() {
        if (this.f53i != null) {
            this.f53i.m10a(f51g);
        }
    }
}
