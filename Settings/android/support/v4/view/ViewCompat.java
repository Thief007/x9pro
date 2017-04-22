package android.support.v4.view;

import android.graphics.Paint;
import android.os.Build.VERSION;
import android.support.annotation.Nullable;
import android.view.View;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.WeakHashMap;

public class ViewCompat {
    static final ViewCompatImpl IMPL;

    interface ViewCompatImpl {
        boolean canScrollHorizontally(View view, int i);

        int getImportantForAccessibility(View view);

        int getOverScrollMode(View view);

        void postInvalidateOnAnimation(View view);

        void postOnAnimation(View view, Runnable runnable);

        void setAccessibilityDelegate(View view, @Nullable AccessibilityDelegateCompat accessibilityDelegateCompat);

        void setImportantForAccessibility(View view, int i);

        void setLayerType(View view, int i, Paint paint);
    }

    static class BaseViewCompatImpl implements ViewCompatImpl {
        WeakHashMap<View, ViewPropertyAnimatorCompat> mViewPropertyAnimatorCompatMap = null;

        BaseViewCompatImpl() {
        }

        public boolean canScrollHorizontally(View v, int direction) {
            if (v instanceof ScrollingView) {
                return canScrollingViewScrollHorizontally((ScrollingView) v, direction);
            }
            return false;
        }

        public int getOverScrollMode(View v) {
            return 2;
        }

        public void setAccessibilityDelegate(View v, AccessibilityDelegateCompat delegate) {
        }

        public void postInvalidateOnAnimation(View view) {
            view.invalidate();
        }

        public void postOnAnimation(View view, Runnable action) {
            view.postDelayed(action, getFrameTime());
        }

        long getFrameTime() {
            return 10;
        }

        public int getImportantForAccessibility(View view) {
            return 0;
        }

        public void setImportantForAccessibility(View view, int mode) {
        }

        public void setLayerType(View view, int layerType, Paint paint) {
        }

        private boolean canScrollingViewScrollHorizontally(ScrollingView view, int direction) {
            boolean z = true;
            int offset = view.computeHorizontalScrollOffset();
            int range = view.computeHorizontalScrollRange() - view.computeHorizontalScrollExtent();
            if (range == 0) {
                return false;
            }
            if (direction < 0) {
                if (offset <= 0) {
                    z = false;
                }
                return z;
            }
            if (offset >= range - 1) {
                z = false;
            }
            return z;
        }
    }

    static class EclairMr1ViewCompatImpl extends BaseViewCompatImpl {
        EclairMr1ViewCompatImpl() {
        }
    }

    static class GBViewCompatImpl extends EclairMr1ViewCompatImpl {
        GBViewCompatImpl() {
        }

        public int getOverScrollMode(View v) {
            return ViewCompatGingerbread.getOverScrollMode(v);
        }
    }

    static class HCViewCompatImpl extends GBViewCompatImpl {
        HCViewCompatImpl() {
        }

        long getFrameTime() {
            return ViewCompatHC.getFrameTime();
        }

        public void setLayerType(View view, int layerType, Paint paint) {
            ViewCompatHC.setLayerType(view, layerType, paint);
        }
    }

    static class ICSViewCompatImpl extends HCViewCompatImpl {
        static boolean accessibilityDelegateCheckFailed = false;

        ICSViewCompatImpl() {
        }

        public boolean canScrollHorizontally(View v, int direction) {
            return ViewCompatICS.canScrollHorizontally(v, direction);
        }

        public void setAccessibilityDelegate(View v, @Nullable AccessibilityDelegateCompat delegate) {
            Object obj = null;
            if (delegate != null) {
                obj = delegate.getBridge();
            }
            ViewCompatICS.setAccessibilityDelegate(v, obj);
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface ImportantForAccessibility {
    }

    static class JBViewCompatImpl extends ICSViewCompatImpl {
        JBViewCompatImpl() {
        }

        public void postInvalidateOnAnimation(View view) {
            ViewCompatJB.postInvalidateOnAnimation(view);
        }

        public void postOnAnimation(View view, Runnable action) {
            ViewCompatJB.postOnAnimation(view, action);
        }

        public int getImportantForAccessibility(View view) {
            return ViewCompatJB.getImportantForAccessibility(view);
        }

        public void setImportantForAccessibility(View view, int mode) {
            if (mode == 4) {
                mode = 2;
            }
            ViewCompatJB.setImportantForAccessibility(view, mode);
        }
    }

    static class JbMr1ViewCompatImpl extends JBViewCompatImpl {
        JbMr1ViewCompatImpl() {
        }
    }

    static class JbMr2ViewCompatImpl extends JbMr1ViewCompatImpl {
        JbMr2ViewCompatImpl() {
        }
    }

    static class KitKatViewCompatImpl extends JbMr2ViewCompatImpl {
        KitKatViewCompatImpl() {
        }

        public void setImportantForAccessibility(View view, int mode) {
            ViewCompatJB.setImportantForAccessibility(view, mode);
        }
    }

    static class LollipopViewCompatImpl extends KitKatViewCompatImpl {
        LollipopViewCompatImpl() {
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface OverScroll {
    }

    static {
        int version = VERSION.SDK_INT;
        if (version >= 21) {
            IMPL = new LollipopViewCompatImpl();
        } else if (version >= 19) {
            IMPL = new KitKatViewCompatImpl();
        } else if (version >= 17) {
            IMPL = new JbMr1ViewCompatImpl();
        } else if (version >= 16) {
            IMPL = new JBViewCompatImpl();
        } else if (version >= 14) {
            IMPL = new ICSViewCompatImpl();
        } else if (version >= 11) {
            IMPL = new HCViewCompatImpl();
        } else if (version >= 9) {
            IMPL = new GBViewCompatImpl();
        } else if (version >= 7) {
            IMPL = new EclairMr1ViewCompatImpl();
        } else {
            IMPL = new BaseViewCompatImpl();
        }
    }

    public static boolean canScrollHorizontally(View v, int direction) {
        return IMPL.canScrollHorizontally(v, direction);
    }

    public static int getOverScrollMode(View v) {
        return IMPL.getOverScrollMode(v);
    }

    public static void setAccessibilityDelegate(View v, AccessibilityDelegateCompat delegate) {
        IMPL.setAccessibilityDelegate(v, delegate);
    }

    public static void postInvalidateOnAnimation(View view) {
        IMPL.postInvalidateOnAnimation(view);
    }

    public static void postOnAnimation(View view, Runnable action) {
        IMPL.postOnAnimation(view, action);
    }

    public static int getImportantForAccessibility(View view) {
        return IMPL.getImportantForAccessibility(view);
    }

    public static void setImportantForAccessibility(View view, int mode) {
        IMPL.setImportantForAccessibility(view, mode);
    }

    public static void setLayerType(View view, int layerType, Paint paint) {
        IMPL.setLayerType(view, layerType, paint);
    }
}
