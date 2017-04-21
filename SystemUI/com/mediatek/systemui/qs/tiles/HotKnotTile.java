package com.mediatek.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSTile.Host;
import com.mediatek.hotknot.HotKnotAdapter;
import com.mediatek.systemui.statusbar.policy.HotKnotController;

public class HotKnotTile extends QSTile<BooleanState> {
    private final HotKnotController mController;
    private final AnimationIcon mDisable = new AnimationIcon(R.drawable.ic_signal_hotknot_disable_animation);
    private final AnimationIcon mEnable = new AnimationIcon(R.drawable.ic_signal_hotknot_enable_animation);
    private boolean mListening;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("com.mediatek.hotknot.action.ADAPTER_STATE_CHANGED".equals(intent.getAction())) {
                Log.d("HotKnotTile", "HotKnotAdapter onReceive DAPTER_STATE_CHANGED");
                HotKnotTile.this.refreshState();
            }
        }
    };

    public HotKnotTile(Host host) {
        super(host);
        this.mController = host.getHotKnotController();
    }

    protected BooleanState newTileState() {
        return new BooleanState();
    }

    public void setListening(boolean listening) {
        if (this.mListening != listening) {
            this.mListening = listening;
            if (listening) {
                IntentFilter filter = new IntentFilter();
                filter.addAction("com.mediatek.hotknot.action.ADAPTER_STATE_CHANGED");
                this.mContext.registerReceiver(this.mReceiver, filter);
            } else {
                this.mContext.unregisterReceiver(this.mReceiver);
            }
        }
    }

    protected void handleClick() {
        HotKnotAdapter adapter = this.mController.getAdapter();
        boolean desiredState = !this.mController.isHotKnotOn();
        Log.d("HotKnotTile", "hotknot desiredState=" + desiredState);
        this.mEnable.setAllowAnimation(true);
        this.mDisable.setAllowAnimation(true);
        if (desiredState) {
            adapter.enable();
        } else {
            adapter.disable();
        }
    }

    protected void handleLongClick() {
        Intent intent = new Intent("mediatek.settings.HOTKNOT_SETTINGS");
        intent.setFlags(335544320);
        this.mHost.startActivityDismissingKeyguard(intent);
    }

    protected void handleUpdateState(BooleanState state, Object arg) {
        state.label = this.mContext.getString(R.string.quick_settings_hotknot_label);
        state.visible = true;
        boolean desiredState = this.mController.isHotKnotOn();
        Log.d("HotKnotTile", "HotKnot UpdateState desiredState=" + desiredState);
        if (desiredState) {
            state.icon = this.mEnable;
        } else {
            state.icon = this.mDisable;
        }
    }

    public int getMetricsCategory() {
        return 111;
    }
}
