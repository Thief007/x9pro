package com.android.systemui.recents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class RecentsUserEventProxyReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        Recents recents = Recents.getInstanceAndStartIfNeeded(context);
        String action = intent.getAction();
        if (action.equals("com.android.systemui.recents.action.SHOW_RECENTS_FOR_USER")) {
            recents.showRecentsInternal(intent.getBooleanExtra("triggeredFromAltTab", false));
        } else if (action.equals("com.android.systemui.recents.action.HIDE_RECENTS_FOR_USER")) {
            recents.hideRecentsInternal(intent.getBooleanExtra("triggeredFromAltTab", false), intent.getBooleanExtra("triggeredFromHomeKey", false));
        } else if (action.equals("com.android.systemui.recents.action.TOGGLE_RECENTS_FOR_USER")) {
            recents.toggleRecentsInternal();
        } else if (action.equals("com.android.systemui.recents.action.PRELOAD_RECENTS_FOR_USER")) {
            recents.preloadRecentsInternal();
        } else if (action.equals("com.android.systemui.recents.action.CONFIG_CHANGED_FOR_USER")) {
            recents.configurationChanged();
        }
    }
}
