package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;

public class QSDetailItems extends FrameLayout {
    private static final boolean DEBUG = Log.isLoggable("QSDetailItems", 3);
    private Callback mCallback;
    private final Context mContext;
    private View mEmpty;
    private ImageView mEmptyIcon;
    private TextView mEmptyText;
    private final H mHandler = new H();
    private LinearLayout mItems;
    private boolean mItemsVisible = true;
    private int mMaxItems;
    private View mMinHeightSpacer;
    private String mTag;

    public interface Callback {
        void onDetailItemClick(Item item);

        void onDetailItemDisconnect(Item item);
    }

    private class H extends Handler {
        public H() {
            super(Looper.getMainLooper());
        }

        public void handleMessage(Message msg) {
            boolean z = true;
            if (msg.what == 1) {
                QSDetailItems.this.handleSetItems((Item[]) msg.obj);
            } else if (msg.what == 2) {
                QSDetailItems.this.handleSetCallback((Callback) msg.obj);
            } else if (msg.what == 3) {
                QSDetailItems qSDetailItems = QSDetailItems.this;
                if (msg.arg1 == 0) {
                    z = false;
                }
                qSDetailItems.handleSetItemsVisible(z);
            }
        }
    }

    public static class Item {
        public boolean canDisconnect;
        public int icon;
        public CharSequence line1;
        public CharSequence line2;
        public Drawable overlay;
        public Object tag;
    }

    public QSDetailItems(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        this.mTag = "QSDetailItems";
    }

    public static QSDetailItems convertOrInflate(Context context, View convert, ViewGroup parent) {
        if (convert instanceof QSDetailItems) {
            return (QSDetailItems) convert;
        }
        return (QSDetailItems) LayoutInflater.from(context).inflate(R.layout.qs_detail_items, parent, false);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mItems = (LinearLayout) findViewById(16908298);
        this.mItems.setVisibility(8);
        this.mEmpty = findViewById(16908292);
        this.mEmpty.setVisibility(8);
        this.mEmptyText = (TextView) this.mEmpty.findViewById(16908310);
        this.mEmptyIcon = (ImageView) this.mEmpty.findViewById(16908294);
        this.mMinHeightSpacer = findViewById(R.id.min_height_spacer);
        this.mMaxItems = getResources().getInteger(R.integer.quick_settings_detail_max_item_count);
        setMinHeightInItems(this.mMaxItems);
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        FontSizeUtils.updateFontSize(this.mEmptyText, R.dimen.qs_detail_empty_text_size);
        int count = this.mItems.getChildCount();
        for (int i = 0; i < count; i++) {
            View item = this.mItems.getChildAt(i);
            FontSizeUtils.updateFontSize(item, 16908310, R.dimen.qs_detail_item_primary_text_size);
            FontSizeUtils.updateFontSize(item, 16908304, R.dimen.qs_detail_item_secondary_text_size);
        }
    }

    public void setTagSuffix(String suffix) {
        this.mTag = "QSDetailItems." + suffix;
    }

    public void setEmptyState(int icon, int text) {
        this.mEmptyIcon.setImageResource(icon);
        this.mEmptyText.setText(text);
    }

    public void setMinHeightInItems(int minHeightInItems) {
        LayoutParams lp = this.mMinHeightSpacer.getLayoutParams();
        lp.height = getResources().getDimensionPixelSize(R.dimen.qs_detail_item_height) * minHeightInItems;
        this.mMinHeightSpacer.setLayoutParams(lp);
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (DEBUG) {
            Log.d(this.mTag, "onAttachedToWindow");
        }
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (DEBUG) {
            Log.d(this.mTag, "onDetachedFromWindow");
        }
        this.mCallback = null;
    }

    public void setCallback(Callback callback) {
        this.mHandler.removeMessages(2);
        this.mHandler.obtainMessage(2, callback).sendToTarget();
    }

    public void setItems(Item[] items) {
        this.mHandler.removeMessages(1);
        this.mHandler.obtainMessage(1, items).sendToTarget();
    }

    public void setItemsVisible(boolean visible) {
        int i;
        this.mHandler.removeMessages(3);
        H h = this.mHandler;
        if (visible) {
            i = 1;
        } else {
            i = 0;
        }
        h.obtainMessage(3, i, 0).sendToTarget();
    }

    private void handleSetCallback(Callback callback) {
        this.mCallback = callback;
    }

    private void handleSetItems(Item[] items) {
        int i;
        int i2;
        int i3 = 8;
        int itemCount = items != null ? Math.min(items.length, this.mMaxItems) : 0;
        View view = this.mEmpty;
        if (itemCount == 0) {
            i = 0;
        } else {
            i = 8;
        }
        view.setVisibility(i);
        LinearLayout linearLayout = this.mItems;
        if (itemCount != 0) {
            i3 = 0;
        }
        linearLayout.setVisibility(i3);
        for (i2 = this.mItems.getChildCount() - 1; i2 >= itemCount; i2--) {
            this.mItems.removeViewAt(i2);
        }
        for (i2 = 0; i2 < itemCount; i2++) {
            bind(items[i2], this.mItems.getChildAt(i2));
        }
    }

    private void handleSetItemsVisible(boolean visible) {
        if (this.mItemsVisible != visible) {
            this.mItemsVisible = visible;
            for (int i = 0; i < this.mItems.getChildCount(); i++) {
                this.mItems.getChildAt(i).setVisibility(this.mItemsVisible ? 0 : 4);
            }
        }
    }

    private void bind(final Item item, View view) {
        int i;
        CharSequence charSequence;
        int i2 = 0;
        if (view == null) {
            view = LayoutInflater.from(this.mContext).inflate(R.layout.qs_detail_item, this, false);
            this.mItems.addView(view);
        }
        view.setVisibility(this.mItemsVisible ? 0 : 4);
        ImageView iv = (ImageView) view.findViewById(16908294);
        iv.setImageResource(item.icon);
        iv.getOverlay().clear();
        if (item.overlay != null) {
            item.overlay.setBounds(0, 0, item.overlay.getIntrinsicWidth(), item.overlay.getIntrinsicHeight());
            iv.getOverlay().add(item.overlay);
        }
        TextView title = (TextView) view.findViewById(16908310);
        title.setText(item.line1);
        TextView summary = (TextView) view.findViewById(16908304);
        boolean twoLines = !TextUtils.isEmpty(item.line2);
        title.setMaxLines(twoLines ? 1 : 2);
        if (twoLines) {
            i = 0;
        } else {
            i = 8;
        }
        summary.setVisibility(i);
        if (twoLines) {
            charSequence = item.line2;
        } else {
            charSequence = null;
        }
        summary.setText(charSequence);
        view.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (QSDetailItems.this.mCallback != null) {
                    QSDetailItems.this.mCallback.onDetailItemClick(item);
                }
            }
        });
        ImageView disconnect = (ImageView) view.findViewById(16908296);
        if (!item.canDisconnect) {
            i2 = 8;
        }
        disconnect.setVisibility(i2);
        disconnect.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (QSDetailItems.this.mCallback != null) {
                    QSDetailItems.this.mCallback.onDetailItemDisconnect(item);
                }
            }
        });
    }
}
