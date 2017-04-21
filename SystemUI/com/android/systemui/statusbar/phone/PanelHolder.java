package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.util.AttributeSet;
import android.util.EventLog;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class PanelHolder extends FrameLayout {
    private PanelBar mBar;
    private int mSelectedPanelIndex = -1;

    public PanelHolder(Context context, AttributeSet attrs) {
        super(context, attrs);
        setChildrenDrawingOrderEnabled(true);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        setChildrenDrawingOrderEnabled(true);
    }

    public int getPanelIndex(PanelView pv) {
        int N = getChildCount();
        for (int i = 0; i < N; i++) {
            if (pv == ((PanelView) getChildAt(i))) {
                return i;
            }
        }
        return -1;
    }

    public void setSelectedPanel(PanelView pv) {
        this.mSelectedPanelIndex = getPanelIndex(pv);
    }

    protected int getChildDrawingOrder(int childCount, int i) {
        if (this.mSelectedPanelIndex == -1) {
            return i;
        }
        if (i == childCount - 1) {
            return this.mSelectedPanelIndex;
        }
        if (i >= this.mSelectedPanelIndex) {
            return i + 1;
        }
        return i;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != 2) {
            EventLog.writeEvent(36040, new Object[]{Integer.valueOf(event.getActionMasked()), Integer.valueOf((int) event.getX()), Integer.valueOf((int) event.getY())});
        }
        return false;
    }

    public void setBar(PanelBar panelBar) {
        this.mBar = panelBar;
    }
}
