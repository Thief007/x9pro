package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import java.util.ArrayList;

public abstract class PanelBar extends FrameLayout {
    public static final String TAG = PanelBar.class.getSimpleName();
    float mPanelExpandedFractionSum;
    PanelHolder mPanelHolder;
    ArrayList<PanelView> mPanels = new ArrayList();
    private int mState = 0;
    PanelView mTouchingPanel;
    private boolean mTracking;

    public abstract void panelScrimMinFractionChanged(float f);

    public void go(int state) {
        this.mState = state;
    }

    public PanelBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public void addPanel(PanelView pv) {
        this.mPanels.add(pv);
        pv.setBar(this);
    }

    public void setPanelHolder(PanelHolder ph) {
        if (ph == null) {
            Log.e(TAG, "setPanelHolder: null PanelHolder", new Throwable());
            return;
        }
        ph.setBar(this);
        this.mPanelHolder = ph;
        int N = ph.getChildCount();
        for (int i = 0; i < N; i++) {
            View v = ph.getChildAt(i);
            if (v != null && (v instanceof PanelView)) {
                addPanel((PanelView) v);
            }
        }
    }

    public void setBouncerShowing(boolean showing) {
        int important;
        if (showing) {
            important = 4;
        } else {
            important = 0;
        }
        setImportantForAccessibility(important);
        if (this.mPanelHolder != null) {
            this.mPanelHolder.setImportantForAccessibility(important);
        }
    }

    public PanelView selectPanelForTouch(MotionEvent touch) {
        return (PanelView) this.mPanels.get((int) ((((float) this.mPanels.size()) * touch.getX()) / ((float) getMeasuredWidth())));
    }

    public boolean panelsEnabled() {
        return true;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (panelsEnabled()) {
            boolean onTouchEvent;
            if (event.getAction() == 0) {
                PanelView panel = selectPanelForTouch(event);
                if (panel == null) {
                    Log.v(TAG, String.format("onTouch: no panel for touch at (%d,%d)", new Object[]{Integer.valueOf((int) event.getX()), Integer.valueOf((int) event.getY())}));
                    this.mTouchingPanel = null;
                    return true;
                } else if (panel.isEnabled()) {
                    startOpeningPanel(panel);
                } else {
                    Log.v(TAG, String.format("onTouch: panel (%s) is disabled, ignoring touch at (%d,%d)", new Object[]{panel, Integer.valueOf((int) event.getX()), Integer.valueOf((int) event.getY())}));
                    this.mTouchingPanel = null;
                    return true;
                }
            }
            if (this.mTouchingPanel != null) {
                onTouchEvent = this.mTouchingPanel.onTouchEvent(event);
            } else {
                onTouchEvent = true;
            }
            return onTouchEvent;
        }
        if (event.getAction() == 0) {
            Log.v(TAG, String.format("onTouch: all panels disabled, ignoring touch at (%d,%d)", new Object[]{Integer.valueOf((int) event.getX()), Integer.valueOf((int) event.getY())}));
        }
        return false;
    }

    public void startOpeningPanel(PanelView panel) {
        this.mTouchingPanel = panel;
        this.mPanelHolder.setSelectedPanel(this.mTouchingPanel);
        for (PanelView pv : this.mPanels) {
            if (pv != panel) {
                pv.collapse(false, 1.0f);
            }
        }
    }

    public void panelExpansionChanged(PanelView panel, float frac, boolean expanded) {
        boolean fullyClosed = true;
        PanelView fullyOpenedPanel = null;
        this.mPanelExpandedFractionSum = 0.0f;
        for (PanelView pv : this.mPanels) {
            pv.setVisibility(expanded ? 0 : 4);
            if (expanded) {
                if (this.mState == 0) {
                    go(1);
                    onPanelPeeked();
                }
                fullyClosed = false;
                float thisFrac = pv.getExpandedFraction();
                this.mPanelExpandedFractionSum += thisFrac;
                if (panel == pv && thisFrac == 1.0f) {
                    fullyOpenedPanel = panel;
                }
            }
        }
        this.mPanelExpandedFractionSum /= (float) this.mPanels.size();
        if (fullyOpenedPanel != null && !this.mTracking) {
            go(2);
            onPanelFullyOpened(fullyOpenedPanel);
        } else if (fullyClosed && !this.mTracking && this.mState != 0) {
            go(0);
            onAllPanelsCollapsed();
        }
    }

    public void collapseAllPanels(boolean animate, boolean delayed, float speedUpFactor) {
        boolean waiting = false;
        for (PanelView pv : this.mPanels) {
            if (!animate || pv.isFullyCollapsed()) {
                pv.resetViews();
                pv.setExpandedFraction(0.0f);
                pv.cancelPeek();
            } else {
                pv.collapse(delayed, speedUpFactor);
                waiting = true;
            }
        }
        if (!waiting && this.mState != 0) {
            go(0);
            onAllPanelsCollapsed();
        }
    }

    public void onPanelPeeked() {
    }

    public void onAllPanelsCollapsed() {
    }

    public void onPanelFullyOpened(PanelView openPanel) {
    }

    public void onTrackingStarted(PanelView panel) {
        this.mTracking = true;
    }

    public void onTrackingStopped(PanelView panel, boolean expand) {
        this.mTracking = false;
    }

    public void onExpandingFinished() {
    }

    public void onClosingFinished() {
    }
}
