package com.android.systemui;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.SystemProperties;
import android.util.Log;
import com.android.systemui.assis.app.MAIN.EVENT;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.media.RingtonePlayer;
import com.android.systemui.power.PowerUI;
import com.android.systemui.recents.Recents;
import com.android.systemui.statusbar.SystemBars;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.usb.StorageNotification;
import com.android.systemui.volume.VolumeUI;
import com.mediatek.systemui.statusbar.extcb.FeatureOptionUtils;
import java.util.HashMap;
import java.util.Map;

public class SystemUIApplication extends Application {
    private final Class<?>[] SERVICES = new Class[]{TunerService.class, KeyguardViewMediator.class, Recents.class, VolumeUI.class, SystemBars.class, StorageNotification.class, PowerUI.class, RingtonePlayer.class};
    private boolean mBootCompleted;
    private final Map<Class<?>, Object> mComponents = new HashMap();
    private final SystemUI[] mServices = new SystemUI[this.SERVICES.length];
    private boolean mServicesStarted;

    public void onCreate() {
        super.onCreate();
        setTheme(R.style.systemui_theme);
        IntentFilter filter = new IntentFilter("android.intent.action.BOOT_COMPLETED");
        filter.setPriority(EVENT.DYNAMIC_PACK_EVENT_BASE);
        registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (!SystemUIApplication.this.mBootCompleted) {
                    SystemUIApplication.this.unregisterReceiver(this);
                    SystemUIApplication.this.mBootCompleted = true;
                    if (SystemUIApplication.this.mServicesStarted) {
                        for (SystemUI onBootCompleted : SystemUIApplication.this.mServices) {
                            onBootCompleted.onBootCompleted();
                        }
                    }
                }
            }
        }, filter);
    }

    public void startServicesIfNeeded() {
        if (!this.mServicesStarted) {
            if (!this.mBootCompleted && FeatureOptionUtils.SUPPORT_YES.equals(SystemProperties.get("sys.boot_completed"))) {
                this.mBootCompleted = true;
            }
            Log.v("SystemUIService", "Starting SystemUI services.");
            int N = this.SERVICES.length;
            int i = 0;
            while (i < N) {
                try {
                    this.mServices[i] = (SystemUI) this.SERVICES[i].newInstance();
                    this.mServices[i].mContext = this;
                    this.mServices[i].mComponents = this.mComponents;
                    this.mServices[i].start();
                    if (this.mBootCompleted) {
                        this.mServices[i].onBootCompleted();
                    }
                    i++;
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                } catch (InstantiationException ex2) {
                    throw new RuntimeException(ex2);
                }
            }
            this.mServicesStarted = true;
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        if (this.mServicesStarted) {
            for (SystemUI onConfigurationChanged : this.mServices) {
                onConfigurationChanged.onConfigurationChanged(newConfig);
            }
        }
    }

    public <T> T getComponent(Class<T> interfaceType) {
        return this.mComponents.get(interfaceType);
    }

    public SystemUI[] getServices() {
        return this.mServices;
    }
}
