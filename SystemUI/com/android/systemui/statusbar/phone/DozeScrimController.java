package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.android.systemui.doze.DozeHost.PulseCallback;
import com.android.systemui.doze.DozeLog;

public class DozeScrimController {
    private static final boolean DEBUG = Log.isLoggable("DozeScrimController", 3);
    private Animator mBehindAnimator;
    private float mBehindTarget;
    private final Interpolator mDozeAnimationInterpolator;
    private final DozeParameters mDozeParameters;
    private boolean mDozing;
    private final Handler mHandler = new Handler();
    private Animator mInFrontAnimator;
    private float mInFrontTarget;
    private PulseCallback mPulseCallback;
    private final Runnable mPulseIn = new Runnable() {
        public void run() {
            if (DozeScrimController.DEBUG) {
                Log.d("DozeScrimController", "Pulse in, mDozing=" + DozeScrimController.this.mDozing + " mPulseReason=" + DozeLog.pulseReasonToString(DozeScrimController.this.mPulseReason));
            }
            if (DozeScrimController.this.mDozing) {
                DozeLog.tracePulseStart(DozeScrimController.this.mPulseReason);
                DozeScrimController.this.pulseStarted();
            }
        }
    };
    private final Runnable mPulseInFinished = new Runnable() {
        public void run() {
            if (DozeScrimController.DEBUG) {
                Log.d("DozeScrimController", "Pulse in finished, mDozing=" + DozeScrimController.this.mDozing);
            }
            if (DozeScrimController.this.mDozing) {
                DozeScrimController.this.mHandler.postDelayed(DozeScrimController.this.mPulseOut, (long) DozeScrimController.this.mDozeParameters.getPulseVisibleDuration());
            }
        }
    };
    private final Interpolator mPulseInInterpolator = PhoneStatusBar.ALPHA_OUT;
    private final Interpolator mPulseInInterpolatorPickup;
    private final Runnable mPulseOut = new Runnable() {
        public void run() {
            if (DozeScrimController.DEBUG) {
                Log.d("DozeScrimController", "Pulse out, mDozing=" + DozeScrimController.this.mDozing);
            }
            if (DozeScrimController.this.mDozing) {
                DozeScrimController.this.startScrimAnimation(true, 1.0f, (long) DozeScrimController.this.mDozeParameters.getPulseOutDuration(), DozeScrimController.this.mPulseOutInterpolator, DozeScrimController.this.mPulseOutFinished);
            }
        }
    };
    private final Runnable mPulseOutFinished = new Runnable() {
        public void run() {
            if (DozeScrimController.DEBUG) {
                Log.d("DozeScrimController", "Pulse out finished");
            }
            DozeLog.tracePulseFinish();
            DozeScrimController.this.pulseFinished();
        }
    };
    private final Interpolator mPulseOutInterpolator = PhoneStatusBar.ALPHA_IN;
    private int mPulseReason;
    private final ScrimController mScrimController;

    public DozeScrimController(ScrimController scrimController, Context context) {
        this.mScrimController = scrimController;
        this.mDozeParameters = new DozeParameters(context);
        Interpolator loadInterpolator = AnimationUtils.loadInterpolator(context, 17563662);
        this.mPulseInInterpolatorPickup = loadInterpolator;
        this.mDozeAnimationInterpolator = loadInterpolator;
    }

    public void setDozing(boolean dozing, boolean animate) {
        if (this.mDozing != dozing) {
            this.mDozing = dozing;
            if (this.mDozing) {
                abortAnimations();
                this.mScrimController.setDozeBehindAlpha(1.0f);
                this.mScrimController.setDozeInFrontAlpha(1.0f);
            } else {
                cancelPulsing();
                if (animate) {
                    startScrimAnimation(false, 0.0f, 700, this.mDozeAnimationInterpolator);
                    startScrimAnimation(true, 0.0f, 700, this.mDozeAnimationInterpolator);
                } else {
                    abortAnimations();
                    this.mScrimController.setDozeBehindAlpha(0.0f);
                    this.mScrimController.setDozeInFrontAlpha(0.0f);
                }
            }
        }
    }

