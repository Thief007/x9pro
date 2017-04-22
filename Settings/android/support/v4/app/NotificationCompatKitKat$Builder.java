package android.support.v4.app;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.SparseArray;
import android.widget.RemoteViews;
import java.util.ArrayList;
import java.util.List;

public class NotificationCompatKitKat$Builder implements NotificationBuilderWithBuilderAccessor, NotificationBuilderWithActions {
    private Builder f4b;
    private List<Bundle> mActionExtrasList = new ArrayList();
    private Bundle mExtras;

    public NotificationCompatKitKat$Builder(Context context, Notification n, CharSequence contentTitle, CharSequence contentText, CharSequence contentInfo, RemoteViews tickerView, int number, PendingIntent contentIntent, PendingIntent fullScreenIntent, Bitmap largeIcon, int progressMax, int progress, boolean progressIndeterminate, boolean showWhen, boolean useChronometer, int priority, CharSequence subText, boolean localOnly, ArrayList<String> people, Bundle extras, String groupKey, boolean groupSummary, String sortKey) {
        boolean z;
        Builder lights = new Builder(context).setWhen(n.when).setShowWhen(showWhen).setSmallIcon(n.icon, n.iconLevel).setContent(n.contentView).setTicker(n.tickerText, tickerView).setSound(n.sound, n.audioStreamType).setVibrate(n.vibrate).setLights(n.ledARGB, n.ledOnMS, n.ledOffMS);
        if ((n.flags & 2) != 0) {
            z = true;
        } else {
            z = false;
        }
        lights = lights.setOngoing(z);
        if ((n.flags & 8) != 0) {
            z = true;
        } else {
            z = false;
        }
        lights = lights.setOnlyAlertOnce(z);
        if ((n.flags & 16) != 0) {
            z = true;
        } else {
            z = false;
        }
        lights = lights.setAutoCancel(z).setDefaults(n.defaults).setContentTitle(contentTitle).setContentText(contentText).setSubText(subText).setContentInfo(contentInfo).setContentIntent(contentIntent).setDeleteIntent(n.deleteIntent);
        if ((n.flags & 128) != 0) {
            z = true;
        } else {
            z = false;
        }
        this.f4b = lights.setFullScreenIntent(fullScreenIntent, z).setLargeIcon(largeIcon).setNumber(number).setUsesChronometer(useChronometer).setPriority(priority).setProgress(progressMax, progress, progressIndeterminate);
        this.mExtras = new Bundle();
        if (extras != null) {
            this.mExtras.putAll(extras);
        }
        if (!(people == null || people.isEmpty())) {
            this.mExtras.putStringArray("android.people", (String[]) people.toArray(new String[people.size()]));
        }
        if (localOnly) {
            this.mExtras.putBoolean("android.support.localOnly", true);
        }
        if (groupKey != null) {
            this.mExtras.putString("android.support.groupKey", groupKey);
            if (groupSummary) {
                this.mExtras.putBoolean("android.support.isGroupSummary", true);
            } else {
                this.mExtras.putBoolean("android.support.useSideChannel", true);
            }
        }
        if (sortKey != null) {
            this.mExtras.putString("android.support.sortKey", sortKey);
        }
    }

    public void addAction(NotificationCompatBase$Action action) {
        this.mActionExtrasList.add(NotificationCompatJellybean.writeActionAndGetExtras(this.f4b, action));
    }

    public Builder getBuilder() {
        return this.f4b;
    }

    public Notification build() {
        SparseArray<Bundle> actionExtrasMap = NotificationCompatJellybean.buildActionExtrasMap(this.mActionExtrasList);
        if (actionExtrasMap != null) {
            this.mExtras.putSparseParcelableArray("android.support.actionExtras", actionExtrasMap);
        }
        this.f4b.setExtras(this.mExtras);
        return this.f4b.build();
    }
}
