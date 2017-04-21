package com.android.systemui.recents;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;

public class RecentsAppWidgetHost extends AppWidgetHost {
    RecentsAppWidgetHostCallbacks mCb;
    boolean mIsListening;

    interface RecentsAppWidgetHostCallbacks {
        void refreshSearchWidgetView();
    }

    public RecentsAppWidgetHost(Context context, int hostId) {
        super(context, hostId);
    }

    public void startListening(RecentsAppWidgetHostCallbacks cb) {
        this.mCb = cb;
        if (!this.mIsListening) {
            this.mIsListening = true;
            super.startListening();
        }
    }

    public void stopListening() {
        if (this.mIsListening) {
            super.stopListening();
        }
        this.mCb = null;
        this.mIsListening = false;
    }

    protected AppWidgetHostView onCreateView(Context context, int appWidgetId, AppWidgetProviderInfo appWidget) {
        return new RecentsAppWidgetHostView(context);
    }

    protected void onProviderChanged(int appWidgetId, AppWidgetProviderInfo appWidgetInfo) {
        super.onProviderChanged(appWidgetId, appWidgetInfo);
        if (this.mIsListening && this.mCb != null) {
            this.mCb.refreshSearchWidgetView();
        }
    }
}
