package android.support.v4.app;

import android.app.PendingIntent;
import android.os.Bundle;

public abstract class NotificationCompatBase$Action {

    public interface Factory {
    }

    public abstract PendingIntent getActionIntent();

    public abstract Bundle getExtras();

    public abstract int getIcon();

    public abstract RemoteInputCompatBase$RemoteInput[] getRemoteInputs();

    public abstract CharSequence getTitle();
}
