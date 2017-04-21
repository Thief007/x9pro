package com.android.systemui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.RectF;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import com.android.systemui.assis.app.MAIN.EVENT;

public class SwipeHelper {
    public static float SWIPE_PROGRESS_FADE_START = 0.0f;
    private static LinearInterpolator sLinearInterpolator = new LinearInterpolator();
    private int DEFAULT_ESCAPE_ANIMATION_DURATION = 200;
    private int MAX_DISMISS_VELOCITY = 2000;
    private int MAX_ESCAPE_ANIMATION_DURATION = 400;
    private float SWIPE_ESCAPE_VELOCITY = 100.0f;
    private Callback mCallback;
    private boolean mCanCurrViewBeDimissed;
    private View mCurrAnimView;
    private View mCurrView;
    private float mDensityScale;
    private boolean mDragging;
    private int mFalsingThreshold;
    private final Interpolator mFastOutLinearInInterpolator;
    private Handler mHandler;
    private float mInitialTouchPos;
    private LongPressListener mLongPressListener;
    private boolean mLongPressSent;
    private long mLongPressTimeout;
    private float mMaxSwipeProgress = 1.0f;
    private float mMinSwipeProgress = 0.0f;
    private float mPagingTouchSlop;
    private int mSwipeDirection;
    private final int[] mTmpPos = new int[2];
    private boolean mTouchAboveFalsingThreshold;
    private VelocityTracker mVelocityTracker;
    private Runnable mWatchLongPress;

    public interface Callback {
        boolean canChildBeDismissed(View view);

        View getChildAtPosition(MotionEvent motionEvent);

        View getChildContentView(View view);

        float getFalsingThresholdFactor();

        boolean isAntiFalsingNeeded();

        void onBeginDrag(View view);

        void onChildDismissed(View view);

        void onChildSnappedBack(View view);

        void onDragCancelled(View view);

        boolean updateSwipeProgress(View view, boolean z, float f);
    }

    public interface LongPressListener {
        boolean onLongPress(View view, int i, int i2);
    }

    public SwipeHelper(int swipeDirection, Callback callback, Context context) {
        this.mCallback = callback;
        this.mHandler = new Handler();
        this.mSwipeDirection = swipeDirection;
        this.mVelocityTracker = VelocityTracker.obtain();
        this.mDensityScale = context.getResources().getDisplayMetrics().density;
        this.mPagingTouchSlop = (float) ViewConfiguration.get(context).getScaledPagingTouchSlop();
        this.mLongPressTimeout = (long) (((float) ViewConfiguration.getLongPressTimeout()) * 1.5f);
        this.mFastOutLinearInInterpolator = AnimationUtils.loadInterpolator(context, 17563663);
        this.mFalsingThreshold = context.getResources().getDimensionPixelSize(R.dimen.swipe_helper_falsing_threshold);
    }

    public void setLongPressListener(LongPressListener listener) {
        this.mLongPressListener = listener;
    }

    public void setDensityScale(float densityScale) {
        this.mDensityScale = densityScale;
    }

    public void setPagingTouchSlop(float pagingTouchSlop) {
        this.mPagingTouchSlop = pagingTouchSlop;
    }

    private float getPos(MotionEvent ev) {
        return this.mSwipeDirection == 0 ? ev.getX() : ev.getY();
    }

    private float getTranslation(View v) {
        return this.mSwipeDirection == 0 ? v.getTranslationX() : v.getTranslationY();
    }

    private float getVelocity(VelocityTracker vt) {
        if (this.mSwipeDirection == 0) {
            return vt.getXVelocity();
        }
        return vt.getYVelocity();
    }

    private ObjectAnimator createTranslationAnimation(View v, float newPos) {
        return ObjectAnimator.ofFloat(v, this.mSwipeDirection == 0 ? "translationX" : "translationY", new float[]{newPos});
    }

