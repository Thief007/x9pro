package com.fai.sst.p001c;

import android.os.Handler;
import android.os.Message;
import com.fai.sst.p002d.C0673d;

/* compiled from: Unknown */
public class C0670f extends Handler {
    final C0673d f22a;

    public C0670f(C0673d c0673d) {
        this.f22a = c0673d;
    }

    public final void handleMessage(Message message) {
        switch (message.what) {
            case 0:
                this.f22a.m31a();
                return;
            case 1:
                this.f22a.m32c();
                return;
            default:
                return;
        }
    }
}
