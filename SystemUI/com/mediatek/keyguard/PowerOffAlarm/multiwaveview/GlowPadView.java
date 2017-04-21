package com.mediatek.keyguard.PowerOffAlarm.multiwaveview;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.accessibility.AccessibilityManager;
import com.android.keyguard.R$styleable;
import java.util.ArrayList;

public class GlowPadView extends View {
    private int mActiveTarget;
    private boolean mAllowScaling;
    private boolean mAlwaysTrackFinger;
    private boolean mAnimatingTargets;
    private Tweener mBackgroundAnimator;
    private ArrayList<String> mDirectionDescriptions;
    private int mDirectionDescriptionsResourceId;
    private boolean mDragging;
    private int mFeedbackCount;
    private AnimationBundle mGlowAnimations;
    private float mGlowRadius;
    private int mGrabbedState;
    private int mGravity;
    private TargetDrawable mHandleDrawable;
    private int mHorizontalInset;
    private boolean mInitialLayout;
    private float mInnerRadius;
    private int mMaxTargetHeight;
    private int mMaxTargetWidth;
    private int mNewTargetResources;
    private OnTriggerListener mOnTriggerListener;
    private float mOuterRadius;
    private TargetDrawable mOuterRing;
    private PointCloud mPointCloud;
    private int mPointerId;
    private AnimatorListener mResetListener;
    private AnimatorListener mResetListenerWithPing;
    private float mRingScaleFactor;
    private float mSnapMargin;
    private AnimationBundle mTargetAnimations;
    private ArrayList<String> mTargetDescriptions;
    private int mTargetDescriptionsResourceId;
    private ArrayList<TargetDrawable> mTargetDrawables;
    private int mTargetResourceId;
    private AnimatorListener mTargetUpdateListener;
    private AnimatorUpdateListener mUpdateListener;
    private int mVerticalInset;
    private int mVibrationDuration;
    private Vibrator mVibrator;
    private AnimationBundle mWaveAnimations;
    private float mWaveCenterX;
    private float mWaveCenterY;

    public interface OnTriggerListener {
        void onFinishFinalAnimation();

        void onGrabbed(View view, int i);

        void onGrabbedStateChange(View view, int i);

        void onReleased(View view, int i);

        void onTrigger(View view, int i);
    }

    private class AnimationBundle extends ArrayList<Tweener> {
        private static final long serialVersionUID = -6319262269245852568L;
        private boolean mSuspended;

        private AnimationBundle() {
        }

        public void start() {
            if (!this.mSuspended) {
                int count = size();
                for (int i = 0; i < count; i++) {
                    ((Tweener) get(i)).animator.start();
                }
            }
        }

        public void cancel() {
            int count = size();
            for (int i = 0; i < count; i++) {
                ((Tweener) get(i)).animator.cancel();
            }
            clear();
        }

        public void stop() {
            int count = size();
            for (int i = 0; i < count; i++) {
                ((Tweener) get(i)).animator.end();
            }
            clear();
        }
    }

    public GlowPadView(Context context) {
        this(context, null);
    }

