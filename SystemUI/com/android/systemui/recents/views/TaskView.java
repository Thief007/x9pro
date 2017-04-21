package com.android.systemui.recents.views;

import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewOutlineProvider;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.Task.TaskCallbacks;
import com.android.systemui.statusbar.phone.PhoneStatusBar;

public class TaskView extends FrameLayout implements TaskCallbacks, OnClickListener, OnLongClickListener {
    float mActionButtonTranslationZ;
    View mActionButtonView;
    TaskViewCallbacks mCb;
    boolean mClipViewInStack;
    RecentsConfiguration mConfig;
    View mContent;
    int mDimAlpha;
    PorterDuffColorFilter mDimColorFilter;
    AccelerateInterpolator mDimInterpolator;
    Paint mDimLayerPaint;
    boolean mFocusAnimationsEnabled;
    TaskViewHeader mHeaderView;
    boolean mIsFocused;
    float mMaxDimScale;
    Task mTask;
    boolean mTaskDataLoaded;
    float mTaskProgress;
    ObjectAnimator mTaskProgressAnimator;
    TaskViewThumbnail mThumbnailView;
    AnimatorUpdateListener mUpdateDimListener;
    AnimateableViewBounds mViewBounds;

    interface TaskViewCallbacks {
        void onTaskResize(TaskView taskView);

        void onTaskViewAppInfoClicked(TaskView taskView);

        void onTaskViewClicked(TaskView taskView, Task task, boolean z);

        void onTaskViewClipStateChanged(TaskView taskView);

        void onTaskViewDismissed(TaskView taskView);

        void onTaskViewFocusChanged(TaskView taskView, boolean z);
    }

    public TaskView(Context context) {
        this(context, null);
    }

