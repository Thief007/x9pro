package com.mediatek.systemui;

import android.content.Context;
import com.mediatek.common.MPlugin;
import com.mediatek.systemui.ext.DefaultMobileIconExt;
import com.mediatek.systemui.ext.DefaultStatusBarPlugin;
import com.mediatek.systemui.ext.IMobileIconExt;
import com.mediatek.systemui.ext.IStatusBarPlugin;

public class PluginManager {
    private static IMobileIconExt sMobileIconExt = null;
    private static IStatusBarPlugin sSystemUIStatusBarExt = null;

    public static synchronized IMobileIconExt getMobileIconExt(Context context) {
        IMobileIconExt iMobileIconExt;
        synchronized (PluginManager.class) {
            if (sMobileIconExt == null) {
                sMobileIconExt = (IMobileIconExt) MPlugin.createInstance(IMobileIconExt.class.getName(), context);
                if (sMobileIconExt == null) {
                    sMobileIconExt = new DefaultMobileIconExt();
                }
            }
            iMobileIconExt = sMobileIconExt;
        }
        return iMobileIconExt;
    }

    public static synchronized IStatusBarPlugin getSystemUIStatusBarExt(Context context) {
        IStatusBarPlugin statusBarExt;
        synchronized (PluginManager.class) {
            if (sSystemUIStatusBarExt == null) {
                sSystemUIStatusBarExt = new DefaultStatusBarPlugin(context);
            }
            statusBarExt = (IStatusBarPlugin) MPlugin.createInstance(IStatusBarPlugin.class.getName(), context);
            if (statusBarExt == null) {
                statusBarExt = sSystemUIStatusBarExt;
            }
        }
        return statusBarExt;
    }
}
