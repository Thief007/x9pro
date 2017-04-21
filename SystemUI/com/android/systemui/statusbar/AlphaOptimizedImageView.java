package com.android.systemui.statusbar;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.android.systemui.R$styleable;

public class AlphaOptimizedImageView extends ImageView {
    private final boolean mHasOverlappingRendering;

    public AlphaOptimizedImageView(Context context) {
        this(context, null);
    }

    public AlphaOptimizedImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AlphaOptimizedImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AlphaOptimizedImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R$styleable.AlphaOptimizedImageView, 0, 0);
        try {
            this.mHasOverlappingRendering = a.getBoolean(0, true);
        } finally {
            a.recycle();
        }
    }

    public boolean hasOverlappingRendering() {
        return this.mHasOverlappingRendering;
    }
}
