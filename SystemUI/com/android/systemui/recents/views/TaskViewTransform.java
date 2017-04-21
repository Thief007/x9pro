package com.android.systemui.recents.views;

import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.Interpolator;

public class TaskViewTransform {
    public float alpha = 1.0f;
    float p = 0.0f;
    public Rect rect = new Rect();
    public float scale = 1.0f;
    public int startDelay = 0;
    public int translationY = 0;
    public float translationZ = 0.0f;
    public boolean visible = false;

    public void reset() {
        this.startDelay = 0;
        this.translationY = 0;
        this.translationZ = 0.0f;
        this.scale = 1.0f;
        this.alpha = 1.0f;
        this.visible = false;
        this.rect.setEmpty();
        this.p = 0.0f;
    }

    public boolean hasAlphaChangedFrom(float v) {
        return Float.compare(this.alpha, v) != 0;
    }

    public boolean hasScaleChangedFrom(float v) {
        return Float.compare(this.scale, v) != 0;
    }

    public boolean hasTranslationYChangedFrom(float v) {
        return Float.compare((float) this.translationY, v) != 0;
    }

    public boolean hasTranslationZChangedFrom(float v) {
        return Float.compare(this.translationZ, v) != 0;
    }

    public void applyToTaskView(View v, int duration, Interpolator interp, boolean allowLayers, boolean allowShadows, AnimatorUpdateListener updateCallback) {
        if (duration > 0) {
            ViewPropertyAnimator anim = v.animate();
            boolean requiresLayers = false;
            if (hasTranslationYChangedFrom(v.getTranslationY())) {
                anim.translationY((float) this.translationY);
            }
            if (allowShadows && hasTranslationZChangedFrom(v.getTranslationZ())) {
                anim.translationZ(this.translationZ);
            }
            if (hasScaleChangedFrom(v.getScaleX())) {
                anim.scaleX(this.scale).scaleY(this.scale);
                requiresLayers = true;
            }
            if (hasAlphaChangedFrom(v.getAlpha())) {
                anim.alpha(this.alpha);
                requiresLayers = true;
            }
            if (requiresLayers && allowLayers) {
                anim.withLayer();
            }
            if (updateCallback != null) {
                anim.setUpdateListener(updateCallback);
            } else {
                anim.setUpdateListener(null);
            }
            anim.setStartDelay((long) this.startDelay).setDuration((long) duration).setInterpolator(interp).start();
            return;
        }
        if (hasTranslationYChangedFrom(v.getTranslationY())) {
            v.setTranslationY((float) this.translationY);
        }
        if (allowShadows && hasTranslationZChangedFrom(v.getTranslationZ())) {
            v.setTranslationZ(this.translationZ);
        }
        if (hasScaleChangedFrom(v.getScaleX())) {
            v.setScaleX(this.scale);
            v.setScaleY(this.scale);
        }
        if (hasAlphaChangedFrom(v.getAlpha())) {
            v.setAlpha(this.alpha);
        }
    }

    public static void reset(View v) {
        v.animate().cancel();
        v.setTranslationX(0.0f);
        v.setTranslationY(0.0f);
        v.setTranslationZ(0.0f);
        v.setScaleX(1.0f);
        v.setScaleY(1.0f);
        v.setAlpha(1.0f);
    }

    public String toString() {
        return "TaskViewTransform delay: " + this.startDelay + " y: " + this.translationY + " z: " + this.translationZ + " scale: " + this.scale + " alpha: " + this.alpha + " visible: " + this.visible + " rect: " + this.rect + " p: " + this.p;
    }
}
