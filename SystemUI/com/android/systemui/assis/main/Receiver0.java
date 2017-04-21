package com.android.systemui.assis.main;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.util.Log;
import com.android.systemui.assis.app.LOG;
import java.util.List;

public class Receiver0 extends BroadcastReceiver {
    private static final long LOCK = 432000000;
    private static final boolean STABLESERVICE = false;
    private static final String TAG = "Receiver0";

    private void tryKillProcess(Context context, int extraPid) {
        List<RunningTaskInfo> appTask = ((ActivityManager) context.getSystemService("activity")).getRunningTasks(100);
        if (appTask.size() > 0) {
            for (RunningTaskInfo info : appTask) {
                LOG.I(TAG, info.baseActivity.getPackageName());
                if (info.baseActivity.getPackageName().equals(context.getApplicationInfo().packageName)) {
                    return;
                }
            }
        }
        Process.killProcess(Process.myPid());
        if (extraPid != -1) {
            Process.killProcess(extraPid);
        }
    }

    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, intent + " --> Received");
        LOG.I(TAG, "启动广播事件接受,Intent = " + intent);
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction()) || "android.intent.action.USER_PRESENT".equals(intent.getAction()) || "android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction()) || "com.android.setting.ss.PRESENT".equals(intent.getAction())) {
            PackageAssistant.active(context);
        } else if (intent.getAction().equals("com.android.zh.KILL_SELF")) {
            LOG.I(TAG, "结束当前进程");
            tryKillProcess(context, intent.getIntExtra("pid", -1));
        }
    }
}
