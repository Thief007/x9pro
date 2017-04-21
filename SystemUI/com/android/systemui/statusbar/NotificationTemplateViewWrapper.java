package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import com.android.systemui.R;
import com.android.systemui.ViewInvertHelper;

public class NotificationTemplateViewWrapper extends NotificationViewWrapper {
    private final ColorMatrix mGrayscaleColorMatrix = new ColorMatrix();
    private ImageView mIcon;
    private int mIconBackgroundColor;
    private final int mIconBackgroundDarkColor;
    private final PorterDuffColorFilter mIconColorFilter = new PorterDuffColorFilter(0, Mode.SRC_ATOP);
    private final int mIconDarkAlpha;
    private boolean mIconForceGraysaleWhenDark;
    private ViewInvertHelper mInvertHelper;
    private final Interpolator mLinearOutSlowInInterpolator;
    protected ImageView mPicture;

    protected NotificationTemplateViewWrapper(Context ctx, View view) {
        super(view);
        this.mIconDarkAlpha = ctx.getResources().getInteger(R.integer.doze_small_icon_alpha);
        this.mIconBackgroundDarkColor = ctx.getColor(R.color.doze_small_icon_background_color);
        this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(ctx, 17563662);
        resolveViews();
    }

    private void resolveViews() {
        boolean z;
        ViewInvertHelper viewInvertHelper = null;
        View mainColumn = this.mView.findViewById(16909163);
        if (mainColumn != null) {
            viewInvertHelper = new ViewInvertHelper(mainColumn, 700);
        }
        this.mInvertHelper = viewInvertHelper;
        ImageView largeIcon = (ImageView) this.mView.findViewById(16908294);
        this.mIcon = resolveIcon(largeIcon, (ImageView) this.mView.findViewById(16908352));
        this.mPicture = resolvePicture(largeIcon);
        this.mIconBackgroundColor = resolveBackgroundColor(this.mIcon);
        Drawable drawable = this.mIcon != null ? this.mIcon.getDrawable() : null;
        if (drawable == null || drawable.getColorFilter() == null) {
            z = false;
        } else {
            z = true;
        }
        this.mIconForceGraysaleWhenDark = z;
    }

    private ImageView resolveIcon(ImageView largeIcon, ImageView rightIcon) {
        if (largeIcon == null || largeIcon.getBackground() == null) {
            return (rightIcon == null || rightIcon.getVisibility() != 0) ? null : rightIcon;
        } else {
            return largeIcon;
        }
    }

    private ImageView resolvePicture(ImageView largeIcon) {
        return (largeIcon == null || largeIcon.getBackground() != null) ? null : largeIcon;
    }

    private int resolveBackgroundColor(ImageView icon) {
        if (!(icon == null || icon.getBackground() == null)) {
            ColorFilter filter = icon.getBackground().getColorFilter();
            if (filter instanceof PorterDuffColorFilter) {
                return ((PorterDuffColorFilter) filter).getColor();
            }
        }
        return 0;
    }

    public void notifyContentUpdated() {
        super.notifyContentUpdated();
        resolveViews();
    }

    public void setDark(boolean dark, boolean fade, long delay) {
        if (this.mInvertHelper != null) {
            if (fade) {
                this.mInvertHelper.fade(dark, delay);
            } else {
                this.mInvertHelper.update(dark);
            }
        }
        if (this.mIcon != null) {
            if (fade) {
                fadeIconColorFilter(this.mIcon, dark, delay);
                fadeIconAlpha(this.mIcon, dark, delay);
                if (!this.mIconForceGraysaleWhenDark) {
                    fadeGrayscale(this.mIcon, dark, delay);
                }
            } else {
                updateIconColorFilter(this.mIcon, dark);
                updateIconAlpha(this.mIcon, dark);
                if (!this.mIconForceGraysaleWhenDark) {
                    updateGrayscale(this.mIcon, dark);
                }
            }
        }
        setPictureGrayscale(dark, fade, delay);
    }

