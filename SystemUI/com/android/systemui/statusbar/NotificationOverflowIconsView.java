package com.android.systemui.statusbar;

import android.app.Notification;
import android.content.Context;
import android.graphics.PorterDuff.Mode;
import android.util.AttributeSet;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import com.android.internal.util.NotificationColorUtil;
import com.android.systemui.R;
import com.android.systemui.statusbar.NotificationData.Entry;
import com.android.systemui.statusbar.phone.IconMerger;

public class NotificationOverflowIconsView extends IconMerger {
    private int mIconSize;
    private TextView mMoreText;
    private NotificationColorUtil mNotificationColorUtil;
    private int mTintColor;

    public NotificationOverflowIconsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mNotificationColorUtil = NotificationColorUtil.getInstance(getContext());
        this.mTintColor = getContext().getColor(R.color.keyguard_overflow_content_color);
        this.mIconSize = getResources().getDimensionPixelSize(17104923);
    }

    public void setMoreText(TextView moreText) {
        this.mMoreText = moreText;
    }

    public void addNotification(Entry notification) {
        StatusBarIconView v = new StatusBarIconView(getContext(), "", notification.notification.getNotification());
        v.setScaleType(ScaleType.CENTER_INSIDE);
        addView(v, this.mIconSize, this.mIconSize);
        v.set(notification.icon.getStatusBarIcon());
        applyColor(notification.notification.getNotification(), v);
        updateMoreText();
    }

    private void applyColor(Notification notification, StatusBarIconView view) {
        view.setColorFilter(this.mTintColor, Mode.MULTIPLY);
    }

    private void updateMoreText() {
        this.mMoreText.setText(getResources().getString(R.string.keyguard_more_overflow_text, new Object[]{Integer.valueOf(getChildCount())}));
    }
}