    public GlowPadView(Context context, AttributeSet attrs) {
        int i;
        boolean z = false;
        super(context, attrs);
        this.mTargetDrawables = new ArrayList();
        this.mWaveAnimations = new AnimationBundle();
        this.mTargetAnimations = new AnimationBundle();
        this.mGlowAnimations = new AnimationBundle();
        this.mFeedbackCount = 3;
        this.mVibrationDuration = 0;
        this.mActiveTarget = -1;
        this.mRingScaleFactor = 1.0f;
        this.mOuterRadius = 0.0f;
        this.mSnapMargin = 0.0f;
        this.mResetListener = new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                GlowPadView.this.switchToState(0, GlowPadView.this.mWaveCenterX, GlowPadView.this.mWaveCenterY);
                GlowPadView.this.dispatchOnFinishFinalAnimation();
            }
        };
        this.mResetListenerWithPing = new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                GlowPadView.this.ping();
                GlowPadView.this.switchToState(0, GlowPadView.this.mWaveCenterX, GlowPadView.this.mWaveCenterY);
                GlowPadView.this.dispatchOnFinishFinalAnimation();
            }
        };
        this.mUpdateListener = new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                GlowPadView.this.invalidate();
            }
        };
        this.mTargetUpdateListener = new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                if (GlowPadView.this.mNewTargetResources != 0) {
                    GlowPadView.this.internalSetTargetResources(GlowPadView.this.mNewTargetResources);
                    GlowPadView.this.mNewTargetResources = 0;
                    GlowPadView.this.hideTargets(false, false);
                }
                GlowPadView.this.mAnimatingTargets = false;
            }
        };
        this.mGravity = 48;
        this.mInitialLayout = true;
        Resources res = context.getResources();
        TypedArray a = context.obtainStyledAttributes(attrs, R$styleable.GlowPadView);
        this.mInnerRadius = a.getDimension(R$styleable.GlowPadView_innerRadius, this.mInnerRadius);
        this.mOuterRadius = a.getDimension(R$styleable.GlowPadView_outerRadius, this.mOuterRadius);
        this.mSnapMargin = a.getDimension(R$styleable.GlowPadView_snapMargin, this.mSnapMargin);
        this.mVibrationDuration = a.getInt(R$styleable.GlowPadView_vibrationDuration, this.mVibrationDuration);
        this.mFeedbackCount = a.getInt(R$styleable.GlowPadView_feedbackCount, this.mFeedbackCount);
        this.mAllowScaling = a.getBoolean(R$styleable.GlowPadView_allowScaling, false);
        TypedValue handle = a.peekValue(R$styleable.GlowPadView_handleDrawable);
        if (handle != null) {
            i = handle.resourceId;
        } else {
            i = 0;
        }
        this.mHandleDrawable = new TargetDrawable(res, i, 1);
        this.mHandleDrawable.setState(TargetDrawable.STATE_INACTIVE);
        this.mOuterRing = new TargetDrawable(res, getResourceId(a, R$styleable.GlowPadView_outerRingDrawable), 1);
        this.mAlwaysTrackFinger = a.getBoolean(R$styleable.GlowPadView_alwaysTrackFinger, false);
        int pointId = getResourceId(a, R$styleable.GlowPadView_pointDrawable);
        Drawable drawable = pointId != 0 ? res.getDrawable(pointId) : null;
        this.mGlowRadius = a.getDimension(R$styleable.GlowPadView_glowRadius, 0.0f);
        TypedValue outValue = new TypedValue();
        if (a.getValue(R$styleable.GlowPadView_targetDrawables, outValue)) {
            internalSetTargetResources(outValue.resourceId);
        }
        if (this.mTargetDrawables == null || this.mTargetDrawables.size() == 0) {
            throw new IllegalStateException("Must specify at least one target drawable");
        }
        int resourceId;
        if (a.getValue(R$styleable.GlowPadView_targetDescriptions, outValue)) {
            resourceId = outValue.resourceId;
            if (resourceId == 0) {
                throw new IllegalStateException("Must specify target descriptions");
            }
            setTargetDescriptionsResourceId(resourceId);
        }
        if (a.getValue(R$styleable.GlowPadView_directionDescriptions, outValue)) {
            resourceId = outValue.resourceId;
            if (resourceId == 0) {
                throw new IllegalStateException("Must specify direction descriptions");
            }
            setDirectionDescriptionsResourceId(resourceId);
        }
        this.mGravity = a.getInt(R$styleable.GlowPadView_android_gravity, 48);
        a.recycle();
        if (this.mVibrationDuration > 0) {
            z = true;
        }
        setVibrateEnabled(z);
        assignDefaultsIfNeeded();
        this.mPointCloud = new PointCloud(drawable);
        this.mPointCloud.makePointCloud(this.mInnerRadius, this.mOuterRadius);
        this.mPointCloud.glowManager.setRadius(this.mGlowRadius);
    }

    private int getResourceId(TypedArray a, int id) {
        TypedValue tv = a.peekValue(id);
        return tv == null ? 0 : tv.resourceId;
    }

    protected int getSuggestedMinimumWidth() {
        return (int) (Math.max((float) this.mOuterRing.getWidth(), this.mOuterRadius * 2.0f) + ((float) this.mMaxTargetWidth));
    }

    protected int getSuggestedMinimumHeight() {
        return (int) (Math.max((float) this.mOuterRing.getHeight(), this.mOuterRadius * 2.0f) + ((float) this.mMaxTargetHeight));
    }

    protected int getScaledSuggestedMinimumWidth() {
        return (int) ((this.mRingScaleFactor * Math.max((float) this.mOuterRing.getWidth(), this.mOuterRadius * 2.0f)) + ((float) this.mMaxTargetWidth));
    }

    protected int getScaledSuggestedMinimumHeight() {
        return (int) ((this.mRingScaleFactor * Math.max((float) this.mOuterRing.getHeight(), this.mOuterRadius * 2.0f)) + ((float) this.mMaxTargetHeight));
    }

    private int resolveMeasured(int measureSpec, int desired) {
        int specSize = MeasureSpec.getSize(measureSpec);
        switch (MeasureSpec.getMode(measureSpec)) {
            case Integer.MIN_VALUE:
                return Math.min(specSize, desired);
            case 0:
                return desired;
            default:
                return specSize;
        }
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int minimumWidth = getSuggestedMinimumWidth();
        int minimumHeight = getSuggestedMinimumHeight();
        int computedWidth = resolveMeasured(widthMeasureSpec, minimumWidth);
        int computedHeight = resolveMeasured(heightMeasureSpec, minimumHeight);
        this.mRingScaleFactor = computeScaleFactor(minimumWidth, minimumHeight, computedWidth, computedHeight);
        computeInsets(computedWidth - getScaledSuggestedMinimumWidth(), computedHeight - getScaledSuggestedMinimumHeight());
        setMeasuredDimension(computedWidth, computedHeight);
    }

    private void switchToState(int state, float x, float y) {
        switch (state) {
            case 0:
                deactivateTargets();
                hideTargets(false, false);
                hideGlow(0, 0, 0.0f, null);
                startBackgroundAnimation(0, 0.0f);
                this.mHandleDrawable.setState(TargetDrawable.STATE_INACTIVE);
                this.mHandleDrawable.setAlpha(1.0f);
                return;
            case 1:
                startBackgroundAnimation(0, 0.0f);
                return;
            case 2:
                this.mHandleDrawable.setAlpha(0.0f);
                deactivateTargets();
                showTargets(true);
                startBackgroundAnimation(200, 1.0f);
                setGrabbedState(1);
                if (((AccessibilityManager) getContext().getSystemService("accessibility")).isEnabled()) {
                    announceTargets();
                    return;
                }
                return;
            case 3:
                this.mHandleDrawable.setAlpha(0.0f);
                return;
            case 4:
                this.mHandleDrawable.setAlpha(0.0f);
                showGlow(0, 0, 0.0f, null);
                return;
            case 5:
                doFinish();
                return;
            default:
                return;
        }
    }

    private void showGlow(int duration, int delay, float finalAlpha, AnimatorListener finishListener) {
        this.mGlowAnimations.cancel();
        this.mGlowAnimations.add(Tweener.to(this.mPointCloud.glowManager, (long) duration, "ease", Ease$Cubic.easeIn, "delay", Integer.valueOf(delay), "alpha", Float.valueOf(finalAlpha), "onUpdate", this.mUpdateListener, "onComplete", finishListener));
        this.mGlowAnimations.start();
    }

    private void hideGlow(int duration, int delay, float finalAlpha, AnimatorListener finishListener) {
        this.mGlowAnimations.cancel();
        this.mGlowAnimations.add(Tweener.to(this.mPointCloud.glowManager, (long) duration, "ease", Ease$Quart.easeOut, "delay", Integer.valueOf(delay), "alpha", Float.valueOf(finalAlpha), "x", Float.valueOf(0.0f), "y", Float.valueOf(0.0f), "onUpdate", this.mUpdateListener, "onComplete", finishListener));
        this.mGlowAnimations.start();
    }

    private void deactivateTargets() {
        int count = this.mTargetDrawables.size();
        for (int i = 0; i < count; i++) {
            ((TargetDrawable) this.mTargetDrawables.get(i)).setState(TargetDrawable.STATE_INACTIVE);
        }
        this.mActiveTarget = -1;
    }

    private void dispatchTriggerEvent(int whichTarget) {
        vibrate();
        if (this.mOnTriggerListener != null) {
            this.mOnTriggerListener.onTrigger(this, whichTarget);
        }
    }

    private void dispatchOnFinishFinalAnimation() {
        if (this.mOnTriggerListener != null) {
            this.mOnTriggerListener.onFinishFinalAnimation();
        }
    }

    private void doFinish() {
        int activeTarget = this.mActiveTarget;
        if (activeTarget != -1) {
            highlightSelected(activeTarget);
            hideGlow(200, 1200, 0.0f, this.mResetListener);
            dispatchTriggerEvent(activeTarget);
            if (!this.mAlwaysTrackFinger) {
                this.mTargetAnimations.stop();
            }
        } else {
            hideGlow(200, 0, 0.0f, this.mResetListenerWithPing);
            hideTargets(true, false);
        }
        setGrabbedState(0);
    }

    private void highlightSelected(int activeTarget) {
        ((TargetDrawable) this.mTargetDrawables.get(activeTarget)).setState(TargetDrawable.STATE_ACTIVE);
        hideUnselected(activeTarget);
    }

    private void hideUnselected(int active) {
        for (int i = 0; i < this.mTargetDrawables.size(); i++) {
            if (i != active) {
                ((TargetDrawable) this.mTargetDrawables.get(i)).setAlpha(0.0f);
            }
        }
    }

    private void hideTargets(boolean animate, boolean expanded) {
        this.mTargetAnimations.cancel();
        this.mAnimatingTargets = animate;
        int duration = animate ? 200 : 0;
        int delay = animate ? 200 : 0;
        float targetScale = expanded ? 1.0f : 0.8f;
        int length = this.mTargetDrawables.size();
        TimeInterpolator interpolator = Ease$Cubic.easeOut;
        for (int i = 0; i < length; i++) {
            TargetDrawable target = (TargetDrawable) this.mTargetDrawables.get(i);
            target.setState(TargetDrawable.STATE_INACTIVE);
            this.mTargetAnimations.add(Tweener.to(target, (long) duration, "ease", interpolator, "alpha", Float.valueOf(0.0f), "scaleX", Float.valueOf(targetScale), "scaleY", Float.valueOf(targetScale), "delay", Integer.valueOf(delay), "onUpdate", this.mUpdateListener));
        }
        float ringScaleTarget = (expanded ? 1.0f : 0.5f) * this.mRingScaleFactor;
        this.mTargetAnimations.add(Tweener.to(this.mOuterRing, (long) duration, "ease", interpolator, "alpha", Float.valueOf(0.0f), "scaleX", Float.valueOf(ringScaleTarget), "scaleY", Float.valueOf(ringScaleTarget), "delay", Integer.valueOf(delay), "onUpdate", this.mUpdateListener, "onComplete", this.mTargetUpdateListener));
        this.mTargetAnimations.start();
    }

    private void showTargets(boolean animate) {
        this.mTargetAnimations.stop();
        this.mAnimatingTargets = animate;
        int delay = animate ? 50 : 0;
        int duration = animate ? 200 : 0;
        int length = this.mTargetDrawables.size();
        for (int i = 0; i < length; i++) {
            TargetDrawable target = (TargetDrawable) this.mTargetDrawables.get(i);
            target.setState(TargetDrawable.STATE_INACTIVE);
            this.mTargetAnimations.add(Tweener.to(target, (long) duration, "ease", Ease$Cubic.easeOut, "alpha", Float.valueOf(1.0f), "scaleX", Float.valueOf(1.0f), "scaleY", Float.valueOf(1.0f), "delay", Integer.valueOf(delay), "onUpdate", this.mUpdateListener));
        }
        float ringScale = this.mRingScaleFactor * 1.0f;
        this.mTargetAnimations.add(Tweener.to(this.mOuterRing, (long) duration, "ease", Ease$Cubic.easeOut, "alpha", Float.valueOf(1.0f), "scaleX", Float.valueOf(ringScale), "scaleY", Float.valueOf(ringScale), "delay", Integer.valueOf(delay), "onUpdate", this.mUpdateListener, "onComplete", this.mTargetUpdateListener));
        this.mTargetAnimations.start();
    }

    private void vibrate() {
        if (this.mVibrator != null) {
            this.mVibrator.vibrate((long) this.mVibrationDuration);
        }
    }

    private ArrayList<TargetDrawable> loadDrawableArray(int resourceId) {
        Resources res = getContext().getResources();
        TypedArray array = res.obtainTypedArray(resourceId);
        int count = array.length();
        ArrayList<TargetDrawable> drawables = new ArrayList(count);
        for (int i = 0; i < count; i++) {
            TypedValue value = array.peekValue(i);
            drawables.add(new TargetDrawable(res, value != null ? value.resourceId : 0, 3));
        }
        array.recycle();
        return drawables;
    }

    private void internalSetTargetResources(int resourceId) {
        ArrayList<TargetDrawable> targets = loadDrawableArray(resourceId);
        this.mTargetDrawables = targets;
        this.mTargetResourceId = resourceId;
        int maxWidth = this.mHandleDrawable.getWidth();
        int maxHeight = this.mHandleDrawable.getHeight();
        int count = targets.size();
        for (int i = 0; i < count; i++) {
            TargetDrawable target = (TargetDrawable) targets.get(i);
            maxWidth = Math.max(maxWidth, target.getWidth());
            maxHeight = Math.max(maxHeight, target.getHeight());
        }
        if (this.mMaxTargetWidth == maxWidth && this.mMaxTargetHeight == maxHeight) {
            updateTargetPositions(this.mWaveCenterX, this.mWaveCenterY);
            updatePointCloudPosition(this.mWaveCenterX, this.mWaveCenterY);
            return;
        }
        this.mMaxTargetWidth = maxWidth;
        this.mMaxTargetHeight = maxHeight;
        requestLayout();
    }

    public void setTargetDescriptionsResourceId(int resourceId) {
        this.mTargetDescriptionsResourceId = resourceId;
        if (this.mTargetDescriptions != null) {
            this.mTargetDescriptions.clear();
        }
    }

    public void setDirectionDescriptionsResourceId(int resourceId) {
        this.mDirectionDescriptionsResourceId = resourceId;
        if (this.mDirectionDescriptions != null) {
            this.mDirectionDescriptions.clear();
        }
    }

    public void setVibrateEnabled(boolean enabled) {
        if (enabled && this.mVibrator == null) {
            this.mVibrator = (Vibrator) getContext().getSystemService("vibrator");
        } else {
            this.mVibrator = null;
        }
    }

    public void ping() {
        if (this.mFeedbackCount > 0) {
            boolean doWaveAnimation = true;
            AnimationBundle waveAnimations = this.mWaveAnimations;
            if (waveAnimations.size() > 0 && ((Tweener) waveAnimations.get(0)).animator.isRunning() && ((Tweener) waveAnimations.get(0)).animator.getCurrentPlayTime() < 675) {
                doWaveAnimation = false;
            }
            if (doWaveAnimation) {
                startWaveAnimation();
            }
        }
    }

    private void stopAndHideWaveAnimation() {
        this.mWaveAnimations.cancel();
        this.mPointCloud.waveManager.setAlpha(0.0f);
    }

    private void startWaveAnimation() {
        this.mWaveAnimations.cancel();
        this.mPointCloud.waveManager.setAlpha(1.0f);
        this.mPointCloud.waveManager.setRadius(((float) this.mHandleDrawable.getWidth()) / 2.0f);
        this.mWaveAnimations.add(Tweener.to(this.mPointCloud.waveManager, 1350, "ease", Ease$Quad.easeOut, "delay", Integer.valueOf(0), "radius", Float.valueOf(this.mOuterRadius * 2.0f), "onUpdate", this.mUpdateListener, "onComplete", new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                GlowPadView.this.mPointCloud.waveManager.setRadius(0.0f);
                GlowPadView.this.mPointCloud.waveManager.setAlpha(0.0f);
            }
        }));
        this.mWaveAnimations.start();
    }

    private void startBackgroundAnimation(int duration, float alpha) {
        Drawable background = getBackground();
        if (this.mAlwaysTrackFinger && background != null) {
            if (this.mBackgroundAnimator != null) {
                this.mBackgroundAnimator.animator.cancel();
            }
            this.mBackgroundAnimator = Tweener.to(background, (long) duration, "ease", Ease$Cubic.easeIn, "alpha", Integer.valueOf((int) (255.0f * alpha)), "delay", Integer.valueOf(50));
            this.mBackgroundAnimator.animator.start();
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = false;
        switch (event.getActionMasked()) {
            case 0:
            case 5:
                handleDown(event);
                handleMove(event);
                handled = true;
                break;
            case 1:
            case 6:
                handleMove(event);
                handleUp(event);
                handled = true;
                break;
            case 2:
                handleMove(event);
                handled = true;
                break;
            case 3:
                handleMove(event);
                handleCancel(event);
                handled = true;
                break;
        }
        invalidate();
        if (handled) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void updateGlowPosition(float x, float y) {
        float dy = (y - this.mOuterRing.getY()) * (1.0f / this.mRingScaleFactor);
        this.mPointCloud.glowManager.setX(this.mOuterRing.getX() + ((x - this.mOuterRing.getX()) * (1.0f / this.mRingScaleFactor)));
        this.mPointCloud.glowManager.setY(this.mOuterRing.getY() + dy);
    }

    private void handleDown(MotionEvent event) {
        int actionIndex = event.getActionIndex();
        float eventX = event.getX(actionIndex);
        float eventY = event.getY(actionIndex);
        switchToState(1, eventX, eventY);
        if (trySwitchToFirstTouchState(eventX, eventY)) {
            this.mPointerId = event.getPointerId(actionIndex);
            updateGlowPosition(eventX, eventY);
            return;
        }
        this.mDragging = false;
    }

    private void handleUp(MotionEvent event) {
        int actionIndex = event.getActionIndex();
        if (event.getPointerId(actionIndex) == this.mPointerId) {
            switchToState(5, event.getX(actionIndex), event.getY(actionIndex));
        }
    }

    private void handleCancel(MotionEvent event) {
        int actionIndex = event.findPointerIndex(this.mPointerId);
        if (actionIndex == -1) {
            actionIndex = 0;
        }
        switchToState(5, event.getX(actionIndex), event.getY(actionIndex));
    }

    private void handleMove(MotionEvent event) {
        int activeTarget = -1;
        int historySize = event.getHistorySize();
        ArrayList<TargetDrawable> targets = this.mTargetDrawables;
        int ntargets = targets.size();
        float x = 0.0f;
        float y = 0.0f;
        int actionIndex = event.findPointerIndex(this.mPointerId);
        if (actionIndex != -1) {
            for (int k = 0; k < historySize + 1; k++) {
                float eventX;
                float eventY;
                if (k < historySize) {
                    eventX = event.getHistoricalX(actionIndex, k);
                } else {
                    eventX = event.getX(actionIndex);
                }
                if (k < historySize) {
                    eventY = event.getHistoricalY(actionIndex, k);
                } else {
                    eventY = event.getY(actionIndex);
                }
                float tx = eventX - this.mWaveCenterX;
                float ty = eventY - this.mWaveCenterY;
                float touchRadius = (float) Math.hypot((double) tx, (double) ty);
                float scale = touchRadius > this.mOuterRadius ? this.mOuterRadius / touchRadius : 1.0f;
                float limitX = tx * scale;
                float limitY = ty * scale;
                double angleRad = Math.atan2((double) (-ty), (double) tx);
                if (!this.mDragging) {
                    trySwitchToFirstTouchState(eventX, eventY);
                }
                if (this.mDragging) {
                    float snapRadius = (this.mRingScaleFactor * this.mOuterRadius) - this.mSnapMargin;
                    float snapDistance2 = snapRadius * snapRadius;
                    for (int i = 0; i < ntargets; i++) {
                        double targetMinRad = (((((double) i) - 0.5d) * 2.0d) * 3.141592653589793d) / ((double) ntargets);
                        double targetMaxRad = (((((double) i) + 0.5d) * 2.0d) * 3.141592653589793d) / ((double) ntargets);
                        if (((TargetDrawable) targets.get(i)).isEnabled()) {
                            boolean angleMatches = (angleRad <= targetMinRad || angleRad > targetMaxRad) ? 6.283185307179586d + angleRad > targetMinRad ? 6.283185307179586d + angleRad <= targetMaxRad : false : true;
                            if (angleMatches && dist2(tx, ty) > snapDistance2) {
                                activeTarget = i;
                            }
                        }
                    }
                }
                x = limitX;
                y = limitY;
            }
            if (this.mDragging) {
                if (activeTarget != -1) {
                    switchToState(4, x, y);
                    updateGlowPosition(x, y);
                } else {
                    switchToState(3, x, y);
                    updateGlowPosition(x, y);
                }
                if (this.mActiveTarget != activeTarget) {
                    if (this.mActiveTarget != -1) {
                        ((TargetDrawable) targets.get(this.mActiveTarget)).setState(TargetDrawable.STATE_INACTIVE);
                    }
                    if (activeTarget != -1) {
                        ((TargetDrawable) targets.get(activeTarget)).setState(TargetDrawable.STATE_FOCUSED);
                        if (((AccessibilityManager) getContext().getSystemService("accessibility")).isEnabled()) {
                            announceForAccessibility(getTargetDescription(activeTarget));
                        }
                    }
                }
                this.mActiveTarget = activeTarget;
            }
        }
    }

    public boolean onHoverEvent(MotionEvent event) {
        if (((AccessibilityManager) getContext().getSystemService("accessibility")).isTouchExplorationEnabled()) {
            int action = event.getAction();
            switch (action) {
                case 7:
                    event.setAction(2);
                    break;
                case 9:
                    event.setAction(0);
                    break;
                case 10:
                    event.setAction(1);
                    break;
            }
            onTouchEvent(event);
            event.setAction(action);
        }
        super.onHoverEvent(event);
        return true;
    }

    private void setGrabbedState(int newState) {
        if (newState != this.mGrabbedState) {
            if (newState != 0) {
                vibrate();
            }
            this.mGrabbedState = newState;
            if (this.mOnTriggerListener != null) {
                if (newState == 0) {
                    this.mOnTriggerListener.onReleased(this, 1);
                } else {
                    this.mOnTriggerListener.onGrabbed(this, 1);
                }
                this.mOnTriggerListener.onGrabbedStateChange(this, newState);
            }
        }
    }

    private boolean trySwitchToFirstTouchState(float x, float y) {
        float tx = x - this.mWaveCenterX;
        float ty = y - this.mWaveCenterY;
        if (!this.mAlwaysTrackFinger && dist2(tx, ty) > getScaledGlowRadiusSquared()) {
            return false;
        }
        switchToState(2, x, y);
        updateGlowPosition(tx, ty);
        this.mDragging = true;
        return true;
    }

    private void assignDefaultsIfNeeded() {
        if (this.mOuterRadius == 0.0f) {
            this.mOuterRadius = ((float) Math.max(this.mOuterRing.getWidth(), this.mOuterRing.getHeight())) / 2.0f;
        }
        if (this.mSnapMargin == 0.0f) {
            this.mSnapMargin = TypedValue.applyDimension(1, 20.0f, getContext().getResources().getDisplayMetrics());
        }
        if (this.mInnerRadius == 0.0f) {
            this.mInnerRadius = ((float) this.mHandleDrawable.getWidth()) / 10.0f;
        }
    }

    private void computeInsets(int dx, int dy) {
        int absoluteGravity = Gravity.getAbsoluteGravity(this.mGravity, getLayoutDirection());
        switch (absoluteGravity & 7) {
            case 3:
                this.mHorizontalInset = 0;
                break;
            case 5:
                this.mHorizontalInset = dx;
                break;
            default:
                this.mHorizontalInset = dx / 2;
                break;
        }
        switch (absoluteGravity & 112) {
            case 48:
                this.mVerticalInset = 0;
                return;
            case 80:
                this.mVerticalInset = dy;
                return;
            default:
                this.mVerticalInset = dy / 2;
                return;
        }
    }

    private float computeScaleFactor(int desiredWidth, int desiredHeight, int actualWidth, int actualHeight) {
        if (!this.mAllowScaling) {
            return 1.0f;
        }
        int absoluteGravity = Gravity.getAbsoluteGravity(this.mGravity, getLayoutDirection());
        float scaleX = 1.0f;
        float scaleY = 1.0f;
        switch (absoluteGravity & 7) {
            case 3:
            case 5:
                break;
            default:
                if (desiredWidth > actualWidth) {
                    scaleX = ((((float) actualWidth) * 1.0f) - ((float) this.mMaxTargetWidth)) / ((float) (desiredWidth - this.mMaxTargetWidth));
                    break;
                }
                break;
        }
        switch (absoluteGravity & 112) {
            case 48:
            case 80:
                break;
            default:
                if (desiredHeight > actualHeight) {
                    scaleY = ((((float) actualHeight) * 1.0f) - ((float) this.mMaxTargetHeight)) / ((float) (desiredHeight - this.mMaxTargetHeight));
                    break;
                }
                break;
        }
        return Math.min(scaleX, scaleY);
    }

    private float getRingWidth() {
        return this.mRingScaleFactor * Math.max((float) this.mOuterRing.getWidth(), this.mOuterRadius * 2.0f);
    }

    private float getRingHeight() {
        return this.mRingScaleFactor * Math.max((float) this.mOuterRing.getHeight(), this.mOuterRadius * 2.0f);
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int width = right - left;
        int height = bottom - top;
        float placementWidth = getRingWidth();
        float newWaveCenterX = ((float) this.mHorizontalInset) + ((((float) this.mMaxTargetWidth) + placementWidth) / 2.0f);
        float newWaveCenterY = ((float) this.mVerticalInset) + ((((float) this.mMaxTargetHeight) + getRingHeight()) / 2.0f);
        if (this.mInitialLayout) {
            stopAndHideWaveAnimation();
            hideTargets(false, false);
            this.mInitialLayout = false;
        }
        this.mOuterRing.setPositionX(newWaveCenterX);
        this.mOuterRing.setPositionY(newWaveCenterY);
        this.mPointCloud.setScale(this.mRingScaleFactor);
        this.mHandleDrawable.setPositionX(newWaveCenterX);
        this.mHandleDrawable.setPositionY(newWaveCenterY);
        updateTargetPositions(newWaveCenterX, newWaveCenterY);
        updatePointCloudPosition(newWaveCenterX, newWaveCenterY);
        updateGlowPosition(newWaveCenterX, newWaveCenterY);
        this.mWaveCenterX = newWaveCenterX;
        this.mWaveCenterY = newWaveCenterY;
    }

    private void updateTargetPositions(float centerX, float centerY) {
        ArrayList<TargetDrawable> targets = this.mTargetDrawables;
        int size = targets.size();
        float alpha = (float) (-6.283185307179586d / ((double) size));
        for (int i = 0; i < size; i++) {
            TargetDrawable targetIcon = (TargetDrawable) targets.get(i);
            float angle = alpha * ((float) i);
            targetIcon.setPositionX(centerX);
            targetIcon.setPositionY(centerY);
            targetIcon.setX((getRingWidth() / 2.0f) * ((float) Math.cos((double) angle)));
            targetIcon.setY((getRingHeight() / 2.0f) * ((float) Math.sin((double) angle)));
        }
    }

    private void updatePointCloudPosition(float centerX, float centerY) {
        this.mPointCloud.setCenter(centerX, centerY);
    }

    protected void onDraw(Canvas canvas) {
        this.mPointCloud.draw(canvas);
        this.mOuterRing.draw(canvas);
        int ntargets = this.mTargetDrawables.size();
        for (int i = 0; i < ntargets; i++) {
            TargetDrawable target = (TargetDrawable) this.mTargetDrawables.get(i);
            if (target != null) {
                target.draw(canvas);
            }
        }
        this.mHandleDrawable.draw(canvas);
    }

    public void setOnTriggerListener(OnTriggerListener listener) {
        this.mOnTriggerListener = listener;
    }

    private float square(float d) {
        return d * d;
    }

    private float dist2(float dx, float dy) {
        return (dx * dx) + (dy * dy);
    }

    private float getScaledGlowRadiusSquared() {
        float scaledTapRadius;
        if (((AccessibilityManager) getContext().getSystemService("accessibility")).isEnabled()) {
            scaledTapRadius = 1.3f * this.mGlowRadius;
        } else {
            scaledTapRadius = this.mGlowRadius;
        }
        return square(scaledTapRadius);
    }

    private void announceTargets() {
        StringBuilder utterance = new StringBuilder();
        int targetCount = this.mTargetDrawables.size();
        for (int i = 0; i < targetCount; i++) {
            String targetDescription = getTargetDescription(i);
            String directionDescription = getDirectionDescription(i);
            if (!(TextUtils.isEmpty(targetDescription) || TextUtils.isEmpty(directionDescription))) {
                utterance.append(String.format(directionDescription, new Object[]{targetDescription}));
            }
        }
        if (utterance.length() > 0) {
            announceForAccessibility(utterance.toString());
        }
    }

    private String getTargetDescription(int index) {
        if (this.mTargetDescriptions == null || this.mTargetDescriptions.isEmpty()) {
            this.mTargetDescriptions = loadDescriptions(this.mTargetDescriptionsResourceId);
            if (this.mTargetDrawables.size() != this.mTargetDescriptions.size()) {
                Log.w("GlowPadView", "The number of target drawables must be equal to the number of target descriptions.");
                return null;
            }
        }
        return (String) this.mTargetDescriptions.get(index);
    }

    private String getDirectionDescription(int index) {
        if (this.mDirectionDescriptions == null || this.mDirectionDescriptions.isEmpty()) {
            this.mDirectionDescriptions = loadDescriptions(this.mDirectionDescriptionsResourceId);
            if (this.mTargetDrawables.size() != this.mDirectionDescriptions.size()) {
                Log.w("GlowPadView", "The number of target drawables must be equal to the number of direction descriptions.");
                return null;
            }
        }
        return (String) this.mDirectionDescriptions.get(index);
    }

    private ArrayList<String> loadDescriptions(int resourceId) {
        TypedArray array = getContext().getResources().obtainTypedArray(resourceId);
        int count = array.length();
        ArrayList<String> targetContentDescriptions = new ArrayList(count);
        for (int i = 0; i < count; i++) {
            targetContentDescriptions.add(array.getString(i));
        }
        array.recycle();
        return targetContentDescriptions;
    }

    public int getResourceIdForTarget(int index) {
        TargetDrawable drawable = (TargetDrawable) this.mTargetDrawables.get(index);
        return drawable == null ? 0 : drawable.getResourceId();
    }
}
