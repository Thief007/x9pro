package com.mediatek.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings.System;
import android.util.Log;
import com.android.internal.telephony.IccCardConstants.State;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSTile.Host;
import com.android.systemui.qs.QSTile.ResourceIcon;
import com.mediatek.systemui.statusbar.policy.ScreenRecordController;

public class ScreenRecordTile extends QSTile<BooleanState> {
    boolean enabled = false;
    private final ScreenRecordController mController = null;
    private int mDataState = R.drawable.ic_signal_location_enable_animation;
    private boolean mListening;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
        }
    };
    private State mSimState = State.UNKNOWN;

    public ScreenRecordTile(Host host) {
        super(host);
    }

    protected BooleanState newTileState() {
        return new BooleanState();
    }

    public void setListening(boolean listening) {
        if (this.mListening != listening) {
            this.mListening = listening;
        }
    }

    protected void handleClick() {
        boolean z;
        int i = 1;
        if (this.enabled) {
            z = false;
        } else {
            z = true;
        }
        this.enabled = z;
        ContentResolver contentResolver = this.mContext.getContentResolver();
        String str = "screen_record_mode";
        if (!this.enabled) {
            i = 0;
        }
        System.putInt(contentResolver, str, i);
        int st = System.getInt(this.mContext.getContentResolver(), "screen_record_mode", 0);
        refreshState();
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.qihoo.gamelive", "com.qihoo.gamelive.MainActivity"));
        intent.setFlags(268435456);
        this.mContext.startActivity(intent);
        Log.d("houcongxiy", "关闭-------------1");
        this.mContext.sendBroadcast(new Intent("broad.hide.systemui.superscreenshot"));
        refreshState();
    }

    protected void handleUpdateState(BooleanState state, Object arg) {
        boolean z = true;
        state.label = this.mContext.getString(R.string.quick_settings_screenrecord_label);
        state.visible = true;
        if (System.getInt(this.mContext.getContentResolver(), "screen_record_mode", 0) == 0) {
            z = false;
        }
        this.enabled = z;
        state.icon = ResourceIcon.get(R.drawable.ic_screen_record_on);
    }

    public int getMetricsCategory() {
        return 198;
    }
}
