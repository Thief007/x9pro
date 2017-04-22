package com.android.settings.applock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.util.Log;

public class ScreenReceiver extends BroadcastReceiver {
    Context context;

    class updateThread implements Runnable {
        updateThread() {
        }

        public void run() {
            new AppListDaoImpl(ScreenReceiver.this.context).update();
        }
    }

    public void onReceive(Context context, Intent intent) {
        this.context = context;
        String action = intent.getAction();
        if (context.getSharedPreferences("settinglock", 0).getBoolean("settinglock", true)) {
            Thread thread = new Thread(new updateThread());
            if ("android.intent.action.SCREEN_ON".equals(action)) {
                Log.d("dkl", "screen is on...");
            }
            if ("android.intent.action.USER_PRESENT".equals(action)) {
                Log.d("dkl", "screen is unlock...");
                if (!false) {
                    thread.start();
                    thread.interrupt();
                }
            }
            return;
        }
        Process.killProcess(Process.myPid());
    }
}
