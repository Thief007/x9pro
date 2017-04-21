package com.android.systemui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import com.android.systemui.assis.app.MAIN.EVENT;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.FlingAnimationUtils;
import com.android.systemui.statusbar.policy.ScrollAdapter;

public class ExpandHelper {
    private Callback mCallback;
    private Context mContext;
    private float mCurrentHeight;
    private boolean mEnabled = true;
    private View mEventSource;
    private boolean mExpanding;
    private int mExpansionStyle = 0;
    private FlingAnimationUtils mFlingAnimationUtils;
    private int mGravity;
    private boolean mHasPopped;
    private float mInitialTouchFocusY;
    private float mInitialTouchSpan;
    private float mInitialTouchY;
    private int mLargeSize;
    private float mLastFocusY;
    private float mLastMotionY;
    private float mLastSpanY;
    private float mMaximumStretch;
    private float mNaturalHeight;
    private float mOldHeight;
    private boolean mOnlyMovements;
    private float mPullGestureMinXSpan;
    private ExpandableView mResizedView;
    private ScaleGestureDetector mSGD;
    private ObjectAnimator mScaleAnimation;
    private OnScaleGestureListener mScaleGestureListener = new SimpleOnScaleGestureListener() {
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            ExpandHelper.this.startExpanding(ExpandHelper.this.mResizedView, 4);
            return ExpandHelper.this.mExpanding;
        }

        public boolean onScale(ScaleGestureDetector detector) {
            return true;
        }