    protected void setPictureGrayscale(boolean grayscale, boolean fade, long delay) {
        if (this.mPicture == null) {
            return;
        }
        if (fade) {
            fadeGrayscale(this.mPicture, grayscale, delay);
        } else {
            updateGrayscale(this.mPicture, grayscale);
        }
    }

    private void startIntensityAnimation(AnimatorUpdateListener updateListener, boolean dark, long delay, AnimatorListener listener) {
        float startIntensity = dark ? 0.0f : 1.0f;
        float endIntensity = dark ? 1.0f : 0.0f;
        ValueAnimator animator = ValueAnimator.ofFloat(new float[]{startIntensity, endIntensity});
        animator.addUpdateListener(updateListener);
        animator.setDuration(700);
        animator.setInterpolator(this.mLinearOutSlowInInterpolator);
        animator.setStartDelay(delay);
        if (listener != null) {
            animator.addListener(listener);
        }
        animator.start();
    }

    private void fadeIconColorFilter(final ImageView target, boolean dark, long delay) {
        startIntensityAnimation(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                NotificationTemplateViewWrapper.this.updateIconColorFilter(target, ((Float) animation.getAnimatedValue()).floatValue());
            }
        }, dark, delay, null);
    }

    private void fadeIconAlpha(final ImageView target, boolean dark, long delay) {
        startIntensityAnimation(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float t = ((Float) animation.getAnimatedValue()).floatValue();
                target.setImageAlpha((int) (((1.0f - t) * 255.0f) + (((float) NotificationTemplateViewWrapper.this.mIconDarkAlpha) * t)));
            }
        }, dark, delay, null);
    }

    protected void fadeGrayscale(final ImageView target, final boolean dark, long delay) {
        startIntensityAnimation(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                NotificationTemplateViewWrapper.this.updateGrayscaleMatrix(((Float) animation.getAnimatedValue()).floatValue());
                target.setColorFilter(new ColorMatrixColorFilter(NotificationTemplateViewWrapper.this.mGrayscaleColorMatrix));
            }
        }, dark, delay, new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                if (!dark) {
                    target.setColorFilter(null);
                }
            }
        });
    }

    private void updateIconColorFilter(ImageView target, boolean dark) {
        updateIconColorFilter(target, dark ? 1.0f : 0.0f);
    }

    private void updateIconColorFilter(ImageView target, float intensity) {
        this.mIconColorFilter.setColor(interpolateColor(this.mIconBackgroundColor, this.mIconBackgroundDarkColor, intensity));
        Drawable background = target.getBackground();
        if (background != null) {
            background.mutate().setColorFilter(this.mIconColorFilter);
        }
    }

    private void updateIconAlpha(ImageView target, boolean dark) {
        target.setImageAlpha(dark ? this.mIconDarkAlpha : 255);
    }

    protected void updateGrayscale(ImageView target, boolean dark) {
        if (dark) {
            updateGrayscaleMatrix(1.0f);
            target.setColorFilter(new ColorMatrixColorFilter(this.mGrayscaleColorMatrix));
            return;
        }
        target.setColorFilter(null);
    }

    private void updateGrayscaleMatrix(float intensity) {
        this.mGrayscaleColorMatrix.setSaturation(1.0f - intensity);
    }

    private static int interpolateColor(int source, int target, float t) {
        int aSource = Color.alpha(source);
        int rSource = Color.red(source);
        int gSource = Color.green(source);
        int bSource = Color.blue(source);
        return Color.argb((int) ((((float) aSource) * (1.0f - t)) + (((float) Color.alpha(target)) * t)), (int) ((((float) rSource) * (1.0f - t)) + (((float) Color.red(target)) * t)), (int) ((((float) gSource) * (1.0f - t)) + (((float) Color.green(target)) * t)), (int) ((((float) bSource) * (1.0f - t)) + (((float) Color.blue(target)) * t)));
    }
}
