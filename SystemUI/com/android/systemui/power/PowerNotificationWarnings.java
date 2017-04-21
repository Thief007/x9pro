package com.android.systemui.power;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioAttributes.Builder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.util.Slog;
import com.android.systemui.R;
import com.android.systemui.power.PowerUI.WarningsUI;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import java.io.PrintWriter;
import java.text.NumberFormat;

public class PowerNotificationWarnings implements WarningsUI {
    private static final AudioAttributes AUDIO_ATTRIBUTES = new Builder().setContentType(4).setUsage(13).build();
    private static final boolean DEBUG = PowerUI.DEBUG;
    private static final String[] SHOWING_STRINGS = new String[]{"SHOWING_NOTHING", "SHOWING_WARNING", "SHOWING_SAVER", "SHOWING_INVALID_CHARGER"};
    private int mBatteryLevel;
    private int mBucket;
    private long mBucketDroppedNegativeTimeMs;
    private final Context mContext;
    private final Handler mHandler = new Handler();
    private boolean mInvalidCharger;
    private final NotificationManager mNoMan;
    private final Intent mOpenBatterySettings = settings("android.intent.action.POWER_USAGE_SUMMARY");
    private final Intent mOpenSaverSettings = settings("android.settings.BATTERY_SAVER_SETTINGS");
    private boolean mPlaySound;
    private final PowerManager mPowerMan;
    private final Receiver mReceiver = new Receiver();
    private boolean mSaver;
    private SystemUIDialog mSaverConfirmation;
    private long mScreenOffTime;
    private int mShowing;
    private final OnClickListener mStartSaverMode = new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            AsyncTask.execute(new Runnable() {
                public void run() {
                    PowerNotificationWarnings.this.setSaverMode(true);
                }
            });
        }
    };
    private boolean mWarning;

    private final class Receiver extends BroadcastReceiver {
        private Receiver() {
        }

        public void init() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("PNW.batterySettings");
            filter.addAction("PNW.startSaver");
            filter.addAction("PNW.stopSaver");
            filter.addAction("PNW.dismissedWarning");
            PowerNotificationWarnings.this.mContext.registerReceiverAsUser(this, UserHandle.ALL, filter, "android.permission.STATUS_BAR_SERVICE", PowerNotificationWarnings.this.mHandler);
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Slog.i("PowerUI.Notification", "Received " + action);
            if (action.equals("PNW.batterySettings")) {
                PowerNotificationWarnings.this.dismissLowBatteryNotification();
                PowerNotificationWarnings.this.mContext.startActivityAsUser(PowerNotificationWarnings.this.mOpenBatterySettings, UserHandle.CURRENT);
            } else if (action.equals("PNW.startSaver")) {
                PowerNotificationWarnings.this.dismissLowBatteryNotification();
                PowerNotificationWarnings.this.showStartSaverConfirmation();
            } else if (action.equals("PNW.stopSaver")) {
                PowerNotificationWarnings.this.dismissSaverNotification();
                PowerNotificationWarnings.this.dismissLowBatteryNotification();
                PowerNotificationWarnings.this.setSaverMode(false);
            } else if (action.equals("PNW.dismissedWarning")) {
                PowerNotificationWarnings.this.dismissLowBatteryWarning();
            }
        }
    }

    public PowerNotificationWarnings(Context context, PhoneStatusBar phoneStatusBar) {
        this.mContext = context;
        this.mNoMan = (NotificationManager) context.getSystemService("notification");
        this.mPowerMan = (PowerManager) context.getSystemService("power");
        this.mReceiver.init();
    }

    public void dump(PrintWriter pw) {
        String str = null;
        pw.print("mSaver=");
        pw.println(this.mSaver);
        pw.print("mWarning=");
        pw.println(this.mWarning);
        pw.print("mPlaySound=");
        pw.println(this.mPlaySound);
        pw.print("mInvalidCharger=");
        pw.println(this.mInvalidCharger);
        pw.print("mShowing=");
        pw.println(SHOWING_STRINGS[this.mShowing]);
        pw.print("mSaverConfirmation=");
        if (this.mSaverConfirmation != null) {
            str = "not null";
        }
        pw.println(str);
    }

    public void update(int batteryLevel, int bucket, long screenOffTime) {
        this.mBatteryLevel = batteryLevel;
        if (bucket >= 0) {
            this.mBucketDroppedNegativeTimeMs = 0;
        } else if (bucket < this.mBucket) {
            this.mBucketDroppedNegativeTimeMs = System.currentTimeMillis();
        }
        this.mBucket = bucket;
        this.mScreenOffTime = screenOffTime;
    }

    public void showSaverMode(boolean mode) {
        this.mSaver = mode;
        if (this.mSaver && this.mSaverConfirmation != null) {
            this.mSaverConfirmation.dismiss();
        }
        updateNotification();
    }

    private void updateNotification() {
        if (DEBUG) {
            Slog.d("PowerUI.Notification", "updateNotification mWarning=" + this.mWarning + " mPlaySound=" + this.mPlaySound + " mSaver=" + this.mSaver + " mInvalidCharger=" + this.mInvalidCharger);
        }
        if (this.mInvalidCharger) {
            showInvalidChargerNotification();
            this.mShowing = 3;
        } else if (this.mWarning) {
            showWarningNotification();
            this.mShowing = 1;
        } else if (this.mSaver) {
            showSaverNotification();
            this.mShowing = 2;
        } else {
            this.mNoMan.cancelAsUser("low_battery", R.id.notification_power, UserHandle.ALL);
            this.mShowing = 0;
        }
    }

    private void showInvalidChargerNotification() {
        Notification n = new Notification.Builder(this.mContext).setSmallIcon(R.drawable.ic_power_low).setWhen(0).setShowWhen(false).setOngoing(true).setContentTitle(this.mContext.getString(R.string.invalid_charger_title)).setContentText(this.mContext.getString(R.string.invalid_charger_text)).setPriority(2).setVisibility(1).setColor(this.mContext.getColor(17170521)).build();
        if (n.headsUpContentView != null) {
            n.headsUpContentView.setViewVisibility(16908352, 8);
        }
        this.mNoMan.notifyAsUser("low_battery", R.id.notification_power, n, UserHandle.ALL);
    }

    private void showWarningNotification() {
        int textRes;
        if (this.mSaver) {
            textRes = R.string.battery_low_percent_format_saver_started;
        } else {
            textRes = R.string.battery_low_percent_format;
        }
        String percentage = NumberFormat.getPercentInstance().format(((double) this.mBatteryLevel) / 100.0d);
        Notification.Builder nb = new Notification.Builder(this.mContext).setSmallIcon(R.drawable.ic_power_low).setWhen(this.mBucketDroppedNegativeTimeMs).setShowWhen(false).setContentTitle(this.mContext.getString(R.string.battery_low_title)).setContentText(this.mContext.getString(textRes, new Object[]{percentage})).setOnlyAlertOnce(true).setDeleteIntent(pendingBroadcast("PNW.dismissedWarning")).setPriority(2).setVisibility(1).setColor(this.mContext.getColor(17170522));
        if (hasBatterySettings()) {
            nb.setContentIntent(pendingBroadcast("PNW.batterySettings"));
        }
        if (this.mSaver) {
            addStopSaverAction(nb);
        } else {
            nb.addAction(0, this.mContext.getString(R.string.battery_saver_start_action), pendingBroadcast("PNW.startSaver"));
        }
        if (this.mPlaySound) {
            attachLowBatterySound(nb);
            this.mPlaySound = false;
        }
        Notification n = nb.build();
        if (n.headsUpContentView != null) {
            n.headsUpContentView.setViewVisibility(16908352, 8);
        }
        this.mNoMan.notifyAsUser("low_battery", R.id.notification_power, n, UserHandle.ALL);
    }

    private void showSaverNotification() {
        Notification.Builder nb = new Notification.Builder(this.mContext).setSmallIcon(R.drawable.ic_power_saver).setContentTitle(this.mContext.getString(R.string.battery_saver_notification_title)).setContentText(this.mContext.getString(R.string.battery_saver_notification_text)).setOngoing(true).setShowWhen(false).setVisibility(1).setColor(this.mContext.getColor(17170522));
        addStopSaverAction(nb);
        if (hasSaverSettings()) {
            nb.setContentIntent(pendingActivity(this.mOpenSaverSettings));
        }
        this.mNoMan.notifyAsUser("low_battery", R.id.notification_power, nb.build(), UserHandle.ALL);
    }

    private void addStopSaverAction(Notification.Builder nb) {
        nb.addAction(0, this.mContext.getString(R.string.battery_saver_notification_action_text), pendingBroadcast("PNW.stopSaver"));
    }

    private void dismissSaverNotification() {
        if (this.mSaver) {
            Slog.i("PowerUI.Notification", "dismissing saver notification");
        }
        this.mSaver = false;
        updateNotification();
    }

    private PendingIntent pendingActivity(Intent intent) {
        return PendingIntent.getActivityAsUser(this.mContext, 0, intent, 0, null, UserHandle.CURRENT);
    }

    private PendingIntent pendingBroadcast(String action) {
        return PendingIntent.getBroadcastAsUser(this.mContext, 0, new Intent(action), 0, UserHandle.CURRENT);
    }

    private static Intent settings(String action) {
        return new Intent(action).setFlags(1551892480);
    }

    public boolean isInvalidChargerWarningShowing() {
        return this.mInvalidCharger;
    }

    public void updateLowBatteryWarning() {
        updateNotification();
    }

    public void dismissLowBatteryWarning() {
        if (DEBUG) {
            Slog.d("PowerUI.Notification", "dismissing low battery warning: level=" + this.mBatteryLevel);
        }
        dismissLowBatteryNotification();
    }

    private void dismissLowBatteryNotification() {
        if (this.mWarning) {
            Slog.i("PowerUI.Notification", "dismissing low battery notification");
        }
        this.mWarning = false;
        updateNotification();
    }

    private boolean hasBatterySettings() {
        return this.mOpenBatterySettings.resolveActivity(this.mContext.getPackageManager()) != null;
    }

    private boolean hasSaverSettings() {
        return this.mOpenSaverSettings.resolveActivity(this.mContext.getPackageManager()) != null;
    }

    public void showLowBatteryWarning(boolean playSound) {
        Slog.i("PowerUI.Notification", "show low battery warning: level=" + this.mBatteryLevel + " [" + this.mBucket + "] playSound=" + playSound);
        this.mPlaySound = playSound;
        this.mWarning = true;
        updateNotification();
    }

    private void attachLowBatterySound(Notification.Builder b) {
        ContentResolver cr = this.mContext.getContentResolver();
        int silenceAfter = Global.getInt(cr, "low_battery_sound_timeout", 0);
        long offTime = SystemClock.elapsedRealtime() - this.mScreenOffTime;
        if (silenceAfter <= 0 || this.mScreenOffTime <= 0 || offTime <= ((long) silenceAfter)) {
            if (DEBUG) {
                Slog.d("PowerUI.Notification", "playing low battery sound. pick-a-doop!");
            }
            if (Global.getInt(cr, "power_sounds_enabled", 1) == 1) {
                String soundPath = Global.getString(cr, "low_battery_sound");
                if (soundPath != null) {
                    Uri soundUri = Uri.parse("file://" + soundPath);
                    if (soundUri != null) {
                        b.setSound(soundUri, AUDIO_ATTRIBUTES);
                        if (DEBUG) {
                            Slog.d("PowerUI.Notification", "playing sound " + soundUri);
                        }
                    }
                }
            }
            return;
        }
        Slog.i("PowerUI.Notification", "screen off too long (" + offTime + "ms, limit " + silenceAfter + "ms): not waking up the user with low battery sound");
    }

    public void dismissInvalidChargerWarning() {
        dismissInvalidChargerNotification();
    }

    private void dismissInvalidChargerNotification() {
        if (this.mInvalidCharger) {
            Slog.i("PowerUI.Notification", "dismissing invalid charger notification");
        }
        this.mInvalidCharger = false;
        updateNotification();
    }

    public void showInvalidChargerWarning() {
        this.mInvalidCharger = true;
        updateNotification();
    }

    public void userSwitched() {
        updateNotification();
    }

    private void showStartSaverConfirmation() {
        if (this.mSaverConfirmation == null) {
            SystemUIDialog d = new SystemUIDialog(this.mContext);
            d.setTitle(R.string.battery_saver_confirmation_title);
            d.setMessage(17040753);
            d.setNegativeButton(17039360, null);
            d.setPositiveButton(R.string.battery_saver_confirmation_ok, this.mStartSaverMode);
            d.setShowForAllUsers(true);
            d.setOnDismissListener(new OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    PowerNotificationWarnings.this.mSaverConfirmation = null;
                }
            });
            d.show();
            this.mSaverConfirmation = d;
        }
    }

    private void setSaverMode(boolean mode) {
        this.mPowerMan.setPowerSaveMode(mode);
    }
}
