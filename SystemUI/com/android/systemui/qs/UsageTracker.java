package com.android.systemui.qs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.assis.app.MAIN.CONSTANT;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.android.systemui.statusbar.policy.Listenable;

public class UsageTracker implements Listenable {
    private final Context mContext;
    private final String mPrefKey;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (UsageTracker.this.mResetAction.equals(intent.getAction())) {
                UsageTracker.this.reset();
            }
        }
    };
    private boolean mRegistered;
    private final String mResetAction;
    private final long mTimeToShowTile;

    public UsageTracker(Context context, String prefKey, Class<?> tile, int timeoutResource) {
        this.mContext = context;
        this.mPrefKey = prefKey;
        this.mTimeToShowTile = ((long) this.mContext.getResources().getInteger(timeoutResource)) * CONSTANT.ONE_DAY;
        this.mResetAction = "com.android.systemui.qs." + tile.getSimpleName() + ".usage_reset";
    }

    public void setListening(boolean listen) {
        if (listen && !this.mRegistered) {
            this.mContext.registerReceiver(this.mReceiver, new IntentFilter(this.mResetAction));
            this.mRegistered = true;
        } else if (!listen && this.mRegistered) {
            this.mContext.unregisterReceiver(this.mReceiver);
            this.mRegistered = false;
        }
    }

    public boolean isRecentlyUsed() {
        return System.currentTimeMillis() - Prefs.getLong(this.mContext, this.mPrefKey, 0) < this.mTimeToShowTile;
    }

    public void trackUsage() {
        Prefs.putLong(this.mContext, this.mPrefKey, System.currentTimeMillis());
    }

    public void reset() {
        Prefs.remove(this.mContext, this.mPrefKey);
    }

    public void showResetConfirmation(String title, final Runnable onConfirmed) {
        SystemUIDialog d = new SystemUIDialog(this.mContext);
        d.setTitle(title);
        d.setMessage(this.mContext.getString(R.string.quick_settings_reset_confirmation_message));
        d.setNegativeButton(17039360, null);
        d.setPositiveButton(R.string.quick_settings_reset_confirmation_button, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                UsageTracker.this.reset();
                if (onConfirmed != null) {
                    onConfirmed.run();
                }
            }
        });
        d.setCanceledOnTouchOutside(true);
        d.show();
    }
}
