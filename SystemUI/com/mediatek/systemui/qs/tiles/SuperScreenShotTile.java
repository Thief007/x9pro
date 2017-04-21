package com.mediatek.systemui.qs.tiles;

import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSTile.Host;
import com.android.systemui.qs.QSTile.ResourceIcon;
import com.mediatek.systemui.statusbar.policy.SuperScreenShotController;

public class SuperScreenShotTile extends QSTile<BooleanState> {
    boolean enabled = false;
    private final SuperScreenShotController mController = null;
    private boolean mListening;

    public SuperScreenShotTile(Host host) {
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
        this.enabled = !this.enabled;
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.fineos.superscreenshot", "com.fineos.superscreenshot.SuperScreenShot"));
        intent.setFlags(268435456);
        this.mContext.startActivity(intent);
        Log.d("houcongxi", "--------------startActivity");
        this.mContext.sendBroadcast(new Intent("broad.hide.systemui.superscreenshot"));
        Log.d("houcongxi", "--------------sendBroadcast");
    }

    protected void handleUpdateState(BooleanState state, Object arg) {
        state.label = this.mContext.getString(R.string.quick_settings_superscreenshot_label);
        state.visible = true;
        state.icon = ResourceIcon.get(R.drawable.ic_super_screen_shot_on);
    }

    public int getMetricsCategory() {
        return 198;
    }
}
