package android.support.v4.view;

import android.os.Build.VERSION;
import android.view.VelocityTracker;

public class VelocityTrackerCompat {
    static final VelocityTrackerVersionImpl IMPL;

    interface VelocityTrackerVersionImpl {
        float getXVelocity(VelocityTracker velocityTracker, int i);
    }

    static class BaseVelocityTrackerVersionImpl implements VelocityTrackerVersionImpl {
        BaseVelocityTrackerVersionImpl() {
        }

        public float getXVelocity(VelocityTracker tracker, int pointerId) {
            return tracker.getXVelocity();
        }
    }

    static class HoneycombVelocityTrackerVersionImpl implements VelocityTrackerVersionImpl {
        HoneycombVelocityTrackerVersionImpl() {
        }

        public float getXVelocity(VelocityTracker tracker, int pointerId) {
            return VelocityTrackerCompatHoneycomb.getXVelocity(tracker, pointerId);
        }
    }

    static {
        if (VERSION.SDK_INT >= 11) {
            IMPL = new HoneycombVelocityTrackerVersionImpl();
        } else {
            IMPL = new BaseVelocityTrackerVersionImpl();
        }
    }

    public static float getXVelocity(VelocityTracker tracker, int pointerId) {
        return IMPL.getXVelocity(tracker, pointerId);
    }
}
