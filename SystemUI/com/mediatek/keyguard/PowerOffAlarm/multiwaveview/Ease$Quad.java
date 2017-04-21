package com.mediatek.keyguard.PowerOffAlarm.multiwaveview;

import android.animation.TimeInterpolator;

class Ease$Quad {
    public static final TimeInterpolator easeIn = new TimeInterpolator() {
        public float getInterpolation(float input) {
            input /= 1.0f;
            return ((1.0f * input) * input) + 0.0f;
        }
    };
    public static final TimeInterpolator easeInOut = new TimeInterpolator() {
        public float getInterpolation(float input) {
            input /= 0.5f;
            if (input < 1.0f) {
                return ((0.5f * input) * input) + 0.0f;
            }
            input -= 1.0f;
            return ((((input - 2.0f) * input) - 1.0f) * -0.5f) + 0.0f;
        }
    };
    public static final TimeInterpolator easeOut = new TimeInterpolator() {
        public float getInterpolation(float input) {
            input /= 1.0f;
            return ((-1.0f * input) * (input - 2.0f)) + 0.0f;
        }
    };

    Ease$Quad() {
    }
}
