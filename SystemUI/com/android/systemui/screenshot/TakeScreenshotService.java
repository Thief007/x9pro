package com.android.systemui.screenshot;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

public class TakeScreenshotService extends Service {
    private static GlobalScreenshot mScreenshot;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            boolean z = true;
            switch (msg.what) {
                case 1:
                    final Messenger callback = msg.replyTo;
                    if (TakeScreenshotService.mScreenshot == null) {
                        TakeScreenshotService.mScreenshot = new GlobalScreenshot(TakeScreenshotService.this);
                    }
                    GlobalScreenshot -get0 = TakeScreenshotService.mScreenshot;
                    Runnable anonymousClass1 = new Runnable() {
                        public void run() {
                            try {
                                callback.send(Message.obtain(null, 1));
                            } catch (RemoteException e) {
                            }
                        }
                    };
                    boolean z2 = msg.arg1 > 0;
                    if (msg.arg2 <= 0) {
                        z = false;
                    }
                    -get0.takeScreenshot(anonymousClass1, z2, z);
                    return;
                default:
                    return;
            }
        }
    };

    public IBinder onBind(Intent intent) {
        return new Messenger(this.mHandler).getBinder();
    }
}
