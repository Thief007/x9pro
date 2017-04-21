package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.systemui.R;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.RecentsTaskLoader;
import com.android.systemui.recents.model.Task;

public class TaskViewHeader extends FrameLayout {
    static Paint sHighlightPaint;
    TextView mActivityDescription;
    ImageView mApplicationIcon;
    RippleDrawable mBackground;
    int mBackgroundColor;
    GradientDrawable mBackgroundColorDrawable;
    RecentsConfiguration mConfig;
    int mCurrentPrimaryColor;
    boolean mCurrentPrimaryColorIsDark;
    Drawable mDarkDismissDrawable;
    PorterDuffColorFilter mDimColorFilter;
    Paint mDimLayerPaint;
    ImageView mDismissButton;
    String mDismissContentDescription;
    AnimatorSet mFocusAnimator;
    boolean mLayersDisabled;
    Drawable mLightDismissDrawable;
    ImageView mMoveTaskButton;
    private SystemServicesProxy mSsp;

    public TaskViewHeader(Context context) {
        this(context, null);
    }

    public TaskViewHeader(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TaskViewHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TaskViewHeader(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mDimLayerPaint = new Paint();
        this.mDimColorFilter = new PorterDuffColorFilter(0, Mode.SRC_ATOP);
        this.mConfig = RecentsConfiguration.getInstance();
        this.mSsp = RecentsTaskLoader.getInstance().getSystemServicesProxy();
        setWillNotDraw(false);
        setClipToOutline(true);
        setOutlineProvider(new ViewOutlineProvider() {
            public void getOutline(View view, Outline outline) {
                outline.setRect(0, 0, TaskViewHeader.this.getMeasuredWidth(), TaskViewHeader.this.getMeasuredHeight());
            }
        });
        this.mLightDismissDrawable = context.getDrawable(R.drawable.recents_dismiss_light);
        this.mDarkDismissDrawable = context.getDrawable(R.drawable.recents_dismiss_dark);
        this.mDismissContentDescription = context.getString(R.string.accessibility_recents_item_will_be_dismissed);
        if (sHighlightPaint == null) {
            sHighlightPaint = new Paint();
            sHighlightPaint.setStyle(Style.STROKE);
            sHighlightPaint.setStrokeWidth((float) this.mConfig.taskViewHighlightPx);
            sHighlightPaint.setColor(this.mConfig.taskBarViewHighlightColor);
            sHighlightPaint.setXfermode(new PorterDuffXfermode(Mode.ADD));
            sHighlightPaint.setAntiAlias(true);
        }
    }

    protected void onFinishInflate() {
        this.mApplicationIcon = (ImageView) findViewById(R.id.application_icon);
        this.mActivityDescription = (TextView) findViewById(R.id.activity_description);
        this.mDismissButton = (ImageView) findViewById(R.id.dismiss_task);
        this.mMoveTaskButton = (ImageView) findViewById(R.id.move_task);
        if (this.mApplicationIcon.getBackground() instanceof RippleDrawable) {
            this.mApplicationIcon.setBackground(null);
        }
        this.mBackgroundColorDrawable = (GradientDrawable) getContext().getDrawable(R.drawable.recents_task_view_header_bg_color);
        this.mBackground = (RippleDrawable) getContext().getDrawable(R.drawable.recents_task_view_header_bg);
        this.mBackground = (RippleDrawable) this.mBackground.mutate().getConstantState().newDrawable();
        this.mBackground.setColor(ColorStateList.valueOf(0));
        this.mBackground.setDrawableByLayerId(this.mBackground.getId(0), this.mBackgroundColorDrawable);
        setBackground(this.mBackground);
    }

    protected void onDraw(Canvas canvas) {
        float offset = (float) Math.ceil((double) (((float) this.mConfig.taskViewHighlightPx) / 2.0f));
        float radius = (float) this.mConfig.taskViewRoundedCornerRadiusPx;
        int count = canvas.save(2);
        canvas.clipRect(0, 0, getMeasuredWidth(), getMeasuredHeight());
        canvas.drawRoundRect(-offset, 0.0f, ((float) getMeasuredWidth()) + offset, ((float) getMeasuredHeight()) + radius, radius, radius, sHighlightPaint);
        canvas.restoreToCount(count);
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    void setDimAlpha(int alpha) {
        this.mDimColorFilter.setColor(Color.argb(alpha, 0, 0, 0));
        this.mDimLayerPaint.setColorFilter(this.mDimColorFilter);
        if (!this.mLayersDisabled) {
            setLayerType(2, this.mDimLayerPaint);
        }
    }

    int getSecondaryColor(int primaryColor, boolean useLightOverlayColor) {
        return Utilities.getColorWithOverlay(primaryColor, useLightOverlayColor ? -1 : -16777216, 0.8f);
    }

    public void rebindToTask(Task t) {
        if (t.activityIcon != null) {
            this.mApplicationIcon.setImageDrawable(t.activityIcon);
        } else if (t.applicationIcon != null) {
            this.mApplicationIcon.setImageDrawable(t.applicationIcon);
        }
        if (!this.mActivityDescription.getText().toString().equals(t.activityLabel)) {
            this.mActivityDescription.setText(t.activityLabel);
        }
        this.mActivityDescription.setContentDescription(t.contentDescription);
        if ((getBackground() instanceof ColorDrawable ? ((ColorDrawable) getBackground()).getColor() : 0) != t.colorPrimary) {
            this.mBackgroundColorDrawable.setColor(t.colorPrimary);
            this.mBackgroundColor = t.colorPrimary;
        }
        this.mCurrentPrimaryColor = t.colorPrimary;
        this.mCurrentPrimaryColorIsDark = t.useLightOnPrimaryColor;
        this.mActivityDescription.setTextColor(t.useLightOnPrimaryColor ? this.mConfig.taskBarViewLightTextColor : this.mConfig.taskBarViewDarkTextColor);
        this.mDismissButton.setImageDrawable(t.useLightOnPrimaryColor ? this.mLightDismissDrawable : this.mDarkDismissDrawable);
        this.mDismissButton.setContentDescription(String.format(this.mDismissContentDescription, new Object[]{t.contentDescription}));
        this.mMoveTaskButton.setVisibility(this.mConfig.multiStackEnabled ? 0 : 4);
        if (this.mConfig.multiStackEnabled) {
            updateResizeTaskBarIcon(t);
        }
    }

    void updateResizeTaskBarIcon(Task t) {
        Rect display = this.mSsp.getWindowRect();
        Rect taskRect = this.mSsp.getTaskBounds(t.key.stackId);
        int resId = R.drawable.star;
        if (display.equals(taskRect) || taskRect.isEmpty()) {
            resId = R.drawable.vector_drawable_place_fullscreen;
        } else {
            boolean top = display.top == taskRect.top;
            boolean bottom = display.bottom == taskRect.bottom;
            boolean left = display.left == taskRect.left;
            boolean right = display.right == taskRect.right;
            if (top && bottom && left) {
                resId = R.drawable.vector_drawable_place_left;
            } else if (top && bottom && right) {
                resId = R.drawable.vector_drawable_place_right;
            } else if (top && left && right) {
                resId = R.drawable.vector_drawable_place_top;
            } else if (bottom && left && right) {
                resId = R.drawable.vector_drawable_place_bottom;
            } else if (top && right) {
                resId = R.drawable.vector_drawable_place_top_right;
            } else if (top && left) {
                resId = R.drawable.vector_drawable_place_top_left;
            } else if (bottom && right) {
                resId = R.drawable.vector_drawable_place_bottom_right;
            } else if (bottom && left) {
                resId = R.drawable.vector_drawable_place_bottom_left;
            }
        }
        this.mMoveTaskButton.setImageResource(resId);
    }

    void unbindFromTask() {
        this.mApplicationIcon.setImageDrawable(null);
    }

    void startLaunchTaskDismissAnimation() {
        if (this.mDismissButton.getVisibility() == 0) {
            this.mDismissButton.animate().cancel();
            this.mDismissButton.animate().alpha(0.0f).setStartDelay(0).setInterpolator(this.mConfig.fastOutSlowInInterpolator).setDuration((long) this.mConfig.taskViewExitToAppDuration).start();
        }
    }

    void startNoUserInteractionAnimation() {
        if (this.mDismissButton.getVisibility() != 0) {
            this.mDismissButton.setVisibility(0);
            this.mDismissButton.setAlpha(0.0f);
            this.mDismissButton.animate().alpha(1.0f).setStartDelay(0).setInterpolator(this.mConfig.fastOutLinearInInterpolator).setDuration((long) this.mConfig.taskViewEnterFromAppDuration).start();
        }
    }

    void setNoUserInteractionState() {
        if (this.mDismissButton.getVisibility() != 0) {
            this.mDismissButton.animate().cancel();
            this.mDismissButton.setVisibility(0);
            this.mDismissButton.setAlpha(1.0f);
        }
    }

    void resetNoUserInteractionState() {
        this.mDismissButton.setVisibility(4);
    }

    protected int[] onCreateDrawableState(int extraSpace) {
        return new int[0];
    }

    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (this.mLayersDisabled) {
            this.mLayersDisabled = false;
            postOnAnimation(new Runnable() {
                public void run() {
                    TaskViewHeader.this.mLayersDisabled = false;
                    TaskViewHeader.this.setLayerType(2, TaskViewHeader.this.mDimLayerPaint);
                }
            });
        }
    }

    public void disableLayersForOneFrame() {
        this.mLayersDisabled = true;
        setLayerType(0, null);
    }

    void onTaskViewFocusChanged(boolean focused, boolean animateFocusedState) {
        if (animateFocusedState) {
            boolean z = false;
            if (this.mFocusAnimator != null) {
                z = this.mFocusAnimator.isRunning();
                Utilities.cancelAnimationWithoutCallbacks(this.mFocusAnimator);
            }
            int currentColor;
            ValueAnimator backgroundColor;
            ObjectAnimator translation;
            if (focused) {
                currentColor = this.mBackgroundColor;
                int secondaryColor = getSecondaryColor(this.mCurrentPrimaryColor, this.mCurrentPrimaryColorIsDark);
                states = new int[3][];
                states[1] = new int[]{16842910};
                states[2] = new int[]{16842919};
                int[] newStates = new int[]{0, 16842910, 16842919};
                this.mBackground.setColor(new ColorStateList(states, new int[]{currentColor, secondaryColor, secondaryColor}));
                this.mBackground.setState(newStates);
                int lightPrimaryColor = getSecondaryColor(this.mCurrentPrimaryColor, this.mCurrentPrimaryColorIsDark);
                backgroundColor = ValueAnimator.ofObject(new ArgbEvaluator(), new Object[]{Integer.valueOf(currentColor), Integer.valueOf(lightPrimaryColor)});
                backgroundColor.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationStart(Animator animation) {
                        TaskViewHeader.this.mBackground.setState(new int[0]);
                    }
                });
                backgroundColor.addUpdateListener(new AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int color = ((Integer) animation.getAnimatedValue()).intValue();
                        TaskViewHeader.this.mBackgroundColorDrawable.setColor(color);
                        TaskViewHeader.this.mBackgroundColor = color;
                    }
                });
                backgroundColor.setRepeatCount(-1);
                backgroundColor.setRepeatMode(2);
                translation = ObjectAnimator.ofFloat(this, "translationZ", new float[]{15.0f});
                translation.setRepeatCount(-1);
                translation.setRepeatMode(2);
                this.mFocusAnimator = new AnimatorSet();
                this.mFocusAnimator.playTogether(new Animator[]{backgroundColor, translation});
                this.mFocusAnimator.setStartDelay(150);
                this.mFocusAnimator.setDuration(750);
                this.mFocusAnimator.start();
            } else if (z) {
                currentColor = this.mBackgroundColor;
                ValueAnimator.ofObject(new ArgbEvaluator(), new Object[]{Integer.valueOf(currentColor), Integer.valueOf(this.mCurrentPrimaryColor)}).addUpdateListener(new AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int color = ((Integer) animation.getAnimatedValue()).intValue();
                        TaskViewHeader.this.mBackgroundColorDrawable.setColor(color);
                        TaskViewHeader.this.mBackgroundColor = color;
                    }
                });
                translation = ObjectAnimator.ofFloat(this, "translationZ", new float[]{0.0f});
                this.mFocusAnimator = new AnimatorSet();
                this.mFocusAnimator.playTogether(new Animator[]{backgroundColor, translation});
                this.mFocusAnimator.setDuration(150);
                this.mFocusAnimator.start();
            } else {
                this.mBackground.setState(new int[0]);
                setTranslationZ(0.0f);
            }
        }
    }
}