    private float getPerpendicularVelocity(VelocityTracker vt) {
        if (this.mSwipeDirection == 0) {
            return vt.getYVelocity();
        }
        return vt.getXVelocity();
    }

    private void setTranslation(View v, float translate) {
        if (this.mSwipeDirection == 0) {
            v.setTranslationX(translate);
        } else {
            v.setTranslationY(translate);
        }
    }

    private float getSize(View v) {
        int measuredWidth;
        if (this.mSwipeDirection == 0) {
            measuredWidth = v.getMeasuredWidth();
        } else {
            measuredWidth = v.getMeasuredHeight();
        }
        return (float) measuredWidth;
    }

    private float getSwipeProgressForOffset(View view) {
        float viewSize = getSize(view);
        float fadeSize = 0.5f * viewSize;
        float result = 1.0f;
        float pos = getTranslation(view);
        if (pos >= SWIPE_PROGRESS_FADE_START * viewSize) {
            result = 1.0f - ((pos - (SWIPE_PROGRESS_FADE_START * viewSize)) / fadeSize);
        } else if (pos < (1.0f - SWIPE_PROGRESS_FADE_START) * viewSize) {
            result = 1.0f + (((SWIPE_PROGRESS_FADE_START * viewSize) + pos) / fadeSize);
        }
        return Math.min(Math.max(this.mMinSwipeProgress, result), this.mMaxSwipeProgress);
    }

    private void updateSwipeProgressFromOffset(View animView, boolean dismissable) {
        float swipeProgress = getSwipeProgressForOffset(animView);
        if (!this.mCallback.updateSwipeProgress(animView, dismissable, swipeProgress) && dismissable) {
            float alpha = swipeProgress;
            if (alpha == 0.0f || alpha == 1.0f) {
                animView.setLayerType(0, null);
            } else {
                animView.setLayerType(2, null);
            }
            animView.setAlpha(getSwipeProgressForOffset(animView));
        }
        invalidateGlobalRegion(animView);
    }

    public static void invalidateGlobalRegion(View view) {
        invalidateGlobalRegion(view, new RectF((float) view.getLeft(), (float) view.getTop(), (float) view.getRight(), (float) view.getBottom()));
    }

    public static void invalidateGlobalRegion(View view, RectF childBounds) {
        while (view.getParent() != null && (view.getParent() instanceof View)) {
            view = (View) view.getParent();
            view.getMatrix().mapRect(childBounds);
            view.invalidate((int) Math.floor((double) childBounds.left), (int) Math.floor((double) childBounds.top), (int) Math.ceil((double) childBounds.right), (int) Math.ceil((double) childBounds.bottom));
        }
    }

    public void removeLongPressCallback() {
        if (this.mWatchLongPress != null) {
            this.mHandler.removeCallbacks(this.mWatchLongPress);
            this.mWatchLongPress = null;
        }
    }

