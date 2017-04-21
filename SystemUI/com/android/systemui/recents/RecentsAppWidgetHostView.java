package com.android.systemui.recents;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.view.View;
import android.widget.RemoteViews;

public class RecentsAppWidgetHostView extends AppWidgetHostView {
    private Context mContext;
    private int mPreviousOrientation;

    public RecentsAppWidgetHostView(Context context) {
        super(context);
        this.mContext = context;
    }

    public void updateAppWidget(RemoteViews remoteViews) {
        updateLastInflationOrientation();
        super.updateAppWidget(remoteViews);
    }

    protected View getErrorView() {
        return new View(this.mContext);
    }

    private void updateLastInflationOrientation() {
        this.mPreviousOrientation = this.mContext.getResources().getConfiguration().orientation;
    }

    public boolean isReinflateRequired() {
        if (this.mPreviousOrientation != this.mContext.getResources().getConfiguration().orientation) {
            return true;
        }
        return false;
    }
}
