package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.media.session.MediaSessionLegacyHelper;
import android.os.IBinder;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewRootImpl;
import android.view.WindowManagerGlobal;
import android.widget.FrameLayout;
import com.android.systemui.R;
import com.android.systemui.R$styleable;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.DragDownHelper;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;

public class StatusBarWindowView extends FrameLayout {
    public static final boolean DEBUG = BaseStatusBar.DEBUG;
    private View mBrightnessMirror;
    private DragDownHelper mDragDownHelper;
    private NotificationPanelView mNotificationPanel;
    private int mRightInset = 0;
    private PhoneStatusBar mService;
    private NotificationStackScrollLayout mStackScrollLayout;
    private final Paint mTransparentSrcPaint = new Paint();

    public class LayoutParams extends android.widget.FrameLayout.LayoutParams {
        public boolean ignoreRightInset;

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, R$styleable.StatusBarWindowView_Layout);
            this.ignoreRightInset = a.getBoolean(0, false);
            a.recycle();
        }
    }

    public StatusBarWindowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setMotionEventSplittingEnabled(false);
        this.mTransparentSrcPaint.setColor(0);
        this.mTransparentSrcPaint.setXfermode(new PorterDuffXfermode(Mode.SRC));
    }

    protected boolean fitSystemWindows(Rect insets) {
        if (getFitsSystemWindows()) {
            boolean paddingChanged = (insets.left == getPaddingLeft() && insets.top == getPaddingTop()) ? insets.bottom != getPaddingBottom() : true;
            if (insets.right != this.mRightInset) {
                this.mRightInset = insets.right;
                applyMargins();
            }
            if (paddingChanged) {
                setPadding(insets.left, 0, 0, 0);
            }
            insets.left = 0;
            insets.top = 0;
            insets.right = 0;
        } else {
            if (this.mRightInset != 0) {
                this.mRightInset = 0;
                applyMargins();
            }
            boolean changed = (getPaddingLeft() == 0 && getPaddingRight() == 0 && getPaddingTop() == 0) ? getPaddingBottom() != 0 : true;
            if (changed) {
                setPadding(0, 0, 0, 0);
            }
            insets.top = 0;
        }
        return false;
    }

    private void applyMargins() {
        int N = getChildCount();
        for (int i = 0; i < N; i++) {
            View child = getChildAt(i);
            if (child.getLayoutParams() instanceof LayoutParams) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (!(lp.ignoreRightInset || lp.rightMargin == this.mRightInset)) {
                    lp.rightMargin = this.mRightInset;
                    child.requestLayout();
                }
            }
        }
    }

    public android.widget.FrameLayout.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    protected android.widget.FrameLayout.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-1, -1);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mStackScrollLayout = (NotificationStackScrollLayout) findViewById(R.id.notification_stack_scroller);
        this.mNotificationPanel = (NotificationPanelView) findViewById(R.id.notification_panel);
        this.mBrightnessMirror = findViewById(R.id.brightness_mirror);
    }

    public void setService(PhoneStatusBar service) {
        this.mService = service;
        this.mDragDownHelper = new DragDownHelper(getContext(), this, this.mStackScrollLayout, this.mService);
    }

    protected void onAttachedToWindow() {
        boolean z = false;
        super.onAttachedToWindow();
        ViewRootImpl root = getViewRootImpl();
        if (root != null) {
            root.setDrawDuringWindowsAnimating(true);
        }
        if (this.mService.isScrimSrcModeEnabled()) {
            IBinder windowToken = getWindowToken();
            android.view.WindowManager.LayoutParams lp = (android.view.WindowManager.LayoutParams) getLayoutParams();
            lp.token = windowToken;
            setLayoutParams(lp);
            WindowManagerGlobal.getInstance().changeCanvasOpacity(windowToken, true);
            setWillNotDraw(false);
            return;
        }
        if (!DEBUG) {
            z = true;
        }
        setWillNotDraw(z);
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean down = event.getAction() == 0;
        switch (event.getKeyCode()) {
            case 4:
                if (!down) {
                    this.mService.onBackPressed();
                }
                return true;
            case 24:
            case 25:
                if (this.mService.isDozing()) {
                    MediaSessionLegacyHelper.getHelper(this.mContext).sendVolumeKeyEvent(event, true);
                    return true;
                }
                break;
            case 62:
                break;
            case 82:
                if (!down) {
                    return this.mService.onMenuPressed();
                }
                break;
        }
        if (!down) {
            return this.mService.onSpacePressed();
        }
        if (this.mService.interceptMediaKey(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (this.mBrightnessMirror != null && this.mBrightnessMirror.getVisibility() == 0 && ev.getActionMasked() == 5) {
            return false;
        }
        return super.dispatchTouchEvent(ev);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercept = false;
        if (this.mNotificationPanel.isFullyExpanded() && this.mStackScrollLayout.getVisibility() == 0 && this.mService.getBarState() == 1 && !this.mService.isBouncerShowing()) {
            intercept = this.mDragDownHelper.onInterceptTouchEvent(ev);
            if (ev.getActionMasked() == 0) {
                this.mService.wakeUpIfDozing(ev.getEventTime(), ev);
            }
        }
        if (!intercept) {
            super.onInterceptTouchEvent(ev);
        }
        if (intercept) {
            MotionEvent cancellation = MotionEvent.obtain(ev);
            cancellation.setAction(3);
            this.mStackScrollLayout.onInterceptTouchEvent(cancellation);
            this.mNotificationPanel.onInterceptTouchEvent(cancellation);
            cancellation.recycle();
        }
        return intercept;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        boolean handled = false;
        if (this.mService.getBarState() == 1) {
            handled = this.mDragDownHelper.onTouchEvent(ev);
        }
        if (!handled) {
            handled = super.onTouchEvent(ev);
        }
        int action = ev.getAction();
        if (!handled && (action == 1 || action == 3)) {
            this.mService.setInteracting(1, false);
        }
        return handled;
    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mService.isScrimSrcModeEnabled()) {
            int paddedBottom = getHeight() - getPaddingBottom();
            int paddedRight = getWidth() - getPaddingRight();
            if (getPaddingTop() != 0) {
                canvas.drawRect(0.0f, 0.0f, (float) getWidth(), (float) getPaddingTop(), this.mTransparentSrcPaint);
            }
            if (getPaddingBottom() != 0) {
                canvas.drawRect(0.0f, (float) paddedBottom, (float) getWidth(), (float) getHeight(), this.mTransparentSrcPaint);
            }
            if (getPaddingLeft() != 0) {
                canvas.drawRect(0.0f, (float) getPaddingTop(), (float) getPaddingLeft(), (float) paddedBottom, this.mTransparentSrcPaint);
            }
            if (getPaddingRight() != 0) {
                canvas.drawRect((float) paddedRight, (float) getPaddingTop(), (float) getWidth(), (float) paddedBottom, this.mTransparentSrcPaint);
            }
        }
        if (DEBUG) {
            Paint pt = new Paint();
            pt.setColor(-2130706688);
            pt.setStrokeWidth(12.0f);
            pt.setStyle(Style.STROKE);
            canvas.drawRect(0.0f, 0.0f, (float) canvas.getWidth(), (float) canvas.getHeight(), pt);
        }
    }

    public void cancelExpandHelper() {
        if (this.mStackScrollLayout != null) {
            this.mStackScrollLayout.cancelExpandHelper();
        }
    }
}
