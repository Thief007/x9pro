package com.mediatek.systemui.ext;

import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;

public interface IQuickSettingsPlugin {
    Object customizeAddQSTile(Object obj);

    String customizeApnSettingsTile(boolean z, IconIdWrapper iconIdWrapper, String str);

    String customizeDataConnectionTile(int i, IconIdWrapper iconIdWrapper, String str);

    boolean customizeDisplayDataUsage(boolean z);

    String customizeDualSimSettingsTile(boolean z, IconIdWrapper iconIdWrapper, String str);

    String customizeQuickSettingsTileOrder(String str);

    void customizeSimDataConnectionTile(int i, IconIdWrapper iconIdWrapper);
}
