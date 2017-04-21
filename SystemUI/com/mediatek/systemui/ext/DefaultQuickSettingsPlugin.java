package com.mediatek.systemui.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;

public class DefaultQuickSettingsPlugin extends ContextWrapper implements IQuickSettingsPlugin {
    private static final String TAG = "DefaultQuickSettingsPlugin";
    protected Context mContext;

    public DefaultQuickSettingsPlugin(Context context) {
        super(context);
        this.mContext = context;
    }

    public boolean customizeDisplayDataUsage(boolean isDisplay) {
        Log.i(TAG, "customizeDisplayDataUsage, return isDisplay = " + isDisplay);
        return isDisplay;
    }

    public String customizeQuickSettingsTileOrder(String defaultString) {
        return defaultString;
    }

    public Object customizeAddQSTile(Object qsTile) {
        return null;
    }

    public String customizeDataConnectionTile(int dataState, IconIdWrapper icon, String orgLabelStr) {
        Log.i(TAG, "customizeDataConnectionTile, icon = " + icon + ", orgLabelStr=" + orgLabelStr);
        return orgLabelStr;
    }

    public String customizeDualSimSettingsTile(boolean enable, IconIdWrapper icon, String labelStr) {
        Log.i(TAG, "customizeDualSimSettingsTile, enable = " + enable + " icon=" + icon + " labelStr=" + labelStr);
        return labelStr;
    }

    public void customizeSimDataConnectionTile(int state, IconIdWrapper icon) {
        Log.i(TAG, "customizeSimDataConnectionTile, state = " + state + " icon=" + icon);
    }

    public String customizeApnSettingsTile(boolean enable, IconIdWrapper icon, String orgLabelStr) {
        return orgLabelStr;
    }
}
