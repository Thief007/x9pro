package com.android.systemui.recents.views;

import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.Rect;
import com.android.systemui.recents.misc.ReferenceCountedTrigger;

public class ViewAnimation$TaskViewEnterContext {
    int currentStackViewCount;
    int currentStackViewIndex;
    boolean currentTaskOccludesLaunchTarget;
    Rect currentTaskRect;
    TaskViewTransform currentTaskTransform;
    public ReferenceCountedTrigger postAnimationTrigger;
    AnimatorUpdateListener updateListener;

    public ViewAnimation$TaskViewEnterContext(ReferenceCountedTrigger t) {
        this.postAnimationTrigger = t;
    }
}
