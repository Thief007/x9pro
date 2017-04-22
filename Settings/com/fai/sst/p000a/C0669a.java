package com.fai.sst.p000a;

import com.fai.sst.p002d.C0678j;

/* compiled from: Unknown */
public class C0669a {
    final String f19a;
    final Class[] f20b;
    final Object[] f21c;

    public C0669a(String str, Class[] clsArr, Object[] objArr) {
        this.f19a = str;
        this.f20b = clsArr;
        this.f21c = objArr;
    }

    public final void m10a(Object obj) {
        if (obj != null) {
            try {
                Class cls = obj.getClass();
                if (C0678j.m41a(this.f20b)) {
                    cls.getMethod(this.f19a, new Class[0]).invoke(obj, new Object[0]);
                } else {
                    cls.getMethod(this.f19a, this.f20b).invoke(obj, this.f21c);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
