package com.android.systemui.recents.misc;

import android.animation.Animator;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.view.View;
import java.util.ArrayList;

public class Utilities {
    public static void scaleRectAboutCenter(Rect r, float scale) {
        if (scale != 1.0f) {
            int cx = r.centerX();
            int cy = r.centerY();
            r.offset(-cx, -cy);
            r.left = (int) ((((float) r.left) * scale) + 0.5f);
            r.top = (int) ((((float) r.top) * scale) + 0.5f);
            r.right = (int) ((((float) r.right) * scale) + 0.5f);
            r.bottom = (int) ((((float) r.bottom) * scale) + 0.5f);
            r.offset(cx, cy);
        }
    }

    public static float mapCoordInDescendentToSelf(View descendant, View root, float[] coord, boolean includeRootScroll) {
        ArrayList<View> ancestorChain = new ArrayList();
        float[] pt = new float[]{coord[0], coord[1]};
        View v = descendant;
        while (v != root && v != null) {
            ancestorChain.add(v);
            v = (View) v.getParent();
        }
        ancestorChain.add(root);
        float scale = 1.0f;
        int count = ancestorChain.size();
        for (int i = 0; i < count; i++) {
            View v0 = (View) ancestorChain.get(i);
            if (v0 != descendant || includeRootScroll) {
                pt[0] = pt[0] - ((float) v0.getScrollX());
                pt[1] = pt[1] - ((float) v0.getScrollY());
            }
            v0.getMatrix().mapPoints(pt);
            pt[0] = pt[0] + ((float) v0.getLeft());
            pt[1] = pt[1] + ((float) v0.getTop());
            scale *= v0.getScaleX();
        }
        coord[0] = pt[0];
        coord[1] = pt[1];
        return scale;
    }

    public static float mapCoordInSelfToDescendent(View descendant, View root, float[] coord, Matrix tmpInverseMatrix) {
        ArrayList<View> ancestorChain = new ArrayList();
        float[] pt = new float[]{coord[0], coord[1]};
        for (View v = descendant; v != root; v = (View) v.getParent()) {
            ancestorChain.add(v);
        }
        ancestorChain.add(root);
        float scale = 1.0f;
        int count = ancestorChain.size();
        tmpInverseMatrix.set(Matrix.IDENTITY_MATRIX);
        int i = count - 1;
        while (i >= 0) {
            View ancestor = (View) ancestorChain.get(i);
            View view = i > 0 ? (View) ancestorChain.get(i - 1) : null;
            pt[0] = pt[0] + ((float) ancestor.getScrollX());
            pt[1] = pt[1] + ((float) ancestor.getScrollY());
            if (view != null) {
                pt[0] = pt[0] - ((float) view.getLeft());
                pt[1] = pt[1] - ((float) view.getTop());
                view.getMatrix().invert(tmpInverseMatrix);
                tmpInverseMatrix.mapPoints(pt);
                scale *= view.getScaleX();
            }
            i--;
        }
        coord[0] = pt[0];
        coord[1] = pt[1];
        return scale;
    }

    public static float computeContrastBetweenColors(int bg, int fg) {
        float bgR = ((float) Color.red(bg)) / 255.0f;
        float bgG = ((float) Color.green(bg)) / 255.0f;
        float bgB = ((float) Color.blue(bg)) / 255.0f;
        float fgR = ((float) Color.red(fg)) / 255.0f;
        float fgG = ((float) Color.green(fg)) / 255.0f;
        float fgB = ((float) Color.blue(fg)) / 255.0f;
        return Math.abs((0.05f + (((0.2126f * (fgR < 0.03928f ? fgR / 12.92f : (float) Math.pow((double) ((0.055f + fgR) / 1.055f), 2.4000000953674316d))) + (0.7152f * (fgG < 0.03928f ? fgG / 12.92f : (float) Math.pow((double) ((0.055f + fgG) / 1.055f), 2.4000000953674316d)))) + (0.0722f * (fgB < 0.03928f ? fgB / 12.92f : (float) Math.pow((double) ((0.055f + fgB) / 1.055f), 2.4000000953674316d))))) / (0.05f + (((0.2126f * (bgR < 0.03928f ? bgR / 12.92f : (float) Math.pow((double) ((0.055f + bgR) / 1.055f), 2.4000000953674316d))) + (0.7152f * (bgG < 0.03928f ? bgG / 12.92f : (float) Math.pow((double) ((0.055f + bgG) / 1.055f), 2.4000000953674316d)))) + (0.0722f * (bgB < 0.03928f ? bgB / 12.92f : (float) Math.pow((double) ((0.055f + bgB) / 1.055f), 2.4000000953674316d))))));
    }

    public static int getColorWithOverlay(int baseColor, int overlayColor, float overlayAlpha) {
        return Color.rgb((int) ((((float) Color.red(baseColor)) * overlayAlpha) + ((1.0f - overlayAlpha) * ((float) Color.red(overlayColor)))), (int) ((((float) Color.green(baseColor)) * overlayAlpha) + ((1.0f - overlayAlpha) * ((float) Color.green(overlayColor)))), (int) ((((float) Color.blue(baseColor)) * overlayAlpha) + ((1.0f - overlayAlpha) * ((float) Color.blue(overlayColor)))));
    }

    public static void cancelAnimationWithoutCallbacks(Animator animator) {
        if (animator != null) {
            animator.removeAllListeners();
            animator.cancel();
        }
    }
}
