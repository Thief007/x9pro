package com.android.systemui.volume;

import android.content.Context;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

public class IconPulser {
    private final Interpolator mFastOutSlowInInterpolator;

    public IconPulser(Context context) {
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, 17563661);
    }
}
