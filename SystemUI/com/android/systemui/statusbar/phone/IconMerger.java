package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import com.android.systemui.R;

public class IconMerger extends LinearLayout {
    private int mIconSize;
    private View mMoreView;

    public IconMerger(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mIconSize = context.getResources().getDimensionPixelSize(R.dimen.status_bar_icon_size);
    }

    public void setOverflowIndicator(View v) {
        this.mMoreView = v;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        setMeasuredDimension(width - (width % this.mIconSize), getMeasuredHeight());
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        checkOverflow(r - l);
    }

    private void checkOverflow(int width) {
        if (this.mMoreView != null) {
            int N = getChildCount();
            int visibleChildren = 0;
            for (int i = 0; i < N; i++) {
                if (getChildAt(i).getVisibility() != 8) {
                    visibleChildren++;
                }
            }
            boolean overflowShown = this.mMoreView.getVisibility() == 0;
            if (overflowShown) {
                visibleChildren--;
            }
            final boolean moreRequired = this.mIconSize * visibleChildren > width;
            if (moreRequired != overflowShown) {
                post(new Runnable() {
                    public void run() {
                        IconMerger.this.mMoreView.setVisibility(moreRequired ? 0 : 8);
                    }
                });
            }
        }
    }
}
