package com.android.systemui.statusbar;

import android.content.Context;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import com.android.systemui.R;

public class NotificationContentView extends FrameLayout {
    private boolean mAnimate;
    private final Rect mClipBounds = new Rect();
    private int mClipTopAmount;
    private int mContentHeight;
    private View mContractedChild;
    private NotificationViewWrapper mContractedWrapper;
    private boolean mDark;
    private final OnPreDrawListener mEnableAnimationPredrawListener = new OnPreDrawListener() {
        public boolean onPreDraw() {
            NotificationContentView.this.mAnimate = true;
            NotificationContentView.this.getViewTreeObserver().removeOnPreDrawListener(this);
            return true;
        }
    };
    private View mExpandedChild;
    private NotificationViewWrapper mExpandedWrapper;
    private final Paint mFadePaint = new Paint();
    private View mHeadsUpChild;
    private final int mHeadsUpHeight;
    private NotificationViewWrapper mHeadsUpWrapper;
    private boolean mIsHeadsUp;
    private final Interpolator mLinearInterpolator = new LinearInterpolator();
    private final ViewOutlineProvider mOutlineProvider = new ViewOutlineProvider() {
        public void getOutline(View view, Outline outline) {
            outline.setRoundRect(0, 0, view.getWidth(), NotificationContentView.this.mUnrestrictedContentHeight, (float) NotificationContentView.this.mRoundRectRadius);
        }
    };
    private final boolean mRoundRectClippingEnabled;
    private final int mRoundRectRadius;
    private boolean mShowingLegacyBackground;
    private final int mSmallHeight;
    private int mUnrestrictedContentHeight;
    private int mVisibleType = 0;

