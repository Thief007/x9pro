package com.android.systemui.tuner;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.util.ArrayMap;
import com.android.systemui.R;
import com.android.systemui.SystemUI;
import com.android.systemui.SystemUIApplication;
import com.android.systemui.settings.CurrentUserTracker;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TunerService extends SystemUI {
    private static TunerService sInstance;
    private ContentResolver mContentResolver;
    private int mCurrentUser;
    private final ArrayMap<Uri, String> mListeningUris = new ArrayMap();
    private final Observer mObserver = new Observer();
    private final HashMap<String, List<Tunable>> mTunableLookup = new HashMap();
    private CurrentUserTracker mUserTracker;

    public interface Tunable {
        void onTuningChanged(String str, String str2);
    }

    public static class ClearReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            if ("com.android.systemui.action.CLEAR_TUNER".equals(intent.getAction())) {
                TunerService.get(context).clearAll();
            }
        }
    }

    private class Observer extends ContentObserver {
        public Observer() {
            super(new Handler(Looper.getMainLooper()));
        }

        public void onChange(boolean selfChange, Uri uri, int userId) {
            if (userId == ActivityManager.getCurrentUser()) {
                TunerService.this.reloadSetting(uri);
            }
        }
    }

    public void start() {
        this.mContentResolver = this.mContext.getContentResolver();
        putComponent(TunerService.class, this);
        this.mCurrentUser = ActivityManager.getCurrentUser();
        this.mUserTracker = new CurrentUserTracker(this.mContext) {
            public void onUserSwitched(int newUserId) {
                TunerService.this.mCurrentUser = newUserId;
                TunerService.this.reloadAll();
                TunerService.this.reregisterAll();
            }
        };
        this.mUserTracker.startTracking();
    }

    public void addTunable(Tunable tunable, String... keys) {
        for (String key : keys) {
            addTunable(tunable, key);
        }
    }

    private void addTunable(Tunable tunable, String key) {
        if (!this.mTunableLookup.containsKey(key)) {
            this.mTunableLookup.put(key, new ArrayList());
        }
        ((List) this.mTunableLookup.get(key)).add(tunable);
        Uri uri = Secure.getUriFor(key);
        if (!this.mListeningUris.containsKey(uri)) {
            this.mListeningUris.put(uri, key);
            this.mContentResolver.registerContentObserver(uri, false, this.mObserver, this.mCurrentUser);
        }
        tunable.onTuningChanged(key, Secure.getStringForUser(this.mContentResolver, key, this.mCurrentUser));
    }

    public void removeTunable(Tunable tunable) {
        for (List<Tunable> list : this.mTunableLookup.values()) {
            list.remove(tunable);
        }
    }

    protected void reregisterAll() {
        if (this.mListeningUris.size() != 0) {
            this.mContentResolver.unregisterContentObserver(this.mObserver);
            for (Uri uri : this.mListeningUris.keySet()) {
                this.mContentResolver.registerContentObserver(uri, false, this.mObserver, this.mCurrentUser);
            }
        }
    }

    public void reloadSetting(Uri uri) {
        String key = (String) this.mListeningUris.get(uri);
        String value = Secure.getStringForUser(this.mContentResolver, key, this.mCurrentUser);
        for (Tunable tunable : (List) this.mTunableLookup.get(key)) {
            tunable.onTuningChanged(key, value);
        }
    }

    private void reloadAll() {
        for (String key : this.mTunableLookup.keySet()) {
            String value = Secure.getStringForUser(this.mContentResolver, key, this.mCurrentUser);
            for (Tunable tunable : (List) this.mTunableLookup.get(key)) {
                tunable.onTuningChanged(key, value);
            }
        }
    }

    public void clearAll() {
        Global.putString(this.mContentResolver, "sysui_demo_allowed", null);
        System.putString(this.mContentResolver, "status_bar_show_battery_percent", null);
        Intent intent = new Intent("com.android.systemui.demo");
        intent.putExtra("command", "exit");
        this.mContext.sendBroadcast(intent);
        for (String key : this.mTunableLookup.keySet()) {
            Secure.putString(this.mContentResolver, key, null);
        }
    }

    public static TunerService get(Context context) {
        TunerService service = (TunerService) ((SystemUIApplication) context.getApplicationContext()).getComponent(TunerService.class);
        if (service == null) {
            return getStaticService(context);
        }
        return service;
    }

    private static TunerService getStaticService(Context context) {
        if (sInstance == null) {
            sInstance = new TunerService();
            sInstance.mContext = context.getApplicationContext();
            sInstance.mComponents = new HashMap();
            sInstance.start();
        }
        return sInstance;
    }

    public static final void showResetRequest(final Context context, final Runnable onDisabled) {
        SystemUIDialog dialog = new SystemUIDialog(context);
        dialog.setShowForAllUsers(true);
        dialog.setMessage(R.string.remove_from_settings_prompt);
        dialog.setButton(-2, context.getString(R.string.cancel), (OnClickListener) null);
        dialog.setButton(-1, context.getString(R.string.guest_exit_guest_dialog_remove), new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                context.sendBroadcast(new Intent("com.android.systemui.action.CLEAR_TUNER"));
                TunerService.setTunerEnabled(context, false);
                Secure.putInt(context.getContentResolver(), "seen_tuner_warning", 0);
                if (onDisabled != null) {
                    onDisabled.run();
                }
            }
        });
        dialog.show();
    }

    public static final void setTunerEnabled(Context context, boolean enabled) {
        int i;
        PackageManager packageManager = userContext(context).getPackageManager();
        ComponentName componentName = new ComponentName(context, TunerActivity.class);
        if (enabled) {
            i = 1;
        } else {
            i = 2;
        }
        packageManager.setComponentEnabledSetting(componentName, i, 1);
    }

    public static final boolean isTunerEnabled(Context context) {
        return userContext(context).getPackageManager().getComponentEnabledSetting(new ComponentName(context, TunerActivity.class)) == 1;
    }

    private static Context userContext(Context context) {
        try {
            return context.createPackageContextAsUser(context.getPackageName(), 0, new UserHandle(ActivityManager.getCurrentUser()));
        } catch (NameNotFoundException e) {
            return context;
        }
    }
}
