package com.mediatek.keyguard.PowerOffAlarm.multiwaveview;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

class Tweener {
    private static AnimatorListener mCleanupListener = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animation) {
            Tweener.remove(animation);
        }

        public void onAnimationCancel(Animator animation) {
            Tweener.remove(animation);
        }
    };
    private static HashMap<Object, Tweener> sTweens = new HashMap();
    ObjectAnimator animator;

    public Tweener(ObjectAnimator anim) {
        this.animator = anim;
    }

    private static void remove(Animator animator) {
        Iterator<Entry<Object, Tweener>> iter = sTweens.entrySet().iterator();
        while (iter.hasNext()) {
            if (((Tweener) ((Entry) iter.next()).getValue()).animator == animator) {
                iter.remove();
                return;
            }
        }
    }

    public static Tweener to(Object object, long duration, Object... vars) {
        ObjectAnimator anim;
        long delay = 0;
        AnimatorUpdateListener updateListener = null;
        AnimatorListener listener = null;
        TimeInterpolator interpolator = null;
        ArrayList<PropertyValuesHolder> props = new ArrayList(vars.length / 2);
        int i = 0;
        while (i < vars.length) {
            if (vars[i] instanceof String) {
                String key = vars[i];
                TimeInterpolator value = vars[i + 1];
                if (!"simultaneousTween".equals(key)) {
                    if ("ease".equals(key)) {
                        interpolator = value;
                    } else if ("onUpdate".equals(key) || "onUpdateListener".equals(key)) {
                        updateListener = (AnimatorUpdateListener) value;
                    } else if ("onComplete".equals(key) || "onCompleteListener".equals(key)) {
                        listener = (AnimatorListener) value;
                    } else if ("delay".equals(key)) {
                        delay = ((Number) value).longValue();
                    } else if ("syncWith".equals(key)) {
                        continue;
                    } else if (value instanceof float[]) {
                        props.add(PropertyValuesHolder.ofFloat(key, new float[]{((float[]) value)[0], ((float[]) value)[1]}));
                    } else if (value instanceof int[]) {
                        props.add(PropertyValuesHolder.ofInt(key, new int[]{((int[]) value)[0], ((int[]) value)[1]}));
                    } else if (value instanceof Number) {
                        props.add(PropertyValuesHolder.ofFloat(key, new float[]{((Number) value).floatValue()}));
                    } else {
                        throw new IllegalArgumentException("Bad argument for key \"" + key + "\" with value " + value.getClass());
                    }
                }
                i += 2;
            } else {
                throw new IllegalArgumentException("Key must be a string: " + vars[i]);
            }
        }
        Tweener tween = (Tweener) sTweens.get(object);
        if (tween == null) {
            anim = ObjectAnimator.ofPropertyValuesHolder(object, (PropertyValuesHolder[]) props.toArray(new PropertyValuesHolder[props.size()]));
            tween = new Tweener(anim);
            sTweens.put(object, tween);
        } else {
            anim = ((Tweener) sTweens.get(object)).animator;
            replace(props, object);
        }
        if (interpolator != null) {
            anim.setInterpolator(interpolator);
        }
        anim.setStartDelay(delay);
        anim.setDuration(duration);
        if (updateListener != null) {
            anim.removeAllUpdateListeners();
            anim.addUpdateListener(updateListener);
        }
        if (listener != null) {
            anim.removeAllListeners();
            anim.addListener(listener);
        }
        anim.addListener(mCleanupListener);
        return tween;
    }

    private static void replace(ArrayList<PropertyValuesHolder> props, Object... args) {
        for (Object killobject : args) {
            Tweener tween = (Tweener) sTweens.get(killobject);
            if (tween != null) {
                tween.animator.cancel();
                if (props != null) {
                    tween.animator.setValues((PropertyValuesHolder[]) props.toArray(new PropertyValuesHolder[props.size()]));
                } else {
                    sTweens.remove(tween);
                }
            }
        }
    }
}
