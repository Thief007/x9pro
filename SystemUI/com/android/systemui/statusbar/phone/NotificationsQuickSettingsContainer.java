package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewStub;
import android.view.ViewStub.OnInflateListener;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import com.android.systemui.R;

public class NotificationsQuickSettingsContainer extends FrameLayout implements OnInflateListener {
    private boolean mInflated;
    private View mKeyguardStatusBar;
    private boolean mQsExpanded;
    private View mScrollView;
    private View mStackScroller;
    private View mUserSwitcher;

    public NotificationsQuickSettingsContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mScrollView = findViewById(R.id.scroll_view);
        this.mStackScroller = findViewById(R.id.notification_stack_scroller);
        this.mKeyguardStatusBar = findViewById(R.id.keyguard_header);
        ViewStub userSwitcher = (ViewStub) findViewById(R.id.keyguard_user_switcher);
        userSwitcher.setOnInflateListener(this);
        this.mUserSwitcher = userSwitcher;
    }

    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
        return insets;
    }

    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean userSwitcherVisible = this.mInflated && this.mUserSwitcher.getVisibility() == 0;
        boolean statusBarVisible = this.mKeyguardStatusBar.getVisibility() == 0;
        View stackQsTop = this.mQsExpanded ? this.mStackScroller : this.mScrollView;
        View stackQsBottom = !this.mQsExpanded ? this.mStackScroller : this.mScrollView;
        if (child == this.mScrollView) {
            if (userSwitcherVisible && statusBarVisible) {
                stackQsBottom = this.mUserSwitcher;
            } else if (statusBarVisible) {
                stackQsBottom = this.mKeyguardStatusBar;
            } else if (userSwitcherVisible) {
                stackQsBottom = this.mUserSwitcher;
            }
            return super.drawChild(canvas, stackQsBottom, drawingTime);
        } else if (child == this.mStackScroller) {
            if (userSwitcherVisible && statusBarVisible) {
                stackQsTop = this.mKeyguardStatusBar;
            } else if (statusBarVisible || userSwitcherVisible) {
                stackQsTop = stackQsBottom;
            }
            return super.drawChild(canvas, stackQsTop, drawingTime);
        } else if (child == this.mUserSwitcher) {
            if (!(userSwitcherVisible && statusBarVisible)) {
                stackQsBottom = stackQsTop;
            }
            return super.drawChild(canvas, stackQsBottom, drawingTime);
        } else if (child == this.mKeyguardStatusBar) {
            return super.drawChild(canvas, stackQsTop, drawingTime);
        } else {
            return super.drawChild(canvas, child, drawingTime);
        }
    }

    public void onInflate(ViewStub stub, View inflated) {
        if (stub == this.mUserSwitcher) {
            this.mUserSwitcher = inflated;
            this.mInflated = true;
        }
    }

    public void setQsExpanded(boolean expanded) {
        if (this.mQsExpanded != expanded) {
            this.mQsExpanded = expanded;
            invalidate();
        }
    }
}
