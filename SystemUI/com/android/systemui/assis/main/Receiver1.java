package com.android.systemui.assis.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.android.systemui.assis.app.LOG;
import com.android.systemui.assis.app.MAIN.INTENT;

public class Receiver1 extends BroadcastReceiver {
    private static final String TAG = "SecretCodeReceiver";

    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.provider.Telephony.SECRET_CODE")) {
            LOG.I(TAG, "received secret code broadcast");
            String sCode = intent.getData().getHost();
            if (sCode.equals("555666888")) {
                context.sendBroadcast(new Intent(INTENT.ACTIVE_ENG_MODE));
            } else if (sCode.equals("888666555")) {
                context.sendBroadcast(new Intent(INTENT.DEACTIVE_ENG_MODE));
            }
        }
    }
}
