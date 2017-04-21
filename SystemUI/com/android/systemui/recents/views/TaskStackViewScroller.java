package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.widget.OverScroller;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.Utilities;

public class TaskStackViewScroller {
    TaskStackViewScrollerCallbacks mCb;
    RecentsConfiguration mConfig;
    float mFinalAnimatedScroll;
    TaskStackViewLayoutAlgorithm mLayoutAlgorithm;
    ObjectAnimator mScrollAnimator;
    OverScroller mScroller;
    float mStackScrollP;

    public interface TaskStackViewScrollerCallbacks {
        void onScrollChanged(float f);
    }

    public TaskStackViewScroller(Context context, RecentsConfiguration config, TaskStackViewLayoutAlgorithm layoutAlgorithm) {
        this.mConfig = config;
        this.mScroller = new OverScroller(context);
        this.mLayoutAlgorithm = layoutAlgorithm;
        setStackScroll(getStackScroll());
    }

    void reset() {
        this.mStackScrollP = 0.0f;
    }

    void setCallbacks(TaskStackViewScrollerCallbacks cb) {
        this.mCb = cb;
    }

    public float getStackScroll() {
        return this.mStackScrollP;
    }

    public void setStackScroll(float s) {
        this.mStackScrollP = s;
        if (this.mCb != null) {
            this.mCb.onScrollChanged(this.mStackScrollP);
        }
    }

    void setStackScrollRaw(float s) {
        this.mStackScrollP = s;
    }

    public boolean setStackScrollToInitialState() {
        float prevStackScrollP = this.mStackScrollP;
        setStackScroll(getBoundedStackScroll(this.mLayoutAlgorithm.mInitialScrollP));
        if (Float.compare(prevStackScrollP, this.mStackScrollP) != 0) {
            return true;
        }
        return false;
    }

    public boolean boundScroll() {
        float curScroll = getStackScroll();
        float newScroll = getBoundedStackScroll(curScroll);
        if (Float.compare(newScroll, curScroll) == 0) {
            return false;
        }
        setStackScroll(newScroll);
        return true;
    }

    float getBoundedStackScroll(float scroll) {
        return Math.max(this.mLayoutAlgorithm.mMinScrollP, Math.min(this.mLayoutAlgorithm.mMaxScrollP, scroll));
    }

    float getScrollAmountOutOfBounds(float scroll) {
        if (scroll < this.mLayoutAlgorithm.mMinScrollP) {
            return Math.abs(scroll - this.mLayoutAlgorithm.mMinScrollP);
        }
        if (scroll > this.mLayoutAlgorithm.mMaxScrollP) {
            return Math.abs(scroll - this.mLayoutAlgorithm.mMaxScrollP);
        }
        return 0.0f;
    }

    boolean isScrollOutOfBounds() {
        return Float.compare(getScrollAmountOutOfBounds(this.mStackScrollP), 0.0f) != 0;
    }

    ObjectAnimator animateBoundScroll() {
        float curScroll = getStackScroll();
        float newScroll = getBoundedStackScroll(curScroll);
        if (Float.compare(newScroll, curScroll) != 0) {
            animateScroll(curScroll, newScroll, null);
        }
        return this.mScrollAnimator;
    }

    void animateScroll(float curScroll, float newScroll, final Runnable postRunnable) {
        if (this.mScrollAnimator != null && this.mScrollAnimator.isRunning()) {
            setStackScroll(this.mFinalAnimatedScroll);
            this.mScroller.startScroll(0, progressToScrollRange(this.mFinalAnimatedScroll), 0, 0, 0);
        }
        stopScroller();
        stopBoundScrollAnimation();
        this.mFinalAnimatedScroll = newScroll;
        this.mScrollAnimator = ObjectAnimator.ofFloat(this, "stackScroll", new float[]{curScroll, newScroll});
        this.mScrollAnimator.setDuration((long) this.mConfig.taskStackScrollDuration);
        this.mScrollAnimator.setInterpolator(this.mConfig.linearOutSlowInInterpolator);
        this.mScrollAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                TaskStackViewScroller.this.setStackScroll(((Float) animation.getAnimatedValue()).floatValue());
            }
        });
        this.mScrollAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                if (postRunnable != null) {
                    postRunnable.run();
                }
                TaskStackViewScroller.this.mScrollAnimator.removeAllListeners();
            }
        });
        this.mScrollAnimator.start();
    }

    void stopBoundScrollAnimation() {
        Utilities.cancelAnimationWithoutCallbacks(this.mScrollAnimator);
    }

    int progressToScrollRange(float p) {
        return (int) (((float) this.mLayoutAlgorithm.mStackVisibleRect.height()) * p);
    }

    float scrollRangeToProgress(int s) {
        return ((float) s) / ((float) this.mLayoutAlgorithm.mStackVisibleRect.height());
    }

    boolean computeScroll() {
        if (!this.mScroller.computeScrollOffset()) {
            return false;
        }
        float scroll = scrollRangeToProgress(this.mScroller.getCurrY());
        setStackScrollRaw(scroll);
        if (this.mCb != null) {
            this.mCb.onScrollChanged(scroll);
        }
        return true;
    }

    boolean isScrolling() {
        return !this.mScroller.isFinished();
    }

    void stopScroller() {
        if (!this.mScroller.isFinished()) {
            this.mScroller.abortAnimation();
        }
    }
}
