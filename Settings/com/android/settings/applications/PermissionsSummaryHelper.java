package com.android.settings.applications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class PermissionsSummaryHelper {

    public interface PermissionsResultCallback {
        void onPermissionSummaryResult(int[] iArr, CharSequence[] charSequenceArr);
    }

    public static BroadcastReceiver getPermissionSummary(Context context, String pkg, PermissionsResultCallback callback) {
        Intent request = new Intent("android.intent.action.GET_PERMISSIONS_COUNT");
        request.putExtra("android.intent.extra.PACKAGE_NAME", pkg);
        return sendPermissionRequest(context, "com.android.settings.PERM_COUNT_RESPONSE", request, callback);
    }

    private static BroadcastReceiver sendPermissionRequest(Context context, String action, Intent request, final PermissionsResultCallback callback) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                callback.onPermissionSummaryResult(intent.getIntArrayExtra("android.intent.extra.GET_PERMISSIONS_COUNT_RESULT"), intent.getCharSequenceArrayExtra("android.intent.extra.GET_PERMISSIONS_GROUP_LIST_RESULT"));
                context.unregisterReceiver(this);
            }
        };
        context.registerReceiver(receiver, new IntentFilter(action));
        request.putExtra("android.intent.extra.GET_PERMISSIONS_RESONSE_INTENT", action);
        request.setFlags(268435456);
        context.sendBroadcast(request);
        return receiver;
    }
}
