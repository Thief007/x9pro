package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.android.internal.telephony.Phone;

public class WriteIMEI extends BroadcastReceiver {
    String TAG = "WriteIMEI";
    private Context mContext = null;
    private Phone mPhone = null;
    private Phone mPhone_1 = null;
    private Phone mPhone_2 = null;

    public void onReceive(Context context, Intent intent) {
        Log.e(this.TAG, "Auto generate IMEI disabled");
    }
}