    public void pulse(PulseCallback callback, int reason) {
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        } else if (this.mDozing && this.mPulseCallback == null) {
            this.mPulseCallback = callback;
            this.mPulseReason = reason;
            this.mHandler.post(this.mPulseIn);
        } else {
            callback.onPulseFinished();
        }
    }

    public void abortPulsing() {
        this.mHandler.removeCallbacks(this.mPulseIn);
        abortAnimations();
        if (this.mDozing) {
            this.mScrimController.setDozeBehindAlpha(1.0f);
            this.mScrimController.setDozeInFrontAlpha(1.0f);
        }
        this.mPulseCallback = null;
    }

    public void onScreenTurnedOn() {
        if (isPulsing()) {
            boolean pickup = this.mPulseReason == 3;
            startScrimAnimation(true, 0.0f, (long) this.mDozeParameters.getPulseInDuration(pickup), pickup ? this.mPulseInInterpolatorPickup : this.mPulseInInterpolator, this.mPulseInFinished);
        }
    }

    public boolean isPulsing() {
        return this.mPulseCallback != null;
    }

    private void cancelPulsing() {
        if (DEBUG) {
            Log.d("DozeScrimController", "Cancel pulsing");
        }
        if (this.mPulseCallback != null) {
            this.mHandler.removeCallbacks(this.mPulseIn);
            this.mHandler.removeCallbacks(this.mPulseOut);
            pulseFinished();
        }
    }

    private void pulseStarted() {
        if (this.mPulseCallback != null) {
            this.mPulseCallback.onPulseStarted();
        }
    }

    private void pulseFinished() {
        if (this.mPulseCallback != null) {
            this.mPulseCallback.onPulseFinished();
            this.mPulseCallback = null;
        }
    }

    private void abortAnimations() {
        if (this.mInFrontAnimator != null) {
            this.mInFrontAnimator.cancel();
        }
        if (this.mBehindAnimator != null) {
            this.mBehindAnimator.cancel();
        }
    }

    private void startScrimAnimation(boolean inFront, float target, long duration, Interpolator interpolator) {
        startScrimAnimation(inFront, target, duration, interpolator, null);
    }

    private void startScrimAnimation(final boolean inFront, float target, long duration, Interpolator interpolator, final Runnable endRunnable) {
        Animator current = getCurrentAnimator(inFront);
        if (current != null) {
            if (getCurrentTarget(inFront) != target) {
                current.cancel();
            } else {
                return;
            }
        }
        ValueAnimator anim = ValueAnimator.ofFloat(new float[]{getDozeAlpha(inFront), target});
        anim.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                DozeScrimController.this.setDozeAlpha(inFront, ((Float) animation.getAnimatedValue()).floatValue());
            }
        });
        anim.setInterpolator(interpolator);
        anim.setDuration(duration);
        anim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                DozeScrimController.this.setCurrentAnimator(inFront, null);
                if (endRunnable != null) {
                    endRunnable.run();
                }
            }
        });
        anim.start();
        setCurrentAnimator(inFront, anim);
        setCurrentTarget(inFront, target);
    }

    private float getCurrentTarget(boolean inFront) {
        return inFront ? this.mInFrontTarget : this.mBehindTarget;
    }

    private void setCurrentTarget(boolean inFront, float target) {
        if (inFront) {
            this.mInFrontTarget = target;
        } else {
            this.mBehindTarget = target;
        }
    }

    private Animator getCurrentAnimator(boolean inFront) {
        return inFront ? this.mInFrontAnimator : this.mBehindAnimator;
    }

    private void setCurrentAnimator(boolean inFront, Animator animator) {
        if (inFront) {
            this.mInFrontAnimator = animator;
        } else {
            this.mBehindAnimator = animator;
        }
    }

    private void setDozeAlpha(boolean inFront, float alpha) {
        if (inFront) {
            this.mScrimController.setDozeInFrontAlpha(alpha);
        } else {
            this.mScrimController.setDozeBehindAlpha(alpha);
        }
    }

    private float getDozeAlpha(boolean inFront) {
        if (inFront) {
            return this.mScrimController.getDozeInFrontAlpha();
        }
        return this.mScrimController.getDozeBehindAlpha();
    }
}
