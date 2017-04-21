package com.android.systemui.usb;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.Notification.Action;
import android.app.Notification.BigTextStyle;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.MoveCallback;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.systemui.SystemUI;

public class StorageNotification extends SystemUI {
    private final BroadcastReceiver mFinishReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            StorageNotification.this.mNotificationManager.cancelAsUser(null, 1397575510, UserHandle.ALL);
        }
    };
    private boolean mIsLastVisible = false;
    private boolean mIsUmsConnect = false;
    private final StorageEventListener mListener = new StorageEventListener() {
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            StorageNotification.this.onVolumeStateChangedInternal(vol);
        }

        public void onVolumeRecordChanged(VolumeRecord rec) {
            VolumeInfo vol = StorageNotification.this.mStorageManager.findVolumeByUuid(rec.getFsUuid());
            if (vol != null && vol.isMountedReadable()) {
                StorageNotification.this.onVolumeStateChangedInternal(vol);
            }
        }

        public void onVolumeForgotten(String fsUuid) {
            StorageNotification.this.mNotificationManager.cancelAsUser(fsUuid, 1397772886, UserHandle.ALL);
        }

        public void onDiskScanned(DiskInfo disk, int volumeCount) {
            StorageNotification.this.onDiskScannedInternal(disk, volumeCount);
        }

        public void onDiskDestroyed(DiskInfo disk) {
            StorageNotification.this.mNotificationManager.cancelAsUser(disk.getId(), 1396986699, UserHandle.ALL);
        }
    };
    private final MoveCallback mMoveCallback = new MoveCallback() {
        public void onCreated(int moveId, Bundle extras) {
            MoveInfo move = new MoveInfo();
            move.moveId = moveId;
            move.extras = extras;
            if (extras != null) {
                move.packageName = extras.getString("android.intent.extra.PACKAGE_NAME");
                move.label = extras.getString("android.intent.extra.TITLE");
                move.volumeUuid = extras.getString("android.os.storage.extra.FS_UUID");
            }
            StorageNotification.this.mMoves.put(moveId, move);
        }

        public void onStatusChanged(int moveId, int status, long estMillis) {
            MoveInfo move = (MoveInfo) StorageNotification.this.mMoves.get(moveId);
            if (move == null) {
                Log.w("StorageNotification", "Ignoring unknown move " + moveId);
                return;
            }
            if (PackageManager.isMoveStatusFinished(status)) {
                StorageNotification.this.onMoveFinished(move, status);
            } else {
                StorageNotification.this.onMoveProgress(move, status, estMillis);
            }
        }
    };
    private final SparseArray<MoveInfo> mMoves = new SparseArray();
    private int mNotifcationState = 0;
    private NotificationManager mNotificationManager;
    private final BroadcastReceiver mSnoozeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            StorageNotification.this.mStorageManager.setVolumeSnoozed(intent.getStringExtra("android.os.storage.extra.FS_UUID"), true);
        }
    };
    private StorageManager mStorageManager;
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            boolean booleanExtra;
            if (intent.getBooleanExtra("configured", false)) {
                booleanExtra = intent.getBooleanExtra("mass_storage", false);
            } else {
                booleanExtra = false;
            }
            Log.i("StorageNotification", "onReceive=" + intent.getAction() + ",available=" + booleanExtra);
            StorageNotification.this.onUsbMassStorageConnectionChangedAsync(booleanExtra);
        }
    };
    private Notification mUsbStorageNotification;
    private final BroadcastReceiver mUserReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                StorageNotification.this.updateUsbMassStorageNotification();
            }
        }
    };

    private static class MoveInfo {
        public Bundle extras;
        public String label;
        public int moveId;
        public String packageName;
        public String volumeUuid;

        private MoveInfo() {
        }
    }

    public void start() {
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        this.mStorageManager = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        this.mStorageManager.registerListener(this.mListener);
        this.mContext.registerReceiver(this.mSnoozeReceiver, new IntentFilter("com.android.systemui.action.SNOOZE_VOLUME"), "android.permission.MOUNT_UNMOUNT_FILESYSTEMS", null);
        this.mContext.registerReceiver(this.mFinishReceiver, new IntentFilter("com.android.systemui.action.FINISH_WIZARD"), "android.permission.MOUNT_UNMOUNT_FILESYSTEMS", null);
        this.mContext.registerReceiver(this.mUsbReceiver, new IntentFilter("android.hardware.usb.action.USB_STATE"));
        this.mContext.registerReceiver(this.mUserReceiver, new IntentFilter("android.intent.action.USER_SWITCHED"));
        for (DiskInfo disk : this.mStorageManager.getDisks()) {
            onDiskScannedInternal(disk, disk.volumeCount);
        }
        for (VolumeInfo vol : this.mStorageManager.getVolumes()) {
            onVolumeStateChangedInternal(vol);
        }
        this.mContext.getPackageManager().registerMoveCallback(this.mMoveCallback, new Handler());
        updateMissingPrivateVolumes();
    }

    private int sharableStorageNum() {
        int num = 0;
        for (VolumeInfo info : this.mStorageManager.getVolumes()) {
            if (info != null && info.isAllowUsbMassStorage(ActivityManager.getCurrentUser()) && info.getType() == 0) {
                if (info.getState() == 6 && info.getState() == 7 && info.getState() == 8) {
                    if (info.getState() != 4) {
                    }
                }
                num++;
            }
        }
        return num;
    }

    private int sharedStorageNum() {
        int num = 0;
        for (VolumeInfo info : this.mStorageManager.getVolumes()) {
            if (info != null && info.getState() == 9 && info.getType() == 0) {
                num++;
            }
        }
        return num;
    }

    private void onUsbMassStorageConnectionChangedAsync(boolean connected) {
        this.mIsUmsConnect = connected;
        updateUsbMassStorageNotification();
    }

    void updateUsbMassStorageNotification() {
        int canSharedNum = sharableStorageNum();
        int sharedNum = sharedStorageNum();
        Log.d("StorageNotification", "updateUsbMassStorageNotification - canSharedNum=" + canSharedNum + ",sharedNum=" + sharedNum + ",mIsUmsConnect=" + this.mIsUmsConnect + ",mNotifcationState=" + this.mNotifcationState);
        Intent intent;
        if (this.mIsUmsConnect && canSharedNum > 0 && this.mNotifcationState != 1) {
            Log.d("StorageNotification", "updateUsbMassStorageNotification - Turn on noti.");
            intent = new Intent();
            intent.setClass(this.mContext, UsbStorageActivity.class);
            intent.setFlags(268435456);
            setUsbStorageNotification(17040333, 17040334, 17303148, false, true, PendingIntent.getActivityAsUser(this.mContext, 0, intent, 0, null, UserHandle.CURRENT));
            this.mNotifcationState = 1;
        } else if (this.mIsUmsConnect && sharedNum > 0 && this.mNotifcationState != 2) {
            Log.d("StorageNotification", "updateUsbMassStorageNotification - Turn off noti.");
            intent = new Intent();
            intent.setClass(this.mContext, UsbStorageActivity.class);
            setUsbStorageNotification(17040335, 17040336, 17301642, false, true, PendingIntent.getActivityAsUser(this.mContext, 0, intent, 0, null, UserHandle.CURRENT));
            this.mNotifcationState = 2;
        } else if ((!this.mIsUmsConnect || canSharedNum == 0) && this.mNotifcationState != 0) {
            Log.d("StorageNotification", "updateUsbMassStorageNotification - Cancel noti.");
            setUsbStorageNotification(0, 0, 0, false, false, null);
            this.mNotifcationState = 0;
        } else {
            Log.d("StorageNotification", "updateUsbMassStorageNotification - What?");
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized void setUsbStorageNotification(int titleId, int messageId, int icon, boolean sound, boolean visible, PendingIntent pi) {
        Log.d("StorageNotification", "setUsbStorageNotification visible=" + visible + ",mIsLastVisible=" + this.mIsLastVisible);
        if (visible || this.mUsbStorageNotification != null) {
            NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
            if (notificationManager != null) {
                if (visible) {
                    Resources r = Resources.getSystem();
                    CharSequence title = r.getText(titleId);
                    CharSequence message = r.getText(messageId);
                    if (this.mUsbStorageNotification == null) {
                        this.mUsbStorageNotification = new Notification();
                        this.mUsbStorageNotification.icon = icon;
                        this.mUsbStorageNotification.when = 0;
                        this.mUsbStorageNotification.priority = -2;
                    }
                    Notification notification;
                    if (sound) {
                        notification = this.mUsbStorageNotification;
                        notification.defaults |= 1;
                    } else {
                        notification = this.mUsbStorageNotification;
                        notification.defaults &= -2;
                    }
                    this.mUsbStorageNotification.flags = 2;
                    this.mUsbStorageNotification.tickerText = title;
                    if (pi == null) {
                        pi = PendingIntent.getBroadcastAsUser(this.mContext, 0, new Intent(), 0, UserHandle.CURRENT);
                    }
                    this.mUsbStorageNotification.color = this.mContext.getResources().getColor(17170521);
                    this.mUsbStorageNotification.setLatestEventInfo(this.mContext, title, message, pi);
                    this.mUsbStorageNotification.visibility = 1;
                    this.mUsbStorageNotification.category = "sys";
                }
                int notificationId = this.mUsbStorageNotification.icon;
                if (visible) {
                    notificationManager.notifyAsUser(null, notificationId, this.mUsbStorageNotification, UserHandle.ALL);
                    this.mIsLastVisible = true;
                } else {
                    notificationManager.cancelAsUser(null, notificationId, UserHandle.ALL);
                    this.mIsLastVisible = false;
                }
            }
        }
    }

    private void updateMissingPrivateVolumes() {
        for (VolumeRecord rec : this.mStorageManager.getVolumeRecords()) {
            if (rec.getType() == 1) {
                String fsUuid = rec.getFsUuid();
                VolumeInfo info = this.mStorageManager.findVolumeByUuid(fsUuid);
                if ((info == null || !info.isMountedWritable()) && !rec.isSnoozed()) {
                    CharSequence title = this.mContext.getString(17040380, new Object[]{rec.getNickname()});
                    CharSequence text = this.mContext.getString(17040381);
                    this.mNotificationManager.notifyAsUser(fsUuid, 1397772886, new Builder(this.mContext).setSmallIcon(17302555).setColor(this.mContext.getColor(17170521)).setContentTitle(title).setContentText(text).setContentIntent(buildForgetPendingIntent(rec)).setStyle(new BigTextStyle().bigText(text)).setVisibility(1).setLocalOnly(true).setCategory("sys").setDeleteIntent(buildSnoozeIntent(fsUuid)).build(), UserHandle.ALL);
                } else {
                    this.mNotificationManager.cancelAsUser(fsUuid, 1397772886, UserHandle.ALL);
                }
            }
        }
    }

    private void onDiskScannedInternal(DiskInfo disk, int volumeCount) {
        if (volumeCount != 0 || disk.size <= 0) {
            this.mNotificationManager.cancelAsUser(disk.getId(), 1396986699, UserHandle.ALL);
            return;
        }
        CharSequence title = this.mContext.getString(17040369, new Object[]{disk.getDescription()});
        CharSequence text = this.mContext.getString(17040370, new Object[]{disk.getDescription()});
        this.mNotificationManager.notifyAsUser(disk.getId(), 1396986699, new Builder(this.mContext).setSmallIcon(getSmallIcon(disk, 6)).setColor(this.mContext.getColor(17170521)).setContentTitle(title).setContentText(text).setContentIntent(buildInitPendingIntent(disk)).setStyle(new BigTextStyle().bigText(text)).setVisibility(1).setLocalOnly(true).setCategory("err").build(), UserHandle.ALL);
    }

    private void onVolumeStateChangedInternal(VolumeInfo vol) {
        switch (vol.getType()) {
            case 0:
                onPublicVolumeStateChangedInternal(vol);
                return;
            case 1:
                onPrivateVolumeStateChangedInternal(vol);
                return;
            default:
                return;
        }
    }

    private void onPrivateVolumeStateChangedInternal(VolumeInfo vol) {
        Log.d("StorageNotification", "Notifying about private volume: " + vol.toString());
        updateMissingPrivateVolumes();
    }

    private void onPublicVolumeStateChangedInternal(VolumeInfo vol) {
        Notification notif;
        Log.d("StorageNotification", "Notifying about public volume: " + vol.toString());
        switch (vol.getState()) {
            case 0:
                notif = onVolumeUnmounted(vol);
                break;
            case 1:
                notif = onVolumeChecking(vol);
                break;
            case 2:
            case 3:
                notif = onVolumeMounted(vol);
                break;
            case 4:
                notif = onVolumeFormatting(vol);
                break;
            case 5:
                notif = onVolumeEjecting(vol);
                break;
            case 6:
                notif = onVolumeUnmountable(vol);
                break;
            case 7:
                notif = onVolumeRemoved(vol);
                break;
            case 8:
                notif = onVolumeBadRemoval(vol);
                break;
            case 9:
                notif = null;
                break;
            default:
                notif = null;
                break;
        }
        updateUsbMassStorageNotification();
        if (notif != null) {
            this.mNotificationManager.notifyAsUser(vol.getId(), 1397773634, notif, UserHandle.ALL);
        } else {
            this.mNotificationManager.cancelAsUser(vol.getId(), 1397773634, UserHandle.ALL);
        }
    }

    private Notification onVolumeUnmounted(VolumeInfo vol) {
        return null;
    }

    private Notification onVolumeChecking(VolumeInfo vol) {
        DiskInfo disk = vol.getDisk();
        return buildNotificationBuilder(vol, this.mContext.getString(17040363, new Object[]{disk.getDescription()}), this.mContext.getString(17040364, new Object[]{disk.getDescription()})).setCategory("progress").setPriority(-1).setOngoing(true).build();
    }

    private Notification onVolumeMounted(VolumeInfo vol) {
        VolumeRecord rec = this.mStorageManager.findRecordByUuid(vol.getFsUuid());
        DiskInfo disk = vol.getDisk();
        if (rec.isSnoozed() && disk.isAdoptable()) {
            return null;
        }
        if (!disk.isAdoptable() || rec.isInited()) {
            CharSequence title = disk.getDescription();
            CharSequence text = this.mContext.getString(17040366, new Object[]{disk.getDescription()});
            PendingIntent browseIntent = buildBrowsePendingIntent(vol);
            Builder builder = buildNotificationBuilder(vol, title, text).addAction(new Action(17302346, this.mContext.getString(17040379), browseIntent)).addAction(new Action(17302334, this.mContext.getString(17040378), buildUnmountPendingIntent(vol))).setContentIntent(browseIntent).setCategory("sys").setPriority(-1);
            if (disk.isAdoptable()) {
                builder.setDeleteIntent(buildSnoozeIntent(vol.getFsUuid()));
            }
            return builder.build();
        }
        title = disk.getDescription();
        text = this.mContext.getString(17040365, new Object[]{disk.getDescription()});
        PendingIntent initIntent = buildInitPendingIntent(vol);
        return buildNotificationBuilder(vol, title, text).addAction(new Action(17302561, this.mContext.getString(17040377), initIntent)).addAction(new Action(17302334, this.mContext.getString(17040378), buildUnmountPendingIntent(vol))).setContentIntent(initIntent).setDeleteIntent(buildSnoozeIntent(vol.getFsUuid())).setCategory("sys").build();
    }

    private Notification onVolumeFormatting(VolumeInfo vol) {
        return null;
    }

    private Notification onVolumeEjecting(VolumeInfo vol) {
        DiskInfo disk = vol.getDisk();
        return buildNotificationBuilder(vol, this.mContext.getString(17040375, new Object[]{disk.getDescription()}), this.mContext.getString(17040376, new Object[]{disk.getDescription()})).setCategory("progress").setPriority(-1).setOngoing(true).build();
    }

    private Notification onVolumeUnmountable(VolumeInfo vol) {
        DiskInfo disk = vol.getDisk();
        return buildNotificationBuilder(vol, this.mContext.getString(17040367, new Object[]{disk.getDescription()}), this.mContext.getString(17040368, new Object[]{disk.getDescription()})).setContentIntent(buildInitPendingIntent(vol)).setCategory("err").build();
    }

    private Notification onVolumeRemoved(VolumeInfo vol) {
        if (!vol.isPrimary()) {
            return null;
        }
        DiskInfo disk = vol.getDisk();
        return buildNotificationBuilder(vol, this.mContext.getString(17040373, new Object[]{disk.getDescription()}), this.mContext.getString(17040374, new Object[]{disk.getDescription()})).setCategory("err").build();
    }

    private Notification onVolumeBadRemoval(VolumeInfo vol) {
        if (!vol.isPrimary()) {
            return null;
        }
        DiskInfo disk = vol.getDisk();
        return buildNotificationBuilder(vol, this.mContext.getString(17040371, new Object[]{disk.getDescription()}), this.mContext.getString(17040372, new Object[]{disk.getDescription()})).setCategory("err").build();
    }

    private void onMoveProgress(MoveInfo move, int status, long estMillis) {
        CharSequence title;
        CharSequence charSequence;
        PendingIntent intent;
        if (TextUtils.isEmpty(move.label)) {
            title = this.mContext.getString(17040383);
        } else {
            title = this.mContext.getString(17040382, new Object[]{move.label});
        }
        if (estMillis < 0) {
            charSequence = null;
        } else {
            charSequence = DateUtils.formatDuration(estMillis);
        }
        if (move.packageName != null) {
            intent = buildWizardMovePendingIntent(move);
        } else {
            intent = buildWizardMigratePendingIntent(move);
        }
        if (intent != null) {
            this.mNotificationManager.notifyAsUser(move.packageName, 1397575510, new Builder(this.mContext).setSmallIcon(17302555).setColor(this.mContext.getColor(17170521)).setContentTitle(title).setContentText(charSequence).setContentIntent(intent).setStyle(new BigTextStyle().bigText(charSequence)).setVisibility(1).setLocalOnly(true).setCategory("progress").setPriority(-1).setProgress(100, status, false).setOngoing(true).build(), UserHandle.ALL);
        }
    }

    private void onMoveFinished(MoveInfo move, int status) {
        if (move.packageName != null) {
            this.mNotificationManager.cancelAsUser(move.packageName, 1397575510, UserHandle.ALL);
            return;
        }
        CharSequence title;
        CharSequence text;
        PendingIntent buildWizardReadyPendingIntent;
        VolumeInfo privateVol = this.mContext.getPackageManager().getPrimaryStorageCurrentVolume();
        String descrip = this.mStorageManager.getBestVolumeDescription(privateVol);
        if (status == -100) {
            title = this.mContext.getString(17040384);
            text = this.mContext.getString(17040385, new Object[]{descrip});
        } else {
            title = this.mContext.getString(17040386);
            text = this.mContext.getString(17040387);
        }
        if (privateVol != null && privateVol.getDisk() != null) {
            buildWizardReadyPendingIntent = buildWizardReadyPendingIntent(privateVol.getDisk());
        } else if (privateVol != null) {
            buildWizardReadyPendingIntent = buildVolumeSettingsPendingIntent(privateVol);
        } else {
            buildWizardReadyPendingIntent = null;
        }
        this.mNotificationManager.notifyAsUser(move.packageName, 1397575510, new Builder(this.mContext).setSmallIcon(17302555).setColor(this.mContext.getColor(17170521)).setContentTitle(title).setContentText(text).setContentIntent(buildWizardReadyPendingIntent).setStyle(new BigTextStyle().bigText(text)).setVisibility(1).setLocalOnly(true).setCategory("sys").setPriority(-1).setAutoCancel(true).build(), UserHandle.ALL);
    }

    private int getSmallIcon(DiskInfo disk, int state) {
        if (disk.isSd()) {
            switch (state) {
                case 1:
                case 5:
                    return 17302555;
                default:
                    return 17302555;
            }
        } else if (disk.isUsb()) {
            return 17302577;
        } else {
            return 17302555;
        }
    }

    private Builder buildNotificationBuilder(VolumeInfo vol, CharSequence title, CharSequence text) {
        return new Builder(this.mContext).setSmallIcon(getSmallIcon(vol.getDisk(), vol.getState())).setColor(this.mContext.getColor(17170521)).setContentTitle(title).setContentText(text).setStyle(new BigTextStyle().bigText(text)).setVisibility(1).setLocalOnly(true);
    }

    private PendingIntent buildInitPendingIntent(DiskInfo disk) {
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageWizardInit");
        intent.putExtra("android.os.storage.extra.DISK_ID", disk.getId());
        return PendingIntent.getActivityAsUser(this.mContext, disk.getId().hashCode(), intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildInitPendingIntent(VolumeInfo vol) {
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageWizardInit");
        intent.putExtra("android.os.storage.extra.VOLUME_ID", vol.getId());
        return PendingIntent.getActivityAsUser(this.mContext, vol.getId().hashCode(), intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildUnmountPendingIntent(VolumeInfo vol) {
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageUnmountReceiver");
        intent.putExtra("android.os.storage.extra.VOLUME_ID", vol.getId());
        return PendingIntent.getBroadcastAsUser(this.mContext, vol.getId().hashCode(), intent, 268435456, UserHandle.CURRENT);
    }

    private PendingIntent buildBrowsePendingIntent(VolumeInfo vol) {
        Intent intent = vol.buildBrowseIntent();
        return PendingIntent.getActivityAsUser(this.mContext, vol.getId().hashCode(), intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildVolumeSettingsPendingIntent(VolumeInfo vol) {
        Intent intent = new Intent();
        switch (vol.getType()) {
            case 0:
                intent.setClassName("com.android.settings", "com.android.settings.Settings$PublicVolumeSettingsActivity");
                break;
            case 1:
                intent.setClassName("com.android.settings", "com.android.settings.Settings$PrivateVolumeSettingsActivity");
                break;
            default:
                return null;
        }
        intent.putExtra("android.os.storage.extra.VOLUME_ID", vol.getId());
        return PendingIntent.getActivityAsUser(this.mContext, vol.getId().hashCode(), intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildSnoozeIntent(String fsUuid) {
        Intent intent = new Intent("com.android.systemui.action.SNOOZE_VOLUME");
        intent.putExtra("android.os.storage.extra.FS_UUID", fsUuid);
        return PendingIntent.getBroadcastAsUser(this.mContext, fsUuid.hashCode(), intent, 268435456, UserHandle.CURRENT);
    }

    private PendingIntent buildForgetPendingIntent(VolumeRecord rec) {
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.Settings$PrivateVolumeForgetActivity");
        intent.putExtra("android.os.storage.extra.FS_UUID", rec.getFsUuid());
        return PendingIntent.getActivityAsUser(this.mContext, rec.getFsUuid().hashCode(), intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildWizardMigratePendingIntent(MoveInfo move) {
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageWizardMigrateProgress");
        intent.putExtra("android.content.pm.extra.MOVE_ID", move.moveId);
        VolumeInfo vol = this.mStorageManager.findVolumeByQualifiedUuid(move.volumeUuid);
        if (vol == null) {
            Log.d("StorageNotification", "build migration intent failed because volume is null.");
            return null;
        }
        intent.putExtra("android.os.storage.extra.VOLUME_ID", vol.getId());
        return PendingIntent.getActivityAsUser(this.mContext, move.moveId, intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildWizardMovePendingIntent(MoveInfo move) {
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageWizardMoveProgress");
        intent.putExtra("android.content.pm.extra.MOVE_ID", move.moveId);
        return PendingIntent.getActivityAsUser(this.mContext, move.moveId, intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildWizardReadyPendingIntent(DiskInfo disk) {
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageWizardReady");
        intent.putExtra("android.os.storage.extra.DISK_ID", disk.getId());
        return PendingIntent.getActivityAsUser(this.mContext, disk.getId().hashCode(), intent, 268435456, null, UserHandle.CURRENT);
    }
}
