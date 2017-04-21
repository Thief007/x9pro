package com.mediatek.systemui.statusbar.extcb;

import android.content.Context;
import android.util.Log;
import com.mediatek.common.MPlugin;
import com.mediatek.systemui.ext.DefaultQuickSettingsPlugin;
import com.mediatek.systemui.ext.DefaultStatusBarPlmnPlugin;
import com.mediatek.systemui.ext.DefaultStatusBarPlugin;
import com.mediatek.systemui.ext.IQuickSettingsPlugin;
import com.mediatek.systemui.ext.IStatusBarPlmnPlugin;
import com.mediatek.systemui.ext.IStatusBarPlugin;

public class PluginFactory {
    private static final String TAG = "PluginFactory";
    private static boolean sIsDefaultStatusBarPlugin = true;
    private static IQuickSettingsPlugin sQuickSettingsPlugin = null;
    private static IStatusBarPlmnPlugin sStatusBarPlmnPlugin = null;
    private static IStatusBarPlugin sStatusBarPlugin = null;

    public static synchronized IStatusBarPlugin getStatusBarPlugin(Context context) {
        IStatusBarPlugin iStatusBarPlugin;
        synchronized (PluginFactory.class) {
            if (sStatusBarPlugin == null) {
                sStatusBarPlugin = (IStatusBarPlugin) MPlugin.createInstance(IStatusBarPlugin.class.getName(), context);
                sIsDefaultStatusBarPlugin = false;
                if (sStatusBarPlugin == null) {
                    sStatusBarPlugin = new DefaultStatusBarPlugin(context);
                    sIsDefaultStatusBarPlugin = true;
                }
            }
            iStatusBarPlugin = sStatusBarPlugin;
        }
        return iStatusBarPlugin;
    }

    public static synchronized IQuickSettingsPlugin getQuickSettingsPlugin(Context context) {
        IQuickSettingsPlugin iQuickSettingsPlugin;
        synchronized (PluginFactory.class) {
            if (sQuickSettingsPlugin == null) {
                sQuickSettingsPlugin = (IQuickSettingsPlugin) MPlugin.createInstance(IQuickSettingsPlugin.class.getName(), context);
                Log.d("@M_PluginFactory", "getQuickSettingsPlugin mQuickSettingsPlugin= " + sQuickSettingsPlugin);
                if (sQuickSettingsPlugin == null) {
                    sQuickSettingsPlugin = new DefaultQuickSettingsPlugin(context);
                    Log.d("@M_PluginFactory", "getQuickSettingsPlugin get DefaultQuickSettingsPlugin = " + sQuickSettingsPlugin);
                }
            }
            iQuickSettingsPlugin = sQuickSettingsPlugin;
        }
        return iQuickSettingsPlugin;
    }

    public static synchronized boolean isDefaultStatusBarPlugin() {
        boolean z;
        synchronized (PluginFactory.class) {
            z = sIsDefaultStatusBarPlugin;
        }
        return z;
    }

    public static synchronized IStatusBarPlmnPlugin getStatusBarPlmnPlugin(Context context) {
        IStatusBarPlmnPlugin iStatusBarPlmnPlugin;
        synchronized (PluginFactory.class) {
            if (sStatusBarPlmnPlugin == null) {
                sStatusBarPlmnPlugin = (IStatusBarPlmnPlugin) MPlugin.createInstance(IStatusBarPlmnPlugin.class.getName(), context);
                if (sStatusBarPlmnPlugin == null) {
                    sStatusBarPlmnPlugin = new DefaultStatusBarPlmnPlugin(context);
                }
                Log.d("@M_PluginFactory", "getStatusBarPlmnPlugin: " + sStatusBarPlmnPlugin);
            }
            iStatusBarPlmnPlugin = sStatusBarPlmnPlugin;
        }
        return iStatusBarPlmnPlugin;
    }
}
