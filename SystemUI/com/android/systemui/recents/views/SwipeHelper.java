package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.TargetApi;
import android.os.Build.VERSION;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.LinearInterpolator;
import com.android.systemui.assis.app.MAIN.EVENT;
import com.android.systemui.recents.RecentsConfiguration;

public class SwipeHelper {
    public static float ALPHA_FADE_START = 0.15f;
    private static LinearInterpolator sLinearInterpolator = new LinearInterpolator();
    private int DEFAULT_ESCAPE_ANIMATION_DURATION = 75;
    private int MAX_ESCAPE_ANIMATION_DURATION = 150;
    private float SWIPE_ESCAPE_VELOCITY = 100.0f;
    public boolean mAllowSwipeTowardsEnd = true;
    public boolean mAllowSwipeTowardsStart = true;
    Callback mCallback;
    private boolean mCanCurrViewBeDimissed;
    private View mCurrView;
    private float mDensityScale;
    private boolean mDragging;
    private float mInitialTouchPos;
    private float mMinAlpha = 0.0f;
    private float mPagingTouchSlop;
    private boolean mRtl;
    private int mSwipeDirection;
    private VelocityTracker mVelocityTracker;

    public interface Callback {
        boolean canChildBeDismissed(View view);

        View getChildAtPosition(MotionEvent motionEvent);

        void onBeginDrag(View view);

        void onChildDismissed(View view);

        void onDragCancelled(View view);

        void onSnapBackCompleted(View view);

        void onSwipeChanged(View view, float f);
    }

    public SwipeHelper(int swipeDirection, Callback callback, float densityScale, float pagingTouchSlop) {
        this.mCallback = callback;
        this.mSwipeDirection = swipeDirection;
        this.mVelocityTracker = VelocityTracker.obtain();
        this.mDensityScale = densityScale;
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
        return ObjectAnimator.ofFloat(v, this.mSwipeDirection == 0 ? View.TRANSLATION_X : View.TRANSLATION_Y, new float[]{newPos});
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
        DisplayMetrics dm = v.getContext().getResources().getDisplayMetrics();
        return (float) (this.mSwipeDirection == 0 ? dm.widthPixels : dm.heightPixels);
    }

    public void setMinAlpha(float minAlpha) {
        this.mMinAlpha = minAlpha;
    }

    float getAlphaForOffset(View view) {
        float viewSize = getSize(view);
        float fadeSize = 0.65f * viewSize;
        float result = 1.0f;
        float pos = getTranslation(view);
        if (pos >= ALPHA_FADE_START * viewSize) {
            result = 1.0f - ((pos - (ALPHA_FADE_START * viewSize)) / fadeSize);
        } else if (pos < (1.0f - ALPHA_FADE_START) * viewSize) {
            result = 1.0f + (((ALPHA_FADE_START * viewSize) + pos) / fadeSize);
        }
        return Math.max(this.mMinAlpha, Math.max(Math.min(result, 1.0f), 0.0f));
    }

