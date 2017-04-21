package com.android.systemui.statusbar.stack;

import java.util.ArrayList;

public class AnimationFilter {
    boolean animateAlpha;
    boolean animateDark;
    boolean animateDimmed;
    boolean animateHeight;
    boolean animateHideSensitive;
    boolean animateScale;
    boolean animateTopInset;
    boolean animateY;
    boolean animateZ;
    int darkAnimationOriginIndex;
    boolean hasDarkEvent;
    boolean hasDelays;
    boolean hasGoToFullShadeEvent;

    public AnimationFilter animateAlpha() {
        this.animateAlpha = true;
        return this;
    }

    public AnimationFilter animateY() {
        this.animateY = true;
        return this;
    }

    public AnimationFilter hasDelays() {
        this.hasDelays = true;
        return this;
    }

    public AnimationFilter animateZ() {
        this.animateZ = true;
        return this;
    }

    public AnimationFilter animateScale() {
        this.animateScale = true;
        return this;
    }

    public AnimationFilter animateHeight() {
        this.animateHeight = true;
        return this;
    }

    public AnimationFilter animateTopInset() {
        this.animateTopInset = true;
        return this;
    }

    public AnimationFilter animateDimmed() {
        this.animateDimmed = true;
        return this;
    }

    public AnimationFilter animateDark() {
        this.animateDark = true;
        return this;
    }

    public AnimationFilter animateHideSensitive() {
        this.animateHideSensitive = true;
        return this;
    }

    public void applyCombination(ArrayList<AnimationEvent> events) {
        reset();
        int size = events.size();
        for (int i = 0; i < size; i++) {
            AnimationEvent ev = (AnimationEvent) events.get(i);
            combineFilter(((AnimationEvent) events.get(i)).filter);
            if (ev.animationType == 10) {
                this.hasGoToFullShadeEvent = true;
            }
            if (ev.animationType == 9) {
                this.hasDarkEvent = true;
                this.darkAnimationOriginIndex = ev.darkAnimationOriginIndex;
            }
        }
    }

    private void combineFilter(AnimationFilter filter) {
        this.animateAlpha |= filter.animateAlpha;
        this.animateY |= filter.animateY;
        this.animateZ |= filter.animateZ;
        this.animateScale |= filter.animateScale;
        this.animateHeight |= filter.animateHeight;
        this.animateTopInset |= filter.animateTopInset;
        this.animateDimmed |= filter.animateDimmed;
        this.animateDark |= filter.animateDark;
        this.animateHideSensitive |= filter.animateHideSensitive;
        this.hasDelays |= filter.hasDelays;
    }

    private void reset() {
        this.animateAlpha = false;
        this.animateY = false;
        this.animateZ = false;
        this.animateScale = false;
        this.animateHeight = false;
        this.animateTopInset = false;
        this.animateDimmed = false;
        this.animateDark = false;
        this.animateHideSensitive = false;
        this.hasDelays = false;
        this.hasGoToFullShadeEvent = false;
        this.hasDarkEvent = false;
        this.darkAnimationOriginIndex = -1;
    }
}
