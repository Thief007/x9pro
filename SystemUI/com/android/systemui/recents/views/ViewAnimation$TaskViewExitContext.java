package com.android.systemui.recents.views;

import com.android.systemui.recents.misc.ReferenceCountedTrigger;

public class ViewAnimation$TaskViewExitContext {
    int offscreenTranslationY;
    ReferenceCountedTrigger postAnimationTrigger;

    public ViewAnimation$TaskViewExitContext(ReferenceCountedTrigger t) {
        this.postAnimationTrigger = t;
    }
}
