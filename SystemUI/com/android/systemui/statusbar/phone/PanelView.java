package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import com.android.systemui.EventLogTags;
import com.android.systemui.R;
import com.android.systemui.assis.app.MAIN.EVENT;
import com.android.systemui.doze.DozeLog;
import com.android.systemui.statusbar.FlingAnimationUtils;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public abstract class PanelView extends FrameLayout {
    public static final String TAG = PanelView.class.getSimpleName();
    private boolean mAnimatingOnDown;
    PanelBar mBar;
    private Interpolator mBounceInterpolator;
    private boolean mClosing;
    private boolean mCollapseAfterPeek;
    private boolean mCollapsedAndHeadsUpOnDown;
    private int mEdgeTapAreaWidth;
    private float mExpandedFraction = 0.0f;
    protected float mExpandedHeight = 0.0f;
    protected boolean mExpanding;
    private Interpolator mFastOutSlowInInterpolator;
    private FlingAnimationUtils mFlingAnimationUtils;
    private final Runnable mFlingCollapseRunnable = new Runnable() {
        public void run() {
            PanelView.this.fling(0.0f, false, PanelView.this.mNextCollapseSpeedUpFactor, false);
        }
    };
    private boolean mGestureWaitForTouchSlop;
    private boolean mHasLayoutedSinceDown;
    protected HeadsUpManager mHeadsUpManager;
    private ValueAnimator mHeightAnimator;
    protected boolean mHintAnimationRunning;
    private float mHintDistance;
    private boolean mIgnoreXTouchSlop;
    private float mInitialOffsetOnTouch;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private boolean mInstantExpanding;
    private boolean mJustPeeked;
    protected KeyguardBottomAreaView mKeyguardBottomArea;
    private Interpolator mLinearOutSlowInInterpolator;
    private boolean mMotionAborted;
    private float mNextCollapseSpeedUpFactor = 1.0f;
    private boolean mOverExpandedBeforeFling;
    private boolean mPanelClosedOnDown;
    private ObjectAnimator mPeekAnimator;
    private float mPeekHeight;
    private boolean mPeekPending;
    private Runnable mPeekRunnable = new Runnable() {
        public void run() {
            PanelView.this.mPeekPending = false;
            PanelView.this.runPeekAnimation();
        }
    };
    private boolean mPeekTouching;
    protected final Runnable mPostCollapseRunnable = new Runnable() {
        public void run() {
            PanelView.this.collapse(false, 1.0f);
        }
    };
    protected PhoneStatusBar mStatusBar;
    private boolean mTouchAboveFalsingThreshold;
    private boolean mTouchDisabled;
    protected int mTouchSlop;
    private boolean mTouchSlopExceeded;
    private boolean mTouchStartedInEmptyArea;
    protected boolean mTracking;
    private int mTrackingPointer;
    private int mUnlockFalsingThreshold;
    private boolean mUpdateFlingOnLayout;
    private float mUpdateFlingVelocity;
    private boolean mUpwardsWhenTresholdReached;
    private VelocityTrackerInterface mVelocityTracker;
    private String mViewName;

    protected abstract boolean fullyExpandedClearAllVisible();

    protected abstract float getCannedFlingDurationFactor();

    protected abstract int getClearAllHeight();

    protected abstract int getMaxPanelHeight();

    protected abstract float getOverExpansionAmount();

    protected abstract float getOverExpansionPixels();

    protected abstract float getPeekHeight();

    protected abstract boolean hasConflictingGestures();

    protected abstract boolean isClearAllVisible();

    protected abstract boolean isInContentBounds(float f, float f2);

    protected abstract boolean isPanelVisibleBecauseOfHeadsUp();

    protected abstract boolean isTrackingBlocked();

    protected abstract void onHeightUpdated(float f);

    protected abstract boolean onMiddleClicked();

    public abstract void resetViews();

    protected abstract void setOverExpansion(float f, boolean z);

    protected abstract boolean shouldGestureIgnoreXTouchSlop(float f, float f2);

    protected void onExpandingFinished() {
        endClosing();
        this.mBar.onExpandingFinished();
    }

    protected void onExpandingStarted() {
    }

    private void notifyExpandingStarted() {
        if (!this.mExpanding) {
            this.mExpanding = true;
            onExpandingStarted();
        }
    }

    protected final void notifyExpandingFinished() {
        if (this.mExpanding) {
            this.mExpanding = false;
            onExpandingFinished();
        }
    }

    private void schedulePeek() {
        this.mPeekPending = true;
        postOnAnimationDelayed(this.mPeekRunnable, (long) ViewConfiguration.getTapTimeout());
        notifyBarPanelExpansionChanged();
    }

    private void runPeekAnimation() {
        this.mPeekHeight = getPeekHeight();
        if (this.mHeightAnimator == null) {
            this.mPeekAnimator = ObjectAnimator.ofFloat(this, "expandedHeight", new float[]{this.mPeekHeight}).setDuration(250);
            this.mPeekAnimator.setInterpolator(this.mLinearOutSlowInInterpolator);
            this.mPeekAnimator.addListener(new AnimatorListenerAdapter() {
                private boolean mCancelled;

                public void onAnimationCancel(Animator animation) {
                    this.mCancelled = true;
                }

                public void onAnimationEnd(Animator animation) {
                    PanelView.this.mPeekAnimator = null;
                    if (PanelView.this.mCollapseAfterPeek && !this.mCancelled) {
                        PanelView.this.postOnAnimation(PanelView.this.mPostCollapseRunnable);
                    }
                    PanelView.this.mCollapseAfterPeek = false;
                }
            });
            notifyExpandingStarted();
            this.mPeekAnimator.start();
            this.mJustPeeked = true;
        }
    }

    public PanelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mFlingAnimationUtils = new FlingAnimationUtils(context, 0.6f);
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, 17563661);
        this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, 17563662);
        this.mBounceInterpolator = new BounceInterpolator();
    }

    protected void loadDimens() {
        Resources res = getContext().getResources();
        this.mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        this.mHintDistance = res.getDimension(R.dimen.hint_move_distance);
        this.mEdgeTapAreaWidth = res.getDimensionPixelSize(R.dimen.edge_tap_area_width);
        this.mUnlockFalsingThreshold = res.getDimensionPixelSize(R.dimen.unlock_falsing_threshold);
    }

    private void trackMovement(MotionEvent event) {
        float deltaX = event.getRawX() - event.getX();
        float deltaY = event.getRawY() - event.getY();
        event.offsetLocation(deltaX, deltaY);
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.addMovement(event);
        }
        event.offsetLocation(-deltaX, -deltaY);
    }

    public void setTouchDisabled(boolean disabled) {
        this.mTouchDisabled = disabled;
    }

    public boolean onTouchEvent(MotionEvent event) {
        boolean z = true;
        boolean z2 = false;
        if (this.mInstantExpanding || this.mTouchDisabled || (this.mMotionAborted && event.getActionMasked() != 0)) {
            return false;
        }
        boolean z3;
        int pointerIndex = event.findPointerIndex(this.mTrackingPointer);
        if (pointerIndex < 0) {
            pointerIndex = 0;
            this.mTrackingPointer = event.getPointerId(0);
        }
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);
        if (event.getActionMasked() == 0) {
            this.mGestureWaitForTouchSlop = !isFullyCollapsed() ? hasConflictingGestures() : true;
            if (isFullyCollapsed()) {
                z3 = true;
            } else {
                z3 = shouldGestureIgnoreXTouchSlop(x, y);
            }
            this.mIgnoreXTouchSlop = z3;
        }
        switch (event.getActionMasked()) {
            case 0:
                startExpandMotion(x, y, false, this.mExpandedHeight);
                this.mJustPeeked = false;
                this.mPanelClosedOnDown = isFullyCollapsed();
                this.mHasLayoutedSinceDown = false;
                this.mUpdateFlingOnLayout = false;
                this.mMotionAborted = false;
                this.mPeekTouching = this.mPanelClosedOnDown;
                this.mTouchAboveFalsingThreshold = false;
                if (isFullyCollapsed()) {
                    z3 = this.mHeadsUpManager.hasPinnedHeadsUp();
                } else {
                    z3 = false;
                }
                this.mCollapsedAndHeadsUpOnDown = z3;
                if (this.mVelocityTracker == null) {
                    initVelocityTracker();
                }
                trackMovement(event);
                if (!(this.mGestureWaitForTouchSlop && ((this.mHeightAnimator == null || this.mHintAnimationRunning) && !this.mPeekPending && this.mPeekAnimator == null))) {
                    cancelHeightAnimator();
                    cancelPeek();
                    if ((this.mHeightAnimator != null && !this.mHintAnimationRunning) || this.mPeekPending) {
                        z2 = true;
                    } else if (this.mPeekAnimator != null) {
                        z2 = true;
                    }
                    this.mTouchSlopExceeded = z2;
                    onTrackingStarted();
                }
                if (isFullyCollapsed() && !this.mHeadsUpManager.hasPinnedHeadsUp()) {
                    schedulePeek();
                    break;
                }
                break;
            case 1:
            case 3:
                trackMovement(event);
                endMotionEvent(event, x, y, false);
                break;
            case 2:
                float h = y - this.mInitialTouchY;
                if (Math.abs(h) > ((float) this.mTouchSlop) && (Math.abs(h) > Math.abs(x - this.mInitialTouchX) || this.mIgnoreXTouchSlop)) {
                    this.mTouchSlopExceeded = true;
                    if (!(!this.mGestureWaitForTouchSlop || this.mTracking || this.mCollapsedAndHeadsUpOnDown)) {
                        if (!(this.mJustPeeked || this.mInitialOffsetOnTouch == 0.0f)) {
                            startExpandMotion(x, y, false, this.mExpandedHeight);
                            h = 0.0f;
                        }
                        cancelHeightAnimator();
                        removeCallbacks(this.mPeekRunnable);
                        this.mPeekPending = false;
                        onTrackingStarted();
                    }
                }
                float newHeight = Math.max(0.0f, this.mInitialOffsetOnTouch + h);
                if (newHeight > this.mPeekHeight) {
                    if (this.mPeekAnimator != null) {
                        this.mPeekAnimator.cancel();
                    }
                    this.mJustPeeked = false;
                }
                if ((-h) >= ((float) getFalsingThreshold())) {
                    this.mTouchAboveFalsingThreshold = true;
                    this.mUpwardsWhenTresholdReached = isDirectionUpwards(x, y);
                }
                if (!this.mJustPeeked && ((!this.mGestureWaitForTouchSlop || this.mTracking) && !isTrackingBlocked())) {
                    setExpandedHeightInternal(newHeight);
                }
                trackMovement(event);
                break;
            case 5:
                if (this.mStatusBar.getBarState() == 1) {
                    this.mMotionAborted = true;
                    endMotionEvent(event, x, y, true);
                    return false;
                }
                break;
            case 6:
                int upPointer = event.getPointerId(event.getActionIndex());
                if (this.mTrackingPointer == upPointer) {
                    int newIndex = event.getPointerId(0) != upPointer ? 0 : 1;
                    float newY = event.getY(newIndex);
                    float newX = event.getX(newIndex);
                    this.mTrackingPointer = event.getPointerId(newIndex);
                    startExpandMotion(newX, newY, true, this.mExpandedHeight);
                    break;
                }
                break;
        }
        if (this.mGestureWaitForTouchSlop) {
            z = this.mTracking;
        }
        return z;
    }

    private boolean isDirectionUpwards(float x, float y) {
        boolean z = false;
        float xDiff = x - this.mInitialTouchX;
        float yDiff = y - this.mInitialTouchY;
        if (yDiff >= 0.0f) {
            return false;
        }
        if (Math.abs(yDiff) >= Math.abs(xDiff)) {
            z = true;
        }
        return z;
    }

    protected void startExpandMotion(float newX, float newY, boolean startTracking, float expandedHeight) {
        this.mInitialOffsetOnTouch = expandedHeight;
        this.mInitialTouchY = newY;
        this.mInitialTouchX = newX;
        if (startTracking) {
            this.mTouchSlopExceeded = true;
            onTrackingStarted();
        }
    }

    private void endMotionEvent(MotionEvent event, float x, float y, boolean forceCancel) {
        this.mTrackingPointer = -1;
        if ((this.mTracking && this.mTouchSlopExceeded) || Math.abs(x - this.mInitialTouchX) > ((float) this.mTouchSlop) || Math.abs(y - this.mInitialTouchY) > ((float) this.mTouchSlop) || event.getActionMasked() == 3 || forceCancel) {
            boolean z;
            float vel = 0.0f;
            float vectorVel = 0.0f;
            if (this.mVelocityTracker != null) {
                this.mVelocityTracker.computeCurrentVelocity(EVENT.DYNAMIC_PACK_EVENT_BASE);
                vel = this.mVelocityTracker.getYVelocity();
                vectorVel = (float) Math.hypot((double) this.mVelocityTracker.getXVelocity(), (double) this.mVelocityTracker.getYVelocity());
            }
            if (flingExpands(vel, vectorVel, x, y) || event.getActionMasked() == 3) {
                z = true;
            } else {
                z = forceCancel;
            }
            DozeLog.traceFling(z, this.mTouchAboveFalsingThreshold, this.mStatusBar.isFalsingThresholdNeeded(), this.mStatusBar.isWakeUpComingFromTouch());
            if (!z && this.mStatusBar.getBarState() == 1) {
                float displayDensity = this.mStatusBar.getDisplayDensity();
                EventLogTags.writeSysuiLockscreenGesture(1, (int) Math.abs((y - this.mInitialTouchY) / displayDensity), (int) Math.abs(vel / displayDensity));
            }
            fling(vel, z, isFalseTouch(x, y));
            onTrackingStopped(z);
            boolean z2 = z && this.mPanelClosedOnDown && !this.mHasLayoutedSinceDown;
            this.mUpdateFlingOnLayout = z2;
            if (this.mUpdateFlingOnLayout) {
                this.mUpdateFlingVelocity = vel;
            }
        } else {
            onTrackingStopped(onEmptySpaceClick(this.mInitialTouchX));
        }
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
        this.mPeekTouching = false;
    }

    private int getFalsingThreshold() {
        return (int) (((float) this.mUnlockFalsingThreshold) * (this.mStatusBar.isWakeUpComingFromTouch() ? 1.5f : 1.0f));
    }

    protected void onTrackingStopped(boolean expand) {
        this.mTracking = false;
        this.mBar.onTrackingStopped(this, expand);
        notifyBarPanelExpansionChanged();
    }

    protected void onTrackingStarted() {
        endClosing();
        this.mTracking = true;
        this.mCollapseAfterPeek = false;
        this.mBar.onTrackingStarted(this);
        notifyExpandingStarted();
        notifyBarPanelExpansionChanged();
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean z = true;
        if (this.mInstantExpanding || (this.mMotionAborted && event.getActionMasked() != 0)) {
            return false;
        }
        int pointerIndex = event.findPointerIndex(this.mTrackingPointer);
        if (pointerIndex < 0) {
            pointerIndex = 0;
            this.mTrackingPointer = event.getPointerId(0);
        }
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);
        boolean scrolledToBottom = isScrolledToBottom();
        switch (event.getActionMasked()) {
            case 0:
                this.mStatusBar.userActivity();
                this.mAnimatingOnDown = this.mHeightAnimator != null;
                if ((!this.mAnimatingOnDown || !this.mClosing || this.mHintAnimationRunning) && !this.mPeekPending && this.mPeekAnimator == null) {
                    this.mInitialTouchY = y;
                    this.mInitialTouchX = x;
                    if (isInContentBounds(x, y)) {
                        z = false;
                    }
                    this.mTouchStartedInEmptyArea = z;
                    this.mTouchSlopExceeded = false;
                    this.mJustPeeked = false;
                    this.mMotionAborted = false;
                    this.mPanelClosedOnDown = isFullyCollapsed();
                    this.mCollapsedAndHeadsUpOnDown = false;
                    this.mHasLayoutedSinceDown = false;
                    this.mUpdateFlingOnLayout = false;
                    this.mTouchAboveFalsingThreshold = false;
                    initVelocityTracker();
                    trackMovement(event);
                    break;
                }
                cancelHeightAnimator();
                cancelPeek();
                this.mTouchSlopExceeded = true;
                return true;
                break;
            case 1:
            case 3:
                if (this.mVelocityTracker != null) {
                    this.mVelocityTracker.recycle();
                    this.mVelocityTracker = null;
                    break;
                }
                break;
            case 2:
                float h = y - this.mInitialTouchY;
                trackMovement(event);
                if (scrolledToBottom || this.mTouchStartedInEmptyArea || this.mAnimatingOnDown) {
                    float hAbs = Math.abs(h);
                    if ((h < ((float) (-this.mTouchSlop)) || (this.mAnimatingOnDown && hAbs > ((float) this.mTouchSlop))) && hAbs > Math.abs(x - this.mInitialTouchX)) {
                        cancelHeightAnimator();
                        startExpandMotion(x, y, true, this.mExpandedHeight);
                        return true;
                    }
                }
            case 5:
                if (this.mStatusBar.getBarState() == 1) {
                    this.mMotionAborted = true;
                    if (this.mVelocityTracker != null) {
                        this.mVelocityTracker.recycle();
                        this.mVelocityTracker = null;
                        break;
                    }
                }
                break;
            case 6:
                int upPointer = event.getPointerId(event.getActionIndex());
                if (this.mTrackingPointer == upPointer) {
                    int newIndex = event.getPointerId(0) != upPointer ? 0 : 1;
                    this.mTrackingPointer = event.getPointerId(newIndex);
                    this.mInitialTouchX = event.getX(newIndex);
                    this.mInitialTouchY = event.getY(newIndex);
                    break;
                }
                break;
        }
        return false;
    }

    protected void cancelHeightAnimator() {
        if (this.mHeightAnimator != null) {
            this.mHeightAnimator.cancel();
        }
        endClosing();
    }

    private void endClosing() {
        if (this.mClosing) {
            this.mClosing = false;
            onClosingFinished();
        }
    }

    private void initVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
        }
        this.mVelocityTracker = VelocityTrackerFactory.obtain(getContext());
    }

    protected boolean isScrolledToBottom() {
        return true;
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        loadDimens();
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        loadDimens();
    }

    protected boolean flingExpands(float vel, float vectorVel, float x, float y) {
        boolean z = true;
        if (isFalseTouch(x, y)) {
            return true;
        }
        if (Math.abs(vectorVel) < this.mFlingAnimationUtils.getMinVelocityPxPerSecond()) {
            if (getExpandedFraction() <= 0.5f) {
                z = false;
            }
            return z;
        }
        if (vel <= 0.0f) {
            z = false;
        }
        return z;
    }

    private boolean isFalseTouch(float x, float y) {
        boolean z = false;
        if (!this.mStatusBar.isFalsingThresholdNeeded()) {
            return false;
        }
        if (!this.mTouchAboveFalsingThreshold) {
            return true;
        }
        if (this.mUpwardsWhenTresholdReached) {
            return false;
        }
        if (!isDirectionUpwards(x, y)) {
            z = true;
        }
        return z;
    }

    protected void fling(float vel, boolean expand) {
        fling(vel, expand, 1.0f, false);
    }

    protected void fling(float vel, boolean expand, boolean expandBecauseOfFalsing) {
        fling(vel, expand, 1.0f, expandBecauseOfFalsing);
    }

    protected void fling(float vel, boolean expand, float collapseSpeedUpFactor, boolean expandBecauseOfFalsing) {
        cancelPeek();
        float target = expand ? (float) getMaxPanelHeight() : 0.0f;
        if (!expand) {
            this.mClosing = true;
        }
        flingToHeight(vel, expand, target, collapseSpeedUpFactor, expandBecauseOfFalsing);
    }

    protected void flingToHeight(float vel, boolean expand, float target, float collapseSpeedUpFactor, boolean expandBecauseOfFalsing) {
        boolean clearAllExpandHack = (expand && fullyExpandedClearAllVisible() && this.mExpandedHeight < ((float) (getMaxPanelHeight() - getClearAllHeight()))) ? !isClearAllVisible() : false;
        if (clearAllExpandHack) {
            target = (float) (getMaxPanelHeight() - getClearAllHeight());
        }
        if (target == this.mExpandedHeight || (getOverExpansionAmount() > 0.0f && expand)) {
            notifyExpandingFinished();
            return;
        }
        this.mOverExpandedBeforeFling = getOverExpansionAmount() > 0.0f;
        ValueAnimator animator = createHeightAnimator(target);
        if (expand) {
            if (expandBecauseOfFalsing) {
                vel = 0.0f;
            }
            this.mFlingAnimationUtils.apply(animator, this.mExpandedHeight, target, vel, (float) getHeight());
            if (expandBecauseOfFalsing) {
                animator.setDuration(350);
            }
        } else {
            this.mFlingAnimationUtils.applyDismissing(animator, this.mExpandedHeight, target, vel, (float) getHeight());
            if (vel == 0.0f) {
                animator.setDuration((long) ((((float) animator.getDuration()) * getCannedFlingDurationFactor()) / collapseSpeedUpFactor));
            }
        }
        animator.addListener(new AnimatorListenerAdapter() {
            private boolean mCancelled;

            public void onAnimationCancel(Animator animation) {
                this.mCancelled = true;
            }

            public void onAnimationEnd(Animator animation) {
                if (clearAllExpandHack && !this.mCancelled) {
                    PanelView.this.setExpandedHeightInternal((float) PanelView.this.getMaxPanelHeight());
                }
                PanelView.this.mHeightAnimator = null;
                if (!this.mCancelled) {
                    PanelView.this.notifyExpandingFinished();
                }
                PanelView.this.notifyBarPanelExpansionChanged();
            }
        });
        this.mHeightAnimator = animator;
        animator.start();
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mViewName = getResources().getResourceName(getId());
    }

    public void setExpandedHeight(float height) {
        setExpandedHeightInternal(getOverExpansionPixels() + height);
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        requestPanelHeightUpdate();
        this.mHasLayoutedSinceDown = true;
        if (this.mUpdateFlingOnLayout) {
            abortAnimations();
            fling(this.mUpdateFlingVelocity, true);
            this.mUpdateFlingOnLayout = false;
        }
    }

    protected void requestPanelHeightUpdate() {
        float currentMaxPanelHeight = (float) getMaxPanelHeight();
        if ((!this.mTracking || isTrackingBlocked()) && this.mHeightAnimator == null && !isFullyCollapsed() && currentMaxPanelHeight != this.mExpandedHeight && !this.mPeekPending && this.mPeekAnimator == null && !this.mPeekTouching) {
            setExpandedHeight(currentMaxPanelHeight);
        }
    }

    public void setExpandedHeightInternal(float h) {
        float f = 0.0f;
        float fhWithoutOverExpansion = ((float) getMaxPanelHeight()) - getOverExpansionAmount();
        if (this.mHeightAnimator == null) {
            float overExpansionPixels = Math.max(0.0f, h - fhWithoutOverExpansion);
            if (getOverExpansionPixels() != overExpansionPixels && this.mTracking) {
                setOverExpansion(overExpansionPixels, true);
            }
            this.mExpandedHeight = Math.min(h, fhWithoutOverExpansion) + getOverExpansionAmount();
        } else {
            this.mExpandedHeight = h;
            if (this.mOverExpandedBeforeFling) {
                setOverExpansion(Math.max(0.0f, h - fhWithoutOverExpansion), false);
            }
        }
        this.mExpandedHeight = Math.max(0.0f, this.mExpandedHeight);
        if (fhWithoutOverExpansion != 0.0f) {
            f = this.mExpandedHeight / fhWithoutOverExpansion;
        }
        this.mExpandedFraction = Math.min(1.0f, f);
        onHeightUpdated(this.mExpandedHeight);
        notifyBarPanelExpansionChanged();
    }

    public void setExpandedFraction(float frac) {
        setExpandedHeight(((float) getMaxPanelHeight()) * frac);
    }

    public float getExpandedHeight() {
        return this.mExpandedHeight;
    }

    public float getExpandedFraction() {
        return this.mExpandedFraction;
    }

    public boolean isFullyExpanded() {
        return this.mExpandedHeight >= ((float) getMaxPanelHeight());
    }

    public boolean isFullyCollapsed() {
        return this.mExpandedHeight <= 0.0f;
    }

    public boolean isCollapsing() {
        return this.mClosing;
    }

    public boolean isTracking() {
        return this.mTracking;
    }

    public void setBar(PanelBar panelBar) {
        this.mBar = panelBar;
    }

    public void collapse(boolean delayed, float speedUpFactor) {
        if (this.mPeekPending || this.mPeekAnimator != null) {
            this.mCollapseAfterPeek = true;
            if (this.mPeekPending) {
                removeCallbacks(this.mPeekRunnable);
                this.mPeekRunnable.run();
            }
        } else if (!isFullyCollapsed() && !this.mTracking && !this.mClosing) {
            cancelHeightAnimator();
            notifyExpandingStarted();
            this.mClosing = true;
            if (delayed) {
                this.mNextCollapseSpeedUpFactor = speedUpFactor;
                postDelayed(this.mFlingCollapseRunnable, 120);
                return;
            }
            fling(0.0f, false, speedUpFactor, false);
        }
    }

    public void expand() {
        if (isFullyCollapsed()) {
            this.mBar.startOpeningPanel(this);
            notifyExpandingStarted();
            fling(0.0f, true);
        }
    }

    public void cancelPeek() {
        if (this.mPeekAnimator != null) {
            this.mPeekAnimator.cancel();
        }
        removeCallbacks(this.mPeekRunnable);
        this.mPeekPending = false;
        notifyBarPanelExpansionChanged();
    }

    public void instantExpand() {
        this.mInstantExpanding = true;
        this.mUpdateFlingOnLayout = false;
        abortAnimations();
        cancelPeek();
        if (this.mTracking) {
            onTrackingStopped(true);
        }
        if (this.mExpanding) {
            notifyExpandingFinished();
        }
        notifyBarPanelExpansionChanged();
        getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                if (PanelView.this.mStatusBar.getStatusBarWindow().getHeight() != PanelView.this.mStatusBar.getStatusBarHeight()) {
                    PanelView.this.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    PanelView.this.setExpandedFraction(1.0f);
                    PanelView.this.mInstantExpanding = false;
                }
            }
        });
        requestLayout();
    }

    public void instantCollapse() {
        abortAnimations();
        setExpandedFraction(0.0f);
        if (this.mExpanding) {
            notifyExpandingFinished();
        }
    }

    private void abortAnimations() {
        cancelPeek();
        cancelHeightAnimator();
        removeCallbacks(this.mPostCollapseRunnable);
        removeCallbacks(this.mFlingCollapseRunnable);
    }

    protected void onClosingFinished() {
        this.mBar.onClosingFinished();
    }

    protected void startUnlockHintAnimation() {
        if (this.mHeightAnimator == null && !this.mTracking) {
            cancelPeek();
            notifyExpandingStarted();
            startUnlockHintAnimationPhase1(new Runnable() {
                public void run() {
                    PanelView.this.notifyExpandingFinished();
                    PanelView.this.mStatusBar.onHintFinished();
                    PanelView.this.mHintAnimationRunning = false;
                }
            });
            this.mStatusBar.onUnlockHintStarted();
            this.mHintAnimationRunning = true;
        }
    }

    private void startUnlockHintAnimationPhase1(final Runnable onAnimationFinished) {
        ValueAnimator animator = createHeightAnimator(Math.max(0.0f, ((float) getMaxPanelHeight()) - this.mHintDistance));
        animator.setDuration(250);
        animator.setInterpolator(this.mFastOutSlowInInterpolator);
        animator.addListener(new AnimatorListenerAdapter() {
            private boolean mCancelled;

            public void onAnimationCancel(Animator animation) {
                this.mCancelled = true;
            }

            public void onAnimationEnd(Animator animation) {
                if (this.mCancelled) {
                    PanelView.this.mHeightAnimator = null;
                    onAnimationFinished.run();
                    return;
                }
                PanelView.this.startUnlockHintAnimationPhase2(onAnimationFinished);
            }
        });
        animator.start();
        this.mHeightAnimator = animator;
        this.mKeyguardBottomArea.getIndicationView().animate().translationY(-this.mHintDistance).setDuration(250).setInterpolator(this.mFastOutSlowInInterpolator).withEndAction(new Runnable() {
            public void run() {
                PanelView.this.mKeyguardBottomArea.getIndicationView().animate().translationY(0.0f).setDuration(450).setInterpolator(PanelView.this.mBounceInterpolator).start();
            }
        }).start();
    }

    private void startUnlockHintAnimationPhase2(final Runnable onAnimationFinished) {
        ValueAnimator animator = createHeightAnimator((float) getMaxPanelHeight());
        animator.setDuration(450);
        animator.setInterpolator(this.mBounceInterpolator);
        animator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                PanelView.this.mHeightAnimator = null;
                onAnimationFinished.run();
                PanelView.this.notifyBarPanelExpansionChanged();
            }
        });
        animator.start();
        this.mHeightAnimator = animator;
    }

    private ValueAnimator createHeightAnimator(float targetHeight) {
        ValueAnimator animator = ValueAnimator.ofFloat(new float[]{this.mExpandedHeight, targetHeight});
        animator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                PanelView.this.setExpandedHeightInternal(((Float) animation.getAnimatedValue()).floatValue());
            }
        });
        return animator;
    }

    protected void notifyBarPanelExpansionChanged() {
        boolean z = true;
        PanelBar panelBar = this.mBar;
        float f = this.mExpandedFraction;
        if (!(this.mExpandedFraction > 0.0f || this.mPeekPending || this.mPeekAnimator != null || this.mInstantExpanding || isPanelVisibleBecauseOfHeadsUp() || this.mTracking || this.mHeightAnimator != null)) {
            z = false;
        }
        panelBar.panelExpansionChanged(this, f, z);
    }

    protected boolean onEmptySpaceClick(float x) {
        if (this.mHintAnimationRunning) {
            return true;
        }
        return onMiddleClicked();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        String str = "[PanelView(%s): expandedHeight=%f maxPanelHeight=%d closing=%s tracking=%s justPeeked=%s peekAnim=%s%s timeAnim=%s%s touchDisabled=%s]";
        Object[] objArr = new Object[11];
        objArr[0] = getClass().getSimpleName();
        objArr[1] = Float.valueOf(getExpandedHeight());
        objArr[2] = Integer.valueOf(getMaxPanelHeight());
        objArr[3] = this.mClosing ? "T" : "f";
        objArr[4] = this.mTracking ? "T" : "f";
        objArr[5] = this.mJustPeeked ? "T" : "f";
        objArr[6] = this.mPeekAnimator;
        String str2 = (this.mPeekAnimator == null || !this.mPeekAnimator.isStarted()) ? "" : " (started)";
        objArr[7] = str2;
        objArr[8] = this.mHeightAnimator;
        str2 = (this.mHeightAnimator == null || !this.mHeightAnimator.isStarted()) ? "" : " (started)";
        objArr[9] = str2;
        if (this.mTouchDisabled) {
            str2 = "T";
        } else {
            str2 = "f";
        }
        objArr[10] = str2;
        pw.println(String.format(str, objArr));
    }

    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        this.mHeadsUpManager = headsUpManager;
    }
}