    public NotificationContentView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mFadePaint.setXfermode(new PorterDuffXfermode(Mode.ADD));
        this.mSmallHeight = getResources().getDimensionPixelSize(R.dimen.notification_min_height);
        this.mHeadsUpHeight = getResources().getDimensionPixelSize(R.dimen.notification_mid_height);
        this.mRoundRectRadius = getResources().getDimensionPixelSize(R.dimen.notification_material_rounded_rect_radius);
        this.mRoundRectClippingEnabled = getResources().getBoolean(R.bool.config_notifications_round_rect_clipping);
        reset(true);
        setOutlineProvider(this.mOutlineProvider);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size;
        LayoutParams layoutParams;
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        boolean hasFixedHeight = heightMode == 1073741824;
        boolean isHeightLimited = heightMode == Integer.MIN_VALUE;
        int maxSize = Integer.MAX_VALUE;
        if (hasFixedHeight || isHeightLimited) {
            maxSize = MeasureSpec.getSize(heightMeasureSpec);
        }
        int maxChildHeight = 0;
        if (this.mContractedChild != null) {
            this.mContractedChild.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(Math.min(maxSize, this.mSmallHeight), 1073741824));
            maxChildHeight = Math.max(0, this.mContractedChild.getMeasuredHeight());
        }
        if (this.mExpandedChild != null) {
            int spec;
            size = maxSize;
            layoutParams = this.mExpandedChild.getLayoutParams();
            if (layoutParams.height >= 0) {
                size = Math.min(maxSize, layoutParams.height);
            }
            if (size == Integer.MAX_VALUE) {
                spec = MeasureSpec.makeMeasureSpec(0, 0);
            } else {
                spec = MeasureSpec.makeMeasureSpec(size, Integer.MIN_VALUE);
            }
            this.mExpandedChild.measure(widthMeasureSpec, spec);
            maxChildHeight = Math.max(maxChildHeight, this.mExpandedChild.getMeasuredHeight());
        }
        if (this.mHeadsUpChild != null) {
            size = Math.min(maxSize, this.mHeadsUpHeight);
            layoutParams = this.mHeadsUpChild.getLayoutParams();
            if (layoutParams.height >= 0) {
                size = Math.min(maxSize, layoutParams.height);
            }
            this.mHeadsUpChild.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(size, Integer.MIN_VALUE));
            maxChildHeight = Math.max(maxChildHeight, this.mHeadsUpChild.getMeasuredHeight());
        }
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), Math.min(maxChildHeight, maxSize));
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateClipping();
        invalidateOutline();
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateVisibility();
    }

    public void reset(boolean resetActualHeight) {
        if (this.mContractedChild != null) {
            this.mContractedChild.animate().cancel();
        }
        if (this.mExpandedChild != null) {
            this.mExpandedChild.animate().cancel();
        }
        if (this.mHeadsUpChild != null) {
            this.mHeadsUpChild.animate().cancel();
        }
        removeAllViews();
        this.mContractedChild = null;
        this.mExpandedChild = null;
        this.mHeadsUpChild = null;
        this.mVisibleType = 0;
        if (resetActualHeight) {
            this.mContentHeight = this.mSmallHeight;
        }
    }

    public View getContractedChild() {
        return this.mContractedChild;
    }

    public View getExpandedChild() {
        return this.mExpandedChild;
    }

    public View getHeadsUpChild() {
        return this.mHeadsUpChild;
    }

    public void setContractedChild(View child) {
        if (this.mContractedChild != null) {
            this.mContractedChild.animate().cancel();
            removeView(this.mContractedChild);
        }
        addView(child);
        this.mContractedChild = child;
        this.mContractedWrapper = NotificationViewWrapper.wrap(getContext(), child);
        selectLayout(false, true);
        this.mContractedWrapper.setDark(this.mDark, false, 0);
        updateRoundRectClipping();
    }

    public void setExpandedChild(View child) {
        if (this.mExpandedChild != null) {
            this.mExpandedChild.animate().cancel();
            removeView(this.mExpandedChild);
        }
        addView(child);
        this.mExpandedChild = child;
        this.mExpandedWrapper = NotificationViewWrapper.wrap(getContext(), child);
        selectLayout(false, true);
        updateRoundRectClipping();
    }

    public void setHeadsUpChild(View child) {
        if (this.mHeadsUpChild != null) {
            this.mHeadsUpChild.animate().cancel();
            removeView(this.mHeadsUpChild);
        }
        addView(child);
        this.mHeadsUpChild = child;
        this.mHeadsUpWrapper = NotificationViewWrapper.wrap(getContext(), child);
        selectLayout(false, true);
        updateRoundRectClipping();
    }

    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        updateVisibility();
    }

    private void updateVisibility() {
        setVisible(isShown());
    }

    private void setVisible(boolean isVisible) {
        if (isVisible) {
            getViewTreeObserver().addOnPreDrawListener(this.mEnableAnimationPredrawListener);
            return;
        }
        getViewTreeObserver().removeOnPreDrawListener(this.mEnableAnimationPredrawListener);
        this.mAnimate = false;
    }

    public void setContentHeight(int contentHeight) {
        this.mContentHeight = Math.max(Math.min(contentHeight, getHeight()), getMinHeight());
        this.mUnrestrictedContentHeight = Math.max(contentHeight, getMinHeight());
        selectLayout(this.mAnimate, false);
        updateClipping();
        invalidateOutline();
    }

    public int getContentHeight() {
        return this.mContentHeight;
    }

    public int getMaxHeight() {
        if (this.mIsHeadsUp && this.mHeadsUpChild != null) {
            return this.mHeadsUpChild.getHeight();
        }
        if (this.mExpandedChild != null) {
            return this.mExpandedChild.getHeight();
        }
        return this.mSmallHeight;
    }

    public int getMinHeight() {
        return this.mSmallHeight;
    }

    public void setClipTopAmount(int clipTopAmount) {
        this.mClipTopAmount = clipTopAmount;
        updateClipping();
    }

    private void updateRoundRectClipping() {
        setClipToOutline(needsRoundRectClipping());
    }

    private boolean needsRoundRectClipping() {
        if (!this.mRoundRectClippingEnabled) {
            return false;
        }
        boolean needsForContracted;
        boolean needsForHeadsUp;
        if (this.mContractedChild == null || this.mContractedChild.getVisibility() != 0) {
            needsForContracted = false;
        } else {
            needsForContracted = this.mContractedWrapper.needsRoundRectClipping();
        }
        boolean needsForExpanded;
        if (this.mExpandedChild == null || this.mExpandedChild.getVisibility() != 0) {
            needsForExpanded = false;
        } else {
            needsForExpanded = this.mExpandedWrapper.needsRoundRectClipping();
        }
        if (this.mExpandedChild == null || this.mExpandedChild.getVisibility() != 0) {
            needsForHeadsUp = false;
        } else {
            needsForHeadsUp = this.mExpandedWrapper.needsRoundRectClipping();
        }
        if (needsForContracted || r1) {
            needsForHeadsUp = true;
        }
        return needsForHeadsUp;
    }

    private void updateClipping() {
        this.mClipBounds.set(0, this.mClipTopAmount, getWidth(), this.mContentHeight);
        setClipBounds(this.mClipBounds);
    }

    private void selectLayout(boolean animate, boolean force) {
        if (this.mContractedChild != null) {
            int visibleType = calculateVisibleType();
            if (visibleType != this.mVisibleType || force) {
                if (!animate || ((visibleType != 1 || this.mExpandedChild == null) && ((visibleType != 2 || this.mHeadsUpChild == null) && visibleType != 0))) {
                    updateViewVisibilities(visibleType);
                } else {
                    runSwitchAnimation(visibleType);
                }
                this.mVisibleType = visibleType;
            }
        }
    }

    private void updateViewVisibilities(int visibleType) {
        int i;
        float f;
        int i2 = 4;
        float f2 = 1.0f;
        boolean contractedVisible = visibleType == 0;
        View view = this.mContractedChild;
        if (contractedVisible) {
            i = 0;
        } else {
            i = 4;
        }
        view.setVisibility(i);
        view = this.mContractedChild;
        if (contractedVisible) {
            f = 1.0f;
        } else {
            f = 0.0f;
        }
        view.setAlpha(f);
        this.mContractedChild.setLayerType(0, null);
        if (this.mExpandedChild != null) {
            boolean expandedVisible = visibleType == 1;
            view = this.mExpandedChild;
            if (expandedVisible) {
                i = 0;
            } else {
                i = 4;
            }
            view.setVisibility(i);
            view = this.mExpandedChild;
            if (expandedVisible) {
                f = 1.0f;
            } else {
                f = 0.0f;
            }
            view.setAlpha(f);
            this.mExpandedChild.setLayerType(0, null);
        }
        if (this.mHeadsUpChild != null) {
            boolean headsUpVisible = visibleType == 2;
            View view2 = this.mHeadsUpChild;
            if (headsUpVisible) {
                i2 = 0;
            }
            view2.setVisibility(i2);
            view2 = this.mHeadsUpChild;
            if (!headsUpVisible) {
                f2 = 0.0f;
            }
            view2.setAlpha(f2);
            this.mHeadsUpChild.setLayerType(0, null);
        }
        setLayerType(0, null);
        updateRoundRectClipping();
    }

    private void runSwitchAnimation(int visibleType) {
        View shownView = getViewForVisibleType(visibleType);
        View hiddenView = getViewForVisibleType(this.mVisibleType);
        shownView.setVisibility(0);
        hiddenView.setVisibility(0);
        shownView.setLayerType(2, this.mFadePaint);
        hiddenView.setLayerType(2, this.mFadePaint);
        setLayerType(2, null);
        hiddenView.animate().alpha(0.0f).setDuration(170).setInterpolator(this.mLinearInterpolator).withEndAction(null);
        shownView.animate().alpha(1.0f).setDuration(170).setInterpolator(this.mLinearInterpolator).withEndAction(new Runnable() {
            public void run() {
                NotificationContentView.this.updateViewVisibilities(NotificationContentView.this.mVisibleType);
            }
        });
        updateRoundRectClipping();
    }

    private View getViewForVisibleType(int visibleType) {
        switch (visibleType) {
            case 1:
                return this.mExpandedChild;
            case 2:
                return this.mHeadsUpChild;
            default:
                return this.mContractedChild;
        }
    }

    private int calculateVisibleType() {
        boolean noExpandedChild = this.mExpandedChild == null;
        if (!this.mIsHeadsUp || this.mHeadsUpChild == null) {
            if (this.mContentHeight <= this.mSmallHeight || noExpandedChild) {
                return 0;
            }
            return 1;
        } else if (this.mContentHeight <= this.mHeadsUpChild.getHeight() || noExpandedChild) {
            return 2;
        } else {
            return 1;
        }
    }

    public void notifyContentUpdated() {
        selectLayout(false, true);
        if (this.mContractedChild != null) {
            this.mContractedWrapper.notifyContentUpdated();
            this.mContractedWrapper.setDark(this.mDark, false, 0);
        }
        if (this.mExpandedChild != null) {
            this.mExpandedWrapper.notifyContentUpdated();
        }
        updateRoundRectClipping();
    }

    public boolean isContentExpandable() {
        return this.mExpandedChild != null;
    }

    public void setDark(boolean dark, boolean fade, long delay) {
        boolean z = false;
        if (this.mDark != dark && this.mContractedChild != null) {
            this.mDark = dark;
            NotificationViewWrapper notificationViewWrapper = this.mContractedWrapper;
            if (dark && !this.mShowingLegacyBackground) {
                z = true;
            }
            notificationViewWrapper.setDark(z, fade, delay);
        }
    }

    public void setHeadsUp(boolean headsUp) {
        this.mIsHeadsUp = headsUp;
        selectLayout(false, true);
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public void setShowingLegacyBackground(boolean showing) {
        this.mShowingLegacyBackground = showing;
    }
}
