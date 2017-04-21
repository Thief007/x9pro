package com.android.systemui.statusbar.phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import com.android.systemui.DejankUtils;
import com.android.systemui.R;

public class PhoneStatusBarView extends PanelBar {
    PhoneStatusBar mBar;
    private final PhoneStatusBarTransitions mBarTransitions;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("broad.hide.systemui.superscreenshot")) {
                PhoneStatusBarView.this.mBar.makeExpandedInvisible();
            }
        }
    };
    private Runnable mHideExpandedRunnable = new Runnable() {
        public void run() {
            PhoneStatusBarView.this.mBar.makeExpandedInvisible();
        }
    };
    PanelView mLastFullyOpenedPanel = null;
    private float mMinFraction;
    PanelView mNotificationPanel;
    private float mPanelFraction;
    private ScrimController mScrimController;

    public PhoneStatusBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources res = getContext().getResources();
        this.mBarTransitions = new PhoneStatusBarTransitions(this);
        registerReceiver();
    }

    private void registerReceiver() {
        IntentFilter myIntentFilter = new IntentFilter();
        myIntentFilter.addAction("broad.hide.systemui.superscreenshot");
        getContext().registerReceiver(this.mBroadcastReceiver, myIntentFilter);
    }

    public BarTransitions getBarTransitions() {
        return this.mBarTransitions;
    }

    public void setBar(PhoneStatusBar bar) {
        this.mBar = bar;
    }

    public void setScrimController(ScrimController scrimController) {
        this.mScrimController = scrimController;
    }

    public void onFinishInflate() {
        this.mBarTransitions.init();
    }

    public void addPanel(PanelView pv) {
        super.addPanel(pv);
        if (pv.getId() == R.id.notification_panel) {
            this.mNotificationPanel = pv;
        }
    }

    public boolean panelsEnabled() {
        return this.mBar.panelsEnabled();
    }

    public boolean onRequestSendAccessibilityEventInternal(View child, AccessibilityEvent event) {
        if (!super.onRequestSendAccessibilityEventInternal(child, event)) {
            return false;
        }
        AccessibilityEvent record = AccessibilityEvent.obtain();
        onInitializeAccessibilityEvent(record);
        dispatchPopulateAccessibilityEvent(record);
        event.appendRecord(record);
        return true;
    }

    public PanelView selectPanelForTouch(MotionEvent touch) {
        if (this.mNotificationPanel.getExpandedHeight() > 0.0f) {
            return null;
        }
        return this.mNotificationPanel;
    }

    public void onPanelPeeked() {
        super.onPanelPeeked();
        this.mBar.makeExpandedVisible(false);
    }

    public void onAllPanelsCollapsed() {
        super.onAllPanelsCollapsed();
        DejankUtils.postAfterTraversal(this.mHideExpandedRunnable);
        this.mLastFullyOpenedPanel = null;
    }

    public void removePendingHideExpandedRunnables() {
        DejankUtils.removeCallbacks(this.mHideExpandedRunnable);
    }

    public void onPanelFullyOpened(PanelView openPanel) {
        super.onPanelFullyOpened(openPanel);
        if (openPanel != this.mLastFullyOpenedPanel) {
            openPanel.sendAccessibilityEvent(32);
        }
        this.mLastFullyOpenedPanel = openPanel;
    }

    public boolean onTouchEvent(MotionEvent event) {
        return !this.mBar.interceptTouchEvent(event) ? super.onTouchEvent(event) : true;
    }

    public void onTrackingStarted(PanelView panel) {
        super.onTrackingStarted(panel);
        this.mBar.onTrackingStarted();
        this.mScrimController.onTrackingStarted();
    }

    public void onClosingFinished() {
        super.onClosingFinished();
        this.mBar.onClosingFinished();
    }

    public void onTrackingStopped(PanelView panel, boolean expand) {
        super.onTrackingStopped(panel, expand);
        this.mBar.onTrackingStopped(expand);
    }

    public void onExpandingFinished() {
        super.onExpandingFinished();
        this.mScrimController.onExpandingFinished();
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        return !this.mBar.interceptTouchEvent(event) ? super.onInterceptTouchEvent(event) : true;
    }

    public void panelScrimMinFractionChanged(float minFraction) {
        if (this.mMinFraction != minFraction) {
            this.mMinFraction = minFraction;
            updateScrimFraction();
        }
    }

    public void panelExpansionChanged(PanelView panel, float frac, boolean expanded) {
        super.panelExpansionChanged(panel, frac, expanded);
        this.mPanelFraction = frac;
        updateScrimFraction();
    }

    private void updateScrimFraction() {
        this.mScrimController.setPanelExpansion(Math.max(this.mPanelFraction - (this.mMinFraction / (1.0f - this.mMinFraction)), 0.0f));
    }
}
