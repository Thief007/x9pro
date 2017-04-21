package com.mediatek.keyguard.PowerOffAlarm;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class MediatekZeroTopPaddingTextView extends TextView {
    private static final Typeface SAN_SERIF_BOLD = Typeface.create("san-serif", 1);
    private int mPaddingRight;

    public MediatekZeroTopPaddingTextView(Context context) {
        this(context, null);
    }

    public MediatekZeroTopPaddingTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MediatekZeroTopPaddingTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mPaddingRight = 0;
        setIncludeFontPadding(false);
        updatePadding();
    }

    public void updatePadding() {
        if (getTypeface().equals(SAN_SERIF_BOLD)) {
        }
        setPadding(0, 0, this.mPaddingRight, 0);
    }
}
