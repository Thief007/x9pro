package com.android.settings.users;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.android.settings.Utils;

public class ProfileUpdateReceiver extends BroadcastReceiver {
    public void onReceive(final Context context, Intent intent) {
        Log.d("ProfileUpdateReceiver", "Profile photo changed, get the PROFILE_CHANGED receiver.");
        new Thread() {
            public void run() {
                Utils.copyMeProfilePhoto(context, null);
            }
        }.start();
    }
}
