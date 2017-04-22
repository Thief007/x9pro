package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import com.android.internal.telephony.Phone;

public class SimLockReceiver extends BroadcastReceiver {
    private final int EVENT_MODIFY_NP = 1;
    private final int EVENT_QUERY_NP = 2;
    private Phone currentPhoneEx;
    private Context mContext;
    private Handler mResponseHander = new C01911();

    class C01911 extends Handler {
        C01911() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (msg.obj.exception == null) {
                        Log.i("SimLockReceiver", "successfully.");
                        Intent intent;
                        if (SystemProperties.get("persist.sys.simlock_state", "lock").equals("lock")) {
                            SystemProperties.set("persist.sys.simlock_state", "unlock");
                            intent = new Intent(SimLockReceiver.this.mContext, RebootDialog.class);
                            intent.addFlags(268435456);
                            intent.putExtra("reason", "unlock");
                            SimLockReceiver.this.mContext.startActivity(intent);
                            Log.d("SimLockReceiver", "unlock done");
                            return;
                        }
                        SystemProperties.set("persist.sys.simlock_state", "lock");
                        intent = new Intent(SimLockReceiver.this.mContext, RebootDialog.class);
                        intent.putExtra("reason", "lock");
                        intent.addFlags(268435456);
                        SimLockReceiver.this.mContext.startActivity(intent);
                        Log.d("SimLockReceiver", "lock done");
                        return;
                    }
                    Log.i("SimLockReceiver", "Fail.");
                    return;
                case 2:
                    Object result = ((AsyncResult) msg.obj).result;
                    if (result == null || !(result instanceof String[])) {
                        Log.d("SimLockReceiver", "no result string");
                        return;
                    }
                    String[] data = (String[]) result;
                    if (data.length > 0) {
                        for (int i = 0; i < data.length; i++) {
                            Log.d("SimLockReceiver", "string data[i] = " + data[i]);
                            if (data[i].substring(12, 13).equals("1")) {
                                Log.d("SimLockReceiver", "set simlock status : lock");
                                SystemProperties.set("persist.sys.simlock_state", "lock");
                            } else {
                                Log.d("SimLockReceiver", "set simlock status : unlock");
                                SystemProperties.set("persist.sys.simlock_state", "unlock");
                            }
                        }
                        return;
                    }
                    Log.d("SimLockReceiver", "string length 0");
                    return;
                default:
                    return;
            }
        }
    }

    public void onReceive(Context context, Intent intent) {
    }
}
