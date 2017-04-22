package com.mediatek.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import com.android.internal.util.Preconditions;
import com.android.settings.R$styleable;
import com.android.settings.widget.ChartAxis;

public class ChartBandwidthGridView extends View {
    private ChartAxis mHoriz;
    private Drawable mPrimary;
    private Drawable mSecondary;
    private ChartAxis mVert;

    public ChartBandwidthGridView(Context context) {
        this(context, null, 0);
    }

    public ChartBandwidthGridView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChartBandwidthGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setWillNotDraw(false);
        TypedArray a = context.obtainStyledAttributes(attrs, R$styleable.ChartGridView, defStyle, 0);
        this.mPrimary = a.getDrawable(2);
        this.mSecondary = a.getDrawable(3);
        a.recycle();
    }

    void init(ChartAxis horiz, ChartAxis vert) {
        this.mHoriz = (ChartAxis) Preconditions.checkNotNull(horiz, "missing horiz");
        this.mVert = (ChartAxis) Preconditions.checkNotNull(vert, "missing vert");
    }

    protected void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        Drawable secondary = this.mSecondary;
        if (secondary != null) {
            int secondaryHeight = secondary.getIntrinsicHeight();
            for (float y : this.mVert.getTickPoints()) {
                secondary.setBounds(0, (int) y, width, (int) Math.min(((float) secondaryHeight) + y, (float) height));
                secondary.draw(canvas);
            }
            secondary.setBounds(0, 0, width, secondaryHeight * 2);
            secondary.draw(canvas);
        }
        Drawable primary = this.mPrimary;
        if (primary != null) {
            int primaryWidth = primary.getIntrinsicWidth();
            for (float x : this.mHoriz.getTickPoints()) {
                primary.setBounds((int) x, 0, (int) (((float) primaryWidth) + x), height);
                primary.draw(canvas);
            }
        }
    }
}