    @TargetApi(17)
    public static boolean isLayoutRtl(View view) {
        boolean z = true;
        if (VERSION.SDK_INT < 17) {
            return false;
        }
        if (1 != view.getLayoutDirection()) {
            z = false;
        }
        return z;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case 0:
                this.mDragging = false;
                this.mCurrView = this.mCallback.getChildAtPosition(ev);
                this.mVelocityTracker.clear();
                if (this.mCurrView == null) {
                    this.mCanCurrViewBeDimissed = false;
                    break;
                }
                this.mRtl = isLayoutRtl(this.mCurrView);
                this.mCanCurrViewBeDimissed = this.mCallback.canChildBeDismissed(this.mCurrView);
                this.mVelocityTracker.addMovement(ev);
                this.mInitialTouchPos = getPos(ev);
                break;
            case 1:
            case 3:
                this.mDragging = false;
                this.mCurrView = null;
                break;
            case 2:
                if (this.mCurrView != null) {
                    this.mVelocityTracker.addMovement(ev);
                    float pos = getPos(ev);
                    if (Math.abs(pos - this.mInitialTouchPos) > this.mPagingTouchSlop) {
                        this.mCallback.onBeginDrag(this.mCurrView);
                        this.mDragging = true;
                        this.mInitialTouchPos = pos - getTranslation(this.mCurrView);
                        break;
                    }
                }
                break;
        }
        return this.mDragging;
    }

    private void dismissChild(final View view, float velocity) {
        float newPos;
        final boolean canAnimViewBeDismissed = this.mCallback.canChildBeDismissed(view);
        if (velocity < 0.0f || ((velocity == 0.0f && getTranslation(view) < 0.0f) || (velocity == 0.0f && getTranslation(view) == 0.0f && this.mSwipeDirection == 1))) {
            newPos = -getSize(view);
        } else {
            newPos = getSize(view);
        }
        int duration = this.MAX_ESCAPE_ANIMATION_DURATION;
        if (velocity != 0.0f) {
            duration = Math.min(duration, (int) ((Math.abs(newPos - getTranslation(view)) * 1000.0f) / Math.abs(velocity)));
        } else {
            duration = this.DEFAULT_ESCAPE_ANIMATION_DURATION;
        }
        ValueAnimator anim = createTranslationAnimation(view, newPos);
        anim.setInterpolator(sLinearInterpolator);
        anim.setDuration((long) duration);
        anim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                SwipeHelper.this.mCallback.onChildDismissed(view);
                if (canAnimViewBeDismissed) {
                    view.setAlpha(1.0f);
                }
            }
        });
        anim.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                if (canAnimViewBeDismissed) {
                    view.setAlpha(SwipeHelper.this.getAlphaForOffset(view));
                }
            }
        });
        anim.start();
    }

    private void snapChild(final View view, float velocity) {
        final boolean canAnimViewBeDismissed = this.mCallback.canChildBeDismissed(view);
        ValueAnimator anim = createTranslationAnimation(view, 0.0f);
        anim.setDuration(250);
        anim.setInterpolator(RecentsConfiguration.getInstance().linearOutSlowInInterpolator);
        anim.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                if (canAnimViewBeDismissed) {
                    view.setAlpha(SwipeHelper.this.getAlphaForOffset(view));
                }
                SwipeHelper.this.mCallback.onSwipeChanged(SwipeHelper.this.mCurrView, view.getTranslationX());
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                if (canAnimViewBeDismissed) {
                    view.setAlpha(1.0f);
                }
                SwipeHelper.this.mCallback.onSnapBackCompleted(view);
            }
        });
        anim.start();
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (!this.mDragging && !onInterceptTouchEvent(ev)) {
            return this.mCanCurrViewBeDimissed;
        }
        this.mVelocityTracker.addMovement(ev);
        switch (ev.getAction()) {
            case 1:
            case 3:
                if (this.mCurrView != null) {
                    endSwipe(this.mVelocityTracker);
                    break;
                }
                break;
            case 2:
            case 4:
                if (this.mCurrView != null) {
                    float delta = getPos(ev) - this.mInitialTouchPos;
                    setSwipeAmount(delta);
                    this.mCallback.onSwipeChanged(this.mCurrView, delta);
                    break;
                }
                break;
        }
        return true;
    }

    private void setSwipeAmount(float amount) {
        if (!(isValidSwipeDirection(amount) && this.mCallback.canChildBeDismissed(this.mCurrView))) {
            float size = getSize(this.mCurrView);
            float maxScrollDistance = 0.15f * size;
            amount = Math.abs(amount) >= size ? amount > 0.0f ? maxScrollDistance : -maxScrollDistance : maxScrollDistance * ((float) Math.sin(((double) (amount / size)) * 1.5707963267948966d));
        }
        setTranslation(this.mCurrView, amount);
        if (this.mCanCurrViewBeDimissed) {
            this.mCurrView.setAlpha(getAlphaForOffset(this.mCurrView));
        }
    }

    private boolean isValidSwipeDirection(float amount) {
        if (this.mSwipeDirection != 0) {
            return true;
        }
        if (this.mRtl) {
            return amount <= 0.0f ? this.mAllowSwipeTowardsEnd : this.mAllowSwipeTowardsStart;
        }
        return amount <= 0.0f ? this.mAllowSwipeTowardsStart : this.mAllowSwipeTowardsEnd;
    }

    private void endSwipe(VelocityTracker velocityTracker) {
        boolean childSwipedFastEnough;
        velocityTracker.computeCurrentVelocity(EVENT.DYNAMIC_PACK_EVENT_BASE);
        float velocity = getVelocity(velocityTracker);
        float perpendicularVelocity = getPerpendicularVelocity(velocityTracker);
        float escapeVelocity = this.SWIPE_ESCAPE_VELOCITY * this.mDensityScale;
        float translation = getTranslation(this.mCurrView);
        boolean childSwipedFarEnough = ((double) Math.abs(translation)) > ((double) getSize(this.mCurrView)) * 0.6d;
        if (Math.abs(velocity) <= escapeVelocity || Math.abs(velocity) <= Math.abs(perpendicularVelocity)) {
            childSwipedFastEnough = false;
        } else {
            childSwipedFastEnough = ((velocity > 0.0f ? 1 : (velocity == 0.0f ? 0 : -1)) > 0 ? 1 : null) == ((translation > 0.0f ? 1 : (translation == 0.0f ? 0 : -1)) > 0 ? 1 : null);
        }
        boolean dismissChild = (this.mCallback.canChildBeDismissed(this.mCurrView) && isValidSwipeDirection(translation)) ? !childSwipedFastEnough ? childSwipedFarEnough : true : false;
        if (dismissChild) {
            View view = this.mCurrView;
            if (!childSwipedFastEnough) {
                velocity = 0.0f;
            }
            dismissChild(view, velocity);
            return;
        }
        this.mCallback.onDragCancelled(this.mCurrView);
        snapChild(this.mCurrView, velocity);
    }
}
