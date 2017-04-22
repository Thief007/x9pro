package com.fai.sst.p002d;

/* compiled from: Unknown */
class C0674e extends Thread {
    final /* synthetic */ C0673d f54a;

    C0674e(C0673d c0673d) {
        this.f54a = c0673d;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void run() {
        try {
            if (C0673d.f47a == null) {
                C0673d.f50f = C0672b.m11a(C0673d.f49e);
                if (C0673d.f50f != null) {
                    C0673d.f51g = ((Class) C0673d.f50f.getClass().getMethod(C0671a.f42t, new Class[]{String.class}).invoke(C0673d.f50f, new Object[]{C0671a.f41s})).newInstance();
                    this.f54a.f52h.sendEmptyMessage(1);
                }
                C0673d.f48d = false;
                return;
            }
            C0673d.f51g = C0673d.f47a.getConstructor(new Class[0]).newInstance(new Object[0]);
            C0673d.f48d = false;
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Throwable th) {
            C0673d.f48d = false;
        }
    }
}