    public boolean onInterceptTouchEvent(final MotionEvent ev) {
        boolean z = true;
        switch (ev.getAction()) {
            case 0:
                this.mTouchAboveFalsingThreshold = false;
                this.mDragging = false;
                this.mLongPressSent = false;
                this.mCurrView = this.mCallback.getChildAtPosition(ev);
                this.mVelocityTracker.clear();
                if (this.mCurrView != null) {
                    this.mCurrAnimView = this.mCallback.getChildContentView(this.mCurrView);
                    this.mCanCurrViewBeDimissed = this.mCallback.canChildBeDismissed(this.mCurrView);
                    this.mVelocityTracker.addMovement(ev);
                    this.mInitialTouchPos = getPos(ev);
                    if (this.mLongPressListener != null) {
                        if (this.mWatchLongPress == null) {
                            this.mWatchLongPress = new Runnable() {
                                public void run() {
                                    if (SwipeHelper.this.mCurrView != null && !SwipeHelper.this.mLongPressSent) {
                                        SwipeHelper.this.mLongPressSent = true;
                                        SwipeHelper.this.mCurrView.sendAccessibilityEvent(2);
                                        SwipeHelper.this.mCurrView.getLocationOnScreen(SwipeHelper.this.mTmpPos);
                                        SwipeHelper.this.mLongPressListener.onLongPress(SwipeHelper.this.mCurrView, ((int) ev.getRawX()) - SwipeHelper.this.mTmpPos[0], ((int) ev.getRawY()) - SwipeHelper.this.mTmpPos[1]);
                                    }
                                }
                            };
                        }
                        this.mHandler.postDelayed(this.mWatchLongPress, this.mLongPressTimeout);
                        break;
                    }
                }
                break;
            case 1:
            case 3:
                boolean z2 = !this.mDragging ? this.mLongPressSent : true;
                this.mDragging = false;
                this.mCurrView = null;
                this.mCurrAnimView = null;
                this.mLongPressSent = false;
                removeLongPressCallback();
                if (z2) {
                    return true;
                }
                break;
            case 2:
                if (!(this.mCurrView == null || this.mLongPressSent)) {
                    this.mVelocityTracker.addMovement(ev);
                    if (Math.abs(getPos(ev) - this.mInitialTouchPos) > this.mPagingTouchSlop) {
                        this.mCallback.onBeginDrag(this.mCurrView);
                        this.mDragging = true;
                        this.mInitialTouchPos = getPos(ev) - getTranslation(this.mCurrAnimView);
                        removeLongPressCallback();
                        break;
                    }
                }
                break;
        }
        if (!this.mDragging) {
            z = this.mLongPressSent;
        }
        return z;
    }

    public void dismissChild(View view, float velocity) {
        dismissChild(view, velocity, null, 0, false, 0);
    }