        public void onScaleEnd(ScaleGestureDetector detector) {
        }
    };
    private ViewScaler mScaler;
    private ScrollAdapter mScrollAdapter;
    private int mSmallSize;
    private int mTouchSlop;
    private VelocityTracker mVelocityTracker;
    private boolean mWatchingForPull;

    public interface Callback {
        boolean canChildBeExpanded(View view);

        void expansionStateChanged(boolean z);

        ExpandableView getChildAtPosition(float f, float f2);

        ExpandableView getChildAtRawPosition(float f, float f2);

        void setUserExpandedChild(View view, boolean z);

        void setUserLockedChild(View view, boolean z);
    }

    private class ViewScaler {
        ExpandableView mView;

        public void setView(ExpandableView v) {
            this.mView = v;
        }

        public void setHeight(float h) {
            this.mView.setContentHeight((int) h);
            ExpandHelper.this.mCurrentHeight = h;
        }

        public float getHeight() {
            return (float) this.mView.getContentHeight();
        }

        public int getNaturalHeight(int maximum) {
            return Math.min(maximum, this.mView.getMaxContentHeight());
        }
    }

    public ExpandHelper(Context context, Callback callback, int small, int large) {
        this.mSmallSize = small;
        this.mMaximumStretch = ((float) this.mSmallSize) * 2.0f;
        this.mLargeSize = large;
        this.mContext = context;
        this.mCallback = callback;
        this.mScaler = new ViewScaler();
        this.mGravity = 48;
        this.mScaleAnimation = ObjectAnimator.ofFloat(this.mScaler, "height", new float[]{0.0f});
        this.mPullGestureMinXSpan = this.mContext.getResources().getDimension(R.dimen.pull_span_min);
        this.mTouchSlop = ViewConfiguration.get(this.mContext).getScaledTouchSlop();
        this.mSGD = new ScaleGestureDetector(context, this.mScaleGestureListener);
        this.mFlingAnimationUtils = new FlingAnimationUtils(context, 0.3f);
    }

    private void updateExpansion() {
        float f;
        float span = (this.mSGD.getCurrentSpan() - this.mInitialTouchSpan) * 1.0f;
        float drag = (this.mSGD.getFocusY() - this.mInitialTouchFocusY) * 1.0f;
        if (this.mGravity == 80) {
            f = -1.0f;
        } else {
            f = 1.0f;
        }
        drag *= f;
        float pull = (Math.abs(drag) + Math.abs(span)) + 1.0f;
        this.mScaler.setHeight(clamp((((Math.abs(drag) * drag) / pull) + ((Math.abs(span) * span) / pull)) + this.mOldHeight));
        this.mLastFocusY = this.mSGD.getFocusY();
        this.mLastSpanY = this.mSGD.getCurrentSpan();
    }

    private float clamp(float target) {
        int i;
        float out = target;
        if (out < ((float) this.mSmallSize)) {
            i = this.mSmallSize;
        } else {
            if (out > ((float) this.mLargeSize)) {
                i = this.mLargeSize;
            }
            if (out <= this.mNaturalHeight) {
                return this.mNaturalHeight;
            }
            return out;
        }
        out = (float) i;
        if (out <= this.mNaturalHeight) {
            return out;
        }
        return this.mNaturalHeight;
    }

    private ExpandableView findView(float x, float y) {
        if (this.mEventSource == null) {
            return this.mCallback.getChildAtPosition(x, y);
        }
        int[] location = new int[2];
        this.mEventSource.getLocationOnScreen(location);
        return this.mCallback.getChildAtRawPosition(x + ((float) location[0]), y + ((float) location[1]));
    }

    private boolean isInside(View v, float x, float y) {
        int i = 1;
        if (v == null) {
            return false;
        }
        int[] location;
        boolean inside;
        if (this.mEventSource != null) {
            location = new int[2];
            this.mEventSource.getLocationOnScreen(location);
            x += (float) location[0];
            y += (float) location[1];
        }
        location = new int[2];
        v.getLocationOnScreen(location);
        x -= (float) location[0];
        y -= (float) location[1];
        if (x <= 0.0f || y <= 0.0f) {
            inside = false;
        } else {
            int i2;
            if (x < ((float) v.getWidth())) {
                i2 = 1;
            } else {
                i2 = 0;
            }
            if (y >= ((float) v.getHeight())) {
                i = 0;
            }
            inside = i2 & i;
        }
        return inside;
    }

    public void setEventSource(View eventSource) {
        this.mEventSource = eventSource;
    }

    public void setScrollAdapter(ScrollAdapter adapter) {
        this.mScrollAdapter = adapter;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!isEnabled()) {
            return false;
        }
        trackVelocity(ev);
        int action = ev.getAction();
        this.mSGD.onTouchEvent(ev);
        int x = (int) this.mSGD.getFocusX();
        int y = (int) this.mSGD.getFocusY();
        this.mInitialTouchFocusY = (float) y;
        this.mInitialTouchSpan = this.mSGD.getCurrentSpan();
        this.mLastFocusY = this.mInitialTouchFocusY;
        this.mLastSpanY = this.mInitialTouchSpan;
        if (this.mExpanding) {
            this.mLastMotionY = ev.getRawY();
            maybeRecycleVelocityTracker(ev);
            return true;
        } else if (action == 2 && (this.mExpansionStyle & 1) != 0) {
            return true;
        } else {
            switch (action & 255) {
                case 0:
                    boolean z;
                    if (this.mScrollAdapter == null || !isInside(this.mScrollAdapter.getHostView(), (float) x, (float) y)) {
                        z = false;
                    } else {
                        z = this.mScrollAdapter.isScrolledToTop();
                    }
                    this.mWatchingForPull = z;
                    this.mResizedView = findView((float) x, (float) y);
                    if (!(this.mResizedView == null || this.mCallback.canChildBeExpanded(this.mResizedView))) {
                        this.mResizedView = null;
                        this.mWatchingForPull = false;
                    }
                    this.mInitialTouchY = ev.getY();
                    break;
                case 1:
                case 3:
                    finishExpanding(false, getCurrentVelocity());
                    clearView();
                    break;
                case 2:
                    float xspan = this.mSGD.getCurrentSpanX();
                    if (xspan > this.mPullGestureMinXSpan && xspan > this.mSGD.getCurrentSpanY() && !this.mExpanding) {
                        startExpanding(this.mResizedView, 2);
                        this.mWatchingForPull = false;
                    }
                    if (this.mWatchingForPull && ev.getRawY() - this.mInitialTouchY > ((float) this.mTouchSlop)) {
                        this.mWatchingForPull = false;
                        if (!(this.mResizedView == null || isFullyExpanded(this.mResizedView) || !startExpanding(this.mResizedView, 1))) {
                            this.mLastMotionY = ev.getRawY();
                            this.mInitialTouchY = ev.getRawY();
                            this.mHasPopped = false;
                            break;
                        }
                    }
            }
            this.mLastMotionY = ev.getRawY();
            maybeRecycleVelocityTracker(ev);
            return this.mExpanding;
        }
    }

    private void trackVelocity(MotionEvent event) {
        switch (event.getActionMasked()) {
            case 0:
                if (this.mVelocityTracker == null) {
                    this.mVelocityTracker = VelocityTracker.obtain();
                } else {
                    this.mVelocityTracker.clear();
                }
                this.mVelocityTracker.addMovement(event);
                return;
            case 2:
                if (this.mVelocityTracker == null) {
                    this.mVelocityTracker = VelocityTracker.obtain();
                }
                this.mVelocityTracker.addMovement(event);
                return;
            default:
                return;
        }
    }

    private void maybeRecycleVelocityTracker(MotionEvent event) {
        if (this.mVelocityTracker == null) {
            return;
        }
        if (event.getActionMasked() == 3 || event.getActionMasked() == 1) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    private float getCurrentVelocity() {
        if (this.mVelocityTracker == null) {
            return 0.0f;
        }
        this.mVelocityTracker.computeCurrentVelocity(EVENT.DYNAMIC_PACK_EVENT_BASE);
        return this.mVelocityTracker.getYVelocity();
    }

    public void setEnabled(boolean enable) {
        this.mEnabled = enable;
    }

    private boolean isEnabled() {
        return this.mEnabled;
    }

    private boolean isFullyExpanded(ExpandableView underFocus) {
        return underFocus.areChildrenExpanded() || underFocus.getIntrinsicHeight() - underFocus.getBottomDecorHeight() == underFocus.getMaxContentHeight();
    }

    public boolean onTouchEvent(MotionEvent ev) {
        boolean z = false;
        if (!isEnabled()) {
            return false;
        }
        trackVelocity(ev);
        int action = ev.getActionMasked();
        this.mSGD.onTouchEvent(ev);
        int x = (int) this.mSGD.getFocusX();
        int y = (int) this.mSGD.getFocusY();
        if (this.mOnlyMovements) {
            this.mLastMotionY = ev.getRawY();
            return false;
        }
        switch (action) {
            case 0:
                boolean isInside;
                if (this.mScrollAdapter != null) {
                    isInside = isInside(this.mScrollAdapter.getHostView(), (float) x, (float) y);
                } else {
                    isInside = false;
                }
                this.mWatchingForPull = isInside;
                this.mResizedView = findView((float) x, (float) y);
                this.mInitialTouchY = ev.getY();
                break;
            case 1:
            case 3:
                finishExpanding(false, getCurrentVelocity());
                clearView();
                break;
            case 2:
                if (this.mWatchingForPull && ev.getRawY() - this.mInitialTouchY > ((float) this.mTouchSlop)) {
                    this.mWatchingForPull = false;
                    if (!(this.mResizedView == null || isFullyExpanded(this.mResizedView) || !startExpanding(this.mResizedView, 1))) {
                        this.mInitialTouchY = ev.getRawY();
                        this.mLastMotionY = ev.getRawY();
                        this.mHasPopped = false;
                    }
                }
                if (this.mExpanding && (this.mExpansionStyle & 1) != 0) {
                    float rawHeight = (ev.getRawY() - this.mLastMotionY) + this.mCurrentHeight;
                    float newHeight = clamp(rawHeight);
                    boolean isFinished = false;
                    boolean expanded = false;
                    if (rawHeight > this.mNaturalHeight) {
                        isFinished = true;
                        expanded = true;
                    }
                    if (rawHeight < ((float) this.mSmallSize)) {
                        isFinished = true;
                        expanded = false;
                    }
                    if (!this.mHasPopped) {
                        if (this.mEventSource != null) {
                            this.mEventSource.performHapticFeedback(1);
                        }
                        this.mHasPopped = true;
                    }
                    this.mScaler.setHeight(newHeight);
                    this.mLastMotionY = ev.getRawY();
                    if (isFinished) {
                        this.mCallback.setUserExpandedChild(this.mResizedView, expanded);
                        this.mCallback.expansionStateChanged(false);
                        return false;
                    }
                    this.mCallback.expansionStateChanged(true);
                    return true;
                } else if (this.mExpanding) {
                    updateExpansion();
                    this.mLastMotionY = ev.getRawY();
                    return true;
                }
                break;
            case 5:
            case 6:
                this.mInitialTouchY += this.mSGD.getFocusY() - this.mLastFocusY;
                this.mInitialTouchSpan += this.mSGD.getCurrentSpan() - this.mLastSpanY;
                break;
        }
        this.mLastMotionY = ev.getRawY();
        maybeRecycleVelocityTracker(ev);
        if (this.mResizedView != null) {
            z = true;
        }
        return z;
    }

    private boolean startExpanding(ExpandableView v, int expandType) {
        if (!(v instanceof ExpandableNotificationRow)) {
            return false;
        }
        this.mExpansionStyle = expandType;
        if (this.mExpanding && v == this.mResizedView) {
            return true;
        }
        this.mExpanding = true;
        this.mCallback.expansionStateChanged(true);
        this.mCallback.setUserLockedChild(v, true);
        this.mScaler.setView(v);
        this.mOldHeight = this.mScaler.getHeight();
        this.mCurrentHeight = this.mOldHeight;
        if (this.mCallback.canChildBeExpanded(v)) {
            this.mNaturalHeight = (float) this.mScaler.getNaturalHeight(this.mLargeSize);
        } else {
            this.mNaturalHeight = this.mOldHeight;
        }
        return true;
    }

    private void finishExpanding(boolean force, float velocity) {
        if (this.mExpanding) {
            boolean z;
            float currentHeight = this.mScaler.getHeight();
            float targetHeight = (float) this.mSmallSize;
            float h = this.mScaler.getHeight();
            targetHeight = (this.mOldHeight > ((float) this.mSmallSize) ? 1 : (this.mOldHeight == ((float) this.mSmallSize) ? 0 : -1)) == 0 ? (force || currentHeight > ((float) this.mSmallSize)) ? this.mNaturalHeight : (float) this.mSmallSize : (force || currentHeight < this.mNaturalHeight) ? (float) this.mSmallSize : this.mNaturalHeight;
            if (this.mScaleAnimation.isRunning()) {
                this.mScaleAnimation.cancel();
            }
            Callback callback = this.mCallback;
            View view = this.mResizedView;
            if (targetHeight == this.mNaturalHeight) {
                z = true;
            } else {
                z = false;
            }
            callback.setUserExpandedChild(view, z);
            this.mCallback.expansionStateChanged(false);
            if (targetHeight != currentHeight) {
                this.mScaleAnimation.setFloatValues(new float[]{targetHeight});
                this.mScaleAnimation.setupStartValues();
                final View scaledView = this.mResizedView;
                this.mScaleAnimation.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        ExpandHelper.this.mCallback.setUserLockedChild(scaledView, false);
                        ExpandHelper.this.mScaleAnimation.removeListener(this);
                    }
                });
                this.mFlingAnimationUtils.apply(this.mScaleAnimation, currentHeight, targetHeight, velocity);
                this.mScaleAnimation.start();
            } else {
                this.mCallback.setUserLockedChild(this.mResizedView, false);
            }
            this.mExpanding = false;
            this.mExpansionStyle = 0;
        }
    }

    private void clearView() {
        this.mResizedView = null;
    }

    public void cancel() {
        finishExpanding(true, 0.0f);
        clearView();
        this.mSGD = new ScaleGestureDetector(this.mContext, this.mScaleGestureListener);
    }

    public void onlyObserveMovements(boolean onlyMovements) {
        this.mOnlyMovements = onlyMovements;
    }
}
