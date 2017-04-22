package com.mediatek.audioprofile;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.UserHandle;
import android.util.Log;
import android.widget.RemoteViews;
import com.android.settings.R;

public class LosslessReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService("notification");
        String action = intent.getAction();
        Intent audioProfileIntent = new Intent();
        audioProfileIntent.setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$SoundEnhancementActivity"));
        if (SoundEnhancement.LOSSLESS_CLOSE.equals(action)) {
            Log.d("@M_LosslessReceiver", "close the lossless.");
            cancelNotification(mNotificationManager, R.drawable.bt_audio);
        } else if (SoundEnhancement.LOSSLESS_PLAYING.equals(action)) {
            Log.d("@M_LosslessReceiver", "playing the lossless.");
            createNotification(mNotificationManager, R.drawable.bt_audio_play, audioProfileIntent, context, true, R.string.lossless_playing);
        } else if (SoundEnhancement.LOSSLESS_ADD.equals(action)) {
            Log.d("@M_LosslessReceiver", "open the lossless.");
            createNotification(mNotificationManager, R.drawable.bt_audio, audioProfileIntent, context, false, R.string.lossless_on);
        } else if (SoundEnhancement.LOSSLESS_STOP.equals(action)) {
            Log.d("@M_LosslessReceiver", "stop the lossless.");
            createNotification(mNotificationManager, R.drawable.bt_audio, audioProfileIntent, context, false, R.string.lossless_volume_max);
        } else if (SoundEnhancement.CLOSE_LOSSLESS_NOTIFICATION.equals(action)) {
            Log.d("@M_LosslessReceiver", "close the notification lossless.");
            ((AudioManager) context.getSystemService("audio")).setParameters(SoundEnhancement.SET_LOSSLESSBT_DISABLED);
        } else if (SoundEnhancement.LOSSLESS_NOT_SUPPORT.equals(action)) {
            Log.d("@M_LosslessReceiver", "cannot found the lossless device.");
            createNotification(mNotificationManager, R.drawable.bt_audio, audioProfileIntent, context, false, R.string.lossless_cannot_found_device);
        }
    }

    private void createNotification(NotificationManager mNotificationManager, int icon, Intent audioProfileIntent, Context context, boolean iconChange, int textId) {
        PendingIntent pendingIntent = PendingIntent.getActivityAsUser(context, 0, audioProfileIntent, 268435456, null, UserHandle.CURRENT);
        PendingIntent deleteIntent = PendingIntent.getBroadcast(context, 0, new Intent(SoundEnhancement.CLOSE_LOSSLESS_NOTIFICATION), 0);
        Builder builder = new Builder(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.lossless_notification);
        Notification notification = new Notification();
        notification.contentView = views;
        notification.icon = icon;
        notification.contentIntent = pendingIntent;
        notification.deleteIntent = deleteIntent;
        if (iconChange) {
            views.setImageViewResource(R.id.icon, R.drawable.bt_audio_play);
        } else {
            views.setImageViewResource(R.id.icon, R.drawable.bt_audio);
        }
        views.setTextViewText(R.id.text, context.getResources().getText(textId));
        installNotification(mNotificationManager, R.drawable.bt_audio, notification);
    }

    private void installNotification(NotificationManager mNotificationManager, int notificationId, Notification n) {
        mNotificationManager.notifyAsUser("Lossless_notification", notificationId, n, UserHandle.CURRENT);
    }

    private void cancelNotification(NotificationManager mNotificationManager, int id) {
        mNotificationManager.cancelAsUser("Lossless_notification", id, UserHandle.CURRENT);
    }
}