    public void dismissChild(final View view, float velocity, final Runnable endAction, long delay, boolean useAccelerateInterpolator, long fixedDuration) {
        float newPos;
        long duration;
        final View animView = this.mCallback.getChildContentView(view);
        final boolean canAnimViewBeDismissed = this.mCallback.canChildBeDismissed(view);
        boolean isLayoutRtl = view.getLayoutDirection() == 1;
        if (velocity < 0.0f || ((velocity == 0.0f && getTranslation(animView) < 0.0f) || ((velocity == 0.0f && getTranslation(animView) == 0.0f && this.mSwipeDirection == 1) || (velocity == 0.0f && getTranslation(animView) == 0.0f && isLayoutRtl)))) {
            newPos = -getSize(animView);
        } else {
            newPos = getSize(animView);
        }
        if (fixedDuration == 0) {
            duration = (long) this.MAX_ESCAPE_ANIMATION_DURATION;
            if (velocity != 0.0f) {
                duration = Math.min(duration, (long) ((int) ((Math.abs(newPos - getTranslation(animView)) * 1000.0f) / Math.abs(velocity))));
            } else {
                duration = (long) this.DEFAULT_ESCAPE_ANIMATION_DURATION;
            }
        } else {
            duration = fixedDuration;
        }
        animView.setLayerType(2, null);
        ObjectAnimator anim = createTranslationAnimation(animView, newPos);
        if (useAccelerateInterpolator) {
            anim.setInterpolator(this.mFastOutLinearInInterpolator);
        } else {
            anim.setInterpolator(sLinearInterpolator);
        }
        anim.setDuration(duration);
        if (delay > 0) {
            anim.setStartDelay(delay);
        }
        anim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                SwipeHelper.this.mCallback.onChildDismissed(view);
                if (endAction != null) {
                    endAction.run();
                }
                animView.setLayerType(0, null);
            }
        });
        anim.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                SwipeHelper.this.updateSwipeProgressFromOffset(animView, canAnimViewBeDismissed);
            }
        });
        anim.start();
    }

    public void snapChild(View view, float velocity) {
        final View animView = this.mCallback.getChildContentView(view);
        final boolean canAnimViewBeDismissed = this.mCallback.canChildBeDismissed(animView);
        ObjectAnimator anim = createTranslationAnimation(animView, 0.0f);
        anim.setDuration(150);
        anim.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                SwipeHelper.this.updateSwipeProgressFromOffset(animView, canAnimViewBeDismissed);
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                SwipeHelper.this.updateSwipeProgressFromOffset(animView, canAnimViewBeDismissed);
                SwipeHelper.this.mCallback.onChildSnappedBack(animView);
            }
        });
        anim.start();
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (this.mLongPressSent) {
            return true;
        }
        if (this.mDragging) {
            this.mVelocityTracker.addMovement(ev);
            switch (ev.getAction()) {
                case 1:
                case 3:
                    if (this.mCurrView != null) {
                        boolean childSwipedFastEnough;
                        this.mVelocityTracker.computeCurrentVelocity(EVENT.DYNAMIC_PACK_EVENT_BASE, ((float) this.MAX_DISMISS_VELOCITY) * this.mDensityScale);
                        float escapeVelocity = this.SWIPE_ESCAPE_VELOCITY * this.mDensityScale;
                        float velocity = getVelocity(this.mVelocityTracker);
                        float perpendicularVelocity = getPerpendicularVelocity(this.mVelocityTracker);
                        boolean childSwipedFarEnough = ((double) Math.abs(getTranslation(this.mCurrAnimView))) > ((double) getSize(this.mCurrAnimView)) * 0.4d;
                        if (Math.abs(velocity) <= escapeVelocity || Math.abs(velocity) <= Math.abs(perpendicularVelocity)) {
                            childSwipedFastEnough = false;
                        } else {
                            childSwipedFastEnough = ((velocity > 0.0f ? 1 : (velocity == 0.0f ? 0 : -1)) > 0 ? 1 : null) == ((getTranslation(this.mCurrAnimView) > 0.0f ? 1 : (getTranslation(this.mCurrAnimView) == 0.0f ? 0 : -1)) > 0 ? 1 : null);
                        }
                        boolean falsingDetected = this.mCallback.isAntiFalsingNeeded() ? !this.mTouchAboveFalsingThreshold : false;
                        boolean dismissChild = (this.mCallback.canChildBeDismissed(this.mCurrView) && !falsingDetected && (childSwipedFastEnough || childSwipedFarEnough)) ? ev.getActionMasked() == 1 : false;
                        if (!dismissChild) {
                            this.mCallback.onDragCancelled(this.mCurrView);
                            snapChild(this.mCurrView, velocity);
                            break;
                        }
                        View view = this.mCurrView;
                        if (!childSwipedFastEnough) {
                            velocity = 0.0f;
                        }
                        dismissChild(view, velocity);
                        break;
                    }
                    break;
                case 2:
                case 4:
                    if (this.mCurrView != null) {
                        float delta = getPos(ev) - this.mInitialTouchPos;
                        float absDelta = Math.abs(delta);
                        if (absDelta >= ((float) getFalsingThreshold())) {
                            this.mTouchAboveFalsingThreshold = true;
                        }
                        if (!this.mCallback.canChildBeDismissed(this.mCurrView)) {
                            float size = getSize(this.mCurrAnimView);
                            float maxScrollDistance = 0.15f * size;
                            delta = absDelta >= size ? delta > 0.0f ? maxScrollDistance : -maxScrollDistance : maxScrollDistance * ((float) Math.sin(((double) (delta / size)) * 1.5707963267948966d));
                        }
                        setTranslation(this.mCurrAnimView, delta);
                        updateSwipeProgressFromOffset(this.mCurrAnimView, this.mCanCurrViewBeDimissed);
                        break;
                    }
                    break;
            }
            return true;
        } else if (this.mCallback.getChildAtPosition(ev) != null) {
            onInterceptTouchEvent(ev);
            return true;
        } else {
            removeLongPressCallback();
            return false;
        }
    }

    private int getFalsingThreshold() {
        return (int) (((float) this.mFalsingThreshold) * this.mCallback.getFalsingThresholdFactor());
    }
}