    public TaskView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TaskView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TaskView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mDimInterpolator = new AccelerateInterpolator(1.0f);
        this.mDimColorFilter = new PorterDuffColorFilter(0, Mode.SRC_ATOP);
        this.mDimLayerPaint = new Paint();
        this.mUpdateDimListener = new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                TaskView.this.setTaskProgress(((Float) animation.getAnimatedValue()).floatValue());
            }
        };
        this.mConfig = RecentsConfiguration.getInstance();
        this.mMaxDimScale = ((float) this.mConfig.taskStackMaxDim) / 255.0f;
        this.mClipViewInStack = true;
        this.mViewBounds = new AnimateableViewBounds(this, this.mConfig.taskViewRoundedCornerRadiusPx);
        setTaskProgress(getTaskProgress());
        setDim(getDim());
        if (this.mConfig.fakeShadows) {
            setBackground(new FakeShadowDrawable(context.getResources(), this.mConfig));
        }
        setOutlineProvider(this.mViewBounds);
    }

    void setCallbacks(TaskViewCallbacks cb) {
        this.mCb = cb;
    }

    void reset() {
        resetViewProperties();
        resetNoUserInteractionState();
        setClipViewInStack(false);
        setCallbacks(null);
    }

    Task getTask() {
        return this.mTask;
    }

    AnimateableViewBounds getViewBounds() {
        return this.mViewBounds;
    }

    protected void onFinishInflate() {
        this.mContent = findViewById(R.id.task_view_content);
        this.mHeaderView = (TaskViewHeader) findViewById(R.id.task_view_bar);
        this.mThumbnailView = (TaskViewThumbnail) findViewById(R.id.task_view_thumbnail);
        this.mThumbnailView.updateClipToTaskBar(this.mHeaderView);
        this.mActionButtonView = findViewById(R.id.lock_to_app_fab);
        this.mActionButtonView.setOutlineProvider(new ViewOutlineProvider() {
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, TaskView.this.mActionButtonView.getWidth(), TaskView.this.mActionButtonView.getHeight());
            }
        });
        this.mActionButtonTranslationZ = this.mActionButtonView.getTranslationZ();
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int widthWithoutPadding = (width - this.mPaddingLeft) - this.mPaddingRight;
        int heightWithoutPadding = (height - this.mPaddingTop) - this.mPaddingBottom;
        this.mContent.measure(MeasureSpec.makeMeasureSpec(widthWithoutPadding, 1073741824), MeasureSpec.makeMeasureSpec(widthWithoutPadding, 1073741824));
        this.mHeaderView.measure(MeasureSpec.makeMeasureSpec(widthWithoutPadding, 1073741824), MeasureSpec.makeMeasureSpec(this.mConfig.taskBarHeight, 1073741824));
        this.mActionButtonView.measure(MeasureSpec.makeMeasureSpec(widthWithoutPadding, Integer.MIN_VALUE), MeasureSpec.makeMeasureSpec(heightWithoutPadding, Integer.MIN_VALUE));
        this.mThumbnailView.measure(MeasureSpec.makeMeasureSpec(widthWithoutPadding, 1073741824), MeasureSpec.makeMeasureSpec(widthWithoutPadding, 1073741824));
        setMeasuredDimension(width, height);
        invalidateOutline();
    }

    void updateViewPropertiesToTaskTransform(TaskViewTransform toTransform, int duration) {
        updateViewPropertiesToTaskTransform(toTransform, duration, null);
    }

    void updateViewPropertiesToTaskTransform(TaskViewTransform toTransform, int duration, AnimatorUpdateListener updateCallback) {
        boolean z;
        Interpolator interpolator = this.mConfig.fastOutSlowInInterpolator;
        if (this.mConfig.fakeShadows) {
            z = false;
        } else {
            z = true;
        }
        toTransform.applyToTaskView(this, duration, interpolator, false, z, updateCallback);
        Utilities.cancelAnimationWithoutCallbacks(this.mTaskProgressAnimator);
        if (duration <= 0) {
            setTaskProgress(toTransform.p);
            return;
        }
        this.mTaskProgressAnimator = ObjectAnimator.ofFloat(this, "taskProgress", new float[]{toTransform.p});
        this.mTaskProgressAnimator.setDuration((long) duration);
        this.mTaskProgressAnimator.addUpdateListener(this.mUpdateDimListener);
        this.mTaskProgressAnimator.start();
    }

    void resetViewProperties() {
        setDim(0);
        setLayerType(0, null);
        TaskViewTransform.reset(this);
        if (this.mActionButtonView != null) {
            this.mActionButtonView.setScaleX(1.0f);
            this.mActionButtonView.setScaleY(1.0f);
            this.mActionButtonView.setAlpha(1.0f);
            this.mActionButtonView.setTranslationZ(this.mActionButtonTranslationZ);
        }
    }

    void prepareEnterRecentsAnimation(boolean isTaskViewLaunchTargetTask, boolean occludesLaunchTarget, int offscreenY) {
        int initialDim = getDim();
        if (!this.mConfig.launchedHasConfigurationChanged) {
            if (this.mConfig.launchedFromAppWithThumbnail) {
                if (isTaskViewLaunchTargetTask) {
                    initialDim = 0;
                    this.mActionButtonView.setAlpha(0.0f);
                } else if (occludesLaunchTarget) {
                    setTranslationY((float) offscreenY);
                }
            } else if (this.mConfig.launchedFromHome) {
                setTranslationY((float) offscreenY);
                setTranslationZ(0.0f);
                setScaleX(1.0f);
                setScaleY(1.0f);
            }
        }
        setDim(initialDim);
        this.mThumbnailView.prepareEnterRecentsAnimation(isTaskViewLaunchTargetTask);
    }

    void startEnterRecentsAnimation(final ViewAnimation$TaskViewEnterContext ctx) {
        TaskViewTransform transform = ctx.currentTaskTransform;
        int startDelay = 0;
        if (this.mConfig.launchedFromAppWithThumbnail) {
            if (this.mTask.isLaunchTarget) {
                animateDimToProgress(this.mConfig.transitionEnterFromAppDelay, this.mConfig.taskViewEnterFromAppDuration, ctx.postAnimationTrigger.decrementOnAnimationEnd());
                ctx.postAnimationTrigger.increment();
                fadeInActionButton(this.mConfig.transitionEnterFromAppDelay, this.mConfig.taskViewEnterFromAppDuration);
            } else if (ctx.currentTaskOccludesLaunchTarget) {
                setTranslationY((float) (transform.translationY + this.mConfig.taskViewAffiliateGroupEnterOffsetPx));
                setAlpha(0.0f);
                animate().alpha(1.0f).translationY((float) transform.translationY).setStartDelay((long) this.mConfig.transitionEnterFromAppDelay).setUpdateListener(null).setInterpolator(this.mConfig.fastOutSlowInInterpolator).setDuration((long) this.mConfig.taskViewEnterFromHomeDuration).withEndAction(new Runnable() {
                    public void run() {
                        ctx.postAnimationTrigger.decrement();
                    }
                }).start();
                ctx.postAnimationTrigger.increment();
            }
            startDelay = this.mConfig.transitionEnterFromAppDelay;
        } else if (this.mConfig.launchedFromHome) {
            int frontIndex = (ctx.currentStackViewCount - ctx.currentStackViewIndex) - 1;
            int delay = this.mConfig.transitionEnterFromHomeDelay + (this.mConfig.taskViewEnterFromHomeStaggerDelay * frontIndex);
            setScaleX(transform.scale);
            setScaleY(transform.scale);
            if (!this.mConfig.fakeShadows) {
                animate().translationZ(transform.translationZ);
            }
            animate().translationY((float) transform.translationY).setStartDelay((long) delay).setUpdateListener(ctx.updateListener).setInterpolator(this.mConfig.quintOutInterpolator).setDuration((long) (this.mConfig.taskViewEnterFromHomeDuration + (this.mConfig.taskViewEnterFromHomeStaggerDelay * frontIndex))).withEndAction(new Runnable() {
                public void run() {
                    ctx.postAnimationTrigger.decrement();
                }
            }).start();
            ctx.postAnimationTrigger.increment();
            startDelay = delay;
        }
        postDelayed(new Runnable() {
            public void run() {
                TaskView.this.enableFocusAnimations();
            }
        }, (long) startDelay);
    }

    public void fadeInActionButton(int delay, int duration) {
        this.mActionButtonView.setAlpha(0.0f);
        this.mActionButtonView.animate().alpha(1.0f).setStartDelay((long) delay).setDuration((long) duration).setInterpolator(PhoneStatusBar.ALPHA_IN).start();
    }

    void startExitToHomeAnimation(ViewAnimation$TaskViewExitContext ctx) {
        animate().translationY((float) ctx.offscreenTranslationY).setStartDelay(0).setUpdateListener(null).setInterpolator(this.mConfig.fastOutLinearInInterpolator).setDuration((long) this.mConfig.taskViewExitToHomeDuration).withEndAction(ctx.postAnimationTrigger.decrementAsRunnable()).start();
        ctx.postAnimationTrigger.increment();
    }

    void startLaunchTaskAnimation(Runnable postAnimRunnable, boolean isLaunchingTask, boolean occludesLaunchTarget, boolean lockToTask) {
        if (isLaunchingTask) {
            this.mThumbnailView.startLaunchTaskAnimation(postAnimRunnable);
            if (this.mDimAlpha > 0) {
                ObjectAnimator anim = ObjectAnimator.ofInt(this, "dim", new int[]{0});
                anim.setDuration((long) this.mConfig.taskViewExitToAppDuration);
                anim.setInterpolator(this.mConfig.fastOutLinearInInterpolator);
                anim.start();
            }
            if (!lockToTask) {
                this.mActionButtonView.animate().scaleX(0.9f).scaleY(0.9f);
            }
            this.mActionButtonView.animate().alpha(0.0f).setStartDelay(0).setDuration((long) this.mConfig.taskViewExitToAppDuration).setInterpolator(this.mConfig.fastOutLinearInInterpolator).start();
            return;
        }
        this.mHeaderView.startLaunchTaskDismissAnimation();
        if (occludesLaunchTarget) {
            animate().alpha(0.0f).translationY(getTranslationY() + ((float) this.mConfig.taskViewAffiliateGroupEnterOffsetPx)).setStartDelay(0).setUpdateListener(null).setInterpolator(this.mConfig.fastOutLinearInInterpolator).setDuration((long) this.mConfig.taskViewExitToAppDuration).start();
        }
    }

    void startDeleteTaskAnimation(final Runnable r, int delay) {
        setClipViewInStack(false);
        animate().translationX((float) this.mConfig.taskViewRemoveAnimTranslationXPx).alpha(0.0f).setStartDelay((long) delay).setUpdateListener(null).setInterpolator(this.mConfig.fastOutSlowInInterpolator).setDuration((long) this.mConfig.taskViewRemoveAnimDuration).withEndAction(new Runnable() {
            public void run() {
                if (r != null) {
                    r.run();
                }
                TaskView.this.setClipViewInStack(true);
            }
        }).start();
    }

    void setTouchEnabled(boolean enabled) {
        setOnClickListener(enabled ? this : null);
    }

    void startNoUserInteractionAnimation() {
        this.mHeaderView.startNoUserInteractionAnimation();
    }

    void setNoUserInteractionState() {
        this.mHeaderView.setNoUserInteractionState();
    }

    void resetNoUserInteractionState() {
        this.mHeaderView.resetNoUserInteractionState();
    }

    void dismissTask() {
        final TaskView tv = this;
        startDeleteTaskAnimation(new Runnable() {
            public void run() {
                if (TaskView.this.mCb != null) {
                    TaskView.this.mCb.onTaskViewDismissed(tv);
                }
            }
        }, 0);
    }

    boolean shouldClipViewInStack() {
        return this.mClipViewInStack && getVisibility() == 0;
    }

    void setClipViewInStack(boolean clip) {
        if (clip != this.mClipViewInStack) {
            this.mClipViewInStack = clip;
            if (this.mCb != null) {
                this.mCb.onTaskViewClipStateChanged(this);
            }
        }
    }

    public void setTaskProgress(float p) {
        this.mTaskProgress = p;
        this.mViewBounds.setAlpha(p);
        updateDimFromTaskProgress();
    }

    public float getTaskProgress() {
        return this.mTaskProgress;
    }

    public void setDim(int dim) {
        this.mDimAlpha = dim;
        if (!this.mConfig.useHardwareLayers) {
            float dimAlpha = ((float) this.mDimAlpha) / 255.0f;
            if (this.mThumbnailView != null) {
                this.mThumbnailView.setDimAlpha(dimAlpha);
            }
            if (this.mHeaderView != null) {
                this.mHeaderView.setDimAlpha(dim);
            }
        } else if (getMeasuredWidth() > 0 && getMeasuredHeight() > 0) {
            this.mDimColorFilter.setColor(Color.argb(this.mDimAlpha, 0, 0, 0));
            this.mDimLayerPaint.setColorFilter(this.mDimColorFilter);
            this.mContent.setLayerType(2, this.mDimLayerPaint);
        }
    }

    public int getDim() {
        return this.mDimAlpha;
    }

    void animateDimToProgress(int delay, int duration, AnimatorListener postAnimRunnable) {
        if (getDimFromTaskProgress() != getDim()) {
            ObjectAnimator anim = ObjectAnimator.ofInt(this, "dim", new int[]{getDimFromTaskProgress()});
            anim.setStartDelay((long) delay);
            anim.setDuration((long) duration);
            if (postAnimRunnable != null) {
                anim.addListener(postAnimRunnable);
            }
            anim.start();
        }
    }

    int getDimFromTaskProgress() {
        return (int) (255.0f * (this.mMaxDimScale * this.mDimInterpolator.getInterpolation(1.0f - this.mTaskProgress)));
    }

    void updateDimFromTaskProgress() {
        setDim(getDimFromTaskProgress());
    }

    public void setFocusedTask(boolean animateFocusedState) {
        this.mIsFocused = true;
        if (this.mFocusAnimationsEnabled) {
            this.mHeaderView.onTaskViewFocusChanged(true, animateFocusedState);
        }
        this.mThumbnailView.onFocusChanged(true);
        if (this.mCb != null) {
            this.mCb.onTaskViewFocusChanged(this, true);
        }
        setFocusableInTouchMode(true);
        requestFocus();
        setFocusableInTouchMode(false);
        invalidate();
    }

    void unsetFocusedTask() {
        this.mIsFocused = false;
        if (this.mFocusAnimationsEnabled) {
            this.mHeaderView.onTaskViewFocusChanged(false, true);
        }
        this.mThumbnailView.onFocusChanged(false);
        if (this.mCb != null) {
            this.mCb.onTaskViewFocusChanged(this, false);
        }
        invalidate();
    }

    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (!gainFocus) {
            unsetFocusedTask();
        }
    }

    public boolean isFocusedTask() {
        return !this.mIsFocused ? isFocused() : true;
    }

    void enableFocusAnimations() {
        boolean wasFocusAnimationsEnabled = this.mFocusAnimationsEnabled;
        this.mFocusAnimationsEnabled = true;
        if (this.mIsFocused && !wasFocusAnimationsEnabled) {
            this.mHeaderView.onTaskViewFocusChanged(true, true);
        }
    }

    public void disableLayersForOneFrame() {
        this.mHeaderView.disableLayersForOneFrame();
    }

    public void onTaskBound(Task t) {
        this.mTask = t;
        this.mTask.setCallbacks(this);
        int lockButtonVisibility = (t.lockToTaskEnabled && t.lockToThisTask) ? 0 : 8;
        if (this.mActionButtonView.getVisibility() != lockButtonVisibility) {
            this.mActionButtonView.setVisibility(lockButtonVisibility);
            requestLayout();
        }
    }

    public void onTaskDataLoaded() {
        if (!(this.mThumbnailView == null || this.mHeaderView == null)) {
            this.mThumbnailView.rebindToTask(this.mTask);
            this.mHeaderView.rebindToTask(this.mTask);
            AccessibilityManager am = (AccessibilityManager) getContext().getSystemService("accessibility");
            if (am != null && am.isEnabled()) {
                this.mHeaderView.mApplicationIcon.setOnClickListener(this);
            }
            this.mHeaderView.mDismissButton.setOnClickListener(this);
            if (this.mConfig.multiStackEnabled) {
                this.mHeaderView.mMoveTaskButton.setOnClickListener(this);
            }
            this.mActionButtonView.setOnClickListener(this);
            this.mHeaderView.mApplicationIcon.setOnLongClickListener(this);
        }
        this.mTaskDataLoaded = true;
    }

    public void onTaskDataUnloaded() {
        if (!(this.mThumbnailView == null || this.mHeaderView == null)) {
            this.mTask.setCallbacks(null);
            this.mThumbnailView.unbindFromTask();
            this.mHeaderView.unbindFromTask();
            this.mHeaderView.mApplicationIcon.setOnClickListener(null);
            this.mHeaderView.mDismissButton.setOnClickListener(null);
            if (this.mConfig.multiStackEnabled) {
                this.mHeaderView.mMoveTaskButton.setOnClickListener(null);
            }
            this.mActionButtonView.setOnClickListener(null);
            this.mHeaderView.mApplicationIcon.setOnLongClickListener(null);
        }
        this.mTaskDataLoaded = false;
    }

    public void onClick(final View v) {
        final TaskView tv = this;
        boolean delayViewClick = (v == this || v == this.mActionButtonView) ? false : true;
        if (delayViewClick) {
            postDelayed(new Runnable() {
                public void run() {
                    if (v == TaskView.this.mHeaderView.mApplicationIcon) {
                        AccessibilityManager am = (AccessibilityManager) TaskView.this.getContext().getSystemService("accessibility");
                        if (am != null && am.isEnabled() && TaskView.this.mCb != null) {
                            TaskView.this.mCb.onTaskViewAppInfoClicked(tv);
                        }
                    } else if (v == TaskView.this.mHeaderView.mDismissButton) {
                        TaskView.this.dismissTask();
                        MetricsLogger.histogram(TaskView.this.getContext(), "overview_task_dismissed_source", 2);
                    } else if (v == TaskView.this.mHeaderView.mMoveTaskButton && TaskView.this.mCb != null) {
                        TaskView.this.mCb.onTaskResize(tv);
                    }
                }
            }, 125);
            return;
        }
        if (v == this.mActionButtonView) {
            this.mActionButtonView.setTranslationZ(0.0f);
        }
        if (this.mCb != null) {
            this.mCb.onTaskViewClicked(tv, getTask(), v == this.mActionButtonView);
        }
    }

    public boolean onLongClick(View v) {
        if (v != this.mHeaderView.mApplicationIcon || this.mCb == null) {
            return false;
        }
        this.mCb.onTaskViewAppInfoClicked(this);
        return true;
    }
}
