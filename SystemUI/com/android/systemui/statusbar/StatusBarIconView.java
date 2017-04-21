package com.android.systemui.statusbar;

import android.app.Notification;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewDebug.ExportedProperty;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView.ScaleType;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.R;
import java.text.NumberFormat;

public class StatusBarIconView extends AnimatedImageView {
    private final boolean mBlocked;
    private StatusBarIcon mIcon;
    private Notification mNotification;
    private Drawable mNumberBackground;
    private Paint mNumberPain;
    private String mNumberText;
    private int mNumberX;
    private int mNumberY;
    @ExportedProperty
    private String mSlot;

    public StatusBarIconView(Context context, String slot, Notification notification) {
        this(context, slot, notification, false);
    }

    public StatusBarIconView(Context context, String slot, Notification notification, boolean blocked) {
        super(context);
        Resources res = context.getResources();
        this.mBlocked = blocked;
        this.mSlot = slot;
        this.mNumberPain = new Paint();
        this.mNumberPain.setTextAlign(Align.CENTER);
        this.mNumberPain.setColor(context.getColor(R.drawable.notification_number_text_color));
        this.mNumberPain.setAntiAlias(true);
        setNotification(notification);
        if (notification != null) {
            float scale = ((float) res.getDimensionPixelSize(R.dimen.status_bar_icon_drawing_size)) / ((float) res.getDimensionPixelSize(R.dimen.status_bar_icon_size));
            setScaleX(scale);
            setScaleY(scale);
        }
        setScaleType(ScaleType.CENTER);
    }

    public void setNotification(Notification notification) {
        this.mNotification = notification;
        setContentDescription(notification);
    }

    public StatusBarIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mBlocked = false;
        Resources res = context.getResources();
        float scale = ((float) res.getDimensionPixelSize(R.dimen.status_bar_icon_drawing_size)) / ((float) res.getDimensionPixelSize(R.dimen.status_bar_icon_size));
        setScaleX(scale);
        setScaleY(scale);
    }

    public boolean equalIcons(Icon a, Icon b) {
        boolean z = true;
        if (a == b) {
            return true;
        }
        if (a.getType() != b.getType()) {
            return false;
        }
        switch (a.getType()) {
            case 2:
                if (!(a.getResPackage().equals(b.getResPackage()) && a.getResId() == b.getResId())) {
                    z = false;
                }
                return z;
            case 4:
                return a.getUriString().equals(b.getUriString());
            default:
                return false;
        }
    }

    public boolean set(StatusBarIcon icon) {
        int i = 0;
        boolean equalIcons = this.mIcon != null ? equalIcons(this.mIcon.icon, icon.icon) : false;
        boolean levelEquals = equalIcons ? this.mIcon.iconLevel == icon.iconLevel : false;
        boolean visibilityEquals = this.mIcon != null ? this.mIcon.visible == icon.visible : false;
        boolean numberEquals = this.mIcon != null ? this.mIcon.number == icon.number : false;
        this.mIcon = icon.clone();
        setContentDescription(icon.contentDescription);
        if (!equalIcons && !updateDrawable(false)) {
            return false;
        }
        if (!levelEquals) {
            setImageLevel(icon.iconLevel);
        }
        if (!numberEquals) {
            if (icon.number <= 0 || !getContext().getResources().getBoolean(R.bool.config_statusBarShowNumber)) {
                this.mNumberBackground = null;
                this.mNumberText = null;
            } else {
                if (this.mNumberBackground == null) {
                    this.mNumberBackground = getContext().getResources().getDrawable(R.drawable.ic_notification_overlay);
                }
                placeNumber();
            }
            invalidate();
        }
        if (!visibilityEquals) {
            if (!icon.visible || this.mBlocked) {
                i = 8;
            }
            setVisibility(i);
        }
        return true;
    }

    public void updateDrawable() {
        updateDrawable(true);
    }

    private boolean updateDrawable(boolean withClear) {
        if (this.mIcon == null) {
            return false;
        }
        Drawable drawable = getIcon(this.mIcon);
        if (drawable == null) {
            Log.w("StatusBarIconView", "No icon for slot " + this.mSlot);
            return false;
        }
        if (withClear) {
            setImageDrawable(null);
        }
        setImageDrawable(drawable);
        return true;
    }

    private Drawable getIcon(StatusBarIcon icon) {
        return getIcon(getContext(), icon);
    }

    public static Drawable getIcon(Context context, StatusBarIcon icon) {
        int userId = icon.user.getIdentifier();
        if (userId == -1) {
            userId = 0;
        }
        return icon.icon.loadDrawableAsUser(context, userId);
    }

    public StatusBarIcon getStatusBarIcon() {
        return this.mIcon;
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        if (this.mNotification != null) {
            event.setParcelableData(this.mNotification);
        }
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (this.mNumberBackground != null) {
            placeNumber();
        }
    }

    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        updateDrawable();
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mNumberBackground != null) {
            this.mNumberBackground.draw(canvas);
            canvas.drawText(this.mNumberText, (float) this.mNumberX, (float) this.mNumberY, this.mNumberPain);
        }
    }

    protected void debug(int depth) {
        super.debug(depth);
        Log.d("View", debugIndent(depth) + "slot=" + this.mSlot);
        Log.d("View", debugIndent(depth) + "icon=" + this.mIcon);
    }

    void placeNumber() {
        String str;
        if (this.mIcon.number > getContext().getResources().getInteger(17694723)) {
            str = getContext().getResources().getString(17039383);
        } else {
            str = NumberFormat.getIntegerInstance().format((long) this.mIcon.number);
        }
        this.mNumberText = str;
        int w = getWidth();
        int h = getHeight();
        Rect r = new Rect();
        this.mNumberPain.getTextBounds(str, 0, str.length(), r);
        int tw = r.right - r.left;
        int th = r.bottom - r.top;
        this.mNumberBackground.getPadding(r);
        int dw = (r.left + tw) + r.right;
        if (dw < this.mNumberBackground.getMinimumWidth()) {
            dw = this.mNumberBackground.getMinimumWidth();
        }
        this.mNumberX = (w - r.right) - (((dw - r.right) - r.left) / 2);
        int dh = (r.top + th) + r.bottom;
        if (dh < this.mNumberBackground.getMinimumWidth()) {
            dh = this.mNumberBackground.getMinimumWidth();
        }
        this.mNumberY = (h - r.bottom) - ((((dh - r.top) - th) - r.bottom) / 2);
        this.mNumberBackground.setBounds(w - dw, h - dh, w, h);
    }

    private void setContentDescription(Notification notification) {
        if (notification != null) {
            CharSequence tickerText = notification.tickerText;
            if (!TextUtils.isEmpty(tickerText)) {
                setContentDescription(tickerText);
            }
        }
    }

    public String toString() {
        return "StatusBarIconView(slot=" + this.mSlot + " icon=" + this.mIcon + " notification=" + this.mNotification + ")";
    }

    public String getSlot() {
        return this.mSlot;
    }
}
