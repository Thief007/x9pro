package com.android.systemui.qs;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.graphics.drawable.TransitionDrawable;
import android.view.View;
import android.view.ViewAnimationUtils;

public class QSDetailClipper {
    private Animator mAnimator;
    private final TransitionDrawable mBackground;
    private final View mDetail;
    private final AnimatorListenerAdapter mGoneOnEnd = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animation) {
            QSDetailClipper.this.mDetail.setVisibility(8);
            QSDetailClipper.this.mBackground.resetTransition();
            QSDetailClipper.this.mAnimator = null;
        }
    };
    private final Runnable mReverseBackground = new Runnable() {
        public void run() {
            if (QSDetailClipper.this.mAnimator != null) {
                QSDetailClipper.this.mBackground.reverseTransition((int) (((double) QSDetailClipper.this.mAnimator.getDuration()) * 0.35d));
            }
        }
    };
    private final AnimatorListenerAdapter mVisibleOnStart = new AnimatorListenerAdapter() {
        public void onAnimationStart(Animator animation) {
            QSDetailClipper.this.mDetail.setVisibility(0);
        }

        public void onAnimationEnd(Animator animation) {
            QSDetailClipper.this.mAnimator = null;
        }
    };

    public QSDetailClipper(View detail) {
        this.mDetail = detail;
        this.mBackground = (TransitionDrawable) detail.getBackground();
    }

    public void animateCircularClip(int x, int y, boolean in, AnimatorListener listener) {
        int r;
        if (this.mAnimator != null) {
            this.mAnimator.cancel();
        }
        int w = this.mDetail.getWidth() - x;
        int h = this.mDetail.getHeight() - y;
        int innerR = 0;
        if (x >= 0 && w >= 0 && y >= 0) {
            if (h < 0) {
            }
            r = (int) Math.max((double) ((int) Math.max((double) ((int) Math.max((double) ((int) Math.ceil(Math.sqrt((double) ((x * x) + (y * y))))), Math.ceil(Math.sqrt((double) ((w * w) + (y * y)))))), Math.ceil(Math.sqrt((double) ((w * w) + (h * h)))))), Math.ceil(Math.sqrt((double) ((x * x) + (h * h)))));
            if (in) {
                this.mAnimator = ViewAnimationUtils.createCircularReveal(this.mDetail, x, y, (float) r, (float) innerR);
            } else {
                this.mAnimator = ViewAnimationUtils.createCircularReveal(this.mDetail, x, y, (float) innerR, (float) r);
            }
            this.mAnimator.setDuration((long) (((double) this.mAnimator.getDuration()) * 1.5d));
            if (listener != null) {
                this.mAnimator.addListener(listener);
            }
            if (in) {
                this.mDetail.postDelayed(this.mReverseBackground, (long) (((double) this.mAnimator.getDuration()) * 0.65d));
                this.mAnimator.addListener(this.mGoneOnEnd);
            } else {
                this.mBackground.startTransition((int) (((double) this.mAnimator.getDuration()) * 0.6d));
                this.mAnimator.addListener(this.mVisibleOnStart);
            }
            this.mAnimator.start();
        }
        innerR = Math.min(Math.min(Math.min(Math.abs(x), Math.abs(y)), Math.abs(w)), Math.abs(h));
        r = (int) Math.max((double) ((int) Math.max((double) ((int) Math.max((double) ((int) Math.ceil(Math.sqrt((double) ((x * x) + (y * y))))), Math.ceil(Math.sqrt((double) ((w * w) + (y * y)))))), Math.ceil(Math.sqrt((double) ((w * w) + (h * h)))))), Math.ceil(Math.sqrt((double) ((x * x) + (h * h)))));
        if (in) {
            this.mAnimator = ViewAnimationUtils.createCircularReveal(this.mDetail, x, y, (float) r, (float) innerR);
        } else {
            this.mAnimator = ViewAnimationUtils.createCircularReveal(this.mDetail, x, y, (float) innerR, (float) r);
        }
        this.mAnimator.setDuration((long) (((double) this.mAnimator.getDuration()) * 1.5d));
        if (listener != null) {
            this.mAnimator.addListener(listener);
        }
        if (in) {
            this.mDetail.postDelayed(this.mReverseBackground, (long) (((double) this.mAnimator.getDuration()) * 0.65d));
            this.mAnimator.addListener(this.mGoneOnEnd);
        } else {
            this.mBackground.startTransition((int) (((double) this.mAnimator.getDuration()) * 0.6d));
            this.mAnimator.addListener(this.mVisibleOnStart);
        }
        this.mAnimator.start();
    }
}
